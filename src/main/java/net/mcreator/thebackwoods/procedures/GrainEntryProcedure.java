package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
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
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import net.mcreator.thebackwoods.TheBackwoodsMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class GrainEntryProcedure {

    private static final int INITIAL_DELAY_TICKS = 40;
    private static final int REAPPLY_INTERVAL_TICKS = 100;
    private static final int TELEPORT_ON_APPLICATION = 4;
    private static final int TELEPORT_DELAY_TICKS = 78;
    private static final double SPLINTER_DETECT_RADIUS = 32.0;
    private static final int BLINDNESS_DURATION_TICKS = 40;
    private static final int BLINDNESS_AMPLIFIER = 1;

    private static final float CAVE_SOUND_VOLUME = 2.0f;
    private static final float CAVE_SOUND_PITCH = 0.85f;

    private static final float PRE_TELEPORT_SOUND_VOLUME = 2.0f;
    private static final float PRE_TELEPORT_SOUND_PITCH = 0.8f;

    private static final ResourceKey<Level> THE_BACKWOODS = ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.parse("the_backwoods:backwoods")
    );

    private static final ResourceKey<Level> THE_GRAIN = ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.parse("the_backwoods:the_grain")
    );

    private static final Map<UUID, Boolean> thirdFrontState = new HashMap<>();
    private static final Map<UUID, Boolean> prevThirdFrontState = new HashMap<>();
    private static final Map<UUID, Integer> heldTicks = new HashMap<>();
    private static final Map<UUID, Integer> applicationCount = new HashMap<>();
    private static final Map<UUID, Boolean> teleportQueued = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null)
            return;

        UUID id = player.getUUID();
        Level level = player.level();

        if (!level.dimension().equals(THE_BACKWOODS)) {
            thirdFrontState.remove(id);
            prevThirdFrontState.remove(id);
            heldTicks.remove(id);
            applicationCount.remove(id);
            teleportQueued.remove(id);
            return;
        }

        if (level.isClientSide()) {
            boolean inThirdFront = isThirdPersonFrontForLocalPlayer(player);
            thirdFrontState.put(id, inThirdFront);
            return;
        }

        boolean inThirdFront = thirdFrontState.getOrDefault(id, false);
        boolean wasInThirdFront = prevThirdFrontState.getOrDefault(id, false);

        int currentHeld = heldTicks.getOrDefault(id, 0);
        int apps = applicationCount.getOrDefault(id, 0);

        if (inThirdFront && isChasedBySplinter(player)) {
            if (!wasInThirdFront) {
                currentHeld = 1;
            } else {
                currentHeld++;
            }

            if (currentHeld == INITIAL_DELAY_TICKS) {
                apps++;
                applyBlindnessAndCaveSound(player);
            } else if (currentHeld > INITIAL_DELAY_TICKS && (currentHeld - INITIAL_DELAY_TICKS) % REAPPLY_INTERVAL_TICKS == 0) {
                apps++;
                applyBlindnessAndCaveSound(player);
            }
        } else {
            currentHeld = 0;
            apps = 0;
        }

        heldTicks.put(id, currentHeld);
        applicationCount.put(id, apps);
        prevThirdFrontState.put(id, inThirdFront);

        if (apps >= TELEPORT_ON_APPLICATION && player instanceof ServerPlayer sp) {
            if (teleportQueued.getOrDefault(id, false))
                return;

            teleportQueued.put(id, true);

            // Select one of your chosen messages at random
            String[] messages = {
                "Curiosity is a heavy burden.",
                "We see you seeing us.",
                "Eyes belong in sockets, traveler.",
                "Your eyes are in the wrong place.",
                "The camera doesn't protect you.",
                "Last chance.",
            };
            String selectedMsg = messages[sp.getRandom().nextInt(messages.length)];
            
            // Send to action bar with spooky formatting
            sp.displayClientMessage(Component.literal(selectedMsg).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY), true);

            playPreTeleportSound(sp);

            heldTicks.put(id, 0);
            applicationCount.put(id, 0);
            prevThirdFrontState.put(id, false);

            TheBackwoodsMod.queueServerWork(TELEPORT_DELAY_TICKS, () -> {
                if (sp.isRemoved()) {
                    teleportQueued.put(id, false);
                    return;
                }

                ServerLevel grainLevel = sp.server.getLevel(THE_GRAIN);
                if (grainLevel != null) {
                    sp.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
                    sp.teleportTo(grainLevel, sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
                    sp.connection.send(new ClientboundPlayerAbilitiesPacket(sp.getAbilities()));
                    for (MobEffectInstance effect : sp.getActiveEffects()) {
                        sp.connection.send(new ClientboundUpdateMobEffectPacket(sp.getId(), effect, false));
                    }
                    sp.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
                }

                teleportQueued.put(id, false);
            });
        }
    }

    private static void applyBlindnessAndCaveSound(Player player) {
        if (player instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, BLINDNESS_AMPLIFIER, false, false));

            if (sp.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, BlockPos.containing(sp.getX(), sp.getY(), sp.getZ()),
                    BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.cave")),
                    SoundSource.HOSTILE, CAVE_SOUND_VOLUME, CAVE_SOUND_PITCH);
            }
        }
    }

    private static boolean isChasedBySplinter(Player player) {
        net.minecraft.world.phys.AABB box = net.minecraft.world.phys.AABB.ofSize(player.position(), SPLINTER_DETECT_RADIUS, SPLINTER_DETECT_RADIUS, SPLINTER_DETECT_RADIUS);
    
        java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> isChasing = splinter ->
            splinter.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
            && splinter.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue() > 0;
    
        boolean regularChasing = !player.level().getEntitiesOfClass(
            net.mcreator.thebackwoods.entity.SplinterEntity.class, box, isChasing
        ).isEmpty();
    
        boolean logChasing = !player.level().getEntitiesOfClass(
            net.mcreator.thebackwoods.entity.LogSplinterEntity.class, box, isChasing
        ).isEmpty();
    
        return regularChasing || logChasing;
    }

    private static void playPreTeleportSound(ServerPlayer sp) {
        if (sp.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, BlockPos.containing(sp.getX(), sp.getY(), sp.getZ()),
                BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_creak_paranoia")),
                SoundSource.HOSTILE, PRE_TELEPORT_SOUND_VOLUME, PRE_TELEPORT_SOUND_PITCH);
        }
    }

    private static boolean isThirdPersonFrontForLocalPlayer(Player player) {
        try {
            Object minecraft = Class.forName("net.minecraft.client.Minecraft").getMethod("getInstance").invoke(null);
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