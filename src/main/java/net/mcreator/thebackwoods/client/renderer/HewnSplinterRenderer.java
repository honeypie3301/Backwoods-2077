package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.mcreator.thebackwoods.entity.HewnSplinterEntity;
import net.mcreator.thebackwoods.client.model.ModelHewn_Splinter;

public class HewnSplinterRenderer extends MobRenderer<HewnSplinterEntity, ModelHewn_Splinter<HewnSplinterEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/hewn_splinter.png");

	public HewnSplinterRenderer(EntityRendererProvider.Context context) {
		super(context, new ModelHewn_Splinter<HewnSplinterEntity>(context.bakeLayer(ModelHewn_Splinter.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(HewnSplinterEntity entity) {
		return entityTexture;
	}
}