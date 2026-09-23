package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;

import net.mcreator.thebackwoods.entity.LignumVermisEntity;

public class LignumVermisPlayerCollidesWithThisEntityProcedure {
	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof Player) {
			if (entity instanceof LignumVermisEntity) {
				if (Math.random() < 0.02) {
					sourceentity.hurt(new DamageSource(world.holderOrThrow(DamageTypes.SWEET_BERRY_BUSH)), (float) 0.25);
				}
			}
		}
	}
}