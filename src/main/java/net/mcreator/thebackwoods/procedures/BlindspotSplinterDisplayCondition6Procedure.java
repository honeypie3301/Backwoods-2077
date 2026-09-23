package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;

public class BlindspotSplinterDisplayCondition6Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity instanceof BlindspotSplinterEntity _datEntI ? _datEntI.getEntityData().get(BlindspotSplinterEntity.DATA_Age) : 0) >= 432000
				&& (entity instanceof BlindspotSplinterEntity _datEntI ? _datEntI.getEntityData().get(BlindspotSplinterEntity.DATA_Age) : 0) < 504000) {
			return true;
		}
		return false;
	}
}