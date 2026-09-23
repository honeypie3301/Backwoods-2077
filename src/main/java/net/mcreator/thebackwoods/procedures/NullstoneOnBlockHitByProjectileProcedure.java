package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Comparator;

public class NullstoneOnBlockHitByProjectileProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		{
			final Vec3 _center = new Vec3(x, y, z);
			for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
				if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("minecraft:arrows"))) || entityiterator instanceof SpectralArrow || entityiterator instanceof Arrow) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.FLASH, (entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()), 1, 0, 0, 0, 0);
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
		}
	}
}