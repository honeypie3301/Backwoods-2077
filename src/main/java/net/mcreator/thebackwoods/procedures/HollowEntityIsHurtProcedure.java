package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

public class HollowEntityIsHurtProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (!entity.level().isClientSide())
			entity.discard();
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, (y + 1), z, 25, 0.5, 0.5, 0.5, 0.001);
		if (Math.random() < 0.0333) {
			if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 6, true, false));
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.cave")), SoundSource.AMBIENT, 8, (float) 0.5);
				} else {
					_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.cave")), SoundSource.AMBIENT, 8, (float) 0.5, false);
				}
			}
		}
	}
}