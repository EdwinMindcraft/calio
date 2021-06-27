package dev.experimental.calio.api.network;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.OptionalInt;

public interface INetworkHandler {
    @Environment(EnvType.CLIENT)
    static PlayerEntity localPlayer() {
        return MinecraftClient.getInstance().player;
    }

    static Pair<Entity, String> find(World world, OptionalInt entity) {
        Entity foundEntity = EnvExecutor.getInEnv(Env.CLIENT, () -> INetworkHandler::localPlayer).orElse(null);
        if (entity.isPresent())
            foundEntity = world.getEntityById(entity.getAsInt());
        return Pair.of(foundEntity, foundEntity != null ? foundEntity.getEntityName() : (entity.isPresent() ? "EntityId[" + entity.getAsInt() + "]" : "LocalPlayer"));
    }

    void queue(Runnable runnable);

    void setHandled(boolean handled);

    <MSG> void reply(MSG message);

    PlayerEntity getPlayer();

    void disconnect(Text reason);

    World getWorld();
}
