package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;

import net.mcreator.thebackwoods.procedures.PetrifiedRottenSwordLivingEntityIsHitWithToolProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

public class PetrifiedRottenSwordItem extends SwordItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 2013;
		}

		@Override
		public float getSpeed() {
			return 2f;
		}

		@Override
		public float getAttackDamageBonus() {
			return 0;
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
		}

		@Override
		public int getEnchantmentValue() {
			return 8;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of(new ItemStack(TheBackwoodsModItems.PETRIFIED_BARK.get()));
		}
	};

	public PetrifiedRottenSwordItem() {
		super(TOOL_TIER, new Item.Properties().attributes(SwordItem.createAttributes(TOOL_TIER, 8f, -3f)).fireResistant());
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		PetrifiedRottenSwordLivingEntityIsHitWithToolProcedure.execute(entity);
		return retval;
	}
}