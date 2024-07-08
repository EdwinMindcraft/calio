package io.github.edwinmindcraft.calio.common.ability;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.ability.AbilityHolder;
import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityHolderImpl implements AbilityHolder {
	public static final ResourceLocation ID = CalioAPI.resource("ability_holder");

	private final Multimap<PlayerAbility, ResourceLocation> abilities;
	private final Set<PlayerAbility> toTick;
	private final Player player;

	public AbilityHolderImpl(Player player) {
		this.player = player;
		this.abilities = Multimaps.newMultimap(new ConcurrentHashMap<>(), () -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
		this.toTick = Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	@Override
	public void grant(@NotNull PlayerAbility ability, @NotNull ResourceLocation source) {
		this.abilities.put(ability, source);
	}

	@Override
	public void revoke(@NotNull PlayerAbility ability, @NotNull ResourceLocation source) {
		boolean flag = this.abilities.remove(ability, source);
		if (flag && !this.abilities.containsKey(ability))
			this.toTick.add(ability);
	}

	@Override
	public boolean has(@NotNull PlayerAbility ability, @NotNull ResourceLocation source) {
		return this.abilities.containsEntry(ability, source);
	}

	@Override
	public boolean has(@NotNull PlayerAbility ability) {
		return this.abilities.containsKey(ability);
	}

	@Override
	public boolean applyRemovals() {
		boolean flag = false;
		for (PlayerAbility ability : this.toTick) {
			if (!this.has(ability) && ability.has(this.player)) {
				ability.revoke(this.player, getPlayerGameType());
				flag = true;
			}
		}
		this.toTick.clear();
		return flag;
	}

	@Override
	public boolean applyAdditions() {
		boolean flag = false;
		for (PlayerAbility ability : this.abilities.keySet()) {
			if (!ability.has(this.player) && this.has(ability)) {
				ability.grant(this.player, getPlayerGameType());
				flag = true;
			}
		}
		return flag;
	}

    private GameType getPlayerGameType() {
        if (this.player.level().isClientSide()) {
            PlayerInfo playerinfo = Minecraft.getInstance().getConnection().getPlayerInfo(player.getGameProfile().getId());
            return playerinfo == null ? GameType.SURVIVAL : playerinfo.getGameMode();
        } else {
            return ((ServerPlayer)this.player).gameMode.getGameModeForPlayer();
        }
    }

	@Nullable
	public static AbilityHolder get(Entity entity) {
		return CalioAPI.ABILITY_HOLDER.getCapability(entity, null);
	}
}
