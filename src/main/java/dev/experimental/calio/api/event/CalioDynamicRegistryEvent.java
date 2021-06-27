package dev.experimental.calio.api.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.experimental.calio.api.registry.ICalioDynamicRegistryManager;

public interface CalioDynamicRegistryEvent {
    Event<CalioDynamicRegistryEvent> INITIALIZE_EVENT = EventFactory.createLoop();

    void accept(ICalioDynamicRegistryManager registryManager);
}
