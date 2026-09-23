package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.network.TheBackwoodsModVariables;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class FamiliarEntityLookProcedure {

    private static final double LOOK_RANGE = 32.0;
    private static final double STARE_RANGE = 24.0;
    private static final double STARE_DOT_THRESHOLD = 0.97;
    private static final int STARE_TICKS_MIN = 60;
    private static final int STARE_TICKS_MAX = 100;
    private static final int SOUND_COOLDOWN = 200;
    private static final double DESPAWN_CHANCE = 0.15;

    private static final Map<UUID, Integer> stareTimers = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> soundCooldowns = new ConcurrentHashMap<>();

    private static final ResourceKey<Level> FAMILIAR_DIM =
        ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_familiar"));

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide())
            return;

        if (!player.level().dimension().equals(FAMILIAR_DIM))
            return;

        UUID playerUUID = player.getUUID();
        TheBackwoodsModVariables.PlayerVariables vars = player.getData(TheBackwoodsModVariables.PLAYER_VARIABLES);

        // --- IMPROVED DAY TRACKING ---
        long time = player.level().getDayTime() % 24000;
        
        // Between 0 and 1000 ticks is early morning
        if (time < 1000 && !vars.dayIncrementedToday) {
            vars.daysInFamiliar += 1;
            vars.dayIncrementedToday = true;
            vars.markSyncDirty();
        } else if (time > 1000 && vars.dayIncrementedToday) {
            // Reset the "Check" after morning passes so it can trigger tomorrow
            vars.dayIncrementedToday = false;
            vars.markSyncDirty();
        }

        // Difficulty scaling: Day 1: 0%, Day 2: 33%, Day 3: 66%, Day 4+: 100%
        double stareChance = Math.min(1.0, Math.max(0, (vars.daysInFamiliar - 1) * 0.33));

		// --- THE LOOKING LOGIC ---
        Vec3 playerPos = player.position();
        player.level().getEntitiesOfClass(Mob.class,
                new AABB(playerPos, playerPos).inflate(LOOK_RANGE),
                mob -> true) // We already know they are Mobs, no need to check for Player
            .forEach(mob -> {
                // Use Mob UUID to keep behavior consistent for that specific entity
                double entitySeed = Math.abs(mob.getUUID().getLeastSignificantBits() % 100) / 100.0;
                
                if (entitySeed < stareChance) {
                    mob.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                        new Vec3(player.getX(), player.getEyeY(), player.getZ()));
                }
            });

        // --- STARE DETECTION SOUNDS ---
        int cooldown = soundCooldowns.getOrDefault(playerUUID, 0);
        if (cooldown > 0) soundCooldowns.put(playerUUID, cooldown - 1);

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle().normalize();

        boolean staringAtMob = player.level().getEntitiesOfClass(Mob.class,
                new AABB(playerPos, playerPos).inflate(STARE_RANGE))
            .stream()
            .anyMatch(mob -> {
                Vec3 toEntity = mob.getEyePosition().subtract(eyePos).normalize();
                return lookDir.dot(toEntity) > STARE_DOT_THRESHOLD;
            });

        if (staringAtMob) {
            int stareTime = stareTimers.getOrDefault(playerUUID, 0) + 1;
            stareTimers.put(playerUUID, stareTime);

            if (stareTime >= (STARE_TICKS_MIN + player.getRandom().nextInt(40)) && cooldown <= 0) {
                player.level().playSound(null, player.blockPosition(), 
                    BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("minecraft:ambient.cave")), 
                    SoundSource.AMBIENT, 1.0f, 0.8f);
                stareTimers.put(playerUUID, 0);
                soundCooldowns.put(playerUUID, SOUND_COOLDOWN);
            }
        } else {
            stareTimers.put(playerUUID, 0);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide() || !event.getEntity().level().dimension().equals(FAMILIAR_DIM))
            return;

        if (Math.random() < DESPAWN_CHANCE) {
            Entity target = event.getTarget();
            if (target.level() instanceof ServerLevel _level) {
                _level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, target.getX(), target.getY() + 1, target.getZ(), 50, 0.2, 0.2, 0.2, 0.1);
            }
            target.discard();
            event.setCanceled(true);
        }
    }
}