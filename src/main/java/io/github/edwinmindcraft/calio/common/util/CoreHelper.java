package io.github.edwinmindcraft.calio.common.util;

import io.github.apace100.calio.NbtConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class CoreHelper {
	public static Component removeItalic(ItemStack stack, Component component) {
		if (component instanceof MutableComponent mutableComponent)
			mutableComponent.withStyle(style -> style.withItalic(false));
		return component;
	}

	public static void removeNonItalicFlag(ItemStack stack) {
		CompoundTag display = stack.getTagElement("display");
		if (display != null && display.contains(NbtConstants.NON_ITALIC_NAME))
			display.remove(NbtConstants.NON_ITALIC_NAME);
	}
}
