package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

public class StabilizedPaleRemedyPlayerFinishesUsingItemProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putDouble("backwoods_time", (entity.getPersistentData().getDouble("backwoods_time") - 5000));
		if (entity instanceof Player _player) {
			ItemStack _setstack = new ItemStack(Items.BOWL).copy();
			_setstack.setCount(1);
			ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
		}
	}
}