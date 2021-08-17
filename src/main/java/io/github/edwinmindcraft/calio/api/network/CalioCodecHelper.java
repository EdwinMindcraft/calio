package io.github.edwinmindcraft.calio.api.network;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.FilterableWeightedList;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CalioCodecHelper {
	/**
	 * Creates a codec of a registry key, used for dynamic registries (Biomes, Dimensions...)
	 *
	 * @param registry The root key of the registry.
	 * @param <T>      The type of the registry.
	 * @return A codec for the given {@link ResourceKey}
	 */
	public static <T> Codec<ResourceKey<T>> resourceKey(ResourceKey<? extends Registry<T>> registry) {
		return SerializableDataTypes.IDENTIFIER.xmap(x -> ResourceKey.create(registry, x), ResourceKey::location);
	}

	/**
	 * Creates a typed codec for a {@link FilterableWeightedList} from the given codec.
	 *
	 * @param source The codec to make a weighted list of.
	 * @param <T>    The type of the codec.
	 * @return A new weighted list codec.
	 * @see net.minecraft.world.entity.ai.behavior.ShufflingList#codec(Codec) for minecraft's weighted list.
	 */
	public static <T> Codec<FilterableWeightedList<T>> weightedListOf(Codec<T> source) {
		return RecordCodecBuilder.<Pair<T, Integer>>create(instance -> instance.group(
				source.fieldOf("element").forGetter(Pair::getFirst),
				Codec.INT.fieldOf("weight").forGetter(Pair::getSecond)
		).apply(instance, Pair::of)).listOf().xmap(pairs -> {
			FilterableWeightedList<T> list = new FilterableWeightedList<>();
			pairs.forEach(pair -> list.add(pair.getFirst(), pair.getSecond()));
			return list;
		}, list -> list.entryStream().map(x -> Pair.of(x.getData(), x.getWeight())).toList());
	}

	/**
	 * Returns a list codec that will accept either a single element, or
	 * an array of element.
	 *
	 * @param source The codec to make a list of.
	 * @param <T>    The type of the codec.
	 * @return A new list codec.
	 */
	public static <T> Codec<List<T>> listOf(Codec<T> source) {
		return Codec.either(source, source.listOf()).xmap(x -> x.map(Arrays::asList, Function.identity()), Either::right);
	}

	/**
	 * Returns a list codec of optional elements, the resulting codec will remove
	 * all fields that would match {@link Optional#isEmpty()}.
	 *
	 * @param source The codec to make a list of.
	 * @param <T>    The type of the codec.
	 * @return A new list codec.
	 */
	public static <T> Codec<List<T>> optionalListOf(Codec<Optional<T>> source) {
		return Codec.either(source, source.listOf()).xmap(x -> x.map(Optional::stream, y -> y.stream().flatMap(Optional::stream)).collect(Collectors.toList()),
				objects -> Either.right(objects.stream().filter(Objects::nonNull).map(Optional::of).collect(Collectors.toList())));
	}

	/**
	 * A utility function to create a codec that accepts {@link Set Sets} as an input
	 * instead of a {@link List}.
	 *
	 * @param source The codec to make a set of.
	 * @param <T>    The type of the codec
	 * @return A new set codec.
	 */
	public static <T> Codec<Set<T>> setOf(Codec<T> source) {
		return Codec.either(source, source.listOf()).xmap(x -> x.map(ImmutableSet::of, HashSet::new), x -> Either.right(new ArrayList<>(x)));
	}
}
