package net.mcreator.thebackwoods.client.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
public class ModelListener<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("the_backwoods", "model_listener"), "main");
	public final ModelPart body;
	public final ModelPart head;
	public final ModelPart ear_left;
	public final ModelPart ear_right;
	public final ModelPart headwear;
	public final ModelPart right_arm;
	public final ModelPart left_arm;
	public final ModelPart right_leg;
	public final ModelPart left_leg;

	public ModelListener(ModelPart root) {
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.ear_left = this.head.getChild("ear_left");
		this.ear_right = this.head.getChild("ear_right");
		this.headwear = root.getChild("headwear");
		this.right_arm = root.getChild("right_arm");
		this.left_arm = root.getChild("left_arm");
		this.right_leg = root.getChild("right_leg");
		this.left_leg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(35, 36).addBox(-2.0F, -9.0F, -2.0F, 4.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -15.0F, 0.0F));
		PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(29, 32).addBox(-4.0F, -7.0F, -2.0F, 8.0F, 14.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 7.0012F, -0.2792F, 0.1396F, 0.0F, 0.0F));
		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(6, 6).addBox(-3.0F, -17.0F, -3.0F, 6.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -15.0F, 0.0F));
		PartDefinition ear_left = head.addOrReplaceChild("ear_left", CubeListBuilder.create(), PartPose.offsetAndRotation(2.8886F, -10.8595F, 0.329F, 2.7548F, -0.5629F, -2.7682F));
		PartDefinition ear_left_r1 = ear_left.addOrReplaceChild("ear_left_r1", CubeListBuilder.create().texOffs(0, 37).mirror().addBox(-0.2335F, -7.9857F, -3.5278F, 0.0F, 19.0F, 6.0F, new CubeDeformation(0.0015F)).mirror(false),
				PartPose.offsetAndRotation(3.8694F, -10.4051F, -10.3885F, 0.3491F, -0.3491F, 0.0F));
		PartDefinition ear_left_r2 = ear_left.addOrReplaceChild("ear_left_r2", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(-6.2335F, -61.9857F, -2.0278F, 0.0F, 23.0F, 6.0F, new CubeDeformation(0.0015F)).mirror(false),
				PartPose.offsetAndRotation(1.8614F, 37.8595F, 12.671F, 0.3491F, -0.3491F, 0.0F));
		PartDefinition ear_right = head.addOrReplaceChild("ear_right", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.8886F, -10.8595F, 0.329F, 2.7548F, 0.5629F, 2.7682F));
		PartDefinition ear_right_r1 = ear_right.addOrReplaceChild("ear_right_r1", CubeListBuilder.create().texOffs(26, -3).addBox(6.2335F, -62.9857F, -4.0278F, 0.0F, 24.0F, 8.0F, new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.8614F, 37.8595F, 12.671F, 0.3491F, 0.3491F, 0.0F));
		PartDefinition ear_right_r2 = ear_right.addOrReplaceChild("ear_right_r2", CubeListBuilder.create().texOffs(-1, 37).addBox(0.2335F, -8.9857F, -5.5278F, 0.0F, 20.0F, 6.0F, new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-3.8694F, -10.4051F, -10.3885F, 0.3491F, 0.3491F, 0.0F));
		PartDefinition headwear = partdefinition.addOrReplaceChild("headwear", CubeListBuilder.create().texOffs(9, 30).addBox(-2.0F, -17.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(-0.5F)), PartPose.offset(0.0F, -15.0F, 0.0F));
		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(55, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 37.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -13.0F, 0.0F));
		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(55, 0).mirror().addBox(-1.0F, -1.0F, -2.0F, 2.0F, 37.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offset(5.0F, -13.0F, 0.0F));
		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(55, 0).addBox(-1.0F, 4.0F, -0.25F, 2.0F, 26.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -6.0F, 0.0F));
		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(55, 0).mirror().addBox(-1.0F, 4.0F, -0.25F, 2.0F, 26.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offset(2.0F, -6.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		headwear.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.head.xRot = headPitch / (180F / (float) Math.PI);
		this.headwear.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.headwear.xRot = headPitch / (180F / (float) Math.PI);
		this.right_arm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
		this.left_leg.xRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
		this.left_arm.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
		this.right_leg.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
	}
}