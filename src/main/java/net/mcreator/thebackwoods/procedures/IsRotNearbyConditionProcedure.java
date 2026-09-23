package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.RotEntity;

public class IsRotNearbyConditionProcedure {
	public static boolean execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return false;
		return !world.getEntitiesOfClass(RotEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3((entity.getX()), (entity.getY()), (entity.getZ()))).inflate(200 / 2d), e -> true).isEmpty();
	}
}