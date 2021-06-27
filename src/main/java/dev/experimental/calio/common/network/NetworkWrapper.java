package dev.experimental.calio.common.network;

import dev.experimental.calio.api.network.MessageDefinition;
import dev.experimental.calio.api.network.NetworkChannel;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

public class NetworkWrapper {
	public static final String ABSENT = "ABSENT \ud83e\udd14";
	public static final String ACCEPTVANILLA = "ALLOWVANILLA \ud83d\udc93\ud83d\udc93\ud83d\udc93";

	//@ExpectPlatform
	public static <MSG> void registerMessage(MessageDefinition<MSG> messageDefinition) {
		throw new AssertionError();
	}

	//@ExpectPlatform
	public static NetworkChannel registerChannel(NetworkChannel channel) {
		throw new AssertionError();
	}

	//@ExpectPlatform
	public static <MSG> void sendToServer(NetworkChannel channel, MSG message) { throw new AssertionError(); }

	//@ExpectPlatform
	public static <MSG> void sendToPlayer(NetworkChannel channel, ServerPlayerEntity player, MSG message) { throw new AssertionError(); }

	//@ExpectPlatform
	public static <MSG> void sendToTracking(NetworkChannel channel, Entity player, MSG message) { throw new AssertionError(); }

	//@ExpectPlatform
	public static <MSG> void sendToTrackingAndSelf(NetworkChannel channel, ServerPlayerEntity player, MSG message) { throw new AssertionError(); }
}
