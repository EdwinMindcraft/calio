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
public enum FloatCodec implements PrimitiveCodec<Float> {
	INSTANCE;

	@Override
	public <T> DataResult<Float> read(DynamicOps<T> ops, T input) {
		DataResult<Float> base = ops.getNumberValue(input).map(Number::floatValue);
		if (base.error().isEmpty())
			return base;
		if (input instanceof JsonPrimitive primitive) {
			try {
				return DataResult.success(primitive.getAsFloat());
			} catch (Exception ignored) {}
		}
		return base;
	}

	@Override
	public <T> T write(DynamicOps<T> ops, Float value) {
		return ops.createFloat(value);
	}
}
