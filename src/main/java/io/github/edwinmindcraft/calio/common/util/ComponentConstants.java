package io.github.edwinmindcraft.calio.common.util;

import io.github.edwinmindcraft.calio.common.registry.CalioRegisters;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Unit;

import java.util.function.Supplier;

// TODO: Wait to see what Origins Fabric does with components, and then do that.
public final class ComponentConstants {
	public static final Supplier<DataComponentType<Unit>> NON_ITALIC_NAME = CalioRegisters.COMPONENT_TYPES.register("non_italic_name", () -> DataComponentType.<Unit>builder()
			.persistent(Unit.CODEC)
			.networkSynchronized(ByteBufCodecs.fromCodec(Unit.CODEC))
			.build());
	public static final Supplier<DataComponentType<Unit>> ADDITIONAL_ATTRIBUTES = CalioRegisters.COMPONENT_TYPES.register("additional_attributes", () -> DataComponentType.<Unit>builder()
			.persistent(Unit.CODEC)
			.networkSynchronized(ByteBufCodecs.fromCodec(Unit.CODEC))
			.build());

	public static void register() {

	}
}
