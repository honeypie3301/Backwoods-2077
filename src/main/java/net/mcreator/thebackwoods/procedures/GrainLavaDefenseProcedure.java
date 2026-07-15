package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import java.util.Set;

@EventBusSubscriber
public class GrainLavaDefenseProcedure {

    private static final int CHECK_INTERVAL_TICKS = 8;
    private static final int LAVA_SCAN_RADIUS_XZ = 64;
    private static final int LAVA_SCAN_RADIUS_Y = 24;
    private static final int BIG_LAVA_THRESHOLD = 10;
    private static final int PURGE_COOLDOWN_TICKS = 20 * 100; // 6 sec

    private static final String NBT_COOLDOWN_KEY = "grain_lava_purge_cd";

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

    // 2. Add it to the immutable Set of allowed dimensions
    private static final Set<ResourceKey<Level>> VALID_DIMENSIONS = Set.of(THE_GRAIN, BACKWOODS, THE_SUB_STRATA);

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null) return;

        Level level = player.level();
        if (level.isClientSide()) return;

        if (!VALID_DIMENSIONS.contains(level.dimension())) return;
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) return;

        int cd = player.getPersistentData().getInt(NBT_COOLDOWN_KEY);
        if (cd > 0) {
            player.getPersistentData().putInt(NBT_COOLDOWN_KEY, Math.max(0, cd - CHECK_INTERVAL_TICKS));
            return;
        }

        BlockPos center = player.blockPosition();

        int lavaCount = countLavaInRadius(level, center);
        if (lavaCount < BIG_LAVA_THRESHOLD) return;

        // Apply visual concealment effects
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 5, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 5, false, false));

        // Play cave ambience sound
        BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse("minecraft:ambient.cave")).ifPresent(cave -> {
            level.playSound(null, center, cave, SoundSource.AMBIENT, 1.3f, 0.85f);
        });

        // Purge lava
        purgeLavaInRadius(level, center);

        player.getPersistentData().putInt(NBT_COOLDOWN_KEY, PURGE_COOLDOWN_TICKS);
    }

    private static int countLavaInRadius(Level level, BlockPos center) {
        int count = 0;
        for (int dx = -LAVA_SCAN_RADIUS_XZ; dx <= LAVA_SCAN_RADIUS_XZ; dx += 2) {
            for (int dz = -LAVA_SCAN_RADIUS_XZ; dz <= LAVA_SCAN_RADIUS_XZ; dz += 2) {
                for (int dy = -LAVA_SCAN_RADIUS_Y; dy <= LAVA_SCAN_RADIUS_Y; dy += 2) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (isLava(level.getBlockState(p))) count += 8;
                }
            }
        }
        return count;
    }

    private static void purgeLavaInRadius(Level level, BlockPos center) {
        for (int dx = -LAVA_SCAN_RADIUS_XZ; dx <= LAVA_SCAN_RADIUS_XZ; dx++) {
            for (int dz = -LAVA_SCAN_RADIUS_XZ; dz <= LAVA_SCAN_RADIUS_XZ; dz++) {
                for (int dy = -LAVA_SCAN_RADIUS_Y; dy <= LAVA_SCAN_RADIUS_Y; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (isLava(level.getBlockState(p))) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static boolean isLava(BlockState state) {
        return state.getFluidState().is(Fluids.LAVA)
                || state.getFluidState().is(Fluids.FLOWING_LAVA)
                || state.is(Blocks.LAVA);
    }
}