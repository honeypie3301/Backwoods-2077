package net.mcreator.thebackwoods.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.thebackwoods.procedures.PetrifiedOakGallRangedItemShootsProjectileProcedure;
import net.mcreator.thebackwoods.entity.PetrifiedOakGallProjectileEntity;

public class PetrifiedOakGallItem extends Item {
	public PetrifiedOakGallItem() {
		super(new Item.Properties().stacksTo(32));
	}

	@Override
	public int getUseDuration(ItemStack itemstack, LivingEntity livingEntity) {
		return 20;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = InteractionResultHolder.fail(entity.getItemInHand(hand));
		if (entity.getAbilities().instabuild || findAmmo(entity) != ItemStack.EMPTY) {
			ar = InteractionResultHolder.consume(entity.getItemInHand(hand));
			entity.startUsingItem(hand);
		}
		return ar;
	}

	@Override
	public void onUseTick(Level world, LivingEntity entity, ItemStack itemstack, int time) {
		if (!world.isClientSide() && entity instanceof ServerPlayer player) {
			ItemStack stack = findAmmo(player);
			if (player.getAbilities().instabuild || stack != ItemStack.EMPTY) {
				PetrifiedOakGallProjectileEntity projectile = PetrifiedOakGallProjectileEntity.shoot(world, entity, world.getRandom());
				if (player.getAbilities().instabuild) {
					projectile.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
				} else {
					if (stack.isDamageableItem()) {
						if (world instanceof ServerLevel serverLevel)
							stack.hurtAndBreak(1, serverLevel, player, _stkprov -> {
							});
					} else {
						stack.shrink(1);
					}
				}
				PetrifiedOakGallRangedItemShootsProjectileProcedure.execute(entity, itemstack);
			}
			entity.releaseUsingItem();
		}
	}

	private ItemStack findAmmo(Player player) {
		return new ItemStack(PetrifiedOakGallProjectileEntity.PROJECTILE_ITEM.getItem());
	}
}