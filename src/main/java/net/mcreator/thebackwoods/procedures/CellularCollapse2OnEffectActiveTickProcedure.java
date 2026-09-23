package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

public class CellularCollapse2OnEffectActiveTickProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity.tickCount % 40 == 0) {
			entity.hurt(new DamageSource(world.holderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:cellular_collapse_damage")))), (float) 0.5);
			if (entity instanceof Player _player)
				_player.causeFoodExhaustion((float) 0.4);
			if (Math.random() <= 0.3 && !(entity instanceof LivingEntity _livEnt4 && _livEnt4.hasEffect(MobEffects.POISON))) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.POISON, 90, 0, true, false));
			}
			if (Math.random() <= 0.45 && !(entity instanceof LivingEntity _livEnt6 && _livEnt6.hasEffect(MobEffects.HUNGER))) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 90, 0, true, false));
			}
		}
	}
}