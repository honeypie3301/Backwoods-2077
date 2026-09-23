package net.mcreator.thebackwoods.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

public class PlaqueTreeFeature extends Feature<NoneFeatureConfiguration> {

    // ==========================================
    // TREE CONFIGURATION
    // Adjust this multiplier to change the overall size of the tree.
    // 1.0f = Standard Bonsai Size (Previous version)
    // 1.5f = 50% Larger (Current setting)
    // 2.0f = Double Size (Massive)
    private static final float SIZE_SCALE = 1.5f; 
    // ==========================================

    public PlaqueTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockState plaqueState = BuiltInRegistries.BLOCK
            .getOptional(ResourceLocation.fromNamespaceAndPath("the_backwoods", "plaque"))
            .orElse(Blocks.OAK_LOG)
            .defaultBlockState();

        BlockPos startPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, origin);
        if (startPos.getY() <= level.getMinBuildHeight() || startPos.getY() >= level.getMaxBuildHeight() - (50 * SIZE_SCALE)) {
            return false;
        }

        // 1. Root Anchors
        for (int i = 0; i < 4; i++) {
            double rootAngle = (Math.PI / 2.0) * i + (random.nextDouble() * 0.5);
            int rootLen = (int) ((random.nextInt(3) + 3) * SIZE_SCALE);
            BlockPos rootEnd = startPos.offset(
                (int) (Math.cos(rootAngle) * rootLen),
                (int) (-2 * SIZE_SCALE),
                (int) (Math.sin(rootAngle) * rootLen)
            );
            List<BlockPos> rootPath = interpolateBezier(startPos, startPos.above(1), rootEnd, (int) (8 * SIZE_SCALE));
            for (int r = 0; r < rootPath.size(); r++) {
                float rad = (1.5f * SIZE_SCALE) * (1.0f - ((float) r / rootPath.size()));
                placeSphere(level, rootPath.get(r), plaqueState, rad, 1.0);
            }
        }

        // 2. Elegant S-Curve Trunk
        int trunkHeight = (int) ((random.nextInt(6) + 22) * SIZE_SCALE); 
        double leanAngle = random.nextDouble() * 2 * Math.PI;
        int maxLean = (int) ((random.nextInt(4) + 10) * SIZE_SCALE); 

        // Control point pushes out, end point curves heavily back in
        BlockPos p0 = startPos;
        BlockPos p1 = startPos.above((int) (trunkHeight * 0.45)).offset(
            (int) (Math.cos(leanAngle) * maxLean),
            0,
            (int) (Math.sin(leanAngle) * maxLean)
        );
        BlockPos p2 = startPos.above(trunkHeight).offset(
            (int) (Math.cos(leanAngle) * (maxLean * 0.2)), 
            0,
            (int) (Math.sin(leanAngle) * (maxLean * 0.2))
        );

        // Increase interpolation steps based on scale to avoid block gaps on huge trees
        List<BlockPos> trunkPath = interpolateBezier(p0, p1, p2, (int) (40 * Math.max(1.0f, SIZE_SCALE)));

        for (int i = 0; i < trunkPath.size(); i++) {
            float progress = (float) i / trunkPath.size();
            float currentRadius = (2.0f * SIZE_SCALE) * (1.0f - progress) + (0.8f * SIZE_SCALE) * progress;
            placeSphere(level, trunkPath.get(i), plaqueState, currentRadius, 1.0);
        }

        // 3. Upward-Arching Thin Boughs
        int numBranches = random.nextInt(2) + 4; // 4 to 5 main branches
        double currentAngle = leanAngle + Math.PI; // Start opposite to trunk lean

        for (int b = 0; b < numBranches; b++) {
            float progress = 0.35f + (0.55f * ((float) b / Math.max(1, numBranches - 1)));
            int trunkIdx = (int) (trunkPath.size() * progress);
            if (trunkIdx >= trunkPath.size()) continue;

            BlockPos bStart = trunkPath.get(trunkIdx);
            
            // Spiral around the tree
            currentAngle += (Math.PI / 1.8) + (random.nextDouble() * 0.5);
            
            int bLength = (int) ((random.nextInt(5) + 9) * SIZE_SCALE); 
            int bHeight = (int) ((random.nextInt(4) + 5) * SIZE_SCALE); 

            BlockPos bEnd = bStart.offset(
                (int) (Math.cos(currentAngle) * bLength),
                bHeight,
                (int) (Math.sin(currentAngle) * bLength)
            );
            
            BlockPos bMid = bStart.above(bHeight / 2).offset(
                (int) (Math.cos(currentAngle) * (bLength * 0.5)),
                0,
                (int) (Math.sin(currentAngle) * (bLength * 0.5))
            );

            List<BlockPos> branchPath = interpolateBezier(bStart, bMid, bEnd, (int) (20 * Math.max(1.0f, SIZE_SCALE)));
            
            float branchBaseRadius = (1.2f * SIZE_SCALE) * (1.0f - progress) + (0.5f * SIZE_SCALE);

            for (int i = 0; i < branchPath.size(); i++) {
                float bProgress = (float) i / branchPath.size();
                float bRad = branchBaseRadius * (1.0f - bProgress) + (0.4f * SIZE_SCALE) * bProgress;
                placeSphere(level, branchPath.get(i), plaqueState, bRad, 1.0);
            }

            // Distribute wide, flat canopy pads at the ends
            buildFluffyCanopy(level, bEnd, plaqueState, random);
        }

        // Hanging twisted detail branch
        int hangIndex = (int) (trunkPath.size() * 0.45f);
        if (hangIndex < trunkPath.size()) {
            BlockPos hangStart = trunkPath.get(hangIndex);
            BlockPos hangEnd = hangStart.offset(0, (int) (-5 * SIZE_SCALE), 0).offset(
                (int) (Math.cos(leanAngle - Math.PI/2) * (3 * SIZE_SCALE)), 
                0, 
                (int) (Math.sin(leanAngle - Math.PI/2) * (3 * SIZE_SCALE))
            );
            List<BlockPos> hangPath = interpolateBezier(hangStart, hangStart.offset(1, (int) (-2 * SIZE_SCALE), 1), hangEnd, (int) (12 * SIZE_SCALE));
            for (BlockPos hp : hangPath) {
                placeSphere(level, hp, plaqueState, 0.6f * SIZE_SCALE, 1.0);
            }
        }

        // Crown Main Canopy
        buildFluffyCanopy(level, trunkPath.get(trunkPath.size() - 1), plaqueState, random);

        return true;
    }

    private void buildFluffyCanopy(WorldGenLevel level, BlockPos center, BlockState state, RandomSource random) {
        int numBlobs = random.nextInt(3) + 4; // 4 to 6 clusters per pad
        for (int i = 0; i < numBlobs; i++) {
            int offsetX = (int) ((random.nextInt(9) - 4) * SIZE_SCALE); 
            int offsetY = (int) ((random.nextInt(3) - 1) * SIZE_SCALE); 
            int offsetZ = (int) ((random.nextInt(9) - 4) * SIZE_SCALE);
            float blobRadius = (random.nextFloat() * 1.5f + 3.0f) * SIZE_SCALE; 
            
            placeCanopyBlob(level, center.offset(offsetX, offsetY, offsetZ), state, blobRadius, random);
        }
    }

    private void placeCanopyBlob(WorldGenLevel level, BlockPos center, BlockState state, float radius, RandomSource random) {
        int r = (int) Math.ceil(radius);
        
        // Scale the flat-bottom boundary limits based on our size config
        int yMin = (int) Math.floor(-1.0 * SIZE_SCALE);
        int yMax = (int) Math.ceil(2.5 * SIZE_SCALE);

        for (int x = -r; x <= r; x++) {
            for (int y = yMin; y <= yMax; y++) { 
                for (int z = -r; z <= r; z++) {
                    // Squashes the Y axis. Bottom is flat (weight 5.0), top is slightly domed (weight 2.5)
                    double yWeight = (y > 0) ? (y * y * 2.5) : (y * y * 5.0);
                    double distSq = x * x + yWeight + z * z; 
                    
                    if (distSq <= radius * radius) {
                        placeIfReplaceable(level, center.offset(x, y, z), state);
                    }
                    
                    // Sparse hanging bits for texture
                    if (y == yMin && distSq > radius * radius * 0.4 && distSq <= radius * radius) {
                        if (random.nextFloat() < 0.15f) {
                            placeIfReplaceable(level, center.offset(x, yMin - 1, z), state);
                            if (random.nextBoolean()) {
                                placeIfReplaceable(level, center.offset(x, yMin - 2, z), state);
                            }
                        }
                    }
                }
            }
        }
    }

    private void placeSphere(WorldGenLevel level, BlockPos center, BlockState state, float radius, double yScale) {
        int r = (int) Math.ceil(radius);
        if (r == 0) r = 1; 
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + (y * y / yScale) + z * z <= radius * radius) {
                        placeIfReplaceable(level, center.offset(x, y, z), state);
                    }
                }
            }
        }
    }

    private void placeIfReplaceable(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, state, 2);
        }
    }

    private List<BlockPos> interpolateBezier(BlockPos p0, BlockPos p1, BlockPos p2, int steps) {
        List<BlockPos> points = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double u = 1.0 - t;

            double x = u * u * p0.getX() + 2 * u * t * p1.getX() + t * t * p2.getX();
            double y = u * u * p0.getY() + 2 * u * t * p1.getY() + t * t * p2.getY();
            double z = u * u * p0.getZ() + 2 * u * t * p1.getZ() + t * t * p2.getZ();

            BlockPos pos = new BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
            if (points.isEmpty() || !points.get(points.size() - 1).equals(pos)) {
                points.add(pos);
            }
        }
        return points;
    }
}