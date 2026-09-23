package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;

import net.mcreator.thebackwoods.procedures.*;
import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class BlindspotSplinterRenderer extends HumanoidMobRenderer<BlindspotSplinterEntity, HumanoidModel<BlindspotSplinterEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/oak_biped.png");

	public BlindspotSplinterRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<BlindspotSplinterEntity>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
		this.addLayer(new RenderLayer<BlindspotSplinterEntity, HumanoidModel<BlindspotSplinterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/rot_stage_0.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, BlindspotSplinterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (BlindspotSplinterDisplayConditionProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<BlindspotSplinterEntity, HumanoidModel<BlindspotSplinterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/rot_stage_1.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, BlindspotSplinterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (BlindspotSplinterDisplayCondition2Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<BlindspotSplinterEntity, HumanoidModel<BlindspotSplinterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/rot_stage_2.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, BlindspotSplinterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (BlindspotSplinterDisplayCondition3Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<BlindspotSplinterEntity, HumanoidModel<BlindspotSplinterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/rot_stage_3.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, BlindspotSplinterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (BlindspotSplinterDisplayCondition4Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<BlindspotSplinterEntity, HumanoidModel<BlindspotSplinterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/rot_stage_4.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, BlindspotSplinterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (BlindspotSplinterDisplayCondition5Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<BlindspotSplinterEntity, HumanoidModel<BlindspotSplinterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/rot_stage_5.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, BlindspotSplinterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (BlindspotSplinterDisplayCondition6Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<BlindspotSplinterEntity, HumanoidModel<BlindspotSplinterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/rot_stage_6.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, BlindspotSplinterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (BlindspotSplinterDisplayCondition7Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<BlindspotSplinterEntity, HumanoidModel<BlindspotSplinterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/rot_stage_7.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, BlindspotSplinterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (BlindspotSplinterDisplayCondition8Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<BlindspotSplinterEntity, HumanoidModel<BlindspotSplinterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/rot_biped_base.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, BlindspotSplinterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (BlindspotSplinterDisplayCondition9Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
	}

	@Override
	public ResourceLocation getTextureLocation(BlindspotSplinterEntity entity) {
		return entityTexture;
	}
}