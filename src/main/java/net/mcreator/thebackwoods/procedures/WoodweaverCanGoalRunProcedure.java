package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

public class WoodweaverCanGoalRunProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return !entity.getPersistentData().getBoolean("is_leaping") && !entity.getPersistentData().getBoolean("is_hypnotizing") && !entity.getPersistentData().getBoolean("is_attacking") && !entity.getPersistentData().getBoolean("is_landing_anim")
				&& !entity.getPersistentData().getBoolean("is_preparing_leap") && !entity.getPersistentData().getBoolean("is_closing_hypno") && entity.getPersistentData().getDouble("stare_ticks") <= 0
				&& entity.getPersistentData().getDouble("hypno_phase") == 0 && entity.getPersistentData().getDouble("hypno_anim_timer") <= 0 && entity.getPersistentData().getDouble("hypno_attack_lockout") <= 0;
	}
}