package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;

import net.mcreator.thebackwoods.procedures.RottenAxeLivingEntityIsHitWithToolProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class RottenAxeItem extends AxeItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 200;
		}

		@Override
		public float getSpeed() {
			return 1.9f;
		}

		@Override
		public float getAttackDamageBonus() {
			return 0;
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return BlockTags.INCORRECT_FOR_IRON_TOOL;
		}

		@Override
		public int getEnchantmentValue() {
			return 10;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of(new ItemStack(TheBackwoodsModBlocks.ROTTEN_OAK_PLANKS.get()));
		}
	};

	public RottenAxeItem() {
		super(TOOL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TOOL_TIER, 6f, -3.3f)));
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		RottenAxeLivingEntityIsHitWithToolProcedure.execute(entity);
		return retval;
	}
}