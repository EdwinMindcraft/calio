package dev.experimental.calio.api;

import dev.architectury.utils.GameInstance;
import dev.experimental.calio.api.registry.ICalioDynamicRegistryManager;
import dev.experimental.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CalioAPI {
    public static final Logger LOGGER = LogManager.getLogger("Calio");
    public static final String MODID = "calio";

    public static Identifier identifier(String path) {
        return new Identifier(MODID, path);
    }

    /**
     * Returns the current server on the server, and null on the client.
     * FIXME: Find if there is an equivalent of EffectiveSide on Fabric.
     */
    public static MinecraftServer getServer() {
        return GameInstance.getServer();
    }

    public static ICalioDynamicRegistryManager getDynamicRegistries() {
        return getDynamicRegistries(getServer());
    }
    public static ICalioDynamicRegistryManager getDynamicRegistries(MinecraftServer server) {
        return CalioDynamicRegistryManager.getInstance(server);
    }
}
