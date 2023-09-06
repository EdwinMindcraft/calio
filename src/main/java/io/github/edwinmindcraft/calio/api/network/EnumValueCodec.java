package io.github.edwinmindcraft.calio.api.network;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class EnumValueCodec<A extends Enum<A>> implements Codec<A> {
	private final A[] values;
	private final Multimap<A, String> names;
	private final Map<String, A> fields;

	public EnumValueCodec(A[] values, @Nullable Map<String, A> names) {
		this.values = values;
		ImmutableMultimap.Builder<A, String> namesBuilder = ImmutableSetMultimap.builder();
		HashMap<String, A> fieldsBuilder = new HashMap<>();
		for (A value : values) {
			String name = value.name().toLowerCase(Locale.ROOT);
			namesBuilder.put(value, name);
			fieldsBuilder.put(name, value);
		}
		if (names != null) {
			for (Map.Entry<String, A> entry : names.entrySet()) {
				String name = entry.getKey().toLowerCase(Locale.ROOT);
				A value = entry.getValue();
				if (fieldsBuilder.containsKey(name) && fieldsBuilder.get(name) != value)
					throw new IllegalArgumentException("Name " + name + " was valid for values \"" + fieldsBuilder.get(name) + "\" and \"" + value + "\". This is unsupported");
				fieldsBuilder.put(name, value);
				namesBuilder.put(value, name);
			}
		}
		this.names = namesBuilder.build();
		this.fields = ImmutableMap.copyOf(fieldsBuilder);
	}

	@Override
	@NotNull
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		DataResult<Integer> intValue = ops.getNumberValue(input).map(Number::intValue);
		if (intValue.error().isPresent()) {
			DataResult<String> stringValue = ops.getStringValue(input);
			if (stringValue.error().isPresent())
				return DataResult.error(() -> "Errors reading " + this.values.getClass().getComponentType().getSimpleName() + ": I:" + intValue.error().get() + " S:" + stringValue.error().get());
			String key = stringValue.result().get().toLowerCase(Locale.ROOT); //If this crashes, something is very wrong.
			A value = this.fields.get(key);
			if (value == null)
				return DataResult.error(() -> "Error reading " + this.values.getClass().getComponentType().getSimpleName() + ": No value was found for name: \"" + key + "\"");
			return DataResult.success(Pair.of(value, ops.empty()));
		}
		int integer = intValue.result().get();
		if (integer < 0 || integer >= this.values.length)
			return DataResult.error(() -> "Error reading " + this.values.getClass().getComponentType().getSimpleName() + ": Value " + integer + " was out of range: [0," + (this.values.length - 1) + "]");
		return DataResult.success(Pair.of(this.values[integer], ops.empty()));
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if (!CalioCodecHelper.isDataContext(ops))
			return DataResult.success(ops.createInt(input.ordinal()));
		Collection<String> names = this.names.get(input);
		if (names.isEmpty()) //This should never happen, but this is for safety.
			return DataResult.success(ops.createInt(input.ordinal()));
		String name = names.stream().min(Comparator.comparingInt(String::length)).orElseThrow();
		return DataResult.success(ops.createString(name));
	}
}
