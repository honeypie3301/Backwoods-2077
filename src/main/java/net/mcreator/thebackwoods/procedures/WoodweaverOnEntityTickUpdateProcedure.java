package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.RandomSource;
import net.mcreator.thebackwoods.entity.WoodweaverEntity;

import java.util.List;

@EventBusSubscriber
public class WoodweaverOnEntityTickUpdateProcedure {
	public static boolean WEAVER_DEBUG = false;
	public static boolean HYPNO_EFFECTS_ENABLED = false; // Hypnosis special/visual effects disabled by default
	public static boolean WEAVER_LEG_WALL_AVOIDANCE = true; // Toggleable, helps prevent spider legs from clipping through walls
	public static double MAX_CLAW_BLOCK_HARDNESS = 10.0; // Maximum block hardness breakable by claw attacks

	// ==========================================
	// --- WEAVER CONFIGURABLE AI SETTINGS ------
	// ==========================================
	
	// Encounter Phase & Mysterious Sightings (Dormant vs Awakened)
	public static final int ENCOUNTER_PHASE_DORMANT = 0; // Mysterious sightings / holds distant perimeter
	public static final int ENCOUNTER_PHASE_AWAKENED = 1; // Full Boss AI (Leap, Hypno, Melee, Combos, Mining)

	public static double DORMANT_MIN_DISTANCE = 60.0; // Distance Weaver seeks to maintain from survival players (60 blocks)
	public static double DORMANT_CORNERED_DISTANCE = 24.0; // Distance where Weaver feels trapped/cornered and vanishes
	public static double DORMANT_RETREAT_SPEED = 0.28; // Smooth, deliberate retreat speed
	public static double DORMANT_SIGHTING_MIN = 25.0; // Min range for player sighting gaze detection
	public static double DORMANT_SIGHTING_MAX = 70.0; // Max range for player sighting gaze detection
	public static int SIGHTING_THRESHOLD_TICKS = 18; // ~0.9s of sustained player gaze to trigger retreat into fog

	public static int DREAD_MAX_AWAKEN = 100; // Dread required to awaken into aggressive boss
	public static int DREAD_SIGHTING_GAIN = 15; // Dread gained per completed sighting threshold
	public static int DREAD_SIGHTING_COOLDOWN = 200; // 10s cooldown between dread gains
	public static int DREAD_DISENGAGE_DECAY = 30; // Retains elevated dread after boss encounter ends

	private static final String K_ENCOUNTER_PHASE = "weaver_encounter_phase";
	private static final String K_DREAD_METER = "weaver_dread_meter";
	private static final String K_SIGHTING_TICKS = "weaver_sighting_ticks";
	private static final String K_DREAD_GAIN_CD = "weaver_dread_gain_cd";
	private static final String K_PREV_BODY_YAW = "weaver_prev_body_yaw";
	private static final String K_RETREAT_TICKS = "weaver_retreat_ticks";
	private static final String K_VANISH_COOLDOWN = "weaver_vanish_cd";
	
	// Hypnosis and Stare Max Ranges & Conditions (Strict 32-block cap)
	public static double HYPNO_MAX_RANGE = 32.0; // All Hypnosis modes cap strictly at 32 block range
	public static float INCINERATE_CRITICAL_HP_RATIO = 0.35F; // Incinerate mode is ONLY allowed if Weaver HP <= 35%

	// Retreat, Blindness, and Teleport Escape Configurable Settings
	public static double RETREAT_TRIGGER_RANGE = 24.0; // Distance where Weaver feels trapped/cornered and vanishes (configurable, default 24.0)
	public static double RETREAT_BLIND_RANGE = 40.0; // Range around Weaver to blind players when vanishing/teleporting away (configurable)
	public static int RETREAT_BLIND_DURATION = 20; // Duration of blindness effect in ticks (1 second, configurable)
	public static double RETREAT_TELEPORT_MIN_DIST = 75.0; // Minimum teleport escape distance (configurable)
	public static double RETREAT_TELEPORT_MAX_DIST = 100.0; // Maximum teleport escape distance (configurable)

	// Wither Storm-style Tractor Beam field & speed constants
	public static double BEAM_SIZE_MULTIPLIER = 3.5; // Expanding cone width multiplier
	public static double TRACTOR_BEAM_MAX_RANGE = 32.0; // Maximum effective range of hypnosis tractor beam (32 blocks)
	public static double DIRECT_STARE_MAX_RANGE = 32.0; // Direct stare detection & pull range (32 blocks)
	public static double TRACTOR_BEAM_BASE_SPEED = 0.50; // Base tractor beam velocity pull speed
	public static double HYPNO_HOLD_DISTANCE = 6.0; // Minimum hypnosis holding distance between target center and Weaver beam origin
	public static double HYPNO_HOLD_HEIGHT = 4.5; // Desired target levitation height above Weaver base feet level

	// Configurable distance under which the target's camera is locked onto the Weaver during hypnosis
	private static final double HYPNO_SIGHT_LOCK_DISTANCE = 8.5;

	// Transition timing from start hypno animation to looping orb animation (ticks)
	public static int HYPNO_PULL_START_TICK = 20; // 1.0s stationary telegraph window before tractor beam pull begins
	private static final int HYPNO_ORB_TRANSITION_TICKS = 60; // 3.0s (60 ticks)
	
	// Hypnosis total holding/active duration (ticks) of telekinetic suspension
	private static final int HYPNO_HOLD_DURATION = 160;

	// Hypnosis closing reversed animation duration variants (ticks)
	public static int HYPNO_CLOSING_DURATION = 30;

	// Post-hypnosis attack lockout duration (ticks) before Weaver can melee attack target again
	public static int HYPNO_RECOVERY_TICKS = 15;

	// Hypnosis global cooldown across all modes (ticks)
	public static int HYPNO_GLOBAL_COOLDOWN = 500; // Default global cooldown (25 seconds)
	public static int HYPNO_LEECH_COOLDOWN = 500; // Configurable global cooldown specifically applied when leech mode is used (25 seconds)

	// Projectile parry / swat cooldown (ticks) before Weaver can swat projectiles again
	public static int PARRY_COOLDOWN = 100; // 5 seconds default parry cooldown

	// Configurable charge teleport settings (when chasing/separated from player)
	public static int CHARGE_TELEPORT_SEPARATION_TICKS = 100; // Time without proximity (ticks, default 5s / 100 ticks) before charge teleport triggers
	public static int CHARGE_TELEPORT_MIN_DURATION = 40; // Doubled minimum windup charge time (ticks, 3.0s / 60 ticks)
	public static int CHARGE_TELEPORT_MAX_DURATION = 100; // Doubled maximum windup charge time (ticks, 6.0s / 120 ticks)
	public static int CHARGE_TELEPORT_COOLDOWN = 200; // Configurable post-teleport cooldown (ticks, default 10s / 200 ticks)

	// Configurable Kool-Aid Man / Ramming Dash Settings
	public static boolean ENABLE_DASH = true; // Toggleable, enabled by default
	public static double DASH_MAX_SPEED = 1.00; // 20 blocks per second (1.00 blocks/tick)
	public static float DASH_MAX_BLOCK_HARDNESS = 20.0F; // Breaks blocks with hardness under 20 (stone, wood, brick, etc.)
	public static float DASH_IMPACT_DAMAGE = 32.0F; // 32 damage on impact
	public static int DASH_FATIGUE_TICKS = 20; // 1.0 second (20 ticks) frozen post-fatigue
	public static int DASH_COOLDOWN_TICKS = 420; // 21 seconds cooldown (increased by 75%)
	public static double DASH_MIN_DISTANCE = 12.0; // Min distance to initiate dash
	public static double DASH_MAX_DISTANCE = 64.0; // Max distance to initiate dash
	public static double TARGET_FOLLOW_RANGE = 128.0; // Target following and detection range expanded to 128 blocks

	// Configurable amplified trajectory height boost for Woodweaver jumps
	public static double LEAP_HEIGHT_BOOST = 0.65;
	
	// Cooldown ticks between leaping attempts (10 seconds default)
	private static final int LEAP_COOLDOWN_DEFAULT = 200;
	
	// Minimum distance to target to trigger leaping
	private static final double LEAP_MIN_DISTANCE = 8.0;
	
	// Maximum distance to target to trigger leaping
	private static final double LEAP_MAX_DISTANCE = 48.0;
	
	// Trajectory speed factor/multiplier of leaping
	private static final double LEAP_SPEED_MULTIPLIER = 1.32;
	
	// Damage dealt by landing shockwave
	private static final double LANDING_DAMAGE = 16.0;
	
	// Block destruction/shockwave radius on landing
	private static final double LANDING_RADIUS = 3.0;
	
	// Toggle to enable/disable block destruction on shockwave landing
	private static final boolean IS_SHOCKWAVE_DESTRUCTION_ENABLED = false;

	// Toggle to enable/disable wall destruction when Weaver leaps into walls
	public static boolean IS_LEAP_WALL_DESTRUCTION_ENABLED = true;
	
	// Swipe & reach range of melee claw attacks (synced to 5.0 block reach animations)
	private static final double MELEE_ATTACK_RANGE = 5.0;
	
	// Delay ticks before physically jumping while the leaping animation is playing
	public static int LEAP_PREPARATION_TICKS = 20;

	// Duration of the landing animation freeze / recovery state (ticks).
	public static int LANDING_ANIM_DURATION = 25;

	// Total duration of the melee claw attack animations (ticks).
	public static int MELEE_ATTACK_DURATION = 40;

	// Default peak tick offset for damage dealing
	public static int ATTACK_DAMAGE_TICK = 15;
	
	// Configurable claw hit offsets for 5-block reach melee animations
	public static double CLAW_FRONT_OFFSET = 4.2;
	public static double CLAW_SIDE_OFFSET = 1.2;
	public static double CLAW_HEIGHT_OFFSET = 1.2;
	public static double CLAW_HIT_RADIUS = 2.8;

	// Toggle to enable/disable block destruction on claw swipes
	private static final boolean IS_CLAW_DESTRUCTION_ENABLED = true;

	// ==========================================

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		if (world.isClientSide()) {
			return;
		}

		if (!(entity instanceof Mob mob))
			return;

		CompoundTag mData = mob.getPersistentData();

		// CUSTOM DEATH SEQUENCE INTERCEPTOR
		boolean isDying = persistentBoolean(mData, "is_dying", false);
		if (!isDying && (mob.getHealth() <= 0.01F || mob.isDeadOrDying())) {
			isDying = true;
			mData.putBoolean("is_dying", true);
			mData.putInt("death_ticks", 0);
			mData.putDouble("death_start_y", mob.getY());
			mob.setHealth(1.0F); // lock health during sequence
			mob.setInvulnerable(true);
			mob.setNoAi(true);
			stopMobMotion(mob);
			mob.getNavigation().stop();
			mob.setTarget(null);
		}

		if (isDying) {
			stopMobMotion(mob);
			mob.getNavigation().stop();
			mob.setTarget(null);
			mob.setHealth(1.0F); // lock health during sequence

			int deathTicks = persistentInt(mData, "death_ticks", 0) + 1;
			mData.putInt("death_ticks", deathTicks);
			int maxDeathTicks = 200; // 10 seconds dramatic cinematic sequence

			double progress = Math.min(1.0, (double) deathTicks / (double) maxDeathTicks);

			// Check ceiling room allowing upward lift (levitation speed 0.12 blocks/tick up to 18 blocks)
			double startY = persistentDouble(mData, "death_start_y", mob.getY());
			double targetMaxLift = 18.0;
			double desiredLift = Math.min(targetMaxLift, deathTicks * 0.12);
			
			boolean hasCeilingSpace = true;
			for (int h = 1; h <= (int) Math.ceil(desiredLift) + (int) Math.ceil(mob.getBbHeight()); h++) {
				BlockPos checkPos = BlockPos.containing(mob.getX(), startY + h, mob.getZ());
				BlockState state = world.getBlockState(checkPos);
				if (state.isSolid() || !state.isAir()) {
					hasCeilingSpace = false;
					break;
				}
			}

			if (hasCeilingSpace) {
				mob.teleportTo(mob.getX(), startY + desiredLift, mob.getZ());
			}

			// Cinematic dense spherical flash & audio buildup
			if (world instanceof ServerLevel sl) {
				double bbHeight = Math.max(3.0, mob.getBbHeight());
				double centerY = mob.getY() + (bbHeight * 0.5);
				
				// Dense spherical aura growing tightly from 0.6 to 3.2 blocks around the bounding box center
				double sphereRadius = 0.6 + (progress * 2.6);

				int flashCount = 6 + (int) (progress * 44.0); // Dense flash cloud (6 to 50 particles)
				sl.sendParticles(
					ParticleTypes.FLASH,
					mob.getX(),
					centerY,
					mob.getZ(),
					flashCount,
					sphereRadius,
					sphereRadius,
					sphereRadius,
					0.01
				);

				// Secondary energy sparks inside the dense spherical aura
				int sparkCount = 4 + (int) (progress * 26.0);
				sl.sendParticles(
					ParticleTypes.END_ROD,
					mob.getX(),
					centerY,
					mob.getZ(),
					sparkCount,
					sphereRadius * 0.8,
					sphereRadius * 0.8,
					sphereRadius * 0.8,
					0.03
				);
				
				// Roaring beacon hum building up to ear-shaking volume (up to 13.5F volume)
				if (deathTicks % 5 == 0) {
					float beaconVolume = 2.0F + (float) (progress * 11.5F); // Scales up to 13.5F
					float beaconPitch = 0.4F + (float) (progress * 1.3F);   // Pitch ramps from 0.4F to 1.7F
					sl.playSound(null, mob.blockPosition(), getSound(SoundEvents.BEACON_AMBIENT), SoundSource.HOSTILE, beaconVolume, beaconPitch);
				}
			}

			if (deathTicks >= maxDeathTicks) { // End of 10-second cinematic sequence
				// Massive multi-layered detonation with real environmental terrain damage
				if (world instanceof ServerLevel sl) {
					double bbHeight = Math.max(3.0, mob.getBbHeight());
					double centerY = mob.getY() + (bbHeight * 0.5);
					
					sl.playSound(null, mob.blockPosition(), getSound(SoundEvents.GENERIC_EXPLODE), SoundSource.HOSTILE, 15.0F, 0.4F);
					sl.playSound(null, mob.blockPosition(), getSound(SoundEvents.LIGHTNING_BOLT_THUNDER), SoundSource.HOSTILE, 6.0F, 0.6F); // Reduced thunder volume by half
					
					sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, mob.getX(), centerY, mob.getZ(), 15, 2.0, 2.0, 2.0, 0.1);
					sl.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), centerY, mob.getZ(), 120, 3.5, 3.5, 3.5, 0.2);
					sl.sendParticles(ParticleTypes.FLASH, mob.getX(), centerY, mob.getZ(), 100, 4.0, 4.0, 4.0, 0.1);

					// Real environmental block-destroying explosion
					sl.explode(mob, mob.getX(), centerY, mob.getZ(), 10.0F, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
				}
				mob.discard();
			}

			syncWeaverEntityData(mob, false, mData);
			return;
		}

		// ENCOUNTER PHASE MANAGEMENT (Dormant Mysterious Sightings vs Awakened Boss AI)
		LivingEntity recentAttacker = mob.getLastHurtByMob();
		if (recentAttacker != null && recentAttacker.isAlive()) {
			mData.putInt(K_ENCOUNTER_PHASE, ENCOUNTER_PHASE_AWAKENED);
			mData.putInt(K_DREAD_METER, DREAD_MAX_AWAKEN);
		}

		int dreadMeter = persistentInt(mData, K_DREAD_METER, 0);
		if (dreadMeter >= DREAD_MAX_AWAKEN) {
			mData.putInt(K_ENCOUNTER_PHASE, ENCOUNTER_PHASE_AWAKENED);
		}

		int encounterPhase = persistentInt(mData, K_ENCOUNTER_PHASE, ENCOUNTER_PHASE_DORMANT);
		if (encounterPhase == ENCOUNTER_PHASE_DORMANT) {
			handleDormantPhase(world, mob, mData);
			return;
		}

		// TELEPORT CHARGING STATE (Underpowered/Trapped when target is overpowered)
		boolean tpCharging = persistentBoolean(mData, "weaver_tp_charging", false);
		if (tpCharging) {
			stopMobMotion(mob);
			mob.getNavigation().stop();
			mob.setTarget(null);

			int timer = persistentInt(mData, "weaver_tp_timer", 0) + 1;
			mData.putInt("weaver_tp_timer", timer);

			double progress = Math.min(1.0, (double) timer / 60.0);

			// Always look directly at the target during teleport charging to prevent visual twitching/yaw fighting
			int savedTargetId = persistentInt(mData, "weaver_tp_target_id", 0);
			if (savedTargetId != 0) {
				Entity e = mob.level().getEntity(savedTargetId);
				if (e instanceof LivingEntity tpTarget && tpTarget.isAlive()) {
					double dx = tpTarget.getX() - mob.getX();
					double dz = tpTarget.getZ() - mob.getZ();
					float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;

					mob.setYRot(targetYaw);
					mob.setYBodyRot(targetYaw);
					mob.setYHeadRot(targetYaw);
					mob.yRotO = targetYaw;
					mob.yBodyRotO = targetYaw;
					mob.yHeadRotO = targetYaw;

					mob.getLookControl().setLookAt(tpTarget.getX(), tpTarget.getEyeY(), tpTarget.getZ(), 100.0F, 100.0F);
				}
			}

			if (world instanceof ServerLevel sl) {
				int particleCount = 1 + (int) (progress * 15.0);
				sl.sendParticles(
					ParticleTypes.FLASH,
					mob.getX() + (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth() * 1.5,
					mob.getY() + mob.getRandom().nextDouble() * mob.getBbHeight(),
					mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth() * 1.5,
					particleCount,
					0.1, 0.1, 0.1, 0.05
				);
				if (timer % 10 == 0) {
					sl.playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.3F + (float)progress * 0.3F);
				}
			}

			if (timer >= 60) {
				mData.putBoolean("weaver_tp_charging", false);
				mData.putInt("weaver_tp_timer", 0);
				performWeaverTpOrDespawn(world, mob);
			}

			syncWeaverEntityData(mob, false, mData);
			return;
		}

		// Update stationary rotation tracking during Awakened phase
		updateStationaryRotation(mob, mData);

		// POST-FATIGUE STATE (Triggered after Flash Vaporize Mode ends):
		// Freeze movement, prevent rotation changes, and lockout all following abilities
		int postFatigueTicks = persistentInt(mData, "weapon_post_fatigue_ticks", 0);
		if (postFatigueTicks > 0) {
			mData.putInt("weapon_post_fatigue_ticks", postFatigueTicks - 1);
			stopMobMotion(mob);
			mob.getNavigation().stop();
			mob.setTarget(null);

			double startX = persistentDouble(mData, "hypno_start_x", mob.getX());
			double startY = persistentDouble(mData, "hypno_start_y", mob.getY());
			double startZ = persistentDouble(mData, "hypno_start_z", mob.getZ());
			if (mob.distanceToSqr(startX, mob.getY(), startZ) > 0.001) {
				mob.teleportTo(startX, mob.getY(), startZ);
			}

			if (mData.contains("post_fatigue_yaw")) {
				float lockedYaw = persistentFloat(mData, "post_fatigue_yaw", mob.getYRot());
				mob.setYRot(lockedYaw);
				mob.yRotO = lockedYaw;
				mob.setYBodyRot(lockedYaw);
				mob.setYHeadRot(lockedYaw);
				mob.yHeadRotO = lockedYaw;
				mob.yBodyRotO = lockedYaw;
				double rad = Math.toRadians(lockedYaw);
				double lookX = mob.getX() - Math.sin(rad) * 10.0;
				double lookZ = mob.getZ() + Math.cos(rad) * 10.0;
				mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 30.0F, 30.0F);
			}
			updateStateAndLog(mob, mData, "POST_HYPNO_FATIGUE");
			syncWeaverEntityData(mob, false, mData);
			return;
		} else if (mData.contains("post_fatigue_yaw")) {
			mData.remove("post_fatigue_yaw");
			mData.remove("weaver_hypnosis_yaw");
			mData.remove("hypno_start_x");
			mData.remove("hypno_start_y");
			mData.remove("hypno_start_z");
			int savedTargetId = persistentInt(mData, "weapon_flash_saved_target", 0);
			if (savedTargetId != 0) {
				Entity raw = mob.level().getEntity(savedTargetId);
				if (raw instanceof LivingEntity le && le.isAlive()) {
					mob.setTarget(le);
				}
				mData.remove("weapon_flash_saved_target");
			}
		}

		// WEAPONIZED FLASH GUARD (Interception at top of tick to prevent target pursuit or standard AI from running)
		int activeFlashPhase = persistentInt(mData, "weapon_flash_phase", 0);
		if (activeFlashPhase > 0) {
			clearOtherStates(mData, "FLASH");
			if (mob.getTarget() != null) {
				mData.putInt("weapon_flash_saved_target", mob.getTarget().getId());
				mob.setTarget(null);
			}
		}

		// INITIALIZATION & STALE STATE CLEANUP ON FIRST TICK / SPAWN
		applyPermanentMaxHpBonus(mob, mData, 0.0F);
		processLightExtinctionPulseTick(mob, mData);

		// Cognitive decision-making: decay success/failure biases slightly over time (0.5% per tick towards 0.0)
		if (mData.contains("ai_bias_infect")) mData.putDouble("ai_bias_infect", mData.getDouble("ai_bias_infect") * 0.995);
		if (mData.contains("ai_bias_leech")) mData.putDouble("ai_bias_leech", mData.getDouble("ai_bias_leech") * 0.995);
		if (mData.contains("ai_bias_incinerate")) mData.putDouble("ai_bias_incinerate", mData.getDouble("ai_bias_incinerate") * 0.995);
		if (mData.contains("ai_bias_vaporize")) mData.putDouble("ai_bias_vaporize", mData.getDouble("ai_bias_vaporize") * 0.995);

		// Track damage taken by Weaver for Leech mode condition
		float currentHp = mob.getHealth();
		float lastHp = persistentFloat(mData, "weaver_track_last_hp", currentHp);
		int recentDmgTimer = persistentInt(mData, "weaver_recent_damage_timer", 0);
		int recentDmgHits = persistentInt(mData, "weaver_recent_damage_hits", 0);

		if (recentDmgTimer > 0) {
			mData.putInt("weaver_recent_damage_timer", recentDmgTimer - 1);
		} else {
			mData.putInt("weaver_recent_damage_hits", 0);
		}

		if (currentHp < lastHp - 0.2F) {
			recentDmgHits++;
			mData.putInt("weaver_recent_damage_hits", recentDmgHits);
			mData.putInt("weaver_recent_damage_timer", 160); // 8-second damage window
		}
		mData.putFloat("weaver_track_last_hp", currentHp);

		if (world instanceof ServerLevel serverLevel) {
			tickWeightCollapse(serverLevel, mob, mData);
		}

		// Cooldown ticks management
		int leapCooldown = persistentInt(mData, "leap_cooldown", 0);
		if (leapCooldown > 0) {
			mData.putInt("leap_cooldown", leapCooldown - 1);
		}

		int attackTimer = persistentInt(mData, "attack_timer", 0);
		if (attackTimer > 0) {
			mData.putInt("attack_timer", attackTimer - 1);
		}

		int hypnoCooldown = persistentInt(mData, "hypno_cooldown", 0);
		if (hypnoCooldown > 0) {
			mData.putInt("hypno_cooldown", hypnoCooldown - 1);
		}

		int hypnoAttackLockout = persistentInt(mData, "hypno_attack_lockout", 0);
		if (hypnoAttackLockout > 0) {
			mData.putInt("hypno_attack_lockout", hypnoAttackLockout - 1);
		}

		int parryCooldown = persistentInt(mData, "parry_cooldown", 0);
		if (parryCooldown > 0) {
			mData.putInt("parry_cooldown", parryCooldown - 1);
		}

		// Single Authoritative Hypnosis Phase Integer (0=NONE, 1=OPENING, 2=OPEN/ORB, 3=CLOSING)
		int hypnoPhase = persistentInt(mData, "hypno_phase", 0);

		if (hypnoPhase > 0) {
			clearOtherStates(mData, "HYPNO");
			mob.setTarget(null); // Unset target so MeleeAttackGoal stops running during hypnosis

			// Hard lock position, movement and rotation to start position during opening and active hold (Phases 1 & 2 only)
			if (hypnoPhase == 1 || hypnoPhase == 2) {
				double startX = persistentDouble(mData, "hypno_start_x", mob.getX());
				double startY = persistentDouble(mData, "hypno_start_y", mob.getY());
				double startZ = persistentDouble(mData, "hypno_start_z", mob.getZ());

				stopMobMotion(mob);
				mob.getNavigation().stop();
				if (mob.distanceToSqr(startX, mob.getY(), startZ) > 0.001) {
					mob.teleportTo(startX, mob.getY(), startZ);
				}
			}

			int hits = persistentInt(mData, "hypno_hits_received", 0);
			int targetId = persistentInt(mData, "hypno_target_id", 0);
			List<LivingEntity> hypnoTargets = getHypnoTargets(mob, mData);
			LivingEntity primaryTarget = hypnoTargets.isEmpty() ? null : hypnoTargets.get(0);
			Player hypnoPlayerTarget = (primaryTarget instanceof Player p) ? p : null;

			// HYPER-AWARE LINE OF SIGHT: Check LOS with spotlight grace period
			boolean hasDirectLos = false;
			if (primaryTarget != null && primaryTarget.isAlive()) {
				Vec3 startEye = mob.getEyePosition(1.0F);
				Vec3 targetEye = primaryTarget.getEyePosition(1.0F);
				BlockHitResult hit = world.clip(new net.minecraft.world.level.ClipContext(
					startEye, targetEye,
					net.minecraft.world.level.ClipContext.Block.COLLIDER,
					net.minecraft.world.level.ClipContext.Fluid.NONE,
					mob
				));
				if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK || mob.hasLineOfSight(primaryTarget)) {
					hasDirectLos = true;
				}
			}

			int losLostTicks = persistentInt(mData, "hypno_los_lost_ticks", 0);
			if (!hasDirectLos) {
				losLostTicks++;
				mData.putInt("hypno_los_lost_ticks", losLostTicks);
			} else {
				mData.putInt("hypno_los_lost_ticks", 0);
			}

			boolean losGraceExpired = (losLostTicks >= 10); // ~0.5 sec spotlight grace period before canceling hypnosis

			// Phase 1 (Opening/Charging): Damage taken does NOT interrupt, and LOS loss does NOT cut off charging animation.
			boolean targetLostInPhase1 = (hypnoTargets.isEmpty() || primaryTarget == null || !primaryTarget.isAlive() 
				|| (hypnoPlayerTarget != null && (hypnoPlayerTarget.isCreative() || hypnoPlayerTarget.isSpectator())) 
				|| primaryTarget.distanceTo(mob) > HYPNO_MAX_RANGE * 1.5);

			// Phase 2 (Active Hold): Damage taken does NOT interrupt. Cancels if target dies, escapes range, or LOS grace expires.
			boolean targetLostInPhase2 = (hypnoTargets.isEmpty() || primaryTarget == null || !primaryTarget.isAlive() 
				|| (hypnoPlayerTarget != null && (hypnoPlayerTarget.isCreative() || hypnoPlayerTarget.isSpectator())) 
				|| primaryTarget.distanceTo(mob) > HYPNO_MAX_RANGE || losGraceExpired);

			if (hypnoPhase == 1) { // HYPNO_PHASE_OPENING (Ticks 0 - 59)
				String currentHypnoMode = persistentString(mData, "hypno_mode", "infect");
				if ("incinerate".equals(currentHypnoMode) && (mob.getHealth() / mob.getMaxHealth()) > INCINERATE_CRITICAL_HP_RATIO) {
					currentHypnoMode = (primaryTarget != null && targetHasCellularCollapse(primaryTarget)) ? "leech" : "infect";
					mData.putString("hypno_mode", currentHypnoMode);
				}
				updateStateAndLog(mob, mData, currentHypnoMode.toUpperCase() + "_OPENING");
				if ("incinerate".equals(currentHypnoMode)) {
					mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 40, 0, false, false, false));
				} else {
					mob.removeEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
				}
				updateAndApplyHypnosisYaw(mob, mData, primaryTarget, false);

				boolean gazeBroken = false; // Gaze/Staring at Weaver is no longer required!

				if (targetLostInPhase1 || gazeBroken) {
					// ADAPTIVE TARGET SWITCHING: Prioritize recent attacker who hit Weaver or broke hypnosis
					LivingEntity lastAttacker = mob.getLastHurtByMob();
					if (lastAttacker != null && lastAttacker.isAlive() && !isWoodboundOrWeaver(lastAttacker)) {
						mob.setTarget(lastAttacker);
					}

					setHypnoPhase(mData, 3);
					updateStateAndLog(mob, mData, persistentString(mData, "hypno_mode", "infect").toUpperCase() + "_CLOSING");
					mData.putInt("closing_hypno_ticks", 0);
					mData.putInt("hypno_closing_duration", 20);
					mData.putBoolean("hypno_interrupted", true);
					applyHypnoCooldown(mData);
					mData.putInt("hypno_los_lost_ticks", 0);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 6.0F, 0.50F);

					for (LivingEntity target : hypnoTargets) {
						if (target != null) {
							target.setDeltaMovement(0, 0, 0);
							target.hasImpulse = true;
							if (target instanceof ServerPlayer sp) {
								sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
							}
						}
					}

					syncWeaverEntityData(mob, false, mData);
					return;
				}

				int hypnoTicks = persistentInt(mData, "hypno_ticks", 0) + 1;
				mData.putInt("hypno_ticks", hypnoTicks);

				if (hypnoTicks == 1) {
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 7.5F, 0.45F);
					// Start light extinction pulse (up to 64 blocks)
					mData.putInt("extinction_pulse_radius", 1);
				}

				// TARGET PULL STARTS EARLY with linear force ramp up to tick 60
				if (hypnoTicks >= HYPNO_PULL_START_TICK) {
					double pullRamp = Math.min(1.0, (double)(hypnoTicks - HYPNO_PULL_START_TICK + 1) / (HYPNO_ORB_TRANSITION_TICKS - HYPNO_PULL_START_TICK));
					applyTractorBeamPullMulti(mob, hypnoTargets, pullRamp);
				}

				// At exact tick 60: Seamless transition to HYPNO_PHASE_OPEN (Orb Loop)
				if (hypnoTicks >= HYPNO_ORB_TRANSITION_TICKS) {
					setHypnoPhase(mData, 2); // Phase 2: HYPNO_OPEN (Orb Loop)
					updateStateAndLog(mob, mData, persistentString(mData, "hypno_mode", "infect").toUpperCase() + "_OPEN");
					mData.putInt("hypno_ticks", 0);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 7.5F, 0.50F);
				}

				syncWeaverEntityData(mob, false, mData);
				return;

			} else if (hypnoPhase == 2) { // HYPNO_PHASE_OPEN (Orb Loop Hold, Ticks 0 - 160)
				String currentHypnoMode = persistentString(mData, "hypno_mode", "infect");
				updateStateAndLog(mob, mData, currentHypnoMode.toUpperCase() + "_OPEN");
				if ("incinerate".equals(currentHypnoMode) || "vaporize".equals(currentHypnoMode)) {
					mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 40, 0, false, false, false));
				}
				updateAndApplyHypnosisYaw(mob, mData, primaryTarget, false);

				if (targetLostInPhase2) {
					setHypnoPhase(mData, 3);
					updateStateAndLog(mob, mData, persistentString(mData, "hypno_mode", "infect").toUpperCase() + "_CLOSING");
					mData.putInt("closing_hypno_ticks", 0);
					mData.putInt("hypno_closing_duration", 20);
					mData.putBoolean("hypno_interrupted", true);
					mData.putInt("hypno_hits_received", 0);
					applyHypnoCooldown(mData);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 6.0F, 0.50F);

					for (LivingEntity target : hypnoTargets) {
						if (target != null) {
							target.setDeltaMovement(0, 0, 0);
							target.hasImpulse = true;
							if (target instanceof ServerPlayer sp) {
								sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
							}
						}
					}

					syncWeaverEntityData(mob, false, mData);
					return;
				}

				int hypnoTicks = persistentInt(mData, "hypno_ticks", 0) + 1;
				mData.putInt("hypno_ticks", hypnoTicks);

				String hypnoMode = persistentString(mData, "hypno_mode", "infect");
				if ("incinerate".equals(hypnoMode) || "vaporize".equals(hypnoMode)) {
					if (hypnoTicks % 15 == 0) {
						mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 6.0F, 0.5F + (hypnoTicks / 160.0F));
					}
					if (hypnoTicks % 10 == 0) {
						float progress = (float) hypnoTicks / 160.0F;
						float vol = 1.0F + progress * 4.0F;
						float pitch = 0.6F + progress * 0.9F;
						for (LivingEntity target : hypnoTargets) {
							if (target != null && target.isAlive()) {
								mob.level().playSound(null, target.getX(), target.getY() + 1.0, target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, vol, pitch);
							}
						}
					}
				} else {
					if (hypnoTicks % 25 == 0) {
						mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 6.0F, 0.45F);
					}
				}

				// Smooth Tractor Beam Pull for all active targets
				applyTractorBeamPullMulti(mob, hypnoTargets, 1.0);

				// Apply distinct mode behaviors
				switch (hypnoMode) {
					case "infect": {
						// 1. INFECT MODE: Single-target player corruption (Cellular Collapse effect + spores)
						for (LivingEntity target : hypnoTargets) {
							if (target == null || !target.isAlive()) continue;
							Holder<net.minecraft.world.effect.MobEffect> cellularCollapse = getCellularCollapseEffect();
							if (cellularCollapse != null) {
								int maxDur = persistentInt(mData, "infect_max_duration", 0);
								if (maxDur == 0) {
									maxDur = 2400 + mob.getRandom().nextInt(3601); // 2400 to 6000 ticks (2 to 5 minutes)
									mData.putInt("infect_max_duration", maxDur);
								}
								int currentDur = 0;
								MobEffectInstance existing = target.getEffect(cellularCollapse);
								if (existing != null) {
									currentDur = existing.getDuration();
								}
								int nextDur = Math.min(maxDur, currentDur + 150);
								target.addEffect(new MobEffectInstance(cellularCollapse, nextDur, 0, false, false, true));
							}
						}
						break;
					}
					case "leech": {
						// 2. LEECH MODE: Single-target HP drain (2 to 30 HP total) -> heals Weaver and permanently increases Weaver max HP
						for (LivingEntity target : hypnoTargets) {
							if (target == null || !target.isAlive()) continue;
							if (hypnoTicks % 10 == 0) {
								String leechKey = "hypno_leech_total_" + target.getId();
								float totalLeechToTake = persistentFloat(mData, leechKey, 0.0F);
								if (totalLeechToTake <= 0.0F) {
									totalLeechToTake = 2.0F + mob.getRandom().nextFloat() * 28.0F; // 2 to 30 HP
									mData.putFloat(leechKey, totalLeechToTake);
								}
								float intervalLeech = totalLeechToTake / 16.0F; // 16 intervals during 160 ticks hold
								currentHp = target.getHealth();
								if (currentHp > 1.0F) { // Never kill the target
									float actualDrain = Math.min(intervalLeech, currentHp - 1.0F);
									if (actualDrain > 0.0F) {
										target.setHealth(currentHp - actualDrain);
										applyPermanentMaxHpBonus(mob, mData, actualDrain);
									}
								}
							}
						}
						break;
					}

					case "incinerate": {
						// 4. INCINERATE MODE: Fiery hold (slowness on target + burns and 80 magic damage on finish)
						for (LivingEntity target : hypnoTargets) {
							if (target == null || !target.isAlive()) continue;
							if (HYPNO_EFFECTS_ENABLED) {
								target.addEffect(new MobEffectInstance(getSlownessEffect(), 40, 4, false, false, true));
							}
						}
						break;
					}

					case "vaporize": {
						// 5. VAPORIZE MODE: Vaporizing hold (slowness on target + 50 magic damage and fire on finish)
						for (LivingEntity target : hypnoTargets) {
							if (target == null || !target.isAlive()) continue;
							if (HYPNO_EFFECTS_ENABLED) {
								target.addEffect(new MobEffectInstance(getSlownessEffect(), 40, 4, false, false, true));
							}
						}
						break;
					}
					default:
						break;
				}

				if (hypnoTicks >= HYPNO_HOLD_DURATION) {
					for (LivingEntity target : hypnoTargets) {
						if ("incinerate".equals(hypnoMode) && target != null && target.isAlive()) {
							float dmg = 80.0F;
							target.hurt(mob.damageSources().magic(), dmg);
							target.setRemainingFireTicks(200);
							if (mob.level() instanceof ServerLevel sl) {
								sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 100, 0.5, 0.5, 0.5, 0.2);
								sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 50, 0.5, 0.5, 0.5, 0.05);
							}
						} else if ("vaporize".equals(hypnoMode) && target != null && target.isAlive()) {
							float dmg = 50.0F;
							target.hurt(mob.damageSources().magic(), dmg);
							target.setRemainingFireTicks(200);
							if (mob.level() instanceof ServerLevel sl) {
								sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 100, 0.5, 0.5, 0.5, 0.2);
								sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 50, 0.5, 0.5, 0.5, 0.05);
							}
						}
					}
					
					setHypnoPhase(mData, 3); // Phase 3: HYPNO_CLOSING
					updateStateAndLog(mob, mData, persistentString(mData, "hypno_mode", "infect").toUpperCase() + "_CLOSING");
					mData.putInt("closing_hypno_ticks", 0);
					mData.putBoolean("hypno_interrupted", false);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 6.0F, 0.50F);

					int chosenClosingDuration = 30;
					double weaverHpRatio = mob.getHealth() / mob.getMaxHealth();
					if (hits >= 2 || weaverHpRatio < 0.35) {
						chosenClosingDuration = 20;
					} else if (weaverHpRatio > 0.70 && primaryTarget != null && primaryTarget.distanceTo(mob) <= 5.0) {
						chosenClosingDuration = 60;
					} else {
						chosenClosingDuration = 30;
					}
					mData.putInt("hypno_closing_duration", chosenClosingDuration);

					for (LivingEntity target : hypnoTargets) {
						if (target != null) {
							target.setDeltaMovement(0, -0.05, 0);
							if (target instanceof ServerPlayer sp) {
								sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
							}
						}
					}
				}

				syncWeaverEntityData(mob, false, mData);
				return;

			} else if (hypnoPhase == 3) {
				updateStateAndLog(mob, mData, persistentString(mData, "hypno_mode", "infect").toUpperCase() + "_CLOSING");
				updateAndApplyHypnosisYaw(mob, mData, primaryTarget, false);

				// Release trapped projectiles as hypnosis closes
				releaseTrappedProjectiles(mob);

				int closingTicks = persistentInt(mData, "closing_hypno_ticks", 0) + 1;
				mData.putInt("closing_hypno_ticks", closingTicks);

				for (LivingEntity target : hypnoTargets) {
					if (target != null) {
						target.fallDistance = 0.0F;
					}
				}

				int duration = persistentInt(mData, "hypno_closing_duration", HYPNO_CLOSING_DURATION);
				if (closingTicks >= duration) {
					cancelHypnosisCleanly(mob, mData);
					return;
				}

				syncWeaverEntityData(mob, false, mData);
				return;
			}
		}

		// WEAPONIZED FLASH SPECIAL MINING ABILITY ROUTINE
		int flashPhase = persistentInt(mData, "weapon_flash_phase", 0);
		if (flashPhase > 0) {
			int flashTicks = persistentInt(mData, "weapon_flash_ticks", 0) + 1;
			mData.putInt("weapon_flash_ticks", flashTicks);

			// Hard position anchor and motion cancel during Charge and Beam Flash phases (Phases 1 & 2 only)
			if (flashPhase == 1 || flashPhase == 2) {
				stopMobMotion(mob);
				mob.getNavigation().stop();

				double startX = persistentDouble(mData, "hypno_start_x", mob.getX());
				double startY = persistentDouble(mData, "hypno_start_y", mob.getY());
				double startZ = persistentDouble(mData, "hypno_start_z", mob.getZ());
				if (mob.distanceToSqr(startX, mob.getY(), startZ) > 0.001) {
					mob.teleportTo(startX, mob.getY(), startZ);
				}
			}

			// Resolve active target for vaporize beam aiming (saved target, mob target, last hurt mob, or nearest player)
			LivingEntity flashTarget = null;
			int savedTargetId = persistentInt(mData, "weapon_flash_saved_target", 0);
			if (savedTargetId != 0) {
				Entity rawFlashTarget = mob.level().getEntity(savedTargetId);
				if (rawFlashTarget instanceof LivingEntity le && le.isAlive() && !isWoodboundOrWeaver(le)) {
					flashTarget = le;
				}
			}
			if (flashTarget == null) {
				LivingEntity t = mob.getTarget();
				if (t != null && t.isAlive() && !isWoodboundOrWeaver(t)) {
					flashTarget = t;
				} else {
					LivingEntity hurtBy = mob.getLastHurtByMob();
					if (hurtBy != null && hurtBy.isAlive() && !isWoodboundOrWeaver(hurtBy)) {
						flashTarget = hurtBy;
					}
				}
			}
			if (flashTarget == null && mob.level() instanceof ServerLevel sl) {
				Player nearest = sl.getNearestPlayer(mob, 64.0);
				if (nearest != null && nearest.isAlive() && !nearest.isCreative() && !nearest.isSpectator()) {
					flashTarget = nearest;
				}
			}

			if (flashPhase == 1) { // Charge Phase
				updateStateAndLog(mob, mData, "FLASH_VAPORIZE_CHARGING");
				mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 400, 0, false, false, false));
				boolean isFastCharge = persistentBoolean(mData, "isHypnotizing3", false);
				int chargeTargetTicks = isFastCharge ? 20 : 60;

				if (flashTarget != null) {
					Vec3 flashTargetEye = flashTarget.getEyePosition(1.0F);
					mData.putDouble("flash_last_target_x", flashTargetEye.x);
					mData.putDouble("flash_last_target_y", flashTargetEye.y);
					mData.putDouble("flash_last_target_z", flashTargetEye.z);

					double dx = flashTargetEye.x - mob.getX();
					double dy = flashTargetEye.y - mob.getEyeY();
					double dz = flashTargetEye.z - mob.getZ();
					double horizDist = Math.sqrt(dx * dx + dz * dz);

					float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
					float targetPitch = (float) (-(Math.atan2(dy, Math.max(0.1, horizDist)) * (180.0 / Math.PI)));

					mData.putFloat("weaver_hypnosis_yaw", targetYaw);
					mData.putFloat("flash_saved_yaw", targetYaw);
					mData.putFloat("flash_saved_pitch", targetPitch);

					mob.setYRot(targetYaw);
					mob.yRotO = targetYaw;
					mob.setYBodyRot(targetYaw);
					mob.setYHeadRot(targetYaw);
					mob.yHeadRotO = targetYaw;
					mob.setXRot(targetPitch);
					mob.xRotO = targetPitch;
					mob.getLookControl().setLookAt(flashTargetEye.x, flashTargetEye.y, flashTargetEye.z, 30.0F, 30.0F);
				} else if (mData.contains("flash_last_target_x")) {
					double lastX = mData.getDouble("flash_last_target_x");
					double lastY = mData.getDouble("flash_last_target_y");
					double lastZ = mData.getDouble("flash_last_target_z");
					float savedYaw = persistentFloat(mData, "flash_saved_yaw", persistentFloat(mData, "weaver_hypnosis_yaw", mob.getYRot()));
					float savedPitch = persistentFloat(mData, "flash_saved_pitch", mob.getXRot());

					mob.setYRot(savedYaw);
					mob.yRotO = savedYaw;
					mob.setYBodyRot(savedYaw);
					mob.setYHeadRot(savedYaw);
					mob.yHeadRotO = savedYaw;
					mob.setXRot(savedPitch);
					mob.xRotO = savedPitch;
					mob.getLookControl().setLookAt(lastX, lastY, lastZ, 30.0F, 30.0F);
				} else {
					float lockedFlashYaw = persistentFloat(mData, "weaver_hypnosis_yaw", mob.getYRot());
					mob.setYRot(lockedFlashYaw);
					mob.yRotO = lockedFlashYaw;
					mob.setYBodyRot(lockedFlashYaw);
					mob.setYHeadRot(lockedFlashYaw);
					mob.yHeadRotO = lockedFlashYaw;
					mob.yBodyRotO = lockedFlashYaw;

					double rad = Math.toRadians(lockedFlashYaw);
					double lookX = mob.getX() - Math.sin(rad) * 10.0;
					double lookZ = mob.getZ() + Math.cos(rad) * 10.0;
					mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 30.0F, 30.0F);
				}

				if (flashTicks % 3 == 0 || flashTicks == 1) {
					float progress = (float) flashTicks / (float) chargeTargetTicks;
					float volume = 6.0F + progress * 14.0F; // Escalates dramatically up to 20.0!
					float pitch = 0.50F + progress * 1.10F;  // Ramping pitch from 0.50 to 1.60!
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, volume, pitch);
					if (flashTicks % 6 == 0) {
						mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, volume * 0.9F, pitch * 0.85F);
					}
				}

				if (flashTicks >= chargeTargetTicks) {
					mData.putInt("weapon_flash_phase", 2);
					mData.putInt("weapon_flash_ticks", 0);
				}
				syncWeaverEntityData(mob, false, mData);
				return;

			} else if (flashPhase == 2) { // Sudden Yellow Beam Flash & Cube Cone Block Vaporization Phase
				updateStateAndLog(mob, mData, "FLASH_VAPORIZE_BEAM");

				if (flashTicks == 1) {
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 10.0F, 1.20F);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 8.0F, 1.20F);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.HOSTILE, 8.0F, 0.80F);
				}

				Vec3 look = mob.getLookAngle();
				Vec3 orbPos = new Vec3(
					mob.getX() + look.x * 1.9,
					mob.getY() + 4.2 + look.y * 1.9,
					mob.getZ() + look.z * 1.9
				);

				Vec3 beamDir = look;
				double maxBeamDist = 64.0;

				if (flashTarget != null) {
					Vec3 flashTargetEye = flashTarget.getEyePosition(1.0F);
					mData.putDouble("flash_last_target_x", flashTargetEye.x);
					mData.putDouble("flash_last_target_y", flashTargetEye.y);
					mData.putDouble("flash_last_target_z", flashTargetEye.z);

					Vec3 rawBeamDir = flashTargetEye.subtract(orbPos);
					beamDir = (rawBeamDir.lengthSqr() > 1.0E-4) ? rawBeamDir.normalize() : look;
					maxBeamDist = Math.min(64.0, mob.distanceTo(flashTarget) + 8.0);

					double dx = flashTargetEye.x - orbPos.x;
					double dy = flashTargetEye.y - orbPos.y;
					double dz = flashTargetEye.z - orbPos.z;
					double horizDist = Math.sqrt(dx * dx + dz * dz);

					float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
					float targetPitch = (float) (-(Math.atan2(dy, Math.max(0.1, horizDist)) * (180.0 / Math.PI)));

					mData.putFloat("weaver_hypnosis_yaw", targetYaw);
					mData.putFloat("flash_saved_yaw", targetYaw);
					mData.putFloat("flash_saved_pitch", targetPitch);

					mob.setYRot(targetYaw);
					mob.yRotO = targetYaw;
					mob.setYBodyRot(targetYaw);
					mob.setYHeadRot(targetYaw);
					mob.yHeadRotO = targetYaw;
					mob.setXRot(targetPitch);
					mob.xRotO = targetPitch;
					mob.getLookControl().setLookAt(flashTargetEye.x, flashTargetEye.y, flashTargetEye.z, 30.0F, 30.0F);
				} else if (mData.contains("flash_last_target_x")) {
					double lastX = mData.getDouble("flash_last_target_x");
					double lastY = mData.getDouble("flash_last_target_y");
					double lastZ = mData.getDouble("flash_last_target_z");
					Vec3 lastTargetPos = new Vec3(lastX, lastY, lastZ);

					Vec3 rawBeamDir = lastTargetPos.subtract(orbPos);
					beamDir = (rawBeamDir.lengthSqr() > 1.0E-4) ? rawBeamDir.normalize() : look;
					maxBeamDist = Math.min(64.0, orbPos.distanceTo(lastTargetPos) + 8.0);

					float savedYaw = persistentFloat(mData, "flash_saved_yaw", persistentFloat(mData, "weaver_hypnosis_yaw", mob.getYRot()));
					float savedPitch = persistentFloat(mData, "flash_saved_pitch", mob.getXRot());

					mob.setYRot(savedYaw);
					mob.yRotO = savedYaw;
					mob.setYBodyRot(savedYaw);
					mob.setYHeadRot(savedYaw);
					mob.yHeadRotO = savedYaw;
					mob.setXRot(savedPitch);
					mob.xRotO = savedPitch;
					mob.getLookControl().setLookAt(lastX, lastY, lastZ, 30.0F, 30.0F);
				} else {
					float fallbackYaw = persistentFloat(mData, "weaver_hypnosis_yaw", mob.getYRot());
					double fallbackRad = Math.toRadians(fallbackYaw);
					double fX = -Math.sin(fallbackRad);
					double fZ = Math.cos(fallbackRad);
					beamDir = new Vec3(fX, look.y, fZ);
					if (beamDir.lengthSqr() > 1.0E-4) {
						beamDir = beamDir.normalize();
					} else {
						beamDir = look;
					}
					maxBeamDist = 64.0;

					mob.setYRot(fallbackYaw);
					mob.yRotO = fallbackYaw;
					mob.setYBodyRot(fallbackYaw);
					mob.setYHeadRot(fallbackYaw);
					mob.yHeadRotO = fallbackYaw;
					mob.yBodyRotO = fallbackYaw;
				}

				// Build orthonormal basis for exact 4-sided square frustum pyramid beam matching BeamRendererWeaver tractor beam layer
				Vec3 upRef = new Vec3(0, 1, 0);
				if (Math.abs(beamDir.y) > 0.95) {
					upRef = new Vec3(1, 0, 0);
				}
				Vec3 beamRight = beamDir.cross(upRef).normalize();
				Vec3 beamUp = beamRight.cross(beamDir).normalize();

				// Effectively final copies for lambda closure
				final Vec3 finalOrbPos = orbPos;
				final Vec3 finalBeamDir = beamDir;
				final Vec3 finalBeamRight = beamRight;
				final Vec3 finalBeamUp = beamUp;
				final double finalMaxBeamDist = maxBeamDist;
				final LivingEntity finalFlashTarget = flashTarget;

				// Precise 3D Square Pyramid Frustum Block Vaporization
				if (world instanceof ServerLevel serverLevel) {
					// Bounding box bounds around the beam pyramid matching tractor beam layer size
					double maxRadiusAtEnd = 0.28 + maxBeamDist * 0.070 + 1.2;
					int minX = (int) Math.floor(Math.min(orbPos.x, orbPos.x + beamDir.x * maxBeamDist) - maxRadiusAtEnd);
					int maxX = (int) Math.ceil(Math.max(orbPos.x, orbPos.x + beamDir.x * maxBeamDist) + maxRadiusAtEnd);
					int minY = (int) Math.floor(Math.min(orbPos.y, orbPos.y + beamDir.y * maxBeamDist) - maxRadiusAtEnd);
					int maxY = (int) Math.ceil(Math.max(orbPos.y, orbPos.y + beamDir.y * maxBeamDist) + maxRadiusAtEnd);
					int minZ = (int) Math.floor(Math.min(orbPos.z, orbPos.z + beamDir.z * maxBeamDist) - maxRadiusAtEnd);
					int maxZ = (int) Math.ceil(Math.max(orbPos.z, orbPos.z + beamDir.z * maxBeamDist) + maxRadiusAtEnd);

					BlockPos.betweenClosedStream(minX, minY, minZ, maxX, maxY, maxZ).forEach(bp -> {
						Vec3 bpCenter = new Vec3(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5);
						Vec3 relVec = bpCenter.subtract(finalOrbPos);

						double distAlongBeam = relVec.dot(finalBeamDir);
						if (distAlongBeam >= 0.0 && distAlongBeam <= finalMaxBeamDist) {
							double rightOffset = Math.abs(relVec.dot(finalBeamRight));
							double upOffset = Math.abs(relVec.dot(finalBeamUp));

							// Half-width of 4-sided square beam pyramid at distance d (start 0.28, spread rate 0.070) plus body passage clearance
							double halfWidth = 0.45 + distAlongBeam * 0.080 + 1.25;

							if (rightOffset <= halfWidth && upOffset <= halfWidth) {
								BlockState state = world.getBlockState(bp);
								float hardness = state.getDestroySpeed(world, bp);
								LivingEntity beamTarget = finalFlashTarget;
								int mobFootY = mob.blockPosition().getY();
								int posDistSq = (int) Math.round(mob.position().distanceToSqr(bp.getX() + 0.5, mob.getY(), bp.getZ() + 0.5));
								
								boolean allowBreak = true;
								// Never vaporize floor blocks directly under or within 1.5 blocks of the mob's feet
								if (bp.getY() < mobFootY && posDistSq <= 4) {
									allowBreak = false;
								}
								int effectiveMinY = (beamTarget != null) ? Math.min(mobFootY, beamTarget.blockPosition().getY()) : mobFootY;
								if (bp.getY() < effectiveMinY) {
									allowBreak = false;
								}

								// Instantly vaporize ALL blocks inside the cube cone shape (hardness <= 50.0, down to effective target Y level)
								if (allowBreak && !state.isAir() && hardness >= 0.0F && hardness <= 50.0F) {
									world.destroyBlock(bp, false);
									serverLevel.sendParticles(ParticleTypes.EXPLOSION, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
									serverLevel.sendParticles(ParticleTypes.FLAME, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.05);
									serverLevel.sendParticles(ParticleTypes.SMOKE, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.05);
								}
							}
						}
					});

					// Damage entities caught inside the flash cone
					AABB beamBounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
					for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, beamBounds, ent -> ent != mob && ent.isAlive() && !isWoodboundOrWeaver(ent))) {
						Vec3 eCenter = e.position().add(0, e.getBbHeight() * 0.5, 0);
						Vec3 relVec = eCenter.subtract(orbPos);
						double distAlongBeam = relVec.dot(beamDir);
						if (distAlongBeam >= 0.0 && distAlongBeam <= maxBeamDist) {
							double rightOffset = Math.abs(relVec.dot(beamRight));
							double upOffset = Math.abs(relVec.dot(beamUp));
							double halfWidth = 0.28 + distAlongBeam * 0.070 + 0.75;
							if (rightOffset <= halfWidth && upOffset <= halfWidth) {
								e.hurt(mob.damageSources().mobAttack(mob), 50.0F);
								e.setRemainingFireTicks(100);
								e.setDeltaMovement(beamDir.x * 0.6, 0.25, beamDir.z * 0.6);
								e.hasImpulse = true;
								if (e instanceof ServerPlayer sp) {
									sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
								}
							}
						}
					}
				}

				if (flashTicks >= 10) { // 0.5s brief flash
					mData.putBoolean("isHypnotizing2", false);
					mData.putBoolean("isHypnotizing3", false);
					mData.putBoolean("isWeaponClose", true);
					mData.putInt("weapon_flash_phase", 3);
					mData.putInt("weapon_flash_ticks", 0);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 8.0F, 0.60F);
				}
				syncWeaverEntityData(mob, false, mData);
				return;

			} else if (flashPhase == 3) { // Weaponized Closing Animation Phase (isWeaponClose)
				updateStateAndLog(mob, mData, "FLASH_VAPORIZE_CLOSING_REVERSE");
				mData.putBoolean("isWeaponClose", true);

				if (flashTicks >= 25) {
					mData.putBoolean("isHypnotizing2", false);
					mData.putBoolean("isHypnotizing3", false);
					mData.putBoolean("isWeaponClose", false);
					mData.putBoolean("was_fast_flash", false);
					mData.putInt("weapon_flash_phase", 0);
					mob.removeEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING);
					mData.putInt("weapon_flash_ticks", 0);

					// Enter POST-FATIGUE STATE: Freeze movement, locked rotation, no follow-up abilities for 45 ticks (~2.25s)
					mData.putInt("weapon_post_fatigue_ticks", 45);
					float finalHypnoYaw = persistentFloat(mData, "weaver_hypnosis_yaw", mob.getYRot());
					mData.putFloat("post_fatigue_yaw", finalHypnoYaw);
					stopMobMotion(mob);
					mob.getNavigation().stop();
				}
				syncWeaverEntityData(mob, false, mData);
				return;
			}
		}

		// EXCLUSIVE STARE CHARGING / GAZE PRE-CHARGING STATE
		int stareTicks = persistentInt(mData, "stare_ticks", 0);
		if (hypnoPhase == 0 && stareTicks > 0) {
			clearOtherStates(mData, "STARE_CHARGING");
			updateStateAndLog(mob, mData, "STARE_CHARGING");

			stopMobMotion(mob);
			mob.getNavigation().stop();

			List<LivingEntity> hypnoTargets = getHypnoTargets(mob, mData);
			LivingEntity starePrimary = hypnoTargets.isEmpty() ? null : hypnoTargets.get(0);
			if (starePrimary == null || !starePrimary.isAlive()) {
				int stareTargetId = persistentInt(mData, "hypno_target_id", 0);
				Entity rawStare = (stareTargetId != 0) ? mob.level().getEntity(stareTargetId) : null;
				if (rawStare instanceof LivingEntity le && le.isAlive()) {
					starePrimary = le;
				}
			}

			if (starePrimary instanceof Player p && p.isAlive()) {
				mData.putInt("hypno_target_id", p.getId());
				mData.putString("hypno_target_ids", "");
				String currentMode = persistentString(mData, "hypno_mode", "");
				if (currentMode.isEmpty() || ("infect".equals(currentMode) && targetHasCellularCollapse(p))) {
					mData.putString("hypno_mode", chooseHypnoMode(mob, mData, p));
				}
			} else {
				Player p2 = (mob.getTarget() instanceof Player pT) ? pT : getNearestSurvivalPlayer(world, mob);
				if (p2 != null) {
					starePrimary = p2;
					mData.putInt("hypno_target_id", p2.getId());
					mData.putString("hypno_target_ids", "");
					mData.putString("hypno_mode", chooseHypnoMode(mob, mData, p2));
				}
			}

			// Keep target nulled on mob to completely suppress vanilla BodyRotationControl & MeleeAttackGoal fighting locked yaw
			mob.setTarget(null);

			hypnoCooldown = persistentInt(mData, "hypno_cooldown", 0);
			boolean isMiningOrAttackActive = mData.getBoolean("is_block_attack") || mData.getBoolean("is_attacking");
			boolean isGrounded = mob.onGround();
			double minGazeDist = 4.0;

			boolean gazeMaintained = false;
			if (hypnoCooldown <= 0 && isGrounded && starePrimary != null && starePrimary.isAlive() && !isMiningOrAttackActive && mob.distanceTo(starePrimary) >= minGazeDist && mob.distanceTo(starePrimary) <= HYPNO_MAX_RANGE) {
				boolean physBlocked = (findPhysicalObstruction(world, mob, starePrimary, mob.distanceTo(starePrimary)) != null) || (findFrontalObstruction(world, mob, starePrimary) != null);
				if (!physBlocked && mob.hasLineOfSight(starePrimary)) {
					gazeMaintained = true;
				}
			}

			// Prevent endless hypno retry loop when hurt during gaze pre-charging
			boolean wasHurtDuringStare = (mob.getLastHurtByMob() != null && mob.tickCount - mob.getLastHurtByMobTimestamp() <= 10);
			if (wasHurtDuringStare) {
				applyHypnoCooldown(mData);
				mData.putInt("hypno_cooldown", 140); // 7s cooldown when hurt during hypno startup
				gazeMaintained = false;
			}

			if (gazeMaintained) {
				stareTicks++;
				mData.putInt("stare_ticks", stareTicks);

				// Seed hypnosis yaw if not set, then always run smooth unified handler to avoid 1-tick transition snaps
				if (!mData.contains("weaver_hypnosis_yaw")) {
					double dx = starePrimary.getX() - mob.getX();
					double dz = starePrimary.getZ() - mob.getZ();
					float directYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
					mData.putFloat("weaver_hypnosis_yaw", directYaw);
				}
				updateAndApplyHypnosisYaw(mob, mData, starePrimary, true);

				if (stareTicks >= 40) {
					clearOtherStates(mData, "HYPNO");
					setHypnoPhase(mData, 1);
					updateStateAndLog(mob, mData, persistentString(mData, "hypno_mode", "infect").toUpperCase() + "_OPENING");

					mData.putInt("hypno_target_id", starePrimary.getId());
					mData.putString("hypno_target_ids", "");

					mData.putInt("hypno_ticks", 0);
					mData.putInt("stare_ticks", 0);

					mData.putDouble("hypno_start_x", mob.getX());
					mData.putDouble("hypno_start_y", mob.getY());
					mData.putDouble("hypno_start_z", mob.getZ());

					double dx = starePrimary.getX() - mob.getX();
					double dz = starePrimary.getZ() - mob.getZ();
					float initYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
					mData.putFloat("weaver_hypnosis_yaw", initYaw);
					applyRotationToAll(mob, initYaw);

					stopMobMotion(mob);
					mob.setTarget(null);

					syncWeaverEntityData(mob, false, mData);
					return;
				}

				syncWeaverEntityData(mob, false, mData);
				return;
			} else {
				// Gaze/LOS lost or hurt during pre-charge stare: cancel stare cleanly without triggering false closing phase animation
				mData.putInt("stare_ticks", 0);
				applyHypnoCooldown(mData);
				if (wasHurtDuringStare) {
					mData.putInt("hypno_cooldown", 140); // 7s cooldown when hurt during hypno startup
				}
				clearOtherStates(mData, "NORMAL");
				updateStateAndLog(mob, mData, "NORMAL");
				syncWeaverEntityData(mob, false, mData);
				return;
			}
		}

		boolean isStaring = persistentInt(mData, "stare_ticks", 0) > 0;
		boolean isAnyHypnoActive = (hypnoPhase != 0) || isStaring;

		// STATE -1: ANIMATION PREVIEW OVERRIDE STATE ("weaver anim ...")
		boolean isPlayingAnim = persistentBoolean(mData, "is_playing_anim", false);
		if (isPlayingAnim) {
			clearOtherStates(mData, "ANIM_PREVIEW");
			updateStateAndLog(mob, mData, "ANIM_PREVIEW");

			int animTimer = persistentInt(mData, "anim_timer", 0);
			if (animTimer > 0) {
				stopMobMotion(mob);
				mData.putInt("anim_timer", animTimer - 1);
				syncWeaverEntityData(mob, persistentBoolean(mData, "anim_jump", false), mData);
				return;
			} else {
				clearOtherStates(mData, "NORMAL");
				updateStateAndLog(mob, mData, "NORMAL");
				syncWeaverEntityData(mob, false, mData);
				return;
			}
		}

		// HYPNO PRIORITY: Any active hypnosis state hard-locks Woodweaver motion and cancels leap/attack states
		if (isAnyHypnoActive) {
			clearOtherStates(mData, "HYPNO");
		}

		// Force Leap request (debug / procedure)
		boolean forceLeap = persistentBoolean(mData, "leap_requested", false);
		if (forceLeap) {
			if (isAnyHypnoActive || persistentBoolean(mData, "is_leaping", false) || persistentBoolean(mData, "is_preparing_leap", false) || persistentBoolean(mData, "is_attacking", false)) {
				mData.putBoolean("leap_requested", false);
			} else {
				stopMobMotion(mob);
				double landX = persistentDouble(mData, "leap_requested_x", 0.0);
				double landY = persistentDouble(mData, "leap_requested_y", 0.0);
				double landZ = persistentDouble(mData, "leap_requested_z", 0.0);
				mData.putBoolean("leap_requested", false);

				if (LEAP_PREPARATION_TICKS > 0) {
					clearOtherStates(mData, "LEAP_PREP");
					updateStateAndLog(mob, mData, "LEAP_PREPARING");
					mData.putBoolean("is_preparing_leap", true);
					mData.putInt("leap_prep_ticks", 0);
					mData.putDouble("leap_land_x", landX);
					mData.putDouble("leap_land_y", landY);
					mData.putDouble("leap_land_z", landZ);
					mData.putInt("leap_cooldown", LEAP_COOLDOWN_DEFAULT);
					syncWeaverEntityData(mob, true, mData);
					return;
				} else {
					clearOtherStates(mData, "LEAPING");
					updateStateAndLog(mob, mData, "LEAP_LAUNCH");
					mData.putBoolean("is_leaping", true);
					mData.putDouble("leap_land_x", landX);
					mData.putDouble("leap_land_y", landY);
					mData.putDouble("leap_land_z", landZ);
					mData.putInt("leap_ticks", 0);
					mData.putInt("leap_cooldown", LEAP_COOLDOWN_DEFAULT);

					mData.putDouble("leap_start_x", mob.getX());
					mData.putDouble("leap_start_y", mob.getY());
					mData.putDouble("leap_start_z", mob.getZ());
					mData.putBoolean("was_airborne", false);

					double jumpDX = landX - mob.getX();
					double jumpDY = landY - mob.getY();
					double jumpDZ = landZ - mob.getZ();
					double jumpHorizontalDist = Math.sqrt(jumpDX * jumpDX + jumpDZ * jumpDZ);
					if (jumpHorizontalDist < 0.1) jumpHorizontalDist = 0.1;

					double estAirTicks = Math.max(16.0, Math.min(26.0, 12.0 + jumpHorizontalDist * 0.7));
					double horizDragFactor = (1.0 - Math.pow(0.91, estAirTicks)) / 0.09;
					double horizSpeed = (jumpHorizontalDist / horizDragFactor) * LEAP_SPEED_MULTIPLIER;

					double velX = (jumpDX / jumpHorizontalDist) * horizSpeed;
					double velZ = (jumpDZ / jumpHorizontalDist) * horizSpeed;
					double velY = calculateLeapVelY(estAirTicks, jumpDY);

					stopMobMotion(mob);
					mob.setDeltaMovement(velX, velY, velZ);
					mob.hasImpulse = true;
					mob.setOnGround(false);

					float launchYaw = (float) (Math.atan2(jumpDZ, jumpDX) * 180.0 / Math.PI) - 90.0F;
					mob.setYRot(launchYaw);
					mob.setYBodyRot(launchYaw);
					mob.setYHeadRot(launchYaw);
					mData.putFloat("leap_lock_yaw", launchYaw);

					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 1.5F, 0.40F);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.2F, 0.60F);
					syncWeaverEntityData(mob, true, mData);
					return;
				}
			}
		}

		// STATE 2: LEAP PREPARATION STATE
		boolean isPreparingLeap = persistentBoolean(mData, "is_preparing_leap", false);
		if (isPreparingLeap) {
			clearOtherStates(mData, "LEAP_PREP");
			updateStateAndLog(mob, mData, "LEAP_PREPARING");
			stopMobMotion(mob);

			int prepTicks = persistentInt(mData, "leap_prep_ticks", 0) + 1;
			mData.putInt("leap_prep_ticks", prepTicks);

			double targetLandX = persistentDouble(mData, "leap_land_x", mob.getX());
			double targetLandY = persistentDouble(mData, "leap_land_y", mob.getY());
			double targetLandZ = persistentDouble(mData, "leap_land_z", mob.getZ());
			double ldx = targetLandX - mob.getX();
			double ldz = targetLandZ - mob.getZ();
			float targetYaw = (float) (Math.atan2(ldz, ldx) * 180.0 / Math.PI) - 90.0F;
			applyRotationToAll(mob, targetYaw);
			mData.putFloat("leap_lock_yaw", targetYaw);

			if (prepTicks >= LEAP_PREPARATION_TICKS) {
				clearOtherStates(mData, "LEAPING");
				updateStateAndLog(mob, mData, "LEAP_LAUNCH");
				mData.putBoolean("is_preparing_leap", false);
				mData.putInt("leap_prep_ticks", 0);
				mData.putBoolean("is_leaping", true);
				mData.putInt("leap_ticks", 0);
				mData.putBoolean("was_airborne", false);

				mData.putDouble("leap_start_x", mob.getX());
				mData.putDouble("leap_start_y", mob.getY());
				mData.putDouble("leap_start_z", mob.getZ());

				double jumpDX = targetLandX - mob.getX();
				double jumpDY = targetLandY - mob.getY();
				double jumpDZ = targetLandZ - mob.getZ();
				double jumpHorizontalDist = Math.sqrt(jumpDX * jumpDX + jumpDZ * jumpDZ);
				if (jumpHorizontalDist < 0.1) jumpHorizontalDist = 0.1;

				double estAirTicks = Math.max(16.0, Math.min(26.0, 12.0 + jumpHorizontalDist * 0.7));
				double horizDragFactor = (1.0 - Math.pow(0.91, estAirTicks)) / 0.09;
				double horizSpeed = (jumpHorizontalDist / horizDragFactor) * LEAP_SPEED_MULTIPLIER;

				double velX = (jumpDX / jumpHorizontalDist) * horizSpeed;
				double velZ = (jumpDZ / jumpHorizontalDist) * horizSpeed;
				double velY = calculateLeapVelY(estAirTicks, jumpDY);

				stopMobMotion(mob);
				mob.setDeltaMovement(velX, velY, velZ);
				mob.hasImpulse = true;
				mob.setOnGround(false);

				float launchYaw = (float) (Math.atan2(jumpDZ, jumpDX) * 180.0 / Math.PI) - 90.0F;
				applyRotationToAll(mob, launchYaw);
				mData.putFloat("leap_lock_yaw", launchYaw);

				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 1.5F, 0.40F);
				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.2F, 0.60F);

				syncWeaverEntityData(mob, true, mData);
				return;
			}

			syncWeaverEntityData(mob, true, mData);
			return;
		}

		// STATE 3: ACTIVE LEAP STATE (PHYSICAL_LAUNCH -> AIRBORNE -> LANDING)
		boolean isLeaping = persistentBoolean(mData, "is_leaping", false);
		if (isLeaping) {
			clearOtherStates(mData, "LEAPING");

			int leapTicks = persistentInt(mData, "leap_ticks", 0) + 1;
			mData.putInt("leap_ticks", leapTicks);

			double startY = persistentDouble(mData, "leap_start_y", mob.getY());
			boolean wasAirborne = persistentBoolean(mData, "was_airborne", false);

			if (!mob.onGround() && (mob.getY() >= startY + 0.25 || mob.getDeltaMovement().y > 0.05)) {
				if (!wasAirborne) {
					wasAirborne = true;
					mData.putBoolean("was_airborne", true);
					updateStateAndLog(mob, mData, "LEAP_AIRBORNE");
				}
			}

			double targetLandX = persistentDouble(mData, "leap_land_x", mob.getX());
			double targetLandY = persistentDouble(mData, "leap_land_y", mob.getY());
			double targetLandZ = persistentDouble(mData, "leap_land_z", mob.getZ());

			if (leapTicks <= 2 && !wasAirborne) {
				double jumpDX = targetLandX - mob.getX();
				double jumpDY = targetLandY - mob.getY();
				double jumpDZ = targetLandZ - mob.getZ();
				double jumpHorizontalDist = Math.sqrt(jumpDX * jumpDX + jumpDZ * jumpDZ);
				if (jumpHorizontalDist < 0.1) jumpHorizontalDist = 0.1;

				double estAirTicks = Math.max(16.0, Math.min(26.0, 12.0 + jumpHorizontalDist * 0.7));
				double horizDragFactor = (1.0 - Math.pow(0.91, estAirTicks)) / 0.09;
				double horizSpeed = (jumpHorizontalDist / horizDragFactor) * LEAP_SPEED_MULTIPLIER;

				double velX = (jumpDX / jumpHorizontalDist) * horizSpeed;
				double velZ = (jumpDZ / jumpHorizontalDist) * horizSpeed;
				double velY = calculateLeapVelY(estAirTicks, jumpDY);

				mob.setDeltaMovement(velX, velY, velZ);
				mob.hasImpulse = true;
				mob.setOnGround(false);
			}

			if (wasAirborne || leapTicks > 1) {
				destroyWallsInLeapPath(world, mob);
			}

			boolean isLanding = wasAirborne && (mob.onGround() || leapTicks > 60);

			if (isLanding) {
				clearOtherStates(mData, "LANDING");
				updateStateAndLog(mob, mData, "LANDING");
				mData.putBoolean("is_landing_anim", true);
				mData.putInt("landing_anim_timer", LANDING_ANIM_DURATION);

				float landingYaw = persistentFloat(mData, "leap_lock_yaw", mob.getYRot());
				double jumpDX = targetLandX - mob.getX();
				double jumpDZ = targetLandZ - mob.getZ();
				if (jumpDX * jumpDX + jumpDZ * jumpDZ > 0.001) {
					landingYaw = (float) (Math.atan2(jumpDZ, jumpDX) * 180.0 / Math.PI) - 90.0F;
				}

				LivingEntity landTarget = mob.getTarget();
				if (landTarget == null || !landTarget.isAlive() || isWoodboundOrWeaver(landTarget) || (landTarget instanceof Player p && (p.isCreative() || p.isSpectator()))) {
					landTarget = getNearestSurvivalPlayer(world, mob);
				}
				if (landTarget != null && landTarget.isAlive()) {
					double dx = landTarget.getX() - mob.getX();
					double dz = landTarget.getZ() - mob.getZ();
					if (dx * dx + dz * dz > 0.001) {
						landingYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
					}
				}
				applyRotationToAll(mob, landingYaw);
				mData.putFloat("leap_lock_yaw", landingYaw);
				mData.putFloat("weaver_hypnosis_yaw", landingYaw);
				stopMobMotion(mob);
				mob.getNavigation().stop();

				if (IS_SHOCKWAVE_DESTRUCTION_ENABLED) {
					BlockPos landCenter = BlockPos.containing(mob.getX(), mob.getY() - 0.5, mob.getZ());
					int minX = landCenter.getX() - (int) LANDING_RADIUS;
					int maxX = landCenter.getX() + (int) LANDING_RADIUS;
					int minY = landCenter.getY() - 1;
					int maxY = landCenter.getY() + 1;
					int minZ = landCenter.getZ() - (int) LANDING_RADIUS;
					int maxZ = landCenter.getZ() + (int) LANDING_RADIUS;
					double radiusSq = LANDING_RADIUS * LANDING_RADIUS;

					BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
					for (int bx = minX; bx <= maxX; bx++) {
						for (int by = minY; by <= maxY; by++) {
							for (int bz = minZ; bz <= maxZ; bz++) {
								mpos.set(bx, by, bz);
								if (mpos.distSqr(landCenter) <= radiusSq) {
									BlockState state = world.getBlockState(mpos);
									float hardness = state.getDestroySpeed(world, mpos);
									if (!state.isAir() && hardness >= 0 && hardness < 40f) {
										world.setBlock(mpos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
									}
								}
							}
						}
					}
				}

				if (world instanceof ServerLevel serverLevel) {
					spawnHeavyLandingShockwave(serverLevel, mob);
				}

				List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(LANDING_RADIUS + 1.5), e -> e != mob && !isWoodboundOrWeaver(e));
				for (LivingEntity victim : nearby) {
					victim.hurt(getDamageSource(world, mob), (float) LANDING_DAMAGE);
					Vec3 pushVec = victim.position().subtract(mob.position());
					double hDist = Math.sqrt(pushVec.x * pushVec.x + pushVec.z * pushVec.z);
					if (hDist > 0.01) {
						pushVec = new Vec3(pushVec.x / hDist, 0, pushVec.z / hDist);
					} else {
						pushVec = new Vec3(1, 0, 0);
					}
					victim.setDeltaMovement(pushVec.x * 0.9, 0.3, pushVec.z * 0.9);
					victim.hasImpulse = true;
				}

				mob.setDeltaMovement(0, -0.1, 0);
				syncWeaverEntityData(mob, false, mData);
				return;
			} else {
				if (wasAirborne) {
					double jumpDX = targetLandX - mob.getX();
					double jumpDZ = targetLandZ - mob.getZ();
					if (jumpDX * jumpDX + jumpDZ * jumpDZ > 0.001) {
						float leapYaw = (float) (Math.atan2(jumpDZ, jumpDX) * 180.0 / Math.PI) - 90.0F;
						applyRotationToAll(mob, leapYaw);
					}
				} else {
					if (leapTicks > 8) {
						clearOtherStates(mData, "NORMAL");
						updateStateAndLog(mob, mData, "NORMAL");
						int currentCd = persistentInt(mData, "leap_cooldown", 0);
						if (currentCd < 80) {
							mData.putInt("leap_cooldown", 80); // Longer failed leap penalty cooldown, prevents spamming failed hops
						}
						syncWeaverEntityData(mob, false, mData);
						return;
					}
				}
			}

			syncWeaverEntityData(mob, isLeaping, mData);
			return;
		}

		// STATE 4: LANDING ANIMATION RECOVERY STATE
		boolean isLandingAnimState = persistentBoolean(mData, "is_landing_anim", false) || persistentInt(mData, "landing_anim_timer", 0) > 0;
		if (isLandingAnimState) {
			clearOtherStates(mData, "LANDING");
			updateStateAndLog(mob, mData, "LANDING");
			stopMobMotion(mob);
			mob.getNavigation().stop();

			int landingAnimTimer = persistentInt(mData, "landing_anim_timer", 0);
			if (landingAnimTimer > 0) {
				float targetYaw = persistentFloat(mData, "leap_lock_yaw", mob.getYRot());
				applyRotationToAll(mob, targetYaw);
				mData.putFloat("weaver_hypnosis_yaw", targetYaw);

				mData.putInt("landing_anim_timer", landingAnimTimer - 1);
			}

			if (landingAnimTimer <= 1) {
				float finalLandingYaw = persistentFloat(mData, "leap_lock_yaw", mob.getYRot());
				applyRotationToAll(mob, finalLandingYaw);
				mData.putFloat("weaver_hypnosis_yaw", finalLandingYaw);

				stopMobMotion(mob);
				mob.getNavigation().stop();

				LivingEntity landTarget = mob.getTarget();
				if (landTarget == null || !landTarget.isAlive() || isWoodboundOrWeaver(landTarget) || (landTarget instanceof Player p && (p.isCreative() || p.isSpectator()))) {
					landTarget = getNearestSurvivalPlayer(world, mob);
					if (landTarget != null) {
						mob.setTarget(landTarget);
					}
				}

				hypnoCooldown = persistentInt(mData, "hypno_cooldown", 0);
				boolean isMiningOrAttackActive = mData.getBoolean("is_block_attack") || mData.getBoolean("is_attacking");

				if (hypnoCooldown <= 0 && landTarget instanceof Player playerTarget && !isMiningOrAttackActive) {
					double distToTarget = mob.distanceTo(playerTarget);
					if (distToTarget >= 5.0 && distToTarget <= 20.0) {
						Vec3 startEye = mob.getEyePosition(1.0F);
						Vec3 targetEye = playerTarget.getEyePosition(1.0F);
						BlockHitResult hit = world.clip(new net.minecraft.world.level.ClipContext(
							startEye, targetEye,
							net.minecraft.world.level.ClipContext.Block.COLLIDER,
							net.minecraft.world.level.ClipContext.Fluid.NONE,
							mob
						));
						boolean isLineOfSightBlocked = hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;

						if (!isLineOfSightBlocked) {
							Vec3 lookVec = playerTarget.getLookAngle();
							Vec3 toMobVec = new Vec3(mob.getX() - playerTarget.getX(), (mob.getY() + 1.8) - playerTarget.getEyeY(), mob.getZ() - playerTarget.getZ()).normalize();
							double dot = lookVec.dot(toMobVec);

							if (dot > 0.82) {
								stareTicks = persistentInt(mData, "stare_ticks", 0) + 1;
								mData.putInt("stare_ticks", stareTicks);
								mData.putBoolean("is_landing_anim", false);
								mData.putInt("landing_anim_timer", 0);
								clearOtherStates(mData, "STARE_CHARGING");
								updateStateAndLog(mob, mData, "STARE_CHARGING");

								stopMobMotion(mob);
								mob.getNavigation().stop();
								updateAndApplyHypnosisYaw(mob, mData, playerTarget, true);

								syncWeaverEntityData(mob, false, mData);
								return;
							}
						}
					}
				}

				mData.putBoolean("is_landing_anim", false);
				mData.putInt("landing_anim_timer", 0);
				clearOtherStates(mData, "NORMAL");
				updateStateAndLog(mob, mData, "NORMAL");
				syncWeaverEntityData(mob, false, mData);
				return;
			} else {
				mData.putBoolean("is_landing_anim", true);
			}

			syncWeaverEntityData(mob, false, mData);
			return;
		}

		// STATE: AMBUSH WINDUP
		boolean isAmbushWindup = persistentBoolean(mData, "is_ambush_windup", false);
		if (isAmbushWindup) {
			clearOtherStates(mData, "AMBUSH_WINDUP");
			int windupTicks = persistentInt(mData, "ambush_windup_ticks", 0) + 1;
			mData.putInt("ambush_windup_ticks", windupTicks);
			
			// Lock rotation towards target
			float ambushYaw = persistentFloat(mData, "ambush_target_yaw", mob.getYRot());
			mob.setYRot(ambushYaw);
			mob.yRotO = ambushYaw;
			mob.setYBodyRot(ambushYaw);
			mob.yBodyRotO = ambushYaw;
			mob.setYHeadRot(ambushYaw);
			mob.yHeadRotO = ambushYaw;
			mob.setXRot(0.0F);
			mob.xRotO = 0.0F;

			if (windupTicks == 1) {
				if (world instanceof ServerLevel sl) {
					sl.playSound(null, mob.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0F, 0.5F);
				}
			}

			if (windupTicks % 4 == 0 && world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mob.getX(), mob.getY() + 1.0, mob.getZ(), 20, 1.0, 1.0, 1.0, 0.0);
				sl.playSound(null, mob.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.5F, 0.5F);
			}

			if (windupTicks >= 15) { // 0.75s windup
				mData.putBoolean("is_ambush_windup", false);
				// TRIGGER EXPLOSIVE DASH
				double dashTargetX = persistentDouble(mData, "dash_target_x", mob.getX());
				double dashTargetY = persistentDouble(mData, "dash_target_y", mob.getY());
				double dashTargetZ = persistentDouble(mData, "dash_target_z", mob.getZ());
				
				clearOtherStates(mData, "DASHING");
				updateStateAndLog(mob, mData, "DASHING_AMBUSH_WALL_OVERRIDE");
				mData.putBoolean("is_dashing", true);
				mData.putInt("dash_ticks", 0);
				mData.putFloat("dash_lock_yaw", ambushYaw);
				mData.putDouble("dash_start_x", mob.getX());
				mData.putDouble("dash_start_y", mob.getY());
				mData.putDouble("dash_start_z", mob.getZ());
				mData.putDouble("dash_total_dist", 50.0); // Dash far to ensure breakthrough
				mData.putBoolean("dash_is_override", false);

				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.5F, 0.60F);
				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 0.65F);
				if (world instanceof ServerLevel sl) {
					sl.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 1.0, mob.getZ(), 10, 1.0, 1.0, 1.0, 0.0);
				}
			}
			syncWeaverEntityData(mob, false, mData);
			return;
		}

		// STATE: KOOL-AID MAN / TRUCK RAMMING DASH STATE
		boolean isDashing = persistentBoolean(mData, "is_dashing", false);
		if (isDashing) {
			clearOtherStates(mData, "DASHING");
			updateStateAndLog(mob, mData, "DASHING");

			int dashTicks = persistentInt(mData, "dash_ticks", 0) + 1;
			mData.putInt("dash_ticks", dashTicks);

			float lockedYaw = persistentFloat(mData, "dash_lock_yaw", mob.getYRot());

			// Direct rock-solid rotation lock without wobbling or interpolation fighting (same as leap lock)
			mob.getNavigation().stop();
			mob.setYRot(lockedYaw);
			mob.yRotO = lockedYaw;
			mob.setYBodyRot(lockedYaw);
			mob.yBodyRotO = lockedYaw;
			mob.setYHeadRot(lockedYaw);
			mob.yHeadRotO = lockedYaw;
			mob.setXRot(0.0F);
			mob.xRotO = 0.0F;

			double startX = persistentDouble(mData, "dash_start_x", mob.getX());
			double startY = persistentDouble(mData, "dash_start_y", mob.getY());
			double startZ = persistentDouble(mData, "dash_start_z", mob.getZ());
			double targetX = persistentDouble(mData, "dash_target_x", mob.getX());
			double targetY = persistentDouble(mData, "dash_target_y", mob.getY());
			double targetZ = persistentDouble(mData, "dash_target_z", mob.getZ());
			double targetDist = persistentDouble(mData, "dash_total_dist", 24.0);

			// Dynamic dash speed based on reduced health (+0.05 blocks/sec per HP reduced, capped at 20 blocks/sec = 1.00 block/tick)
			double missingHp = Math.max(0.0, mob.getMaxHealth() - mob.getHealth());
			double baseDashSpeedBlocksPerSec = 11.0; // 11.0 blocks/sec base
			double targetDashSpeedBlocksPerSec = Math.min(20.0, baseDashSpeedBlocksPerSec + missingHp * 0.05);
			double maxDashSpeedBlocksPerTick = Math.min(1.00, targetDashSpeedBlocksPerSec / 20.0);

			// Rapid linear acceleration up to max speed
			double accelTicks = 12.0;
			double speedRatio = Math.min(1.0, (double) dashTicks / accelTicks);
			double startSpeed = Math.min(0.35, maxDashSpeedBlocksPerTick);
			double currentSpeed = startSpeed + (maxDashSpeedBlocksPerTick - startSpeed) * speedRatio;

			double rad = Math.toRadians(lockedYaw + 90.0F);
			double dirX = Math.cos(rad);
			double dirZ = Math.sin(rad);
			double perpX = -dirZ;
			double perpZ = dirX;

			// Look straight ahead in dash direction
			double lookX = mob.getX() + dirX * 10.0;
			double lookZ = mob.getZ() + dirZ * 10.0;
			mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 100.0F, 100.0F);

			// Slope detection and ground clamping (step up slopes without caving or falling)
			double probeX = mob.getX() + dirX * Math.max(0.8, currentSpeed * 1.5);
			double probeZ = mob.getZ() + dirZ * Math.max(0.8, currentSpeed * 1.5);

			// Detect sharp drop ahead (at least 2.5 blocks of air below the probe)
			boolean hasSharpDropAhead = true;
			BlockPos probeBlockPos = BlockPos.containing(probeX, mob.getY(), probeZ);
			for (int dy = 0; dy <= 2; dy++) {
				if (!world.getBlockState(probeBlockPos.below(dy)).isAir()) {
					hasSharpDropAhead = false;
					break;
				}
			}

			boolean wasFreeFalling = mData.contains("dash_free_fall") && mData.getBoolean("dash_free_fall");
			boolean isFreeFalling = wasFreeFalling;

			if (hasSharpDropAhead) {
				isFreeFalling = true;
			}
			if (mob.onGround()) {
				isFreeFalling = false;
			}
			mData.putBoolean("dash_free_fall", isFreeFalling);

			double yVel = mob.getDeltaMovement().y;
			if (!isFreeFalling) {
				BlockPos groundPos = findGroundPos(world, probeX, mob.getY() + 1.0, probeZ);
				if (groundPos != null) {
					double groundY = groundPos.getY();
					double heightDiff = groundY - mob.getY();
					if (heightDiff > 0.1 && heightDiff <= 1.5) {
						yVel = 0.25 + heightDiff * 0.15; // Smoothly scale step-up
						mob.setOnGround(true);
					} else if (heightDiff < -0.1) {
						yVel = Math.max(-0.6, yVel - 0.08); // Stick to downward slope
					}
				}
			} else {
				// Free falling: let gravity pull natural downward velocity
				yVel = mob.getDeltaMovement().y;
				if (!mob.onGround()) {
					yVel -= 0.08;
				}
			}

			mob.setDeltaMovement(dirX * currentSpeed, yVel, dirZ * currentSpeed);
			mob.hasImpulse = true;

			// Integrate with walk animation controller so legs drive dynamically at high speed
			mob.zza = 1.0F;

			// Screen shaking effect on nearby players during intense high-speed charge
			List<Player> nearbyPlayers = world.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(20.0));
			for (Player p : nearbyPlayers) {
				if (p instanceof ServerPlayer sp) {
					double pDist = p.distanceTo(mob);
					double intensity = Math.max(0.05, 1.0 - (pDist / 20.0));
					// Subtly perturb player motion to create physical camera/screen tremor
					double shakeX = (p.getRandom().nextDouble() - 0.5) * 0.08 * intensity;
					double shakeY = (p.getRandom().nextDouble() - 0.5) * 0.04 * intensity;
					double shakeZ = (p.getRandom().nextDouble() - 0.5) * 0.08 * intensity;
					p.setDeltaMovement(p.getDeltaMovement().add(shakeX, shakeY, shakeZ));
					p.hasImpulse = true;
					sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
				}
			}

			// Flank particles exactly 4.5 blocks away from bounding box sides (left and right only)
			if (world instanceof ServerLevel sl) {
				double flankOffset = 4.5;
				// Left flank
				double leftX = mob.getX() - perpX * flankOffset;
				double leftZ = mob.getZ() - perpZ * flankOffset;
				sl.sendParticles(ParticleTypes.POOF, leftX, mob.getY() + 0.3, leftZ, 4, 0.3, 0.2, 0.3, 0.05);
				sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, leftX, mob.getY() + 0.2, leftZ, 3, 0.3, 0.1, 0.3, 0.02);

				// Right flank
				double rightX = mob.getX() + perpX * flankOffset;
				double rightZ = mob.getZ() + perpZ * flankOffset;
				sl.sendParticles(ParticleTypes.POOF, rightX, mob.getY() + 0.3, rightZ, 4, 0.3, 0.2, 0.3, 0.05);
				sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, rightX, mob.getY() + 0.2, rightZ, 3, 0.3, 0.1, 0.3, 0.02);
			}

			// Rapid heavy footstep sounds and sonic boom crackle
			if (dashTicks % 3 == 0) {
				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2.0F, 0.55F);
				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WARDEN_STEP, SoundSource.HOSTILE, 1.8F, 0.65F);
				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.6F, 1.6F);
			}

			// Effortlessly break blocks with hardness under 20 in front without caving floor
			performDashBlockDestruction(world, mob, lockedYaw);

			// Ramming impact on living entities
			AABB impactBox = mob.getBoundingBox().inflate(1.5, 0.5, 1.5);
			List<LivingEntity> hitEntities = world.getEntitiesOfClass(LivingEntity.class, impactBox, e -> e != mob && !isWoodboundOrWeaver(e) && e.isAlive());
			for (LivingEntity victim : hitEntities) {
				victim.hurt(getDamageSource(world, mob), DASH_IMPACT_DAMAGE);
				victim.setDeltaMovement(dirX * 1.6, 0.50, dirZ * 1.6);
				victim.hasImpulse = true;
				if (victim instanceof ServerPlayer sp) {
					sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
				}
				mob.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.70F);
				mob.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.8F, 0.80F);
				if (world instanceof ServerLevel sl) {
					sl.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 1.0, victim.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
				}
			}

			double maxHealth = mob.getMaxHealth();
			double currentHealth = mob.getHealth();
			double criticalThreshold = maxHealth * 0.35;
			boolean isWeaverCritical = currentHealth <= criticalThreshold;
			if (isWeaverCritical) {
				double hpReducedInCritical = criticalThreshold - currentHealth;
				double extraBlocks = Math.floor(hpReducedInCritical / 3.0) * 6.0;
				targetDist += 16.0 + extraBlocks; // Base critical prolongation of 16 blocks, plus 6 blocks every 3 HP reduced in critical state
				for (LivingEntity victim : hitEntities) {
					if (dashTicks % 8 == 0) {
						mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
						victim.hurt(getDamageSource(world, mob), 18.0F);
						mob.level().playSound(null, victim.getX(), victim.getY() + 1.0, victim.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F, 0.6F);
						mob.level().playSound(null, victim.getX(), victim.getY() + 1.0, victim.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.0F, 0.5F);
						if (world instanceof ServerLevel sl) {
							sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
						}
					}
				}
			}

			// Distance check towards saved target destination
			double traveledDist = Math.sqrt((mob.getX() - startX) * (mob.getX() - startX) + (mob.getZ() - startZ) * (mob.getZ() - startZ));
			if (traveledDist >= targetDist || dashTicks >= 120) {
				stopMobMotion(mob);
				mob.getNavigation().stop();
				clearOtherStates(mData, "DASH_FATIGUE");
				updateStateAndLog(mob, mData, "DASH_FATIGUE");
				mData.putBoolean("is_dashing", false);
				mData.putBoolean("is_dash_fatigue", true);
				mData.putInt("dash_fatigue_timer", DASH_FATIGUE_TICKS);
				mData.putFloat("dash_lock_yaw", lockedYaw);

				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 2.0F, 0.65F);
				if (world instanceof ServerLevel sl) {
					sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mob.getX(), mob.getY() + 1.2, mob.getZ(), 30, 0.6, 0.6, 0.6, 0.05);
					sl.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 1.0, mob.getZ(), 2, 0.4, 0.4, 0.4, 0.0);
				}
				syncWeaverEntityData(mob, false, mData);
				return;
			}

			syncWeaverEntityData(mob, false, mData);
			return;
		}

		// STATE: DASH FATIGUE STATE (1.0s post-dash recovery freeze)
		boolean isDashFatigue = persistentBoolean(mData, "is_dash_fatigue", false);
		if (isDashFatigue) {
			clearOtherStates(mData, "DASH_FATIGUE");
			updateStateAndLog(mob, mData, "DASH_FATIGUE");
			stopMobMotion(mob);
			mob.getNavigation().stop();

			float lockedYaw = persistentFloat(mData, "dash_lock_yaw", mob.getYRot());
			mob.setYRot(lockedYaw);
			mob.yRotO = lockedYaw;
			mob.setYBodyRot(lockedYaw);
			mob.yBodyRotO = lockedYaw;
			mob.setYHeadRot(lockedYaw);
			mob.yHeadRotO = lockedYaw;
			mob.setXRot(0.0F);
			mob.xRotO = 0.0F;

			int fatigueTimer = persistentInt(mData, "dash_fatigue_timer", DASH_FATIGUE_TICKS) - 1;
			mData.putInt("dash_fatigue_timer", fatigueTimer);

			if (world instanceof ServerLevel sl && fatigueTimer % 4 == 0) {
				sl.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1.2, mob.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
			}

			if (fatigueTimer <= 0) {
				mData.putBoolean("is_dash_fatigue", false);
				mData.putInt("dash_fatigue_timer", 0);
				if (persistentBoolean(mData, "dash_is_override", false)) {
					int savedCd = persistentInt(mData, "dash_saved_cooldown", 0);
					mData.putInt("dash_cooldown", savedCd);
					mData.putBoolean("dash_is_override", false);
				} else {
					LivingEntity dashTarget = mob.getTarget();
					if (dashTarget != null) {
						double dist = mob.distanceTo(dashTarget);
						int baseDashCooldown = DASH_COOLDOWN_TICKS;
						if (dist > 16.0) {
							baseDashCooldown = 160; // Far target: aggressive quick-recharge to catch up
						} else if (dist < 12.0) {
							baseDashCooldown = 420; // Close target: relaxed pace
						} else {
							// Interpolate smoothly between 12 and 16 blocks:
							double pct = (dist - 12.0) / 4.0;
							baseDashCooldown = (int) (420 - pct * 260);
						}
						mData.putInt("dash_cooldown", baseDashCooldown);
					} else {
						mData.putInt("dash_cooldown", DASH_COOLDOWN_TICKS);
					}
				}
				clearOtherStates(mData, "NORMAL");
				updateStateAndLog(mob, mData, "NORMAL");
			}

			syncWeaverEntityData(mob, false, mData);
			return;
		}

		// STATE: CHARGE TELEPORT STATE (Distance-based chargeup teleport)
		boolean isChargeTeleporting = persistentBoolean(mData, "is_charge_teleporting", false);
		if (isChargeTeleporting) {
			clearOtherStates(mData, "CHARGE_TELEPORT");
			updateStateAndLog(mob, mData, "CHARGE_TELEPORT");
			stopMobMotion(mob);
			mob.getNavigation().stop();

			int chargeTimer = persistentInt(mData, "charge_tp_timer", 0) + 1;
			int maxChargeTicks = persistentInt(mData, "charge_tp_max_duration", CHARGE_TELEPORT_MIN_DURATION);
			mData.putInt("charge_tp_timer", chargeTimer);

			LivingEntity tpTarget = mob.getTarget();
			if (tpTarget == null || !tpTarget.isAlive()) {
				tpTarget = getNearestSurvivalPlayer(world, mob);
			}

			if (tpTarget != null) {
				double dx = tpTarget.getX() - mob.getX();
				double dz = tpTarget.getZ() - mob.getZ();
				float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
				applyRotationToAll(mob, targetYaw);
				mData.putFloat("weaver_hypnosis_yaw", targetYaw);
			}

			if (world instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1.5, mob.getZ(), 8, 0.5, 1.0, 0.5, 0.2);
				serverLevel.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1.0, mob.getZ(), 5, 0.3, 0.8, 0.3, 0.05);
				if (chargeTimer % 10 == 0) {
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5F, 0.5F + (chargeTimer / (float) maxChargeTicks) * 0.5F);
				}
			}

			if (chargeTimer >= maxChargeTicks) {
				long targetSpotLong = mData.getLong("charge_tp_pos");
				BlockPos destPos = (targetSpotLong != 0) ? BlockPos.of(targetSpotLong) : null;

				if (destPos != null) {
					if (world instanceof ServerLevel sl) {
						sl.sendParticles(ParticleTypes.DRAGON_BREATH, mob.getX(), mob.getY() + 1.5, mob.getZ(), 30, 0.5, 1.0, 0.5, 0.1);
					}
					mob.teleportTo(destPos.getX() + 0.5, destPos.getY(), destPos.getZ() + 0.5);
					if (world instanceof ServerLevel sl) {
						sl.sendParticles(ParticleTypes.PORTAL, destPos.getX() + 0.5, destPos.getY() + 1.5, destPos.getZ() + 0.5, 40, 0.6, 1.2, 0.6, 0.2);
						sl.sendParticles(ParticleTypes.REVERSE_PORTAL, destPos.getX() + 0.5, destPos.getY() + 1.5, destPos.getZ() + 0.5, 20, 0.4, 1.0, 0.4, 0.1);
					}
					mob.level().playSound(null, destPos.getX(), destPos.getY(), destPos.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.70F);

					if (tpTarget != null) {
						double dx = tpTarget.getX() - (destPos.getX() + 0.5);
						double dz = tpTarget.getZ() - (destPos.getZ() + 0.5);
						float finalYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
						applyRotationToAll(mob, finalYaw);
					}
				}

				mData.putBoolean("is_charge_teleporting", false);
				mData.putInt("charge_tp_timer", 0);
				mData.putInt("charge_tp_cooldown", 200); // 10s cooldown
				mData.putInt("chase_far_ticks", 0);
			}

			syncWeaverEntityData(mob, false, mData);
			return;
		}

		// STATE 5: MELEE / MINING ATTACK STATE (Supports 7 attack types)
		boolean isAttacking = persistentBoolean(mData, "is_attacking", false);
		if (isAttacking) {
			clearOtherStates(mData, "ATTACKING");
			updateStateAndLog(mob, mData, "ATTACKING");
			int attackTime = persistentInt(mData, "attack_timer", 0);
			boolean isBlockAttack = mData.getBoolean("is_block_attack");
			boolean isParrying = mData.getBoolean("is_parrying");
			float attackYaw = persistentFloat(mData, "attack_facing_yaw", mob.getYRot());

			if (isParrying) {
				// PARRY/SWAT STATE: Always face projectile source, but override with player facing if near player
				LivingEntity parryTarget = mob.getTarget();
				double pDist = (parryTarget != null && parryTarget.isAlive()) ? mob.distanceTo(parryTarget) : 999.0;
				if (pDist <= 10.0 && parryTarget != null) {
					double dx = parryTarget.getX() - mob.getX();
					double dz = parryTarget.getZ() - mob.getZ();
					float pYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
					applyRotationToAll(mob, pYaw);
					mData.putFloat("attack_facing_yaw", pYaw);
				} else {
					applyRotationToAll(mob, attackYaw);
				}

				// Align physical projectile swatting strictly with the visual apex of the sweep claw swing (ticks 14-26 of 40, attackTime 26 down to 14)
				if (attackTime <= 26 && attackTime >= 14) {
					processSwatProjectiles(mob, mData, world);
				}
			} else if (isBlockAttack) {
				// MINING STATE: Hold position during windup & 0.5s (10 ticks) post-break pause, then move while claw animation finishes naturally!
				int maxDuration = persistentInt(mData, "attack_max_duration", MELEE_ATTACK_DURATION);
				int targetPeakTick = persistentInt(mData, "attack_peak_tick", ATTACK_DAMAGE_TICK);
				int elapsedTicks = maxDuration - attackTime;

				long blockPosLong = mData.getLong("attack_block_pos");
				BlockPos minePos = (blockPosLong != 0) ? BlockPos.of(blockPosLong) : null;

				if (elapsedTicks <= targetPeakTick + 10) {
					// Stand still during chargeup, impact (targetPeakTick), and exactly 0.5s (10 ticks) post-break pause
					stopMobMotion(mob);
					if (minePos != null) {
						double dx_block = minePos.getX() + 0.5 - mob.getX();
						double dz_block = minePos.getZ() + 0.5 - mob.getZ();
						if (dx_block * dx_block + dz_block * dz_block > 0.001) {
							attackYaw = (float) (Math.atan2(dz_block, dx_block) * (180.0 / Math.PI)) - 90.0F;
							applyRotationToAll(mob, attackYaw);
							mData.putFloat("attack_facing_yaw", attackYaw);
						}
					} else {
						applyRotationToAll(mob, attackYaw);
					}
				} else {
					// 0.5s post-break pause complete! Resume moving immediately toward target while claw animation finishes playing
					LivingEntity bTarget = mob.getTarget();
					if (bTarget != null && bTarget.isAlive()) {
						mob.getNavigation().moveTo(bTarget, 1.25);
						// If obstacle ahead is broken/cleared, clear is_block_attack so Weaver seamlessly transitions to target attack without cutting animation
						if (minePos == null || world.getBlockState(minePos).isAir() || mob.getSensing().hasLineOfSight(bTarget)) {
							mData.putBoolean("is_block_attack", false);
						}
					}
				}
			} else {
				// MELEE ATTACK STATE: Lock facing to the attack target for the attack
				LivingEntity attackTarget = mob.getTarget();

				// Dynamic Melee Spacing: allow Woodweaver to move towards target during claw swings, but maintain comfortable distance (don't ram bounding box)
				if (attackTarget != null && attackTarget.isAlive()) {
					double distToTarget = mob.distanceTo(attackTarget);
					if (distToTarget < 2.8) {
						// Within ideal claw strike distance (2.8m): stop forward movement so it doesn't ram or clip player's bounding box
						mob.getNavigation().stop();
						mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
					} else if (distToTarget <= 6.0) {
						// Advance steadily towards target during claw swing using straight-line vector physics to avoid pathfinder prediction twitching
						mob.getNavigation().stop();
						double speed = 0.22;
						double dx = attackTarget.getX() - mob.getX();
						double dz = attackTarget.getZ() - mob.getZ();
						double len = Math.sqrt(dx * dx + dz * dz);
						if (len > 0.001) {
							mob.setDeltaMovement(dx / len * speed, mob.getDeltaMovement().y, dz / len * speed);
						}
					}
					
					// Perfect lock-on aiming (Instant direct facing and vertical eye-locking to prevent lag/wobbling)
					double dx = attackTarget.getX() - mob.getX();
					double dz = attackTarget.getZ() - mob.getZ();
					double flatDist = Math.sqrt(dx * dx + dz * dz);
					if (flatDist > 0.001) {
						float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
						attackYaw = targetYaw;
						applyRotationToAll(mob, attackYaw);
						mData.putFloat("attack_facing_yaw", attackYaw);

						double lookDy = attackTarget.getEyeY() - mob.getEyeY();
						float targetPitch = (float) (-(Math.atan2(lookDy, flatDist) * (180.0 / Math.PI)));
						mob.setXRot(targetPitch);
						mob.xRotO = targetPitch;
					}
				} else {
					applyRotationToAll(mob, attackYaw);
				}
			}

			int attackType = persistentInt(mData, "attack_type", 1);
			int targetPeakTick = persistentInt(mData, "attack_peak_tick", ATTACK_DAMAGE_TICK);
			int maxDuration = persistentInt(mData, "attack_max_duration", MELEE_ATTACK_DURATION);

			if (attackTime == maxDuration - targetPeakTick) {
				if (isBlockAttack) {
					long blockPosLong = mData.getLong("attack_block_pos");
					if (blockPosLong != 0) {
						BlockPos minePos = BlockPos.of(blockPosLong);
						double rad = Math.toRadians(attackYaw);
						double fX = -Math.sin(rad);
						double fZ = Math.cos(rad);
						double rX = Math.cos(rad);
						double rZ = Math.sin(rad);

						LivingEntity currentTarget = mob.getTarget();

						// Generate randomized shift offsets per attack swing so successive claw slashes carve different sections of the 6x6 area
						double swingShiftS = (mob.getRandom().nextDouble() - 0.5) * 1.5;
						double swingShiftY = (mob.getRandom().nextDouble() - 0.5) * 1.5;

						// Organic scratch hole carving centered on minePos starting at foot level (5x5 to 6x6 area)
						int footY = (int) Math.floor(mob.getY());
						int minMineX = minePos.getX() - 3;
						int maxMineX = minePos.getX() + 3;
						int minMineY = Math.min(footY, minePos.getY());
						int maxMineY = Math.max(footY + 5, minePos.getY() + 5);
						int minMineZ = minePos.getZ() - 3;
						int maxMineZ = minePos.getZ() + 3;

						BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
						for (int bx = minMineX; bx <= maxMineX; bx++) {
							for (int by = minMineY; by <= maxMineY; by++) {
								for (int bz = minMineZ; bz <= maxMineZ; bz++) {
									mpos.set(bx, by, bz);
									double dx = bx - minePos.getX();
									double dy = by - minePos.getY();
									double dz = bz - minePos.getZ();
									double s = dx * rX + dz * rZ;

									// Organic 3D noise for jagged claw tear marks
									double noise = Math.sin(bx * 0.4 + by * 0.5 + bz * 0.4) * 0.6;
									
									boolean inScratch = false;

									// Core passability (guarantees Weaver's 11x11x6 model passes through the center)
									if (Math.abs(s) <= 1.2 && (by - footY) >= 0 && (by - footY) <= 4) {
										inScratch = true;
									}

									// Organic claw scratch lines matching swing type
									double shiftedS = s + swingShiftS;
									double shiftedY = dy + swingShiftY;
									double distFromCenter = Math.sqrt(shiftedS * shiftedS + shiftedY * shiftedY);
									double dynamicThickness = Math.max(0.4, 0.95 - (distFromCenter * 0.12));

									if (attackType == 3) { // Parallel vertical claw scrapes
										for (double cx : new double[]{-2.2, -0.8, 0.8, 2.2}) {
											if (Math.abs(shiftedS - (cx + noise)) <= dynamicThickness && shiftedY >= -3.5 && shiftedY <= 3.5) {
												inScratch = true;
											}
										}
									} else if (attackType == 6 || attackType == 7) { // Sweep Left / Sweep Right
										double slope = (attackType == 7) ? -0.4 : 0.4;
										for (double cy : new double[]{-1.8, -0.6, 0.6, 1.8}) {
											if (Math.abs(shiftedY - (slope * shiftedS + cy + noise)) <= dynamicThickness && Math.abs(shiftedS) <= 3.5) {
												inScratch = true;
											}
										}
									} else if (attackType == 4 || attackType == 5) { // Hook Left / Right (Steeper arcs)
										double slope = (attackType == 4) ? 0.7 : -0.7;
										for (double cy : new double[]{-1.8, -0.6, 0.6, 1.8}) {
											if (Math.abs(shiftedY - (slope * shiftedS + cy + noise)) <= dynamicThickness && Math.abs(shiftedS) <= 3.5) {
												inScratch = true;
											}
										}
									} else { // Diagonal Slash (Type 1, 2)
										double slope = (attackType == 1) ? -1.0 : 1.0;
										for (double cy : new double[]{-1.6, -0.5, 0.5, 1.6}) {
											if (Math.abs(shiftedY - (slope * shiftedS + cy + noise)) <= dynamicThickness && Math.abs(shiftedS) <= 3.2) {
												inScratch = true;
											}
										}
									}

									if (inScratch && canMine(world, mpos, mob, currentTarget)) {
										BlockState bpState = world.getBlockState(mpos);
										float bpHardness = bpState.getDestroySpeed(world, mpos);
										if (!bpState.isAir() && bpHardness >= 0 && bpHardness <= MAX_CLAW_BLOCK_HARDNESS) {
											world.destroyBlock(mpos, true);
											if (world instanceof ServerLevel level) {
												level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, bpState), bx + 0.5, by + 0.5, bz + 0.5, 5, 0.2, 0.2, 0.2, 0.05);
											}
										}
									}
								}
							}
						}
					}
					mob.level().playSound(null, mob.getX(), mob.getY() + 1.2, mob.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F, 0.6F);
					mob.level().playSound(null, mob.getX(), mob.getY() + 1.2, mob.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.0F, 0.5F);
				}

				// Melee entity damage hit detection (runs for ALL 7 attack types)
				double yawRad = Math.toRadians(attackYaw);
				double fX = -Math.sin(yawRad);
				double fZ = Math.cos(yawRad);
				double rX = Math.cos(yawRad);
				double rZ = Math.sin(yawRad);

				// Determine attack geometry parameters based on attackType (1-7)
				double sideMult = 0.0;
				double frontMult = 1.0;

				switch (attackType) {
					case 1: // Attack Left (Left Arm Claw)
						sideMult = -1.0;
						frontMult = 1.0;
						break;
					case 2: // Attack Right (Right Arm Claw)
						sideMult = 1.0;
						frontMult = 1.0;
						break;
					case 3: // Attack Both / Stab (Thrust forward into ground)
						sideMult = 0.0;
						frontMult = 1.15;
						break;
					case 4: // Hook Left
						sideMult = -1.4;
						frontMult = 0.9;
						break;
					case 5: // Hook Right
						sideMult = 1.4;
						frontMult = 0.9;
						break;
					case 6: // Sweep Left
						sideMult = -1.8;
						frontMult = 1.05;
						break;
					case 7: // Sweep Right
						sideMult = 1.8;
						frontMult = 1.05;
						break;
					default:
						sideMult = 0.0;
						frontMult = 1.0;
						break;
				}

				double mobX = mob.getX();
				double mobY = mob.getY();
				double mobZ = mob.getZ();

				double clawFront = CLAW_FRONT_OFFSET * frontMult;
				double clawSide = CLAW_SIDE_OFFSET * sideMult;
				double clawY = mobY + CLAW_HEIGHT_OFFSET;

				double clawX = mobX + fX * clawFront + rX * clawSide;
				double clawZ = mobZ + fZ * clawFront + rZ * clawSide;
				Vec3 clawImpactPos = new Vec3(clawX, clawY, clawZ);

				// Special ground shockwave & impact destruction for both-arm ground stab / hammer (attackType == 3)
				if (attackType == 3) {
					double impactX = mobX + fX * clawFront;
					double impactZ = mobZ + fZ * clawFront;
					double impactY = mobY;
					BlockPos impactBP = BlockPos.containing(impactX, impactY, impactZ);

					if (world instanceof ServerLevel serverLevel) {
						for (int i = 0; i < 16; i++) {
							double angle = (2.0 * Math.PI / 16) * i;
							double px = impactX + Math.cos(angle) * 1.6;
							double pz = impactZ + Math.sin(angle) * 1.6;
							serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, impactY + 0.1, pz, 1, 0.05, 0.12, 0.05, 0.02);
							serverLevel.sendParticles(ParticleTypes.POOF, px, impactY + 0.1, pz, 1, 0.1, 0.18, 0.1, 0.04);
						}
						serverLevel.sendParticles(ParticleTypes.EXPLOSION, impactX, impactY + 0.2, impactZ, 1, 0.0, 0.0, 0.0, 0.0);
					}

					mob.level().playSound(null, impactX, impactY, impactZ, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2F, 0.7F);
					mob.level().playSound(null, impactX, impactY, impactZ, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 1.5F, 0.5F);
					mob.level().playSound(null, impactX, impactY, impactZ, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);

					BlockPos.betweenClosedStream(impactBP.offset(-1, -1, -1), impactBP.offset(1, 2, 1)).forEach(bp -> {
						double distSq = bp.distSqr(impactBP);
						if (distSq <= 3.2) {
							BlockState bpState = world.getBlockState(bp);
							float bpHardness = bpState.getDestroySpeed(world, bp);
							if (!bpState.isAir() && bpHardness >= 0 && bpHardness <= MAX_CLAW_BLOCK_HARDNESS) {
								if (mob.getRandom().nextFloat() < 0.70) {
									world.destroyBlock(bp, true);
									if (world instanceof ServerLevel level) {
										level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, bpState), bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.05);
									}
								}
							}
						}
					});
				}

				AABB searchBox = new AABB(
					mobX - 6.5, mobY - 2.0, mobZ - 6.5,
					mobX + 6.5, mobY + 6.5, mobZ + 6.5
				);

				List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != mob && !isWoodboundOrWeaver(e));
				for (LivingEntity targetEntity : targets) {
					AABB targetBox = targetEntity.getBoundingBox();
					double distSq = targetBox.distanceToSqr(clawImpactPos);
					boolean isHit = distSq <= (CLAW_HIT_RADIUS * CLAW_HIT_RADIUS);

					double dx = targetEntity.getX() - mobX;
					double dz = targetEntity.getZ() - mobZ;
					double f = dx * fX + dz * fZ;
					double s = dx * rX + dz * rZ;

					if (!isHit && f >= 0.5 && f <= 5.8) {
						double ds = s - clawSide;
						double relY = (targetEntity.getY() + targetEntity.getBbHeight() * 0.5) - clawY;
						if (Math.abs(ds) <= 2.2 && Math.abs(relY) <= 2.5) {
							isHit = true;
						}
					}

					if (isHit) {
						targetEntity.hurt(getDamageSource(world, mob), 18.0F);

						// MELEE ATTACKS VS SHIELDS: Disable shields and force randomized 2-7s (40-140 ticks) cooldown if target is blocking
						if (targetEntity instanceof Player blockingPlayer && blockingPlayer.isBlocking()) {
							int disableTicks = 40 + mob.getRandom().nextInt(101); // 40 to 140 ticks (2.0s to 7.0s)
							blockingPlayer.getCooldowns().addCooldown(blockingPlayer.getUseItem().getItem(), disableTicks);
							blockingPlayer.stopUsingItem();
							mob.level().playSound(null, blockingPlayer.getX(), blockingPlayer.getY(), blockingPlayer.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.3F, 0.8F);
							if (world instanceof ServerLevel serverLevel) {
								serverLevel.sendParticles(ParticleTypes.CRIT, blockingPlayer.getX(), blockingPlayer.getY() + 1.0, blockingPlayer.getZ(), 18, 0.3, 0.3, 0.3, 0.2);
							}
						}

						double kx, ky, kz;
						double tYaw = targetEntity.getYRot();
						double tRad = Math.toRadians(tYaw);

						if (attackType == 4) { // Left Hook: Left claw reaches out and hooks inward toward center-left
							kx = (fX * 0.8 - rX * 1.5);
							kz = (fZ * 0.8 - rZ * 1.5);
							ky = 0.25;
						} else if (attackType == 5) { // Right Hook: Right claw reaches out and hooks inward toward center-right
							kx = (fX * 0.8 + rX * 1.5);
							kz = (fZ * 0.8 + rZ * 1.5);
							ky = 0.25;
						} else if (attackType == 6) { // Sweep Left: Left claw sweeps across to the right (up-right / across)
							kx = (fX * 1.0 + rX * 2.0);
							kz = (fZ * 1.0 + rZ * 2.0);
							ky = 0.35;
						} else if (attackType == 7) { // Sweep Right: Right claw sweeps across to the left (down-left / across)
							kx = (fX * 1.0 - rX * 2.0);
							kz = (fZ * 1.0 - rZ * 2.0);
							ky = 0.35;
						} else if (attackType == 3) { // Attack Both: Dual downward slam/stab (↓ ↓)
							kx = fX * 1.40;
							kz = fZ * 1.40;
							ky = 0.12;
						} else { // Standard Slashes (1, 2)
							Vec3 away = targetEntity.position().subtract(mob.position());
							double d2d = Math.sqrt(away.x * away.x + away.z * away.z);
							double dirX = d2d > 0.001 ? away.x / d2d : fX;
							double dirZ = d2d > 0.001 ? away.z / d2d : fZ;
							kx = dirX * 0.90;
							kz = dirZ * 0.90;
							ky = 0.25;
						}

						targetEntity.setDeltaMovement(kx, ky, kz);
						targetEntity.hasImpulse = true;
						if (targetEntity instanceof ServerPlayer sp) {
							sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
						}
					}
				}

				mob.level().playSound(null, clawX, clawY, clawZ, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F, 0.6F);
				mob.level().playSound(null, clawX, clawY, clawZ, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.0F, 0.5F);

				if (IS_CLAW_DESTRUCTION_ENABLED && !isBlockAttack) {
					BlockPos centerBP = BlockPos.containing(mobX, mobY, mobZ);
					BlockPos.betweenClosedStream(centerBP.offset(-6, -2, -6), centerBP.offset(6, 6, 6)).forEach(bp -> {
						double dx = bp.getX() + 0.5 - mobX;
						double dz = bp.getZ() + 0.5 - mobZ;
						double f = dx * fX + dz * fZ;
						double s = dx * rX + dz * rZ;
						double relY = bp.getY() - mobY;

						if (f >= 0.5 && f <= 5.5) {
							boolean shouldDestroy = false;

							if (attackType == 6 || attackType == 7 || attackType == 4 || attackType == 5) {
								// Hooks & Sweeps: Horizontal ribbon path cut through terrain/walls at claw height
								double ds = s - clawSide;
								double dy = relY - CLAW_HEIGHT_OFFSET;
								if (Math.abs(dy) <= 0.85 && Math.abs(ds) <= 2.2) {
									shouldDestroy = (mob.getRandom().nextFloat() < 0.70);
								}
							} else if (attackType == 3) {
								// Stab / Both-arm Hammer: Vertically oriented path/slice
								if (Math.abs(s) <= 0.85 && relY >= -0.5 && relY <= 3.2) {
									shouldDestroy = (mob.getRandom().nextFloat() < 0.75);
								}
							} else {
								// Slashes (1, 2): Diagonal claw path cut
								double targetY = (attackType == 1) ? (CLAW_HEIGHT_OFFSET - (s - clawSide) * 0.7) : (CLAW_HEIGHT_OFFSET + (s - clawSide) * 0.7);
								double dy = relY - targetY;
								double ds = s - clawSide;
								if (Math.abs(dy) <= 0.9 && Math.abs(ds) <= 2.0) {
									shouldDestroy = (mob.getRandom().nextFloat() < 0.70);
								}
							}

							if (shouldDestroy) {
								BlockState state = world.getBlockState(bp);
								float hardness = state.getDestroySpeed(world, bp);
								if (!state.isAir() && hardness >= 0 && hardness <= MAX_CLAW_BLOCK_HARDNESS) {
									world.destroyBlock(bp, true);
									if (world instanceof ServerLevel level) {
										level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, state), bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 4, 0.1, 0.1, 0.1, 0.05);
									}
								}
							}
						}
					});
				}
			}

			if (attackTime <= 0) {
				mData.putBoolean("is_block_attack", false);
				mData.putLong("attack_block_pos", 0);
				mData.putBoolean("is_parrying", false);
				clearOtherStates(mData, "NORMAL");
				updateStateAndLog(mob, mData, "NORMAL");
				syncWeaverEntityData(mob, false, mData);
				return;
			}

			syncWeaverEntityData(mob, false, mData);
			return;
		}

		// GENERAL DECISION MAKING AND ACTIONS (STATE 0: NORMAL CHASE / IDLE)
		updateStateAndLog(mob, mData, "NORMAL");

		// Check for incoming projectiles and trigger Parry Sweep if timing is optimal
		if (processProjectileParryCheck(mob, mData, world)) {
			return;
		}


		// 2. Target Validation & Low-HP Harvest Mode Target Prioritization
		LivingEntity target = mob.getTarget();
		boolean isTargetValid = false;
		if (target != null && target.isAlive() && !isWoodboundOrWeaver(target)) {
			if (target instanceof Player p) {
				if (p instanceof ServerPlayer sp && sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL) {
					isTargetValid = true;
				}
			} else {
				isTargetValid = true;
			}
		}

		if (!isTargetValid) {
			mob.setTarget(null);
			target = null;
		}

		// 3. Search: Automatically acquire nearest Survival Player
		if (target == null) {
			Player nearestSurvival = getNearestSurvivalPlayer(world, mob);
			if (nearestSurvival != null) {
				mob.setTarget(nearestSurvival);
				target = nearestSurvival;
			}
		}

		if (target != null && target.isAlive()) {
			double distToTarget = mob.distanceTo(target);

			// =========================================================================
			// COGNITIVE LAYER: PERCEPTION & DIRECT-STARE TRACTOR BEAM MECHANIC
			// =========================================================================
			boolean isPillaring = isTargetPillaring(world, target, mob);
			Vec3 startEye = mob.getEyePosition(1.0F);
			Vec3 targetEye = target.getEyePosition(1.0F);
			BlockHitResult hit = world.clip(new net.minecraft.world.level.ClipContext(
				startEye, targetEye,
				net.minecraft.world.level.ClipContext.Block.COLLIDER,
				net.minecraft.world.level.ClipContext.Fluid.NONE,
				mob
			));
			boolean isLineOfSightBlocked = hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;

			mData.putBoolean("is_circling", false);
			Vec3 clawOrigin = new Vec3(mob.getX(), mob.getY() + 4.5, mob.getZ());

			if (isPillaring) {
				int toweringCircleTicks = persistentInt(mData, "towering_circle_ticks", 0);
				if (toweringCircleTicks < 80) {
					mData.putInt("towering_circle_ticks", toweringCircleTicks + 1);
					performSmoothCircling(mob, mData, target, world, 10.0, 1.25);
					syncWeaverEntityData(mob, false, mData);
					return;
				} else {
					BlockPos minePos = null;
					int targetY = (int) target.getY();
					int selfY = (int) mob.getY();
					
					outerLoop:
					for (int scanY = targetY - 1; scanY >= selfY; scanY--) {
						for (int dx = -1; dx <= 1; dx++) {
							for (int dz = -1; dz <= 1; dz++) {
								BlockPos bp = BlockPos.containing(target.getX() + dx, scanY, target.getZ() + dz);
								if (canMine(world, bp, mob, target)) {
									double dist = clawOrigin.distanceTo(new Vec3(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5));
									if (dist <= 5.5) {
										minePos = bp;
										break outerLoop;
									}
								}
							}
						}
					}

					if (minePos != null) {
						stopMobMotion(mob);
						clearOtherStates(mData, "ATTACKING");
						updateStateAndLog(mob, mData, "PLATFORM_DESTRUCTION");
						mData.putBoolean("is_attacking", true);
						
						int chooseAttack = mob.getRandom().nextInt(7) + 1;
						mData.putInt("attack_type", chooseAttack);
						
						int peakTick = 15;
						if (chooseAttack == 4 || chooseAttack == 5) peakTick = 12;
						else if (chooseAttack == 6 || chooseAttack == 7) peakTick = 25;
						mData.putInt("attack_peak_tick", peakTick);
						mData.putInt("attack_timer", MELEE_ATTACK_DURATION);
						mData.putBoolean("is_block_attack", true);
						mData.putLong("attack_block_pos", minePos.asLong());

						double dx_block = minePos.getX() + 0.5 - mob.getX();
						double dz_block = minePos.getZ() + 0.5 - mob.getZ();
						float attackYaw = (float) (Math.atan2(dz_block, dx_block) * (180.0 / Math.PI)) - 90.0F;
						mob.setYRot(attackYaw);
						mob.setYBodyRot(attackYaw);
						mob.setYHeadRot(attackYaw);
						mData.putFloat("attack_facing_yaw", attackYaw);

						syncWeaverEntityData(mob, false, mData);
						return;
					} else {
						mob.getNavigation().moveTo(target.getX(), mob.getY(), target.getZ(), 1.25);
						updateStateAndLog(mob, mData, "TOWER_APPROACHING");
						syncWeaverEntityData(mob, false, mData);
						return;
					}
				}
			} else {
				mData.putInt("towering_circle_ticks", 0);
			}

			// Decrement Flash Vaporize Mode cooldown
			int weaponFlashCooldown = persistentInt(mData, "weapon_flash_cooldown", 0);
			if (weaponFlashCooldown > 0) {
				mData.putInt("weapon_flash_cooldown", weaponFlashCooldown - 1);
			}

			boolean hasDirectLOS = (target != null && mob.getSensing().hasLineOfSight(target));
			BlockPos physicalObstruction = findPhysicalObstruction(world, mob, target, mob.distanceTo(target));
			if (physicalObstruction == null && !hasDirectLOS) {
				physicalObstruction = findFrontalObstruction(world, mob, target);
			}
			boolean isPhysicallyBlocked = (physicalObstruction != null && !hasDirectLOS);

			// Track position progression to detect if Weaver is stuck trying to reach target
			double prevX = persistentDouble(mData, "weaver_prev_x", mob.getX());
			double prevY = persistentDouble(mData, "weaver_prev_y", mob.getY());
			double prevZ = persistentDouble(mData, "weaver_prev_z", mob.getZ());
			double dMoveX = mob.getX() - prevX;
			double dMoveZ = mob.getZ() - prevZ;
			double horizMovedSqr = dMoveX * dMoveX + dMoveZ * dMoveZ;

			mData.putDouble("weaver_prev_x", mob.getX());
			mData.putDouble("weaver_prev_y", mob.getY());
			mData.putDouble("weaver_prev_z", mob.getZ());

			int stuckTicks = persistentInt(mData, "weaver_stuck_ticks", 0);
			boolean isTryingToReachTarget = (target != null && target.isAlive() && distToTarget > MELEE_ATTACK_RANGE && !isAnyHypnoActive && !mData.getBoolean("is_attacking"));
			if (isTryingToReachTarget && (horizMovedSqr < 0.0025 || isPhysicallyBlocked || !mob.getNavigation().isInProgress())) {
				stuckTicks++;
			} else {
				stuckTicks = Math.max(0, stuckTicks - 2);
			}
			mData.putInt("weaver_stuck_ticks", stuckTicks);
			boolean isMobStuck = (stuckTicks >= 18);

			// STUCK COOLDOWN OVERRIDE: If stuck, bypass/ignore leap or dash cooldown to reach target, preserving current cooldowns
			if (isMobStuck && mob.onGround() && !isAnyHypnoActive && distToTarget >= 3.5 && distToTarget <= 36.0) {
				boolean canLeap = distToTarget >= 8.0 && !isPressedAgainstWall(world, mob) && isCeilingHighEnough(world, mob) && isCeilingHighEnoughAboveTarget(world, target) && isLeapSpaceClear(world, mob, target);
				if (canLeap) {
					Vec3 predTarget = predictTargetLandingPos(mob, target, LEAP_PREPARATION_TICKS);
					BlockPos landPos = findGroundPos(world, predTarget.x, predTarget.y, predTarget.z);
					if (landPos == null) {
						landPos = findGroundPos(world, target.getX(), target.getY(), target.getZ());
					}
					if (landPos != null) {
						int savedLeapCd = persistentInt(mData, "leap_cooldown", 0);
						mData.putInt("weaver_stuck_ticks", 0);

						if (LEAP_PREPARATION_TICKS > 0) {
							clearOtherStates(mData, "LEAP_PREP");
							updateStateAndLog(mob, mData, "LEAP_PREPARING_STUCK_OVERRIDE");
							mData.putBoolean("is_preparing_leap", true);
							mData.putInt("leap_prep_ticks", 0);
							mData.putDouble("leap_land_x", landPos.getX() + 0.5);
							mData.putDouble("leap_land_y", landPos.getY());
							mData.putDouble("leap_land_z", landPos.getZ() + 0.5);
							mData.putInt("leap_cooldown", savedLeapCd); // Preserve current cooldown so it continues decrementing

							stopMobMotion(mob);

							double ldx = (landPos.getX() + 0.5) - mob.getX();
							double ldz = (landPos.getZ() + 0.5) - mob.getZ();
							float targetYaw = (float) (Math.atan2(ldz, ldx) * 180.0 / Math.PI) - 90.0F;
							mob.setYRot(targetYaw);
							mob.setYBodyRot(targetYaw);
							mob.setYHeadRot(targetYaw);
							mData.putFloat("leap_lock_yaw", targetYaw);

							syncWeaverEntityData(mob, true, mData);
							return;
						} else {
							clearOtherStates(mData, "LEAPING");
							updateStateAndLog(mob, mData, "LEAP_LAUNCH_STUCK_OVERRIDE");
							mData.putBoolean("is_leaping", true);
							mData.putDouble("leap_land_x", landPos.getX() + 0.5);
							mData.putDouble("leap_land_y", landPos.getY());
							mData.putDouble("leap_land_z", landPos.getZ() + 0.5);
							mData.putInt("leap_ticks", 0);
							mData.putInt("leap_cooldown", savedLeapCd); // Preserve current cooldown so it continues decrementing

							mData.putDouble("leap_start_x", mob.getX());
							mData.putDouble("leap_start_y", mob.getY());
							mData.putDouble("leap_start_z", mob.getZ());
							mData.putBoolean("was_airborne", false);

							double jumpDX = (landPos.getX() + 0.5) - mob.getX();
							double jumpDY = landPos.getY() - mob.getY();
							double jumpDZ = (landPos.getZ() + 0.5) - mob.getZ();
							double jumpHorizontalDist = Math.sqrt(jumpDX * jumpDX + jumpDZ * jumpDZ);
							if (jumpHorizontalDist < 0.1) jumpHorizontalDist = 0.1;

							double estAirTicks = Math.max(16.0, Math.min(26.0, 12.0 + jumpHorizontalDist * 0.7));
							double horizDragFactor = (1.0 - Math.pow(0.91, estAirTicks)) / 0.09;
							double horizSpeed = (jumpHorizontalDist / horizDragFactor) * LEAP_SPEED_MULTIPLIER;

							double velX = (jumpDX / jumpHorizontalDist) * horizSpeed;
							double velZ = (jumpDZ / jumpHorizontalDist) * horizSpeed;
							double velY = calculateLeapVelY(estAirTicks, jumpDY);

							stopMobMotion(mob);
							mob.setDeltaMovement(velX, velY, velZ);
							mob.hasImpulse = true;
							mob.setOnGround(false);

							float launchYaw = (float) (Math.atan2(jumpDZ, jumpDX) * 180.0 / Math.PI) - 90.0F;
							mob.setYRot(launchYaw);
							mob.setYBodyRot(launchYaw);
							mob.setYHeadRot(launchYaw);
							mData.putFloat("leap_lock_yaw", launchYaw);

							mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 1.5F, 0.40F);
							mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.2F, 0.60F);

							syncWeaverEntityData(mob, true, mData);
							return;
						}
					}
				} else if (ENABLE_DASH) {
					int savedDashCd = persistentInt(mData, "dash_cooldown", 0);
					mData.putInt("weaver_stuck_ticks", 0);

					double savedTargetX = target.getX();
					double savedTargetY = target.getY();
					double savedTargetZ = target.getZ();

					double ddx = savedTargetX - mob.getX();
					double ddz = savedTargetZ - mob.getZ();
					float dashYaw = (float) (Math.atan2(ddz, ddx) * (180.0 / Math.PI)) - 90.0F;

					clearOtherStates(mData, "DASHING");
					updateStateAndLog(mob, mData, "DASHING_STUCK_OVERRIDE");
					mData.putBoolean("is_dashing", true);
					mData.putInt("dash_ticks", 0);
					mData.putFloat("dash_lock_yaw", dashYaw);
					mData.putDouble("dash_start_x", mob.getX());
					mData.putDouble("dash_start_y", mob.getY());
					mData.putDouble("dash_start_z", mob.getZ());
					mData.putDouble("dash_target_x", savedTargetX);
					mData.putDouble("dash_target_y", savedTargetY);
					mData.putDouble("dash_target_z", savedTargetZ);
					mData.putDouble("dash_total_dist", distToTarget + 4.0);
					mData.putInt("dash_cooldown", savedDashCd); // Preserve current cooldown so it continues decrementing

					mob.getNavigation().stop();
					mob.setYRot(dashYaw);
					mob.yRotO = dashYaw;
					mob.setYBodyRot(dashYaw);
					mob.yBodyRotO = dashYaw;
					mob.setYHeadRot(dashYaw);
					mob.yHeadRotO = dashYaw;
					mob.setXRot(0.0F);
					mob.xRotO = 0.0F;

					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.70F);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.6F, 0.75F);
					if (world instanceof ServerLevel sl) {
						sl.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 1.0, mob.getZ(), 4, 0.5, 0.5, 0.5, 0.0);
					}

					syncWeaverEntityData(mob, false, mData);
					return;
				}
			}

			// CHARGE TELEPORT TRIGGER CHECK (When Weaver hasn't been near player for a while while chasing)
			int chargeTpCooldown = persistentInt(mData, "charge_tp_cooldown", 0);
			if (chargeTpCooldown > 0) {
				mData.putInt("charge_tp_cooldown", chargeTpCooldown - 1);
			}

			if (target != null && target.isAlive() && chargeTpCooldown <= 0 && !isPhysicallyBlocked) {
				distToTarget = mob.distanceTo(target);
				if (distToTarget >= 14.0 || !hasDirectLOS) {
					int chaseFarTicks = persistentInt(mData, "chase_far_ticks", 0) + 1;
					mData.putInt("chase_far_ticks", chaseFarTicks);

					if (chaseFarTicks >= CHARGE_TELEPORT_SEPARATION_TICKS) { // Hasn't been near player for configurable threshold
						BlockPos tpDest = findChargeTeleportSpot(world, target, mob);
						if (tpDest != null) {
							// Charge duration scales dynamically between configurable min and max
							int durationTicks = (int) Math.max(CHARGE_TELEPORT_MIN_DURATION, Math.min(CHARGE_TELEPORT_MAX_DURATION, CHARGE_TELEPORT_MIN_DURATION + (distToTarget - 12.0) * ((CHARGE_TELEPORT_MAX_DURATION - CHARGE_TELEPORT_MIN_DURATION) / 18.0)));
							mData.putBoolean("is_charge_teleporting", true);
							mData.putInt("charge_tp_timer", 0);
							mData.putInt("charge_tp_max_duration", durationTicks);
							mData.putLong("charge_tp_pos", tpDest.asLong());
							mData.putInt("chase_far_ticks", 0);

							mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.8F, 0.6F);
							syncWeaverEntityData(mob, false, mData);
							return;
						}
					}
				} else {
					mData.putInt("chase_far_ticks", 0);
				}
			}

			if (isPhysicallyBlocked) {
				int miningDuration = persistentInt(mData, "mining_duration_ticks", 0) + 1;
				mData.putInt("mining_duration_ticks", miningDuration);
				int currentDashCooldown = persistentInt(mData, "dash_cooldown", 0);

				if (ENABLE_DASH && currentDashCooldown <= 0 && target != null && target.isAlive() && miningDuration >= 80) {
					double savedTargetX = target.getX();
					double savedTargetY = target.getY();
					double savedTargetZ = target.getZ();

					double dx = savedTargetX - mob.getX();
					double dz = savedTargetZ - mob.getZ();
					float dashYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;

					clearOtherStates(mData, "DASHING");
					updateStateAndLog(mob, mData, "DASHING_MINING_IMPATIENCE");
					mData.putBoolean("is_dashing", true);
					mData.putInt("dash_ticks", 0);
					mData.putFloat("dash_lock_yaw", dashYaw);
					mData.putDouble("dash_start_x", mob.getX());
					mData.putDouble("dash_start_y", mob.getY());
					mData.putDouble("dash_start_z", mob.getZ());
					mData.putDouble("dash_target_x", savedTargetX);
					mData.putDouble("dash_target_y", savedTargetY);
					mData.putDouble("dash_target_z", savedTargetZ);
					mData.putDouble("dash_total_dist", mob.distanceTo(target) + 6.0); // Dash through and past target
					mData.putBoolean("dash_is_override", false);
					
					mob.getNavigation().stop();
					mob.setYRot(dashYaw);
					mob.yRotO = dashYaw;
					mob.setYBodyRot(dashYaw);
					mob.yBodyRotO = dashYaw;
					mob.setYHeadRot(dashYaw);
					mob.yHeadRotO = dashYaw;
					mob.setXRot(0.0F);
					mob.xRotO = 0.0F;

					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.70F);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.6F, 0.75F);
					if (world instanceof ServerLevel sl) {
						sl.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 1.0, mob.getZ(), 4, 0.5, 0.5, 0.5, 0.0);
					}
					syncWeaverEntityData(mob, false, mData);
					return;
				}

				mData.putInt("stare_ticks", 0);
				BlockPos minePos = physicalObstruction;

				// Trigger Weaponized Flash Special Move when obstruction is encountered and off cooldown
				boolean targetInReach = (target != null && mob.distanceTo(target) <= 36.0) || mob.getLastHurtByMob() != null;
				if (weaponFlashCooldown <= 0 && targetInReach && mob.getRandom().nextFloat() < 0.35F) {
					clearOtherStates(mData, "FLASH");
					mData.putInt("weapon_flash_cooldown", 400); // 20s cooldown (special move)
					mData.putInt("weapon_flash_phase", 1);
					mData.putInt("weapon_flash_ticks", 0);
					mData.putBoolean("isWeaponClose", false);

					// Store start anchor positions to completely eliminate entity drift & wobbling
					mData.putDouble("hypno_start_x", mob.getX());
					mData.putDouble("hypno_start_y", mob.getY());
					mData.putDouble("hypno_start_z", mob.getZ());
					mData.putInt("weapon_flash_saved_target", target.getId());

					double dx = target.getX() - mob.getX();
					double dz = target.getZ() - mob.getZ();
					if (dx * dx + dz * dz > 0.001) {
						float directYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
						mData.putFloat("weaver_hypnosis_yaw", directYaw);
						mob.setYRot(directYaw);
						mob.yRotO = directYaw;
						mob.setYBodyRot(directYaw);
						mob.setYHeadRot(directYaw);
						mob.yHeadRotO = directYaw;
						mob.yBodyRotO = directYaw;

						double rad = Math.toRadians(directYaw);
						double lookX = mob.getX() - Math.sin(rad) * 10.0;
						double lookZ = mob.getZ() + Math.cos(rad) * 10.0;
						mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 30.0F, 30.0F);
					}

					// Randomly choose opening animation between isHypnotizing2 (3.0s) and isHypnotizing3 (1.0s)
					boolean chooseHypno3 = mob.getRandom().nextBoolean();
					if (chooseHypno3) {
						mData.putBoolean("isHypnotizing3", true);
						mData.putBoolean("isHypnotizing2", false);
						mData.putBoolean("was_fast_flash", true);
					} else {
						mData.putBoolean("isHypnotizing2", true);
						mData.putBoolean("isHypnotizing3", false);
						mData.putBoolean("was_fast_flash", false);
					}

					stopMobMotion(mob);
					mob.getNavigation().stop();
					syncWeaverEntityData(mob, false, mData);
					return;
				}

				if (canMine(world, minePos, mob, target)) {
					mData.putInt("hard_block_wait_ticks", 0);
					// Measure 3D distance from mob center/eye position to minePos
					double distToMine = mob.getEyePosition().distanceTo(new Vec3(minePos.getX() + 0.5, minePos.getY() + 0.5, minePos.getZ() + 0.5));

					// Mine through obstruction if within claw mining reach (5.5 blocks)
					if (distToMine <= 5.5) {
						stopMobMotion(mob);
						if (!mData.getBoolean("is_block_attack")) {
							clearOtherStates(mData, "ATTACKING");
							updateStateAndLog(mob, mData, "OBSTRUCTION_DESTRUCTION");
							mData.putBoolean("is_attacking", true);
							
							int chooseAttack = mob.getRandom().nextInt(7) + 1;
							mData.putInt("attack_type", chooseAttack);
							
							int peakTick = 15;
							if (chooseAttack == 4 || chooseAttack == 5) peakTick = 12;
							else if (chooseAttack == 6 || chooseAttack == 7) peakTick = 25;
							mData.putInt("attack_peak_tick", peakTick);
							int blockAttackDuration = peakTick + 20; // peakTick + 20 (1s post-break delay)
							mData.putInt("attack_timer", blockAttackDuration);
							mData.putInt("attack_max_duration", blockAttackDuration);
							mData.putBoolean("is_block_attack", true);
							mData.putLong("attack_block_pos", minePos.asLong());

							double dx_block = minePos.getX() + 0.5 - mob.getX();
							double dz_block = minePos.getZ() + 0.5 - mob.getZ();
							float attackYaw = (float) (Math.atan2(dz_block, dx_block) * (180.0 / Math.PI)) - 90.0F;
							mob.setYRot(attackYaw);
							mob.setYBodyRot(attackYaw);
							mob.setYHeadRot(attackYaw);
							mData.putFloat("attack_facing_yaw", attackYaw);

							syncWeaverEntityData(mob, false, mData);
							return;
						} else {
							syncWeaverEntityData(mob, false, mData);
							return;
						}
					} else {
						// Outside mining reach (> 5.5 blocks): Move towards the obstruction block to get in mining range!
						mob.getNavigation().moveTo(minePos.getX() + 0.5, minePos.getY(), minePos.getZ() + 0.5, 1.25);
						updateStateAndLog(mob, mData, "APPROACHING_OBSTRUCTION");
						syncWeaverEntityData(mob, false, mData);
						return;
					}
				} else {
					// Encountered unmineable / harder block (e.g. Obsidian, Bedrock, or hardness > MAX_CLAW_BLOCK_HARDNESS)
					int waitTicks = persistentInt(mData, "hard_block_wait_ticks", 0) + 1;
					mData.putInt("hard_block_wait_ticks", waitTicks);

					mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

					if (waitTicks < 120) { // Wait up to 6 seconds (120 ticks) for player to emerge or move into sight
						mob.getNavigation().moveTo(minePos.getX() + 0.5, minePos.getY(), minePos.getZ() + 0.5, 0.8);
						updateStateAndLog(mob, mData, "WAITING_HARD_BLOCK");
						syncWeaverEntityData(mob, false, mData);
						return;
					} else {
						// Give up on target behind unmineable hard blocks!
						mData.putInt("hard_block_wait_ticks", 0);
						updateStateAndLog(mob, mData, "GIVING_UP_HARD_BLOCK");
						mob.setTarget(null);

						Player newTarget = getNearestSurvivalPlayer(world, mob);
						if (newTarget != null && !newTarget.equals(target)) {
							mob.setTarget(newTarget);
						}
						syncWeaverEntityData(mob, false, mData);
						return;
					}
				}
			} else {
				mData.putInt("hard_block_wait_ticks", 0);
				mData.putInt("mining_duration_ticks", 0);
			}

			// OVERPOWERED TARGET (INFINITY TOTEM) AND RANGED / DEFENSIVE INTELLIGENCE
			boolean isOverpowered = isTargetHoldingInfinityTotem(target);
			if (isOverpowered) {
				// If cornered (too close < 6.0 blocks), initiate retreat logic
				if (distToTarget < 6.0) {
					if (leapCooldown <= 0 && !persistentBoolean(mData, "is_preparing_leap", false) && !persistentBoolean(mData, "is_leaping", false)) {
						BlockPos landPos = getOverpowerLeapTarget(world, mob, target);
						if (landPos != null) {
							// Trigger Leap over Target
							clearOtherStates(mData, "LEAP_PREP");
							updateStateAndLog(mob, mData, "LEAP_PREPARING");
							mData.putBoolean("is_preparing_leap", true);
							mData.putInt("leap_prep_ticks", 0);
							mData.putDouble("leap_land_x", landPos.getX() + 0.5);
							mData.putDouble("leap_land_y", landPos.getY());
							mData.putDouble("leap_land_z", landPos.getZ() + 0.5);
							mData.putInt("leap_cooldown", 100);

							stopMobMotion(mob);

							double ldx = (landPos.getX() + 0.5) - mob.getX();
							double ldz = (landPos.getZ() + 0.5) - mob.getZ();
							float targetYaw = (float) (Math.atan2(ldz, ldx) * 180.0 / Math.PI) - 90.0F;
							mob.setYRot(targetYaw);
							mob.setYBodyRot(targetYaw);
							mob.setYHeadRot(targetYaw);
							mData.putFloat("leap_lock_yaw", targetYaw);

							syncWeaverEntityData(mob, true, mData);
							return;
						} else {
							// No room to leap! Charge up teleportation
							mData.putBoolean("weaver_tp_charging", true);
							mData.putInt("weaver_tp_timer", 0);
							mData.putInt("weaver_tp_target_id", target.getId());
							stopMobMotion(mob);
							syncWeaverEntityData(mob, false, mData);
							return;
						}
					} else {
						// Leap on cooldown, back up!
						if (target instanceof Player playerTarget) {
							performSmoothRetreat(world, mob, playerTarget, mData);
							syncWeaverEntityData(mob, false, mData);
							return;
						}
					}
				}
			}

			// HYPNOSIS INITIALIZATION TRIGGER (Requires Grounded, Line of Sight, and 4.0 - 22.0 block proximity)
			boolean isMiningOrAttackActive = mData.getBoolean("is_block_attack") || mData.getBoolean("is_attacking");
			boolean isAirborne = !mob.onGround() || persistentBoolean(mData, "is_leaping", false) || persistentBoolean(mData, "is_preparing_leap", false);
			double minHypnoDist = 4.0;

			if (hypnoCooldown <= 0 && !isAirborne && distToTarget >= minHypnoDist && distToTarget <= HYPNO_MAX_RANGE && !isLineOfSightBlocked && !isPhysicallyBlocked && !isMiningOrAttackActive) {
				if (target instanceof Player playerTarget) {
					if (persistentInt(mData, "stare_ticks", 0) == 0) {
						mData.putInt("stare_ticks", 1);
						mData.putString("hypno_mode", chooseHypnoMode(mob, mData, playerTarget));
						mData.putInt("hypno_target_id", playerTarget.getId());
						mData.putString("hypno_target_ids", "");
						mData.putFloat("hypno_target_start_health", playerTarget.getHealth());
						stopMobMotion(mob);
						mob.getNavigation().stop();
						mob.setTarget(null);

						double dx = playerTarget.getX() - mob.getX();
						double dz = playerTarget.getZ() - mob.getZ();
						float directYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
						mData.putFloat("weaver_hypnosis_yaw", directYaw);
						applyRotationToAll(mob, directYaw);

						mData.putDouble("hypno_start_x", mob.getX());
						mData.putDouble("hypno_start_y", mob.getY());
						mData.putDouble("hypno_start_z", mob.getZ());

						clearOtherStates(mData, "STARE_CHARGING");
						updateStateAndLog(mob, mData, "STARE_CHARGING");
						syncWeaverEntityData(mob, false, mData);
						return;
					}
				}
			}

			// Melee claw attack trigger (Suppressed if target is holding Infinity Totem)
			if (!isOverpowered && !isAnyHypnoActive && distToTarget <= MELEE_ATTACK_RANGE && attackTimer <= 0) {
				clearOtherStates(mData, "ATTACKING");
				mData.putInt("stare_ticks", 0); // Clear stare ticks so gaze pre-charging never overrides active melee
				updateStateAndLog(mob, mData, "ATTACKING");
				mData.putBoolean("is_attacking", true);
				
				// Pick attack type: 1=Attack_left, 2=Attack_right, 3=Attack_both (Stab), 4=Hook Left, 5=Hook Right, 6=Sweep Left, 7=Sweep Right
				int chooseAttack = mob.getRandom().nextInt(7) + 1;
				mData.putInt("attack_type", chooseAttack);
				
				int peakTick = 15;
				if (chooseAttack == 4 || chooseAttack == 5) peakTick = 12; // Hooks strike fast
				else if (chooseAttack == 6 || chooseAttack == 7) peakTick = 25; // Sweeps strike late
				mData.putInt("attack_peak_tick", peakTick);
				mData.putInt("attack_timer", MELEE_ATTACK_DURATION);

				double dx = target.getX() - mob.getX();
				double dz = target.getZ() - mob.getZ();
				float attackYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
				applyRotationToAll(mob, attackYaw);
				mData.putFloat("attack_facing_yaw", attackYaw);
				syncWeaverEntityData(mob, false, mData);
				return;
			}

			// DASH ABILITY TRIGGER (Toggleable Kool-Aid Man / Ramming Dash when far from player)
			int dashCooldown = persistentInt(mData, "dash_cooldown", 0);
			if (dashCooldown > 0) {
				mData.putInt("dash_cooldown", dashCooldown - 1);
			}

			boolean targetGettingAwayQuickly = false;
			double targetSpeedSqr = 0;
			if (ENABLE_DASH && dashCooldown > 0 && mob.onGround() && !isAnyHypnoActive && distToTarget >= DASH_MIN_DISTANCE && distToTarget <= DASH_MAX_DISTANCE) {
				Vec3 targetVel = target.getDeltaMovement();
				targetSpeedSqr = targetVel.x * targetVel.x + targetVel.z * targetVel.z;
				if (targetSpeedSqr > 0.0225) { // Faster than normal walking speed (~0.15 blocks/tick / 3 blocks/sec)
					Vec3 toTarget = target.position().subtract(mob.position());
					double dist = toTarget.length();
					if (dist > 0.1) {
						Vec3 dirToTarget = toTarget.normalize();
						double dot = targetVel.x * dirToTarget.x + targetVel.z * dirToTarget.z;
						if (dot > 0.12) { // Moving away from the Weaver at a decent speed
							targetGettingAwayQuickly = true;
						}
					}
				}
			}

			int ambushCooldown = persistentInt(mData, "ambush_wall_cooldown", 0);
			if (ambushCooldown > 0) mData.putInt("ambush_wall_cooldown", ambushCooldown - 1);
			
			if (ENABLE_DASH && dashCooldown <= 0 && ambushCooldown <= 0 && mob.onGround() && !isAnyHypnoActive && target != null && target.isAlive()) {
				boolean isTurtling = (targetSpeedSqr < 0.0025) && !hasDirectLOS;
				boolean isHealing = target.getHealth() < target.getMaxHealth() && !hasDirectLOS;
				
				if (isTurtling || isHealing || targetGettingAwayQuickly) {
					BlockPos ambushSpot = findWallAmbushSpot(world, target, mob);
					if (ambushSpot != null) {
						if (world instanceof ServerLevel sl) {
							sl.sendParticles(ParticleTypes.DRAGON_BREATH, mob.getX(), mob.getY() + 1.5, mob.getZ(), 30, 0.5, 1.0, 0.5, 0.1);
						}
						mob.teleportTo(ambushSpot.getX() + 0.5, ambushSpot.getY(), ambushSpot.getZ() + 0.5);
						mob.level().playSound(null, ambushSpot.getX(), ambushSpot.getY(), ambushSpot.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.60F);
						
						if (mob.getRandom().nextFloat() < 0.3F && target instanceof net.minecraft.world.entity.player.Player player) {
							player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false, false));
						}
						
						double dx = target.getX() - mob.getX();
						double dz = target.getZ() - mob.getZ();
						float ambushYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
						
						mData.putBoolean("is_ambush_windup", true);
						mData.putInt("ambush_windup_ticks", 0);
						mData.putFloat("ambush_target_yaw", ambushYaw);
						mData.putDouble("dash_target_x", target.getX());
						mData.putDouble("dash_target_y", target.getY());
						mData.putDouble("dash_target_z", target.getZ());
						
						mData.putInt("ambush_wall_cooldown", 400); // 20s cooldown
						syncWeaverEntityData(mob, false, mData);
						return;
					}
				}
			}

			if (ENABLE_DASH && (dashCooldown <= 0 || targetGettingAwayQuickly) && mob.onGround() && !isAnyHypnoActive && distToTarget >= DASH_MIN_DISTANCE && distToTarget <= DASH_MAX_DISTANCE) {
				double savedTargetX = target.getX();
				double savedTargetY = target.getY();
				double savedTargetZ = target.getZ();

				double dx = savedTargetX - mob.getX();
				double dz = savedTargetZ - mob.getZ();
				float dashYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;

				clearOtherStates(mData, "DASHING");
				if (targetGettingAwayQuickly) {
					updateStateAndLog(mob, mData, "DASHING_ESCAPE_OVERRIDE");
				} else {
					updateStateAndLog(mob, mData, "DASHING");
				}
				mData.putBoolean("is_dashing", true);
				mData.putInt("dash_ticks", 0);
				mData.putFloat("dash_lock_yaw", dashYaw);
				mData.putDouble("dash_start_x", mob.getX());
				mData.putDouble("dash_start_y", mob.getY());
				mData.putDouble("dash_start_z", mob.getZ());
				mData.putDouble("dash_target_x", savedTargetX);
				mData.putDouble("dash_target_y", savedTargetY);
				mData.putDouble("dash_target_z", savedTargetZ);
				mData.putDouble("dash_total_dist", distToTarget + 4.0); // Dash through and past saved target position

				if (targetGettingAwayQuickly) {
					mData.putBoolean("dash_is_override", true);
					mData.putInt("dash_saved_cooldown", dashCooldown);
				} else {
					mData.putBoolean("dash_is_override", false);
				}

				mob.getNavigation().stop();
				mob.setYRot(dashYaw);
				mob.yRotO = dashYaw;
				mob.setYBodyRot(dashYaw);
				mob.yBodyRotO = dashYaw;
				mob.setYHeadRot(dashYaw);
				mob.yHeadRotO = dashYaw;
				mob.setXRot(0.0F);
				mob.xRotO = 0.0F;

				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.70F);
				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.6F, 0.75F);
				if (world instanceof ServerLevel sl) {
					sl.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 1.0, mob.getZ(), 4, 0.5, 0.5, 0.5, 0.0);
				}

				syncWeaverEntityData(mob, false, mData);
				return;
			}

			// LOW-HEALTH / DESPERATION & HEIGHT ADAPTATION CHECKS
			boolean isDesperate = (mob.getHealth() / mob.getMaxHealth()) < 0.35f;
			boolean isTargetElevated = (target.getY() > mob.getY() + 3.0);

			// Leap ability trigger
			boolean isFleeing = false;
			if (target.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
				Vec3 tVel = target.getDeltaMovement().normalize();
				Vec3 toTarget = target.position().subtract(mob.position()).normalize();
				double dot = tVel.dot(toTarget);
				if (dot > 0.25) {
					isFleeing = true;
				}
			}

			boolean forceLeapLocal = persistentBoolean(mData, "leap_requested", false);
			boolean leapConditionMet = forceLeapLocal || isFleeing || isTargetElevated || isDesperate;

			int baseLeapCooldown = LEAP_COOLDOWN_DEFAULT;
			if (distToTarget > 16.0) {
				baseLeapCooldown = 100; // Far target: aggressive quick-recharge to catch up
			} else if (distToTarget < 12.0) {
				baseLeapCooldown = 240; // Close target: relaxed pace
			} else {
				// Interpolate smoothly between 12 and 16 blocks:
				double pct = (distToTarget - 12.0) / 4.0;
				baseLeapCooldown = (int) (240 - pct * 140);
			}

			int chosenLeapCooldown = isDesperate ? (baseLeapCooldown / 2) : baseLeapCooldown;

			if ((forceLeapLocal || (leapCooldown <= 0 && leapConditionMet)) && distToTarget >= LEAP_MIN_DISTANCE && distToTarget <= LEAP_MAX_DISTANCE && !isPressedAgainstWall(world, mob) && isCeilingHighEnoughAboveTarget(world, target)) {
				if (isCeilingHighEnough(world, mob)) {
					if (isLeapSpaceClear(world, mob, target)) {
						Vec3 predTarget = predictTargetLandingPos(mob, target, LEAP_PREPARATION_TICKS);
						BlockPos landPos = findGroundPos(world, predTarget.x, predTarget.y, predTarget.z);
						if (landPos == null) {
							landPos = findGroundPos(world, target.getX(), target.getY(), target.getZ());
						}
						if (landPos != null) {
							if (LEAP_PREPARATION_TICKS > 0) {
								clearOtherStates(mData, "LEAP_PREP");
								updateStateAndLog(mob, mData, "LEAP_PREPARING");
								mData.putBoolean("is_preparing_leap", true);
								mData.putInt("leap_prep_ticks", 0);
								mData.putDouble("leap_land_x", landPos.getX() + 0.5);
								mData.putDouble("leap_land_y", landPos.getY());
								mData.putDouble("leap_land_z", landPos.getZ() + 0.5);
								mData.putInt("leap_cooldown", chosenLeapCooldown);

								stopMobMotion(mob);

								double ldx = (landPos.getX() + 0.5) - mob.getX();
								double ldz = (landPos.getZ() + 0.5) - mob.getZ();
								float targetYaw = (float) (Math.atan2(ldz, ldx) * 180.0 / Math.PI) - 90.0F;
								mob.setYRot(targetYaw);
								mob.setYBodyRot(targetYaw);
								mob.setYHeadRot(targetYaw);
								mData.putFloat("leap_lock_yaw", targetYaw);

								syncWeaverEntityData(mob, true, mData);
								return;
							} else {
								clearOtherStates(mData, "LEAPING");
								updateStateAndLog(mob, mData, "LEAP_LAUNCH");
								mData.putBoolean("is_leaping", true);
								mData.putDouble("leap_land_x", landPos.getX() + 0.5);
								mData.putDouble("leap_land_y", landPos.getY());
								mData.putDouble("leap_land_z", landPos.getZ() + 0.5);
								mData.putInt("leap_ticks", 0);
								mData.putInt("leap_cooldown", chosenLeapCooldown);

								mData.putDouble("leap_start_x", mob.getX());
								mData.putDouble("leap_start_y", mob.getY());
								mData.putDouble("leap_start_z", mob.getZ());
								mData.putBoolean("was_airborne", false);

								double jumpDX = (landPos.getX() + 0.5) - mob.getX();
								double jumpDY = landPos.getY() - mob.getY();
								double jumpDZ = (landPos.getZ() + 0.5) - mob.getZ();
								double jumpHorizontalDist = Math.sqrt(jumpDX * jumpDX + jumpDZ * jumpDZ);
								if (jumpHorizontalDist < 0.1) jumpHorizontalDist = 0.1;

								double estAirTicks = Math.max(16.0, Math.min(26.0, 12.0 + jumpHorizontalDist * 0.7));
								double horizDragFactor = (1.0 - Math.pow(0.91, estAirTicks)) / 0.09;
								double horizSpeed = (jumpHorizontalDist / horizDragFactor) * LEAP_SPEED_MULTIPLIER;

								double velX = (jumpDX / jumpHorizontalDist) * horizSpeed;
								double velZ = (jumpDZ / jumpHorizontalDist) * horizSpeed;
								double velY = calculateLeapVelY(estAirTicks, jumpDY);

								stopMobMotion(mob);
								mob.setDeltaMovement(velX, velY, velZ);
								mob.hasImpulse = true;
								mob.setOnGround(false);

								float launchYaw = (float) (Math.atan2(jumpDZ, jumpDX) * 180.0 / Math.PI) - 90.0F;
								mob.setYRot(launchYaw);
								mob.setYBodyRot(launchYaw);
								mob.setYHeadRot(launchYaw);
								mData.putFloat("leap_lock_yaw", launchYaw);

								mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 1.5F, 0.40F);
								mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.2F, 0.60F);

								syncWeaverEntityData(mob, true, mData);
								return;
							}
						}
					} else {
						mData.putInt("leap_cooldown", 30);
						mData.putBoolean("leap_requested", false);
					}
				}
			}
			// Active relentless forward pursuit navigation towards target when outside melee range
			if (distToTarget > MELEE_ATTACK_RANGE) {
				updateStateAndLog(mob, mData, "PURSUING");

				int repathCooldown = persistentInt(mData, "path_repath_cooldown", 0);
				double lastTargetX = persistentDouble(mData, "path_last_target_x", 0.0);
				double lastTargetY = persistentDouble(mData, "path_last_target_y", 0.0);
				double lastTargetZ = persistentDouble(mData, "path_last_target_z", 0.0);

				double targetMovedSqr = (target.getX() - lastTargetX) * (target.getX() - lastTargetX)
									  + (target.getY() - lastTargetY) * (target.getY() - lastTargetY)
									  + (target.getZ() - lastTargetZ) * (target.getZ() - lastTargetZ);

				boolean navDoneOrStuck = !mob.getNavigation().isInProgress();
				int maxRepathCooldown = isDesperate ? 10 : 20; // Stable repath cooldown (same pattern as Tremorzilla/Alex's Caves)
				boolean shouldRepath = (repathCooldown <= 0 && targetMovedSqr > 4.0) || navDoneOrStuck;

				double navSpeed = isDesperate ? 1.45 : 1.30;

				if (shouldRepath) {
					// Use direct entity navigation so Vanilla's pathfinder tracks the target smoothly without discrete coordinate snapping
					mob.getNavigation().moveTo(target, navSpeed);
					mData.putInt("path_repath_cooldown", maxRepathCooldown);
					mData.putDouble("path_last_target_x", target.getX());
					mData.putDouble("path_last_target_y", target.getY());
					mData.putDouble("path_last_target_z", target.getZ());
				} else {
					mData.putInt("path_repath_cooldown", repathCooldown - 1);
				}

				// Head look tracking towards target - let MoveControl and BodyRotationControl handle body rotation smoothly without manual yaw fighting
				mob.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), 30.0F, 30.0F);

				// SMOOTH ADAPTIVE BODY ROTATION AND TURNING RATE LIMITING:
				// Automatically align the entity's body yaw dynamically with its real movement/velocity vector
				// to eliminate high-frequency visual "yaw fighting" and snapping when turning corners.
				net.minecraft.world.phys.Vec3 vel = mob.getDeltaMovement();
				double horizontalSpeedSqr = vel.x * vel.x + vel.z * vel.z;
				float targetYaw;
				if (horizontalSpeedSqr > 0.001) {
					targetYaw = (float) (Math.atan2(vel.z, vel.x) * (180.0 / Math.PI)) - 90.0F;
				} else {
					double dx = target.getX() - mob.getX();
					double dz = target.getZ() - mob.getZ();
					targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
				}

				float currentYaw = mob.getYRot();
				float currentBody = mob.yBodyRot;
				float currentHead = mob.yHeadRot;

				float yawDiff = net.minecraft.util.Mth.wrapDegrees(targetYaw - currentYaw);
				float clampedDiff = net.minecraft.util.Mth.clamp(yawDiff, -12.0F, 12.0F); // Cap turn-rate per tick for gorgeous physical weight
				float newYaw = currentYaw + clampedDiff;

				mob.yRotO = currentYaw;
				mob.yBodyRotO = currentBody;
				mob.yHeadRotO = currentHead;

				mob.setYRot(newYaw);
				mob.setYBodyRot(newYaw);

				float headYawDiff = net.minecraft.util.Mth.wrapDegrees(targetYaw - currentHead);
				mob.setYHeadRot(currentHead + net.minecraft.util.Mth.clamp(headYawDiff, -20.0F, 20.0F));

				// LEG WALL AVOIDANCE (Toggleable):
				if (WEAVER_LEG_WALL_AVOIDANCE) {
					double dL = getWallDistance(world, mob, getLeftVec(mob.getYRot()), 4.5);
					double dR = getWallDistance(world, mob, getRightVec(mob.getYRot()), 4.5);
					if (dL < 4.5 && dR >= 4.5) {
						double steerForce = Math.min(0.15, (4.5 - dL) * 0.04);
						net.minecraft.world.phys.Vec3 steerVec = getRightVec(mob.getYRot()).scale(steerForce);
						net.minecraft.world.phys.Vec3 motion = mob.getDeltaMovement();
						mob.setDeltaMovement(motion.x + steerVec.x, motion.y, motion.z + steerVec.z);
					} else if (dR < 4.5 && dL >= 4.5) {
						double steerForce = Math.min(0.15, (4.5 - dR) * 0.04);
						net.minecraft.world.phys.Vec3 steerVec = getLeftVec(mob.getYRot()).scale(steerForce);
						net.minecraft.world.phys.Vec3 motion = mob.getDeltaMovement();
						mob.setDeltaMovement(motion.x + steerVec.x, motion.y, motion.z + steerVec.z);
					}
				}
			}
		} else {
			mData.putInt("stare_ticks", 0);
		}

		syncWeaverEntityData(mob, false, mData);
	}

	private static void stopMobMotion(Mob mob) {
		mob.getNavigation().stop();
		mob.setZza(0);
		mob.setXxa(0);
		if (mob.getDeltaMovement().x != 0 || mob.getDeltaMovement().z != 0) {
			mob.setDeltaMovement(0, Math.min(0.0, mob.getDeltaMovement().y), 0);
		}
	}

	private static void updateStateAndLog(Mob mob, CompoundTag mData, String newState) {
		String oldState = persistentString(mData, "last_debug_state", "NORMAL");
		if (!oldState.equals(newState)) {
			mData.putString("last_debug_state", newState);
			if (WEAVER_DEBUG) {
				System.out.printf("[Weaver AI] [SERVER] %s state transition: %s -> %s%n",
					mob.getDisplayName().getString(), oldState, newState);
			}
		}
	}

	private static void clearOtherStates(CompoundTag mData, String activeState) {
		if (!"HYPNO".equals(activeState) && !"STARE".equals(activeState) && !"STARE_CHARGING".equals(activeState) && !"HYPNO_POSITIONING".equals(activeState)) {
			setHypnoPhase(mData, 0);
			mData.putInt("stare_ticks", 0);
			mData.putInt("hypno_ticks", 0);
			mData.putInt("closing_hypno_ticks", 0);
			mData.putInt("hypno_pos_ticks", 0);
			mData.putInt("hypno_target_id", 0);
			mData.putBoolean("is_backing_up", false);
		}
		if (!"LEAP_PREP".equals(activeState) && !"LEAPING".equals(activeState)) {
			mData.putBoolean("is_preparing_leap", false);
			mData.putBoolean("is_leaping", false);
			mData.putInt("leap_prep_ticks", 0);
			mData.putInt("leap_ticks", 0);
			mData.putBoolean("was_airborne", false);
			mData.putBoolean("leap_requested", false);
		}
		if (!"LANDING".equals(activeState)) {
			mData.putBoolean("is_landing_anim", false);
			mData.putInt("landing_anim_timer", 0);
		}
		if (!"DASHING".equals(activeState) && !"DASH_FATIGUE".equals(activeState)) {
			mData.putBoolean("is_dashing", false);
			mData.putBoolean("is_dash_fatigue", false);
			mData.putInt("dash_ticks", 0);
			mData.putInt("dash_fatigue_timer", 0);
		}
		if (!"ATTACKING".equals(activeState)) {
			mData.putBoolean("is_attacking", false);
			mData.putInt("attack_timer", 0);
		}
		if (!"ANIM_PREVIEW".equals(activeState)) {
			mData.putBoolean("is_playing_anim", false);
			mData.putInt("anim_timer", 0);
			mData.putBoolean("anim_jump", false);
		}
	}

	private static double calculateLeapVelY(double estAirTicks, double jumpDY) {
		double velY = 0.04 * estAirTicks + (jumpDY / estAirTicks) + LEAP_HEIGHT_BOOST * 0.48;
		return Math.max(0.65, Math.min(1.85, velY));
	}

	private static void setHypnoPhase(CompoundTag mData, int phase) {
		mData.putInt("hypno_phase", phase);
		mData.putBoolean("is_hypnotizing", phase == 1 || phase == 2);
		mData.putBoolean("is_closing_hypno", phase == 3);
		mData.putBoolean("is_backing_up", false);
		if (phase == 1 || phase == 2) {
			mData.putInt("hypno_attack_lockout", 0);
			mData.putBoolean("is_block_attack", false);
			mData.putLong("attack_block_pos", 0);
			mData.putBoolean("is_block_obstruction", false);
			mData.putBoolean("is_climbing_obstruction", false);
			mData.putInt("obstruction_destruction_ticks", 0);
			mData.putBoolean("is_attacking", false);
			mData.putInt("attack_timer", 0);
		}
	}

	private static Vec3 predictTargetLandingPos(Mob mob, LivingEntity target, double prepTicks) {
		Vec3 targetVel = target.getKnownMovement();
		if (targetVel.lengthSqr() < 0.0001) {
			targetVel = new Vec3(target.getX() - target.xOld, 0, target.getZ() - target.zOld);
		}

		double currentDist = mob.distanceTo(target);
		double estimatedFlightTicks = Math.max(16.0, Math.min(26.0, 12.0 + currentDist * 0.7));
		double totalLeadTicks = prepTicks + estimatedFlightTicks;

		double predX = target.getX() + targetVel.x * totalLeadTicks;
		double predZ = target.getZ() + targetVel.z * totalLeadTicks;
		double predY = target.getY();

		double dx = predX - mob.getX();
		double dz = predZ - mob.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist > LEAP_MAX_DISTANCE) {
			predX = mob.getX() + (dx / dist) * LEAP_MAX_DISTANCE;
			predZ = mob.getZ() + (dz / dist) * LEAP_MAX_DISTANCE;
		}

		return new Vec3(predX, predY, predZ);
	}

	private static void spawnHeavyLandingShockwave(ServerLevel level, Mob mob) {
		double x = mob.getX();
		double y = mob.getY();
		double z = mob.getZ();

		BlockPos groundPos = mob.blockPosition().below();
		BlockState floorState = level.getBlockState(groundPos);
		if (floorState.isAir()) {
			floorState = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
		}

		net.minecraft.core.particles.BlockParticleOption dustPillar = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, floorState);
		net.minecraft.core.particles.BlockParticleOption blockParticle = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, floorState);

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 0.2, z, 2, 0.5, 0.2, 0.5, 0.0);
		level.sendParticles(ParticleTypes.FLASH, x, y + 0.5, z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.POOF, x, y + 0.3, z, 40, 1.5, 0.4, 1.5, 0.12);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.2, z, 25, 1.8, 0.3, 1.8, 0.08);

		int ringParticles = 36;
		for (int i = 0; i < ringParticles; i++) {
			double angle = (2.0 * Math.PI / ringParticles) * i;
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);

			double vx = cos * 1.8;
			double vz = sin * 1.8;
			double px = x + cos * 1.0;
			double pz = z + sin * 1.0;

			level.sendParticles(dustPillar, px, y + 0.15, pz, 0, vx, 0.05, vz, 1.0);
			level.sendParticles(blockParticle, px, y + 0.15, pz, 0, vx * 0.7, 0.15, vz * 0.7, 0.5);
		}

		for (int r = 1; r <= 3; r++) {
			double dist = r * 1.2;
			int count = r * 6;
			for (int i = 0; i < count; i++) {
				double angle = (2.0 * Math.PI / count) * i;
				double px = x + Math.cos(angle) * dist;
				double pz = z + Math.sin(angle) * dist;

				level.sendParticles(dustPillar, px, y + 0.1, pz, 2, 0.1, 0.4 + r * 0.1, 0.1, 0.15);
				level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, y + 0.1, pz, 1, 0.1, 0.3, 0.1, 0.05);
			}
		}

		level.playSound(null, x, y, z, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 3.0F, 0.40F);
		level.playSound(null, x, y, z, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 3.0F, 0.45F);
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.40F);
		level.playSound(null, x, y, z, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2.5F, 0.40F);
	}

	private static void applyRotationToAll(Mob mob, float targetYaw) {
		float currentYaw = mob.getYRot();
		float currentBody = mob.yBodyRot;
		float currentHead = mob.yHeadRot;

		float yawDiff = net.minecraft.util.Mth.wrapDegrees(targetYaw - currentYaw);
		float clampedDiff = net.minecraft.util.Mth.clamp(yawDiff, -8.0F, 8.0F);
		float newYaw = currentYaw + clampedDiff;

		mob.yRotO = currentYaw;
		mob.yBodyRotO = currentBody;
		mob.yHeadRotO = currentHead;

		mob.setYRot(newYaw);
		mob.setYBodyRot(newYaw);
		mob.setYHeadRot(newYaw);

		double rad = Math.toRadians(newYaw);
		double lookX = mob.getX() - Math.sin(rad) * 10.0;
		double lookZ = mob.getZ() + Math.cos(rad) * 10.0;
		mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 10.0F, 10.0F);
	}

	private static void updateAndApplyHypnosisYaw(Mob mob, CompoundTag mData, Entity target, boolean allowSmoothTurn) {
		float currentHypnoYaw = persistentFloat(mData, "weaver_hypnosis_yaw", mob.getYRot());
		if (target != null) {
			double dx = target.getX() - mob.getX();
			double dy = (target.getEyeY()) - mob.getEyeY();
			double dz = target.getZ() - mob.getZ();
			double horizontalDist = Math.sqrt(dx * dx + dz * dz);

			if (horizontalDist > 0.001) {
				float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
				float yawDiff = net.minecraft.util.Mth.wrapDegrees(targetYaw - currentHypnoYaw);
				float maxTurn = allowSmoothTurn ? 8.0F : 12.0F;
				currentHypnoYaw += net.minecraft.util.Mth.clamp(yawDiff, -maxTurn, maxTurn);
				mData.putFloat("weaver_hypnosis_yaw", currentHypnoYaw);
			}

			// Calculate vertical pitch (up/down) so head and eyes aim smoothly at target without oscillation
			float targetPitch = (float) (-(Math.atan2(dy, Math.max(0.1, horizontalDist)) * (180.0 / Math.PI)));
			targetPitch = net.minecraft.util.Mth.clamp(targetPitch, -75.0F, 75.0F);
			float currentXRot = mob.getXRot();
			float pitchDiff = net.minecraft.util.Mth.wrapDegrees(targetPitch - currentXRot);
			float maxPitchTurn = allowSmoothTurn ? 5.0F : 10.0F;
			float newPitch = currentXRot + net.minecraft.util.Mth.clamp(pitchDiff, -maxPitchTurn, maxPitchTurn);
			mob.setXRot(newPitch);
			mob.xRotO = newPitch;

			mob.setYRot(currentHypnoYaw);
			mob.yRotO = currentHypnoYaw;
			mob.setYBodyRot(currentHypnoYaw);
			mob.setYHeadRot(currentHypnoYaw);
			mob.yHeadRotO = currentHypnoYaw;
			mob.yBodyRotO = currentHypnoYaw;

			mob.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), 12.0F, 12.0F);
		} else {
			applyRotationToAll(mob, currentHypnoYaw);
		}
	}

	private static void applyPermanentMaxHpBonus(Mob mob, CompoundTag mData, float addAmount) {
		if (mob == null || mob.level().isClientSide()) return;
		float currentBonus = persistentFloat(mData, "weaver_permanent_bonus_max_hp", 0.0F);
		float newBonus = currentBonus + addAmount;
		if (addAmount > 0.0F) {
			mData.putFloat("weaver_permanent_bonus_max_hp", newBonus);
		}

		net.minecraft.world.entity.ai.attributes.AttributeInstance maxHealthAttr = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
		if (maxHealthAttr != null) {
			net.minecraft.resources.ResourceLocation modifierId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thebackwoods", "weaver_hp_leech_bonus");
			maxHealthAttr.removeModifier(modifierId);
			if (newBonus > 0.0F) {
				maxHealthAttr.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
					modifierId, (double) newBonus, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
				));
			}
		}

		if (addAmount > 0.0F) {
			mob.heal(addAmount);
		}
	}

	private static List<LivingEntity> getHypnoTargets(Mob mob, CompoundTag mData) {
		List<LivingEntity> list = new java.util.ArrayList<>();
		if (mob == null || mob.level() == null) return list;

		String targetIdsStr = persistentString(mData, "hypno_target_ids", "");
		if (!targetIdsStr.isEmpty()) {
			String[] parts = targetIdsStr.split(",");
			for (String p : parts) {
				try {
					int id = Integer.parseInt(p.trim());
					Entity e = mob.level().getEntity(id);
					if (e instanceof LivingEntity le && le.isAlive()) {
						list.add(le);
					}
				} catch (Exception ignored) {}
			}
		} else {
			int singleId = persistentInt(mData, "hypno_target_id", 0);
			if (singleId != 0) {
				Entity e = mob.level().getEntity(singleId);
				if (e instanceof LivingEntity le && le.isAlive()) {
					list.add(le);
				}
			}
		}
		return list;
	}

	private static void setHypnoTargets(CompoundTag mData, List<? extends LivingEntity> targets) {
		if (targets == null || targets.isEmpty()) {
			mData.putInt("hypno_target_id", 0);
			mData.putString("hypno_target_ids", "");
			return;
		}

		mData.putInt("hypno_target_id", targets.get(0).getId());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < targets.size(); i++) {
			if (i > 0) sb.append(",");
			sb.append(targets.get(i).getId());
		}
		mData.putString("hypno_target_ids", sb.toString());
	}

	private static void applyTractorBeamPullMulti(Mob mob, List<LivingEntity> targets, double pullRamp) {
		if (mob == null || targets == null || targets.isEmpty()) return;

		CompoundTag mData = mob.getPersistentData();
		float hypnoYaw = persistentFloat(mData, "weaver_hypnosis_yaw", mob.getYRot());
		double yawRad = Math.toRadians(hypnoYaw);

		Vec3 beamOrigin = new Vec3(mob.getX(), mob.getEyeY(), mob.getZ());
		Vec3 beamDir = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();

		int N = targets.size();
		double mainHoldX = mob.getX() + beamDir.x * HYPNO_HOLD_DISTANCE;
		double mainHoldZ = mob.getZ() + beamDir.z * HYPNO_HOLD_DISTANCE;
		double mainHoldY = mob.getY() + HYPNO_HOLD_HEIGHT;

		for (int i = 0; i < N; i++) {
			LivingEntity target = targets.get(i);
			if (target == null || !target.isAlive()) continue;

			if (target instanceof Player p && (p.isCreative() || p.isSpectator())) continue;

			if (!mob.hasLineOfSight(target)) continue;

			Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
			Vec3 toTarget = targetCenter.subtract(beamOrigin);
			double distAlongRay = toTarget.x * beamDir.x + toTarget.z * beamDir.z;

			if (distAlongRay < 0.0 || distAlongRay > TRACTOR_BEAM_MAX_RANGE) continue;

			Vec3 closestPoint = beamOrigin.add(beamDir.scale(distAlongRay));
			Vec3 perpVec = new Vec3(targetCenter.x - closestPoint.x, 0, targetCenter.z - closestPoint.z);
			double perpendicularDistance = perpVec.length();

			double maxRadius = BEAM_SIZE_MULTIPLIER * (distAlongRay + 30.0) * 0.014;
			if (perpendicularDistance > maxRadius) continue;

			double targetHoldX, targetHoldY, targetHoldZ;
			if (N == 1) {
				targetHoldX = mainHoldX;
				targetHoldY = mainHoldY;
				targetHoldZ = mainHoldZ;
			} else {
				// Distribute multiple harvested targets in an arc spread so they never overlap or clip
				double spreadAngle = (i - (N - 1) / 2.0) * 0.45; // ~25 degrees per target
				double targetAngle = yawRad + spreadAngle;
				double holdRadius = 4.2 + (i % 2 == 0 ? 0.0 : 0.8);
				targetHoldX = mob.getX() - Math.sin(targetAngle) * holdRadius;
				targetHoldZ = mob.getZ() + Math.cos(targetAngle) * holdRadius;
				targetHoldY = mob.getY() + HYPNO_HOLD_HEIGHT + (i % 2 == 1 ? 0.35 : -0.2);
			}

			double pullX = targetHoldX - target.getX();
			double pullY = targetHoldY - target.getY();
			double pullZ = targetHoldZ - target.getZ();
			double dist = Math.sqrt(pullX * pullX + pullY * pullY + pullZ * pullZ);

			double ramp = Math.max(0.0, Math.min(1.0, pullRamp));
			if (dist > 0.05) {
				double pullFactorXZ = 0.28 * ramp;
				double pullFactorY = 0.25 * ramp;
				double maxPull = 0.65 * Math.max(0.1, ramp);

				double velX = pullX * pullFactorXZ;
				double velY = pullY * pullFactorY;
				double velZ = pullZ * pullFactorXZ;

				double len = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
				if (len > maxPull) {
					velX = (velX / len) * maxPull;
					velY = (velY / len) * maxPull;
					velZ = (velZ / len) * maxPull;
				}

				if (velY > 0.38) velY = 0.38;

				target.setDeltaMovement(velX, velY, velZ);
			} else {
				target.setDeltaMovement(0, 0.01, 0);
			}

			target.fallDistance = 0.0F;
			target.hasImpulse = true;

			if (target instanceof ServerPlayer sp) {
				sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
			}
		}

		// Trap and suspend projectiles in mid-air
		trapAndHoldProjectilesInBeam(mob, beamOrigin, beamDir, mainHoldX, mainHoldY, mainHoldZ);
	}

	private static void processLightExtinctionPulseTick(Mob mob, CompoundTag mData) {
		int pulseR = persistentInt(mData, "extinction_pulse_radius", 0);
		if (pulseR > 0 && pulseR <= 64) {
			processLightExtinctionShell(mob, pulseR, Math.min(64, pulseR + 3));
			mData.putInt("extinction_pulse_radius", pulseR + 4);
		} else if (pulseR > 64) {
			mData.putInt("extinction_pulse_radius", 0);
		}
	}

	private static void processLightExtinctionShell(Mob mob, int minR, int maxR) {
		if (mob == null || !(mob.level() instanceof ServerLevel level)) return;

		BlockPos mobPos = mob.blockPosition();
		int minR2 = minR * minR;
		int maxR2 = maxR * maxR;

		for (int x = -maxR; x <= maxR; x += 2) {
			for (int z = -maxR; z <= maxR; z += 2) {
				int x2z2 = x * x + z * z;
				if (x2z2 < minR2 - 16 || x2z2 > maxR2 + 16) continue;

				for (int y = -16; y <= 16; y += 2) {
					int dist2 = x2z2 + y * y;
					if (dist2 >= minR2 && dist2 <= maxR2) {
						BlockPos targetPos = mobPos.offset(x, y, z);
						BlockState state = level.getBlockState(targetPos);

						if (!state.isAir()) {
							int lightEmission = state.getLightEmission(level, targetPos);
							if (lightEmission > 0) {
								if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
									level.setBlock(targetPos, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false), 3);
									level.sendParticles(ParticleTypes.SMOKE, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.02);
								} else {
									float hardness = state.getDestroySpeed(level, targetPos);
									if (hardness >= 0.0F && hardness <= 10.0F) {
										level.destroyBlock(targetPos, true);
										level.sendParticles(ParticleTypes.SMOKE, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private static void trapAndHoldProjectilesInBeam(Mob mob, Vec3 beamOrigin, Vec3 beamDir, double targetHoldX, double targetHoldY, double targetHoldZ) {
		if (mob == null || mob.level() == null) return;
		AABB searchBox = mob.getBoundingBox().inflate(36.0);
		List<net.minecraft.world.entity.projectile.Projectile> projectiles = mob.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, searchBox);

		long gameTime = mob.level().getGameTime();

		for (net.minecraft.world.entity.projectile.Projectile proj : projectiles) {
			if (proj == null || !proj.isAlive()) continue;

			Vec3 projPos = proj.position();
			Vec3 toProj = projPos.subtract(beamOrigin);
			double distAlongRay = toProj.x * beamDir.x + toProj.z * beamDir.z;

			boolean isTrapped = proj.getPersistentData().getBoolean("trapped_in_hypno_beam");

			if (!isTrapped) {
				if (distAlongRay >= -2.0 && distAlongRay <= TRACTOR_BEAM_MAX_RANGE + 10.0) {
					Vec3 closestPoint = beamOrigin.add(beamDir.scale(Math.max(0, distAlongRay)));
					double perpDist = projPos.distanceTo(closestPoint);
					double holdPointDist = projPos.distanceTo(new Vec3(targetHoldX, targetHoldY, targetHoldZ));

					if (perpDist <= 5.5 || holdPointDist <= 7.0) {
						proj.getPersistentData().putBoolean("trapped_in_hypno_beam", true);
						isTrapped = true;
					}
				}
			}

			if (isTrapped) {
				proj.setNoGravity(true);

				Vec3 vel = proj.getDeltaMovement();

				// Anti-gravity air drag (gradually decelerates forward momentum in zero-g like elastic fluid)
				double drag = 0.88;
				double velX = vel.x * drag;
				double velY = vel.y * drag;
				double velZ = vel.z * drag;

				// Elastic repulsion away from the target's bounding box (ensures projectiles NEVER hit the target)
				Vec3 targetCenter = new Vec3(targetHoldX, targetHoldY + 0.9, targetHoldZ);
				double distToTarget = projPos.distanceTo(targetCenter);
				double targetSafeRadius = 2.5;

				if (distToTarget < targetSafeRadius && distToTarget > 0.001) {
					Vec3 pushAway = projPos.subtract(targetCenter).normalize();
					double pushForce = (targetSafeRadius - distToTarget) * 0.25;
					velX += pushAway.x * pushForce;
					velY += pushAway.y * pushForce;
					velZ += pushAway.z * pushForce;
				}

				// Elastic repulsion away from Woodweaver's bounding box
				Vec3 weaverCenter = mob.position().add(0, mob.getBbHeight() * 0.5, 0);
				double distToWeaver = projPos.distanceTo(weaverCenter);
				double weaverSafeRadius = Math.max(3.0, mob.getBbWidth() + 1.0);
				if (distToWeaver < weaverSafeRadius && distToWeaver > 0.001) {
					Vec3 pushFromWeaver = projPos.subtract(weaverCenter).normalize();
					double pushForce = (weaverSafeRadius - distToWeaver) * 0.3;
					velX += pushFromWeaver.x * pushForce;
					velY += pushFromWeaver.y * pushForce;
					velZ += pushFromWeaver.z * pushForce;
				}

				// Anti-floor bounce protection: Ensure projectiles NEVER touch the floor
				BlockPos checkFloor = BlockPos.containing(proj.getX(), proj.getY() - 0.5, proj.getZ());
				if (proj.onGround() || mob.level().getBlockState(checkFloor).isSolid() || proj.getY() <= targetHoldY - 1.2) {
					velY = Math.max(velY, 0.30); // Gentle elastic bounce upward
					if (proj.onGround()) {
						proj.teleportTo(proj.getX(), proj.getY() + 0.25, proj.getZ());
					}
				}

				// Soft zero-g float buoyancy
				velY += 0.008;

				proj.setDeltaMovement(velX, velY, velZ);
				proj.hasImpulse = true;
			}
		}
	}

	private static void releaseTrappedProjectiles(Mob mob) {
		if (mob == null || mob.level() == null) return;
		AABB box = mob.getBoundingBox().inflate(48.0);
		List<net.minecraft.world.entity.projectile.Projectile> projectiles = mob.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, box);
		for (net.minecraft.world.entity.projectile.Projectile proj : projectiles) {
			if (proj != null && proj.getPersistentData().getBoolean("trapped_in_hypno_beam")) {
				proj.getPersistentData().remove("trapped_in_hypno_beam");
				proj.setNoGravity(false);
				proj.setDeltaMovement(0.0, -0.15, 0.0);
				proj.hasImpulse = true;
			}
		}
	}

	private static void cancelHypnosisCleanly(Mob mob, CompoundTag mData) {
		int savedTargetId = persistentInt(mData, "hypno_target_id", 0);
		String hypnoMode = persistentString(mData, "hypno_mode", "");
		boolean isInterrupted = persistentBoolean(mData, "hypno_interrupted", false);

		releaseTrappedProjectiles(mob);

		if (persistentBoolean(mData, "is_hypnotizing", false) || persistentInt(mData, "hypno_phase", 0) != 0) {
			mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.5F, 0.50F);
		}

		// Adjust AI bias based on performance if we had an active hypnosis mode
		if (!hypnoMode.isEmpty() && savedTargetId != 0) {
			Entity e = mob.level().getEntity(savedTargetId);
			if (e instanceof LivingEntity target) {
				float startHealth = persistentFloat(mData, "hypno_target_start_health", target.getHealth());
				float currentHealth = target.getHealth();
				float dmgDealt = startHealth - currentHealth;

				// Calculate success score
				double successScore = 0.0;
				if (isInterrupted) {
					successScore -= 2.0; // Heavily penalize interruptions/failures
				} else {
					successScore += Math.max(0.0, dmgDealt * 0.2); // Reward damage dealt
					if ("leech".equals(hypnoMode)) {
						// Leech also gains value from healing the weaver
						successScore += 1.0; 
					}
				}

				// Update bias
				String biasKey = "ai_bias_" + hypnoMode;
				double currentBias = persistentDouble(mData, biasKey, 0.0);
				currentBias = net.minecraft.util.Mth.clamp(currentBias + successScore, -10.0, 10.0);
				mData.putDouble(biasKey, currentBias);

				if (WEAVER_DEBUG) {
					System.out.printf("[Weaver AI] Evaluated hypnosis %s: dmgDealt=%.2f, interrupted=%b -> Score: %.2f. New bias: %.2f%n",
						hypnoMode, dmgDealt, isInterrupted, successScore, currentBias);
				}
			}
		}

		mData.putInt("hypno_phase", 0);
		mData.putBoolean("is_hypnotizing", false);
		mData.putBoolean("is_closing_hypno", false);
		mData.putBoolean("is_closing3s", false);
		mData.putBoolean("is_closing1_5s", false);
		mData.putBoolean("is_closing1s", false);
		mData.putBoolean("is_backing_up", false);
		mData.putBoolean("hypno_interrupted", false);
		mData.putInt("hypno_ticks", 0);
		mData.putInt("closing_hypno_ticks", 0);
		mData.putInt("hypno_pos_ticks", 0);
		mData.putInt("stare_ticks", 0);
		mData.putInt("hypno_hits_received", 0);
		mData.putInt("hypno_target_id", 0);
		mData.putString("hypno_target_ids", "");
		mData.remove("hypno_start_x");
		mData.remove("hypno_start_y");
		mData.remove("hypno_start_z");
		mData.remove("weaver_hypnosis_yaw");
		mData.remove("infect_max_duration");
		applyHypnoCooldown(mData);
		int currentLeapCd = persistentInt(mData, "leap_cooldown", 0);
		if (currentLeapCd < 60) {
			mData.putInt("leap_cooldown", 60);
		}
		mData.putInt("hypno_attack_lockout", HYPNO_RECOVERY_TICKS);

		// ADAPTIVE TARGET SWITCHING: Prioritize recent attacker who hit Weaver or broke hypnosis
		LivingEntity attacker = mob.getLastHurtByMob();
		if (attacker != null && attacker.isAlive() && !isWoodboundOrWeaver(attacker) && attacker instanceof ServerPlayer sp && sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL) {
			mob.setTarget(attacker);
		} else if (savedTargetId != 0) {
			Entity rawTarget = mob.level().getEntity(savedTargetId);
			if (rawTarget instanceof ServerPlayer sp && sp.isAlive() && !isWoodboundOrWeaver(sp) && sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL) {
				mob.setTarget(sp);
			}
		}

		clearOtherStates(mData, "NORMAL");
		updateStateAndLog(mob, mData, "NORMAL");
		syncWeaverEntityData(mob, false, mData);
	}

	private static <T> void setEntityDataIfChanged(WoodweaverEntity woodweaver, EntityDataAccessor<T> key, T value) {
		if (!woodweaver.getEntityData().get(key).equals(value)) {
			woodweaver.getEntityData().set(key, value);
		}
	}

	private static final java.util.Map<String, java.lang.reflect.Field> FIELD_CACHE = new java.util.HashMap<>();

	private static <T> void setEntityDataIfPresent(WoodweaverEntity woodweaver, String fieldName, T value) {
		try {
			java.lang.reflect.Field field = FIELD_CACHE.get(fieldName);
			if (field == null && !FIELD_CACHE.containsKey(fieldName)) {
				try {
					field = WoodweaverEntity.class.getDeclaredField(fieldName);
					field.setAccessible(true);
				} catch (NoSuchFieldException e) {
					try {
						field = WoodweaverEntity.class.getField(fieldName);
					} catch (NoSuchFieldException e2) {
						field = null;
					}
				}
				FIELD_CACHE.put(fieldName, field);
			}
			if (field != null) {
				@SuppressWarnings("unchecked")
				EntityDataAccessor<T> key = (EntityDataAccessor<T>) field.get(null);
				setEntityDataIfChanged(woodweaver, key, value);
			}
		} catch (Exception e) {
			// Field not present in this version's WoodweaverEntity, ignore safely
		}
	}

	/**
	 * Synchronizes local compound tag animation states directly to the SynchedEntityData of WoodweaverEntity.
	 */
	private static void syncWeaverEntityData(Mob mob, boolean isLeaping, CompoundTag mData) {
		if (mob instanceof WoodweaverEntity woodweaver) {
			boolean isLanding = persistentBoolean(mData, "is_landing_anim", false);
			int currentHypnoPhase = persistentInt(mData, "hypno_phase", 0);
			boolean isStaring = persistentInt(mData, "stare_ticks", 0) > 0;
			boolean isBackingUp = persistentBoolean(mData, "is_backing_up", false);
			boolean isAttacking = persistentBoolean(mData, "is_attacking", false);

			boolean isRetreating = persistentBoolean(mData, "is_retreating", false);
			boolean isRotatingLeft = persistentBoolean(mData, "is_rotating_left", false);
			boolean isRotatingRight = persistentBoolean(mData, "is_rotating_right", false);

			boolean isDying = persistentBoolean(mData, "is_dying", false);
			if (isDying) {
				isLeaping = false;
				isLanding = false;
				isAttacking = false;
				isBackingUp = false;
				isRetreating = false;
				isRotatingLeft = false;
				isRotatingRight = false;
				currentHypnoPhase = 0;
				isStaring = false;
			}
			
			// Force cancel stationary rotational animations while preparing leap, airborne/leaping, landing, dashing, dash fatigue, or attacking
			boolean isPreparingLeap = persistentBoolean(mData, "is_preparing_leap", false);
			boolean isDashing = persistentBoolean(mData, "is_dashing", false);
			boolean isDashFatigue = persistentBoolean(mData, "is_dash_fatigue", false);
			if (isPreparingLeap || isLeaping || isLanding || isAttacking || isDashing || isDashFatigue || !mob.onGround()) {
				isRotatingLeft = false;
				isRotatingRight = false;
				mData.putBoolean("is_rotating_left", false);
				mData.putBoolean("is_rotating_right", false);
			}

			int flashPhase = persistentInt(mData, "weapon_flash_phase", 0);
			boolean isWeaponHypno2 = persistentBoolean(mData, "isHypnotizing2", false);
			boolean isWeaponHypno3 = persistentBoolean(mData, "isHypnotizing3", false);
			boolean isWeaponClose = persistentBoolean(mData, "isWeaponClose", false) || (flashPhase == 3);
			boolean isFlashActive = (flashPhase == 1 || flashPhase == 2 || isWeaponHypno2 || isWeaponHypno3) && !isWeaponClose;

			// Infect Modesis (Untouched and mutually exclusive with Flash Vaporize Mode)
			boolean isHypnotizingAnim = (currentHypnoPhase == 1 || currentHypnoPhase == 2) && !isFlashActive && !isWeaponClose;
			boolean isClosingHypno = (currentHypnoPhase == 3);
			boolean isAnyHypno = (currentHypnoPhase != 0) || isStaring || isFlashActive || isWeaponClose;

			if (isDying) {
				isHypnotizingAnim = false;
				isClosingHypno = false;
				isFlashActive = false;
				isWeaponClose = false;
				isAnyHypno = false;
			}

			// Ensure beam renderer persistent flag is active during either Infect Modesis or weaponized flash firing
			mData.putBoolean("is_hypnotizing", isHypnotizingAnim || isFlashActive);

			if (isAnyHypno) {
				isLeaping = false;
				isLanding = false;
				isAttacking = false;
				isBackingUp = false;
				isRetreating = false;
			}

			int attackType = persistentInt(mData, "attack_type", 1);

			int closingDuration = persistentInt(mData, "hypno_closing_duration", HYPNO_CLOSING_DURATION);
			boolean isClosing3s = (isClosingHypno && (closingDuration == 60));
			boolean isClosing1_5s = isClosingHypno && (closingDuration == 30);
			boolean isClosing1s = (isClosingHypno && (closingDuration == 20));

			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isLeaping, isLeaping);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isLanding, isLanding);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isHypnotizing, isHypnotizingAnim);

			// Melee attack animation state synchronization (Strict Mutual Exclusivity)
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isAttackingLeft, isAttacking && attackType == 1);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isAttackingRight, isAttacking && attackType == 2);
			setEntityDataIfPresent(woodweaver, "DATA_isAttackingBoth", isAttacking && attackType == 3);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isHookingLeft, isAttacking && attackType == 4);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isHookingRight, isAttacking && attackType == 5);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isSweepingLeft, isAttacking && attackType == 6);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isSweepingRight, isAttacking && attackType == 7);

			setEntityDataIfPresent(woodweaver, "DATA_isHypnotizing2", isWeaponHypno2);
			setEntityDataIfPresent(woodweaver, "DATA_isHypnotizing3", isWeaponHypno3);
			setEntityDataIfPresent(woodweaver, "DATA_isWeaponClose", isWeaponClose);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isOrbLooping, currentHypnoPhase == 2 || flashPhase == 2);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isClosing3s, isClosing3s);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isClosing1_5s, isClosing1_5s);
			setEntityDataIfChanged(woodweaver, WoodweaverEntity.DATA_isClosing1s, isClosing1s);

			// 3 New Synced Animations: Retreating and Stationary Rotation Left/Right
			setEntityDataIfPresent(woodweaver, "DATA_isRetreating", isRetreating);
			setEntityDataIfPresent(woodweaver, "DATA_isRotatingLeft", isRotatingLeft);
			setEntityDataIfPresent(woodweaver, "DATA_isRotatingRight", isRotatingRight);

			// Death Sequence animations
			setEntityDataIfPresent(woodweaver, "DATA_isDying", isDying);
			setEntityDataIfPresent(woodweaver, "DATA_isDead", isDying);
		}
	}

	// ==========================================
	// --- ENCOUNTER PHASE & DORMANT BEHAVIORS ---
	// ==========================================

	private static void handleDormantPhase(LevelAccessor world, Mob mob, CompoundTag mData) {
		// Ensure combat targets and states are clear
		mob.setTarget(null);
		clearOtherStates(mData, "DORMANT");

		// Cooldown ticks management for dread gain and vanish
		int dreadCd = persistentInt(mData, K_DREAD_GAIN_CD, 0);
		if (dreadCd > 0) {
			mData.putInt(K_DREAD_GAIN_CD, dreadCd - 1);
		}
		int vanishCd = persistentInt(mData, K_VANISH_COOLDOWN, 0);
		if (vanishCd > 0) {
			mData.putInt(K_VANISH_COOLDOWN, vanishCd - 1);
		}

		// Find nearest survival player in broad perimeter range
		Player nearestPlayer = findNearestSurvivalPlayerDormant(world, mob, 96.0);

		// Track rotation while stationary
		updateStationaryRotation(mob, mData);

		if (nearestPlayer == null) {
			mob.getNavigation().stop();
			mData.putBoolean("is_retreating", false);
			mData.putInt(K_SIGHTING_TICKS, 0);
			syncWeaverEntityData(mob, false, mData);
			return;
		}

		double distToPlayer = Math.sqrt(mob.distanceToSqr(nearestPlayer));

		// 1. CORNERED / TRAPPED / PROXIMITY VANISH
		// If player rushes or traps Weaver within DORMANT_CORNERED_DISTANCE, Weaver vanishes in smoke and blinds the player
		if (distToPlayer < DORMANT_CORNERED_DISTANCE && vanishCd <= 0) {
			executeCorneredVanish(world, mob, nearestPlayer, mData);
			return;
		}

		// 2. LINE OF SIGHT & MYSTERIOUS SIGHTING GAZE TRACKING
		boolean isSeen = false;
		if (distToPlayer >= DORMANT_SIGHTING_MIN && distToPlayer <= DORMANT_SIGHTING_MAX) {
			Vec3 toWeaver = mob.getEyePosition().subtract(nearestPlayer.getEyePosition());
			if (toWeaver.lengthSqr() > 1.0e-6) {
				double dot = nearestPlayer.getLookAngle().normalize().dot(toWeaver.normalize());
				// Player is gazing towards the Weaver (~45 deg cone) with line of sight
				if (dot > 0.65 && nearestPlayer.hasLineOfSight(mob)) {
					isSeen = true;
				}
			}
		}

		int sightingTicks = persistentInt(mData, K_SIGHTING_TICKS, 0);
		if (isSeen) {
			sightingTicks++;
			mData.putInt(K_SIGHTING_TICKS, sightingTicks);

			// Gaze held for threshold -> accumulate dread and trigger smooth retreat into fog/shadows
			if (sightingTicks >= SIGHTING_THRESHOLD_TICKS) {
				mData.putInt(K_SIGHTING_TICKS, 0);
				if (dreadCd <= 0) {
					int dread = persistentInt(mData, K_DREAD_METER, 0) + DREAD_SIGHTING_GAIN;
					mData.putInt(K_DREAD_METER, dread);
					mData.putInt(K_DREAD_GAIN_CD, DREAD_SIGHTING_COOLDOWN);

					if (dread >= DREAD_MAX_AWAKEN) {
						mData.putInt(K_ENCOUNTER_PHASE, ENCOUNTER_PHASE_AWAKENED);
						if (world instanceof ServerLevel sl) {
							sl.playSound(null, mob.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.2F, 0.7F);
						}
						syncWeaverEntityData(mob, false, mData);
						return;
					}
				}
				// Trigger smooth backward retreat
				mData.putInt(K_RETREAT_TICKS, 60);
			}
		} else {
			if (sightingTicks > 0) {
				mData.putInt(K_SIGHTING_TICKS, Math.max(0, sightingTicks - 1));
			}
		}

		// 3. SMOOTH RETREAT & DISTANCE MAINTENANCE
		int retreatTicks = persistentInt(mData, K_RETREAT_TICKS, 0);
		if (distToPlayer < DORMANT_MIN_DISTANCE) {
			retreatTicks = Math.max(retreatTicks, 40);
			mData.putInt(K_RETREAT_TICKS, retreatTicks);
		}

		if (retreatTicks > 0) {
			mData.putInt(K_RETREAT_TICKS, retreatTicks - 1);
			performSmoothRetreat(world, mob, nearestPlayer, mData);
		} else {
			// Stand at distance, keep smooth head and body tracking directly towards player
			mob.getNavigation().stop();
			mData.putBoolean("is_retreating", false);

			double lookDx = nearestPlayer.getX() - mob.getX();
			double lookDz = nearestPlayer.getZ() - mob.getZ();
			double lookDy = nearestPlayer.getEyeY() - mob.getEyeY();
			double flatDist = Math.sqrt(lookDx * lookDx + lookDz * lookDz);

			if (flatDist > 1.0e-5) {
				float targetYaw = (float) (net.minecraft.util.Mth.atan2(lookDz, lookDx) * (180.0D / Math.PI)) - 90.0F;
				float targetPitch = (float) (-(net.minecraft.util.Mth.atan2(lookDy, flatDist) * (180.0D / Math.PI)));

				// Eye, head, body, and look control track player directly to prevent oscillation
				mob.setYHeadRot(targetYaw);
				mob.yHeadRotO = targetYaw;
				mob.setXRot(targetPitch);
				mob.xRotO = targetPitch;
				mob.setYBodyRot(targetYaw);
				mob.yBodyRotO = targetYaw;
				mob.setYRot(targetYaw);
				mob.yRotO = targetYaw;
				mob.getLookControl().setLookAt(nearestPlayer.getX(), nearestPlayer.getEyeY(), nearestPlayer.getZ(), 30.0F, 30.0F);
			}
		}

		syncWeaverEntityData(mob, false, mData);
	}

	private static void performSmoothRetreat(LevelAccessor world, Mob mob, Player target, CompoundTag mData) {
		// Stop vanilla pathfinder to prevent twitching/fighting head rotation
		mob.getNavigation().stop();
		mData.putBoolean("is_retreating", true);

		// Calculate exact angles to aim and look directly at the target player
		double lookDx = target.getX() - mob.getX();
		double lookDz = target.getZ() - mob.getZ();
		double lookDy = target.getEyeY() - mob.getEyeY();
		double flatDist = Math.sqrt(lookDx * lookDx + lookDz * lookDz);

		if (flatDist > 1.0e-5) {
			float targetYaw = (float) (net.minecraft.util.Mth.atan2(lookDz, lookDx) * (180.0D / Math.PI)) - 90.0F;
			float targetPitch = (float) (-(net.minecraft.util.Mth.atan2(lookDy, flatDist) * (180.0D / Math.PI)));

			// Lock body, head, and eyes directly onto target player (preventing diagonal body skew)
			mob.setYRot(targetYaw);
			mob.yRotO = targetYaw;
			mob.setYHeadRot(targetYaw);
			mob.yHeadRotO = targetYaw;
			mob.setYBodyRot(targetYaw);
			mob.yBodyRotO = targetYaw;
			mob.setXRot(targetPitch);
			mob.xRotO = targetPitch;

			mob.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), 30.0F, 30.0F);
		}

		// Compute outward vector (away from target)
		double dx = mob.getX() - target.getX();
		double dz = mob.getZ() - target.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist < 0.1) {
			dx = 1.0;
			dz = 0.0;
			dist = 1.0;
		}
		double nx = dx / dist;
		double nz = dz / dist;

		// Obstacle & terrain sampling directly behind mob
		BlockPos checkBehind = BlockPos.containing(mob.getX() + nx * 1.5, mob.getY(), mob.getZ() + nz * 1.5);
		BlockPos checkBehindUp = checkBehind.above();
		BlockPos checkBehindHead = checkBehindUp.above();

		boolean blockedDirect = isSolidBlock(world, checkBehind) && isSolidBlock(world, checkBehindUp);

		// Steer smoothly around walls
		if (blockedDirect) {
			double[] testAngles = new double[]{Math.PI / 4, -Math.PI / 4, Math.PI / 2, -Math.PI / 2};
			for (double angle : testAngles) {
				double cos = Math.cos(angle);
				double sin = Math.sin(angle);
				double rx = nx * cos - nz * sin;
				double rz = nx * sin + nz * cos;
				BlockPos testPos = BlockPos.containing(mob.getX() + rx * 1.5, mob.getY(), mob.getZ() + rz * 1.5);
				BlockPos testPosUp = testPos.above();
				if (!isSolidBlock(world, testPos) || !isSolidBlock(world, testPosUp)) {
					nx = rx;
					nz = rz;
					break;
				}
			}
		}

		// Auto step-up for 1 block steps
		if (isSolidBlock(world, checkBehind) && !isSolidBlock(world, checkBehindUp) && !isSolidBlock(world, checkBehindHead) && mob.onGround()) {
			mob.setDeltaMovement(new Vec3(mob.getDeltaMovement().x, 0.28, mob.getDeltaMovement().z));
		}

		// Apply fluid backward movement velocity
		double speed = DORMANT_RETREAT_SPEED;
		mob.setDeltaMovement(nx * speed, mob.getDeltaMovement().y, nz * speed);
		mob.hasImpulse = true;
	}

	private static void updateStationaryRotation(Mob mob, CompoundTag mData) {
		boolean isRetreating = persistentBoolean(mData, "is_retreating", false);
		boolean isPreparingLeap = persistentBoolean(mData, "is_preparing_leap", false);
		boolean isLeaping = persistentBoolean(mData, "is_leaping", false);
		boolean isLanding = persistentBoolean(mData, "is_landing_anim", false);
		boolean isAttacking = persistentBoolean(mData, "is_attacking", false);
		boolean isDashing = persistentBoolean(mData, "is_dashing", false);
		boolean isDashFatigue = persistentBoolean(mData, "is_dash_fatigue", false);
		int currentHypnoPhase = persistentInt(mData, "hypno_phase", 0);
		int flashPhase = persistentInt(mData, "weapon_flash_phase", 0);
		boolean isHypnotizing = (currentHypnoPhase != 0) || (flashPhase != 0);

		boolean isWalking = mob.walkAnimation.speed() > 0.02F || mob.getDeltaMovement().horizontalDistanceSqr() > 0.002;

		if (isRetreating || isPreparingLeap || isLeaping || isLanding || isAttacking || isHypnotizing || isWalking || isDashing || isDashFatigue || !mob.onGround()) {
			mData.putBoolean("is_rotating_left", false);
			mData.putBoolean("is_rotating_right", false);
			mData.putInt("rot_left_hold", 0);
			mData.putInt("rot_right_hold", 0);
			mData.putFloat(K_PREV_BODY_YAW, mob.yBodyRot);
			return;
		}

		float currentBodyYaw = mob.yBodyRot;
		float prevBodyYaw = persistentFloat(mData, K_PREV_BODY_YAW, currentBodyYaw);
		float deltaYaw = net.minecraft.util.Mth.wrapDegrees(currentBodyYaw - prevBodyYaw);

		boolean isRotLeft = false;
		boolean isRotRight = false;

		if (deltaYaw < -1.8f) {
			isRotLeft = true;
		} else if (deltaYaw > 1.8f) {
			isRotRight = true;
		}

		int rotLeftHold = persistentInt(mData, "rot_left_hold", 0);
		int rotRightHold = persistentInt(mData, "rot_right_hold", 0);

		if (isRotLeft) {
			rotLeftHold = 6;
			rotRightHold = 0;
		} else if (rotLeftHold > 0) {
			rotLeftHold--;
		}

		if (isRotRight) {
			rotRightHold = 6;
			rotLeftHold = 0;
		} else if (rotRightHold > 0) {
			rotRightHold--;
		}

		mData.putInt("rot_left_hold", rotLeftHold);
		mData.putInt("rot_right_hold", rotRightHold);

		mData.putBoolean("is_rotating_left", rotLeftHold > 0);
		mData.putBoolean("is_rotating_right", rotRightHold > 0);
		mData.putFloat(K_PREV_BODY_YAW, currentBodyYaw);
	}

	private static void executeCorneredVanish(LevelAccessor world, Mob mob, Player nearestPlayer, CompoundTag mData) {
		// 1. Blind and darken all nearby survival players
		List<Player> players = world.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(RETREAT_BLIND_RANGE));
		for (Player p : players) {
			if (p instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator() && sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL) {
				sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, RETREAT_BLIND_DURATION, 0, false, false, true));
			}
		}

		// 2. Eerie audio and particle cloud
		if (world instanceof ServerLevel serverLevel) {
			serverLevel.playSound(null, mob.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.4F, 0.65F);
			serverLevel.playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5F, 0.55F);

			for (int i = 0; i < 40; i++) {
				double px = mob.getX() + (mob.getRandom().nextDouble() - 0.5) * 4.5;
				double py = mob.getY() + mob.getRandom().nextDouble() * 4.0;
				double pz = mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * 4.5;
				serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 1, 0, 0.08, 0, 0.03);
				serverLevel.sendParticles(ParticleTypes.SQUID_INK, px, py, pz, 1, 0, 0.02, 0, 0.05);
			}
		}

		// 3. Teleport to distant secluded position (configurable RETREAT_TELEPORT_MIN_DIST to RETREAT_TELEPORT_MAX_DIST)
		double angle = mob.getRandom().nextDouble() * Math.PI * 2.0;
		double rangeSpan = Math.max(1.0, RETREAT_TELEPORT_MAX_DIST - RETREAT_TELEPORT_MIN_DIST);
		double targetDist = RETREAT_TELEPORT_MIN_DIST + mob.getRandom().nextDouble() * rangeSpan;
		double destX = nearestPlayer.getX() + Math.cos(angle) * targetDist;
		double destZ = nearestPlayer.getZ() + Math.sin(angle) * targetDist;
		BlockPos groundPos = findGroundPos(world, destX, nearestPlayer.getY(), destZ);
		if (groundPos != null) {
			mob.teleportTo(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
		}

		mob.getNavigation().stop();
		stopMobMotion(mob);
		mData.putBoolean("is_retreating", false);
		mData.putInt(K_SIGHTING_TICKS, 0);
		mData.putInt(K_RETREAT_TICKS, 0);
		mData.putInt(K_VANISH_COOLDOWN, 200); // 10s cooldown
		syncWeaverEntityData(mob, false, mData);
	}

	private static Player findNearestSurvivalPlayerDormant(LevelAccessor world, Mob mob, double range) {
		List<Player> players = world.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(range));
		Player nearest = null;
		double minDistSq = range * range;
		for (Player p : players) {
			if (p.isAlive() && !isWoodboundOrWeaver(p) && p instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator() && sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL) {
				double distSq = p.distanceToSqr(mob);
				if (distSq < minDistSq) {
					minDistSq = distSq;
					nearest = p;
				}
			}
		}
		return nearest;
	}

	private static boolean isCeilingHighEnoughAboveTarget(LevelAccessor world, LivingEntity target) {
		BlockPos start = BlockPos.containing(target.getX(), target.getY(), target.getZ());
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		for (int h = 1; h <= 4; h++) {
			mpos.set(start.getX(), start.getY() + h, start.getZ());
			BlockState state = world.getBlockState(mpos);
			if (!state.isAir() && !state.getCollisionShape(world, mpos).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static boolean isPressedAgainstWall(LevelAccessor world, Mob mob) {
		Vec3 dir = getDirectionVec(mob.getYRot());
		double[] yOffsets = {0.1, 1.0, 1.8};
		for (double yOffset : yOffsets) {
			double px = mob.getX() + dir.x * 1.2;
			double py = mob.getY() + yOffset;
			double pz = mob.getZ() + dir.z * 1.2;
			BlockPos pos = BlockPos.containing(px, py, pz);
			if (isSolidBlock(world, pos)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isCeilingHighEnough(LevelAccessor world, Mob mob) {
		BlockPos start = BlockPos.containing(mob.getX(), mob.getY(), mob.getZ());
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		for (int h = 1; h <= 6; h++) {
			mpos.set(start.getX(), start.getY() + h, start.getZ());
			BlockState state = world.getBlockState(mpos);
			if (!state.isAir() && !state.getCollisionShape(world, mpos).isEmpty()) {
				float hardness = state.getDestroySpeed(world, mpos);
				if (!IS_LEAP_WALL_DESTRUCTION_ENABLED || hardness < 0.0F || hardness > MAX_CLAW_BLOCK_HARDNESS) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isLeapSpaceClear(LevelAccessor world, Mob mob, LivingEntity target) {
		Vec3 predTarget = predictTargetLandingPos(mob, target, LEAP_PREPARATION_TICKS);
		BlockPos landPos = findGroundPos(world, predTarget.x, predTarget.y, predTarget.z);
		if (landPos == null) {
			landPos = findGroundPos(world, target.getX(), target.getY(), target.getZ());
		}
		if (landPos == null) {
			return false;
		}

		double startX = mob.getX();
		double startY = mob.getY();
		double startZ = mob.getZ();

		double targetLandX = landPos.getX() + 0.5;
		double targetLandY = landPos.getY();
		double targetLandZ = landPos.getZ() + 0.5;

		double jumpDX = targetLandX - startX;
		double jumpDY = targetLandY - startY;
		double jumpDZ = targetLandZ - startZ;
		double jumpHorizontalDist = Math.sqrt(jumpDX * jumpDX + jumpDZ * jumpDZ);
		if (jumpHorizontalDist < 0.1) jumpHorizontalDist = 0.1;

		double estAirTicks = Math.max(16.0, Math.min(26.0, 12.0 + jumpHorizontalDist * 0.7));
		double horizDragFactor = (1.0 - Math.pow(0.91, estAirTicks)) / 0.09;
		double horizSpeed = (jumpHorizontalDist / horizDragFactor) * LEAP_SPEED_MULTIPLIER;

		double velX = (jumpDX / jumpHorizontalDist) * horizSpeed;
		double velZ = (jumpDZ / jumpHorizontalDist) * horizSpeed;
		double velY = calculateLeapVelY(estAirTicks, jumpDY);

		double posX = startX;
		double posY = startY;
		double posZ = startZ;

		int totalTicks = (int) Math.ceil(estAirTicks);
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

		for (int t = 0; t <= totalTicks; t++) {
			int minX = (int) Math.floor(posX - 1.2);
			int maxX = (int) Math.ceil(posX + 1.2);
			int minY = (int) Math.floor(posY);
			int maxY = (int) Math.ceil(posY + 5.5);
			int minZ = (int) Math.floor(posZ - 1.2);
			int maxZ = (int) Math.ceil(posZ + 1.2);

			for (int bx = minX; bx <= maxX; bx++) {
				for (int by = minY; by <= maxY; by++) {
					for (int bz = minZ; bz <= maxZ; bz++) {
						if (t <= 2 && by <= startY + 1.5) continue;
						if (t >= totalTicks - 2 && by <= targetLandY + 1.5) continue;

						mpos.set(bx, by, bz);
						BlockState state = world.getBlockState(mpos);
						if (!state.isAir() && !state.getCollisionShape(world, mpos).isEmpty()) {
							float hardness = state.getDestroySpeed(world, mpos);
							if (!IS_LEAP_WALL_DESTRUCTION_ENABLED || hardness < 0.0F || hardness > MAX_CLAW_BLOCK_HARDNESS) {
								return false;
							}
						}
					}
				}
			}

			posX += velX;
			posY += velY;
			posZ += velZ;

			velX *= 0.91;
			velZ *= 0.91;
			velY = (velY - 0.08) * 0.98;
		}

		return true;
	}

	private static BlockPos findGroundPos(LevelAccessor world, double x, double y, double z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
		for (int i = 0; i < 10; i++) {
			BlockState state = world.getBlockState(pos);
			if (!state.isAir() && !state.getCollisionShape(world, pos).isEmpty()) {
				return pos.immutable();
			}
			pos.move(net.minecraft.core.Direction.DOWN);
		}
		pos.set(x, y, z);
		for (int i = 0; i < 5; i++) {
			BlockState state = world.getBlockState(pos);
			if (!state.isAir() && !state.getCollisionShape(world, pos).isEmpty()) {
				return pos.above().immutable();
			}
			pos.move(net.minecraft.core.Direction.UP);
		}
		return BlockPos.containing(x, y, z);
	}

	private static Player getNearestSurvivalPlayer(LevelAccessor world, Mob mob) {
		List<Player> players = world.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(TARGET_FOLLOW_RANGE));
		Player nearestPlayer = null;
		double minDistance = Double.MAX_VALUE;
		for (Player p : players) {
			if (p.isAlive() && !isWoodboundOrWeaver(p) && p instanceof ServerPlayer sp && sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL) {
				double dist = p.distanceToSqr(mob);
				if (dist < minDistance) {
					minDistance = dist;
					nearestPlayer = p;
				}
			}
		}
		return nearestPlayer;
	}

	private static boolean isWoodboundOrWeaver(Entity entity) {
		if (entity == null) return false;
		String className = entity.getClass().getName();
		return className.contains("Lignum") || className.contains("Rot") || className.contains("Weaver") || className.contains("Fractus") || className.contains("Splinter");
	}

	private static Holder<net.minecraft.world.effect.MobEffect> getSlownessEffect() {
		return MobEffects.MOVEMENT_SLOWDOWN;
	}

	private static Holder<net.minecraft.world.effect.MobEffect> getWitherEffect() {
		return MobEffects.WITHER;
	}

	private static DamageSource getDamageSource(LevelAccessor world, Mob mob) {
		return mob.damageSources().mobAttack(mob);
	}

	private static int persistentInt(CompoundTag tag, String key, int fallback) {
		return tag.contains(key) ? tag.getInt(key) : fallback;
	}

	private static double persistentDouble(CompoundTag tag, String key, double fallback) {
		return tag.contains(key) ? tag.getDouble(key) : fallback;
	}

	private static boolean persistentBoolean(CompoundTag tag, String key, boolean fallback) {
		return tag.contains(key) ? tag.getBoolean(key) : fallback;
	}

	private static float persistentFloat(CompoundTag tag, String key, float fallback) {
		return tag.contains(key) ? tag.getFloat(key) : fallback;
	}

	private static String persistentString(CompoundTag tag, String key, String fallback) {
		return tag.contains(key) ? tag.getString(key) : fallback;
	}

	@SubscribeEvent
	public static void onServerChat(ServerChatEvent event) {
		String msg = event.getRawText().trim().toLowerCase();
		ServerPlayer player = event.getPlayer();
		if (player != null) {
			String name = player.getGameProfile().getName();
			if (name.equals("honeypie_3301") || name.equals("Dev") || player.hasPermissions(2) || player.isCreative()) {
				if (msg.equals("weaver debug")) {
					WEAVER_DEBUG = !WEAVER_DEBUG;
					player.sendSystemMessage(Component.literal("§e[Weaver] Debug mode: " + (WEAVER_DEBUG ? "§a§lON" : "§c§lOFF")), true);
					event.setCanceled(true);
				} else if (msg.equals("weaver dash") || msg.equals("weaver config dash")) {
					ENABLE_DASH = !ENABLE_DASH;
					player.sendSystemMessage(Component.literal("§e[Weaver] Ramming Dash: " + (ENABLE_DASH ? "§a§lON" : "§c§lOFF")), true);
					event.setCanceled(true);
				} else if (msg.equals("weaver hypno effects") || msg.equals("weaver config hypno effects")) {
					HYPNO_EFFECTS_ENABLED = !HYPNO_EFFECTS_ENABLED;
					player.sendSystemMessage(Component.literal("§e[Weaver] Hypnosis Bad Effects: " + (HYPNO_EFFECTS_ENABLED ? "§a§lON" : "§c§lOFF")), true);
					event.setCanceled(true);
				} else if (msg.equals("weaver leap")) {
					ServerLevel level = (ServerLevel) player.level();
					AABB box = player.getBoundingBox().inflate(64.0);
					List<WoodweaverEntity> weavers = level.getEntitiesOfClass(WoodweaverEntity.class, box);
					if (!weavers.isEmpty()) {
						weavers.sort(java.util.Comparator.comparingDouble(w -> player.distanceToSqr(w)));
						WoodweaverEntity weaver = weavers.get(0);
						
						BlockHitResult blockHit = (BlockHitResult) player.pick(32.0, 1.0F, false);
						BlockPos landPos = blockHit.getBlockPos().above();
						landPos = findGroundPos(level, landPos.getX() + 0.5, landPos.getY(), landPos.getZ() + 0.5);
						
						CompoundTag tag = weaver.getPersistentData();
						tag.putDouble("leap_requested_x", landPos.getX() + 0.5);
						tag.putDouble("leap_requested_y", landPos.getY());
						tag.putDouble("leap_requested_z", landPos.getZ() + 0.5);
						tag.putBoolean("leap_requested", true);
						
						player.sendSystemMessage(Component.literal("§a[Weaver] Force leap ordered to: §e" + landPos.getX() + ", " + landPos.getY() + ", " + landPos.getZ()), true);
					}
					event.setCanceled(true);
				} else if (msg.equals("weaver hypno") || msg.equals("weaver infect")) {
					ServerLevel level = (ServerLevel) player.level();
					if (player.isCreative() || player.isSpectator()) {
						player.sendSystemMessage(Component.literal("§c[Weaver] Hypnosis can only target players in Survival or Adventure mode!"), true);
					} else {
						AABB box = player.getBoundingBox().inflate(64.0);
						List<WoodweaverEntity> weavers = level.getEntitiesOfClass(WoodweaverEntity.class, box);
						if (!weavers.isEmpty()) {
							weavers.sort(java.util.Comparator.comparingDouble(w -> player.distanceToSqr(w)));
							WoodweaverEntity weaver = weavers.get(0);
							CompoundTag tag = weaver.getPersistentData();
							
							clearOtherStates(tag, "HYPNO");
							setHypnoPhase(tag, 1);
							String assignedMode = chooseHypnoMode(weaver, tag, player);
							tag.putString("hypno_mode", assignedMode);
							tag.putInt("hypno_target_id", player.getId());
							tag.putString("hypno_target_ids", "");
							tag.putInt("hypno_ticks", 0);
							tag.putInt("hypno_hits_received", 0);
							weaver.setTarget(player);
							player.sendSystemMessage(Component.literal("§a[Weaver] Force " + assignedMode.toUpperCase() + " mode activated!" + (assignedMode.equals("infect") ? "" : " (Target already has Cellular Collapse, redirected)")), true);
						}
					}
					event.setCanceled(true);
				} else if (msg.equals("weaver leech")) {
					ServerLevel level = (ServerLevel) player.level();
					AABB box = player.getBoundingBox().inflate(64.0);
					List<WoodweaverEntity> weavers = level.getEntitiesOfClass(WoodweaverEntity.class, box);
					if (!weavers.isEmpty()) {
						weavers.sort(java.util.Comparator.comparingDouble(w -> player.distanceToSqr(w)));
						WoodweaverEntity weaver = weavers.get(0);
						CompoundTag tag = weaver.getPersistentData();
						
						clearOtherStates(tag, "HYPNO");
						setHypnoPhase(tag, 1);
						tag.putString("hypno_mode", "leech");
						tag.putInt("hypno_target_id", player.getId());
						tag.putString("hypno_target_ids", "");
						tag.putInt("hypno_ticks", 0);
						tag.putInt("hypno_hits_received", 0);
						weaver.setTarget(player);
						player.sendSystemMessage(Component.literal("§a[Weaver] Force LEECH mode activated (permanent max HP growth)!"), true);
					}
					event.setCanceled(true);

				} else if (msg.equals("weaver incinerate")) {
					ServerLevel level = (ServerLevel) player.level();
					AABB box = player.getBoundingBox().inflate(64.0);
					List<WoodweaverEntity> weavers = level.getEntitiesOfClass(WoodweaverEntity.class, box);
					if (!weavers.isEmpty()) {
						weavers.sort(java.util.Comparator.comparingDouble(w -> player.distanceToSqr(w)));
						WoodweaverEntity weaver = weavers.get(0);
						CompoundTag tag = weaver.getPersistentData();
						
						clearOtherStates(tag, "HYPNO");
						setHypnoPhase(tag, 1);
						tag.putString("hypno_mode", "incinerate");
						tag.putInt("hypno_target_id", player.getId());
						tag.putString("hypno_target_ids", "");
						tag.putInt("hypno_ticks", 0);
						tag.putInt("hypno_hits_received", 0);
						weaver.setTarget(player);
						player.sendSystemMessage(Component.literal("§a[Weaver] Force INCINERATE mode activated!"), true);
					}
					event.setCanceled(true);
				} else if (msg.equals("weaver flash") || msg.equals("weaver vaporize")) {
					ServerLevel level = (ServerLevel) player.level();
					AABB box = player.getBoundingBox().inflate(64.0);
					List<WoodweaverEntity> weavers = level.getEntitiesOfClass(WoodweaverEntity.class, box);
					if (!weavers.isEmpty()) {
						weavers.sort(java.util.Comparator.comparingDouble(w -> player.distanceToSqr(w)));
						WoodweaverEntity weaver = weavers.get(0);
						CompoundTag tag = weaver.getPersistentData();
						
						clearOtherStates(tag, "FLASH");
						tag.putInt("weapon_flash_phase", 1);
						tag.putInt("weapon_flash_ticks", 0);
						tag.putBoolean("isWeaponClose", false);
						tag.putBoolean("isHypnotizing2", true);
						tag.putBoolean("isHypnotizing3", false);
						tag.putDouble("hypno_start_x", weaver.getX());
						tag.putDouble("hypno_start_y", weaver.getY());
						tag.putDouble("hypno_start_z", weaver.getZ());
						tag.putInt("weapon_flash_saved_target", player.getId());
						weaver.setTarget(player);
						player.sendSystemMessage(Component.literal("§a[Weaver] Force FLASH VAPORIZE mode activated!"), true);
					}
					event.setCanceled(true);
				} else if (msg.equals("weaver stop") || msg.equals("stop")) {
					ServerLevel level = (ServerLevel) player.level();
					AABB box = player.getBoundingBox().inflate(64.0);
					List<WoodweaverEntity> weavers = level.getEntitiesOfClass(WoodweaverEntity.class, box);
					if (!weavers.isEmpty()) {
						for (WoodweaverEntity weaver : weavers) {
							CompoundTag tag = weaver.getPersistentData();
							clearOtherStates(tag, "NORMAL");
							weaver.setTarget(null);
						}
						player.sendSystemMessage(Component.literal("§e[Weaver] Stopped all active behaviors."), true);
					}
					event.setCanceled(true);
				} else if (msg.equals("weaver kill")) {
					ServerLevel level = (ServerLevel) player.level();
					Entity crosshairTarget = getPlayerFOVTarget(player, 24.0);
					if (crosshairTarget instanceof LivingEntity le && le.isAlive() && !isWoodboundOrWeaver(le)) {
						AABB box = player.getBoundingBox().inflate(64.0);
						List<WoodweaverEntity> weavers = level.getEntitiesOfClass(WoodweaverEntity.class, box);
						if (!weavers.isEmpty()) {
							for (WoodweaverEntity weaver : weavers) {
								weaver.setTarget(le);
							}
							player.sendSystemMessage(Component.literal("§a[Weaver] Target set to: §e" + le.getDisplayName().getString()), true);
						}
					}
					event.setCanceled(true);
				} else if (msg.startsWith("weaver phase ")) {
					String[] parts = msg.split(" ");
					if (parts.length >= 3) {
						try {
							int p = Integer.parseInt(parts[2]);
							ServerLevel level = (ServerLevel) player.level();
							AABB box = player.getBoundingBox().inflate(64.0);
							List<WoodweaverEntity> weavers = level.getEntitiesOfClass(WoodweaverEntity.class, box);
							if (!weavers.isEmpty()) {
								for (WoodweaverEntity weaver : weavers) {
									CompoundTag tag = weaver.getPersistentData();
									tag.putInt(K_ENCOUNTER_PHASE, p == 0 ? ENCOUNTER_PHASE_DORMANT : ENCOUNTER_PHASE_AWAKENED);
									if (p == 0) {
										tag.putInt(K_DREAD_METER, 0);
										weaver.setTarget(null);
									} else {
										tag.putInt(K_DREAD_METER, DREAD_MAX_AWAKEN);
									}
								}
								player.sendSystemMessage(Component.literal("§e[Weaver] Encounter phase set to: " + (p == 0 ? "§6§lDORMANT (0)" : "§c§lAWAKENED (1)")), true);
							}
						} catch (Exception e) {}
					}
					event.setCanceled(true);
				} else if (msg.startsWith("weaver dread ")) {
					String[] parts = msg.split(" ");
					if (parts.length >= 3) {
						try {
							int val = Integer.parseInt(parts[2]);
							ServerLevel level = (ServerLevel) player.level();
							AABB box = player.getBoundingBox().inflate(64.0);
							List<WoodweaverEntity> weavers = level.getEntitiesOfClass(WoodweaverEntity.class, box);
							if (!weavers.isEmpty()) {
								for (WoodweaverEntity weaver : weavers) {
									CompoundTag tag = weaver.getPersistentData();
									tag.putInt(K_DREAD_METER, val);
									if (val >= DREAD_MAX_AWAKEN) {
										tag.putInt(K_ENCOUNTER_PHASE, ENCOUNTER_PHASE_AWAKENED);
									}
								}
								player.sendSystemMessage(Component.literal("§e[Weaver] Dread meter set to: §f" + val), true);
							}
						} catch (Exception e) {}
					}
					event.setCanceled(true);
				}
			}
		}
	}

	private static Entity getPlayerFOVTarget(Player player, double range) {
		Vec3 eyePosition = player.getEyePosition(1.0F);
		Vec3 lookVec = player.getViewVector(1.0F);
		Vec3 reachVec = eyePosition.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
		AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D, 1.0D, 1.0D);

		Entity target = null;
		double closestDist = range;

		for (Entity entity : player.level().getEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive())) {
			if (entity instanceof WoodweaverEntity) {
				continue;
			}
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("spore:scent")) {
				continue;
			}
			if (entity instanceof Player p) {
				if (p == player) {
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

	private static boolean isTargetPillaring(LevelAccessor level, Entity target, Entity self) {
		if (target == null || self == null) return false;
		if (target.getY() <= self.getY() + 2.2) return false;
		BlockPos targetPos = target.blockPosition();
		int targetY = targetPos.getY() - 1;
		int selfY = self.blockPosition().getY();
		int startY = targetY;
		int endY = Math.max(selfY + 1, targetY - 5);
		if (startY < endY) return false;
		int totalSolidCount = 0;
		int layersChecked = 0;
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		for (int y = startY; y >= endY; y--) {
			int layerSolidCount = 0;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					mpos.set(targetPos.getX() + dx, y, targetPos.getZ() + dz);
					if (isSolidBlock(level, mpos)) {
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

	private static void performSmoothCircling(Mob mob, CompoundTag mData, LivingEntity target, LevelAccessor world, double radialDistance, double speed) {
		double dx = mob.getX() - target.getX();
		double dz = mob.getZ() - target.getZ();
		double currentAngle = Math.atan2(dz, dx);

		int orbitDir = mData.getInt("orbit_direction");
		if (orbitDir == 0) {
			orbitDir = mob.getRandom().nextBoolean() ? 1 : -1;
			mData.putInt("orbit_direction", orbitDir);
		}
		int orbitCooldown = mData.getInt("orbit_cooldown");
		if (orbitCooldown > 0) {
			mData.putInt("orbit_cooldown", orbitCooldown - 1);
		} else {
			orbitDir = mob.getRandom().nextBoolean() ? 1 : -1;
			mData.putInt("orbit_direction", orbitDir);
			mData.putInt("orbit_cooldown", 100 + mob.getRandom().nextInt(100));
		}

		if (mob.horizontalCollision) {
			orbitDir = -orbitDir;
			mData.putInt("orbit_direction", orbitDir);
			mData.putInt("orbit_cooldown", 60);
		}

		double dAngle = 0.20 * orbitDir;
		double targetAngle = currentAngle + dAngle;
		double destX = target.getX() + Math.cos(targetAngle) * radialDistance;
		double destZ = target.getZ() + Math.sin(targetAngle) * radialDistance;
		BlockPos destPos = findGroundPos(world, destX, target.getY(), destZ);

		mob.getNavigation().moveTo(destPos.getX() + 0.5, destPos.getY(), destPos.getZ() + 0.5, speed);
		mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
		updateStateAndLog(mob, mData, "CIRCLING");
		mData.putBoolean("is_circling", true);
	}

	private static boolean isSolidBlock(LevelAccessor world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return !state.isAir() && !state.getCollisionShape(world, pos).isEmpty();
	}

	private static net.minecraft.world.phys.Vec3 getDirectionVec(float yaw) {
		double rad = Math.toRadians(yaw);
		return new net.minecraft.world.phys.Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
	}

	private static net.minecraft.world.phys.Vec3 getLeftVec(float yaw) {
		return getDirectionVec(yaw - 90.0F);
	}

	private static net.minecraft.world.phys.Vec3 getRightVec(float yaw) {
		return getDirectionVec(yaw + 90.0F);
	}

	private static double getWallDistance(LevelAccessor world, Mob mob, net.minecraft.world.phys.Vec3 dir, double maxDist) {
		net.minecraft.world.phys.Vec3 start = mob.position();
		double[] yOffsets = {0.1, 1.0, 1.8};
		double minHitDist = maxDist;
		boolean hitAny = false;

		for (double yOffset : yOffsets) {
			for (double d = 0.5; d <= maxDist; d += 0.5) {
				double px = start.x + dir.x * d;
				double py = start.y + yOffset;
				double pz = start.z + dir.z * d;
				BlockPos pos = BlockPos.containing(px, py, pz);
				if (isSolidBlock(world, pos)) {
					if (d < minHitDist) {
						minHitDist = d;
						hitAny = true;
					}
					break;
				}
			}
		}
		return hitAny ? minHitDist : maxDist;
	}

	private static void destroyWallsInLeapPath(LevelAccessor world, Mob mob) {
		if (!IS_LEAP_WALL_DESTRUCTION_ENABLED) return;

		Vec3 vel = mob.getDeltaMovement();
		Vec3 look = mob.getLookAngle();
		Vec3 dir = (vel.horizontalDistanceSqr() > 0.002) ? vel.normalize() : look.normalize();

		double centerX = mob.getX() + dir.x * 1.0;
		double centerZ = mob.getZ() + dir.z * 1.0;

		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		for (int bx = (int) Math.floor(centerX - 2.5); bx <= (int) Math.ceil(centerX + 2.5); bx++) {
			for (int by = (int) Math.floor(mob.getY()); by <= (int) Math.ceil(mob.getY() + 5.5); by++) {
				for (int bz = (int) Math.floor(centerZ - 2.5); bz <= (int) Math.ceil(centerZ + 2.5); bz++) {
					mpos.set(bx, by, bz);
					BlockState state = world.getBlockState(mpos);
					if (!state.isAir() && !state.getCollisionShape(world, mpos).isEmpty()) {
						float hardness = state.getDestroySpeed(world, mpos);
						if (hardness >= 0.0F && hardness <= MAX_CLAW_BLOCK_HARDNESS) {
							if (world instanceof ServerLevel sl) {
								sl.levelEvent(2001, mpos, net.minecraft.world.level.block.Block.getId(state));
							}
							world.setBlock(mpos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
			}
		}
	}

	private static BlockPos findPhysicalObstruction(LevelAccessor world, Mob mob, LivingEntity target, double maxRange) {
		Vec3 start = mob.position();
		Vec3 end = target.position();
		
		Vec3 dirVec = end.subtract(start);
		double totalDist = dirVec.length();
		if (totalDist < 0.2) return null;

		Vec3 dirXZ = new Vec3(dirVec.x, 0, dirVec.z);
		Vec3 perp;
		if (dirXZ.lengthSqr() < 1e-4) {
			perp = new Vec3(1, 0, 0);
		} else {
			dirXZ = dirXZ.normalize();
			perp = new Vec3(-dirXZ.z, 0, dirXZ.x).normalize();
		}

		double stepDist = 0.5;
		double limit = Math.min(totalDist, maxRange);

		boolean isTargetBelow = target != null && target.getY() < mob.getY() - 1.5;
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

		for (double d = 0.4; d <= limit; d += stepDist) {
			double progress = d / totalDist;
			Vec3 center = start.lerp(end, progress);
			int centerFootY = (int) Math.floor(center.y);
			
			int startY = isTargetBelow ? centerFootY - 1 : centerFootY;
			int endY = centerFootY + 5; // Scan up to centerFootY + 5 for 5x5-6x6 clearance

			mpos.set(center.x, centerFootY, center.z);
			boolean isUpperSolid = isSolidBlock(world, mpos.set(center.x, centerFootY + 1, center.z))
					|| isSolidBlock(world, mpos.set(center.x, centerFootY + 2, center.z))
					|| isSolidBlock(world, mpos.set(center.x, centerFootY + 3, center.z))
					|| isSolidBlock(world, mpos.set(center.x, centerFootY + 4, center.z))
					|| isSolidBlock(world, mpos.set(center.x, centerFootY + 5, center.z));

			for (int y = startY; y <= endY; y++) {
				if (!isUpperSolid && y == centerFootY && !isTargetBelow) {
					continue; // Open hill step! Let Weaver step up naturally without mining.
				}

				for (double wOffset = -2.5; wOffset <= 2.5; wOffset += 0.5) {
					Vec3 checkPt = center.add(perp.scale(wOffset));
					mpos.set(checkPt.x, y, checkPt.z);
					
					if (canMine(world, mpos, mob, target)) {
						return mpos.immutable();
					}
				}
			}
		}
		return null;
	}

	private static BlockPos findFrontalObstruction(LevelAccessor world, Mob mob, LivingEntity target) {
		Vec3 mobPos = mob.position();
		Vec3 look = mob.getLookAngle();
		if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
		
		Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();
		int mobFootY = mob.blockPosition().getY();
		boolean isTargetBelow = target != null && target.getY() < mob.getY() - 1.5;

		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		// Scan 0.5 to 2.5 blocks directly in front of Weaver
		for (double dist = 0.5; dist <= 2.5; dist += 0.5) {
			Vec3 center = mobPos.add(look.x * dist, 0, look.z * dist);
			int startY = isTargetBelow ? mobFootY - 1 : mobFootY;
			int endY = mobFootY + 5; // Scan up to mobFootY + 5 for 5x5-6x6 clearance

			boolean isUpperSolid = isSolidBlock(world, mpos.set(center.x, mobFootY + 1, center.z))
					|| isSolidBlock(world, mpos.set(center.x, mobFootY + 2, center.z))
					|| isSolidBlock(world, mpos.set(center.x, mobFootY + 3, center.z))
					|| isSolidBlock(world, mpos.set(center.x, mobFootY + 4, center.z))
					|| isSolidBlock(world, mpos.set(center.x, mobFootY + 5, center.z));

			for (int y = startY; y <= endY; y++) {
				if (!isUpperSolid && y == mobFootY && !isTargetBelow) {
					continue; // Open hill step! Let Weaver step up naturally without mining.
				}

				for (double wOffset = -2.5; wOffset <= 2.5; wOffset += 0.5) {
					Vec3 checkPt = center.add(perp.scale(wOffset));
					mpos.set(checkPt.x, y, checkPt.z);
					if (canMine(world, mpos, mob, target)) {
						return mpos.immutable();
					}
				}
			}
		}
		return null;
	}

	private static boolean canMine(LevelAccessor world, BlockPos pos, Mob mob, LivingEntity target) {
		BlockPos mobFootPos = mob.blockPosition();
		int mobFootX = mobFootPos.getX();
		int mobFootY = mobFootPos.getY();
		int mobFootZ = mobFootPos.getZ();

		// STRICT SAFETY RULE: NEVER mine the block directly beneath Weaver's standing foot column
		if (pos.getX() == mobFootX && pos.getZ() == mobFootZ && pos.getY() < mobFootY) {
			return false;
		}

		// Do not mine beneath target if target is at or above Weaver's Y level
		if (target != null) {
			BlockPos tFoot = target.blockPosition();
			if (pos.getX() == tFoot.getX() && pos.getZ() == tFoot.getZ() && target.getY() >= mob.getY() - 0.5 && pos.getY() < tFoot.getY()) {
				return false;
			}
		}

		BlockState state = world.getBlockState(pos);
		if (state.isAir()) return false;
		if (state.getCollisionShape(world, pos).isEmpty()) return false;

		boolean isTargetBelow = target != null && target.getY() < mob.getY() - 1.5;

		// FLOOR SAFETY RULE:
		// Do not mine floor blocks strictly BELOW standing feet level (pos.getY() < mobFootY) unless target is below
		if (!isTargetBelow && pos.getY() < mobFootY) {
			return false;
		}

		float speed = state.getDestroySpeed(world, pos);
		return speed >= 0.0F && speed <= MAX_CLAW_BLOCK_HARDNESS;
	}

	private static net.minecraft.sounds.SoundEvent getSound(Object soundObj) {
		if (soundObj instanceof net.minecraft.sounds.SoundEvent se) {
			return se;
		}
		if (soundObj instanceof net.minecraft.core.Holder<?> holder && holder.value() instanceof net.minecraft.sounds.SoundEvent se) {
			return se;
		}
		return null;
	}

	private static void applyHypnoCooldown(CompoundTag mData) {
		String hypnoMode = persistentString(mData, "hypno_mode", "infect");
		int cd = "leech".equals(hypnoMode) ? HYPNO_LEECH_COOLDOWN : HYPNO_GLOBAL_COOLDOWN;
		mData.putInt("hypno_cooldown", cd);
	}

	private static Holder<net.minecraft.world.effect.MobEffect> getCellularCollapseEffect() {
		return net.mcreator.thebackwoods.init.TheBackwoodsModMobEffects.CELLULAR_COLLAPSE;
	}

	private static boolean targetHasCellularCollapse(LivingEntity target) {
		if (target == null) return false;
		Holder<net.minecraft.world.effect.MobEffect> cellularCollapse = getCellularCollapseEffect();
		return cellularCollapse != null && target.hasEffect(cellularCollapse);
	}

	private static boolean isWeaverRepeatedlyDamaged(Mob mob, CompoundTag mData) {
		if (mob == null || mData == null) return false;
		if (mob.hurtTime > 0) return true;
		int hits = persistentInt(mData, "weaver_recent_damage_hits", 0);
		int timer = persistentInt(mData, "weaver_recent_damage_timer", 0);
		return hits >= 1 && timer > 0;
	}

	private enum TargetIntent {
		ENGAGING,
		EVADING,
		FORTIFYING,
		UNKNOWN
	}

	private static TargetIntent inferTargetIntent(Mob mob, LivingEntity target) {
		if (target == null) return TargetIntent.UNKNOWN;

		double dist = mob.distanceTo(target);
		Vec3 targetVel = target.getDeltaMovement();
		double targetSpeed = targetVel.horizontalDistance();

		// Check if target is pillaring or hiding/blocking
		boolean isPillaring = isTargetPillaring(target.level(), target, mob);
		if (isPillaring) {
			return TargetIntent.FORTIFYING;
		}

		// Check if target is shielding/blocking or eating/using item
		if (target.isUsingItem()) {
			net.minecraft.world.item.UseAnim anim = target.getUseItem().getUseAnimation();
			if (anim == net.minecraft.world.item.UseAnim.BLOCK) {
				return TargetIntent.FORTIFYING;
			}
		}

		// Calculate relative velocity
		Vec3 toTarget = target.position().subtract(mob.position()).normalize();
		double approachVelocity = targetVel.x * -toTarget.x + targetVel.z * -toTarget.z; // positive if moving towards mob

		if (approachVelocity > 0.1) {
			return TargetIntent.ENGAGING;
		} else if (approachVelocity < -0.1 || targetSpeed > 0.22) {
			return TargetIntent.EVADING;
		}

		return TargetIntent.ENGAGING; // Default to ENGAGING if close and active
	}

	private static String chooseHypnoMode(Mob mob, CompoundTag mData, LivingEntity target) {
		boolean hasCollapse = targetHasCellularCollapse(target);
		boolean isNonPlayer = target != null && !(target instanceof Player);
		boolean keepsGettingDamaged = isWeaverRepeatedlyDamaged(mob, mData);
		boolean isCriticalHp = mob != null && (mob.getHealth() / mob.getMaxHealth()) <= INCINERATE_CRITICAL_HP_RATIO;

		TargetIntent intent = inferTargetIntent(mob, target);

		// Calculate base scores for each ability mode
		double infectScore = 0.0;
		double leechScore = 0.0;
		double incinerateScore = 0.0;
		double vaporizeScore = 0.0;

		// 1. Target Type Constraints
		if (isNonPlayer) {
			infectScore = -999.0; // Non-players cannot contract player cellular collapse
		} else {
			if (!hasCollapse) {
				// Target doesn't have Cellular Collapse -> Highly prioritize INFECT to fulfill primary directive
				infectScore += 8.0;
			} else {
				// Target already has Cellular Collapse -> Reduce infect score
				infectScore -= 5.0;
			}
		}

		// 2. Critical HP / Emergency Constraints
		if (isCriticalHp) {
			incinerateScore += 6.0; // Desperately vaporize them with fire!
			leechScore += 4.0;       // Need health regeneration!
		}

		// 3. Target Intent Adjustments
		switch (intent) {
			case ENGAGING:
				// Player is aggressively fighting/approaching
				leechScore += 2.0;       // Great to siphon HP during close combat
				incinerateScore += 1.0;  // Deal huge close-quarters burst damage
				break;
			case EVADING:
				// Player is trying to escape, run away, or spacing out
				vaporizeScore += 4.0;    // Vaporize mode has set-fire mechanics and great range impact
				infectScore += 1.5;      // Hook them before they escape
				break;
			case FORTIFYING:
				// Player is shielding, blocking, or towering/pillaring
				incinerateScore += 3.0;  // Burn down their defenses!
				vaporizeScore += 2.0;    // Vaporize set-fire works extremely well against static/blocking targets
				break;
			default:
				break;
		}

		// 4. Incorporate Learned Biases
		infectScore += persistentDouble(mData, "ai_bias_infect", 0.0);
		leechScore += persistentDouble(mData, "ai_bias_leech", 0.0);
		incinerateScore += persistentDouble(mData, "ai_bias_incinerate", 0.0);
		vaporizeScore += persistentDouble(mData, "ai_bias_vaporize", 0.0);

		// Select the highest-scoring mode (with a slight random perturbation to avoid getting stuck in local minima)
		double rngFactor = 0.1;
		infectScore += mob.getRandom().nextDouble() * rngFactor;
		leechScore += mob.getRandom().nextDouble() * rngFactor;
		incinerateScore += mob.getRandom().nextDouble() * rngFactor;
		vaporizeScore += mob.getRandom().nextDouble() * rngFactor;

		String chosenMode = "infect";
		double maxScore = infectScore;

		if (leechScore > maxScore) {
			chosenMode = "leech";
			maxScore = leechScore;
		}
		if (incinerateScore > maxScore) {
			chosenMode = "incinerate";
			maxScore = incinerateScore;
		}
		if (vaporizeScore > maxScore) {
			chosenMode = "vaporize";
			maxScore = vaporizeScore;
		}

		if (WEAVER_DEBUG) {
			System.out.printf("[Weaver AI] Evaluated modes (Intent: %s, Critical: %b) -> INFECT: %.2f, LEECH: %.2f, INCINERATE: %.2f, VAPORIZE: %.2f -> CHOSEN: %s%n",
				intent.name(), isCriticalHp, infectScore, leechScore, incinerateScore, vaporizeScore, chosenMode);
		}

		return chosenMode;
	}

	private static boolean isTargetHoldingInfinityTotem(LivingEntity target) {
		if (target == null) return false;
		net.minecraft.world.item.ItemStack mainHand = target.getMainHandItem();
		net.minecraft.world.item.ItemStack offHand = target.getOffhandItem();
		if (!mainHand.isEmpty() && BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString().equals("avaritia:infinity_totem")) {
			return true;
		}
		if (!offHand.isEmpty() && BuiltInRegistries.ITEM.getKey(offHand.getItem()).toString().equals("avaritia:infinity_totem")) {
			return true;
		}
		return false;
	}

	private static boolean isSpaceClearAt(LevelAccessor world, BlockPos pos) {
		if (pos == null) return false;
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		for (int h = 0; h < 6; h++) {
			mpos.set(pos.getX(), pos.getY() + h, pos.getZ());
			BlockState state = world.getBlockState(mpos);
			if (!state.isAir() && !state.getCollisionShape(world, mpos).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static BlockPos getOverpowerLeapTarget(LevelAccessor world, Mob mob, LivingEntity target) {
		if (!isCeilingHighEnough(world, mob)) return null;
		double jumpDX = target.getX() - mob.getX();
		double jumpDZ = target.getZ() - mob.getZ();
		double len = Math.sqrt(jumpDX * jumpDX + jumpDZ * jumpDZ);
		if (len > 0) { jumpDX /= len; jumpDZ /= len; }
		
		double landX = target.getX() + jumpDX * 6.0;
		double landZ = target.getZ() + jumpDZ * 6.0;
		BlockPos landPos = findGroundPos(world, landX, target.getY(), landZ);
		if (landPos != null && isSpaceClearAt(world, landPos)) {
			return landPos;
		}
		return null;
	}

	private static BlockPos findWallAmbushSpot(LevelAccessor world, LivingEntity target, Mob mob) {
		net.minecraft.util.RandomSource rand = mob.getRandom();
		BlockPos targetPos = target.blockPosition();

		for (int attempt = 0; attempt < 30; attempt++) {
			double angle = rand.nextDouble() * 2.0 * Math.PI;
			double radius = 12.0 + rand.nextDouble() * 28.0; // 12 to 40 blocks randomly away from player
			int candidateX = (int) Math.floor(target.getX() + Math.cos(angle) * radius);
			int candidateZ = (int) Math.floor(target.getZ() + Math.sin(angle) * radius);

			for (int yOffset = -5; yOffset <= 5; yOffset++) {
				int candidateY = targetPos.getY() + yOffset;
				BlockPos footPos = new BlockPos(candidateX, candidateY, candidateZ);

				// Check floor block below candidate
				BlockState floorState = world.getBlockState(footPos.below());
				if (floorState.isAir() || !floorState.isSolidRender(world, footPos.below())) continue;

				// Check for air space: minimum 2 width (dx 0..1, dz 0..1) and 6 high air (dy 0..5)
				boolean fitsAir = true;
				BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
				for (int dx = 0; dx <= 1; dx++) {
					for (int dz = 0; dz <= 1; dz++) {
						for (int dy = 0; dy <= 5; dy++) {
							mpos.set(candidateX + dx, candidateY + dy, candidateZ + dz);
							BlockState state = world.getBlockState(mpos);
							if (!state.isAir() && !state.getCollisionShape(world, mpos).isEmpty()) {
								fitsAir = false;
								break;
							}
						}
						if (!fitsAir) break;
					}
					if (!fitsAir) break;
				}

				if (fitsAir) {
					// We have a space. Now check if there is a wall between this and the target.
					net.minecraft.world.phys.HitResult hitResult = world.clip(new net.minecraft.world.level.ClipContext(
							new Vec3(candidateX + 0.5, candidateY + 1.5, candidateZ + 0.5),
							target.getEyePosition(),
							net.minecraft.world.level.ClipContext.Block.COLLIDER,
							net.minecraft.world.level.ClipContext.Fluid.NONE,
							mob
					));
					
					if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
						BlockPos hitPos = ((net.minecraft.world.phys.BlockHitResult)hitResult).getBlockPos();
						BlockState hitState = world.getBlockState(hitPos);
						float hardness = hitState.getDestroySpeed(world, hitPos);
						if (hardness >= 0.0F && hardness <= DASH_MAX_BLOCK_HARDNESS) {
							return footPos;
						}
					}
				}
			}
		}
		return null;
	}

	private static BlockPos findChargeTeleportSpot(LevelAccessor world, LivingEntity target, Mob mob) {
		net.minecraft.util.RandomSource rand = mob.getRandom();
		BlockPos targetPos = target.blockPosition();

		for (int attempt = 0; attempt < 25; attempt++) {
			double angle = rand.nextDouble() * 2.0 * Math.PI;
			double radius = 8.0 + rand.nextDouble() * 7.0; // 8 to 15 blocks randomly away from player
			int candidateX = (int) Math.floor(target.getX() + Math.cos(angle) * radius);
			int candidateZ = (int) Math.floor(target.getZ() + Math.sin(angle) * radius);

			for (int yOffset = -4; yOffset <= 4; yOffset++) {
				int candidateY = targetPos.getY() + yOffset;
				BlockPos footPos = new BlockPos(candidateX, candidateY, candidateZ);

				// Check floor block below candidate
				BlockState floorState = world.getBlockState(footPos.below());
				if (floorState.isAir() || !floorState.isSolidRender(world, footPos.below())) continue;

				// Check for air space: minimum 2 width (dx 0..1, dz 0..1) and 6 high air (dy 0..5)
				boolean fitsAir = true;
				BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
				for (int dx = 0; dx <= 1; dx++) {
					for (int dz = 0; dz <= 1; dz++) {
						for (int dy = 0; dy <= 5; dy++) {
							mpos.set(candidateX + dx, candidateY + dy, candidateZ + dz);
							BlockState state = world.getBlockState(mpos);
							if (!state.isAir() && !state.getCollisionShape(world, mpos).isEmpty()) {
								fitsAir = false;
								break;
							}
						}
						if (!fitsAir) break;
					}
					if (!fitsAir) break;
				}

				if (fitsAir) {
					return footPos;
				}
			}
		}
		return null;
	}

	private static void performWeaverTpOrDespawn(LevelAccessor world, Mob mob) {
		double x = mob.getX();
		double y = mob.getY();
		double z = mob.getZ();
		if (world instanceof ServerLevel sl) {
			sl.playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.5F);
			sl.sendParticles(ParticleTypes.FLASH, x, y + 3.0, z, 50, 0.5, 1.5, 0.5, 0.1);
			sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 2.0, z, 40, 0.5, 1.0, 0.5, 0.05);
		}
		if (mob.getRandom().nextBoolean()) {
			mob.discard();
		} else {
			double angle = mob.getRandom().nextDouble() * Math.PI * 2.0;
			double dist = 75.0 + mob.getRandom().nextDouble() * 35.0;
			double tx = x + Math.cos(angle) * dist;
			double tz = z + Math.sin(angle) * dist;
			BlockPos ground = findGroundPos(world, tx, y, tz);
			if (ground != null) {
				mob.teleportTo(ground.getX() + 0.5, ground.getY() + 0.1, ground.getZ() + 0.5);
				if (world instanceof ServerLevel sl) {
					sl.playSound(null, ground, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.5F);
					sl.sendParticles(ParticleTypes.FLASH, ground.getX() + 0.5, ground.getY() + 2.0, ground.getZ() + 0.5, 40, 0.5, 1.0, 0.5, 0.1);
				}
			} else {
				mob.discard();
			}
		}
	}

	private static boolean processProjectileParryCheck(Mob mob, CompoundTag mData, LevelAccessor world) {
		if (mob == null || world == null || mData == null) return false;
		int parryCd = persistentInt(mData, "parry_cooldown", 0);
		if (parryCd > 0) return false;

		// Do not trigger parry if Weaver is already in Hypnosis, dashing, or dying
		if (persistentBoolean(mData, "is_dying", false) || persistentBoolean(mData, "is_dashing", false) || persistentInt(mData, "hypno_phase", 0) != 0) return false;

		AABB searchBox = mob.getBoundingBox().inflate(24.0);
		List<net.minecraft.world.entity.projectile.Projectile> projectiles = world.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, searchBox);

		Vec3 weaverCenter = mob.getBoundingBox().getCenter();

		for (net.minecraft.world.entity.projectile.Projectile proj : projectiles) {
			if (proj == null || !proj.isAlive()) continue;
			if (proj.getOwner() == mob) continue;

			Vec3 pPos = proj.position();
			Vec3 pVel = proj.getDeltaMovement();
			double speed = pVel.length();
			if (speed < 0.15) continue;

			Vec3 toWeaver = weaverCenter.subtract(pPos);
			double dot = pVel.dot(toWeaver);
			if (dot <= 0) continue; // Projectile moving away from Weaver

			double distance = toWeaver.length();
			double ticksToImpact = distance / speed;

			// Predict location at point of closest impact
			Vec3 futurePos = pPos.add(pVel.scale(ticksToImpact));
			double closestDist = futurePos.distanceTo(weaverCenter);

			// Trigger predictive sweep parry: start windup so the claw reaches apex right as projectile arrives within reach
			if (closestDist <= 4.2 && ticksToImpact >= 4.0 && ticksToImpact <= 22.0) {
				clearOtherStates(mData, "ATTACKING");
				updateStateAndLog(mob, mData, "ATTACKING");
				mData.putBoolean("is_attacking", true);
				mData.putBoolean("is_parrying", true);

				// Select Sweep Left (6) or Sweep Right (7) animation
				int sweepType = mob.getRandom().nextBoolean() ? 6 : 7;
				mData.putInt("attack_type", sweepType);
				mData.putInt("attack_peak_tick", 20); // Apex swing centered at tick 20 of 40
				mData.putInt("attack_timer", MELEE_ATTACK_DURATION);
				mData.putInt("parry_cooldown", PARRY_COOLDOWN);

				// Lock facing toward incoming projectile source, but override with player facing if near player
				LivingEntity target = mob.getTarget();
				double distToTarget = (target != null && target.isAlive()) ? mob.distanceTo(target) : 999.0;

				Vec3 projSource = (proj.getOwner() != null) ? proj.getOwner().position() : pPos;
				double dx_source = projSource.x - mob.getX();
				double dz_source = projSource.z - mob.getZ();
				float parryYaw = (float) (Math.atan2(dz_source, dx_source) * (180.0 / Math.PI)) - 90.0F;

				if (distToTarget <= 10.0 && target != null) {
					double dx_t = target.getX() - mob.getX();
					double dz_t = target.getZ() - mob.getZ();
					parryYaw = (float) (Math.atan2(dz_t, dx_t) * (180.0 / Math.PI)) - 90.0F;
				}

				applyRotationToAll(mob, parryYaw);
				mData.putFloat("attack_facing_yaw", parryYaw);

				syncWeaverEntityData(mob, false, mData);
				return true;
			}
		}
		return false;
	}

	private static void processSwatProjectiles(Mob mob, CompoundTag mData, LevelAccessor world) {
		if (mob == null || world == null) return;
		AABB swatArea = mob.getBoundingBox().inflate(5.0, 3.5, 5.0);
		List<net.minecraft.world.entity.projectile.Projectile> projectiles = world.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, swatArea);

		for (net.minecraft.world.entity.projectile.Projectile proj : projectiles) {
			if (proj == null || !proj.isAlive()) continue;
			if (proj.getOwner() == mob) continue;

			Vec3 pVel = proj.getDeltaMovement();
			double deflectSpeed = Math.max(1.35, pVel.length() * 1.25);
			Vec3 away = proj.position().subtract(mob.getBoundingBox().getCenter()).normalize();
			if (away.lengthSqr() < 0.001) away = mob.getLookAngle();

			Vec3 swatVel = new Vec3(
				away.x * deflectSpeed + (mob.getRandom().nextDouble() - 0.5) * 0.4,
				Math.max(0.25, away.y * 0.5 + 0.35),
				away.z * deflectSpeed + (mob.getRandom().nextDouble() - 0.5) * 0.4
			);

			proj.setDeltaMovement(swatVel);
			proj.hasImpulse = true;
			proj.setOwner(mob);

			mob.level().playSound(null, proj.getX(), proj.getY(), proj.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.4F, 0.85F);
			mob.level().playSound(null, mob.getX(), mob.getY() + 1.2, mob.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 1.0F);

			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.SWEEP_ATTACK, proj.getX(), proj.getY() + 1.0, proj.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
				sl.sendParticles(ParticleTypes.CRIT, proj.getX(), proj.getY() + 1.0, proj.getZ(), 12, 0.25, 0.25, 0.25, 0.2);
			}
		}
	}

	private static void performDashBlockDestruction(LevelAccessor world, Mob mob, float lockedYaw) {
		double rad = Math.toRadians(lockedYaw + 90.0F);
		double dirX = Math.cos(rad);
		double dirZ = Math.sin(rad);
		double perpX = -dirZ;
		double perpZ = dirX;

		double footY = Math.floor(mob.getY());
		double topY = Math.ceil(mob.getY() + 4.5);

		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

		for (double fwd = 0.6; fwd <= 3.2; fwd += 0.8) {
			for (double side = -1.6; side <= 1.6; side += 0.8) {
				double checkX = mob.getX() + dirX * fwd + perpX * side;
				double checkZ = mob.getZ() + dirZ * fwd + perpZ * side;

				for (int y = (int) footY; y <= (int) topY; y++) {
					mpos.set((int) Math.floor(checkX), y, (int) Math.floor(checkZ));
					BlockState state = world.getBlockState(mpos);
					if (!state.isAir() && !state.getCollisionShape(world, mpos).isEmpty()) {
						float hardness = state.getDestroySpeed(world, mpos);
						if (hardness >= 0.0F && hardness <= DASH_MAX_BLOCK_HARDNESS) {
							if (world instanceof ServerLevel sl) {
								sl.levelEvent(2001, mpos, net.minecraft.world.level.block.Block.getId(state));
							}
							world.setBlock(mpos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
						}
					}
				}
			}
		}
	}

	private static void tickWeightCollapse(ServerLevel world, Mob mob, CompoundTag mData) {
		if (!mob.onGround()) return;

		int centerFloorX = (int) Math.floor(mob.getX());
		int centerFloorY = (int) Math.floor(mob.getY());
		int centerFloorZ = (int) Math.floor(mob.getZ());

		CompoundTag collapsingMap = mData.getCompound("collapsing_map");
		boolean updatedMap = false;

		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		
		// Iterate 3x3 footprint directly below feet
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				int bx = centerFloorX + dx;
				int bz = centerFloorZ + dz;
				int by = centerFloorY - 1;
				mpos.set(bx, by, bz);
				BlockState state = world.getBlockState(mpos);
				
				if (state.isAir() || state.is(net.minecraft.world.level.block.Blocks.BEDROCK) || state.is(net.minecraft.world.level.block.Blocks.BARRIER)) {
					continue;
				}
				float destroySpeed = state.getDestroySpeed(world, mpos);
				if (destroySpeed < 0) {
					continue; // Indestructible blocks
				}

				// Trace downwards to measure contiguous solid thickness before air/void layer
				int solidThickness = 0;
				boolean hasAirBeneath = false;
				BlockPos.MutableBlockPos tracePos = new BlockPos.MutableBlockPos();
				for (int dy = 1; dy <= 6; dy++) {
					tracePos.set(bx, by - dy, bz);
					BlockState belowState = world.getBlockState(tracePos);
					if (belowState.isAir()) {
						hasAirBeneath = true;
						break;
					} else if (belowState.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
						// Bedrock means solid ground, never collapse
						solidThickness = 999;
						break;
					} else {
						solidThickness++;
					}
				}

				// Only collapse if there is actually air beneath (suspended) AND thickness is <= 3 blocks
				if (hasAirBeneath && solidThickness >= 1 && solidThickness <= 3) {
					String posKey = bx + "_" + by + "_" + bz;
					int progress = collapsingMap.getInt(posKey);

					int rate = 1;
					if (solidThickness == 1) rate = 15;
					else if (solidThickness == 2) rate = 5;
					else if (solidThickness == 3) rate = 2;

					progress += rate;
					collapsingMap.putInt(posKey, progress);
					updatedMap = true;

					BlockPos finalPos = new BlockPos(bx, by, bz);

					if (progress >= 100) {
						// CRASH AND COLLAPSE!
						world.destroyBlock(finalPos, false);
						world.playSound(null, finalPos.getX(), finalPos.getY(), finalPos.getZ(), 
							state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.2F, 0.6F);
						collapsingMap.remove(posKey);
					} else {
						// Play stress creaking/cracking and spawn stress particles
						if (progress % 10 == 0) {
							world.playSound(null, finalPos.getX(), finalPos.getY(), finalPos.getZ(), 
								SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 0.40F);
							if (world.getRandom().nextFloat() < 0.4F) {
								world.playSound(null, finalPos.getX(), finalPos.getY(), finalPos.getZ(), 
									SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.5F, 0.50F);
							}
						}
						if (progress % 5 == 0) {
							world.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, state), 
								finalPos.getX() + 0.5, finalPos.getY() + 0.5, finalPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
						}
					}
				}
			}
		}

		if (updatedMap || !collapsingMap.isEmpty()) {
			// Slowly decay any non-active crumbling blocks back to 0
			java.util.Set<String> keys = new java.util.HashSet<>(collapsingMap.getAllKeys());
			for (String key : keys) {
				try {
					String[] parts = key.split("_");
					if (parts.length == 3) {
						int kx = Integer.parseInt(parts[0]);
						int ky = Integer.parseInt(parts[1]);
						int kz = Integer.parseInt(parts[2]);
						// If the Weaver is no longer standing close to this block, decay progress
						double distSqr = mob.distanceToSqr(kx + 0.5, ky + 1.0, kz + 0.5);
						if (distSqr > 16.0) {
							int currentProgress = collapsingMap.getInt(key);
							if (currentProgress > 5) {
								collapsingMap.putInt(key, currentProgress - 5);
							} else {
								collapsingMap.remove(key);
							}
							updatedMap = true;
						}
					}
				} catch (Exception ignored) {}
			}
		}

		if (updatedMap) {
			mData.put("collapsing_map", collapsingMap);
		}
	}
}
// Version: 1.21.1
