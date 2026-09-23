package net.mcreator.thebackwoods;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.particles.ParticleOptions;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class LowSimDistanceSolution {

	public static boolean ENABLE_CHUNK_FORCING = true;
	public static int FORCED_TICKET_RADIUS = 2;
	public static int TARGET_HEIGHT_OFFSET = 42;
	public static double PARTICLE_MAX_DIST = 1024.0;
	public static double PARTICLE_MAX_DIST_SQ = 1024.0 * 1024.0;
	public static boolean FORCE_LONG_DISTANCE_PARTICLES = true;

	public LowSimDistanceSolution() {
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new LowSimDistanceSolution();
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
	}

	// Tracks active temporary rift chunk tickets to prevent duplicate ticket allocations
	private static final Set<Long> ACTIVE_RIFT_CHUNKS = ConcurrentHashMap.newKeySet();

	/**
	 * Non-blocking, asynchronous chunk loading optimized for C2ME and low simulation distances.
	 * Uses TicketType.PORTAL (auto-expiring in 300 ticks / 15 seconds) instead of permanent setChunkForced.
	 * This completely eliminates main-thread chunk freezes, disk writes to forced_chunks.dat, and chunk leaks.
	 */
	public static void forceLoadRiftArea(ServerLevel level, BlockPos pos) {
		forceLoadRiftArea(level, pos, FORCED_TICKET_RADIUS);
	}

	public static void forceLoadRiftArea(ServerLevel level, BlockPos pos, int radius) {
		if (!ENABLE_CHUNK_FORCING || level == null || pos == null) return;
		try {
			ChunkPos chunkPos = new ChunkPos(pos);
			long chunkKey = chunkPos.toLong();

			// If chunk was previously permanently forced in an older version, unforce it to unburden C2ME/server
			if (level.getForcedChunks().contains(chunkKey) && !EntityTrackerData.isChunkOwnedByTracker(level, chunkPos.x, chunkPos.z)) {
				level.setChunkForced(chunkPos.x, chunkPos.z, false);
			}

			// Add a temporary 300-tick (15s) non-persistent region ticket.
			// This keeps the area loaded and ticking during the rift spawning sequence without writing to disk.
			int ticketRadius = Math.max(1, radius > 0 ? radius : FORCED_TICKET_RADIUS);
			level.getChunkSource().addRegionTicket(TicketType.PORTAL, chunkPos, ticketRadius, pos);
			ACTIVE_RIFT_CHUNKS.add(chunkKey);
		} catch (Throwable ignored) {}
	}

	/**
	 * Explicit cleanup method to release tickets and ensure no chunks remain pinned.
	 */
	public static void unforceRiftArea(ServerLevel level, BlockPos pos) {
		unforceRiftArea(level, pos, FORCED_TICKET_RADIUS);
	}

	public static void unforceRiftArea(ServerLevel level, BlockPos pos, int radius) {
		if (level == null || pos == null) return;
		try {
			ChunkPos chunkPos = new ChunkPos(pos);
			long chunkKey = chunkPos.toLong();
			ACTIVE_RIFT_CHUNKS.remove(chunkKey);
			int ticketRadius = Math.max(1, radius > 0 ? radius : FORCED_TICKET_RADIUS);
			level.getChunkSource().removeRegionTicket(TicketType.PORTAL, chunkPos, ticketRadius, pos);
			if (level.getForcedChunks().contains(chunkKey) && !EntityTrackerData.isChunkOwnedByTracker(level, chunkPos.x, chunkPos.z)) {
				level.setChunkForced(chunkPos.x, chunkPos.z, false);
			}
		} catch (Throwable ignored) {}
	}

	/**
	 * Fast O(1) height calculation using chunk heightmap arrays.
	 * Avoids blocking chunk generation or synchronously stalling worker threads.
	 */
	public static double getOptimalRiftY(ServerLevel level, BlockPos playerPos) {
		if (level == null || playerPos == null) return 100.0;
		int px = playerPos.getX();
		int pz = playerPos.getZ();
		int surfaceY;

		// If chunk is loaded, read directly from the cached chunk heightmap without disk I/O or block decompression
		if (level.getChunkSource().hasChunk(px >> 4, pz >> 4)) {
			surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
		} else {
			surfaceY = playerPos.getY();
		}

		double minY = level.getMinBuildHeight() + 30;
		double maxY = level.getMaxBuildHeight() - 20;
		double calculatedY = surfaceY + TARGET_HEIGHT_OFFSET;
		return Math.min(maxY, Math.max(minY, calculatedY));
	}

	/**
	 * High-efficiency particle dispatcher with Manhattan-distance bounding-box pre-filtering
	 * to prevent network congestion and avoid iterating distant players needlessly.
	 */
	public static <T extends ParticleOptions> void sendFarParticle(ServerLevel level, T particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
		if (level == null || count <= 0) return;
		double maxDist = PARTICLE_MAX_DIST;
		double maxDistSq = PARTICLE_MAX_DIST_SQ;

		for (ServerPlayer player : level.players()) {
			double diffX = Math.abs(player.getX() - x);
			if (diffX > maxDist) continue;
			double diffZ = Math.abs(player.getZ() - z);
			if (diffZ > maxDist) continue;
			double diffY = Math.abs(player.getY() - y);
			if (diffY > maxDist) continue;

			if (diffX * diffX + diffY * diffY + diffZ * diffZ <= maxDistSq) {
				level.sendParticles(player, particle, FORCE_LONG_DISTANCE_PARTICLES, x, y, z, count, dx, dy, dz, speed);
			}
		}
	}

	@EventBusSubscriber
	private static class LowSimDistanceSolutionForgeBusEvents {
		@SubscribeEvent
		public static void serverLoad(ServerStartingEvent event) {
			ACTIVE_RIFT_CHUNKS.clear();
		}
	}
} // 1.21.1