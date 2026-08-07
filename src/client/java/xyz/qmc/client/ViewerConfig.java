package xyz.qmc.client;

import net.fabricmc.loader.api.FabricLoader;
import xyz.qmc.ViewerMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small dependency-free config.  It is deliberately opt-in. */
final class ViewerConfig {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("viewer-mod.json");
	private static final Pattern ENABLED = Pattern.compile("\\\"enabled\\\"\\s*:\\s*(true|false)");
	private static boolean enabled;

	static void load() {
		enabled = false;
		try {
			if (Files.exists(FILE)) {
				Matcher match = ENABLED.matcher(Files.readString(FILE, StandardCharsets.UTF_8));
				if (match.find()) enabled = Boolean.parseBoolean(match.group(1));
			} else {
				save();
			}
		} catch (IOException exception) {
			ViewerMod.LOGGER.warn("Could not read viewer-mod configuration", exception);
		}
	}

	static boolean enabled() { return enabled; }

	static void toggle() {
		enabled = !enabled;
		save();
	}

	private static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, "{\n  \"enabled\": " + enabled + "\n}\n", StandardCharsets.UTF_8);
		} catch (IOException exception) {
			ViewerMod.LOGGER.warn("Could not save viewer-mod configuration", exception);
		}
	}
}
