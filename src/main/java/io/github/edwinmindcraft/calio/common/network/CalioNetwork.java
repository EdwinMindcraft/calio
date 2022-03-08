package io.github.edwinmindcraft.calio.common.network;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.network.packet.C2SAcknowledgePacket;
import io.github.edwinmindcraft.calio.common.network.packet.C2SShareItemPacket;
import io.github.edwinmindcraft.calio.common.network.packet.S2CDataObjectRegistryPacket;
import io.github.edwinmindcraft.calio.common.network.packet.S2CDynamicRegistriesPacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class CalioNetwork {
	public static final String NETWORK_VERSION = "1.0";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CalioAPI.resource("channel"), () -> NETWORK_VERSION, NETWORK_VERSION::equals, NETWORK_VERSION::equals);

	public static void register() {
		int index = 0;
		//Login
		CHANNEL.messageBuilder(C2SAcknowledgePacket.class, index++, NetworkDirection.LOGIN_TO_SERVER)
				.decoder(C2SAcknowledgePacket::decode).encoder(C2SAcknowledgePacket::encode)
				.consumer(C2SAcknowledgePacket::handle)
				.loginIndex(C2SAcknowledgePacket::getLoginIndex, C2SAcknowledgePacket::setLoginIndex)
				.add();
		//Play
		CHANNEL.messageBuilder(S2CDynamicRegistriesPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
				.decoder(S2CDynamicRegistriesPacket::decode).encoder(S2CDynamicRegistriesPacket::encode)
				.consumer(S2CDynamicRegistriesPacket::handle).add();
		CHANNEL.messageBuilder(S2CDataObjectRegistryPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
				.decoder(S2CDataObjectRegistryPacket::decode).encoder(S2CDataObjectRegistryPacket::encode)
				.consumer(S2CDataObjectRegistryPacket::handle).add();
		CHANNEL.messageBuilder(C2SShareItemPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
				.decoder(C2SShareItemPacket::decode).encoder(C2SShareItemPacket::encode)
				.consumer(C2SShareItemPacket::handle).add();
		CalioAPI.LOGGER.debug("Registered {} packets", index);
	}
}
