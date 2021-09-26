package io.github.edwinmindcraft.calio.common.registry;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class CalioRegisters {
	public static final DeferredRegister<PlayerAbility> PLAYER_ABILITIES = DeferredRegister.create(PlayerAbility.class, CalioAPI.MODID);

	public static void register(IEventBus bus) {
		PLAYER_ABILITIES.register(bus);
	}
}
