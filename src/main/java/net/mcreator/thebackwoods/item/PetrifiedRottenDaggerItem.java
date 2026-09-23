package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.thebackwoods.procedures.PetrifiedRottenSwordLivingEntityIsHitWithToolProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModItems;
import net.mcreator.thebackwoods.TheBackwoodsMod;

public class PetrifiedRottenDaggerItem extends SwordItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 1013;
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

	public PetrifiedRottenDaggerItem() {
		super(TOOL_TIER,
				new Item.Properties().attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
						.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
						.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "petrified_rotten_dagger_0"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
								EquipmentSlotGroup.MAINHAND)
						.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "petrified_rotten_dagger_1"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
								EquipmentSlotGroup.OFFHAND)
						.build()).fireResistant());
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		PetrifiedRottenSwordLivingEntityIsHitWithToolProcedure.execute(entity);
		return retval;
	}
}