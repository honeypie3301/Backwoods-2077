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

public class FarLandsFeature extends Feature<NoneFeatureConfiguration> {

    // Improved 3D Perlin Noise permutation table
    private static final int[] P = new int[512];
    static {
        int[] permutation = {
            151,160,137,91,90,15,
            131,13,201,95,96,53,194,233, 7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
            190, 6,148,247,120,234,75,  0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
            88,237,149,56,87,174,20,125,136,171,206,197,  0,19,231,190,120,240,34,121,245,212,141,178,21,110,72,137, 10,
            171, 74, 90,211,247,228,  5,201,151,176,112, 17,210,120,175,133,186,134,196, 79,242,120, 64,121,223, 74, 1,186,
            110,134,242, 6,245,247,180,  4,136,120,229,197, 62,112,238,206,171,196,174,210,228,141,200, 79, 90,201, 30,121,
            247,120,212,197,252,219,190, 72,137,100,  8, 30,247,  4,120, 15,201, 95, 96, 53,194,233,  7,225,140, 36,103, 30,
            69,142,  8, 99, 37,240, 21, 10, 23,190,  6,148,247,120,234, 75,  0, 26,197, 62, 94,252,219,203,117, 35, 11, 32,
            57,177, 33, 88,237,149, 56, 87,174, 20,125,136,171,206,120,134,242,  6,245,247,180,  4,136,120,229,197, 62,112,
            238,206,171,196,174,210,228,141,200, 79, 90,201, 30,121,247,120,212,197,252,219,190, 72,137,100,  8, 30,247,  4,
            120, 15,201, 95, 96, 53,194,233,  7,225,140, 36,103, 30, 69,142,  8, 99, 37,240, 21, 10, 23,190,  6,148,247,120,
            234, 75,  0, 26,197, 62, 94,252,219,203,117, 35, 11, 32, 57,177, 33, 88,237,149, 56, 87,174, 20,125,136,171,206
        };
        for (int i = 0; i < 256 ; i++) {
            P[i] = permutation[i];
            P[256 + i] = permutation[i];
        }
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public static double noise(double x, double y, double z) {
        // OVERFLOW ERROR SIMULATION:
        // Emulating Beta 1.7 noise generation glitch which triggers the Far Lands
        // by casting bounds incorrectly to ints.
        int i = (int) x;
        int j = (int) y;
        int k = (int) z;
        if (x < (double) i) i--;
        if (y < (double) j) j--;
        if (z < (double) k) k--;

        int A_idx = P[i & 255] + j;
        int AA = P[A_idx & 255] + k;
        int AB = P[(A_idx + 1) & 255] + k;
        int B_idx = P[(i + 1) & 255] + j;
        int BA = P[B_idx & 255] + k;
        int BB = P[(B_idx + 1) & 255] + k;

        double dx = x - (double) i;
        double dy = y - (double) j;
        double dz = z - (double) k;

        // REMOVED CLAMPING: 
        // dx and dz are allowed to overflow into Infinity/NaN, 
        // reproducing the classic "hollow grid/sky grid" in the Corner Far Lands.

        double u = fade(dx);
        double v = fade(dy);
        double w = fade(dz);

        return lerp(w, lerp(v, lerp(u, grad(P[AA & 255], dx, dy, dz),
                                       grad(P[BA & 255], dx - 1, dy, dz)),
                               lerp(u, grad(P[AB & 255], dx, dy - 1, dz),
                                       grad(P[BB & 255], dx - 1, dy - 1, dz))),
                       lerp(v, lerp(u, grad(P[(AA + 1) & 255], dx, dy, dz - 1),
                                       grad(P[(BA + 1) & 255], dx - 1, dy, dz - 1)),
                               lerp(u, grad(P[(AB + 1) & 255], dx, dy - 1, dz - 1),
                                       grad(P[(BB + 1) & 255], dx - 1, dy - 1, dz - 1))));
    }

    public static double octaveNoise(double x, double y, double z, int octaves, double offsetX) {
        double out = 0;
        double amp = 1.0;
        double freq = 1.0;
        for (int i = 0; i < octaves; i++) {
            // A stable pseudo-random offset for each octave to prevent correlation between octaves
            double octaveOffset = (Math.abs(Math.sin(i * 12.9898 + offsetX)) * 43758.5453) % 10000.0;
            double ox = x * freq + octaveOffset;
            double oy = y * freq + octaveOffset;
            double oz = z * freq + octaveOffset;
            out += noise(ox, oy, oz) * amp;
            freq *= 2.0;
            amp *= 0.5;
        }
        return out;
    }

    public FarLandsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level  = context.level();
        BlockPos      origin = context.origin();

        int farLandsThreshold = 836721; //distance it generates
        int minX = origin.getX() & (~15);
        int minZ = origin.getZ() & (~15);
        
        boolean forceFarX = Math.abs(minX) >= farLandsThreshold;
        boolean forceFarZ = Math.abs(minZ) >= farLandsThreshold;

        if (!forceFarX && !forceFarZ) {
            return false;
        }

        double artificialOffsetX = 0;
        double artificialOffsetZ = 0;

        if (forceFarX) {
            artificialOffsetX = minX >= 0 ? 12550824 : -12550824;
        }
        if (forceFarZ) {
            artificialOffsetZ = minZ >= 0 ? 12550824 : -12550824;
        }

        ChunkAccess chunk = level.getChunk(origin);

        int bottomY = -64;
        int topY = 170;
        int maxGridY = 176; 
        int numSectionsY = (maxGridY - bottomY) / 8; // 30 sections
        int gridYSize = numSectionsY + 1; // 31 sampling points

        double[] densityGrid = new double[5 * 5 * gridYSize];

        for (int rx = 0; rx < 5; rx++) {
            for (int rz = 0; rz < 5; rz++) {
                for (int ry = 0; ry < gridYSize; ry++) {
                    double gx = minX + rx * 4 + artificialOffsetX;
                    double gy = bottomY + ry * 8;
                    double gz = minZ + rz * 4 + artificialOffsetZ;

                    // Beta 1.7 scale factors
                    double scaleXZ = 684.412;
                    double scaleY = 684.412;

                    // ACCURACY FIX: Scaled block coordinates horizontally by 4.0 and vertically by 8.0
                    double min = octaveNoise(gx * (scaleXZ / 4.0), gy * (scaleY / 8.0), gz * (scaleXZ / 4.0), 16, 123.456);
                    double max = octaveNoise(gx * (scaleXZ / 4.0), gy * (scaleY / 8.0), gz * (scaleXZ / 4.0), 16, 789.012);
                    double main = octaveNoise(gx * (scaleXZ / 16.0), gy * (scaleY / 32.0), gz * (scaleXZ / 16.0), 8, 345.678);

                    double val = (main / 10.0 + 1.0) / 2.0;
                    double density;
                    if (val < 0.0) density = min;
                    else if (val > 1.0) density = max;
                    else density = min + (max - min) * val;

                    if (gy > 64.0) {
                        density -= (gy - 64.0) * 16.0;
                    } else if (gy < 64.0) {
                        density += (64.0 - gy) * 16.0;
                    }

                    densityGrid[(rx * 5 + rz) * gridYSize + ry] = density;
                }
            }
        }

        // Tri-linear interpolation across the 4x4x8 blocks 
        for (int rx = 0; rx < 4; rx++) {
            for (int rz = 0; rz < 4; rz++) {
                for (int bx = 0; bx < 4; bx++) {
                    for (int bz = 0; bz < 4; bz++) {
                        int blockX = minX + rx * 4 + bx;
                        int blockZ = minZ + rz * 4 + bz;

                        for (int ry = numSectionsY - 1; ry >= 0; ry--) {
                            double d000 = densityGrid[(rx * 5 + rz) * gridYSize + ry];
                            double d001 = densityGrid[(rx * 5 + rz) * gridYSize + ry + 1];
                            double d010 = densityGrid[(rx * 5 + (rz + 1)) * gridYSize + ry];
                            double d011 = densityGrid[(rx * 5 + (rz + 1)) * gridYSize + ry + 1];
                            double d100 = densityGrid[((rx + 1) * 5 + rz) * gridYSize + ry];
                            double d101 = densityGrid[((rx + 1) * 5 + rz) * gridYSize + ry + 1];
                            double d110 = densityGrid[((rx + 1) * 5 + (rz + 1)) * gridYSize + ry];
                            double d111 = densityGrid[((rx + 1) * 5 + (rz + 1)) * gridYSize + ry + 1];

                            double mx = bx / 4.0;
                            double mz = bz / 4.0;

                            double d00 = lerp(mx, d000, d100);
                            double d01 = lerp(mx, d001, d101);
                            double d10 = lerp(mx, d010, d110);
                            double d11 = lerp(mx, d011, d111);

                            double d0 = lerp(mz, d00, d10);
                            double d1 = lerp(mz, d01, d11);

                            for (int by = 7; by >= 0; by--) {
                                double my = by / 8.0;
                                double density = lerp(my, d0, d1);

                                int currentY = bottomY + ry * 8 + by;
                                if (currentY > topY) continue;

                                BlockPos targetPos = new BlockPos(blockX, currentY, blockZ);

                                if (density > 0.0) {
                                    level.setBlock(targetPos, Blocks.OAK_PLANKS.defaultBlockState(), 16);
                                } else {
                                    level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 16);
                                }
                            } // 1.21.1
                        }
                    }
                }
            }
        }
        return true;
    }
}