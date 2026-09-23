package net.mcreator.thebackwoods;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class LargeCalcifiedSpike extends Feature<NoneFeatureConfiguration> {
    public LargeCalcifiedSpike() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        
        // ==========================================
        //       LARGE SPIKE CONFIGURATION (1.21.1)
        // ==========================================
        int minHeight = 100;
        int heightVariation = 85;
        float minRadius = 10.0f;
        float radiusVariation = 4.0f;
        float minLean = 2.0f;
        float leanVariation = 5.0f;

        float roughnessIntensity = 1.1f; 
        int maxPillarDepth = 25; 
        // ==========================================

        // Dynamic Water Check: Uses OCEAN_FLOOR_WG to find the bedrock/soil under bodies of water
        int groundY = world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        BlockPos basePos = new BlockPos(origin.getX(), groundY, origin.getZ());

        int height = minHeight + random.nextInt(heightVariation); 
        float baseRadius = minRadius + random.nextFloat() * radiusVariation; 
        float leanIntensity = minLean + random.nextFloat() * leanVariation; 
        
        BlockState spikeBlock = TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_WOOD.get().defaultBlockState();

        // 1. Generate the Spike Body (Water-Logging Safe)
        for (int y = 0; y < height; y++) {
            float progress = (float) y / height;
            float targetRadius = baseRadius * (1.0f - progress);
            int leanX = (int) (progress * -leanIntensity);

            if (targetRadius < 0.1f) break;

            int loopRange = (int) Math.ceil(targetRadius + roughnessIntensity);
            
            for (int x = -loopRange; x <= loopRange; x++) {
                for (int z = -loopRange; z <= loopRange; z++) {
                    
                    float jitter = (float) (Math.sin(x * 0.8 + y * 0.5) * Math.cos(z * 0.8 + y * 0.5)) * roughnessIntensity;
                    float finalRadius = targetRadius + jitter;

                    if (x * x + z * z <= finalRadius * finalRadius) {
                        BlockPos pos = basePos.offset(x + leanX, y, z);
                        
                        // Replaces water, air, or soft non-solid blocks safely
                        BlockState currentState = world.getBlockState(pos);
                        if (world.isEmptyBlock(pos) || !currentState.isSolidRender(world, pos) || currentState.getFluidState().isSource()) {
                            world.setBlock(pos, spikeBlock, 2);
                        }
                    }
                }
            }
        }

        // 2. Deep Root Foundation
        int rootRange = (int) Math.ceil(baseRadius + roughnessIntensity);
        for (int x = -rootRange; x <= rootRange; x++) {
            for (int z = -rootRange; z <= rootRange; z++) {
                if (x * x + z * z <= (baseRadius * baseRadius)) {
                    for (int dy = -1; dy >= -maxPillarDepth; dy--) {
                        BlockPos checkPos = basePos.offset(x, dy, z);
                        BlockState currentState = world.getBlockState(checkPos);
                        
                        if (currentState.isSolidRender(world, checkPos) && currentState.getBlock() != spikeBlock.getBlock()) {
                            break;
                        }
                        
                        world.setBlock(checkPos, spikeBlock, 2);
                    }
                }
            }
        }

        return true;
    }
}