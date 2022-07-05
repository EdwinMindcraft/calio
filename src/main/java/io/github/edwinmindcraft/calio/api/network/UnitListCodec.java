package io.github.edwinmindcraft.calio.api.network;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;
import net.minecraft.nbt.NbtOps;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
		//NBT needs absolute consistency over the content of lists.
		//Which means larger packets.
		if (input.size() == 1 && !(ops instanceof NbtOps))
			return this.elementCodec.encode(input.get(0), ops, prefix);
		DataResult<T> result = DataResult.error("Failed to serialize list.");
		try {
			final ListBuilder<T> builder = ops.listBuilder();
			for (final A a : input) {
				builder.add(this.elementCodec.encodeStart(ops, a));
			}
			result = builder.build(prefix);
		} catch (Exception e) {
			//NBT Catch-all, if anything breaks above, just consider it a list.
			//This is significantly slower, but I don't really care at this point.
		}
		if (result.result().isEmpty()) {
			final RecordBuilder<T> builder = ops.mapBuilder();
			for (int i = 0; i < input.size(); i++)
				builder.add(Integer.toString(i), this.elementCodec.encodeStart(ops, input.get(i)));
			result = builder.build(prefix);
		}
		return result;
	}

	@Override
	public <T> DataResult<Pair<List<A>, T>> decode(final DynamicOps<T> ops, final T input) {
		DataResult<Consumer<Consumer<T>>> list = ops.getList(input);
		if (list.error().isPresent()) {
			DataResult<MapLike<T>> map1 = ops.getMap(input);
			if (map1.error().isEmpty()) {
				list = map1.flatMap(map -> {
					List<Pair<DataResult<Integer>, T>> pairs = map.entries().map(value -> Pair.of(ops.getStringValue(value.getFirst()).flatMap(x -> {
						try {
							return DataResult.success(Integer.valueOf(x));
						} catch (NumberFormatException e) {
							return DataResult.error("At element: " + x + ": " + e.getMessage());
						}
					}), value.getSecond())).toList();
					Optional<Pair<DataResult<Integer>, T>> first = pairs.stream().filter(pair -> pair.getFirst().error().isPresent()).findFirst();
					return first.<DataResult<Consumer<Consumer<T>>>>map(dataResultTPair -> DataResult.error(dataResultTPair.getFirst().error().get().message()))
							.orElseGet(() -> DataResult.success(consumer -> pairs.stream().sorted(Comparator.comparingInt(value -> value.getFirst().result().get())).map(Pair::getSecond).forEach(consumer)));
				});
			}
		}
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
		return list.error().isPresent() ? decode : listDecode;
	}

	@Override
	public String toString() {
		return "UnitList[" + this.elementCodec.toString() + "]";
	}
}
