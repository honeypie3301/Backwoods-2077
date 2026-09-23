package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class PetrifiedRottenSwordLivingEntityIsHitWithToolProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double roll = 0;
		roll = Mth.nextInt(RandomSource.create(), 1, 5);
		if (entity instanceof Player) {
			if (roll == 1) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1, false, false));
			} else if (roll == 2) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 250, 1, false, false));
			} else if (roll == 3) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 90, 0, false, false));
			}
		} else if (entity instanceof Mob) {
			if (roll == 1) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 2, false, false));
			} else if (roll == 2) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 270, 2, false, false));
			} else if (roll == 3) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 1, false, false));
			}
		}
	}
}