package net.mcreator.thebackwoods.procedures;

import net.mcreator.thebackwoods.entity.LignumGigasEntity;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LignumGigasNaturalEntitySpawningConditionProcedure {

	private static final int RING_RADIUS = 200;
	private static final int GIGAS_COUNT = 7;
	private static final int EXISTING_SCAN_RADIUS = 950;

	private static final float RARE_CHANCE = 0.00001f;
	private static long lastRingSpawnGameTime = -999999L;
	private static final long RING_SPAWN_COOLDOWN_TICKS = 20L * 90L; // multiplied by seconds

	private static final int MIN_Y = 63;
	private static final int EXTRA_SPAWN_Y = 0; // slight lift so giant feet don't clip

	// candidate search around each ring slot
	private static final int CANDIDATE_TRIES = 12;
	private static final int CANDIDATE_JITTER_RADIUS = 10;

	// slope/roughness rejection
	private static final int SLOPE_SAMPLE_RADIUS = 8;
	private static final int MAX_HEIGHT_DELTA = 9;

	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof ServerLevel level)) return false;

		long now = level.getGameTime();
		if (now - lastRingSpawnGameTime < RING_SPAWN_COOLDOWN_TICKS) return false;

		BlockPos center = BlockPos.containing(x, y, z);

		boolean allowedBiome =
				world.getBiome(center).is(ResourceLocation.fromNamespaceAndPath("the_backwoods", "wood_plains"))
						|| world.getBiome(center).is(ResourceLocation.fromNamespaceAndPath("the_backwoods", "the_thicket"));

		if (!allowedBiome) return false;
		if (level.random.nextFloat() > RARE_CHANCE) return false;

		boolean hasNearby = !level.getEntitiesOfClass(
				LignumGigasEntity.class,
				new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(EXISTING_SCAN_RADIUS / 2d),
				e -> true
		).isEmpty();
		if (hasNearby) return false;

		double startAngle = level.random.nextDouble() * (Math.PI * 2.0);
		int spawnedCount = 0;

		for (int i = 0; i < GIGAS_COUNT; i++) {
			double angle = startAngle + (Math.PI * 2.0 * i / GIGAS_COUNT);

			int baseX = Mth.floor(x + Math.cos(angle) * RING_RADIUS);
			int baseZ = Mth.floor(z + Math.sin(angle) * RING_RADIUS);

			boolean spawnedThisSlot = false;

			// Try several nearby candidates so we avoid mountain walls/overhang intersections
			for (int attempt = 0; attempt < CANDIDATE_TRIES; attempt++) {
				int sx = baseX;
				int sz = baseZ;

				if (attempt > 0) {
					int jx = level.random.nextInt(CANDIDATE_JITTER_RADIUS * 2 + 1) - CANDIDATE_JITTER_RADIUS;
					int jz = level.random.nextInt(CANDIDATE_JITTER_RADIUS * 2 + 1) - CANDIDATE_JITTER_RADIUS;
					sx += jx;
					sz += jz;
				}

				int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
				if (sy < MIN_Y) sy = MIN_Y;

				// Reject steep/cliffy terrain (common source of "inside mountain" spawns for huge mobs)
				int hN = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz - SLOPE_SAMPLE_RADIUS);
				int hS = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz + SLOPE_SAMPLE_RADIUS);
				int hW = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx - SLOPE_SAMPLE_RADIUS, sz);
				int hE = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx + SLOPE_SAMPLE_RADIUS, sz);

				int minH = Math.min(Math.min(hN, hS), Math.min(hW, hE));
				int maxH = Math.max(Math.max(hN, hS), Math.max(hW, hE));
				if (maxH - minH > MAX_HEIGHT_DELTA) continue;

				BlockPos spawnPos = new BlockPos(sx, sy + EXTRA_SPAWN_Y, sz);

				// Preview collision checks at multiple heights to approximate full 80-block body clearance
				LignumGigasEntity preview = TheBackwoodsModEntities.LIGNUM_GIGAS.get().create(level);
				if (preview == null) continue;

				double px = spawnPos.getX() + 0.5;
				double py = spawnPos.getY();
				double pz = spawnPos.getZ() + 0.5;

				preview.setPos(px, py, pz);
				boolean clearBase = level.noCollision(preview) && !level.containsAnyLiquid(preview.getBoundingBox());

				preview.setPos(px, py + 20.0, pz);
				boolean clear20 = level.noCollision(preview);

				preview.setPos(px, py + 40.0, pz);
				boolean clear40 = level.noCollision(preview);

				preview.setPos(px, py + 60.0, pz);
				boolean clear60 = level.noCollision(preview);

				if (!(clearBase && clear20 && clear40 && clear60)) continue;

				Entity spawned = TheBackwoodsModEntities.LIGNUM_GIGAS.get().spawn(
						level,
						spawnPos,
						MobSpawnType.MOB_SUMMONED
				);

				if (spawned != null) {
					spawned.setNoGravity(false);
					// Face center
					double dx = x - spawned.getX();
					double dz = z - spawned.getZ();
					float yawToCenter = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;

					spawned.setYRot(yawToCenter);
					spawned.setYBodyRot(yawToCenter);
					spawned.setYHeadRot(yawToCenter);

					spawnedCount++;
					spawnedThisSlot = true;
				}

				if (spawnedThisSlot) break;
			}
		}

		if (spawnedCount > 0) {
			lastRingSpawnGameTime = now;
		} // 1.21.1

		return false;
	}
}