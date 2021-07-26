package dev.experimental.calio.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SShareItemPacket {
	private final ItemStack stack;

	public C2SShareItemPacket(ItemStack stack) {
		this.stack = stack;
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeItemStack(this.stack, true);
	}

	public static C2SShareItemPacket decode(FriendlyByteBuf buffer) {
		return new C2SShareItemPacket(buffer.readItem());
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		contextSupplier.get().enqueueWork(() -> {
			ServerPlayer sender = contextSupplier.get().getSender();
			Component chatText = new TranslatableComponent("chat.type.text", sender.getDisplayName(), this.stack.getDisplayName());
			sender.getServer().getPlayerList().broadcastMessage(chatText, ChatType.CHAT, sender.getUUID());
		});
		contextSupplier.get().setPacketHandled(true);
	}
}
