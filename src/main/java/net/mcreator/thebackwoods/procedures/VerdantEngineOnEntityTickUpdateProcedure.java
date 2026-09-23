package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.nbt.CompoundTag;

import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class VerdantEngineOnEntityTickUpdateProcedure {
	public static double HOVER_TARGET_ALTITUDE = 72.0, BEAM_RADIUS = 3.0, BEAM_START_Y_OFFSET = 0.0, BASE_GRAVITY_RADIUS = 32.0, BASE_POUND_RADIUS = 32.0, POUND_RADIUS_INCREMENT = 16.0, MAX_POUND_RADIUS_CAP = 224.0, SHOCKWAVE_BASE_SCALE = 8.0;
	public static int CHARGE_DURATION_TICKS = 110, CYCLE_TOTAL_TICKS = 240, RINGS_START_TICK = 160;
	public static boolean ENABLE_FLUID_EVAPORATION = false;
	public static boolean VERDANT_DEATH_EXPLOSION_GRIEFING = true;

	// --- Configurable Planetary Pounding & Gravity Beam Damage ---
	public static float PLANETARY_POUND_BASE_DAMAGE = 65.0F;
	public static float PLANETARY_POUND_PER_POUND_DAMAGE = 22.0F;
	public static float SHOCKWAVE_BASE_DAMAGE = 65.0F;
	public static float SHOCKWAVE_MIN_DAMAGE = 25.0F;
	public static float GRAVITY_SLAM_IMPACT_DAMAGE = 10.0F;
	public static float CORE_ASPHYXIATION_DAMAGE = 2.0F;
	public static double ZERO_G_LIFT_VERTICAL_MIN = -12.0;
	public static double ZERO_G_LIFT_VERTICAL_MAX = 36.0;
	public static double ZERO_G_LIFT_MAX_Y_CAP = 16.0;

	public static double MAX_INFECTION_RADIUS = 256.0;
	public static double GRAVITY_BEAM_PARTICLE_QUALITY = 0.65;
	public static double GRAVITY_BEAM_PARTICLE_RENDER_DIST = 1024.0;
	public static double GRAVITY_BEAM_PARTICLE_RENDER_DIST_SQ = 1024.0 * 1024.0;
	public static double INFECTION_SPEED_MULTIPLIER = 0.25;
	public static boolean GRAVITY_BEAM_PENETRATION_ENABLED = false;
	public static double GRAVITY_BEAM_PENETRATION_SPEED = 0.125;
	public static final double GRAVITY_BEAM_EFFECT_RADIUS = 12.0; // Strictly fixed radius around beam column; does not expand
	public static double GRAVITY_BEAM_MAX_GRAVITY_MODIFIER = 3.5;
	public static double SHOCKWAVE_PARTICLE_RADIUS = 48.0;
	public static double SHOCKWAVE_PARTICLE_QUALITY = 1.0;
	public static double INFECTION_SATURATION_THRESHOLD = 0.90;
	public static boolean INFECT_INDESTRUCTIBLE_BLOCKS = false;

	// --- Configurable Indestructible / Bedrock Shattering Pounding Thresholds ---
	public static boolean BEDROCK_TESTING_MODE = true; // Master Testing Toggle: Enabled for rapid testing

	// Default Standard Gameplay Bedrock Pounding Thresholds (Halved)
	public static int BEDROCK_CRACK_BASE_POUNDS_DEFAULT = 3;
	public static int BEDROCK_SHATTER_BASE_POUNDS_DEFAULT = 5;
	public static int BEDROCK_CRACK_MID_POUNDS_DEFAULT = 5;
	public static int BEDROCK_SHATTER_MID_POUNDS_DEFAULT = 7;
	public static int BEDROCK_CRACK_OUTER_POUNDS_DEFAULT = 7;
	public static int BEDROCK_SHATTER_OUTER_POUNDS_DEFAULT = 9;

	// Testing Bedrock Pounding Thresholds (Rapid verification - Halved)
	public static int BEDROCK_CRACK_BASE_POUNDS_TESTING = 1;
	public static int BEDROCK_SHATTER_BASE_POUNDS_TESTING = 2;
	public static int BEDROCK_CRACK_MID_POUNDS_TESTING = 2;
	public static int BEDROCK_SHATTER_MID_POUNDS_TESTING = 3;
	public static int BEDROCK_CRACK_OUTER_POUNDS_TESTING = 3;
	public static int BEDROCK_SHATTER_OUTER_POUNDS_TESTING = 4;

	public static boolean isBedrockTestingEnabled(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_bedrock_testing_mode")) {
			return nbt.getBoolean("verdant_bedrock_testing_mode");
		}
		return BEDROCK_TESTING_MODE;
	}

	public static int getBedrockCrackPounds(CompoundTag nbt, double distFromCenter) {
		boolean testing = isBedrockTestingEnabled(nbt);
		int base = testing ? BEDROCK_CRACK_BASE_POUNDS_TESTING : BEDROCK_CRACK_BASE_POUNDS_DEFAULT;
		int mid = testing ? BEDROCK_CRACK_MID_POUNDS_TESTING : BEDROCK_CRACK_MID_POUNDS_DEFAULT;
		int outer = testing ? BEDROCK_CRACK_OUTER_POUNDS_TESTING : BEDROCK_CRACK_OUTER_POUNDS_DEFAULT;

		if (nbt != null && nbt.contains("verdant_bedrock_crack_pounds")) {
			base = nbt.getInt("verdant_bedrock_crack_pounds");
		}
		if (distFromCenter <= 1.5) return base;
		if (distFromCenter <= 3.5) return Math.max(base + 1, mid);
		return Math.max(base + 2, outer);
	}

	public static int getBedrockShatterPounds(CompoundTag nbt, double distFromCenter) {
		boolean testing = isBedrockTestingEnabled(nbt);
		int base = testing ? BEDROCK_SHATTER_BASE_POUNDS_TESTING : BEDROCK_SHATTER_BASE_POUNDS_DEFAULT;
		int mid = testing ? BEDROCK_SHATTER_MID_POUNDS_TESTING : BEDROCK_SHATTER_MID_POUNDS_DEFAULT;
		int outer = testing ? BEDROCK_SHATTER_OUTER_POUNDS_TESTING : BEDROCK_SHATTER_OUTER_POUNDS_DEFAULT;

		if (nbt != null && nbt.contains("verdant_bedrock_shatter_pounds")) {
			base = nbt.getInt("verdant_bedrock_shatter_pounds");
		}
		if (distFromCenter <= 1.5) return base;
		if (distFromCenter <= 3.5) return Math.max(base + 1, mid);
		return Math.max(base + 2, outer);
	}

	public static boolean isMaterialAdapted(CompoundTag nbt, Block block) {
		if (nbt == null || block == null) return false;
		String id = BuiltInRegistries.BLOCK.getKey(block).toString();
		String adapted = nbt.contains("verdant_adapted_blocks") ? nbt.getString("verdant_adapted_blocks") : "";
		return adapted.contains(id);
	}

	public static void recordMaterialEncounter(CompoundTag nbt, Block block) {
		if (nbt == null || block == null) return;
		String id = BuiltInRegistries.BLOCK.getKey(block).toString();
		String adapted = nbt.contains("verdant_adapted_blocks") ? nbt.getString("verdant_adapted_blocks") : "";
		if (!adapted.contains(id)) {
			if (!adapted.isEmpty()) adapted += ",";
			adapted += id;
			nbt.putString("verdant_adapted_blocks", adapted);
		}
	}

	public static int getBedrockCrackPounds(CompoundTag nbt, double distFromCenter, Block block) {
		if (isMaterialAdapted(nbt, block)) return 1;
		return getBedrockCrackPounds(nbt, distFromCenter);
	}

	public static int getBedrockShatterPounds(CompoundTag nbt, double distFromCenter, Block block) {
		if (isMaterialAdapted(nbt, block)) return 1;
		return getBedrockShatterPounds(nbt, distFromCenter);
	}

	public static boolean shouldShatterBedrock(CompoundTag nbt, double distFromCenter, int poundCount, Block block) {
		if (isMaterialAdapted(nbt, block)) return poundCount >= 1;
		return shouldShatterBedrock(nbt, distFromCenter, poundCount);
	}

	public static boolean shouldCrackBedrock(CompoundTag nbt, double distFromCenter, int poundCount, Block block) {
		if (isMaterialAdapted(nbt, block)) return poundCount >= 1;
		return shouldCrackBedrock(nbt, distFromCenter, poundCount);
	}

	public static boolean shouldShatterBedrock(CompoundTag nbt, double distFromCenter, int poundCount) {
		return poundCount >= getBedrockShatterPounds(nbt, distFromCenter);
	}

	public static boolean shouldCrackBedrock(CompoundTag nbt, double distFromCenter, int poundCount) {
		return poundCount >= getBedrockCrackPounds(nbt, distFromCenter);
	}

	public static boolean canInfectIndestructible(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_infect_indestructible_blocks")) {
			return nbt.getBoolean("verdant_infect_indestructible_blocks");
		}
		return INFECT_INDESTRUCTIBLE_BLOCKS;
	}

	public static boolean isCeiledDimension(Level level) {
		if (level == null) return false;
		return level.dimensionType().hasCeiling() || level.dimension() == Level.NETHER;
	}

	public static boolean isIndestructibleBlock(BlockState state, ServerLevel level, BlockPos pos) {
		if (state == null) return false;
		Block b = state.getBlock();
		if (b == Blocks.BEDROCK || b == Blocks.BARRIER || b == Blocks.REINFORCED_DEEPSLATE
			|| b == Blocks.END_PORTAL || b == Blocks.END_PORTAL_FRAME
			|| b == Blocks.COMMAND_BLOCK || b == Blocks.CHAIN_COMMAND_BLOCK || b == Blocks.REPEATING_COMMAND_BLOCK
			|| b == Blocks.STRUCTURE_BLOCK || b == Blocks.JIGSAW) {
			return true;
		}
		try {
			return state.getDestroySpeed(level, pos) < 0.0F;
		} catch (Exception ignored) {
			return false;
		}
	}

	private static final ResourceLocation VERDANT_BEAM_GRAV_MOD_ID = ResourceLocation.parse("the_backwoods:verdant_beam_gravity");
	private static final Map<UUID, Long> GRAVITY_MODIFIED_ENTITIES = new ConcurrentHashMap<>();

	public static double getGravityBeamEffectRadius(CompoundTag nbt) {
		return GRAVITY_BEAM_EFFECT_RADIUS;
	}

	public static double getGravityBeamMaxModifier(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_gravity_max_modifier")) {
			return Math.max(0.1, Math.min(20.0, nbt.getDouble("verdant_gravity_max_modifier")));
		}
		return GRAVITY_BEAM_MAX_GRAVITY_MODIFIER;
	}

	public static boolean isGravityBeamPenetrationEnabled(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_penetration_enabled")) {
			return persistentBoolean(nbt, "verdant_penetration_enabled", GRAVITY_BEAM_PENETRATION_ENABLED);
		}
		return GRAVITY_BEAM_PENETRATION_ENABLED;
	}

	public static double getGravityBeamPenetrationSpeed(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_penetration_speed")) {
			return Math.max(0.01, Math.min(5.0, nbt.getDouble("verdant_penetration_speed")));
		}
		return GRAVITY_BEAM_PENETRATION_SPEED;
	}

	public static double getShockwaveParticleRadius(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_shockwave_particle_radius")) {
			return Math.max(16.0, Math.min(256.0, nbt.getDouble("verdant_shockwave_particle_radius")));
		}
		return SHOCKWAVE_PARTICLE_RADIUS;
	}

	public static double getShockwaveParticleQuality(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_shockwave_particle_quality")) {
			return Math.max(0.0, Math.min(3.0, nbt.getDouble("verdant_shockwave_particle_quality")));
		}
		return Math.max(0.0, Math.min(3.0, SHOCKWAVE_PARTICLE_QUALITY));
	}

	public static double getInfectionSaturationThreshold(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_infection_saturation_threshold")) {
			return Math.max(0.10, Math.min(1.00, nbt.getDouble("verdant_infection_saturation_threshold")));
		}
		return INFECTION_SATURATION_THRESHOLD;
	}

	public static double getInfectionSpeedMultiplier(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_infection_speed_multiplier")) {
			return Math.max(0.01, Math.min(5.0, nbt.getDouble("verdant_infection_speed_multiplier")));
		}
		return Math.max(0.01, Math.min(5.0, INFECTION_SPEED_MULTIPLIER));
	}

	public static double getBeamParticleQuality(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_beam_particle_quality")) {
			return Math.max(0.0, Math.min(2.0, nbt.getDouble("verdant_beam_particle_quality")));
		}
		return Math.max(0.0, Math.min(2.0, GRAVITY_BEAM_PARTICLE_QUALITY));
	}

	public static double getBeamParticleQuality(ServerLevel level, double x, double z, CompoundTag nbt) {
		double baseQuality = getBeamParticleQuality(nbt);
		if (baseQuality <= 0.001 || level == null) return 0.0;

		double distSq = getNearestPlayerDistanceSq(level, x, z);
		if (distSq > GRAVITY_BEAM_PARTICLE_RENDER_DIST_SQ) return 0.0;

		double tierFactor;
		if (distSq <= 4096.0) { // 0-64 blocks (64*64 = 4096)
			tierFactor = 0.65;
		} else if (distSq <= 20736.0) { // 65-144 blocks (144*144 = 20736)
			tierFactor = 0.40;
		} else { // 144+ blocks
			tierFactor = 0.20;
		}

		return tierFactor * (baseQuality / 0.65);
	}

	public static double getBeamParticleRenderDistSq(CompoundTag nbt) {
		if (nbt != null && nbt.contains("verdant_beam_particle_render_dist")) {
			double dist = Math.max(16.0, Math.min(2048.0, persistentDouble(nbt, "verdant_beam_particle_render_dist", GRAVITY_BEAM_PARTICLE_RENDER_DIST)));
			return dist * dist;
		}
		return GRAVITY_BEAM_PARTICLE_RENDER_DIST_SQ;
	}

	private static final DustParticleOptions BEAM_WHITE_DUST = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 2.2f);
	private static final DustParticleOptions BEAM_CYAN_DUST = new DustParticleOptions(new Vector3f(0.85f, 0.95f, 1.0f), 1.8f);
	private static final DustParticleOptions RING_ENERGY_DUST = new DustParticleOptions(new Vector3f(0.9f, 1.0f, 0.95f), 2.2f);
	private static final Map<String, Field> SYNCED_DATA_FIELD_CACHE = new ConcurrentHashMap<>();

	private static final TagKey<EntityType<?>> WOODBOUND_TAG_1 = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities"));
	private static final TagKey<EntityType<?>> WOODBOUND_TAG_2 = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("thebackwoods:woodbound_entities"));

	private static boolean isWoodbound(Entity entity) {
		if (entity == null) return false;
		String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
		if (typeKey.contains("fractus")) return true;
		return entity.getType().is(WOODBOUND_TAG_1) || entity.getType().is(WOODBOUND_TAG_2) || entity.getTags().contains("woodbound_entities");
	}

	public static void execute() {}
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) { execute(null, world, x, y, z, entity); }

	private static void execute(Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !(world instanceof ServerLevel level) || !isVerdantEngine(entity)) return;

		CompoundTag nbt = entity.getPersistentData();
		long gameTime = level.getGameTime();
		if (nbt.getLong("verdant_last_game_tick") == gameTime) return;
		nbt.putLong("verdant_last_game_tick", gameTime);

		cleanseNegativeEffects(entity);

		if (!nbt.getBoolean("verdant_initialized")) {
			setupInitialSpawnData(level, entity, nbt);
		}

		int state = nbt.getInt("verdant_state");
		double tx = nbt.getDouble("verdant_locked_x"), ty = nbt.getDouble("verdant_locked_y"), tz = nbt.getDouble("verdant_locked_z");
		float yaw = nbt.getFloat("verdant_locked_yaw"), pitch = nbt.getFloat("verdant_locked_pitch");

		if (tx == 0.0 && ty == 0.0 && tz == 0.0) {
			tx = entity.getX();
			ty = entity.getY();
			tz = entity.getZ();
		}

		// Register in persistent tracker to maintain authoritative ticking loader around Verdant
		if (gameTime % 40 == 0) {
			ensureLongDistanceTracking(level, entity);
		}

		boolean isDying = persistentBoolean(nbt, "verdant_death_sequence_active", false);
		if (!isDying && entity instanceof LivingEntity living && (living.getHealth() <= 20.0F || living.isDeadOrDying())) {
			if (isTerraformingActive(entity)) {
				startDeathSequence(level, entity, nbt);
				isDying = true;
			} else {
				setSyncedIsDeath(entity, false);
			}
		}

		if (isDying) {
			handleDeathSequence(level, entity, nbt, tx, ty, tz, yaw, pitch);
			return;
		}

		// 12-Chunk (192 Block) Territorial Spreading & Willing Relocation AI
		if (state >= 1) {
			handleVerdantMusic(level, entity, nbt, tx, ty, tz);

			int stopTerraDelay = persistentInt(nbt, "verdant_stop_terra_delay", 0);
			if (stopTerraDelay > 0) {
				stopTerraDelay--;
				nbt.putInt("verdant_stop_terra_delay", stopTerraDelay);
				if (stopTerraDelay == 0) {
					setSyncedStopTerra(entity, true);
					nbt.putInt("verdant_clear_stop_terra_ticks", 70); // auto-clear stop state in 3.5s
				}
			}

			int clearStopTerra = persistentInt(nbt, "verdant_clear_stop_terra_ticks", 0);
			if (clearStopTerra > 0) {
				clearStopTerra--;
				nbt.putInt("verdant_clear_stop_terra_ticks", clearStopTerra);
				if (clearStopTerra == 0) {
					setSyncedStopTerra(entity, false);
					setSyncedIsDeath(entity, false);
				}
			}

			int startTerraDelay = persistentInt(nbt, "verdant_start_terra_delay", 0);
			if (startTerraDelay > 0) {
				startTerraDelay--;
				nbt.putInt("verdant_start_terra_delay", startTerraDelay);
				if (startTerraDelay == 0) {
					setSyncedIsTerraforming(entity, false);
					setSyncedCurrentlyTerraforming(entity, true);
					setSyncedStopTerra(entity, false);
					setSyncedIsDeath(entity, false);
				}
			}

			int relocateCooldown = persistentInt(nbt, "verdant_relocate_cooldown", 0);
			if (relocateCooldown > 0) {
				nbt.putInt("verdant_relocate_cooldown", relocateCooldown - 1);
			}

			boolean isRelocating = persistentBoolean(nbt, "verdant_is_relocating", false);

			if (!isRelocating && relocateCooldown <= 0 && gameTime % 40 == 0) {
				double searchR = 384.0;
				AABB searchBox = new AABB(tx - searchR, ty - 128.0, tz - searchR, tx + searchR, ty + 128.0, tz + searchR);
				List<Entity> neighbors = level.getEntities(entity, searchBox, e -> isVerdantEngine(e) && e.isAlive() && e != entity);
				
				boolean shouldRelocate = false;
				double preferredDx = 0.0, preferredDz = 0.0;

				if (!neighbors.isEmpty()) {
					double avgDx = 0.0, avgDz = 0.0;
					int count = 0;
					for (Entity other : neighbors) {
						double distSq = entity.distanceToSqr(other);
						if (distSq < 224.0 * 224.0 && distSq > 0.01) {
							// Entity ID tie-breaker: higher ID entity willingly relocates to avoid double-movement
							if (entity.getId() < other.getId()) continue;
							avgDx += (entity.getX() - other.getX());
							avgDz += (entity.getZ() - other.getZ());
							count++;
						}
					}
					if (count > 0) {
						double len = Math.sqrt(avgDx * avgDx + avgDz * avgDz);
						if (len < 0.001) { avgDx = 1.0; avgDz = 0.0; len = 1.0; }
						preferredDx = avgDx / len;
						preferredDz = avgDz / len;
						shouldRelocate = true;
					}
				}

				// Multi-Ring Biome Saturation & Territory Exhaustion Check (Evaluated every 160 ticks / 8 seconds)
				if (!shouldRelocate && gameTime % 160 == 0) {
					int totalSamples = 33;
					int infectedCount = 0;

					// Center core column
					if (isColumnDeeplyInfected(level, Mth.floor(tx), Mth.floor(tz), ty)) {
						infectedCount++;
					}

					// Ring 1: 48 blocks (4 samples)
					for (int i = 0; i < 4; i++) {
						double angle = (Math.PI * 2.0 / 4.0) * i;
						int sx = Mth.floor(tx + fastCos(angle) * 48.0);
						int sz = Mth.floor(tz + fastSin(angle) * 48.0);
						if (isColumnDeeplyInfected(level, sx, sz, ty)) infectedCount++;
					}

					// Ring 2: 112 blocks (8 samples)
					for (int i = 0; i < 8; i++) {
						double angle = (Math.PI * 2.0 / 8.0) * i + 0.2;
						int sx = Mth.floor(tx + fastCos(angle) * 112.0);
						int sz = Mth.floor(tz + fastSin(angle) * 112.0);
						if (isColumnDeeplyInfected(level, sx, sz, ty)) infectedCount++;
					}

					// Ring 3: 176 blocks (12 samples)
					for (int i = 0; i < 12; i++) {
						double angle = (Math.PI * 2.0 / 12.0) * i;
						int sx = Mth.floor(tx + fastCos(angle) * 176.0);
						int sz = Mth.floor(tz + fastSin(angle) * 176.0);
						if (isColumnDeeplyInfected(level, sx, sz, ty)) infectedCount++;
					}

					// Ring 4: 240 blocks (8 samples)
					for (int i = 0; i < 8; i++) {
						double angle = (Math.PI * 2.0 / 8.0) * i + 0.35;
						int sx = Mth.floor(tx + fastCos(angle) * 240.0);
						int sz = Mth.floor(tz + fastSin(angle) * 240.0);
						if (isColumnDeeplyInfected(level, sx, sz, ty)) infectedCount++;
					}

					double saturationRatio = (double) infectedCount / (double) totalSamples;
					int currentPounds = persistentInt(nbt, "verdant_pound_count", 0);
					double currentInfR = persistentDouble(nbt, "verdant_infection_radius", 0.0);
					double satThreshold = getInfectionSaturationThreshold(nbt);

					// Trigger relocation ONLY when territory is deeply infected past saturation threshold (surface + caves + subterranean strata)
					if (saturationRatio >= satThreshold || (saturationRatio >= (satThreshold * 0.90) && currentPounds >= 20 && currentInfR >= 224.0)) {
						shouldRelocate = true;
					}
				}

				if (shouldRelocate) {
					double[] relocationTarget = findBestRelocationTarget(level, entity, nbt, tx, ty, tz, preferredDx, preferredDz);
					if (relocationTarget != null) {
						double bestTargetX = relocationTarget[0];
						double bestTargetY = relocationTarget[1];
						double bestTargetZ = relocationTarget[2];

						nbt.putDouble("verdant_target_x", bestTargetX);
						nbt.putDouble("verdant_target_y", bestTargetY);
						nbt.putDouble("verdant_target_z", bestTargetZ);
						nbt.putBoolean("verdant_is_relocating", true);
						isRelocating = true;
						resetInfectionAndProgress(nbt);
						nbt.putInt("verdant_state", 1);
						nbt.putInt("verdant_charge_ticks", 0);
						state = 1;

						boolean wasActive = persistentBoolean(nbt, "verdant_synced_active", false);
						if (wasActive) {
							double moveDist = Math.hypot(bestTargetX - tx, bestTargetZ - tz);
							boolean isFarRelocation = moveDist >= 96.0;
							stopTerraformingFlow(entity, nbt, isFarRelocation);
							nbt.putBoolean("verdant_synced_active", false);
							playEngineSound(level, tx, ty, tz, "block.beacon.deactivate", 48.0F, 0.85F);
						} else {
							stopTerraformingFlow(entity, nbt, false);
							setSyncedStopTerra(entity, false);
						}
						releaseAllCaughtEntities(level, entity.position(), 72.0);
					}
				}
			}

			if (isRelocating && nbt.contains("verdant_target_x") && nbt.contains("verdant_target_z")) {
				double targetX = persistentDouble(nbt, "verdant_target_x", tx);
				double targetZ = persistentDouble(nbt, "verdant_target_z", tz);
				double targetY = persistentDouble(nbt, "verdant_target_y", ty);
				double dx = targetX - tx;
				double dz = targetZ - tz;
				double dist = Math.sqrt(dx * dx + dz * dz);

				if (dist > 0.8) {
					double moveSpeed = Math.min(0.275, Math.max(0.075, dist * 0.025));
					tx += (dx / dist) * Math.min(dist, moveSpeed);
					tz += (dz / dist) * Math.min(dist, moveSpeed);

					// Dynamic terrain elevation tracking: sample ground directly beneath current flight position
					double localGroundY = findGroundBelow(level, tx, tz, level.getMaxBuildHeight() - 10.0);
					double desiredY = calculateOptimalVerdantHoverY(level, tx, tz, localGroundY);

					if (Math.abs(desiredY - ty) > 0.05) {
						double climbStep = Math.min(0.175, Math.max(0.05, Math.abs(desiredY - ty) * 0.02));
						if (desiredY > ty) {
							ty += climbStep;
						} else {
							ty -= climbStep;
						}
					}
					// Strictly prevent clipping beneath ground surface
					if (ty < localGroundY + 1.5) {
						ty = localGroundY + 1.5;
					}

					nbt.putDouble("verdant_locked_x", tx);
					nbt.putDouble("verdant_locked_y", ty);
					nbt.putDouble("verdant_locked_z", tz);

					entity.setDeltaMovement(0, 0, 0);

					// Keep gravity beam paused and skip terraforming cycles while in transit
					entity.setPos(tx, ty + Math.sin(gameTime * 0.04) * 0.35, tz);

					// Keep chunk loading tickets updated as Verdant travels across chunk borders
					if (gameTime % 20 == 0) {
						try {
							net.mcreator.thebackwoods.EntityTrackerData.updatePosition(entity, net.mcreator.thebackwoods.EntityTrackerData.LOADER_TICKET_RADIUS);
						} catch (Throwable ignored) {}
					}
					return;
				} else {
					// Settled at new territory location -> Reactivate gravity beam & terraforming
					nbt.putBoolean("verdant_is_relocating", false);
					nbt.putInt("verdant_relocate_cooldown", 1200); // 60s settling cooldown lock
					double groundY = findHighestGroundInRadius(level, tx, tz, level.getMaxBuildHeight() - 10.0, 5);
					nbt.putDouble("verdant_ground_y", groundY);

					// Register new location in persistent tracker immediately upon arrival
					try {
						net.mcreator.thebackwoods.EntityTrackerData.track(entity, net.mcreator.thebackwoods.EntityTrackerData.LOADER_TICKET_RADIUS, "verdant_engine");
					} catch (Throwable ignored) {}

					// Completely reset infection range, burst targets, pounds, and terraforming progress to start ALL over
					resetInfectionAndProgress(nbt);

					// Reset state to 1 (charging sequence) so it starts fresh with its corona buildup before beam activation
					nbt.putInt("verdant_state", 1);
					nbt.putInt("verdant_charge_ticks", 0);
					state = 1;

					startTerraformingFlow(entity, nbt);
					nbt.putBoolean("verdant_synced_active", true);
					playEngineSound(level, tx, ty, tz, "block.beacon.activate", 48.0F, 1.0F);

					entity.setPos(tx, ty + Math.sin(gameTime * 0.04) * 0.35, tz);
					return;
				}
			}
		}

		Vec3 core = new Vec3(tx, entity.getY() + entity.getBbHeight() * 0.5 + BEAM_START_Y_OFFSET, tz);
		handleBoilingCoreHeatAndWaterBubbles(level, entity, core);

		if (state == 0) {
			handleAscensionPhase(level, entity, nbt, tx, ty, tz, yaw, pitch);
			return;
		}

		double hoverY = ty + Math.sin(gameTime * 0.04) * 0.35;
		entity.setNoGravity(true);
		entity.fallDistance = 0.0F;
		entity.setYRot(yaw);
		entity.setXRot(pitch);
		entity.setYHeadRot(yaw);
		entity.setYBodyRot(yaw);
		entity.setDeltaMovement(0, 0, 0);
		entity.setPos(tx, hoverY, tz);

		core = new Vec3(tx, entity.getY() + entity.getBbHeight() * 0.5 + BEAM_START_Y_OFFSET, tz);
		double groundY = nbt.getDouble("verdant_ground_y");
		if (groundY <= level.getMinBuildHeight() || gameTime % 20 == 0) {
			groundY = findGroundBelow(level, core.x, core.z, core.y);
			nbt.putDouble("verdant_ground_y", groundY);
		}

		if (state == 1) {
			int charge = nbt.getInt("verdant_charge_ticks") + 1;
			nbt.putInt("verdant_charge_ticks", charge);
			if (!nbt.getBoolean("verdant_synced_active")) {
				startTerraformingFlow(entity, nbt);
				nbt.putBoolean("verdant_synced_active", true);
			}

			double prog = (double) charge / CHARGE_DURATION_TICKS;
			double expandMargin = 0.8 + prog * 14.2;
			if (charge % 4 == 0) {
				carveOpenedBodyEncroachment(level, entity, expandMargin);
			}

			double beamQuality = getBeamParticleQuality(level, core.x, core.z, nbt);
			spawnChargingCorona(level, core, prog, beamQuality);
			if (charge % 15 == 1) {
				playEngineSound(level, core.x, core.y, core.z, "block.beacon.ambient", 48.0F + (float) prog * 32.0F, 0.35F + (float) prog * 0.30F);
			}
			if (charge >= CHARGE_DURATION_TICKS) {
				nbt.putInt("verdant_state", 2);
				nbt.putInt("verdant_cycle_ticks", 0);
				playEngineSound(level, core.x, core.y, core.z, "block.beacon.activate", 40.0F, 0.40F);
				playEngineSound(level, core.x, core.y, core.z, "block.end_portal.spawn", 48.0F, 1.85F);
				playEngineSound(level, core.x, groundY, core.z, "block.end_portal.spawn", 48.0F, 1.85F);
			}
			handleActiveShockwaves(level, nbt);
			return;
		}

		if (!nbt.getBoolean("verdant_synced_active")) {
			startTerraformingFlow(entity, nbt);
			nbt.putBoolean("verdant_synced_active", true);
		}

		int cycle = nbt.getInt("verdant_cycle_ticks") + 1;
		int pounds = nbt.getInt("verdant_pound_count");
		double gravR = Math.min(MAX_POUND_RADIUS_CAP, BASE_GRAVITY_RADIUS + (pounds * POUND_RADIUS_INCREMENT));
		double poundR = Math.min(MAX_POUND_RADIUS_CAP, BASE_POUND_RADIUS + (pounds * POUND_RADIUS_INCREMENT));

		double densityFactor = 0.0;
		int decayTicks = persistentInt(nbt, "verdant_pound_decay_ticks", 0);
		if (cycle >= RINGS_START_TICK && cycle < CYCLE_TOTAL_TICKS) {
			double chargeProg = (double) (cycle - RINGS_START_TICK) / (CYCLE_TOTAL_TICKS - RINGS_START_TICK);
			densityFactor = chargeProg * chargeProg * (3.0 - 2.0 * chargeProg);
		} else if (decayTicks > 0) {
			decayTicks--;
			nbt.putInt("verdant_pound_decay_ticks", decayTicks);
			double decayProg = (double) decayTicks / 50.0;
			densityFactor = decayProg * decayProg * (3.0 - 2.0 * decayProg);
		}

		double beamQuality = getBeamParticleQuality(level, core.x, core.z, nbt);
		renderToweringBeam(level, core, groundY, BEAM_RADIUS, densityFactor, beamQuality);
		carveObstructingCanopy(level, core, groundY, BEAM_RADIUS);
		if (level.getGameTime() % 10 == 0) {
			carveOpenedBodyEncroachment(level, entity, 15.0);
		}

		boolean terraform = isDimensionAllowedForTerraforming(level);
		if (terraform) {
			if (isGravityBeamPenetrationEnabled(nbt)) {
				performSlowKineticTerraforming(level, nbt, core.x, groundY, core.z, BEAM_RADIUS, pounds);
			}
			performContinuousBeamTipInfection(level, nbt, core.x, groundY, core.z, cycle, pounds);
			performContinuousSolidRadialWoodInfection(level, entity, nbt, core.x, groundY, core.z, pounds);
			handleBurstInfectionSpill(level, nbt, core.x, groundY, core.z, pounds);
			double curInfR = Math.min(MAX_INFECTION_RADIUS, persistentDouble(nbt, "verdant_infection_radius", BEAM_RADIUS + 2.0));
			net.mcreator.thebackwoods.VerdantEngineTerraforming.processBiomeTerraforming(level, nbt, core.x, groundY, core.z, curInfR);
		}

		handleGravityDistortionAndProjectiles(level, entity, core, groundY, gravR, cycle);

		if (cycle < RINGS_START_TICK) {
			if (cycle % 35 == 0) playEngineSound(level, core.x, groundY + 4.0, core.z, "block.beacon.ambient", 64.0F, 0.40F);
		} else if (cycle < CYCLE_TOTAL_TICKS) {
			double chargeProg = (double) (cycle - RINGS_START_TICK) / (CYCLE_TOTAL_TICKS - RINGS_START_TICK);
			double easeQuint = Math.pow(chargeProg, 3.2);
			double ringSpeed = 0.008 + 0.075 * easeQuint;
			double ringTravel = persistentDouble(nbt, "verdant_ring_travel", 0.0) + ringSpeed;
			nbt.putDouble("verdant_ring_travel", ringTravel);

			spawnPreSlamOnionRings(level, core, groundY, BEAM_RADIUS, chargeProg, ringTravel, beamQuality);
			if (cycle % 8 == 0) playEngineSound(level, core.x, groundY + 2.0, core.z, "block.beacon.power_select", 64.0F, 0.38F + (float) chargeProg * 0.25F);
		} else {
			triggerPlanetaryPound(level, entity, core.x, groundY, core.z, poundR, pounds, terraform);
			nbt.putInt("verdant_pound_count", pounds + 1);
			nbt.putInt("verdant_pound_decay_ticks", 50);
			nbt.putDouble("verdant_ring_travel", 0.0);
			cycle = 0;
		}

		nbt.putInt("verdant_cycle_ticks", cycle);
		handleActiveShockwaves(level, nbt);
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		Entity e = event.getEntity();
		if (e != null && isVerdantEngine(e)) {
			if (e instanceof LivingEntity living) {
				boolean isDying = e.getPersistentData().getBoolean("verdant_death_sequence_active");
				if (isDying) {
					living.hurtTime = 0;
					living.hurtDuration = 0;
					living.deathTime = 0;
					living.setHealth(1.0F);
				}
			}
			execute(event, e.level(), e.getX(), e.getY(), e.getZ(), e);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingDamage(LivingDamageEvent.Pre event) {
		Entity entity = event.getEntity();
		if (entity != null && isVerdantEngine(entity) && entity instanceof LivingEntity living) {
			boolean isAlreadyDying = persistentBoolean(entity.getPersistentData(), "verdant_death_sequence_active", false);
			if (isAlreadyDying) {
				event.setNewDamage(0.0F);
				living.setHealth(1.0F);
				living.deathTime = 0;
				living.hurtDuration = 0;
				living.hurtTime = 0;
				living.setInvulnerable(true);
				return;
			}

			float incomingDmg = event.getNewDamage();
			float currentHp = living.getHealth();
			boolean isLethal = currentHp <= 20.0F || (currentHp - incomingDmg) <= 0.0F || living.isDeadOrDying() || Float.isInfinite(incomingDmg) || Float.isNaN(incomingDmg);

			if (isLethal) {
				if (isTerraformingActive(entity)) {
					event.setNewDamage(0.0F);
					living.setHealth(1.0F);
					living.deathTime = 0;
					living.hurtDuration = 0;
				living.hurtTime = 0;
				living.setInvulnerable(true);
					startDeathSequence(entity.level() instanceof ServerLevel sl ? sl : null, entity, entity.getPersistentData());
				} else {
					setSyncedIsDeath(entity, false);
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		Entity entity = event.getEntity();
		if (entity != null && isVerdantEngine(entity) && entity instanceof LivingEntity living) {
			boolean isAlreadyDying = persistentBoolean(entity.getPersistentData(), "verdant_death_sequence_active", false);
			if (isAlreadyDying) {
				event.setAmount(0.0F);
				event.setCanceled(true);
				living.setHealth(1.0F);
				living.deathTime = 0;
				living.hurtDuration = 0;
				living.hurtTime = 0;
				living.setInvulnerable(true);
				return;
			}

			float incomingDmg = event.getAmount();
			float currentHp = living.getHealth();
			boolean isLethal = currentHp <= 20.0F || (currentHp - incomingDmg) <= 0.0F || living.isDeadOrDying() || Float.isInfinite(incomingDmg) || Float.isNaN(incomingDmg);

			if (isLethal) {
				if (isTerraformingActive(entity)) {
					event.setAmount(0.0F);
					event.setCanceled(true);
					living.setHealth(1.0F);
					living.deathTime = 0;
					living.hurtDuration = 0;
				living.hurtTime = 0;
				living.setInvulnerable(true);
					startDeathSequence(entity.level() instanceof ServerLevel sl ? sl : null, entity, entity.getPersistentData());
				} else {
					setSyncedIsDeath(entity, false);
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingDeath(LivingDeathEvent event) {
		Entity entity = event.getEntity();
		if (entity != null && isVerdantEngine(entity) && entity instanceof LivingEntity living) {
			boolean isAlreadyDying = persistentBoolean(entity.getPersistentData(), "verdant_death_sequence_active", false);
			if (isAlreadyDying) {
				event.setCanceled(true);
				living.setHealth(1.0F);
				living.deathTime = 0;
				living.hurtDuration = 0;
				living.hurtTime = 0;
				living.setInvulnerable(true);
				return;
			}

			if (isTerraformingActive(entity)) {
				event.setCanceled(true);
				living.setHealth(1.0F);
				living.deathTime = 0;
				living.hurtDuration = 0;
				living.hurtTime = 0;
				living.setInvulnerable(true);
				startDeathSequence(entity.level() instanceof ServerLevel sl ? sl : null, entity, entity.getPersistentData());
			} else {
				setSyncedIsDeath(entity, false);
			}
		}
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		Entity entity = event.getEntity();
		if (entity != null && isVerdantEngine(entity) && event.getLevel() instanceof ServerLevel level) {
			releaseAllCaughtEntities(level, entity.position(), 256.0);
			if (entity.isRemoved() && (entity.getRemovalReason() == Entity.RemovalReason.KILLED || entity.getRemovalReason() == Entity.RemovalReason.DISCARDED)) {
				boolean isDying = persistentBoolean(entity.getPersistentData(), "verdant_death_sequence_active", false);
				if (isDying || getSyncedIsDeath(entity)) {
					ACTIVE_CORES.remove(entity.getUUID());
				}
			}
		}
	}

	public static class ActiveVerdantCore {
		public final java.util.UUID entityUuid;
		public final ResourceLocation dimension;
		public double coreX;
		public double coreZ;
		public double groundY;
		public double scanTop;
		public double infectionRadius;
		public int pounds;
		public long lastUpdateTick;

		public ActiveVerdantCore(java.util.UUID uuid, ResourceLocation dim, double cx, double cz, double gy, double scanTop, double r, int pounds, long tick) {
			this.entityUuid = uuid;
			this.dimension = dim;
			this.coreX = cx;
			this.coreZ = cz;
			this.groundY = gy;
			this.scanTop = scanTop;
			this.infectionRadius = r;
			this.pounds = pounds;
			this.lastUpdateTick = tick;
		}
	}

	private static final java.util.Map<java.util.UUID, ActiveVerdantCore> ACTIVE_CORES = new java.util.concurrent.ConcurrentHashMap<>();

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		if (event == null || event.getLevel() == null || event.getLevel().isClientSide()) return;
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		if (!(event.getChunk() instanceof LevelChunk chunk)) return;
		if (!isDimensionAllowedForTerraforming(level)) return;

		ResourceLocation dim = level.dimension().location();
		int chunkX = chunk.getPos().x;
		int chunkZ = chunk.getPos().z;
		int midBlockX = (chunkX << 4) + 8;
		int midBlockZ = (chunkZ << 4) + 8;

		for (ActiveVerdantCore core : ACTIVE_CORES.values()) {
			if (!core.dimension.equals(dim)) continue;

			double distToCenter = Math.hypot(midBlockX - core.coreX, midBlockZ - core.coreZ);
			if (distToCenter > core.infectionRadius + 32.0) continue;

			catchUpInfectChunk(level, chunk, core);
		}
	}

	private static void catchUpInfectChunk(ServerLevel level, LevelChunk chunk, ActiveVerdantCore core) {
		int chunkX = chunk.getPos().x;
		int chunkZ = chunk.getPos().z;
		int minWorldY = (int) level.getMinBuildHeight() + 5;
		int topY = (int) core.scanTop;
		if (topY <= minWorldY) return;

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		Block petrified = getPetrifiedOakBlock();
		Block splintered = getSplinteredOakBlock();
		Block lignumCaro = getLignumCaroBlock();
		Block falseOak = getFalseOakPlanksBlock();
		long seed = level.getSeed();

		double solidR = Math.max(0.0, core.infectionRadius - 3.0);

		for (int lx = 0; lx < 16; lx++) {
			int bx = (chunkX << 4) + lx;
			for (int lz = 0; lz < 16; lz++) {
				int bz = (chunkZ << 4) + lz;

				double dist = Math.hypot(bx - core.coreX, bz - core.coreZ);
				if (dist > core.infectionRadius) continue;

				boolean isSolidCore = dist <= solidR;
				if (!isSolidCore) {
					double edgeDist = core.infectionRadius - dist;
					double prob = edgeDist / 3.0;
					double dither = 0.5 + 0.5 * Math.sin(bx * 0.45 + bz * 0.45 + seed);
					if (Math.random() > prob * (0.65 + 0.35 * dither)) continue;
				}

				boolean isCell = isCellularMembrane(bx, bz, seed);
				int worldTop = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
				int oceanFloor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, lx, lz);
				boolean hasCeiling = isCeiledDimension(level);
				int startY = hasCeiling ? Math.min(topY, (int) core.groundY + 35) : Math.min(topY, worldTop);
				int crustEndY = Math.max(minWorldY, Math.min(oceanFloor, startY) - 32);

				// Process fluid / water bodies completely across column
				if (worldTop > oceanFloor) {
					for (int y = worldTop; y >= oceanFloor; y--) {
						pos.set(bx, y, bz);
						BlockState st = chunk.getBlockState(pos);
						if (st.isAir()) continue;
						replaceBlockWithOakPlague(level, pos, st, false, isCell);
					}
				}

				// 1. Surface Crust & Mountain Body Infection
				for (int y = startY; y >= crustEndY; y--) {
					pos.set(bx, y, bz);
					BlockState st = chunk.getBlockState(pos);
					if (st.isAir()) continue;

					Block b = st.getBlock();
					if (b == Blocks.ANCIENT_DEBRIS || isAncientDebris(st, b)) continue;

					if (isIndestructibleBlock(st, level, pos) && !canInfectIndestructible(null)) break;

					boolean isAlreadyInfected = (b == Blocks.OAK_PLANKS || b == Blocks.OAK_LOG || b == petrified || b == splintered || b == lignumCaro || b == falseOak || b == Blocks.PETRIFIED_OAK_SLAB);
					if (isAlreadyInfected) continue;

					if (isFoliageOrDebris(st, b)) {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
						continue;
					}

					replaceBlockWithOakPlague(level, pos, st, false, isCell);
				}

				// 2. Subterranean & Cave Strata Infection (Upper/Mid and Deepslate Caverns)
				if (crustEndY > minWorldY + 8) {
					int caveSamples = isSolidCore ? 8 : 4;
					for (int cs = 0; cs < caveSamples; cs++) {
						int cy = minWorldY + (int) (Math.random() * (crustEndY - minWorldY));
						pos.set(bx, cy, bz);
						BlockState st = chunk.getBlockState(pos);
						Block b = st.getBlock();
						if (b == Blocks.BEDROCK || b == Blocks.BARRIER) continue;

						if (st.isAir()) {
							pos.set(bx, cy - 1, bz);
							st = chunk.getBlockState(pos);
							b = st.getBlock();
							if (st.isAir() || b == Blocks.BEDROCK || b == Blocks.BARRIER) continue;
						}

						boolean isAlreadyInfected = (b == Blocks.OAK_PLANKS || b == Blocks.OAK_LOG || b == petrified || b == splintered || b == lignumCaro || b == falseOak || b == Blocks.PETRIFIED_OAK_SLAB);
						if (isAlreadyInfected) continue;

						if (isFoliageOrDebris(st, b)) {
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
							continue;
						}

						replaceBlockWithOakPlague(level, pos, st, false, isCell);
					}
				}
			}
		}

		try {
			net.mcreator.thebackwoods.VerdantEngineTerraforming.processBiomeTerraforming(level, null, core.coreX, core.groundY, core.coreZ, core.infectionRadius);
		} catch (Throwable ignored) {}
	}

	private static boolean isDimensionAllowedForTerraforming(ServerLevel level) {
		if (level == null) return false;
		String id = level.dimension().location().toString().toLowerCase();
		return !id.contains("the_backwoods") && !id.contains("backwood");
	}

	private static double findHighestGroundInRadius(ServerLevel level, double cx, double cz, double fromY, int radius) {
		double maxGround = level.getMinBuildHeight() + 4.0;
		for (int dx = -radius; dx <= radius; dx += Math.max(1, radius / 2)) {
			for (int dz = -radius; dz <= radius; dz += Math.max(1, radius / 2)) {
				double gy = findGroundBelow(level, cx + dx, cz + dz, fromY);
				if (gy > maxGround) {
					maxGround = gy;
				}
			}
		}
		return maxGround;
	}

	private static double findCeilingAbove(ServerLevel level, double x, double z, double fromY) {
		int floorX = Mth.floor(x);
		int floorZ = Mth.floor(z);
		int startY = Math.max((int) level.getMinBuildHeight() + 2, Mth.floor(fromY));
		int maxY = (int) level.getMaxBuildHeight() - 2;
		double lowestCeiling = maxY + 2.0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int dx = -5; dx <= 5; dx += 2) {
			for (int dz = -5; dz <= 5; dz += 2) {
				for (int y = startY; y < maxY; y++) {
					pos.set(floorX + dx, y, floorZ + dz);
					if (!level.hasChunkAt(pos)) continue;
					BlockState st = level.getBlockState(pos);
					if (!st.isAir() && st.getFluidState().isEmpty() && !st.is(BlockTags.LEAVES) && !st.is(BlockTags.FLOWERS) && !st.is(BlockTags.SAPLINGS)) {
						if (y < lowestCeiling) {
							lowestCeiling = y;
						}
						break;
					}
				}
			}
		}
		return lowestCeiling;
	}

	private static double calculateOptimalVerdantHoverY(ServerLevel level, double x, double z, double currentY) {
		double scanStart = Math.min(level.getMaxBuildHeight() - 5.0, Math.max(currentY + 60.0, 180.0));
		double highestGroundY = findHighestGroundInRadius(level, x, z, scanStart, 5);

		// First check if there is an impenetrable ceiling directly above current ground altitude
		double ceilingY = findCeilingAbove(level, x, z, highestGroundY + 3.0);

		// Operational target hover altitude (HOVER_TARGET_ALTITUDE above ground surface)
		double targetY = highestGroundY + HOVER_TARGET_ALTITUDE;

		// Check for dimension ceilings (e.g., Nether ceiling or subterranean cavern roofs)
		if (ceilingY < level.getMaxBuildHeight()) {
			double headroom = 28.0; // Ample 28-block buffer under lowest ceiling point to prevent animation clipping
			double maxAllowedUnderCeiling = ceilingY - headroom;
			double minAllowedAboveGround = highestGroundY + 12.0;

			if (maxAllowedUnderCeiling > minAllowedAboveGround) {
				targetY = Math.min(targetY, maxAllowedUnderCeiling);
				targetY = Math.max(targetY, minAllowedAboveGround);
			} else {
				// Tight cave space: downward-biased centering to guarantee head clearance
				targetY = highestGroundY + Math.max(6.0, (ceilingY - highestGroundY) * 0.40);
			}
		}

		return Math.min(level.getMaxBuildHeight() - 12.0, Math.max(level.getMinBuildHeight() + 15.0, targetY));
	}

	private static double[] findBestRelocationTarget(ServerLevel level, Entity entity, CompoundTag nbt, double currentX, double currentY, double currentZ, double preferredDx, double preferredDz) {
		double bestTargetX = currentX;
		double bestTargetY = currentY;
		double bestTargetZ = currentZ;
		double highestFitness = -999999.0;

		double[] testDistances = new double[]{288.0, 384.0};
		int angleSteps = 12;

		AABB engineBox = new AABB(currentX - 600.0, currentY - 256.0, currentZ - 600.0, currentX + 600.0, currentY + 256.0, currentZ + 600.0);
		List<Entity> existingEngines = level.getEntities(entity, engineBox, e -> isVerdantEngine(e) && e.isAlive() && e != entity);

		double defaultGroundY = persistentDouble(nbt, "verdant_ground_y", Math.max(level.getMinBuildHeight() + 20.0, currentY - HOVER_TARGET_ALTITUDE));

		for (double dist : testDistances) {
			for (int dir = 0; dir < angleSteps; dir++) {
				double angle = (Math.PI * 2.0 / angleSteps) * dir + (Math.random() * 0.2 - 0.1);
				double candX = currentX + Math.cos(angle) * dist;
				double candZ = currentZ + Math.sin(angle) * dist;

				int candChunkX = Mth.floor(candX) >> 4;
				int candChunkZ = Mth.floor(candZ) >> 4;
				LevelChunk candChunk = level.getChunkSource().getChunkNow(candChunkX, candChunkZ);

				double candGroundY;
				if (candChunk != null) {
					candGroundY = candChunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(candX) & 15, Mth.floor(candZ) & 15);
				} else {
					candGroundY = defaultGroundY;
				}

				if (candGroundY <= level.getMinBuildHeight() + 4.0 || candGroundY >= level.getMaxBuildHeight() - 25.0) {
					continue;
				}

				double candHoverY = calculateOptimalVerdantHoverY(level, candX, candZ, candGroundY);
				double fitness = 100.0;

				if (preferredDx != 0.0 || preferredDz != 0.0) {
					double cosSim = (Math.cos(angle) * preferredDx + Math.sin(angle) * preferredDz);
					fitness += cosSim * 80.0;
				}

				for (Entity other : existingEngines) {
					double d = Math.hypot(other.getX() - candX, other.getZ() - candZ);
					if (d < 224.0) {
						fitness -= (224.0 - d) * 15.0;
					}
				}

				if (candChunk != null) {
					if (isColumnInfected(level, Mth.floor(candX), Mth.floor(candZ), candHoverY)) {
						fitness -= 70.0;
					} else {
						fitness += 50.0;
					}

					// 1. Tree Canopy Check
					int worldTopY = candChunk.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(candX) & 15, Mth.floor(candZ) & 15);
					BlockPos surfacePos = new BlockPos(Mth.floor(candX), worldTopY - 1, Mth.floor(candZ));
					BlockState surfaceState = level.getBlockState(surfacePos);
					boolean isTree = surfaceState.is(BlockTags.LEAVES) || surfaceState.is(BlockTags.LOGS);
					if (!isTree) {
						String blockId = BuiltInRegistries.BLOCK.getKey(surfaceState.getBlock()).getPath();
						if (blockId.contains("stem") || blockId.contains("hyphae") || blockId.contains("leaves") || blockId.contains("log")) {
							isTree = true;
						}
					}
					if (isTree) {
						fitness -= 150.0; // Avoid landing on top of trees (leaves/logs/stems)
					}

					// 2. Thin Pillar Check (checks 3x3 footprint surrounding candidate)
					int lowerNeighbors = 0;
					int sampleRadius = 1;
					for (int dx = -sampleRadius; dx <= sampleRadius; dx++) {
						for (int dz = -sampleRadius; dz <= sampleRadius; dz++) {
							if (dx == 0 && dz == 0) continue;
							int nx = Mth.floor(candX) + dx;
							int nz = Mth.floor(candZ) + dz;
							LevelChunk nChunk = level.getChunkSource().getChunkNow(nx >> 4, nz >> 4);
							double nGroundY = nChunk != null ? nChunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, nx & 15, nz & 15) : candGroundY;
							if (candGroundY - nGroundY > 4.0) {
								lowerNeighbors++;
							}
						}
					}
					if (lowerNeighbors >= 4) { // If 4 or more of the 8 neighbors are significantly lower, it's a thin pillar/spire
						fitness -= 120.0; // Avoid aiming on thin isolated structures
					}
				} else {
					BlockPos candPos = new BlockPos(Mth.floor(candX), Mth.floor(candHoverY), Mth.floor(candZ));
					if (isInfectedBiome(level, candPos)) {
						fitness -= 70.0;
					} else {
						fitness += 50.0;
					}
				}

				if (fitness > highestFitness) {
					highestFitness = fitness;
					bestTargetX = candX;
					bestTargetY = candHoverY;
					bestTargetZ = candZ;
				}
			}
		}

		if (highestFitness > -900000.0 && (Math.hypot(bestTargetX - currentX, bestTargetZ - currentZ) >= 64.0)) {
			return new double[]{bestTargetX, bestTargetY, bestTargetZ};
		}
		return null;
	}

	private static void setupInitialSpawnData(ServerLevel level, Entity entity, CompoundTag nbt) {
		double sx = Math.floor(entity.getX()) + 0.5, sz = Math.floor(entity.getZ()) + 0.5;
		double spawnY = entity.getY(); // Entity stays at its natural spawn altitude without being forced high into ceilings
		double sy = findHighestGroundInRadius(level, sx, sz, spawnY > 0 ? spawnY + 10.0 : level.getMaxBuildHeight() - 10.0, 5);

		nbt.putBoolean("verdant_initialized", true);
		nbt.putFloat("verdant_locked_yaw", entity.getYRot());
		nbt.putFloat("verdant_locked_pitch", entity.getXRot());
		nbt.putDouble("verdant_ground_y", sy);
		nbt.putInt("verdant_charge_ticks", 0);
		nbt.putInt("verdant_cycle_ticks", 0);
		nbt.putInt("verdant_pound_count", 0);
		nbt.putDouble("verdant_crater_depth", 0.0);
		nbt.putDouble("verdant_infection_radius", BEAM_RADIUS + 2.0);
		nbt.putInt("verdant_burst_spread_ticks", 0);
		nbt.putInt("verdant_pound_decay_ticks", 0);
		nbt.putDouble("verdant_ring_travel", 0.0);
		nbt.putBoolean("verdant_synced_active", false);
		setSyncedIsTerraforming(entity, false);
		setSyncedCurrentlyTerraforming(entity, false);
		setSyncedStopTerra(entity, false);
		setSyncedIsDeath(entity, false);
		entity.setNoGravity(true);
		ensureLongDistanceTracking(level, entity);

		// Proactively scan for any existing nearby active/terraforming Verdant on spawn
		double searchR = 384.0;
		AABB searchBox = new AABB(sx - searchR, spawnY - 128.0, sz - searchR, sx + searchR, spawnY + 128.0, sz + searchR);
		List<Entity> neighbors = level.getEntities(entity, searchBox, e -> isVerdantEngine(e) && e.isAlive() && e != entity);

		boolean shouldRelocateOnSpawn = false;
		double preferredDx = 0.0, preferredDz = 0.0;
		if (!neighbors.isEmpty()) {
			double avgDx = 0.0, avgDz = 0.0;
			int count = 0;
			for (Entity other : neighbors) {
				double distSq = entity.distanceToSqr(other);
				if (distSq < 224.0 * 224.0 && distSq > 0.01) {
					avgDx += (sx - other.getX());
					avgDz += (sz - other.getZ());
					count++;
				}
			}
			if (count > 0) {
				double len = Math.sqrt(avgDx * avgDx + avgDz * avgDz);
				if (len < 0.001) { avgDx = 1.0; avgDz = 0.0; len = 1.0; }
				preferredDx = avgDx / len;
				preferredDz = avgDz / len;
				shouldRelocateOnSpawn = true;
			}
		}

		if (shouldRelocateOnSpawn) {
			double optimalSpawnHoverY = calculateOptimalVerdantHoverY(level, sx, sz, spawnY);
			double[] relocationTarget = findBestRelocationTarget(level, entity, nbt, sx, optimalSpawnHoverY, sz, preferredDx, preferredDz);
			if (relocationTarget != null) {
				// Immediately queue relocation starting from spawn coordinates and gliding upward into cruising altitude
				nbt.putInt("verdant_state", 1);
				nbt.putDouble("verdant_locked_x", sx);
				nbt.putDouble("verdant_locked_y", spawnY);
				nbt.putDouble("verdant_locked_z", sz);
				nbt.putDouble("verdant_target_x", relocationTarget[0]);
				nbt.putDouble("verdant_target_y", relocationTarget[1]);
				nbt.putDouble("verdant_target_z", relocationTarget[2]);
				nbt.putBoolean("verdant_is_relocating", true);
				return;
			}
		}

		// Standard uncrowded spawn: proceed with state 0 ascension towards optimal hover altitude
		double targetHoverY = calculateOptimalVerdantHoverY(level, sx, sz, spawnY);
		nbt.putInt("verdant_state", 0); // State 0: floats smoothly down from spawnY to targetHoverY
		nbt.putDouble("verdant_locked_x", sx);
		nbt.putDouble("verdant_locked_y", targetHoverY); // Operational hover target (+72 blocks above ground)
		nbt.putDouble("verdant_locked_z", sz);
		nbt.putBoolean("verdant_is_relocating", false);
	}

	public static void ensureLongDistanceTracking(ServerLevel level, Entity entity) {
		if (level == null || entity == null || !entity.isAlive()) return;
		try {
			CompoundTag nbt = entity.getPersistentData();
			double curInfR = persistentDouble(nbt, "verdant_infection_radius", BEAM_RADIUS + 2.0);
			int chunkRadius = Math.min(18, Math.max(net.mcreator.thebackwoods.EntityTrackerData.LOADER_TICKET_RADIUS, (int) Math.ceil((curInfR + 24.0) / 16.0)));
			net.mcreator.thebackwoods.EntityTrackerData.track(entity, chunkRadius, "verdant_engine");
		} catch (Throwable ignored) {}
		// Boost server tracking range to 1024 blocks so far-away players never lose entity sync
		try {
			for (java.lang.reflect.Field f : EntityType.class.getDeclaredFields()) {
				if (f.getType() == int.class && (f.getName().equals("clientTrackingRange") || f.getName().equals("f_20560_"))) {
					f.setAccessible(true);
					if (f.getInt(entity.getType()) < 64) {
						f.setInt(entity.getType(), 64);
					}
					break;
				}
			}
		} catch (Throwable ignored) {}
		try {
			Object chunkMap = level.getChunkSource().chunkMap;
			for (java.lang.reflect.Field mapField : chunkMap.getClass().getDeclaredFields()) {
				if (mapField.getName().equals("entityMap") || mapField.getName().equals("f_140134_") || java.util.Map.class.isAssignableFrom(mapField.getType())) {
					mapField.setAccessible(true);
					Object mapObj = mapField.get(chunkMap);
					if (mapObj instanceof java.util.Map<?, ?> map) {
						Object te = map.get(entity.getId());
						if (te != null) {
							for (java.lang.reflect.Field rf : te.getClass().getDeclaredFields()) {
								if (rf.getType() == int.class && (rf.getName().equals("range") || rf.getName().equals("f_140416_"))) {
									rf.setAccessible(true);
									if (rf.getInt(te) < 1024) {
										rf.setInt(te, 1024);
									}
									break;
								}
							}
						}
						break;
					}
				}
			}
		} catch (Throwable ignored) {}
	}

	private static void handleAscensionPhase(ServerLevel level, Entity entity, CompoundTag nbt, double tx, double ty, double tz, float yaw, float pitch) {
		entity.setNoGravity(true);
		entity.fallDistance = 0.0F;
		entity.setYRot(yaw);
		entity.setXRot(pitch);

		AABB box = entity.getBoundingBox();
		double cx = entity.getX(), cz = entity.getZ();
		double organicMargin = 1.0;

		int searchMinX = Mth.floor(box.minX - (organicMargin + 1.2));
		int searchMaxX = Mth.floor(box.maxX + (organicMargin + 1.2));
		int searchMinZ = Mth.floor(box.minZ - (organicMargin + 1.2));
		int searchMaxZ = Mth.floor(box.maxZ + (organicMargin + 1.2));

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int clearBottom = Math.max(level.getMinBuildHeight() + 1, Mth.floor(box.minY - 1.5));
		int clearTop = Math.min(level.getMaxBuildHeight() - 1, Mth.floor(box.maxY + 3.5));
		for (int y = clearBottom; y <= clearTop; y++) {
			for (int x = searchMinX; x <= searchMaxX; x++) {
				for (int z = searchMinZ; z <= searchMaxZ; z++) {
					if (isInsideOrganicHitboxClearing(box, cx, cz, x, y, z, organicMargin)) {
						pos.set(x, y, z);
						if (!level.hasChunkAt(pos)) continue;
						BlockState st = level.getBlockState(pos);
						if (st.isAir()) continue;
						if (isIndestructibleBlock(st, level, pos)) continue;
						level.destroyBlock(pos, false);
						if (!level.getBlockState(pos).isAir()) {
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
						}
						if (Math.random() < 0.12) {
							sendFarParticles(level, ParticleTypes.POOF, x + 0.5, y + 0.5, z + 0.5, 2, 0.15, 0.15, 0.15, 0.02);
						}
					}
				}
			}
		}

		double dx = tx - entity.getX(), dy = ty - entity.getY(), dz = tz - entity.getZ();
		if (Math.abs(dy) <= 0.5 && Math.abs(dx) <= 0.5 && Math.abs(dz) <= 0.5) {
			entity.setPos(tx, ty, tz);
			entity.setDeltaMovement(0, 0, 0);
			nbt.putInt("verdant_state", 1);
			nbt.putInt("verdant_charge_ticks", 0);
			nbt.putInt("verdant_ascend_stuck_ticks", 0);
			return;
		}

		int stuckTicks = persistentInt(nbt, "verdant_ascend_stuck_ticks", 0) + 1;
		nbt.putInt("verdant_ascend_stuck_ticks", stuckTicks);
		if (stuckTicks > 80) { // 4-second stuck failsafe smooth position reset
			entity.setPos(tx, ty, tz);
			entity.setDeltaMovement(0, 0, 0);
			nbt.putInt("verdant_state", 1);
			nbt.putInt("verdant_charge_ticks", 0);
			nbt.putInt("verdant_ascend_stuck_ticks", 0);
			return;
		}

		double vy = dy >= 0 ? Math.min(0.28, Math.max(0.12, dy * 0.08)) : Math.max(-0.28, Math.min(-0.12, dy * 0.08));
		entity.setDeltaMovement(Math.signum(dx) * Math.min(Math.abs(dx) * 0.1, 0.15), vy, Math.signum(dz) * Math.min(Math.abs(dz) * 0.1, 0.15));
		entity.hasImpulse = true;
		if (level.getGameTime() % 25 == 0) playEngineSound(level, entity.getX(), entity.getY(), entity.getZ(), "block.beacon.ambient", 20.0F, 0.5F);
	}

	private static double findGroundBelow(ServerLevel level, double x, double z, double fromY) {
		int floorX = Mth.floor(x);
		int floorZ = Mth.floor(z);
		int startY = Mth.floor(fromY);
		LevelChunk chunk = level.getChunkSource().getChunkNow(floorX >> 4, floorZ >> 4);
		if (chunk == null) {
			return Math.min(fromY, 64.0);
		}
		int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, floorX & 15, floorZ & 15);
		if (startY > surfaceY) {
			startY = surfaceY + 1;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(floorX, startY, floorZ);
		int minY = (int) level.getMinBuildHeight() + 2;
		while (pos.getY() > minY) {
			BlockState st = chunk.getBlockState(pos);
			if (!st.isAir() && !st.is(BlockTags.LEAVES) && !st.is(BlockTags.FLOWERS) && !st.is(BlockTags.SAPLINGS) && !st.is(BlockTags.CROPS)
				&& st.getBlock() != Blocks.SHORT_GRASS && st.getBlock() != Blocks.TALL_GRASS && st.getBlock() != Blocks.FERN && st.getBlock() != Blocks.LARGE_FERN
				&& st.getBlock() != Blocks.DEAD_BUSH && st.getBlock() != Blocks.VINE && st.getBlock() != Blocks.HANGING_ROOTS && st.getBlock() != Blocks.SNOW && st.getBlock() != Blocks.MOSS_CARPET) {
				// If we encounter a fluid, return its exact Y level so replacement starts directly on the fluid surface
				if (!st.getFluidState().isEmpty() || st.getBlock() == Blocks.WATER || st.getBlock() == Blocks.LAVA) {
					return pos.getY();
				}
				return pos.getY() + 1.0;
			}
			pos.move(Direction.DOWN);
		}
		return Math.max(level.getMinBuildHeight() + 4.0, surfaceY);
	}

	private static int findTopBlock(ServerLevel level, int x, int z, double scanTop) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, (int) scanTop, z);
		int bottom = (int) level.getMinBuildHeight() + 2;
		while (pos.getY() > bottom) {
			BlockState st = level.getBlockState(pos);
			if (!st.isAir()) {
				return pos.getY();
			}
			pos.move(Direction.DOWN);
		}
		return bottom;
	}

	private static void cleanseNegativeEffects(Entity entity) {
		if (entity instanceof LivingEntity living) {
			Collection<MobEffectInstance> active = living.getActiveEffects();
			if (!active.isEmpty()) {
				List<Holder<MobEffect>> toRemove = null;
				for (MobEffectInstance inst : active) {
					Holder<MobEffect> holder = inst.getEffect();
					if (holder == null) continue;
					MobEffect effect = holder.value();
					if (effect == null) continue;

					boolean isNegative = !effect.isBeneficial();
					if (!isNegative) {
						String key = holder.unwrapKey().map(k -> k.location().toString()).orElse("").toLowerCase();
						if (key.contains("wither") || key.contains("irradiated") || key.contains("radiation")) {
							isNegative = true;
						}
					}

					if (isNegative) {
						if (toRemove == null) toRemove = new ArrayList<>(4);
						toRemove.add(holder);
					}
				}
				if (toRemove != null) {
					for (int i = 0; i < toRemove.size(); i++) {
						living.removeEffect(toRemove.get(i));
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> EntityDataAccessor<T> getEntityDataAccessor(Entity entity, String paramName) {
		if (entity == null || paramName == null) return null;
		String key = entity.getClass().getName() + ":" + paramName;
		Field field = SYNCED_DATA_FIELD_CACHE.get(key);
		if (field == null) {
			Class<?> cur = entity.getClass();
			while (cur != null && cur != Object.class) {
				for (Field f : cur.getDeclaredFields()) {
					if (EntityDataAccessor.class.isAssignableFrom(f.getType()) && (f.getName().equalsIgnoreCase("DATA_" + paramName) || f.getName().equalsIgnoreCase(paramName) || f.getName().toLowerCase().contains(paramName.toLowerCase()))) {
						f.setAccessible(true);
						SYNCED_DATA_FIELD_CACHE.put(key, f);
						field = f;
						break;
					}
				}
				if (field != null) break;
				cur = cur.getSuperclass();
			}
		}
		if (field != null) {
			try { return (EntityDataAccessor<T>) field.get(null); } catch (Exception ignored) {}
		}
		return null;
	}

	private static void setSyncedIsTerraforming(Entity entity, boolean val) {
		if (entity == null) return;
		EntityDataAccessor<Boolean> acc = getEntityDataAccessor(entity, "isTerraforming");
		if (acc != null) {
			try { entity.getEntityData().set(acc, val); } catch (Exception ignored) {}
		}
		entity.getPersistentData().putBoolean("isTerraforming", val);
		entity.getPersistentData().putBoolean("DataisTerraforming", val);
		if (val) {
			setSyncedCurrentlyTerraforming(entity, false);
			setSyncedStopTerra(entity, false);
			setSyncedIsDeath(entity, false);
		}
	}

	private static void setSyncedCurrentlyTerraforming(Entity entity, boolean val) {
		if (entity == null) return;
		EntityDataAccessor<Boolean> acc = getEntityDataAccessor(entity, "currentlyTerraforming");
		if (acc != null) {
			try { entity.getEntityData().set(acc, val); } catch (Exception ignored) {}
		}
		entity.getPersistentData().putBoolean("currentlyTerraforming", val);
		entity.getPersistentData().putBoolean("DatacurrentlyTerraforming", val);
		if (val) {
			setSyncedIsTerraforming(entity, false);
			setSyncedStopTerra(entity, false);
			setSyncedIsDeath(entity, false);
		}
	}

	private static boolean getSyncedIsTerraforming(Entity entity) {
		if (entity == null) return false;
		EntityDataAccessor<Boolean> acc = getEntityDataAccessor(entity, "isTerraforming");
		if (acc != null) {
			try { return entity.getEntityData().get(acc); } catch (Exception ignored) {}
		}
		return persistentBoolean(entity.getPersistentData(), "isTerraforming", false) || persistentBoolean(entity.getPersistentData(), "DataisTerraforming", false);
	}

	private static boolean getSyncedCurrentlyTerraforming(Entity entity) {
		if (entity == null) return false;
		EntityDataAccessor<Boolean> acc = getEntityDataAccessor(entity, "currentlyTerraforming");
		if (acc != null) {
			try { return entity.getEntityData().get(acc); } catch (Exception ignored) {}
		}
		return persistentBoolean(entity.getPersistentData(), "currentlyTerraforming", false) || persistentBoolean(entity.getPersistentData(), "DatacurrentlyTerraforming", false);
	}

	private static void resetInfectionAndProgress(CompoundTag nbt) {
		nbt.putDouble("verdant_infection_radius", BEAM_RADIUS + 2.0);
		nbt.putDouble("verdant_burst_target_radius", 0.0);
		nbt.putInt("verdant_burst_spread_ticks", 0);
		nbt.putInt("verdant_pound_burst_ticks", 0);
		nbt.putInt("verdant_pound_count", 0);
		nbt.putInt("verdant_cycle_ticks", 0);
		nbt.putInt("verdant_pound_decay_ticks", 0);
		nbt.putDouble("verdant_ring_travel", 0.0);
		nbt.putDouble("verdant_crater_depth", 0.0);
		nbt.putInt("verdant_charge_ticks", 0);
		nbt.putDouble("verdant_shockwave_stage", 0.0);
		nbt.putDouble("verdant_shockwave_max_r", 0.0);
	}

	private static void startTerraformingFlow(Entity entity, CompoundTag nbt) {
		setSyncedIsTerraforming(entity, true);
		setSyncedCurrentlyTerraforming(entity, false);
		setSyncedStopTerra(entity, false);
		nbt.putInt("verdant_start_terra_delay", 70); // 3.5 seconds delay (70 ticks)
		nbt.putInt("verdant_stop_terra_delay", 0); // cancel stop delay
		nbt.putInt("verdant_clear_stop_terra_ticks", 0); // cancel active stop clear
	}

	private static void stopTerraformingFlow(Entity entity, CompoundTag nbt, boolean delayed) {
		setSyncedIsTerraforming(entity, false);
		setSyncedCurrentlyTerraforming(entity, false);
		setSyncedIsDeath(entity, false);
		nbt.putInt("verdant_start_terra_delay", 0); // cancel start delay
		setSyncedStopTerra(entity, true);
		nbt.putInt("verdant_stop_terra_delay", 0);
		nbt.putInt("verdant_clear_stop_terra_ticks", 70); // auto-clear stop in 3.5s
	}

	public static void onEntityDeath(Entity entity) {
		if (entity == null) return;
		if (entity.level() instanceof ServerLevel level) {
			releaseAllCaughtEntities(level, entity.position(), 256.0);
		}
	}

	private static void releaseAllCaughtEntities(ServerLevel level, Vec3 center, double radius) {
		if (level == null || center == null) return;
		try {
			double safeR = Math.min(72.0, radius);
			AABB releaseBox = new AABB(center.x - safeR, center.y - safeR, center.z - safeR, center.x + safeR, center.y + safeR, center.z + safeR);
			for (Entity victim : level.getEntitiesOfClass(Entity.class, releaseBox, e -> true)) {
				if (victim.isNoGravity() && !isVerdantEngine(victim)) {
					victim.setNoGravity(false);
				}
				if (victim instanceof LivingEntity living) {
					try {
						var gravAttr = living.getAttribute(Attributes.GRAVITY);
						if (gravAttr != null) {
							gravAttr.removeModifier(VERDANT_BEAM_GRAV_MOD_ID);
						}
					} catch (Throwable ignored) {}
				}
				if (victim.getPersistentData() != null && victim.getPersistentData().contains("verdant_slammed_ticks")) {
					victim.getPersistentData().putInt("verdant_slammed_ticks", 0);
				}
			}
			for (UUID id : GRAVITY_MODIFIED_ENTITIES.keySet()) {
				Entity victim = level.getEntity(id);
				if (victim instanceof LivingEntity living) {
					try {
						var gravAttr = living.getAttribute(Attributes.GRAVITY);
						if (gravAttr != null) {
							gravAttr.removeModifier(VERDANT_BEAM_GRAV_MOD_ID);
						}
					} catch (Throwable ignored) {}
				}
			}
			GRAVITY_MODIFIED_ENTITIES.clear();
		} catch (Throwable ignored) {}
	}

	private static void setSyncedStopTerra(Entity entity, boolean val) {
		if (entity == null) return;
		EntityDataAccessor<Boolean> acc = getEntityDataAccessor(entity, "stopTerra");
		if (acc != null) {
			try { entity.getEntityData().set(acc, val); } catch (Exception ignored) {}
		}
		entity.getPersistentData().putBoolean("stopTerra", val);
		entity.getPersistentData().putBoolean("DatastopTerra", val);
		if (val) {
			setSyncedIsTerraforming(entity, false);
			setSyncedCurrentlyTerraforming(entity, false);
			setSyncedIsDeath(entity, false);
		}
	}

	private static void setSyncedIsDeath(Entity entity, boolean val) {
		if (entity == null) return;
		EntityDataAccessor<Boolean> acc = getEntityDataAccessor(entity, "isDead");
		if (acc == null) acc = getEntityDataAccessor(entity, "isDeath");
		if (acc != null) {
			try { entity.getEntityData().set(acc, val); } catch (Exception ignored) {}
		}
		entity.getPersistentData().putBoolean("isDead", val);
		entity.getPersistentData().putBoolean("isDeath", val);
		entity.getPersistentData().putBoolean("DataisDead", val);
		entity.getPersistentData().putBoolean("DataisDeath", val);
		if (val) {
			setSyncedIsTerraforming(entity, false);
			setSyncedCurrentlyTerraforming(entity, false);
			setSyncedStopTerra(entity, false);
		}
	}

	private static boolean getSyncedIsDeath(Entity entity) {
		if (entity == null) return false;
		EntityDataAccessor<Boolean> acc = getEntityDataAccessor(entity, "isDead");
		if (acc == null) acc = getEntityDataAccessor(entity, "isDeath");
		if (acc != null) {
			try { return entity.getEntityData().get(acc); } catch (Exception ignored) {}
		}
		return persistentBoolean(entity.getPersistentData(), "isDead", false) || persistentBoolean(entity.getPersistentData(), "isDeath", false) || persistentBoolean(entity.getPersistentData(), "DataisDead", false) || persistentBoolean(entity.getPersistentData(), "DataisDeath", false);
	}

	private static boolean isTerraformingActive(Entity entity) {
		if (entity == null) return false;
		CompoundTag nbt = entity.getPersistentData();
		boolean byNbt = persistentBoolean(nbt, "isTerraforming", false) || persistentBoolean(nbt, "DataisTerraforming", false) ||
		                persistentBoolean(nbt, "currentlyTerraforming", false) || persistentBoolean(nbt, "DatacurrentlyTerraforming", false) ||
		                persistentBoolean(nbt, "verdant_synced_active", false);
		boolean bySynced = getSyncedIsTerraforming(entity) || getSyncedCurrentlyTerraforming(entity);
		return byNbt || bySynced;
	}

	private static void startDeathSequence(ServerLevel level, Entity entity, CompoundTag nbt) {
		if (entity instanceof LivingEntity living) {
			living.setHealth(1.0F);
			living.deathTime = 0;
			living.setInvulnerable(true);
		}
		setSyncedIsDeath(entity, true);
		setSyncedIsTerraforming(entity, false);
		setSyncedCurrentlyTerraforming(entity, false);
		setSyncedStopTerra(entity, false);
		nbt.putBoolean("verdant_death_sequence_active", true);
		nbt.putInt("verdant_death_ticks", 0);
		nbt.putInt("verdant_start_terra_delay", 0);
		nbt.putInt("verdant_stop_terra_delay", 0);
		nbt.putInt("verdant_clear_stop_terra_ticks", 0);

		if (level != null) {
			releaseAllCaughtEntities(level, entity.position(), 256.0);
			double tx = persistentDouble(nbt, "verdant_locked_x", 0.0);
			double ty = persistentDouble(nbt, "verdant_locked_y", 0.0);
			double tz = persistentDouble(nbt, "verdant_locked_z", 0.0);
			if (tx == 0.0 && ty == 0.0 && tz == 0.0) {
				tx = entity.getX();
				ty = entity.getY();
				tz = entity.getZ();
			}
			playEngineSound(level, tx, ty, tz, "the_backwoods:fractus_hurt", 32.0F, 0.6F);
		}
	}

	private static void handleDeathSequence(ServerLevel level, Entity entity, CompoundTag nbt, double tx, double ty, double tz, float yaw, float pitch) {
		entity.setNoGravity(true);
		entity.fallDistance = 0.0F;
		entity.setYRot(yaw);
		entity.setXRot(pitch);
		entity.setYHeadRot(yaw);
		entity.setYBodyRot(yaw);

		if (entity instanceof LivingEntity living) {
			living.setHealth(1.0F);
			living.deathTime = 0;
			living.setInvulnerable(true);
			if (living instanceof Mob mob) {
				mob.setNoAi(true);
				mob.getNavigation().stop();
				mob.setTarget(null);
				mob.setLastHurtByMob(null);
			}
		}

		setSyncedIsDeath(entity, true);
		setSyncedIsTerraforming(entity, false);
		setSyncedCurrentlyTerraforming(entity, false);
		setSyncedStopTerra(entity, false);

		// Keep position rock-solid stabilized during death sequence without impulse jitter
		double hoverY = ty + Math.sin(level.getGameTime() * 0.04) * 0.15;
		entity.setDeltaMovement(0, 0, 0);
		entity.setPos(tx, hoverY, tz);

		Vec3 core = new Vec3(tx, entity.getY() + entity.getBbHeight() * 0.5 + BEAM_START_Y_OFFSET, tz);

		int deathTicks = persistentInt(nbt, "verdant_death_ticks", 0) + 1;
		nbt.putInt("verdant_death_ticks", deathTicks);

		// Visual breakdown & failure FX during death animation (10 seconds total = 200 ticks)
		if (deathTicks < 180) {
			double progress = deathTicks / 180.0;
			if (level.random.nextFloat() < 0.45f + (float) progress * 0.40f) {
				sendFarParticles(level, ParticleTypes.SMOKE, core.x + (level.random.nextDouble() - 0.5) * 4.0, core.y + (level.random.nextDouble() - 0.5) * 4.0, core.z + (level.random.nextDouble() - 0.5) * 4.0, 3, 0.1, 0.1, 0.1, 0.05);
			}
			if (level.random.nextFloat() < 0.30f + (float) progress * 0.35f) {
				sendFarParticles(level, ParticleTypes.LAVA, core.x + (level.random.nextDouble() - 0.5) * 3.0, core.y + (level.random.nextDouble() - 0.5) * 3.0, core.z + (level.random.nextDouble() - 0.5) * 3.0, 2, 0.1, 0.1, 0.1, 0.05);
			}
			if (level.random.nextFloat() < 0.20f + (float) progress * 0.30f) {
				sendFarParticles(level, ParticleTypes.FLASH, core.x + (level.random.nextDouble() - 0.5) * 2.0, core.y + (level.random.nextDouble() - 0.5) * 2.0, core.z + (level.random.nextDouble() - 0.5) * 2.0, 1, 0, 0, 0, 0);
			}
			if (deathTicks % 20 == 1) {
				playEngineSound(level, core.x, core.y, core.z, "the_backwoods:fractus_hurt", 24.0F, 0.6F + (float) progress * 0.4F);
				playEngineSound(level, core.x, core.y, core.z, "block.beacon.ambient", 20.0F, 0.4F + (float) progress * 0.6F);
			}
		}

		// 9th Second (Tick 180): Spawn Size 27 Black Hole with duration 60 in the middle of Verdant Engine
		if (deathTicks == 180) {
			spawnDeathBlackHole(level, core.x, core.y, core.z, 27.0f, 60.0f);
			playEngineSound(level, core.x, core.y, core.z, "the_backwoods:blackhole_spawn", 48.0F, 0.7F);
			playEngineSound(level, core.x, core.y, core.z, "block.end_portal.spawn", 48.0F, 0.4F);
			sendCitadelCameraShake(0.8F, 40, 64.0F);
		}

		// 15th Second (Tick 300): Black hole has expanded and engulfed the core -> Seamlessly Despawn Verdant entity
		if (deathTicks >= 300) {
			playEngineSound(level, core.x, core.y, core.z, "entity.generic.explode", 32.0F, 0.6F);
			releaseAllCaughtEntities(level, core, 256.0);
			try {
				net.mcreator.thebackwoods.EntityTrackerData.untrack(entity);
			} catch (Throwable ignored) {}
			entity.discard();
		}
	}

	private static void spawnDeathBlackHole(ServerLevel level, double x, double y, double z, float radius, float durationSeconds) {
		if (level == null) return;
		// 1. Primary: spawn through the dedicated BlackHole system without ground clipping offset
		try {
			net.mcreator.thebackwoods.BlackHole.spawnSingularity(level, x, y, z, radius, durationSeconds, net.mcreator.thebackwoods.BlackHole.MODE_BLACK_HOLE, false);
			return;
		} catch (Throwable directEx) {
			// 2. Fallback: reflection if BlackHole class is relocated
			try {
				String[] candidateClasses = new String[] {
					"net.mcreator.thebackwoods.BlackHole",
					"net.mcreator.thebackwoods.procedures.BlackHole",
					"net.mcreator.thebackwoods.procedures.BlackHoleProcedure",
					"net.mcreator.thebackwoods.procedures.BlackholeProcedure"
				};
				for (String clsName : candidateClasses) {
					try {
						Class<?> bhClass = Class.forName(clsName);
						try {
							java.lang.reflect.Method m = bhClass.getMethod("spawnSingularity", Level.class, double.class, double.class, double.class, float.class, float.class, int.class, boolean.class);
							m.invoke(null, level, x, y, z, radius, durationSeconds, 0, false);
							return;
						} catch (NoSuchMethodException e1) {
							try {
								java.lang.reflect.Method m2 = bhClass.getMethod("spawnBlackHole", Level.class, double.class, double.class, double.class, float.class, int.class);
								m2.invoke(null, level, x, y, z, radius, (int) durationSeconds);
								return;
							} catch (NoSuchMethodException e2) {
								java.lang.reflect.Method m3 = bhClass.getMethod("spawnSingularity", Level.class, double.class, double.class, double.class, float.class, float.class, int.class);
								m3.invoke(null, level, x, y, z, radius, durationSeconds, 0);
								return;
							}
						}
					} catch (ClassNotFoundException ignored) {}
				}
			} catch (Throwable ignoredReflection) {}
		}

		// 3. Fallback: create exactly ONE tagged AreaEffectCloud only if the BlackHole class could not be resolved
		try {
			int totalTicks = Math.max(10, (int) (durationSeconds * 20.0f));
			AreaEffectCloud cloud = new AreaEffectCloud(EntityType.AREA_EFFECT_CLOUD, level);
			cloud.setPos(x, y, z);
			cloud.setRadius(0.0F);
			cloud.setRadiusPerTick(0.0F);
			cloud.setWaitTime(0);
			cloud.setDuration(totalTicks + 200);
			cloud.addTag("BlackHoleEntity");
			cloud.addTag("BH_RAD_" + radius);
			cloud.addTag("BH_MAXTICKS_" + totalTicks);
			cloud.addTag("BH_BLACK_HOLE");
			cloud.setCustomName(net.minecraft.network.chat.Component.literal("BlackHole:" + radius + ":" + totalTicks));
			cloud.setCustomNameVisible(false);
			level.addFreshEntity(cloud);
		} catch (Throwable ignored) {}
	}

	private static boolean isPlayerInParticleRange(ServerLevel level, double x, double z) {
		return isPlayerInParticleRange(level, x, z, GRAVITY_BEAM_PARTICLE_RENDER_DIST_SQ);
	}

	private static boolean isPlayerInParticleRange(ServerLevel level, double x, double z, double maxDistSq) {
		if (level == null) return false;
		double maxDist = Math.sqrt(maxDistSq);
		for (ServerPlayer player : level.players()) {
			double dx = Math.abs(player.getX() - x);
			if (dx > maxDist) continue;
			double dz = Math.abs(player.getZ() - z);
			if (dz > maxDist) continue;
			if (dx * dx + dz * dz <= maxDistSq) return true;
		}
		return false;
	}

	private static boolean isPlayerInParticleRange3D(ServerLevel level, double x, double y, double z, double maxDistSq) {
		if (level == null) return false;
		double maxDist = Math.sqrt(maxDistSq);
		for (ServerPlayer player : level.players()) {
			double dx = Math.abs(player.getX() - x);
			if (dx > maxDist) continue;
			double dz = Math.abs(player.getZ() - z);
			if (dz > maxDist) continue;
			double dy = Math.abs(player.getY() - y);
			if (dy > maxDist) continue;
			if (dx * dx + dy * dy + dz * dz <= maxDistSq) return true;
		}
		return false;
	}

	private static double getNearestPlayerDistanceSq(ServerLevel level, double x, double z) {
		if (level == null) return Double.MAX_VALUE;
		double nearestSq = Double.MAX_VALUE;
		for (ServerPlayer player : level.players()) {
			double dx = player.getX() - x;
			double dz = player.getZ() - z;
			double dSq = dx * dx + dz * dz;
			if (dSq < nearestSq) {
				nearestSq = dSq;
			}
		}
		return nearestSq;
	}

	private static <T extends ParticleOptions> void sendFarParticles(ServerLevel level, T particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
		if (level == null || count <= 0) return;
		boolean isSmoke = particle == ParticleTypes.CAMPFIRE_COSY_SMOKE || particle == ParticleTypes.LARGE_SMOKE || particle == ParticleTypes.SMOKE;
		double maxDist = isSmoke ? 64.0 : GRAVITY_BEAM_PARTICLE_RENDER_DIST;
		double maxDistSq = isSmoke ? (64.0 * 64.0) : GRAVITY_BEAM_PARTICLE_RENDER_DIST_SQ;
		for (ServerPlayer player : level.players()) {
			double diffX = Math.abs(player.getX() - x);
			if (diffX > maxDist) continue;
			double diffZ = Math.abs(player.getZ() - z);
			if (diffZ > maxDist) continue;
			double diffY = Math.abs(player.getY() - y);
			if (diffY > maxDist) continue;

			if (diffX * diffX + diffY * diffY + diffZ * diffZ <= maxDistSq) {
				level.sendParticles(player, particle, true, x, y, z, count, dx, dy, dz, speed);
			}
		}
	}

	private static void handleBoilingCoreHeatAndWaterBubbles(ServerLevel level, Entity engine, Vec3 core) {
		AABB coreBox6 = new AABB(core.x - 3.0, core.y - 3.0, core.z - 3.0, core.x + 3.0, core.y + 3.0, core.z + 3.0);
		boolean inWater = engine.isInWater() || engine.isInWaterOrBubble() || engine.isUnderWater() || level.containsAnyLiquid(coreBox6);

		if (inWater) {
			BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
			for (int i = 0; i < 14; i++) {
				double bx = core.x + (Math.random() - 0.5) * 6.0;
				double by = core.y + (Math.random() - 0.5) * 6.0;
				double bz = core.z + (Math.random() - 0.5) * 6.0;
				mpos.set(Mth.floor(bx), Mth.floor(by), Mth.floor(bz));
				if (level.hasChunkAt(mpos) && level.getFluidState(mpos).is(FluidTags.WATER)) {
					sendFarParticles(level, ParticleTypes.BUBBLE, bx, by, bz, 1, 0, 0.16, 0, 0.05);
					if (Math.random() < 0.3) sendFarParticles(level, ParticleTypes.BUBBLE_COLUMN_UP, bx, by, bz, 1, 0, 0.22, 0, 0.06);
					if (Math.random() < 0.12) sendFarParticles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, bx, by, bz, 1, 0, 0.06, 0, 0.01);
				}
			}

			for (int i = 0; i < 6; i++) {
				double px = core.x + ((i % 2 == 0 ? 1 : -1) * 3.0), py = core.y + (Math.random() - 0.5) * 6.0, pz = core.z + (Math.random() - 0.5) * 6.0;
				if (i >= 2 && i < 4) { double t = px; px = py; py = t; }
				else if (i >= 4) { double t = px; px = pz; pz = t; }
				mpos.set(Mth.floor(px), Mth.floor(py), Mth.floor(pz));
				if (level.hasChunkAt(mpos) && level.getFluidState(mpos).is(FluidTags.WATER)) {
					sendFarParticles(level, ParticleTypes.BUBBLE, px, py, pz, 1, 0, 0.14, 0, 0.04);
				}
			}

			if (level.getGameTime() % 8 == 0) playEngineSound(level, core.x, core.y, core.z, "block.bubble_column.bubble_pop", 14.0F, 1.1F + (float) Math.random() * 0.4F);
			if (level.getGameTime() % 16 == 0) playEngineSound(level, core.x, core.y, core.z, "block.fire.extinguish", 10.0F, 1.4F + (float) Math.random() * 0.3F);
		}

		AABB heatBox = new AABB(core.x - 6.5, core.y - 6.5, core.z - 6.5, core.x + 6.5, core.y + 6.5, core.z + 6.5);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, heatBox, e -> e != engine && e.isAlive())) {
			if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
			if (isWoodbound(victim)) continue;
			if (victim.distanceToSqr(core) <= 42.25) {
				victim.setRemainingFireTicks(40);
				if (victim.isInWater() && level.getGameTime() % 10 == 0) victim.hurt(victim.damageSources().hotFloor(), 2.0F);
				if (level.getGameTime() % 4 == 0) sendFarParticles(level, ParticleTypes.FLAME, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 2, 0.12, 0.12, 0.12, 0.02);
			}
		}
	}

	private static boolean isInsideOrganicHitboxClearing(AABB box, double cx, double cz, int x, int y, int z, double organicMargin) {
		double bx = x + 0.5, bz = z + 0.5;
		double ox = 0.0;
		if (bx < box.minX) ox = box.minX - bx;
		else if (bx > box.maxX) ox = bx - box.maxX;

		double oz = 0.0;
		if (bz < box.minZ) oz = box.minZ - bz;
		else if (bz > box.maxZ) oz = bz - box.maxZ;

		double distToBox = Math.sqrt(ox * ox + oz * oz);
		if (distToBox <= organicMargin) return true;
		if (distToBox > organicMargin + 1.2) return false;

		double angle = Math.atan2(bz - cz, bx - cx);
		double organicOffset = Math.sin(angle * 3.0 + y * 0.5) * 0.45 + Math.cos(angle * 5.0 - y * 0.3) * 0.35;
		double voxelJitter = Math.sin(x * 12.3 + z * 37.7 + y * 19.1) * 0.25;
		return distToBox <= (organicMargin + Math.max(0.0, organicOffset + voxelJitter));
	}

	private static void carveOpenedBodyEncroachment(ServerLevel level, Entity entity, double organicMargin) {
		AABB box = entity.getBoundingBox();
		double cx = entity.getX(), cz = entity.getZ();
		int clearBottom = Math.max(level.getMinBuildHeight() + 1, Mth.floor(box.minY - 1.5));
		int clearTop = Math.min(level.getMaxBuildHeight() - 1, Mth.floor(box.maxY + 3.5));
		int minX = Mth.floor(box.minX - (organicMargin + 1.2));
		int maxX = Mth.floor(box.maxX + (organicMargin + 1.2));
		int minZ = Mth.floor(box.minZ - (organicMargin + 1.2));
		int maxZ = Mth.floor(box.maxZ + (organicMargin + 1.2));
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int y = clearBottom; y <= clearTop; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (isInsideOrganicHitboxClearing(box, cx, cz, x, y, z, organicMargin)) {
						pos.set(x, y, z);
						if (!level.hasChunkAt(pos)) continue;
						BlockState st = level.getBlockState(pos);
						if (st.isAir()) continue;
						if (isIndestructibleBlock(st, level, pos)) continue;
						level.destroyBlock(pos, false);
						if (!level.getBlockState(pos).isAir()) {
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
						}
					}
				}
			}
		}
	}

	private static void spawnChargingCorona(ServerLevel level, Vec3 center, double prog, double quality) {
		if (quality <= 0.01 || !isPlayerInParticleRange(level, center.x, center.z)) return;
		int count = Math.max(1, (int) ((10 + prog * 12) * quality));
		double r = 1.0 + (1.0 - prog) * 1.4;
		for (int i = 0; i < count; i++) {
			double th = Math.random() * Math.PI * 2, ph = Math.random() * Math.PI;
			double px = center.x + r * Math.sin(ph) * Math.cos(th), py = center.y + r * Math.cos(ph), pz = center.z + r * Math.sin(ph) * Math.sin(th);
			sendFarParticles(level, BEAM_WHITE_DUST, px, py, pz, 1, 0, 0, 0, 0);
			if (i % 2 == 0) sendFarParticles(level, ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0);
			if (i % 3 == 0) sendFarParticles(level, ParticleTypes.END_ROD, px, py, pz, 1, 0, 0, 0, 0.02);
		}
	}

	private static void renderToweringBeam(ServerLevel level, Vec3 origin, double groundY, double r, double densityFactor, double quality) {
		if (net.mcreator.thebackwoods.VerdantEngineGravityBeam.MASTER_ENABLED) return;
		double h = origin.y - groundY;
		if (h <= 0 || quality <= 0.01) return;

		long gameTick = level.getGameTime();
		double densityBoost = 1.0 + (densityFactor * 2.0);
		int coreSteps = Math.max(2, (int) (h * 1.0 * densityBoost * quality));

		for (int i = 0; i < coreSteps; i++) {
			double y = origin.y - (i / (double) coreSteps) * h;
			sendFarParticles(level, BEAM_WHITE_DUST, origin.x, y, origin.z, 1, 0, 0, 0, 0);
			if (i % 2 == 0 || densityFactor > 0.45) {
				sendFarParticles(level, ParticleTypes.FLASH, origin.x, y, origin.z, 1, 0, 0, 0, 0);
			}
			if (i % 3 == 0 || densityFactor > 0.60) {
				sendFarParticles(level, ParticleTypes.END_ROD, origin.x, y, origin.z, 1, 0.02, 0.02, 0.02, 0.01 + densityFactor * 0.02);
			}
		}

		int ringCoils = densityFactor > 0.4 ? 6 : 4;
		int ringSteps = Math.max(2, (int) (h * 0.70 * (0.8 + densityFactor * 1.0) * quality));

		for (int s = 0; s < ringSteps; s++) {
			double y = origin.y - (s / (double) ringSteps) * h;
			for (int p = 0; p < ringCoils; p++) {
				double a = ((2 * Math.PI) / ringCoils) * p + (y * (0.35 + densityFactor * 0.2)) + (gameTick * (0.12 + densityFactor * 0.25));
				double dynamicR = r * (0.8 + Math.sin(y * 0.2 + gameTick * 0.15) * 0.25 + densityFactor * 0.45);
				double px = origin.x + Math.cos(a) * dynamicR, pz = origin.z + Math.sin(a) * dynamicR;
				sendFarParticles(level, BEAM_CYAN_DUST, px, y, pz, 1, 0, 0, 0, 0);
				if (densityFactor > 0.45 && s % 2 == 0) {
					sendFarParticles(level, RING_ENERGY_DUST, px, y, pz, 1, 0, 0, 0, 0);
				}
			}
		}

		int groundSmoke = (int) ((2 + densityFactor * 3) * quality);
		for (int i = 0; i < groundSmoke; i++) {
			double a = Math.random() * Math.PI * 2, d = Math.random() * (r + 0.8 + densityFactor * 2.0);
			sendFarParticles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, origin.x + Math.cos(a) * d, groundY + 0.2, origin.z + Math.sin(a) * d, 1, Math.cos(a) * (0.15 + densityFactor * 0.25), 0.05 + densityFactor * 0.08, Math.sin(a) * (0.15 + densityFactor * 0.25), 0.02);
		}
	}

	private static void crumbleThinPillarsUnderBeam(ServerLevel level, Vec3 origin, double groundY, double r) {
		if (level.getGameTime() % 4 != 0) return;
		int centerX = Mth.floor(origin.x);
		int centerZ = Mth.floor(origin.z);
		int topY = Mth.floor(groundY);

		BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				double distSq = dx * dx + dz * dz;
				if (distSq > 0 && Math.random() > 0.85) continue;

				int bx = centerX + dx;
				int bz = centerZ + dz;

				for (int y = topY + 2; y >= topY - 10; y--) {
					scanPos.set(bx, y, bz);
					if (!level.hasChunkAt(scanPos)) continue;
					BlockState state = level.getBlockState(scanPos);
					if (!state.isAir() && !state.getCollisionShape(level, scanPos).isEmpty() && state.getBlock() != Blocks.BEDROCK) {
						int lowerNeighbors = 0;
						for (int nx = -1; nx <= 1; nx++) {
							for (int nz = -1; nz <= 1; nz++) {
								if (nx == 0 && nz == 0) continue;
								neighborPos.set(bx + nx, y, bz + nz);
								BlockState nState = level.getBlockState(neighborPos);
								boolean isOpen = nState.isAir() || nState.getCollisionShape(level, neighborPos).isEmpty();
								if (isOpen) {
									neighborPos.set(bx + nx, y - 1, bz + nz);
									BlockState nStateDown = level.getBlockState(neighborPos);
									if (nStateDown.isAir() || nStateDown.getCollisionShape(level, neighborPos).isEmpty()) {
										lowerNeighbors++;
									}
								}
							}
						}

						if (lowerNeighbors >= 4 || (dx == 0 && dz == 0 && lowerNeighbors >= 3)) {
							level.destroyBlock(scanPos, false);
							sendFarParticles(level, ParticleTypes.POOF, bx + 0.5, y + 0.5, bz + 0.5, 3, 0.3, 0.3, 0.3, 0.05);
							playEngineSound(level, bx + 0.5, y + 0.5, bz + 0.5, "minecraft:block.stone.break", 0.8f, 0.7f);
							break;
						}
					}
				}
			}
		}
	}

	private static void carveObstructingCanopy(ServerLevel level, Vec3 origin, double groundY, double r) {
		if (level.getGameTime() % 6 != 0) return;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int minY = Mth.floor(groundY), maxY = Mth.floor(origin.y);
		for (int s = 0; s < 6; s++) {
			double rad = Math.random() * r, ang = Math.random() * Math.PI * 2;
			pos.set(Mth.floor(origin.x + Math.cos(ang) * rad), minY + (int) (Math.random() * (maxY - minY + 1)), Mth.floor(origin.z + Math.sin(ang) * rad));
			if (level.hasChunkAt(pos)) {
				BlockState st = level.getBlockState(pos);
				if (st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || st.is(BlockTags.FLOWERS) || st.is(BlockTags.SAPLINGS)) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	private static void spawnPreSlamOnionRings(ServerLevel level, Vec3 origin, double groundY, double beamR, double chargeProg, double ringTravel, double quality) {
		if (net.mcreator.thebackwoods.VerdantEngineGravityBeam.MASTER_ENABLED) return;
		double h = origin.y - groundY;
		if (h <= 0 || quality <= 0.01) return;

		double corePulse = (ringTravel * 2.0) % 1.0;
		double coreRingR = (beamR * 0.45) + corePulse * (beamR * 0.5);
		int coreCount = Math.max(3, (int) (2 * Math.PI * coreRingR * 1.0 * quality));
		for (int i = 0; i < coreCount; i++) {
			double a = (2 * Math.PI / coreCount) * i;
			double px = origin.x + Math.cos(a) * coreRingR;
			double pz = origin.z + Math.sin(a) * coreRingR;
			sendFarParticles(level, RING_ENERGY_DUST, px, origin.y, pz, 1, 0, 0, 0, 0);
		}

		int totalTiers = quality < 0.4 ? 2 : 3;
		double rotAngle = ringTravel * Math.PI * 4.0;
		for (int tier = 0; tier < totalTiers; tier++) {
			double phase = (ringTravel + (tier / (double) totalTiers)) % 1.0;
			double ringY = origin.y - phase * h;

			double oldInR = beamR + 0.35 + (1.0 - phase) * 1.2;
			double birthGrowth = Math.min(1.0, phase * 4.0);
			double smoothGrowth = Math.sin(birthGrowth * Math.PI * 0.5);

			double inR = (beamR * 0.4) + (oldInR - beamR * 0.4) * smoothGrowth;
			double outR = inR + (0.7 * smoothGrowth);

			int count = Math.max(4, (int) (2 * Math.PI * outR * 1.2 * quality));
			for (int i = 0; i < count; i++) {
				double a = (2 * Math.PI / count) * i + rotAngle;
				double c = Math.cos(a), s = Math.sin(a);
				sendFarParticles(level, RING_ENERGY_DUST, origin.x + c * inR, ringY, origin.z + s * inR, 1, 0, 0, 0, 0);
				sendFarParticles(level, RING_ENERGY_DUST, origin.x + c * outR, ringY, origin.z + s * outR, 1, 0, 0, 0, 0);
				if (chargeProg > 0.45 && i % 2 == 0) {
					double midR = (inR + outR) * 0.5;
					sendFarParticles(level, BEAM_CYAN_DUST, origin.x + c * midR, ringY, origin.z + s * midR, 1, 0, 0, 0, 0);
				}
				if (i % 4 == 0) {
					sendFarParticles(level, ParticleTypes.FLASH, origin.x + c * inR, ringY, origin.z + s * inR, 1, 0, 0, 0, 0);
				}
				if (i % 6 == 0) {
					sendFarParticles(level, ParticleTypes.END_ROD, origin.x + c * outR, ringY, origin.z + s * outR, 1, 0, 0, 0, 0.02);
				}
			}
		}
	}

	private static boolean isSuspectedProjectile(Entity e) {
		String n = e.getClass().getSimpleName().toLowerCase();
		return n.contains("bullet") || n.contains("projectile") || n.contains("ammo");
	}

	private static void handleGravityDistortionAndProjectiles(ServerLevel level, Entity engine, Vec3 core, double groundY, double gravR, int cycle) {
		for (ServerPlayer sp : level.players()) {
			if (sp.getPersistentData().getBoolean("verdant_lifted_by_zero_g")) {
				long lastLift = sp.getPersistentData().getLong("verdant_last_lift_tick");
				if (sp.isCreative() || sp.isSpectator() || level.getGameTime() - lastLift > 2) {
					sp.setNoGravity(false);
					sp.getPersistentData().remove("verdant_lifted_by_zero_g");
					sp.getPersistentData().remove("verdant_last_lift_tick");
				}
			}
		}

		int burstTicks = engine.getPersistentData().getInt("verdant_pound_burst_ticks");
		if (burstTicks > 0) {
			engine.getPersistentData().putInt("verdant_pound_burst_ticks", burstTicks - 1);
		}

		double defenseSphereR = 48.0;
		AABB defenseBox = new AABB(core.x - defenseSphereR, core.y - defenseSphereR, core.z - defenseSphereR, core.x + defenseSphereR, core.y + defenseSphereR, core.z + defenseSphereR);
		for (Entity ent : level.getEntitiesOfClass(Entity.class, defenseBox, e -> (e instanceof Projectile) || (e != null && !(e instanceof Player) && isSuspectedProjectile(e)))) {
			if (ent.isAlive() && ent != engine) {
				double dist = ent.position().distanceTo(core);
				if (dist <= defenseSphereR) {
					String entName = ent.getClass().getSimpleName().toLowerCase();
					// Passive Defense 1: Spacetime Curvature on Ender Pearls and Wind Charges
					if (entName.contains("enderpearl") || entName.contains("thrownenderpearl") || entName.contains("windcharge")) {
						ent.setDeltaMovement(0.0, -1.8, 0.0);
						ent.hasImpulse = true;
						playEngineSound(level, ent.getX(), ent.getY(), ent.getZ(), "entity.ender_eye.death", 0.9F, 0.6F);
						continue;
					}

					double drag;
					if (dist <= 4.0) {
						// Absolute close-quarters deflection barrier (< 4.0 blocks): collapse projectile velocity entirely
						drag = 0.01;
						ent.setNoGravity(false);
						ent.setDeltaMovement(0.0, -0.6, 0.0);
						ent.hasImpulse = true;
						if (Math.random() < 0.25) {
							sendFarParticles(level, ParticleTypes.POOF, ent.getX(), ent.getY(), ent.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
						}
						continue;
					} else if (dist <= 12.0) {
						// Extreme dampening sphere (4.0 to 12.0 blocks): 95% to 82% velocity drain
						double t = (dist - 4.0) / 8.0;
						drag = 0.05 + t * 0.13;
						ent.setNoGravity(true);
					} else if (dist <= 24.0) {
						double t = (dist - 12.0) / 12.0;
						drag = 0.18 + t * 0.32;
					} else {
						double t = (dist - 24.0) / 24.0;
						drag = 0.50 + t * 0.35;
					}
					if (burstTicks > 0) {
						drag *= 0.10;
					}
					Vec3 vel = ent.getDeltaMovement();
					// Hard clamp speed on fast modded projectiles penetrating field
					double currentSpeed = vel.length();
					double maxAllowedSpeed = Math.max(0.08, dist * 0.04);
					if (currentSpeed > maxAllowedSpeed) {
						vel = vel.scale(maxAllowedSpeed / currentSpeed);
					}

					double vx = vel.x * drag;
					double vy = vel.y * drag;
					double vz = vel.z * drag;

					// Passive Defense 5: Coriolis Projectile Deflection (tangential orbital nudge)
					if (dist > 7.0 && dist <= 36.0 && (Math.abs(vel.x) + Math.abs(vel.z) > 0.05)) {
						Vec3 toCenter = core.subtract(ent.position()).normalize();
						Vec3 tangent = new Vec3(-toCenter.z, 0.0, toCenter.x);
						double nudgeStrength = (1.0 - (dist / 36.0)) * 0.08;
						vx += tangent.x * nudgeStrength;
						vz += tangent.z * nudgeStrength;
					}

					ent.setDeltaMovement(vx, vy, vz);
					ent.hasImpulse = true;
				}
			}
		}

		double queryR = Math.max(defenseSphereR, Math.max(gravR, 48.0));
		AABB livingBox = new AABB(core.x - queryR, groundY + ZERO_G_LIFT_VERTICAL_MIN, core.z - queryR, core.x + queryR, core.y + ZERO_G_LIFT_VERTICAL_MAX, core.z + queryR);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, livingBox, e -> e != engine && e.isAlive())) {
			if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) {
				try {
					var gravAttr = p.getAttribute(Attributes.GRAVITY);
					if (gravAttr != null) {
						gravAttr.removeModifier(VERDANT_BEAM_GRAV_MOD_ID);
					}
				} catch (Throwable ignored) {}
				GRAVITY_MODIFIED_ENTITIES.remove(p.getUUID());
				if (victim.getPersistentData().getBoolean("verdant_lifted_by_zero_g") || victim.isNoGravity()) {
					victim.setNoGravity(false);
					victim.getPersistentData().remove("verdant_lifted_by_zero_g");
					victim.getPersistentData().remove("verdant_last_lift_tick");
				}
				continue;
			}
			if (isWoodbound(victim)) continue;

			int slammed = victim.getPersistentData().getInt("verdant_slammed_ticks");
			if (slammed > 0) {
				victim.getPersistentData().putInt("verdant_slammed_ticks", slammed - 1);
				victim.setNoGravity(false);
				if (victim instanceof Player p) {
					p.stopFallFlying();
				}
				if (!victim.onGround()) {
					victim.setDeltaMovement(victim.getDeltaMovement().x * 0.25, Math.min(-3.8, victim.getDeltaMovement().y - 0.65), victim.getDeltaMovement().z * 0.25);
					victim.hasImpulse = true;
					victim.hurtMarked = true;
					if (victim instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
				} else {
					victim.getPersistentData().putInt("verdant_slammed_ticks", 0);
					applyTruePoundDamage(level, victim, engine, GRAVITY_SLAM_IMPACT_DAMAGE);
					victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, false, true));
					playEngineSound(level, victim.getX(), victim.getY(), victim.getZ(), "item.mace.smash_ground_heavy", 1.4F, 0.6F);
				}
				continue;
			}

			double distToCore = victim.position().distanceTo(core);

			// Passive Defense 2: Trident Riptide Cavitation & Kinetic Stall
			if (distToCore <= defenseSphereR && victim.isAutoSpinAttack()) {
				victim.setDeltaMovement(victim.getDeltaMovement().scale(0.15));
				victim.hasImpulse = true;
				victim.hurtMarked = true;
				playEngineSound(level, victim.getX(), victim.getY(), victim.getZ(), "item.trident.riptide_1", 1.2F, 0.4F);
				playEngineSound(level, victim.getX(), victim.getY(), victim.getZ(), "entity.generic.extinguish_fire", 1.0F, 0.6F);
			}

			// Passive Defense 4: Barometric Core Proximity Asphyxiation (< 8.0 blocks)
			if (distToCore <= 8.0) {
				int air = victim.getAirSupply();
				if (air > -20) {
					victim.setAirSupply(Math.max(-20, air - 6));
					if (air <= 0 && victim.tickCount % 20 == 0) {
						applyTruePoundDamage(level, victim, engine, CORE_ASPHYXIATION_DAMAGE);
						playEngineSound(level, victim.getX(), victim.getY(), victim.getZ(), "entity.player.hurt_drown", 0.9F, 1.0F);
					}
				}
			}

			if (distToCore <= defenseSphereR && victim.isFallFlying()) {
				if (victim instanceof Player p) {
					p.stopFallFlying();
				}
				net.minecraft.world.item.ItemStack chestItem = victim.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
				if (!chestItem.isEmpty() && (chestItem.is(net.minecraft.world.item.Items.ELYTRA) || chestItem.getItem().getDescriptionId().contains("elytra"))) {
					int elytraDmg = 25;
					if (victim instanceof ServerPlayer sp) {
						chestItem.hurtAndBreak(elytraDmg, level, sp, item -> {});
					} else {
						chestItem.hurtAndBreak(elytraDmg, level, null, item -> {});
					}
					playEngineSound(level, victim.getX(), victim.getY(), victim.getZ(), "entity.item.break", 1.2F, 0.85F);
				}
				for (Entity pr : level.getEntitiesOfClass(Entity.class, victim.getBoundingBox().inflate(4.0), p -> p instanceof Projectile && p.getClass().getSimpleName().toLowerCase().contains("firework"))) {
					pr.discard();
					playEngineSound(level, pr.getX(), pr.getY(), pr.getZ(), "entity.firework_rocket.blast", 1.2F, 0.6F);
				}
				Vec3 m = victim.getDeltaMovement();
				double incomingSpeed = m.length();
				double downSlam = Math.min(-2.6, -1.3 - (incomingSpeed * 1.5));
				victim.setDeltaMovement(m.x * 0.08, downSlam, m.z * 0.08);
				victim.hasImpulse = true;
				victim.hurtMarked = true;
				victim.getPersistentData().putInt("verdant_slammed_ticks", 35);
				playEngineSound(level, victim.getX(), victim.getY(), victim.getZ(), "entity.warden.sonic_boom", 1.4F, 0.55F);
				playEngineSound(level, victim.getX(), victim.getY(), victim.getZ(), "entity.wind_charge.wind_burst", 1.6F, 0.4F);
				if (victim instanceof ServerPlayer sp) {
					sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
				}
				continue;
			}

			double dx = core.x - victim.getX();
			double dz = core.z - victim.getZ();
			double horizDist = Math.sqrt(dx * dx + dz * dz);
			boolean inZeroGLift = (cycle >= RINGS_START_TICK && cycle < CYCLE_TOTAL_TICKS);

			// Gravity Beam Proximity Attribute Dynamic Linear Scaling
			// Strictly fixed range around the beam column; does not expand with pounds, infection, or terraforming
			double effectRadius = GRAVITY_BEAM_EFFECT_RADIUS;
			double maxBonus = getGravityBeamMaxModifier(engine != null ? engine.getPersistentData() : null);

			// Exclude non-survival players (Creative & Spectator) from receiving gravity modifications
			if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) {
				try {
					var gravAttr = p.getAttribute(Attributes.GRAVITY);
					if (gravAttr != null) {
						gravAttr.removeModifier(VERDANT_BEAM_GRAV_MOD_ID);
					}
				} catch (Throwable ignored) {}
				GRAVITY_MODIFIED_ENTITIES.remove(victim.getUUID());
			} else if (horizDist <= effectRadius && victim.getY() <= core.y + ZERO_G_LIFT_MAX_Y_CAP && victim.getY() >= groundY + ZERO_G_LIFT_VERTICAL_MIN) {
				double proximityFactor = Math.max(0.0, Math.min(1.0, 1.0 - (horizDist / effectRadius))); // 0.0 at boundary, 1.0 at epicenter
				double gravityAdd = proximityFactor * maxBonus;
				try {
					var gravAttr = victim.getAttribute(Attributes.GRAVITY);
					if (gravAttr != null) {
						gravAttr.removeModifier(VERDANT_BEAM_GRAV_MOD_ID);
						if (gravityAdd > 0.005) {
							gravAttr.addTransientModifier(new AttributeModifier(VERDANT_BEAM_GRAV_MOD_ID, gravityAdd, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
							GRAVITY_MODIFIED_ENTITIES.put(victim.getUUID(), level.getGameTime());
						}
					}
				} catch (Throwable ignored) {}
			} else {
				try {
					var gravAttr = victim.getAttribute(Attributes.GRAVITY);
					if (gravAttr != null) {
						gravAttr.removeModifier(VERDANT_BEAM_GRAV_MOD_ID);
					}
				} catch (Throwable ignored) {}
				GRAVITY_MODIFIED_ENTITIES.remove(victim.getUUID());
			}

			if (inZeroGLift && horizDist <= Math.max(gravR, 24.0) && victim.getY() <= core.y + ZERO_G_LIFT_MAX_Y_CAP) {
				victim.setNoGravity(true);
				victim.getPersistentData().putBoolean("verdant_lifted_by_zero_g", true);
				victim.getPersistentData().putLong("verdant_last_lift_tick", level.getGameTime());
				victim.fallDistance = 0.0F;
				victim.setOnGround(false);

				double chargeProg = (double) (cycle - RINGS_START_TICK) / (CYCLE_TOTAL_TICKS - RINGS_START_TICK);
				double baseLift = 0.12;
				double accel = Math.pow(chargeProg, 1.6) * 0.95;
				double liftSpeed = baseLift + accel;

				if (victim.getY() >= core.y - 2.0) {
					liftSpeed = Math.max(0.04, (core.y - victim.getY()) * 0.18);
				}

				Vec3 m = victim.getDeltaMovement();
				double driftX = m.x * 0.88 + Math.cos((level.getGameTime() + victim.getId() * 5) * 0.08) * 0.015;
				double driftZ = m.z * 0.88 + Math.sin((level.getGameTime() + victim.getId() * 5) * 0.08) * 0.015;

				victim.setDeltaMovement(driftX, liftSpeed, driftZ);
				victim.hasImpulse = true;
				victim.hurtMarked = true;
				if (victim instanceof ServerPlayer sp) {
					sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
				}
			} else {
				if (victim.getPersistentData().getBoolean("verdant_lifted_by_zero_g")) {
					victim.setNoGravity(false);
					victim.getPersistentData().remove("verdant_lifted_by_zero_g");
					victim.getPersistentData().remove("verdant_last_lift_tick");
				} else if (victim.isNoGravity() && slammed <= 0) {
					victim.setNoGravity(false);
				}
				if (distToCore <= 48.0) {
					if (distToCore <= 16.0 && !inZeroGLift) {
						Vec3 delta = victim.position().subtract(core);
						double push = (16.0 - distToCore) / 16.0;
						Vec3 pushVec = new Vec3(delta.x, delta.y * 0.25, delta.z).normalize().scale(push * 0.16);
						victim.push(pushVec.x, pushVec.y, pushVec.z);
					}
					double drag = distToCore <= 12.0 ? 0.30 : (distToCore <= 24.0 ? 0.48 : (distToCore <= 36.0 ? 0.65 : 0.78));
					Vec3 m = victim.getDeltaMovement();
					double yMotion = m.y;
					if (burstTicks > 0) {
						drag *= 0.08;
						yMotion = Math.min(yMotion, yMotion * 0.08);
					} else if (yMotion < 0 && !inZeroGLift) {
						yMotion *= drag;
					} else if (yMotion > 0 && !inZeroGLift && distToCore <= 24.0) {
						// Passive Defense 3: High-G Jump Suppression (Micro-Gravity Ground Lock)
						yMotion *= 0.50;
					}
					victim.setDeltaMovement(m.x * drag, yMotion, m.z * drag);
					victim.hasImpulse = true;
					if (victim instanceof ServerPlayer sp) {
						sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
					}
				}
			}
		}

		// Automatic expiration pass: Ensure that the gravity attribute modification is 100% temporary
		// Any entity that moved out of range, exited the query box, died, or disconnected is cleaned up immediately
		long now = level.getGameTime();
		for (java.util.Map.Entry<UUID, Long> entry : GRAVITY_MODIFIED_ENTITIES.entrySet()) {
			if (entry.getValue() < now) {
				Entity tracked = level.getEntity(entry.getKey());
				if (tracked instanceof LivingEntity living) {
					try {
						var attr = living.getAttribute(Attributes.GRAVITY);
						if (attr != null) {
							attr.removeModifier(VERDANT_BEAM_GRAV_MOD_ID);
						}
					} catch (Throwable ignored) {}
				}
				GRAVITY_MODIFIED_ENTITIES.remove(entry.getKey());
			}
		}
	}

	private static DamageSource getVerdantDamage(ServerLevel level, Entity engine) {
		String[] candidateIds = new String[] {
			"the_backwoods:verdant_engine_gravity_beam_damage",
			"thebackwoods:verdant_engine_gravity_beam_damage",
			"the_backwoods:verdant_engine_gravity_beam",
			"thebackwoods:verdant_engine_gravity_beam"
		};
		var reg = level.registryAccess().registry(Registries.DAMAGE_TYPE).orElse(null);
		if (reg != null) {
			for (String id : candidateIds) {
				ResourceLocation loc = ResourceLocation.tryParse(id);
				if (loc != null && reg.containsKey(loc)) {
					var holder = reg.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, loc)).orElse(null);
					if (holder != null) {
						return new DamageSource(holder, engine);
					}
				}
			}
		}
		for (String id : candidateIds) {
			try {
				ResourceLocation loc = ResourceLocation.parse(id);
				ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, loc);
				return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), engine);
			} catch (Exception ignored) {}
		}
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC), engine);
	}

	public static float calculateDamageAfterArmorAndEffects(LivingEntity victim, float rawDamage) {
		float armor = (float) victim.getArmorValue();
		float toughness = (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
		float f = 2.0F + toughness / 4.0F;
		float f1 = Mth.clamp(armor - rawDamage / f, armor * 0.2F, 20.0F);
		float dmg = rawDamage * (1.0F - f1 / 25.0F);

		if (victim.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
			int amplifier = victim.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1;
			dmg = Math.max(0.0F, dmg * (1.0F - (amplifier * 0.20F)));
		}
		return Math.max(0.0F, dmg);
	}

	public static void applyTruePoundDamage(ServerLevel level, LivingEntity victim, Entity engine, float rawDamage) {
		if (victim == null || !victim.isAlive()) return;
		if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) return;
		if (isWoodbound(victim)) return;

		// 1. Reset invulnerable tick cooldown so entity cannot evade pound damage via i-frames
		victim.invulnerableTime = 0;

		DamageSource dmg = getVerdantDamage(level, engine != null ? engine : victim);

		// 2. Trigger standard damage pipeline for entity animations, knockback events, and durability/shield logic
		float initialHp = victim.getHealth();
		try {
			victim.hurt(dmg, rawDamage);
		} catch (Throwable ignored) {}

		// 3. Fallback bypass: If entity has immunity tags (e.g., player-only damage requirement, damage type blacklist, or i-frame lock)
		// and took zero damage, compute armor + toughness + Resistance + Absorption values and apply directly to health
		if (victim.getHealth() >= initialHp && victim.isAlive()) {
			float calculatedDmg = calculateDamageAfterArmorAndEffects(victim, rawDamage);
			float absorption = victim.getAbsorptionAmount();
			if (absorption > 0.0F) {
				if (absorption >= calculatedDmg) {
					victim.setAbsorptionAmount(absorption - calculatedDmg);
					calculatedDmg = 0.0F;
				} else {
					victim.setAbsorptionAmount(0.0F);
					calculatedDmg -= absorption;
				}
			}

			if (calculatedDmg > 0.0F) {
				victim.setHealth(Math.max(0.0F, victim.getHealth() - calculatedDmg));
				if (victim.getHealth() <= 0.0F) {
					victim.die(dmg);
				}
			}
			victim.hurtMarked = true;
		}

		// 4. Clear i-frame timer post-strike so successive multi-phase impacts are never swallowed
		victim.invulnerableTime = 0;
	}

	private static void triggerPlanetaryPound(ServerLevel level, Entity engine, double ix, double gy, double iz, double poundR, int poundCount, boolean terraform) {
		double lockedY = engine.getPersistentData().getDouble("verdant_locked_y");
		if (lockedY <= 0) lockedY = gy + HOVER_TARGET_ALTITUDE;

		playEngineSound(level, ix, gy, iz, "entity.warden.sonic_boom", 36.0F, 0.01F);
		playEngineSound(level, ix, gy, iz, "entity.generic.explode", 36.0F, 0.01F);
		playEngineSound(level, ix, gy, iz, "item.mace.smash_ground_heavy", 36.0F, 0.01F);
		playEngineSound(level, ix, gy, iz, "entity.elder_guardian.curse", 32.0F, 0.01F);
		playEngineSound(level, ix, gy, iz, "entity.wither.spawn", 30.0F, 0.01F);
		playEngineSound(level, ix, gy, iz, "entity.lightning_bolt.impact", 28.0F, 0.01F);
		playEngineSound(level, ix, gy, iz, "entity.iron_golem.attack", 26.0F, 0.01F);
		playEngineSound(level, ix, gy, iz, "entity.wither.break_block", 24.0F, 0.01F);

		sendCitadelCameraShakeAt(level, ix, gy, iz, 8.5F + (poundCount * 1.2F), 65 + (poundCount * 8), 48.0F);

		sendFarParticles(level, ParticleTypes.EXPLOSION_EMITTER, ix, gy + 0.5, iz, 4, 1.0, 0.3, 1.0, 0);
		sendFarParticles(level, ParticleTypes.FLASH, ix, gy + 0.5, iz, 2, 0.4, 0.4, 0.4, 0);

		double maxY = lockedY + 32.0;
		AABB poundArea = new AABB(ix - poundR, gy - 12.0, iz - poundR, ix + poundR, maxY, iz + poundR);
		for (Entity p : level.getEntitiesOfClass(Entity.class, poundArea, e -> e instanceof Projectile || e.getClass().getSimpleName().toLowerCase().contains("bullet") || e.getClass().getSimpleName().toLowerCase().contains("projectile") || e.getClass().getSimpleName().toLowerCase().contains("ammo"))) {
			if (p.isAlive() && p != engine) {
				p.setNoGravity(false);
				p.setDeltaMovement(p.getDeltaMovement().x * 0.05, -12.0, p.getDeltaMovement().z * 0.05);
				p.hasImpulse = true;
			}
		}

		float slamDmg = PLANETARY_POUND_BASE_DAMAGE + (poundCount * PLANETARY_POUND_PER_POUND_DAMAGE);

		java.util.List<Long> hitIds = new java.util.ArrayList<>();
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, poundArea, e -> e != engine && e.isAlive())) {
			if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
			if (isWoodbound(victim)) continue;

			victim.setNoGravity(false);
			victim.getPersistentData().putInt("verdant_slammed_ticks", 50);
			victim.setDeltaMovement(victim.getDeltaMovement().x * 0.02, -12.0, victim.getDeltaMovement().z * 0.02);
			victim.hurtMarked = true;
			victim.hasImpulse = true;
			victim.fallDistance += 16.0F;
			applyTruePoundDamage(level, victim, engine, slamDmg);
			victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4, false, false, true));
			victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2, false, false, true));
			if (victim instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));

			hitIds.add((long) victim.getId());
		}

		CompoundTag nbt = engine.getPersistentData();
		nbt.putInt("verdant_pound_burst_ticks", 20);

		// Cull shockwave if impact point is buried deep underground (solid block 2 blocks above impact)
		BlockPos impactHeadPos = BlockPos.containing(ix, gy + 2.0, iz);
		if (level.hasChunkAt(impactHeadPos) && !level.getBlockState(impactHeadPos).isAir() && level.getBlockState(impactHeadPos).isRedstoneConductor(level, impactHeadPos)) {
			nbt.putDouble("verdant_shockwave_stage", 0);
		} else {
			nbt.putDouble("verdant_shockwave_stage", 1.0);
		}

		nbt.putDouble("verdant_shockwave_ox", ix);
		nbt.putDouble("verdant_shockwave_oy", gy);
		nbt.putDouble("verdant_shockwave_oz", iz);
		nbt.putDouble("verdant_shockwave_max_r", Math.min(getShockwaveParticleRadius(nbt), poundR));
		nbt.putFloat("verdant_shockwave_dmg", slamDmg);

		long[] hitIdArray = new long[hitIds.size()];
		for (int i = 0; i < hitIds.size(); i++) {
			hitIdArray[i] = hitIds.get(i);
		}
		nbt.putLongArray("verdant_shockwave_hit_ids", hitIdArray);

		double curDepth = persistentDouble(nbt, "verdant_crater_depth", 0.0);
		double penSpeed = getGravityBeamPenetrationSpeed(nbt);
		nbt.putDouble("verdant_crater_depth", Math.min(128.0, curDepth + (1.8 + (poundCount * 0.3)) * penSpeed));

		if (terraform) {
			double burstCarveRadius = Math.min(64.0, Math.min(poundR * 0.95, 12.0 + (poundCount * 4.5)));
			BlockPos.MutableBlockPos cPos = new BlockPos.MutableBlockPos();
			for (int i = 0; i < 64; i++) {
				double a = (2 * Math.PI / 64) * i;
				for (double dist = 0.0; dist <= burstCarveRadius; dist += 1.2) {
					int bx = Mth.floor(ix + Math.cos(a) * dist);
					int bz = Mth.floor(iz + Math.sin(a) * dist);
					double dNorm = dist / Math.max(1.0, burstCarveRadius);
					int carveDown = (int) Math.round((1.0 - dNorm * 0.6) * (3.5 + Math.min(32.0, poundCount * 1.5)) * penSpeed);
					for (int dy = 2; dy >= -carveDown; dy--) {
						cPos.set(bx, Mth.floor(gy + dy), bz);
						if (!level.hasChunkAt(cPos)) continue;
						BlockState st = level.getBlockState(cPos);
						boolean isCurBedrock = st.getBlock() == Blocks.BEDROCK;
						boolean isCurIndestructible = isCurBedrock || isIndestructibleBlock(st, level, cPos);
						if (isCurIndestructible) {
							recordMaterialEncounter(nbt, st.getBlock());
						}
						boolean shatterBedrock = shouldShatterBedrock(nbt, dist, poundCount, st.getBlock());
						boolean crackBedrock = shouldCrackBedrock(nbt, dist, poundCount, st.getBlock());

						if (!st.getFluidState().isEmpty() || st.getBlock() == Blocks.WATER || st.getBlock() == Blocks.LAVA) {
							replaceBlockWithOakPlague(level, cPos, st, shatterBedrock || crackBedrock);
							continue;
						}
						if (dy > -carveDown) {
							if (!st.isAir()) {
								if (isCurIndestructible) {
									if (shatterBedrock) {
										level.setBlock(cPos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
									} else if (crackBedrock && canInfectIndestructible(nbt)) {
										level.setBlock(cPos, getSplinteredOakState(), INFECTION_BLOCK_FLAGS);
									}
								} else {
									level.setBlock(cPos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
								}
							}
						} else {
							replaceBlockWithOakPlague(level, cPos, st, shatterBedrock || crackBedrock);
						}
					}
				}
			}
		}

		if (terraform) {
			nbt.putDouble("verdant_burst_target_radius", Math.min(MAX_INFECTION_RADIUS, persistentDouble(nbt, "verdant_infection_radius", 0.0) + 32.0 + (poundCount * 14.0)));
			nbt.putInt("verdant_burst_spread_ticks", 40);
			nbt.putInt("verdant_pound_burst_ticks", 40);
		}
	}

	private static void handleActiveShockwaves(ServerLevel level, CompoundTag nbt) {
		double stage = persistentDouble(nbt, "verdant_shockwave_stage", 0.0);
		if (stage <= 0.0) return;

		// Deduplication Guard: Enforce exactly 1 shockwave update per game tick
		long gameTime = level.getGameTime();
		if (persistentLong(nbt, "verdant_shockwave_last_tick", -1L) == gameTime) return;
		nbt.putLong("verdant_shockwave_last_tick", gameTime);

		double ox = persistentDouble(nbt, "verdant_shockwave_ox", 0.0);
		double oy = persistentDouble(nbt, "verdant_shockwave_oy", 0.0);
		double oz = persistentDouble(nbt, "verdant_shockwave_oz", 0.0);

		if (!isPlayerInParticleRange3D(level, ox, oy, oz, 128.0 * 128.0)) {
			clearActiveShockwave(nbt);
			return;
		}

		double maxR = Math.min(getShockwaveParticleRadius(nbt), persistentDouble(nbt, "verdant_shockwave_max_r", 48.0));
		int pounds = persistentInt(nbt, "verdant_pound_count", 0);
		double expandSpeed = 2.0 + (pounds * 0.35);
		double radius = BEAM_RADIUS + (stage * expandSpeed);

		// Expiry / Boundary termination check
		if (radius >= maxR || stage > 50.0) {
			clearActiveShockwave(nbt);
			return;
		}

		double quality = getShockwaveParticleQuality(nbt);
		if (quality > 0.0) {
			int count = Math.min(48, Math.max((int) (10 * quality), (int) (2.0 * Math.PI * radius * 0.45 * quality)));

			int lastChunkX = Integer.MIN_VALUE;
			int lastChunkZ = Integer.MIN_VALUE;
			LevelChunk chunk = null;

			for (int i = 0; i < count; i++) {
				double a = (2.0 * Math.PI / count) * i;
				
				// Multi-frequency organic radial wave offsets
				double wave1 = fastSin(a * 5.0 + stage * 0.3) * 0.85;
				double wave2 = fastCos(a * 11.0 - stage * 0.2) * 0.55;
				double organicRadialOffset = wave1 + wave2 + (Math.random() - 0.5) * 1.5;

				double effRadius = Math.max(0.5, radius + organicRadialOffset);
				double c = Math.cos(a);
				double s = Math.sin(a);

				double px = ox + c * effRadius + (Math.random() - 0.5) * 0.5;
				double pz = oz + s * effRadius + (Math.random() - 0.5) * 0.5;
				int ipx = Mth.floor(px);
				int ipz = Mth.floor(pz);
				int cx = ipx >> 4;
				int cz = ipz >> 4;

				if (cx != lastChunkX || cz != lastChunkZ) {
					lastChunkX = cx;
					lastChunkZ = cz;
					chunk = level.getChunkSource().getChunkNow(cx, cz);
				}
				if (chunk == null) continue;

				// Ultra-fast O(1) terrain heightmap lookup - smooth on slopes/uphill/downhill without looping
				int groundY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, ipx & 15, ipz & 15);
				if (groundY < level.getMinBuildHeight() + 1 || Math.abs(groundY - oy) > 24.0) continue;

				// Spawn height: block top + 1.5 (raised to prevent clipping inside blocks)
				double surfaceY = groundY + 1.5;

				double outwardSpeed = 0.08 + Math.random() * 0.14;
				double upwardSpeed = 0.03 + Math.random() * 0.05;

				// 1. Outward Blast Layer (Big Smoke)
				sendFarParticles(level, getBigSmokeParticle(), px, surfaceY, pz, 1, c * outwardSpeed, upwardSpeed, s * outwardSpeed, 0.03);

				// 2. Toroidal Curling Vortex Layer (smoke arcs upward and rolls backward for volumetric look)
				if (i % 3 == 0) {
					double curlRadius = Math.max(0.5, effRadius - 0.4 - Math.random() * 0.5);
					double curlY = surfaceY + 0.6 + Math.random() * 0.6;
					double inwardRollSpeed = 0.04 + Math.random() * 0.06;
					double upwardRollSpeed = 0.10 + Math.random() * 0.08;
					sendFarParticles(level, getBigSmokeParticle(), ox + c * curlRadius, curlY, oz + s * curlRadius, 1, -c * inwardRollSpeed, upwardRollSpeed, -s * inwardRollSpeed, 0.02);
				}
			}
		}

		// Shockwave damage & knockback perfectly synced with expanding particle ring
		float slamDmg = persistentFloat(nbt, "verdant_shockwave_dmg", 195.0F);
		double innerR = Math.max(0.0, radius - expandSpeed - 2.0);
		double outerR = radius + expandSpeed + 2.0;
		double innerRSq = innerR * innerR;
		double outerRSq = outerR * outerR;
		double maxRSq = maxR * maxR;
		AABB waveBox = new AABB(ox - outerR, oy - 12.0, oz - outerR, ox + outerR, oy + 35.0, oz + outerR);

		long[] hitIds = nbt.contains("verdant_shockwave_hit_ids") ? nbt.getLongArray("verdant_shockwave_hit_ids") : new long[0];
		java.util.Set<Long> hitSet = new java.util.HashSet<>();
		for (long id : hitIds) hitSet.add(id);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, waveBox, e -> e.isAlive())) {
			if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
			if (isWoodbound(victim)) continue;
			long id = victim.getId();
			if (hitSet.contains(id)) continue;

			double dx = victim.getX() - ox;
			double dz = victim.getZ() - oz;
			double distSq = dx * dx + dz * dz;

			if (distSq >= innerRSq && distSq <= outerRSq && distSq <= maxRSq) {
				double dist = Math.sqrt(distSq);
				hitSet.add(id);
				float waveDmg = Math.max(SHOCKWAVE_MIN_DAMAGE, slamDmg * (float) (1.0 - (dist / (maxR * 1.2))));

				victim.setNoGravity(false);
				victim.getPersistentData().putInt("verdant_slammed_ticks", 30);

				double len = dist > 0.001 ? dist : 1.0;
				double pushX = (dx / len) * 1.8 * (1.0 - dist / (maxR * 1.5));
				double pushZ = (dz / len) * 1.8 * (1.0 - dist / (maxR * 1.5));
				double pushY = 0.6 + 0.4 * (1.0 - dist / (maxR * 1.5));

				victim.setDeltaMovement(pushX, pushY, pushZ);
				victim.hurtMarked = true;
				victim.hasImpulse = true;

				applyTruePoundDamage(level, victim, null, waveDmg);
				victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2, false, false, true));
				if (victim instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
			}
		}

		long[] updatedHits = new long[hitSet.size()];
		int idx = 0;
		for (Long id : hitSet) updatedHits[idx++] = id;
		nbt.putLongArray("verdant_shockwave_hit_ids", updatedHits);

		if ((int) stage % 3 == 1) playEngineSound(level, ox, oy, oz, "block.beacon.power_select", 24.0F, 0.20F - ((float) stage * 0.005F));
		processShockwaveDominoEffects(level, ox, oy, oz, radius, expandSpeed, oy + 85.0);

		nbt.putDouble("verdant_shockwave_stage", stage + 1.0);
	}

	private static void clearActiveShockwave(CompoundTag nbt) {
		if (nbt == null) return;
		nbt.putDouble("verdant_shockwave_stage", 0.0);
		nbt.remove("verdant_shockwave_hit_ids");
		nbt.remove("verdant_shockwave_last_tick");
	}

	private static void performSlowKineticTerraforming(ServerLevel level, CompoundTag nbt, double cx, double gy, double cz, double r, int pounds) {
		double depth = persistentDouble(nbt, "verdant_crater_depth", 0.0);
		if (depth < 64.0) {
			depth += 0.0012;
			nbt.putDouble("verdant_crater_depth", depth);
		}

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int s = 0; s < 2; s++) {
			double a = Math.random() * Math.PI * 2, d = Math.sqrt(Math.random()) * r * 1.6;
			double dx = Math.cos(a) * d, dz = Math.sin(a) * d, norm = d / r;
			if (norm > 1.65) continue;

			double rough = 0.5 + 0.22 * Math.sin(dx * 0.75 + dz * 0.6) + 0.15 * Math.cos(dx * 1.2 - dz * 0.8);
			int targetD = (int) Math.round(depth * Math.max(0.0, 1.0 - (norm * 0.65)) * (0.8 + 0.4 * rough));

			for (int dy = 2; dy >= -targetD - 1; dy--) {
				pos.set(Mth.floor(cx + dx), Mth.floor(gy + dy), Mth.floor(cz + dz));
				if (!level.hasChunkAt(pos)) continue;
				BlockState st = level.getBlockState(pos);
				boolean isCurBedrock = st.getBlock() == Blocks.BEDROCK;
				boolean isCurIndestructible = isCurBedrock || isIndestructibleBlock(st, level, pos);
				if (isCurIndestructible) {
					recordMaterialEncounter(nbt, st.getBlock());
				}
				boolean shatterBedrock = shouldShatterBedrock(nbt, d, pounds, st.getBlock());
				boolean crackBedrock = shouldCrackBedrock(nbt, d, pounds, st.getBlock());

				if (!st.getFluidState().isEmpty() || st.getBlock() == Blocks.WATER || st.getBlock() == Blocks.LAVA) {
					if (ENABLE_FLUID_EVAPORATION) level.setBlock(pos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
					continue;
				}
				if (dy > -targetD) {
					if (!st.isAir()) {
						if (isCurIndestructible) {
							if (shatterBedrock) {
								level.setBlock(pos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
							} else if (crackBedrock && canInfectIndestructible(nbt)) {
								level.setBlock(pos, getSplinteredOakState(), INFECTION_BLOCK_FLAGS);
							}
						} else {
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
						}
					}
				} else {
					replaceBlockWithOakPlague(level, pos, st, shatterBedrock || crackBedrock);
					break;
				}
			}
		}
	}

	private static boolean isColumnDeeplyInfected(ServerLevel level, int bx, int bz, double hoverY) {
		int chunkX = bx >> 4;
		int chunkZ = bz >> 4;
		LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
		if (chunk == null) {
			return isInfectedBiome(level, new BlockPos(bx, Mth.floor(hoverY), bz));
		}

		Block falseOak = getFalseOakPlanksBlock();
		Block petrified = getPetrifiedOakBlock();
		Block splintered = getSplinteredOakBlock();
		Block lignumCaro = getLignumCaroBlock();

		int minWorldY = (int) level.getMinBuildHeight() + 5;
		double groundY = findGroundBelow(level, bx, bz, hoverY);
		int surfaceY = (int) groundY;
		if (surfaceY <= minWorldY) return true;

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		// Sample 1: Surface
		pos.set(bx, Math.max(minWorldY, surfaceY - 1), bz);
		Block bSurf = chunk.getBlockState(pos).getBlock();
		boolean surfInfected = (bSurf == Blocks.OAK_PLANKS || bSurf == Blocks.OAK_LOG || bSurf == petrified || bSurf == splintered || bSurf == lignumCaro || bSurf == falseOak || bSurf == Blocks.PETRIFIED_OAK_SLAB);
		if (!surfInfected) return false;

		// Sample 2: Upper Subterranean (y = surfaceY - 25)
		int yMid = Math.max(minWorldY + 10, surfaceY - 25);
		pos.set(bx, yMid, bz);
		BlockState stMid = chunk.getBlockState(pos);
		if (stMid.isAir()) {
			pos.set(bx, Math.max(minWorldY + 1, yMid - 2), bz);
			stMid = chunk.getBlockState(pos);
		}
		Block bMid = stMid.getBlock();
		boolean midInfected = stMid.isAir() || (bMid == Blocks.BEDROCK || bMid == Blocks.BARRIER || bMid == Blocks.OAK_PLANKS || bMid == Blocks.OAK_LOG || bMid == petrified || bMid == splintered || bMid == lignumCaro || bMid == falseOak || bMid == Blocks.PETRIFIED_OAK_SLAB);
		if (!midInfected) return false;

		// Sample 3: Deep Cavern/Deepslate Layer (y = minWorldY + 20)
		int yDeep = Math.max(minWorldY + 5, Math.min(yMid - 10, minWorldY + 20));
		pos.set(bx, yDeep, bz);
		BlockState stDeep = chunk.getBlockState(pos);
		if (stDeep.isAir()) {
			pos.set(bx, Math.max(minWorldY + 1, yDeep - 2), bz);
			stDeep = chunk.getBlockState(pos);
		}
		Block bDeep = stDeep.getBlock();
		boolean deepInfected = stDeep.isAir() || (bDeep == Blocks.BEDROCK || bDeep == Blocks.BARRIER || bDeep == Blocks.OAK_PLANKS || bDeep == Blocks.OAK_LOG || bDeep == petrified || bDeep == splintered || bDeep == lignumCaro || bDeep == falseOak || bDeep == Blocks.PETRIFIED_OAK_SLAB);

		return deepInfected;
	}

	private static final long[] PROCESSED_COLUMNS = new long[4096];
	private static final int[] PROCESSED_COLUMNS_TAG = new int[4096];

	private static boolean markColumnVisitedThisTick(int bx, int bz, int tickEpoch) {
		long key = (((long) bx) << 32) | (bz & 0xFFFFFFFFL);
		int mask = 4095;
		int idx = (int) (key ^ (key >>> 16) ^ (key >>> 32)) & mask;
		for (int i = 0; i < 16; i++) {
			int slot = (idx + i) & mask;
			if (PROCESSED_COLUMNS_TAG[slot] == tickEpoch) {
				if (PROCESSED_COLUMNS[slot] == key) return false;
			} else {
				PROCESSED_COLUMNS_TAG[slot] = tickEpoch;
				PROCESSED_COLUMNS[slot] = key;
				return true;
			}
		}
		return true;
	}

	private static void infectRod3DBlock(ServerLevel level, int bx, int bz, double scanTop, double gy, double curR, boolean isSolidCore, boolean isBurst, boolean spawnBurstParticles) {
		int chunkX = bx >> 4;
		int chunkZ = bz >> 4;
		LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
		if (chunk == null) return;

		int minWorldY = (int) level.getMinBuildHeight() + 5;
		int topY = (int) scanTop;
		if (topY <= minWorldY) return;

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		Block falseOak = getFalseOakPlanksBlock();
		Block petrified = getPetrifiedOakBlock();
		Block splintered = getSplinteredOakBlock();
		Block lignumCaro = getLignumCaroBlock();

		int maxTarget = isSolidCore ? (isBurst ? 16 : 10) : (isBurst ? 5 : 3);
		int converted = 0;
		boolean isCell = isCellularMembrane(bx, bz, level.getSeed());

		int worldTop = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, bx & 15, bz & 15);
		boolean hasCeiling = isCeiledDimension(level);
		int startY;
		if (hasCeiling) {
			// Ceiled Dimension Mode (e.g. Nether / subterranean): Clamps start height around Verdant altitude
			startY = Math.min(topY, (int) gy + 35);
		} else {
			// Open-Sky Dimension Mode (Overworld / End / surface): Full mountain and world height penetration
			startY = Math.min(topY, worldTop);
		}

		// Full-Column Penetration: Scan continuously from top down to deep world minimum
		for (int y = startY; y >= minWorldY; y--) {
			pos.set(bx, y, bz);
			BlockState st = chunk.getBlockState(pos);
			if (st.isAir()) continue;

			Block b = st.getBlock();
			if (b == Blocks.ANCIENT_DEBRIS || isAncientDebris(st, b)) continue;

			if (isIndestructibleBlock(st, level, pos) && !canInfectIndestructible(null)) break;

			boolean isAlreadyInfected = (b == Blocks.OAK_PLANKS || b == Blocks.OAK_LOG || b == petrified || b == splintered || b == lignumCaro || b == falseOak || b == Blocks.PETRIFIED_OAK_SLAB);
			if (isAlreadyInfected) continue;

			boolean isFoliage = isFoliageOrDebris(st, b);

			if (isFoliage) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
				continue;
			}

			if (replaceBlockWithOakPlague(level, pos, st, false, isCell)) {
				converted++;
				if (spawnBurstParticles && Math.random() < 0.10) {
					sendFarParticles(level, getOakPlanksParticle(), pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.05);
				}
				if (converted >= maxTarget) return;
			}
		}
	}

	private static void performContinuousBeamTipInfection(ServerLevel level, CompoundTag nbt, double cx, double gy, double cz, int cycle, int pounds) {
		double speedMult = getInfectionSpeedMultiplier(nbt);
		double cycleProg = (double) Math.min(cycle, CYCLE_TOTAL_TICKS) / (double) CYCLE_TOTAL_TICKS;
		// Expands outward slowly until the pound detonates (starts at BEAM_RADIUS ~3.0, expands to ~12.0)
		double tipRadius = BEAM_RADIUS + cycleProg * (9.0 + Math.min(6.0, pounds * 0.5));

		int sampleCount = (int) Math.max(12, Math.min(36, (12 + tipRadius * 2.0) * speedMult));
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean canInfectIndestructible = canInfectIndestructible(nbt);
		boolean isCell = isCellularMembrane(Mth.floor(cx), Mth.floor(cz), level.getSeed());

		for (int i = 0; i < sampleCount; i++) {
			double angle = Math.random() * Math.PI * 2.0;
			double dist = Math.sqrt(Math.random()) * tipRadius;
			int bx = Mth.floor(cx + Math.cos(angle) * dist);
			int bz = Mth.floor(cz + Math.sin(angle) * dist);

			int startY = (int) Math.min(level.getMaxBuildHeight() - 2.0, gy + 5.0);
			int endY = (int) Math.max(level.getMinBuildHeight() + 2.0, gy - 12.0);

			for (int y = startY; y >= endY; y--) {
				pos.set(bx, y, bz);
				if (!level.hasChunkAt(pos)) continue;
				BlockState st = level.getBlockState(pos);
				if (st.isAir()) continue;

				if (isIndestructibleBlock(st, level, pos)) {
					if (!canInfectIndestructible) {
						break;
					}
				}

				if (isFoliageOrDebris(st, st.getBlock())) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
					continue;
				}

				for (int dy = 0; dy < 3; dy++) {
					int subY = y - dy;
					if (subY < endY) break;
					pos.set(bx, subY, bz);
					BlockState subSt = level.getBlockState(pos);
					if (subSt.isAir()) continue;
					if (isIndestructibleBlock(subSt, level, pos)) {
						if (!canInfectIndestructible) break;
					}
					if (isFoliageOrDebris(subSt, subSt.getBlock())) {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
						continue;
					}
					replaceBlockWithOakPlague(level, pos, subSt, false, isCell);
				}
				break;
			}
		}
	}

	private static void performContinuousSolidRadialWoodInfection(ServerLevel level, Entity entity, CompoundTag nbt, double cx, double gy, double cz, int pounds) {
		double speedMult = getInfectionSpeedMultiplier(nbt);
		double curR = Math.min(MAX_INFECTION_RADIUS, persistentDouble(nbt, "verdant_infection_radius", BEAM_RADIUS + 2.0) + (0.08 * speedMult));
		nbt.putDouble("verdant_infection_radius", curR);

		boolean breakBedrock = shouldShatterBedrock(nbt, 0.0, pounds);
		boolean hasCeiling = isCeiledDimension(level);
		double lockedY = persistentDouble(nbt, "verdant_locked_y", gy + HOVER_TARGET_ALTITUDE);
		double scanTop = hasCeiling
			? Math.min(level.getMaxBuildHeight() - 4.0, Math.max(gy + 85.0, (lockedY > 0 ? lockedY + 15.0 : gy + 85.0)))
			: (level.getMaxBuildHeight() - 4.0);
		int tickEpoch = (int) level.getGameTime();

		if (entity != null) {
			ACTIVE_CORES.put(entity.getUUID(), new ActiveVerdantCore(entity.getUUID(), level.dimension().location(), cx, cz, gy, scanTop, curR, pounds, level.getGameTime()));
		}

		int totalSamples = (int) Math.min(200, Math.max(40, (2.0 * Math.PI * curR * 1.2 + (pounds * 12)) * speedMult));

		int poundBurst = persistentInt(nbt, "verdant_pound_burst_ticks", 0);
		if (poundBurst > 0) {
			totalSamples += (int) ((80 + (Math.random() * 40)) * speedMult);
			nbt.putInt("verdant_pound_burst_ticks", poundBurst - 1);
		}

		int solidSamples = (int) (totalSamples * 0.90);
		double solidR = Math.max(0.0, curR - 2.5);

		for (int i = 0; i < totalSamples; i++) {
			double a = Math.random() * Math.PI * 2;
			boolean isSolidCore = i < solidSamples;

			double d = isSolidCore
				? (Math.sqrt(Math.random()) * solidR)
				: (solidR + (Math.random() * Math.max(0.1, curR - solidR)));

			int bx = Mth.floor(cx + Math.cos(a) * d);
			int bz = Mth.floor(cz + Math.sin(a) * d);

			if (!markColumnVisitedThisTick(bx, bz, tickEpoch)) continue;
			infectRod3DBlock(level, bx, bz, scanTop, gy, curR, isSolidCore, false, false);
		}
	}

	private static void handleBurstInfectionSpill(ServerLevel level, CompoundTag nbt, double cx, double gy, double cz, int pounds) {
		double speedMult = getInfectionSpeedMultiplier(nbt);
		int burstTicks = persistentInt(nbt, "verdant_burst_spread_ticks", 0);
		if (burstTicks <= 0) return;

		burstTicks--;
		nbt.putInt("verdant_burst_spread_ticks", burstTicks);

		double targetR = Math.min(MAX_INFECTION_RADIUS, persistentDouble(nbt, "verdant_burst_target_radius", 0.0));
		double curR = Math.min(MAX_INFECTION_RADIUS, persistentDouble(nbt, "verdant_infection_radius", 0.0));
		double stepR = Math.min(MAX_INFECTION_RADIUS, curR + (((targetR - curR) * (1.0 / (burstTicks + 1))) * speedMult));
		nbt.putDouble("verdant_infection_radius", stepR);

		boolean breakBedrock = shouldShatterBedrock(nbt, 0.0, pounds);
		boolean hasCeiling = isCeiledDimension(level);
		double lockedY = persistentDouble(nbt, "verdant_locked_y", gy + HOVER_TARGET_ALTITUDE);
		double scanTop = hasCeiling
			? Math.min(level.getMaxBuildHeight() - 4.0, Math.max(gy + 85.0, (lockedY > 0 ? lockedY + 15.0 : gy + 85.0)))
			: (level.getMaxBuildHeight() - 4.0);
		int tickEpoch = (int) level.getGameTime();

		int burstSamples = (int) Math.min(600, Math.max(150, (2.0 * Math.PI * stepR * 1.8) * speedMult));
		int solidSamples = (int) (burstSamples * 0.90);
		double solidR = Math.max(0.0, stepR - 2.5);

		for (int i = 0; i < burstSamples; i++) {
			double a = Math.random() * Math.PI * 2;
			boolean isSolidCore = i < solidSamples;

			double d = isSolidCore
				? (Math.sqrt(Math.random()) * solidR)
				: (solidR + (Math.random() * Math.max(0.1, stepR - solidR)));

			int bx = Mth.floor(cx + Math.cos(a) * d);
			int bz = Mth.floor(cz + Math.sin(a) * d);

			if (!markColumnVisitedThisTick(bx, bz, tickEpoch)) continue;
			infectRod3DBlock(level, bx, bz, scanTop, gy, stepR, isSolidCore, true, true);
		}
	}

	private static Block CACHED_PETRIFIED_OAK = null;
	private static Block CACHED_LIGNUM_CARO = null;
	private static Block CACHED_FALSE_OAK_PLANKS = null;

	private static BlockState CACHED_PETRIFIED_OAK_STATE = null;
	private static BlockState CACHED_LIGNUM_CARO_STATE = null;
	private static BlockState CACHED_FALSE_OAK_PLANKS_STATE = null;
	private static BlockState CACHED_OAK_PLANKS_STATE = null;
	private static BlockParticleOption CACHED_OAK_PLANKS_PARTICLE = null;

	private static final java.util.Map<Block, Boolean> IS_LEAF_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Map<Block, Boolean> IS_ORE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Map<Block, Boolean> IS_FOLIAGE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Map<Block, Boolean> IS_SAND_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Map<Block, Boolean> IS_FIRE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	// Flag 18 = 2 (Block.UPDATE_CLIENTS) | 16 (Block.UPDATE_KNOWN_SHAPE / Block.UPDATE_SUPPRESS_SHAPE_UPDATE)
	// - Bit 2 ensures clients receive real-time block change packets immediately
	// - Bit 1 (UPDATE_NEIGHBORS) is skipped, preventing cascading redstone/fluid neighbor loops (which accounted for 34.4% tick CPU)
	// - Bit 16 suppresses expensive neighbor shape recalculations across all 6 faces (which accounted for 28.5% tick CPU)
	private static final int INFECTION_BLOCK_FLAGS = 18;

	private static BlockState getPetrifiedOakState() {
		if (CACHED_PETRIFIED_OAK_STATE == null) {
			CACHED_PETRIFIED_OAK_STATE = getPetrifiedOakBlock().defaultBlockState();
		}
		return CACHED_PETRIFIED_OAK_STATE;
	}

	private static BlockState getLignumCaroState() {
		if (CACHED_LIGNUM_CARO_STATE == null) {
			CACHED_LIGNUM_CARO_STATE = getLignumCaroBlock().defaultBlockState();
		}
		return CACHED_LIGNUM_CARO_STATE;
	}

	private static BlockState getFalseOakPlanksState() {
		if (CACHED_FALSE_OAK_PLANKS_STATE == null) {
			CACHED_FALSE_OAK_PLANKS_STATE = getFalseOakPlanksBlock().defaultBlockState();
		}
		return CACHED_FALSE_OAK_PLANKS_STATE;
	}

	private static BlockState getOakPlanksState() {
		if (CACHED_OAK_PLANKS_STATE == null) {
			CACHED_OAK_PLANKS_STATE = Blocks.OAK_PLANKS.defaultBlockState();
		}
		return CACHED_OAK_PLANKS_STATE;
	}

	private static BlockParticleOption getOakPlanksParticle() {
		if (CACHED_OAK_PLANKS_PARTICLE == null) {
			CACHED_OAK_PLANKS_PARTICLE = new BlockParticleOption(ParticleTypes.BLOCK, getOakPlanksState());
		}
		return CACHED_OAK_PLANKS_PARTICLE;
	}

	private static ParticleOptions CACHED_BIG_SMOKE_PARTICLE = null;

	private static ParticleOptions getBigSmokeParticle() {
		if (CACHED_BIG_SMOKE_PARTICLE == null) {
			try {
				ResourceLocation loc = ResourceLocation.tryParse("the_backwoods:big_smoke");
				if (loc != null && BuiltInRegistries.PARTICLE_TYPE.containsKey(loc)) {
					Object p = BuiltInRegistries.PARTICLE_TYPE.get(loc);
					if (p instanceof ParticleOptions po) {
						CACHED_BIG_SMOKE_PARTICLE = po;
					}
				}
			} catch (Throwable ignored) {}
			if (CACHED_BIG_SMOKE_PARTICLE == null) {
				CACHED_BIG_SMOKE_PARTICLE = ParticleTypes.LARGE_SMOKE;
			}
		}
		return CACHED_BIG_SMOKE_PARTICLE;
	}

	private static final java.util.Map<Block, Boolean> IS_CACTUS_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Map<Block, Boolean> IS_SANDSTONE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	private static boolean isLeafBlock(Block block, BlockState state) {
		Boolean cached = IS_LEAF_CACHE.get(block);
		if (cached != null) return cached;
		boolean isLeaf = state.is(BlockTags.LEAVES) || state.is(BlockTags.WART_BLOCKS)
			|| block == Blocks.NETHER_WART_BLOCK || block == Blocks.WARPED_WART_BLOCK || block == Blocks.SHROOMLIGHT;
		if (!isLeaf) {
			String path = BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase();
			isLeaf = path.contains("leaves") || path.contains("wart_block") || path.contains("shroomlight") || block.getClass().getSimpleName().toLowerCase().contains("leaves");
		}
		IS_LEAF_CACHE.put(block, isLeaf);
		return isLeaf;
	}

	private static boolean isAncientDebris(BlockState state, Block block) {
		if (block == Blocks.ANCIENT_DEBRIS) return true;
		String path = BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase();
		return path.contains("ancient_debris");
	}

	private static boolean isOreBlock(Block block, BlockState state) {
		if (isAncientDebris(state, block)) return false;
		Boolean cached = IS_ORE_CACHE.get(block);
		if (cached != null) return cached;
		boolean isOre = state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COPPER_ORES)
			|| state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.REDSTONE_ORES) || state.is(BlockTags.LAPIS_ORES)
			|| state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES);
		if (!isOre) {
			String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
			isOre = path.endsWith("_ore") || path.contains("ore_");
		}
		IS_ORE_CACHE.put(block, isOre);
		return isOre;
	}

	private static boolean isFoliageOrDebris(BlockState state, Block block) {
		Boolean cached = IS_FOLIAGE_CACHE.get(block);
		if (cached != null) return cached;

		boolean result = false;
		if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS)
			|| block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS || block == Blocks.FERN || block == Blocks.LARGE_FERN
			|| block == Blocks.DEAD_BUSH || block == Blocks.SWEET_BERRY_BUSH || block == Blocks.AZALEA || block == Blocks.FLOWERING_AZALEA || block == Blocks.PITCHER_PLANT || block == Blocks.PITCHER_CROP || block == Blocks.TORCHFLOWER || block == Blocks.TORCHFLOWER_CROP || block == Blocks.PINK_PETALS || block == Blocks.LILY_PAD || block == Blocks.SMALL_DRIPLEAF || block == Blocks.BIG_DRIPLEAF || block == Blocks.BIG_DRIPLEAF_STEM || block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM || block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.RED_MUSHROOM_BLOCK || block == Blocks.MOSS_BLOCK || block == Blocks.MOSS_CARPET || block == Blocks.VINE || block == Blocks.HANGING_ROOTS || block == Blocks.SNOW
			|| block == Blocks.CHORUS_PLANT || block == Blocks.CHORUS_FLOWER || block == Blocks.CRIMSON_ROOTS || block == Blocks.WARPED_ROOTS
			|| block == Blocks.NETHER_SPROUTS || block == Blocks.WEEPING_VINES || block == Blocks.WEEPING_VINES_PLANT || block == Blocks.TWISTING_VINES
			|| block == Blocks.TWISTING_VINES_PLANT || block == Blocks.CRIMSON_FUNGUS || block == Blocks.WARPED_FUNGUS || block == Blocks.SUGAR_CANE
			|| block == Blocks.BAMBOO || block == Blocks.BAMBOO_SAPLING) {
			result = true;
		} else {
			String path = BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase();
			result = (path.contains("leaf") || path.contains("bush") || path.contains("plant") || path.contains("flower")
				/* removed grass keyword */ || path.contains("shrub") || path.contains("berry") || path.contains("crop")
				|| path.contains("herb") || path.contains("sprout") || path.contains("root") || path.contains("vine")
				|| path.contains("weed") || path.contains("petal") || path.contains("moss") || path.contains("foliage")
				|| path.contains("flora") || path.contains("fern") || path.contains("firefly")) && !path.contains("cactus") && !path.contains("cacti");
		}
		IS_FOLIAGE_CACHE.put(block, result);
		return result;
	}

	private static boolean isCactus(BlockState state, Block block) {
		if (block == Blocks.CACTUS) return true;
		Boolean cached = IS_CACTUS_CACHE.get(block);
		if (cached != null) return cached;
		String path = BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase();
		boolean result = path.contains("cactus") || path.contains("cacti");
		IS_CACTUS_CACHE.put(block, result);
		return result;
	}

	private static boolean isSandstone(BlockState state, Block block) {
		if (block == Blocks.SANDSTONE || block == Blocks.RED_SANDSTONE || block == Blocks.SMOOTH_SANDSTONE
			|| block == Blocks.CUT_SANDSTONE || block == Blocks.CHISELED_SANDSTONE || block == Blocks.SMOOTH_RED_SANDSTONE
			|| block == Blocks.CUT_RED_SANDSTONE || block == Blocks.CHISELED_RED_SANDSTONE) {
			return true;
		}
		Boolean cached = IS_SANDSTONE_CACHE.get(block);
		if (cached != null) return cached;
		String path = BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase();
		boolean result = path.contains("sandstone") || path.contains("red_sandstone");
		IS_SANDSTONE_CACHE.put(block, result);
		return result;
	}

	private static boolean isAnySand(BlockState state, Block block) {
		Boolean cached = IS_SAND_CACHE.get(block);
		if (cached != null) return cached;

		boolean result = false;
		if (state.is(BlockTags.SAND) || block == Blocks.SAND || block == Blocks.RED_SAND || block == Blocks.SUSPICIOUS_SAND || block == Blocks.SOUL_SAND) {
			result = true;
		} else {
			String path = BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase();
			result = path.contains("sand") && !path.contains("sandstone");
		}
		IS_SAND_CACHE.put(block, result);
		return result;
	}

	private static Block getPetrifiedOakBlock() {
		if (CACHED_PETRIFIED_OAK == null) {
			CACHED_PETRIFIED_OAK = getRegisteredBlock("the_backwoods:petrified_oak_planks", null);
			if (CACHED_PETRIFIED_OAK == null) {
				CACHED_PETRIFIED_OAK = getRegisteredBlock("thebackwoods:petrified_oak_planks", Blocks.OAK_PLANKS);
			}
		}
		return CACHED_PETRIFIED_OAK;
	}

	private static volatile Block CACHED_SPLINTERED_OAK = null;
	private static volatile BlockState CACHED_SPLINTERED_OAK_STATE = null;

	private static Block getSplinteredOakBlock() {
		if (CACHED_SPLINTERED_OAK == null) {
			CACHED_SPLINTERED_OAK = getRegisteredBlock("the_backwoods:splintered_oak_planks", null);
			if (CACHED_SPLINTERED_OAK == null) {
				CACHED_SPLINTERED_OAK = getRegisteredBlock("thebackwoods:splintered_oak_planks", Blocks.OAK_PLANKS);
			}
		}
		return CACHED_SPLINTERED_OAK;
	}

	private static BlockState getSplinteredOakState() {
		if (CACHED_SPLINTERED_OAK_STATE == null) {
			CACHED_SPLINTERED_OAK_STATE = getSplinteredOakBlock().defaultBlockState();
		}
		return CACHED_SPLINTERED_OAK_STATE;
	}

	private static Block getLignumCaroBlock() {
		if (CACHED_LIGNUM_CARO == null) {
			CACHED_LIGNUM_CARO = getRegisteredBlock("the_backwoods:lignum_caro", null);
			if (CACHED_LIGNUM_CARO == null) {
				CACHED_LIGNUM_CARO = getRegisteredBlock("thebackwoods:lignum_caro", Blocks.OAK_PLANKS);
			}
		}
		return CACHED_LIGNUM_CARO;
	}

	private static Block getFalseOakPlanksBlock() {
		if (CACHED_FALSE_OAK_PLANKS == null) {
			CACHED_FALSE_OAK_PLANKS = getRegisteredBlock("the_backwoods:false_oak_planks", null);
			if (CACHED_FALSE_OAK_PLANKS == null) {
				CACHED_FALSE_OAK_PLANKS = getRegisteredBlock("thebackwoods:false_oak_planks", Blocks.OAK_PLANKS);
			}
		}
		return CACHED_FALSE_OAK_PLANKS;
	}

	private static boolean isCellularMembrane(double x, double z, long seed) {
		double baseScale = 0.026;
		double thickness = 0.045;
		double macroNoise = Math.sin(x * (baseScale * 0.1) + seed) * Math.cos(z * (baseScale * 0.1) - seed);
		double activeScale = baseScale + (macroNoise * (baseScale * 0.3));

		double scaledX = x * activeScale;
		double scaledZ = z * activeScale;

		int cellX = (int) Math.floor(scaledX);
		int cellZ = (int) Math.floor(scaledZ);

		double minDistance1 = Double.MAX_VALUE;
		double minDistance2 = Double.MAX_VALUE;

		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				int targetGridX = cellX + i;
				int targetGridZ = cellZ + j;

				long hash = hashCoords(targetGridX, targetGridZ, seed);
				double offsetX = ((hash & 0xFFF) / 4095.0);
				double offsetZ = (((hash >> 12) & 0xFFF) / 4095.0);

				double warpX = Math.sin(targetGridZ * 2.0 + seed) * 0.35;
				double warpZ = Math.cos(targetGridX * 2.0 - seed) * 0.35;

				double featurePointX = targetGridX + offsetX + warpX;
				double featurePointZ = targetGridZ + offsetZ + warpZ;

				double diffX = scaledX - featurePointX;
				double diffZ = scaledZ - featurePointZ;
				double distSq = diffX * diffX + diffZ * diffZ;

				if (distSq < minDistance1) {
					minDistance2 = minDistance1;
					minDistance1 = distSq;
				} else if (distSq < minDistance2) {
					minDistance2 = distSq;
				}
			}
		}

		double boundaryField = Math.sqrt(minDistance2) - Math.sqrt(minDistance1);
		double localWobble = Math.sin(x * 0.1) * Math.cos(z * 0.1);
		double thicknessThreshold = thickness + (localWobble * (thickness * 0.3));

		return boundaryField < thicknessThreshold;
	}

	private static long hashCoords(int x, int z, long seed) {
		long h = seed + x * 3129841L + z * 116129781L;
		h = (h ^ (h >>> 25)) * 268435459L;
		return h;
	}

	private static boolean replaceBlockWithOakPlague(ServerLevel level, BlockPos pos, BlockState state, boolean breakBedrock) {
		return replaceBlockWithOakPlague(level, pos, state, breakBedrock, isCellularMembrane(pos.getX(), pos.getZ(), level.getSeed()));
	}

	private static boolean replaceBlockWithOakPlague(ServerLevel level, BlockPos pos, BlockState state, boolean breakBedrock, boolean isCell) {
		if (state.isAir()) return false;
		if (!canInfectIndestructible(null) && isIndestructibleBlock(state, level, pos)) {
			if (!breakBedrock) {
				return false;
			}
		}
		Block block = state.getBlock();
		if (block == Blocks.ANCIENT_DEBRIS || isAncientDebris(state, block)) {
			return false;
		}
		if (isIndestructibleBlock(state, level, pos)) {
			level.setBlock(pos, getSplinteredOakState(), INFECTION_BLOCK_FLAGS);
			return true;
		}
		Block petrified = getPetrifiedOakBlock();
		Block splintered = getSplinteredOakBlock();
		Block lignumCaro = getLignumCaroBlock();
		Block falseOak = getFalseOakPlanksBlock();

		// Torches and Redstone Dust must be completely ignored by infection
		if (block instanceof net.minecraft.world.level.block.TorchBlock || block instanceof net.minecraft.world.level.block.WallTorchBlock || block instanceof net.minecraft.world.level.block.RedStoneWireBlock || block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH || block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH || block == Blocks.REDSTONE_WIRE) {
			return false;
		}

		// Slabs infection -> vanilla Oak Planks slab counterpart copying state
		if (state.is(BlockTags.SLABS) || state.getBlock() instanceof net.minecraft.world.level.block.SlabBlock) {
			if (block != Blocks.OAK_SLAB && block != Blocks.PETRIFIED_OAK_SLAB && block != petrified) {
				BlockState oakSlab = Blocks.OAK_SLAB.defaultBlockState();
				if (state.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
					oakSlab = oakSlab.setValue(net.minecraft.world.level.block.SlabBlock.TYPE, state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE));
				}
				if (state.hasProperty(net.minecraft.world.level.block.SlabBlock.WATERLOGGED)) {
					oakSlab = oakSlab.setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED, state.getValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED));
				}
				level.setBlock(pos, oakSlab, INFECTION_BLOCK_FLAGS);
				return true;
			}
			return false;
		}

		// Stairs infection -> vanilla Oak Planks stairs counterpart copying state
		if (state.is(BlockTags.STAIRS) || state.getBlock() instanceof net.minecraft.world.level.block.StairBlock) {
			if (block != Blocks.OAK_STAIRS) {
				BlockState oakStairs = Blocks.OAK_STAIRS.defaultBlockState();
				if (state.hasProperty(net.minecraft.world.level.block.StairBlock.FACING)) {
					oakStairs = oakStairs.setValue(net.minecraft.world.level.block.StairBlock.FACING, state.getValue(net.minecraft.world.level.block.StairBlock.FACING));
				}
				if (state.hasProperty(net.minecraft.world.level.block.StairBlock.HALF)) {
					oakStairs = oakStairs.setValue(net.minecraft.world.level.block.StairBlock.HALF, state.getValue(net.minecraft.world.level.block.StairBlock.HALF));
				}
				if (state.hasProperty(net.minecraft.world.level.block.StairBlock.SHAPE)) {
					oakStairs = oakStairs.setValue(net.minecraft.world.level.block.StairBlock.SHAPE, state.getValue(net.minecraft.world.level.block.StairBlock.SHAPE));
				}
				if (state.hasProperty(net.minecraft.world.level.block.StairBlock.WATERLOGGED)) {
					oakStairs = oakStairs.setValue(net.minecraft.world.level.block.StairBlock.WATERLOGGED, state.getValue(net.minecraft.world.level.block.StairBlock.WATERLOGGED));
				}
				level.setBlock(pos, oakStairs, INFECTION_BLOCK_FLAGS);
				return true;
			}
			return false;
		}

		if (block == Blocks.OAK_PLANKS || block == petrified || block == splintered || block == lignumCaro || block == falseOak || block == Blocks.PETRIFIED_OAK_SLAB) {
			return false;
		}

		if (isCactus(state, block)) {
			level.setBlock(pos, getSplinteredOakState(), INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (isSandstone(state, block)) {
			level.setBlock(pos, getPetrifiedOakState(), INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (isLeafBlock(block, state)) {
			level.setBlock(pos, getFalseOakPlanksState(), INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (!state.getFluidState().isEmpty() || block == Blocks.WATER || block == Blocks.LAVA || block == Blocks.BUBBLE_COLUMN
			|| block == Blocks.SEAGRASS || block == Blocks.TALL_SEAGRASS || block == Blocks.KELP || block == Blocks.KELP_PLANT) {
			if (state.getFluidState().is(FluidTags.LAVA) || block == Blocks.LAVA) {
				level.setBlock(pos, getPetrifiedOakState(), INFECTION_BLOCK_FLAGS);
			} else {
				BlockState chosen = isCell ? getLignumCaroState() : (Math.random() < 0.60 ? getPetrifiedOakState() : getOakPlanksState());
				level.setBlock(pos, chosen, INFECTION_BLOCK_FLAGS);
			}
			return true;
		}

		if (isFoliageOrDebris(state, block)) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), INFECTION_BLOCK_FLAGS);
			return false;
		}

		if (isOreBlock(block, state)) {
			level.setBlock(pos, getPetrifiedOakState(), INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (block == Blocks.IRON_BARS || block == Blocks.CHAIN) {
			level.setBlock(pos, Blocks.OAK_FENCE.defaultBlockState(), INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (block == Blocks.PURPUR_BLOCK || block == Blocks.PURPUR_PILLAR || block == Blocks.PURPUR_STAIRS || block == Blocks.PURPUR_SLAB) {
			level.setBlock(pos, getPetrifiedOakState(), INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (block == Blocks.END_STONE_BRICKS || block == Blocks.END_STONE_BRICK_STAIRS || block == Blocks.END_STONE_BRICK_SLAB || block == Blocks.END_STONE_BRICK_WALL) {
			level.setBlock(pos, getPetrifiedOakState(), INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (block == Blocks.END_STONE) {
			BlockState chosen = isCell ? getLignumCaroState() : (Math.random() < 0.70 ? getOakPlanksState() : getPetrifiedOakState());
			level.setBlock(pos, chosen, INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (isAnySand(state, block) || block == Blocks.GRAVEL || block == Blocks.SUSPICIOUS_GRAVEL) {
			float hardness = state.getDestroySpeed(level, pos);
			if (hardness > 0.5F) {
				level.setBlock(pos, getPetrifiedOakState(), INFECTION_BLOCK_FLAGS);
			} else {
				BlockState chosen = isCell ? getLignumCaroState() : getOakPlanksState();
				level.setBlock(pos, chosen, INFECTION_BLOCK_FLAGS);
			}
			return true;
		}

		if (state.is(BlockTags.DIRT) || block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT ||
			block == Blocks.GRASS_BLOCK || block == Blocks.PODZOL || block == Blocks.MYCELIUM || block == Blocks.MUD ||
			block == Blocks.MUDDY_MANGROVE_ROOTS || block == Blocks.FARMLAND || block == Blocks.DIRT_PATH || block == Blocks.MOSS_BLOCK ||
			block == Blocks.CLAY) {
			BlockState chosen = isCell ? getLignumCaroState() : getOakPlanksState();
			level.setBlock(pos, chosen, INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (block == Blocks.NETHERRACK || block == Blocks.CRIMSON_NYLIUM || block == Blocks.WARPED_NYLIUM ||
			block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL || block == Blocks.MAGMA_BLOCK) {
			BlockState chosen = isCell ? getLignumCaroState() : (Math.random() < 0.60 ? getPetrifiedOakState() : getOakPlanksState());
			level.setBlock(pos, chosen, INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (state.is(BlockTags.ICE) || block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE ||
			block == Blocks.SNOW_BLOCK || block == Blocks.POWDER_SNOW) {
			BlockState chosen = isCell ? getLignumCaroState() : getPetrifiedOakState();
			level.setBlock(pos, chosen, INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_BRICKS) || state.is(BlockTags.TERRACOTTA) ||
			block == Blocks.STONE || block == Blocks.COBBLESTONE || block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE ||
			block == Blocks.GRANITE || block == Blocks.DIORITE || block == Blocks.ANDESITE || block == Blocks.TUFF ||
			block == Blocks.CALCITE || block == Blocks.DRIPSTONE_BLOCK || block == Blocks.BLACKSTONE || block == Blocks.SMOOTH_BASALT ||
			block == Blocks.BASALT || block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.AMETHYST_BLOCK) {
			level.setBlock(pos, getPetrifiedOakState(), INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (block == Blocks.BEDROCK && breakBedrock && canInfectIndestructible(null)) {
			level.setBlock(pos, getPetrifiedOakState(), INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (state.is(BlockTags.LOGS)) {
			BlockState oakLog = Blocks.OAK_LOG.defaultBlockState();
			if (state.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)) {
				oakLog = oakLog.setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, state.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS));
			}
			level.setBlock(pos, oakLog, INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_FENCES)) {
			double r = Math.random();
			BlockState chosen = r < 0.65 ? getOakPlanksState() : (r < 0.85 ? getPetrifiedOakState() : getLignumCaroState());
			level.setBlock(pos, chosen, INFECTION_BLOCK_FLAGS);
			return true;
		}

		if (isBurntBlock(state)) {
			BlockState healthy = getHealthyEquivalent(state);
			level.setBlock(pos, healthy, INFECTION_BLOCK_FLAGS);
			return true;
		}

		return false;
	}

	private static Block getRegisteredBlock(String id, Block fallback) {
		try {
			ResourceLocation loc = ResourceLocation.parse(id);
			if (BuiltInRegistries.BLOCK.containsKey(loc)) return BuiltInRegistries.BLOCK.get(loc);
		} catch (Exception ignored) {}
		return fallback;
	}

	private static void sendCitadelCameraShake(float intensity, int duration, float range) {
		sendCitadelCameraShakeAt(null, 0, 0, 0, intensity, duration, range, true);
	}

	private static void sendCitadelCameraShakeAt(ServerLevel level, double x, double y, double z, float maxIntensity, int duration, float maxRadius) {
		sendCitadelCameraShakeAt(level, x, y, z, maxIntensity, duration, maxRadius, false);
	}

	private static void sendCitadelCameraShakeAt(ServerLevel level, double x, double y, double z, float maxIntensity, int duration, float maxRadius, boolean broadcastAll) {
		try {
			Class<?> msgClass = Class.forName("com.github.alexmodguy.citadel.server.message.CameraShakeMessage");
			Class<?> citadelClass = Class.forName("com.github.alexmodguy.citadel.Citadel");

			if (broadcastAll || level == null) {
				Object msgInstance = createCameraShakeMessage(msgClass, maxIntensity, duration, maxRadius);
				if (msgInstance != null) {
					java.lang.reflect.Method sendMethod = citadelClass.getMethod("sendMSGToAll", Object.class);
					sendMethod.invoke(null, msgInstance);
				}
				return;
			}

			java.lang.reflect.Method sendToPlayer = null;
			java.lang.reflect.Method sendToAll = null;
			try {
				for (java.lang.reflect.Method m : citadelClass.getMethods()) {
					if (m.getName().equals("sendMSGToPlayer") && m.getParameterCount() == 2) {
						sendToPlayer = m;
					} else if (m.getName().equals("sendMSGToAll") && m.getParameterCount() == 1) {
						sendToAll = m;
					}
				}
			} catch (Exception ignored) {}

			for (net.minecraft.server.level.ServerPlayer player : level.players()) {
				double dx = player.getX() - x;
				double dy = player.getY() - y;
				double dz = player.getZ() - z;
				double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
				if (dist <= maxRadius) {
					// Linear decay over 48 sphere radius at tip of the beam
					float decay = (float) (1.0 - (dist / maxRadius));
					float scaledIntensity = maxIntensity * decay;
					if (scaledIntensity > 0.02F) {
						Object msgInstance = createCameraShakeMessage(msgClass, scaledIntensity, duration, maxRadius);
						if (msgInstance != null) {
							if (sendToPlayer != null) {
								sendToPlayer.invoke(null, msgInstance, player);
							} else if (sendToAll != null) {
								sendToAll.invoke(null, msgInstance);
								break;
							}
						}
					}
				}
			}
		} catch (Exception ignored) {}
	}

	private static Object createCameraShakeMessage(Class<?> msgClass, float intensity, int duration, float range) {
		for (java.lang.reflect.Constructor<?> c : msgClass.getConstructors()) {
			Class<?>[] p = c.getParameterTypes();
			try {
				if (p.length == 3) {
					if (p[0] == float.class && p[1] == int.class) return c.newInstance(intensity, duration, range);
					if (p[0] == int.class && p[1] == float.class) return c.newInstance(duration, intensity, range);
					if (p[0] == float.class && p[1] == float.class) return c.newInstance(intensity, (float) duration, range);
				} else if (p.length == 2) {
					if (p[0] == float.class && p[1] == int.class) return c.newInstance(intensity, duration);
					if (p[0] == int.class && p[1] == float.class) return c.newInstance(duration, intensity);
					if (p[0] == float.class && p[1] == float.class) return c.newInstance(intensity, (float) duration);
				}
			} catch (Exception ignored) {}
		}
		return null;
	}

	private static boolean isVerdantEngine(Entity entity) {
		if (entity == null) return false;
		String descId = entity.getType().getDescriptionId();
		String className = entity.getClass().getSimpleName();
		return descId.contains("verdant") || className.contains("Verdant");
	}

	private static void playEngineSound(ServerLevel level, double x, double y, double z, String soundId, float volume, float pitch) {
		try {
			ResourceLocation loc = ResourceLocation.parse(soundId);
			SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(loc);
			if (sound != null) level.playSound(null, x, y, z, sound, SoundSource.HOSTILE, volume, pitch);
		} catch (Exception ignored) {}
	}

	private static final Map<Block, Boolean> BURNT_CACHE = new java.util.IdentityHashMap<>();

	private static void processShockwaveDominoEffects(ServerLevel level, double ox, double oy, double oz, double radius, double expandSpeed, double scanTopY) {
		int samplePoints = Math.max(18, (int) (2 * Math.PI * radius * 0.6));
		double dr = Math.max(1.5, expandSpeed * 0.6);
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

		int lastChunkX = Integer.MIN_VALUE;
		int lastChunkZ = Integer.MIN_VALUE;
		LevelChunk chunk = null;

		for (int i = 0; i < samplePoints; i++) {
			double a = (2 * Math.PI / samplePoints) * i;
			double c = Math.cos(a);
			double s = Math.sin(a);

			for (double rOffset = -dr; rOffset <= dr; rOffset += 2.5) {
				double curR = radius + rOffset;
				if (curR < 1.0) continue;
				double px = ox + c * curR;
				double pz = oz + s * curR;
				int ipx = Mth.floor(px);
				int ipz = Mth.floor(pz);
				int cx = ipx >> 4;
				int cz = ipz >> 4;

				if (cx != lastChunkX || cz != lastChunkZ) {
					lastChunkX = cx;
					lastChunkZ = cz;
					chunk = level.getChunkSource().getChunkNow(cx, cz);
				}
				if (chunk == null) continue;
				int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ipx & 15, ipz & 15);
				double ceilY = findCeilingAbove(level, ipx, ipz, Math.max(level.getMinBuildHeight() + 2.0, oy));
				if (ceilY < surfaceY && ceilY > oy) {
					surfaceY = (int) findGroundBelow(level, ipx, ipz, Math.min(ceilY - 1.0, oy + 15.0));
				}

				for (int dy = -3; dy <= 4; dy++) {
					p.set(ipx, surfaceY + dy, ipz);
					if (!level.hasChunkAt(p)) continue;

					BlockState state = chunk.getBlockState(p);

					// 1. Domino fire extinguishing in wood plains biome only
					if (isFire(state)) {
						Holder<net.minecraft.world.level.biome.Biome> bHolder = level.getBiome(p);
						if (bHolder.is(ResourceLocation.parse("the_backwoods:wood_plains")) || bHolder.is(ResourceLocation.parse("thebackwoods:wood_plains"))) {
							level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
							level.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.15, 0.15, 0.15, 0.05);
							if (level.random.nextFloat() < 0.20f) {
								playEngineSound(level, p.getX(), p.getY(), p.getZ(), "block.fire.extinguish", 0.4F, 1.1F + level.random.nextFloat() * 0.3F);
							}
						}
					}

					// 2. Domino burnt block replacement
					if (isBurntBlock(state)) {
						BlockState restored = getHealthyEquivalent(state);
						level.setBlock(p, restored, INFECTION_BLOCK_FLAGS);
						level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.05);
						if (level.random.nextFloat() < 0.12f) {
							playEngineSound(level, p.getX(), p.getY(), p.getZ(), "block.cherry_wood.place", 0.6F, 0.9F + level.random.nextFloat() * 0.3F);
						}
					}
				}
			}
		}
	}

	private static boolean isFire(BlockState state) {
		if (state == null || state.isAir()) return false;
		Block block = state.getBlock();
		Boolean cached = IS_FIRE_CACHE.get(block);
		if (cached != null) return cached;

		boolean result = false;
		ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(block);
		if (reg != null) {
			String namespace = reg.getNamespace();
			String path = reg.getPath().toLowerCase();
			if (namespace.equals("burnt") && (path.equals("wood_fire") || path.startsWith("wood_fire_"))) {
				result = true;
			}
		}
		if (!result) {
			result = block instanceof BaseFireBlock
					|| state.is(Blocks.FIRE)
					|| state.is(Blocks.SOUL_FIRE);
		}
		IS_FIRE_CACHE.put(block, result);
		return result;
	}

	private static boolean isBurntBlock(BlockState state) {
		if (state == null || state.isAir()) return false;
		Block block = state.getBlock();
		Boolean cached = BURNT_CACHE.get(block);
		if (cached != null) return cached;

		boolean result = false;
		ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(block);
		if (reg != null) {
			String namespace = reg.getNamespace();
			String path = reg.getPath().toLowerCase();

			if (!(namespace.equals("burnt") && (path.equals("wood_fire") || path.startsWith("wood_fire_")))) {
				if (!path.contains("brick") && !path.contains("stone") && !path.contains("tile") && !path.contains("glass") && !path.contains("metal") && !path.contains("ore")) {
					if (namespace.equals("burnt") || namespace.equals("burnt_basic") || namespace.equals("firesdelight") || namespace.equals("nether_delight") || path.contains("burnt") || path.contains("smoldering") || path.contains("sooty") || path.contains("charred")) {
						result = true;
					}
				}
			}
		}
		BURNT_CACHE.put(block, result);
		return result;
	}

	private static BlockState getHealthyEquivalent(BlockState burntState) {
		if (burntState == null) {
			return getLignumCaroState();
		}
		Block block = burntState.getBlock();
		ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(block);
		if (reg != null) {
			String path = reg.getPath().toLowerCase();

			if (path.contains("broken_log")) {
				Block healthyBlock = null;
				boolean isLog = path.contains("log") || path.contains("stem") || path.contains("wood") || path.contains("hyphae") || path.contains("bark");
				boolean isPlanks = path.contains("plank");

				if (path.contains("spruce")) {
					healthyBlock = isLog ? Blocks.SPRUCE_LOG : (isPlanks ? Blocks.SPRUCE_PLANKS : Blocks.SPRUCE_WOOD);
				} else if (path.contains("birch")) {
					healthyBlock = isLog ? Blocks.BIRCH_LOG : (isPlanks ? Blocks.BIRCH_PLANKS : Blocks.BIRCH_WOOD);
				} else if (path.contains("jungle")) {
					healthyBlock = isLog ? Blocks.JUNGLE_LOG : (isPlanks ? Blocks.JUNGLE_PLANKS : Blocks.JUNGLE_WOOD);
				} else if (path.contains("acacia")) {
					healthyBlock = isLog ? Blocks.ACACIA_LOG : (isPlanks ? Blocks.ACACIA_PLANKS : Blocks.ACACIA_WOOD);
				} else if (path.contains("dark_oak")) {
					healthyBlock = isLog ? Blocks.DARK_OAK_LOG : (isPlanks ? Blocks.DARK_OAK_PLANKS : Blocks.DARK_OAK_WOOD);
				} else if (path.contains("mangrove")) {
					healthyBlock = isLog ? Blocks.MANGROVE_LOG : (isPlanks ? Blocks.MANGROVE_PLANKS : Blocks.MANGROVE_WOOD);
				} else if (path.contains("cherry")) {
					healthyBlock = isLog ? Blocks.CHERRY_LOG : (isPlanks ? Blocks.CHERRY_PLANKS : Blocks.CHERRY_WOOD);
				} else {
					healthyBlock = isLog ? Blocks.OAK_LOG : (isPlanks ? Blocks.OAK_PLANKS : Blocks.OAK_WOOD);
				}

				if (healthyBlock != null) {
					BlockState healthyState = healthyBlock.defaultBlockState();
					return copyBlockStateProperties(burntState, healthyState);
				}
			}
		}

		BlockState defaultState = Math.random() < 0.65 ? getOakPlanksState() : getLignumCaroState();
		return copyBlockStateProperties(burntState, defaultState);
	}

	private static boolean isInfectedBiome(ServerLevel level, BlockPos pos) {
		if (level == null || pos == null) return false;
		try {
			Holder<net.minecraft.world.level.biome.Biome> b = level.getBiome(pos);
			if (b != null) {
				if (b.is(ResourceLocation.parse("the_backwoods:wood_plains")) || b.is(ResourceLocation.parse("thebackwoods:wood_plains"))) {
					return true;
				}
				if (b.unwrapKey().isPresent()) {
					String id = b.unwrapKey().get().location().toString().toLowerCase();
					if (id.contains("wood_plain") || id.contains("backwood") || id.contains("verdant")) {
						return true;
					}
				}
				ResourceLocation key = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(b.value());
				if (key != null) {
					String id = key.toString().toLowerCase();
					if (id.contains("wood_plain") || id.contains("backwood") || id.contains("verdant")) {
						return true;
					}
				}
			}
		} catch (Throwable ignored) {}
		return false;
	}

	private static boolean isColumnInfected(ServerLevel level, int sx, int sz, double ty) {
		if (level == null) return false;
		int chunkX = sx >> 4;
		int chunkZ = sz >> 4;
		LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
		int surfaceY = chunk != null ? chunk.getHeight(Heightmap.Types.WORLD_SURFACE, sx & 15, sz & 15) : (int) ty;

		// 1. Biome checks (surface terrain, sub-surface, and engine hover altitude)
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(sx, surfaceY, sz);
		if (isInfectedBiome(level, p)) return true;
		p.set(sx, Math.max(level.getMinBuildHeight() + 1, surfaceY - 3), sz);
		if (isInfectedBiome(level, p)) return true;
		p.set(sx, Mth.floor(ty), sz);
		if (isInfectedBiome(level, p)) return true;

		// 2. Physical block infection check at surface (recognizes infected areas in ANY dimension)
		if (chunk != null) {
			Block petrified = getPetrifiedOakBlock();
			Block splintered = getSplinteredOakBlock();
			Block lignumCaro = getLignumCaroBlock();
			Block falseOak = getFalseOakPlanksBlock();
			for (int y = surfaceY; y >= Math.max(level.getMinBuildHeight() + 1, surfaceY - 5); y--) {
				p.set(sx, y, sz);
				BlockState st = chunk.getBlockState(p);
				if (st.isAir()) continue;
				Block b = st.getBlock();
				if (b == Blocks.OAK_PLANKS || b == Blocks.OAK_LOG || b == petrified || b == splintered || b == lignumCaro || b == falseOak || b == Blocks.PETRIFIED_OAK_SLAB) {
					return true;
				}
			}
		}
		return false;
	}

	private static void handleVerdantMusic(ServerLevel level, Entity entity, CompoundTag nbt, double tx, double ty, double tz) {
		if (level == null || entity == null || !entity.isAlive()) return;

		// Periodically stop standard background/biome music (SoundSource.MUSIC) for all players in range
		int tickEpoch = (int) level.getGameTime();
		if (tickEpoch % 20 == 0) {
			AABB musicStopBox = new AABB(tx - 128.0, ty - 64.0, tz - 128.0, tx + 128.0, ty + 64.0, tz + 128.0);
			List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, musicStopBox, p -> p.isAlive() && !p.isSpectator());
			for (ServerPlayer p : nearbyPlayers) {
				try {
					p.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(null, SoundSource.MUSIC));
				} catch (Throwable ignored) {}
			}
		}

		int musicTimer = persistentInt(nbt, "verdant_music_timer", 0);
		if (musicTimer > 0) {
			nbt.putInt("verdant_music_timer", musicTimer - 1);
			return;
		}

		if (tickEpoch % 20 != 0) return;

		AABB searchBox = new AABB(tx - 128.0, ty - 64.0, tz - 128.0, tx + 128.0, ty + 64.0, tz + 128.0);
		List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, searchBox, p -> p.isAlive() && !p.isSpectator());

		ServerPlayer targetPlayer = null;
		for (ServerPlayer p : players) {
			long playerMusicLockUntil = persistentLong(p.getPersistentData(), "verdant_global_music_until", 0L);
			if (level.getGameTime() < playerMusicLockUntil) continue; // Player is already listening to music!

			targetPlayer = p;
			break;
		}

		if (targetPlayer != null) {
			int lastTrack = persistentInt(nbt, "verdant_last_music_track", 0);
			int nextTrack;
			if (lastTrack == 1) {
				nextTrack = 2;
			} else if (lastTrack == 2) {
				nextTrack = 1;
			} else {
				nextTrack = Math.random() < 0.5 ? 1 : 2;
			}

			String soundName = nextTrack == 1 ? "the_backwoods:caretaker_patience_1" : "the_backwoods:caretaker_patience_2";
			int trackDurationTicks = nextTrack == 1 ? 4900 : 6100; // 4:05 (4900 ticks) vs 5:05 (6100 ticks)

			try {
				ResourceLocation soundRes = ResourceLocation.parse(soundName);
				SoundEvent soundEvent = null;
				Object rawRes = BuiltInRegistries.SOUND_EVENT.get(soundRes);
				if (rawRes instanceof java.util.Optional<?> opt) {
					soundEvent = (SoundEvent) opt.map(o -> o instanceof net.minecraft.core.Holder<?> h ? h.value() : o).orElse(null);
				} else if (rawRes instanceof SoundEvent se) {
					soundEvent = se;
				}
				if (soundEvent == null) {
					soundEvent = SoundEvent.createVariableRangeEvent(soundRes);
				}

				// Stop standard MUSIC for the targeted player first
				try {
					targetPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(null, SoundSource.MUSIC));
				} catch (Throwable ignored) {}

				if (soundEvent != null) {
					// Play exclusively to targetPlayer as non-spatial client UI music using SoundSource.RECORDS
					// This prevents it from being stopped by our periodic SoundSource.MUSIC stopper!
					targetPlayer.playNotifySound(soundEvent, SoundSource.RECORDS, 1.0F, 1.0F);
				}
			} catch (Throwable ignored) {}

			targetPlayer.getPersistentData().putLong("verdant_global_music_until", level.getGameTime() + trackDurationTicks);
			nbt.putInt("verdant_last_music_track", nextTrack);
			nbt.putInt("verdant_music_timer", trackDurationTicks);
		}
	}

	private static BlockState copyBlockStateProperties(BlockState from, BlockState to) {
		BlockState result = to;
		for (Property<?> property : from.getProperties()) {
			if (result.hasProperty(property)) {
				result = copyPropertyHelper(from, result, property);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> BlockState copyPropertyHelper(BlockState from, BlockState to, Property<T> property) {
		return to.setValue(property, from.getValue(property));
	}

	private static final float[] SIN_LOOKUP = new float[1024];
	private static final float[] COS_LOOKUP = new float[1024];
	static {
		for (int i = 0; i < 1024; i++) {
			double rad = (i / 1024.0) * Math.PI * 2.0;
			SIN_LOOKUP[i] = (float) Math.sin(rad);
			COS_LOOKUP[i] = (float) Math.cos(rad);
		}
	}

	private static double fastSin(double rad) {
		int idx = ((int) (rad * (1024.0 / (Math.PI * 2.0)))) & 1023;
		return SIN_LOOKUP[idx];
	}

	private static double fastCos(double rad) {
		int idx = ((int) (rad * (1024.0 / (Math.PI * 2.0)))) & 1023;
		return COS_LOOKUP[idx];
	}

	private static int persistentInt(CompoundTag tag, String key, int fallback) {
		return tag.contains(key) ? tag.getInt(key) : fallback;
	}

	private static double persistentDouble(CompoundTag tag, String key, double fallback) {
		return tag != null && tag.contains(key) ? tag.getDouble(key) : fallback;
	}

	private static boolean persistentBoolean(CompoundTag tag, String key, boolean fallback) {
		return tag != null && tag.contains(key) ? tag.getBoolean(key) : fallback;
	}

	private static long persistentLong(CompoundTag tag, String key, long fallback) {
		return tag != null && tag.contains(key) ? tag.getLong(key) : fallback;
	}

	private static float persistentFloat(CompoundTag tag, String key, float fallback) {
		return tag != null && tag.contains(key) ? tag.getFloat(key) : fallback;
	}
}
// 1.21.1
