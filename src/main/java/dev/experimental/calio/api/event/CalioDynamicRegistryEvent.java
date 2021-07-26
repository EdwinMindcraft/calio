package dev.experimental.calio.api.event;

import dev.experimental.calio.api.registry.ICalioDynamicRegistryManager;
import net.minecraftforge.eventbus.api.Event;

public class CalioDynamicRegistryEvent extends Event {
	private final ICalioDynamicRegistryManager registryManager;

	public CalioDynamicRegistryEvent(ICalioDynamicRegistryManager registryManager) {
		this.registryManager = registryManager;
	}

	public ICalioDynamicRegistryManager getRegistryManager() {
		return this.registryManager;
	}
}
