package net.mcreator.thebackwoods.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CavernGridFeature extends Feature<NoneFeatureConfiguration> {

    public CavernGridFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level  = context.level();
        BlockPos      origin = context.origin();
        RandomSource  random = context.random();

        // --- TUNABLE DEEP CAVERN CONSTANTS ---
        int minCavernY          = -12;   
        int maxCavernY          = 3;     
        float pillarProbability = 0.005f; // DECREASED: Creates significantly more open horizontal space
        // --------------------------------------------

        BlockState floorBlock   = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState plankBlock   = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState fenceBlock   = Blocks.OAK_FENCE.defaultBlockState();
        BlockState airBlock     = Blocks.AIR.defaultBlockState();

        // Phase 1: Carve out the clean open cavern slab room from Y = -12 to Y = 3
        for (int rx = 0; rx < 16; rx++) {
            for (int rz = 0; rz < 16; rz++) {
                int blockX = origin.getX() + rx;
                int blockZ = origin.getZ() + rz;

                for (int currentY = minCavernY; currentY <= maxCavernY; currentY++) {
                    BlockPos targetPos = new BlockPos(blockX, currentY, blockZ);
                    
                    if (currentY == minCavernY) {
                        level.setBlock(targetPos, floorBlock, 2); 
                    } else {
                        level.setBlock(targetPos, airBlock, 2);  
                    }
                }
            }
        }

        // Phase 2: Place scattered support columns & architectural scaffolding variants
        for (int rx = 0; rx < 16; rx++) {
            for (int rz = 0; rz < 16; rz++) {
                int blockX = origin.getX() + rx;
                int blockZ = origin.getZ() + rz;

                long columnSeed = (long) blockX * 4961821041L + (long) blockZ * 324157877L;
                random.setSeed(columnSeed);

                if (random.nextFloat() < pillarProbability) {
                    // Randomly choose between Variant 1 (Random Size Fence Pillar) and Variant 2 (Solid Plank Pillar)
                    int variant = random.nextInt(2) + 1;

                    switch (variant) {
                        case 1 -> { // --- VARIANT 1: CUSTOM RANDOM SIZE FENCE PILLAR (CENTERED) ---
                            int widthX = random.nextInt(5) + 1; // 1 to 5
                            int lengthZ = random.nextInt(5) + 1; // 1 to 5

                            // Center the randomized dimensions to sit evenly around the target column coordinate
                            int startOffsetX = -(widthX / 2);
                            int startOffsetZ = -(lengthZ / 2);

                            for (int dx = 0; dx < widthX; dx++) {
                                for (int dz = 0; dz < lengthZ; dz++) {
                                    int targetLocalX = rx + startOffsetX + dx;
                                    int targetLocalZ = rz + startOffsetZ + dz;

                                    // Safely stay completely within current chunk limits
                                    if (targetLocalX >= 0 && targetLocalX < 16 && targetLocalZ >= 0 && targetLocalZ < 16) {
                                        int pX = origin.getX() + targetLocalX;
                                        int pZ = origin.getZ() + targetLocalZ;
                                        
                                        for (int currentY = minCavernY + 1; currentY <= maxCavernY; currentY++) {
                                            level.setBlock(new BlockPos(pX, currentY, pZ), fenceBlock, 3);
                                        }
                                    }
                                }
                            }
                        }
                        case 2 -> { // --- VARIANT 2: SOLID PLANKS ONLY (Standard 1x1 Column) ---
                            for (int currentY = minCavernY + 1; currentY <= maxCavernY; currentY++) {
                                level.setBlock(new BlockPos(blockX, currentY, blockZ), plankBlock, 2);
                            }
                        }
                    }
                }
            }
        }

        // Phase 3: Post-processing pass to manually stitch all adjacent horizontal fence connections
        for (int rx = 0; rx < 16; rx++) {
            for (int rz = 0; rz < 16; rz++) {
                int blockX = origin.getX() + rx;
                int blockZ = origin.getZ() + rz;

                for (int currentY = minCavernY + 1; currentY <= maxCavernY; currentY++) {
                    BlockPos pos = new BlockPos(blockX, currentY, blockZ);
                    BlockState currentState = level.getBlockState(pos);

                    if (currentState.is(Blocks.OAK_FENCE)) {
                        boolean north = level.getBlockState(pos.north()).is(Blocks.OAK_FENCE) || level.getBlockState(pos.north()).isSolid();
                        boolean south = level.getBlockState(pos.south()).is(Blocks.OAK_FENCE) || level.getBlockState(pos.south()).isSolid();
                        boolean east  = level.getBlockState(pos.east()).is(Blocks.OAK_FENCE)  || level.getBlockState(pos.east()).isSolid();
                        boolean west  = level.getBlockState(pos.west()).is(Blocks.OAK_FENCE)  || level.getBlockState(pos.west()).isSolid();

                        BlockState connectedState = currentState
                            .setValue(FenceBlock.NORTH, north)
                            .setValue(FenceBlock.SOUTH, south)
                            .setValue(FenceBlock.EAST, east)
                            .setValue(FenceBlock.WEST, west);

                        level.setBlock(pos, connectedState, 2);
                    }
                }
            }
        }

        return true;
    }
}