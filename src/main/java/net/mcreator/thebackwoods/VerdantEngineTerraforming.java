/*
 * The code of this mod element is always locked.
 *
 * You can register new events in this class too.
 *
 * If you want to make a plain independent class, create it using
 * Project Browser -> New... and make sure to make the class
 * outside net.mcreator.thebackwoods as this package is managed by MCreator.
 *
 * If you change workspace package, modid or prefix, you will need
 * to manually adapt this file to these changes or remake it.
 *
 * This class will be added in the mod root package.
*/
package net.mcreator.thebackwoods;

import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber
public class VerdantEngineTerraforming {
	public static final ResourceLocation WOOD_PLAINS_RL = ResourceLocation.fromNamespaceAndPath("the_backwoods", "wood_plains");
	public static final ResourceKey<Biome> WOOD_PLAINS_KEY = ResourceKey.create(Registries.BIOME, WOOD_PLAINS_RL);

	public VerdantEngineTerraforming() {
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new VerdantEngineTerraforming();
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
	}

	@EventBusSubscriber
	private static class VerdantEngineTerraformingForgeBusEvents {
		@SubscribeEvent
		public static void serverLoad(ServerStartingEvent event) {
		}
	}

	private static final Set<ChunkPos> PENDING_BIOME_CHUNKS = new HashSet<>();

	/**
	 * Maximum radius for biome terraforming: 16 chunks radius = 256.0 blocks.
	 */
	public static double MAX_BIOME_RADIUS = 256.0;

	/**
	 * Smooth, gradual biome terraforming to the 'wood_plains' biome.
	 * Activates only when the 3x3 chunks around the World Engine reach >= 85% infection.
	 * Once activated, gradually expands the biome radius outwards from the center in smooth quart waves up to 16 chunks.
	 */
	public static void processBiomeTerraforming(ServerLevel level, CompoundTag nbt, double cx, double gy, double cz, double curInfectionRadius) {
		if (level == null || nbt == null) return;

		Holder<Biome> targetBiome = getWoodPlainsHolder(level);
		if (targetBiome == null) return;

		int centerChunkX = Mth.floor(cx) >> 4;
		int centerChunkZ = Mth.floor(cz) >> 4;

		// Freeze-Ray Gate: If the beam core is hitting an unbroken indestructible barrier at the epicenter,
		// biome conversion is completely halted until that barrier is shattered or penetrated.
		double verdantLockedY = nbt.getDouble("verdant_locked_y");
		if (verdantLockedY <= 0) verdantLockedY = gy + 60.0;
		if (isEpicenterBlockedByIndestructibleBarrier(level, cx, gy, cz, verdantLockedY)) {
			return;
		}

		// Step 1: Verify 3x3 Chunk 85% Infection Requirement
		boolean unlocked = nbt.getBoolean("verdant_biome_unlocked");
		if (!unlocked) {
			int checkTimer = nbt.getInt("verdant_biome_check_timer") + 1;
			nbt.putInt("verdant_biome_check_timer", checkTimer);

			if (curInfectionRadius >= 24.0 && checkTimer % 20 == 0) {
				double infectionRatio = calculate3x3InfectionRatio(level, cx, gy, cz, centerChunkX, centerChunkZ);
				if (infectionRatio >= 0.85) {
					unlocked = true;
					nbt.putBoolean("verdant_biome_unlocked", true);
					nbt.putDouble("verdant_biome_radius", 2.0);
				}
			}
		}

		if (!unlocked) return;

		// Step 2: Smooth, gradual radial biome expansion with dynamic catchup (capped at 16 chunks / 256 blocks)
		double effectiveMaxR = Math.min(MAX_BIOME_RADIUS, curInfectionRadius);
		double biomeR = nbt.contains("verdant_biome_radius") ? nbt.getDouble("verdant_biome_radius") : 2.0;
		if (biomeR < effectiveMaxR) {
			double diff = effectiveMaxR - biomeR;
			double speed = Math.min(diff, Math.max(0.20, diff * 0.08));
			biomeR += speed;
			biomeR = Math.min(effectiveMaxR, biomeR);
			nbt.putDouble("verdant_biome_radius", biomeR);
		} else if (biomeR > effectiveMaxR) {
			biomeR = effectiveMaxR;
			nbt.putDouble("verdant_biome_radius", biomeR);
		}

		biomeR = Math.min(MAX_BIOME_RADIUS, biomeR);

		// Step 3: Optimized Quart-Level Biome Terraforming
		// Biomes are stored in 4x4x4 Quart cells (64x faster than per-block).
		int minQuartX = QuartPos.fromBlock(Mth.floor(cx - biomeR));
		int maxQuartX = QuartPos.fromBlock(Mth.floor(cx + biomeR));
		int minQuartZ = QuartPos.fromBlock(Mth.floor(cz - biomeR));
		int maxQuartZ = QuartPos.fromBlock(Mth.floor(cz + biomeR));

		double biomeRSqr = biomeR * biomeR;
		int sweepTimer = nbt.getInt("verdant_biome_sweep_timer") + 1;
		nbt.putInt("verdant_biome_sweep_timer", sweepTimer);
		boolean fullSweep = (sweepTimer % 40 == 0);
		double innerRSqr = (!fullSweep && biomeR > 5.0) ? (biomeR - 4.0) * (biomeR - 4.0) : 0.0;

		// Process an advancing annular ring of quart columns per tick to ensure smooth transition & near-zero CPU
		int quartBudget = 96;
		verdantLockedY = nbt.getDouble("verdant_locked_y");
		if (verdantLockedY <= 0) verdantLockedY = gy + 60.0;

		for (int qx = minQuartX; qx <= maxQuartX && quartBudget > 0; qx++) {
			int bx = QuartPos.toBlock(qx) + 2;
			double dx = bx - cx;
			for (int qz = minQuartZ; qz <= maxQuartZ && quartBudget > 0; qz++) {
				int bz = QuartPos.toBlock(qz) + 2;
				double dz = bz - cz;
				double distSqr = dx * dx + dz * dz;

				if (distSqr <= biomeRSqr && distSqr >= innerRSqr) {
					int chunkX = bx >> 4;
					int chunkZ = bz >> 4;
					if (!level.hasChunk(chunkX, chunkZ)) continue;

					LevelChunk chunk = level.getChunk(chunkX, chunkZ);
					if (chunk == null) continue;

					boolean chunkChanged = applyQuartColumnBiome(level, chunk, qx, qz, bx, bz, verdantLockedY, targetBiome);
					if (chunkChanged) {
						PENDING_BIOME_CHUNKS.add(chunk.getPos());
						chunk.setUnsaved(true);
						quartBudget--;
					}
				}
			}
		}

		// Step 4: Batch Sync Updated Chunk Biomes to Clients without dropping packets
		if (!PENDING_BIOME_CHUNKS.isEmpty()) {
			int syncTimer = nbt.getInt("verdant_biome_sync_timer") + 1;
			nbt.putInt("verdant_biome_sync_timer", syncTimer);

			if (syncTimer % 5 == 0 || biomeR >= curInfectionRadius || PENDING_BIOME_CHUNKS.size() >= 4) {
				List<ChunkAccess> chunkList = new ArrayList<>();
				for (ChunkPos cp : PENDING_BIOME_CHUNKS) {
					if (level.hasChunk(cp.x, cp.z)) {
						LevelChunk c = level.getChunk(cp.x, cp.z);
						if (c != null) chunkList.add(c);
					}
				}
				if (!chunkList.isEmpty()) {
					level.getChunkSource().chunkMap.resendBiomesForChunks(chunkList);
				}
				PENDING_BIOME_CHUNKS.clear();
			}
		}
	}

	private static boolean isEpicenterBlockedByIndestructibleBarrier(ServerLevel level, double cx, double gy, double cz, double verdantY) {
		int bx = Mth.floor(cx);
		int bz = Mth.floor(cz);
		// Only check for barriers obstructing the space between the Verdant and the active ground target
		int bottomY = (int) Math.max(level.getMinBuildHeight() + 2.0, gy - 2.0);
		int scanY = (int) Math.min(level.getMaxBuildHeight() - 1.0, verdantY);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(bx, scanY, bz);

		while (pos.getY() >= bottomY) {
			BlockState st = level.getBlockState(pos);
			if (st.getBlock() == Blocks.BEDROCK || st.getBlock() == Blocks.BARRIER || st.getBlock() == Blocks.REINFORCED_DEEPSLATE) {
				return true; // Still unbroken barrier directly intercepting the beam before reaching ground
			}
			if (isInfectedBlock(st)) {
				return false; // Beam has already converted/breached this column
			}
			pos.move(Direction.DOWN);
		}
		return false;
	}

	private static final java.util.Map<Block, Boolean> INFECTED_BLOCK_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	private static boolean applyQuartColumnBiome(ServerLevel level, LevelChunk chunk, int qx, int qz, int worldBx, int worldBz, double verdantY, Holder<Biome> targetBiome) {
		boolean changed = false;
		int sectionCount = chunk.getSections().length;
		int localBx = QuartPos.toBlock(qx) & 15;
		int localBz = QuartPos.toBlock(qz) & 15;
		int localQx = qx & 3;
		int localQz = qz & 3;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		// Freeze-Ray Indestructible Barrier Check:
		int barrierStopY = (int) level.getMinBuildHeight();
		if (level.dimensionType().hasCeiling()) {
			barrierStopY = findIndestructibleBarrierBelow(level, worldBx, worldBz, verdantY);
		} else {
			barrierStopY = (int) level.getMinBuildHeight();
		}

		for (int sIndex = 0; sIndex < sectionCount; sIndex++) {
			LevelChunkSection section = chunk.getSection(sIndex);
			if (section == null) continue;

			int secBottomY = chunk.getSectionYFromSectionIndex(sIndex) << 4;
			int secTopY = secBottomY + 15;

			if (secTopY < barrierStopY) {
				continue;
			}

			if (!isSectionOrColumnInfected(chunk, localBx, localBz, secBottomY, barrierStopY, pos)) {
				continue;
			}

			try {
				PalettedContainer<Holder<Biome>> container = (PalettedContainer<Holder<Biome>>) section.getBiomes();
				for (int qy = 0; qy < 4; qy++) {
					int quartWorldY = secBottomY + (qy << 2);
					if (quartWorldY < barrierStopY) continue;

					Holder<Biome> current = container.get(localQx, qy, localQz);
					if (current != targetBiome) {
						container.set(localQx, qy, localQz, targetBiome);
						changed = true;
					}
				}
			} catch (Exception ignored) {
			}
		}
		return changed;
	}

	private static boolean isSectionOrColumnInfected(LevelChunk chunk, int localBx, int localBz, int secBottomY, int barrierStopY, BlockPos.MutableBlockPos pos) {
		int worldMinBx = chunk.getPos().getMinBlockX() + localBx;
		int worldMinBz = chunk.getPos().getMinBlockZ() + localBz;

		int checkMinY = Math.max((int) chunk.getLevel().getMinBuildHeight(), secBottomY - 16);
		int checkMaxY = secBottomY + 15;

		for (int ox = 0; ox <= 2; ox += 2) {
			for (int oz = 0; oz <= 2; oz += 2) {
				int worldBx = worldMinBx + ox;
				int worldBz = worldMinBz + oz;

				for (int y = checkMaxY; y >= checkMinY; y--) {
					if (y < barrierStopY) break;
					pos.set(worldBx, y, worldBz);
					BlockState st = chunk.getBlockState(pos);
					if (isInfectedBlock(st)) {
						return true;
					}
					if (y < secBottomY && (st.getBlock() == Blocks.BEDROCK || st.getBlock() == Blocks.BARRIER || st.getBlock() == Blocks.REINFORCED_DEEPSLATE)) {
						break;
					}
				}
			}
		}

		return false;
	}

	private static int findIndestructibleBarrierBelow(ServerLevel level, int bx, int bz, double startY) {
		int minY = (int) level.getMinBuildHeight();
		if (!level.dimensionType().hasCeiling()) {
			return minY;
		}
		int scanY = (int) Math.min(level.getMaxBuildHeight() - 1.0, startY);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(bx, scanY, bz);

		while (pos.getY() > minY + 5) {
			BlockState st = level.getBlockState(pos);
			if (st.getBlock() == Blocks.BEDROCK || st.getBlock() == Blocks.BARRIER || st.getBlock() == Blocks.REINFORCED_DEEPSLATE) {
				return pos.getY();
			}
			pos.move(Direction.DOWN);
		}
		return minY;
	}

	private static double calculate3x3InfectionRatio(ServerLevel level, double cx, double gy, double cz, int centerChunkX, int centerChunkZ) {
		int totalSamples = 0;
		int infectedSamples = 0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		double scanTop = Math.min(level.getMaxBuildHeight() - 5.0, gy + 85.0);

		int startX = (centerChunkX - 1) << 4;
		int startZ = (centerChunkZ - 1) << 4;

		for (int x = startX + 3; x < startX + 48; x += 6) {
			for (int z = startZ + 3; z < startZ + 48; z += 6) {
				pos.set(x, (int) gy, z);
				if (!level.hasChunkAt(pos)) continue;

				totalSamples++;
				int groundY = findSurfaceY(level, x, z, scanTop, gy);
				pos.set(x, groundY, z);
				BlockState state = level.getBlockState(pos);

				if (isInfectedBlock(state)) {
					infectedSamples++;
				}
			}
		}

		return totalSamples > 0 ? ((double) infectedSamples / totalSamples) : 0.0;
	}

	private static boolean isInfectedBlock(BlockState state) {
		if (state == null || state.isAir()) return false;
		Block b = state.getBlock();
		return INFECTED_BLOCK_CACHE.computeIfAbsent(b, block -> {
			if (block == Blocks.OAK_PLANKS || block == Blocks.OAK_LOG || block == Blocks.PETRIFIED_OAK_SLAB) return true;
			ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(block);
			if (loc == null) return false;
			String id = loc.toString().toLowerCase();
			return id.contains("petrified") || id.contains("lignum_caro") || id.contains("splintered") || id.contains("oak_planks") || id.contains("wood_plains") || id.contains("false_oak");
		});
	}

	private static int findSurfaceY(ServerLevel level, int x, int z, double scanTop, double gy) {
		try {
			int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
			boolean hasCeiling = level.dimensionType().hasCeiling();
			int startY = (int) Math.min(scanTop, height);
			if (hasCeiling && height > gy + 35.0) {
				startY = (int) Math.min(height - 1, gy + 30.0);
			}
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int bottom = (int) level.getMinBuildHeight() + 1;
			for (int y = startY; y >= bottom; y--) {
				pos.set(x, y, z);
				BlockState st = level.getBlockState(pos);
				if (!st.isAir()) {
					return y;
				}
			}
			return startY;
		} catch (Exception ignored) {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int top = (int) scanTop;
			int bottom = (int) level.getMinBuildHeight() + 1;
			for (int y = top; y >= bottom; y--) {
				pos.set(x, y, z);
				BlockState st = level.getBlockState(pos);
				if (!st.isAir() && st.getBlock() != Blocks.BEDROCK) {
					return y;
				}
			}
			return (int) scanTop;
		}
	}

	public static Holder<Biome> getWoodPlainsHolder(ServerLevel level) {
		try {
			return level.registryAccess().lookupOrThrow(Registries.BIOME).get(WOOD_PLAINS_KEY).orElse(null);
		} catch (Exception e) {
			return null;
		}
	}
} // 1.21.1