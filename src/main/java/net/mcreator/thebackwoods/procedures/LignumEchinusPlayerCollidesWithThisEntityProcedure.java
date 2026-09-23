package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import net.mcreator.thebackwoods.entity.PetrifiedLignumEchinusEntity;
import net.mcreator.thebackwoods.entity.LignumEchinusEntity;

public class LignumEchinusPlayerCollidesWithThisEntityProcedure {
	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof LivingEntity && !sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities")))) {
			if (entity instanceof LignumEchinusEntity || entity instanceof PetrifiedLignumEchinusEntity) {
				if (Math.random() < 0.02) {
					sourceentity.hurt(new DamageSource(world.holderOrThrow(DamageTypes.THORNS)), (float) 0.25);
				}
			}
		}
	}
}