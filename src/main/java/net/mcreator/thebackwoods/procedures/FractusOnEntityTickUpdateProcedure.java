package net.mcreator.thebackwoods.procedures;
// 1.21.1
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;
import java.util.Comparator;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

@EventBusSubscriber
public class FractusOnEntityTickUpdateProcedure {

	public static final boolean USE_VANILLA_SOUNDS = false;
	public static final boolean CHAT_DEBUG = true;

	private static final ResourceLocation FRACTUS_ID = ResourceLocation.parse("the_backwoods:fractus");
	private static final ResourceLocation FRACTUS_LASER_SOUND = ResourceLocation.parse("the_backwoods:fractus_laser");
	private static final ResourceLocation FRACTUS_LASER_BURST_SOUND = ResourceLocation.parse("the_backwoods:fractus_laser_burst");
	private static final ResourceLocation FRACTUS_ANGER_SOUND = ResourceLocation.parse("the_backwoods:fractus_anger");
	private static final ResourceKey<Level> SUB_STRATA_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:backwoods"));
	private static final TagKey<EntityType<?>> WOODBOUND_ENTITIES_TAG = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities"));

	private static final double DETECTION_RANGE = 28.0;
	private static final double LASER_RANGE = 32.0;
	private static final double ANGRY_LASER_RANGE = 44.0;
	private static final double AIM_LOCK_TURN_RATE = 0.0900;
	private static final double ANGRY_AIM_LOCK_TURN_RATE = 0.1200;
	private static final double FIRING_AIM_TURN_RATE = 0.0350;
	private static final double ANGRY_FIRING_AIM_TURN_RATE = 0.0550;
	private static final double TARGET_LEAD_TICKS = 2.25;
	private static final double ANGRY_TARGET_LEAD_TICKS = 3.00;
	private static final double BURST_LASER_RANGE = 128.0;
	private static final double BURST_LASER_RADIUS = 2.60;
	private static final double BURST_LASER_TURN_RATE = 0.0150;
	private static final double BURST_LASER_SPIRAL_RADIUS = 1.20;
	private static final double BURST_LASER_SPIRAL_SPACING = 0.350;
	private static final int SUMMON_MIN_TICK_COUNT = 300;

	private static final double HOME_LEASH_RANGE = 42.0;

	private static final double PREFERRED_COMBAT_RANGE = 13.0;
	private static final double ANGRY_PREFERRED_COMBAT_RANGE = 17.0;
	private static final double TOO_CLOSE_RANGE = 6.0;
	private static final double HOVER_HEIGHT = 3.25;
	private static final double ANGRY_HOVER_HEIGHT = 4.35;
	private static final double ESCAPED_CONTAINMENT_HOVER_BONUS = 2.25;
	private static final double IDLE_HOVER_HEIGHT = 2.25;
	private static final double SUB_STRATA_PLAYER_COMBAT_HOVER_REDUCTION = 1.0;
	public static double VERDANT_INNER_RING_DISTANCE = 10.4;
	public static double VERDANT_OUTER_RING_DISTANCE = 16.9;
	public static int MAX_VERDANT_PRIME_GUARDS = 2;
	public static int MAX_VERDANT_STANDARD_GUARDS = 4;
	public static double VERDANT_RING_HOVER_CLEARANCE = 4.0;
	public static double FRACTUS_VERDANT_DETECTION_RANGE = 256.0;
	private static final double DRONE_ACCELERATION = 0.0850;
	private static final double MAX_DRONE_SPEED = 0.360;
	private static final double ANGRY_MAX_DRONE_SPEED = 0.440;
	private static final double IDLE_MAX_SPEED = 0.160;
	private static final double RETREAT_BURST_SPEED = 0.680;
	private static final double COOLDOWN_REPOSITION_SPEED = 0.580;
	private static final double VERTICAL_SPEED_LIMIT = 0.220;
	private static final double ORBIT_RADIUS = 3.00;
	private static final double ORBIT_SPEED = 0.0550;
	private static final double ANGRY_ORBIT_SPEED = 0.0750;
	private static final double BOB_AMOUNT = 0.38;
	private static final double BOB_SPEED = 0.120;
	private static final double IDLE_SCAN_SPEED = 0.0060;
	private static final double RETREAT_RANGE_BONUS = 8.0;
	private static final double COVER_SEARCH_RADIUS = 8.0;
	private static final double COVER_MIN_PLAYER_DISTANCE = 9.0;
	private static final int COVER_SEARCH_STEPS = 16;
	private static final int ANGER_RETREAT_TICKS = 35;
	private static final int COOLDOWN_REPOSITION_TICKS = 18;
	private static final int SUPPRESSION_TICKS = 45;
	private static final double SUPPRESSION_RANGE = 48.0;
	private static final double FLANK_RANGE_BONUS = 4.0;
	private static final double FLANK_SIDE_DISTANCE = 9.0;
	private static final double FLANK_REPOSITION_SPEED = 0.500;
	private static final int VULNERABILITY_TICKS = 30;
	private static final int COVER_FLANK_TICKS = 60;
	private static final int BURST_TOTAL_TICKS = 110;
	private static final int BURST_DEACT_DURATION_TICKS = 35; // 1.75s animation length (fractus_anims.java: laser_burst_deactivate)
	private static final int BURST_FIRE_PEAK_TICK = 50;
	private static final int BURST_CORE_END_TICK = 73;
	private static final int BURST_COOLDOWN_TICKS = 350;
	private static final int STRONG_TARGET_BURST_COOLDOWN_TICKS = 180;
	private static final double BURST_KNOCKBACK_HORIZONTAL = 2.40;
	private static final double BURST_KNOCKBACK_VERTICAL = 0.55;
	private static final float STRONG_TARGET_HEALTH_THRESHOLD = 80.0f;
	private static final float STRONG_TARGET_CURRENT_HEALTH_THRESHOLD = 60.0f;
	private static final int STRONG_TARGET_BURST_CHECK_INTERVAL = 80;
	private static final double STRONG_TARGET_BURST_CHANCE = 0.380;
	private static final int ESCAPED_BURST_CHECK_INTERVAL = 150;
	private static final double ESCAPED_BURST_CHANCE = 0.200;
	private static final double PROJECTILE_DODGE_CHANCE = 0.420;
	private static final double ANGRY_PROJECTILE_DODGE_CHANCE = 0.580;
	private static final double ESCAPED_PROJECTILE_DODGE_BONUS = 0.140;
	private static final int PROJECTILE_DODGE_ATTEMPTS = 12;
	private static final int STUCK_ESCAPE_TICKS = 12;
	private static final int OPEN_SPACE_SEARCH_STEPS = 20;
	private static final double OPEN_SPACE_SEARCH_RADIUS = 7.0;
	private static final double OPEN_SPACE_VERTICAL_RANGE = 4.0;
	private static final double IDLE_PATROL_RADIUS = 4.50;
	private static final int GLOWING_CORE_LIGHT_LEVEL = 0;

	private static final double IDLE_PATROL_REACH_DISTANCE = 0.850;
	private static final int IDLE_PATROL_WAIT_TICKS = 14;
	private static final int IDLE_PATROL_POINTS = 3;
	private static final double HARMFUL_BLOCK_AVOID_RADIUS = 2.25;
	private static final double HARMFUL_BLOCK_ESCAPE_SPEED = 0.620;
	private static final int TARGET_LOS_CANDIDATE_LIMIT = 8;
	private static final double ESCAPED_PASSIVE_FLEE_RANGE = 18.0;
	private static final double ESCAPED_MOB_RETALIATION_RANGE = 24.0;
	private static final double PASSIVE_FLEE_SPEED = 1.350;
	private static final double PASSIVE_FLEE_PUSH = 0.220;
	private static final int ESCAPED_THREAT_UPDATE_INTERVAL = 10;
	private static final int DESTROYING_FIRE_TICKS = 96;
	private static final int DESTROYING_COOLDOWN_TICKS = 120;
	private static final double DESTROYING_LASER_RANGE = 52.0;
	private static final double DESTROYING_VERTICAL_AIM_MIN = -0.35;
	private static final double DESTROYING_VERTICAL_AIM_MAX = 0.12;
	private static final double DESTROYING_TARGET_SCAN_RADIUS = 18.0;
	private static final int DESTROYING_AIM_REFRESH_TICKS = 8;

	private static final float ANGER_HEALTH_THRESHOLD = 20.0f;
	private static final int ANGER_SOUND_INTERVAL_TICKS = 90;

	private static final int CHARGE_TICKS = 28;
	private static final int FIRE_TICKS = 123;
	private static final int COOLDOWN_TICKS = 46;
	private static final int LASER_DEACT_DURATION_TICKS = 28; // 1.4167s animation length (fractus_anims.java: laser_deactivate)

	private static final int DAMAGE_INTERVAL_TICKS = 10;
	private static final int ANGRY_DAMAGE_INTERVAL_TICKS = 6;
	private static final float LASER_DAMAGE = 2.0f;
	private static final float ANGRY_LASER_DAMAGE = 3.0f;
	private static final float BURST_LASER_DAMAGE = 140.0f;

	private static final float WEAK_BLOCK_MAX_HARDNESS = 0.20f;
	private static final float ANGRY_WEAK_BLOCK_MAX_HARDNESS = 2.00f;
	private static final float BURST_BLOCK_MAX_HARDNESS = 60.00f;
	// Configurable burst laser thermal power for Fractus (Approach A: Cumulative Thermal Degradation)
	public static double BURST_LASER_THERMAL_POWER = 0.85;
	public static boolean BURST_BLOCK_DROPS = false;
	private static final java.util.Map<BlockPos, Float> BURST_THERMAL_PROGRESS = new java.util.concurrent.ConcurrentHashMap<>();
	private static final double BLOCK_BREAK_STEP = 0.250;
	private static final double BURST_BLOCK_BREAK_STEP = 0.750;

	private static final double LASER_PARTICLE_SPACING = 0.140;
	private static final double ANGRY_LASER_PARTICLE_SPACING = 0.080;
	private static final double CHARGE_PARTICLE_SPACING = 0.780;
	private static final double ANGRY_CHARGE_PARTICLE_SPACING = 0.500;
	private static final double LASER_PARTICLE_JITTER = 0.018;
	private static final double ANGRY_LASER_PARTICLE_JITTER = 0.028;
	private static final double CHARGE_PARTICLE_JITTER = 0.035;
	private static final double FIRING_SPIRAL_RADIUS = 0.055;
	private static final double FIRE_START_RING_RADIUS = 0.720;
	private static final int LASER_ENTITY_FIRE_TICKS = 100;
	private static final double LASER_TRAIL_FIRE_STEP = 1.350;

	// 1.21.1 custom white, red, and orange Registry particles with vanilla dust fallbacks (lazy loaded).
	private static net.minecraft.core.particles.ParticleOptions normalLaserParticle = null;
	private static net.minecraft.core.particles.ParticleOptions angryLaserParticle = null;
	private static net.minecraft.core.particles.ParticleOptions burstLaserParticle = null;
	private static net.minecraft.core.particles.ParticleOptions burstChargeStartParticle = null;
	private static net.minecraft.core.particles.ParticleOptions burstChargeMidParticle = null;

	private static net.minecraft.core.particles.ParticleOptions getNormalLaserParticle() {
		if (normalLaserParticle == null) {
			normalLaserParticle = getLaserParticle("orange", 0xFF6A00, 1.0f);
		}
		return normalLaserParticle;
	}

	private static net.minecraft.core.particles.ParticleOptions getAngryLaserParticle() {
		if (angryLaserParticle == null) {
			angryLaserParticle = getLaserParticle("red", 0xFF1A00, 1.25f);
		}
		return angryLaserParticle;
	}

	private static net.minecraft.core.particles.ParticleOptions getBurstLaserParticle() {
		if (burstLaserParticle == null) {
			burstLaserParticle = getLaserParticle("burst", 0xFF0000, 1.8f);
		}
		return burstLaserParticle;
	}

	private static net.minecraft.core.particles.ParticleOptions getBurstChargeStartParticle() {
		if (burstChargeStartParticle == null) {
			burstChargeStartParticle = getLaserParticle("white", 0xFFFFFF, 1.45f);
		}
		return burstChargeStartParticle;
	}

	private static net.minecraft.core.particles.ParticleOptions getBurstChargeMidParticle() {
		if (burstChargeMidParticle == null) {
			burstChargeMidParticle = getLaserParticle("orange", 0xFF2A00, 1.65f);
		}
		return burstChargeMidParticle;
	}

	@SuppressWarnings("unchecked")
	private static net.minecraft.core.particles.ParticleOptions getLaserParticle(String color, int fallbackRgbColor, float fallbackScale) {
		String[] potentialIds;
		if ("burst".equals(color)) {
			potentialIds = new String[]{"the_backwoods:fractus_laser_particle_burst", "the_backwoods:fractus_laser_particle_red", "the_backwoods:fractus_laser_red", "the_backwoods:red_fractus_laser_particle", "the_backwoods:fractus_laser_particle"};
		} else if ("red".equals(color)) {
			potentialIds = new String[]{
				"the_backwoods:fractus_laser_particle_red",
				"the_backwoods:fractus_laser_red",
				"the_backwoods:red_fractus_laser_particle",
				"the_backwoods:fractus_laser_particle"
			};
		} else if ("orange".equals(color)) {
			potentialIds = new String[]{
				"the_backwoods:fractus_laser_particle_orange",
				"the_backwoods:fractus_laser_orange",
				"the_backwoods:orange_fractus_laser_particle",
				"the_backwoods:fractus_laser_particle"
			};
		} else {
			potentialIds = new String[]{
				"the_backwoods:fractus_laser_particle_white",
				"the_backwoods:fractus_laser_white",
				"the_backwoods:white_fractus_laser_particle",
				"the_backwoods:fractus_laser_particle"
			};
		}

		for (String id : potentialIds) {
			net.minecraft.core.particles.ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(id));
			if (type != null) {
				if (type instanceof net.minecraft.core.particles.SimpleParticleType simpleType) {
					return simpleType;
				}
				try {
					return net.minecraft.core.particles.ColorParticleOption.create((net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.ColorParticleOption>) type, fallbackRgbColor | 0xFF000000);
				} catch (Exception ignored) {}
			}
		}

		float red = ((fallbackRgbColor >> 16) & 255) / 255.0f;
		float green = ((fallbackRgbColor >> 8) & 255) / 255.0f;
		float blue = (fallbackRgbColor & 255) / 255.0f;
		return new DustParticleOptions(new Vector3f(red, green, blue), fallbackScale);
	}

	private static final String K_HOME_SET = "fractus_home_set";
	private static final String K_HOME_X = "fractus_home_x";
	private static final String K_HOME_Y = "fractus_home_y";
	private static final String K_HOME_Z = "fractus_home_z";

	private static final String K_CHARGE = "fractus_laser_charge";
	private static final String K_FIRE = "fractus_laser_fire";
	private static final String K_COOLDOWN = "fractus_laser_cooldown";
	private static final String K_AIM_X = "fractus_laser_aim_x";
	private static final String K_AIM_Y = "fractus_laser_aim_y";
	private static final String K_AIM_Z = "fractus_laser_aim_z";
	private static final String K_BURST_AIM_X = "fractus_burst_aim_x";
	private static final String K_BURST_AIM_Y = "fractus_burst_aim_y";
	private static final String K_BURST_AIM_Z = "fractus_burst_aim_z";
	private static final String K_WAS_ANGRY = "fractus_was_angry";
	private static final String K_ANGER_RETREAT = "fractus_anger_retreat";
	private static final String K_SUPPRESSION = "fractus_suppression";
	private static final String K_VULNERABLE = "fractus_vulnerable";
	private static final String K_RETURNING_HOME = "fractus_returning_home";
	private static final String K_COVER_WAIT = "fractus_cover_wait";
	private static final String K_BURST_TIMER = "fractus_burst_timer";
	private static final String K_BURST_COOLDOWN = "fractus_burst_cooldown";
	private static final String K_BURST_DEACT_DELAY = "fractus_burst_deact_delay";
	private static final String K_BURST_DEACT_PLAY = "fractus_burst_deact_play";
	private static final String K_LASER_DEACT_PLAY = "fractus_laser_deact_play";
	private static final String K_DESTROYING_FIRE = "fractus_destroying_fire";
	private static final String K_DESTROYING_COOLDOWN = "fractus_destroying_cooldown";
	private static final String K_DESTROYING_AIM_X = "fractus_destroying_aim_x";
	private static final String K_DESTROYING_AIM_Y = "fractus_destroying_aim_y";
	private static final String K_DESTROYING_AIM_Z = "fractus_destroying_aim_z";

	private static final String K_ORBIT_SEED = "fractus_orbit_seed";
	private static final String K_ORBIT_SIDE = "fractus_orbit_side";
	private static final String K_IDLE_PATROL_INDEX = "fractus_idle_patrol_index";
	private static final String K_IDLE_PATROL_WAIT = "fractus_idle_patrol_wait";
	private static final String K_STUCK_TICKS = "fractus_stuck_ticks";
	private static final String K_LAST_X = "fractus_last_x";
	private static final String K_LAST_Y = "fractus_last_y";
	private static final String K_LAST_Z = "fractus_last_z";

	private static final String K_ANGER_SOUND_COOLDOWN = "fractus_anger_sound_cooldown";
	private static final String K_LASER_SOUND_PLAYING = "fractus_laser_sound_playing";
	private static final String K_ESCAPED_THREAT_UPDATE = "fractus_escaped_threat_update";

	private static final String K_LASER_STATE = "fractus_laser_state";

	private static final String K_SUMMON_ROLE = "fractus_summon_role";
	private static final String K_SUMMON_TICKS = "fractus_summon_ticks";
	private static final String K_SUMMON_START_TICKS = "fractus_summon_start_ticks";
	private static final String K_SUMMON_CENTER_X = "fractus_summon_center_x";
	private static final String K_SUMMON_CENTER_Y = "fractus_summon_center_y";
	private static final String K_SUMMON_CENTER_Z = "fractus_summon_center_z";

	private static final String K_SUMMON_REMAINING = "fractus_summon_remaining";
	private static final String K_SUMMON_COOLDOWN = "fractus_summon_cooldown";
	private static final String K_SUMMON_INITIALIZED = "fractus_summon_init";

	private static final int SUMMON_INTERVAL_COOLDOWN_TICKS = 600;

	private static final double SUMMON_ROT_CHANCE = 0.30;

	private static final int ADAPTIVE_SUMMON_DURATION_INCREASE_PER_DEATH = 120;

	private static final int SUMMON_BASE_DURATION_OFFSET = 420;

	private static final int SUMMON_DURATION_REDUCTION_PER_PARTICIPANT = 40;

	private static net.minecraft.core.particles.ParticleOptions summonLaserParticle = null;

	private static final java.util.Map<String, java.util.Optional<java.lang.reflect.Field>> SYNCED_DATA_FIELD_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	private static <T> EntityDataAccessor<T> getEntityDataAccessor(Entity entity, String paramName) {
		if (entity == null || paramName == null) return null;
		String cacheKey = entity.getClass().getName() + ":" + paramName;
		java.util.Optional<java.lang.reflect.Field> optField = SYNCED_DATA_FIELD_CACHE.get(cacheKey);
		if (optField == null) {
			java.lang.reflect.Field field = null;
			String[] candidateNames = new String[]{
				"DATA_" + paramName,
				"DATA_" + paramName.toUpperCase(),
				paramName,
				"DATA_" + paramName.toLowerCase()
			};
			Class<?> current = entity.getClass();
			while (current != null && current != Object.class) {
				for (String name : candidateNames) {
					try {
						java.lang.reflect.Field f = current.getDeclaredField(name);
						f.setAccessible(true);
						field = f;
						break;
					} catch (NoSuchFieldException ignored) {}
				}
				if (field != null) break;
				for (java.lang.reflect.Field f : current.getDeclaredFields()) {
					if (f.getType() == EntityDataAccessor.class) {
						if (f.getName().equalsIgnoreCase("DATA_" + paramName) || f.getName().equalsIgnoreCase(paramName)) {
							f.setAccessible(true);
							field = f;
							break;
						}
					}
				}
				if (field != null) break;
				current = current.getSuperclass();
			}
			optField = java.util.Optional.ofNullable(field);
			SYNCED_DATA_FIELD_CACHE.put(cacheKey, optField);
		}
		if (optField.isPresent()) {
			try {
				return (EntityDataAccessor<T>) optField.get().get(null);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	public static void setSyncedDataBoolean(Entity entity, String paramName, boolean value) {
		if (entity == null) return;
		EntityDataAccessor<Boolean> accessor = getEntityDataAccessor(entity, paramName);
		if (accessor != null) {
			try {
				entity.getEntityData().set(accessor, value);
			} catch (Exception ignored) {}
		}
		entity.getPersistentData().putBoolean(paramName, value);
	}

	public static boolean getSyncedDataBoolean(Entity entity, String paramName, boolean defaultValue) {
		if (entity == null) return defaultValue;
		EntityDataAccessor<Boolean> accessor = getEntityDataAccessor(entity, paramName);
		if (accessor != null) {
			try {
				boolean val = entity.getEntityData().get(accessor);
				entity.getPersistentData().putBoolean(paramName, val);
				return val;
			} catch (Exception ignored) {}
		}
		return persistentBoolean(entity, paramName, defaultValue);
	}

	public static void setSyncedDataInt(Entity entity, String paramName, int value) {
		if (entity == null) return;
		EntityDataAccessor<Integer> accessor = getEntityDataAccessor(entity, paramName);
		if (accessor != null) {
			try {
				entity.getEntityData().set(accessor, value);
			} catch (Exception ignored) {}
		}
		entity.getPersistentData().putInt(paramName, value);
	}

	public static int getSyncedDataInt(Entity entity, String paramName, int defaultValue) {
		if (entity == null) return defaultValue;
		EntityDataAccessor<Integer> accessor = getEntityDataAccessor(entity, paramName);
		if (accessor != null) {
			try {
				return entity.getEntityData().get(accessor);
			} catch (Exception ignored) {}
		}
		return persistentInt(entity, paramName, defaultValue);
	}

	private static void setLaserState(Entity entity, int state) {
		if (entity == null) return;

		int burstTicks = persistentInt(entity, K_BURST_TIMER, 0);
		int burstDeactDelay = persistentInt(entity, K_BURST_DEACT_DELAY, 0);
		int burstDeactPlay = persistentInt(entity, K_BURST_DEACT_PLAY, 0);
		int vulnerable = persistentInt(entity, K_VULNERABLE, 0);
		boolean burstSequenceActive = burstTicks > 0 || burstDeactDelay > 0 || burstDeactPlay > 0;

		if (burstSequenceActive) {
			setSyncedDataBoolean(entity, "is_laser_charging", false);
			setSyncedDataBoolean(entity, "is_laser_firing", false);
			setSyncedDataBoolean(entity, "is_laser_activating", false);
			setSyncedDataBoolean(entity, "is_laser_deactivating", false);
			setSyncedDataBoolean(entity, "is_laser_activate", false);
			setSyncedDataBoolean(entity, "is_laser_deactivate", false);
			setSyncedDataBoolean(entity, "laser_activate", false);
			setSyncedDataBoolean(entity, "laser_deactivate", false);
			return;
		}

		if (vulnerable > 0 && (state == 1 || state == 2 || state == 3)) {
			state = 0;
		}

		entity.getPersistentData().putInt(K_LASER_STATE, state);
		setSyncedDataInt(entity, "laser_state", state);
		boolean charging = (state == 1 || state == 2);
		boolean firing = (state == 3);
		boolean activating = (state == 1 || state == 2 || state == 3);
		boolean deactivating = (state == 4);
		setSyncedDataBoolean(entity, "is_laser_charging", charging);
		setSyncedDataBoolean(entity, "is_laser_firing", firing);
		setSyncedDataBoolean(entity, "is_laser_activating", activating);
		setSyncedDataBoolean(entity, "is_laser_deactivating", deactivating);
		setSyncedDataBoolean(entity, "is_laser_activate", activating);
		setSyncedDataBoolean(entity, "is_laser_deactivate", deactivating);
		setSyncedDataBoolean(entity, "laser_activate", activating);
		setSyncedDataBoolean(entity, "laser_deactivate", deactivating);

		setSyncedDataBoolean(entity, "is_laser_burst_activating", false);
		setSyncedDataBoolean(entity, "is_laser_burst_deactivating", false);
		setSyncedDataBoolean(entity, "is_laser_burst_activate", false);
		setSyncedDataBoolean(entity, "is_laser_burst_deactivate", false);
		setSyncedDataBoolean(entity, "is_burst_laser_activating", false);
		setSyncedDataBoolean(entity, "is_burst_laser_deactivating", false);
		setSyncedDataBoolean(entity, "is_burst_activating", false);
		setSyncedDataBoolean(entity, "is_burst_deactivating", false);
		setSyncedDataBoolean(entity, "is_laser_burst", false);
		setSyncedDataBoolean(entity, "is_burst_charging", false);
		setSyncedDataBoolean(entity, "is_burst_firing", false);
	}

	private static void setBurstLaserState(Entity entity, int state) {
		if (entity == null) return;
		entity.getPersistentData().putInt(K_LASER_STATE, state);
		entity.getPersistentData().putInt("fractus_burst_state", state);
		setSyncedDataInt(entity, "laser_state", state);
		setSyncedDataInt(entity, "burst_state", state);
		boolean burstActivating = (state == 1 || state == 2 || state == 3);
		boolean burstCharging = (state == 1 || state == 2);
		boolean burstFiring = (state == 3);
		boolean burstDeactivating = (state == 4);
		setSyncedDataBoolean(entity, "is_burst_charging", burstCharging);
		setSyncedDataBoolean(entity, "is_burst_firing", burstFiring);
		setSyncedDataBoolean(entity, "is_laser_charging", false);
		setSyncedDataBoolean(entity, "is_laser_firing", false);
		setSyncedDataBoolean(entity, "is_laser_activating", false);
		setSyncedDataBoolean(entity, "is_laser_deactivating", false);
		setSyncedDataBoolean(entity, "is_laser_burst_activating", burstActivating);
		setSyncedDataBoolean(entity, "is_laser_burst_deactivating", burstDeactivating);
		setSyncedDataBoolean(entity, "is_laser_burst_activate", burstActivating);
		setSyncedDataBoolean(entity, "is_laser_burst_deactivate", burstDeactivating);
		setSyncedDataBoolean(entity, "is_burst_laser_activating", burstActivating);
		setSyncedDataBoolean(entity, "is_burst_laser_deactivating", burstDeactivating);
		setSyncedDataBoolean(entity, "is_burst_activating", burstActivating);
		setSyncedDataBoolean(entity, "is_burst_deactivating", burstDeactivating);
		setSyncedDataBoolean(entity, "is_laser_burst", burstActivating);
	}

	private static void setSummoningState(Entity entity, boolean summoning) {
		if (entity == null) return;
		setSyncedDataBoolean(entity, "is_summoning", summoning);
		setSyncedDataBoolean(entity, "is_laser_activating", summoning);
		setSyncedDataBoolean(entity, "is_laser_deactivating", !summoning);
		entity.setSwimming(summoning);
	}

	public static void execute() {
	}

	@SubscribeEvent
	public static void onServerChat(ServerChatEvent event) {
		if (!CHAT_DEBUG) {
			return;
		}
		String raw = event.getRawText();
		if (raw == null) {
			return;
		}
		String msg = raw.trim().toLowerCase();
		if (!msg.equals("burst") && !msg.equals("burst fractus") && !msg.equals("fractus burst") && !msg.equals("fractus")) {
			return;
		}
		ServerPlayer player = event.getPlayer();
		if (player == null) {
			return;
		}
		ServerLevel level = player.serverLevel();
		java.util.List<Entity> fractusList = level.getEntities((Entity) null, player.getBoundingBox().inflate(64.0), FractusOnEntityTickUpdateProcedure::isFractus);
		if (fractusList.isEmpty()) {
			return;
		}
		fractusList.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
		Entity fractus = fractusList.get(0);

		LivingEntity target = findNearestDebugBurstTarget(level, fractus);
		Vec3 targetBlock = null;
		if (target == null) {
			targetBlock = findDebugBurstBlock(level, fractus);
		}
		startDebugBurstAttack(level, fractus, target, targetBlock);
	}

	private static LivingEntity findNearestDebugBurstTarget(ServerLevel level, Entity entity) {
		Vec3 pos = entity.position();
		java.util.List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(48.0), e -> canBurstDamage(entity, e));
		if (list.isEmpty()) {
			return null;
		}
		list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pos)));
		return list.get(0);
	}

	private static Vec3 findDebugBurstBlock(ServerLevel level, Entity entity) {
		Vec3 start = laserStart(entity);
		Vec3 look = entity.getLookAngle();
		if (look.lengthSqr() < 0.001) {
			look = new Vec3(0, 0, 1);
		}
		Vec3 end = start.add(look.scale(32.0));
		BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
		if (hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceTo(start) >= 6.0) {
			return hit.getLocation();
		}
		return start.add(look.scale(12.0));
	}

	private static void startDebugBurstAttack(ServerLevel level, Entity entity, LivingEntity target, Vec3 targetBlock) {
		int burstTicks = BURST_TOTAL_TICKS;
		entity.getPersistentData().putInt(K_BURST_TIMER, burstTicks - 1);
		entity.getPersistentData().putInt(K_BURST_COOLDOWN, BURST_COOLDOWN_TICKS);
		playFractusLaserBurstSound(level, entity);
		stopFractusLaserSound(level, entity);
		entity.getPersistentData().putInt(K_CHARGE, 0);
		entity.getPersistentData().putInt(K_FIRE, 0);
		setBurstLaserState(entity, 1);

		if (targetBlock != null) {
			entity.getPersistentData().putDouble("fractus_burst_target_block_x", targetBlock.x);
			entity.getPersistentData().putDouble("fractus_burst_target_block_y", targetBlock.y);
			entity.getPersistentData().putDouble("fractus_burst_target_block_z", targetBlock.z);

			Vec3 dir = targetBlock.subtract(laserStart(entity)).normalize();
			entity.getPersistentData().putDouble(K_BURST_AIM_X, dir.x);
			entity.getPersistentData().putDouble(K_BURST_AIM_Y, dir.y);
			entity.getPersistentData().putDouble(K_BURST_AIM_Z, dir.z);
			entity.getPersistentData().putDouble(K_AIM_X, dir.x);
			entity.getPersistentData().putDouble(K_AIM_Y, dir.y);
			entity.getPersistentData().putDouble(K_AIM_Z, dir.z);
			setEntityLookDirection(entity, dir);
		} else if (target != null) {
			entity.getPersistentData().remove("fractus_burst_target_block_x");
			entity.getPersistentData().remove("fractus_burst_target_block_y");
			entity.getPersistentData().remove("fractus_burst_target_block_z");

			Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(laserStart(entity)).normalize();
			entity.getPersistentData().putDouble(K_BURST_AIM_X, dir.x);
			entity.getPersistentData().putDouble(K_BURST_AIM_Y, dir.y);
			entity.getPersistentData().putDouble(K_BURST_AIM_Z, dir.z);
			entity.getPersistentData().putDouble(K_AIM_X, dir.x);
			entity.getPersistentData().putDouble(K_AIM_Y, dir.y);
			entity.getPersistentData().putDouble(K_AIM_Z, dir.z);
			setEntityLookDirection(entity, dir);
		}
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		Entity entity = event.getEntity();

		if (entity == null || !isFractus(entity)) {
			return;
		}

		execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity);
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
		Entity entity = event.getEntity();

		if (entity == null || !isFractus(entity) || !(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		Entity.RemovalReason reason = entity.getRemovalReason();
		if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
			cleanupLightSource(serverLevel, entity);
		}
	}

	@SubscribeEvent
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		LivingEntity entity = event.getEntity();

		if (entity == null || !isFractus(entity)) {
			return;
		}

		boolean angry = isAngry(entity);
		boolean projectileDamage = isProjectileDamage(event.getSource());

		if (projectileDamage) {
			if (tryDodgeProjectile(entity, event.getSource())) {
				event.setCanceled(true);
				return;
			}
			return;
		}

		if (angry || !projectileDamage) {
			return;
		}

		event.setCanceled(true);

		if (entity.level() instanceof ServerLevel serverLevel) {
			spawnShieldParticles(serverLevel, entity);
		}
	}

	private static BlockPos findNearbyPortalFrame(ServerLevel level, BlockPos start, int radius) {
		for (BlockPos pos : BlockPos.betweenClosed(start.offset(-radius, -6, -radius), start.offset(radius, 6, radius))) {
			if (checkPortalFrameStructure(level, pos, true)) {
				return pos.immutable();
			}
			if (checkPortalFrameStructure(level, pos, false)) {
				return pos.immutable();
			}
		}
		return null;
	}

	private static boolean checkPortalFrameStructure(ServerLevel level, BlockPos pos, boolean xAligned) {
		if (xAligned) {
			if (level.getBlockState(pos.below()).getBlock() != Blocks.OBSIDIAN
				|| level.getBlockState(pos.east().below()).getBlock() != Blocks.OBSIDIAN) {
				return false;
			}
			if (level.getBlockState(pos.above(3)).getBlock() != Blocks.OBSIDIAN
				|| level.getBlockState(pos.east().above(3)).getBlock() != Blocks.OBSIDIAN) {
				return false;
			}
			for (int dy = 0; dy < 3; dy++) {
				if (level.getBlockState(pos.west().above(dy)).getBlock() != Blocks.OBSIDIAN) {
					return false;
				}
			}
			for (int dy = 0; dy < 3; dy++) {
				if (level.getBlockState(pos.east(2).above(dy)).getBlock() != Blocks.OBSIDIAN) {
					return false;
				}
			}
		} else {
			if (level.getBlockState(pos.below()).getBlock() != Blocks.OBSIDIAN
				|| level.getBlockState(pos.south().below()).getBlock() != Blocks.OBSIDIAN) {
				return false;
			}
			if (level.getBlockState(pos.above(3)).getBlock() != Blocks.OBSIDIAN
				|| level.getBlockState(pos.south().above(3)).getBlock() != Blocks.OBSIDIAN) {
				return false;
			}
			for (int dy = 0; dy < 3; dy++) {
				if (level.getBlockState(pos.north().above(dy)).getBlock() != Blocks.OBSIDIAN) {
					return false;
				}
			}
			for (int dy = 0; dy < 3; dy++) {
				if (level.getBlockState(pos.south(2).above(dy)).getBlock() != Blocks.OBSIDIAN) {
					return false;
				}
			}
		}
		for (int dy = 0; dy < 3; dy++) {
			for (int dx = 0; dx < 2; dx++) {
				BlockPos interiorPos = xAligned ? pos.east(dx).above(dy) : pos.south(dx).above(dy);
				BlockState state = level.getBlockState(interiorPos);
				if (state.getBlock() != Blocks.AIR && state.getBlock() != Blocks.FIRE && state.getBlock() != Blocks.NETHER_PORTAL) {
					float hardness = state.getDestroySpeed(level, interiorPos);
					if (hardness < 0 || hardness > 5.0f || state.getBlock() == Blocks.OBSIDIAN) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static boolean isPortalFrameXAligned(ServerLevel level, BlockPos pos) {
		return checkPortalFrameStructure(level, pos, true);
	}

	private static boolean isPortalFrameLit(ServerLevel level, BlockPos pos, boolean xAligned) {
		for (int dy = 0; dy < 3; dy++) {
			for (int dx = 0; dx < 2; dx++) {
				BlockPos interiorPos = xAligned ? pos.east(dx).above(dy) : pos.south(dx).above(dy);
				if (level.getBlockState(interiorPos).getBlock() == Blocks.NETHER_PORTAL) {
					return true;
				}
			}
		}
		return false;
	}

	private static BlockPos findLowestBlockedInterior(ServerLevel level, BlockPos pos, boolean xAligned) {
		for (int dy = 0; dy < 3; dy++) {
			for (int dx = 0; dx < 2; dx++) {
				BlockPos interiorPos = xAligned ? pos.east(dx).above(dy) : pos.south(dx).above(dy);
				BlockState state = level.getBlockState(interiorPos);
				if (state.getBlock() != Blocks.AIR && state.getBlock() != Blocks.FIRE && state.getBlock() != Blocks.NETHER_PORTAL) {
					return interiorPos.immutable();
				}
			}
		}
		return null;
	}

	private static boolean handleNetherPortalInteractions(ServerLevel level, Entity entity) {
		if (level.dimension() != Level.OVERWORLD) {
			if (entity.getPortalCooldown() > 0) {
				BlockPos entityPos = entity.blockPosition();
				if (level.getBlockState(entityPos).getBlock() == Blocks.NETHER_PORTAL
					|| level.getBlockState(entityPos.above()).getBlock() == Blocks.NETHER_PORTAL) {
					double angle = entity.getRandom().nextDouble() * Math.PI * 2;
					entity.setDeltaMovement(Math.cos(angle) * 0.15, 0.05, Math.sin(angle) * 0.15);
				}
			}
			return false;
		}

		if (entity.getPortalCooldown() > 0) {
			BlockPos entityPos = entity.blockPosition();
			if (level.getBlockState(entityPos).getBlock() == Blocks.NETHER_PORTAL
				|| level.getBlockState(entityPos.above()).getBlock() == Blocks.NETHER_PORTAL) {
				double angle = entity.getRandom().nextDouble() * Math.PI * 2;
				entity.setDeltaMovement(Math.cos(angle) * 0.15, 0.05, Math.sin(angle) * 0.15);
			}
			return false;
		}

		if (!persistentBoolean(entity, "fractus_portal_seeker_set", false)) {
			entity.getPersistentData().putBoolean("fractus_portal_seeker_set", true);
			entity.getPersistentData().putBoolean("fractus_portal_seeker", entity.getRandom().nextDouble() < 0.40);
		}

		if (!persistentBoolean(entity, "fractus_portal_seeker", false)) {
			return false;
		}

		BlockPos currentPos = entity.blockPosition();
		BlockPos framePos = findNearbyPortalFrame(level, currentPos, 16);
		if (framePos != null) {
			boolean xAligned = isPortalFrameXAligned(level, framePos);
			Vec3 frameCenter = framePos.getCenter().add(xAligned ? 0.5 : 0.0, 1.0, xAligned ? 0.0 : 0.5);

			if (isPortalFrameLit(level, framePos, xAligned)) {
				entity.lookAt(EntityAnchorArgument.Anchor.EYES, frameCenter);
				moveToward(entity, frameCenter, MAX_DRONE_SPEED);
				level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY(), entity.getZ(), 5, 0.3, 0.3, 0.3, 0.1);

				BlockPos entityPos = entity.blockPosition();
				if (level.getBlockState(entityPos).getBlock() == Blocks.NETHER_PORTAL
					|| level.getBlockState(entityPos.above()).getBlock() == Blocks.NETHER_PORTAL) {
					if (entity.getPortalCooldown() == 0) {
						entity.setPortalCooldown(80);
						level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.2);
					}
				}
				return true;
			}

			BlockPos blockedPos = findLowestBlockedInterior(level, framePos, xAligned);
			if (blockedPos != null) {
				Vec3 blockedCenter = blockedPos.getCenter();
				entity.lookAt(EntityAnchorArgument.Anchor.EYES, blockedCenter);

				double dist = entity.position().distanceTo(frameCenter);
				if (dist > 8.0) {
					moveToward(entity, frameCenter, IDLE_MAX_SPEED);
				} else if (dist < 4.0) {
					Vec3 away = entity.position().subtract(frameCenter).normalize().scale(6.0);
					moveToward(entity, frameCenter.add(away), IDLE_MAX_SPEED);
				} else {
					entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5));
				}

				if (entity.tickCount % 5 == 0) {
					spawnLaser(level, laserStart(entity), blockedCenter, LASER_PARTICLE_SPACING, LASER_PARTICLE_JITTER, true, false);
					if (entity.tickCount % 12 == 0) {
						level.playSound(null, currentPos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0f, 1.2f);
					}
				}

				if (entity.tickCount % 15 == 0) {
					level.destroyBlock(blockedPos, true, entity);
					level.playSound(null, blockedPos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 1.0f, 1.5f);
				}
				return true;
			}

			BlockPos ignitPos = framePos;
			Vec3 ignitPosCenter = ignitPos.getCenter();
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, ignitPosCenter);

			double dist = entity.position().distanceTo(frameCenter);
			if (dist > 8.0) {
				moveToward(entity, frameCenter, IDLE_MAX_SPEED);
			} else if (dist < 4.0) {
				Vec3 away = entity.position().subtract(frameCenter).normalize().scale(6.0);
				moveToward(entity, frameCenter.add(away), IDLE_MAX_SPEED);
			} else {
				entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5));
			}

			if (entity.tickCount % 5 == 0) {
				spawnLaser(level, laserStart(entity), ignitPosCenter, LASER_PARTICLE_SPACING, LASER_PARTICLE_JITTER, true, false);
				if (entity.tickCount % 20 == 0) {
					level.playSound(null, currentPos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0f, 1.2f);
				}
			}

			if (entity.tickCount % 15 == 0) {
				level.setBlock(ignitPos, Blocks.FIRE.defaultBlockState(), 3);
				level.playSound(null, ignitPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, ignitPosCenter.x, ignitPosCenter.y, ignitPosCenter.z, 10, 0.2, 0.2, 0.2, 0.1);
			}
			return true;
		}

		return false;
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !isFractus(entity)) {
			return;
		}

		if (entity instanceof Mob mob && mob.getTarget() != null) {
			if (!mob.getTarget().isAlive() || isNonSurvivalPlayer(mob.getTarget())) {
				mob.setTarget(null);
			}
		}

		ensureHome(entity, x, y, z);

		if (!(world instanceof ServerLevel serverLevel)) {
			return;
		}

		if (checkFractusDespawn(serverLevel, entity)) {
			return;
		}

		if (handleSummoningRitual(serverLevel, entity)) {
			return;
		}

		if (handleActivationWarmup(serverLevel, entity)) {
			return;
		}

		int summonCooldown = persistentInt(entity, K_SUMMON_COOLDOWN, 0);
		if (summonCooldown > 0) {
			entity.getPersistentData().putInt(K_SUMMON_COOLDOWN, summonCooldown - 1);
		}

		if (checkAndStartSummoningRitual(serverLevel, entity)) {
			return;
		}

		updateLightSource(serverLevel, entity);

		if (handleNetherPortalInteractions(serverLevel, entity)) {
			return;
		}

		int vulnerabilityTicks = tickStoredTimer(entity, K_VULNERABLE);

		if (entity instanceof LivingEntity livingEntity) {
			if (vulnerabilityTicks > 0) {
				livingEntity.removeEffect(MobEffects.REGENERATION);
				spawnVulnerabilityParticles(serverLevel, entity, vulnerabilityTicks);
			} else {
				applyInfiniteRegeneration(livingEntity);
			}
		}

		boolean angry = isAngry(entity);
		boolean wasAngry = persistentBoolean(entity, K_WAS_ANGRY, false);

		if (angry && !wasAngry) {
			entity.getPersistentData().putInt(K_ANGER_RETREAT, ANGER_RETREAT_TICKS);
			spawnAngerTransitionParticles(serverLevel, entity);
		}

		entity.getPersistentData().putBoolean(K_WAS_ANGRY, angry);
		handleAngerSound(serverLevel, entity, angry);

		int angerRetreatTicks = tickStoredTimer(entity, K_ANGER_RETREAT);
		int suppressionTicks = tickStoredTimer(entity, K_SUPPRESSION);
		int cooldown = tickStoredTimer(entity, K_COOLDOWN);
		int fireTicks = Math.max(0, persistentInt(entity, K_FIRE, 0));
		int chargeTicks = Math.max(0, persistentInt(entity, K_CHARGE, 0));
		int burstTicks = tickStoredTimer(entity, K_BURST_TIMER);
		int burstCooldown = tickStoredTimer(entity, K_BURST_COOLDOWN);
		int burstDeactDelay = tickStoredTimer(entity, K_BURST_DEACT_DELAY);
		int burstDeactPlay = tickStoredTimer(entity, K_BURST_DEACT_PLAY);
		int laserDeactPlay = tickStoredTimer(entity, K_LASER_DEACT_PLAY);

		if (burstDeactDelay == 1) {
			setBurstLaserState(entity, 4);
			entity.getPersistentData().putInt(K_BURST_DEACT_PLAY, BURST_DEACT_DURATION_TICKS);
		} else if (burstDeactPlay == 1) {
			setBurstLaserState(entity, 0);
		} else if (laserDeactPlay == 1) {
			setLaserState(entity, 0);
		}
		int previousLaserState = persistentInt(entity, K_LASER_STATE, 0);
		LivingEntity target = findTarget(serverLevel, entity, x, y, z);
		boolean isGuarding = findNearestVerdantEngine(serverLevel, entity, FRACTUS_VERDANT_DETECTION_RANGE) != null;
		double projRange = isGuarding ? 30.0 : 15.0; // doubled intercept range when guarding Verdant
		Entity incomingProjectile = findIncomingProjectile(serverLevel, entity, projRange);
		Entity laserTarget = incomingProjectile != null ? incomingProjectile : target;

		if (incomingProjectile != null) {
			setSyncedDataBoolean(entity, "is_laser_activating", true);
		}

		entity.getPersistentData().putInt("fractus_target_id", target != null && target.isAlive() ? target.getId() : -1);

		if (isEscapedContainmentDimension(entity)) {
			alertNearbyMobsAndPanicPassives(serverLevel, entity);
		}

		if (cooldown > 0) {
			stopFractusLaserSound(serverLevel, entity);
		}

		double currentLaserRange = angry ? ANGRY_LASER_RANGE : LASER_RANGE;
		boolean canSeeTarget = laserTarget != null && hasClearShot(serverLevel, entity, laserTarget, currentLaserRange);
		boolean canDrill = target != null && canLaserDrillThrough(serverLevel, entity, target, currentLaserRange, angry);
		double distance = laserTarget != null ? entity.distanceTo(laserTarget) : Double.MAX_VALUE;

		double evalBurst = evaluateBurstAttackValue(serverLevel, entity, target, burstTicks, burstCooldown);
		double evalRegular = incomingProjectile != null ? 100.0 : evaluateRegularLaserValue(serverLevel, entity, target, currentLaserRange, canSeeTarget, canDrill);

		// Decision Hysteresis & Plan Commitment:
		String currentPlan = entity.getPersistentData().getString("fractus_active_plan");
		double HYSTERESIS_THRESHOLD = 20.0;
		String chosenPlan = "regular";

		if (evalBurst >= 50.0 && evalBurst > evalRegular) {
			if ("regular".equals(currentPlan) && evalRegular > 0.0) {
				if (evalBurst >= evalRegular + HYSTERESIS_THRESHOLD) {
					chosenPlan = "burst";
				} else {
					chosenPlan = "regular";
				}
			} else {
				chosenPlan = "burst";
			}
		}

		entity.getPersistentData().putString("fractus_active_plan", chosenPlan);

		if ("burst".equals(chosenPlan) && burstTicks <= 0 && burstCooldown <= 0 && evalBurst >= 50.0) {
			burstTicks = BURST_TOTAL_TICKS;
			entity.getPersistentData().putInt(K_BURST_TIMER, burstTicks - 1);
			entity.getPersistentData().putInt(K_BURST_COOLDOWN, burstCooldownTicksForTarget(target));
			playFractusLaserBurstSound(serverLevel, entity);
			stopFractusLaserSound(serverLevel, entity);
			entity.getPersistentData().putInt(K_CHARGE, 0);
			entity.getPersistentData().putInt(K_FIRE, 0);
			setBurstLaserState(entity, 1);
		}

		if (burstTicks > 0) {
			LivingEntity combatTarget = target != null ? target : retaliationTarget(entity);
			// Heavy braking and stationary hover during laser burst attack
			Vec3 currentVel = entity.getDeltaMovement();
			entity.setDeltaMovement(currentVel.x * 0.1, currentVel.y * 0.15, currentVel.z * 0.1);
			entity.hasImpulse = true;
			handleBurstLaser(serverLevel, entity, combatTarget, burstTicks);
			entity.getPersistentData().putInt(K_CHARGE, 0);
			entity.getPersistentData().putInt(K_FIRE, 0);
			stopFractusLaserSound(serverLevel, entity);
			return;
		}

		if (burstDeactPlay > 0) {
			// Measured deceleration and hover while burst deactivation animation plays to completion
			Vec3 currentVel = entity.getDeltaMovement();
			entity.setDeltaMovement(currentVel.x * 0.70, currentVel.y * 0.85, currentVel.z * 0.70);
			entity.hasImpulse = true;
			stopFractusLaserSound(serverLevel, entity);
			faceMovement(entity);
			return;
		}

		if (laserDeactPlay > 0) {
			// Measured deceleration and hover while regular laser deactivation animation plays to completion
			Vec3 currentVel = entity.getDeltaMovement();
			entity.setDeltaMovement(currentVel.x * 0.80, currentVel.y * 0.90, currentVel.z * 0.80);
			entity.hasImpulse = true;
			stopFractusLaserSound(serverLevel, entity);
			faceMovement(entity);
			return;
		}

		if (laserTarget == null) {
			driftIdleScan(entity);
			entity.getPersistentData().putBoolean(K_RETURNING_HOME, false);
			entity.getPersistentData().putInt(K_COVER_WAIT, 0);

			if (handleEscapedDestroyingMode(serverLevel, entity)) {
				double speed = isWorldTakeoverDimension(entity) ? MAX_DRONE_SPEED : IDLE_MAX_SPEED;
				moveToward(entity, idleHomePoint(entity), speed);
				return;
			}

			stopFractusLaserSound(serverLevel, entity);
			if (previousLaserState == 2 || previousLaserState == 3 || previousLaserState == 1) {
				setLaserState(entity, 4);
				entity.getPersistentData().putInt(K_LASER_DEACT_PLAY, LASER_DEACT_DURATION_TICKS);
				entity.getPersistentData().putInt(K_COOLDOWN, Math.max(cooldown, LASER_DEACT_DURATION_TICKS + 10));
			} else {
				setLaserState(entity, 0);
			}
			resetLaser(entity);
			Entity verdant = findNearestVerdantEngine(serverLevel, entity, FRACTUS_VERDANT_DETECTION_RANGE);
			if (verdant != null && verdant.isAlive()) {
				int deathTimer = verdant.getPersistentData().getInt("verdant_death_timer");
				boolean isCollapsing = deathTimer > 0 || (verdant instanceof LivingEntity le && le.getHealth() < 20.0f);
				if (isCollapsing) {
					Vec3 escape = entity.position().subtract(verdant.position()).normalize().scale(64.0);
					moveToward(entity, entity.position().add(escape), MAX_DRONE_SPEED);
				} else {
					Vec3 orbitalPos = getVerdantOrbitalRingPosition(serverLevel, entity, verdant, isFractusPrime(entity));
					if (orbitalPos != null) {
						moveToward(entity, orbitalPos, IDLE_MAX_SPEED);
					} else {
						double speed = isWorldTakeoverDimension(entity) ? MAX_DRONE_SPEED : IDLE_MAX_SPEED;
						moveToward(entity, idleHomePoint(entity), speed);
					}
				}
				faceMovement(entity);
				return;
			}
			double speed = isWorldTakeoverDimension(entity) ? MAX_DRONE_SPEED : IDLE_MAX_SPEED;
			moveToward(entity, idleHomePoint(entity), speed);
			faceMovement(entity);
			return;
		}

		if (shouldReturnHome(entity)) {
			stopFractusLaserSound(serverLevel, entity);
			resetLaser(entity);
			entity.getPersistentData().putInt(K_COVER_WAIT, 0);

			if (angry && !persistentBoolean(entity, K_RETURNING_HOME, false)) {
				playFractusAngerSound(serverLevel, entity);
			}

			entity.getPersistentData().putBoolean(K_RETURNING_HOME, true);
			setLaserState(entity, 0);
			spawnLeashBreakTrail(serverLevel, entity);
			moveToward(entity, idleHomePoint(entity), IDLE_MAX_SPEED);
			faceMovement(entity);
			return;
		}

		entity.getPersistentData().putBoolean(K_RETURNING_HOME, false);
		cancelDestroyingMode(serverLevel, entity);

		if (previousLaserState == 0) {
			playTargetAcquiredSound(serverLevel, entity);
		}

		faceTarget(entity, laserTarget);

		if (cooldown > 0 || vulnerabilityTicks > 0) {
			if (laserDeactPlay <= 0) {
				setLaserState(entity, 0);
			}
			entity.getPersistentData().putInt(K_CHARGE, 0);
			entity.getPersistentData().putInt(K_FIRE, 0);
			moveCombat(entity, target, false, angry, Math.max(cooldown, vulnerabilityTicks), angerRetreatTicks);
			return;
		}

		if ((!canSeeTarget && !canDrill) || distance > currentLaserRange) {
			if (fireTicks > 0) {
				stopFractusLaserSound(serverLevel, entity);
			}

			int coverWaitTicks = !canSeeTarget ? persistentInt(entity, K_COVER_WAIT, 0) + 1 : 0;
			entity.getPersistentData().putInt(K_COVER_WAIT, coverWaitTicks);
			setLaserState(entity, previousLaserState > 0 ? 4 : 0);
			entity.getPersistentData().putInt(K_CHARGE, Math.max(0, chargeTicks - 2));
			entity.getPersistentData().putInt(K_FIRE, 0);

			int maxCoverWait = angry ? 300 : 180;
			if (coverWaitTicks >= maxCoverWait) {
				if (entity instanceof Mob mob) {
					mob.setTarget(null);
				}
				entity.getPersistentData().putInt(K_COVER_WAIT, 0);
				stopFractusLaserSound(serverLevel, entity);
				resetLaser(entity);
				return;
			}

			if (coverWaitTicks >= COVER_FLANK_TICKS && coverWaitTicks % 40 == 0) {
				int currentSide = persistentInt(entity, K_ORBIT_SIDE, 1);
				entity.getPersistentData().putInt(K_ORBIT_SIDE, currentSide * -1);
			}

			if (coverWaitTicks >= COVER_FLANK_TICKS) {
				Entity verdant = findNearestVerdantEngine(serverLevel, entity, FRACTUS_VERDANT_DETECTION_RANGE);
				if (verdant != null && verdant.isAlive()) {
					moveCombat(entity, target, false, angry, cooldown, angerRetreatTicks);
				} else {
					if (entity.getId() % 2 == 0) {
						moveAscendForSight(entity, target, angry);
					} else {
						moveFlankForSight(entity, target, angry);
					}
				}
			} else {
				moveCombat(entity, target, false, angry, cooldown, angerRetreatTicks);
			}

			return;
		}

		entity.getPersistentData().putInt(K_COVER_WAIT, 0);

		if (fireTicks > 0) {
			setLaserState(entity, 3);
			entity.getPersistentData().putInt(K_FIRE, fireTicks - 1);
			moveCombat(entity, target, true, angry, cooldown, angerRetreatTicks);

			if (fireTicks == FIRE_TICKS || (shouldUseVanillaSounds(entity) && fireTicks % 25 == 0)) {
				playFractusLaserSound(serverLevel, entity);
				if (fireTicks == FIRE_TICKS) {
					spawnFireStartWarning(serverLevel, entity, angry);
				}
			}

			Vec3 laserDirection = updateLaserAim(entity, laserTarget, angry, true);
			LaserHit firstHit = raycastLaser(serverLevel, entity, laserDirection, currentLaserRange);
			LaserHit finalHit = firstHit;

			if (firstHit.entity() == null) {
				destroyWeakBlocksInLaserPath(serverLevel, entity, laserStart(entity), firstHit.location(), firstHit.blockPos(), angry);
				finalHit = raycastLaser(serverLevel, entity, laserDirection, currentLaserRange);
			}

			Vec3 truncatedEnd = interceptProjectilesAlongLaser(serverLevel, entity, laserStart(entity), finalHit.location());
			if (laserTarget != null && !(laserTarget instanceof LivingEntity)) {
				Vec3 pPos = laserTarget.position().add(0, laserTarget.getBbHeight() * 0.5, 0);
				if (pPos.distanceToSqr(laserStart(entity)) < truncatedEnd.distanceToSqr(laserStart(entity))) {
					truncatedEnd = pPos;
				}
			}

			if (truncatedEnd.distanceToSqr(laserStart(entity)) < finalHit.location().distanceToSqr(laserStart(entity))) {
				finalHit = new LaserHit(truncatedEnd, null, finalHit.blockPos(), finalHit.blockFace());
			}

			spawnLaser(
				serverLevel,
				laserStart(entity),
				finalHit.location(),
				angry ? ANGRY_LASER_PARTICLE_SPACING : LASER_PARTICLE_SPACING,
				angry ? ANGRY_LASER_PARTICLE_JITTER : LASER_PARTICLE_JITTER,
				true,
				angry
			);

			if (finalHit.entity() instanceof Player) {
				spawnPlayerLaserImpact(serverLevel, finalHit.location(), angry);
			}

			igniteLaserHitBlock(serverLevel, entity, finalHit.blockPos(), finalHit.blockFace());
			igniteLaserTrailBlocks(serverLevel, entity, laserStart(entity), finalHit.location(), angry);
			igniteLaserHitEntity(finalHit.entity());
			int damageInterval = angry ? ANGRY_DAMAGE_INTERVAL_TICKS : DAMAGE_INTERVAL_TICKS;
			float damage = angry ? ANGRY_LASER_DAMAGE : LASER_DAMAGE;

			if (finalHit.entity() instanceof LivingEntity hitLiving && canDamage(entity, hitLiving) && entity.tickCount % damageInterval == 0) {
				if (entity instanceof LivingEntity attacker) {
					hitLiving.hurt(attacker.damageSources().mobAttack(attacker), damage);
				} else {
					hitLiving.hurt(new DamageSource(world.holderOrThrow(DamageTypes.MOB_ATTACK)), damage);
				}
			}

			if (fireTicks - 1 <= 0) {
				entity.getPersistentData().putInt(K_COOLDOWN, Math.max(COOLDOWN_TICKS, LASER_DEACT_DURATION_TICKS + 5));
				entity.getPersistentData().putInt(K_BURST_COOLDOWN, Math.max(persistentInt(entity, K_BURST_COOLDOWN, 0), 20));
				entity.getPersistentData().putInt(K_CHARGE, 0);
				entity.getPersistentData().putInt(K_VULNERABLE, VULNERABILITY_TICKS);
				stopFractusLaserSound(serverLevel, entity);
				setLaserState(entity, 4);
				entity.getPersistentData().putInt(K_LASER_DEACT_PLAY, LASER_DEACT_DURATION_TICKS);
				shuffleCooldownOrbit(entity);
			}

			return;
		}

		if (suppressionTicks <= 0 && isNearbyFractusFiring(serverLevel, entity)) {
			suppressionTicks = SUPPRESSION_TICKS;
			entity.getPersistentData().putInt(K_SUPPRESSION, suppressionTicks);
		}

		if (suppressionTicks > 0) {
			setLaserState(entity, 4);
			entity.getPersistentData().putInt(K_CHARGE, Math.max(0, chargeTicks - 1));
			moveCombat(entity, target, false, angry, cooldown, angerRetreatTicks);
			spawnSuppressionPulse(serverLevel, entity, suppressionTicks);
			return;
		}

		chargeTicks++;
		entity.getPersistentData().putInt(K_CHARGE, chargeTicks);
		setLaserState(entity, 2);
		moveCombat(entity, target, true, angry, cooldown, angerRetreatTicks);

		Vec3 laserDirection = updateLaserAim(entity, laserTarget, angry, false);
		LaserHit previewHit = raycastLaser(serverLevel, entity, laserDirection, currentLaserRange);
		double chargeProgress = Mth.clamp((double) chargeTicks / CHARGE_TICKS, 0.0, 1.0);
		double previewSpacing = angry
			? Mth.lerp(chargeProgress, ANGRY_CHARGE_PARTICLE_SPACING, 0.16)
			: Mth.lerp(chargeProgress, CHARGE_PARTICLE_SPACING, 0.22);
		double previewJitter = CHARGE_PARTICLE_JITTER + chargeProgress * 0.018;
		spawnLaser(
			serverLevel,
			laserStart(entity),
			previewHit.location(),
			previewSpacing,
			previewJitter,
			false,
			angry
		);
		spawnChargeBeamHum(serverLevel, laserStart(entity), previewHit.location(), chargeProgress, angry);
		spawnChargeParticles(serverLevel, entity, chargeTicks, angry);

		if (chargeTicks == CHARGE_TICKS - 1) {
			spawnPreFireWarning(serverLevel, entity, angry);
		}

		if (chargeTicks >= CHARGE_TICKS) {
			entity.getPersistentData().putInt(K_FIRE, FIRE_TICKS);
			entity.getPersistentData().putInt(K_CHARGE, 0);
		}
	}

	private static void updateLightSource(ServerLevel level, Entity entity) {
		entity.setGlowingTag(false);
		cleanupLightSource(level, entity);
	}

	private static void cleanupLightSource(ServerLevel level, Entity entity) {
		boolean hasLast = persistentBoolean(entity, "has_core_light", false);
		if (hasLast) {
			int lastX = persistentInt(entity, "core_light_x", 0);
			int lastY = persistentInt(entity, "core_light_y", 0);
			int lastZ = persistentInt(entity, "core_light_z", 0);
			BlockPos lastPos = new BlockPos(lastX, lastY, lastZ);
			cleanupLightAt(level, lastPos);
			entity.getPersistentData().putBoolean("has_core_light", false);
		}
	}

	private static void cleanupLightAt(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.is(Blocks.LIGHT)) {
			boolean waterlogged = state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
			if (waterlogged) {
				level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
			} else {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			}
		}
	}

	private static boolean isFractus(Entity entity) {
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		return FRACTUS_ID.equals(id) || "fractus".equals(id.getPath());
	}

	private static boolean isFractusKind(Entity entity) {
		if (entity == null) {
			return false;
		}
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		return "the_backwoods:fractus".equals(id.toString())
			|| "the_backwoods:fractus_prime".equals(id.toString())
			|| "fractus".equals(id.getPath())
			|| "fractus_prime".equals(id.getPath());
	}

	private static boolean isAngry(Entity entity) {
		return (entity instanceof LivingEntity living && living.getHealth() <= ANGER_HEALTH_THRESHOLD)
			|| persistentBoolean(entity, "is_angered_burst", false);
	}

	private static boolean isEscapedContainmentDimension(Entity entity) {
		return !SUB_STRATA_DIMENSION.equals(entity.level().dimension());
	}

	private static boolean handleActivationWarmup(ServerLevel level, Entity entity) {
		if (!persistentBoolean(entity, "fractus_activation_initialized", false)) {
			entity.getPersistentData().putBoolean("fractus_activation_initialized", true);
			if (!entity.getPersistentData().contains("fractus_activation_ticks")) {
				entity.getPersistentData().putInt("fractus_activation_ticks", 16 + entity.getRandom().nextInt(13));
			}
		}

		int activationTicks = persistentInt(entity, "fractus_activation_ticks", 0);
		if (activationTicks <= 0) {
			return false;
		}

		LivingEntity retaliation = retaliationTarget(entity);
		if (retaliation != null) {
			entity.getPersistentData().putInt("fractus_activation_ticks", 0);
			return false;
		}

		entity.getPersistentData().putInt("fractus_activation_ticks", activationTicks - 1);

		Optional<Vec3> hazardEscape = adjustDesiredForHazards(level, entity, entity.position());
		if (hazardEscape.isPresent()) {
			moveToward(entity, hazardEscape.get(), 0.15);
			return true;
		}

		BlockPos currentPos = entity.blockPosition();
		double targetHover = currentHoverHeight(entity, false);
		double floorY = entity.getY();
		for (int dy = 0; dy <= 6; dy++) {
			BlockPos check = currentPos.below(dy);
			if (!level.hasChunkAt(check)) break;
			BlockState state = level.getBlockState(check);
			if (!state.isAir() && (state.isCollisionShapeFullBlock(level, check) || !state.getCollisionShape(level, check).isEmpty())) {
				floorY = check.getY() + 1.0;
				break;
			}
		}

		double currentElev = entity.getY() - floorY;
		Vec3 vel = entity.getDeltaMovement();
		if (currentElev < targetHover - 0.25 || entity.onGround()) {
			entity.setDeltaMovement(vel.x * 0.5, Math.min(0.16, Math.max(0.08, (targetHover - currentElev) * 0.1)), vel.z * 0.5);
			entity.hasImpulse = true;
		} else {
			entity.setDeltaMovement(vel.x * 0.6, vel.y * 0.6, vel.z * 0.6);
		}

		float newYaw = entity.getYRot() + 2.0f;
		entity.setYRot(newYaw);
		entity.setYHeadRot(newYaw);
		if (entity instanceof LivingEntity living) {
			living.yBodyRot = newYaw;
			living.yHeadRot = newYaw;
		}

		if (activationTicks % 8 == 0) {
			level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
		}
		if (activationTicks == 40 || activationTicks == 1) {
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.0f, 1.6f);
		}

		return true;
	}

	private static boolean isSquadBurstActiveNearby(ServerLevel level, Entity entity, double radius) {
		AABB box = entity.getBoundingBox().inflate(radius);
		for (Entity other : level.getEntities(entity, box, FractusOnEntityTickUpdateProcedure::isFractusKind)) {
			if (other != entity && other.isAlive()) {
				int otherBurst = persistentInt(other, K_BURST_TIMER, 0);
				if (otherBurst > 0) {
					return true;
				}
				if (persistentBoolean(other, "is_burst_charging", false) || persistentBoolean(other, "is_laser_burst_activating", false)) {
					return true;
				}
			}
		}
		return false;
	}

	private static double evaluateBurstAttackValue(ServerLevel level, Entity entity, LivingEntity target, int burstTicks, int burstCooldown) {
		if (target == null || burstTicks > 0 || burstCooldown > 0) {
			return 0.0;
		}

		if (persistentInt(entity, K_FIRE, 0) > 0 || persistentInt(entity, K_CHARGE, 0) > 0) {
			return 0.0;
		}

		if (persistentInt(entity, "fractus_burst_fail_penalty", 0) > 0) {
			return 0.0;
		}

		if (isSquadBurstActiveNearby(level, entity, 32.0)) {
			return 0.0;
		}

		double dist = entity.distanceTo(target);
		if (dist < 10.0 || dist > BURST_LASER_RANGE) {
			return 0.0;
		}

		if (!hasClearShot(level, entity, target, BURST_LASER_RANGE)) {
			return 0.0;
		}

		double value = 30.0;

		if (entity instanceof LivingEntity living && living.getHealth() <= 12.0f) {
			value += 50.0;
		}

		if (isStrongTarget(target)) {
			value += 35.0;
		}

		if (target instanceof Player player) {
			value += 30.0;
			if (player.getArmorValue() >= 12) {
				value += 15.0;
			}
		}

		if (dist >= 14.0 && dist <= 38.0) {
			value += 20.0;
		}

		Vec3 targetVel = target.getDeltaMovement();
		double speedSq = targetVel.lengthSqr();
		if (speedSq < 0.005) {
			value += 25.0;
		} else {
			Vec3 futurePos = target.position().add(targetVel.scale(15.0)).add(0, target.getBbHeight() * 0.5, 0);
			Vec3 start = laserStart(entity);
			BlockHitResult blockHit = level.clip(new ClipContext(start, futurePos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
			if (blockHit.getType() != HitResult.Type.MISS) {
				value -= 40.0;
			}
		}

		return Math.max(0.0, value);
	}

	private static double evaluateRegularLaserValue(ServerLevel level, Entity entity, Entity target, double laserRange, boolean canSeeTarget, boolean canDrill) {
		if (target == null) {
			return 0.0;
		}
		if (!(target instanceof LivingEntity livingTarget)) {
			return 100.0;
		}

		double dist = entity.distanceTo(target);
		if (dist > laserRange) {
			return 0.0;
		}

		if (canSeeTarget) {
			double value = 55.0;
			if (dist >= 10.0 && dist <= 24.0) {
				value += 15.0;
			}
			return value;
		} else if (canDrill) {
			if (persistentInt(entity, "fractus_drill_fail_penalty", 0) > 0) {
				return 0.0;
			}
			double drillCost = calculateLaserDrillCost(level, entity, livingTarget, laserRange, isAngry(entity));
			if (drillCost < 0.0 || drillCost > 6.0) {
				return 0.0;
			}
			return Math.max(10.0, 50.0 - drillCost * 6.0);
		}

		return 0.0;
	}

	private static double calculateLaserDrillCost(ServerLevel level, Entity self, LivingEntity target, double maxRange, boolean angry) {
		if (target == null) return -1.0;
		Vec3 start = laserStart(self);
		Vec3 targetEyes = target.getEyePosition();
		double distance = start.distanceTo(targetEyes);
		if (distance > maxRange || distance < 0.5) return -1.0;

		Vec3 dir = targetEyes.subtract(start).normalize();
		float maxHardness = angry ? ANGRY_WEAK_BLOCK_MAX_HARDNESS : WEAK_BLOCK_MAX_HARDNESS;
		BlockPos lastPos = null;
		int weakBlockCount = 0;
		double totalHardness = 0.0;

		for (double d = 0.5; d < distance; d += 0.5) {
			Vec3 sample = start.add(dir.scale(d));
			BlockPos pos = BlockPos.containing(sample.x, sample.y, sample.z);
			if (pos.equals(lastPos)) continue;
			lastPos = pos;
			BlockState state = level.getBlockState(pos);
			if (state.isAir()) continue;

			float hardness = state.getDestroySpeed(level, pos);
			if (hardness < 0.0f || hardness > maxHardness || state.hasBlockEntity()) {
				return -1.0;
			}
			weakBlockCount++;
			totalHardness += hardness;
			if (weakBlockCount > 3) return -1.0;
		}

		return totalHardness;
	}

	private static boolean canLaserDrillThrough(ServerLevel level, Entity self, LivingEntity target, double maxRange, boolean angry) {
		return calculateLaserDrillCost(level, self, target, maxRange, angry) >= 0.0;
	}

	private static boolean isStrongTarget(LivingEntity target) {
		return target != null && (target.getMaxHealth() >= STRONG_TARGET_HEALTH_THRESHOLD || target.getHealth() >= STRONG_TARGET_CURRENT_HEALTH_THRESHOLD);
	}

	private static int burstCooldownTicksForTarget(LivingEntity target) {
		return isStrongTarget(target) ? STRONG_TARGET_BURST_COOLDOWN_TICKS : BURST_COOLDOWN_TICKS;
	}

	private static void applyInfiniteRegeneration(LivingEntity entity) {
		entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) Double.POSITIVE_INFINITY, 0, true, false));
	}

	private static void handleAngerSound(ServerLevel level, Entity entity, boolean angry) {
		if (!angry) {
			entity.getPersistentData().putInt(K_ANGER_SOUND_COOLDOWN, 0);
			return;
		}

		int cooldown = tickStoredTimer(entity, K_ANGER_SOUND_COOLDOWN);

		if (cooldown > 0) {
			return;
		}

		playFractusAngerSound(level, entity);
		int interval = shouldUseVanillaSounds(entity) ? 25 : ANGER_SOUND_INTERVAL_TICKS;
		entity.getPersistentData().putInt(K_ANGER_SOUND_COOLDOWN, interval);
	}

	private static int persistentInt(Entity entity, String key, int fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getInt(key) : fallback;
	}

	private static long persistentLong(Entity entity, String key, long fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getLong(key) : fallback;
	}

	private static boolean checkFractusDespawn(ServerLevel level, Entity entity) {
		if (!(entity instanceof Mob mob)) return false;
		if (mob.hasCustomName() || mob.isPersistenceRequired()) return false;

		if (mob.getTarget() != null && mob.getTarget().isAlive()) return false;
		if (retaliationTarget(mob) != null) return false;

		if (isAngry(mob)) return false;
		if (mob.tickCount - mob.getLastHurtByMobTimestamp() < 200) return false;
		if (persistentInt(mob, K_VULNERABLE, 0) > 0) return false;

		if (persistentInt(mob, K_SUMMON_ROLE, 0) > 0 || persistentInt(mob, K_SUMMON_TICKS, 0) > 0) return false;
		if (persistentInt(mob, K_BURST_TIMER, 0) > 0 || persistentInt(mob, K_BURST_DEACT_DELAY, 0) > 0
				|| persistentInt(mob, K_BURST_DEACT_PLAY, 0) > 0 || persistentInt(mob, K_LASER_DEACT_PLAY, 0) > 0) return false;
		if (persistentBoolean(mob, "is_telekinesis_active", false)) return false;
		if (persistentInt(mob, "fractus_sphere_timer", 0) > 0 || persistentInt(mob, "sphere_ticks", 0) > 0) return false;

		if (findNearestVerdantEngine(level, mob, FRACTUS_VERDANT_DETECTION_RANGE) != null) return false;

		Player nearestPlayer = level.getNearestPlayer(mob, -1.0);
		if (nearestPlayer == null) {
			mob.discard();
			return true;
		}

		double distSqr = mob.distanceToSqr(nearestPlayer);
		if (distSqr > 128.0 * 128.0) {
			mob.discard();
			return true;
		}

		if (distSqr > 32.0 * 32.0) {
			int idleTicks = persistentInt(mob, "fractus_idle_despawn_ticks", 0) + 1;
			mob.getPersistentData().putInt("fractus_idle_despawn_ticks", idleTicks);

			if (idleTicks > 1200 || (idleTicks > 600 && mob.getRandom().nextInt(20) == 0)) {
				mob.discard();
				return true;
			}
		} else {
			mob.getPersistentData().putInt("fractus_idle_despawn_ticks", 0);
		}

		return false;
	}

	private static double persistentDouble(Entity entity, String key, double fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getDouble(key) : fallback;
	}

	private static boolean persistentBoolean(Entity entity, String key, boolean fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getBoolean(key) : fallback;
	}

	private static void ensureHome(Entity entity, double x, double y, double z) {
		if (persistentBoolean(entity, K_HOME_SET, false)) {
			return;
		}

		entity.getPersistentData().putBoolean(K_HOME_SET, true);
		entity.getPersistentData().putDouble(K_HOME_X, x);
		entity.getPersistentData().putDouble(K_HOME_Y, y);
		entity.getPersistentData().putDouble(K_HOME_Z, z);
		entity.getPersistentData().putDouble(K_ORBIT_SEED, entity.getRandom().nextDouble() * Math.PI * 2.0);
		entity.getPersistentData().putInt(K_ORBIT_SIDE, entity.getRandom().nextBoolean() ? 1 : -1);
	}

	private static boolean shouldUseVanillaSounds(Entity entity) {
		return entity.getPersistentData().contains("UseVanillaSounds") ? entity.getPersistentData().getBoolean("UseVanillaSounds") : USE_VANILLA_SOUNDS;
	}

	private static void playFractusLaserSound(ServerLevel level, Entity entity) {
		entity.getPersistentData().putBoolean(K_LASER_SOUND_PLAYING, true);
		if (shouldUseVanillaSounds(entity)) {
			level.playSound(
				null,
				entity.getX(),
				entity.getY(),
				entity.getZ(),
				SoundEvents.BEACON_POWER_SELECT,
				SoundSource.HOSTILE,
				12.0f,
				0.75f
			);
			level.playSound(
				null,
				entity.getX(),
				entity.getY(),
				entity.getZ(),
				SoundEvents.BEACON_ACTIVATE,
				SoundSource.HOSTILE,
				10.0f,
				0.65f
			);
			return;
		}
		net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(FRACTUS_LASER_SOUND);
		if (sound != null) {
			level.playSound(
				null,
				BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()),
				sound,
				SoundSource.HOSTILE,
				net.mcreator.thebackwoods.FractusLaserBeam.FRACTUS_LASER_VOLUME,
				0.85f
			);
		}
	}

	private static void playFractusLaserBurstSound(ServerLevel level, Entity entity) {
		net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(FRACTUS_LASER_BURST_SOUND);
		if (sound != null) {
			level.playSound(
				null,
				BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()),
				sound,
				SoundSource.HOSTILE,
				net.mcreator.thebackwoods.FractusLaserBeam.FRACTUS_BURST_VOLUME,
				1.0f
			);
		}
	}

	private static void playFractusAngerSound(ServerLevel level, Entity entity) {
		if (shouldUseVanillaSounds(entity)) {
			level.playSound(
				null,
				entity.getX(),
				entity.getY(),
				entity.getZ(),
				SoundEvents.BEACON_AMBIENT,
				SoundSource.HOSTILE,
				16.0f,
				0.50f
			);
			return;
		}
		net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(FRACTUS_ANGER_SOUND);
		if (sound != null) {
			level.playSound(
				null,
				BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()),
				sound,
				SoundSource.HOSTILE,
				net.mcreator.thebackwoods.FractusLaserBeam.FRACTUS_ANGER_VOLUME,
				1.0f
			);
		}
	}

	private static void playTargetAcquiredSound(ServerLevel level, Entity entity) {
		level.playSound(
			null,
			BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()),
			SoundEvents.WARDEN_TENDRIL_CLICKS,
			SoundSource.HOSTILE,
			0.75f,
			0.55f
		);
	}

	private static void stopFractusLaserSound(ServerLevel level, Entity entity) {
		if (!persistentBoolean(entity, K_LASER_SOUND_PLAYING, false) && persistentInt(entity, K_FIRE, 0) <= 0) {
			return;
		}

		entity.getPersistentData().putBoolean(K_LASER_SOUND_PLAYING, false);
		ClientboundStopSoundPacket stopLaser = new ClientboundStopSoundPacket(FRACTUS_LASER_SOUND, SoundSource.HOSTILE);

		for (ServerPlayer player : level.players()) {
			player.connection.send(stopLaser);
		}
	}

	private static void stopFractusActiveSounds(ServerLevel level, Entity entity) {
		entity.getPersistentData().putBoolean(K_LASER_SOUND_PLAYING, false);
		ClientboundStopSoundPacket stopLaser = new ClientboundStopSoundPacket(FRACTUS_LASER_SOUND, SoundSource.HOSTILE);
		ClientboundStopSoundPacket stopBurst = new ClientboundStopSoundPacket(FRACTUS_LASER_BURST_SOUND, SoundSource.HOSTILE);
		ClientboundStopSoundPacket stopAnger = new ClientboundStopSoundPacket(FRACTUS_ANGER_SOUND, SoundSource.HOSTILE);

		for (ServerPlayer player : level.players()) {
			player.connection.send(stopLaser);
			player.connection.send(stopBurst);
			player.connection.send(stopAnger);
		}
	}

	private static int tickStoredTimer(Entity entity, String key) {
		int value = Math.max(0, persistentInt(entity, key, 0));

		if (value > 0) {
			entity.getPersistentData().putInt(key, value - 1);
		}

		return value;
	}

	private static void resetLaser(Entity entity) {
		if (entity == null) return;
		if (persistentInt(entity, K_BURST_DEACT_PLAY, 0) > 0 || persistentInt(entity, K_LASER_DEACT_PLAY, 0) > 0) {
			return; // Guard active burst and regular laser deactivation closing animations
		}
		entity.getPersistentData().putInt(K_CHARGE, 0);
		entity.getPersistentData().putInt(K_FIRE, 0);
		entity.getPersistentData().putDouble(K_AIM_X, 0.0);
		entity.getPersistentData().putDouble(K_AIM_Y, 0.0);
		entity.getPersistentData().putDouble(K_AIM_Z, 0.0);
		entity.getPersistentData().putDouble(K_BURST_AIM_X, 0.0);
		entity.getPersistentData().putDouble(K_BURST_AIM_Y, 0.0);
		entity.getPersistentData().putDouble(K_BURST_AIM_Z, 0.0);
		entity.getPersistentData().putInt(K_BURST_TIMER, 0);
		entity.getPersistentData().putInt(K_BURST_DEACT_DELAY, 0);
		entity.getPersistentData().putInt(K_BURST_DEACT_PLAY, 0);
		if (persistentInt(entity, K_COOLDOWN, 0) <= 0 && persistentInt(entity, K_LASER_STATE, 0) != 4) {
			setLaserState(entity, 0);
		}
		setBurstLaserState(entity, 0);
	}

	private static Vec3 home(Entity entity) {
		return new Vec3(
			persistentDouble(entity, K_HOME_X, entity.getX()),
			persistentDouble(entity, K_HOME_Y, entity.getY()),
			persistentDouble(entity, K_HOME_Z, entity.getZ())
		);
	}

	private static boolean shouldReturnHome(Entity entity) {
		if (entity.level() instanceof ServerLevel sl && findNearestVerdantEngine(sl, entity, FRACTUS_VERDANT_DETECTION_RANGE) != null) {
			return false;
		}
		// Allow entities to freely roam across dimensions searching for targets rather than leashing to spawn
		return false;
	}

	private static void alertNearbyMobsAndPanicPassives(ServerLevel level, Entity entity) {
		if (!(entity instanceof LivingEntity fractusLiving)) {
			return;
		}

		int updateTimer = tickStoredTimer(entity, K_ESCAPED_THREAT_UPDATE);

		if (updateTimer > 0) {
			return;
		}

		entity.getPersistentData().putInt(K_ESCAPED_THREAT_UPDATE, ESCAPED_THREAT_UPDATE_INTERVAL);
		AABB searchBox = entity.getBoundingBox().inflate(Math.max(ESCAPED_PASSIVE_FLEE_RANGE, ESCAPED_MOB_RETALIATION_RANGE));

		for (Mob mob : level.getEntitiesOfClass(Mob.class, searchBox, mob -> mob.isAlive() && mob != entity && !isFractusKind(mob))) {
			if (isPassiveFleeCandidate(mob)) {
				panicPassiveAwayFromFractus(mob, entity);
				continue;
			}

			if (mob.distanceTo(entity) <= ESCAPED_MOB_RETALIATION_RANGE && canRetaliateAgainst(entity, mob) && canSeeForTargeting(level, mob, fractusLiving)) {
				mob.setTarget(fractusLiving);
				mob.setLastHurtByMob(fractusLiving);
			}
		}
	}

	private static boolean isPassiveFleeCandidate(Mob mob) {
		return mob instanceof AgeableMob && !isSeaAnimal(mob) && !isFractusKind(mob);
	}

	private static void panicPassiveAwayFromFractus(Mob mob, Entity fractus) {
		if (mob.distanceTo(fractus) > ESCAPED_PASSIVE_FLEE_RANGE) {
			return;
		}

		Vec3 away = mob.position().subtract(fractus.position());
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(mob.getRandom().nextDouble() - 0.5, 0.0, mob.getRandom().nextDouble() - 0.5);
		}

		horizontalAway = horizontalAway.normalize();
		Vec3 fleePoint = mob.position().add(horizontalAway.scale(10.0));

		mob.getNavigation().moveTo(fleePoint.x, fleePoint.y, fleePoint.z, PASSIVE_FLEE_SPEED);
		mob.setDeltaMovement(mob.getDeltaMovement().add(horizontalAway.scale(PASSIVE_FLEE_PUSH)));
		mob.hasImpulse = true;
	}

	private static boolean handleEscapedDestroyingMode(ServerLevel level, Entity entity) {
		if (entity.tickCount < 80) {
			return false;
		}

		if (persistentInt(entity, K_SUMMON_ROLE, 0) > 0) {
			return false;
		}

		if (!isWorldTakeoverDimension(entity) || isInWoodPlainsBiome(level, entity) || findNearestVerdantEngine(level, entity, FRACTUS_VERDANT_DETECTION_RANGE) != null) {
			cancelDestroyingMode(level, entity);
			return false;
		}

		int fireTicks = tickStoredTimer(entity, K_DESTROYING_FIRE);

		if (fireTicks <= 0) {
			int cooldown = tickStoredTimer(entity, K_DESTROYING_COOLDOWN);

			if (cooldown > 0) {
				return false;
			}

			Optional<Vec3> targetDir = destroyingDirectionTowardNearbyBlock(level, entity);
			if (targetDir.isEmpty()) {
				cancelDestroyingMode(level, entity);
				return false;
			}

			fireTicks = DESTROYING_FIRE_TICKS;
			entity.getPersistentData().putInt(K_DESTROYING_FIRE, fireTicks);
			entity.getPersistentData().putInt(K_DESTROYING_COOLDOWN, DESTROYING_COOLDOWN_TICKS + entity.getRandom().nextInt(80));
			storeDestroyingAim(entity, targetDir.get());
			playFractusLaserSound(level, entity);
		} else if (shouldUseVanillaSounds(entity) && fireTicks % 25 == 0) {
			playFractusLaserSound(level, entity);
		}

		Vec3 start = laserStart(entity);

		if (fireTicks % DESTROYING_AIM_REFRESH_TICKS == 0) {
			Optional<Vec3> targetDir = destroyingDirectionTowardNearbyBlock(level, entity);
			if (targetDir.isPresent()) {
				storeDestroyingAim(entity, targetDir.get());
			} else {
				cancelDestroyingMode(level, entity);
				return false;
			}
		}

		Vec3 direction = destroyingAim(entity);
		entity.lookAt(EntityAnchorArgument.Anchor.EYES, start.add(direction));
		if (entity instanceof LivingEntity living) {
			living.setYBodyRot(living.getYRot());
		}
		if (entity instanceof Mob mob) {
			Vec3 targetLook = start.add(direction);
			mob.getLookControl().setLookAt(targetLook.x, targetLook.y, targetLook.z, 180.0F, 180.0F);
		}
		BlockHitResult blockHit = clipBlocks(level, entity, start, start.add(direction.scale(DESTROYING_LASER_RANGE)));
		Vec3 end = blockHit.getType() == HitResult.Type.MISS ? start.add(direction.scale(DESTROYING_LASER_RANGE)) : blockHit.getLocation();

		destroyWeakBlocksInLaserPath(level, entity, start, end, blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getBlockPos(), true);
		blockHit = clipBlocks(level, entity, start, start.add(direction.scale(DESTROYING_LASER_RANGE)));
		end = blockHit.getType() == HitResult.Type.MISS ? start.add(direction.scale(DESTROYING_LASER_RANGE)) : blockHit.getLocation();
		spawnLaser(level, start, end, ANGRY_LASER_PARTICLE_SPACING, ANGRY_LASER_PARTICLE_JITTER, true, true);
		igniteLaserHitBlock(level, entity, blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getBlockPos(), blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getDirection());
		setLaserState(entity, 3);
		igniteLaserTrailBlocks(level, entity, start, end, true);
		if (fireTicks <= 1) {
			stopFractusLaserSound(level, entity);
			setLaserState(entity, 4);
		}

		return true;
	}

	private static void cancelDestroyingMode(ServerLevel level, Entity entity) {
		if (persistentInt(entity, K_DESTROYING_FIRE, 0) <= 0) {
			return;
		}

		entity.getPersistentData().putInt(K_DESTROYING_FIRE, 0);
		stopFractusLaserSound(level, entity);
		setLaserState(entity, 4);
	}

	private static boolean isWorldTakeoverDimension(Entity entity) {
		if (entity == null) {
			return false;
		}
		String dim = entity.level().dimension().location().toString();
		if (dim.startsWith("the_backwoods:") && java.util.Set.of("backwoods", "the_grain", "the_sub_strata", "loss", "rotting", "the_still", "the_familiar", "the_petrified_weald").contains(dim.substring(14))) {
			return false;
		}
		return entity.level().dimension() == Level.OVERWORLD || entity.level().dimension() == Level.NETHER;
	}

	private static boolean isInWoodPlainsBiome(ServerLevel level, Entity entity) {
		if (level == null || entity == null) return false;
		return level.getBiome(entity.blockPosition()).unwrapKey().map(key -> key.location().getNamespace().equals("the_backwoods") && key.location().getPath().equals("wood_plains")).orElse(false);
	}

	private static boolean isNoFireDimension(Entity entity) {
		if (entity == null) {
			return false;
		}
		String dim = entity.level().dimension().location().toString();
		return dim.startsWith("the_backwoods:") && java.util.Set.of("backwoods", "the_grain", "the_sub_strata", "loss", "rotting", "the_still", "the_familiar", "the_petrified_weald").contains(dim.substring(14));
	}

	private static Vec3 randomDestroyingDirection(Entity entity) {
		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);
		double angle = seed + entity.getRandom().nextDouble() * Math.PI * 2.0;
		double y = Mth.wrapDegrees(Mth.lerp(entity.getRandom().nextDouble(), DESTROYING_VERTICAL_AIM_MIN, DESTROYING_VERTICAL_AIM_MAX));
		return new Vec3(Math.cos(angle), y, Math.sin(angle)).normalize();
	}

	private static Optional<Vec3> destroyingDirectionTowardNearbyBlock(ServerLevel level, Entity entity) {
		Vec3 start = laserStart(entity);
		BlockPos center = entity.blockPosition();
		int radius = (int) Math.ceil(DESTROYING_TARGET_SCAN_RADIUS);
		BlockPos bestPos = null;
		double bestScore = Double.MAX_VALUE;
		float maxHardness = ANGRY_WEAK_BLOCK_MAX_HARDNESS;

		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			Vec3 blockCenter = pos.getCenter();
			double distanceSqr = blockCenter.distanceToSqr(start);

			if (distanceSqr > DESTROYING_TARGET_SCAN_RADIUS * DESTROYING_TARGET_SCAN_RADIUS) {
				continue;
			}

			BlockState state = level.getBlockState(pos);

			if (!isDestroyingLaserTarget(level, pos, state, maxHardness)) {
				continue;
			}

			double score = distanceSqr + Math.abs(blockCenter.y - start.y) * 2.0;

			if (score < bestScore) {
				bestScore = score;
				bestPos = pos.immutable();
			}
		}

		if (bestPos == null) {
			return Optional.empty();
		}

		Vec3 direction = bestPos.getCenter().subtract(start);
		return direction.lengthSqr() < 0.001 ? Optional.empty() : Optional.of(direction.normalize());
	}

	private static boolean isDestroyingLaserTarget(ServerLevel level, BlockPos pos, BlockState state, float maxHardness) {
		if (state.isAir() || state.hasBlockEntity()) {
			return false;
		}

		if (state.getFluidState().is(FluidTags.WATER)) {
			return true;
		}

		float hardness = state.getDestroySpeed(level, pos);
		return hardness >= 0.0f && hardness <= maxHardness;
	}

	private static void storeDestroyingAim(Entity entity, Vec3 direction) {
		entity.getPersistentData().putDouble(K_DESTROYING_AIM_X, direction.x);
		entity.getPersistentData().putDouble(K_DESTROYING_AIM_Y, direction.y);
		entity.getPersistentData().putDouble(K_DESTROYING_AIM_Z, direction.z);
	}

	private static Vec3 destroyingAim(Entity entity) {
		Vec3 direction = new Vec3(
			persistentDouble(entity, K_DESTROYING_AIM_X, 0.0),
			persistentDouble(entity, K_DESTROYING_AIM_Y, 0.0),
			persistentDouble(entity, K_DESTROYING_AIM_Z, 0.0)
		);

		return direction.lengthSqr() < 0.001 ? randomDestroyingDirection(entity) : direction.normalize();
	}

	private static Vec3 idleHomePoint(Entity entity) {
		Vec3 home = home(entity);
		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);
		int index = Math.floorMod(persistentInt(entity, K_IDLE_PATROL_INDEX, 0), IDLE_PATROL_POINTS);
		int wait = tickStoredTimer(entity, K_IDLE_PATROL_WAIT);
		Vec3 target = idlePatrolPoint(home, seed, index, entity.tickCount, isEscapedContainmentDimension(entity));

		if (wait <= 0 && entity.position().distanceTo(target) <= IDLE_PATROL_REACH_DISTANCE) {
			index = (index + 1) % IDLE_PATROL_POINTS;
			entity.getPersistentData().putInt(K_IDLE_PATROL_INDEX, index);
			entity.getPersistentData().putInt(K_IDLE_PATROL_WAIT, IDLE_PATROL_WAIT_TICKS);
			target = idlePatrolPoint(home, seed, index, entity.tickCount, isEscapedContainmentDimension(entity));

			boolean guardingVerdant = false;
			if (entity.level() instanceof ServerLevel sl) {
				guardingVerdant = findNearestVerdantEngine(sl, entity, FRACTUS_VERDANT_DETECTION_RANGE) != null;
			}
			if (!guardingVerdant) {
				Vec3 look = entity.getLookAngle();
				if (look.lengthSqr() < 0.001) {
					double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
					look = new Vec3(Math.cos(angle), 0, Math.sin(angle));
				}
				double roamDist = 18.0 + entity.getRandom().nextDouble() * 24.0;
				Vec3 newHome = home.add(look.x * roamDist, (entity.getRandom().nextDouble() - 0.5) * 5.0, look.z * roamDist);
				entity.getPersistentData().putDouble(K_HOME_X, newHome.x);
				entity.getPersistentData().putDouble(K_HOME_Y, newHome.y);
				entity.getPersistentData().putDouble(K_HOME_Z, newHome.z);
			}
		}

		return target;
	}

	private static Vec3 idlePatrolPoint(Vec3 home, double seed, int index, int tickCount, boolean escapedContainment) {
		double angle = seed + Math.PI * 2.0 * index / IDLE_PATROL_POINTS;
		double bob = Math.sin(tickCount * BOB_SPEED + seed + index) * BOB_AMOUNT;
		double hoverHeight = IDLE_HOVER_HEIGHT + (escapedContainment ? ESCAPED_CONTAINMENT_HOVER_BONUS : 0.0);

		return home.add(
			Math.cos(angle) * IDLE_PATROL_RADIUS,
			hoverHeight + bob,
			Math.sin(angle) * IDLE_PATROL_RADIUS
		);
	}

	private static boolean isDoomedByVerdantPound(Entity self, LivingEntity target) {
		if (self == null || target == null || !target.isAlive()) return false;
		if (!(self.level() instanceof ServerLevel serverLevel)) return false;

		double targetHP = target.getHealth() + target.getAbsorptionAmount();

		AABB searchBox = self.getBoundingBox().inflate(64.0);
		java.util.List<Entity> nearbyVerdants = serverLevel.getEntities(self, searchBox, e -> {
			if (e == null || !e.isAlive()) return false;
			ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
			return key != null && key.getPath().toLowerCase().contains("verdant");
		});

		if (nearbyVerdants.isEmpty()) return false;

		for (Entity verdant : nearbyVerdants) {
			int cycleTicks = persistentInt(verdant, "verdant_cycle_ticks", 0);
			double shockwaveStage = persistentDouble(verdant, "verdant_shockwave_stage", 0.0);

			if (cycleTicks < 30 && shockwaveStage <= 0.0) continue;

			double coreX = persistentDouble(verdant, "verdant_locked_x", verdant.getX());
			double coreZ = persistentDouble(verdant, "verdant_locked_z", verdant.getZ());
			double groundY = persistentDouble(verdant, "verdant_ground_y", verdant.getY() - 50.0);

			int pounds = persistentInt(verdant, "verdant_pound_count", 0);
			double poundRadius = Math.min(48.0, 16.0 + (pounds * 2.5));

			double dx = target.getX() - coreX;
			double dz = target.getZ() - coreZ;
			double dy = Math.abs(target.getY() - groundY);
			double distSq = dx * dx + dz * dz;

			if (distSq <= poundRadius * poundRadius && dy <= 16.0) {
				float expectedDamage = 195.0F + (pounds * 15.0F);
				if (targetHP <= expectedDamage) {
					return true;
				}
			}
		}

		return false;
	}

	private static LivingEntity findTarget(ServerLevel level, Entity self, double x, double y, double z) {
		if (self instanceof Mob mob && mob.getTarget() != null) {
			LivingEntity currentTarget = mob.getTarget();
			if (!currentTarget.isAlive() || isNonSurvivalPlayer(currentTarget) || isDoomedByVerdantPound(self, currentTarget)) {
				mob.setTarget(null);
			} else if (shouldIgnoreCombatFilter(self) || shouldIgnoreCombatFilter(currentTarget)) {
				return currentTarget;
			} else if (isTargetOccupied(level, self, currentTarget)) {
				mob.setTarget(null);
			} else {
				return currentTarget;
			}
		}

		LivingEntity retaliationTarget = retaliationTarget(self);

		if (retaliationTarget != null && !isNonSurvivalPlayer(retaliationTarget) && retaliationTarget.distanceTo(self) <= DETECTION_RANGE * 1.6) {
			int coverWait = persistentInt(self, K_COVER_WAIT, 0);
			if (canSeeForTargeting(level, self, retaliationTarget) || coverWait < (isAngry(self) ? 300 : 180)) {
				return retaliationTarget;
			}
		}

		if (self instanceof Mob mob && mob.getTarget() == null && retaliationTarget == null) {
			if ((self.tickCount + self.getId()) % 10 != 0) {
				return null;
			}
		}

		AABB searchBox = new AABB(
			x - DETECTION_RANGE,
			y - DETECTION_RANGE,
			z - DETECTION_RANGE,
			x + DETECTION_RANGE,
			y + DETECTION_RANGE,
			z + DETECTION_RANGE
		);

		List<LivingEntity> visibleCandidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, target -> canTargetNormally(self, target))
			.stream()
			.sorted(Comparator.comparingDouble(target -> target.distanceToSqr(self)))
			.limit(TARGET_LOS_CANDIDATE_LIMIT)
			.filter(target -> canSeeForTargeting(level, self, target))
			.toList();

		if (visibleCandidates.isEmpty()) {
			return null;
		}

		// Prefer unengaged targets so Fractus units distribute across threats rather than dogpiling the same target
		return visibleCandidates.stream()
			.filter(target -> !isTargetOccupied(level, self, target))
			.min(Comparator.comparingDouble(target -> targetPriorityScore(level, self, target)))
			.orElse(null);
	}

	private static boolean isTargetOccupied(ServerLevel level, Entity self, LivingEntity target) {
		if (target == null || !target.isAlive()) {
			return false;
		}
		int targetId = target.getId();
		int maxClaimants = 1;
		int claimants = 0;

		AABB searchBox = self.getBoundingBox().inflate(96.0);
		for (Entity entity : level.getEntities(self, searchBox, e -> isFractusKind(e))) {
			if (entity != self && entity.isAlive()) {
				boolean hasTarget = false;
				if (entity instanceof Mob otherMob && otherMob.getTarget() == target) {
					hasTarget = true;
				} else if (entity.getPersistentData().getInt("fractus_target_id") == targetId) {
					hasTarget = true;
				}
				if (hasTarget) {
					claimants++;
					if (claimants >= maxClaimants) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static double targetPriorityScore(ServerLevel level, Entity self, LivingEntity target) {
		double score = target.distanceToSqr(self);

		if (target instanceof Player) {
			score -= DETECTION_RANGE * DETECTION_RANGE * 2.0;
		}

		String mainHandItem = BuiltInRegistries.ITEM.getKey(target.getMainHandItem().getItem()).getPath();
		if (mainHandItem.contains("bow") || mainHandItem.contains("crossbow") || mainHandItem.contains("trident") || mainHandItem.contains("mace")) {
			score -= 2500.0;
		}

		if (target.getArmorValue() > 10) {
			score -= 500.0;
		}

		if (isTargetOccupied(level, self, target)) {
			score += 1000000.0;
		}

		return score;
	}

	private static boolean canDamage(Entity self, LivingEntity target) {
		return canTargetNormally(self, target) || isRetaliationTarget(self, target);
	}

	private static boolean canBurstDamage(Entity self, LivingEntity target) {
		return canTargetNormally(self, target) || canRetaliateAgainst(self, target);
	}

	private static boolean isNonSurvivalPlayer(Entity entity) {
		if (entity instanceof Player player) {
			return player.isCreative() || player.isSpectator();
		}
		return false;
	}

	private static boolean canTargetNormally(Entity self, LivingEntity target) {
		if (target == null || target == self || !target.isAlive() || isNonSurvivalPlayer(target)) {
			return false;
		}

		if (isWoodboundEntity(target)) {
			return false;
		}

		if (target.hasEffect(MobEffects.INVISIBILITY)) {
			boolean isSelfRetaliating = retaliationTarget(self) == target;
			if (!isSelfRetaliating) {
				return false;
			}
		}

		boolean bypassFactionFilter = shouldIgnoreCombatFilter(self) || shouldIgnoreCombatFilter(target)
			|| (self instanceof Mob m && m.getTarget() == target);

		if (bypassFactionFilter) {
			return true;
		}

		if (isFractusKind(target)) {
			return false;
		}

		if (isWoodboundEntity(target)) {
			return false;
		}

		if (target instanceof AgeableMob ageableMob && ageableMob.isBaby()) {
			return false;
		}

		if (isSeaAnimal(target)) {
			return false;
		}

		if (isPassiveOrAmbientMob(target)) {
			return false;
		}

		if (isDoomedByVerdantPound(self, target)) {
			return false;
		}

		if (isEscapedContainmentDimension(self) || isWorldTakeoverDimension(self)) {
			return true;
		}

		if (false) {
			return (self.getId() + self.tickCount / 120) % 3 == 0;
		}

		return false;
	}

	private static boolean isWoodboundEntity(LivingEntity target) {
		if (target == null) return false;
		ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
		if (key != null) {
			String path = key.getPath().toLowerCase();
			String full = key.toString().toLowerCase();
			if (path.contains("rot") || full.contains("rot") || path.contains("verdant") || full.contains("verdant")) return true;
		}
		String className = target.getClass().getSimpleName().toLowerCase();
		if (className.contains("rot") || className.contains("verdant")) return true;
		return target.getType().is(WOODBOUND_ENTITIES_TAG);
	}

	private static boolean isSeaAnimal(LivingEntity target) {
		if (target instanceof WaterAnimal) {
			return true;
		}

		String idPath = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
		return "turtle".equals(idPath) || "axolotl".equals(idPath);
	}

	private static boolean canRetaliateAgainst(Entity self, LivingEntity target) {
		if (target == null || target == self || !target.isAlive() || isNonSurvivalPlayer(target)) {
			return false;
		}
		if (isWoodboundEntity(target)) {
			return false;
		}
		boolean bypassFactionFilter = shouldIgnoreCombatFilter(self) || shouldIgnoreCombatFilter(target)
			|| (self instanceof Mob m && m.getTarget() == target);
		if (bypassFactionFilter) {
			return true;
		}
		return !isFractusKind(target)
			&& !isWoodboundEntity(target)
			&& !(target instanceof AgeableMob ageableMob && ageableMob.isBaby())
			&& !isSeaAnimal(target);
	}

	private static boolean shouldIgnoreCombatFilter(Entity entity) {
		if (entity == null) return false;
		if (entity.getTags().contains("mob_battle")
			|| entity.getTags().contains("mobbattle")
			|| entity.getTags().contains("test")
			|| entity.getTags().contains("ignore_targets")
			|| entity.getTeam() != null
			|| entity.getPersistentData().getBoolean("mob_battle_mode")
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

	private static boolean isRetaliationTarget(Entity self, LivingEntity target) {
		return retaliationTarget(self) == target && canRetaliateAgainst(self, target);
	}

	private static LivingEntity retaliationTarget(Entity self) {
		if (self instanceof Mob mob) {
			LivingEntity target = mob.getTarget();

			if (canRetaliateAgainst(self, target)) {
				return target;
			}

			LivingEntity lastHurtBy = mob.getLastHurtByMob();

			if (canRetaliateAgainst(self, lastHurtBy)) {
				return lastHurtBy;
			}
		}

		return null;
	}

	private static boolean isProjectileDamage(DamageSource source) {
		Entity direct = source.getDirectEntity();
		Entity shooter = source.getEntity();

		if (source.typeHolder() != null && source.typeHolder().unwrapKey().isPresent()) {
			String typeId = source.typeHolder().unwrapKey().get().location().toString().toLowerCase();
			if (typeId.matches(".*(bullet|shot|projectile|gun|pointblank|point_blank|tacz|firearm).*")) {
				return true;
			}
		}

		String msgId = source.getMsgId();
		if (msgId != null) {
			String lowerMsg = msgId.toLowerCase();
			if (lowerMsg.matches(".*(bullet|shot|projectile|gun|pointblank|point_blank|tacz|firearm).*")) {
				return true;
			}
		}

		if (source.is(DamageTypeTags.IS_PROJECTILE)) {
			return true;
		}

		if (direct == null) {
			return false;
		}

		if (direct instanceof Projectile) {
			return true;
		}

		if (!(direct instanceof LivingEntity)) {
			String className = direct.getClass().getSimpleName().toLowerCase();
			if (className.contains("bullet")
				|| className.contains("projectile")
				|| className.contains("shot")
				|| className.contains("pellet")
				|| className.contains("ammo")
				|| className.contains("laser")
				|| className.contains("round")
				|| className.contains("slug")
				|| className.contains("shell")
				|| className.contains("pointblank")
				|| className.contains("tacz")
				|| className.contains("gun")
				|| className.contains("missile")
				|| className.contains("casing")
				|| className.contains("tracer")) {
				return true;
			}
		}

		if (shooter != null && direct != shooter) {
			return true;
		}

		return false;
	}

	private static void markRetaliationTarget(Entity self, DamageSource source) {
		if (persistentInt(self, K_SUMMON_ROLE, 0) > 0) {
			return;
		}

		LivingEntity attacker = sourceAttacker(source);

		if (attacker == null || !canRetaliateAgainst(self, attacker)) {
			return;
		}

		if (self instanceof Mob mob) {
			mob.setTarget(attacker);
		}
	}

	private static LivingEntity sourceAttacker(DamageSource source) {
		if (source == null) {
			return null;
		}

		Entity attacker = source.getEntity();

		if (attacker instanceof LivingEntity livingAttacker) {
			return livingAttacker;
		}

		Entity direct = source.getDirectEntity();

		if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
			return owner;
		}

		if (direct instanceof LivingEntity directLiving) {
			return directLiving;
		}

		return null;
	}

	private static boolean tryDodgeProjectile(Entity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return false;
		}

		double dodgeChance = isAngry(entity) ? ANGRY_PROJECTILE_DODGE_CHANCE : PROJECTILE_DODGE_CHANCE;

		if (isEscapedContainmentDimension(entity)) {
			dodgeChance += ESCAPED_PROJECTILE_DODGE_BONUS;
		}

		if (entity.getRandom().nextDouble() > Mth.clamp(dodgeChance, 0.0, 0.85)) {
			return false;
		}

		Vec3 away = entity.position().subtract(source.getDirectEntity() == null ? source.getSourcePosition() == null ? entity.position() : source.getSourcePosition() : source.getDirectEntity().position());
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
		}

		horizontalAway = horizontalAway.normalize();
		Vec3 side = new Vec3(-horizontalAway.z, 0.0, horizontalAway.x).scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);

		for (int i = 0; i < PROJECTILE_DODGE_ATTEMPTS; i++) {
			double distance = 4.5 + entity.getRandom().nextDouble() * 5.5;
			double lift = 0.6 + entity.getRandom().nextDouble() * (isEscapedContainmentDimension(entity) ? 3.0 : 2.0);
			Vec3 candidate = entity.position()
				.add(horizontalAway.scale(distance))
				.add(side.scale((entity.getRandom().nextDouble() - 0.5) * 7.0))
				.add(0.0, lift, 0.0);

			if (!isOpenForDrone(level, entity, candidate)) {
				continue;
			}

			spawnDodgeParticles(level, entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0));
			entity.teleportTo(candidate.x, candidate.y, candidate.z);
			entity.setDeltaMovement(Vec3.ZERO);
			entity.hasImpulse = true;
			level.playSound(null, BlockPos.containing(candidate.x, candidate.y, candidate.z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.45f, 0.62f);
			spawnDodgeParticles(level, candidate.add(0.0, entity.getBbHeight() * 0.5, 0.0));
			return true;
		}

		return false;
	}

	private static void faceTarget(Entity entity, Entity target) {
		if (entity == null || target == null) return;
		Vec3 targetEyes = (target instanceof LivingEntity living) ? living.getEyePosition() : target.position().add(0, target.getBbHeight() * 0.5, 0);
		entity.lookAt(EntityAnchorArgument.Anchor.EYES, targetEyes);

		if (entity instanceof Mob mob) {
			float yRot = (float) (Mth.atan2(target.getZ() - entity.getZ(), target.getX() - entity.getX()) * (180.0F / Math.PI)) - 90.0F;

			mob.setYRot(yRot);
			mob.setYHeadRot(yRot);
			mob.setYBodyRot(yRot);
			mob.getNavigation().stop();
			if (target instanceof LivingEntity livingTarget) {
				mob.setTarget(livingTarget);
			}
		}
	}

	private static void faceMovement(Entity entity) {
		Vec3 movement = entity.getDeltaMovement();

		if (movement.horizontalDistanceSqr() < 0.0005) {
			return;
		}

		float yRot = (float) (Mth.atan2(movement.z, movement.x) * (180.0F / Math.PI)) - 90.0F;
		entity.setYRot(yRot);

		if (entity instanceof Mob mob) {
			mob.setYHeadRot(yRot);
			mob.setYBodyRot(yRot);
		}
	}

	private static void driftIdleScan(Entity entity) {
		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);
		int side = persistentInt(entity, K_ORBIT_SIDE, 1);
		entity.getPersistentData().putDouble(K_ORBIT_SEED, seed + IDLE_SCAN_SPEED * side);
	}

	private static void shuffleCooldownOrbit(Entity entity) {
		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);
		int side = persistentInt(entity, K_ORBIT_SIDE, 1);
		entity.getPersistentData().putDouble(K_ORBIT_SEED, seed + (Math.PI * 0.65 + entity.getRandom().nextDouble() * Math.PI * 0.45) * side);
	}

	private static double getTargetThreatReach(LivingEntity target) {
		if (target == null) return 3.0;
		double reach = 3.0;
		if (target instanceof Player player) {
			try {
				reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
			} catch (Throwable ignored) {
				reach = player.isCreative() ? 5.0 : 3.0;
			}
			reach += 1.8;
		} else {
			reach = (target.getBbWidth() * 1.5) + 1.8;
		}
		return reach;
	}

	private static void moveCombat(Entity entity, LivingEntity target, boolean attacking, boolean angry, int cooldown, int angerRetreatTicks) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			Entity verdant = findNearestVerdantEngine(serverLevel, entity, FRACTUS_VERDANT_DETECTION_RANGE);
			if (verdant != null && verdant.isAlive()) {
				int deathTimer = verdant.getPersistentData().getInt("verdant_death_timer");
				boolean isCollapsing = deathTimer > 0 || (verdant instanceof LivingEntity le && le.getHealth() < 20.0f);
				if (isCollapsing) {
					Vec3 escape = entity.position().subtract(verdant.position()).normalize().scale(64.0);
					moveToward(entity, entity.position().add(escape), MAX_DRONE_SPEED);
					return;
				}
				Vec3 orbitalPos = getVerdantOrbitalRingPosition(serverLevel, entity, verdant, isFractusPrime(entity));
				if (orbitalPos != null) {
					double maxSpeed = angry ? ANGRY_MAX_DRONE_SPEED : MAX_DRONE_SPEED;
					moveToward(entity, orbitalPos, attacking ? maxSpeed * 0.40 : maxSpeed);
					return;
				}
			}
		}

		if (target != null) {
			double threatReach = getTargetThreatReach(target);
			double currentDist = entity.distanceTo(target);
			if (currentDist < threatReach + 1.2 && !attacking) {
				moveRetreat(entity, target, angry);
				return;
			}
		}

		if (angerRetreatTicks > 0) {
			moveRetreat(entity, target, angry);
			return;
		}

		if (attacking) {
			Vec3 vel = entity.getDeltaMovement();
			entity.setDeltaMovement(vel.x * 0.25, vel.y * 0.35, vel.z * 0.25);
			entity.hasImpulse = true;
			return;
		}

		if (cooldown > COOLDOWN_TICKS - COOLDOWN_REPOSITION_TICKS) {
			moveCooldownReposition(entity, target, angry);
			return;
		}

		moveLikeDrone(entity, target, attacking, angry);
	}

	private static void moveRetreat(Entity entity, LivingEntity target, boolean angry) {
		Vec3 targetCenter = target.position();
		Vec3 away = entity.position().subtract(targetCenter);
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(1.0, 0.0, 0.0);
		}

		horizontalAway = horizontalAway.normalize();

		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);
		double bob = Math.sin(entity.tickCount * BOB_SPEED + seed) * BOB_AMOUNT;
		double threatReach = getTargetThreatReach(target);
		double basePreferredRange = angry ? ANGRY_PREFERRED_COMBAT_RANGE : PREFERRED_COMBAT_RANGE;
		double preferredRange = Math.max(basePreferredRange, threatReach + 4.0);
		Vec3 desired = targetCenter
			.add(horizontalAway.scale(preferredRange + RETREAT_RANGE_BONUS))
			.add(0.0, currentHoverHeight(entity, angry) + 1.0 + bob, 0.0);

		moveToward(entity, desired, RETREAT_BURST_SPEED);
	}

	private static void moveCooldownReposition(Entity entity, LivingEntity target, boolean angry) {
		if (entity.level() instanceof ServerLevel level) {
			Optional<Vec3> coverPoint = findCooldownCoverPoint(level, entity, target, angry);

			if (coverPoint.isPresent()) {
				moveToward(entity, coverPoint.get(), COOLDOWN_REPOSITION_SPEED);
				return;
			}
		}

		Vec3 targetCenter = target.position();
		Vec3 away = entity.position().subtract(targetCenter);
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(1.0, 0.0, 0.0);
		}

		horizontalAway = horizontalAway.normalize();

		Vec3 side = new Vec3(-horizontalAway.z, 0.0, horizontalAway.x)
			.scale(persistentInt(entity, K_ORBIT_SIDE, 1));
		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);
		double bob = Math.sin(entity.tickCount * BOB_SPEED + seed) * BOB_AMOUNT;
		double threatReach = getTargetThreatReach(target);
		double basePreferredRange = angry ? ANGRY_PREFERRED_COMBAT_RANGE : PREFERRED_COMBAT_RANGE;
		double preferredRange = Math.max(basePreferredRange, threatReach + 4.0);
		Vec3 desired = targetCenter
			.add(horizontalAway.scale(preferredRange))
			.add(side.scale(ORBIT_RADIUS * 2.35))
			.add(0.0, currentHoverHeight(entity, angry) + bob, 0.0);

		moveToward(entity, desired, COOLDOWN_REPOSITION_SPEED);
	}

	private static Optional<Vec3> findCooldownCoverPoint(ServerLevel level, Entity entity, LivingEntity target, boolean angry) {
		Vec3 targetEyes = target.getEyePosition();
		Vec3 entityPos = entity.position();
		Vec3 away = entityPos.subtract(target.position());
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(1.0, 0.0, 0.0);
		}

		horizontalAway = horizontalAway.normalize();
		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);
		double bob = Math.sin(entity.tickCount * BOB_SPEED + seed) * BOB_AMOUNT;
		double preferredRange = angry ? ANGRY_PREFERRED_COMBAT_RANGE : PREFERRED_COMBAT_RANGE;
		Vec3 best = null;
		double bestScore = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < COVER_SEARCH_STEPS; i++) {
			double angle = seed + Math.PI * 2.0 * i / COVER_SEARCH_STEPS;
			Vec3 radial = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
			Vec3 candidate = target.position()
				.add(horizontalAway.scale(preferredRange + 2.0))
				.add(radial.scale(COVER_SEARCH_RADIUS))
				.add(0.0, currentHoverHeight(entity, angry) + bob, 0.0);

			if (!isOpenForDrone(level, entity, candidate) || candidate.distanceTo(target.position()) < COVER_MIN_PLAYER_DISTANCE) {
				continue;
			}

			if (!hasCoverFrom(level, targetEyes, candidate.add(0.0, entity.getBbHeight() * 0.35, 0.0), target)) {
				continue;
			}

			double score = candidate.distanceToSqr(target.position()) - candidate.distanceToSqr(entityPos) * 0.35;

			if (score > bestScore) {
				bestScore = score;
				best = candidate;
			}
		}

		return Optional.ofNullable(best);
	}

	private static boolean isOpenForDrone(ServerLevel level, Entity entity, Vec3 center) {
		// Standard Fractus hitbox is exactly 1 block in dimensions (1.0 width x 1.0 height)
		double width = Math.max(1.0, entity.getBbWidth());
		double height = Math.max(1.0, entity.getBbHeight());
		double halfW = width * 0.5;
		AABB box = new AABB(
			center.x - halfW, center.y, center.z - halfW,
			center.x + halfW, center.y + height, center.z + halfW
		).inflate(0.10);
		return level.noCollision(entity, box) && !hasNearbyHarmfulBlock(level, center, HARMFUL_BLOCK_AVOID_RADIUS);
	}

	private static Optional<Vec3> findNearbyExplosiveHazard(ServerLevel level, Entity entity, double radius) {
		AABB scanBox = entity.getBoundingBox().inflate(radius);
		for (Entity nearby : level.getEntities(entity, scanBox)) {
			if (!nearby.isAlive()) continue;
			String typeName = BuiltInRegistries.ENTITY_TYPE.getKey(nearby.getType()).getPath();
			boolean isExplosive = "tnt".equals(typeName) || "end_crystal".equals(typeName)
				|| "wither_skull".equals(typeName) || "large_fireball".equals(typeName) || "dragon_fireball".equals(typeName) || "wind_charge".equals(typeName);
			if (!isExplosive && nearby instanceof net.minecraft.world.entity.monster.Creeper creeper) {
				if (creeper.isIgnited() || creeper.getSwelling(0.0f) > 0.1f) {
					isExplosive = true;
				}
			}
			if (isExplosive) {
				return Optional.of(nearby.position());
			}
		}
		return Optional.empty();
	}

	private static Optional<Vec3> adjustDesiredForHazards(ServerLevel level, Entity entity, Vec3 desired) {
		if (desired == null) return Optional.empty();
		Optional<Vec3> explosive = findNearbyExplosiveHazard(level, entity, 6.5);
		if (explosive.isPresent()) {
			Vec3 hazardPos = explosive.get();
			Vec3 away = entity.position().subtract(hazardPos);
			Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);
			if (horizontalAway.lengthSqr() < 0.001) {
				horizontalAway = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
			}
			horizontalAway = horizontalAway.normalize();
			Vec3 escape = entity.position().add(horizontalAway.scale(7.5)).add(0.0, 2.0, 0.0);
			if (isOpenForDrone(level, entity, escape)) {
				entity.setDeltaMovement(entity.getDeltaMovement().add(horizontalAway.scale(HARMFUL_BLOCK_ESCAPE_SPEED * 0.45)).add(0.0, 0.18, 0.0));
				return Optional.of(escape);
			}
		}

		if (!hasNearbyHarmfulBlock(level, entity.position(), HARMFUL_BLOCK_AVOID_RADIUS) && !hasNearbyHarmfulBlock(level, desired, HARMFUL_BLOCK_AVOID_RADIUS)) {
			return Optional.empty();
		}

		Vec3 hazardCenter = nearestHarmfulBlockCenter(level, entity.position(), HARMFUL_BLOCK_AVOID_RADIUS + 1.5).orElse(desired);
		Vec3 away = entity.position().subtract(hazardCenter);
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
		}

		horizontalAway = horizontalAway.normalize();
		Vec3 escape = entity.position().add(horizontalAway.scale(6.0)).add(0.0, 1.25, 0.0);

		if (isOpenForDrone(level, entity, escape)) {
			entity.setDeltaMovement(entity.getDeltaMovement().add(horizontalAway.scale(HARMFUL_BLOCK_ESCAPE_SPEED * 0.25)).add(0.0, 0.08, 0.0));
			return Optional.of(escape);
		}

		return findOpenMovementPoint(level, entity, entity.position().add(0.0, 3.0, 0.0));
	}

	private static boolean hasNearbyHarmfulBlock(ServerLevel level, Vec3 center, double radius) {
		if (center == null) return false;
		return nearestHarmfulBlockCenter(level, center, radius).isPresent();
	}

	private static Optional<Vec3> nearestHarmfulBlockCenter(ServerLevel level, Vec3 center, double radius) {
		if (center == null) return Optional.empty();
		BlockPos centerPos = BlockPos.containing(center.x, center.y, center.z);
		int blockRadius = (int) Math.ceil(radius);
		Vec3 nearest = null;
		double nearestDistance = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-blockRadius, -blockRadius, -blockRadius), centerPos.offset(blockRadius, blockRadius, blockRadius))) {
			BlockState state = level.getBlockState(pos);

			if (!isHarmfulBlock(state)) {
				continue;
			}

			Vec3 blockCenter = pos.getCenter();
			double distance = blockCenter.distanceToSqr(center);

			if (distance <= radius * radius && distance < nearestDistance) {
				nearestDistance = distance;
				nearest = blockCenter;
			}
		}

		return Optional.ofNullable(nearest);
	}

	private static boolean isHarmfulBlock(BlockState state) {
		if (state.isAir()) return false;
		return state.is(Blocks.TNT)
			|| state.is(Blocks.FIRE)
			|| state.is(Blocks.SOUL_FIRE)
			|| state.is(Blocks.LAVA)
			|| state.is(Blocks.MAGMA_BLOCK)
			|| state.is(Blocks.CAMPFIRE)
			|| state.is(Blocks.SOUL_CAMPFIRE)
			|| state.getFluidState().is(FluidTags.LAVA);
	}

	private static boolean hasCoverFrom(ServerLevel level, Vec3 viewer, Vec3 coveredPoint, Entity target) {
		BlockHitResult blockHit = level.clip(new ClipContext(
			viewer,
			coveredPoint,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			target
		));

		return blockHit.getType() != HitResult.Type.MISS;
	}

	private static void moveAscendForSight(Entity entity, LivingEntity target, boolean angry) {
		Vec3 targetCenter = target.position();
		Vec3 away = entity.position().subtract(targetCenter);
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(1.0, 0.0, 0.0);
		}

		horizontalAway = horizontalAway.normalize();
		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);
		double bob = Math.sin(entity.tickCount * BOB_SPEED + seed) * BOB_AMOUNT;
		double preferredRange = angry ? ANGRY_PREFERRED_COMBAT_RANGE : PREFERRED_COMBAT_RANGE;
		Vec3 desired = targetCenter
			.add(horizontalAway.scale(preferredRange * 0.75))
			.add(0.0, currentHoverHeight(entity, angry) + 4.5 + bob, 0.0);

		moveToward(entity, desired, FLANK_REPOSITION_SPEED * 1.1);
	}

	private static void moveFlankForSight(Entity entity, LivingEntity target, boolean angry) {
		Vec3 targetCenter = target.position();
		Vec3 away = entity.position().subtract(targetCenter);
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(1.0, 0.0, 0.0);
		}

		horizontalAway = horizontalAway.normalize();
		Vec3 side = new Vec3(-horizontalAway.z, 0.0, horizontalAway.x)
			.scale(persistentInt(entity, K_ORBIT_SIDE, 1));
		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);
		double bob = Math.sin(entity.tickCount * BOB_SPEED + seed) * BOB_AMOUNT;
		double preferredRange = angry ? ANGRY_PREFERRED_COMBAT_RANGE : PREFERRED_COMBAT_RANGE;
		Vec3 desired = targetCenter
			.add(horizontalAway.scale(preferredRange + FLANK_RANGE_BONUS))
			.add(side.scale(FLANK_SIDE_DISTANCE))
			.add(0.0, currentHoverHeight(entity, angry) + bob, 0.0);

		moveToward(entity, desired, FLANK_REPOSITION_SPEED);
	}

	private static void moveLikeDrone(Entity entity, LivingEntity target, boolean attacking, boolean angry) {
		Vec3 targetCenter = target.position();
		Vec3 away = entity.position().subtract(targetCenter);
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(1.0, 0.0, 0.0);
		}

		horizontalAway = horizontalAway.normalize();

		Vec3 side = new Vec3(-horizontalAway.z, 0.0, horizontalAway.x)
			.scale(persistentInt(entity, K_ORBIT_SIDE, 1));

		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);
		double orbitSpeed = angry ? ANGRY_ORBIT_SPEED : ORBIT_SPEED;
		double orbitPulse = Math.sin(entity.tickCount * orbitSpeed + seed) * ORBIT_RADIUS;
		double bob = Math.sin(entity.tickCount * BOB_SPEED + seed) * BOB_AMOUNT;
		double currentDistance = entity.distanceTo(target);
		double threatReach = getTargetThreatReach(target);
		double basePreferredRange = angry ? ANGRY_PREFERRED_COMBAT_RANGE : PREFERRED_COMBAT_RANGE;
		double preferredRange = Math.max(basePreferredRange, threatReach + 4.0);
		double effectiveTooClose = Math.max(TOO_CLOSE_RANGE, threatReach + 2.5);
		double desiredRange = currentDistance < effectiveTooClose ? preferredRange + 3.0 : preferredRange;

		Vec3 desired;
		if (entity.level() instanceof ServerLevel serverLevel) {
			AABB searchBox = entity.getBoundingBox().inflate(SUPPRESSION_RANGE);
			java.util.List<Entity> squad = serverLevel.getEntities(entity, searchBox, e -> isFractusKind(e) && e instanceof Mob m && m.getTarget() == target);
			squad.add(entity);
			squad.sort(Comparator.comparingInt(Entity::getId));
			int squadSize = squad.size();
			int myIndex = squad.indexOf(entity);

			if (squadSize > 1 && myIndex >= 0) {
				double baseAngle = (Math.PI * 2.0 / squadSize) * myIndex;
				double dynamicAngle = baseAngle + Math.sin(entity.tickCount * orbitSpeed * 0.5 + seed) * 0.45;
				Vec3 targetLook = target.getLookAngle();
				double lookYaw = Mth.atan2(targetLook.z, targetLook.x);
				double finalAngle = lookYaw + dynamicAngle;
				Vec3 squadOffset = new Vec3(Math.cos(finalAngle), 0.0, Math.sin(finalAngle));
				desired = targetCenter
					.add(squadOffset.scale(desiredRange))
					.add(0.0, currentHoverHeight(entity, angry) + bob, 0.0);
			} else {
				desired = targetCenter
					.add(horizontalAway.scale(desiredRange))
					.add(side.scale(orbitPulse))
					.add(0.0, currentHoverHeight(entity, angry) + bob, 0.0);
			}
		} else {
			desired = targetCenter
				.add(horizontalAway.scale(desiredRange))
				.add(side.scale(orbitPulse))
				.add(0.0, currentHoverHeight(entity, angry) + bob, 0.0);
		}

		double maxSpeed = angry ? ANGRY_MAX_DRONE_SPEED : MAX_DRONE_SPEED;
		moveToward(entity, desired, attacking ? maxSpeed * 0.55 : maxSpeed);
	}

	private static boolean isFractusPrime(Entity entity) {
		if (entity == null) return false;
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		return "the_backwoods:fractus_prime".equals(id.toString()) || "fractus_prime".equals(id.getPath());
	}

	private static Entity findNearestVerdantEngine(ServerLevel level, Entity entity) {
		return findNearestVerdantEngine(level, entity, FRACTUS_VERDANT_DETECTION_RANGE);
	}

	private static Entity findNearestVerdantEngine(ServerLevel level, Entity entity, double radius) {
		if (entity == null || !entity.isAlive()) return null;
		// Standard Fractus must finish its summoning ritual completely before guarding the Verdant Engine
		if (persistentInt(entity, K_SUMMON_ROLE, 0) > 0 || persistentInt(entity, K_SUMMON_TICKS, 0) > 0) {
			return null;
		}

		long currentTick = level.getGameTime();
		int cachedId = persistentInt(entity, "cached_verdant_id", -1);
		long lastSearchTick = persistentLong(entity, "cached_verdant_tick", -1000L);

		if ((currentTick - lastSearchTick) < 15) {
			if (cachedId != -1) {
				Entity cached = level.getEntity(cachedId);
				if (cached != null && cached.isAlive() && entity.distanceToSqr(cached) <= radius * radius) {
					return cached;
				}
			} else {
				return null;
			}
		}

		AABB searchBox = entity.getBoundingBox().inflate(radius);
		List<Entity> list = level.getEntities(entity, searchBox, e -> {
			if (!e.isAlive()) return false;
			String desc = e.getType().getDescriptionId();
			return desc.endsWith(".verdant_engine") || desc.contains("verdant_engine");
		});
		Entity nearest = null;
		double minDistSq = Double.MAX_VALUE;
		for (Entity e : list) {
			double dSq = entity.distanceToSqr(e);
			if (dSq < minDistSq) {
				minDistSq = dSq;
				nearest = e;
			}
		}

		if (nearest != null) {
			entity.getPersistentData().putInt("cached_verdant_id", nearest.getId());
		} else {
			entity.getPersistentData().putInt("cached_verdant_id", -1);
		}
		entity.getPersistentData().putLong("cached_verdant_tick", currentTick);

		return nearest;
	}

	private static boolean hasClearPathToVerdant(ServerLevel level, Entity entity, Vec3 targetPos) {
		Vec3 start = entity.getEyePosition();
		BlockHitResult hit = level.clip(new ClipContext(start, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
		return hit.getType() == HitResult.Type.MISS;
	}

	private static Vec3 getVerdantOrbitalRingPosition(ServerLevel level, Entity entity, Entity verdant, boolean isPrime) {
		if (entity == null || verdant == null || !verdant.isAlive()) return null;
		// If entity is in summoning ritual, finish summoning before guarding Verdant
		if (persistentInt(entity, K_SUMMON_ROLE, 0) > 0 || persistentInt(entity, K_SUMMON_TICKS, 0) > 0) {
			entity.getPersistentData().putInt("fractus_verdant_slot", -1);
			return null;
		}

		AABB verdantBb = verdant.getBoundingBox();
		double verdantHitboxRadius = Math.max(verdantBb.getXsize(), verdantBb.getZsize()) * 0.5;
		double ringRadius = (verdantHitboxRadius + 10.4) * 1.125;

		final int SLOTS_PER_RING = 13;
		final int TOTAL_SLOTS = SLOTS_PER_RING * 3; // 39 slots

		// Collect all active guards around Verdant (extended search so guards temporarily fighting nearby retain their slots)
		List<Entity> guards = level.getEntities(verdant, verdantBb.inflate(FRACTUS_VERDANT_DETECTION_RANGE + 32.0), e -> e.isAlive() && isFractusKind(e) && persistentInt(e, K_SUMMON_ROLE, 0) == 0);
		if (!guards.contains(entity)) {
			guards.add(entity);
		}

		// Check if entity already holds a valid reserved slot
		int currentSlot = persistentInt(entity, "fractus_verdant_slot", -1);
		boolean slotValid = false;
		if (currentSlot >= 0 && currentSlot < TOTAL_SLOTS) {
			boolean conflict = false;
			for (Entity other : guards) {
				if (other != entity && other.getId() < entity.getId() && persistentInt(other, "fractus_verdant_slot", -1) == currentSlot) {
					conflict = true;
					break;
				}
			}
			if (!conflict) {
				slotValid = true;
			}
		}

		int assignedSlot = currentSlot;
		if (!slotValid) {
			// Find the lowest available slot: Middle (0..12) first, Bottom (13..25) second, Top (26..38) third
			boolean[] occupied = new boolean[TOTAL_SLOTS];
			for (Entity other : guards) {
				if (other != entity) {
					int s = persistentInt(other, "fractus_verdant_slot", -1);
					if (s >= 0 && s < TOTAL_SLOTS) {
						occupied[s] = true;
					}
				}
			}
			assignedSlot = -1;
			for (int i = 0; i < TOTAL_SLOTS; i++) {
				if (!occupied[i]) {
					assignedSlot = i;
					break;
				}
			}
			if (assignedSlot < 0) {
				entity.getPersistentData().putInt("fractus_verdant_slot", -1);
				return null;
			}
			entity.getPersistentData().putInt("fractus_verdant_slot", assignedSlot);
		}

		int ring;
		double layerY;
		double ringPhaseOffset;

		if (assignedSlot < SLOTS_PER_RING) {
			// Priority 1: Middle Ring (Slots 0..12) - STRICTLY FILLED FIRST
			ring = 0;
			layerY = verdant.getY() + (verdantBb.getYsize() * 0.50);
			ringPhaseOffset = 0.0;
		} else if (assignedSlot < SLOTS_PER_RING * 2) {
			// Priority 2: Bottom Ring (Slots 13..25) - FILLED SECOND
			ring = 1;
			layerY = verdant.getY() + (verdantBb.getYsize() * 0.25);
			ringPhaseOffset = Math.PI / SLOTS_PER_RING;
		} else {
			// Priority 3: Top Ring (Slots 26..38) - FILLED THIRD
			ring = 2;
			layerY = verdant.getY() + (verdantBb.getYsize() * 0.75);
			ringPhaseOffset = (Math.PI * 2.0) / (SLOTS_PER_RING * 3.0);
		}

		List<Integer> slotsInThisRing = new java.util.ArrayList<>();
		for (Entity g : guards) {
			int s = persistentInt(g, "fractus_verdant_slot", -1);
			if (ring == 0 && s >= 0 && s < SLOTS_PER_RING) {
				if (!slotsInThisRing.contains(s)) slotsInThisRing.add(s);
			} else if (ring == 1 && s >= SLOTS_PER_RING && s < SLOTS_PER_RING * 2) {
				if (!slotsInThisRing.contains(s)) slotsInThisRing.add(s);
			} else if (ring == 2 && s >= SLOTS_PER_RING * 2 && s < TOTAL_SLOTS) {
				if (!slotsInThisRing.contains(s)) slotsInThisRing.add(s);
			}
		}
		slotsInThisRing.sort(Integer::compareTo);

		int rankInRing = slotsInThisRing.indexOf(assignedSlot);
		if (rankInRing < 0) rankInRing = 0;
		int guardsInThisRing = Math.max(1, slotsInThisRing.size());

		// Evenly distributed angle around the 360-degree circle (matching summoning ritual spacing)
		double baseAngle = ((Math.PI * 2.0) / guardsInThisRing) * rankInRing + ringPhaseOffset;
		double spinRate = 0.00125; // 75% slower rotation speed (0.005 * 0.25)
		double currentAngle = (verdant.tickCount * spinRate) + baseAngle;

		double px = verdant.getX() + Math.cos(currentAngle) * ringRadius;
		double pz = verdant.getZ() + Math.sin(currentAngle) * ringRadius;

		double bob = Math.sin((verdant.tickCount + (long) entity.getId() * 7) * 0.05) * 0.20;
		double py = layerY + bob;

		return new Vec3(px, py, pz);
	}

	private static Vec3 adjustDesiredForVerdantBeamAvoidance(ServerLevel level, Entity entity, Vec3 desired) {
		if (desired == null) return entity.position();
		Entity verdant = findNearestVerdantEngine(level, entity, FRACTUS_VERDANT_DETECTION_RANGE);
		if (verdant == null || !verdant.isAlive()) return desired;

		Vec3 entityPos = entity.position();
		double vx = verdant.getX();
		double vz = verdant.getZ();

		AABB verdantBb = verdant.getBoundingBox();
		double verdantHitboxRadius = Math.max(verdantBb.getXsize(), verdantBb.getZsize()) * 0.5;
		double safeRadius = Math.max(verdantHitboxRadius + 4.0, 12.0);

		double dx = entityPos.x - vx;
		double dz = entityPos.z - vz;
		double distToAxis = Math.sqrt(dx * dx + dz * dz);

		// If entity is inside or too close to Verdant hitbox, push directly outward
		if (distToAxis < safeRadius) {
			if (distToAxis < 0.001) {
				dx = 1.0;
				dz = 0.0;
				distToAxis = 1.0;
			}
			double pushX = (dx / distToAxis) * (safeRadius + 2.0);
			double pushZ = (dz / distToAxis) * (safeRadius + 2.0);
			return new Vec3(vx + pushX, desired.y, vz + pushZ);
		}

		// Check if the straight line path to 'desired' passes through or near Verdant's cylinder
		Vec3 seg = desired.subtract(entityPos);
		double segLen = Math.sqrt(seg.x * seg.x + seg.z * seg.z);
		if (segLen < 0.001) return desired;

		double dirX = seg.x / segLen;
		double dirZ = seg.z / segLen;

		// Vector from entity to Verdant center
		double toVerdantX = vx - entityPos.x;
		double toVerdantZ = vz - entityPos.z;
		double proj = toVerdantX * dirX + toVerdantZ * dirZ;

		// If Verdant is along the segment trajectory
		if (proj > 0 && proj < segLen) {
			double closestX = entityPos.x + dirX * proj;
			double closestZ = entityPos.z + dirZ * proj;
			double perpDistSq = (closestX - vx) * (closestX - vx) + (closestZ - vz) * (closestZ - vz);

			if (perpDistSq < safeRadius * safeRadius) {
				// Path would intersect Verdant's body. Route circumferentially around the perimeter!
				double currentAngle = Math.atan2(dz, dx);
				double targetAngle = Math.atan2(desired.z - vz, desired.x - vx);
				double angleDiff = targetAngle - currentAngle;
				while (angleDiff > Math.PI) angleDiff -= (Math.PI * 2.0);
				while (angleDiff < -Math.PI) angleDiff += (Math.PI * 2.0);

				double stepSign = (angleDiff >= 0) ? 1.0 : -1.0;
				// Step along the perimeter orbit by 45 degrees in the shortest direction around Verdant
				double stepAngle = currentAngle + stepSign * (Math.PI / 4.0);
				double orbitRadius = Math.max(safeRadius + 2.0, distToAxis);

				double waypointX = vx + Math.cos(stepAngle) * orbitRadius;
				double waypointZ = vz + Math.sin(stepAngle) * orbitRadius;
				return new Vec3(waypointX, desired.y, waypointZ);
			}
		}

		return desired;
	}

	private static void moveToward(Entity entity, Vec3 desired, double maxSpeed) {
		if (desired == null) return;
		if (entity.level() instanceof ServerLevel level) {
			desired = adjustDesiredForVerdantBeamAvoidance(level, entity, desired);
			desired = adjustDesiredForHazards(level, entity, desired).orElse(desired);
			desired = adjustDesiredForCeilingAndGround(level, entity, desired);
			boolean guardingVerdant = findNearestVerdantEngine(level, entity, FRACTUS_VERDANT_DETECTION_RANGE) != null && persistentInt(entity, "fractus_verdant_slot", -1) >= 0;
			if (!guardingVerdant) {
				desired = adjustDesiredForAllySeparation(level, entity, desired);
				desired = adjustDesiredForOpenSpace(level, entity, desired);
			}
		}

		Vec3 toDesired = desired.subtract(entity.position());
		double distSq = toDesired.lengthSqr();

		boolean isGuardingVerdant = false;
		boolean isRelocating = false;
		if (entity.level() instanceof ServerLevel level) {
			Entity verdant = findNearestVerdantEngine(level, entity, FRACTUS_VERDANT_DETECTION_RANGE);
			if (verdant != null && verdant.isAlive() && persistentInt(entity, "fractus_verdant_slot", -1) >= 0) {
				isGuardingVerdant = true;
				isRelocating = verdant.getPersistentData().getBoolean("verdant_is_relocating");
			}
		}

		int burstTicks = persistentInt(entity, K_BURST_TIMER, 0);
		boolean isBursting = burstTicks > 0;

		double accelRate = isBursting ? DRONE_ACCELERATION * 0.30 : DRONE_ACCELERATION;
		double currentVertLimit = isBursting ? VERTICAL_SPEED_LIMIT * 0.30 : VERTICAL_SPEED_LIMIT;

		if (isGuardingVerdant && (isRelocating || distSq > 6.0 * 6.0)) {
			accelRate = DRONE_ACCELERATION * 1.2;
			if (maxSpeed < 0.35) {
				maxSpeed = 0.35;
			}
			currentVertLimit = Math.max(currentVertLimit, 0.35);
		}

		Vec3 acceleration = toDesired.scale(accelRate);
		
		double drag = isBursting ? 0.78 : 0.82;
		Vec3 next = entity.getDeltaMovement().scale(drag).add(acceleration);

		if (entity.level() instanceof ServerLevel level) {
			next = applyCeilingCushion(level, entity, next);
		}

		double speedLimit = isBursting ? maxSpeed * 0.30 : maxSpeed;
		double horizontalSpeed = Math.sqrt(next.x * next.x + next.z * next.z);

		if (horizontalSpeed > speedLimit) {
			double scale = speedLimit / horizontalSpeed;
			next = new Vec3(next.x * scale, next.y, next.z * scale);
		}

		next = new Vec3(
			next.x,
			Mth.clamp(next.y, -currentVertLimit, currentVertLimit),
			next.z
		);

		entity.setDeltaMovement(next);
		entity.hasImpulse = true;
	}

	private static Vec3 adjustDesiredForCeilingAndGround(ServerLevel level, Entity entity, Vec3 desired) {
		BlockPos destPos = BlockPos.containing(desired.x, desired.y, desired.z);
		double entityHeight = Math.max(1.0, entity.getBbHeight());
		
		double floorY = desired.y - 4.0;
		for (int dy = 0; dy <= 12; dy++) {
			BlockPos check = destPos.below(dy);
			if (!level.hasChunkAt(check)) break;
			BlockState state = level.getBlockState(check);
			if (!state.isAir() && (state.isCollisionShapeFullBlock(level, check) || !state.getCollisionShape(level, check).isEmpty())) {
				floorY = check.getY() + 1.0;
				break;
			}
		}

		double ceilingY = Double.MAX_VALUE;
		for (int dy = 1; dy <= 16; dy++) {
			BlockPos check = BlockPos.containing(desired.x, floorY + dy, desired.z);
			if (!level.hasChunkAt(check)) break;
			BlockState state = level.getBlockState(check);
			if (!state.isAir() && (state.isCollisionShapeFullBlock(level, check) || !state.getCollisionShape(level, check).isEmpty())) {
				ceilingY = check.getY();
				break;
			}
		}

		double clampedY = desired.y;
		if (ceilingY < Double.MAX_VALUE) {
			double headroom = ceilingY - floorY;
			double maxAllowedY = ceilingY - entityHeight - 0.40;
			double minAllowedY = floorY + 0.30;

			if (headroom <= entityHeight + 0.70) {
				clampedY = floorY + Math.max(0.1, (headroom - entityHeight) * 0.5);
			} else {
				clampedY = Math.min(clampedY, maxAllowedY);
				clampedY = Math.max(clampedY, minAllowedY);
			}
		} else {
			clampedY = Math.max(clampedY, floorY + 0.5);
		}

		return new Vec3(desired.x, clampedY, desired.z);
	}

	private static Vec3 adjustDesiredForAllySeparation(ServerLevel level, Entity entity, Vec3 desired) {
		double comfortRadius = 2.2;
		AABB personalSpace = entity.getBoundingBox().inflate(comfortRadius);
		Vec3 separationSteer = Vec3.ZERO;
		int allyCount = 0;

		for (Entity ally : level.getEntities(entity, personalSpace, e -> isFractusKind(e) && e.isAlive())) {
			if (ally == entity) continue;
			Vec3 diff = entity.position().subtract(ally.position());
			double dist = diff.length();

			if (dist > 0.001 && dist < comfortRadius) {
				double factor = (1.0 - (dist / comfortRadius));
				separationSteer = separationSteer.add(diff.normalize().scale(factor * factor * 2.2));
				allyCount++;
			} else if (dist <= 0.001) {
				double fanAngle = ((entity.getId() * 37) % 360) * (Math.PI / 180.0);
				separationSteer = separationSteer.add(Math.cos(fanAngle) * 1.8, 0.15, Math.sin(fanAngle) * 1.8);
				allyCount++;
			}
		}

		if (allyCount > 0) {
			return desired.add(separationSteer);
		}
		return desired;
	}

	private static Vec3 applyCeilingCushion(ServerLevel level, Entity entity, Vec3 vel) {
		double entityTopY = entity.getY() + Math.max(1.0, entity.getBbHeight());
		BlockPos headPos = BlockPos.containing(entity.getX(), entityTopY + 0.35, entity.getZ());
		if (level.hasChunkAt(headPos)) {
			BlockState state = level.getBlockState(headPos);
			if (!state.isAir() && (state.isCollisionShapeFullBlock(level, headPos) || !state.getCollisionShape(level, headPos).isEmpty())) {
				double newY = vel.y > 0 ? -0.04 : vel.y - 0.02;
				return new Vec3(vel.x, Math.max(-0.15, newY), vel.z);
			}
		}
		return vel;
	}

	private static Vec3 adjustDesiredForOpenSpace(ServerLevel level, Entity entity, Vec3 desired) {
		Vec3 position = entity.position();
		double movedSqr = position.distanceToSqr(
			persistentDouble(entity, K_LAST_X, position.x),
			persistentDouble(entity, K_LAST_Y, position.y),
			persistentDouble(entity, K_LAST_Z, position.z)
		);
		boolean tryingToMove = desired.distanceToSqr(position) > 4.0;
		boolean stuck = tryingToMove && movedSqr < 0.0125;
		AABB nextBox = entity.getBoundingBox().move(desired.subtract(position).normalize().scale(0.6));
		boolean blockedAhead = !level.noCollision(entity, nextBox);
		int stuckTicks = stuck || blockedAhead ? persistentInt(entity, K_STUCK_TICKS, 0) + 1 : Math.max(0, persistentInt(entity, K_STUCK_TICKS, 0) - 1);

		entity.getPersistentData().putInt(K_STUCK_TICKS, stuckTicks);
		entity.getPersistentData().putDouble(K_LAST_X, position.x);
		entity.getPersistentData().putDouble(K_LAST_Y, position.y);
		entity.getPersistentData().putDouble(K_LAST_Z, position.z);

		if (!blockedAhead && stuckTicks < STUCK_ESCAPE_TICKS) {
			return desired;
		}

		return findOpenMovementPoint(level, entity, desired).orElse(desired);
	}

	private static Optional<Vec3> findOpenMovementPoint(ServerLevel level, Entity entity, Vec3 desired) {
		Vec3 origin = entity.position();
		Vec3 best = null;
		double bestScore = Double.MAX_VALUE;
		double seed = persistentDouble(entity, K_ORBIT_SEED, 0.0);

		for (int vertical = 0; vertical <= 3; vertical++) {
			double yOffset = vertical == 0 ? 1.4 : (vertical == 1 ? 2.8 : (vertical == 2 ? 0.0 : -1.0));

			for (int i = 0; i < OPEN_SPACE_SEARCH_STEPS; i++) {
				double angle = seed + Math.PI * 2.0 * i / OPEN_SPACE_SEARCH_STEPS;
				double radius = OPEN_SPACE_SEARCH_RADIUS * (0.55 + 0.45 * (i % 3) / 2.0);
				Vec3 candidate = origin.add(Math.cos(angle) * radius, Mth.clamp(desired.y - origin.y + yOffset, -OPEN_SPACE_VERTICAL_RANGE, OPEN_SPACE_VERTICAL_RANGE), Math.sin(angle) * radius);

				if (!isOpenForDrone(level, entity, candidate)) {
					continue;
				}

				BlockHitResult path = clipBlocks(level, entity, origin.add(0.0, entity.getBbHeight() * 0.5, 0.0), candidate.add(0.0, entity.getBbHeight() * 0.5, 0.0));

				if (path.getType() != HitResult.Type.MISS) {
					continue;
				}

				double score = candidate.distanceToSqr(desired) + candidate.distanceToSqr(origin) * 0.25 - yOffset * 1.5;

				if (score < bestScore) {
					bestScore = score;
					best = candidate;
				}
			}
		}

		return Optional.ofNullable(best);
	}

	private static boolean canSeeForTargeting(ServerLevel level, Entity self, LivingEntity target) {
		Vec3 start = laserStart(self);
		Vec3 targetEyes = target.getEyePosition();
		BlockHitResult blockHit = clipBlocks(level, self, start, targetEyes);

		return blockHit.getType() == HitResult.Type.MISS
			|| blockHit.getLocation().distanceToSqr(start) + 0.35 >= targetEyes.distanceToSqr(start);
	}

	private static boolean hasClearShot(ServerLevel level, Entity self, Entity target, double maxRange) {
		if (target == null) return false;
		Vec3 start = laserStart(self);
		Vec3 targetEyes = (target instanceof LivingEntity living) ? living.getEyePosition() : target.position().add(0, target.getBbHeight() * 0.5, 0);

		if (start.distanceTo(targetEyes) > maxRange) {
			return false;
		}

		BlockHitResult blockHit = clipBlocks(level, self, start, targetEyes);

		if (blockHit.getType() != HitResult.Type.MISS && blockHit.getLocation().distanceToSqr(start) + 0.35 < targetEyes.distanceToSqr(start)) {
			return false;
		}

		Vec3 beamDir = targetEyes.subtract(start);
		double beamDist = beamDir.length();
		if (beamDist > 0.5) {
			Vec3 normDir = beamDir.normalize();
			AABB beamBox = new AABB(start, targetEyes).inflate(1.2);
			for (Entity ally : level.getEntities(self, beamBox, e -> isFractusKind(e) && e != self && e.isAlive())) {
				Vec3 toAlly = ally.position().add(0, ally.getBbHeight() * 0.5, 0).subtract(start);
				double proj = toAlly.dot(normDir);
				if (proj > 0.8 && proj < beamDist - 0.8) {
					Vec3 closestPoint = start.add(normDir.scale(proj));
					double distToBeam = ally.position().add(0, ally.getBbHeight() * 0.5, 0).distanceTo(closestPoint);
					if (distToBeam < (ally.getBbWidth() * 0.5 + 0.55)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	private static boolean isInterceptableProjectile(Entity e, Entity self) {
		if (e == null || !e.isAlive() || e == self) return false;

		boolean isProj = e instanceof net.minecraft.world.entity.projectile.Projectile;
		if (!isProj) {
			String className = e.getClass().getName().toLowerCase(java.util.Locale.ROOT);
			String regName = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().toLowerCase(java.util.Locale.ROOT);
			if (className.contains("projectile") || className.contains("bullet") || className.contains("arrow")
					|| className.contains("rocket") || className.contains("trident") || className.contains("fireball")
					|| className.contains("spell") || className.contains("skull") || className.contains("bolt")
					|| className.contains("ammo") || className.contains("round") || className.contains("shell") || className.contains("shot")
					|| regName.contains("projectile") || regName.contains("bullet") || regName.contains("arrow") || regName.contains("shot")) {
				isProj = true;
			}
		}

		if (!isProj) return false;

		if (e instanceof net.minecraft.world.entity.projectile.Projectile proj) {
			Entity owner = proj.getOwner();
			if (owner == self || (owner != null && isFractusKind(owner))) {
				return false;
			}
		}

		return true;
	}

	private static boolean isProjectileHeadingTowards(Entity self, Entity proj, double maxDistance) {
		if (!isInterceptableProjectile(proj, self)) return false;

		double dist = proj.distanceTo(self);
		Entity verdant = self.level() instanceof ServerLevel sl ? findNearestVerdantEngine(sl, self, FRACTUS_VERDANT_DETECTION_RANGE) : null;
		double vDist = verdant != null ? proj.distanceTo(verdant) : Double.MAX_VALUE;

		if (dist > maxDistance && vDist > maxDistance) return false;

		Vec3 vel = proj.getDeltaMovement();
		Vec3 pPos = proj.position();

		if (dist <= 8.0 || vDist <= 8.0) return true;

		double speed = vel.length();
		if (speed < 0.001) {
			return dist <= 8.0 || vDist <= 8.0;
		}

		Vec3 normVel = vel.normalize();
		Vec3 selfEye = self.position().add(0, self.getBbHeight() * 0.5, 0);

		Vec3 toSelf = selfEye.subtract(pPos);
		double dotSelf = toSelf.dot(normVel);
		if (dotSelf > 0 && dotSelf <= maxDistance) {
			Vec3 closestPoint = pPos.add(normVel.scale(dotSelf));
			if (closestPoint.distanceTo(selfEye) <= 6.5) {
				return true;
			}
		}

		if (verdant != null) {
			Vec3 vEye = verdant.position().add(0, verdant.getBbHeight() * 0.5, 0);
			Vec3 toV = vEye.subtract(pPos);
			double dotV = toV.dot(normVel);
			if (dotV > 0 && dotV <= maxDistance) {
				Vec3 closestPoint = pPos.add(normVel.scale(dotV));
				if (closestPoint.distanceTo(vEye) <= 8.5) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean canInterceptProjectiles(ServerLevel level, Entity self) {
		if (!(self instanceof LivingEntity living) || !living.isAlive()) return false;
		boolean criticalHp = (living.getHealth() / living.getMaxHealth()) <= 0.35f;
		boolean guardingVerdant = findNearestVerdantEngine(level, self, FRACTUS_VERDANT_DETECTION_RANGE) != null;
		return criticalHp || guardingVerdant;
	}

	private static Entity findIncomingProjectile(ServerLevel level, Entity self, double range) {
		if (!canInterceptProjectiles(level, self)) {
			self.getPersistentData().putInt("intercept_proj_id", -1);
			return null;
		}

		AABB searchBox = self.getBoundingBox().inflate(range);
		List<Entity> candidates = level.getEntities(self, searchBox, e -> isProjectileHeadingTowards(self, e, range));

		if (candidates.isEmpty()) {
			self.getPersistentData().putInt("intercept_proj_id", -1);
			return null;
		}

		java.util.Set<Integer> claimedTargetIds = new java.util.HashSet<>();
		List<Entity> nearbyAllies = level.getEntities(self, self.getBoundingBox().inflate(32.0), e -> e != self && isFractusKind(e) && e.isAlive() && canInterceptProjectiles(level, e));
		for (Entity ally : nearbyAllies) {
			int claimedId = persistentInt(ally, "intercept_proj_id", -1);
			if (claimedId != -1) {
				claimedTargetIds.add(claimedId);
			}
		}

		candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(self)));

		for (Entity candidate : candidates) {
			int cId = candidate.getId();
			if (claimedTargetIds.contains(cId)) {
				continue;
			}

			double selfDistSqr = candidate.distanceToSqr(self);
			boolean hasCloserAlly = false;

			for (Entity ally : nearbyAllies) {
				int allyClaim = persistentInt(ally, "intercept_proj_id", -1);
				if (allyClaim == -1 || allyClaim == cId) {
					if (candidate.distanceToSqr(ally) < selfDistSqr - 0.25) {
						hasCloserAlly = true;
						break;
					}
				}
			}

			if (!hasCloserAlly) {
				self.getPersistentData().putInt("intercept_proj_id", cId);
				return candidate;
			}
		}

		self.getPersistentData().putInt("intercept_proj_id", -1);
		return null;
	}

	private static Vec3 interceptProjectilesAlongLaser(ServerLevel level, Entity self, Vec3 start, Vec3 end) {
		Vec3 ray = end.subtract(start);
		AABB box = new AABB(start, end).inflate(3.5);
		List<Entity> projs = level.getEntities(self, box, e -> isInterceptableProjectile(e, self));
		Vec3 truncatedEnd = end;
		double closestDistSqr = end.distanceToSqr(start);

		for (Entity proj : projs) {
			Vec3 pPos = proj.position().add(0, proj.getBbHeight() * 0.5, 0);
			Vec3 pToStart = pPos.subtract(start);
			double lenSqr = ray.lengthSqr();
			if (lenSqr > 0.001) {
				double t = Mth.clamp(pToStart.dot(ray) / lenSqr, 0.0, 1.0);
				Vec3 closestOnBeam = start.add(ray.scale(t));
				if (pPos.distanceToSqr(closestOnBeam) <= 12.25) {
					level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, pPos.x, pPos.y, pPos.z, 15, 0.2, 0.2, 0.2, 0.1);
					level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, pPos.x, pPos.y, pPos.z, 1, 0.0, 0.0, 0.0, 0.0);
					level.playSound(null, pPos.x, pPos.y, pPos.z, net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.4f);
					proj.discard();

					double dSqr = pPos.distanceToSqr(start);
					if (dSqr < closestDistSqr) {
						closestDistSqr = dSqr;
						truncatedEnd = pPos;
					}
				}
			}
		}
		return truncatedEnd;
	}

	private static boolean isNearbyFractusFiring(ServerLevel level, Entity self) {
		if (!(self instanceof Mob selfMob)) {
			return false;
		}
		LivingEntity target = selfMob.getTarget();
		if (target == null) {
			return false;
		}

		AABB searchBox = self.getBoundingBox().inflate(SUPPRESSION_RANGE);

		for (Entity entity : level.getEntities(self, searchBox, e -> isFractusKind(e))) {
			if (entity instanceof Mob otherMob && otherMob.getTarget() == target) {
				if (persistentInt(entity, K_LASER_STATE, 0) == 3 || persistentInt(entity, K_FIRE, 0) > 0) {
					return true;
				}
			}
		}

		return false;
	}

	private static Vec3 predictedTargetEyePosition(LivingEntity target, boolean angry) {
		double leadTicks = angry ? ANGRY_TARGET_LEAD_TICKS : TARGET_LEAD_TICKS;
		Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
		Vec3 vel = target.getDeltaMovement();
		double velY = target.onGround() ? 0.0 : vel.y;
		Vec3 leadVel = new Vec3(vel.x, velY, vel.z);
		return targetCenter.add(leadVel.scale(leadTicks));
	}

	private static Vec3 updateLaserAim(Entity self, Entity target, boolean angry, boolean firing) {
		Vec3 start = laserStart(self);
		Vec3 targetPos = (target instanceof LivingEntity living) ? predictedTargetEyePosition(living, angry) : (target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0) : start.add(self.getLookAngle().scale(10.0)));
		Vec3 desired = targetPos.subtract(start);

		if (desired.lengthSqr() < 0.001) {
			desired = self.getLookAngle();
		}

		desired = desired.normalize();

		Vec3 current = new Vec3(
			persistentDouble(self, K_AIM_X, 0.0),
			persistentDouble(self, K_AIM_Y, 0.0),
			persistentDouble(self, K_AIM_Z, 0.0)
		);

		if (current.lengthSqr() < 0.001) {
			current = desired;
		} else {
			current = current.normalize();
		}

		double turnRate = firing
			? (angry ? ANGRY_FIRING_AIM_TURN_RATE : FIRING_AIM_TURN_RATE)
			: (angry ? ANGRY_AIM_LOCK_TURN_RATE : AIM_LOCK_TURN_RATE);
		Vec3 adjusted = rotateToward(current, desired, turnRate);

		self.getPersistentData().putDouble(K_AIM_X, adjusted.x);
		self.getPersistentData().putDouble(K_AIM_Y, adjusted.y);
		self.getPersistentData().putDouble(K_AIM_Z, adjusted.z);

		setEntityLookDirection(self, adjusted);

		return adjusted;
	}

	private static void setEntityLookDirection(Entity entity, Vec3 dir) {
		if (dir.lengthSqr() < 0.001) return;
		Vec3 norm = dir.normalize();
		double dx = norm.x;
		double dy = norm.y;
		double dz = norm.z;
		double dh = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		float pitch = (float)(-(Mth.atan2(dy, dh) * (180.0 / Math.PI)));

		entity.setXRot(pitch);
		entity.setYRot(yaw);
		entity.xRotO = pitch;
		entity.yRotO = yaw;

		if (entity instanceof LivingEntity living) {
			living.yHeadRot = yaw;
			living.yBodyRot = yaw;
			living.yHeadRotO = yaw;
			living.yBodyRotO = yaw;
			living.setYHeadRot(yaw);
			living.setYBodyRot(yaw);
		}
		if (entity instanceof Mob mob) {
			Vec3 targetLook = entity.getEyePosition().add(norm.scale(10.0));
			mob.getLookControl().setLookAt(targetLook.x, targetLook.y, targetLook.z, 180.0F, 180.0F);
		}
	}

	private static Vec3 rotateToward(Vec3 current, Vec3 desired, double maxRadians) {
		double dot = Mth.clamp(current.dot(desired), -1.0, 1.0);
		double angle = Math.acos(dot);

		if (angle <= maxRadians || angle < 0.0001) {
			return desired;
		}

		double blend = maxRadians / angle;
		return current.scale(1.0 - blend).add(desired.scale(blend)).normalize();
	}

	private static LaserHit raycastLaser(ServerLevel level, Entity self, Vec3 direction, double maxRange) {
		Vec3 start = laserStart(self);

		if (direction.lengthSqr() < 0.001) {
			direction = self.getLookAngle();
		}

		Vec3 end = start.add(direction.normalize().scale(maxRange));

		BlockHitResult blockHit = clipBlocks(level, self, start, end);
		Vec3 blockedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

		EntityHitResult entityHit = clipEntities(level, self, start, blockedEnd);

		if (entityHit != null && entityHit.getEntity() != null) {
			Entity hitEnt = entityHit.getEntity();
			if (hitEnt instanceof LivingEntity living && canDamage(self, living)) {
				return new LaserHit(entityHit.getLocation(), living, null, null);
			} else if (isInterceptableProjectile(hitEnt, self)) {
				return new LaserHit(entityHit.getLocation(), hitEnt, null, null);
			}
		}

		return new LaserHit(blockedEnd, null, blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getBlockPos(), blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getDirection());
	}

	private static Vec3 laserStart(Entity entity) {
		Vec3 base = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
		float yaw = entity.getYRot();
		float yawRad = -yaw * ((float)Math.PI / 180F);
		float cosYaw = Mth.cos(yawRad);
		float sinYaw = Mth.sin(yawRad);
		double rx = net.mcreator.thebackwoods.FractusLaserBeam.OFFSET_X * cosYaw - net.mcreator.thebackwoods.FractusLaserBeam.OFFSET_Z * sinYaw;
		double rz = net.mcreator.thebackwoods.FractusLaserBeam.OFFSET_X * sinYaw + net.mcreator.thebackwoods.FractusLaserBeam.OFFSET_Z * cosYaw;
		return base.add(rx, net.mcreator.thebackwoods.FractusLaserBeam.OFFSET_Y, rz);
	}

	private static BlockHitResult clipBlocks(ServerLevel level, Entity self, Vec3 start, Vec3 end) {
		return level.clip(new ClipContext(
			start,
			end,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			self
		));
	}

	private static EntityHitResult clipEntities(ServerLevel level, Entity self, Vec3 start, Vec3 end) {
		Vec3 ray = end.subtract(start);
		AABB search = self.getBoundingBox().expandTowards(ray).inflate(1.0);

		Entity closestEntity = null;
		Vec3 closestHit = null;
		double closestDistance = Double.MAX_VALUE;

		for (Entity candidate : level.getEntities(self, search, candidate -> (candidate instanceof LivingEntity living && canDamage(self, living)) || isInterceptableProjectile(candidate, self))) {
			AABB hitBox = candidate.getBoundingBox().inflate(0.25);
			Optional<Vec3> optionalHit = hitBox.clip(start, end);

			if (hitBox.contains(start)) {
				optionalHit = Optional.of(start);
			}

			if (optionalHit.isEmpty()) {
				continue;
			}

			double distance = start.distanceToSqr(optionalHit.get());

			if (distance < closestDistance) {
				closestDistance = distance;
				closestEntity = candidate;
				closestHit = optionalHit.get();
			}
		}

		return closestEntity == null ? null : new EntityHitResult(closestEntity, closestHit);
	}

	private static void igniteLaserHitBlock(ServerLevel level, Entity entity, BlockPos blockPos, Direction blockFace) {
		if (blockPos == null) {
			return;
		}

		if (entity != null && isNoFireDimension(entity)) {
			return;
		}

		BlockPos firePos = blockFace == null ? blockPos : blockPos.relative(blockFace);

		if (entity != null && firePos.closerToCenterThan(entity.position(), 2.8)) {
			return;
		}

		if (!level.getBlockState(firePos).isAir()) {
			return;
		}

		BlockState fireState = Blocks.FIRE.defaultBlockState();

		if (fireState.canSurvive(level, firePos)) {
			level.setBlock(firePos, fireState, 3);
		}
	}

	private static void igniteLaserTrailBlocks(ServerLevel level, Entity entity, Vec3 start, Vec3 end, boolean aggressive) {
		if (entity != null && isNoFireDimension(entity)) {
			return;
		}

		Vec3 line = end.subtract(start);
		double length = line.length();

		if (length < 0.01) {
			return;
		}

		Vec3 direction = line.normalize();
		BlockPos lastPos = null;
		int sampleIndex = 0;
		int igniteEvery = aggressive ? 1 : 3;

		for (double d = 0.0; d <= length; d += LASER_TRAIL_FIRE_STEP) {
			Vec3 sample = start.add(direction.scale(d));
			BlockPos pos = BlockPos.containing(sample.x, sample.y, sample.z);

			if (pos.equals(lastPos)) {
				continue;
			}

			lastPos = pos;

			if (sampleIndex++ % igniteEvery != 0) {
				continue;
			}

			igniteNearLaserSample(level, entity, pos);
		}
	}

	private static void igniteNearLaserSample(ServerLevel level, Entity entity, BlockPos pos) {
		if (trySetLaserFire(level, entity, pos)) {
			return;
		}

		for (Direction direction : Direction.values()) {
			if (trySetLaserFire(level, entity, pos.relative(direction))) {
				return;
			}
		}
	}

	private static boolean trySetLaserFire(ServerLevel level, Entity entity, BlockPos firePos) {
		if (entity != null && isNoFireDimension(entity)) {
			return false;
		}

		if (entity != null && firePos.closerToCenterThan(entity.position(), 2.8)) {
			return false;
		}

		if (!level.getBlockState(firePos).isAir()) {
			return false;
		}

		BlockState fireState = Blocks.FIRE.defaultBlockState();

		if (!fireState.canSurvive(level, firePos)) {
			return false;
		}

		level.setBlock(firePos, fireState, 3);
		return true;
	}

	private static void igniteLaserHitEntity(Entity hitEntity) {
		if (hitEntity == null) {
			return;
		}
		if (hitEntity instanceof LivingEntity living) {
			if (!isNoFireDimension(hitEntity) && !living.fireImmune()) {
				living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), LASER_ENTITY_FIRE_TICKS));
			}
		} else if (isInterceptableProjectile(hitEntity, null)) {
			Vec3 pos = hitEntity.position();
			if (hitEntity.level() instanceof ServerLevel level) {
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, pos.x, pos.y, pos.z, 15, 0.2, 0.2, 0.2, 0.1);
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
				level.playSound(null, pos.x, pos.y, pos.z, net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.4f);
			}
			hitEntity.discard();
		}
	}

	private static void destroyWeakBlocksInLaserPath(ServerLevel level, Entity entity, Vec3 start, Vec3 end, BlockPos blockedPos, boolean angry) {
		Vec3 line = end.subtract(start);
		double length = line.length();

		if (length < 0.01) {
			return;
		}

		Vec3 direction = line.normalize();
		BlockPos lastPos = null;
		float maxHardness = angry ? ANGRY_WEAK_BLOCK_MAX_HARDNESS : WEAK_BLOCK_MAX_HARDNESS;

		for (double d = 0.0; d <= length; d += BLOCK_BREAK_STEP) {
			Vec3 sample = start.add(direction.scale(d));
			BlockPos pos = BlockPos.containing(sample.x, sample.y, sample.z);

			if (pos.equals(lastPos)) {
				continue;
			}

			lastPos = pos;
			BlockState state = level.getBlockState(pos);

			if (state.isAir()) {
				continue;
			}

			float hardness = state.getDestroySpeed(level, pos);

			if (hardness < 0.0f) {
				break;
			}

			if (hardness > maxHardness) {
				break;
			}

			if (state.hasBlockEntity()) {
				break;
			}

			boolean shouldDrop = level.getRandom().nextFloat() < 0.20f && level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(pos).inflate(4.0)).size() < 12;
			level.destroyBlock(pos, shouldDrop, entity);
		}

		if (blockedPos != null) {
			destroyWeakBlock(level, entity, blockedPos, maxHardness);
		}
	}

	private static void destroyWeakBlock(ServerLevel level, Entity entity, BlockPos pos, float maxHardness) {
		BlockState state = level.getBlockState(pos);

		if (evaporateWaterAt(level, pos, state)) {
			return;
		}

		if (state.isAir()) {
			return;
		}

		float hardness = state.getDestroySpeed(level, pos);

		if (hardness < 0.0f || hardness > maxHardness || state.hasBlockEntity()) {
			return;
		}

		boolean shouldDrop = BURST_BLOCK_DROPS && level.getRandom().nextFloat() < 0.20f && level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(pos).inflate(4.0)).size() < 12;
		level.destroyBlock(pos, shouldDrop, entity);
	}

	private static void handleBurstLaser(ServerLevel level, Entity entity, LivingEntity target, int burstTicks) {
		int elapsed = BURST_TOTAL_TICKS - burstTicks;
		Vec3 start = laserStart(entity);
		Vec3 direction = burstDirection(entity, target, start);
		Vec3 end = start.add(direction.scale(BURST_LASER_RANGE));

		setEntityLookDirection(entity, direction);

		if (elapsed < BURST_FIRE_PEAK_TICK) {
			setBurstLaserState(entity, 2);
			spawnBurstBuildup(level, entity, start, elapsed);
			return;
		}

		if (elapsed <= BURST_CORE_END_TICK) {
			setBurstLaserState(entity, 3);
			BlockHitResult blockHit = clipBlocks(level, entity, start, end);
			Vec3 blockedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
			destroyBurstBlocksInLaserPath(level, entity, start, blockedEnd, blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getBlockPos());
			blockHit = clipBlocks(level, entity, start, end);
			blockedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
			igniteLaserHitBlock(level, entity, blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getBlockPos(), blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getDirection());

			spawnBurstBeam(level, start, blockedEnd, elapsed);

			if ((elapsed - BURST_FIRE_PEAK_TICK) % 10 == 0) {
				damageBurstEntities(level, entity, start, blockedEnd, 1.0f);
			}

			return;
		}

		if (burstTicks == 1) {
			// Seamless transition: stop activating and start deactivating at the exact same time
			setBurstLaserState(entity, 4);
			entity.getPersistentData().putInt(K_BURST_DEACT_PLAY, BURST_DEACT_DURATION_TICKS);
			entity.getPersistentData().putInt(K_BURST_DEACT_DELAY, 0);
			setLaserState(entity, 0);
			entity.getPersistentData().putInt(K_COOLDOWN, Math.max(BURST_DEACT_DURATION_TICKS + 10, 60));
			entity.getPersistentData().putInt(K_BURST_COOLDOWN, target != null ? burstCooldownTicksForTarget(target) : BURST_COOLDOWN_TICKS);
			entity.getPersistentData().putInt(K_CHARGE, 0);
			entity.getPersistentData().putInt(K_FIRE, 0);
			entity.getPersistentData().putDouble(K_BURST_AIM_X, 0.0);
			entity.getPersistentData().putDouble(K_BURST_AIM_Y, 0.0);
			entity.getPersistentData().putDouble(K_BURST_AIM_Z, 0.0);
			entity.getPersistentData().remove("fractus_burst_target_block_x");
			entity.getPersistentData().remove("fractus_burst_target_block_y");
			entity.getPersistentData().remove("fractus_burst_target_block_z");
			entity.getPersistentData().putInt(K_VULNERABLE, 40);
			clearBurstBlockDamage(level, entity);
			return;
		}

		setBurstLaserState(entity, 3); // Hold activating animation during dissipation until final tick
		BlockHitResult blockHit = clipBlocks(level, entity, start, end);
		Vec3 impact = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
		destroyBurstBlocksInLaserPath(level, entity, start, impact, blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getBlockPos());
		blockHit = clipBlocks(level, entity, start, end);
		impact = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
		igniteLaserHitBlock(level, entity, blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getBlockPos(), blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getDirection());
		spawnBurstDissipation(level, impact, direction, elapsed);

		int dissipationTotal = BURST_TOTAL_TICKS - BURST_CORE_END_TICK;
		float dissipationProgress = (float) (elapsed - BURST_CORE_END_TICK) / (float) Math.max(1, dissipationTotal);
		float damageMultiplier = (float) Math.pow(Math.max(0.0f, 1.0f - dissipationProgress), 1.8);

		if ((elapsed - BURST_FIRE_PEAK_TICK) % 10 == 0) {
			damageBurstEntities(level, entity, start, impact, damageMultiplier);
		}
	}

	private static Vec3 burstDirection(Entity entity, LivingEntity target, Vec3 start) {
		LivingEntity burstTarget = target != null ? target : retaliationTarget(entity);
		Vec3 desired;

		if (burstTarget != null) {
			Vec3 burstTargetCenter = burstTarget.position().add(0, burstTarget.getBbHeight() * 0.5, 0);
			desired = burstTargetCenter.subtract(start);
			if (desired.lengthSqr() < 0.001) {
				desired = entity.getLookAngle();
			}
		} else if (entity.getPersistentData().contains("fractus_burst_target_block_x")) {
			Vec3 blockTarget = new Vec3(
				entity.getPersistentData().getDouble("fractus_burst_target_block_x"),
				entity.getPersistentData().getDouble("fractus_burst_target_block_y"),
				entity.getPersistentData().getDouble("fractus_burst_target_block_z")
			);
			desired = blockTarget.subtract(start);
			if (desired.lengthSqr() < 0.001) {
				desired = entity.getLookAngle();
			}
		} else {
			desired = entity.getLookAngle();
		}
		desired = desired.normalize();

		Vec3 current = new Vec3(
			persistentDouble(entity, K_BURST_AIM_X, 0.0),
			persistentDouble(entity, K_BURST_AIM_Y, 0.0),
			persistentDouble(entity, K_BURST_AIM_Z, 0.0)
		);

		if (current.lengthSqr() < 0.001) {
			current = desired;
		} else {
			current = current.normalize();
		}

		Vec3 adjusted = rotateToward(current, desired, BURST_LASER_TURN_RATE * 3.0);

		entity.getPersistentData().putDouble(K_BURST_AIM_X, adjusted.x);
		entity.getPersistentData().putDouble(K_BURST_AIM_Y, adjusted.y);
		entity.getPersistentData().putDouble(K_BURST_AIM_Z, adjusted.z);

		entity.getPersistentData().putDouble(K_AIM_X, adjusted.x);
		entity.getPersistentData().putDouble(K_AIM_Y, adjusted.y);
		entity.getPersistentData().putDouble(K_AIM_Z, adjusted.z);

		setEntityLookDirection(entity, adjusted);

		return adjusted;
	}

	private static double squareDistanceToLineSegment(double px, double py, double pz, double sx, double sy, double sz, double lx, double ly, double lz, double lengthSq) {
		if (lengthSq < 1e-6) {
			double dx = px - sx;
			double dy = py - sy;
			double dz = pz - sz;
			return dx * dx + dy * dy + dz * dz;
		}
		double t = ((px - sx) * lx + (py - sy) * ly + (pz - sz) * lz) / lengthSq;
		if (t < 0.0) t = 0.0;
		else if (t > 1.0) t = 1.0;
		double projX = sx + t * lx;
		double projY = sy + t * ly;
		double projZ = sz + t * lz;
		double dx = px - projX;
		double dy = py - projY;
		double dz = pz - projZ;
		return dx * dx + dy * dy + dz * dz;
	}

	private static void destroyBurstBlocksInLaserPath(ServerLevel level, Entity entity, Vec3 start, Vec3 end, BlockPos blockedPos) {
		if (level.getGameTime() % 2 != 0) {
			return;
		}

		Vec3 line = end.subtract(start);
		double length = line.length();

		if (length < 0.01) {
			return;
		}

		double radius = BURST_LASER_RADIUS + 0.35;
		double radiusSq = radius * radius;

		double sx = start.x;
		double sy = start.y;
		double sz = start.z;
		double lx = line.x;
		double ly = line.y;
		double lz = line.z;
		double lengthSq = lx * lx + ly * ly + lz * lz;

		java.util.Set<Long> visited = new java.util.HashSet<>();
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		double stepSize = 2.0;
		int steps = (int) Math.ceil(length / stepSize);
		Vec3 direction = line.normalize();
		for (int i = 0; i <= steps; i++) {
			double currentDist = Math.min(length, i * stepSize);
			Vec3 stepPoint = start.add(direction.scale(currentDist));

			int sMinX = (int) Math.floor(stepPoint.x - radius);
			int sMaxX = (int) Math.ceil(stepPoint.x + radius);
			int sMinY = (int) Math.floor(stepPoint.y - radius);
			int sMaxY = (int) Math.ceil(stepPoint.y + radius);
			int sMinZ = (int) Math.floor(stepPoint.z - radius);
			int sMaxZ = (int) Math.ceil(stepPoint.z + radius);

			for (int x = sMinX; x <= sMaxX; x++) {
				for (int y = sMinY; y <= sMaxY; y++) {
					for (int z = sMinZ; z <= sMaxZ; z++) {
						long packed = BlockPos.asLong(x, y, z);
						if (visited.add(packed)) {
							double px = x + 0.5;
							double py = y + 0.5;
							double pz = z + 0.5;
							if (squareDistanceToLineSegment(px, py, pz, sx, sy, sz, lx, ly, lz, lengthSq) <= radiusSq) {
								mutablePos.set(x, y, z);
								degradeBurstBlockThermal(level, entity, mutablePos, BURST_BLOCK_MAX_HARDNESS);
							}
						}
					}
				}
			}
		}

		if (blockedPos != null) {
			degradeBurstBlockThermal(level, entity, blockedPos.immutable(), BURST_BLOCK_MAX_HARDNESS);
		}
	}

	private static boolean isMagmaCandidate(BlockState state, float hardness) {
		if (state.isAir() || state.hasBlockEntity()) return false;
		if (hardness < 0.8f) return false;
		if (state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.BEDROCK) || state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)) return false;

		ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		String path = key != null ? key.getPath().toLowerCase(java.util.Locale.ROOT) : "";

		if (path.contains("cloud") || path.contains("leaf") || path.contains("leaves") || path.contains("dirt")
			|| path.contains("sand") || path.contains("gravel") || path.contains("wood") || path.contains("log")
			|| path.contains("wool") || path.contains("glass") || path.contains("ice") || path.contains("snow")
			|| path.contains("door") || path.contains("trapdoor") || path.contains("rail") || path.contains("anvil")) {
			return false;
		}

		if (state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
			|| state.is(net.minecraft.tags.BlockTags.BASE_STONE_NETHER)
			|| state.is(net.minecraft.tags.BlockTags.STONE_BRICKS)
			|| state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) {
			return true;
		}

		if (path.contains("stone") || path.contains("rock") || path.contains("deepslate") || path.contains("granite")
			|| path.contains("diorite") || path.contains("andesite") || path.contains("basalt") || path.contains("blackstone")
			|| path.contains("terracotta") || path.contains("tuff") || path.contains("sandstone")) {
			return true;
		}

		return false;
	}

	private static void degradeBurstBlockThermal(ServerLevel level, Entity entity, BlockPos pos, float maxHardness) {
		BlockState state = level.getBlockState(pos);

		if (evaporateWaterAt(level, pos, state)) {
			BURST_THERMAL_PROGRESS.remove(pos.immutable());
			return;
		}

		if (state.isAir()) {
			BURST_THERMAL_PROGRESS.remove(pos.immutable());
			return;
		}

		float hardness = state.getDestroySpeed(level, pos);

		if (hardness < 0.0f || hardness > maxHardness || state.hasBlockEntity()) {
			return;
		}

		// Instantly vaporize trivial blocks (leaves, grass, flowers, torches, etc.)
		if (hardness <= 0.25f) {
			level.destroyBlock(pos, false, entity);
			BURST_THERMAL_PROGRESS.remove(pos.immutable());
			return;
		}

		// Progressive thermal degradation based on configurable thermal power
		double degradationRate = BURST_LASER_THERMAL_POWER / Math.max(1.0, hardness);
		float progress = BURST_THERMAL_PROGRESS.getOrDefault(pos, 0.0f) + (float) degradationRate;

		if (progress >= 1.0f) {
			level.destroyBlockProgress(pos.hashCode(), pos, -1);
			boolean shouldDrop = level.getRandom().nextFloat() < 0.25f && level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(pos).inflate(4.0)).size() < 12;
			level.destroyBlock(pos, shouldDrop, entity);
			BURST_THERMAL_PROGRESS.remove(pos.immutable());
			if (level.getRandom().nextFloat() < 0.09f) {
				level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.01);
			}
		} else {
			BURST_THERMAL_PROGRESS.put(pos.immutable(), progress);
			if (progress >= 0.45f && isMagmaCandidate(state, hardness)) {
				if (level.getRandom().nextFloat() < 0.35f) {
					level.destroyBlockProgress(pos.hashCode(), pos, -1);
					level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
					if (level.getRandom().nextFloat() < 0.12f) {
						level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.02);
					}
				} else {
					int stage = Math.min(9, (int) (progress * 10.0f));
					level.destroyBlockProgress(pos.hashCode(), pos, stage);
				}
			} else {
				int stage = Math.min(9, (int) (progress * 10.0f));
				level.destroyBlockProgress(pos.hashCode(), pos, stage);
			}
		}
	}

	private static boolean isPassiveOrAmbientMob(LivingEntity target) {
		if (target == null) return false;
		if (target instanceof Player) return false;
		if (target instanceof net.minecraft.world.entity.monster.Enemy) return false;
		if (target instanceof net.minecraft.world.entity.animal.Animal || target instanceof net.minecraft.world.entity.ambient.AmbientCreature || target instanceof net.minecraft.world.entity.animal.WaterAnimal || target instanceof net.minecraft.world.entity.npc.Villager || target instanceof net.minecraft.world.entity.npc.Npc) return true;
		return false;
	}

	private static boolean isBurstSequenceActive(Entity entity) {
		if (entity == null) return false;
		int burstTicks = persistentInt(entity, K_BURST_TIMER, 0);
		int burstDeactDelay = persistentInt(entity, K_BURST_DEACT_DELAY, 0);
		int burstDeactPlay = persistentInt(entity, K_BURST_DEACT_PLAY, 0);
		boolean burstCharging = persistentBoolean(entity, "is_burst_charging", false)
			|| persistentBoolean(entity, "is_laser_burst_activating", false)
			|| persistentBoolean(entity, "is_burst_firing", false);
		int sphereTicks = persistentInt(entity, "fractus_sphere_burst_ticks", 0);
		boolean sphereBursting = persistentBoolean(entity, "is_sphere_bursting", false);
		return burstTicks > 0 || burstDeactDelay > 0 || burstDeactPlay > 0 || burstCharging || sphereTicks > 0 || sphereBursting;
	}

	private static void clearBurstBlockDamage(ServerLevel level, Entity entity) {
		if (!BURST_THERMAL_PROGRESS.isEmpty()) {
			for (java.util.Map.Entry<BlockPos, Float> entry : BURST_THERMAL_PROGRESS.entrySet()) {
				BlockPos damagedPos = entry.getKey();
				level.destroyBlockProgress(damagedPos.hashCode(), damagedPos, -1);
			}
			BURST_THERMAL_PROGRESS.clear();
		}
	}

	private static boolean evaporateWaterAt(ServerLevel level, BlockPos pos, BlockState state) {
		if (!state.getFluidState().is(FluidTags.WATER)) {
			return false;
		}

		if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
			level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
		} else {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}

		return true;
	}

	private static void damageBurstEntities(ServerLevel level, Entity entity, Vec3 start, Vec3 end, float damageMultiplier) {
		if (damageMultiplier <= 0.01f) {
			return;
		}

		Vec3 line = end.subtract(start);
		double lengthSqr = line.lengthSqr();

		if (lengthSqr < 0.001) {
			return;
		}

		double effectiveRadius = BURST_LASER_RADIUS * Math.pow(damageMultiplier, 0.4);
		AABB search = new AABB(start, end).inflate(effectiveRadius + 1.0);
		float effectiveDamage = BURST_LASER_DAMAGE * damageMultiplier;

		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search, target -> canBurstDamage(entity, target))) {
			Vec3 toCandidate = candidate.getEyePosition().subtract(start);
			double t = Mth.clamp(toCandidate.dot(line) / lengthSqr, 0.0, 1.0);
			Vec3 closest = start.add(line.scale(t));

			if (candidate.getEyePosition().distanceTo(closest) <= effectiveRadius + candidate.getBbWidth() * 0.5) {
				candidate.invulnerableTime = 0;
				if (entity instanceof LivingEntity attacker) {
					candidate.hurt(attacker.damageSources().mobAttack(attacker), effectiveDamage);
				} else {
					candidate.hurt(new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), effectiveDamage);
				}
				applyBurstKnockback(candidate, start, line, damageMultiplier);
			}
		}

		for (Entity nonLiving : level.getEntities(entity, search, e -> !(e instanceof LivingEntity) && e.isAlive())) {
			Vec3 pos = nonLiving.position().add(0, nonLiving.getBbHeight() * 0.5, 0);
			Vec3 toNonLiving = pos.subtract(start);
			double t = Mth.clamp(toNonLiving.dot(line) / lengthSqr, 0.0, 1.0);
			Vec3 closest = start.add(line.scale(t));

			if (pos.distanceTo(closest) <= effectiveRadius + nonLiving.getBbWidth() * 0.5 + 0.3) {
				level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 2, 0.15, 0.15, 0.15, 0.05);
				level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 3, 0.1, 0.1, 0.1, 0.02);
				level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 5, 0.15, 0.15, 0.15, 0.02);
				level.playSound(
					null,
					BlockPos.containing(pos.x, pos.y, pos.z),
					SoundEvents.FIRE_EXTINGUISH,
					SoundSource.HOSTILE,
					0.6f,
					1.2f + level.random.nextFloat() * 0.4f
				);
				nonLiving.discard();
			}
		}
	}

	private static void applyBurstKnockback(LivingEntity target, Vec3 start, Vec3 beamLine, float powerMultiplier) {
		Vec3 forward = new Vec3(beamLine.x, 0.0, beamLine.z);

		if (forward.lengthSqr() < 0.001) {
			Vec3 away = target.position().subtract(start);
			forward = new Vec3(away.x, 0.0, away.z);
			if (forward.lengthSqr() < 0.001) {
				forward = target.getLookAngle().scale(-1.0);
			}
		}

		forward = forward.normalize();
		double knockbackH = BURST_KNOCKBACK_HORIZONTAL * (double) powerMultiplier;
		double knockbackV = BURST_KNOCKBACK_VERTICAL * (double) powerMultiplier;
		Vec3 knockback = forward.scale(knockbackH).add(0.0, knockbackV, 0.0);
		target.setDeltaMovement(target.getDeltaMovement().add(knockback));
		target.hasImpulse = true;
		target.hurtMarked = true;
	}

	private static void spawnBurstBuildup(ServerLevel level, Entity entity, Vec3 center, int elapsed) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_BURST_CHARGING_PARTICLES) {
			return;
		}
		double progress = Mth.clamp((double) elapsed / BURST_FIRE_PEAK_TICK, 0.0, 1.0);
		int baseCount = 35;
		int addedCount = 85;
		int count = baseCount + (int) (progress * addedCount);
		double radius = 2.8 - progress * 2.1;
		net.minecraft.core.particles.ParticleOptions particle = burstChargeParticle(progress);

		for (int i = 0; i < count; i++) {
			double u = entity.getRandom().nextDouble();
			double v = entity.getRandom().nextDouble();
			double theta = u * 2.0 * Math.PI;
			double phi = Math.acos(2.0 * v - 1.0);

			double dx = Math.sin(phi) * Math.cos(theta) * radius;
			double dy = Math.sin(phi) * Math.sin(theta) * radius;
			double dz = Math.cos(phi) * radius;

			Vec3 pos = center.add(dx, dy, dz);
			level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.08, 0.08, 0.08, 0.0);
		}

		Vec3 direction = entity.getLookAngle().normalize();
		Vec3 up = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 side = direction.cross(up).normalize();
		Vec3 verticalSide = direction.cross(side).normalize();

		double ringRadius = (2.8) * (1.0 - progress * 0.95);
		int ringCount = 14;
		for (int j = 0; j < ringCount; j++) {
			double angle = Math.PI * 2.0 * j / ringCount + elapsed * 0.25;
			Vec3 ringPos = center.add(side.scale(Math.cos(angle) * ringRadius)).add(verticalSide.scale(Math.sin(angle) * ringRadius));
			level.sendParticles(particle, ringPos.x, ringPos.y, ringPos.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	private static void spawnBurstBeam(ServerLevel level, Vec3 start, Vec3 end, int elapsed) {
		Vec3 line = end.subtract(start);
		double length = line.length();

		if (length < 0.01) {
			return;
		}

		Vec3 direction = line.normalize();
		double ringRadius = elapsed == BURST_FIRE_PEAK_TICK ? BURST_LASER_SPIRAL_RADIUS * 1.8 : BURST_LASER_SPIRAL_RADIUS;
		int numArms = 2;
		double spacing = BURST_LASER_SPIRAL_SPACING * 0.4;

		Vec3 up = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 side = direction.cross(up).normalize();
		Vec3 verticalSide = direction.cross(side).normalize();
		net.minecraft.core.particles.ParticleOptions particle = getBurstLaserParticle();

		if (net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			double optimizedSpacing = spacing * 8.0; // 8x less dense sampling
			for (double d = 0.0; d <= length; d += optimizedSpacing) {
				Vec3 center = start.add(direction.scale(d));
				double coreRadius = 0.15;
				level.sendParticles(particle, center.x, center.y, center.z, 1, coreRadius, coreRadius, coreRadius, 0.0);

				for (int a = 0; a < numArms; a++) {
					double angle = (Math.PI * 2.0 * a / numArms) + (d * 0.22) + (elapsed * 0.75);
					Vec3 pos = center.add(side.scale(Math.cos(angle) * ringRadius)).add(verticalSide.scale(Math.sin(angle) * ringRadius));
					level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
				}
			}
		}

		if (net.mcreator.thebackwoods.FractusLaserBeam.USE_BURST_IMPACT_PARTICLES) {
			level.sendParticles(particle, end.x, end.y, end.z, 15, 0.8, 0.8, 0.8, 0.0);
		}
	}

	private static void spawnBurstDissipation(ServerLevel level, Vec3 impact, Vec3 direction, int elapsed) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_BURST_IMPACT_PARTICLES) {
			return;
		}
		double progress = Mth.clamp((double) (elapsed - BURST_CORE_END_TICK) / (BURST_TOTAL_TICKS - BURST_CORE_END_TICK), 0.0, 1.0);

		if (elapsed % 4 == 0) {
			int count = 28 - (int) (progress * 18.0);
			level.sendParticles(getBurstLaserParticle(), impact.x, impact.y, impact.z, Math.max(6, count), 1.2 + progress * 2.2, 0.7, 1.2 + progress * 2.2, 0.0);
		}
	}

	private static void spawnDodgeParticles(ServerLevel level, Vec3 center) {
		level.sendParticles(getAngryLaserParticle(), center.x, center.y, center.z, 26, 0.35, 0.35, 0.35, 0.0);
	}

	private static net.minecraft.core.particles.ParticleOptions laserParticle(boolean angry) {
		return angry ? getAngryLaserParticle() : getNormalLaserParticle();
	}

	private static net.minecraft.core.particles.ParticleOptions burstChargeParticle(double progress) {
		if (progress < 0.45) {
			return getBurstChargeStartParticle();
		}

		if (progress < 0.75) {
			return getBurstChargeMidParticle();
		}

		return getBurstLaserParticle();
	}

	private static double currentHoverHeight(Entity entity, boolean angry) {
		return currentHoverHeight(entity, entity.position(), angry);
	}

	private static double currentHoverHeight(Entity entity, Vec3 referencePos, boolean angry) {
		double height = angry ? ANGRY_HOVER_HEIGHT : HOVER_HEIGHT;

		if (isEscapedContainmentDimension(entity)) {
			height += ESCAPED_CONTAINMENT_HOVER_BONUS;
		}

		if (entity.level().dimension().location().toString().equals("the_backwoods:the_sub_strata")) {
			if (entity instanceof Mob mob && mob.getTarget() instanceof Player) {
				height -= SUB_STRATA_PLAYER_COMBAT_HOVER_REDUCTION;
			}
		}

		if (entity.level() instanceof ServerLevel level) {
			BlockPos checkCenter = BlockPos.containing(referencePos.x, referencePos.y, referencePos.z);
			BlockPos below = checkCenter.below();
			for (int dy = 0; dy <= 4; dy++) {
				BlockPos checkPos = below.below(dy);
				if (!level.hasChunkAt(checkPos)) break;
				BlockState state = level.getBlockState(checkPos);
				if (isHarmfulBlock(state)) {
					height += (3.5 - dy * 0.5);
					break;
				}
			}

			// Ceiling & Enclosure Aware Hovering:
			// Standard Fractus hitbox is exactly 1 block in dimensions (1.0 width x 1.0 height).
			double entityHeight = Math.max(1.0, entity.getBbHeight());
			double floorY = referencePos.y;
			for (int dy = 0; dy <= 8; dy++) {
				BlockPos floorCheck = checkCenter.below(dy);
				if (!level.hasChunkAt(floorCheck)) break;
				BlockState fState = level.getBlockState(floorCheck);
				if (!fState.isAir() && (fState.isCollisionShapeFullBlock(level, floorCheck) || !fState.getCollisionShape(level, floorCheck).isEmpty())) {
					floorY = floorCheck.getY() + 1.0;
					break;
				}
			}

			double ceilingY = Double.MAX_VALUE;
			int maxScan = (int) Math.ceil(height + entityHeight + 3.0);
			for (int dy = 1; dy <= Math.max(12, maxScan); dy++) {
				BlockPos ceilCheck = BlockPos.containing(referencePos.x, floorY + dy, referencePos.z);
				if (!level.hasChunkAt(ceilCheck)) break;
				BlockState cState = level.getBlockState(ceilCheck);
				if (!cState.isAir() && (cState.isCollisionShapeFullBlock(level, ceilCheck) || !cState.getCollisionShape(level, ceilCheck).isEmpty())) {
					ceilingY = ceilCheck.getY();
					break;
				}
			}

			if (ceilingY < Double.MAX_VALUE) {
				double headroom = ceilingY - floorY;
				double topCushion = 0.35;
				double bottomCushion = 0.25;
				double availableHover = headroom - entityHeight - topCushion;

				if (availableHover <= bottomCushion) {
					height = Math.max(0.1, (headroom - entityHeight) * 0.5);
				} else {
					height = Math.min(height, availableHover);
					height = Math.max(bottomCushion, height);
				}
			}
		}

		return height;
	}

	private static void spawnLaser(ServerLevel level, Vec3 start, Vec3 end, double spacing, double jitter, boolean firing, boolean angry) {
		if (!firing) {
			return;
		}

		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			return;
		}

		net.minecraft.core.particles.ParticleOptions particle = laserParticle(angry);
		level.sendParticles(particle, end.x, end.y, end.z, 10, 0.08, 0.08, 0.08, 0.0);

		Vec3 line = end.subtract(start);
		double length = line.length();
		if (length >= 0.01) {
			Vec3 direction = line.normalize();
			for (double d = 0.0; d <= length; d += spacing) {
				Vec3 pos = start.add(direction.scale(d));
				level.sendParticles(particle, pos.x, pos.y, pos.z, 1, jitter, jitter, jitter, 0.0);
			}
		}
	}

	private static void spawnChargeBeamHum(ServerLevel level, Vec3 start, Vec3 end, double progress, boolean angry) {
	}

	private static void spawnChargeParticles(ServerLevel level, Entity entity, int chargeTicks, boolean angry) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			return;
		}
		Vec3 center = laserStart(entity);
		double progress = Mth.clamp((double) chargeTicks / CHARGE_TICKS, 0.0, 1.0);
		int count = angry ? 5 + (int) (progress * 12.0) : 2 + (int) (progress * 7.0);
		double radius = angry ? 0.75 - progress * 0.36 : 0.55 - progress * 0.28;

		for (int i = 0; i < count; i++) {
			double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
			double y = (entity.getRandom().nextDouble() - 0.5) * 0.55;
			Vec3 pos = center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
			level.sendParticles(laserParticle(angry), pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
			if (level.getRandom().nextFloat() < 0.25f) {
				level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
			}
		}
	}

	private static void spawnChargeInterruptParticles(ServerLevel level, Entity entity) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			return;
		}
		Vec3 center = laserStart(entity);

		for (int i = 0; i < 22; i++) {
			double angle = Math.PI * 2.0 * i / 22.0;
			double radius = 0.18 + i * 0.012;
			Vec3 pos = center.add(Math.cos(angle) * radius, (entity.getRandom().nextDouble() - 0.5) * 0.35, Math.sin(angle) * radius);
			level.sendParticles(getNormalLaserParticle(), pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.0);
		}
	}

	private static void spawnSuppressionPulse(ServerLevel level, Entity entity, int suppressionTicks) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			return;
		}
		if (suppressionTicks % 8 != 0) {
			return;
		}

		Vec3 center = laserStart(entity);
		double radius = 0.22 + (SUPPRESSION_TICKS - suppressionTicks) * 0.004;

		for (int i = 0; i < 14; i++) {
			double angle = Math.PI * 2.0 * i / 14.0;
			Vec3 pos = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
			level.sendParticles(laserParticle(isAngry(entity)), pos.x, pos.y, pos.z, 1, 0.018, 0.018, 0.018, 0.0);
		}
	}

	private static void spawnLeashBreakTrail(ServerLevel level, Entity entity) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			return;
		}
		Vec3 center = laserStart(entity);
		Vec3 movement = entity.getDeltaMovement();

		for (int i = 0; i < 5; i++) {
			Vec3 pos = center.subtract(movement.scale(i * 0.7));
			level.sendParticles(laserParticle(isAngry(entity)), pos.x, pos.y, pos.z, 1, 0.06, 0.06, 0.06, 0.0);
		}
	}

	private static void spawnVulnerabilityParticles(ServerLevel level, Entity entity, int vulnerabilityTicks) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			return;
		}
		if (vulnerabilityTicks % 3 != 0) {
			return;
		}

		Vec3 center = laserStart(entity);
		double progress = (double) vulnerabilityTicks / VULNERABILITY_TICKS;
		int count = 3 + (int) (progress * 5.0);

		for (int i = 0; i < count; i++) {
			double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
			double radius = 0.18 + entity.getRandom().nextDouble() * 0.32;
			double y = (entity.getRandom().nextDouble() - 0.5) * 0.35;
			Vec3 pos = center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
			level.sendParticles(laserParticle(isAngry(entity)), pos.x, pos.y, pos.z, 1, 0.035, 0.035, 0.035, 0.0);
		}
	}

	private static void spawnShieldParticles(ServerLevel level, Entity entity) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			return;
		}
		Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
		int count = 28;
		double radius = Math.max(entity.getBbWidth(), entity.getBbHeight()) * 0.65;

		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2.0 * i / count;
			Vec3 pos = center.add(Math.cos(angle) * radius, Math.sin(angle * 2.0) * 0.18, Math.sin(angle) * radius);
			level.sendParticles(getNormalLaserParticle(), pos.x, pos.y, pos.z, 1, 0.025, 0.025, 0.025, 0.0);
		}
	}

	private static void spawnPreFireWarning(ServerLevel level, Entity entity, boolean angry) {
		return; // Completely remove pre-fire warning particles!
	}

	private static void spawnFireStartWarning(ServerLevel level, Entity entity, boolean angry) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			return;
		}
		Vec3 center = laserStart(entity);
		net.minecraft.core.particles.ParticleOptions particle = laserParticle(angry);
		int count = angry ? 64 : 46;
		double radius = angry ? FIRE_START_RING_RADIUS * 1.18 : FIRE_START_RING_RADIUS;

		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2.0 * i / count;
			Vec3 outward = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
			Vec3 pos = center.add(outward.scale(radius));
			level.sendParticles(particle, pos.x, pos.y, pos.z, 0, outward.x, 0.0, outward.z, angry ? 0.12 : 0.09);
		}

		level.sendParticles(particle, center.x, center.y, center.z, angry ? 30 : 18, 0.12, 0.12, 0.12, 0.0);
	}

	private static void spawnPlayerLaserImpact(ServerLevel level, Vec3 impact, boolean angry) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_IMPACT_PARTICLES) {
			return;
		}
		net.minecraft.core.particles.ParticleOptions particle = laserParticle(angry);
		level.sendParticles(particle, impact.x, impact.y, impact.z, angry ? 35 : 26, 0.24, 0.24, 0.24, 0.0);
	}

	private static void spawnAngerTransitionParticles(ServerLevel level, Entity entity) {
		Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
		int count = 72;

		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2.0 * i / count;
			double yWave = Math.sin(angle * 3.0) * 0.24;
			Vec3 outward = new Vec3(Math.cos(angle), yWave, Math.sin(angle)).normalize();
			Vec3 pos = center.add(outward.scale(0.45));
			level.sendParticles(getAngryLaserParticle(), pos.x, pos.y, pos.z, 0, outward.x, outward.y, outward.z, 0.16);
		}

		level.sendParticles(getAngryLaserParticle(), center.x, center.y, center.z, 34, 0.32, 0.32, 0.32, 0.0);
	}

	private static void spawnDeathBurst(ServerLevel level, Entity entity) {
		Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
		net.minecraft.core.particles.ParticleOptions particle = laserParticle(isAngry(entity));
		int count = 96;

		for (int ring = 0; ring < 4; ring++) {
			double radius = 0.55 + ring * 0.34;
			double y = (ring - 1.5) * 0.16;
			double speed = 0.12 + ring * 0.035;

			for (int i = 0; i < count; i += 2) {
				double angle = Math.PI * 2.0 * i / count;
				Vec3 outward = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
				Vec3 pos = center.add(outward.scale(radius)).add(0.0, y, 0.0);
				level.sendParticles(particle, pos.x, pos.y, pos.z, 0, outward.x, 0.015 * (ring - 1.5), outward.z, speed);
			}
		}

		level.sendParticles(particle, center.x, center.y, center.z, 36, 0.28, 0.28, 0.28, 0.0);
	}

	private static net.minecraft.core.particles.ParticleOptions getSummonLaserParticle() {
		if (summonLaserParticle == null) {
			String[] potentialIds = new String[]{
				"the_backwoods:fractus_laser_particle_summon",
				"the_backwoods:fractus_laser_particle_blue",
				"the_backwoods:fractus_laser_blue",
				"the_backwoods:blue_fractus_laser_particle",
				"the_backwoods:fractus_laser_particle"
			};
			for (String id : potentialIds) {
				net.minecraft.core.particles.ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(id));
				if (type != null) {
					if (type instanceof net.minecraft.core.particles.SimpleParticleType simpleType) {
						summonLaserParticle = simpleType;
						break;
					}
					try {
						summonLaserParticle = net.minecraft.core.particles.ColorParticleOption.create((net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.ColorParticleOption>) type, 0xFF00A2FF);
						break;
					} catch (Exception ignored) {}
				}
			}
			if (summonLaserParticle == null) {
				summonLaserParticle = new DustParticleOptions(new Vector3f(0.0f, 0.635f, 1.0f), 1.25f);
			}
		}
		return summonLaserParticle;
	}

	private static int getSummonRemaining(Entity entity) {
		if (persistentInt(entity, K_SUMMON_INITIALIZED, 0) == 0) {
			int rRemaining = 1 + entity.getRandom().nextInt(3);
			entity.getPersistentData().putInt(K_SUMMON_REMAINING, rRemaining);
			entity.getPersistentData().putInt(K_SUMMON_INITIALIZED, 1);
			entity.getPersistentData().putInt(K_SUMMON_COOLDOWN, 0);
			return rRemaining;
		}
		return persistentInt(entity, K_SUMMON_REMAINING, 0);
	}

	private static boolean checkAndStartSummoningRitual(ServerLevel level, Entity entity) {
		if (isBurstSequenceActive(entity)) {
			return false;
		}
		if (entity.tickCount % 40 != 0) {
			return false;
		}
		if (persistentInt(entity, K_SUMMON_ROLE, 0) > 0) {
			return false;
		}
		if (level.dimension().location().toString().equals("the_backwoods:the_sub_strata")) {
			return false;
		}
		if (findNearestVerdantEngine(level, entity, FRACTUS_VERDANT_DETECTION_RANGE) != null) {
			return false; // Do not summon if currently guarding a Verdant Engine
		}

		int remSelf = getSummonRemaining(entity);
		int cdSelf = persistentInt(entity, K_SUMMON_COOLDOWN, 0);
		if (remSelf <= 0 || cdSelf > 0) {
			return false;
		}

		System.out.println("[Fractus Sentry] " + entity.getName().getString() + " (ID: " + entity.getId() + ") evaluating summoning ritual at tick " + entity.tickCount + ". Dimension: " + level.dimension().location() + ", Remaining uses: " + remSelf + ", Cooldown: " + cdSelf);

		ResourceKey<Level> dim = level.dimension();
		if (dim == Level.OVERWORLD || dim == Level.NETHER) {
			if (entity.tickCount < SUMMON_MIN_TICK_COUNT) {
				System.out.println("[Fractus Sentry]   Aborting: tickCount (" + entity.tickCount + ") is less than SUMMON_MIN_TICK_COUNT (" + SUMMON_MIN_TICK_COUNT + ")");
				return false;
			}
		}

		AABB alertBox = entity.getBoundingBox().inflate(96.0);
		for (Entity other : level.getEntities(entity, alertBox, e -> isFractus(e))) {
			if (persistentInt(other, K_SUMMON_ROLE, 0) > 0) {
				System.out.println("[Fractus Sentry]   Aborting: Another Fractus nearby (ID: " + other.getId() + ") is already summoning (role > 0).");
				return false;
			}
		}

		AABB searchBox = entity.getBoundingBox().inflate(96.0);
		java.util.List<Entity> partners = new java.util.ArrayList<>();
		partners.add(entity);

		for (Entity other : level.getEntities(entity, searchBox, e -> isFractus(e))) {
			if (other != entity) {
				if (findNearestVerdantEngine(level, other, FRACTUS_VERDANT_DETECTION_RANGE) != null) {
					System.out.println("[Fractus Sentry]   Excluding close-by Fractus partner Candidate (ID: " + other.getId() + "): currently guarding a Verdant Engine.");
					continue;
				}
				if (persistentInt(other, K_SUMMON_ROLE, 0) == 0) {
					int otherRem = getSummonRemaining(other);
					int otherCD = persistentInt(other, K_SUMMON_COOLDOWN, 0);
					if (otherRem > 0 && otherCD <= 0) {
						partners.add(other);
						if (partners.size() >= 6) {
							break;
						}
					} else {
						System.out.println("[Fractus Sentry]   Excluding close-by Fractus partner Candidate (ID: " + other.getId() + "): on cooldown (" + otherCD + ") or no remaining uses (" + otherRem + ").");
					}
				} else {
					System.out.println("[Fractus Sentry]   Excluding close-by Fractus partner Candidate (ID: " + other.getId() + "): already in a ritual (role > 0).");
				}
			}
		}

		System.out.println("[Fractus Sentry]   Found " + partners.size() + " total valid summoning partners (including leader). Range: 96 blocks.");

		if (partners.size() >= 2) {
			int N = partners.size();
			int baseTicks = SUMMON_BASE_DURATION_OFFSET - (N * SUMMON_DURATION_REDUCTION_PER_PARTICIPANT);
			System.out.println("[Fractus Sentry]   SUCCESS! Starting summoning ritual with " + N + " participants for " + baseTicks + " ticks!");

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < partners.size(); i++) {
				if (i > 0) sb.append(",");
				sb.append(partners.get(i).getUUID().toString());
			}
			String uuidsList = sb.toString();

			double sumX = 0, sumY = 0, sumZ = 0;
			for (Entity partner : partners) {
				sumX += partner.getX();
				sumZ += partner.getZ();

				double gy = persistentDouble(partner, K_HOME_Y, partner.getY());
				sumY += gy;
			}
			double rawCenterX = sumX / (double) N;
			double avgY = sumY / (double) N;
			double rawCenterZ = sumZ / (double) N;

			Vec3 safeCenter = findSafeSummonCenter(level, rawCenterX, avgY, rawCenterZ, entity);
			double centerX = safeCenter.x;
			double centerY = safeCenter.y;
			double centerZ = safeCenter.z;

			level.playSound(
				null,
				centerX,
				centerY,
				centerZ,
				SoundEvents.EVOKER_PREPARE_SUMMON,
				SoundSource.HOSTILE,
				8.0f,
				0.7f
			);
			level.playSound(
				null,
				entity.getX(),
				entity.getY(),
				entity.getZ(),
				SoundEvents.EVOKER_PREPARE_SUMMON,
				SoundSource.HOSTILE,
				8.0f,
				0.7f
			);

			for (int i = 0; i < partners.size(); i++) {
				Entity p = partners.get(i);
				p.getPersistentData().putInt(K_SUMMON_ROLE, (i == 0) ? 1 : 2);
				p.getPersistentData().putInt(K_SUMMON_TICKS, baseTicks);
				p.getPersistentData().putInt(K_SUMMON_START_TICKS, baseTicks);
				p.getPersistentData().putDouble(K_SUMMON_CENTER_X, centerX);
				p.getPersistentData().putDouble(K_SUMMON_CENTER_Y, centerY);
				p.getPersistentData().putDouble(K_SUMMON_CENTER_Z, centerZ);
				p.getPersistentData().putInt("fractus_summon_last_participants", N);
				p.getPersistentData().putString("fractus_summon_participant_uuids", uuidsList);

				p.setDeltaMovement(Vec3.ZERO);
				if (p instanceof Mob m) {
					m.setTarget(null);
				}
				resetLaser(p);
				stopFractusActiveSounds(level, p);
			}
			return true;
		} else {
			System.out.println("[Fractus Sentry]   Aborting: Not enough valid partners nearby (min required: 2, currently: " + partners.size() + "). Spawning additional ones closer than 96 blocks might help!");
			return false;
		}
	}

	private static Vec3 findSafeSummonCenter(ServerLevel level, double x, double avgY, double z, Entity initiator) {
		int startYFloor = Mth.floor(avgY);

		for (int r = 0; r <= 8; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (r == 0 || Math.abs(dx) == r || Math.abs(dz) == r) {
						double sampleX = x + dx;
						double sampleZ = z + dz;

						Double safeY = findVerticalAirGap(level, sampleX, avgY, sampleZ, 4);
						if (safeY != null) {
							return new Vec3(sampleX, safeY, sampleZ);
						}
					}
				}
			}
		}

		Double initiatorSafeY = findVerticalAirGap(level, initiator.getX(), initiator.getY(), initiator.getZ(), 4);
		if (initiatorSafeY != null) {
			return new Vec3(initiator.getX(), initiatorSafeY, initiator.getZ());
		}
		return initiator.position().add(0.0, 1.5, 0.0);
	}

	private static Double findVerticalAirGap(ServerLevel level, double x, double startY, double z, int minHeight) {
		BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
		int startYFloor = Mth.floor(startY);

		int chunkX = Mth.floor(x) >> 4;
		int chunkZ = Mth.floor(z) >> 4;
		if (!level.hasChunk(chunkX, chunkZ)) {
			return null;
		}

		int bestGapY = -1;
		int bestGapSize = 0;

		int minSearchY = Math.max(level.getMinBuildHeight() + 2, startYFloor - 20);
		int maxSearchY = Math.min(level.getMaxBuildHeight() - 5, startYFloor + 28);

		int currentGapStart = -1;
		int currentGapSize = 0;

		for (int y = minSearchY; y <= maxSearchY; y++) {
			mut.set(Mth.floor(x), y, Mth.floor(z));
			BlockState state = level.getBlockState(mut);
			boolean passable = isPassableBlockForSummoning(level, mut, state);

			if (passable) {
				if (currentGapStart == -1) {
					currentGapStart = y;
				}
				currentGapSize++;
			} else {
				if (currentGapSize >= minHeight) {
					if (bestGapSize == 0 || Math.abs((currentGapStart + currentGapSize / 2) - startYFloor) < Math.abs((bestGapY + bestGapSize / 2) - startYFloor)) {
						bestGapY = currentGapStart;
						bestGapSize = currentGapSize;
					}
				}
				currentGapStart = -1;
				currentGapSize = 0;
			}
		}

		if (currentGapSize >= minHeight) {
			if (bestGapSize == 0 || Math.abs((currentGapStart + currentGapSize / 2) - startYFloor) < Math.abs((bestGapY + bestGapSize / 2) - startYFloor)) {
				bestGapY = currentGapStart;
				bestGapSize = currentGapSize;
			}
		}

		if (bestGapSize > 0) {
			if (bestGapSize > 12) {
				return (double) bestGapY + 6.0;
			} else {
				return (double) bestGapY + (double) bestGapSize / 2.0;
			}
		}
		return null;
	}

	private static boolean isPassableBlockForSummoning(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.isAir()) {
			return true;
		}
		if (!state.getFluidState().isEmpty()) {
			return false;
		}
		try {
			if (!state.getCollisionShape(level, pos).isEmpty()) {
				return false;
			}
		} catch (Exception e) {
			if (state.isCollisionShapeFullBlock(level, pos)) {
				return false;
			}
		}
		return true;
	}

	private static boolean handleSummoningRitual(ServerLevel level, Entity entity) {
		if (isBurstSequenceActive(entity)) {
			setSummoningState(entity, false);
			return false;
		}
		int role = persistentInt(entity, K_SUMMON_ROLE, 0);
		if (role == 0) {
			setSummoningState(entity, false);
			return false;
		}

		setSummoningState(entity, true);

		if (entity instanceof Mob mob) {
			if (mob.getTarget() != null) {
				mob.setTarget(null);
			}
			mob.getNavigation().stop();
		}

		int ticks = persistentInt(entity, K_SUMMON_TICKS, 0);
		if (ticks <= 0) {
			entity.getPersistentData().putInt(K_SUMMON_ROLE, 0);
			return false;
		}

		double centerX = persistentDouble(entity, K_SUMMON_CENTER_X, 0.0);
		double centerY = persistentDouble(entity, K_SUMMON_CENTER_Y, 0.0);
		double centerZ = persistentDouble(entity, K_SUMMON_CENTER_Z, 0.0);
		Vec3 center = new Vec3(centerX, centerY, centerZ);

		String uuidsStr = entity.getPersistentData().getString("fractus_summon_participant_uuids");
		java.util.List<Entity> participants = new java.util.ArrayList<>();
		if (!uuidsStr.isEmpty()) {
			for (String uuidS : uuidsStr.split(",")) {
				try {
					java.util.UUID targetUuid = java.util.UUID.fromString(uuidS.trim());
					Entity member = level.getEntity(targetUuid);
					if (member != null && member.isAlive() && isFractus(member)) {
						if (!participants.contains(member)) {
							participants.add(member);
						}
					}
				} catch (Exception ignored) {}
			}
		}
		if (!participants.contains(entity)) {
			participants.add(entity);
		}

		participants.sort(java.util.Comparator.comparingInt(Entity::getId));
		int N = Math.max(1, participants.size());

		if (N < 2) {
			System.out.println("[Fractus Sentry] Summoning ritual cancelled: under 2 participants remaining (only " + N + " left).");
			entity.getPersistentData().putInt(K_SUMMON_ROLE, 0);
			entity.getPersistentData().putInt(K_SUMMON_TICKS, 0);
			resetLaser(entity);
			return true;
		}

		int lastN = persistentInt(entity, "fractus_summon_last_participants", N);
		int startTicks = persistentInt(entity, K_SUMMON_START_TICKS, 240);
		if (startTicks <= 0) {
			startTicks = 240;
		}

		if (N < lastN) {
			int diff = lastN - N;
			int increase = diff * ADAPTIVE_SUMMON_DURATION_INCREASE_PER_DEATH;
			ticks += increase;
			startTicks += increase;
			entity.getPersistentData().putInt(K_SUMMON_TICKS, ticks);
			entity.getPersistentData().putInt(K_SUMMON_START_TICKS, startTicks);
			System.out.println("[Fractus Sentry] " + entity.getName().getString() + " detected " + diff + " participant(s) died! Increasing summon duration by " + increase + " ticks. New remaining: " + ticks);
		}
		entity.getPersistentData().putInt("fractus_summon_last_participants", N);

		int index = participants.indexOf(entity);
		if (index < 0) index = 0;

		double circleRadius = 8.0;
		double elapsed = (double) startTicks - ticks;
		double rotSpeed = 0.015;
		double angle = index * (Math.PI * 2.0 / N) + (elapsed * rotSpeed);

		double wave = Math.sin((elapsed * 0.05) + index) * 0.45;
		double targetX = centerX + Math.cos(angle) * circleRadius;
		double targetY = centerY + wave;
		double targetZ = centerZ + Math.sin(angle) * circleRadius;

		moveToward(entity, new Vec3(targetX, targetY, targetZ), IDLE_MAX_SPEED * 1.5);
		entity.lookAt(EntityAnchorArgument.Anchor.EYES, center);
		if (entity instanceof LivingEntity living) {
			living.yHeadRot = living.getYRot();
			living.yBodyRot = living.getYRot();
		}
		if (entity instanceof Mob mob) {
			mob.getLookControl().setLookAt(center.x, center.y, center.z, 30.0F, 30.0F);
		}

		if (entity.tickCount % 25 == 0) {
			if (shouldUseVanillaSounds(entity)) {
				level.playSound(
					null,
					entity.getX(),
					entity.getY(),
					entity.getZ(),
					SoundEvents.BEACON_POWER_SELECT,
					SoundSource.HOSTILE,
					12.0f,
					0.75f
				);
				level.playSound(
					null,
					entity.getX(),
					entity.getY(),
					entity.getZ(),
					SoundEvents.BEACON_ACTIVATE,
					SoundSource.HOSTILE,
					10.0f,
					0.65f
				);
			} else {
				net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(FRACTUS_LASER_SOUND);
				if (sound != null) {
					level.playSound(
						null,
						entity.getX(),
						entity.getY(),
						entity.getZ(),
						sound,
						SoundSource.HOSTILE,
						3.0f,
						0.9f
					);
				}
			}
		}

		if (ticks == 1) {
			int remVal = getSummonRemaining(entity);
			if (remVal > 0) {
				entity.getPersistentData().putInt(K_SUMMON_REMAINING, remVal - 1);
			}
			entity.getPersistentData().putInt(K_SUMMON_COOLDOWN, SUMMON_INTERVAL_COOLDOWN_TICKS);
			System.out.println("[Fractus Sentry] " + entity.getName().getString() + " (ID: " + entity.getId() + ") completed summoning ritual. Cooldown set: " + SUMMON_INTERVAL_COOLDOWN_TICKS + " ticks. Remaining uses: " + (remVal - 1));
		}

		if (role == 1) {
			java.util.Random rnd = new java.util.Random();

			if (ticks == 1) {
				String chosenRegistry = (rnd.nextDouble() < SUMMON_ROT_CHANCE) ? "the_backwoods:rot" : "the_backwoods:fractus_prime";
				System.out.println("[Fractus Sentry] Spawning summoned entity! Target: " + chosenRegistry);

				EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(chosenRegistry));

				if (entityType != null) {
					Entity summoned = entityType.create(level);
					if (summoned != null) {
						Vec3 safePos = findSafeSummonCenter(level, centerX, centerY + 0.5, centerZ, entity);
						summoned.teleportTo(safePos.x, safePos.y, safePos.z);
						summoned.getPersistentData().putInt("fractus_activation_ticks", 100);
						summoned.getPersistentData().putBoolean("fractus_activation_initialized", true);
						summoned.setYRot(rnd.nextFloat() * 360.0F);
						summoned.setXRot(0.0F);
						if (summoned instanceof Mob mob) {
							mob.setYHeadRot(summoned.getYRot());
							mob.setYBodyRot(summoned.getYRot());
						}
						boolean added = level.addFreshEntity(summoned);
						System.out.println("[Fractus Sentry] Successfully created and added summoned entity to world: " + added);
					} else {
						System.out.println("[Fractus Sentry] Failed to create summoned entity instance from EntityType.");
					}
				} else {
					System.out.println("[Fractus Sentry] Critical Error: Unable to resolve EntityType for any of the registry targets.");
				}

				level.playSound(
					null,
					centerX,
					centerY,
					centerZ,
					SoundEvents.END_PORTAL_SPAWN,
					SoundSource.HOSTILE,
					8.0f,
					0.55f
				);
				level.playSound(
					null,
					centerX,
					centerY,
					centerZ,
					SoundEvents.END_PORTAL_FRAME_FILL,
					SoundSource.HOSTILE,
					8.0f,
					0.8f
				);
				level.playSound(
					null,
					centerX,
					centerY,
					centerZ,
					SoundEvents.BEACON_ACTIVATE,
					SoundSource.HOSTILE,
					8.0f,
					0.7f
				);
				level.playSound(
					null,
					entity.getX(),
					entity.getY(),
					entity.getZ(),
					SoundEvents.END_PORTAL_SPAWN,
					SoundSource.HOSTILE,
					8.0f,
					0.55f
				);

				if (net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
					level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centerX, centerY, centerZ, 12, 0.5, 0.5, 0.5, 0.1);
					level.sendParticles(ParticleTypes.FLASH, centerX, centerY, centerZ, 6, 0.2, 0.2, 0.2, 0.2);
					level.sendParticles(getSummonLaserParticle(), centerX, centerY, centerZ, 200, 1.5, 1.5, 1.5, 0.25);
				}
			}
		}

		entity.getPersistentData().putInt(K_SUMMON_TICKS, ticks - 1);

		if (ticks - 1 <= 0) {
			entity.getPersistentData().putInt(K_SUMMON_ROLE, 0);
			entity.getPersistentData().putInt(K_COOLDOWN, 20);
			entity.getPersistentData().putInt(K_BURST_COOLDOWN, Math.max(persistentInt(entity, K_BURST_COOLDOWN, 0), 20));
		}

		return true;
	}

	private static void spawnSummonLaser(ServerLevel level, Vec3 start, Vec3 end, double elapsed, double startTicks) {
		if (!net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			return;
		}
		Vec3 line = end.subtract(start);
		double length = line.length();
		if (length < 0.01) {
			return;
		}
		Vec3 direction = line.normalize();
		net.minecraft.core.particles.ParticleOptions particle = getSummonLaserParticle();

		double spacing = 0.35 + (0.5 * (1.0 - (elapsed / startTicks)));
		double pulse = level.getGameTime() * 0.22;

		for (double d = 0.0; d <= length; d += spacing) {
			double flicker = 0.5 + 0.5 * Math.sin(d * 2.2 + pulse);
			if (flicker < 0.28) {
				continue;
			}
			Vec3 pos = start.add(direction.scale(d));

			double angle = elapsed * 0.15 + d * 0.45;
			double spiralRadius = 0.18 * (1.0 - (elapsed / startTicks));
			Vec3 up = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
			Vec3 side = direction.cross(up).normalize();
			Vec3 verticalSide = direction.cross(side).normalize();
			Vec3 spiralPos = pos.add(side.scale(Math.cos(angle) * spiralRadius)).add(verticalSide.scale(Math.sin(angle) * spiralRadius));

			level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.012, 0.012, 0.012, 0.0);
			level.sendParticles(particle, spiralPos.x, spiralPos.y, spiralPos.z, 1, 0.005, 0.005, 0.005, 0.0);
		}
	}

	private record LaserHit(Vec3 location, Entity entity, BlockPos blockPos, Direction blockFace) {
	}
}
// 1.21.1