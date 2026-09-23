package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class ScandereLignumLogOnTickUpdateProcedure {

	// GROWTH SPEED SETTING: Controls the growth rate (from 0.0 to 5.0+)
	// - 1.0 = Default standard growth rate (100% speed)
	// - 0.5 = Half speed (50% slower)
	// - 2.0 = Double speed (2x rate)
	// - 5.0 = Five times speed (5x rate, maximum recommended)
	// - 0.0 = Completely disables growth
	public static final double GROWTH_SPEED = 1.0;

	public static void execute() {
	}

	public static void execute(LevelAccessor world, double x, double y, double z) {
		// Fallback if called without blockstate dependency
		BlockPos currentPos = new BlockPos((int) x, (int) y, (int) z);
		BlockState state = world.getBlockState(currentPos);
		execute(world, x, y, z, state);
	}

	public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
		if (world == null || blockstate == null) {
			return;
		}

		if (GROWTH_SPEED <= 0.0) {
			return;
		}

		RandomSource random = world.getRandom();

		// Handle growth speed (supporting values from 0 to 5+):
		// - Values < 1.0 execute probabilistically
		// - Values >= 1.0 run the growth logic multiple times per tick simulation
		int fullRuns = (int) GROWTH_SPEED;
		double fractionalChance = GROWTH_SPEED - fullRuns;

		for (int i = 0; i < fullRuns; i++) {
			runGrowthTick(world, x, y, z, blockstate, random);
		}

		if (fractionalChance > 0.0 && random.nextDouble() < fractionalChance) {
			runGrowthTick(world, x, y, z, blockstate, random);
		}
	}

	private static void runGrowthTick(LevelAccessor world, double x, double y, double z, BlockState blockstate, RandomSource random) {
		BlockPos currentPos = new BlockPos((int) x, (int) y, (int) z);
		boolean isWoodbound = world.getBiome(currentPos).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("the_backwoods", "woodbound_biomes")));

		// 1. Age the active block for realistic, finite growth
		Property<?> rawAgeProp = blockstate.getBlock().getStateDefinition().getProperty("age");
		int currentAge = 0;
		int maxAge = 450;
		boolean reachedMaxAge = false;
		if (rawAgeProp instanceof IntegerProperty ageProperty) {
			currentAge = blockstate.getValue(ageProperty);
			
			// Dynamically get the max age from the property
			for (Integer val : ageProperty.getPossibleValues()) {
				if (val > maxAge) maxAge = val;
			}

			if (currentAge >= maxAge) {
				reachedMaxAge = true;
			} else {
				currentAge++;
				blockstate = blockstate.setValue(ageProperty, currentAge);
				// Flag 2 prevents unnecessary neighbor block updates for simple age changes
				world.setBlock(currentPos, blockstate, 2);
			}
		}

		// 2. Oozing Resin Logic
		Property<?> rawOozingProp = blockstate.getBlock().getStateDefinition().getProperty("oozing");
		if (rawOozingProp instanceof net.minecraft.world.level.block.state.properties.BooleanProperty oozingProperty) {
			boolean isOozing = blockstate.getValue(oozingProperty);
			if (!isOozing && currentAge >= 75) {
				// Calculate chance: scales up to 80% (0.80f) as age approaches maxAge
				float chance = 0.80f * ((float)(currentAge - 75) / (maxAge - 75));
				
				// Only let exposed blocks ooze (must have air horizontally adjacent)
				boolean isExposed = false;
				for (Direction dir : Direction.Plane.HORIZONTAL) {
					if (world.getBlockState(currentPos.relative(dir)).isAir()) {
						isExposed = true;
						break;
					}
				}
				
				if (isExposed && random.nextFloat() < chance) {
					blockstate = blockstate.setValue(oozingProperty, true);
					world.setBlock(currentPos, blockstate, 3);
				}
			}
		}

		if (reachedMaxAge) {
			return; // Block has reached maximum maturity and stops growing
		}

		// Check block above and below early for optimization
		boolean hasLogAbove = world.getBlockState(currentPos.above()).is(blockstate.getBlock());
		boolean hasLogBelow = world.getBlockState(currentPos.below()).is(blockstate.getBlock());

		// OPTIMIZATION 1: If this block is deeply buried inside the trunk, it does not grow leaves or branches.
		// Exiting here prevents massive lag from leaf-checking on the 50-80 tall trunk.
		if (hasLogAbove && hasLogBelow) {
			// If there is wood at least 8 blocks above us, skip branching/canopy logic.
			if (world.getBlockState(currentPos.above(8)).is(blockstate.getBlock())) {
				return;
			}
		}

		boolean isTip = !hasLogAbove;

		// 1. OPTIMIZATION & PRECISION: Calculate absolute height from the soil/ground
		// We scan up to 100 blocks down to reliably locate the ground for super tall trees.
		BlockPos.MutableBlockPos groundScan = new BlockPos.MutableBlockPos(currentPos.getX(), currentPos.getY(), currentPos.getZ());
		int heightFromSoil = 0;
		while (heightFromSoil < 100) {
			groundScan.move(0, -1, 0);
			BlockState scanState = world.getBlockState(groundScan);
			if (scanState.isAir() || scanState.is(Blocks.OAK_LEAVES) || scanState.is(TheBackwoodsModBlocks.FALSE_OAK_PLANKS.get()) || scanState.is(Blocks.VINE) || scanState.is(blockstate.getBlock())) {
				heightFromSoil++;
			} else {
				// We hit dirt, grass, stone, sand, sand etc. This is the actual ground.
				break;
			}
		}

		// Calculate local trunk height straight below us (for local logic)
		BlockPos.MutableBlockPos tempPos = new BlockPos.MutableBlockPos(currentPos.getX(), currentPos.getY(), currentPos.getZ());
		int localTrunkHeight = 1;
		while (localTrunkHeight < 12) {
			tempPos.move(0, -1, 0);
			BlockState downState = world.getBlockState(tempPos);
			if (downState.is(blockstate.getBlock())) {
				localTrunkHeight++;
			} else {
				break;
			}
		}

		// 2. Trunk growth (Ascendance)
		if (isTip) {
			// Limit maximum height naturally to a colossal range (50 to 80 blocks tall)
			int maxAllowedHeight = 50 + random.nextInt(31); 
			if (heightFromSoil < maxAllowedHeight) {
				BlockPos abovePos = currentPos.above();
				BlockState aboveState = world.getBlockState(abovePos);
				if (isReplaceable(aboveState)) {
					// Grow trunk upwards! Preserve the log block state
					world.setBlock(abovePos, blockstate, 3);
					
					// Spawn growth particles and play wood place sound
					if (world instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.COMPOSTER, abovePos.getX() + 0.5, abovePos.getY() + 0.5, abovePos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.05);
						serverLevel.playSound(null, abovePos, SoundEvents.CHERRY_WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 0.85f);
					}
					return; // Trunk grew! Let's exit this tick update
				}
			}

			// 3. Sprout horizontal branches (15% chance if tree has scaled high enough)
			// This generates gorgeous massive branches mainly on the mid-to-high sections of the giant tree.
			if (heightFromSoil >= 10 && random.nextFloat() < 0.15f) {
				Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
				BlockPos branchPos = currentPos.relative(dir);
				BlockState branchState = world.getBlockState(branchPos);
				if (isReplaceable(branchState)) {
					BlockState orientedState = blockstate;
					if (blockstate.hasProperty(BlockStateProperties.AXIS)) {
						orientedState = blockstate.setValue(BlockStateProperties.AXIS, dir.getAxis());
					}
					world.setBlock(branchPos, orientedState, 3);
					
					if (world instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.COMPOSTER, branchPos.getX() + 0.5, branchPos.getY() + 0.5, branchPos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.02);
						serverLevel.playSound(null, branchPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8f, 0.95f);
					}
				}
			}
		}

		// 4. Canopy leaf growth (Oak leaves spawn, forming a majestic dense crown at the top - Sequoia/Redwood style)
		// We scan upwards to check if we are within the top 12 blocks of the trunk (the active canopy crown).
		int blocksAbove = 0;
		BlockPos.MutableBlockPos tipScan = new BlockPos.MutableBlockPos(currentPos.getX(), currentPos.getY(), currentPos.getZ());
		while (blocksAbove < 12) {
			tipScan.move(0, 1, 0);
			if (world.getBlockState(tipScan).is(blockstate.getBlock())) {
				blocksAbove++;
			} else {
				break;
			}
		}

		// Only grow beautiful crown foliage in the top 12 blocks of the trunk!
		// This keeps the lower/middle 80% of the monolithic giant trunk clean and pristine.
		if (blocksAbove < 12 && heightFromSoil >= 3) {
			// Periodically grow oak leaves around this log segment
			int dx = random.nextInt(5) - 2; // -2 to 2
			int dy = random.nextInt(4) - 1; // -1 to 2
			int dz = random.nextInt(5) - 2; // -2 to 2
			BlockPos leafPos = currentPos.offset(dx, dy, dz);
			double distSq = dx * dx + dy * dy + dz * dz;

			// Define a beautiful natural canopy shape (radius limit of 7.5)
			if (distSq <= 7.5) {
				BlockState leafState = world.getBlockState(leafPos);
				if (leafState.isAir() || leafState.is(Blocks.VINE)) {
					BlockState placedLeafState;
					if (isWoodbound) {
						placedLeafState = TheBackwoodsModBlocks.FALSE_OAK_PLANKS.get().defaultBlockState();
					} else {
						placedLeafState = Blocks.OAK_LEAVES.defaultBlockState();
						if (placedLeafState.hasProperty(BlockStateProperties.PERSISTENT)) {
							placedLeafState = placedLeafState.setValue(BlockStateProperties.PERSISTENT, false);
						}
						// Calculate decay distance property relative to closest wood block (which is currentPos)
						if (placedLeafState.hasProperty(BlockStateProperties.DISTANCE)) {
							int dist = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
							int finalDist = Math.max(1, Math.min(7, dist));
							placedLeafState = placedLeafState.setValue(BlockStateProperties.DISTANCE, finalDist);
						}
					}
					world.setBlock(leafPos, placedLeafState, 3);

					// AESTHETIC ENHANCEMENT: Grow beautiful hanging Vines underneath this newly created leaf block!
					if (random.nextFloat() < 0.15f) { // 15% chance
						BlockPos belowLeaf = leafPos.below();
						if (world.getBlockState(belowLeaf).isAir()) {
							// Place natural draping vine block on the proper side orientation
							BlockState vineState = Blocks.VINE.defaultBlockState();
							if (vineState.hasProperty(BlockStateProperties.UP)) {
								vineState = vineState.setValue(BlockStateProperties.UP, true);
							}
							world.setBlock(belowLeaf, vineState, 3);
						}
					}
				}
			}
		}

		// 5. Climbing Walls (Ascend along adjacent solid structures)
		// Optimization: Climbing only places a block directly above us, which is only possible if we are the tip.
		if (isTip && heightFromSoil < 80) {
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				BlockPos wallPos = currentPos.relative(dir);
				BlockState wallState = world.getBlockState(wallPos);
				if (wallState.isRedstoneConductor(world, wallPos) && !wallState.is(blockstate.getBlock())) {
					// We are adjacent to a solid wall. Let's climb up!
					BlockPos climbPos = currentPos.above();
					BlockState climbState = world.getBlockState(climbPos);
					if (isReplaceable(climbState)) {
						// 10% chance to climb on random tick
						if (random.nextFloat() < 0.10f) {
							world.setBlock(climbPos, blockstate, 3);
							if (world instanceof ServerLevel serverLevel) {
								serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, climbPos.getX() + 0.5, climbPos.getY() + 0.5, climbPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.01);
							}
						}
					}
				}
			}
		}
	}

	private static boolean isReplaceable(BlockState state) {
		return state.isAir() 
			|| state.is(Blocks.VINE) 
			|| state.is(Blocks.OAK_LEAVES) 
			|| state.is(TheBackwoodsModBlocks.FALSE_OAK_PLANKS.get())
			|| state.is(Blocks.GLOW_LICHEN) 
			|| state.is(Blocks.SHORT_GRASS) 
			|| state.is(Blocks.TALL_GRASS) 
			|| state.is(Blocks.FERN) 
			|| state.is(Blocks.LARGE_FERN);
	}
} // 1.21.1