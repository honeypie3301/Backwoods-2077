package net.mcreator.thebackwoods.item;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;

import net.mcreator.thebackwoods.procedures.FadedEnchantedGoldenApplePlayerFinishesUsingItemProcedure;

public class FadedEnchantedGoldenAppleItem extends Item {
	public FadedEnchantedGoldenAppleItem() {
		super(new Item.Properties().rarity(Rarity.RARE).food((new FoodProperties.Builder()).nutrition(2).saturationModifier(4.8f).alwaysEdible().build()));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = super.finishUsingItem(itemstack, world, entity);
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		FadedEnchantedGoldenApplePlayerFinishesUsingItemProcedure.execute(world, entity);
		return retval;
	}
}