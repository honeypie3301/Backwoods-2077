package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class SinkingAshOnBlockRightclickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheBackwoodsModItems.PETRIFIED_RESIN.get()) {
			world.setBlock(BlockPos.containing(x, y, z), TheBackwoodsModBlocks.FADED_BLOCK.get().defaultBlockState(), 3);
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheBackwoodsModItems.COMPACT_PETRIFIED_RESIN.get()) {
			world.setBlock(BlockPos.containing(x, y, z), TheBackwoodsModBlocks.NULLSTONE.get().defaultBlockState(), 3);
		}
		if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
			(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).shrink(1);
		}
	}
}