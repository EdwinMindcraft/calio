package io.github.edwinmindcraft.calio.api.network;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record HolderCodec<A>(Codec<A> direct,
							 Codec<ResourceLocation> reference,
							 Supplier<Registry<A>> access) implements Codec<Holder<A>> {

	@Override
	public <T> DataResult<Pair<Holder<A>, T>> decode(DynamicOps<T> ops, T input) {
		DataResult<ResourceLocation> stringValue = this.reference.decode(ops, input).map(Pair::getFirst);
		List<String> errors = new ArrayList<>();
		if (stringValue.result().isPresent()) {
			ResourceLocation id = stringValue.result().get();
			ResourceKey<A> key = ResourceKey.create(this.access.get().key(), id);
			Holder<A> holder = this.access.get().getOrCreateHolder(key);
			return DataResult.success(Pair.of(holder, ops.empty()));
		} else if (stringValue.error().isPresent())
			errors.add(stringValue.error().get().message());
		DataResult<Pair<Holder<A>, T>> decode = this.direct.decode(ops, input).map(pair -> pair.mapFirst(Holder::direct));
		return decode.mapError(err -> errors.stream()
				.reduce(new StringBuilder(err), (sb, s) -> sb.append(", ").append(s), (r1, r2) -> {
					r1.append(r2);
					return r1;
				}).toString());
	}

	@Override
	public <T> DataResult<T> encode(Holder<A> input, DynamicOps<T> ops, T prefix) {
		if (input.kind() == Holder.Kind.REFERENCE) {
			ResourceLocation key = this.access.get().getKey(input.value());
			if (key != null)
				return this.reference.encodeStart(ops, key);
		}
		return this.direct.encode(input.value(), ops, prefix);
	}
}
