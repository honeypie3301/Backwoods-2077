package net.mcreator.thebackwoods.world.feature;

// 1.21.1 neo
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class TheUndersideFeature extends Feature<NoneFeatureConfiguration> {

    public TheUndersideFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level  = context.level();
        BlockPos      origin = context.origin();

        // --- CONFIGURABLE BOUNDS ---
        int bottomY = 20;
        int topY    = 44;
        int gridSpacing = 4; 
        // ----------------------------

        BlockState plankState = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState fenceState = Blocks.OAK_FENCE.defaultBlockState();
        BlockState airState   = Blocks.AIR.defaultBlockState();

        // Grab raw chunk access to overwrite data sections directly
        ChunkAccess chunk = level.getChunk(origin);
        int minX = origin.getX() & (~15);
        int minZ = origin.getZ() & (~15);

        // Loop across the strict 16x16 chunk grid columns
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int absX = minX + x;
                int absZ = minZ + z;

                // Determine if this absolute coordinate hits our fence grid lines
                boolean isFenceColumn = (Math.abs(absX) % gridSpacing == 0) && (Math.abs(absZ) % gridSpacing == 0);

                for (int y = bottomY; y <= topY; y++) {
                    BlockPos targetPos = new BlockPos(absX, y, absZ);

                    if (y == topY) {
                        // Strict Ceiling
                        chunk.setBlockState(targetPos, plankState, false);
                    } else if (y == bottomY) {
                        // Solid Floor Base
                        chunk.setBlockState(targetPos, plankState, false);
                    } else {
                        if (isFenceColumn) {
                            chunk.setBlockState(targetPos, fenceState, false);
                        } else {
                            // DEEP FORCE-OVERWRITE: Overwrites existing chunk generation states directly
                            // This replaces terrain rule blocks/mazes instantly as they process
                            chunk.setBlockState(targetPos, airState, false);
                        }
                    }
                }
            }
        }

        return true;
    }
}