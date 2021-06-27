package dev.experimental.calio.common.network.packet;

import dev.experimental.calio.api.CalioAPI;
import dev.experimental.calio.api.network.INetworkHandler;
import net.minecraft.network.PacketByteBuf;

import java.util.function.IntSupplier;

public class C2SAcknowledgePacket implements IntSupplier {
    private int loginIndex;

    public C2SAcknowledgePacket() {

    }

    public static C2SAcknowledgePacket decode(PacketByteBuf buf) {
        return new C2SAcknowledgePacket();
    }

    public void encode(PacketByteBuf buf) {
    }

    public void handle(INetworkHandler handler) {
        CalioAPI.LOGGER.info("Received acknowledgment for login packet with id {}", this.loginIndex);
        handler.setHandled(true);
    }

    @Override
    public int getAsInt() {
        return this.loginIndex;
    }

    public int getLoginIndex() {
        return loginIndex;
    }

    public void setLoginIndex(int loginIndex) {
        this.loginIndex = loginIndex;
    }
}
