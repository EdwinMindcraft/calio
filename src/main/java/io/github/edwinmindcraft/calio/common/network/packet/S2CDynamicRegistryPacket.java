package io.github.edwinmindcraft.calio.common.network.packet;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.network.CalioNetwork;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public abstract class S2CDynamicRegistryPacket<T> {

	private final ResourceKey<Registry<T>> key;
	private final Registry<T> registry;
	private final Codec<T> codec;

	public S2CDynamicRegistryPacket(ResourceKey<Registry<T>> key, Registry<T> registry, Codec<T> codec) {
		this.key = key;
		this.registry = registry;
		this.codec = codec;
	}

	public S2CDynamicRegistryPacket(ResourceKey<Registry<?>> key, CalioDynamicRegistryManager drm) {
		this.key = (ResourceKey<Registry<T>>) (ResourceKey) key;
		this.registry = drm.get(this.key);
		this.codec = drm.getCodec(this.key);
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeResourceLocation(this.key.location());
		List<Holder.Reference<T>> references = this.registry.holders().filter(Holder.Reference::isBound).toList();
		buffer.writeInt(references.size());
		for (Holder.Reference<T> entry : references) {
			buffer.writeInt(this.registry.getId(entry.value()));
			buffer.writeResourceLocation(entry.key().location());
			buffer.writeWithCodec(this.codec, entry.value());
		}
	}

	private static <T, V extends S2CDynamicRegistryPacket<T>> V decodeWithBuilder(FriendlyByteBuf buffer, Decoder<T, V> decoder) {
		CalioDynamicRegistryManager cdrm = CalioDynamicRegistryManager.getInstance(null);
		ResourceKey<Registry<T>> registryKey = ResourceKey.createRegistryKey(buffer.readResourceLocation());
		MappedRegistry<T> registry = new MappedRegistry<>(registryKey, Lifecycle.experimental(), null);
		Codec<T> codec = cdrm.getCodec(registryKey);
		int count = buffer.readInt();
		for (int i = 0; i < count; i++) {
			int index = buffer.readInt();
			ResourceKey<T> key = ResourceKey.create(registryKey, buffer.readResourceLocation());
			T value = buffer.readWithCodec(codec);
			registry.registerMapping(index, key, value, Lifecycle.experimental());
		}
		return decoder.apply(registryKey, registry, codec);
	}

	public void handle(Supplier<NetworkEvent.Context> handler) {
		handler.get().enqueueWork(() -> {
			CalioDynamicRegistryManager instance = CalioDynamicRegistryManager.getInstance(null);
			WritableRegistry<T> target = instance.reset(this.key);
			for (Map.Entry<ResourceKey<T>, T> entry : this.registry.entrySet()) {
				if (entry.getValue() instanceof IForgeRegistryEntry<?> fre)
					fre.setRegistryName(entry.getKey().location());
				target.registerOrOverride(OptionalInt.of(this.registry.getId(entry.getValue())), entry.getKey(), entry.getValue(), Lifecycle.experimental());
			}
		});
		if (this instanceof S2CDynamicRegistryPacket.Login<T>)
			CalioNetwork.CHANNEL.reply(new C2SAcknowledgePacket(), handler.get());
		handler.get().setPacketHandled(true);
	}

	@FunctionalInterface
	public interface Decoder<T, V extends S2CDynamicRegistryPacket<T>> {
		V apply(ResourceKey<Registry<T>> key, Registry<T> registry, Codec<T> codec);
	}

	public static final class Login<T> extends S2CDynamicRegistryPacket<T> implements IntSupplier {
		public int loginIndex;

		public Login(ResourceKey<Registry<T>> key, Registry<T> registry, Codec<T> codec) {
			super(key, registry, codec);
		}

		public Login(ResourceKey<Registry<?>> key, CalioDynamicRegistryManager drm) {
			super(key, drm);
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

		public static List<Pair<String, S2CDynamicRegistryPacket.Login>> create(boolean isLocal) {
			try {
				ImmutableList.Builder<Pair<String, S2CDynamicRegistryPacket.Login>> builder = ImmutableList.builder();
				CalioDynamicRegistryManager drm = (CalioDynamicRegistryManager) CalioAPI.getDynamicRegistries(ServerLifecycleHooks.getCurrentServer());
				for (ResourceKey<Registry<?>> registryName : drm.getRegistryNames()) {
					builder.add(Pair.of("CALIO-S2CLDRP-" + registryName.location(), new Login<>(registryName, drm)));
				}
				return builder.build();
			} catch (Throwable t) {
				t.printStackTrace();
				throw t;
			}
		}
	}

	public static final class Play<T> extends S2CDynamicRegistryPacket<T> {
		public Play(ResourceKey<Registry<T>> key, Registry<T> registry, Codec<T> codec) {
			super(key, registry, codec);
		}

		public Play(ResourceKey<Registry<?>> key, CalioDynamicRegistryManager drm) {
			super(key, drm);
		}

		public static <T> Play<T> decode(FriendlyByteBuf buf) {
			return S2CDynamicRegistryPacket.<T, Play<T>>decodeWithBuilder(buf, Play<T>::new);
		}

		public static List<S2CDynamicRegistryPacket.Play<?>> create(CalioDynamicRegistryManager manager) {
			try {
				ImmutableList.Builder<S2CDynamicRegistryPacket.Play<?>> builder = ImmutableList.builder();
				for (ResourceKey<Registry<?>> registryName : manager.getRegistryNames())
					builder.add(new Play<>(registryName, manager));
				return builder.build();
			} catch (Throwable t) {
				t.printStackTrace();
				throw t;
			}
		}
	}
}
