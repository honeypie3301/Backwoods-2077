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

public class RightSideUpCityFeature extends Feature<NoneFeatureConfiguration> {

    public RightSideUpCityFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level  = context.level();
        BlockPos      origin = context.origin();

        // --- CONFIGURABLE CITY SETTINGS ---
        int floorY = 63;               
        int gridSize = 10;             // Building grid spacing (blocks)

        int minBuildingWidthX  = 5;    
        int maxBuildingWidthX  = 8;   
        int minBuildingWidthZ  = 5;    
        int maxBuildingWidthZ  = 8;   
        int minBuildingLengthY = 20;   
        int maxBuildingLengthY = 50;   

        // Configurable chance out of 100 for a building cell to generate as a hollow shell
        int hollowBuildingChance = 40;  
        // ----------------------------------

        // --- ROUNDABOUT SETTINGS ---
        int macroChunkGrid = 10;       // Spawn a roundabout every 10x10 chunks
        double parkRadius  = 24.0;     // Radius of the roundabout circle in blocks
        // ----------------------------------

        BlockState plankState = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState airState   = Blocks.AIR.defaultBlockState();

        // Convert the origin block coordinates to raw chunk grids
        ChunkAccess chunk = level.getChunk(origin);
        int minX = origin.getX() & (~15);
        int minZ = origin.getZ() & (~15);
        int chunkX = minX >> 4;
        int chunkZ = minZ >> 4;

        // Find the center chunk coordinates for the current 10x10 macro chunk sector
        int macroSectorX = Math.floorDiv(chunkX, macroChunkGrid);
        int macroSectorZ = Math.floorDiv(chunkZ, macroChunkGrid);
        
        // Find the absolute center block point of the roundabout for this 10x10 chunk macro sector
        int centerRoundaboutX = (macroSectorX * macroChunkGrid + (macroChunkGrid / 2)) * 16 + 8;
        int centerRoundaboutZ = (macroSectorZ * macroChunkGrid + (macroChunkGrid / 2)) * 16 + 8;

        // Single-pass direct buffer generation loop
        for (int rx = 0; rx < 16; rx++) {
            for (int rz = 0; rz < 16; rz++) {
                int blockX = minX + rx;
                int blockZ = minZ + rz;

                // Calculate the exact distance from this block to the roundabout center point
                double dx = blockX - centerRoundaboutX;
                double dz = blockZ - centerRoundaboutZ;
                double distanceToCenter = Math.sqrt(dx * dx + dz * dz);

                boolean isInRoundabout = distanceToCenter <= parkRadius;

                // Max clear height for air buffer clearance
                int maxClearY = floorY + maxBuildingLengthY + 5;

                for (int currentY = floorY; currentY <= maxClearY; currentY++) {
                    BlockPos targetPos = new BlockPos(blockX, currentY, blockZ);

                    if (currentY == floorY) {
                        // 1. Foundation Layer Pass - Reverted parameter to false for 1.21.1
                        chunk.setBlockState(targetPos, plankState, false); 
                    } else {
                        // 2. Above-ground Structuring Pass
                        if (isInRoundabout) {
                            // Carve everything within the radius into clear open air space
                            chunk.setBlockState(targetPos, airState, false);
                        } else {
                            // Standard building placement logic
                            int cellX = Math.floorMod(blockX, gridSize);
                            int cellZ = Math.floorMod(blockZ, gridSize);

                            int sectorX = Math.floorDiv(blockX, gridSize);
                            int sectorZ = Math.floorDiv(blockZ, gridSize);
                            
                            long sectorHash = ((long) sectorX * 418731283L) ^ ((long) sectorZ * 13289791L) ^ 84729184L;
                            
                            int bWidthX = minBuildingWidthX + (int) (Math.abs(sectorHash ^ 4567L) % (maxBuildingWidthX - minBuildingWidthX + 1));
                            int bWidthZ = minBuildingWidthZ + (int) (Math.abs(sectorHash ^ 9876L) % (maxBuildingWidthZ - minBuildingWidthZ + 1));
                            int bHeight = minBuildingLengthY + (int) (Math.abs(sectorHash) % (maxBuildingLengthY - minBuildingLengthY + 1));

                            int dy = currentY - floorY;
                            if (cellX < bWidthX && cellZ < bWidthZ && dy <= bHeight && Math.abs(sectorHash % 100) < 85) {
                                // Determine deterministically if this specific building should be hollow
                                boolean isHollowBuilding = (Math.abs(sectorHash ^ 12345L) % 100) < hollowBuildingChance;

                                if (isHollowBuilding) {
                                    // Bounding shell detection mechanics
                                    boolean isOuterWall = (cellX == 0 || cellX == bWidthX - 1 || cellZ == 0 || cellZ == bWidthZ - 1);
                                    boolean isRoofOrFloor = (dy == 1 || dy == bHeight);

                                    if (isOuterWall || isRoofOrFloor) {
                                        chunk.setBlockState(targetPos, plankState, false);
                                    } else {
                                        chunk.setBlockState(targetPos, airState, false); // Inner structural air cavity carve
                                    }
                                } else {
                                    chunk.setBlockState(targetPos, plankState, false);
                                }
                            } else {
                                chunk.setBlockState(targetPos, airState, false); // Clears background noise walls
                            }
                        }
                    }
                }
            }
        }

        return true;
    }
}