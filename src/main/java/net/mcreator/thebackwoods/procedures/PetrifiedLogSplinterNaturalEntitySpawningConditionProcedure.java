package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.entity.PetrifiedLogSplinterEntity;

public class PetrifiedLogSplinterNaturalEntitySpawningConditionProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		if (!(!world.getEntitiesOfClass(PetrifiedLogSplinterEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(96 / 2d), e -> true).isEmpty()) && world.getBlockState(BlockPos.containing(x, y - 1, z)).canOcclude()) {
			return true;
		}
		return false;
	}
}