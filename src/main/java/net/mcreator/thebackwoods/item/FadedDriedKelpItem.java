package net.mcreator.thebackwoods.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;

public class FadedDriedKelpItem extends Item {
	public FadedDriedKelpItem() {
		super(new Item.Properties().food((new FoodProperties.Builder()).nutrition(0).saturationModifier(0.2f).build()));
	}
}