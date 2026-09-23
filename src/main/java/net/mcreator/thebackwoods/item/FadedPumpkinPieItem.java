package net.mcreator.thebackwoods.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;

public class FadedPumpkinPieItem extends Item {
	public FadedPumpkinPieItem() {
		super(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationModifier(0.3f).build()));
	}
}