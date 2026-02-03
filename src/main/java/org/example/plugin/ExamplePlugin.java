package org.example.plugin;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.example.plugin.resonance.ResonanceBlock;
import org.example.plugin.resonance.ResonanceDestroyerBlock;
import org.example.plugin.resonance.event.ResonanceCreatedEvent;
import org.example.plugin.resonance.system.ResonanceBlockInitializer;
import org.example.plugin.resonance.system.ResonanceBlockSystem;
import org.example.plugin.resonance.interaction.EmitResonanceInteraction;
import org.example.plugin.resonance.system.ResonanceCreatedEventSystem;


import javax.annotation.Nonnull;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class ExamplePlugin extends JavaPlugin {

    private ComponentType<ChunkStore, ResonanceBlock> resonanceStorageComponentType;
    private ComponentType<ChunkStore, ResonanceDestroyerBlock> destroyerComponentType;

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ExamplePlugin INSTANCE;

    public ExamplePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    public static ExamplePlugin get() {
        return INSTANCE;
    }

    @Override
    protected void setup() {
        INSTANCE = this;
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));

        this.getCodecRegistry(Interaction.CODEC).register("EmitResonance",EmitResonanceInteraction.class, EmitResonanceInteraction.CODEC);

        this.resonanceStorageComponentType = getChunkStoreRegistry().registerComponent(ResonanceBlock.class, "ResonanceBlock", ResonanceBlock.CODEC);


        this.destroyerComponentType =
                getChunkStoreRegistry().registerComponent(
                        ResonanceDestroyerBlock.class,
                        "ResonanceDestroyerBlock",
                        BuilderCodec.builder(ResonanceDestroyerBlock.class, ResonanceDestroyerBlock::new).build()
                );

    }

    @Override
    protected void start() {
        this.getChunkStoreRegistry().registerSystem(new ResonanceBlockSystem());
        this.getChunkStoreRegistry().registerSystem(new ResonanceBlockInitializer());

        this.getChunkStoreRegistry().registerEntityEventType(ResonanceCreatedEvent.class);
        this.getChunkStoreRegistry().registerSystem(new ResonanceCreatedEventSystem());

        this.getChunkStoreRegistry().registerSystem(new org.example.plugin.piston.system.ResonancePistonEventSystem());
    }




    public ComponentType<ChunkStore, ResonanceBlock> getResonanceStorageComponentType() {
        return this.resonanceStorageComponentType;
    }
    public ComponentType<ChunkStore, ResonanceDestroyerBlock> getDestroyerComponentType() {
        return destroyerComponentType;
    }


}