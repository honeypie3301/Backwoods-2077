package net.mcreator.thebackwoods.procedures;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.mcreator.thebackwoods.entity.HollowEntity;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Comparator;

@EventBusSubscriber
public class HollowOnEntityTickUpdateProcedure {

	// In-code toggle for debug chat commands ("debug", "appear", "rush")
	private static final boolean DEBUG_COMMANDS_ENABLED = false;

	@SubscribeEvent
	public static void onServerChat(ServerChatEvent event) {
		if (!DEBUG_COMMANDS_ENABLED) return;

		ServerPlayer player = event.getPlayer();
		if (player == null) return;

		String msg = event.getRawText().trim().toLowerCase(java.util.Locale.ROOT);
		CompoundTag pData = player.getPersistentData();
		boolean debugEnabled = persistentBoolean(pData, "hollow_debug_mode", false);

		if (msg.equals("debug") || msg.equals("hollow debug") || msg.equals("hollowdebug")) {
			debugEnabled = !debugEnabled;
			pData.putBoolean("hollow_debug_mode", debugEnabled);
			player.sendSystemMessage(Component.literal("§e[Hollow Debug] Debug mode: " + (debugEnabled ? "§aENABLED" : "§cDISABLED")));
			return;
		}

		if (debugEnabled || msg.equals("appear") || msg.equals("rush")) {
			if (msg.equals("appear")) {
				player.sendSystemMessage(Component.literal("§e[Hollow Debug] Executing 'appear'..."));
				triggerAppearDebug(player.level(), player);
			} else if (msg.equals("rush")) {
				player.sendSystemMessage(Component.literal("§e[Hollow Debug] Executing 'rush'..."));
				triggerRushDebug(player.level(), player);
			}
		}
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !entity.isAlive() || entity.isRemoved())
			return;

		if (!(entity instanceof HollowEntity mob))
			return;

		if (world instanceof net.minecraft.world.level.Level lvl && lvl.isClientSide())
			return;

		CompoundTag mData = mob.getPersistentData();

		// World Difficulty Scaling
		net.minecraft.world.Difficulty diff = (world instanceof net.minecraft.world.level.Level lvl) ? lvl.getDifficulty() : net.minecraft.world.Difficulty.NORMAL;
		int triggerChance = switch (diff) {
			case EASY -> 540;
			case HARD -> 180;
			default -> 360;
		};
		double scaledRushSpeed = switch (diff) {
			case EASY -> 0.42;
			case HARD -> 0.60;
			default -> 0.50;
		};

		// Decrement Scare Cooldown on Mob
		int scareCd = persistentInt(mData, "scare_cooldown", 0);
		if (scareCd > 0) {
			mData.putInt("scare_cooldown", scareCd - 1);
		}

		// Decrement Scare Cooldown on Nearby Players
		List<Player> nearbyPlayersForCd = world.getEntitiesOfClass(Player.class, new AABB(x - 64, y - 32, z - 64, x + 64, y + 32, z + 64));
		for (Player p : nearbyPlayersForCd) {
			CompoundTag pTag = p.getPersistentData();
			int pCd = persistentInt(pTag, "hollow_player_scare_cooldown", 0);
			if (pCd > 0) {
				pTag.putInt("hollow_player_scare_cooldown", pCd - 1);
			}
		}

		boolean isAppearing = persistentBoolean(mData, "is_appearing", false);
		boolean isRushing = persistentBoolean(mData, "is_rushing", false);

		// --- PHASE 1: APPEARING / STANDING STILL PHASE (1 SECOND PAUSE) ---
		if (isAppearing) {
			int appearTicks = persistentInt(mData, "appear_ticks", 0) + 1;
			mData.putInt("appear_ticks", appearTicks);

			String targetUUID = persistentString(mData, "target_player_uuid", "");
			Player targetPlayer = null;
			if (!targetUUID.isEmpty()) {
				for (Player p : world.players()) {
					if (p.getUUID().toString().equals(targetUUID)) {
						targetPlayer = p;
						break;
					}
				}
			}

			// Validate Target Player
			if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isCreative() || targetPlayer.isSpectator() || mob.distanceTo(targetPlayer) > 40.0) {
				performVanish(world, mob, null, mData);
				return;
			}

			// Face target player while standing menacingly
			double dx = targetPlayer.getX() - mob.getX();
			double dz = targetPlayer.getZ() - mob.getZ();
			float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
			mob.setYRot(targetYaw);
			mob.yRotO = targetYaw;
			mob.setYBodyRot(targetYaw);
			mob.yBodyRotO = targetYaw;
			mob.setYHeadRot(targetYaw);
			mob.yHeadRotO = targetYaw;
			mob.getLookControl().setLookAt(targetPlayer, 100.0F, 100.0F);

			// Freeze velocity so it stands in place
			mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);

			// After 20 ticks (1 full second), transition to Rushing!
			if (appearTicks >= 20) {
				mData.putBoolean("is_appearing", false);
				mData.putBoolean("is_rushing", true);
				mData.putInt("rush_ticks", 0);
				mData.putDouble("rush_last_x", mob.getX());
				mData.putDouble("rush_last_z", mob.getZ());
				mData.putInt("rush_stuck_ticks", 0);

				// Screech sound on rush start
				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
					SoundEvents.PHANTOM_SWOOP,
					SoundSource.HOSTILE,
					1.2F, 0.6F);
			}

			return;
		}

		// --- PHASE 2: RUSHING PHASE (RUNNING DIRECTLY AT PLAYER) ---
		if (isRushing) {
			int rushTicks = persistentInt(mData, "rush_ticks", 0) + 1;
			mData.putInt("rush_ticks", rushTicks);

			String targetUUID = persistentString(mData, "target_player_uuid", "");
			Player targetPlayer = null;
			if (!targetUUID.isEmpty()) {
				for (Player p : world.players()) {
					if (p.getUUID().toString().equals(targetUUID)) {
						targetPlayer = p;
						break;
					}
				}
			}

			// Validate Target Player
			if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isCreative() || targetPlayer.isSpectator() || mob.distanceTo(targetPlayer) > 40.0) {
				performVanish(world, mob, null, mData);
				return;
			}

			// Distance & Rotation toward target player
			double dx = targetPlayer.getX() - mob.getX();
			double dy = targetPlayer.getY() - mob.getY();
			double dz = targetPlayer.getZ() - mob.getZ();
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

			float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
			mob.setYRot(targetYaw);
			mob.yRotO = targetYaw;
			mob.setYBodyRot(targetYaw);
			mob.yBodyRotO = targetYaw;
			mob.setYHeadRot(targetYaw);
			mob.yHeadRotO = targetYaw;
			mob.getLookControl().setLookAt(targetPlayer, 100.0F, 100.0F);

			// Proximity or Timeout Vanish Check
			boolean reachedPlayer = (dist <= 1.3 || mob.getBoundingBox().inflate(0.3).intersects(targetPlayer.getBoundingBox()));
			if (reachedPlayer || rushTicks >= 50) {
				performVanish(world, mob, targetPlayer, mData);
				return;
			}

			// Obstacle / Stuck Detection during rush
			double lastX = persistentDouble(mData, "rush_last_x", mob.getX());
			double lastZ = persistentDouble(mData, "rush_last_z", mob.getZ());
			double distMoved = Math.sqrt((mob.getX() - lastX) * (mob.getX() - lastX) + (mob.getZ() - lastZ) * (mob.getZ() - lastZ));
			mData.putDouble("rush_last_x", mob.getX());
			mData.putDouble("rush_last_z", mob.getZ());

			int stuckTicks = persistentInt(mData, "rush_stuck_ticks", 0);
			if (distMoved < 0.01 && rushTicks > 5) {
				stuckTicks++;
			} else if (distMoved >= 0.01) {
				stuckTicks = 0;
			}
			mData.putInt("rush_stuck_ticks", stuckTicks);

			if (stuckTicks > 15) {
				performVanish(world, mob, targetPlayer, mData);
				return;
			}

			// Strobe Teleportation (The Flicker) & Gap/Cliff Prevention
			boolean facingGap = isFacingGapOrCliff(world, mob, 2.5);
			boolean periodicFlicker = (rushTicks % 8 == 0); // strobe flicker every 8 ticks

			if (facingGap || periodicFlicker) {
				double maxTeleportDist = facingGap ? 8.0 : 5.0;
				Vec3 teleportSpot = findStrobeTeleportSpot(world, mob, targetPlayer, 2.0, maxTeleportDist);
				if (teleportSpot != null) {
					// Play vanish effect at current location
					if (world instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1.0, mob.getZ(), 10, 0.2, 0.4, 0.2, 0.1);
						serverLevel.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1.0, mob.getZ(), 10, 0.2, 0.4, 0.2, 0.05);
					}
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
						SoundEvents.ENDERMAN_TELEPORT,
						SoundSource.HOSTILE,
						0.8F, 1.4F);

					// Teleport to the closer spot
					mob.teleportTo(teleportSpot.x, teleportSpot.y, teleportSpot.z);

					// Update rotation to face player at new location
					double ndx = targetPlayer.getX() - mob.getX();
					double ndz = targetPlayer.getZ() - mob.getZ();
					float nYaw = (float) (Math.atan2(ndz, ndx) * 180.0 / Math.PI) - 90.0F;
					mob.setYRot(nYaw);
					mob.yRotO = nYaw;
					mob.setYBodyRot(nYaw);
					mob.yBodyRotO = nYaw;
					mob.setYHeadRot(nYaw);
					mob.yHeadRotO = nYaw;

					// Play reappear effect at new location
					if (world instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1.0, mob.getZ(), 15, 0.2, 0.4, 0.2, 0.1);
					}
					mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
						SoundEvents.CHORUS_FRUIT_TELEPORT,
						SoundSource.HOSTILE,
						0.9F, 1.2F);

					// Reset stuck ticks
					stuckTicks = 0;
					mData.putInt("rush_stuck_ticks", 0);
				} else if (facingGap) {
					// Facing gap, but can't find a safe place to teleport closer.
					// Stop velocity to prevent falling down, then vanish!
					mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
					mob.getNavigation().stop();
					performVanish(world, mob, targetPlayer, mData);
					return;
				}
			}

			// Rush Movement: pathing + direct velocity boost towards player
			mob.getNavigation().moveTo(targetPlayer, 1.8);
			if (dist > 0.01) {
				double vx = (dx / dist) * scaledRushSpeed;
				double vz = (dz / dist) * scaledRushSpeed;
				mob.setDeltaMovement(vx, mob.getDeltaMovement().y, vz);
			}

			// Faint rush audio feedback
			if (mob.tickCount % 5 == 0) {
				mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
					SoundEvents.PHANTOM_FLAP,
					SoundSource.HOSTILE,
					0.5F, 1.6F);
			}

			return;
		}

		// --- IDLE / NATURAL TRIGGER PHASE ---
		if (scareCd > 0)
			return;

		// Scan for candidate players
		List<Player> nearbyPlayers = world.getEntitiesOfClass(Player.class, new AABB(x - 32, y - 16, z - 32, x + 32, y + 16, z + 32));
		if (nearbyPlayers.isEmpty())
			return;

		// Filter candidate players (alive, survival/adventure, cannot see sky, staring at Hollow, AND no active per-player cooldown)
		List<Player> validCandidates = new java.util.ArrayList<>();
		for (Player p : nearbyPlayers) {
			if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;

			// Per-player scare cooldown check for multiplayer fairness
			int pScareCd = persistentInt(p.getPersistentData(), "hollow_player_scare_cooldown", 0);
			if (pScareCd > 0) continue;

			BlockPos pPos = BlockPos.containing(p.getX(), p.getY(), p.getZ());
			if (world.canSeeSkyFromBelowWater(pPos)) continue;

			if (isPlayerStaring(world, p, mob)) {
				validCandidates.add(p);
			}
		}

		if (validCandidates.isEmpty())
			return;

		// Rare chance roll while staring at the Hollow
		if (mob.getRandom().nextInt(triggerChance) != 0)
			return;

		// Sort by distance to pick nearest valid candidate
		validCandidates.sort(Comparator.comparingDouble(p -> p.distanceToSqr(mob)));
		Player targetPlayer = validCandidates.get(0);

		startScareSequence(world, mob, targetPlayer);
	}

	private static boolean isPlayerStaring(LevelAccessor world, Player player, HollowEntity mob) {
		if (!player.isAlive() || player.isCreative() || player.isSpectator()) return false;

		double distSqr = player.distanceToSqr(mob);
		if (distSqr > 32.0 * 32.0 || distSqr < 0.5) return false;

		Vec3 playerEye = player.getEyePosition();
		Vec3 mobCenter = mob.getBoundingBox().getCenter();
		Vec3 toMob = mobCenter.subtract(playerEye).normalize();
		Vec3 playerLook = player.getLookAngle().normalize();

		double dot = playerLook.dot(toMob);
		if (dot < 0.75) return false; // Must be looking in Hollow's direction

		net.minecraft.world.level.ClipContext clipCtx = new net.minecraft.world.level.ClipContext(
			playerEye,
			mobCenter,
			net.minecraft.world.level.ClipContext.Block.COLLIDER,
			net.minecraft.world.level.ClipContext.Fluid.NONE,
			player
		);
		return world.clip(clipCtx).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
	}

	private static boolean isPositionValidGroundAndClear(LevelAccessor world, BlockPos pos) {
		BlockPos posBelow = pos.below();
		BlockState stateBelow = world.getBlockState(posBelow);
		if (stateBelow.isAir() || stateBelow.getCollisionShape(world, posBelow).isEmpty()) {
			return false;
		}
		for (int h = 0; h < 2; h++) {
			BlockPos checkPos = pos.above(h);
			BlockState stateAt = world.getBlockState(checkPos);
			if (!stateAt.isAir() && stateAt.blocksMotion()) {
				return false;
			}
		}
		return true;
	}

	private static void startScareSequence(LevelAccessor world, HollowEntity mob, Player targetPlayer) {
		CompoundTag mData = mob.getPersistentData();

		// Face target player immediately
		double dx = targetPlayer.getX() - mob.getX();
		double dz = targetPlayer.getZ() - mob.getZ();
		float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
		mob.setYRot(targetYaw);
		mob.yRotO = targetYaw;
		mob.setYBodyRot(targetYaw);
		mob.yBodyRotO = targetYaw;
		mob.setYHeadRot(targetYaw);
		mob.yHeadRotO = targetYaw;

		// Atmospheric cave sound cue
		mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
			SoundEvents.AMBIENT_CAVE,
			SoundSource.HOSTILE,
			0.6F, 0.5F);

		// Apply per-player scare cooldown (600 ticks = 30 seconds)
		targetPlayer.getPersistentData().putInt("hollow_player_scare_cooldown", 600);

		// Transition to Appearing state (stands for 20 ticks before rushing)
		mData.putBoolean("is_appearing", true);
		mData.putInt("appear_ticks", 0);
		mData.putBoolean("is_rushing", false);
		mData.putInt("rush_ticks", 0);
		mData.putString("target_player_uuid", targetPlayer.getUUID().toString());
	}

	private static void triggerAppearDebug(LevelAccessor world, Player player) {
		HollowEntity mob = getOrCreateHollow(world, player);
		if (mob == null) return;

		CompoundTag mData = mob.getPersistentData();
		mData.putInt("scare_cooldown", 0);

		// Teleport ~6 blocks in front of player if not staring, with ground & clearance validation
		if (!isPlayerStaring(world, player, mob)) {
			Vec3 lookVec = player.getLookAngle().normalize();
			Vec3 validSpot = null;

			for (double d = 6.0; d >= 3.0; d -= 0.5) {
				double candX = player.getX() + lookVec.x * d;
				double candY = player.getY();
				double candZ = player.getZ() + lookVec.z * d;
				BlockPos candPos = BlockPos.containing(candX, candY, candZ);

				if (isPositionValidGroundAndClear(world, candPos)) {
					validSpot = new Vec3(candX, candY, candZ);
					break;
				}
			}

			if (validSpot != null) {
				mob.teleportTo(validSpot.x, validSpot.y, validSpot.z);
			}
		}

		startScareSequence(world, mob, player);
	}

	private static void triggerRushDebug(LevelAccessor world, Player player) {
		HollowEntity mob = getOrCreateHollow(world, player);
		if (mob == null) return;

		CompoundTag mData = mob.getPersistentData();
		mData.putInt("scare_cooldown", 0);

		mData.putBoolean("is_appearing", false);
		mData.putBoolean("is_rushing", true);
		mData.putInt("rush_ticks", 0);
		mData.putString("target_player_uuid", player.getUUID().toString());
		mData.putDouble("rush_last_x", mob.getX());
		mData.putDouble("rush_last_z", mob.getZ());
		mData.putInt("rush_stuck_ticks", 0);

		mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
			SoundEvents.PHANTOM_SWOOP,
			SoundSource.HOSTILE,
			1.2F, 0.6F);
	}

	private static HollowEntity getOrCreateHollow(LevelAccessor world, Player player) {
		List<HollowEntity> list = world.getEntitiesOfClass(HollowEntity.class, new AABB(player.getX() - 64, player.getY() - 32, player.getZ() - 64, player.getX() + 64, player.getY() + 32, player.getZ() + 64));
		if (!list.isEmpty()) {
			return list.get(0);
		}
		if (world instanceof ServerLevel serverLevel) {
			Entity newEntity = TheBackwoodsModEntities.HOLLOW.get().create(serverLevel);
			if (newEntity instanceof HollowEntity hollow) {
				Vec3 lookVec = player.getLookAngle().normalize();
				Vec3 validSpot = null;

				for (double d = 6.0; d >= 3.0; d -= 0.5) {
					double candX = player.getX() + lookVec.x * d;
					double candY = player.getY();
					double candZ = player.getZ() + lookVec.z * d;
					BlockPos candPos = BlockPos.containing(candX, candY, candZ);

					if (isPositionValidGroundAndClear(world, candPos)) {
						validSpot = new Vec3(candX, candY, candZ);
						break;
					}
				}

				if (validSpot == null) {
					validSpot = new Vec3(player.getX() + lookVec.x * 5.0, player.getY(), player.getZ() + lookVec.z * 5.0);
				}

				hollow.teleportTo(validSpot.x, validSpot.y, validSpot.z);
				hollow.setYRot(player.getYRot());
				hollow.setXRot(player.getXRot());
				serverLevel.addFreshEntity(hollow);
				return hollow;
			}
		}
		return null;
	}

	private static boolean isFacingGapOrCliff(LevelAccessor world, Mob mob, double checkDistance) {
		Vec3 look = mob.getLookAngle().normalize();
		double mobX = mob.getX();
		double mobY = mob.getY();
		double mobZ = mob.getZ();

		for (double d = 1.0; d <= checkDistance; d += 1.0) {
			double cx = mobX + look.x * d;
			double cy = mobY;
			double cz = mobZ + look.z * d;
			BlockPos basePos = BlockPos.containing(cx, cy, cz);

			boolean solidBelow = false;
			for (int yOffset = 0; yOffset >= -4; yOffset--) {
				BlockPos checkPos = basePos.offset(0, yOffset, 0);
				BlockState bs = world.getBlockState(checkPos);
				if (!bs.isAir() && !bs.getCollisionShape(world, checkPos).isEmpty()) {
					solidBelow = true;
					break;
				}
			}
			if (!solidBelow) {
				return true; // Found a gap where there's no floor
			}
		}
		return false;
	}

	private static Vec3 findStrobeTeleportSpot(LevelAccessor world, Mob mob, Player targetPlayer, double minProgress, double maxProgress) {
		Vec3 start = mob.position();
		Vec3 target = targetPlayer.position();
		Vec3 dir = target.subtract(start);
		double fullDist = dir.length();
		if (fullDist < 1.5) return null;

		// Try various progress distances from max down to min
		for (double d = maxProgress; d >= minProgress; d -= 0.5) {
			if (d > fullDist - 1.0) continue; // Don't teleport too close or past the player

			double ratio = d / fullDist;
			double candX = start.x + dir.x * ratio;
			double candY = start.y + dir.y * ratio;
			double candZ = start.z + dir.z * ratio;

			// Scan vertically around the calculated height for a valid safe spot
			for (int yOffset = 2; yOffset >= -3; yOffset--) {
				BlockPos candPos = BlockPos.containing(candX, candY + yOffset, candZ);
				if (isPositionValidGroundAndClear(world, candPos)) {
					return new Vec3(candX, candY + yOffset, candZ);
				}
			}
		}
		return null;
	}

	private static void performVanish(LevelAccessor world, Mob mob, @Nullable Player targetPlayer, CompoundTag mData) {
		if (targetPlayer != null) {
			// Subtle scare effect on player
			targetPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false, true));
			mob.level().playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
				SoundEvents.PHANTOM_SWOOP,
				SoundSource.HOSTILE,
				1.8F, 0.5F);
		}

		// Vanish sound & particle burst
		mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
			SoundEvents.ENDERMAN_TELEPORT,
			SoundSource.HOSTILE,
			1.5F, 0.4F);

		if (world instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1.0, mob.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
			serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 1.0, mob.getZ(), 15, 0.3, 0.6, 0.3, 0.05);
		}

		mData.putBoolean("is_appearing", false);
		mData.putBoolean("is_rushing", false);
		mData.putInt("scare_cooldown", 300); // 15-second cooldown on mob

		mob.discard();
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
} // 1.21.1
