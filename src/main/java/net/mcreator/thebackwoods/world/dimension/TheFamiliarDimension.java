package net.mcreator.thebackwoods.world.dimension;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import net.mcreator.thebackwoods.procedures.TheFamiliarPlayerEntersDimensionProcedure;

@EventBusSubscriber
public class TheFamiliarDimension {
	@SubscribeEvent
	public static void onPlayerChangedDimensionEvent(PlayerEvent.PlayerChangedDimensionEvent event) {
		Entity entity = event.getEntity();
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		if (event.getTo() == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_familiar"))) {
			TheFamiliarPlayerEntersDimensionProcedure.execute(world, x, y, z, entity);
		}
	}
}