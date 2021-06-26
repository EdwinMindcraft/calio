package io.github.apace100.calio.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.github.apace100.calio.ClassUtil;
import io.github.apace100.calio.FilterableWeightedList;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SerializableDataType<T> implements Codec<T> {

    private final Class<T> dataClass;
    private final Codec<T> codec;
    private final BiConsumer<PacketByteBuf, T> send;
    private final Function<PacketByteBuf, T> receive;
    private final Function<JsonElement, T> read;
    private final Function<T, JsonElement> write;

    public SerializableDataType(Class<T> dataClass,
                                BiConsumer<PacketByteBuf, T> send,
                                Function<PacketByteBuf, T> receive,
                                Function<JsonElement, T> read) {
        this(dataClass, send, receive, read, null);
    }

    public SerializableDataType(Class<T> dataClass,
                                BiConsumer<PacketByteBuf, T> send,
                                Function<PacketByteBuf, T> receive,
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
        this.send = (buf, t) -> buf.encode(this.codec, t);
        this.receive = buf -> buf.decode(codec);
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
        return new SerializableDataType<>(ClassUtil.castClass(List.class), Codec.either(singleDataType, singleDataType.listOf()).xmap(x -> x.map(Arrays::asList, Function.identity()), Either::right));
    }

    public static <T> SerializableDataType<FilterableWeightedList<T>> weightedList(SerializableDataType<T> singleDataType) {
        return new SerializableDataType<>(ClassUtil.castClass(FilterableWeightedList.class), Codec.pair(singleDataType, Codec.INT).listOf().xmap(pairs -> {
            FilterableWeightedList<T> list = new FilterableWeightedList<>();
            pairs.forEach(pair -> list.add(pair.getFirst(), pair.getSecond()));
            return list;
        }, list -> list.entryStream().map(x -> Pair.of(x.getElement(), x.getWeight())).toList()));
    }

    public static <T> SerializableDataType<T> registry(Class<T> dataClass, Registry<T> registry) {
        return wrap(dataClass, SerializableDataTypes.IDENTIFIER, registry::getId, id -> {
            Optional<T> optional = registry.getOrEmpty(id);
            if (optional.isPresent()) {
                return optional.get();
            } else {
                throw new RuntimeException(
                        "Identifier \"" + id + "\" was not registered in registry \"" + registry.getKey().getValue() + "\".");
            }
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

    public void send(PacketByteBuf buffer, Object value) {
        send.accept(buffer, cast(value));
    }

    public T receive(PacketByteBuf buffer) {
        return receive.apply(buffer);
    }

    public T read(JsonElement jsonElement) {
        return read.apply(jsonElement);
    }

    public JsonElement write(T value) {
        return write == null ? JsonNull.INSTANCE : write.apply(value);
    }

    public T cast(Object data) {
        return dataClass.cast(data);
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
            PacketByteBuf buffer = new PacketByteBuf(Unpooled.copiedBuffer(x));
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

        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
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
