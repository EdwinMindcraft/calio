package io.github.edwinmindcraft.calio.api.network;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.FilterableWeightedList;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CalioCodecHelper {
	/**
	 * Creates a codec of a registry key, used for dynamic registries (Biomes, Dimensions...)
	 *
	 * @param registry The root key of the registry.
	 * @param <T>      The type of the registry.
	 *
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
	 *
	 * @return A new weighted list codec.
	 *
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
	 *
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
	 *
	 * @return A new list codec.
	 */
	public static <T> Codec<List<T>> optionalListOf(Codec<Optional<T>> source) {
		return Codec.either(source, source.listOf()).xmap(x -> x.map(Optional::stream, y -> y.stream().flatMap(Optional::stream)).collect(Collectors.toList()),
				objects -> Either.right(objects.stream().filter(Objects::nonNull).map(Optional::of).collect(Collectors.toList())));
	}

	public static <T> MapCodec<List<T>> listOf(Codec<T> source, String singular, String plural) {
		return Codec.mapEither(source.optionalFieldOf(singular), listOf(source).optionalFieldOf(plural, ImmutableList.of())).xmap(x -> x.map(opt -> opt.stream().toList(), Function.identity()), ls -> {
			if (ls.isEmpty())
				return Either.left(Optional.empty());
			if (ls.size() == 1)
				return Either.left(Optional.of(ls.get(0)));
			return Either.right(ls);
		});
	}

	/**
	 * A utility function to create a codec that accepts {@link Set Sets} as an input
	 * instead of a {@link List}.
	 *
	 * @param source The codec to make a set of.
	 * @param <T>    The type of the codec
	 *
	 * @return A new set codec.
	 */
	public static <T> Codec<Set<T>> setOf(Codec<T> source) {
		return Codec.either(source, source.listOf()).xmap(x -> x.map(ImmutableSet::of, HashSet::new), x -> Either.right(new ArrayList<>(x)));
	}

	public static final Codec<Component> COMPONENT_CODEC = new IContextAwareCodec<>() {
		@Override
		public JsonElement asJson(Component input) {
			return Component.Serializer.toJsonTree(input);
		}

		@Override
		public Component fromJson(JsonElement input) {
			return Component.Serializer.fromJson(input);
		}

		@Override
		public void encode(Component input, FriendlyByteBuf buf) {
			buf.writeComponent(input);
		}

		@Override
		public Component decode(FriendlyByteBuf buf) {
			return buf.readComponent();
		}
	};

	public static <T> CodecJsonAdapter<T> jsonAdapter(Codec<T> input) {
		return new CodecJsonAdapter<>(input);
	}

	public static class CodecJsonAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

		private final Codec<T> codec;

		private CodecJsonAdapter(Codec<T> codec) {this.codec = codec;}

		@Override
		public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return this.codec.decode(JsonOps.INSTANCE, json).getOrThrow(false, s -> {
				throw new JsonParseException("Found error: " + s);
			}).getFirst();
		}

		@Override
		public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
			return this.codec.encodeStart(JsonOps.INSTANCE, src).getOrThrow(false, s -> {});
		}
	}
}
