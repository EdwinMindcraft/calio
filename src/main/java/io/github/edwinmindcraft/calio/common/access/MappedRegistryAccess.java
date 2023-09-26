package io.github.edwinmindcraft.calio.common.access;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

/**
 This exists, so we have a way to get or create holders without running
 the risk of a {@link java.util.ConcurrentModificationException}
 */
public interface MappedRegistryAccess<T> {
    Holder<T> calio$getOrCreateHolderOrThrow(ResourceKey<T> pKey);
}
