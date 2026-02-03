package org.example.plugin.piston.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.example.plugin.ExamplePlugin;
import org.example.plugin.resonance.event.ResonanceCreatedEvent;

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


    private static final Vector3i DIR = new Vector3i(0, 0, -1);

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


                                    if (!id.contains("Piston_Block")) return;

                                    ExamplePlugin.LOGGER.atInfo().log("PISTON_HIT: " + id + " at " + worldX + "," + worldY + "," + worldZ);

                                    logBlockAt(world, worldX, worldY, worldZ, "DEBUG_PISTON_SELF");
                                    logBlockAt(world,
                                            worldX + DIR.getX(),
                                            worldY + DIR.getY(),
                                            worldZ + DIR.getZ(),
                                            "DEBUG_IN_FRONT"
                                    );

                                    int hx = worldX + DIR.getX();
                                    int hy = worldY + DIR.getY();
                                    int hz = worldZ + DIR.getZ();

                                    String frontId = getBlockId(world.getBlockType(hx, hy, hz));
                                    boolean isExtended = (frontId != null && frontId.contains("Piston_Head"));

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
        int hx = x + DIR.getX();
        int hy = y + DIR.getY();
        int hz = z + DIR.getZ();

        String frontId = getBlockId(world.getBlockType(hx, hy, hz));
        if (frontId != null && !isAirLike(frontId)) {
            ExamplePlugin.LOGGER.atInfo().log("PISTON_BLOCKED: frontId=" + frontId);
            return;
        }

        String usedHeadKey = trySetBlockAnyKey(world, hx, hy, hz, HEAD_KEYS);
        if (usedHeadKey == null) {
            ExamplePlugin.LOGGER.atSevere().log(
                    "PISTON_EXTEND FAIL: no existe ninguna key para HEAD. Probadas: " + String.join(", ", HEAD_KEYS)
            );
            return;
        }

        ExamplePlugin.LOGGER.atInfo().log("PISTON_EXTEND OK headKey=" + usedHeadKey + " at " + hx + "," + hy + "," + hz);

        try {
            BlockType headBt = world.getBlockType(hx, hy, hz);
            if (headBt != null) {
                world.setBlockInteractionState(new Vector3i(hx, hy, hz), headBt, "Extend");
            }
        } catch (Throwable t) {
            ExamplePlugin.LOGGER.atSevere().log("PISTON_HEAD_ANIM Extend failed");
            t.printStackTrace();
        }
    }

    private void retract(World world, int x, int y, int z) {
        int hx = x + DIR.getX();
        int hy = y + DIR.getY();
        int hz = z + DIR.getZ();

        ExamplePlugin.LOGGER.atInfo().log("PISTON_RETRACT at " + x + "," + y + "," + z);

        try {
            BlockType headBt = world.getBlockType(hx, hy, hz);
            if (headBt != null) {
                world.setBlockInteractionState(new Vector3i(hx, hy, hz), headBt, "Retract");
            }
        } catch (Throwable t) {
            ExamplePlugin.LOGGER.atSevere().log("PISTON_HEAD_ANIM Retract failed");
            t.printStackTrace();
        }

        try {
            world.breakBlock(hx, hy, hz, 0);
        } catch (Throwable t) {
            ExamplePlugin.LOGGER.atSevere().log("PISTON_BREAK_HEAD failed at " + hx + "," + hy + "," + hz);
            t.printStackTrace();
        }
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
        if (blockType == null) return null;
        try {
            try {
                return (String) blockType.getClass().getMethod("getId").invoke(blockType);
            } catch (Exception ignored) {
                try {
                    return (String) blockType.getClass().getMethod("getName").invoke(blockType);
                } catch (Exception ignored2) {
                    return blockType.toString();
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Query<ChunkStore> getQuery() {
        return QUERY;
    }
}
