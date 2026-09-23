package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.procedures.WoodweaverPlaybackConditionWalkProcedure;
import net.mcreator.thebackwoods.procedures.WoodweaverPlaybackConditionRetreatProcedure;
import net.mcreator.thebackwoods.entity.WoodweaverEntity;
import net.mcreator.thebackwoods.client.model.animations.WoodweaverAnimation;
import net.mcreator.thebackwoods.client.model.ModelWoodweaver;

public class WoodweaverRenderer extends MobRenderer<WoodweaverEntity, ModelWoodweaver<WoodweaverEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/woodweaver_skin.png");

	public WoodweaverRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelWoodweaver.LAYER_LOCATION)), 3f);
	}

	@Override
	public ResourceLocation getTextureLocation(WoodweaverEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelWoodweaver<WoodweaverEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<WoodweaverEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(WoodweaverEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				if (WoodweaverPlaybackConditionWalkProcedure.execute(entity))
					this.animateWalk(WoodweaverAnimation.Walk, limbSwing, limbSwingAmount, 1.3f, 1f);
				this.animate(entity.animationState1, WoodweaverAnimation.Hypnotize, ageInTicks, 1f);
				this.animate(entity.animationState2, WoodweaverAnimation.Attack_left, ageInTicks, 1f);
				this.animate(entity.animationState3, WoodweaverAnimation.Attack_right, ageInTicks, 1f);
				this.animate(entity.animationState4, WoodweaverAnimation.Jump, ageInTicks, 1f);
				this.animate(entity.animationState5, WoodweaverAnimation.Land, ageInTicks, 1f);
				this.animate(entity.animationState6, WoodweaverAnimation.Orb, ageInTicks, 1f);
				this.animate(entity.animationState7, WoodweaverAnimation.Hypnotize_reverse, ageInTicks, 1f);
				this.animate(entity.animationState8, WoodweaverAnimation.Hypnotize_reverse_2, ageInTicks, 1f);
				this.animate(entity.animationState9, WoodweaverAnimation.Hypnotize_reverse_3, ageInTicks, 1f);
				this.animate(entity.animationState10, WoodweaverAnimation.Attack_both, ageInTicks, 1f);
				this.animate(entity.animationState11, WoodweaverAnimation.Hook_Left, ageInTicks, 1f);
				this.animate(entity.animationState12, WoodweaverAnimation.Hook_Right, ageInTicks, 1f);
				this.animate(entity.animationState13, WoodweaverAnimation.Sweep_Right, ageInTicks, 1f);
				this.animate(entity.animationState14, WoodweaverAnimation.Sweep_Left, ageInTicks, 1f);
				this.animate(entity.animationState15, WoodweaverAnimation.Hypnotize_2, ageInTicks, 1f);
				this.animate(entity.animationState16, WoodweaverAnimation.Hypnotize_3, ageInTicks, 1f);
				this.animate(entity.animationState17, WoodweaverAnimation.Hypnotize_reverse_4, ageInTicks, 1f);
				this.animate(entity.animationState18, WoodweaverAnimation.Rotate_left, ageInTicks, 1f);
				this.animate(entity.animationState19, WoodweaverAnimation.Rotate_right, ageInTicks, 1f);
				if (WoodweaverPlaybackConditionRetreatProcedure.execute(entity))
					this.animateWalk(WoodweaverAnimation.Walk_backward, limbSwing, limbSwingAmount, 1.3f, 1f);
				this.animate(entity.animationState21, WoodweaverAnimation.Death, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(WoodweaverEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}