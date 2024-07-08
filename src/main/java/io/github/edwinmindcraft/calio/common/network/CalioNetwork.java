package io.github.edwinmindcraft.calio.common.network;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.network.packet.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = CalioAPI.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CalioNetwork {
	public static final String NETWORK_VERSION = "2.0";

	@SubscribeEvent
	public static void register(RegisterPayloadHandlersEvent event) {
		event.registrar(CalioAPI.MODID)
				.versioned(NETWORK_VERSION)
				.configurationToServer(C2SAcknowledgePacket.TYPE, C2SAcknowledgePacket.STREAM_CODEC, C2SAcknowledgePacket::handle)
				.playToClient(S2CDataObjectRegistryPacket.TYPE, S2CDataObjectRegistryPacket.STREAM_CODEC, S2CDataObjectRegistryPacket::handle);
	}
}
