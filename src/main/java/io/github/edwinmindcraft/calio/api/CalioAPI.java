package io.github.edwinmindcraft.calio.api;

import io.github.edwinmindcraft.calio.api.ability.IAbilityHolder;
import io.github.edwinmindcraft.calio.api.registry.ICalioDynamicRegistryManager;
import io.github.edwinmindcraft.calio.client.util.ClientHelper;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import io.github.edwinmindcraft.calio.common.util.SideUtil;
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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class CalioAPI {
	public static final Logger LOGGER = LogManager.getLogger("Calio");
	public static final String MODID = "calio";
	public static Capability<IAbilityHolder> ABILITY_HOLDER = CapabilityManager.get(new CapabilityToken<>() {});

	@Contract(pure = true)
	public static ResourceLocation resource(String path) {
		return new ResourceLocation(MODID, path);
	}

	@Contract(pure = true)
	public static MinecraftServer getServer() {
		return ServerLifecycleHooks.getCurrentServer();
	}

	@Contract(pure = true)
	public static ICalioDynamicRegistryManager getDynamicRegistries() {
		return getDynamicRegistries(getSidedRegistryAccess());
	}

	@Contract(pure = true)
	private static RegistryAccess getSidedRegistryAccess() {
		if (SideUtil.isClient())
			return ClientHelper.getClientRegistryAccess();
		if (ServerLifecycleHooks.getCurrentServer() != null)
			return ServerLifecycleHooks.getCurrentServer().registryAccess();
		return RegistryAccess.EMPTY;
	}

	@Contract(pure = true)
	public static ICalioDynamicRegistryManager getDynamicRegistries(@Nullable MinecraftServer server) {
		return CalioDynamicRegistryManager.getInstance(server == null ? getSidedRegistryAccess() : server.registryAccess());
	}

	@Contract(pure = true)
	public static ICalioDynamicRegistryManager getDynamicRegistries(@Nullable CommonLevelAccessor level) {
		return CalioDynamicRegistryManager.getInstance(level == null ? getSidedRegistryAccess() : level.registryAccess());
	}

	@Contract(pure = true)
	public static ICalioDynamicRegistryManager getDynamicRegistries(@Nullable RegistryAccess access) {
		return CalioDynamicRegistryManager.getInstance(access);
	}

	@Contract(pure = true)
	public static LazyOptional<IAbilityHolder> getAbilityHolder(Entity entity) {
		return entity.getCapability(ABILITY_HOLDER);
	}
}
