package org.example.plugin.util;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

import javax.annotation.Nullable;

public class BlockStateUtil {
	public static @Nullable String getStateIdFromDefinition(BlockType type) {
		return type.getState().getStateForBlock(type.getId());
	}
}
