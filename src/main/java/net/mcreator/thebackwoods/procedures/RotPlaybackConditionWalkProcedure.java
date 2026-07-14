package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.RotEntity;

public class RotPlaybackConditionWalkProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return !(entity instanceof RotEntity _datEntL0 && _datEntL0.getEntityData().get(RotEntity.DATA_is_sonic_boom_large)) && !(entity instanceof RotEntity _datEntL1 && _datEntL1.getEntityData().get(RotEntity.DATA_is_airborne_state))
				&& !(entity instanceof RotEntity _datEntL2 && _datEntL2.getEntityData().get(RotEntity.DATA_is_overhead_preparing)) && !(entity instanceof RotEntity _datEntL3 && _datEntL3.getEntityData().get(RotEntity.DATA_is_ground_crushing))
				&& !(entity instanceof RotEntity _datEntL4 && _datEntL4.getEntityData().get(RotEntity.DATA_is_overhead)) && !(entity instanceof RotEntity _datEntL5 && _datEntL5.getEntityData().get(RotEntity.DATA_is_slam_charge))
				&& !(entity instanceof RotEntity _datEntL6 && _datEntL6.getEntityData().get(RotEntity.DATA_is_rider_charging)) && !(entity instanceof RotEntity _datEntL7 && _datEntL7.getEntityData().get(RotEntity.DATA_is_falling_heavy))
				&& !(entity instanceof RotEntity _datEntL8 && _datEntL8.getEntityData().get(RotEntity.DATA_is_uppercutting)) && !(entity instanceof RotEntity _datEntL9 && _datEntL9.getEntityData().get(RotEntity.DATA_is_rider_kick));
	}
}