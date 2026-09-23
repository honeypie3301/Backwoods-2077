package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.mcreator.thebackwoods.TheBackwoodsMod;

import javax.annotation.Nullable;
import net.neoforged.bus.api.Event;
import java.util.Collections;

@EventBusSubscriber
public class GrainAfterDeathRespawnProcedure {

    // 1. DATA BRIDGE: Preserves both tags and saved inventory through the death screen barrier
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player oldPlayer = event.getOriginal();
            Player newPlayer = event.getEntity();
            
            if (oldPlayer != null && newPlayer != null) {
                CompoundTag oldData = oldPlayer.getPersistentData();
                CompoundTag newData = newPlayer.getPersistentData();
                
                if (oldData != null && newData != null) {
                    // Corrected for 1.21.1: direct booleans (no .orElse required)
                    boolean hadGrainTag = oldData.getBoolean("died_in_grain");
                    boolean hadSubstrataTag = oldData.getBoolean("died_in_substrata");
                    
                    newData.putBoolean("died_in_grain", hadGrainTag);
                    newData.putBoolean("died_in_substrata", hadSubstrataTag);

                    // Preserve saved items
                    if (oldData.contains("saved_death_items")) {
                        newData.put("saved_death_items", oldData.get("saved_death_items"));
                    }
                }
            }
        }
    }

    // 2. SAVING INVENTORY ON DEATH: Intercepts drops and stores them in player persistent NBT
    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player != null && !player.level().isClientSide()) {
                if (player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
                    return;
                }

                ResourceKey<Level> grainKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("the_backwoods", "the_grain"));
                ResourceKey<Level> subStrataKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("the_backwoods", "the_sub_strata"));
                ResourceKey<Level> currentDim = player.level().dimension();

                if (currentDim == grainKey || currentDim == subStrataKey) {
                    ListTag listTag = new ListTag();
                    for (net.minecraft.world.entity.item.ItemEntity itemEntity : event.getDrops()) {
                        ItemStack stack = itemEntity.getItem();
                        if (!stack.isEmpty()) {
                            Tag itemTag = stack.save(player.registryAccess());
                            listTag.add(itemTag);
                        }
                    }

                    if (!listTag.isEmpty()) {
                        player.getPersistentData().put("saved_death_items", listTag);
                        event.getDrops().clear();
                    }
                }
            }
        }
    }

    // 2.1. CONTAINER CLOSE LISTENER: Destroys death chest with smoke if empty
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        Player player = event.getEntity();
        if (player != null && !player.level().isClientSide() && player.level() instanceof ServerLevel level) {
            BlockPos playerPos = player.blockPosition();
            int radius = 8;
            for (BlockPos p : BlockPos.betweenClosed(playerPos.offset(-radius, -radius, -radius), playerPos.offset(radius, radius, radius))) {
                if (level.getBlockState(p).is(Blocks.CHEST)) {
                    BlockEntity be = level.getBlockEntity(p);
                    if (be != null && be.getPersistentData().getBoolean("is_death_chest")) {
                        if (be instanceof Container container && container.isEmpty()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, 
                                p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 
                                20, 0.25, 0.25, 0.25, 0.05);
                            level.playSound(null, p, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.0f);
                            player.getPersistentData().putBoolean("has_death_chest", false);
                        }
                    }
                }
            }
        }
    }

    // 2.2. ACTION BAR COORDINATES TRACKER FOR SUB STRATA: Displays pulsing coords to locate the death chest
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player != null && !player.level().isClientSide()) {
            CompoundTag data = player.getPersistentData();
            if (data != null && data.getBoolean("has_death_chest")) {
                String currentDim = player.level().dimension().location().toString();
                String targetDim = data.getString("death_chest_dim");
                
                if (currentDim.contains("the_sub_strata") && currentDim.equals(targetDim)) {
                    int cx = data.getInt("death_chest_x");
                    int cy = data.getInt("death_chest_y");
                    int cz = data.getInt("death_chest_z");
                    
                    // Double check if chest still exists to clean up if mined or emptied
                    BlockPos targetPos = new BlockPos(cx, cy, cz);
                    if (player.tickCount % 20 == 0 && player.level().hasChunkAt(targetPos)) {
                        if (!player.level().getBlockState(targetPos).is(Blocks.CHEST)) {
                            data.putBoolean("has_death_chest", false);
                            return;
                        }
                    }
                    
                    // Periodically resending once every 70 ticks (3.5 seconds) allows 
                    // Minecraft's native action bar to complete its fade-in, display, and fade-out sequence.
                    if (player.tickCount % 70 == 0) {
                        String message = String.format("XYZ: %d, %d, %d", cx, cy, cz);
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
                    }
                }
            }
        }
    }

    // 3. RESPAWN TRIGGER: Processes dimension shifting using the stable 1-tick delay
    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        execute(event, event.getEntity().level(), event.getEntity());
    }

    public static void execute(LevelAccessor world, Entity entity) {
        execute(null, world, entity);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
        if (entity == null)
            return;

        TheBackwoodsMod.queueServerWork(1, () -> {
            if (entity instanceof ServerPlayer player && !player.level().isClientSide()) {
                CompoundTag data = player.getPersistentData();
                if (data == null) return;

                ResourceKey<Level> grainKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("the_backwoods", "the_grain"));
                ResourceKey<Level> subStrataKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("the_backwoods", "the_sub_strata"));

                boolean wasInGrain = data.getBoolean("died_in_grain");
                boolean wasInSubstrata = data.getBoolean("died_in_substrata");

                if (player.getServer() != null) {
                    // LOOP ROUTINE A: Died in Grain -> Teleport down to Sub-Strata
                    if (wasInGrain) {
                        data.putBoolean("died_in_grain", false); // Consume tag
                        ServerLevel nextLevel = player.getServer().getLevel(subStrataKey);
                        if (nextLevel != null) {
                            double targetY = 161.0;
                            player.teleportTo(nextLevel, player.getX(), targetY, player.getZ(), Collections.emptySet(), player.getYRot(), player.getXRot());
                            createDeathChest(nextLevel, (ServerPlayer) player);
                        }
                    }
                    // LOOP ROUTINE B: Died in Sub-Strata -> Teleport back up to The Grain
                    else if (wasInSubstrata) {
                        data.putBoolean("died_in_substrata", false); // Consume tag
                        ServerLevel nextLevel = player.getServer().getLevel(grainKey);
                        if (nextLevel != null) {
                            double targetY = nextLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) player.getX(), (int) player.getZ());
                            if (targetY <= nextLevel.getMinBuildHeight()) {
                                targetY = player.getY();
                            }
                            player.teleportTo(nextLevel, player.getX(), targetY, player.getZ(), Collections.emptySet(), player.getYRot(), player.getXRot());
                            createDeathChest(nextLevel, (ServerPlayer) player);
                        }
                    }
                }
            }
        });
    }

    // 4. STORAGE CREATION: Spawns the chest(s) under the target level next to the player and populates them
    private static void createDeathChest(ServerLevel level, ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data == null || !data.contains("saved_death_items")) {
            return;
        }

        ListTag listTag = data.getList("saved_death_items", 10); // 10 is CompoundTag ID
        if (listTag.isEmpty()) {
            data.remove("saved_death_items");
            return;
        }

        java.util.List<ItemStack> itemStacks = new java.util.ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            java.util.Optional<ItemStack> parsed = ItemStack.parse(player.registryAccess(), listTag.get(i));
            if (parsed.isPresent() && !parsed.get().isEmpty()) {
                itemStacks.add(parsed.get());
            }
        }

        if (itemStacks.isEmpty()) {
            data.remove("saved_death_items");
            return;
        }

        BlockPos respawnPos = player.blockPosition();
        ResourceKey<Level> subStrataKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("the_backwoods", "the_sub_strata"));
        if (level.dimension() == subStrataKey) {
            respawnPos = new BlockPos(respawnPos.getX(), 161, respawnPos.getZ());
        }

        BlockPos chestPos1 = null;
        BlockPos chestPos2 = null;

        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(respawnPos.offset(-12, -12, -12), respawnPos.offset(12, 12, 12))) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(p);
            boolean isReplaceable = state.isAir() 
                || state.canBeReplaced() 
                || state.getBlock() instanceof StairBlock 
                || state.getBlock() instanceof SlabBlock;

            if (isReplaceable) {
                candidates.add(p.immutable());
            }
        }

        // Prioritize candidates between 3 and 10 blocks away (distSq between 9.0 and 100.0) 
        // down to closer/farther ones as fallbacks, so the player cleanly witnesses the ender particles
        final BlockPos finalRespawnPos = respawnPos;
        java.util.List<BlockPos> preferred = new java.util.ArrayList<>();
        java.util.List<BlockPos> other = new java.util.ArrayList<>();

        for (BlockPos p : candidates) {
            double distSq = Math.pow(p.getX() - finalRespawnPos.getX(), 2) + Math.pow(p.getY() - finalRespawnPos.getY(), 2) + Math.pow(p.getZ() - finalRespawnPos.getZ(), 2);
            if (distSq >= 9.0 && distSq <= 100.0) {
                preferred.add(p);
            } else {
                other.add(p);
            }
        }

        java.util.Comparator<BlockPos> distanceComparator = (p1, p2) -> {
            double d1 = Math.pow(p1.getX() - finalRespawnPos.getX(), 2) + Math.pow(p1.getY() - finalRespawnPos.getY(), 2) + Math.pow(p1.getZ() - finalRespawnPos.getZ(), 2);
            double d2 = Math.pow(p2.getX() - finalRespawnPos.getX(), 2) + Math.pow(p2.getY() - finalRespawnPos.getY(), 2) + Math.pow(p2.getZ() - finalRespawnPos.getZ(), 2);
            return Double.compare(d1, d2);
        };

        preferred.sort(distanceComparator);
        other.sort(distanceComparator);

        java.util.List<BlockPos> sortedCandidates = new java.util.ArrayList<>(preferred);
        sortedCandidates.addAll(other);

        if (!sortedCandidates.isEmpty()) {
            chestPos1 = sortedCandidates.get(0);
            if (sortedCandidates.size() > 1) {
                for (BlockPos p : sortedCandidates) {
                    if (!p.equals(chestPos1)) {
                        chestPos2 = p;
                        break;
                    }
                }
            }
        }

        if (chestPos1 == null) {
            chestPos1 = respawnPos.immutable();
        }
        if (chestPos2 == null) {
            chestPos2 = chestPos1.east();
        }

        // Place and fill first chest
        level.setBlock(chestPos1, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity be1 = level.getBlockEntity(chestPos1);
        if (be1 != null) {
            be1.getPersistentData().putBoolean("is_death_chest", true);
        }
        int itemIndex = 0;

        if (be1 instanceof Container container1) {
            int slotSize = container1.getContainerSize();
            for (int slot = 0; slot < slotSize && itemIndex < itemStacks.size(); slot++) {
                container1.setItem(slot, itemStacks.get(itemIndex));
                itemIndex++;
            }
        }

        // If items are left over (more than 27 slots), populate the second chest
        if (itemIndex < itemStacks.size()) {
            level.setBlock(chestPos2, Blocks.CHEST.defaultBlockState(), 3);
            BlockEntity be2 = level.getBlockEntity(chestPos2);
            if (be2 != null) {
                be2.getPersistentData().putBoolean("is_death_chest", true);
            }
            if (be2 instanceof Container container2) {
                int slotSize = container2.getContainerSize();
                for (int slot = 0; slot < slotSize && itemIndex < itemStacks.size(); slot++) {
                    container2.setItem(slot, itemStacks.get(itemIndex));
                    itemIndex++;
                }
            }
        }

        // Save chest coordinates to player persistent data for action bar tracking
        data.putInt("death_chest_x", chestPos1.getX());
        data.putInt("death_chest_y", chestPos1.getY());
        data.putInt("death_chest_z", chestPos1.getZ());
        data.putString("death_chest_dim", level.dimension().location().toString());
        data.putBoolean("has_death_chest", true);

        // Clean NBT data on player
        data.remove("saved_death_items");
    }
}