package io.github.edwinmindcraft.calio.api.event;

import io.github.edwinmindcraft.calio.api.registry.ICalioDynamicRegistryManager;
import net.minecraftforge.eventbus.api.Event;

public class CalioDynamicRegistryEvent extends Event {
	private final ICalioDynamicRegistryManager registryManager;

	public CalioDynamicRegistryEvent(ICalioDynamicRegistryManager registryManager) {
		this.registryManager = registryManager;
	}

	public ICalioDynamicRegistryManager getRegistryManager() {
		return this.registryManager;
	}

	public static class Initialize extends CalioDynamicRegistryEvent {
		public Initialize(ICalioDynamicRegistryManager registryManager) {
			super(registryManager);
		}
	}

	public static class LoadComplete extends CalioDynamicRegistryEvent {
		public LoadComplete(ICalioDynamicRegistryManager registryManager) {
			super(registryManager);
		}
	}

	public static class Reload extends CalioDynamicRegistryEvent {
		public Reload(ICalioDynamicRegistryManager registryManager) {
			super(registryManager);
		}
	}
}
