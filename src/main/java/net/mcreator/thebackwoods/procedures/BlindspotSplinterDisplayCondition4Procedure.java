package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;

public class BlindspotSplinterDisplayCondition4Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity instanceof BlindspotSplinterEntity _datEntI ? _datEntI.getEntityData().get(BlindspotSplinterEntity.DATA_Age) : 0) >= 216000
				&& (entity instanceof BlindspotSplinterEntity _datEntI ? _datEntI.getEntityData().get(BlindspotSplinterEntity.DATA_Age) : 0) < 360000) {
			return true;
		}
		return false;
	}
}