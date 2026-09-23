package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionResult;

import net.mcreator.thebackwoods.procedures.WindcapMushroomRightclickedOnBlockProcedure;
import net.mcreator.thebackwoods.procedures.WindcapMushroomPlayerFinishesUsingItemProcedure;

public class WindcapMushroomItem extends Item {
	public WindcapMushroomItem() {
		super(new Item.Properties().food((new FoodProperties.Builder()).nutrition(1).saturationModifier(0.1f).build()));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = super.finishUsingItem(itemstack, world, entity);
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		WindcapMushroomPlayerFinishesUsingItemProcedure.execute(entity);
		return retval;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		super.useOn(context);
		WindcapMushroomRightclickedOnBlockProcedure.execute(context.getLevel(), context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ(), context.getClickedFace(), context.getPlayer(), context.getItemInHand());
		return InteractionResult.SUCCESS;
	}
}