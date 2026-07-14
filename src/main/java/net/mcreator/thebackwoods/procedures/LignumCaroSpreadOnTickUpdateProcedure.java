package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.IdentityHashMap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;

public class LignumCaroSpreadOnTickUpdateProcedure {
	// ---------- CONFIGURABLE SPREAD & DEFENSE SETTINGS ----------
	private static final double SPREAD_CHANCE = 0.35;
	private static final int SPREAD_DELAY_TICKS = 7;
	private static final long INFECTION_START_DELAY_TICKS = 1200L;
	private static final double SNEEZE_RANGE = 4.0;
	private static final double SNEEZE_CHANCE = 0.40;
	private static final float SNEEZE_MIN_DAMAGE = 1.0F;
	private static final float SNEEZE_MAX_DAMAGE = 2.0F;
	private static final double CLEANSING_CHANCE = 0.25;
	private static final double COLONY_SPREAD_BASE_CHANCE = 0.15;
	private static final int REGEN_WAVE_DELAY_MIN = 2;
	private static final int REGEN_WAVE_DELAY_MAX = 4;

	// Performance Caches (IdentityHashMap for O(1) Block lookup without registry string overhead)
	private static final Map<Block, Boolean> BURNT_CACHE = new IdentityHashMap<>();
	private static final Map<Block, Boolean> AIR_OR_VEG_CACHE = new IdentityHashMap<>();
	private static final Map<Block, Boolean> SPORE_CACHE = new IdentityHashMap<>();

	private static boolean isSporeBlock(Block block) {
		Boolean cached = SPORE_CACHE.get(block);
		if (cached != null) {
			return cached;
		}
		boolean result = false;
		ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(block);
		if (reg != null && reg.getNamespace().equals("spore")) {
			result = true;
		}
		SPORE_CACHE.put(block, result);
		return result;
	}

	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof Level level)) {
			return;
		}

		if (level.getGameTime() < INFECTION_START_DELAY_TICKS) {
			return;
		}

		BlockPos sourcePos = BlockPos.containing(x, y, z);
		BlockState sourceState = world.getBlockState(sourcePos);
		if (sourceState.isAir()) {
			return;
		}

		RandomSource random = world.getRandom();
		if (random.nextFloat() < 0.60) {
			return;
		}

		int sx = sourcePos.getX();
		int sy = sourcePos.getY();
		int sz = sourcePos.getZ();

		Block customBlock = TheBackwoodsModBlocks.LIGNUM_CARO.get();
		BlockState defaultSpreadState = customBlock.defaultBlockState();

		Property<?> infectedProp = sourceState.getBlock().getStateDefinition().getProperty("infected");
		boolean isInfected = false;
		BooleanProperty boolProp = null;

		if (infectedProp instanceof BooleanProperty bProp) {
			boolProp = bProp;
			isInfected = sourceState.getValue(bProp);
		}

		if (isInfected) {
			BlockState infectedSpreadState = defaultSpreadState;
			if (boolProp != null) {
				infectedSpreadState = defaultSpreadState.setValue(boolProp, true);
			}

			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}

						BlockPos targetPos = new BlockPos(sx + dx, sy + dy, sz + dz);
						if (world.isOutsideBuildHeight(targetPos)) {
							continue;
						}

						BlockState targetState = world.getBlockState(targetPos);
						if (targetState.isAir()) {
							continue;
						}

						if (isSporeBlock(targetState.getBlock())) {
							if (random.nextDouble() <= SPREAD_CHANCE) {
								world.setBlock(targetPos, infectedSpreadState, 3);
								world.scheduleTick(targetPos, customBlock, SPREAD_DELAY_TICKS);
							}
						}
					}
				}
			}
		} else {
			if (random.nextDouble() <= SNEEZE_CHANCE) {
				double range = SNEEZE_RANGE;
				AABB scanArea = new AABB(
					x - range, y - range, z - range,
					x + range, y + range, z + range
				);
				List<LivingEntity> nearbySporeEntities = level.getEntitiesOfClass(LivingEntity.class, scanArea, entity -> {
					ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
					return rl != null && rl.getNamespace().equals("spore");
				});

				if (!nearbySporeEntities.isEmpty()) {
					boolean sneezed = false;
					for (LivingEntity target : nearbySporeEntities) {
						if (target.isAlive()) {
							sneezed = true;
							float damage = SNEEZE_MIN_DAMAGE + random.nextFloat() * (SNEEZE_MAX_DAMAGE - SNEEZE_MIN_DAMAGE);
							target.hurt(level.damageSources().magic(), damage);

							Vec3 blockCenter = new Vec3(x + 0.5, y + 0.5, z + 0.5);
							Vec3 pushVec = target.position().subtract(blockCenter).normalize();
							target.setDeltaMovement(target.getDeltaMovement().add(pushVec.x * 0.4, 0.25, pushVec.z * 0.4));
							target.hurtMarked = true;

							target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0, false, false));

							if (level instanceof ServerLevel serverLevel) {
								serverLevel.sendParticles(ParticleTypes.SNEEZE, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
							}
						}
					}

					if (sneezed) {
						level.playSound(null, sourcePos, SoundEvents.LLAMA_SPIT, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.4F);
						level.playSound(null, sourcePos, SoundEvents.BAMBOO_WOOD_BREAK, SoundSource.BLOCKS, 0.6F, 1.2F + random.nextFloat() * 0.4F);
						
						if (level instanceof ServerLevel serverLevel) {
							serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 1.0, z + 0.5, 12, 0.5, 0.5, 0.5, 0.1);
						}
					}
				}
			}

			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}

						BlockPos targetPos = new BlockPos(sx + dx, sy + dy, sz + dz);
						if (world.isOutsideBuildHeight(targetPos)) {
							continue;
						}

						BlockState targetState = world.getBlockState(targetPos);
						if (targetState.isAir()) {
							continue;
						}

						if (isSporeBlock(targetState.getBlock())) {
							if (random.nextDouble() <= SPREAD_CHANCE) {
								BlockState pristineSpreadState = defaultSpreadState;
								if (boolProp != null) {
									pristineSpreadState = defaultSpreadState.setValue(boolProp, false);
								}
								world.setBlock(targetPos, pristineSpreadState, 3);
								world.scheduleTick(targetPos, customBlock, SPREAD_DELAY_TICKS);
								level.playSound(null, targetPos, SoundEvents.BAMBOO_WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F + random.nextFloat() * 0.2F);
								
								if (level instanceof ServerLevel serverLevel) {
									serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
								}
							}
						} else if (targetState.is(customBlock) && boolProp != null) {
							boolean targetInfected = targetState.getValue(boolProp);
							if (targetInfected && random.nextDouble() <= CLEANSING_CHANCE) {
								BlockState cleansedState = defaultSpreadState.setValue(boolProp, false);
								world.setBlock(targetPos, cleansedState, 3);
								level.playSound(null, targetPos, SoundEvents.CHERRY_WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 1.1F + random.nextFloat() * 0.2F);
								
								if (level instanceof ServerLevel serverLevel) {
									serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.02);
								}
							}
						}
					}
				}
			}

			performColonyGrowth(world, sourcePos, random);
		}
	}

	private static void performColonyGrowth(LevelAccessor world, BlockPos currentPos, RandomSource random) {
		Block customBlock = TheBackwoodsModBlocks.LIGNUM_CARO.get();
		int burntCount = countNearbyBurntBlocks(world, currentPos);
		double adaptiveChance = Math.min(1.0, COLONY_SPREAD_BASE_CHANCE + (burntCount * 0.17));
		if (burntCount > 0 && random.nextDouble() > adaptiveChance) {
			return;
		}
		performBurntCompatibility(world, currentPos, random, customBlock);
	}

	private static int countNearbyBurntBlocks(LevelAccessor world, BlockPos pos) {
		int count = 0;
		for (Direction dir : Direction.values()) {
			BlockPos checkPos = pos.relative(dir);
			if (!world.isOutsideBuildHeight(checkPos)) {
				BlockState state = world.getBlockState(checkPos);
				if (isBurntBlock(state)) {
					count++;
				}
			}
		}
		return count;
	}

	private static boolean isReplaceableForGrowth(BlockState state) {
		if (state == null) {
			return true;
		}
		return state.isAir()
			|| state.is(Blocks.VINE)
			|| state.is(Blocks.OAK_LEAVES)
			|| state.is(Blocks.CHERRY_LEAVES)
			|| state.is(Blocks.SPRUCE_LEAVES)
			|| state.is(Blocks.BIRCH_LEAVES)
			|| state.is(Blocks.JUNGLE_LEAVES)
			|| state.is(Blocks.ACACIA_LEAVES)
			|| state.is(Blocks.DARK_OAK_LEAVES)
			|| state.is(Blocks.MANGROVE_LEAVES)
			|| state.is(Blocks.GLOW_LICHEN)
			|| state.is(Blocks.SHORT_GRASS)
			|| state.is(Blocks.TALL_GRASS)
			|| state.is(Blocks.FERN)
			|| state.is(Blocks.LARGE_FERN);
	}

	private static int countHealthyNeighbors(LevelAccessor world, BlockPos pos, Block customBlock) {
		int count = 0;
		int px = pos.getX();
		int py = pos.getY();
		int pz = pos.getZ();
		Property<?> infectedProp = customBlock.getStateDefinition().getProperty("infected");

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue;
					}
					BlockPos checkPos = new BlockPos(px + dx, py + dy, pz + dz);
					if (!world.isOutsideBuildHeight(checkPos)) {
						BlockState state = world.getBlockState(checkPos);
						if (state.is(customBlock)) {
							boolean isInfected = false;
							if (infectedProp instanceof BooleanProperty boolProp) {
								isInfected = state.getValue(boolProp);
							}
							if (!isInfected) {
								count++;
							}
						}
					}
				}
			}
		}
		return count;
	}

	private static void performBurntCompatibility(LevelAccessor world, BlockPos currentPos, RandomSource random, Block customBlock) {
		for (int attempt = 0; attempt < 6; attempt++) {
			double roll = random.nextDouble();
			Direction dir;
			if (roll < 0.70) {
				Direction[] horizontals = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
				dir = horizontals[random.nextInt(horizontals.length)];
			} else if (roll < 0.90) {
				dir = Direction.UP;
			} else {
				dir = Direction.DOWN;
			}

			BlockPos targetPos = currentPos.relative(dir);
			if (world.isOutsideBuildHeight(targetPos)) {
				continue;
			}

			BlockState targetState = world.getBlockState(targetPos);
			if (isAirOrVegetation(targetState)) {
				continue;
			}

			if (isBurntBlock(targetState)) {
				BlockState restoredState = getHealthyEquivalent(targetState);
				world.setBlock(targetPos, restoredState, 3);

				List<BlockPos> neighborList = new ArrayList<>();
				for (Direction d : Direction.values()) {
					BlockPos neighborPos = targetPos.relative(d);
					if (!world.isOutsideBuildHeight(neighborPos)) {
						BlockState neighborState = world.getBlockState(neighborPos);
						if (neighborState.is(customBlock)) {
							neighborList.add(neighborPos);
						}
					}
				}

				if (!neighborList.isEmpty()) {
					java.util.Collections.shuffle(neighborList, new java.util.Random(random.nextLong()));
					int toSchedule = Math.min(1 + random.nextInt(2), neighborList.size());
					int delay = REGEN_WAVE_DELAY_MIN + random.nextInt(REGEN_WAVE_DELAY_MAX - REGEN_WAVE_DELAY_MIN + 1);
					for (int s = 0; s < toSchedule; s++) {
						world.scheduleTick(neighborList.get(s), customBlock, delay);
					}
				}

				if (random.nextFloat() < 0.08 && world instanceof Level level) {
					level.playSound(null, targetPos, SoundEvents.CHERRY_WOOD_BREAK, SoundSource.BLOCKS, 0.7F, 1.1F + random.nextFloat() * 0.2F);
				}

				if (world instanceof ServerLevel serverLevel) {
					serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.05);
				}
			}
		}
	}

	private static boolean isAirOrVegetation(BlockState state) {
		if (state == null || state.isAir()) {
			return true;
		}
		Block block = state.getBlock();
		Boolean cached = AIR_OR_VEG_CACHE.get(block);
		if (cached != null) {
			return cached;
		}

		boolean result = false;
		ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(block);
		if (reg != null) {
			String path = reg.getPath().toLowerCase();
			if (path.contains("leaves") || path.contains("grass") || path.contains("flower") || path.contains("sapling") || path.contains("fern") || path.contains("vine") || path.contains("crop") || path.contains("mushroom") || path.contains("bush") || path.contains("plant") || path.contains("shrub") || path.contains("water") || path.contains("lava") || path.contains("fire")) {
				result = true;
			}
		}
		AIR_OR_VEG_CACHE.put(block, result);
		return result;
	}

	private static boolean isBurntBlock(BlockState state) {
		if (state == null || state.isAir()) {
			return false;
		}
		Block block = state.getBlock();
		Boolean cached = BURNT_CACHE.get(block);
		if (cached != null) {
			return cached;
		}

		boolean result = false;
		ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(block);
		if (reg != null) {
			String namespace = reg.getNamespace();
			String path = reg.getPath().toLowerCase();

			if (!(namespace.equals("burnt") && (path.equals("wood_fire") || path.matches("wood_fire_[2-9]")))) {
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
			return TheBackwoodsModBlocks.LIGNUM_CARO.get().defaultBlockState();
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

		BlockState defaultState = TheBackwoodsModBlocks.LIGNUM_CARO.get().defaultBlockState();
		return copyBlockStateProperties(burntState, defaultState);
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
} // 1.21.1
