package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

public class LignumCaroSwordToolInInventoryTickProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if (itemstack.getDamageValue() > 0) {
			if (!world.isClientSide()) {
				if (world.getLevelData().getGameTime() % 60 == 0) {
					if ((entity instanceof Player _plr ? _plr.getFoodData().getFoodLevel() : 0) > 0) {
						itemstack.setDamageValue(itemstack.getDamageValue() - 1);
						if (entity instanceof Player _player)
							_player.getFoodData().setFoodLevel((int) ((entity instanceof Player _plr ? _plr.getFoodData().getFoodLevel() : 0) - 0.25));
					}
				}
			}
		}
	}
}