package xyz.qmc.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ViewerModClient implements ClientModInitializer {
	private static final KeyBinding TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.viewer-mod.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, KeyBinding.Category.create(Identifier.of("viewer-mod", "general"))));
	@Override public void onInitializeClient() {
		ViewerConfig.load();
		ClientChunkEvents.CHUNK_LOAD.register(SnapshotRecorder::capture);
		ClientChunkEvents.CHUNK_UNLOAD.register(SnapshotRecorder::capture);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null) SnapshotRecorder.endSession();
			while (TOGGLE.wasPressed()) {
				ViewerConfig.toggle();
				if (ViewerConfig.enabled()) SnapshotRecorder.queueLoadedArea(client);
				if (client.player != null) client.player.sendMessage(Text.literal("[Viewer Mod] Native world saving: " + (ViewerConfig.enabled() ? "ON" : "OFF")), true);
			}
			SnapshotRecorder.processPending(client);
		});
	}
}
