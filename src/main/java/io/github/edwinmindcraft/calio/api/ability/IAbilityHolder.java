package io.github.edwinmindcraft.calio.api.ability;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.LazyOptional;

public interface IAbilityHolder {
	static LazyOptional<IAbilityHolder> get(Entity entity) {
		return entity.getCapability(CalioAPI.ABILITY_HOLDER);
	}

	static boolean has(Entity entity, PlayerAbility ability) {
		return get(entity).map(x -> x.has(ability)).orElse(false);
	}

	void grant(PlayerAbility ability, ResourceLocation source);

	void revoke(PlayerAbility ability, ResourceLocation source);

	boolean has(PlayerAbility ability, ResourceLocation source);

	boolean has(PlayerAbility ability);

	boolean applyRemovals();

	boolean applyAdditions();
}
