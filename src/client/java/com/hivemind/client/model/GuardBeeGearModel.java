package com.hivemind.client.model;

import com.hivemind.client.render.GuardBeeRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The guard's helmet and spear. Shares the vanilla bee's "bone" pivot and copies its bobbing,
 * so the gear stays put on the bee body it is drawn over.
 */
public class GuardBeeGearModel extends EntityModel<GuardBeeRenderState> {
	private final ModelPart bone;
	private final ModelPart spear;

	public GuardBeeGearModel(final ModelPart root) {
		super(root);
		this.bone = root.getChild("bone");
		this.spear = this.bone.getChild("spear");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));
		bone.addOrReplaceChild(
			"helmet",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -5.0F, -5.5F, 8.0F, 2.0F, 7.0F, new CubeDeformation(0.05F))
				.texOffs(32, 0).addBox(-0.5F, -8.0F, -4.0F, 1.0F, 3.0F, 6.0F),
			PartPose.ZERO
		);
		bone.addOrReplaceChild(
			"spear",
			CubeListBuilder.create()
				.texOffs(0, 12).addBox(-0.5F, -0.5F, -9.0F, 1.0F, 1.0F, 12.0F)
				.texOffs(36, 12).addBox(-1.0F, -1.0F, -11.0F, 2.0F, 2.0F, 2.0F),
			PartPose.offsetAndRotation(-4.5F, 1.5F, 0.0F, 0.15F, 0.0F, 0.0F)
		);
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(final GuardBeeRenderState state) {
		super.setupAnim(state);
		if (!state.isAngry && !state.isOnGround) {
			float bob = Mth.cos(state.ageInTicks * 0.18F);
			this.bone.xRot = 0.1F + bob * (float) Math.PI * 0.025F;
			this.bone.y = this.bone.y - Mth.cos(state.ageInTicks * 0.18F) * 0.9F;
		}
		// Thrust the spear forward and back over the attack swing.
		float thrust = Mth.sin(state.swing * (float) Math.PI);
		this.spear.z = this.spear.z - thrust * 5.0F;
		this.spear.xRot = this.spear.xRot - thrust * 0.15F;
	}
}
