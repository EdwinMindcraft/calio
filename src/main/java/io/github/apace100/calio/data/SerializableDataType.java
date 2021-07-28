package io.github.apace100.calio.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.experimental.calio.api.network.CalioCodecHelper;
import io.github.apace100.calio.ClassUtil;
import io.github.apace100.calio.FilterableWeightedList;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SerializableDataType<T> implements Codec<T> {

	private final Class<T> dataClass;
	private final Codec<T> codec;
	private final BiConsumer<FriendlyByteBuf, T> send;
	private final Function<FriendlyByteBuf, T> receive;
	private final Function<JsonElement, T> read;
	private final Function<T, JsonElement> write;

	public SerializableDataType(Class<T> dataClass,
								BiConsumer<FriendlyByteBuf, T> send,
								Function<FriendlyByteBuf, T> receive,
								Function<JsonElement, T> read) {
		this(dataClass, send, receive, read, null);
	}

	public SerializableDataType(Class<T> dataClass,
								BiConsumer<FriendlyByteBuf, T> send,
								Function<FriendlyByteBuf, T> receive,
								Function<JsonElement, T> read,
								Function<T, JsonElement> write) {
		this.dataClass = dataClass;
		this.send = send;
		this.receive = receive;
		this.read = read;
		this.write = write;
		this.codec = null;
	}

	public SerializableDataType(Class<T> dataClass, Codec<T> codec) {
		this.dataClass = dataClass;
		this.codec = codec;
		this.send = (buf, t) -> buf.writeWithCodec(this.codec, t);
		this.receive = buf -> buf.readWithCodec(codec);
		this.read = jsonElement -> codec.decode(JsonOps.INSTANCE, jsonElement).map(Pair::getFirst).getOrThrow(false, s -> {
			throw new JsonParseException(s);
		});
		this.write = t -> codec.encodeStart(JsonOps.INSTANCE, t).getOrThrow(false, s -> {
		});
	}

	public static <T> boolean isDataContext(DynamicOps<T> ops) {
		return ops.compressMaps() && ops instanceof JsonElement;
	}

	public static <T> SerializableDataType<List<T>> list(SerializableDataType<T> singleDataType) {
		return new SerializableDataType<>(ClassUtil.castClass(List.class), CalioCodecHelper.listOf(singleDataType));
	}

	public static <T> SerializableDataType<FilterableWeightedList<T>> weightedList(SerializableDataType<T> singleDataType) {
		return new SerializableDataType<>(ClassUtil.castClass(FilterableWeightedList.class), CalioCodecHelper.weightedListOf(singleDataType));
	}

	public static <T> SerializableDataType<T> registry(Class<T> dataClass, Registry<T> registry) {
		return wrap(dataClass, SerializableDataTypes.IDENTIFIER, registry::getKey, id -> registry.getOptional(id).orElseThrow(() -> new RuntimeException("Identifier \"" + id + "\" was not registered in registry \"" + registry.key().location() + "\".")));
	}

	public static <T extends ForgeRegistryEntry<T>> SerializableDataType<T> registry(Class<T> dataClass, IForgeRegistry<T> registry) {
		return wrap(dataClass, SerializableDataTypes.IDENTIFIER, registry::getKey, id -> {
			T value = registry.getValue(id);
			if (value == null || !Objects.equals(id, value.getRegistryName()))
				throw new RuntimeException("Identifier \"" + id + "\" was not registered in registry \"" + registry.getRegistryName() + "\".");
			return value;
		});
	}

	public static <T> SerializableDataType<T> compound(Class<T> dataClass, SerializableData data, Function<SerializableData.Instance, T> toInstance, BiFunction<SerializableData, T, SerializableData.Instance> toData) {
		return new SerializableDataType<>(dataClass, data.codec().xmap(toInstance, t -> toData.apply(data, t)));
	}

	public static <T extends Enum<T>> SerializableDataType<T> enumValue(Class<T> dataClass) {
		return enumValue(dataClass, null);
	}

	public static <T extends Enum<T>> SerializableDataType<T> enumValue(Class<T> dataClass, HashMap<String, T> additionalMap) {
		T[] enumConstants = dataClass.getEnumConstants();
		Codec<T> ordinalCodec = Codec.intRange(0, enumConstants.length - 1).xmap(i -> enumConstants[i], Enum::ordinal);
		BiMap<String, T> map = HashBiMap.create();
		for (T enumConstant : enumConstants)
			map.put(enumConstant.name().toLowerCase(Locale.ROOT), enumConstant);
		if (additionalMap != null)
			additionalMap.forEach((s, t) -> map.put(s.toLowerCase(Locale.ROOT), t));
		Codec<T> stringCodec = Codec.STRING.xmap(x -> map.get(x.toLowerCase(Locale.ROOT)), x -> map.inverse().get(x));
		return new SerializableDataType<>(dataClass, Codec.either(ordinalCodec, stringCodec).xmap(either -> either.map(Function.identity(), Function.identity()), Either::left));
	}

	public static <T> SerializableDataType<T> mapped(Class<T> dataClass, BiMap<String, T> map) {
		return new SerializableDataType<>(dataClass, Codec.STRING.xmap(map::get, map.inverse()::get));
	}

	public static <T, U> SerializableDataType<T> wrap(Class<T> dataClass, SerializableDataType<U> base, Function<T, U> toFunction, Function<U, T> fromFunction) {
		return base.codec()
				.map(codec -> new SerializableDataType<>(dataClass, codec.xmap(fromFunction, toFunction)))
				.orElseGet(() -> new SerializableDataType<>(dataClass,
						(buf, t) -> base.send(buf, toFunction.apply(t)),
						(buf) -> fromFunction.apply(base.receive(buf)),
						(json) -> fromFunction.apply(base.read(json)),
						base.write != null ? (t) -> base.write(toFunction.apply(t)) : null));
	}

	public void send(FriendlyByteBuf buffer, Object value) {
		this.send.accept(buffer, this.cast(value));
	}

	public T receive(FriendlyByteBuf buffer) {
		return this.receive.apply(buffer);
	}

	public T read(JsonElement jsonElement) {
		return this.read.apply(jsonElement);
	}

	public JsonElement write(T value) {
		return this.write == null ? JsonNull.INSTANCE : this.write.apply(value);
	}

	public T cast(Object data) {
		return this.dataClass.cast(data);
	}

	public Optional<Codec<T>> codec() {
		return Optional.ofNullable(this.codec);
	}

	@Override
	public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
		if (this.codec != null)
			return this.codec.decode(ops, input);
		if (isDataContext(ops)) {
			JsonElement jsonElement = ops.convertTo(JsonOps.INSTANCE, input);
			try {
				return DataResult.success(Pair.of(this.read.apply(jsonElement), ops.empty()));
			} catch (Exception e) {
				return DataResult.error(e.getMessage());
			}
		}

		return ops.getByteBuffer(input).flatMap(x -> {
			FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.copiedBuffer(x));
			try {
				return DataResult.success(Pair.of(this.receive.apply(buffer), ops.empty()));
			} catch (Exception e) {
				return DataResult.error(e.getMessage());
			} finally {
				buffer.release();
			}
		});
	}

	@Override
	public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
		if (this.codec != null)
			return this.codec.encode(input, ops, prefix);
		if (isDataContext(ops)) {
			if (this.write == null)
				return DataResult.error("Writing is unsupported for type: " + this.dataClass.getSimpleName());
			try {
				return DataResult.success(JsonOps.INSTANCE.convertTo(ops, this.write.apply(input)));
			} catch (Exception e) {
				return DataResult.error(e.getMessage());
			}
		}

		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		try {
			this.receive.apply(buffer);
			return DataResult.success(ops.createByteList(buffer.nioBuffer()));
		} catch (Exception e) {
			return DataResult.error(e.getMessage());
		} finally {
			buffer.release();
		}
	}
}
