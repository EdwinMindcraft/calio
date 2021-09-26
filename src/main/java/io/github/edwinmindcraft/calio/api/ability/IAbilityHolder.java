package io.github.edwinmindcraft.calio.api.ability;

import net.minecraft.resources.ResourceLocation;

public interface IAbilityHolder {

	void grant(PlayerAbility ability, ResourceLocation source);

	void revoke(PlayerAbility ability, ResourceLocation source);

	boolean has(PlayerAbility ability, ResourceLocation source);

	boolean has(PlayerAbility ability);

	boolean applyRemovals();
	boolean applyAdditions();
}
