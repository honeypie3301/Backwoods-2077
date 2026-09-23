package net.mcreator.thebackwoods.procedures;

// 1.21.1 neoforge
import net.mcreator.thebackwoods.TheBackwoodsMod;
import net.mcreator.thebackwoods.entity.RotEntity;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

@EventBusSubscriber
public class ImmuneSystemProcedure {

	// ---------- CONFIGURABLE/TUNABLE COOLDOWNS ----------
	// Cooldown in ticks for spawning a Rot guardian from Spore Threat
	public static int SUMMON_COOLDOWN_TICKS = 20 * 60 * 10; // 10 mins
	// Minimum small spore entities (< 150hp) nearby to start having a chance to summon the Rot (default: 25)
	public static int MIN_SMALL_SPORE_THRESHOLD = 35;
	// Maximum small spore entities (< 150hp) nearby at which the chance to summon the Rot is 100% (default: 100)
	public static int MAX_SMALL_SPORE_THRESHOLD = 100;

	// Cooldown in ticks for spawning a Splinter cocoon (Tier 2)
	public static int SPLINTER_COOLDOWN_TICKS = 20 * 60 * 1; // 1 min (faster activation!)
	public static int MIN_SPLINTER_SPORE_THRESHOLD = 14;
	public static int MAX_SPLINTER_SPORE_THRESHOLD = 100;
	// ----------------------------------------------------

	private static final ResourceKey<Level> BACKWOODS_DIM = ResourceKey.create(
		Registries.DIMENSION,
		ResourceLocation.parse("the_backwoods:backwoods")
	);

	private static final String K_SPORE_THREAT = "bw_spore_threat";
	private static final String K_LAST_SPORE_DECAY = "bw_last_spore_decay";
	private static final String K_LAST_SPORE_WARN = "bw_last_spore_warn";
	private static final String K_LAST_SPORE_SUMMON = "bw_last_spore_summon";
	private static final String K_LAST_SPLINTER_SUMMON = "bw_last_splinter_summon";

	private static final java.util.List<ActiveCocoon> ACTIVE_COCOONS = new java.util.concurrent.CopyOnWriteArrayList<>();
	private static long lastTickedGameTime = -1;

	private static final String[] SPORE_WARN_LINES = new String[] {
		"Vile creatures.",
		"Leave or face death.",
		"This pale sickness does not belong here.",
		"Pruning must begin.",
		"The dimension rejects this corruption.",
		"Trespassers."
	};

	public static boolean SHOW_COCOON_MESSAGES = true;

	public static class ActiveCocoon {
		public final BlockPos center;
		public final ServerLevel level;
		public final int totalGestationTicks;
		public final int incubationTicks;
		public final int numSplinters;
		public final int radius;
		public int elapsedTicks;
		public final List<BlockPos> shellPositions = new java.util.ArrayList<>();
		public final List<BlockPos> placedBlocks = new java.util.ArrayList<>();

		public ActiveCocoon(ServerLevel level, BlockPos center, int numSplinters) {
			this.level = level;
			this.center = center;
			this.numSplinters = numSplinters;
			this.radius = numSplinters <= 3 ? 2 : 3;
			this.totalGestationTicks = 60 + numSplinters * 15; // 3 to 6 seconds building phase
			this.incubationTicks = 40 + level.random.nextInt(61); // 2 to 5 seconds incubation phase (40 to 100 ticks)
			this.elapsedTicks = 0;

			// Pre-calculate shell coordinates for a beautiful, solid organic dome (hemisphere)
			int r = this.radius;
			for (int dy = 0; dy <= r; dy++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
						if (dist <= r) {
							shellPositions.add(center.above(dy).offset(dx, 0, dz));
						}
					}
				}
			}

			// Sort from bottom to top so the dome grows upwards naturally
			shellPositions.sort(java.util.Comparator.comparingInt(pos -> pos.getY()));
		}

		public boolean tick() {
			if (level == null || level.isOutsideBuildHeight(center)) {
				return true; // remove
			}

			elapsedTicks++;

			// Incrementally build the cocoon shell made of Lignum Caro blocks during construction phase
			if (elapsedTicks <= totalGestationTicks) {
				int totalShell = shellPositions.size();
				if (totalShell > 0) {
					int targetToPlace = (int) Math.ceil(((double) elapsedTicks / (double) totalGestationTicks) * totalShell);
					targetToPlace = Math.min(targetToPlace, totalShell);
					BlockState state = TheBackwoodsModBlocks.LIGNUM_CARO.get().defaultBlockState();
					
					// Safely set infected property on cocoon blocks to false if it exists
					var infectedProp = state.getBlock().getStateDefinition().getProperty("infected");
					if (infectedProp instanceof net.minecraft.world.level.block.state.properties.BooleanProperty boolProp) {
						state = state.setValue(boolProp, false); // Formed of pristine Lignum Caro
					}

					for (int i = placedBlocks.size(); i < targetToPlace; i++) {
						BlockPos p = shellPositions.get(i);
						if (level.isEmptyBlock(p) || level.getBlockState(p).isAir()) {
							level.setBlock(p, state, 3);
							placedBlocks.add(p);

							// Anchor Root Support: prevent floating block on slopes and rough terrain
							BlockPos below = p.below();
							int maxDepth = 5;
							while (maxDepth > 0 && (level.isEmptyBlock(below) || level.getBlockState(below).isAir() || level.getBlockState(below).getDestroySpeed(level, below) == 0.0F)) {
								level.setBlock(below, state, 3);
								placedBlocks.add(below);
								below = below.below();
								maxDepth--;
							}
						}
					}
				}
			}

			// Play gestation thumping / heartbeat sounds and particles
			boolean isIncubating = elapsedTicks > totalGestationTicks;
			int thumpInterval = isIncubating ? 8 : 15; // Thumps much faster when fully formed and incubating!
			if (elapsedTicks % thumpInterval == 0) {
				level.playSound(
					null, center.above(radius),
					SoundEvents.BAMBOO_WOOD_FALL, SoundSource.BLOCKS,
					isIncubating ? 1.1F : 0.8F, (isIncubating ? 0.4F : 0.5F) + level.random.nextFloat() * 0.3F
				);
				level.playSound(
					null, center.above(radius),
					SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS,
					isIncubating ? 0.9F : 0.6F, (isIncubating ? 0.3F : 0.4F) + level.random.nextFloat() * 0.3F
				);
				level.sendParticles(
					ParticleTypes.HAPPY_VILLAGER,
					center.getX() + 0.5, center.getY() + radius, center.getZ() + 0.5,
					isIncubating ? 16 : 8, radius * 0.5, radius * 0.5, radius * 0.5, 0.02
				);
				if (isIncubating) {
					// Dripping mucus and organic debris during intense incubation
					level.sendParticles(
						ParticleTypes.SNEEZE,
						center.getX() + 0.5, center.getY() + radius, center.getZ() + 0.5,
						12, radius * 0.6, radius * 0.8, radius * 0.6, 0.05
					);
					level.sendParticles(
						ParticleTypes.COMPOSTER,
						center.getX() + 0.5, center.getY() + radius, center.getZ() + 0.5,
						8, radius * 0.5, radius * 0.8, radius * 0.5, 0.02
					);
				}
			}

			// Gestation complete: Burst/destroy cocoon and reveal Splinters inside!
			if (elapsedTicks >= totalGestationTicks + incubationTicks) {
				// Destroy all placed blocks (bursting cocoon)
				for (BlockPos p : placedBlocks) {
					if (level.getBlockState(p).is(TheBackwoodsModBlocks.LIGNUM_CARO.get())) {
						level.destroyBlock(p, false); // break without drops
					}
				}

				// Play epic rupture sounds
				level.playSound(
					null, center.above(radius),
					SoundEvents.WOOD_BREAK, SoundSource.HOSTILE,
					1.5F, 0.7F
				);
				level.playSound(
					null, center.above(radius),
					SoundEvents.CHERRY_WOOD_BREAK, SoundSource.HOSTILE,
					1.5F, 0.5F
				);
				level.playSound(
					null, center.above(radius),
					SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE,
					1.2F, 0.6F
				);

				// Particles burst
				level.sendParticles(
					ParticleTypes.EXPLOSION,
					center.getX() + 0.5, center.getY() + radius, center.getZ() + 0.5,
					8, 1.0, 1.0, 1.0, 0.1
				);
				level.sendParticles(
					ParticleTypes.HAPPY_VILLAGER,
					center.getX() + 0.5, center.getY() + radius, center.getZ() + 0.5,
					50, radius * 0.8, radius * 1.2, radius * 0.8, 0.15
				);
				level.sendParticles(
					ParticleTypes.COMPOSTER,
					center.getX() + 0.5, center.getY() + radius, center.getZ() + 0.5,
					40, radius * 0.8, radius * 1.2, radius * 0.8, 0.1
				);
				level.sendParticles(
					ParticleTypes.SNEEZE,
					center.getX() + 0.5, center.getY() + radius, center.getZ() + 0.5,
					30, radius * 0.8, radius * 1.2, radius * 0.8, 0.1
				);

				// Spawn Splinters!
				net.minecraft.world.entity.EntityType<?> splinterType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("the_backwoods:splinter"));
				if (splinterType != null) {
					for (int i = 0; i < numSplinters; i++) {
						double sx = center.getX() + 0.5 + (level.random.nextDouble() - 0.5) * (radius * 0.8);
						double sy = center.getY() + 1.0;
						double sz = center.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * (radius * 0.8);
						BlockPos spawnPos = BlockPos.containing(sx, sy, sz);
						Entity splinter = splinterType.spawn(level, spawnPos, MobSpawnType.MOB_SUMMONED);
						if (splinter instanceof LivingEntity livingSplinter) {
							// Find nearest spore to target
							AABB searchBox = new AABB(spawnPos).inflate(64.0);
							List<LivingEntity> nearbySpores = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> isSporeEntity(e));
							if (!nearbySpores.isEmpty()) {
								livingSplinter.setLastHurtByMob(nearbySpores.get(0));
								if (livingSplinter instanceof Mob mob) {
									mob.setTarget(nearbySpores.get(0));
								}
							}
						}
					}
					System.out.println("[ImmuneSystem] Cocoon ruptured at " + center + " spawning " + numSplinters + " splinters.");
				} else {
					System.out.println("[ImmuneSystem] ERROR: 'the_backwoods:splinter' entity type not found!");
				}

				return true; // remove cocoon
			}

			return false; // keep ticking
		}
	}

	private static void tickCocoons(ServerLevel level, long gameTime) {
		if (lastTickedGameTime == gameTime) return;
		lastTickedGameTime = gameTime;

		ACTIVE_COCOONS.removeIf(cocoon -> cocoon.tick());
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player == null) return;

		Level level = player.level();
		if (level.isClientSide()) return;
		if (!level.dimension().equals(BACKWOODS_DIM)) return;
		if (!(level instanceof ServerLevel serverLevel)) return;

		long gameTime = serverLevel.getGameTime();

		// Tick active cocoons every game tick
		tickCocoons(serverLevel, gameTime);

		if (gameTime % 10 != 0) return;

		// Decay spore threat slowly over time
		decaySporeThreat(player, gameTime);

		// 1. Scan for nearby Spore entities to increase threat over time
		if (gameTime % 20 == 0) {
			scanAndAddEntitySporeThreat(player, serverLevel);
		}

		// Count small spore entities (< 150hp) nearby to be used for early warnings and summoning
		AABB searchBox = player.getBoundingBox().inflate(128.0);
		int smallSporeCount = 0;
		List<LivingEntity> nearbyLiving = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox);
		for (LivingEntity e : nearbyLiving) {
			if (isSporeEntity(e) && !isStrongSporeEntity(e)) {
				smallSporeCount++;
			}
		}

		// 2. Splinter Cocoon Summoning (Tier 2)
		maybeSummonSplinterCocoon(player, serverLevel, gameTime, smallSporeCount);

		// 3. Spore Entity Detection & Rot Summoning (either from threat/strong spore or many small spores)
		maybeSummonRotForSpore(player, serverLevel, gameTime, smallSporeCount);
	}

	@SubscribeEvent
	public static void onLivingDamage(LivingDamageEvent.Post event) {
		LivingEntity victim = event.getEntity();
		if (victim == null) return;
		Level level = victim.level();
		if (level.isClientSide() || level.dimension() != BACKWOODS_DIM) return;

		Entity attacker = event.getSource().getEntity();
		boolean involveSpore = isSporeEntity(victim) || isSporeEntity(attacker);
		if (!involveSpore) return;

		// Add threat to all nearby players to trigger dimension's defenses
		level.getEntitiesOfClass(Player.class, victim.getBoundingBox().inflate(128.0)).forEach(player -> {
			addSporeThreat(player, 2);
		});
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity dead = event.getEntity();
		if (dead == null) return;
		Level level = dead.level();
		if (level.isClientSide() || level.dimension() != BACKWOODS_DIM) return;

		Entity killer = event.getSource().getEntity();
		boolean involveSpore = isSporeEntity(dead) || isSporeEntity(killer);
		if (!involveSpore) return;

		// Add higher threat to nearby players when spore creatures kill or die
		level.getEntitiesOfClass(Player.class, dead.getBoundingBox().inflate(128.0)).forEach(player -> {
			addSporeThreat(player, 5);
		});
	}

	private static void scanAndAddEntitySporeThreat(Player player, ServerLevel level) {
		AABB box = player.getBoundingBox().inflate(128.0);
		List<LivingEntity> nearbyLiving = level.getEntitiesOfClass(LivingEntity.class, box);
		int threatToAdd = 0;
		int strongCount = 0;
		int normalCount = 0;
		for (LivingEntity entity : nearbyLiving) {
			if (isStrongSporeEntity(entity)) {
				threatToAdd += 5;
				strongCount++;
			} else if (isSporeEntity(entity)) {
				threatToAdd += 1;
				normalCount++;
			}
		}
		if (threatToAdd > 0) {
			int oldThreat = getInt(player, K_SPORE_THREAT, 0);
			addSporeThreat(player, threatToAdd);
			int newThreat = getInt(player, K_SPORE_THREAT, 0);
			System.out.println("[ImmuneSystem] Scanned nearby entities: " + strongCount + " strong, " + normalCount + " normal spore entities. Threat increased from " + oldThreat + " to " + newThreat);
		}
	}

	private static BlockPos findSafeCocoonPosition(ServerLevel level, LivingEntity targetSpore, int radiusXZ, int radiusY) {
		BlockPos targetPos = targetSpore.blockPosition();
		BlockPos bestPos = null;
		double bestScore = -Double.MAX_VALUE;

		// Attempt up to 30 random checks within a 15-block horizontal radius
		for (int i = 0; i < 30; i++) {
			int offsetX = level.random.nextInt(21) - 10;
			int offsetZ = level.random.nextInt(21) - 10;
			// We want to keep some distance from the target spore (e.g., at least 4 blocks away horizontally)
			double distToSpore = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
			if (distToSpore < 4.0) continue;

			int spawnY = targetPos.getY();
			BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos(targetPos.getX() + offsetX, spawnY, targetPos.getZ() + offsetZ);
			
			// Search vertically to find a solid block floor with empty space above it
			boolean foundGround = false;
			for (int dy = -6; dy <= 6; dy++) {
				testPos.setY(targetPos.getY() + dy);
				if (level.getBlockState(testPos).isCollisionShapeFullBlock(level, testPos) && level.isEmptyBlock(testPos.above())) {
					spawnY = targetPos.getY() + dy + 1;
					foundGround = true;
					break;
				}
			}
			if (!foundGround) continue;

			BlockPos candidate = new BlockPos(targetPos.getX() + offsetX, spawnY, targetPos.getZ() + offsetZ);

			if (level.isOutsideBuildHeight(candidate)) continue;

			// Score the candidate based on spore presence (less is better) and vertical support
			int sporeBlockCount = 0;
			int checkRadius = radiusXZ + 2;
			for (int dx = -checkRadius; dx <= checkRadius; dx++) {
				for (int dy = -checkRadius; dy <= checkRadius; dy++) {
					for (int dz = -checkRadius; dz <= checkRadius; dz++) {
						BlockPos p = candidate.offset(dx, dy, dz);
						BlockState state = level.getBlockState(p);
						ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(state.getBlock());
						if (reg != null && reg.getNamespace().equals("spore")) {
							sporeBlockCount++;
						}
					}
				}
			}

			// Check nearby spore entities within checkRadius
			AABB checkArea = new AABB(candidate).inflate(checkRadius + 1.0);
			List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, checkArea, e -> isSporeEntity(e));
			int sporeEntityCount = nearbyEntities.size();

			// Penalty for empty air spaces underneath the cocoon base to ensure vertical support
			int airUnderneath = 0;
			for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
				for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
					if (level.isEmptyBlock(candidate.offset(dx, -1, dz))) {
						airUnderneath++;
					}
				}
			}

			// High score is better. Penalize spore presence heavily.
			double score = - (sporeBlockCount * 20.0) - (sporeEntityCount * 50.0) - (airUnderneath * 5.0);

			if (score > bestScore) {
				bestScore = score;
				bestPos = candidate;
			}

			// Perfect score
			if (score == 0.0) {
				return candidate;
			}
		}

		if (bestPos != null) {
			return bestPos;
		}

		// Fallback if no ideal spot found
		return targetPos.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
	}

	private static void maybeSummonSplinterCocoon(Player player, ServerLevel level, long gameTime, int smallSporeCount) {
		boolean isCreative = player.isCreative();
		long lastSummon = getLong(player, K_LAST_SPLINTER_SUMMON, 0L);
		long cooldown = isCreative ? 200L : (long) SPLINTER_COOLDOWN_TICKS;
		if (lastSummon != 0L && gameTime - lastSummon < cooldown) return;

		if (smallSporeCount >= MIN_SPLINTER_SPORE_THRESHOLD) {
			double chance;
			double minChance = 0.005; // 0.5% chance
			double maxChance = 0.75;  // 75% chance
			if (smallSporeCount >= MAX_SPLINTER_SPORE_THRESHOLD) {
				chance = maxChance;
			} else {
				double factor = (double) (smallSporeCount - MIN_SPLINTER_SPORE_THRESHOLD) / (double) (MAX_SPLINTER_SPORE_THRESHOLD - MIN_SPLINTER_SPORE_THRESHOLD);
				chance = minChance + factor * (maxChance - minChance);
			}

			if (isCreative || level.random.nextDouble() < chance) {
				// Pick a target from the small spore entities
				AABB searchBox = player.getBoundingBox().inflate(128.0);
				List<LivingEntity> smallSporeEntities = level.getEntitiesOfClass(
					LivingEntity.class,
					searchBox,
					entity -> isSporeEntity(entity) && !isStrongSporeEntity(entity)
				);

				if (!smallSporeEntities.isEmpty()) {
					LivingEntity targetSpore = null;
					double nearestDistSqr = Double.MAX_VALUE;
					for (LivingEntity e : smallSporeEntities) {
						double dSqr = e.distanceToSqr(player);
						if (dSqr < nearestDistSqr) {
							nearestDistSqr = dSqr;
							targetSpore = e;
						}
					}

					if (targetSpore != null) {
						// Cooldown triggered successfully
						putLong(player, K_LAST_SPLINTER_SUMMON, gameTime);

						// Determine cocoon size and splinter count based on spore quantity
						int numSplinters = Math.min(6, 2 + (smallSporeCount - MIN_SPLINTER_SPORE_THRESHOLD) / 3);
						int radiusXZ = numSplinters <= 3 ? 2 : 3;
						int radiusY = radiusXZ;

						// Find safe cocoon position away from spores
						BlockPos safeGroundPos = findSafeCocoonPosition(level, targetSpore, radiusXZ, radiusY);
						BlockPos cocoonCenter = safeGroundPos; // Bottom-center of the cocoon rests on the ground
						
						// Start gestating the cocoon!
						ACTIVE_COCOONS.add(new ActiveCocoon(level, cocoonCenter, numSplinters));

						if (SHOW_COCOON_MESSAGES) {
							player.displayClientMessage(Component.literal("An organic cocoon of Lignum Caro is forming...").withStyle(ChatFormatting.GOLD), true);
						}
						
						// Play an ominous startup sound
						level.playSound(
							null, cocoonCenter,
							SoundEvents.BAMBOO_WOOD_BREAK, SoundSource.BLOCKS,
							1.2f, 0.6f
						);
						level.playSound(
							null, cocoonCenter,
							SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS,
							1.0f, 0.5f
						);

						System.out.println("[ImmuneSystem] Formed Splinter Cocoon at " + cocoonCenter + " with " + numSplinters + " splinters in gestation.");
					}
				}
			}
		}
	}

	private static void maybeSummonRotForSpore(Player player, ServerLevel level, long gameTime, int smallSporeCount) {
		int threat = getInt(player, K_SPORE_THREAT, 0);
		boolean isCreative = player.isCreative();

		long lastSummon = getLong(player, K_LAST_SPORE_SUMMON, 0L);
		long cooldown = isCreative ? 600L : (long) SUMMON_COOLDOWN_TICKS; // 30s cooldown for creative mode testing
		if (lastSummon != 0L && gameTime - lastSummon < cooldown) return;

		AABB searchBox = player.getBoundingBox().inflate(128.0);
		LivingEntity targetSpore = null;
		boolean triggerSummon = false;

		// 1. Try to summon due to threat level + strong spore entity (HP >= 150)
		if (isCreative || threat >= 40) {
			List<LivingEntity> strongSporeEntities = level.getEntitiesOfClass(
				LivingEntity.class,
				searchBox,
				entity -> isStrongSporeEntity(entity)
			);
			if (!strongSporeEntities.isEmpty()) {
				double nearestDistSqr = Double.MAX_VALUE;
				for (LivingEntity e : strongSporeEntities) {
					double dSqr = e.distanceToSqr(player);
					if (dSqr < nearestDistSqr) {
						nearestDistSqr = dSqr;
						targetSpore = e;
					}
				}
				if (targetSpore != null) {
					triggerSummon = true;
				}
			}
		}

		// 2. Try to summon due to a large count of small spore entities (HP < 150)
		// Reduced chance 10x since Tier 2 Splinters now handle early/small spore infestations! (Rot)
		if (!triggerSummon && smallSporeCount >= MIN_SMALL_SPORE_THRESHOLD) {
			double chance;
			double minChance = 0.00001; // very low chance (0.001%) at minimum spore threshold
			double maxChance = 0.05;  // max 5% chance at max spore threshold
			if (smallSporeCount >= MAX_SMALL_SPORE_THRESHOLD) {
				chance = maxChance;
			} else {
				// Linear probability: increases from very low chance up to 5% at max threshold
				double factor = (double) (smallSporeCount - MIN_SMALL_SPORE_THRESHOLD) / (double) (MAX_SMALL_SPORE_THRESHOLD - MIN_SMALL_SPORE_THRESHOLD);
				chance = minChance + factor * (maxChance - minChance);
			}

			if (isCreative || level.random.nextDouble() < chance) {
				// Pick a target from the small spore entities
				List<LivingEntity> smallSporeEntities = level.getEntitiesOfClass(
					LivingEntity.class,
					searchBox,
					entity -> isSporeEntity(entity) && !isStrongSporeEntity(entity)
				);
				if (!smallSporeEntities.isEmpty()) {
					double nearestDistSqr = Double.MAX_VALUE;
					for (LivingEntity e : smallSporeEntities) {
						double dSqr = e.distanceToSqr(player);
						if (dSqr < nearestDistSqr) {
							nearestDistSqr = dSqr;
							targetSpore = e;
						}
					}
					if (targetSpore != null) {
						triggerSummon = true;
						System.out.println("[ImmuneSystem] Summon triggered by small spores (10x nerfed). Count: " + smallSporeCount + ", calculated chance: " + (chance * 100) + "%");
					}
				}
			}
		}

		if (!triggerSummon || targetSpore == null) return;

		// Play warning dialogue and alert strictly ONCE right when the rot is about to spawn
		if (!player.getPersistentData().getBoolean("bw_spore_warn_played")) {
			player.getPersistentData().putBoolean("bw_spore_warn_played", true);
			String line = SPORE_WARN_LINES[level.random.nextInt(SPORE_WARN_LINES.length)];
			player.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.RED), true);

			SoundEvent bell = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bell.resonate"));
			if (bell != null) {
				level.playSound(
					null,
					BlockPos.containing(player.getX(), player.getY(), player.getZ()),
					bell,
					SoundSource.HOSTILE,
					1.2f,
					0.75f
				);
			}
		}

		// Summon cooldown triggered successfully
		putLong(player, K_LAST_SPORE_SUMMON, gameTime);

		// Cool entrance: spawn high up in the sky directly above/near the target spore
		double rx = targetSpore.getX() + level.random.nextDouble() * 6.0 - 3.0;
		double rz = targetSpore.getZ() + level.random.nextDouble() * 6.0 - 3.0;
		double ry = targetSpore.getY() + 20.0;

		BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos(rx, ry, rz);
		double highestSafeY = targetSpore.getY();
		for (int dy = 1; dy <= 24; dy++) {
			testPos.set(rx, targetSpore.getY() + dy, rz);
			if (level.isEmptyBlock(testPos) && level.isEmptyBlock(testPos.above()) && level.isEmptyBlock(testPos.above(2))) {
				highestSafeY = targetSpore.getY() + dy;
			} else if (level.getBlockState(testPos).isCollisionShapeFullBlock(level, testPos)) {
				break;
			}
		}
		ry = highestSafeY;

		BlockPos spawnPos = BlockPos.containing(rx, ry, rz);
		if (level.isOutsideBuildHeight(spawnPos)) return;

		Entity spawned = TheBackwoodsModEntities.ROT.get().spawn(
			level,
			spawnPos,
			MobSpawnType.MOB_SUMMONED
		);

		if (spawned instanceof RotEntity rot) {
			System.out.println("[ImmuneSystem] Summoned Rot at " + spawnPos + " to defend against Spore entity: " + targetSpore.getType().toString());
			rot.setDeltaMovement(0, 0, 0);
			if (targetSpore != null) {
				rot.setTarget(targetSpore);
			}

			int summons = player.getPersistentData().contains("bw_spore_rots_summoned") ? player.getPersistentData().getInt("bw_spore_rots_summoned") : 0;
			summons++;
			player.getPersistentData().putInt("bw_spore_rots_summoned", summons);

			rot.getPersistentData().putBoolean("sentinel_should_scan", true);
			rot.getPersistentData().putInt("sentinel_spore_summons_at_birth", summons);

			// Trigger built-in cool entrance: crush slam phase 2 (mid-air hover charge)
			rot.getPersistentData().putDouble("sentinel_slam_phase", 2);
			rot.getPersistentData().putDouble("sentinel_slam_ticks", 30); // Hover and charge for 1.5 seconds
			rot.getPersistentData().putBoolean("sentinel_immune_slam_landing_active", true);

			// Beautiful summon effects & audio
			level.playSound(
				null,
				spawnPos,
				SoundEvents.WITHER_SPAWN,
				SoundSource.HOSTILE,
				1.0f,
				0.8f
			);
			level.sendParticles(
				ParticleTypes.CAMPFIRE_COSY_SMOKE,
				rot.getX(),
				rot.getY() + 1.0,
				rot.getZ(),
				100,
				0.5,
				1.0,
				0.5,
				0.03
			);
		}
	}

	private static boolean isSporeEntity(Entity entity) {
		if (entity == null) return false;
		ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		return rl != null && rl.getNamespace().equals("spore");
	}

	private static boolean isStrongSporeEntity(Entity entity) {
		if (!(entity instanceof LivingEntity living)) return false;

		ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		if (rl == null || !rl.getNamespace().equals("spore")) return false;

		double maxHp = living.getMaxHealth();
		return maxHp >= 150.0;
	}

	private static void decaySporeThreat(Player player, long gameTime) {
		long last = getLong(player, K_LAST_SPORE_DECAY, 0L);
		if (gameTime - last < 20) return;
		putLong(player, K_LAST_SPORE_DECAY, gameTime);

		int current = getInt(player, K_SPORE_THREAT, 0);
		int next = Math.max(0, current - 1);
		putInt(player, K_SPORE_THREAT, next);

		if (next == 0) {
			player.getPersistentData().putBoolean("bw_spore_warn_played", false);
		}
	}

	private static int getInt(Player p, String key, int def) {
		return p.getPersistentData().contains(key) ? p.getPersistentData().getInt(key) : def;
	}

	private static long getLong(Player p, String key, long def) {
		return p.getPersistentData().contains(key) ? p.getPersistentData().getLong(key) : def;
	}

	private static void putInt(Player p, String key, int value) {
		p.getPersistentData().putInt(key, value);
	}

	private static void putLong(Player p, String key, long value) {
		p.getPersistentData().putLong(key, value);
	}

	private static void addSporeThreat(Player player, int amount) {
		int current = getInt(player, K_SPORE_THREAT, 0);
		putInt(player, K_SPORE_THREAT, Mth.clamp(current + amount, 0, 100));
	} // 1.21.1
}
