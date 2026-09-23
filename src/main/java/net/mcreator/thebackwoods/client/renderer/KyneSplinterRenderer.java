package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.entity.KyneSplinterEntity;
import net.mcreator.thebackwoods.client.model.animations.KyneAnimation;
import net.mcreator.thebackwoods.client.model.ModelKyne;

public class KyneSplinterRenderer extends MobRenderer<KyneSplinterEntity, ModelKyne<KyneSplinterEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/kyne.png");

	public KyneSplinterRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelKyne.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(KyneSplinterEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelKyne<KyneSplinterEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<KyneSplinterEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(KyneSplinterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, KyneAnimation.right_punch, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(KyneSplinterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}