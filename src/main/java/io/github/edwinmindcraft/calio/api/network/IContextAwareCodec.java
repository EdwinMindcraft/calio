package io.github.edwinmindcraft.calio.api.network;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.ByteBuffer;

public interface IContextAwareCodec<A> extends Codec<A> {
	JsonElement asJson(A input);
	A fromJson(JsonElement input);
	void encode(A input, FriendlyByteBuf buf);
	A decode(FriendlyByteBuf buf);

	default boolean useJson(DynamicOps<?> ops) {
		return ops instanceof JsonOps && !ops.compressMaps();
	}

	@Override
	default <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		if (this.useJson(ops)) {
			JsonElement json = ops.convertTo(JsonOps.INSTANCE, input);
			try {
				return DataResult.success(Pair.of(this.fromJson(json), ops.empty()));
			} catch (Exception e) {
				return DataResult.error(e.getMessage());
			}
		}
		return ops.getByteBuffer(input).flatMap(x -> {
			FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.copiedBuffer(x));
			try {
				A decode = this.decode(buffer);
				return DataResult.success(Pair.of(decode, ops.empty()));
			} catch (Exception e) {
				return DataResult.error(e.getMessage());
			} finally {
				buffer.release();
			}
		});
	}

	@Override
	default <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if (this.useJson(ops)) {
			try {
				JsonElement json = this.asJson(input);
				return DataResult.success(JsonOps.INSTANCE.convertTo(ops, json));
			} catch (Exception e) {
				return DataResult.error(e.getMessage());
			}
		}

		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		try {
			this.encode(input, buffer);
			ByteBuffer byteBuffer = buffer.nioBuffer();
			return DataResult.success(ops.createByteList(byteBuffer));
		} catch (Exception e) {
			return DataResult.error(e.getMessage());
		} finally {
			buffer.release();
		}
	}
}
