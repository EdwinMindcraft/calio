package io.github.apace100.calio.mixin;

import net.minecraft.util.collection.WeightedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Kept for compatibility. Shouldn't be used as 1.17 has a getter for this.
 */
@Deprecated
@Mixin(WeightedList.Entry.class)
public interface WeightedListEntryAccessor {

    @Accessor
    int getWeight();
}
