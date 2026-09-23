package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.entity.LignumPalusEntity;
import net.mcreator.thebackwoods.client.model.animations.LignumPalusAnimation;
import net.mcreator.thebackwoods.client.model.ModelLignumPalus;

public class LignumPalusRenderer extends MobRenderer<LignumPalusEntity, ModelLignumPalus<LignumPalusEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/lignum_palus_skin_3.png");

	public LignumPalusRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelLignumPalus.LAYER_LOCATION)), 0.4f);
	}

	@Override
	public ResourceLocation getTextureLocation(LignumPalusEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelLignumPalus<LignumPalusEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<LignumPalusEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(LignumPalusEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, LignumPalusAnimation.open_mouth, ageInTicks, 1f);
				this.animate(entity.animationState1, LignumPalusAnimation.close_mouth, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(LignumPalusEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}