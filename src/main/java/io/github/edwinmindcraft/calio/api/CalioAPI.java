package io.github.edwinmindcraft.calio.api;

import io.github.edwinmindcraft.calio.api.ability.IAbilityHolder;
import io.github.edwinmindcraft.calio.api.registry.ICalioDynamicRegistryManager;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import io.github.edwinmindcraft.calio.common.util.CoreHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
import net.minecraftforge.forgespi.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class CalioAPI {
	public static final Logger LOGGER = LogManager.getLogger("Calio");
	public static final String MODID = "calio";
	@CapabilityInject(IAbilityHolder.class)
	public static Capability<IAbilityHolder> ABILITY_HOLDER;

	public static ResourceLocation resource(String path) {
		return new ResourceLocation(MODID, path);
	}

	public static MinecraftServer getServer() {
		return ServerLifecycleHooks.getCurrentServer();
	}

	public static ICalioDynamicRegistryManager getDynamicRegistries() {
		RegistryAccess registryAccess = EffectiveSide.get().isClient() ? null : ServerLifecycleHooks.getCurrentServer() != null ? ServerLifecycleHooks.getCurrentServer().registryAccess() : RegistryAccess.builtin();
		return getDynamicRegistries(registryAccess);
	}

	public static ICalioDynamicRegistryManager getDynamicRegistries(MinecraftServer server) {
		RegistryAccess registryAccess = EffectiveSide.get().isClient() ? null : server != null ? server.registryAccess() : RegistryAccess.builtin();
		return CalioDynamicRegistryManager.getInstance(registryAccess);
	}

	public static ICalioDynamicRegistryManager getDynamicRegistries(RegistryAccess server) {
		return CalioDynamicRegistryManager.getInstance(server);
	}

	public static LazyOptional<IAbilityHolder> getAbilityHolder(Entity entity) {
		return entity.getCapability(ABILITY_HOLDER);
	}
}
