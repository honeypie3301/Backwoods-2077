package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.procedures.*;
import net.mcreator.thebackwoods.entity.LignumVermisEntity;
import net.mcreator.thebackwoods.client.model.animations.LignumVermisAnimation;
import net.mcreator.thebackwoods.client.model.ModelLignumVermis;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class LignumVermisRenderer extends MobRenderer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/vermis.png");

	public LignumVermisRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelLignumVermis.LAYER_LOCATION)), 0.5f);
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/acacia_planks_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionAcaciaProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/bamboo_planks_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionBambooProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/birch_planks_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionBirchProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/cherry_planks_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionCherryProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/crimson_planks_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionCrimsonProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/dark_planks_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionDarkProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/mangrove_planks_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionMangroveProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/jungle_planks_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionJungleProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/warped_planks_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionWarpedProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionOakProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/dark_log_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionDarkLogProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/log_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionOakLogProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/cherry_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionCherryLogProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/acacia_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionAcaciaLogProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/jungle_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionJungleLogProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/spruce_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionSpruceLogProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/birch_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionBirchLogProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/mangrove_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionMangroveLogProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
		this.addLayer(new RenderLayer<LignumVermisEntity, ModelLignumVermis<LignumVermisEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/spruce_planks_vermis.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (LignumVermisDisplayConditionSpruceProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0));
				}
			}
		});
	}

	@Override
	public ResourceLocation getTextureLocation(LignumVermisEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelLignumVermis<LignumVermisEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<LignumVermisEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animateWalk(LignumVermisAnimation.wiggle, limbSwing, limbSwingAmount, 1f, 5f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(LignumVermisEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}