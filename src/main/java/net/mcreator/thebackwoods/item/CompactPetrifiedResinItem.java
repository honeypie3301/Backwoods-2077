package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;

import net.mcreator.thebackwoods.procedures.CompactPetrifiedResinRightclickedProcedure;

public class CompactPetrifiedResinItem extends Item {
	public CompactPetrifiedResinItem() {
		super(new Item.Properties().stacksTo(16));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		CompactPetrifiedResinRightclickedProcedure.execute(entity);
		return ar;
	}
}