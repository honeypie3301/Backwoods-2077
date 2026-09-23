package net.mcreator.thebackwoods.world.feature;
// 1.21.1 neo
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class UpsideDownCityFeature extends Feature<NoneFeatureConfiguration> {

    public UpsideDownCityFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level  = context.level();
        BlockPos      origin = context.origin();

        // --- CONFIGURABLE CITY SETTINGS ---
        int ceilingY = 160;            // Fixed height altitude
        int gridSize = 10;             // The total size of each global sector tile

        // Configurable Building Sizes
        int minBuildingWidthX  = 5;    
        int maxBuildingWidthX  = 8;   
        int minBuildingWidthZ  = 5;    
        int maxBuildingWidthZ  = 8;   
        int minBuildingLengthY = 20;   
        int maxBuildingLengthY = 50;   
        // ----------------------------------

        BlockState plankState = Blocks.OAK_PLANKS.defaultBlockState();

        // Run through every block in the 16x16 chunk space sequentially
        for (int rx = 0; rx < 16; rx++) {
            for (int rz = 0; rz < 16; rz++) {
                int blockX = origin.getX() + rx;
                int blockZ = origin.getZ() + rz;

                BlockPos ceilingPos = new BlockPos(blockX, ceilingY, blockZ);
                
                // 1. Force the solid ceiling plate using Flag 16 (Bypasses carvers and boundary clipping)
                level.setBlock(ceilingPos, plankState, 16);

                // 2. Global Coordinate Grid Math
                int cellX = Math.floorMod(blockX, gridSize);
                int cellZ = Math.floorMod(blockZ, gridSize);

                int sectorX = Math.floorDiv(blockX, gridSize);
                int sectorZ = Math.floorDiv(blockZ, gridSize);
                
                long sectorHash = ((long) sectorX * 418731283L) ^ ((long) sectorZ * 13289791L);
                
                int bWidthX = minBuildingWidthX + (int) (Math.abs(sectorHash ^ 4567L) % (maxBuildingWidthX - minBuildingWidthX + 1));
                int bWidthZ = minBuildingWidthZ + (int) (Math.abs(sectorHash ^ 9876L) % (maxBuildingWidthZ - minBuildingWidthZ + 1));
                int bHeight = minBuildingLengthY + (int) (Math.abs(sectorHash) % (maxBuildingLengthY - minBuildingLengthY + 1));

                // If the block falls inside the calculated global box zone, stamp it down directly
                if (cellX < bWidthX && cellZ < bWidthZ) {
                    if (Math.abs(sectorHash % 100) < 85) { 
                        for (int dy = 1; dy <= bHeight; dy++) {
                            int currentY = ceilingY - dy;
                            BlockPos targetPos = new BlockPos(blockX, currentY, blockZ);
                            
                            // Inject building block straight into world data via Flag 16
                            level.setBlock(targetPos, plankState, 16);
                        }
                    }
                }
            }
        }

        return true;
    }
}