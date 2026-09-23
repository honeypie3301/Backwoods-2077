package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class RecoveredFadedHoeRightclickedOnBlockProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if ((world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == TheBackwoodsModBlocks.TALL_SEDGEBRUSH.get() || (world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == TheBackwoodsModBlocks.SEDGEBRUSH.get()) {
			world.destroyBlock(BlockPos.containing(x, y, z), false);
			if (world instanceof ServerLevel _level) {
				itemstack.hurtAndBreak(1, _level, null, _stkprov -> {
				});
			}
			if (entity.getPersistentData().getDouble("hoe_resin_cd") <= 0) {
				if (Math.random() < 0.06) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(TheBackwoodsModItems.PETRIFIED_RESIN.get()));
						entityToSpawn.setPickUpDelay(10);
						_level.addFreshEntity(entityToSpawn);
					}
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.DUST_PLUME, (x + 0.5), (y + 0.6), (z + 0.5), 15, 0.2, 0.2, 0.2, 0.1);
					if (world instanceof ServerLevel _level) {
						itemstack.hurtAndBreak(10, _level, null, _stkprov -> {
						});
					}
					entity.getPersistentData().putDouble("hoe_resin_cd", 100);
				}
			}
		}
	}
}