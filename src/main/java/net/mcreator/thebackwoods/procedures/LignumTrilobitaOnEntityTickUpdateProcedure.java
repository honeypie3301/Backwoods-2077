package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.RotEntity;
import net.mcreator.thebackwoods.entity.LignumTrilobitaEntity;

public class LignumTrilobitaOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity.isAlive()) {
			if (!world.getEntitiesOfClass(RotEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3((entity.getX()), (entity.getY()), (entity.getZ()))).inflate(200 / 2d), e -> true).isEmpty()) {
				if (entity instanceof LignumTrilobitaEntity _datEntSetL)
					_datEntSetL.getEntityData().set(LignumTrilobitaEntity.DATA_isHiding, false);
			} else if (entity.level().getGameTime() % 10 == 0) {
				if ((entity instanceof LignumTrilobitaEntity _datEntL6 && _datEntL6.getEntityData().get(LignumTrilobitaEntity.DATA_isHiding)) == true) {
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + 1);
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) >= 10) {
						if (entity instanceof LignumTrilobitaEntity _datEntSetL)
							_datEntSetL.getEntityData().set(LignumTrilobitaEntity.DATA_isHiding, false);
					}
				} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) <= 4) {
					if (entity instanceof LignumTrilobitaEntity _datEntSetL)
						_datEntSetL.getEntityData().set(LignumTrilobitaEntity.DATA_isHiding, true);
				}
			}
		}
	}
}