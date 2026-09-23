package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.PetrifiedLignumTrilobitaEntity;

public class PetrifiedTrilobitaAvoidConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return (entity instanceof PetrifiedLignumTrilobitaEntity _datEntI ? _datEntI.getEntityData().get(PetrifiedLignumTrilobitaEntity.DATA_eatTimer) : 0) == 0
				&& !(entity instanceof PetrifiedLignumTrilobitaEntity _datEntL1 && _datEntL1.getEntityData().get(PetrifiedLignumTrilobitaEntity.DATA_isHiding));
	}
}