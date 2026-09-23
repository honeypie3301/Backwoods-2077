package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

public class ScandereLignumPlayerStartsToDestroyProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (Math.random() < 0.01) {
			if (world instanceof ServerLevel _level) {
				Entity entityToSpawn = TheBackwoodsModEntities.LIGNUM_VERMIS.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
				if (entityToSpawn != null) {
					entityToSpawn.setDeltaMovement(0, 0, 0);
				}
			}
		}
	}
}