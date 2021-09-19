package io.github.edwinmindcraft.calio.api.registry;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@FunctionalInterface
public interface DynamicEntryFactory<T> {
	T accept(ResourceLocation location, List<JsonElement> entries);
}
