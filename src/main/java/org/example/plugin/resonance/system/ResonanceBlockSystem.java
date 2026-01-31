package org.example.plugin.resonance.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.example.plugin.resonance.ResonanceBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ResonanceBlockSystem extends EntityTickingSystem<ChunkStore> {
	private static final Query<ChunkStore> QUERY = Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());

	public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
		BlockSection blocks = archetypeChunk.getComponent(index, BlockSection.getComponentType());

		assert blocks != null;
		if (blocks.getTickingBlocksCountCopy() != 0) {
			ChunkSection section = archetypeChunk.getComponent(index, ChunkSection.getComponentType());

			assert section != null;

			BlockComponentChunk blockComponentChunk = commandBuffer.getComponent(section.getChunkColumnReference(), BlockComponentChunk.getComponentType());

			assert blockComponentChunk != null;

			blocks.forEachTicking(blockComponentChunk, commandBuffer, section.getY(), (blockComponentChunk1, commandBuffer1, localX, localY, localZ, blockId) ->
			{
				Ref<ChunkStore> blockRef = blockComponentChunk1.getEntityReference(ChunkUtil.indexBlockInColumn(localX, localY, localZ));
				if (blockRef == null) {
					return BlockTickStrategy.IGNORED;
				} else {
					ResonanceBlock resonanceBlock = commandBuffer1.getComponent(blockRef, ResonanceBlock.getComponentType());
					if (resonanceBlock != null) {
						WorldChunk worldChunk = commandBuffer.getComponent(section.getChunkColumnReference(), WorldChunk.getComponentType());

						int globalX = localX + (worldChunk.getX() * 32);
						int globalZ = localZ + (worldChunk.getZ() * 32);

						resonanceBlock.runBlockAction(globalX, localY, globalZ, worldChunk.getWorld());

						return BlockTickStrategy.CONTINUE;

					} else {
						return BlockTickStrategy.IGNORED;
					}
				}
			});
		}
	}

	@Nullable
	public Query<ChunkStore> getQuery() {
		return QUERY;
	}}
