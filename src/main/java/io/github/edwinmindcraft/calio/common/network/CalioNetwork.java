package io.github.edwinmindcraft.calio.common.network;

import io.github.apace100.calio.ClassUtil;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.network.packet.*;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class CalioNetwork {
	public static final String NETWORK_VERSION = "1.2";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CalioAPI.resource("channel"), () -> NETWORK_VERSION, NETWORK_VERSION::equals, NETWORK_VERSION::equals);

	private static <T, R> Function<T, R> failsafe(Function<T, R> function) {
		return t -> {
			try {
				return function.apply(t);
			} catch (Throwable err) {
				err.printStackTrace();
				throw err;
			}
		};
	}

	private static <T, R> BiConsumer<T, R> failsafe(BiConsumer<T, R> function) {
		return (t, r) -> {
			try {
				function.accept(t, r);
			} catch (Throwable err) {
				err.printStackTrace();
				throw err;
			}
		};
	}

	public static void register() {
		int index = 0;
		//Login
		CHANNEL.messageBuilder(C2SAcknowledgePacket.class, index++, NetworkDirection.LOGIN_TO_SERVER)
				.decoder(failsafe(C2SAcknowledgePacket::decode)).encoder(failsafe(C2SAcknowledgePacket::encode))
				.consumerNetworkThread(failsafe(HandshakeHandler.indexFirst((handler, c2SAcknowledgePacket, context) -> c2SAcknowledgePacket.handle(context))))
				.loginIndex(C2SAcknowledgePacket::getLoginIndex, C2SAcknowledgePacket::setLoginIndex)
				.add();

		Class<S2CDynamicRegistryPacket.Login<?>> loginClass = ClassUtil.get();
		CHANNEL.messageBuilder(loginClass, index++, NetworkDirection.LOGIN_TO_CLIENT)
				.decoder(failsafe(S2CDynamicRegistryPacket.Login::decode)).encoder(failsafe(S2CDynamicRegistryPacket::encode))
				.consumerNetworkThread(failsafe(S2CDynamicRegistryPacket::handle))
				.loginIndex(S2CDynamicRegistryPacket.Login::getLoginIndex, S2CDynamicRegistryPacket.Login::setLoginIndex)
				.buildLoginPacketList(S2CDynamicRegistryPacket.Login::create).add();
		//Play
		CHANNEL.messageBuilder(S2CDataObjectRegistryPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
				.decoder(failsafe(S2CDataObjectRegistryPacket::decode)).encoder(failsafe(S2CDataObjectRegistryPacket::encode))
				.consumerNetworkThread(failsafe(S2CDataObjectRegistryPacket::handle)).add();
		Class<S2CDynamicRegistryPacket.Play<?>> playClass = ClassUtil.get();
		CHANNEL.messageBuilder(playClass, index++, NetworkDirection.PLAY_TO_CLIENT)
				.decoder(failsafe(S2CDynamicRegistryPacket.Play::decode)).encoder(failsafe(S2CDynamicRegistryPacket::encode))
				.consumerNetworkThread(failsafe(S2CDynamicRegistryPacket::handle))
				.add();
		CalioAPI.LOGGER.debug("Registered {} packets", index);
	}
}
