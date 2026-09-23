package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

public class RotWanderConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (entity.getPersistentData().getDouble("sentinel_landing_ticks") > 0 || entity.getPersistentData().getDouble("sentinel_rider_hold_ticks") > 0 || entity.getPersistentData().getDouble("sentinel_judgment_ticks") > 0
				|| entity.getPersistentData().getDouble("sentinel_die_kick_phase") > 0 || entity.getPersistentData().getDouble("controlled_adaptation_ticks") > 0) {
			return false;
		}
		if ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) != null) {
			if ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).isAlive()) {
				return false;
			}
		}
		return true;
	}
}