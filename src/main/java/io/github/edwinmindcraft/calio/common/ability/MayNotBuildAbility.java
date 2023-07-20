package io.github.edwinmindcraft.calio.common.ability;

import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

public class MayNotBuildAbility extends PlayerAbility {
	@Override
	public void grant(@NotNull Player player, @NotNull GameType gameType) {
		player.getAbilities().mayBuild = false;
	}

	@Override
	public void revoke(@NotNull Player player, @NotNull GameType gameType) {
		player.getAbilities().mayBuild = !gameType.isBlockPlacingRestricted();
	}

	@Override
	public boolean has(@NotNull Player player) {
		return !player.getAbilities().mayBuild;
	}
}
