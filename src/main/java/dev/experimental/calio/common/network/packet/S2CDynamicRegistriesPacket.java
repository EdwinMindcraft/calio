package dev.experimental.calio.common.network.packet;

import dev.experimental.calio.common.network.CalioNetwork;
import dev.experimental.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class S2CDynamicRegistriesPacket implements IntSupplier {
	private final CalioDynamicRegistryManager manager;
	private int loginIndex;

	public S2CDynamicRegistriesPacket(CalioDynamicRegistryManager manager) {
		this.manager = manager;
	}

	public static S2CDynamicRegistriesPacket decode(FriendlyByteBuf buf) {
		return new S2CDynamicRegistriesPacket(CalioDynamicRegistryManager.decode(buf));
	}

	public int getLoginIndex() {
		return this.loginIndex;
	}

	public void setLoginIndex(int loginIndex) {
		this.loginIndex = loginIndex;
	}

	public void encode(FriendlyByteBuf buf) {
		this.manager.encode(buf);
	}

	public void handle(Supplier<NetworkEvent.Context> handler) {
		handler.get().enqueueWork(() -> CalioDynamicRegistryManager.setClientInstance(this.manager));
		CalioNetwork.CHANNEL.reply(new C2SAcknowledgePacket(), handler.get());
		handler.get().setPacketHandled(true);
	}

	@Override
	public int getAsInt() {
		return this.loginIndex;
	}
}
