package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.LogSplinterEntity;

public class LogSplinterDisplayCondition4Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity instanceof LogSplinterEntity _datEntI ? _datEntI.getEntityData().get(LogSplinterEntity.DATA_Age) : 0) >= 240000
				&& (entity instanceof LogSplinterEntity _datEntI ? _datEntI.getEntityData().get(LogSplinterEntity.DATA_Age) : 0) < 300000) {
			return true;
		}
		return false;
	}
}