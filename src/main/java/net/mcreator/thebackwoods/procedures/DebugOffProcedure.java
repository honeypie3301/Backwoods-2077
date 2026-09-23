package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;

public class DebugOffProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putDouble("debug_mode", 0);
	}
}