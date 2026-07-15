package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.mcreator.thebackwoods.entity.LogSplinterEntity;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
// 1.21.1 neoforge
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;

import javax.annotation.Nullable;
import java.util.List;

@EventBusSubscriber
public class LogSplinterOnEntityTickUpdateProcedure {

	private static final double WATCH_DOT_THRESHOLD = 0.5;
	private static final double ACTIVE_MOVE_SPEED = 0.335;
	private static final double DEGRADED_MOVE_SPEED = 0.20;
	private static final float MINE_SPEED_MULTIPLIER = 40f;
	private static final float MINE_SPEED_BASE = 40f;
	private static final float DEGRADED_MINE_SPEED_MULTIPLIER = 80f;
	private static final float DEGRADED_MINE_SPEED_BASE = 80f;
	private static final float MAX_BREAKABLE_HARDNESS = 50f;
	private static final double TARGET_RANGE = 56;
	private static final double MINE_RAY_DISTANCE = 3.0;
	private static final double ROSE_WILT_TICKS = 750;
	private static final int ROSE_SCAN_XZ = 6;
	private static final int ROSE_SCAN_Y = 3;
	private static final int RAGE_WATCH_THRESHOLD = 590;
	private static final int RAGE_THRESHOLD_HIT_BONUS = 150;
	private static final double RAGE_ESCAPE_RANGE = 18.0;

	private static final int PATH_RECALC_INTERVAL_TICKS = 8;
	private static final double TARGET_MOVE_REPATH_DIST_SQR = 1.25;

	private static final String K_RAGE_BONUS = "rage_watch_bonus";
	private static final String K_LAST_HURT_TIME = "rage_last_hurt_time";

	private static final String K_HM_PHASE = "hm_phase";
	private static final String K_HM_PHASE_TIMER = "hm_phase_timer";
	private static final String K_HM_SILENT_CD = "hm_silent_cd";
	private static final String K_FORCED_HUNT_TICKS = "forced_hunt_ticks";

	private static final String K_NAV_LAST_TICK = "nav_last_tick";
	private static final String K_NAV_TX = "nav_tx";
	private static final String K_NAV_TY = "nav_ty";
	private static final String K_NAV_TZ = "nav_tz";
	private static final String K_NAV_MODE = "nav_mode";

	private static final String K_STALK_WATCH_TICKS = "stalk_watch_ticks";
	private static final int STALK_WATCH_MIN = 20;
	private static final int STALK_WATCH_MAX = 50;
	private static final int STALK_WATCH_CHANCE = 160;

	// Personality
	private static final String K_PERSONALITY_INIT = "ai_personality_init";
	private static final String K_PERSONALITY = "ai_personality"; // 0=stalk/hunt, 1=angel
	private static final int PERSONALITY_STALK_HUNT = 0;
	private static final int PERSONALITY_ANGEL = 1;
	private static final int ANGEL_CHANCE_PERCENT = 17;

	private static final int PHASE_SILENT = 0;
	private static final int PHASE_STALK = 1;
	private static final int PHASE_HUNT = 2;

	private static final int SILENT_MIN_TICKS = 80;
	private static final int SILENT_MAX_TICKS = 220;
	private static final int STALK_MIN_TICKS = 120;
	private static final int STALK_MAX_TICKS = 300;
	private static final int HUNT_TICKS = 400;
	private static final int POST_HUNT_SILENT_CD = 240;

	private static final double STALK_SPEED = 0.14;
	private static final double STALK_FOLLOW_DIST = 18.0;

	private static final int AGE_THIRD_TO_LAST = 360000;
	private static final int AGE_SECOND_TO_LAST = 420000;
	private static final int AGE_LAST = 480000;

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof LogSplinterEntity logSplinter)) return;

		if (logSplinter.getTarget() instanceof Player p && (p.isCreative() || p.isSpectator())) {
			logSplinter.setTarget(null);
		}
		if (logSplinter.getLastHurtByMob() instanceof Player p && (p.isCreative() || p.isSpectator())) {
			logSplinter.setLastHurtByMob(null);
		}

		if (!logSplinter.getPersistentData().getBoolean(K_PERSONALITY_INIT)) {
			int p = (logSplinter.getRandom().nextInt(100) < ANGEL_CHANCE_PERCENT) ? PERSONALITY_ANGEL : PERSONALITY_STALK_HUNT;
			logSplinter.getPersistentData().putInt(K_PERSONALITY, p);
			logSplinter.getPersistentData().putBoolean(K_PERSONALITY_INIT, true);
		}
		int personality = logSplinter.getPersistentData().getInt(K_PERSONALITY);

		if (logSplinter.isPassenger()) {
			Entity vehicle = logSplinter.getVehicle();
			if (vehicle instanceof net.minecraft.world.entity.vehicle.Boat || vehicle instanceof net.minecraft.world.entity.vehicle.ChestBoat) {
				logSplinter.stopRiding();
				logSplinter.setDeltaMovement(logSplinter.getDeltaMovement().add(0, 0.2, 0));
			}
		}

		int age = logSplinter.getEntityData().get(LogSplinterEntity.DATA_Age);
		if (age >= AGE_LAST) {
			logSplinter.discard();
			return;
		}

		boolean isDegraded = age >= AGE_THIRD_TO_LAST;
		boolean isCritical = age >= AGE_SECOND_TO_LAST;

		Player foundPlayerDirect = findNearestPlayerInRange(world, logSplinter.getX(), logSplinter.getY(), logSplinter.getZ(), TARGET_RANGE);
		if (world instanceof Level level && level.isClientSide()) {
			return;
		}
		LivingEntity foundPlayer = foundPlayerDirect;

		LivingEntity prioritizedMob = null;
		String attackerUUIDStr = logSplinter.getPersistentData().getString("splinter_attacker_uuid");
		if (!attackerUUIDStr.isEmpty() && world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			try {
				java.util.UUID attackerUUID = java.util.UUID.fromString(attackerUUIDStr);
				net.minecraft.world.entity.Entity foundEntity = serverLevel.getEntity(attackerUUID);
				if (foundEntity instanceof LivingEntity livingAttacker && livingAttacker.isAlive() && logSplinter.distanceToSqr(livingAttacker) < TARGET_RANGE * TARGET_RANGE) {
					boolean isWoodbound = livingAttacker.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
					if (!isWoodbound && !(livingAttacker instanceof Player)) {
						prioritizedMob = livingAttacker;
					}
				}
			} catch (Exception e) {}
		}

		LivingEntity attacker = logSplinter.getLastHurtByMob();
		if (attacker != null) {
			boolean isWoodbound = attacker.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
			if (isWoodbound) {
				if (logSplinter.getLastHurtByMob() == attacker) {
					logSplinter.setLastHurtByMob(null);
				}
				attacker = null;
			} else if (attacker.isAlive() && logSplinter.distanceToSqr(attacker) < TARGET_RANGE * TARGET_RANGE) {
				if (!(attacker instanceof Player)) {
					prioritizedMob = attacker;
					logSplinter.getPersistentData().putString("splinter_attacker_uuid", attacker.getUUID().toString());
				}
			}
		}

		boolean hadPrioritizedMob = !attackerUUIDStr.isEmpty();
		if (hadPrioritizedMob && prioritizedMob == null) {
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_isEnraged, 0);
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_watchTimer, 0);
			logSplinter.getPersistentData().putInt(K_RAGE_BONUS, 0);
			logSplinter.getPersistentData().putInt(K_FORCED_HUNT_TICKS, 0);
			logSplinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
			if (logSplinter.getLastHurtByMob() != null) {
				logSplinter.setLastHurtByMob(null);
			}
			logSplinter.getPersistentData().remove("splinter_attacker_uuid");
		}

		if (prioritizedMob != null) {
			foundPlayer = prioritizedMob;
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_isEnraged, 1);
			if (logSplinter.getTarget() != prioritizedMob) {
				logSplinter.setTarget(prioritizedMob);
			}
			if (logSplinter.getLastHurtByMob() != prioritizedMob) {
				logSplinter.setLastHurtByMob(prioritizedMob);
			}
		} else {
			if (attacker instanceof Player p && p.isAlive() && !p.isCreative() && !p.isSpectator() && logSplinter.distanceToSqr(attacker) < TARGET_RANGE * TARGET_RANGE) {
				foundPlayer = p;
			}
			if (foundPlayer != null) {
				if (logSplinter.getTarget() != foundPlayer) {
					logSplinter.setTarget(foundPlayer);
				}
			} else {
				if (logSplinter.getTarget() != null) {
					logSplinter.setTarget(null);
				}
			}
		}

		if (foundPlayer != null) {
			boolean isWoodbound = foundPlayer.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
			if (isWoodbound) {
				foundPlayer = null;
			}
		}

		if (foundPlayer == null) {
			int remainingForcedHunt = logSplinter.getPersistentData().getInt(K_FORCED_HUNT_TICKS);

			if (remainingForcedHunt <= 0) {
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_watchTimer, 0);
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_isEnraged, 0);
				logSplinter.getPersistentData().putInt(K_RAGE_BONUS, 0);
				logSplinter.getPersistentData().putInt(K_HM_PHASE, PHASE_SILENT);
				logSplinter.getPersistentData().putInt(K_HM_PHASE_TIMER, 0);
				logSplinter.getPersistentData().putInt(K_FORCED_HUNT_TICKS, 0);
				logSplinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
				stopNav(logSplinter);
			} else {
				logSplinter.getPersistentData().putInt(K_FORCED_HUNT_TICKS, remainingForcedHunt - 1);
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_isEnraged, 1);
				logSplinter.getPersistentData().putInt(K_HM_PHASE, PHASE_HUNT);
				logSplinter.getPersistentData().putInt(K_HM_PHASE_TIMER, HUNT_TICKS);
			}
			return;
		}

		int frozenByRose = logSplinter.getEntityData().get(LogSplinterEntity.DATA_frozenByRose);
		int isEnraged = logSplinter.getEntityData().get(LogSplinterEntity.DATA_isEnraged);
		int watchTimer = logSplinter.getEntityData().get(LogSplinterEntity.DATA_watchTimer);

		int forcedHuntTicks = logSplinter.getPersistentData().getInt(K_FORCED_HUNT_TICKS);
		if (forcedHuntTicks > 0) {
			logSplinter.getPersistentData().putInt(K_FORCED_HUNT_TICKS, forcedHuntTicks - 1);
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_isEnraged, 1);
			isEnraged = 1;
		}

		if (logSplinter.getLastHurtByMob() != null) {
			boolean isWoodbound = logSplinter.getLastHurtByMob().getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
			if (!isWoodbound) {
				int lastSeenHurtTime = logSplinter.getPersistentData().getInt(K_LAST_HURT_TIME);
				int currentHurtTime = logSplinter.hurtTime;
				if (currentHurtTime > 0 && currentHurtTime != lastSeenHurtTime) {
					int bonus = logSplinter.getPersistentData().getInt(K_RAGE_BONUS) + RAGE_THRESHOLD_HIT_BONUS;
					logSplinter.getPersistentData().putInt(K_RAGE_BONUS, bonus);
					logSplinter.getPersistentData().putInt(K_LAST_HURT_TIME, currentHurtTime);
				}
			}
		}

		int effectiveRageThreshold = Math.max(1, RAGE_WATCH_THRESHOLD - logSplinter.getPersistentData().getInt(K_RAGE_BONUS));

		boolean isWatched = false;
		if (foundPlayerDirect != null) {
			Vec3 toLogSplinter = logSplinter.getEyePosition().subtract(foundPlayerDirect.getEyePosition());
			if (toLogSplinter.lengthSqr() > 1.0e-8) toLogSplinter = toLogSplinter.normalize();
			double dot = foundPlayerDirect.getLookAngle().normalize().dot(toLogSplinter);
			isWatched = (dot > WATCH_DOT_THRESHOLD) && foundPlayerDirect.hasLineOfSight(logSplinter);
		}

		double distToPlayer = foundPlayerDirect != null ? logSplinter.position().distanceTo(foundPlayerDirect.position()) : Double.MAX_VALUE;
		if (isEnraged == 1 && distToPlayer > RAGE_ESCAPE_RANGE) {
			if (logSplinter.getPersistentData().getInt(K_FORCED_HUNT_TICKS) <= 0) {
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_isEnraged, 0);
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_watchTimer, 0);
				isEnraged = 0;
			}
		}

		if (isWatched && isEnraged == 0) {
			watchTimer++;
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_watchTimer, watchTimer);
			if (watchTimer >= effectiveRageThreshold) {
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_isEnraged, 1);
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_watchTimer, 0);
				isEnraged = 1;
			}
		} else if (!isWatched && isEnraged == 0) {
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_watchTimer, 0);
		}

		boolean foundRose = false;
		if (foundPlayerDirect != null) {
			foundRose = checkHeldRose(foundPlayerDirect, logSplinter, world, foundPlayerDirect.getX(), foundPlayerDirect.getY(), foundPlayerDirect.getZ());
		}
		if (!foundRose && logSplinter.tickCount % 5 == 0) {
			foundRose = checkNearbyRoseBlocks(world, logSplinter);
		}
		if (foundRose || frozenByRose == 1) {
			setSpeed(logSplinter, 0);
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_isEnraged, 0);
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_watchTimer, 0);
			logSplinter.getPersistentData().putInt(K_RAGE_BONUS, 0);
			logSplinter.getPersistentData().putInt(K_FORCED_HUNT_TICKS, 0);
			logSplinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
			stopNav(logSplinter);
			return;
		}

		double heightDiff = foundPlayer.getY() - logSplinter.getY();
		double horizontalDist = logSplinter.position().distanceTo(foundPlayer.position());
		boolean isTowering = !isCritical && (heightDiff >= 1.4 && horizontalDist < 12);

		if (personality == PERSONALITY_ANGEL) {
			if (isWatched && isEnraged == 0) {
				setSpeed(logSplinter, 0);
				stopNav(logSplinter);
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineProgress, 0);
				return;
			}

			if (logSplinter.tickCount % 3 == 0) {
				logSplinter.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(foundPlayer.getX(), foundPlayer.getEyeY(), foundPlayer.getZ()));
			}

			setSpeed(logSplinter, isDegraded ? DEGRADED_MOVE_SPEED : ACTIVE_MOVE_SPEED);
			if (logSplinter instanceof Mob angelMob) {
				navMoveToEntityThrottled(logSplinter, angelMob, foundPlayer, 1.0, 3);
			}
		} else {
			int phase = logSplinter.getPersistentData().getInt(K_HM_PHASE);
			int phaseTimer = logSplinter.getPersistentData().getInt(K_HM_PHASE_TIMER);
			int silentCd = logSplinter.getPersistentData().getInt(K_HM_SILENT_CD);

			if (phase == PHASE_SILENT && isEnraged == 0 && forcedHuntTicks <= 0) {
				phase = PHASE_STALK;
				phaseTimer = randomBetween(logSplinter, STALK_MIN_TICKS, STALK_MAX_TICKS);
				logSplinter.getPersistentData().putInt(K_HM_PHASE, phase);
				logSplinter.getPersistentData().putInt(K_HM_PHASE_TIMER, phaseTimer);
				logSplinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
			}

			if (silentCd > 0) {
				silentCd--;
				logSplinter.getPersistentData().putInt(K_HM_SILENT_CD, silentCd);
			}

			if (isEnraged == 1 || logSplinter.getPersistentData().getInt(K_FORCED_HUNT_TICKS) > 0) {
				phase = PHASE_HUNT;
				phaseTimer = HUNT_TICKS + 40;
				logSplinter.getPersistentData().putInt(K_HM_PHASE, phase);
				logSplinter.getPersistentData().putInt(K_HM_PHASE_TIMER, phaseTimer);
				logSplinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
			} else {
				phaseTimer--;
				if (phaseTimer <= 0) {
					if (phase == PHASE_HUNT) {
						phase = PHASE_SILENT;
						phaseTimer = randomBetween(logSplinter, SILENT_MIN_TICKS, SILENT_MAX_TICKS);
						logSplinter.getPersistentData().putInt(K_HM_SILENT_CD, POST_HUNT_SILENT_CD);
						logSplinter.getEntityData().set(LogSplinterEntity.DATA_watchTimer, 0);
						logSplinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
					} else if (phase == PHASE_SILENT) {
						phase = PHASE_STALK;
						phaseTimer = randomBetween(logSplinter, STALK_MIN_TICKS, STALK_MAX_TICKS);
						logSplinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
					} else {
						phase = PHASE_SILENT;
						phaseTimer = randomBetween(logSplinter, SILENT_MIN_TICKS, SILENT_MAX_TICKS);
						logSplinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
					}
				}
				logSplinter.getPersistentData().putInt(K_HM_PHASE, phase);
				logSplinter.getPersistentData().putInt(K_HM_PHASE_TIMER, phaseTimer);
			}

			if (phase == PHASE_SILENT) {
				setSpeed(logSplinter, 0);
				stopNav(logSplinter);
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineProgress, 0);
				return;
			}

			if (logSplinter.tickCount % 3 == 0) {
				logSplinter.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(foundPlayer.getX(), foundPlayer.getEyeY(), foundPlayer.getZ()));
			}

			if (phase == PHASE_STALK) {
				setSpeed(logSplinter, STALK_SPEED);

				int watchTicks = logSplinter.getPersistentData().getInt(K_STALK_WATCH_TICKS);
				if (watchTicks > 0) {
					logSplinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, watchTicks - 1);
					stopNav(logSplinter);
					logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineProgress, 0);
					return;
				}

				if (logSplinter.getRandom().nextInt(STALK_WATCH_CHANCE) == 0) {
					logSplinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, randomBetween(logSplinter, STALK_WATCH_MIN, STALK_WATCH_MAX));
					stopNav(logSplinter);
					logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineProgress, 0);
					return;
				}

				if (logSplinter instanceof Mob mob) {
					if (distToPlayer > STALK_FOLLOW_DIST) {
						navMoveToEntityThrottled(logSplinter, mob, foundPlayer, 0.8, 1);
					} else {
						if (!mob.getNavigation().isDone() && logSplinter.tickCount % 10 == 0) {
							stopNav(logSplinter);
						}
					}
				}
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineProgress, 0);
				return;
			}

			setSpeed(logSplinter, isDegraded ? DEGRADED_MOVE_SPEED : ACTIVE_MOVE_SPEED);
			if (logSplinter instanceof Mob huntMob) {
				navMoveToEntityThrottled(logSplinter, huntMob, foundPlayer, 1.0, 3);
			}
		}

		Vec3 logEyes = logSplinter.getEyePosition(1f);
		Vec3 logView = logSplinter.getViewVector(1f);
		Vec3 blockCheckTarget = logEyes.add(logView.scale(MINE_RAY_DISTANCE));

		HitResult hit = world.clip(new ClipContext(logEyes, blockCheckTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, logSplinter));

		BlockPos facePos;
		BlockPos feetPos;

		if (hit.getType() == HitResult.Type.BLOCK) {
			facePos = ((BlockHitResult) hit).getBlockPos();
			feetPos = new BlockPos(facePos.getX(), Mth.floor(logSplinter.getY()), facePos.getZ());
		} else {
			Vec3 look = logSplinter.getLookAngle().normalize();
			int fx = Mth.floor(logSplinter.getX() + look.x);
			int fz = Mth.floor(logSplinter.getZ() + look.z);
			feetPos = new BlockPos(fx, Mth.floor(logSplinter.getY()), fz);
			facePos = new BlockPos(fx, Mth.floor(logSplinter.getEyeY()), fz);
		}

		boolean canMineFeet = canMine(world, feetPos, foundPlayer);
		boolean canMineFace = canMine(world, facePos, foundPlayer);

		if (canMineFeet || canMineFace) {
			logSplinter.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			int mineProgress = logSplinter.getEntityData().get(LogSplinterEntity.DATA_mineProgress) + 1;
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineProgress, mineProgress);

			if (logSplinter.tickCount % 6 == 0) {
				logSplinter.swing(InteractionHand.MAIN_HAND);
			}

			BlockPos trackPos = canMineFeet ? feetPos : facePos;
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineX, trackPos.getX());
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineY, trackPos.getY());
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineZ, trackPos.getZ());

			float speedRef = canMineFeet
					? world.getBlockState(feetPos).getDestroySpeed(world, feetPos)
					: world.getBlockState(facePos).getDestroySpeed(world, facePos);

			float mineThreshold = isDegraded
					? speedRef * DEGRADED_MINE_SPEED_MULTIPLIER + DEGRADED_MINE_SPEED_BASE
					: speedRef * MINE_SPEED_MULTIPLIER + MINE_SPEED_BASE;

			if (mineProgress > mineThreshold) {
				if (canMineFeet) world.destroyBlock(feetPos, false);
				if (canMineFace) world.destroyBlock(facePos, false);
				stopNav(logSplinter);
				logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineProgress, 0);
			}
		} else {
			logSplinter.getEntityData().set(LogSplinterEntity.DATA_mineProgress, 0);

			if (!isCritical) {
				if (heightDiff >= 1.4 && horizontalDist < 12) {
					logSplinter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.OAK_WOOD));
					if (logSplinter.tickCount % 6 == 0) {
						logSplinter.swing(InteractionHand.MAIN_HAND);
					}

					BlockPos under = BlockPos.containing(logSplinter.getX(), logSplinter.getY() - 1, logSplinter.getZ());
					BlockPos body = BlockPos.containing(logSplinter.getX(), logSplinter.getY(), logSplinter.getZ());
					BlockPos bodyUp = body.above();

					if (world.getBlockState(under).isAir()) {
						world.setBlock(under, Blocks.OAK_WOOD.defaultBlockState(), 3);
					}
					if (world.getBlockState(body).is(Blocks.OAK_WOOD)) {
						world.setBlock(body, Blocks.AIR.defaultBlockState(), 3);
					}
					if (world.getBlockState(bodyUp).is(Blocks.OAK_WOOD)) {
						world.setBlock(bodyUp, Blocks.AIR.defaultBlockState(), 3);
					}

					world.destroyBlock(BlockPos.containing(logSplinter.getX(), logSplinter.getY() + 2, logSplinter.getZ()), false);
					world.destroyBlock(BlockPos.containing(logSplinter.getX(), logSplinter.getY() + 3, logSplinter.getZ()), false);

					if (logSplinter.onGround()) {
						logSplinter.setDeltaMovement(new Vec3(logSplinter.getDeltaMovement().x(), 0.4, logSplinter.getDeltaMovement().z()));
					}
					logSplinter.fallDistance = 0;
				} else if (heightDiff > -0.5 && heightDiff < 2.5) {
					Vec3 look = logSplinter.getLookAngle();
					BlockPos bridgePos = BlockPos.containing(logSplinter.getX() + look.x, logSplinter.getY() - 1, logSplinter.getZ() + look.z);
					BlockPos farBridgePos = BlockPos.containing(logSplinter.getX() + look.x * 2.0, logSplinter.getY() - 1, logSplinter.getZ() + look.z * 2.0);
					boolean isNearGap = !(world.getBlockFloorHeight(bridgePos) > 0) || !(world.getBlockFloorHeight(farBridgePos) > 0);

					if (isNearGap) {
						logSplinter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.OAK_WOOD));
						if (!(world.getBlockFloorHeight(bridgePos) > 0)) {
							if (logSplinter.tickCount % 6 == 0) {
								logSplinter.swing(InteractionHand.MAIN_HAND);
							}
							world.setBlock(bridgePos, Blocks.OAK_WOOD.defaultBlockState(), 3);
						}
					} else {
						logSplinter.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					}
				} else {
					logSplinter.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				}
			} else {
				logSplinter.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			}
		}

		// Melee reach range increase for non-players only (by 50%)
		LivingEntity target = logSplinter.getTarget();
		if (target != null && target.isAlive() && !(target instanceof Player)) {
			double reach = logSplinter.getBbWidth() + target.getBbWidth() + 0.8;
			double increasedReach = reach * 1.5;
			double distSqr = logSplinter.distanceToSqr(target);
			if (distSqr <= increasedReach * increasedReach) {
				int lastAttackTick = logSplinter.getPersistentData().getInt("last_melee_attack_tick");
				if (logSplinter.tickCount - lastAttackTick >= 20) {
					logSplinter.doHurtTarget(target);
					logSplinter.swing(InteractionHand.MAIN_HAND);
					logSplinter.getPersistentData().putInt("last_melee_attack_tick", logSplinter.tickCount);
				}
			}
		}
	}

	private static int randomBetween(LogSplinterEntity entity, int min, int max) {
		if (max <= min) return min;
		return min + entity.getRandom().nextInt(max - min + 1);
	}

	private static void setSpeed(LivingEntity entity, double speed) {
		if (entity.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)) {
			entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
		}
	}

	private static boolean canMine(LevelAccessor world, BlockPos pos, LivingEntity player) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir()) return false;
		float speed = state.getDestroySpeed(world, pos);
		if (speed < 0 || speed >= MAX_BREAKABLE_HARDNESS) return false;
		if (pos.getY() == (int) (player.getY() - 2)) return false;
		return true;
	}

	private static boolean checkHeldRose(Entity holder, LogSplinterEntity logSplinter, LevelAccessor world, double x, double y, double z) {
		if (!(holder instanceof LivingEntity living)) return false;

		ItemStack main = living.getMainHandItem();
		ItemStack off = living.getOffhandItem();

		boolean mainIsRose = main.getItem() == TheBackwoodsModBlocks.ASH_ROSE.get().asItem();
		boolean offIsRose = off.getItem() == TheBackwoodsModBlocks.ASH_ROSE.get().asItem();

		if (!mainIsRose && !offIsRose) return false;

		setSpeed(logSplinter, 0);
		stopNav(logSplinter);

		if (mainIsRose) tickRoseItem(main, world, x, y, z);
		if (offIsRose) tickRoseItem(off, world, x, y, z);

		return true;
	}

	private static void tickRoseItem(ItemStack rose, LevelAccessor world, double x, double y, double z) {
		double wiltTimer = rose.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("wilt_timer") + 1.0;
		CustomData.update(DataComponents.CUSTOM_DATA, rose, tag -> tag.putDouble("wilt_timer", wiltTimer));

		if (wiltTimer > ROSE_WILT_TICKS) {
			if (world instanceof Level level) {
				if (!level.isClientSide()) {
					level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1f, 1f);
				} else {
					level.playLocalSound(x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1f, 1f, false);
				}
			}
			rose.shrink(1);
			CustomData.update(DataComponents.CUSTOM_DATA, rose, tag -> tag.putDouble("wilt_timer", 0));
		}
	}

	private static boolean checkNearbyRoseBlocks(LevelAccessor world, LogSplinterEntity logSplinter) {
		BlockPos base = logSplinter.blockPosition();
		for (int sx = -ROSE_SCAN_XZ; sx < ROSE_SCAN_XZ; sx++) {
			for (int sy = -ROSE_SCAN_Y; sy < ROSE_SCAN_Y; sy++) {
				for (int sz = -ROSE_SCAN_XZ; sz < ROSE_SCAN_XZ; sz++) {
					BlockPos check = base.offset(sx, sy, sz);
					if (world.getBlockState(check).getBlock() == TheBackwoodsModBlocks.ASH_ROSE.get()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static Player findNearestPlayerInRange(LevelAccessor world, double x, double y, double z, double range) {
		AABB box = AABB.ofSize(new Vec3(x, y, z), range, range, range);
		List<Player> players = world.getEntitiesOfClass(Player.class, box, e -> !e.isCreative() && !e.isSpectator());

		Player nearest = null;
		double best = range * range;
		for (Player p : players) {
			double d = p.distanceToSqr(x, y, z);
			if (d < best) {
				best = d;
				nearest = p;
			}
		}
		return nearest;
	}

	private static void stopNav(LogSplinterEntity logSplinter) {
		if (logSplinter instanceof Mob mob) {
			mob.getNavigation().stop();
		}
		logSplinter.getPersistentData().putInt(K_NAV_MODE, 0);
	}

	private static void navMoveToEntityThrottled(LogSplinterEntity logSplinter, Mob mob, Entity target, double speed, int mode) {
		navMoveToPosThrottled(logSplinter, mob, target.getX(), target.getY(), target.getZ(), speed, mode);
	}

	private static void navMoveToPosThrottled(LogSplinterEntity logSplinter, Mob mob, double tx, double ty, double tz, double speed, int mode) {
		int now = logSplinter.tickCount;
		int lastTick = logSplinter.getPersistentData().getInt(K_NAV_LAST_TICK);
		int lastMode = logSplinter.getPersistentData().getInt(K_NAV_MODE);

		double lastTx = logSplinter.getPersistentData().getDouble(K_NAV_TX);
		double lastTy = logSplinter.getPersistentData().getDouble(K_NAV_TY);
		double lastTz = logSplinter.getPersistentData().getDouble(K_NAV_TZ);

		boolean timerReady = (now - lastTick) >= PATH_RECALC_INTERVAL_TICKS;
		boolean modeChanged = lastMode != mode;
		boolean noLast = (lastTick == 0 && lastMode == 0 && lastTx == 0.0 && lastTy == 0.0 && lastTz == 0.0);

		double movedSqr = noLast ? Double.MAX_VALUE : sqr(tx - lastTx) + sqr(ty - lastTy) + sqr(tz - lastTz);
		boolean targetMovedEnough = movedSqr >= TARGET_MOVE_REPATH_DIST_SQR;

		if (modeChanged || timerReady || targetMovedEnough || mob.getNavigation().isDone()) {
			mob.getNavigation().moveTo(tx, ty, tz, speed);
			logSplinter.getPersistentData().putInt(K_NAV_LAST_TICK, now);
			logSplinter.getPersistentData().putInt(K_NAV_MODE, mode);
			logSplinter.getPersistentData().putDouble(K_NAV_TX, tx);
			logSplinter.getPersistentData().putDouble(K_NAV_TY, ty);
			logSplinter.getPersistentData().putDouble(K_NAV_TZ, tz);
		}
	}

	private static double sqr(double v) {
		return v * v;
	}
}