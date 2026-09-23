package net.mcreator.thebackwoods.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;

public class AshBerriesItem extends Item {
	public AshBerriesItem() {
		super(new Item.Properties().food((new FoodProperties.Builder()).nutrition(1).saturationModifier(0.3f).build()));
	}
}