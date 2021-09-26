package io.github.edwinmindcraft.calio.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.GenericEvent;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

@Cancelable
public class DynamicRegistrationEvent<T> extends GenericEvent<T> {
	private final ResourceLocation registryName;
	private final T original;
	private T newEntry;
	private String cancellationReason = null;

	public DynamicRegistrationEvent(Class<T> type, ResourceLocation path, T original) {
		super(type);
		this.registryName = path;
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
		return this.registryName;
	}
}
