package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

public class GetMemoryShardModeProcedure {
	public static double execute(ItemStack itemstack) {
		return itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("mode");
	}
}