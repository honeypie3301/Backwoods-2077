package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.common.NeoForgeMod;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class LignumTrilobitaOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livingEntity0 && _livingEntity0.getAttributes().hasAttribute(NeoForgeMod.SWIM_SPEED))
			_livingEntity0.getAttribute(NeoForgeMod.SWIM_SPEED).setBaseValue(0.8);
		if (entity instanceof LivingEntity _livingEntity1 && _livingEntity1.getAttributes().hasAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY))
			_livingEntity1.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(0.8);
	}
}