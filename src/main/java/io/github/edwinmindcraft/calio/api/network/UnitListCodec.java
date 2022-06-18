package io.github.edwinmindcraft.calio.api.network;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class UnitListCodec<A> implements Codec<List<A>> {
	private final Codec<A> elementCodec;

	public UnitListCodec(Codec<A> elementCodec) {
		this.elementCodec = elementCodec;
	}

	@Override
	public <T> DataResult<T> encode(final List<A> input, final DynamicOps<T> ops, final T prefix) {
		if (input.size() == 1)
			return this.elementCodec.encode(input.get(0), ops, prefix);
		final ListBuilder<T> builder = ops.listBuilder();

		for (final A a : input) {
			builder.add(this.elementCodec.encodeStart(ops, a));
		}

		return builder.build(prefix);
	}

	@Override
	public <T> DataResult<Pair<List<A>, T>> decode(final DynamicOps<T> ops, final T input) {
		DataResult<Consumer<Consumer<T>>> list = ops.getList(input);
		DataResult<Pair<List<A>, T>> listDecode = list.setLifecycle(Lifecycle.stable()).flatMap(stream -> {
			final ImmutableList.Builder<A> read = ImmutableList.builder();
			final Stream.Builder<T> failed = Stream.builder();
			final AtomicReference<DataResult<Unit>> result = new AtomicReference<>(DataResult.success(Unit.INSTANCE, Lifecycle.stable()));

			stream.accept(t -> {
				final DataResult<Pair<A, T>> element = this.elementCodec.decode(ops, t);
				element.error().ifPresent(e -> failed.add(t));
				result.setPlain(result.getPlain().apply2stable((r, v) -> {
					read.add(v.getFirst());
					return r;
				}, element));
			});

			final ImmutableList<A> elements = read.build();
			final T errors = ops.createList(failed.build());

			final Pair<List<A>, T> pair = Pair.of(elements, errors);

			return result.getPlain().map(unit -> pair).setPartial(pair);
		});
		if (listDecode.error().isEmpty())
			return listDecode;
		DataResult<Pair<List<A>, T>> decode = this.elementCodec.decode(ops, input).map(pair -> pair.mapFirst(ImmutableList::of));
		if (decode.error().isEmpty())
			return decode;
		//Attempt to write the correct version of the error based on the context.
		//Lists of lists may be broken.
		return list.error().isPresent() ? listDecode : decode;
	}

	@Override
	public String toString() {
		return "UnitList[" + this.elementCodec.toString() + "]";
	}
}
