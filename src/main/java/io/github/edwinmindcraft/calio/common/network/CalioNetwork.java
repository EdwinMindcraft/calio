package io.github.edwinmindcraft.calio.common.network;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.network.packet.*;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class CalioNetwork {
	public static final String NETWORK_VERSION = "1.0";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CalioAPI.resource("channel"), () -> NETWORK_VERSION, NETWORK_VERSION::equals, NETWORK_VERSION::equals);

	public static void register() {
		int index = 0;
		//Login
		CHANNEL.messageBuilder(C2SAcknowledgePacket.class, index++, NetworkDirection.LOGIN_TO_SERVER)
				.decoder(C2SAcknowledgePacket::decode).encoder(C2SAcknowledgePacket::encode)
				.consumer(HandshakeHandler.indexFirst((handler, c2SAcknowledgePacket, context) -> c2SAcknowledgePacket.handle(context)))
				.loginIndex(C2SAcknowledgePacket::getLoginIndex, C2SAcknowledgePacket::setLoginIndex)
				.add();

		/*CHANNEL.messageBuilder(S2CLoginDynamicRegistriesPacket.class, index++, NetworkDirection.LOGIN_TO_CLIENT)
				.decoder(S2CLoginDynamicRegistriesPacket::decode).encoder(S2CLoginDynamicRegistriesPacket::encode)
				.consumer(S2CLoginDynamicRegistriesPacket::handle)
				.loginIndex(S2CLoginDynamicRegistriesPacket::getLoginIndex, S2CLoginDynamicRegistriesPacket::setLoginIndex)
				.buildLoginPacketList(S2CLoginDynamicRegistriesPacket::createLoginPacket)
				.add();*/
		CHANNEL.messageBuilder(S2CDynamicRegistryPacket.Login.class, index++, NetworkDirection.LOGIN_TO_CLIENT)
				.decoder(S2CDynamicRegistryPacket.Login::decode).encoder(S2CDynamicRegistryPacket::encode)
				.consumer((BiConsumer<S2CDynamicRegistryPacket.Login, Supplier<NetworkEvent.Context>>) S2CDynamicRegistryPacket::handle)
				.loginIndex(S2CDynamicRegistryPacket.Login::getLoginIndex, S2CDynamicRegistryPacket.Login::setLoginIndex)
				.buildLoginPacketList(S2CDynamicRegistryPacket.Login::create).add();
		//Play
		/*CHANNEL.messageBuilder(S2CDynamicRegistriesPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
				.decoder(S2CDynamicRegistriesPacket::decode).encoder(S2CDynamicRegistriesPacket::encode)
				.consumer(S2CDynamicRegistriesPacket::handle).add();*/
		CHANNEL.messageBuilder(S2CDataObjectRegistryPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
				.decoder(S2CDataObjectRegistryPacket::decode).encoder(S2CDataObjectRegistryPacket::encode)
				.consumer(S2CDataObjectRegistryPacket::handle).add();
		CHANNEL.messageBuilder(S2CDynamicRegistryPacket.Play.class, index++, NetworkDirection.PLAY_TO_CLIENT)
				.decoder(S2CDynamicRegistryPacket.Play::decode).encoder(S2CDynamicRegistryPacket::encode)
				.consumer((BiConsumer<S2CDynamicRegistryPacket.Play, Supplier<NetworkEvent.Context>>) S2CDynamicRegistryPacket::handle)
				.add();
		CalioAPI.LOGGER.debug("Registered {} packets", index);
	}
}
