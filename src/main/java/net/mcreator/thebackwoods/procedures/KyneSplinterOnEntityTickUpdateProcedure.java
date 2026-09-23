package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

public class KyneSplinterOnEntityTickUpdateProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity.getPersistentData().getDouble("avenge_cooldown") > 0) {
			entity.getPersistentData().putDouble("avenge_cooldown", (entity.getPersistentData().getDouble("avenge_cooldown") - 1));
		}
	}
}