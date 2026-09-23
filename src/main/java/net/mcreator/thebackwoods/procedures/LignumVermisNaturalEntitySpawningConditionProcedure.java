package net.mcreator.thebackwoods.procedures;

public class LignumVermisNaturalEntitySpawningConditionProcedure {
	public static boolean execute(double y) {
		if (y < 52 && y > 44) {
			return true;
		}
		return false;
	}
}