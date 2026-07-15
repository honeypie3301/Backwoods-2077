package net.mcreator.thebackwoods.procedures;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
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

@EventBusSubscriber
public class FractusPrimeOnEntityTickUpdateProcedure {
	// Colossal 2x Scaled Fractus Prime (Orbital Laser Cannon Variant) NeoForge AI Update Procedure.
	// Generated dynamically by Fractus AI Customizer app.

	private static final ResourceLocation FRACTUS_ID = ResourceLocation.parse("the_backwoods:fractus_prime");
	private static final ResourceLocation FRACTUS_LASER_SOUND = ResourceLocation.parse("the_backwoods:fractus_laser");
	private static final ResourceLocation FRACTUS_LASER_BURST_SOUND = ResourceLocation.parse("the_backwoods:fractus_prime_laser_burst");
	private static final ResourceLocation FRACTUS_LASER_SPHERE_BURST_SOUND = ResourceLocation.parse("the_backwoods:fractus_prime_laser_sphere_burst");
	private static final ResourceLocation FRACTUS_ANGER_SOUND = ResourceLocation.parse("the_backwoods:fractus_anger");
	private static final ResourceKey<Level> SUB_STRATA_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:backwoods"));
	private static final TagKey<EntityType<?>> WOODBOUND_ENTITIES_TAG = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities"));

	// Targeting
	private static final double DETECTION_RANGE = 48.0;
	private static final double LASER_RANGE = 64.0;
	private static final double ANGRY_LASER_RANGE = 80.0;
	private static final double AIM_LOCK_TURN_RATE = 0.0900;
	private static final double ANGRY_AIM_LOCK_TURN_RATE = 0.1200;
	private static final double FIRING_AIM_TURN_RATE = 0.0350;
	private static final double ANGRY_FIRING_AIM_TURN_RATE = 0.0550;
	private static final double TARGET_LEAD_TICKS = 2.25;
	private static final double ANGRY_TARGET_LEAD_TICKS = 3.00;
	private static final double BURST_LASER_RANGE = 128.0;
	private static final double BURST_LASER_RADIUS = 4.00;
	private static final double BURST_LASER_TURN_RATE = 0.0050;
	private static final double BURST_LASER_SPIRAL_RADIUS = 2.40;
	private static final double BURST_LASER_SPIRAL_SPACING = 0.280;

	// Home / leash
	private static final double HOME_LEASH_RANGE = 42.0;

	// Drone movement
	private static final double PREFERRED_COMBAT_RANGE = 16.0;
	private static final double ANGRY_PREFERRED_COMBAT_RANGE = 20.0;
	private static final double TOO_CLOSE_RANGE = 6.0;
	private static final double HOVER_HEIGHT = 5.50;
	private static final double ANGRY_HOVER_HEIGHT = 7.50;
	private static final double ESCAPED_CONTAINMENT_HOVER_BONUS = 2.25;
	private static final double IDLE_HOVER_HEIGHT = 2.25;
	private static final double SUB_STRATA_PLAYER_COMBAT_HOVER_REDUCTION = 1.5;
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
	private static final int BURST_TOTAL_TICKS = 610;
	private static final int BURST_FIRE_PEAK_TICK = 125;
	private static final int BURST_CORE_END_TICK = 380;
	private static final int BURST_COOLDOWN_TICKS = 350;
	private static final int STRONG_TARGET_BURST_COOLDOWN_TICKS = 180;

	// Configurable sphere specs
	private static final double SPHERE_START_RADIUS = 1.5;
	private static final double SPHERE_MAX_BUILDUP_RADIUS = 4.5;
	private static final double SPHERE_MAX_RELEASE_RADIUS = 17.0;
	private static final float SPHERE_VAPORIZE_MAX_HARDNESS = 100.0f;
	private static final int SPHERE_COOLDOWN_TICKS = 320;
	private static final int STRONG_TARGET_SPHERE_COOLDOWN_TICKS = 160;
	private static final double BURST_KNOCKBACK_HORIZONTAL = 2.40;
	private static final double BURST_KNOCKBACK_VERTICAL = 0.55;
	private static final float STRONG_TARGET_HEALTH_THRESHOLD = 80.0f;
	private static final float STRONG_TARGET_CURRENT_HEALTH_THRESHOLD = 60.0f;
	private static final int STRONG_TARGET_BURST_CHECK_INTERVAL = 80;
	private static final double STRONG_TARGET_BURST_CHANCE = 0.380;

	// Player-Proximity Sphere Burst Boost configurations
	private static final double PROXIMITY_PLAYER_INNER_RADIUS = 6.0;
	private static final double PLAYER_INNER_CHANCE_BOOST = 2.3;
	private static final int PLAYER_INNER_INTERVAL_DIVISOR = 5;
	private static final double PROXIMITY_PLAYER_OUTER_RADIUS = 10.0;
	private static final double PLAYER_OUTER_CHANCE_BOOST = 1.7;
	private static final int PLAYER_OUTER_INTERVAL_DIVISOR = 3;
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
	private static final double IDLE_PATROL_REACH_DISTANCE = 0.850;
	private static final int IDLE_PATROL_WAIT_TICKS = 14;
	private static final int IDLE_PATROL_POINTS = 3;
	private static final double HARMFUL_BLOCK_AVOID_RADIUS = 2.25;
	private static final double HARMFUL_BLOCK_ESCAPE_SPEED = 0.620;
	private static final int TARGET_LOS_CANDIDATE_LIMIT = 3;
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

	// Telekinesis Configs
	private static final int TELEKINESIS_LIFT_ONLY_TICKS = 40; // Ticks where victim is pulled up before laser cooldown or burst buildup begins
	private static final double TELEKINESIS_HOLD_DISTANCE = 14.0; // Distance in front of Fractus Prime to hold target
	private static final double TELEKINESIS_HOLD_Y_OFFSET = -1.5; // Y offset relative to Fractus Prime to hold target
	private static final double TELEKINESIS_PULL_SPEED_MAX = 0.50; // Maximum speed of the telekinetic pull
	private static final double TELEKINESIS_PULL_FACTOR = 0.25; // Pull interpolation factor
	private static final double TELEKINESIS_BASE_CHANCE = 0.20; // Base chance of using telekinesis
	private static final double TELEKINESIS_MAX_CHANCE = 0.85; // Maximum chance of using telekinesis (increases with target max health)

	// Anger
	private static final float ANGER_HEALTH_THRESHOLD = 100.0f;
	private static final int ANGER_SOUND_INTERVAL_TICKS = 90;

	// Laser timing
	private static final int CHARGE_TICKS = 28;
	private static final int FIRE_TICKS = 123;
	private static final int COOLDOWN_TICKS = 46;

	// Laser damage
	private static final int DAMAGE_INTERVAL_TICKS = 10;
	private static final int ANGRY_DAMAGE_INTERVAL_TICKS = 6;
	private static final float LASER_DAMAGE = 21.3f;
	private static final float ANGRY_LASER_DAMAGE = 55.0f;
	private static final float BURST_LASER_DAMAGE = 180.0f;
	private static final float SPHERE_BURST_DAMAGE = 175.0f;
	private static final float ANGRY_SPHERE_BURST_DAMAGE = 225.0f;

	// Weak block destruction.
	private static final float WEAK_BLOCK_MAX_HARDNESS = 4.50f;
	private static final float ANGRY_WEAK_BLOCK_MAX_HARDNESS = 15.00f;
	private static final float BURST_BLOCK_MAX_HARDNESS = 100.00f;
	private static final double BLOCK_BREAK_STEP = 0.250;
	private static final double BURST_BLOCK_BREAK_STEP = 0.750;

	// Dense redstone dust laser visual.
	private static final double LASER_PARTICLE_SPACING = 0.140;
	private static final double ANGRY_LASER_PARTICLE_SPACING = 0.055;
	private static final double CHARGE_PARTICLE_SPACING = 0.780;
	private static final double ANGRY_CHARGE_PARTICLE_SPACING = 0.500;
	private static final double LASER_PARTICLE_JITTER = 0.018;
	private static final double ANGRY_LASER_PARTICLE_JITTER = 0.028;
	private static final double CHARGE_PARTICLE_JITTER = 0.035;
	private static final double FIRING_SPIRAL_RADIUS = 0.193;
	private static final double FIRE_START_RING_RADIUS = 2.160;
	private static final int LASER_ENTITY_FIRE_TICKS = 100;
	private static final double LASER_TRAIL_FIRE_STEP = 1.350;

	// 1.21.1 neoforge - custom white, red, and orange Registry particles with vanilla dust fallbacks (lazy loaded).
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
			potentialIds = new String[]{
				"the_backwoods:fractus_laser_particle_burst",
				"the_backwoods:fractus_laser_particle_red",
				"the_backwoods:fractus_laser_red",
				"the_backwoods:red_fractus_laser_particle",
				"the_backwoods:fractus_laser_particle"
			};
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
		} else { // white
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

	// Persistent data keys.
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
	private static final String K_SPHERE_TIMER = "fractus_sphere_timer";
	private static final String K_SPHERE_COOLDOWN = "fractus_sphere_cooldown";
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

	// 0 = idle, 1 = tracking, 2 = charging, 3 = firing, 4 = cooldown.
	private static final String K_LASER_STATE = "fractus_laser_state";
	private static final int GLOWING_CORE_LIGHT_LEVEL = 0; // Set 0 to disable, or 1 to 15 to configure light source intensity


	public static void execute() {
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
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();

		if (entity == null || !isFractus(entity) || !(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		stopFractusActiveSounds(serverLevel, entity);
		resetLaser(entity);
		spawnDeathBurst(serverLevel, entity);
		cleanupLightSource(serverLevel, entity);
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
		Entity entity = event.getEntity();

		if (entity == null || !isFractus(entity) || !(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		stopFractusActiveSounds(serverLevel, entity);
		cleanupLightSource(serverLevel, entity);
	}

	@SubscribeEvent
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		LivingEntity entity = event.getEntity();

		if (entity == null || !isFractus(entity)) {
			return;
		}

		// Anti-Warden defense: scale down all Warden & Sonic Boom damage to ensure survival
		if (event.getSource().is(DamageTypes.SONIC_BOOM)) {
			event.setAmount(event.getAmount() * 0.55f);
		}
		if (isWarden(event.getSource().getEntity())) {
			event.setAmount(event.getAmount() * 0.70f);
		}

		boolean angry = isAngry(entity);
		boolean projectileDamage = isProjectileDamage(event.getSource());

		// Telekinesis target escape mechanic: release target if they deal damage to Prime during the hold phase
		if (persistentBoolean(entity, "telekinesis_active", false)) {
			Entity attacker = event.getSource().getEntity();
			if (attacker != null) {
				String uuidStr = persistentString(entity, "telekinesis_target_uuid", "");
				if (!uuidStr.isEmpty() && attacker.getUUID().toString().equals(uuidStr)) {
					int hitsLeft = persistentInt(entity, "telekinesis_hits_left", 0) - 1;
					if (hitsLeft <= 0) {
						entity.getPersistentData().putBoolean("telekinesis_active", false);
						entity.getPersistentData().putString("telekinesis_target_uuid", "");
						entity.getPersistentData().putInt("telekinesis_hits_left", 0);
						if (entity.level() instanceof ServerLevel serverLevel) {
							serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.5f, 0.8f);
							serverLevel.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
							spawnChargeInterruptParticles(serverLevel, entity);
						}
					} else {
						entity.getPersistentData().putInt("telekinesis_hits_left", hitsLeft);
						if (entity.level() instanceof ServerLevel serverLevel) {
							serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.5f, 1.8f);
						}
					}
				}
			}
		}

		markRetaliationTarget(entity, event.getSource());

		boolean vulnerable = persistentInt(entity, K_VULNERABLE, 0) > 0;

		if (projectileDamage) {
			if (vulnerable) {
				// Let projectile damage through if vulnerable (do not dodge, do not cancel)
				return;
			}
			if (tryDodgeProjectile(entity, event.getSource())) {
				event.setCanceled(true);
				return;
			}
			// Failed to dodge -> takes projectile damage instead of shields canceling it
			return;
		}

		if (!angry && (persistentInt(entity, K_CHARGE, 0) > 0 || persistentInt(entity, K_FIRE, 0) > 0)) {
			entity.getPersistentData().putInt(K_CHARGE, 0);
			entity.getPersistentData().putInt(K_FIRE, 0);
			entity.getPersistentData().putInt(K_LASER_STATE, 1);

			if (entity.level() instanceof ServerLevel serverLevel) {
				stopFractusLaserSound(serverLevel, entity);
				spawnChargeInterruptParticles(serverLevel, entity);
			}
		}

		if (angry || !projectileDamage) {
			return;
		}

		event.setCanceled(true);

		if (entity.level() instanceof ServerLevel serverLevel) {
			spawnShieldParticles(serverLevel, entity);
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !isFractus(entity)) {
			return;
		}

		entity.setNoGravity(true);
		entity.fallDistance = 0.0f;
		entity.setRemainingFireTicks(0);
		ensureHome(entity, x, y, z);

		if (!(world instanceof ServerLevel serverLevel)) {
			return;
		}

		if (entity instanceof LivingEntity living && !living.isAlive()) {
			stopFractusActiveSounds(serverLevel, entity);
			return;
		}

		updateLightSource(serverLevel, entity);
		handleEcholocation(serverLevel, entity);

		int vulnerabilityTicks = tickStoredTimer(entity, K_VULNERABLE);

		if (entity instanceof LivingEntity livingEntity) {
			if (vulnerabilityTicks > 0) {
				livingEntity.removeEffect(MobEffects.REGENERATION);
				spawnVulnerabilityParticles(serverLevel, entity, vulnerabilityTicks);
			} else {
				applyInfiniteRegeneration(livingEntity);
				// Passive direct speed regeneration to make sure it functions properly and heals fast
				if (livingEntity.tickCount % 20 == 0 && livingEntity.getHealth() < livingEntity.getMaxHealth()) {
					livingEntity.heal(2.0f); // 1 Heart per second
				}
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
		int sphereTicks = tickStoredTimer(entity, K_SPHERE_TIMER);
		int sphereCooldown = tickStoredTimer(entity, K_SPHERE_COOLDOWN);
		int previousLaserState = persistentInt(entity, K_LASER_STATE, 0);
		LivingEntity target = findTarget(serverLevel, entity, x, y, z);
		double currentLaserRange = angry ? ANGRY_LASER_RANGE : LASER_RANGE;
		boolean canSeeTarget = target != null && hasClearShot(serverLevel, entity, target, currentLaserRange);
		boolean canDrill = target != null && canLaserDrillThrough(serverLevel, entity, target, currentLaserRange, angry);

		if (isEscapedContainmentDimension(entity)) {
			alertNearbyMobsAndPanicPassives(serverLevel, entity);
		}

		if (cooldown > 0) {
			stopFractusLaserSound(serverLevel, entity);
		}

		boolean triggerBurst = sphereTicks <= 0 && shouldStartBurstAttack(entity, target, burstTicks, burstCooldown);
		boolean triggerSphere = false;
		if (!triggerBurst && sphereTicks <= 0 && burstTicks <= 0) {
			triggerSphere = shouldStartSphereAttack(entity, target, sphereTicks, sphereCooldown, burstTicks);
		} else if (triggerBurst) {
			if (sphereCooldown <= 0 && entity.getRandom().nextDouble() < 0.50) {
				double dist = target != null ? entity.distanceTo(target) : 0.0;
				if (target != null && dist <= 15.0) {
					triggerBurst = false;
					triggerSphere = true;
				}
			}
		}

		if (triggerBurst) {
			// telekinesis initialization: only apply to strong targets, strong flying targets, or players
			boolean useTelekinesis = false;
			if (target != null) {
				boolean isStrong = isStrongTarget(target) || isStrongFlyingTarget(target) || target instanceof Player;
				if (isStrong && canSeeTarget) {
					float hp = target.getMaxHealth();
					double baseChance = isStrongFlyingTarget(target) ? 0.50 : TELEKINESIS_BASE_CHANCE;
					double maxChance = isStrongFlyingTarget(target) ? 0.95 : TELEKINESIS_MAX_CHANCE;
					double telekinesisChance = Mth.clamp(baseChance + 0.65 * (hp / 100.0f), baseChance, maxChance);
					useTelekinesis = entity.getRandom().nextDouble() < telekinesisChance;
				}
				entity.getPersistentData().putBoolean("telekinesis_active", useTelekinesis);
				entity.getPersistentData().putString("telekinesis_target_uuid", target.getUUID().toString());
				if (useTelekinesis) {
					int hits;
					if (entity instanceof LivingEntity living && living.getHealth() <= 100.0f) {
						hits = 8 + entity.getRandom().nextInt(17); // 8 to 24 inclusive
					} else {
						hits = 1 + entity.getRandom().nextInt(6); // 1 to 6 inclusive
					}
					entity.getPersistentData().putInt("telekinesis_hits_left", hits);
				}
			} else {
				entity.getPersistentData().putBoolean("telekinesis_active", false);
				entity.getPersistentData().putString("telekinesis_target_uuid", "");
				entity.getPersistentData().putInt("telekinesis_hits_left", 0);
			}

			burstTicks = BURST_TOTAL_TICKS + (useTelekinesis ? TELEKINESIS_LIFT_ONLY_TICKS : 0);
			entity.getPersistentData().putInt(K_BURST_TIMER, burstTicks - 1);
			entity.getPersistentData().putInt(K_BURST_COOLDOWN, target != null ? burstCooldownTicksForTarget(target) : BURST_COOLDOWN_TICKS);
			stopFractusLaserSound(serverLevel, entity);
			entity.getPersistentData().putInt(K_CHARGE, 0);
			entity.getPersistentData().putInt(K_FIRE, 0);
			entity.getPersistentData().putInt(K_LASER_STATE, 0);
		} else if (triggerSphere) {
			sphereTicks = 270;
			entity.getPersistentData().putInt(K_SPHERE_TIMER, sphereTicks - 1);
			entity.getPersistentData().putInt(K_SPHERE_COOLDOWN, target != null ? sphereCooldownTicksForTarget(target) : SPHERE_COOLDOWN_TICKS);
			stopFractusLaserSound(serverLevel, entity);
			entity.getPersistentData().putInt(K_CHARGE, 0);
			entity.getPersistentData().putInt(K_FIRE, 0);
			entity.getPersistentData().putInt(K_LASER_STATE, 0);
		}

		if (burstTicks > 0) {
			boolean hasTelekinesis = persistentBoolean(entity, "telekinesis_active", false);
			LivingEntity combatTarget = null;
			if (hasTelekinesis) {
				String uuidStr = persistentString(entity, "telekinesis_target_uuid", "");
				if (!uuidStr.isEmpty()) {
					try {
						java.util.UUID targetUuid = java.util.UUID.fromString(uuidStr);
						Entity found = serverLevel.getEntity(targetUuid);
						if (found instanceof LivingEntity livingFound && livingFound.isAlive()) {
							combatTarget = livingFound;
						} else {
							// Target is dead or gone! Turn off telekinesis
							entity.getPersistentData().putBoolean("telekinesis_active", false);
							hasTelekinesis = false;
						}
					} catch (Exception e) {
						entity.getPersistentData().putBoolean("telekinesis_active", false);
						hasTelekinesis = false;
					}
				}
			}

			if (combatTarget == null) {
				combatTarget = target != null ? target : retaliationTarget(entity);
			}

			if (combatTarget != null) {
				faceTarget(entity, combatTarget);
			}

			if (hasTelekinesis && combatTarget != null) {
				int totalDuration = BURST_TOTAL_TICKS + TELEKINESIS_LIFT_ONLY_TICKS;
				int elapsed = totalDuration - burstTicks;
				int coreEndTick = BURST_CORE_END_TICK + TELEKINESIS_LIFT_ONLY_TICKS;

				if (elapsed <= coreEndTick) {
					// Position player horizontally 14 blocks in front of Fractus Prime, at Y level slightly below Fractus Prime
					Vec3 look = entity.getLookAngle();
					Vec3 horizontalLook = new Vec3(look.x, 0, look.z);
					if (horizontalLook.lengthSqr() < 0.001) {
						horizontalLook = new Vec3(1, 0, 0);
					}
					horizontalLook = horizontalLook.normalize();
					Vec3 holdPoint = entity.position().add(horizontalLook.scale(TELEKINESIS_HOLD_DISTANCE));
					double holdY = entity.getY() + TELEKINESIS_HOLD_Y_OFFSET;
					Vec3 adjustedHoldPoint = new Vec3(holdPoint.x, holdY, holdPoint.z);

					Vec3 toHold = adjustedHoldPoint.subtract(combatTarget.position());
					double dist = toHold.length();
					if (dist > 0.1) {
						Vec3 pullVec = toHold.normalize().scale(Math.min(dist * TELEKINESIS_PULL_FACTOR, TELEKINESIS_PULL_SPEED_MAX));
						combatTarget.setDeltaMovement(pullVec.x, pullVec.y, pullVec.z);
						combatTarget.fallDistance = 0.0f;
						combatTarget.hasImpulse = true;
						combatTarget.hurtMarked = true; // Force client velocity packets to sync for players
					} else {
						combatTarget.setDeltaMovement(0, 0.02, 0);
						combatTarget.fallDistance = 0.0f;
						combatTarget.hasImpulse = true;
						combatTarget.hurtMarked = true; // Force client velocity packets to sync for players
					}

					// Cylinder sound & end rod particles
					spawnTelekinesisRay(serverLevel, laserStart(entity), combatTarget.getEyePosition());
					if (entity.tickCount % 40 == 0) {
						serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.2f, 1.0f);
					}
				}
			}

			handleBurstLaser(serverLevel, entity, combatTarget, burstTicks);
			if (combatTarget != null) {
				moveCombat(entity, combatTarget, true, angry, cooldown, angerRetreatTicks);
			} else {
				moveToward(entity, idleHomePoint(entity), IDLE_MAX_SPEED);
				faceMovement(entity);
			}
			entity.getPersistentData().putInt(K_CHARGE, 0);
			entity.getPersistentData().putInt(K_FIRE, 0);
			entity.getPersistentData().putInt(K_LASER_STATE, 0);
			stopFractusLaserSound(serverLevel, entity);
			return;
		}

		if (sphereTicks > 0) {
			if (target != null) {
				faceTarget(entity, target);
			}

			handleLaserSphereBurst(serverLevel, entity, sphereTicks);

			if (target != null) {
				moveCombat(entity, target, true, angry, cooldown, angerRetreatTicks);
			} else {
				moveToward(entity, idleHomePoint(entity), IDLE_MAX_SPEED);
				faceMovement(entity);
			}
			entity.getPersistentData().putInt(K_CHARGE, 0);
			entity.getPersistentData().putInt(K_FIRE, 0);
			entity.getPersistentData().putInt(K_LASER_STATE, 0);
			stopFractusLaserSound(serverLevel, entity);
			return;
		}

		if (target == null) {
			driftIdleScan(entity);
			entity.getPersistentData().putBoolean(K_RETURNING_HOME, false);
			entity.getPersistentData().putInt(K_COVER_WAIT, 0);

			if (handleEscapedDestroyingMode(serverLevel, entity)) {
				resetLaser(entity);
				double speed = isWorldTakeoverDimension(entity) ? MAX_DRONE_SPEED : IDLE_MAX_SPEED;
				moveToward(entity, idleHomePoint(entity), speed);
				return;
			}

			stopFractusLaserSound(serverLevel, entity);
			resetLaser(entity);
			entity.getPersistentData().putInt(K_LASER_STATE, cooldown > 0 ? 4 : 0);
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
			entity.getPersistentData().putInt(K_LASER_STATE, cooldown > 0 ? 4 : 0);
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

		faceTarget(entity, target);

		currentLaserRange = angry ? ANGRY_LASER_RANGE : LASER_RANGE;
		canSeeTarget = target != null && hasClearShot(serverLevel, entity, target, currentLaserRange);
		canDrill = target != null && canLaserDrillThrough(serverLevel, entity, target, currentLaserRange, angry);
		double distance = entity.distanceTo(target);

		if (cooldown > 0) {
			entity.getPersistentData().putInt(K_LASER_STATE, 4);
			entity.getPersistentData().putInt(K_CHARGE, 0);
			entity.getPersistentData().putInt(K_FIRE, 0);
			moveCombat(entity, target, false, angry, cooldown, angerRetreatTicks);
			return;
		}

		if (target == null || distance > currentLaserRange) {
			if (fireTicks > 0) {
				stopFractusLaserSound(serverLevel, entity);
			}

			int coverWaitTicks = !canSeeTarget ? persistentInt(entity, K_COVER_WAIT, 0) + 1 : 0;
			entity.getPersistentData().putInt(K_COVER_WAIT, coverWaitTicks);
			entity.getPersistentData().putInt(K_LASER_STATE, 1);
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
				moveFlankForSight(entity, target, angry);
			} else {
				moveCombat(entity, target, false, angry, cooldown, angerRetreatTicks);
			}

			return;
		}

		entity.getPersistentData().putInt(K_COVER_WAIT, 0);

		if (fireTicks > 0) {
			entity.getPersistentData().putInt(K_LASER_STATE, 3);
			entity.getPersistentData().putInt(K_FIRE, fireTicks - 1);
			moveCombat(entity, target, true, angry, cooldown, angerRetreatTicks);

			if (fireTicks == FIRE_TICKS) {
				playFractusLaserSound(serverLevel, entity);
				spawnFireStartWarning(serverLevel, entity, angry);
			}

			// Support multiple targets when angry or multiple strong entities are present
			java.util.List<LivingEntity> targetList = findMultipleTargets(serverLevel, entity, target, angry, currentLaserRange);

			int numLasers = Math.max(1, targetList.size());
			float damage = angry ? (ANGRY_LASER_DAMAGE / numLasers) : (LASER_DAMAGE / numLasers);
			double spacing = (angry ? ANGRY_LASER_PARTICLE_SPACING : LASER_PARTICLE_SPACING) * numLasers;
			double jitter = angry ? ANGRY_LASER_PARTICLE_JITTER : LASER_PARTICLE_JITTER;

			for (int idx = 0; idx < targetList.size(); idx++) {
				LivingEntity currentTarget = targetList.get(idx);
				Vec3 laserDirection;
				if (idx == 0) {
					laserDirection = updateLaserAim(entity, currentTarget, angry, true);
				} else {
					laserDirection = predictedTargetEyePosition(currentTarget, angry).subtract(laserStart(entity));
					if (laserDirection.lengthSqr() < 0.001) {
						laserDirection = entity.getLookAngle();
					}
					laserDirection = laserDirection.normalize();
				}

				LaserHit firstHit = raycastLaser(serverLevel, entity, laserDirection, currentLaserRange);
				LaserHit finalHit = firstHit;

				if (firstHit.entity() == null) {
					destroyWeakBlocksInLaserPath(serverLevel, entity, laserStart(entity), firstHit.location(), firstHit.blockPos(), angry);
					finalHit = raycastLaser(serverLevel, entity, laserDirection, currentLaserRange);
				}

				spawnLaser(
					serverLevel,
					laserStart(entity),
					finalHit.location(),
					spacing,
					jitter,
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

				if (finalHit.entity() instanceof LivingEntity hitLiving && canDamage(entity, hitLiving) && entity.tickCount % damageInterval == 0) {
					if (entity instanceof LivingEntity attacker) {
						hitLiving.hurt(attacker.damageSources().mobAttack(attacker), damage);
					} else {
						hitLiving.hurt(new DamageSource(world.holderOrThrow(DamageTypes.MOB_ATTACK)), damage);
					}
				}
			}

			if (fireTicks - 1 <= 0) {
				entity.getPersistentData().putInt(K_COOLDOWN, COOLDOWN_TICKS);
				entity.getPersistentData().putInt(K_CHARGE, 0);
				entity.getPersistentData().putInt(K_VULNERABLE, VULNERABILITY_TICKS);
				shuffleCooldownOrbit(entity);
			}

			return;
		}

		if (suppressionTicks <= 0 && isNearbyFractusFiring(serverLevel, entity)) {
			suppressionTicks = SUPPRESSION_TICKS;
			entity.getPersistentData().putInt(K_SUPPRESSION, suppressionTicks);
		}

		if (suppressionTicks > 0) {
			entity.getPersistentData().putInt(K_LASER_STATE, 1);
			entity.getPersistentData().putInt(K_CHARGE, Math.max(0, chargeTicks - 1));
			moveCombat(entity, target, false, angry, cooldown, angerRetreatTicks);
			spawnSuppressionPulse(serverLevel, entity, suppressionTicks);
			return;
		}

		chargeTicks++;
		entity.getPersistentData().putInt(K_CHARGE, chargeTicks);
		entity.getPersistentData().putInt(K_LASER_STATE, 2);
		moveCombat(entity, target, true, angry, cooldown, angerRetreatTicks);

		// Support multiple targets when angry or multiple strong entities are present for charge preview
		java.util.List<LivingEntity> targetList = findMultipleTargets(serverLevel, entity, target, angry, currentLaserRange);

		int numLasers = Math.max(1, targetList.size());
		double chargeProgress = Mth.clamp((double) chargeTicks / CHARGE_TICKS, 0.0, 1.0);
		double baseSpacing = angry
			? Mth.lerp(chargeProgress, ANGRY_CHARGE_PARTICLE_SPACING, 0.16)
			: Mth.lerp(chargeProgress, CHARGE_PARTICLE_SPACING, 0.22);
		double spacing = baseSpacing * numLasers;
		double previewJitter = CHARGE_PARTICLE_JITTER + chargeProgress * 0.018;

		for (int idx = 0; idx < targetList.size(); idx++) {
			LivingEntity currentTarget = targetList.get(idx);
			Vec3 laserDirection;
			if (idx == 0) {
				laserDirection = updateLaserAim(entity, currentTarget, angry, false);
			} else {
				laserDirection = predictedTargetEyePosition(currentTarget, angry).subtract(laserStart(entity));
				if (laserDirection.lengthSqr() < 0.001) {
					laserDirection = entity.getLookAngle();
				}
				laserDirection = laserDirection.normalize();
			}

			LaserHit previewHit = raycastLaser(serverLevel, entity, laserDirection, currentLaserRange);
			spawnLaser(
				serverLevel,
				laserStart(entity),
				previewHit.location(),
				spacing,
				previewJitter,
				false,
				angry
			);

			if (idx == 0) {
				spawnChargeBeamHum(serverLevel, laserStart(entity), previewHit.location(), chargeProgress, angry);
			}
		}
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
		int cx = pos.getX() >> 4;
		int cz = pos.getZ() >> 4;
		net.minecraft.world.level.chunk.LevelChunk levelChunk = level.getChunkSource().getChunkNow(cx, cz);
		if (levelChunk != null) {
			BlockState state = levelChunk.getBlockState(pos);
			if (state.is(Blocks.LIGHT)) {
				boolean waterlogged = state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
				BlockState newState = waterlogged ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
				levelChunk.setBlockState(pos, newState, false);
				level.sendBlockUpdated(pos, state, newState, 3);
			}
		}
	}

	private static boolean isFractus(Entity entity) {
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		return FRACTUS_ID.equals(id) || "fractus_prime".equals(id.getPath());
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
		return entity instanceof LivingEntity living && living.getHealth() <= ANGER_HEALTH_THRESHOLD;
	}

	private static boolean isEscapedContainmentDimension(Entity entity) {
		return !SUB_STRATA_DIMENSION.equals(entity.level().dimension());
	}

	private static boolean shouldStartBurstAttack(Entity entity, LivingEntity target, int burstTicks, int burstCooldown) {
		if (burstTicks > 0 || burstCooldown > 0) {
			entity.getPersistentData().putBoolean("fractus_backing_up_for_burst", false);
			return false;
		}

		if (persistentInt(entity, K_FIRE, 0) > 0 || persistentInt(entity, K_CHARGE, 0) > 0) {
			entity.getPersistentData().putBoolean("fractus_backing_up_for_burst", false);
			return false;
		}

		// 1. COMBAT BURST TRIGGERS (when we have a target)
		if (target != null) {
			double dist = entity.distanceTo(target);
			boolean isBackingUp = persistentBoolean(entity, "fractus_backing_up_for_burst", false);

			if (isBackingUp) {
				int backUpTicks = persistentInt(entity, "fractus_backing_up_ticks", 0);
				if (backUpTicks > 100) {
					entity.getPersistentData().putBoolean("fractus_backing_up_for_burst", false);
					entity.getPersistentData().putInt("fractus_backing_up_ticks", 0);
					return true;
				}
				entity.getPersistentData().putInt("fractus_backing_up_ticks", backUpTicks + 1);

				if (dist >= 16.0) {
					entity.getPersistentData().putBoolean("fractus_backing_up_for_burst", false);
					entity.getPersistentData().putInt("fractus_backing_up_ticks", 0);
					return true;
				}
				return false;
			}

			if (dist > 96.0) {
				return false;
			}

			boolean wantsToFire = false;

			if (entity instanceof LivingEntity livingEntity && livingEntity.getHealth() <= 36.0f) {
				wantsToFire = true;
			}

			// Crowd nearby check - highly favored for crowd control!
			if (!wantsToFire && entity.level() instanceof ServerLevel serverLevel) {
				if (isCrowdNearby(serverLevel, entity)) {
					if (entity.tickCount % 35 == 0 && entity.getRandom().nextDouble() < 0.50) {
						wantsToFire = true;
					}
				}
			}

			if (!wantsToFire && isStrongFlyingTarget(target)) {
				if (entity.tickCount % (STRONG_TARGET_BURST_CHECK_INTERVAL / 2) == 0 && entity.getRandom().nextDouble() < 0.650) {
					wantsToFire = true;
				}
			}

			boolean strongTarget = isStrongTarget(target) || target instanceof Player;

			if (!wantsToFire && strongTarget && entity.tickCount % STRONG_TARGET_BURST_CHECK_INTERVAL == 0 && entity.getRandom().nextDouble() < STRONG_TARGET_BURST_CHANCE) {
				wantsToFire = true;
			}

			if (!wantsToFire) {
				if (isEscapedContainmentDimension(entity)
					&& entity.tickCount % ESCAPED_BURST_CHECK_INTERVAL == 0
					&& entity.getRandom().nextDouble() < ESCAPED_BURST_CHANCE) {
					wantsToFire = true;
				}
			}

			if (wantsToFire) {
				if (dist < 15.0) {
					entity.getPersistentData().putBoolean("fractus_backing_up_for_burst", true);
					entity.getPersistentData().putInt("fractus_backing_up_ticks", 0);
					return false;
				}
				return true;
			}

			return false;
		}

		// 2. DESTROYER / OVERWORLD TAKEOVER BURST TRIGGERS (when target is null, purely for block destruction)
		if (isWorldTakeoverDimension(entity)) {
			// Avoid conflict with regular sweeping block fire ticks
			if (persistentInt(entity, K_DESTROYING_FIRE, 0) > 0) {
				return false;
			}
			// Unleash a giant straight line laser burst on the environment
			if (entity.tickCount % 120 == 0 && entity.getRandom().nextDouble() < 0.12) {
				return true;
			}
		}

		return false;
	}

	private static boolean shouldStartSphereAttack(Entity entity, LivingEntity target, int sphereTicks, int sphereCooldown, int burstTicks) {
		if (sphereTicks > 0 || sphereCooldown > 0 || burstTicks > 0) {
			return false;
		}

		if (persistentInt(entity, K_FIRE, 0) > 0 || persistentInt(entity, K_CHARGE, 0) > 0) {
			return false;
		}

		// 1. COMBAT SPHERE TRIGGERS (when we have a target)
		if (target != null) {
			double dist = entity.distanceTo(target);
			if (dist > 15.0) {
				return false;
			}

			if (entity instanceof LivingEntity livingEntity && livingEntity.getHealth() <= 12.0f) {
				return true;
			}

			// Crowd nearby check - highly favored for crowd control!
			if (entity.level() instanceof ServerLevel serverLevel) {
				int hostiles = countHostilesNearby(serverLevel, entity, 16.0);
				if (hostiles >= 4) {
					if (dist <= 6.5) {
						if (entity.tickCount % 20 == 0 && entity.getRandom().nextDouble() < 0.50) {
							return true;
						}
					}
				}

				if (isCrowdNearby(serverLevel, entity)) {
					if (entity.tickCount % 35 == 0 && entity.getRandom().nextDouble() < 0.50) {
						return true;
					}
				}
			}

			if (isStrongFlyingTarget(target)) {
				if (entity.tickCount % (STRONG_TARGET_BURST_CHECK_INTERVAL / 2) == 0 && entity.getRandom().nextDouble() < 0.650) {
					return true;
				}
			}

			boolean strongTarget = isStrongTarget(target) || target instanceof Player;

			if (strongTarget) {
				int checkInterval = STRONG_TARGET_BURST_CHECK_INTERVAL;
				double chance = STRONG_TARGET_BURST_CHANCE;
				if (target instanceof Player) {
					if (dist <= PROXIMITY_PLAYER_INNER_RADIUS) {
						checkInterval = Math.max(5, checkInterval / PLAYER_INNER_INTERVAL_DIVISOR);
						chance = Math.min(0.98, chance * PLAYER_INNER_CHANCE_BOOST);
					} else if (dist <= PROXIMITY_PLAYER_OUTER_RADIUS) {
						checkInterval = Math.max(10, checkInterval / PLAYER_OUTER_INTERVAL_DIVISOR);
						chance = Math.min(0.95, chance * PLAYER_OUTER_CHANCE_BOOST);
					}
				} else {
					if (dist <= 8.0) {
						checkInterval = Math.max(10, checkInterval / 4);
						chance = Math.min(0.95, chance * 2.2);
					} else if (dist <= 12.0) {
						checkInterval = Math.max(10, checkInterval / 2);
						chance = Math.min(0.95, chance * 1.5);
					}
				}
				if (entity.tickCount % checkInterval == 0 && entity.getRandom().nextDouble() < chance) {
					return true;
				}
			}

			return isEscapedContainmentDimension(entity)
				&& entity.tickCount % ESCAPED_BURST_CHECK_INTERVAL == 0
				&& entity.getRandom().nextDouble() < ESCAPED_BURST_CHANCE;
		}

		return false;
	}

	private static void playFractusLaserSphereBurstSound(ServerLevel level, Entity entity) {
		net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(FRACTUS_LASER_SPHERE_BURST_SOUND);
		if (sound != null) {
			level.playSound(
				null,
				BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()),
				sound,
				SoundSource.HOSTILE,
				net.mcreator.thebackwoods.FractusLaserBeam.PRIME_SPHERE_BURST_VOLUME,
				1.0f
			);
		}
	}

	private static void spawnHollowSphereParticles(ServerLevel level, Vec3 center, double radius, double densityFactor) {
		int count = (int) Math.max(10, (4.0 * Math.PI * radius * radius * densityFactor));
		if (count > 800) {
			count = 800;
		}
		double goldenRatio = (1.0 + Math.sqrt(5.0)) / 2.0;
		for (int i = 0; i < count; i++) {
			double theta = 2 * Math.PI * i / goldenRatio;
			double phi = Math.acos(1.0 - 2.0 * (i + 0.5) / count);
			double x = Math.cos(theta) * Math.sin(phi);
			double y = Math.sin(theta) * Math.sin(phi);
			double z = Math.cos(phi);
			
			double px = center.x + x * radius;
			double py = center.y + y * radius;
			double pz = center.z + z * radius;
			
			double rx = (level.getRandom().nextDouble() - 0.5) * 0.05;
			double ry = (level.getRandom().nextDouble() - 0.5) * 0.05;
			double rz = (level.getRandom().nextDouble() - 0.5) * 0.05;
			
			level.sendParticles(getBurstLaserParticle(), px + rx, py + ry, pz + rz, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static void spawnSphereBuildupWarningParticles(ServerLevel level, Entity entity) {
		Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
		level.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 2, 0.5, 0.5, 0.5, 0.1);
	}

	private static void handleLaserSphereBurst(ServerLevel level, Entity entity, int sphereTicks) {
		// Pre-sphere buildup warning (sphereTicks starts at 270 when triggered)
		if (sphereTicks > 240) {
			int buildupElapsed = 270 - sphereTicks;
			if (buildupElapsed == 0) {
				level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0f, 1.2f);
			}
			if (buildupElapsed % 10 == 0) {
				level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.5f, 1.5f);
			}
			spawnSphereBuildupWarningParticles(level, entity);
			return;
		}

		if (sphereTicks == 1) {
			entity.getPersistentData().putInt(K_VULNERABLE, 50);
		}

		int elapsed = 240 - sphereTicks;
		Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);

		if (elapsed == 0) {
			playFractusLaserSphereBurstSound(level, entity);
		}

		double currentRadius;
		boolean isReleasing = elapsed >= 135;

		if (!isReleasing) {
			double progress = (double) Math.min(elapsed, 134) / 134.0;
			currentRadius = SPHERE_START_RADIUS + progress * (SPHERE_MAX_BUILDUP_RADIUS - SPHERE_START_RADIUS);
			spawnHollowSphereParticles(level, center, currentRadius, 15.0);
		} else {
			double releaseProgress = Math.min(1.0, (double) (elapsed - 135) / 5.0);
			currentRadius = SPHERE_MAX_BUILDUP_RADIUS + releaseProgress * (SPHERE_MAX_RELEASE_RADIUS - SPHERE_MAX_BUILDUP_RADIUS);
			spawnHollowSphereParticles(level, center, currentRadius, elapsed <= 140 ? 5.0 : 2.0);

			if (elapsed <= 140) {
				double prevRadius;
				if (elapsed == 135) {
					prevRadius = SPHERE_MAX_BUILDUP_RADIUS;
				} else {
					double prevReleaseProgress = Math.min(1.0, (double) (elapsed - 1 - 135) / 5.0);
					prevRadius = SPHERE_MAX_BUILDUP_RADIUS + prevReleaseProgress * (SPHERE_MAX_RELEASE_RADIUS - SPHERE_MAX_BUILDUP_RADIUS);
				}

				vaporizeBlocksAndDamageEntitiesInSphereShell(level, entity, center, prevRadius, currentRadius);
			}
		}
	}

	private static void vaporizeBlocksAndDamageEntitiesInSphereShell(ServerLevel level, Entity entity, Vec3 center, double innerRadius, double outerRadius) {
		BlockPos centerPos = BlockPos.containing(center.x, center.y, center.z);
		double rMinSq = (innerRadius - 0.5) * (innerRadius - 0.5);
		double rMaxSq = (outerRadius + 0.5) * (outerRadius + 0.5);
		int r = (int) Math.ceil(outerRadius);

		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double distXZSq = dx * dx + dz * dz;
				if (distXZSq > rMaxSq) {
					continue;
				}
				double minDySq = Math.max(0.0, rMinSq - distXZSq);
				double maxDySq = rMaxSq - distXZSq;
				if (maxDySq < 0.0) {
					continue;
				}
				int minDy = (int) Math.floor(Math.sqrt(minDySq));
				int maxDy = (int) Math.ceil(Math.sqrt(maxDySq));

				for (int dy = minDy; dy <= maxDy; dy++) {
					BlockPos pos1 = centerPos.offset(dx, dy, dz);
					destroyWeakBlock(level, entity, pos1, SPHERE_VAPORIZE_MAX_HARDNESS);
					if (dy != 0) {
						BlockPos pos2 = centerPos.offset(dx, -dy, dz);
						destroyWeakBlock(level, entity, pos2, SPHERE_VAPORIZE_MAX_HARDNESS);
					}
				}
			}
		}

		AABB searchBox = new AABB(center.x - outerRadius - 1, center.y - outerRadius - 1, center.z - outerRadius - 1,
								 center.x + outerRadius + 1, center.y + outerRadius + 1, center.z + outerRadius + 1);
		java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox, t -> t != null && t.isAlive() && t != entity && (shouldIgnoreCombatFilter(entity) || !isWoodboundEntity(t)));
		for (LivingEntity target : targets) {
			double dist = target.position().distanceTo(center);
			if (dist >= innerRadius - 1.0 && dist <= outerRadius + 1.0) {
				float damage = isAngry(entity) ? ANGRY_SPHERE_BURST_DAMAGE : SPHERE_BURST_DAMAGE;
				if (entity instanceof LivingEntity attacker) {
					target.hurt(attacker.damageSources().mobAttack(attacker), damage);
				} else {
					target.hurt(new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), damage);
				}
				Vec3 push = target.position().subtract(center).normalize().scale(1.2);
				target.setDeltaMovement(push.x, push.y + 0.35, push.z);
				target.hasImpulse = true;
				target.hurtMarked = true;
			}
		}
	}

	private static boolean isCrowdNearby(ServerLevel level, Entity self) {
		AABB searchBox = self.getBoundingBox().inflate(16.0);
		java.util.List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, searchBox, target -> canTargetNormally(self, target) || canRetaliateAgainst(self, target));
		return near.size() >= 3;
	}

	private static int countHostilesNearby(ServerLevel level, Entity self, double radius) {
		AABB searchBox = self.getBoundingBox().inflate(radius);
		java.util.List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, searchBox, target -> canTargetNormally(self, target) || canRetaliateAgainst(self, target));
		return near.size();
	}

	private static Optional<Vec3> findNearbyBreakableBlock(ServerLevel level, Entity entity, double radius) {
		BlockPos center = entity.blockPosition();
		int r = (int) Math.ceil(radius);
		BlockPos bestPos = null;
		double bestDistanceSqr = Double.MAX_VALUE;
		float maxHardness = SPHERE_VAPORIZE_MAX_HARDNESS;

		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.hasBlockEntity()) {
				continue;
			}
			float hardness = state.getDestroySpeed(level, pos);
			if (hardness >= 0.0f && hardness <= maxHardness) {
				double dist = pos.getCenter().distanceToSqr(entity.position());
				if (dist < bestDistanceSqr) {
					bestDistanceSqr = dist;
					bestPos = pos.immutable();
				}
			}
		}

		return Optional.ofNullable(bestPos).map(BlockPos::getCenter);
	}

	private static boolean isStrongFlyingTarget(LivingEntity target) {
		if (target == null) {
			return false;
		}
		boolean isFlying = false;
		String className = target.getClass().getName().toLowerCase();
		if (className.contains("flying") || className.contains("flyer") || className.contains("dragon") || className.contains("gargoyle") || className.contains("valkyrie") || className.contains("aerial")) {
			isFlying = true;
		}
		if (!isFlying && target instanceof Mob mob) {
			try {
				String navClass = mob.getNavigation().getClass().getSimpleName().toLowerCase();
				if (navClass.contains("fly") || navClass.contains("aerial") || navClass.contains("air")) {
					isFlying = true;
				}
			} catch (Exception ignored) {}
			try {
				String moveClass = mob.getMoveControl().getClass().getSimpleName().toLowerCase();
				if (moveClass.contains("fly") || moveClass.contains("aerial") || moveClass.contains("hover")) {
					isFlying = true;
				}
			} catch (Exception ignored) {}
		}
		return isFlying && target.getMaxHealth() >= 20.0f;
	}

	private static boolean isStrongTarget(LivingEntity target) {
		if (target == null) {
			return false;
		}
		boolean healthStrong = target.getMaxHealth() >= STRONG_TARGET_HEALTH_THRESHOLD || target.getHealth() >= STRONG_TARGET_CURRENT_HEALTH_THRESHOLD;
		boolean damageStrong = false;
		try {
			if (target.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
				damageStrong = target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) >= 6.0;
			}
		} catch (Exception ignored) {}
		boolean livedLong = target.tickCount >= 1200; // Has lived for at least 1 minute (1200 ticks)
		return healthStrong || damageStrong || livedLong;
	}

	private static int burstCooldownTicksForTarget(LivingEntity target) {
		if (isStrongFlyingTarget(target)) {
			return STRONG_TARGET_BURST_COOLDOWN_TICKS / 2;
		}
		return isStrongTarget(target) ? STRONG_TARGET_BURST_COOLDOWN_TICKS : BURST_COOLDOWN_TICKS;
	}

	private static int sphereCooldownTicksForTarget(LivingEntity target) {
		if (isStrongFlyingTarget(target)) {
			return STRONG_TARGET_SPHERE_COOLDOWN_TICKS / 2;
		}
		return isStrongTarget(target) ? STRONG_TARGET_SPHERE_COOLDOWN_TICKS : SPHERE_COOLDOWN_TICKS;
	}

	private static void applyInfiniteRegeneration(LivingEntity entity) {
		if (!entity.hasEffect(MobEffects.REGENERATION)) {
			entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 72000, 0, true, false));
		}
	}

	private static boolean isWarden(Entity entity) {
		if (entity == null) return false;
		String name = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
		return name.contains("warden") || entity.getClass().getSimpleName().toLowerCase().contains("warden");
	}

	private static boolean isRegularFractus(Entity entity) {
		if (entity == null) return false;
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		String path = id.getPath();
		return "the_backwoods:fractus".equals(id.toString()) || ("fractus".equals(path) && !"fractus_prime".equals(path));
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
		entity.getPersistentData().putInt(K_ANGER_SOUND_COOLDOWN, ANGER_SOUND_INTERVAL_TICKS);
	}

	private static int persistentInt(Entity entity, String key, int fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getInt(key) : fallback;
	}

	private static double persistentDouble(Entity entity, String key, double fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getDouble(key) : fallback;
	}

	private static boolean persistentBoolean(Entity entity, String key, boolean fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getBoolean(key) : fallback;
	}

	private static String persistentString(Entity entity, String key, String fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getString(key) : fallback;
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

	private static void playFractusLaserSound(ServerLevel level, Entity entity) {
		entity.getPersistentData().putBoolean(K_LASER_SOUND_PLAYING, true);
		net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(FRACTUS_LASER_SOUND);
		if (sound != null) {
			level.playSound(
				null,
				BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()),
				sound,
				SoundSource.HOSTILE,
				net.mcreator.thebackwoods.FractusLaserBeam.PRIME_LASER_VOLUME,
				0.65f
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
				net.mcreator.thebackwoods.FractusLaserBeam.PRIME_BURST_VOLUME,
				1.0f
			);
		}
	}

	private static void playFractusAngerSound(ServerLevel level, Entity entity) {
		net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(FRACTUS_ANGER_SOUND);
		if (sound != null) {
			level.playSound(
				null,
				BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()),
				sound,
				SoundSource.HOSTILE,
				net.mcreator.thebackwoods.FractusLaserBeam.PRIME_ANGER_VOLUME,
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
		ClientboundStopSoundPacket stopSphere = new ClientboundStopSoundPacket(FRACTUS_LASER_SPHERE_BURST_SOUND, SoundSource.HOSTILE);

		for (ServerPlayer player : level.players()) {
			player.connection.send(stopLaser);
			player.connection.send(stopBurst);
			player.connection.send(stopAnger);
			player.connection.send(stopSphere);
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
		entity.getPersistentData().putInt(K_CHARGE, 0);
		entity.getPersistentData().putInt(K_FIRE, 0);
		entity.getPersistentData().putDouble(K_AIM_X, 0.0);
		entity.getPersistentData().putDouble(K_AIM_Y, 0.0);
		entity.getPersistentData().putDouble(K_AIM_Z, 0.0);
		entity.getPersistentData().putDouble(K_BURST_AIM_X, 0.0);
		entity.getPersistentData().putDouble(K_BURST_AIM_Y, 0.0);
		entity.getPersistentData().putDouble(K_BURST_AIM_Z, 0.0);
		entity.getPersistentData().putInt(K_SPHERE_TIMER, 0);
		entity.getPersistentData().putBoolean("telekinesis_active", false);
	}

	private static Vec3 home(Entity entity) {
		return new Vec3(
			persistentDouble(entity, K_HOME_X, entity.getX()),
			persistentDouble(entity, K_HOME_Y, entity.getY()),
			persistentDouble(entity, K_HOME_Z, entity.getZ())
		);
	}

	private static boolean shouldReturnHome(Entity entity) {
		if (isEscapedContainmentDimension(entity) || isWorldTakeoverDimension(entity)) {
			return false;
		}

		return entity.position().distanceTo(home(entity)) > HOME_LEASH_RANGE;
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

		if (!isWorldTakeoverDimension(entity)) {
			return false;
		}

		int fireTicks = tickStoredTimer(entity, K_DESTROYING_FIRE);

		if (fireTicks <= 0) {
			int cooldown = tickStoredTimer(entity, K_DESTROYING_COOLDOWN);

			if (cooldown > 0) {
				return false;
			}

			fireTicks = DESTROYING_FIRE_TICKS;
			entity.getPersistentData().putInt(K_DESTROYING_FIRE, fireTicks);
			entity.getPersistentData().putInt(K_DESTROYING_COOLDOWN, DESTROYING_COOLDOWN_TICKS + entity.getRandom().nextInt(80));
			storeDestroyingAim(entity, destroyingDirectionTowardNearbyBlock(level, entity).orElseGet(() -> randomDestroyingDirection(entity)));
			playFractusLaserSound(level, entity);
		}

		Vec3 start = laserStart(entity);
		
		if (fireTicks % DESTROYING_AIM_REFRESH_TICKS == 0) {
			destroyingDirectionTowardNearbyBlock(level, entity).ifPresent(direction -> storeDestroyingAim(entity, direction));
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
		entity.getPersistentData().putInt(K_LASER_STATE, 3);
		igniteLaserTrailBlocks(level, entity, start, end, true);
		if (fireTicks <= 1) {
			stopFractusLaserSound(level, entity);
			entity.getPersistentData().putInt(K_LASER_STATE, 0);
		}

		return true;
	}

	private static void cancelDestroyingMode(ServerLevel level, Entity entity) {
		if (persistentInt(entity, K_DESTROYING_FIRE, 0) <= 0) {
			return;
		}

		entity.getPersistentData().putInt(K_DESTROYING_FIRE, 0);
		stopFractusLaserSound(level, entity);
	}

	private static boolean isWorldTakeoverDimension(Entity entity) {
		if (entity == null) {
			return false;
		}
		String dim = entity.level().dimension().location().toString();
		if (dim.equals("the_backwoods:backwoods")
			|| dim.equals("the_backwoods:the_grain")
			|| dim.equals("the_backwoods:the_sub_strata")
			|| dim.equals("the_backwoods:loss")
			|| dim.equals("the_backwoods:rotting")
			|| dim.equals("the_backwoods:the_still")
			|| dim.equals("the_backwoods:the_familiar")
			|| dim.equals("the_backwoods:the_petrified_weald")) {
			return false;
		}
		return entity.level().dimension() == Level.OVERWORLD || entity.level().dimension() == Level.NETHER;
	}

	private static boolean isNoFireDimension(Entity entity) {
		if (entity == null) {
			return false;
		}
		String dim = entity.level().dimension().location().toString();
		return dim.equals("the_backwoods:backwoods")
			|| dim.equals("the_backwoods:the_grain")
			|| dim.equals("the_backwoods:the_sub_strata")
			|| dim.equals("the_backwoods:loss")
			|| dim.equals("the_backwoods:rotting")
			|| dim.equals("the_backwoods:the_still")
			|| dim.equals("the_backwoods:the_familiar")
			|| dim.equals("the_backwoods:the_petrified_weald");
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

			if (isWorldTakeoverDimension(entity)) {
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

	private static LivingEntity findTarget(ServerLevel level, Entity self, double x, double y, double z) {
		if (self instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
			LivingEntity currentTarget = mob.getTarget();
			if (shouldIgnoreCombatFilter(self) || shouldIgnoreCombatFilter(currentTarget)) {
				return currentTarget;
			}
		}

		LivingEntity retaliationTarget = retaliationTarget(self);

		if (retaliationTarget != null && retaliationTarget.distanceTo(self) <= DETECTION_RANGE * 1.6) {
			int coverWait = persistentInt(self, K_COVER_WAIT, 0);
			if (canSeeForTargeting(level, self, retaliationTarget) || coverWait < (isAngry(self) ? 300 : 180)) {
				return retaliationTarget;
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

		return level.getEntitiesOfClass(LivingEntity.class, searchBox, target -> canTargetNormally(self, target))
			.stream()
			.sorted(Comparator.comparingDouble(target -> target.distanceToSqr(self)))
			.limit(TARGET_LOS_CANDIDATE_LIMIT)
			.filter(target -> canSeeForTargeting(level, self, target))
			.min(Comparator.comparingDouble(target -> targetPriorityScore(level, self, target)))
			.orElse(null);
	}

	private static boolean isTargetOccupied(ServerLevel level, Entity self, LivingEntity target) {
		AABB searchBox = self.getBoundingBox().inflate(SUPPRESSION_RANGE);
		for (Entity entity : level.getEntities(self, searchBox, e -> isFractusKind(e))) {
			if (entity instanceof Mob otherMob && otherMob.getTarget() == target) {
				return true;
			}
		}
		return false;
	}

	private static double targetPriorityScore(ServerLevel level, Entity self, LivingEntity target) {
		double score = target.distanceToSqr(self);

		if (target instanceof Player) {
			score -= DETECTION_RANGE * DETECTION_RANGE * 2.0;
		}

		if (isTargetOccupied(level, self, target)) {
			score += 1000000.0;
		}

		return score;
	}

	private static java.util.List<LivingEntity> findMultipleTargets(ServerLevel level, Entity self, LivingEntity primaryTarget, boolean angry, double range) {
		java.util.List<LivingEntity> targets = new java.util.ArrayList<>();
		if (primaryTarget != null && primaryTarget.isAlive()) {
			targets.add(primaryTarget);
		}

		if (targets.size() >= 5) {
			return targets;
		}

		boolean primaryIsStrong = primaryTarget != null && (primaryTarget instanceof Player || isStrongTarget(primaryTarget));

		if (!angry && !primaryIsStrong) {
			return targets;
		}

		AABB searchBox = self.getBoundingBox().inflate(range);
		java.util.List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, searchBox, candidate -> {
			if (candidate == self || candidate == primaryTarget) {
				return false;
			}
			if (!angry) {
				boolean candidateIsStrong = candidate instanceof Player || isStrongTarget(candidate);
				if (!candidateIsStrong) {
					return false;
				}
			}
			return canDamage(self, candidate) && hasClearShot(level, self, candidate, range);
		});

		java.util.List<LivingEntity> sortedNearby = new java.util.ArrayList<>(nearby);
		sortedNearby.sort(java.util.Comparator.comparingDouble(candidate -> candidate.distanceToSqr(self)));

		for (LivingEntity candidate : sortedNearby) {
			targets.add(candidate);
			if (targets.size() >= 5) {
				break;
			}
		}

		return targets;
	}

	private static boolean canDamage(Entity self, LivingEntity target) {
		return canTargetNormally(self, target) || isRetaliationTarget(self, target);
	}

	private static boolean canBurstDamage(Entity self, LivingEntity target) {
		return canTargetNormally(self, target) || canRetaliateAgainst(self, target);
	}

	private static boolean canEcholocateTarget(Entity self, LivingEntity target) {
		if (target == null || target == self || !target.isAlive()) {
			return false;
		}

		if (shouldIgnoreCombatFilter(self) || shouldIgnoreCombatFilter(target)) {
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

		if (target instanceof Player player) {
			if (player.isCreative() || player.isSpectator()) {
				return false;
			}
		}

		return true;
	}

	private static boolean canTargetNormally(Entity self, LivingEntity target) {
		if (target == null || target == self || !target.isAlive()) {
			return false;
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

		if (target instanceof Player player) {
			if (player.isCreative() || player.isSpectator()) {
				return false;
			}
			if (player.hasEffect(MobEffects.INVISIBILITY) && isRegularFractus(self)) {
				boolean hasBeenAttackedByPlayer = retaliationTarget(self) == player;
				if (!hasBeenAttackedByPlayer) {
					return false;
				}
			}
		}

		if (target.hasEffect(MobEffects.INVISIBILITY)) {
			boolean isSelfRetaliating = retaliationTarget(self) == target;
			String detectedUuid = persistentString(self, "fractus_echo_detected_uuid", "");
			boolean isSelfDetected = !detectedUuid.isEmpty() && detectedUuid.equals(target.getUUID().toString()) && persistentInt(self, "fractus_echo_detected_time", 0) > 0;
			if (!isSelfRetaliating && !isSelfDetected) {
				return false;
			}
		}

		if (isEscapedContainmentDimension(self) || isWorldTakeoverDimension(self)) {
			return true;
		}

		if (true) {
			// Prime will occasionally target valid entities (mobs) even in home dimension
			return (self.getId() + self.tickCount / 120) % 3 == 0;
		}

		return false;
	}

	private static boolean isWoodboundEntity(LivingEntity target) {
		return target != null && target.getType().is(WOODBOUND_ENTITIES_TAG);
	}

	private static boolean isSeaAnimal(LivingEntity target) {
		if (target instanceof WaterAnimal) {
			return true;
		}

		String idPath = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
		return "turtle".equals(idPath) || "axolotl".equals(idPath);
	}

	private static boolean canRetaliateAgainst(Entity self, LivingEntity target) {
		boolean bypassFactionFilter = shouldIgnoreCombatFilter(self) || shouldIgnoreCombatFilter(target)
			|| (self instanceof Mob m && m.getTarget() == target);
		if (bypassFactionFilter) {
			return target != null && target != self && target.isAlive();
		}
		return target != null
			&& target != self
			&& target.isAlive()
			&& !isFractusKind(target)
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
		return source.is(DamageTypeTags.IS_PROJECTILE) || direct instanceof Projectile;
	}

	private static void markRetaliationTarget(Entity self, DamageSource source) {
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

		if (persistentInt(entity, K_VULNERABLE, 0) > 0) {
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

	private static void faceTarget(Entity entity, LivingEntity target) {
		entity.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

		if (entity instanceof Mob mob) {
			float yRot = (float) (Mth.atan2(target.getZ() - entity.getZ(), target.getX() - entity.getX()) * (180.0F / Math.PI)) - 90.0F;

			mob.setYRot(yRot);
			mob.setYHeadRot(yRot);
			mob.setYBodyRot(yRot);
			mob.getNavigation().stop();
			mob.setTarget(target);
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

	private static void moveCombat(Entity entity, LivingEntity target, boolean attacking, boolean angry, int cooldown, int angerRetreatTicks) {
		if (angerRetreatTicks > 0) {
			moveRetreat(entity, target, angry);
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
		double preferredRange = angry ? ANGRY_PREFERRED_COMBAT_RANGE : PREFERRED_COMBAT_RANGE;
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
		double preferredRange = angry ? ANGRY_PREFERRED_COMBAT_RANGE : PREFERRED_COMBAT_RANGE;
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
		AABB box = entity.getBoundingBox().move(center.subtract(entity.position())).inflate(0.15);
		return level.noCollision(entity, box) && !hasNearbyHarmfulBlock(level, center, HARMFUL_BLOCK_AVOID_RADIUS);
	}

	private static Optional<Vec3> adjustDesiredForHazards(ServerLevel level, Entity entity, Vec3 desired) {
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
		return nearestHarmfulBlockCenter(level, center, radius).isPresent();
	}

	private static Optional<Vec3> nearestHarmfulBlockCenter(ServerLevel level, Vec3 center, double radius) {
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
		double preferredRange = angry ? ANGRY_PREFERRED_COMBAT_RANGE : PREFERRED_COMBAT_RANGE;

		int sphereTicks = persistentInt(entity, K_SPHERE_TIMER, 0);
		int sphereCooldown = persistentInt(entity, K_SPHERE_COOLDOWN, 0);
		boolean lotOfHostiles = entity.level() instanceof ServerLevel level && countHostilesNearby(level, entity, 16.0) >= 4;
		boolean approachForSphere = sphereTicks > 0 || (sphereCooldown <= 0 && lotOfHostiles);
		boolean isBackingUpForBurst = persistentBoolean(entity, "fractus_backing_up_for_burst", false);

		double desiredRange;
		Vec3 desired;
		if (isBackingUpForBurst) {
			desiredRange = 18.0;
			desired = targetCenter
				.add(horizontalAway.scale(desiredRange))
				.add(0.0, currentHoverHeight(entity, angry) + bob, 0.0);
		} else if (approachForSphere) {
			desiredRange = 5.5;
			desired = targetCenter
				.add(horizontalAway.scale(desiredRange))
				.add(0.0, currentHoverHeight(entity, angry) + bob, 0.0);
		} else {
			desiredRange = currentDistance < TOO_CLOSE_RANGE ? preferredRange + 3.0 : preferredRange;
			desired = targetCenter
				.add(horizontalAway.scale(desiredRange))
				.add(side.scale(orbitPulse))
				.add(0.0, currentHoverHeight(entity, angry) + bob, 0.0);
		}

		double maxSpeed = angry ? ANGRY_MAX_DRONE_SPEED : MAX_DRONE_SPEED;
		double speedMultiplier = isBackingUpForBurst ? 1.50 : (approachForSphere ? 1.25 : (attacking ? 0.55 : 1.0));
		moveToward(entity, desired, maxSpeed * speedMultiplier);
	}

	private static void moveToward(Entity entity, Vec3 desired, double maxSpeed) {
		if (entity.level() instanceof ServerLevel level) {
			desired = adjustDesiredForHazards(level, entity, desired).orElse(desired);
			desired = adjustDesiredForOpenSpace(level, entity, desired);
			desired = clampDesiredForCeiling(level, entity, desired);
		}

		int burstTicks = persistentInt(entity, K_BURST_TIMER, 0);
		int sphereTicks = persistentInt(entity, K_SPHERE_TIMER, 0);
		boolean isBursting = burstTicks > 0 || sphereTicks > 0;

		Vec3 toDesired = desired.subtract(entity.position());
		double accelRate = isBursting ? DRONE_ACCELERATION * 0.12 : DRONE_ACCELERATION;
		Vec3 acceleration = toDesired.scale(accelRate);
		
		double drag = isBursting ? 0.45 : 0.82;
		Vec3 next = entity.getDeltaMovement().scale(drag).add(acceleration);

		double speedLimit = isBursting ? maxSpeed * 0.15 : maxSpeed;
		double horizontalSpeed = Math.sqrt(next.x * next.x + next.z * next.z);

		if (horizontalSpeed > speedLimit) {
			double scale = speedLimit / horizontalSpeed;
			next = new Vec3(next.x * scale, next.y, next.z * scale);
		}

		next = new Vec3(
			next.x,
			Mth.clamp(next.y, isBursting ? -0.05 : -VERTICAL_SPEED_LIMIT, isBursting ? 0.05 : VERTICAL_SPEED_LIMIT),
			next.z
		);

		entity.setDeltaMovement(next);
		entity.hasImpulse = true;
	}

	private static Vec3 clampDesiredForCeiling(ServerLevel level, Entity entity, Vec3 desired) {
		double clearanceNeeded = entity.getBbHeight() + 0.6;
		double startY = Math.min(entity.getY(), desired.y);
		
		int checkRange = (int) Math.max(6.0, (desired.y - startY) + 6.0);
		
		// Fractus Prime is 2.0 blocks wide. To scan its full 2x2 horizontal footprint at each height, we check all overlapping blocks columns around (desired.x, desired.z) using a half-width offset.
		double halfWidth = 0.95;
		int minX = Mth.floor(desired.x - halfWidth);
		int maxX = Mth.floor(desired.x + halfWidth);
		int minZ = Mth.floor(desired.z - halfWidth);
		int maxZ = Mth.floor(desired.z + halfWidth);
		int startBlockY = Mth.floor(startY);
		
		for (int dy = 0; dy < checkRange; dy++) {
			int currentY = startBlockY + dy;
			BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
			mut.setY(currentY);
			
			if (level.isOutsideBuildHeight(mut)) {
				break;
			}
			
			boolean collided = false;
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					mut.setX(x);
					mut.setZ(z);
					
					BlockState state = level.getBlockState(mut);
					boolean isPassableBlock = state.isAir();
					if (!isPassableBlock) {
						try {
							isPassableBlock = state.getCollisionShape(level, mut).isEmpty();
						} catch (Exception e) {
							isPassableBlock = !state.isCollisionShapeFullBlock(level, mut);
						}
					}
					if (!isPassableBlock) {
						collided = true;
						break;
					}
				}
				if (collided) {
					break;
				}
			}
			
			if (collided) {
				double ceilingY = currentY;
				double maxAllowedY = ceilingY - clearanceNeeded;
				if (desired.y > maxAllowedY) {
					return new Vec3(desired.x, Math.max(startY, maxAllowedY), desired.z);
				}
				break;
			}
		}
		
		if (level.dimensionType().hasCeiling()) {
			double ceilingY = 127.0;
			double maxAllowedY = ceilingY - clearanceNeeded;
			if (desired.y > maxAllowedY) {
				return new Vec3(desired.x, Math.max(startY, maxAllowedY), desired.z);
			}
		}
		
		return desired;
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
		boolean blockedAhead = false;
		if (tryingToMove) {
			Vec3 diff = desired.subtract(position);
			if (diff.lengthSqr() > 0.001) {
				AABB nextBox = entity.getBoundingBox().move(diff.normalize().scale(0.6));
				blockedAhead = !level.noCollision(entity, nextBox);
			}
		}
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

	private static boolean hasClearShot(ServerLevel level, Entity self, LivingEntity target, double maxRange) {
		Vec3 start = laserStart(self);
		Vec3 targetEyes = target.getEyePosition();

		if (start.distanceTo(targetEyes) > maxRange) {
			return false;
		}

		BlockHitResult blockHit = clipBlocks(level, self, start, targetEyes);

		return blockHit.getType() == HitResult.Type.MISS
			|| blockHit.getLocation().distanceToSqr(start) + 0.35 >= targetEyes.distanceToSqr(start);
	}

	private static boolean canLaserDrillThrough(ServerLevel level, Entity self, LivingEntity target, double maxRange, boolean angry) {
		if (target == null) {
			return false;
		}
		Vec3 start = laserStart(self);
		Vec3 targetEyes = target.getEyePosition();
		double dist = start.distanceTo(targetEyes);

		if (dist > maxRange) {
			return false;
		}

		float maxHardness = angry ? ANGRY_WEAK_BLOCK_MAX_HARDNESS : WEAK_BLOCK_MAX_HARDNESS;
		Vec3 direction = targetEyes.subtract(start);
		if (direction.lengthSqr() < 0.001) {
			return true;
		}
		direction = direction.normalize();

		double step = 0.5;
		BlockPos lastPos = null;
		for (double d = 0.0; d <= dist; d += step) {
			Vec3 sample = start.add(direction.scale(d));
			BlockPos pos = BlockPos.containing(sample.x, sample.y, sample.z);

			if (pos.equals(lastPos)) {
				continue;
			}
			lastPos = pos;

			if (!level.hasChunkAt(pos)) {
				return false;
			}

			BlockState state = level.getBlockState(pos);
			if (state.isAir()) {
				continue;
			}

			float hardness = state.getDestroySpeed(level, pos);
			if (hardness < 0.0f || hardness > maxHardness || state.hasBlockEntity()) {
				return false;
			}
		}

		return true;
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
		Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.45, 0);
		return targetCenter.add(target.getDeltaMovement().scale(leadTicks));
	}

	private static Vec3 updateLaserAim(Entity self, LivingEntity target, boolean angry, boolean firing) {
		Vec3 start = laserStart(self);
		Vec3 desired = predictedTargetEyePosition(target, angry).subtract(start);

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

		self.lookAt(EntityAnchorArgument.Anchor.EYES, start.add(adjusted));
		if (self instanceof LivingEntity living) {
			living.setYBodyRot(living.getYRot());
		}
		if (self instanceof Mob mob) {
			Vec3 targetLook = start.add(adjusted);
			mob.getLookControl().setLookAt(targetLook.x, targetLook.y, targetLook.z, 180.0F, 180.0F);
		}

		return adjusted;
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

		if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living && canDamage(self, living)) {
			return new LaserHit(entityHit.getLocation(), living, null, null);
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

		for (Entity candidate : level.getEntities(self, search, candidate -> candidate instanceof LivingEntity living && canDamage(self, living))) {
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

		if (!level.hasChunkAt(firePos)) {
			return;
		}

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

		if (!level.hasChunkAt(firePos)) {
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
		if (isNoFireDimension(hitEntity)) {
			return;
		}
		if (hitEntity instanceof LivingEntity living && !living.fireImmune()) {
			living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), LASER_ENTITY_FIRE_TICKS));
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

			if (!level.hasChunkAt(pos)) {
				break;
			}

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

			level.destroyBlock(pos, false, entity);
		}

		if (blockedPos != null) {
			destroyWeakBlock(level, entity, blockedPos, maxHardness);
		}
	}

	private static void destroyWeakBlock(ServerLevel level, Entity entity, BlockPos pos, float maxHardness) {
		if (!level.hasChunkAt(pos)) {
			return;
		}
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

		level.destroyBlock(pos, false, entity);
	}

	private static void handleBurstLaser(ServerLevel level, Entity entity, LivingEntity target, int burstTicks) {
		boolean hasTelekinesis = persistentBoolean(entity, "telekinesis_active", false);
		int totalDuration = BURST_TOTAL_TICKS + (hasTelekinesis ? TELEKINESIS_LIFT_ONLY_TICKS : 0);
		int elapsed = totalDuration - burstTicks;
		Vec3 start = laserStart(entity);
		Vec3 direction = burstDirection(entity, target, start);
		Vec3 end = start.add(direction.scale(BURST_LASER_RANGE));

		if (hasTelekinesis && elapsed < TELEKINESIS_LIFT_ONLY_TICKS) {
			entity.getPersistentData().putInt(K_LASER_STATE, 0); // Maintain 0 state during lift
			// Pure lift phase: do nothing here regarding the laser burst
			return;
		}

		int activeElapsed = hasTelekinesis ? elapsed - TELEKINESIS_LIFT_ONLY_TICKS : elapsed;
		int firePeak = BURST_FIRE_PEAK_TICK;
		int coreEnd = BURST_CORE_END_TICK;

		if (activeElapsed == 0) {
			playFractusLaserBurstSound(level, entity);
		}

		if (activeElapsed < firePeak) {
			entity.getPersistentData().putInt(K_LASER_STATE, 2); // Synced Charging for burst visual
			spawnBurstBuildup(level, entity, start, activeElapsed);
			return;
		}

		if (activeElapsed <= coreEnd) {
			entity.getPersistentData().putInt(K_LASER_STATE, 3); // Synced Firing for burst visual
			BlockHitResult blockHit = clipBlocks(level, entity, start, end);
			Vec3 blockedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
			destroyBurstBlocksInLaserPath(level, entity, start, blockedEnd, blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getBlockPos());
			blockHit = clipBlocks(level, entity, start, end);
			blockedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
			igniteLaserHitBlock(level, entity, blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getBlockPos(), blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getDirection());

			spawnBurstBeam(level, start, blockedEnd, activeElapsed);

			if ((activeElapsed - firePeak) % 10 == 0) {
				damageBurstEntities(level, entity, start, blockedEnd);
			}

			return;
		}

		entity.getPersistentData().putInt(K_LASER_STATE, 4); // Synced Cooldown / dissipation

		if (burstTicks == 1) {
			entity.getPersistentData().putInt(K_LASER_STATE, 0); // Reset state
			entity.getPersistentData().putDouble(K_BURST_AIM_X, 0.0);
			entity.getPersistentData().putDouble(K_BURST_AIM_Y, 0.0);
			entity.getPersistentData().putDouble(K_BURST_AIM_Z, 0.0);
			entity.getPersistentData().putInt(K_VULNERABLE, 50);
		}
	}

	private static Vec3 burstDirection(Entity entity, LivingEntity target, Vec3 start) {
		Vec3 desired = null;
		boolean isCrowd = false;
		boolean hasTelekinesis = persistentBoolean(entity, "telekinesis_active", false);

		if (!hasTelekinesis && entity.level() instanceof ServerLevel serverLevel) {
			AABB searchBox = entity.getBoundingBox().inflate(16.0);
			java.util.List<LivingEntity> near = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox, t -> t != null && t.isAlive() && (canTargetNormally(entity, t) || canRetaliateAgainst(entity, t)));
			if (near.size() >= 3) {
				isCrowd = true;
				double sumX = 0, sumY = 0, sumZ = 0;
				for (LivingEntity e : near) {
					sumX += e.getX();
					sumY += e.getY() + e.getBbHeight() * 0.45;
					sumZ += e.getZ();
				}
				Vec3 crowdCenter = new Vec3(sumX / near.size(), sumY / near.size(), sumZ / near.size());
				desired = crowdCenter.subtract(start);
				if (desired.lengthSqr() < 0.001) {
					desired = entity.getLookAngle();
				}
			} else if (near.size() >= 2) {
				isCrowd = true;
			}
		}

		if (desired == null) {
			LivingEntity burstTarget = target != null ? target : retaliationTarget(entity);
			if (burstTarget != null) {
				Vec3 burstTargetCenter = burstTarget.position().add(0, burstTarget.getBbHeight() * 0.45, 0);
				desired = burstTargetCenter.subtract(start);
				if (desired.lengthSqr() < 0.001) {
					desired = entity.getLookAngle();
				}
			} else {
				desired = entity.getLookAngle();
			}
		}

		desired = desired.normalize();

		if (isCrowd && !hasTelekinesis) {
			// Horizontal sinusoidal sweeping motion to spray across multiple targets
			double wave = Math.sin(entity.tickCount * 0.12) * 0.22;
			Vec3 right = desired.cross(new Vec3(0, 1, 0));
			if (right.lengthSqr() < 0.001) {
				right = desired.cross(new Vec3(1, 0, 0));
			}
			right = right.normalize();
			desired = desired.add(right.scale(wave)).normalize();
		}

		Vec3 current = new Vec3(
			persistentDouble(entity, K_BURST_AIM_X, 0.0),
			persistentDouble(entity, K_BURST_AIM_Y, 0.0),
			persistentDouble(entity, K_BURST_AIM_Z, 0.0)
		);

		if (current.lengthSqr() < 0.001) {
			current = entity.getLookAngle().normalize();
		} else {
			current = current.normalize();
		}

		double turnRate = BURST_LASER_TURN_RATE;
		Vec3 adjusted = rotateToward(current, desired, turnRate);

		entity.getPersistentData().putDouble(K_BURST_AIM_X, adjusted.x);
		entity.getPersistentData().putDouble(K_BURST_AIM_Y, adjusted.y);
		entity.getPersistentData().putDouble(K_BURST_AIM_Z, adjusted.z);

		return adjusted;
	}

	private static void destroyBurstBlocksInLaserPath(ServerLevel level, Entity entity, Vec3 start, Vec3 end, BlockPos blockedPos) {
		Vec3 line = end.subtract(start);
		double length = line.length();

		if (length < 0.01) {
			return;
		}

		Vec3 direction = line.normalize();
		int radius = (int) Math.ceil(BURST_LASER_RADIUS);

		for (double d = 0.0; d <= length; d += BURST_BLOCK_BREAK_STEP) {
			Vec3 center = start.add(direction.scale(d));
			BlockPos centerPos = BlockPos.containing(center.x, center.y, center.z);

			for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-radius, -radius, -radius), centerPos.offset(radius, radius, radius))) {
				if (pos.getCenter().distanceTo(center) > BURST_LASER_RADIUS + 0.35) {
					continue;
				}

				destroyWeakBlock(level, entity, pos, BURST_BLOCK_MAX_HARDNESS);
			}
		}

		if (blockedPos != null) {
			destroyWeakBlock(level, entity, blockedPos, BURST_BLOCK_MAX_HARDNESS);
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

	private static void damageBurstEntities(ServerLevel level, Entity entity, Vec3 start, Vec3 end) {
		Vec3 line = end.subtract(start);
		double lengthSqr = line.lengthSqr();

		if (lengthSqr < 0.001) {
			return;
		}

		AABB search = new AABB(start, end).inflate(BURST_LASER_RADIUS + 1.0);

		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search, target -> canBurstDamage(entity, target))) {
			Vec3 toCandidate = candidate.getEyePosition().subtract(start);
			double t = Mth.clamp(toCandidate.dot(line) / lengthSqr, 0.0, 1.0);
			Vec3 closest = start.add(line.scale(t));

			if (candidate.getEyePosition().distanceTo(closest) <= BURST_LASER_RADIUS + candidate.getBbWidth() * 0.5) {
				candidate.invulnerableTime = 0;
				if (entity instanceof LivingEntity attacker) {
					candidate.hurt(attacker.damageSources().mobAttack(attacker), BURST_LASER_DAMAGE);
				} else {
					candidate.hurt(new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), BURST_LASER_DAMAGE);
				}
				applyBurstKnockback(candidate, start, closest, entity);
			}
		}
	}

	private static void applyBurstKnockback(LivingEntity target, Vec3 start, Vec3 closestBeamPoint, Entity attacker) {
		if (persistentBoolean(attacker, "telekinesis_active", false)) {
			// Do not apply knockback if telekinetically held
			return;
		}

		Vec3 away = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(closestBeamPoint);

		if (away.lengthSqr() < 0.001) {
			away = target.position().subtract(start);
		}

		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = target.getLookAngle().scale(-1.0);
		}

		horizontalAway = new Vec3(horizontalAway.x, 0.0, horizontalAway.z).normalize();
		Vec3 knockback = horizontalAway.scale(BURST_KNOCKBACK_HORIZONTAL).add(0.0, BURST_KNOCKBACK_VERTICAL, 0.0);
		target.setDeltaMovement(target.getDeltaMovement().add(knockback));
		target.hasImpulse = true;
		target.hurtMarked = true; // Sync velocity knockback for players
	}

	private static void spawnBurstBuildup(ServerLevel level, Entity entity, Vec3 center, int elapsed) {
		double progress = Mth.clamp((double) elapsed / BURST_FIRE_PEAK_TICK, 0.0, 1.0);
		int baseCount = 100;
		int addedCount = 250;
		int count = baseCount + (int) (progress * addedCount);
		double radius = 9.5 - progress * 8.2;
		net.minecraft.core.particles.ParticleOptions particle = burstChargeParticle(progress);

		for (int i = 0; i < count; i++) {
			// Uniform distribution on a 3D sphere surface
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

		// Collapsing energy halo/ring centered at look angle
		Vec3 direction = entity.getLookAngle().normalize();
		Vec3 up = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 side = direction.cross(up).normalize();
		Vec3 verticalSide = direction.cross(side).normalize();

		double ringRadius = (8.5) * (1.0 - progress * 0.95);
		int ringCount = 36;
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
		int numArms = 3;
		double spacing = BURST_LASER_SPIRAL_SPACING * 0.35;

		Vec3 up = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 side = direction.cross(up).normalize();
		Vec3 verticalSide = direction.cross(side).normalize();

		if (net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
			for (double d = 0.0; d <= length; d += spacing) {
				Vec3 center = start.add(direction.scale(d));
				double coreRadius = 0.65; // Increased cylinder radius for the ray of the laser burst
				level.sendParticles(getBurstLaserParticle(), center.x, center.y, center.z, 12, coreRadius, coreRadius, coreRadius, 0.0);

				for (int a = 0; a < numArms; a++) {
					double angle = (Math.PI * 2.0 * a / numArms) + (d * 0.20) + (elapsed * 0.75);
					Vec3 pos = center.add(side.scale(Math.cos(angle) * ringRadius)).add(verticalSide.scale(Math.sin(angle) * ringRadius));
					level.sendParticles(getBurstLaserParticle(), pos.x, pos.y, pos.z, 4, 0.02, 0.02, 0.02, 0.0);
				}
			}
		}

		level.sendParticles(getBurstLaserParticle(), end.x, end.y, end.z, 150, 0.8, 0.8, 0.8, 0.0);
	}

	private static void spawnBurstDissipation(ServerLevel level, Vec3 impact, Vec3 direction, int elapsed) {
		// Empty to remove rings and avoid setting fire during dissipation phase
	}

	private static void spawnDodgeParticles(ServerLevel level, Vec3 center) {
		level.sendParticles(getAngryLaserParticle(), center.x, center.y, center.z, 26, 0.35, 0.35, 0.35, 0.0);
	}

	private static void spawnEchoScanParticles(ServerLevel level, Vec3 center, double radius) {
		int count = (int) Math.max(12, (0.32 * 4.0 * Math.PI * radius * radius));
		if (count > 250) {
			count = 250;
		}
		double goldenRatio = (1.0 + Math.sqrt(5.0)) / 2.0;
		for (int i = 0; i < count; i++) {
			double theta = 2 * Math.PI * i / goldenRatio;
			double phi = Math.acos(1.0 - 2.0 * (i + 0.5) / count);
			double x = Math.cos(theta) * Math.sin(phi);
			double y = Math.sin(theta) * Math.sin(phi);
			double z = Math.cos(phi);
			
			double px = center.x + x * radius;
			double py = center.y + y * radius;
			double pz = center.z + z * radius;
			
			level.sendParticles(getBurstChargeStartParticle(), px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static void handleEcholocation(ServerLevel level, Entity entity) {
		int detectedTime = tickStoredTimer(entity, "fractus_echo_detected_time");
		int echoScanTicks = tickStoredTimer(entity, "fractus_echo_scan_ticks");

		if (detectedTime > 0) {
			String uuidStr = persistentString(entity, "fractus_echo_detected_uuid", "");
			if (!uuidStr.isEmpty()) {
				try {
					java.util.UUID targetUuid = java.util.UUID.fromString(uuidStr);
					Entity target = level.getEntity(targetUuid);
					if (target instanceof LivingEntity living && (!living.isAlive() || !living.hasEffect(MobEffects.INVISIBILITY))) {
						entity.getPersistentData().putInt("fractus_echo_detected_time", 0);
						entity.getPersistentData().putString("fractus_echo_detected_uuid", "");
					}
				} catch (Exception ignored) {}
			}
		}

		if (echoScanTicks > 0) {
			Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
			double currentRadius = (25.0 - echoScanTicks) * 0.72;
			spawnEchoScanParticles(level, center, currentRadius);

			AABB boundingBox = entity.getBoundingBox().inflate(currentRadius + 1.5);
			java.util.List<LivingEntity> nearbyInvisibles = level.getEntitiesOfClass(LivingEntity.class, boundingBox, candidate -> {
				if (candidate == entity || !candidate.isAlive()) {
					return false;
				}
				return candidate.hasEffect(MobEffects.INVISIBILITY) && canEcholocateTarget(entity, candidate);
			});

			for (LivingEntity inv : nearbyInvisibles) {
				double dist = inv.distanceTo(entity);
				if (dist >= currentRadius - 0.95 && dist <= currentRadius + 0.95) {
					level.playSound(null, BlockPos.containing(inv.getX(), inv.getY(), inv.getZ()), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5f, 1.4f);
					level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5f, 1.25f);

					level.sendParticles(ParticleTypes.GLOW, inv.getX(), inv.getY() + inv.getBbHeight() * 0.5, inv.getZ(), 45, 0.35, 0.35, 0.35, 0.05);
					level.sendParticles(getAngryLaserParticle(), inv.getX(), inv.getY() + inv.getBbHeight() * 0.5, inv.getZ(), 20, 0.25, 0.25, 0.25, 0.0);

					entity.getPersistentData().putString("fractus_echo_detected_uuid", inv.getUUID().toString());
					entity.getPersistentData().putInt("fractus_echo_detected_time", 180); // 9 seconds

					faceTarget(entity, inv);

					entity.getPersistentData().putInt("fractus_echo_scan_ticks", 0);
					break;
				}
			}
		} else if (detectedTime <= 0) {
			AABB boundingBox = entity.getBoundingBox().inflate(15.0);
			java.util.List<LivingEntity> nearbyInvisibles = level.getEntitiesOfClass(LivingEntity.class, boundingBox, candidate -> {
				if (candidate == entity || !candidate.isAlive()) {
					return false;
				}
				return candidate.hasEffect(MobEffects.INVISIBILITY) && canEcholocateTarget(entity, candidate);
			});

			if (!nearbyInvisibles.isEmpty()) {
				int clickTimer = tickStoredTimer(entity, "fractus_echo_click_timer");
				if (clickTimer <= 0) {
					level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), SoundEvents.DISPENSER_FAIL, SoundSource.HOSTILE, 1.3f, 0.75f);
					entity.getPersistentData().putInt("fractus_echo_click_timer", 35 + entity.getRandom().nextInt(20));
				}

				int scanCooldown = tickStoredTimer(entity, "fractus_echo_scan_cooldown");
				if (scanCooldown <= 0) {
					entity.getPersistentData().putInt("fractus_echo_scan_ticks", 25);
					entity.getPersistentData().putInt("fractus_echo_scan_cooldown", 140);
				}
			}
		}
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
		double height = angry ? ANGRY_HOVER_HEIGHT : HOVER_HEIGHT;

		if (isEscapedContainmentDimension(entity)) {
			height += ESCAPED_CONTAINMENT_HOVER_BONUS;
		}

		if (entity.level().dimension().location().toString().equals("the_backwoods:the_sub_strata")) {
			if (entity instanceof Mob mob && mob.getTarget() instanceof Player) {
				height -= SUB_STRATA_PLAYER_COMBAT_HOVER_REDUCTION;
			}
		}

		return height;
	}

	private static void spawnLaser(ServerLevel level, Vec3 start, Vec3 end, double spacing, double jitter, boolean firing, boolean angry) {
		if (!firing) {
			return; // Completely remove pre-aiming laser particles!
		}

		net.minecraft.core.particles.ParticleOptions particle = laserParticle(angry);
		level.sendParticles(particle, end.x, end.y, end.z, 10, 0.08, 0.08, 0.08, 0.0);

		if (net.mcreator.thebackwoods.FractusLaserBeam.USE_OLD_LASER_PARTICLES) {
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
	}

	private static void spawnTelekinesisRay(ServerLevel level, Vec3 start, Vec3 end) {
		Vec3 line = end.subtract(start);
		double length = line.length();

		if (length < 0.01) {
			return;
		}

		Vec3 direction = line.normalize();
		Vec3 up = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 side = direction.cross(up).normalize();
		Vec3 verticalSide = direction.cross(side).normalize();

		double spacing = 0.12; // tighter spacing for density
		double radius = 0.045; // thin cylinder radius
		double time = level.getGameTime() * 0.32;

		for (double d = 0.0; d <= length; d += spacing) {
			Vec3 pos = start.add(direction.scale(d));
			// Clean helix forming a thin beautiful cylinder
			double angle = (d * 5.0) + time;
			Vec3 offset = side.scale(Math.cos(angle) * radius).add(verticalSide.scale(Math.sin(angle) * radius));
			pos = pos.add(offset);
			level.sendParticles(getBurstChargeStartParticle(), pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static void spawnChargeBeamHum(ServerLevel level, Vec3 start, Vec3 end, double progress, boolean angry) {
		// Completely removed old pre-aiming lasers per instructions
	}

	private static void spawnChargeParticles(ServerLevel level, Entity entity, int chargeTicks, boolean angry) {
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
		Vec3 center = laserStart(entity);

		for (int i = 0; i < 22; i++) {
			double angle = Math.PI * 2.0 * i / 22.0;
			double radius = 0.18 + i * 0.012;
			Vec3 pos = center.add(Math.cos(angle) * radius, (entity.getRandom().nextDouble() - 0.5) * 0.35, Math.sin(angle) * radius);
			level.sendParticles(getNormalLaserParticle(), pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.0);
		}
	}

	private static void spawnSuppressionPulse(ServerLevel level, Entity entity, int suppressionTicks) {
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
		Vec3 center = laserStart(entity);
		Vec3 movement = entity.getDeltaMovement();

		for (int i = 0; i < 5; i++) {
			Vec3 pos = center.subtract(movement.scale(i * 0.7));
			level.sendParticles(laserParticle(isAngry(entity)), pos.x, pos.y, pos.z, 1, 0.06, 0.06, 0.06, 0.0);
		}
	}

	private static void spawnVulnerabilityParticles(ServerLevel level, Entity entity, int vulnerabilityTicks) {
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
		Vec3 center = laserStart(entity);
		int count = angry ? 42 : 26;
		double radius = angry ? 0.42 : 0.30;

		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2.0 * i / count;
			Vec3 pos = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
			level.sendParticles(laserParticle(angry), pos.x, pos.y, pos.z, 1, 0.025, 0.025, 0.025, 0.0);
		}

		level.sendParticles(laserParticle(angry), center.x, center.y, center.z, angry ? 18 : 10, 0.08, 0.08, 0.08, 0.0);
	}

	private static void spawnFireStartWarning(ServerLevel level, Entity entity, boolean angry) {
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

	private record LaserHit(Vec3 location, Entity entity, BlockPos blockPos, Direction blockFace) {
	}
}
// 1.21.1