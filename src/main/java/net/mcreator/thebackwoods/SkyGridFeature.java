package net.mcreator.thebackwoods.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SkyGridFeature extends Feature<NoneFeatureConfiguration> {

    public SkyGridFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {

        // --- EQUALIZED CONSTANTS FOR PERFECT CUBE CAVITIES ---
        int gridBaseHeight   = 190;  // Lowest Y layer of the sky grid
        int maxHeightLimit   = 319;  // Hard ceiling (overworld build limit)
        int gridSpacing      = 12;   // Horizontal distance between beam centers (11 blocks air + 1 block beam)
        int layerIntervalY   = 12;   // MATCHED: Vertical distance between floors (11 blocks air + 1 block beam)
        // -----------------------------------------------------

        WorldGenLevel level  = context.level();
        BlockPos      origin = context.origin();

        // Iterate strictly within this chunk's local offsets (0-15)
        for (int rx = 0; rx < 16; rx++) {
            for (int rz = 0; rz < 16; rz++) {

                // Absolute world-space coordinates for this column
                int blockX = origin.getX() + rx;
                int blockZ = origin.getZ() + rz;

                // Mathematically sound modulo that works in negative coordinate space identically
                int modX = ((blockX % gridSpacing) + gridSpacing) % gridSpacing;
                int modZ = ((blockZ % gridSpacing) + gridSpacing) % gridSpacing;

                boolean onXLine = (modX == 0); // Column lies on an X-aligned beam
                boolean onZLine = (modZ == 0); // Column lies on a Z-aligned beam
                boolean onPillar = onXLine && onZLine; // Exact grid intersection

                // Skip columns that are on neither beam axis
                if (!onXLine && !onZLine) continue;

                for (int currentY = gridBaseHeight; currentY <= maxHeightLimit; currentY++) {

                    boolean isLayerY = ((currentY - gridBaseHeight) % layerIntervalY == 0);
                    boolean place = false;

                    if (onPillar) {
                        // Vertical pillars run the full height at every intersection point
                        place = true;
                    } else if (isLayerY) {
                        // Horizontal beams only placed at matching layer intervals
                        if (onXLine || onZLine) {
                            place = true;
                        }
                    }

                    if (place) {
                        BlockPos pos = new BlockPos(blockX, currentY, blockZ);
                        // Only replace air to avoid overwriting terrain plates
                        if (level.isEmptyBlock(pos)) {
                            level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}