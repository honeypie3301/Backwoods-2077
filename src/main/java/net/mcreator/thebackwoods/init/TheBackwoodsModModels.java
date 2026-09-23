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
		event.registerLayerDefinition(ModelFractus_1.LAYER_LOCATION, ModelFractus_1::createBodyLayer);
		event.registerLayerDefinition(ModelFractus.LAYER_LOCATION, ModelFractus::createBodyLayer);
		event.registerLayerDefinition(ModelRot.LAYER_LOCATION, ModelRot::createBodyLayer);
		event.registerLayerDefinition(ModelLignumSpina.LAYER_LOCATION, ModelLignumSpina::createBodyLayer);
		event.registerLayerDefinition(ModelLignumTrilobita.LAYER_LOCATION, ModelLignumTrilobita::createBodyLayer);
		event.registerLayerDefinition(ModelLignumEchinus.LAYER_LOCATION, ModelLignumEchinus::createBodyLayer);
		event.registerLayerDefinition(ModelLignumPalus.LAYER_LOCATION, ModelLignumPalus::createBodyLayer);
		event.registerLayerDefinition(ModelLignumVermis.LAYER_LOCATION, ModelLignumVermis::createBodyLayer);
		event.registerLayerDefinition(ModelWoodweaver.LAYER_LOCATION, ModelWoodweaver::createBodyLayer);
		event.registerLayerDefinition(ModelLignumGigas.LAYER_LOCATION, ModelLignumGigas::createBodyLayer);
		event.registerLayerDefinition(ModelStiltWalker.LAYER_LOCATION, ModelStiltWalker::createBodyLayer);
		event.registerLayerDefinition(ModelVerdantEngine.LAYER_LOCATION, ModelVerdantEngine::createBodyLayer);
		event.registerLayerDefinition(ModelListener.LAYER_LOCATION, ModelListener::createBodyLayer);
		event.registerLayerDefinition(ModelDorceless.LAYER_LOCATION, ModelDorceless::createBodyLayer);
		event.registerLayerDefinition(ModelHewn_Splinter.LAYER_LOCATION, ModelHewn_Splinter::createBodyLayer);
		event.registerLayerDefinition(ModelKyne.LAYER_LOCATION, ModelKyne::createBodyLayer);
	}
}