package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
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

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;
import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;

import javax.annotation.Nullable;
import java.util.List;

@EventBusSubscriber
public class BlindspotSplinterOnEntityTickUpdateProcedure {

	private static final double WATCH_DOT_THRESHOLD = 0.5;
	private static final double ACTIVE_MOVE_SPEED = 0.35;
	private static final double DEGRADED_MOVE_SPEED = 0.22;
	private static final float MINE_SPEED_MULTIPLIER = 50f;
	private static final float MINE_SPEED_BASE = 50f;
	private static final float DEGRADED_MINE_SPEED_MULTIPLIER = 100f;
	private static final float DEGRADED_MINE_SPEED_BASE = 100f;
	private static final float MAX_BREAKABLE_HARDNESS = 50f;

	private static final double TARGET_RANGE = 56;
	private static final double MINE_RAY_DISTANCE = 2.0;
	private static final double ROSE_WILT_TICKS = 150;
	private static final int ROSE_SCAN_XZ = 6;
	private static final int ROSE_SCAN_Y = 3;

	private static final int RAGE_WATCH_THRESHOLD = 420;
	private static final int RAGE_THRESHOLD_HIT_BONUS = 180;
	private static final double RAGE_ESCAPE_RANGE = 24.0;

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
	private static final int STALK_WATCH_MIN = 18;
	private static final int STALK_WATCH_MAX = 45;
	private static final int STALK_WATCH_CHANCE = 140;

	// Personality
	private static final String K_PERSONALITY_INIT = "ai_personality_init";
	private static final String K_PERSONALITY = "ai_personality"; // 0=stalk/hunt, 1=angel
	private static final int PERSONALITY_STALK_HUNT = 0;
	private static final int PERSONALITY_ANGEL = 1;
	private static final int ANGEL_CHANCE_PERCENT = 25;

	private static final int PHASE_SILENT = 0;
	private static final int PHASE_STALK = 1;
	private static final int PHASE_HUNT = 2;

	private static final int SILENT_MIN_TICKS = 45;
	private static final int SILENT_MAX_TICKS = 120;
	private static final int STALK_MIN_TICKS = 70;
	private static final int STALK_MAX_TICKS = 160;
	private static final int HUNT_TICKS = 520;
	private static final int POST_HUNT_SILENT_CD = 120;

	private static final double STALK_SPEED = 0.17;
	private static final double STALK_FOLLOW_DIST = 15.5;

	private static final int AGE_THIRD_TO_LAST = 504000;
	private static final int AGE_SECOND_TO_LAST = 576000;
	private static final int AGE_LAST = 648000;

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute() {
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof BlindspotSplinterEntity splinter)) return;

		if (splinter.getTarget() instanceof Player p && (p.isCreative() || p.isSpectator())) {
			splinter.setTarget(null);
		}
		if (splinter.getLastHurtByMob() instanceof Player p && (p.isCreative() || p.isSpectator())) {
			splinter.setLastHurtByMob(null);
		}

		if (!splinter.getPersistentData().getBoolean(K_PERSONALITY_INIT)) {
			int p = (splinter.getRandom().nextInt(100) < ANGEL_CHANCE_PERCENT) ? PERSONALITY_ANGEL : PERSONALITY_STALK_HUNT;
			splinter.getPersistentData().putInt(K_PERSONALITY, p);
			splinter.getPersistentData().putBoolean(K_PERSONALITY_INIT, true);
		}
		int personality = splinter.getPersistentData().getInt(K_PERSONALITY);

		if (splinter.isPassenger()) {
			Entity vehicle = splinter.getVehicle();
			if (vehicle instanceof net.minecraft.world.entity.vehicle.Boat || vehicle instanceof net.minecraft.world.entity.vehicle.ChestBoat) {
				splinter.stopRiding();
				splinter.setDeltaMovement(splinter.getDeltaMovement().add(0, 0.2, 0));
			}
		}

		int age = splinter.getEntityData().get(BlindspotSplinterEntity.DATA_Age);
		if (age >= AGE_LAST) {
			splinter.discard();
			return;
		}

		boolean isDegraded = age >= AGE_THIRD_TO_LAST;
		boolean isCritical = age >= AGE_SECOND_TO_LAST;

		Player foundPlayerDirect = findNearestPlayerInRange(world, splinter.getX(), splinter.getY(), splinter.getZ(), TARGET_RANGE);
		if (world instanceof Level level && level.isClientSide()) {
			if (foundPlayerDirect != null) {
				splinter.setInvisible(isThirdPersonFrontForLocalPlayer(foundPlayerDirect));
			}
			return;
		}
		LivingEntity foundPlayer = foundPlayerDirect;

		LivingEntity prioritizedMob = null;
		String attackerUUIDStr = splinter.getPersistentData().getString("splinter_attacker_uuid");
		if (!attackerUUIDStr.isEmpty() && world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			try {
				java.util.UUID attackerUUID = java.util.UUID.fromString(attackerUUIDStr);
				net.minecraft.world.entity.Entity foundEntity = serverLevel.getEntity(attackerUUID);
				if (foundEntity instanceof LivingEntity livingAttacker && livingAttacker.isAlive() && splinter.distanceToSqr(livingAttacker) < TARGET_RANGE * TARGET_RANGE) {
					boolean isWoodbound = livingAttacker.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
					if (!isWoodbound && !(livingAttacker instanceof Player)) {
						prioritizedMob = livingAttacker;
					}
				}
			} catch (Exception e) {}
		}

		LivingEntity attacker = splinter.getLastHurtByMob();
		if (attacker != null) {
			boolean isWoodbound = attacker.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
			if (isWoodbound) {
				if (splinter.getLastHurtByMob() == attacker) {
					splinter.setLastHurtByMob(null);
				}
				attacker = null;
			} else if (attacker.isAlive() && splinter.distanceToSqr(attacker) < TARGET_RANGE * TARGET_RANGE) {
				if (!(attacker instanceof Player)) {
					prioritizedMob = attacker;
					splinter.getPersistentData().putString("splinter_attacker_uuid", attacker.getUUID().toString());
				}
			}
		}

		boolean hadPrioritizedMob = !attackerUUIDStr.isEmpty();
		if (hadPrioritizedMob && prioritizedMob == null) {
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_isEnraged, 0);
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_watchTimer, 0);
			splinter.getPersistentData().putInt(K_RAGE_BONUS, 0);
			splinter.getPersistentData().putInt(K_FORCED_HUNT_TICKS, 0);
			splinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
			if (splinter.getLastHurtByMob() != null) {
				splinter.setLastHurtByMob(null);
			}
			splinter.getPersistentData().remove("splinter_attacker_uuid");
		}

		if (prioritizedMob != null) {
			foundPlayer = prioritizedMob;
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_isEnraged, 1);
			if (splinter.getTarget() != prioritizedMob) {
				splinter.setTarget(prioritizedMob);
			}
			if (splinter.getLastHurtByMob() != prioritizedMob) {
				splinter.setLastHurtByMob(prioritizedMob);
			}
		} else {
			if (attacker instanceof Player p && p.isAlive() && !p.isCreative() && !p.isSpectator() && splinter.distanceToSqr(attacker) < TARGET_RANGE * TARGET_RANGE) {
				foundPlayer = p;
			}
			if (foundPlayer != null) {
				if (splinter.getTarget() != foundPlayer) {
					splinter.setTarget(foundPlayer);
				}
			} else {
				if (splinter.getTarget() != null) {
					splinter.setTarget(null);
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
			int remainingForcedHunt = splinter.getPersistentData().getInt(K_FORCED_HUNT_TICKS);

			if (remainingForcedHunt <= 0) {
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_watchTimer, 0);
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_isEnraged, 0);
				splinter.getPersistentData().putInt(K_RAGE_BONUS, 0);
				splinter.getPersistentData().putInt(K_HM_PHASE, PHASE_SILENT);
				splinter.getPersistentData().putInt(K_HM_PHASE_TIMER, 0);
				splinter.getPersistentData().putInt(K_FORCED_HUNT_TICKS, 0);
				splinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
				stopNav(splinter);
			} else {
				splinter.getPersistentData().putInt(K_FORCED_HUNT_TICKS, remainingForcedHunt - 1);
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_isEnraged, 1);
				splinter.getPersistentData().putInt(K_HM_PHASE, PHASE_HUNT);
				splinter.getPersistentData().putInt(K_HM_PHASE_TIMER, HUNT_TICKS);
			}
			return;
		}



		int frozenByRose = splinter.getEntityData().get(BlindspotSplinterEntity.DATA_frozenByRose);
		int isEnraged = splinter.getEntityData().get(BlindspotSplinterEntity.DATA_isEnraged);
		int watchTimer = splinter.getEntityData().get(BlindspotSplinterEntity.DATA_watchTimer);

		int forcedHuntTicks = splinter.getPersistentData().getInt(K_FORCED_HUNT_TICKS);
		if (forcedHuntTicks > 0) {
			splinter.getPersistentData().putInt(K_FORCED_HUNT_TICKS, forcedHuntTicks - 1);
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_isEnraged, 1);
			isEnraged = 1;
		}

		if (splinter.getLastHurtByMob() != null) {
			boolean isWoodbound = splinter.getLastHurtByMob().getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
			if (!isWoodbound) {
				int lastSeenHurtTime = splinter.getPersistentData().getInt(K_LAST_HURT_TIME);
				int currentHurtTime = splinter.hurtTime;
				if (currentHurtTime > 0 && currentHurtTime != lastSeenHurtTime) {
					int bonus = splinter.getPersistentData().getInt(K_RAGE_BONUS) + RAGE_THRESHOLD_HIT_BONUS;
					splinter.getPersistentData().putInt(K_RAGE_BONUS, bonus);
					splinter.getPersistentData().putInt(K_LAST_HURT_TIME, currentHurtTime);
				}
			}
		}

		int effectiveRageThreshold = Math.max(1, RAGE_WATCH_THRESHOLD - splinter.getPersistentData().getInt(K_RAGE_BONUS));

		boolean isWatched = false;
		if (foundPlayerDirect != null) {
			Vec3 toSplinter = splinter.getEyePosition().subtract(foundPlayerDirect.getEyePosition());
			if (toSplinter.lengthSqr() > 1.0e-8) toSplinter = toSplinter.normalize();
			double dot = foundPlayerDirect.getLookAngle().normalize().dot(toSplinter);
			isWatched = (dot > WATCH_DOT_THRESHOLD) && foundPlayerDirect.hasLineOfSight(splinter);
		}

		double distToPlayer = foundPlayerDirect != null ? splinter.position().distanceTo(foundPlayerDirect.position()) : Double.MAX_VALUE;
		if (isEnraged == 1 && distToPlayer > RAGE_ESCAPE_RANGE) {
			if (splinter.getPersistentData().getInt(K_FORCED_HUNT_TICKS) <= 0) {
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_isEnraged, 0);
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_watchTimer, 0);
				isEnraged = 0;
			}
		}

		if (isWatched && isEnraged == 0) {
			watchTimer++;
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_watchTimer, watchTimer);
			if (watchTimer >= effectiveRageThreshold) {
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_isEnraged, 1);
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_watchTimer, 0);
				isEnraged = 1;
			}
		} else if (!isWatched && isEnraged == 0) {
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_watchTimer, 0);
		}

		boolean foundRose = false;
		if (foundPlayerDirect != null) {
			foundRose = checkHeldRose(foundPlayerDirect, splinter, world, foundPlayerDirect.getX(), foundPlayerDirect.getY(), foundPlayerDirect.getZ());
		}
		if (!foundRose && splinter.tickCount % 5 == 0) {
			foundRose = checkNearbyRoseBlocks(world, splinter);
		}
		if (foundRose || frozenByRose == 1) {
			setSpeed(splinter, 0);
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_isEnraged, 0);
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_watchTimer, 0);
			splinter.getPersistentData().putInt(K_RAGE_BONUS, 0);
			splinter.getPersistentData().putInt(K_FORCED_HUNT_TICKS, 0);
			splinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
			stopNav(splinter);
			return;
		}

		if (personality == PERSONALITY_ANGEL) {
			if (isWatched && isEnraged == 0) {
				setSpeed(splinter, 0);
				stopNav(splinter);
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineProgress, 0);
				return;
			}

			if (splinter.tickCount % 3 == 0) {
				splinter.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(foundPlayer.getX(), foundPlayer.getEyeY(), foundPlayer.getZ()));
			}

			setSpeed(splinter, isDegraded ? DEGRADED_MOVE_SPEED : ACTIVE_MOVE_SPEED);
			if (splinter instanceof Mob angelMob) {
				navMoveToEntityThrottled(splinter, angelMob, foundPlayer, 1.05, 3);
			}
		} else {
			int phase = splinter.getPersistentData().getInt(K_HM_PHASE);
			int phaseTimer = splinter.getPersistentData().getInt(K_HM_PHASE_TIMER);
			int silentCd = splinter.getPersistentData().getInt(K_HM_SILENT_CD);

			if (phase == PHASE_SILENT && isEnraged == 0 && forcedHuntTicks <= 0) {
				phase = PHASE_STALK;
				phaseTimer = randomBetween(splinter, STALK_MIN_TICKS, STALK_MAX_TICKS);
				splinter.getPersistentData().putInt(K_HM_PHASE, phase);
				splinter.getPersistentData().putInt(K_HM_PHASE_TIMER, phaseTimer);
				splinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
			}

			if (silentCd > 0) {
				silentCd--;
				splinter.getPersistentData().putInt(K_HM_SILENT_CD, silentCd);
			}

			if (isEnraged == 1 || splinter.getPersistentData().getInt(K_FORCED_HUNT_TICKS) > 0) {
				phase = PHASE_HUNT;
				phaseTimer = HUNT_TICKS + 40;
				splinter.getPersistentData().putInt(K_HM_PHASE, phase);
				splinter.getPersistentData().putInt(K_HM_PHASE_TIMER, phaseTimer);
				splinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
			} else {
				phaseTimer--;
				if (phaseTimer <= 0) {
					if (phase == PHASE_HUNT) {
						phase = PHASE_SILENT;
						phaseTimer = randomBetween(splinter, SILENT_MIN_TICKS, SILENT_MAX_TICKS);
						splinter.getPersistentData().putInt(K_HM_SILENT_CD, POST_HUNT_SILENT_CD);
						splinter.getEntityData().set(BlindspotSplinterEntity.DATA_watchTimer, 0);
						splinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
					} else if (phase == PHASE_SILENT) {
						phase = PHASE_STALK;
						phaseTimer = randomBetween(splinter, STALK_MIN_TICKS, STALK_MAX_TICKS);
						splinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
					} else {
						phase = PHASE_SILENT;
						phaseTimer = randomBetween(splinter, SILENT_MIN_TICKS, SILENT_MAX_TICKS);
						splinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, 0);
					}
				}
				splinter.getPersistentData().putInt(K_HM_PHASE, phase);
				splinter.getPersistentData().putInt(K_HM_PHASE_TIMER, phaseTimer);
			}

			if (phase == PHASE_SILENT) {
				setSpeed(splinter, 0);
				stopNav(splinter);
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineProgress, 0);
				return;
			}

			if (splinter.tickCount % 3 == 0) {
				splinter.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(foundPlayer.getX(), foundPlayer.getEyeY(), foundPlayer.getZ()));
			}

			if (phase == PHASE_STALK) {
				setSpeed(splinter, STALK_SPEED);

				int watchTicks = splinter.getPersistentData().getInt(K_STALK_WATCH_TICKS);
				if (watchTicks > 0) {
					splinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, watchTicks - 1);
					stopNav(splinter);
					splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineProgress, 0);
					return;
				}

				if (splinter.getRandom().nextInt(STALK_WATCH_CHANCE) == 0) {
					splinter.getPersistentData().putInt(K_STALK_WATCH_TICKS, randomBetween(splinter, STALK_WATCH_MIN, STALK_WATCH_MAX));
					stopNav(splinter);
					splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineProgress, 0);
					return;
				}

				if (splinter instanceof Mob mob) {
					if (distToPlayer > STALK_FOLLOW_DIST) {
						navMoveToEntityThrottled(splinter, mob, foundPlayer, 0.9, 1);
					} else {
						if (!mob.getNavigation().isDone() && splinter.tickCount % 10 == 0) {
							stopNav(splinter);
						}
					}
				}
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineProgress, 0);
				return;
			}

			setSpeed(splinter, isDegraded ? DEGRADED_MOVE_SPEED : ACTIVE_MOVE_SPEED);
			if (splinter instanceof Mob huntMob) {
				navMoveToEntityThrottled(splinter, huntMob, foundPlayer, 1.05, 3);
			}
		}

		Vec3 splinterEyes = splinter.getEyePosition(1f);
		Vec3 splinterView = splinter.getViewVector(1f);
		Vec3 blockCheckTarget = splinterEyes.add(splinterView.scale(MINE_RAY_DISTANCE));

		HitResult hit = world.clip(new ClipContext(splinterEyes, blockCheckTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, splinter));

		BlockPos facePos;
		BlockPos feetPos;

		if (hit.getType() == HitResult.Type.BLOCK) {
			facePos = ((BlockHitResult) hit).getBlockPos();
			feetPos = new BlockPos(facePos.getX(), Mth.floor(splinter.getY()), facePos.getZ());
		} else {
			Vec3 look = splinter.getLookAngle().normalize();
			int fx = Mth.floor(splinter.getX() + look.x);
			int fz = Mth.floor(splinter.getZ() + look.z);
			feetPos = new BlockPos(fx, Mth.floor(splinter.getY()), fz);
			facePos = new BlockPos(fx, Mth.floor(splinter.getEyeY()), fz);
		}

		boolean canMineFeet = canMine(world, feetPos, foundPlayer);
		boolean canMineFace = canMine(world, facePos, foundPlayer);

		if (canMineFeet || canMineFace) {
			int mineProgress = splinter.getEntityData().get(BlindspotSplinterEntity.DATA_mineProgress) + 1;
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineProgress, mineProgress);

			if (splinter.tickCount % 6 == 0) {
				splinter.swing(InteractionHand.MAIN_HAND);
			}

			BlockPos trackPos = canMineFeet ? feetPos : facePos;
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineX, trackPos.getX());
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineY, trackPos.getY());
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineZ, trackPos.getZ());

			float speedRef = canMineFeet
					? world.getBlockState(feetPos).getDestroySpeed(world, feetPos)
					: world.getBlockState(facePos).getDestroySpeed(world, facePos);

			float mineThreshold = isDegraded
					? speedRef * DEGRADED_MINE_SPEED_MULTIPLIER + DEGRADED_MINE_SPEED_BASE
					: speedRef * MINE_SPEED_MULTIPLIER + MINE_SPEED_BASE;

			if (mineProgress > mineThreshold) {
				if (canMineFeet) world.destroyBlock(feetPos, false);
				if (canMineFace) world.destroyBlock(facePos, false);
				stopNav(splinter);
				splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineProgress, 0);
			}
		} else {
			splinter.getEntityData().set(BlindspotSplinterEntity.DATA_mineProgress, 0);

			double heightDiff = foundPlayer.getY() - splinter.getY();
			double horizontalDist = splinter.position().distanceTo(foundPlayer.position());

			if (!isCritical) {
				if (heightDiff >= 1.4 && horizontalDist < 12) {
					BlockPos under = BlockPos.containing(splinter.getX(), splinter.getY() - 1, splinter.getZ());
					BlockPos body = BlockPos.containing(splinter.getX(), splinter.getY(), splinter.getZ());
					BlockPos bodyUp = body.above();

					if (world.getBlockState(under).isAir()) {
						world.setBlock(under, Blocks.OAK_PLANKS.defaultBlockState(), 3);
					}
					if (world.getBlockState(body).is(Blocks.OAK_PLANKS)) {
						world.setBlock(body, Blocks.AIR.defaultBlockState(), 3);
					}
					if (world.getBlockState(bodyUp).is(Blocks.OAK_PLANKS)) {
						world.setBlock(bodyUp, Blocks.AIR.defaultBlockState(), 3);
					}

					world.destroyBlock(BlockPos.containing(splinter.getX(), splinter.getY() + 2, splinter.getZ()), false);
					world.destroyBlock(BlockPos.containing(splinter.getX(), splinter.getY() + 3, splinter.getZ()), false);

					if (splinter.onGround()) {
						splinter.setDeltaMovement(new Vec3(splinter.getDeltaMovement().x(), 0.4, splinter.getDeltaMovement().z()));
					}
					splinter.fallDistance = 0;
				} else if (heightDiff > -0.5 && heightDiff < 2.5) {
					Vec3 look = splinter.getLookAngle();
					BlockPos bridgePos = BlockPos.containing(splinter.getX() + look.x, splinter.getY() - 1, splinter.getZ() + look.z);
					if (!(world.getBlockFloorHeight(bridgePos) > 0)) {
						world.setBlock(bridgePos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
					}
				}
			}
		}

		// Melee reach range increase for non-players only (by 50%)
		LivingEntity target = splinter.getTarget();
		if (target != null && target.isAlive() && !(target instanceof Player)) {
			double reach = splinter.getBbWidth() + target.getBbWidth() + 0.8;
			double increasedReach = reach * 1.5;
			double distSqr = splinter.distanceToSqr(target);
			if (distSqr <= increasedReach * increasedReach) {
				int lastAttackTick = splinter.getPersistentData().getInt("last_melee_attack_tick");
				if (splinter.tickCount - lastAttackTick >= 20) {
					splinter.doHurtTarget(target);
					splinter.swing(InteractionHand.MAIN_HAND);
					splinter.getPersistentData().putInt("last_melee_attack_tick", splinter.tickCount);
				}
			}
		}
	}

	private static boolean isThirdPersonFrontForLocalPlayer(Player player) {
		try {
			Object minecraft = Class.forName("net.minecraft.client.Minecraft").getMethod("getInstance").invoke(null);
			Object localPlayer = minecraft.getClass().getField("player").get(minecraft);
			if (localPlayer == null || localPlayer != player) return false;
			Object options = minecraft.getClass().getField("options").get(minecraft);
			Object cameraType = options.getClass().getMethod("getCameraType").invoke(options);
			boolean isMirrored = (boolean) cameraType.getClass().getMethod("isMirrored").invoke(cameraType);
			boolean isFirstPerson = (boolean) cameraType.getClass().getMethod("isFirstPerson").invoke(cameraType);
			return isMirrored && !isFirstPerson;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static int randomBetween(BlindspotSplinterEntity entity, int min, int max) {
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

	private static boolean checkHeldRose(Entity holder, BlindspotSplinterEntity splinter, LevelAccessor world, double x, double y, double z) {
		if (!(holder instanceof LivingEntity living)) return false;

		ItemStack main = living.getMainHandItem();
		ItemStack off = living.getOffhandItem();
		boolean mainIsRose = main.getItem() == TheBackwoodsModBlocks.ASH_ROSE.get().asItem();
		boolean offIsRose = off.getItem() == TheBackwoodsModBlocks.ASH_ROSE.get().asItem();

		if (!mainIsRose && !offIsRose) return false;

		setSpeed(splinter, 0);
		stopNav(splinter);

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

	private static boolean checkNearbyRoseBlocks(LevelAccessor world, BlindspotSplinterEntity splinter) {
		BlockPos base = splinter.blockPosition();
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

	private static void stopNav(BlindspotSplinterEntity splinter) {
		if (splinter instanceof Mob mob) {
			mob.getNavigation().stop();
		}
		splinter.getPersistentData().putInt(K_NAV_MODE, 0);
	}

	private static void navMoveToEntityThrottled(BlindspotSplinterEntity splinter, Mob mob, Entity target, double speed, int mode) {
		navMoveToPosThrottled(splinter, mob, target.getX(), target.getY(), target.getZ(), speed, mode);
	}

	private static void navMoveToPosThrottled(BlindspotSplinterEntity splinter, Mob mob, double tx, double ty, double tz, double speed, int mode) {
		int now = splinter.tickCount;
		int lastTick = splinter.getPersistentData().getInt(K_NAV_LAST_TICK);
		int lastMode = splinter.getPersistentData().getInt(K_NAV_MODE);

		double lastTx = splinter.getPersistentData().getDouble(K_NAV_TX);
		double lastTy = splinter.getPersistentData().getDouble(K_NAV_TY);
		double lastTz = splinter.getPersistentData().getDouble(K_NAV_TZ);

		boolean timerReady = (now - lastTick) >= PATH_RECALC_INTERVAL_TICKS;
		boolean modeChanged = lastMode != mode;
		boolean noLast = (lastTick == 0 && lastMode == 0 && lastTx == 0.0 && lastTy == 0.0 && lastTz == 0.0);

		double movedSqr = noLast ? Double.MAX_VALUE : sqr(tx - lastTx) + sqr(ty - lastTy) + sqr(tz - lastTz);
		boolean targetMovedEnough = movedSqr >= TARGET_MOVE_REPATH_DIST_SQR;

		if (modeChanged || timerReady || targetMovedEnough || mob.getNavigation().isDone()) {
			mob.getNavigation().moveTo(tx, ty, tz, speed);
			splinter.getPersistentData().putInt(K_NAV_LAST_TICK, now);
			splinter.getPersistentData().putInt(K_NAV_MODE, mode);
			splinter.getPersistentData().putDouble(K_NAV_TX, tx);
			splinter.getPersistentData().putDouble(K_NAV_TY, ty);
			splinter.getPersistentData().putDouble(K_NAV_TZ, tz);
		}
	}

	private static double sqr(double v) {
		return v * v;
	}
}