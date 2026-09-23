package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Difficulty;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import java.util.Comparator;

public class SplinterAvengeOnHurtProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double scan_radius = 0;
		if ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) instanceof Player) {
			if (world.getDifficulty() == Difficulty.HARD) {
				scan_radius = (double) (world instanceof net.minecraft.server.level.ServerLevel _slv ? _slv.getServer().getPlayerList().getViewDistance() * 16 : 64);
			} else if (world.getDifficulty() == Difficulty.NORMAL) {
				scan_radius = 80;
			} else {
				scan_radius = 40;
			}
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(scan_radius / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities")))) {
						entityiterator.getPersistentData().putDouble("isEnraged", 1);
						entityiterator.getPersistentData().putDouble("forced_hunt_ticks", 600);
						if (entityiterator instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
							_entity.setTarget(_ent);
					}
				}
			}
		}
	}
}