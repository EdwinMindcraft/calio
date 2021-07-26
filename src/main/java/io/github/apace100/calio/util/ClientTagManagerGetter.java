package io.github.apace100.calio.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.TagContainer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientTagManagerGetter implements TagManagerGetter {
	@Override
	public TagContainer get() {
		ClientPacketListener networkHandler = Minecraft.getInstance().getConnection();
		if (networkHandler != null)
			return networkHandler.getTags();
		return SerializationTags.getInstance();
	}
}
