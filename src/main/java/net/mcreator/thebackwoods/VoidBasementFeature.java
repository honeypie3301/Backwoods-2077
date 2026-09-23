package net.mcreator.thebackwoods.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class VoidBasementFeature extends Feature<NoneFeatureConfiguration> {

    public VoidBasementFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level  = context.level();
        BlockPos      origin = context.origin();

        // --- CONFIGURABLE MINESHAFTS SETTINGS ---
        int minVoidY        = -50;
        int maxVoidY        = -13; 
        int fixedCatwalkY   = -34; 
        
        int catwalkWidth    = 2;  // Adjustable width of the walkways
        int trackFrequency  = 24; // Distance between parallel tracks
        // ----------------------------------------

        BlockState plankState = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState fenceState = Blocks.OAK_FENCE.defaultBlockState();
        BlockState airState   = Blocks.AIR.defaultBlockState();

        // Direct chunk manipulation optimization
        ChunkAccess chunk = level.getChunk(origin);
        int minX = origin.getX() & (~15);
        int minZ = origin.getZ() & (~15);

        // Single-pass direct block-buffer generation pipeline
        for (int rx = 0; rx < 16; rx++) {
            for (int rz = 0; rz < 16; rz++) {
                int blockX = minX + rx;
                int blockZ = minZ + rz;

                // Step functions to create sharp 90-degree offsets (Square Snakes)
                int squareStepX = ((Math.floorMod(blockZ, 128) < 64) ? 24 : 0);
                int squareStepZ = ((Math.floorMod(blockX, 128) < 64) ? 24 : 0);

                // Grid mapping verification logic
                boolean inEastWestPath   = Math.floorMod(blockZ - squareStepZ, trackFrequency) < catwalkWidth;
                boolean inNorthSouthPath = Math.floorMod(blockX - squareStepX, trackFrequency) < catwalkWidth;
                boolean isCatwalkPath    = inEastWestPath || inNorthSouthPath;

                // Check support fence conditions along grid ticks (placed every 5 blocks)
                boolean isFenceColumn = isCatwalkPath && ((blockX % 5 == 0 && inEastWestPath) || (blockZ % 5 == 0 && inNorthSouthPath));

                for (int currentY = minVoidY; currentY <= maxVoidY; currentY++) {
                    BlockPos targetPos = new BlockPos(blockX, currentY, blockZ);

                    if (currentY == minVoidY) {
                        // 1. Strict Floor Overwrite
                        chunk.setBlockState(targetPos, plankState, false);
                    } else if (currentY == maxVoidY) {
                        // 2. Strict Ceiling Overwrite
                        chunk.setBlockState(targetPos, plankState, false);
                    } else if (currentY == fixedCatwalkY) {
                        // 3. Catwalk Layer Block Injection
                        if (isCatwalkPath) {
                            chunk.setBlockState(targetPos, plankState, false);
                        } else {
                            chunk.setBlockState(targetPos, airState, false);
                        }
                    } else if (currentY < fixedCatwalkY && isFenceColumn) {
                        // 4. Downward Support Columns
                        chunk.setBlockState(targetPos, fenceState, false);
                    } else {
                        // 5. Total Structural Clearance (Forcefully voids any encroaching terrain noise/maze walls)
                        chunk.setBlockState(targetPos, airState, false);
                    }
                }
            }
        }

        return true;
    }
}