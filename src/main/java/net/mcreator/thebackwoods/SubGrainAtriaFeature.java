package net.mcreator.thebackwoods;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class SubGrainAtriaFeature extends Feature<NoneFeatureConfiguration> {
    public SubGrainAtriaFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        
        BlockState plank = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        
        int startY = 8;        // Starting Y level
        int floors = 3;        // Number of stacked floors
        int floorHeight = 6;   // Distance from one floor to the next (5 blocks structure + 1 block gap)
        
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int f = 0; f < floors; f++) {
            int currentFloorY = startY + (f * floorHeight);

            // 1. Generate the solid structural volume for this floor
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 5; y++) {
                        mpos.set(origin.getX() + x, currentFloorY + y, origin.getZ() + z);
                        world.setBlock(mpos, plank, 2);
                    }
                }
            }

            // 2. Global Coordinate Carving
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = origin.getX() + x;
                    int worldZ = origin.getZ() + z;

                    boolean isHallX = Math.abs(worldX % 10) < 3;
                    boolean isHallZ = Math.abs(worldZ % 10) < 3;

                    if (isHallX || isHallZ) {
                        for (int y = 1; y <= 3; y++) {
                            mpos.set(worldX, currentFloorY + y, worldZ);
                            world.setBlock(mpos, air, 2);
                        }
                        
                        // OPTIONAL: Vertical Connection (Shafts)
                        // This carves a hole in the ceiling/floor to connect levels
                        // Only happens at certain intersections (e.g., every 20 blocks)
                        if (worldX % 20 == 0 && worldZ % 20 == 0 && f < floors - 1) {
                            // Carve a 2x2 hole through the ceiling of this floor and floor of the next
                            mpos.set(worldX, currentFloorY + 4, worldZ); world.setBlock(mpos, air, 2);
                            mpos.set(worldX + 1, currentFloorY + 4, worldZ); world.setBlock(mpos, air, 2);
                            mpos.set(worldX, currentFloorY + 5, worldZ); world.setBlock(mpos, air, 2);
                            mpos.set(worldX + 1, currentFloorY + 5, worldZ); world.setBlock(mpos, air, 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}