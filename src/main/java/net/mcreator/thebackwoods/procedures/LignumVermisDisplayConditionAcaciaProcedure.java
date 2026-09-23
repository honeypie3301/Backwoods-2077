package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

public class LignumVermisDisplayConditionAcaciaProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity.getPersistentData().getString("camoType")).equals("acacia_planks")) {
			return true;
		}
		return false;
	}
}