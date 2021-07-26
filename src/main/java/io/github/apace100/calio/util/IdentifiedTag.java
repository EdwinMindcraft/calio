package io.github.apace100.calio.util;

import io.github.apace100.calio.Calio;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IdentifiedTag<T> implements Tag.Named<T> {

	private final ResourceKey<? extends Registry<T>> registryKey;
	private final ResourceLocation id;
	private Tag<T> containedTag;

	public IdentifiedTag(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation identifier) {
		this.registryKey = registryKey;
		this.id = identifier;
	}

	private void updateContainedTag() {
		this.containedTag = Calio.getTagManager().getTagOrThrow(this.registryKey, this.id, id -> new RuntimeException("Could not load tag: " + id.toString()));
	}

	@Override
	@NotNull
	public ResourceLocation getName() {
		return this.id;
	}

	@Override
	@NotNull
	public List<T> getValues() {
		if (this.containedTag == null) {
			this.updateContainedTag();
		}
		return this.containedTag.getValues();
	}

	@Override
	public boolean contains(@NotNull T entry) {
		if (this.containedTag == null) {
			this.updateContainedTag();
		}
		return this.containedTag.contains(entry);
	}
}
