package net.mcreator.thebackwoods.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class LignumColonyFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceLocation LIGNUM_CARO_RL = ResourceLocation.fromNamespaceAndPath("the_backwoods", "lignum_caro");

    public LignumColonyFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        long seed = level.getSeed();

        BlockState colonyBlock = BuiltInRegistries.BLOCK.get(LIGNUM_CARO_RL).defaultBlockState();
        if (colonyBlock.isAir()) {
            return false;
        }

        // =========================================================================
        // CONFIGURABLE COLONY ADJUSTMENTS
        // =========================================================================
        // Higher value = smaller, more compact cells. Lower value = massive sprawling cells.
        // Try values like 0.015 for medium-large, 0.025 for medium-small.
        double baseCellScale = 0.026; 
        
        // Controls how wide/thick the actual lines of Lignum Caro generate.
        double lineThickness = 0.045; 
        
        // How deep the it embeds down into the ground layers (conforming to slopes)
        int placementDepth = 45;
        // =========================================================================

        // Direct memory chunk targeting matching the MengerSponge architecture
        ChunkAccess chunk = level.getChunk(origin);
        int minX = origin.getX() & (~15);
        int minZ = origin.getZ() & (~15);

        boolean generatedAny = false;

        // Structured 16x16 column space loop to ensure absolute chunk alignment
        for (int rx = 0; rx < 16; rx++) {
            for (int rz = 0; rz < 16; rz++) {
                int worldX = minX + rx;
                int worldZ = minZ + rz;

                // Determine cellular membrane structure using a massive scaled-up field
                if (isCellularMembrane(worldX, worldZ, seed, baseCellScale, lineThickness)) {
                    int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, worldX, worldZ) - 1;
                    
                    if (surfaceY > level.getMinBuildHeight()) {
                        // Generate down into the terrain to make sure steep slopes stay perfectly connected
                        for (int depth = 0; depth < placementDepth; depth++) {
                            BlockPos targetPos = new BlockPos(worldX, surfaceY - depth, worldZ);
                            
                            if (targetPos.getY() > level.getMinBuildHeight()) {
                                BlockState existingBlock = chunk.getBlockState(targetPos);
                                if (!existingBlock.isAir()) {
                                    chunk.setBlockState(targetPos, colonyBlock, false);
                                    generatedAny = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return generatedAny;
    }

    /**
     * Determines whether a coordinate belongs to a continuous scaled-up cell membrane.
     */
    private boolean isCellularMembrane(double x, double z, long seed, double baseScale, double thickness) {
        // Subtle macro shifting to keep structures dynamic and unpredictable
        double macroNoise = Math.sin(x * (baseScale * 0.1) + seed) * Math.cos(z * (baseScale * 0.1) - seed);
        double activeScale = baseScale + (macroNoise * (baseScale * 0.3)); 

        double scaledX = x * activeScale;
        double scaledZ = z * activeScale;

        int cellX = (int) Math.floor(scaledX);
        int cellZ = (int) Math.floor(scaledZ);

        double minDistance1 = Double.MAX_VALUE;
        double minDistance2 = Double.MAX_VALUE;

        // Inspect neighboring cell grids
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int targetGridX = cellX + i;
                int targetGridZ = cellZ + j;

                long hash = hashCoords(targetGridX, targetGridZ, seed);
                double offsetX = ((hash & 0xFFF) / 4095.0);
                double offsetZ = (((hash >> 12) & 0xFFF) / 4095.0);

                // Organic path distortion
                double warpX = Math.sin(targetGridZ * 2.0 + seed) * 0.35;
                double warpZ = Math.cos(targetGridX * 2.0 - seed) * 0.35;

                double featurePointX = targetGridX + offsetX + warpX;
                double featurePointZ = targetGridZ + offsetZ + warpZ;

                double distance = Math.hypot(scaledX - featurePointX, scaledZ - featurePointZ);

                if (distance < minDistance1) {
                    minDistance2 = minDistance1;
                    minDistance1 = distance;
                } else if (distance < minDistance2) {
                    minDistance2 = distance;
                }
            }
        }

        // F2 - F1 boundary field values
        double boundaryField = minDistance2 - minDistance1;

        // Localized thickness variation that dynamically scales with size variables
        double localWobble = Math.sin(x * 0.1) * Math.cos(z * 0.1);
        double thicknessThreshold = thickness + (localWobble * (thickness * 0.3));

        return boundaryField < thicknessThreshold;
    }

    private long hashCoords(int x, int z, long seed) {
        long h = seed + x * 3129841L + z * 116129781L;
        h = (h ^ (h >>> 25)) * 268435459L;
        return h ^ (h >>> 27);
    } // 1.21.1 neoforge
}