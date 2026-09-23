package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;

import net.mcreator.thebackwoods.procedures.SplinterNeedleRightclickedOnBlockProcedure;
import net.mcreator.thebackwoods.procedures.SplinterNeedleLivingEntityIsHitWithItemProcedure;
import net.mcreator.thebackwoods.procedures.SplinterNeedleItemInInventoryTickProcedure;

public class SplinterNeedleItem extends Item {
	public SplinterNeedleItem() {
		super(new Item.Properties().durability(20).attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, -0.3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		super.useOn(context);
		SplinterNeedleRightclickedOnBlockProcedure.execute(context.getLevel(), context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ(), context.getPlayer(), context.getItemInHand());
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		itemstack.hurtAndBreak(1, entity, LivingEntity.getSlotForHand(entity.getUsedItemHand()));
		SplinterNeedleLivingEntityIsHitWithItemProcedure.execute(entity);
		return true;
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		SplinterNeedleItemInInventoryTickProcedure.execute(world, entity);
	}
}