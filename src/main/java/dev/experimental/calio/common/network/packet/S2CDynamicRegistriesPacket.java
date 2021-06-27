package dev.experimental.calio.common.network.packet;

import dev.experimental.calio.api.network.INetworkHandler;
import dev.experimental.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.network.PacketByteBuf;

import java.util.function.IntSupplier;

public class S2CDynamicRegistriesPacket implements IntSupplier {
    private final CalioDynamicRegistryManager manager;
    private int loginIndex;
    public S2CDynamicRegistriesPacket(CalioDynamicRegistryManager manager) {
        this.manager = manager;
    }

    public static S2CDynamicRegistriesPacket decode(PacketByteBuf buf) {
        return new S2CDynamicRegistriesPacket(CalioDynamicRegistryManager.decode(buf));
    }

    public int getLoginIndex() {
        return loginIndex;
    }

    public void setLoginIndex(int loginIndex) {
        this.loginIndex = loginIndex;
    }

    public void encode(PacketByteBuf buf) {
        this.manager.encode(buf);
    }

    public void handle(INetworkHandler handler) {
        handler.queue(() -> CalioDynamicRegistryManager.setClientInstance(this.manager));
        handler.reply(new C2SAcknowledgePacket());
        handler.setHandled(true);
    }

    @Override
    public int getAsInt() {
        return this.loginIndex;
    }
}
