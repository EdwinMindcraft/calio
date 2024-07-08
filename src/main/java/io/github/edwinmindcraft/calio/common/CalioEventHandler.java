package io.github.edwinmindcraft.calio.common;

import io.github.apace100.calio.Calio;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.ability.AbilityHolder;
import io.github.edwinmindcraft.calio.api.registry.PlayerAbilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;

import java.util.List;

@EventBusSubscriber(modid = CalioAPI.MODID)
public class CalioEventHandler {
	@SubscribeEvent
	public static void createNewRegistries(NewRegistryEvent event) {
		event.register(PlayerAbilities.REGISTRY);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void playerFirstTick(EntityTickEvent.Pre event) {
		if (event.getEntity() instanceof Player player) {
			//Trigger removals on HIGH, as most mods will have their modifications on NORMAL
			if (CalioAPI.getAbilityHolder(player) != null && CalioAPI.getAbilityHolder(player).applyRemovals())
				player.onUpdateAbilities();
		}
	}


	@SubscribeEvent(priority = EventPriority.LOW)
	public static void playerTick(EntityTickEvent.Pre event) {
		//Trigger additions on LOW, as most mods will have their modifications on NORMAL
		//This also allows mods that would control everything to have LOWEST to place their overrides.
		if (event.getEntity() instanceof Player player) {
			AbilityHolder abilities = CalioAPI.getAbilityHolder(player);
			if (abilities != null && abilities.applyAdditions())
				player.onUpdateAbilities();
		}
	}

	@SubscribeEvent
	public static void updateAttributes(ItemAttributeModifierEvent event) {
		ItemStack stack = event.getItemStack();
		if (Calio.areEntityAttributesAdditional(stack) && stack.getComponentsPatch().get(DataComponents.ATTRIBUTE_MODIFIERS) != null && stack.getComponentsPatch().get(DataComponents.ATTRIBUTE_MODIFIERS).isPresent() && stack.getItem().components().has(DataComponents.ATTRIBUTE_MODIFIERS))
			event.getModifiers().addAll(stack.getItem().components().get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers());
	}

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		List<Component> toolTip = event.getToolTip();
		ItemStack itemStack = event.getItemStack();
		if (toolTip.isEmpty() || !Calio.hasNonItalicName(itemStack) || !itemStack.has(DataComponents.CUSTOM_NAME))
			return;
		Component name = toolTip.get(0);
		if (name instanceof MutableComponent mName)
			mName.withStyle(style -> style.withItalic(false));
		if (!event.getFlags().isAdvanced() && itemStack.is(Items.FILLED_MAP) && itemStack.has(DataComponents.MAP_ID)) {
			int integer = itemStack.get(DataComponents.MAP_ID).id();
            toolTip.add(1, (Component.literal("#" + integer)).withStyle(ChatFormatting.GRAY));
		}
	}
}
