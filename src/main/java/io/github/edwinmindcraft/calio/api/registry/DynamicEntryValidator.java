package io.github.edwinmindcraft.calio.api.registry;

import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public interface DynamicEntryValidator<T> {
	@NotNull
	DataResult<T> validate(@NotNull ResourceLocation location, @NotNull T input, @NotNull CalioDynamicRegistryManager manager);
}
