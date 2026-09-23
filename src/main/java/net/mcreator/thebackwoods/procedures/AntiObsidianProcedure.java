package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class AntiObsidianProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double slot = 0;
		double itemCount = 0;
		slot = 0;
		if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandlerIter) {
			for (int _idx = 0; _idx < _modHandlerIter.getSlots(); _idx++) {
				ItemStack itemstackiterator = _modHandlerIter.getStackInSlot(_idx).copy();
				itemCount = itemstackiterator.getCount();
				if (itemstackiterator.getItem() == Blocks.OBSIDIAN.asItem()) {
					if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
						ItemStack _setstack = new ItemStack(TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_WOOD.get()).copy();
						_setstack.setCount(itemstackiterator.getCount());
						_modHandler.setStackInSlot((int) slot, _setstack);
					}
				}
				slot = slot + 1;
			}
		}
	}
}