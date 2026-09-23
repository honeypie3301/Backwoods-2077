package net.mcreator.thebackwoods.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;

public class FadedPotatoItem extends Item {
	public FadedPotatoItem() {
		super(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationModifier(0.4f).build()));
	}
}