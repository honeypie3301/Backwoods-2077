package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;

import net.mcreator.thebackwoods.procedures.RecoveredFadedSwordToolInInventoryTickProcedure;

public class RecoveredFadedShovelItem extends ShovelItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 45;
		}

		@Override
		public float getSpeed() {
			return 3.7f;
		}

		@Override
		public float getAttackDamageBonus() {
			return 0;
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return BlockTags.INCORRECT_FOR_STONE_TOOL;
		}

		@Override
		public int getEnchantmentValue() {
			return 1;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of();
		}
	};

	public RecoveredFadedShovelItem() {
		super(TOOL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TOOL_TIER, 2.4f, -3f)));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		RecoveredFadedSwordToolInInventoryTickProcedure.execute(world, entity, itemstack);
	}
}