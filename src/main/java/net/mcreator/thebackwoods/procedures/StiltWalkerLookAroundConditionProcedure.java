package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;

public class StiltWalkerLookAroundConditionProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		if (!(!world.getEntitiesOfClass(Player.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(100 / 2d), e -> true).isEmpty())) {
			return true;
		}
		return false;
	}
}