package io.github.edwinmindcraft.calio.api.network;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.FilterableWeightedList;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
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
		return Codec.either(source, source.listOf()).xmap(x -> x.map(Arrays::asList, Function.identity()), x -> x.size() == 1 ? Either.left(x.get(0)) : Either.right(x));
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
		return CalioCodecHelper.listOf(source).xmap(x -> x.stream().flatMap(Optional::stream).collect(Collectors.toList()),
				objects -> objects.stream().filter(Objects::nonNull).map(Optional::of).collect(Collectors.toList()));
	}

	public static <T> MapCodec<List<T>> listOf(Codec<T> source, String singular, String plural) {
		Codec<List<T>> listCodec = listOf(source);
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				optionalField(listCodec, singular, ImmutableList.of()).forGetter(x -> x.size() == 1 ? x : ImmutableList.of()),
				optionalField(listCodec, plural, ImmutableList.of()).forGetter(x -> x.size() == 1 ? ImmutableList.of() : x)
		).apply(instance, (ls1, ls2) -> ImmutableList.<T>builder().addAll(ls1).addAll(ls2).build()));
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
		return CalioCodecHelper.listOf(source).xmap(HashSet::new, ArrayList::new);
	}

	public static <T> Codec<Holder<T>> holder(Supplier<Registry<T>> access, Codec<ResourceLocation> reference, Codec<T> direct) {
		return new HolderCodec<>(direct, reference, access);
	}

	public static <T> Codec<HolderSet<T>> holderSet(Supplier<Registry<T>> access, Codec<TagKey<T>> tag, Codec<Holder<T>> holder) {
		return new HolderSetCodec<>(access, holder, tag);
	}

	public static <T> CodecSet<T> codecSet(Supplier<Registry<T>> access, ResourceKey<Registry<T>> key, Codec<ResourceLocation> reference, Codec<T> direct) {
		Codec<Holder<T>> holder = holder(access, reference, direct);
		Codec<Holder<T>> holderRef = reference.flatComapMap(id -> access.get().getOrCreateHolder(ResourceKey.create(key, id)), h -> h.unwrap().map(x -> DataResult.success(x.location()), t -> access.get().getResourceKey(t).map(ResourceKey::location).map(DataResult::success).orElseGet(() -> DataResult.error("Key not in registry."))));
		Codec<TagKey<T>> directTag = reference.xmap(location -> TagKey.create(key, location), TagKey::location);
		//FIXME Add support for * operator.
		Codec<TagKey<T>> hashedTag = Codec.STRING.comapFlatMap(string -> string.startsWith("#") ?
				ResourceLocation.read(string.substring(1)).map(loc -> TagKey.create(key, loc)) :
				DataResult.error("Not a tag id"), tag -> "#" + tag.location());
		Codec<HolderSet<T>> directSet = holderSet(access, directTag, holder);
		Codec<HolderSet<T>> hashedSet = holderSet(access, hashedTag, holder);
		Codec<List<HolderSet<T>>> set = listOf(hashedSet);
		return new CodecSet<>(holder, holderRef, directTag, hashedTag, directSet, hashedSet, set);
	}

	public static <T> CodecSet<T> forDynamicRegistry(ResourceKey<Registry<T>> key, Codec<ResourceLocation> reference, Codec<T> direct) {
		Supplier<Registry<T>> supplier = () -> CalioAPI.getDynamicRegistries().get(key);
		return codecSet(supplier, key, reference, direct);
	}

	public static <A> PropagatingOptionalFieldCodec<A> optionalField(Codec<A> codec, String name) {
		return new PropagatingOptionalFieldCodec<>(name, codec);
	}

	public static <A> PropagatingDefaultedOptionalFieldCodec<A> optionalField(Codec<A> codec, String name, A defaultValue) {
		return new PropagatingDefaultedOptionalFieldCodec<>(name, codec, () -> defaultValue);
	}

	public static <A> PropagatingDefaultedOptionalFieldCodec<A> optionalField(Codec<A> codec, String name, Supplier<A> defaultValue) {
		return new PropagatingDefaultedOptionalFieldCodec<>(name, codec, defaultValue);
	}

	public static <A extends IForgeRegistryEntry<A>> PropagatingDefaultedOptionalFieldCodec<Holder<A>> registryDefaultedField(Codec<Holder<A>> codec, String name, ResourceKey<Registry<A>> registry, Supplier<IForgeRegistry<A>> builtin) {
		Supplier<Holder<A>> supplier = () -> CalioAPI.getDynamicRegistries().get(registry) instanceof DefaultedRegistry<A> def ? def.getHolderOrThrow(ResourceKey.create(registry, def.getDefaultKey())) : builtin.get().getHolder(builtin.get().getDefaultKey()).orElseThrow();
		return optionalField(codec, name, supplier);
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

	public static MapCodec<Vec3> vec3d(String xName, String yName, String zName) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				CalioCodecHelper.optionalField(Codec.DOUBLE, xName, 0.0).forGetter(Vec3::x),
				CalioCodecHelper.optionalField(Codec.DOUBLE, yName, 0.0).forGetter(Vec3::y),
				CalioCodecHelper.optionalField(Codec.DOUBLE, zName, 0.0).forGetter(Vec3::z)
		).apply(instance, Vec3::new));
	}

	public static MapCodec<Vector3f> vec3f(String xName, String yName, String zName) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				CalioCodecHelper.optionalField(Codec.FLOAT, xName, 0.0F).forGetter(Vector3f::x),
				CalioCodecHelper.optionalField(Codec.FLOAT, yName, 0.0F).forGetter(Vector3f::y),
				CalioCodecHelper.optionalField(Codec.FLOAT, zName, 0.0F).forGetter(Vector3f::z)
		).apply(instance, Vector3f::new));
	}

	public static MapCodec<Vec3> vec3d(String prefix) {
		return vec3d(prefix + "x", prefix + "y", prefix + "z");
	}

	public static MapCodec<BlockPos> blockPos(String xName, String yName, String zName) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				CalioCodecHelper.optionalField(Codec.INT, xName, 0).forGetter(BlockPos::getX),
				CalioCodecHelper.optionalField(Codec.INT, yName, 0).forGetter(BlockPos::getY),
				CalioCodecHelper.optionalField(Codec.INT, zName, 0).forGetter(BlockPos::getZ)
		).apply(instance, BlockPos::new));
	}

	public static MapCodec<BlockPos> blockPos(String prefix) {
		return blockPos(prefix + "x", prefix + "y", prefix + "z");
	}

	public static MapCodec<Vec3> VEC3D = vec3d("x", "y", "z");
	public static MapCodec<Vector3f> VEC3F = vec3f("x", "y", "z");
	public static MapCodec<BlockPos> BLOCK_POS = blockPos("x", "y", "z");

	public static <T> CodecJsonAdapter<T> jsonAdapter(Codec<T> input) {
		return new CodecJsonAdapter<>(input);
	}

	public static boolean isDataContext(DynamicOps<?> ops) {
		return ops instanceof JsonOps && !ops.compressMaps();
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
