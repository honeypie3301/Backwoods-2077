package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

public class LignumVermisDisplayConditionJungleLogProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity.getPersistentData().getString("camoType")).equals("jungle_log")) {
			return true;
		}
		return false;
	}
}