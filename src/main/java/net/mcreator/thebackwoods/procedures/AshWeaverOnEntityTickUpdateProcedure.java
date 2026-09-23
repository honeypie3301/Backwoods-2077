package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;
import net.mcreator.thebackwoods.entity.AshWeaverEntity;
import net.mcreator.thebackwoods.entity.SplinterEntity;
import net.mcreator.thebackwoods.entity.LogSplinterEntity;
import net.mcreator.thebackwoods.entity.HollowEntity;

import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber
public class AshWeaverOnEntityTickUpdateProcedure {

	private static final double PLAYER_RANGE = 32.0;
	private static final double THREAT_RANGE = 32.0;
	private static final double STOP_DISTANCE = 3.0;
	private static final double MOVE_SPEED = 1.45;
	private static final int MAX_NEARBY_ROSES = 3;
	private static final int ROSE_COUNT_RADIUS_XZ = 8;
	private static final int ROSE_COUNT_RADIUS_Y = 3;
	private static final int PLACE_OFFSET_MIN = -3;
	private static final int PLACE_OFFSET_MAX = 3;
	private static final int MIN_ROSE_SPACING = 2;
	private static final double PLACE_CHANCE_PER_TICK = 1.0 / 140.0;
	private static final int PLACE_COOLDOWN_TICKS = 120;

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (!(entity instanceof AshWeaverEntity ashWeaver))
			return;

		// 1. Passive garbage collection for stray unassigned weavers in the world
		if (ashWeaver.tickCount % 100 == 0) {
			String assignedUUIDStr = ashWeaver.getEntityData().get(AshWeaverEntity.DATA_assignedPlayer);
			if (assignedUUIDStr == null || assignedUUIDStr.isEmpty()) {
				Player closestPlayer = world.getNearestPlayer(x, y, z, 128.0, false);
				if (closestPlayer == null) {
					ashWeaver.discard();
					return;
				}
			}
		}

		Player foundPlayer = null;
		String assignedUUIDStr = ashWeaver.getEntityData().get(AshWeaverEntity.DATA_assignedPlayer);

		// 2. High-Performance UUID-based retrieval of assigned player
		if (assignedUUIDStr != null && !assignedUUIDStr.isEmpty()) {
			try {
				java.util.UUID playerUUID = java.util.UUID.fromString(assignedUUIDStr);
				if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
					Entity pEnt = serverLevel.getEntity(playerUUID);
					if (pEnt instanceof Player p) {
						foundPlayer = p;
					}
				}
			} catch (IllegalArgumentException e) {
				ashWeaver.getEntityData().set(AshWeaverEntity.DATA_assignedPlayer, "");
			}
		}

		// Look for a player to assign only if unassigned (once every 10 ticks to avoid heavy spatial queries)
		if (foundPlayer == null && (ashWeaver.tickCount % 10 == 0)) {
			Player nearest = world.getNearestPlayer(x, y, z, PLAYER_RANGE, false);
			if (nearest != null) {
				String playerUUIDStr = nearest.getUUID().toString();
				List<AshWeaverEntity> allWeavers = world.getEntitiesOfClass(AshWeaverEntity.class,
						AABB.ofSize(new Vec3(nearest.getX(), nearest.getY(), nearest.getZ()), PLAYER_RANGE * 2, PLAYER_RANGE * 2, PLAYER_RANGE * 2),
						e -> e != ashWeaver);
				boolean playerAlreadyHasWeaver = allWeavers.stream()
						.anyMatch(w -> playerUUIDStr.equals(w.getEntityData().get(AshWeaverEntity.DATA_assignedPlayer)));
				if (playerAlreadyHasWeaver) {
					ashWeaver.discard();
					return;
				} else {
					ashWeaver.getEntityData().set(AshWeaverEntity.DATA_assignedPlayer, playerUUIDStr);
					foundPlayer = nearest;
				}
			}
		}

		// 3. Movement & Smart Teleportation follow mechanic
		if (foundPlayer != null) {
			double distanceSqr = ashWeaver.distanceToSqr(foundPlayer);
			double distance = Math.sqrt(distanceSqr);

			if (distance > 32.0 && ashWeaver.tickCount % 10 == 0) {
				// Teleport closer if extremely far away to prevent chunk-unload or getting stuck
				double pX = foundPlayer.getX();
				double pY = foundPlayer.getY();
				double pZ = foundPlayer.getZ();
				BlockPos.MutableBlockPos tpPos = new BlockPos.MutableBlockPos();
				boolean tpSuccess = false;
				for (int attempt = 0; attempt < 10; attempt++) {
					int dx = Mth.nextInt(RandomSource.create(), -3, 3);
					int dz = Mth.nextInt(RandomSource.create(), -3, 3);
					int dy = Mth.nextInt(RandomSource.create(), -1, 1);
					tpPos.set(pX + dx, pY + dy, pZ + dz);
					if (world.getBlockState(tpPos).isAir() && world.getBlockState(tpPos.below()).canOcclude()) {
						ashWeaver.teleportTo(pX + dx + 0.5, pY + dy, pZ + dz + 0.5);
						tpSuccess = true;
						break;
					}
				}
				if (tpSuccess && ashWeaver instanceof Mob mob) {
					mob.getNavigation().stop();
				}
			} else if (distance > STOP_DISTANCE) {
				if (ashWeaver.tickCount % 10 == 0 || ashWeaver.getNavigation().isDone()) {
					if (ashWeaver instanceof Mob mob) {
						mob.getNavigation().moveTo(foundPlayer, MOVE_SPEED);
					}
				}
			} else {
				if (ashWeaver instanceof Mob mob) {
					mob.getNavigation().stop();
				}
			}
		}

		int roseCooldown = ashWeaver.getEntityData().get(AshWeaverEntity.DATA_roseCooldown);
		if (roseCooldown > 0) {
			ashWeaver.getEntityData().set(AshWeaverEntity.DATA_roseCooldown, roseCooldown - 1);
			return;
		}

		if (foundPlayer == null)
			return;

		// 4. Thread-Safe and High-Performance Threat Checking (Once every 10 ticks)
		boolean threatNearby = false;
		if (ashWeaver.tickCount % 10 == 0) {
			threatNearby = findEntityInWorldRange(world, SplinterEntity.class, x, y, z, THREAT_RANGE) != null
					|| findEntityInWorldRange(world, LogSplinterEntity.class, x, y, z, THREAT_RANGE) != null
					|| findEntityInWorldRange(world, HollowEntity.class, x, y, z, THREAT_RANGE) != null;
			ashWeaver.getPersistentData().putBoolean("lastThreatStatus", threatNearby);
		} else {
			threatNearby = ashWeaver.getPersistentData().getBoolean("lastThreatStatus");
		}

		if (!threatNearby) {
			ashWeaver.getEntityData().set(AshWeaverEntity.DATA_roseCount, 0);
			return;
		}

		// 5. Optimized Block Scanning (Only once every 20 ticks and only when cooldown is ready)
		int nearbyRoseCount = ashWeaver.getEntityData().get(AshWeaverEntity.DATA_roseCount);
		if (ashWeaver.tickCount % 20 == 0) {
			nearbyRoseCount = countNearbyAshRoses(world, foundPlayer.blockPosition(), ROSE_COUNT_RADIUS_XZ, ROSE_COUNT_RADIUS_Y);
			ashWeaver.getEntityData().set(AshWeaverEntity.DATA_roseCount, nearbyRoseCount);
		}

		if (nearbyRoseCount >= MAX_NEARBY_ROSES)
			return;

		if (Math.random() >= PLACE_CHANCE_PER_TICK)
			return;

		int offsetX = Mth.nextInt(RandomSource.create(), PLACE_OFFSET_MIN, PLACE_OFFSET_MAX);
		int offsetZ = Mth.nextInt(RandomSource.create(), PLACE_OFFSET_MIN, PLACE_OFFSET_MAX);

		BlockPos targetPos = BlockPos.containing(foundPlayer.getX() + offsetX, foundPlayer.getY(), foundPlayer.getZ() + offsetZ);
		BlockPos belowPos = targetPos.below();

		boolean airAtTarget = world.getBlockState(targetPos).isAir();
		boolean solidBelow = world.getBlockFloorHeight(belowPos) > 0;
		boolean spacedFromOtherRoses = !hasNearbyAshRose(world, targetPos, MIN_ROSE_SPACING);

		if (airAtTarget && solidBelow && spacedFromOtherRoses) {
			world.setBlock(targetPos, TheBackwoodsModBlocks.ASH_ROSE.get().defaultBlockState(), 3);
			ashWeaver.getEntityData().set(AshWeaverEntity.DATA_roseCooldown, PLACE_COOLDOWN_TICKS);
			ashWeaver.getEntityData().set(AshWeaverEntity.DATA_roseCount, nearbyRoseCount + 1);
		}
	}

	private static int countNearbyAshRoses(LevelAccessor world, BlockPos center, int rXZ, int rY) {
		int count = 0;
		for (int sx = -rXZ; sx <= rXZ; sx++) {
			for (int sy = -rY; sy <= rY; sy++) {
				for (int sz = -rXZ; sz <= rXZ; sz++) {
					BlockPos check = center.offset(sx, sy, sz);
					if (world.getBlockState(check).getBlock() == TheBackwoodsModBlocks.ASH_ROSE.get()) {
						count++;
					}
				}
			}
		}
		return count;
	}

	private static boolean hasNearbyAshRose(LevelAccessor world, BlockPos center, int radius) {
		for (int sx = -radius; sx <= radius; sx++) {
			for (int sy = -1; sy <= 1; sy++) {
				for (int sz = -radius; sz <= radius; sz++) {
					BlockPos check = center.offset(sx, sy, sz);
					if (world.getBlockState(check).getBlock() == TheBackwoodsModBlocks.ASH_ROSE.get()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range) {
		return world.getEntitiesOfClass(clazz, AABB.ofSize(new Vec3(x, y, z), range, range, range), e -> true)
				.stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z))).findFirst().orElse(null);
	} // 1.21.1
}