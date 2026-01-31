package org.example.plugin.resonance.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.example.plugin.resonance.ResonanceBlock;

public class ResonanceBlockInitializer extends RefSystem<ChunkStore> {

	@Override
	public void onEntityAdded(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl AddReason addReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
		BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
		if (info == null) return;

		ResonanceBlock generator = commandBuffer.getComponent(ref, ResonanceBlock.getComponentType());
		if (generator != null) {
			int x = ChunkUtil.xFromBlockInColumn(info.getIndex());
			int y = ChunkUtil.yFromBlockInColumn(info.getIndex());
			int z = ChunkUtil.zFromBlockInColumn(info.getIndex());

			WorldChunk worldChunk = commandBuffer.getComponent(info.getChunkRef(), WorldChunk.getComponentType());
			if (worldChunk != null) {
				worldChunk.setTicking(x, y, z, true);
			}
		}
	}

	@Override
	public void onEntityRemove(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl RemoveReason removeReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {

	}

	@NullableDecl
	@Override
	public Query<ChunkStore> getQuery() {
		return Query.and(BlockModule.BlockStateInfo.getComponentType(), ResonanceBlock.getComponentType());
	}
}
