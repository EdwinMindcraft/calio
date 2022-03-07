package io.github.edwinmindcraft.calio.api.registry;

import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import io.github.edwinmindcraft.calio.common.ability.AllowFlightAbility;
import io.github.edwinmindcraft.calio.common.ability.FlightAbility;
import io.github.edwinmindcraft.calio.common.registry.CalioRegisters;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class PlayerAbilities {
	public static Supplier<IForgeRegistry<PlayerAbility>> REGISTRY;

	public static final RegistryObject<PlayerAbility> FLIGHT = CalioRegisters.PLAYER_ABILITIES.register("flight", FlightAbility::new);
	public static final RegistryObject<PlayerAbility> ALLOW_FLIGHT = CalioRegisters.PLAYER_ABILITIES.register("allow_flight", AllowFlightAbility::new);

	public static void register() {
	}
}
