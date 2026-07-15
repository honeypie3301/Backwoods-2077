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

import java.util.List;
import java.util.Optional;

@EventBusSubscriber
public class LignumPalusOnEntityTickUpdateProcedure {
	// Configuration Constants
	private static final double WANDER_SPEED = 0.05;              // Extremely slow, eerie pace
	private static final double EXTREME_CLOSE_IN_SPEED = 0.12;    // Closes in steadily
	private static final double BUMP_ATTACK_RADIUS = 1.20;        // Hitbox collision threshold
	private static final int STATIONARY_TICKS_THRESHOLD = 100;    // 5 seconds threshold of standing still
	private static final double PAUSE_CHANCE_O_GRID = 0.50;       // 50% chance to pause at any block center and pretend to be a pillar
	private static final int REQUIRED_STARE_TICKS = 70;           // Ticks required to stare directly before getting telekinetically lifted

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

			if (attacker instanceof LivingEntity livingAttacker) {
				if (livingAttacker instanceof Player player) {
					if (persistentBoolean(mData, "is_hypnotizing", false)) {
						mData.putBoolean("is_hypnotizing", false);
						mData.putBoolean("isHypnotizing", false);
						mData.putBoolean("hypnotizing_non_players", false);
						mData.putInt("stare_ticks", 0);
						mData.putInt("hypnosis_ticks", 0);
						// Cooldown of 5 seconds (100 ticks) so it does not immediately re-hypnotize the player when taking damage
						mData.putInt("hypnosis_cooldown", 100);

						// Clear hypnosis tag from any nearby entities that were hypnotized by this Palus
						String myUUID = entity.getStringUUID();
						double x = entity.getX();
						double y = entity.getY();
						double z = entity.getZ();
						List<LivingEntity> potentialTargets = entity.level().getEntitiesOfClass(LivingEntity.class, new AABB(x - 32, y - 16, z - 32, x + 32, y + 16, z + 32));
						for (LivingEntity le : potentialTargets) {
							if (le.getPersistentData().getString("palus_hypnotized_by").equals(myUUID)) {
								le.getPersistentData().remove("palus_hypnotized_by");
							}
						}
					}
				} else {
					livingAttacker.getPersistentData().putString("palus_hypnotized_by", entity.getStringUUID());
					
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
		double mobX = mob.getX();
		double mobY = mob.getY();
		double mobZ = mob.getZ();
		CompoundTag mData = mob.getPersistentData();
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
				1.5F, 0.35F); // Lower pitch of the teleporting sound

			if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, mob.getX(), mob.getY() + 3.0, mob.getZ(), 40, 0.5, 1.5, 0.5, 1.0);
			}

			mob.teleportTo(targetX, targetY, targetZ);
			
			// Reset current travel direction so it re-evaluates its grid route at the new position
			mData.putString("grid_dir", "NONE");
			mData.putInt("grid_cooldown", 0);

			mob.level().playSound(null, targetX, targetY, targetZ,
				net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
				net.minecraft.sounds.SoundSource.HOSTILE,
				1.5F, 0.35F); // Lower pitch of the teleporting sound

			if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, targetX, targetY + 3.0, targetZ, 40, 0.5, 1.5, 0.5, 1.0);
			}

			return true;
		}
		return false;
	}

	private static void setSyncedMouthState(Entity entity, int value) {
		try {
			Class<?> clazz = entity.getClass();
			java.lang.reflect.Field field = null;
			for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
				if (f.getName().equalsIgnoreCase("DATA_mouth_state") || f.getName().equalsIgnoreCase("DATA_mouthState")) {
					field = f;
					break;
				}
			}
			if (field != null) {
				field.setAccessible(true);
				net.minecraft.network.syncher.EntityDataAccessor<Integer> accessor = 
					(net.minecraft.network.syncher.EntityDataAccessor<Integer>) field.get(null);
				entity.getEntityData().set(accessor, value);
			}
		} catch (Exception e) {
			// Fail silently
		}
	}

	private static int getSyncedMouthState(Entity entity) {
		try {
			Class<?> clazz = entity.getClass();
			java.lang.reflect.Field field = null;
			for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
				if (f.getName().equalsIgnoreCase("DATA_mouth_state") || f.getName().equalsIgnoreCase("DATA_mouthState")) {
					field = f;
					break;
				}
			}
			if (field != null) {
				field.setAccessible(true);
				net.minecraft.network.syncher.EntityDataAccessor<Integer> accessor = 
					(net.minecraft.network.syncher.EntityDataAccessor<Integer>) field.get(null);
				return entity.getEntityData().get(accessor);
			}
		} catch (Exception e) {
			// Fail silently
		}
		return 0;
	}

	public static void execute() {
		// Empty signature to satisfy MCreator's call inside LignumPalusEntity when it has no parameters setup in MCreator GUI
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		if (!(entity instanceof Mob mob))
			return;

		CompoundTag mData = mob.getPersistentData();
		boolean isClient = (world instanceof net.minecraft.world.level.Level level && level.isClientSide());

		// 1. Find nearest player (required for both client-side lifting and server-side AI)
		List<Player> players = world.getEntitiesOfClass(Player.class, new AABB(x - 48, y - 16, z - 48, x + 48, y + 16, z + 48));
		Player nearestPlayer = null;
		double minDistance = Double.MAX_VALUE;
		for (Player p : players) {
			double dist = p.distanceToSqr(x, p.getY(), z);
			if (dist < minDistance) {
				minDistance = dist;
				nearestPlayer = p;
			}
		}

		double mobEyeY = mob.getY() + 5.2; // Based on 5.8 high bounding box
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

				// Smooth telekinetic lift: Pull and hold player horizontally in front of mob's face, slightly lowered
				double holdDistance = 2.0; // Suspended 2 blocks away as requested
				double targetHoldX = mob.getX() - Math.sin(Math.toRadians(targetYawToPlayer)) * holdDistance;
				double targetHoldY = mobEyeY - 1.30; // Suspended slightly lower for ideal direct stare angle
				double targetHoldZ = mob.getZ() + Math.cos(Math.toRadians(targetYawToPlayer)) * holdDistance;

				double pullX = targetHoldX - nearestPlayer.getX();
				double pullY = targetHoldY - nearestPlayer.getY();
				double pullZ = targetHoldZ - nearestPlayer.getZ();
				double dist = Math.sqrt(pullX * pullX + pullY * pullY + pullZ * pullZ);

				if (dist > 0.05) {
					double pullFactorXZ = 0.22;
					double pullFactorY = 0.08; // Slower lifting for a creepy, dramatic levitation effect
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
					// Cap rising velocity to make lifting slower
					if (velY > 0.12) {
						velY = 0.12;
					}
					nearestPlayer.setDeltaMovement(velX, velY, velZ);
				} else {
					nearestPlayer.setDeltaMovement(0, 0.01, 0);
				}

				nearestPlayer.fallDistance = 0.0F;
				nearestPlayer.hasImpulse = true;
			}
			return; // Client-side execution ends here!
		}

		// Handle hypnosis cooldown decrement
		int hypnosisCooldown = persistentInt(mData, "hypnosis_cooldown", 0);
		if (hypnosisCooldown > 0) {
			hypnosisCooldown--;
			mData.putInt("hypnosis_cooldown", hypnosisCooldown);
		}

		// 2. Track Player Stationary Tick Count with persistent tags in custom mob data
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

			// If player moves barely anything we flag them as standing still nearby
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

		// 3. Melee Bump Attack Execution (Attacks immediately if bumped)
		if (nearestPlayer != null) {
			double distToPlayer = nearestPlayer.distanceTo(mob);
			if (distToPlayer < BUMP_ATTACK_RADIUS) {
				mob.swing(InteractionHand.MAIN_HAND);
				nearestPlayer.hurt(mob.damageSources().mobAttack(mob), 6.0F); // Deals 3 hearts of structural impact damage
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

			// Under 20 blocks, staring directly into entity's eye area (gaze dot > 0.82) with unobstructed line of sight
			if (dot > 0.82 && mob.distanceTo(nearestPlayer) <= 20.0 && mob.hasLineOfSight(nearestPlayer)) {
				int stareTicks = persistentInt(mData, "stare_ticks", 0) + 1;
				mData.putInt("stare_ticks", stareTicks);
				
				// Read duration from config constant, allowing dynamic NBT override if wanted
				int reqTicks = persistentInt(mData, "required_stare_ticks", REQUIRED_STARE_TICKS);
				
				if (stareTicks >= reqTicks) {
					isHypnotizing = true;
					mData.putBoolean("is_hypnotizing", true);
					mData.putBoolean("isHypnotizing", true);
					mData.putInt("stare_ticks", 0);
					// Play intensive portal trigger sound on transition - loud and low pitch!
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
						net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER,
						net.minecraft.sounds.SoundSource.HOSTILE,
						2.0F, 0.35F);
					// Play deep creepy distorted End Portal activation sound once
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
				// Start opening the mouth (plays open_mouth animation, 2.5 seconds = 50 ticks)
				mouthState = 1;
				mouthTimer = 50;
			} else if (mouthState == 1) {
				if (mouthTimer > 0) {
					mouthTimer--;
				}
				if (mouthTimer <= 0) {
					mouthState = 2; // Transition to fully open / hold position
				}
			}
		} else {
			if (mouthState == 2 || mouthState == 1) {
				// Start closing the mouth (plays close_mouth animation, 2.5 seconds = 50 ticks)
				mouthState = 3;
				mouthTimer = 50;
			} else if (mouthState == 3) {
				if (mouthTimer > 0) {
					mouthTimer--;
				}
				if (mouthTimer <= 0) {
					mouthState = 0; // Transition to fully closed
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
					if (le.isAlive() && le.getPersistentData().getString("palus_hypnotized_by").equals(mob.getStringUUID())) {
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
					double spacing = 1.2;

					for (int i = 0; i < N; i++) {
						LivingEntity le = hypnotizedEntities.get(i);

						double offset = (i - (N - 1) / 2.0) * spacing;
						double targetHoldX = mob.getX() - Math.sin(rad) * holdDistance + rightX * offset;
						double targetHoldY = mobEyeY - 1.30;
						double targetHoldZ = mob.getZ() + Math.cos(rad) * holdDistance + rightZ * offset;

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

						double pullX = targetHoldX - le.getX();
						double pullY = targetHoldY - le.getY();
						double pullZ = targetHoldZ - le.getZ();
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
							le.setDeltaMovement(velX, velY, velZ);
						} else {
							le.setDeltaMovement(0, 0.01, 0);
						}

						le.fallDistance = 0.0F;
						le.hasImpulse = true;

						if (hypnosisTicks >= 40) {
							le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
								net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
								140,
								2,
								false,
								false,
								true
							));
							le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
								net.minecraft.world.effect.MobEffects.DARKNESS,
								140,
								0,
								false,
								false,
								true
							));
							le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
								net.minecraft.world.effect.MobEffects.WITHER,
								140,
								1,
								false,
								false,
								true
							));
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
					// Instantly cancel if player left, died, set to creative, or mob was hit/hurt!
					isHypnotizing = false;
					mData.putBoolean("is_hypnotizing", false);
					mData.putBoolean("isHypnotizing", false);
					mData.putInt("stare_ticks", 0);
					mData.putInt("hypnosis_ticks", 0);
					// Cooldown of 5 seconds (100 ticks) so it does not immediately re-hypnotize the player when damaged
					mData.putInt("hypnosis_cooldown", 100);
				} else {
					// Increment active hypnosis ticks count
					int hypnosisTicks = persistentInt(mData, "hypnosis_ticks", 0) + 1;
					mData.putInt("hypnosis_ticks", hypnosisTicks);

					// Lock entity rigidly in place on its grid node
					mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
					if (mob.distanceToSqr(targetGridX, mob.getY(), targetGridZ) > 0.002) {
						mob.teleportTo(targetGridX, mob.getY(), targetGridZ);
					}

					// Snap entity rotation to face player perfectly
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

					// Smooth telekinetic lift: Pull and hold player horizontally in front of mob's face, slightly lowered
					double holdDistance = 2.0; // Suspended 2 blocks away as requested
					double targetHoldX = mob.getX() - Math.sin(Math.toRadians(targetYawToPlayer)) * holdDistance;
					double targetHoldY = mobEyeY - 1.30; // Suspended slightly lower for ideal direct stare angle
					double targetHoldZ = mob.getZ() + Math.cos(Math.toRadians(targetYawToPlayer)) * holdDistance;

					double pullX = targetHoldX - nearestPlayer.getX();
					double pullY = targetHoldY - nearestPlayer.getY();
					double pullZ = targetHoldZ - nearestPlayer.getZ();
					double dist = Math.sqrt(pullX * pullX + pullY * pullY + pullZ * pullZ);

					if (dist > 0.05) {
						double pullFactorXZ = 0.22;
						double pullFactorY = 0.08; // Slower lifting for a creepy, dramatic levitation effect
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
						// Cap rising velocity to make lifting slower
						if (velY > 0.12) {
							velY = 0.12;
						}
						nearestPlayer.setDeltaMovement(velX, velY, velZ);
					} else {
						nearestPlayer.setDeltaMovement(0, 0.01, 0);
					}

					nearestPlayer.fallDistance = 0.0F;
					nearestPlayer.hasImpulse = true;

					// Target gets continuous slowness III, darkness, and wither for 7 seconds (140 ticks) during active hypnosis/telekinesis
					// But we only inflict the negative potion effects after waiting for 2 seconds (40 ticks)
					if (hypnosisTicks >= 40) {
						nearestPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
							net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
							140, // 7 seconds duration
							2,   // slowness 3 (amplifier 2)
							false,
							false,
							true
						));
						nearestPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
							net.minecraft.world.effect.MobEffects.DARKNESS,
							140, // 7 seconds duration
							0,
							false,
							false,
							true
						));
						nearestPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
							net.minecraft.world.effect.MobEffects.WITHER,
							140, // 7 seconds duration
							1,   // wither II (amplifier 1)
							false,
							false,
							true
						));
					}

					// Play deep, terrifying rumbling nether portal ambient sound repeatedly
					if (mob.tickCount % 30 == 0) {
						mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
							net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT,
							net.minecraft.sounds.SoundSource.HOSTILE,
							1.5F, 0.35F); // Louder but not deafening, highly deep distorted pitch
					}

					// Play Warden heartbeat louder periodically during high-intensity hypnosis
					if (mob.tickCount % 25 == 0) {
						mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
							net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
							net.minecraft.sounds.SoundSource.HOSTILE,
							5.0F, 0.75F); // Louder Warden heartbeat
					}

					// Abort normal navigation loop entirely while hypnotizing
					mob.getNavigation().stop();
					return;
				}
			}
		}

		// Ensure NBT flags remain synced with current hypnosis state for Blockly layer matching
		mData.putBoolean("is_hypnotizing", isHypnotizing);
		mData.putBoolean("isHypnotizing", isHypnotizing);

		// 4. Handle Blending / Frozen Disguise State
		// We only freeze if the player is active (not stationary) AND the player is actively looking at us.
		// If they look away or break line of sight, we subtly creep closer!
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

			// Override look control to face exactly straight ahead of our grid yaw - prevents twitching
			double lookX = mob.getX() - Math.sin(Math.toRadians(yaw)) * 5.0;
			double lookZ = mob.getZ() + Math.cos(Math.toRadians(yaw)) * 5.0;
			mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 30.0F, 30.0F);

			mob.getNavigation().stop();
			return;
		}

		int pauseTicks = persistentInt(mData, "grid_pause_ticks", 0);

		// Stuck Detect & Automatic Obstacle Resolution Engine:
		// If we are colliding horizontally or haven't moved despite trying to, flag as stuck and force a redirection
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
			// Teleport onto absolute grid center of intersection for zero collision margin errors when stuck
			if (mob.distanceToSqr(targetGridX, mob.getY(), targetGridZ) > 0.002) {
				mob.teleportTo(targetGridX, mob.getY(), targetGridZ);
			}
		}

		if (pauseTicks > 0) {
			mData.putInt("grid_pause_ticks", pauseTicks - 1);
			
			// Freeze physically at the block center coordinate (safe only when fully static)
			mob.setTarget(null);
			mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
			if (mob.distanceToSqr(targetGridX, mob.getY(), targetGridZ) > 0.002) {
				mob.teleportTo(targetGridX, mob.getY(), targetGridZ);
			}
			
			// Snap rotation perfectly onto cardinal to make it look completely rigid and block-aligned
			float yaw = persistentFloat(mData, "grid_yaw", 0.0F);
			mob.setYRot(yaw);
			mob.yRotO = yaw;
			mob.setYHeadRot(yaw);
			mob.yHeadRotO = yaw;
			mob.setYBodyRot(yaw);
			mob.yBodyRotO = yaw;

			// Override look control to face exactly straight ahead of our grid yaw - prevents twitching
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
			// Center perpendicular coordinate when initiating a turn for zero collision margin errors
			if (mob.distanceToSqr(targetGridX, mob.getY(), targetGridZ) > 0.002) {
				mob.teleportTo(targetGridX, mob.getY(), targetGridZ);
			}

			// If it's normal wandering cycle, evaluate if we stop and disguise as a pillar here
			if (!currentDir.equals("NONE") && Math.random() < PAUSE_CHANCE_O_GRID) {
				int ticksToPause = 60 + (int) (Math.random() * 120); // Stops for 3 to 9 seconds
				mData.putInt("grid_pause_ticks", ticksToPause);
				
				// Keep current alignment, stop moving
				mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
				
				// Secure perfectly cardinal looking direction to align seamlessly
				float randomYaw = (int)(Math.random() * 4) * 90.0F;
				mData.putFloat("grid_yaw", randomYaw);
				mob.setYRot(randomYaw);
				mob.yRotO = randomYaw;
				mob.setYHeadRot(randomYaw);
				mob.yHeadRotO = randomYaw;
				mob.setYBodyRot(randomYaw);
				mob.yBodyRotO = randomYaw;

				// Override look control to face exactly straight ahead of our grid yaw - prevents twitching
				double lookX = mob.getX() - Math.sin(Math.toRadians(randomYaw)) * 5.0;
				double lookZ = mob.getZ() + Math.cos(Math.toRadians(randomYaw)) * 5.0;
				mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 30.0F, 30.0F);
				
				mob.getNavigation().stop();
				return;
			}

			String nextDir = "NONE";
			float targetYaw = 0.0F;

			if (isClosingIn) {
				// CLOSING IN / TRAPPING: Path on Manhattan vectors straight to the player while avoiding obstacles
				double dX = nearestPlayer.getX() - mobX;
				double dZ = nearestPlayer.getZ() - mobZ;

				String primaryDir = Math.abs(dX) > Math.abs(dZ) ? (dX > 0 ? "EAST" : "WEST") : (dZ > 0 ? "SOUTH" : "NORTH");
				String secondaryDir = Math.abs(dX) > Math.abs(dZ) ? (dZ > 0 ? "SOUTH" : "NORTH") : (dX > 0 ? "EAST" : "WEST");

				if (!isDirectionBlocked(world, targetGridX, mob.getY(), targetGridZ, primaryDir)) {
					nextDir = primaryDir;
				} else if (!isDirectionBlocked(world, targetGridX, mob.getY(), targetGridZ, secondaryDir)) {
					nextDir = secondaryDir;
				} else {
					// Both direct paths are blocked. Choose any unblocked direction to find a way around
					java.util.List<String> okDirs = new java.util.ArrayList<>();
					for (String d : new String[]{"NORTH", "SOUTH", "EAST", "WEST"}) {
						if (!isDirectionBlocked(world, targetGridX, mob.getY(), targetGridZ, d)) {
							okDirs.add(d);
						}
					}
					if (!okDirs.isEmpty()) {
						nextDir = okDirs.get((int) (Math.random() * okDirs.size()));
					} else {
						nextDir = primaryDir; // Hard fallback
					}
				}
			} else {
				// WANDERING Corridors: High chance to go forward, moderate chance to turn orthogonal
				java.util.List<String> okDirs = new java.util.ArrayList<>();
				for (String d : new String[]{"NORTH", "SOUTH", "EAST", "WEST"}) {
					if (!isDirectionBlocked(world, targetGridX, mob.getY(), targetGridZ, d)) {
						okDirs.add(d);
					}
				}

				if (!okDirs.isEmpty()) {
					double rand = Math.random();
					if (currentDir.equals("NONE") || rand < 0.20) {
						// Choose fully random, valid direction
						nextDir = okDirs.get((int) (Math.random() * okDirs.size()));
					} else {
						// Seek options
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
							// Turn 90 degrees orthogonal if unblocked
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
			mData.putInt("grid_cooldown", 10); // Locking frame ticks to exit current intersection safely

			// Configure target cardinal orientation but don't instantly snap
			if (currentDir.equals("EAST") || currentDir.equals("WEST")) {
				targetYaw = currentDir.equals("EAST") ? -90.0F : 90.0F;
			} else if (currentDir.equals("NORTH") || currentDir.equals("SOUTH")) {
				targetYaw = currentDir.equals("NORTH") ? 180.0F : 0.0F;
			}
			mData.putFloat("grid_yaw", targetYaw);
		}

		// 6. Set Movement Velocity Vectors and Rotations
		double currentSpeed = isPlayerStationary ? EXTREME_CLOSE_IN_SPEED : WANDER_SPEED;
		float targetYaw = persistentFloat(mData, "grid_yaw", 0.0F);

		switch (currentDir) {
			case "NORTH" -> targetYaw = 180.0F;
			case "SOUTH" -> targetYaw = 0.0F;
			case "EAST"  -> targetYaw = -90.0F;
			case "WEST"  -> targetYaw = 90.0F;
		}

		// Smooth natural turning interpolation for both body and head
		float entityYaw = mob.getYRot();
		float diff = net.minecraft.util.Mth.wrapDegrees(targetYaw - entityYaw);
		float nextYaw = entityYaw + diff * 0.25F; // Smooth turning interpolation

		mob.setYRot(nextYaw);
		mob.yRotO = nextYaw;
		mob.setYBodyRot(nextYaw);
		mob.yBodyRotO = nextYaw;
		mob.setYHeadRot(nextYaw);
		mob.yHeadRotO = nextYaw;

		// Override look control to face exactly straight ahead of our smooth movement rotation to prevent vanilla AI from twisting/twitching the head!
		double lookX = mob.getX() - Math.sin(Math.toRadians(nextYaw)) * 5.0;
		double lookZ = mob.getZ() + Math.cos(Math.toRadians(nextYaw)) * 5.0;
		mob.getLookControl().setLookAt(lookX, mob.getEyeY(), lookZ, 30.0F, 30.0F);

		// Calculate velocity vectors rigidly aligned with selected movement direction axis to prevent walking sideways/laterally completely!
		double velX = 0;
		double velZ = 0;
		switch (currentDir) {
			case "NORTH" -> velZ = -currentSpeed;
			case "SOUTH" -> velZ = currentSpeed;
			case "EAST"  -> velX = currentSpeed;
			case "WEST"  -> velX = -currentSpeed;
		}

		// Smooth perpendicular axis centring/alignment (no sudden teleport snaps)
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

		// Maintain movement velocity
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
		net.minecraft.core.BlockPos targetPos = net.minecraft.core.BlockPos.containing(currentX + dx, feetY, currentZ + dz);
		net.minecraft.world.level.block.state.BlockState state = world.getBlockState(targetPos);
		net.minecraft.core.BlockPos targetPosUp = targetPos.above();
		net.minecraft.world.level.block.state.BlockState stateUp = world.getBlockState(targetPosUp);
		net.minecraft.core.BlockPos targetPosFarUp = targetPosUp.above();
		net.minecraft.world.level.block.state.BlockState stateFarUp = world.getBlockState(targetPosFarUp);

		boolean hasCollision = !state.getCollisionShape(world, targetPos).isEmpty() || 
		                       !stateUp.getCollisionShape(world, targetPosUp).isEmpty() ||
		                       !stateFarUp.getCollisionShape(world, targetPosFarUp).isEmpty();
		return hasCollision;
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
	} // 1.21.1
}
