package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.VerdantEngineEntity;

public class VerdantEnginePlaybackConditionDeathProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return entity instanceof VerdantEngineEntity _datEntL0 && _datEntL0.getEntityData().get(VerdantEngineEntity.DATA_isDead);
	}
}