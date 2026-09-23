package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

public class RotOnInitialEntitySpawnProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livingEntity0 && _livingEntity0.getAttributes().hasAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY))
			_livingEntity0.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(0.8);
		entity.getPersistentData().putBoolean("master_follow_enabled", true);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.FLASH, (entity.getX()), (entity.getY() + 1), (entity.getZ()), 1, 0.5, 1, 0.5, 0.1);
	}
}