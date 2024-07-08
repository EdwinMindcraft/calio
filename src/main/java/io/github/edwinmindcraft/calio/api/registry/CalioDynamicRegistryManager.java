package io.github.edwinmindcraft.calio.api.registry;

import com.mojang.serialization.Codec;
import io.github.apace100.calio.data.MultiJsonDataLoader;
import io.github.edwinmindcraft.calio.api.event.DynamicRegistrationEvent;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface CalioDynamicRegistryManager {
	/**
	 * Creates a new dynamic registry for the given key and copies values given
	 * to the {@link BiConsumer} if it exists.
	 *
	 * @param key          The key of this dynamic registry.
	 * @param builtin      If this field is not null, objects supplied to the {@link BiConsumer}
	 *                     will be copied into the newly created dynamic registry.
	 * @param codec        The codec used to send the data from the client to the server.
	 * @param defaultValue The default value of the instantiated registries.
	 */
	<T> void add(@NotNull ResourceKey<Registry<T>> key, @Nullable Consumer<BiConsumer<ResourceKey<T>, T>> builtin, Codec<T> codec, @Nullable Supplier<ResourceLocation> defaultValue);

	/**
	 * Creates a new dynamic registry for the given key and copies values given
	 * to the {@link BiConsumer} if it exists.
	 *
	 * @param key     The key of this dynamic registry.
	 * @param builtin If this field is not null, objects supplied to the {@link BiConsumer}
	 *                will be copied into the newly created dynamic registry.
	 * @param codec   The codec used to send the data from the client to the server.
	 */
	default <T> void add(@NotNull ResourceKey<Registry<T>> key, @Nullable Consumer<BiConsumer<ResourceKey<T>, T>> builtin, Codec<T> codec) {
		this.add(key, builtin, codec, null);
	}

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
	 * @param key        The key of the registry to validate entries for.
	 * @param validator  The validator used.
	 * @param eventClass The class used forge the {@link DynamicRegistrationEvent}. If null, the event won't fire.
	 * @param after      The registries that are required to exist before this validator runs.
	 */
	<T> void addValidation(ResourceKey<Registry<T>> key, DynamicEntryValidator<T> validator, @Nullable Class<T> eventClass, @NotNull ResourceKey<?>... after);

	/**
	 * Adds a validation step to the created entries.
	 *
	 * @param key       The key of the registry to validate entries for.
	 * @param validator The validator used.
	 * @param after     The registries that are required to exist before this validator runs.
	 */
	default <T> void addValidation(ResourceKey<Registry<T>> key, DynamicEntryValidator<T> validator, @NotNull ResourceKey<?>... after) {
		this.addValidation(key, validator, null, after);
	}
}
