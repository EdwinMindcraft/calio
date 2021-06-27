package dev.experimental.calio.common.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import dev.experimental.calio.api.event.CalioDynamicRegistryEvent;
import dev.experimental.calio.api.registry.ICalioDynamicRegistryManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
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
    private final Map<RegistryKey<?>, SimpleRegistry<?>> registries;
    private final Map<RegistryKey<?>, RegistryDefinition<?>> definitions;

    public CalioDynamicRegistryManager() {
        this.registries = new HashMap<>();
        this.definitions = new HashMap<>();
        CalioDynamicRegistryEvent.INITIALIZE_EVENT.invoker().accept(this);
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

    @Environment(EnvType.CLIENT)
    public static void initializeClient() {
        clientInstance = new CalioDynamicRegistryManager();
    }

    @Environment(EnvType.CLIENT)
    public static void setClientInstance(CalioDynamicRegistryManager clientInstance) {
        CalioDynamicRegistryManager.clientInstance = clientInstance;
    }

    public static CalioDynamicRegistryManager decode(PacketByteBuf buffer) {
        int registryCount = buffer.readVarInt();
        CalioDynamicRegistryManager manager = new CalioDynamicRegistryManager();
        for (int i = 0; i < registryCount; i++) {
            readRegistry(buffer, manager);
        }
        return manager;
    }

    @SuppressWarnings("unchecked")
    private static <T> void readRegistry(PacketByteBuf buffer, CalioDynamicRegistryManager manager) {
        RegistryKey<Registry<T>> key = RegistryKey.ofRegistry(buffer.readIdentifier());
        int count = buffer.readVarInt();
        MutableRegistry<T> registry = manager.get(key);
        Codec<T> codec = (Codec<T>) manager.definitions.get(key).codec();
        for (int i = 0; i < count; i++) {
            RegistryKey<T> objectKey = RegistryKey.of(key, buffer.readIdentifier());
            T decode = buffer.decode(codec);
            registry.add(objectKey, decode, Lifecycle.stable());
        }
    }

    @Override
    public <T> void add(@NotNull RegistryKey<Registry<T>> key, @Nullable Supplier<Registry<T>> builtin, Codec<T> codec) {
        if (definitions.containsKey(key))
            throw new IllegalArgumentException("Registry for key " + key + " is already added.");
        definitions.put(key, new RegistryDefinition<>(builtin, codec));
        this.reset(key);
        this.registries.computeIfAbsent(key, k -> this.definitions.get(key).newRegistry((RegistryKey) key));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> MutableRegistry<T> reset(RegistryKey<Registry<T>> key) {
        this.registries.remove(key);
        return (MutableRegistry<T>) this.registries.put(key, this.definitions.get(key).newRegistry((RegistryKey) key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull <T> MutableRegistry<T> get(@NotNull RegistryKey<Registry<T>> key) {
        SimpleRegistry<?> registry = this.registries.get(key);
        if (registry == null)
            throw new IllegalArgumentException("Registry " + key + " was missing.");
        return (SimpleRegistry<T>) registry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<MutableRegistry<T>> getOrEmpty(RegistryKey<Registry<T>> key) {
        return Optional.ofNullable((MutableRegistry<T>) this.registries.get(key));
    }

    @Override
    public <T> T register(RegistryKey<Registry<T>> registry, RegistryKey<T> name, T value) {
        if (!name.isOf(registry))
            throw new IllegalArgumentException("Registry key " + name + " doesn't target registry " + registry + ".");
        return null;
    }

    public void encode(PacketByteBuf buffer) {
        //Size, <Names>
        buffer.writeVarInt(this.registries.size());
        this.registries.forEach((registryKey, objects) -> writeRegistry(registryKey, objects, buffer));
    }

    @SuppressWarnings("unchecked")
    private <T> void writeRegistry(RegistryKey<?> key, Registry<T> registry, PacketByteBuf buffer) {
        Codec<T> codec = (Codec<T>) this.definitions.get(key).codec();
        buffer.writeIdentifier(key.getValue());
        buffer.writeVarInt(registry.getEntries().size());
        registry.getEntries().forEach(entry -> {
            buffer.writeIdentifier(entry.getKey().getValue());
            buffer.encode(codec, entry.getValue());
        });
    }

    private record RegistryDefinition<T>(Supplier<Registry<T>> builtin, Codec<T> codec) {

        public SimpleRegistry<T> newRegistry(RegistryKey<Registry<T>> key) {
            SimpleRegistry<T> registry = new SimpleRegistry<>(key, Lifecycle.experimental());
            if (builtin != null)
                builtin.get().getEntries().forEach(entry -> registry.add(entry.getKey(), entry.getValue(), Lifecycle.experimental()));
            return registry;
        }
    }
}
