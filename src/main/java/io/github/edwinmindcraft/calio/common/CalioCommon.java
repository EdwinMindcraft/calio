package io.github.edwinmindcraft.calio.common;

import io.github.edwinmindcraft.calio.api.registry.PlayerAbilities;
import io.github.edwinmindcraft.calio.common.registry.CalioRegisters;
import io.github.edwinmindcraft.calio.common.util.ComponentConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;

public class CalioCommon {
	public static void initialize(IEventBus bus) {
		CalioRegisters.register(bus);
		PlayerAbilities.register();
		ComponentConstants.register();

		ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, CalioConfig.COMMON_SPECS);
	}
}
