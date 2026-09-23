package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

public class TetherlessPearlRangedItemShootsProjectileProcedure {
	public static void execute(Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if (entity instanceof Player _player)
			_player.getCooldowns().addCooldown(itemstack.getItem(), 40);
		if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
			itemstack.shrink(1);
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheBackwoodsModItems.RESONANT_TETHERLESS_PEARL.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheBackwoodsModItems.TETHERLESS_PEARL.get()) {
			if (entity instanceof LivingEntity _entity)
				_entity.swing(InteractionHand.MAIN_HAND, true);
		} else {
			if (entity instanceof LivingEntity _entity)
				_entity.swing(InteractionHand.OFF_HAND, true);
		}
	}
}