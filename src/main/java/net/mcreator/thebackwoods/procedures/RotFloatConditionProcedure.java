package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;

public class RotFloatConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;

		if (entity instanceof Mob mob) {
			// If charging or executing the superheat water evaporation, suppress floating to settle onto the seabed/floor
			if (entity.getPersistentData().getDouble("rot_superheat_charging") > 0 || entity.getPersistentData().getDouble("rot_superheat_active") > 0) {
				return false;
			}

			LivingEntity target = mob.getTarget();

			// KEY FIX: If the Rot is in water and has a live target, NEVER float.
			// FloatGoal (setJumping=true) directly fights the 3D swim velocity code,
			// causing the Rot to stall in place instead of swimming toward the target.
			// The swim code in RotAI handles all movement when submerged.
			if ((entity instanceof LivingEntity rotLiv && (rotLiv.isInWater() || rotLiv.isInLava())) && target != null && target.isAlive()) {
				return false;
			}

			if (target != null && target.isAlive()) {
				double rotY = entity.getY();
				double targetY = target.getY();

				// If the target is positioned below the Rot (underwater, on the sea floor, or below on shore),
				// suppress floating so the Rot can dive down and fight effectively.
				if (targetY < rotY - 0.5) {
					return false;
				}
				return true;
			}
		}

		// Default: float normally when wandering or out of combat
		return true;
	} // 1.21.1
}
