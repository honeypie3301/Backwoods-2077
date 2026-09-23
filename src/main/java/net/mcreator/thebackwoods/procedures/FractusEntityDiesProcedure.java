package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

public class FractusEntityDiesProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putDouble("fractus_laser_state", 0);
		entity.getPersistentData().putDouble("fractus_charge", 0);
		entity.getPersistentData().putDouble("fractus_fire", 0);
		entity.getPersistentData().putDouble("fractus_laser_burst_ticks", 0);
		entity.getPersistentData().putBoolean("is_angered_burst", false);
		entity.getPersistentData().putBoolean("is_laser_burst", false);
		entity.getPersistentData().putBoolean("is_burst_charging", false);
		entity.getPersistentData().putBoolean("is_burst_firing", false);
	}
}