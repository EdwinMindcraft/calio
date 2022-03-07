// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.
package io.github.edwinmindcraft.calio.api.network;

import com.mojang.serialization.*;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A copy of {@link com.mojang.serialization.codecs.OptionalFieldCodec} that propagates errors if
 * the field isn't missing.
 * @param <A> The type of the codec.
 */
public class PropagatingOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
    private final String name;
    private final Codec<A> elementCodec;

    public PropagatingOptionalFieldCodec(final String name, final Codec<A> elementCodec) {
        this.name = name;
        this.elementCodec = elementCodec;
    }

    @Override
    public <T> DataResult<Optional<A>> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        final T value = input.get(this.name);
        if (value == null)
            return DataResult.success(Optional.empty());
        return this.elementCodec.parse(ops, value).map(Optional::of);
    }

    @Override
    public <T> RecordBuilder<T> encode(final Optional<A> input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
        if (input.isPresent())
            return prefix.add(this.name, this.elementCodec.encodeStart(ops, input.get()));
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
        final PropagatingOptionalFieldCodec<?> that = (PropagatingOptionalFieldCodec<?>) o;
        return Objects.equals(this.name, that.name) && Objects.equals(this.elementCodec, that.elementCodec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.elementCodec);
    }

    @Override
    public String toString() {
        return "PropagatingOptionalFieldCodec[" + this.name + ": " + this.elementCodec + ']';
    }
}
