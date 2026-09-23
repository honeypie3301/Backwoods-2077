package net.mcreator.thebackwoods.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

public class MengerSpongeSmallFeature extends Feature<NoneFeatureConfiguration> {

    private static final int SPONGE_SIZE = 27;    
    private static final int BOTTOM_Y = -50;       
    private static final int TOP_Y = -24;          

    // Lazy-loaded BlockState Cache
    private static BlockState LIGNUM_CARO;
    private static BlockState SPLINTERED_OAK_PLANKS;
    private static BlockState OAK_PLANKS;
    private static final BlockState AIR_STATE = Blocks.AIR.defaultBlockState();
    private static final BlockState FURNACE_STATE = Blocks.FURNACE.defaultBlockState();
    
    private static final ResourceKey<LootTable> SPONGE_LOOT_TABLE = ResourceKey.create(
        Registries.LOOT_TABLE,
        ResourceLocation.fromNamespaceAndPath("the_backwoods", "chests/small_menger_sponge_loot")
    );

    public MengerSpongeSmallFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static void initBlockStates() {
        if (LIGNUM_CARO == null) {
            LIGNUM_CARO = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("the_backwoods", "lignum_caro")).defaultBlockState();
            SPLINTERED_OAK_PLANKS = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("the_backwoods", "splintered_oak_planks")).defaultBlockState();
            OAK_PLANKS = Blocks.OAK_PLANKS.defaultBlockState();
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        initBlockStates();

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        int minX = origin.getX() & (~15);
        int minZ = origin.getZ() & (~15);

        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();

        for (int rx = 0; rx < 16; rx++) {
            int blockX = minX + rx;
            int fx = Math.floorMod(blockX, SPONGE_SIZE);

            for (int rz = 0; rz < 16; rz++) {
                int blockZ = minZ + rz;
                int fz = Math.floorMod(blockZ, SPONGE_SIZE);

                int spongeGridX = Math.floorDiv(blockX, SPONGE_SIZE);
                int spongeGridZ = Math.floorDiv(blockZ, SPONGE_SIZE);

                // Deterministic seed for structure loot determination
                long spongeSeed = (long) spongeGridX * 341873128712L + (long) spongeGridZ * 132897987541L;
                RandomSource spongeRandom = RandomSource.create(spongeSeed);

                // Loot Structure Chances
                boolean allowChest = spongeRandom.nextFloat() < 0.15f;
                boolean allowFurnace = spongeRandom.nextFloat() < 0.02f; // 2% chance per sponge structure
                boolean allowBed = spongeRandom.nextFloat() < 0.08f;

                for (int currentY = BOTTOM_Y; currentY <= TOP_Y; currentY++) {
                    targetPos.set(blockX, currentY, blockZ);

                    if (currentY <= -59 && level.getBlockState(targetPos).is(Blocks.BEDROCK)) {
                        continue;
                    }

                    int fy = currentY - BOTTOM_Y;

                    if (isMengerSpongeSolid(fx, fy, fz)) {
                        // Weighted block placement (90% Lignum Caro, 9% Splintered Oak, 1% Oak Planks)
                        BlockState blockToPlace = getWeightedSpongeBlock(blockX, currentY, blockZ);
                        level.setBlock(targetPos, blockToPlace, 2);
                    } else {
                        level.setBlock(targetPos, AIR_STATE, 2);

                        // Placement inside voids
                        if (fy > 1 && fy < SPONGE_SIZE - 1 && fx > 1 && fx < SPONGE_SIZE - 1 && fz > 1 && fz < SPONGE_SIZE - 1) {
                            BlockPos belowPos = targetPos.below();
                            BlockState stateBelow = level.getBlockState(belowPos);
                            boolean hasSolidFloor = !stateBelow.isAir() && !stateBelow.is(Blocks.BEDROCK);

                            if (hasSolidFloor) {
                                if (allowChest && context.random().nextFloat() < 0.002f) {
                                    level.setBlock(targetPos, Blocks.CHEST.defaultBlockState(), 3);
                                    if (level.getBlockEntity(targetPos) instanceof RandomizableContainerBlockEntity container) {
                                        container.setLootTable(SPONGE_LOOT_TABLE);
                                    }
                                } else if (allowFurnace && context.random().nextFloat() < 0.001f) {
                                    level.setBlock(targetPos, FURNACE_STATE, 2);
                                } else if (allowBed && context.random().nextFloat() < 0.001f) {
                                    BlockPos headPos = targetPos.east();
                                    boolean headInChunk = (headPos.getX() & (~15)) == minX;
                                    
                                    if (headInChunk && level.getBlockState(headPos).isAir() && !level.getBlockState(headPos.below()).isAir()) {
                                        BlockState footState = Blocks.RED_BED.defaultBlockState()
                                                .setValue(BedBlock.PART, BedPart.FOOT)
                                                .setValue(BedBlock.FACING, Direction.EAST);
                                        BlockState headState = Blocks.RED_BED.defaultBlockState()
                                                .setValue(BedBlock.PART, BedPart.HEAD)
                                                .setValue(BedBlock.FACING, Direction.EAST);

                                        level.setBlock(targetPos, footState, 2);
                                        level.setBlock(headPos, headState, 2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private static BlockState getWeightedSpongeBlock(int x, int y, int z) {
        long posHash = (long) x * 3129871L ^ (long) y * 11687L ^ (long) z * 823187L;
        float roll = (Math.abs(posHash % 10000) / 10000.0f);

        if (roll < 0.90f) {
            return LIGNUM_CARO;              // 90%
        } else if (roll < 0.99f) {
            return SPLINTERED_OAK_PLANKS;    // 9%
        } else {
            return OAK_PLANKS;               // 1%
        }
    }

    private static boolean isMengerSpongeSolid(int x, int y, int z) {
        if (x < 0 || x >= SPONGE_SIZE || y < 0 || y >= SPONGE_SIZE || z < 0 || z >= SPONGE_SIZE) {
            return false;
        }

        while (x > 0 || y > 0 || z > 0) {
            int checkX = x % 3;
            int checkY = y % 3;
            int checkZ = z % 3;

            if ((checkX == 1 ? 1 : 0) + (checkY == 1 ? 1 : 0) + (checkZ == 1 ? 1 : 0) >= 2) {
                return false;
            }

            x /= 3;
            y /= 3;
            z /= 3;
        }
        return true;
    }
} // 1.21.1