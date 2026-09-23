package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.LignumTrilobitaEntity;

public class LignumTrilobitaPlaybackConditionLowHPProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return entity instanceof LignumTrilobitaEntity _datEntL0 && _datEntL0.getEntityData().get(LignumTrilobitaEntity.DATA_isHiding);
	}
}