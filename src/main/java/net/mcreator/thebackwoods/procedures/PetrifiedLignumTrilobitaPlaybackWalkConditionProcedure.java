package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.PetrifiedLignumTrilobitaEntity;

public class PetrifiedLignumTrilobitaPlaybackWalkConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return !(entity instanceof PetrifiedLignumTrilobitaEntity _datEntL0 && _datEntL0.getEntityData().get(PetrifiedLignumTrilobitaEntity.DATA_isHiding));
	}
}