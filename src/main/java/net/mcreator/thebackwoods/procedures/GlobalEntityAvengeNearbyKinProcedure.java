package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Difficulty;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

import java.util.Comparator;

@EventBusSubscriber
public class GlobalEntityAvengeNearbyKinProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double scan_radius = 0;
		if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities")))) {
			if (world.getDifficulty() == Difficulty.HARD) {
				scan_radius = (double) (world instanceof net.minecraft.server.level.ServerLevel _slv ? _slv.getServer().getPlayerList().getViewDistance() * 16 : 64);
			} else if (world.getDifficulty() == Difficulty.NORMAL) {
				scan_radius = 80;
			} else {
				scan_radius = 48;
			}
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(scan_radius / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities")))) {
						entityiterator.getPersistentData().putDouble("isEnraged", 1);
						entityiterator.getPersistentData().putDouble("forced_hunt_ticks", 600);
						if (event instanceof net.neoforged.neoforge.event.entity.living.LivingDeathEvent _deathEvent) {
							net.minecraft.world.entity.Entity _attacker = _deathEvent.getSource().getEntity();
							if (entityiterator instanceof net.minecraft.world.entity.LivingEntity _livingEntity && _attacker instanceof net.minecraft.world.entity.LivingEntity _livingAttacker) {
								_livingEntity.setLastHurtByMob(_livingAttacker);
							}
						}
					}
				}
			}
		}
	}
}