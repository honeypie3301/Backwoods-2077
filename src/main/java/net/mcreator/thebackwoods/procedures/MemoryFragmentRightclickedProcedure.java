package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;

public class MemoryFragmentRightclickedProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		boolean converted = false;
		converted = false;
		if (entity instanceof Player) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).is(ItemTags.create(ResourceLocation.parse("the_backwoods:faded_food")))) {
				if (itemstack.getDamageValue() < itemstack.getMaxDamage()) {
					if (entity instanceof net.minecraft.world.entity.player.Player player) {
						net.minecraft.world.item.ItemStack off = player.getOffhandItem();
						if (!off.isEmpty() && off.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("the_backwoods:faded_food")))) {
							net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(off.getItem());
							if (id != null) {
								String path = id.getPath();
								if (path.startsWith("faded_")) {
									String normalPath = path.substring("faded_".length());
									// Try same namespace first
									net.minecraft.resources.ResourceLocation sameNs = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(id.getNamespace(), normalPath);
									net.minecraft.world.item.Item normal = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(sameNs);
									// Fallback to vanilla namespace
									if (normal == net.minecraft.world.item.Items.AIR) {
										net.minecraft.resources.ResourceLocation mc = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", normalPath);
										normal = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(mc);
									}
									if (normal != net.minecraft.world.item.Items.AIR) {
										if (off.getCount() == 1) {
											player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new net.minecraft.world.item.ItemStack(normal, 1));
										} else {
											off.shrink(1);
											net.neoforged.neoforge.items.ItemHandlerHelper.giveItemToPlayer(player, new net.minecraft.world.item.ItemStack(normal, 1));
										}
										converted = true; // use your existing Blockly boolean
									}
								}
							}
						}
					}
					if (converted == true) {
						if (entity instanceof LivingEntity _entity)
							_entity.swing(InteractionHand.MAIN_HAND, true);
						if (world instanceof ServerLevel _level) {
							itemstack.hurtAndBreak(1, _level, null, _stkprov -> {
							});
						}
						if (entity instanceof Player _player)
							_player.getCooldowns().addCooldown(itemstack.getItem(), 16);
					}
				}
			}
		}
	}
}