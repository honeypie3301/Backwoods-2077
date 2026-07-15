/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.thebackwoods.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.thebackwoods.client.model.*;

@EventBusSubscriber(Dist.CLIENT)
public class TheBackwoodsModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ModelFractus.LAYER_LOCATION, ModelFractus::createBodyLayer);
		event.registerLayerDefinition(ModelStiltWalker.LAYER_LOCATION, ModelStiltWalker::createBodyLayer);
		event.registerLayerDefinition(ModelRot.LAYER_LOCATION, ModelRot::createBodyLayer);
		event.registerLayerDefinition(ModelLignumPalus.LAYER_LOCATION, ModelLignumPalus::createBodyLayer);
		event.registerLayerDefinition(ModelListener.LAYER_LOCATION, ModelListener::createBodyLayer);
		event.registerLayerDefinition(ModelLignumVermis.LAYER_LOCATION, ModelLignumVermis::createBodyLayer);
		event.registerLayerDefinition(ModelLignumGigas_1.LAYER_LOCATION, ModelLignumGigas_1::createBodyLayer);
	}
}