package com.hivemind.client.render;

import com.hivemind.Hivemind;
import com.hivemind.client.ModModelLayers;
import com.hivemind.client.model.GuardBeeGearModel;
import com.hivemind.entity.GuardBee;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.bee.AdultBeeModel;
import net.minecraft.client.model.animal.bee.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/** A vanilla bee body, drawn twice as large, with a helmet and spear layered on top. */
public class GuardBeeRenderer extends MobRenderer<GuardBee, GuardBeeRenderState, BeeModel> {
	private static final Identifier BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee.png");
	private static final Identifier ANGRY_BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_angry.png");
	private static final Identifier GEAR_TEXTURE = Hivemind.id("textures/entity/guard_bee_gear.png");
	public static final float SCALE = 2.0F;

	public GuardBeeRenderer(final EntityRendererProvider.Context context) {
		super(context, new AdultBeeModel(context.bakeLayer(ModelLayers.BEE)), 0.35F * SCALE);
		this.addLayer(new GearLayer(this, new GuardBeeGearModel(context.bakeLayer(ModModelLayers.GUARD_BEE_GEAR))));
	}

	@Override
	protected void scale(final GuardBeeRenderState state, final PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	@Override
	public Identifier getTextureLocation(final GuardBeeRenderState state) {
		return state.isAngry ? ANGRY_BEE_TEXTURE : BEE_TEXTURE;
	}

	@Override
	public GuardBeeRenderState createRenderState() {
		return new GuardBeeRenderState();
	}

	@Override
	public void extractRenderState(final GuardBee entity, final GuardBeeRenderState state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.rollAmount = 0.0F;
		state.hasStinger = true;
		state.hasNectar = false;
		state.isOnGround = entity.onGround() && entity.getDeltaMovement().lengthSqr() < 1.0E-7;
		state.isAngry = entity.isAngry();
		state.swing = entity.getSwingAnimation(partialTicks);
	}

	private static class GearLayer extends RenderLayer<GuardBeeRenderState, BeeModel> {
		private final GuardBeeGearModel gear;

		GearLayer(final RenderLayerParent<GuardBeeRenderState, BeeModel> parent, final GuardBeeGearModel gear) {
			super(parent);
			this.gear = gear;
		}

		@Override
		public void submit(
			final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final GuardBeeRenderState state, final float yRot, final float xRot
		) {
			coloredCutoutModelCopyLayerRender(this.gear, GEAR_TEXTURE, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
		}
	}
}
