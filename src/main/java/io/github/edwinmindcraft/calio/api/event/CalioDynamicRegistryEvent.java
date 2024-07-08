package io.github.edwinmindcraft.calio.api.event;

import io.github.edwinmindcraft.calio.api.registry.CalioDynamicRegistryManager;
import net.minecraft.core.RegistryAccess;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class CalioDynamicRegistryEvent extends Event {
	private final CalioDynamicRegistryManager registryManager;

	public CalioDynamicRegistryEvent(CalioDynamicRegistryManager registryManager) {
		this.registryManager = registryManager;
	}

	public CalioDynamicRegistryManager getRegistryManager() {
		return this.registryManager;
	}

	public static class Initialize extends CalioDynamicRegistryEvent implements IModBusEvent {
		public Initialize(CalioDynamicRegistryManager registryManager) {
			super(registryManager);
		}
	}

	public static class LoadComplete extends CalioDynamicRegistryEvent {
		private final RegistryAccess registryAccess;

		public LoadComplete(CalioDynamicRegistryManager registryManager, RegistryAccess registryAccess) {
			super(registryManager);
			this.registryAccess = registryAccess;
		}

		public RegistryAccess getRegistryAccess() {
			return this.registryAccess;
		}
	}

	public static class Reload extends CalioDynamicRegistryEvent {
		private final RegistryAccess registryAccess;

		public Reload(CalioDynamicRegistryManager registryManager, RegistryAccess registryAccess) {
			super(registryManager);
			this.registryAccess = registryAccess;
		}

		public RegistryAccess getRegistryAccess() {
			return this.registryAccess;
		}
	}
}
