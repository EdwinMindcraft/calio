package io.github.apace100.calio.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.apace100.calio.Calio;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.io.FilenameUtils;
import org.quiltmc.parsers.json.JsonFormat;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.gson.GsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 *  Similar to {@link net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener}, except it supports the JSON5 and JSONC spec.
 */
public abstract class ExtendedJsonDataLoader extends ExtendedSinglePreparationResourceReloader<Map<ResourceLocation, JsonElement>> implements IExtendedJsonDataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedJsonDataLoader.class);

    private final Gson gson;
    protected final String directoryName;

    public ExtendedJsonDataLoader(Gson gson, String directoryName) {
        this.gson = gson;
        this.directoryName = directoryName;
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {

        Map<ResourceLocation, JsonElement> result = new HashMap<>();
        manager.listResources(directoryName, this::hasValidFormat).forEach((fileId, resource) -> {

            ResourceLocation resourceId = this.trim(fileId, directoryName);
            String fileExtension = "." + FilenameUtils.getExtension(fileId.getPath());

            JsonFormat jsonFormat = this.getValidFormats().get(fileExtension);
            String packName = resource.sourcePackId();

            try (Reader resourceReader = resource.openAsReader()) {

                GsonReader gsonReader = new GsonReader(JsonReader.create(resourceReader, jsonFormat));
                JsonElement jsonElement = gson.fromJson(gsonReader, JsonElement.class);

                if (jsonElement == null) {
                    throw new JsonParseException("JSON cannot be empty!");
                }

                if (result.put(resourceId, jsonElement) != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + resourceId);
                }

            } catch (Exception e) {
                this.onError(packName, resourceId, fileExtension, e);
            }

        });

        return result;

    }

    @Override
    protected void preApply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {

        var preparedEntryIterator = prepared.entrySet().iterator();
        while (preparedEntryIterator.hasNext()) {

            var preparedEntry = preparedEntryIterator.next();

            ResourceLocation id = preparedEntry.getKey();
            JsonElement jsonElement = preparedEntry.getValue();

            // FIXME: Use NeoForge conditions.
            if (!(jsonElement instanceof JsonObject jsonObject) || ResourceConditionsImpl.applyResourceConditions(jsonObject, directoryName, id, Calio.DYNAMIC_REGISTRIES.get())) {
                continue;
            }

            this.onReject("", id);
            preparedEntryIterator.remove();

        }

    }

    @Override
    public void onError(String packName, ResourceLocation resourceId, String fileExtension, Exception exception) {
        String filePath = packName + "/.../" + resourceId.getNamespace() + "/" + directoryName + "/" + resourceId.getPath() + fileExtension;
        LOGGER.error("Couldn't parse data file \"{}\" from \"{}\":", resourceId, filePath, exception);
    }

}
