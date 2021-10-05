package io.github.edwinmindcraft.calio.api.registry;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface DynamicEntryFactory<T> {
	@Nullable
	T accept(@NotNull ResourceLocation location, @NotNull List<JsonElement> entries);

	@NotNull
	default Map<ResourceLocation, T> create(ResourceLocation location, @NotNull List<JsonElement> entries) {
		T accept = this.accept(location, entries);
		if (accept != null)
			return ImmutableMap.of(location, accept);
		return ImmutableMap.of();
	}
}
