package org.example.plugin.resonance.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.example.plugin.resonance.ResonanceBlock;
import org.example.plugin.resonance.ResonanceDestroyerBlock;
import org.example.plugin.resonance.event.ResonanceCreatedEvent;

public class ResonanceCreatedEventSystem extends WorldEventSystem<ChunkStore, ResonanceCreatedEvent> {

	private static final Query<ChunkStore> QUERY = Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());

	public ResonanceCreatedEventSystem() {
		super(ResonanceCreatedEvent.class);
	}

	@Override
	public void handle(@NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer, @NonNullDecl ResonanceCreatedEvent resonanceCreatedEvent) {
		double distSq = resonanceCreatedEvent.blockRange() * resonanceCreatedEvent.blockRange();

		store.forEachChunk(QUERY, (archetypeChunk, buffer) -> {

			for (int index = 0; index < archetypeChunk.size(); index++) {
				BlockSection blocks = archetypeChunk.getComponent(index, BlockSection.getComponentType());

				assert blocks != null;
				if (blocks.getTickingBlocksCountCopy() == 0) {
					continue;
				}

				ChunkSection section = archetypeChunk.getComponent(index, ChunkSection.getComponentType());

				assert section != null;

				BlockComponentChunk blockComponentChunk = commandBuffer.getComponent(section.getChunkColumnReference(), BlockComponentChunk.getComponentType());

				assert blockComponentChunk != null;

				blocks.forEachTicking(blockComponentChunk, commandBuffer, section.getY(), (blockComponentChunk1, commandBuffer1, localX, localY, localZ, blockId) ->
				{
					int worldX = ChunkUtil.worldCoordFromLocalCoord(section.getX(), localX);
					int worldY = ChunkUtil.worldCoordFromLocalCoord(section.getY(), localY);
					int worldZ = ChunkUtil.worldCoordFromLocalCoord(section.getZ(), localZ);

					// Don't interact with blocks outside our radius
					if (new Vector3d(worldX, worldY, worldZ).distanceSquaredTo(resonanceCreatedEvent.origin()) >= distSq) {
						return BlockTickStrategy.IGNORED;
					}

					Ref<ChunkStore> blockRef =
							blockComponentChunk1.getEntityReference(
									ChunkUtil.indexBlockInColumn(localX, localY, localZ)
							);

					if (blockRef == null) {
						return BlockTickStrategy.IGNORED;
					}

					// ResonanceBlock
					ResonanceBlock resonanceBlock =
							commandBuffer1.getComponent(blockRef, ResonanceBlock.getComponentType());

					if (resonanceBlock != null) {
						WorldChunk worldChunk =
								commandBuffer.getComponent(section.getChunkColumnReference(), WorldChunk.getComponentType());

						resonanceBlock.onResonanceCreated(worldChunk.getWorld(), resonanceCreatedEvent);
					}

					// DestroyerBlock
					ResonanceDestroyerBlock destroyer =
							commandBuffer1.getComponent(blockRef, ResonanceDestroyerBlock.getComponentType());

					if (destroyer != null) {
						WorldChunk worldChunk =
								commandBuffer.getComponent(section.getChunkColumnReference(), WorldChunk.getComponentType());

						destroyer.onResonance(
								worldChunk.getWorld(),
								worldX,
								worldY,
								worldZ,
								resonanceCreatedEvent
						);
					}

					return BlockTickStrategy.CONTINUE;

				});
			}
		});
	}
}
