package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.ThePetrifiedWealdCanMakePortalProcedure;
import net.mcreator.thebackwoods.block.ThePetrifiedWealdPortalBlock;

public class ThePetrifiedWealdItem extends Item {
	public ThePetrifiedWealdItem() {
		super(new Item.Properties().rarity(Rarity.UNCOMMON).durability(64));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player entity = context.getPlayer();
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		ItemStack itemstack = context.getItemInHand();
		Level world = context.getLevel();
		if (!entity.mayUseItemAt(pos, context.getClickedFace(), itemstack)) {
			return InteractionResult.FAIL;
		} else {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			boolean success = false;
			if (world.isEmptyBlock(pos) && ThePetrifiedWealdCanMakePortalProcedure.execute(world, entity, itemstack)) {
				ThePetrifiedWealdPortalBlock.portalSpawn(world, pos);
				itemstack.hurtAndBreak(1, entity, LivingEntity.getSlotForHand(context.getHand()));
				success = true;
			}
			return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
	}
}