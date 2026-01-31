package org.example.plugin.resonance.interaction;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.example.plugin.resonance.ResonanceHelper;

public class EmitResonanceInteraction extends SimpleInstantInteraction {

	public static final BuilderCodec<EmitResonanceInteraction> CODEC = BuilderCodec.builder(
			EmitResonanceInteraction.class, EmitResonanceInteraction::new, SimpleInstantInteraction.CODEC
	).append(new KeyedCodec<>("Amount", BuilderCodec.INTEGER), EmitResonanceInteraction::setEmissionAmount, EmitResonanceInteraction::emissionAmount)
			.add().build();

	int emissionAmount = 100;

	EmitResonanceInteraction() {}

	@Override
	protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NonNullDecl CooldownHandler cooldownHandler) {

		CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
		if (commandBuffer == null) {
			interactionContext.getState().state = InteractionState.Failed;
			return;
		}

		TransformComponent pos = commandBuffer.getComponent(interactionContext.getEntity(), TransformComponent.getComponentType());

		if (pos == null) {
			return;
		}

		ResonanceHelper.emitResonance(commandBuffer.getExternalData().getWorld(), 10.0, pos.getPosition(), emissionAmount);
	}

	public int emissionAmount() {
		return emissionAmount;
	}

	public void setEmissionAmount(int emissionAmount) {
		this.emissionAmount = emissionAmount;
	}
}
