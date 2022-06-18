package io.github.edwinmindcraft.calio.api.network.primitives;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;

/**
 * Integer codec with added support for strings.<br/>
 * This is only so I can stop getting yelled at because fabric considers
 * strings to be freely castable into any other type.
 */
public class IntegerCodec implements PrimitiveCodec<Integer> {
	@Override
	public <T> DataResult<Integer> read(DynamicOps<T> ops, T input) {
		DataResult<Integer> base = ops.getNumberValue(input).map(Number::intValue);
		if (base.error().isEmpty())
			return base;
		if (input instanceof JsonPrimitive primitive) {
			try {
				return DataResult.success(primitive.getAsInt());
			} catch (Exception ignored) {}
		}
		return base;
	}

	@Override
	public <T> T write(DynamicOps<T> ops, Integer value) {
		return ops.createInt(value);
	}
}
