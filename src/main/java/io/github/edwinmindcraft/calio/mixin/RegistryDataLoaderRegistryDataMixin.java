package io.github.edwinmindcraft.calio.mixin;

import com.mojang.serialization.Lifecycle;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManagerImpl;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

@Mixin(RegistryDataLoader.RegistryData.class)
public class RegistryDataLoaderRegistryDataMixin<T> {
    @Shadow @Final private ResourceKey<? extends Registry<T>> key;

    @ModifyVariable(method = "create", at = @At(value = "LOAD"))
    private WritableRegistry<T> calio$createDrmRegistry(WritableRegistry<T> original, Lifecycle lifecycle, Map<ResourceKey<?>, Exception> exceptionMap) {
        WritableRegistry<T> registry = ((CalioDynamicRegistryManagerImpl)CalioAPI.getDynamicRegistryManager()).newRegistry(this.key, lifecycle);
        if (registry != null)
            return registry;
        return original;
    }
}