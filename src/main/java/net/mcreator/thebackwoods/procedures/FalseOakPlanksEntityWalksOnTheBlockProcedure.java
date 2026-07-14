package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.TheBackwoodsMod;

public class FalseOakPlanksEntityWalksOnTheBlockProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double entityVolume = 0;
		entityVolume = entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight();
		if (entity != null && entityVolume > 0.5) {
			TheBackwoodsMod.queueServerWork(Mth.nextInt(RandomSource.create(), 20, 60), () -> {
				if (!world.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(x + 0.1, y + 1.0, z + 0.1, x + 0.9, y + 1.5, z + 0.9), e -> (e.getBbWidth() * e.getBbWidth() * e.getBbHeight()) > 0.5)
						.isEmpty()) {
					world.destroyBlock(BlockPos.containing(x, y, z), false);
				}
			});
		}
	}
}