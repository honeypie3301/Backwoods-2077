package net.mcreator.thebackwoods.world.feature;

// 1.21.1 neoforge compatible
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class MengerSpongeFeature extends Feature<NoneFeatureConfiguration> {

    // --- CONFIGURABLE BOUNDS (81x81x81) ---
    private static final int BOTTOM_Y = -62; 
    private static final int TOP_Y = 18;        
    private static final int SPONGE_SIZE = 81;     
    // --------------------------------------

    private static final BlockState PLANK_STATE = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState AIR_STATE = Blocks.AIR.defaultBlockState();

    public MengerSpongeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        int minX = origin.getX() & (~15);
        int minZ = origin.getZ() & (~15);

        // Single mutable position instance reused across the entire chunk generation
        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();

        for (int rx = 0; rx < 16; rx++) {
            int blockX = minX + rx;
            int fx = Math.floorMod(blockX, SPONGE_SIZE);

            for (int rz = 0; rz < 16; rz++) {
                int blockZ = minZ + rz;
                int fz = Math.floorMod(blockZ, SPONGE_SIZE);

                for (int currentY = BOTTOM_Y; currentY <= TOP_Y; currentY++) {
                    targetPos.set(blockX, currentY, blockZ);

                    // Protection layer check: Don't slice into existing floor structures
                    if (currentY <= -59) {
                        BlockState existingBlock = level.getBlockState(targetPos);
                        if (existingBlock.is(Blocks.BEDROCK) || existingBlock.is(Blocks.OAK_PLANKS)) {
                            continue;
                        }
                    }

                    int fy = currentY - BOTTOM_Y;

                    // Evaluate if this specific 3D point belongs inside the fractal matrix
                    if (isMengerSpongeSolid(fx, fy, fz)) {
                        // Flag 2 = Update client side, bypass unnecessary block updates during generation
                        level.setBlock(targetPos, PLANK_STATE, 2);
                    } else {
                        level.setBlock(targetPos, AIR_STATE, 2);
                    }
                }
            }
        }

        return true;
    }

    /**
     * Fast iterative Menger Sponge checker.
     * Evaluates up to level 4 (3^4 = 81 size limit).
     */
    private static boolean isMengerSpongeSolid(int x, int y, int z) {
        if (x < 0 || x >= SPONGE_SIZE || y < 0 || y >= SPONGE_SIZE || z < 0 || z >= SPONGE_SIZE) {
            return false;
        }

        while (x > 0 || y > 0 || z > 0) {
            int checkX = x % 3;
            int checkY = y % 3;
            int checkZ = z % 3;

            // Branch early: if 2 or more coordinates equal 1, it's a hole.
            if ((checkX == 1 ? 1 : 0) + (checkY == 1 ? 1 : 0) + (checkZ == 1 ? 1 : 0) >= 2) {
                return false;
            }

            x /= 3;
            y /= 3;
            z /= 3;
        }
        return true;
    }
}