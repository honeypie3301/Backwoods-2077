package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.LogSplinterEntity;

public class LogSplinterDisplayConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity instanceof LogSplinterEntity _datEntI ? _datEntI.getEntityData().get(LogSplinterEntity.DATA_Age) : 0) < 60000) {
			return true;
		}
		return false;
	}
}