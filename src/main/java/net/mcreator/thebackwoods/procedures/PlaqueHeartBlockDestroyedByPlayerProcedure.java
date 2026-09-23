package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

public class PlaqueHeartBlockDestroyedByPlayerProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double roll = 0;
		roll = Mth.nextDouble(RandomSource.create(), 0, 1);
		entity.getPersistentData().putDouble("void_time", (entity.getPersistentData().getDouble("void_time") - 1000));
		if (roll < 0.65) {
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(TheBackwoodsModItems.MEMORY_FRAGMENT.get()));
				entityToSpawn.setPickUpDelay(5);
				entityToSpawn.setUnlimitedLifetime();
				_level.addFreshEntity(entityToSpawn);
			}
		} else if (roll < 0.675) {
			if (world instanceof ServerLevel _level) {
				ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(TheBackwoodsModItems.DISTORTED_MEMORY_FRAGMENT.get()));
				entityToSpawn.setPickUpDelay(7);
				entityToSpawn.setUnlimitedLifetime();
				_level.addFreshEntity(entityToSpawn);
			}
		}
		if (entity instanceof ServerPlayer _player) {
			AdvancementHolder _adv = _player.server.getAdvancements().get(ResourceLocation.parse("the_backwoods:borrowed_time"));
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