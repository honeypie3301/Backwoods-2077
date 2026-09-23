package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

public class FadedToolInInventoryTickProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if (!hasEntityInInventory(entity, new ItemStack(TheBackwoodsModItems.MEMORY_SHARD.get())) && world.getLevelData().getGameTime() % 100 == 0 && !world.isClientSide()) {
			if (Mth.nextInt(RandomSource.create(), 1, 3) == 1) {
				if (world instanceof ServerLevel _level) {
					itemstack.hurtAndBreak(1, _level, null, _stkprov -> {
					});
				}
			}
		}
	}

	private static boolean hasEntityInInventory(Entity entity, ItemStack itemstack) {
		if (entity instanceof Player player)
			return player.getInventory().contains(stack -> !stack.isEmpty() && ItemStack.isSameItem(stack, itemstack));
		return false;
	}
}