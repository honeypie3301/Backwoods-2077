package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.entity.LignumEchinusEntity;

public class LignumEchinusNaturalEntitySpawningConditionProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		if (y >= 58) {
			if (!(!world.getEntitiesOfClass(LignumEchinusEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(72 / 2d), e -> true).isEmpty()) && !world.isEmptyBlock(BlockPos.containing(x, y - 1, z))) {
				return true;
			}
		}
		return false;
	}
}