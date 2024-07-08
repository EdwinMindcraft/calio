package io.github.edwinmindcraft.calio.api.registry;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public interface DynamicRegistryListener {
	default void whenAvailable(@NotNull CalioDynamicRegistryManager manager) {}

	default void whenNamed(@NotNull ResourceLocation name) {}
}
