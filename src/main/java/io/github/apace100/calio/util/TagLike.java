package io.github.apace100.calio.util;

import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.*;
import java.util.function.Consumer;

public class TagLike<T> {

    private final Registry<T> registry;
    private final List<TagKey<T>> tags = new LinkedList<>();
    private final Set<T> items = new HashSet<>();

    public TagLike(Registry<T> registry) {
        this.registry = registry;
    }

    public void forEach(Consumer<Either<TagKey<T>, T>> consumer) {
        List<Either<TagKey<T>, T>> list = Streams.concat(tags.stream().map(tagKey -> (Either<TagKey<T>, T>)Either.left(tagKey)), items.stream().map(item -> (Either<TagKey<T>, T>)(Object)Either.right(item))).toList();
        list.forEach(consumer);
    }

    public void addTag(ResourceLocation id) {
        addTag(TagKey.create(registry.key(), id));
    }

    public void add(ResourceLocation id) {
        add(registry.get(id));
    }

    public void addTag(TagKey<T> tagKey) {
        tags.add(tagKey);
    }

    public void add(T t) {
        items.add(t);
    }

    public boolean contains(T t) {
        if(items.contains(t)) {
            return true;
        }
        Holder<T> entry = registry.createIntrusiveHolder(t);
        for(TagKey<T> tagKey : tags) {
            if(entry.is(tagKey)) {
                return true;
            }
        }
        return false;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(tags.size());
        for(TagKey<T> tagKey : tags) {
            buf.writeUtf(tagKey.location().toString());
        }
        buf.writeVarInt(items.size());
        for(T t : items) {
            buf.writeUtf(registry.getKey(t).toString());
        }
    }

    public void read(FriendlyByteBuf buf) {
        tags.clear();
        int count = buf.readVarInt();
        for(int i = 0; i < count; i++) {
            tags.add(TagKey.create(registry.key(), new ResourceLocation(buf.readUtf())));
        }
        items.clear();
        count = buf.readVarInt();
        for(int i = 0; i < count; i++) {
            T t = registry.get(new ResourceLocation(buf.readUtf()));
            items.add(t);
        }
    }
}
