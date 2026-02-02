package org.example.plugin.resonance;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.example.plugin.ExamplePlugin;
import org.example.plugin.resonance.event.ResonanceCreatedEvent;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ResonanceBlock implements Component<ChunkStore>  {

	public Set<Vector3i> neighbors = new HashSet<>();
	public boolean active = false;

	private long resonance = 0;

	private long consumePerTick = 0; // Set to -1 to consume all instantly
	private long generatePerTick = 0;

	// When to emit resonance into the world.
	private EmissionType emissionType = EmissionType.Never;
	private long generateForTicks = 0;

	// Amount of times to start generating from the initial signal.
	private long generationCycles = 1;

	private long generateTicksLeft = 0;
	private long generationCyclesLeft = 0;

	public static final BuilderCodec<ResonanceBlock> CODEC;

	public ResonanceBlock() {
	}

	public long getResonance() {
		return resonance;
	}

	public void setResonance(long resonance) {
		this.resonance = Math.max(0, resonance);

		active = resonance > 0;
	}

	public long generatePerTick() {
		return generatePerTick;
	}

	public void setGeneratePerTick(long generatePerTick) {
		this.generatePerTick = generatePerTick;
	}

	public long consumePerTick() {
		return consumePerTick;
	}

	public void setConsumePerTick(long consumePerTick) {
		this.consumePerTick = consumePerTick;
	}

	public EmissionType emissionType() {
		return emissionType;
	}

	public void setEmissionType(EmissionType emissionType) {
		this.emissionType = emissionType;
	}

	public long generateForTicks() {
		return generateForTicks;
	}

	public void setGenerateForTicks(long generateForTicks) {
		this.generateForTicks = generateForTicks;
	}

	public long generationCycles() {
		return generationCycles;
	}

	public void setGenerationCycles(long generationCycles) {
		this.generationCycles = generationCycles;
	}

	public static ComponentType<ChunkStore, ResonanceBlock> getComponentType() {
		return ExamplePlugin.get().getResonanceStorageComponentType();
	}

	public void runBlockAction(int x, int y, int z, World world) {
		boolean was0 = generateTicksLeft == 0;
		// 1. Decrease amount of time to generate resonance.
		generateTicksLeft = Math.max(0, generateTicksLeft - 1);

		// Generate (potentially for the last time)
		long generate = was0 ? 0 : generatePerTick;
		setResonance(resonance + generate - consumePerTick);

		if (!was0 && generateTicksLeft == 0 && generationCyclesLeft > 0) {
			if (emissionType == EmissionType.OnGenerationDone) {
				ResonanceUtil.emitResonance(world, ResonanceUtil.BLOCK_DETECTION_RANGE, new Vector3i(x, y, z), (int) resonance);
				ResonanceUtil.spawnSoundWaveParticlesBlockCenter(new Vector3i(x, y, z), world);
				resonance = 0;
				generationCyclesLeft--;
				generateTicksLeft = generateForTicks;
			}
		}
	}

	public void onResonanceCreated(int ownX, int ownY, int ownZ, World world, ResonanceCreatedEvent event) {
		// Don't allow generators to gain more
		if (generatePerTick > 0 && event.amountCreated() > 0) {
			return;
		}

		setResonance(resonance + event.amountCreated());

		generationCyclesLeft = generationCycles;
		generateTicksLeft = generateForTicks;
	}

	@Nullable
	public Component<ChunkStore> clone() {
		ResonanceBlock block = new ResonanceBlock();
		block.resonance = resonance;
		block.consumePerTick = consumePerTick;
		block.generatePerTick = generatePerTick;
		block.emissionType = emissionType;
		block.generateForTicks = generateForTicks;
		block.generateTicksLeft = generateTicksLeft;
		block.generationCycles = generationCycles;
		return block;
	}

	Vector3i[] neighborsAsArray() {
		return neighbors.toArray(Vector3i[]::new);
	}

	void setNeighbors(Vector3i[] neighbors) {
		this.neighbors.clear();
		this.neighbors.addAll(Arrays.asList(neighbors));
	}

	static {
		CODEC = BuilderCodec.builder(ResonanceBlock.class, ResonanceBlock::new)
				.append(new KeyedCodec<>("Resonance", Codec.LONG), ResonanceBlock::setResonance, ResonanceBlock::getResonance).add()
				.append(new KeyedCodec<>("GeneratePerTick", Codec.LONG), ResonanceBlock::setGeneratePerTick, ResonanceBlock::generatePerTick).add()
				.append(new KeyedCodec<>("ConsumePerTick", Codec.LONG), ResonanceBlock::setConsumePerTick, ResonanceBlock::consumePerTick).add()
				.append(new KeyedCodec<>("Neighbors", new ArrayCodec<>(Vector3i.CODEC, Vector3i[]::new)), ResonanceBlock::setNeighbors, ResonanceBlock::neighborsAsArray).add()
				.append(new KeyedCodec<>("EmissionType", EmissionType.CODEC), ResonanceBlock::setEmissionType, ResonanceBlock::emissionType).add()
				.append(new KeyedCodec<>("GenerateForTicks", Codec.LONG), ResonanceBlock::setGenerateForTicks, ResonanceBlock::generateForTicks).add()
				.append(new KeyedCodec<>("GenerationCycles", Codec.LONG), ResonanceBlock::setGenerationCycles, ResonanceBlock::generationCycles).add()
				.build();
	}
}
