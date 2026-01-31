package org.example.plugin.resonance;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import org.example.plugin.ExamplePlugin;

public class ResonanceHelper {

	public static void emitResonance(World world, double blockRadius, Vector3d pos, long amount) {
		ExamplePlugin.LOGGER.atInfo().log("Emitting " + amount + " resonance!");
		// TODO: search around in the radius for resonance blocks and add resonance to them based on the amount of total blocks found.
	}
}
