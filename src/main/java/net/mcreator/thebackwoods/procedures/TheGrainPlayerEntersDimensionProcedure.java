package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

public class TheGrainPlayerEntersDimensionProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		AntiObsidianProcedure.execute(world, entity);
	}
}