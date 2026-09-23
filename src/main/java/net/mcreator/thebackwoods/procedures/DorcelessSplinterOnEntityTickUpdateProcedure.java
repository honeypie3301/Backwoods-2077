package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Comparator;

public class DorcelessSplinterOnEntityTickUpdateProcedure {

	private static final double DETECTION_RADIUS = 32.0;
	private static final double MIN_TELEPORT_DIST = 3.0; // Keeps at least 3 blocks distance during gaze
	private static final String NBT_TELEPORT_TIMER = "dorceless_tp_timer";
	private static final String NBT_GAZE_TICKS = "dorceless_gaze_ticks";

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Object event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !(entity instanceof Mob mob))
			return;

		// 1. Target Acquisition / Retention (Exclude Creative & Spectator players)
		Player targetPlayer = null;
		if (mob.getTarget() instanceof Player playerTarget && isSurvivalPlayer(playerTarget)) {
			targetPlayer = playerTarget;
		} else {
			final Vec3 center = new Vec3(x, y, z);
			targetPlayer = world.getEntitiesOfClass(Player.class, new AABB(center, center).inflate(DETECTION_RADIUS), p -> isSurvivalPlayer(p))
				.stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(center))).orElse(null);
			if (targetPlayer != null) {
				mob.setTarget(targetPlayer);
			}
		}

		if (targetPlayer == null) {
			// Decay gaze counter when no target is in range
			double currentVal = Math.max(0, mob.getPersistentData().getDouble(NBT_GAZE_TICKS) - 2);
			mob.getPersistentData().putDouble(NBT_GAZE_TICKS, currentVal);
			mob.getPersistentData().putDouble("stare_ticks", currentVal);
			return;
		}

		// 2. Body Alignment & Head Snap (Whole body turns to face target instantly)
		double headY = mob.getY() + mob.getEyeHeight();
		double targetHeadY = targetPlayer.getY() + targetPlayer.getEyeHeight();
		
		double dxToPlayer = targetPlayer.getX() - mob.getX();
		double dzToPlayer = targetPlayer.getZ() - mob.getZ();
		float targetYaw = (float) (Mth.atan2(dzToPlayer, dxToPlayer) * (180.0 / Math.PI)) - 90.0F;

		mob.setYRot(targetYaw);
		mob.setYBodyRot(targetYaw);
		mob.setYHeadRot(targetYaw);
		mob.getLookControl().setLookAt(targetPlayer.getX(), targetHeadY, targetPlayer.getZ(), 360.0F, 360.0F);

		// 3. Mutual Stare / Line-of-Sight Detection
		Vec3 mobEyePos = new Vec3(mob.getX(), headY, mob.getZ());
		Vec3 playerEyePos = new Vec3(targetPlayer.getX(), targetHeadY, targetPlayer.getZ());
		Vec3 pToD = mobEyePos.subtract(playerEyePos).normalize();
		Vec3 playerLook = targetPlayer.getLookAngle().normalize();

		double dot = playerLook.dot(pToD);
		boolean playerLookingAtDorceless = dot > 0.45 && targetPlayer.hasLineOfSight(mob);

		double currentDist = mob.distanceTo(targetPlayer);

		// 4. EYE MAGNET (Subtle crosshair magnet drag toward Dorceless's hollow face)
		if (playerLookingAtDorceless && currentDist <= 20.0) {
			double dxToMob = mob.getX() - targetPlayer.getX();
			double dzToMob = mob.getZ() - targetPlayer.getZ();
			double dyToMob = headY - targetPlayer.getEyePosition().y;
			double distHoriz = Math.sqrt(dxToMob * dxToMob + dzToMob * dzToMob);

			float idealYaw = (float) (Mth.atan2(dzToMob, dxToMob) * (180.0 / Math.PI)) - 90.0F;
			float idealPitch = (float) (-(Mth.atan2(dyToMob, distHoriz) * (180.0 / Math.PI)));

			float yawDiff = Mth.wrapDegrees(idealYaw - targetPlayer.getYRot());
			float pitchDiff = Mth.wrapDegrees(idealPitch - targetPlayer.getXRot());

			// Smooth magnetic pull locking player crosshair onto Dorceless's face
			targetPlayer.setYRot(targetPlayer.getYRot() + yawDiff * 0.08F);
			targetPlayer.setXRot(targetPlayer.getXRot() + pitchDiff * 0.08F);
		}

		// 5. PROGRESSIVE DARKNESS & CONTINUOUS GAZE OVERLOAD
		double gazeTicks = mob.getPersistentData().getDouble(NBT_GAZE_TICKS);
		double alertTicks = mob.getPersistentData().getDouble("dorceless_alert_ticks");

		if (playerLookingAtDorceless) {
			gazeTicks += 1;
			alertTicks = 80; // Keep fully alert (4s linger duration) when actively looked at
			mob.getPersistentData().putDouble(NBT_GAZE_TICKS, gazeTicks);
			mob.getPersistentData().putDouble("stare_ticks", gazeTicks);

			if (gazeTicks < 40) { // 0 - 2 seconds: Initial Darkness I
				targetPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, true));
			} else if (gazeTicks < 100) { // 2 - 5 seconds: Darkness II + Slowness I
				targetPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 1, false, false, true));
				targetPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
			} else if (gazeTicks < 160) { // 5 - 8 seconds: Darkness III + Slowness II + Nausea
				targetPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 2, false, false, true));
				targetPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
				targetPlayer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, true));
			} else { // 8+ seconds: CONTINUOUS SUSTAINED OVERLOAD
				targetPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 4, false, false, true));
				targetPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
				targetPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false, true));

				// Inflict void/magic damage pulse every 30 ticks (1.5 seconds) continuously while staring
				if (gazeTicks % 30 == 0) {
					targetPlayer.hurt(targetPlayer.damageSources().magic(), 3.0F);

					if (world instanceof Level level) {
						level.playSound(null, BlockPos.containing(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ()),
							SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.6F, 0.2F);
					}

					// Jump-scare positioning shift on pulse
					attemptSmartTeleportNear(world, mob, targetPlayer, MIN_TELEPORT_DIST, MIN_TELEPORT_DIST + 2.0, false);
				}

				// Cap gaze counter at 200 so pulse loops seamlessly
				if (gazeTicks > 200) {
					mob.getPersistentData().putDouble(NBT_GAZE_TICKS, 160);
					mob.getPersistentData().putDouble("stare_ticks", 160);
				}
			}
		} else {
			if (gazeTicks > 0) {
				double decayVal = Math.max(0, gazeTicks - 2);
				mob.getPersistentData().putDouble(NBT_GAZE_TICKS, decayVal);
				mob.getPersistentData().putDouble("stare_ticks", decayVal);
			}
			if (alertTicks > 0) {
				alertTicks -= 1; // Gradually cool down alert timer after looking away
			}
		}
		mob.getPersistentData().putDouble("dorceless_alert_ticks", alertTicks);

		// 6. POSITIONING TELEPORTATION (ACTIVE APPROACH OR LINGERING STALKING)
		double tpTimer = mob.getPersistentData().getDouble(NBT_TELEPORT_TIMER);
		tpTimer -= 1;
		
		boolean isAlert = alertTicks > 0;

		if (playerLookingAtDorceless && gazeTicks >= 20) {
			// Active Stare Approach
			if (tpTimer <= 0) {
				mob.getPersistentData().putDouble(NBT_TELEPORT_TIMER, 40 + mob.getRandom().nextInt(51)); // 2-4.5s cooldown

				if (currentDist > MIN_TELEPORT_DIST + 0.5) {
					double targetTpDist = Math.max(MIN_TELEPORT_DIST, currentDist - (3.0 + mob.getRandom().nextDouble() * 3.5));
					attemptSmartTeleportNear(world, mob, targetPlayer, MIN_TELEPORT_DIST, targetTpDist, false);
				}
			} else {
				mob.getPersistentData().putDouble(NBT_TELEPORT_TIMER, tpTimer);
			}
		} else if (isAlert && currentDist > 6.0) {
			// Lingering Stalking: Player turned away, but Dorceless remains alert and stalks behind
			if (tpTimer <= 0) {
				mob.getPersistentData().putDouble(NBT_TELEPORT_TIMER, 30 + mob.getRandom().nextInt(41)); // 1.5-3.5s cooldown

				double targetTpDist = Math.max(MIN_TELEPORT_DIST + 1.0, currentDist - (2.5 + mob.getRandom().nextDouble() * 3.0));
				attemptSmartTeleportNear(world, mob, targetPlayer, MIN_TELEPORT_DIST + 1.0, targetTpDist, true);
			} else {
				mob.getPersistentData().putDouble(NBT_TELEPORT_TIMER, tpTimer);
			}
		} else {
			// Player looked away and alert timer expired -> Dorceless stays frozen in position
			mob.getPersistentData().putDouble(NBT_TELEPORT_TIMER, 40);
		}
	}

	private static boolean isSurvivalPlayer(Player p) {
		return p.isAlive() && !p.isSpectator() && !p.isCreative();
	}

	private static void attemptSmartTeleportNear(LevelAccessor world, Mob mob, Player player, double minDist, double maxDist, boolean isStalking) {
		double px = player.getX();
		double py = player.getY();
		double pz = player.getZ();

		boolean playerHoldingRanged = player.getMainHandItem().is(Items.BOW) || player.getMainHandItem().is(Items.CROSSBOW) || player.isUsingItem();

		// Current vector from player to mob
		double dxToMob = mob.getX() - px;
		double dzToMob = mob.getZ() - pz;
		double currentAngleFromPlayer = Math.atan2(dzToMob, dxToMob);

		for (int attempt = 0; attempt < 14; attempt++) {
			double tryX, tryZ;
			double dist = minDist + mob.getRandom().nextDouble() * (maxDist - minDist);

			if (isStalking) {
				// Teleport behind or in blind spot of player when player turns away
				double sideSign = mob.getRandom().nextBoolean() ? 1.0 : -1.0;
				double blindAngle = player.getYRot() * (Math.PI / 180.0) + Math.PI + sideSign * (mob.getRandom().nextDouble() * (Math.PI / 6.0));
				tryX = px + Math.sin(-blindAngle) * dist;
				tryZ = pz + Math.cos(-blindAngle) * dist;
			} else if (playerHoldingRanged) {
				// Zigzag Flank maneuver if player has ranged weapon out
				double sideSign = mob.getRandom().nextBoolean() ? 1.0 : -1.0;
				double flankAngle = currentAngleFromPlayer + sideSign * (Math.PI / 4.0 + mob.getRandom().nextDouble() * (Math.PI / 6.0));
				tryX = px + Math.cos(flankAngle) * dist;
				tryZ = pz + Math.sin(flankAngle) * dist;
			} else {
				// DIRECT STRAIGHT LINE APPROACH toward player with small side jitter (±15°)
				double sideOffset = (mob.getRandom().nextDouble() - 0.5) * (Math.PI / 6.0);
				double approachAngle = currentAngleFromPlayer + sideOffset;
				tryX = px + Math.cos(approachAngle) * dist;
				tryZ = pz + Math.sin(approachAngle) * dist;
			}

			for (int yOffset = 3; yOffset >= -4; yOffset--) {
				BlockPos pos = BlockPos.containing(tryX, py + yOffset, tryZ);
				BlockPos below = pos.below();

				if (world.getBlockState(below).isSolid() && world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above())) {
					double finalX = tryX;
					double finalY = pos.getY();
					double finalZ = tryZ;

					if (world instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.SQUID_INK, mob.getX(), mob.getY() + 1.0, mob.getZ(), 15, 0.2, 0.5, 0.2, 0.05);
					}
					if (world instanceof Level level) {
						level.playSound(null, BlockPos.containing(mob.getX(), mob.getY(), mob.getZ()), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 0.5F);
					}

					mob.teleportTo(finalX, finalY, finalZ);
					
					// Face body & head immediately after teleport
					double dx = player.getX() - finalX;
					double dz = player.getZ() - finalZ;
					float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
					mob.setYRot(yaw);
					mob.setYBodyRot(yaw);
					mob.setYHeadRot(yaw);
					mob.getLookControl().setLookAt(player, 360.0F, 360.0F);

					if (world instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.SQUID_INK, finalX, finalY + 1.0, finalZ, 15, 0.2, 0.5, 0.2, 0.05);
					}
					if (world instanceof Level level) {
						level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 0.5F);
					}

					return;
				}
			}
		}
	}
} // 1.21.1