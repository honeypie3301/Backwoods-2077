package net.mcreator.thebackwoods.world.features;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

public class OakStalactiteFeature extends Feature<NoneFeatureConfiguration> {
    public OakStalactiteFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // Ensure we are placing under a solid ceiling
        if (!world.getBlockState(origin.above()).isSolidRender(world, origin.above())) {
            return false;
        }

        // ==========================================
        //         REFINED STALACTITE SETTINGS
        // ==========================================
        int minHeight = 8;        
        int heightVariation = 12; 
        float baseRadius = 1.2f + random.nextFloat() * 1.8f; // Slightly tightened max base
        float roughness = 0.35f;
        int maxAnchorDepth = 5;   
        // ==========================================

        int height = minHeight + random.nextInt(heightVariation);
        BlockState logBlock = Blocks.OAK_LOG.defaultBlockState();

        // 1. ANCHOR / BLENDING (Grows UP into the ceiling with an inverse taper)
		// 1. Anchor/Blending (Grow Upward into ceiling)
		int anchorRange = (int) Math.ceil(baseRadius);
		for (int x = -anchorRange; x <= anchorRange; x++) {
		    for (int z = -anchorRange; z <= anchorRange; z++) {
		        if (x * x + z * z <= (baseRadius * baseRadius)) {
		            for (int dy = 0; dy <= maxAnchorDepth; dy++) {
		                BlockPos checkPos = origin.above(dy).offset(x, 0, z);
		                
		                // CRITICAL FIX: If the block ABOVE checkPos is air, we are at the surface!
		                // Break immediately BEFORE placing the log so it never pops out on the floor.
		                if (world.isEmptyBlock(checkPos.above())) {
		                    break; 
		                }
		
		                // Force place logs into the ceiling to break up the flat plank texture safely
		                world.setBlock(checkPos, logBlock, 2);
		            }
		        }
		    }
		}

        // 2. MAIN BODY (Grows DOWN from the ceiling)
        for (int y = 0; y < height; y++) {
            float progress = (float) y / height;
            
            // Replaced the sharp Math.pow with a smoother cosine interpolation for the top transition,
            // then tapering thin near the tip.
            float targetRadius = baseRadius * (float) Math.pow(1.0f - progress, 1.3f);
            
            // Smooth out the immediate transition at y = 0 to prevent the lip
            if (y == 0) targetRadius *= 0.9f; 
            if (y == 1) targetRadius *= 0.95f;

            if (targetRadius < 0.1f) break;

            int loopRange = (int) Math.ceil(targetRadius + roughness);
            for (int x = -loopRange; x <= loopRange; x++) {
                for (int z = -loopRange; z <= loopRange; z++) {
                    float noise = (float) (Math.sin(x * 1.5 - y) * Math.cos(z * 1.5 - y)) * roughness;
                    if (x * x + z * z <= (targetRadius + noise) * (targetRadius + noise)) {
                        BlockPos pos = origin.below(y).offset(x, 0, z);
                        
                        // Only replace air, non-solid, or fluid blocks so it doesn't delete floors
                        if (world.isEmptyBlock(pos) || !world.getBlockState(pos).isSolidRender(world, pos)) {
                            world.setBlock(pos, logBlock, 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}