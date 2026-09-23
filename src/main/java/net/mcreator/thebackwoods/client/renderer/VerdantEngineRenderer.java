package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.procedures.VerdantEngineDisplayConditionCoreGlowProcedure;
import net.mcreator.thebackwoods.entity.VerdantEngineEntity;
import net.mcreator.thebackwoods.client.model.animations.VerdantEngineAnimation;
import net.mcreator.thebackwoods.client.model.ModelVerdantEngine;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class VerdantEngineRenderer extends MobRenderer<VerdantEngineEntity, ModelVerdantEngine<VerdantEngineEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/fractus_skin_2.png");

	public VerdantEngineRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelVerdantEngine.LAYER_LOCATION)), 0.5f);
		this.addLayer(new RenderLayer<VerdantEngineEntity, ModelVerdantEngine<VerdantEngineEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/fractus_core_e.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, VerdantEngineEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.eyes(LAYER_TEXTURE));
				this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
			}
		});
		this.addLayer(new RenderLayer<VerdantEngineEntity, ModelVerdantEngine<VerdantEngineEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/verdant_skin_full.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, VerdantEngineEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (VerdantEngineDisplayConditionCoreGlowProcedure.execute(world)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
	}

	@Override
	protected void scale(VerdantEngineEntity entity, PoseStack poseStack, float f) {
		poseStack.scale(15f, 15f, 15f);
	}

	@Override
	public ResourceLocation getTextureLocation(VerdantEngineEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelVerdantEngine<VerdantEngineEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<VerdantEngineEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(VerdantEngineEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, VerdantEngineAnimation.idle, ageInTicks, 1f);
				this.animate(entity.animationState1, VerdantEngineAnimation.laser_burst_activate, ageInTicks, 1f);
				this.animate(entity.animationState2, VerdantEngineAnimation.death, ageInTicks, 1f);
				this.animate(entity.animationState3, VerdantEngineAnimation.laser_burst_deactivate, ageInTicks, 1f);
				this.animate(entity.animationState4, VerdantEngineAnimation.laser_burst_activate_hold, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(VerdantEngineEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}