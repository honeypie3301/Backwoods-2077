package net.mcreator.thebackwoods.client.model;

import net.minecraft.world.entity.Entity;
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
public class ModelLignumVermis<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("the_backwoods", "model_lignum_vermis"), "main");
	public final ModelPart body1;
	public final ModelPart body2;
	public final ModelPart body3;
	public final ModelPart body4;
	public final ModelPart body5;
	public final ModelPart body6;
	public final ModelPart body7;
	public final ModelPart wing1;
	public final ModelPart wing2;
	public final ModelPart wing3;

	public ModelLignumVermis(ModelPart root) {
		this.body1 = root.getChild("body1");
		this.body2 = root.getChild("body2");
		this.body3 = root.getChild("body3");
		this.body4 = root.getChild("body4");
		this.body5 = root.getChild("body5");
		this.body6 = root.getChild("body6");
		this.body7 = root.getChild("body7");
		this.wing1 = root.getChild("wing1");
		this.wing2 = root.getChild("wing2");
		this.wing3 = root.getChild("wing3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition body1 = partdefinition.addOrReplaceChild("body1", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.0F, -7.0F));
		PartDefinition body2 = partdefinition.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(0, 4).addBox(-2.0F, -1.5F, -1.0F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.5F, -5.0F));
		PartDefinition body3 = partdefinition.addOrReplaceChild("body3", CubeListBuilder.create().texOffs(0, 9).addBox(-3.0F, -2.0F, -1.5F, 6.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.0F, -2.5F));
		PartDefinition body4 = partdefinition.addOrReplaceChild("body4", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.5F, 0.5F));
		PartDefinition body5 = partdefinition.addOrReplaceChild("body5", CubeListBuilder.create().texOffs(0, 22).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.0F, 3.5F));
		PartDefinition body6 = partdefinition.addOrReplaceChild("body6", CubeListBuilder.create().texOffs(11, 0).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.5F, 6.0F));
		PartDefinition body7 = partdefinition.addOrReplaceChild("body7", CubeListBuilder.create().texOffs(13, 4).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.5F, 8.0F));
		PartDefinition wing1 = partdefinition.addOrReplaceChild("wing1",
				CubeListBuilder.create().texOffs(23, 3).addBox(-5.0F, 0.0F, -1.5F, 10.0F, 8.0F, 0.0F, new CubeDeformation(-0.01F)).texOffs(23, 3).addBox(-5.0F, 0.0F, 1.5F, 10.0F, 8.0F, 0.0F, new CubeDeformation(-0.01F)),
				PartPose.offset(0.0F, 16.0F, -2.5F));
		PartDefinition wing2 = partdefinition.addOrReplaceChild("wing2",
				CubeListBuilder.create().texOffs(23, 14).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(23, 14).addBox(-3.0F, 0.0F, 1.5F, 6.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 20.0F, 3.5F));
		PartDefinition wing3 = partdefinition.addOrReplaceChild("wing3",
				CubeListBuilder.create().texOffs(22, 20).addBox(-3.0F, 0.0F, 0.5F, 6.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(22, 20).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 19.0F, -5.0F));
		return LayerDefinition.create(meshdefinition, 64, 32);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		body1.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		body2.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		body3.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		body4.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		body5.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		body6.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		body7.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		wing1.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		wing2.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		wing3.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}
}