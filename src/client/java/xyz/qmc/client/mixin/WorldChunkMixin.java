package xyz.qmc.client.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.qmc.client.SnapshotRecorder;

/** Marks a chunk for a debounced save after a block-change packet modifies it. */
@Mixin(WorldChunk.class)
abstract class WorldChunkMixin {
	@Inject(method = "setBlockState", at = @At("RETURN"))
	private void viewerMod$queueSave(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> callback) {
		WorldChunk chunk = (WorldChunk) (Object) this;
		World world = chunk.getWorld();
		if (world instanceof ClientWorld clientWorld) SnapshotRecorder.markDirty(clientWorld, chunk);
	}
}
