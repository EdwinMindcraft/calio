package io.github.edwinmindcraft.calio.common.registry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import io.github.apace100.calio.data.IExtendedJsonDataLoader;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.event.CalioDynamicRegistryEvent;
import io.github.edwinmindcraft.calio.api.event.DynamicRegistrationEvent;
import io.github.edwinmindcraft.calio.api.registry.DynamicEntryFactory;
import io.github.edwinmindcraft.calio.api.registry.DynamicEntryValidator;
import io.github.edwinmindcraft.calio.api.registry.DynamicRegistryListener;
import io.github.edwinmindcraft.calio.api.registry.CalioDynamicRegistryManager;
import io.github.edwinmindcraft.calio.common.CalioConfig;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import net.minecraft.Util;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.callback.BakeCallback;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.parsers.json.JsonFormat;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.gson.GsonReader;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * These registries do not have a fixed state, and are designed to be added to via datapacks.
 */
// TODO: Rewrite to moreso utilise vanilla's dynamic registry code.
public class CalioDynamicRegistryManagerImpl implements CalioDynamicRegistryManager {
	private static final Gson GSON = new GsonBuilder().create();
	private static final int FILE_SUFFIX_LENGTH = ".json".length();
	private static final CalioDynamicRegistryManager INSTANCE = new CalioDynamicRegistryManagerImpl();
	private boolean lock;
	private final Map<ResourceKey<?>, RegistryDefinition<?>> definitions;
	private final Map<ResourceKey<?>, ReloadFactory<?>> factories;
	private final Map<ResourceKey<?>, Validator<?>> validators;
	private final List<ResourceKey<?>> validatorOrder;
    public static final Set<String> LOADED_NAMESPACES = new HashSet<>();
	private static final Function<Optional<KnownPack>, RegistrationInfo> REGISTRATION_INFO_CACHE = Util.memoize((knownPack) -> {
		Lifecycle lifecycle = knownPack.map(KnownPack::isVanilla).map((p_325560_) ->
				Lifecycle.stable()).orElse(Lifecycle.experimental());
		return new RegistrationInfo(knownPack, lifecycle);
	});

	public CalioDynamicRegistryManagerImpl() {
		this.definitions = new HashMap<>();
		this.factories = new HashMap<>();
		this.validators = new HashMap<>();
		this.lock = false;
		ModLoader.postEvent(new CalioDynamicRegistryEvent.Initialize(this));
        CalioAPI.LOGGER.info("CDRM Initialized with {} registries", String.valueOf(definitions.size()));
		this.lock = true;
		List<ResourceKey<?>> handled = new ArrayList<>();
		int prevSize;
		do {
			prevSize = handled.size();
			for (Validator<?> value : this.validators.values()) {
				if (handled.contains(value.key()))
					continue;
				if (Arrays.stream(value.dependencies()).allMatch(handled::contains))
					handled.add(value.key());
			}
		} while (prevSize != handled.size());
		if (handled.size() != this.validators.size())
			throw new IllegalStateException("Some validators have missing or circular dependencies: [" + String.join(",", this.validators.keySet().stream().filter(x -> !handled.contains(x)).map(x -> x.location().toString()).collect(Collectors.toSet())) + "]");
		for (ResourceKey<?> resourceKey : this.factories.keySet()) {
			if (!handled.contains(resourceKey))
				handled.add(resourceKey);
		}
		this.validatorOrder = ImmutableList.copyOf(handled);
	}

	public <T> WritableRegistry<T> newRegistry(ResourceKey<? extends Registry<T>> registryKey, Lifecycle lifecycle) {
		if (definitions.containsKey(registryKey))
			return (WritableRegistry<T>) definitions.get(registryKey).newRegistry((ResourceKey)registryKey, lifecycle);
		return null;
	}

	public Stream<RegistryDataLoader.RegistryData<?>> sort(Stream<RegistryDataLoader.RegistryData<?>> original) {
		if (validatorOrder.isEmpty())
			return original;
		return original.sorted(Comparator.comparing(registryData -> validatorOrder.indexOf(registryData.key())));
	}

	public void prepare(ResourceManager manager, RegistryAccess registryAccess) {
		NeoForge.EVENT_BUS.post(new CalioDynamicRegistryEvent.Reload(this, registryAccess));
		LOADED_NAMESPACES.addAll(manager.getNamespaces());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <E> boolean reload(
			ResourceManager manager,
			WritableRegistry<E> registry) {
		if (!factories.containsKey(registry.key()))
			return false;
		Map<ResourceLocation, ?> map = factories.get(registry.key()).reload(factories.get(registry.key()).prepare(manager));
		this.validate(registry, (Validator) this.validators.get(registry.key()), (Map)map);
		registry.holders().filter(Holder::isBound).forEach(obj -> {
			if (obj instanceof DynamicRegistryListener drl)
				drl.whenAvailable(this);
		});
		return true;
	}

	public void flush(RegistryAccess access) {
		NeoForge.EVENT_BUS.post(new CalioDynamicRegistryEvent.LoadComplete(this, access));
		LOADED_NAMESPACES.clear();
	}

	public static CalioDynamicRegistryManager getInstance() {
		return INSTANCE;
	}

	@SuppressWarnings("unchecked")
	public <T> Codec<T> getCodec(ResourceKey<Registry<T>> key) {
		return ((Codec<T>) this.definitions.get(key).codec());
	}

	private static <T> void dumpRegistry(Registry<T> reg) {
		if (!CalioConfig.COMMON.logging.get())
			return;
		CalioAPI.LOGGER.info("{}: {} entries", reg.key().location(), String.valueOf(reg.keySet().size()));
		if (CalioConfig.COMMON.reducedLogging.get()) return;
		for (ResourceLocation resourceLocation : reg.keySet()) {
			Optional<Holder.Reference<T>> holder = reg.getHolder(ResourceKey.create(reg.key(), resourceLocation)).filter(Holder::isBound);
			CalioAPI.LOGGER.info("  {}: {}", resourceLocation, holder.map(x -> x.value().toString()).orElse("Missing"));
		}
	}

	private <T> void validate(WritableRegistry<T> registry, @Nullable Validator<T> validator, Map<ResourceLocation, Pair<T, RegistrationInfo>> entries) {
		entries.forEach((location, pair) -> {
			ResourceKey<T> resourceKey = ResourceKey.create(registry.key(), location);
			T t = pair.getFirst();
			if (validator != null)
				t = validator.validate(resourceKey, pair.getFirst(), this);
			if (t != null) {
				if (t instanceof DynamicRegistryListener drl)
					drl.whenNamed(location);
				registry.register(resourceKey, t, pair.getSecond());
			}
		});
	}

	@Override
	public <T> void add(@NotNull ResourceKey<Registry<T>> key, @Nullable Consumer<BiConsumer<ResourceKey<T>, T>> builtin, Codec<T> codec, @Nullable Supplier<ResourceLocation> defaultValue) {
		Validate.isTrue(!this.lock, "Cannot add registries after the dynamic registry manager has initialized");
        if (this.definitions.containsKey(key))
			throw new IllegalArgumentException("Registry for key " + key + " is already added.");
		RegistryDefinition<T> value = new RegistryDefinition<>(builtin, codec, defaultValue);
		this.definitions.put(key, value);
	}

	@Override
	public <T> void addReload(ResourceKey<Registry<T>> key, String directory, DynamicEntryFactory<T> factory) {
		Validate.isTrue(!this.lock, "Cannot add reload factories after the dynamic registry manager has initialized");
		if (this.factories.containsKey(key))
			throw new IllegalArgumentException("Reload factory for registry " + key + " is already added.");
		this.factories.put(key, new ReloadFactory<>(directory, factory));
	}

	@Override
	public <T> void addValidation(ResourceKey<Registry<T>> key, DynamicEntryValidator<T> validator, Class<T> eventClass, @NotNull ResourceKey<?>... after) {
		Validate.isTrue(!this.lock, "Cannot add validators after the dynamic registry manager has initialized");
		if (this.validators.containsKey(key))
			throw new IllegalArgumentException("Reload factory for registry " + key + " is already added.");
		this.validators.put(key, new Validator<>(key, validator, eventClass, after));
	}

	public Set<ResourceKey<Registry<?>>> getRegistryNames() {
		return (Set<ResourceKey<Registry<?>>>) (Set) this.definitions.keySet().stream().sorted(Comparator.comparingInt(value -> !this.validatorOrder.contains(value) ? -1 : this.validatorOrder.indexOf(value))).collect(Collectors.toCollection(() -> new ObjectAVLTreeSet<>()));
	}

	private record Validator<T>(ResourceKey<Registry<T>> key, DynamicEntryValidator<T> validator, Class<T> eventClass,
								ResourceKey<?>[] dependencies) {
		public T validate(ResourceKey<T> entry, T input, CalioDynamicRegistryManager manager) {
			DataResult<T> result = this.validator().validate(entry.location(), input, manager);
			if (result.error().isPresent()) {
				CalioAPI.LOGGER.error("Validation for {} failed with error: {}", entry, result.error().get());
				return null;
			} else {
				T t = result.getOrThrow();
				if (this.eventClass() != null) {
					DynamicRegistrationEvent<T> event = new DynamicRegistrationEvent<>(key, input);
					if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
						if (event.getCancellationReason() != null)
							CalioAPI.LOGGER.info("Registration of {} was cancelled: {}", entry, event.getCancellationReason());
						return null;
					}
					return event.getNewEntry();
				}
				return t;
			}
		}
	}

	private record ReloadFactory<T>(String directory, DynamicEntryFactory<T> factory) implements IExtendedJsonDataLoader {
		public Map<ResourceLocation, Pair<List<JsonElement>, RegistrationInfo>> prepare(ResourceManager resourceManager) {
			Map<ResourceLocation, Pair<List<JsonElement>, RegistrationInfo>> map = Maps.newHashMap();
			Set<String> resourcesHandled = new HashSet<>();
			for (Map.Entry<ResourceLocation, List<Resource>> entry : resourceManager.listResourceStacks(this.directory(), this::hasValidFormat).entrySet()) {
				ResourceLocation identifier = entry.getKey();
				ResourceLocation identifier2 = trim(identifier, this.directory());
				String fileExtension = "." + FilenameUtils.getExtension(identifier.getPath());
				JsonFormat format = getValidFormats().get(fileExtension);
				resourcesHandled.clear();
				for (Resource resource : entry.getValue()) {
					if (!resourcesHandled.contains(resource.sourcePackId())) {
						resourcesHandled.add(resource.sourcePackId());
						try (Reader reader = resource.openAsReader()) {
							GsonReader gsonReader = new GsonReader(JsonReader.create(reader, format));
							JsonElement jsonElement = GSON.fromJson(gsonReader, JsonElement.class);
							if (jsonElement != null) {
								if (map.containsKey(identifier2)) {
									map.get(identifier2).getFirst().add(jsonElement);
								} else {
									List<JsonElement> elementList = new LinkedList<>();
									elementList.add(jsonElement);
									map.put(identifier2, Pair.of(elementList, REGISTRATION_INFO_CACHE.apply(resource.knownPackInfo())));
								}
							} else {
								CalioAPI.LOGGER.error("Couldn't load data file {} from {} as it's null or empty", identifier2, identifier);
							}
						} catch (IllegalArgumentException | IOException | JsonParseException var68) {
							CalioAPI.LOGGER.error("Couldn't parse data file {} from {}", identifier2, identifier, var68);
						}
					}
				}
			}

			return map;
		}

		public Map<ResourceLocation, Pair<T, RegistrationInfo>> reload(Map<ResourceLocation, Pair<List<JsonElement>, RegistrationInfo>> input) {
			ImmutableMap.Builder<ResourceLocation, Pair<T, RegistrationInfo>> builder = ImmutableMap.builder();
			input.forEach((location, pair) -> {
                this.factory().create(location, pair.getFirst()).forEach((rl, t) -> builder.put(rl, Pair.of(t, pair.getSecond())));
            });
			return builder.build();
		}
	}

	private record RegistryDefinition<T>(Consumer<BiConsumer<ResourceKey<T>, T>> builtin, Codec<T> codec,
										 @Nullable Supplier<ResourceLocation> defaultValue) {

		public MappedRegistry<T> newRegistry(ResourceKey<Registry<T>> key, Lifecycle lifecycle) {
			//As this is dynamic, there is no certainty as to the content, so we don't have a backing IdentityHashMap.
			ResourceLocation defaultKey = this.defaultValue() != null ? this.defaultValue().get() : null;
			MappedRegistry<T> registry = defaultKey == null ?
					new MappedRegistry<>(key, Lifecycle.experimental(), false) :
					new DefaultedMappedRegistry<>(defaultKey.toString(), key, lifecycle, false);
			if (this.builtin() != null)
				this.builtin().accept((rl, value) -> registry.register(rl, value, RegistrationInfo.BUILT_IN));
			registry.addCallback(BakeCallback.class, CalioDynamicRegistryManagerImpl::dumpRegistry);
			return registry;
		}
	}
}
