package net.mcreator.thebackwoods.procedures;
// 1.21.1 neo
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.thebackwoods.entity.StiltWalkerEntity;
import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber
public class StiltWalkerOnEntityTickUpdateProcedure {

	// ================================================================
	// CONFIGURATION CONSTANTS
	// ================================================================

	// --- Rage / Stare Detection ---
	private static final double WATCH_DOT_THRESHOLD = 0.35;
	private static final int WATCH_TICKS_TO_ENRAGE_NORMAL = 100;
	private static final int WATCH_TICKS_TO_ENRAGE_EASY = 140;
	private static final int WATCH_TICKS_TO_ENRAGE_HARD = 70;
	private static final double PROXIMITY_ALARM_RANGE = 3.0; // Instant enrage if player gets within 3 blocks
	private static final double ESCAPE_RANGE = 22.0;
	private static final double TARGET_RANGE = 48.0;
	private static final int STARE_DECAY_RATE = 1; // Gradual decay per tick instead of instant snap to 0

	// --- Stalk Cycle & Escalation ---
	private static final double APPROACH_SPEED_NORMAL = 0.14;
	private static final double APPROACH_SPEED_EASY = 0.11;
	private static final double APPROACH_SPEED_HARD = 0.18;
	private static final double APPROACH_TRIGGER_HORIZONTAL = 6.0;
	private static final double APPROACH_TRIGGER_VERTICAL = 3.5; // Prevent triggering through floors/ceilings

	private static final int STALK_MIN_TICKS_STARE = 60;   // Short punishing cycle for staring
	private static final int STALK_MAX_TICKS_STARE = 140;
	private static final int STALK_MIN_TICKS_NORMAL = 120; // Default cycle
	private static final int STALK_MAX_TICKS_NORMAL = 260;
	private static final int MAX_CYCLE_COUNT_SCALING = 10;
	private static final int TELEPORT_TELEGRAPH_TICKS = 3; // Brief warning pulse before teleport/punish

	// --- Teleportation & Hazards ---
	private static final double TELEPORT_RADIUS_NORMAL = 24.0;
	private static final int TELEPORT_ATTEMPTS = 20;
	private static final int REQUIRED_AIR_BLOCKS = 4;
	private static final double FLANK_BEHIND_DISTANCE = 8.0;

	// --- Melee & Projectile Dodge ---
	private static final double PROJECTILE_DODGE_RANGE = 8.0;
	private static final double PROJECTILE_DODGE_DOT_THRESHOLD = 0.6;
	private static final int PROJECTILE_DODGE_COOLDOWN_TICKS = 10;

	private static final double MELEE_DETECTION_RADIUS = 3.5;
	private static final double MELEE_ATTACK_REACH = 3.2;
	private static final double MELEE_AIM_DOT_THRESHOLD = 0.4; // Facing check for swing
	private static final int MELEE_DODGE_COOLDOWN_TICKS = 25;  // Prevent per-tick dodge spam

	// --- Mining ---
	private static final double MINE_RAY_DISTANCE = 2.0;
	private static final float MINE_SPEED_MULTIPLIER = 50f;
	private static final float MINE_SPEED_BASE = 50f;
	private static final float MAX_BREAKABLE_HARDNESS = 50f;

	// --- Reinforcements & Splinter Cleanup ---
	private static final int MAX_SUMMONED_SPLINTERS_PER_PARENT = 6;
	private static final double REINFORCEMENT_SURROUND_RADIUS = 3.2;
	private static final int SUMMONED_IDLE_TIMEOUT_NORMAL = 140;
	private static final int SUMMONED_IDLE_TIMEOUT_ORPHANED = 40; // Fast despawn if parent is dead/unloaded
	private static final double SUMMONED_IDLE_PLAYER_RANGE = 14.0;
	private static final java.util.Map<UUID, java.lang.ref.WeakReference<StiltWalkerEntity>> PARENT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	public static void execute() {
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		Entity e = event.getEntity();
		if (e.level().isClientSide()) return;

		// Cleanup for summoned splinters
		if (e instanceof BlindspotSplinterEntity splinter) {
			handleSummonedSplinterCleanup(splinter.level(), splinter);
			return;
		}

		if (!(e instanceof StiltWalkerEntity stilt)) return;
		execute(stilt.level(), stilt.getX(), stilt.getY(), stilt.getZ(), stilt);
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof StiltWalkerEntity stilt)) return;

		if (handleRotAvoidanceAndTeleport(world, stilt)) {
			setSpeed(stilt, APPROACH_SPEED_NORMAL);
			return;
		}

		// Cache living parent reference for summoned splinters
		PARENT_CACHE.put(stilt.getUUID(), new java.lang.ref.WeakReference<>(stilt));

		// Decrement dodge cooldowns
		int meleeDodgeCooldown = getInt(stilt, "sw_melee_dodge_cooldown", 0);
		if (meleeDodgeCooldown > 0) putInt(stilt, "sw_melee_dodge_cooldown", meleeDodgeCooldown - 1);

		int projDodgeCooldown = getInt(stilt, "sw_projectile_dodge_cooldown", 0);
		if (projDodgeCooldown > 0) putInt(stilt, "sw_projectile_dodge_cooldown", projDodgeCooldown - 1);

		// 1. Predictive Melee Dodge: Check for close attackers starting an aimed swing
		if (stilt.tickCount >= 20 && meleeDodgeCooldown <= 0) {
			List<LivingEntity> attackers = world.getEntitiesOfClass(LivingEntity.class, stilt.getBoundingBox().inflate(MELEE_DETECTION_RADIUS));
			for (LivingEntity attacker : attackers) {
				if (attacker == stilt || attacker instanceof BlindspotSplinterEntity) continue;
				if (attacker instanceof Player p && (p.isCreative() || p.isSpectator())) continue;

				boolean isSwinging = attacker.swinging || attacker.swingTime > 0;
				if (isSwinging && stilt.distanceTo(attacker) <= MELEE_ATTACK_REACH) {
					// Require attacker swing to be aimed reasonably at the Stilt Walker
					Vec3 attackerToStilt = stilt.getBoundingBox().getCenter().subtract(attacker.getEyePosition());
					if (attackerToStilt.lengthSqr() > 1.0e-6) attackerToStilt = attackerToStilt.normalize();
					double aimDot = attacker.getLookAngle().normalize().dot(attackerToStilt);

					if (aimDot >= MELEE_AIM_DOT_THRESHOLD || attacker.hasLineOfSight(stilt)) {
						triggerPredictiveDodge(world, stilt, attacker);
						putInt(stilt, "sw_melee_dodge_cooldown", MELEE_DODGE_COOLDOWN_TICKS);
						return;
					}
				}
			}
		}

		// 2. Projectile Dodge
		if (tryDodgeProjectile(world, stilt)) return;

		// Target acquisition
		LivingEntity target = null;
		LivingEntity lastAttacker = stilt.getLastHurtByMob();
		if (lastAttacker != null && lastAttacker.isAlive() && stilt.distanceTo(lastAttacker) <= TARGET_RANGE) {
			target = lastAttacker;
		} else {
			target = findNearestPlayer(world, x, y, z, TARGET_RANGE);
		}

		if (target == null || (target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
			putInt(stilt, "sw_watch_timer", 0);
			putBool(stilt, "sw_enraged", false);
			putInt(stilt, "sw_stalk_timer", 0);
			setSpeed(stilt, 0.0);
			if (stilt instanceof Mob mob) mob.getNavigation().stop();
			return;
		}

		// 3. Continuous head & body locking onto target
		double dX = target.getX() - stilt.getX();
		double dY = target.getEyeY() - (stilt.getY() + stilt.getEyeHeight());
		double dZ = target.getZ() - stilt.getZ();
		float targetYaw = (float) (Math.atan2(dZ, dX) * 180.0 / Math.PI) - 90.0F;

		stilt.setYRot(targetYaw);
		stilt.yRotO = targetYaw;
		stilt.setYBodyRot(targetYaw);
		stilt.yBodyRotO = targetYaw;
		stilt.setYHeadRot(targetYaw);
		stilt.yHeadRotO = targetYaw;
		if (stilt instanceof Mob mob) {
			mob.getLookControl().setLookAt(target, 100.0F, 100.0F);
		}

		boolean enraged = getBool(stilt, "sw_enraged", false);
		int watchTimer = getInt(stilt, "sw_watch_timer", 0);
		String triggerType = getString(stilt, "sw_trigger_type", "staring");

		// Attacked trigger override
		if (lastAttacker != null && lastAttacker.isAlive() && !enraged) {
			enraged = true;
			watchTimer = 0;
			triggerType = "attacked";
			putString(stilt, "sw_trigger_type", triggerType);
			playAlertSound(world, stilt.blockPosition(), triggerType);
		}

		double dist = stilt.distanceTo(target);

		// Proximity alarm: instant enrage if player steps within 3 blocks
		if (!enraged && dist <= PROXIMITY_ALARM_RANGE) {
			enraged = true;
			watchTimer = 0;
			triggerType = "proximity";
			putString(stilt, "sw_trigger_type", triggerType);
			playAlertSound(world, stilt.blockPosition(), triggerType);
		}

		// Calculate gaze dot product
		Vec3 toStilt = stilt.getBoundingBox().getCenter().subtract(target.getEyePosition());
		if (toStilt.lengthSqr() > 1.0e-8) toStilt = toStilt.normalize();
		double dot = target.getLookAngle().normalize().dot(toStilt);
		boolean watched = dot > WATCH_DOT_THRESHOLD && target.hasLineOfSight(stilt);

		// Escape range resets enrage
		if (enraged && dist > ESCAPE_RANGE) {
			enraged = false;
			watchTimer = 0;
			putInt(stilt, "sw_stalk_timer", 0);
			putInt(stilt, "sw_teleport_telegraph", 0);
		}

		// Difficulty scaling parameters
		Difficulty diff = (world instanceof Level level) ? level.getDifficulty() : Difficulty.NORMAL;
		int ticksToEnrage = WATCH_TICKS_TO_ENRAGE_NORMAL;
		double baseApproachSpeed = APPROACH_SPEED_NORMAL;
		if (diff == Difficulty.EASY) {
			ticksToEnrage = WATCH_TICKS_TO_ENRAGE_EASY;
			baseApproachSpeed = APPROACH_SPEED_EASY;
		} else if (diff == Difficulty.HARD) {
			ticksToEnrage = WATCH_TICKS_TO_ENRAGE_HARD;
			baseApproachSpeed = APPROACH_SPEED_HARD;
		}

		// 4. Stare detection with gradual decay & distance weighting
		if (!enraged) {
			if (watched) {
				// Closer stares build rage faster
				double distWeight = Math.max(0.5, (TARGET_RANGE - dist) / 12.0);
				watchTimer += Math.max(1, (int) Math.round(1.0 * distWeight));

				if (watchTimer >= ticksToEnrage) {
					enraged = true;
					watchTimer = 0;
					triggerType = "staring";
					putString(stilt, "sw_trigger_type", triggerType);
					playAlertSound(world, stilt.blockPosition(), triggerType);
				}
			} else {
				// Gradual decay instead of snapping instantly to 0
				watchTimer = Math.max(0, watchTimer - STARE_DECAY_RATE);
			}
		}

		putBool(stilt, "sw_enraged", enraged);
		putInt(stilt, "sw_watch_timer", watchTimer);

		if (!enraged) {
			setSpeed(stilt, 0.0);
			if (stilt instanceof Mob mob) mob.getNavigation().stop();
			return;
		}

		// 5. Stalk & Cycle Escalation
		int cycleCount = getInt(stilt, "sw_cycle_count", 0);
		double speedBoost = Math.min(0.12, cycleCount * 0.015); // Escalates approach speed on repeated cycles
		double currentSpeed = baseApproachSpeed + speedBoost;

		setSpeed(stilt, currentSpeed);
		if (stilt instanceof Mob mob) {
			mob.setTarget(target);
			mob.getNavigation().moveTo(target, 1.0);
		}

		// Mine while approaching
		tryMineFrontBlock(world, stilt, target);

		// Stalk Goal Calculation based on trigger type and cycle escalation
		int stalkTimer = getInt(stilt, "sw_stalk_timer", 0) + 1;
		putInt(stilt, "sw_stalk_timer", stalkTimer);

		int stalkGoal = getInt(stilt, "sw_stalk_goal", 0);
		if (stalkGoal <= 0) {
			int minTicks = "staring".equals(triggerType) ? STALK_MIN_TICKS_STARE : STALK_MIN_TICKS_NORMAL;
			int maxTicks = "staring".equals(triggerType) ? STALK_MAX_TICKS_STARE : STALK_MAX_TICKS_NORMAL;

			// Scale stalk goal shorter on repeated cycles
			int cycleReduction = Math.min(60, cycleCount * 8);
			int finalMin = Math.max(30, minTicks - cycleReduction);
			int finalMax = Math.max(60, maxTicks - cycleReduction);

			stalkGoal = Mth.nextInt(stilt.getRandom(), finalMin, finalMax);
			putInt(stilt, "sw_stalk_goal", stalkGoal);
		}

		// Check approached condition (factoring vertical distance)
		double dist2D = Math.sqrt(dX * dX + dZ * dZ);
		double absDY = Math.abs(stilt.getY() - target.getY());
		boolean approached = (dist2D <= APPROACH_TRIGGER_HORIZONTAL) && (absDY <= APPROACH_TRIGGER_VERTICAL);
		boolean timerDone = stalkTimer >= stalkGoal;

		if (approached || timerDone) {
			// Telegraph pulse 2-3 ticks before punishment fires
			int telegraph = getInt(stilt, "sw_teleport_telegraph", TELEPORT_TELEGRAPH_TICKS);
			if (telegraph > 1) {
				putInt(stilt, "sw_teleport_telegraph", telegraph - 1);
				if (world instanceof ServerLevel serverLevel) {
					serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, stilt.getX(), stilt.getY() + 1.2, stilt.getZ(), 12, 0.3, 0.6, 0.3, 0.05);
				}
				return;
			}

			// Punishment sequence
			if (watched) {
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
				playAlertSound(world, stilt.blockPosition(), "punish");
				spawnReinforcements(world, stilt, target);
			}

			teleportAway(world, stilt, target);

			// Reset stalk cycle & increment cycle counter
			putInt(stilt, "sw_cycle_count", Math.min(MAX_CYCLE_COUNT_SCALING, cycleCount + 1));
			putInt(stilt, "sw_stalk_timer", 0);
			putInt(stilt, "sw_stalk_goal", 0);
			putInt(stilt, "sw_watch_timer", 0);
			putInt(stilt, "sw_teleport_telegraph", TELEPORT_TELEGRAPH_TICKS);
			putBool(stilt, "sw_enraged", false);
		}
	}

	private static void triggerPredictiveDodge(LevelAccessor world, StiltWalkerEntity stilt, LivingEntity attacker) {
		playAlertSound(world, stilt.blockPosition(), "dodge");

		if (attacker instanceof Player player) {
			player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
		}

		int count = Mth.nextInt(stilt.getRandom(), 2, 4);
		spawnReinforcementsCount(world, stilt, attacker, count);

		teleportAway(world, stilt, attacker);

		putInt(stilt, "sw_stalk_timer", 0);
		putInt(stilt, "sw_stalk_goal", 0);
		putInt(stilt, "sw_watch_timer", 0);
		putBool(stilt, "sw_enraged", false);
	}

	private static boolean tryDodgeProjectile(LevelAccessor world, StiltWalkerEntity stilt) {
		int dodgeCooldown = getInt(stilt, "sw_projectile_dodge_cooldown", 0);
		if (dodgeCooldown > 0) return false;

		AABB dodgeBox = stilt.getBoundingBox().inflate(PROJECTILE_DODGE_RANGE);
		List<Projectile> projectiles = world.getEntitiesOfClass(Projectile.class, dodgeBox, Projectile::isAlive);

		Vec3 stiltCenter = stilt.getBoundingBox().getCenter();
		for (Projectile projectile : projectiles) {
			if (projectile.getOwner() == stilt) continue;

			Vec3 motion = projectile.getDeltaMovement();
			if (motion.lengthSqr() < 1.0e-6) continue;

			Vec3 projectileToStilt = stiltCenter.subtract(projectile.position());
			if (projectileToStilt.lengthSqr() < 1.0e-6) continue;

			double approachDot = motion.normalize().dot(projectileToStilt.normalize());
			if (approachDot < PROJECTILE_DODGE_DOT_THRESHOLD) continue;

			teleportAway(world, stilt, null);
			putInt(stilt, "sw_projectile_dodge_cooldown", PROJECTILE_DODGE_COOLDOWN_TICKS);
			return true;
		}

		return false;
	}

	private static void tryMineFrontBlock(LevelAccessor world, StiltWalkerEntity stilt, LivingEntity target) {
		Vec3 eyes = stilt.getEyePosition(1f);
		Vec3 view = stilt.getViewVector(1f);
		HitResult hit = world.clip(new ClipContext(eyes, eyes.add(view.scale(MINE_RAY_DISTANCE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, stilt));

		if (hit.getType() != HitResult.Type.BLOCK) {
			putInt(stilt, "sw_mine_progress", 0);
			return;
		}

		BlockPos facePos = ((BlockHitResult) hit).getBlockPos();
		BlockPos feetPos = new BlockPos(facePos.getX(), Mth.floor(stilt.getY()), facePos.getZ());
		BlockPos torsoPos = feetPos.above(1);

		// Area mining check (sweeping adjacent blocks around hit area)
		boolean canMineFeet = canMine(world, feetPos, target);
		boolean canMineTorso = canMine(world, torsoPos, target);
		boolean canMineHead = canMine(world, facePos, target);

		if (!(canMineFeet || canMineTorso || canMineHead)) {
			putInt(stilt, "sw_mine_progress", 0);
			return;
		}

		int mineProgress = getInt(stilt, "sw_mine_progress", 0) + 1;
		putInt(stilt, "sw_mine_progress", mineProgress);

		if (stilt.tickCount % 6 == 0) stilt.swing(InteractionHand.MAIN_HAND);

		float speedRef = 0f;
		if (canMineFeet) speedRef = Math.max(speedRef, world.getBlockState(feetPos).getDestroySpeed(world, feetPos));
		if (canMineTorso) speedRef = Math.max(speedRef, world.getBlockState(torsoPos).getDestroySpeed(world, torsoPos));
		if (canMineHead) speedRef = Math.max(speedRef, world.getBlockState(facePos).getDestroySpeed(world, facePos));

		float mineThreshold = speedRef * MINE_SPEED_MULTIPLIER + MINE_SPEED_BASE;

		// Particle & Sound crack feedback as progress approaches threshold
		float progressRatio = Math.min(1.0f, (float) mineProgress / Math.max(1.0f, mineThreshold));
		if (world instanceof ServerLevel serverLevel && mineProgress % 4 == 0) {
			BlockPos targetPos = canMineHead ? facePos : (canMineTorso ? torsoPos : feetPos);
			BlockState state = world.getBlockState(targetPos);
			serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
					targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
					(int) (progressRatio * 6 + 2), 0.2, 0.2, 0.2, 0.05);

			if (world instanceof Level level) {
				float pitch = 0.6f + (progressRatio * 0.7f);
				level.playSound(null, targetPos, SoundEvents.WOOD_HIT, SoundSource.HOSTILE, 0.6f, pitch);
			}
		}

		if (mineProgress > mineThreshold) {
			if (canMineFeet) world.destroyBlock(feetPos, false);
			if (canMineTorso) world.destroyBlock(torsoPos, false);
			if (canMineHead) world.destroyBlock(facePos, false);
			putInt(stilt, "sw_mine_progress", 0);
		}
	}

	private static boolean canMine(LevelAccessor world, BlockPos pos, LivingEntity player) {
		float speed = world.getBlockState(pos).getDestroySpeed(world, pos);
		return speed >= 0 && speed < MAX_BREAKABLE_HARDNESS && pos.getY() != (int) (player.getY() - 2) && !world.getBlockState(pos).isAir();
	}

	@SubscribeEvent
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity() instanceof StiltWalkerEntity stilt) {
			Entity attacker = event.getSource().getEntity();
			if (attacker instanceof LivingEntity livingAttacker) {
				// Scale reinforcement count off actual damage dealt & missing health
				float damageAmount = event.getAmount();
				float maxHp = stilt.getMaxHealth();
				float missingHpRatio = Math.max(0f, (maxHp - stilt.getHealth()) / Math.max(1f, maxHp));

				int count = Math.min(4, Math.max(1, Math.round(damageAmount / 4.0f + missingHpRatio * 2.0f)));

				spawnReinforcementsCount(stilt.level(), stilt, livingAttacker, count);
				teleportAway(stilt.level(), stilt, livingAttacker);
			}
		}
	}

	private static void spawnReinforcementsCount(LevelAccessor world, StiltWalkerEntity stilt, LivingEntity attacker, int count) {
		if (!(world instanceof ServerLevel serverLevel) || attacker == null) return;

		// Global cap on active summoned splinters per parent
		List<BlindspotSplinterEntity> existingSplinters = serverLevel.getEntitiesOfClass(
				BlindspotSplinterEntity.class, stilt.getBoundingBox().inflate(32.0),
				s -> getBool(s, "bw_stilt_summoned", false) && stilt.getStringUUID().equals(getString(s, "bw_stilt_parent_uuid", "")));

		if (existingSplinters.size() >= MAX_SUMMONED_SPLINTERS_PER_PARENT) return;

		int spawnCount = Math.min(count, MAX_SUMMONED_SPLINTERS_PER_PARENT - existingSplinters.size());
		double startAngle = stilt.getRandom().nextDouble() * Math.PI * 2.0;

		long gameTime = (world.getLevelData() != null) ? world.getLevelData().getGameTime() : 0L;

		for (int i = 0; i < spawnCount; i++) {
			double angle = startAngle + (i * Math.PI * 2.0 / spawnCount);
			double sx = attacker.getX() + Math.cos(angle) * REINFORCEMENT_SURROUND_RADIUS;
			double sz = attacker.getZ() + Math.sin(angle) * REINFORCEMENT_SURROUND_RADIUS;
			double sy = attacker.getY();

			BlindspotSplinterEntity splinter = new BlindspotSplinterEntity(TheBackwoodsModEntities.BLINDSPOT_SPLINTER.get(), serverLevel);
			splinter.teleportTo(sx, sy, sz);
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_isEnraged, 1);

			putBool(splinter, "bw_stilt_summoned", true);
			putString(splinter, "bw_stilt_parent_uuid", stilt.getStringUUID());
			putLong(splinter, "bw_stilt_summon_time", gameTime);
			putInt(splinter, "bw_stilt_idle_ticks", 0);

			serverLevel.addFreshEntity(splinter);
		}
	}

	private static void spawnReinforcements(LevelAccessor world, StiltWalkerEntity stilt, LivingEntity attacker) {
		int count = Mth.nextInt(stilt.getRandom(), 2, 3);
		spawnReinforcementsCount(world, stilt, attacker, count);
	}

	private static void handleSummonedSplinterCleanup(LevelAccessor world, BlindspotSplinterEntity splinter) {
		if (!(world instanceof ServerLevel serverLevel)) return;
		if (!getBool(splinter, "bw_stilt_summoned", false)) return;

		long gameTime = (world.getLevelData() != null) ? world.getLevelData().getGameTime() : 0L;

		long summonTime = getLong(splinter, "bw_stilt_summon_time", 0L);
		if (summonTime == 0L) {
			summonTime = gameTime;
			putLong(splinter, "bw_stilt_summon_time", summonTime);
		}

		long age = gameTime - summonTime;
		boolean shouldDespawn = age >= 300L; // 15s max joint lifespan

		if (!shouldDespawn) {
			String parentUuid = getString(splinter, "bw_stilt_parent_uuid", "");
			boolean parentEnraged = false;
			boolean parentOrphaned = getBool(splinter, "bw_stilt_parent_dead", false);

			if (!parentOrphaned && !parentUuid.isEmpty()) {
				try {
					UUID pUuid = UUID.fromString(parentUuid);
					java.lang.ref.WeakReference<StiltWalkerEntity> ref = PARENT_CACHE.get(pUuid);
					StiltWalkerEntity cachedParent = (ref != null) ? ref.get() : null;

					if (cachedParent != null && cachedParent.isAlive() && !cachedParent.isRemoved() && cachedParent.level() == serverLevel) {
						parentEnraged = getBool(cachedParent, "sw_enraged", false);
					} else {
						// Fallback to server level entity lookup if cache miss or invalidated reference
						Entity parent = serverLevel.getEntity(pUuid);
						if (parent instanceof StiltWalkerEntity stilt && parent.isAlive()) {
							PARENT_CACHE.put(pUuid, new java.lang.ref.WeakReference<>(stilt));
							parentEnraged = getBool(stilt, "sw_enraged", false);
						} else {
							// Parent entity confirmed dead or unloaded
							parentOrphaned = true;
							putBool(splinter, "bw_stilt_parent_dead", true);
							PARENT_CACHE.remove(pUuid);
						}
					}
				} catch (Throwable ignored) {
					parentOrphaned = true;
					putBool(splinter, "bw_stilt_parent_dead", true);
				}
			}

			if (parentEnraged) {
				putInt(splinter, "bw_stilt_idle_ticks", 0);
			} else {
				Player nearby = findNearestPlayer(world, splinter.getX(), splinter.getY(), splinter.getZ(), SUMMONED_IDLE_PLAYER_RANGE);
				int idle = getInt(splinter, "bw_stilt_idle_ticks", 0);
				idle = (nearby == null) ? idle + 1 : 0;
				putInt(splinter, "bw_stilt_idle_ticks", idle);

				int timeoutThreshold = parentOrphaned ? SUMMONED_IDLE_TIMEOUT_ORPHANED : SUMMONED_IDLE_TIMEOUT_NORMAL;
				if (idle >= timeoutThreshold) {
					shouldDespawn = true;
				}
			}
		}

		if (shouldDespawn) {
			serverLevel.sendParticles(ParticleTypes.SMOKE, splinter.getX(), splinter.getY() + 0.8, splinter.getZ(), 16, 0.25, 0.4, 0.25, 0.01);
			serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, splinter.getX(), splinter.getY() + 0.8, splinter.getZ(), 8, 0.2, 0.35, 0.2, 0.005);
			splinter.discard();
		}
	}

	private static void teleportAway(LevelAccessor world, StiltWalkerEntity stilt, LivingEntity target) {
		Difficulty diff = (world instanceof Level level) ? level.getDifficulty() : Difficulty.NORMAL;
		double radius = (diff == Difficulty.HARD) ? 30.0 : ((diff == Difficulty.EASY) ? 18.0 : TELEPORT_RADIUS_NORMAL);

		// Try biased candidate spots (flanking/behind target) for first 10 attempts
		for (int i = 0; i < TELEPORT_ATTEMPTS; i++) {
			double tx, ty, tz;

			if (target != null && i < 10) {
				// Position candidate behind target
				Vec3 targetLook = target.getLookAngle();
				Vec3 behind = target.position().subtract(targetLook.scale(FLANK_BEHIND_DISTANCE));

				tx = behind.x + (stilt.getRandom().nextDouble() - 0.5) * 8.0;
				ty = target.getY() + Mth.nextInt(stilt.getRandom(), -2, 4);
				tz = behind.z + (stilt.getRandom().nextDouble() - 0.5) * 8.0;
			} else {
				// Random candidate in radius
				tx = stilt.getX() + (stilt.getRandom().nextDouble() - 0.5) * 2.0 * radius;
				ty = stilt.getY() + Mth.nextInt(stilt.getRandom(), -4, 6);
				tz = stilt.getZ() + (stilt.getRandom().nextDouble() - 0.5) * 2.0 * radius;
			}

			BlockPos base = BlockPos.containing(tx, ty, tz);
			if (!isValidTeleportSpot(world, base)) continue;

			stilt.teleportTo(tx, ty, tz);

			// Post-teleport line of sight verification with 1 retry
			if (target != null && !stilt.hasLineOfSight(target) && i < TELEPORT_ATTEMPTS - 1) {
				continue; // Blind behind wall, retry for a better angle
			}

			playEndermanTeleportSound(world, base);
			if (stilt instanceof Mob mob) mob.getNavigation().stop();
			return;
		}

		// Fallback blink if all candidates fail (4 blocks upward/forward safe spot)
		BlockPos fallbackPos = stilt.blockPosition().above(3);
		if (isValidTeleportSpot(world, fallbackPos)) {
			stilt.teleportTo(fallbackPos.getX() + 0.5, fallbackPos.getY(), fallbackPos.getZ() + 0.5);
			playEndermanTeleportSound(world, fallbackPos);
		}
	}

	private static boolean isValidTeleportSpot(LevelAccessor world, BlockPos base) {
		BlockState below = world.getBlockState(base.below());
		if (below.isAir()) return false;

		// Hazard check: avoid lava, magma, cactus, powder snow, fire
		if (below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.CACTUS)
				|| below.is(Blocks.POWDER_SNOW) || below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE)) {
			return false;
		}

		// Check clearance & fluid presence
		for (int i = 0; i < REQUIRED_AIR_BLOCKS; i++) {
			BlockPos checkPos = base.above(i);
			BlockState state = world.getBlockState(checkPos);
			if (!state.isAir() || !world.getFluidState(checkPos).isEmpty()) return false;
		}
		return true;
	}

	private static void playAlertSound(LevelAccessor world, BlockPos pos, String type) {
		if (world instanceof Level level) {
			float pitch = "staring".equals(type) ? 0.75f : ("proximity".equals(type) ? 1.25f : 0.50f);
			level.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 2.0f, pitch);
		}
	}

	private static void playEndermanTeleportSound(LevelAccessor world, BlockPos pos) {
		if (world instanceof Level level) {
			level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.6f, 0.7f);
		}
	}

	private static void setSpeed(LivingEntity entity, double speed) {
		if (entity.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)) {
			entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
		}
	}

	private static Player findNearestPlayer(LevelAccessor world, double x, double y, double z, double range) {
		List<Player> players = world.getEntitiesOfClass(Player.class,
				AABB.ofSize(new Vec3(x, y, z), range, range, range),
				p -> !p.isCreative() && !p.isSpectator());

		return players.stream()
				.min(Comparator.comparingDouble(p -> p.distanceToSqr(x, y, z)))
				.orElse(null);
	}

	// Persistent data helpers (1.21.1)
	private static int getInt(Entity e, String key, int fallback) {
		return e.getPersistentData().contains(key) ? e.getPersistentData().getInt(key) : fallback;
	}

	private static boolean getBool(Entity e, String key, boolean fallback) {
		return e.getPersistentData().contains(key) ? e.getPersistentData().getBoolean(key) : fallback;
	}

	private static long getLong(Entity e, String key, long fallback) {
		return e.getPersistentData().contains(key) ? e.getPersistentData().getLong(key) : fallback;
	}

	private static String getString(Entity e, String key, String fallback) {
		return e.getPersistentData().contains(key) ? e.getPersistentData().getString(key) : fallback;
	}

	private static void putInt(Entity e, String key, int value) {
		e.getPersistentData().putInt(key, value);
	}

	private static void putBool(Entity e, String key, boolean value) {
		e.getPersistentData().putBoolean(key, value);
	}

	private static void putLong(Entity e, String key, long value) {
		e.getPersistentData().putLong(key, value);
	}

	private static void putString(Entity e, String key, String value) {
		e.getPersistentData().putString(key, value);
	}

	private static boolean handleRotAvoidanceAndTeleport(LevelAccessor world, Mob mob) {
		if (!IsRotNearbyConditionProcedure.execute(world, mob)) {
			if (getBool(mob, "rot_tp_charging", false)) {
				putBool(mob, "rot_tp_charging", false);
				putInt(mob, "rot_tp_timer", 0);
			}
			return false;
		}

		mob.setTarget(null);
		mob.setLastHurtByMob(null);

		boolean isCharging = getBool(mob, "rot_tp_charging", false);

		if (!isCharging) {
			double distToRot = 100.0;
			if (world instanceof Level _lvl) {
				List<net.mcreator.thebackwoods.entity.RotEntity> rots = _lvl.getEntitiesOfClass(
					net.mcreator.thebackwoods.entity.RotEntity.class,
					mob.getBoundingBox().inflate(64.0),
					e -> e.isAlive()
				);
				net.mcreator.thebackwoods.entity.RotEntity nearestRot = rots.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
				if (nearestRot != null) {
					distToRot = mob.distanceTo(nearestRot);
				} else {
					distToRot = 10.0;
				}
			}

			float chance;
			if (distToRot <= 10.0) {
				chance = 1.0f;
			} else if (distToRot <= 40.0) {
				chance = (float) (1.0 - (distToRot - 10.0) / 30.0);
			} else {
				chance = 0.05f;
			}

			if (distToRot <= 10.0 || mob.getRandom().nextFloat() < (chance * 0.15f)) {
				isCharging = true;
				putBool(mob, "rot_tp_charging", true);
				putInt(mob, "rot_tp_timer", 0);
				int duration = 60 + mob.getRandom().nextInt(41);
				putInt(mob, "rot_tp_duration", duration);
			}
		}

		if (isCharging) {
			int timer = getInt(mob, "rot_tp_timer", 0) + 1;
			int duration = getInt(mob, "rot_tp_duration", 80);
			putInt(mob, "rot_tp_timer", timer);

			double progress = Math.min(1.0, (double) timer / (double) duration);

			if (world instanceof ServerLevel _sLevel) {
				int particleCount = 1 + (int) (progress * 15.0);
				_sLevel.sendParticles(
					ParticleTypes.FLASH,
					mob.getX() + (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth() * 1.5,
					mob.getY() + mob.getRandom().nextDouble() * mob.getBbHeight(),
					mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth() * 1.5,
					particleCount,
					0.1, 0.1, 0.1, 0.05
				);
			}

			if (timer >= duration) {
				performFarTeleportAwayFromRot(world, mob);
				putBool(mob, "rot_tp_charging", false);
				putInt(mob, "rot_tp_timer", 0);
			}
		}

		return true;
	}

	private static void performFarTeleportAwayFromRot(LevelAccessor world, Mob mob) {
		if (!(world instanceof Level level)) return;

		Vec3 rotPos = null;
		List<net.mcreator.thebackwoods.entity.RotEntity> rots = level.getEntitiesOfClass(
			net.mcreator.thebackwoods.entity.RotEntity.class,
			mob.getBoundingBox().inflate(128.0),
			e -> e.isAlive()
		);
		if (!rots.isEmpty()) {
			net.mcreator.thebackwoods.entity.RotEntity closest = rots.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
			if (closest != null) {
				rotPos = closest.position();
			}
		}

		double mobX = mob.getX();
		double mobY = mob.getY();
		double mobZ = mob.getZ();

		level.playSound(null, BlockPos.containing(mobX, mobY, mobZ), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0f, 0.6f);

		if (world instanceof ServerLevel _sLevel) {
			_sLevel.sendParticles(ParticleTypes.FLASH, mobX, mobY + mob.getBbHeight() / 2.0, mobZ, 20, 0.5, 0.8, 0.5, 0.1);
		}

		double baseAngle;
		if (rotPos != null) {
			baseAngle = Math.atan2(mobZ - rotPos.z, mobX - rotPos.x);
		} else {
			baseAngle = mob.getRandom().nextDouble() * Math.PI * 2.0;
		}

		boolean teleported = false;
		for (int attempt = 0; attempt < 24; attempt++) {
			double dist = 70.0 + mob.getRandom().nextDouble() * 50.0;
			double angle = baseAngle + (mob.getRandom().nextDouble() - 0.5) * (attempt < 12 ? Math.PI / 1.5 : Math.PI * 2.0);

			double targetX = mobX + Math.cos(angle) * dist;
			double targetZ = mobZ + Math.sin(angle) * dist;

			int startY = (int) mobY;
			for (int yOffset = 0; yOffset <= 20; yOffset = (yOffset <= 0) ? -yOffset + 1 : -yOffset) {
				int checkY = startY + yOffset;
				if (checkY < level.getMinBuildHeight() + 2 || checkY > level.getMaxBuildHeight() - 2) continue;

				BlockPos targetPos = BlockPos.containing(targetX, checkY, targetZ);
				BlockPos posBelow = targetPos.below();
				BlockPos posAbove = targetPos.above();

				if (level.getBlockState(posBelow).isSolid() && 
					level.getBlockState(targetPos).isAir() && 
					level.getBlockState(posAbove).isAir()) {
					
					mob.teleportTo(targetX + 0.5, checkY, targetZ + 0.5);

					level.playSound(null, targetPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0f, 0.8f);
					if (world instanceof ServerLevel _sLevel) {
						_sLevel.sendParticles(ParticleTypes.FLASH, targetX + 0.5, checkY + mob.getBbHeight() / 2.0, targetZ + 0.5, 25, 0.5, 0.8, 0.5, 0.1);
					}
					teleported = true;
					break;
				}
			}

			if (teleported) break;
		}

		if (!teleported) {
			double dist = 75.0;
			double targetX = mobX + Math.cos(baseAngle) * dist;
			double targetZ = mobZ + Math.sin(baseAngle) * dist;
			int checkY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) targetX, (int) targetZ);
			mob.teleportTo(targetX, checkY, targetZ);
			level.playSound(null, BlockPos.containing(targetX, checkY, targetZ), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0f, 0.8f);
		}
	}
} // 1.21.1
