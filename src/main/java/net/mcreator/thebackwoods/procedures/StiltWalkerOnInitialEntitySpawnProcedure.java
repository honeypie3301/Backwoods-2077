package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class StiltWalkerOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livingEntity0 && _livingEntity0.getAttributes().hasAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY))
			_livingEntity0.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(0.4);
	}
}