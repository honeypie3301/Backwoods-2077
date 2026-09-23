package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.SplinterEntity;

public class SplinterDisplayCondition3Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (((entity instanceof SplinterEntity _datEntI ? _datEntI.getEntityData().get(SplinterEntity.DATA_Age) : 0) >= 144000) == (entity instanceof SplinterEntity _datEntI ? _datEntI.getEntityData().get(SplinterEntity.DATA_Age) : 0) < 216000) {
			return true;
		}
		return false;
	}
}