package io.github.edwinmindcraft.calio.client;

import io.github.apace100.calio.resource.OrderedResourceListenerManager;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManagerImpl;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CalioAPI.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CalioClientEventHandler {
	@SubscribeEvent
	public void addClientResources(RegisterClientReloadListenersEvent event) {
		OrderedResourceListenerManager.getInstance().addResources(PackType.CLIENT_RESOURCES, event::registerReloadListener);
	}

	@SubscribeEvent
	public void onDisconnecting(ClientPlayerNetworkEvent.LoggingOut event) {
		CalioDynamicRegistryManagerImpl.removeClientInstance();
	}
}
