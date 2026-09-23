package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;

public class BlindspotSplinterItIsStruckByLightningProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof BlindspotSplinterEntity _datEntSetI)
			_datEntSetI.getEntityData().set(BlindspotSplinterEntity.DATA_Age, (int) ((entity instanceof BlindspotSplinterEntity _datEntI ? _datEntI.getEntityData().get(BlindspotSplinterEntity.DATA_Age) : 0) + 60000));
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SMOKE, x, (y + 1), z, 15, 0.3, 0.9, 0.3, 0.04);
	}
}