package io.github.edwinmindcraft.calio.common.registry;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.ability.PlayerAbility;
import io.github.edwinmindcraft.calio.api.registry.PlayerAbilities;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class CalioRegisters {
	public static final DeferredRegister<DataComponentType<?>> COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CalioAPI.MODID);
	public static final DeferredRegister<PlayerAbility> PLAYER_ABILITIES = DeferredRegister.create(CalioAPI.resource("player_ability"), CalioAPI.MODID);

	public static void register(IEventBus bus) {
		//Note for myself: Always call makeRegistry before register.
		PlayerAbilities.REGISTRY = PLAYER_ABILITIES.makeRegistry(RegistryBuilder::create);

		COMPONENT_TYPES.register(bus);
		PLAYER_ABILITIES.register(bus);
	}
}
