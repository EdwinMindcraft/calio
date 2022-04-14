package io.github.edwinmindcraft.calio.common.network.packet;

import io.github.apace100.calio.registry.DataObjectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
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

	@OnlyIn(Dist.CLIENT)
	private void handleClient(Supplier<NetworkEvent.Context> ctx) {
		Minecraft minecraftClient = Minecraft.getInstance();
		DataObjectRegistry.getRegistry(this.registry()).receive(this.buffer(),
				minecraftClient.hasSingleplayerServer() ? r -> {} : ctx.get()::enqueueWork);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.handleClient(ctx));
	}
}
