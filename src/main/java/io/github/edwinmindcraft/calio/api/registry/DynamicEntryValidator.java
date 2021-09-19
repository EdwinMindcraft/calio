package io.github.edwinmindcraft.calio.api.registry;

import com.mojang.serialization.DataResult;

public interface DynamicEntryValidator<T> {
	DataResult<T> validate(T input, ICalioDynamicRegistryManager manager);
}
