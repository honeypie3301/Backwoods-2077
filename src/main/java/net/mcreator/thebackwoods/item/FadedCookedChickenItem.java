package net.mcreator.thebackwoods.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;

public class FadedCookedChickenItem extends Item {
	public FadedCookedChickenItem() {
		super(new Item.Properties().food((new FoodProperties.Builder()).nutrition(6).saturationModifier(0.6f).build()));
	}
}