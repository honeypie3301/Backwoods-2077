package net.mcreator.thebackwoods.procedures;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.mcreator.thebackwoods.entity.LignumPalusEntity;

import java.util.List;
import java.util.Comparator;

@EventBusSubscriber
public class LignumPalusOnEntityTickUpdateProcedure {
	// Configuration Constants
	private static final double WANDER_SPEED = 0.05;              // Extremely slow, eerie pace
	private static final double EXTREME_CLOSE_IN_SPEED = 0.12;    // Closes in steadily
	private static final double BUMP_ATTACK_RADIUS = 1.20;        // Hitbox collision threshold
	private static final int STATIONARY_TICKS_THRESHOLD = 100;    // 5 seconds threshold of standing still
	private static final double PAUSE_CHANCE_O_GRID = 0.50;       // 50% chance to pause at any block center and pretend to be a pillar
	private static final int REQUIRED_STARE_TICKS = 70;           // Ticks required to stare directly before getting telekinetically lifted
	private static final int MAX_HYPNOTIZED_NON_PLAYERS = 5;      // Cap on non-player entities hypnotized at once

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		if (event.getEntity() != null && event.getEntity().getClass().getName().contains("LignumPalus")) {
			Entity entity = event.getEntity();
			execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity);
		}
	}

	@SubscribeEvent
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity() != null && event.getEntity().getClass().getName().contains("LignumPalus")) {
			Entity entity = event.getEntity();
			CompoundTag mData = entity.getPersistentData();
			Entity attacker = event.getSource().getEntity();

			// Ensure ANY incoming damage interrupts active hypnosis regardless of attacker identity
			if (persistentBoolean(mData, "is_hypnotizing", false)) {
				mData.putBoolean("is_hypnotizing", false);
				mData.putBoolean("isHypnotizing", false);
				mData.putBoolean("hypnotizing_non_players", false);
				mData.putInt("stare_ticks", 0);
				mData.putInt("hypnosis_ticks", 0);
				// Cooldown of 5 seconds (100 ticks) so it does not immediately re-hypnotize when damaged
				mData.putInt("hypnosis_cooldown", 100);

				// Clear hypnosis tag from any nearby entities that were hypnotized by this Palus
				clearAllHypnotizedTags(entity.level(), entity);
			}

			if (attacker instanceof LivingEntity livingAttacker && !(livingAttacker instanceof Player)) {
				// Cap non-player hypnotized count
				List<LivingEntity> potentialTargets = entity.level().getEntitiesOfClass(LivingEntity.class, new AABB(entity.getX() - 32, entity.getY() - 16, entity.getZ() - 32, entity.getX() + 32, entity.getY() + 16, entity.getZ() + 32));
				int count = 0;
				for (LivingEntity le : potentialTargets) {
					if (isHypnotizedBy(le, entity.getStringUUID())) {
						count++;
					}
				}

				if (count < MAX_HYPNOTIZED_NON_PLAYERS) {
					setHypnotizedTag(livingAttacker, entity.getStringUUID());
					
					boolean isHypnotizing = persistentBoolean(mData, "is_hypnotizing", false);
					boolean hypnotizingNonPlayers = persistentBoolean(mData, "hypnotizing_non_players", false);
					
					if (!isHypnotizing || !hypnotizingNonPlayers) {
						mData.putBoolean("is_hypnotizing", true);
						mData.putBoolean("isHypnotizing", true);
						mData.putBoolean("hypnotizing_non_players", true);
						mData.putInt("stare_ticks", 0);
						mData.putInt("hypnosis_ticks", 0);
						
						entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
							net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER,
							net.minecraft.sounds.SoundSource.HOSTILE,
							2.0F, 0.35F);
						entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
							net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN,
							net.minecraft.sounds.SoundSource.HOSTILE,
							2.5F, 0.40F);
					}
				}
			}

			if (entity instanceof Mob mob) {
				net.minecraft.world.damagesource.DamageSource source = event.getSource();
				if (isProjectileDamage(source) && tryDodgeProjectile(mob)) {
					event.setCanceled(true);
					return;
				}
			}
		}
	}

	private static boolean isProjectileDamage(net.minecraft.world.damagesource.DamageSource source) {
		Entity direct = source.getDirectEntity();
		return source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) || direct instanceof net.minecraft.world.entity.projectile.Projectile;
	}

	private static boolean tryDodgeProjectile(Mob mob) {
		CompoundTag mData = mob.getPersistentData();
		// Add cooldown to tryDodgeProjectile so it cannot trigger back-to-back endlessly
		if (persistentInt(mData, "dodge_cooldown", 0) > 0) {
			return false;
		}

		double mobX = mob.getX();
		double mobY = mob.getY();
		double mobZ = mob.getZ();
		net.minecraft.util.RandomSource random = mob.getRandom();

		for (int i = 0; i < 32; i++) {
			int dx = random.nextInt(17) - 8; // range -8 to 8
			int dz = random.nextInt(17) - 8; // range -8 to 8
			int dy = random.nextInt(5) - 2;   // range -2 to 2

			if (Math.abs(dx) < 3 && Math.abs(dz) < 3) {
				continue; // too close, try another
			}

			double targetX = Math.floor(mobX + dx) + 0.5;
			double targetY = Math.floor(mobY + dy);
			double targetZ = Math.floor(mobZ + dz);

			net.minecraft.core.BlockPos targetPos = net.minecraft.core.BlockPos.containing(targetX, targetY, targetZ);
			net.minecraft.core.BlockPos posBelow = targetPos.below();

			net.minecraft.world.level.block.state.BlockState stateBelow = mob.level().getBlockState(posBelow);
			if (stateBelow.isAir() || stateBelow.getCollisionShape(mob.level(), posBelow).isEmpty()) {
				continue;
			}

			boolean spaceClear = true;
			for (int h = 0; h < 6; h++) {
				net.minecraft.core.BlockPos checkHeight = targetPos.above(h);
				net.minecraft.world.level.block.state.BlockState stateAtH = mob.level().getBlockState(checkHeight);
				if (!stateAtH.getCollisionShape(mob.level(), checkHeight).isEmpty()) {
					spaceClear = false;
					break;
				}
			}

			if (!spaceClear) {
				continue;
			}

			// Found a valid grid coordinate!
			mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
				net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
				net.minecraft.sounds.SoundSource.HOSTILE,
				1.5F, 0.35F);

			if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, mob.getX(), mob.getY() + 3.0, mob.getZ(), 40, 0.5, 1.5, 0.5, 1.0);
			}

			mob.teleportTo(targetX, targetY, targetZ);
			
			// Reset current travel direction so it re-evaluates its grid route at the new position
			mData.putString("grid_dir", "NONE");
			mData.putInt("grid_cooldown", 0);
			mData.putInt("dodge_cooldown", 40); // 2-second dodge cooldown

			mob.level().playSound(null, targetX, targetY, targetZ,
				net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
				net.minecraft.sounds.SoundSource.HOSTILE,
				1.5F, 0.35F);

			if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, targetX, targetY + 3.0, targetZ, 40, 0.5, 1.5, 0.5, 1.0);
			}

			return true;
		}
		return false;
	}

	private static void setSyncedMouthState(Entity entity, int value) {
		entity.getEntityData().set(LignumPalusEntity.DATA_mouth_state, value);
	}

	private static int getSyncedMouthState(Entity entity) {
		return entity.getEntityData().get(LignumPalusEntity.DATA_mouth_state);
	}

	private static void applyTelekinesisPull(LivingEntity target, double targetHoldX, double targetHoldY, double targetHoldZ) {
		double pullX = targetHoldX - target.getX();
		double pullY = targetHoldY - target.getY();
		double pullZ = targetHoldZ - target.getZ();
		double dist = Math.sqrt(pullX * pullX + pullY * pullY + pullZ * pullZ);

		if (dist > 0.05) {
			double pullFactorXZ = 0.22;
			double pullFactorY = 0.08;
			double maxPull = 0.45;
			double velX = pullX * pullFactorXZ;
			double velY = pullY * pullFactorY;
			double velZ = pullZ * pullFactorXZ;
			double len = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
			if (len > maxPull) {
				velX = (velX / len) * maxPull;
				velY = (velY / len) * maxPull;
				velZ = (velZ / len) * maxPull;
			}
			if (velY > 0.12) {
				velY = 0.12;
			}
			target.setDeltaMovement(velX, velY, velZ);
		} else {
			target.setDeltaMovement(0, 0.01, 0);
		}

		target.fallDistance = 0.0F;
		target.hasImpulse = true;
	}

	private static Vec3 sanitizeHoldPosition(LevelAccessor world, double holdX, double holdY, double holdZ) {
		double minY = world.getMinBuildHeight() + 1;
		double clampedY = Math.max(minY, holdY);
		net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(holdX, clampedY, holdZ);

		int maxAdjust = 4;
		while (maxAdjust > 0 && !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
			clampedY += 1.0;
			pos = net.minecraft.core.BlockPos.containing(holdX, clampedY, holdZ);
			maxAdjust--;
		}

		net.minecraft.core.BlockPos below = pos.below();
		if (world.getFluidState(below).is(net.minecraft.tags.FluidTags.LAVA) || world.getBlockState(below).is(net.minecraft.world.level.block.Blocks.LAVA)) {
			clampedY += 1.5;
		}

		return new Vec3(holdX, clampedY, holdZ);
	}

	public static void execute() {
		// Empty signature to satisfy MCreator's call inside LignumPalusEntity when it has no parameters setup in MCreator GUI
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		if (!(entity instanceof Mob mob))
			return;

		if (handleRotAvoidanceAndTeleport(world, mob)) {
			return;
		}

		// Cleanup orphaned hypnotized tags if mob is removed or dead
		if (!mob.isAlive() || mob.isRemoved()) {
			clearAllHypnotizedTags(world, mob);
			return;
		}

		CompoundTag mData = mob.getPersistentData();
		boolean isClient = (world instanceof net.minecraft.world.level.Level level && level.isClientSide());

		// World Difficulty Scaling
		net.minecraft.world.Difficulty diff = (world instanceof net.minecraft.world.level.Level lvl) ? lvl.getDifficulty() : net.minecraft.world.Difficulty.NORMAL;
		int reqStareTicks = switch (diff) {
			case EASY -> 90;
			case HARD -> 50;
			default -> 70;
		};
		double scaledWanderSpeed = switch (diff) {
			case EASY -> 0.04;
			case HARD -> 0.07;
			default -> WANDER_SPEED;
		};
		double scaledCloseInSpeed = switch (diff) {
			case EASY -> 0.09;
			case HARD -> 0.16;
			default -> EXTREME_CLOSE_IN_SPEED;
		};

		// Decrement dodge cooldown
		int dodgeCd = persistentInt(mData, "dodge_cooldown", 0);
		if (dodgeCd > 0) {
			mData.putInt("dodge_cooldown", dodgeCd - 1);
		}

		double mobEyeY = mob.getY() + 5.2; // Based on 5.8 high bounding box

		// 1. Target selection: prefer player actively staring over nearest player by raw distance
		List<Player> players = world.getEntitiesOfClass(Player.class, new AABB(x - 48, y - 16, z - 48, x + 48, y + 16, z + 48));
		Player nearestPlayer = null;
		double bestScore = Double.MAX_VALUE;

		for (Player p : players) {
			if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
			double distSq = p.distanceToSqr(x, p.getY(), z);
			double dist = Math.sqrt(distSq);

			Vec3 lookVec = p.getLookAngle();
			Vec3 toMobVec = new Vec3(mob.getX() - p.getX(), mobEyeY - p.getEyeY(), mob.getZ() - p.getZ()).normalize();
			double dot = lookVec.dot(toMobVec);
			boolean isStaringAtMob = (dot > 0.82) && (dist <= 20.0) && mob.hasLineOfSight(p);

			// Staring players are prioritized over non-staring players
			double score = dist - (isStaringAtMob ? 1000.0 : 0.0);
			if (score < bestScore) {
				bestScore = score;
				nearestPlayer = p;
			}
		}

		boolean isPlayerWatching = false;
		if (nearestPlayer != null) {
			Vec3 lookVec = nearestPlayer.getLookAngle();
			Vec3 toMobVec = new Vec3(mob.getX() - nearestPlayer.getX(), mobEyeY - nearestPlayer.getEyeY(), mob.getZ() - nearestPlayer.getZ()).normalize();
			double dot = lookVec.dot(toMobVec);
			isPlayerWatching = (dot > 0.3) && mob.hasLineOfSight(nearestPlayer);
		}

		if (isClient) {
			int syncedMouth = getSyncedMouthState(mob);
			boolean isHypnotizing = (syncedMouth == 1 || syncedMouth == 2) && isPlayerWatching;
			if (isHypnotizing && nearestPlayer != null && nearestPlayer.isAlive() && !nearestPlayer.isCreative() && !nearestPlayer.isSpectator() && nearestPlayer.distanceTo(mob) <= 24.0) {
				// Lock player camera directly and helplessly to mob's eyes to force the staring gaze!
				double pDX = mob.getX() - nearestPlayer.getX();
				double pDY = mobEyeY - nearestPlayer.getEyeY();
				double pDZ = mob.getZ() - nearestPlayer.getZ();
				double r = Math.sqrt(pDX * pDX + pDZ * pDZ);
				float pYaw = (float) (Math.atan2(pDZ, pDX) * 180.0 / Math.PI) - 90.0F;
				float pPitch = (float) -(Math.atan2(pDY, r) * 180.0 / Math.PI);
				nearestPlayer.setYRot(pYaw);
				nearestPlayer.yRotO = pYaw;
				nearestPlayer.setXRot(pPitch);
				nearestPlayer.xRotO = pPitch;

				// Snap mob rotation to face player on client
				double dX = nearestPlayer.getX() - mob.getX();
				double dZ = nearestPlayer.getZ() - mob.getZ();
				float targetYawToPlayer = (float) (Math.atan2(dZ, dX) * 180.0 / Math.PI) - 90.0F;
				mob.setYRot(targetYawToPlayer);
				mob.yRotO = targetYawToPlayer;
				mob.setYBodyRot(targetYawToPlayer);
				mob.yBodyRotO = targetYawToPlayer;
				mob.setYHeadRot(targetYawToPlayer);
				mob.yHeadRotO = targetYawToPlayer;

				double holdDistance = 2.0;
				double targetHoldX = mob.getX() - Math.sin(Math.toRadians(targetYawToPlayer)) * holdDistance;
				double targetHoldY = mobEyeY - 1.30;
				double targetHoldZ = mob.getZ() + Math.cos(Math.toRadians(targetYawToPlayer)) * holdDistance;

				Vec3 safeHold = sanitizeHoldPosition(world, targetHoldX, targetHoldY, targetHoldZ);
				applyTelekinesisPull(nearestPlayer, safeHold.x, safeHold.y, safeHold.z);
			}
			return; // Client-side execution ends here!
		}

		// Handle hypnosis cooldown decrement
		int hypnosisCooldown = persistentInt(mData, "hypnosis_cooldown", 0);
		if (hypnosisCooldown > 0) {
			hypnosisCooldown--;
			mData.putInt("hypnosis_cooldown", hypnosisCooldown);
		}

		// 2. Track Player Stationary Tick Count
		boolean isPlayerStationary = false;
		if (nearestPlayer != null) {
			double lastX = persistentDouble(mData, "player_last_x", nearestPlayer.getX());
			double lastY = persistentDouble(mData, "player_last_y", nearestPlayer.getY());
			double lastZ = persistentDouble(mData, "player_last_z", nearestPlayer.getZ());
			int stationaryTicks = persistentInt(mData, "player_stationary_ticks", 0);

			double dx = nearestPlayer.getX() - lastX;
			double dy = nearestPlayer.getY() - lastY;
			double dz = nearestPlayer.getZ() - lastZ;
			double sqDistMoved = dx * dx + dy * dy + dz * dz;

			if (sqDistMoved < 0.005) {
				stationaryTicks++;
			} else {
				stationaryTicks = 0;
			}
			mData.putDouble("player_last_x", nearestPlayer.getX());
			mData.putDouble("player_last_y", nearestPlayer.getY());
			mData.putDouble("player_last_z", nearestPlayer.getZ());
			mData.putInt("player_stationary_ticks", stationaryTicks);

			if (stationaryTicks > STATIONARY_TICKS_THRESHOLD) {
				isPlayerStationary = true;
			}
		}

		// 3. Melee Bump Attack Execution
		if (nearestPlayer != null) {
			double distToPlayer = nearestPlayer.distanceTo(mob);
			if (distToPlayer < BUMP_ATTACK_RADIUS) {
				mob.swing(InteractionHand.MAIN_HAND);
				nearestPlayer.hurt(mob.damageSources().mobAttack(mob), 6.0F);
			}
		}

		double mobX = mob.getX();
		double mobZ = mob.getZ();
		double targetGridX = Math.floor(mobX) + 0.5;
		double targetGridZ = Math.floor(mobZ) + 0.5;

		// 3.6 Deadlights Gaze checking & Hypnosis Activation
		boolean isHypnotizing = persistentBoolean(mData, "is_hypnotizing", false);

		if (hypnosisCooldown <= 0 && !isHypnotizing && nearestPlayer != null && !nearestPlayer.isCreative() && !nearestPlayer.isSpectator()) {
			Vec3 lookVec = nearestPlayer.getLookAngle();
			Vec3 toMobVec = new Vec3(mob.getX() - nearestPlayer.getX(), mobEyeY - nearestPlayer.getEyeY(), mob.getZ() - nearestPlayer.getZ()).normalize();
			double dot = lookVec.dot(toMobVec);

			double distToPlayer = mob.distanceTo(nearestPlayer);
			if (dot > 0.82 && distToPlayer <= 20.0 && mob.hasLineOfSight(nearestPlayer)) {
				// Distance-weighted stare-tick accumulation
				int stareIncrement = distToPlayer <= 6.0 ? 2 : (distToPlayer >= 14.0 ? (mob.tickCount % 2 == 0 ? 1 : 0) : 1);
				int stareTicks = persistentInt(mData, "stare_ticks", 0) + stareIncrement;
				mData.putInt("stare_ticks", stareTicks);

				// Visual telegraph particles as stare_ticks approaches reqStareTicks
				if (world instanceof net.minecraft.server.level.ServerLevel serverLevel && mob.tickCount % 2 == 0) {
					double progress = (double) stareTicks / (double) reqStareTicks;
					int particleCount = (int) (2 + progress * 8);
					serverLevel.sendParticles(
						net.minecraft.core.particles.ParticleTypes.WITCH,
						mob.getX(), mobEyeY - 0.5, mob.getZ(),
						particleCount, 0.3, 0.4, 0.3, 0.05
					);
					if (progress > 0.4) {
						serverLevel.sendParticles(
							net.minecraft.core.particles.ParticleTypes.PORTAL,
							mob.getX(), mobEyeY, mob.getZ(),
							(int) (progress * 6), 0.2, 0.3, 0.2, 0.1
						);
					}
				}
				
				if (stareTicks >= reqStareTicks) {
					isHypnotizing = true;
					mData.putBoolean("is_hypnotizing", true);
					mData.putBoolean("isHypnotizing", true);
					mData.putInt("stare_ticks", 0);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
						net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER,
						net.minecraft.sounds.SoundSource.HOSTILE,
						2.0F, 0.35F);
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
						net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN,
						net.minecraft.sounds.SoundSource.HOSTILE,
						2.5F, 0.40F);
				}
			} else {
				int stareTicks = persistentInt(mData, "stare_ticks", 0);
				if (stareTicks > 0) {
					mData.putInt("stare_ticks", stareTicks - 1);
				}
			}
		}

		// --- Mouth State Machine for Hypnosis ---
		int mouthState = persistentInt(mData, "mouth_state", 0);
		int mouthTimer = persistentInt(mData, "mouth_timer", 0);

		if (isHypnotizing) {
			if (mouthState == 0 || mouthState == 3) {
				mouthState = 1;
				mouthTimer = 50;
			} else if (mouthState == 1) {
				if (mouthTimer > 0) {
					mouthTimer--;
				}
				if (mouthTimer <= 0) {
					mouthState = 2;
				}
			}
		} else {
			if (mouthState == 2 || mouthState == 1) {
				mouthState = 3;
				mouthTimer = 50;
			} else if (mouthState == 3) {
				if (mouthTimer > 0) {
					mouthTimer--;
				}
				if (mouthTimer <= 0) {
					mouthState = 0;
				}
			}
		}

		mData.putInt("mouth_state", mouthState);
		mData.putInt("mouth_timer", mouthTimer);
		setSyncedMouthState(mob, mouthState);
		// ----------------------------------------

		// 3.7 Deadlights Hypnosis Active State Loop
		if (isHypnotizing) {
			if (persistentBoolean(mData, "hypnotizing_non_players", false)) {
				List<LivingEntity> potentialTargets = world.getEntitiesOfClass(LivingEntity.class, new AABB(x - 32, y - 16, z - 32, x + 32, y + 16, z + 32));
				List<LivingEntity> hypnotizedEntities = new java.util.ArrayList<>();
				for (LivingEntity le : potentialTargets) {
					if (le.isAlive() && isHypnotizedBy(le, mob.getStringUUID())) {
						hypnotizedEntities.add(le);
					}
				}

				if (hypnotizedEntities.isEmpty()) {
					isHypnotizing = false;
					mData.putBoolean("is_hypnotizing", false);
					mData.putBoolean("isHypnotizing", false);
					mData.putBoolean("hypnotizing_non_players", false);
					mData.putInt("stare_ticks", 0);
					mData.putInt("hypnosis_ticks", 0);
					mData.putInt("hypnosis_cooldown", 100);
					mData.remove("hypnosis_yaw");
				} else {
					int hypnosisTicks = persistentInt(mData, "hypnosis_ticks", 0) + 1;
					mData.putInt("hypnosis_ticks", hypnosisTicks);

					mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
					if (mob.distanceToSqr(targetGridX, mob.getY(), targetGridZ) > 0.002) {
						mob.teleportTo(targetGridX, mob.getY(), targetGridZ);
					}

					hypnotizedEntities.sort(java.util.Comparator.comparing(Entity::getStringUUID));

					double avgX = 0;
					double avgY = 0;
					double avgZ = 0;
					for (LivingEntity le : hypnotizedEntities) {
						avgX += le.getX();
						avgY += le.getEyeY();
						avgZ += le.getZ();
					}
					avgX /= hypnotizedEntities.size();
					avgY /= hypnotizedEntities.size();
					avgZ /= hypnotizedEntities.size();

					double dX = avgX - mob.getX();
					double dY = avgY - mobEyeY;
					double dZ = avgZ - mob.getZ();
					float targetYawToEntities = (float) (Math.atan2(dZ, dX) * 180.0 / Math.PI) - 90.0F;

					float currentHypnosisYaw = persistentFloat(mData, "hypnosis_yaw", mob.getYRot());
					float nextHypnosisYaw = currentHypnosisYaw + net.minecraft.util.Mth.wrapDegrees(targetYawToEntities - currentHypnosisYaw) * 0.1F;
					mData.putFloat("hypnosis_yaw", nextHypnosisYaw);

					mob.setYRot(nextHypnosisYaw);
					mob.yRotO = nextHypnosisYaw;
					mob.setYBodyRot(nextHypnosisYaw);
					mob.yBodyRotO = nextHypnosisYaw;
					mob.setYHeadRot(nextHypnosisYaw);
					mob.yHeadRotO = nextHypnosisYaw;
					mob.getLookControl().setLookAt(avgX, avgY, avgZ, 10.0F, 10.0F);

					int N = hypnotizedEntities.size();
					double holdDistance = 3.0;
					double rad = Math.toRadians(nextHypnosisYaw);
					double rightX = Math.cos(rad);
					double rightZ = Math.sin(rad);

					for (int i = 0; i < N; i++) {
						LivingEntity le = hypnotizedEntities.get(i);

						// Width-based spacing and overlap avoidance
						double widthSpacing = Math.max(1.4, le.getBbWidth() + 0.5);
						double offset = (i - (N - 1) / 2.0) * widthSpacing;
						double targetHoldX = mob.getX() - Math.sin(rad) * holdDistance + rightX * offset;
						double targetHoldY = mobEyeY - 1.30;
						double targetHoldZ = mob.getZ() + Math.cos(rad) * holdDistance + rightZ * offset;

						Vec3 safeHold = sanitizeHoldPosition(world, targetHoldX, targetHoldY, targetHoldZ);

						double eDX = mob.getX() - le.getX();
						double eDY = mobEyeY - le.getEyeY();
						double eDZ = mob.getZ() - le.getZ();
						double r = Math.sqrt(eDX * eDX + eDZ * eDZ);
						float eYaw = (float) (Math.atan2(eDZ, eDX) * 180.0 / Math.PI) - 90.0F;
						float ePitch = (float) -(Math.atan2(eDY, r) * 180.0 / Math.PI);
						le.setYRot(eYaw);
						le.yRotO = eYaw;
						le.setXRot(ePitch);
						le.xRotO = ePitch;

						applyTelekinesisPull(le, safeHold.x, safeHold.y, safeHold.z);

						// Periodically apply debuffs rather than every single tick
						if (hypnosisTicks >= 40 && (hypnosisTicks - 40) % 30 == 0) {
							int slownessAmp = switch (diff) { case EASY -> 0; case HARD -> 3; default -> 2; };
							int witherAmp = switch (diff) { case EASY -> 0; case HARD -> 2; default -> 1; };
							int dur = switch (diff) { case EASY -> 100; case HARD -> 180; default -> 140; };

							le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
								net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, dur, slownessAmp, false, false, true));
							le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
								net.minecraft.world.effect.MobEffects.DARKNESS, dur, 0, false, false, true));
							le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
								net.minecraft.world.effect.MobEffects.WITHER, dur, witherAmp, false, false, true));
						}
					}

					if (mob.tickCount % 30 == 0) {
						mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
							net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT,
							net.minecraft.sounds.SoundSource.HOSTILE,
							1.5F, 0.35F);
					}
					if (mob.tickCount % 25 == 0) {
						mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
							net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
							net.minecraft.sounds.SoundSource.HOSTILE,
							5.0F, 0.75F);
					}

					mob.getNavigation().stop();
					return;
				}
			} else {
				if (nearestPlayer == null || !nearestPlayer.isAlive() || nearestPlayer.isCreative() || nearestPlayer.isSpectator() || nearestPlayer.distanceTo(mob) > 24.0) {
					// Cancel hypnosis if player is invalid or out of range
					isHypnotizing = false;
					mData.putBoolean("is_hypnotizing", false);
					mData.putBoolean("isHypnotizing", false);
					mData.putInt("stare_ticks", 0);
					mData.putInt("hypnosis_ticks", 0);
					mData.putInt("hypnosis_cooldown", 100);
				} else {
					int hypnosisTicks = persistentInt(mData, "hypnosis_ticks", 0) + 1;
					mData.putInt("hypnosis_ticks", hypnosisTicks);

					mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
					if (mob.distanceToSqr(targetGridX, mob.getY(), targetGridZ) > 0.002) {
						mob.teleportTo(targetGridX, mob.getY(), targetGridZ);
					}

					double dX = nearestPlayer.getX() - mob.getX();
					double dY = nearestPlayer.getEyeY() - mobEyeY;
					double dZ = nearestPlayer.getZ() - mob.getZ();
					float targetYawToPlayer = (float) (Math.atan2(dZ, dX) * 180.0 / Math.PI) - 90.0F;

					mob.setYRot(targetYawToPlayer);
					mob.yRotO = targetYawToPlayer;
					mob.setYBodyRot(targetYawToPlayer);
					mob.yBodyRotO = targetYawToPlayer;
					mob.setYHeadRot(targetYawToPlayer);
					mob.yHeadRotO = targetYawToPlayer;
					mob.getLookControl().setLookAt(nearestPlayer, 100.0F, 100.0F);

					double pDX = mob.getX() - nearestPlayer.getX();
					double pDY = mobEyeY - nearestPlayer.getEyeY();
					double pDZ = mob.getZ() - nearestPlayer.getZ();
					double r = Math.sqrt(pDX * pDX + pDZ * pDZ);
					float pYaw = (float) (Math.atan2(pDZ, pDX) * 180.0 / Math.PI) - 90.0F;
					float pPitch = (float) -(Math.atan2(pDY, r) * 180.0 / Math.PI);
					nearestPlayer.setYRot(pYaw);
					nearestPlayer.yRotO = pYaw;
					nearestPlayer.setXRot(pPitch);
					nearestPlayer.xRotO = pPitch;

					double holdDistance = 2.0;
					double targetHoldX = mob.getX() - Math.sin(Math.toRadians(targetYawToPlayer)) * holdDistance;
					double targetHoldY = mobEyeY - 1.30;
					double targetHoldZ = mob.getZ() + Math.cos(Math.toRadians(targetYawToPlayer)) * holdDistance;

					Vec3 safeHold = sanitizeHoldPosition(world, targetHoldX, targetHoldY, targetHoldZ);
					applyTelekinesisPull(nearestPlayer, safeHold.x, safeHold.y, safeHold.z);

					// Periodically apply debuffs rather than every single tick
					if (hypnosisTicks >= 40 && (hypnosisTicks - 40) % 30 == 0) {
						int slownessAmp = switch (diff) { case EASY -> 0; case HARD -> 3; default -> 2; };
						int witherAmp = switch (diff) { case EASY -> 0; case HARD -> 2; default -> 1; };
						int dur = switch (diff) { case EASY -> 100; case HARD -> 180; default -> 140; };

						nearestPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
							net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, dur, slownessAmp, false, false, true));
						nearestPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
							net.minecraft.world.effect.MobEffects.DARKNESS, dur, 0, false, false, true));
						nearestPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
							net.minecraft.world.effect.MobEffects.WITHER, dur, witherAmp, false, false, true));
					}

					if (mob.tickCount % 30 == 0) {
						mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
							net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT,
							net.minecraft.sounds.SoundSource.HOSTILE,
							1.5F, 0.35F);
					}

					if (mob.tickCount % 25 == 0) {
						mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
							net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
							net.minecraft.sounds.SoundSource.HOSTILE,
							5.0F, 0.75F);
					}

					mob.getNavigation().stop();
					return;
				}
			}
		}

		mData.putBoolean("is_hypnotizing", isHypnotizing);
		mData.putBoolean("isHypnotizing", isHypnotizing);

		// 4. Handle Blending / Frozen Disguise State
		boolean shouldFreeze = !isPlayerStationary && isPlayerWatching;
		boolean isClosingIn = (nearestPlayer != null) && (isPlayerStationary || !isPlayerWatching);

		if (shouldFreeze) {
			mob.setTarget(null);
			mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
			if (mob.distanceToSqr(targetGridX, mob.getY(), targetGridZ) > 0.002) {
				mob.teleportTo(targetGridX, mob.getY(), targetGridZ);
			}
			
			float yaw = persistentFloat(mData, "grid_yaw", 0.0F);
			if (!mData.contains("grid_yaw")) {
				float randomYaw = (int)(Math.random() * 4) * 90.0F;
				mData.putFloat("grid_yaw", randomYaw);
				yaw = randomYaw;
			}
			mob.setYRot(yaw);
			mob.yRotO = yaw;
			mob.setYHeadRot(yaw);
			mob.yHeadRotO = yaw;
			mob.setYBodyRot(yaw);
			mob.yBodyRotO = yaw;

			double lookX = mob.getX() - Math.sin(Math.toRadians(yaw)) * 5.0;
			double lookZ = mob.getZ() + Math.cos(Math.toRadians(yaw)) * 5.0;
			mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 30.0F, 30.0F);

			mob.getNavigation().stop();
			return;
		}

		int pauseTicks = persistentInt(mData, "grid_pause_ticks", 0);

		String currentDir = persistentString(mData, "grid_dir", "NONE");
		boolean isMoving = !currentDir.equals("NONE") && pauseTicks == 0;
		boolean isStuck = mob.horizontalCollision;

		double lastTrackedX = persistentDouble(mData, "grid_last_x", mobX);
		double lastTrackedZ = persistentDouble(mData, "grid_last_z", mobZ);
		double distMovedThisTick = Math.sqrt((mobX - lastTrackedX) * (mobX - lastTrackedX) + (mobZ - lastTrackedZ) * (mobZ - lastTrackedZ));

		if (isMoving && distMovedThisTick < 0.005) {
			int consecutiveStuck = persistentInt(mData, "grid_consecutive_stuck", 0) + 1;
			mData.putInt("grid_consecutive_stuck", consecutiveStuck);
			if (consecutiveStuck > 5) {
				isStuck = true;
			}
		} else {
			mData.putInt("grid_consecutive_stuck", 0);
		}

		mData.putDouble("grid_last_x", mobX);
		mData.putDouble("grid_last_z", mobZ);

		int gridCooldown = persistentInt(mData, "grid_cooldown", 0);
		if (isStuck && isMoving) {
			currentDir = "NONE";
			gridCooldown = 0;
			mData.putString("grid_dir", "NONE");
			mData.putInt("grid_cooldown", 0);
			if (mob.distanceToSqr(targetGridX, mob.getY(), targetGridZ) > 0.002) {
				mob.teleportTo(targetGridX, mob.getY(), targetGridZ);
			}
		}

		if (pauseTicks > 0) {
			mData.putInt("grid_pause_ticks", pauseTicks - 1);
			
			mob.setTarget(null);
			mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
			if (mob.distanceToSqr(targetGridX, mob.getY(), targetGridZ) > 0.002) {
				mob.teleportTo(targetGridX, mob.getY(), targetGridZ);
			}
			
			float yaw = persistentFloat(mData, "grid_yaw", 0.0F);
			mob.setYRot(yaw);
			mob.yRotO = yaw;
			mob.setYHeadRot(yaw);
			mob.yHeadRotO = yaw;
			mob.setYBodyRot(yaw);
			mob.yBodyRotO = yaw;

			double lookX = mob.getX() - Math.sin(Math.toRadians(yaw)) * 5.0;
			double lookZ = mob.getZ() + Math.cos(Math.toRadians(yaw)) * 5.0;
			mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 30.0F, 30.0F);

			mob.getNavigation().stop();
			return;
		}

		// 5. Grid Travel Navigation Engine
		if (gridCooldown > 0) {
			mData.putInt("grid_cooldown", gridCooldown - 1);
		}

		boolean isAtIntersection = Math.abs(mobX - targetGridX) < 0.15 && Math.abs(mobZ - targetGridZ) < 0.15;

		if ((isAtIntersection && gridCooldown == 0) || currentDir.equals("NONE")) {
			if (mob.distanceToSqr(targetGridX, mob.getY(), targetGridZ) > 0.002) {
				mob.teleportTo(targetGridX, mob.getY(), targetGridZ);
			}

			if (!currentDir.equals("NONE") && Math.random() < PAUSE_CHANCE_O_GRID) {
				int ticksToPause = 60 + (int) (Math.random() * 120);
				mData.putInt("grid_pause_ticks", ticksToPause);
				
				mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
				
				float randomYaw = (int)(Math.random() * 4) * 90.0F;
				mData.putFloat("grid_yaw", randomYaw);
				mob.setYRot(randomYaw);
				mob.yRotO = randomYaw;
				mob.setYHeadRot(randomYaw);
				mob.yHeadRotO = randomYaw;
				mob.setYBodyRot(randomYaw);
				mob.yBodyRotO = randomYaw;

				double lookX = mob.getX() - Math.sin(Math.toRadians(randomYaw)) * 5.0;
				double lookZ = mob.getZ() + Math.cos(Math.toRadians(randomYaw)) * 5.0;
				mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 30.0F, 30.0F);
				
				mob.getNavigation().stop();
				return;
			}

			String nextDir = "NONE";
			float targetYaw = 0.0F;

			if (isClosingIn) {
				double dX = nearestPlayer.getX() - mobX;
				double dZ = nearestPlayer.getZ() - mobZ;

				String primaryDir = Math.abs(dX) > Math.abs(dZ) ? (dX > 0 ? "EAST" : "WEST") : (dZ > 0 ? "SOUTH" : "NORTH");
				String secondaryDir = Math.abs(dX) > Math.abs(dZ) ? (dZ > 0 ? "SOUTH" : "NORTH") : (dX > 0 ? "EAST" : "WEST");

				// Direction-commitment inertia: preserve currentDir if unblocked and valid
				if (!currentDir.equals("NONE") && (currentDir.equals(primaryDir) || currentDir.equals(secondaryDir)) && !isDirectionBlocked(world, targetGridX, mob.getY(), targetGridZ, currentDir)) {
					nextDir = currentDir;
				} else if (!isDirectionBlocked(world, targetGridX, mob.getY(), targetGridZ, primaryDir)) {
					nextDir = primaryDir;
				} else if (!isDirectionBlocked(world, targetGridX, mob.getY(), targetGridZ, secondaryDir)) {
					nextDir = secondaryDir;
				} else {
					java.util.List<String> okDirs = new java.util.ArrayList<>();
					for (String d : new String[]{"NORTH", "SOUTH", "EAST", "WEST"}) {
						if (!isDirectionBlocked(world, targetGridX, mob.getY(), targetGridZ, d)) {
							okDirs.add(d);
						}
					}
					if (!okDirs.isEmpty()) {
						nextDir = okDirs.get((int) (Math.random() * okDirs.size()));
					} else {
						nextDir = primaryDir;
					}
				}
			} else {
				java.util.List<String> okDirs = new java.util.ArrayList<>();
				for (String d : new String[]{"NORTH", "SOUTH", "EAST", "WEST"}) {
					if (!isDirectionBlocked(world, targetGridX, mob.getY(), targetGridZ, d)) {
						okDirs.add(d);
					}
				}

				if (!okDirs.isEmpty()) {
					double rand = Math.random();
					if (currentDir.equals("NONE") || rand < 0.20) {
						nextDir = okDirs.get((int) (Math.random() * okDirs.size()));
					} else {
						String forwardDir = currentDir;
						String leftDir = "NONE";
						String rightDir = "NONE";
						if (currentDir.equals("NORTH") || currentDir.equals("SOUTH")) {
							leftDir = "EAST"; rightDir = "WEST";
						} else {
							leftDir = "NORTH"; rightDir = "SOUTH";
						}

						if (rand >= 0.55 && okDirs.contains(forwardDir)) {
							nextDir = forwardDir;
						} else {
							java.util.List<String> turns = new java.util.ArrayList<>();
							if (okDirs.contains(leftDir)) turns.add(leftDir);
							if (okDirs.contains(rightDir)) turns.add(rightDir);

							if (!turns.isEmpty()) {
								nextDir = turns.get((int) (Math.random() * turns.size()));
							} else if (okDirs.contains(forwardDir)) {
								nextDir = forwardDir;
							} else {
								nextDir = okDirs.get((int) (Math.random() * okDirs.size()));
							}
						}
					}
				} else {
					nextDir = currentDir.equals("NONE") ? "NORTH" : currentDir;
				}
			}

			currentDir = nextDir;
			mData.putString("grid_dir", currentDir);
			mData.putInt("grid_cooldown", 10);

			if (currentDir.equals("EAST") || currentDir.equals("WEST")) {
				targetYaw = currentDir.equals("EAST") ? -90.0F : 90.0F;
			} else if (currentDir.equals("NORTH") || currentDir.equals("SOUTH")) {
				targetYaw = currentDir.equals("NORTH") ? 180.0F : 0.0F;
			}
			mData.putFloat("grid_yaw", targetYaw);
		}

		// 6. Set Movement Velocity Vectors and Rotations
		double currentSpeed = isPlayerStationary ? scaledCloseInSpeed : scaledWanderSpeed;
		float targetYaw = persistentFloat(mData, "grid_yaw", 0.0F);

		switch (currentDir) {
			case "NORTH" -> targetYaw = 180.0F;
			case "SOUTH" -> targetYaw = 0.0F;
			case "EAST"  -> targetYaw = -90.0F;
			case "WEST"  -> targetYaw = 90.0F;
		}

		float entityYaw = mob.getYRot();
		float diffYaw = net.minecraft.util.Mth.wrapDegrees(targetYaw - entityYaw);
		float nextYaw = entityYaw + diffYaw * 0.25F;

		mob.setYRot(nextYaw);
		mob.yRotO = nextYaw;
		mob.setYBodyRot(nextYaw);
		mob.yBodyRotO = nextYaw;
		mob.setYHeadRot(nextYaw);
		mob.yHeadRotO = nextYaw;

		double lookX = mob.getX() - Math.sin(Math.toRadians(nextYaw)) * 5.0;
		double lookZ = mob.getZ() + Math.cos(Math.toRadians(nextYaw)) * 5.0;
		mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 30.0F, 30.0F);

		double velX = 0;
		double velZ = 0;
		switch (currentDir) {
			case "NORTH" -> velZ = -currentSpeed;
			case "SOUTH" -> velZ = currentSpeed;
			case "EAST"  -> velX = currentSpeed;
			case "WEST"  -> velX = -currentSpeed;
		}

		double alignedX = mob.getX();
		double alignedZ = mob.getZ();
		if (currentDir.equals("NORTH") || currentDir.equals("SOUTH")) {
			double diffX = targetGridX - alignedX;
			if (Math.abs(diffX) > 0.01) {
				alignedX = alignedX + diffX * 0.15;
			}
		} else if (currentDir.equals("EAST") || currentDir.equals("WEST")) {
			double diffZ = targetGridZ - alignedZ;
			if (Math.abs(diffZ) > 0.01) {
				alignedZ = alignedZ + diffZ * 0.15;
			}
		}
		if (Math.abs(alignedX - mob.getX()) > 0.01 || Math.abs(alignedZ - mob.getZ()) > 0.01) {
			mob.teleportTo(alignedX, mob.getY(), alignedZ);
		}

		mob.setDeltaMovement(velX, mob.getDeltaMovement().y, velZ);
		mob.getNavigation().stop();
	}

	private static boolean isDirectionBlocked(LevelAccessor world, double currentX, double currentY, double currentZ, String dir) {
		int dx = 0;
		int dz = 0;
		switch (dir) {
			case "NORTH" -> dz = -1;
			case "SOUTH" -> dz = 1;
			case "EAST"  -> dx = 1;
			case "WEST"  -> dx = -1;
		}
		double feetY = Math.floor(currentY + 0.1);
		net.minecraft.core.BlockPos basePos = net.minecraft.core.BlockPos.containing(currentX + dx, feetY, currentZ + dz);

		// Check vertical clearance across entity's full 6-block height
		for (int h = 0; h < 6; h++) {
			net.minecraft.core.BlockPos checkPos = basePos.above(h);
			if (!world.getBlockState(checkPos).getCollisionShape(world, checkPos).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isHypnotizedBy(LivingEntity le, String uuid) {
		return le.getPersistentData().contains("palus_hypnotized_by") && le.getPersistentData().getString("palus_hypnotized_by").equals(uuid);
	}

	private static void setHypnotizedTag(LivingEntity le, String uuid) {
		le.getPersistentData().putString("palus_hypnotized_by", uuid);
	}

	private static void removeHypnotizedTag(LivingEntity le) {
		le.getPersistentData().remove("palus_hypnotized_by");
	}

	private static void clearAllHypnotizedTags(LevelAccessor world, Entity palus) {
		String myUUID = palus.getStringUUID();
		double x = palus.getX();
		double y = palus.getY();
		double z = palus.getZ();
		List<LivingEntity> potentialTargets = world.getEntitiesOfClass(LivingEntity.class, new AABB(x - 32, y - 16, z - 32, x + 32, y + 16, z + 32));
		for (LivingEntity le : potentialTargets) {
			if (isHypnotizedBy(le, myUUID)) {
				removeHypnotizedTag(le);
			}
		}
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

	private static int getInt(Entity e, String key, int fallback) {
		return e.getPersistentData().contains(key) ? e.getPersistentData().getInt(key) : fallback;
	}

	private static boolean getBool(Entity e, String key, boolean fallback) {
		return e.getPersistentData().contains(key) ? e.getPersistentData().getBoolean(key) : fallback;
	}

	private static void putInt(Entity e, String key, int value) {
		e.getPersistentData().putInt(key, value);
	}

	private static void putBool(Entity e, String key, boolean value) {
		e.getPersistentData().putBoolean(key, value);
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
	} // 1.21.1
}
