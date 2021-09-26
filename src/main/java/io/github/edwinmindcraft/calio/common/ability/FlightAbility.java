package io.github.edwinmindcraft.calio.common.ability;

import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import net.minecraft.world.entity.player.Player;

public class FlightAbility extends PlayerAbility {
	@Override
	public void grant(Player player) {
		player.getAbilities().flying = true;
	}

	@Override
	public void revoke(Player player) {
		player.getAbilities().flying = false;
	}

	@Override
	public boolean has(Player player) {
		return player.getAbilities().flying;
	}
}
