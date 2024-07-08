package io.github.edwinmindcraft.calio.api;

import io.github.edwinmindcraft.calio.api.ability.AbilityHolder;
import io.github.edwinmindcraft.calio.api.registry.CalioDynamicRegistryManager;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManagerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;

public class CalioAPI {
	public static final Logger LOGGER = LogManager.getLogger("Calio");
	public static final String MODID = "calio";
	public static final EntityCapability<AbilityHolder, Void> ABILITY_HOLDER = EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath(MODID, "ability_holder"), AbilityHolder.class);

	@Contract(pure = true)
	public static ResourceLocation resource(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}

	@Contract(pure = true)
	public static MinecraftServer getServer() {
		return ServerLifecycleHooks.getCurrentServer();
	}

	@Contract(pure = true)
	public static CalioDynamicRegistryManager getDynamicRegistryManager() {
		return CalioDynamicRegistryManagerImpl.getInstance();
	}

	@Contract(pure = true)
	public static RegistryAccess getRegistryAccess() {
		if (EffectiveSide.get().isClient())
			return Minecraft.getInstance().level.registryAccess();
		if (ServerLifecycleHooks.getCurrentServer() != null)
			return ServerLifecycleHooks.getCurrentServer().registryAccess();
		return RegistryAccess.EMPTY;
	}

	/*
	@Contract(pure = true)
	public static CalioDynamicRegistryManager getDynamicRegistries(@Nullable MinecraftServer server) {
		return CalioDynamicRegistryManagerImpl.getInstance(server == null ? getSidedRegistryAccess() : server.registryAccess());
	}

	@Contract(pure = true)
	public static CalioDynamicRegistryManager getDynamicRegistries(@Nullable CommonLevelAccessor level) {
		return CalioDynamicRegistryManagerImpl.getInstance(level == null ? getSidedRegistryAccess() : level.registryAccess());
	}

	@Contract(pure = true)
	public static CalioDynamicRegistryManager getDynamicRegistries(@Nullable RegistryAccess access) {
		return CalioDynamicRegistryManagerImpl.getInstance(access);
	}
	 */

	@Contract(pure = true)
	public static AbilityHolder getAbilityHolder(Entity entity) {
		return AbilityHolder.get(entity);
	}
}
