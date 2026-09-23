package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import net.mcreator.thebackwoods.entity.LignumGigasEntity;

public class LignumGigasOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LignumGigasEntity _datEntSetL)
			_datEntSetL.getEntityData().set(LignumGigasEntity.DATA_play_attack_anim, false);
		if (entity instanceof LignumGigasEntity _datEntSetI)
			_datEntSetI.getEntityData().set(LignumGigasEntity.DATA_hit_stage, 0);
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) Double.POSITIVE_INFINITY, 7, true, false));
		if (entity instanceof LivingEntity _livingEntity3 && _livingEntity3.getAttributes().hasAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY))
			_livingEntity3.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(1);
		if (entity instanceof LivingEntity _livingEntity4 && _livingEntity4.getAttributes().hasAttribute(Attributes.GRAVITY))
			_livingEntity4.getAttribute(Attributes.GRAVITY).setBaseValue(10);
	}
}