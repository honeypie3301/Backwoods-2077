package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.thebackwoods.entity.LogSplinterEntity;

public class LogSplinterItIsStruckByLightningProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LogSplinterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(LogSplinterEntity.DATA_Age, (int) ((entity instanceof LogSplinterEntity _datEntI ? _datEntI.getEntityData().get(LogSplinterEntity.DATA_Age) : 0) + 55000));
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SMOKE, x, (y + 1), z, 15, 0.3, 0.9, 0.3, 0.04);
	}
}