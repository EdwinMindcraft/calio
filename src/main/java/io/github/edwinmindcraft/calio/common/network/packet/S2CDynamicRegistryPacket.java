package io.github.edwinmindcraft.calio.common.network.packet;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.registry.DynamicRegistryListener;
import io.github.edwinmindcraft.calio.common.network.CalioNetwork;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public abstract sealed class S2CDynamicRegistryPacket<T> permits S2CDynamicRegistryPacket.Login, S2CDynamicRegistryPacket.Play {
	private final ResourceKey<Registry<T>> key;
	private final Registry<T> registry;
	private final Codec<T> codec;
	private final int start;
	private final int count;

	public S2CDynamicRegistryPacket(ResourceKey<Registry<T>> key, Registry<T> registry, Codec<T> codec, int start, int count) {
		this.key = key;
		this.registry = registry;
		this.codec = codec;
		this.start = start;
		this.count = count;
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeResourceLocation(this.key.location());
		List<Holder.Reference<T>> references = this.registry.holders().filter(Holder.Reference::isBound).toList();
		buffer.writeVarInt(this.start);
		buffer.writeVarInt(this.count);
		for (int i = this.start; i < this.start + this.count; i++) {
			Holder.Reference<T> entry = references.get(i);
			buffer.writeInt(this.registry.getId(entry.value()));
			buffer.writeResourceLocation(entry.key().location());
			buffer.writeWithCodec(this.codec, entry.value());
		}
	}

	private static <T, V extends S2CDynamicRegistryPacket<T>> List<V> splitPackets(ResourceKey<Registry<T>> key, CalioDynamicRegistryManager drm, Builder<T, V> builder, int size) {
		WritableRegistry<T> registry = drm.get(key);
		Codec<T> codec = drm.getCodec(key);
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		buffer.writeResourceLocation(key.location());
		List<Holder.Reference<T>> references = registry.holders().filter(Holder.Reference::isBound).toList();
		List<V> packets = new ArrayList<>();
		buffer.writeInt(0);
		buffer.writeInt(0);
		int overhead = buffer.writerIndex();
		int start = 0;

		for (int i = 0; i < references.size(); i++) {
			Holder.Reference<T> entry = references.get(i);
			buffer.writeInt(registry.getId(entry.value()));
			buffer.writeResourceLocation(entry.key().location());
			buffer.writeWithCodec(codec, entry.value());
			if (buffer.writerIndex() >= size) {
				packets.add(builder.apply(key, registry, codec, start, i - start));
				start = i;
				buffer.writerIndex(overhead);
			}
		}
		buffer.release();
		packets.add(builder.apply(key, registry, codec, start, references.size() - start));
		return packets;
	}

	private static <T, V extends S2CDynamicRegistryPacket<T>> V decodeWithBuilder(FriendlyByteBuf buffer, Builder<T, V> decoder) {
		CalioDynamicRegistryManager cdrm = CalioDynamicRegistryManager.getInstance(null);
		ResourceKey<Registry<T>> registryKey = ResourceKey.createRegistryKey(buffer.readResourceLocation());
		MappedRegistry<T> registry = new MappedRegistry<>(registryKey, Lifecycle.experimental(), null);
		Codec<T> codec = cdrm.getCodec(registryKey);
		int start = buffer.readVarInt();
		int count = buffer.readVarInt();
		for (int i = 0; i < count; i++) {
			int index = buffer.readInt();
			ResourceKey<T> key = ResourceKey.create(registryKey, buffer.readResourceLocation());
			T value = buffer.readWithCodec(codec);
			if (value instanceof DynamicRegistryListener drl)
				drl.whenNamed(key.location());
			registry.registerMapping(index, key, value, Lifecycle.experimental());
		}
		return decoder.apply(registryKey, registry, codec, start, count);
	}

	public void handle(Supplier<NetworkEvent.Context> handler) {
		handler.get().enqueueWork(() -> {
			CalioDynamicRegistryManager instance = CalioDynamicRegistryManager.getInstance(null);
			WritableRegistry<T> target = this.start == 0 ? instance.reset(this.key) : instance.get(this.key);
			for (Map.Entry<ResourceKey<T>, T> entry : this.registry.entrySet())
				target.registerOrOverride(OptionalInt.of(this.registry.getId(entry.getValue())), entry.getKey(), entry.getValue(), Lifecycle.experimental());
			instance.dump();
		});
		if (this instanceof S2CDynamicRegistryPacket.Login<T>)
			CalioNetwork.CHANNEL.reply(new C2SAcknowledgePacket(), handler.get());
		handler.get().setPacketHandled(true);
	}

	@FunctionalInterface
	public interface Builder<T, V extends S2CDynamicRegistryPacket<T>> {
		V apply(ResourceKey<Registry<T>> key, Registry<T> registry, Codec<T> codec, int start, int count);
	}

	public static final class Login<T> extends S2CDynamicRegistryPacket<T> implements IntSupplier {
		public int loginIndex;

		public Login(ResourceKey<Registry<T>> key, Registry<T> registry, Codec<T> codec, int start, int count) {
			super(key, registry, codec, start, count);
		}

		public int getLoginIndex() {
			return this.loginIndex;
		}

		public void setLoginIndex(int loginIndex) {
			this.loginIndex = loginIndex;
		}

		@Override
		public int getAsInt() {
			return this.loginIndex;
		}

		public static <T> Login<T> decode(FriendlyByteBuf buf) {
			return S2CDynamicRegistryPacket.<T, Login<T>>decodeWithBuilder(buf, Login<T>::new);
		}

		private static <T> List<Login<T>> split(ResourceKey<Registry<?>> key, CalioDynamicRegistryManager drm, int size) {
			return S2CDynamicRegistryPacket.splitPackets((ResourceKey<Registry<T>>) (ResourceKey) key, drm, Login<T>::new, size);
		}

		public static List<Pair<String, S2CDynamicRegistryPacket.Login>> create(boolean isLocal) {
			try {
				ImmutableList.Builder<Pair<String, S2CDynamicRegistryPacket.Login>> builder = ImmutableList.builder();
				CalioDynamicRegistryManager drm = (CalioDynamicRegistryManager) CalioAPI.getDynamicRegistries(ServerLifecycleHooks.getCurrentServer());
				for (ResourceKey<Registry<?>> registryName : drm.getRegistryNames()) {
					List<Login<Object>> splitPackets = split(registryName, drm, 1048576);
					for (int i = 0; i < splitPackets.size(); i++) {
						builder.add(Pair.of("CALIO-S2CLDRP-" + registryName.location() + "-" + i, splitPackets.get(i)));
					}
				}
				return builder.build();
			} catch (Throwable t) {
				t.printStackTrace();
				throw t;
			}
		}
	}

	public static final class Play<T> extends S2CDynamicRegistryPacket<T> {
		public Play(ResourceKey<Registry<T>> key, Registry<T> registry, Codec<T> codec, int start, int count) {
			super(key, registry, codec, start, count);
		}

		public static <T> Play<T> decode(FriendlyByteBuf buf) {
			return S2CDynamicRegistryPacket.<T, Play<T>>decodeWithBuilder(buf, Play<T>::new);
		}

		private static <T> List<Play<T>> split(ResourceKey<Registry<?>> key, CalioDynamicRegistryManager drm, int size) {
			return S2CDynamicRegistryPacket.splitPackets((ResourceKey<Registry<T>>) (ResourceKey) key, drm, Play<T>::new, size);
		}

		public static List<S2CDynamicRegistryPacket.Play<?>> create(CalioDynamicRegistryManager manager) {
			try {
				ImmutableList.Builder<S2CDynamicRegistryPacket.Play<?>> builder = ImmutableList.builder();
				for (ResourceKey<Registry<?>> registryName : manager.getRegistryNames()) {
					for (Play<?> splitPacket : split(registryName, manager, 1048576))
						builder.add(splitPacket);
				}
				return builder.build();
			} catch (Throwable t) {
				t.printStackTrace();
				throw t;
			}
		}
	}
}
