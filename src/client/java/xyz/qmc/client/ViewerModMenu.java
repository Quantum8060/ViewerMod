package xyz.qmc.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Opens the QoLib-backed Viewer Mod settings screen from Mod Menu. */
public final class ViewerModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ViewerConfig::createScreen;
	}
}
