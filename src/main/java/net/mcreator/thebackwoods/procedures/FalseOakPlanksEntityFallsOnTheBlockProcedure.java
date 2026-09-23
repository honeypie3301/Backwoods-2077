package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

public class FalseOakPlanksEntityFallsOnTheBlockProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity.fallDistance > 4) {
			world.destroyBlock(BlockPos.containing(x, y, z), false);
		}
	}
}