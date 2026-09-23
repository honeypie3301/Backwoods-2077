package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.FractusPrimeEntity;

public class FractusPrimePlaybackConditionPrimeActivateProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return entity instanceof FractusPrimeEntity _datEntL0 && _datEntL0.getEntityData().get(FractusPrimeEntity.DATA_is_laser_activating);
	}
}