package io.github.apace100.calio.data;

import com.google.common.collect.BiMap;
import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.github.apace100.calio.ClassUtil;
import io.github.apace100.calio.FilterableWeightedList;
import io.github.apace100.calio.SerializationHelper;
import io.github.apace100.calio.util.ArgumentWrapper;
import io.github.apace100.calio.util.TagLike;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import io.github.edwinmindcraft.calio.api.network.EnumValueCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SerializableDataType<T> implements Codec<T> {

    private final Class<T> dataClass;
    private final Codec<T> codec;
    private final BiConsumer<RegistryFriendlyByteBuf, T> send;
    private final Function<RegistryFriendlyByteBuf, T> receive;
    private final Function<JsonElement, T> read;
    private final Function<T, JsonElement> write;

    public SerializableDataType(Class<T> dataClass,
                                BiConsumer<RegistryFriendlyByteBuf, T> send,
                                Function<RegistryFriendlyByteBuf, T> receive,
                                Function<JsonElement, T> read,
                                Function<T, JsonElement> write) {
        this.dataClass = dataClass;
        this.send = send;
        this.receive = receive;
        this.read = read;
        this.write = write;
        this.codec = null;
    }

    // FORGE: Codec
    public SerializableDataType(Class<T> dataClass, Codec<T> codec) {
        this(dataClass, codec, ByteBufCodecs.fromCodec(codec).cast());
    }

    public SerializableDataType(Class<T> dataClass, Codec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        this.dataClass = dataClass;
        this.codec = codec;
        this.send = streamCodec::encode;
        this.receive = streamCodec::decode;
        this.read = jsonElement -> codec.decode(JsonOps.INSTANCE, jsonElement).map(Pair::getFirst).getOrThrow();
        this.write = t -> codec.encodeStart(JsonOps.INSTANCE, t).getOrThrow();
    }

    public static <T> boolean isDataContext(DynamicOps<T> ops) {
        return !ops.compressMaps() && ops instanceof JsonOps;
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

    public static <T> SerializableDataType<T> compound(Class<T> dataClass, SerializableData data, Function<SerializableData.Instance, T> toInstance, BiFunction<SerializableData, T, SerializableData.Instance> toData) {
        return new SerializableDataType<>(dataClass, data.codec().xmap(toInstance, t -> toData.apply(data, t)));
    }

    public static <T extends Enum<T>> SerializableDataType<T> enumValue(Class<T> dataClass) {
        return enumValue(dataClass, (HashMap<String, T>) null);
    }

    public static <T extends Enum<T>> SerializableDataType<T> enumValue(Class<T> dataClass, @Nullable HashMap<String, T> additionalMap) {
        return new SerializableDataType<>(dataClass, new EnumValueCodec<>(dataClass.getEnumConstants(), additionalMap));
    }

    public static <T extends Enum<T>> SerializableDataType<T> enumValue(Class<T> dataClass, @NotNull Function<T, String> additionalMap) {
        return new SerializableDataType<>(dataClass, new EnumValueCodec<>(dataClass.getEnumConstants(), SerializationHelper.buildEnumMap(dataClass, additionalMap)));
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

    public static <T> SerializableDataType<TagKey<T>> tag(ResourceKey<? extends Registry<T>> registryKey) {
        return SerializableDataType.wrap(ClassUtil.castClass(TagKey.class), SerializableDataTypes.IDENTIFIER,
                TagKey::location,
                id -> TagKey.create(registryKey, id));
    }


    public static <T> SerializableDataType<Holder<T>> registryEntry(Registry<T> registry) {
        return wrap(
                ClassUtil.castClass(Holder.class),
                SerializableDataTypes.IDENTIFIER,
                registryEntry -> registryEntry.unwrapKey()
                        .orElseThrow(() -> new IllegalArgumentException("Registry entry \"" + registryEntry + "\" is not registered in registry \"" + registry.key().location() + "\""))
                        .location(),
                id -> registry
                        .getHolder(id)
                        .orElseThrow(() -> new IllegalArgumentException("Type \"" + id + "\" is not registered in registry \"" + registry.key().location() + "\""))
        );
    }

    public static <T> SerializableDataType<ResourceKey<T>> registryKey(ResourceKey<Registry<T>> registryRef) {
        return registryKey(registryRef, List.of());
    }

    public static <T> SerializableDataType<ResourceKey<T>> registryKey(ResourceKey<Registry<T>> registryRef, Collection<ResourceKey<T>> exemptions) {
        return wrap(
                ClassUtil.castClass(ResourceKey.class),
                SerializableDataTypes.IDENTIFIER,
                ResourceKey::location,
                id -> {

                    ResourceKey<T> registryKey = ResourceKey.create(registryRef, id);
                    RegistryAccess dynamicRegistries = CalioAPI.getSidedRegistryAccess();

                    if (dynamicRegistries == null || exemptions.contains(registryKey)) {
                        return registryKey;
                    }

                    if (!dynamicRegistries.registryOrThrow(registryRef).containsKey(registryKey)) {
                        throw new IllegalArgumentException("Type \"" + id + "\" is not registered in registry \"" + registryRef.location() + "\"");
                    }

                    return registryKey;

                }
        );
    }

    public static <T extends Enum<T>> SerializableDataType<EnumSet<T>> enumSet(Class<T> enumClass, SerializableDataType<T> enumDataType) {
        return new SerializableDataType<>(ClassUtil.castClass(EnumSet.class),
                CalioCodecHelper.setOf(enumDataType).xmap(EnumSet::copyOf, Function.identity()));
    }

    public void send(RegistryFriendlyByteBuf buffer, Object value) {
        this.send.accept(buffer, this.cast(value));
    }

    public T receive(RegistryFriendlyByteBuf buffer) {
        return this.receive.apply(buffer);
    }

    public T read(JsonElement jsonElement) {
        return this.read.apply(jsonElement);
    }


    public JsonElement writeUnsafely(Object value) throws Exception {
        try {
            return write.apply(cast(value));
        } catch (ClassCastException e) {
            throw new Exception(e);
        }
    }

    public JsonElement write(T value) {
        return this.write.apply(value);
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
                return DataResult.error(() -> "At " + this.dataClass.getSimpleName() + ": " + e.getMessage());
            }
        }

        return ops.getByteBuffer(input).flatMap(x -> {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.copiedBuffer(x), CalioAPI.getSidedRegistryAccess(), ConnectionType.NEOFORGE);
            try {
                Pair<T, T1> pair = Pair.of(this.receive.apply(buffer), ops.empty());
                return DataResult.success(pair);
            } catch (Exception e) {
                return DataResult.error(() -> "At " + this.dataClass.getSimpleName() + ": " + e.getMessage());
            } finally {
                buffer.release();
            }
        });
    }

    @Override
    public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
        if (this.codec != null) {
            try {
                return this.codec.encode(input, ops, prefix);
            } catch (Throwable t) {
                CalioAPI.LOGGER.error("Error", t);
                return DataResult.error(() -> "Error caught while encoding " + this.dataClass.getSimpleName() + ": " + input + ":" + t.getMessage());
            }
        }
        if (isDataContext(ops)) {
            if (this.write == null)
                return DataResult.error(() -> "Writing is unsupported for type: " + this.dataClass.getSimpleName());
            try {
                return DataResult.success(JsonOps.INSTANCE.convertTo(ops, this.write.apply(input)));
            } catch (Exception e) {
                return DataResult.error(() -> "At " + this.dataClass.getSimpleName() + ": " + e.getMessage());
            }
        }

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), CalioAPI.getSidedRegistryAccess(), ConnectionType.NEOFORGE);
        try {
            this.send.accept(buffer, input);
            return DataResult.success(ops.createByteList(buffer.nioBuffer()));
        } catch (Exception e) {
            return DataResult.error(() -> "At " + this.dataClass.getSimpleName() + ": " + e.getMessage());
        } finally {
            buffer.release();
        }
    }

    public static <T, U extends ArgumentType<T>> SerializableDataType<ArgumentWrapper<T>> argumentType(U argumentType) {
        return wrap(ClassUtil.castClass(ArgumentWrapper.class), SerializableDataTypes.STRING,
                ArgumentWrapper::rawArgument,
                str -> {
                    try {
                        T t = argumentType.parse(new StringReader(str));
                        return new ArgumentWrapper<>(t, str);
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException("Wrong syntax in argument type data", e);
                    }
                });
    }

    public static <T> SerializableDataType<TagLike<T>> tagLike(Registry<T> registry) {
        return new SerializableDataType<>(ClassUtil.castClass(TagLike.class),
                (packetByteBuf, tagLike) -> tagLike.write(packetByteBuf),
                packetByteBuf -> {
                    TagLike<T> tagLike = new TagLike<>(registry);
                    tagLike.read(packetByteBuf);
                    return tagLike;
                },
                jsonElement -> {
                    TagLike<T> tagLike = new TagLike<>(registry);
                    if (!jsonElement.isJsonArray()) {
                        throw new JsonSyntaxException("Expected a JSON array,");
                    }
                    JsonArray jsonArray = jsonElement.getAsJsonArray();
                    jsonArray.forEach(je -> {
                        String s = je.getAsString();
                        if (s.startsWith("#")) {
                            ResourceLocation id = ResourceLocation.tryParse(s.substring(1));
                            tagLike.addTag(id);
                        } else {
                            tagLike.add(ResourceLocation.tryParse(s));
                        }
                    });
                    return tagLike;
                },
                tagLike -> {
                    JsonArray array = new JsonArray();
                    tagLike.forEach(either -> {
                        either.ifLeft(tagKey -> {
                            String s = "#" + tagKey.location();
                            array.add(s);
                        }).ifRight(t -> {
                            String s = registry.getKey(t).toString();
                            array.add(s);
                        });
                    });
                    return array;
                });
    }

}
