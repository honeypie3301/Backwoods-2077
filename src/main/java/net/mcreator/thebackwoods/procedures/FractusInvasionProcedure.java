package net.mcreator.thebackwoods.procedures;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3f;

@EventBusSubscriber
public class FractusInvasionProcedure {

	// CONFIGURABLE CONSTANTS FOR SPAWN TIMING, QUANTITIES AND SCALING
	private static final int BASE_COOLDOWN_TICKS = 2400;          // 2 minutes default interval between breaches
	private static final int MIN_COOLDOWN_TICKS = 900;            // 45 seconds minimum interval under maximum escalation
	private static final int COOLDOWN_REDUCTION_INTERVAL = 1200;  // Decrease cooldown threshold for every 1 minute held (1200 ticks)
	private static final int COOLDOWN_REDUCTION_AMOUNT = 300;     // Reduce cooldown by 15 seconds per step of held time

	private static final int BREACH_PREPARATION_DURATION = 140;   // 7 seconds of theatrical outer blue vortex build-up (Stage 2)
	private static final int BREACH_ACTIVE_DURATION = 120;        // 6 seconds of steady gateway opening and staggered summons (Stage 3)

	private static final double PORTAL_MAX_RADIUS = 2.8;          // Massive Avengers visual scale

	// CONFIGURABLE: Fractus spawn quantities per breach occurrence
	private static final int MIN_SPAWN_COUNT = 3;                 // Minimum Fractus spawned under normal conditions
	private static final int MAX_SPAWN_COUNT = 7;                 // Maximum base Fractus spawned (before escalation bonus)

	// SPARK OPTIMIZATION: Cached references for 0-allocation item/block lookups
	private static Item cachedCoreItem = null;
	private static Block cachedCoreBlock = null;
	private static EntityType<?> cachedFractusType = null;

	private static Item getFractusCoreItem() {
		if (cachedCoreItem == null) {
			cachedCoreItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("the_backwoods:fractus_core"));
			if (cachedCoreItem == null) cachedCoreItem = Items.AIR;
		}
		return cachedCoreItem;
	}

	private static Block getFractusCoreBlock() {
		if (cachedCoreBlock == null) {
			cachedCoreBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("the_backwoods:fractus_core"));
			if (cachedCoreBlock == null) cachedCoreBlock = Blocks.AIR;
		}
		return cachedCoreBlock;
	}

	private static EntityType<?> getFractusEntityType() {
		if (cachedFractusType == null) {
			cachedFractusType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("the_backwoods:fractus"));
		}
		return cachedFractusType;
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player player && !player.level().isClientSide() && player.level() instanceof ServerLevel level) {
			if (level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER) {
				processInvasionStates(level, player);
			}
		}
	}

	private static void processInvasionStates(ServerLevel level, Player player) {
		int checkTimer = player.getPersistentData().getInt("fractus_core_check_timer");
		boolean hasCore = false;
		if (checkTimer <= 0) {
			hasCore = hasFractusCoreNearby(level, player);
			player.getPersistentData().putBoolean("fractus_has_core_cached", hasCore);
			// Check every 60 ticks (3 seconds) to minimize chunk scanning overhead
			player.getPersistentData().putInt("fractus_core_check_timer", 60);
		} else {
			player.getPersistentData().putInt("fractus_core_check_timer", checkTimer - 1);
			hasCore = player.getPersistentData().getBoolean("fractus_has_core_cached");
		}

		// Global escalation hold-tracking (Stage 1)
		int holdTime = player.getPersistentData().getInt("fractus_core_hold_time");
		if (hasCore) {
			holdTime++;
		} else {
			if (holdTime > 0) {
				holdTime = Math.max(0, holdTime - 5); // Decays 5 times faster if discarded
			}
		}
		player.getPersistentData().putInt("fractus_core_hold_time", holdTime);

		// Tick down active breach cooldowns
		int cooldown = player.getPersistentData().getInt("fractus_invasion_cooldown");
		if (cooldown > 0) {
			player.getPersistentData().putInt("fractus_invasion_cooldown", cooldown - 1);
			return;
		}

		int portalState = player.getPersistentData().getInt("fractus_portal_state"); // 0 = Idle, 1 = Forming (Stage 2), 2 = Active (Stage 3)

		// State 0: IDLE
		if (portalState == 0) {
			// Require the player to hold the core for at least 10 seconds (200 ticks) before the portal triggers
			if (hasCore && holdTime >= 200) {
				BlockPos breachPos = findBreachPosition(level, player);
				if (breachPos != null) {
					double px = breachPos.getX() + 0.5;
					double py = breachPos.getY() + 1.8;
					double pz = breachPos.getZ() + 0.5;

					// Calculate angle facing the player's core
					double dx = player.getX() - px;
					double dz = player.getZ() - pz;
					double yaw = Math.atan2(dz, dx); // Radians angle towards player

					player.getPersistentData().putInt("fractus_portal_state", 1);
					player.getPersistentData().putInt("fractus_portal_ticks", BREACH_PREPARATION_DURATION);
					player.getPersistentData().putDouble("fractus_portal_x", px);
					player.getPersistentData().putDouble("fractus_portal_y", py);
					player.getPersistentData().putDouble("fractus_portal_z", pz);
					player.getPersistentData().putDouble("fractus_portal_yaw", yaw);

					// Trigger initialization thunder sound
					level.playSound(null, px, py, pz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.5F, 0.45F);
					level.playSound(null, px, py, pz, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4.0F, 0.50F);
				}
			}
		}
		// State 1: BREACH FORMING (Stage 2)
		else if (portalState == 1) {
			int ticksLeft = player.getPersistentData().getInt("fractus_portal_ticks") - 1;
			double px = player.getPersistentData().getDouble("fractus_portal_x");
			double py = player.getPersistentData().getDouble("fractus_portal_y");
			double pz = player.getPersistentData().getDouble("fractus_portal_z");
			double yaw = player.getPersistentData().getDouble("fractus_portal_yaw");

			// Periodic powerful beacon low-pitch rumble loop simulating cosmic expansion
			if (ticksLeft % 20 == 0) {
				level.playSound(null, px, py, pz, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 3.5F, 0.45F);
			}
			if (ticksLeft % 25 == 0) {
				level.playSound(null, px, py, pz, SoundEvents.PORTAL_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.55F);
			}

			// Render Space Stone colorable vanilla dust portal growing outwards (throttled smoothly)
			double progress = (double) (BREACH_PREPARATION_DURATION - ticksLeft) / BREACH_PREPARATION_DURATION;
			spawnSpaceStoneVisuals(level, px, py, pz, yaw, progress * PORTAL_MAX_RADIUS);

			if (ticksLeft <= 0) {
				player.getPersistentData().putInt("fractus_portal_state", 2);
				player.getPersistentData().putInt("fractus_portal_ticks", BREACH_ACTIVE_DURATION);

				// Determine exact stable spawn quantities based on min/max settings
				int minS = MIN_SPAWN_COUNT;
				int maxS = MAX_SPAWN_COUNT;
				int baseCount = minS;
				if (maxS > minS) {
					baseCount = minS + level.getRandom().nextInt(maxS - minS + 1);
				}
				int bonus = Math.min(3, (holdTime / COOLDOWN_REDUCTION_INTERVAL) / 2); // Escalation bonus
				int totalToSpawn = baseCount + bonus;
				player.getPersistentData().putInt("fractus_spawn_total_count", totalToSpawn);

				// High-energy ignition sound when portal becomes fully active
				level.playSound(null, px, py, pz, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 3.0F, 0.50F);
			} else {
				player.getPersistentData().putInt("fractus_portal_ticks", ticksLeft);
			}
		}
		// State 2: ARRIVAL / ACTIVE PORTAL (Stage 3)
		else if (portalState == 2) {
			int ticksLeft = player.getPersistentData().getInt("fractus_portal_ticks") - 1;
			double px = player.getPersistentData().getDouble("fractus_portal_x");
			double py = player.getPersistentData().getDouble("fractus_portal_y");
			double pz = player.getPersistentData().getDouble("fractus_portal_z");
			double yaw = player.getPersistentData().getDouble("fractus_portal_yaw");

			// Continuously stream the space sound hum
			if (ticksLeft % 15 == 0) {
				level.playSound(null, px, py, pz, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 4.0F, 0.35F);
			}

			// Render visually stable or slightly pulsing portal size
			double currentRadius = PORTAL_MAX_RADIUS;
			if (ticksLeft <= 30) {
				// Smoothly shrink on final collapse phase
				currentRadius = ((double) ticksLeft / 30.0) * PORTAL_MAX_RADIUS;
			}
			spawnSpaceStoneVisuals(level, px, py, pz, yaw, currentRadius);

			// Staggered summoning sequence: even and smooth distribution
			int totalToSpawn = player.getPersistentData().getInt("fractus_spawn_total_count");
			if (totalToSpawn > 0) {
				int interval = BREACH_ACTIVE_DURATION / (totalToSpawn + 1);
				if (interval <= 0) interval = 12;
				int currentElapsed = BREACH_ACTIVE_DURATION - ticksLeft;
				if (currentElapsed > 0 && currentElapsed % interval == 0 && (currentElapsed / interval) <= totalToSpawn) {
					spawnAndLaunchFractus(level, player, px, py, pz, yaw);
				}
			}

			if (ticksLeft <= 0) {
				// Collapse sound trigger
				level.playSound(null, px, py, pz, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0F, 0.60F);
				level.playSound(null, px, py, pz, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.80F);

				// Put portal into final cooldown
				int reduction = Math.min(BASE_COOLDOWN_TICKS - MIN_COOLDOWN_TICKS, (holdTime / COOLDOWN_REDUCTION_INTERVAL) * COOLDOWN_REDUCTION_AMOUNT);
				int nextCooldown = BASE_COOLDOWN_TICKS - reduction;
				// organic variance (+/- 15 second delay fluctuation)
				nextCooldown += (level.getRandom().nextInt(600) - 300);

				player.getPersistentData().putInt("fractus_invasion_cooldown", Math.max(MIN_COOLDOWN_TICKS, nextCooldown));
				player.getPersistentData().putInt("fractus_portal_state", 0);
				player.getPersistentData().putInt("fractus_spawn_total_count", 0);
			} else {
				player.getPersistentData().putInt("fractus_portal_ticks", ticksLeft);
			}
		}
	}

	private static void spawnSpaceStoneVisuals(ServerLevel level, double cx, double cy, double cz, double yaw, double radius) {
		if (radius <= 0.05) return;

		// SPARK OPTIMIZATION: Run particle generation every 2 ticks to cut network packet serialization by 50%
		if (level.getGameTime() % 2 != 0) return;

		net.minecraft.util.RandomSource rnd = level.getRandom();

		// Face orientation trigonometry
		double perpX = -Math.sin(yaw);
		double perpZ = Math.cos(yaw);

		// Backward vector tunneling deep into the breach
		double bx = -Math.cos(yaw);
		double bz = -Math.sin(yaw);

		// Volumetric Space Stone Color Palette:
		DustParticleOptions spaceSmoke = new DustParticleOptions(new Vector3f(0.35F, 0.38F, 0.42F), 1.95F);
		DustParticleOptions spaceVortex = new DustParticleOptions(new Vector3f(0.00F, 0.30F, 0.95F), 1.65F);
		DustParticleOptions spaceCyan = new DustParticleOptions(new Vector3f(0.15F, 0.88F, 1.00F), 1.75F);
		DustParticleOptions electricArc = new DustParticleOptions(new Vector3f(0.50F, 0.95F, 1.00F), 1.15F);

		// Optimized 5-slice 3D funnel curving backwards
		int totalSlices = 5;
		for (int s = 0; s < totalSlices; s++) {
			double d = s * 0.35;
			double rAtD = radius * (1.0 - (s / (double) totalSlices) * 0.55);
			if (rAtD <= 0.1) continue;

			// A. Outer smoky border shell
			int borderCount = (int) (rAtD * 8);
			for (int i = 0; i < borderCount; i++) {
				double theta = rnd.nextDouble() * 2.0 * Math.PI;
				double jitterRadius = rAtD + (rnd.nextDouble() - 0.5) * 0.25;
				double depthOffset = d + (1.0 - (jitterRadius / rAtD)) * 0.52;
				
				double px = cx + depthOffset * bx + jitterRadius * Math.cos(theta) * perpX;
				double py = cy + jitterRadius * Math.sin(theta);
				double pz = cz + depthOffset * bz + jitterRadius * Math.cos(theta) * perpZ;

				level.sendParticles(spaceSmoke, px, py, pz, 1, 0.0, 0.0, 0.0, 0.01);
			}

			// B. Hollow central swirling energy disk
			int swirlCount = (int) (rAtD * 14);
			for (int i = 0; i < swirlCount; i++) {
				double theta = rnd.nextDouble() * 2.0 * Math.PI;
				double minFraction = (s < 2) ? 0.60 : ((s < 4) ? 0.30 : 0.0);
				double r = rAtD * (minFraction + (1.0 - minFraction) * rnd.nextDouble());
				double depthOffset = d + (1.0 - (r / rAtD)) * 0.52;

				double px = cx + depthOffset * bx + r * Math.cos(theta) * perpX;
				double py = cy + r * Math.sin(theta);
				double pz = cz + depthOffset * bz + r * Math.cos(theta) * perpZ;

				DustParticleOptions activeColor = (s >= 3 || r < rAtD * 0.25) ? spaceCyan : spaceVortex;
				double vx = -Math.sin(theta) * perpX * 0.065;
				double vy = Math.cos(theta) * 0.065;
				double vz = -Math.sin(theta) * perpZ * 0.065;

				level.sendParticles(activeColor, px, py, pz, 1, vx, vy, vz, 0.02);
			}
		}

		// C. Crackling electrical discharge loops
		int electricCount = (int) (radius * 4);
		for (int i = 0; i < electricCount; i++) {
			double theta = rnd.nextDouble() * 2.0 * Math.PI;
			double d = rnd.nextDouble() * 1.8;
			double r = (0.2 + rnd.nextDouble() * 0.8) * radius * (1.0 - (d / 1.8) * 0.55);
			double depthOffset = d + (1.0 - (r / (radius * (1.0 - (d / 1.8) * 0.55)))) * 0.52;

			double px = (cx + depthOffset * bx) + r * Math.cos(theta) * perpX;
			double py = cy + r * Math.sin(theta);
			double pz = (cz + depthOffset * bz) + r * Math.cos(theta) * perpZ;

			double rx = (rnd.nextDouble() - 0.5) * 0.25;
			double ry = (rnd.nextDouble() - 0.5) * 0.25;
			double rz = (rnd.nextDouble() - 0.5) * 0.25;

			level.sendParticles(electricArc, px + rx, py + ry, pz + rz, 1, 0.0, 0.0, 0.0, 0.02);
		}
	}

	private static void spawnAndLaunchFractus(ServerLevel level, Player player, double cx, double cy, double cz, double yaw) {
		EntityType<?> type = getFractusEntityType();
		if (type != null) {
			Entity summon = type.create(level);
			if (summon != null) {
				// Align initial orientation standing straight, facing forward yaw
				summon.moveTo(cx, cy - 0.5, cz, (float) (yaw * 180.0 / Math.PI), 0.0F);

				// Direct mathematical forward push vector out of the visual portal frame
				double forwardX = Math.cos(yaw);
				double forwardZ = Math.sin(yaw);

				// Imparts smooth horizontal launch speed and subtle levitation thrust
				summon.setDeltaMovement(forwardX * 0.42, 0.14, forwardZ * 0.42);
				summon.hurtMarked = true; // Signals clients to sync new velocities smoothly

				if (summon instanceof Mob mob) {
					mob.setTarget(player); // Immediately locks hostility onto the core handler
				}

				level.addFreshEntity(summon);

				// Heavy summoning spatial burst sounds and visual exit shockwaves
				level.playSound(null, cx, cy, cz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.8F, 0.80F);
				level.playSound(null, cx, cy, cz, SoundEvents.SHULKER_TELEPORT, SoundSource.HOSTILE, 1.5F, 0.90F);

				// Beautiful cyan dust spatial emission puff
				level.sendParticles(new DustParticleOptions(new Vector3f(0.0F, 0.90F, 1.0F), 1.7F), cx, cy, cz, 30, 0.5, 0.5, 0.5, 0.10);
			}
		}
	}

	private static boolean hasFractusCore(Player player) {
		Item coreItem = getFractusCoreItem();
		if (coreItem == Items.AIR) return false;

		// Direct reference comparison with 0 string allocations
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (!stack.isEmpty() && stack.is(coreItem)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasFractusCoreNearby(ServerLevel level, Player player) {
		// FAST PATH: 99.9% of cases the player carries it directly
		if (hasFractusCore(player)) {
			return true;
		}

		Item coreItem = getFractusCoreItem();
		if (coreItem == Items.AIR) return false;

		// 1. Scan for dropped ItemEntity items on ground within realistic 32-block radius
		double scanRange = 32.0;
		java.util.List<net.minecraft.world.entity.item.ItemEntity> items = level.getEntitiesOfClass(
			net.minecraft.world.entity.item.ItemEntity.class,
			player.getBoundingBox().inflate(scanRange),
			e -> {
				ItemStack stack = e.getItem();
				return !stack.isEmpty() && stack.is(coreItem);
			}
		);
		if (!items.isEmpty()) {
			return true;
		}

		// 2. Scan container block entities in surrounding loaded chunks (48-block radius)
		int playerChunkX = player.blockPosition().getX() >> 4;
		int playerChunkZ = player.blockPosition().getZ() >> 4;
		int chunkRadius = 3; // 48-block horizontal radius
		double maxDistSq = 48.0 * 48.0;
		double maxYDiff = 32.0;

		for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
			for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
				int cx = playerChunkX + dx;
				int cz = playerChunkZ + dz;
				if (level.getChunkSource().hasChunk(cx, cz)) {
					net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(cx, cz);
					for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
						if (be != null && !be.isRemoved() && be instanceof net.minecraft.world.Container container) {
							BlockPos pos = be.getBlockPos();
							if (Math.abs(pos.getY() - player.getY()) <= maxYDiff && pos.distToCenterSqr(player.position()) <= maxDistSq) {
								for (int i = 0; i < container.getContainerSize(); i++) {
									ItemStack stack = container.getItem(i);
									if (!stack.isEmpty() && stack.is(coreItem)) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		}

		// 3. Fast palette-filtered placed block scan in loaded chunks
		Block coreBlock = getFractusCoreBlock();
		if (coreBlock != Blocks.AIR) {
			for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
				for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
					int cx = playerChunkX + dx;
					int cz = playerChunkZ + dz;
					if (level.getChunkSource().hasChunk(cx, cz)) {
						net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(cx, cz);
						net.minecraft.world.level.chunk.LevelChunkSection[] sections = chunk.getSections();
						for (int sIndex = 0; sIndex < sections.length; sIndex++) {
							net.minecraft.world.level.chunk.LevelChunkSection section = sections[sIndex];
							if (section == null || section.hasOnlyAir()) {
								continue;
							}

							// Fast palette check: skips scanning 4096 blocks unless section actually has core
							if (section.getStates().maybeHas(state -> state.is(coreBlock))) {
								for (int lx = 0; lx < 16; lx++) {
									for (int lz = 0; lz < 16; lz++) {
										for (int ly = 0; ly < 16; ly++) {
											if (section.getBlockState(lx, ly, lz).is(coreBlock)) {
												return true;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	private static BlockPos findBreachPosition(ServerLevel level, Player player) {
		double px = player.getX();
		double py = player.getY();
		double pz = player.getZ();
		net.minecraft.util.RandomSource random = level.getRandom();

		double playerYaw = (player.getYRot() + 90.0F) * (Math.PI / 180.0);
		int[] dyOffsets = {0, 1, 2, -1, -2, 3};

		// Pass 1: Try to find a completely clear floating space (3x3x4 of air) near the player's height
		for (int attempts = 0; attempts < 40; attempts++) {
			double angle = playerYaw + (random.nextDouble() - 0.5) * (Math.PI * 80.0 / 180.0);
			double dist = 5.5 + random.nextDouble() * 4.5;

			double targetX = px + Math.cos(angle) * dist;
			double targetZ = pz + Math.sin(angle) * dist;

			for (int dy : dyOffsets) {
				BlockPos candidatePos = BlockPos.containing(targetX, py + dy, targetZ);

				boolean clear = true;
				CheckBounds:
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						for (int h = 0; h < 4; h++) {
							BlockPos checkPos = candidatePos.offset(dx, h, dz);
							if (!level.isEmptyBlock(checkPos) && level.getFluidState(checkPos).isEmpty()) {
								clear = false;
								break CheckBounds;
							}
						}
					}
				}

				if (clear) {
					return candidatePos;
				}
			}
		}

		// Pass 2 (Fallback): If no floating space found, locate ground position close to player
		for (int attempts = 0; attempts < 25; attempts++) {
			double angle = playerYaw + (random.nextDouble() - 0.5) * (Math.PI * 80.0 / 180.0);
			double dist = 6.0 + random.nextDouble() * 5.0;

			double targetX = px + Math.cos(angle) * dist;
			double targetZ = pz + Math.sin(angle) * dist;

			BlockPos basePos = BlockPos.containing(targetX, py, targetZ);
			BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, basePos);

			if (Math.abs(surfacePos.getY() - py) <= 8.0) {
				boolean clear = true;
				CheckSurface:
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						for (int h = 0; h < 3; h++) {
							BlockPos checkPos = surfacePos.offset(dx, h, dz);
							if (!level.isEmptyBlock(checkPos) && level.getFluidState(checkPos).isEmpty()) {
								clear = false;
								break CheckSurface;
							}
						}
					}
				}
				if (clear) {
					return surfacePos;
				}
			}
		}

		// Ultimate Fallback: Place in front of player's gaze floating in air
		double fallbackX = px + Math.cos(playerYaw) * 6.0;
		double fallbackZ = pz + Math.sin(playerYaw) * 6.0;
		return BlockPos.containing(fallbackX, py + 1.0, fallbackZ);
	}
} // 1.21.1
