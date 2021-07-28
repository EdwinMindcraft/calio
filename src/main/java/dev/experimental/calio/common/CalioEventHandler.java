package dev.experimental.calio.common;

import dev.experimental.calio.api.CalioAPI;
import io.github.apace100.calio.Calio;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = CalioAPI.MODID)
public class CalioEventHandler {

	@SubscribeEvent
	public static void updateAttributes(ItemAttributeModifierEvent event) {
		ItemStack stack = event.getItemStack();
		if (Calio.areEntityAttributesAdditional(stack) && stack.hasTag() && stack.getTag().contains("AttributeModifiers", 9))
			event.getModifiers().putAll(stack.getItem().getAttributeModifiers(event.getSlotType(), stack));
	}

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		List<Component> toolTip = event.getToolTip();
		ItemStack itemStack = event.getItemStack();
		if (toolTip.isEmpty() || !Calio.hasNonItalicName(itemStack) || !itemStack.hasCustomHoverName())
			return;
		Component name = toolTip.get(0);
		if (name instanceof MutableComponent mName)
			mName.withStyle(style -> style.withItalic(false));
		if (!event.getFlags().isAdvanced() && itemStack.is(Items.FILLED_MAP)) {
			Integer integer = MapItem.getMapId(itemStack);
			if (integer != null)
				toolTip.add(1, (new TextComponent("#" + integer)).withStyle(ChatFormatting.GRAY));
		}
	}
}
