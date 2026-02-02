package org.example.plugin.resonance.event;

import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;

public class ResonanceCreatedEvent extends EcsEvent {

	private final int amountCreated;
	private final Vector3d origin;
	private final Vector3i blockOrigin;
	private final double blockRange;

	public ResonanceCreatedEvent(int amountCreated, Vector3d origin, Vector3i blockOrigin, double blockRange) {
		this.amountCreated = amountCreated;
		this.origin = origin;
		this.blockOrigin = blockOrigin;
		this.blockRange = blockRange;
	}

	public double blockRange() {
		return blockRange;
	}

	public Vector3i blockOrigin() {
		return blockOrigin;
	}

	public Vector3d origin() {
		return origin;
	}

	public int amountCreated() {
		return amountCreated;
	}
}
