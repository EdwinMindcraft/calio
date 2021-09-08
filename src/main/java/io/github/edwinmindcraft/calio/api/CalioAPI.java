package io.github.edwinmindcraft.calio.api;

import io.github.edwinmindcraft.calio.api.registry.ICalioDynamicRegistryManager;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
import net.minecraftforge.forgespi.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CalioAPI {
	public static final Logger LOGGER = LogManager.getLogger("Calio");
	public static final String MODID = "calio";

	public static ResourceLocation resource(String path) {
		return new ResourceLocation(MODID, path);
	}

	public static MinecraftServer getServer() {
		return ServerLifecycleHooks.getCurrentServer();
	}

	public static ICalioDynamicRegistryManager getDynamicRegistries() {
		return getDynamicRegistries(EffectiveSide.get().isClient() ? null : ServerLifecycleHooks.getCurrentServer());
	}

	public static ICalioDynamicRegistryManager getDynamicRegistries(MinecraftServer server) {
		return CalioDynamicRegistryManager.getInstance(server);
	}
}
