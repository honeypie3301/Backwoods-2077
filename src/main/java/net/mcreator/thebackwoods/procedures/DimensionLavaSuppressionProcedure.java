package net.mcreator.thebackwoods.procedures;
// 1.21.8 neo / 1.21.1 neo

import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class DimensionLavaSuppressionProcedure {

    // --- TUNABLE CONSTANTS ---
    private static final int CHECK_INTERVAL_TICKS = 3;           // Scan roughly 7 times per second
    private static final int SCAN_RADIUS_XZ = 128;               // Expanded horizontal range (257x257 block chunk diameter)
    private static final int Y_THRESHOLD = -54;                  // only suppress at or below this Y
    private static final int MAX_CHECKS_PER_PLAYER_SCAN = 1200000; // High budget so the scan actually completes
    private static final int MAX_REMOVALS_PER_PLAYER_SCAN = 8192; // Allows removing vast sections of lakes at once
    
    private static final long PLAYER_LAVA_PROTECTION_TICKS = 20L * 60L * 10L; // 10 minutes
    private static final int PROTECTION_CLEANUP_INTERVAL_TICKS = 20 * 30;     // every 30 seconds
    // -------------------------

    // OPTIMIZATION: Store primitives (Long coordinates) instead of heavy concatenated Strings
    private static final Map<Long, Long> PLAYER_PLACED_LAVA = new ConcurrentHashMap<>();

    private static final ResourceKey<Level> BACKWOODS = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.parse("the_backwoods:backwoods"));
    private static final ResourceKey<Level> THE_GRAIN = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.parse("the_backwoods:the_grain"));
    private static final ResourceKey<Level> THE_SUB_STRATA = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.parse("the_backwoods:the_sub_strata"));

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!event.getPlacedBlock().getFluidState().is(Fluids.LAVA)) return;
        if (!(event.getLevel() instanceof Level level)) return;

        ResourceKey<Level> dim = level.dimension();
        if (!isTargetDimension(dim)) return;

        PLAYER_PLACED_LAVA.put(event.getPos().asLong(), level.getGameTime());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) return;

        Level level = player.level();
        ResourceKey<Level> dim = level.dimension();

        if (!isTargetDimension(dim)) return;

        // Trigger scan only when the player is deep underground where underground pools exist (-30 and below)
        if (player.getY() > -30) return;

        cleanupOldPlayerPlacedLava(level);

        BlockPos center = player.blockPosition();
        
        // Slicing to distribute performance budget over multiple ticks
        int totalSlices = 12;
        int currentSlice = (int) ((player.tickCount / CHECK_INTERVAL_TICKS) % totalSlices);
        
        int range = SCAN_RADIUS_XZ;
        int sliceWidth = (range * 2 + 1 + totalSlices - 1) / totalSlices;
        int startDx = -range + currentSlice * sliceWidth;
        int endDx = Math.min(range, startDx + sliceWidth - 1);

        // OPTIMIZATION: Reusable pos pointers prevent billions of temporary object allocations
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        int checked = 0;
        int removed = 0;

        // ABSOLUTE Y-BAND OPTIMIZATION:
        // Scanning relative to the player's fine vertical position introduces massive overlapping checks and Y-levels that don't need suppression.
        // Instead, we target the exact fixed absolute band where deep slate lava pools exist (-64 to -54), and clean up fire up to 5 blocks above (-53 to -49).
        // This is 3 times faster and perfectly fixes any fire from escaping on top!
        // We also check chunk loading ONCE per block column (X, Z) instead of on every single Y coord lookup, speeding checks up by 16x.
        for (int dx = startDx; dx <= endDx; dx++) {
            int absX = center.getX() + dx;
            for (int dz = -SCAN_RADIUS_XZ; dz <= SCAN_RADIUS_XZ; dz++) {
                int absZ = center.getZ() + dz;

                mutablePos.set(absX, -64, absZ);
                if (!level.hasChunkAt(mutablePos)) continue;

                for (int absY = -64; absY <= -49; absY++) {
                    checked++;
                    if (checked > MAX_CHECKS_PER_PLAYER_SCAN) return;

                    mutablePos.set(absX, absY, absZ);
                    BlockState state = level.getBlockState(mutablePos);

                    if (absY <= Y_THRESHOLD) {
                        // --- LAVA PLACEMENT ZONE (-64 to -54) ---
                        // Skip any coordinates protected by player placement actions
                        if (PLAYER_PLACED_LAVA.containsKey(mutablePos.asLong())) continue;

                        if (state.is(Blocks.LAVA) || state.getFluidState().is(Fluids.LAVA)) {
                            level.setBlock(mutablePos, Blocks.OAK_PLANKS.defaultBlockState(), 2);
                            removed++;

                            // Extinguish adjacent fire blocks immediately around the newly converted wood
                            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                                if (dir == net.minecraft.core.Direction.DOWN) continue;
                                neighborPos.set(mutablePos.getX() + dir.getStepX(), mutablePos.getY() + dir.getStepY(), mutablePos.getZ() + dir.getStepZ());
                                if (level.hasChunkAt(neighborPos)) {
                                    BlockState neighborState = level.getBlockState(neighborPos);
                                    if (neighborState.getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock 
                                        || neighborState.is(Blocks.FIRE) 
                                        || neighborState.is(Blocks.SOUL_FIRE)) {
                                        level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), 3);
                                    }
                                }
                            }

                            if (removed >= MAX_REMOVALS_PER_PLAYER_SCAN) {
                                return;
                            }
                        }
                    } else {
                        // --- THE ACTIVE FIRE CEILING ZONE (-53 to -49) ---
                        // Extinguish any fire blocks in the air layers sitting directly on top of the suppression threshold
                        if (state.getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock 
                            || state.is(Blocks.FIRE) 
                            || state.is(Blocks.SOUL_FIRE)) {
                            level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    private static void cleanupOldPlayerPlacedLava(Level level) {
        if (level.getGameTime() % PROTECTION_CLEANUP_INTERVAL_TICKS != 0) return;

        long now = level.getGameTime();
        PLAYER_PLACED_LAVA.entrySet().removeIf(entry -> now - entry.getValue() > PLAYER_LAVA_PROTECTION_TICKS);
    }

    private static boolean isTargetDimension(ResourceKey<Level> dim) {
        return dim.equals(BACKWOODS) || dim.equals(THE_GRAIN) || dim.equals(THE_SUB_STRATA);
    }
}