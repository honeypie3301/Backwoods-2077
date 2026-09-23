package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.procedures.FractusCoreNuggetItemInInventoryTickProcedure;

public class FractusCoreNuggetItem extends Item {
	public FractusCoreNuggetItem() {
		super(new Item.Properties().rarity(Rarity.RARE));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		FractusCoreNuggetItemInInventoryTickProcedure.execute(world, entity);
	}
}