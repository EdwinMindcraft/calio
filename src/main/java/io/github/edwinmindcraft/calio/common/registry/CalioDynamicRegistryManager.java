package io.github.edwinmindcraft.calio.common.registry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.event.CalioDynamicRegistryEvent;
import io.github.edwinmindcraft.calio.api.event.DynamicRegistrationEvent;
import io.github.edwinmindcraft.calio.api.registry.DynamicEntryFactory;
import io.github.edwinmindcraft.calio.api.registry.DynamicEntryValidator;
import io.github.edwinmindcraft.calio.api.registry.ICalioDynamicRegistryManager;
import io.github.edwinmindcraft.calio.client.util.ClientHelper;
import io.github.edwinmindcraft.calio.common.CalioConfig;
import io.github.edwinmindcraft.calio.common.network.CalioNetwork;
import io.github.edwinmindcraft.calio.common.network.packet.S2CDynamicRegistryPacket;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * These registries do not have a fixed state, and are designed to be added to via datapacks.
 */
public class CalioDynamicRegistryManager implements ICalioDynamicRegistryManager {
	private static final Gson GSON = new GsonBuilder().create();
	private static final int FILE_SUFFIX_LENGTH = ".json".length();

	private static final Map<RegistryAccess, CalioDynamicRegistryManager> INSTANCES = new ConcurrentHashMap<>();
	private static CalioDynamicRegistryManager clientInstance = null;
	private static CalioDynamicRegistryManager serverInstance = null;
	private boolean lock;
	private final Map<ResourceKey<?>, MappedRegistry<?>> registries;
	private final Map<ResourceKey<?>, RegistryDefinition<?>> definitions;
	private final Map<ResourceKey<?>, ReloadFactory<?>> factories;
	private final Map<ResourceKey<?>, Validator<?>> validators;
	private final Map<ResourceKey<?>, Object> locks;
	private final List<ResourceKey<?>> validatorOrder;

	public CalioDynamicRegistryManager() {
		this.registries = new HashMap<>();
		this.definitions = new HashMap<>();
		this.factories = new HashMap<>();
		this.validators = new HashMap<>();
		this.locks = new HashMap<>();
		this.lock = false;
		MinecraftForge.EVENT_BUS.post(new CalioDynamicRegistryEvent.Initialize(this));
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

	@Override
	@NotNull
	public CompletableFuture<Void> reload(PreparationBarrier barrier, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller preparationsProfiler, @NotNull ProfilerFiller reloadProfiler, @NotNull Executor preparationsExecutor, @NotNull Executor reloadExecutor) {
		return this.prepare(resourceManager, preparationsExecutor)
				.thenCompose(barrier::wait)
				.thenCompose(x -> this.reload(x, reloadExecutor));
	}

	private CompletableFuture<Map<ResourceKey<?>, Map<ResourceLocation, List<JsonElement>>>> prepare(ResourceManager resourceManager, Executor executor) {
		ConcurrentHashMap<ResourceKey<?>, Map<ResourceLocation, List<JsonElement>>> map = new ConcurrentHashMap<>();
		CompletableFuture<?>[] completableFutures = this.factories.entrySet().stream()
				.map(x -> CompletableFuture.runAsync(() -> map.put(x.getKey(), x.getValue().prepare(resourceManager)), executor))
				.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(completableFutures).thenApplyAsync(x -> map, executor);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private CompletableFuture<Void> reload(Map<ResourceKey<?>, Map<ResourceLocation, List<JsonElement>>> input, Executor executor) {
		MinecraftForge.EVENT_BUS.post(new CalioDynamicRegistryEvent.Reload(this));
		input.keySet().forEach(x -> this.reset((ResourceKey) x));
		ConcurrentHashMap<ResourceKey<?>, Map<ResourceLocation, ?>> map = new ConcurrentHashMap<>();
		CompletableFuture<?>[] completableFutures = this.factories.entrySet().stream()
				.sorted(Comparator.comparingInt(value -> !this.validatorOrder.contains(value.getKey()) ? -1 : this.validatorOrder.indexOf(value.getKey())))
				.map(x -> CompletableFuture.runAsync(() -> map.put(x.getKey(), x.getValue().reload(input.get(x.getKey()))), executor))
				.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(completableFutures).thenAcceptAsync(x -> {
			for (ResourceKey<?> resourceKey : this.validatorOrder) {
				this.validate((ResourceKey) resourceKey, (Validator) this.validators.get(resourceKey), (Map) map.get(resourceKey));
			}
			MinecraftForge.EVENT_BUS.post(new CalioDynamicRegistryEvent.LoadComplete(this));
			this.dump();
		}, executor);
	}

	public void synchronize(PacketDistributor.PacketTarget target) {
		//CalioNetwork.CHANNEL.send(target, new S2CDynamicRegistriesPacket(this));
		for (S2CDynamicRegistryPacket.Play<?> packet : S2CDynamicRegistryPacket.Play.create(this))
			CalioNetwork.CHANNEL.send(target, packet);
	}

	@SuppressWarnings("unchecked")
	public <T> Codec<T> getCodec(ResourceKey<Registry<T>> key) {
		return ((Codec<T>) this.definitions.get(key).codec());
	}

	public void dump() {
		if (!CalioConfig.COMMON.logging.get())
			return;
		CalioAPI.LOGGER.info("Calio dynamic registry dump:");
		this.registries.values().forEach(CalioDynamicRegistryManager::dumpRegistry);
	}

	private static <T> void dumpRegistry(Registry<T> reg) {
		CalioAPI.LOGGER.info("{}: {} entries", reg.key().location(), reg.keySet().size());
		if (CalioConfig.COMMON.reducedLogging.get()) return;
		for (ResourceLocation resourceLocation : reg.keySet()) {
			Optional<Holder<T>> holder = reg.getHolder(ResourceKey.create(reg.key(), resourceLocation)).filter(Holder::isBound);
			CalioAPI.LOGGER.info("  {}: {}", resourceLocation, holder.map(x -> x.value().toString()).orElse("Missing"));
		}
	}

	private <T> void validate(ResourceKey<Registry<T>> key, @Nullable Validator<T> validator, Map<ResourceLocation, T> entries) {
		WritableRegistry<T> registry = this.get(key);
		entries.forEach((location, t) -> {
			ResourceKey<T> resourceKey = ResourceKey.create(key, location);
			if (validator != null)
				t = validator.validate(resourceKey, t, this);
			if (t != null) {
				if (t instanceof IForgeRegistryEntry<?> re && re.getRegistryName() == null) //This ensures that get registry name is always valid.
					re.setRegistryName(location);
				registry.register(resourceKey, t, Lifecycle.experimental());
			}
		});
	}

	public static boolean isServerContext(RegistryAccess access) {
		return DistExecutor.unsafeRunForDist(() -> () -> ClientHelper.isServerContext(access), () -> () -> true);
	}

	public static CalioDynamicRegistryManager getInstance(RegistryAccess server) {
		if (isServerContext(server))
			return addInstance(server);

		if (clientInstance == null)
			initializeClient();
		return clientInstance;
	}

	private static CalioDynamicRegistryManager addInstance(RegistryAccess server) {
		if (serverInstance == null)
			serverInstance = new CalioDynamicRegistryManager();
		return serverInstance;//INSTANCES.computeIfAbsent(server, s -> new CalioDynamicRegistryManager());
	}

	public static void removeInstance(RegistryAccess server) {
		//INSTANCES.remove(server);
		serverInstance = null;
	}

	@OnlyIn(Dist.CLIENT)
	public static void initializeClient() {
		clientInstance = new CalioDynamicRegistryManager();
	}

	@OnlyIn(Dist.CLIENT)
	public static void setClientInstance(CalioDynamicRegistryManager clientInstance) {
		CalioDynamicRegistryManager.clientInstance = clientInstance;
	}

	public static CalioDynamicRegistryManager decode(FriendlyByteBuf buffer) {
		int registryCount = buffer.readVarInt();
		CalioDynamicRegistryManager manager = new CalioDynamicRegistryManager();
		for (int i = 0; i < registryCount; i++) {
			readRegistry(buffer, manager);
		}
		return manager;
	}

	@SuppressWarnings("unchecked")
	private static <T> void readRegistry(FriendlyByteBuf buffer, CalioDynamicRegistryManager manager) {
		ResourceKey<Registry<T>> key = ResourceKey.createRegistryKey(buffer.readResourceLocation());
		int count = buffer.readVarInt();
		WritableRegistry<T> registry = manager.get(key);
		Codec<T> codec = (Codec<T>) manager.definitions.get(key).codec();
		for (int i = 0; i < count; i++) {
			ResourceKey<T> objectKey = ResourceKey.create(key, buffer.readResourceLocation());
			T decode = buffer.readWithCodec(codec);
			if (decode instanceof IForgeRegistryEntry<?> fre)
				fre.setRegistryName(objectKey.location());
			registry.registerOrOverride(OptionalInt.empty(), objectKey, decode, Lifecycle.stable());
		}
	}

	@Override
	public <T> void add(@NotNull ResourceKey<Registry<T>> key, @Nullable Consumer<BiConsumer<ResourceKey<T>, T>> builtin, Codec<T> codec, @Nullable Supplier<ResourceLocation> defaultValue) {
		Validate.isTrue(!this.lock, "Cannot add registries after the dynamic registry manager has initialized");
		if (this.definitions.containsKey(key))
			throw new IllegalArgumentException("Registry for key " + key + " is already added.");
		RegistryDefinition<T> value = new RegistryDefinition<>(builtin, codec, defaultValue);
		this.definitions.put(key, value);
		this.reset(key);
		this.registries.computeIfAbsent(key, k -> value.newRegistry(key));
		this.locks.put(key, new Object());
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

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <T> WritableRegistry<T> reset(ResourceKey<Registry<T>> key) {
		MappedRegistry mappedRegistry = this.definitions.get(key).newRegistry((ResourceKey) key);
		this.registries.put(key, mappedRegistry);
		return mappedRegistry;
	}

	@Override
	@SuppressWarnings("unchecked")
	public @NotNull <T> WritableRegistry<T> get(@NotNull ResourceKey<Registry<T>> key) {
		MappedRegistry<?> registry = this.registries.get(key);
		if (registry == null)
			throw new IllegalArgumentException("Registry " + key + " was missing.");
		return (MappedRegistry<T>) registry;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<WritableRegistry<T>> getOrEmpty(ResourceKey<Registry<T>> key) {
		return Optional.ofNullable((WritableRegistry<T>) this.registries.get(key));
	}

	@Override
	public <T> T register(ResourceKey<Registry<T>> registry, ResourceKey<T> name, T value) {
		if (!name.isFor(registry))
			throw new IllegalArgumentException("Registry key " + name + " doesn't target registry " + registry + ".");
		return null;
	}

	public <T> Object getLock(ResourceKey<Registry<T>> registry) {
		return this.locks.get(registry);
	}

	public void encode(FriendlyByteBuf buffer) {
		//Size, <Names>
		buffer.writeVarInt(this.registries.size());
		this.registries.forEach((registryKey, objects) -> this.writeRegistry(registryKey, objects, buffer));
	}

	@SuppressWarnings("unchecked")
	public <T> void writeRegistry(ResourceKey<?> key, Registry<T> registry, FriendlyByteBuf buffer) {
		Codec<T> codec = (Codec<T>) this.definitions.get(key).codec();
		buffer.writeResourceLocation(key.location());
		List<Pair<ResourceLocation, T>> entries = new ArrayList<>(registry.size());
		for (ResourceLocation entry : registry.keySet()) { //If holders are missing, using entry would prevent login if a power was missing.
			Optional<Holder<T>> holder = registry.getHolder(ResourceKey.create(registry.key(), entry)).filter(Holder::isBound);
			holder.ifPresent(tHolder -> entries.add(Pair.of(entry, tHolder.value())));
		}
		buffer.writeVarInt(entries.size());
		entries.forEach(entry -> {
			buffer.writeResourceLocation(entry.getKey());
			buffer.writeWithCodec(codec, entry.getValue());
		});
	}

	private record Validator<T>(ResourceKey<Registry<T>> key, DynamicEntryValidator<T> validator, Class<T> eventClass,
								ResourceKey<?>[] dependencies) {
		public T validate(ResourceKey<T> entry, T input, ICalioDynamicRegistryManager manager) {
			DataResult<T> result = this.validator().validate(entry.location(), input, manager);
			if (result.error().isPresent()) {
				CalioAPI.LOGGER.error("Validation for {} failed with error: {}", entry, result.error().get());
				return null;
			} else {
				T t = result.getOrThrow(false, s -> {});
				if (this.eventClass() != null) {
					DynamicRegistrationEvent<T> event = new DynamicRegistrationEvent<>(this.eventClass(), entry.location(), input);
					if (MinecraftForge.EVENT_BUS.post(event)) {
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

	private record ReloadFactory<T>(String directory, DynamicEntryFactory<T> factory) {
		public Map<ResourceLocation, List<JsonElement>> prepare(ResourceManager resourceManager) {
			Map<ResourceLocation, List<JsonElement>> map = Maps.newHashMap();
			int i = this.directory().length() + 1;
			Set<String> resourcesHandled = new HashSet<>();
			for (ResourceLocation identifier : resourceManager.listResources(this.directory(), (stringx) -> stringx.endsWith(".json"))) {
				String string = identifier.getPath();
				ResourceLocation identifier2 = new ResourceLocation(identifier.getNamespace(), string.substring(i, string.length() - FILE_SUFFIX_LENGTH));
				resourcesHandled.clear();
				try {
					resourceManager.getResources(identifier).forEach(resource -> {
						if (!resourcesHandled.contains(resource.getSourceName())) {
							resourcesHandled.add(resource.getSourceName());
							try {
								try (resource) {
									try (InputStream inputStream = resource.getInputStream()) {
										try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
											JsonElement jsonElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
											if (jsonElement != null) {
												if (map.containsKey(identifier2)) {
													map.get(identifier2).add(jsonElement);
												} else {
													List<JsonElement> elementList = new LinkedList<>();
													elementList.add(jsonElement);
													map.put(identifier2, elementList);
												}
											} else {
												CalioAPI.LOGGER.error("Couldn't load data file {} from {} as it's null or empty", identifier2, identifier);
											}
										}
									}
								}
							} catch (IllegalArgumentException | IOException | JsonParseException var68) {
								CalioAPI.LOGGER.error("Couldn't parse data file {} from {}", identifier2, identifier, var68);
							}
						}
					});
				} catch (IOException e) {
					CalioAPI.LOGGER.error("Couldn't parse data file {} from {}", identifier2, identifier, e);
				}
			}

			return map;
		}

		public Map<ResourceLocation, T> reload(Map<ResourceLocation, List<JsonElement>> input) {
			ImmutableMap.Builder<ResourceLocation, T> builder = ImmutableMap.builder();
			input.forEach((location, jsonElements) -> this.factory().create(location, jsonElements).forEach(builder::put));
			return builder.build();
		}
	}

	private record RegistryDefinition<T>(Consumer<BiConsumer<ResourceKey<T>, T>> builtin, Codec<T> codec,
										 @Nullable Supplier<ResourceLocation> defaultValue) {

		public MappedRegistry<T> newRegistry(ResourceKey<Registry<T>> key) {
			//As this is dynamic, there is no certainty as to the content, so we don't have a backing IdentityHashMap.
			ResourceLocation defaultKey = this.defaultValue() != null ? this.defaultValue().get() : null;
			MappedRegistry<T> registry = defaultKey == null ?
					new MappedRegistry<>(key, Lifecycle.experimental(), null) :
					new DefaultedRegistry<>(defaultKey.toString(), key, Lifecycle.experimental(), null);
			if (this.builtin() != null)
				this.builtin().accept((rl, value) -> registry.register(rl, value, Lifecycle.experimental()));
			return registry;
		}
	}
}
