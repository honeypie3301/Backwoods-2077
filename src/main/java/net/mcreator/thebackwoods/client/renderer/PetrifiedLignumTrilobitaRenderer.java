package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.procedures.PetrifiedLignumTrilobitaPlaybackWalkConditionProcedure;
import net.mcreator.thebackwoods.entity.PetrifiedLignumTrilobitaEntity;
import net.mcreator.thebackwoods.client.model.animations.LignumTrilobitaAnimation;
import net.mcreator.thebackwoods.client.model.ModelLignumTrilobita;

public class PetrifiedLignumTrilobitaRenderer extends MobRenderer<PetrifiedLignumTrilobitaEntity, ModelLignumTrilobita<PetrifiedLignumTrilobitaEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/petrified_lignum_trilobita.png");

	public PetrifiedLignumTrilobitaRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelLignumTrilobita.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(PetrifiedLignumTrilobitaEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelLignumTrilobita<PetrifiedLignumTrilobitaEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<PetrifiedLignumTrilobitaEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(PetrifiedLignumTrilobitaEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				if (PetrifiedLignumTrilobitaPlaybackWalkConditionProcedure.execute(entity))
					this.animateWalk(LignumTrilobitaAnimation.walk, limbSwing, limbSwingAmount, 7f, 888f);
				this.animate(entity.animationState1, LignumTrilobitaAnimation.hide, ageInTicks, 1f);
				this.animate(entity.animationState2, LignumTrilobitaAnimation.unhide, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(PetrifiedLignumTrilobitaEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}