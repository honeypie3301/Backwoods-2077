package net.mcreator.thebackwoods.world.feature;

// 1.21.1 neo
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

public class CatwalkSubCavernFeature extends Feature<NoneFeatureConfiguration> {

    public CatwalkSubCavernFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // --- CONFIGURABLE CAVERN & MAZE SETTINGS ---
        int floorY = 45;   // Strict, solid floor height
        int ceilingY = 60; // Absolute ceiling height of the room
        int catwalkY = 53; // Fixed altitude for the floating maze walkways

        // Density modifier (0.0f = completely empty room, 1.0f = max pathways)
        float mazeDensity = 0.55f; 

        // Path settings
        int pathWidth = 2;
        int cellSize = 6; 
        // --------------------------------------------

        BlockState airState = Blocks.AIR.defaultBlockState();
        BlockState plankState = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState fenceState = Blocks.OAK_FENCE.defaultBlockState();

        // Phase 1: Initialize the Cavern Space (Strict Floor & Air room)
        for (int rx = 0; rx < 16; rx++) {
            for (int rz = 0; rz < 16; rz++) {
                int blockX = origin.getX() + rx;
                int blockZ = origin.getZ() + rz;

                for (int y = floorY + 1; y <= ceilingY; y++) {
                    level.setBlock(new BlockPos(blockX, y, blockZ), airState, 16);
                }
                level.setBlock(new BlockPos(blockX, floorY, blockZ), plankState, 16);
            }
        }

        // Phase 2: Deterministic Maze Generation
        int startCellX = Math.floorDiv(origin.getX(), cellSize);
        int startCellZ = Math.floorDiv(origin.getZ(), cellSize);
        int endCellX = Math.floorDiv(origin.getX() + 16, cellSize);
        int endCellZ = Math.floorDiv(origin.getZ() + 16, cellSize);

        for (int cx = startCellX; cx <= endCellX; cx++) {
            for (int cz = startCellZ; cz <= endCellZ; cz++) {
                
                long cellSeed = (long) cx * 341873128712L + (long) cz * 1328979813L;
                random.setSeed(cellSeed);

                List<Direction> dirs = new ArrayList<>();
                dirs.add(Direction.NORTH);
                dirs.add(Direction.SOUTH);
                dirs.add(Direction.EAST);
                dirs.add(Direction.WEST);

                for (int i = dirs.size() - 1; i > 0; i--) {
                    int index = random.nextInt(i + 1);
                    Direction a = dirs.get(index);
                    dirs.set(index, dirs.get(i));
                    dirs.set(i, a);
                }

                int openCount = 0;
                for (Direction dir : dirs) {
                    if (openCount < 2) {
                        if (random.nextFloat() < mazeDensity) {
                            // Generate the walking platform segment
                            generateDeterministicPath(level, origin, cx, cz, dir, catwalkY, plankState, cellSize, pathWidth);
                            
                            // Drop occasional fence supports down from the center node block of active paths
                            generateVerticalFenceSupport(level, origin, cx, cz, catwalkY, floorY, fenceState, cellSize);
                        }
                        openCount++;
                    }
                }
            }
        }

        return true;
    }

    private void generateDeterministicPath(WorldGenLevel level, BlockPos origin, int cellX, int cellZ, Direction dir, int y, BlockState state, int cellSize, int pathWidth) {
        int centerX = (cellX * cellSize) + (cellSize / 2);
        int centerZ = (cellZ * cellSize) + (cellSize / 2);

        int halfWidth = pathWidth / 2;
        int remainingWidth = pathWidth - halfWidth;
        int length = cellSize;

        int minX = origin.getX();
        int maxX = origin.getX() + 15;
        int minZ = origin.getZ();
        int maxZ = origin.getZ() + 15;

        if (dir == Direction.NORTH || dir == Direction.SOUTH) {
            int zStart = (dir == Direction.NORTH) ? centerZ - length : centerZ;
            int zEnd = (dir == Direction.NORTH) ? centerZ : centerZ + length;
            
            for (int z = zStart; z < zEnd; z++) {
                for (int xOffset = -halfWidth; xOffset < remainingWidth; xOffset++) {
                    int targetX = centerX + xOffset;
                    if (targetX >= minX && targetX <= maxX && z >= minZ && z <= maxZ) {
                        level.setBlock(new BlockPos(targetX, y, z), state, 16);
                    }
                }
            }
        } else if (dir == Direction.EAST || dir == Direction.WEST) {
            int xStart = (dir == Direction.WEST) ? centerX - length : centerX;
            int xEnd = (dir == Direction.WEST) ? centerX : centerX + length;

            for (int x = xStart; x < xEnd; x++) {
                for (int zOffset = -halfWidth; zOffset < remainingWidth; zOffset++) {
                    int targetZ = centerZ + zOffset;
                    if (x >= minX && x <= maxX && targetZ >= minZ && targetZ <= maxZ) {
                        level.setBlock(new BlockPos(x, y, targetZ), state, 16);
                    }
                }
            }
        }
    }

    /**
     * Drops vertical lines of oak fences down safely from the center of active catwalk nodes to the strict floor.
     */
    private void generateVerticalFenceSupport(WorldGenLevel level, BlockPos origin, int cellX, int cellZ, int catwalkY, int floorY, BlockState fenceState, int cellSize) {
        int centerX = (cellX * cellSize) + (cellSize / 2);
        int centerZ = (cellZ * cellSize) + (cellSize / 2);

        // Define our current local chunk boundaries
        int minX = origin.getX();
        int maxX = origin.getX() + 15;
        int minZ = origin.getZ();
        int maxZ = origin.getZ() + 15;

        // Verify the node center point falls inside the actively decorating chunk to eliminate warning logs
        if (centerX >= minX && centerX <= maxX && centerZ >= minZ && centerZ <= maxZ) {
            // Drop fence columns straight down to the plank floor
            for (int dy = catwalkY - 1; dy > floorY; dy--) {
                level.setBlock(new BlockPos(centerX, dy, centerZ), fenceState, 16);
            }
        }
    }
}