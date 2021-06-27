package dev.experimental.calio.api.network;

import dev.experimental.calio.common.network.NetworkWrapper;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.function.Predicate;
import java.util.function.Supplier;

public record NetworkChannel(Identifier channel,
                             Predicate<String> acceptedServerVersion,
                             Predicate<String> acceptedClientVersion,
                             Supplier<String> version) {

    public static NetworkChannel create(Identifier channel, Predicate<String> acceptedServerVersion, Predicate<String> acceptedClientVersion, Supplier<String> version) {
        return NetworkWrapper.registerChannel(new NetworkChannel(channel, acceptedServerVersion, acceptedClientVersion, version));
    }

    public <MSG> MessageDefinition.Builder<MSG> messageBuilder(int index, Class<MSG> message, PacketDirection direction) {
        return new MessageDefinition.Builder<>(this, index, message, direction);
    }

    public <MSG> void sendToServer(MSG message) {
        NetworkWrapper.sendToServer(this, message);
    }

    public <MSG> void sendToPlayer(ServerPlayerEntity player, MSG message) {
        NetworkWrapper.sendToPlayer(this, player, message);
    }

    public <MSG> void sendToTracking(Entity player, MSG message) {
        NetworkWrapper.sendToTracking(this, player, message);
    }

    public <MSG> void sendToTrackingAndSelf(ServerPlayerEntity player, MSG message) {
        NetworkWrapper.sendToTrackingAndSelf(this, player, message);
    }
}
