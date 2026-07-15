package net.mcreator.thebackwoods.procedures;
// 1.21.1 neo
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class ScandereLignumSpreadOnTickUpdateProcedure {
	// Simulated tickrate target (20 = normal MC, 50 = faster spread behavior)
	private static final int DESIRED_TPS = 50;

	// Fertilization influence from nearby passive animals
	private static final int FERTILIZE_ANIMAL_RADIUS = 6;
	private static final int FERTILIZE_MAX_DELAY_REDUCTION = 4;

	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (world == null) return;

		RandomSource random = (world instanceof ServerLevel sl) ? sl.getRandom() : RandomSource.create();
		BlockPos origin = BlockPos.containing(x, y, z);
		BlockState currentState = world.getBlockState(origin);
		
		int currentAge = 0;
		IntegerProperty ageProperty = null;
		for (Property<?> prop : currentState.getProperties()) {
			if (prop.getName().equals("age") && prop instanceof IntegerProperty) {
				ageProperty = (IntegerProperty) prop;
				currentAge = currentState.getValue(ageProperty);
				break;
			}
		}
		if (currentAge >= 10) return;

		boolean openSky = world.canSeeSky(origin);

		// Direction bias
		int roll = Mth.nextInt(random, 0, 99);
		int dir;
		if (openSky) {
			// mostly horizontal on surface
			if (roll < 80) {
				int h = Mth.nextInt(random, 0, 3);
				dir = switch (h) {
					case 0 -> 0; // +X
					case 1 -> 1; // -X
					case 2 -> 4; // +Z
					default -> 5; // -Z
				};
			} else if (roll < 92) {
				dir = 3; // down a bit
			} else {
				dir = 2; // rare up
			}
		} else {
			// underground roots: down + horizontal
			if (roll < 60) dir = 3;
			else if (roll < 95) {
				int h = Mth.nextInt(random, 0, 3);
				dir = switch (h) {
					case 0 -> 0;
					case 1 -> 1;
					case 2 -> 4;
					default -> 5;
				};
			} else dir = 2;
		}

		BlockPos target = switch (dir) {
			case 0 -> origin.east();
			case 1 -> origin.west();
			case 2 -> origin.above();
			case 3 -> origin.below();
			case 4 -> origin.south();
			default -> origin.north();
		};

		// Base speed model
		int spreadDelay = 1;
		if (!openSky) spreadDelay += 1;
		if (dir == 3) spreadDelay = Math.max(1, spreadDelay - 4);

		// Animal fertilization: nearby passive entities accelerate spread a bit
		int nearbyAdults = countNearbyAdultAnimals(world, origin, FERTILIZE_ANIMAL_RADIUS);
		int fertilizeBoost = Math.min(FERTILIZE_MAX_DELAY_REDUCTION, nearbyAdults);
		spreadDelay = Math.max(1, spreadDelay - fertilizeBoost);

		BlockState targetState = world.getBlockState(target);
		if (world.getBlockEntity(target) != null) return;

		// Basic exclusions
		if (targetState.isAir() || !world.getFluidState(target).isEmpty()) return;

		net.minecraft.world.level.block.Block tBlock = targetState.getBlock();
		boolean isValidTarget = tBlock == Blocks.DIRT 
				|| tBlock == Blocks.COARSE_DIRT 
				|| tBlock == Blocks.ROOTED_DIRT 
				|| tBlock == Blocks.GRASS_BLOCK
				|| tBlock == Blocks.ANDESITE 
				|| tBlock == Blocks.GRANITE 
				|| tBlock == Blocks.DIORITE
				|| tBlock == Blocks.CLAY 
				|| tBlock == Blocks.SAND 
				|| tBlock == Blocks.RED_SAND
				|| tBlock == Blocks.STONE;

		if (!isValidTarget) return;

		// Vein-shape control
		int scandereNeighbors = countScandereNeighbors(world, target);

		// hard anti-blob cap
		if (scandereNeighbors >= 2) return;

		boolean goodVeinConnection = (scandereNeighbors == 1);
		boolean rareBranch = (scandereNeighbors == 1 && random.nextInt(12) == 0);

		if (!goodVeinConnection && !rareBranch) return;

		BlockState targetPlacementState = TheBackwoodsModBlocks.SCANDERE_LIGNUM.get().defaultBlockState();
		if (ageProperty != null && targetPlacementState.hasProperty(ageProperty)) {
			targetPlacementState = targetPlacementState.setValue(ageProperty, currentAge + 1);
		}
		world.setBlock(target, targetPlacementState, 3);

		// Simulate faster tickrate (e.g., 50 TPS instead of 20 TPS)
		int spreadDelayAdjusted = Math.max(1, Mth.ceil(spreadDelay * (20.0 / DESIRED_TPS)));
		world.scheduleTick(target, TheBackwoodsModBlocks.SCANDERE_LIGNUM.get(), spreadDelayAdjusted);
	}

	private static int countScandereNeighbors(LevelAccessor world, BlockPos pos) {
		int count = 0;
		if (world.getBlockState(pos.east()).getBlock() == TheBackwoodsModBlocks.SCANDERE_LIGNUM.get()) count++;
		if (world.getBlockState(pos.west()).getBlock() == TheBackwoodsModBlocks.SCANDERE_LIGNUM.get()) count++;
		if (world.getBlockState(pos.above()).getBlock() == TheBackwoodsModBlocks.SCANDERE_LIGNUM.get()) count++;
		if (world.getBlockState(pos.below()).getBlock() == TheBackwoodsModBlocks.SCANDERE_LIGNUM.get()) count++;
		if (world.getBlockState(pos.south()).getBlock() == TheBackwoodsModBlocks.SCANDERE_LIGNUM.get()) count++;
		if (world.getBlockState(pos.north()).getBlock() == TheBackwoodsModBlocks.SCANDERE_LIGNUM.get()) count++;
		return count;
	}

	private static int countNearbyAdultAnimals(LevelAccessor world, BlockPos pos, int radius) {
		if (!(world instanceof ServerLevel level)) return 0;
		AABB box = AABB.ofSize(
				new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
				radius * 2.0, radius * 2.0, radius * 2.0
		);
		return level.getEntitiesOfClass(Animal.class, box, a -> a.isAlive() && !a.isBaby()).size();
	}
}