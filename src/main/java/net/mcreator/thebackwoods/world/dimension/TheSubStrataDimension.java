package net.mcreator.thebackwoods.world.dimension;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.renderer.DimensionSpecialEffects;

import net.mcreator.thebackwoods.procedures.TheSubStrataPlayerEntersDimensionProcedure;

@EventBusSubscriber
public class TheSubStrataDimension {
	@EventBusSubscriber(Dist.CLIENT)
	public static class TheSubStrataSpecialEffectsHandler {
		@SubscribeEvent
		public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
			DimensionSpecialEffects customEffect = new DimensionSpecialEffects(Float.NaN, true, DimensionSpecialEffects.SkyType.NONE, false, false) {
				@Override
				public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
					return new Vec3(0.6235294118, 0.5450980392, 0.2509803922);
				}

				@Override
				public boolean isFoggyAt(int x, int y) {
					return true;
				}
			};
			event.register(ResourceLocation.parse("the_backwoods:the_sub_strata"), customEffect);
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimensionEvent(PlayerEvent.PlayerChangedDimensionEvent event) {
		Entity entity = event.getEntity();
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		if (event.getTo() == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_sub_strata"))) {
			TheSubStrataPlayerEntersDimensionProcedure.execute(world, x, z, entity);
		}
	}
}