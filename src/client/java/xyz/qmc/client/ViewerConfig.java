package xyz.qmc.client;

import net.minecraft.client.gui.screen.Screen;
import xyz.qmc.qolib.api.client.config.ClientConfig;
import xyz.qmc.qolib.api.client.config.ConfigValue;

/** Client settings stored by QoLib in config/viewer-mod.json. */
final class ViewerConfig {
	private static final ConfigValue<Boolean> ENABLED = ConfigValue.booleanValue("enabled", false);
	private static final ClientConfig CONFIG = ClientConfig.builder("viewer-mod")
		.add(ENABLED)
		.build();

	private ViewerConfig() {
	}

	static void load() {
		CONFIG.load();
	}

	static boolean enabled() {
		return ENABLED.get();
	}

	static void toggle() {
		ENABLED.set(!ENABLED.get());
		CONFIG.save();
	}

	static Screen createScreen(Screen parent) {
		return CONFIG.screen("Viewer Mod")
			.category("general", "General", menu -> menu
				.toggle("Native world saving", "Store received chunks in a local archive.", ENABLED))
			.build(parent);
	}
}
