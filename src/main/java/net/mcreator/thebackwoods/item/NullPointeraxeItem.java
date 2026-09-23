package net.mcreator.thebackwoods.item;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;
import net.mcreator.thebackwoods.TheBackwoodsMod;

import java.util.List;

public class NullPointeraxeItem extends AxeItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 2079;
		}

		@Override
		public float getSpeed() {
			return 10f;
		}

		@Override
		public float getAttackDamageBonus() {
			return 0;
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
		}

		@Override
		public int getEnchantmentValue() {
			return 7;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of(new ItemStack(TheBackwoodsModBlocks.FADED_BLOCK.get()), new ItemStack(TheBackwoodsModItems.PETRIFIED_BARK.get()), new ItemStack(TheBackwoodsModItems.SHARPENED_SPLINTER_SHARD.get()),
					new ItemStack(TheBackwoodsModBlocks.PLAQUE_HEART.get()));
		}
	};

	public NullPointeraxeItem() {
		super(TOOL_TIER,
				new Item.Properties()
						.attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 2012, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
								.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, 2.7, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
								.add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "null_pointeraxe_0"), 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
								.add(Attributes.OXYGEN_BONUS, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "null_pointeraxe_1"), 10, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
								.add(Attributes.MAX_HEALTH, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "null_pointeraxe_2"), 500, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
								.add(Attributes.STEP_HEIGHT, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "null_pointeraxe_3"), 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
								.add(Attributes.WATER_MOVEMENT_EFFICIENCY, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "null_pointeraxe_4"), 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
								.add(Attributes.SNEAKING_SPEED, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "null_pointeraxe_5"), 0.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY).build())
						.rarity(Rarity.RARE).fireResistant());
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("item.the_backwoods.null_pointeraxe.description_0"));
	}
}