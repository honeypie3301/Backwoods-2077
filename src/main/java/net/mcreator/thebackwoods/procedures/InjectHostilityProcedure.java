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
import net.minecraft.world.entity.player.Player;
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
	public static boolean ENABLE_BOSS_HOSTILITY_INJECTION = true;

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
		if (entity == null || !ENABLE_BOSS_HOSTILITY_INJECTION)
			return;

		if (entity instanceof PathfinderMob mob) {
			// Don't inject onto our own Rot entities
			if (isRotEntity(mob)) {
				return;
			}
			
			// Initial injection
			if (!mob.getPersistentData().getBoolean("RotHostilityInjected")) {
				// 1. Remove the boss's hardcoded AlwaysTargetPlayerGoal that overrides everything
				mob.targetSelector.removeAllGoals(goal -> {
					String className = goal.getClass().getName();
					return className.contains("AlwaysTargetPlayer") || className.contains("NearestAttackableTargetGoal");
				});

				// 2. Add HurtByTargetGoal at Priority 0 (highest)
				mob.targetSelector.addGoal(0, new HurtByTargetGoal(mob));

				// 3. Add our Rot target goal at Priority 1
				mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<LivingEntity>(mob, LivingEntity.class, true) {
					@Override
					public boolean canUse() {
						boolean canUse = super.canUse();
						if (canUse && this.target != null && !isRotEntity(this.target)) {
							this.target = null;
							return false;
						}
						return canUse;
					}
				});
				
				mob.getPersistentData().putBoolean("RotHostilityInjected", true);
			}

			// 4. Tick safeguard: If the entity was forced to target a Creative/Spectator player, clear it
			LivingEntity currentTarget = mob.getTarget();
			if (currentTarget instanceof Player player) {
				if (player.isCreative() || player.isSpectator()) {
					mob.setTarget(null);
				}
			}
		}
	}

	private static boolean isRotEntity(LivingEntity entity) {
		if (entity == null) return false;
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
}
