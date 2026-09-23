package net.mcreator.thebackwoods.potion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

import net.mcreator.thebackwoods.procedures.InoculationEffectOnEffectActiveTickProcedure;

public class InoculationEffectMobEffect extends MobEffect {
	public InoculationEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -29117);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		InoculationEffectOnEffectActiveTickProcedure.execute(entity);
		return super.applyEffectTick(entity, amplifier);
	}
}