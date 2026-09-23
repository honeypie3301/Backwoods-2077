package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.RotEntity;

public class RotPlaybackConditionRiderKickProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return entity instanceof RotEntity _datEntL0 && _datEntL0.getEntityData().get(RotEntity.DATA_is_rider_charging) || entity instanceof RotEntity _datEntL1 && _datEntL1.getEntityData().get(RotEntity.DATA_is_rider_kick);
	}
}