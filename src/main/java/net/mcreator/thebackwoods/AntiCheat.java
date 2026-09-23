/*
 * GrimAC-Inspired Server-Side AntiCheat for NeoForge 1.21.1
 * 
 * Features:
 * - Master Toggle (ENABLE / DISABLE AntiCheat system)
 * - Exclusive Developer Whitelist (UUID + Username matching for "Dev" and "honeypie_3301")
 * - Thread-Safe Concurrent Multi-Threading Support (ConcurrentHashMap)
 * - Reconnection Exploit Mitigation (60-second VL persistence cache on disconnect/relog)
 * - Teleport & Dimension Change Immunity (Fixes /tp, Ender Pearls, Portals, Chorus Fruit)
 * - Knockback, Explosion, Piston, Flowing Water & Wind Charge Velocity Grace Windows
 * - Vehicle Dismount & Riding Position Re-Synchronization
 * - Multi-Tick Packet Batching Buffer (Smooths 150-250ms high-ping latency spikes)
 * - Dynamic /tick rate, /tick freeze & /tick sprint Compensation
 * - NoSlow Check (Item usage, eating, blocking, drawing bow, crouching)
 * - Terrain-Aware Speed Allowance (Ice variants, Honey blocks, Soul Sand, Soul Speed)
 * - Elytra Boundary & Firework Speed Cap Checks
 * - Ability Spoof Validation (Detects & resets unauthorized client fly toggles)
 * - Server-Side NoFall & Fall Damage Verification
 * - Ranged Attack & Projectile Aimbot Validation
 * - Entity Box Size Geometry Awareness (Fixes false KillAura on Dragon/Warden/Boats)
 * - Rotational Snap & Aimbot Locking Detection
 * - Dynamic Server-Side Entity Attribute Calculations (Movement Speed & Attack Range)
 * - Comprehensive Mod Compatibility Bypasses (the_backwoods, Iron's Spells, Ars Nouveau, Create, etc.)
*/
package net.mcreator.thebackwoods;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.Set;

@EventBusSubscriber
public class AntiCheat {

	// MASTER TOGGLE: Set to false to temporarily disable all AntiCheat checks
	public static boolean ENABLED = true;

	// WHITELISTED DEVELOPERS (Usernames + Known UUIDs)
	private static final Set<String> WHITELISTED_USERNAMES = Set.of(
		"Dev",
		"honeypie_3301",
		"honeypie3301"
	);

	private static final Set<UUID> WHITELISTED_UUIDS = Set.of(
		UUID.fromString("00000000-0000-0000-0000-000000000000") // Placeholder for developer UUIDs
	);

	private static final Set<String> KNOWN_COMPATIBLE_MOD_IDS = Set.of(
		"the_backwoods",
		"irons_spellbooks",
		"ars_nouveau",
		"create",
		"createaddition",
		"createoreexcavation",
		"epicfight",
		"puffish_skills",
		"origins",
		"waystones",
		"citadel",
		"geckolib",
		"cobblemon",
		"mekanism",
		"applied_energistics2",
		"thermal",
		"botania",
		"twilightforest",
		"alexsmobs",
		"supplementaries",
		"farmersdelight",
		"sophisticatedbackpacks",
		"sophisticatedstorage",
		"iceandfire",
		"mutantmonsters",
		"simplyswords",
		"tconstruct",
		"framedblocks",
		"corpse",
		"jade",
		"jei",
		"curios",
		"balm",
		"cloth_config",
		"architectury",
		"kotlinforforge",
		"patchouli",
		"ferritecore",
		"embeddium",
		"oculus",
		"entityculling",
		"lithium",
		"immediatelyfast",
		"yacl",
		"fabric_language_kotlin",
		"modernfix",
		"c2me",
		"spark",
		"packetfixer",
		"moonlight",
		"cristellib",
		"trinkets",
		"cosmeticarmorreworked",
		"cropslover_ain",
		"cutthrough",
		"visuality",
		"distanthorizons",
		"presencefootsteps",
		"soundphysicsremastered",
		"3dskinlayers",
		"pamhc2trees",
		"pamhc2crops",
		"chipped",
		"macawsbridges",
		"macawsfurniture",
		"macawsdoors",
		"quark",
		"dungeoncrawl",
		"naturesaura",
		"blueprint",
		"structory",
		"structory_towers",
		"terrablender",
		"biomesoplenty",
		"regions_unexplored",
		"createbigcannons",
		"create_jetpack",
		"immersiveengineering",
		"pneumaticcraft",
		"industrialforegoing",
		"powah",
		"advent_of_ascension",
		"vinery",
		"the_bumblezone",
		"deeperdarker",
		"aquamirae",
		"goose_expansion",
		"scythes_vanilla_mod",
		"atlatls",
		"projecte",
		"projectexpansion",
		"darwinism",
		"arphex",
		"superbwarfare",
		"artifacts",
		"relics",
		"alexscaves",
		"bosses_of_mass_destruction",
		"apothic_attributes",
		"apotheosis",
		"attribute_fix",
		"playerex",
		"puffish_attributes",
		"enchdesc",
		"enchantinginfuser",
		"tetra",
		"simplyskills",
		"skilltree",
		"passiveskilltree",
		"levelz",
		"reskillable",
		"ironjetpacks",
		"travelersbackpack",
		"paraglider",
		"simplywalk",
		"walljump",
		"parcool",
		"parkour",
		"bettercombat",
		"combatroll",
		"playeranimator"
	);

	// COMPATIBILITY MODE: unlike KNOWN_COMPATIBLE_MOD_IDS above (pure documentation, unused by any
	// check), this is an active, server-wide tolerance widening. It is safe to use ModList here
	// because it answers "is this movement-altering mod installed on the server at all", which is
	// identical for every player and cannot be spoofed or selectively triggered by an individual
	// cheater, unlike a per-player ability flag. If any of these mods are present, every player
	// could plausibly use their mechanics, so widening thresholds for everyone is consistent and
	// not exploitable. Computed once at server start, not per-tick.
	private static final Set<String> MOVEMENT_ALTERING_MOD_IDS = Set.of(
		"paraglider", "ironjetpacks", "walljump", "parcool", "parkour", "combatroll", "tetra", "bettercombat",
		"irons_spellbooks", "ars_nouveau", "epicfight", "origins", "mekanism", "botania", "tconstruct",
		"artifacts", "relics", "alexscaves", "simplyswords", "createbigcannons", "create_jetpack",
		"projecte", "pneumaticcraft"
	);
	private static volatile boolean MOVEMENT_MOD_COMPAT_MODE = false;

	// LOGGING TOGGLES: Configure alert and logging destinations (All defaulted to false for silent enforcement)
	// NOTE: When all three are false, AntiCheat operates in silent mode: setbacks and combat cancels still trigger actively, but no messages or files are written.
	public static boolean LOG_TO_CONSOLE = false;
	public static boolean LOG_TO_FILE = false;
	public static boolean LOG_TO_STAFF_CHAT = false;

	// Log Rotation Settings (5 MB max size, keeps up to 5 historical backup files)
	private static final long MAX_LOG_FILE_SIZE = 5 * 1024 * 1024;
	private static final int MAX_LOG_BACKUPS = 5;

	// Async Single-Threaded Violation File Logger (Prevents Main Thread Disk I/O & Daemon Thread Allows Clean JVM Exit)
	private static final java.util.concurrent.ExecutorService LOG_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "AntiCheat-Log-Worker");
		t.setDaemon(true);
		return t;
	});

	// Thread-Safe Player Anti-Cheat Tracking Data
	private static final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

	// 60-Second Reconnection Cache (Mitigates VL reset by relogging)
	private static final Map<UUID, DisconnectRecord> recentDisconnectCache = new ConcurrentHashMap<>();
	private static long lastCacheCleanupTime = 0;

	public AntiCheat() {
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new AntiCheat();
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
	}

	@EventBusSubscriber
	private static class AntiCheatForgeBusEvents {

		@SubscribeEvent
		public static void serverLoad(ServerStartingEvent event) {
			playerDataMap.clear();
			recentDisconnectCache.clear();
			lastLogTimestamps.clear();
			MOVEMENT_MOD_COMPAT_MODE = MOVEMENT_ALTERING_MOD_IDS.stream()
				.anyMatch(id -> net.neoforged.fml.ModList.get().isLoaded(id));
			if (MOVEMENT_MOD_COMPAT_MODE) {
				System.out.println("[Backwoods AntiCheat] Movement-altering mod detected, compatibility tolerance widened for all players.");
			}
		}

		@SubscribeEvent
		public static void onServerStopping(ServerStoppingEvent event) {
			try {
				LOG_EXECUTOR.shutdown();
			} catch (Exception ignored) {
			}
			playerDataMap.clear();
			recentDisconnectCache.clear();
			lastLogTimestamps.clear();
		}

		@SubscribeEvent
		public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
			if (event.getEntity() instanceof ServerPlayer player) {
				UUID uuid = player.getUUID();
				DisconnectRecord record = recentDisconnectCache.remove(uuid);
				if (record != null && (System.currentTimeMillis() - record.timestamp < 60000)) {
					// Restore VL values from recent session
					playerDataMap.put(uuid, record.data);
				}
			}
		}

		@SubscribeEvent
		public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
			if (event.getEntity() instanceof ServerPlayer player) {
				UUID uuid = player.getUUID();
				PlayerData data = playerDataMap.remove(uuid);
				if (data != null) {
					// Cache VL for 60 seconds to prevent reconnect exploits
					recentDisconnectCache.put(uuid, new DisconnectRecord(data, System.currentTimeMillis()));
				}
				lastLogTimestamps.keySet().removeIf(key -> key.startsWith(uuid + ":"));
			}
		}

		// --- EVENT 1: TELEPORT EXEMPTION (Fixes /tp, pearls, portals, chorus fruit, respawns) ---
		@SubscribeEvent
		public static void onEntityTeleport(EntityTeleportEvent event) {
			if (event.getEntity() instanceof ServerPlayer player) {
				PlayerData data = getPlayerData(player);
				if (data != null) {
					Vec3 targetPos = new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ());
					data.lastPos = targetPos;
					data.lastValidPos = targetPos;
					data.teleportGraceTicks = 20; // 20 ticks of grace after teleporting
					data.movementVL = 0.0;
					data.flyVL = 0.0;
					data.serverFallDistance = 0.0F;
				}
			}
		}

		@SubscribeEvent
		public static void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
			if (event.getEntity() instanceof ServerPlayer player) {
				PlayerData data = getPlayerData(player);
				if (data != null) {
					data.lastPos = player.position();
					data.lastValidPos = player.position();
					data.teleportGraceTicks = 25;
					data.movementVL = 0.0;
					data.serverFallDistance = 0.0F;
				}
			}
		}

		@SubscribeEvent
		public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
			if (event.getEntity() instanceof ServerPlayer player) {
				PlayerData data = getPlayerData(player);
				if (data != null) {
					data.lastPos = player.position();
					data.lastValidPos = player.position();
					data.teleportGraceTicks = 25;
					data.movementVL = 0.0;
					data.serverFallDistance = 0.0F;
				}
			}
		}

		// --- EVENT 2: KNOCKBACK, DAMAGE, EXPLOSION & PUSH VELOCITY GRACE ---
		@SubscribeEvent
		public static void onLivingDamage(LivingDamageEvent.Post event) {
			if (event.getEntity() instanceof ServerPlayer player) {
				PlayerData data = getPlayerData(player);
				if (data != null) {
					data.velocityGraceTicks = 35; // 1.75s grace window for knockback momentum & arcs
				}
			}
		}

		@SubscribeEvent
		public static void onLivingKnockback(LivingKnockBackEvent event) {
			if (event.getEntity() instanceof ServerPlayer player) {
				PlayerData data = getPlayerData(player);
				if (data != null) {
					data.velocityGraceTicks = 40; // Extended grace window for Knockback II+ arcs
				}
			}
		}

		// --- EVENT 3: VEHICLE MOUNT / DISMOUNT RESYNC ---
		@SubscribeEvent
		public static void onEntityMount(EntityMountEvent event) {
			if (event.getEntityMounting() instanceof ServerPlayer player) {
				PlayerData data = getPlayerData(player);
				if (data != null) {
					data.lastPos = player.position();
					data.lastValidPos = player.position();
					data.teleportGraceTicks = 15;
				}
			}
		}

		// --- EVENT 3.5: PROJECTILE LAUNCH LOOK ANGLE CAPTURE ---
		@SubscribeEvent
		public static void onProjectileSpawn(EntityJoinLevelEvent event) {
			if (event.getEntity() instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer shooter) {
				Vec3 look = shooter.getLookAngle();
				var persistentData = projectile.getPersistentData();
				persistentData.putDouble("LaunchLookX", look.x);
				persistentData.putDouble("LaunchLookY", look.y);
				persistentData.putDouble("LaunchLookZ", look.z);
				persistentData.putBoolean("HasLaunchAngle", true);
			}

			// Firework rocket used for elytra boosting: open a short velocity grace window so the
			// resulting acceleration isn't mistaken for an elytra speed hack.
			if (event.getEntity() instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity firework
				&& firework.getOwner() instanceof ServerPlayer booster) {
				PlayerData data = getPlayerData(booster);
				data.velocityGraceTicks = Math.max(data.velocityGraceTicks, 30);
			}
		}

		// --- EVENT 4: RANGED PROJECTILE AIMBOT CHECK ---
		@SubscribeEvent
		public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
			if (!ENABLED) {
				return;
			}

			if (event.getSource().getDirectEntity() instanceof Projectile projectile && event.getSource().getEntity() instanceof ServerPlayer shooter) {
				if (shooter.isCreative() || isWhitelistedUser(shooter) || hasModBypass(shooter)) {
					return;
				}

				Entity victim = event.getEntity();
				if (victim == null || victim.equals(shooter)) {
					return;
				}

				PlayerData data = getPlayerData(shooter);

				// Retrieve shooter's look vector captured at launch time
				Vec3 shooterLook;
				var persistentData = projectile.getPersistentData();
				if (persistentData.getBoolean("HasLaunchAngle")) {
					shooterLook = new Vec3(
						persistentData.getDouble("LaunchLookX"),
						persistentData.getDouble("LaunchLookY"),
						persistentData.getDouble("LaunchLookZ")
					);
				} else {
					shooterLook = shooter.getLookAngle();
				}

				Vec3 targetDir = victim.position().subtract(shooter.position()).normalize();
				double dot = shooterLook.dot(targetDir);

				// If shooter was facing away from projectile target during fire (< -0.3 dot product)
				if (dot < -0.3 && shooter.distanceToSqr(victim) > 9.0) {
					data.rangedAimbotVL += 1.5;
					if (data.rangedAimbotVL >= 3.0) {
						logViolation(shooter, "RangedAimbot", data.rangedAimbotVL, String.format("Ranged shot outside launch camera direction (Dot: %.2f)", dot));
						event.setCanceled(true);
					}
				} else {
					data.rangedAimbotVL = Math.max(0, data.rangedAimbotVL - 0.1);
				}
			}
		}

		// --- MAIN TICK CHECK (Movement, Rotation, Timer, NoSlow, Elytra, Spoof, NoFall) ---
		@SubscribeEvent
		public static void onPlayerTick(PlayerTickEvent.Post event) {
			// Master Toggle Check
			if (!ENABLED) {
				return;
			}

			if (!(event.getEntity() instanceof ServerPlayer player)) {
				return;
			}

			// Exclude Spectator, Creative, or Whitelisted Developer Accounts
			if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
				return;
			}

			if (isWhitelistedUser(player)) {
				return;
			}

			// --- MOD COMPATIBILITY BYPASS CHECK ---
			if (hasModBypass(player)) {
				return;
			}

			PlayerData data = getPlayerData(player);

			// Decrement Grace Counters
			if (data.teleportGraceTicks > 0) {
				data.teleportGraceTicks--;
				data.lastPos = player.position();
				data.lastValidPos = player.position();
				data.serverFallDistance = 0.0F;
				return;
			}

			if (data.velocityGraceTicks > 0) {
				data.velocityGraceTicks--;
			}

			// Riptide trident launches the player at high velocity similar to an elytra/firework
			// boost. There is no dedicated "riptide activated" event on the bus, so this is detected
			// after the fact: only grant grace when the player is wet, holding an enchanted trident,
			// AND actually shows a sudden velocity spike this tick. Requiring the spike (not just
			// held-item + wet) prevents a player from standing in rain holding a trident to farm a
			// permanent, continuously-renewing speed-hack grace window.
			net.minecraft.world.item.ItemStack mainHandItem = player.getMainHandItem();
			double preGraceSpeedSq = player.getDeltaMovement().lengthSqr();
			if (mainHandItem.is(net.minecraft.world.item.Items.TRIDENT)
				&& mainHandItem.getEnchantments().keySet().stream().anyMatch(h -> h.is(Enchantments.RIPTIDE))
				&& (player.isInWater() || player.level().isRainingAt(player.blockPosition()))
				&& preGraceSpeedSq > 2.25) { // ~1.5 blocks/tick, well above passive swim/walk speed
				data.velocityGraceTicks = Math.max(data.velocityGraceTicks, 25);
			}

			// Resync if riding vehicle
			if (player.isPassenger()) {
				data.lastPos = player.position();
				data.lastValidPos = player.position();
				data.serverFallDistance = 0.0F;
				return;
			}

			long now = System.currentTimeMillis();

			// Throttled cleanup for recentDisconnectCache (runs once per 5 seconds server-wide instead of per player-tick)
			if (now - lastCacheCleanupTime > 5000) {
				lastCacheCleanupTime = now;
				if (!recentDisconnectCache.isEmpty()) {
					recentDisconnectCache.entrySet().removeIf(entry -> (now - entry.getValue().timestamp) > 60000);
				}
			}

			// --- CHECK 1: ABILITY SPOOF VALIDATION (Flight Hack Detection) ---
			boolean isMayFlyAllowed = player.gameMode.getGameModeForPlayer() == GameType.CREATIVE 
				|| player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR 
				|| player.hasEffect(MobEffects.LEVITATION)
				|| hasModBypass(player);

			if (!isMayFlyAllowed && (player.getAbilities().mayfly || player.getAbilities().flying)) {
				data.abilitySpoofVL += 2.0;
				if (data.abilitySpoofVL >= 4.0) {
					logViolation(player, "AbilitySpoof", data.abilitySpoofVL, "Unauthorized fly ability packet detected");
					// Reset flight status server-side
					player.getAbilities().mayfly = false;
					player.getAbilities().flying = false;
					player.onUpdateAbilities();
					data.abilitySpoofVL = 2.0;
				}
			} else {
				data.abilitySpoofVL = Math.max(0, data.abilitySpoofVL - 0.1);
			}

			// --- CHECK 2: DYNAMIC /tick rate, /tick freeze & /tick sprint TIMER CHECK ---
			double currentServerTickRate = 20.0;
			boolean isFrozen = false;
			boolean isSprinting = false;

			if (player.getServer() != null) {
				var tickRateManager = player.getServer().tickRateManager();
				currentServerTickRate = tickRateManager.tickrate();
				isFrozen = tickRateManager.isFrozen();
				isSprinting = tickRateManager.isSprinting();
			}

			// Suspend Timer check during /tick freeze and /tick sprint loops
			if (!isFrozen && !isSprinting && currentServerTickRate > 1.0) {
				// Primitive long[] ring buffer instead of Deque<Long>: avoids boxing 20 Long objects
				// per player on every single tick, which is a real, constant allocation cost that a
				// deque of boxed Longs incurs whether or not the timer check ever fires a violation.
				data.tickTimestamps[data.tickTimestampHead] = now;
				data.tickTimestampHead = (data.tickTimestampHead + 1) % PlayerData.TICK_WINDOW;
				if (data.tickTimestampCount < PlayerData.TICK_WINDOW) {
					data.tickTimestampCount++;
				}
				if (data.tickTimestampCount >= PlayerData.TICK_WINDOW) {
					long oldest = data.tickTimestamps[data.tickTimestampHead]; // slot about to be overwritten next is the oldest
					long elapsed = now - oldest;
					double expectedMs = (20.0 / currentServerTickRate) * 1000.0;
					double minimumThresholdMs = expectedMs * 0.60; // 40% packet batching margin for 150-250ms high ping

					if (elapsed < minimumThresholdMs && !player.isPassenger()) {
						data.timerVL += 1.5;
						if (data.timerVL > 10.0) {
							logViolation(player, "Timer", data.timerVL, String.format("Packets arriving too fast (%dms / expected %.0fms)", elapsed, expectedMs));
							data.timerVL = 5.0;
						}
					} else {
						data.timerVL = Math.max(0, data.timerVL - 0.1);
					}
				}
			}

			// --- CHECK 3: ROTATION SNAP & PITCH CLAMP CHECK ---
			float currentYaw = player.getYRot();
			float currentPitch = player.getXRot();

			// Impossible Pitch Check
			if (Math.abs(currentPitch) > 90.5F) {
				data.rotationVL += 2.0;
				logViolation(player, "InvalidPitch", data.rotationVL, "Pitch out of bounds: " + currentPitch);
				player.setXRot(Math.max(-90.0F, Math.min(90.0F, currentPitch)));
			}

			// Rotational Snap / Aimbot Lock Check
			float yawDelta = Math.abs(currentYaw - data.lastYaw);
			if (yawDelta > 180.0F) {
				yawDelta = 360.0F - yawDelta;
			}
			float pitchDelta = Math.abs(currentPitch - data.lastPitch);

			// Freecam mods can cause the reported look angle to jump when a player snaps their real
			// camera back before attacking, so this threshold is intentionally lenient (not a true
			// detection of freecam, just a tolerance so it doesn't false-flag on the snap-back).
			if (yawDelta > 120.0F && pitchDelta > 45.0F && !player.isPassenger()) {
				data.rotationSnapVL += 1.0;
				if (data.rotationSnapVL >= 4.0) {
					logViolation(player, "RotationSnap", data.rotationSnapVL, String.format("Instant camera snap (Yaw Delta: %.1f, Pitch Delta: %.1f)", yawDelta, pitchDelta));
					data.rotationSnapVL = 2.0;
				}
			} else {
				data.rotationSnapVL = Math.max(0, data.rotationSnapVL - 0.1);
			}

			// --- CHECK 4: MOVEMENT PHYSICS, TERRAIN & NOSLOW PREDICTION ---
			Vec3 currentPos = player.position();
			double dx = currentPos.x - data.lastPos.x;
			double dy = currentPos.y - data.lastPos.y;
			double dz = currentPos.z - data.lastPos.z;
			double horizontalDist = Math.sqrt(dx * dx + dz * dz);

			// GENERIC MODDED-ABILITY SPIKE GRACE: many spell/dash/jetpack/launcher mods (Iron's
			// Spellbooks, Ars Nouveau, Epic Fight, Origins, Mekanism, Botania, Tinkers' Slime Sling,
			// Artifacts, Relics, cannons, ProjectE, PneumaticCraft, etc) move the player via direct
			// setDeltaMovement calls with no damage and no vanilla knockback/teleport event, so there
			// is nothing to hook per-mod. Instead of listing every mod's classes individually (a list
			// that can never be complete), detect the PATTERN: a single-tick velocity spike far beyond
			// anything vanilla movement produces. If compat mode is on (a movement-altering mod is
			// present) and no explanation exists yet (no damage grace already open), treat it as a
			// probable ability launch and open a short, RATE-LIMITED grace window rather than
			// flagging immediately. The 2-second cooldown between grants prevents this from being
			// abused as a renewable speed-hack window, a real hack sustained over many ticks will
			// still exceed one 15-tick grace and get caught by the checks below once it expires.
			if (MOVEMENT_MOD_COMPAT_MODE && data.velocityGraceTicks == 0) {
				double spikeTotal = Math.sqrt(dx * dx + dy * dy + dz * dz);
				long nowMs = System.currentTimeMillis();
				if (spikeTotal > 4.0 && (nowMs - data.lastSpikeGraceGrantTime) > 2000) {
					data.velocityGraceTicks = 15;
					data.lastSpikeGraceGrantTime = nowMs;
				}
			}

			boolean isFlyingAllowed = player.getAbilities().flying || player.isFallFlying() || player.isPassenger() || player.isSpectator() || player.hasEffect(MobEffects.LEVITATION);
			boolean isInLiquid = player.isInWater() || player.isInLava() || player.level().getBlockState(player.blockPosition()).is(Blocks.POWDER_SNOW);
			// Crawling has its own vanilla speed profile distinct from standing/sneaking; treat it
			// like liquid for the purposes of skipping the standard speed cap so legitimate crawling
			// through 1-block gaps doesn't false-flag as NoSlow/Speed.
			boolean isCrawling = player.getPose() == net.minecraft.world.entity.Pose.SWIMMING && !player.isInWater() && !player.isInLava();

			// --- TERRAIN-AWARE FRICTION & BLOCK SPEED ALLOWANCE ---
			BlockPos feetPos = player.blockPosition();
			BlockPos groundPos = feetPos.below();
			BlockState feetState = player.level().getBlockState(feetPos);
			BlockState groundState = player.level().getBlockState(groundPos);

			double terrainMultiplier = 1.0;
			boolean isOnIce = groundState.getBlock() instanceof IceBlock 
				|| groundState.is(Blocks.PACKED_ICE) 
				|| groundState.is(Blocks.BLUE_ICE);

			if (isOnIce) {
				terrainMultiplier *= 1.85; // Ice sliding friction allowance
			} else if (groundState.is(Blocks.HONEY_BLOCK) || feetState.is(Blocks.HONEY_BLOCK)) {
				terrainMultiplier *= 0.55; // Honey block slowdown
			} else if (groundState.is(Blocks.SOUL_SAND) || groundState.is(Blocks.SOUL_SOIL)) {
				if (player.getEffect(MobEffects.MOVEMENT_SPEED) == null) {
					terrainMultiplier *= 0.65; // Soul sand slowdown without soul speed / speed effect
				}
			}

			// Piston push or flowing water allowance
			boolean isInFlowingWaterOrPiston = feetState.is(Blocks.WATER) || feetState.is(Blocks.PISTON) || feetState.is(Blocks.MOVING_PISTON);
			if (isInFlowingWaterOrPiston) {
				terrainMultiplier *= 1.75;
			}

			// --- NOSLOW CHECK (Item Usage & Sneaking) ---
			boolean isUsingItem = player.isUsingItem();
			boolean isCrouching = player.isCrouching();

			double noSlowMaxMultiplier = 1.0;
			if (isUsingItem) {
				noSlowMaxMultiplier = 0.35; // Item usage (eating, blocking, drawing bow) slows to 35% speed
			} else if (isCrouching) {
				noSlowMaxMultiplier = MOVEMENT_MOD_COMPAT_MODE ? 0.70 : 0.45; // Sliding/parkour mods can move fast while crouched
			}

			double baseAttrSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
			double maxAllowedSpeed = baseAttrSpeed * 2.8 * terrainMultiplier;

			MobEffectInstance speedEffect = player.getEffect(MobEffects.MOVEMENT_SPEED);
			if (speedEffect != null) {
				maxAllowedSpeed += maxAllowedSpeed * 0.20 * (speedEffect.getAmplifier() + 1);
			}

			if (!player.onGround()) {
				maxAllowedSpeed *= MOVEMENT_MOD_COMPAT_MODE ? 1.85 : 1.45; // Air momentum buffer, widened if a wall-run/parkour-style mod is present
			}

			if (data.velocityGraceTicks > 0) {
				maxAllowedSpeed *= 2.5; // Knockback/explosion grace multiplier
			}

			// Enforce NoSlow speed limit if using item/crouching
			if ((isUsingItem || isCrouching) && !isFlyingAllowed && !isInLiquid && !isCrawling && data.velocityGraceTicks == 0) {
				double itemSlowSpeedCap = maxAllowedSpeed * noSlowMaxMultiplier + 0.20;
				if (horizontalDist > itemSlowSpeedCap) {
					data.noSlowVL += 1.0;
					if (data.noSlowVL >= 4.0) {
						logViolation(player, "NoSlow", data.noSlowVL, String.format("Speed %.2f exceeded item slow cap %.2f (UsingItem: %b, Crouching: %b)", horizontalDist, itemSlowSpeedCap, isUsingItem, isCrouching));
						setbackPlayer(player, data.lastValidPos);
						data.noSlowVL = 2.5;
					}
				} else {
					data.noSlowVL = Math.max(0, data.noSlowVL - 0.1);
				}
			} else {
				data.noSlowVL = Math.max(0, data.noSlowVL - 0.1);
			}

			// --- CHECK 5: ELYTRA BOUNDARY CHECK ---
			if (player.isFallFlying()) {
				double totalSpeed = Math.sqrt(dx * dx + dy * dy + dz * dz);
				// Steep dives (dy < 0) gain significant vertical acceleration
				double verticalAllowance = dy < 0 ? Math.abs(dy) * 1.8 : 0.0;
				// Firework boosting accelerates forward/upward rather than diving, so it needs its
				// own allowance or legitimate boosted flight gets flagged the same as an
				// unauthorized elytra speed hack. There is no clean server-side "is boosting right
				// now" flag for fireworks, so this approximates it with a short grace window: any
				// nearby firework rocket explosion/consumption within the last few ticks. Simplest
				// reliable proxy is checking recent velocity grace ticks, which already opens
				// briefly on knockback-style impulses; extend that same window to cover boosts too.
				boolean isFireworkBoosting = data.velocityGraceTicks > 0;
				double fireworkAllowance = isFireworkBoosting ? 2.2 : 0.0;
				double maxElytraTotal = 3.5 + verticalAllowance + fireworkAllowance;
				double maxElytraHorizontal = 3.2 + (isFireworkBoosting ? 1.5 : 0.0);

				if (data.velocityGraceTicks > 0) {
					maxElytraTotal *= 1.8;
					maxElytraHorizontal *= 1.8;
				}

				if (horizontalDist > maxElytraHorizontal || totalSpeed > maxElytraTotal) {
					data.elytraSpeedVL += 1.5;
					if (data.elytraSpeedVL >= 5.0) {
						logViolation(player, "ElytraSpeed", data.elytraSpeedVL, String.format("Elytra speed (H: %.2f, Total: %.2f) exceeded limit (Max H: %.2f, Max Total: %.2f)", horizontalDist, totalSpeed, maxElytraHorizontal, maxElytraTotal));
						setbackPlayer(player, data.lastValidPos);
						data.elytraSpeedVL = 2.0;
					}
				} else {
					data.elytraSpeedVL = Math.max(0, data.elytraSpeedVL - 0.1);
				}
			}

			// --- CHECK 6: HORIZONTAL SPEED CHECK ---
			if (!isFlyingAllowed && !isInLiquid && !isCrawling && player.hurtTime == 0 && data.velocityGraceTicks == 0 && horizontalDist > maxAllowedSpeed + 0.50) {
				data.movementVL += 1.0;
				if (data.movementVL >= 6.0) {
					logViolation(player, "Speed", data.movementVL, String.format("Speed %.2f > Max %.2f", horizontalDist, maxAllowedSpeed));
					setbackPlayer(player, data.lastValidPos);
					data.movementVL = 3.0;
				}
			} else {
				data.movementVL = Math.max(0, data.movementVL - 0.05);
			}

			// --- CHECK 7: AIR HOLD / FLY CHECK ---
			if (!isFlyingAllowed && !isInLiquid && !player.onGround() && !player.onClimbable() && data.velocityGraceTicks == 0) {
				if (Math.abs(dy) < 0.0001 && player.getDeltaMovement().y >= 0) {
					data.airHoverTicks++;
					int hoverThreshold = MOVEMENT_MOD_COMPAT_MODE ? 30 : 14; // Wall-run/vault mods can legitimately hold near-zero dy longer
					if (data.airHoverTicks > hoverThreshold) {
						data.flyVL += 1.5;
						if (data.flyVL >= 5.0) {
							logViolation(player, "Fly", data.flyVL, "Hovering in mid-air for " + data.airHoverTicks + " ticks");
							setbackPlayer(player, data.lastValidPos);
							data.flyVL = 2.0;
						}
					}
				} else {
					data.airHoverTicks = 0;
				}
			} else {
				data.airHoverTicks = 0;
				data.flyVL = Math.max(0, data.flyVL - 0.1);
			}

			// --- CHECK 8: SERVER-SIDE NOFALL / FALL DAMAGE CHECK ---
			if (!player.onGround() && dy < 0 && !isFlyingAllowed && !isInLiquid && !player.hasEffect(MobEffects.SLOW_FALLING)) {
				data.serverFallDistance += (float) Math.abs(dy);
			} else if (player.onGround()) {
				boolean isSafeFallBlock = feetState.is(Blocks.SLIME_BLOCK) || feetState.is(Blocks.HAY_BLOCK) || feetState.is(Blocks.WATER) || groundState.is(Blocks.SLIME_BLOCK) || groundState.is(Blocks.HAY_BLOCK);
				
				if (data.serverFallDistance > 3.8F && player.fallDistance == 0.0F && !isSafeFallBlock && player.hurtTime == 0 && data.velocityGraceTicks == 0) {
					data.noFallVL += 1.5;
					if (data.noFallVL >= 4.0) {
						logViolation(player, "NoFall", data.noFallVL, String.format("Landed from %.1f blocks without fall damage packet", data.serverFallDistance));
						// Calculate raw fall damage
						float rawDamage = data.serverFallDistance - 3.0F;
						// Reduce fall damage based on Feather Falling enchantment on equipped boots
						net.minecraft.world.item.ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
						if (!boots.isEmpty()) {
							int featherFallingLevel = 0;
							var enchants = boots.getEnchantments();
							for (var entry : enchants.entrySet()) {
								var holder = entry.getKey();
								if (holder.is(Enchantments.FEATHER_FALLING) 
									|| holder.unwrapKey().map(k -> k.location().getPath().equals("feather_falling")).orElse(false)) {
									featherFallingLevel = entry.getValue();
									break;
								}
							}
							if (featherFallingLevel > 0) {
								float reduction = Math.min(0.48F, featherFallingLevel * 0.12F);
								rawDamage *= (1.0F - reduction);
							}
						}
						if (rawDamage > 0.5F) {
							player.hurt(player.damageSources().fall(), rawDamage);
						}
						data.noFallVL = 2.0;
					}
				} else {
					data.noFallVL = Math.max(0, data.noFallVL - 0.1);
				}
				data.serverFallDistance = 0.0F;
			}

			// Update Valid Position State
			if (player.onGround() && !isFlyingAllowed) {
				data.lastValidPos = currentPos;
			}

			data.lastPos = currentPos;
			data.lastYaw = currentYaw;
			data.lastPitch = currentPitch;
		}

		// --- COMBAT CHECKS (Reach & KillAura with Geometry Scaling) ---
		@SubscribeEvent
		public static void onPlayerAttack(AttackEntityEvent event) {
			if (!ENABLED) {
				return;
			}

			if (!(event.getEntity() instanceof ServerPlayer player)) {
				return;
			}

			Entity target = event.getTarget();
			if (target == null || !(target instanceof LivingEntity)) {
				return;
			}

			if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
				return;
			}

			if (isWhitelistedUser(player) || hasModBypass(player)) {
				return;
			}

			PlayerData data = getPlayerData(player);

			// --- A0. ATTACK COOLDOWN CHECK (Full-damage hit-spam prevention) ---
			// player.getAttackStrengthScale(0.5F) mirrors vanilla's own cooldown calculation; a
			// hacked client suppressing cooldown client-side still shows up here since this reads
			// server-authoritative swing timing, not anything the client reports.
			float attackStrength = player.getAttackStrengthScale(0.5F);
			float attackCooldownThreshold = MOVEMENT_MOD_COMPAT_MODE ? 0.20F : 0.35F; // Combat roll/BetterCombat can legitimately alter swing timing
			if (attackStrength < attackCooldownThreshold) {
				data.attackCooldownVL += 1.0;
				if (data.attackCooldownVL >= 5.0) {
					logViolation(player, "AttackCooldown", data.attackCooldownVL, String.format("Hit registered at %.2f attack strength (bypassing cooldown penalty)", attackStrength));
					data.attackCooldownVL = 2.5;
				}
			} else {
				data.attackCooldownVL = Math.max(0, data.attackCooldownVL - 0.15);
			}

			// --- A. REACH CHECK (Eye-to-AABB Distance) ---
			Vec3 eyePos = player.getEyePosition(1.0F);
			AABB targetBox = target.getBoundingBox().inflate(0.1);
			
			double distSq = targetBox.distanceToSqr(eyePos);
			double reachDist = Math.sqrt(distSq);

			double baseAttributeReach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
			// Crouching, swimming, and crawling shift effective eye height/hitbox slightly versus
			// standing; give a small pose-based allowance so legitimate crouched combat isn't tight
			// against the threshold.
			double poseAllowance = (player.isCrouching() || player.isSwimming() || player.getPose() == net.minecraft.world.entity.Pose.SWIMMING) ? 0.25 : 0.0;
			double compatReachAllowance = MOVEMENT_MOD_COMPAT_MODE ? 0.75 : 0.0; // Tetra/BetterCombat can apply non-attribute reach bonuses
			double maxReachThreshold = Math.max(3.0, baseAttributeReach) + 0.85 + poseAllowance + compatReachAllowance;

			if (reachDist > maxReachThreshold) {
				data.reachVL += 1.0;
				if (data.reachVL >= 2.0) {
					logViolation(player, "Reach", data.reachVL, String.format("Attacked target from %.2f blocks (Max Allowed: %.2f)", reachDist, maxReachThreshold));
					event.setCanceled(true);
					return;
				}
			} else {
				data.reachVL = Math.max(0, data.reachVL - 0.2);
			}

			// --- B. GEOMETRY-AWARE KILLAURA / FOV CHECK ---
			Vec3 targetCenter = target.position();
			double bbWidth = target.getBbWidth();

			// Widened from -0.15 to -0.35 to reduce false flags from freecam mods that briefly report
			// a stale or transitional look angle during the camera snap-back on attack.
			double fovThreshold = -0.35;
			if (bbWidth > 2.0) {
				fovThreshold = -0.65;
			}

			Vec3 dirToTarget = targetCenter.subtract(player.position()).normalize();
			Vec3 lookDir = player.getLookAngle();
			double dotProduct = lookDir.dot(dirToTarget);

			if (dotProduct < fovThreshold && bbWidth <= 3.5) {
				data.auraVL += 1.5;
				if (data.auraVL >= 3.0) {
					logViolation(player, "KillAura", data.auraVL, "Attacking entity outside camera FOV (Dot: " + String.format("%.2f", dotProduct) + ")");
					event.setCanceled(true);
				}
			} else {
				data.auraVL = Math.max(0, data.auraVL - 0.1);
			}
		}
	}

	// Helper: Get or Create Thread-Safe PlayerData
	private static PlayerData getPlayerData(ServerPlayer player) {
		return playerDataMap.computeIfAbsent(player.getUUID(), k -> new PlayerData(player));
	}

	// Helper: Dual Whitelist Checker (UUID + Username)
	private static boolean isWhitelistedUser(ServerPlayer player) {
		if (WHITELISTED_UUIDS.contains(player.getUUID())) {
			return true;
		}
		String username = player.getScoreboardName();
		return WHITELISTED_USERNAMES.contains(username);
	}

	// Helper: Dynamic Ability and Mod Bypass Checker
	// Checks active custom ability flags populated by The Backwoods procedures, plus engine-level passenger/riding states
	private static boolean hasModBypass(ServerPlayer player) {
		// Native engine checks: riding entities, vehicles, or passenger states
		if (player.isPassenger() || player.getVehicle() != null || player.getRootVehicle() != player) {
			return true;
		}

		var persistentData = player.getPersistentData();

		// Check active NBT ability flags set by The Backwoods or addon procedures
		return persistentData.getBoolean("BackwoodsAbilityActive") || 
			persistentData.getBoolean("IsHypnotized") || 
			persistentData.getBoolean("SpellCasting") || 
			persistentData.getBoolean("DashActive") || 
			persistentData.getBoolean("FlightActive") || 
			persistentData.getBoolean("IsTeleporting") || 
			persistentData.getBoolean("SuperpowerActive") || 
			persistentData.getBoolean("GodMode") || 
			persistentData.getBoolean("ModdedFlight") || 
			persistentData.getBoolean("CustomMovement");
	}

	// Helper: Setback Player to Safe Location
	private static void setbackPlayer(ServerPlayer player, Vec3 setbackPos) {
		if (setbackPos != null) {
			player.teleportTo(player.serverLevel(), setbackPos.x, setbackPos.y, setbackPos.z, java.util.Set.of(), player.getYRot(), player.getXRot());
			// Zero out velocity so a speed/fly client doesn't resume the same illegal motion next tick
			player.setDeltaMovement(Vec3.ZERO);
			player.hurtMarked = true;
		}
	}

	// Debounce window: suppress repeat log entries for the same player+check within this many ms,
	// prevents log spam from a player oscillating right around a threshold.
	private static final long LOG_DEBOUNCE_MS = 4000;
	private static final Map<String, Long> lastLogTimestamps = new ConcurrentHashMap<>();

	// Helper: Log AntiCheat Flag to Server Console, Rolling Log File & Staff Alert
	private static void logViolation(ServerPlayer player, String checkName, double vl, String details) {
		String debounceKey = player.getUUID() + ":" + checkName;
		long now = System.currentTimeMillis();
		Long lastLogged = lastLogTimestamps.get(debounceKey);
		if (lastLogged != null && (now - lastLogged) < LOG_DEBOUNCE_MS) {
			return;
		}
		lastLogTimestamps.put(debounceKey, now);

		String msg = String.format("[Backwoods AntiCheat] %s failed %s (VL: %.1f) - %s", player.getScoreboardName(), checkName, vl, details);
		
		if (LOG_TO_CONSOLE) {
			System.out.println(msg);
		}

		if (LOG_TO_FILE) {
			writeViolationToFile(msg);
		}

		if (LOG_TO_STAFF_CHAT && player.getServer() != null) {
			Component alertComponent = Component.literal("§c[AntiCheat] §f" + player.getScoreboardName() + " §7failed §e" + checkName + " §7(VL: " + String.format("%.1f", vl) + ")");
			player.getServer().getPlayerList().getPlayers().forEach(p -> {
				if (p.hasPermissions(2)) {
					p.sendSystemMessage(alertComponent);
				}
			});
		}
	}

	private static void writeViolationToFile(String logMessage) {
		if (!LOG_TO_FILE) {
			return;
		}

		LOG_EXECUTOR.submit(() -> {
			try {
				java.io.File dir = new java.io.File("logs");
				if (!dir.exists()) {
					dir.mkdirs();
				}
				java.io.File logFile = new java.io.File(dir, "anticheat-violations.log");
				
				// Multi-generation rolling log rotation (Keeps up to MAX_LOG_BACKUPS generations)
				if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
					// Shift existing backup files (.4 -> .5, .3 -> .4, .2 -> .3, .1 -> .2)
					for (int i = MAX_LOG_BACKUPS; i >= 1; i--) {
						java.io.File currentBackup = new java.io.File(dir, "anticheat-violations." + i + ".log");
						if (i == MAX_LOG_BACKUPS) {
							if (currentBackup.exists()) {
								currentBackup.delete();
							}
						} else {
							if (currentBackup.exists()) {
								java.io.File nextBackup = new java.io.File(dir, "anticheat-violations." + (i + 1) + ".log");
								if (nextBackup.exists()) {
									nextBackup.delete();
								}
								currentBackup.renameTo(nextBackup);
							}
						}
					}
					// Rotate active log to .1.log
					java.io.File backupOne = new java.io.File(dir, "anticheat-violations.1.log");
					logFile.renameTo(backupOne);
				}

				java.time.LocalDateTime now = java.time.LocalDateTime.now();
				String timestampedMessage = String.format("[%s] %s%n", now.toString(), logMessage);
				java.nio.file.Files.writeString(
					logFile.toPath(), 
					timestampedMessage, 
					java.nio.file.StandardOpenOption.CREATE, 
					java.nio.file.StandardOpenOption.APPEND
				);
			} catch (Exception ignored) {
			}
		});
	}

	// Record class for short-lived reconnect cache
	private static class DisconnectRecord {
		public final PlayerData data;
		public final long timestamp;

		public DisconnectRecord(PlayerData data, long timestamp) {
			this.data = data;
			this.timestamp = timestamp;
		}
	}

	// Internal Player Tracking Data Model
	private static class PlayerData {
		public Vec3 lastPos;
		public Vec3 lastValidPos;
		public float lastYaw;
		public float lastPitch;

		public int airHoverTicks = 0;
		public int teleportGraceTicks = 0;
		public int velocityGraceTicks = 0;
		public float serverFallDistance = 0.0F;

		public double movementVL = 0.0;
		public double flyVL = 0.0;
		public double reachVL = 0.0;
		public double auraVL = 0.0;
		public double rotationVL = 0.0;
		public double rotationSnapVL = 0.0;
		public double timerVL = 0.0;
		public double noSlowVL = 0.0;
		public double elytraSpeedVL = 0.0;
		public double abilitySpoofVL = 0.0;
		public double noFallVL = 0.0;
		public double rangedAimbotVL = 0.0;
		public double attackCooldownVL = 0.0;
		public long lastSpikeGraceGrantTime = 0L;

		public static final int TICK_WINDOW = 20;
		public final long[] tickTimestamps = new long[TICK_WINDOW];
		public int tickTimestampHead = 0;
		public int tickTimestampCount = 0;

		public PlayerData(ServerPlayer player) {
			this.lastPos = player.position();
			this.lastValidPos = player.position();
			this.lastYaw = player.getYRot();
			this.lastPitch = player.getXRot();
		}
	} // 1.21.1
}