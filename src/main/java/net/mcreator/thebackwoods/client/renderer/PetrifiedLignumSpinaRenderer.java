package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.mcreator.thebackwoods.entity.PetrifiedLignumSpinaEntity;
import net.mcreator.thebackwoods.client.model.animations.LignumSpinaAnimation;
import net.mcreator.thebackwoods.client.model.ModelLignumSpina;

public class PetrifiedLignumSpinaRenderer extends MobRenderer<PetrifiedLignumSpinaEntity, ModelLignumSpina<PetrifiedLignumSpinaEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/petrified_lignum_spina.png");

	public PetrifiedLignumSpinaRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(ModelLignumSpina.LAYER_LOCATION)), 0.3f);
	}

	@Override
	public ResourceLocation getTextureLocation(PetrifiedLignumSpinaEntity entity) {
		return entityTexture;
	}

	private static final class AnimatedModel extends ModelLignumSpina<PetrifiedLignumSpinaEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<PetrifiedLignumSpinaEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(PetrifiedLignumSpinaEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animateWalk(LignumSpinaAnimation.walk_2, limbSwing, limbSwingAmount, 8f, 1000f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(PetrifiedLignumSpinaEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}