package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

public class RotIsEntityModelTransparentProcedure {
	public static boolean execute(Entity entity) {
		return execute(entity != null ? entity.level() : null, entity);
	}

	public static boolean execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return false;

		boolean isPhaseShifting = entity.getPersistentData().getBoolean("rot_phase_shifting");
		if (!isPhaseShifting) {
			return false;
		}

		double mastery = entity.getPersistentData().contains("rot_phase_mastery") ? entity.getPersistentData().getDouble("rot_phase_mastery") : 1.0;
		if (mastery < 0.75) {
			// Imperfect Phasing: Visual Flickering / Glitching effect
			long time = entity.level() != null ? entity.level().dayTime() : (world != null ? world.dayTime() : System.currentTimeMillis() / 50);
			int flickerInterval = mastery < 0.35 ? 4 : 12; // Rapid flicker for novice, slower glitch for adept
			return (time % flickerInterval) < (flickerInterval / 2);
		}

		// Mastered Phase Shift: Flawless, smooth ghost transparency
		return true;
	}
 // 1.21.1
}
