// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.
package io.github.edwinmindcraft.calio.api.network;

import com.mojang.serialization.*;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A copy of {@link com.mojang.serialization.codecs.OptionalFieldCodec} that propagates errors if
 * the field isn't missing.
 *
 * @param <A> The type of the codec.
 */
public class PropagatingDefaultedOptionalFieldCodec<A> extends MapCodec<A> {
	private final String name;
	private final Codec<A> elementCodec;
	private final Supplier<A> defaultValue;

	public PropagatingDefaultedOptionalFieldCodec(final String name, final Codec<A> elementCodec, Supplier<A> defaultValue) {
		this.name = name;
		this.elementCodec = elementCodec;
		this.defaultValue = defaultValue;
	}

	@Override
	public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
		final T value = input.get(this.name);
		if (value == null)
			return DataResult.success(this.defaultValue.get());
		return this.elementCodec.parse(ops, value);
	}

	@Override
	public <T> RecordBuilder<T> encode(final A input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
		if (!Objects.equals(this.defaultValue.get(), input))
			return prefix.add(this.name, this.elementCodec.encodeStart(ops, input));
		return prefix;
	}

	@Override
	public <T> Stream<T> keys(final DynamicOps<T> ops) {
		return Stream.of(ops.createString(this.name));
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || this.getClass() != o.getClass())
			return false;
		final PropagatingDefaultedOptionalFieldCodec<?> that = (PropagatingDefaultedOptionalFieldCodec<?>) o;
		return Objects.equals(this.name, that.name) && Objects.equals(this.elementCodec, that.elementCodec);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.elementCodec);
	}

	@Override
	public String toString() {
		return "PropagatingDefaultedOptionalFieldCodec[" + this.name + ": " + this.elementCodec + ']';
	}
}
