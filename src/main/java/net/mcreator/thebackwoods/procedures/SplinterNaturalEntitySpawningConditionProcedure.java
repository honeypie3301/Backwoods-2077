package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.entity.SplinterEntity;

import java.util.Comparator;

public class SplinterNaturalEntitySpawningConditionProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		Entity targetPlayer = null;
		double radius = 0;
		radius = 128;
		targetPlayer = findEntityInWorldRange(world, Player.class, x, y, z, 64);
		if (targetPlayer != null) {
			if (targetPlayer.getPersistentData().getDouble("backwoods_time") > 20000) {
				radius = 32;
			} else if (targetPlayer.getPersistentData().getDouble("backwoods_time") > 17000) {
				radius = 48;
			} else if (targetPlayer.getPersistentData().getDouble("backwoods_time") > 11000) {
				radius = 72;
			} else if (targetPlayer.getPersistentData().getDouble("backwoods_time") > 7500) {
				radius = 96;
			} else if (targetPlayer.getPersistentData().getDouble("backwoods_time") > 4000) {
				radius = 112;
			}
		}
		if (!(!world.getEntitiesOfClass(SplinterEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(radius / 2d), e -> true).isEmpty()) && world.getBlockState(BlockPos.containing(x, y - 1, z)).canOcclude()) {
			return true;
		}
		return false;
	}

	private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range) {
		return (Entity) world.getEntitiesOfClass(clazz, AABB.ofSize(new Vec3(x, y, z), range, range, range), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z))).findFirst().orElse(null);
	}
}