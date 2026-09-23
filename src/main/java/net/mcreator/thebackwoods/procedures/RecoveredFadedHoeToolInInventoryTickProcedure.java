package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;

public class RecoveredFadedHoeToolInInventoryTickProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if (itemstack != null) {
			if (entity instanceof ServerPlayer) {
				if (!(entity instanceof ServerPlayer _plr2 && _plr2.level() instanceof ServerLevel
						&& _plr2.getAdvancements().getOrStartProgress(_plr2.server.getAdvancements().get(ResourceLocation.parse("the_backwoods:obtain_first_recovered_tool"))).isDone())) {
					if (entity instanceof ServerPlayer _player) {
						AdvancementHolder _adv = _player.server.getAdvancements().get(ResourceLocation.parse("the_backwoods:obtain_first_recovered_tool"));
						if (_adv != null) {
							AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
							if (!_ap.isDone()) {
								for (String criteria : _ap.getRemainingCriteria())
									_player.getAdvancements().award(_adv, criteria);
							}
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("hoe_resin_cd") > 0) {
				entity.getPersistentData().putDouble("hoe_resin_cd", (entity.getPersistentData().getDouble("hoe_resin_cd") - 1));
			}
			if (itemstack.getDamageValue() > 0) {
				if (!world.isClientSide()) {
					if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:loss"))) {
						if (world.getLevelData().getGameTime() % 100 == 0) {
							itemstack.setDamageValue(itemstack.getDamageValue() - 1);
						}
					} else {
						if (world.getLevelData().getGameTime() % 300 == 0) {
							itemstack.setDamageValue(itemstack.getDamageValue() - 2);
						}
					}
				}
			}
		}
	}
}