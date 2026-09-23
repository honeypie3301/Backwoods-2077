package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

public class ScandereLignumBlockDestroyedByExplosionProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (Math.random() < 0.1) {
			if (world instanceof ServerLevel _level) {
				Entity entityToSpawn = TheBackwoodsModEntities.LIGNUM_VERMIS.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
				if (entityToSpawn != null) {
					entityToSpawn.setDeltaMovement(0, 0, 0);
				}
			}
		}
		if (world instanceof ServerLevel _level) {
			ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(Blocks.DIRT));
			entityToSpawn.setPickUpDelay(10);
			_level.addFreshEntity(entityToSpawn);
		}
	}
}