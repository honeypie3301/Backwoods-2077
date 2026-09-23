package net.mcreator.thebackwoods.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class DistortedMemoryFragmentItem extends Item {
	public DistortedMemoryFragmentItem() {
		super(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
	}
}