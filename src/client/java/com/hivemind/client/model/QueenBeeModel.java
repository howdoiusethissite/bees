package com.hivemind.client.model;

import com.hivemind.client.render.QueenBeeRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * An anthropomorphic queen bee: hooded head with a gold mask and big compound eyes, hooked
 * antennae, a gold breastplate over a dark suit, segmented belly, fluffy ankles, a short striped
 * abdomen, and long translucent wings that hang behind her like a cape.
 *
 * Texture offsets must stay in sync with tools/gen_textures.py.
 */
public class QueenBeeModel extends EntityModel<QueenBeeRenderState> {
	private final ModelPart head;
	private final ModelPart rightAntenna;
	private final ModelPart leftAntenna;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart abdomen;

	public QueenBeeModel(final ModelPart root) {
		// Translucent so the wings show what's behind them.
		super(root, RenderTypes::entityTranslucent);
		this.head = root.getChild("head");
		this.rightAntenna = this.head.getChild("right_antenna");
		this.leftAntenna = this.head.getChild("left_antenna");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		ModelPart body = root.getChild("body");
		this.rightWing = body.getChild("right_wing");
		this.leftWing = body.getChild("left_wing");
		this.abdomen = body.getChild("abdomen");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild(
			"head",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
				.texOffs(32, 0).addBox(-3.5F, -5.5F, -4.6F, 3.0F, 3.0F, 1.0F)
				.texOffs(40, 0).addBox(0.5F, -5.5F, -4.6F, 3.0F, 3.0F, 1.0F)
				.texOffs(48, 0).addBox(-1.0F, -7.75F, -4.5F, 2.0F, 2.0F, 1.0F),
			PartPose.offset(0.0F, -2.0F, 0.0F)
		);
		PartDefinition rightAntenna = head.addOrReplaceChild(
			"right_antenna",
			CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F),
			PartPose.offsetAndRotation(-1.5F, -8.0F, -2.0F, -0.25F, 0.0F, -0.3F)
		);
		rightAntenna.addOrReplaceChild(
			"tip", CubeListBuilder.create().texOffs(64, 0).addBox(-3.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.6F)
		);
		PartDefinition leftAntenna = head.addOrReplaceChild(
			"left_antenna",
			CubeListBuilder.create().texOffs(60, 0).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F),
			PartPose.offsetAndRotation(1.5F, -8.0F, -2.0F, -0.25F, 0.0F, 0.3F)
		);
		leftAntenna.addOrReplaceChild(
			"tip", CubeListBuilder.create().texOffs(72, 0).addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, -0.6F)
		);

		PartDefinition body = root.addOrReplaceChild(
			"body",
			CubeListBuilder.create()
				.texOffs(0, 16).addBox(-4.0F, -12.0F, -2.5F, 8.0F, 6.0F, 5.0F)
				.texOffs(0, 28).addBox(-3.0F, -6.0F, -2.0F, 6.0F, 6.0F, 4.0F)
				.texOffs(28, 16).addBox(-3.5F, -1.0F, -2.5F, 7.0F, 2.0F, 5.0F)
				.texOffs(80, 0).addBox(-1.0F, -11.5F, -3.0F, 2.0F, 2.0F, 1.0F),
			PartPose.offset(0.0F, 10.0F, 0.0F)
		);
		PartDefinition abdomen = body.addOrReplaceChild(
			"abdomen",
			CubeListBuilder.create().texOffs(28, 24).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 7.0F),
			PartPose.offsetAndRotation(0.0F, -1.0F, 1.5F, 0.7F, 0.0F, 0.0F)
		);
		abdomen.addOrReplaceChild("stinger", CubeListBuilder.create().texOffs(86, 0).addBox(-0.5F, 0.0F, 7.0F, 1.0F, 1.0F, 2.0F), PartPose.ZERO);

		CubeDeformation wingDeformation = new CubeDeformation(0.001F);
		body.addOrReplaceChild(
			"right_wing",
			CubeListBuilder.create().texOffs(0, 64).addBox(-9.0F, 0.0F, 0.0F, 9.0F, 22.0F, 0.0F, wingDeformation),
			PartPose.offsetAndRotation(-0.5F, -11.0F, 2.6F, 0.12F, 0.35F, 0.2F)
		);
		body.addOrReplaceChild(
			"left_wing",
			CubeListBuilder.create().texOffs(20, 64).addBox(0.0F, 0.0F, 0.0F, 9.0F, 22.0F, 0.0F, wingDeformation),
			PartPose.offsetAndRotation(0.5F, -11.0F, 2.6F, 0.12F, -0.35F, -0.2F)
		);

		root.addOrReplaceChild(
			"right_arm",
			CubeListBuilder.create()
				.texOffs(56, 16).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 12.0F, 3.0F)
				.texOffs(80, 16).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 3.0F, 5.0F),
			PartPose.offsetAndRotation(-5.5F, -1.0F, 0.0F, 0.0F, 0.0F, 0.1F)
		);
		root.addOrReplaceChild(
			"left_arm",
			CubeListBuilder.create()
				.texOffs(68, 16).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 12.0F, 3.0F)
				.texOffs(100, 16).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 3.0F, 5.0F),
			PartPose.offsetAndRotation(5.5F, -1.0F, 0.0F, 0.0F, 0.0F, -0.1F)
		);

		addLeg(root, "right_leg", -2.0F, 0, 32, 56, 88);
		addLeg(root, "left_leg", 2.0F, 16, 44, 72, 102);
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static void addLeg(final PartDefinition root, final String name, final float x, final int thighU, final int shinU, final int fluffU, final int hoofU) {
		PartDefinition leg = root.addOrReplaceChild(
			name, CubeListBuilder.create().texOffs(thighU, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.offset(x, 10.0F, 0.0F)
		);
		leg.addOrReplaceChild(
			"shin",
			CubeListBuilder.create()
				.texOffs(shinU, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F)
				.texOffs(fluffU, 40).addBox(-2.0F, 3.0F, -2.0F, 4.0F, 3.0F, 4.0F)
				.texOffs(hoofU, 40).addBox(-1.5F, 6.0F, -2.5F, 3.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, 7.0F, 0.0F)
		);
	}

	@Override
	public void setupAnim(final QueenBeeRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;

		this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
		this.head.xRot = state.xRot * (float) (Math.PI / 180.0);

		float twitch = Mth.sin(age * 0.11F) * 0.06F;
		this.rightAntenna.zRot -= twitch;
		this.leftAntenna.zRot += twitch;
		this.rightAntenna.xRot += Mth.sin(age * 0.07F) * 0.04F;
		this.leftAntenna.xRot += Mth.sin(age * 0.07F + 1.3F) * 0.04F;

		// Wings shimmer gently; now and then she gives them a quick buzz.
		float buzz = Mth.sin(age * 0.05F) > 0.92F ? Mth.sin(age * 2.6F) * 0.12F : 0.0F;
		float shimmer = Mth.sin(age * 0.35F) * 0.04F + buzz;
		this.rightWing.yRot += shimmer;
		this.leftWing.yRot -= shimmer;

		// Idle arm sway and breathing.
		float breathe = Mth.cos(age * 0.09F) * 0.04F;
		this.rightArm.zRot += breathe;
		this.leftArm.zRot -= breathe;
		this.abdomen.xRot += Mth.sin(age * 0.09F) * 0.04F;

		// Walking, in the unlikely event she's ever moved.
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		this.rightLeg.xRot = Mth.cos(walk * 0.6662F) * 1.2F * speed;
		this.leftLeg.xRot = Mth.cos(walk * 0.6662F + (float) Math.PI) * 1.2F * speed;
		this.rightArm.xRot = Mth.cos(walk * 0.6662F + (float) Math.PI) * 0.8F * speed;
		this.leftArm.xRot = Mth.cos(walk * 0.6662F) * 0.8F * speed;

		// Reaching out to hand over a gift.
		if (state.swing > 0.0F) {
			float reach = Mth.sin(state.swing * (float) Math.PI);
			this.rightArm.xRot -= reach * 1.3F;
			this.rightArm.zRot -= reach * 0.2F;
		}
	}
}
