package io.github.apace100.calio.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 *  <p>Like {@link net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener}, except it provides a list of {@link JsonElement JsonElements} associated
 *  with an {@link ResourceLocation}, where each element is loaded by different resource packs. This allows for overriding and merging several
 *  data files into one, similar to how tags work. There is no guarantee on the order of the resulting list, so make sure to implement
 *  some kind of "priority" system.</p>
 *
 *  <p>This is <b>deprecated</b> in favor of using {@link IdentifiableMultiJsonDataLoader}.</p>
 */
@Deprecated
public abstract class MultiJsonDataLoader extends SimplePreparableReloadListener<Map<ResourceLocation, List<JsonElement>>> {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final int FILE_SUFFIX_LENGTH = ".json".length();
	private final Gson gson;
	private final String dataType;

	public MultiJsonDataLoader(Gson gson, String dataType) {
		this.gson = gson;
		this.dataType = dataType;
	}

	protected @NotNull Map<ResourceLocation, List<JsonElement>> prepare(ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
		Map<ResourceLocation, List<JsonElement>> map = Maps.newHashMap();
		int i = this.dataType.length() + 1;
		Set<String> resourcesHandled = new HashSet<>();
		for (Map.Entry<ResourceLocation, List<Resource>> entry : resourceManager.listResourceStacks(this.dataType, (stringx) -> stringx.getPath().endsWith(".json")).entrySet()) {
			ResourceLocation identifier = entry.getKey();
			String string = identifier.getPath();
			ResourceLocation identifier2 = new ResourceLocation(identifier.getNamespace(), string.substring(i, string.length() - FILE_SUFFIX_LENGTH));
			resourcesHandled.clear();
			for (Resource resource : entry.getValue()) {
				if (!resourcesHandled.contains(resource.sourcePackId())) {
					resourcesHandled.add(resource.sourcePackId());
					try {
						try (Reader reader = resource.openAsReader()) {
							JsonElement jsonElement = GsonHelper.fromNullableJson(this.gson, reader, JsonElement.class, false);
							if (jsonElement != null) {
								if (map.containsKey(identifier2)) {
									map.get(identifier2).add(jsonElement);
								} else {
									List<JsonElement> elementList = new LinkedList<>();
									elementList.add(jsonElement);
									map.put(identifier2, elementList);
								}
							} else {
								LOGGER.error("Couldn't load data file {} from {} as it's null or empty", identifier2, identifier);
							}
						}
					} catch (IllegalArgumentException | IOException | JsonParseException e) {
						LOGGER.error("Couldn't parse data file {} from {}", identifier2, identifier, e);
					}
				}
			}
		}

		return map;
	}
}
