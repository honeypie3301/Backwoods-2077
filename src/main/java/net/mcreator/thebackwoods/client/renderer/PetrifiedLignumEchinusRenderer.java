package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.entity.PetrifiedLignumEchinusEntity;
import net.mcreator.thebackwoods.client.model.animations.LignumEchinusAnimation;
import net.mcreator.thebackwoods.client.model.ModelLignumEchinus;

public class PetrifiedLignumEchinusRenderer extends MobRenderer<PetrifiedLignumEchinusEntity, ModelLignumEchinus<PetrifiedLignumEchinusEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/petrified_lignum_echinus.png");

	public PetrifiedLignumEchinusRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelLignumEchinus.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(PetrifiedLignumEchinusEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelLignumEchinus<PetrifiedLignumEchinusEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<PetrifiedLignumEchinusEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(PetrifiedLignumEchinusEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animateWalk(LignumEchinusAnimation.roll, limbSwing, limbSwingAmount, 2f, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(PetrifiedLignumEchinusEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}