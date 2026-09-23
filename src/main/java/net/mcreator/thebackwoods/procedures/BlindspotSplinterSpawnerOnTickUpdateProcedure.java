package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Difficulty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;
import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;

public class BlindspotSplinterSpawnerOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double range_difficulty = 0;
		double spawnAmount = 0;
		if (world.getDifficulty() == Difficulty.PEACEFUL) {
			range_difficulty = 12;
			spawnAmount = 1;
		} else if (world.getDifficulty() == Difficulty.EASY) {
			range_difficulty = 16;
			spawnAmount = 1;
		} else if (world.getDifficulty() == Difficulty.NORMAL) {
			spawnAmount = 2;
			range_difficulty = 32;
		} else if (world.getDifficulty() == Difficulty.HARD) {
			spawnAmount = 2;
			range_difficulty = 48;
		} else {
			spawnAmount = 4;
			range_difficulty = 52;
		}
		if (!(!world.getEntitiesOfClass(Player.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(range_difficulty / 2d), e -> true).isEmpty())) {
			return;
		}
		if (!(!world.getEntitiesOfClass(BlindspotSplinterEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(6 / 2d), e -> true).isEmpty())) {
			for (int index0 = 0; index0 < (int) spawnAmount; index0++) {
				if (world instanceof ServerLevel _level) {
					Entity entityToSpawn = TheBackwoodsModEntities.BLINDSPOT_SPLINTER.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
					if (entityToSpawn != null) {
						entityToSpawn.setDeltaMovement(0, 0, 0);
					}
				}
			}
		}
	}
}