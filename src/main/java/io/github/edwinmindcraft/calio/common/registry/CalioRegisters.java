package io.github.edwinmindcraft.calio.common.registry;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import io.github.edwinmindcraft.calio.api.registry.PlayerAbilities;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryBuilder;

public class CalioRegisters {
	public static final DeferredRegister<PlayerAbility> PLAYER_ABILITIES = DeferredRegister.create(CalioAPI.resource("player_ability"), CalioAPI.MODID);

	public static void register(IEventBus bus) {
		//Note for myself: Always call makeRegistry before register.
		PlayerAbilities.REGISTRY = PLAYER_ABILITIES.makeRegistry(() -> new RegistryBuilder<PlayerAbility>().disableSaving());

		PLAYER_ABILITIES.register(bus);
	}
}
