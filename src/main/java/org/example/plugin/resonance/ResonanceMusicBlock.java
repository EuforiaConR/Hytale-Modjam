package org.example.plugin.resonance;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.example.plugin.ExamplePlugin;
import org.example.plugin.resonance.event.ResonanceCreatedEvent;

import javax.annotation.Nullable;

public class ResonanceMusicBlock implements Component<ChunkStore> {

    // ✅ MISMO SONIDO QUE TU ExampleCommand (debug fijo)
    private static final String DEBUG_SOUND_ID = "SFX_Cactus_Large_Hit";

    // ======== CONFIG (desde JSON via codec) ========
    private float volume = 1.0f;

    // pitch = basePitch + amountCreated * pitchPerAmount
    private float basePitch = 1.0f;
    private float pitchPerAmount = 0.0f;

    // Distancia a la que se escucha
    private double hearRadius = 16.0;

    // Anti-spam en ms
    private long cooldownMs = 200;

    // ======== RUNTIME (no serializado) ========
    private transient long lastPlayedAtMs = 0L;

    // ======== CODEC ========
    public static final BuilderCodec<ResonanceMusicBlock> CODEC =
            BuilderCodec.builder(ResonanceMusicBlock.class, ResonanceMusicBlock::new)
                    .append(new KeyedCodec<>("Volume", Codec.FLOAT),
                            ResonanceMusicBlock::setVolume, ResonanceMusicBlock::volume).add()
                    .append(new KeyedCodec<>("BasePitch", Codec.FLOAT),
                            ResonanceMusicBlock::setBasePitch, ResonanceMusicBlock::basePitch).add()
                    .append(new KeyedCodec<>("PitchPerAmount", Codec.FLOAT),
                            ResonanceMusicBlock::setPitchPerAmount, ResonanceMusicBlock::pitchPerAmount).add()
                    .append(new KeyedCodec<>("HearRadius", Codec.DOUBLE),
                            ResonanceMusicBlock::setHearRadius, ResonanceMusicBlock::hearRadius).add()
                    .append(new KeyedCodec<>("CooldownMs", Codec.LONG),
                            ResonanceMusicBlock::setCooldownMs, ResonanceMusicBlock::cooldownMs).add()
                    .build();

    public static ComponentType<ChunkStore, ResonanceMusicBlock> getComponentType() {
        return ExamplePlugin.get().getMusicBlockComponentType();
    }

    public void onResonance(World world, int x, int y, int z, ResonanceCreatedEvent event) {
        // ✅ Todo lo que toca mundo / store dentro de world.execute
        world.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                if (cooldownMs > 0 && now - lastPlayedAtMs < cooldownMs) return;
                lastPlayedAtMs = now;

                int soundIndex = SoundEvent.getAssetMap().getIndex(DEBUG_SOUND_ID);
                if (soundIndex < 0) {
                    ExamplePlugin.LOGGER.atSevere().log("ResonanceMusicBlock: no existe SoundEvent: " + DEBUG_SOUND_ID);
                    return;
                }

                float pitch = basePitch + (event.amountCreated() * pitchPerAmount);
                pitch = clamp(pitch, 0.1f, 3.0f);

                float vol = clamp(volume, 0.0f, 3.0f);

                double sx = x + 0.5;
                double sy = y + 0.5;
                double sz = z + 0.5;

                broadcast3dToNearbyPlayers(world, soundIndex, sx, sy, sz, vol, pitch, hearRadius);

                // Log opcional para debug
                ExamplePlugin.LOGGER.atInfo().log(
                        "MUSIC_BLOCK: played " + DEBUG_SOUND_ID +
                                " pitch=" + pitch + " vol=" + vol +
                                " at " + x + "," + y + "," + z
                );

            } catch (Throwable t) {
                ExamplePlugin.LOGGER.atSevere().log("ResonanceMusicBlock: error en onResonance");
                t.printStackTrace();
            }
        });
    }

    private static void broadcast3dToNearbyPlayers(World world,
                                                   int soundIndex,
                                                   double x, double y, double z,
                                                   float volume, float pitch,
                                                   double radius) {

        EntityStore es = world.getEntityStore();
        if (es == null) return;

        Store<EntityStore> store = es.getStore();
        if (store == null) return;

        double r2 = radius * radius;

        Query<EntityStore> q = Query.and(
                PlayerRef.getComponentType(),
                TransformComponent.getComponentType()
        );

        store.forEachChunk(q, (arch, buf) -> {
            for (int i = 0; i < arch.size(); i++) {
                Ref<EntityStore> ref = arch.getReferenceTo(i);
                if (ref == null || !ref.isValid()) continue;

                TransformComponent tc = buf.getComponent(ref, TransformComponent.getComponentType());
                if (tc == null) continue;

                Vector3d p = tc.getPosition();
                double dx = p.getX() - x;
                double dy = p.getY() - y;
                double dz = p.getZ() - z;

                if ((dx * dx + dy * dy + dz * dz) > r2) continue;

                // ✅ MISMA OVERLOAD QUE TU ExampleCommand
                SoundUtil.playSoundEvent3dToPlayer(
                        ref,
                        soundIndex,
                        SoundCategory.UI,
                        x, y, z,
                        volume,
                        pitch,
                        store
                );
            }
        });
    }

    // ======== getters/setters (codec) ========
    public float volume() { return volume; }
    public void setVolume(float volume) { this.volume = volume; }

    public float basePitch() { return basePitch; }
    public void setBasePitch(float basePitch) { this.basePitch = basePitch; }

    public float pitchPerAmount() { return pitchPerAmount; }
    public void setPitchPerAmount(float pitchPerAmount) { this.pitchPerAmount = pitchPerAmount; }

    public double hearRadius() { return hearRadius; }
    public void setHearRadius(double hearRadius) { this.hearRadius = hearRadius; }

    public long cooldownMs() { return cooldownMs; }
    public void setCooldownMs(long cooldownMs) { this.cooldownMs = cooldownMs; }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        ResonanceMusicBlock b = new ResonanceMusicBlock();
        b.volume = this.volume;
        b.basePitch = this.basePitch;
        b.pitchPerAmount = this.pitchPerAmount;
        b.hearRadius = this.hearRadius;
        b.cooldownMs = this.cooldownMs;
        return b;
    }
}
