package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.entity.DorcelessSplinterEntity;
import net.mcreator.thebackwoods.client.model.animations.DorcelessAnimation;
import net.mcreator.thebackwoods.client.model.ModelDorceless;

public class DorcelessSplinterRenderer extends MobRenderer<DorcelessSplinterEntity, ModelDorceless<DorcelessSplinterEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/dorceless.png");

	public DorcelessSplinterRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelDorceless.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(DorcelessSplinterEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelDorceless<DorcelessSplinterEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<DorcelessSplinterEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(DorcelessSplinterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animateWalk(DorcelessAnimation.walk, limbSwing, limbSwingAmount, 3f, 1000f);
				this.animate(entity.animationState1, DorcelessAnimation.right_punch, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(DorcelessSplinterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}