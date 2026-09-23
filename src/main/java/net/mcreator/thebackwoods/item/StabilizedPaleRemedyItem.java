package net.mcreator.thebackwoods.item;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;

import net.mcreator.thebackwoods.procedures.StabilizedPaleRemedyPlayerFinishesUsingItemProcedure;

import java.util.List;

public class StabilizedPaleRemedyItem extends Item {
	public StabilizedPaleRemedyItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).food((new FoodProperties.Builder()).nutrition(4).saturationModifier(4f).alwaysEdible().build()));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("item.the_backwoods.stabilized_pale_remedy.description_0"));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = super.finishUsingItem(itemstack, world, entity);
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		StabilizedPaleRemedyPlayerFinishesUsingItemProcedure.execute(entity);
		return retval;
	}
}