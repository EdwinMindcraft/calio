package io.github.edwinmindcraft.calio.api.event;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

public class DynamicRegistrationEvent<T> extends Event implements ICancellableEvent {
	private final ResourceKey<Registry<T>> registryKey;
	private final T original;
	private T newEntry;
	private String cancellationReason = null;

	public DynamicRegistrationEvent(ResourceKey<Registry<T>> registryKey, T original) {
		this.registryKey = registryKey;
		this.original = original;
		this.newEntry = original;
	}

	/**
	 * Adds a new reason to cancel this event.
	 * If set, the reason will be used when logging via event.
	 *
	 * @param reason The reason for stopping registration
	 */
	public DynamicRegistrationEvent<T> withCancellationReason(String reason) {
		this.cancellationReason = reason;
		return this;
	}

	public String getCancellationReason() {
		return this.cancellationReason;
	}

	public T getOriginal() {
		return this.original;
	}

	public T getNewEntry() {
		return this.newEntry;
	}

	public void setNewEntry(@NotNull T newEntry) {
		Validate.notNull(newEntry, "Entry cannot be null. Use Event.setCancelled(true) instead");
		this.newEntry = newEntry;
	}

	public ResourceLocation getRegistryName() {
		return this.registryKey.location();
	}
}
