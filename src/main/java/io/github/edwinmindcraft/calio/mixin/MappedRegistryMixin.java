package io.github.edwinmindcraft.calio.mixin;

import io.github.edwinmindcraft.calio.common.access.MappedRegistryAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> implements MappedRegistryAccess<T> {

    @Shadow @Final private Map<ResourceKey<T>, Holder.Reference<T>> byKey;

    @Shadow public abstract HolderOwner<T> holderOwner();

    @Shadow @Nullable protected Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;

    @Override
    public Holder<T> calio$getOrCreateHolderOrThrow(ResourceKey<T> key) {
        if (this.unregisteredIntrusiveHolders != null)
            throw new IllegalStateException("This registry can't create new holders without value");
        if (!this.byKey.containsKey(key))
            this.byKey.put(key, Holder.Reference.createStandAlone(this.holderOwner(), key));
        return this.byKey.get(key);
    }
}
