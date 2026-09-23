package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.WoodweaverEntity;

public class WoodweaverPlaybackConditionHypno15Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return entity instanceof WoodweaverEntity _datEntL0 && _datEntL0.getEntityData().get(WoodweaverEntity.DATA_isClosing1_5s);
	}
}