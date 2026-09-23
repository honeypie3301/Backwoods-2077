package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;

import net.mcreator.thebackwoods.entity.PetrifiedLogSplinterEntity;

public class PetrifiedLogSplinterRenderer extends HumanoidMobRenderer<PetrifiedLogSplinterEntity, HumanoidModel<PetrifiedLogSplinterEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/petrified_oak_log_biped.png");

	public PetrifiedLogSplinterRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<PetrifiedLogSplinterEntity>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(PetrifiedLogSplinterEntity entity) {
		return entityTexture;
	}
}