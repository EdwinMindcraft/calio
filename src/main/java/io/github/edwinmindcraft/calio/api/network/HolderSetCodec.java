package io.github.edwinmindcraft.calio.api.network;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;

import java.util.List;
import java.util.function.Supplier;

public class HolderSetCodec<A> implements Codec<HolderSet<A>> {
	private final Supplier<Registry<A>> registryKey;
	private final Codec<Either<TagKey<A>, List<Holder<A>>>> registryAwareCodec;

	public HolderSetCodec(Supplier<Registry<A>> registryKey, Codec<Holder<A>> holder, Codec<TagKey<A>> tag) {
		this.registryKey = registryKey;
		this.registryAwareCodec = Codec.either(tag, CalioCodecHelper.listOf(holder));
	}

	@Override
	public <T> DataResult<Pair<HolderSet<A>, T>> decode(DynamicOps<T> ops, T input) {
		return this.registryAwareCodec.decode(ops, input).map(x -> x.mapFirst(either -> either.<HolderSet<A>>map(this.registryKey.get()::getOrCreateTag, HolderSet::direct)));
	}

	@Override
	public <T> DataResult<T> encode(HolderSet<A> input, DynamicOps<T> ops, T prefix) {
		Either<TagKey<A>, List<Holder<A>>> either = input instanceof HolderSet.Named<A> named ? Either.left(named.key()) : Either.right(input.stream().toList());
		return this.registryAwareCodec.encode(either, ops, prefix);
	}
}
