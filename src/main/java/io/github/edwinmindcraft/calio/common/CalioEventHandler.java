package io.github.edwinmindcraft.calio.common;

import io.github.apace100.calio.Calio;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.ability.IAbilityHolder;
import io.github.edwinmindcraft.calio.common.ability.AbilityHolder;
import io.github.edwinmindcraft.calio.common.network.CalioNetwork;
import io.github.edwinmindcraft.calio.common.network.packet.S2CDynamicRegistriesPacket;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
import net.minecraftforge.fmlserverevents.FMLServerStoppedEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = CalioAPI.MODID)
public class CalioEventHandler {
	@SubscribeEvent
 	public static void onDatapack(OnDatapackSyncEvent event) {
		PacketDistributor.PacketTarget target = event.getPlayer() == null ? PacketDistributor.ALL.noArg() : PacketDistributor.PLAYER.with(event::getPlayer);
		CalioNetwork.CHANNEL.send(target, new S2CDynamicRegistriesPacket(CalioDynamicRegistryManager.getInstance(event.getPlayerList().getServer())));
	}

	@SubscribeEvent
	public static void onServerReload(AddReloadListenerEvent event) {
		CalioDynamicRegistryManager instance = CalioDynamicRegistryManager.getInstance(ServerLifecycleHooks.getCurrentServer());
		event.addListener(instance);
	}

	@SubscribeEvent
	public static void onServerStopped(FMLServerStoppedEvent event) {
		CalioDynamicRegistryManager.removeInstance(event.getServer());
	}

	@SubscribeEvent
	public static void onCapability(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof Player player)
			event.addCapability(AbilityHolder.ID, new AbilityHolder(player));
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void playerFirstTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			//Trigger removals on HIGH, as most mods will have their modifications on NORMAL
			if (CalioAPI.getAbilityHolder(event.player).map(IAbilityHolder::applyRemovals).orElse(false))
				event.player.onUpdateAbilities();
		}
	}


	@SubscribeEvent(priority = EventPriority.LOW)
	public static void playerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			//Trigger additions on LOW, as most mods will have their modifications on NORMAL
			//This also allows mods that would control everything to have LOWEST to place their overrides.
			if (CalioAPI.getAbilityHolder(event.player).map(IAbilityHolder::applyAdditions).orElse(false))
				event.player.onUpdateAbilities();
		}
	}

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
