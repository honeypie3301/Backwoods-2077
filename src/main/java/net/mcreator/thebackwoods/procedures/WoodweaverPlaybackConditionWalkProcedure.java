package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.WoodweaverEntity;

public class WoodweaverPlaybackConditionWalkProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (!(entity instanceof WoodweaverEntity _datEntL0 && _datEntL0.getEntityData().get(WoodweaverEntity.DATA_isRotatingLeft))
				&& !(entity instanceof WoodweaverEntity _datEntL1 && _datEntL1.getEntityData().get(WoodweaverEntity.DATA_isRotatingRight))
				&& !(entity instanceof WoodweaverEntity _datEntL2 && _datEntL2.getEntityData().get(WoodweaverEntity.DATA_isLeaping)) && !(entity instanceof WoodweaverEntity _datEntL3 && _datEntL3.getEntityData().get(WoodweaverEntity.DATA_isLanding))
				&& !(entity instanceof WoodweaverEntity _datEntL4 && _datEntL4.getEntityData().get(WoodweaverEntity.DATA_isRetreating))) {
			return true;
		}
		return false;
	}
}