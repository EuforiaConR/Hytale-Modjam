package org.example.plugin.resonance;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

public enum EmissionType {
	Never,
	OnGenerationDone,
	;

	public static final Codec<EmissionType> CODEC = new EnumCodec<>(EmissionType.class);
}
