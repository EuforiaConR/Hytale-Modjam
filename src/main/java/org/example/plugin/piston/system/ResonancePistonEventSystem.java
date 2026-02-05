package org.example.plugin.piston.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.example.plugin.ExamplePlugin;
import org.example.plugin.piston.ExtendResult;
import org.example.plugin.resonance.event.ResonanceCreatedEvent;
import org.example.plugin.util.BlockStateUtil;

public class ResonancePistonEventSystem extends WorldEventSystem<ChunkStore, ResonanceCreatedEvent> {


    private static final String[] BASE_KEYS = new String[]{
            "Piston_Block",
            "Piston/Piston_Block",
            "Items/Piston/Piston_Block"
    };

    private static final String[] HEAD_KEYS = new String[]{
            "Piston_Head",
            "Piston/Piston_Head",
            "Items/Piston/Piston_Head"
    };

    private static final Query<ChunkStore> QUERY =
            Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());

    public ResonancePistonEventSystem() {
        super(ResonanceCreatedEvent.class);
    }

    @Override
    public void handle(@NonNullDecl Store<ChunkStore> store,
                       @NonNullDecl CommandBuffer<ChunkStore> commandBuffer,
                       @NonNullDecl ResonanceCreatedEvent event) {

        ExamplePlugin.LOGGER.atInfo().log(
                "PISTON_EVT: amount=" + event.amountCreated() +
                        " radius=" + event.blockRange() +
                        " origin=" + event.origin()
        );

        final double distSq = event.blockRange() * event.blockRange();
        final Vector3d origin = event.origin();

        store.forEachChunk(QUERY, (archetypeChunk, buffer) -> {

            for (int index = 0; index < archetypeChunk.size(); index++) {

                BlockSection blocks = archetypeChunk.getComponent(index, BlockSection.getComponentType());
                if (blocks == null) continue;

                int ticking = blocks.getTickingBlocksCountCopy();
                if (ticking == 0) continue;

                ChunkSection section = archetypeChunk.getComponent(index, ChunkSection.getComponentType());
                if (section == null) continue;

                BlockComponentChunk blockComponentChunk =
                        commandBuffer.getComponent(section.getChunkColumnReference(), BlockComponentChunk.getComponentType());
                if (blockComponentChunk == null) continue;

                blocks.forEachTicking(blockComponentChunk, commandBuffer, section.getY(),
                        (blockComponentChunk1, commandBuffer1, localX, localY, localZ, blockId) -> {

                            int worldX = ChunkUtil.worldCoordFromLocalCoord(section.getX(), localX);
                            int worldY = ChunkUtil.worldCoordFromLocalCoord(section.getY(), localY);
                            int worldZ = ChunkUtil.worldCoordFromLocalCoord(section.getZ(), localZ);


                            if (new Vector3d(worldX, worldY, worldZ).distanceSquaredTo(origin) >= distSq) {
                                return BlockTickStrategy.IGNORED;
                            }

                            WorldChunk worldChunk =
                                    commandBuffer.getComponent(section.getChunkColumnReference(), WorldChunk.getComponentType());
                            if (worldChunk == null) return BlockTickStrategy.IGNORED;

                            World world = worldChunk.getWorld();


                            world.execute(() -> {
                                try {
                                    BlockType bt = world.getBlockType(worldX, worldY, worldZ);
                                    String id = getBlockId(bt);
                                    if (id == null) return;


                                    if (!id.contains("Piston")) return;

                                    BlockType type = world.getBlockType(worldX, worldY, worldZ);
                                    String stateId = BlockStateUtil.getStateIdFromDefinition(type);
                                    boolean isExtended = "Extend".equals(stateId);

                                    if (!isExtended) {
                                        extend(world, worldX, worldY, worldZ);
                                    } else {
                                        retract(world, worldX, worldY, worldZ);
                                    }

                                } catch (Throwable t) {
                                    ExamplePlugin.LOGGER.atSevere().log("PISTON ERROR inside world.execute at " + worldX + "," + worldY + "," + worldZ);
                                    t.printStackTrace();
                                }
                            });

                            return BlockTickStrategy.CONTINUE;
                        }
                );
            }
        });
    }

    private void extend(World world, int x, int y, int z) {
        Vector3i targetPos = getPistonTarget(world, x, y, z, 1);
        Vector3i blockedByPos = getPistonTarget(world, x, y, z, 2);

        BlockType targetType = world.getBlockType(targetPos.x, targetPos.y, targetPos.z);

        ExtendResult result;
        if (targetType == null || targetType == BlockType.EMPTY) {
            // No issues here, the piston target spot is free.
            result = ExtendResult.Empty;
        }
        else if(targetType.getMaterial() == BlockMaterial.Empty) {
            result = ExtendResult.BreakTarget;
        } else {
            // Check the blocked pos to see if we can push the block;
            BlockType blockedType = world.getBlockType(blockedByPos.x, blockedByPos.y, blockedByPos.z);
            if (blockedType == null || blockedType == BlockType.EMPTY) {
                result = ExtendResult.PushTarget;
            }
            else if (blockedType.getMaterial() == BlockMaterial.Empty) {
                result = ExtendResult.BreakBlocked;
            }
            else {
                result = ExtendResult.PistonBlocked;
            }
        }

        if (result == ExtendResult.PistonBlocked) {
            return;
        }

        // We should actually break the target in front of the piston (because it does not make sense to move it).
        if (result == ExtendResult.BreakTarget) {
            // TODO: Find out correct settings for breaking a block...
            world.breakBlock(targetPos.x, targetPos.y, targetPos.z, 0);
        }

        boolean push = result == ExtendResult.PushTarget || result == ExtendResult.BreakBlocked;

        // For now, we do not want to push complex blocks that have a component state (tile entities).
        if (world.getBlockComponentHolder(targetPos.x, targetPos.y, targetPos.z) != null) {
            return;
        }

        if (push) {
            world.breakBlock(targetPos.x, targetPos.y, targetPos.z, 0);
        }

        // Move piston visually and update hitbox.
        int localX = ChunkUtil.localCoordinate(x);
        int localZ = ChunkUtil.localCoordinate(z);
        world.setBlockInteractionState(new Vector3i(x, y, z), world.getBlockType(x, y, z), "Extend");
        world.getChunk(ChunkUtil.indexChunkFromBlock(x, z)).setTicking(localX, y, localZ, true);

        if (result == ExtendResult.BreakBlocked) {
            world.breakBlock(blockedByPos.x, blockedByPos.y, blockedByPos.z, 0);
        }

        if (push) {
            world.setBlock(blockedByPos.x, blockedByPos.y, blockedByPos.z, targetType.getId());
        }
    }

    private void retract(World world, int x, int y, int z) {
        Vector3i targetPos = getPistonTarget(world, x, y, z, 2);
        BlockType targetType = world.getBlockType(targetPos.x, targetPos.y, targetPos.z);

        Vector3i newPos = getPistonTarget(world, x, y, z, 1);

        world.breakBlock(targetPos.x, targetPos.y, targetPos.z, 0);

        int localX = ChunkUtil.localCoordinate(x);
        int localZ = ChunkUtil.localCoordinate(z);
        world.setBlockInteractionState(new Vector3i(x, y, z), world.getBlockType(x, y, z), "Retract");
        // setting the state resets the ticking functionality
        world.getChunk(ChunkUtil.indexChunkFromBlock(x, z)).setTicking(localX, y, localZ, true);

        if (targetType != null && targetType != BlockType.EMPTY
        && targetType.getMaterial() != BlockMaterial.Empty) {
            world.setBlock(newPos.x, newPos.y, newPos.z, targetType.getId());
        }
    }

    private static Vector3i getPistonTarget(World world, int x, int y, int z, int amountOfBlocksInFront) {
        // Get rotation of the piston block
        RotationTuple rotation = RotationTuple.get(world.getBlockRotationIndex(x, y, z));
        // Get the target position that the piston is pushing towards by rotating a unit vector with the piston's rotation.
		return new Vector3i(x, y, z).add(rotation.rotate(Vector3d.BACKWARD.clone().scale(amountOfBlocksInFront)).toVector3i());
    }

    private static void logBlockAt(World world, int x, int y, int z, String tag) {
        try {
            BlockType bt = world.getBlockType(x, y, z);
            String id = getBlockId(bt);
            ExamplePlugin.LOGGER.atInfo().log(tag + ": " + id + " at " + x + "," + y + "," + z);
        } catch (Throwable t) {
            ExamplePlugin.LOGGER.atSevere().log(tag + ": FAILED at " + x + "," + y + "," + z);
            t.printStackTrace();
        }
    }

    private static String trySetBlockAnyKey(World world, int x, int y, int z, String[] keys) {
        for (String key : keys) {
            try {
                world.setBlock(x, y, z, key);
                return key;
            } catch (Throwable ignored) {
                // probamos la siguiente
            }
        }
        return null;
    }

    private static boolean isAirLike(String id) {
        String s = id.toLowerCase();
        return s.contains("air") || s.contains("empty");
    }

    private static String getBlockId(BlockType blockType) {
        return blockType.getId();
    }

    public Query<ChunkStore> getQuery() {
        return QUERY;
    }
}
