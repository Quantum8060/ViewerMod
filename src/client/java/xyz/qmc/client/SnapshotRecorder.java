package xyz.qmc.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

/** Receives chunks on the client thread and stores native Anvil chunks. */
public final class SnapshotRecorder {
	private static AnvilWorldArchive archive;
	private static final Queue<ChunkPos> pending = new ArrayDeque<>();
	private static final Map<Long, DirtyChunk> dirty = new HashMap<>();
	private static long nextEntitySave;

	static void beginSession(MinecraftClient client) {
		if (archive != null || client.getCurrentServerEntry() == null) return;
		archive = new AnvilWorldArchive(client, client.getCurrentServerEntry().address);
	}

	static void endSession() {
		if (archive != null) archive.close();
		archive = null;
		pending.clear();
		dirty.clear();
	}

	static void capture(ClientWorld world, WorldChunk chunk) {
		if (!ViewerConfig.enabled()) return;
		beginSession(MinecraftClient.getInstance());
		if (archive != null) archive.save(world, chunk);
	}

	/** Captures every chunk which is already present when saving is enabled. */
	static void queueLoadedArea(MinecraftClient client) {
		if (client.world == null || client.player == null || !ViewerConfig.enabled()) return;
		beginSession(client);
		pending.clear();
		int radius = client.options.getViewDistance().getValue() + 2;
		ChunkPos centre = client.player.getChunkPos();
		for (int z = centre.z - radius; z <= centre.z + radius; z++) for (int x = centre.x - radius; x <= centre.x + radius; x++) pending.add(new ChunkPos(x, z));
	}

	/** Limits the initial sweep to a few chunks per frame to avoid a multi-second freeze. */
	static void processPending(MinecraftClient client) {
		if (client.world == null || !ViewerConfig.enabled()) return;
		for (int i = 0; i < 4 && !pending.isEmpty(); i++) {
			ChunkPos pos = pending.remove();
			Chunk chunk = client.world.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
			if (chunk instanceof WorldChunk full) capture(client.world, full);
		}
		long now = System.currentTimeMillis();
		int saved = 0;
		for (Iterator<Map.Entry<Long, DirtyChunk>> it = dirty.entrySet().iterator(); it.hasNext() && saved < 4;) {
			DirtyChunk change = it.next().getValue();
			if (change.deadline > now) continue;
			capture(change.world, change.chunk);
			it.remove();
			saved++;
		}
		if (archive != null && now >= nextEntitySave) {
			archive.saveEntities(client.world, client.world.getEntities());
			nextEntitySave = now + 500;
		}
	}

	/** Called by the client-world mixin whenever a server block update changes a loaded chunk. */
	public static void markDirty(ClientWorld world, WorldChunk chunk) {
		if (ViewerConfig.enabled()) dirty.put(chunk.getPos().toLong(), new DirtyChunk(world, chunk, System.currentTimeMillis() + 250));
	}

	private record DirtyChunk(ClientWorld world, WorldChunk chunk, long deadline) { }
}
