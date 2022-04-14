package io.github.edwinmindcraft.calio.api.ability;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.LazyOptional;

/**
 * This is a workaround for PlayerAbilityLib not having a real forge alternative.
 * I wouldn't recommend using it, but it's there.
 */
public interface IAbilityHolder {
	/**
	 * Simple access for {@code entity.getCapability(CalioAPI.ABILITY_HOLDER)}.
	 *
	 * @param entity The entity to get the component for.
	 *
	 * @return A {@link LazyOptional} containing the {@link IAbilityHolder} if applicable.
	 */
	static LazyOptional<IAbilityHolder> get(Entity entity) {
		return entity.getCapability(CalioAPI.ABILITY_HOLDER);
	}

	/**
	 * Checks if the given entity has the given ability.
	 *
	 * @param entity  The entity
	 * @param ability The ability
	 *
	 * @return {@code true} if the entity has the ability, {@code false} otherwise.
	 */
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
