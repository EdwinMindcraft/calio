package io.github.apace100.calio.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/**
 * Use access transformers instead.
 */
@Deprecated
//@Mixin(AbstractContainerScreen.class)
public interface HandledScreenFocusedSlotAccessor {

	/**
	 * @deprecated Use {@link AbstractContainerScreen#hoveredSlot} instead.
	 */
	//@Accessor("hoveredSlot")
	@Deprecated
	default Slot getFocusedSlot() {
		return ((AbstractContainerScreen<?>) this).hoveredSlot;
	}
}
