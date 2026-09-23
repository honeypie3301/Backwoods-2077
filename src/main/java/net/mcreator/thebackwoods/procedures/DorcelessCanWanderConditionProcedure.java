package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

public class DorcelessCanWanderConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) != null || entity.getPersistentData().getDouble("stare_ticks") > 0) {
			return false;
		}
		return true;
	}
}