package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.procedures.FractusDisplayConditionNormalProcedure;
import net.mcreator.thebackwoods.procedures.FractusDisplayConditionAngerProcedure;
import net.mcreator.thebackwoods.entity.FractusEntity;
import net.mcreator.thebackwoods.client.model.animations.Fractus_1Animation;
import net.mcreator.thebackwoods.client.model.ModelFractus_1;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class FractusRenderer extends MobRenderer<FractusEntity, ModelFractus_1<FractusEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/fractus_skin_2.png");

	public FractusRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelFractus_1.LAYER_LOCATION)), 0.5f);
		this.addLayer(new RenderLayer<FractusEntity, ModelFractus_1<FractusEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/fractus_core_2_e.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, FractusEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (FractusDisplayConditionAngerProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.eyes(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
				}
			}
		});
		this.addLayer(new RenderLayer<FractusEntity, ModelFractus_1<FractusEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/fractus_core_e.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, FractusEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (FractusDisplayConditionNormalProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.eyes(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
				}
			}
		});
	}

	@Override
	public ResourceLocation getTextureLocation(FractusEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelFractus_1<FractusEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<FractusEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(FractusEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, Fractus_1Animation.idle, ageInTicks, 1f);
				this.animate(entity.animationState1, Fractus_1Animation.laser_activate, ageInTicks, 1f);
				this.animate(entity.animationState2, Fractus_1Animation.laser_deactivate, ageInTicks, 1f);
				this.animate(entity.animationState3, Fractus_1Animation.laser_burst_activate, ageInTicks, 1f);
				this.animate(entity.animationState4, Fractus_1Animation.laser_burst_deactivate, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(FractusEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}