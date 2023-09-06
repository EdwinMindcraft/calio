package io.github.apace100.calio.data;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class JsonDataProvider<T> implements DataProvider {

	protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	protected final String modid;
	protected final String folder;
	protected final DataGenerator generator;
	protected final ExistingFileHelper existingFileHelper;
	protected final Map<ResourceLocation, T> objects = new HashMap<>();
	private final PackType resourceType;

	protected JsonDataProvider(DataGenerator generator, String modid, ExistingFileHelper existingFileHelper, String folder, PackType resourceType) {
		this.modid = modid;
		this.folder = folder;
		this.generator = generator;
		this.existingFileHelper = existingFileHelper;
		this.resourceType = resourceType;
	}

	protected abstract void populate();

	protected void validate() {}

	protected abstract JsonElement asJson(T input);

	public void add(ResourceLocation location, T input) {
		this.objects.put(location, input);
	}

	public void add(String name, T input) {
		this.add(new ResourceLocation(this.modid, name), input);
	}

	@Override
	public CompletableFuture<?> run(@NotNull CachedOutput cache) {
		this.populate();
		this.validate();
        List<CompletableFuture<?>> list = Lists.newArrayList();

		for (Map.Entry<ResourceLocation, T> entry : this.objects.entrySet()) {
			list.add(CompletableFuture.supplyAsync(() ->
                            DataProvider.saveStable(cache, this.asJson(entry.getValue()), this.getPath(entry.getKey())),
                    Util.backgroundExecutor()).thenCompose((p_253441_) -> p_253441_));
		}
        return Util.sequenceFailFast(list);
    }

	protected Path getPath(ResourceLocation entry) {
		return this.generator.getPackOutput().getOutputFolder().resolve(Paths.get(this.resourceType.getDirectory(), entry.getNamespace(), this.folder, entry.getPath() + ".json"));
	}
}
