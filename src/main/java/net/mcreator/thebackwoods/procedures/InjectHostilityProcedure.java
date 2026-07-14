package net.mcreator.thebackwoods.procedures;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.thebackwoods.entity.RotEntity;
import net.mcreator.thebackwoods.entity.SplinterEntity;
import net.mcreator.thebackwoods.entity.LogSplinterEntity;
import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;
import net.mcreator.thebackwoods.entity.PetrifiedLogSplinterEntity;

import javax.annotation.Nullable;
import net.neoforged.bus.api.Event;

@EventBusSubscriber
public class InjectHostilityProcedure {
	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof PathfinderMob mob) {
			// Don't inject onto our own Rot entities, players, or friendly animals/villagers
			if (isRotEntity(mob)) {
				return;
			}
			
			// Only inject if not already injected (safety and performance)
			if (!mob.getPersistentData().getBoolean("RotHostilityInjected")) {
				// Inject the dynamic targeting goal with priority 2
				mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, true, (target) -> {
					return isRotEntity(target);
				}));
				
				// Make sure they also retaliate when hurt by Rot or others
				mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));
				
				mob.getPersistentData().putBoolean("RotHostilityInjected", true);
			}
		}
	}

	private static boolean isRotEntity(LivingEntity entity) {
		if (entity instanceof RotEntity 
			|| entity instanceof SplinterEntity 
			|| entity instanceof LogSplinterEntity 
			|| entity instanceof BlindspotSplinterEntity 
			|| entity instanceof PetrifiedLogSplinterEntity) {
			return true;
		}
		// Also check the tag
		return entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities")));
	}
} // 1.21.1
