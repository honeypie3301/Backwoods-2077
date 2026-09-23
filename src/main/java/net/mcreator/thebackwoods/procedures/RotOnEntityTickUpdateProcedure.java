package net.mcreator.thebackwoods.procedures;
// 1.21.1 - Sentinel Adaptive Overhaul
import net.mcreator.thebackwoods.BlackHole;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;

import net.mcreator.thebackwoods.entity.RotEntity;

import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

@EventBusSubscriber
public class RotOnEntityTickUpdateProcedure {
	public static boolean dealTrueDamageToBosses(net.minecraft.world.entity.Entity target, net.minecraft.world.damagesource.DamageSource ds, float amount) {
		if (target == null || !target.isAlive()) return false;
		if (target instanceof net.minecraft.world.entity.player.Player player) {
			if (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable) return false;
			float hpBefore = player.getHealth();
			boolean hurtSuccess = player.hurt(ds, amount);
			float hpAfter = player.getHealth();
			float actualDealt = Math.max(0.0F, hpBefore - hpAfter);
			if (actualDealt <= 0.0F && amount > 0.0F && !player.isInvulnerable()) {
				actualDealt = Math.min(amount, hpBefore);
			}
			if (ds != null && ds.getEntity() instanceof Entity attacker) {
				recordDamageDealtMetrics(attacker, target, actualDealt);
			}
			return hurtSuccess;
		}
		if (target.isInvulnerable()) return false;

		String targetType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase(java.util.Locale.ROOT);
		if ("alexscaves:ferrouswroughtnaut".equals(targetType) || "alexscaves:ferrous_wroughtnaut".equals(targetType)) return false;

		if (target instanceof net.minecraft.world.entity.LivingEntity living) {
			living.invulnerableTime = 0;

			living.getActiveEffects().removeIf(effect -> {
				var effKey = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
				String effectId = effKey != null ? effKey.toString().toLowerCase(java.util.Locale.ROOT) : "";
				return effectId.contains("invincib") || effectId.contains("immunity") || effectId.contains("invulnerab");
			});

			float oldHealth = living.getHealth();
			boolean hurtSuccess = living.hurt(ds, amount);
			float newHealth = living.getHealth();
			float actualDealt = Math.max(0.0F, oldHealth - newHealth);

			if ((!hurtSuccess || newHealth >= oldHealth) && amount > 0) {
				float targetHealth = Math.max(0.0F, oldHealth - amount);
				actualDealt = oldHealth - targetHealth;
				living.setHealth(targetHealth);
				living.hurtTime = 10;
				living.hurtDuration = 10;
				living.hurtMarked = true;
				if (targetHealth <= 0.0F && !living.isDeadOrDying()) {
					living.die(ds);
				}
				if (ds != null && ds.getEntity() instanceof Entity attacker) {
					recordDamageDealtMetrics(attacker, target, actualDealt);
				}
				return true;
			}
			if (ds != null && ds.getEntity() instanceof Entity attacker) {
				recordDamageDealtMetrics(attacker, target, actualDealt);
			}
			return hurtSuccess;
		}
		boolean res = target.hurt(ds, amount);
		if (ds != null && ds.getEntity() instanceof Entity attacker) {
			recordDamageDealtMetrics(attacker, target, amount);
		}
		return res;
	}

	private static void recordDamageDealtMetrics(Entity attacker, Entity victim, float dealtAmount) {
		if (attacker == null || dealtAmount <= 0.0F) return;
		try {
			putD(attacker, "sentinel_total_damage_dealt", getD(attacker, "sentinel_total_damage_dealt") + dealtAmount);
			putD(attacker, "rot_dmg_this_sec", getD(attacker, "rot_dmg_this_sec") + dealtAmount);
			putD(attacker, "sentinel_last_damage_dealt_amount", dealtAmount);
			putD(attacker, "sentinel_last_damage_dealt_time", attacker.level() != null ? (double) attacker.level().getGameTime() : 0.0);
			if (attacker instanceof RotEntity rot && victim instanceof LivingEntity targetLiv) {
				UniversalCombatPredictionEngine.recordActualAttack(rot, targetLiv, inferCurrentAttackType(rot));
			}
		} catch (Exception ignored) {}
	}

	public static double TELEGRAPH_JITTER_MAX_TICKS = 3.0;
	public static double CONTEXT_SCORE_WEIGHT_NN = 25.0;
	public static double EARNED_UNLOCK_SKILL_SCALING = 1.5;
	public static double DYNAMIC_RANGE_OFFSET_VARIANCE = 1.5;
	public static boolean ENABLE_EXTRACTION_GRAPPLE = false;
	public static boolean ENABLE_TELEKINESIS = false;
	public static boolean ENABLE_BLOCKING = true;
	public static boolean ENABLE_CONTROLLED_ADAPTATION = true;
	public static boolean ENABLE_PHASE_SHIFT = false;
	public static boolean ENABLE_ASYNC_PATHFINDING = false;
	public static boolean ENABLE_CHAT_LOGGING = false;
	public static final java.util.List<String> WHITELISTED_FRIENDS = new java.util.ArrayList<>(java.util.Arrays.asList("AEDADA"));

	public static boolean isWhitelistedFriend(String name) {
		if (name == null || name.isEmpty()) return false;
		for (String friend : WHITELISTED_FRIENDS) {
			if (friend.equalsIgnoreCase(name)) return true;
		}
		return false;
	}

	public static int countNearbyAlliedRots(Entity rot) {
		if (rot == null || rot.level() == null) return 0;
		AABB searchBox = rot.getBoundingBox().inflate(32.0);
		List<Entity> list = rot.level().getEntities(rot, searchBox, e -> e != rot && (e instanceof RotEntity || e.getClass().getSimpleName().contains("Rot")));
		return list.size();
	}

	public static boolean canRotTankTarget(Entity rot, Entity target) {
		if (rot == null || target == null) return true;
		if (!(rot instanceof LivingEntity rotLiv)) return true;
		float hpRatio = rotLiv.getMaxHealth() > 0 ? rotLiv.getHealth() / rotLiv.getMaxHealth() : 1.0f;
		if (target instanceof LivingEntity targetLiv) {
			String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(targetLiv.getType()).toString().toLowerCase();
			if (typeKey.contains("warden") || typeKey.contains("cow") || typeKey.contains("pig") || typeKey.contains("sheep") || typeKey.contains("villager")) {
				return true;
			}
		}
		return hpRatio > 0.35f;
	}

	private static void applyFacingPreservingBackOff(Entity entity, Mob mob, Entity target, double speed) {
		if (entity == null || mob == null || target == null) return;
		mob.getNavigation().stop();

		double lookDx = target.getX() - entity.getX();
		double lookDz = target.getZ() - entity.getZ();
		double lookDy = target.getEyeY() - entity.getEyeY();
		double flatDist = Math.sqrt(lookDx * lookDx + lookDz * lookDz);

		if (flatDist > 1.0e-5) {
			float targetYaw = (float) (net.minecraft.util.Mth.atan2(lookDz, lookDx) * (180.0D / Math.PI)) - 90.0F;
			float targetPitch = (float) (-(net.minecraft.util.Mth.atan2(lookDy, flatDist) * (180.0D / Math.PI)));

			entity.setYRot(targetYaw);
			entity.yRotO = targetYaw;
			if (entity instanceof LivingEntity le) {
				le.setYHeadRot(targetYaw);
				le.yHeadRotO = targetYaw;
				le.setYBodyRot(targetYaw);
				le.yBodyRotO = targetYaw;
			}
			entity.setXRot(targetPitch);
			entity.xRotO = targetPitch;

			mob.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), 60.0F, 60.0F);
		}

		double dx = entity.getX() - target.getX();
		double dz = entity.getZ() - target.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist < 0.1) {
			dx = 1.0;
			dz = 0.0;
			dist = 1.0;
		}
		double nx = dx / dist;
		double nz = dz / dist;

		if ((entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0) {
			entity.setDeltaMovement(nx * speed, entity.getDeltaMovement().y, nz * speed);
		}
		entity.hasImpulse = true;
	}

	public static double PARTICLE_QUALITY = 1.0;
	public static double COOLDOWN_MULTIPLIER = 1.5;
	private static final double TARGET_RANGE = 128.0;

	public static double ROT_PLAYER_BACK_OFF_DISTANCE = 1.35;
	public static double ROT_PILLAR_BACK_OFF_DISTANCE = 15.0;
	public static double ROT_PILLAR_INITIAL_ATTACK_DELAY = 40.0;
	public static double ROT_PILLAR_ATTACK_CHANCE = 0.005;
	public static double ROT_PILLAR_CIRCLING_SPEED = 0.8;
	public static double ROT_PILLAR_CIRCLING_CHANCE = 0.15;

	public static double SONIC_SCREAM_COOLDOWN = 1800.0;

	public static double SUPERHEAT_EVAPORATION_COOLDOWN = 700.0;
	public static double SUPERHEAT_EVAPORATION_RADIUS = 45.0;
	public static double SUPERHEAT_EVAPORATION_DAMAGE = 26.0;
	public static double SUPERHEAT_EVAPORATION_WAVE_TICKS = 90.0;

	public static double SONIC_BOOM_RANGE = 24.0;
	public static double SONIC_BOOM_MIN_DIST = 2.5;
	public static double SONIC_BOOM_COOLDOWN = 324.0;
	public static double SONIC_BOOM_TORSO_Y_FACTOR = 1;
	public static double WARDEN_LEARN_REQUIRED_TICKS = 405.0;
	public static double SONIC_BOOM_ANIMATION_TICKS = 90.0;
	public static double SONIC_BOOM_TRIGGER_TICK = 36.0;

	private static final double LASER_Y_OFFSET = 0.25;
	private static final double LASER_CLOSING_TICKS = 80.0;
	private static final double TOTEM_LASER_MIN_DIST = 2.5;

	private static final double TELEPORT_MIN_GAP = 4.0;
	private static final double TELEPORT_BACK_OFFSET = 2.4;
	private static final double TELEPORT_SIDE_MIN = 1.6;
	private static final double TELEPORT_SIDE_MAX = 3.0;
	private static final double TELEPORT_MAX_VERTICAL_DIFF = 6.0;

	private static final double DODGE_TRIGGER_DIST = 6.0;
	private static final double DODGE_SWING_CHANCE = 0.85;

	private static final int MINE_REACH = 3;
	private static final int MINE_HEIGHT = 3;
	private static final int MINE_HALF_WIDTH = 1;
	private static final float MAX_BREAKABLE_HARDNESS = 60f;
	private static final float MINE_SPEED_MULTIPLIER = 16.665f;
	private static final float MINE_SPEED_BASE = 16.665f;
	private static final double MINE_RAY_DISTANCE = 2.0;

	private static final double DIE_KICK_SPEED = 10.0;

	public static int TP_DODGE_CD = 40;
	public static int TP_FLANK_CD = 100;
	public static int SOLAR_CD = 360;
	public static int ADAPT_CD = 320;
	public static int GRAPPLE_CD = 220;
	public static int TK_CD = 220;

	public static double MELEE_ATTACK_CD = 20.0;
	public static double COMBO_GLOBAL_CD = 50.0;
	public static double COMBO_GLOBAL_CD_TOTEM = 20.0;
	public static int TELEPORT_DODGE_COOLDOWN = 18;
	public static int TELEPORT_FLANK_COOLDOWN = 30;
	public static int SOLAR_BEAM_COOLDOWN = 360;
	public static int CRYO_BEAM_COOLDOWN = 360;
	public static int MUTANT_GRAPPLE_COOLDOWN = 220;
	public static int TELEKINESIS_COOLDOWN = 220;

	public static double COMBO_TRIPLE_THREAT_CD = 1000.0;
	public static double COMBO_TRIPLE_THREAT_CD_TOTEM = 800.0;
	public static double COMBO_HIGH_SKY_SLAM_CD = 1000.0;
	public static double COMBO_HIGH_SKY_SLAM_CD_TOTEM = 800.0;
	public static double COMBO_PUNCH_DROPKICK_CD = 1000.0;
	public static double COMBO_PUNCH_DROPKICK_CD_TOTEM = 800.0;
	public static double COMBO_PUNCH_RIDER_KICK_CD = 1000.0;
	public static double COMBO_PUNCH_RIDER_KICK_CD_TOTEM = 800.0;
	public static double COMBO_HEAVENLY_REPENTANCE_PLUS_CD = 1000.0;
	public static double COMBO_HEAVENLY_REPENTANCE_PLUS_CD_TOTEM = 800.0;

	public static double DIVE_COUNTER_MIN_HEIGHT = 4.0;
	public static double DIVE_COUNTER_TRIGGER_RANGE = 12.0;
	public static double DIVE_COUNTER_COOLDOWN = 240.0;

	public static double OMNI_SONIC_BOOM_CD = 600.0;
	public static double OMNI_SONIC_BOOM_ANIMATION_TICKS = 270.0;
	public static double OMNI_SONIC_BOOM_TRIGGER_TICK = 140.0;
	public static double OMNI_SONIC_BOOM_RANGE = 24.0;
	public static boolean OMNI_SONIC_BOOM_SHOW_PARTICLES = true;

	public static double ADAPTATION_REGEN_BASE_HEAL = 1.0;
	public static double ADAPTATION_REGEN_MAX_HEALTH_RATIO = 0.0075;
	public static double ADAPTATION_REGEN_COMBAT_MULTIPLIER = 2.0;
	public static double ADAPTATION_REGEN_HEALTH_LOW_BURST = 3.5;
	public static double ADAPTATION_REGEN_HEALTH_MID_BURST = 2.0;

	public static double ADAPTATION_RESISTANCE_DECAY = 0.95;
	public static double ADAPTATION_RESISTANCE_HIGH_THRESHOLD = 70.0;
	public static double ADAPTATION_RESISTANCE_MID_THRESHOLD = 40.0;
	public static double ADAPTATION_RESISTANCE_LOW_THRESHOLD = 16.0;

	public static double ADAPTATION_SPEED_MIN_MULTIPLIER = -0.45;
	public static double ADAPTATION_SPEED_MAX_MULTIPLIER = 0.10;
	public static double ADAPTATION_SPEED_MAX_FALLBACK = 0.25;
	public static double ADAPTATION_SPEED_SCALING_TELEPORT = 1000.0;
	public static double ADAPTATION_SPEED_SCALING_FALLBACK = 600.0;

	public static double ROT_WALK_SPEED = 1.0;
	public static double ROT_RUN_SPEED = 1.45;

	public static double MELEE_PUNCH_DAMAGE = 18.0;
	public static double SONIC_BOOM_DMG = 38.0;
	public static double SONIC_BOOM_DMG_TOTEM = 65.0;
	public static double SONIC_BOOM_SPLASH_DAMAGE = 10.0;
	public static double SONIC_BOOM_SPLASH_TOTEM_DMG = 22.0;
	public static double SOLAR_BEAM_DMG_BASE = 8.0;
	public static double SOLAR_BEAM_DMG_BOOST = 18.0;
	public static double CRYO_BEAM_DMG_BASE = 8.0;
	public static double CRYO_BEAM_DMG_BOOST = 18.0;
	public static double MUTANT_DNA_GRAPPLE_DMG = 4.0;

	public static double COMBO_SEISMIC_SLAM_DMG = 35.0;
	public static double COMBO_OVERHEAD_SLAM_DMG = 40.0;

	public static double OVERHEAD_TOTAL_TICKS = 56.0;
	public static double OVERHEAD_PREP_THRESHOLD = 31.0;
	public static double OVERHEAD_STRIKE_TICK = 30.0;
	public static double OVERHEAD_Y_OFFSET_1 = 0.72;
	public static double OVERHEAD_Y_OFFSET_2 = 0.84;
	public static double OVERHEAD_STRIKE_FALL_VELOCITY = -10.0;
	public static double OVERHEAD_POST_STRIKE_FALL_VELOCITY = -3.2;

	public static double HEAVY_PUNCH_TOTAL_TICKS = 61.0;
	public static double HEAVY_PUNCH_STRIKE_TICK = 35.0;

	public static double UPPERCUT_TOTAL_TICKS = 80.0;
	public static double UPPERCUT_LAUNCH_TICK = 49.0;
	public static double UPPERCUT_DAMAGE = 30.0;

	public static double JUDGMENT_KICK_IMPACT_DIST = 0.7;
	public static double DIE_KICK_IMPACT_DIST = 0.6;
	public static double DIE_KICK_GROUND_OFFSET = 0.6;

	public static double COMBO_JUDGMENT_KICK_DMG = 50.0;
	public static double COMBO_DIE_RIDER_KICK_DMG = 65.0;
	public static double COMBO_GRAPPLE_SIPHON_DMG = 12.0;
	public static double COMBO_TK_SLAM_DMG = 15.0;
	public static double COMBO_NANITE_BLITZ_DMG = 8.0;
	public static double COMBO_THERMAL_SHOCK_DMG = 25.0;

	public static double BETA_VARIANCE_GATE_THRESHOLD = 0.08;
	public static double NN_LEARNING_RATE = 0.12;
	public static int NN_HIDDEN_NEURONS = 8;

	public static double PERSONALITY_DRIFT_RATE = 0.02;

	public static double SURPRISE_Z_SCORE_THRESHOLD = 2.5;
	public static double ANOMALY_MIN_SAMPLES = 5.0;

	public static int ROLE_AUCTION_DURATION_TICKS = 60;
	public static double ROLE_AUCTION_RANGE = 32.0;

	public static boolean ENABLE_ARMOR_RIP = true;
	public static double ARMOR_RIP_COOLDOWN = 600.0;
	public static double ARMOR_RIP_TICKS = 120.0;
	public static double ARMOR_RIP_TRIGGER_DISTANCE = 1.5;
	public static double ARMOR_RIP_MAX_DISTANCE = 16.0;
	public static double ARMOR_RIP_HOLD_DISTANCE = 0.8;
	public static double ARMOR_RIP_RIGHT_OFFSET = 0.55;
	public static double ARMOR_RIP_HEIGHT_OFFSET = 0.50;
	public static double ARMOR_RIP_CHANCE_REGULAR = 0.001;
	public static double ARMOR_RIP_CHANCE_INDESTRUCTIBLE = 0.02;
	public static double TOTEM_STEAL_TIME_MIN = 150.0;
	public static double TOTEM_STEAL_TIME_MAX = 1000.0;
	public static double BLOCK_MIN_TICKS = 20.0;
	public static double BLOCK_MAX_TICKS = 45.0;
	public static int CHOKE_MIN_HITS = 5;
	public static int CHOKE_MAX_HITS = 20;
	public static int CHOKE_TOTEM_MIN_HITS = 15;
	public static int CHOKE_TOTEM_MAX_HITS = 30;
	public static double CHOKE_DAMAGE = 2.0;
	public static int CHOKE_DAMAGE_INTERVAL = 15;
	public static int CHOKE_ARMOR_DURABILITY_LOSS = 100;
	public static int CHOKE_INDESTRUCTIBLE_DROP_INTERVAL = 30;

	private static final String K_WOODBOUND = "the_backwoods:woodbound_entities";

	private static final String K_TP_DODGE_CD = "sentinel_dodge_cd";
	private static final String K_TP_FLANK_CD = "sentinel_flank_cd";
	private static final String K_SOLAR_CD = "sentinel_solar_cd";
	private static final String K_SOLAR_CHARGE = "sentinel_solar_charge";
	private static final String K_ADAPT_MODE = "sentinel_adapt_mode";
	private static final String K_ADAPT_CD = "sentinel_adapt_cd";
	private static final String K_GRAPPLE_CD = "sentinel_grapple_cd";
	private static final String K_GRAPPLE_TICKS = "sentinel_grapple_ticks";
	private static final String K_CREATIVE_MSG = "creative_msg_fired";
	private static final String K_AGE = "Age";
	private static final String K_TK_CD = "sentinel_tk_cd";
	private static final String K_TK_TICKS = "sentinel_tk_ticks";

	private static double getDynamicGlobalCooldown(Entity entity) {
		if (getB(entity, "sentinel_totem_active") && !getB(entity, "sentinel_is_infinity_totem")) {
			return 20.0;
		}
		double phaseDuration = getD(entity, "sentinel_cd_phase_duration");
		if (phaseDuration <= 0) {
			phaseDuration = 900.0 + entity.level().getRandom().nextDouble() * 1500.0;
			putD(entity, "sentinel_cd_phase_duration", phaseDuration);
		}

		double rawCombatTicks = getD(entity, "sentinel_combat_ticks");
		double t = Math.min(rawCombatTicks / phaseDuration, 1.0);
		double ease = -(Math.cos(Math.PI * t) - 1.0) / 2.0;

		return 160.0 - (ease * 140.0);
	}

private static float lerpAngle(float pct, float start, float end) {
		float delta = Mth.wrapDegrees(end - start);
		return start + delta * pct;
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		if (event == null || event.getEntity() == null) return;
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	private static String formatTargetNames(List<? extends LivingEntity> entities) {
		Map<String, Integer> counts = new java.util.LinkedHashMap<>();
		for (LivingEntity e : entities) {
			String dName = e.getDisplayName().getString();
			counts.put(dName, counts.getOrDefault(dName, 0) + 1);
		}
		StringBuilder namesSb = new StringBuilder();
		int cIdx = 0;
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			if (cIdx > 0) namesSb.append(", ");
			if (entry.getValue() > 1) {
				namesSb.append(entry.getValue()).append(" ").append(entry.getKey());
			} else {
				namesSb.append(entry.getKey());
			}
			cIdx++;
		}
		return namesSb.toString();
	}

	public static double getAdaptationMultiplier(Entity entity) {
		double combatTicks = getD(entity, "sentinel_combat_ticks");
		double targetMaxTicks = 12000.0;
		if (entity instanceof Mob mob && mob.getTarget() instanceof LivingEntity target) {
			double hpRatio = target.getHealth() / Math.max(1.0f, target.getMaxHealth());
			if (hpRatio > 0.70) {
				targetMaxTicks = 18000.0;
			} else if (hpRatio < 0.25) {
				targetMaxTicks = 3600.0;
			} else if (hpRatio < 0.50) {
				targetMaxTicks = 6000.0;
			}
			if (target.isUsingItem()) {
				targetMaxTicks *= 0.6;
			}
		}
		double fraction = Math.min(1.0, combatTicks / Math.max(600.0, targetMaxTicks));
		double easeInQuart = fraction * fraction * fraction * fraction;
		return 1.0 + (easeInQuart * 49.0);
	}

	public static Player getGuardPlayer(LevelAccessor world, Entity self) {
		String targetUuid = getS(self, "master_guard_target_uuid");
		if (targetUuid.isEmpty() || !(world instanceof ServerLevel level)) return null;
		try {
			return level.getPlayerByUUID(UUID.fromString(targetUuid));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public static Player getFollowPlayer(LevelAccessor world, Entity self) {
		String targetUuid = getS(self, "master_follow_target_uuid");
		if (!(world instanceof ServerLevel level)) return null;
		if (targetUuid.isEmpty()) {
			List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, self.getBoundingBox().inflate(128.0), player -> player.isAlive());
			nearbyPlayers.sort(Comparator.comparingDouble(self::distanceToSqr));
			if (nearbyPlayers.isEmpty()) return null;
			Player nearestMaster = nearbyPlayers.get(0);
			targetUuid = nearestMaster.getUUID().toString();
			putS(self, "master_follow_target_uuid", targetUuid);
		}
		try {
			return level.getPlayerByUUID(UUID.fromString(targetUuid));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static Entity findGuardThreat(LevelAccessor world, Entity self, Player guardPlayer) {
		AABB box = guardPlayer.getBoundingBox().inflate(128.0);
		List<LivingEntity> threats = world.getEntitiesOfClass(LivingEntity.class, box, candidate -> {
			if (candidate == self || candidate == guardPlayer || !candidate.isAlive()) return false;
			if (candidate instanceof Player p) {
				String pName = p.getGameProfile().getName();
				if (pName.equals("honeypie_3301") || pName.equals("Dev") || isWhitelistedFriend(pName)) return false;
				return candidate.getLastHurtMob() == guardPlayer || guardPlayer.getLastHurtByMob() == candidate;
			}
			if (candidate instanceof Mob mob && mob.getTarget() == guardPlayer) return true;
			return candidate.getLastHurtByMob() == guardPlayer || candidate.getLastHurtMob() == guardPlayer
				|| guardPlayer.getLastHurtByMob() == candidate || guardPlayer.getLastHurtMob() == candidate;
		});

		if (threats.isEmpty()) {
			putD(self, "guard_target_lock_ticks", 0.0);
			return null;
		}

		threats.sort(java.util.Comparator.comparingDouble(candidate -> guardPlayer.distanceToSqr(candidate)));
		LivingEntity nearestThreat = threats.get(0);

		LivingEntity currentTarget = null;
		if (self instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
			currentTarget = mob.getTarget();
		}

		if (currentTarget == null || !currentTarget.isAlive() || currentTarget == guardPlayer) {
			putD(self, "guard_target_lock_ticks", 50.0);
			return nearestThreat;
		}

		if (currentTarget == nearestThreat) {
			double lockTicks = getD(self, "guard_target_lock_ticks");
			if (lockTicks <= 0) putD(self, "guard_target_lock_ticks", 40.0);
			return currentTarget;
		}

		boolean isActivelyAttacking = false;
		if (self instanceof RotEntity) {
			try {
				boolean leftPunch = self.getEntityData().get(RotEntity.DATA_is_heavy_left_punching);
				boolean rightPunch = self.getEntityData().get(RotEntity.DATA_is_heavy_right_punching);
				double slam = getD(self, "rot_slam_ticks");
				double overhead = getD(self, "rot_overhead_ticks");
				double solar = getD(self, "sentinel_solar_fire_ticks");
				double cryo = getD(self, "sentinel_cryo_fire_ticks");
				if (leftPunch || rightPunch || slam > 0 || overhead > 0 || solar > 0 || cryo > 0) {
					isActivelyAttacking = true;
				}
			} catch (Exception ignored) {}
		}

		double lockTicks = getD(self, "guard_target_lock_ticks");
		if (lockTicks > 0) {
			putD(self, "guard_target_lock_ticks", lockTicks - 1.0);
		}

		if (isActivelyAttacking) {
			return currentTarget;
		}

		boolean currentTargetStillThreat = threats.contains(currentTarget) || (currentTarget.distanceToSqr(guardPlayer) <= 64.0 * 64.0 && (currentTarget.getLastHurtMob() == guardPlayer || guardPlayer.getLastHurtByMob() == currentTarget));

		if (!currentTargetStillThreat || guardPlayer.distanceToSqr(currentTarget) > 48.0 * 48.0) {
			putD(self, "guard_target_lock_ticks", 50.0);
			return nearestThreat;
		}

		double nearestDistSqr = guardPlayer.distanceToSqr(nearestThreat);
		double currentDistSqr = guardPlayer.distanceToSqr(currentTarget);
		if (nearestDistSqr < 25.0 && currentDistSqr > 144.0) {
			putD(self, "guard_target_lock_ticks", 50.0);
			return nearestThreat;
		}

		if (lockTicks <= 0) {
			double nearestDist = Math.sqrt(nearestDistSqr);
			double currentDist = Math.sqrt(currentDistSqr);
			if (nearestDist < currentDist - 4.0) {
				putD(self, "guard_target_lock_ticks", 60.0);
				return nearestThreat;
			}
		}

		return currentTarget;
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (world != null && world.isClientSide()) {
			if (entity instanceof RotEntity rot) {
				try {
					int solarCharge = rot.getEntityData().get(RotEntity.DATA_sentinel_solar_charge_ticks);
					int cryoCharge = rot.getEntityData().get(RotEntity.DATA_sentinel_cryo_charge_ticks);
					boolean firingOrCharging = rot.getEntityData().get(RotEntity.DATA_is_laser_firing);
					boolean charging = solarCharge > 0 || cryoCharge > 0;
					boolean firing = firingOrCharging && !charging;

					boolean hadLaser = getB(entity, "client_had_laser");
					double closingTicks = getD(entity, "client_laser_closing_ticks");
					if (hadLaser && !firingOrCharging) {
						closingTicks = LASER_CLOSING_TICKS;
					} else if (closingTicks > 0) {
						closingTicks = Math.max(0, closingTicks - 1);
					}
					putB(entity, "client_had_laser", firingOrCharging);
					putD(entity, "client_laser_closing_ticks", closingTicks);
					boolean closing = closingTicks > 0 && !firingOrCharging;

					putB(entity, "laser_charging", charging);
					putB(entity, "is_laser_charging", charging);
					putB(entity, "laser_firing", firing);
					putB(entity, "is_laser_firing", firing);
					putB(entity, "laser_closing", closing);
					putB(entity, "is_laser_closing", closing);
					try {
						rot.getEntityData().set(RotEntity.DATA_is_laser_closing, closing);
					} catch (Exception e) {}
				} catch (Exception e) {}
				syncDataBool(rot, entity, RotEntity.DATA_is_left_punching, "is_left_punching");
				syncDataBool(rot, entity, RotEntity.DATA_is_right_punching, "is_right_punching");
				syncDataBool(rot, entity, RotEntity.DATA_is_heavy_left_punching, "is_heavy_left_punching");
				syncDataBool(rot, entity, RotEntity.DATA_is_heavy_right_punching, "is_heavy_right_punching");
				try {
					boolean airborneState = rot.getEntityData().get(RotEntity.DATA_is_airborne_state);
					putB(entity, "is_airborne_state", airborneState);
					putB(entity, "is_air_time", airborneState);
					putB(entity, "sentinel_is_airborne_state", airborneState);
				} catch (Exception e) {}
				syncDataBool(rot, entity, RotEntity.DATA_is_falling_heavy, "is_falling_heavy");
				syncDataBool(rot, entity, RotEntity.DATA_is_sonic_boom, "is_sonic_boom");
				syncDataBool(rot, entity, RotEntity.DATA_is_sonic_boom_large, "is_sonic_boom_large");
				syncDataBool(rot, entity, RotEntity.DATA_is_overhead_preparing, "is_overhead_preparing");
				syncDataBool(rot, entity, RotEntity.DATA_is_overhead, "is_overhead");
				syncDataBool(rot, entity, RotEntity.DATA_is_slam_charge, "is_slam_charge");
				syncDataBool(rot, entity, RotEntity.DATA_is_ground_crushing, "is_ground_crushing");
				syncDataBool(rot, entity, RotEntity.DATA_is_rider_charging, "is_rider_charging");
				syncDataBool(rot, entity, RotEntity.DATA_is_rider_kick, "is_rider_kick");
				syncDataBool(rot, entity, RotEntity.DATA_is_armor_ripping, "is_armor_ripping");
				syncDataBool(rot, entity, RotEntity.DATA_is_blocking, "is_blocking");
				syncDataBool(rot, entity, RotEntity.DATA_is_blocking_finish, "is_blocking_finish");
				syncDataBool(rot, entity, RotEntity.DATA_is_uppercutting, "is_uppercutting");
				syncDataBool(rot, entity, RotEntity.DATA_is_uppercut_charging_left, "is_uppercutting_left");
				syncDataBool(rot, entity, RotEntity.DATA_is_uppercut_charging_right, "is_uppercutting_right");
				syncDataBool(rot, entity, RotEntity.DATA_is_dropkick_charging, "is_dropkick_charging");
				syncDataBool(rot, entity, RotEntity.DATA_isLand, "isLand");
				syncDataBool(rot, entity, RotEntity.DATA_isLand2, "isLand2");
				syncDataBool(rot, entity, RotEntity.DATA_isDeath, "isDead");
			}
			return;
		}
		if (!(entity instanceof RotEntity)) return;
		if (world instanceof ServerLevel serverLevel) monitorWitherSkullOutcome(serverLevel, entity);
		removeIrradiatedEffect(entity);
		try {
			executeInternal(event, world, x, y, z, entity);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			syncNBTFlags(entity);
		}
	}

	private static void removeIrradiatedEffect(Entity entity) {
		if (!(entity instanceof LivingEntity living)) return;
		BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("alexscaves:irradiated"))
			.ifPresent(living::removeEffect);
	}

	private static void monitorWitherSkullOutcome(ServerLevel level, Entity rot) {
		double checkTicks = getD(rot, "sentinel_wither_skull_outcome_ticks");
		if (checkTicks <= 0.0) return;
		String targetUuid = getS(rot, "sentinel_wither_skull_outcome_target");
		Entity targetEntity;
		try {
			targetEntity = level.getEntity(UUID.fromString(targetUuid));
		} catch (IllegalArgumentException exception) {
			putD(rot, "sentinel_wither_skull_outcome_ticks", 0.0);
			return;
		}
		if (targetEntity instanceof LivingEntity target && target.isAlive()) {
			MobEffectInstance wither = target.getEffect(MobEffects.WITHER);
			double baselineDuration = getD(rot, "sentinel_wither_skull_outcome_baseline");
			if (wither != null && wither.getDuration() > baselineDuration) {
				putD(rot, "sentinel_wither_skull_outcome_ticks", 0.0);
				return;
			}
		}
		checkTicks--;
		putD(rot, "sentinel_wither_skull_outcome_ticks", checkTicks);
		if (checkTicks <= 0.0 && targetEntity instanceof LivingEntity target && target.isAlive()) {
			recordWitherSkullFailure(rot, target);
		}
	}

	private static void executeInternal(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		boolean isDying = false;
		try {
			isDying = entity.getEntityData().get(net.mcreator.thebackwoods.entity.RotEntity.DATA_isDeath);
		} catch (Exception e) {}
		if (!isDying) {
			isDying = getB(entity, "rot_death_sequence_active");
		}
		if (!isDying && entity instanceof LivingEntity living && (living.getHealth() <= 20.0F || living.isDeadOrDying())) {
			isDying = true;
			try {
				entity.getEntityData().set(net.mcreator.thebackwoods.entity.RotEntity.DATA_isDeath, true);
			} catch (Exception e) {}
			putB(entity, "rot_death_sequence_active", true);
			putD(entity, "rot_death_ticks", 240.0);
			putD(entity, "rot_death_start_x", entity.getX());
			putD(entity, "rot_death_start_y", entity.getY());
			putD(entity, "rot_death_start_z", entity.getZ());

			double randomTargetHeight = 12.0 + (Math.random() * 4.0);
			putD(entity, "rot_death_target_height", randomTargetHeight);

			double randomHoleSize = 8.0 + (Math.random() * 0.5);
			putD(entity, "rot_death_hole_size", randomHoleSize);

			entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			entity.hasImpulse = false;
			entity.noPhysics = true;
		}

		if (!isDying && entity != null) {
			monitorAndLogAllActions(world, entity);
		}

		if (isDying) {
			try {
				entity.getEntityData().set(net.mcreator.thebackwoods.entity.RotEntity.DATA_isDeath, true);
			} catch (Exception e) {}
			if (entity instanceof LivingEntity living) {
				living.setHealth(1.0F);
				living.deathTime = 0;
				living.setInvulnerable(true);
				if (living instanceof Mob mob) {
					mob.setNoAi(true);
					mob.getNavigation().stop();
					mob.setTarget(null);
					mob.setLastHurtByMob(null);
				}
			}
			entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			entity.hasImpulse = false;
			entity.fallDistance = 0.0F;
			entity.noPhysics = true;

			double deathTicks = getRotPersistentDouble(entity, "rot_death_ticks", 240.0);
			if (deathTicks <= 0) {
				deathTicks = 240.0;
				putD(entity, "rot_death_ticks", 240.0);
			}

			cancelActiveCombosAndAbilities(entity);
			cleanupCombatFlags(entity);
			if (entity instanceof net.minecraft.world.entity.Mob mob) {
				mob.setTarget(null);
				mob.setLastHurtByMob(null);
			}

			double startX = getRotPersistentDouble(entity, "rot_death_start_x", entity.getX());
			if (startX == 0.0) {
				startX = entity.getX();
				putD(entity, "rot_death_start_x", startX);
			}
			double startY = getRotPersistentDouble(entity, "rot_death_start_y", entity.getY());
			if (startY == 0.0) {
				startY = entity.getY();
				putD(entity, "rot_death_start_y", startY);
			}
			double startZ = getRotPersistentDouble(entity, "rot_death_start_z", entity.getZ());
			if (startZ == 0.0) {
				startZ = entity.getZ();
				putD(entity, "rot_death_start_z", startZ);
			}

			double targetHeight = getRotPersistentDouble(entity, "rot_death_target_height", 14.0);
			if (targetHeight < 10.0 || targetHeight > 16.0) {
				targetHeight = 12.0 + (Math.random() * 4.0);
				putD(entity, "rot_death_target_height", targetHeight);
			}

			double levDuration = 160.0;
			double elapsedLevTicks = Math.min(levDuration, 240.0 - deathTicks);
			if (elapsedLevTicks >= 0.0) {
				double levProgress = Math.min(1.0, elapsedLevTicks / levDuration);
				double smoothEase = levProgress * levProgress * (3.0 - 2.0 * levProgress);
				double desiredY = Math.min(startY + 16.0, startY + (targetHeight * smoothEase));

				if (world instanceof net.minecraft.world.level.Level lvl) {
					net.minecraft.core.BlockPos abovePos = net.minecraft.core.BlockPos.containing(startX, desiredY + entity.getBbHeight() + 0.2, startZ);
					if (lvl.getBlockState(abovePos).isSolid()) {
						desiredY = Math.min(desiredY, entity.getY());
					}
				}

				entity.setPos(startX, desiredY, startZ);
				setMotion(entity, 0.0, 0.0, 0.0);
			}

			if (deathTicks <= 140.0) {
				if (!getB(entity, "rot_death_hole_spawned")) {
					putB(entity, "rot_death_hole_spawned", true);
					if (world instanceof net.minecraft.world.level.Level lvl && !lvl.isClientSide()) {
						double holeSize = getRotPersistentDouble(entity, "rot_death_hole_size", 8.0);
						if (holeSize < 5.0) holeSize = 8.0 + (Math.random() * 0.5);

						double torsoY = entity.getY() + (entity.getBbHeight() * 0.5);
						BlackHole.spawnRotDeathHole(lvl, entity.getX(), torsoY, entity.getZ(), (float) holeSize, 7.0f);
					}
				}
			}

			if (deathTicks <= 1.0) {
				putD(entity, "rot_death_ticks", 0.0);
				if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
					double torsoY = entity.getY() + (entity.getBbHeight() * 0.5);
					serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, entity.getX(), torsoY, entity.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
					serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, entity.getX(), torsoY, entity.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
				}
				if (!entity.level().isClientSide()) {
					entity.discard();
				}
				return;
			} else {
				putD(entity, "rot_death_ticks", deathTicks - 1.0);
			}
			return;
		}

		double heat = getD(entity, "sentinel_laser_heat");
		if (heat > 0) {
			putD(entity, "sentinel_laser_heat", Math.max(0.0, heat - 0.25));
		}

		if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
			double uppercutLaunchTicks = getD(living, "sentinel_uppercut_launch_ticks");
			if (uppercutLaunchTicks > 0) {
				putD(living, "sentinel_uppercut_launch_ticks", uppercutLaunchTicks - 1);
			}
			if (living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) != null) {
				living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT).setBaseValue(1.25D);
			}
			BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("legendary_monsters:soul_fracture")).ifPresent(soulFractureHolder -> {
				if (living.hasEffect(soulFractureHolder)) {
					living.removeEffect(soulFractureHolder);
					if (world instanceof net.minecraft.server.level.ServerLevel level) {
						level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 15, 0.3, 0.3, 0.3, 0.03);
						level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
					}
				}
			});

			if (getB(living, "unlocked_solar_beam")) {
				living.clearFire();

				BlockPos feetPos = living.blockPosition();
				BlockPos belowPos = feetPos.below();
				BlockState feetState = living.level().getBlockState(feetPos);
				BlockState belowState = living.level().getBlockState(belowPos);
				boolean inLava = feetState.getFluidState().is(FluidTags.LAVA);
				boolean onLava = belowState.getFluidState().is(FluidTags.LAVA);

				if (inLava || onLava) {
					living.setOnGround(true);
					living.fallDistance = 0.0F;
					Vec3 curDelta = living.getDeltaMovement();
					if (inLava) {
						living.setDeltaMovement(curDelta.x * 1.15, Math.max(0.12, curDelta.y + 0.15), curDelta.z * 1.15);
					} else if (onLava && curDelta.y < 0) {
						living.setDeltaMovement(curDelta.x * 1.05, 0.0, curDelta.z * 1.05);
					}
				}
			}
			if (getB(living, "unlocked_cryo_beam")) {
				living.setTicksFrozen(0);

				BlockPos feetPos = living.blockPosition();
				BlockPos belowPos = feetPos.below();
				BlockState feetState = living.level().getBlockState(feetPos);
				BlockState belowState = living.level().getBlockState(belowPos);
				boolean inPowderSnow = feetState.is(Blocks.POWDER_SNOW);
				boolean onPowderSnow = belowState.is(Blocks.POWDER_SNOW);

				if (inPowderSnow || onPowderSnow) {
					living.setOnGround(true);
					living.fallDistance = 0.0F;
					Vec3 curDelta = living.getDeltaMovement();
					if (inPowderSnow) {
						living.setDeltaMovement(curDelta.x * 1.25, Math.max(0.1, curDelta.y + 0.12), curDelta.z * 1.25);
					} else if (onPowderSnow && curDelta.y < 0) {
						living.setDeltaMovement(curDelta.x, 0.0, curDelta.z);
					}
				}
			}

			boolean isExecutingAirAbility = getD(living, "sentinel_slam_phase") > 0
				|| getD(living, "sentinel_judgment_ticks") > 0
				|| getD(living, "sentinel_die_kick_phase") > 0
				|| getD(living, "rot_overhead_ticks") > 0
				|| getD(living, "sentinel_uppercut_launch_ticks") > 0
				|| getD(living, "sentinel_cc1_stage") > 0
				|| getD(living, "sentinel_cc2_stage") > 0
				|| getD(living, "sentinel_cc3_stage") > 0
				|| getD(living, "sentinel_cc4_stage") > 0
				|| getD(living, "sentinel_cc5_stage") > 0
				|| getB(living, "sentinel_is_air_maneuvering")
				|| getB(living, "is_uppercutting")
				|| getB(living, "debug_force_overhead")
				|| getB(living, "debug_force_rider");

			if (!isExecutingAirAbility && !living.isInWater() && !living.isInLava()) {
				boolean hasLevitation = living.hasEffect(MobEffects.LEVITATION);
				boolean hasInvoluntaryLift = !living.onGround() && living.getDeltaMovement().y() > 0.08;
				boolean isPermanentlyAdapted = getB(living, "adapted_gravitational_mass");

				if (hasLevitation || hasInvoluntaryLift) {
					double floatAdaptTicks = getD(living, "rot_involuntary_float_ticks") + 1;
					putD(living, "rot_involuntary_float_ticks", floatAdaptTicks);

					if (isPermanentlyAdapted) {
						if (hasLevitation) {
							living.removeEffect(MobEffects.LEVITATION);
						}
						Vec3 curDelta = living.getDeltaMovement();
						setMotion(living, curDelta.x() * 0.94, -0.90, curDelta.z() * 0.94);
						putB(living, "rot_forced_gravity_active", true);
					} else {
						if (floatAdaptTicks >= 25) {
							double progress = Math.min(1.0, (floatAdaptTicks - 25) / 25.0);
							Vec3 curDelta = living.getDeltaMovement();
							double pullY = -0.15 - (0.75 * progress);
							setMotion(living, curDelta.x() * 0.95, Math.min(curDelta.y(), pullY), curDelta.z() * 0.95);
							putB(living, "rot_forced_gravity_active", true);

							if (floatAdaptTicks >= 45) {
								if (hasLevitation) living.removeEffect(MobEffects.LEVITATION);
								putB(living, "adapted_gravitational_mass", true);
								announceLearnedAbility(living);
							}
						}
					}
				} else {
					putD(living, "rot_involuntary_float_ticks", Math.max(0, getD(living, "rot_involuntary_float_ticks") - 2));
				}
			} else {
				putD(living, "rot_involuntary_float_ticks", 0);
			}

			if (living.onGround()) {
				if (getB(living, "rot_forced_gravity_active")) {
					putB(living, "rot_forced_gravity_active", false);
					putD(living, "rot_involuntary_float_ticks", 0);
					if (world instanceof ServerLevel sLevel) {
						sLevel.sendParticles(ParticleTypes.CRIT, living.getX(), living.getY() + 0.1, living.getZ(), 16, 0.4, 0.1, 0.4, 0.15);
						BlockState groundState = living.level().getBlockState(living.blockPosition().below());
						if (!groundState.isAir()) {
							try {
								sLevel.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, groundState), living.getX(), living.getY() + 0.1, living.getZ(), 12, 0.3, 0.1, 0.3, 0.05);
							} catch (Exception ignored) {}
						}
						playHostileSound(sLevel, living, "entity.wind_charge.wind_burst", 1.2F, 0.6F);

						AABB miniAABB = living.getBoundingBox().inflate(3.5, 1.5, 3.5);
						List<LivingEntity> nearbyVictims = sLevel.getEntitiesOfClass(LivingEntity.class, miniAABB, e -> e != living && !isWoodboundEntity(e, living));
						for (LivingEntity v : nearbyVictims) {
							dealTrueDamageToBosses(v, getBackwoodsDamage(sLevel, "rot_seismic_slam", living), 8.0F * (float) getAdaptationMultiplier(living));
							v.setDeltaMovement(v.getDeltaMovement().x() * 0.5, 0.35, v.getDeltaMovement().z() * 0.5);
						}
					}
				}
			}
		}

			if (entity instanceof net.minecraft.world.entity.Mob mob) {
				BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("mowziesmobs:frozen")).ifPresent(frozenHolder -> {
					if (mob.hasEffect(frozenHolder)) {
						if (getB(mob, "unlocked_cryo_beam")) {
							mob.removeEffect(frozenHolder);
							mob.setNoAi(false);
							return;
						}
						int frozenTicks = getI(mob, "sentinel_frozen_ticks");
						int breakTicks = getI(mob, "sentinel_frozen_break_ticks");
						if (breakTicks == 0) {
							breakTicks = 60 + mob.getRandom().nextInt(141);
							putI(mob, "sentinel_frozen_break_ticks", breakTicks);
						}

						frozenTicks++;
						putI(mob, "sentinel_frozen_ticks", frozenTicks);

						if (world instanceof ServerLevel level) {
							double progress = (double) frozenTicks / breakTicks;
							if (progress > 0.3) {
								int frequency = (int) (20 * (1.0 - progress)) + 2;
								if (frozenTicks % frequency == 0) {
									float volume = (float) (0.2 + 0.8 * progress);
									float pitch = (float) (0.8 + 0.4 * progress);
									playHostileSound(level, entity, "block.fire.extinguish", volume, pitch);
									level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 1.0 + mob.getRandom().nextDouble(), entity.getZ(), (int)(5 * progress) + 1, 0.4, 0.4, 0.4, 0.02);
								}
							}
						}

						if (frozenTicks >= breakTicks) {
							mob.removeEffect(frozenHolder);
							mob.setNoAi(false);
							putI(mob, "sentinel_frozen_ticks", 0);
							putI(mob, "sentinel_frozen_break_ticks", 0);
							if (world instanceof ServerLevel level) {
								playHostileSound(level, entity, "block.fire.extinguish", 1.0F, 1.0F);
								level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 1.5, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
								level.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + 1.5, entity.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
							}
						}
					} else {
						putI(mob, "sentinel_frozen_ticks", 0);
						putI(mob, "sentinel_frozen_break_ticks", 0);
					}
				});
				if (mob.isNoAi() && BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("mowziesmobs:frozen")).map(frozenHolder -> !mob.hasEffect(frozenHolder)).orElse(true)) {
					if (entity.tickCount > 20 && !getB(entity, "mapmaker_noai")) {
						mob.setNoAi(false);
					}
				}
			}

			if (!getB(entity, "sentinel_spawn_initialized")) {
			putB(entity, "sentinel_spawn_initialized", true);
			if (getB(entity, "sentinel_should_scan")) {
				if (getD(entity, "sentinel_slam_phase") == 0) {
					putD(entity, "sentinel_scanning_ticks", 60);
					putD(entity, "sentinel_scan_max_ticks", 60.0);
					putD(entity, "sentinel_scanning_base_yaw", entity.getYRot());
				}
			}
		}

		if (handleScanningState(entity)) {
			return;
		}

		if (getB(entity, "sentinel_totem_active") && entity.tickCount % 4 == 0) {
			if (world instanceof ServerLevel level) {
				for (int i = 0; i < 3; i++) {
					double theta = Math.random() * Math.PI * 2;
					double phi = Math.acos(Math.random() * 2 - 1);
					double radius = 1.8;
					double px = Math.sin(phi) * Math.cos(theta) * radius;
					double py = Math.sin(phi) * Math.sin(theta) * radius + 1.2;
					double pz = Math.cos(phi) * radius;
					double vx = -px * 0.08;
					double vy = -(py - 1.2) * 0.08;
					double vz = -pz * 0.08;
					level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX() + px, entity.getY() + py, entity.getZ() + pz, 0, vx, vy, vz, 0.4);
				}
			}
		}

		if (entity instanceof LivingEntity living) {
			if (living.getLastHurtByMob() != null) {
				Entity attacker = living.getLastHurtByMob();
				if (BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString().equals("spore:scent")) {
					living.setLastHurtByMob(null);
				} else if (!shouldIgnoreCombatFilter(entity) && !shouldIgnoreCombatFilter(attacker) && (attacker instanceof RotEntity || attacker.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND))))) {
					living.setLastHurtByMob(null);
				}
			}
			if (living.isInWater() || living.isInLava()) {
				boolean isSuperheating = getD(living, "rot_superheat_charging") > 0 || getD(living, "rot_superheat_active") > 0;
				if (!isSuperheating && living instanceof Mob mob && mob.getTarget() != null) {
					Entity target = mob.getTarget();
					Vec3 targetEye = target.getEyePosition();
					Vec3 livingEye = living.getEyePosition();
					Vec3 dir = targetEye.subtract(livingEye);
					double dist = dir.length();

					if (dist > 0.001) {
						Vec3 look = dir.normalize();
						Vec3 mv = living.getDeltaMovement();
						double horizDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);

						double swimSpeedHoriz = 0.04;
						double swimSpeedVert = 0.04;

						double newX = mv.x * 0.82 + look.x * swimSpeedHoriz;
						double newZ = mv.z * 0.82 + look.z * swimSpeedHoriz;
						double newY = mv.y * 0.82 + look.y * swimSpeedVert;

						if (dir.y > 0.8) {
							living.setJumping(true);
						} else {
							living.setJumping(false);
						}

						if (horizDist < 0.6) {
							newX = mv.x * 0.5;
							newZ = mv.z * 0.5;
						}

						living.setDeltaMovement(newX, newY, newZ);
					}
				} else {
					living.setJumping(false);
				}
			}
		}
		if (entity instanceof Mob mob) {
			if (mob.getTarget() != null) {
				Entity t = mob.getTarget();
				boolean isRetaliation = (mob.getLastHurtByMob() == t || (t instanceof Mob tm && tm.getTarget() == mob));
				if (!shouldIgnoreCombatFilter(entity) && !shouldIgnoreCombatFilter(t) && (t instanceof RotEntity || t.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND))))) {
					mob.setTarget(null);
				} else if (!isRetaliation && !shouldIgnoreCombatFilter(entity) && !shouldIgnoreCombatFilter(t) && (t instanceof Villager || t instanceof AmbientCreature || t instanceof Animal || t instanceof Slime || t instanceof net.minecraft.world.entity.animal.WaterAnimal)) {
					mob.setTarget(null);
				}
			}
		}

		double landingTicksBefore = getD(entity, "sentinel_landing_ticks");

		tickCooldowns(entity);

		if (ENABLE_PHASE_SHIFT) {
			double phaseTicks = getD(entity, "rot_phase_ticks");
			if (phaseTicks > 0) {
				putB(entity, "rot_phase_shifting", true);
				if (entity.tickCount % 20 == 0) {
					double currentMastery = getD(entity, "rot_phase_mastery");
					if (currentMastery < 1.0) {
						putD(entity, "rot_phase_mastery", Math.min(1.0, currentMastery + 0.010));
					}
				}
			} else if (getB(entity, "rot_phase_shifting")) {
				putB(entity, "rot_phase_shifting", false);
				entity.noPhysics = false;
				putD(entity, "rot_phase_cooldown", 200.0);
			}
		} else if (getB(entity, "rot_phase_shifting")) {
			putB(entity, "rot_phase_shifting", false);
			entity.noPhysics = false;
		}

		if (ENABLE_BLOCKING) {
			if (getB(entity, "debug_force_block")) {
				putB(entity, "debug_force_block", false);
				double minTicks = BLOCK_MIN_TICKS;
				double maxTicks = BLOCK_MAX_TICKS;
				double blockTicks = minTicks + Math.random() * (maxTicks - minTicks);
				putD(entity, "rot_block_active_ticks", blockTicks);
				putB(entity, "is_blocking", true);
			}

			double blockActiveTicks = getD(entity, "rot_block_active_ticks");
			if (blockActiveTicks > 0) {
				blockActiveTicks--;
				putD(entity, "rot_block_active_ticks", blockActiveTicks);
				putB(entity, "is_blocking", true);
				if (blockActiveTicks == 0) {
					putB(entity, "is_blocking", false);
					putB(entity, "is_blocking_finish", true);
					putD(entity, "rot_block_finish_ticks", 5);
					putD(entity, "rot_block_cooldown", 300);

					if (Math.random() < 0.5) {
						Entity followTarget = acquireTarget(world, entity, entity.getX(), entity.getY(), entity.getZ());
						if (followTarget instanceof LivingEntity targetLiv && targetLiv.isAlive()) {
							double moveRand = Math.random();
							if (moveRand < 0.33) {
								putD(entity, "sentinel_slam_phase", 1);
								putD(entity, "sentinel_slam_ticks", 22);
								putD(entity, "rot_overhead_ticks", 0);
								setMotion(entity, 0.0, 1.9, 0.0);
								if (world instanceof ServerLevel level) {
									playHostileSound(level, entity, "entity.iron_golem.attack", 1.5F, 0.8F);
									level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
								}
							} else if (moveRand < 0.66) {
								if (entity.distanceTo(targetLiv) >= 7.5) {
									putD(entity, "sentinel_judgment_ticks", 60);
									clearDoubles(entity, "sentinel_slam_phase", "rot_overhead_ticks");
									putD(entity, "sentinel_die_kick_phase", 0);
									setMotion(entity, 0.0, 0.0, 0.0);
								} else {
									executeMinosHeavyPunchBlink(world, entity, targetLiv, true);
								}
							} else {
								putD(entity, "rot_overhead_ticks", OVERHEAD_TOTAL_TICKS);
								putD(entity, "sentinel_slam_phase", 0);
								putS(entity, "overhead_target_uuid", targetLiv.getUUID().toString());
							}
						}
					}
				}
			} else {
				if (getB(entity, "is_blocking")) {
					putB(entity, "is_blocking", false);
				}
			}
		} else {
			if (getB(entity, "is_blocking")) {
				putB(entity, "is_blocking", false);
				putD(entity, "rot_block_active_ticks", 0.0);
			}
			putB(entity, "debug_force_block", false);
		}

		double blockFinishTicks = getD(entity, "rot_block_finish_ticks");
		if (blockFinishTicks > 0) {
			blockFinishTicks--;
			putD(entity, "rot_block_finish_ticks", blockFinishTicks);
			if (blockFinishTicks == 0) {
				putB(entity, "is_blocking_finish", false);
			}
		}

		double dpsTickCounter = getD(entity, "rot_dps_tick_counter") + 1;
		if (dpsTickCounter >= 20) {
			dpsTickCounter = 0;
			double sec0 = getD(entity, "rot_dmg_this_sec");
			double sec1 = getD(entity, "rot_dmg_sec_0");
			double sec2 = getD(entity, "rot_dmg_sec_1");
			double sec3 = getD(entity, "rot_dmg_sec_2");
			double sec4 = getD(entity, "rot_dmg_sec_3");

			putD(entity, "rot_dmg_sec_4", sec4);
			putD(entity, "rot_dmg_sec_3", sec3);
			putD(entity, "rot_dmg_sec_2", sec2);
			putD(entity, "rot_dmg_sec_1", sec1);
			putD(entity, "rot_dmg_sec_0", sec0);
			putD(entity, "rot_dmg_this_sec", 0);
		}
		putD(entity, "rot_dps_tick_counter", dpsTickCounter);

		double landingTicksAfter = getD(entity, "sentinel_landing_ticks");
		if (landingTicksBefore > 0 && landingTicksAfter <= 0 && getB(entity, "sentinel_immune_slam_landing_active")) {
			putB(entity, "sentinel_immune_slam_landing_active", false);
			if (getB(entity, "sentinel_should_scan")) {
				int summons = getI(entity, "sentinel_spore_summons_at_birth");
				double scanTicks = Math.max(15.0, 60.0 - (summons - 1) * 15.0);
				putD(entity, "sentinel_scanning_ticks", scanTicks);
				putD(entity, "sentinel_scan_max_ticks", scanTicks);
				putD(entity, "sentinel_scanning_base_yaw", entity.getYRot());
			}
		}

		double rotLandTimer = getD(entity, "rot_land_timer");
		if (rotLandTimer > 0) {
			rotLandTimer--;
			putD(entity, "rot_land_timer", rotLandTimer);
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
				mob.setSpeed(0.0F);
				mob.xxa = 0.0F;
				mob.zza = 0.0F;
			}
			setMotion(entity, 0, Math.min(0, entity.getDeltaMovement().y()), 0);
			if (rotLandTimer <= 0) {
				if (entity instanceof RotEntity rot) {
					rot.getEntityData().set(RotEntity.DATA_isLand, false);
					rot.getEntityData().set(RotEntity.DATA_isLand2, false);
				}
				putB(entity, "isLand", false);
				putB(entity, "isLand2", false);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double shockwaveStage = getD(entity, "sentinel_shockwave_stage");
		if (shockwaveStage > 0) {
			double originX = getD(entity, "sentinel_shockwave_x");
			double originY = getD(entity, "sentinel_shockwave_y");
			double originZ = getD(entity, "sentinel_shockwave_z");
			double radius = shockwaveStage * 2.2;
			boolean isVertical = getB(entity, "sentinel_shockwave_vertical");

			if (world instanceof ServerLevel level) {
				int particleCount = Math.max(1, (int) (2 * Math.PI * radius * 7.5 * Mth.clamp(PARTICLE_QUALITY, 0.1, 1.0)));
				net.minecraft.world.level.block.state.BlockState floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(originX, originY - 0.5, originZ));
				if (floorState.isAir()) {
					floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(originX, originY - 1.5, originZ));
				}
				if (floorState.isAir()) {
					floorState = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
				}
				net.minecraft.core.particles.BlockParticleOption dustPillarOptions = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, floorState);
				net.minecraft.core.particles.ParticleType<?> _tsdType = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse("trial_spawner_detection"));
				net.minecraft.core.particles.ParticleOptions trialSpawnerDetection = _tsdType instanceof net.minecraft.core.particles.ParticleOptions _tsdOpt ? _tsdOpt : net.minecraft.core.particles.ParticleTypes.EFFECT;

				for (int i = 0; i < particleCount; i++) {
					double angle = (2 * Math.PI / particleCount) * i;
					double cos = Math.cos(angle);
					double sin = Math.sin(angle);

					if (isVertical) {
						double yaw = entity.getPersistentData().contains("sentinel_shockwave_yaw")
							? getD(entity, "sentinel_shockwave_yaw")
							: entity.getYRot();
						double yawRad = Math.toRadians(yaw);
						double cosYaw = Math.cos(yawRad);
						double sinYaw = Math.sin(yawRad);

						double r = radius + (Math.random() - 0.5) * 0.8;
						double px = originX + cos * r * sinYaw;
						double py = originY + sin * r;
						double pz = originZ - cos * r * cosYaw;

						double spread = 0.85 + Math.random() * 0.3;
						double vx = cos * sinYaw * (2.2 * spread);
						double vy = sin * (2.2 * spread);
						double vz = -cos * cosYaw * (2.2 * spread);

						level.sendParticles(dustPillarOptions, px, py, pz, 0, vx, vy, vz, 1.0);
						level.sendParticles(trialSpawnerDetection, px, py, pz, 0, vx * 0.5, vy * 0.5, vz * 0.5, 1.0);
					} else {
						double r = radius + (Math.random() - 0.5) * 0.8;
						double px = originX + cos * r;
						double pz = originZ + sin * r;

						double spread = 0.85 + Math.random() * 0.3;
						double vx = cos * (2.2 * spread);
						double vz = sin * (2.2 * spread);

						level.sendParticles(dustPillarOptions, px, originY + 0.15, pz, 0, vx, 0.02, vz, 1.0);
						level.sendParticles(trialSpawnerDetection, px, originY + 0.15, pz, 0, vx * 0.5, 0.02, vz * 0.5, 1.0);

						if (i % 2 == 0) {
							double rCloud = radius + (Math.random() - 0.5) * 0.8;
							double pxCloud = originX + cos * rCloud;
							double pzCloud = originZ + sin * rCloud;

							double spreadCloud = 0.45 + Math.random() * 0.2;
							double vxCloud = cos * (2.2 * spreadCloud);
							double vzCloud = sin * (2.2 * spreadCloud);

							level.sendParticles(dustPillarOptions, pxCloud, originY + 0.1, pzCloud, 0, vxCloud, 0.02, vzCloud, 1.0);
						}
					}
				}
				if (shockwaveStage % 2 == 1) {
					playHostileSound(level, originX, originY, originZ, "entity.wind_charge.wind_burst", 1.4F, 0.7F - ((float) shockwaveStage * 0.03F));
				}

				AABB checkArea = new AABB(originX - radius - 1.5, originY - radius - 1.5, originZ - radius - 1.5, originX + radius + 1.5, originY + radius + 1.5, originZ + radius + 1.5);

				java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, checkArea, e -> e != entity && !isWoodboundEntity(e, entity));
				for (LivingEntity victim : targets) {
					double dist = Math.sqrt(victim.distanceToSqr(originX, originY, originZ));
					if (Math.abs(dist - radius) <= 1.5) {
						float waveDmg = 12.0F * (float) getAdaptationMultiplier(entity);
						if (victim instanceof Player p && p.isBlocking()) {
							disablePlayerShield(level, victim, waveDmg * 2.2, 100);
						}
						dealTrueDamageToBosses(victim, getBackwoodsDamage(level, "rot_seismic_slam", entity), waveDmg);
						double dx = victim.getX() - originX;
						double dz = victim.getZ() - originZ;
						double hLen = Math.sqrt(dx * dx + dz * dz);
						Vec3 push;
						if (hLen > 1E-4) {
							push = new Vec3(dx / hLen, 0.0, dz / hLen);
						} else {
							double angle = Math.random() * Math.PI * 2.0;
							push = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
						}
						double pushMult = 1.15;
						setMotion(victim, push.x * pushMult, 0.65, push.z * pushMult);
						level.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 0.5, victim.getZ(), 1, 0, 0, 0, 0);
					}
				}
			}

			if (shockwaveStage >= 8) {
				putD(entity, "sentinel_shockwave_stage", 0);
				putB(entity, "sentinel_shockwave_vertical", false);
			} else {
				putD(entity, "sentinel_shockwave_stage", shockwaveStage + 1);
			}
		}

		double landingTicks = getD(entity, "sentinel_landing_ticks");
		if (landingTicks > 0) {
			entity.setDeltaMovement(0, -0.05, 0);
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (entity instanceof LivingEntity living) {
				living.setYBodyRot(living.getYRot());
			}
			double cc1 = getD(entity, "sentinel_cc1_stage");
			double cc2 = getD(entity, "sentinel_cc2_stage");
			double cc3 = getD(entity, "sentinel_cc3_stage");
			double cc4 = getD(entity, "sentinel_cc4_stage");
			double cc5 = getD(entity, "sentinel_cc5_stage");
			if (cc1 <= 0 && cc2 <= 0 && cc3 <= 0 && cc4 <= 0 && cc5 <= 0) {
				return;
			}
		}

		if (entity instanceof LivingEntity living) {
			boolean inColdBiome = false;
			if (living.tickCount % 40 == 0 || !living.getPersistentData().contains("sentinel_cached_in_cold_biome")) {
				try {
					inColdBiome = living.level().getBiome(living.blockPosition()).unwrapKey().map(key -> {
						String path = key.location().getPath().toLowerCase(java.util.Locale.ROOT);
						return path.contains("snow") || path.contains("frozen") || path.contains("ice") || path.contains("cold");
					}).orElse(false);
					putB(living, "sentinel_cached_in_cold_biome", inColdBiome);
				} catch (Exception ignored) {}
			} else {
				inColdBiome = getB(living, "sentinel_cached_in_cold_biome");
			}
			if (inColdBiome) {
				double coldTime = getD(living, "sentinel_time_in_cold_biome");
				putD(living, "sentinel_time_in_cold_biome", coldTime + 1.0);
			}
			if (living.level().dimension() == net.minecraft.world.level.Level.NETHER) {
				double netherTime = getD(living, "sentinel_time_in_nether");
				putD(living, "sentinel_time_in_nether", netherTime + 1.0);
			}
		}

		Entity combatTarget = acquireTarget(world, entity, x, y, z);
		if (combatTarget == null || !combatTarget.isAlive() || combatTarget.isRemoved()) {
			cleanupCombatFlags(entity);
		}

		boolean isRipping = getD(entity, "rot_armor_rip_ticks") > 0 || getB(entity, "is_armor_ripping");
		boolean isBlocking = getD(entity, "rot_block_active_ticks") > 0 || getB(entity, "is_blocking");
		boolean isTotemInspecting = getD(entity, "sentinel_totem_inspect_ticks") > 0;

		if (isRipping) {
			putB(entity, "is_blocking", false);
			clearDoubles(entity, "rot_block_active_ticks", "sentinel_totem_inspect_ticks");
		} else if (isBlocking) {
			putD(entity, "rot_armor_rip_ticks", 0);
			putB(entity, "is_armor_ripping", false);
			putD(entity, "sentinel_totem_inspect_ticks", 0);
		} else if (isTotemInspecting) {
			putB(entity, "is_blocking", false);
			clearDoubles(entity, "rot_block_active_ticks", "rot_armor_rip_ticks");
			putB(entity, "is_armor_ripping", false);
		}

		boolean inCombat = combatTarget != null || (entity instanceof LivingEntity living ? living.getLastHurtByMob() != null : false) || getD(entity, "sentinel_recent_damage") > 0.0 || getB(entity, "is_blocking") || !"NONE".equals(getRotPersistentString(entity, "sentinel_predicted_threat_level", "NONE"));
		handleAdaptationScaling(entity, inCombat);

		if (combatTarget instanceof LivingEntity livTarget) {
			UniversalCombatPredictionEngine.tickPrediction(world, entity, livTarget);
		} else {
			UniversalCombatPredictionEngine.clearPrediction(entity);
		}

		if (combatTarget instanceof LivingEntity livTarget) {
			boolean isFlying = livTarget.isFallFlying() || (!livTarget.onGround() && livTarget.getY() > entity.getY() + 2.0 && !livTarget.isInWater() && !livTarget.isInLava());
			boolean isPillaring = isTargetPillaring(world, livTarget, entity);
			boolean isUnreachable = (isFlying || isPillaring) && (entity.distanceTo(livTarget) > 4.5 || livTarget.getY() > entity.getY() + 2.5);
			if (isUnreachable) {
				double flyTicks = getD(entity, "target_unreachable_flying_ticks") + 1.0;
				putD(entity, "target_unreachable_flying_ticks", flyTicks);

				if (flyTicks >= 120.0 && !isChannelingAbility(entity) && getB(entity, "unlocked_overhead_combo")) {
					double chance = Math.min(0.50, (flyTicks - 120.0) * 0.002);
					if (entity.getRandom().nextDouble() < chance) {
						putD(entity, "rot_overhead_ticks", OVERHEAD_TOTAL_TICKS);
						putS(entity, "overhead_target_uuid", livTarget.getUUID().toString());
						putD(entity, "target_unreachable_flying_ticks", 0.0);
					}
				}
			} else {
				putD(entity, "target_unreachable_flying_ticks", 0.0);
			}
		} else {
			putD(entity, "target_unreachable_flying_ticks", 0.0);
		}

		if (interceptEnderPearlsPipeline(world, entity, combatTarget)) {
			handlePassengerAndGrowth(entity);
			return;
		}

		double totemInspectTicks = getD(entity, "sentinel_totem_inspect_ticks");
		if (totemInspectTicks > 0) {
			if (entity instanceof LivingEntity living) {
				boolean isInfinity = getB(entity, "sentinel_is_infinity_totem");
				net.minecraft.world.item.Item infinityTotemItem = isInfinity ? BuiltInRegistries.ITEM.get(ResourceLocation.parse("avaritia:infinity_totem")) : null;
				net.minecraft.world.item.ItemStack stackToHold = (infinityTotemItem != null) ? new net.minecraft.world.item.ItemStack(infinityTotemItem) : new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
				living.setItemInHand(InteractionHand.MAIN_HAND, stackToHold);
			}
			putD(entity, "sentinel_totem_inspect_ticks", totemInspectTicks - 1.0);
			entity.setDeltaMovement(0.0, entity.getDeltaMovement().y(), 0.0);
			entity.hurtMarked = true;
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
				mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0);
			}

			Vec3 lookDownTarget = entity.position().add(entity.getViewVector(1.0F).scale(0.5)).add(0, entity.getBbHeight() * 0.35, 0);
			Vec3 targetPos = null;
			if (combatTarget != null) {
				targetPos = new Vec3(combatTarget.getX(), combatTarget.getY() + combatTarget.getBbHeight() * 0.75, combatTarget.getZ());
			} else {
				targetPos = entity.position().add(entity.getViewVector(1.0F).scale(4.0)).add(0, entity.getBbHeight() * 0.75, 0);
			}
			double w = 1.0;
			if (totemInspectTicks > 150.0) {
				w = (180.0 - totemInspectTicks) / 30.0;
			} else if (totemInspectTicks < 40.0) {
				w = totemInspectTicks / 40.0;
			}
			Vec3 finalLookTarget = new Vec3(
				targetPos.x + (lookDownTarget.x - targetPos.x) * w,
				targetPos.y + (lookDownTarget.y - targetPos.y) * w,
				targetPos.z + (lookDownTarget.z - targetPos.z) * w
			);
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, finalLookTarget);

			if (totemInspectTicks > 60.0) {
				if (totemInspectTicks % 20 == 0) {
					playHostileSound(world, entity, "entity.warden.sniff", 1.2F, 0.65F);
				}
			} else {
				if (totemInspectTicks % 10 == 0) {
					playHostileSound(world, entity, "block.amethyst_block.hit", 1.4F, 0.75F);
				}
			}

			boolean totemPoppedEarly = false;
			if (entity instanceof LivingEntity living && totemInspectTicks < 178.0) {
				boolean isInfinity = getB(entity, "sentinel_is_infinity_totem");
				net.minecraft.world.item.Item infinityTotemItem = isInfinity ? BuiltInRegistries.ITEM.get(ResourceLocation.parse("avaritia:infinity_totem")) : null;
				net.minecraft.world.item.Item expectedItem = (infinityTotemItem != null) ? infinityTotemItem : net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
				if (living.getItemInHand(InteractionHand.MAIN_HAND).getItem() != expectedItem) {
					totemPoppedEarly = true;
				}
			}

			if (totemInspectTicks == 1.0 || totemPoppedEarly) {
				putB(entity, "sentinel_totem_active", true);
				if (world instanceof ServerLevel level) {
					boolean isInfinity = getB(entity, "sentinel_is_infinity_totem");
					level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 1.0, entity.getZ(), 45, 0.6, 0.6, 0.6, 0.25);
					if (isInfinity) {
						level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
						level.sendParticles(ParticleTypes.DRAGON_BREATH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 100, 0.8, 0.8, 0.8, 0.15);
						level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 80, 0.8, 0.8, 0.8, 0.15);
					}
					playHostileSound(level, entity, "item.totem.use", 2.0F, isInfinity ? 0.45F : 0.85F);
					playHostileSound(level, entity, "entity.player.hurt_on_fire", 1.2F, 0.5F);
					if (isInfinity) {
						playHostileSound(level, entity, "entity.generic.explode", 2.0F, 0.5F);
						playHostileSound(level, entity, "entity.warden.sonic_boom", 2.0F, 0.4F);
					}
					if (combatTarget instanceof Player p) {
						if (isInfinity) {
							boolean alreadySaid = getB(entity, "sentinel_said_prepare_thyself");
							if (!alreadySaid) {
								putB(entity, "sentinel_said_prepare_thyself", true);
								RotDialoguesProcedure.sendInfinityTotemQuote(p);
							}
						} else {
							RotDialoguesProcedure.sendRandomTotemQuote(p);
						}
					}
				}

				if (entity instanceof LivingEntity living && getB(entity, "sentinel_has_queued_totem")) {
					putB(entity, "sentinel_has_queued_totem", false);
					net.minecraft.world.item.ItemStack offStack = living.getItemInHand(InteractionHand.OFF_HAND);
					if (!offStack.isEmpty()) {
						living.setItemInHand(InteractionHand.MAIN_HAND, offStack);
						living.setItemInHand(InteractionHand.OFF_HAND, net.minecraft.world.item.ItemStack.EMPTY);
						putD(entity, "sentinel_totem_inspect_ticks", 180.0);
					} else {
						living.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
						putD(entity, "sentinel_totem_inspect_ticks", 0.0);
					}
				} else {
					if (entity instanceof LivingEntity living) {
						living.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
					}
					putD(entity, "sentinel_totem_inspect_ticks", 0.0);
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		if (checkAndSeekDroppedTotems(world, entity)) {
			return;
		}

		handleThreeBlockHeightSituationalAwareness(world, entity, combatTarget);
		if (handleElevatedUnreachableTarget(world, entity, combatTarget)) {
			handlePassengerAndGrowth(entity);
			return;
		}

		if (handleTotemStealing(world, entity, combatTarget)) {
			return;
		}

		if (handleSuperheatEvaporationState(world, entity, combatTarget)) {
			handlePassengerAndGrowth(entity);
			return;
		}

		handleHeavyPunchState(world, entity, combatTarget);

		if (handleThreatAwareEvasiveSpacing(world, entity, combatTarget)) {
			return;
		}

		if (handleDiveCounterState(world, entity, combatTarget)) {
			return;
		}

		if (handleCustomCombos(world, entity, combatTarget)) {
			return;
		}

		if (handleOverheadState(world, entity, combatTarget)) {
			return;
		}

		if (handleSlamState(world, entity, combatTarget)) {
			return;
		}

		if (handleDieKickState(world, entity, combatTarget)) {
			return;
		}

		double sonicScreamTicksFirstCheck = getD(entity, "sentinel_sonic_scream_ticks");
		if (sonicScreamTicksFirstCheck > 0) {
			executeSentinelSonicScream(world, entity, combatTarget, (int) sonicScreamTicksFirstCheck);
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double omniSonicTicks = getD(entity, "sentinel_omni_sonic_charge_ticks");
		if (omniSonicTicks > 0) {
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (entity.getDeltaMovement().y() > 0) {
				entity.setDeltaMovement(entity.getDeltaMovement().x(), 0.0, entity.getDeltaMovement().z());
			}
			putB(entity, "is_sonic_boom_large", true);

			if (world instanceof ServerLevel level) {
				Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);

				if (OMNI_SONIC_BOOM_SHOW_PARTICLES) {
					if (omniSonicTicks > OMNI_SONIC_BOOM_TRIGGER_TICK) {
						double chargeTotal = OMNI_SONIC_BOOM_ANIMATION_TICKS - OMNI_SONIC_BOOM_TRIGGER_TICK;
						double elapsed = OMNI_SONIC_BOOM_ANIMATION_TICKS - omniSonicTicks;
						double progress = elapsed / chargeTotal;

						double radius = 6.0 - progress * 5.0;

						int count = (int) Math.max(4, (4.0 * Math.PI * radius * radius * 1.0));
						if (count > 60) count = 60;
						double goldenRatio = (1.0 + Math.sqrt(5.0)) / 2.0;
						for (int i = 0; i < count; i++) {
							double theta = 2 * Math.PI * i / goldenRatio;
							double phi = Math.acos(1.0 - 2.0 * (i + 0.5) / count);
							double sx = Math.cos(theta) * Math.sin(phi);
							double sy = Math.sin(theta) * Math.sin(phi);
							double sz = Math.cos(phi);

							double px = center.x + sx * radius;
							double py = center.y + sy * radius;
							double pz = center.z + sz * radius;

							double rx = (level.getRandom().nextDouble() - 0.5) * 0.02;
							double ry = (level.getRandom().nextDouble() - 0.5) * 0.02;
							double rz = (level.getRandom().nextDouble() - 0.5) * 0.02;

							level.sendParticles(ParticleTypes.CRIT, px + rx, py + ry, pz + rz, 1, 0.0, 0.0, 0.0, 0.0);
							if (level.getRandom().nextDouble() < 0.10) {
								level.sendParticles(ParticleTypes.SONIC_BOOM, px + rx, py + ry, pz + rz, 1, 0.0, 0.0, 0.0, 0.0);
							}
						}

						if (level.getRandom().nextDouble() < progress * 0.85) {
							level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.2, entity.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
						}
					} else {
						double decayTotal = OMNI_SONIC_BOOM_TRIGGER_TICK;
						double elapsedDecay = OMNI_SONIC_BOOM_TRIGGER_TICK - omniSonicTicks;
						double progressDecay = elapsedDecay / decayTotal;

						double currentRadius = 1.0 + progressDecay * OMNI_SONIC_BOOM_RANGE;

						int count = (int) Math.max(6, (4.0 * Math.PI * currentRadius * currentRadius * 0.3));
						if (count > 35) count = 35;
						double goldenRatio = (1.0 + Math.sqrt(5.0)) / 2.0;
						for (int i = 0; i < count; i++) {
							double theta = 2 * Math.PI * i / goldenRatio;
							double phi = Math.acos(1.0 - 2.0 * (i + 0.5) / count);
							double sx = Math.cos(theta) * Math.sin(phi);
							double sy = Math.sin(theta) * Math.sin(phi);
							double sz = Math.cos(phi);

							double px = center.x + sx * currentRadius;
							double py = center.y + sy * currentRadius;
							double pz = center.z + sz * currentRadius;

							if (level.getRandom().nextDouble() < 0.25) {
								level.sendParticles(ParticleTypes.SONIC_BOOM, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
							}
							if (level.getRandom().nextDouble() < 0.2) {
								level.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0.1, 0.1, 0.1, 0.02);
							}
						}
					}
				}
			}

			if (omniSonicTicks == getTelegraphJitter(entity, "omni_sonic", OMNI_SONIC_BOOM_TRIGGER_TICK, -5.0, 5.0)) {
				if (world instanceof ServerLevel level) {
					boolean totemActive = getB(entity, "sentinel_totem_active");
					boolean unlockedExplosion = getB(entity, "unlocked_explosion_boom");
					double radius = totemActive ? OMNI_SONIC_BOOM_RANGE * 1.5 : OMNI_SONIC_BOOM_RANGE;

					if (unlockedExplosion) {
						level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, entity.getX(), entity.getY() + 1.2, entity.getZ(), 2, 1.0, 1.0, 1.0, 0.1);
						playHostileSound(level, entity, "entity.generic.explode", 2.0F, 0.8F);
					}

					if (OMNI_SONIC_BOOM_SHOW_PARTICLES) {
						for (int angleDeg = 0; angleDeg < 360; angleDeg += (totemActive ? 12 : 24)) {
							double rad = Math.toRadians(angleDeg);
							double dx = Math.sin(rad);
							double dz = Math.cos(rad);
							for (double r = 1.0; r <= radius; r += 4.0) {
								double px = entity.getX() + dx * r;
								double py = entity.getY() + 1.2;
								double pz = entity.getZ() + dz * r;
								level.sendParticles(ParticleTypes.SONIC_BOOM, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
								if (unlockedExplosion) {
									level.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 1, 0.2, 0.2, 0.2, 0.02);
								}
							}
						}
					}

					java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius), e -> e != entity && !isWoodboundEntity(e, entity));
					for (LivingEntity targetVictim : targets) {
						float damageVal = totemActive ? 85.0F : 48.0F;
						if (unlockedExplosion) {
							damageVal = totemActive ? 130.0F : 78.0F;
						}
						float finalOmniDamage = Math.min(150.0F, damageVal * (float) getAdaptationMultiplier(entity));
						dealTrueDamageToBosses(targetVictim, getBackwoodsDamage(level, "rot_sonic_boom", entity), finalOmniDamage);
						Vec3 push = targetVictim.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0).normalize();
						double pushMult = totemActive ? 12.0 : 8.5;
						double pushUp = totemActive ? 2.5 : 1.8;
						setMotion(targetVictim, push.x * pushMult, pushUp, push.z * pushMult);
						ClearFlightPathProcedure.execute(world, targetVictim, targetVictim.getX(), targetVictim.getY(), targetVictim.getZ(), targetVictim.getX() + push.x * pushMult, targetVictim.getY() + pushUp, targetVictim.getZ() + push.z * pushMult, 2.5, 1);
					}

					BlockPos posCenter = BlockPos.containing(entity.position());
					double baseRadius = totemActive ? 12.0 : 6.5;
					int rangeBound = totemActive ? 12 : 6;
					int heightBoundUpper = totemActive ? 6 : 4;
					int heightBoundLower = totemActive ? -3 : -1;
					for (BlockPos bp : BlockPos.betweenClosed(posCenter.offset(-rangeBound, heightBoundLower, -rangeBound), posCenter.offset(rangeBound, heightBoundUpper, rangeBound))) {
						double dx = bp.getX() - posCenter.getX();
						double dz = bp.getZ() - posCenter.getZ();
						double distSq = dx * dx + dz * dz;
						double randomRadius = baseRadius + (Math.random() * 2.0 - 1.0);
						if (distSq <= randomRadius * randomRadius) {
							BlockState bs = level.getBlockState(bp);
							if (!bs.isAir() && bs.getDestroySpeed(level, bp) >= 0 && bs.getDestroySpeed(level, bp) < (totemActive ? 80.0F : 50.0F)) {
								level.destroyBlock(bp, false);
							}
						}
					}
					playHostileSound(level, entity, "entity.warden.sonic_boom", 2.5F, 0.40F);
					playHostileSound(level, entity, "entity.generic.explode", 2.0F, 0.5F);
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		} else {
			putB(entity, "is_sonic_boom_large", false);
		}

		double sonicTicksFirstCheck = getD(entity, "sentinel_sonic_ticks");
		if (sonicTicksFirstCheck > 0) {
			if (combatTarget == null || !combatTarget.isAlive() || combatTarget.isRemoved()) {
				Entity altTarget = findEntityInWorldRange(world, LivingEntity.class, entity.getX(), entity.getY(), entity.getZ(), SONIC_BOOM_RANGE, entity);
				if (altTarget != null) {
					combatTarget = altTarget;
					if (entity instanceof Mob mob) mob.setTarget((LivingEntity) altTarget);
				}
			}
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (entity.getDeltaMovement().y() > 0) {
				entity.setDeltaMovement(entity.getDeltaMovement().x(), 0.0, entity.getDeltaMovement().z());
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}
			double triggerTick = getTelegraphJitter(entity, "sonic_boom", SONIC_BOOM_TRIGGER_TICK, -4.0, 4.0);
			boolean alreadyTriggered = getB(entity, "sentinel_sonic_triggered");
			if (!alreadyTriggered && sonicTicksFirstCheck <= triggerTick) {
				if (combatTarget != null) {
					double tdx = combatTarget.getX() - entity.getX();
					double tdz = combatTarget.getZ() - entity.getZ();
					double tdy = (combatTarget.getY() + combatTarget.getBbHeight() * 0.5) - (entity.getY() + entity.getBbHeight() * SONIC_BOOM_TORSO_Y_FACTOR);
					double flatD = Math.sqrt(tdx * tdx + tdz * tdz);
					double pitchAngle = -Math.toDegrees(Math.atan2(tdy, flatD));

					int repositionAttempts = (int) getD(entity, "sentinel_sonic_reposition_attempts");
					if ((pitchAngle > 45.0 || (flatD < 2.2 && tdy < -1.0)) && repositionAttempts < 2) {
						double normDist = flatD < 0.001 ? 1.0 : flatD;
						double backX = entity.getX() - (tdx / normDist) * 3.5;
						double backZ = entity.getZ() - (tdz / normDist) * 3.5;
						if (entity instanceof Mob mob) {
							mob.getNavigation().moveTo(backX, entity.getY(), backZ, ROT_WALK_SPEED * 1.3);
						}
						putD(entity, "sentinel_sonic_reposition_attempts", repositionAttempts + 1.0);
						putD(entity, "sentinel_sonic_ticks", sonicTicksFirstCheck + 8.0);
					} else if (entity.distanceTo(combatTarget) <= SONIC_BOOM_RANGE + 3.0) {
						putB(entity, "sentinel_sonic_triggered", true);
						fireSuperchargedSonicBoomEffectAndDamage(world, entity, combatTarget);
					}
				} else {
					putB(entity, "sentinel_sonic_triggered", true);
					playHostileSound(world, entity, "entity.warden.sonic_boom", 1.5F, 0.3F);
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double skyWarpTicks = getD(entity, "sentinel_sky_warp_slam_ticks");
		if (skyWarpTicks > 0) {
			if (skyWarpTicks > 1) {
				entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.1, Math.max(0, entity.getDeltaMovement().y() * 0.5), entity.getDeltaMovement().z() * 0.1);
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
				if (skyWarpTicks == 1) {
					double targetX = combatTarget.getX();
					double targetY = combatTarget.getY() + 3.8;
					double targetZ = combatTarget.getZ();
					if (world instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.1, entity.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
						teleportEntity(entity, targetX, targetY, targetZ);
						level.sendParticles(ParticleTypes.SMOKE, targetX, targetY + 1.1, targetZ, 8, 0.2, 0.2, 0.2, 0.05);
						playHostileSound(level, targetX, targetY, targetZ, "item.chorus_fruit.teleport", 1.3F, 1.1F);
						entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(combatTarget.getX(), combatTarget.getY() + combatTarget.getBbHeight() * 0.5, combatTarget.getZ()));
						playHostileSound(level, targetX, targetY, targetZ, "entity.iron_golem.attack", 1.4F, 0.5F);
						playHostileSound(level, targetX, targetY, targetZ, "entity.player.attack.sweep", 1.4F, 0.7F);
						level.sendParticles(ParticleTypes.SWEEP_ATTACK, targetX, targetY - 1.0, targetZ, 3, 0.4, 0.4, 0.4, 0.0);
						if (combatTarget instanceof LivingEntity livTarget) {
							setMotion(livTarget, 0.0, -4.5, 0.0);
							putB(livTarget, "sentinel_sky_warp_slam_impact", true);
							double punchDmg = getD(entity, "adapted_punch_damage");
							if (punchDmg < 8.0) punchDmg = 8.0;
							dealTrueDamageToBosses(livTarget, getBackwoodsDamage(level, "rot_overhead_slam", entity), (float) (punchDmg * 2.2) * (float) getAdaptationMultiplier(entity));
						}
					}
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		if (combatTarget instanceof LivingEntity livTarget && getB(livTarget, "sentinel_sky_warp_slam_impact")) {
			if (livTarget.onGround() || livTarget.getDeltaMovement().y > -0.1) {
				putB(livTarget, "sentinel_sky_warp_slam_impact", false);
				if (world instanceof ServerLevel level) {
					double tx = livTarget.getX();
					double ty = livTarget.getY();
					double tz = livTarget.getZ();
					boolean totemActive = getB(entity, "sentinel_totem_active");
					float explosionForce = totemActive ? 6.2F : 3.5F;
					explodeWoodbound(level, entity, tx, ty + 0.5, tz, explosionForce);
					level.sendParticles(ParticleTypes.EXPLOSION, tx, ty + 0.5, tz, totemActive ? 15 : 6, 0.6, 0.2, 0.6, 0.15);
					level.sendParticles(ParticleTypes.CRIT, tx, ty + 0.5, tz, totemActive ? 40 : 20, 0.5, 0.5, 0.5, 0.3);
					level.sendParticles(ParticleTypes.SONIC_BOOM, tx, ty + 0.5, tz, totemActive ? 3 : 1, 0.1, 0.1, 0.1, 0.0);
					playHostileSound(level, tx, ty, tz, "entity.generic.explode", 1.4F, 0.85F);
					playHostileSound(level, tx, ty, tz, "entity.iron_golem.death", 1.1F, 0.65F);
					List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(tx, ty, tz), totemActive ? 10 : 5, totemActive ? 8 : 5, totemActive ? 10 : 5), e -> e != entity && e != livTarget && !isWoodboundEntity(e, entity));
					for (LivingEntity near : nearby) {
						if (near.isAlive()) {
							float nearDmg = totemActive ? 22.0F : 10.0F;
							dealTrueDamageToBosses(near, getBackwoodsDamage(level, "rot_overhead_slam", entity), nearDmg * (float) getAdaptationMultiplier(entity));
							Vec3 pushAway = near.position().subtract(livTarget.position()).normalize();
							double pushMult = totemActive ? 2.5 : 1.5;
							setMotion(near, pushAway.x * pushMult, 0.5, pushAway.z * pushMult);
						}
					}
				}
			}
		}

		double judgmentTicks = getD(entity, "sentinel_judgment_ticks");
		if (judgmentTicks > 0) {
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}

			if (judgmentTicks > 20) {
				if (!isCustomComboActive(entity) && combatTarget != null && entity.distanceTo(combatTarget) < 6.5 && judgmentTicks > 25 && getD(combatTarget, "bw_recent_kb_ticks") <= 0) {
					putD(entity, "sentinel_judgment_ticks", 0);
					if (combatTarget instanceof LivingEntity targetLiv) {
						executeMinosHeavyPunchBlink(world, entity, targetLiv, false);
					}
					handlePassengerAndGrowth(entity);
					return;
				}
				entity.setDeltaMovement(0.0, 0.0, 0.0);
				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 0.2, entity.getZ(), 3, 0.3, 0.1, 0.3, 0.02);
					if (judgmentTicks % 5 == 0) {
						playHostileSound(level, entity, "entity.warden.heartbeat", 1.2F, 0.7F);
					}
				}
			} else if (judgmentTicks == 20) {
				Entity targetLoc = combatTarget != null ? combatTarget : entity;
				double targetYaw = targetLoc.getYRot();
				double radians = Math.toRadians(targetYaw);
				double kickStartX = targetLoc.getX() - 12.0 * Math.sin(radians);
				double kickStartY = targetLoc.getY() + 2.8;
				double kickStartZ = targetLoc.getZ() + 12.0 * Math.cos(radians);

				teleportEntity(entity, kickStartX, kickStartY, kickStartZ);
				if (combatTarget != null) {
					snapLookAtTarget(entity, combatTarget);
				}

				Vec3 dir;
				if (combatTarget != null) {
					Vec3 rotCenter = entity.getBoundingBox().getCenter();
					Vec3 targetCenter = combatTarget.getBoundingBox().getCenter();
					double dist = rotCenter.distanceTo(targetCenter);
					double speed = 5.5;
					double flightTicks = Math.max(1.0, dist / speed);

					if (!combatTarget.onGround()) {
						Vec3 targetVel = combatTarget.getDeltaMovement();
						double grav = 0.08;
						double predX = targetCenter.x + targetVel.x * flightTicks;
						double predY = targetCenter.y + targetVel.y * flightTicks - 0.5 * grav * flightTicks * flightTicks;
						double predZ = targetCenter.z + targetVel.z * flightTicks;

						double groundY = findGroundY(world, combatTarget) + combatTarget.getBbHeight() * 0.5;
						if (predY < groundY) {
							predY = groundY;
						}
						Vec3 predictedCenter = new Vec3(predX, predY, predZ);
						dir = predictedCenter.subtract(rotCenter);
					} else {
						dir = targetCenter.subtract(rotCenter);
					}

					if (dir.length() > 0.1) {
						dir = dir.normalize();
					} else {
						dir = entity.getLookAngle().normalize();
					}
				} else {
					dir = entity.getLookAngle().normalize();
				}
				putD(entity, "sentinel_judgment_dir_x", dir.x);
				putD(entity, "sentinel_judgment_dir_y", dir.y);
				putD(entity, "sentinel_judgment_dir_z", dir.z);

				playHostileSound(world, entity, "entity.warden.sonic_charge", 1.8F, 0.65F);
				spawnParticles(world, ParticleTypes.FLASH, entity.getX(), entity.getY() + 0.5, entity.getZ(), 1, 0, 0, 0, 0);
			} else if (judgmentTicks > 1) {
				double dirX = getD(entity, "sentinel_judgment_dir_x");
				double dirY = getD(entity, "sentinel_judgment_dir_y");
				double dirZ = getD(entity, "sentinel_judgment_dir_z");
				double speed = 5.5;

				setMotion(entity, dirX * speed, dirY * speed, dirZ * speed);
				entity.fallDistance = 0;

				double dh = Math.sqrt(dirX * dirX + dirZ * dirZ);
				float targetYRot = (float) (Mth.atan2(dirZ, dirX) * (180F / Math.PI)) - 90F;
				float targetXRot = (float) (-(Mth.atan2(dirY, dh) * (180F / Math.PI)));
				entity.setYRot(targetYRot);
				entity.setXRot(targetXRot);
				if (entity instanceof Mob mob) {
					mob.yBodyRot = targetYRot;
					mob.yHeadRot = targetYRot;
				}

				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.GUST, entity.getX(), entity.getY() + 0.3, entity.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
					level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.3, entity.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
				}

				boolean hitTarget = false;
				Vec3 impactPoint = null;

				if (combatTarget != null && combatTarget.isAlive()) {
					AABB targetBox = combatTarget.getBoundingBox();
					double rotHalfWidth = entity.getBbWidth() * 0.5;
					double rotHalfHeight = entity.getBbHeight() * 0.5;
					AABB wallBox = targetBox.inflate(rotHalfWidth, rotHalfHeight, rotHalfWidth);
					Vec3 startPos = entity.getBoundingBox().getCenter();
					Vec3 endPos = startPos.add(dirX * speed, dirY * speed, dirZ * speed);

					if (wallBox.contains(startPos) || entity.getBoundingBox().intersects(targetBox)) {
						hitTarget = true;
						impactPoint = startPos;
					} else {
						java.util.Optional<Vec3> clipOpt = wallBox.clip(startPos, endPos);
						if (clipOpt.isPresent()) {
							hitTarget = true;
							impactPoint = clipOpt.get();
						}
					}
				}

				double distToTarget = combatTarget != null ? entity.distanceTo(combatTarget) : 999.0;
				if (hitTarget || distToTarget < JUDGMENT_KICK_IMPACT_DIST || entity.onGround()) {
					if (hitTarget && impactPoint != null) {
						double stopX = impactPoint.x;
						double stopY = impactPoint.y - entity.getBbHeight() * 0.5;
						double stopZ = impactPoint.z;
						entity.teleportTo(stopX, stopY, stopZ);
						entity.setDeltaMovement(0, 0, 0);
					}
					putD(entity, "sentinel_judgment_ticks", 2);
					judgmentTicks = 2;
				}
			} else if (judgmentTicks == 1) {
				double impactX = entity.getX();
				double impactY = entity.getY();
				double impactZ = entity.getZ();

				if (world instanceof ServerLevel level) {
					double groundY = findGroundY(level, entity);
					if (impactY > groundY + 0.5 && entity.onGround()) {
						impactY = groundY;
					}
					teleportEntity(entity, impactX, impactY, impactZ);

					net.minecraft.world.level.block.state.BlockState floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(impactX, impactY - 0.5, impactZ));
					if (floorState.isAir()) {
						floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(impactX, impactY - 1.5, impactZ));
					}
					if (floorState.isAir()) {
						floorState = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
					}
					net.minecraft.core.particles.BlockParticleOption dustPillarOptions = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, floorState);
					net.minecraft.core.particles.ParticleType<?> _tsdType = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse("trial_spawner_detection"));
					net.minecraft.core.particles.ParticleOptions trialSpawnerDetection = _tsdType instanceof net.minecraft.core.particles.ParticleOptions _tsdOpt ? _tsdOpt : net.minecraft.core.particles.ParticleTypes.EFFECT;

					for (int r = 1; r <= 3; r++) {
						double radiusVal = r * 1.2;
						for (int deg = 0; deg < 360; deg += 30) {
							double rads = Math.toRadians(deg);
							double px = impactX + Math.cos(rads) * radiusVal;
							double pz = impactZ + Math.sin(rads) * radiusVal;
							level.sendParticles(dustPillarOptions, px, impactY + 0.1, pz, 1, 0.0, 0.05, 0.0, 0.01);
							level.sendParticles(trialSpawnerDetection, px, impactY + 0.1, pz, 1, 0.0, 0.05, 0.0, 0.01);
						}
					}

					for (int r = 1; r <= 5; r++) {
						final double rVal = r;
						int particleCount = (int) (12 * rVal);
						for (int i = 0; i < particleCount; i++) {
							double angle = (2 * Math.PI * i) / particleCount;
							double px = impactX + Math.cos(angle) * rVal;
							double py = impactY + 0.15;
							double pz = impactZ + Math.sin(angle) * rVal;
							double vx = Math.cos(angle) * 0.18;
							double vy = 0.05 + 0.02 * rVal;
							double vz = Math.sin(angle) * 0.18;

							level.sendParticles(dustPillarOptions, px, py, pz, 0, vx, vy, vz, 1.0);
							level.sendParticles(trialSpawnerDetection, px, py, pz, 0, vx * 0.5, vy, vz * 0.5, 1.0);
						}
					}

					playHostileSound(level, impactX, impactY, impactZ, "entity.generic.explode", 1.8F, 0.6F);
					playHostileSound(level, impactX, impactY, impactZ, "entity.warden.sonic_boom", 1.8F, 0.55F);
					playHostileSound(level, impactX, impactY, impactZ, "entity.iron_golem.death", 1.3F, 0.45F);

					boolean totemActive = getB(entity, "sentinel_totem_active");
					boolean isInfinity = getB(entity, "sentinel_is_infinity_totem");
					sendCameraShake(totemActive ? 1.5F : 1.0F, totemActive ? 25 : 15, totemActive ? 30.0F : 20.0F);

					if (isInfinity) {
						explodeWoodbound(level, entity, impactX, impactY + 0.5, impactZ, 6.0F);
					} else if (totemActive) {
						explodeWoodbound(level, entity, impactX, impactY + 0.5, impactZ, 5.5F);
					}
					level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impactX, impactY + 0.5, impactZ, totemActive ? 15 : 6, 0.5, 0.2, 0.5, 0.1);
					level.sendParticles(ParticleTypes.CRIT, impactX, impactY + 0.5, impactZ, totemActive ? 40 : 22, 0.4, 0.4, 0.4, 0.2);

					if (combatTarget instanceof LivingEntity targetLiv) {
						double distToTarget = entity.distanceTo(targetLiv);
						if (distToTarget <= 3.5 || entity.getBoundingBox().inflate(1.5).intersects(targetLiv.getBoundingBox())) {
							double punchDmg = getD(entity, "adapted_punch_damage");
							if (punchDmg < 8.0) punchDmg = 8.0;
							double dropkickDamageMult = 3.5;
							if (isInfinity) {
								dropkickDamageMult = 3.5 * 2.5;
							}
							dealTrueDamageToBosses(targetLiv, getBackwoodsDamage(level, "rot_judgement_dropkick", entity), (float) (punchDmg * dropkickDamageMult) * (float) getAdaptationMultiplier(entity));

							Vec3 pushVec = targetLiv.position().subtract(entity.position());
							double horizontalDist = Math.sqrt(pushVec.x * pushVec.x + pushVec.z * pushVec.z);
							if (horizontalDist < 0.1) {
								pushVec = entity.getLookAngle();
								horizontalDist = Math.sqrt(pushVec.x * pushVec.x + pushVec.z * pushVec.z);
							}
							if (horizontalDist > 0.01) {
								pushVec = new Vec3(pushVec.x / horizontalDist, 0, pushVec.z / horizontalDist);
							} else {
								pushVec = new Vec3(1, 0, 0);
							}

							double pushForce = totemActive ? 3.0 : 2.2;
							double pushUp = getD(entity, "sentinel_cc5_stage") > 0 ? 1.45 : 0.6;
							if (isInfinity) {
								pushForce = 7.5;
								pushUp = 1.45;
							}
							applyKnockbackAndSync(targetLiv, pushVec.x * pushForce, pushUp, pushVec.z * pushForce);
						}
					}

					putD(entity, "sentinel_shockwave_stage", 1);
					putD(entity, "sentinel_shockwave_x", impactX);
					putD(entity, "sentinel_shockwave_y", impactY);
					putD(entity, "sentinel_shockwave_z", impactZ);
					putB(entity, "sentinel_shockwave_vertical", true);
					putD(entity, "sentinel_shockwave_yaw", entity.getYRot());

					final Entity dropkickTarget = combatTarget;
					List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(impactX, impactY, impactZ), 8, 6, 8), e -> e != entity && e != dropkickTarget && !isWoodboundEntity(e, entity));
					for (LivingEntity near : nearby) {
						if (near.isAlive()) {
							float baseNearDmg = 15.0F;
							if (isInfinity) baseNearDmg *= 2.5F;
							dealTrueDamageToBosses(near, getBackwoodsDamage(level, "rot_judgement_dropkick", entity), baseNearDmg * (float) getAdaptationMultiplier(entity));
							Vec3 pushAway = near.position().subtract(entity.position());
							double nearHorizontalDist = Math.sqrt(pushAway.x * pushAway.x + pushAway.z * pushAway.z);
							if (nearHorizontalDist > 0.01) {
								pushAway = new Vec3(pushAway.x / nearHorizontalDist, 0, pushAway.z / nearHorizontalDist);
							} else {
								pushAway = new Vec3(1, 0, 0);
							}
							double nearPush = 2.2;
							double nearPushUp = 0.45;
							if (isInfinity) {
								nearPush = 5.5;
								nearPushUp = 1.0;
							}
							applyKnockbackAndSync(near, pushAway.x * nearPush, nearPushUp, pushAway.z * nearPush);
						}
					}
				}
				putD(entity, "sentinel_rider_hold_ticks", 12);
				putB(entity, "sentinel_rider_hold_onground", entity.onGround());
				putD(entity, "sentinel_landing_ticks", 0);
				putB(entity, "sentinel_is_slam_landing", false);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double riderHoldTicks = getD(entity, "sentinel_rider_hold_ticks");
		if (riderHoldTicks > 0) {
			boolean isGroundHold = getB(entity, "sentinel_rider_hold_onground");
			if (isGroundHold || entity.onGround()) {
				entity.setDeltaMovement(0, -0.05, 0);
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}
			if (riderHoldTicks == 1) {
				if (isGroundHold || entity.onGround()) {
					putD(entity, "sentinel_landing_ticks", 20);
					putB(entity, "sentinel_is_slam_landing", true);
				} else {
					putD(entity, "sentinel_landing_ticks", 0);
					putB(entity, "sentinel_is_slam_landing", false);
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double minosStage = getD(entity, "sentinel_minos_stage");
		double minosTicks = getD(entity, "sentinel_minos_ticks");
		if (minosStage == 0 && minosTicks > 0) {
			minosStage = 1;
			putD(entity, "sentinel_minos_stage", 1);
		}
		if (minosStage > 0 || minosTicks > 0) {
			cancelActiveBeams(world, entity);
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
				if (entity instanceof Mob mob) {
					mob.getNavigation().stop();
				}
			}
			entity.setDeltaMovement(0, entity.getDeltaMovement().y(), 0);

			if (minosStage == 1) {
				if (combatTarget instanceof LivingEntity targetLiv) {
					if (!hasHeavyPunchSupport(world, targetLiv)) {
						return;
					}
					putD(entity, "sentinel_minos_punch_count", 1);
					executeMinosHeavyPunchBlink(world, entity, targetLiv, true);
					putD(entity, "sentinel_minos_stage", 2);
				} else {
					clearDoubles(entity, "sentinel_minos_stage", "sentinel_minos_ticks");
				}
			} else if (minosStage == 2) {
				double heavyLeft = getD(entity, "sentinel_heavy_left_punch_ticks");
				double heavyRight = getD(entity, "sentinel_heavy_right_punch_ticks");

				if (heavyLeft == 0 && heavyRight == 0) {
					boolean isCornered = (combatTarget != null) && isTargetCornered(world, combatTarget, entity);
					double baseWait = isCornered ? 12.0 : 32.0;

					double targetVel = (combatTarget != null) ? combatTarget.getDeltaMovement().horizontalDistance() : 0.0;
					double speedDiscount = Math.min(12.0, targetVel * 20.0);
					double missCount = getD(entity, "sentinel_heavy_punch_misses");
					double missDiscount = Math.min(15.0, missCount * 5.0);

					baseWait = Math.max(4.0, baseWait - speedDiscount - missDiscount);

					double adaptation = getAdaptationMultiplier(entity);
					double adaptedWait = baseWait / Math.max(0.8, adaptation);
					double waitTicks = Math.max(4.0, getTelegraphJitter(entity, "minos_wait", adaptedWait, -10.0, 10.0));

					if (isCornered && combatTarget != null && world instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.ANGRY_VILLAGER, combatTarget.getX(), combatTarget.getY() + 1.2, combatTarget.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
						level.sendParticles(ParticleTypes.CRIT, combatTarget.getX(), combatTarget.getY() + 1.0, combatTarget.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
						playHostileSound(level, combatTarget, "entity.warden.heartbeat", 1.5F, 1.2F);
					}

					putD(entity, "sentinel_minos_wait_ticks", waitTicks);
					putD(entity, "sentinel_minos_stage", 3);
				}
			} else if (minosStage == 3) {
				double waitTicks = getD(entity, "sentinel_minos_wait_ticks");
				if (waitTicks <= 0) {
					double punchCount = getD(entity, "sentinel_minos_punch_count");
					if (punchCount < 4 && combatTarget instanceof LivingEntity targetLiv) {
						if (!hasHeavyPunchSupport(world, targetLiv)) {
							return;
						}
						punchCount++;
						putD(entity, "sentinel_minos_punch_count", punchCount);

						boolean isLeftHand = (punchCount % 2 != 0);
						executeMinosHeavyPunchBlink(world, entity, targetLiv, isLeftHand);

						putD(entity, "sentinel_minos_stage", 2);
					} else {
						clearDoubles(entity, "sentinel_minos_stage", "sentinel_minos_ticks");
						putD(entity, "sentinel_minos_wait_ticks", 0);
					}
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double closingTicksFirstCheck = getD(entity, "sentinel_laser_closing_ticks");
		if (closingTicksFirstCheck > 0) {
			entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		if (combatTarget == null) {
			putD(entity, K_ADAPT_MODE, 0);
			cancelActiveBeams(world, entity);

			if (entity instanceof Mob mob) {
				Player master = null;
				if (getB(entity, "master_guard_mode")) {
					master = getGuardPlayer(world, entity);
				} else if (getB(entity, "master_follow_enabled")) {
					master = getFollowPlayer(world, entity);
				}
				for (Player p : world.getEntitiesOfClass(Player.class, new AABB(x - 128, y - 128, z - 128, x + 128, y + 128, z + 128))) {
					if (master != null) break;
					String name = p.getGameProfile().getName();
					if (name.equals("honeypie_3301") || name.equals("Dev")) {
						boolean isDueling = getB(mob, "is_dueling");
						if (!isDueling) {
							master = p;
							break;
						}
					}
				}
				if (master != null) {
					double tpDelay = getD(mob, "rot_post_kill_teleport_delay_ticks");
					if (tpDelay > 0) {
						putD(mob, "rot_post_kill_teleport_delay_ticks", tpDelay - 1.0);
					}
					double distToMaster = mob.distanceTo(master);
					double dy = Math.abs(mob.getY() - master.getY());
					if ((distToMaster > 24.0 || dy > 8.0) && tpDelay <= 0) {
						double tx = master.getX() + (mob.getRandom().nextDouble() - 0.5) * 4.0;
						double tz = master.getZ() + (mob.getRandom().nextDouble() - 0.5) * 4.0;
						double ty = master.getY();
						teleportEntity(mob, tx, ty, tz);
					} else if (distToMaster > 5.0 && mob.tickCount % 5 == 0) {
						mob.getNavigation().moveTo(master, 1.25);
					} else if (distToMaster <= 3.0) {
						mob.getNavigation().stop();
					}
				}
			}

			handlePassengerAndGrowth(entity);
			return;
		}

		if (combatTarget instanceof Player p && p.getAbilities().instabuild && getD(entity, K_CREATIVE_MSG) == 0) {
			putD(entity, K_CREATIVE_MSG, 1);
			handlePassengerAndGrowth(entity);
			return;
		}

		String speciesKey = BuiltInRegistries.ENTITY_TYPE.getKey(combatTarget.getType()).toString().toLowerCase(java.util.Locale.ROOT);
		if (combatTarget instanceof Player) speciesKey = "minecraft:player";

		boolean hasAnalyzed = getB(entity, "analyzed_species_" + speciesKey);
		if (!hasAnalyzed) {
			double analyzingTicks = getD(entity, "sentinel_analyzing_ticks");
			int currentAnalysisTargetId = getI(entity, "sentinel_analysis_target_id");

			if (analyzingTicks <= 0 && currentAnalysisTargetId != combatTarget.getId()) {
				analyzingTicks = 40.0 + entity.getRandom().nextDouble() * 100.0;
				putD(entity, "sentinel_analyzing_ticks", analyzingTicks);
				putI(entity, "sentinel_analysis_target_id", combatTarget.getId());
			} else if (analyzingTicks > 0 && currentAnalysisTargetId == combatTarget.getId()) {
				putD(entity, "sentinel_analyzing_ticks", analyzingTicks - 1);
				entity.setDeltaMovement(0.0, entity.getDeltaMovement().y(), 0.0);

				lockLookAtTarget(entity, combatTarget);
				if (entity instanceof Mob _mob) {
					_mob.getNavigation().stop();
				}

				if (world instanceof ServerLevel _level && entity.tickCount % 4 == 0) {
					_level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
				}

				if (analyzingTicks - 1 <= 0) {
					putB(entity, "analyzed_species_" + speciesKey, true);
					putI(entity, "sentinel_analysis_target_id", 0);
					playHostileSound(world, entity, "entity.warden.agitated", 1.0F, 0.8F);
				}
				handlePassengerAndGrowth(entity);
				return;
			}
		} else {
			if (getD(entity, "sentinel_analyzing_ticks") > 0) {
				putD(entity, "sentinel_analyzing_ticks", 0);
				putI(entity, "sentinel_analysis_target_id", 0);
			}
		}

		lockLookAtTarget(entity, combatTarget);

		TargetIntent currentIntent = inferTargetIntent(entity, combatTarget);
		putS(entity, "sentinel_target_intent", currentIntent.name());
		adaptCapabilitiesToIntent(entity, currentIntent);

		if (interceptEnderPearlsPipeline(world, entity, combatTarget)) {
			handlePassengerAndGrowth(entity);
			return;
		}

		InterceptionPrediction currentInterception = evaluateInterceptionPipeline(entity, combatTarget, currentIntent);
		if (currentInterception.recommendWait) {
			putB(entity, "sentinel_waiting_intercept", true);
			if (currentInterception.repositionTargetPos != null) {
				lockLookAtTarget(entity, currentInterception.repositionTargetPos);
				if (entity instanceof Mob mob) {
					mob.getNavigation().moveTo(currentInterception.repositionTargetPos.x, currentInterception.repositionTargetPos.y, currentInterception.repositionTargetPos.z, ROT_RUN_SPEED);
				}
			}
		} else {
			putB(entity, "sentinel_waiting_intercept", false);
		}

		double distToTarget = entity.distanceTo(combatTarget);

		if (!isRotChannelingAbility(entity) && getD(entity, "sentinel_eat_punish_cooldown") <= 0.0) {
			CombatContext eatCtx = getCombatContext(entity, combatTarget);
			if (eatCtx.isEatingHealingItem && eatCtx.eatingTicksRemaining >= 6 && distToTarget <= 10.0) {
				if (distToTarget <= 7.0) {
					putD(entity, "sentinel_melee_windup", 4.0);
					putD(entity, "sentinel_eat_punish_cooldown", 100.0);
				} else if (getB(entity, "unlocked_teleportation") && getD(entity, "sentinel_judgment_ticks") <= 0.0) {
					putD(entity, "sentinel_judgment_ticks", 60.0);
					putD(entity, "sentinel_eat_punish_cooldown", 100.0);
				}
			}
		}

		double aiPlanTicks = getD(entity, "ai_combat_plan_ticks");
		if (aiPlanTicks <= 0) {
			putI(entity, "ai_combat_plan", Math.random() < 0.3 ? 1 : 0);
			putD(entity, "ai_combat_plan_ticks", 40.0 + Math.random() * 60.0);
		} else {
			putD(entity, "ai_combat_plan_ticks", aiPlanTicks - 1);
		}

		if (combatTarget instanceof LivingEntity lsTarget) {
			if (!lsTarget.onGround() && !lsTarget.isInWater()) {
				putD(entity, "ai_target_air_ticks", getD(entity, "ai_target_air_ticks") + 1.0);
			} else {
				putD(entity, "ai_target_air_ticks", Math.max(0.0, getD(entity, "ai_target_air_ticks") - 2.0));
			}
			if (lsTarget.isBlocking()) {
				putD(entity, "ai_target_shield_ticks", getD(entity, "ai_target_shield_ticks") + 1.0);
			} else {
				putD(entity, "ai_target_shield_ticks", Math.max(0.0, getD(entity, "ai_target_shield_ticks") - 2.0));
			}
			double lastDist = getRotPersistentDouble(entity, "ai_last_dist", distToTarget);
			putD(entity, "ai_distance_trend", Math.max(-1.0, Math.min(1.0, (lastDist - distToTarget) / 0.35)));
			if (lsTarget.isSprinting() && distToTarget > lastDist + 0.05) {
				putD(entity, "ai_target_sprint_away_ticks", getD(entity, "ai_target_sprint_away_ticks") + 1.0);
			} else {
				putD(entity, "ai_target_sprint_away_ticks", Math.max(0.0, getD(entity, "ai_target_sprint_away_ticks") - 1.0));
			}
			putD(entity, "ai_last_dist", distToTarget);
			float lastH = (float) (double) getRotPersistentDouble(entity, "ai_last_target_health", (double)lsTarget.getHealth());
			float currentH = lsTarget.getHealth();
			if (currentH < lastH) {
				String lastMove = getS(entity, "sentinel_mem_1");
				if (!lastMove.isEmpty()) {
					double currentBias = getRotPersistentDouble(entity, "ai_bias_" + lastMove, 1.0);
					putD(entity, "ai_bias_" + lastMove, Math.min(2.5, currentBias + 0.3));
					recordBiasIndexUpdate(entity, lastMove);
				}
			}
			putD(entity, "ai_last_target_health", currentH);
		}
		if (entity.tickCount % 5 == 0) {
			Set<Integer> activeIndices = getActiveBiasIndices(entity);
			if (!activeIndices.isEmpty()) {
				Set<Integer> toRemove = new HashSet<>();
				for (int i : activeIndices) {
					String bKey = "ai_bias_combo_" + i;
					double val = getRotPersistentDouble(entity, bKey, 1.0);
					if (val > 1.0) val = Math.max(1.0, val - 0.015);
					else if (val < 1.0) val = Math.min(1.0, val + 0.015);

					if (Math.abs(val - 1.0) < 0.001) {
						entity.getPersistentData().remove(bKey);
						toRemove.add(i);
					} else {
						putD(entity, bKey, val);
					}
				}
				if (!toRemove.isEmpty()) {
					activeIndices.removeAll(toRemove);
					setActiveBiasIndices(entity, activeIndices);
				}
			}
		}
		double tpComboPenalty = getD(entity, "ai_tp_combo_penalty_ticks");
		if (tpComboPenalty > 0) {
			putD(entity, "ai_tp_combo_penalty_ticks", tpComboPenalty - 1);
		}
		double fakePressureTicks = getD(entity, "ai_fake_pressure_ticks");
		if (fakePressureTicks > 0) {
			putD(entity, "ai_fake_pressure_ticks", fakePressureTicks - 1);
		}
		double attackOutcomeScore = getD(entity, "sentinel_attack_outcome_score");
		if (attackOutcomeScore > 0.0) putD(entity, "sentinel_attack_outcome_score", Math.max(0.0, attackOutcomeScore - 0.05));

		double controlledAdaptationTicks = getD(entity, "controlled_adaptation_ticks");
		if (controlledAdaptationTicks > 0) {
			putD(entity, "controlled_adaptation_ticks", controlledAdaptationTicks - 1);
		}
		double controlledAdaptationCooldown = getD(entity, "controlled_adaptation_cooldown");
		if (controlledAdaptationCooldown > 0) {
			putD(entity, "controlled_adaptation_cooldown", controlledAdaptationCooldown - 1);
		}

		if (ENABLE_CONTROLLED_ADAPTATION && controlledAdaptationTicks <= 0 && controlledAdaptationCooldown <= 0 && entity instanceof LivingEntity rotLiv && rotLiv.getHealth() >= rotLiv.getMaxHealth() * 0.75f && combatTarget instanceof LivingEntity ltTarget && ltTarget.isAlive()) {
			double distToTgt = entity.distanceTo(combatTarget);
			if (distToTgt >= 4.5 && distToTgt <= 7.5 && !ltTarget.isUsingItem() && !isTargetHighlyDangerous(ltTarget)) {
				if (rotLiv.getRandom().nextDouble() < 0.008) {
					putD(entity, "controlled_adaptation_ticks", 80.0 + rotLiv.getRandom().nextInt(41));
					putD(entity, "controlled_adaptation_cooldown", 500.0);
					String currentItem = BuiltInRegistries.ITEM.getKey(ltTarget.getMainHandItem().getItem()).toString();
					putS(entity, "controlled_adapt_start_item", currentItem);
					spawnParticles(world, net.minecraft.core.particles.ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
				}
			}
		}

		boolean targetOnPillar = false;
		if (combatTarget != null && isTargetPillaring(world, combatTarget, entity)) {
			double tdx = combatTarget.getX() - entity.getX();
			double tdz = combatTarget.getZ() - entity.getZ();
			double distSqXZ = tdx * tdx + tdz * tdz;
			double maxPillarDist = ROT_PILLAR_BACK_OFF_DISTANCE + 2.0;
			if (distSqXZ < maxPillarDist * maxPillarDist) {
				targetOnPillar = true;
			}
		}

		if (!targetOnPillar && combatTarget instanceof Player && distToTarget < ROT_PLAYER_BACK_OFF_DISTANCE && entity instanceof Mob mobBack && countNearbyAlliedRots(entity) > 0 && !canRotTankTarget(entity, combatTarget)) {
			applyFacingPreservingBackOff(entity, mobBack, combatTarget, 0.22);
		}

		if (entity instanceof Mob mob) {
			double activeAdaptTicks = getD(entity, "controlled_adaptation_ticks");
			if (ENABLE_CONTROLLED_ADAPTATION && activeAdaptTicks > 0) {
				mob.getNavigation().stop();
				mob.getLookControl().setLookAt(entity.getX() + entity.getLookAngle().x * 2.0, entity.getY() + entity.getEyeHeight(), entity.getZ() + entity.getLookAngle().z * 2.0, 0.0F, 0.0F);
				if ((entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0) {
					if (entity.onGround()) {
						entity.setDeltaMovement(0.0, entity.getDeltaMovement().y, 0.0);
					}
				}
				if (combatTarget instanceof LivingEntity ltTarget) {
					String currentItem = BuiltInRegistries.ITEM.getKey(ltTarget.getMainHandItem().getItem()).toString();
					String startItem = getS(entity, "controlled_adapt_start_item");
					boolean itemHotSwapped = !startItem.isEmpty() && !currentItem.equals(startItem);
					boolean targetDangerous = isTargetHighlyDangerous(ltTarget);
					boolean isUsingItem = ltTarget.isUsingItem();

					if (itemHotSwapped || targetDangerous || isUsingItem) {
						putD(entity, "controlled_adaptation_ticks", 0.0);
						putD(entity, "controlled_adaptation_cooldown", 500.0);
						if (world instanceof ServerLevel _sLevel) {
							_sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 15, 0.2, 0.4, 0.2, 0.05);
						}
						boolean unlockedTP = getB(entity, "unlocked_teleportation");
						double tpCd = getD(entity, K_TP_DODGE_CD);
						if (unlockedTP && tpCd <= 0) {
							tryPredictiveDodge(world, entity, ltTarget, distToTarget);
						} else {
							putB(entity, "is_blocking", true);
							putD(entity, "sentinel_block_ticks", 20.0);
						}
						return;
					}
				}
				return;
			}

			boolean isHeavyPunchingAI = false;
			try {
				isHeavyPunchingAI = entity.getEntityData().get(RotEntity.DATA_is_heavy_left_punching) || entity.getEntityData().get(RotEntity.DATA_is_heavy_right_punching);
			} catch (Exception e) {}
			if (isHeavyPunchingAI) {
				mob.getNavigation().stop();
				entity.setDeltaMovement(entity.getDeltaMovement().x * 0.05, entity.getDeltaMovement().y, entity.getDeltaMovement().z * 0.05);
			} else {
				double pathSpeed = ROT_WALK_SPEED;

				boolean isWroughtnaut = false;
				boolean isStuck = false;
				if (combatTarget instanceof LivingEntity ltTarget) {
					String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(ltTarget.getType()).toString().toLowerCase();
					if (typeKey.contains("wroughtnaut") || typeKey.contains("ferrous_wroughtnaut")) {
						isWroughtnaut = true;
						isStuck = isWroughtnautStuck(ltTarget);
					}
				}

				if (isWroughtnaut) {
					if (isStuck) {
						float tYaw = combatTarget.getYRot() * ((float) Math.PI / 180F);
						double behindX = combatTarget.getX() + Math.sin(tYaw) * 1.8;
						double behindZ = combatTarget.getZ() - Math.cos(tYaw) * 1.8;
						double behindY = combatTarget.getY();

						if (getB(entity, "unlocked_teleportation") && getD(entity, "sentinel_flank_cd") <= 0) {
							putD(entity, "sentinel_flank_cd", 50.0);
							entity.teleportTo(behindX, behindY, behindZ);
							if (world instanceof ServerLevel sLevel) {
								sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 20, 0.2, 0.5, 0.2, 0.1);
								playHostileSound(sLevel, entity, "entity.enderman.teleport", 1.0F, 1.0F);
							}
						}

						pathSpeed = ROT_RUN_SPEED * 1.4;
						if (!targetOnPillar && mob.tickCount % 2 == 0) {
							mob.getNavigation().moveTo(behindX, behindY, behindZ, pathSpeed);
						}
					} else {
						boolean isAttacking = false;
						if (combatTarget instanceof LivingEntity ltTarget) {
							isAttacking = isWroughtnautAttacking(ltTarget);
						}

						if (isAttacking) {
							boolean needsRetreat = distToTarget < 6.5;
							if (needsRetreat) {
								applyFacingPreservingBackOff(entity, mob, combatTarget, 0.28);
							} else {
								mob.getNavigation().stop();
								if ((entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0 && entity.onGround()) {
									entity.setDeltaMovement(0.0, entity.getDeltaMovement().y, 0.0);
								}
								mob.getLookControl().setLookAt(combatTarget.getX(), combatTarget.getEyeY(), combatTarget.getZ(), 30.0F, 30.0F);
							}
						} else {
							if (distToTarget > 2.5) {
								requestAsynchronousPathUpdate(mob, combatTarget, ROT_WALK_SPEED);
							} else {
								mob.getNavigation().stop();
								if (entity.onGround() && (entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0) {
									entity.setDeltaMovement(0.0, entity.getDeltaMovement().y, 0.0);
								}
							}
						}
					}
				} else if (combatTarget instanceof LivingEntity ltTarget && BuiltInRegistries.ENTITY_TYPE.getKey(ltTarget.getType()).toString().toLowerCase().contains("tormentor")) {
					double tormentorObsTicks = getD(entity, "tormentor_obs_ticks");
					double rotHp = entity instanceof LivingEntity liv ? liv.getHealth() : 20.0;
					double rotMaxHp = entity instanceof LivingEntity liv ? liv.getMaxHealth() : 20.0;
					double hpRatio = rotMaxHp > 0 ? rotHp / rotMaxHp : 1.0;
					
					boolean hasTeleport = getB(entity, "unlocked_teleportation");
					boolean hasBeam = getB(entity, "unlocked_solar_beam");
					boolean hasSonic = getB(entity, "unlocked_sonic_boom");
					boolean hasRanged = hasBeam || hasSonic;

					if (tormentorObsTicks < 80) {
						putD(entity, "tormentor_obs_ticks", tormentorObsTicks + 1.0);
						mob.getLookControl().setLookAt(combatTarget.getX(), combatTarget.getEyeY(), combatTarget.getZ(), 40.0F, 40.0F);
						if (distToTarget < 16.0) {
							applyFacingPreservingBackOff(entity, mob, combatTarget, 0.32);
						} else if (distToTarget > 24.0) {
							requestAsynchronousPathUpdate(mob, combatTarget, ROT_WALK_SPEED);
						} else {
							mob.getNavigation().stop();
							if ((entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0 && entity.onGround()) {
								entity.setDeltaMovement(0.0, entity.getDeltaMovement().y, 0.0);
							}
						}
					} else {
						boolean canKeepUp = hpRatio >= 0.70 && hasRanged && hasTeleport;
						if (!canKeepUp) {
							mob.getLookControl().setLookAt(combatTarget.getX(), combatTarget.getEyeY(), combatTarget.getZ(), 40.0F, 40.0F);
							double escapeTpCd = getD(entity, "tormentor_escape_tp_cd");
							if (escapeTpCd > 0) {
								putD(entity, "tormentor_escape_tp_cd", escapeTpCd - 1.0);
							}

							if (hasTeleport && distToTarget < 10.0 && escapeTpCd <= 0) {
								putD(entity, "tormentor_escape_tp_cd", 120.0);
								double angle = mob.getRandom().nextDouble() * Math.PI * 2;
								double tX = entity.getX() + Math.cos(angle) * 20.0;
								double tZ = entity.getZ() + Math.sin(angle) * 20.0;
								double tY = entity.getY();
								teleportEntity(entity, tX, tY, tZ);
								if (world instanceof ServerLevel sLevel) {
									sLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 25, 0.3, 0.6, 0.3, 0.1);
									playHostileSound(sLevel, entity, "entity.enderman.teleport", 1.2F, 0.8F);
								}
							} else {
								applyFacingPreservingBackOff(entity, mob, combatTarget, 0.34);
							}
						} else {
							if (distToTarget < 12.0) {
								applyFacingPreservingBackOff(entity, mob, combatTarget, 0.30);
							} else if (distToTarget > 20.0) {
								requestAsynchronousPathUpdate(mob, combatTarget, ROT_WALK_SPEED);
							} else {
								mob.getNavigation().stop();
								if ((entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0 && entity.onGround()) {
									entity.setDeltaMovement(0.0, entity.getDeltaMovement().y, 0.0);
								}
								mob.getLookControl().setLookAt(combatTarget.getX(), combatTarget.getEyeY(), combatTarget.getZ(), 30.0F, 30.0F);
							}
						}
					}
				} else {
					String assignedRole = getS(entity, "sentinel_assigned_role");
					boolean isSolo = countNearbyAlliedRots(entity) == 0;
					boolean canTank = canRotTankTarget(entity, combatTarget);

					if (isSolo || canTank || assignedRole.equals("TANK")) {
						pathSpeed = determineSmartPathSpeed(entity, mob, combatTarget, distToTarget);
						boolean rotInWater = entity instanceof LivingEntity _rliv && (_rliv.isInWater() || _rliv.isInLava());
						if (!rotInWater && !targetOnPillar) {
							if (distToTarget > 2.2) {
								requestAsynchronousPathUpdate(mob, combatTarget, pathSpeed);
							} else {
								mob.getNavigation().stop();
								if (entity.onGround() && (entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0) {
									entity.setDeltaMovement(0.0, entity.getDeltaMovement().y(), 0.0);
								}
							}
						} else if (rotInWater) {
							mob.getNavigation().stop();
						}
					} else if (assignedRole.equals("CASTER")) {
						if (distToTarget < 8.0) {
							applyFacingPreservingBackOff(entity, mob, combatTarget, 0.28);
						} else if (distToTarget > 18.0) {
							requestAsynchronousPathUpdate(mob, combatTarget, ROT_WALK_SPEED);
						} else {
							mob.getNavigation().stop();
							if ((entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0 && entity.onGround()) {
								entity.setDeltaMovement(0.0, entity.getDeltaMovement().y, 0.0);
							}
							mob.getLookControl().setLookAt(combatTarget.getX(), combatTarget.getEyeY(), combatTarget.getZ(), 30.0F, 30.0F);
						}
					} else if (assignedRole.equals("FLANKER")) {
						if (distToTarget < 3.0 && combatTarget instanceof LivingEntity lt && lt.hasLineOfSight(entity)) {
							applyFacingPreservingBackOff(entity, mob, combatTarget, 0.25);
						} else if (distToTarget > 4.0) {
							requestAsynchronousPathUpdate(mob, combatTarget, ROT_RUN_SPEED);
						} else {
							mob.getNavigation().stop();
							mob.getLookControl().setLookAt(combatTarget.getX(), combatTarget.getEyeY(), combatTarget.getZ(), 30.0F, 30.0F);
						}
					}
				}
			}

		checkTrenchAndJump(world, entity, combatTarget);

		if (!isRotChannelingAbility(entity) && combatTarget instanceof LivingEntity livTgt && getB(entity, "unlocked_teleportation")) {
			double distToTargetNow = entity.distanceTo(livTgt);
			double lastRecordedDist = getD(entity, "ai_last_dist");
			Vec3 velocity = entity.getDeltaMovement();
			Vec3 toTarget = livTgt.position().subtract(entity.position()).normalize();
			double dotProduct = velocity.x * toTarget.x + velocity.z * toTarget.z;
			boolean isPermanentlyLearned = getB(entity, "adapted_forcefield_repulsion");

			boolean isWrought = false;
			String typeKeyStr = BuiltInRegistries.ENTITY_TYPE.getKey(livTgt.getType()).toString().toLowerCase();
			if (typeKeyStr.contains("wroughtnaut") || typeKeyStr.contains("ferrous_wroughtnaut")) {
				isWrought = true;
			}

			boolean isRepelled = !isWrought && ((distToTargetNow < 14.0 && dotProduct < -0.15) || (distToTargetNow < 14.0 && distToTargetNow > lastRecordedDist + 0.15 && getD(entity, "sentinel_repulsion_push_ticks") > 5));
			if (isRepelled) {
				putD(entity, "sentinel_repulsion_push_ticks", getD(entity, "sentinel_repulsion_push_ticks") + 1);
				double neededPushTicks = isPermanentlyLearned ? 6.0 : 35.0;

				if (getD(entity, "sentinel_repulsion_push_ticks") >= neededPushTicks && getD(entity, "sentinel_flank_cd") <= 0) {
					float tYaw = livTgt.getYRot() * ((float) Math.PI / 180F);
					double behindX = livTgt.getX() + Math.sin(tYaw) * 1.5;
					double behindZ = livTgt.getZ() - Math.cos(tYaw) * 1.5;
					double behindY = livTgt.getY();

					teleportEntity(entity, behindX, behindY, behindZ);
					entity.setDeltaMovement(0, 0, 0);
					putD(entity, "sentinel_flank_cd", 80);
					putD(entity, "sentinel_repulsion_push_ticks", 0);

					if (!isPermanentlyLearned) {
						putB(entity, "adapted_forcefield_repulsion", true);
						announceLearnedAbility(entity);
					}

					if (world instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.SMOKE, behindX, behindY + 1.0, behindZ, 15, 0.3, 0.3, 0.3, 0.1);
						playHostileSound(level, behindX, behindY, behindZ, "entity.enderman.teleport", 1.2F, 0.7F);
					}
				}
			} else {
				putD(entity, "sentinel_repulsion_push_ticks", Math.max(0, getD(entity, "sentinel_repulsion_push_ticks") - 1));
			}
		}

		double combatTicks = getD(entity, "sentinel_combat_ticks");
		boolean isWardenCombatTarget = "minecraft:warden".equals(BuiltInRegistries.ENTITY_TYPE.getKey(combatTarget.getType()).toString());
		boolean unlockedSonicBoom = getB(entity, "unlocked_sonic_boom");
		boolean isFalling = !entity.onGround() || getB(entity, "is_falling_heavy") || entity.getDeltaMovement().y() < -0.2;
		boolean targetInRange = distToTarget <= SONIC_BOOM_RANGE && distToTarget >= SONIC_BOOM_MIN_DIST;
		boolean hasLos = entity instanceof LivingEntity ls ? ls.hasLineOfSight(combatTarget) : true;

		if (!isFalling && hasLos && targetInRange && !isRotChannelingAbility(entity) && ((isWardenCombatTarget && combatTicks >= 600) || unlockedSonicBoom) && getD(entity, "sentinel_warden_sonic_cooldown") <= 0 &&
			scoreAbility(getAbilityById("sonic_boom"), getCombatContext(entity, combatTarget), entity, combatTarget) > 12.0) {
			fireSuperchargedSonicBoom(world, entity, combatTarget);
			putD(entity, "sentinel_warden_sonic_cooldown", SONIC_BOOM_COOLDOWN);
			return;
		}

		boolean unlockedTP = getB(entity, "unlocked_teleportation");
		if (!isRotChannelingAbility(entity) && combatTarget instanceof LivingEntity livTarget && (unlockedSonicBoom && unlockedTP)) {
			boolean isFlying = livTarget.isFallFlying() || (!livTarget.onGround() && livTarget.getY() > entity.getY() + 2.0 && !livTarget.isInWater() && !livTarget.isInLava());
			if (isFlying) {
				double flightCd = getD(entity, "sentinel_flight_intercept_cooldown");
				if (flightCd <= 0) {
					if (livTarget instanceof Player p) {
						p.stopFallFlying();
					}
					setMotion(livTarget, Vec3.ZERO);
					setMotion(livTarget, 0, -2.5, 0);
					if (world instanceof ServerLevel level) {
						dealTrueDamageToBosses(livTarget, getBackwoodsDamage(level, "rot_seismic_slam", entity), 15.0F * (float) getAdaptationMultiplier(entity));
						double targetNewX = livTarget.getX();
						double targetNewY = livTarget.getY();
						double targetNewZ = livTarget.getZ();
						level.sendParticles(ParticleTypes.EXPLOSION, targetNewX, targetNewY, targetNewZ, 4, 0.4, 0.4, 0.4, 0.05);
						level.sendParticles(ParticleTypes.SONIC_BOOM, targetNewX, targetNewY, targetNewZ, 1, 0.1, 0.1, 0.1, 0.0);
						teleportEntity(entity, targetNewX, targetNewY + 2.0, targetNewZ);
						playHostileSound(level, targetNewX, targetNewY, targetNewZ, "entity.warden.sonic_boom", 1.2F, 0.4F);
					}
					putD(entity, "sentinel_flight_intercept_cooldown", 80);
					return;
				}
			}
		}

		if (isWither(combatTarget)) {
			double witherCd = getD(entity, "rot_wither_dialogue_cooldown");
			if (witherCd <= 0 && Math.random() < 0.015) {
				RotDialoguesProcedure.sendRandomWitherQuote(world, entity, 32.0);
				putD(entity, "rot_wither_dialogue_cooldown", 400);
			}
		}

		double dist = combatTarget.position().distanceTo(entity.position());

		int activeMode = 0;
		putD(entity, K_ADAPT_MODE, 0);

		if (ENABLE_BLOCKING && getB(entity, "is_blocking")) {
			double activeTicks = getD(entity, "rot_block_active_ticks");
			if (activeTicks <= 0) {
				putB(entity, "is_blocking", false);
			} else {
				if (entity instanceof Mob _mob2139) {
					_mob2139.getNavigation().stop();
					if (_mob2139.getDeltaMovement().y() > 0) {
						_mob2139.setDeltaMovement(_mob2139.getDeltaMovement().x(), 0.0, _mob2139.getDeltaMovement().z());
					}
				}
				handlePassengerAndGrowth(entity);
				return;
			}
		}

		int armorRipTicks = (int) (double) getD(entity, "rot_armor_rip_ticks");
		if (armorRipTicks > 0) {
			putD(entity, "rot_armor_rip_ticks", armorRipTicks - 1);
			executeArmorRipChoke(world, entity, combatTarget, armorRipTicks - 1);
			handlePassengerAndGrowth(entity);
			return;
		}

		int grappleTicks = (int) (double) getD(entity, K_GRAPPLE_TICKS);
		if (ENABLE_EXTRACTION_GRAPPLE && grappleTicks > 0) {
			putD(entity, K_GRAPPLE_TICKS, grappleTicks - 1);
			executeGrappleSiphon(world, entity, combatTarget, grappleTicks - 1);
			lockLookAtTarget(entity, combatTarget);
			handlePassengerAndGrowth(entity);
			return;
		}

		int tkTicks = (int) (double) getD(entity, K_TK_TICKS);
		if (ENABLE_TELEKINESIS && tkTicks > 0) {
			putD(entity, K_TK_TICKS, tkTicks - 1);
			executeTelekinesis(world, entity, combatTarget, tkTicks - 1);
			lockLookAtTarget(entity, combatTarget);
			handlePassengerAndGrowth(entity);
			return;
		}

		double solarCharge = getD(entity, "sentinel_solar_charge_ticks");
		double solarFire = getD(entity, "sentinel_solar_fire_ticks");

		if (solarFire > 0) {
			if (getD(entity, "sentinel_minos_stage") > 0 || getD(entity, "sentinel_minos_ticks") > 0) {
				putD(entity, "sentinel_solar_fire_ticks", 0);
				putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
				stopHostileSound(world, entity, "the_backwoods:fractus_laser", 256.0);
			} else {
				putD(entity, "sentinel_solar_fire_ticks", solarFire - 1);
				executeSentinelFaceLaserFiring(world, entity, combatTarget, (int) solarFire);
				handlePassengerAndGrowth(entity);
				if (solarFire == 1.0) {
					putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
				}
				return;
			}
		}

		if (solarCharge > 0) {
			if (getD(entity, "sentinel_minos_stage") > 0 || getD(entity, "sentinel_minos_ticks") > 0) {
				putD(entity, "sentinel_solar_charge_ticks", 0);
				putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
				stopHostileSound(world, entity, "the_backwoods:fractus_laser", 256.0);
			} else {
				putD(entity, "sentinel_solar_charge_ticks", solarCharge + 1);
				executeSentinelFaceLaserCharging(world, entity, combatTarget, (int) solarCharge);
				handlePassengerAndGrowth(entity);
				return;
			}
		}

		double cryoCharge = getD(entity, "sentinel_cryo_charge_ticks");
		double cryoFire = getD(entity, "sentinel_cryo_fire_ticks");

		if (cryoFire > 0) {
			if (getD(entity, "sentinel_minos_stage") > 0 || getD(entity, "sentinel_minos_ticks") > 0) {
				putD(entity, "sentinel_cryo_fire_ticks", 0);
				putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
				stopHostileSound(world, entity, "the_backwoods:fractus_laser", 256.0);
			} else {
				putD(entity, "sentinel_cryo_fire_ticks", cryoFire - 1);
				executeSentinelCryoLaserFiring(world, entity, combatTarget, (int) cryoFire);
				handlePassengerAndGrowth(entity);
				if (cryoFire == 1.0) {
					putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
				}
				return;
			}
		}

		if (cryoCharge > 0) {
			if (getD(entity, "sentinel_minos_stage") > 0 || getD(entity, "sentinel_minos_ticks") > 0) {
				putD(entity, "sentinel_cryo_charge_ticks", 0);
				putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
				stopHostileSound(world, entity, "the_backwoods:fractus_laser", 256.0);
			} else {
				putD(entity, "sentinel_cryo_charge_ticks", cryoCharge + 1);
				executeSentinelCryoLaserCharging(world, entity, combatTarget, (int) cryoCharge);
				handlePassengerAndGrowth(entity);
				return;
			}
		}

		double witherSkullFire = getD(entity, "sentinel_wither_skull_fire_ticks");
		if (witherSkullFire > 0) {
			putD(entity, "sentinel_wither_skull_fire_ticks", witherSkullFire - 1);
			executeSentinelWitherSkullFiring(world, entity, combatTarget, (int) witherSkullFire);
			handlePassengerAndGrowth(entity);
			return;
		}

		interceptEnderPearls(world, entity);

		handleAdaptiveEffects(world, entity, combatTarget, activeMode, dist);

		double globalCd = getD(entity, "sentinel_global_ability_cooldown");
		double coreCd = getDynamicGlobalCooldown(entity);

		if (globalCd <= 0) {
			if (combatTarget instanceof LivingEntity livTarget) {
				boolean unlockedScream = getB(entity, "unlocked_sonic_scream");
				double screamCd = getD(entity, "sentinel_sonic_scream_cooldown");

				boolean isPlayerTarget = livTarget instanceof Player;
				boolean isSurvivalPlayer = false;
				if (livTarget instanceof Player p) {
					isSurvivalPlayer = !p.isCreative() && !p.isSpectator();
				}

				boolean isFlying = false;
				if (livTarget instanceof Player p) {
					isFlying = p.isFallFlying() || p.getAbilities().flying;
				} else {
					isFlying = livTarget instanceof net.minecraft.world.entity.animal.FlyingAnimal
						|| livTarget instanceof net.minecraft.world.entity.monster.Phantom
						|| livTarget instanceof net.minecraft.world.entity.monster.Ghast
						|| livTarget instanceof net.minecraft.world.entity.monster.Vex
						|| livTarget instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
						|| isWither(livTarget)
						|| livTarget instanceof net.minecraft.world.entity.ambient.Bat;
				}

				boolean isValidScreamTarget = (isPlayerTarget && isSurvivalPlayer) || isFlying;

				if (isValidScreamTarget && unlockedScream && screamCd <= 0.0 && dist <= 24.0) {
					boolean isShielding = livTarget.isBlocking();
					boolean isHealing = livTarget.isUsingItem() || (livTarget.getHealth() < livTarget.getMaxHealth() * 0.45);
					boolean notBlind = !livTarget.hasEffect(MobEffects.BLINDNESS);

					double screamWeight = 0.0;
					if (isFlying) screamWeight += 0.40;
					if (isShielding) screamWeight += 0.35;
					if (isHealing) screamWeight += 0.30;
					if (notBlind && dist <= 16.0) screamWeight += 0.20;

					if (Math.random() < screamWeight) {
						recordAttack(entity, "sonic_scream");
						putD(entity, "sentinel_sonic_scream_ticks", 240.0);
						putD(entity, "sentinel_sonic_scream_cooldown", SONIC_SCREAM_COOLDOWN);
						putD(entity, "sentinel_global_ability_cooldown", coreCd);
						return;
					}
				}
			}

			CombatContext ctx = getCombatContext(entity, combatTarget);
			boolean totemActive = getB(entity, "sentinel_totem_active");

			List<AbilityInfo> availableAbilities = getAvailableAbilities(entity);
			AbilityInfo bestAbility = null;
			double maxScore = 0.0;
			for (AbilityInfo ab : availableAbilities) {
				if ("wither_skulls".equals(ab.id) || "omni_sonic_boom".equals(ab.id) || "solar_beam".equals(ab.id) || "cryo_beam".equals(ab.id) || "telekinesis".equals(ab.id) || "grapple".equals(ab.id)) {
					if ("wither_skulls".equals(ab.id) && shouldAvoidWitherSkulls(entity, combatTarget)) continue;
					double s = scoreAbility(ab, ctx, entity, combatTarget);
					if (s > maxScore) {
						maxScore = s;
						bestAbility = ab;
					}
				}
			}

			double rangedChance = totemActive ? 0.35 : 0.15;
			double _cTicks = getD(entity, "sentinel_combat_ticks");
			if (_cTicks > 3000.0) {
				rangedChance *= Math.max(0.1, 1.0 - ((_cTicks - 3000.0) / 12000.0));
			}

			if (maxScore > 5.0 && bestAbility != null && Math.random() < rangedChance) {
				String abId = bestAbility.id;
				if ("wither_skulls".equals(abId)) {
					recordAttack(entity, "wither_skulls");
					putD(entity, "sentinel_wither_skull_fire_ticks", 18.0);
					putB(entity, "sentinel_wither_skull_has_fired", false);
					putD(entity, "sentinel_wither_skull_cd", 60.0);
					putD(entity, "sentinel_global_ability_cooldown", coreCd);
					playHostileSound(world, entity, "entity.wither.ambient", 1.0F, 0.8F);
					return;
				} else if ("omni_sonic_boom".equals(abId)) {
					recordAttack(entity, "omni_sonic_boom");
					putD(entity, "sentinel_omni_sonic_cooldown", OMNI_SONIC_BOOM_CD);
					putD(entity, "sentinel_omni_sonic_charge_ticks", OMNI_SONIC_BOOM_ANIMATION_TICKS);
					putD(entity, "sentinel_global_ability_cooldown", coreCd);
					if (world instanceof ServerLevel level) {
						playHostileSound(level, entity, "entity.warden.sonic_charge", 1.8F, 0.45F);
					}
					return;
				} else if ("solar_beam".equals(abId) || "cryo_beam".equals(abId)) {
					recordAttack(entity, abId);
					double currentHeat = getD(entity, "sentinel_laser_heat");
					putD(entity, "sentinel_laser_heat", Math.min(200.0, currentHeat + 80.0));

					putI(entity, "sentinel_laser_target_id", combatTarget.getId());
					putD(entity, K_SOLAR_CD, SOLAR_CD + 180);
					putD(entity, "sentinel_global_ability_cooldown", coreCd);

					String combatTargetId = BuiltInRegistries.ENTITY_TYPE.getKey(combatTarget.getType()).toString();
					boolean isWardenTarget = combatTargetId.contains("warden");
					boolean isHotTarget = (combatTarget.fireImmune() && !isWardenTarget) || combatTarget.level().dimension() == net.minecraft.world.level.Level.NETHER;
					boolean unlockedSolar = getB(entity, "unlocked_solar_beam");
					boolean unlockedCryo = getB(entity, "unlocked_cryo_beam");

					if (getD(entity, "sentinel_minos_stage") == 0 && getD(entity, "sentinel_minos_ticks") == 0) {
						if ("cryo_beam".equals(abId) || (isHotTarget && unlockedCryo)) {
							putD(entity, "sentinel_cryo_charge_ticks", 1);
						} else {
							putD(entity, "sentinel_solar_charge_ticks", 1);
						}
					}
					return;
				} else if ("telekinesis".equals(abId)) {
					recordAttack(entity, "telekinesis");
					putD(entity, K_TK_TICKS, 25);
					putD(entity, K_TK_CD, TK_CD);
					putD(entity, "sentinel_global_ability_cooldown", coreCd);
					playHostileSound(world, entity, "entity.warden.heartbeat", 0.8F, 0.50F);
					return;
				} else if ("grapple".equals(abId)) {
					recordAttack(entity, "grapple");
					putD(entity, K_GRAPPLE_TICKS, 40);
					putD(entity, K_GRAPPLE_CD, GRAPPLE_CD);
					putD(entity, "sentinel_global_ability_cooldown", coreCd);
					playHostileSound(world, entity, "entity.spider.ambient", 1.2F, 0.6F);
					return;
				}
			}
		}
		double frustration = getD(entity, "sentinel_armor_frustration");
		boolean isArmorFrustrated = frustration > 20.0;
		double requiredCombatTicks = isArmorFrustrated ? 40.0 : 240.0;
		double maxRipDistance = isArmorFrustrated ? 3.2 : 2.2;

		if (!isChannelingAbility(entity) && ENABLE_ARMOR_RIP && combatTicks >= requiredCombatTicks && globalCd <= 0 && getD(entity, "rot_armor_rip_cooldown") <= 0 && dist <= maxRipDistance && combatTarget instanceof LivingEntity livingTarget && getD(entity, "sentinel_totem_inspect_ticks") <= 0) {
			if (livingTarget.getBbWidth() < entity.getBbWidth() && livingTarget.getBbHeight() < entity.getBbHeight()) {
				if (entity instanceof Mob _mob2377 && _mob2377.hasLineOfSight(livingTarget)) {
					boolean hasIndestructibleArmor = false;
					boolean hasAnyArmor = false;
					for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
						if (slot.isArmor()) {
							ItemStack armorStack = livingTarget.getItemBySlot(slot);
							if (!armorStack.isEmpty()) {
								hasAnyArmor = true;
								if (isIndestructibleArmorStack(armorStack)) {
									hasIndestructibleArmor = true;
								}
							}
						}
					}
					boolean holdsTotem = livingTarget instanceof Player pCheck && (!pCheck.getMainHandItem().isEmpty() && (pCheck.getMainHandItem().getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || BuiltInRegistries.ITEM.getKey(pCheck.getMainHandItem().getItem()).toString().equals("avaritia:infinity_totem")) || (!pCheck.getOffhandItem().isEmpty() && (pCheck.getOffhandItem().getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || BuiltInRegistries.ITEM.getKey(pCheck.getOffhandItem().getItem()).toString().equals("avaritia:infinity_totem"))));
					if (hasAnyArmor || holdsTotem) {
						double baseRipChance = holdsTotem ? 0.20 : (hasIndestructibleArmor ? ARMOR_RIP_CHANCE_INDESTRUCTIBLE : ARMOR_RIP_CHANCE_REGULAR);
						if (isArmorFrustrated) {
							baseRipChance = 0.95;
						}
						double contextMultiplier = 1.0;
						if (livingTarget.isUsingItem()) contextMultiplier *= 5.0;
						if (livingTarget.getHealth() < livingTarget.getMaxHealth() * 0.35) contextMultiplier *= 3.0;
						double ripChance = isArmorFrustrated ? 0.95 : Math.min(0.60, baseRipChance * contextMultiplier);
						if (Math.random() < ripChance) {
							putD(entity, "sentinel_armor_frustration", Math.max(0.0, frustration - 25.0));
							putD(entity, "rot_armor_rip_ticks", ARMOR_RIP_TICKS);
							putB(entity, "is_armor_ripping", true);
							putD(entity, "rot_choke_locked_yaw", entity.getYRot());
							putD(entity, "rot_armor_rip_cooldown", ARMOR_RIP_COOLDOWN);
							putD(entity, "sentinel_global_ability_cooldown", coreCd);
							putD(entity, "rot_choke_hits_taken", 0);
							boolean totemActive = getB(entity, "sentinel_totem_active");
							int minHits = totemActive ? CHOKE_TOTEM_MIN_HITS : CHOKE_MIN_HITS;
							int maxHits = totemActive ? CHOKE_TOTEM_MAX_HITS : CHOKE_MAX_HITS;
							int requiredHits = minHits + net.minecraft.util.RandomSource.create().nextInt(maxHits - minHits + 1);
							putD(entity, "rot_choke_break_hits", requiredHits);

							if (combatTarget instanceof LivingEntity livTarget && livTarget.isBlocking()) {
								disablePlayerShield(world, livTarget, 30.0, 100);
							}

							playHostileSound(world, entity, "entity.warden.sniff", 1.2F, 0.5F);
							return;
						}
					}
				}
			}
		}

		if (ENABLE_EXTRACTION_GRAPPLE && !isRotChannelingAbility(entity) && globalCd <= 0 && getB(entity, "unlocked_grapple") && getD(entity, K_GRAPPLE_CD) <= 0 &&
			dist <= getDynamicRangeThreshold(entity, "grapple", 8.5, 1.5) && scoreAbility(getAbilityById("grapple"), getCombatContext(entity, combatTarget), entity, combatTarget) > 14.0) {
			putD(entity, K_GRAPPLE_TICKS, 32);
			putD(entity, K_GRAPPLE_CD, GRAPPLE_CD);
			putD(entity, "sentinel_global_ability_cooldown", coreCd);
			playHostileSound(world, entity, "entity.warden.sniff", 1.1F, 0.85F);
			return;
		}

		tryPredictiveDodge(world, entity, combatTarget, dist);
		tryFlankTeleport(world, entity, combatTarget, dist);

		double meleeWindup = getD(entity, "sentinel_melee_windup");
		double meleeCooldown = getD(entity, "sentinel_melee_cooldown");

		if (meleeWindup > 0) {
			lockLookAtTarget(entity, combatTarget);
			spawnParticles(world, ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.2, entity.getZ(), 2, 0.3, 0.3, 0.3, 0.05);
			if (meleeWindup == 1) {
				executeSentinelPunch(world, entity, combatTarget);
				putD(entity, "sentinel_melee_cooldown", 20);
			}
		} else if (!getB(entity, "sentinel_waiting_intercept") && getD(entity, "ai_fake_pressure_ticks") <= 0 && meleeCooldown <= 0 && dist <= ((combatTarget instanceof Player) ? 2.5 : 3.2)) {
			putD(entity, "sentinel_melee_windup", 8);
			playHostileSound(world, entity, "entity.player.attack.weak", 0.9F, 0.85F);
			spawnParticles(world, ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.2, entity.getZ(), 5, 0.4, 0.4, 0.4, 0.1);
		}

		if (combatTarget != null) {
			lockLookAtTarget(entity, combatTarget);
		}

		handleForwardCarveMining(world, entity, combatTarget);
		handlePassengerAndGrowth(entity);
	}
	}

	private static void handleAdaptiveEffects(LevelAccessor world, Entity self, Entity target, int mode, double dist) {
		if (!(world instanceof ServerLevel level) || !(self instanceof LivingEntity ls)) return;

		if (mode == 1) {
			BlockPos centerPos = BlockPos.containing(ls.position());
			for (BlockPos bp : BlockPos.betweenClosed(centerPos.offset(-2, -1, -2), centerPos.offset(2, 1, 2))) {
				BlockState st = level.getBlockState(bp);
				if (st.is(net.minecraft.world.level.block.Blocks.ICE) || st.is(net.minecraft.world.level.block.Blocks.SNOW)) {
					level.destroyBlock(bp, false);
				}
			}

			if (dist <= 1.5 && self.tickCount % 4 == 0) {
				target.setRemainingFireTicks(60);
				dealTrueDamageToBosses(target, getBackwoodsDamage(level, "rot_inferno_laser", self), 4.0F * (float) getAdaptationMultiplier(self));
			}

			ls.setRemainingFireTicks(0);
			ls.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

		} else if (mode == 2) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, ls.getX(), ls.getY() + 1.3, ls.getZ(), 1, 0.4, 0.5, 0.4, 0.005);

			if (self.tickCount % 8 == 0) {
				List<Entity> nearby = level.getEntitiesOfClass(Entity.class, new AABB(self.position(), self.position()).inflate(6.0), e -> e != self && (e instanceof LivingEntity));
				for (Entity ent : nearby) {
					if (ent instanceof Player p) {
						if (p instanceof ServerPlayer sp) {
							if (sp.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SURVIVAL) continue;
						} else {
							if (p.isCreative() || p.isSpectator()) continue;
						}
					}
					if (ent instanceof LivingEntity liv) {
					}
				}
			}
		}
	}

	private static void disablePlayerShield(LevelAccessor world, LivingEntity victim, double incomingDamage, int cooldownTicks) {
		if (victim instanceof Player player && player.isBlocking()) {
			ItemStack shield = player.getUseItem();
			if (shield.isEmpty() || !shield.isDamageableItem()) {
				if (player.getOffhandItem().isDamageableItem() && player.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD)) {
					shield = player.getOffhandItem();
				} else if (player.getMainHandItem().isDamageableItem() && player.getMainHandItem().is(net.minecraft.world.item.Items.SHIELD)) {
					shield = player.getMainHandItem();
				}
			}

			if (!shield.isEmpty() && shield.isDamageableItem()) {
				int maxDurability = shield.getMaxDamage();
				double variance = 0.85 + (Math.random() * 0.5);
				double durabilityChunk = maxDurability > 0 ? (maxDurability * (0.12 + Math.random() * 0.08)) : 25.0;
				double calculatedDmg = (incomingDamage * 1.5 * variance) + durabilityChunk;
				int shieldDamage = Math.max(35, (int) Math.round(calculatedDmg));

				if (world instanceof ServerLevel sLevel && player instanceof net.minecraft.server.level.ServerPlayer sPlayer) {
					shield.hurtAndBreak(shieldDamage, sLevel, sPlayer, (item) -> {
						sLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
							net.minecraft.sounds.SoundEvents.SHIELD_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.75F);
					});
				} else {
					shield.setDamageValue(shield.getDamageValue() + shieldDamage);
					if (maxDurability > 0 && shield.getDamageValue() >= maxDurability) {
						shield.shrink(1);
						player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
							net.minecraft.sounds.SoundEvents.SHIELD_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.75F);
					}
				}
				if (!shield.isEmpty()) {
					player.getCooldowns().addCooldown(shield.getItem(), cooldownTicks);
				}
			}

			player.stopUsingItem();
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				net.minecraft.sounds.SoundEvents.SHIELD_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
			if (player.level() instanceof ServerLevel sLevel) {
				sLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getEyeY(), player.getZ(), 14, 0.25, 0.25, 0.25, 0.12);
			}
		}
	}

	private static void disablePlayerShield(LivingEntity victim, int cooldownTicks) {
		disablePlayerShield(victim != null ? victim.level() : null, victim, 25.0, cooldownTicks);
	}

	private static Vec3 updateRotLaserAim(Entity entity, Vec3 facePos, Entity target, boolean firing) {
		double curAimX = getD(entity, "sentinel_laser_aim_x");
		double curAimY = getD(entity, "sentinel_laser_aim_y");
		double curAimZ = getD(entity, "sentinel_laser_aim_z");
		Vec3 currentAim = (curAimX != 0.0 || curAimY != 0.0 || curAimZ != 0.0)
			? new Vec3(curAimX, curAimY, curAimZ).normalize()
			: Vec3.ZERO;

		Vec3 targetPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
		Vec3 desiredAim = targetPos.subtract(facePos);
		if (desiredAim.lengthSqr() < 0.0001) {
			desiredAim = entity.getLookAngle();
		}
		desiredAim = desiredAim.normalize();

		if (currentAim.lengthSqr() < 0.0001) {
			putD(entity, "sentinel_laser_aim_x", desiredAim.x);
			putD(entity, "sentinel_laser_aim_y", desiredAim.y);
			putD(entity, "sentinel_laser_aim_z", desiredAim.z);
			return desiredAim;
		}

		double turnRate = firing ? 0.055 : 0.095;
		Vec3 newAim = currentAim.lerp(desiredAim, turnRate).normalize();
		putD(entity, "sentinel_laser_aim_x", newAim.x);
		putD(entity, "sentinel_laser_aim_y", newAim.y);
		putD(entity, "sentinel_laser_aim_z", newAim.z);

		if (entity instanceof Mob mob) {
			double dx = newAim.x;
			double dy = newAim.y;
			double dz = newAim.z;
			double dh = Math.sqrt(dx * dx + dz * dz);
			float targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
			float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
			mob.setYRot(targetYRot);
			mob.setXRot(targetXRot);
			mob.setYHeadRot(targetYRot);
			mob.yBodyRot = targetYRot;
		}

		return newAim;
	}

	private static void executeSentinelFaceLaserCharging(LevelAccessor world, Entity entity, Entity target, int chargeTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		if (target != null) {
			updateRotLaserAim(entity, facePos, target, false);
		}

		net.minecraft.core.particles.ParticleOptions beamPart = getAdaptiveBeamParticle(entity);

		double progress = (double) chargeTicks / 40.0;
		double radius = 1.9 - progress * 1.6;
		int particleCount = 2 + (int) (progress * 5.0);
		for (int i = 0; i < particleCount; i++) {
			double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
			double yOffset = (entity.getRandom().nextDouble() - 0.5) * radius * 0.5;
			Vec3 pPos = facePos.add(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
			level.sendParticles(beamPart, pPos.x, pPos.y, pPos.z, 1, 0.01, 0.01, 0.01, 0.0);
		}

		level.sendParticles(ParticleTypes.LAVA, facePos.x, facePos.y, facePos.z, 1, 0.15, 0.15, 0.15, 0.01);

		if (chargeTicks % 6 == 0) {
			playHostileSound(level, entity, "item.firecharge.use", 0.6F, 1.5F);
		}

		if (chargeTicks >= 40) {
			putD(entity, "sentinel_solar_charge_ticks", 0);
			putD(entity, "sentinel_solar_fire_ticks", 123);
			playHostileSound(level, entity, "the_backwoods:fractus_laser", 4.5F, 0.65F);
		}
	}

	private static void executeSentinelFaceLaserFiring(LevelAccessor world, Entity entity, Entity target, int fireTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		int targetId = (int) getD(entity, "sentinel_laser_target_id");
		if (targetId == 0) {
			targetId = getI(entity, "sentinel_laser_target_id");
		}
		if (targetId != 0) {
			Entity storedTarget = level.getEntity(targetId);
			if (storedTarget != null && storedTarget.isAlive()) {
				target = storedTarget;
			}
		}

		if (target == null || !target.isAlive()) {
			putD(entity, "sentinel_solar_fire_ticks", 0);
			putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			putD(entity, K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			entity.getPersistentData().remove("sentinel_laser_target_id");
			entity.getPersistentData().remove("sentinel_laser_aim_x");
			entity.getPersistentData().remove("sentinel_laser_aim_y");
			entity.getPersistentData().remove("sentinel_laser_aim_z");
			stopHostileSound(level, entity, "the_backwoods:fractus_laser", 256.0);
			return;
		}

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		Vec3 direction = updateRotLaserAim(entity, facePos, target, true);

		double maxRange = 96.0;
		Vec3 beamEnd = facePos.add(direction.scale(maxRange));

		BlockHitResult blockHit = level.clip(new ClipContext(facePos, beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
		Vec3 firstIntersectionPos = beamEnd;
		boolean hitBlock = false;
		if (blockHit.getType() != HitResult.Type.MISS) {
			firstIntersectionPos = blockHit.getLocation();
			hitBlock = true;
		}

		AABB searchRange = new AABB(facePos, firstIntersectionPos).inflate(1.5);
		List<Entity> possibleEntities = level.getEntitiesOfClass(Entity.class, searchRange, e -> e != entity && e != living && e.isAlive());

		Entity hitEntity = null;
		double closestDist = facePos.distanceTo(firstIntersectionPos);
		Vec3 finalBeamEnd = firstIntersectionPos;

		for (Entity possible : possibleEntities) {
			if (possible instanceof Player p && p.isCreative()) continue;
			if (isContraptionEntity(possible)) {
				destroyContraption(level, entity, possible, possible.position(), true);
				continue;
			}
			if (possible instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl pearl) {
				level.sendParticles(ParticleTypes.EXPLOSION, pearl.getX(), pearl.getY(), pearl.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
				playHostileSound(level, pearl, "entity.generic.explode", 0.8F, 1.8F);
				pearl.discard();
				continue;
			}
			AABB possibleBb = possible.getBoundingBox().inflate(0.3);
			java.util.Optional<Vec3> clipResult = possibleBb.clip(facePos, firstIntersectionPos);
			if (clipResult.isPresent()) {
				double distToClip = facePos.distanceTo(clipResult.get());
				if (distToClip < closestDist) {
					closestDist = distToClip;
					finalBeamEnd = clipResult.get();
					hitEntity = possible;
					hitBlock = false;
				}
			}
		}

		beamEnd = finalBeamEnd;

		net.minecraft.core.particles.ParticleOptions beamPart = getAdaptiveBeamParticle(entity);

		for (int i = 0; i < 4; i++) {
			double oX = (living.getRandom().nextDouble() - 0.5) * 0.42;
			double oY = (living.getRandom().nextDouble() - 0.5) * 0.42;
			double oZ = (living.getRandom().nextDouble() - 0.5) * 0.42;
			level.sendParticles(beamPart, facePos.x + oX, facePos.y + oY, facePos.z + oZ, 1, 0.01, 0.01, 0.01, 0.0);
		}
		if (living.getRandom().nextFloat() < 0.25F) {
			level.sendParticles(ParticleTypes.LAVA, facePos.x, facePos.y, facePos.z, 1, 0.2, 0.2, 0.2, 0.1);
		}

		double spacing = (0.22 / Mth.clamp(PARTICLE_QUALITY, 0.1, 1.0));
		Vec3 upVec = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 sideVec = direction.cross(upVec).normalize();
		Vec3 vertVec = direction.cross(sideVec).normalize();
		double time = level.getGameTime() * 0.38;
		double beamLength = beamEnd.distanceTo(facePos);

		for (double d = 0.0; d <= beamLength; d += spacing) {
			Vec3 pos = facePos.add(direction.scale(d));

			level.sendParticles(beamPart, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);

			if (!level.getFluidState(BlockPos.containing(pos)).isEmpty()) {
				if (level.getRandom().nextFloat() < 0.3F) {
					level.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.02);
				}
			}

			double angle = (d * 5.0) + time;
			double cylinderRadius = 0.15;

			if (level.getRandom().nextFloat() < 0.70F) {
				Vec3 offset1 = sideVec.scale(Math.cos(angle) * cylinderRadius).add(vertVec.scale(Math.sin(angle) * cylinderRadius));
				Vec3 p1 = pos.add(offset1);
				level.sendParticles(beamPart, p1.x, p1.y, p1.z, 1, 0.01, 0.01, 0.01, 0.0);

				Vec3 offset2 = sideVec.scale(Math.cos(angle + Math.PI) * cylinderRadius).add(vertVec.scale(Math.sin(angle + Math.PI) * cylinderRadius));
				Vec3 p2 = pos.add(offset2);
				level.sendParticles(beamPart, p2.x, p2.y, p2.z, 1, 0.01, 0.01, 0.01, 0.0);
			}
		}

		if (hitBlock && blockHit.getType() == HitResult.Type.BLOCK) {
			BlockPos hitPos = blockHit.getBlockPos();
			BlockState hitState = level.getBlockState(hitPos);
			if (isGunOrRadarBlock(hitState)) {
				destroyScorchedGunOrRadarBlock(level, entity, hitPos, hitState);
			} else {
				float hardness = hitState.getDestroySpeed(level, hitPos);
				if (hardness >= 0.0F && hardness <= 50.0F) {
					int lastDrillX = getI(entity, "sentinel_laser_drill_x");
					int lastDrillY = getI(entity, "sentinel_laser_drill_y");
					int lastDrillZ = getI(entity, "sentinel_laser_drill_z");
					double drillProgress = getD(entity, "sentinel_laser_drill_progress");

					if (lastDrillX != hitPos.getX() || lastDrillY != hitPos.getY() || lastDrillZ != hitPos.getZ()) {
						drillProgress = 0.0;
						putI(entity, "sentinel_laser_drill_x", hitPos.getX());
						putI(entity, "sentinel_laser_drill_y", hitPos.getY());
						putI(entity, "sentinel_laser_drill_z", hitPos.getZ());
					}

					double progressAdd = 1.0 / Math.max(1.0, hardness * 2.5);
					if (getB(entity, "sentinel_totem_active")) {
						progressAdd *= 3.0;
					}
					drillProgress += progressAdd;
					putD(entity, "sentinel_laser_drill_progress", drillProgress);

					if (level.getRandom().nextFloat() < 0.45F) {
						level.sendParticles(ParticleTypes.CRIT, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
						level.sendParticles(ParticleTypes.LAVA, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
					}

					if (drillProgress >= 1.0) {
						level.destroyBlock(hitPos, false);
						playHostileSound(level, hitPos, "entity.item.break", 0.8F, 0.8F);

						for (int dy = -1; dy <= 2; dy++) {
							for (int dx = -1; dx <= 1; dx++) {
								for (int dz = -1; dz <= 1; dz++) {
									if (dx*dx + dy*dy + dz*dz <= 2.5) {
										BlockPos adj = hitPos.offset(dx, dy, dz);
										BlockState adjState = level.getBlockState(adj);
										if (isGunOrRadarBlock(adjState)) {
											destroyScorchedGunOrRadarBlock(level, entity, adj, adjState);
										} else {
											float adjHard = adjState.getDestroySpeed(level, adj);
											if (adjHard >= 0.0F && adjHard <= hardness + 1.5F && !adjState.isAir()) {
												level.destroyBlock(adj, false);
											}
										}
									}
								}
							}
						}
						putD(entity, "sentinel_laser_drill_progress", 0.0);
					}
				}
			}

			BlockPos firePos = hitPos.relative(blockHit.getDirection());
			if (level.isEmptyBlock(firePos) && level.getBlockState(firePos.below()).isSolidRender(level, firePos.below())) {
				level.setBlock(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
			}
		}

		if (hitBlock && level.getRandom().nextFloat() < 0.3F) {
			BlockPos nearBeam = BlockPos.containing(
				beamEnd.x + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.y + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.z + (level.getRandom().nextDouble() - 0.5) * 4.0
			);
			if (level.isEmptyBlock(nearBeam) && level.getBlockState(nearBeam.below()).isSolidRender(level, nearBeam.below())) {
				level.setBlock(nearBeam, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
			}
		}

		if (fireTicks % 3 == 0) {
			level.sendParticles(ParticleTypes.EXPLOSION, beamEnd.x, beamEnd.y, beamEnd.z, 1, 0.1, 0.1, 0.1, 0.05);
		}

		level.sendParticles(ParticleTypes.DRIPPING_LAVA, beamEnd.x, beamEnd.y, beamEnd.z, 3, 0.1, 0.1, 0.1, 0.02);

		Entity targetVictim = (hitEntity != null) ? hitEntity : target;
		if (targetVictim instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl pearl && pearl.isAlive()) {
			level.sendParticles(ParticleTypes.EXPLOSION, pearl.getX(), pearl.getY(), pearl.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
			level.sendParticles(ParticleTypes.LAVA, pearl.getX(), pearl.getY(), pearl.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
			playHostileSound(level, pearl, "entity.generic.explode", 0.8F, 1.8F);
			pearl.discard();
		}

		if (entity.tickCount % 6 == 0) {
			if (targetVictim instanceof LivingEntity livVictim && livVictim.isAlive() && facePos.distanceTo(livVictim.position()) <= maxRange) {
				if (livVictim instanceof Player p) {
					if (p instanceof ServerPlayer sp) {
						if (sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE || sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
							targetVictim = null;
						}
					}
				}
				if (targetVictim != null) {
					String victimId = BuiltInRegistries.ENTITY_TYPE.getKey(targetVictim.getType()).toString();
					boolean isColdEntity = victimId.contains("stray") || victimId.contains("snow_golem") || victimId.contains("snowman") || (targetVictim instanceof LivingEntity lv && lv.getTicksFrozen() > 0);
					float sonicDmg = 8.0F;
					float explosionDmg = 4.0F;
					if (isColdEntity) {
						sonicDmg = 18.0F;
						explosionDmg = 8.0F;
					}
					if (getB(entity, "sentinel_totem_active")) {
						sonicDmg *= 2.0F;
						explosionDmg *= 2.0F;
					}
					float finalLaserDmg = Math.min(20.0F, (sonicDmg + explosionDmg) * (float) getAdaptationMultiplier(entity));
					dealTrueDamageToBosses(targetVictim, getBackwoodsDamage(level, "rot_inferno_laser", entity), finalLaserDmg);
					targetVictim.setRemainingFireTicks(120);

					Vec3 push = targetVictim.position().subtract(entity.position()).normalize();
					double[] smartKb = calculateSmartLaserKnockback(entity, targetVictim, true);
					targetVictim.setDeltaMovement(push.x * smartKb[0], smartKb[1], push.z * smartKb[0]);

					breakBlocksBehindTarget(level, targetVictim, push, getB(entity, "sentinel_totem_active"));
				}
			}
		}

		if (fireTicks <= 1) {
			putD(entity, "sentinel_solar_fire_ticks", 0);
			putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			putD(entity, K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			entity.getPersistentData().remove("sentinel_laser_target_id");
			stopHostileSound(level, entity, "the_backwoods:fractus_laser", 256.0);
		}
	}

	private static void executeSentinelCryoLaserCharging(LevelAccessor world, Entity entity, Entity target, int chargeTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		if (target != null) {
			updateRotLaserAim(entity, facePos, target, false);
		}

		double progress = (double) chargeTicks / 40.0;
		double radius = 1.9 - progress * 1.6;
		int particleCount = 2 + (int) (progress * 5.0);
		for (int i = 0; i < particleCount; i++) {
			double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
			double yOffset = (entity.getRandom().nextDouble() - 0.5) * radius * 0.5;
			Vec3 pPos = facePos.add(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
			level.sendParticles(ParticleTypes.SNOWFLAKE, pPos.x, pPos.y, pPos.z, 1, 0.01, 0.01, 0.01, 0.0);
		}

		level.sendParticles(ParticleTypes.INSTANT_EFFECT, facePos.x, facePos.y, facePos.z, 1, 0.15, 0.15, 0.15, 0.01);

		if (chargeTicks % 6 == 0) {
			playHostileSound(level, entity, "block.powder_snow.break", 1.2F, 0.8F);
		}

		if (chargeTicks >= 40) {
			putD(entity, "sentinel_cryo_charge_ticks", 0);
			putD(entity, "sentinel_cryo_fire_ticks", 123);
			playHostileSound(level, entity, "the_backwoods:fractus_laser", 4.5F, 1.25F);
		}
	}

	private static void executeSentinelCryoLaserFiring(LevelAccessor world, Entity entity, Entity target, int fireTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		int targetId = (int) getD(entity, "sentinel_laser_target_id");
		if (targetId == 0) {
			targetId = getI(entity, "sentinel_laser_target_id");
		}
		if (targetId != 0) {
			Entity storedTarget = level.getEntity(targetId);
			if (storedTarget != null && storedTarget.isAlive()) {
				target = storedTarget;
			}
		}

		if (target == null || !target.isAlive()) {
			putD(entity, "sentinel_cryo_fire_ticks", 0);
			putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			putD(entity, K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			entity.getPersistentData().remove("sentinel_laser_target_id");
			entity.getPersistentData().remove("sentinel_laser_aim_x");
			entity.getPersistentData().remove("sentinel_laser_aim_y");
			entity.getPersistentData().remove("sentinel_laser_aim_z");
			stopHostileSound(level, entity, "the_backwoods:fractus_laser", 256.0);
			return;
		}

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		Vec3 direction = updateRotLaserAim(entity, facePos, target, true);

		double maxRange = 96.0;
		Vec3 beamEnd = facePos.add(direction.scale(maxRange));

		BlockHitResult blockHit = level.clip(new ClipContext(facePos, beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
		Vec3 firstIntersectionPos = beamEnd;
		boolean hitBlock = false;
		if (blockHit.getType() != HitResult.Type.MISS) {
			firstIntersectionPos = blockHit.getLocation();
			hitBlock = true;
		}

		AABB searchRange = new AABB(facePos, firstIntersectionPos).inflate(1.5);
		List<Entity> possibleEntities = level.getEntitiesOfClass(Entity.class, searchRange, e -> e != entity && e != living && e.isAlive());

		Entity hitEntity = null;
		double closestDist = facePos.distanceTo(firstIntersectionPos);
		Vec3 finalBeamEnd = firstIntersectionPos;

		for (Entity possible : possibleEntities) {
			if (possible instanceof Player p && p.isCreative()) continue;
			if (isContraptionEntity(possible)) {
				destroyContraption(level, entity, possible, possible.position(), true);
				continue;
			}
			if (possible instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl pearl) {
				level.sendParticles(ParticleTypes.EXPLOSION, pearl.getX(), pearl.getY(), pearl.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
				playHostileSound(level, pearl, "entity.generic.explode", 0.8F, 1.8F);
				pearl.discard();
				continue;
			}
			AABB possibleBb = possible.getBoundingBox().inflate(0.3);
			java.util.Optional<Vec3> clipResult = possibleBb.clip(facePos, firstIntersectionPos);
			if (clipResult.isPresent()) {
				double distToClip = facePos.distanceTo(clipResult.get());
				if (distToClip < closestDist) {
					closestDist = distToClip;
					finalBeamEnd = clipResult.get();
					hitEntity = possible;
					hitBlock = false;
				}
			}
		}

		beamEnd = finalBeamEnd;

		for (int i = 0; i < 4; i++) {
			double oX = (living.getRandom().nextDouble() - 0.5) * 0.42;
			double oY = (living.getRandom().nextDouble() - 0.5) * 0.42;
			double oZ = (living.getRandom().nextDouble() - 0.5) * 0.42;
			level.sendParticles(ParticleTypes.SNOWFLAKE, facePos.x + oX, facePos.y + oY, facePos.z + oZ, 1, 0.01, 0.01, 0.01, 0.0);
		}
		if (living.getRandom().nextFloat() < 0.25F) {
			level.sendParticles(ParticleTypes.INSTANT_EFFECT, facePos.x, facePos.y, facePos.z, 2, 0.2, 0.2, 0.2, 0.1);
		}

		double spacing = (0.22 / Mth.clamp(PARTICLE_QUALITY, 0.1, 1.0));
		Vec3 upVec = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 sideVec = direction.cross(upVec).normalize();
		Vec3 vertVec = direction.cross(sideVec).normalize();
		double time = level.getGameTime() * 0.38;
		double beamLength = beamEnd.distanceTo(facePos);

		for (double d = 0.0; d <= beamLength; d += spacing) {
			Vec3 pos = facePos.add(direction.scale(d));

			level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);

			if (!level.getFluidState(BlockPos.containing(pos)).isEmpty()) {
				if (level.getRandom().nextFloat() < 0.3F) {
					level.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.02);
				}
			}

			double angle = (d * 5.0) + time;
			double cylinderRadius = 0.15;

			if (level.getRandom().nextFloat() < 0.70F) {
				Vec3 offset1 = sideVec.scale(Math.cos(angle) * cylinderRadius).add(vertVec.scale(Math.sin(angle) * cylinderRadius));
				Vec3 p1 = pos.add(offset1);
				level.sendParticles(ParticleTypes.INSTANT_EFFECT, p1.x, p1.y, p1.z, 1, 0.01, 0.01, 0.01, 0.0);

				Vec3 offset2 = sideVec.scale(Math.cos(angle + Math.PI) * cylinderRadius).add(vertVec.scale(Math.sin(angle + Math.PI) * cylinderRadius));
				Vec3 p2 = pos.add(offset2);
				level.sendParticles(ParticleTypes.INSTANT_EFFECT, p2.x, p2.y, p2.z, 1, 0.01, 0.01, 0.01, 0.0);
			}
		}

		if (hitBlock && blockHit.getType() == HitResult.Type.BLOCK) {
			BlockPos hitPos = blockHit.getBlockPos();
			BlockState hitState = level.getBlockState(hitPos);
			if (isGunOrRadarBlock(hitState)) {
				destroyScorchedGunOrRadarBlock(level, entity, hitPos, hitState);
			} else {
				float hardness = hitState.getDestroySpeed(level, hitPos);
				if (hardness >= 0.0F && hardness <= 50.0F) {
					int lastDrillX = getI(entity, "sentinel_laser_drill_x");
					int lastDrillY = getI(entity, "sentinel_laser_drill_y");
					int lastDrillZ = getI(entity, "sentinel_laser_drill_z");
					double drillProgress = getD(entity, "sentinel_laser_drill_progress");

					if (lastDrillX != hitPos.getX() || lastDrillY != hitPos.getY() || lastDrillZ != hitPos.getZ()) {
						drillProgress = 0.0;
						putI(entity, "sentinel_laser_drill_x", hitPos.getX());
						putI(entity, "sentinel_laser_drill_y", hitPos.getY());
						putI(entity, "sentinel_laser_drill_z", hitPos.getZ());
					}

					double progressAdd = 1.0 / Math.max(1.0, hardness * 2.5);
					if (getB(entity, "sentinel_totem_active")) {
						progressAdd *= 3.0;
					}
					drillProgress += progressAdd;
					putD(entity, "sentinel_laser_drill_progress", drillProgress);

					if (level.getRandom().nextFloat() < 0.45F) {
						level.sendParticles(ParticleTypes.SNOWFLAKE, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
						level.sendParticles(ParticleTypes.INSTANT_EFFECT, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
					}

					if (drillProgress >= 1.0) {
						level.destroyBlock(hitPos, false);
						playHostileSound(level, hitPos, "block.glass.break", 1.0F, 1.2F);

						for (int dy = -1; dy <= 2; dy++) {
							for (int dx = -1; dx <= 1; dx++) {
								for (int dz = -1; dz <= 1; dz++) {
									if (dx*dx + dy*dy + dz*dz <= 2.5) {
										BlockPos adj = hitPos.offset(dx, dy, dz);
										BlockState adjState = level.getBlockState(adj);
										if (isGunOrRadarBlock(adjState)) {
											destroyScorchedGunOrRadarBlock(level, entity, adj, adjState);
										} else {
											float adjHard = adjState.getDestroySpeed(level, adj);
											if (adjHard >= 0.0F && adjHard <= hardness + 1.5F && !adjState.isAir()) {
												level.destroyBlock(adj, false);
											}
										}
									}
								}
							}
						}
						putD(entity, "sentinel_laser_drill_progress", 0.0);
					}
				}
			}

			BlockPos surfacePos = hitPos.relative(blockHit.getDirection());
			if (level.isEmptyBlock(surfacePos) && level.getBlockState(surfacePos.below()).isSolidRender(level, surfacePos.below())) {
				level.setBlock(surfacePos, net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState(), 3);
			}
		}

		if (hitBlock && level.getRandom().nextFloat() < 0.3F) {
			BlockPos nearBeam = BlockPos.containing(
				beamEnd.x + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.y + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.z + (level.getRandom().nextDouble() - 0.5) * 4.0
			);
			if (level.isEmptyBlock(nearBeam) && level.getBlockState(nearBeam.below()).isSolidRender(level, nearBeam.below())) {
				level.setBlock(nearBeam, net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState(), 3);
			}
		}

		if (fireTicks % 3 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, beamEnd.x, beamEnd.y, beamEnd.z, 3, 0.15, 0.15, 0.15, 0.02);
		}

		Entity targetVictim = (hitEntity != null) ? hitEntity : target;
		if (targetVictim instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl pearl && pearl.isAlive()) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, pearl.getX(), pearl.getY(), pearl.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
			level.sendParticles(ParticleTypes.INSTANT_EFFECT, pearl.getX(), pearl.getY(), pearl.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
			playHostileSound(level, pearl, "block.glass.break", 1.0F, 1.5F);
			pearl.discard();
		}

		if (entity.tickCount % 6 == 0) {
			if (targetVictim instanceof LivingEntity livVictim && livVictim.isAlive() && facePos.distanceTo(livVictim.position()) <= maxRange) {
				if (livVictim instanceof Player p) {
					if (p instanceof ServerPlayer sp) {
						if (sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE || sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
							targetVictim = null;
						}
					}
				}
				if (targetVictim != null) {
					float dmg = 8.0F;
					String victimId = BuiltInRegistries.ENTITY_TYPE.getKey(targetVictim.getType()).toString();
					boolean isNetherEntity = (targetVictim.fireImmune() && !victimId.contains("warden")) || targetVictim.level().dimension() == net.minecraft.world.level.Level.NETHER || victimId.contains("piglin") || victimId.contains("wither_skeleton");
					if (isNetherEntity) {
						dmg = 18.0F;
					}
					if (getB(entity, "sentinel_totem_active")) {
						dmg *= 2.0F;
					}
					float finalCryoDmg = Math.min(20.0F, dmg * (float) getAdaptationMultiplier(entity));
					dealTrueDamageToBosses(targetVictim, getBackwoodsDamage(level, "rot_cryo_laser", entity), finalCryoDmg);
					((LivingEntity) targetVictim).setTicksFrozen(((LivingEntity) targetVictim).getTicksFrozen() + 180);
					applyEffectSafe((LivingEntity) targetVictim, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 4, false, false));
					playHostileSound(level, targetVictim, "entity.player.hurt_freeze", 1.2F, 0.8F);

					Vec3 push = targetVictim.position().subtract(entity.position()).normalize();
					double[] smartKb = calculateSmartLaserKnockback(entity, targetVictim, false);
					targetVictim.setDeltaMovement(push.x * smartKb[0], smartKb[1], push.z * smartKb[0]);

					breakBlocksBehindTarget(level, targetVictim, push, getB(entity, "sentinel_totem_active"));
				}
			}
		}

		if (fireTicks <= 1) {
			putD(entity, "sentinel_cryo_fire_ticks", 0);
			putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			putD(entity, K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			entity.getPersistentData().remove("sentinel_laser_target_id");
			stopHostileSound(level, entity, "the_backwoods:fractus_laser", 256.0);
		}
	}

	private static void executeSentinelSonicScream(LevelAccessor world, Entity entity, Entity target, int ticksLeft) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
		}
		if (entity.getDeltaMovement().y() > 0) {
			entity.setDeltaMovement(entity.getDeltaMovement().x(), 0.0, entity.getDeltaMovement().z());
		}

		if (target != null && target.isAlive()) {
			lockLookAtTarget(entity, target);
		}

		if (ticksLeft == 200) {
			playHostileSound(level, entity, "the_backwoods:sonic_scream", 4.0F, 1.0F);
		}

		if (ticksLeft > 20 && ticksLeft <= 200) {
			List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(24.0), e -> e != entity && !isWoodboundEntity(e, entity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
			for (LivingEntity victim : targets) {
				applyEffectSafe(victim, new MobEffectInstance(MobEffects.BLINDNESS, 140, 0, false, false));
				applyEffectSafe(victim, new MobEffectInstance(MobEffects.CONFUSION, 140, 2, false, false));
				applyEffectSafe(victim, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, false));

				if (victim instanceof Player player) {
					player.setYRot(player.getYRot() + (float)(entity.getRandom().nextDouble() - 0.5) * 20.0F);
					player.setXRot(player.getXRot() + (float)(entity.getRandom().nextDouble() - 0.5) * 15.0F);

					Vec3 motion = player.getDeltaMovement();
					player.setDeltaMovement(motion.x() + (entity.getRandom().nextDouble() - 0.5) * 0.15, motion.y(), motion.z() + (entity.getRandom().nextDouble() - 0.5) * 0.15);
					player.hurtMarked = true;

					if (player.getAbilities().flying || player.isFallFlying()) {
						player.getAbilities().flying = false;
						player.stopFallFlying();
						player.onUpdateAbilities();
						player.setDeltaMovement(player.getDeltaMovement().x(), -0.6, player.getDeltaMovement().z());
						player.hurtMarked = true;
					}
				} else {
					victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.0, 1.0, 0.0).add((entity.getRandom().nextDouble() - 0.5) * 0.05, 0.0, (entity.getRandom().nextDouble() - 0.5) * 0.05));
					if (victim instanceof Mob mob) {
						mob.setTarget(null);
						mob.getNavigation().stop();
						mob.setYRot(mob.getYRot() + (float)(entity.getRandom().nextDouble() - 0.5) * 25.0F);
					}

					boolean targetIsFlying = victim.isFallFlying() || (!victim.onGround() && victim.getY() > entity.getY() + 2.0 && !victim.isInWater() && !victim.isInLava());
					if (targetIsFlying) {
						victim.setDeltaMovement(0.0, -0.6, 0.0);
						victim.hurtMarked = true;
					}
				}
			}
			putD(entity, "sentinel_target_disoriented_ticks", 140.0);
		}

		if (ticksLeft <= 1) {
			putD(entity, "sentinel_sonic_scream_ticks", 0);
			putD(entity, "sentinel_sonic_scream_cooldown", SONIC_SCREAM_COOLDOWN);
			putD(entity, "sentinel_global_ability_cooldown", getDynamicGlobalCooldown(entity));
		}
	}

	private static void checkLearnedMilestone(LivingEntity self, double combatTicks, boolean inCombat) {
		Entity target = (self instanceof Mob mob) ? mob.getTarget() : null;
		if (target != null) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase(java.util.Locale.ROOT);
			boolean isColdEntity = targetId.contains("stray") || targetId.contains("snow") || targetId.contains("ice") || targetId.contains("polar") || targetId.contains("frost") || targetId.contains("freeze");
			if (isColdEntity) {
				putB(self, "fought_cold_entity", true);
			}
			boolean isNetherEntity = targetId.contains("wither") || targetId.contains("ghast") || targetId.contains("piglin") || targetId.contains("blaze") || targetId.contains("magma") || targetId.contains("hoglin") || targetId.contains("strider") || targetId.contains("skeleton");
			if (isNetherEntity || target.level().dimension() == net.minecraft.world.level.Level.NETHER) {
				putB(self, "fought_nether_entity", true);
			}
			boolean isWardenEntity = targetId.contains("warden");
			if (isWardenEntity) {
				double wardenTicks = getD(self, "sentinel_warden_combat_ticks") + 1.0;
				putD(self, "sentinel_warden_combat_ticks", wardenTicks);
				if (wardenTicks >= WARDEN_LEARN_REQUIRED_TICKS) {
					putB(self, "fought_warden_entity", true);
				}
			}
		}
		double totalDamageTaken = getD(self, "sentinel_total_damage_taken");
		tryUnlockAbility(self, "unlocked_regen", totalDamageTaken >= 50.0);
		if (ENABLE_TELEKINESIS) tryUnlockAbility(self, "unlocked_telekinesis", combatTicks >= 400);
		if (ENABLE_EXTRACTION_GRAPPLE) tryUnlockAbility(self, "unlocked_grapple", combatTicks >= getOrInitTicks(self, "sentinel_required_grapple_ticks", 200.0, 300.0));
		tryUnlockAbility(self, "unlocked_sonic_boom", getB(self, "fought_warden_entity"));
		tryUnlockAbility(self, "unlocked_sonic_scream", getD(self, "sentinel_flying_target_ticks") >= 160.0);

		if (!getB(self, "unlocked_solar_beam")) {
			boolean foughtNether = getB(self, "fought_nether_entity");
			boolean inNetherLong = getD(self, "sentinel_time_in_nether") >= 600.0;
			boolean tookFireDmg = getB(self, "taken_fire_damage");
			if (foughtNether || inNetherLong || tookFireDmg) {
				putB(self, "unlocked_solar_beam", true);
				putB(self, "unlocked_water_evaporation", true);
				announceLearnedAbility(self);
			}
		} else if (!getB(self, "unlocked_water_evaporation")) {
			putB(self, "unlocked_water_evaporation", true);
		}

		if (!getB(self, "unlocked_cryo_beam")) {
			boolean foughtCold = getB(self, "fought_cold_entity");
			boolean inColdLong = getD(self, "sentinel_time_in_cold_biome") >= 600.0;
			boolean tookFreezeDmg = getB(self, "taken_freeze_damage");
			if (foughtCold || inColdLong || tookFreezeDmg) {
				tryUnlockAbility(self, "unlocked_cryo_beam", true);
			}
		}

		tryUnlockAbility(self, "unlocked_teleportation", getD(self, "sentinel_teleport_learning_progress") >= 160.0);
		tryUnlockAbility(self, "unlocked_overhead_combo", combatTicks >= getOrInitTicks(self, "sentinel_required_overhead_ticks", 200.0, 300.0));
		tryUnlockAbility(self, "unlocked_dropkick_combo", combatTicks >= getOrInitTicks(self, "sentinel_required_dropkick_ticks", 200.0, 300.0));
		tryUnlockAbility(self, "unlocked_minos_combo", combatTicks >= getOrInitTicks(self, "sentinel_required_minos_ticks", 400.0, 400.0));
		tryUnlockAbility(self, "unlocked_triple_threat_combo", combatTicks >= getOrInitTicks(self, "sentinel_required_cc1_ticks_learn", 140.0, 100.0));
		tryUnlockAbility(self, "unlocked_high_sky_slam_combo", combatTicks >= getOrInitTicks(self, "sentinel_required_cc2_ticks_learn", 160.0, 100.0));
		tryUnlockAbility(self, "unlocked_knockback_dropkick_combo", combatTicks >= getOrInitTicks(self, "sentinel_required_cc3_ticks_learn", 180.0, 100.0));
		tryUnlockAbility(self, "unlocked_knockback_rider_combo", combatTicks >= getOrInitTicks(self, "sentinel_required_cc4_ticks_learn", 200.0, 100.0));
		tryUnlockAbility(self, "unlocked_die_rider_kick", getB(self, "unlocked_knockback_rider_combo"));
		tryUnlockAbility(self, "unlocked_heavenly_repentance_plus", combatTicks >= getOrInitTicks(self, "sentinel_required_cc5_ticks_learn", 240.0, 120.0));
	}

	private static void announceLearnedAbility(Entity self) {
		if (self == null) return;
		playHostileSound(self.level(), self.getX(), self.getY(), self.getZ(), "entity.warden.heartbeat", 1.2F, 1.4F);
	}

	private static void executeGrappleSiphon(LevelAccessor world, Entity self, Entity target, int ticksLeft) {
		if (!(world instanceof ServerLevel level) || !(self instanceof LivingEntity rawSelf)) return;

		Vec3 vectorToSelf = self.position().add(0, 1.0, 0).subtract(target.position()).normalize();
		double distance = target.position().distanceTo(self.position());

		if (distance > 2.0) {
			target.setDeltaMovement(vectorToSelf.x * 0.82, 0.15, vectorToSelf.z * 0.82);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
		} else {
			target.setDeltaMovement(0, -0.05, 0);
			if (target instanceof LivingEntity liv) {
				applyEffectSafe(liv, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4, false, false));
				applyEffectSafe(liv, new MobEffectInstance(MobEffects.BLINDNESS, 40, 1, false, false));
			}

			if (ticksLeft % 4 == 0) {
				dealTrueDamageToBosses(target, getBackwoodsDamage(level, "rot_extraction_grapple", self), 4.0F * (float) getAdaptationMultiplier(self));
				rawSelf.setHealth(Math.min(rawSelf.getMaxHealth(), rawSelf.getHealth() + 4.5F));

				level.sendParticles(ParticleTypes.SWEEP_ATTACK, self.getX(), self.getY() + 1.2, self.getZ(), 1, 0.3, 0.3, 0.3, 0.0);
				level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.1, 0.1, 0.1, 0.1);

				playHostileSound(level, self, "entity.warden.attack_impact", 0.7F, 0.85F);
				if (self instanceof LivingEntity ls) ls.swing(InteractionHand.MAIN_HAND, true);
			}
		}
	}

	private static void executeTelekinesis(LevelAccessor world, Entity self, Entity target, int ticksLeft) {
		if (!(world instanceof ServerLevel level) || target == null) return;

		Vec3 hoverPos = self.position().add(0, 3.5, 0);
		Vec3 diff = hoverPos.subtract(target.position());
		double liftStrength = 0.22;
		setMotion(target, diff.x * liftStrength, 0.15, diff.z * liftStrength);

		if (target instanceof LivingEntity liv) {
			applyEffectSafe(liv, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 3, false, false));
		}

		if (ticksLeft <= 0) {
			if (Math.random() < 0.5) {
				setMotion(target, Vec3.ZERO);
				setMotion(target, 0, -2.4, 0);
				dealTrueDamageToBosses(target, getBackwoodsDamage(level, "rot_telekinesis", self), 12.0F * (float) getAdaptationMultiplier(self));
				playHostileSound(level, target, "entity.iron_golem.damage", 1.0F, 0.65F);
			} else {
				Vec3 throwDir = target.position().subtract(self.position()).normalize();
				double ty = Math.max(throwDir.y, 0.2);
				target.setDeltaMovement(throwDir.x * 2.0, ty * 1.5 + 0.3, throwDir.z * 2.0);
				dealTrueDamageToBosses(target, getBackwoodsDamage(level, "rot_telekinesis", self), 8.0F * (float) getAdaptationMultiplier(self));
				playHostileSound(level, self, "entity.player.attack.sweep", 1.0F, 0.6F);
			}
		}
	}

	private static void executeSentinelPunch(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level)) return;
		LivingEntity ls = (self instanceof LivingEntity) ? (LivingEntity) self : null;

		if (target instanceof LivingEntity targetLiving) {
			String targetTypeName = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase();
			if (targetTypeName.contains("wroughtnaut") || targetTypeName.contains("ferrous_wroughtnaut")) {
				if (!isWroughtnautStuck(targetLiving)) {
					return;
				}
			}
		}

		if (ls != null) {
			boolean lastHandLeft = getB(self, "sentinel_punch_hand_toggle");
			ls.swing(InteractionHand.MAIN_HAND, true);
			putB(self, "sentinel_punch_hand_toggle", !lastHandLeft);

			if (lastHandLeft) {
				putD(self, "sentinel_left_punch_ticks", 18);
				putD(self, "sentinel_right_punch_ticks", 0);
			} else {
				putD(self, "sentinel_right_punch_ticks", 18);
				putD(self, "sentinel_left_punch_ticks", 0);
			}
		}

		double PUNCH_LEAD_TICKS = 8.0;
		Vec3 targetVel = target.getDeltaMovement();
		double predictedX = target.getX() + targetVel.x * PUNCH_LEAD_TICKS;
		double predictedZ = target.getZ() + targetVel.z * PUNCH_LEAD_TICKS;
		double pDx = self.getX() - predictedX;
		double pDy = self.getY() - target.getY();
		double pDz = self.getZ() - predictedZ;
		double predictedDist = Math.sqrt(pDx * pDx + pDy * pDy + pDz * pDz);
		double liveDist = self.distanceTo(target);

		double finalDist = Math.min(liveDist, predictedDist);
		double maxReach = (target instanceof Player) ? 2.5 : 3.2;
		if (finalDist > maxReach) {
			return;
		}

		double combatTicks = getD(self, "sentinel_combat_ticks");

		double targetCurrentHp = (target instanceof LivingEntity tLiv) ? tLiv.getHealth() : 999.0;
		double rotPunchDmg = MELEE_PUNCH_DAMAGE * getAdaptationMultiplier(self);
		boolean isLowHpTarget = !(target instanceof Player) && (targetCurrentHp <= rotPunchDmg || targetCurrentHp <= 20.0);

		boolean totemActive = getB(self, "sentinel_totem_active");

		double basePunchDmg = MELEE_PUNCH_DAMAGE;

		boolean unlockedSonicBoom = getB(self, "unlocked_sonic_boom");
		boolean unlockedSolar = getB(self, "unlocked_solar_beam");
		boolean unlockedCryo = getB(self, "unlocked_cryo_beam");
		boolean unlockedGrapple = ENABLE_EXTRACTION_GRAPPLE && getB(self, "unlocked_grapple");
		boolean unlockedTK = ENABLE_TELEKINESIS && getB(self, "unlocked_telekinesis");
		boolean unlockedTP = getB(self, "unlocked_teleportation");
		boolean unlockedOverhead = getB(self, "unlocked_overhead_combo");
		boolean unlockedDropkick = getB(self, "unlocked_dropkick_combo");
		boolean unlockedMinos = getB(self, "unlocked_minos_combo");

		int selectCombo = 0;
		double globalCd = getD(self, "sentinel_global_ability_cooldown");
		double fakePressureTicks = getD(self, "ai_fake_pressure_ticks");
		if (fakePressureTicks <= 0 && globalCd <= 0 && target instanceof LivingEntity && Math.random() < (getB(self, "sentinel_totem_active") ? 0.85 : 0.28)) {
			java.util.List<Integer> availableCombos = new java.util.ArrayList<>();
			if (unlockedGrapple) availableCombos.add(2);
			if (unlockedTK) availableCombos.add(3);
			if (unlockedTP) availableCombos.add(4);
			if (!isLowHpTarget && unlockedTK) availableCombos.add(5);
			if (unlockedSolar || unlockedCryo) availableCombos.add(6);
			if (unlockedTP) availableCombos.add(7);
			if (!isLowHpTarget && unlockedTP && unlockedOverhead) availableCombos.add(8);
			if (unlockedTP) availableCombos.add(9);
			boolean isSelfOnGroundOrFluid = self.onGround() || self.isInWater() || self.isInLava();
			if (unlockedTP && unlockedDropkick && isSelfOnGroundOrFluid && (self.distanceTo(target) >= 5.5 || (self.distanceTo(target) >= 4.0 && getD(target, "bw_recent_kb_ticks") > 0))) availableCombos.add(10);
			if (unlockedTK) availableCombos.add(12);
			double dropkickThreshold = Math.max(5.5, getDynamicRangeThreshold(self, "dropkick", 5.5, DYNAMIC_RANGE_OFFSET_VARIANCE));
			if (unlockedDropkick && isSelfOnGroundOrFluid && (self.distanceTo(target) >= dropkickThreshold || (self.distanceTo(target) >= 4.0 && getD(target, "bw_recent_kb_ticks") > 0))) availableCombos.add(13);
			if (!isLowHpTarget && unlockedMinos) availableCombos.add(14);

			if (!isLowHpTarget && getB(self, "unlocked_triple_threat_combo") && getD(self, "sentinel_cc1_cd") <= 0) availableCombos.add(101);
			if (!isLowHpTarget && getB(self, "unlocked_high_sky_slam_combo") && getD(self, "sentinel_cc2_cd") <= 0) availableCombos.add(102);
			if (!isLowHpTarget && getB(self, "unlocked_knockback_dropkick_combo") && getD(self, "sentinel_cc3_cd") <= 0 && isSelfOnGroundOrFluid && (self.distanceTo(target) >= 5.5 || (self.distanceTo(target) >= 4.0 && getD(target, "bw_recent_kb_ticks") > 0))) availableCombos.add(103);
			if (!isLowHpTarget && (getB(self, "unlocked_knockback_rider_combo") || getB(self, "unlocked_die_rider_kick")) && getD(self, "sentinel_cc4_cd") <= 0 && target.onGround()) availableCombos.add(104);
			if (!isLowHpTarget && getB(self, "unlocked_heavenly_repentance_plus") && getD(self, "sentinel_cc5_cd") <= 0 && isSelfOnGroundOrFluid) availableCombos.add(105);

			if (!availableCombos.isEmpty()) {
				boolean hasCombos = false;
				boolean hasStandalone = false;
				for (int c : availableCombos) {
					if (c >= 100) {
						hasCombos = true;
					} else {
						hasStandalone = true;
					}
				}
				if (hasCombos && hasStandalone) {
					// Flip a coin to strictly balance standalone moves vs complex Minos combos
					boolean filterToStandalone = Math.random() < 0.50;
					java.util.List<Integer> filtered = new java.util.ArrayList<>();
					for (int c : availableCombos) {
						if (filterToStandalone && c < 100) {
							filtered.add(c);
						} else if (!filterToStandalone && c >= 100) {
							filtered.add(c);
						}
					}
					if (!filtered.isEmpty()) {
						availableCombos = filtered;
					}
				}

				CombatContext ctx = getCombatContext(self, target);
				boolean favorHeavy = getB(target, "bw_threat_high_armor");
				if (favorHeavy && Math.random() < 0.6) {
					java.util.List<Integer> heavyCombos = new java.util.ArrayList<>();
					for (int c : availableCombos) {
						if (c == 5 || c == 8 || c == 12 || c == 13 || c == 14 || c == 101 || c == 102 || c == 103 || c == 104 || c == 105) heavyCombos.add(c);
					}
					if (!heavyCombos.isEmpty()) {
						selectCombo = evaluateComboUtility(self, target, ctx, heavyCombos);
					} else {
						selectCombo = evaluateComboUtility(self, target, ctx, availableCombos);
					}
				} else {
					selectCombo = evaluateComboUtility(self, target, ctx, availableCombos);
				}
				if (selectCombo > 0) {
					String comboName = "Unknown";
					if (selectCombo == 2) comboName = "GRAPPLE SIPHON (Combo 2)";
					else if (selectCombo == 3) comboName = "TELEKINESIS (Combo 3)";
					else if (selectCombo == 4) comboName = "TELEPORT BEHIND (Combo 4)";
					else if (selectCombo == 5) comboName = "TELEKINESIS SLAM (Combo 5)";
					else if (selectCombo == 6) comboName = "BEAM ATTACK (Combo 6)";
					else if (selectCombo == 7) comboName = "TELEPORT AND FLANK (Combo 7)";
					else if (selectCombo == 8) comboName = "TELEPORT + OVERHEAD PUNCH (Combo 8)";
					else if (selectCombo == 9) comboName = "TELEPORT CHASE (Combo 9)";
					else if (selectCombo == 10) comboName = "JUDGMENT CHARGE (Combo 10)";
					else if (selectCombo == 11) comboName = "TERRAIN-DESTROYING LEAP (Combo 11)";
					else if (selectCombo == 12) comboName = "HIGH SKY UPPERCUT (Combo 12)";
					else if (selectCombo == 13) comboName = "DROPKICK (Combo 13)";
					else if (selectCombo == 14) comboName = "MINOS BURST (Combo 14)";
					else if (selectCombo == 101) comboName = "TRIPLE THREAT COMBO (CC1)";
					else if (selectCombo == 102) comboName = "HIGH SKY SLAM COMBO (CC2)";
					else if (selectCombo == 103) comboName = "KNOCKBACK DROPKICK COMBO (CC3)";
					else if (selectCombo == 104) comboName = "KNOCKBACK RIDER COMBO (CC4)";
					else if (selectCombo == 105) comboName = "HEAVENLY REPENTANCE PLUS (CC5)";
					logActionToChat(level, self, "Selected combo/ability: §a" + comboName + " (ID: " + selectCombo + ") §ftargeting " + target.getDisplayName().getString());
				}
				double coreCd = getDynamicGlobalCooldown(self);
				putD(self, "sentinel_global_ability_cooldown", coreCd);

				if (selectCombo == 101) {
					putD(self, "sentinel_cc1_stage", 1);
					putD(self, "sentinel_cc1_ticks", 20);
					putD(self, "sentinel_combo_active_ticks", 120);
				} else if (selectCombo == 102) {
					putD(self, "sentinel_cc2_stage", 1);
					putD(self, "sentinel_combo_active_ticks", 180);
				} else if (selectCombo == 103) {
					putD(self, "sentinel_cc3_stage", 1);
					putD(self, "sentinel_combo_active_ticks", 120);
				} else if (selectCombo == 104) {
					putD(self, "sentinel_cc4_stage", 1);
					putD(self, "sentinel_combo_active_ticks", 120);
				} else if (selectCombo == 105) {
					putD(self, "sentinel_cc5_stage", 1);
					putD(self, "sentinel_combo_active_ticks", 180);
				} else {
					putD(self, "sentinel_combo_active_ticks", 40);
				}

				if (selectCombo >= 101) {
					return;
				}
			}
		}

		boolean targetHasArmor = false;
		boolean targetHasMace = false;
		double targetDmg = 2.0;

		if (target instanceof LivingEntity targetLiving) {
			if (targetLiving.getArmorValue() > 0) {
				targetHasArmor = true;
			}
			var mainHand = targetLiving.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
			var offHand = targetLiving.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND);
			String mainHandName = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();
			String offHandName = BuiltInRegistries.ITEM.getKey(offHand.getItem()).toString();
			if (mainHandName.contains("mace") || offHandName.contains("mace")) {
				targetHasMace = true;
			}

			var attr = getSafeAttribute(targetLiving, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
			if (attr != null) {
				targetDmg = attr.getValue();
			}
		}

		double adaptedPunchDmg = getD(self, "adapted_punch_damage");
		if (adaptedPunchDmg < basePunchDmg) {
			adaptedPunchDmg = basePunchDmg;
		}

		if (targetDmg > adaptedPunchDmg) {
			adaptedPunchDmg += (targetDmg - adaptedPunchDmg) * 0.45;
			putD(self, "adapted_punch_damage", adaptedPunchDmg);
		}

		double finalDamage = adaptedPunchDmg;

		if (targetHasMace && target instanceof LivingEntity targetLiving) {
			for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
				if (slot.isArmor()) {
					var stack = targetLiving.getItemBySlot(slot);
					if (!stack.isEmpty()) {
						stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 25));
					}
				}
			}
			finalDamage += targetLiving.getArmorValue() * 0.65;
		}

		if (selectCombo != 4 && selectCombo != 5 && selectCombo != 6 && selectCombo != 7 && selectCombo != 9) {
			float punchDmgToDeal = (selectCombo == 0) ? (float) MELEE_PUNCH_DAMAGE : (float) (finalDamage * getAdaptationMultiplier(ls != null ? ls : self));
			if (ls != null) {
				dealTrueDamageToBosses(target, ls.damageSources().mobAttack(ls), punchDmgToDeal);
			} else {
				dealTrueDamageToBosses(target, getMobAttackDamage(level), punchDmgToDeal);
			}
		}

		Vec3 sweepPush = target.position().subtract(self.position()).normalize();
		double knockbackStrength = 1.05;

		if (adaptedPunchDmg > basePunchDmg) {
			knockbackStrength += (adaptedPunchDmg - basePunchDmg) * 0.045;
		}

		if (selectCombo == 2 && target instanceof LivingEntity liv) {
			putD(self, K_GRAPPLE_CD, 20);
			putD(self, K_GRAPPLE_TICKS, 32);
			Vec3 vectorToSelf = self.position().subtract(target.position()).normalize();
			setMotion(liv, vectorToSelf.x * 1.25, 0.22, vectorToSelf.z * 1.25);
			playHostileSound(level, self, "entity.warden.attack_impact", 1.2F, 0.85F);
			level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
		} else if (selectCombo == 3 && target instanceof LivingEntity liv) {
			putD(self, K_TK_CD, 20);
			putD(self, K_TK_TICKS, 25);
			setMotion(liv, 0.0, 0.85, 0.0);
			playHostileSound(level, self, "entity.warden.sonic_boom", 0.9F, 1.4F);
			level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1.2, target.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
		} else if (selectCombo == 4 && target instanceof LivingEntity liv) {
			double angle = target.getYRot() * (Math.PI / 180.0);
			double spawnX = target.getX() - Math.sin(angle) * 1.5;
			double spawnZ = target.getZ() + Math.cos(angle) * 1.5;
			double spawnY = target.getY();

			level.sendParticles(ParticleTypes.SMOKE, self.getX(), self.getY() + 1.1, self.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
			teleportEntity(self, spawnX, spawnY, spawnZ);
			level.sendParticles(ParticleTypes.SMOKE, spawnX, spawnY + 1.1, spawnZ, 8, 0.2, 0.2, 0.2, 0.05);
			playHostileSound(level, spawnX, spawnY, spawnZ, "item.chorus_fruit.teleport", 1.1F, 0.95F);

			dealTrueDamageToBosses(liv, ls != null ? ls.damageSources().mobAttack(ls) : getMobAttackDamage(level), (float) (finalDamage * 1.4) * (float) getAdaptationMultiplier(ls));
			setMotion(liv, liv.getDeltaMovement().add(self.getViewVector(1.0F).scale(1.8)));
			playHostileSound(level, target, "entity.player.attack.sweep", 1.2F, 0.65F);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, self.getX(), self.getY() + 1.2, self.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
		} else if (selectCombo == 5 && target instanceof LivingEntity liv) {
			setMotion(liv, 0.0, 0.45, 0.0);
			applyEffectSafe(liv, new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, false));
			dealTrueDamageToBosses(liv, ls != null ? ls.damageSources().mobAttack(ls) : getMobAttackDamage(level), (float) (finalDamage * 1.5) * (float) getAdaptationMultiplier(ls));
			setMotion(liv, sweepPush.x * 0.2, -1.8, sweepPush.z * 0.2);

			playHostileSound(level, target, "entity.iron_golem.damage", 1.4F, 0.5F);
			playHostileSound(level, target, "entity.iron_golem.death", 0.9F, 0.6F);
			level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(), 5, 0.5, 0.1, 0.5, 0.1);
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(), target.getZ(), 10, 0.4, 0.4, 0.4, 0.2);
		} else if (selectCombo == 6 && target instanceof LivingEntity liv) {
			applyEffectSafe(liv, new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));
			dealTrueDamageToBosses(liv, ls != null ? ls.damageSources().mobAttack(ls) : getMobAttackDamage(level), (float) (finalDamage * 1.35) * (float) getAdaptationMultiplier(ls));

			level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
			playHostileSound(level, target, "block.powder_snow.break", 1.2F, 1.1F);
			playHostileSound(level, target, "entity.generic.explode", 0.8F, 1.4F);
		} else if (selectCombo == 7 && target instanceof LivingEntity liv) {
			for (int i = 0; i < 3; i++) {
				double oX = (Math.random() - 0.5) * 3.0;
				double oZ = (Math.random() - 0.5) * 3.0;
				level.sendParticles(ParticleTypes.SMOKE, target.getX() + oX, target.getY() + 1.0, target.getZ() + oZ, 4, 0.1, 0.1, 0.1, 0.05);
			}
			playHostileSound(level, target, "item.chorus_fruit.teleport", 1.3F, 1.2F);
			playHostileSound(level, target, "entity.player.attack.sweep", 1.4F, 1.5F);

			dealTrueDamageToBosses(liv, ls != null ? ls.damageSources().mobAttack(ls) : getMobAttackDamage(level), (float) (finalDamage * 1.6) * (float) getAdaptationMultiplier(ls));
			setMotion(liv, sweepPush.x * 1.6, 0.45, sweepPush.z * 1.6);

			level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.08);
		} else if (selectCombo == 8 && target instanceof LivingEntity liv) {
			putD(self, "rot_overhead_ticks", OVERHEAD_TOTAL_TICKS);
			putS(self, "overhead_target_uuid", target.getUUID().toString());
		} else if (selectCombo == 9 && target instanceof LivingEntity liv) {
			for (int hit = 1; hit <= 3; hit++) {
				double targetYRot = target.getYRot() * (Math.PI / 180.0);
				double tx = target.getX() - Math.sin(targetYRot) * 1.6;
				double tz = target.getZ() + Math.cos(targetYRot) * 1.6;
				double ty = target.getY();
				level.sendParticles(ParticleTypes.SMOKE, self.getX(), self.getY() + 0.5, self.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
				teleportEntity(self, tx, ty, tz);
				level.sendParticles(ParticleTypes.SMOKE, tx, ty + 0.5, tz, 6, 0.2, 0.2, 0.2, 0.05);
				playHostileSound(level, tx, ty, tz, "item.chorus_fruit.teleport", 1.1F, 1.0F + hit * 0.1F);
				playHostileSound(level, tx, ty, tz, "entity.player.attack.knockback", 1.3F, 0.7F + hit * 0.1F);
				dealTrueDamageToBosses(target, ls != null ? ls.damageSources().mobAttack(ls) : getMobAttackDamage(level), (float) (finalDamage * 1.2) * (float) getAdaptationMultiplier(ls));
				Vec3 pushVec = target.position().subtract(self.position()).normalize();
				setMotion(target, pushVec.x * 1.9, 0.35, pushVec.z * 1.9);
			}
		} else if (selectCombo == 10 && target instanceof LivingEntity liv) {
			setMotion(self, 0.0, 0.0, 0.0);
			playHostileSound(level, self, "entity.warden.sonic_charge", 1.5F, 0.5F);
			putD(self, "sentinel_judgment_ticks", 60);
		} else if (selectCombo == 12 && target instanceof LivingEntity liv) {
			setMotion(liv, 0.0, 1.45, 0.0);
			playHostileSound(level, self, "entity.iron_golem.attack", 1.5F, 0.8F);
			level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.5, target.getZ(), 10, 0.2, 0.2, 0.2, 0.05);

			setMotion(self, 0.0, 1.9, 0.0);

			putD(self, "sentinel_slam_phase", 1);
			putD(self, "sentinel_slam_ticks", 22);
		} else if (selectCombo == 13 && target instanceof LivingEntity liv) {
			if (self.distanceTo(liv) < 4.5) {
				// Avoid awkward point-blank dropkick; execute heavy punch instead
				executeSentinelPunch(world, self, liv);
			} else {
				setMotion(self, 0.0, 1.95, 0.0);

				putD(self, "sentinel_die_kick_phase", 1);
				putD(self, "sentinel_die_kick_ticks", 22);
				playHostileSound(level, self, "entity.iron_golem.attack", 1.5F, 0.8F);
			}
		} else if (selectCombo == 14 && target instanceof LivingEntity liv) {
			cancelActiveBeams(level, self);
			putD(self, "sentinel_minos_ticks", 50);
			putD(self, "sentinel_minos_stage", 1);
		} else {
		}

		if (targetHasArmor) {
			playHostileSound(level, target, "item.mace.heavy_smash", 1.2F, 0.65F);
		} else if (adaptedPunchDmg > basePunchDmg) {
			playHostileSound(level, target, "item.mace.knockback", 1.1F, 0.95F);
		} else {
			playHostileSound(level, target, "entity.iron_golem.damage", 1.0F, 0.85F);
		}

		level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.2, target.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
	}

	private static boolean isWither(Entity entity) {
		if (entity == null) return false;
		String key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
		return "minecraft:wither".equals(key);
	}

	private static void sendActionBarToNearbyPlayers(LevelAccessor world, Vec3 pos, double range, String text) {
		if (world instanceof ServerLevel s) {
			s.players().stream()
				.filter(p -> p.position().distanceToSqr(pos) <= range * range)
				.forEach(p -> p.displayClientMessage(Component.literal(text), true));
		}
	}

	private static void logActionToChat(LevelAccessor world, Entity self, String message) {
		if (!ENABLE_CHAT_LOGGING) return;
		if (world instanceof ServerLevel s) {
			String name = self.getDisplayName().getString();
			String formatted = "§6[Rot AI Log] §e" + name + " §f" + message;
			s.players().forEach(p -> p.displayClientMessage(Component.literal(formatted), false));
		}
	}

	private static void monitorAndLogAllActions(LevelAccessor world, Entity self) {
		if (!(world instanceof ServerLevel level)) return;

		if (self instanceof Mob mob) {
			LivingEntity currentTarget = mob.getTarget();
			int currentTargetId = currentTarget != null ? currentTarget.getId() : 0;
			int lastTargetId = getI(self, "log_last_target_id");
			if (currentTargetId != lastTargetId) {
				putI(self, "log_last_target_id", currentTargetId);
				if (currentTarget != null) {
					logActionToChat(world, self, "Target acquired: §d" + currentTarget.getDisplayName().getString() + " §7(HP: " + (int)currentTarget.getHealth() + "/" + (int)currentTarget.getMaxHealth() + ")");
				} else {
					logActionToChat(world, self, "Lost combat target / entered passive state");
				}
			}
		}

		String[] doubleKeys = {
			"sentinel_solar_charge_ticks", "sentinel_solar_fire_ticks",
			"sentinel_cryo_charge_ticks", "sentinel_cryo_fire_ticks",
			"sentinel_sonic_ticks", "sentinel_omni_sonic_charge_ticks", "sentinel_sonic_scream_ticks",
			"sentinel_slam_phase", "sentinel_sky_warp_slam_ticks",
			"rot_overhead_ticks",
			"sentinel_judgment_ticks",
			"sentinel_die_kick_phase", "sentinel_die_kick_ticks",
			"sentinel_armor_rip_ticks",
			"sentinel_grapple_ticks", "sentinel_tk_ticks",
			"rot_superheat_charging", "rot_superheat_active"
		};

		for (String key : doubleKeys) {
			double currentVal = getD(self, key);
			double lastVal = getD(self, "log_last_" + key);
			if (currentVal != lastVal) {
				putD(self, "log_last_" + key, currentVal);
				if (currentVal > 0 && lastVal <= 0) {
					String msg = getActionStartMessage(key, currentVal);
					if (msg != null) logActionToChat(world, self, msg);
				} else if (currentVal <= 0 && lastVal > 0) {
					String msg = getActionStopMessage(key);
					if (msg != null) logActionToChat(world, self, msg);
				}
			}
		}

		String[] booleanKeys = {
			"is_blocking",
			"is_uppercutting"
		};

		for (String key : booleanKeys) {
			boolean currentVal = getB(self, key);
			boolean lastVal = getB(self, "log_last_" + key);
			if (currentVal != lastVal) {
				putB(self, "log_last_" + key, currentVal);
				if (currentVal) {
					String msg = getActionStartMessage(key, 1.0);
					if (msg != null) logActionToChat(world, self, msg);
				} else {
					String msg = getActionStopMessage(key);
					if (msg != null) logActionToChat(world, self, msg);
				}
			}
		}
	}

	private static String getActionStartMessage(String key, double val) {
		switch (key) {
			case "sentinel_melee_windup": return "Winding up a melee attack... §7(Ticks: " + (int)val + ")";
			case "sentinel_solar_charge_ticks": return "Charging §cSolar Laser Beam§f... §7(Powering up)";
			case "sentinel_solar_fire_ticks": return "§cFIRES SOLAR LASER BEAM! §e(High Thermal Damage)§f";
			case "sentinel_cryo_charge_ticks": return "Charging §bCryo Laser Beam§f... §7(Freezing up)";
			case "sentinel_cryo_fire_ticks": return "§bFIRES CRYO LASER BEAM! §d(Freezing Target)§f";
			case "sentinel_sonic_ticks": return "Charging §aSonic Blast§f...";
			case "sentinel_omni_sonic_charge_ticks": return "Preparing §aOmnidirectional Sonic Shockwave§f...";
			case "sentinel_sonic_scream_ticks": return "§aSCREAMING SONIC BLAST! §e(Huge Knockback & Damage)";
			case "sentinel_slam_phase": return "Preparing §eGravity Seismic Slam §7(Phase: " + (int)val + ")";
			case "sentinel_sky_warp_slam_ticks": return "Executing §eSky Warp Teleport Slam§f!";
			case "rot_overhead_ticks": return "Executing §eOverhead Strike§f!";
			case "sentinel_judgment_ticks": return "Charging §4Judgment Kick/Charge§f... §7(Ticks: " + (int)val + ")";
			case "sentinel_die_kick_phase": return "Executing §dDie Rider Kick §7(Phase: " + (int)val + ")";
			case "sentinel_armor_rip_ticks": return "Executing §cArmor Rip Choke move§f! §7(Ticks: " + (int)val + ")";
			case "sentinel_grapple_ticks": return "Executing §dGrapple Siphon§f! §7(Sucking HP)";
			case "sentinel_tk_ticks": return "Lifting target with §dTelekinesis§f!";
			case "rot_superheat_charging": return "§4ALERT: Charging Superheat Catalyst Ultimate State!§f";
			case "rot_superheat_active": return "§4ALERT: SUPERHEAT CATALYST ENRAGED MODE IS NOW ACTIVE!§f";
			case "is_blocking": return "Raised guard: §aNow BLOCKING/SHIELDING incoming attacks§f";
			case "is_uppercutting": return "Executing §eUppercut Strike§f!";
			default: return null;
		}
	}

	private static String getActionStopMessage(String key) {
		switch (key) {
			case "sentinel_melee_windup": return "Melee windup finished";
			case "sentinel_solar_fire_ticks": return "§cSolar Laser Beam finished firing";
			case "sentinel_cryo_fire_ticks": return "§bCryo Laser Beam finished firing";
			case "sentinel_sonic_scream_ticks": return "Sonic scream completed";
			case "sentinel_slam_phase": return "Gravity Seismic Slam completed";
			case "sentinel_sky_warp_slam_ticks": return "Sky Warp Slam completed";
			case "rot_overhead_ticks": return "Overhead Strike completed";
			case "sentinel_judgment_ticks": return "Judgment Kick completed";
			case "sentinel_die_kick_phase": return "Die Rider Kick completed";
			case "sentinel_armor_rip_ticks": return "Armor Rip Choke completed";
			case "sentinel_grapple_ticks": return "Grapple Siphon finished";
			case "sentinel_tk_ticks": return "Telekinesis dropped";
			case "rot_superheat_active": return "§7Superheat Catalyst enraged mode expired";
			case "is_blocking": return "Lowered guard: Stopped blocking";
			case "is_uppercutting": return "Uppercut completed";
			default: return null;
		}
	}

	private static boolean isTargetPillaring(LevelAccessor level, Entity target, Entity self) {
		if (target == null || self == null) return false;
		if (target.getY() <= self.getY() + 2.2) return false;
		net.minecraft.core.BlockPos targetPos = target.blockPosition();
		int targetY = targetPos.getY() - 1;
		int selfY = self.blockPosition().getY();
		int startY = targetY;
		int endY = Math.max(selfY + 1, targetY - 5);
		if (startY < endY) return false;
		int totalSolidCount = 0;
		int layersChecked = 0;
		for (int y = startY; y >= endY; y--) {
			int layerSolidCount = 0;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					net.minecraft.core.BlockPos p = targetPos.offset(dx, y - targetPos.getY(), dz);
					if (level.getBlockState(p).isCollisionShapeFullBlock(level, p)) {
						layerSolidCount++;
					}
				}
			}
			totalSolidCount += layerSolidCount;
			layersChecked++;
		}
		if (layersChecked == 0) return false;
		double avgSolid = (double) totalSolidCount / layersChecked;
		return avgSolid < 4.5;
	}

	private static void snapLookAtTarget(Entity entity, Entity target) {
		if (entity == null || target == null) return;
		double dx = target.getX() - entity.getX();
		double dy = (target.getY() + target.getBbHeight() * 0.5) - (entity.getY() + entity.getEyeHeight());
		double dz = target.getZ() - entity.getZ();
		double dh = Math.sqrt(dx * dx + dz * dz);
		if (dh > 0.001) {
			float targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
			float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
			float currentY = entity.getYRot();
			float currentX = entity.getXRot();
			float maxTurn = 35.0F;
			float yawDelta = Mth.wrapDegrees(targetYRot - currentY);
			float pitchDelta = Mth.wrapDegrees(targetXRot - currentX);
			float newYRot = currentY + Mth.clamp(yawDelta, -maxTurn, maxTurn);
			float newXRot = currentX + Mth.clamp(pitchDelta, -maxTurn, maxTurn);
			if (entity instanceof Mob mob) {
				mob.yHeadRot = newYRot;
				boolean isMoving = mob.getNavigation().isInProgress() && mob.getDeltaMovement().horizontalDistanceSqr() > 0.005;
				if (!isMoving || dh <= 3.5) {
					mob.setYRot(newYRot);
					mob.yBodyRot = newYRot;
				}
				double dieKickPhase = getD(entity, "sentinel_die_kick_phase");
				if (dieKickPhase > 0) {
					mob.setXRot(0.0F);
					entity.setXRot(0.0F);
				} else {
					mob.setXRot(newXRot);
				}
			} else {
				entity.setYRot(newYRot);
				entity.setXRot(newXRot);
			}
		}
	}

	private static void lockLookAtTarget(Entity entity, Vec3 targetPos) {
		if (entity == null || targetPos == null) return;
		double dieKickPhase = getD(entity, "sentinel_die_kick_phase");
		double judgmentTicks = getD(entity, "sentinel_judgment_ticks");
		double landingTicks = getD(entity, "sentinel_landing_ticks");
		if (dieKickPhase > 0 || (judgmentTicks > 0 && judgmentTicks <= 20) || landingTicks > 0) {
			return;
		}
		if (entity instanceof Mob mob) {
			double dx = targetPos.x() - mob.getX();
			double dy = targetPos.y() - mob.getEyeY();
			double dz = targetPos.z() - mob.getZ();
			double dh = Math.sqrt(dx * dx + dz * dz);
			if (dh > 0.001 || Math.abs(dy) > 0.001) {
				float targetYRot = mob.getYRot();
				if (dh > 0.25) {
					targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
				}
				float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
				float currentYRot = mob.getYRot();
				float currentXRot = mob.getXRot();
				float yawDelta = Mth.wrapDegrees(targetYRot - currentYRot);
				float pitchDelta = Mth.wrapDegrees(targetXRot - currentXRot);
				float newYRot = currentYRot + Mth.clamp(yawDelta, -24.0F, 24.0F);
				float newXRot = currentXRot + Mth.clamp(pitchDelta, -24.0F, 24.0F);
				boolean isMoving = mob.getNavigation().isInProgress() && mob.getDeltaMovement().horizontalDistanceSqr() > 0.005;
				if (!isMoving) {
					mob.setYRot(newYRot);
					mob.yBodyRot = newYRot;
				}
				mob.setXRot(newXRot);
				mob.setYHeadRot(newYRot);
				mob.getLookControl().setLookAt(targetPos.x(), targetPos.y(), targetPos.z(), 24.0F, 24.0F);
			}
		} else {
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos);
		}
	}

	private static void lockLookAtTarget(Entity entity, Entity target) {
		if (entity == null || target == null) return;
		double dieKickPhase = getD(entity, "sentinel_die_kick_phase");
		double judgmentTicks = getD(entity, "sentinel_judgment_ticks");
		double landingTicks = getD(entity, "sentinel_landing_ticks");
		if (dieKickPhase > 0 || (judgmentTicks > 0 && judgmentTicks <= 20) || landingTicks > 0) {
			if (entity instanceof Mob mob) {
				double dx = target.getX() - mob.getX();
				double dy = (target.getY() + target.getBbHeight() * 0.5) - mob.getEyeY();
				double dz = target.getZ() - mob.getZ();
				double dh = Math.sqrt(dx * dx + dz * dz);
				float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
				float currentXRot = mob.getXRot();
				float pitchDelta = Mth.wrapDegrees(targetXRot - currentXRot);
				float newXRot = currentXRot + Mth.clamp(pitchDelta, -24.0F, 24.0F);
				mob.setXRot(newXRot);
				mob.getLookControl().setLookAt(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 30.0F, 30.0F);
			}
			return;
		}
		if (entity instanceof Mob mob) {
			boolean isChanneling = isChannelingAbility(entity);
			boolean isFiringLaser = getD(entity, "sentinel_solar_fire_ticks") > 0
					|| getD(entity, "sentinel_cryo_fire_ticks") > 0;

			double dx = target.getX() - mob.getX();
			double dy = (target.getY() + target.getBbHeight() * 0.5) - mob.getEyeY();
			double dz = target.getZ() - mob.getZ();
			double dh = Math.sqrt(dx * dx + dz * dz);
			boolean targetInWater = target.isInWater() || target.isUnderWater();
			boolean mobInWater = mob.isInWater() || mob.isUnderWater();

			if (isChanneling) {
				float targetYRot = mob.getYRot();
				if (dh > 0.25) {
					targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
				}
				float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
				if (targetInWater && !mobInWater) targetXRot = Mth.clamp(targetXRot, -35.0F, 35.0F);

				float curY = mob.getYRot();
				float curX = mob.getXRot();

				float maxTurnRate = isFiringLaser ? 22.0F : 28.0F;

				float yawDelta = Mth.wrapDegrees(targetYRot - curY);
				float pitchDelta = Mth.wrapDegrees(targetXRot - curX);

				float newYRot = curY + Mth.clamp(yawDelta, -maxTurnRate, maxTurnRate);
				float newXRot = curX + Mth.clamp(pitchDelta, -maxTurnRate, maxTurnRate);

				mob.setYRot(newYRot);
				mob.setXRot(newXRot);
				mob.setYHeadRot(newYRot);
				mob.yBodyRot = newYRot;
				double lookY = target.getY() + target.getBbHeight() * 0.5;
				if (targetInWater && !mobInWater) lookY = mob.getEyeY() - Math.tan(Math.toRadians(35.0)) * dh;
				mob.getLookControl().setLookAt(target.getX(), lookY, target.getZ(), maxTurnRate, maxTurnRate);
			} else {
				float currentYRot = mob.getYRot();
				float maxTurnRate = isFiringLaser ? 18.0F : 24.0F;
				if (dh > 0.35) {
					float targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;

					float yawDelta = Mth.wrapDegrees(targetYRot - currentYRot);
					float newYRot = currentYRot + Mth.clamp(yawDelta, -maxTurnRate, maxTurnRate);

					boolean isMoving = mob.getNavigation().isInProgress() && mob.getDeltaMovement().horizontalDistanceSqr() > 0.005;
					if (!isMoving) {
						mob.setYRot(newYRot);
						mob.yBodyRot = newYRot;
					}
					mob.setYHeadRot(newYRot);
				}

				float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
				if (targetInWater && !mobInWater) targetXRot = Mth.clamp(targetXRot, -35.0F, 35.0F);
				float currentXRot = mob.getXRot();
				float pitchDelta = Mth.wrapDegrees(targetXRot - currentXRot);
				float newXRot = currentXRot + Mth.clamp(pitchDelta, -24.0F, 24.0F);
				mob.setXRot(newXRot);
				double lookY = target.getY() + target.getBbHeight() * 0.5;
				if (targetInWater && !mobInWater) lookY = mob.getEyeY() - Math.tan(Math.toRadians(35.0)) * dh;
				mob.getLookControl().setLookAt(target.getX(), lookY, target.getZ(), maxTurnRate, maxTurnRate);
			}
		} else {
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ()));
		}
	}

	private static List<LivingEntity> getEntitiesInPlayerFOV(Player player, double range) {
		List<LivingEntity> targets = new java.util.ArrayList<>();
		Vec3 eyePosition = player.getEyePosition(1.0F);
		Vec3 lookVec = player.getViewVector(1.0F).normalize();
		AABB searchBox = player.getBoundingBox().inflate(range);
		for (Entity entity : player.level().getEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive())) {
			if (entity instanceof RotEntity) {
				continue;
			}
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("spore:scent")) {
				continue;
			}
			if (entity instanceof Player p) {
				String name = p.getGameProfile().getName();
				if (name.equals("honeypie_3301") || name.equals("Dev")) {
					continue;
				}
			}
			Vec3 toEntity = entity.position().subtract(eyePosition);
			double dist = toEntity.length();
			if (dist > range) continue;
			if (dist < 4.0) {
				targets.add((LivingEntity) entity);
				continue;
			}
			toEntity = toEntity.normalize();
			double dot = lookVec.dot(toEntity);
			if (dot > 0.5) {
				targets.add((LivingEntity) entity);
			}
		}
		return targets;
	}

	private static Entity getPlayerFOVTarget(Player player, double range) {
		Vec3 eyePosition = player.getEyePosition(1.0F);
		Vec3 lookVec = player.getViewVector(1.0F);
		Vec3 reachVec = eyePosition.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
		AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D, 1.0D, 1.0D);

		Entity target = null;
		double closestDist = range;

		for (Entity entity : player.level().getEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive())) {
			if (entity instanceof RotEntity) {
				continue;
			}
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("spore:scent")) {
				continue;
			}
			if (entity instanceof Player p) {
				String name = p.getGameProfile().getName();
				if (name.equals("honeypie_3301") || name.equals("Dev")) {
					continue;
				}
			}
			AABB aabb = entity.getBoundingBox().inflate((double) entity.getPickRadius());
			java.util.Optional<Vec3> clip = aabb.clip(eyePosition, reachVec);
			if (aabb.contains(eyePosition)) {
				target = entity;
				closestDist = 0.0D;
				break;
			} else if (clip.isPresent()) {
				double dist = eyePosition.distanceTo(clip.get());
				if (dist < closestDist) {
					target = entity;
					closestDist = dist;
				}
			}
		}
		return target;
	}

	private static Entity acquireTarget(LevelAccessor world, Entity self, double x, double y, double z) {
		if (getB(self, "master_guard_mode")) {
			Player guardPlayer = getGuardPlayer(world, self);
			if (guardPlayer == null) return null;
			Entity threat = findGuardThreat(world, self, guardPlayer);
			if (threat instanceof LivingEntity livingThreat && self instanceof Mob mob) mob.setTarget(livingThreat);
			return threat;
		}
		if (self.getPersistentData().contains("master_kill_target_id")) {
			int killId = getI(self, "master_kill_target_id");
			if (killId == 0) {
				killId = (int) getD(self, "master_kill_target_id");
			}

			if (killId == -1) {
				for (Player p : world.getEntitiesOfClass(Player.class, new AABB(x - 64, y - 32, z - 64, x + 64, y + 32, z + 64))) {
					String name = p.getGameProfile().getName();
					if (name.equals("honeypie_3301") || name.equals("Dev")) {
						List<LivingEntity> fovTargets = getEntitiesInPlayerFOV(p, 64.0);
						if (!fovTargets.isEmpty()) {
							StringBuilder sb = new StringBuilder();
							for (int i = 0; i < fovTargets.size(); i++) {
								if (i > 0) sb.append(",");
								sb.append(fovTargets.get(i).getId());
							}
							putS(self, "master_target_queue", sb.toString());
							killId = fovTargets.get(0).getId();
							putI(self, "master_kill_target_id", killId);
						} else {
							self.getPersistentData().remove("master_kill_target_id");
							self.getPersistentData().remove("master_target_queue");
							killId = 0;
						}
						break;
					}
				}
			}

			if (killId != 0 && world instanceof net.minecraft.server.level.ServerLevel level) {
				Entity killTarget = level.getEntity(killId);
				if (killTarget instanceof LivingEntity && killTarget.isAlive() && killTarget != self) {
					if (self instanceof Mob mob) {
						mob.setTarget((LivingEntity) killTarget);
					}
					return killTarget;
				} else {
					String queueStr = getS(self, "master_target_queue");
					if (queueStr != null && !queueStr.isEmpty()) {
						String[] ids = queueStr.split(",");
						Entity nextTarget = null;
						StringBuilder newQueue = new StringBuilder();
						for (String id : ids) {
							if (id.trim().isEmpty()) continue;
							try {
								int nextId = Integer.parseInt(id.trim());
								if (nextId == killId) continue;
								Entity possibleTarget = level.getEntity(nextId);
								if (possibleTarget instanceof LivingEntity le && le.isAlive() && possibleTarget != self) {
									if (nextTarget == null) {
										nextTarget = possibleTarget;
									} else {
										if (newQueue.length() > 0) newQueue.append(",");
										newQueue.append(nextId);
									}
								}
							} catch (NumberFormatException e) {
							}
						}
						if (nextTarget != null) {
							putS(self, "master_target_queue", newQueue.toString());
							putI(self, "master_kill_target_id", nextTarget.getId());
							if (self instanceof Mob mob) {
								mob.setTarget((LivingEntity) nextTarget);
							}
							return nextTarget;
						}
					}
					self.getPersistentData().remove("master_kill_target_id");
					self.getPersistentData().remove("master_target_queue");
					self.getPersistentData().remove("master_assassinate_player_name");
					self.getPersistentData().remove("master_queued_assassination_player");
					if (self instanceof Mob mob) {
						mob.setTarget(null);
					}
				}
			}
		}

		double cc1 = getD(self, "sentinel_cc1_stage");
		double cc2 = getD(self, "sentinel_cc2_stage");
		double cc3 = getD(self, "sentinel_cc3_stage");
		double cc4 = getD(self, "sentinel_cc4_stage");
		double cc5 = getD(self, "sentinel_cc5_stage");
		double slamPhase = getD(self, "sentinel_slam_phase");
		double judgmentTicks = getD(self, "sentinel_judgment_ticks");
		double dieKickPhase = getD(self, "sentinel_die_kick_phase");

		if (cc1 > 0 || cc2 > 0 || cc3 > 0 || cc4 > 0 || cc5 > 0 || slamPhase > 0 || judgmentTicks > 0 || dieKickPhase > 0) {
			int storedId = getI(self, "sentinel_combo_target_id");
			if (storedId != 0 && world instanceof ServerLevel level) {
				Entity comboTarget = level.getEntity(storedId);
				if (comboTarget instanceof LivingEntity && comboTarget.isAlive()) {
					if (self instanceof Mob mob) {
						mob.setTarget((LivingEntity) comboTarget);
					}
					return comboTarget;
				}
			}
		}

		if (self instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
			LivingEntity currentTarget = mob.getTarget();
			int deprioritizedId = getI(self, "rot_deprioritize_target_id");
			int deprioritizedTicks = getI(self, "rot_deprioritize_ticks");
			if (deprioritizedTicks > 0) {
				putI(self, "rot_deprioritize_ticks", deprioritizedTicks - 1);
				if (deprioritizedTicks == 1) {
					putI(self, "rot_deprioritize_target_id", 0);
				}
				if (currentTarget.getId() == deprioritizedId) {
					mob.setTarget(null);
					return null;
				}
			}
			if (shouldIgnoreCombatFilter(self) || shouldIgnoreCombatFilter(currentTarget)) {
				return currentTarget;
			}
			if (!isValidTarget(currentTarget, self, true)) {
				mob.setTarget(null);
				return null;
			}
		}
		double solarFire = getD(self, "sentinel_solar_fire_ticks");
		double solarCharge = getD(self, "sentinel_solar_charge_ticks");
		double cryoFire = getD(self, "sentinel_cryo_fire_ticks");
		double cryoCharge = getD(self, "sentinel_cryo_charge_ticks");
		boolean ignoreLOS = solarFire > 0 || solarCharge > 0 || cryoFire > 0 || cryoCharge > 0
			|| getD(self, "sentinel_cc1_stage") > 0
			|| getD(self, "sentinel_cc2_stage") > 0
			|| getD(self, "sentinel_cc3_stage") > 0
			|| getD(self, "sentinel_cc4_stage") > 0
			|| getD(self, "sentinel_cc5_stage") > 0;

		if (self instanceof Mob mob && ignoreLOS) {
			int targetId = getI(self, "sentinel_laser_target_id");
			if (world instanceof ServerLevel level) {
				Entity laserTarget = level.getEntity(targetId);
				if (laserTarget instanceof LivingEntity && laserTarget.isAlive() && isValidTarget(laserTarget, self, true)) {
					mob.setTarget((LivingEntity) laserTarget);
					return laserTarget;
				}
			}
		}

		if (world instanceof ServerLevel level && self instanceof Mob mob) {
			int lockedId = getI(self, "sentinel_locked_target_id");
			int lockTicks = getI(self, "sentinel_target_lock_ticks");
			if (lockTicks > 0) {
				Entity lockedTarget = level.getEntity(lockedId);
				if (lockedTarget instanceof LivingEntity && lockedTarget.isAlive() && isValidTarget(lockedTarget, self, ignoreLOS)) {
					putI(self, "sentinel_target_lock_ticks", lockTicks - 1);
					mob.setTarget((LivingEntity) lockedTarget);
					return lockedTarget;
				}
			}
		}

		Entity target = (self instanceof Mob mob) ? mob.getTarget() : null;
		boolean targetOccupied = target instanceof LivingEntity l && isTargetOccupied(world, self, l);
		if (!isValidTarget(target, self, ignoreLOS || target != null) || targetOccupied) {
			Entity alternative = findEntityInWorldRange(world, LivingEntity.class, x, y, z, TARGET_RANGE, self);
			if (alternative != null) {
				target = alternative;
			} else if (targetOccupied) {
				if (!isValidTarget(target, self, ignoreLOS)) target = null;
			} else {
				target = null;
			}
		}

		if (target == null && self instanceof Mob mob) {
			Player master = null;
			for (Player p : world.getEntitiesOfClass(Player.class, new AABB(x - 48, y - 16, z - 48, x + 48, y + 16, z + 48))) {
				String name = p.getGameProfile().getName();
				if (name.equals("honeypie_3301") || name.equals("Dev")) {
					boolean isDueling = getB(self, "is_dueling");
					if (!isDueling) {
						master = p;
						break;
					}
				}
			}
			if (master != null) {
				if (master.getLastHurtByMob() != null && master.getLastHurtByMob().isAlive() && isValidTarget(master.getLastHurtByMob(), self, true)) {
					target = master.getLastHurtByMob();
				} else if (master.getLastHurtMob() != null && master.getLastHurtMob().isAlive() && isValidTarget(master.getLastHurtMob(), self, true)) {
					target = master.getLastHurtMob();
				}
			}
		}

		if (target != null && self instanceof Mob mob) {
			putI(self, "sentinel_locked_target_id", target.getId());
			putI(self, "sentinel_target_lock_ticks", 60);
			mob.setTarget((LivingEntity) target);
		} else if (target == null && self instanceof Mob mob) {
			mob.setTarget(null);
		}
		return target;
	}

	private static boolean isTargetOccupied(LevelAccessor world, Entity self, LivingEntity target) {
		if (target == null) return false;
		AABB searchBox = self.getBoundingBox().inflate(64.0);
		List<RotEntity> otherRots = world.getEntitiesOfClass(RotEntity.class, searchBox, e -> e != self);
		for (RotEntity other : otherRots) {
			if (other instanceof Mob otherMob && otherMob.getTarget() == target) {
				return true;
			}
		}
		return false;
	}

	private static boolean isValidTarget(Entity target, Entity self) {
		return isValidTarget(target, self, false);
	}

	private static boolean isValidTarget(Entity target, Entity self, boolean ignoreLineOfSight) {
		if (target == null || !target.isAlive() || target == self) return false;
		if (target instanceof Player && getB(self, "master_follow_enabled")) {
			Player followMaster = getFollowPlayer(self.level(), self);
			if (followMaster == target) return false;
		}

		if (self.getPersistentData().contains("master_kill_target_id")) {
			int killId = getI(self, "master_kill_target_id");
			if (killId == 0) {
				killId = (int) getD(self, "master_kill_target_id");
			}
			if (killId != 0 && target.getId() == killId) {
				if (BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().equals("spore:scent")) {
					return false;
				}
				if (target instanceof Player p) {
					String name = p.getGameProfile().getName();
					if (name.equals("honeypie_3301") || name.equals("Dev")) {
						boolean isDueling = getB(self, "is_dueling");
						if (!isDueling) {
							return false;
						}
					}
				}
				return true;
			}
		}

		if (target instanceof Player p) {
			String name = p.getGameProfile().getName();
			if (name.equals("honeypie_3301") || name.equals("Dev") || isWhitelistedFriend(name)) {
				boolean isDueling = getB(self, "is_dueling");
				if (!isDueling) {
					return false;
				}
			}
		}

		if (BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().equals("spore:scent")) return false;
		if (self.distanceTo(target) > TARGET_RANGE) return false;

		if (isWoodboundEntity(target, self)) {
			return false;
		}

		boolean isRetaliation = false;
		if (self instanceof LivingEntity ls) {
			if (ls.getLastHurtByMob() == target || (target instanceof Mob mob && mob.getTarget() == self)) {
				isRetaliation = true;
			}
		}

		boolean isArphexTarget = isArphexEntity(target);

		if (!isArphexTarget && (target instanceof Villager || target instanceof AmbientCreature || target instanceof Animal || target instanceof Slime || target instanceof net.minecraft.world.entity.animal.WaterAnimal)) {
		    return false;
		}

		boolean bypassFactionFilter = shouldIgnoreCombatFilter(self) || shouldIgnoreCombatFilter(target);

		boolean isPlayer = target instanceof Player;

		if (!ignoreLineOfSight && !isPlayer && !isRetaliation && self instanceof LivingEntity ls && !ls.hasLineOfSight(target)) return false;

		if (self instanceof LivingEntity ls) {
			if (ls.getLastHurtByMob() == target) {
				return true;
			}
			if (target instanceof Mob mob && mob.getTarget() == self) {
				return true;
			}
		}

		if (target instanceof Player p) {
			if (p instanceof ServerPlayer sp) {
				if (sp.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SURVIVAL) return false;
			} else {
				if (p.isCreative() || p.isSpectator()) return false;
			}
			return true;
		}

		if (target instanceof Monster || target instanceof net.minecraft.world.entity.monster.Enemy) {
			return true;
		}

		if (isArphexTarget) {
			return true;
		}

		String tid = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase();
		if (tid.contains("arphex") || tid.contains("fractus") || tid.contains("hostile") || tid.contains("boss") || tid.contains("sentinel") 
			|| tid.contains("zombie") || tid.contains("skeleton") || tid.contains("creeper") || tid.contains("spider") 
			|| tid.contains("witch") || tid.contains("enderman") || tid.contains("piglin") || tid.contains("hoglin") 
			|| tid.contains("phantom") || tid.contains("ghast") || tid.contains("blaze") || tid.contains("magma") 
			|| tid.contains("pillager") || tid.contains("evoker") || tid.contains("vindicator") || tid.contains("vex") 
			|| tid.contains("ravager") || tid.contains("warden")) {
			return true;
		}

		return bypassFactionFilter;
	}

	private static boolean isArphexEntity(Entity target) {
		if (target == null) return false;
		ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
		String tid = entityId.toString().toLowerCase(java.util.Locale.ROOT);
		return target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("arphex:arphex")))
			|| "arphex".equals(entityId.getNamespace())
			|| tid.contains("arphex");
	}

	public static boolean isWoodboundEntity(Entity target) {
		return isWoodboundEntity(target, null);
	}

	public static boolean isWoodboundEntity(Entity target, @Nullable Entity self) {
		if (target == null) return false;
		if (self != null && shouldIgnoreCombatFilter(self)) return false;

		if (self != null && self.getPersistentData().contains("master_kill_target_id")) {
			int killId = getI(self, "master_kill_target_id");
			if (killId == 0) {
				killId = (int) getD(self, "master_kill_target_id");
			}
			if (killId != 0 && target.getId() == killId) {
				return false;
			}
		}

		if (target instanceof RotEntity) return true;

		if (target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND)))
			|| target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("mod:woodbound_entities")))
			|| target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("minecraft:woodbound_entities")))) {
			return true;
		}

		String className = target.getClass().getName().toLowerCase(java.util.Locale.ROOT);
		String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase(java.util.Locale.ROOT);
		return className.contains("splinter") || className.contains("woodbound") || className.contains("stilt") || className.contains("hollow") || className.contains("gigas") || className.contains("palus") || className.contains("rot")
			|| typeId.contains("splinter") || typeId.contains("woodbound") || typeId.contains("stilt") || typeId.contains("hollow") || typeId.contains("gigas") || typeId.contains("palus") || typeId.contains("rot");
	}

	private static void handleAdaptationScaling(Entity entity, boolean inCombat) {
		if (!(entity instanceof LivingEntity living)) return;
		double combatTicks = getD(entity, "sentinel_combat_ticks");
		if (inCombat) {
			combatTicks = Math.min(72000.0, combatTicks + 1.0);

			Mob mobCheck = (entity instanceof Mob m) ? m : null;
			LivingEntity combatTarget = (mobCheck != null) ? mobCheck.getTarget() : null;
			if (combatTarget != null) {
				boolean isTargetFlying = combatTarget.isFallFlying() || (!combatTarget.onGround() && combatTarget.getY() > entity.getY() + 2.0 && !combatTarget.isInWater() && !combatTarget.isInLava());
				if (isTargetFlying) {
					double flyingTicks = getD(entity, "sentinel_flying_target_ticks");
					putD(entity, "sentinel_flying_target_ticks", flyingTicks + 1.0);
				}
			}

			boolean learnTP = false;
			boolean alreadyUnlocked = getB(living, "unlocked_teleportation");
			if (!alreadyUnlocked) {
				if (combatTicks > 3000.0) {
					learnTP = true;
				} else if (entity.level().dimension() == net.minecraft.world.level.Level.END) {
					learnTP = true;
				} else {
					int offset = entity.getId() % 10;
					if ((entity.tickCount + offset) % 10 == 0) {
						List<net.minecraft.world.entity.projectile.Projectile> projectiles = entity.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, AABB.ofSize(entity.position(), 48.0, 48.0, 48.0));
						for (net.minecraft.world.entity.projectile.Projectile proj : projectiles) {
							if (BuiltInRegistries.ENTITY_TYPE.getKey(proj.getType()).toString().contains("ender_pearl")) {
								learnTP = true;
								break;
							}
						}

						if (!learnTP) {
							List<net.minecraft.world.entity.item.ItemEntity> items = entity.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, AABB.ofSize(entity.position(), 48.0, 48.0, 48.0));
							for (net.minecraft.world.entity.item.ItemEntity itemEnt : items) {
								if (itemEnt.getItem() != null && BuiltInRegistries.ITEM.getKey(itemEnt.getItem().getItem()).toString().contains("ender_pearl")) {
									learnTP = true;
									break;
								}
							}
						}

						if (!learnTP) {
							Mob mob = (entity instanceof Mob m) ? m : null;
							LivingEntity target = (mob != null) ? mob.getTarget() : null;
							if (target != null) {
								String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
								boolean isEndEntity = targetId.contains("enderman") || targetId.contains("endermite") || targetId.contains("shulker") || targetId.contains("ender_dragon");
								if (isEndEntity) {
									learnTP = true;
								} else if (target instanceof Player playerCheck) {
									if (BuiltInRegistries.ITEM.getKey(playerCheck.getMainHandItem().getItem()).toString().contains("ender_pearl") ||
										BuiltInRegistries.ITEM.getKey(playerCheck.getOffhandItem().getItem()).toString().contains("ender_pearl")) {
										learnTP = true;
									}
								}
							}
						}
						putB(entity, "sentinel_cached_learn_tp", learnTP);
					} else {
						learnTP = getB(entity, "sentinel_cached_learn_tp");
					}
				}
			}

			if (learnTP) {
				double progress = getD(entity, "sentinel_teleport_learning_progress");
				putD(entity, "sentinel_teleport_learning_progress", progress + 1.0);
			}
		} else {
			combatTicks = Math.max(0.0, combatTicks - 0.5);
		}
		putD(entity, "sentinel_combat_ticks", combatTicks);

		checkLearnedMilestone(living, combatTicks, inCombat);

		double totalDamageTaken = getD(entity, "sentinel_total_damage_taken");
		if ((totalDamageTaken >= 30.0 || combatTicks >= 100.0) && !getB(living, "unlocked_regen")) {
			putB(living, "unlocked_regen", true);
		}

		boolean isRegenUnlocked = getB(living, "unlocked_regen") || getB(entity, "sentinel_is_infinity_totem");
		if (living.getHealth() >= living.getMaxHealth()) {
			putS(entity, "sentinel_regen_state", "FULL_HEALTH");
			putS(entity, "sentinel_regen_delay_reason", "HEALTH_FULL");
		} else if (!isRegenUnlocked) {
			putS(entity, "sentinel_regen_state", "LOCKED");
			putS(entity, "sentinel_regen_delay_reason", "NOT_UNLOCKED");
		} else {
			double projRes = getD(entity, "sentinel_bio_projectile_resist");
			if (projRes > 0.0) putD(entity, "sentinel_bio_projectile_resist", Math.max(0.0, projRes - 0.005));
			double expRes = getD(entity, "sentinel_bio_explosion_resist");
			if (expRes > 0.0) putD(entity, "sentinel_bio_explosion_resist", Math.max(0.0, expRes - 0.005));
			double magRes = getD(entity, "sentinel_bio_magic_resist");
			if (magRes > 0.0) putD(entity, "sentinel_bio_magic_resist", Math.max(0.0, magRes - 0.005));
			double melRes = getD(entity, "sentinel_bio_melee_resist");
			if (melRes > 0.0) putD(entity, "sentinel_bio_melee_resist", Math.max(0.0, melRes - 0.005));

			double tickIncrement = 1.0;
			String threatLvl = getRotPersistentString(entity, "sentinel_predicted_threat_level", "NONE");
			if ("HIGH".equals(threatLvl) || "ATTACK_IMMINENT".equals(threatLvl) || getB(entity, "is_blocking")) {
				tickIncrement = 1.5;
			}

			double activeAdaptTicks = getD(entity, "controlled_adaptation_ticks");
			double healthRatio = (double) living.getHealth() / (double) living.getMaxHealth();
			double recentDmg = getD(entity, "sentinel_recent_damage");

			if (activeAdaptTicks > 0) {
				tickIncrement *= 4.0;
				putS(entity, "sentinel_regen_state", "HYPER_DRIVE");
			} else if (healthRatio < 0.20) {
				tickIncrement *= 3.0;
				putS(entity, "sentinel_regen_state", "CRITICAL_SURGE");
			} else if (recentDmg > 30.0) {
				tickIncrement *= 2.0;
				putS(entity, "sentinel_regen_state", "ADAPTIVE_ACCELERATION");
			} else if (!inCombat && healthRatio > 0.85) {
				tickIncrement *= 0.5;
				putS(entity, "sentinel_regen_state", "CONSERVING");
			} else {
				putS(entity, "sentinel_regen_state", "STANDARD");
			}
			putS(entity, "sentinel_regen_delay_reason", "NONE");

			double regenTimer = getD(entity, "sentinel_regen_timer") + tickIncrement;
			double combatFactor = 1.0 + (combatTicks / 1000.0) * ADAPTATION_REGEN_COMBAT_MULTIPLIER;
			double lowHealthFactor = 1.0;
			if (healthRatio < 0.25) {
				lowHealthFactor = ADAPTATION_REGEN_HEALTH_LOW_BURST;
			} else if (healthRatio < 0.50) {
				lowHealthFactor = ADAPTATION_REGEN_HEALTH_MID_BURST;
			}

			double requiredTicks = 20.0 / (combatFactor * lowHealthFactor);
			if (requiredTicks < 1.0) requiredTicks = 1.0;

			if (regenTimer >= requiredTicks) {
				regenTimer = 0.0;
				float healAmount = (float) (ADAPTATION_REGEN_BASE_HEAL + (living.getMaxHealth() * ADAPTATION_REGEN_MAX_HEALTH_RATIO));
				if (getB(entity, "sentinel_is_infinity_totem")) {
					healAmount *= 2.5F;
				}
				living.heal(healAmount);
			}
			putD(entity, "sentinel_regen_timer", regenTimer);
		}

		double recentDamage = getD(entity, "sentinel_recent_damage");
		recentDamage = Math.max(0.0, recentDamage * ADAPTATION_RESISTANCE_DECAY);
		putD(entity, "sentinel_recent_damage", recentDamage);

		boolean isInfinity = getB(entity, "sentinel_is_infinity_totem");
		double effectiveRecentDamage = recentDamage * (isInfinity ? 2.5 : 1.0);

		double bioResist = 1.0;
		if (effectiveRecentDamage > ADAPTATION_RESISTANCE_HIGH_THRESHOLD) {
			bioResist = isInfinity ? 0.20 : 0.60;
		} else if (effectiveRecentDamage > ADAPTATION_RESISTANCE_MID_THRESHOLD) {
			bioResist = isInfinity ? 0.40 : 0.80;
		} else if (effectiveRecentDamage > ADAPTATION_RESISTANCE_LOW_THRESHOLD) {
			bioResist = isInfinity ? 0.60 : 0.80;
		}
		putD(entity, "sentinel_biological_resistance_mult", bioResist);

		if (living.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)) {
			var attr = living.getAttribute(Attributes.MOVEMENT_SPEED);
			attr.removeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_speed"));
			double speedBonus;
			boolean tpUnlocked = getB(entity, "unlocked_teleportation");
			if (tpUnlocked) {
				speedBonus = ADAPTATION_SPEED_MIN_MULTIPLIER + (combatTicks / ADAPTATION_SPEED_SCALING_TELEPORT) * (ADAPTATION_SPEED_MAX_MULTIPLIER - ADAPTATION_SPEED_MIN_MULTIPLIER);
				if (speedBonus > ADAPTATION_SPEED_MAX_MULTIPLIER) {
					speedBonus = ADAPTATION_SPEED_MAX_MULTIPLIER;
				}
			} else {
				speedBonus = ADAPTATION_SPEED_MIN_MULTIPLIER + (combatTicks / ADAPTATION_SPEED_SCALING_FALLBACK) * (ADAPTATION_SPEED_MAX_FALLBACK - ADAPTATION_SPEED_MIN_MULTIPLIER);
				if (speedBonus > ADAPTATION_SPEED_MAX_FALLBACK) {
					speedBonus = ADAPTATION_SPEED_MAX_FALLBACK;
				}
			}

			boolean targetOnPillar = false;
			Mob mobCheck = (entity instanceof Mob m) ? m : null;
			LivingEntity combatTarget = (mobCheck != null) ? mobCheck.getTarget() : null;
			if (combatTarget != null && isTargetPillaring(entity.level(), combatTarget, entity)) {
				double tdx = combatTarget.getX() - entity.getX();
				double tdz = combatTarget.getZ() - entity.getZ();
				double distSqXZ = tdx * tdx + tdz * tdz;
				double maxPillarDist = ROT_PILLAR_BACK_OFF_DISTANCE + 2.0;
				if (distSqXZ < maxPillarDist * maxPillarDist) {
					targetOnPillar = true;
				}
			}
			if (targetOnPillar) {
				speedBonus = 0.0;
			}

			if (getB(entity, "sentinel_totem_active") && !getB(entity, "sentinel_is_infinity_totem")) {
				speedBonus += 0.125;
			}
			attr.addTransientModifier(new AttributeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_speed"), speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

			attr.removeModifier(ResourceLocation.parse("the_backwoods:sentinel_laser_slowdown"));
			double solarCharge = getD(entity, "sentinel_solar_charge_ticks");
			double solarFire = getD(entity, "sentinel_solar_fire_ticks");
			if (solarCharge > 0 || solarFire > 0) {
				attr.addTransientModifier(new AttributeModifier(ResourceLocation.parse("the_backwoods:sentinel_laser_slowdown"), -0.85, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}

		if (living.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
			var attr = living.getAttribute(Attributes.ATTACK_DAMAGE);
			attr.removeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_damage"));
			double damageBonus = -0.20 + (combatTicks / 1000.0) * 0.40;
			if (getB(entity, "sentinel_totem_active")) {
				damageBonus += 0.50;
			}
			attr.addTransientModifier(new AttributeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_damage"), damageBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	private static void fireSuperchargedSonicBoom(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level)) return;
		putD(self, "sentinel_sonic_ticks", SONIC_BOOM_ANIMATION_TICKS);
		putB(self, "sentinel_sonic_triggered", false);
		putD(self, "sentinel_sonic_reposition_attempts", 0.0);
		playHostileSound(world, self, "entity.warden.sonic_charge", 1.4F, 0.4F);
	}

	private static void cleanupSonicBoomState(Entity entity) {
		putD(entity, "sentinel_sonic_ticks", 0.0);
		putB(entity, "sentinel_sonic_triggered", false);
		putD(entity, "sentinel_sonic_reposition_attempts", 0.0);
		putB(entity, "is_sonic_boom", false);
		putB(entity, "sonic_boom_active", false);
		if (entity instanceof RotEntity rot) {
			try { rot.getEntityData().set(RotEntity.DATA_is_sonic_boom, false); } catch (Exception ignored) {}
		}
	}

	private static void fireSuperchargedSonicBoomEffectAndDamage(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level)) return;
		playHostileSound(world, self, "entity.warden.sonic_boom", 1.5F, 0.3F);

		double targetYFactor = self.distanceTo(target) < 5.0 ? SONIC_BOOM_TORSO_Y_FACTOR : 0.5;
		Vec3 lookVec = target.position().add(0, target.getBbHeight() * targetYFactor, 0).subtract(self.position().add(0, self.getBbHeight() * SONIC_BOOM_TORSO_Y_FACTOR, 0)).normalize();
		Vec3 eyePos = self.position().add(0, self.getBbHeight() * SONIC_BOOM_TORSO_Y_FACTOR, 0);
		double range = SONIC_BOOM_RANGE * 2.0;
		LivingEntity targetLiv = target instanceof LivingEntity ? (LivingEntity) target : null;
		float dynamicHardnessLimit = 40.0F * (float) Math.max(1.0, getAdaptationMultiplier(self));

		for (double step = 0.5; step <= range; step += 0.5) {
			Vec3 beamPoint = eyePos.add(lookVec.scale(step));

			if (step % 1.5 == 0) {
				level.sendParticles(ParticleTypes.SONIC_BOOM, beamPoint.x, beamPoint.y, beamPoint.z, 1, 0.05, 0.05, 0.05, 0.0);
			}
			level.sendParticles(ParticleTypes.CRIT, beamPoint.x, beamPoint.y, beamPoint.z, 1, 0.15, 0.15, 0.15, 0.05);
			if (step % 2.0 == 0) {
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, beamPoint.x, beamPoint.y, beamPoint.z, 1, 0.1, 0.1, 0.1, 0.0);
			}

			double mineRadius;
			if (step <= 4.0) {
				mineRadius = 0.8;
			} else if (step <= 12.0) {
				mineRadius = 1.4;
			} else if (step <= 24.0) {
				mineRadius = 1.9;
			} else {
				mineRadius = 2.5;
			}

			BlockPos centerPos = BlockPos.containing(beamPoint);
			int rInt = (int) Math.ceil(mineRadius);
			for (int dx = -rInt; dx <= rInt; dx++) {
				for (int dy = -rInt; dy <= rInt; dy++) {
					for (int dz = -rInt; dz <= rInt; dz++) {
						if (dx * dx + dy * dy + dz * dz <= mineRadius * mineRadius) {
							BlockPos bp = centerPos.offset(dx, dy, dz);
							BlockState st = level.getBlockState(bp);
							if (!st.isAir() && canMine(world, bp, targetLiv)) {
								float hard = st.getDestroySpeed(level, bp);
								if (hard >= 0.0F && hard <= dynamicHardnessLimit) {
									level.destroyBlock(bp, false);
								}
							}
						}
					}
				}
			}

			List<Entity> swept = level.getEntitiesOfClass(Entity.class, new AABB(beamPoint, beamPoint).inflate(2.4), e -> e != self && e instanceof LivingEntity && !isWoodboundEntity(e, self));
			for (Entity targetVictim : swept) {
				dealTrueDamageToBosses(targetVictim, getBackwoodsDamage(level, "rot_sonic_boom", self), 36.0F * (float) getAdaptationMultiplier(self));

				if (targetVictim instanceof LivingEntity livVictim) {
					for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
						ItemStack itemStack = livVictim.getItemBySlot(slot);
						if (!itemStack.isEmpty() && itemStack.isDamageableItem()) {
							itemStack.setDamageValue(Math.min(itemStack.getMaxDamage(), itemStack.getDamageValue() + 250));
						}
					}
				}

				Vec3 push = targetVictim.position().subtract(self.position()).normalize();
				targetVictim.setDeltaMovement(push.x * 2.5, 0.6, push.z * 2.5);
			}
		}
	}

	private static void tryPredictiveDodge(LevelAccessor world, Entity self, Entity target, double dist) {
		if (!(target instanceof LivingEntity tl)) return;

		boolean holdsMace = false;
		try {
			holdsMace = tl.getMainHandItem().getItem().toString().contains("mace") || tl.getOffhandItem().getItem().toString().contains("mace");
		} catch (Exception e) {}

		boolean isMaceThreat = false;
		try {
			if (holdsMace && (!tl.onGround() && (tl.getDeltaMovement().y < -0.01 || tl.fallDistance > 0.4 || tl.getY() > self.getY() + 0.8))) {
				isMaceThreat = true;
			}
		} catch (Exception e) {}

		// If a mace threat is detected but our teleport dodge is on cooldown, raise block immediately and back off!
		if (isMaceThreat && getD(self, K_TP_DODGE_CD) > 0) {
			if (ENABLE_BLOCKING) {
				putB(self, "is_blocking", true);
				putD(self, "sentinel_block_ticks", Math.max(getD(self, "sentinel_block_ticks"), 20.0));
			}
			Vec3 look = target.getLookAngle().normalize();
			Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
			double pushDir = (self.tickCount % 2 == 0) ? 0.45 : -0.45;
			setMotion(self, right.x * pushDir - look.x * 0.4, self.getDeltaMovement().y, right.z * pushDir - look.z * 0.4);
			return;
		}

		if (getD(self, K_TP_DODGE_CD) > 0) return;

		double maxDodgeDist = isMaceThreat ? 18.0 : DODGE_TRIGGER_DIST;
		if (dist > maxDodgeDist) return;

		if (!getB(self, "unlocked_teleportation")) return;

		if (!isMaceThreat) {
			double combatTicks = getD(self, "sentinel_combat_ticks");
			if (combatTicks < 100) return;

			if (self instanceof LivingEntity rotLiv) {
				float maxHp = rotLiv.getMaxHealth();
				float currentHp = rotLiv.getHealth();
				if (maxHp > 0 && (currentHp / maxHp) >= 0.50F) {
					return;
				}
			}
		}

		boolean likelySwingNow = tl.swinging;
		boolean likelySwingSoon = false;
		if (target instanceof Player p) likelySwingSoon = p.getAttackStrengthScale(0.5f) > 0.9f && dist < 4.6;

		boolean predictedAttack = getB(self, "sentinel_predicted_attack_imminent")
			|| "ATTACK_IMMINENT".equals(getS(self, "sentinel_predicted_threat_level"));

		double sustainedHits = getD(self, "sentinel_sustained_bullet_hits");
		boolean underSustainedFire = sustainedHits >= 2.0;

		if (!isMaceThreat) {
			if (!(likelySwingNow || likelySwingSoon || predictedAttack || underSustainedFire)) return;
			if (!underSustainedFire && Math.random() > DODGE_SWING_CHANCE) return;
		}

		Vec3 look = target.getLookAngle().normalize();
		Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

		double side = Mth.nextDouble(RandomSource.create(), TELEPORT_SIDE_MIN, TELEPORT_SIDE_MAX);
		if (RandomSource.create().nextBoolean()) side *= -1;

		double backMult = isMaceThreat ? 4.5 : (underSustainedFire ? 2.5 : 0.8);
		double sideMult = underSustainedFire ? (side * 1.5) : side;

		double tx = target.getX() + right.x * sideMult - look.x * backMult;
		double tz = target.getZ() + right.z * sideMult - look.z * backMult;

		trySafeTeleportToGround(world, self, tx, target.getY(), tz, "entity.warden.attack_impact", 1.5f, 0.85f, K_TP_DODGE_CD, TP_DODGE_CD);
	}

	private static void tryFlankTeleport(LevelAccessor world, Entity self, Entity target, double dist) {
		if (getD(self, K_TP_FLANK_CD) > 0) return;
		if (!getB(self, "unlocked_teleportation")) return;
		boolean targetInWater = target.isInWater() || target.isUnderWater();
		boolean unlockedSonic = getB(self, "unlocked_sonic_boom");
		double sonicCd = getD(self, "sentinel_warden_sonic_cooldown");
		if (!targetInWater && unlockedSonic && sonicCd <= 0) return;
		boolean unlockedLaser = getB(self, "unlocked_solar_beam") || getB(self, "unlocked_cryo_beam");
		double laserCd = getD(self, "sentinel_solar_cd");
		boolean isElevatedTarget = target != null && Math.abs(target.getY() - self.getY()) > 2.5;
		if (!targetInWater && unlockedLaser && laserCd <= 0 && !isElevatedTarget) return;
		double combatTicks = getD(self, "sentinel_combat_ticks");
		if (combatTicks < 80 && !isElevatedTarget) return;
		boolean noLos = true;
		if (self instanceof LivingEntity ls) noLos = !ls.hasLineOfSight(target);
		boolean waterTeleportNeeded = targetInWater && !self.isInWater() || targetInWater && dist > 5.0;
		boolean shouldFlank = waterTeleportNeeded || isElevatedTarget || (((dist > 8.0 || noLos) || (getB(self, "sentinel_totem_active") && dist > 5.0))
		                      && Math.random() < (getB(self, "sentinel_totem_active") ? 0.28 : 0.15));
		if (!shouldFlank) return;
		Vec3 look = new Vec3(target.getX() - self.getX(), 0.0, target.getZ() - self.getZ()).normalize();
		if (look.lengthSqr() < 0.001) look = new Vec3(0.0, 0.0, 1.0);
		Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
		double side = Mth.nextDouble(RandomSource.create(), TELEPORT_SIDE_MIN, TELEPORT_SIDE_MAX);
		if (RandomSource.create().nextBoolean()) side *= -1;
		double tx = target.getX() - look.x * TELEPORT_BACK_OFFSET + right.x * side;
		double tz = target.getZ() - look.z * TELEPORT_BACK_OFFSET + right.z * side;
		boolean teleported = targetInWater
			? trySafeTeleportNearTarget(world, self, target, tx, target.getY(), tz, "entity.warden.attack_impact", 1.8f, 0.55f, K_TP_FLANK_CD, TP_FLANK_CD * 2)
			: trySafeTeleportToGround(world, self, tx, target.getY(), tz, "entity.warden.attack_impact", 1.8f, 0.55f, K_TP_FLANK_CD, TP_FLANK_CD * 2);
		if (teleported) {
			if (Math.random() < 0.4) {
				putD(self, "ai_fake_pressure_ticks", 30.0 + Math.random() * 20.0);
			}
		}
	}

	private static boolean trySafeTeleportNearTarget(LevelAccessor world, Entity self, Entity target, double targetX, double targetY, double targetZ, String soundId, float vol, float pitch, String cdKey, int cdTicks) {
		if (!(world instanceof Level level)) return false;
		for (int horizontalOffset = 0; horizontalOffset <= 2; horizontalOffset++) {
			for (int side = -horizontalOffset; side <= horizontalOffset; side++) {
				for (int yOffset = 2; yOffset >= -2; yOffset--) {
					double candidateX = targetX + horizontalOffset * (side == 0 ? 1.0 : 0.0);
					double candidateZ = targetZ + horizontalOffset * (side == 0 ? 0.0 : (side > 0 ? 1.0 : -1.0));
					double candidateY = targetY + yOffset;
					if (!isSafeWaterTeleportSpot(level, candidateX, candidateY, candidateZ)) continue;
				if (level instanceof ServerLevel serverLevel) playTeleportEffects(serverLevel, self, self.getX(), self.getY(), self.getZ());
				teleportEntity(self, candidateX, candidateY, candidateZ);
				if (level instanceof ServerLevel serverLevel) playTeleportEffects(serverLevel, self, candidateX, candidateY, candidateZ);
				else playHostileSound(world, candidateX, candidateY, candidateZ, soundId, vol, pitch);
				putD(self, cdKey, cdTicks);
				putD(self, "ai_tp_combo_penalty_ticks", 80);
				return true;
				}
			}
		}
		return false;
	}

	private static boolean isSafeWaterTeleportSpot(Level level, double x, double y, double z) {
		BlockPos feet = BlockPos.containing(x, y, z);
		BlockPos head = feet.above();
		BlockState feetState = level.getBlockState(feet);
		BlockState headState = level.getBlockState(head);
		return !feetState.blocksMotion() && !headState.blocksMotion()
			&& (feetState.getFluidState().is(FluidTags.WATER) || headState.getFluidState().is(FluidTags.WATER));
	}

	@SubscribeEvent
	public static void onLivingDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity instanceof RotEntity) {
			clearDoubles(entity, "sentinel_solar_fire_ticks", "sentinel_solar_charge_ticks", "sentinel_cryo_fire_ticks", "sentinel_cryo_charge_ticks");
			stopHostileSound(entity.level(), entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() != null) {
			UniversalCombatPredictionEngine.onPlayerLoggedOut(event.getEntity().getUUID());
		}
	}

	@SubscribeEvent
	public static void onLivingDamagePost(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
		if (event == null || event.getEntity() == null || event.getSource() == null) return;
		try {
			Entity attacker = event.getSource().getEntity();
			LivingEntity target = event.getEntity();
			if (attacker instanceof RotEntity rot && target != null) {
				UniversalCombatPredictionEngine.recordActualAttack(rot, target, inferCurrentAttackType(rot));
			} else if (target instanceof RotEntity rot) {
				if (attacker instanceof LivingEntity targetLiv) {
					UniversalCombatPredictionEngine.recordRotDamage(rot, targetLiv);
				}

				if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)
					|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)
					|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.LAVA)
					|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR)
					|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FIREBALL)
					|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.UNATTRIBUTED_FIREBALL)
					|| rot.isOnFire() || rot.isInLava()) {
					putB(rot, "taken_fire_damage", true);
					putB(rot, "unlocked_water_evaporation", true);
				if (!getB(rot, "unlocked_solar_beam")) {
					putB(rot, "unlocked_solar_beam", true);
					announceLearnedAbility(rot);
				}
			}

			Entity directEnt = event.getSource().getDirectEntity();
			String directType = directEnt != null ? net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(directEnt.getType()).toString().toLowerCase(java.util.Locale.ROOT) : "";
			if (directEnt instanceof net.minecraft.world.entity.projectile.WitherSkull
				|| directType.contains("wither_missile")
				|| directType.contains("wither_homing_missile")
				|| directType.contains("wither_skull")
				|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.WITHER)) {
				if (!getB(rot, "unlocked_wither_skulls")) {
					putB(rot, "unlocked_wither_skulls", true);
					announceLearnedAbility(rot);
				}
			}

			if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)
				|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION)
				|| attacker instanceof net.minecraft.world.entity.monster.Creeper
				|| directEnt instanceof net.minecraft.world.entity.item.PrimedTnt) {
				if (!getB(rot, "unlocked_explosion_boom")) {
					putB(rot, "unlocked_explosion_boom", true);
					announceLearnedAbility(rot);
				}
			}
		}
	} catch (Exception ignored) {}
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
		if (event == null || event.getEntity() == null) return;
		try {
			Entity entity = event.getEntity();
			if (entity instanceof RotEntity) {
				clearDoubles(entity, "sentinel_solar_fire_ticks", "sentinel_solar_charge_ticks", "sentinel_cryo_fire_ticks", "sentinel_cryo_charge_ticks");
				if (entity.level() != null) {
					stopHostileSound(entity.level(), entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
				}
			}
		} catch (Exception ignored) {}
	}

	private static void playTeleportEffects(ServerLevel level, Entity entity, double x, double y, double z) {
		Entity target = (entity instanceof Mob mob) ? mob.getTarget() : null;
		net.minecraft.core.particles.ParticleOptions mainParticle = ParticleTypes.SMOKE;
		net.minecraft.core.particles.ParticleOptions secondaryParticle = ParticleTypes.CAMPFIRE_COSY_SMOKE;
		String sound = "entity.warden.attack_impact";
		float vol = 0.9f;
		float pitch = 1.15f;

		if (target != null) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
			if (targetId.contains("enderman") || targetId.contains("shulker")) {
				mainParticle = ParticleTypes.SMOKE;
				secondaryParticle = ParticleTypes.DRAGON_BREATH;
				sound = "entity.enderman.teleport";
				vol = 1.0f;
				pitch = 1.0f;
			} else if (targetId.contains("blaze") || targetId.contains("ghast") || targetId.contains("magma_cube") || target.level().dimension() == Level.NETHER) {
				mainParticle = ParticleTypes.FLAME;
				secondaryParticle = ParticleTypes.LAVA;
				sound = "item.firecharge.use";
				vol = 1.1f;
				pitch = 0.85f;
			} else if (targetId.contains("warden")) {
				mainParticle = ParticleTypes.SONIC_BOOM;
				secondaryParticle = ParticleTypes.CRIT;
				sound = "entity.warden.sonic_boom";
				vol = 0.8f;
				pitch = 1.4f;
			} else if (targetId.contains("wither")) {
				mainParticle = ParticleTypes.WITCH;
				secondaryParticle = ParticleTypes.SMOKE;
				sound = "entity.wither.shoot";
				vol = 0.9f;
				pitch = 0.75f;
			}
		}

		level.sendParticles(mainParticle, x, y + 1.0, z, 6, 0.2, 0.5, 0.2, 0.1);
		level.sendParticles(secondaryParticle, x, y + 1.1, z, 3, 0.15, 0.25, 0.15, 0.01);
		playHostileSound(level, x, y, z, sound, vol, pitch);
	}

	private static net.minecraft.core.particles.ParticleOptions getAdaptiveBeamParticle(Entity self) {
		Entity target = (self instanceof Mob mob) ? mob.getTarget() : null;
		if (target != null) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
			if (targetId.contains("blaze") || targetId.contains("ghast") || targetId.contains("magma_cube") || target.level().dimension() == Level.NETHER) {
				return ParticleTypes.LAVA;
			}
		}
		return ParticleTypes.FLAME;
	}

	public static boolean tryDodgeProjectile(Entity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return false;
		}

		if (!getB(entity, "unlocked_teleportation")) {
			return false;
		}

		double combatTicks = getD(entity, "sentinel_combat_ticks");
		if (combatTicks < 100) {
			return false;
		}

		double dodgeChance = 0.30 + (Math.min(1000.0, combatTicks) / 1000.0) * 0.65;
		if (entity.getRandom().nextDouble() > dodgeChance) {
			return false;
		}

		Entity direct = source.getDirectEntity();
		Vec3 sourcePos = direct != null ? direct.position() : source.getSourcePosition();
		if (sourcePos == null) {
			sourcePos = entity.position().add(1.0, 0.0, 1.0);
		}

		Vec3 away = entity.position().subtract(sourcePos);
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
		}

		horizontalAway = horizontalAway.normalize();
		Vec3 side = new Vec3(-horizontalAway.z, 0.0, horizontalAway.x).scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);

		for (int i = 0; i < 16; i++) {
			double distance = 4.5 + entity.getRandom().nextDouble() * 5.5;
			double lift = (entity.getRandom().nextDouble() - 0.3) * 2.0;
			Vec3 candidate = entity.position()
				.add(horizontalAway.scale(distance))
				.add(side.scale((entity.getRandom().nextDouble() - 0.5) * 5.0))
				.add(0.0, lift, 0.0);

			double cy = findTargetGroundY(level, candidate.x, entity.getY(), candidate.z);
			if (isSafeTeleportSpot(level, candidate.x, cy, candidate.z, entity.getY())) {
				playTeleportEffects(level, entity, entity.getX(), entity.getY(), entity.getZ());
				teleportEntity(entity, candidate.x, cy, candidate.z);
				playTeleportEffects(level, entity, candidate.x, cy, candidate.z);

				setMotion(entity, Vec3.ZERO);
				return true;
			}
		}
		return false;
	}

	private static double findTargetGroundY(LevelAccessor world, double tx, double referenceY, double tz) {
		if (world instanceof Level level) {
			BlockPos refPos = BlockPos.containing(tx, referenceY + 2, tz);
			boolean inTunnel = !level.getBlockState(refPos).isAir() && level.getBlockState(refPos).isSolid();
			int startY = inTunnel ? (int) Math.floor(referenceY) + 2 : (int) Math.floor(referenceY) + 6;
			int minSearchY = inTunnel ? Math.max(level.getMinBuildHeight(), (int) Math.floor(referenceY) - 4) : Math.max(level.getMinBuildHeight(), (int) Math.floor(referenceY) - 24);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int y = startY; y >= minSearchY; y--) {
				pos.set((int) Math.floor(tx), y, (int) Math.floor(tz));
				BlockState state = level.getBlockState(pos);
				if (!state.isAir() && state.isSolid()) {
					BlockState above1 = level.getBlockState(pos.above(1));
					BlockState above2 = level.getBlockState(pos.above(2));
					if (!above1.isSolid() && !above2.isSolid()) {
						return y + 1.0;
					}
				}
			}
		}
		return referenceY;
	}

	private static boolean trySafeTeleportToGround(LevelAccessor world, Entity self, double targetX, double targetY, double targetZ, String soundId, float vol, float pitch, String cdKey, int cdTicks) {
		double groundY = findTargetGroundY(world, targetX, targetY, targetZ);
		if (Math.abs(groundY - targetY) > 3.0) return false;
		if (!isSafeTeleportSpot(world, targetX, groundY, targetZ, self.getY())) return false;
		if (world instanceof ServerLevel level) {
			playTeleportEffects(level, self, self.getX(), self.getY(), self.getZ());
			teleportEntity(self, targetX, groundY, targetZ);
			playTeleportEffects(level, self, targetX, groundY, targetZ);
		} else {
			teleportEntity(self, targetX, groundY, targetZ);
			playHostileSound(world, targetX, groundY, targetZ, soundId, vol, pitch);
		}
		putD(self, cdKey, cdTicks);
		putD(self, "ai_tp_combo_penalty_ticks", 80);
		return true;
	}

	private static boolean isSafeTeleportSpot(LevelAccessor world, double x, double y, double z, double fromY) {
		BlockPos feet = BlockPos.containing(x, y, z);
		BlockPos head = feet.above();
		BlockPos below = feet.below();

		BlockState feetState = world.getBlockState(feet);
		BlockState headState = world.getBlockState(head);
		BlockState belowState = world.getBlockState(below);

		if (!belowState.blocksMotion()) return false;
		if (!feetState.isAir() && !feetState.canBeReplaced()) return false;
		if (!headState.isAir() && !headState.canBeReplaced()) return false;
		return true;
	}

	private static void handleForwardCarveMining(LevelAccessor world, Entity self, Entity target) {
		if (target != null && isTargetPillaring(world, target, self)) {
			if (getB(self, "unlocked_teleportation") && getD(self, K_TP_FLANK_CD) <= 0) {
				tryFlankTeleport(world, self, target, self.distanceTo(target));
			}
			double tdx = target.getX() - self.getX();
			double tdz = target.getZ() - self.getZ();
			double distSqXZ = tdx * tdx + tdz * tdz;
			double maxPillarDist = ROT_PILLAR_BACK_OFF_DISTANCE + 2.0;
			if (distSqXZ < maxPillarDist * maxPillarDist) {
				double circleTicks = 0;
				if (self.getPersistentData() != null) {
					circleTicks = getD(self, "rot_pillar_circle_ticks") + 1.0;
					putD(self, "rot_pillar_circle_ticks", circleTicks);
				}

				double stateTimer = 0.0;
				double isCircling = 1.0;
				double angle = self.tickCount * 0.05;

				if (self.getPersistentData() != null) {
					if (!self.getPersistentData().contains("rot_pillar_is_circling")) {
						putD(self, "rot_pillar_is_circling", 1.0);
					}
					if (!self.getPersistentData().contains("rot_pillar_angle")) {
						putD(self, "rot_pillar_angle", angle);
					}
					stateTimer = getD(self, "rot_pillar_state_timer");
					isCircling = getD(self, "rot_pillar_is_circling");
					angle = getD(self, "rot_pillar_angle");
				}

				if (stateTimer <= 0) {
					double randState = self.getRandom().nextDouble();
					if (randState < ROT_PILLAR_CIRCLING_CHANCE) {
						isCircling = 1.0;
						stateTimer = 60.0 + self.getRandom().nextInt(61);
					} else {
						isCircling = 0.0;
						stateTimer = 160.0 + self.getRandom().nextInt(201);
					}
					if (self.getPersistentData() != null) {
						putD(self, "rot_pillar_is_circling", isCircling);
						putD(self, "rot_pillar_state_timer", stateTimer);
					}
				} else {
					stateTimer--;
					if (self.getPersistentData() != null) {
						putD(self, "rot_pillar_state_timer", stateTimer);
					}
				}

				if (isCircling == 1.0) {
					angle += 0.05;
					if (self.getPersistentData() != null) {
						putD(self, "rot_pillar_angle", angle);
					}
				}

				double circleRadius = ROT_PILLAR_BACK_OFF_DISTANCE;
				double targetCircleX = target.getX() + Math.cos(angle) * circleRadius;
				double targetCircleZ = target.getZ() + Math.sin(angle) * circleRadius;

				if (self instanceof Mob mob) {
					lockLookAtTarget(mob, target);
					if (isCircling == 1.0) {
						mob.getNavigation().moveTo(targetCircleX, self.getY(), targetCircleZ, ROT_PILLAR_CIRCLING_SPEED);
					} else {
						mob.getNavigation().stop();
					}
				}

				double adaptivePillarDelay = ROT_PILLAR_INITIAL_ATTACK_DELAY;
				if (target instanceof LivingEntity livTarget) {
					if (livTarget.isUsingItem()) adaptivePillarDelay *= 0.1;
					if (livTarget.getHealth() < livTarget.getMaxHealth() * 0.4) adaptivePillarDelay *= 0.25;
				}
				double adaptMult = getAdaptationMultiplier(self);
				if (adaptMult > 1.0) adaptivePillarDelay = Math.max(40.0, adaptivePillarDelay / adaptMult);

				if (circleTicks >= adaptivePillarDelay) {
					double rand = self.getRandom().nextDouble();
					if (self.getPersistentData() != null && rand < ROT_PILLAR_ATTACK_CHANCE) {
						if (rand < (ROT_PILLAR_ATTACK_CHANCE / 2.0) && getB(self, "unlocked_overhead_combo")) {
							putD(self, "rot_overhead_ticks", OVERHEAD_TOTAL_TICKS);
							putS(self, "overhead_target_uuid", target.getUUID().toString());
							putD(self, "rot_pillar_circle_ticks", 0.0);
						} else {
							setMotion(self, 0.0, 1.95, 0.0);
							putD(self, "sentinel_die_kick_phase", 1);
							putD(self, "sentinel_die_kick_ticks", 22);
							clearDoubles(self, "sentinel_landing_ticks", "rot_pillar_circle_ticks");
							if (world instanceof ServerLevel level) {
								playHostileSound(level, self, "entity.iron_golem.attack", 1.5F, 0.8F);
								level.sendParticles(ParticleTypes.CLOUD, self.getX(), self.getY() + 0.5, self.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
							}
						}
					}
				}
				return;
			}
		}

		if (self.getPersistentData() != null && self.getPersistentData().contains("rot_pillar_circle_ticks")) {
			putD(self, "rot_pillar_circle_ticks", 0.0);
		}

		LivingEntity foundPlayer = target instanceof LivingEntity liv ? liv : null;
		double heightDiff = target != null ? target.getY() - self.getY() : 0;
		double horizontalDist = target != null ? Math.sqrt(self.distanceToSqr(target.getX(), self.getY(), target.getZ())) : 999;

		Vec3 selfEyes = self.getEyePosition(1f);
		Vec3 selfView = self.getViewVector(1f);
		Vec3 blockCheckTarget = selfEyes.add(selfView.scale(MINE_RAY_DISTANCE));

		HitResult hit = world.clip(new ClipContext(selfEyes, blockCheckTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));

		BlockPos facePos;
		BlockPos feetPos;
		BlockPos headPos;

		if (hit.getType() == HitResult.Type.BLOCK) {
			facePos = ((BlockHitResult) hit).getBlockPos();
			feetPos = new BlockPos(facePos.getX(), Mth.floor(self.getY()), facePos.getZ());
			headPos = new BlockPos(facePos.getX(), Mth.floor(self.getY() + 2), facePos.getZ());
		} else {
			Vec3 look = self.getLookAngle().normalize();
			int fx = Mth.floor(self.getX() + look.x);
			int fz = Mth.floor(self.getZ() + look.z);
			feetPos = new BlockPos(fx, Mth.floor(self.getY()), fz);
			facePos = new BlockPos(fx, Mth.floor(self.getEyeY()), fz);
			headPos = new BlockPos(fx, Mth.floor(self.getY() + 2), fz);
		}

		boolean canMineFeet = canMine(world, feetPos, foundPlayer);
		boolean canMineFace = canMine(world, facePos, foundPlayer);
		boolean canMineHead = canMine(world, headPos, foundPlayer);

		BlockPos downPos = self.blockPosition().below();
		boolean canMineDown = (heightDiff <= -2.0 && horizontalDist <= 3.0) && canMine(world, downPos, foundPlayer);

		int prevProgress = self instanceof RotEntity rot ? rot.getEntityData().get(RotEntity.DATA_mineProgress) : 0;
		int prevX = self.getPersistentData() != null ? getI(self, "rot_mine_x") : 0;
		int prevY = self.getPersistentData() != null ? getI(self, "rot_mine_y") : 0;
		int prevZ = self.getPersistentData() != null ? getI(self, "rot_mine_z") : 0;
		BlockPos prevTrackPos = new BlockPos(prevX, prevY, prevZ);

		BlockPos trackPos = canMineDown ? downPos : (canMineFeet ? feetPos : (canMineFace ? facePos : headPos));
		if (prevProgress > 0 && trackPos != null && !trackPos.equals(prevTrackPos)) {
			boolean lockPrev = canMine(world, prevTrackPos, foundPlayer) && prevTrackPos.closerToCenterThan(self.position(), 4.0) && !isPositionClaimed(world, prevTrackPos, self);
			if (lockPrev) {
				trackPos = prevTrackPos;
				canMineDown = trackPos.equals(downPos);
				canMineFeet = trackPos.equals(feetPos);
				canMineFace = trackPos.equals(facePos);
				canMineHead = trackPos.equals(headPos);
				if (!canMineDown && !canMineFeet && !canMineFace && !canMineHead) {
					if (trackPos.getY() < self.getY()) { canMineDown = true; downPos = trackPos; }
					else if (trackPos.getY() == Mth.floor(self.getY())) { canMineFeet = true; feetPos = trackPos; }
					else if (trackPos.getY() == Mth.floor(self.getY() + 1)) { canMineFace = true; facePos = trackPos; }
					else { canMineHead = true; headPos = trackPos; }
				}
			} else {
				prevProgress = 0;
			}
		}

		if (canMineDown && isPositionClaimed(world, downPos, self)) canMineDown = false;
		if (canMineFeet && isPositionClaimed(world, feetPos, self)) canMineFeet = false;
		if (canMineFace && isPositionClaimed(world, facePos, self)) canMineFace = false;
		if (canMineHead && isPositionClaimed(world, headPos, self)) canMineHead = false;

		double curX = self.getX(), curY = self.getY(), curZ = self.getZ();
		double lastX = getRotPersistentDouble(self, "rot_last_x", curX);
		double lastZ = getRotPersistentDouble(self, "rot_last_z", curZ);
		double horizDistMoved = Math.sqrt((curX - lastX) * (curX - lastX) + (curZ - lastZ) * (curZ - lastZ));

		putD(self, "rot_last_x", curX);
		putD(self, "rot_last_z", curZ);

		int curMineProg = self instanceof RotEntity rot ? rot.getEntityData().get(RotEntity.DATA_mineProgress) : 0;
		int lastMineProg = getRotPersistentInt(self, "rot_last_mine_prog", curMineProg);
		int mineProgressDelta = curMineProg - lastMineProg;
		putI(self, "rot_last_mine_prog", curMineProg);

		int ticksNoMove = getI(self, "rot_ticks_no_movement");
		int ticksNoMine = getI(self, "rot_ticks_no_mining");

		if (horizDistMoved > 0.05) ticksNoMove = 0; else ticksNoMove++;
		if (mineProgressDelta > 0) ticksNoMine = 0; else ticksNoMine++;

		putI(self, "rot_ticks_no_movement", ticksNoMove);
		putI(self, "rot_ticks_no_mining", ticksNoMine);

		boolean isActivelyMining = mineProgressDelta > 0 || (curMineProg > 0 && ticksNoMine < 10);
		boolean isStuckNow = (ticksNoMove > 20) && !isActivelyMining;

		int stuckTier = getI(self, "rot_stuck_tier");
		int stuckTierTicks = getI(self, "rot_stuck_tier_ticks");

		if (horizDistMoved > 0.1 || mineProgressDelta > 0) {
			if (stuckTier > 0) {
				String env = CombatProfile.analyzeEnvironment(world, self, foundPlayer);
				String winKey = "rot_stuck_wins_" + env + "_" + stuckTier;
				putI(self, winKey, getI(self, winKey) + 1);
			}
			stuckTier = 0;
			stuckTierTicks = 0;
			putI(self, "rot_stuck_tier", 0);
			putI(self, "rot_stuck_tier_ticks", 0);
		} else if (isStuckNow) {
			stuckTierTicks++;
			putI(self, "rot_stuck_tier_ticks", stuckTierTicks);

			String cause = "PATH_BLOCKED";
			if (target != null && Math.abs(heightDiff) >= 2.0) {
				cause = "VERTICAL_GAP";
			} else if (canMineFace && world.getBlockState(facePos).getDestroySpeed(world, facePos) >= MAX_BREAKABLE_HARDNESS) {
				cause = "UNBREAKABLE_BLOCK";
			} else if (isPositionClaimed(world, facePos, self) || isPositionClaimed(world, feetPos, self)) {
				cause = "CLAIM_CONTESTED";
			}
			putS(self, "rot_stuck_cause", cause);

			if (stuckTierTicks > 30) {
				stuckTier = Math.min(3, stuckTier + 1);
				stuckTierTicks = 0;
				putI(self, "rot_stuck_tier", stuckTier);
				putI(self, "rot_stuck_tier_ticks", 0);
			}

			if (stuckTier == 0) {
				BlockPos widenUp = facePos.above();
				BlockPos widenL = facePos.west();
				BlockPos widenR = facePos.east();
				if (canMine(world, widenUp, foundPlayer)) world.destroyBlock(widenUp, false);
				if (canMine(world, widenL, foundPlayer)) world.destroyBlock(widenL, false);
				if (canMine(world, widenR, foundPlayer)) world.destroyBlock(widenR, false);
			} else if (stuckTier == 1) {
				if (self instanceof Mob mob && target != null) {
					mob.getNavigation().moveTo(target, 1.25);
				}
			} else if (stuckTier == 2) {
				boolean tpUnlocked = getB(self, "unlocked_teleportation");
				if (tpUnlocked && target != null && self instanceof LivingEntity) {
					double dx = target.getX() - self.getX();
					double dz = target.getZ() - self.getZ();
					double dist = Math.sqrt(dx * dx + dz * dz);
					if (dist > 1.0) {
						double stepX = self.getX() + (dx / dist) * Math.min(dist, 4.0);
						double stepZ = self.getZ() + (dz / dist) * Math.min(dist, 4.0);
						int groundY = Mth.floor(target.getY());
						BlockPos tpPos = new BlockPos(Mth.floor(stepX), groundY, Mth.floor(stepZ));
						if (world.getBlockState(tpPos).isAir() && world.getBlockState(tpPos.above()).isAir()) {
							teleportEntity(self, stepX + 0.5, groundY, stepZ + 0.5);
							putI(self, "rot_ticks_no_movement", 0);
							putI(self, "rot_ticks_no_mining", 0);
						}
					}
				} else if (!tpUnlocked && target != null) {
					putI(self, "rot_deprioritize_target_id", target.getId());
					putI(self, "rot_deprioritize_ticks", 200);
					if (self instanceof Mob mob) mob.setTarget(null);
					putI(self, "rot_stuck_tier", 0);
					putI(self, "rot_stuck_tier_ticks", 0);
				}
			} else if (stuckTier == 3) {
				if (target != null) {
					putI(self, "rot_deprioritize_target_id", target.getId());
					putI(self, "rot_deprioritize_ticks", 200);
					if (self instanceof Mob mob) mob.setTarget(null);
				}
				putI(self, "rot_stuck_tier", 0);
				putI(self, "rot_stuck_tier_ticks", 0);
			}
		}

		if (canMineFeet || canMineFace || canMineHead || canMineDown) {
			int mineProgress = prevProgress + 1;
			if (self instanceof RotEntity rotSet) {
				rotSet.getEntityData().set(RotEntity.DATA_mineProgress, mineProgress);
			}
			if (self.getPersistentData() != null) {
				putI(self, "rot_mine_x", trackPos.getX());
				putI(self, "rot_mine_y", trackPos.getY());
				putI(self, "rot_mine_z", trackPos.getZ());
			}

			if (self.tickCount % 10 == 0 && self instanceof LivingEntity liv) {
				liv.swing(InteractionHand.MAIN_HAND);
				spawnParticles(world, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, world.getBlockState(trackPos)), trackPos.getX() + 0.5, trackPos.getY() + 0.5, trackPos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.1);
				boolean lastHandLeft = false;
				if (self.getPersistentData() != null) {
					lastHandLeft = getB(self, "sentinel_punch_hand_toggle");
					putB(self, "sentinel_punch_hand_toggle", !lastHandLeft);
					if (lastHandLeft) {
						putD(self, "sentinel_left_punch_ticks", 8);
						putD(self, "sentinel_right_punch_ticks", 0);
					} else {
						putD(self, "sentinel_right_punch_ticks", 8);
						putD(self, "sentinel_left_punch_ticks", 0);
					}
				}
			}

			float speedRef;
			if (canMineDown) {
				speedRef = world.getBlockState(downPos).getDestroySpeed(world, downPos);
			} else if (canMineFeet) {
				speedRef = world.getBlockState(feetPos).getDestroySpeed(world, feetPos);
			} else if (canMineFace) {
				speedRef = world.getBlockState(facePos).getDestroySpeed(world, facePos);
			} else {
				speedRef = world.getBlockState(headPos).getDestroySpeed(world, headPos);
			}

			float mineThreshold = speedRef * MINE_SPEED_MULTIPLIER + MINE_SPEED_BASE;

			if (mineProgress > mineThreshold) {
				if (canMineDown) world.destroyBlock(downPos, false);
				if (canMineFeet) world.destroyBlock(feetPos, false);
				if (canMineFace) world.destroyBlock(facePos, false);
				if (canMineHead) world.destroyBlock(headPos, false);
				if (self instanceof Mob mob) mob.getNavigation().stop();
				if (self instanceof RotEntity rotSet) {
					rotSet.getEntityData().set(RotEntity.DATA_mineProgress, 0);
				}
			}
		} else {
			if (self instanceof RotEntity rotSet) {
				rotSet.getEntityData().set(RotEntity.DATA_mineProgress, 0);
			}
		}
	}

	private static boolean isPositionClaimed(LevelAccessor world, BlockPos pos, Entity thisEntity) {
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pos).inflate(3);
		java.util.List<RotEntity> others = world.getEntitiesOfClass(RotEntity.class, box, e -> e != thisEntity && e.isAlive());
		int ourProgress = thisEntity instanceof RotEntity rot ? rot.getEntityData().get(RotEntity.DATA_mineProgress) : 0;
		for (RotEntity other : others) {
			int otherProgress = other.getEntityData().get(RotEntity.DATA_mineProgress);
			int otherX = other.getPersistentData() != null ? getI(other, "rot_mine_x") : 0;
			int otherY = other.getPersistentData() != null ? getI(other, "rot_mine_y") : 0;
			int otherZ = other.getPersistentData() != null ? getI(other, "rot_mine_z") : 0;
			if (otherProgress > 0 && otherX == pos.getX() && otherY == pos.getY() && otherZ == pos.getZ()) {
				if (ourProgress == 0) return true;
				if (otherProgress > ourProgress) return true;
				if (otherProgress == ourProgress && other.getId() < thisEntity.getId()) return true;
			}
		}
		return false;
	}

	private static boolean canMine(LevelAccessor world, BlockPos pos, LivingEntity player) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir()) return false;
		if (state.getCollisionShape(world, pos).isEmpty()) return false;
		float speed = state.getDestroySpeed(world, pos);
		if (speed < 0 || speed >= MAX_BREAKABLE_HARDNESS) return false;
		if (player != null && pos.getY() == (int) (player.getY() - 2)) return false;
		return true;
	}

	private static boolean isDoingCombo(Entity entity) {
		return getD(entity, "sentinel_combo_active_ticks") > 0
			|| getD(entity, "sentinel_sky_warp_slam_ticks") > 0
			|| getD(entity, "sentinel_judgment_ticks") > 0
			|| getD(entity, "sentinel_rider_hold_ticks") > 0
			|| getD(entity, "sentinel_minos_ticks") > 0
			|| getD(entity, "sentinel_minos_stage") > 0
			|| getD(entity, "sentinel_cc1_stage") > 0
			|| getD(entity, "sentinel_cc2_stage") > 0
			|| getD(entity, "sentinel_cc3_stage") > 0
			|| getD(entity, "sentinel_cc4_stage") > 0
			|| getD(entity, "sentinel_cc5_stage") > 0;
	}

	private static double determineSmartPathSpeed(Entity entity, Mob mob, Entity target, double distToTarget) {
		if (getD(entity, "sentinel_solar_charge_ticks") > 0 || getD(entity, "sentinel_solar_fire_ticks") > 0
			|| getD(entity, "sentinel_cryo_charge_ticks") > 0 || getD(entity, "sentinel_cryo_fire_ticks") > 0) {
			return 0.1;
		}

		if (getD(entity, "sentinel_sonic_scream_ticks") > 0) {
			return ROT_RUN_SPEED * 0.25;
		}

		boolean isHeavyPunching = false;
		try {
			isHeavyPunching = entity.getEntityData().get(RotEntity.DATA_is_heavy_left_punching) || entity.getEntityData().get(RotEntity.DATA_is_heavy_right_punching);
		} catch (Exception ignored) {}
		if (isHeavyPunching) return 0.1;

		boolean targetFleeing = false;
		if (target != null) {
			Vec3 tVel = target.getDeltaMovement();
			targetFleeing = (tVel.x * tVel.x + tVel.z * tVel.z) > 0.015;
		}

		if (distToTarget <= 4.5) {
			return targetFleeing ? ROT_RUN_SPEED : ROT_WALK_SPEED;
		}

		if (distToTarget > 8.0) {
			return ROT_RUN_SPEED;
		}

		double rotHpRatio = 1.0;
		if (entity instanceof LivingEntity liv) {
			rotHpRatio = liv.getHealth() / Math.max(1.0, liv.getMaxHealth());
		}

		boolean targetBlocking = target instanceof LivingEntity ltTarget && ltTarget.isBlocking();
		if (targetBlocking) {
			return ROT_WALK_SPEED;
		}

		if (targetFleeing || rotHpRatio < 0.4) {
			return ROT_RUN_SPEED;
		}

		return ROT_RUN_SPEED;
	}

	private static void checkTrenchAndJump(LevelAccessor world, Entity self, Entity target) {
		if (!(self instanceof LivingEntity living) || !living.onGround() || target == null) {
			return;
		}

		boolean isFiringLaser = getD(self, "sentinel_solar_charge_ticks") > 0 
			|| getD(self, "sentinel_solar_fire_ticks") > 0 
			|| getD(self, "sentinel_cryo_charge_ticks") > 0 
			|| getD(self, "sentinel_cryo_fire_ticks") > 0 
			|| getD(self, "sentinel_laser_closing_ticks") > 0;
		if (isFiringLaser) return;

		boolean isMining = getD(self, "sentinel_mining_ticks") > 0 || getI(self, "rot_ticks_no_mining") == 0;
		if (isMining) return;

		double jumpCd = getD(self, "sentinel_jump_cd");
		if (jumpCd > 0) {
			putD(self, "sentinel_jump_cd", jumpCd - 1.0);
			return;
		}

		double dx = target.getX() - self.getX();
		double dz = target.getZ() - self.getZ();
		double distXZ = Math.sqrt(dx * dx + dz * dz);
		if (distXZ < 0.1) {
			return;
		}
		double dirX = dx / distXZ;
		double dirZ = dz / distXZ;

		net.minecraft.core.BlockPos headAbovePos = self.blockPosition().above(2);
		if (!world.getBlockState(headAbovePos).isAir()) {
			return;
		}

		boolean hasTrench = false;
		double[] checkDistances = {1.2, 2.2};
		for (double d : checkDistances) {
			double cx = self.getX() + dirX * d;
			double cz = self.getZ() + dirZ * d;
			net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(cx, self.getY(), cz);

			if (world.getBlockState(pos).isAir() && world.getBlockState(pos.below()).isAir()) {
				int depth = 0;
				net.minecraft.core.BlockPos tracePos = pos.below();
				while (depth < 5 && world.getBlockState(tracePos).isAir()) {
					tracePos = tracePos.below();
					depth++;
				}
				if (depth >= 2) {
					hasTrench = true;
					break;
				}
			}
		}

		if (hasTrench) {
			putD(self, "sentinel_jump_cd", 20.0);
			Vec3 motion = self.getDeltaMovement();
			double jumpY = 0.52;
			double jumpForward = 0.35;
			self.setDeltaMovement(motion.x + dirX * jumpForward, jumpY, motion.z + dirZ * jumpForward);
			living.setJumping(true);
			self.hasImpulse = true;

			if (world instanceof ServerLevel level) {
				net.minecraft.core.BlockPos belowPos = net.minecraft.core.BlockPos.containing(self.getX(), self.getY() - 0.5, self.getZ());
				net.minecraft.world.level.block.state.BlockState floorState = level.getBlockState(belowPos);
				if (!floorState.isAir()) {
					net.minecraft.core.particles.BlockParticleOption dust = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, floorState);
					level.sendParticles(dust, self.getX(), self.getY() + 0.1, self.getZ(), 8, 0.25, 0.1, 0.25, 0.05);
				}
			}
		}
	}

	private static void handlePassengerAndGrowth(Entity entity) {
		if (entity.isPassenger()) entity.stopRiding();

		putD(entity, K_AGE, getD(entity, K_AGE) + 1);
		if (getD(entity, K_AGE) % 1200 == 0) {
			if (entity instanceof LivingEntity living && living.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
				living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(living.getAttribute(Attributes.MAX_HEALTH).getBaseValue() + 2);
				living.setHealth(living.getHealth() + 2);
			}
		}
	}

	private static final String[] ROT_COOLDOWN_KEYS = {
		K_TP_DODGE_CD, K_TP_FLANK_CD, K_SOLAR_CD, K_ADAPT_CD, K_GRAPPLE_CD, K_TK_CD,
		"rot_wither_dialogue_cooldown", "sentinel_wither_skull_cd", "sentinel_flight_intercept_cooldown",
		"sentinel_warden_sonic_cooldown", "sentinel_global_ability_cooldown", "sentinel_sonic_ticks",
		"sentinel_uppercut_cd", "sentinel_uppercut_anim_ticks", "sentinel_laser_closing_ticks",
		"sentinel_landing_ticks", "sentinel_rider_hold_ticks", "sentinel_melee_cooldown",
		"sentinel_melee_windup", "sentinel_left_punch_ticks", "sentinel_right_punch_ticks",
		"sentinel_heavy_left_punch_ticks", "sentinel_heavy_right_punch_ticks", "rot_overhead_ticks",
		"sentinel_sky_warp_slam_ticks", "sentinel_judgment_ticks", "sentinel_omni_sonic_cooldown",
		"sentinel_omni_sonic_charge_ticks", "sentinel_combo_active_ticks", "sentinel_minos_ticks",
		"sentinel_minos_wait_ticks", "sentinel_cc1_ticks", "sentinel_cc1_cd", "sentinel_cc2_ticks",
		"sentinel_cc2_cd", "sentinel_cc3_ticks", "sentinel_cc3_cd", "sentinel_cc4_ticks",
		"sentinel_cc4_cd", "sentinel_cc5_ticks", "sentinel_cc5_cd", "sentinel_sonic_scream_ticks",
		"sentinel_sonic_scream_cooldown", "rot_armor_rip_cooldown", "rot_block_cooldown",
		"sentinel_eat_punish_cooldown", "sentinel_dive_counter_cd", "rot_phase_cooldown",
		"rot_phase_ticks", "rot_superheat_cd", "rot_superheat_charging", "rot_superheat_active"
	};

	private static void tickCooldowns(Entity e) {
		boolean totemAccelerated = getB(e, "sentinel_totem_active") && !getB(e, "sentinel_is_infinity_totem");
		for (String key : ROT_COOLDOWN_KEYS) {
			if (e.getPersistentData().contains(key)) tickCooldown(e, key, 1, totemAccelerated);
		}
	}

	private static void tickCooldown(Entity e, String key, int step) {
		boolean totemAccelerated = getB(e, "sentinel_totem_active") && !getB(e, "sentinel_is_infinity_totem");
		tickCooldown(e, key, step, totemAccelerated);
	}

	private static void tickCooldown(Entity e, String key, int step, boolean totemAccelerated) {
		double v = getD(e, key);
		if (v > 0.0) {
			double finalStep = step;
			if (totemAccelerated && (key.endsWith("cd") || key.contains("_cd") || key.contains("cooldown") || key.equals("sentinel_solar_cd"))) {
				finalStep = step * 2.0;
			}
			if (COOLDOWN_MULTIPLIER > 0 && (key.endsWith("cd") || key.contains("_cd") || key.contains("cooldown") || key.equals("sentinel_solar_cd"))) {
				finalStep = finalStep / COOLDOWN_MULTIPLIER;
			}
			double next = Math.max(0.0, v - finalStep);
			if (next != v) {
				if (next > 0.0) putD(e, key, next);
				else e.getPersistentData().remove(key);
			}
		}
	}

	private static void playHostileSound(LevelAccessor world, double x, double y, double z, String soundId, float volume, float pitch) {
		if (!(world instanceof Level level)) return;
		try {
			ResourceLocation rl = ResourceLocation.tryParse(soundId);
			if (rl == null) rl = ResourceLocation.parse(soundId.toLowerCase(java.util.Locale.ROOT));
			net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(rl);
			if (sound != null) {
				if (!level.isClientSide()) {
					level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.HOSTILE, volume, pitch);
				} else {
					level.playLocalSound(x, y, z, sound, SoundSource.HOSTILE, volume, pitch, false);
				}
			}
		} catch (Exception ignored) {}
	}

	private static void stopHostileSound(LevelAccessor world, double x, double y, double z, String soundId, double range) {
		if (world instanceof ServerLevel level) {
			try {
				ResourceLocation rl = ResourceLocation.tryParse(soundId);
				if (rl == null) rl = ResourceLocation.parse(soundId.toLowerCase(java.util.Locale.ROOT));
				net.minecraft.network.protocol.game.ClientboundStopSoundPacket packet = new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(rl, SoundSource.HOSTILE);
				for (ServerPlayer player : level.getPlayers(p -> p.position().distanceToSqr(x, y, z) <= range * range)) {
					player.connection.send(packet);
				}
			} catch (Exception ignored) {}
		}
	}

	private static void playHostileSound(LevelAccessor world, Entity e, String soundId, float volume, float pitch) {
		if (e != null) playHostileSound(world, e.getX(), e.getY(), e.getZ(), soundId, volume, pitch);
	}

	private static void playHostileSound(LevelAccessor world, BlockPos pos, String soundId, float volume, float pitch) {
		if (pos != null) playHostileSound(world, pos.getX(), pos.getY(), pos.getZ(), soundId, volume, pitch);
	}

	private static void stopHostileSound(LevelAccessor world, Entity e, String soundId, double range) {
		if (e != null) stopHostileSound(world, e.getX(), e.getY(), e.getZ(), soundId, range);
	}

	private static void applyKnockbackAndSync(Entity victim, double vx, double vy, double vz) {
		if (!(victim instanceof Player)) {
			vx *= 1.6;
			vz *= 1.6;
			vy *= 1.2;
		}
		setMotion(victim, vx, vy, vz);
		victim.hurtMarked = true;
		if (victim instanceof ServerPlayer sp) {
			sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
		}
	}

	private static void teleportEntity(Entity ent, double x, double y, double z) {
		double startX = ent.getX();
		double startY = ent.getY();
		double startZ = ent.getZ();

		if (ent.level() instanceof ServerLevel level) {
			if (ent.distanceToSqr(x, y, z) >= 400.0) {
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, startX, startY + ent.getBbHeight() / 2.0, startZ, 1, 0, 0, 0, 0);
			}
		}

		ent.teleportTo(x, y, z);
		if (ent instanceof Mob mob) {
			mob.getNavigation().recomputePath();
		}
		if (ent instanceof ServerPlayer sp) {
			sp.connection.teleport(x, y, z, ent.getYRot(), ent.getXRot());
		}
		if (ent.level() instanceof ServerLevel level) {
			spawnTeleportTrail(level, startX, startY, startZ, x, y, z);
		}
	}

	private static void spawnTeleportTrail(ServerLevel level, double startX, double startY, double startZ, double targetX, double targetY, double targetZ) {
		Vec3 start = new Vec3(startX, startY, startZ);
		Vec3 end = new Vec3(targetX, targetY, targetZ);
		double distance = start.distanceTo(end);
		if (distance < 1.0) return;

		int steps = (int) Math.ceil(distance * 1.5);
		for (int i = 0; i <= steps; i++) {
			double pct = (double) i / steps;
			double px = startX + (targetX - startX) * pct;
			double pz = startZ + (targetZ - startZ) * pct;
			double py = startY + (targetY - startY) * pct;

			BlockPos checkPos = BlockPos.containing(px, py + 1.0, pz);
			BlockPos groundPos = checkPos;
			boolean foundGround = false;
			for (int dy = 2; dy >= -5; dy--) {
				BlockPos bp = checkPos.above(dy);
				if (!level.getBlockState(bp).isAir() && level.getBlockState(bp).getFluidState().isEmpty() && level.getBlockState(bp).blocksMotion()) {
					groundPos = bp;
					foundGround = true;
					break;
				}
			}

			double spawnY = foundGround ? (groundPos.getY() + 1.0) : py;
			BlockState state = foundGround ? level.getBlockState(groundPos) : net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();

			if (!state.isAir()) {
				try {
					level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state), px, spawnY + 0.1, pz, 4, 0.1, 0.1, 0.1, 0.15);
				} catch (Exception ignored) {}
			}
		}
	}

	private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range, Entity self) {
		AABB searchBox = AABB.ofSize(new Vec3(x, y, z), range, range, range);
		java.util.Set<Integer> occupiedTargetIds = new java.util.HashSet<>();
		try {
			for (RotEntity other : world.getEntitiesOfClass(RotEntity.class, searchBox, e -> e != self)) {
				if (other instanceof Mob otherMob && otherMob.getTarget() != null) {
					occupiedTargetIds.add(otherMob.getTarget().getId());
				}
			}
		} catch (Exception e) {}

		return world.getEntitiesOfClass(clazz, searchBox, e -> isValidTarget(e, self))
				.stream()
				.sorted((e1, e2) -> {
					if (e1 instanceof LivingEntity l1 && e2 instanceof LivingEntity l2) {
						if (l1.getVehicle() != null && isContraptionEntity(l1.getVehicle())) {
							return -1;
						}
						if (l2.getVehicle() != null && isContraptionEntity(l2.getVehicle())) {
							return 1;
						}
						boolean occ1 = occupiedTargetIds.contains(l1.getId());
						boolean occ2 = occupiedTargetIds.contains(l2.getId());
						if (occ1 != occ2) {
							return occ1 ? 1 : -1;
						}
						boolean tpUnlocked = getB(self, "unlocked_teleportation");
						if (!tpUnlocked && self instanceof LivingEntity ls) {
							boolean los1 = ls.hasLineOfSight(l1);
							boolean los2 = ls.hasLineOfSight(l2);
							if (los1 != los2) {
								return los1 ? -1 : 1;
							}
						}
						float hp1 = l1.getHealth();
						float hp2 = l2.getHealth();
						if (Math.abs(hp1 - hp2) > 0.05f) {
							return Float.compare(hp1, hp2);
						}
					}
					return Double.compare(e1.distanceToSqr(x, y, z), e2.distanceToSqr(x, y, z));
				})
				.findFirst()
				.orElse(null);
	}

	public static boolean isChannelingAbility(Entity entity) {
		if (entity == null) return false;
		double solarCharge = getD(entity, "sentinel_solar_charge_ticks");
		double solarFire = getD(entity, "sentinel_solar_fire_ticks");
		double cryoCharge = getD(entity, "sentinel_cryo_charge_ticks");
		double cryoFire = getD(entity, "sentinel_cryo_fire_ticks");
		double grappleTicks = getD(entity, "sentinel_grapple_ticks");
		double tkTicks = getD(entity, "sentinel_tk_ticks");
		double sonicTicks = getD(entity, "sentinel_sonic_ticks");
		double closingTicks = getD(entity, "sentinel_laser_closing_ticks");
		double skyWarp = getD(entity, "sentinel_sky_warp_slam_ticks");
		double judgment = getD(entity, "sentinel_judgment_ticks");
		double riderHold = getD(entity, "sentinel_rider_hold_ticks");
		double omniSonic = getD(entity, "sentinel_omni_sonic_charge_ticks");
		double sonicScream = getD(entity, "sentinel_sonic_scream_ticks");
		double armorRipTicks = getD(entity, "rot_armor_rip_ticks");
		double blockTicks = getD(entity, "rot_block_active_ticks");
		boolean isUppercutting = getB(entity, "is_uppercutting");
		double superheatCharging = getD(entity, "rot_superheat_charging");
		double superheatActive = getD(entity, "rot_superheat_active");
		return solarCharge > 0 || solarFire > 0 || cryoCharge > 0 || cryoFire > 0 || grappleTicks > 0 || tkTicks > 0 || sonicTicks > 0 || closingTicks > 0 || skyWarp > 0 || judgment > 0 || riderHold > 0 || omniSonic > 0 || sonicScream > 0 || armorRipTicks > 0 || blockTicks > 0 || isUppercutting || superheatCharging > 0 || superheatActive > 0 || isDoingCombo(entity);
	}

	private static void syncNBTFlags(Entity entity) {
		double solarCharge = getD(entity, "sentinel_solar_charge_ticks");
		double cryoCharge = getD(entity, "sentinel_cryo_charge_ticks");
		double solarFire = getD(entity, "sentinel_solar_fire_ticks");
		double cryoFire = getD(entity, "sentinel_cryo_fire_ticks");
		double closingTicks = getD(entity, "sentinel_laser_closing_ticks");
		double sonicTicks = getD(entity, "sentinel_sonic_ticks");
		double sonicCooldown = getD(entity, "sentinel_warden_sonic_cooldown");
		double landingTicks = getD(entity, "sentinel_landing_ticks");

		double leftPunchTicks = getD(entity, "sentinel_left_punch_ticks");
		double rightPunchTicks = getD(entity, "sentinel_right_punch_ticks");
		double heavyLeftPunchTicks = getD(entity, "sentinel_heavy_left_punch_ticks");
		double heavyRightPunchTicks = getD(entity, "sentinel_heavy_right_punch_ticks");
		double overheadTicks = getD(entity, "rot_overhead_ticks");
		if (getB(entity, "debug_force_overhead")) {
			if (overheadTicks <= 1) {
				putD(entity, "rot_overhead_ticks", OVERHEAD_TOTAL_TICKS);
				overheadTicks = OVERHEAD_TOTAL_TICKS;
			}
		}

		double uppercutLeftTicks = getD(entity, "debug_uppercut_left_ticks");
		if (getB(entity, "debug_force_uppercut_left")) {
			if (uppercutLeftTicks <= 1) {
				putD(entity, "debug_uppercut_left_ticks", 40.0);
				uppercutLeftTicks = 40.0;
			} else {
				putD(entity, "debug_uppercut_left_ticks", uppercutLeftTicks - 1);
			}
		}

		double uppercutRightTicks = getD(entity, "debug_uppercut_right_ticks");
		if (getB(entity, "debug_force_uppercut_right")) {
			if (uppercutRightTicks <= 1) {
				putD(entity, "debug_uppercut_right_ticks", 40.0);
				uppercutRightTicks = 40.0;
			} else {
				putD(entity, "debug_uppercut_right_ticks", uppercutRightTicks - 1);
			}
		}
		double slamPhase = getD(entity, "sentinel_slam_phase");
		double skyWarpTicks = getD(entity, "sentinel_sky_warp_slam_ticks");

		boolean leftPunching = leftPunchTicks > 0;
		boolean rightPunching = rightPunchTicks > 0;
		boolean isHeavyLeftPunching = heavyLeftPunchTicks > 0 || getB(entity, "debug_force_heavy_left");
		boolean isHeavyRightPunching = heavyRightPunchTicks > 0 || getB(entity, "debug_force_heavy_right");
		boolean overheadActive = overheadTicks > 0;
		boolean inSlamCharge = slamPhase == 2;

		boolean isAirborne = false;
		boolean isHeavyFalling = false;
		if (!entity.onGround() && !entity.isInWater() && !entity.isInLava()) {
			if (entity.getDeltaMovement().y() > 0.4) {
				isAirborne = true;
			}
			if (entity.getDeltaMovement().y() < -0.15 && entity.fallDistance > 3.0F) {
				isHeavyFalling = true;
			}
		}

		if (slamPhase == 1 || skyWarpTicks > 0) {
			isAirborne = true;
		} else if (slamPhase == 2) {
			isAirborne = false;
		} else if (slamPhase == 3) {
			isAirborne = false;
		}

		double dieKickPhase = getD(entity, "sentinel_die_kick_phase");
		if (dieKickPhase == 1) {
			isAirborne = true;
		} else if (dieKickPhase == 2) {
			isAirborne = true;
		} else if (dieKickPhase == 3) {
			isAirborne = false;
		}

		double judgmentTicks = getD(entity, "sentinel_judgment_ticks");
		double sonicScreamTicks = getD(entity, "sentinel_sonic_scream_ticks");
		double witherSkullFire = getD(entity, "sentinel_wither_skull_fire_ticks");
		boolean charging = solarCharge > 0 || cryoCharge > 0 || (sonicScreamTicks > 200.0) || (witherSkullFire > 9.0);
		boolean firing = solarFire > 0 || cryoFire > 0 || (sonicScreamTicks > 20.0 && sonicScreamTicks <= 200.0) || (witherSkullFire > 0.0 && witherSkullFire <= 9.0);
		boolean closing = (closingTicks > 0 && !firing && !charging) || (sonicScreamTicks > 0.0 && sonicScreamTicks <= 20.0);
		boolean sonic = sonicTicks > 0;
		boolean landing = landingTicks > 0;
		boolean isGroundCrushing = slamPhase == 3 || skyWarpTicks > 0 || (landingTicks > 0 && getB(entity, "sentinel_is_slam_landing"));
		boolean isDropkickCharging = judgmentTicks > 20;

		putB(entity, "laser_charging", charging);
		putB(entity, "is_laser_charging", charging);
		putB(entity, "laser_firing", firing);
		putB(entity, "is_laser_firing", firing);
		putB(entity, "laser_closing", closing);
		putB(entity, "is_laser_closing", closing);
		putB(entity, "sonic_boom_active", sonic);
		putB(entity, "is_airborne", isAirborne);
		putB(entity, "sentinel_is_airborne_state", isAirborne);
		putB(entity, "is_landing", landing);

		putB(entity, "is_left_punching", leftPunching);
		putB(entity, "is_right_punching", rightPunching);
		putB(entity, "is_heavy_left_punching", isHeavyLeftPunching);
		putB(entity, "is_heavy_right_punching", isHeavyRightPunching);
		putB(entity, "is_overhead", overheadTicks > 0);
		putB(entity, "is_slam_charge", inSlamCharge);
		putB(entity, "is_air_time", isAirborne);
		putB(entity, "is_overhead_preparing", overheadTicks >= OVERHEAD_PREP_THRESHOLD);
		putB(entity, "is_ground_crushing", isGroundCrushing);
		putB(entity, "is_dropkick_charging", isDropkickCharging);
		putB(entity, "is_airborne_state", isAirborne);

		if (entity instanceof RotEntity rot) {
			setEntityData(rot, RotEntity.DATA_sentinel_solar_charge_ticks, (int) solarCharge);
			setEntityData(rot, RotEntity.DATA_sentinel_cryo_charge_ticks, (int) cryoCharge);
			setEntityData(rot, RotEntity.DATA_is_laser_firing, firing || charging);
			setEntityData(rot, RotEntity.DATA_is_left_punching, leftPunching);
			setEntityData(rot, RotEntity.DATA_is_right_punching, rightPunching);
			setEntityData(rot, RotEntity.DATA_is_heavy_left_punching, isHeavyLeftPunching);
			setEntityData(rot, RotEntity.DATA_is_heavy_right_punching, isHeavyRightPunching);
			setEntityData(rot, RotEntity.DATA_is_laser_closing, closing);
			setEntityData(rot, RotEntity.DATA_is_airborne_state, isAirborne);
			setEntityData(rot, RotEntity.DATA_is_overhead_preparing, overheadTicks >= OVERHEAD_PREP_THRESHOLD);
			setEntityData(rot, RotEntity.DATA_is_overhead, overheadTicks > 0);
			setEntityData(rot, RotEntity.DATA_is_slam_charge, inSlamCharge);
			setEntityData(rot, RotEntity.DATA_is_ground_crushing, isGroundCrushing);
			setEntityData(rot, RotEntity.DATA_is_falling_heavy, getB(entity, "debug_force_fall") || isHeavyFalling);
			setEntityData(rot, RotEntity.DATA_is_rider_charging, getB(entity, "debug_force_rider") || (judgmentTicks == 20));
			setEntityData(rot, RotEntity.DATA_is_rider_kick, dieKickPhase == 3 || dieKickPhase == 4 || (judgmentTicks > 1 && judgmentTicks < 20) || getD(entity, "sentinel_rider_hold_ticks") > 0);
			setEntityData(rot, RotEntity.DATA_is_sonic_boom, getB(entity, "debug_force_sonic") || sonic);
			setEntityData(rot, RotEntity.DATA_is_sonic_boom_large, getB(entity, "is_sonic_boom_large"));
			setEntityData(rot, RotEntity.DATA_is_armor_ripping, getB(entity, "is_armor_ripping"));
			setEntityData(rot, RotEntity.DATA_is_blocking, getB(entity, "is_blocking"));
			setEntityData(rot, RotEntity.DATA_is_blocking_finish, getB(entity, "is_blocking_finish"));

			double uppercutAnim = getD(entity, "sentinel_uppercut_anim_ticks");
			double cc2Stage = getD(entity, "sentinel_cc2_stage");
			double cc2Ticks = getD(entity, "sentinel_cc2_ticks");
			boolean isUppercuttingFlag = getB(entity, "is_uppercutting");
			boolean uppercutDodged = getB(entity, "sentinel_uppercut_dodged");
			if (uppercutAnim <= 0 && cc2Stage == 0.0 && cc2Ticks <= 0) {
				putB(entity, "is_uppercutting", false);
				putB(entity, "is_uppercutting_left", false);
				putB(entity, "is_uppercutting_right", false);
				putB(entity, "is_uppercut_standalone", false);
			}
			boolean isUppercutting = getB(entity, "is_uppercutting") || getB(entity, "debug_force_uppercut_left") || getB(entity, "debug_force_uppercut_right");
			setEntityData(rot, RotEntity.DATA_is_uppercutting, isUppercutting);
			setEntityData(rot, RotEntity.DATA_is_uppercut_charging_left, (isUppercutting && getB(entity, "is_uppercutting_left")) || (getB(entity, "debug_force_uppercut_left") && getD(entity, "debug_uppercut_left_ticks") > 1));
			setEntityData(rot, RotEntity.DATA_is_uppercut_charging_right, (isUppercutting && getB(entity, "is_uppercutting_right")) || (getB(entity, "debug_force_uppercut_right") && getD(entity, "debug_uppercut_right_ticks") > 1));
			setEntityData(rot, RotEntity.DATA_is_dropkick_charging, getB(entity, "is_dropkick_charging"));
			setEntityData(rot, RotEntity.DATA_isLand, getB(entity, "isLand"));
			setEntityData(rot, RotEntity.DATA_isLand2, getB(entity, "isLand2"));
		}
	}

	private static boolean handleSuperheatEvaporationState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		if (entity == null || !(world instanceof ServerLevel serverLevel)) return false;

		boolean learnedSuperheat = getB(entity, "unlocked_water_evaporation")
				|| getB(entity, "unlocked_solar_beam")
				|| getB(entity, "taken_fire_damage");

		double chargeTicks = getD(entity, "rot_superheat_charging");
		double activeTicks = getD(entity, "rot_superheat_active");
		double cd = getD(entity, "rot_superheat_cd");

		boolean isSubmerged = entity.isInWater() || entity.isUnderWater();
		if (!isSubmerged && entity.level() != null) {
			BlockState eyeBs = entity.level().getBlockState(BlockPos.containing(entity.getEyePosition()));
			BlockState feetBs = entity.level().getBlockState(entity.blockPosition());
			isSubmerged = eyeBs.is(Blocks.WATER) || feetBs.is(Blocks.WATER);
		}

		boolean fightingColdTarget = false;
		if (combatTarget instanceof LivingEntity livTarget && livTarget.isAlive()) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(livTarget.getType()).toString().toLowerCase(java.util.Locale.ROOT);
			fightingColdTarget = targetId.contains("stray") || targetId.contains("snow") || targetId.contains("ice") 
				|| targetId.contains("polar") || targetId.contains("frost") || targetId.contains("freeze") 
				|| livTarget.getTicksFrozen() > 0;
		}

		if (chargeTicks <= 0 && activeTicks <= 0) {
			boolean tacticalTrigger = (isSubmerged || (fightingColdTarget && entity.distanceTo(combatTarget) <= 24.0));
			if (learnedSuperheat && tacticalTrigger && combatTarget != null && combatTarget.isAlive() && cd <= 0 && !isDoingCombo(entity)) {
				chargeTicks = fightingColdTarget ? 35.0 : (40.0 + entity.getRandom().nextDouble() * 50.0);
				putD(entity, "rot_superheat_charging", chargeTicks);
				putD(entity, "rot_superheat_max_charge", chargeTicks);
				putD(entity, "rot_superheat_cd", SUPERHEAT_EVAPORATION_COOLDOWN);
				putD(entity, "sentinel_global_ability_cooldown", 30.0);

				if (entity instanceof Mob mob) {
					mob.getNavigation().stop();
				}
				Vec3 mv = entity.getDeltaMovement();
				entity.setDeltaMovement(mv.x * 0.2, Math.min(0.0, mv.y), mv.z * 0.2);
			}
		}

		if (chargeTicks > 0) {
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			Vec3 mv = entity.getDeltaMovement();
			entity.setDeltaMovement(mv.x * 0.85, Math.min(0.0, mv.y - 0.04), mv.z * 0.85);

			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}

			double maxCharge = getRotPersistentDouble(entity, "rot_superheat_max_charge", 60.0);
			double progress = Math.max(0.0, Math.min(1.0, 1.0 - (chargeTicks / maxCharge)));

			int steamCount = (int) (4 + progress * 14);
			double spread = 0.5 + progress * 1.5;
			serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 0.8, entity.getZ(), steamCount, spread, 0.6, spread, 0.03);
			serverLevel.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, entity.getX(), entity.getY() + 0.2, entity.getZ(), (int)(6 + progress * 20), spread, 0.4, spread, 0.05);
			serverLevel.sendParticles(ParticleTypes.BUBBLE_POP, entity.getX(), entity.getY() + 1.0, entity.getZ(), (int)(4 + progress * 10), spread, 0.5, spread, 0.02);

			if (progress > 0.35) {
				serverLevel.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 0.8, entity.getZ(), (int)(3 + progress * 10), spread * 0.6, 0.5, spread * 0.6, 0.03);
				serverLevel.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + 1.0, entity.getZ(), (int)(1 + progress * 5), spread * 0.5, 0.5, spread * 0.5, 0.0);
			}

			int soundInterval = Math.max(2, (int) (12 * (1.0 - progress)) + 2);
			if (entity.tickCount % soundInterval == 0) {
				float volume = (float) (0.6 + 1.2 * progress);
				float pitch = (float) (0.6 + 0.8 * progress);
				playHostileSound(serverLevel, entity, "block.fire.extinguish", volume, pitch);
				if (progress > 0.5) {
					playHostileSound(serverLevel, entity, "block.lava.extinguish", volume * 0.8F, pitch);
				}
			}

			if (progress > 0.5 && entity.tickCount % 5 == 0) {
				double boilRadius = 4.0 + progress * 8.0;
				AABB boilBox = entity.getBoundingBox().inflate(boilRadius);
				List<LivingEntity> scaldVictims = serverLevel.getEntitiesOfClass(LivingEntity.class, boilBox, e -> e != entity && !isWoodboundEntity(e, entity));
				for (LivingEntity victim : scaldVictims) {
					dealTrueDamageToBosses(victim, getBackwoodsDamage(serverLevel, "rot_solar_beam", entity), 4.0F);
					victim.setRemainingFireTicks(60);
					serverLevel.sendParticles(ParticleTypes.SMOKE, victim.getX(), victim.getY() + 0.8, victim.getZ(), 4, 0.2, 0.4, 0.2, 0.02);
				}
			}

			if (chargeTicks <= 1) {
				putD(entity, "rot_superheat_charging", 0.0);
				putD(entity, "rot_superheat_active", SUPERHEAT_EVAPORATION_WAVE_TICKS);
				putD(entity, "rot_superheat_current_radius", 1.0);

				playHostileSound(serverLevel, entity, "entity.generic.explode", 2.2F, 0.5F);
				playHostileSound(serverLevel, entity, "entity.warden.sonic_boom", 2.0F, 0.4F);
				playHostileSound(serverLevel, entity, "block.lava.extinguish", 2.5F, 0.7F);
				serverLevel.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.2, entity.getZ(), 2, 0, 0, 0, 0);
			}
			return true;
		}

		if (activeTicks > 0) {
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}

			double progress = 1.0 - (activeTicks / SUPERHEAT_EVAPORATION_WAVE_TICKS);
			double prevRadius = getRotPersistentDouble(entity, "rot_superheat_current_radius", 1.0);
			double curRadius = 1.0 + (SUPERHEAT_EVAPORATION_RADIUS - 1.0) * Math.sin(progress * (Math.PI / 2.0));
			putD(entity, "rot_superheat_current_radius", curRadius);

			BlockPos centerPos = entity.blockPosition();
			int minX = Mth.floor(centerPos.getX() - curRadius);
			int maxX = Mth.ceil(centerPos.getX() + curRadius);
			int minY = Math.max(serverLevel.getMinBuildHeight(), Mth.floor(centerPos.getY() - curRadius * 0.6));
			int maxY = Math.min(serverLevel.getMaxBuildHeight(), Mth.ceil(centerPos.getY() + curRadius * 0.9));
			int minZ = Mth.floor(centerPos.getZ() - curRadius);
			int maxZ = Mth.ceil(centerPos.getZ() + curRadius);

			double curRadiusSq = curRadius * curRadius;
			double prevRadiusSq = Math.max(0.0, (prevRadius - 0.75) * (prevRadius - 0.75));

			int transformedCount = 0;
			for (int bx = minX; bx <= maxX; bx++) {
				for (int bz = minZ; bz <= maxZ; bz++) {
					double dx = (bx + 0.5) - entity.getX();
					double dz = (bz + 0.5) - entity.getZ();
					double distHsq = dx * dx + dz * dz;
					if (distHsq > curRadiusSq) continue;

					for (int by = minY; by <= maxY; by++) {
						double dy = (by + 0.5) - (entity.getY() + 0.5);
						double distSq = distHsq + (dy > 0 ? dy * dy * 0.9 : dy * dy * 1.5);

						if (distSq <= curRadiusSq && distSq >= prevRadiusSq) {
							BlockPos bp = new BlockPos(bx, by, bz);
							BlockState bs = serverLevel.getBlockState(bp);

							if (bs.is(Blocks.WATER) || bs.getFluidState().is(FluidTags.WATER)) {
								serverLevel.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
								transformedCount++;
								if (serverLevel.getRandom().nextDouble() < 0.35) {
									serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, bx + 0.5, by + 0.5, bz + 0.5, 2, 0.3, 0.3, 0.3, 0.04);
									serverLevel.sendParticles(ParticleTypes.CLOUD, bx + 0.5, by + 0.5, bz + 0.5, 1, 0.2, 0.2, 0.2, 0.02);
								}
							}
							else if (bs.is(Blocks.POWDER_SNOW) || bs.is(Blocks.SNOW) || bs.is(Blocks.SNOW_BLOCK) 
								|| bs.is(Blocks.ICE) || bs.is(Blocks.PACKED_ICE) || bs.is(Blocks.FROSTED_ICE)) {
								serverLevel.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
								transformedCount++;
								serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, bx + 0.5, by + 0.5, bz + 0.5, 3, 0.3, 0.3, 0.3, 0.05);
								serverLevel.sendParticles(ParticleTypes.FLAME, bx + 0.5, by + 0.5, bz + 0.5, 1, 0.1, 0.1, 0.1, 0.02);
							}
							else if (bs.is(Blocks.BLUE_ICE)) {
								serverLevel.setBlock(bp, Blocks.WATER.defaultBlockState(), 3);
								transformedCount++;
								serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, bx + 0.5, by + 0.5, bz + 0.5, 2, 0.3, 0.3, 0.3, 0.03);
							}
						}
					}
				}
			}

			if (transformedCount > 0 || entity.tickCount % 4 == 0) {
				float wavePitch = (float) (0.7 + (curRadius / SUPERHEAT_EVAPORATION_RADIUS) * 0.5);
				playHostileSound(serverLevel, entity, "block.fire.extinguish", 1.8F, wavePitch);
				if (entity.tickCount % 6 == 0) {
					playHostileSound(serverLevel, entity, "block.lava.extinguish", 1.4F, 0.8F);
				}
			}

			serverLevel.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 1.2, entity.getZ(), 12, 0.8, 1.0, 0.8, 0.08);
			serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 1.5, entity.getZ(), 8, 0.5, 0.8, 0.5, 0.05);

			AABB hitBox = entity.getBoundingBox().inflate(curRadius);
			List<LivingEntity> burnVictims = serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && !isWoodboundEntity(e, entity));
			for (LivingEntity victim : burnVictims) {
				double distToRot = entity.distanceTo(victim);
				if (distToRot <= curRadius + 1.5) {
					float dmgFactor = (float) (1.0 - (distToRot / (SUPERHEAT_EVAPORATION_RADIUS + 2.0)) * 0.4);
					double baseDmg = SUPERHEAT_EVAPORATION_DAMAGE;
					String vicId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString().toLowerCase(java.util.Locale.ROOT);
					if (vicId.contains("stray") || vicId.contains("snow") || vicId.contains("ice") || vicId.contains("polar") || vicId.contains("frost") || vicId.contains("freeze") || victim.getTicksFrozen() > 0) {
						baseDmg *= (2.5 + entity.getRandom().nextDouble() * 0.5);
						victim.setTicksFrozen(0);
					}

					float finalSuperheatDmg = (float) Math.min(20.0, baseDmg * dmgFactor * getAdaptationMultiplier(entity));
					dealTrueDamageToBosses(victim, getBackwoodsDamage(serverLevel, "rot_solar_beam", entity), finalSuperheatDmg);
					victim.setRemainingFireTicks(200);

					Vec3 push = victim.position().subtract(entity.position()).normalize();
					setMotion(victim, push.x * 0.6, 0.25, push.z * 0.6);
				}
			}

			if (activeTicks <= 1) {
				clearDoubles(entity, "rot_superheat_active", "rot_superheat_current_radius");
			}
			return true;
		}

		return false;
	}

	private static boolean handleOverheadState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double overheadTicks = getD(entity, "rot_overhead_ticks");
		if (getB(entity, "debug_force_overhead")) {
			if (overheadTicks <= 1) {
				putD(entity, "rot_overhead_ticks", OVERHEAD_TOTAL_TICKS);
				overheadTicks = OVERHEAD_TOTAL_TICKS;
			}
		}

		if (overheadTicks > 0) {
			Entity targetEntity = null;
			if (world instanceof ServerLevel serverLevel) {
				try {
					String targetUUIDStr = getS(entity, "overhead_target_uuid");
					java.util.UUID targetUUID = !targetUUIDStr.isEmpty() ? java.util.UUID.fromString(targetUUIDStr) : null;
					if (targetUUID != null) {
						targetEntity = serverLevel.getEntity(targetUUID);
					}
				} catch (Exception ignored) {}
			}
			if (targetEntity == null || !targetEntity.isAlive() || targetEntity.isRemoved()) {
				targetEntity = combatTarget;
			}
			if (targetEntity == null || !targetEntity.isAlive() || targetEntity.isRemoved()) {
				targetEntity = findEntityInWorldRange(world, LivingEntity.class, entity.getX(), entity.getY(), entity.getZ(), 64.0, entity);
			}

			double strikeTick = OVERHEAD_STRIKE_TICK;
			boolean overheadStarted = getB(entity, "rot_overhead_started");
			if (!overheadStarted) {
				putB(entity, "rot_overhead_started", true);
				entity.setDeltaMovement(0.0, 0.0, 0.0);

				if (targetEntity != null) {
					Vec3 lookVec = targetEntity.getLookAngle();
					Vec3 horizLook = new Vec3(lookVec.x, 0.0, lookVec.z);
					if (horizLook.lengthSqr() > 0.001) {
						horizLook = horizLook.normalize();
					} else {
						horizLook = new Vec3(0, 0, 1);
					}
					double backwardOffset = 0.5;
					double targetX = targetEntity.getX() - horizLook.x * backwardOffset;
					double targetZ = targetEntity.getZ() - horizLook.z * backwardOffset;
					double overheadY = targetEntity.getY() + targetEntity.getEyeHeight() + OVERHEAD_Y_OFFSET_1;

					ClearFlightPathProcedure.execute(world, entity, entity.getX(), entity.getY(), entity.getZ(), targetX, overheadY, targetZ, 2.5);
					entity.teleportTo(targetX, overheadY, targetZ);
					lockLookAtTarget(entity, targetEntity);
				}
			} else {
				// Hold position in mid-air during the entire overhead strike sequence
				entity.setDeltaMovement(0.0, 0.0, 0.0);

				if (overheadTicks == strikeTick) {
					if (targetEntity instanceof LivingEntity liv) {
					double originalY = liv.getY();
					double targetSunkY = originalY - 1.0;
					BlockPos belowPos = BlockPos.containing(liv.getX(), targetSunkY, liv.getZ());
					if (world.getBlockState(belowPos).isCollisionShapeFullBlock(world, belowPos)) {
						targetSunkY = originalY;
					}
					disablePlayerShield(world, liv, 25.0 * getAdaptationMultiplier(entity), 100);
					net.minecraft.world.damagesource.DamageSource damageSource;
					if (world instanceof ServerLevel level) {
						damageSource = getBackwoodsDamage(level, "rot_overhead", entity);
					} else {
						damageSource = liv.damageSources().generic();
					}
					dealTrueDamageToBosses(liv, damageSource, 25.0F * (float) getAdaptationMultiplier(entity));
					liv.teleportTo(liv.getX(), targetSunkY, liv.getZ());
					setMotion(liv, liv.getDeltaMovement().x(), OVERHEAD_STRIKE_FALL_VELOCITY, liv.getDeltaMovement().z());
					liv.hurtMarked = true;
					liv.fallDistance += 5.0F;
					putD(entity, "sentinel_shockwave_stage", 1);
					putD(entity, "sentinel_shockwave_x", liv.getX());
					putD(entity, "sentinel_shockwave_y", liv.getY());
					putD(entity, "sentinel_shockwave_z", liv.getZ());
					putB(entity, "sentinel_shockwave_vertical", false);
					playHostileSound(world, liv, "entity.generic.explode", 1.5F, 0.55F);
					playHostileSound(world, liv, "entity.iron_golem.attack", 1.8F, 0.45F);
					if (world instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.SONIC_BOOM, liv.getX(), liv.getY() + 0.5, liv.getZ(), 1, 0, 0, 0, 0);
						boolean totemActive = getB(entity, "sentinel_totem_active");
						sendCameraShake(totemActive ? 0.4F : 0.25F, totemActive ? 12 : 6, totemActive ? 15.0F : 10.0F);
					}
				}
				} else if (overheadTicks < strikeTick) {
					if (targetEntity instanceof LivingEntity liv && !liv.onGround()) {
						setMotion(liv, liv.getDeltaMovement().x(), OVERHEAD_POST_STRIKE_FALL_VELOCITY, liv.getDeltaMovement().z());
						liv.hurtMarked = true;
						liv.fallDistance += 2.0F;
					}
				}
			}

			handlePassengerAndGrowth(entity);
			return true;
		}
		putB(entity, "rot_overhead_started", false);
		return false;
	}

	private static boolean handleThreatAwareEvasiveSpacing(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		if (entity == null || combatTarget == null || !combatTarget.isAlive()) {
			return false;
		}

		double evasiveCD = getD(entity, "sentinel_evasive_spacing_cd");
		if (evasiveCD > 0) {
			tickCooldown(entity, "sentinel_evasive_spacing_cd", 1);
			return false;
		}

		if (isRotChannelingAbility(entity)
			|| getD(entity, "rot_overhead_ticks") > 0
			|| getB(entity, "is_uppercutting")
			|| getD(entity, "sentinel_landing_ticks") > 0) {
			return false;
		}

		if (!(combatTarget instanceof LivingEntity livTarget)) {
			return false;
		}

		double estimatedDmg = 1.0;
		ItemStack mainHand = livTarget.getMainHandItem();
		if (!mainHand.isEmpty()) {
			String itemId = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString().toLowerCase();
			if (itemId.contains("mace")) {
				estimatedDmg += 12.0 + Math.max(0.0, livTarget.fallDistance * 8.0);
			} else if (itemId.contains("infinity") || itemId.contains("kill") || itemId.contains("god") || itemId.contains("op")) {
				estimatedDmg += 2000.0;
			} else if (itemId.contains("sword") || itemId.contains("axe")) {
				estimatedDmg += 10.0;
			}
		}

		if (livTarget.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)) {
			var eff = livTarget.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
			if (eff != null) estimatedDmg *= (1.0 + (eff.getAmplifier() + 1) * 0.5);
		}

		double rotHp = entity instanceof LivingEntity liv ? liv.getHealth() : 200.0;
		boolean isUltraThreat = estimatedDmg >= 120.0 || estimatedDmg >= rotHp * 0.5;

		double dist = entity.distanceTo(livTarget);
		if (isUltraThreat && dist < 12.0) {
			Vec3 retreatDir = entity.position().subtract(livTarget.position()).normalize();
			if (retreatDir.lengthSqr() < 0.001) {
				retreatDir = new Vec3(1, 0, 0);
			}

			double safeDist = 14.0 + Math.random() * 4.0;
			double targetX = entity.getX() + retreatDir.x * safeDist;
			double targetZ = entity.getZ() + retreatDir.z * safeDist;
			double targetY = findTargetGroundY(world, targetX, entity.getY(), targetZ);

			teleportEntity(entity, targetX, targetY, targetZ);
			lockLookAtTarget(entity, livTarget);
			putD(entity, "sentinel_evasive_spacing_cd", 60.0);

			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 25, 0.5, 1.0, 0.5, 0.15);
				playHostileSound(level, entity, "entity.enderman.teleport", 1.5F, 1.1F);
			}

			double randVal = Math.random();
			if (randVal < 0.40 && (getB(entity, "unlocked_die_rider_kick") || getB(entity, "unlocked_knockback_rider_combo"))) {
				putD(entity, "sentinel_die_kick_phase", 1.0);
				putD(entity, "sentinel_die_kick_ticks", 0.0);
			} else if (randVal < 0.75 && getB(entity, "unlocked_judgment")) {
				putD(entity, "sentinel_judgment_ticks", 40.0);
			} else {
				putD(entity, "omni_sonic_boom_ticks", 30.0);
			}

			return true;
		}

		return false;
	}

	private static boolean handleDiveCounterState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		if (entity == null) {
			return false;
		}

		if (isRotChannelingAbility(entity)
			|| getB(entity, "is_uppercutting")
			|| getD(entity, "sentinel_landing_ticks") > 0
			|| getD(entity, "sentinel_dive_counter_cd") > 0) {
			return false;
		}

		LivingEntity targetLiv = null;
		if (combatTarget instanceof LivingEntity liv) {
			targetLiv = liv;
		} else {
			AABB searchBox = new AABB(entity.getX() - 16.0, entity.getY() - 4.0, entity.getZ() - 16.0, entity.getX() + 16.0, entity.getY() + 28.0, entity.getZ() + 16.0);
			List<Player> nearbyPlayers = world.getEntitiesOfClass(Player.class, searchBox, p -> p.isAlive() && !p.isSpectator());
			for (Player p : nearbyPlayers) {
				String mainHand = BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString();
				String offHand = BuiltInRegistries.ITEM.getKey(p.getOffhandItem().getItem()).toString();
				if (mainHand.contains("mace") || offHand.contains("mace")) {
					targetLiv = p;
					break;
				}
			}
		}

		if (targetLiv == null || !targetLiv.isAlive()) {
			return false;
		}

		boolean targetHasMace = false;
		if (targetLiv instanceof Player player) {
			String mainHandName = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
			String offHandName = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
			targetHasMace = mainHandName.contains("mace") || offHandName.contains("mace");
		} else {
			String mainHandName = BuiltInRegistries.ITEM.getKey(targetLiv.getMainHandItem().getItem()).toString();
			String offHandName = BuiltInRegistries.ITEM.getKey(targetLiv.getOffhandItem().getItem()).toString();
			targetHasMace = mainHandName.contains("mace") || offHandName.contains("mace");
		}

		if (!targetHasMace) {
			return false;
		}

		double yDiff = targetLiv.getY() - entity.getY();
		if (yDiff < DIVE_COUNTER_MIN_HEIGHT) {
			return false;
		}

		double dx = targetLiv.getX() - entity.getX();
		double dz = targetLiv.getZ() - entity.getZ();
		double horizDist = Math.sqrt(dx * dx + dz * dz);
		if (horizDist > DIVE_COUNTER_TRIGGER_RANGE) {
			return false;
		}

		boolean isAirborne = !targetLiv.onGround();
		if (!isAirborne) {
			return false;
		}

		double downwardVel = targetLiv.getDeltaMovement().y();
		boolean isAtPeakOrDescending = downwardVel <= 0.35 || targetLiv.fallDistance >= 0.5;
		if (!isAtPeakOrDescending) {
			return false;
		}

		if (entity instanceof Mob mob && mob.getTarget() == null) {
			mob.setTarget(targetLiv);
		}
		putI(entity, "sentinel_combo_target_id", targetLiv.getId());

		putD(entity, "sentinel_dive_counter_cd", DIVE_COUNTER_COOLDOWN);

		boolean tryOverhead = Math.random() < 0.50 || getB(entity, "unlocked_overhead_combo");
		double currentOverheadTicks = getD(entity, "rot_overhead_ticks");
		if (tryOverhead && currentOverheadTicks <= 0) {
			putB(entity, "unlocked_overhead_combo", true);
			putD(entity, "rot_overhead_ticks", OVERHEAD_TOTAL_TICKS);
			putS(entity, "overhead_target_uuid", targetLiv.getUUID().toString());

			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.2, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.2);
				level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.2, entity.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
				playHostileSound(level, entity, "entity.warden.snarl", 1.8F, 1.2F);
				playHostileSound(level, entity, "entity.enderman.teleport", 1.8F, 0.9F);
			}

			handlePassengerAndGrowth(entity);
			return true;
		}

		double PUNCH_LEAD_TICKS = 2.0;
		Vec3 targetVel = targetLiv.getDeltaMovement();
		double predictedX = targetLiv.getX() + targetVel.x * PUNCH_LEAD_TICKS;
		double predictedZ = targetLiv.getZ() + targetVel.z * PUNCH_LEAD_TICKS;
		double groundY = findTargetGroundY(world, predictedX, targetLiv.getY(), predictedZ);

		teleportEntity(entity, predictedX, groundY, predictedZ);
		lockLookAtTarget(entity, targetLiv);

		putB(entity, "sentinel_dive_counter_active", true);

		putB(entity, "unlocked_high_sky_slam_combo", true);
		putB(entity, "is_uppercutting", true);
		boolean isLeft = Math.random() < 0.5;
		putB(entity, "is_uppercutting_left", isLeft);
		putB(entity, "is_uppercutting_right", !isLeft);
		putB(entity, "is_uppercut_standalone", true);
		putD(entity, "sentinel_cc2_stage", 1.0);
		putD(entity, "sentinel_cc2_ticks", UPPERCUT_LAUNCH_TICK + 5.0);
		if (entity instanceof RotEntity rot) {
			try { rot.getEntityData().set(RotEntity.DATA_is_uppercutting, true); } catch (Exception e) {}
		}

		if (world instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.2, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.2);
			level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 1.2, entity.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
			playHostileSound(level, entity, "entity.warden.snarl", 1.8F, 1.2F);
			playHostileSound(level, entity, "entity.iron_golem.attack", 1.8F, 0.9F);
		}

		handlePassengerAndGrowth(entity);
		return true;
	}

	private static void handleHeavyPunchState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double heavyLeftTicks = getD(entity, "sentinel_heavy_left_punch_ticks");
		double heavyRightTicks = getD(entity, "sentinel_heavy_right_punch_ticks");

		if (combatTarget != null && (heavyLeftTicks > 0 || heavyRightTicks > 0)) {
			snapLookAtTarget(entity, combatTarget);
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			entity.setDeltaMovement(0, entity.getDeltaMovement().y(), 0);
		}

		double jitteredPunchStrike = getTelegraphJitter(entity, "heavy_punch", HEAVY_PUNCH_STRIKE_TICK, -5.0, 5.0);
		boolean strikeLeft = (heavyLeftTicks == jitteredPunchStrike);
		boolean strikeRight = (heavyRightTicks == jitteredPunchStrike);

		if (strikeLeft || strikeRight) {
			Entity target = combatTarget;
			if (target == null && world instanceof ServerLevel level) {
				target = level.getNearestPlayer(entity, 4.5);
			}
			if (target != null && entity.distanceTo(target) <= 4.5) {
				putD(entity, "sentinel_heavy_punch_misses", 0.0);
				Vec3 knockDir = target.position().subtract(entity.position()).normalize();
				applyKnockbackAndSync(target, knockDir.x * 4.4, 0.70, knockDir.z * 4.4);
				ClearFlightPathProcedure.execute(world, target, target.getX(), target.getY(), target.getZ(), target.getX() + knockDir.x * 4.4, target.getY() + 0.70, target.getZ() + knockDir.z * 4.4, 1.2, true);

				if (target instanceof LivingEntity targetLiv) {
					double punchDmg = Math.min(100.0, MELEE_PUNCH_DAMAGE * getAdaptationMultiplier(entity));
					disablePlayerShield(world, targetLiv, punchDmg, 100);
				}
				if (world instanceof ServerLevel level) {
					float finalHeavyPunchDmg = (float) Math.min(100.0, MELEE_PUNCH_DAMAGE * getAdaptationMultiplier(entity));
					dealTrueDamageToBosses(target, getBackwoodsDamage(level, "rot_consecutive_punches", entity), finalHeavyPunchDmg);
				}

				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.8, target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
					level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
					level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
					playHostileSound(level, entity, "entity.iron_golem.attack", 1.6F, 0.45F);
					playHostileSound(level, entity, "entity.generic.explode", 0.8F, 0.45F);
					playHostileSound(level, entity, "entity.warden.sonic_boom", 0.6F, 0.4F);
				}
			} else {
				double misses = getD(entity, "sentinel_heavy_punch_misses");
				putD(entity, "sentinel_heavy_punch_misses", misses + 1.0);
			}
		}
	}

	private static void executeMinosHeavyPunchBlink(LevelAccessor world, Entity entity, LivingEntity targetLiv, boolean isLeftHand) {
		if (targetLiv == null || !hasHeavyPunchSupport(world, targetLiv)) return;
		cancelActiveBeams(world, entity);
		double targetYaw = targetLiv.getYRot();
		double radians = Math.toRadians(targetYaw);

		double missCount = getD(entity, "sentinel_heavy_punch_misses");

		Vec3 targetVel = targetLiv.getDeltaMovement();
		double speedSqr = targetVel.x * targetVel.x + targetVel.z * targetVel.z;
		boolean targetMovingAway = speedSqr > 0.01;

		double offsetDist = (missCount >= 1.0 || targetMovingAway) ? 2.2 : -1.3;

		double tx = targetLiv.getX() + Math.sin(radians) * offsetDist;
		double tz = targetLiv.getZ() - Math.cos(radians) * offsetDist;
		double ty = targetLiv.getY();

		double distance = entity.distanceTo(targetLiv);
		boolean targetStuck = (targetVel.horizontalDistance() < 0.05) || isTargetCornered(world, targetLiv, entity);
		boolean inRange = distance <= 4.2;
		boolean teleportRedundant = inRange && targetStuck;

		if (world instanceof ServerLevel level) {
			if (teleportRedundant) {
				level.sendParticles(ParticleTypes.GUST, entity.getX(), entity.getY() + 0.5, entity.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
				playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_charge", 0.9F, 1.3F);
			} else {
				level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 0.8, entity.getZ(), 12, 0.3, 0.5, 0.3, 0.1);
				ClearFlightPathProcedure.execute(world, entity, entity.getX(), entity.getY(), entity.getZ(), tx, ty, tz, 2.5);
				teleportEntity(entity, tx, ty, tz);
				level.sendParticles(ParticleTypes.SMOKE, tx, ty + 0.8, tz, 12, 0.3, 0.5, 0.3, 0.1);
				level.sendParticles(ParticleTypes.GUST, tx, ty + 0.5, tz, 1, 0.1, 0.1, 0.1, 0.0);

				playHostileSound(level, tx, ty, tz, "item.chorus_fruit.teleport", 1.4F, 0.9F);
				playHostileSound(level, tx, ty, tz, "entity.enderman.teleport", 1.2F, 0.6F);
				playHostileSound(level, tx, ty, tz, "entity.warden.sonic_charge", 1.0F, 1.2F);
			}
		}
		snapLookAtTarget(entity, targetLiv);

		if (isLeftHand) {
			putD(entity, "sentinel_heavy_left_punch_ticks", HEAVY_PUNCH_TOTAL_TICKS);
			putD(entity, "sentinel_heavy_right_punch_ticks", 0);
		} else {
			putD(entity, "sentinel_heavy_right_punch_ticks", HEAVY_PUNCH_TOTAL_TICKS);
			putD(entity, "sentinel_heavy_left_punch_ticks", 0);
		}
		if (entity instanceof LivingEntity ls) {
			ls.swing(InteractionHand.MAIN_HAND, true);
		}
	}

	private static boolean hasHeavyPunchSupport(LevelAccessor world, LivingEntity target) {
		if (target == null || target.onGround() || target.isInWater() || target.isInLava()) return true;
		BlockPos belowTarget = BlockPos.containing(target.getX(), target.getBoundingBox().minY - 0.05, target.getZ());
		BlockState belowState = world.getBlockState(belowTarget);
		if (belowState.blocksMotion() || !belowState.getFluidState().isEmpty() || belowState.is(net.minecraft.tags.BlockTags.LEAVES)) return true;
		BlockPos feet = BlockPos.containing(target.getX(), target.getBoundingBox().minY + 0.05, target.getZ());
		BlockPos head = BlockPos.containing(target.getX(), target.getBoundingBox().maxY - 0.05, target.getZ());
		return !world.getBlockState(feet).blocksMotion() && !world.getBlockState(head).blocksMotion();
	}

	private static boolean isTargetCornered(LevelAccessor world, Entity target, Entity attacker) {
		if (target == null) return false;
		int solidBlocks = 0;
		BlockPos p = target.blockPosition();
		if (world.getBlockState(p.east()).isSolid() || !world.getBlockState(p.east()).isAir()) solidBlocks++;
		if (world.getBlockState(p.west()).isSolid() || !world.getBlockState(p.west()).isAir()) solidBlocks++;
		if (world.getBlockState(p.north()).isSolid() || !world.getBlockState(p.north()).isAir()) solidBlocks++;
		if (world.getBlockState(p.south()).isSolid() || !world.getBlockState(p.south()).isAir()) solidBlocks++;
		if (solidBlocks >= 2) return true;

		Vec3 dir = target.position().subtract(attacker.position());
		if (dir.lengthSqr() > 0.01) {
			dir = dir.normalize();
		} else {
			dir = attacker.getLookAngle();
		}
		BlockPos behindPos = BlockPos.containing(target.getX() + dir.x * 1.2, target.getY() + 0.5, target.getZ() + dir.z * 1.2);
		BlockPos behindHead = behindPos.above();
		if (world.getBlockState(behindPos).isSolid() || world.getBlockState(behindHead).isSolid() || !world.getBlockState(behindPos).isAir()) {
			return true;
		}
		return false;
	}

	private static boolean handleSlamState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double slamPhase = getD(entity, "sentinel_slam_phase");
		double slamTicks = getD(entity, "sentinel_slam_ticks");

		if (slamPhase > 0) {
			if (slamTicks > 0) {
				putD(entity, "sentinel_slam_ticks", slamTicks - 1);
			}

			if (slamPhase == 1) {
				if (combatTarget != null) {
					lockLookAtTarget(entity, combatTarget);
				}
				Vec3 m = entity.getDeltaMovement();
				entity.setDeltaMovement(0.0, m.y(), 0.0);

				if (slamTicks <= 1) {
					putD(entity, "sentinel_slam_phase", 2);
					putD(entity, "sentinel_slam_ticks", 25);
					playHostileSound(world, entity, "entity.warden.sonic_charge", 1.5F, 0.75F);
				}
			} else if (slamPhase == 2) {
				entity.setDeltaMovement(0, 0, 0);
				if (combatTarget != null) {
					lockLookAtTarget(entity, combatTarget);
				}
				spawnParticles(world, ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.2, entity.getZ(), 4, 0.2, 0.2, 0.2, 0.1);

				if (slamTicks <= 1) {
					putD(entity, "sentinel_slam_phase", 3);
					putD(entity, "sentinel_slam_ticks", 5);
				}
			} else if (slamPhase == 3) {
				double startX = entity.getX();
				double startY = entity.getY();
				double startZ = entity.getZ();
				double groundY = findGroundY(world, entity);

				if (combatTarget != null && combatTarget.isAlive()) {
					Vec3 toTarget = combatTarget.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
					if (toTarget.lengthSqr() < 0.01) {
						toTarget = combatTarget.getLookAngle().multiply(1.0, 0.0, 1.0);
					}
					if (toTarget.lengthSqr() > 0.01) {
						toTarget = toTarget.normalize();
					} else {
						toTarget = new Vec3(1.0, 0.0, 0.0);
					}
					// Offset the landing spot from the target so they aren't exactly in the center
					double offsetDist = 0.8;
					startX = combatTarget.getX() - toTarget.x * offsetDist;
					startZ = combatTarget.getZ() - toTarget.z * offsetDist;
					groundY = findTargetGroundY(world, startX, combatTarget.getY(), startZ);
				}

				if (world instanceof ServerLevel level) {
					for (double sy = groundY; sy <= startY; sy += 0.5) {
						level.sendParticles(ParticleTypes.SONIC_BOOM, startX, sy, startZ, 1, 0.1, 0.1, 0.1, 0.0);
						level.sendParticles(ParticleTypes.CLOUD, startX, sy, startZ, 2, 0.15, 0.15, 0.15, 0.02);
					}
				}

				ClearFlightPathProcedure.execute(world, entity, entity.getX(), entity.getY(), entity.getZ(), startX, groundY, startZ, 3.0);
				entity.teleportTo(startX, groundY, startZ);
				entity.setDeltaMovement(0, -0.05, 0);

				clearDoubles(entity, "sentinel_slam_phase", "sentinel_slam_ticks");
				putD(entity, "sentinel_landing_ticks", 20);
				putB(entity, "sentinel_is_slam_landing", true);

				executeCrushLandingBlast(world, entity, combatTarget, false);
			}
			handlePassengerAndGrowth(entity);
			return true;
		}
		return false;
	}

	private static boolean handleDieKickState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double dieKickPhase = getD(entity, "sentinel_die_kick_phase");
		double dieKickTicks = getD(entity, "sentinel_die_kick_ticks");

		if (dieKickPhase > 0) {
			if (dieKickTicks > 0) {
				putD(entity, "sentinel_die_kick_ticks", dieKickTicks - 1);
			}

			if (dieKickPhase == 1) {
				if (combatTarget != null) {
					snapLookAtTarget(entity, combatTarget);
				}
				entity.setXRot(0.0F);
				if (entity instanceof Mob mob) {
					mob.setXRot(0.0F);
					mob.yHeadRot = mob.getYRot();
				}
				Vec3 m = entity.getDeltaMovement();
				entity.setDeltaMovement(0.0, m.y(), 0.0);

				if (dieKickTicks <= 1) {
					putD(entity, "sentinel_die_kick_phase", 2);
					putD(entity, "sentinel_die_kick_ticks", 25);
					playHostileSound(world, entity, "entity.warden.sonic_charge", 1.5F, 0.75F);
				}
			} else if (dieKickPhase == 2) {
				entity.setDeltaMovement(0, 0, 0);
				if (combatTarget != null) {
					snapLookAtTarget(entity, combatTarget);
				}
				spawnParticles(world, ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.2, entity.getZ(), 4, 0.2, 0.2, 0.2, 0.1);

				if (dieKickTicks <= 1) {
					putD(entity, "sentinel_die_kick_phase", 3);
					putD(entity, "sentinel_die_kick_ticks", 40);
					playHostileSound(world, entity, "entity.warden.sonic_boom", 1.5F, 0.55F);

					Vec3 dir;
					if (combatTarget != null) {
						snapLookAtTarget(entity, combatTarget);
						Vec3 targetCenter = combatTarget.getBoundingBox().getCenter();
						Vec3 rotCenter = entity.getBoundingBox().getCenter();
						dir = targetCenter.subtract(rotCenter);
						if (dir.length() > 0.1) {
							dir = dir.normalize();
						} else {
							dir = entity.getLookAngle().normalize();
						}
					} else {
						dir = entity.getLookAngle().normalize();
					}

					putD(entity, "sentinel_die_kick_dir_x", dir.x);
					putD(entity, "sentinel_die_kick_dir_y", dir.y);
					putD(entity, "sentinel_die_kick_dir_z", dir.z);

					setMotion(entity, dir.x * DIE_KICK_SPEED, dir.y * DIE_KICK_SPEED, dir.z * DIE_KICK_SPEED);
				}
			} else if (dieKickPhase == 3) {
				double dirX = getD(entity, "sentinel_die_kick_dir_x");
				double dirY = getRotPersistentDouble(entity, "sentinel_die_kick_dir_y", -1.0);
				double dirZ = getD(entity, "sentinel_die_kick_dir_z");

				setMotion(entity, dirX * DIE_KICK_SPEED, dirY * DIE_KICK_SPEED, dirZ * DIE_KICK_SPEED);
				entity.fallDistance = 0;

				double dh = Math.sqrt(dirX * dirX + dirZ * dirZ);
				float targetYRot = (float) (Mth.atan2(dirZ, dirX) * (180F / Math.PI)) - 90F;
				float targetXRot = (float) (-(Mth.atan2(dirY, dh) * (180F / Math.PI)));
				entity.setYRot(targetYRot);
				entity.setXRot(targetXRot);
				if (entity instanceof Mob mob) {
					mob.yBodyRot = targetYRot;
					mob.yHeadRot = targetYRot;
				}

				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.GUST, entity.getX(), entity.getY() + 0.3, entity.getZ(), 3, 0.1, 0.1, 0.1, 0.05);
					level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.3, entity.getZ(), 1, 0.1, 0.1, 0.1, 0.02);
				}

				double groundY = findGroundY(world, entity);
				boolean hitTarget = false;
				Vec3 impactPoint = null;

				if (combatTarget != null && combatTarget.isAlive()) {
					AABB targetBox = combatTarget.getBoundingBox();
					double rotHalfWidth = entity.getBbWidth() * 0.5;
					double rotHalfHeight = entity.getBbHeight() * 0.5;
					AABB wallBox = targetBox.inflate(rotHalfWidth, rotHalfHeight, rotHalfWidth);
					Vec3 startPos = entity.getBoundingBox().getCenter();
					Vec3 endPos = startPos.add(dirX * DIE_KICK_SPEED, dirY * DIE_KICK_SPEED, dirZ * DIE_KICK_SPEED);

					if (wallBox.contains(startPos) || entity.getBoundingBox().intersects(targetBox)) {
						hitTarget = true;
						impactPoint = startPos;
					} else {
						java.util.Optional<Vec3> clipOpt = wallBox.clip(startPos, endPos);
						if (clipOpt.isPresent()) {
							hitTarget = true;
							impactPoint = clipOpt.get();
						}
					}
				}

				double distToTarget = combatTarget != null ? entity.distanceTo(combatTarget) : 999.0;
				if (entity.onGround() || entity.getY() <= groundY + DIE_KICK_GROUND_OFFSET || hitTarget || distToTarget < DIE_KICK_IMPACT_DIST || dieKickTicks <= 1) {
					if (hitTarget && impactPoint != null) {
						double stopX = impactPoint.x;
						double stopY = impactPoint.y - entity.getBbHeight() * 0.5;
						double stopZ = impactPoint.z;
						entity.teleportTo(stopX, stopY, stopZ);
					} else {
						entity.teleportTo(entity.getX(), groundY, entity.getZ());
					}
					entity.setDeltaMovement(0, -0.05, 0);

					executeCrushLandingBlast(world, entity, combatTarget, true);
					putD(entity, "sentinel_die_kick_phase", 4);
					putD(entity, "sentinel_die_kick_ticks", 10);
				}
			} else if (dieKickPhase == 4) {
				entity.setDeltaMovement(0, -0.05, 0);
				if (dieKickTicks <= 1) {
					clearDoubles(entity, "sentinel_die_kick_phase", "sentinel_die_kick_ticks");
					putD(entity, "sentinel_landing_ticks", 20);
					putB(entity, "sentinel_is_slam_landing", true);
				}
			}
			handlePassengerAndGrowth(entity);
			return true;
		}
		return false;
	}

	private static void executeCrushLandingBlast(LevelAccessor world, Entity self, @Nullable Entity target, boolean shakeCamera) {
		if (!(world instanceof ServerLevel level)) return;
		LivingEntity ls = (self instanceof LivingEntity) ? (LivingEntity) self : null;

		double targetX = self.getX();
		double targetY = self.getY();
		double targetZ = self.getZ();

		boolean totemActive = getB(self, "sentinel_totem_active");
		boolean isInfinity = getB(self, "sentinel_is_infinity_totem");
		double blastRadius = totemActive ? 12.0 : 6.5;
		if (isInfinity) blastRadius = 15.0;
		double _adaptation = getAdaptationMultiplier(self);
		blastRadius *= _adaptation;
		float blastDamage = totemActive ? 55.0F : 30.0F;
		if (isInfinity) blastDamage *= 2.5F;
		blastDamage *= (float) _adaptation;

		playHostileSound(level, targetX, targetY, targetZ, "entity.generic.explode", 1.8F, 0.45F);
		if (isInfinity) {
			explodeWoodbound(level, self, targetX, targetY + 0.5, targetZ, 6.5F);
		}
		if (shakeCamera) {
			playHostileSound(level, targetX, targetY, targetZ, "entity.warden.sonic_boom", 1.5F, 0.6F);
		}
		playHostileSound(level, targetX, targetY, targetZ, "entity.iron_golem.attack", 2.0F, 0.5F);

		putD(self, "sentinel_shockwave_stage", 1);
		putD(self, "sentinel_shockwave_x", targetX);
		putD(self, "sentinel_shockwave_y", targetY);
		putD(self, "sentinel_shockwave_z", targetZ);

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetX, targetY + 0.5, targetZ, totemActive ? 6 : 3, 0.5, 0.3, 0.5, 0.15);

		net.minecraft.world.level.block.state.BlockState floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(targetX, targetY - 0.5, targetZ));
		if (floorState.isAir()) {
			floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(targetX, targetY - 1.5, targetZ));
		}
		if (floorState.isAir()) {
			floorState = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
		}
		net.minecraft.core.particles.BlockParticleOption dustPillarOptions = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, floorState);

		int smallRingCount = totemActive ? 28 : 16;
		for (int rIndex = 0; rIndex < smallRingCount; rIndex++) {
			double ang = (2 * Math.PI / smallRingCount) * rIndex;
			double c = Math.cos(ang);
			double s = Math.sin(ang);
			double r = totemActive ? 2.0 : 1.2;
			double px = targetX + c * r;
			double pz = targetZ + s * r;
			level.sendParticles(dustPillarOptions, px, targetY + 0.2, pz, 1, 0.0, 0.1, 0.0, 0.05);
		}

		net.minecraft.core.particles.ParticleType<?> _tsdType = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse("trial_spawner_detection"));
		net.minecraft.core.particles.ParticleOptions trialSpawnerDetection = _tsdType instanceof net.minecraft.core.particles.ParticleOptions _tsdOpt ? _tsdOpt : net.minecraft.core.particles.ParticleTypes.EFFECT;

		level.sendParticles(dustPillarOptions, targetX, targetY + 0.2, targetZ, totemActive ? 90 : 55, 1.5, 0.6, 1.5, 0.25);
		level.sendParticles(trialSpawnerDetection, targetX, targetY + 0.2, targetZ, totemActive ? 90 : 55, 1.5, 0.6, 1.5, 0.25);

		if (shakeCamera) {
			sendCameraShake(totemActive ? 1.5F : 1.0F, totemActive ? 25 : 15, totemActive ? 30.0F : 20.0F);
		}

		for (Entity contraption : level.getEntities(self, self.getBoundingBox().inflate(blastRadius), e -> isContraptionEntity(e))) {
			destroyContraption(level, self, contraption, contraption.position(), true);
		}

		java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, self.getBoundingBox().inflate(blastRadius), e -> e != self && !isWoodboundEntity(e, self));
		if (target instanceof LivingEntity && !targets.contains((LivingEntity) target) && !isWoodboundEntity(target, self)) {
			targets.add((LivingEntity) target);
		}
		for (LivingEntity victim : targets) {
			if (isWoodboundEntity(victim, self)) continue;
			disablePlayerShield(level, victim, blastDamage * getAdaptationMultiplier(self), 100);
			String dmgType = getD(self, "sentinel_die_kick_phase") > 0 ? "rot_die_rider_kick" : "rot_seismic_slam";
			dealTrueDamageToBosses(victim, getBackwoodsDamage(level, dmgType, self), blastDamage * (float) getAdaptationMultiplier(self));
			Vec3 push = victim.position().subtract(self.position()).multiply(1.0, 0.0, 1.0);
			double horizontalDist = Math.sqrt(push.x * push.x + push.z * push.z);

			if (horizontalDist < 0.1) {
				push = self.getLookAngle().multiply(1.0, 0.0, 1.0);
				horizontalDist = Math.sqrt(push.x * push.x + push.z * push.z);
			}
			if (horizontalDist > 0.01) {
				push = new Vec3(push.x / horizontalDist, 0, push.z / horizontalDist);
			} else {
				push = new Vec3(1, 0, 0);
			}
			double pushMult = totemActive ? 5.5 : 3.5;
			double pushUp = totemActive ? 1.4 : 0.95;
			if (isInfinity) {
				pushMult = 9.5;
				pushUp = 2.2;
			}
			applyKnockbackAndSync(victim, push.x * pushMult, pushUp, push.z * pushMult);
		}
	}

	private static double findGroundY(LevelAccessor world, Entity entity) {
		BlockPos pos = entity.blockPosition();
		int minY = world.getMinBuildHeight();
		int startY = pos.getY();
		int maxDist = Math.max(64, startY - minY + 2);
		for (int i = 0; i < maxDist; i++) {
			BlockPos belowPos = pos.below(i);
			if (belowPos.getY() < minY) {
				return minY + 1.0;
			}
			BlockState state = world.getBlockState(belowPos);
			if (state.isCollisionShapeFullBlock(world, belowPos) || !state.getFluidState().isEmpty()) {
				return belowPos.getY() + 1.0;
			}
		}
		return minY + 1.0;
	}

	private static boolean isHighAboveGround(Entity entity, double height) {
		LevelAccessor world = entity.level();
		BlockPos pos = entity.blockPosition();
		for (int i = 1; i <= (int) Math.ceil(height); i++) {
			BlockState state = world.getBlockState(pos.below(i));
			if (state.isCollisionShapeFullBlock(world, pos.below(i))) {
				return false;
			}
		}
		return true;
	}

	private static void breakBlocksBehindTarget(LevelAccessor world, Entity target, Vec3 push, boolean totemActive) {
		if (!(world instanceof ServerLevel level) || target == null) return;
		double height = target.getBbHeight();
		Vec3 targetPos = target.position();
		Vec3 behindDir = push.normalize();
		double[] distances = totemActive ? new double[]{0.8, 1.4, 2.0, 2.6, 3.2, 3.8} : new double[]{0.8, 1.4};
		for (double dist : distances) {
			Vec3 pathPos = targetPos.add(behindDir.scale(dist));
			for (double dy = 0; dy < height + 0.5; dy += 0.9) {
				if (totemActive) {
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							BlockPos bp = BlockPos.containing(pathPos.x + dx, pathPos.y + dy, pathPos.z + dz);
							BlockState state = level.getBlockState(bp);
							if (isGunOrRadarBlock(state)) {
								destroyScorchedGunOrRadarBlock(level, null, bp, state);
							} else {
								float hardness = state.getDestroySpeed(level, bp);
								if (hardness >= 0.0F && hardness <= 50.0F && !state.isAir()) {
									level.destroyBlock(bp, false);
									level.sendParticles(ParticleTypes.EXPLOSION, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.02);
								}
							}
						}
					}
				} else {
					BlockPos bp = BlockPos.containing(pathPos.x, pathPos.y + dy, pathPos.z);
					BlockState state = level.getBlockState(bp);
					if (isGunOrRadarBlock(state)) {
						destroyScorchedGunOrRadarBlock(level, null, bp, state);
					} else {
						float hardness = state.getDestroySpeed(level, bp);
						if (hardness >= 0.0F && hardness <= 50.0F && !state.isAir()) {
							level.destroyBlock(bp, false);
							level.sendParticles(ParticleTypes.CRIT, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.02);
						}
					}
				}
			}
		}
	}

	public static float ROT_CAMERA_SHAKE_STRENGTH = 0.3F;

	private static void sendCameraShake(float intensity, int duration, float range) {
		try {
			Class<?> msgClass = Class.forName("com.github.alexmodguy.citadel.server.message.CameraShakeMessage");
			java.lang.reflect.Constructor<?> constructor = msgClass.getConstructor(float.class, int.class, float.class);
			Object msgInstance = constructor.newInstance(intensity * ROT_CAMERA_SHAKE_STRENGTH, duration, range);

			Class<?> citadelClass = Class.forName("com.github.alexmodguy.citadel.Citadel");
			java.lang.reflect.Method sendMethod = citadelClass.getMethod("sendMSGToAll", Object.class);
			sendMethod.invoke(null, msgInstance);
		} catch (Exception e) {
		}
	}

	private static boolean handleScanningState(Entity entity) {
		double scanningTicks = getD(entity, "sentinel_scanning_ticks");
		if (scanningTicks > 0) {
			putD(entity, "sentinel_scanning_ticks", scanningTicks - 1.0);
			entity.setDeltaMovement(0, entity.getDeltaMovement().y(), 0);
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();

				double baseYaw = getD(entity, "sentinel_scanning_base_yaw");
				double maxTicks = getD(entity, "sentinel_scan_max_ticks");
				if (maxTicks <= 0.1) maxTicks = 60.0;
				double elapsed = maxTicks - scanningTicks;
				double fraction = elapsed / maxTicks;

				double targetOffset = 0;
				double targetPitch = 0;
				float lerpFactor = 0.18F;

				if (getB(entity, "sentinel_reacting_to_attacker")) {
					targetOffset = 0;
					targetPitch = getD(entity, "sentinel_scan_target_pitch");
					lerpFactor = 0.35F;
				} else {
					if (fraction < 0.25) {
						targetOffset = 0;
						targetPitch = 2.0;
					} else if (fraction < 0.55) {
						targetOffset = -55.0;
						targetPitch = -8.0;
					} else if (fraction < 0.85) {
						targetOffset = 55.0;
						targetPitch = 12.0;
					} else {
						targetOffset = 0;
						targetPitch = 0;
					}
				}

				float currentY = mob.getYRot();
				float targetY = (float) (baseYaw + targetOffset);
				float lerpedY = Mth.rotLerp(lerpFactor, currentY, targetY);

				mob.setYRot(lerpedY);
				mob.setYHeadRot(lerpedY);
				mob.yBodyRot = lerpedY;

				float currentX = mob.getXRot();
				float targetX = (float) targetPitch;
				float lerpedX = Mth.lerp(lerpFactor, currentX, targetX);
				mob.setXRot(lerpedX);

				double lx = mob.getX() - Math.sin(Math.toRadians(lerpedY)) * 5.0;
				double ly = mob.getEyeY() + Math.sin(Math.toRadians(-lerpedX)) * 5.0;
				double lz = mob.getZ() + Math.cos(Math.toRadians(lerpedY)) * 5.0;
				mob.getLookControl().setLookAt(lx, ly, lz, 45.0F, 45.0F);
			}
			return true;
		}
		putB(entity, "sentinel_reacting_to_attacker", false);
		return false;
	}

	private static boolean isCustomComboActive(Entity entity) {
		if (entity == null) return false;
		return getD(entity, "sentinel_cc1_stage") > 0
			|| getD(entity, "sentinel_cc2_stage") > 0
			|| getD(entity, "sentinel_cc3_stage") > 0
			|| getD(entity, "sentinel_cc4_stage") > 0
			|| getD(entity, "sentinel_cc5_stage") > 0;
	}

	private static boolean handleCustomCombos(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double cc1 = getD(entity, "sentinel_cc1_stage");
		double cc2 = getD(entity, "sentinel_cc2_stage");
		double cc3 = getD(entity, "sentinel_cc3_stage");
		double cc4 = getD(entity, "sentinel_cc4_stage");
		double cc5 = getD(entity, "sentinel_cc5_stage");

		boolean result = handleCustomCombosInternal(world, entity, combatTarget);

		double nextCc1 = getD(entity, "sentinel_cc1_stage");
		double nextCc2 = getD(entity, "sentinel_cc2_stage");
		double nextCc3 = getD(entity, "sentinel_cc3_stage");
		double nextCc4 = getD(entity, "sentinel_cc4_stage");
		double nextCc5 = getD(entity, "sentinel_cc5_stage");

		if (nextCc1 != cc1) {
			if (nextCc1 > 0) logActionToChat(world, entity, "TRIPLE THREAT COMBO (CC1) progressed to §aStage " + (int)nextCc1);
			else logActionToChat(world, entity, "TRIPLE THREAT COMBO (CC1) §cFinished/Cooldown started");
		}
		if (nextCc2 != cc2) {
			if (nextCc2 > 0) logActionToChat(world, entity, "HIGH SKY SLAM COMBO (CC2) progressed to §aStage " + (int)nextCc2);
			else logActionToChat(world, entity, "HIGH SKY SLAM COMBO (CC2) §cFinished/Cooldown started");
		}
		if (nextCc3 != cc3) {
			if (nextCc3 > 0) logActionToChat(world, entity, "KNOCKBACK DROPKICK COMBO (CC3) progressed to §aStage " + (int)nextCc3);
			else logActionToChat(world, entity, "KNOCKBACK DROPKICK COMBO (CC3) §cFinished/Cooldown started");
		}
		if (nextCc4 != cc4) {
			if (nextCc4 > 0) logActionToChat(world, entity, "KNOCKBACK RIDER COMBO (CC4) progressed to §aStage " + (int)nextCc4);
			else logActionToChat(world, entity, "KNOCKBACK RIDER COMBO (CC4) §cFinished/Cooldown started");
		}
		if (nextCc5 != cc5) {
			if (nextCc5 > 0) logActionToChat(world, entity, "HEAVENLY REPENTANCE PLUS (CC5) progressed to §aStage " + (int)nextCc5);
			else logActionToChat(world, entity, "HEAVENLY REPENTANCE PLUS (CC5) §cFinished/Cooldown started");
		}

		return result;
	}

	private static boolean handleCustomCombosInternal(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double cc1 = getD(entity, "sentinel_cc1_stage");
		double cc2 = getD(entity, "sentinel_cc2_stage");
		double cc3 = getD(entity, "sentinel_cc3_stage");
		double cc4 = getD(entity, "sentinel_cc4_stage");
		double cc5 = getD(entity, "sentinel_cc5_stage");
		double slamPhase = getD(entity, "sentinel_slam_phase");
		double judgmentTicks = getD(entity, "sentinel_judgment_ticks");
		double dieKickPhase = getD(entity, "sentinel_die_kick_phase");

		if (cc1 <= 0 && cc2 <= 0 && cc3 <= 0 && cc4 <= 0 && cc5 <= 0 && slamPhase <= 0 && judgmentTicks <= 0 && dieKickPhase <= 0) {
			putI(entity, "sentinel_combo_target_id", 0);
			return false;
		}

		if (combatTarget == null || !combatTarget.isAlive() || combatTarget.isRemoved()) {
			Entity recoveredTarget = null;
			if (world instanceof ServerLevel level) {
				int storedId = getI(entity, "sentinel_combo_target_id");
				if (storedId != 0) {
					Entity found = level.getEntity(storedId);
					if (found instanceof LivingEntity liv && liv.isAlive() && !liv.isRemoved()) {
						recoveredTarget = found;
					}
				}
				if (recoveredTarget == null) {
					try {
						String targetUUIDStr = getS(entity, "overhead_target_uuid");
						java.util.UUID targetUUID = !targetUUIDStr.isEmpty() ? java.util.UUID.fromString(targetUUIDStr) : null;
						if (targetUUID != null) {
							Entity found = level.getEntity(targetUUID);
							if (found instanceof LivingEntity liv && liv.isAlive() && !liv.isRemoved()) {
								recoveredTarget = found;
							}
						}
					} catch (Exception ignored) {}
				}
			}
			if (recoveredTarget == null) {
				recoveredTarget = findEntityInWorldRange(world, LivingEntity.class, entity.getX(), entity.getY(), entity.getZ(), 64.0, entity);
			}
			if (recoveredTarget != null) {
				combatTarget = recoveredTarget;
				if (entity instanceof Mob mob) {
					mob.setTarget((LivingEntity) recoveredTarget);
				}
				putI(entity, "sentinel_combo_target_id", recoveredTarget.getId());
			} else {
				clearDoubles(entity, "sentinel_cc1_stage", "sentinel_cc2_stage", "sentinel_cc3_stage", "sentinel_cc4_stage", "sentinel_cc5_stage", "sentinel_cc2_ticks", "sentinel_cc2_air_ticks", "sentinel_uppercut_anim_ticks", "rot_overhead_ticks");
				putB(entity, "is_uppercutting", false);
				putB(entity, "is_uppercutting_left", false);
				putB(entity, "is_uppercutting_right", false);
				putB(entity, "is_uppercut_standalone", false);
				putB(entity, "sentinel_cc2_launched", false);
				putB(entity, "rot_overhead_started", false);
				putI(entity, "sentinel_combo_target_id", 0);
				if (entity instanceof RotEntity rot) {
					try {
						rot.getEntityData().set(RotEntity.DATA_is_uppercutting, false);
						rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_left, false);
						rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_right, false);
					} catch (Exception ignored) {}
				}
				cleanupCombatFlags(entity);
				return false;
			}
		}

		int storedId = getI(entity, "sentinel_combo_target_id");
		if (storedId == 0) {
			putI(entity, "sentinel_combo_target_id", combatTarget != null ? combatTarget.getId() : 0);
		}

		if (getD(entity, "sentinel_slam_phase") > 0 
			|| getD(entity, "sentinel_judgment_ticks") > 0 
			|| getD(entity, "sentinel_die_kick_phase") > 0) {
			return false;
		}

		if (combatTarget != null) {
			lockLookAtTarget(entity, combatTarget);
		}

		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
		}

		boolean launchedAlready = getB(entity, "sentinel_cc2_launched");
		boolean isStandaloneUppercut = getB(entity, "is_uppercut_standalone");
		if ((cc1 == 1 || (cc2 == 1 && !launchedAlready && !isStandaloneUppercut) || cc3 == 1 || cc4 == 1) && entity.distanceTo(combatTarget) > 5.0) {
			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 20, 0.4, 0.8, 0.4, 0.1);
				playHostileSound(level, entity, "entity.enderman.teleport", 1.2F, 0.5F);
			}

			Vec3 dir = combatTarget.position().subtract(entity.position()).normalize();
			double targetX = combatTarget.getX() - dir.x * 2.5;
			double targetY = combatTarget.getY();
			double targetZ = combatTarget.getZ() - dir.z * 2.5;
			entity.teleportTo(targetX, targetY, targetZ);

			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.SMOKE, targetX, targetY + 1.0, targetZ, 20, 0.4, 0.8, 0.4, 0.1);
				playHostileSound(level, targetX, targetY, targetZ, "entity.enderman.teleport", 1.2F, 0.55F);
			}
		}

		boolean totemActive = getB(entity, "sentinel_totem_active");

		if (cc1 > 0) {
			double cc1Ticks = getD(entity, "sentinel_cc1_ticks");
			if (cc1 == 1) {
				setMotion(entity, 0.0, 1.9, 0.0);
				if (world instanceof ServerLevel level) {
					playHostileSound(level, entity, "entity.iron_golem.attack", 1.5F, 0.8F);
					level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
				}

				putD(entity, "sentinel_slam_phase", 1);
				putD(entity, "sentinel_slam_ticks", 22);

				putD(entity, "sentinel_cc1_stage", 2);
				putD(entity, "sentinel_cc1_ticks", 2);
			} else if (cc1 == 2) {
				slamPhase = getD(entity, "sentinel_slam_phase");
				if (slamPhase == 0) {
					if (combatTarget != null && entity.distanceTo(combatTarget) < 7.5) {
						Vec3 pushVec = combatTarget.position().subtract(entity.position()).normalize();
						double pushForce = totemActive ? 3.5 : 2.5;
						applyKnockbackAndSync(combatTarget, pushVec.x * pushForce, 1.45, pushVec.z * pushForce);
						putD(entity, "sentinel_cc1_stage", 4);
						putD(entity, "sentinel_cc1_ticks", 20);
					} else {
						putD(entity, "sentinel_judgment_ticks", 60);
						putD(entity, "sentinel_landing_ticks", 0);
						putD(entity, "sentinel_cc1_stage", 3);
						putD(entity, "sentinel_cc1_ticks", 40);
					}
				}
			} else if (cc1 == 3) {
				judgmentTicks = getD(entity, "sentinel_judgment_ticks");
				if (judgmentTicks == 0) {
					Vec3 pushVec = combatTarget.position().subtract(entity.position()).normalize();
					double pushForce = totemActive ? 3.5 : 2.5;
					applyKnockbackAndSync(combatTarget, pushVec.x * pushForce, 1.45, pushVec.z * pushForce);

					putD(entity, "sentinel_cc1_stage", 4);
					putD(entity, "sentinel_cc1_ticks", 20);
				}
			} else if (cc1 == 4) {
				if (cc1Ticks == 0) {
					setMotion(entity, 0.0, 1.95, 0.0);

					putD(entity, "sentinel_die_kick_phase", 1);
					putD(entity, "sentinel_die_kick_ticks", 22);
					putD(entity, "sentinel_landing_ticks", 0);

					if (world instanceof ServerLevel level) {
						playHostileSound(level, entity, "entity.iron_golem.attack", 1.5F, 0.8F);
						level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
					}
					putD(entity, "sentinel_cc1_stage", 5);
				}
			} else if (cc1 == 5) {
				dieKickPhase = getD(entity, "sentinel_die_kick_phase");
				double landingTicks = getD(entity, "sentinel_landing_ticks");
				if (dieKickPhase == 0 && landingTicks == 0) {
					double cd = totemActive ? COMBO_TRIPLE_THREAT_CD_TOTEM : COMBO_TRIPLE_THREAT_CD;
					putD(entity, "sentinel_cc1_cd", cd);
					putD(entity, "sentinel_cc1_stage", 0);
				}
				handlePassengerAndGrowth(entity);
			}
			handlePassengerAndGrowth(entity);
			return true;
		}

		if (cc2 > 0) {
			double cc2Ticks = getD(entity, "sentinel_cc2_ticks");
			double cc2Air = getD(entity, "sentinel_cc2_air_ticks");

			if (cc2 == 1) {
				putB(entity, "is_blocking", false);
				putD(entity, "rot_block_active_ticks", 0);
				if (cc2Ticks == 0.0) {
					boolean isDiveCounter = getB(entity, "sentinel_dive_counter_active");
					if (isDiveCounter) {
						putB(entity, "sentinel_dive_counter_active", false);
					} else if (combatTarget != null) {
						Vec3 look = combatTarget.getLookAngle().normalize();
						double tx = combatTarget.getX() + look.x * 1.8;
						double tz = combatTarget.getZ() + look.z * 1.8;
						double ty = findTargetGroundY(world, tx, combatTarget.getY(), tz);

						teleportEntity(entity, tx, ty, tz);
						lockLookAtTarget(entity, combatTarget);

						if (world instanceof ServerLevel level) {
							playTeleportEffects(level, entity, entity.getX(), entity.getY(), entity.getZ());
							playTeleportEffects(level, entity, tx, ty, tz);
							playHostileSound(level, tx, ty, tz, "entity.warden.snarl", 1.5F, 1.3F);
						}
					}

					putD(entity, "sentinel_cc2_ticks", UPPERCUT_TOTAL_TICKS);
					putB(entity, "is_uppercutting", true);
					boolean isLeftUppercut = Math.random() < 0.5;
					putB(entity, "is_uppercutting_left", isLeftUppercut);
					putB(entity, "is_uppercutting_right", !isLeftUppercut);
					putB(entity, "sentinel_cc2_launched", false);
				} else {
					if (combatTarget != null) {
						lockLookAtTarget(entity, combatTarget);
					}

					boolean launched = getB(entity, "sentinel_cc2_launched");
					if (cc2Ticks <= UPPERCUT_LAUNCH_TICK && !launched) {
						putB(entity, "sentinel_cc2_launched", true);
						if (entity instanceof LivingEntity ls) ls.swing(InteractionHand.MAIN_HAND, true);

						if (combatTarget != null) {
							if (entity.distanceTo(combatTarget) > 2.5) {
								boolean isDiveCounter = getB(entity, "sentinel_dive_counter_active");
								double tx, tz;
								if (isDiveCounter || combatTarget.getY() - entity.getY() > 2.0) {
									tx = combatTarget.getX();
									tz = combatTarget.getZ();
								} else {
									Vec3 look = combatTarget.getLookAngle().normalize();
									tx = combatTarget.getX() + look.x * 1.5;
									tz = combatTarget.getZ() + look.z * 1.5;
								}
								double ty = findTargetGroundY(world, tx, combatTarget.getY(), tz);
								teleportEntity(entity, tx, ty, tz);
							}

							boolean isTargetBlocking = (combatTarget instanceof LivingEntity liv && liv.isBlocking());
							if (world instanceof ServerLevel level) {
								try {
									dealTrueDamageToBosses(combatTarget, getBackwoodsDamage(level, "rot_uppercut", entity), (float) UPPERCUT_DAMAGE * (float) getAdaptationMultiplier(entity));
								} catch (Exception e) {
									dealTrueDamageToBosses(combatTarget, entity instanceof LivingEntity ls ? ls.damageSources().mobAttack(ls) : getMobAttackDamage(world), (float) UPPERCUT_DAMAGE * (float) getAdaptationMultiplier(entity));
								}
							} else {
								dealTrueDamageToBosses(combatTarget, entity instanceof LivingEntity ls ? ls.damageSources().mobAttack(ls) : getMobAttackDamage(world), (float) UPPERCUT_DAMAGE * (float) getAdaptationMultiplier(entity));
							}

							if (isTargetBlocking) {
								Vec3 knockDir = combatTarget.position().subtract(entity.position()).normalize();
								applyKnockbackAndSync(combatTarget, knockDir.x * 1.5, 0.60, knockDir.z * 1.5);
							} else if (combatTarget.isAlive()) {
								double launchHeightVel = 3.6 + Math.random() * 0.8;
								applyKnockbackAndSync(combatTarget, 0.0, launchHeightVel, 0.0);
								putD(entity, "sentinel_uppercut_launch_ticks", 4.0);

								ClearFlightPathProcedure.execute(world, combatTarget, combatTarget.getY(), combatTarget.getY() + 35.0, 1.5);
								if (!getB(entity, "is_uppercut_standalone")) {
									ClearFlightPathProcedure.execute(world, entity, entity.getY(), entity.getY() + 35.0, 2.0);
									putD(entity, "sentinel_cc2_ticks", Math.min(getD(entity, "sentinel_cc2_ticks"), 8.0));
								}
							} else {
								putB(entity, "sentinel_uppercut_dodged", true);
							}
						} else {
							putB(entity, "sentinel_uppercut_dodged", true);
						}

						if (world instanceof ServerLevel level) {
							if (combatTarget != null) {
								level.sendParticles(ParticleTypes.CLOUD, combatTarget.getX(), combatTarget.getY() + 0.2, combatTarget.getZ(), 15, 0.4, 0.4, 0.4, 0.15);
								level.sendParticles(ParticleTypes.EXPLOSION, combatTarget.getX(), combatTarget.getY() + 0.5, combatTarget.getZ(), 3, 0.2, 0.2, 0.2, 0.1);
							}
							playHostileSound(level, entity, "entity.iron_golem.attack", 1.8F, 0.6F);
							playHostileSound(level, entity, "entity.generic.explode", 1.2F, 1.4F);
						}
					}

					boolean targetDodged = getB(entity, "sentinel_uppercut_dodged");
					boolean isStandalone = getB(entity, "is_uppercut_standalone");

					if (cc2Ticks <= 1 || targetDodged) {
						putB(entity, "sentinel_uppercut_dodged", false);

						if (targetDodged || isStandalone) {
							putB(entity, "is_uppercutting", false);
							putB(entity, "is_uppercutting_left", false);
							putB(entity, "is_uppercutting_right", false);
							putB(entity, "is_uppercut_standalone", false);
							putB(entity, "sentinel_cc2_launched", false);
							clearDoubles(entity, "sentinel_cc2_stage", "sentinel_cc2_ticks", "sentinel_cc2_air_ticks");
							if (entity instanceof RotEntity rot) {
								try {
									rot.getEntityData().set(RotEntity.DATA_is_uppercutting, false);
									rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_left, false);
									rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_right, false);
								} catch (Exception ignored) {}
							}
							if (isStandalone) {
								putD(entity, "sentinel_uppercut_cd", 120.0);
							} else if (targetDodged) {
								putD(entity, "sentinel_cc2_cd", 60.0);
							}
						} else {
							putB(entity, "is_uppercutting", true);
							if (entity instanceof RotEntity rot) {
								try {
									rot.getEntityData().set(RotEntity.DATA_is_uppercutting, true);
									rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_left, false);
									rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_right, false);
								} catch (Exception ignored) {}
							}
							putD(entity, "sentinel_cc2_air_ticks", 1);
							putD(entity, "sentinel_cc2_stage", 2);
						}
					}
				}
			} else if (cc2 == 2) {
				if (getB(entity, "is_uppercut_standalone")) {
					putB(entity, "is_uppercutting", false);
					putB(entity, "is_uppercut_standalone", false);
					putB(entity, "sentinel_cc2_launched", false);
					clearDoubles(entity, "sentinel_cc2_stage", "sentinel_cc2_air_ticks", "sentinel_cc2_ticks");
					if (entity instanceof RotEntity rot) {
						try { rot.getEntityData().set(RotEntity.DATA_is_uppercutting, false); } catch (Exception ignored) {}
					}
					cleanupCombatFlags(entity);
					return false;
				}

				cc2Air = getD(entity, "sentinel_cc2_air_ticks");
				putD(entity, "sentinel_cc2_air_ticks", cc2Air + 1);

				if (cc2Air > 40 || combatTarget == null || !combatTarget.isAlive()) {
					putB(entity, "is_uppercutting", false);
					putB(entity, "is_uppercutting_left", false);
					putB(entity, "is_uppercutting_right", false);
					putB(entity, "is_uppercut_standalone", false);
					putB(entity, "sentinel_cc2_launched", false);
					if (entity instanceof RotEntity rot) {
						try {
							rot.getEntityData().set(RotEntity.DATA_is_uppercutting, false);
							rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_left, false);
							rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_right, false);
						} catch (Exception ignored) {}
					}
					clearDoubles(entity, "sentinel_cc2_stage", "sentinel_cc2_air_ticks", "sentinel_cc2_ticks");
					cleanupCombatFlags(entity);
					return false;
				}

				lockLookAtTarget(entity, combatTarget);

				// Seamless overhead trigger as target reaches/approaches peak Y height
				if (cc2Air >= 10 && (combatTarget.getDeltaMovement().y() <= 0.35 || combatTarget.onGround() || cc2Air >= 35)) {
					putB(entity, "is_uppercutting", false);
					putB(entity, "is_uppercutting_left", false);
					putB(entity, "is_uppercutting_right", false);
					if (entity instanceof RotEntity rot) {
						try {
							rot.getEntityData().set(RotEntity.DATA_is_uppercutting, false);
							rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_left, false);
							rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_right, false);
						} catch (Exception ignored) {}
					}
					Vec3 lookVec = combatTarget.getLookAngle();
					Vec3 horizLook = new Vec3(lookVec.x, 0.0, lookVec.z);
					if (horizLook.lengthSqr() > 0.001) {
						horizLook = horizLook.normalize();
					} else {
						horizLook = new Vec3(0, 0, 1);
					}
					double backwardOffset = 0.5;
					double overheadX = combatTarget.getX() - horizLook.x * backwardOffset;
					double overheadZ = combatTarget.getZ() - horizLook.z * backwardOffset;
					double overheadY = combatTarget.getY() + combatTarget.getEyeHeight() + OVERHEAD_Y_OFFSET_1;

					teleportEntity(entity, overheadX, overheadY, overheadZ);
					setMotion(entity, 0.0, 0.0, 0.0);

					if (world instanceof ServerLevel level) {
						playTeleportEffects(level, entity, entity.getX(), entity.getY(), entity.getZ());
						playTeleportEffects(level, entity, overheadX, overheadY, overheadZ);
						playHostileSound(level, overheadX, overheadY, overheadZ, "entity.enderman.teleport", 1.3F, 0.6F);
					}

					putD(entity, "rot_overhead_ticks", OVERHEAD_TOTAL_TICKS);
					putS(entity, "overhead_target_uuid", combatTarget.getUUID().toString());
					putB(entity, "rot_overhead_started", true);

					putB(entity, "is_left_punching", false);
					putB(entity, "is_right_punching", false);
					if (entity instanceof RotEntity rot) {
						try {
							rot.getEntityData().set(RotEntity.DATA_is_left_punching, false);
							rot.getEntityData().set(RotEntity.DATA_is_right_punching, false);
						} catch (Exception ignored) {}
					}

					putD(entity, "sentinel_cc2_stage", 3);
				}
			} else if (cc2 == 3) {
				if (combatTarget == null || !combatTarget.isAlive() || combatTarget.isRemoved()) {
					Entity altTarget = findEntityInWorldRange(world, LivingEntity.class, entity.getX(), entity.getY(), entity.getZ(), 16.0, entity);
					if (altTarget != null) {
						combatTarget = altTarget;
						if (entity instanceof Mob mob) mob.setTarget((LivingEntity) altTarget);
					}
				}

				handleOverheadState(world, entity, combatTarget);

				double overheadTicks = getD(entity, "rot_overhead_ticks");
				if (overheadTicks <= 0) {
					// 0.5s calculation & tracking interval (10 ticks) as target plummets to determine precise ground landing coordinates
					putD(entity, "sentinel_cc2_ticks", 10);
					putD(entity, "sentinel_cc2_stage", 4);
				}
			} else if (cc2 == 4) {
				cc2Ticks = getD(entity, "sentinel_cc2_ticks");
				if (combatTarget == null || !combatTarget.isAlive() || combatTarget.isRemoved()) {
					Entity altTarget = findEntityInWorldRange(world, LivingEntity.class, entity.getX(), entity.getY(), entity.getZ(), 16.0, entity);
					if (altTarget != null) {
						combatTarget = altTarget;
						if (entity instanceof Mob mob) mob.setTarget((LivingEntity) altTarget);
					}
				}

				if (cc2Ticks > 0) {
					putD(entity, "sentinel_cc2_ticks", cc2Ticks - 1);
					if (combatTarget != null) {
						lockLookAtTarget(entity, combatTarget);
						Vec3 targetVel = combatTarget.getDeltaMovement();
						double predX = combatTarget.getX() + targetVel.x * 2.5;
						double predZ = combatTarget.getZ() + targetVel.z * 2.5;
						double targetGroundY = findTargetGroundY(world, predX, combatTarget.getY(), predZ);

						teleportEntity(entity, predX, Math.max(entity.getY(), targetGroundY + 10.0), predZ);
						entity.setDeltaMovement(0.0, 0.0, 0.0);

						if (world instanceof ServerLevel level && cc2Ticks % 3 == 0) {
							level.sendParticles(ParticleTypes.SONIC_BOOM, entity.getX(), entity.getY() + 0.5, entity.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
							level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
						}
					}
				} else {
					entity.setDeltaMovement(0.0, -0.05, 0.0);
					if (world instanceof ServerLevel level) {
						playHostileSound(level, entity, "entity.warden.sonic_charge", 1.5F, 0.75F);
						level.sendParticles(ParticleTypes.SONIC_BOOM, entity.getX(), entity.getY() + 0.5, entity.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
					}
					putD(entity, "sentinel_slam_phase", 2);
					putD(entity, "sentinel_slam_ticks", 8);

					double cd = totemActive ? COMBO_HIGH_SKY_SLAM_CD_TOTEM : COMBO_HIGH_SKY_SLAM_CD;
					putD(entity, "sentinel_cc2_cd", cd);
					putD(entity, "sentinel_cc2_stage", 0);
				}
			}
			handlePassengerAndGrowth(entity);
			return true;
		}

		if (cc3 > 0) {
			double cc3Ticks = getD(entity, "sentinel_cc3_ticks");
			if (cc3 == 1) {
				if (entity instanceof LivingEntity ls) ls.swing(InteractionHand.MAIN_HAND, true);

				boolean lastHandLeft = getB(entity, "sentinel_punch_hand_toggle");
				putB(entity, "sentinel_punch_hand_toggle", !lastHandLeft);
				if (lastHandLeft) {
					putD(entity, "sentinel_heavy_left_punch_ticks", HEAVY_PUNCH_TOTAL_TICKS);
					putD(entity, "sentinel_heavy_right_punch_ticks", 0);
				} else {
					putD(entity, "sentinel_heavy_right_punch_ticks", HEAVY_PUNCH_TOTAL_TICKS);
					putD(entity, "sentinel_heavy_left_punch_ticks", 0);
				}
				clearDoubles(entity, "sentinel_left_punch_ticks", "sentinel_right_punch_ticks");

				putD(entity, "sentinel_cc3_ticks", HEAVY_PUNCH_TOTAL_TICKS + 5);
				putD(entity, "sentinel_cc3_stage", 2);
			} else if (cc3 == 2) {
				if (cc3Ticks == 0) {
					if (combatTarget != null && entity.distanceTo(combatTarget) >= 3.5) {
						putD(entity, "sentinel_judgment_ticks", 60);
						putD(entity, "sentinel_cc3_stage", 3);
					} else {
						if (combatTarget instanceof LivingEntity targetLiv) {
							executeMinosHeavyPunchBlink(world, entity, targetLiv, false);
						}
						putD(entity, "sentinel_cc3_stage", 0);
					}
				}
			} else if (cc3 == 3) {
				judgmentTicks = getD(entity, "sentinel_judgment_ticks");
				double landingTicks = getD(entity, "sentinel_landing_ticks");
				double riderHoldTicks = getD(entity, "sentinel_rider_hold_ticks");
				if (judgmentTicks == 0 && landingTicks == 0 && riderHoldTicks == 0) {
					double cd = totemActive ? COMBO_PUNCH_DROPKICK_CD_TOTEM : COMBO_PUNCH_DROPKICK_CD;
					putD(entity, "sentinel_cc3_cd", cd);
					putD(entity, "sentinel_cc3_stage", 0);
				}
				handlePassengerAndGrowth(entity);
			}
			handlePassengerAndGrowth(entity);
			return true;
		}

		if (cc4 > 0) {
			double cc4Ticks = getD(entity, "sentinel_cc4_ticks");
			if (cc4 == 1) {
				if (entity instanceof LivingEntity ls) ls.swing(InteractionHand.MAIN_HAND, true);

				boolean lastHandLeft = getB(entity, "sentinel_punch_hand_toggle");
				putB(entity, "sentinel_punch_hand_toggle", !lastHandLeft);
				if (lastHandLeft) {
					putD(entity, "sentinel_heavy_left_punch_ticks", HEAVY_PUNCH_TOTAL_TICKS);
					putD(entity, "sentinel_heavy_right_punch_ticks", 0);
				} else {
					putD(entity, "sentinel_heavy_right_punch_ticks", HEAVY_PUNCH_TOTAL_TICKS);
					putD(entity, "sentinel_heavy_left_punch_ticks", 0);
				}
				clearDoubles(entity, "sentinel_left_punch_ticks", "sentinel_right_punch_ticks");

				putD(entity, "sentinel_cc4_ticks", HEAVY_PUNCH_TOTAL_TICKS + 5);
				putD(entity, "sentinel_cc4_stage", 2);
			} else if (cc4 == 2) {
				if (cc4Ticks == 0) {
					if (combatTarget != null && combatTarget.isAlive()) {
						setMotion(entity, 0.0, 2.1, 0.0);

						putD(entity, "sentinel_die_kick_phase", 1);
						putD(entity, "sentinel_die_kick_ticks", 22);
						putD(entity, "sentinel_landing_ticks", 0);

						if (world instanceof ServerLevel level) {
							playHostileSound(level, entity, "entity.iron_golem.attack", 1.5F, 0.8F);
							level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
						}
						putD(entity, "sentinel_cc4_stage", 3);
					} else {
						putD(entity, "sentinel_cc4_stage", 0);
					}
				}
			} else if (cc4 == 3) {
				dieKickPhase = getD(entity, "sentinel_die_kick_phase");
				double landingTicks = getD(entity, "sentinel_landing_ticks");
				if (dieKickPhase == 0 && landingTicks == 0) {
					double cd = totemActive ? COMBO_PUNCH_RIDER_KICK_CD_TOTEM : COMBO_PUNCH_RIDER_KICK_CD;
					putD(entity, "sentinel_cc4_cd", cd);
					putD(entity, "sentinel_cc4_stage", 0);
				}
				handlePassengerAndGrowth(entity);
			}
			handlePassengerAndGrowth(entity);
			return true;
		}

		if (cc5 > 0) {
			double cc5Ticks = getD(entity, "sentinel_cc5_ticks");
			if (cc5 == 1) {
				// Step 1: Dropkick initiation (Judgment Dropkick)
				putD(entity, "sentinel_judgment_ticks", 60);
				putD(entity, "sentinel_slam_phase", 0);
				putD(entity, "rot_overhead_ticks", 0);
				putD(entity, "sentinel_die_kick_phase", 0);
				putD(entity, "sentinel_landing_ticks", 0);
				putB(entity, "rot_overhead_started", false);
				setMotion(entity, 0.0, 0.0, 0.0);

				if (world instanceof ServerLevel level) {
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_charge", 1.5F, 0.5F);
				}

				putD(entity, "sentinel_cc5_ticks", -1);
				putD(entity, "sentinel_cc5_stage", 2);
				return false;
			} else if (cc5 == 2) {
				// Step 2: Dropkick hits and launches target with air time; wait 0.5s interval (10 ticks) then predict trajectory for overhead
				judgmentTicks = getD(entity, "sentinel_judgment_ticks");
				if (judgmentTicks == 0) {
					if (cc5Ticks < 0) {
						putD(entity, "sentinel_cc5_ticks", 10);
					} else if (cc5Ticks > 0) {
						putD(entity, "sentinel_cc5_ticks", cc5Ticks - 1);
						if (combatTarget != null) {
							lockLookAtTarget(entity, combatTarget);
						}
					} else {
						// Predict trajectory of target to execute non-homing overhead
						Vec3 targetVel = combatTarget.getDeltaMovement();
						double predX = combatTarget.getX() + targetVel.x * 2.5;
						double predZ = combatTarget.getZ() + targetVel.z * 2.5;
						double groundY = findGroundY(world, combatTarget);
						double predY = Math.max(groundY + 1.0, combatTarget.getY() + Math.max(0.0, targetVel.y * 2.5));

						Vec3 lookVec = combatTarget.getLookAngle();
						Vec3 horizLook = new Vec3(lookVec.x, 0.0, lookVec.z);
						if (horizLook.lengthSqr() > 0.001) {
							horizLook = horizLook.normalize();
						} else {
							horizLook = new Vec3(0, 0, 1);
						}
						double backwardOffset = 0.5;
						double overheadX = predX - horizLook.x * backwardOffset;
						double overheadZ = predZ - horizLook.z * backwardOffset;
						double overheadY = predY + combatTarget.getEyeHeight() + OVERHEAD_Y_OFFSET_1;

						ClearFlightPathProcedure.execute(world, entity, entity.getX(), entity.getY(), entity.getZ(), overheadX, overheadY, overheadZ, 2.5);
						teleportEntity(entity, overheadX, overheadY, overheadZ);
						setMotion(entity, 0.0, 0.0, 0.0);
						putD(entity, "sentinel_rider_hold_ticks", 0);
						putD(entity, "sentinel_landing_ticks", 0);

						if (world instanceof ServerLevel level) {
							playTeleportEffects(level, entity, entity.getX(), entity.getY(), entity.getZ());
							playTeleportEffects(level, entity, overheadX, overheadY, overheadZ);
							playHostileSound(level, overheadX, overheadY, overheadZ, "entity.enderman.teleport", 1.3F, 0.6F);
						}

						putD(entity, "rot_overhead_ticks", OVERHEAD_TOTAL_TICKS);
						putS(entity, "overhead_target_uuid", combatTarget.getUUID().toString());
						putB(entity, "rot_overhead_started", true);

						putB(entity, "is_left_punching", false);
						putB(entity, "is_right_punching", false);
						if (entity instanceof RotEntity rot) {
							try {
								rot.getEntityData().set(RotEntity.DATA_is_left_punching, false);
								rot.getEntityData().set(RotEntity.DATA_is_right_punching, false);
							} catch (Exception ignored) {}
						}

						putD(entity, "sentinel_cc5_stage", 3);
					}
				}
			} else if (cc5 == 3) {
				// Step 3: Overhead strike execution (non-homing)
				handleOverheadState(world, entity, combatTarget);

				double overheadTicks = getD(entity, "rot_overhead_ticks");
				if (overheadTicks <= 0) {
					// 0.5s interval (10 ticks) before the crush move
					putD(entity, "sentinel_cc5_ticks", 10);
					putD(entity, "sentinel_cc5_stage", 4);
				}
			} else if (cc5 == 4) {
				// Step 4: After interval, execute Crush Move
				if (cc5Ticks > 0) {
					putD(entity, "sentinel_cc5_ticks", cc5Ticks - 1);
					entity.setDeltaMovement(0.0, 0.0, 0.0);
				} else {
					entity.setDeltaMovement(0.0, -0.05, 0.0);
					if (world instanceof ServerLevel level) {
						playHostileSound(level, entity, "entity.warden.sonic_charge", 1.5F, 0.75F);
						level.sendParticles(ParticleTypes.SONIC_BOOM, entity.getX(), entity.getY() + 0.5, entity.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
					}
					putD(entity, "sentinel_slam_phase", 2);
					putD(entity, "sentinel_slam_ticks", 8);

					double cd = totemActive ? COMBO_HEAVENLY_REPENTANCE_PLUS_CD_TOTEM : COMBO_HEAVENLY_REPENTANCE_PLUS_CD;
					putD(entity, "sentinel_cc5_cd", cd);
					putD(entity, "sentinel_cc5_stage", 0);
				}
			}
			handlePassengerAndGrowth(entity);
			return true;
		}

		return false;
	}

	private static boolean isIndestructibleArmorStack(ItemStack stack) {
		if (stack.isEmpty()) return false;
		if (stack.getMaxDamage() <= 0) return true;
		if (!stack.isDamageableItem()) return true;
		try {
			if (stack.has(net.minecraft.core.component.DataComponents.UNBREAKABLE)) return true;
		} catch (Throwable t) {}
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(java.util.Locale.ROOT);
		String ns = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().toLowerCase(java.util.Locale.ROOT);
		if (ns.equals("projecte") || ns.equals("avaritia") || ns.equals("mekanism") || ns.equals("cataclysm") || ns.equals("draconicevolution") || ns.equals("botania") || ns.equals("bloodmagic") || ns.equals("enigmaticlegacy")) {
			return true;
		}
		if (id.contains("infinity") || id.contains("indestructible") || id.contains("mekasuit") || id.contains("draconic") || id.contains("unbreakable") || id.contains("creative") || id.contains("quantum") || id.contains("bound")) {
			return true;
		}
		return false;
	}

	private static void executeArmorRipChoke(LevelAccessor world, Entity entity, @Nullable Entity combatTarget, int tickRemaining) {
		if (combatTarget == null || !combatTarget.isAlive()) {
			putD(entity, "rot_armor_rip_ticks", 0);
			putB(entity, "is_armor_ripping", false);
			return;
		}

		if (combatTarget.level() != entity.level() || combatTarget.position().distanceTo(entity.position()) > ARMOR_RIP_MAX_DISTANCE) {
			putD(entity, "rot_armor_rip_ticks", 0);
			putB(entity, "is_armor_ripping", false);
			return;
		}

		double lockedYaw = getD(entity, "rot_choke_locked_yaw");
		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
			mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
			mob.setYRot((float) lockedYaw);
			mob.setYHeadRot((float) lockedYaw);
			mob.yBodyRot = (float) lockedYaw;
		}

		if (combatTarget instanceof LivingEntity livTarget) {
			livTarget.setYRot((float) (lockedYaw + 180.0));
			livTarget.setYHeadRot((float) (lockedYaw + 180.0));
			livTarget.yBodyRot = (float) (lockedYaw + 180.0);
		} else {
			combatTarget.setYRot((float) (lockedYaw + 180.0));
		}

		double dx = -Math.sin(Math.toRadians(lockedYaw));
		double dz = Math.cos(Math.toRadians(lockedYaw));
		double right_dx = Math.cos(Math.toRadians(lockedYaw));
		double right_dz = Math.sin(Math.toRadians(lockedYaw));

		double holdDistance = ARMOR_RIP_HOLD_DISTANCE;
		double rightOffset = ARMOR_RIP_RIGHT_OFFSET;
		double targetHoldX = entity.getX() + dx * holdDistance + right_dx * rightOffset;
		double targetHoldY = entity.getY() + ARMOR_RIP_HEIGHT_OFFSET;
		double targetHoldZ = entity.getZ() + dz * holdDistance + right_dz * rightOffset;

		combatTarget.teleportTo(targetHoldX, targetHoldY, targetHoldZ);
		combatTarget.setDeltaMovement(0, 0, 0);
		combatTarget.fallDistance = 0.0F;

		if (combatTarget instanceof Player chokePlayer) {
			boolean chokeTotemLearned = getB(entity, "sentinel_totem_learned");
			net.minecraft.world.item.ItemStack chokeTotem = net.minecraft.world.item.ItemStack.EMPTY;
			boolean chokeIsInfinity = false;
			net.minecraft.world.item.ItemStack chokeMain = chokePlayer.getMainHandItem();
			net.minecraft.world.item.ItemStack chokeOff = chokePlayer.getOffhandItem();
			if (!chokeMain.isEmpty()) {
				String id = BuiltInRegistries.ITEM.getKey(chokeMain.getItem()).toString();
				if (chokeMain.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || id.equals("avaritia:infinity_totem")) {
					chokeTotem = chokeMain;
					chokeIsInfinity = id.equals("avaritia:infinity_totem");
				}
			}
			if (chokeTotem.isEmpty() && !chokeOff.isEmpty()) {
				String id = BuiltInRegistries.ITEM.getKey(chokeOff.getItem()).toString();
				if (chokeOff.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || id.equals("avaritia:infinity_totem")) {
					chokeTotem = chokeOff;
					chokeIsInfinity = id.equals("avaritia:infinity_totem");
				}
			}
			if (!chokeTotem.isEmpty()) {
				chokeTotemLearned = true;
				putB(entity, "sentinel_totem_learned", true);
			}
			if (chokeTotem.isEmpty() && chokeTotemLearned) {
				for (int slot = 0; slot < chokePlayer.getInventory().getContainerSize(); slot++) {
					net.minecraft.world.item.ItemStack s = chokePlayer.getInventory().getItem(slot);
					if (!s.isEmpty()) {
						String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
						if (s.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || id.equals("avaritia:infinity_totem")) {
							chokeTotem = s;
							chokeIsInfinity = id.equals("avaritia:infinity_totem");
							break;
						}
					}
				}
			}

			if (!chokeTotem.isEmpty()) {
				chokeTotem.shrink(1);
				putB(entity, "sentinel_is_infinity_totem", chokeIsInfinity);
				putB(entity, "sentinel_totem_stolen", true);
				putB(entity, "sentinel_just_stole_totem", true);
				putB(entity, "sentinel_totem_learned", true);
				putD(entity, "sentinel_totem_inspect_ticks", 180);
				putD(entity, "rot_armor_rip_ticks", 0);
				putB(entity, "is_armor_ripping", false);
				if (world instanceof ServerLevel level) {
					playHostileSound(level, entity, "block.bell.resonate", 1.5F, 0.6F);
				}
				handlePassengerAndGrowth(entity);
				return;
			}
		}

		if (combatTarget instanceof LivingEntity livTarget && livTarget.isBlocking()) {
			disablePlayerShield(world, livTarget, 30.0, 100);
		}

		if (combatTarget instanceof LivingEntity livTarget) {
			applyEffectSafe(livTarget, new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 60, 1, false, false, false));
			try {
				applyEffectSafe(livTarget, new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 60, 0, false, false, false));
			} catch (Throwable t) {
				applyEffectSafe(livTarget, new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0, false, false, false));
			}
			java.util.List<net.minecraft.world.entity.EquipmentSlot> equippedArmorSlots = new java.util.ArrayList<>();
			for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
				if (slot.isArmor() && !livTarget.getItemBySlot(slot).isEmpty()) {
					equippedArmorSlots.add(slot);
				}
			}

			if (!equippedArmorSlots.isEmpty()) {
				int randomIndex = net.minecraft.util.RandomSource.create().nextInt(equippedArmorSlots.size());
				net.minecraft.world.entity.EquipmentSlot randomSlot = equippedArmorSlots.get(randomIndex);
				ItemStack armorPiece = livTarget.getItemBySlot(randomSlot);

				boolean isIndestructible = isIndestructibleArmorStack(armorPiece);

				if (isIndestructible) {
					if (tickRemaining % CHOKE_INDESTRUCTIBLE_DROP_INTERVAL == 0) {
						livTarget.setItemSlot(randomSlot, ItemStack.EMPTY);
						if (world instanceof ServerLevel level) {
							net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
								level, livTarget.getX(), livTarget.getY(), livTarget.getZ(), armorPiece
							);
							itemEntity.setPickUpDelay(30);
							level.addFreshEntity(itemEntity);

							level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
								net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.85F);
							level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
								net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.85F);

							level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, armorPiece),
								livTarget.getX(), livTarget.getY() + 1.0, livTarget.getZ(), 20, 0.2, 0.2, 0.2, 0.05);
						}
					}
				} else {
					if (tickRemaining % CHOKE_DAMAGE_INTERVAL == 0) {
						if (world instanceof ServerLevel level) {
							if (livTarget instanceof ServerPlayer serverPlayer) {
								armorPiece.hurtAndBreak(CHOKE_ARMOR_DURABILITY_LOSS, level, serverPlayer, (item) -> {
									level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
										net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.85F);
								});
							} else {
								armorPiece.hurtAndBreak(CHOKE_ARMOR_DURABILITY_LOSS, level, null, (item) -> {
									level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
										net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.85F);
								});
							}
							level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
								net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.7F);
						}
					}
				}
			}

			if (tickRemaining % CHOKE_DAMAGE_INTERVAL == 0) {
				if (world instanceof ServerLevel level) {
					try {
						dealTrueDamageToBosses(livTarget, getBackwoodsDamage(level, "rot_choke_rip", entity), (float) CHOKE_DAMAGE * (float) getAdaptationMultiplier(entity));
					} catch (Exception e) {
						dealTrueDamageToBosses(livTarget, livTarget.damageSources().mobAttack(entity instanceof LivingEntity le ? le : null), (float) CHOKE_DAMAGE * (float) getAdaptationMultiplier(entity));
					}
					level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
						net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
					level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
						net.minecraft.sounds.SoundEvents.PLAYER_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.75F);
					level.sendParticles(ParticleTypes.SQUID_INK, livTarget.getX(), livTarget.getY() + 1.2, livTarget.getZ(), 8, 0.2, 0.3, 0.2, 0.05);
					level.sendParticles(ParticleTypes.DUST_PLUME, livTarget.getX(), livTarget.getY() + 1.0, livTarget.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
				}
			}
		}

		if (tickRemaining == 0) {
			putB(entity, "is_armor_ripping", false);
			combatTarget.setDeltaMovement(combatTarget.getDeltaMovement().x, -0.4, combatTarget.getDeltaMovement().z);
			combatTarget.hurtMarked = true;
		}
	}

	private static boolean shouldIgnoreCombatFilter(Entity entity) {
		if (entity == null) return false;
		if (getB(entity, "has_kill_command_override")
			|| entity.getTags().contains("mob_battle")
			|| entity.getTags().contains("mobbattle")
			|| entity.getTags().contains("test")
			|| entity.getTags().contains("ignore_targets")
			|| entity.getTeam() != null
			|| getB(entity, "mob_battle_mode")
			|| entity.getPersistentData().contains("MobBattleTarget")
			|| (entity instanceof Mob mob && mob.getTarget() != null && (mob.getTarget().getTags().contains("mob_battle") || mob.getTarget().getTeam() != null))) {
			return true;
		}
		for (String tag : entity.getTags()) {
			String lower = tag.toLowerCase(java.util.Locale.ROOT);
			if (lower.contains("battle") || lower.contains("stick") || lower.contains("target")) {
				return true;
			}
		}
		return false;
	}

	public static class CombatContext {
		public boolean isAirborne;
		public boolean isHealing;
		public boolean isBlocking;
		public boolean isCornered;
		public boolean isMovingFast;
		public boolean isEatingHealingItem;
		public int eatingTicksRemaining;
		public double dist;
		public double dY;

		public boolean isJumpCritIncoming;
		public double incomingProjectileDistance;
		public int nearbyTargetCount;
		public boolean targetNearLedgeOrHazard;
		public boolean isEnclosedSpace;
		public double expectedIncomingDamage;

		public boolean isAggressive;
		public boolean isFleeing;
		public boolean isIncomingDamage;
	}

	public static CombatContext getCombatContext(Entity self, Entity target) {
		CombatContext ctx = new CombatContext();
		if (self == null || target == null) return ctx;
		ctx.dist = self.distanceTo(target);
		ctx.dY = target.getY() - self.getY();
		ctx.isAirborne = !target.onGround() && ctx.dY > 2.0;

		if (target instanceof LivingEntity liv) {
			ctx.isBlocking = liv.isBlocking();
			if (liv.isUsingItem()) {
				ItemStack useItem = liv.getUseItem();
				if (useItem.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
						|| useItem.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE)
						|| useItem.is(net.minecraft.world.item.Items.MILK_BUCKET)
						|| useItem.getItem() instanceof net.minecraft.world.item.PotionItem) {
					ctx.isEatingHealingItem = true;
					ctx.eatingTicksRemaining = liv.getUseItemRemainingTicks();
				}
			}
			ctx.isHealing = ctx.isEatingHealingItem || liv.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION) || liv.getHealth() < liv.getMaxHealth() * 0.4 || (liv.isUsingItem() && liv.getHealth() < liv.getMaxHealth());

			boolean isFalling = target.getDeltaMovement().y < -0.05 && !target.onGround();
			boolean isSwingingOrAttacking = liv.swinging || (liv instanceof Player p && p.getAttackStrengthScale(0.5f) > 0.6f);
			ctx.isJumpCritIncoming = isFalling && isSwingingOrAttacking && ctx.dist <= 4.0;
		}

		ctx.isMovingFast = target.getDeltaMovement().horizontalDistanceSqr() > 0.04;

		int solidBlocks = 0;
		net.minecraft.core.BlockPos p = target.blockPosition();
		net.minecraft.world.level.Level lvl = target.level();
		if (lvl.getBlockState(p.east()).isSolid()) solidBlocks++;
		if (lvl.getBlockState(p.west()).isSolid()) solidBlocks++;
		if (lvl.getBlockState(p.north()).isSolid()) solidBlocks++;
		if (lvl.getBlockState(p.south()).isSolid()) solidBlocks++;
		ctx.isCornered = solidBlocks >= 2;

		ctx.incomingProjectileDistance = -1.0;
		if (self != null && self.level() instanceof net.minecraft.world.level.Level level) {
			AABB pBox = self.getBoundingBox().inflate(16.0);
			List<net.minecraft.world.entity.projectile.Projectile> projs = level.getEntitiesOfClass(
				net.minecraft.world.entity.projectile.Projectile.class, pBox,
				proj -> proj.getOwner() != self && proj.getDeltaMovement().lengthSqr() > 0.04
			);
			Vec3 selfPos = self.position().add(0, self.getBbHeight() * 0.5, 0);
			double minProjDist = 999.0;
			for (net.minecraft.world.entity.projectile.Projectile proj : projs) {
				Vec3 pPos = proj.position();
				Vec3 pVel = proj.getDeltaMovement();
				Vec3 toSelf = selfPos.subtract(pPos);
				if (pVel.dot(toSelf) > 0) {
					double t = toSelf.dot(pVel) / pVel.lengthSqr();
					if (t > 0 && t <= 20.0) {
						Vec3 closest = pPos.add(pVel.scale(t));
						if (closest.distanceTo(selfPos) < 2.5) {
							double d = pPos.distanceTo(selfPos);
							if (d < minProjDist) minProjDist = d;
						}
					}
				}
			}
			if (minProjDist < 990.0) {
				ctx.incomingProjectileDistance = minProjDist;
			}
		}

		double expectedDamage = 0.0;
		if (target instanceof LivingEntity liv) {
			double attackDamage = getSafeAttributeValue(liv, Attributes.ATTACK_DAMAGE, 4.0);
			if (attackDamage <= 0.0) attackDamage = 4.0;
			double readiness = target instanceof Player ? ((Player) target).getAttackStrengthScale(0.5f) : (liv.swinging ? 1.0 : 0.35);
			if (ctx.dist <= 4.5 && (liv.swinging || readiness > 0.8 || target instanceof Mob)) {
				expectedDamage += attackDamage * readiness;
			}
			if (ctx.isJumpCritIncoming) expectedDamage *= 1.5;
		}
		if (ctx.incomingProjectileDistance > 0.0) {
			double projectilePressure = 4.0 + getD(self, "sentinel_sustained_bullet_hits") * 1.5;
			expectedDamage += projectilePressure * Math.max(0.25, 1.0 - ctx.incomingProjectileDistance / 20.0);
		}
		ctx.expectedIncomingDamage = expectedDamage;

		if (self != null && self.level() instanceof net.minecraft.world.level.Level level) {
			AABB crowdBox = self.getBoundingBox().inflate(8.0);
			List<LivingEntity> crowd = level.getEntitiesOfClass(
				LivingEntity.class, crowdBox,
				e -> e != self && e.isAlive() && (e instanceof Player || e instanceof Mob)
			);
			ctx.nearbyTargetCount = Math.max(1, crowd.size());
		} else {
			ctx.nearbyTargetCount = 1;
		}

		if (target != null && target.level() instanceof net.minecraft.world.level.Level level) {
			BlockPos tPos = target.blockPosition();
			boolean hazard = false;
			for (BlockPos offset : BlockPos.betweenClosed(tPos.offset(-2, -1, -2), tPos.offset(2, 0, 2))) {
				net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(offset);
				if (bs.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) || bs.is(net.minecraft.world.level.block.Blocks.VOID_AIR)) {
					hazard = true;
					break;
				}
			}
			if (!hazard) {
				for (int x = -2; x <= 2; x++) {
					for (int z = -2; z <= 2; z++) {
						if (Math.abs(x) + Math.abs(z) == 0) continue;
						BlockPos checkPos = tPos.offset(x, 0, z);
						int drop = 0;
						while (drop < 5 && level.isEmptyBlock(checkPos.below(drop))) {
							drop++;
						}
						if (drop >= 4) {
							hazard = true;
							break;
						}
					}
					if (hazard) break;
				}
			}
			ctx.targetNearLedgeOrHazard = hazard;
		}

		if (self != null && self.level() instanceof net.minecraft.world.level.Level level) {
			BlockPos selfPos = self.blockPosition();
			int solidCount = 0;
			for (BlockPos offset : BlockPos.betweenClosed(selfPos.offset(-2, 0, -2), selfPos.offset(2, 3, 2))) {
				if (level.getBlockState(offset).isSolid()) {
					solidCount++;
				}
			}
			ctx.isEnclosedSpace = solidCount >= 12;
		}

				Vec3 awayVec = target.position().subtract(self.position()).normalize();
		Vec3 tVel = target.getDeltaMovement();
		double dotAway = tVel.horizontalDistanceSqr() > 0.001 ? tVel.normalize().dot(awayVec) : 0.0;
		ctx.isFleeing = dotAway > 0.4 && ctx.dist > 3.0;
		ctx.isAggressive = (target instanceof LivingEntity livTarget && livTarget.swinging) || (dotAway < -0.4 && ctx.dist <= 4.5);
		ctx.isIncomingDamage = (ctx.expectedIncomingDamage > 0.0 || ctx.isJumpCritIncoming);

		return ctx;
	}

	public static class AbilityInfo {
		public final String id;
		public final String unlockFlag;
		public final String cooldownKey;
		public final double range;
		public final double damage;
		public final Set<String> tags;

		public double horizontalReach;
		public double verticalReach;
		public double forwardMovement;
		public double upwardMovement;
		public double downwardMovement;
		public String movementType = "GROUND";
		public double maximumTravelDistance;
		public double travelSpeed = 1.0;
		public boolean canCrossWater = false;
		public boolean canCrossGaps = false;
		public boolean canTraverseAir = false;
		public boolean requiresGround = false;
		public boolean requiresLineOfSight = true;

		public double trackingStrength = 1.0;
		public double predictionStrength = 1.0;
		public double homingStrength = 0.0;
		public boolean lockOnDuringMove = false;
		public boolean usableAgainstAir = true;
		public boolean usableAgainstGround = true;
		public boolean usableAgainstWater = true;
		public boolean usableAgainstBoats = true;
		public boolean usableAgainstMountedTargets = true;

		public double antiAirRating = 0.0;
		public double gapCloserRating = 0.0;
		public double crowdControlRating = 0.0;
		public double burstRating = 0.0;
		public double sustainedRating = 0.0;
		public double escapePunishRating = 0.0;
		public double comboStarterRating = 0.0;
		public double comboExtenderRating = 0.0;
		public double finisherRating = 0.0;
		public double shieldBreakRating = 0.0;
		public double interruptionRating = 0.0;
		public double zoningRating = 0.0;

		public double startupTicks = 10.0;
		public double activeTicks = 10.0;
		public double recoveryTicks = 10.0;
		public double cooldownWeight = 1.0;
		public double commitment = 1.0;
		public double missPunishment = 1.0;
		public double interruptResistance = 1.0;

		public double knockbackPower = 1.0;
		public boolean launchesTarget = false;
		public boolean launchesSelf = false;
		public boolean causesAirborneState = false;
		public boolean slamsTarget = false;
		public boolean createsAOE = false;
		public double preferredMinimumRange = 0.0;
		public double preferredMaximumRange = 16.0;

		public double closesDistance = 0.0;
		public double gainsAltitude = 0.0;
		public double descendsQuickly = 0.0;
		public double interceptsMovingTargets = 0.0;
		public double punishesRetreat = 0.0;
		public double antiEscape = 0.0;
		public double antiFlight = 0.0;
		public double antiShield = 0.0;
		public double antiGroup = 0.0;
		public double forcesMovement = 0.0;
		public double keepsPressure = 0.0;
		public double opensCombo = 0.0;
		public double extendsCombo = 0.0;
		public double endsCombo = 0.0;

		public Set<String> affordances = new HashSet<>();

		public AbilityInfo(String id, String unlockFlag, String cooldownKey, double range, double damage, String... tags) {
			this.id = id;
			this.unlockFlag = unlockFlag;
			this.cooldownKey = cooldownKey;
			this.range = range;
			this.damage = damage;
			this.tags = new HashSet<>(java.util.Arrays.asList(tags));
			this.horizontalReach = range;
			this.verticalReach = range * 0.5;
			this.maximumTravelDistance = range;
			this.preferredMaximumRange = range;
		}

		public boolean hasTag(String tag) {
			return tags.contains(tag);
		}

		public boolean hasAffordance(String affordance) {
			return affordances.contains(affordance);
		}

		public AbilityInfo mobility(double hReach, double vReach, double fwdMove, double upMove, double downMove, String type, double speed, boolean crossWater, boolean crossGaps, boolean traverseAir) {
			this.horizontalReach = hReach;
			this.verticalReach = vReach;
			this.forwardMovement = fwdMove;
			this.upwardMovement = upMove;
			this.downwardMovement = downMove;
			this.movementType = type;
			this.travelSpeed = speed;
			this.canCrossWater = crossWater;
			this.canCrossGaps = crossGaps;
			this.canTraverseAir = traverseAir;
			this.maximumTravelDistance = Math.max(hReach, vReach);
			return this;
		}

		public AbilityInfo targeting(double track, double predict, double homing, boolean lockOn, boolean air, boolean ground, boolean water, boolean boats, boolean mounted) {
			this.trackingStrength = track;
			this.predictionStrength = predict;
			this.homingStrength = homing;
			this.lockOnDuringMove = lockOn;
			this.usableAgainstAir = air;
			this.usableAgainstGround = ground;
			this.usableAgainstWater = water;
			this.usableAgainstBoats = boats;
			this.usableAgainstMountedTargets = mounted;
			return this;
		}

		public AbilityInfo ratings(double antiAir, double gapCloser, double cc, double burst, double sustained, double escapePunish, double comboStart, double comboExt, double finisher, double shieldBreak, double interrupt, double zoning) {
			this.antiAirRating = antiAir;
			this.gapCloserRating = gapCloser;
			this.crowdControlRating = cc;
			this.burstRating = burst;
			this.sustainedRating = sustained;
			this.escapePunishRating = escapePunish;
			this.comboStarterRating = comboStart;
			this.comboExtenderRating = comboExt;
			this.finisherRating = finisher;
			this.shieldBreakRating = shieldBreak;
			this.interruptionRating = interrupt;
			this.zoningRating = zoning;
			return this;
		}

		public AbilityInfo risk(double startup, double active, double recovery, double cdWeight, double commitment, double missPunish, double intResist) {
			this.startupTicks = startup;
			this.activeTicks = active;
			this.recoveryTicks = recovery;
			this.cooldownWeight = cdWeight;
			this.commitment = commitment;
			this.missPunishment = missPunish;
			this.interruptResistance = intResist;
			return this;
		}

		public AbilityInfo control(double kbPower, boolean launchTarget, boolean launchSelf, boolean airborneState, boolean slamTarget, boolean aoe, double minR, double maxR) {
			this.knockbackPower = kbPower;
			this.launchesTarget = launchTarget;
			this.launchesSelf = launchSelf;
			this.causesAirborneState = airborneState;
			this.slamsTarget = slamTarget;
			this.createsAOE = aoe;
			this.preferredMinimumRange = minR;
			this.preferredMaximumRange = maxR;
			return this;
		}

		public AbilityInfo tactical(double closesDist, double gainsAlt, double descendsQuick, double interceptMoving, double punishRetreat, double antiEsc, double antiFlt, double antiShld, double antiGrp, double forcesMove, double pressure, double openC, double extC, double endC) {
			this.closesDistance = closesDist;
			this.gainsAltitude = gainsAlt;
			this.descendsQuickly = descendsQuick;
			this.interceptsMovingTargets = interceptMoving;
			this.punishesRetreat = punishRetreat;
			this.antiEscape = antiEsc;
			this.antiFlight = antiFlt;
			this.antiShield = antiShld;
			this.antiGroup = antiGrp;
			this.forcesMovement = forcesMove;
			this.keepsPressure = pressure;
			this.opensCombo = openC;
			this.extendsCombo = extC;
			this.endsCombo = endC;
			return this;
		}

		public AbilityInfo affordances(String... affs) {
			for (String a : affs) {
				this.affordances.add(a);
			}
			return this;
		}
	}

	public static final List<AbilityInfo> ABILITY_REGISTRY = new ArrayList<>();
	static {
		ABILITY_REGISTRY.add(new AbilityInfo("sonic_boom", "unlocked_sonic_boom", "sentinel_warden_sonic_cooldown", 24.0, SONIC_BOOM_DMG, "ranged", "burst", "anti-air", "control")
			.mobility(24.0, 12.0, 24.0, 0.0, 0.0, "PROJECTILE", 3.0, true, true, true)
			.targeting(2.0, 2.0, 0.5, true, true, true, true, true, true)
			.ratings(2.8, 0.0, 2.0, 2.5, 0.5, 2.0, 0.0, 0.5, 1.5, 0.5, 2.5, 2.5)
			.tactical(0.0, 0.0, 0.0, 2.2, 2.0, 2.0, 2.5, 0.0, 0.0, 1.5, 1.0, 0.0, 0.0, 0.0)
			.affordances("ranged_stagger", "anti_flight_deny", "knockback"));

		ABILITY_REGISTRY.add(new AbilityInfo("omni_sonic_boom", "unlocked_sonic_boom", "sentinel_omni_sonic_cooldown", 6.0, SONIC_BOOM_DMG, "aoe", "burst", "control")
			.mobility(6.0, 6.0, 0.0, 0.0, 0.0, "AOE", 1.0, true, true, true)
			.ratings(1.5, 0.0, 3.0, 2.8, 0.0, 0.0, 0.0, 0.0, 1.5, 1.0, 3.0, 3.0)
			.control(2.5, true, false, true, false, true, 0.0, 6.0)
			.tactical(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 2.5, 1.5, 0.0, 0.0, 0.0)
			.affordances("shockwave", "area_denial", "multi_target_stagger"));

		ABILITY_REGISTRY.add(new AbilityInfo("solar_beam", "unlocked_solar_beam", K_SOLAR_CD, 32.0, SOLAR_BEAM_DMG_BASE, "ranged", "sustained", "burst")
			.mobility(32.0, 16.0, 32.0, 0.0, 0.0, "PROJECTILE", 4.0, true, true, true)
			.ratings(1.8, 0.0, 1.0, 2.8, 2.5, 2.2, 0.0, 0.0, 2.0, 1.5, 1.5, 2.8)
			.tactical(0.0, 0.0, 0.0, 2.0, 2.5, 2.2, 1.8, 1.0, 0.0, 1.0, 2.5, 0.0, 0.0, 0.0)
			.affordances("sustained_damage", "zoning_fire", "heat_buildup"));

		ABILITY_REGISTRY.add(new AbilityInfo("cryo_beam", "unlocked_cryo_beam", K_SOLAR_CD, 32.0, CRYO_BEAM_DMG_BASE, "ranged", "sustained", "control")
			.mobility(32.0, 16.0, 32.0, 0.0, 0.0, "PROJECTILE", 4.0, true, true, true)
			.ratings(1.8, 0.0, 2.8, 1.8, 2.8, 2.2, 0.0, 0.0, 1.5, 1.0, 2.0, 3.0)
			.tactical(0.0, 0.0, 0.0, 2.0, 2.5, 2.5, 1.8, 1.0, 0.0, 1.0, 2.5, 0.0, 0.0, 0.0)
			.affordances("slow_debuff", "freeze_lock", "zoning_ice"));

		ABILITY_REGISTRY.add(new AbilityInfo("wither_skulls", "unlocked_wither_skulls", "sentinel_wither_skull_cd", 32.0, 12.0, "ranged", "burst")
			.mobility(32.0, 16.0, 32.0, 0.0, 0.0, "PROJECTILE", 2.0, true, true, true)
			.ratings(1.5, 0.0, 1.5, 2.2, 1.0, 1.8, 0.0, 0.0, 1.0, 0.5, 1.0, 2.0)
			.affordances("wither_debuff", "ranged_harass"));

		ABILITY_REGISTRY.add(new AbilityInfo("telekinesis", "unlocked_telekinesis", K_TK_CD, 16.0, COMBO_TK_SLAM_DMG, "control", "ranged", "gap-closer")
			.mobility(16.0, 10.0, 16.0, 5.0, 5.0, "TELEPORT", 3.0, true, true, true)
			.targeting(2.5, 2.5, 2.0, true, true, true, true, true, true)
			.ratings(2.0, 2.8, 3.0, 1.8, 0.0, 2.8, 2.0, 1.5, 1.0, 2.0, 3.0, 2.0)
			.tactical(2.8, 1.5, 1.5, 2.5, 2.8, 2.8, 2.0, 2.0, 1.0, 3.0, 2.0, 2.0, 1.5, 0.0)
			.affordances("pull_target", "disrupt_boat", "ground_slam", "juggle_opportunity"));

		ABILITY_REGISTRY.add(new AbilityInfo("grapple", "unlocked_grapple", K_GRAPPLE_CD, 12.0, MUTANT_DNA_GRAPPLE_DMG, "drain", "control", "gap-closer")
			.mobility(12.0, 8.0, 12.0, 0.0, 0.0, "DASH", 2.5, true, true, true)
			.ratings(1.0, 2.8, 2.5, 1.5, 1.5, 2.5, 2.2, 1.5, 1.0, 1.5, 2.5, 1.0)
			.tactical(2.8, 0.0, 0.0, 2.2, 2.5, 2.5, 1.0, 1.5, 0.0, 2.5, 2.0, 2.2, 1.5, 0.0)
			.affordances("pull_self_to_target", "choke_grab", "life_drain"));

		ABILITY_REGISTRY.add(new AbilityInfo("overhead_combo", "unlocked_overhead_combo", "sentinel_overhead_cooldown", 4.5, COMBO_OVERHEAD_SLAM_DMG, "burst", "anti-shield", "control")
			.mobility(4.5, 8.0, 2.0, 0.0, 8.0, "LEAP", 2.0, false, true, true)
			.ratings(1.5, 1.0, 2.5, 2.8, 0.0, 1.5, 1.0, 2.5, 2.8, 3.0, 2.5, 1.0)
			.control(2.0, false, false, false, true, true, 0.0, 4.5)
			.tactical(1.0, 0.0, 3.0, 1.5, 1.5, 1.5, 1.5, 3.0, 1.0, 2.0, 2.0, 1.0, 2.5, 2.8)
			.affordances("ground_slam", "shockwave", "target_grounded", "area_denial", "target_stunned"));

		ABILITY_REGISTRY.add(new AbilityInfo("dropkick_combo", "unlocked_dropkick_combo", "sentinel_dropkick_cooldown", 6.0, COMBO_JUDGMENT_KICK_DMG, "gap-closer", "burst", "control")
			.mobility(200.0, 20.0, 200.0, 5.0, 5.0, "DASH", 4.5, true, true, true)
			.targeting(3.0, 3.0, 2.5, true, true, true, true, true, true)
			.ratings(2.0, 3.0, 2.5, 3.0, 0.0, 3.0, 2.0, 2.0, 2.5, 2.0, 2.5, 1.0)
			.control(3.0, true, false, false, false, false, 4.0, 200.0)
			.tactical(3.0, 0.5, 0.5, 3.0, 3.0, 3.0, 2.0, 1.5, 0.0, 2.5, 2.5, 2.0, 2.0, 2.2)
			.affordances("large_displacement", "knockback", "spacing", "pursuit_opportunity"));

		ABILITY_REGISTRY.add(new AbilityInfo("minos_combo", "unlocked_minos_combo", "sentinel_minos_cooldown", 5.0, COMBO_SEISMIC_SLAM_DMG, "burst", "sustained")
			.mobility(5.0, 3.0, 5.0, 0.0, 0.0, "DASH", 2.0, false, true, false)
			.ratings(0.5, 1.8, 2.0, 2.8, 2.0, 1.5, 2.0, 2.5, 2.0, 1.5, 2.0, 1.0)
			.control(2.0, false, false, false, true, true, 0.0, 5.0)
			.affordances("seismic_shockwave", "combo_chain", "ground_pressure"));

		ABILITY_REGISTRY.add(new AbilityInfo("die_rider_kick", "unlocked_knockback_rider_combo", "sentinel_cc4_cd", 8.0, COMBO_DIE_RIDER_KICK_DMG, "gap-closer", "burst", "anti-air")
			.mobility(30.0, 15.0, 30.0, 8.0, 0.0, "LEAP", 3.5, true, true, true)
			.targeting(2.8, 2.8, 1.8, true, true, true, true, true, true)
			.ratings(2.8, 3.0, 2.0, 3.0, 0.0, 2.8, 1.5, 2.0, 2.8, 1.5, 2.5, 1.0)
			.tactical(3.0, 2.2, 0.0, 2.8, 2.8, 2.8, 2.8, 1.0, 0.0, 2.0, 2.5, 1.5, 2.0, 2.8)
			.affordances("aerial_pursuit", "knockback", "juggle_opportunity", "supersonic_kick"));

		ABILITY_REGISTRY.add(new AbilityInfo("triple_threat_combo", "unlocked_triple_threat_combo", "sentinel_cc1_cd", 4.5, MELEE_PUNCH_DAMAGE * 3.0, "sustained", "burst")
			.mobility(4.5, 2.5, 3.0, 0.0, 0.0, "GROUND", 1.0, false, false, false)
			.ratings(0.5, 1.0, 1.5, 2.5, 2.8, 1.0, 2.8, 2.8, 2.0, 1.8, 2.0, 0.5)
			.tactical(1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.5, 1.5, 0.0, 1.5, 2.8, 2.8, 2.8, 1.5)
			.affordances("triple_strike", "melee_pressure", "combo_starter"));

		ABILITY_REGISTRY.add(new AbilityInfo("high_sky_slam_combo", "unlocked_high_sky_slam_combo", "sentinel_cc2_cd", 6.0, UPPERCUT_DAMAGE + COMBO_SEISMIC_SLAM_DMG, "anti-air", "aoe", "control")
			.mobility(6.0, 12.0, 4.0, 12.0, 12.0, "LEAP", 3.0, true, true, true)
			.targeting(2.5, 2.5, 1.0, true, true, true, true, true, true)
			.ratings(3.0, 2.2, 2.8, 2.5, 1.0, 2.2, 2.0, 2.5, 2.2, 1.5, 2.8, 1.5)
			.control(2.5, true, true, true, true, true, 0.0, 6.0)
			.tactical(2.0, 3.0, 2.5, 2.8, 2.0, 2.2, 3.0, 1.0, 2.0, 2.5, 2.0, 2.0, 2.5, 2.2)
			.affordances("self_airborne", "target_airborne", "juggle_opportunity", "aerial_followup_window"));

		ABILITY_REGISTRY.add(new AbilityInfo("knockback_dropkick_combo", "unlocked_knockback_dropkick_combo", "sentinel_cc3_cd", 6.0, COMBO_JUDGMENT_KICK_DMG, "gap-closer", "control")
			.mobility(200.0, 20.0, 200.0, 5.0, 5.0, "DASH", 4.5, true, true, true)
			.targeting(3.0, 3.0, 2.5, true, true, true, true, true, true)
			.ratings(2.0, 3.0, 2.5, 2.8, 0.0, 3.0, 1.5, 2.0, 2.5, 2.0, 2.5, 1.0)
			.control(3.0, true, false, false, false, false, 4.0, 200.0)
			.tactical(3.0, 0.5, 0.5, 3.0, 3.0, 3.0, 2.0, 1.5, 0.0, 2.5, 2.5, 1.5, 2.0, 2.2)
			.affordances("large_displacement", "knockback", "spacing", "pursuit_opportunity"));

		ABILITY_REGISTRY.add(new AbilityInfo("heavenly_repentance_plus", "unlocked_heavenly_repentance_plus", "sentinel_cc5_cd", 5.0, COMBO_DIE_RIDER_KICK_DMG, "burst", "aoe", "control")
			.mobility(8.0, 8.0, 8.0, 5.0, 5.0, "LEAP", 3.0, true, true, true)
			.ratings(2.2, 2.5, 3.0, 3.0, 1.0, 2.5, 2.0, 2.5, 2.8, 2.0, 2.8, 2.0)
			.control(2.8, true, false, true, true, true, 0.0, 8.0)
			.affordances("divine_explosion", "massive_knockback", "shockwave", "finisher_slam"));

		ABILITY_REGISTRY.add(new AbilityInfo("armor_rip", "unlocked_armor_rip", "rot_armor_rip_cooldown", ARMOR_RIP_TRIGGER_DISTANCE, CHOKE_DAMAGE, "anti-shield", "control", "drain")
			.mobility(3.5, 2.0, 3.5, 0.0, 0.0, "GROUND", 1.0, false, false, false)
			.ratings(0.0, 1.0, 3.0, 2.0, 2.5, 1.5, 1.0, 2.0, 1.5, 3.0, 3.0, 0.5)
			.tactical(1.0, 0.0, 0.0, 1.0, 1.5, 2.0, 0.0, 3.0, 0.0, 2.5, 2.5, 1.0, 2.0, 1.5)
			.affordances("armor_destruction", "choke_grab", "protection_strip"));

		ABILITY_REGISTRY.add(new AbilityInfo("block", "unlocked_block", "rot_block_cooldown", 5.5, 0.0, "control")
			.ratings(0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
			.affordances("damage_reduction", "parry_window"));
	}

	public static List<AbilityInfo> getAvailableAbilities(Entity self) {
		List<AbilityInfo> available = new ArrayList<>();
		if (self == null) return available;
		for (AbilityInfo ability : ABILITY_REGISTRY) {
			boolean unlocked = false;
			if ("unlocked_armor_rip".equals(ability.unlockFlag)) {
				unlocked = ENABLE_ARMOR_RIP && getRotPersistentBoolean(self, "unlocked_armor_rip", true);
			} else if ("unlocked_block".equals(ability.unlockFlag)) {
				unlocked = ENABLE_BLOCKING && getRotPersistentBoolean(self, "unlocked_block", true);
			} else if (ability.unlockFlag != null) {
				unlocked = getB(self, ability.unlockFlag);
			} else {
				unlocked = true;
			}

			if (!unlocked) continue;

			if (ability.cooldownKey != null) {
				double cd = getD(self, ability.cooldownKey);
				if (cd > 0.0) continue;
			}
			available.add(ability);
		}
		return available;
	}

	public static void recordAttack(Entity self, String attackType) {
		String last1 = getS(self, "sentinel_mem_1");
		String last2 = getS(self, "sentinel_mem_2");
		putS(self, "sentinel_mem_3", last2);
		putS(self, "sentinel_mem_2", last1);
		putS(self, "sentinel_mem_1", attackType);

		String history = getS(self, "recent_attack_history");
		List<String> list = new ArrayList<>();
		if (!history.isEmpty()) {
			for (String s : history.split(",")) {
				if (!s.trim().isEmpty()) list.add(s.trim());
			}
		}
		list.add(attackType);
		while (list.size() > 10) {
			list.remove(0);
		}
		putS(self, "recent_attack_history", String.join(",", list));
	}

	public static double getTelegraphJitter(Entity entity, String key, double baseTick, double minOffset, double maxOffset) {
		if (entity == null) return baseTick;
		long uuidBits = entity.getUUID().getLeastSignificantBits();
		double castCount = getD(entity, key + "_cast_instance");
		double h = Math.abs((uuidBits ^ Double.doubleToRawLongBits(castCount)) % 1000) / 1000.0;
		double offset = minOffset + h * (maxOffset - minOffset);
		return Math.round(baseTick + offset);
	}

	public static double getDynamicRangeThreshold(Entity entity, String key, double baseRange, double variance) {
		if (entity == null) return baseRange;
		long uuidBits = entity.getUUID().getLeastSignificantBits();
		double h = Math.abs((uuidBits ^ key.hashCode()) % 1000) / 1000.0;
		return baseRange + (h - 0.5) * 2.0 * variance;
	}

	public static double getEffectiveCombatTicks(Entity entity) {
		if (entity == null) return 0.0;
		double rawTicks = getD(entity, "sentinel_combat_ticks");
		double playerDps = getD(entity, "sentinel_player_dps_window");
		double misses = getD(entity, "sentinel_heavy_punch_misses");
		double blockSuccess = getRotPersistentDouble(entity, "sentinel_target_block_rate", 0.2);
		double dpsFactor = Math.min(2.5, 1.0 + (playerDps / 10.0) * 0.5);
		double skillFactor = Math.min(2.0, 1.0 + (misses * 0.08) + (blockSuccess * 0.5));
		return rawTicks * dpsFactor * skillFactor;
	}

	public static AbilityInfo getAbilityById(String id) {
		if (id == null) return null;
		for (AbilityInfo info : ABILITY_REGISTRY) {
			if (id.equals(info.id)) return info;
		}
		return null;
	}

	public static boolean evaluateComboTriggerChance(Entity self, Entity target, CombatContext ctx) {
		if (self == null || target == null) return false;
		boolean totemActive = getB(self, "sentinel_totem_active");
		double baseChance = totemActive ? 0.85 : 0.28;
		double multiplier = 1.0;
		double dist = self.distanceTo(target);
		if (dist <= 4.0) multiplier *= 1.4;
		if (ctx != null) {
			if (ctx.isAirborne) multiplier *= 1.3;
			if (ctx.isBlocking) multiplier *= 1.5;
			if (ctx.isCornered) multiplier *= 1.6;
			if (ctx.isHealing) multiplier *= 1.4;
		}
		double threat = getD(self, "sentinel_threat_score");
		if (threat > 50.0) multiplier *= 1.3;
		double finalChance = Math.min(0.95, baseChance * multiplier);
		return Math.random() < finalChance;
	}

	public enum TargetIntent {
		ENGAGING,
		ESCAPING,
		AERIAL_ADVANTAGE,
		REPOSITIONING,
		HEALING,
		RANGED_ATTACK,
		BAITING,
		CREATING_DISTANCE,
		AGGRESSIVE_MELEE,
		TURTLING,
		SPACING_RESET,
		TERRAIN_ABUSE
	}

	public static TargetIntent inferTargetIntent(Entity self, Entity target) {
		if (self == null || target == null) return TargetIntent.ENGAGING;

		Vec3 targetVel = target.getDeltaMovement();
		double speedSq = targetVel.horizontalDistanceSqr();
		double dist = self.distanceTo(target);

		EntityObservation obs = (target instanceof LivingEntity liv) ? UniversalCombatPredictionEngine.getObservation(liv.getUUID()) : null;

		if (target.isPassenger() && target.getVehicle() != null) {
			String vType = target.getVehicle().getType().toString().toLowerCase();
			if (vType.contains("boat") || vType.contains("horse") || vType.contains("minecart")) {
				return TargetIntent.ESCAPING;
			}
		}

		if (target instanceof LivingEntity liv) {
			if (liv.isFallFlying() || (!liv.onGround() && target.getY() > self.getY() + 2.5 && !liv.isInWater())) {
				return TargetIntent.AERIAL_ADVANTAGE;
			}
		}

		if (target.level() instanceof ServerLevel sLvl) {
			AABB searchBox = target.getBoundingBox().inflate(48.0);
			java.util.List<net.minecraft.world.entity.projectile.ThrownEnderpearl> pearls = sLvl.getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownEnderpearl.class, searchBox);
			for (net.minecraft.world.entity.projectile.ThrownEnderpearl p : pearls) {
				if (p.isAlive() && (p.getOwner() == null || p.getOwner().equals(target))) {
					return TargetIntent.REPOSITIONING;
				}
			}
		}

		if (target instanceof LivingEntity liv) {
			if (liv.isUsingItem()) {
				ItemStack useItem = liv.getUseItem();
				if (useItem.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
					|| useItem.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE)
					|| useItem.is(net.minecraft.world.item.Items.MILK_BUCKET)
					|| useItem.getItem() instanceof net.minecraft.world.item.PotionItem) {
					return TargetIntent.HEALING;
				}
			}

			ItemStack mainHand = liv.getMainHandItem();
			if (mainHand.getItem() instanceof net.minecraft.world.item.BowItem 
				|| mainHand.getItem() instanceof net.minecraft.world.item.CrossbowItem
				|| mainHand.getItem() instanceof net.minecraft.world.item.TridentItem) {
				if (dist > 5.0) return TargetIntent.RANGED_ATTACK;
			}

			if (liv.isBlocking()) {
				if (obs != null && obs.isShieldBaiting) {
					return TargetIntent.BAITING;
				}
				return TargetIntent.TURTLING;
			}

			if (obs != null) {
				if (obs.isConsumingHeal) return TargetIntent.HEALING;
				if (obs.isSpacingFeint || obs.isReachTesting) return TargetIntent.SPACING_RESET;
				if (obs.isCritBaiting || obs.isShieldBaiting || obs.isMovementBaiting) return TargetIntent.BAITING;
				if (obs.isDeliberateLOSBreak || obs.isVerticalPillaring || obs.isWaterTrapping) return TargetIntent.TERRAIN_ABUSE;
			}

			boolean highElevation = !liv.onGround() && liv.getY() > self.getY() + 2.5;
			boolean inWater = liv.isInWater();
			boolean brokenLos = self instanceof LivingEntity selfLiv && !selfLiv.hasLineOfSight(liv);
			if (highElevation || inWater || (brokenLos && dist < 12.0)) {
				return TargetIntent.TERRAIN_ABUSE;
			}

			boolean lowHealth = liv.getHealth() < liv.getMaxHealth() * 0.4f;
			Vec3 awayVector = target.position().subtract(self.position()).normalize();
			double dotAway = targetVel.normalize().dot(awayVector);
			if (dist <= 4.2 && (liv.swinging || dotAway < -0.35 || (obs != null && (obs.isWpingDetected || obs.isStappingDetected)))) {
				return TargetIntent.AGGRESSIVE_MELEE;
			}
			if (liv.isSprinting() && dotAway > 0.5 && dist > 4.0) {
				return TargetIntent.ESCAPING;
			}
			if (lowHealth && speedSq > 0.02 && dotAway > 0.3) {
				return TargetIntent.ESCAPING;
			}
			if (dist > 8.0 && liv.isSprinting() && dotAway > 0.4) {
				return TargetIntent.CREATING_DISTANCE;
			}
		}

		return TargetIntent.ENGAGING;
	}

	public static void adaptCapabilitiesToIntent(Entity self, TargetIntent intent) {
		if (self == null || intent == null) return;
		if (intent == TargetIntent.ESCAPING) {
			putB(self, "unlocked_dropkick_combo", true);
			putB(self, "unlocked_knockback_dropkick_combo", true);
			putB(self, "unlocked_telekinesis", true);
			putB(self, "unlocked_teleportation", true);
		} else if (intent == TargetIntent.AERIAL_ADVANTAGE) {
			putB(self, "unlocked_high_sky_slam_combo", true);
			putB(self, "unlocked_knockback_rider_combo", true);
			putB(self, "unlocked_sonic_scream", true);
			putB(self, "unlocked_teleportation", true);
		} else if (intent == TargetIntent.REPOSITIONING) {
			putB(self, "unlocked_teleportation", true);
			putB(self, "unlocked_knockback_rider_combo", true);
			putB(self, "unlocked_dropkick_combo", true);
		} else if (intent == TargetIntent.AGGRESSIVE_MELEE) {
			putB(self, "unlocked_parry", true);
			putB(self, "unlocked_counter", true);
			putB(self, "unlocked_block", true);
		} else if (intent == TargetIntent.TURTLING) {
			putB(self, "unlocked_high_sky_slam_combo", true);
			putB(self, "unlocked_overhead_slam", true);
			putB(self, "unlocked_telekinesis", true);
		} else if (intent == TargetIntent.SPACING_RESET || intent == TargetIntent.BAITING) {
			putB(self, "unlocked_parry", true);
			putB(self, "unlocked_counter", true);
			putB(self, "unlocked_telekinesis", true);
			putB(self, "unlocked_teleportation", true);
		} else if (intent == TargetIntent.TERRAIN_ABUSE) {
			putB(self, "unlocked_sky_warp_slam", true);
			putB(self, "unlocked_sonic_boom", true);
			putB(self, "unlocked_telekinesis", true);
			putB(self, "unlocked_high_sky_slam_combo", true);
		}
	}

	private static void cancelActiveCombosAndAbilities(Entity entity) {
		if (entity == null) return;
		clearDoubles(entity,
			"sentinel_minos_ticks", "sentinel_minos_stage", "sentinel_minos_wait_ticks",
			"rot_overhead_ticks", "sentinel_slam_ticks", "sentinel_slam_phase",
			"sentinel_die_kick_ticks", "sentinel_die_kick_phase",
			"sentinel_cc1_ticks", "sentinel_cc1_stage", "sentinel_cc2_ticks", "sentinel_cc2_stage",
			"sentinel_cc3_ticks", "sentinel_cc3_stage", "sentinel_cc4_ticks", "sentinel_cc4_stage",
			"sentinel_cc5_ticks", "sentinel_cc5_stage",
			"sentinel_solar_fire_ticks", "sentinel_solar_charge_ticks",
			"sentinel_cryo_fire_ticks", "sentinel_cryo_charge_ticks",
			"sentinel_grapple_ticks", "sentinel_tk_ticks", "sentinel_sonic_ticks",
			"sentinel_laser_closing_ticks", "sentinel_sky_warp_slam_ticks",
			"sentinel_judgment_ticks", "sentinel_rider_hold_ticks",
			"sentinel_omni_sonic_charge_ticks", "sentinel_sonic_scream_ticks", "rot_armor_rip_ticks"
		);
		clearBooleans(entity, "is_armor_ripping", "is_uppercutting", "sentinel_said_prepare_thyself");
	}

	private static boolean handleTotemStealing(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		Player p = null;
		if (combatTarget instanceof Player playerTarget) {
			p = playerTarget;
		} else if (entity instanceof Mob mob && mob.getTarget() instanceof Player playerTarget) {
			p = playerTarget;
		}
		if (entity == null || p == null) {
			return false;
		}

		int stolenCount = getI(entity, "sentinel_stolen_totem_count");
		if (getB(entity, "sentinel_totem_stolen") || getB(entity, "sentinel_totem_active")) {
			if (stolenCount == 0) stolenCount = 1;
		}
		if (stolenCount >= 3) {
			putD(entity, "sentinel_totem_steal_timer", 0.0);
			return false;
		}

		double inspectTicks = getD(entity, "sentinel_totem_inspect_ticks");
		double stealTimer = getD(entity, "sentinel_totem_steal_timer");
		double combatTicks = getD(entity, "sentinel_combat_ticks");
		net.minecraft.world.item.ItemStack totemStack = net.minecraft.world.item.ItemStack.EMPTY;
		boolean isInfinityTotem = false;
		boolean isHeldTotem = false;
		int currentTotemSlot = -99;

		net.minecraft.world.item.ItemStack mainHand = p.getMainHandItem();
		net.minecraft.world.item.ItemStack offHand = p.getOffhandItem();

		if (!mainHand.isEmpty()) {
			String itemId = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();
			if (itemId.equals("avaritia:infinity_totem")) {
				totemStack = mainHand;
				isInfinityTotem = true;
				isHeldTotem = true;
				currentTotemSlot = -1;
			} else if (mainHand.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING) {
				totemStack = mainHand;
				isHeldTotem = true;
				currentTotemSlot = -1;
			}
		}

		if (totemStack.isEmpty() && !offHand.isEmpty()) {
			String itemId = BuiltInRegistries.ITEM.getKey(offHand.getItem()).toString();
			if (itemId.equals("avaritia:infinity_totem")) {
				totemStack = offHand;
				isInfinityTotem = true;
				isHeldTotem = true;
				currentTotemSlot = -2;
			} else if (offHand.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING) {
				totemStack = offHand;
				isHeldTotem = true;
				currentTotemSlot = -2;
			}
		}

		boolean totemLearned = getB(entity, "sentinel_totem_learned");

		if (isHeldTotem && !totemLearned) {
			double observeProgress = getD(entity, "sentinel_totem_observe_progress");
			observeProgress += 1.0;
			putD(entity, "sentinel_totem_observe_progress", observeProgress);
			if (observeProgress >= 60.0) {
				totemLearned = true;
				putB(entity, "sentinel_totem_learned", true);
				if (world instanceof ServerLevel level) {
					playHostileSound(level, entity, "entity.warden.sniff", 1.2F, 0.6F);
					RotDialoguesProcedure.sendTotemObserved(p);
				}
			}
		}

		if (!isHeldTotem || totemStack.isEmpty()) {
			putD(entity, "sentinel_totem_steal_timer", 0.0);
			putI(entity, "sentinel_last_totem_slot", -99);
			return false;
		}

		double awareness = getD(entity, "sentinel_totem_awareness");
		int lastTotemCount = getRotPersistentInt(entity, "sentinel_last_totem_count", -1);
		int currentTotemCount = 0;

		for (int slot = 0; slot < p.getInventory().getContainerSize(); slot++) {
			net.minecraft.world.item.ItemStack s = p.getInventory().getItem(slot);
			if (!s.isEmpty() && (s.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("avaritia:infinity_totem"))) {
				currentTotemCount += s.getCount();
			}
		}

		boolean justStoleThisTick = getB(entity, "sentinel_just_stole_totem");
		if (justStoleThisTick) {
			putB(entity, "sentinel_just_stole_totem", false);
		} else if (lastTotemCount >= 0 && currentTotemCount < lastTotemCount) {
			totemLearned = true;
			putB(entity, "sentinel_totem_learned", true);
			putD(entity, "sentinel_totem_awareness", 160.0);
			int popsWitnessed = getI(entity, "sentinel_totem_pops_witnessed") + (lastTotemCount - currentTotemCount);
			putI(entity, "sentinel_totem_pops_witnessed", popsWitnessed);
			UniversalCombatPredictionEngine.recordExterminationMilestone(entity, p, "TOTEM_POPPED", 1.0);
			if (world instanceof ServerLevel level) {
				playHostileSound(level, entity, "entity.warden.sniff", 1.5F, 0.5F);
				RotDialoguesProcedure.sendTotemPopped(p);
			}
		}

		putI(entity, "sentinel_last_totem_count", currentTotemCount);

		if (!totemStack.isEmpty() && isHeldTotem) {
			awareness += 1.0;
			putD(entity, "sentinel_totem_awareness", awareness);
		}

		if (isChannelingAbility(entity) || getB(entity, "is_armor_ripping")) {
			return false;
		}

		if (totemLearned) {
			int lastSlot = entity.getPersistentData().contains("sentinel_last_totem_slot") ? getRotPersistentInt(entity, "sentinel_last_totem_slot", -99) : -99;
			boolean slotSwapped = (lastSlot != -99 && lastSlot != currentTotemSlot);
			putI(entity, "sentinel_last_totem_slot", currentTotemSlot);

			double minTime = 10.0;
			double maxTime = 20.0;
			double reqCombatTicks = 0.0;
			double maxDist = 6.0;

			double targetStealTime = getD(entity, "sentinel_totem_target_steal_time");
			if (targetStealTime <= 0 || targetStealTime > maxTime) {
				double range = maxTime - minTime;
				if (range > 0) {
					targetStealTime = minTime + RandomSource.create().nextInt((int) range + 1);
				} else {
					targetStealTime = minTime;
				}
				putD(entity, "sentinel_totem_target_steal_time", targetStealTime);
			}

			if (entity.distanceTo(p) <= maxDist && combatTicks >= reqCombatTicks) {
				double increment = 3.0;
				if (slotSwapped) {
					increment += 1.5;
				}
				float hpRatio = p.getHealth() / p.getMaxHealth();
				if (hpRatio < 0.25f) {
					increment += 2.0;
				} else if (hpRatio < 0.5f) {
					increment += 1.0;
				}

				stealTimer += increment;
				putD(entity, "sentinel_totem_steal_timer", stealTimer);
				if (stealTimer >= targetStealTime) {
					cancelActiveCombosAndAbilities(entity);
					stopHostileSound(world, entity, "the_backwoods:fractus_laser", 256.0);

					totemStack.shrink(1);
					stolenCount++;
					putI(entity, "sentinel_stolen_totem_count", stolenCount);
					putB(entity, "sentinel_is_infinity_totem", isInfinityTotem);
					putB(entity, "sentinel_totem_stolen", true);
					putB(entity, "sentinel_just_stole_totem", true);
					putB(entity, "sentinel_totem_learned", true);

					if (entity instanceof LivingEntity living) {
						net.minecraft.world.item.Item infinityTotemItem = isInfinityTotem ? BuiltInRegistries.ITEM.get(ResourceLocation.parse("avaritia:infinity_totem")) : null;
						net.minecraft.world.item.ItemStack stackToHold = (infinityTotemItem != null) ? new net.minecraft.world.item.ItemStack(infinityTotemItem) : new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);

						if (getD(entity, "sentinel_totem_inspect_ticks") > 0) {
							living.setItemInHand(InteractionHand.OFF_HAND, stackToHold);
							putB(entity, "sentinel_has_queued_totem", true);
						} else {
							living.setItemInHand(InteractionHand.MAIN_HAND, stackToHold);
							putD(entity, "sentinel_totem_inspect_ticks", 180);
						}
					}
					clearDoubles(entity, "sentinel_solar_charge_ticks", "sentinel_solar_fire_ticks", "sentinel_cryo_charge_ticks", "sentinel_cryo_fire_ticks");
					if (world instanceof ServerLevel level) {
						playHostileSound(level, entity, "block.bell.resonate", 1.5F, 0.6F);
						if (!isInfinityTotem) {
							RotDialoguesProcedure.sendTotemStolen(p);
						}
					}
					handlePassengerAndGrowth(entity);
					return true;
				}
			} else {
				if (stealTimer > 0) {
					putD(entity, "sentinel_totem_steal_timer", Math.max(0.0, stealTimer - 0.5));
				}
			}
		}

		return false;
	}

	private static boolean checkAndSeekDroppedTotems(LevelAccessor world, Entity entity) {
		if (world == null || entity == null) return false;
		if (getB(entity, "sentinel_totem_active") || getB(entity, "sentinel_totem_stolen")) return false;
		if (getD(entity, "sentinel_totem_inspect_ticks") > 0) return false;
		if (isChannelingAbility(entity) || getB(entity, "is_armor_ripping")) return false;

		List<net.minecraft.world.entity.item.ItemEntity> items = world.getEntitiesOfClass(
			net.minecraft.world.entity.item.ItemEntity.class,
			AABB.ofSize(entity.position(), 96.0, 96.0, 96.0)
		);

		net.minecraft.world.entity.item.ItemEntity bestItem = null;
		boolean bestIsInfinity = false;
		double bestDistSq = Double.MAX_VALUE;

		for (net.minecraft.world.entity.item.ItemEntity itemEnt : items) {
			if (itemEnt == null || !itemEnt.isAlive() || itemEnt.getItem().isEmpty()) continue;
			net.minecraft.world.item.ItemStack stack = itemEnt.getItem();
			String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			boolean isInfinity = itemId.equals("avaritia:infinity_totem");
			boolean isVanillaTotem = stack.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING;

			if (!isInfinity && !isVanillaTotem) continue;

			if (isHazardousLocation(world, itemEnt.getX(), itemEnt.getY(), itemEnt.getZ())) {
				continue;
			}

			double distSq = entity.distanceToSqr(itemEnt);

			if (bestItem == null) {
				bestItem = itemEnt;
				bestIsInfinity = isInfinity;
				bestDistSq = distSq;
			} else {
				if (isInfinity && !bestIsInfinity) {
					bestItem = itemEnt;
					bestIsInfinity = true;
					bestDistSq = distSq;
				} else if (isInfinity == bestIsInfinity) {
					if (RandomSource.create().nextBoolean() || distSq < bestDistSq) {
						bestItem = itemEnt;
						bestIsInfinity = isInfinity;
						bestDistSq = distSq;
					}
				}
			}
		}

		if (bestItem != null) {
			putB(entity, "sentinel_totem_learned", true);
			double dist = entity.distanceTo(bestItem);

			if (dist <= 2.8) {
				bestItem.discard();
				putB(entity, "sentinel_is_infinity_totem", bestIsInfinity);
				putB(entity, "sentinel_totem_stolen", true);
				putD(entity, "sentinel_totem_inspect_ticks", 180);
				clearDoubles(entity, "sentinel_solar_charge_ticks", "sentinel_solar_fire_ticks", "sentinel_cryo_charge_ticks", "sentinel_cryo_fire_ticks");
				if (world instanceof ServerLevel level) {
					playHostileSound(level, entity, "block.bell.resonate", 1.5F, 0.6F);
					level.sendParticles(ParticleTypes.CRIT, bestItem.getX(), bestItem.getY(), bestItem.getZ(), 35, 0.4, 0.4, 0.4, 0.2);
				}
				handlePassengerAndGrowth(entity);
				return true;
			} else {
				if (entity instanceof Mob mob) {
					mob.getNavigation().moveTo(bestItem.getX(), bestItem.getY(), bestItem.getZ(), 1.45);
				}
				snapLookAtTarget(entity, bestItem);
				if (dist <= 10.0) {
					Vec3 pull = entity.position().subtract(bestItem.position()).normalize().scale(0.18);
					setMotion(bestItem, bestItem.getDeltaMovement().add(pull));
				}
				return true;
			}
		}
		return false;
	}

	public static boolean isHazardousLocation(LevelAccessor world, double x, double y, double z) {
		if (world == null) return true;
		if (y <= world.getMinBuildHeight() + 1.0) return true;

		BlockPos posFeet = BlockPos.containing(x, y, z);
		BlockPos posHead = posFeet.above();
		BlockPos posBelow = posFeet.below();

		net.minecraft.world.level.block.state.BlockState feetState = world.getBlockState(posFeet);
		net.minecraft.world.level.block.state.BlockState headState = world.getBlockState(posHead);
		net.minecraft.world.level.block.state.BlockState belowState = world.getBlockState(posBelow);

		if (feetState.is(net.minecraft.world.level.block.Blocks.LAVA) || belowState.is(net.minecraft.world.level.block.Blocks.LAVA)) return true;
		if (feetState.is(net.minecraft.world.level.block.Blocks.WATER) || feetState.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)) return true;
		if (belowState.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) || feetState.is(net.minecraft.world.level.block.Blocks.FIRE) || feetState.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) return true;
		if (feetState.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH) || feetState.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE) || feetState.is(net.minecraft.world.level.block.Blocks.COBWEB)) return true;

		if (feetState.is(net.minecraft.world.level.block.Blocks.TNT) || belowState.is(net.minecraft.world.level.block.Blocks.TNT)) return true;
		if (feetState.is(net.minecraft.world.level.block.Blocks.TRIPWIRE) || feetState.getBlock() instanceof net.minecraft.world.level.block.BasePressurePlateBlock) return true;

		if (feetState.isSolid() && headState.isSolid()) return true;

		if (!belowState.isSolid() && !belowState.isCollisionShapeFullBlock(world, posBelow)) {
			int airCount = 0;
			for (int dy = 1; dy <= 5; dy++) {
				BlockPos checkPos = posFeet.below(dy);
				if (!world.getBlockState(checkPos).isSolid()) {
					airCount++;
				} else {
					break;
				}
			}
			if (airCount >= 4) return true;
		}

		return false;
	}

	public static boolean interceptEnderPearlsPipeline(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level) || self == null || target == null) return false;

		AABB box = target.getBoundingBox().inflate(48.0);
		java.util.List<net.minecraft.world.entity.projectile.ThrownEnderpearl> pearls = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownEnderpearl.class, box);

		net.minecraft.world.entity.projectile.ThrownEnderpearl targetPearl = null;
		for (net.minecraft.world.entity.projectile.ThrownEnderpearl p : pearls) {
			if (p.isAlive() && (p.getOwner() == null || p.getOwner().equals(target))) {
				targetPearl = p;
				break;
			}
		}

		if (targetPearl == null) return false;

		cancelActiveCombosAndAbilities(self);

		Vec3 pearlPos = targetPearl.position();
		Vec3 pearlVel = targetPearl.getDeltaMovement();

		Vec3 predictedLanding = pearlPos.add(pearlVel.scale(15.0));
		BlockHitResult hit = level.clip(new ClipContext(pearlPos, pearlPos.add(pearlVel.scale(30.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, targetPearl));
		if (hit.getType() != HitResult.Type.MISS) {
			predictedLanding = hit.getLocation();
		}

		int pearlSeed = targetPearl.getId();
		double offsetAngle = (pearlSeed % 360) * (Math.PI / 180.0);
		double offsetRadius = 1.6 + (Math.abs(pearlSeed * 37) % 15) * 0.1;
		double ambushX = predictedLanding.x + Math.cos(offsetAngle) * offsetRadius;
		double ambushZ = predictedLanding.z + Math.sin(offsetAngle) * offsetRadius;
		double groundY = findTargetGroundY(level, ambushX, predictedLanding.y, ambushZ);

		boolean isTrap = isHazardousLocation(level, ambushX, groundY, ambushZ);

		if (isTrap) {
			if (self instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			self.setDeltaMovement(self.getDeltaMovement().x() * 0.1, self.getDeltaMovement().y(), self.getDeltaMovement().z() * 0.1);

			boolean unlockedSolar = getB(self, "unlocked_solar_beam");
			boolean unlockedCryo = getB(self, "unlocked_cryo_beam");
			if ((unlockedSolar || unlockedCryo) && getD(self, "sentinel_global_ability_cooldown") <= 0) {
				lockLookAtTarget(self, targetPearl);
				if (unlockedSolar) {
					putD(self, "sentinel_solar_fire_ticks", 15.0);
				} else {
					putD(self, "sentinel_cryo_fire_ticks", 15.0);
				}
				putD(self, "sentinel_global_ability_cooldown", 20.0);
			} else {
				lockLookAtTarget(self, target);
			}
			return true;
		}

		double distToAmbush = self.position().distanceTo(new Vec3(ambushX, groundY, ambushZ));

		if (self instanceof Mob mob) {
			mob.getNavigation().stop();
		}
		self.setDeltaMovement(self.getDeltaMovement().x() * 0.1, self.getDeltaMovement().y(), self.getDeltaMovement().z() * 0.1);

		lockLookAtTarget(self, targetPearl);
		putB(self, "sentinel_waiting_intercept", true);

		putB(self, "unlocked_teleportation", true);
		putB(self, "unlocked_knockback_rider_combo", true);
		putB(self, "unlocked_dropkick_combo", true);

		double pearlFlightDist = pearlPos.distanceTo(predictedLanding);
		boolean aboutToLand = pearlFlightDist < 3.0 || targetPearl.tickCount > 12;

		if (aboutToLand && distToAmbush > 2.0) {
			double globalCd = getD(self, "sentinel_global_ability_cooldown");
			if (globalCd <= 0) {
				teleportEntity(self, ambushX, groundY, ambushZ);
				self.setOnGround(true);
				self.setDeltaMovement(0.0, 0.0, 0.0);
				lockLookAtTarget(self, target);

				putD(self, "sentinel_cc4_stage", 1);
				putD(self, "sentinel_cc4_ticks", 20);
				putD(self, "sentinel_combo_active_ticks", 120);
				putD(self, "sentinel_global_ability_cooldown", 30.0);
			}
		} else if (self instanceof Mob mob && distToAmbush > 2.0) {
			mob.getNavigation().moveTo(ambushX, groundY, ambushZ, ROT_RUN_SPEED);
		}

		return true;
	}

	public static InterceptionPrediction evaluateInterceptionPipeline(Entity self, Entity target, TargetIntent intent) {
		if (self == null || target == null) {
			return new InterceptionPrediction(target != null ? target.position() : Vec3.ZERO, 0.0, 0.5, false, 0.0, null);
		}

		Vec3 selfPos = self.position();
		Vec3 targetPos = target.position();
		Vec3 targetVel = target.getDeltaMovement();

		double dist = selfPos.distanceTo(targetPos);

		double leadSec = 0.5 + Math.min(1.5, dist / 10.0);
		Vec3 predictedTargetPos = targetPos.add(targetVel.scale(leadSec * 20.0));

		double confidence = 0.85;
		if (intent == TargetIntent.AERIAL_ADVANTAGE) {
			confidence = 0.90;
		} else if (intent == TargetIntent.ESCAPING) {
			confidence = 0.88;
		} else if (intent == TargetIntent.REPOSITIONING) {
			confidence = 0.95;
		} else if (intent == TargetIntent.SPACING_RESET || intent == TargetIntent.BAITING) {
			confidence = 0.70;
		} else if (intent == TargetIntent.TERRAIN_ABUSE) {
			confidence = 0.75;
		}

		Vec3 alternateCutoff = null;
		double routeDenialConfidence = 0.6;

		if (intent == TargetIntent.ESCAPING) {
			Vec3 accel = Vec3.ZERO;
			CombatProfile prof = null;
			if (target instanceof LivingEntity targetLiv) {
				String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetLiv.getType()).toString();
				String profKey = (targetLiv instanceof Player p) ? ("player:" + p.getUUID()) : typeId;
				prof = UniversalCombatPredictionEngine.getProfile(profKey);
				EntityObservation obs = UniversalCombatPredictionEngine.getObservation(targetLiv.getUUID());
				if (obs != null && obs.lastDeltaMovement != null) {
					accel = targetVel.subtract(obs.lastDeltaMovement);
				}
			}

			double leadTicks = leadSec * 20.0;
			Vec3 rawProjected = targetPos.add(targetVel.scale(leadTicks)).add(accel.scale(0.5 * leadTicks * 0.15));
			Vec3 escapeDirection = targetVel.lengthSqr() > 0.01 ? targetVel.normalize() : targetPos.subtract(selfPos).normalize();
			Vec3 targetDestination = rawProjected;

			if (self.level() instanceof ServerLevel level) {
				BlockHitResult hit = level.clip(new ClipContext(targetPos, rawProjected, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
				if (hit.getType() != HitResult.Type.MISS) {
					Vec3 wallPos = hit.getLocation();
					Vec3 normal = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
					Vec3 slideDir = escapeDirection.subtract(normal.scale(escapeDirection.dot(normal))).normalize();
					targetDestination = wallPos.add(slideDir.scale(6.0));
					confidence *= 0.75;
				}
			}

			if (prof != null && !prof.recentEscapeDestinations.isEmpty()) {
				for (Vec3 prevDest : prof.recentEscapeDestinations) {
					Vec3 dirToPrev = prevDest.subtract(targetPos).normalize();
					if (escapeDirection.dot(dirToPrev) > 0.6) {
						targetDestination = prevDest;
						confidence = Math.min(0.95, confidence + 0.15);
						routeDenialConfidence = 0.90;
						break;
					}
				}
			}

			predictedTargetPos = targetDestination;
			alternateCutoff = targetPos.add(escapeDirection.scale(Math.min(16.0, dist + 6.0)));
		} else if (self.level() instanceof ServerLevel level) {
			BlockHitResult hit = level.clip(new ClipContext(targetPos, predictedTargetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
			if (hit.getType() != HitResult.Type.MISS) {
				predictedTargetPos = hit.getLocation();
				confidence *= 0.6;
			}
		}

		boolean recommendWait = false;
		double waitTicks = 0.0;
		Vec3 repositionPos = null;

		if (intent == TargetIntent.AERIAL_ADVANTAGE) {
			repositionPos = new Vec3(predictedTargetPos.x, selfPos.y, predictedTargetPos.z);
			double distToReposition = selfPos.distanceTo(repositionPos);
			if (distToReposition > 2.0 && dist > 4.0) {
				recommendWait = true;
				waitTicks = Math.min(30.0, distToReposition * 4.0);
			}
		} else if (intent == TargetIntent.ESCAPING) {
			repositionPos = (alternateCutoff != null && selfPos.distanceTo(alternateCutoff) < selfPos.distanceTo(predictedTargetPos)) ? alternateCutoff : predictedTargetPos;
			double distToReposition = selfPos.distanceTo(repositionPos);
			if (distToReposition > 4.0 && dist > 5.0) {
				recommendWait = true;
				waitTicks = Math.min(20.0, distToReposition * 2.0);
			}
		}

		return new InterceptionPrediction(predictedTargetPos, leadSec * 20.0, confidence, recommendWait, waitTicks, repositionPos, alternateCutoff, routeDenialConfidence);
	}

	public static class InterceptionPrediction {
		public final Vec3 predictedPos;
		public final double leadTicks;
		public final double interceptProbability;
		public final boolean recommendWait;
		public final double waitTicks;
		public final Vec3 repositionTargetPos;
		public Vec3 alternateCutoffPos = null;
		public double routeDenialConfidence = 0.5;

		public InterceptionPrediction(Vec3 predictedPos, double leadTicks, double interceptProbability, boolean recommendWait, double waitTicks, Vec3 repositionTargetPos) {
			this.predictedPos = predictedPos;
			this.leadTicks = leadTicks;
			this.interceptProbability = interceptProbability;
			this.recommendWait = recommendWait;
			this.waitTicks = waitTicks;
			this.repositionTargetPos = repositionTargetPos;
		}

		public InterceptionPrediction(Vec3 predictedPos, double leadTicks, double interceptProbability, boolean recommendWait, double waitTicks, Vec3 repositionTargetPos, Vec3 alternateCutoffPos, double routeDenialConfidence) {
			this.predictedPos = predictedPos;
			this.leadTicks = leadTicks;
			this.interceptProbability = interceptProbability;
			this.recommendWait = recommendWait;
			this.waitTicks = waitTicks;
			this.repositionTargetPos = repositionTargetPos;
			this.alternateCutoffPos = alternateCutoffPos;
			this.routeDenialConfidence = routeDenialConfidence;
		}
	}

	public static InterceptionPrediction evaluateInterception(AbilityInfo ability, Entity self, Entity target) {
		if (ability == null || self == null || target == null) {
			return new InterceptionPrediction(target != null ? target.position() : Vec3.ZERO, 0.0, 0.0, false, 0.0, null);
		}

		Vec3 selfPos = self.position();
		Vec3 targetPos = target.position();
		Vec3 targetVel = target.getDeltaMovement();

		boolean isMounted = target.isPassenger();
		boolean inBoat = isMounted && target.getVehicle() != null && target.getVehicle().getType().toString().toLowerCase().contains("boat");
		boolean isFlying = (target instanceof LivingEntity liv) && (liv.isFallFlying() || (!liv.onGround() && targetPos.y > selfPos.y + 2.0));
		boolean inWater = target.isInWater();

		double startupSec = ability.startupTicks / 20.0;
		double travelTime = 0.0;
		double speed = Math.max(0.1, ability.travelSpeed * 1.5);
		double dist = selfPos.distanceTo(targetPos);

		if ("DASH".equals(ability.movementType) || "LEAP".equals(ability.movementType) || "TELEPORT".equals(ability.movementType)) {
			travelTime = Math.min(dist / speed, 2.0);
		} else if ("PROJECTILE".equals(ability.movementType)) {
			travelTime = dist / 2.0;
		} else {
			travelTime = dist / Math.max(0.3, self instanceof LivingEntity liv ? getSafeAttributeValue(liv, Attributes.MOVEMENT_SPEED, 0.25) * 5.0 : 1.0);
		}

		double totalLeadSec = startupSec + travelTime;
		Vec3 effectiveVel = targetVel;
		if (inBoat) {
			effectiveVel = new Vec3(targetVel.x * 1.25, targetVel.y, targetVel.z * 1.25);
		} else if (isFlying) {
			effectiveVel = new Vec3(targetVel.x, targetVel.y * 0.85, targetVel.z);
		}

		Vec3 predictedPos = targetPos.add(effectiveVel.scale(totalLeadSec * 20.0));
		double maxReach = Math.max(ability.horizontalReach, ability.range);

		double immediateProb = 1.0;
		if (dist > maxReach) {
			immediateProb = 0.0;
		} else {
			immediateProb = Math.max(0.1, 1.0 - (dist / maxReach));
		}

		if (inWater || inBoat) {
			if (ability.usableAgainstBoats || ability.canCrossWater) {
				immediateProb = Math.min(1.0, immediateProb * 1.8);
			} else {
				immediateProb *= 0.3;
			}
		}

		if (isFlying) {
			if (ability.usableAgainstAir || ability.antiAirRating > 1.0) {
				immediateProb = Math.min(1.0, immediateProb * 1.6);
			} else {
				immediateProb *= 0.2;
			}
		}

		Vec3 predPos10 = targetPos.add(effectiveVel.scale(10.0));
		Vec3 predPos20 = targetPos.add(effectiveVel.scale(20.0));
		double dist10 = selfPos.distanceTo(predPos10);
		double dist20 = selfPos.distanceTo(predPos20);

		double prob10 = (dist10 <= maxReach) ? (1.0 - (dist10 / maxReach)) : 0.0;
		double prob20 = (dist20 <= maxReach) ? (1.0 - (dist20 / maxReach)) : 0.0;

		boolean recommendWait = false;
		double waitTicks = 0.0;
		if (prob10 > immediateProb + 0.35) {
			recommendWait = true;
			waitTicks = 10.0;
		} else if (prob20 > immediateProb + 0.50) {
			recommendWait = true;
			waitTicks = 20.0;
		}

		Vec3 repositionPos = null;
		if (isFlying && ability.gainsAltitude > 1.0) {
			repositionPos = new Vec3(predictedPos.x, selfPos.y, predictedPos.z);
		}

		return new InterceptionPrediction(predictedPos, totalLeadSec * 20.0, Math.min(1.0, Math.max(0.0, immediateProb)), recommendWait, waitTicks, repositionPos);
	}

	public static double scoreAbility(AbilityInfo ability, CombatContext ctx, Entity self, Entity target) {
		if (ability == null || self == null || target == null) return 0.0;

		double baseScore = 10.0;
		double multiplier = 1.0;
		double dist = self.distanceTo(target);

		if (target instanceof LivingEntity targetLiv) {
			double targetHp = targetLiv.getHealth();
			double rotDmg = MELEE_PUNCH_DAMAGE * getAdaptationMultiplier(self);
			if (!(target instanceof Player) && (targetHp <= rotDmg || targetHp <= 20.0)) {
				if (ability.hasTag("aoe") || ability.createsAOE) {
					multiplier *= 2.5;
				} else if (ability.hasTag("burst") || ability.id.contains("combo") || "overhead_combo".equals(ability.id) || "high_sky_slam_combo".equals(ability.id)) {
					multiplier *= 0.1;
				}
			}
		}

		TargetIntent intent = inferTargetIntent(self, target);
		if (intent == TargetIntent.AERIAL_ADVANTAGE && (ability.antiAirRating > 0.0 || ability.usableAgainstAir || ability.gainsAltitude > 0.0)) {
			multiplier *= 4.0;
		} else if (intent == TargetIntent.ESCAPING && (ability.usableAgainstBoats || ability.gapCloserRating > 0.0 || ability.escapePunishRating > 0.0)) {
			multiplier *= 4.0;
		} else if (intent == TargetIntent.REPOSITIONING && "TELEPORT".equals(ability.movementType)) {
			multiplier *= 4.0;
		} else if (intent == TargetIntent.SPACING_RESET && (ability.hasTag("control") || ability.gapCloserRating > 0.0 || "block".equals(ability.id))) {
			multiplier *= 3.0;
		} else if (intent == TargetIntent.TERRAIN_ABUSE && (ability.hasTag("aoe") || "sky_warp_slam".equals(ability.id) || ability.id.contains("sonic") || ability.hasTag("ranged"))) {
			multiplier *= 3.5;
		}

		// Future combat state lookahead across competing hypotheses:
		PendingPrediction pending = (target instanceof LivingEntity liv) ? UniversalCombatPredictionEngine.getPending(liv.getUUID()) : null;
		if (pending != null && !pending.competingHypotheses.isEmpty()) {
			double expectedUtility = 0.0;
			for (ActionHypothesis hyp : pending.competingHypotheses) {
				double payoff = 1.0;
				if ("CRIT_ATTACK".equals(hyp.action) || "MELEE_ATTACK".equals(hyp.action)) {
					if (ability.antiAirRating > 0.0 || ability.hasTag("anti-air") || "parry".equals(ability.id) || "block".equals(ability.id) || "uppercut".equals(ability.id)) {
						payoff = 2.8;
					} else if (ability.startupTicks > 12.0) {
						payoff = 0.3;
					}
				} else if ("SHIELD_BLOCK".equals(hyp.action)) {
					if (ability.shieldBreakRating > 0.0 || ability.hasTag("anti-shield") || "overhead_slam".equals(ability.id) || "armor_rip".equals(ability.id)) {
						payoff = 3.2;
					} else if ("block".equals(ability.id)) {
						payoff = 0.2;
					} else if (ability.commitment > 2.0) {
						payoff = 0.4;
					}
				} else if ("RETREAT_HEAL".equals(hyp.action) || "ESCAPING".equals(hyp.action)) {
					if (ability.gapCloserRating > 0.0 || ability.hasTag("burst") || ability.id.contains("laser") || ability.id.contains("sonic") || "teleport".equals(ability.id) || "telekinesis".equals(ability.id)) {
						payoff = 3.0;
					} else if (ability.range < 4.0) {
						payoff = 0.2;
					}
				} else if ("BAIT_FEINT".equals(hyp.action) || "SPACING_RESET".equals(hyp.action)) {
					if (ability.commitment > 1.8 || ability.startupTicks > 10.0) {
						payoff = 0.15;
					} else if (ability.startupTicks <= 5.0 && ability.horizontalReach <= 3.8) {
						payoff = 1.8;
					} else {
						payoff = 0.6;
					}
				} else if ("DODGE_STRAFE".equals(hyp.action)) {
					if (ability.createsAOE || ability.hasTag("aoe") || "omni_sonic_boom".equals(ability.id) || "sonic_scream".equals(ability.id)) {
						payoff = 2.5;
					} else if (ability.hasTag("linear") || ability.range > 6.0) {
						payoff = 0.4;
					}
				} else if ("TERRAIN_RESET".equals(hyp.action)) {
					if (ability.id.contains("sonic") || "sky_warp_slam".equals(ability.id) || "teleport".equals(ability.id) || "telekinesis".equals(ability.id)) {
						payoff = 3.0;
					} else if (ability.hasTag("melee")) {
						payoff = 0.3;
					}
				}
				expectedUtility += hyp.probability * payoff;
			}
			multiplier *= Math.max(0.1, expectedUtility);

			// Uncertainty and bait aversion
			if (pending.entropyUncertainty > 0.60 || pending.isLowConfidence()) {
				if (ability.commitment > 1.8) {
					multiplier *= 0.30;
				} else if (ability.startupTicks <= 6.0) {
					multiplier *= 1.40;
				}
			} else if (pending.isHighConfidence()) {
				multiplier *= 1.30;
			}
		}

		// Dominant survival resource denial:
		if (target instanceof LivingEntity targetLiv) {
			SurvivalResourceAnalysis res = analyzeSurvivalResources(self, targetLiv);
			if (res.dominantResource == DominantResource.TOTEM) {
				if (ability.hasTag("burst") || ability.burstRating > 1.5 || "judgment".equals(ability.id)) {
					multiplier *= 2.8;
				}
			} else if (res.dominantResource == DominantResource.SHIELD) {
				if (ability.shieldBreakRating > 0.0 || ability.hasTag("anti-shield") || "overhead_slam".equals(ability.id) || "armor_rip".equals(ability.id)) {
					multiplier *= 3.0;
				}
			} else if (res.dominantResource == DominantResource.HEALING) {
				if (ability.interruptionRating > 1.0 || ability.gapCloserRating > 1.0 || ability.hasTag("burst") || "dash_punch".equals(ability.id)) {
					multiplier *= 3.0;
				}
			} else if (res.dominantResource == DominantResource.MOBILITY_ESCAPE) {
				if (ability.escapePunishRating > 0.0 || "telekinesis".equals(ability.id) || "teleport".equals(ability.id) || "dropkick".equals(ability.id)) {
					multiplier *= 2.8;
				}
			} else if (res.dominantResource == DominantResource.TERRAIN_COVER) {
				if (ability.id.contains("sonic") || "sky_warp_slam".equals(ability.id) || "telekinesis".equals(ability.id)) {
					multiplier *= 3.2;
				}
			}
		}

		CombatProfile prof = null;
		if (target instanceof LivingEntity targetLiv) {
			String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetLiv.getType()).toString();
			String profKey = (targetLiv instanceof Player p) ? ("player:" + p.getUUID()) : typeId;
			prof = UniversalCombatPredictionEngine.getProfile(profKey);
		}
		if (prof != null && prof.activeChainStep != TacticalChainStep.NONE) {
			if (prof.activeChainStep == TacticalChainStep.FORCE_DEFENSE && (ability.gapCloserRating > 0.0 || ability.id.contains("punch") || ability.startupTicks <= 6.0)) {
				multiplier *= 2.0;
			} else if (prof.activeChainStep == TacticalChainStep.PUNISH_OPENING && (ability.shieldBreakRating > 0.0 || ability.burstRating > 1.0 || ability.hasTag("burst"))) {
				multiplier *= 2.2;
			} else if (prof.activeChainStep == TacticalChainStep.DENY_ESCAPE && (ability.escapePunishRating > 0.0 || "telekinesis".equals(ability.id) || ability.gapCloserRating > 1.0)) {
				multiplier *= 2.4;
			} else if (prof.activeChainStep == TacticalChainStep.EXECUTE && (ability.hasTag("burst") || ability.burstRating > 1.5 || "judgment".equals(ability.id))) {
				multiplier *= 2.5;
			}
		}

		InterceptionPrediction prediction = evaluateInterception(ability, self, target);
		multiplier *= (0.2 + 1.8 * prediction.interceptProbability);

		if (prediction.recommendWait) {
			multiplier *= 0.3;
		}

		double maxReach = Math.max(ability.horizontalReach, ability.range);
		if (dist > maxReach) {
			multiplier *= 0.1;
		} else if (ability.id.contains("dropkick") && dist < 3.5) {
			multiplier *= 0.05;
		} else if (ability.preferredMinimumRange > 0.0 && dist < ability.preferredMinimumRange) {
			multiplier *= 0.1;
		} else if (dist < 3.0 && ability.hasTag("ranged")) {
			multiplier *= 0.3;
		} else if (dist > 10.0 && ability.hasTag("ranged")) {
			multiplier *= (1.0 + ability.zoningRating * 0.4);
		} else if (dist > 5.0 && ability.gapCloserRating > 0.0) {
			multiplier *= (1.0 + ability.gapCloserRating * 0.5);
		}

		if (ability.gapCloserRating > 0.0 && dist >= 6.0) {
			multiplier *= (1.0 + ability.gapCloserRating * 0.4);
		}
		if (ability.antiAirRating > 0.0 && (ctx != null && ctx.isAirborne)) {
			multiplier *= (1.0 + ability.antiAirRating * 0.5);
		}
		if (ability.shieldBreakRating > 0.0 && (ctx != null && ctx.isBlocking)) {
			multiplier *= (1.0 + ability.shieldBreakRating * 0.5);
		}
		if (ability.escapePunishRating > 0.0 && target.getDeltaMovement().lengthSqr() > 0.1) {
			multiplier *= (1.0 + ability.escapePunishRating * 0.4);
		}

		if (ctx != null) {
			if (ctx.expectedIncomingDamage > 0.0) {
				boolean defensiveAbility = ability.hasTag("defense") || ability.hasTag("anti-projectile") || "block".equals(ability.id) || "teleport".equals(ability.id) || ability.interruptionRating > 1.5;
				if (defensiveAbility) multiplier *= 1.0 + Math.min(1.5, ctx.expectedIncomingDamage / 20.0);
				if (ctx.expectedIncomingDamage > 0.5 * Math.max(1.0f, self instanceof LivingEntity liv ? liv.getMaxHealth() : 20.0f) && ability.commitment > 2.5) multiplier *= 0.45;
			}
			if (ctx.isAirborne && (ability.hasTag("anti-air") || ability.antiAirRating > 1.0)) multiplier *= 2.0;
			if (ctx.isBlocking && (ability.hasTag("anti-shield") || ability.shieldBreakRating > 1.0)) multiplier *= 2.0;
			if (ctx.isCornered && (ability.hasTag("aoe") || ability.hasTag("control") || ability.crowdControlRating > 1.0)) multiplier *= 1.8;
			if (ctx.isHealing && (ability.hasTag("burst") || ability.burstRating > 1.0)) multiplier *= 2.0;

			if (ctx.isJumpCritIncoming && (ability.hasTag("anti-air") || ability.shieldBreakRating > 0.0 || ability.id.contains("punch") || "block".equals(ability.id) || "uppercut".equals(ability.id))) {
				multiplier *= 2.8;
			}

			if (ctx.incomingProjectileDistance > 0.0 && ctx.incomingProjectileDistance <= 10.0 && (ability.hasTag("anti-projectile") || ability.hasTag("defense") || "block".equals(ability.id) || "teleport".equals(ability.id) || ability.gapCloserRating > 1.5)) {
				multiplier *= 3.0;
			}

			if (ctx.nearbyTargetCount >= 2 && (ability.hasTag("aoe") || ability.createsAOE || "omni_sonic_boom".equals(ability.id) || "sonic_scream".equals(ability.id) || ability.id.contains("slam"))) {
				multiplier *= (1.0 + (ctx.nearbyTargetCount - 1) * 0.75);
			}

			if (ctx.targetNearLedgeOrHazard && (ability.hasTag("knockback") || ability.id.contains("kick") || ability.id.contains("punch") || "sonic_boom".equals(ability.id) || "overhead_combo".equals(ability.id))) {
				multiplier *= 3.5;
			}

			if (ctx.isEnclosedSpace) {
				if (ability.gainsAltitude > 1.0 || "high_sky_slam_combo".equals(ability.id) || "sky_warp_slam".equals(ability.id)) {
					multiplier *= 0.15;
				} else if (ability.hasTag("corridor") || ability.hasTag("grab") || "grapple".equals(ability.id) || "armor_rip".equals(ability.id) || ability.id.contains("beam") || "telekinesis".equals(ability.id)) {
					multiplier *= 2.2;
				}
			}
		}

		if ("omni_sonic_boom".equals(ability.id)) {
			AABB box = self.getBoundingBox().inflate(6.0);
			List<LivingEntity> nearby = self.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != self && e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
			double targetMaxHp = (target instanceof LivingEntity tLiv) ? tLiv.getMaxHealth() : 999.0;
			double targetHp = (target instanceof LivingEntity tLiv) ? tLiv.getHealth() : 999.0;
			boolean isSingleWeakTarget = nearby.size() < 2 || (nearby.size() == 2 && (targetHp <= 40.0 || targetMaxHp <= 50.0));

			if (isSingleWeakTarget) {
				multiplier = 0.0;
			} else if (nearby.size() >= 3) {
				multiplier *= (1.0 + nearby.size() * 0.5);
			} else {
				multiplier *= 0.1;
			}
		} else if (ability.id.contains("beam") || "solar_beam".equals(ability.id) || "cryo_beam".equals(ability.id)) {
			double heat = getD(self, "sentinel_laser_heat");
			if (heat > 50.0) multiplier *= 0.1;
			else if (heat > 0.0) multiplier *= (1.0 - heat / 100.0);
		}

		String currentStrategy = getRotPersistentString(self, "sentinel_current_strategy", "BALANCED");
		if ("ANTI_AIR".equals(currentStrategy)) {
			if (ability.hasTag("anti-air") || ability.antiAirRating > 1.5) multiplier *= 2.2;
		} else if ("BURST_AND_DISENGAGE".equals(currentStrategy)) {
			if (ability.hasTag("burst") || ability.gapCloserRating > 1.5) multiplier *= 1.8;
			if (ability.hasTag("sustained") || ability.sustainedRating > 2.0) multiplier *= 0.4;
		} else if ("MELEE_DOMINANCE".equals(currentStrategy)) {
			if (maxReach <= 6.0 || ability.gapCloserRating > 1.0) multiplier *= 1.8;
			if (ability.hasTag("ranged")) multiplier *= 0.5;
		} else if ("GROUNDED_STABILITY".equals(currentStrategy)) {
			if (ability.hasTag("control") || ability.gapCloserRating > 1.0) multiplier *= 1.6;
		} else if ("HEAVY_BURST_SPACING".equals(currentStrategy)) {
			if (ability.hasTag("burst") || ability.hasTag("ranged")) multiplier *= 1.8;
		} else if ("AOE_CLEAR".equals(currentStrategy)) {
			if (ability.hasTag("aoe") || ability.createsAOE) multiplier *= 2.2;
		} else if ("FLANK_AND_PUNISH".equals(currentStrategy)) {
			if (ability.hasTag("anti-shield") || ability.shieldBreakRating > 1.5 || ability.hasTag("control")) multiplier *= 1.8;
		} else if ("CONTINUOUS_PRESSURE".equals(currentStrategy)) {
			if (ability.hasTag("sustained") || ability.sustainedRating > 1.5 || ability.hasTag("burst")) multiplier *= 1.6;
		} else if ("EVASIVE_FLANK".equals(currentStrategy)) {
			if (ability.gapCloserRating > 1.5 || ability.hasTag("ranged")) multiplier *= 1.5;
		} else if ("BAIT_AND_PUNISH".equals(currentStrategy)) {
			if (ability.hasTag("control") || ability.hasTag("burst")) multiplier *= 1.6;
		}

		String currentPlan = getRotPersistentString(self, "sentinel_tactical_plan", "BALANCED_PRESSURE");
		if ("MAINTAIN_DISTANCE".equals(currentPlan)) {
			if (ability.hasTag("ranged") || ability.zoningRating > 1.5) multiplier *= 2.0;
			if (ability.gapCloserRating > 1.0 || maxReach <= 5.0) multiplier *= 0.3;
		} else if ("AGGRESSIVE_CHARGE".equals(currentPlan)) {
			if (ability.gapCloserRating > 1.0 || ability.hasTag("burst")) multiplier *= 2.0;
		} else if ("RETREAT_AND_HEAL".equals(currentPlan)) {
			if (ability.hasTag("ranged") || ability.hasTag("control")) multiplier *= 1.5;
			if (maxReach <= 5.0) multiplier *= 0.2;
		}

		double recentDps = (getD(self, "rot_dmg_sec_0") + getD(self, "rot_dmg_sec_1") + getD(self, "rot_dmg_sec_2") + getD(self, "rot_dmg_sec_3") + getD(self, "rot_dmg_sec_4")) / 5.0;
		if (recentDps < 4.0 && dist <= 8.0) {
			// Low damage throughput - target is actively avoiding or mitigating hits; prioritize heavy unblockables and burst shield-breakers
			if (ability.hasTag("anti-shield") || ability.shieldBreakRating > 1.0 || ability.hasTag("burst") || ability.id.contains("overhead") || ability.id.contains("slam")) {
				multiplier *= 1.8;
			}
		}

		multiplier *= getMemoryPenalty(self, ability.id);

		String last1 = getS(self, "sentinel_mem_1");
		String last2 = getS(self, "sentinel_mem_2");
		if (!last1.isEmpty() && !last2.isEmpty()) {
			String trigram = last2 + "->" + last1 + "->" + ability.id;
			String history = getS(self, "recent_attack_history");
			if (history.contains(trigram)) {
				multiplier *= 0.35;
			}
		}

		return baseScore * multiplier * (0.8 + Math.random() * 0.4);
	}

	public static double getMemoryPenalty(Entity self, String attackType) {
		double penalty = 1.0;
		if (getS(self, "sentinel_mem_1").equals(attackType)) penalty *= 0.15;
		if (getS(self, "sentinel_mem_2").equals(attackType)) penalty *= 0.4;
		if (getS(self, "sentinel_mem_3").equals(attackType)) penalty *= 0.7;
		return penalty;
	}

	public static int evaluateComboUtility(Entity self, Entity target, CombatContext ctx, java.util.List<Integer> available) {
		int bestCombo = 0;
		double bestScore = -1.0;
		double playerAir = getD(target, "ai_target_air_ticks");
		double playerShield = getD(target, "ai_target_shield_ticks");
		TargetIntent intent = inferTargetIntent(self, target);

		double targetHp = (target instanceof LivingEntity tLiv) ? tLiv.getHealth() : 999.0;
		double rotDmg = MELEE_PUNCH_DAMAGE * getAdaptationMultiplier(self);
		boolean lowHpTarget = !(target instanceof Player) && (targetHp <= rotDmg || targetHp <= 20.0);

		for (int c : available) {
			if (c == 1) continue; // Completely remove combo 1
			if (lowHpTarget && (c == 5 || c == 8 || c == 12 || c == 101 || c == 102 || c == 103 || c == 104 || c == 105)) {
				continue;
			}
			double score = 10.0;
			String comboName = "combo_" + c;
			score *= getMemoryPenalty(self, comboName);
			score *= getRotPersistentDouble(self, "ai_bias_" + comboName, 1.0);
			double tpComboPenalty = getD(self, "ai_tp_combo_penalty_ticks");
			if (tpComboPenalty > 0 && (c == 4 || c == 7 || c == 8 || c == 9 || c == 10 || c == 11)) {
				score *= 0.1;
			}

			if (intent == TargetIntent.AERIAL_ADVANTAGE) {
				if (c == 102 || c == 104 || c == 3) score *= 4.0;
			} else if (intent == TargetIntent.ESCAPING) {
				if (c == 13 || c == 103 || c == 104 || c == 3 || c == 2) score *= 4.0;
			} else if (intent == TargetIntent.REPOSITIONING) {
				if (c == 4 || c == 7 || c == 8 || c == 9 || c == 10 || c == 11 || c == 104) score *= 3.5;
			} else if (intent == TargetIntent.AGGRESSIVE_MELEE) {
				if (c == 101 || c == 14 || c == 6 || c == 12 || c == 105) score *= 3.5;
			} else if (intent == TargetIntent.TURTLING) {
				if (c == 8 || c == 102 || c == 4 || c == 105) score *= 4.0;
			}

			if (c == 2) {
				if (ctx.dist > 3.5) score *= 2.2;
				if (ctx.isMovingFast) score *= 1.8;
			} else if (c == 3) {
				if (ctx.isAirborne || playerAir > 15) score *= 2.5;
				if (ctx.dist > 4.0) score *= 2.0;
			} else if (c == 4) {
				if (ctx.isBlocking || playerShield > 15) score *= 2.5;
				if (ctx.isMovingFast) score *= 1.6;
			} else if (c == 5) {
				if (!ctx.isAirborne) score *= 2.0;
				if (ctx.isCornered) score *= 2.0;
			} else if (c == 6) {
				if (ctx.isHealing || ctx.isAggressive) score *= 2.5;
			} else if (c == 7) {
				if (ctx.isMovingFast || ctx.dist > 3.0) score *= 2.0;
			} else if (c == 8) {
				if (ctx.isBlocking || playerShield > 10) score *= 3.0;
				if (!ctx.isAirborne) score *= 1.6;
			} else if (c == 9) {
				if (ctx.isIncomingDamage || ctx.isAggressive) score *= 2.2;
				if (ctx.dist <= 4.0) score *= 1.8;
			} else if (c == 10) {
				if (getD(target, "bw_recent_kb_ticks") > 0) {
					score *= 3.5;
				} else if (ctx.dist < 3.5) {
					score *= 0.05;
				} else {
					score *= 2.2;
				}
			} else if (c == 11) {
				if (ctx.isCornered || ctx.isMovingFast) score *= 2.2;
				if (ctx.dist <= 4.0) score *= 1.8;
			} else if (c == 12) {
				if (!ctx.isAirborne && ctx.dist <= 3.5) score *= 2.2;
			} else if (c == 13) {
				if (getD(target, "bw_recent_kb_ticks") > 0) {
					score *= 3.5;
				} else if (ctx.dist < 3.5) {
					score *= 0.03;
				} else {
					score *= 2.5;
				}
			} else if (c == 14) {
				if (ctx.dist <= 3.5 && !ctx.isAirborne) score *= 2.2;
			} else if (c == 101) {
				if (!ctx.isAirborne && ctx.dist <= 4.0) score *= 2.6;
				if (ctx.isCornered) score *= 2.2;
			} else if (c == 102) {
				if (ctx.isAirborne || playerAir > 10) score *= 3.0;
				if (ctx.isBlocking || playerShield > 10) score *= 2.2;
			} else if (c == 103) {
				if (getD(target, "bw_recent_kb_ticks") > 0) {
					score *= 3.5;
				} else if (ctx.dist < 3.5) {
					score *= 0.05;
				} else {
					score *= 2.5;
				}
			} else if (c == 104) {
				if (!target.onGround()) {
					score = 0.0;
				} else {
					if (ctx.dist >= 3.0 || intent == TargetIntent.ESCAPING) score *= 3.2;
				}
			} else if (c == 105) {
				if (targetHp > 35.0 || getB(self, "sentinel_totem_active")) score *= 3.0;
				if (ctx.dist <= 4.5) score *= 1.8;
			}
			score *= (0.8 + Math.random() * 0.4);
			if (score > bestScore) {
				bestScore = score;
				bestCombo = c;
			}
		}
		if (bestCombo != 0) {
			recordAttack(self, "combo_" + bestCombo);
		}
		return bestCombo;
	}

	public enum FightStyle {
		AGGRESSIVE,
		DEFENSIVE,
		HIT_AND_RUN,
		PROJECTILE_FOCUSED,
		BEAM_FOCUSED,
		COMBO_FOCUSED,
		AOE_FOCUSED,
		MOBILITY_FOCUSED,
		COUNTER_ATTACKER,
		TANK,
		SUMMONER,
		SUPPORT,
		HYBRID
	}

	public enum ThreatLevel {
		NONE(0),
		LOW(1),
		MEDIUM(2),
		HIGH(3),
		ATTACK_IMMINENT(4);

		private final int level;
		ThreatLevel(int level) { this.level = level; }
		public int getLevel() { return level; }
		public boolean isHighOrImminent() { return this == HIGH || this == ATTACK_IMMINENT; }
	}

	public static class PersonalityVector {
		public double aggression;
		public double patience;
		public double riskTolerance;
		public double spite;

		public PersonalityVector() {
			java.util.Random rnd = new java.util.Random();
			this.aggression = 0.2 + rnd.nextDouble() * 0.6;
			this.patience = 0.2 + rnd.nextDouble() * 0.6;
			this.riskTolerance = 0.2 + rnd.nextDouble() * 0.6;
			this.spite = 0.2 + rnd.nextDouble() * 0.6;
		}

		public PersonalityVector(double a, double p, double r, double s) {
			this.aggression = Mth.clamp(a, 0.0, 1.0);
			this.patience = Mth.clamp(p, 0.0, 1.0);
			this.riskTolerance = Mth.clamp(r, 0.0, 1.0);
			this.spite = Mth.clamp(s, 0.0, 1.0);
		}

		public void drift(double dA, double dP, double dR, double dS) {
			// Strictly bounded combat calibration: preserve emotionless extermination focus
			this.aggression = Mth.clamp(this.aggression + dA * PERSONALITY_DRIFT_RATE * 0.5, 0.10, 0.95);
			this.patience = Mth.clamp(this.patience + dP * PERSONALITY_DRIFT_RATE * 0.5, 0.10, 0.95);
			this.riskTolerance = Mth.clamp(this.riskTolerance + dR * PERSONALITY_DRIFT_RATE * 0.5, 0.10, 0.90);
			this.spite = Mth.clamp(this.spite + dS * PERSONALITY_DRIFT_RATE * 0.5, 0.10, 0.95);
		}

		public void updateExterminationWeights(double targetVulnerability, double enemyCounterFrequency, double resourceReliance) {
			this.aggression = Mth.clamp(0.30 + targetVulnerability * 0.60, 0.10, 0.95);
			this.patience = Mth.clamp(0.20 + enemyCounterFrequency * 0.70, 0.10, 0.95);
			this.riskTolerance = Mth.clamp(0.25 + targetVulnerability * 0.50, 0.10, 0.90);
			this.spite = Mth.clamp(0.30 + resourceReliance * 0.65, 0.10, 0.95);
		}

		public void saveToNbt(CompoundTag tag) {
			if (tag == null) return;
			tag.putDouble("rot_personality_aggression", aggression);
			tag.putDouble("rot_personality_patience", patience);
			tag.putDouble("rot_personality_risk_tolerance", riskTolerance);
			tag.putDouble("rot_personality_spite", spite);
			tag.putBoolean("rot_personality_initialized", true);
		}

		public static PersonalityVector loadFromNbt(CompoundTag tag) {
			if (tag == null || !hasNBTKey(tag, "rot_personality_initialized")) {
				return new PersonalityVector();
			}
			double a = tag.getDouble("rot_personality_aggression");
			double p = tag.getDouble("rot_personality_patience");
			double r = tag.getDouble("rot_personality_risk_tolerance");
			double s = tag.getDouble("rot_personality_spite");
			return new PersonalityVector(a, p, r, s);
		}
	}

	public static class TacticalNeuralNetwork {
		public static final int INPUT_SIZE = 96;
		public static final int HIDDEN_SIZE = 48;
		public static final int OUTPUT_SIZE = 15;
		public static final int TOTAL_WEIGHTS = (INPUT_SIZE * HIDDEN_SIZE) + HIDDEN_SIZE + (HIDDEN_SIZE * OUTPUT_SIZE) + OUTPUT_SIZE;

		public double[] weights;

		public TacticalNeuralNetwork() {
			this.weights = new double[TOTAL_WEIGHTS];
			initDefaultWeights();
		}

		public TacticalNeuralNetwork(double[] w) {
			if (w != null && w.length == TOTAL_WEIGHTS) {
				this.weights = w;
			} else {
				this.weights = new double[TOTAL_WEIGHTS];
				initDefaultWeights();
			}
		}

		private void initDefaultWeights() {
			java.util.Random rnd = new java.util.Random(1337);
			for (int i = 0; i < weights.length; i++) {
				weights[i] = (rnd.nextDouble() - 0.5) * 0.2;
			}
		}

		public double[] forward(double[] inputs, double[] hiddenOut) {
			int idx = 0;
			for (int h = 0; h < HIDDEN_SIZE; h++) {
				double sum = weights[idx++];
				for (int i = 0; i < INPUT_SIZE; i++) {
					double val = (i < inputs.length) ? inputs[i] : 0.0;
					sum += val * weights[idx++];
				}
				hiddenOut[h] = Math.tanh(sum);
			}

			double[] outputs = new double[OUTPUT_SIZE];
			for (int o = 0; o < OUTPUT_SIZE; o++) {
				double sum = weights[idx++];
				for (int h = 0; h < HIDDEN_SIZE; h++) {
					sum += hiddenOut[h] * weights[idx++];
				}
				outputs[o] = sum;
			}
			return outputs;
		}

		public void trainDelta(double[] inputs, int chosenPlanOrdinal, double netEfficiency) {
			double[] hiddenOut = new double[HIDDEN_SIZE];
			double[] outputs = forward(inputs, hiddenOut);

			double feedback = Math.max(-2.0, Math.min(2.0, netEfficiency / 5.0));
			double targetOutput = outputs[chosenPlanOrdinal] + feedback;
			double outputError = targetOutput - outputs[chosenPlanOrdinal];

			int idx = (INPUT_SIZE * HIDDEN_SIZE) + HIDDEN_SIZE;
			double[] hiddenErrors = new double[HIDDEN_SIZE];

			for (int o = 0; o < OUTPUT_SIZE; o++) {
				double delta = (o == chosenPlanOrdinal) ? outputError : 0.0;
				weights[idx] = Mth.clamp(weights[idx] + NN_LEARNING_RATE * delta, -5.0, 5.0);
				idx++;
				for (int h = 0; h < HIDDEN_SIZE; h++) {
					hiddenErrors[h] += delta * weights[idx];
					weights[idx] = Mth.clamp(weights[idx] + NN_LEARNING_RATE * delta * hiddenOut[h], -5.0, 5.0);
					idx++;
				}
			}

			idx = 0;
			for (int h = 0; h < HIDDEN_SIZE; h++) {
				double dtanh = 1.0 - (hiddenOut[h] * hiddenOut[h]);
				double hiddenDelta = hiddenErrors[h] * dtanh;
				weights[idx] = Mth.clamp(weights[idx] + NN_LEARNING_RATE * hiddenDelta, -5.0, 5.0);
				idx++;
				for (int i = 0; i < INPUT_SIZE; i++) {
					double val = (i < inputs.length) ? inputs[i] : 0.0;
					weights[idx] = Mth.clamp(weights[idx] + NN_LEARNING_RATE * hiddenDelta * val, -5.0, 5.0);
					idx++;
				}
			}
		}

		public long[] toLongArray() {
			long[] arr = new long[weights.length];
			for (int i = 0; i < weights.length; i++) {
				arr[i] = Double.doubleToRawLongBits(weights[i]);
			}
			return arr;
		}

		public static TacticalNeuralNetwork fromLongArray(long[] arr) {
			if (arr == null || arr.length != TOTAL_WEIGHTS) return new TacticalNeuralNetwork();
			double[] w = new double[TOTAL_WEIGHTS];
			for (int i = 0; i < TOTAL_WEIGHTS; i++) {
				w[i] = Double.longBitsToDouble(arr[i]);
			}
			return new TacticalNeuralNetwork(w);
		}
	}

	public static class CombatExperience {
		public final double[] inputs;
		public final int planOrdinal;
		public final double reward;

		public CombatExperience(double[] inputs, int planOrdinal, double reward) {
			this.inputs = inputs;
			this.planOrdinal = planOrdinal;
			this.reward = reward;
		}
	}

	public static class ExperienceBuffer {
		private final CombatExperience[] buffer = new CombatExperience[32];
		private int head = 0;
		private int count = 0;

		public void add(CombatExperience exp) {
			buffer[head] = exp;
			head = (head + 1) % 32;
			if (count < 32) count++;
		}

		public double getAverageReward() {
			if (count == 0) return 0.0;
			double sum = 0;
			for (int i = 0; i < count; i++) {
				if (buffer[i] != null) sum += buffer[i].reward;
			}
			return sum / count;
		}

		public void replayMiniBatch(TacticalNeuralNetwork net, java.util.Random rnd) {
			if (count < 4) return;
			for (int i = 0; i < 4; i++) {
				CombatExperience exp = buffer[rnd.nextInt(count)];
				if (exp != null) {
					net.trainDelta(exp.inputs, exp.planOrdinal, exp.reward);
				}
			}
		}
	}

	public static class OpponentHabitModel {
		private final int[][] habits = new int[15][8];

		// Deeper combat habit tracking
		public final Map<String, Integer> actionSequences = new HashMap<>();
		public final List<String> recentPlayerActionWindow = new ArrayList<>();
		public final Map<String, Double> historicalActionFrequencies = new HashMap<>();
		public final Map<String, Double> recentActionFrequencies = new HashMap<>();
		public final Map<String, Double> baitObservations = new HashMap<>();
		public final Map<String, Double> genuineObservations = new HashMap<>();
		public final Map<String, Double> counterEffectiveness = new HashMap<>();
		public double adaptationShiftScore = 0.0;
		public double conditioningBaitProbability = 0.0;
		public int conditioningObservations = 0;
		public double rhythmAttackInterval = 20.0;
		public double rhythmAttackVariance = 5.0;

		public void recordReaction(int rotPlan, int reaction) {
			if (rotPlan >= 0 && rotPlan < 15 && reaction >= 0 && reaction < 8) {
				if (habits[rotPlan][reaction] < 100) {
					habits[rotPlan][reaction]++;
				}
			}
		}

		public void recordActionSequence(String action) {
			recentPlayerActionWindow.add(action);
			if (recentPlayerActionWindow.size() > 8) {
				recentPlayerActionWindow.remove(0);
			}
			historicalActionFrequencies.put(action, historicalActionFrequencies.getOrDefault(action, 0.0) + 1.0);
			recalculateFrequenciesAndAdaptation();

			if (recentPlayerActionWindow.size() >= 2) {
				String bigram = recentPlayerActionWindow.get(recentPlayerActionWindow.size() - 2) + "->" + action;
				actionSequences.put(bigram, actionSequences.getOrDefault(bigram, 0) + 1);
			}
			if (recentPlayerActionWindow.size() >= 3) {
				String trigram = recentPlayerActionWindow.get(recentPlayerActionWindow.size() - 3) + "->" + recentPlayerActionWindow.get(recentPlayerActionWindow.size() - 2) + "->" + action;
				actionSequences.put(trigram, actionSequences.getOrDefault(trigram, 0) + 1);
			}
		}

		private void recalculateFrequenciesAndAdaptation() {
			recentActionFrequencies.clear();
			for (String a : recentPlayerActionWindow) {
				recentActionFrequencies.put(a, recentActionFrequencies.getOrDefault(a, 0.0) + 1.0);
			}
			double totalHist = 0.0;
			for (double val : historicalActionFrequencies.values()) totalHist += val;
			if (totalHist >= 12.0 && recentPlayerActionWindow.size() >= 6) {
				double divergence = 0.0;
				for (Map.Entry<String, Double> entry : recentActionFrequencies.entrySet()) {
					double pRecent = entry.getValue() / recentPlayerActionWindow.size();
					double pHist = historicalActionFrequencies.getOrDefault(entry.getKey(), 0.0) / totalHist;
					divergence += Math.abs(pRecent - pHist);
				}
				adaptationShiftScore = divergence * 0.5;
			}
		}

		public void decayOldHabits(double decayFactor) {
			for (int i = 0; i < 15; i++) {
				for (int j = 0; j < 8; j++) {
					habits[i][j] = (int) Math.round(habits[i][j] * decayFactor);
				}
			}
			for (Map.Entry<String, Integer> entry : actionSequences.entrySet()) {
				entry.setValue((int) Math.round(entry.getValue() * decayFactor));
			}
			for (Map.Entry<String, Double> entry : historicalActionFrequencies.entrySet()) {
				entry.setValue(entry.getValue() * decayFactor);
			}
		}

		public void recordBaitEvidence(String action, boolean isDeliberateBait) {
			if (isDeliberateBait) {
				baitObservations.put(action, baitObservations.getOrDefault(action, 0.0) + 1.0);
			} else {
				genuineObservations.put(action, genuineObservations.getOrDefault(action, 0.0) + 1.0);
			}
			conditioningObservations++;
			double target = isDeliberateBait ? 1.0 : 0.0;
			conditioningBaitProbability = 0.85 * conditioningBaitProbability + 0.15 * target;
		}

		public double getBaitProbability(String action) {
			double baits = baitObservations.getOrDefault(action, 0.0);
			double genuine = genuineObservations.getOrDefault(action, 0.0);
			double total = baits + genuine;
			if (total < 2.0) return conditioningBaitProbability;
			return (baits + conditioningBaitProbability) / (total + 1.0);
		}

		public void recordPredictionFailure(String predictedAction, String actualAction) {
			String bigram = (!recentPlayerActionWindow.isEmpty() ? recentPlayerActionWindow.get(recentPlayerActionWindow.size() - 1) : "") + "->" + predictedAction;
			if (actionSequences.containsKey(bigram)) {
				actionSequences.put(bigram, Math.max(0, actionSequences.get(bigram) - 1));
			}
			recordActionSequence(actualAction);
		}

		public void recordCounterResult(String counterName, boolean success) {
			double prev = counterEffectiveness.getOrDefault(counterName, 0.5);
			double updated = success ? Math.min(1.0, prev + 0.15) : Math.max(0.05, prev - 0.15);
			counterEffectiveness.put(counterName, updated);
		}

		public double getCounterEffectiveness(String counterName) {
			return counterEffectiveness.getOrDefault(counterName, 0.5);
		}

		public String predictNextSequenceStep() {
			if (recentPlayerActionWindow.size() >= 2) {
				String key2 = recentPlayerActionWindow.get(recentPlayerActionWindow.size() - 2) + "->" + recentPlayerActionWindow.get(recentPlayerActionWindow.size() - 1);
				int maxCount = 0;
				String bestNext = "";
				for (Map.Entry<String, Integer> e : actionSequences.entrySet()) {
					if (e.getKey().startsWith(key2 + "->") && e.getValue() > maxCount) {
						maxCount = e.getValue();
						bestNext = e.getKey().substring((key2 + "->").length());
					}
				}
				if (maxCount >= 2) return bestNext;
			}
			if (!recentPlayerActionWindow.isEmpty()) {
				String key1 = recentPlayerActionWindow.get(recentPlayerActionWindow.size() - 1);
				int maxCount = 0;
				String bestNext = "";
				for (Map.Entry<String, Integer> e : actionSequences.entrySet()) {
					if (e.getKey().startsWith(key1 + "->") && !e.getKey().contains("->.*->") && e.getValue() > maxCount) {
						maxCount = e.getValue();
						bestNext = e.getKey().substring((key1 + "->").length());
					}
				}
				if (maxCount >= 3) return bestNext;
			}
			return "";
		}

		public void recordConditioningOutcome(boolean wasBait) {
			recordBaitEvidence("CONDITIONING", wasBait);
		}

		public double getHabitProbability(int rotPlan, int reaction) {
			if (rotPlan < 0 || rotPlan >= 15 || reaction < 0 || reaction >= 8) return 0.2;
			int total = 0;
			for (int r = 0; r < 8; r++) total += habits[rotPlan][r];
			if (total < 2) return 0.2;
			return (double) habits[rotPlan][reaction] / total;
		}

		public long[] serialize() {
			long[] data = new long[15];
			for (int i = 0; i < 15; i++) {
				long packed = 0;
				for (int j = 0; j < 8; j++) {
					packed |= ((long) (habits[i][j] & 0xFF) << (j * 8));
				}
				data[i] = packed;
			}
			return data;
		}

		public void deserialize(long[] data) {
			if (data == null || data.length != 15) return;
			for (int i = 0; i < 15; i++) {
				long packed = data[i];
				for (int j = 0; j < 8; j++) {
					habits[i][j] = (int) ((packed >> (j * 8)) & 0xFF);
				}
			}
		}
	}

	public static class WelfordTracker {
		public double count = 0.0;
		public double mean = 0.0;
		public double M2 = 0.0;

		public void update(double val) {
			count += 1.0;
			double delta = val - mean;
			mean += delta / count;
			double delta2 = val - mean;
			M2 += delta * delta2;
		}

		public double getVariance() {
			if (count < 2.0) return 0.0;
			return M2 / (count - 1.0);
		}

		public double getStdDev() {
			return Math.sqrt(getVariance());
		}

		public double calculateZScore(double val) {
			if (count < ANOMALY_MIN_SAMPLES) return 0.0;
			double std = getStdDev();
			if (std < 1e-4) std = 1e-4;
			return Math.abs(val - mean) / std;
		}
	}

	public static class PlayerBehaviorTracker {
		private static final Map<UUID, PlayerBehaviorTracker> TRACKERS = new HashMap<>();

		public WelfordTracker attackIntervalTracker = new WelfordTracker();
		public WelfordTracker distanceTracker = new WelfordTracker();
		public WelfordTracker lateralVelocityTracker = new WelfordTracker();
		public long lastAttackTick = 0;

		// Rhythm and pattern repetition tracking
		public int patternRepetitionStreak = 0;
		public String lastRecognizedHabitKey = "";
		public boolean patternBreakingDetected = false;
		public long patternBreakAlertTicks = 0;

		// Strafe and sprint cadence
		public long lastSprintToggleTick = 0;
		public int sprintTogglePulseCount = 0;
		public boolean lastSprinting = false;
		public double lastLateralVel = 0.0;
		public int lateralDirectionFlips = 0;
		public long lastDirectionFlipTick = 0;

		public static PlayerBehaviorTracker get(UUID playerUuid) {
			return TRACKERS.computeIfAbsent(playerUuid, k -> new PlayerBehaviorTracker());
		}

		public static void remove(UUID playerUuid) {
			TRACKERS.remove(playerUuid);
		}

		public boolean observe(long currentTick, double distance, boolean isAttack) {
			boolean isSurprise = false;
			if (distance > 0) {
				double zDist = distanceTracker.calculateZScore(distance);
				if (zDist >= SURPRISE_Z_SCORE_THRESHOLD) {
					isSurprise = true;
				}
				distanceTracker.update(distance);
			}

			if (isAttack) {
				if (lastAttackTick > 0 && currentTick > lastAttackTick) {
					double interval = (double) (currentTick - lastAttackTick);
					double zInt = attackIntervalTracker.calculateZScore(interval);
					if (zInt >= SURPRISE_Z_SCORE_THRESHOLD) {
						isSurprise = true;
						patternBreakingDetected = true;
						patternBreakAlertTicks = currentTick + 40;
					}
					attackIntervalTracker.update(interval);
				}
				lastAttackTick = currentTick;
			}
			return isSurprise;
		}

		public void observeMovementDetails(long currentTick, Player player, Entity rot, EntityObservation obs, OpponentHabitModel habitModel) {
			if (player == null || rot == null) return;
			boolean sprinting = player.isSprinting();
			if (sprinting != lastSprinting) {
				if (currentTick - lastSprintToggleTick <= 15) {
					sprintTogglePulseCount++;
					if (sprintTogglePulseCount >= 2 && obs != null) {
						obs.isWpingDetected = true;
					}
				} else {
					sprintTogglePulseCount = 0;
				}
				lastSprintToggleTick = currentTick;
				lastSprinting = sprinting;
			}

			Vec3 pVel = player.getDeltaMovement();
			Vec3 toRot = rot.position().subtract(player.position()).normalize();
			Vec3 rightVec = new Vec3(-toRot.z, 0, toRot.x);
			double lateral = pVel.dot(rightVec);
			lateralVelocityTracker.update(lateral);

			if ((lastLateralVel > 0.05 && lateral < -0.05) || (lastLateralVel < -0.05 && lateral > 0.05)) {
				if (currentTick - lastDirectionFlipTick <= 20) {
					lateralDirectionFlips++;
					if (obs != null) obs.lateralStrafeFlips = lateralDirectionFlips;
				} else {
					lateralDirectionFlips = 1;
				}
				lastDirectionFlipTick = currentTick;
			}
			lastLateralVel = lateral;

			// Spacing manipulation & reach testing
			double dist = player.distanceTo(rot);
			boolean inReachBand = dist >= 2.8 && dist <= 3.4;
			double radialVel = pVel.dot(toRot);
			if (inReachBand) {
				if (radialVel < -0.05 && !player.swinging) {
					if (obs != null) {
						obs.isSpacingFeint = true;
						obs.isReachTesting = true;
					}
				}
			} else if (dist < 2.5 || dist > 4.5) {
				if (obs != null) {
					obs.isSpacingFeint = false;
					obs.isReachTesting = false;
				}
			}

			// Strafing patterns: circle strafe vs erratic lateral toggling
			if (Math.abs(lateral) > 0.08) {
				double sign = Math.signum(lateral);
				if (obs != null) {
					if (sign == obs.lastLateralSign) {
						obs.sustainedLateralTicks++;
						if (obs.sustainedLateralTicks >= 10) {
							obs.isCircleStrafing = true;
							obs.isErraticStrafing = false;
						}
					} else {
						obs.sustainedLateralTicks = 1;
						obs.lastLateralSign = sign;
					}
				}
			} else {
				if (obs != null) {
					obs.sustainedLateralTicks = 0;
					obs.isCircleStrafing = false;
				}
			}
			if (lateralDirectionFlips >= 3 && currentTick - lastDirectionFlipTick <= 25) {
				if (obs != null) {
					obs.isErraticStrafing = true;
					obs.isCircleStrafing = false;
				}
			}

			// S-tapping detection
			if (player.swinging && radialVel < -0.06) {
				if (obs != null) obs.isStappingDetected = true;
			}

			// Shield baiting, crit baiting, movement baiting
			if (player.isBlocking()) {
				int useTicks = player.getTicksUsingItem();
				if (useTicks > 0 && useTicks <= 6 && dist > 3.0) {
					if (obs != null) obs.isShieldBaiting = true;
					if (habitModel != null) habitModel.recordBaitEvidence("SHIELD_BLOCK", true);
				}
			} else {
				if (obs != null) obs.isShieldBaiting = false;
			}

			if (!player.onGround() && pVel.y > 0.0 && dist > 4.2) {
				if (obs != null) obs.isCritBaiting = true;
				if (habitModel != null) habitModel.recordBaitEvidence("CRIT_ATTACK", true);
			} else if (player.onGround()) {
				if (obs != null) obs.isCritBaiting = false;
			}

			Vec3 lookVec = player.getLookAngle().normalize();
			double dotAway = lookVec.dot(toRot);
			if (dotAway < -0.70 && dist < 6.0) {
				if (obs != null && obs.ticksFacingRot == 0) {
					obs.isMovementBaiting = true;
					if (habitModel != null) habitModel.recordBaitEvidence("FLEE_BAIT", true);
				}
			} else {
				if (obs != null) obs.isMovementBaiting = false;
			}

			// Terrain, pillaring, and deliberate LOS breaks
			boolean hasLOS = rot instanceof LivingEntity rotLiv && rotLiv.hasLineOfSight(player);
			if (!hasLOS && dist < 12.0) {
				if (obs != null) obs.isDeliberateLOSBreak = true;
			} else {
				if (obs != null) obs.isDeliberateLOSBreak = false;
			}

			if (player.onGround() && obs != null && obs.lastPos != null && player.getY() > obs.lastPos.y + 0.9) {
				obs.ticksGroundedPillarAscent++;
				if (obs.ticksGroundedPillarAscent >= 2) obs.isVerticalPillaring = true;
			} else if (obs != null) {
				obs.ticksGroundedPillarAscent = 0;
				obs.isVerticalPillaring = false;
			}

			if (player.isInWater() || (player.level().getBlockState(player.blockPosition()).is(net.minecraft.world.level.block.Blocks.WATER))) {
				if (obs != null) obs.isWaterTrapping = true;
			} else if (obs != null) {
				obs.isWaterTrapping = false;
			}

			// Inventory & resource switching observables
			ItemStack mainHand = player.getMainHandItem();
			ItemStack offHand = player.getOffhandItem();
			String mainId = mainHand.getItem().toString();
			String offId = offHand.getItem().toString();
			if (obs != null) {
				if (!mainId.equals(obs.lastHeldMainItem) || !offId.equals(obs.lastHeldOffItem)) {
					if (currentTick - obs.lastItemSwapTick <= 20) {
						obs.itemSwapsInWindow++;
					} else {
						obs.itemSwapsInWindow = 1;
					}
					obs.lastItemSwapTick = currentTick;
					obs.lastHeldMainItem = mainId;
					obs.lastHeldOffItem = offId;
				}
				obs.isRapidHotbarSwapping = (obs.itemSwapsInWindow >= 2);
				obs.heldTotem = mainHand.is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING) || offHand.is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
				obs.heldShield = mainHand.is(net.minecraft.world.item.Items.SHIELD) || offHand.is(net.minecraft.world.item.Items.SHIELD);
				obs.isConsumingHeal = player.isUsingItem() && (player.getUseItem().is(net.minecraft.world.item.Items.GOLDEN_APPLE) || player.getUseItem().is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE) || player.getUseItem().getItem() instanceof net.minecraft.world.item.PotionItem);
			}

			// Track observable habit sequence
			String currentAction = "NEUTRAL";
			if (player.isBlocking()) currentAction = "SHIELD";
			else if (player.swinging) currentAction = "ATTACK";
			else if (!player.onGround() && player.getDeltaMovement().y > 0.1) currentAction = "JUMP";
			else if (player.isUsingItem()) currentAction = "USE_ITEM";
			else if (sprinting) currentAction = "SPRINT";

			if (!"NEUTRAL".equals(currentAction)) {
				if (currentAction.equals(lastRecognizedHabitKey)) {
					patternRepetitionStreak++;
				} else {
					if (habitModel != null) {
						habitModel.recordActionSequence(currentAction);
						if (habitModel.adaptationShiftScore > 0.35) {
							if (obs != null) obs.adaptationShiftDetected = true;
							habitModel.decayOldHabits(0.85);
						}
					}
					patternRepetitionStreak = 1;
					lastRecognizedHabitKey = currentAction;
				}
			}

			if (currentTick > patternBreakAlertTicks) {
				patternBreakingDetected = false;
			}
		}
	}

	public static class RoleAuction {
		public enum Role { TANK, FLANKER, CASTER }

		public static class RoleBid {
			public UUID rotUuid;
			public double bidUtility;
			public long expireTick;

			public RoleBid(UUID rotUuid, double utility, long expireTick) {
				this.rotUuid = rotUuid;
				this.bidUtility = utility;
				this.expireTick = expireTick;
			}
		}

		private static final Map<String, RoleBid> BIDS = new HashMap<>();

		public static void pruneStaleBids(long currentTick) {
			BIDS.entrySet().removeIf(entry -> entry.getValue() == null || currentTick > entry.getValue().expireTick + 100);
		}

		public static Role calculateRotRole(Entity rot, LivingEntity target, long currentTick) {
			if (rot == null || target == null || !target.isAlive()) return Role.TANK;

			if (countNearbyAlliedRots(rot) == 0) {
				return Role.TANK;
			}

			UUID targetUuid = target.getUUID();
			UUID rotUuid = rot.getUUID();
			double dist = rot.distanceTo(target);

			float health = rot instanceof LivingEntity liv ? liv.getHealth() : 20.0f;
			float maxHealth = rot instanceof LivingEntity liv ? liv.getMaxHealth() : 20.0f;
			double hpRatio = maxHealth > 0 ? health / maxHealth : 1.0;

			CompoundTag nbt = rot.getPersistentData();
			boolean teleportUnlocked = getB(rot, "unlocked_teleportation");
			boolean beamUnlocked = getB(rot, "unlocked_solar_beam");
			boolean sonicUnlocked = getB(rot, "unlocked_sonic_boom");

			double tankUtil = hpRatio * 40.0 + Math.max(0.0, (24.0 - dist) * 1.5);
			double flankerUtil = (teleportUnlocked ? 25.0 : 10.0) + (dist >= 3.0 && dist <= 12.0 ? 30.0 : 10.0);
			double casterUtil = ((beamUnlocked || sonicUnlocked) ? 35.0 : 5.0) + (dist >= 8.0 ? 30.0 : 10.0) + (1.0 - hpRatio) * 20.0;

			Role bestRole = Role.TANK;
			double maxWonUtil = -1.0;

			Role[] roles = Role.values();
			double[] utils = new double[]{tankUtil, flankerUtil, casterUtil};

			for (int i = 0; i < roles.length; i++) {
				Role role = roles[i];
				double u = utils[i];
				String key = targetUuid.toString() + "_" + role.name();
				RoleBid current = BIDS.get(key);

				if (current == null || currentTick > current.expireTick || rotUuid.equals(current.rotUuid) || u > current.bidUtility + 2.0) {
					BIDS.put(key, new RoleBid(rotUuid, u, currentTick + ROLE_AUCTION_DURATION_TICKS));
					if (u > maxWonUtil) {
						maxWonUtil = u;
						bestRole = role;
					}
				} else if (rotUuid.equals(current.rotUuid)) {
					if (u > maxWonUtil) {
						maxWonUtil = u;
						bestRole = role;
					}
				}
			}
			return bestRole;
		}
	}

	public static class RotHivemindSavedData extends SavedData {
		public static final String DATA_NAME = "rot_hivemind_data";
		public final Map<UUID, CompoundTag> playerMemories = new HashMap<>();

		public RotHivemindSavedData() {}

		public static RotHivemindSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
			RotHivemindSavedData data = new RotHivemindSavedData();
			if (tag != null && tag.contains("PlayerMemories")) {
				CompoundTag mems = tag.getCompound("PlayerMemories");
				for (String key : mems.getAllKeys()) {
					try {
						UUID uuid = UUID.fromString(key);
						data.playerMemories.put(uuid, mems.getCompound(key));
					} catch (Exception ignored) {}
				}
			}
			return data;
		}

		public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
			CompoundTag mems = new CompoundTag();
			for (Map.Entry<UUID, CompoundTag> entry : playerMemories.entrySet()) {
				mems.put(entry.getKey().toString(), entry.getValue());
			}
			tag.put("PlayerMemories", mems);
			return tag;
		}

		public CompoundTag getMemory(UUID playerUuid) {
			return playerMemories.computeIfAbsent(playerUuid, k -> new CompoundTag());
		}

		public void updateMemory(UUID playerUuid, CompoundTag data) {
			playerMemories.put(playerUuid, data);
			setDirty();
		}

		public static RotHivemindSavedData get(LevelAccessor world) {
			if (world instanceof ServerLevel serverLevel) {
				ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
				if (overworld != null) {
					return overworld.getDataStorage().computeIfAbsent(
						new SavedData.Factory<>(RotHivemindSavedData::new, RotHivemindSavedData::load, null),
						DATA_NAME
					);
				}
			}
			return null;
		}
	}

	public enum DominantResource {
		HEALTH_POOL,
		ARMOR_TOUGHNESS,
		TOTEM,
		SHIELD,
		HEALING,
		MOBILITY_ESCAPE,
		TERRAIN_COVER
	}

	public static class SurvivalResourceAnalysis {
		public DominantResource dominantResource = DominantResource.HEALTH_POOL;
		public double totalEffectiveHealth = 20.0;
		public int estimatedTotemCount = 0;
		public boolean hasShield = false;
		public boolean hasHealingAccess = false;
		public double mobilityScore = 0.0;
		public double terrainSafetyScore = 0.0;
	}

	public static SurvivalResourceAnalysis analyzeSurvivalResources(Entity rot, LivingEntity target) {
		SurvivalResourceAnalysis analysis = new SurvivalResourceAnalysis();
		if (target == null) return analysis;

		double health = target.getHealth();
		double armor = target.getArmorValue();
		double armorToughness = 0.0;
		try {
			var attr = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
			if (attr != null) armorToughness = attr.getValue();
		} catch (Exception ignored) {}

		double armorFactor = 1.0 + (armor * 0.04) + (armorToughness * 0.02);
		analysis.totalEffectiveHealth = health * armorFactor;

		if (target instanceof Player player) {
			int totems = 0;
			if (player.getMainHandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) totems++;
			if (player.getOffhandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) totems++;
			analysis.estimatedTotemCount = totems;
		}

		analysis.hasShield = target.isBlocking() 
			|| target.getMainHandItem().is(net.minecraft.world.item.Items.SHIELD) 
			|| target.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD);

		boolean isConsumingHeal = false;
		if (target.isUsingItem()) {
			ItemStack useItem = target.getUseItem();
			isConsumingHeal = useItem.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
				|| useItem.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE)
				|| useItem.getItem() instanceof net.minecraft.world.item.PotionItem;
		}
		analysis.hasHealingAccess = isConsumingHeal || (target.getHealth() < target.getMaxHealth() && target.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION));

		double speed = target.getDeltaMovement().horizontalDistance();
		analysis.mobilityScore = (target.isSprinting() ? 1.5 : 1.0) * (speed / 0.15);

		if (target.level() instanceof Level level && rot != null) {
			boolean inHazard = target.isInLava() || target.isOnFire();
			boolean hasCover = !target.hasLineOfSight(rot);
			analysis.terrainSafetyScore = (hasCover ? 2.0 : 0.0) - (inHazard ? 3.0 : 0.0);
		}

		if (analysis.estimatedTotemCount > 0 && health <= 15.0) {
			analysis.dominantResource = DominantResource.TOTEM;
		} else if (analysis.hasShield && (target.isBlocking() || health <= 14.0)) {
			analysis.dominantResource = DominantResource.SHIELD;
		} else if (analysis.hasHealingAccess) {
			analysis.dominantResource = DominantResource.HEALING;
		} else if (analysis.mobilityScore > 1.8 && speed > 0.25) {
			analysis.dominantResource = DominantResource.MOBILITY_ESCAPE;
		} else if (analysis.terrainSafetyScore > 1.5) {
			analysis.dominantResource = DominantResource.TERRAIN_COVER;
		} else if (armorFactor > 1.6) {
			analysis.dominantResource = DominantResource.ARMOR_TOUGHNESS;
		} else {
			analysis.dominantResource = DominantResource.HEALTH_POOL;
		}

		return analysis;
	}

	public enum TacticalChainStep {
		NONE,
		FORCE_DEFENSE,
		PUNISH_OPENING,
		DENY_ESCAPE,
		EXECUTE
	}

	public static class CombatProfile {
		public String entityTypeId = "";
		public int totalObservedAttacks = 0;
		public long lastAttackTick = 0;
		public double averageMeleeInterval = 40.0;
		public double averageProjectileInterval = 60.0;
		public double minInterval = 999.0;
		public double maxInterval = 0.0;
		public double intervalVariance = 15.0;

		public double averageChargeDuration = 25.0;
		public double averageRecoveryDuration = 15.0;
		public double preferredAttackRange = 3.5;
		public double maxAttackRange = 16.0;
		public double aoeRadiusEstimate = 4.0;
		public double preferredEngagementDistance = 3.5;
		public double preferredMovementSpeed = 0.15;

		public int meleeUsageCount = 0;
		public int projectileUsageCount = 0;
		public int beamUsageCount = 0;
		public int aoeUsageCount = 0;
		public int dashCount = 0;
		public int teleportCount = 0;
		public int verticalAttackCount = 0;
		public int airAttackCount = 0;

		public double averageComboLength = 1.0;
		public int comboCount = 0;
		public String lastAttackType = "";
		public Map<String, Integer> sequenceTransitions = new HashMap<>();

		public int attacksMissed = 0;
		public int attacksBlocked = 0;
		public int hitsDealt = 0;

		public double confidence = 0.0;

		public FightStyle fightStyle = FightStyle.HYBRID;
		public int currentPhase = 1;
		public double lastHealthRatio = 1.0;
		public double phaseAggressionBaseline = 40.0;

		public double recentInterval = 40.0;
		public double longTermInterval = 40.0;
		public List<String> recentAttackHistory = new ArrayList<>();

		public Map<String, Double> signalAlpha = new HashMap<>();
		public Map<String, Double> signalBeta = new HashMap<>();
		public Map<String, Double> defenseAlpha = new HashMap<>();
		public Map<String, Double> defenseBeta = new HashMap<>();
		public Map<String, Double> patternAlpha = new HashMap<>();
		public Map<String, Double> patternBeta = new HashMap<>();

		public TacticalNeuralNetwork neuralNet = new TacticalNeuralNetwork();
		public ExperienceBuffer experienceBuffer = new ExperienceBuffer();
		public OpponentHabitModel opponentHabits = new OpponentHabitModel();
		public PersonalityVector personality = new PersonalityVector();
		public boolean hasLoadedHivemindWeights = false;

		public TacticalChainStep activeChainStep = TacticalChainStep.NONE;
		public long chainStepStartTick = 0;
		public List<Vec3> recentEscapeDestinations = new ArrayList<>();

		public void recordExterminationProgress(String type, double score) {
			if ("SHIELD_BROKEN".equals(type)) {
				momentum = Math.min(100.0, momentum + 15.0);
				planSuccessRate.put(currentPlan, Math.min(1.0, planSuccessRate.getOrDefault(currentPlan, 0.5) + 0.12));
			} else if ("HEALING_INTERRUPTED".equals(type)) {
				momentum = Math.min(100.0, momentum + 18.0);
				planSuccessRate.put(currentPlan, Math.min(1.0, planSuccessRate.getOrDefault(currentPlan, 0.5) + 0.15));
			} else if ("TOTEM_POPPED".equals(type)) {
				momentum = Math.min(100.0, momentum + 25.0);
				planSuccessRate.put(currentPlan, Math.min(1.0, planSuccessRate.getOrDefault(currentPlan, 0.5) + 0.20));
			} else if ("ROUTE_DENIED".equals(type)) {
				momentum = Math.min(100.0, momentum + 10.0);
			} else if ("EXTERMINATION_SUCCESS".equals(type)) {
				momentum = 100.0;
				planSuccessRate.put(currentPlan, 1.0);
			} else if ("WHIFFED_COMMITMENT".equals(type)) {
				momentum = Math.max(0.0, momentum - 15.0);
				planSuccessRate.put(currentPlan, Math.max(0.05, planSuccessRate.getOrDefault(currentPlan, 0.5) - 0.12));
			}
		}

		public void advanceTacticalChain(TacticalChainStep nextStep, long tick) {
			this.activeChainStep = nextStep;
			this.chainStepStartTick = tick;
		}

		public void abandonTacticalPlan(Entity rot, String reason) {
			this.activeChainStep = TacticalChainStep.NONE;
			this.planStartTick = 0;
			this.experimentalCounter = "";
			if (rot != null) {
				putB(rot, "sentinel_plan_abandoned", true);
				putD(rot, "sentinel_plan_cooldown", 15.0);
			}
		}

		public Map<String, Double> patternConfidence = new HashMap<>();

		public Map<String, Double> defenseSuccessRates = new HashMap<>();

		public Map<String, Double> signalWeights = new HashMap<>();
		public Map<String, Integer> signalTruePositives = new HashMap<>();
		public Map<String, Integer> signalFalsePositives = new HashMap<>();

		public Map<String, Integer> defenseSuccesses = new HashMap<>();
		public Map<String, Integer> defenseAttempts = new HashMap<>();

		public enum EnemyTrait {
			LIFESTEAL, PASSIVE_REGEN, BURST_REGEN, PROJECTILE_REFLECTION, SHIELDING,
			DAMAGE_REFLECTION, THORNS, ARMOR_GROWTH, DAMAGE_REDUCTION, MAGIC_RESISTANCE,
			FIRE_RESISTANCE, FREEZE_RESISTANCE, KNOCKBACK_RESISTANCE, FLIGHT, HOVERING,
			TELEPORTATION, GRAVITY_MANIPULATION, LEVITATION, PULL_EFFECTS, PUSH_EFFECTS,
			BLINDNESS, SLOWNESS, BLEEDING, POISON, WITHER, DECAY, SUMMONING, ILLUSIONS,
			CLONING, REVIVAL, SECOND_PHASE, MULTIPLE_PHASES, TRANSFORMATION, SELF_BUFFING,
			ALLY_BUFFING, HEALING_ALLIES, PROJECTILE_SPAM, BEAM_SPECIALIST, AOE_SPECIALIST,
			COUNTER_ATTACKER, COMBO_SPECIALIST, ENVIRONMENTAL_MANIPULATION
		}

		public Map<EnemyTrait, Double> traitConfidence = new HashMap<>();
		public Map<Integer, Set<EnemyTrait>> phaseTraits = new HashMap<>();
		public List<String> successfulCounters = new ArrayList<>();
		public List<String> failedCounters = new ArrayList<>();
		public String currentStrategy = "BALANCED";
		public String experimentalCounter = "NONE";
		public double experimentalCounterEfficiency = 0.0;
		public long experimentalTrialStartTick = 0;
		public float rotHealthAtTrialStart = 0.0f;
		public float targetHealthAtTrialStart = 0.0f;
		public double lastObservedTargetHealth = -1.0;
		public long lastTargetDamageTick = 0;
		public long lastRotDamageTick = 0;

		public void increaseTraitConfidence(EnemyTrait trait, double delta) {
			double current = traitConfidence.getOrDefault(trait, 0.0);
			double updated = Math.min(1.0, current + delta);
			traitConfidence.put(trait, updated);
			if (updated >= 0.50) {
				phaseTraits.computeIfAbsent(currentPhase, k -> new HashSet<>()).add(trait);
			}
			evolveStrategy();
		}

		public void decreaseTraitConfidence(EnemyTrait trait, double delta) {
			double current = traitConfidence.getOrDefault(trait, 0.0);
			traitConfidence.put(trait, Math.max(0.0, current - delta));
			evolveStrategy();
		}

		public Set<EnemyTrait> getKnownTraits() {
			Set<EnemyTrait> known = new HashSet<>();
			for (Map.Entry<EnemyTrait, Double> entry : traitConfidence.entrySet()) {
				if (entry.getValue() >= 0.50) {
					known.add(entry.getKey());
				}
			}
			return known;
		}

		public String getKnownTraitsString() {
			Set<EnemyTrait> known = getKnownTraits();
			if (known.isEmpty()) return "UNKNOWN";
			List<String> list = new ArrayList<>();
			for (EnemyTrait t : known) list.add(t.name());
			return String.join(",", list);
		}

		public String getTraitConfidenceString() {
			if (traitConfidence.isEmpty()) return "NONE";
			List<String> parts = new ArrayList<>();
			for (Map.Entry<EnemyTrait, Double> entry : traitConfidence.entrySet()) {
				if (entry.getValue() > 0.05) {
					parts.add(entry.getKey().name() + ":" + String.format("%.2f", entry.getValue()));
				}
			}
			return parts.isEmpty() ? "NONE" : String.join(",", parts);
		}

		public void evolveStrategy() {
			Set<EnemyTrait> known = getKnownTraits();
			if (known.contains(EnemyTrait.LIFESTEAL)) {
				currentStrategy = "BURST_AND_DISENGAGE";
				setExperimentalCounter("SHORT_MELEE_TRADES");
			} else if (known.contains(EnemyTrait.PROJECTILE_REFLECTION)) {
				currentStrategy = "MELEE_DOMINANCE";
				setExperimentalCounter("SUPPRESS_PROJECTILES");
			} else if (known.contains(EnemyTrait.GRAVITY_MANIPULATION) || known.contains(EnemyTrait.LEVITATION) || known.contains(EnemyTrait.PULL_EFFECTS) || known.contains(EnemyTrait.PUSH_EFFECTS)) {
				currentStrategy = "GROUNDED_STABILITY";
				setExperimentalCounter("TELEPORT_REPOSITION");
			} else if (known.contains(EnemyTrait.FLIGHT) || known.contains(EnemyTrait.HOVERING)) {
				currentStrategy = "ANTI_AIR";
				setExperimentalCounter("GROUND_PULL_BEAM");
			} else if (known.contains(EnemyTrait.THORNS) || known.contains(EnemyTrait.DAMAGE_REFLECTION)) {
				currentStrategy = "HEAVY_BURST_SPACING";
				setExperimentalCounter("RANGE_SUPPRESSION");
			} else if (known.contains(EnemyTrait.SUMMONING) || known.contains(EnemyTrait.ILLUSIONS) || known.contains(EnemyTrait.CLONING)) {
				currentStrategy = "AOE_CLEAR";
				setExperimentalCounter("AOE_FOCUS");
			} else if (known.contains(EnemyTrait.SHIELDING) || known.contains(EnemyTrait.ARMOR_GROWTH) || known.contains(EnemyTrait.DAMAGE_REDUCTION)) {
				currentStrategy = "FLANK_AND_PUNISH";
				setExperimentalCounter("FLANK_GUARD_BREAK");
			} else if (known.contains(EnemyTrait.PASSIVE_REGEN) || known.contains(EnemyTrait.BURST_REGEN)) {
				currentStrategy = "CONTINUOUS_PRESSURE";
				setExperimentalCounter("BURST_BEFORE_REGEN");
			} else if (known.contains(EnemyTrait.BEAM_SPECIALIST) || known.contains(EnemyTrait.PROJECTILE_SPAM)) {
				currentStrategy = "EVASIVE_FLANK";
				setExperimentalCounter("LATERAL_DODGE");
			} else if (known.contains(EnemyTrait.COUNTER_ATTACKER) || known.contains(EnemyTrait.COMBO_SPECIALIST)) {
				currentStrategy = "BAIT_AND_PUNISH";
				setExperimentalCounter("INTERRUPT_TIMING");
			} else {
				currentStrategy = "BALANCED";
				setExperimentalCounter("NONE");
			}
		}

		public void setExperimentalCounter(String counter) {
			if (counter.equals(experimentalCounter)) return;
			if (failedCounters.contains(counter)) return;
			this.experimentalCounter = counter;
		}

		public void updateExperimentalTrial(Entity rot, LivingEntity target, long currentTick) {
			if (rot == null || target == null || experimentalCounter.equals("NONE")) return;
			if (experimentalTrialStartTick == 0) {
				experimentalTrialStartTick = currentTick;
				rotHealthAtTrialStart = rot instanceof LivingEntity liv ? liv.getHealth() : 0.0f;
				targetHealthAtTrialStart = target.getHealth();
				return;
			}

			if (currentTick - experimentalTrialStartTick >= 60) {
				float rotDamageTaken = (rot instanceof LivingEntity liv ? rotHealthAtTrialStart - liv.getHealth() : 0.0f);
				float targetDamageDealt = targetHealthAtTrialStart - target.getHealth();
				double netEfficiency = (double) targetDamageDealt - (double) rotDamageTaken;

				if (netEfficiency > 0.0) {
					if (!successfulCounters.contains(experimentalCounter)) {
						successfulCounters.add(experimentalCounter);
					}
					failedCounters.remove(experimentalCounter);
				} else if (netEfficiency < -2.0) {
					if (!failedCounters.contains(experimentalCounter)) {
						failedCounters.add(experimentalCounter);
					}
					successfulCounters.remove(experimentalCounter);
					experimentalCounter = "NONE";
				}
				experimentalTrialStartTick = currentTick;
				rotHealthAtTrialStart = rot instanceof LivingEntity liv ? liv.getHealth() : 0.0f;
				targetHealthAtTrialStart = target.getHealth();
			}
		}

		public double getMechanicProfileCompletion() {
			int knownCount = getKnownTraits().size();
			double traitProgress = Math.min(1.0, (double) knownCount / 5.0) * 50.0;
			double confidenceProgress = Math.min(1.0, confidence) * 30.0;
			double counterProgress = Math.min(1.0, (double) (successfulCounters.size() + failedCounters.size()) / 3.0) * 20.0;
			return Math.min(100.0, traitProgress + confidenceProgress + counterProgress);
		}

		public void observeMechanics(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, long currentTick) {
			if (target == null || !target.isAlive()) return;

			double currentTargetHealth = target.getHealth();

			if (lastObservedTargetHealth > 0) {
				double healthDiff = currentTargetHealth - lastObservedTargetHealth;
				putD(rot, "sentinel_target_regen_rate", Math.max(0.0, healthDiff));
				if (healthDiff > 0.4 && (currentTick - lastTargetDamageTick) > 20) {
					if (healthDiff > 3.0) {
						increaseTraitConfidence(EnemyTrait.BURST_REGEN, 0.25);
					} else {
						increaseTraitConfidence(EnemyTrait.PASSIVE_REGEN, 0.15);
					}
				}
			}

			if ((currentTick - lastRotDamageTick) <= 10 && currentTargetHealth > lastObservedTargetHealth + 0.3) {
				increaseTraitConfidence(EnemyTrait.LIFESTEAL, 0.30);
			}

			if (target.isBlocking() || target.isUsingItem()) {
				increaseTraitConfidence(EnemyTrait.SHIELDING, 0.15);
			}

			if (!target.onGround()) {
				Vec3 vel = target.getDeltaMovement();
				if (Math.abs(vel.y) < 0.08 && target.position().y > rot.position().y + 2.5) {
					increaseTraitConfidence(EnemyTrait.HOVERING, 0.15);
				} else if (vel.y > 0.05 && target.position().y > rot.position().y + 3.0) {
					increaseTraitConfidence(EnemyTrait.FLIGHT, 0.15);
				}
			}

			if (obs.lastPos != null) {
				double distMoved = target.position().distanceTo(obs.lastPos);
				if (distMoved > 6.0 && obs.lastSpeed < 1.0) {
					putD(rot, "sentinel_target_escape_score", 1.0);
					increaseTraitConfidence(EnemyTrait.TELEPORTATION, 0.35);
				}
			}
			putD(rot, "sentinel_target_escape_score", Math.max(0.0, getD(rot, "sentinel_target_escape_score") - 0.02));

			if (rot instanceof LivingEntity rotLiv) {
				if (rotLiv.hasEffect(MobEffects.LEVITATION)) {
					increaseTraitConfidence(EnemyTrait.LEVITATION, 0.35);
					increaseTraitConfidence(EnemyTrait.GRAVITY_MANIPULATION, 0.25);
				}
				if (rotLiv.hasEffect(MobEffects.BLINDNESS)) increaseTraitConfidence(EnemyTrait.BLINDNESS, 0.30);
				if (rotLiv.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) increaseTraitConfidence(EnemyTrait.SLOWNESS, 0.30);
				if (rotLiv.hasEffect(MobEffects.POISON)) increaseTraitConfidence(EnemyTrait.POISON, 0.30);
				if (rotLiv.hasEffect(MobEffects.WITHER)) increaseTraitConfidence(EnemyTrait.WITHER, 0.30);
			}

			if ((currentTick - lastTargetDamageTick) <= 5) {
				double targetSpeed = target.getDeltaMovement().horizontalDistance();
				if (targetSpeed < 0.03 && rot.distanceTo(target) < 3.0) {
					increaseTraitConfidence(EnemyTrait.KNOCKBACK_RESISTANCE, 0.15);
				}
			}

			if (beamUsageCount >= 3) increaseTraitConfidence(EnemyTrait.BEAM_SPECIALIST, 0.25);
			if (projectileUsageCount >= 5) increaseTraitConfidence(EnemyTrait.PROJECTILE_SPAM, 0.25);
			if (aoeUsageCount >= 4) increaseTraitConfidence(EnemyTrait.AOE_SPECIALIST, 0.25);
			if (attacksBlocked >= 3) increaseTraitConfidence(EnemyTrait.COUNTER_ATTACKER, 0.25);
			if (averageComboLength >= 2.0) increaseTraitConfidence(EnemyTrait.COMBO_SPECIALIST, 0.25);

			lastObservedTargetHealth = currentTargetHealth;
			updateExperimentalTrial(rot, target, currentTick);
		}

		public enum TacticalPlan {
			AGGRESSIVE_PRESSURE, BURST_DAMAGE, HIT_AND_RUN, MAINTAIN_DISTANCE, COUNTER_FOCUS,
			ATTRITION, DEFENSIVE_RECOVERY, INTERRUPT_SPECIALIST, PROJECTILE_SUPPRESSION,
			GROUND_CONTROL, MOBILITY_WARFARE, SURVIVAL, EXPERIMENTAL, HYBRID, PHASE_DISPLACEMENT
		}

		public TacticalPlan currentPlan = TacticalPlan.AGGRESSIVE_PRESSURE;
		public Map<TacticalPlan, Double> planConfidence = new HashMap<>();
		public Map<TacticalPlan, Double> planSuccessRate = new HashMap<>();
		public Map<TacticalPlan, Integer> planUsageCount = new HashMap<>();
		public double momentum = 0.0;
		public String environmentState = "OPEN_SPACE";
		public String planReason = "BALANCED_ENGAGEMENT";
		public boolean isExperimentingTactics = false;
		public TacticalPlan experimentPlan = null;
		public long lastPlannerUpdateTick = 0;
		public long planStartTick = 0;
		public float rotHealthAtPlanStart = 0.0f;
		public float targetHealthAtPlanStart = 0.0f;

		public double[] constructNeuralInputs(Entity rot, LivingEntity target) {
			return constructNeuralInputs(rot, target, 0.0);
		}

		public double[] constructNeuralInputs(Entity rot, LivingEntity target, double threatScore) {
			CombatContext ctx = getCombatContext(rot, target);
			double[] inputs = new double[TacticalNeuralNetwork.INPUT_SIZE];
			if (rot == null || target == null) return inputs;

			float rotHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getHealth() : 20.0f;
			float rotMaxHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getMaxHealth() : 20.0f;
			float rotHealthPct = rotMaxHealth > 0 ? rotHealth / rotMaxHealth : 1.0f;

			inputs[0] = Math.min(1.0, rot.distanceTo(target) / 32.0);
			inputs[1] = rotHealthPct;
			inputs[2] = Math.max(-1.0, Math.min(1.0, momentum / 100.0));
			inputs[3] = Math.min(1.0, threatScore / 100.0);
			inputs[4] = getEnvOrdinal(environmentState) / 4.0;
			inputs[5] = fightStyle != null ? fightStyle.ordinal() / 10.0 : 0.0;
			inputs[6] = Math.max(0.0, Math.min(1.0, getRotPersistentDouble(rot, "sentinel_recent_hit_rate", 0.5)));
			inputs[7] = Math.max(0.0, Math.min(1.0, getRotPersistentDouble(rot, "sentinel_target_dodge_rate", 0.2)));
			inputs[8] = Math.max(0.0, Math.min(1.0, getRotPersistentDouble(rot, "sentinel_signal_confidence", 50.0) / 100.0));
			inputs[9] = (ctx != null && ctx.isJumpCritIncoming) ? 1.0 : 0.0;
			inputs[10] = (ctx != null && ctx.incomingProjectileDistance > 0.0) ? Math.max(0.0, 1.0 - ctx.incomingProjectileDistance / 16.0) : 0.0;
			inputs[11] = (ctx != null) ? Math.min(1.0, ctx.nearbyTargetCount / 5.0) : 0.2;
			inputs[12] = (ctx != null && ctx.targetNearLedgeOrHazard) ? 1.0 : 0.0;
			inputs[13] = (ctx != null && ctx.isEnclosedSpace) ? 1.0 : 0.0;
			inputs[14] = Math.min(1.0, target.getArmorValue() / 20.0);
			inputs[15] = Math.max(0.0, 1.0 - (target.getHealth() / target.getMaxHealth()));
			inputs[16] = getD(rot, "rot_phase_cooldown") <= 0.0 ? 1.0 : 0.0;
			inputs[17] = Math.min(1.0, getD(rot, "rot_phase_mastery"));

			double sustainedHits = getD(rot, "sentinel_sustained_bullet_hits");
			inputs[18] = Math.max(0.0, Math.min(1.0, sustainedHits / 8.0));

			double crosshairLock = 0.0;
			Vec3 lookVec = target.getLookAngle().normalize();
			Vec3 dirToRot = rot.position().subtract(target.position()).normalize();
			double dot = lookVec.dot(dirToRot);
			if (dot > 0) crosshairLock = Math.max(0.0, Math.min(1.0, dot));
			inputs[19] = crosshairLock;

			boolean hasLos = rot instanceof LivingEntity rLiv && rLiv.hasLineOfSight(target);
			inputs[20] = hasLos ? 1.0 : 0.0;

			double reloadStall = 0.0;
			double lastRpmTick = getD(rot, "sentinel_last_high_rpm_tick");
			long currentTickVal = rot.level() instanceof Level lvl ? lvl.getGameTime() : rot.tickCount;
			if (lastRpmTick > 0 && (currentTickVal - lastRpmTick) < 50 && !target.isUsingItem() && !target.swinging) {
				reloadStall = Math.max(0.0, 1.0 - ((currentTickVal - lastRpmTick) / 50.0));
			}
			inputs[21] = reloadStall;

			Vec3 targetVelocity = target.getDeltaMovement();
			Vec3 targetToRot = rot.position().subtract(target.position()).normalize();
			Vec3 horizontalToRot = new Vec3(targetToRot.x, 0.0, targetToRot.z).normalize();
			Vec3 horizontalVelocity = new Vec3(targetVelocity.x, 0.0, targetVelocity.z);
			inputs[22] = Math.max(-1.0, Math.min(1.0, horizontalVelocity.dot(horizontalToRot) / 0.35));
			inputs[23] = Math.max(-1.0, Math.min(1.0, (horizontalVelocity.x * horizontalToRot.z - horizontalVelocity.z * horizontalToRot.x) / 0.35));
			inputs[24] = Math.min(1.0, getD(rot, "ai_target_shield_ticks") / 40.0);
			inputs[25] = Math.min(1.0, getD(rot, "ai_target_sprint_away_ticks") / 60.0);
			inputs[26] = target.isUsingItem() ? Math.min(1.0, target.getTicksUsingItem() / 40.0) : 0.0;
			inputs[27] = Math.min(1.0, getD(rot, "sentinel_global_ability_cooldown") / 100.0);
			inputs[28] = Math.min(1.0, getAvailableAbilities(rot).size() / 8.0);
			inputs[29] = Math.min(1.0, getD(rot, "sentinel_heavy_punch_misses") / 4.0);
			inputs[30] = Math.max(-1.0, Math.min(1.0, getD(rot, "ai_distance_trend")));
			inputs[31] = Math.max(-1.0, Math.min(1.0, target.getDeltaMovement().y / 0.35));
			inputs[32] = Math.min(1.0, getD(rot, "sentinel_recent_damage") / 40.0);
			inputs[33] = (ctx != null && ctx.isHealing) ? 1.0 : 0.0;
			inputs[34] = isRotChannelingAbility(rot) ? 1.0 : 0.0;
			double alliedRotCount = 0.0;
			if (rot.level() instanceof Level level) {
				alliedRotCount = level.getEntitiesOfClass(RotEntity.class, rot.getBoundingBox().inflate(16.0), ally -> ally != rot && ally.isAlive() && ally instanceof Mob allyMob && allyMob.getTarget() == target).size();
			}
			inputs[35] = Math.min(1.0, alliedRotCount / 4.0);
			String damageCategory = getRotPersistentString(rot, "sentinel_last_damage_category", "NONE");
			inputs[36] = "PROJECTILE".equals(damageCategory) ? 1.0 : "MAGIC".equals(damageCategory) ? 0.8 : "EXPLOSION".equals(damageCategory) ? 0.7 : "MELEE".equals(damageCategory) ? 0.5 : 0.0;
			inputs[37] = Math.max(-1.0, Math.min(1.0, getD(rot, "sentinel_attack_outcome_score")));
			long lastSuccessTick = (long) getD(rot, "sentinel_last_target_damage_tick");
			long nowTick = rot.level() instanceof Level level ? level.getGameTime() : rot.tickCount;
			inputs[38] = lastSuccessTick > 0 ? Math.max(0.0, 1.0 - (nowTick - lastSuccessTick) / 200.0) : 0.0;
			inputs[39] = Math.min(1.0, getD(rot, "sentinel_target_regen_rate") / 4.0);
			inputs[40] = Math.min(1.0, getD(rot, "sentinel_target_escape_score"));
			inputs[41] = Math.min(1.0, alliedRotCount / 4.0) * ("TANK".equals(getRotPersistentString(rot, "sentinel_assigned_role", "TANK")) ? 0.5 : 1.0);
			inputs[42] = Math.max(0.0, Math.min(1.0, getRotPersistentDouble(rot, "sentinel_defense_success_rate", 0.5)));
			float maxHealthForRisk = rot instanceof LivingEntity rotLiving ? rotLiving.getMaxHealth() : 20.0f;
			inputs[43] = Math.max(0.0, Math.min(1.0, ctx.expectedIncomingDamage / Math.max(1.0f, maxHealthForRisk)));

			inputs[44] = (target.isInWater() || target.isUnderWater()) ? 1.0 : 0.0;

			inputs[45] = target.isPassenger() ? 1.0 : 0.0;

			double targetAttackCadence = getD(rot, "ai_target_attack_cadence");
			inputs[46] = Math.max(0.0, Math.min(1.0, targetAttackCadence / 20.0));

			inputs[47] = Math.min(1.0, target.getActiveEffects().size() / 5.0);

			boolean learnedSuperheat = getB(rot, "unlocked_water_evaporation")
					|| getB(rot, "unlocked_solar_beam")
					|| getB(rot, "taken_fire_damage");
			inputs[48] = (learnedSuperheat && getD(rot, "rot_superheat_cd") <= 0.0) ? 1.0 : 0.0;

			Vec3 tgtLook = target.getLookAngle().normalize();
			Vec3 tgtToRotVec = rot.position().subtract(target.position()).normalize();
			inputs[49] = Math.max(-1.0, Math.min(1.0, tgtLook.dot(tgtToRotVec)));

			inputs[50] = isTargetHighlyDangerous(target) ? 1.0 : 0.0;

			inputs[51] = Math.min(1.0, getD(rot, "adaptation_level") / 10.0);

			boolean hasTotem = target.getOffhandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING) || target.getMainHandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
			inputs[52] = hasTotem ? 1.0 : 0.0;

			// Index 53: INPUT_TARGET_HELD_MACE
			boolean holdsMace = false;
			try {
				holdsMace = target.getMainHandItem().getItem().toString().contains("mace") || target.getOffhandItem().getItem().toString().contains("mace");
			} catch (Exception e) {}
			inputs[53] = holdsMace ? 1.0 : 0.0;

			// Index 54: INPUT_TARGET_MACE_POTENTIAL_DAMAGE (scaled by health)
			double potentialMaceDmg = holdsMace ? (6.0 + target.fallDistance * 3.0) : 0.0;
			inputs[54] = Math.min(1.0, potentialMaceDmg / maxHealthForRisk);

			// Index 55: INPUT_TARGET_VERTICAL_VELOCITY
			inputs[55] = Math.max(-1.0, Math.min(1.0, target.getDeltaMovement().y / 0.5));

			// Index 56: INPUT_TARGET_AIRTIME_TICKS (estimated via fallDistance)
			inputs[56] = target.onGround() ? 0.0 : Math.min(1.0, target.fallDistance / 10.0);

			// Index 57: INPUT_TARGET_XZ_REL_POSITION
			Vec3 relPos = target.position().subtract(rot.position());
			double relXz = Math.sqrt(relPos.x * relPos.x + relPos.z * relPos.z);
			inputs[57] = Math.min(1.0, relXz / 16.0);

			// Index 58: INPUT_TARGET_LANDING_EST_DISTANCE
			double vy = target.getDeltaMovement().y;
			double landingEstDist = relXz;
			if (vy < -0.05 && target.getY() > findGroundY(rot.level(), target)) {
				double ticksToGround = (target.getY() - findGroundY(rot.level(), target)) / Math.abs(vy);
				double estX = target.getX() + target.getDeltaMovement().x * ticksToGround;
				double estZ = target.getZ() + target.getDeltaMovement().z * ticksToGround;
				landingEstDist = Math.sqrt(Math.pow(estX - rot.getX(), 2) + Math.pow(estZ - rot.getZ(), 2));
			}
			inputs[58] = Math.min(1.0, landingEstDist / 16.0);

			// Index 59: INPUT_ROT_TELEPORT_READY
			inputs[59] = (getD(rot, K_TP_DODGE_CD) <= 0 && getB(rot, "unlocked_teleportation")) ? 1.0 : 0.0;

			// Index 60: INPUT_ROT_STABILITY_PERCENT
			inputs[60] = rotHealthPct;

			// Index 61: INPUT_TARGET_HORIZONTAL_ACCEL
			double curSpeedSq = target.getDeltaMovement().x * target.getDeltaMovement().x + target.getDeltaMovement().z * target.getDeltaMovement().z;
			double prevSpeedSq = getD(rot, "sentinel_target_prev_speed_sq");
			putD(rot, "sentinel_target_prev_speed_sq", curSpeedSq);
			double accelDiff = Math.sqrt(curSpeedSq) - Math.sqrt(prevSpeedSq);
			inputs[61] = Math.max(-1.0, Math.min(1.0, accelDiff / 0.2));

			// Index 62: INPUT_TARGET_STRAFE_TREND
			Vec3 tgtVelHorizontal = new Vec3(target.getDeltaMovement().x, 0, target.getDeltaMovement().z);
			if (tgtVelHorizontal.lengthSqr() > 1E-4) {
				tgtVelHorizontal = tgtVelHorizontal.normalize();
				double strafeDot = tgtLook.x * tgtVelHorizontal.z - tgtLook.z * tgtVelHorizontal.x;
				inputs[62] = Math.max(-1.0, Math.min(1.0, strafeDot));
			} else {
				inputs[62] = 0.0;
			}

			// Index 63: INPUT_TARGET_MOVEMENT_PREDICTABILITY
			double targetDodgeRate = getRotPersistentDouble(rot, "sentinel_target_dodge_rate", 0.2);
			inputs[63] = Math.max(0.0, Math.min(1.0, 1.0 - targetDodgeRate));

			// Index 64: INPUT_TARGET_VULNERABILITY_WINDOW
			boolean isTargetVuln = !target.onGround() 
					|| target.getTicksUsingItem() > 0 
					|| (target instanceof Player p && p.getCooldowns().isOnCooldown(net.minecraft.world.item.Items.SHIELD))
					|| target.swingTime > 0;
			inputs[64] = isTargetVuln ? 1.0 : 0.0;

			// Index 65: INPUT_TARGET_ATTACK_WINDUP
			inputs[65] = target.swingTime > 0 ? Math.min(1.0, target.swingTime / 6.0) : 0.0;

			// Index 66: INPUT_TARGET_NEXT_ATTACK_PROBABILITY
			inputs[66] = Math.min(1.0, targetAttackCadence / 15.0);

			// Index 67: INPUT_TARGET_INVULNERABILITY_REMAINING
			inputs[67] = Math.min(1.0, target.invulnerableTime / 20.0);

			// Index 68: INPUT_TARGET_ADAPTATION_FACTOR
			inputs[68] = Math.min(1.0, getAdaptationMultiplier(rot) / 3.0);

			// Index 69: INPUT_BEST_ATTACK_EFFECTIVENESS
			double outcomeScore = getD(rot, "sentinel_attack_outcome_score");
			inputs[69] = Math.max(0.0, Math.min(1.0, (outcomeScore + 1.0) / 2.0));

			// Index 70: INPUT_COMBO_CONTINUATION_VALUE
			boolean doingCombo = isDoingCombo(rot);
			inputs[70] = (doingCombo && target.distanceTo(rot) < 4.5) ? 1.0 : 0.0;

			// Index 71: INPUT_COMBO_INTERRUPT_RISK
			inputs[71] = (holdsMace && !target.onGround()) ? 1.0 : 0.0;

			// Index 72: INPUT_FUTURE_DISTANCE_5_TICKS
			Vec3 rotPos = rot.position();
			Vec3 rotVel = rot.getDeltaMovement();
			Vec3 tgtPos = target.position();
			Vec3 tgtVel = target.getDeltaMovement();
			Vec3 rotFuture5 = rotPos.add(rotVel.scale(5.0));
			Vec3 tgtFuture5 = tgtPos.add(tgtVel.scale(5.0));
			inputs[72] = Math.min(1.0, rotFuture5.distanceTo(tgtFuture5) / 32.0);

			// Index 73: INPUT_FUTURE_DISTANCE_10_TICKS
			Vec3 rotFuture10 = rotPos.add(rotVel.scale(10.0));
			Vec3 tgtFuture10 = tgtPos.add(tgtVel.scale(10.0));
			inputs[73] = Math.min(1.0, rotFuture10.distanceTo(tgtFuture10) / 32.0);

			// Index 74: INPUT_FUTURE_DISTANCE_20_TICKS
			Vec3 rotFuture20 = rotPos.add(rotVel.scale(20.0));
			Vec3 tgtFuture20 = tgtPos.add(tgtVel.scale(20.0));
			inputs[74] = Math.min(1.0, rotFuture20.distanceTo(tgtFuture20) / 32.0);

			// Index 75: INPUT_TARGET_KNOCKBACK_SUSCEPTIBILITY
			inputs[75] = (target.isBlocking() || target.isPassenger()) ? 0.2 : 1.0;

			// Index 76: INPUT_TIME_SINCE_SUCCESS
			inputs[76] = lastSuccessTick > 0 ? Math.min(1.0, (nowTick - lastSuccessTick) / 200.0) : 1.0;

			// Index 77: INPUT_VERTICAL_ADVANTAGE
			inputs[77] = Math.max(-1.0, Math.min(1.0, (rot.getY() - target.getY()) / 8.0));

			// Index 78: INPUT_TARGET_ESCAPE_QUALITY
			inputs[78] = (ctx != null && ctx.targetNearLedgeOrHazard) ? 0.2 : 0.8;

			// Index 79: INPUT_TACTICAL_PLAN_CONFIDENCE
			inputs[79] = Math.max(0.0, Math.min(1.0, getRotPersistentDouble(rot, "sentinel_signal_confidence", 50.0) / 100.0));

			// Index 80: INPUT_EXPERIENCE_AVG_REWARD
			inputs[80] = Math.max(-1.0, Math.min(1.0, experienceBuffer.getAverageReward() / 5.0));

			// Index 81: INPUT_HABIT_PREDICTION_PROB
			inputs[81] = opponentHabits.getHabitProbability(currentPlan.ordinal(), 0);

			// Index 82: INPUT_TARGET_BLOCK_FREQUENCY
			inputs[82] = target.isBlocking() ? 1.0 : 0.0;

			// Index 83: INPUT_TARGET_AIR_FREQUENCY
			inputs[83] = target.onGround() ? 0.0 : 1.0;

			// Index 84: INPUT_TARGET_USING_ITEM
			inputs[84] = target.isUsingItem() ? Math.min(1.0, target.getTicksUsingItem() / 30.0) : 0.0;

			// Index 85: INPUT_TARGET_SWING_ACTIVITY
			inputs[85] = target.swingTime > 0 ? 1.0 : 0.0;

			// Index 86: INPUT_ROT_COMBO_ACTIVE
			inputs[86] = isDoingCombo(rot) ? 1.0 : 0.0;

			// Index 87: INPUT_ROT_RECENT_DAMAGE_TAKEN
			inputs[87] = Math.min(1.0, getD(rot, "sentinel_recent_damage") / 30.0);

			// Index 88: INPUT_ROT_ATTACK_OUTCOME_SCORE
			inputs[88] = Math.max(-1.0, Math.min(1.0, getD(rot, "sentinel_attack_outcome_score")));

			// Index 89: INPUT_TARGET_REGEN_RATE
			inputs[89] = Math.min(1.0, getD(rot, "sentinel_target_regen_rate") / 5.0);

			// Index 90: INPUT_TARGET_ESCAPE_SCORE
			inputs[90] = Math.min(1.0, getD(rot, "sentinel_target_escape_score"));

			// Index 91: INPUT_TARGET_MACE_THREAT
			inputs[91] = (holdsMace && !target.onGround() && target.getDeltaMovement().y < -0.05) ? 1.0 : 0.0;

			// Index 92: INPUT_PREDICTION_ACCURACY_SCORE
			inputs[92] = Math.max(0.0, Math.min(1.0, getRotPersistentDouble(rot, "sentinel_signal_confidence", 50.0) / 100.0));

			// Index 93: INPUT_ROT_UPPERCUT_ACTIVE
			inputs[93] = getD(rot, "sentinel_uppercut_launch_ticks") > 0 ? 1.0 : 0.0;

			// Index 94: INPUT_TARGET_EFFECTS_COUNT
			inputs[94] = Math.min(1.0, target.getActiveEffects().size() / 4.0);

			// Index 95: INPUT_ROT_HEALTH_RATIO
			inputs[95] = rotHealthPct;

			return inputs;
		}

		public void updateTacticalPlan(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, long currentTick, double threatScore) {
			if (rot == null || target == null || !target.isAlive()) return;
			if (currentTick - lastPlannerUpdateTick < 15) return;
			lastPlannerUpdateTick = currentTick;

			environmentState = analyzeEnvironment(world, rot, target);
			CombatContext ctx = getCombatContext(rot, target);

			float rotHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getHealth() : 20.0f;
			float rotMaxHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getMaxHealth() : 20.0f;
			float targetHealth = target.getHealth();

			if (planStartTick > 0 && currentTick - planStartTick >= 60) {
				float rotDmgTaken = rotHealthAtPlanStart - rotHealth;
				float targetDmgDealt = targetHealthAtPlanStart - targetHealth;
				double deltaMomentum = (targetDmgDealt * 3.0) - (rotDmgTaken * 4.0);
				momentum = Math.max(-100.0, Math.min(100.0, momentum + deltaMomentum));

				double planEfficiency = targetDmgDealt - rotDmgTaken;
				double reward = (targetDmgDealt * 3.5) - (rotDmgTaken * 4.0);
				if (targetDmgDealt > 0.0) reward += 2.0;
				if (rotDmgTaken == 0.0 && targetDmgDealt > 0.0) reward += 3.0;
				if (!target.isAlive()) reward += 15.0;

				double oldRate = planSuccessRate.getOrDefault(currentPlan, 0.50);
				double newRate = Math.max(0.05, Math.min(1.0, oldRate + (planEfficiency > 0 ? 0.08 : -0.08)));
				planSuccessRate.put(currentPlan, newRate);

				double[] nnInputs = constructNeuralInputs(rot, target, threatScore);
				neuralNet.trainDelta(nnInputs, currentPlan.ordinal(), reward);

				CombatExperience exp = new CombatExperience(nnInputs, currentPlan.ordinal(), reward);
				experienceBuffer.add(exp);
				experienceBuffer.replayMiniBatch(neuralNet, new java.util.Random());

				int targetReaction = 0;
				if (target.isBlocking()) targetReaction = 1;
				else if (!target.onGround()) targetReaction = 2;
				else if (target.isUsingItem()) targetReaction = 3;
				else if (target.swingTime > 0) targetReaction = 4;
				opponentHabits.recordReaction(currentPlan.ordinal(), targetReaction);

				PersonalityVector pVec = PersonalityVector.loadFromNbt(rot.getPersistentData());
				if (planEfficiency > 0.0) {
					pVec.drift(+0.05, -0.02, +0.03, -0.02);
				} else {
					pVec.drift(-0.02, +0.05, -0.03, +0.04);
				}
				pVec.saveToNbt(rot.getPersistentData());

				if (target instanceof Player player) {
					RotHivemindSavedData hivemind = RotHivemindSavedData.get(world);
					if (hivemind != null) {
						CompoundTag mem = hivemind.getMemory(player.getUUID());
						mem.putLongArray("RotNNWeights", neuralNet.toLongArray());
						mem.putLongArray("RotHabits", opponentHabits.serialize());
						hivemind.updateMemory(player.getUUID(), mem);
					}
				}

				if (isExperimentingTactics && experimentPlan != null) {
					double expRate = planSuccessRate.getOrDefault(experimentPlan, 0.50);
					planSuccessRate.put(experimentPlan, Math.max(0.05, Math.min(1.0, expRate + (planEfficiency > 0 ? 0.12 : -0.12))));
					isExperimentingTactics = false;
					experimentPlan = null;
				}

				planStartTick = currentTick;
				rotHealthAtPlanStart = rotHealth;
				targetHealthAtPlanStart = targetHealth;
			} else if (planStartTick == 0) {
				planStartTick = currentTick;
				rotHealthAtPlanStart = rotHealth;
				targetHealthAtPlanStart = targetHealth;
			}

			Set<EnemyTrait> known = getKnownTraits();
			Map<TacticalPlan, Double> weights = new HashMap<>();
			for (TacticalPlan p : TacticalPlan.values()) {
				weights.put(p, planSuccessRate.getOrDefault(p, 0.50) * 10.0);
			}

			double[] nnInputs = constructNeuralInputs(rot, target, threatScore);
			double[] hiddenOut = new double[TacticalNeuralNetwork.HIDDEN_SIZE];
			double[] nnScores = neuralNet.forward(nnInputs, hiddenOut);
			for (TacticalPlan p : TacticalPlan.values()) {
				weights.put(p, weights.get(p) + nnScores[p.ordinal()] * CONTEXT_SCORE_WEIGHT_NN);
			}

			PersonalityVector pVec = PersonalityVector.loadFromNbt(rot.getPersistentData());
			weights.put(TacticalPlan.AGGRESSIVE_PRESSURE, weights.get(TacticalPlan.AGGRESSIVE_PRESSURE) + pVec.aggression * 15.0);
			weights.put(TacticalPlan.BURST_DAMAGE, weights.get(TacticalPlan.BURST_DAMAGE) + pVec.aggression * 10.0);
			weights.put(TacticalPlan.MAINTAIN_DISTANCE, weights.get(TacticalPlan.MAINTAIN_DISTANCE) + pVec.patience * 15.0);
			weights.put(TacticalPlan.COUNTER_FOCUS, weights.get(TacticalPlan.COUNTER_FOCUS) + pVec.patience * 10.0 + pVec.spite * 10.0);
			weights.put(TacticalPlan.DEFENSIVE_RECOVERY, weights.get(TacticalPlan.DEFENSIVE_RECOVERY) + pVec.patience * 12.0);
			weights.put(TacticalPlan.INTERRUPT_SPECIALIST, weights.get(TacticalPlan.INTERRUPT_SPECIALIST) + pVec.spite * 15.0);
			weights.put(TacticalPlan.PROJECTILE_SUPPRESSION, weights.get(TacticalPlan.PROJECTILE_SUPPRESSION) + pVec.spite * 10.0);
			weights.put(TacticalPlan.EXPERIMENTAL, weights.get(TacticalPlan.EXPERIMENTAL) + pVec.riskTolerance * 20.0);
			weights.put(TacticalPlan.MOBILITY_WARFARE, weights.get(TacticalPlan.MOBILITY_WARFARE) + pVec.riskTolerance * 12.0);

			String roleStr = getRotPersistentString(rot, "sentinel_assigned_role", "TANK");
			if ("TANK".equalsIgnoreCase(roleStr)) {
				weights.put(TacticalPlan.AGGRESSIVE_PRESSURE, weights.get(TacticalPlan.AGGRESSIVE_PRESSURE) + 12.0);
				weights.put(TacticalPlan.GROUND_CONTROL, weights.get(TacticalPlan.GROUND_CONTROL) + 10.0);
			} else if ("FLANKER".equalsIgnoreCase(roleStr)) {
				weights.put(TacticalPlan.HIT_AND_RUN, weights.get(TacticalPlan.HIT_AND_RUN) + 12.0);
				weights.put(TacticalPlan.MOBILITY_WARFARE, weights.get(TacticalPlan.MOBILITY_WARFARE) + 10.0);
			} else if ("CASTER".equalsIgnoreCase(roleStr)) {
				weights.put(TacticalPlan.PROJECTILE_SUPPRESSION, weights.get(TacticalPlan.PROJECTILE_SUPPRESSION) + 12.0);
				weights.put(TacticalPlan.MAINTAIN_DISTANCE, weights.get(TacticalPlan.MAINTAIN_DISTANCE) + 10.0);
			}

			if (known.contains(EnemyTrait.LIFESTEAL)) {
				weights.put(TacticalPlan.BURST_DAMAGE, weights.get(TacticalPlan.BURST_DAMAGE) + 15.0);
				weights.put(TacticalPlan.HIT_AND_RUN, weights.get(TacticalPlan.HIT_AND_RUN) + 10.0);
			}
			if (known.contains(EnemyTrait.PROJECTILE_REFLECTION)) {
				weights.put(TacticalPlan.INTERRUPT_SPECIALIST, weights.get(TacticalPlan.INTERRUPT_SPECIALIST) + 12.0);
				weights.put(TacticalPlan.GROUND_CONTROL, weights.get(TacticalPlan.GROUND_CONTROL) + 10.0);
			}
			if (known.contains(EnemyTrait.GRAVITY_MANIPULATION) || known.contains(EnemyTrait.LEVITATION) || known.contains(EnemyTrait.PULL_EFFECTS) || known.contains(EnemyTrait.PUSH_EFFECTS)) {
				weights.put(TacticalPlan.GROUND_CONTROL, weights.get(TacticalPlan.GROUND_CONTROL) + 20.0);
				weights.put(TacticalPlan.MOBILITY_WARFARE, weights.get(TacticalPlan.MOBILITY_WARFARE) + 15.0);
			}
			if (known.contains(EnemyTrait.FLIGHT) || known.contains(EnemyTrait.HOVERING)) {
				weights.put(TacticalPlan.MOBILITY_WARFARE, weights.get(TacticalPlan.MOBILITY_WARFARE) + 15.0);
				weights.put(TacticalPlan.INTERRUPT_SPECIALIST, weights.get(TacticalPlan.INTERRUPT_SPECIALIST) + 10.0);
			}
			if (known.contains(EnemyTrait.BEAM_SPECIALIST) || known.contains(EnemyTrait.PROJECTILE_SPAM)) {
				weights.put(TacticalPlan.PROJECTILE_SUPPRESSION, weights.get(TacticalPlan.PROJECTILE_SUPPRESSION) + 20.0);
				weights.put(TacticalPlan.HIT_AND_RUN, weights.get(TacticalPlan.HIT_AND_RUN) + 10.0);
			}
			if (known.contains(EnemyTrait.SHIELDING) || known.contains(EnemyTrait.COUNTER_ATTACKER)) {
				weights.put(TacticalPlan.COUNTER_FOCUS, weights.get(TacticalPlan.COUNTER_FOCUS) + 15.0);
			}

			float rotHealthPct = rotMaxHealth > 0 ? rotHealth / rotMaxHealth : 1.0f;
			if (rotHealthPct < 0.30) {
				weights.put(TacticalPlan.DEFENSIVE_RECOVERY, weights.get(TacticalPlan.DEFENSIVE_RECOVERY) + 30.0);
				weights.put(TacticalPlan.SURVIVAL, weights.get(TacticalPlan.SURVIVAL) + 25.0);
				weights.put(TacticalPlan.MAINTAIN_DISTANCE, weights.get(TacticalPlan.MAINTAIN_DISTANCE) + 20.0);
				weights.put(TacticalPlan.PHASE_DISPLACEMENT, weights.getOrDefault(TacticalPlan.PHASE_DISPLACEMENT, 0.0) + 30.0);
			}

			double phaseCd = getD(rot, "rot_phase_cooldown");
			if (phaseCd <= 0.0) {
				weights.put(TacticalPlan.PHASE_DISPLACEMENT, weights.getOrDefault(TacticalPlan.PHASE_DISPLACEMENT, 0.0) + 18.0);
			}
			if (ctx != null && (ctx.isJumpCritIncoming || ctx.incomingProjectileDistance > 0.0)) {
				weights.put(TacticalPlan.PHASE_DISPLACEMENT, weights.getOrDefault(TacticalPlan.PHASE_DISPLACEMENT, 0.0) + 30.0);
			}
			if (ctx != null && ctx.nearbyTargetCount >= 2) {
				weights.put(TacticalPlan.PHASE_DISPLACEMENT, weights.getOrDefault(TacticalPlan.PHASE_DISPLACEMENT, 0.0) + 35.0);
			}

			if (momentum > 25.0) {
				weights.put(TacticalPlan.AGGRESSIVE_PRESSURE, weights.get(TacticalPlan.AGGRESSIVE_PRESSURE) + 15.0);
				weights.put(TacticalPlan.BURST_DAMAGE, weights.get(TacticalPlan.BURST_DAMAGE) + 10.0);
			} else if (momentum < -25.0) {
				weights.put(TacticalPlan.DEFENSIVE_RECOVERY, weights.get(TacticalPlan.DEFENSIVE_RECOVERY) + 15.0);
				weights.put(TacticalPlan.HIT_AND_RUN, weights.get(TacticalPlan.HIT_AND_RUN) + 15.0);
			}

			if (environmentState.equals("CONFINED")) {
				weights.put(TacticalPlan.COUNTER_FOCUS, weights.get(TacticalPlan.COUNTER_FOCUS) + 10.0);
				weights.put(TacticalPlan.BURST_DAMAGE, weights.get(TacticalPlan.BURST_DAMAGE) + 10.0);
			} else if (environmentState.equals("HAZARDOUS_LAVA") || environmentState.equals("WATER_BOUND")) {
				weights.put(TacticalPlan.GROUND_CONTROL, weights.get(TacticalPlan.GROUND_CONTROL) + 15.0);
				weights.put(TacticalPlan.MOBILITY_WARFARE, weights.get(TacticalPlan.MOBILITY_WARFARE) + 10.0);
			}

			if (known.size() >= 3) {
				weights.put(TacticalPlan.HYBRID, weights.get(TacticalPlan.HYBRID) + 25.0);
			}

			if (!ENABLE_PHASE_SHIFT) {
				weights.put(TacticalPlan.PHASE_DISPLACEMENT, 0.0);
			}

			double totalWeight = 0.0;
			TacticalPlan bestPlan = TacticalPlan.AGGRESSIVE_PRESSURE;
			double maxWeight = -1.0;

			for (Map.Entry<TacticalPlan, Double> entry : weights.entrySet()) {
				totalWeight += entry.getValue();
				if (entry.getValue() > maxWeight) {
					maxWeight = entry.getValue();
					bestPlan = entry.getKey();
				}
			}

			for (TacticalPlan p : TacticalPlan.values()) {
				planConfidence.put(p, totalWeight > 0 ? weights.get(p) / totalWeight : 0.10);
			}

			double chosenConfidence = planConfidence.getOrDefault(bestPlan, 0.50);

			if (chosenConfidence < 0.25 || (currentTick % 100 == 0 && Math.random() < 0.20)) {
				isExperimentingTactics = true;
				List<TacticalPlan> validPlans = new ArrayList<>();
				for (TacticalPlan p : TacticalPlan.values()) {
					if (p == TacticalPlan.PHASE_DISPLACEMENT && !ENABLE_PHASE_SHIFT) continue;
					validPlans.add(p);
				}
				experimentPlan = validPlans.get((int) (Math.random() * validPlans.size()));
				currentPlan = experimentPlan;
				planReason = "EXPERIMENT_MODE: Testing " + experimentPlan.name();
			} else {
				currentPlan = bestPlan;
				planReason = "MULTI_TRAIT_SYNERGY (" + known.size() + " traits, Momentum " + String.format("%.1f", momentum) + ")";
			}

			planUsageCount.put(currentPlan, planUsageCount.getOrDefault(currentPlan, 0) + 1);

			if (ENABLE_PHASE_SHIFT && phaseCd <= 0.0 && !getB(rot, "rot_phase_shifting")) {
				if (currentPlan == TacticalPlan.PHASE_DISPLACEMENT || rotHealthPct < 0.35 || (ctx != null && (ctx.isJumpCritIncoming || ctx.incomingProjectileDistance > 0.0 || ctx.nearbyTargetCount >= 2))) {
					double phaseDur = (rotHealthPct < 0.35 || (ctx != null && ctx.nearbyTargetCount >= 2)) ? 600.0 + Math.random() * 600.0 : 300.0 + Math.random() * 300.0;
					putD(rot, "rot_phase_ticks", phaseDur);
					putB(rot, "rot_phase_shifting", true);
					if (world instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.SMOKE, rot.getX(), rot.getY() + 1.0, rot.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
						playHostileSound(serverLevel, rot, "entity.evoker.cast_spell", 1.2F, 0.6F);
					}
				}
			}

			if (currentTick % 200 == 0) {
				for (TacticalPlan p : TacticalPlan.values()) {
					double rate = planSuccessRate.getOrDefault(p, 0.50);
					planSuccessRate.put(p, rate * 0.95 + 0.025);
				}
			}
		}

		public static String analyzeEnvironment(LevelAccessor world, Entity rot, LivingEntity target) {
			if (rot == null) return "OPEN_SPACE";
			BlockPos pos = BlockPos.containing(rot.getX(), rot.getY(), rot.getZ());
			int solidSides = 0;
			if (world.getBlockState(pos.east()).isSolid()) solidSides++;
			if (world.getBlockState(pos.west()).isSolid()) solidSides++;
			if (world.getBlockState(pos.north()).isSolid()) solidSides++;
			if (world.getBlockState(pos.south()).isSolid()) solidSides++;

			if (solidSides >= 2) return "CONFINED";
			if (world.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.LAVA)) return "HAZARDOUS_LAVA";
			if (world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER)) return "WATER_BOUND";
			if (target != null && target.getY() - rot.getY() > 3.0) return "ELEVATED";

			return "OPEN_SPACE";
		}

		private double getEnvOrdinal(String state) {
			if ("CONFINED".equalsIgnoreCase(state)) return 1.0;
			if ("HAZARDOUS_LAVA".equalsIgnoreCase(state)) return 2.0;
			if ("WATER_BOUND".equalsIgnoreCase(state)) return 3.0;
			if ("ELEVATED".equalsIgnoreCase(state)) return 4.0;
			return 0.0;
		}

		public double getTacticalProfileCompletion() {
			double base = getMechanicProfileCompletion();
			double planUsageProgress = Math.min(1.0, (double) planUsageCount.size() / 5.0) * 20.0;
			return Math.min(100.0, base * 0.8 + planUsageProgress);
		}

		public double getPosteriorMean(double alpha, double beta) {
			double total = alpha + beta;
			if (total <= 0.0) return 0.5;
			return alpha / total;
		}

		public double getPosteriorVariance(double alpha, double beta) {
			double total = alpha + beta;
			if (total <= 0.0) return 0.25;
			return (alpha * beta) / (total * total * (total + 1.0));
		}

		public CombatProfile() {
			signalAlpha.put("sudden_stop", 2.0); signalBeta.put("sudden_stop", 2.0);
			signalAlpha.put("accel_toward", 2.0); signalBeta.put("accel_toward", 2.0);
			signalAlpha.put("facing_lock", 2.0); signalBeta.put("facing_lock", 2.0);
			signalAlpha.put("windup_pattern", 2.0); signalBeta.put("windup_pattern", 2.0);
			signalAlpha.put("interval_due", 2.0); signalBeta.put("interval_due", 2.0);
			signalAlpha.put("projectile_incoming", 2.0); signalBeta.put("projectile_incoming", 2.0);
			signalAlpha.put("adapter_trigger", 2.0); signalBeta.put("adapter_trigger", 2.0);
			signalAlpha.put("rapid_fire", 2.0); signalBeta.put("rapid_fire", 2.0);
			signalAlpha.put("ballistic_tracking", 2.0); signalBeta.put("ballistic_tracking", 2.0);
			signalAlpha.put("reload_window", 2.0); signalBeta.put("reload_window", 2.0);

			defenseAlpha.put("TELEPORT", 4.0); defenseBeta.put("TELEPORT", 1.33);
			defenseAlpha.put("BLOCK", 3.25); defenseBeta.put("BLOCK", 1.75);
			defenseAlpha.put("COUNTER", 3.0); defenseBeta.put("COUNTER", 2.0);
			defenseAlpha.put("STRAFE", 2.5); defenseBeta.put("STRAFE", 2.5);

			signalWeights.put("sudden_stop", 1.0);
			signalWeights.put("accel_toward", 1.0);
			signalWeights.put("facing_lock", 1.0);
			signalWeights.put("windup_pattern", 1.0);
			signalWeights.put("interval_due", 1.0);
			signalWeights.put("projectile_incoming", 1.0);
			signalWeights.put("adapter_trigger", 1.0);
			signalWeights.put("rapid_fire", 1.0);
			signalWeights.put("ballistic_tracking", 1.0);
			signalWeights.put("reload_window", 1.0);

			defenseSuccessRates.put("TELEPORT", 0.75);
			defenseSuccessRates.put("BLOCK", 0.65);
			defenseSuccessRates.put("COUNTER", 0.60);
			defenseSuccessRates.put("STRAFE", 0.50);
		}

		public double getSignalWeight(String signal) {
			double a = signalAlpha.getOrDefault(signal, 2.0);
			double b = signalBeta.getOrDefault(signal, 2.0);
			double mean = getPosteriorMean(a, b);
			double var = getPosteriorVariance(a, b);
			double effective = mean * 2.0;
			if (var > BETA_VARIANCE_GATE_THRESHOLD) {
				double factor = Math.min(1.0, (var - BETA_VARIANCE_GATE_THRESHOLD) / 0.15);
				effective = (1.0 - factor) * effective + factor * 1.0;
			}
			return Math.max(0.10, Math.min(3.0, effective));
		}

		public void rewardSignal(String signal) {
			double a = signalAlpha.getOrDefault(signal, 2.0) + 1.0;
			signalAlpha.put(signal, a);
			signalTruePositives.put(signal, signalTruePositives.getOrDefault(signal, 0) + 1);
			signalWeights.put(signal, getSignalWeight(signal));
		}

		public void penalizeSignal(String signal) {
			double b = signalBeta.getOrDefault(signal, 2.0) + 1.0;
			signalBeta.put(signal, b);
			signalFalsePositives.put(signal, signalFalsePositives.getOrDefault(signal, 0) + 1);
			signalWeights.put(signal, getSignalWeight(signal));
		}

		public void recordDefenseResult(String action, boolean success) {
			if (action == null || action.isEmpty()) return;
			int attempts = defenseAttempts.getOrDefault(action, 0) + 1;
			defenseAttempts.put(action, attempts);
			if (success) {
				defenseAlpha.put(action, defenseAlpha.getOrDefault(action, 2.0) + 1.0);
				defenseSuccesses.put(action, defenseSuccesses.getOrDefault(action, 0) + 1);
			} else {
				defenseBeta.put(action, defenseBeta.getOrDefault(action, 2.0) + 1.0);
			}
			defenseSuccessRates.put(action, getDefenseSuccessRate(action));
		}

		public double getDefenseSuccessRate(String action) {
			double a = defenseAlpha.getOrDefault(action, 2.0);
			double b = defenseBeta.getOrDefault(action, 2.0);
			double mean = getPosteriorMean(a, b);
			double var = getPosteriorVariance(a, b);
			if (var > BETA_VARIANCE_GATE_THRESHOLD) {
				double factor = Math.min(1.0, (var - BETA_VARIANCE_GATE_THRESHOLD) / 0.15);
				mean = (1.0 - factor) * mean + factor * 0.50;
			}
			return Math.max(0.05, Math.min(0.95, mean));
		}

		public String getTopRecommendedDefense() {
			String[] actions = new String[]{"TELEPORT", "BLOCK", "COUNTER", "STRAFE"};
			String best = "TELEPORT";
			double bestRate = -1.0;
			for (String a : actions) {
				double rate = getDefenseSuccessRate(a);
				if (rate > bestRate) {
					bestRate = rate;
					best = a;
				}
			}
			return best;
		}

		public void classifyFightStyle(LivingEntity target) {
			int total = meleeUsageCount + projectileUsageCount + beamUsageCount + aoeUsageCount + dashCount + teleportCount;
			if (total < 3) {
				fightStyle = FightStyle.HYBRID;
				return;
			}
			double beamRatio = (double) beamUsageCount / total;
			double projRatio = (double) (projectileUsageCount + beamUsageCount) / total;
			double aoeRatio = (double) aoeUsageCount / total;
			double mobilityRatio = (double) (dashCount + teleportCount) / total;
			double blockRatio = (double) attacksBlocked / Math.max(1, total);

			if (target != null && target.getMaxHealth() >= 300.0) {
				fightStyle = FightStyle.TANK;
				return;
			}
			if (beamRatio >= 0.30) {
				fightStyle = FightStyle.BEAM_FOCUSED;
			} else if (projRatio >= 0.45) {
				fightStyle = FightStyle.PROJECTILE_FOCUSED;
			} else if (aoeRatio >= 0.35) {
				fightStyle = FightStyle.AOE_FOCUSED;
			} else if (mobilityRatio >= 0.40) {
				fightStyle = FightStyle.HIT_AND_RUN;
			} else if (averageMeleeInterval <= 25.0 && meleeUsageCount >= total * 0.5) {
				fightStyle = FightStyle.AGGRESSIVE;
			} else if (averageMeleeInterval >= 50.0 && blockRatio >= 0.25) {
				fightStyle = FightStyle.DEFENSIVE;
			} else if (averageComboLength >= 2.0) {
				fightStyle = FightStyle.COMBO_FOCUSED;
			} else if (getDefenseSuccessRate("COUNTER") >= 0.70) {
				fightStyle = FightStyle.COUNTER_ATTACKER;
			} else {
				fightStyle = FightStyle.HYBRID;
			}
		}

		public void checkPhaseShift(LivingEntity target, long currentTick) {
			if (target == null) return;
			double currentHealthRatio = (double) target.getHealth() / (double) target.getMaxHealth();
			boolean phaseChanged = false;

			if ((lastHealthRatio >= 0.75 && currentHealthRatio < 0.75) ||
				(lastHealthRatio >= 0.50 && currentHealthRatio < 0.50) ||
				(lastHealthRatio >= 0.25 && currentHealthRatio < 0.25)) {
				phaseChanged = true;
			}
			if (phaseAggressionBaseline > 0 && averageMeleeInterval < phaseAggressionBaseline * 0.65) {
				phaseChanged = true;
			}

			if (phaseChanged) {
				currentPhase++;
				lastHealthRatio = currentHealthRatio;
				phaseAggressionBaseline = averageMeleeInterval;
				recentAttackHistory.clear();
			}
		}

		public void recordAttack(long currentTick, double distance, Vec3 velocity, double windupTicks, String attackType) {
			if (lastAttackTick > 0 && currentTick > lastAttackTick) {
				double interval = (double) (currentTick - lastAttackTick);
				if (interval < minInterval) minInterval = interval;
				if (interval > maxInterval) maxInterval = interval;

				if ("PROJECTILE".equalsIgnoreCase(attackType) || "BEAM".equalsIgnoreCase(attackType)) {
					averageProjectileInterval = 0.7 * averageProjectileInterval + 0.3 * interval;
				} else {
					averageMeleeInterval = 0.7 * averageMeleeInterval + 0.3 * interval;
				}
				recentInterval = 0.7 * recentInterval + 0.3 * interval;
				longTermInterval = 0.9 * longTermInterval + 0.1 * interval;

				double diff = Math.abs(interval - averageMeleeInterval);
				intervalVariance = 0.8 * intervalVariance + 0.2 * diff;
			}

			if (lastAttackType != null && !lastAttackType.isEmpty()) {
				String transition = lastAttackType + "->" + attackType;
				sequenceTransitions.put(transition, sequenceTransitions.getOrDefault(transition, 0) + 1);

				double prevConf = patternConfidence.getOrDefault(transition, 0.40);
				double newConf = Math.min(0.99, prevConf + 0.12);
				patternConfidence.put(transition, newConf);

				for (String key : new ArrayList<>(patternConfidence.keySet())) {
					if (key.startsWith(lastAttackType + "->") && !key.equals(transition)) {
						patternConfidence.put(key, Math.max(0.05, patternConfidence.get(key) - 0.08));
					}
				}
			}
			lastAttackType = attackType;

			recentAttackHistory.add(attackType);
			if (recentAttackHistory.size() > 10) {
				recentAttackHistory.remove(0);
			}

			lastAttackTick = currentTick;
			totalObservedAttacks++;

			if (windupTicks > 0) {
				averageChargeDuration = 0.7 * averageChargeDuration + 0.3 * windupTicks;
			}
			if (distance > 0) {
				preferredAttackRange = 0.8 * preferredAttackRange + 0.2 * distance;
				preferredEngagementDistance = preferredAttackRange;
				maxAttackRange = Math.max(maxAttackRange, distance);
			}

			if ("PROJECTILE".equalsIgnoreCase(attackType)) projectileUsageCount++;
			else if ("BEAM".equalsIgnoreCase(attackType)) beamUsageCount++;
			else if ("VERTICAL".equalsIgnoreCase(attackType)) verticalAttackCount++;
			else if ("AOE".equalsIgnoreCase(attackType)) aoeUsageCount++;
			else if ("DASH".equalsIgnoreCase(attackType)) dashCount++;
			else if ("TELEPORT".equalsIgnoreCase(attackType)) teleportCount++;
			else meleeUsageCount++;

			confidence = Math.min(1.0, totalObservedAttacks / 8.0);
		}

		public String predictFollowUp(String currentAttack) {
			if (currentAttack == null || currentAttack.isEmpty()) return "UNKNOWN";
			String bestNext = "UNKNOWN";
			double maxConf = -1.0;
			for (Map.Entry<String, Double> entry : patternConfidence.entrySet()) {
				if (entry.getKey().startsWith(currentAttack + "->")) {
					if (entry.getValue() > maxConf) {
						maxConf = entry.getValue();
						bestNext = entry.getKey().substring((currentAttack + "->").length());
					}
				}
			}
			if ("UNKNOWN".equals(bestNext)) {
				int maxCount = 0;
				for (Map.Entry<String, Integer> entry : sequenceTransitions.entrySet()) {
					if (entry.getKey().startsWith(currentAttack + "->")) {
						if (entry.getValue() > maxCount) {
							maxCount = entry.getValue();
							bestNext = entry.getKey().substring((currentAttack + "->").length());
						}
					}
				}
			}
			return bestNext;
		}

		public double getPatternConfidence(String currentAttack, String predictedNext) {
			if (currentAttack == null || predictedNext == null) return 0.0;
			return patternConfidence.getOrDefault(currentAttack + "->" + predictedNext, confidence * 0.75);
		}

		public double getRiskScore(String predictedAttack, ThreatLevel level, Entity rot, LivingEntity target) {
			double baseRisk = level.getLevel() * 20.0;
			if ("BEAM".equalsIgnoreCase(predictedAttack) || "AOE".equalsIgnoreCase(predictedAttack)) {
				baseRisk += 25.0;
			}
			if (target != null && target.getMaxHealth() >= 200.0) {
				baseRisk += 15.0;
			}
			if (rot instanceof LivingEntity rotLiv) {
				double healthRatio = (double) rotLiv.getHealth() / (double) rotLiv.getMaxHealth();
				if (healthRatio < 0.40) baseRisk *= 1.4;
			}
			return Math.min(100.0, baseRisk);
		}

		public double getCompletionPercentage() {
			double factor = 0.0;
			if (totalObservedAttacks >= 8) factor += 40.0;
			else factor += (totalObservedAttacks / 8.0) * 40.0;

			if (!sequenceTransitions.isEmpty()) factor += 20.0;
			if (!defenseAttempts.isEmpty()) factor += 20.0;
			if (!signalTruePositives.isEmpty()) factor += 20.0;

			return Math.min(100.0, factor);
		}

		public void saveToNbt(CompoundTag mem) {
			if (mem == null) return;
			mem.putLongArray("RotNNWeights", neuralNet.toLongArray());
			mem.putLongArray("RotHabits", opponentHabits.serialize());

			CompoundTag traitTag = new CompoundTag();
			for (Map.Entry<EnemyTrait, Double> entry : traitConfidence.entrySet()) {
				traitTag.putDouble(entry.getKey().name(), entry.getValue());
			}
			mem.put("TraitConfidence", traitTag);

			CompoundTag defAlphaTag = new CompoundTag();
			for (Map.Entry<String, Double> entry : defenseAlpha.entrySet()) defAlphaTag.putDouble(entry.getKey(), entry.getValue());
			mem.put("DefenseAlpha", defAlphaTag);

			CompoundTag defBetaTag = new CompoundTag();
			for (Map.Entry<String, Double> entry : defenseBeta.entrySet()) defBetaTag.putDouble(entry.getKey(), entry.getValue());
			mem.put("DefenseBeta", defBetaTag);

			CompoundTag defRatesTag = new CompoundTag();
			for (Map.Entry<String, Double> entry : defenseSuccessRates.entrySet()) defRatesTag.putDouble(entry.getKey(), entry.getValue());
			mem.put("DefenseSuccessRates", defRatesTag);

			CompoundTag pTag = new CompoundTag();
			personality.saveToNbt(pTag);
			mem.put("PersonalityVector", pTag);

			CompoundTag planTag = new CompoundTag();
			for (Map.Entry<TacticalPlan, Double> entry : planSuccessRate.entrySet()) planTag.putDouble(entry.getKey().name(), entry.getValue());
			mem.put("PlanSuccessRate", planTag);

			mem.putString("SuccessfulCounters", String.join(",", successfulCounters));
			mem.putString("FailedCounters", String.join(",", failedCounters));
		}

		public void loadFromNbt(CompoundTag mem) {
			if (mem == null) return;
			if (mem.contains("RotNNWeights")) {
				long[] savedW = mem.getLongArray("RotNNWeights");
				if (savedW != null && savedW.length == TacticalNeuralNetwork.TOTAL_WEIGHTS) {
					neuralNet = TacticalNeuralNetwork.fromLongArray(savedW);
				}
			}

			if (mem.contains("RotHabits")) {
				opponentHabits.deserialize(mem.getLongArray("RotHabits"));
			}

			if (mem.contains("TraitConfidence")) {
				CompoundTag tag = mem.getCompound("TraitConfidence");
				for (String k : tag.getAllKeys()) {
					try {
						EnemyTrait trait = EnemyTrait.valueOf(k);
						traitConfidence.put(trait, tag.getDouble(k));
					} catch (Exception ignored) {}
				}
			}

			if (mem.contains("DefenseAlpha")) {
				CompoundTag tag = mem.getCompound("DefenseAlpha");
				for (String k : tag.getAllKeys()) defenseAlpha.put(k, tag.getDouble(k));
			}

			if (mem.contains("DefenseBeta")) {
				CompoundTag tag = mem.getCompound("DefenseBeta");
				for (String k : tag.getAllKeys()) defenseBeta.put(k, tag.getDouble(k));
			}

			if (mem.contains("DefenseSuccessRates")) {
				CompoundTag tag = mem.getCompound("DefenseSuccessRates");
				for (String k : tag.getAllKeys()) defenseSuccessRates.put(k, tag.getDouble(k));
			}

			if (mem.contains("PersonalityVector")) {
				personality = PersonalityVector.loadFromNbt(mem.getCompound("PersonalityVector"));
			}

			if (mem.contains("PlanSuccessRate")) {
				CompoundTag tag = mem.getCompound("PlanSuccessRate");
				for (String k : tag.getAllKeys()) {
					try {
						TacticalPlan plan = TacticalPlan.valueOf(k);
						planSuccessRate.put(plan, tag.getDouble(k));
					} catch (Exception ignored) {}
				}
			}

			if (mem.contains("SuccessfulCounters")) {
				String str = mem.getString("SuccessfulCounters");
				successfulCounters.clear();
				if (!str.isEmpty()) {
					for (String s : str.split(",")) if (!s.trim().isEmpty()) successfulCounters.add(s.trim());
				}
			}

			if (mem.contains("FailedCounters")) {
				String str = mem.getString("FailedCounters");
				failedCounters.clear();
				if (!str.isEmpty()) {
					for (String s : str.split(",")) if (!s.trim().isEmpty()) failedCounters.add(s.trim());
				}
			}
		}
	}

	public static class ActionHypothesis {
		public final String action;
		public final double probability;
		public final double estimatedLeadTicks;
		public final String recommendedCounter;

		public ActionHypothesis(String action, double probability, double estimatedLeadTicks, String recommendedCounter) {
			this.action = action;
			this.probability = probability;
			this.estimatedLeadTicks = estimatedLeadTicks;
			this.recommendedCounter = recommendedCounter;
		}
	}

	public static class PendingPrediction {
		public UUID targetUuid;
		public long predictionTick;
		public long expireTick;
		public String predictedAttackType = "MELEE";
		public ThreatLevel threatLevel = ThreatLevel.NONE;
		public double threatScore = 0.0;
		public List<String> activeSignals = new ArrayList<>();
		public String rotDefensiveAction = "";
		public boolean evaluated = false;

		// Multi-hypothesis prediction
		public List<ActionHypothesis> competingHypotheses = new ArrayList<>();
		public ActionHypothesis topHypothesis = null;
		public double topHypothesisConfidence = 0.0;
		public double entropyUncertainty = 0.0;

		public boolean isHighConfidence() {
			return topHypothesisConfidence >= 0.70 && entropyUncertainty < 0.40;
		}

		public boolean isLowConfidence() {
			return topHypothesisConfidence < 0.45 || entropyUncertainty >= 0.60;
		}
	}

	public static class EntityObservation {
		public Vec3 lastPos = Vec3.ZERO;
		public Vec3 lastDeltaMovement = Vec3.ZERO;
		public double lastSpeed = 0.0;
		public float lastYaw = 0.0f;
		public float lastPitch = 0.0f;
		public Vec3 lastLookVector = Vec3.ZERO;
		public int ticksFacingRot = 0;
		public int ticksStillAfterMoving = 0;
		public int lastItemUseTicks = 0;
		public long lastObservationTick = 0;

		// Habit & feint observation flags
		public boolean isSpacingFeint = false;
		public boolean isReachTesting = false;
		public boolean isCritBaiting = false;
		public boolean isShieldBaiting = false;
		public boolean isMovementBaiting = false;
		public boolean isWpingDetected = false;
		public boolean isStappingDetected = false;
		public int lateralStrafeFlips = 0;
		public boolean isCircleStrafing = false;
		public boolean isErraticStrafing = false;
		public double lastLateralSign = 0.0;
		public int sustainedLateralTicks = 0;
		public boolean isDeliberateLOSBreak = false;
		public boolean isVerticalPillaring = false;
		public int ticksGroundedPillarAscent = 0;
		public boolean isWaterTrapping = false;
		public boolean isRapidHotbarSwapping = false;
		public int itemSwapsInWindow = 0;
		public long lastItemSwapTick = 0;
		public String lastHeldMainItem = "";
		public String lastHeldOffItem = "";
		public boolean heldTotem = false;
		public boolean heldShield = false;
		public boolean isConsumingHeal = false;
		public boolean patternBrokenRecently = false;
		public boolean adaptationShiftDetected = false;
	}

	public static abstract class AttackPredictorAdapter {
		public abstract double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile);
	}

	public static class VanillaPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			if (target.isUsingItem()) {
				int useTicks = target.getTicksUsingItem();
				score += Math.min(35.0, 10.0 + useTicks * 2.0);
			}
			if (target.swinging) {
				score += 25.0;
			}
			if (target instanceof Player player) {
				if (player.getAttackStrengthScale(0.5f) > 0.85f && rot.distanceTo(target) <= 4.5) {
					score += 20.0;
				}
			}
			return score;
		}
	}

	public static class TACZPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				ItemStack main = target.getMainHandItem();
				ItemStack off = target.getOffhandItem();
				String mainKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				String offKey = BuiltInRegistries.ITEM.getKey(off.getItem()).toString();

				boolean hasGun = mainKey.contains("tacz") || offKey.contains("tacz") || mainKey.contains("modern_kinetic_gun");
				if (hasGun) {
					score += 20.0;
					Vec3 look = target.getLookAngle().normalize();
					Vec3 toRot = rot.position().subtract(target.position()).normalize();
					double dot = look.dot(toRot);
					if (dot > 0.94) {
						score += 35.0;
					} else if (dot > 0.85) {
						score += 15.0;
					}

					double sustainedHits = getD(rot, "sentinel_sustained_bullet_hits");
					if (sustainedHits > 0) {
						score += Math.min(45.0, sustainedHits * 12.0);
					}

					if (target.isUsingItem()) {
						score += 20.0;
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static class CataclysmPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
				if (typeKey.contains("cataclysm")) {
					score += 25.0;
					if (target.swinging) score += 30.0;
					if (target.fallDistance > 1.5) score += 25.0;
					if (target.distanceTo(rot) <= 6.0) score += 20.0;
				}

				ItemStack main = target.getMainHandItem();
				String mainKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				if (mainKey.contains("cataclysm")) {
					score += 20.0;
					if (mainKey.contains("infernal_forge") || mainKey.contains("incinerator") || mainKey.contains("tidal_claws")) {
						if (target.swinging || target.isUsingItem()) score += 35.0;
					} else if (mainKey.contains("laser_gatling") || mainKey.contains("wither_assault")) {
						if (target.isUsingItem()) score += 40.0;
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static boolean isWroughtnautStuck(LivingEntity target) {
		if (target == null) return false;
		String className = target.getClass().getName();
		if (className.contains("Wroughtnaut") || className.contains("EntityWroughtnaut")) {
			try {
				Object currentAnim = target.getClass().getMethod("getAnimation").invoke(target);
				if (currentAnim != null) {
					int currentTick = (Integer) target.getClass().getMethod("getAnimationTick").invoke(target);
					for (java.lang.reflect.Field field : target.getClass().getDeclaredFields()) {
						if (field.getType().getSimpleName().equals("Animation")) {
							field.setAccessible(true);
							Object animVal = field.get(null);
							if (animVal == currentAnim) {
								String fieldName = field.getName().toUpperCase();
								if (fieldName.contains("VERTICAL") || fieldName.contains("SLAM") || fieldName.contains("STUCK")) {
									if (currentTick >= 20) {
										return true;
									}
								}
							}
						}
					}
				}
			} catch (Exception ignored) {
				if (target.getDeltaMovement().horizontalDistanceSqr() < 0.001 && target.swingTime == 0 && target.fallDistance == 0.0) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isWroughtnautAttacking(LivingEntity target) {
		if (target == null) return false;
		String className = target.getClass().getName();
		if (className.contains("Wroughtnaut") || className.contains("EntityWroughtnaut")) {
			try {
				Object currentAnim = target.getClass().getMethod("getAnimation").invoke(target);
				if (currentAnim != null) {
					for (java.lang.reflect.Field field : target.getClass().getDeclaredFields()) {
						if (field.getType().getSimpleName().equals("Animation")) {
							field.setAccessible(true);
							Object animVal = field.get(null);
							if (animVal == currentAnim) {
								String fieldName = field.getName().toUpperCase();
								if (fieldName.contains("ATTACK") || fieldName.contains("SLAM") || fieldName.contains("STOMP") || fieldName.contains("SWING")) {
									return true;
								}
							}
						}
					}
				}
			} catch (Exception ignored) {}
			if (target.swingTime > 0) {
				return true;
			}
		}
		return false;
	}

	public static class MowziesPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
				boolean isWrought = typeKey.contains("wroughtnaut") || typeKey.contains("ferrous_wroughtnaut");

				if (typeKey.contains("mowziesmobs")) {
					score += 25.0;
					if (target.swinging) score += 30.0;
					if (target.fallDistance > 1.2) score += 35.0;
					if (target.distanceTo(rot) <= 5.0) score += 15.0;

					if (isWrought) {
						if (!isWroughtnautStuck(target)) {
							score += 55.0;
						} else {
							score -= 45.0;
						}
					}
				}

				ItemStack main = target.getMainHandItem();
				String itemKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				if (itemKey.contains("mowziesmobs")) {
					score += 15.0;
					if (itemKey.contains("wrought_axe")) {
						if (target.fallDistance > 1.0 || target.getDeltaMovement().y < -0.2) {
							score += 45.0;
						} else if (target.swinging) {
							score += 25.0;
						}
					} else if (itemKey.contains("ice_crystal") || itemKey.contains("blowgun") || itemKey.contains("sol_visage")) {
						if (target.isUsingItem()) {
							score += 35.0;
						}
					} else if (itemKey.contains("spear") && target.swinging) {
						score += 25.0;
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static class AlexsCavesPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
				if (typeKey.contains("alexscaves")) {
					score += 25.0;
					if (target.swinging || target.isUsingItem()) score += 30.0;
				}

				ItemStack main = target.getMainHandItem();
				String itemKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				if (itemKey.contains("alexscaves")) {
					score += 15.0;
					if (itemKey.contains("raygun") || itemKey.contains("tremor_zapper") || itemKey.contains("dreadbow") || itemKey.contains("darkness_incinerator")) {
						if (target.isUsingItem()) {
							score += 40.0;
						}
					} else if (itemKey.contains("extinction_spear") || itemKey.contains("galena_gauntlet")) {
						if (target.swinging || target.isUsingItem()) {
							score += 30.0;
						}
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static class EpicFightPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				ItemStack main = target.getMainHandItem();
				String itemKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				if (itemKey.contains("epicfight") || target.swinging) {
					score += 20.0;
					if (target.swinging && rot.distanceTo(target) <= 4.5) {
						score += 30.0;
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static class IronSpellsPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				ItemStack main = target.getMainHandItem();
				ItemStack off = target.getOffhandItem();
				String mainKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				String offKey = BuiltInRegistries.ITEM.getKey(off.getItem()).toString();

				boolean isHoldingSpellItem = mainKey.contains("irons_spellbooks") || offKey.contains("irons_spellbooks")
					|| mainKey.contains("spell_book") || mainKey.contains("scroll") || offKey.contains("spell_book");

				if (isHoldingSpellItem) {
					score += 20.0;
					if (target.isUsingItem()) {
						int castTicks = target.getTicksUsingItem();
						score += Math.min(45.0, 20.0 + castTicks * 2.5);
					}
				}

				for (net.minecraft.world.effect.MobEffectInstance effect : target.getActiveEffects()) {
					String effectKey = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString();
					if (effectKey.contains("irons_spellbooks")) {
						score += 15.0;
						break;
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static class UniversalCombatPredictionEngine {
		private static final Map<UUID, EntityObservation> OBSERVATIONS = new HashMap<>();
		private static final Map<String, CombatProfile> PROFILES = new HashMap<>();
		private static final Map<UUID, PendingPrediction> PENDING_PREDICTIONS = new HashMap<>();
		private static final List<AttackPredictorAdapter> ADAPTERS = List.of(
			new VanillaPredictorAdapter(),
			new TACZPredictorAdapter(),
			new CataclysmPredictorAdapter(),
			new MowziesPredictorAdapter(),
			new AlexsCavesPredictorAdapter(),
			new EpicFightPredictorAdapter(),
			new IronSpellsPredictorAdapter()
		);

		public static void tickPrediction(LevelAccessor world, Entity rot, LivingEntity target) {
			if (rot == null || target == null || !target.isAlive()) return;

			long currentTick = world instanceof Level lvl ? lvl.getGameTime() : rot.tickCount;
			if (currentTick % 6000 == 0) {
				pruneStaleProfiles(currentTick);
				RoleAuction.pruneStaleBids(currentTick);
			}

			UUID targetUuid = target.getUUID();
			EntityObservation obs = OBSERVATIONS.computeIfAbsent(targetUuid, k -> new EntityObservation());

			String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
			String profileKey = (target instanceof Player player) ? ("player:" + player.getUUID()) : typeId;
			CombatProfile profile = PROFILES.computeIfAbsent(profileKey, k -> {
				CombatProfile p = new CombatProfile();
				p.entityTypeId = typeId;
				return p;
			});

			RoleAuction.Role wonRole = RoleAuction.calculateRotRole(rot, target, currentTick);
			putS(rot, "sentinel_assigned_role", wonRole.name());

			if (target instanceof Player player) {
				boolean isAttacking = target.swinging || target.isUsingItem();
				PlayerBehaviorTracker tracker = PlayerBehaviorTracker.get(player.getUUID());
				boolean isSurprise = tracker.observe(currentTick, rot.distanceTo(target), isAttacking);
				tracker.observeMovementDetails(currentTick, player, rot, obs, profile.opponentHabits);
				if (isSurprise || tracker.patternBreakingDetected) {
					putD(rot, "sentinel_surprise_alert_ticks", 60.0);
					obs.patternBrokenRecently = true;
				}
			}

			if (target instanceof Player player && !profile.hasLoadedHivemindWeights) {
				profile.hasLoadedHivemindWeights = true;
				RotHivemindSavedData hivemind = RotHivemindSavedData.get(world);
				if (hivemind != null) {
					CompoundTag mem = hivemind.getMemory(player.getUUID());
					profile.loadFromNbt(mem);
				}
			}

			profile.classifyFightStyle(target);
			profile.checkPhaseShift(target, currentTick);

			PendingPrediction pending = PENDING_PREDICTIONS.get(targetUuid);
			if (pending != null && !pending.evaluated && currentTick > pending.expireTick) {
				if (pending.threatLevel.isHighOrImminent() || pending.threatScore >= 45.0) {
					for (String signal : pending.activeSignals) {
						profile.penalizeSignal(signal);
					}
					if (!pending.rotDefensiveAction.isEmpty()) {
						profile.recordDefenseResult(pending.rotDefensiveAction, false);
					}
				}
				pending.evaluated = true;
			}

			double score = 0.0;
			List<String> activeSignals = new ArrayList<>();
			double dist = rot.distanceTo(target);
			Vec3 pos = target.position();
			Vec3 rotPos = rot.position();
			Vec3 dirToRot = rotPos.subtract(pos).normalize();

			Vec3 currentMovement = target.getDeltaMovement();
			double currentSpeed = currentMovement.horizontalDistance();

			if (target instanceof LivingEntity targetLiv) {
				if (obs.lastSpeed > 0.18 && currentSpeed < 0.08 && dist >= 3.2 && dist <= 5.0) {
					obs.isSpacingFeint = true;
				} else if (dist < 3.0 || dist > 6.0) {
					obs.isSpacingFeint = false;
				}
				if (targetLiv.isBlocking()) {
					obs.isShieldBaiting = (obs.lastItemUseTicks > 0 && obs.lastItemUseTicks <= 5);
				}
				if (!targetLiv.onGround() && targetLiv.getDeltaMovement().y > 0.0 && dist > 4.5) {
					obs.isCritBaiting = true;
				} else if (targetLiv.onGround()) {
					obs.isCritBaiting = false;
				}
			}
			boolean suddenStop = (obs.lastSpeed > 0.12 && currentSpeed < 0.035);
			if (suddenStop) {
				score += 10.0 * profile.getSignalWeight("sudden_stop");
				activeSignals.add("sudden_stop");
			}

			Vec3 acceleration = currentMovement.subtract(obs.lastDeltaMovement);
			double accelTowardRot = acceleration.dot(dirToRot);
			if (accelTowardRot > 0.08) {
				score += 20.0 * profile.getSignalWeight("accel_toward");
				activeSignals.add("accel_toward");
			}

			Vec3 lookVec = target.getLookAngle().normalize();
			double dot = lookVec.dot(dirToRot);
			double angleDeg = Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0, 1.0)));

			if (angleDeg < 15.0) {
				score += 8.0 * profile.getSignalWeight("facing_lock");
				activeSignals.add("facing_lock");
				obs.ticksFacingRot++;
				if (obs.ticksFacingRot >= 3) {
					score += 12.0 * profile.getSignalWeight("facing_lock");
				}
			} else {
				obs.ticksFacingRot = 0;
			}

			boolean isWindupPattern = suddenStop && angleDeg < 20.0 && dist <= profile.preferredAttackRange + 2.0;
			if (isWindupPattern) {
				score += 30.0 * profile.getSignalWeight("windup_pattern");
				activeSignals.add("windup_pattern");
			}

			if (dist <= 3.2 && (obs.lastSpeed > 0.15 || target.isSprinting())) {
				score += 25.0 * profile.getSignalWeight("accel_toward");
				if (!activeSignals.contains("accel_toward")) activeSignals.add("accel_toward");
			}

			if (profile.confidence > 0.3 && profile.lastAttackTick > 0) {
				double ticksSince = (double) (currentTick - profile.lastAttackTick);
				if (Math.abs(ticksSince - profile.averageMeleeInterval) <= 6.0 || Math.abs(ticksSince - profile.averageProjectileInterval) <= 6.0) {
					score += 50.0 * profile.getSignalWeight("interval_due");
					activeSignals.add("interval_due");
				}
			}

			boolean projectileIncoming = checkIncomingProjectiles(world, rot, target);
			if (projectileIncoming) {
				score += 45.0 * profile.getSignalWeight("projectile_incoming");
				activeSignals.add("projectile_incoming");
			}

			if (world instanceof Level lvl) {
				List<LivingEntity> nearbyHostiles = lvl.getEntitiesOfClass(LivingEntity.class, rot.getBoundingBox().inflate(16.0), e -> e != rot && e != target && e.isAlive() && (e instanceof Player || (e instanceof Mob m && m.getTarget() == rot)));
				if (!nearbyHostiles.isEmpty()) {
					score += Math.min(25.0, nearbyHostiles.size() * 10.0);
					activeSignals.add("multiple_threats");
				}
			}

			for (AttackPredictorAdapter adapter : ADAPTERS) {
				double adapterScore = adapter.evaluateThreat(world, rot, target, obs, profile);
				if (adapterScore > 0) {
					score += adapterScore * profile.getSignalWeight("adapter_trigger");
					activeSignals.add("adapter_trigger");
				}
			}

			double prevScore = getD(rot, "sentinel_predicted_threat_score");
			double finalScore = 0.7 * prevScore + 0.3 * score;

			ThreatLevel level = ThreatLevel.NONE;
			if (finalScore >= 90.0) level = ThreatLevel.ATTACK_IMMINENT;
			else if (finalScore >= 60.0) level = ThreatLevel.HIGH;
			else if (finalScore >= 35.0) level = ThreatLevel.MEDIUM;
			else if (finalScore >= 15.0) level = ThreatLevel.LOW;

			String followup = profile.predictFollowUp(profile.lastAttackType);
			double patternConf = profile.getPatternConfidence(profile.lastAttackType, followup);

			PendingPrediction newPending = new PendingPrediction();
			newPending.targetUuid = targetUuid;
			newPending.predictionTick = currentTick;
			newPending.expireTick = currentTick + 25;
			newPending.threatLevel = level;
			newPending.threatScore = finalScore;
			newPending.activeSignals = activeSignals;
			newPending.predictedAttackType = followup;

			// Multi-hypothesis prediction generation:
			List<ActionHypothesis> hypotheses = new ArrayList<>();

			double meleeProb = 0.15;
			if (dist <= 4.0) meleeProb += 0.30;
			if (!target.onGround() && target.getDeltaMovement().y < -0.05) meleeProb += 0.35;
			if (target.swinging) meleeProb += 0.40;
			if (obs.isWpingDetected || obs.isStappingDetected) meleeProb += 0.25;
			if (obs.isCritBaiting) meleeProb *= 0.4;
			hypotheses.add(new ActionHypothesis("CRIT_ATTACK", Math.min(0.95, meleeProb), 15.0, "UPPERCUT"));

			double shieldProb = 0.10;
			if (target.isBlocking()) shieldProb += 0.60;
			else if (obs.heldShield) {
				shieldProb += 0.25;
				if (dist <= 4.5 && rot.getDeltaMovement().horizontalDistance() > 0.1) shieldProb += 0.20;
			}
			if (obs.isShieldBaiting) shieldProb *= 0.4;
			hypotheses.add(new ActionHypothesis("SHIELD_BLOCK", Math.min(0.95, shieldProb), 10.0, "OVERHEAD_SLAM"));

			double dodgeProb = 0.15;
			if (obs.lateralStrafeFlips >= 2 || obs.isCircleStrafing || obs.isErraticStrafing) dodgeProb += 0.40;
			if (obs.isSpacingFeint) dodgeProb += 0.25;
			hypotheses.add(new ActionHypothesis("DODGE_STRAFE", Math.min(0.95, dodgeProb), 8.0, "SONIC_BOOM"));

			double healProb = 0.05;
			boolean lowTargetHp = target.getHealth() < target.getMaxHealth() * 0.45f;
			if (lowTargetHp) healProb += 0.35;
			if (obs.isConsumingHeal) healProb += 0.55;
			else if (target.getDeltaMovement().lengthSqr() > 0.05 && dist > 5.0 && lowTargetHp) healProb += 0.30;
			hypotheses.add(new ActionHypothesis("RETREAT_HEAL", Math.min(0.95, healProb), 12.0, "TELEPORT_INTERCEPT"));

			double rangedProb = 0.05;
			if (dist > 6.0 && (target.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem || target.getMainHandItem().getItem() instanceof net.minecraft.world.item.CrossbowItem)) rangedProb += 0.60;
			hypotheses.add(new ActionHypothesis("RANGED_ATTACK", Math.min(0.95, rangedProb), 10.0, "PHASE_DISPLACEMENT"));

			double baitProb = 0.05;
			if (obs.isSpacingFeint || obs.isCritBaiting || obs.isShieldBaiting || obs.isMovementBaiting || obs.isReachTesting) baitProb += 0.50;
			if (profile.opponentHabits.conditioningBaitProbability > 0.3) baitProb += profile.opponentHabits.conditioningBaitProbability * 0.4;
			if (obs.patternBrokenRecently) baitProb += 0.30;
			hypotheses.add(new ActionHypothesis("BAIT_FEINT", Math.min(0.95, baitProb), 18.0, "MAINTAIN_SPACING"));

			double terrainProb = 0.05;
			if (obs.isDeliberateLOSBreak || obs.isVerticalPillaring || obs.isWaterTrapping) terrainProb += 0.55;
			hypotheses.add(new ActionHypothesis("TERRAIN_RESET", Math.min(0.95, terrainProb), 14.0, "SKY_WARP_SLAM"));

			hypotheses.sort((h1, h2) -> Double.compare(h2.probability, h1.probability));
			newPending.competingHypotheses = hypotheses;

			ActionHypothesis topH = hypotheses.get(0);
			newPending.topHypothesis = topH;
			newPending.topHypothesisConfidence = topH.probability;

			// Shannon entropy calculation for prediction uncertainty
			double totalHWeight = 0.0;
			for (ActionHypothesis h : hypotheses) totalHWeight += h.probability;
			double entropy = 0.0;
			if (totalHWeight > 0.001) {
				for (ActionHypothesis h : hypotheses) {
					double p = h.probability / totalHWeight;
					if (p > 0.0001) entropy -= p * (Math.log(p) / Math.log(2.0));
				}
				newPending.entropyUncertainty = entropy / (Math.log(hypotheses.size()) / Math.log(2.0));
			}

			String nextSeqStep = profile.opponentHabits.predictNextSequenceStep();
			if (!nextSeqStep.isEmpty()) {
				putS(rot, "sentinel_predicted_sequence_step", nextSeqStep);
			}

			boolean targetTeleported = dist > 14.0 && obs.lastPos != null && obs.lastPos.distanceTo(pos) > 10.0;
			if (targetTeleported || obs.patternBrokenRecently || obs.adaptationShiftDetected) {
				profile.abandonTacticalPlan(rot, obs.adaptationShiftDetected ? "PLAYER_BEHAVIORAL_ADAPTATION" : "TARGET_SUDDEN_SHIFT");
				if (obs.adaptationShiftDetected) {
					putB(rot, "sentinel_adaptation_detected", true);
					obs.adaptationShiftDetected = false;
				}
			}

			PENDING_PREDICTIONS.put(targetUuid, newPending);

			profile.observeMechanics(world, rot, target, obs, currentTick);

			profile.updateTacticalPlan(world, rot, target, obs, currentTick, finalScore);

			putS(rot, "sentinel_predicted_threat_level", level.name());
			putD(rot, "sentinel_predicted_threat_score", finalScore);
			putB(rot, "sentinel_predicted_attack_imminent", level == ThreatLevel.ATTACK_IMMINENT);
			putB(rot, "sentinel_windup_detected", isWindupPattern);
			putD(rot, "sentinel_prediction_confidence", profile.confidence);
			putS(rot, "sentinel_predicted_followup", followup);
			putD(rot, "sentinel_pattern_confidence", patternConf);
			putS(rot, "sentinel_fight_style", profile.fightStyle.name());
			putD(rot, "sentinel_combat_phase", profile.currentPhase);
			putS(rot, "sentinel_top_recommended_defense", profile.getTopRecommendedDefense());
			putD(rot, "sentinel_profile_completion", profile.getCompletionPercentage());

			putS(rot, "sentinel_strategy", profile.currentStrategy);
			putS(rot, "sentinel_known_traits", profile.getKnownTraitsString());
			putS(rot, "sentinel_trait_confidence", profile.getTraitConfidenceString());
			putS(rot, "sentinel_successful_counters", String.join(",", profile.successfulCounters));
			putS(rot, "sentinel_failed_counters", String.join(",", profile.failedCounters));
			putS(rot, "sentinel_experimental_counter", profile.experimentalCounter);
			putD(rot, "sentinel_mechanic_profile_completion", profile.getMechanicProfileCompletion());

			putS(rot, "sentinel_tactical_plan", profile.currentPlan.name());
			putD(rot, "sentinel_plan_confidence", profile.planConfidence.getOrDefault(profile.currentPlan, 0.50));
			putD(rot, "sentinel_plan_success_rate", profile.planSuccessRate.getOrDefault(profile.currentPlan, 0.50));
			putD(rot, "sentinel_momentum", profile.momentum);
			putS(rot, "sentinel_environment_state", profile.environmentState);
			putS(rot, "sentinel_plan_reason", profile.planReason);
			putD(rot, "sentinel_tactical_completion", profile.getTacticalProfileCompletion());

			if (getB(rot, "sentinel_debug_mode") && currentTick % 40 == 0) {
				if (!world.isClientSide() && rot instanceof LivingEntity) {
					System.out.printf("[Sentinel Engine P5] Target: %s | Plan: %s (%.0f%%, Succ: %.0f%%) | Reason: %s | Momentum: %.1f | Env: %s | Exp: %b | Progress: %.0f%%%n",
						typeId, profile.currentPlan.name(), profile.planConfidence.getOrDefault(profile.currentPlan, 0.50) * 100.0,
						profile.planSuccessRate.getOrDefault(profile.currentPlan, 0.50) * 100.0,
						profile.planReason, profile.momentum, profile.environmentState, profile.isExperimentingTactics,
						profile.getTacticalProfileCompletion());
				}
			}

			CombatContext ctx = getCombatContext(rot, target);
			boolean jumpCritDanger = ctx != null && ctx.isJumpCritIncoming;
			boolean projDanger = ctx != null && ctx.incomingProjectileDistance > 0.0 && ctx.incomingProjectileDistance <= 12.0;
			double maxRiskHealth = rot instanceof LivingEntity rotLiving ? rotLiving.getMaxHealth() : 20.0;
			boolean isUrgentDanger = isWindupPattern || projectileIncoming || jumpCritDanger || projDanger || ctx.expectedIncomingDamage >= maxRiskHealth * 0.50;

			if (level.isHighOrImminent() || isUrgentDanger) {
				double riskScore = profile.getRiskScore(followup, level, rot, target);
				if (profile.confidence >= 0.25 || level == ThreatLevel.ATTACK_IMMINENT || riskScore >= 50.0 || isUrgentDanger) {
					triggerReactiveDefenses(world, rot, target, dist, level, isUrgentDanger, profile, newPending);
				}
			}

			obs.lastPos = pos;
			obs.lastDeltaMovement = currentMovement;
			obs.lastSpeed = currentSpeed;
			obs.lastYaw = target.getYRot();
			obs.lastPitch = target.getXRot();
			obs.lastLookVector = lookVec;
			obs.lastObservationTick = currentTick;

			if (target instanceof Player player && currentTick % 20 == 0) {
				RotHivemindSavedData hivemind = RotHivemindSavedData.get(world);
				if (hivemind != null) {
					CompoundTag mem = hivemind.getMemory(player.getUUID());
					profile.saveToNbt(mem);
					hivemind.updateMemory(player.getUUID(), mem);
				}
			}
		}

		private static boolean checkIncomingProjectiles(LevelAccessor world, Entity rot, LivingEntity target) {
			if (!(world instanceof Level level)) return false;
			AABB searchBox = rot.getBoundingBox().inflate(24.0);
			List<net.minecraft.world.entity.projectile.Projectile> projectiles = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, searchBox, p -> p.getOwner() == target || (p.getOwner() != rot && p.getDeltaMovement().lengthSqr() > 0.1));

			Vec3 rotPos = rot.position().add(0, rot.getBbHeight() * 0.5, 0);
			for (net.minecraft.world.entity.projectile.Projectile p : projectiles) {
				Vec3 pPos = p.position();
				Vec3 pVel = p.getDeltaMovement();
				if (pVel.lengthSqr() < 0.01) continue;

				Vec3 pToRot = rotPos.subtract(pPos);
				if (pVel.dot(pToRot) <= 0) continue;

				double t = pToRot.dot(pVel) / pVel.lengthSqr();
				if (t > 0 && t <= 15.0) {
					Vec3 closestPoint = pPos.add(pVel.scale(t));
					if (closestPoint.distanceTo(rotPos) < 2.5) {
						return true;
					}
				}
			}
			return false;
		}

		private static void triggerReactiveDefenses(LevelAccessor world, Entity rot, LivingEntity target, double dist, ThreatLevel level, boolean immediateDanger, CombatProfile profile, PendingPrediction pending) {
			String recommendedDefense = profile.getTopRecommendedDefense();

			EntityObservation obs = (target != null) ? OBSERVATIONS.get(target.getUUID()) : null;
			boolean isBaitDetected = (obs != null && (obs.isShieldBaiting || obs.isCritBaiting || obs.isSpacingFeint || obs.isMovementBaiting));
			boolean highUncertainty = (pending != null && (pending.isLowConfidence() || pending.entropyUncertainty >= 0.55));

			double phaseCd = getD(rot, "rot_phase_cooldown");
			if (ENABLE_PHASE_SHIFT && phaseCd <= 0.0 && !getB(rot, "rot_phase_shifting")) {
				CombatContext reactiveCtx = getCombatContext(rot, target);
				float rotHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getHealth() : 20.0f;
				float rotMaxHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getMaxHealth() : 20.0f;
				float rotHpPct = rotMaxHealth > 0 ? rotHealth / rotMaxHealth : 1.0f;
				if (immediateDanger || rotHpPct < 0.35 || (reactiveCtx != null && (reactiveCtx.isJumpCritIncoming || reactiveCtx.incomingProjectileDistance > 0.0 || reactiveCtx.nearbyTargetCount >= 2))) {
					double phaseDur = (rotHpPct < 0.35 || (reactiveCtx != null && reactiveCtx.nearbyTargetCount >= 2)) ? 600.0 + Math.random() * 600.0 : 300.0 + Math.random() * 300.0;
					putD(rot, "rot_phase_ticks", phaseDur);
					putB(rot, "rot_phase_shifting", true);
					if (world instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.SMOKE, rot.getX(), rot.getY() + 1.0, rot.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
						playHostileSound(serverLevel, rot, "entity.evoker.cast_spell", 1.2F, 0.6F);
					}
				}
			}

			// Don't waste high-commitment block or teleport dodge if target is baiting or uncertain
			if (ENABLE_BLOCKING && !isRotChannelingAbility(rot) && dist <= 6.0 && !getB(rot, "is_blocking") && getD(rot, "rot_block_cooldown") <= 0) {
				if (!isBaitDetected && ("BLOCK".equals(recommendedDefense) || immediateDanger || (level == ThreatLevel.ATTACK_IMMINENT && !highUncertainty))) {
					if (rot.getRandom().nextDouble() < 0.85) {
						putD(rot, "rot_block_active_ticks", BLOCK_MIN_TICKS + rot.getRandom().nextInt((int)(BLOCK_MAX_TICKS - BLOCK_MIN_TICKS + 1)));
						putB(rot, "is_blocking", true);
						pending.rotDefensiveAction = "BLOCK";
						return;
					}
				}
			}

			if (immediateDanger && dist <= 8.0 && getD(rot, K_TP_DODGE_CD) <= 0 && getB(rot, "unlocked_teleportation")) {
				if (!isBaitDetected && ("TELEPORT".equals(recommendedDefense) || rot.getRandom().nextDouble() < 0.85)) {
					tryPredictiveDodge(world, rot, target, dist);
					pending.rotDefensiveAction = "TELEPORT";
					return;
				}
			}

			if (level == ThreatLevel.ATTACK_IMMINENT && dist <= 2.8 && getD(rot, "sentinel_melee_cooldown") <= 0 && getD(rot, "sentinel_melee_windup") <= 0) {
				if ("COUNTER".equals(recommendedDefense) || rot.getRandom().nextDouble() < 0.50) {
					putD(rot, "sentinel_melee_windup", 4);
					pending.rotDefensiveAction = "COUNTER";
				}
			}
		}

		public static void recordActualAttack(Entity rot, Entity target, String attackType) {
			if (rot == null || target == null || rot.level() == null) return;
			try {
			if (target instanceof LivingEntity targetLiv) {
				UUID targetUuid = targetLiv.getUUID();
				String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetLiv.getType()).toString();
				String profileKey = (targetLiv instanceof Player player) ? ("player:" + player.getUUID()) : typeId;
				CombatProfile profile = PROFILES.get(profileKey);
				if (profile != null) {
					long currentTick = rot.level().getGameTime();
					profile.lastTargetDamageTick = currentTick;
					putD(rot, "sentinel_last_target_damage_tick", currentTick);
					double outcome = getD(rot, "sentinel_attack_outcome_score");
					putD(rot, "sentinel_attack_outcome_score", Math.min(1.0, outcome + 0.25));
					profile.recordAttack(currentTick, rot.distanceTo(target), target.getDeltaMovement(), 0, attackType);

					PendingPrediction pending = PENDING_PREDICTIONS.get(targetUuid);
					if (pending != null && !pending.evaluated) {
						if (pending.threatLevel.isHighOrImminent() || pending.threatScore >= 40.0) {
							for (String signal : pending.activeSignals) {
								profile.rewardSignal(signal);
							}
							if (!pending.rotDefensiveAction.isEmpty()) {
								profile.recordDefenseResult(pending.rotDefensiveAction, true);
								putD(rot, "sentinel_defense_success_rate", Math.min(1.0, getRotPersistentDouble(rot, "sentinel_defense_success_rate", 0.5) + 0.1));
							}
						}
						pending.evaluated = true;
					}
				}
			}
			} catch (Exception ignored) {}
		}

		public static CombatProfile getProfile(String key) {
			return PROFILES.get(key);
		}

		public static EntityObservation getObservation(UUID uuid) {
			return OBSERVATIONS.get(uuid);
		}

		public static PendingPrediction getPending(UUID uuid) {
			return PENDING_PREDICTIONS.get(uuid);
		}

		public static void recordExterminationMilestone(Entity rot, Entity target, String milestoneType, double score) {
			if (rot == null || target == null) return;
			try {
				if (target instanceof LivingEntity targetLiv) {
					String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetLiv.getType()).toString();
					String profKey = (targetLiv instanceof Player player) ? ("player:" + player.getUUID()) : typeId;
					CombatProfile profile = PROFILES.get(profKey);
					if (profile != null) {
						profile.recordExterminationProgress(milestoneType, score);
					}
				}
			} catch (Exception ignored) {}
		}

		public static void recordRotDamage(Entity rot, Entity attacker) {
			if (rot == null || attacker == null || rot.level() == null) return;
			try {
			if (attacker instanceof LivingEntity targetLiv) {
				UUID targetUuid = targetLiv.getUUID();
				String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetLiv.getType()).toString();
				String profileKey = (targetLiv instanceof Player player) ? ("player:" + player.getUUID()) : typeId;
				CombatProfile profile = PROFILES.get(profileKey);
				if (profile != null) {
					profile.lastRotDamageTick = rot.level().getGameTime();
					profile.momentum = Math.max(-100.0, profile.momentum - 15.0);
					if (getRotPersistentDouble(rot, "current_action_commitment", 0.0) > 0.5) {
						profile.recordExterminationProgress("WHIFFED_COMMITMENT", -15.0);
					}
					if (profile.currentPlan != null) {
						double[] inputs = profile.constructNeuralInputs(rot, targetLiv);
						profile.neuralNet.trainDelta(inputs, profile.currentPlan.ordinal(), -5.0);
					}
					PendingPrediction pending = PENDING_PREDICTIONS.get(targetUuid);
					if (pending != null) {
						if (!pending.evaluated && pending.threatScore < 40.0) {
							for (String signal : pending.activeSignals) {
								profile.rewardSignal(signal);
							}
						}
						if (!pending.rotDefensiveAction.isEmpty()) {
							profile.recordDefenseResult(pending.rotDefensiveAction, false);
							putD(rot, "sentinel_defense_success_rate", Math.max(0.0, getRotPersistentDouble(rot, "sentinel_defense_success_rate", 0.5) - 0.1));
						}
					}
				}
			}
			} catch (Exception ignored) {}
		}

		public static void recordRotKill(Entity rot, Entity victim) {
			if (rot == null || victim == null) return;
			try {
			if (victim instanceof LivingEntity targetLiv) {
				UUID targetUuid = targetLiv.getUUID();
				String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetLiv.getType()).toString();
				String profileKey = (targetLiv instanceof Player player) ? ("player:" + player.getUUID()) : typeId;
				CombatProfile profile = PROFILES.get(profileKey);
				if (profile != null) {
					profile.momentum = Math.min(100.0, profile.momentum + 25.0);
					profile.recordExterminationProgress("EXTERMINATION_SUCCESS", 100.0);
					profile.activeChainStep = TacticalChainStep.NONE;
					if (profile.currentPlan != null) {
						double[] inputs = profile.constructNeuralInputs(rot, targetLiv);
						profile.neuralNet.trainDelta(inputs, profile.currentPlan.ordinal(), 10.0);
					}
				}
				PENDING_PREDICTIONS.remove(targetUuid);
			}
			} catch (Exception ignored) {}
		}

		public static void recordRotLandingImpact(Entity rot, double fallDistance) {
			if (rot == null || !(rot.level() instanceof net.minecraft.world.level.Level level)) return;
			if (fallDistance >= 4.0) {
				Vec3 pos = rot.position();
				AABB shockBox = rot.getBoundingBox().inflate(6.0);
				List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, shockBox, e -> e != rot && e.isAlive() && (e instanceof Player || e instanceof Mob));
				for (LivingEntity enemy : enemies) {
					Vec3 push = enemy.position().subtract(pos).normalize().scale(0.4).add(0, 0.25, 0);
					setMotion(enemy, enemy.getDeltaMovement().add(push));
					applyEffectSafe(enemy, new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
				}
				if (level instanceof ServerLevel serverLevel && !enemies.isEmpty()) {
					serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.2, pos.z, 12, 1.5, 0.2, 1.5, 0.1);
				}
			}
		}

		public static void onPlayerLoggedOut(UUID playerUuid) {
			OBSERVATIONS.remove(playerUuid);
			PENDING_PREDICTIONS.remove(playerUuid);
			PlayerBehaviorTracker.remove(playerUuid);
		}

		public static void pruneStaleProfiles(long currentTick) {
			PROFILES.entrySet().removeIf(entry -> {
				String key = entry.getKey();
				if (key != null && key.startsWith("player:")) {
					CombatProfile profile = entry.getValue();
					return profile != null && profile.lastAttackTick > 0 && (currentTick - profile.lastAttackTick > 72000);
				}
				return false;
			});
		}

		public static void clearPrediction(Entity rot) {
			putS(rot, "sentinel_predicted_threat_level", "NONE");
			putD(rot, "sentinel_predicted_threat_score", 0.0);
			putB(rot, "sentinel_predicted_attack_imminent", false);
			putB(rot, "sentinel_windup_detected", false);
		}
	}

	private static boolean isRotChannelingAbility(Entity entity) {
		if (entity == null || entity.getPersistentData() == null) return false;
		return getD(entity, "sentinel_solar_charge_ticks") > 0
			|| getD(entity, "sentinel_solar_fire_ticks") > 0
			|| getD(entity, "sentinel_cryo_charge_ticks") > 0
			|| getD(entity, "sentinel_cryo_fire_ticks") > 0
			|| getD(entity, "sentinel_laser_closing_ticks") > 0
			|| getD(entity, "sentinel_wither_skull_fire_ticks") > 0
			|| getD(entity, "sentinel_sonic_ticks") > 0
			|| getD(entity, "sentinel_omni_sonic_charge_ticks") > 0
			|| getD(entity, "sentinel_sonic_scream_ticks") > 0
			|| getD(entity, K_GRAPPLE_TICKS) > 0
			|| getD(entity, K_TK_TICKS) > 0
			|| getB(entity, "is_armor_ripping")
			|| getD(entity, "rot_armor_rip_ticks") > 0
			|| getD(entity, "sentinel_totem_inspect_ticks") > 0
			|| getB(entity, "is_blocking")
			|| getD(entity, "rot_block_active_ticks") > 0
			|| getD(entity, "sentinel_judgment_ticks") > 0
			|| getD(entity, "sentinel_sky_warp_slam_ticks") > 0
			|| getD(entity, "sentinel_minos_ticks") > 0
			|| getD(entity, "sentinel_slam_phase") > 0
			|| getD(entity, "sentinel_die_kick_phase") > 0
			|| getD(entity, "rot_overhead_ticks") > 0
			|| getD(entity, "sentinel_uppercut_anim_ticks") > 0
			|| getB(entity, "is_uppercutting")
			|| getD(entity, "sentinel_left_punch_ticks") > 0
			|| getD(entity, "sentinel_right_punch_ticks") > 0
			|| getD(entity, "sentinel_heavy_left_punch_ticks") > 0
			|| getD(entity, "sentinel_heavy_right_punch_ticks") > 0
			|| isDoingCombo(entity);
	}

	private static void cleanupCombatFlags(Entity entity) {
		if (entity == null || entity.getPersistentData() == null) return;
		// STRICT RULE: Never cut off active animations or channeled abilities!
		if (isRotChannelingAbility(entity)) {
			return;
		}
		stopHostileSound(entity.level(), entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
		clearBooleans(entity,
			"is_armor_ripping", "is_blocking", "is_blocking_finish", "rot_overhead_started",
			"is_uppercutting", "is_uppercutting_left", "is_uppercutting_right",
			"is_uppercut_standalone", "sentinel_uppercut_dodged"
		);
		clearDoubles(entity,
			"rot_armor_rip_ticks", "rot_choke_ticks", "rot_block_active_ticks",
			K_GRAPPLE_TICKS, K_TK_TICKS,
			"sentinel_solar_charge_ticks", "sentinel_solar_fire_ticks",
			"sentinel_cryo_charge_ticks", "sentinel_cryo_fire_ticks",
			"sentinel_wither_skull_fire_ticks", "sentinel_sonic_ticks",
			"sentinel_omni_sonic_charge_ticks", "sentinel_sonic_scream_ticks",
			"sentinel_judgment_ticks", "sentinel_minos_ticks", "sentinel_minos_stage",
			"sentinel_sky_warp_slam_ticks", "sentinel_slam_phase", "sentinel_slam_ticks",
			"sentinel_die_kick_phase", "sentinel_die_kick_ticks", "rot_overhead_ticks",
			"sentinel_cc1_stage", "sentinel_cc2_stage", "sentinel_cc3_stage",
			"sentinel_cc4_stage", "sentinel_cc5_stage", "sentinel_combo_active_ticks",
			"sentinel_melee_windup", "sentinel_totem_inspect_ticks"
		);
		if (entity instanceof RotEntity rot) {
			setEntityData(rot, RotEntity.DATA_is_armor_ripping, false);
			setEntityData(rot, RotEntity.DATA_is_blocking, false);
			setEntityData(rot, RotEntity.DATA_is_blocking_finish, false);
			setEntityData(rot, RotEntity.DATA_is_sonic_boom, false);
			setEntityData(rot, RotEntity.DATA_is_uppercutting, false);
			setEntityData(rot, RotEntity.DATA_is_uppercut_charging_left, false);
			setEntityData(rot, RotEntity.DATA_is_uppercut_charging_right, false);
			setEntityData(rot, RotEntity.DATA_is_dropkick_charging, false);
		}
	}

	private static void executeSentinelWitherSkullFiring(LevelAccessor world, Entity entity, Entity target, int fireTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.25, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.25);

		if (target == null || !target.isAlive()) {
			putD(entity, "sentinel_wither_skull_fire_ticks", 0);
			putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			return;
		}

		lockLookAtTarget(entity, target);

		if (fireTicks == 9 && !getB(entity, "sentinel_wither_skull_has_fired")) {
			if (target instanceof LivingEntity livingTarget && isWitherSkullImmuneTarget(level, livingTarget)) {
				recordWitherSkullFailure(entity, livingTarget);
				putD(entity, "sentinel_wither_skull_fire_ticks", 0);
				putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
				return;
			}
			putB(entity, "sentinel_wither_skull_has_fired", true);
			Vec3 headPos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
			Vec3 targetPos = target.getEyePosition(1.0F);
			Vec3 dir = targetPos.subtract(headPos).normalize();

			double spreadX = (level.getRandom().nextDouble() - 0.5) * 0.04;
			double spreadY = (level.getRandom().nextDouble() - 0.5) * 0.04;
			double spreadZ = (level.getRandom().nextDouble() - 0.5) * 0.04;
			Vec3 finalDir = dir.add(spreadX, spreadY, spreadZ).normalize();

			net.minecraft.world.entity.projectile.WitherSkull skull = new net.minecraft.world.entity.projectile.WitherSkull(net.minecraft.world.entity.EntityType.WITHER_SKULL, level);
			skull.setOwner(living);
			skull.setPos(headPos.x, headPos.y, headPos.z);
			skull.setDeltaMovement(finalDir.scale(3.8));
			if (level.getRandom().nextFloat() < 0.25F) {
				skull.setDangerous(true);
			}
			level.addFreshEntity(skull);
			if (target instanceof LivingEntity livingTarget) {
				MobEffectInstance wither = livingTarget.getEffect(MobEffects.WITHER);
				putS(entity, "sentinel_wither_skull_outcome_target", livingTarget.getUUID().toString());
				putD(entity, "sentinel_wither_skull_outcome_baseline", wither != null ? wither.getDuration() : 0.0);
				putD(entity, "sentinel_wither_skull_outcome_ticks", 30.0);
			}

			level.sendParticles(ParticleTypes.SWEEP_ATTACK, headPos.x, headPos.y, headPos.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.CRIT, headPos.x + dir.x * 0.5, headPos.y + dir.y * 0.5, headPos.z + dir.z * 0.5, 8, 0.1, 0.1, 0.1, 0.1);
			level.sendParticles(ParticleTypes.SMOKE, headPos.x, headPos.y, headPos.z, 5, 0.1, 0.1, 0.1, 0.05);
			playHostileSound(level, headPos.x, headPos.y, headPos.z, "entity.wither.shoot", 1.2F, 1.3F);
		}

		if (fireTicks <= 1) {
			putD(entity, "sentinel_wither_skull_fire_ticks", 0);
			putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
		}
	}

	private static boolean shouldAvoidWitherSkulls(Entity rot, Entity target) {
		if (!(target instanceof LivingEntity livingTarget)) return false;
		String learnedTarget = getS(rot, "sentinel_wither_skull_immune_target");
		return livingTarget.getUUID().toString().equals(learnedTarget);
	}

	private static boolean isWitherSkullImmuneTarget(ServerLevel level, LivingEntity target) {
		return isWither(target) || target.isInvulnerableTo(level.damageSources().wither());
	}

	private static void recordWitherSkullFailure(Entity rot, LivingEntity target) {
		String targetId = target.getUUID().toString();
		String previousTarget = getS(rot, "sentinel_wither_skull_failure_target");
		double failures = previousTarget.equals(targetId) ? getD(rot, "sentinel_wither_skull_failures") : 0.0;
		failures++;
		putS(rot, "sentinel_wither_skull_failure_target", targetId);
		putD(rot, "sentinel_wither_skull_failures", failures);
		if (failures >= 2.0) putS(rot, "sentinel_wither_skull_immune_target", targetId);
	}

	private static void interceptEnderPearls(LevelAccessor world, Entity entity) {
		if (!(world instanceof ServerLevel level)) return;
		boolean unlockedSolar = getB(entity, "unlocked_solar_beam");
		boolean unlockedCryo = getB(entity, "unlocked_cryo_beam");
		if (!unlockedSolar && !unlockedCryo) return;

		if (getD(entity, "sentinel_solar_charge_ticks") > 0
			|| getD(entity, "sentinel_solar_fire_ticks") > 0
			|| getD(entity, "sentinel_cryo_charge_ticks") > 0
			|| getD(entity, "sentinel_cryo_fire_ticks") > 0) {
			return;
		}

		AABB box = entity.getBoundingBox().inflate(64.0);
		java.util.List<net.minecraft.world.entity.projectile.ThrownEnderpearl> pearls = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownEnderpearl.class, box);
		for (net.minecraft.world.entity.projectile.ThrownEnderpearl pearl : pearls) {
			if (pearl.isAlive() && pearl.tickCount >= 3) {
				double dist = entity.distanceTo(pearl);
				boolean approachingLand = pearl.getDeltaMovement().y() < 0 || pearl.tickCount >= 8 || dist < 30.0;
				if (approachingLand) {
					lockLookAtTarget(entity, pearl);
					putI(entity, "sentinel_laser_target_id", pearl.getId());
					if (unlockedSolar) {
						putD(entity, "sentinel_solar_fire_ticks", 15.0);
					} else {
						putD(entity, "sentinel_cryo_fire_ticks", 15.0);
					}
					break;
				}
			}
		}
	}

	private static Set<Integer> getActiveBiasIndices(Entity entity) {
		Set<Integer> set = new HashSet<>();
		String str = getS(entity, "ai_active_bias_indices");
		if (!str.isEmpty()) {
			for (String part : str.split(",")) {
				try {
					set.add(Integer.parseInt(part.trim()));
				} catch (Exception ignored) {}
			}
		}
		return set;
	}

	private static void setActiveBiasIndices(Entity entity, Set<Integer> set) {
		if (set == null || set.isEmpty()) {
			entity.getPersistentData().remove("ai_active_bias_indices");
		} else {
			StringBuilder sb = new StringBuilder();
			for (int idx : set) {
				if (sb.length() > 0) sb.append(",");
				sb.append(idx);
			}
			putS(entity, "ai_active_bias_indices", sb.toString());
		}
	}

	private static void recordBiasIndexUpdate(Entity entity, String lastMove) {
		if (lastMove != null && lastMove.startsWith("combo_")) {
			try {
				int idx = Integer.parseInt(lastMove.substring(6));
				Set<Integer> set = getActiveBiasIndices(entity);
				if (set.add(idx)) {
					setActiveBiasIndices(entity, set);
				}
			} catch (Exception ignored) {}
		}
	}

	public static String inferCurrentAttackType(Entity rot) {
		if (rot == null) return "MELEE";
		if (getD(rot, "sentinel_judgment_ticks") > 0) return "JUDGMENT";
		if (getD(rot, "rot_overhead_ticks") > 0) return "OVERHEAD";
		if (getD(rot, "sentinel_die_kick_ticks") > 0 || getD(rot, "sentinel_die_kick_phase") > 0) return "DIE_KICK";
		if (getD(rot, "sentinel_wither_skull_fire_ticks") > 0) return "WITHER_SKULL";
		if (getD(rot, "sentinel_sonic_ticks") > 0 || getD(rot, "sentinel_omni_sonic_charge_ticks") > 0 || getD(rot, "sentinel_sonic_scream_ticks") > 0) return "SONIC";
		if (getD(rot, "sentinel_slam_ticks") > 0 || getD(rot, "sentinel_sky_warp_slam_ticks") > 0) return "SLAM";
		if (getD(rot, "sentinel_solar_fire_ticks") > 0 || getD(rot, "sentinel_solar_charge_ticks") > 0) return "SOLAR_BEAM";
		if (getD(rot, "sentinel_cryo_fire_ticks") > 0 || getD(rot, "sentinel_cryo_charge_ticks") > 0) return "CRYO_BEAM";
		if (getD(rot, K_GRAPPLE_TICKS) > 0) return "GRAPPLE";
		if (getD(rot, K_TK_TICKS) > 0) return "TELEKINESIS";
		if (getD(rot, "sentinel_melee_windup") > 0) return "MELEE_COUNTER";
		return "MELEE";
	}

	public static boolean isTargetHighlyDangerous(LivingEntity target) {
		if (target == null) return false;
		String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase();
		if (typeKey.contains("wroughtnaut") || typeKey.contains("boss") || typeKey.contains("warden") || typeKey.contains("dragon") || typeKey.contains("wither")) {
			return true;
		}
		ItemStack mainHand = target.getMainHandItem();
		if (!mainHand.isEmpty()) {
			String itemName = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).getPath().toLowerCase();
			if (itemName.contains("wrought") || itemName.contains("giant") || itemName.contains("hammer") || itemName.contains("battleaxe") || itemName.contains("claymore") || itemName.contains("heavy")) {
				return true;
			}
		}
		double attackDmg = getSafeAttributeValue(target, Attributes.ATTACK_DAMAGE, 0.0);
		if (attackDmg >= 8.5) {
			return true;
		}
		return false;
	}

	private static boolean getRotPersistentBoolean(Entity entity, String key, boolean fallback) {
		if (entity == null) return fallback;
		CompoundTag tag = entity.getPersistentData();
		return tag.contains(key) ? tag.getBoolean(key) : fallback;
	}

	private static int getRotPersistentInt(Entity entity, String key, int fallback) {
		if (entity == null) return fallback;
		CompoundTag tag = entity.getPersistentData();
		return tag.contains(key) ? tag.getInt(key) : fallback;
	}

	private static double getRotPersistentDouble(Entity entity, String key, double fallback) {
		if (entity == null) return fallback;
		CompoundTag tag = entity.getPersistentData();
		return tag.contains(key) ? tag.getDouble(key) : fallback;
	}

	private static String getRotPersistentString(Entity entity, String key, String fallback) {
		if (entity == null) return fallback;
		CompoundTag tag = entity.getPersistentData();
		return tag.contains(key) ? tag.getString(key) : fallback;
	}

	private static void setRotPersistentBoolean(Entity entity, String key, boolean val) {
		if (entity == null) return;
		entity.getPersistentData().putBoolean(key, val);
	}

	private static void setRotPersistentInt(Entity entity, String key, int val) {
		if (entity == null) return;
		entity.getPersistentData().putInt(key, val);
	}

	private static void setRotPersistentDouble(Entity entity, String key, double val) {
		if (entity == null) return;
		entity.getPersistentData().putDouble(key, val);
	}

	private static void setRotPersistentString(Entity entity, String key, String val) {
		if (entity == null) return;
		entity.getPersistentData().putString(key, val);
	}

	private static double getSafeAttributeValue(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double fallback) {
		if (entity == null || entity.getAttributes() == null) return fallback;
		try {
			if (entity.getAttributes().hasAttribute(attribute)) {
				return entity.getAttributeValue(attribute);
			}
		} catch (Exception ignored) {}
		return fallback;
	}

	private static net.minecraft.world.entity.ai.attributes.AttributeInstance getSafeAttribute(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
		if (entity == null || entity.getAttributes() == null) return null;
		try {
			if (entity.getAttributes().hasAttribute(attribute)) {
				return entity.getAttribute(attribute);
			}
		} catch (Exception ignored) {}
		return null;
	}

	private static double getD(Entity e, String k) { return getRotPersistentDouble(e, k, 0.0); }
	private static boolean getB(Entity e, String k) { return getRotPersistentBoolean(e, k, false); }
	private static int getI(Entity e, String k) { return getRotPersistentInt(e, k, 0); }
	private static String getS(Entity e, String k) { return getRotPersistentString(e, k, ""); }


	private static double[] calculateSmartLaserKnockback(Entity entity, Entity target, boolean isSolar) {
		if (entity == null || target == null) {
			return isSolar ? new double[]{0.35, 0.08} : new double[]{0.20, 0.04};
		}
		double dist = entity.distanceTo(target);
		Vec3 toTarget = target.position().subtract(entity.position()).normalize();
		Vec3 tVel = target.getDeltaMovement();

		double baseHoriz;
		double baseVert;

		if (dist <= 5.0) {
			// Target in close melee threat range: Rot smartly chooses higher repulsion to create spacing
			baseHoriz = isSolar ? 0.52 : 0.38;
			baseVert = isSolar ? 0.12 : 0.08;
		} else if (dist <= 12.0) {
			// Optimal laser zoning range: steady, controlled pinning push
			baseHoriz = isSolar ? 0.35 : 0.22;
			baseVert = isSolar ? 0.07 : 0.04;
		} else {
			// Long range: gentle knockback to prevent pushing target out of laser beam
			baseHoriz = isSolar ? 0.20 : 0.14;
			baseVert = isSolar ? 0.04 : 0.02;
		}

		// Adapt to target momentum: if charging in, resist; if fleeing, do not push further away
		double approachSpeed = -(tVel.x * toTarget.x + tVel.z * toTarget.z);
		if (approachSpeed > 0.15) {
			baseHoriz += Math.min(0.08, approachSpeed * 0.2);
		} else if (approachSpeed < -0.15) {
			baseHoriz = Math.max(0.12, baseHoriz - 0.07);
		}

		if (target instanceof LivingEntity liv) {
			if (liv.isBlocking()) {
				baseHoriz = Math.min(0.55, baseHoriz + 0.08);
			}
			if (!liv.onGround()) {
				baseVert = Math.min(0.04, baseVert * 0.5);
			}
		}

		// Dynamic tactical variance
		double jitter = ((entity.tickCount % 5) - 2) * 0.015;
		baseHoriz += jitter;

		// Strict bound enforcement: impactful but never absurdly strong
		baseHoriz = Mth.clamp(baseHoriz, 0.12, 0.58);
		baseVert = Mth.clamp(baseVert, 0.02, 0.14);

		return new double[]{baseHoriz, baseVert};
	}

	private static boolean handleElevatedUnreachableTarget(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level) || !(self instanceof Mob mob) || target == null || !target.isAlive()) return false;

		double currentLeapCd = getD(self, "sentinel_elevated_leap_cd");
		if (currentLeapCd > 0.0) {
			putD(self, "sentinel_elevated_leap_cd", currentLeapCd - 1.0);
		}

		if (isRotChannelingAbility(self) || isChannelingAbility(self)) return false;

		double heightDiff = target.getY() - self.getY();
		if (heightDiff < 1.75) return false;

		boolean pathBlocked = false;
		net.minecraft.world.level.pathfinder.Path currentPath = mob.getNavigation().getPath();
		if (currentPath == null || !currentPath.canReach() || mob.getNavigation().isDone()) {
			pathBlocked = true;
		}
		if (isTargetPillaring(world, target, self)) {
			pathBlocked = true;
		}
		if (!pathBlocked) return false;

		double dist = self.distanceTo(target);
		double hDist = Math.max(0.2, Math.sqrt(self.distanceToSqr(target.getX(), self.getY(), target.getZ())));

		// 1. Check unlocked ranged/burst combat abilities first if off cooldown
		if (getB(self, "unlocked_sonic_scream") && getD(self, "sentinel_sonic_scream_cooldown") <= 0.0 && dist <= 18.0) {
			recordAttack(self, "sonic_scream");
			putD(self, "sentinel_sonic_scream_ticks", 240.0);
			putD(self, "sentinel_sonic_scream_cooldown", SONIC_SCREAM_COOLDOWN);
			putD(self, "sentinel_global_ability_cooldown", 40.0);
			playHostileSound(world, self, "entity.warden.roar", 1.8F, 0.6F);
			return true;
		}

		if (getB(self, "unlocked_sonic_boom") && getD(self, "sentinel_warden_sonic_cooldown") <= 0.0 && dist <= 24.0) {
			recordAttack(self, "sonic_boom");
			fireSuperchargedSonicBoom(world, self, target);
			putD(self, "sentinel_warden_sonic_cooldown", SONIC_BOOM_COOLDOWN);
			putD(self, "sentinel_global_ability_cooldown", 40.0);
			return true;
		}

		if ((getB(self, "unlocked_solar_beam") || getB(self, "unlocked_cryo_beam")) && getD(self, K_SOLAR_CD) <= 0.0 && dist <= 30.0) {
			if (getB(self, "unlocked_cryo_beam")) {
				putD(self, "sentinel_cryo_charge_ticks", 1);
			} else {
				putD(self, "sentinel_solar_charge_ticks", 1);
			}
			putD(self, K_SOLAR_CD, SOLAR_CD);
			putD(self, "sentinel_global_ability_cooldown", 40.0);
			return true;
		}

		if (getB(self, "unlocked_telekinesis") && getD(self, K_TK_CD) <= 0.0 && dist <= 16.0) {
			recordAttack(self, "telekinesis");
			putD(self, K_TK_TICKS, 25);
			putD(self, K_TK_CD, TK_CD);
			putD(self, "sentinel_global_ability_cooldown", 40.0);
			playHostileSound(world, self, "entity.warden.heartbeat", 0.8F, 0.50F);
			return true;
		}

		if (getB(self, "unlocked_teleportation") && getD(self, K_TP_FLANK_CD) <= 0.0) {
			tryFlankTeleport(world, self, target, dist);
			putD(self, K_TP_FLANK_CD, 100.0);
			putD(self, "sentinel_global_ability_cooldown", 40.0);
			return true;
		}

		// 3. Smash / Mine the pillar or supporting block underneath the elevated target
		if (hDist <= 3.5) {
			BlockPos targetFeet = target.blockPosition();
			LivingEntity foundTarget = target instanceof LivingEntity liv ? liv : null;
			for (int dy = -1; dy >= -Math.min(6, (int) heightDiff); dy--) {
				BlockPos checkPos = targetFeet.above(dy);
				BlockState st = level.getBlockState(checkPos);
				if (!st.isAir() && canMine(world, checkPos, foundTarget)) {
					float hard = st.getDestroySpeed(level, checkPos);
					if (hard >= 0.0F && hard <= 35.0F) {
						level.destroyBlock(checkPos, false);
						playHostileSound(level, checkPos, "entity.item.break", 0.9F, 0.85F);
						return true;
					}
				}
			}
		}

		// 4. If everything else is on cooldown, leave it (reposition instead of freezing directly underneath)
		if (currentLeapCd > 0.0 && self.tickCount % 20 == 0) {
			double backAngle = mob.getRandom().nextDouble() * Math.PI * 2.0;
			double backDist = 5.0 + mob.getRandom().nextDouble() * 4.0;
			double targetX = target.getX() + Math.cos(backAngle) * backDist;
			double targetZ = target.getZ() + Math.sin(backAngle) * backDist;
			mob.getNavigation().moveTo(targetX, self.getY(), targetZ, ROT_WALK_SPEED);
		}

		return false;
	}

	private static void handleThreeBlockHeightSituationalAwareness(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level) || !(self instanceof Mob mob) || target == null) return;

		BlockPos feetPos = self.blockPosition();
		boolean ceilingTooLow = false;
		for (int dy = 1; dy <= 3; dy++) {
			BlockPos checkPos = feetPos.above(dy);
			BlockState state = level.getBlockState(checkPos);
			if (state.isCollisionShapeFullBlock(level, checkPos) && !state.isAir()) {
				ceilingTooLow = true;
				break;
			}
		}

		BlockPos targetPos = target.blockPosition();
		boolean targetInTunnel = false;
		BlockState targetCeilingState = level.getBlockState(targetPos.above(2));
		if (targetCeilingState.isCollisionShapeFullBlock(level, targetPos.above(2)) && !targetCeilingState.isAir()) {
			targetInTunnel = true;
		}

		double distance = self.distanceTo(target);

		if (ceilingTooLow || targetInTunnel) {
			if (mob.tickCount % 20 == 0) {
				Vec3 dir = target.position().subtract(self.position()).normalize();
				float dynamicHardnessLimit = 30.0F * (float) Math.max(1.0, getAdaptationMultiplier(self));
				for (int step = 1; step <= 3; step++) {
					Vec3 checkPoint = self.position().add(dir.scale(step));
					BlockPos center = BlockPos.containing(checkPoint);
					for (int dy = 0; dy <= 2; dy++) {
						BlockPos carvePos = center.above(dy);
						BlockState st = level.getBlockState(carvePos);
						if (!st.isAir() && canMine(world, carvePos, target instanceof LivingEntity ? (LivingEntity) target : null)) {
							float hard = st.getDestroySpeed(level, carvePos);
							if (hard >= 0.0F && hard <= dynamicHardnessLimit) {
								level.destroyBlock(carvePos, false);
							}
						}
					}
				}
			}

			if (self.isInWall() || (ceilingTooLow && distance > 4.0 && mob.tickCount % 40 == 0)) {
				if (getB(self, "unlocked_teleportation")) {
					double tx = target.getX() + (level.random.nextDouble() - 0.5) * 3.0;
					double tz = target.getZ() + (level.random.nextDouble() - 0.5) * 3.0;
					double ty = target.getY();
					teleportEntity(mob, tx, ty, tz);
				}
			}

			if (targetInTunnel && distance <= 16.0 && mob.tickCount % 60 == 0 && !isChannelingAbility(self)) {
				if (getB(self, "unlocked_sonic_boom")) {
					fireSuperchargedSonicBoom(level, self, target);
				}
			}
		}
	}

	private static void requestAsynchronousPathUpdate(Entity entity, Entity target, double speed) {
		if (!(entity instanceof Mob mob) || target == null || entity.level().isClientSide()) return;

		boolean isBlocking = getB(entity, "is_blocking") || getB(entity, "is_blocking_finish");
		boolean isExecutingAbility = getD(entity, "sentinel_slam_phase") > 0
			|| getD(entity, "sentinel_judgment_ticks") > 0
			|| getD(entity, "sentinel_die_kick_phase") > 0
			|| getD(entity, "rot_overhead_ticks") > 0
			|| getD(entity, "sentinel_uppercut_launch_ticks") > 0
			|| getD(entity, "sentinel_cc1_stage") > 0
			|| getD(entity, "sentinel_cc2_stage") > 0
			|| getD(entity, "sentinel_cc3_stage") > 0
			|| getD(entity, "sentinel_cc4_stage") > 0
			|| getD(entity, "sentinel_cc5_stage") > 0
			|| getB(entity, "is_uppercutting")
			|| getB(entity, "debug_force_overhead")
			|| getB(entity, "debug_force_rider");

		if (isBlocking || isExecutingAbility) {
			if (!mob.getNavigation().isDone()) {
				mob.getNavigation().stop();
			}
			if (entity.isInWater() || entity.isInLava()) {
				Vec3 cur = mob.getDeltaMovement();
				mob.setDeltaMovement(0.0, cur.y() > 0 ? 0.0 : cur.y() * 0.5, 0.0);
			}
			return;
		}

		double targetSpeed = speed;
		if (target.getY() < entity.getY() - 1.2) {
			targetSpeed = Math.min(targetSpeed, ROT_WALK_SPEED);
			if (getB(entity, "unlocked_teleportation") && mob.tickCount % 40 == 0 && entity.distanceTo(target) > 8.0) {
				double tx = target.getX() + (mob.getRandom().nextDouble() - 0.5) * 4.0;
				double tz = target.getZ() + (mob.getRandom().nextDouble() - 0.5) * 4.0;
				double ty = target.getY();
				net.minecraft.core.BlockPos targetPos = net.minecraft.core.BlockPos.containing(tx, ty, tz);
				if (entity.level().getBlockState(targetPos.below()).isCollisionShapeFullBlock(entity.level(), targetPos.below())) {
					entity.teleportTo(tx, ty, tz);
					if (entity.level() instanceof ServerLevel sLevel) {
						sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 15, 0.2, 0.5, 0.2, 0.1);
						playHostileSound(sLevel, entity, "entity.enderman.teleport", 1.0F, 1.0F);
					}
					return;
				}
			}
		}

		final double finalSpeed = targetSpeed;

		if (!ENABLE_ASYNC_PATHFINDING) {
			double dist = mob.distanceTo(target);
			if (dist <= 2.2) {
				if (!mob.getNavigation().isDone()) {
					mob.getNavigation().stop();
				}
			} else {
				double lX = getD(entity, "last_path_tx");
				double lY = getD(entity, "last_path_ty");
				double lZ = getD(entity, "last_path_tz");
				double lastSpeed = getD(entity, "last_path_speed");

				boolean navDone = mob.getNavigation().isDone() || mob.getNavigation().getPath() == null;
				boolean targetMoved = target.distanceToSqr(lX, lY, lZ) > 2.25;
				boolean speedChanged = Math.abs(finalSpeed - lastSpeed) > 0.15;
				boolean pathStale = mob.tickCount % 20 == 0;

				if (navDone || targetMoved || speedChanged || pathStale) {
					putD(entity, "last_path_tx", target.getX());
					putD(entity, "last_path_ty", target.getY());
					putD(entity, "last_path_tz", target.getZ());
					putD(entity, "last_path_speed", finalSpeed);
					mob.getNavigation().moveTo(target, finalSpeed);
				}
			}
			return;
		}

		if (mob.tickCount % 10 != 0 || Boolean.TRUE.equals(getB(entity, "sentinel_pathfinding_active"))) return;
		if (!mob.level().hasChunkAt(mob.blockPosition()) || !mob.level().hasChunkAt(target.blockPosition())) return;

		double lX = getD(entity, "last_path_tx");
		double lY = getD(entity, "last_path_ty");
		double lZ = getD(entity, "last_path_tz");
		if (target.distanceToSqr(lX, lY, lZ) < 1.44 && mob.getNavigation().getPath() != null) return;

		net.minecraft.server.MinecraftServer server = entity.getServer();
		if (server == null) return;

		putB(entity, "sentinel_pathfinding_active", true);
		putD(entity, "last_path_tx", target.getX());
		putD(entity, "last_path_ty", target.getY());
		putD(entity, "last_path_tz", target.getZ());

		java.util.concurrent.CompletableFuture.supplyAsync(() -> {
			try {
				synchronized (mob.getNavigation()) {
					return mob.getNavigation().createPath(target, 0);
				}
			} catch (Throwable t) {
				return null;
			}
		}, java.util.concurrent.ForkJoinPool.commonPool()).thenAcceptAsync(path -> {
			putB(entity, "sentinel_pathfinding_active", false);
			if (path != null && mob.isAlive() && target.isAlive()) {
				int chosenIndex = path.getNextNodeIndex();
				double minForwardDistSqr = Double.MAX_VALUE;
				Vec3 mobPos = mob.position();
				Vec3 targetVec = target.position().subtract(mobPos).normalize();

				for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
					net.minecraft.world.level.pathfinder.Node node = path.getNode(i);
					Vec3 nodeVec = new Vec3(node.x + 0.5 - mobPos.x, node.y - mobPos.y, node.z + 0.5 - mobPos.z);
					double dot = nodeVec.x * targetVec.x + nodeVec.z * targetVec.z;
					double distSqr = nodeVec.lengthSqr();
					if (dot >= 0.1 && distSqr < minForwardDistSqr) {
						minForwardDistSqr = distSqr;
						chosenIndex = i;
					}
				}
				if (chosenIndex < path.getNodeCount()) {
					net.minecraft.world.level.pathfinder.Node cNode = path.getNode(chosenIndex);
					double dSqr = mob.distanceToSqr(cNode.x + 0.5, cNode.y, cNode.z + 0.5);
					if (dSqr < 1.44 && chosenIndex + 1 < path.getNodeCount()) {
						chosenIndex++;
					}
					path.setNextNodeIndex(chosenIndex);
				}
				mob.getNavigation().moveTo(path, finalSpeed);
			}
		}, server);
	}

	private static void putD(Entity e, String k, double v) { setRotPersistentDouble(e, k, v); }
	private static void putB(Entity e, String k, boolean v) { setRotPersistentBoolean(e, k, v); }
	private static void putI(Entity e, String k, int v) { setRotPersistentInt(e, k, v); }
	private static void putS(Entity e, String k, String v) { setRotPersistentString(e, k, v); }

	private static void cancelActiveBeams(LevelAccessor level, Entity entity) {
		if (entity == null) return;
		if (getD(entity, "sentinel_solar_fire_ticks") > 0 || getD(entity, "sentinel_solar_charge_ticks") > 0
			|| getD(entity, "sentinel_cryo_fire_ticks") > 0 || getD(entity, "sentinel_cryo_charge_ticks") > 0) {
			clearDoubles(entity, "sentinel_solar_fire_ticks", "sentinel_solar_charge_ticks", "sentinel_cryo_fire_ticks", "sentinel_cryo_charge_ticks");
			putD(entity, "sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			if (level != null) stopHostileSound(level, entity, "the_backwoods:fractus_laser", 256.0);
		}
	}

	private static void resetPunchTicks(Entity entity) {
		if (entity == null) return;
		clearDoubles(entity, "sentinel_left_punch_ticks", "sentinel_right_punch_ticks", "sentinel_heavy_left_punch_ticks", "sentinel_heavy_right_punch_ticks");
	}

	private static void syncDataBool(RotEntity rot, Entity entity, net.minecraft.network.syncher.EntityDataAccessor<Boolean> accessor, String key) {
		try { putB(entity, key, rot.getEntityData().get(accessor)); } catch (Exception ignored) {}
	}

	private static boolean hasNBTKey(CompoundTag tag, String key) {
		return tag.contains(key);
	}
	private static void tryUnlockAbility(Entity entity, String key, boolean condition) {
		if (condition && !getB(entity, key)) {
			putB(entity, key, true);
			announceLearnedAbility(entity);
		}
	}

	private static double getOrInitTicks(Entity entity, String key, double base, double variance) {
		double val = getD(entity, key);
		if (val <= 0) {
			val = base + (entity instanceof LivingEntity living ? living.getRandom().nextDouble() : Math.random()) * variance;
			putD(entity, key, val);
		}
		return val;
	}

	private static DamageSource getBackwoodsDamage(LevelAccessor world, String id) {
		return getBackwoodsDamage(world, id, null);
	}

	private static DamageSource getBackwoodsDamage(LevelAccessor world, String id, Entity directEntity) {
		Level level = (world instanceof Level l) ? l : null;
		if (level == null) return null;
		String path = id.startsWith("the_backwoods:") ? id : "the_backwoods:" + id;
		var holder = level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(path)));
		return directEntity != null ? new DamageSource(holder, directEntity) : new DamageSource(holder);
	}

	private static DamageSource getMobAttackDamage(LevelAccessor world) {
		Level level = (world instanceof Level l) ? l : null;
		return (level != null) ? new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)) : null;
	}

	private static <T> void setEntityData(RotEntity rot, net.minecraft.network.syncher.EntityDataAccessor<T> accessor, T val) {
		try { rot.getEntityData().set(accessor, val); } catch (Exception ignored) {}
	}

	private static void setMotion(Entity e, double x, double y, double z) {
		if (e != null) {
			e.setDeltaMovement(new Vec3(x, y, z));
			e.hasImpulse = true;
		}
	}

	private static void setMotion(Entity e, Vec3 v) {
		if (e != null && v != null) {
			e.setDeltaMovement(v);
			e.hasImpulse = true;
		}
	}

	private static <T extends net.minecraft.core.particles.ParticleOptions> void spawnParticles(LevelAccessor world, T type, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
		if (world instanceof ServerLevel level) {
			level.sendParticles(type, x, y, z, count, dx, dy, dz, speed);
		}
	}

	private static void explodeWoodbound(Level level, Entity self, double x, double y, double z, float power) {
		level.explode(self, null, new net.minecraft.world.level.ExplosionDamageCalculator() {
			@Override
			public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, Entity ent) {
				return !isWoodboundEntity(ent, self);
			}
		}, x, y, z, power * (float) Math.min(5.0, getAdaptationMultiplier(self)), false, Level.ExplosionInteraction.MOB);
	}

	private static void clearDoubles(Entity entity, String... keys) {
		if (entity == null) return;
		for (String k : keys) putD(entity, k, 0.0);
	}

	private static void clearBooleans(Entity entity, String... keys) {
		if (entity == null) return;
		for (String k : keys) putB(entity, k, false);
	}

	private static boolean isContraptionEntity(Entity entity) {
		if (entity == null || !entity.isAlive()) {
			return false;
		}
		String className = entity.getClass().getName().toLowerCase(java.util.Locale.ROOT);
		if (className.contains("contraption") || className.contains("physicsentity") || className.contains("valkyrien") || className.contains("shipobject")) {
			return true;
		}
		ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		String idStr = typeId.toString().toLowerCase(java.util.Locale.ROOT);
		return idStr.contains("contraption") || idStr.contains("carriage") || idStr.contains("turret") || idStr.contains("cannon");
	}

	private static boolean isGunOrRadarBlock(BlockState state) {
		if (state == null || state.isAir()) {
			return false;
		}
		ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		String path = blockId.toString().toLowerCase(java.util.Locale.ROOT);
		return path.contains("radar") || path.contains("scanner") || path.contains("sensor") || path.contains("target")
			|| path.contains("cannon") || path.contains("autocannon") || path.contains("gun") || path.contains("breech")
			|| path.contains("barrel") || path.contains("turret") || path.contains("mortar") || path.contains("artillery")
			|| path.contains("recoil_spring") || path.contains("mount");
	}

	private static void destroyScorchedGunOrRadarBlock(ServerLevel level, @Nullable Entity attacker, BlockPos pos, BlockState state) {
		if (state.isAir()) {
			return;
		}
		level.destroyBlock(pos, false, attacker);
		ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		String path = blockId.toString().toLowerCase(java.util.Locale.ROOT);

		if (path.contains("radar") || path.contains("scanner") || path.contains("sensor") || path.contains("target")) {
			spawnScorchedItem(level, pos.getCenter(), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE, 2 + level.random.nextInt(3)));
			spawnScorchedItem(level, pos.getCenter(), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 1 + level.random.nextInt(2)));
			spawnScorchedItem(level, pos.getCenter(), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHARCOAL, 1 + level.random.nextInt(2)));
		} else {
			spawnScorchedItem(level, pos.getCenter(), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_NUGGET, 3 + level.random.nextInt(5)));
			spawnScorchedItem(level, pos.getCenter(), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, 1 + level.random.nextInt(2)));
			spawnScorchedItem(level, pos.getCenter(), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHARCOAL, 1 + level.random.nextInt(2)));
			spawnScorchedItem(level, pos.getCenter(), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLACKSTONE, 1));
		}

		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
		level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.04);
		playHostileSound(level, pos, "entity.generic.explode", 0.85F, 1.4F);
	}

	private static void destroyContraption(ServerLevel level, @Nullable Entity attacker, Entity contraptionEntity, Vec3 hitLocation, boolean angry) {
		if (contraptionEntity == null || !contraptionEntity.isAlive()) {
			return;
		}

		Vec3 center = contraptionEntity.position().add(0, contraptionEntity.getBbHeight() * 0.5, 0);

		boolean disassembledViaReflection = false;
		try {
			java.lang.reflect.Method getContraptionMethod = null;
			for (java.lang.reflect.Method m : contraptionEntity.getClass().getMethods()) {
				if (m.getName().equals("getContraption") && m.getParameterCount() == 0) {
					getContraptionMethod = m;
					break;
				}
			}
			if (getContraptionMethod != null) {
				Object contraptionObj = getContraptionMethod.invoke(contraptionEntity);
				if (contraptionObj != null) {
					java.lang.reflect.Method getBlocksMethod = null;
					for (java.lang.reflect.Method m : contraptionObj.getClass().getMethods()) {
						if ((m.getName().equals("getBlocks") || m.getName().equals("getBlocksMap")) && m.getParameterCount() == 0) {
							getBlocksMethod = m;
							break;
						}
					}
					if (getBlocksMethod != null) {
						Object blocksMap = getBlocksMethod.invoke(contraptionObj);
						if (blocksMap instanceof java.util.Map<?, ?> map) {
							for (Object val : map.values()) {
								if (val != null) {
									String blockString = val.toString().toLowerCase(java.util.Locale.ROOT);
									if (blockString.contains("radar") || blockString.contains("scanner") || blockString.contains("sensor")) {
										spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE, 2));
										spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 1));
										spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHARCOAL, 1));
									} else if (blockString.contains("cannon") || blockString.contains("autocannon") || blockString.contains("gun") || blockString.contains("breech") || blockString.contains("barrel") || blockString.contains("mount") || blockString.contains("iron") || blockString.contains("steel") || blockString.contains("brass")) {
										spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, 1));
										spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_NUGGET, 4));
										spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHARCOAL, 1));
									} else if (blockString.contains("wood") || blockString.contains("plank") || blockString.contains("log") || blockString.contains("sail")) {
										spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHARCOAL, 1));
									} else {
										spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLACKSTONE, 1));
									}
								}
							}
							disassembledViaReflection = true;
						}
					}
				}
			}
		} catch (Throwable ignored) {
		}

		if (!disassembledViaReflection) {
			int scrapCount = 3 + level.random.nextInt(4);
			for (int i = 0; i < scrapCount; i++) {
				spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_NUGGET, 3 + level.random.nextInt(4)));
				spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHARCOAL, 1 + level.random.nextInt(2)));
				spawnScorchedItem(level, center, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLACKSTONE, 1));
			}
		}

		BlockPos basePos = contraptionEntity.blockPosition();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos p = basePos.offset(dx, dy, dz);
					BlockState bs = level.getBlockState(p);
					if (bs.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || bs.is(net.minecraft.world.level.block.Blocks.DIRT)) {
						level.setBlock(p, net.minecraft.world.level.block.Blocks.COARSE_DIRT.defaultBlockState(), 3);
					} else if (bs.is(net.minecraft.world.level.block.Blocks.STONE) || bs.is(net.minecraft.world.level.block.Blocks.COBBLESTONE) || bs.is(net.minecraft.world.level.block.Blocks.ANDESITE)) {
						level.setBlock(p, net.minecraft.world.level.block.Blocks.BASALT.defaultBlockState(), 3);
					}
				}
			}
		}

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 2, 0.5, 0.5, 0.5, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 25, 1.2, 0.8, 1.2, 0.08);
		level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 15, 0.8, 0.6, 0.8, 0.1);
		level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 20, 1.0, 0.6, 1.0, 0.06);
		playHostileSound(level, center.x, center.y, center.z, "entity.generic.explode", 1.8F, 0.75F);
		playHostileSound(level, center.x, center.y, center.z, "block.anvil.destroy", 1.4F, 0.65F);
		playHostileSound(level, center.x, center.y, center.z, "block.fire.extinguish", 1.2F, 0.9F);

		contraptionEntity.ejectPassengers();
		contraptionEntity.discard();
	}

	private static void spawnScorchedItem(ServerLevel level, Vec3 pos, net.minecraft.world.item.ItemStack stack) {
		if (stack.isEmpty()) return;
		double rx = (level.random.nextDouble() - 0.5) * 1.2;
		double ry = level.random.nextDouble() * 0.6;
		double rz = (level.random.nextDouble() - 0.5) * 1.2;
		net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(level, pos.x + rx, pos.y + ry, pos.z + rz, stack);
		itemEntity.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.25, 0.2 + level.random.nextDouble() * 0.25, (level.random.nextDouble() - 0.5) * 0.25);
		itemEntity.setDefaultPickUpDelay();
		level.addFreshEntity(itemEntity);
	}

	private static void applyEffectSafe(LivingEntity target, net.minecraft.world.effect.MobEffectInstance effect) {
		if (target == null) return;
		if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return;
		target.addEffect(effect);
	}
}
// 1.21.1
