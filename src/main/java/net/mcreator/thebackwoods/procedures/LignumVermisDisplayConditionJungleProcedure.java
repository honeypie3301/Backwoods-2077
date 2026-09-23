package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

public class LignumVermisDisplayConditionJungleProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity.getPersistentData().getString("camoType")).equals("jungle_planks")) {
			return true;
		}
		return false;
	}
}