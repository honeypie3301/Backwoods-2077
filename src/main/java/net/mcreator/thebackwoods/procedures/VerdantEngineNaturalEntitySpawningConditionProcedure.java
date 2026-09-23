package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import net.mcreator.thebackwoods.entity.VerdantEngineEntity;

import java.util.List;

public class VerdantEngineNaturalEntitySpawningConditionProcedure {

	private static final long GLOBAL_SPAWN_COOLDOWN_TICKS = 12000L;
	private static final long PLAYER_SPAWN_COOLDOWN_TICKS = 48000L;
	private static long lastGlobalSpawnTime = 0L;

	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof ServerLevel level)) {
			return false;
		}

		String dimId = level.dimension().location().toString().toLowerCase();
		if (dimId.contains("the_backwoods") || dimId.contains("backwood")) {
			return false;
		}

		boolean isTestMode = TrackBackwoodsTransgressionProcedure.TEST_MODE;

		BlockPos pos = BlockPos.containing(x, y, z);
		if (!isTestMode && (pos.getY() < 50 || pos.getY() > level.getMaxBuildHeight() - 30)) {
			return false;
		}
		if (!isTestMode && !level.dimensionType().hasCeiling() && (!level.canSeeSky(pos) && !level.canSeeSky(pos.above(10)))) {
			return false;
		}

		// Separation guard: Ensure no Verdant entities or active spawn rifts within 48 blocks
		AABB guardBox = new AABB(x - 48, level.getMinBuildHeight(), z - 48, x + 48, level.getMaxBuildHeight(), z + 48);
		List<VerdantEngineEntity> existingLocal = level.getEntitiesOfClass(VerdantEngineEntity.class, guardBox);
		if (!existingLocal.isEmpty()) {
			return false;
		}

		List<net.minecraft.world.entity.AreaEffectCloud> existingClouds = level.getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class, guardBox);
		for (net.minecraft.world.entity.AreaEffectCloud cloud : existingClouds) {
			if (cloud.getTags().contains("BH_RIFT_VERDANT")) {
				return false;
			}
		}

		long currentTime = level.getGameTime();
		if (!isTestMode && (currentTime - lastGlobalSpawnTime < GLOBAL_SPAWN_COOLDOWN_TICKS)) {
			return false;
		}

		List<? extends Player> nearbyPlayers = level.players();
		if (nearbyPlayers.isEmpty()) {
			return false;
		}

		Player targetPlayer = null;
		for (Player player : nearbyPlayers) {
			if (player == null || player.isSpectator()) continue;

			if (!isTestMode && player.distanceToSqr(x, y, z) > 256.0 * 256.0) continue;

			CompoundTag playerData = player.getPersistentData();

			// Enforce max 1 Verdant per player check
			AABB playerBox = new AABB(player.getX() - 192, level.getMinBuildHeight(), player.getZ() - 192, player.getX() + 192, level.getMaxBuildHeight(), player.getZ() + 192);
			List<VerdantEngineEntity> existingPlayerVerdants = level.getEntitiesOfClass(VerdantEngineEntity.class, playerBox);
			if (!existingPlayerVerdants.isEmpty()) {
				continue;
			}

			long playerCooldownUntil = getLong(playerData, "verdant_spawn_cooldown_until", 0L);
			if (!isTestMode && currentTime < playerCooldownUntil) {
				continue;
			}

			// Multi-Player Collective Guilt Aggregation (Sum nearby team members in 192-block territory)
			double collectiveCol = 0.0;
			double collectiveTrans = 0.0;
			int maxSettlementsInGroup = 0;
			boolean anyRotFlag = false;

			for (Player teamMember : nearbyPlayers) {
				if (teamMember == null || teamMember.isSpectator()) continue;
				if (teamMember.distanceToSqr(player) <= 192.0 * 192.0) {
					CompoundTag memberData = teamMember.getPersistentData();
					collectiveCol += getDouble(memberData, "backwoods_colonization_score", 0.0);
					collectiveTrans += getDouble(memberData, "backwoods_transgression_score", 0.0);
					if (getBoolean(memberData, "backwoods_rot_event_flag", false)) anyRotFlag = true;
					maxSettlementsInGroup = Math.max(maxSettlementsInGroup, TrackBackwoodsTransgressionProcedure.countSignificantSettlements(memberData));
				}
			}

			double severity = TrackBackwoodsTransgressionProcedure.calculateSeverity(level, playerData);

			double dynamicMult = TrackBackwoodsTransgressionProcedure.getDynamicTriggerMultiplier(level);
			double reqTrans = isTestMode ? 15.0 : (TrackBackwoodsTransgressionProcedure.REQ_TRANSGRESSION_JUDGMENT * dynamicMult);
			double reqCol = isTestMode ? 10.0 : (TrackBackwoodsTransgressionProcedure.REQ_COLONIZATION_JUDGMENT * dynamicMult);
			double reqSettleCol = isTestMode ? 8.0 : (TrackBackwoodsTransgressionProcedure.REQ_SETTLEMENT_COLONIZATION_JUDGMENT * dynamicMult);
			double reqSeverity = isTestMode ? 30.0 : TrackBackwoodsTransgressionProcedure.REQ_SEVERITY_JUDGMENT;

			boolean eligible = anyRotFlag
				|| collectiveTrans >= reqTrans
				|| collectiveCol >= reqCol
				|| (maxSettlementsInGroup >= 1 && collectiveCol >= reqSettleCol)
				|| severity >= reqSeverity;

			if (eligible) {
				targetPlayer = player;
				break;
			}
		}

		if (targetPlayer == null) {
			return false;
		}

		// Reset / Cooldown for all team members in the territory
		for (Player teamMember : nearbyPlayers) {
			if (teamMember != null && !teamMember.isSpectator() && teamMember.distanceToSqr(targetPlayer) <= 192.0 * 192.0) {
				CompoundTag memberNbt = teamMember.getPersistentData();
				memberNbt.putLong("verdant_spawn_cooldown_until", currentTime + PLAYER_SPAWN_COOLDOWN_TICKS);
				double currentCol = getDouble(memberNbt, "backwoods_colonization_score", 0.0);
				double currentTrans = getDouble(memberNbt, "backwoods_transgression_score", 0.0);
				memberNbt.putDouble("backwoods_colonization_score", 0.0);
				memberNbt.putDouble("backwoods_transgression_score", 0.0);
				memberNbt.putBoolean("backwoods_rot_event_flag", false);
			}
		}

		lastGlobalSpawnTime = currentTime;

		BlockPos playerPos = targetPlayer.blockPosition();
		double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
		double dist = 20.0 + level.getRandom().nextDouble() * 16.0;
		double spawnX = playerPos.getX() + Math.cos(angle) * dist;
		double spawnZ = playerPos.getZ() + Math.sin(angle) * dist;

		double riftY = calculateHighSkyRiftY(level, spawnX, spawnZ, playerPos.getY());
		BlockPos riftPos = BlockPos.containing(spawnX, riftY, spawnZ);
		net.mcreator.thebackwoods.LowSimDistanceSolution.forceLoadRiftArea(level, riftPos);

		net.mcreator.thebackwoods.BlackHole.spawnVerdantSpawnRift(level, spawnX, riftY, spawnZ);

		return false;
	}

	private static double calculateHighSkyRiftY(ServerLevel level, double x, double z, double playerY) {
		int scanX = net.minecraft.util.Mth.floor(x);
		int scanZ = net.minecraft.util.Mth.floor(z);
		int maxY = level.getMaxBuildHeight();
		int minY = level.getMinBuildHeight();

		if (level.dimensionType().hasCeiling()) {
			// Dynamically locate the highest bedrock ceiling layer across modded and vanilla ceiling heights
			int ceilingBedrockY = -1;
			BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos(scanX, maxY - 2, scanZ);
			while (scanPos.getY() > minY + 20) {
				net.minecraft.world.level.block.state.BlockState st = level.getBlockState(scanPos);
				if (st.getBlock() == net.minecraft.world.level.block.Blocks.BEDROCK || (!st.isAir() && st.isSolidRender(level, scanPos))) {
					ceilingBedrockY = scanPos.getY();
					break;
				}
				scanPos.move(net.minecraft.core.Direction.DOWN);
			}

			if (ceilingBedrockY > minY + 20 && maxY - ceilingBedrockY >= 15) {
				// Spawn strictly above the detected nether ceiling in the high sky
				double targetRiftY = (double) ceilingBedrockY + 45.0 + (level.getRandom().nextDouble() * 35.0);
				return Math.min((double) maxY - 10.0, Math.max((double) ceilingBedrockY + 15.0, targetRiftY));
			}
		}

		int highestGroundY = minY + 10;

		// Fast O(1) heightmap sampling across the local area (zero disk I/O, zero block decompression)
		int[] offsets = {-4, 0, 4};
		for (int ox : offsets) {
			for (int oz : offsets) {
				int sampleX = scanX + ox;
				int sampleZ = scanZ + oz;
				if (level.getChunkSource().hasChunk(sampleX >> 4, sampleZ >> 4)) {
					int h = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sampleX, sampleZ);
					if (h > highestGroundY) {
						highestGroundY = h;
					}
				}
			}
		}

		double riftY = (double) highestGroundY + 90.0 + (level.getRandom().nextDouble() * 60.0);
		return Math.min((double) maxY - 10.0, Math.max((double) minY + 20.0, riftY));
	}

	private static double getDouble(CompoundTag tag, String key, double def) {
		if (tag == null || !tag.contains(key)) return def;
		return tag.getDouble(key);
	}

	private static boolean getBoolean(CompoundTag tag, String key, boolean def) {
		if (tag == null || !tag.contains(key)) return def;
		return tag.getBoolean(key);
	}

	private static int getInt(CompoundTag tag, String key, int def) {
		if (tag == null || !tag.contains(key)) return def;
		return tag.getInt(key);
	}

	private static long getLong(CompoundTag tag, String key, long def) {
		if (tag == null || !tag.contains(key)) return def;
		return tag.getLong(key);
	}
} // 1.21.1
