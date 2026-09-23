package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;

public class BlindspotSplinterNaturalEntitySpawningConditionProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		Entity targetPlayer = null;
		double radius = 0;
		if (world.getBiome(BlockPos.containing(x, y, z)).is(ResourceLocation.parse("the_backwoods:the_dead_grain"))) {
			if (y < 319 && y > 161) {
				radius = 120;
				targetPlayer = (world instanceof net.minecraft.world.level.Level _lvl) ? _lvl.players().stream().filter(_p -> _p.distanceToSqr(x, y, z) < 4096.0).findFirst().orElse(null) : null;
				if (targetPlayer != null) {
					if (targetPlayer.getPersistentData().getDouble("sub_strata_time") > 10000) {
						radius = 16;
					} else if (targetPlayer.getPersistentData().getDouble("sub_strata_time") > 8500) {
						radius = 28;
					} else if (targetPlayer.getPersistentData().getDouble("sub_strata_time") > 5500) {
						radius = 42;
					} else if (targetPlayer.getPersistentData().getDouble("sub_strata_time") > 3750) {
						radius = 58;
					} else if (targetPlayer.getPersistentData().getDouble("sub_strata_time") > 1000) {
						radius = 77;
					}
				}
				if (!(!world.getEntitiesOfClass(BlindspotSplinterEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(radius / 2d), e -> true).isEmpty()) && !world.isEmptyBlock(BlockPos.containing(x, y - 1, z))) {
					return true;
				}
			}
		} else if (world.getBiome(BlockPos.containing(x, y, z)).is(ResourceLocation.parse("the_backwoods:labyrinthine_grids"))) {
			if (!(!world.getEntitiesOfClass(BlindspotSplinterEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(192 / 2d), e -> true).isEmpty()) && world.getBlockState(BlockPos.containing(x, y - 1, z)).canOcclude()) {
				return true;
			}
		} else if (world.getBiome(BlockPos.containing(x, y, z)).is(ResourceLocation.parse("the_backwoods:pillar_thicket"))) {
			if (!(!world.getEntitiesOfClass(BlindspotSplinterEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(192 / 2d), e -> true).isEmpty()) && world.getBlockState(BlockPos.containing(x, y - 1, z)).canOcclude()) {
				return true;
			}
		} else if (world.getBiome(BlockPos.containing(x, y, z)).is(ResourceLocation.parse("the_backwoods:uniform_grain"))) {
			if (!(!world.getEntitiesOfClass(BlindspotSplinterEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(1000 / 2d), e -> true).isEmpty()) && world.getBlockState(BlockPos.containing(x, y - 1, z)).canOcclude()) {
				return true;
			}
		}
		return false;
	}
}