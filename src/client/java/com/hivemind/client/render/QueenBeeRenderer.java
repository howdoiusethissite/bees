package com.hivemind.client.render;

import com.hivemind.Hivemind;
import com.hivemind.client.ModModelLayers;
import com.hivemind.client.model.QueenBeeModel;
import com.hivemind.entity.QueenBee;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class QueenBeeRenderer extends MobRenderer<QueenBee, QueenBeeRenderState, QueenBeeModel> {
	private static final Identifier TEXTURE = Hivemind.id("textures/entity/queen_bee.png");

	public QueenBeeRenderer(final EntityRendererProvider.Context context) {
		super(context, new QueenBeeModel(context.bakeLayer(ModModelLayers.QUEEN_BEE)), 0.5F);
	}

	@Override
	public Identifier getTextureLocation(final QueenBeeRenderState state) {
		return TEXTURE;
	}

	@Override
	public QueenBeeRenderState createRenderState() {
		return new QueenBeeRenderState();
	}

	@Override
	public void extractRenderState(final QueenBee entity, final QueenBeeRenderState state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.swing = entity.getSwingAnimation(partialTicks);
	}
}
