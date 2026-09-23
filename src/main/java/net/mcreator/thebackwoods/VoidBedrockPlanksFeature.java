package net.mcreator.thebackwoods.world.feature;

// 1.21.1 neoforge compatible
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

public class VoidBedrockPlanksFeature extends Feature<NoneFeatureConfiguration> {

    public VoidBedrockPlanksFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level  = context.level();
        BlockPos      origin = context.origin();

        // --- CONFIGURABLE BEDROCK VOID SETTINGS ---
        int minBedrockY = -64; // Bottom of the world
        int maxBedrockY = -59; // Top of the default bedrock layer
        // ------------------------------------------

        BlockState plankState = Blocks.OAK_PLANKS.defaultBlockState();

        // Target the raw chunk layer data directly
        ChunkAccess chunk = level.getChunk(origin);
        int minX = origin.getX() & (~15);
        int minZ = origin.getZ() & (~15);

        // Derive a unique seed from the current chunk layout coordinates
        long chunkSeed = ((long) (minX >> 4) * 418731283L) ^ ((long) (minZ >> 4) * 13289791L);
        Random random = new Random(chunkSeed);

        // Decide a random center point for our oak plank blob within this chunk boundary
        int blobCenterX = minX + random.nextInt(16);
        int blobCenterY = minBedrockY + random.nextInt(maxBedrockY - minBedrockY + 1);
        int blobCenterZ = minZ + random.nextInt(16);
        
        // Randomize the size/radius of the plank blob (3 to 5 blocks wide)
        double blobRadius = 3.0 + random.nextDouble() * 2.5;

        // Single-pass direct overwrite loop across the bedrock layer footprint
        for (int rx = 0; rx < 16; rx++) {
            for (int rz = 0; rz < 16; rz++) {
                int blockX = minX + rx;
                int blockZ = minZ + rz;

                for (int currentY = minBedrockY; currentY <= maxBedrockY; currentY++) {
                    BlockPos targetPos = new BlockPos(blockX, currentY, blockZ);

                    // Calculate 3D Euclidean distance to the blob center
                    double dx = blockX - blobCenterX;
                    double dy = currentY - blobCenterY;
                    double dz = blockZ - blobCenterZ;
                    
                    // Flattened ellipsoidal blob distribution math
                    double distanceSquared = (dx * dx) + (dy * dy * 1.5) + (dz * dz);

                    if (distanceSquared <= (blobRadius * blobRadius)) {
                        // If inside the math radius, punch out the bedrock and replace it with oak planks
                        chunk.setBlockState(targetPos, plankState, false);
                    }
                    // REMOVED THE ELSE PASS: If it's not inside the blob, we don't touch it.
                    // This lets the vanilla bedrock layer remain exactly as it generated originally.
                }
            }
        }

        return true;
    }
}