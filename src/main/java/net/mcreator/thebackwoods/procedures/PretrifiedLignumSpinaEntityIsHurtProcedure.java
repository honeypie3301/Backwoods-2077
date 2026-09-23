package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;

public class PretrifiedLignumSpinaEntityIsHurtProcedure {
	public static void execute(LevelAccessor world, Entity sourceentity) {
		if (sourceentity == null)
			return;
		if (sourceentity instanceof LivingEntity) {
			sourceentity.hurt(new DamageSource(world.holderOrThrow(DamageTypes.THORNS)), 3);
		}
	}
}