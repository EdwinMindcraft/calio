package io.github.edwinmindcraft.calio.common;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CalioConfig {
	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPECS;

	static {
		Pair<Common, ForgeConfigSpec> common = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON = common.getLeft();
		COMMON_SPECS = common.getRight();
	}

	public static final class Common {
		public final ForgeConfigSpec.BooleanValue logging;
		public final ForgeConfigSpec.BooleanValue debugMode;

		public Common(ForgeConfigSpec.Builder builder) {
			builder.push("debug");
			this.logging = builder.comment("Enable calio registry logging.").translation("config.calio.debug.registry_logging").define("registry_logging", false);
			this.debugMode = builder.comment("Enable calio debug mode").translation("config.calio.debug.debug_mode").define("debug_mode", false);
			builder.pop();
		}
	}
}
