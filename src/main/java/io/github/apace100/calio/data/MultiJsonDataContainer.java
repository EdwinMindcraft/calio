package io.github.apace100.calio.data;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MultiJsonDataContainer extends LinkedHashMap<String, Map<ResourceLocation, List<JsonElement>>> {

    public void forEach(Processor processor) {
        this.forEach((packName, idAndData) ->
            idAndData.forEach((id, data) ->
                data.forEach(jsonElement -> processor.process(packName, id, jsonElement))));
    }

    @FunctionalInterface
    public interface Processor {
        void process(String packName, ResourceLocation id, JsonElement jsonElement);
    }

}
