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
import org.example.plugin.ExamplePlugin;
import org.example.plugin.resonance.ResonanceBlock;
import org.example.plugin.resonance.ResonanceDestroyerBlock;
import org.example.plugin.resonance.ResonanceMusicBlock; // ✅ NUEVO
import org.example.plugin.resonance.event.ResonanceCreatedEvent;

public class ResonanceCreatedEventSystem extends WorldEventSystem<ChunkStore, ResonanceCreatedEvent> {

	private static final Query<ChunkStore> QUERY =
			Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());

	public ResonanceCreatedEventSystem() {
		super(ResonanceCreatedEvent.class);
	}

	@Override
	public void handle(@NonNullDecl Store<ChunkStore> store,
					   @NonNullDecl CommandBuffer<ChunkStore> commandBuffer,
					   @NonNullDecl ResonanceCreatedEvent event) {

		ExamplePlugin.LOGGER.atInfo().log(
				"RESONANCE_EVT: amount=" + event.amountCreated()
						+ " radius=" + event.blockRange()
						+ " origin=" + event.origin()
		);

		double distSq = event.blockRange() * event.blockRange();

		store.forEachChunk(QUERY, (archetypeChunk, buffer) -> {
			for (int index = 0; index < archetypeChunk.size(); index++) {

				BlockSection blocks = archetypeChunk.getComponent(index, BlockSection.getComponentType());
				if (blocks == null) continue;

				if (blocks.getTickingBlocksCountCopy() == 0) continue;

				ChunkSection section = archetypeChunk.getComponent(index, ChunkSection.getComponentType());
				if (section == null) continue;

				BlockComponentChunk blockComponentChunk =
						commandBuffer.getComponent(section.getChunkColumnReference(), BlockComponentChunk.getComponentType());
				if (blockComponentChunk == null) continue;

				blocks.forEachTicking(blockComponentChunk, commandBuffer, section.getY(),
						(chunk, chunkBuffer, localX, localY, localZ, blockId) -> {

							int worldX = ChunkUtil.worldCoordFromLocalCoord(section.getX(), localX);
							int worldY = ChunkUtil.worldCoordFromLocalCoord(section.getY(), localY);
							int worldZ = ChunkUtil.worldCoordFromLocalCoord(section.getZ(), localZ);

							if (new Vector3d(worldX, worldY, worldZ).distanceSquaredTo(event.origin()) >= distSq) {
								return BlockTickStrategy.IGNORED;
							}

							Ref<ChunkStore> blockRef =
									chunk.getEntityReference(ChunkUtil.indexBlockInColumn(localX, localY, localZ));
							if (blockRef == null) {
								return BlockTickStrategy.IGNORED;
							}

							WorldChunk worldChunk =
									commandBuffer.getComponent(section.getChunkColumnReference(), WorldChunk.getComponentType());
							if (worldChunk == null) {
								return BlockTickStrategy.IGNORED;
							}

							// ResonanceBlock
							ResonanceBlock resonanceBlock =
									chunkBuffer.getComponent(blockRef, ResonanceBlock.getComponentType());
							if (resonanceBlock != null) {
								resonanceBlock.onResonanceCreated(worldChunk.getWorld(), event);
							}

							// DestroyerBlock
							ResonanceDestroyerBlock destroyer =
									chunkBuffer.getComponent(blockRef, ResonanceDestroyerBlock.getComponentType());
							if (destroyer != null) {
								destroyer.onResonance(worldChunk.getWorld(), worldX, worldY, worldZ, event);
							}

							// MusicBlock
							ResonanceMusicBlock music =
									chunkBuffer.getComponent(blockRef, ResonanceMusicBlock.getComponentType());
							if (music != null) {
								music.onResonance(worldChunk.getWorld(), worldX, worldY, worldZ, event);
							}

							return BlockTickStrategy.CONTINUE;
						});
			}
		});
	}
}
