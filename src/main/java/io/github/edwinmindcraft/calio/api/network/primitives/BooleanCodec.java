package io.github.edwinmindcraft.calio.api.network.primitives;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;

/**
 * Boolean codec with added support for strings.<br/>
 * This is only so I can stop getting yelled at because fabric considers
 * strings to be freely castable into any other type.
 */
public class BooleanCodec implements PrimitiveCodec<Boolean> {
	@Override
	public <T> DataResult<Boolean> read(DynamicOps<T> ops, T input) {
		DataResult<Boolean> base = ops.getBooleanValue(input);
		if (base.error().isEmpty())
			return base;
		if (input instanceof JsonPrimitive primitive) {
			try {
				return DataResult.success(primitive.getAsBoolean());
			} catch (Exception ignored) {}
		}
		return base;
	}

	@Override
	public <T> T write(DynamicOps<T> ops, Boolean value) {
		return ops.createBoolean(value);
	}
}
