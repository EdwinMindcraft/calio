package io.github.edwinmindcraft.calio.api.network.primitives;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;

/**
 * Double codec with added support for strings.<br/>
 * This is only so I can stop getting yelled at because fabric considers
 * strings to be freely castable into any other type.
 */
public class DoubleCodec implements PrimitiveCodec<Double> {
	@Override
	public <T> DataResult<Double> read(DynamicOps<T> ops, T input) {
		DataResult<Double> base = ops.getNumberValue(input).map(Number::doubleValue);
		if (base.error().isEmpty())
			return base;
		if (input instanceof JsonPrimitive primitive) {
			try {
				return DataResult.success(primitive.getAsDouble());
			} catch (Exception ignored) {}
		}
		return base;
	}

	@Override
	public <T> T write(DynamicOps<T> ops, Double value) {
		return ops.createDouble(value);
	}
}
