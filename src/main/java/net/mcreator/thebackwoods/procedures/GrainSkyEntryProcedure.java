package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class GrainSkyEntryProcedure {

    private static final ResourceKey<Level> BACKWOODS_DIM =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:backwoods"));
    private static final ResourceKey<Level> GRAIN_DIM =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_grain"));

    private static final double SKY_THRESHOLD = 200.0;
    private static final double SAFE_FALL_VALUE = 1024.0;
    // 12.5 seconds total pull at 250 ticks
    private static final int LAUNCH_DURATION_TICKS = 250; 

    private static final Map<UUID, Integer> ascensionTimers = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> entryCooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> safeFallResetTimers = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide() || !(player instanceof ServerPlayer _player))
            return;

        UUID playerUUID = _player.getUUID();

        if (entryCooldowns.containsKey(playerUUID)) {
            int cd = entryCooldowns.get(playerUUID) - 1;
            if (cd <= 0) entryCooldowns.remove(playerUUID);
            else entryCooldowns.put(playerUUID, cd);
        }

        if (safeFallResetTimers.containsKey(playerUUID)) {
            int timer = safeFallResetTimers.get(playerUUID) - 1;
            if (timer <= 0 && _player.onGround()) {
                if (_player.getAttributes().hasAttribute(Attributes.SAFE_FALL_DISTANCE)) {
                    _player.getAttribute(Attributes.SAFE_FALL_DISTANCE).setBaseValue(3.0);
                }
                safeFallResetTimers.remove(playerUUID);
            } else {
                safeFallResetTimers.put(playerUUID, timer);
            }
        }

        if (_player.level().dimension().equals(BACKWOODS_DIM) && _player.getY() >= SKY_THRESHOLD 
            && !entryCooldowns.containsKey(playerUUID) && !_player.isCreative()) {
            
            int currentTicks = ascensionTimers.getOrDefault(playerUUID, 0);

            if (currentTicks == 0) {
                _player.displayClientMessage(Component.literal("The sky is only a mirror.")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA), true);
            }

            // --- SEAMLESS LOGIC ---
            // We apply the velocity EVERY tick including the tick we teleport.
            if (currentTicks < LAUNCH_DURATION_TICKS) {
                _player.setDeltaMovement(0, 0.45, 0);
                _player.hurtMarked = true;
                _player.connection.send(new ClientboundSetEntityMotionPacket(_player));
                
                // Increment timer
                ascensionTimers.put(playerUUID, currentTicks + 1);

                // TELEPORT CHECK: Trigger the teleport while the motion is active
                // (Using currentTicks + 1 == LAUNCH_DURATION_TICKS ensures it happens during the pull)
                if (currentTicks + 1 >= LAUNCH_DURATION_TICKS) {
                    executeSeamlessTeleport(_player, playerUUID);
                }
            }
        } else if (ascensionTimers.containsKey(playerUUID)) {
            ascensionTimers.remove(playerUUID);
        }
    }

    private static void executeSeamlessTeleport(ServerPlayer _player, UUID playerUUID) {
        ServerLevel grainLevel = _player.server.getLevel(GRAIN_DIM);
        if (grainLevel != null) {
            if (_player.getAttributes().hasAttribute(Attributes.SAFE_FALL_DISTANCE)) {
                _player.getAttribute(Attributes.SAFE_FALL_DISTANCE).setBaseValue(SAFE_FALL_VALUE);
            }
            
            // Perform the swap while upward momentum is technically still registered
            _player.teleportTo(grainLevel, _player.getX(), 250.0, _player.getZ(), _player.getYRot(), _player.getXRot());
            
            // Finalize State
            ascensionTimers.remove(playerUUID);
            entryCooldowns.put(playerUUID, 400); 
            safeFallResetTimers.put(playerUUID, 800); 

            // Sync
            _player.connection.send(new ClientboundPlayerAbilitiesPacket(_player.getAbilities()));
            for (MobEffectInstance effect : _player.getActiveEffects())
                _player.connection.send(new ClientboundUpdateMobEffectPacket(_player.getId(), effect, false));
            _player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
        }
    }
}