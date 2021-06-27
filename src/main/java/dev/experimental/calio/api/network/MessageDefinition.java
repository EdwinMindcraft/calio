package dev.experimental.calio.api.network;

import com.google.common.collect.ImmutableList;
import dev.experimental.calio.api.CalioAPI;
import dev.experimental.calio.common.network.NetworkWrapper;
import net.minecraft.network.PacketByteBuf;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

public record MessageDefinition<MSG>(NetworkChannel channel, int index, Class<MSG> message,
                                     BiConsumer<MSG, PacketByteBuf> encoder,
                                     Function<PacketByteBuf, MSG> decoder,
                                     BiConsumer<MSG, INetworkHandler> handler,
                                     @Nullable ToIntFunction<MSG> loginGetter,
                                     @Nullable ObjIntConsumer<MSG> loginSetter,
                                     @Nullable Function<Boolean, List<Pair<String, MSG>>> loginMessages,
                                     PacketDirection direction) {
    public static class Builder<MSG> {
        private final NetworkChannel channel;
        private final int index;
        private final Class<MSG> message;
        private final PacketDirection direction;
        private BiConsumer<MSG, PacketByteBuf> encoder;
        private Function<PacketByteBuf, MSG> decoder;
        private BiConsumer<MSG, INetworkHandler> handler;
        private ToIntFunction<MSG> loginGetter = null;
        private ObjIntConsumer<MSG> loginSetter = null;
        private Function<Boolean, List<Pair<String, MSG>>> loginMessages = null;

        public Builder(NetworkChannel channel, int index, Class<MSG> message, PacketDirection direction) {
            this.channel = channel;
            this.index = index;
            this.message = message;
            this.direction = direction;
        }

        public Builder<MSG> encoder(BiConsumer<MSG, PacketByteBuf> encoder) {
            this.encoder = encoder;
            return this;
        }

        public Builder<MSG> decoder(Function<PacketByteBuf, MSG> decoder) {
            this.decoder = decoder;
            return this;
        }

        public Builder<MSG> handler(BiConsumer<MSG, INetworkHandler> handler) {
            this.handler = handler;
            return this;
        }

        public Builder<MSG> loginIndex(ToIntFunction<MSG> loginGetter, ObjIntConsumer<MSG> loginSetter) {
            this.loginGetter = loginGetter;
            this.loginSetter = loginSetter;
            return this;
        }

        public Builder<MSG> markAsLoginPacket() {
            this.loginMessages = isNetwork -> {
                try {
                    MSG msg = this.message.getConstructor(new Class<?>[0]).newInstance();
                    return ImmutableList.of(Pair.of(this.message.getSimpleName(), msg));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    CalioAPI.LOGGER.error("Failed to create message {}", this.message.getSimpleName());
                    throw new RuntimeException(e);
                }
            };
            return this;
        }

        public void add() {
            NetworkWrapper.registerMessage(new MessageDefinition<>(this.channel, this.index, this.message, this.encoder, this.decoder, this.handler, this.loginGetter, this.loginSetter, this.loginMessages, this.direction));
        }
    }
}
