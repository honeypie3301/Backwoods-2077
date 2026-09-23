package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.entity.WoodweaverEntity;

public class WoodweaverNaturalEntitySpawningConditionProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		if (Math.random() <= 0.2) {
			if (!(!world.getEntitiesOfClass(WoodweaverEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(512 / 2d), e -> true).isEmpty()) && world.getBlockState(BlockPos.containing(x, y - 1, z)).canOcclude()
					&& !(!world.getEntitiesOfClass(Player.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(64 / 2d), e -> true).isEmpty())) {
				return true;
			}
		}
		return false;
	}
}