package xyz.qmc.client;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Creates a minimal valid 1.21.11 void-world level.dat. */
final class LevelFile {
	static void write(Path root, String name, MinecraftClient client) throws IOException {
		Files.createDirectories(root);
		NbtCompound data = new NbtCompound();
		NbtHelper.putDataVersion(data);
		int dv = NbtHelper.getDataVersion(data, 0);
		data.putInt("version", 19133);
		NbtCompound version = new NbtCompound(); version.putInt("Id", dv); version.putString("Name", SharedConstants.getGameVersion().name()); version.putString("Series", "main"); version.putBoolean("Snapshot", false); data.put("Version", version);
		data.putString("LevelName", name); data.putInt("GameType", 1); data.putBoolean("allowCommands", true); data.putBoolean("hardcore", false); data.putBoolean("initialized", true); data.putLong("LastPlayed", System.currentTimeMillis()); data.putLong("Time", 0); data.putLong("DayTime", 6000);
		BlockPos spawn = client.player == null ? BlockPos.ORIGIN : client.player.getBlockPos();
		data.putInt("SpawnX", spawn.getX()); data.putInt("SpawnY", spawn.getY()); data.putInt("SpawnZ", spawn.getZ()); data.putFloat("SpawnAngle", 0);
		NbtCompound packs = new NbtCompound(); NbtList enabled = new NbtList(); enabled.add(NbtString.of("vanilla")); packs.put("Enabled", enabled); packs.put("Disabled", new NbtList()); data.put("DataPacks", packs);
		data.put("WorldGenSettings", voidSettings());
		NbtCompound rootTag = new NbtCompound(); rootTag.put("Data", data);
		try (OutputStream out = Files.newOutputStream(root.resolve("level.dat"))) { NbtIo.writeCompressed(rootTag, out); }
	}
	private static NbtCompound voidSettings() {
		NbtCompound dimensions = new NbtCompound();
		dimensions.put("minecraft:overworld", flat("minecraft:overworld", "minecraft:plains"));
		dimensions.put("minecraft:the_nether", flat("minecraft:the_nether", "minecraft:nether_wastes"));
		dimensions.put("minecraft:the_end", flat("minecraft:the_end", "minecraft:the_end"));
		NbtCompound all = new NbtCompound(); all.putLong("seed", 0); all.putBoolean("generate_features", false); all.putBoolean("bonus_chest", false); all.put("dimensions", dimensions); return all;
	}
	private static NbtCompound flat(String type, String biome) {
		NbtCompound settings = new NbtCompound(); settings.putString("biome", biome); settings.putBoolean("features", false); settings.putBoolean("lakes", false); settings.put("layers", new NbtList()); settings.put("structure_overrides", new NbtList());
		NbtCompound generator = new NbtCompound(); generator.putString("type", "minecraft:flat"); generator.put("settings", settings);
		NbtCompound dim = new NbtCompound(); dim.putString("type", type); dim.put("generator", generator); return dim;
	}
}
