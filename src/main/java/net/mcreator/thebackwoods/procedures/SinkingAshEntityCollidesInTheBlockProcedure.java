package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class SinkingAshEntityCollidesInTheBlockProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double slowFactor = 0;
		double damageInterval = 0;
		if (entity instanceof LivingEntity) {
			if (world.getDifficulty() == Difficulty.PEACEFUL || world.getDifficulty() == Difficulty.EASY) {
				damageInterval = 60;
				slowFactor = 0.6;
			} else if (world.getDifficulty() == Difficulty.NORMAL) {
				damageInterval = 40;
				slowFactor = 0.35;
			} else if (world.getDifficulty() == Difficulty.HARD) {
				damageInterval = 20;
				slowFactor = 0.15;
			} else {
				damageInterval = 5;
				slowFactor = 0.1;
			}
			entity.setDeltaMovement(new Vec3((entity.getDeltaMovement().x() * slowFactor), (entity.getDeltaMovement().y() * 0.85), (entity.getDeltaMovement().z() * slowFactor)));
			entity.fallDistance = 0;
			if ((world.getBlockState(BlockPos.containing(entity.getX(), Math.floor(entity.getY()) + 1.6, entity.getZ()))).getBlock() == TheBackwoodsModBlocks.SINKING_ASH.get()) {
				if (!(entity instanceof LivingEntity _livEnt15 && _livEnt15.hasEffect(MobEffects.BLINDNESS))) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, false));
				}
				if (entity.tickCount % damageInterval == 0) {
					entity.hurt(new DamageSource(world.holderOrThrow(DamageTypes.IN_WALL)), 1);
				}
			}
		}
	}
}