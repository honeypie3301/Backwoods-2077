package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.entity.FractusPrimeEntity;

public class FractusPrimeNaturalEntitySpawningConditionProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		if (y < 19 && y > -63) {
			if (Math.random() < 0.005) {
				if (!(!world.getEntitiesOfClass(FractusPrimeEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(128 / 2d), e -> true).isEmpty()) && world.isEmptyBlock(BlockPos.containing(x, y, z))) {
					return true;
				}
			}
		}
		return false;
	}
}