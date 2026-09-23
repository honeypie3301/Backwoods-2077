package net.mcreator.thebackwoods;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class CalcifiedSpikeFeature extends Feature<NoneFeatureConfiguration> {
    public CalcifiedSpikeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        
        // ==========================================
        //         EASY EDIT SETTINGS HERE
        // ==========================================
        int minHeight = 60;
        int heightVariation = 50;
        float minRadius = 4.0f;
        float radiusVariation = 3.0f;
        float minLean = 4.0f;
        float leanVariation = 6.0f;
        int seaLevelAnchor = 63;

        float roughnessIntensity = 0.8f; 
        int maxPillarDepth = 20; 
        // ==========================================

        int startY = Math.max(origin.getY(), seaLevelAnchor);
        BlockPos basePos = new BlockPos(origin.getX(), startY, origin.getZ());

        int height = minHeight + random.nextInt(heightVariation); 
        float baseRadius = minRadius + random.nextFloat() * radiusVariation; 
        float leanIntensity = minLean + random.nextFloat() * leanVariation; 
        
        BlockState spikeBlock = TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_WOOD.get().defaultBlockState();

        // 1. Generate the Spike Body
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
                        
                        // FIXED FOR 1.21.1: Added (world, pos)
                        if (world.isEmptyBlock(pos) || !world.getBlockState(pos).isSolidRender(world, pos)) {
                            world.setBlock(pos, spikeBlock, 2);
                        }
                    }
                }
            }
        }

        // 2. Foundation/Root
        int rootRange = (int) Math.ceil(baseRadius + roughnessIntensity);
        for (int x = -rootRange; x <= rootRange; x++) {
            for (int z = -rootRange; z <= rootRange; z++) {
                if (x * x + z * z <= (baseRadius * baseRadius)) {
                    for (int dy = -1; dy >= -maxPillarDepth; dy--) {
                        BlockPos checkPos = basePos.offset(x, dy, z);
                        BlockState currentState = world.getBlockState(checkPos);
                        
                        // FIXED FOR 1.21.1: Added (world, checkPos)
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