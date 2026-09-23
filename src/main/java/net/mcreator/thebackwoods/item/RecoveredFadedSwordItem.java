package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;

import net.mcreator.thebackwoods.procedures.RecoveredFadedSwordToolInInventoryTickProcedure;
import net.mcreator.thebackwoods.procedures.FadedSwordLivingEntityIsHitWithToolProcedure;

public class RecoveredFadedSwordItem extends SwordItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 80;
		}

		@Override
		public float getSpeed() {
			return 1f;
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
			return 2;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of();
		}
	};

	public RecoveredFadedSwordItem() {
		super(TOOL_TIER, new Item.Properties().attributes(SwordItem.createAttributes(TOOL_TIER, 6f, -1.6f)));
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		FadedSwordLivingEntityIsHitWithToolProcedure.execute(entity);
		return retval;
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		RecoveredFadedSwordToolInInventoryTickProcedure.execute(world, entity, itemstack);
	}
}