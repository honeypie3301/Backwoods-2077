package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.TheBackwoodsMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class GrainExitThirdPersonProcedure {

    private static final int SOUND_1_TICK = 150;
    private static final int SOUND_2_TICK = 250;
    private static final int SOUND_3_TICK = 350;
    private static final int TELEPORT_TICK = 250;
    private static final int TELEPORT_DELAY_TICKS = 195;

    private static final ResourceKey<Level> THE_GRAIN = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_grain"));
    private static final ResourceKey<Level> THE_BACKWOODS = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.parse("the_backwoods:backwoods"));

    private static final Map<UUID, Boolean> thirdFrontState = new HashMap<>();
    private static final Map<UUID, Boolean> prevThirdFrontState = new HashMap<>();
    private static final Map<UUID, Integer> heldTicks = new HashMap<>();
    private static final Map<UUID, Boolean> teleportQueued = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null)
            return;

        UUID id = player.getUUID();
        Level level = player.level();

        if (!level.dimension().equals(THE_GRAIN)) {
            thirdFrontState.remove(id);
            prevThirdFrontState.remove(id);
            heldTicks.remove(id);
            teleportQueued.remove(id);
            return;
        }

        if (level.isClientSide()) {
            thirdFrontState.put(id, isThirdPersonFrontForLocalPlayer(player));
            return;
        }

        boolean inThirdFront = thirdFrontState.getOrDefault(id, false);
        int currentHeld = heldTicks.getOrDefault(id, 0);

        if (!inThirdFront) {
            // Reset if player exits third person front
            heldTicks.put(id, 0);
            prevThirdFrontState.put(id, false);
            return;
        }

        currentHeld++;
        heldTicks.put(id, currentHeld);
        prevThirdFrontState.put(id, true);

        if (!(player instanceof ServerPlayer sp))
            return;

        // Play sounds at specific tick milestones
        if (currentHeld == SOUND_1_TICK) {
            playSound(sp, 1.0f); //volume
        } else if (currentHeld == SOUND_2_TICK) {
            playSound(sp, 3.0f);
        } else if (currentHeld == SOUND_3_TICK) {
            playSound(sp, 5.0f);
        }

        // Trigger teleport
        if (currentHeld >= TELEPORT_TICK && !teleportQueued.getOrDefault(id, false)) {
            teleportQueued.put(id, true);
            heldTicks.put(id, 0);

            TheBackwoodsMod.queueServerWork(TELEPORT_DELAY_TICKS, () -> {
                if (sp.isRemoved()) {
                    teleportQueued.put(id, false);
                    return;
                }

                ServerLevel backwoodsLevel = sp.server.getLevel(THE_BACKWOODS);
                if (backwoodsLevel != null) {
                    sp.teleportTo(backwoodsLevel, sp.getX(), sp.getY(), sp.getZ(),
                            sp.getYRot(), sp.getXRot());
                    sp.connection.send(new ClientboundPlayerAbilitiesPacket(sp.getAbilities()));
                    for (MobEffectInstance effect : sp.getActiveEffects())
                        sp.connection.send(new ClientboundUpdateMobEffectPacket(sp.getId(), effect, false));
                    sp.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
                }

                teleportQueued.put(id, false);
            });
        }
    }

    private static void playSound(ServerPlayer sp, float volume) {
        if (sp.level() instanceof ServerLevel serverLevel) {
            var sound = BuiltInRegistries.SOUND_EVENT.get(
                    ResourceLocation.parse("the_backwoods:wood_crack_7"));
            if (sound != null) {
                serverLevel.playSound(null,
                        BlockPos.containing(sp.getX(), sp.getY(), sp.getZ()),
                        sound, SoundSource.HOSTILE, volume, 1.0f);
            }
        }
    }

    private static boolean isThirdPersonFrontForLocalPlayer(Player player) {
        try {
            Object minecraft = Class.forName("net.minecraft.client.Minecraft")
                    .getMethod("getInstance").invoke(null);
            Object localPlayer = minecraft.getClass().getField("player").get(minecraft);
            if (localPlayer == null || localPlayer != player)
                return false;
            Object options = minecraft.getClass().getField("options").get(minecraft);
            Object cameraType = options.getClass().getMethod("getCameraType").invoke(options);
            boolean isMirrored = (boolean) cameraType.getClass().getMethod("isMirrored").invoke(cameraType);
            boolean isFirstPerson = (boolean) cameraType.getClass().getMethod("isFirstPerson").invoke(cameraType);
            return isMirrored && !isFirstPerson;
        } catch (Throwable ignored) {
            return false;
        }
    }
}