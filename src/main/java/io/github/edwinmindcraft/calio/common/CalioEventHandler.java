package io.github.edwinmindcraft.calio.common;

import io.github.apace100.calio.Calio;
import io.github.apace100.calio.registry.DataObjectRegistry;
import io.github.apace100.calio.resource.OrderedResourceListenerManager;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.ability.IAbilityHolder;
import io.github.edwinmindcraft.calio.common.ability.AbilityHolder;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = CalioAPI.MODID)
public class CalioEventHandler {
	@SubscribeEvent
	public static void onDatapack(OnDatapackSyncEvent event) {
		PacketDistributor.PacketTarget target = event.getPlayer() == null ? PacketDistributor.ALL.noArg() : PacketDistributor.PLAYER.with(event::getPlayer);
		CalioDynamicRegistryManager.getInstance(event.getPlayerList().getServer().registryAccess()).synchronize(target);
		if (event.getPlayer() != null)
			DataObjectRegistry.performAutoSync(event.getPlayer());
	}

	@SubscribeEvent
	public static void onServerReload(AddReloadListenerEvent event) {
		CalioDynamicRegistryManager instance = CalioDynamicRegistryManager.getInstance(event.getServerResources().tagManager.registryAccess);
		event.addListener(instance);
		//OrderedResourceListeners.orderedList().forEach(event::addListener);
		OrderedResourceListenerManager.getInstance().addResources(PackType.SERVER_DATA, event::addListener);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		CalioAPI.LOGGER.info("Removing Dynamic Registries for: " + event.getServer());
		CalioDynamicRegistryManager.removeInstance(event.getServer().registryAccess());
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
		if (Calio.areEntityAttributesAdditional(stack) && stack.hasTag() && Objects.requireNonNull(stack.getTag()).contains("AttributeModifiers", 9))
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
