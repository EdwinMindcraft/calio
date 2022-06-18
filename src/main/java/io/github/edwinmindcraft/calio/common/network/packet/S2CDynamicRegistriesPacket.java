package io.github.edwinmindcraft.calio.common.network.packet;

import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Deprecated
public class S2CDynamicRegistriesPacket {
	private final CalioDynamicRegistryManager manager;

	public S2CDynamicRegistriesPacket(CalioDynamicRegistryManager manager) {
		this.manager = manager;
	}

	public static S2CDynamicRegistriesPacket decode(FriendlyByteBuf buf) {
		return new S2CDynamicRegistriesPacket(CalioDynamicRegistryManager.decode(buf));
	}

	public void encode(FriendlyByteBuf buf) {
		this.manager.encode(buf);
	}

	public void handle(Supplier<NetworkEvent.Context> handler) {
		handler.get().enqueueWork(() -> {
			CalioDynamicRegistryManager.setClientInstance(this.manager);
			CalioDynamicRegistryManager.getInstance(null).dump();
		});
		handler.get().setPacketHandled(true);
	}
}
