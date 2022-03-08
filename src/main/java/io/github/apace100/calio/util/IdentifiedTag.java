package io.github.apace100.calio.util;

import com.google.common.collect.Lists;
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
        try {
		    this.containedTag = Calio.getTagManager().getTagOrThrow(this.registryKey, this.id, id -> new RuntimeException("Could not load tag: " + id.toString()));
        } catch (RuntimeException e) {
            // Fail silently. This sometimes happens one frame at world load.
        }
    }

	@Override
	@NotNull
	public ResourceLocation getName() {
		return this.id;
	}

    @Override
    public boolean contains(T entry) {
        if(containedTag == null) {
            updateContainedTag();
            if(containedTag == null) {
                return false;
            }
        }
        return this.containedTag.contains(entry);
    }

    @Override
    public List<T> values() {
        if(containedTag == null) {
            updateContainedTag();
            //ImmutableList.of() doesn't allocate a new list.
            if(this.containedTag == null)
                return ImmutableList.of();
        }
        return this.containedTag.getValues();
    }
}
