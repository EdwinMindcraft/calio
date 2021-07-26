package io.github.apace100.calio.util;

import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.TagContainer;

public class ServerTagManagerGetter implements TagManagerGetter {
	@Override
	public TagContainer get() {
		return SerializationTags.getInstance();
	}
}
