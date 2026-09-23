package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.FractusEntity;

public class FractusPlaybackConditionLaserDeactivateProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return entity instanceof FractusEntity _datEntL0 && _datEntL0.getEntityData().get(FractusEntity.DATA_is_laser_deactivating);
	}
}