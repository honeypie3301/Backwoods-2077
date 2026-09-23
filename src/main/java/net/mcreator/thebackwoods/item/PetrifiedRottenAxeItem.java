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

import net.mcreator.thebackwoods.procedures.PetrifiedRottenAxeLivingEntityIsHitWithToolProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

public class PetrifiedRottenAxeItem extends AxeItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 2077;
		}

		@Override
		public float getSpeed() {
			return 8.5f;
		}

		@Override
		public float getAttackDamageBonus() {
			return 0;
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
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

	public PetrifiedRottenAxeItem() {
		super(TOOL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TOOL_TIER, 8.5f, -3.05f)).fireResistant());
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		PetrifiedRottenAxeLivingEntityIsHitWithToolProcedure.execute(entity);
		return retval;
	}
}