package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.LignumGigasEntity;

public class LignumGigasPlaybackConditionReverseProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return entity instanceof LignumGigasEntity _datEntL0 && _datEntL0.getEntityData().get(LignumGigasEntity.DATA_play_lower_anim);
	}
}