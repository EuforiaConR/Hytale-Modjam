package org.example.plugin.resonance;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.example.plugin.ExamplePlugin;

import javax.annotation.Nullable;
import java.awt.*;

public class ResonanceBlock implements Component<ChunkStore>  {

	public boolean active = false;

	private long resonance = 0;

	private long consumePerTick = 0;
	private long generatePerTick = 0;

	public static final BuilderCodec<ResonanceBlock> CODEC;

	public ResonanceBlock() {
	}

	public long getResonance() {
		return resonance;
	}

	public void setResonance(long resonance) {
		this.resonance = resonance;
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

	public static ComponentType<ChunkStore, ResonanceBlock> getComponentType() {
		return ExamplePlugin.get().getResonanceStorageComponentType();
	}

	public void runBlockAction(int x, int y, int z, World world) {
		world.execute(() -> {
			resonance += generatePerTick;
			resonance = Math.max(0, resonance - consumePerTick);
			active = resonance > 0;
		});
	}

	@Nullable
	public Component<ChunkStore> clone() {
		ResonanceBlock block = new ResonanceBlock();
		block.resonance = resonance;
		block.consumePerTick = consumePerTick;
		block.generatePerTick = generatePerTick;
		return block;
	}

	static {
		CODEC = BuilderCodec.builder(ResonanceBlock.class, ResonanceBlock::new)
				.append(new KeyedCodec<>("Resonance", Codec.LONG), ResonanceBlock::setResonance, ResonanceBlock::getResonance).add()
				.append(new KeyedCodec<>("GeneratePerTick", Codec.LONG), ResonanceBlock::setGeneratePerTick, ResonanceBlock::generatePerTick).add()
				.append(new KeyedCodec<>("ConsumePerTick", Codec.LONG), ResonanceBlock::setConsumePerTick, ResonanceBlock::consumePerTick).add()
				.build();
	}
}
