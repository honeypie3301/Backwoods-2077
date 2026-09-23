package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import net.mcreator.thebackwoods.init.TheBackwoodsModMobEffects;

public class WindcapMushroomPlayerFinishesUsingItemProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (!(entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(TheBackwoodsModMobEffects.LIGHT_LUNGS))) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(TheBackwoodsModMobEffects.LIGHT_LUNGS, 670, 0, false, false));
		}
	}
}