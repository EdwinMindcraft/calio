package io.github.edwinmindcraft.calio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Decoder;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManagerImpl;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {
    @ModifyExpressionValue(method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;", ordinal = 0))
    private static Stream<RegistryDataLoader.RegistryData<?>> calio$cdrmSort(Stream<RegistryDataLoader.RegistryData<?>> registryData) {
        return ((CalioDynamicRegistryManagerImpl)CalioAPI.getDynamicRegistryManager()).sort(registryData);
    }

    @Inject(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;", at = @At(value = "HEAD"))
    private static void calio$cdrmPrepare(ResourceManager resourceManager, RegistryAccess registryAccess, List<RegistryDataLoader.RegistryData<?>> registryData, CallbackInfoReturnable<RegistryAccess.Frozen> cir) {
        ((CalioDynamicRegistryManagerImpl)CalioAPI.getDynamicRegistryManager()).prepare(resourceManager, registryAccess);
    }

    @Inject(method = "loadContentsFromManager", at = @At(value = "HEAD"), cancellable = true)
    private static <E> void calio$cdrmLoad(ResourceManager manager, RegistryOps.RegistryInfoLookup registryLookup, WritableRegistry<E> registry, Decoder<E> decoder, Map<ResourceKey<?>, Exception> exceptionMap, CallbackInfo ci) {
        if (((CalioDynamicRegistryManagerImpl)CalioAPI.getDynamicRegistryManager()).reload(manager, registry))
            ci.cancel();
    }
}
