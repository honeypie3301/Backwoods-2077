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

import net.mcreator.thebackwoods.procedures.FractusPrimeDisplayConditionProcedure;
import net.mcreator.thebackwoods.procedures.FractusPrimeDisplayConditionNormalProcedure;
import net.mcreator.thebackwoods.entity.FractusPrimeEntity;
import net.mcreator.thebackwoods.client.model.animations.Fractus_1Animation;
import net.mcreator.thebackwoods.client.model.ModelFractus_1;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class FractusPrimeRenderer extends MobRenderer<FractusPrimeEntity, ModelFractus_1<FractusPrimeEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/fractus_skin_2.png");

	public FractusPrimeRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelFractus_1.LAYER_LOCATION)), 0.5f);
		this.addLayer(new RenderLayer<FractusPrimeEntity, ModelFractus_1<FractusPrimeEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/fractus_core_2_e.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, FractusPrimeEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (FractusPrimeDisplayConditionProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.eyes(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
				}
			}
		});
		this.addLayer(new RenderLayer<FractusPrimeEntity, ModelFractus_1<FractusPrimeEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("the_backwoods:textures/entities/fractus_core_e.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, FractusPrimeEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (FractusPrimeDisplayConditionNormalProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.eyes(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
				}
			}
		});
	}

	@Override
	protected void scale(FractusPrimeEntity entity, PoseStack poseStack, float f) {
		poseStack.scale(2f, 2f, 2f);
	}

	@Override
	public ResourceLocation getTextureLocation(FractusPrimeEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelFractus_1<FractusPrimeEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<FractusPrimeEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(FractusPrimeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, Fractus_1Animation.idle, ageInTicks, 1f);
				this.animate(entity.animationState1, Fractus_1Animation.laser_activate, ageInTicks, 1f);
				this.animate(entity.animationState2, Fractus_1Animation.laser_deactivate, ageInTicks, 1f);
				this.animate(entity.animationState3, Fractus_1Animation.laser_burst_prime_activate, ageInTicks, 1f);
				this.animate(entity.animationState4, Fractus_1Animation.laser_burst_prime_deactivate, ageInTicks, 1f);
				this.animate(entity.animationState5, Fractus_1Animation.laser_prime_AOE, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(FractusPrimeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}