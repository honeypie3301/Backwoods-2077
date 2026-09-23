package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

public class WoodweaverOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putString("last_debug_state", "NORMAL");
		entity.getPersistentData().putDouble("leap_cooldown", 0);
		entity.getPersistentData().putDouble("hypno_cooldown", 0);
		entity.getPersistentData().putDouble("attack_timer", 0);
	}
}