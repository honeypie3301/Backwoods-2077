package net.mcreator.thebackwoods;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

public class LabyrinthineGridsMaze extends Feature<NoneFeatureConfiguration> {
    private static final net.minecraft.world.level.block.state.BlockState OAK_PLANKS_STATE = Blocks.OAK_PLANKS.defaultBlockState();
    private static final net.minecraft.world.level.block.state.BlockState AIR_STATE = Blocks.AIR.defaultBlockState();

    public LabyrinthineGridsMaze() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        
        int size = 16;           
        int minWallHeight = 56;
        int maxWallHeight = 100;
        int seaLevel = 63;       

        int[][] columnHeights = new int[size][size];
        boolean[][] isCarved = new boolean[size][size];

        // 1. Fill the chunk with a Randomized Skyline
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                // Determine a unique height for THIS specific 1x1 column
                columnHeights[x][z] = minWallHeight + random.nextInt(maxWallHeight - minWallHeight + 1);
            }
        }

        // 2. High-Density Carver
        for (int x = 1; x < size - 1; x += 2) {
            for (int z = 1; z < size - 1; z += 2) {
                
                // Carve the current cell (We use maxWallHeight to ensure the path is clear to the top)
                markCarved(isCarved, x, z);

                if (random.nextFloat() > 0.3) {
                    markCarved(isCarved, x + 1, z);
                }
                
                if (random.nextFloat() > 0.3) {
                    markCarved(isCarved, x, z + 1);
                }

                if (random.nextFloat() > 0.9) {
                    markCarved(isCarved, x + 1, z + 1);
                }
            }
        }

        // Single-pass world execution to eliminate redundant block overwrites and GC pressure
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int originX = origin.getX();
        int originZ = origin.getZ();

        for (int x = 0; x < size; x++) {
            int blockX = originX + x;
            for (int z = 0; z < size; z++) {
                int blockZ = originZ + z;

                if (isCarved[x][z]) {
                    for (int y = 0; y < maxWallHeight; y++) {
                        pos.set(blockX, seaLevel + y, blockZ);
                        world.setBlock(pos, AIR_STATE, 2);
                    }
                } else {
                    int height = columnHeights[x][z];
                    for (int y = 0; y < height; y++) {
                        pos.set(blockX, seaLevel + y, blockZ);
                        world.setBlock(pos, OAK_PLANKS_STATE, 2);
                    }
                }
            }
        }

        return true;
    }

    private static void markCarved(boolean[][] isCarved, int localX, int localZ) {
        if (localX < 16 && localZ < 16) {
            isCarved[localX][localZ] = true;
        }
        if (localX + 1 < 16 && localZ < 16) {
            isCarved[localX + 1][localZ] = true;
        }
    }

    private void carvePillar(WorldGenLevel world, int x, int z, int seaLevel, int height) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < height; y++) {
            // Carving air up to the maximum possible height to ensure no "ceilings" are left behind
            pos.set(x, seaLevel + y, z);
            world.setBlock(pos, AIR_STATE, 2);
            pos.set(x + 1, seaLevel + y, z);
            world.setBlock(pos, AIR_STATE, 2);
        }
    } // 1.21.1
}