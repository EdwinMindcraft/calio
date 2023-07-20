package io.github.edwinmindcraft.calio.common.ability;

import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

public class FlyingAbility extends PlayerAbility {
	@Override
	public void grant(@NotNull Player player, @NotNull GameType gameType) {
		player.getAbilities().flying = true;
	}

	@Override
	public void revoke(@NotNull Player player, @NotNull GameType gameType) {
		player.getAbilities().flying = gameType == GameType.SPECTATOR;
	}

	@Override
	public boolean has(@NotNull Player player) {
		return player.getAbilities().flying;
	}
}
