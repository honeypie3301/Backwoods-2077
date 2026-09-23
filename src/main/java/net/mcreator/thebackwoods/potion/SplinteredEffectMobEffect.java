package net.mcreator.thebackwoods.potion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

import net.mcreator.thebackwoods.procedures.SplinteredEffectOnEffectActiveTickProcedure;

public class SplinteredEffectMobEffect extends MobEffect {
	public SplinteredEffectMobEffect() {
		super(MobEffectCategory.HARMFUL, -10079488);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		SplinteredEffectOnEffectActiveTickProcedure.execute(entity.level(), entity);
		return super.applyEffectTick(entity, amplifier);
	}
}