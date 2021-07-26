package io.github.apace100.calio.mixin;

import net.minecraft.world.entity.ai.behavior.ShufflingList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Kept for compatibility. Shouldn't be used as 1.17 has a getter for this.
 */
@Deprecated
@Mixin(ShufflingList.WeightedEntry.class)
public interface WeightedListEntryAccessor {

    @Accessor("weight")
    int getWeight();
}
