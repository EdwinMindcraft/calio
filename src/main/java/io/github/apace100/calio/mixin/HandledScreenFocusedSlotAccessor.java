package io.github.apace100.calio.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface HandledScreenFocusedSlotAccessor {

	@Accessor("hoveredSlot")
	Slot getFocusedSlot();
}
