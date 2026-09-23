/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.thebackwoods.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;

import net.mcreator.thebackwoods.client.renderer.*;

@EventBusSubscriber(Dist.CLIENT)
public class TheBackwoodsModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TheBackwoodsModEntities.SPLINTER.get(), SplinterRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.HOLLOW.get(), HollowRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.LOG_SPLINTER.get(), LogSplinterRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.ASH_WEAVER.get(), AshWeaverRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.ROT.get(), RotRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.BLINDSPOT_SPLINTER.get(), BlindspotSplinterRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.PETRIFIED_LOG_SPLINTER.get(), PetrifiedLogSplinterRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.STILT_WALKER.get(), StiltWalkerRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.LIGNUM_GIGAS.get(), LignumGigasRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.LIGNUM_VERMIS.get(), LignumVermisRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.FRACTUS.get(), FractusRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.FRACTUS_PRIME.get(), FractusPrimeRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.LIGNUM_PALUS.get(), LignumPalusRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.LIGNUM_SPINA.get(), LignumSpinaRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.LIGNUM_TRILOBITA.get(), LignumTrilobitaRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.LIGNUM_ECHINUS.get(), LignumEchinusRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.PETRIFIED_LIGNUM_SPINA.get(), PetrifiedLignumSpinaRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.PETRIFIED_LIGNUM_TRILOBITA.get(), PetrifiedLignumTrilobitaRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.PETRIFIED_LIGNUM_ECHINUS.get(), PetrifiedLignumEchinusRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.DORCELESS_SPLINTER.get(), DorcelessSplinterRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.TETHERLESS_PEARL_PROJECTILE.get(), ThrownItemRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.KYNE_SPLINTER.get(), KyneSplinterRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.RESONANT_TETHERLESS_PEARL_PROJECTILE.get(), ThrownItemRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.WOODWEAVER.get(), WoodweaverRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.PETRIFIED_OAK_GALL_PROJECTILE.get(), ThrownItemRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.HEWN_SPLINTER.get(), HewnSplinterRenderer::new);
		event.registerEntityRenderer(TheBackwoodsModEntities.VERDANT_ENGINE.get(), VerdantEngineRenderer::new);
	}
}