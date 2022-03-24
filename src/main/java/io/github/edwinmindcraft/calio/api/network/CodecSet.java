package io.github.edwinmindcraft.calio.api.network;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;

import java.util.List;

public record CodecSet<T>(Codec<Holder<T>> holder, Codec<Holder<T>> holderRef, Codec<TagKey<T>> directTag, Codec<TagKey<T>> hashedTag,
						  Codec<HolderSet<T>> directSet, Codec<HolderSet<T>> hashedSet, Codec<List<HolderSet<T>>> set) {
}
