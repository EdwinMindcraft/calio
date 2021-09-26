package io.github.edwinmindcraft.calio.api.registry;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@FunctionalInterface
public interface DynamicEntryFactory<T> {
	@Nullable
	T accept(@NotNull ResourceLocation location, @NotNull List<JsonElement> entries);
}
