package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;

import net.mcreator.thebackwoods.procedures.FadedCookedBeefPlayerFinishesUsingItemProcedure;

public class FadedCookedPorkchopItem extends Item {
	public FadedCookedPorkchopItem() {
		super(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationModifier(0.8f).build()));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = super.finishUsingItem(itemstack, world, entity);
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		FadedCookedBeefPlayerFinishesUsingItemProcedure.execute(entity);
		return retval;
	}
}