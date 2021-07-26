package dev.experimental.calio.common.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import dev.experimental.calio.api.event.CalioDynamicRegistryEvent;
import dev.experimental.calio.api.registry.ICalioDynamicRegistryManager;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * These registries do not have a fixed state, and are designed to be added to via datapacks.
 */
public class CalioDynamicRegistryManager implements ICalioDynamicRegistryManager {

	private static final Map<MinecraftServer, CalioDynamicRegistryManager> INSTANCES = new ConcurrentHashMap<>();
	private static CalioDynamicRegistryManager clientInstance = null;
	private final Map<ResourceKey<?>, MappedRegistry<?>> registries;
	private final Map<ResourceKey<?>, RegistryDefinition<?>> definitions;

	public CalioDynamicRegistryManager() {
		this.registries = new HashMap<>();
		this.definitions = new HashMap<>();
		MinecraftForge.EVENT_BUS.register(new CalioDynamicRegistryEvent(this));
	}

	public static CalioDynamicRegistryManager getInstance(MinecraftServer server) {
		if (server == null) return clientInstance;
		return INSTANCES.get(server);
	}

	public static void addInstance(MinecraftServer server) {
		INSTANCES.computeIfAbsent(server, s -> new CalioDynamicRegistryManager());
	}

	public static void removeInstance(MinecraftServer server) {
		INSTANCES.remove(server);
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
			registry.register(objectKey, decode, Lifecycle.stable());
		}
	}

	@Override
	public <T> void add(@NotNull ResourceKey<Registry<T>> key, @Nullable Supplier<Registry<T>> builtin, Codec<T> codec) {
		if (this.definitions.containsKey(key))
			throw new IllegalArgumentException("Registry for key " + key + " is already added.");
		this.definitions.put(key, new RegistryDefinition<>(builtin, codec));
		this.reset(key);
		this.registries.computeIfAbsent(key, k -> this.definitions.get(key).newRegistry((ResourceKey) key));
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <T> WritableRegistry<T> reset(ResourceKey<Registry<T>> key) {
		this.registries.remove(key);
		return (WritableRegistry<T>) this.registries.put(key, this.definitions.get(key).newRegistry((ResourceKey) key));
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

	public void encode(FriendlyByteBuf buffer) {
		//Size, <Names>
		buffer.writeVarInt(this.registries.size());
		this.registries.forEach((registryKey, objects) -> this.writeRegistry(registryKey, objects, buffer));
	}

	@SuppressWarnings("unchecked")
	private <T> void writeRegistry(ResourceKey<?> key, Registry<T> registry, FriendlyByteBuf buffer) {
		Codec<T> codec = (Codec<T>) this.definitions.get(key).codec();
		buffer.writeResourceLocation(key.location());
		buffer.writeVarInt(registry.entrySet().size());
		registry.entrySet().forEach(entry -> {
			buffer.writeResourceLocation(entry.getKey().location());
			buffer.writeWithCodec(codec, entry.getValue());
		});
	}

	private record RegistryDefinition<T>(Supplier<Registry<T>> builtin, Codec<T> codec) {

		public MappedRegistry<T> newRegistry(ResourceKey<Registry<T>> key) {
			MappedRegistry<T> registry = new MappedRegistry<>(key, Lifecycle.experimental());
			if (this.builtin != null)
				this.builtin.get().entrySet().forEach(entry -> registry.register(entry.getKey(), entry.getValue(), Lifecycle.experimental()));
			return registry;
		}
	}
}
