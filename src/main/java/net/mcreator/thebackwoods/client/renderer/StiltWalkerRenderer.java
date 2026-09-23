package net.mcreator.thebackwoods.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.mcreator.thebackwoods.entity.StiltWalkerEntity;
import net.mcreator.thebackwoods.client.model.ModelStiltWalker;

public class StiltWalkerRenderer extends MobRenderer<StiltWalkerEntity, ModelStiltWalker<StiltWalkerEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/stiltstalker_skin.png");

	public StiltWalkerRenderer(EntityRendererProvider.Context context) {
		super(context, new ModelStiltWalker<StiltWalkerEntity>(context.bakeLayer(ModelStiltWalker.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(StiltWalkerEntity entity) {
		return entityTexture;
	}
}