package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class RottenPickaxeItem extends PickaxeItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 180;
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

	public RottenPickaxeItem() {
		super(TOOL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TOOL_TIER, 3f, -3.1f)));
	}
}