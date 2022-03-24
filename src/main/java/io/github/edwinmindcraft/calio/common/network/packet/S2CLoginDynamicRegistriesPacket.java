package io.github.edwinmindcraft.calio.common.network.packet;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.network.CalioNetwork;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class S2CLoginDynamicRegistriesPacket implements IntSupplier {
	public static List<Pair<String, S2CLoginDynamicRegistriesPacket>> createLoginPacket(boolean isLocal) {
		try {
			return List.of(Pair.of("CALIO-S2CLoginDynamicRegistriesPacket", new S2CLoginDynamicRegistriesPacket((CalioDynamicRegistryManager) CalioAPI.getDynamicRegistries(ServerLifecycleHooks.getCurrentServer()))));
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	private int loginIndex;
	private final CalioDynamicRegistryManager manager;

	public S2CLoginDynamicRegistriesPacket(CalioDynamicRegistryManager manager) {
		this.manager = manager;
	}

	public static S2CLoginDynamicRegistriesPacket decode(FriendlyByteBuf buf) {
		return new S2CLoginDynamicRegistriesPacket(CalioDynamicRegistryManager.decode(buf));
	}

	public void encode(FriendlyByteBuf buf) {
		this.manager.encode(buf);
	}

	public void handle(Supplier<NetworkEvent.Context> handler) {
		handler.get().enqueueWork(() -> {
			CalioDynamicRegistryManager.setClientInstance(this.manager);
			CalioDynamicRegistryManager.getInstance(null).dump();
		});
		CalioNetwork.CHANNEL.reply(new C2SAcknowledgePacket(), handler.get());
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
