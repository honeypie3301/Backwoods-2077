package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import net.mcreator.thebackwoods.entity.RotEntity;

@EventBusSubscriber
public class TrackBackwoodsTransgressionProcedure {

	public static boolean TEST_MODE = true; // set true to test quickly / set false for normal gameplay
	public static double MASTER_TRIGGER_MULTIPLIER = 1.0; // lower to trigger faster / increase to require larger towns

	public static double getDifficultyMultiplier(Level level) {
		if (level == null) return 1.0;
		if (level.getLevelData().isHardcore()) {
			return 0.07;
		}
		net.minecraft.world.Difficulty diff = level.getDifficulty();
		if (diff == net.minecraft.world.Difficulty.EASY || diff == net.minecraft.world.Difficulty.PEACEFUL) {
			return 3.0;
		} else if (diff == net.minecraft.world.Difficulty.HARD) {
			return 0.3;
		}
		return 1.0; // NORMAL
	}

	public static double getDynamicTriggerMultiplier(Level level) {
		return MASTER_TRIGGER_MULTIPLIER * getDifficultyMultiplier(level);
	}

	private static final int DATA_VERSION = 2;
	public static double MAX_SCORE = 150000.0; // max score cap
	public static int MAX_COUNTER = 1000000; // max counter cap

	// Decay Settings (Tuned for endgame multi-hour building sessions)
	public static long DECAY_INTERVAL_TICKS = 12000L; // lower to decay faster / increase to decay slower (12000 = 10 mins)
	public static double TRANSGRESSION_DECAY_RATE = 0.96; // lower to lose score faster / increase to keep score longer
	public static double COLONIZATION_DECAY_RATE = 0.99; // lower to lose score faster / increase to keep score longer

	// Trigger Thresholds (150x Endgame City / Fortress Scale)
	public static double REQ_TRANSGRESSION_JUDGMENT = 90000.0; // lower to trigger easier / increase to make harder
	public static double REQ_COLONIZATION_JUDGMENT = 30000.0; // lower to trigger easier / increase to make harder
	public static double REQ_SETTLEMENT_COLONIZATION_JUDGMENT = 18000.0; // lower to trigger easier / increase to make harder
	public static double REQ_SEVERITY_JUDGMENT = 85.0; // lower to trigger easier / increase to make harder
	public static int REQ_SETTLEMENT_BLOCK_COUNT = 750; // lower for small builds / increase for large towns only

	// Severity Levels
	public static double SEVERITY_DISTURBED_THRESHOLD = 15.0; // lower to reach faster / increase for slower
	public static double SEVERITY_INTRUSION_THRESHOLD = 40.0; // lower to reach faster / increase for slower
	public static double SEVERITY_COLONIZATION_THRESHOLD = 65.0; // lower to reach faster / increase for slower

	// Score Given Per Action
	public static double SCORE_FOLIAGE_BREAK = 0.05; // lower for less points / increase for more points
	public static double SCORE_LOG_OR_PICKAXE_BREAK = 1.2; // lower for less points / increase for more points
	public static double SCORE_DEFAULT_BREAK = 1.0; // lower for less points / increase for more points
	public static double SCORE_DEFAULT_PLACE_COLONIZATION = 1.0; // lower for less points / increase for more points
	public static double SCORE_DEFAULT_PLACE_TRANSGRESSION = 0.5; // lower for less points / increase for more points
	public static double SCORE_INFRASTRUCTURE_BONUS = 6.0; // lower for less points / increase for more points
	public static double SCORE_CHEAP_SPAM_BLOCK_PLACE = 0.1; // lower for less points / increase for more points

	public static void execute(Entity entity, double transgressionAdd, double colonizationAdd) {
		if (!(entity instanceof Player player) || player.isSpectator()) return;
		if (!(player.level() instanceof ServerLevel level)) return;

		CompoundTag tag = player.getPersistentData();
		ensureDataVersion(tag, player);
		applyScoreDecay(tag, level.getGameTime());

		double curTrans = getDouble(tag, "backwoods_transgression_score", 0.0);
		double curCol = getDouble(tag, "backwoods_colonization_score", 0.0);

		tag.putDouble("backwoods_transgression_score", Math.max(0.0, Math.min(MAX_SCORE, curTrans + transgressionAdd)));
		tag.putDouble("backwoods_colonization_score", Math.max(0.0, Math.min(MAX_SCORE, curCol + colonizationAdd)));
		tag.putLong("backwoods_last_activity_time", level.getGameTime());
	}

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (event == null || !(event.getLevel() instanceof ServerLevel level)) return;
		if (!isBackwoodsDimension(level)) return;

		Player player = event.getPlayer();
		if (player == null || player.isSpectator()) return;

		BlockState state = event.getState();
		if (state == null) return;

		CompoundTag tag = player.getPersistentData();
		ensureDataVersion(tag, player);
		long currentTime = level.getGameTime();
		applyScoreDecay(tag, currentTime);

		BlockPos pos = event.getPos();
		if (checkAndRemovePlayerPlacedBlock(tag, pos)) {
			return;
		}

		String blockPath = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase();

		double disturbanceScore = SCORE_DEFAULT_BREAK;
		if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS)
			|| blockPath.contains("grass") || blockPath.contains("fern") || blockPath.contains("vine")
			|| blockPath.contains("bush") || blockPath.contains("root") || blockPath.contains("moss")) {
			disturbanceScore = SCORE_FOLIAGE_BREAK;
		} else if (state.is(BlockTags.LOGS) || state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.PLANKS)) {
			disturbanceScore = SCORE_LOG_OR_PICKAXE_BREAK;
		}

		if (TEST_MODE) {
			disturbanceScore *= 15.0; // Fast progression multiplier for testing
		}

		double curTrans = getDouble(tag, "backwoods_transgression_score", 0.0);
		int blocksBroken = getInt(tag, "backwoods_blocks_broken", 0);

		tag.putDouble("backwoods_transgression_score", Math.max(0.0, Math.min(MAX_SCORE, curTrans + disturbanceScore)));
		tag.putInt("backwoods_blocks_broken", Math.min(MAX_COUNTER, blocksBroken + 1));
		tag.putLong("backwoods_last_activity_time", currentTime);
	}

	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
		if (event == null || !(event.getLevel() instanceof ServerLevel level)) return;
		if (!isBackwoodsDimension(level)) return;

		if (!(event.getEntity() instanceof Player player) || player.isSpectator()) return;

		BlockState state = event.getPlacedBlock();
		if (state == null) return;

		BlockPos pos = event.getPos();
		CompoundTag tag = player.getPersistentData();
		ensureDataVersion(tag, player);
		long currentTime = level.getGameTime();
		applyScoreDecay(tag, currentTime);

		addPlayerPlacedBlock(tag, pos);

		String blockPath = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase();
		boolean isInfra = isInfrastructureBlock(blockPath);

		double baseColonization = SCORE_DEFAULT_PLACE_COLONIZATION;
		double baseTransgression = SCORE_DEFAULT_PLACE_TRANSGRESSION;

		if (isInfra) {
			baseColonization += SCORE_INFRASTRUCTURE_BONUS;
		} else if (isCheapSpamBlock(state, blockPath)) {
			baseColonization = SCORE_CHEAP_SPAM_BLOCK_PLACE;
		}

		if (TEST_MODE) {
			baseColonization *= 15.0;
			baseTransgression *= 15.0;
		}

		recordSettlementPlacement(tag, pos, isInfra, currentTime);

		double curCol = getDouble(tag, "backwoods_colonization_score", 0.0);
		double curTrans = getDouble(tag, "backwoods_transgression_score", 0.0);
		int blocksPlaced = getInt(tag, "backwoods_blocks_placed", 0);
		int infraPlaced = getInt(tag, "backwoods_infrastructure_placed", 0);

		tag.putDouble("backwoods_colonization_score", Math.max(0.0, Math.min(MAX_SCORE, curCol + baseColonization)));
		tag.putDouble("backwoods_transgression_score", Math.max(0.0, Math.min(MAX_SCORE, curTrans + baseTransgression)));
		tag.putInt("backwoods_blocks_placed", Math.min(MAX_COUNTER, blocksPlaced + 1));
		if (isInfra) {
			tag.putInt("backwoods_infrastructure_placed", Math.min(MAX_COUNTER, infraPlaced + 1));
		}
		tag.putLong("backwoods_last_activity_time", currentTime);
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event == null || event.getEntity() == null) return;

		LivingEntity victim = event.getEntity();
		if (!(victim.level() instanceof ServerLevel level) || !isBackwoodsDimension(level)) return;

		if (victim instanceof RotEntity || isRotEntity(victim)) {
			DamageSource source = event.getSource();
			Player killerPlayer = resolvePlayerKiller(source, level);

			if (killerPlayer != null && !killerPlayer.isSpectator()) {
				CompoundTag tag = killerPlayer.getPersistentData();
				ensureDataVersion(tag, killerPlayer);
				long currentTime = level.getGameTime();
				applyScoreDecay(tag, currentTime);

				int rotKills = getInt(tag, "backwoods_rot_kills_count", 0);

				tag.putBoolean("backwoods_rot_killed", true);
				tag.putInt("backwoods_rot_kills_count", Math.min(MAX_COUNTER, rotKills + 1));
				tag.putLong("backwoods_last_rot_kill_time", currentTime);
				tag.putBoolean("backwoods_rot_event_flag", true);
				tag.putLong("backwoods_last_activity_time", currentTime);
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player == null || player.isSpectator()) return;
		if (!(player.level() instanceof ServerLevel level)) return;

		// Throttled to run every 3 seconds (60 ticks) per player for performance
		if (level.getGameTime() % 60 != 0) return;

		// Fast early exit: skip if player has zero scores and TEST_MODE is disabled
		if (!TEST_MODE) {
			CompoundTag tag = player.getPersistentData();
			double trans = getDouble(tag, "backwoods_transgression_score", 0.0);
			double col = getDouble(tag, "backwoods_colonization_score", 0.0);
			boolean rotFlag = getBoolean(tag, "backwoods_rot_event_flag", false);
			if (trans <= 0.0 && col <= 0.0 && !rotFlag) return;
		}

		// Automatically evaluate Verdant spawn condition in non-Backwoods dimensions (Overworld)
		String dimId = level.dimension().location().toString().toLowerCase();
		CompoundTag tag = player.getPersistentData();
		String lastDimId = getOptionalString(tag, "backwoods_last_dim_id", "");
		long lastExitTime = getLong(tag, "backwoods_dim_exit_time", 0L);

		if (!dimId.equals(lastDimId)) {
			if (lastDimId.contains("backwood") && !dimId.contains("backwood")) {
				tag.putLong("backwoods_dim_exit_time", level.getGameTime());
				lastExitTime = level.getGameTime();
			}
			tag.putString("backwoods_last_dim_id", dimId);
		}

		// Enforce a 6-second (120 tick) grace period after leaving Backwoods dimension before spawning
		if (level.getGameTime() - lastExitTime < 120L) {
			return;
		}

		if (!dimId.contains("the_backwoods") && !dimId.contains("backwood")) {
			VerdantEngineNaturalEntitySpawningConditionProcedure.execute(level, player.getX(), player.getY(), player.getZ());
		}
	}

	@SubscribeEvent
	public static void onServerChat(ServerChatEvent event) {
		if (event == null || event.getPlayer() == null) return;
		String msg = event.getRawText().trim();
		Player player = event.getPlayer();

		if (msg.equalsIgnoreCase("!backwoods_debug") || msg.equalsIgnoreCase("/backwoods_debug")) {
			String debugMsg = getDebugStatus(player);
			player.sendSystemMessage(Component.literal(debugMsg));
			return;
		}

		if (TEST_MODE || msg.equalsIgnoreCase("!test_transgression") || msg.equalsIgnoreCase("!trigger_judgment") || msg.equalsIgnoreCase("!test_judgment") || msg.equalsIgnoreCase("!clear_transgression")) {
			if (msg.equalsIgnoreCase("!test_transgression") || msg.equalsIgnoreCase("!test") || msg.equalsIgnoreCase("!add_transgression")) {
				CompoundTag tag = player.getPersistentData();
				ensureDataVersion(tag, player);
				double trans = getDouble(tag, "backwoods_transgression_score", 0.0);
				double col = getDouble(tag, "backwoods_colonization_score", 0.0);
				tag.putDouble("backwoods_transgression_score", Math.min(MAX_SCORE, trans + 75.0));
				tag.putDouble("backwoods_colonization_score", Math.min(MAX_SCORE, col + 75.0));
				tag.putLong("verdant_spawn_cooldown_until", 0L);
				double severity = calculateSeverity(player.level(), tag);
				player.sendSystemMessage(Component.literal(String.format("§a[TEST MODE] +75 Transgression & Colonization! Severity: %.1f (%s) | State: %s", severity, getSeverityCategory(severity), getJudgmentState(player, tag, severity))));
			} else if (msg.equalsIgnoreCase("!trigger") || msg.equalsIgnoreCase("!trigger_judgment") || msg.equalsIgnoreCase("!test_judgment")) {
				CompoundTag tag = player.getPersistentData();
				ensureDataVersion(tag, player);
				tag.putDouble("backwoods_transgression_score", 500.0);
				tag.putDouble("backwoods_colonization_score", 500.0);
				tag.putBoolean("backwoods_rot_event_flag", true);
				tag.putLong("verdant_spawn_cooldown_until", 0L);
				player.sendSystemMessage(Component.literal("§a[TEST MODE] Transgression & Colonization maxed out! Judgment state is now PENDING!"));
			} else if (msg.equalsIgnoreCase("!clear_transgression") || msg.equalsIgnoreCase("!reset_transgression")) {
				CompoundTag tag = player.getPersistentData();
				ensureDataVersion(tag, player);
				tag.putDouble("backwoods_transgression_score", 0.0);
				tag.putDouble("backwoods_colonization_score", 0.0);
				tag.putBoolean("backwoods_rot_event_flag", false);
				tag.putLong("verdant_spawn_cooldown_until", 0L);
				player.sendSystemMessage(Component.literal("§c[TEST MODE] Transgression & Colonization scores reset to 0!"));
			}
		}
	}

	public static String getDebugStatus(Player player) {
		if (player == null) return "[Backwoods Debug] Invalid Player";
		CompoundTag tag = player.getPersistentData();
		ensureDataVersion(tag, player);

		double trans = getDouble(tag, "backwoods_transgression_score", 0.0);
		double col = getDouble(tag, "backwoods_colonization_score", 0.0);
		int infra = getInt(tag, "backwoods_infrastructure_placed", 0);
		int sigSettlements = countSignificantSettlements(tag);
		int totalSettlements = countTotalSettlements(tag);
		boolean rotFlag = getBoolean(tag, "backwoods_rot_event_flag", false);
		int rotKills = getInt(tag, "backwoods_rot_kills_count", 0);

		double severity = calculateSeverity(player.level(), tag);
		String category = getSeverityCategory(severity);
		String judgmentState = getJudgmentState(player, tag, severity);

		long cooldownUntil = getLong(tag, "verdant_spawn_cooldown_until", 0L);
		long currentTime = player.level().getGameTime();
		long cdLeft = Math.max(0L, cooldownUntil - currentTime);

		String reason = getEligibilityReason(player.level(), tag, severity, sigSettlements, cdLeft);

		return String.format("[Backwoods Debug v%d] State: %s | Severity: %.1f (%s) | Trans: %.1f | Col: %.1f | Infra: %d | Settlements: %d (Sig: %d) | RotFlag: %b (Kills: %d) | Cooldown: %ds | Reason: %s",
			DATA_VERSION, judgmentState, severity, category, trans, col, infra, totalSettlements, sigSettlements, rotFlag, rotKills, cdLeft / 20, reason);
	}

	public static double calculateSeverity(Level level, CompoundTag tag) {
		if (tag == null) return 0.0;
		double trans = getDouble(tag, "backwoods_transgression_score", 0.0);
		double col = getDouble(tag, "backwoods_colonization_score", 0.0);
		int sigSettlements = countSignificantSettlements(tag);
		int infra = getInt(tag, "backwoods_infrastructure_placed", 0);
		boolean rotFlag = getBoolean(tag, "backwoods_rot_event_flag", false);

		double dynamicMult = getDynamicTriggerMultiplier(level);
		double transTarget = Math.max(1.0, REQ_TRANSGRESSION_JUDGMENT * dynamicMult);
		double colTarget = Math.max(1.0, REQ_COLONIZATION_JUDGMENT * dynamicMult);
		double sTrans = Math.min(30.0, (trans / transTarget) * 30.0);
		double sCol = Math.min(40.0, (col / colTarget) * 40.0);
		double sSettle = Math.min(15.0, sigSettlements * 3.0);
		double sInfra = Math.min(15.0, (infra / (3000.0 * dynamicMult)) * 15.0);

		double severity = sTrans + sCol + sSettle + sInfra;
		if (rotFlag) {
			severity = Math.max(severity, 75.0);
		}
		return Math.max(0.0, Math.min(100.0, severity));
	}

	public static String getSeverityCategory(double severity) {
		if (severity < SEVERITY_DISTURBED_THRESHOLD) return "NEGLIGIBLE";
		if (severity < SEVERITY_INTRUSION_THRESHOLD) return "DISTURBED";
		if (severity < SEVERITY_COLONIZATION_THRESHOLD) return "INTRUSION";
		if (severity < REQ_SEVERITY_JUDGMENT) return "COLONIZATION";
		return "JUDGMENT";
	}

	public static String getJudgmentState(Player player, CompoundTag tag, double severity) {
		if (player == null || tag == null) return "NORMAL";
		long cooldownUntil = getLong(tag, "verdant_spawn_cooldown_until", 0L);
		long currentTime = player.level().getGameTime();

		if (currentTime < cooldownUntil) {
			return "MANIFESTED";
		}

		boolean rotFlag = getBoolean(tag, "backwoods_rot_event_flag", false);
		double col = getDouble(tag, "backwoods_colonization_score", 0.0);
		double trans = getDouble(tag, "backwoods_transgression_score", 0.0);
		int sigSettlements = countSignificantSettlements(tag);

		double dynamicMult = getDynamicTriggerMultiplier(player.level());
		double reqCol = REQ_COLONIZATION_JUDGMENT * dynamicMult;
		double reqTrans = REQ_TRANSGRESSION_JUDGMENT * dynamicMult;
		double reqSettleCol = REQ_SETTLEMENT_COLONIZATION_JUDGMENT * dynamicMult;

		boolean eligible = rotFlag || col >= reqCol || trans >= reqTrans || (sigSettlements >= 1 && col >= reqSettleCol) || severity >= REQ_SEVERITY_JUDGMENT;

		if (eligible) {
			return "JUDGMENT_PENDING";
		}
		if (severity >= 45.0 || sigSettlements >= 1 || col >= 80.0) {
			return "WATCHED";
		}
		if (severity >= SEVERITY_DISTURBED_THRESHOLD || trans >= 40.0) {
			return "DISTURBED";
		}
		return "NORMAL";
	}

	public static String getEligibilityReason(Level level, CompoundTag tag, double severity, int sigSettlements, long cdLeft) {
		if (cdLeft > 0) {
			return "INELIGIBLE (COOLDOWN)";
		}
		if (getBoolean(tag, "backwoods_rot_event_flag", false)) {
			return "ROT_KILLED";
		}
		double col = getDouble(tag, "backwoods_colonization_score", 0.0);
		double dynamicMult = getDynamicTriggerMultiplier(level);
		double reqCol = REQ_COLONIZATION_JUDGMENT * dynamicMult;
		double reqSettleCol = REQ_SETTLEMENT_COLONIZATION_JUDGMENT * dynamicMult;
		if (col >= reqCol || (sigSettlements >= 1 && col >= reqSettleCol)) {
			return "COLONIZATION";
		}
		double trans = getDouble(tag, "backwoods_transgression_score", 0.0);
		double reqTrans = REQ_TRANSGRESSION_JUDGMENT * dynamicMult;
		if (trans >= reqTrans) {
			return "EXTREME_DISTURBANCE";
		}
		if (severity >= REQ_SEVERITY_JUDGMENT) {
			return "HIGH_SEVERITY";
		}
		return "INELIGIBLE";
	}

	private static void ensureDataVersion(CompoundTag tag, Player player) {
		if (tag == null) return;
		int version = getInt(tag, "backwoods_data_version", 0);
		if (version < DATA_VERSION) {
			tag.putInt("backwoods_data_version", DATA_VERSION);
			if (!tag.contains("backwoods_player_seed") && player != null) {
				long seed = player.getUUID().hashCode() ^ 0x5DEECE66DL;
				tag.putLong("backwoods_player_seed", seed);
			}
		}
	}

	private static void recordSettlementPlacement(CompoundTag tag, BlockPos pos, boolean isInfra, long currentTime) {
		int gridX = pos.getX() >> 5;
		int gridZ = pos.getZ() >> 5;
		String gridKey = "grid_" + gridX + "_" + gridZ;

		CompoundTag settlements = getCompound(tag, "backwoods_settlements");
		CompoundTag gridTag = getCompound(settlements, gridKey);

		int blocks = Math.min(10000, getInt(gridTag, "blocks", 0) + 1);
		int infra = Math.min(10000, getInt(gridTag, "infra", 0) + (isInfra ? 1 : 0));
		int visits = Math.min(10000, getInt(gridTag, "visits", 1));
		long lastVisit = getLong(gridTag, "last_visit", currentTime);

		if (currentTime - lastVisit >= 2400L) {
			visits = Math.min(10000, visits + 1);
		}

		gridTag.putInt("blocks", blocks);
		gridTag.putInt("infra", infra);
		gridTag.putInt("visits", visits);
		gridTag.putLong("last_visit", currentTime);

		settlements.put(gridKey, gridTag);

		if (settlements.getAllKeys().size() > 8) {
			String oldestKey = null;
			long oldestTime = Long.MAX_VALUE;
			for (String key : settlements.getAllKeys()) {
				CompoundTag entry = getCompound(settlements, key);
				long entryLast = getLong(entry, "last_visit", 0L);
				if (entryLast < oldestTime) {
					oldestTime = entryLast;
					oldestKey = key;
				}
			}
			if (oldestKey != null) {
				settlements.remove(oldestKey);
			}
		}

		tag.put("backwoods_settlements", settlements);
	}

	public static int countSignificantSettlements(CompoundTag tag) {
		if (tag == null || !tag.contains("backwoods_settlements")) return 0;
		CompoundTag settlements = getCompound(tag, "backwoods_settlements");
		int count = 0;
		for (String key : settlements.getAllKeys()) {
			CompoundTag gridTag = getCompound(settlements, key);
			int blocks = getInt(gridTag, "blocks", 0);
			int infra = getInt(gridTag, "infra", 0);
			int visits = getInt(gridTag, "visits", 0);
			if (blocks >= 20 && infra >= 3 && visits >= 3) {
				count++;
			}
		}
		return count;
	}

	public static int countTotalSettlements(CompoundTag tag) {
		if (tag == null || !tag.contains("backwoods_settlements")) return 0;
		CompoundTag settlements = getCompound(tag, "backwoods_settlements");
		return settlements.getAllKeys().size();
	}

	private static void applyScoreDecay(CompoundTag tag, long currentTime) {
		if (tag == null) return;

		long lastDecayTime = getLong(tag, "backwoods_last_decay_time", 0L);
		if (lastDecayTime <= 0L) {
			tag.putLong("backwoods_last_decay_time", currentTime);
			return;
		}

		long elapsed = currentTime - lastDecayTime;
		if (elapsed >= DECAY_INTERVAL_TICKS) {
			int intervals = (int) (elapsed / DECAY_INTERVAL_TICKS);

			double transFactor = Math.pow(TRANSGRESSION_DECAY_RATE, intervals);
			double curTrans = getDouble(tag, "backwoods_transgression_score", 0.0);
			tag.putDouble("backwoods_transgression_score", Math.max(0.0, Math.min(MAX_SCORE, curTrans * transFactor)));

			double colFactor = Math.pow(COLONIZATION_DECAY_RATE, intervals);
			double curCol = getDouble(tag, "backwoods_colonization_score", 0.0);
			tag.putDouble("backwoods_colonization_score", Math.max(0.0, Math.min(MAX_SCORE, curCol * colFactor)));

			tag.putLong("backwoods_last_decay_time", currentTime);
		}
	}

	private static Player resolvePlayerKiller(DamageSource source, ServerLevel level) {
		if (source == null) return null;

		Entity directAttacker = source.getDirectEntity();
		Entity attacker = source.getEntity();

		if (directAttacker instanceof Player p) return p;
		if (attacker instanceof Player p) return p;

		if (attacker != null && attacker.getPersistentData().contains("master_owner_uuid")) {
			String uuidStr = getOptionalString(attacker.getPersistentData(), "master_owner_uuid", "");
			if (!uuidStr.isEmpty()) {
				try {
					return level.getPlayerByUUID(java.util.UUID.fromString(uuidStr));
				} catch (Exception ignored) {}
			}
		}

		return null;
	}

	private static boolean isBackwoodsDimension(ServerLevel level) {
		if (level == null) return false;
		String dimId = level.dimension().location().toString().toLowerCase();
		return dimId.contains("the_backwoods") || dimId.contains("backwood");
	}

	private static boolean isRotEntity(Entity entity) {
		if (entity == null) return false;
		String className = entity.getClass().getSimpleName();
		String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().toLowerCase();
		return className.contains("Rot") || typeKey.contains("rot");
	}

	private static boolean isInfrastructureBlock(String path) {
		return path.contains("chest") || path.contains("barrel") || path.contains("furnace")
			|| path.contains("crafting") || path.contains("anvil") || path.contains("bed")
			|| path.contains("door") || path.contains("torch") || path.contains("lantern")
			|| path.contains("lamp") || path.contains("glass") || path.contains("brick")
			|| path.contains("rail") || path.contains("hopper") || path.contains("piston")
			|| path.contains("observer") || path.contains("brewing") || path.contains("shulker")
			|| path.contains("enchanting") || path.contains("smithing") || path.contains("smoker")
			|| path.contains("stonecutter") || path.contains("loom") || path.contains("grindstone")
			|| path.contains("cartography") || path.contains("machine") || path.contains("terminal")
			|| path.contains("storage") || path.contains("conduit") || path.contains("beacon");
	}

	private static boolean isCheapSpamBlock(BlockState state, String path) {
		return path.contains("dirt") || path.contains("cobblestone") || path.contains("gravel")
			|| path.contains("sand") || path.contains("netherrack") || path.contains("scaffolding")
			|| state.is(BlockTags.LEAVES);
	}

	private static CompoundTag getCompound(CompoundTag tag, String key) {
		if (tag == null || !tag.contains(key)) return new CompoundTag();
		return tag.getCompound(key);
	}

	private static double getDouble(CompoundTag tag, String key, double def) {
		if (tag == null || !tag.contains(key)) return def;
		return tag.getDouble(key);
	}

	private static int getInt(CompoundTag tag, String key, int def) {
		if (tag == null || !tag.contains(key)) return def;
		return tag.getInt(key);
	}

	private static long getLong(CompoundTag tag, String key, long def) {
		if (tag == null || !tag.contains(key)) return def;
		return tag.getLong(key);
	}

	private static boolean getBoolean(CompoundTag tag, String key, boolean def) {
		if (tag == null || !tag.contains(key)) return def;
		return tag.getBoolean(key);
	}

	private static String getOptionalString(CompoundTag tag, String key, String def) {
		if (tag == null || !tag.contains(key)) return def;
		return tag.getString(key);
	}

	private static boolean checkAndRemovePlayerPlacedBlock(CompoundTag tag, BlockPos pos) {
		if (tag == null || !tag.contains("backwoods_placed_blocks")) return false;
		long target = pos.asLong();
		long[] array = tag.getLongArray("backwoods_placed_blocks");
		int index = -1;
		for (int i = 0; i < array.length; i++) {
			if (array[i] == target) {
				index = i;
				break;
			}
		}
		if (index != -1) {
			long[] newArray = new long[array.length - 1];
			System.arraycopy(array, 0, newArray, 0, index);
			System.arraycopy(array, index + 1, newArray, index, array.length - index - 1);
			tag.putLongArray("backwoods_placed_blocks", newArray);
			return true;
		}
		return false;
	}

	private static void addPlayerPlacedBlock(CompoundTag tag, BlockPos pos) {
		if (tag == null) return;
		long target = pos.asLong();
		long[] array = tag.contains("backwoods_placed_blocks") ? tag.getLongArray("backwoods_placed_blocks") : new long[0];
		for (long val : array) {
			if (val == target) return;
		}
		long[] newArray = new long[array.length + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = target;
		tag.putLongArray("backwoods_placed_blocks", newArray);
	}
} // 1.21.1
