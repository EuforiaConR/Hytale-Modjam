package org.example.plugin.resonance;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.example.plugin.ExamplePlugin;
import org.example.plugin.resonance.event.ResonanceCreatedEvent;

import javax.annotation.Nullable;

public class ResonanceDestroyerBlock implements Component<ChunkStore> {

    public static ComponentType<ChunkStore, ResonanceDestroyerBlock> getComponentType() {
        return ExamplePlugin.get().getDestroyerComponentType();
    }

    public void onResonance(World world, int x, int y, int z, ResonanceCreatedEvent event) {
        Vector3i target = new Vector3i(x, y + 1, z);


        world.execute(()->{
            EntityStore store = world.getEntityStore();
            // TODO: extend the logic to allow breaking the block in the direction the block is facing
            BlockType blockType = world.getBlockType(target);
            String itemId = blockType.getId();
            ItemStack stack = new ItemStack(itemId, 1);
            var holders =
                    ItemComponent.generateItemDrops(
                            store.getStore(),
                            java.util.List.of(stack),
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
