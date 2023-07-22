package io.github.edwinmindcraft.calio.common.util;

import net.minecraft.resources.ResourceKey;

import java.util.Objects;

/**
 * This record serves as a wrapper for ResourceKeys to have a proper implementation of the
 * hashCode() method as for some reason ResourceKeys do not have a hashCode implementation.
 * @param resourceKey
 * @param <T>
 */
public record ComparableResourceKey<T>(ResourceKey<T> resourceKey) {

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;

        if ((other instanceof ComparableResourceKey<?> comparable))
            return comparable.resourceKey().equals(this.resourceKey());

        if ((other instanceof ResourceKey<?> key))
            return key.equals(this.resourceKey());

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.resourceKey().registry(), this.resourceKey().location());
    }

}
