package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class RottenAxeLivingEntityIsHitWithToolProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double roll = 0;
		roll = Mth.nextInt(RandomSource.create(), 1, 2);
		if (roll == 2) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 150, 0, false, false));
		}
	}
}