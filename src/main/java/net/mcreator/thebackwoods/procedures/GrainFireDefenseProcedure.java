package net.mcreator.thebackwoods.procedures;
// 1.21.1 neoforge
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.properties.Property;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import net.minecraft.core.Direction;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public class GrainFireDefenseProcedure {

    private static final int CHECK_INTERVAL_TICKS = 8;
    private static final int FIRE_SCAN_RADIUS_XZ = 128;
    private static final int FIRE_SCAN_RADIUS_Y = 48;
    private static final int BIG_FIRE_THRESHOLD = 40; 
    private static final int PURGE_COOLDOWN_TICKS = 20 * 6; // 6 sec

    private static final int THREAT_RADIUS_XZ = 48;
    private static final int THREAT_RADIUS_Y = 24;

    private static final String NBT_COOLDOWN_KEY = "grain_fire_purge_cd";

	private static final ResourceKey<Level> THE_GRAIN = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.parse("the_backwoods:the_grain")
    );

    private static final ResourceKey<Level> BACKWOODS = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.parse("the_backwoods:backwoods")
    );

    // 1. Declare the resource key for the sub strata dimension
    private static final ResourceKey<Level> THE_SUB_STRATA = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.parse("the_backwoods:the_sub_strata")
    );

    private static final ResourceKey<Level> THE_PETRIFIED_WEALD = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.parse("the_backwoods:the_petrified_weald")
    );

    // 2. Add it to the immutable Set of allowed dimensions
    private static final Set<ResourceKey<Level>> VALID_DIMENSIONS = Set.of(THE_GRAIN, BACKWOODS, THE_SUB_STRATA, THE_PETRIFIED_WEALD);
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null) return;

        Level level = player.level();
        if (level.isClientSide()) return;

        // Ensure we are in one of the approved dimensions
        if (!VALID_DIMENSIONS.contains(level.dimension())) return;

        // Process active domino purge queue every tick
        processDominoPurge(level, player);
        
        boolean isStrongDim = level.dimension() == THE_GRAIN || level.dimension() == THE_SUB_STRATA;
        int interval = isStrongDim ? 4 : CHECK_INTERVAL_TICKS;
        if (player.tickCount % interval != 0) return;

        BlockPos center = player.blockPosition();

        // 1. SILENT CLEANUP (Beyond close range, up to full SCAN_RADIUS_XZ)
        // Continuously and silently sweeps coordinates on a fast background slice to delete any fire, no blindness or sounds
        silentPurgeSliced(level, center, player.tickCount);

        // 2. ACTIVE THREAT RADIUS CHECK
        int cd = player.getPersistentData().getInt(NBT_COOLDOWN_KEY);
        if (cd > 0) {
            player.getPersistentData().putInt(NBT_COOLDOWN_KEY, Math.max(0, cd - interval));
            return;
        }

        int threshold = isStrongDim ? 15 : BIG_FIRE_THRESHOLD;
        int fireCount = countFireInThreatRadius(level, center);
        if (fireCount < threshold) return;

        // Perform the domino threat purge
        startDominoPurge(level, player, center);

        // Set the cooldown
        int cooldown = isStrongDim ? 40 : PURGE_COOLDOWN_TICKS;
        player.getPersistentData().putInt(NBT_COOLDOWN_KEY, cooldown);
    }

    private static void silentPurgeSliced(Level level, BlockPos center, int tickCount) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        net.minecraft.server.level.ServerChunkCache cache = serverLevel.getChunkSource();

        boolean isStrongDim = level.dimension() == THE_GRAIN || level.dimension() == THE_SUB_STRATA;
        int totalSlices = isStrongDim ? 3 : 12;
        int interval = isStrongDim ? 4 : CHECK_INTERVAL_TICKS;
        int currentSlice = (int) ((tickCount / interval) % totalSlices);

        int range = FIRE_SCAN_RADIUS_XZ;
        int sliceWidth = (range * 2 + 1 + totalSlices - 1) / totalSlices;
        int startDx = -range + currentSlice * sliceWidth;
        int endDx = Math.min(range, startDx + sliceWidth - 1);

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        int minY = cy - FIRE_SCAN_RADIUS_Y;
        int maxY = cy + FIRE_SCAN_RADIUS_Y;

        for (int dx = startDx; dx <= endDx; dx++) {
            int absX = cx + dx;
            int cxChunk = absX >> 4;
            int lastChunkZ = -999;
            net.minecraft.world.level.chunk.LevelChunk currentChunk = null;

            for (int dz = -FIRE_SCAN_RADIUS_XZ; dz <= FIRE_SCAN_RADIUS_XZ; dz++) {
                int absZ = cz + dz;
                int czChunk = absZ >> 4;

                if (currentChunk == null || czChunk != lastChunkZ) {
                    lastChunkZ = czChunk;
                    currentChunk = cache.getChunkNow(cxChunk, czChunk);
                }

                if (currentChunk == null) continue;

                int localMinY = Math.max(currentChunk.getMinBuildHeight(), minY);
                int localMaxY = Math.min(currentChunk.getMaxBuildHeight() - 1, maxY);

                int minSection = (localMinY - currentChunk.getMinBuildHeight()) >> 4;
                int maxSection = (localMaxY - currentChunk.getMinBuildHeight()) >> 4;

                net.minecraft.world.level.chunk.LevelChunkSection[] sections = currentChunk.getSections();

                for (int secIndex = minSection; secIndex <= maxSection; secIndex++) {
                    if (secIndex < 0 || secIndex >= sections.length) continue;
                    net.minecraft.world.level.chunk.LevelChunkSection section = sections[secIndex];
                    if (section == null || section.hasOnlyAir()) continue;

                    int secMinY = currentChunk.getMinBuildHeight() + (secIndex << 4);
                    int startY = Math.max(localMinY, secMinY);
                    int endY = Math.min(localMaxY, secMinY + 15);

                    for (int absY = startY; absY <= endY; absY++) {
                        int dy = absY - cy;
                        if (Math.abs(dx) <= THREAT_RADIUS_XZ && Math.abs(dy) <= THREAT_RADIUS_Y && Math.abs(dz) <= THREAT_RADIUS_XZ) {
                            continue;
                        }
                        BlockState state = section.getBlockState(absX & 15, absY & 15, absZ & 15);
                        if (isFire(state)) {
                            p.set(absX, absY, absZ);
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                            if (level.random.nextFloat() < 0.05f) {
                                BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse("block.fire.extinguish")).ifPresent(extinguish -> {
                                    level.playSound(null, p, extinguish, SoundSource.BLOCKS, 0.2f, 1.2f + level.random.nextFloat() * 0.4f);
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    private static int countFireInThreatRadius(Level level, BlockPos center) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return 0;
        net.minecraft.server.level.ServerChunkCache cache = serverLevel.getChunkSource();

        int count = 0;
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        int minY = cy - THREAT_RADIUS_Y;
        int maxY = cy + THREAT_RADIUS_Y;

        for (int dx = -THREAT_RADIUS_XZ; dx <= THREAT_RADIUS_XZ; dx += 4) {
            int absX = cx + dx;
            int cxChunk = absX >> 4;
            int lastChunkZ = -999;
            net.minecraft.world.level.chunk.LevelChunk currentChunk = null;

            for (int dz = -THREAT_RADIUS_XZ; dz <= THREAT_RADIUS_XZ; dz += 4) {
                int absZ = cz + dz;
                int czChunk = absZ >> 4;

                if (currentChunk == null || czChunk != lastChunkZ) {
                    lastChunkZ = czChunk;
                    currentChunk = cache.getChunkNow(cxChunk, czChunk);
                }

                if (currentChunk == null) continue;

                int localMinY = Math.max(currentChunk.getMinBuildHeight(), minY);
                int localMaxY = Math.min(currentChunk.getMaxBuildHeight() - 1, maxY);

                int minSection = (localMinY - currentChunk.getMinBuildHeight()) >> 4;
                int maxSection = (localMaxY - currentChunk.getMinBuildHeight()) >> 4;

                net.minecraft.world.level.chunk.LevelChunkSection[] sections = currentChunk.getSections();

                for (int secIndex = minSection; secIndex <= maxSection; secIndex++) {
                    if (secIndex < 0 || secIndex >= sections.length) continue;
                    net.minecraft.world.level.chunk.LevelChunkSection section = sections[secIndex];
                    if (section == null || section.hasOnlyAir()) continue;

                    int secMinY = currentChunk.getMinBuildHeight() + (secIndex << 4);
                    int startY = Math.max(localMinY, secMinY);
                    int endY = Math.min(localMaxY, secMinY + 15);

                    for (int dy = -THREAT_RADIUS_Y; dy <= THREAT_RADIUS_Y; dy += 4) {
                        int absY = cy + dy;
                        if (absY >= startY && absY <= endY) {
                            BlockState state = section.getBlockState(absX & 15, absY & 15, absZ & 15);
                            if (isFire(state)) count += 64;
                        }
                    }
                }
            }
        }
        return count;
    }

    private static void purgeFireInThreatRadius(Level level, BlockPos center) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        net.minecraft.server.level.ServerChunkCache cache = serverLevel.getChunkSource();

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        int minY = cy - THREAT_RADIUS_Y;
        int maxY = cy + THREAT_RADIUS_Y + 1; // Includes offset

        for (int dx = -THREAT_RADIUS_XZ; dx <= THREAT_RADIUS_XZ; dx += 2) {
            int absX = cx + dx;
            int cxChunk = absX >> 4;
            int lastChunkZ = -999;
            net.minecraft.world.level.chunk.LevelChunk currentChunk = null;

            for (int dz = -THREAT_RADIUS_XZ; dz <= THREAT_RADIUS_XZ; dz += 2) {
                int absZ = cz + dz;
                int czChunk = absZ >> 4;

                if (currentChunk == null || czChunk != lastChunkZ) {
                    lastChunkZ = czChunk;
                    currentChunk = cache.getChunkNow(cxChunk, czChunk);
                }

                if (currentChunk == null) continue;

                int localMinY = Math.max(currentChunk.getMinBuildHeight(), minY);
                int localMaxY = Math.min(currentChunk.getMaxBuildHeight() - 1, maxY + 1);

                int minSection = (localMinY - currentChunk.getMinBuildHeight()) >> 4;
                int maxSection = (localMaxY - currentChunk.getMinBuildHeight()) >> 4;

                net.minecraft.world.level.chunk.LevelChunkSection[] sections = currentChunk.getSections();

                for (int secIndex = minSection; secIndex <= maxSection; secIndex++) {
                    if (secIndex < 0 || secIndex >= sections.length) continue;
                    net.minecraft.world.level.chunk.LevelChunkSection section = sections[secIndex];
                    if (section == null || section.hasOnlyAir()) continue;

                    int secMinY = currentChunk.getMinBuildHeight() + (secIndex << 4);
                    int startY = Math.max(localMinY, secMinY);
                    int endY = Math.min(localMaxY, secMinY + 15);

                    for (int dy = -THREAT_RADIUS_Y; dy <= THREAT_RADIUS_Y; dy += 2) {
                        for (int ox = 0; ox < 2; ox++) {
                            int checkX = absX + ox;
                            for (int oz = 0; oz < 2; oz++) {
                                int checkZ = absZ + oz;
                                for (int oy = 0; oy < 2; oy++) {
                                    int absY = cy + dy + oy;
                                    if (absY >= startY && absY <= endY) {
                                        // Need to be careful: ox or oz might cross chunk boundary. 
                                        // Let's get the state correctly using the correct section if we want absolute safety,
                                        // but since they are small offsets, we can either get it from currentChunk if in same chunk,
                                        // or fallback to section/world. 
                                        // Let's just do a direct check:
                                        int subCxChunk = checkX >> 4;
                                        int subCzChunk = checkZ >> 4;
                                        if (subCxChunk == cxChunk && subCzChunk == czChunk) {
                                            BlockState state = section.getBlockState(checkX & 15, absY & 15, checkZ & 15);
                                            if (isFire(state)) {
                                                p.set(checkX, absY, checkZ);
                                                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                                                if (level.random.nextFloat() < 0.15f) {
                                                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.05);
                                                    BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse("block.fire.extinguish")).ifPresent(extinguish -> {
                                                        level.playSound(null, p, extinguish, SoundSource.BLOCKS, 0.25f, 1.2f + serverLevel.random.nextFloat() * 0.4f);
                                                    });
                                                }
                                            }
                                        } else {
                                            // Fallback to world for boundary cross
                                            p.set(checkX, absY, checkZ);
                                            if (isFire(level.getBlockState(p))) {
                                                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                                                if (level.random.nextFloat() < 0.15f) {
                                                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.05);
                                                    BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse("block.fire.extinguish")).ifPresent(extinguish -> {
                                                        level.playSound(null, p, extinguish, SoundSource.BLOCKS, 0.25f, 1.2f + serverLevel.random.nextFloat() * 0.4f);
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void startDominoPurge(Level level, Player player, BlockPos center) {
        List<BlockPos> firePositions = new ArrayList<>();
        List<BlockPos> plankPositions = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int dx = -THREAT_RADIUS_XZ; dx <= THREAT_RADIUS_XZ; dx += 2) {
            for (int dz = -THREAT_RADIUS_XZ; dz <= THREAT_RADIUS_XZ; dz += 2) {
                for (int dy = -THREAT_RADIUS_Y; dy <= THREAT_RADIUS_Y; dy += 2) {
                    for (int ox = 0; ox < 2; ox++) {
                        for (int oy = 0; oy < 2; oy++) {
                            for (int oz = 0; oz < 2; oz++) {
                                p.set(cx + dx + ox, cy + dy + oy, cz + dz + oz);
                                BlockState state = level.getBlockState(p);
                                if (isFire(state)) {
                                    firePositions.add(p.immutable());
                                } else if (isSmolderingPlank(state)) {
                                    plankPositions.add(p.immutable());
                                }
                            }
                        }
                    }
                }
            }
        }

        if (firePositions.isEmpty() && plankPositions.isEmpty()) return;

        firePositions.sort((p1, p2) -> Double.compare(p1.distSqr(center), p2.distSqr(center)));
        
        // Sort plankPositions like an infection/spore spread wave
        plankPositions = sortAsInfectionSpread(plankPositions, center);

        if (firePositions.size() > 1000) {
            firePositions = firePositions.subList(0, 1000);
        }
        
        ListTag fireList = new ListTag();
        for (BlockPos pos : firePositions) {
            fireList.add(StringTag.valueOf(pos.getX() + "," + pos.getY() + "," + pos.getZ()));
        }

        if (!fireList.isEmpty()) {
            player.getPersistentData().put("grain_domino_queue", fireList);
        }

        if (!plankPositions.isEmpty()) {
            // Sort plankPositions like an infection/spore spread wave
            plankPositions = sortAsInfectionSpread(plankPositions, center);

            ListTag plankList = new ListTag();
            for (BlockPos pos : plankPositions) {
                plankList.add(StringTag.valueOf(pos.getX() + "," + pos.getY() + "," + pos.getZ()));
            }

            if (!plankList.isEmpty()) {
                player.getPersistentData().put("grain_plank_queue", plankList);
                player.getPersistentData().putInt("grain_plank_total", plankList.size());
            }
        }

        // Only play extinguish sound if there is actual fire or smoldering planks
        if (!firePositions.isEmpty() || !plankPositions.isEmpty()) {
            if (!firePositions.isEmpty()) {
                BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse("block.fire.extinguish")).ifPresent(extinguish -> {
                    level.playSound(null, center, extinguish, SoundSource.BLOCKS, 1.3f, 0.85f);
                });
            }
        }
    }

    private static boolean isBurntBlock(BlockState state) {
        if (state == null || state.isAir()) return false;
        Block block = state.getBlock();
        ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(block);
        if (reg != null) {
            String namespace = reg.getNamespace();
            String path = reg.getPath().toLowerCase();
            if (namespace.equals("burnt") && (path.equals("wood_fire") || path.matches("wood_fire_[2-9]"))) {
                return false;
            }
            if (path.contains("brick") || path.contains("stone") || path.contains("tile") || path.contains("glass") || path.contains("metal") || path.contains("ore")) {
                return false;
            }
            if (namespace.equals("burnt") || namespace.equals("burnt_basic") || namespace.equals("firesdelight") || namespace.equals("nether_delight")) {
                return true;
            }
            if (path.contains("burnt") || path.contains("smoldering") || path.contains("sooty") || path.contains("charred")) {
                return true;
            }
        }
        return false;
    }

    private static BlockState getHealthyEquivalent(BlockState burntState) {
        if (burntState == null) {
            return TheBackwoodsModBlocks.LIGNUM_CARO.get().defaultBlockState();
        }
        Block block = burntState.getBlock();
        ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(block);
        if (reg != null) {
            String path = reg.getPath().toLowerCase();
            if (path.contains("broken_log")) {
                boolean isLog = path.contains("log") || path.contains("stem") || path.contains("wood") || path.contains("hyphae") || path.contains("bark");
                Block healthyBlock = path.contains("spruce") ? (isLog ? Blocks.SPRUCE_LOG : Blocks.SPRUCE_PLANKS) :
                                     path.contains("birch") ? (isLog ? Blocks.BIRCH_LOG : Blocks.BIRCH_PLANKS) :
                                     path.contains("jungle") ? (isLog ? Blocks.JUNGLE_LOG : Blocks.JUNGLE_PLANKS) :
                                     path.contains("acacia") ? (isLog ? Blocks.ACACIA_LOG : Blocks.ACACIA_PLANKS) :
                                     path.contains("dark_oak") ? (isLog ? Blocks.DARK_OAK_LOG : Blocks.DARK_OAK_PLANKS) :
                                     path.contains("mangrove") ? (isLog ? Blocks.MANGROVE_LOG : Blocks.MANGROVE_PLANKS) :
                                     path.contains("cherry") ? (isLog ? Blocks.CHERRY_LOG : Blocks.CHERRY_PLANKS) :
                                     (isLog ? Blocks.OAK_LOG : Blocks.OAK_PLANKS);
                if (healthyBlock != null) {
                    return copyBlockStateProperties(burntState, healthyBlock.defaultBlockState());
                }
            }
        }
        return copyBlockStateProperties(burntState, TheBackwoodsModBlocks.LIGNUM_CARO.get().defaultBlockState());
    }

    private static List<BlockPos> sortAsInfectionSpread(List<BlockPos> positions, BlockPos center) {
        if (positions.isEmpty()) return positions;
        Set<BlockPos> remaining = new HashSet<>(positions);
        List<BlockPos> ordered = new ArrayList<>();

        BlockPos seed = positions.get(0);
        double minDist = seed.distSqr(center);
        for (BlockPos p : positions) {
            double d = p.distSqr(center);
            if (d < minDist) {
                minDist = d;
                seed = p;
            }
        }

        List<BlockPos> frontier = new ArrayList<>();
        frontier.add(seed);
        remaining.remove(seed);
        ordered.add(seed);

        while (!frontier.isEmpty() && !remaining.isEmpty()) {
            BlockPos current = frontier.remove(0);
            List<BlockPos> neighbors = new ArrayList<>();
            for (Direction d : Direction.values()) {
                BlockPos nPos = current.relative(d);
                if (remaining.contains(nPos)) {
                    neighbors.add(nPos);
                }
            }
            for (BlockPos n : neighbors) {
                remaining.remove(n);
                ordered.add(n);
                frontier.add(n);
            }
            if (frontier.isEmpty() && !remaining.isEmpty()) {
                BlockPos nextSeed = remaining.iterator().next();
                double minNextDist = nextSeed.distSqr(current);
                for (BlockPos r : remaining) {
                    double d = r.distSqr(current);
                    if (d < minNextDist) {
                        minNextDist = d;
                        nextSeed = r;
                    }
                }
                remaining.remove(nextSeed);
                ordered.add(nextSeed);
                frontier.add(nextSeed);
            }
        }

        for (BlockPos r : remaining) {
            ordered.add(r);
        }
        return ordered;
    }

    private static void processDominoPurge(Level level, Player player) {
        CompoundTag data = player.getPersistentData();
        boolean hasFire = data.contains("grain_domino_queue", Tag.TAG_LIST);
        boolean hasPlanks = data.contains("grain_plank_queue", Tag.TAG_LIST);

        if (!hasFire && !hasPlanks) return;

        if (hasFire) {
            ListTag list = data.getList("grain_domino_queue", Tag.TAG_STRING);
            if (list.isEmpty()) {
                data.remove("grain_domino_queue");
            } else {
                int extinguishCount = Math.min(list.size(), 12);
                List<BlockPos> toExtinguish = new ArrayList<>();

                for (int i = 0; i < extinguishCount; i++) {
                    String s = list.getString(0);
                    list.remove(0);
                    BlockPos p = parsePos(s);
                    if (p != null) {
                        toExtinguish.add(p);
                    }
                }

                if (list.isEmpty()) {
                    data.remove("grain_domino_queue");
                } else {
                    data.put("grain_domino_queue", list);
                }

                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    for (BlockPos pos : toExtinguish) {
                        if (isFire(level.getBlockState(pos))) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.15, 0.15, 0.15, 0.05);
                            if (serverLevel.random.nextFloat() < 0.2f) {
                                BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse("block.fire.extinguish")).ifPresent(extinguish -> {
                                    level.playSound(null, pos, extinguish, SoundSource.BLOCKS, 0.25f, 1.2f + serverLevel.random.nextFloat() * 0.4f);
                                });
                            }
                        }
                    }
                }
            }
        }

        if (hasPlanks) {
            int plankDelay = data.contains("grain_plank_delay", Tag.TAG_INT) ? data.getInt("grain_plank_delay") : 0;
            if (plankDelay > 0) {
                data.putInt("grain_plank_delay", plankDelay - 1);
            } else {
                data.putInt("grain_plank_delay", 14); // Much slower infection wave (every 14 ticks)
                ListTag list = data.getList("grain_plank_queue", Tag.TAG_STRING);
                if (list.isEmpty()) {
                    data.remove("grain_plank_queue");
                    data.remove("grain_plank_delay");
                } else {
                    int processCount = Math.min(list.size(), 1);
                    List<BlockPos> toRestore = new ArrayList<>();

                    for (int i = 0; i < processCount; i++) {
                        String s = list.getString(0);
                        list.remove(0);
                        BlockPos p = parsePos(s);
                        if (p != null) {
                            toRestore.add(p);
                        }
                    }

                    if (list.isEmpty()) {
                        data.remove("grain_plank_queue");
                        data.remove("grain_plank_delay");
                        data.remove("grain_plank_total");
                    } else {
                        data.put("grain_plank_queue", list);
                    }

                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        int totalInitial = data.contains("grain_plank_total") ? data.getInt("grain_plank_total") : 50;
                        double conversionChance;
                        if (totalInitial <= 100) {
                            conversionChance = 0.05 + (double) totalInitial * (0.45 / 100.0);
                        } else {
                            conversionChance = 0.50 + Math.min(0.48, (double) (totalInitial - 100) * (0.48 / 100.0));
                        }
                        conversionChance = Math.max(0.02, Math.min(0.98, conversionChance + (serverLevel.random.nextDouble() * 0.1 - 0.05)));
                        
                        for (BlockPos pos : toRestore) {
                            BlockState currentState = level.getBlockState(pos);
                            if (isSmolderingPlank(currentState)) {
                                if (serverLevel.random.nextDouble() < conversionChance) {
                                    BlockState restoredState = getRestoredPlankState(currentState, serverLevel.random);
                                    level.setBlock(pos, restoredState, 3);
                                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.02);
                                    if (serverLevel.random.nextFloat() < 0.15f) {
                                        BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse("block.cherry_wood.place")).ifPresent(placeSound -> {
                                            level.playSound(null, pos, placeSound, SoundSource.BLOCKS, 0.4f, 0.8f + serverLevel.random.nextFloat() * 0.3f);
                                        });
                                    }
                                } else {
                                    serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.01);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isSmolderingPlank(BlockState state) {
        if (state == null || state.isAir()) return false;
        Block block = state.getBlock();
        ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(block);
        if (reg != null) {
            String path = reg.getPath().toLowerCase();
            return path.contains("smoldering") && path.contains("plank");
        }
        return false;
    }

    private static BlockState getRestoredPlankState(BlockState burntState, net.minecraft.util.RandomSource random) {
        Block targetBlock = random.nextBoolean() ? TheBackwoodsModBlocks.LIGNUM_CARO.get() : Blocks.OAK_PLANKS;
        return copyBlockStateProperties(burntState, targetBlock.defaultBlockState());
    }

    private static BlockState copyBlockStateProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> property : from.getProperties()) {
            if (result.hasProperty(property)) {
                result = copyPropertyHelper(from, result, property);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyPropertyHelper(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    private static BlockPos parsePos(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            String[] parts = s.split(",");
            if (parts.length == 3) {
                return new BlockPos(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
                );
            }
        } catch (Exception e) {}
        return null;
    }

    private static boolean isFire(BlockState state) {
        if (state == null || state.isAir()) return false;
        Block block = state.getBlock();
        ResourceLocation reg = BuiltInRegistries.BLOCK.getKey(block);
        if (reg != null) {
            String namespace = reg.getNamespace();
            String path = reg.getPath().toLowerCase();
            // Explicitly include wood_fire and wood_fire_2 to 9 under burnt modID so FireDefense handles extinguishing them
            if (namespace.equals("burnt") && (path.equals("wood_fire") || path.matches("wood_fire_[2-9]"))) {
                return true;
            }
        }
        return state.getBlock() instanceof BaseFireBlock
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE);
    } // 1.21.1
}
