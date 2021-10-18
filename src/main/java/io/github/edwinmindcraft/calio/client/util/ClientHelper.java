package io.github.edwinmindcraft.calio.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import org.jetbrains.annotations.Nullable;

public class ClientHelper {

	public static boolean isServerContext(@Nullable RegistryAccess access) {
		Minecraft instance = Minecraft.getInstance();
		if (instance == null) return true; // Data Context
		if (instance.getConnection() == null) return true; // Outside a world
		return access != null && access != instance.getConnection().registryAccess();
	}
}
