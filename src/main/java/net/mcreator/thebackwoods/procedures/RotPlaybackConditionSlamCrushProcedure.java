package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.RotEntity;

public class RotPlaybackConditionSlamCrushProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return entity instanceof RotEntity _datEntL0 && _datEntL0.getEntityData().get(RotEntity.DATA_is_ground_crushing);
	}
}