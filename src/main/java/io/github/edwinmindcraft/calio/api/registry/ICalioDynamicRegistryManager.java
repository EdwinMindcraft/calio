package io.github.edwinmindcraft.calio.api.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public interface ICalioDynamicRegistryManager {
	/**
	 * Creates a new dynamic registry for the given key and copies values from
	 * a builtin registry if it exists.
	 *
	 * @param key     The key of this dynamic registry.
	 * @param builtin If this field is not null, objects in this registry will
	 *                be copied into the newly created dynamic registry.
	 * @param codec   The codec used to send the data from the client to the server.
	 */
	<T> void add(@NotNull ResourceKey<Registry<T>> key, @Nullable Supplier<Registry<T>> builtin, Codec<T> codec);

	/**
	 * Resets the given registry, creating a new one from the input data.
	 *
	 * @param key The registry to reset.
	 * @return The new registry.
	 */
	<T> WritableRegistry<T> reset(ResourceKey<Registry<T>> key);

	/**
	 * Returns the mutable registry corresponding to the given key.
	 *
	 * @param key The key of the registry.
	 * @return The registry associated with the given key.
	 * @throws IllegalArgumentException if no registry matches the given key.
	 * @see #getOrEmpty(ResourceKey) for the null-safe version.
	 */
	@NotNull <T> WritableRegistry<T> get(@NotNull ResourceKey<Registry<T>> key);

	/**
	 * Returns an optional containing the mutable registry corresponding to the
	 * key, or {@link Optional#empty()} if none are found.
	 *
	 * @param key The key of the registry.
	 * @return The registry associated with the given key.
	 */
	<T> Optional<WritableRegistry<T>> getOrEmpty(ResourceKey<Registry<T>> key);

	/**
	 * Registers the given item in the given registry.
	 *
	 * @param registry The key of the registry to register the item into.
	 * @param name     The name of the item being registered.
	 * @param value    The item begin registered
	 * @return The registered item.
	 */
	<T> T register(ResourceKey<Registry<T>> registry, ResourceKey<T> name, T value);
}
