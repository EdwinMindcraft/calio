package io.github.edwinmindcraft.calio.api.network;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public record HolderCodec<A>(Codec<A> direct,
							 Codec<ResourceLocation> reference,
							 Supplier<Registry<A>> access) implements Codec<Holder<A>> {

	@Override
	public <T> DataResult<Pair<Holder<A>, T>> decode(DynamicOps<T> ops, T input) {
		DataResult<ResourceLocation> stringValue = this.reference.decode(ops, input).map(Pair::getFirst);
		List<String> errors = new ArrayList<>();
		//Parse as a resource location
		if (stringValue.result().isPresent()) {
			ResourceLocation id = stringValue.result().get();
			ResourceKey<A> key = ResourceKey.create(this.access.get().key(), id);
			return this.access.get().getOrCreateHolder(key).map(holder -> Pair.of(holder, ops.empty()));
		} else if (stringValue.error().isPresent())
			errors.add(stringValue.error().get().message());
		DataResult<Pair<Holder<A>, T>> decode = this.direct.decode(ops, input).map(pair -> pair.mapFirst(Holder::direct));
		return decode.mapError(err -> errors.stream()
				.reduce(new StringBuilder(err), (sb, s) -> sb.append(", ").append(s), StringBuilder::append).toString());
	}

	@Override
	public <T> DataResult<T> encode(Holder<A> input, DynamicOps<T> ops, T prefix) {
		ResourceLocation key = input.unwrapKey().map(ResourceKey::location).orElse(null);
		//For some reason registry#getKey doesn't fulfil its contract on defaulted registries.
		// so ResourceKey access it is.
		if (key == null && input.isBound())
			key = this.access.get().getResourceKey(input.value()).map(ResourceKey::location).orElse(null);
		if (key != null)
			return this.reference.encodeStart(ops, key);
		return this.direct.encode(input.value(), ops, prefix);
	}
}
