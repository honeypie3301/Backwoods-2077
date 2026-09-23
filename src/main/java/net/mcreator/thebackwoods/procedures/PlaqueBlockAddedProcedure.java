package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;

public class PlaqueBlockAddedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		PlaqueSpreadOnTickUpdateProcedure.execute(world, x, y, z);
	}
}