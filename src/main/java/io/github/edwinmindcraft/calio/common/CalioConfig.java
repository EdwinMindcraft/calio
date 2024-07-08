package io.github.edwinmindcraft.calio.common;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CalioConfig {
	public static final Common COMMON;
	public static final ModConfigSpec COMMON_SPECS;

	static {
		Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
		COMMON = common.getLeft();
		COMMON_SPECS = common.getRight();
	}

	public static final class Common {
		public final ModConfigSpec.BooleanValue logging;
		public final ModConfigSpec.BooleanValue reducedLogging;
		public final ModConfigSpec.BooleanValue debugMode;

		public Common(ModConfigSpec.Builder builder) {
			builder.push("debug");
			this.logging = builder.comment("Enable calio registry logging.").translation("config.calio.debug.registry_logging").define("registry_logging", false);
			this.reducedLogging = builder.comment("Reduced registry logging.").translation("config.calio.debug.reduced_logging").define("reduced_logging", true);
			this.debugMode = builder.comment("Enable calio debug mode").translation("config.calio.debug.debug_mode").define("debug_mode", false);
			builder.pop();
		}
	}
}
