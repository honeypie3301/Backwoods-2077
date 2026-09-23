package net.mcreator.thebackwoods.world.feature;

// 1.21.1 neo
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ScaffoldingTowerFeature extends Feature<NoneFeatureConfiguration> {

    // Pre-configured BlockStates to avoid evaluating state properties inside inner loops
    private static final BlockState STAIR_EAST = Blocks.OAK_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.EAST)
            .setValue(StairBlock.HALF, Half.BOTTOM);

    private static final BlockState STAIR_SOUTH = Blocks.OAK_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.SOUTH)
            .setValue(StairBlock.HALF, Half.BOTTOM);

    public ScaffoldingTowerFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level  = context.level();
        BlockPos      origin = context.origin();

        // --- CONFIGURABLE SETTINGS (14x14 SYMMETRICAL) ---
        int bottomY = 161;             
        int topY    = 319;             
        
        int gridSpacingXZ = 14;         
        int gridSpacingY  = 14;         
        // -------------------------------------------------

        BlockState plankState = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState airState   = Blocks.AIR.defaultBlockState();

        int minX = origin.getX();
        int maxX = origin.getX() + 15;
        int minZ = origin.getZ();
        int maxZ = origin.getZ() + 15;

        // Reusable position instance to prevent 40,000+ GC allocations per chunk
        BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();

        for (int blockX = minX; blockX <= maxX; blockX++) {
            int cellX = Math.floorMod(blockX, gridSpacingXZ);
            int sectorX = Math.floorDiv(blockX, gridSpacingXZ);
            long sectorXHash = (long) sectorX * 341873128712L;

            for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                int cellZ = Math.floorMod(blockZ, gridSpacingXZ);
                int sectorZ = Math.floorDiv(blockZ, gridSpacingXZ);
                
                // Pre-compute horizontal portion of room hash per column
                long columnHashBase = sectorXHash ^ ((long) sectorZ * 1328979813L);

                boolean isPillar = (cellX == 0) && (cellZ == 0);

                for (int y = bottomY; y <= topY; y++) {
                    currentPos.set(blockX, y, blockZ);

                    // 1. Pillars (Oak Planks Frame)
                    if (isPillar) {
                        level.setBlock(currentPos, plankState, 16);
                        continue;
                    }

                    int localY = y - bottomY;
                    int cellY  = Math.floorMod(localY, gridSpacingY);
                    int sectorY = Math.floorDiv(localY, gridSpacingY);

                    // 2. Horizontal Beams (Oak Planks Frame)
                    boolean isHorizontalBeam = (cellY == 0) && (cellX == 0 || cellZ == 0);
                    if (isHorizontalBeam) {
                        level.setBlock(currentPos, plankState, 16);
                        continue;
                    }

                    // 3. Structural Stair & Correct Under-Stair Block Check
                    long roomHash = columnHashBase ^ ((long) sectorY * 418731283L);
                    
                    BlockState placementState = airState; // Default to open air

                    if (Math.abs(roomHash % 100) < 65) { 
                        int stairDirection = Math.abs((int) (roomHash % 2));

                        if (stairDirection == 0) {
                            // X-Aligned Ramps (Climbing towards East)
                            if (cellZ > 0) { 
                                if (cellY == cellX && cellY > 0) {
                                    // The active step layer
                                    placementState = STAIR_EAST;
                                } else if (cellY == cellX - 1 && cellX > 0) {
                                    // FIXED: True mathematical block BELOW the step layer
                                    placementState = plankState;
                                }
                            }
                        } else {
                            // Z-Aligned Ramps (Climbing towards South)
                            if (cellX > 0) { 
                                if (cellY == cellZ && cellY > 0) {
                                    // The active step layer
                                    placementState = STAIR_SOUTH;
                                } else if (cellY == cellZ - 1 && cellZ > 0) {
                                    // FIXED: True mathematical block BELOW the step layer
                                    placementState = plankState;
                                }
                            }
                        }
                    }

                    // 4. Final Write Execution
                    level.setBlock(currentPos, placementState, 16);
                }
            }
        }

        return true;
    }
}