package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

public class PetrifiedResinRightclickedProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof Player) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheBackwoodsModItems.PETRIFIED_RESIN.get()) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).is(ItemTags.create(ResourceLocation.parse("the_backwoods:fade_immune")))) {
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).isDamaged()) {
						(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).setDamageValue((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getDamageValue() - 25);
						if (entity instanceof LivingEntity _entity)
							_entity.swing(InteractionHand.OFF_HAND, true);
						if (entity instanceof ServerPlayer _player) {
							AdvancementHolder _adv = _player.server.getAdvancements().get(ResourceLocation.parse("the_backwoods:use_resin"));
							if (_adv != null) {
								AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
								if (!_ap.isDone()) {
									for (String criteria : _ap.getRemainingCriteria())
										_player.getAdvancements().award(_adv, criteria);
								}
							}
						}
						if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
							(entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).shrink(1);
						}
					}
				}
			}
		}
	}
}