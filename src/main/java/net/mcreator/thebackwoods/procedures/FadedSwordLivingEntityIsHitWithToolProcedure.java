package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class FadedSwordLivingEntityIsHitWithToolProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double roll = 0;
		roll = Mth.nextInt(RandomSource.create(), 1, 4);
		if (roll == 2) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0, false, false));
		} else if (roll == 3) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 4800, 0, false, false));
		}
	}
}