package org.example.plugin.resonance;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.example.plugin.ExamplePlugin;
import org.example.plugin.resonance.event.ResonanceCreatedEvent;

import javax.annotation.Nullable;
import java.util.List;

public class ResonanceDestroyerBlock implements Component<ChunkStore> {

    public static ComponentType<ChunkStore, ResonanceDestroyerBlock> getComponentType() {
        return ExamplePlugin.get().getDestroyerComponentType();
    }

    public void onResonance(World world, int x, int y, int z, ResonanceCreatedEvent event) {

        world.execute(() -> {

             int rotation = world.getBlockRotationIndex(x, y, z);

             Vector3i[] directions = {
                    new Vector3i(0, 0, 1),   // 0
                    new Vector3i(1, 0, 0),   // 1
                    new Vector3i(0, 0, -1),  // 2
                    new Vector3i(-1, 0, 0)   // 3
            };

            if (rotation < 0 || rotation >= directions.length) {
                world.sendMessage(Message.raw("Rotacion desconocida: " + rotation));
                return;
            }

             Vector3i dir = directions[rotation];
             Vector3i target = new Vector3i(x + dir.x, y + dir.y, z + dir.z);

             EntityStore store = world.getEntityStore();
             BlockType blockType = world.getBlockType(target);

            if (blockType == null) {
                return;
            }

             ItemStack stack = new ItemStack(blockType.getId(), 1);

            var holders = ItemComponent.generateItemDrops(
                    store.getStore(),
                    List.of(stack),
                    target.toVector3d(),
                    Vector3f.ZERO
            );

            store.getStore().addEntities(holders, AddReason.SPAWN);
            world.breakBlock(target.x, target.y, target.z, 0);
        });
    }


    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new ResonanceDestroyerBlock();
    }
}
