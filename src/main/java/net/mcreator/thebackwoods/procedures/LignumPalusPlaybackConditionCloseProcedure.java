package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.LignumPalusEntity;

public class LignumPalusPlaybackConditionCloseProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return (entity instanceof LignumPalusEntity _datEntI ? _datEntI.getEntityData().get(LignumPalusEntity.DATA_mouth_state) : 0) == 3;
	}
}