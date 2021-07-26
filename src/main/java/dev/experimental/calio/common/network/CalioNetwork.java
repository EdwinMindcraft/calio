package dev.experimental.calio.common.network;

import dev.experimental.calio.api.CalioAPI;
import dev.experimental.calio.common.network.packet.C2SAcknowledgePacket;
import dev.experimental.calio.common.network.packet.C2SShareItemPacket;
import dev.experimental.calio.common.network.packet.S2CDynamicRegistriesPacket;
import net.minecraftforge.fmllegacy.network.NetworkDirection;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;

public class CalioNetwork {
	public static final String NETWORK_VERSION = "1.0.0";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CalioAPI.resource("channel"), () -> NETWORK_VERSION, NETWORK_VERSION::equals, NETWORK_VERSION::equals);

	public static void register() {
		int index = 0;
		//Login
		CHANNEL.messageBuilder(C2SAcknowledgePacket.class, index++, NetworkDirection.LOGIN_TO_SERVER)
				.decoder(C2SAcknowledgePacket::decode).encoder(C2SAcknowledgePacket::encode)
				.consumer(C2SAcknowledgePacket::handle)
				.loginIndex(C2SAcknowledgePacket::getLoginIndex, C2SAcknowledgePacket::setLoginIndex)
				.add();
		CHANNEL.messageBuilder(S2CDynamicRegistriesPacket.class, index++, NetworkDirection.LOGIN_TO_CLIENT)
				.decoder(S2CDynamicRegistriesPacket::decode).encoder(S2CDynamicRegistriesPacket::encode)
				.consumer(S2CDynamicRegistriesPacket::handle)
				.loginIndex(S2CDynamicRegistriesPacket::getLoginIndex, S2CDynamicRegistriesPacket::setLoginIndex)
				.markAsLoginPacket().add();

		//Play
		CHANNEL.messageBuilder(C2SShareItemPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
				.decoder(C2SShareItemPacket::decode).encoder(C2SShareItemPacket::encode)
				.consumer(C2SShareItemPacket::handle).add();
		CalioAPI.LOGGER.debug("Registered {} packets", index);
	}
}
