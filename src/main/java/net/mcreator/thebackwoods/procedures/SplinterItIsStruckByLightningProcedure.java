package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.thebackwoods.entity.SplinterEntity;

public class SplinterItIsStruckByLightningProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof SplinterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(SplinterEntity.DATA_Age, (int) ((entity instanceof SplinterEntity _datEntI ? _datEntI.getEntityData().get(SplinterEntity.DATA_Age) : 0) + 60000));
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SMOKE, x, (y + 1), z, 15, 0.3, 0.9, 0.3, 0.04);
	}
}