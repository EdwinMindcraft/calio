package dev.experimental.calio.common.network;

import dev.experimental.calio.api.CalioAPI;
import dev.experimental.calio.api.network.NetworkChannel;
import dev.experimental.calio.api.network.PacketDirection;
import dev.experimental.calio.common.network.packet.C2SAcknowledgePacket;
import dev.experimental.calio.common.network.packet.S2CDynamicRegistriesPacket;

public class CalioNetwork {
    public static final String NETWORK_VERSION = "1.0.0";
    public static final NetworkChannel CHANNEL = NetworkChannel.create(CalioAPI.identifier("channel"), NETWORK_VERSION::equals, NETWORK_VERSION::equals, () -> NETWORK_VERSION);

    public static void register() {
        int index = 0;
        CHANNEL.messageBuilder(index++, C2SAcknowledgePacket.class, PacketDirection.LOGIN_SERVERBOUND)
                .decoder(C2SAcknowledgePacket::decode).encoder(C2SAcknowledgePacket::encode)
                .handler(C2SAcknowledgePacket::handle)
                .loginIndex(C2SAcknowledgePacket::getLoginIndex, C2SAcknowledgePacket::setLoginIndex)
                .add();
        CHANNEL.messageBuilder(index++, S2CDynamicRegistriesPacket.class, PacketDirection.LOGIN_CLIENTBOUND)
                .decoder(S2CDynamicRegistriesPacket::decode).encoder(S2CDynamicRegistriesPacket::encode)
                .handler(S2CDynamicRegistriesPacket::handle)
                .loginIndex(S2CDynamicRegistriesPacket::getLoginIndex, S2CDynamicRegistriesPacket::setLoginIndex)
                .markAsLoginPacket().add();
        CalioAPI.LOGGER.debug("Registered {} packets", index);
    }
}
