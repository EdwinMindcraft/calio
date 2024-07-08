package io.github.edwinmindcraft.calio.api.network;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.FilterableWeightedList;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.network.primitives.BooleanCodec;
import io.github.edwinmindcraft.calio.api.network.primitives.DoubleCodec;
import io.github.edwinmindcraft.calio.api.network.primitives.FloatCodec;
import io.github.edwinmindcraft.calio.api.network.primitives.IntegerCodec;
import io.github.edwinmindcraft.calio.common.access.MappedRegistryAccess;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CalioCodecHelper {
	/**
	 * Defines a boolean codec that support native conversion from
	 * string to boolean for json primitives.
	 */
	public static final Codec<Boolean> BOOL = BooleanCodec.INSTANCE;
	/**
	 * Defines an integer codec that support native conversion from
	 * string to int for json primitives.
	 */
	public static final Codec<Integer> INT = IntegerCodec.INSTANCE;
	/**
	 * Defines a double codec that support native conversion from
	 * string to double for json primitives.
	 */
	public static final Codec<Double> DOUBLE = DoubleCodec.INSTANCE;
	/**
	 * Defines a double codec that support native conversion from
	 * string to double for json primitives.
	 */
	public static final Codec<Float> FLOAT = FloatCodec.INSTANCE;
	public static final Codec<MutableBoolean> MUTABLE_BOOLEAN = BOOL.xmap(MutableBoolean::new, MutableBoolean::toBoolean);
	public static final Codec<MutableInt> MUTABLE_INT = INT.xmap(MutableInt::new, MutableInt::toInteger);
	public static final Codec<MutableDouble> MUTABLE_DOUBLE = DOUBLE.xmap(MutableDouble::new, MutableDouble::toDouble);
	public static final Codec<MutableFloat> MUTABLE_FLOAT = FLOAT.xmap(MutableFloat::new, MutableFloat::toFloat);

	public static <T> MapCodec<Holder<T>> defaultedCodec() {

	}

	/**
	 * Creates a codec of a registry key, used for dynamic registries (Biomes, Dimensions...)
	 *
	 * @param registry The root key of the registry.
	 * @param <T>      The type of the registry.
	 * @return A codec for the given {@link ResourceKey}
	 */
	public static <T> Codec<ResourceKey<T>> resourceKey(ResourceKey<? extends Registry<T>> registry) {
		Validate.notNull(registry, "Registry key cannot be null");
		return SerializableDataTypes.IDENTIFIER.xmap(x -> ResourceKey.create(registry, x), ResourceKey::location);
	}

	/**
	 * Creates a typed codec for a {@link FilterableWeightedList} from the given codec.
	 *
	 * @param codec The codec to make a weighted list of.
	 * @param <T>   The type of the codec.
	 * @return A new weighted list codec.
	 * @see net.minecraft.world.entity.ai.behavior.ShufflingList#codec(Codec) for minecraft's weighted list.
	 */
	public static <T> Codec<FilterableWeightedList<T>> weightedListOf(Codec<T> codec) {
		Validate.notNull(codec, "Codec cannot be null");
		return RecordCodecBuilder.<Pair<T, Integer>>create(instance -> instance.group(
				codec.fieldOf("element").forGetter(Pair::getFirst),
				CalioCodecHelper.INT.fieldOf("weight").forGetter(Pair::getSecond)
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
	 * @param codec The codec to make a list of.
	 * @param <T>   The type of the codec.
	 * @return A new list codec.
	 */
	public static <T> Codec<List<T>> listOf(Codec<T> codec) {
		Validate.notNull(codec, "Codec cannot be null");
		return new UnitListCodec<>(codec);
	}

	/**
	 * Returns a list codec of optional elements, the resulting codec will remove
	 * all fields that would match {@link Optional#isEmpty()}.
	 *
	 * @param codec The codec to make a list of.
	 * @param <T>   The type of the codec.
	 * @return A new list codec.
	 */
	public static <T> Codec<List<T>> optionalListOf(Codec<Optional<T>> codec) {
		Validate.notNull(codec, "Codec cannot be null");
		return CalioCodecHelper.listOf(codec).xmap(x -> x.stream().flatMap(Optional::stream).collect(Collectors.toList()),
				objects -> objects.stream().filter(Objects::nonNull).map(Optional::of).collect(Collectors.toList()));
	}

	/**
	 * Create a representation of a list with a single field and a list field.
	 * Both are effectively interchangeable in the implementation to not make
	 * the system more complicated for the end user.
	 *
	 * @param codec    The codec to create a list of
	 * @param singular The singular term for the field
	 * @param plural   The plural term for the field
	 * @param <T>      The type of the element of the list.
	 * @return The created codec
	 */
	public static <T> MapCodec<List<T>> listOf(Codec<T> codec, String singular, String plural) {
		Validate.notNull(codec, "Codec cannot be null");
		Validate.notNull(singular, "Singular cannot be null");
		Validate.notNull(plural, "Plural cannot be null");
		Codec<List<T>> listCodec = listOf(codec);
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				ExtraCodecs.strictOptionalField(listCodec, singular, ImmutableList.of()).forGetter(x -> x.size() == 1 ? x : ImmutableList.of()),
				ExtraCodecs.strictOptionalField(listCodec, plural, ImmutableList.of()).forGetter(x -> x.size() == 1 ? ImmutableList.of() : x)
		).apply(instance, (ls1, ls2) -> ImmutableList.<T>builder().addAll(ls1).addAll(ls2).build()));
	}

	/**
	 * A utility function to create a codec that accepts {@link Set Sets} as an input
	 * instead of a {@link List}.
	 *
	 * @param codec The codec to make a set of.
	 * @param <T>   The type of the codec
	 * @return A new set codec.
	 */
	public static <T> Codec<Set<T>> setOf(Codec<T> codec) {
		Validate.notNull(codec, "Codec cannot be null");
		return CalioCodecHelper.listOf(codec).xmap(HashSet::new, ArrayList::new);
	}

	public static <T> Codec<Holder<T>> holder(Supplier<Registry<T>> access, Codec<ResourceLocation> reference, Codec<T> direct) {
		return new HolderCodec<>(direct, reference, access);
	}

	public static <T> Codec<HolderSet<T>> holderSet(Supplier<Registry<T>> access, Codec<TagKey<T>> tag, Codec<Holder<T>> holder) {
		return new HolderSetCodec<>(access, holder, tag);
	}

	public static <T> Codec<Holder<T>> holderRef(Supplier<Registry<T>> access, ResourceKey<Registry<T>> key, Codec<ResourceLocation> reference) {
		return reference.flatXmap(id -> {
			if (access.get() instanceof MappedRegistry<T> mapped) {
				return DataResult.success(((MappedRegistryAccess<T>) mapped).calio$getOrCreateHolderOrThrow(ResourceKey.create(key, id)));
			}
			return DataResult.error(() -> "Failed to get holder from non MappedRegistry '" + key.location() + "'.", Holder.direct(null));
		}, h -> {
			try {
				return h.unwrap().map(x -> DataResult.success(x.location()), t -> access.get().getResourceKey(t).map(ResourceKey::location).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Key not in registry.")));
			} catch (IllegalStateException e) {
				return DataResult.error(() -> "Completely unbound input.");
			}
		});
	}

	public static <T> Codec<Holder<T>> holderRef(ResourceKey<Registry<T>> key, Codec<ResourceLocation> reference) {
		Supplier<Registry<T>> supplier = () -> CalioAPI.getRegistryAccess().registryOrThrow(key);
		return holderRef(supplier, key, reference);
	}

	public static <T> CodecSet<T> codecSet(Supplier<Registry<T>> access, ResourceKey<Registry<T>> key, Codec<ResourceLocation> reference, Codec<T> direct) {
		Codec<Holder<T>> holder = holder(access, reference, direct);
		Codec<Holder<T>> holderRef = holderRef(access, key, reference);
		Codec<TagKey<T>> directTag = reference.xmap(location -> TagKey.create(key, location), TagKey::location);
		//FIXME Add support for * operator.
		Codec<TagKey<T>> hashedTag = Codec.STRING.comapFlatMap(string -> string.startsWith("#") ?
				ResourceLocation.read(string.substring(1)).map(loc -> TagKey.create(key, loc)) :
				DataResult.error(() -> "Not a tag id"), tag -> "#" + tag.location());
		Codec<HolderSet<T>> directSet = holderSet(access, directTag, holder);
		Codec<HolderSet<T>> hashedSet = holderSet(access, hashedTag, holder);
		Codec<List<HolderSet<T>>> set = listOf(hashedSet);
		return new CodecSet<>(holder, holderRef, directTag, hashedTag, directSet, hashedSet, set);
	}

	public static <T> CodecSet<T> forDynamicRegistry(ResourceKey<Registry<T>> key, Codec<ResourceLocation> reference, Codec<T> direct) {
		Supplier<Registry<T>> supplier = () -> CalioAPI.getRegistryAccess().registryOrThrow(key);
		return codecSet(supplier, key, reference, direct);
	}

	public static MapCodec<Vec3> vec3d(String xName, String yName, String zName) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				ExtraCodecs.strictOptionalField(CalioCodecHelper.DOUBLE, xName, 0.0).forGetter(Vec3::x),
				ExtraCodecs.strictOptionalField(CalioCodecHelper.DOUBLE, yName, 0.0).forGetter(Vec3::y),
				ExtraCodecs.strictOptionalField(CalioCodecHelper.DOUBLE, zName, 0.0).forGetter(Vec3::z)
		).apply(instance, Vec3::new));
	}

	public static MapCodec<Vector3f> vec3f(String xName, String yName, String zName) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				ExtraCodecs.strictOptionalField(CalioCodecHelper.FLOAT, xName, 0.0F).forGetter(Vector3f::x),
				ExtraCodecs.strictOptionalField(CalioCodecHelper.FLOAT, yName, 0.0F).forGetter(Vector3f::y),
				ExtraCodecs.strictOptionalField(CalioCodecHelper.FLOAT, zName, 0.0F).forGetter(Vector3f::z)
		).apply(instance, Vector3f::new));
	}

	public static MapCodec<Vec3> vec3d(String prefix) {
		return vec3d(prefix + "x", prefix + "y", prefix + "z");
	}

	public static MapCodec<BlockPos> blockPos(String xName, String yName, String zName) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				ExtraCodecs.strictOptionalField(CalioCodecHelper.INT, xName, 0).forGetter(BlockPos::getX),
				ExtraCodecs.strictOptionalField(CalioCodecHelper.INT, yName, 0).forGetter(BlockPos::getY),
				ExtraCodecs.strictOptionalField(CalioCodecHelper.INT, zName, 0).forGetter(BlockPos::getZ)
		).apply(instance, BlockPos::new));
	}

	public static MapCodec<BlockPos> blockPos(String prefix) {
		return blockPos(prefix + "x", prefix + "y", prefix + "z");
	}

	public static final MapCodec<Vec3> VEC3D = vec3d("x", "y", "z");
	public static final MapCodec<Vector3f> VEC3F = vec3f("x", "y", "z");
	public static final MapCodec<BlockPos> BLOCK_POS = blockPos("x", "y", "z");

	public static <T> CodecJsonAdapter<T> jsonAdapter(Codec<T> input) {
		return new CodecJsonAdapter<>(input);
	}

	public static boolean isDataContext(DynamicOps<?> ops) {
		return ops instanceof JsonOps && !ops.compressMaps();
	}

	public static class CodecJsonAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

		private final Codec<T> codec;

		private CodecJsonAdapter(Codec<T> codec) {
			this.codec = codec;
		}

		@Override
		public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return this.codec.decode(JsonOps.INSTANCE, json).getOrThrow(false, s -> {
				throw new JsonParseException("Found error: " + s);
			}).getFirst();
		}

		@Override
		public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
			return this.codec.encodeStart(JsonOps.INSTANCE, src).getOrThrow(false, s -> {
			});
		}
	}
}
