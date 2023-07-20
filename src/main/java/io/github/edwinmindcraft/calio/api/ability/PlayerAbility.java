package io.github.edwinmindcraft.calio.api.ability;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.registries.ForgeRegistryEntry;

public abstract class PlayerAbility extends ForgeRegistryEntry<PlayerAbility> {
	public PlayerAbility() {
	}

	public abstract void grant(Player player, GameType gameType);

	public abstract void revoke(Player player, GameType gameType);

	public abstract boolean has(Player player);
}
