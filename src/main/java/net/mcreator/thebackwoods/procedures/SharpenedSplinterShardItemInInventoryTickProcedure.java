package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;

import net.mcreator.thebackwoods.init.TheBackwoodsModMobEffects;

public class SharpenedSplinterShardItemInInventoryTickProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (world.getLevelData().getGameTime() % 160 == 0) {
			if (entity instanceof LivingEntity && !(entity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(TheBackwoodsModMobEffects.INOCULATION_EFFECT))) {
				entity.hurt(new DamageSource(world.holderOrThrow(DamageTypes.THORNS)), (float) 0.08);
			}
		}
	}
}