package io.github.edwinmindcraft.calio.common.network.packet;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2SAcknowledgePacket implements CustomPacketPayload {
	public static final ResourceLocation ID = CalioAPI.resource("acknowledge");
	public static final Type<C2SAcknowledgePacket> TYPE = new Type<>(ID);
	public static final StreamCodec<FriendlyByteBuf, C2SAcknowledgePacket> STREAM_CODEC = StreamCodec.unit(new C2SAcknowledgePacket());

	public C2SAcknowledgePacket() {}


	public void handle(IPayloadContext ctx) {
		CalioAPI.LOGGER.info("Received acknowledgment for login packet from player with UUID {}", ctx.player().getStringUUID());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return null;
	}
}
