package io.github.edwinmindcraft.calio.common.network.packet;

import io.github.apace100.calio.registry.DataObjectRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CDataObjectRegistryPacket(ResourceLocation registry, FriendlyByteBuf buffer) {
	public static S2CDataObjectRegistryPacket decode(FriendlyByteBuf buf) {
		return new S2CDataObjectRegistryPacket(buf.readResourceLocation(), buf);
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeResourceLocation(this.registry());
		buf.writeBytes(this.buffer());
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DataObjectRegistry.getRegistry(this.registry()).receive(this.buffer()));
	}
}
