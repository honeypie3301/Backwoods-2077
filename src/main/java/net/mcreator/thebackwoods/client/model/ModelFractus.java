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
public class ModelFractus<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("the_backwoods", "model_fractus"), "main");
	public final ModelPart inner_box;
	public final ModelPart core;
	public final ModelPart box;

	public ModelFractus(ModelPart root) {
		this.inner_box = root.getChild("inner_box");
		this.core = root.getChild("core");
		this.box = root.getChild("box");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition inner_box = partdefinition.addOrReplaceChild("inner_box", CubeListBuilder.create().texOffs(12, 6).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0713F, 16.0F, -0.1002F, 0.0F, -1.5708F, 0.0F));
		PartDefinition core = partdefinition.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0713F, 15.0F, -0.1002F));
		PartDefinition box = partdefinition.addOrReplaceChild("box", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0713F, 16.0F, -0.1002F));
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		inner_box.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		core.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		box.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}
}