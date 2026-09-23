package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

public class KyneWanderConditionProcedure {
	public static boolean execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return false;
		if (!world.getEntitiesOfClass(Player.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3((entity.getX()), (entity.getY()), (entity.getZ()))).inflate(200 / 2d), e -> true).isEmpty()) {
			return false;
		}
		return true;
	}
}