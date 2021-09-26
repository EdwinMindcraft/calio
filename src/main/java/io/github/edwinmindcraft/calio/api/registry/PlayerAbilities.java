package io.github.edwinmindcraft.calio.api.registry;

import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import io.github.edwinmindcraft.calio.common.ability.AllowFlightAbility;
import io.github.edwinmindcraft.calio.common.ability.FlightAbility;
import io.github.edwinmindcraft.calio.common.registry.CalioRegisters;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public class PlayerAbilities {
	public static final Supplier<IForgeRegistry<PlayerAbility>> REGISTRY = CalioRegisters.PLAYER_ABILITIES.makeRegistry("player_abilities", RegistryBuilder::new);

	public static final RegistryObject<PlayerAbility> FLIGHT = CalioRegisters.PLAYER_ABILITIES.register("flight", FlightAbility::new);
	public static final RegistryObject<PlayerAbility> ALLOW_FLIGHT = CalioRegisters.PLAYER_ABILITIES.register("allow_flight", AllowFlightAbility::new);

	public static void register() {}
}
