package org.example.plugin.resonance;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.example.plugin.resonance.event.ResonanceCreatedEvent;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

public class ResonanceUtil {

	public static final double BLOCK_DETECTION_RANGE = 8.0;

	public static void emitResonance(World world, double blockRadius, Vector3i pos, int amount) {
		ResonanceCreatedEvent event = new ResonanceCreatedEvent(amount, pos.toVector3d(), pos, blockRadius);

		int soundIndex = SoundEvent.getAssetMap().getIndex("SFX_Simple_Resonance");
		EntityStore store = world.getEntityStore();
		SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX,pos.x, pos.y, pos.z,store.getStore());
		world.getChunkStore().getStore().invoke(event);
	}

	public static void emitResonance(World world, double blockRadius, Vector3d pos, int amount) {
		ResonanceCreatedEvent event = new ResonanceCreatedEvent(amount, pos, pos.toVector3i(), blockRadius);

		int soundIndex = SoundEvent.getAssetMap().getIndex("SFX_Simple_Resonance");
		EntityStore store = world.getEntityStore();
		SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX,pos.x, pos.y, pos.z,store.getStore());
		world.getChunkStore().getStore().invoke(event);
	}

	public static void forBlockInRange(World world, Vector3i pos, double range, ExtendedTickingBlockFunction function) {
		double rangeSq = range * range;
		world.getChunkStore().getStore().forEachChunk((archChunk, buffer) -> {
			for (int index = 0; index < archChunk.size(); index++) {
				BlockSection blocks = archChunk.getComponent(index, BlockSection.getComponentType());

				if (blocks == null) {
					continue;
				}

				if (blocks.getTickingBlocksCountCopy() == 0) {
					continue;
				}

				ChunkSection section = archChunk.getComponent(index, ChunkSection.getComponentType());

				assert section != null;

				BlockComponentChunk blockComponentChunk = buffer.getComponent(section.getChunkColumnReference(), BlockComponentChunk.getComponentType());

				assert blockComponentChunk != null;
				blocks.forEachTicking(blockComponentChunk, buffer, section.getY(), (chunk, chunkBuffer, localX, localY, localZ, blockId) -> {
					int worldX = ChunkUtil.worldCoordFromLocalCoord(section.getX(), localX);
					int worldY = ChunkUtil.worldCoordFromLocalCoord(section.getY(), localY);
					int worldZ = ChunkUtil.worldCoordFromLocalCoord(section.getZ(), localZ);
					Vector3i globalPos = new Vector3i(worldX, worldY, worldZ);
					// Don't interact with blocks outside our radius
					if (globalPos.distanceSquaredTo(pos) >= rangeSq) {
						return BlockTickStrategy.IGNORED;
					}

					Ref<ChunkStore> blockRef = chunk.getEntityReference(ChunkUtil.indexBlockInColumn(localX, localY, localZ));
					if (blockRef == null) {
						return BlockTickStrategy.IGNORED;
					} else {
						function.accept(chunkBuffer, blockRef, section, globalPos, blockId);
						return BlockTickStrategy.CONTINUE;
					}
				});
			}
		});
	}

	public static void forEachResonanceNeighbor(World world, Vector3i pos, BiConsumer<Vector3i, ResonanceBlock> action) {
		world.execute(() -> {
			forBlockInRange(world, pos, BLOCK_DETECTION_RANGE, (buffer, blockRef, chunkColRef, globalPos, blockId) -> {
				ResonanceBlock resonanceBlock = buffer.getComponent(blockRef, ResonanceBlock.getComponentType());
				if (resonanceBlock != null) {
					action.accept(globalPos, resonanceBlock);
				}
			});
		});
	}

	public static @Nullable ResonanceBlock resonanceBlockAtPosition(World world, Vector3i globalPos) {
		Holder<ChunkStore> componentHolder = world.getBlockComponentHolder(globalPos.x, globalPos.y, globalPos.z);

		if (componentHolder == null) {
			return null;
		}

		return componentHolder.getComponent(ResonanceBlock.getComponentType());
	}

	@FunctionalInterface
	public interface ExtendedTickingBlockFunction {

		void accept(CommandBuffer<ChunkStore> buffer, Ref<ChunkStore> blockRef, ChunkSection section, Vector3i globalPos, int blockId);
	}

	public static void spawnSoundWaveParticlesBlockCenter(Vector3i block, World world) {
		Holder<ChunkStore> holder = world.getBlockComponentHolder(block.x, block.y, block.z);

		ParticleUtil.spawnParticleEffect("Sound_Wave_System", new Vector3d(block.x + 0.5, block.y + 1.0, block.z + 0.5), world.getEntityStore().getStore());
	}
}
