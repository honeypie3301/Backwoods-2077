package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;

import net.mcreator.thebackwoods.procedures.RecoveredFadedHoeToolInInventoryTickProcedure;
import net.mcreator.thebackwoods.procedures.RecoveredFadedHoeRightclickedOnBlockProcedure;

public class RecoveredFadedHoeItem extends HoeItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 60;
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
			return 1;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of();
		}
	};

	public RecoveredFadedHoeItem() {
		super(TOOL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TOOL_TIER, 0f, 0f)));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		super.useOn(context);
		RecoveredFadedHoeRightclickedOnBlockProcedure.execute(context.getLevel(), context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ(), context.getPlayer(), context.getItemInHand());
		return InteractionResult.SUCCESS;
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		RecoveredFadedHoeToolInInventoryTickProcedure.execute(world, entity, itemstack);
	}
}