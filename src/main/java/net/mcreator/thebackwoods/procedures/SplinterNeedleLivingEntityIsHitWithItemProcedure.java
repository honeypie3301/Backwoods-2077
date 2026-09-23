package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import net.mcreator.thebackwoods.init.TheBackwoodsModMobEffects;

public class SplinterNeedleLivingEntityIsHitWithItemProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double base_amplifier = 0;
		double base_duration = 0;
		base_duration = 600;
		base_amplifier = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getArmorValue() : 0) > 0) {
			base_duration = 60;
			base_amplifier = 0;
		}
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(TheBackwoodsModMobEffects.SPLINTERED_EFFECT, (int) base_duration, (int) base_amplifier, true, false));
	}
}