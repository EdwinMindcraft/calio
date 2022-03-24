package io.github.edwinmindcraft.calio.common.ability;

import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class FlightAbility extends PlayerAbility {
	@Override
	public void grant(@NotNull Player player) {
		player.getAbilities().flying = true;
	}

	@Override
	public void revoke(@NotNull Player player) {
		player.getAbilities().flying = false;
	}

	@Override
	public boolean has(@NotNull Player player) {
		return player.getAbilities().flying;
	}
}
