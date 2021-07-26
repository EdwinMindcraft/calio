package dev.experimental.calio.common;

import dev.experimental.calio.api.CalioAPI;
import io.github.apace100.calio.Calio;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CalioAPI.MODID)
public class CalioEventHandler {

	@SubscribeEvent
	public static void updateAttributes(ItemAttributeModifierEvent event) {
		ItemStack stack = event.getItemStack();
		if (Calio.areEntityAttributesAdditional(stack) && stack.hasTag() && stack.getTag().contains("AttributeModifiers", 9))
			event.getModifiers().putAll(stack.getItem().getAttributeModifiers(event.getSlotType(), stack));
	}
}
