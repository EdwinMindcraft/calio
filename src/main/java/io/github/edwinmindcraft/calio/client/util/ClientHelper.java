package io.github.edwinmindcraft.calio.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

public class ClientHelper {

	public static boolean isServerContext(@Nullable RegistryAccess access) {
		Minecraft instance = Minecraft.getInstance();
		//This is wrong in a data context, and annotations are busted;
		//noinspection ConstantConditions
		if (instance == null) return true; // Data Context
		if (instance.getConnection() == null) return true; // Outside a world
		if (ServerLifecycleHooks.getCurrentServer() == null) return false; // No server.
		return access != null && access != instance.getConnection().registryAccess();
	}
}
