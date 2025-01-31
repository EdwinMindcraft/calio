package io.github.edwinmindcraft.calio.api;

import io.github.edwinmindcraft.calio.api.ability.IAbilityHolder;
import io.github.edwinmindcraft.calio.api.registry.ICalioDynamicRegistryManager;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class CalioAPI {
	public static final Logger LOGGER = LogManager.getLogger("Calio");
	public static final String MODID = "calio";
	public static Capability<IAbilityHolder> ABILITY_HOLDER = CapabilityManager.get(new CapabilityToken<>() {});

	public static ResourceLocation resource(String path) {
		return new ResourceLocation(MODID, path);
	}

	public static MinecraftServer getServer() {
		return ServerLifecycleHooks.getCurrentServer();
	}

	public static ICalioDynamicRegistryManager getDynamicRegistries() {
		return getDynamicRegistries(getSidedRegistryAccess());
	}

	private static RegistryAccess getSidedRegistryAccess() {
		if (EffectiveSide.get().isClient())
			return null;
		if (ServerLifecycleHooks.getCurrentServer() != null)
			return ServerLifecycleHooks.getCurrentServer().registryAccess();
		return RegistryAccess.BUILTIN.get();
	}

	public static ICalioDynamicRegistryManager getDynamicRegistries(@Nullable MinecraftServer server) {
		return CalioDynamicRegistryManager.getInstance(server == null ? getSidedRegistryAccess() : server.registryAccess());
	}

	public static ICalioDynamicRegistryManager getDynamicRegistries(@Nullable CommonLevelAccessor level) {
		return CalioDynamicRegistryManager.getInstance(level == null ? getSidedRegistryAccess() : level.registryAccess());
	}

	public static ICalioDynamicRegistryManager getDynamicRegistries(@Nullable RegistryAccess access) {
		return CalioDynamicRegistryManager.getInstance(access);
	}

	public static LazyOptional<IAbilityHolder> getAbilityHolder(Entity entity) {
		return entity.getCapability(ABILITY_HOLDER);
	}
}
