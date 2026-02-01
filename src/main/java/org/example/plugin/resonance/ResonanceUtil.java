package org.example.plugin.resonance;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import org.example.plugin.resonance.event.ResonanceCreatedEvent;

public class ResonanceUtil {

	public static void emitResonance(World world, double blockRadius, Vector3d pos, int amount) {
		ResonanceCreatedEvent event = new ResonanceCreatedEvent(amount, pos, blockRadius);
		world.getChunkStore().getStore().invoke(event);
	}
}
