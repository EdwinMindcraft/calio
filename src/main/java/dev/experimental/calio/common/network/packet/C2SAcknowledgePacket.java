package dev.experimental.calio.common.network.packet;

import dev.experimental.calio.api.CalioAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class C2SAcknowledgePacket implements IntSupplier {
	private int loginIndex;

	public C2SAcknowledgePacket() {

	}

	public static C2SAcknowledgePacket decode(FriendlyByteBuf buf) {
		return new C2SAcknowledgePacket();
	}

	public void encode(FriendlyByteBuf buf) {
	}

	public void handle(Supplier<NetworkEvent.Context> handler) {
		CalioAPI.LOGGER.info("Received acknowledgment for login packet with id {}", this.loginIndex);
		handler.get().setPacketHandled(true);
	}

	@Override
	public int getAsInt() {
		return this.loginIndex;
	}

	public int getLoginIndex() {
		return this.loginIndex;
	}

	public void setLoginIndex(int loginIndex) {
		this.loginIndex = loginIndex;
	}
}
