package io.github.edwinmindcraft.calio.common.network.packet;

import io.github.apace100.calio.registry.DataObjectRegistry;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CDataObjectRegistryPacket(ResourceLocation registry, FriendlyByteBuf buffer) implements CustomPacketPayload {
	public static final ResourceLocation ID = CalioAPI.resource("data_object_registry");
	public static final Type<S2CDataObjectRegistryPacket> TYPE = new Type<>(ID);
	public static final StreamCodec<FriendlyByteBuf, S2CDataObjectRegistryPacket> STREAM_CODEC = StreamCodec.of(S2CDataObjectRegistryPacket::write, S2CDataObjectRegistryPacket::new);

	public S2CDataObjectRegistryPacket(FriendlyByteBuf buf) {
		this(buf.readResourceLocation(), buf);
	}

	public static void write(FriendlyByteBuf buf, S2CDataObjectRegistryPacket packet) {
		buf.writeResourceLocation(packet.registry());
		buf.writeBytes(packet.buffer());
	}

	public void handle(IPayloadContext ctx) {
		Minecraft minecraftClient = Minecraft.getInstance();
		DataObjectRegistry.getRegistry(this.registry()).receive(this.buffer(),
				minecraftClient.hasSingleplayerServer() ? r -> {} : Minecraft.getInstance()::execute);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
