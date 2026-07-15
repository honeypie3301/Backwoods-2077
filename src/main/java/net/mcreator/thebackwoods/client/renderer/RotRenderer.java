package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.procedures.RotPlaybackConditionWalkProcedure;
import net.mcreator.thebackwoods.entity.RotEntity;
import net.mcreator.thebackwoods.client.model.animations.RotAnimation;
import net.mcreator.thebackwoods.client.model.ModelRot;

import com.mojang.blaze3d.vertex.PoseStack;

public class RotRenderer extends MobRenderer<RotEntity, ModelRot<RotEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/rotskin.png");

	public RotRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelRot.LAYER_LOCATION)), 0.7f);
	}

	@Override
	protected void scale(RotEntity entity, PoseStack poseStack, float f) {
		poseStack.scale(1.25f, 1.25f, 1.25f);
	}

	@Override
	public ResourceLocation getTextureLocation(RotEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelRot<RotEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<RotEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(RotEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, RotAnimation.rot_rider_kick, ageInTicks, 1f);
				if (RotPlaybackConditionWalkProcedure.execute(entity))
					this.animateWalk(RotAnimation.rot_walk, limbSwing, limbSwingAmount, 2f, 500f);
				this.animate(entity.animationState2, RotAnimation.rot_air_time, ageInTicks, 1f);
				this.animate(entity.animationState3, RotAnimation.rot_overhead, ageInTicks, 1f);
				this.animate(entity.animationState4, RotAnimation.rot_slam_crush, ageInTicks, 1f);
				this.animate(entity.animationState5, RotAnimation.rot_left_punch, ageInTicks, 1f);
				this.animate(entity.animationState6, RotAnimation.rot_right_punch, ageInTicks, 1f);
				this.animate(entity.animationState7, RotAnimation.rot_open_mouth_laser, ageInTicks, 1f);
				this.animate(entity.animationState8, RotAnimation.rot_close_mouth_laser, ageInTicks, 1f);
				this.animate(entity.animationState9, RotAnimation.rot_slam_charge, ageInTicks, 1f);
				this.animate(entity.animationState10, RotAnimation.rot_fall, ageInTicks, 1f);
				this.animate(entity.animationState11, RotAnimation.rot_sonic_boom, ageInTicks, 1f);
				this.animate(entity.animationState12, RotAnimation.rot_sonic_boom_large, ageInTicks, 1f);
				this.animate(entity.animationState13, RotAnimation.rot_armor_rip, ageInTicks, 1f);
				this.animate(entity.animationState14, RotAnimation.rot_block, ageInTicks, 1f);
				this.animate(entity.animationState15, RotAnimation.rot_block_finish, ageInTicks, 1f);
				this.animate(entity.animationState16, RotAnimation.rot_uppercut_charge, ageInTicks, 1f);
				this.animate(entity.animationState17, RotAnimation.rot_dropkick_charge, ageInTicks, 1f);
				this.animate(entity.animationState18, RotAnimation.rot_heavy_left_punch, ageInTicks, 1f);
				this.animate(entity.animationState19, RotAnimation.rot_heavy_right_punch, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(RotEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}