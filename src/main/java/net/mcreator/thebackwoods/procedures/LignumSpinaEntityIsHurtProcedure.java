package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

public class LignumSpinaEntityIsHurtProcedure {
	public static void execute(LevelAccessor world, Entity sourceentity) {
		if (sourceentity == null)
			return;
		if (sourceentity instanceof LivingEntity) {
			sourceentity.hurt(new DamageSource(world.holderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:lignum_spina_thorns")))), 1);
		}
	}
}