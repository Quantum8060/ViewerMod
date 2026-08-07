package xyz.qmc.client;

import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.SerializedChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import xyz.qmc.ViewerMod;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A normal singleplayer save: received chunks go straight to Anvil region files. */
final class AnvilWorldArchive {
	private final Path root;
	private final Map<Path, RegionWriter> regions = new java.util.HashMap<>();
	private final java.util.Set<Long> entityChunks = new java.util.HashSet<>();

	AnvilWorldArchive(MinecraftClient client, String serverAddress) {
		this.root = client.runDirectory.toPath().resolve("saves").resolve("Viewer World " + digest(serverAddress));
		try {
			LevelFile.write(root, root.getFileName().toString(), client);
			ViewerMod.LOGGER.info("Writing native Anvil archive to {}", root);
		} catch (IOException e) { throw new IllegalStateException("Could not create Viewer world", e); }
	}

	void save(ClientWorld world, WorldChunk chunk) {
		try {
			byte[] nbt = encode(world, chunk);
			ChunkPos pos = chunk.getPos();
			Path folder = root.resolve(dimensionFolder(world));
			Path file = folder.resolve("region").resolve("r." + (pos.x >> 5) + "." + (pos.z >> 5) + ".mca");
			RegionWriter writer = regions.computeIfAbsent(file, path -> {
				try { return new RegionWriter(path); } catch (IOException e) { throw new ArchiveException(e); }
			});
			writer.write(pos.x, pos.z, nbt);
		} catch (ArchiveException | IOException e) {
			ViewerMod.LOGGER.warn("Could not save received chunk {}", chunk.getPos(), e);
		}
	}

	/** Writes entities using Minecraft's separate entity-region format. */
	void saveEntities(ClientWorld world, Iterable<Entity> entities) {
		Map<Long, NbtList> byChunk = new java.util.HashMap<>();
		for (Entity entity : entities) {
			if (entity == MinecraftClient.getInstance().player || entity.isRemoved()) continue;
			NbtWriteView view = NbtWriteView.create(ErrorReporter.EMPTY);
			if (!entity.saveData(view)) continue;
			NbtCompound data = view.getNbt();
			byChunk.computeIfAbsent(entity.getChunkPos().toLong(), ignored -> new NbtList()).add(data);
		}
		java.util.Set<Long> changed = new java.util.HashSet<>(entityChunks);
		changed.addAll(byChunk.keySet());
		for (long packed : changed) try {
			ChunkPos pos = new ChunkPos(packed);
			NbtCompound root = new NbtCompound();
			NbtHelper.putDataVersion(root);
			root.put("Position", new NbtIntArray(new int[] {pos.x, pos.z}));
			root.put("Entities", byChunk.getOrDefault(packed, new NbtList()));
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream out = new DataOutputStream(bytes)) { NbtIo.writeCompound(root, out); }
			Path file = root().resolve(dimensionFolder(world)).resolve("entities").resolve("r." + (pos.x >> 5) + "." + (pos.z >> 5) + ".mca");
			RegionWriter writer = regions.computeIfAbsent(file, path -> { try { return new RegionWriter(path); } catch (IOException e) { throw new ArchiveException(e); } });
			writer.write(pos.x, pos.z, bytes.toByteArray());
		} catch (ArchiveException | IOException e) { ViewerMod.LOGGER.warn("Could not save entities", e); }
		entityChunks.clear();
		entityChunks.addAll(byChunk.keySet());
	}
	private Path root() { return root; }

	private static byte[] encode(ClientWorld world, WorldChunk chunk) throws IOException {
		DynamicRegistryManager registries = world.getRegistryManager();
		LightingProvider lights = world.getLightingProvider();
		List<SerializedChunk.SectionData> sections = new ArrayList<>();
		for (int sectionY = lights.getBottomY(); sectionY < lights.getTopY(); sectionY++) {
			int i = chunk.sectionCoordToIndex(sectionY);
			ChunkSection section = i >= 0 && i < chunk.getSectionArray().length ? chunk.getSectionArray()[i].copy() : null;
			ChunkSectionPos sectionPos = ChunkSectionPos.from(chunk.getPos(), sectionY);
			ChunkNibbleArray block = lights.get(LightType.BLOCK).getLightSection(sectionPos);
			ChunkNibbleArray sky = lights.get(LightType.SKY).getLightSection(sectionPos);
			if (section != null || block != null || sky != null) sections.add(new SerializedChunk.SectionData(sectionY, section,
					block != null && !block.isUninitialized() ? block.copy() : null,
					sky != null && !sky.isUninitialized() ? sky.copy() : null));
		}
		Map<Heightmap.Type, long[]> maps = new EnumMap<>(Heightmap.Type.class);
		for (Map.Entry<Heightmap.Type, Heightmap> e : chunk.getHeightmaps()) {
			if (chunk.getStatus().getHeightmapTypes().contains(e.getKey())) maps.put(e.getKey(), e.getValue().asLongArray().clone());
		}
		List<NbtCompound> blockEntities = new ArrayList<>();
		for (BlockPos pos : chunk.getBlockEntityPositions()) {
			NbtCompound nbt = chunk.getPackedBlockEntityNbt(pos, registries);
			if (nbt != null) blockEntities.add(nbt);
		}
		NbtCompound structures = new NbtCompound();
		structures.put("starts", new NbtCompound());
		structures.put("References", new NbtCompound());
		SerializedChunk data = new SerializedChunk(net.minecraft.world.chunk.PalettesFactory.fromRegistryManager(registries),
				chunk.getPos(), chunk.getBottomSectionCoord(), world.getTime(), chunk.getInhabitedTime(), chunk.getStatus(),
				null, null, UpgradeData.NO_UPGRADE_DATA, null, maps, chunk.getTickSchedulers(world.getTime()), new ShortList[0],
				chunk.isLightOn(), sections, List.of(), blockEntities, structures);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(bytes)) { NbtIo.writeCompound(data.serialize(), out); }
		return bytes.toByteArray();
	}

	private static String dimensionFolder(ClientWorld world) {
		Identifier id = world.getRegistryKey().getValue();
		return id.equals(World.NETHER.getValue()) ? "DIM-1" : id.equals(World.END.getValue()) ? "DIM1" : "";
	}
	private static String digest(String value) {
		try { byte[] b = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder s = new StringBuilder(); for (int i = 0; i < 8; i++) s.append(String.format("%02x", b[i])); return s.toString();
		} catch (Exception e) { return "local"; }
	}
	void close() { for (RegionWriter writer : regions.values()) try { writer.close(); } catch (IOException ignored) { } regions.clear(); }
	private static final class ArchiveException extends RuntimeException { ArchiveException(IOException cause) { super(cause); } }
}
