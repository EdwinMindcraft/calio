package io.github.edwinmindcraft.calio.api.ability;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistryEntry;

public abstract class PlayerAbility extends ForgeRegistryEntry<PlayerAbility> {
	public PlayerAbility() {
	}

	public abstract void grant(Player player);

	public abstract void revoke(Player player);

	public abstract boolean has(Player player);
}
