package io.github.edwinmindcraft.calio.api.registry;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DynamicRegistryListener {
	default void whenAvailable(@NotNull ICalioDynamicRegistryManager manager) {}

	default void whenNamed(@NotNull ResourceLocation name) {}
}
