package io.github.edwinmindcraft.calio.api.registry;

import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import io.github.edwinmindcraft.calio.common.ability.AllowFlyingAbility;
import io.github.edwinmindcraft.calio.common.ability.FlyingAbility;
import io.github.edwinmindcraft.calio.common.ability.InstabuildAbility;
import io.github.edwinmindcraft.calio.common.ability.InvulnerableAbility;
import io.github.edwinmindcraft.calio.common.registry.CalioRegisters;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class PlayerAbilities {
	public static Supplier<IForgeRegistry<PlayerAbility>> REGISTRY;

	public static final RegistryObject<PlayerAbility> FLYING = CalioRegisters.PLAYER_ABILITIES.register("flying", FlyingAbility::new);
	public static final RegistryObject<PlayerAbility> ALLOW_FLYING = CalioRegisters.PLAYER_ABILITIES.register("mayfly", AllowFlyingAbility::new);
    public static final RegistryObject<PlayerAbility> INSTABUILD = CalioRegisters.PLAYER_ABILITIES.register("instabuild", InstabuildAbility::new);
    public static final RegistryObject<PlayerAbility> INVULNERABLE = CalioRegisters.PLAYER_ABILITIES.register("invulnerable", InvulnerableAbility::new);
    public static final RegistryObject<PlayerAbility> MAY_NOT_BUILD = CalioRegisters.PLAYER_ABILITIES.register("maynotbuild", InvulnerableAbility::new);

	public static void register() {
	}
}
