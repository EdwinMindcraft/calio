package io.github.edwinmindcraft.calio.api.registry;

import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import io.github.edwinmindcraft.calio.common.ability.AllowFlyingAbility;
import io.github.edwinmindcraft.calio.common.ability.FlyingAbility;
import io.github.edwinmindcraft.calio.common.ability.InstabuildAbility;
import io.github.edwinmindcraft.calio.common.ability.InvulnerableAbility;
import io.github.edwinmindcraft.calio.common.registry.CalioRegisters;
import net.minecraft.core.Registry;

import java.util.function.Supplier;

public class PlayerAbilities {
	public static Registry<PlayerAbility> REGISTRY;

	public static final Supplier<PlayerAbility> FLYING = CalioRegisters.PLAYER_ABILITIES.register("flying", FlyingAbility::new);
	public static final Supplier<PlayerAbility> ALLOW_FLYING = CalioRegisters.PLAYER_ABILITIES.register("mayfly", AllowFlyingAbility::new);
    public static final Supplier<PlayerAbility> INSTABUILD = CalioRegisters.PLAYER_ABILITIES.register("instabuild", InstabuildAbility::new);
    public static final Supplier<PlayerAbility> INVULNERABLE = CalioRegisters.PLAYER_ABILITIES.register("invulnerable", InvulnerableAbility::new);
    public static final Supplier<PlayerAbility> MAY_NOT_BUILD = CalioRegisters.PLAYER_ABILITIES.register("maynotbuild", InvulnerableAbility::new);

	public static void register() {
	}
}
