package net.mcreator.thebackwoods.item;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;

import net.mcreator.thebackwoods.procedures.HeartwoodHeartwoodRottenSwordLivingEntityIsHitWithToolProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

import java.util.List;

public class HeartwoodRottenSwordItem extends SwordItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 400;
		}

		@Override
		public float getSpeed() {
			return 4f;
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
			return 6;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of(new ItemStack(TheBackwoodsModItems.HEARTWOOD_SHARD.get()));
		}
	};

	public HeartwoodRottenSwordItem() {
		super(TOOL_TIER, new Item.Properties().attributes(SwordItem.createAttributes(TOOL_TIER, 7f, -2.9f)));
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		HeartwoodHeartwoodRottenSwordLivingEntityIsHitWithToolProcedure.execute(entity);
		return retval;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("item.the_backwoods.heartwood_rotten_sword.description_0"));
	}
}