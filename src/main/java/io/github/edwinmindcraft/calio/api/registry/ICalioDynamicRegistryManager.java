package io.github.edwinmindcraft.calio.api.registry;

import com.mojang.serialization.Codec;
import io.github.apace100.calio.data.MultiJsonDataLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ICalioDynamicRegistryManager extends PreparableReloadListener {
	/**
	 * Creates a new dynamic registry for the given key and copies values from
	 * a builtin registry.
	 *
	 * @param key     The key of this dynamic registry.
	 * @param builtin Objects in this registry will be copied into the newly created
	 *                dynamic registry.
	 * @param codec   The codec used to send the data from the client to the server.
	 */
	default <T> void addVanilla(@NotNull ResourceKey<Registry<T>> key, @NotNull Supplier<Registry<T>> builtin, Codec<T> codec) {
		Validate.notNull(builtin, "Registry " + key.location() + " has no builtin, use add instead.");
		this.add(key, consumer -> builtin.get().entrySet().forEach(entry -> consumer.accept(entry.getKey(), entry.getValue())), codec);
	}

	/**
	 * Creates a new dynamic registry for the given key and copies values from
	 * a builtin registry.
	 *
	 * @param key     The key of this dynamic registry.
	 * @param builtin Objects in this registry will be copied into the newly created
	 *                dynamic registry.
	 * @param codec   The codec used to send the data from the client to the server.
	 */
	default <T extends IForgeRegistryEntry<T>> void addForge(@NotNull ResourceKey<Registry<T>> key, @NotNull Supplier<IForgeRegistry<T>> builtin, Codec<T> codec) {
		Validate.notNull(builtin, "Registry " + key.location() + " has no builtin, use add instead.");
		this.add(key, consumer -> builtin.get().getEntries().forEach(entry -> consumer.accept(entry.getKey(), entry.getValue())), codec);
	}

	/**
	 * Creates a new dynamic registry for the given key and copies values given
	 * to the {@link BiConsumer} if it exists.
	 *
	 * @param key     The key of this dynamic registry.
	 * @param builtin If this field is not null, objects supplied to the {@link BiConsumer}
	 *                will be copied into the newly created dynamic registry.
	 * @param codec   The codec used to send the data from the client to the server.
	 */
	<T> void add(@NotNull ResourceKey<Registry<T>> key, @Nullable Consumer<BiConsumer<ResourceKey<T>, T>> builtin, Codec<T> codec);

	/**
	 * Creates a new dynamic registry for the given key.
	 *
	 * @param key   The key of this dynamic registry.
	 * @param codec The codec used to send the data from the client to the server.
	 */
	default <T> void add(@NotNull ResourceKey<Registry<T>> key, Codec<T> codec) {
		this.add(key, null, codec);
	}

	/**
	 * Adds a new reload listener in the style of a {@link MultiJsonDataLoader}.
	 *
	 * @param key       The key of the registry this creates entries for.
	 * @param directory The directory in which json files will be loaded.
	 * @param factory   The factory used to create the entry from the json files.
	 */
	<T> void addReload(ResourceKey<Registry<T>> key, String directory, DynamicEntryFactory<T> factory);

	/**
	 * Adds a validation step to the created entries.
	 *
	 * @param key       The key of the registry to validate entries for.
	 * @param validator The validator used.
	 * @param after     The registries that are required to exist before this validator runs.
	 */
	<T> void addValidation(ResourceKey<Registry<T>> key, DynamicEntryValidator<T> validator, @NotNull ResourceKey<?>... after);

	/**
	 * Resets the given registry, creating a new one from the input data.
	 *
	 * @param key The registry to reset.
	 *
	 * @return The new registry.
	 */
	<T> WritableRegistry<T> reset(ResourceKey<Registry<T>> key);

	/**
	 * Returns the mutable registry corresponding to the given key.
	 *
	 * @param key The key of the registry.
	 *
	 * @return The registry associated with the given key.
	 *
	 * @throws IllegalArgumentException if no registry matches the given key.
	 * @see #getOrEmpty(ResourceKey) for the null-safe version.
	 */
	@NotNull <T> WritableRegistry<T> get(@NotNull ResourceKey<Registry<T>> key);

	/**
	 * Returns an optional containing the mutable registry corresponding to the
	 * key, or {@link Optional#empty()} if none are found.
	 *
	 * @param key The key of the registry.
	 *
	 * @return The registry associated with the given key.
	 */
	<T> Optional<WritableRegistry<T>> getOrEmpty(ResourceKey<Registry<T>> key);

	/**
	 * Registers the given item in the given registry.
	 *
	 * @param registry The key of the registry to register the item into.
	 * @param name     The name of the item being registered.
	 * @param value    The item begin registered
	 *
	 * @return The registered item.
	 */
	<T> T register(ResourceKey<Registry<T>> registry, ResourceKey<T> name, T value);
}
