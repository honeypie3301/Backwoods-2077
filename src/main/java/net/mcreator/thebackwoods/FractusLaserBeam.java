/*
 * The code of this mod element is always locked.
 *
 * You can register new events in this class too.
 *
 * If you want to make a plain independent class, create it using
 * Project Browser -> New... and make sure to make the class
 * outside net.mcreator.thebackwoods as this package is managed by MCreator.
 *
 * If you change workspace package, modid or prefix, you will need
 * to manually adapt this file to these changes or remake it.
 *
 * This class will be added in the mod root package.
*/
package net.mcreator.thebackwoods;

import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import io.netty.buffer.ByteBuf;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class FractusLaserBeam {

    // Configurable offsets to align the laser beam with the entity's model center:
    // OFFSET_X: Positive moves right, negative moves left (relative to entity's head facing yaw)
    // OFFSET_Y: Positive moves up, negative moves down
    // OFFSET_Z: Positive moves forward, negative moves backward
    public static double OFFSET_X = 0.0;
    public static double OFFSET_Y = 0.0;
    public static double OFFSET_Z = 0.0;

    // Configurable laser beam sizes (thickness) for Standard Fractus and Fractus Prime
    // Standard Fractus dimensions
    public static float FRACTUS_LASER_CORE_SCALE = 0.06f;
    public static float FRACTUS_LASER_AURA_SCALE = 0.18f;
    public static float FRACTUS_BURST_CORE_SCALE = 0.28f;
    public static float FRACTUS_BURST_MID_SCALE = 0.60f;
    public static float FRACTUS_BURST_OUTER_SCALE = 1.05f;
    public static float BURST_HELIX_RADIUS_FRACTUS = 0.75f;

    // Prime laser dimensions (enlarged burst per user feedback)
    public static float PRIME_LASER_CORE_SCALE = 0.11f;
    public static float PRIME_LASER_CORONA_SCALE = 0.28f;
    public static float PRIME_BURST_CORE_SCALE = 0.45f;
    public static float PRIME_BURST_MID_SCALE = 0.95f;
    public static float PRIME_BURST_OUTER_SCALE = 1.65f;
    public static float BURST_HELIX_RADIUS_PRIME = 1.10f;

    // Configurable size scale factor for lasers when shaders are active (0.55f = 45% reduction)
    public static float SHADER_LASER_SIZE_FACTOR = 0.55f;

    // Set to true to bring back the original particle-based laser beam instead of the custom beacon renderer
    public static boolean USE_OLD_LASER_PARTICLES = false;

    // Set to true to disable custom impact particles and use old particle impact behavior
    public static boolean USE_OLD_IMPACT_PARTICLES = false;

    // Toggle for laser burst impact particles (sparks, smoke, thermal effects)
    public static boolean USE_BURST_IMPACT_PARTICLES = true;

    // Toggle for laser burst charging particles (particles converging towards eye while charging)
    public static boolean USE_BURST_CHARGING_PARTICLES = true;

    // Set to true to use vanilla beacon_beam.png, or false to use custom texture for smooth cinematic visuals
    public static boolean USE_BEACON_BEAM_TEXTURE = false;

    // Configurable sound volumes for the Fractus entities and behaviors
    public static float FRACTUS_LASER_VOLUME = 1.75f;
    public static float FRACTUS_BURST_VOLUME = 4.0f;
    public static float FRACTUS_ANGER_VOLUME = 0.7f;
    public static float PRIME_LASER_VOLUME = 1.95f;
    public static float PRIME_BURST_VOLUME = 3.5f;
    public static float PRIME_ANGER_VOLUME = 0.7f;
    public static float PRIME_SPHERE_BURST_VOLUME = 3.0f;

    public FractusLaserBeam() {
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
    }

    @EventBusSubscriber(value = Dist.CLIENT)
    public static class FractusLaserClientRenderer {

        private static final double LASER_RANGE = 32.0;
        private static final double ANGRY_LASER_RANGE = 44.0;
        private static final double BURST_LASER_RANGE = 256.0;

        private static final ResourceLocation BEACON_BEAM_LOCATION = ResourceLocation.parse("minecraft:textures/entity/beacon_beam.png");
        private static final ResourceLocation CUSTOM_LASER_LOCATION = ResourceLocation.parse("minecraft:textures/misc/white.png");

        private static Boolean shadersActiveCached = null;
        private static long lastShaderCheckTime = 0;

        public static boolean isShaderActive() {
            long now = System.currentTimeMillis();
            if (shadersActiveCached != null && (now - lastShaderCheckTime < 1000)) {
                return shadersActiveCached;
            }
            boolean active = false;
            try {
                Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance = irisApi.getMethod("getInstance").invoke(null);
                if (instance != null) {
                    Object res = irisApi.getMethod("isShaderPackInUse").invoke(instance);
                    if (res instanceof Boolean b && b) {
                        active = true;
                    }
                }
            } catch (Throwable ignored) {}
            if (!active) {
                try {
                    Class<?> optifineConfig = Class.forName("net.optifine.Config");
                    Object res = optifineConfig.getMethod("isShaders").invoke(null);
                    if (res instanceof Boolean b && b) {
                        active = true;
                    }
                } catch (Throwable ignored) {}
            }
            if (!active) {
                try {
                    Class<?> optifineConfig2 = Class.forName("Config");
                    Object res = optifineConfig2.getMethod("isShaders").invoke(null);
                    if (res instanceof Boolean b && b) {
                        active = true;
                    }
                } catch (Throwable ignored) {}
            }
            shadersActiveCached = active;
            lastShaderCheckTime = now;
            return active;
        }

        private static RenderType getGlowRenderType(ResourceLocation texture) {
            return isShaderActive() ? RenderType.entityTranslucent(texture) : RenderType.beaconBeam(texture, true);
        }

        private static MultiBufferSource compatibleBeamBufferSource(MultiBufferSource delegate, ResourceLocation texture) {
            if (isShaderActive()) {
                return ignoredRenderType -> delegate.getBuffer(RenderType.entityTranslucent(texture));
            }
            return delegate;
        }

        private static final java.util.Map<Integer, Float> SUMMON_FADE_TIMERS = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<Integer, Long> LASER_START_TIMES = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<Integer, Long> BURST_START_TIMES = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<Integer, Vec3> BURST_LAST_AIM_VECTORS = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<Integer, Long> BURST_LAST_ACTIVE_TIMES = new java.util.concurrent.ConcurrentHashMap<>();

        public static final int IMPACT_BURST = 0;
        public static final int IMPACT_DESTROYING = 1;
        public static final int IMPACT_REGULAR_BASE = 2;
        public static final int IMPACT_TELEKINESIS = 7;

        private static final java.util.Map<Long, ImpactState> SYNCED_LASER_IMPACT_STATES = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<Integer, Integer> SYNCED_REGULAR_IMPACT_COUNTS = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<Integer, Integer> SYNCED_BURST_PHASES = new java.util.concurrent.ConcurrentHashMap<>();

        private static class ImpactState {
            Vec3 prevImpact;
            Vec3 currentImpact;
            Vec3 displayedImpact;
            long lastUpdateGameTime;
            long lastRenderNanos;

            ImpactState(Vec3 prevImpact, Vec3 currentImpact, long lastUpdateGameTime) {
                this.prevImpact = prevImpact;
                this.currentImpact = currentImpact;
                this.displayedImpact = currentImpact;
                this.lastUpdateGameTime = lastUpdateGameTime;
                this.lastRenderNanos = 0L;
            }
        }

        private static long impactKey(int entityId, int channel) {
            return ((long) entityId << 32) ^ (channel & 0xffffffffL);
        }

        public static void receiveLaserImpact(LaserImpactPayload payload) {
            int channel = payload.route() & 0xff;
            int regularCount = (payload.route() >>> 8) & 0xff;
            Vec3 receivedImpact = new Vec3(payload.x(), payload.y(), payload.z());
            long key = impactKey(payload.entityId(), channel);
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            long updateTime = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
            ImpactState state = SYNCED_LASER_IMPACT_STATES.get(key);
            if (state == null) {
                SYNCED_LASER_IMPACT_STATES.put(key, new ImpactState(receivedImpact, receivedImpact, updateTime));
            } else {
                state.prevImpact = state.currentImpact;
                state.currentImpact = receivedImpact;
                state.lastUpdateGameTime = updateTime;
            }

            if (channel == IMPACT_BURST) {
                SYNCED_BURST_PHASES.put(payload.entityId(), regularCount);
            } else if (channel >= IMPACT_REGULAR_BASE && channel < IMPACT_TELEKINESIS) {
                SYNCED_REGULAR_IMPACT_COUNTS.put(payload.entityId(), regularCount);
                for (int i = regularCount; i < 5; i++) {
                    SYNCED_LASER_IMPACT_STATES.remove(impactKey(payload.entityId(), IMPACT_REGULAR_BASE + i));
                }
            }
        }

        private static Vec3 syncedImpactPosition(LivingEntity entity, int channel, Vec3 startPos, float partialTicks) {
            ImpactState state = SYNCED_LASER_IMPACT_STATES.get(impactKey(entity.getId(), channel));
            if (state == null) return null;

            Vec3 interpolated = state.prevImpact.scale(1.0 - partialTicks)
                .add(state.currentImpact.scale(partialTicks));

            Vec3 resolvedImpact = interpolated;
            if (channel >= IMPACT_REGULAR_BASE && channel < IMPACT_TELEKINESIS) {
                long now = System.nanoTime();
                if (state.lastRenderNanos == 0L) {
                    state.displayedImpact = interpolated;
                } else {
                    double deltaSeconds = Mth.clamp((now - state.lastRenderNanos) / 1_000_000_000.0, 0.0, 0.1);
                    double smoothing = 1.0 - Math.exp(-24.0 * deltaSeconds);
                    state.displayedImpact = state.displayedImpact.scale(1.0 - smoothing).add(interpolated.scale(smoothing));
                }
                state.lastRenderNanos = now;
                resolvedImpact = state.displayedImpact;
            }

            return resolvedImpact.distanceToSqr(startPos) > 0.001 ? resolvedImpact : null;
        }

        private static void clearRegularImpacts(int entityId) {
            SYNCED_REGULAR_IMPACT_COUNTS.remove(entityId);
            for (int i = 0; i < 5; i++) {
                SYNCED_LASER_IMPACT_STATES.remove(impactKey(entityId, IMPACT_REGULAR_BASE + i));
            }
        }

        private static class AimState {
            Vec3 prevAim;
            Vec3 currentAim;
            int lastTickCount;

            AimState(Vec3 prevAim, Vec3 currentAim, int lastTickCount) {
                this.prevAim = prevAim;
                this.currentAim = currentAim;
                this.lastTickCount = lastTickCount;
            }
        }

        private static final java.util.Map<String, AimState> AIM_STATES = new java.util.concurrent.ConcurrentHashMap<>();

        private static final java.lang.reflect.Field NO_CULLING_FIELD;
        static {
            java.lang.reflect.Field f = null;
            try {
                for (java.lang.reflect.Field field : Entity.class.getDeclaredFields()) {
                    if (field.getType() == boolean.class && (field.getName().equals("noCulling") || field.getName().equals("f_19794_"))) {
                        field.setAccessible(true);
                        f = field;
                        break;
                    }
                }
            } catch (Throwable ignored) {}
            NO_CULLING_FIELD = f;
        }

        public static boolean isBurstLaserActive(Entity entity) {
            if (entity == null) return false;
            boolean isPrime = "fractus_prime".equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
            boolean isTelekinesis = isPrime && (
                entity.isShiftKeyDown()
                || getSyncedDataBoolean(entity, "is_telekinesis", false)
                || entity.getPersistentData().getBoolean("is_telekinesis")
                || entity.getPersistentData().getBoolean("telekinesis_active")
                || entity.getPersistentData().getInt("fractus_telekinesis_timer") > 0
                || (entity instanceof LivingEntity living && living.getStingerCount() > 0)
            );
            if (isTelekinesis) return false;

            boolean isBurstChargingFlag = getSyncedDataBoolean(entity, "is_laser_burst_activating", false)
                || getSyncedDataBoolean(entity, "is_laser_burst_activate", false)
                || getSyncedDataBoolean(entity, "is_burst_laser_activating", false)
                || getSyncedDataBoolean(entity, "is_burst_activating", false)
                || getSyncedDataBoolean(entity, "is_burst_charging", false);

            boolean isBurstDeactivating = getSyncedDataBoolean(entity, "is_laser_burst_deactivating", false)
                || getSyncedDataBoolean(entity, "is_laser_burst_deactivate", false)
                || getSyncedDataBoolean(entity, "is_burst_laser_deactivating", false)
                || getSyncedDataBoolean(entity, "is_burst_deactivating", false);

            int burstState = getSyncedDataInt(entity, "laser_state", entity.getPersistentData().getInt("fractus_laser_state"));
            if (burstState == 0) {
                burstState = getSyncedDataInt(entity, "burst_state", entity.getPersistentData().getInt("fractus_burst_state"));
            }

            boolean anyBurstActive = isBurstChargingFlag
                || getSyncedDataBoolean(entity, "is_laser_burst", false)
                || getSyncedDataBoolean(entity, "is_burst_charging", false)
                || getSyncedDataBoolean(entity, "is_burst_firing", false)
                || burstState == 1 || burstState == 2 || burstState == 3
                || entity.getPersistentData().getInt("fractus_burst_timer") > 0;

            return anyBurstActive && !isBurstDeactivating;
        }

        public static void applyFarRender(Entity entity) {
            if (entity == null) return;
            boolean burstActive = isBurstLaserActive(entity);
            try {
                entity.setViewScale(burstActive ? 16.0F : 1.0F);
            } catch (Throwable ignored) {}
            if (NO_CULLING_FIELD != null) {
                try {
                    NO_CULLING_FIELD.setBoolean(entity, burstActive);
                } catch (Throwable ignored) {}
            }
        }

        private static boolean isUntargetable(Entity entity) {
            if (entity == null || !entity.isAlive()) return true;
            if (entity.isSpectator()) return true;
            if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) return true;
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (key != null) {
                String path = key.getPath().toLowerCase();
                String full = key.toString().toLowerCase();
                if (path.contains("rot") || full.contains("rot") || path.contains("fractus") || full.contains("fractus") || path.contains("verdant") || full.contains("verdant")) return true;
            }
            String className = entity.getClass().getSimpleName().toLowerCase();
            if (className.contains("rot") || className.contains("fractus") || className.contains("verdant")) return true;
            return false;
        }

        @SubscribeEvent
        public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
            if (USE_OLD_LASER_PARTICLES) {
                return;
            }
            LivingEntity entity = event.getEntity();
            if (entity == null || !isFractus(entity)) {
                return;
            }

            // Disable frustum culling and expand render bounds so laser beams stay visible even near screen edges / outside center FOV
            applyFarRender(entity);

            boolean isPrime = "fractus_prime".equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());

            // 1. Check Summoning State (Standard Fractus Ritual) with smooth fade-out tracking
            boolean isSummoningRaw = !isPrime && (
                (entity.isShiftKeyDown() && entity.isSprinting())
                || entity.isSwimming()
                || getSyncedDataBoolean(entity, "is_summoning", false)
                || entity.getPersistentData().getInt("fractus_summon_role") > 0
                || entity.getPersistentData().getInt("fractus_summon_ticks") > 0
            );

            float summonFadeAlpha = 1.0f;
            if (isSummoningRaw) {
                SUMMON_FADE_TIMERS.put(entity.getId(), 1.0f);
            } else {
                Float val = SUMMON_FADE_TIMERS.get(entity.getId());
                if (val != null && val > 0.01f) {
                    float newVal = val - 0.2f; // Fade out over ~5 frames (0.25s)
                    if (newVal <= 0.01f) {
                        SUMMON_FADE_TIMERS.remove(entity.getId());
                        summonFadeAlpha = 0.0f;
                    } else {
                        SUMMON_FADE_TIMERS.put(entity.getId(), newVal);
                        summonFadeAlpha = newVal;
                    }
                } else {
                    summonFadeAlpha = 0.0f;
                }
            }
            boolean isSummoning = isSummoningRaw || (summonFadeAlpha > 0.001f);

            // 2. Check Burst Laser State
            // 2. Check Prime Telekinesis State (Priority 2 - Pure White Tractor Beam)
            boolean isTelekinesis = isPrime && (
                getSyncedDataBoolean(entity, "is_telekinesis", false)
                || getSyncedDataBoolean(entity, "is_telekinesis_active", false)
                || getSyncedDataBoolean(entity, "telekinesis_active", false)
                || entity.getPersistentData().getBoolean("is_telekinesis")
                || entity.getPersistentData().getBoolean("telekinesis_active")
                || entity.getPersistentData().getInt("fractus_telekinesis_timer") > 0
                || (entity instanceof LivingEntity living && living.getStingerCount() > 0)
            );

            // 3. Check Burst Laser State
            boolean isBurstChargingFlag = getSyncedDataBoolean(entity, "is_laser_burst_activating", false)
                || getSyncedDataBoolean(entity, "is_laser_burst_activate", false)
                || getSyncedDataBoolean(entity, "is_burst_laser_activating", false)
                || getSyncedDataBoolean(entity, "is_burst_activating", false)
                || getSyncedDataBoolean(entity, "is_burst_charging", false);

            boolean isBurstDeactivating = getSyncedDataBoolean(entity, "is_laser_burst_deactivating", false)
                || getSyncedDataBoolean(entity, "is_laser_burst_deactivate", false)
                || getSyncedDataBoolean(entity, "is_burst_laser_deactivating", false)
                || getSyncedDataBoolean(entity, "is_burst_deactivating", false);

            int burstState = getSyncedDataInt(entity, "laser_state", entity.getPersistentData().getInt("fractus_laser_state"));
            if (burstState == 0) {
                burstState = getSyncedDataInt(entity, "burst_state", entity.getPersistentData().getInt("fractus_burst_state"));
            }

            boolean anyBurstActive = isBurstChargingFlag
                || getSyncedDataBoolean(entity, "is_laser_burst", false)
                || getSyncedDataBoolean(entity, "is_burst_charging", false)
                || getSyncedDataBoolean(entity, "is_burst_firing", false)
                || burstState == 1 || burstState == 2 || burstState == 3
                || entity.getPersistentData().getInt("fractus_burst_timer") > 0;

            long gameTime = entity.level().getGameTime();
            float partialTicks = event.getPartialTick();
            int burstChargeDuration = isPrime ? 125 : 50;

            boolean isBurstActive = !isTelekinesis && anyBurstActive && !isBurstDeactivating;

            if (isBurstActive) {
                Long existingStart = BURST_START_TIMES.get(entity.getId());
                if (existingStart == null || (gameTime - existingStart > 650)) {
                    BURST_START_TIMES.put(entity.getId(), gameTime);
                }
                BURST_LAST_ACTIVE_TIMES.put(entity.getId(), gameTime);
                Vec3 currentAim = getActiveAimVector(entity, partialTicks);
                if (currentAim != null && currentAim.lengthSqr() > 0.001) {
                    BURST_LAST_AIM_VECTORS.put(entity.getId(), currentAim);
                }
            }

            long lastBurstActiveTime = BURST_LAST_ACTIVE_TIMES.getOrDefault(entity.getId(), 0L);
            long ticksSinceBurst = (lastBurstActiveTime > 0) ? (gameTime - lastBurstActiveTime) : 999L;
            // 0.25 seconds = 5 ticks post-burst fadeout
            boolean isBurstPostFading = !isBurstActive && !isTelekinesis && ticksSinceBurst >= 0 && ticksSinceBurst <= 5;
            float postFadeAlpha = isBurstPostFading ? Mth.clamp(1.0f - (((float) ticksSinceBurst + partialTicks) / 5.0f), 0.0f, 1.0f) : 1.0f;

            if (!isBurstActive && !isBurstPostFading) {
                BURST_START_TIMES.remove(entity.getId());
                BURST_LAST_AIM_VECTORS.remove(entity.getId());
                BURST_LAST_ACTIVE_TIMES.remove(entity.getId());
                SYNCED_LASER_IMPACT_STATES.remove(impactKey(entity.getId(), IMPACT_BURST));
                SYNCED_BURST_PHASES.remove(entity.getId());
            }

            long burstStartTime = BURST_START_TIMES.getOrDefault(entity.getId(), 0L);
            long burstElapsed = burstStartTime > 0 ? (gameTime - burstStartTime) : 999;

            boolean isBurst = isBurstActive || isBurstPostFading;

            // 4. Check Destroying Fire Laser State
            boolean isDestroying = !isTelekinesis && !isBurst && entity.getPersistentData().getInt("fractus_destroying_fire") > 0;

            // 5. Check Regular Laser Attack States
            boolean isLaserActivating = getSyncedDataBoolean(entity, "is_laser_activating", false);
            boolean isLaserDeactivating = getSyncedDataBoolean(entity, "is_laser_deactivating", false);
            int laserState = getSyncedDataInt(entity, "laser_state", entity.getPersistentData().getInt("fractus_laser_state"));

            // Regular laser disabled if in cooldown (state 4), deactivating, or other special attack active
            boolean isLaserDisabled = isLaserDeactivating || laserState == 4 || isBurst || isSummoning || isTelekinesis || isDestroying;

            // Track activation start time for charge duration (first 28 ticks / 1.4 seconds is charging)
            long regStartTime = 0;
            if (isLaserActivating && !isLaserDisabled) {
                LASER_START_TIMES.putIfAbsent(entity.getId(), entity.level().getGameTime());
                regStartTime = LASER_START_TIMES.get(entity.getId());
            }
            long regElapsed = regStartTime > 0 ? (entity.level().getGameTime() - regStartTime) : 999;

            boolean charging = !isLaserDisabled && isLaserActivating && (
                laserState == 1 || laserState == 2 || getSyncedDataBoolean(entity, "is_laser_charging", false) || regElapsed < 28
            ) && laserState != 3 && !getSyncedDataBoolean(entity, "is_laser_firing", false);

            boolean firing = !isLaserDisabled && isLaserActivating && !charging;

            if (!isSummoning && !isBurst && !isTelekinesis && !isDestroying && !firing && !charging) {
                LASER_START_TIMES.remove(entity.getId());
                clearRegularImpacts(entity.getId());
                SYNCED_LASER_IMPACT_STATES.remove(impactKey(entity.getId(), IMPACT_DESTROYING));
                return;
            }

            ResourceLocation activeTexture = USE_BEACON_BEAM_TEXTURE ? BEACON_BEAM_LOCATION : CUSTOM_LASER_LOCATION;
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource bufferSource = event.getMultiBufferSource();
            Level level = entity.level();
            gameTime = level.getGameTime();

            // All lasers smoothly fade in over 0.15s (3 ticks)
            boolean anyLaserActive = isBurst || isTelekinesis || isDestroying || firing || charging;
            float laserFadeIn = 1.0f;
            if (anyLaserActive) {
                LASER_START_TIMES.putIfAbsent(entity.getId(), gameTime);
                long startTime = LASER_START_TIMES.get(entity.getId());
                float elapsedTicks = (float) (gameTime - startTime) + partialTicks;
                laserFadeIn = Mth.clamp(elapsedTicks / 3.0f, 0.0f, 1.0f);
            } else {
                LASER_START_TIMES.remove(entity.getId());
            }

            double entityX = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double entityY = Mth.lerp(partialTicks, entity.yo, entity.getY());
            double entityZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());

            Vec3 startPos = laserStartSmooth(entity, partialTicks);
            boolean angry = isAngry(entity);

            // MODE 1: SUMMONING RITUAL (Strict Priority 1)
            if (isSummoning) {
                Vec3 summonCenter = getSummoningCenter(level, entity, partialTicks);
                renderSummonLaserBeam(poseStack, bufferSource, activeTexture, entity, startPos, summonCenter, entityX, entityY, entityZ, gameTime, partialTicks, summonFadeAlpha);
                renderFractusSummonSphere(poseStack, bufferSource, activeTexture, entity, summonCenter, entityX, entityY, entityZ, partialTicks, summonFadeAlpha);
                return;
            }

            // MODE 2: PRIME TELEKINESIS BEAM (Strict Priority 2 - Pure White Tractor Beam)
            if (isTelekinesis) {
                Vec3 targetAim = getActiveAimVector(entity, partialTicks);
                Vec3 endPos = syncedImpactPosition(entity, IMPACT_TELEKINESIS, startPos, partialTicks);
                if (endPos == null) endPos = getTelekinesisEndPos(level, entity, startPos, targetAim, ANGRY_LASER_RANGE, partialTicks);
                renderTelekinesisBeam(poseStack, bufferSource, activeTexture, startPos, endPos, entityX, entityY, entityZ, gameTime, partialTicks, laserFadeIn);
                return;
            }

            // MODE 3: BURST LASER (Strict Priority 3)
            if (isBurst) {
                int threshold = isPrime ? 485 : 23;
                int burstTicks = entity.getPersistentData().getInt("fractus_burst_timer");
                if (burstTicks == 0) {
                    burstTicks = getSyncedDataInt(entity, "burst_timer", 0);
                }

                int syncedBurstPhase = SYNCED_BURST_PHASES.getOrDefault(entity.getId(), 0);
                boolean isBurstFiringFlag = getSyncedDataBoolean(entity, "is_burst_firing", false) || burstState == 3 || syncedBurstPhase == 3 || syncedBurstPhase == 4;
                boolean isBurstChargingFlagDirect = getSyncedDataBoolean(entity, "is_burst_charging", false) || burstState == 1 || burstState == 2 || syncedBurstPhase == 1 || syncedBurstPhase == 2;

                // Charging phase: strictly prioritized based on server-synced flags, with elapsed time as a loose fallback
                boolean isBurstCharging;
                if (isBurstFiringFlag) {
                    isBurstCharging = false;
                } else if (isBurstChargingFlagDirect) {
                    isBurstCharging = true;
                } else {
                    isBurstCharging = !isBurstPostFading && (burstElapsed < burstChargeDuration);
                }

                Vec3 endPos = getBurstImpactPosition(level, entity, startPos, partialTicks, isPrime);

                if (isBurstCharging) {
                    // Only render the charging preview/sphere at the eye, NEVER the main beam or helices
                    renderSingleLaserBeam(poseStack, bufferSource, activeTexture, entity, startPos, endPos, entityX, entityY, entityZ,
                        isPrime, true, false, true, true, gameTime, partialTicks, laserFadeIn, 1.0f, false);
                    return;
                } else {
                    // Firing and dissipation phases: render the full thick burst beam with corona, core, and 3D helices
                    renderSingleLaserBeam(poseStack, bufferSource, activeTexture, entity, startPos, endPos, entityX, entityY, entityZ,
                        isPrime, true, true, false, true, gameTime, partialTicks, laserFadeIn, postFadeAlpha, isBurstPostFading);
                    return;
                }
            }

            // MODE 4: DESTROYING FIRE (Strict Priority 4)
            if (isDestroying) {
                Vec3 targetAim = getActiveAimVector(entity, partialTicks);
                Vec3 endPos = syncedImpactPosition(entity, IMPACT_DESTROYING, startPos, partialTicks);
                if (endPos == null) endPos = getLaserEnd(level, entity, startPos, targetAim, 52.0);
                renderSingleLaserBeam(poseStack, bufferSource, activeTexture, entity, startPos, endPos, entityX, entityY, entityZ,
                    isPrime, true, true, false, false, gameTime, partialTicks, laserFadeIn);
                return;
            }

            // MODE 5: REGULAR LASER ATTACK (Strict Priority 5)
            double currentLaserRange = isPrime ? (angry ? 80.0 : 64.0) : (angry ? ANGRY_LASER_RANGE : LASER_RANGE);
            Vec3 targetAim = getActiveAimVector(entity, partialTicks);

            if (isPrime) {
                int syncedAimCount = getSyncedDataInt(entity, "laser_aim_count", entity.getPersistentData().getInt("fractus_laser_aim_count"));
                syncedAimCount = Math.max(syncedAimCount, SYNCED_REGULAR_IMPACT_COUNTS.getOrDefault(entity.getId(), 0));
                if (syncedAimCount > 5) syncedAimCount = 5;
                if (syncedAimCount >= 1) {
                    for (int i = 0; i < syncedAimCount; i++) {
                        double ax, ay, az;
                        if (i == 0) {
                            ax = entity.getPersistentData().getDouble("fractus_laser_aim_x");
                            ay = entity.getPersistentData().getDouble("fractus_laser_aim_y");
                            az = entity.getPersistentData().getDouble("fractus_laser_aim_z");
                            if (ax == 0.0 && ay == 0.0 && az == 0.0) {
                                ax = entity.getPersistentData().getDouble("fractus_aim_x");
                                ay = entity.getPersistentData().getDouble("fractus_aim_y");
                                az = entity.getPersistentData().getDouble("fractus_aim_z");
                            }
                        } else {
                            ax = entity.getPersistentData().getDouble("fractus_laser_aim_x_" + i);
                            ay = entity.getPersistentData().getDouble("fractus_laser_aim_y_" + i);
                            az = entity.getPersistentData().getDouble("fractus_laser_aim_z_" + i);
                        }
                        Vec3 targetDir = new Vec3(ax, ay, az);
                        if (targetDir.lengthSqr() < 0.001) {
                            targetDir = targetAim;
                        } else {
                            targetDir = targetDir.normalize();
                        }

                        // Smoothly rotate toward server targetDir using the exact AI prime turn rate
                        String mapKey = entity.getId() + "_" + i;
                        AimState state = AIM_STATES.get(mapKey);
                        if (state == null) {
                            state = new AimState(targetDir, targetDir, entity.tickCount);
                            AIM_STATES.put(mapKey, state);
                        }
                        if (entity.tickCount != state.lastTickCount) {
                            state.prevAim = state.currentAim;
                            double turnRate = firing 
                                ? (angry ? 0.0550 : 0.0350) 
                                : (angry ? 0.1200 : 0.0900);
                            state.currentAim = rotateToward(state.currentAim, targetDir, turnRate).normalize();
                            state.lastTickCount = entity.tickCount;
                        }
                        Vec3 beamDir = state.prevAim.scale(1.0 - partialTicks).add(state.currentAim.scale(partialTicks)).normalize();

                        Vec3 endPos = syncedImpactPosition(entity, IMPACT_REGULAR_BASE + i, startPos, partialTicks);
                        if (endPos == null) endPos = getLaserEnd(level, entity, startPos, beamDir, currentLaserRange);
                        renderSingleLaserBeam(poseStack, bufferSource, activeTexture, entity, startPos, endPos, entityX, entityY, entityZ,
                            isPrime, angry, firing, charging, false, gameTime, partialTicks, laserFadeIn);
                    }
                    return;
                }

                List<LivingEntity> multipleTargets = findMultipleTargets(level, entity, angry, currentLaserRange);
                if (!multipleTargets.isEmpty()) {
                    for (int i = 0; i < multipleTargets.size(); i++) {
                        LivingEntity t = multipleTargets.get(i);
                        Vec3 targetCenter = t.position().add(0, t.getBbHeight() * 0.5, 0);
                        Vec3 diff = targetCenter.subtract(startPos);
                        Vec3 targetDir = i == 0 ? targetAim : (diff.lengthSqr() < 0.001 ? targetAim : diff.normalize());

                        // Smoothly rotate toward server targetDir using the exact AI prime turn rate
                        String mapKey = entity.getId() + "_" + t.getId();
                        AimState state = AIM_STATES.get(mapKey);
                        if (state == null) {
                            state = new AimState(targetDir, targetDir, entity.tickCount);
                            AIM_STATES.put(mapKey, state);
                        }
                        if (entity.tickCount != state.lastTickCount) {
                            state.prevAim = state.currentAim;
                            double turnRate = firing 
                                ? (angry ? 0.0550 : 0.0350) 
                                : (angry ? 0.1200 : 0.0900);
                            state.currentAim = rotateToward(state.currentAim, targetDir, turnRate).normalize();
                            state.lastTickCount = entity.tickCount;
                        }
                        Vec3 beamDir = state.prevAim.scale(1.0 - partialTicks).add(state.currentAim.scale(partialTicks)).normalize();

                        Vec3 endPos = getLaserEnd(level, entity, startPos, beamDir, currentLaserRange);
                        renderSingleLaserBeam(poseStack, bufferSource, activeTexture, entity, startPos, endPos, entityX, entityY, entityZ,
                            isPrime, angry, firing, charging, false, gameTime, partialTicks, laserFadeIn);
                    }
                    return;
                }
            }

            // Single regular beam
            Vec3 endPos = syncedImpactPosition(entity, IMPACT_REGULAR_BASE, startPos, partialTicks);
            if (endPos == null) endPos = getLaserEnd(level, entity, startPos, targetAim, currentLaserRange);
            renderSingleLaserBeam(poseStack, bufferSource, activeTexture, entity, startPos, endPos, entityX, entityY, entityZ,
                isPrime, angry, firing, charging, false, gameTime, partialTicks, laserFadeIn);
        }

        @SubscribeEvent
        public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
            if (USE_OLD_LASER_PARTICLES) {
                return;
            }
            LivingEntity entity = event.getEntity();
            if (entity == null || !isFractus(entity)) {
                return;
            }

            boolean isPrime = "fractus_prime".equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
            ResourceLocation activeTexture = USE_BEACON_BEAM_TEXTURE ? BEACON_BEAM_LOCATION : CUSTOM_LASER_LOCATION;

            // 3D AOE SPHERE: Render complete 360-degree geodesic sphere and energy rings for Fractus Prime
            if (isPrime) {
                int sphereTicks = entity.getArrowCount();
                if (sphereTicks <= 0) {
                    sphereTicks = getSyncedDataInt(entity, "sphere_ticks", 0);
                }
                boolean isAoe = sphereTicks > 0
                    || getSyncedDataBoolean(entity, "is_laser_aoe_activating", false);

                if (isAoe) {
                    renderFractusPrimeSphereLayer(event.getPoseStack(), event.getMultiBufferSource(), activeTexture,
                        entity, sphereTicks, event.getPartialTick());
                }
            } else {
                // Expanding celestial blue sphere in the center of the summoning ritual
                Float fadeVal = SUMMON_FADE_TIMERS.get(entity.getId());
                float summonFadeAlpha = (fadeVal != null) ? fadeVal : 0.0f;
                boolean isSummoning = (entity.isShiftKeyDown() && entity.isSprinting())
                    || entity.isSwimming()
                    || getSyncedDataBoolean(entity, "is_summoning", false)
                    || entity.getPersistentData().getInt("fractus_summon_role") > 0
                    || entity.getPersistentData().getInt("fractus_summon_ticks") > 0
                    || (summonFadeAlpha > 0.001f);
                int role = entity.getPersistentData().getInt("fractus_summon_role");

                if (isSummoning && (role == 1 || role == 0) && summonFadeAlpha > 0.001f) {
                    renderFractusSummonSphere(event.getPoseStack(), event.getMultiBufferSource(), activeTexture,
                        entity, event.getPartialTick(), summonFadeAlpha);
                }
            }
        }

        private static void renderSingleLaserBeam(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation activeTexture,
                                                   LivingEntity entity, Vec3 startPos, Vec3 endPos, double entityX, double entityY, double entityZ,
                                                   boolean isPrime, boolean angry, boolean firing, boolean charging, boolean isBurst,
                                                   long gameTime, float partialTicks, float laserFadeIn) {
            renderSingleLaserBeam(poseStack, bufferSource, activeTexture, entity, startPos, endPos, entityX, entityY, entityZ,
                isPrime, angry, firing, charging, isBurst, gameTime, partialTicks, laserFadeIn, 1.0f, false);
        }

        private static void renderSingleLaserBeam(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation activeTexture,
                                                   LivingEntity entity, Vec3 startPos, Vec3 endPos, double entityX, double entityY, double entityZ,
                                                   boolean isPrime, boolean angry, boolean firing, boolean charging, boolean isBurst,
                                                   long gameTime, float partialTicks, float laserFadeIn,
                                                   float postFadeAlpha, boolean isBurstPostFading) {
            if (laserFadeIn <= 0.001f) return;
            if (isBurst && !firing) {
                charging = true;
            }
            poseStack.pushPose();

            double renderStartX = startPos.x - entityX;
            double renderStartY = startPos.y - entityY;
            double renderStartZ = startPos.z - entityZ;
            poseStack.translate(renderStartX, renderStartY, renderStartZ);

            Vec3 dir = endPos.subtract(startPos);
            double length = dir.length();
            if (length > 0.01) {
                Vec3 dirNorm = dir.normalize();
                // Mathematical orientation from +Y to dirNorm without gimbal lock or axis distortion
                Quaternionf rot = new Quaternionf().rotationTo(0, 1, 0, (float) dirNorm.x, (float) dirNorm.y, (float) dirNorm.z);
                poseStack.mulPose(rot);

                float shimmer = (float) Math.sin((gameTime + partialTicks) * 1.6f);
                float shaderScale = isShaderActive() ? SHADER_LASER_SIZE_FACTOR : 1.0f;
                float widthFactor = (1.0f + shimmer * 0.08f) * shaderScale;

                if (charging && !firing) {
                    // Charging preview sphere
                    int chargeColor = (235 << 24) | (255 << 16) | (20 << 8) | 20;
                    float sphereRadius = (isPrime ? 0.22f : 0.15f) * (1.0f + (float) Math.sin((gameTime + partialTicks) * 2.5f) * 0.20f) * shaderScale;
                    poseStack.pushPose();
                    poseStack.translate(-0.5D, 0.0D, -0.5D);
                    poseStack.scale(sphereRadius, 0.02f, sphereRadius);
                    BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, gameTime, 0, 1, chargeColor, 0.0f, 1.0f);
                    poseStack.popPose();
                } else {
                    int height = (int) Math.ceil(length);
                    if (height < 1) height = 1;
                    float scaleY = (float) (length / (double) height);

                    if (isBurst) {
                        int burstTicks = entity.getPersistentData().getInt("fractus_burst_timer");
                        if (burstTicks == 0) {
                            burstTicks = getSyncedDataInt(entity, "burst_timer", 0);
                        }
                        int burstState = entity.getPersistentData().getInt("fractus_burst_state");
                        if (burstState == 0) {
                            burstState = getSyncedDataInt(entity, "burst_state", 0);
                        }

                        // Dissipation Phase Ease-Out Fade (mirroring Fractus Prime Sphere dissipation logic)
                        float burstFade = 1.0f;
                        if (isBurstPostFading) {
                            burstFade = postFadeAlpha;
                        } else if (burstState == 4) {
                            float remainingTicks = (float) burstTicks + (1.0f - partialTicks);
                            burstFade = Mth.clamp(remainingTicks / 10.0f, 0.0f, 1.0f);
                        } else if (burstTicks > 0) {
                            int dissipationThreshold = isPrime ? 30 : 20;
                            if (burstTicks <= dissipationThreshold) {
                                float rem = (float) burstTicks - partialTicks;
                                double fadeProgress = Math.max(0.0, Math.min(1.0, 1.0 - (rem / (double) dissipationThreshold)));
                                burstFade = (float) Math.pow(1.0 - fadeProgress, 1.8);
                            }
                        }

                        float totalFade = Mth.clamp(burstFade * laserFadeIn, 0.0f, 1.0f);
                        if (totalFade <= 0.001f) {
                            poseStack.popPose();
                            return;
                        }

                        float fadeAlpha = (float) Math.pow(totalFade, 1.8);
                        float fadeScale = (float) Math.pow(totalFade, 0.6);

                        int numArms = isPrime ? 3 : 2;
                        double helixRadius = (isPrime ? BURST_HELIX_RADIUS_PRIME : BURST_HELIX_RADIUS_FRACTUS) * fadeScale * shaderScale;
                        float pitch = isPrime ? 0.20f : 0.22f;
                        int burstHelixA = Mth.clamp((int) (235 * fadeAlpha), 0, 255);

                        // 1. Render 3D Spiraling Helices wrapping cleanly around the burst beam
                        renderLaserHelices(poseStack, bufferSource, activeTexture, length, numArms, helixRadius, pitch,
                            255, 45, 15, burstHelixA, gameTime, partialTicks, 0.75f);

                        // 2. Render Main Laser Burst Beam
                        poseStack.pushPose();
                        poseStack.translate(-0.5D, 0.0D, -0.5D);
                        poseStack.scale(1.0f, scaleY, 1.0f);

                        float coreScale = (isPrime ? PRIME_BURST_CORE_SCALE : FRACTUS_BURST_CORE_SCALE) * fadeScale;
                        float midScale = (isPrime ? PRIME_BURST_MID_SCALE : FRACTUS_BURST_MID_SCALE) * fadeScale;
                        float outerScale = (isPrime ? PRIME_BURST_OUTER_SCALE : FRACTUS_BURST_OUTER_SCALE) * fadeScale;

                        // Deep crimson outer corona
                        int coronaA = Mth.clamp((int) (130 * fadeAlpha), 0, 255);
                        int outerCorona = (coronaA << 24) | (220 << 16) | (0 << 8) | 15;
                        BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, -gameTime, 0, height, outerCorona,
                            midScale * widthFactor, outerScale * widthFactor);

                        // Blazing fiery crimson mid
                        int midA = Mth.clamp((int) (245 * fadeAlpha), 0, 255);
                        int midColor = (midA << 24) | (255 << 16) | (40 << 8) | 10;
                        BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, gameTime * 2, 0, height, midColor,
                            coreScale * widthFactor, midScale * widthFactor);

                        // Blazing white / super-hot light orange core
                        int coreA = Mth.clamp((int) (255 * fadeAlpha), 0, 255);
                        int coreColor = (coreA << 24) | (255 << 16) | (245 << 8) | 230;
                        BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, gameTime, 0, height, coreColor,
                            0.0f, coreScale * widthFactor);

                        poseStack.popPose();
                    } else {
                        // Standard / Normal Laser Beam - Add faint elegant spiral helix
                        int stdHelixArms = isPrime ? 2 : 1;
                        double stdHelixRadius = (isPrime ? 0.22 : 0.16) * shaderScale;
                        int stdHelixR = 255;
                        int stdHelixG = angry ? 20 : 60;
                        int stdHelixB = angry ? 10 : 30;
                        int stdHelixA = Mth.clamp((int) (115 * laserFadeIn), 0, 255);
                        renderLaserHelices(poseStack, bufferSource, activeTexture, length, stdHelixArms, stdHelixRadius, 0.35f,
                            stdHelixR, stdHelixG, stdHelixB, stdHelixA, gameTime, partialTicks, 0.45f);

                        poseStack.pushPose();
                        poseStack.translate(-0.5D, 0.0D, -0.5D);
                        poseStack.scale(1.0f, scaleY, 1.0f);

                        if (isPrime) {
                            int coronaA = Mth.clamp((int) ((angry ? 240 : 220) * laserFadeIn), 0, 255);
                            int coronaColor = angry ? (coronaA << 24) | (255 << 16) | (0 << 8) | 40 : (coronaA << 24) | (230 << 16) | (5 << 8) | 15;
                            BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, gameTime, 0, height, coronaColor,
                                PRIME_LASER_CORE_SCALE * widthFactor, PRIME_LASER_CORONA_SCALE * widthFactor);

                            int coreA = Mth.clamp((int) (255 * laserFadeIn), 0, 255);
                            int coreColor = (coreA << 24) | (255 << 16) | (230 << 8) | 235;
                            BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, gameTime, 0, height, coreColor,
                                0.0f, PRIME_LASER_CORE_SCALE * widthFactor);
                        } else {
                            int auraA = Mth.clamp((int) ((angry ? 235 : 215) * laserFadeIn), 0, 255);
                            int redAuraColor = angry ? (auraA << 24) | (255 << 16) | (0 << 8) | 10 : (auraA << 24) | (255 << 16) | (5 << 8) | 25;
                            BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, gameTime, 0, height, redAuraColor,
                                FRACTUS_LASER_CORE_SCALE * widthFactor, FRACTUS_LASER_AURA_SCALE * widthFactor);

                            int whiteA = Mth.clamp((int) (255 * laserFadeIn), 0, 255);
                            int whiteCoreColor = (whiteA << 24) | (255 << 16) | (245 << 8) | 245;
                            BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, gameTime, 0, height, whiteCoreColor,
                                0.0f, FRACTUS_LASER_CORE_SCALE * widthFactor);
                        }

                        poseStack.popPose();
                    }
                }
            }

            poseStack.popPose();
        }

        private static void renderTelekinesisBeam(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation activeTexture,
                                                   Vec3 startPos, Vec3 endPos, double entityX, double entityY, double entityZ,
                                                   long gameTime, float partialTicks, float laserFadeIn) {
            if (laserFadeIn <= 0.001f) return;
            poseStack.pushPose();

            double renderStartX = startPos.x - entityX;
            double renderStartY = startPos.y - entityY;
            double renderStartZ = startPos.z - entityZ;
            poseStack.translate(renderStartX, renderStartY, renderStartZ);

            Vec3 dir = endPos.subtract(startPos);
            double length = dir.length();
            if (length > 0.01) {
                Vec3 dirNorm = dir.normalize();
                Quaternionf rot = new Quaternionf().rotationTo(0, 1, 0, (float) dirNorm.x, (float) dirNorm.y, (float) dirNorm.z);
                poseStack.mulPose(rot);

                int height = (int) Math.ceil(length);
                if (height < 1) height = 1;
                float scaleY = (float) (length / (double) height);

                float shimmer = (float) Math.sin((gameTime + partialTicks) * 2.2f);
                float shaderScale = isShaderActive() ? SHADER_LASER_SIZE_FACTOR : 1.0f;
                float widthFactor = (1.0f + shimmer * 0.06f) * shaderScale;

                // Faint pure white spiral helix around telekinesis tractor beam
                int teleHelixA = Mth.clamp((int) (140 * laserFadeIn), 0, 255);
                renderLaserHelices(poseStack, bufferSource, activeTexture, length, 2, 0.28 * shaderScale, 0.25f,
                    245, 250, 255, teleHelixA, gameTime, partialTicks, 0.55f);

                poseStack.pushPose();
                poseStack.translate(-0.5D, 0.0D, -0.5D);
                poseStack.scale(1.0f, scaleY, 1.0f);

                // Translucent glowing pure white outer aura / corona (30% smaller)
                int auraA = Mth.clamp((int) (150 * laserFadeIn), 0, 255);
                int auraColor = (auraA << 24) | (255 << 16) | (255 << 8) | 255;
                BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, -gameTime, 0, height, auraColor,
                    0.14f * widthFactor, 0.455f * widthFactor);

                // Intense pure white beam core (30% smaller)
                int coreA = Mth.clamp((int) (255 * laserFadeIn), 0, 255);
                int coreColor = (coreA << 24) | (255 << 16) | (255 << 8) | 255;
                BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, gameTime, 0, height, coreColor,
                    0.0f, 0.14f * widthFactor);

                poseStack.popPose();
            }

            poseStack.popPose();
        }

        // 3D Spiraling Helices wrapping cleanly around any laser beam in local coordinate space (Y axis is beam direction)
        private static void renderLaserHelices(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                                               double length, int numArms, double ringRadius, float pitch,
                                               int r, int g, int b, int a, long gameTime, float partialTicks, float speedFactor) {
            if (a <= 0 || length < 0.4) return;
            VertexConsumer builder = bufferSource.getBuffer(getGlowRenderType(texture));
            Matrix4f matrix = poseStack.last().pose();

            double step = 0.35;
            float tubeRadius = (float) (ringRadius * 0.08);
            float speed = (float) ((gameTime + partialTicks) * speedFactor);

            for (int arm = 0; arm < numArms; arm++) {
                double baseArmAngle = (Math.PI * 2.0 * (double) arm / (double) numArms) + speed;

                for (double d = 0.2; d < length - 0.2; d += step) {
                    double dNext = Math.min(length - 0.2, d + step);

                    double a1 = baseArmAngle + (d * pitch);
                    double a2 = baseArmAngle + (dNext * pitch);

                    float x1 = (float) (Math.cos(a1) * ringRadius);
                    float z1 = (float) (Math.sin(a1) * ringRadius);
                    float y1 = (float) d;

                    float x2 = (float) (Math.cos(a2) * ringRadius);
                    float z2 = (float) (Math.sin(a2) * ringRadius);
                    float y2 = (float) dNext;

                    // 3D cylindrical tube facet geometry along the helix
                    renderHelixTubeSegment(builder, matrix, poseStack, x1, y1, z1, x2, y2, z2, tubeRadius, r, g, b, a);
                }
            }
        }

        private static void renderHelixTubeSegment(VertexConsumer builder, Matrix4f matrix, PoseStack poseStack,
                                                   float x1, float y1, float z1, float x2, float y2, float z2,
                                                   float radius, int r, int g, int b, int a) {
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dz = z2 - z1;
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.001f) return;

            // Normalized direction vector
            float udx = dx / len;
            float udy = dy / len;
            float udz = dz / len;

            // Perpendicular vectors for 3D cross-section
            float px = -udz;
            float py = 0;
            float pz = udx;
            float pLen = (float) Math.sqrt(px * px + pz * pz);
            if (pLen < 0.001f) {
                px = 1; py = 0; pz = 0;
            } else {
                px /= pLen; pz /= pLen;
            }

            // Second perpendicular vector = dir x p
            float qx = udy * pz - udz * py;
            float qy = udz * px - udx * pz;
            float qz = udx * py - udy * px;

            int facets = 4;
            for (int f = 0; f < facets; f++) {
                double fa1 = 2.0 * Math.PI * f / facets;
                double fa2 = 2.0 * Math.PI * (f + 1) / facets;

                float c1 = (float) Math.cos(fa1) * radius;
                float s1 = (float) Math.sin(fa1) * radius;
                float c2 = (float) Math.cos(fa2) * radius;
                float s2 = (float) Math.sin(fa2) * radius;

                float vx1 = x1 + px * c1 + qx * s1;
                float vy1 = y1 + py * c1 + qy * s1;
                float vz1 = z1 + pz * c1 + qz * s1;

                float vx2 = x1 + px * c2 + qx * s2;
                float vy2 = y1 + py * c2 + qy * s2;
                float vz2 = z1 + pz * c2 + qz * s2;

                float vx3 = x2 + px * c2 + qx * s2;
                float vy3 = y2 + py * c2 + qy * s2;
                float vz3 = z2 + pz * c2 + qz * s2;

                float vx4 = x2 + px * c1 + qx * s1;
                float vy4 = y2 + py * c1 + qy * s1;
                float vz4 = z2 + pz * c1 + qz * s1;

                float nx = (px * (c1 + c2) * 0.5f + qx * (s1 + s2) * 0.5f) / radius;
                float ny = (py * (c1 + c2) * 0.5f + qy * (s1 + s2) * 0.5f) / radius;
                float nz = (pz * (c1 + c2) * 0.5f + qz * (s1 + s2) * 0.5f) / radius;

                builder.addVertex(matrix, vx1, vy1, vz1).setColor(r, g, b, a).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), nx, ny, nz);
                builder.addVertex(matrix, vx2, vy2, vz2).setColor(r, g, b, a).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), nx, ny, nz);
                builder.addVertex(matrix, vx3, vy3, vz3).setColor(r, g, b, a).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), nx, ny, nz);
                builder.addVertex(matrix, vx4, vy4, vz4).setColor(r, g, b, a).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), nx, ny, nz);
            }
        }

        // 3D AOE Geodesic Sphere and Orbital Energy Rings for Fractus Prime (Full 360-degree volume)
        private static void renderFractusPrimeSphereLayer(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                                                          LivingEntity entity, int sphereTicks, float partialTicks) {
            Level level = entity.level();
            long gameTime = level.getGameTime();
            float animTime = (float) (gameTime + partialTicks);

            // Strictly obeys handleLaserSphereBurst math from FractusPrimeAI:
            // SPHERE_START_RADIUS = 1.5;
            // SPHERE_MAX_BUILDUP_RADIUS = 4.5;
            // SPHERE_MAX_RELEASE_RADIUS = 17.0;
            // Total sphereTicks starts at 270.
            double currentRadius;
            float shellAlpha;
            int r, g, b;
            boolean isReleasing = false;
            float shockwaveRadius = 0.0f;
            float shockwaveAlpha = 0.0f;

            if (sphereTicks > 240) {
                // Pre-sphere buildup warning (270 -> 240 ticks)
                int buildupElapsed = 270 - sphereTicks;
                double progress = ((double) buildupElapsed + partialTicks) / 30.0;
                currentRadius = 0.6 + progress * (1.5 - 0.6);
                shellAlpha = (float) (0.35 + progress * 0.40);
                r = 255; g = 50; b = 20;
            } else {
                int elapsed = 240 - sphereTicks;
                if (elapsed < 135) {
                    // Hollow Sphere Buildup Phase: 1.5 -> 4.5 blocks (elapsed 0 to 134)
                    double progress = ((double) Math.min(elapsed, 134) + partialTicks) / 134.0;
                    currentRadius = 1.5 + progress * (4.5 - 1.5);
                    // High-density humming energy shell with breathing pulsation
                    shellAlpha = (float) (0.65 + Math.sin(animTime * 0.35f) * 0.15f);
                    r = 255; g = (int) (30 + progress * 50); b = 15;
                } else {
                    // Detonation / Release Phase: 4.5 -> 17.0 blocks in 5 ticks! (elapsed 135 to 140)
                    isReleasing = true;
                    double releaseProgress = Math.min(1.0, ((double) (elapsed - 135) + partialTicks) / 5.0);
                    currentRadius = 4.5 + releaseProgress * (17.0 - 4.5);

                    if (elapsed <= 140) {
                        // Flash and violent ground shockwave
                        shellAlpha = 0.95f;
                        r = 255; g = 180; b = 60; // Blazing gold-crimson detonation flash
                        shockwaveRadius = (float) currentRadius;
                        shockwaveAlpha = 1.0f;
                    } else {
                        // Dissipation Phase (elapsed 141 to 240, remaining 100 ticks)
                        // Fades faster with an exponential ease-out curve cleanly to 0.0 without any abrupt cutoff
                        double fadeProgress = Math.min(1.0, ((double) (elapsed - 140) + partialTicks) / 50.0);
                        float fadeCurve = (float) Math.pow(1.0 - fadeProgress, 1.8);
                        shellAlpha = 0.85f * fadeCurve;
                        if (shellAlpha <= 0.001f) {
                            return;
                        }
                        r = 255; g = 30; b = 10;
                        shockwaveRadius = (float) (17.0 + (elapsed - 140) * 0.2);
                        shockwaveAlpha = 0.5f * (float) Math.pow(1.0 - fadeProgress, 2.0);
                    }
                }
            }

            if (currentRadius < 0.15) return;

            poseStack.pushPose();
            // Center sphere at entity's torso center
            poseStack.translate(0.0, (double) entity.getBbHeight() * 0.5, 0.0);

            float rFloat = (float) currentRadius;
            int alphaInt = Mth.clamp((int) (shellAlpha * 255.0f), 0, 255);

            // 1. Render 3 Harmonic 3D Orbital Energy Rings orbiting tightly around the sphere
            float ringSpeed = isReleasing ? 0.12f : 0.045f;
            renderFull3DRing(poseStack, bufferSource, texture, rFloat * 1.025f, animTime * ringSpeed, 0.0f, 1.0f, 0.0f,
                r, g, b, Mth.clamp((int) (alphaInt * 1.15f), 0, 255));
            renderFull3DRing(poseStack, bufferSource, texture, rFloat * 1.025f, animTime * -ringSpeed * 0.85f, 0.707f, 0.0f, 0.707f,
                r, Math.min(255, g + 40), b, Mth.clamp((int) (alphaInt * 1.05f), 0, 255));
            renderFull3DRing(poseStack, bufferSource, texture, rFloat * 1.025f, animTime * ringSpeed * 1.15f, 0.0f, 0.707f, 0.707f,
                r, g, Math.min(255, b + 30), Mth.clamp((int) (alphaInt * 0.95f), 0, 255));

            // 2. Render Multi-layer Volumetric Geodesic Energy Plasma Spheres (Core + Mantle + Corona)
            // Inner Core with counter-rotation UV scrolling
            renderGeodesicShell3D(poseStack, bufferSource, texture, rFloat * 0.72f, alphaInt, 255, Math.min(255, g + 80), 40, animTime, -0.05f, 0.03f);
            // Main Plasma Mantle with forward UV scrolling
            renderGeodesicShell3D(poseStack, bufferSource, texture, rFloat, alphaInt, r, g, b, animTime, 0.04f, -0.02f);
            // Outer Ethereal Atmosphere Aura
            renderGeodesicShell3D(poseStack, bufferSource, texture, rFloat * 1.08f, (int) (alphaInt * 0.35f), 255, 20, 10, animTime, -0.02f, -0.04f);

            // 3. Ground Shockwave during detonation and dissipation
            if (shockwaveRadius > 0.0f && shockwaveAlpha > 0.01f) {
                renderGroundShockwave(poseStack, bufferSource, texture, shockwaveRadius, (int) (shockwaveAlpha * 255.0f));
            }

            poseStack.popPose();
        }

        // Standard Fractus Summoning Inward Celestial Blue Laser Beam
        private static void renderSummonLaserBeam(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation activeTexture,
                                                   LivingEntity entity, Vec3 startPos, Vec3 endPos, double entityX, double entityY, double entityZ,
                                                   long gameTime, float partialTicks, float summonFadeAlpha) {
            if (summonFadeAlpha <= 0.001f) return;
            poseStack.pushPose();
            double renderStartX = startPos.x - entityX;
            double renderStartY = startPos.y - entityY;
            double renderStartZ = startPos.z - entityZ;
            poseStack.translate(renderStartX, renderStartY, renderStartZ);

            Vec3 dir = endPos.subtract(startPos);
            double length = dir.length();
            if (length > 0.01) {
                Vec3 dirNorm = dir.normalize();
                Quaternionf rot = new Quaternionf().rotationTo(0, 1, 0, (float) dirNorm.x, (float) dirNorm.y, (float) dirNorm.z);
                poseStack.mulPose(rot);

                float shimmer = (float) Math.sin((gameTime + partialTicks) * 1.5f);
                float shaderScale = isShaderActive() ? SHADER_LASER_SIZE_FACTOR : 1.0f;
                float widthFactor = (1.0f + shimmer * 0.06f) * shaderScale;

                int height = (int) Math.ceil(length);
                if (height < 1) height = 1;
                float scaleY = (float) (length / (double) height);

                poseStack.pushPose();
                poseStack.translate(-0.5D, 0.0D, -0.5D);
                poseStack.scale(1.0f, scaleY, 1.0f);

                // Celestial cyan/blue aura
                int blueAuraA = Mth.clamp((int) (220 * summonFadeAlpha), 0, 255);
                int blueAuraColor = (blueAuraA << 24) | (0 << 16) | (170 << 8) | 255;
                BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, gameTime, 0, height, blueAuraColor,
                    0.06f * widthFactor, 0.18f * widthFactor);

                // Pure white/cyan core
                int whiteCoreA = Mth.clamp((int) (255 * summonFadeAlpha), 0, 255);
                int whiteCoreColor = (whiteCoreA << 24) | (210 << 16) | (245 << 8) | 255;
                BeaconRenderer.renderBeaconBeam(poseStack, compatibleBeamBufferSource(bufferSource, activeTexture), activeTexture, partialTicks, 1.0f, gameTime, 0, height, whiteCoreColor,
                    0.0f, 0.06f * widthFactor);

                poseStack.popPose();
            }

            poseStack.popPose();
        }

        // Calculates the shared ritual center point so that all summoning Fractus seamlessly connect their beams to the sphere
        private static Vec3 getSummoningCenter(Level level, LivingEntity entity, float partialTicks) {
            double cx = entity.getPersistentData().getDouble("fractus_summon_center_x");
            double cy = entity.getPersistentData().getDouble("fractus_summon_center_y");
            double cz = entity.getPersistentData().getDouble("fractus_summon_center_z");
            if (cx != 0.0 || cy != 0.0 || cz != 0.0) {
                return new Vec3(cx, cy, cz);
            }

            // Calculate geometric centroid of all participating summoning Fractus within 60 blocks
            AABB searchBox = entity.getBoundingBox().inflate(60.0);
            List<LivingEntity> summoners = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> isFractus(e) && (
                (e.isShiftKeyDown() && e.isSprinting()) || e.isSwimming() || getSyncedDataBoolean(e, "is_summoning", false) || e.getPersistentData().getInt("fractus_summon_ticks") > 0 || e.getPersistentData().getInt("fractus_summon_role") > 0
            ));

            if (summoners.isEmpty()) {
                summoners = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> isFractus(e));
            }

            if (!summoners.isEmpty()) {
                double sumX = 0, sumY = 0, sumZ = 0;
                for (LivingEntity s : summoners) {
                    sumX += Mth.lerp(partialTicks, s.xo, s.getX());
                    sumY += Mth.lerp(partialTicks, s.yo, s.getY()) + s.getEyeHeight() * 0.5;
                    sumZ += Mth.lerp(partialTicks, s.zo, s.getZ());
                }
                return new Vec3(sumX / (double) summoners.size(), sumY / (double) summoners.size(), sumZ / (double) summoners.size());
            }

            Vec3 lookDir = getLookVector(entity, partialTicks);
            return laserStartSmooth(entity, partialTicks).add(lookDir.scale(8.0));
        }

        // Standard Fractus Summoning Celestial Blue Expanding Energy Sphere at ritual center (Full 360-degree volume)
        private static void renderFractusSummonSphere(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                                                      LivingEntity entity, float partialTicks, float summonFadeAlpha) {
            Vec3 center = getSummoningCenter(entity.level(), entity, partialTicks);
            double entityX = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double entityY = Mth.lerp(partialTicks, entity.yo, entity.getY());
            double entityZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
            renderFractusSummonSphere(poseStack, bufferSource, texture, entity, center, entityX, entityY, entityZ, partialTicks, summonFadeAlpha);
        }

        private static void renderFractusSummonSphere(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                                                      LivingEntity entity, Vec3 center, double entityX, double entityY, double entityZ, float partialTicks, float summonFadeAlpha) {
            if (summonFadeAlpha <= 0.001f) return;
            int ticks = entity.getPersistentData().getInt("fractus_summon_ticks");
            if (ticks <= 0) ticks = entity.getArrowCount();
            int startTicks = entity.getPersistentData().getInt("fractus_summon_start_ticks");
            if (startTicks <= 0) startTicks = 240;
            double elapsed = (double) (startTicks - ticks) + partialTicks;
            double progress = Math.min(1.0, Math.max(0.0, elapsed / (double) startTicks));

            // Forms and swells smoothly from a small converging nexus (0.15 blocks) up to 4.2 blocks as ritual progresses
            float radius = (float) (0.15 + progress * 4.05);

            poseStack.pushPose();
            poseStack.translate(center.x - entityX, center.y - entityY, center.z - entityZ);

            long gameTime = entity.level().getGameTime();
            float animTime = (float) (gameTime + partialTicks);

            float progressAlpha = (float) Math.min(1.0, progress * 3.5) * summonFadeAlpha;
            int shellAlpha = Mth.clamp((int) (progressAlpha * (150 + Math.sin(animTime * 0.3f) * 25)), 0, 245);
            if (shellAlpha < 5) {
                poseStack.popPose();
                return;
            }

            // Orbiting celestial cyan/blue 3D energy rings with separated radii to prevent Z-fighting and FPS drops
            renderFull3DRing(poseStack, bufferSource, texture, radius * 1.04f, animTime * 0.05f, 0, 1, 0, 0, 200, 255, (int)(235 * progressAlpha));
            renderFull3DRing(poseStack, bufferSource, texture, radius * 1.08f, animTime * -0.04f, 0.707f, 0, 0.707f, 40, 160, 255, (int)(220 * progressAlpha));
            renderFull3DRing(poseStack, bufferSource, texture, radius * 1.12f, animTime * 0.06f, 0, 0.707f, 0.707f, 0, 230, 255, (int)(210 * progressAlpha));

            // Multi-layered Celestial Plasma Shells with smooth animated UV scrolling (Full 360-degree sphere)
            // Inner Core
            renderGeodesicShell3D(poseStack, bufferSource, texture, radius * 0.70f, shellAlpha, 210, 245, 255, animTime, -0.05f, 0.03f);
            // Main Plasma Mantle
            renderGeodesicShell3D(poseStack, bufferSource, texture, radius, shellAlpha, 0, 190, 255, animTime, 0.04f, -0.02f);
            // Atmospheric Corona Aura
            renderGeodesicShell3D(poseStack, bufferSource, texture, radius * 1.12f, (int) (shellAlpha * 0.40f), 0, 120, 255, animTime, -0.02f, -0.04f);

            poseStack.popPose();
        }

        // 3D Toroidal Energy Ring rendered efficiently without Z-fighting or GPU stutter
        private static void renderFull3DRing(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                                             float radius, float angle, float axisX, float axisY, float axisZ,
                                             int r, int g, int b, int a) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationAxis(angle, axisX, axisY, axisZ));

            VertexConsumer builder = bufferSource.getBuffer(getGlowRenderType(texture));
            Matrix4f matrix = poseStack.last().pose();

            int segments = 24;
            int tubeFacets = 4;
            float tubeRadius = radius * 0.045f;

            for (int i = 0; i < segments; i++) {
                double a1 = 2.0 * Math.PI * i / segments;
                double a2 = 2.0 * Math.PI * (i + 1) / segments;

                float x1 = (float) Math.cos(a1) * radius;
                float z1 = (float) Math.sin(a1) * radius;
                float x2 = (float) Math.cos(a2) * radius;
                float z2 = (float) Math.sin(a2) * radius;

                float wave = 0.85f + 0.15f * (float) Math.sin(a1 * 4.0 + angle * 3.0f);
                int ringA = Mth.clamp((int) (a * wave), 0, 255);

                for (int tf = 0; tf < tubeFacets; tf++) {
                    double ta1 = 2.0 * Math.PI * tf / tubeFacets;
                    double ta2 = 2.0 * Math.PI * (tf + 1) / tubeFacets;

                    float cosT1 = (float) Math.cos(ta1) * tubeRadius;
                    float sinT1 = (float) Math.sin(ta1) * tubeRadius;
                    float cosT2 = (float) Math.cos(ta2) * tubeRadius;
                    float sinT2 = (float) Math.sin(ta2) * tubeRadius;

                    float vx1 = x1 + (float) Math.cos(a1) * cosT1;
                    float vy1 = sinT1;
                    float vz1 = z1 + (float) Math.sin(a1) * cosT1;

                    float vx2 = x1 + (float) Math.cos(a1) * cosT2;
                    float vy2 = sinT2;
                    float vz2 = z1 + (float) Math.sin(a1) * cosT2;

                    float vx3 = x2 + (float) Math.cos(a2) * cosT2;
                    float vy3 = sinT2;
                    float vz3 = z2 + (float) Math.sin(a2) * cosT2;

                    float vx4 = x2 + (float) Math.cos(a2) * cosT1;
                    float vy4 = sinT1;
                    float vz4 = z2 + (float) Math.sin(a2) * cosT1;

                    builder.addVertex(matrix, vx1, vy1, vz1).setColor(r, g, b, ringA).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
                    builder.addVertex(matrix, vx2, vy2, vz2).setColor(r, g, b, ringA).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
                    builder.addVertex(matrix, vx3, vy3, vz3).setColor(r, g, b, ringA).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
                    builder.addVertex(matrix, vx4, vy4, vz4).setColor(r, g, b, ringA).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
                }
            }

            poseStack.popPose();
        }

        private static void renderGroundShockwave(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, float radius, int a) {
            VertexConsumer builder = bufferSource.getBuffer(getGlowRenderType(texture));
            Matrix4f matrix = poseStack.last().pose();

            int segments = 48;
            float inner = radius * 0.88f;
            float outer = radius * 1.12f;

            for (int i = 0; i < segments; i++) {
                double a1 = 2.0 * Math.PI * i / segments;
                double a2 = 2.0 * Math.PI * (i + 1) / segments;

                float x1In = (float) (Math.cos(a1) * inner);
                float z1In = (float) (Math.sin(a1) * inner);
                float x1Out = (float) (Math.cos(a1) * outer);
                float z1Out = (float) (Math.sin(a1) * outer);

                float x2In = (float) (Math.cos(a2) * inner);
                float z2In = (float) (Math.sin(a2) * inner);
                float x2Out = (float) (Math.cos(a2) * outer);
                float z2Out = (float) (Math.sin(a2) * outer);

                builder.addVertex(matrix, x1In, 0.02f, z1In).setColor(255, 40, 10, a).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
                builder.addVertex(matrix, x1Out, 0.02f, z1Out).setColor(255, 10, 80, 0).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
                builder.addVertex(matrix, x2Out, 0.02f, z2Out).setColor(255, 10, 80, 0).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
                builder.addVertex(matrix, x2In, 0.02f, z2In).setColor(255, 40, 10, a).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
            }
        }

        // 3D Geodesic Plasma Shell with high-resolution continuous spherical UV mapping (Full 360-degree sphere)
        private static void renderGeodesicShell3D(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                                                  float radius, int a, int r, int g, int b,
                                                  float animTime, float uvSpeedU, float uvSpeedV) {
            VertexConsumer builder = bufferSource.getBuffer(getGlowRenderType(texture));
            Matrix4f matrix = poseStack.last().pose();

            int latSteps = 8;
            int lonSteps = 12;

            float uOffset = animTime * uvSpeedU;
            float vOffset = animTime * uvSpeedV;

            for (int i = 0; i < latSteps; i++) {
                double phi1 = (Math.PI * (double) i / (double) latSteps) - (Math.PI / 2.0);
                double phi2 = (Math.PI * (double) (i + 1) / (double) latSteps) - (Math.PI / 2.0);

                float y1 = (float) (radius * Math.sin(phi1));
                float r1 = (float) (radius * Math.cos(phi1));
                float y2 = (float) (radius * Math.sin(phi2));
                float r2 = (float) (radius * Math.cos(phi2));

                float v1 = (float) ((phi1 + Math.PI * 0.5) / Math.PI) + vOffset;
                float v2 = (float) ((phi2 + Math.PI * 0.5) / Math.PI) + vOffset;

                for (int j = 0; j < lonSteps; j++) {
                    double theta1 = 2.0 * Math.PI * (double) j / (double) lonSteps;
                    double theta2 = 2.0 * Math.PI * (double) (j + 1) / (double) lonSteps;

                    float x1 = (float) (r1 * Math.cos(theta1));
                    float z1 = (float) (r1 * Math.sin(theta1));
                    float x2 = (float) (r1 * Math.cos(theta2));
                    float z2 = (float) (r1 * Math.sin(theta2));

                    float x3 = (float) (r2 * Math.cos(theta2));
                    float z3 = (float) (r2 * Math.sin(theta2));
                    float x4 = (float) (r2 * Math.cos(theta1));
                    float z4 = (float) (r2 * Math.sin(theta1));

                    float u1 = (float) (theta1 / (2.0 * Math.PI)) + uOffset;
                    float u2 = (float) (theta2 / (2.0 * Math.PI)) + uOffset;

                    // Outer and inner faces with full spherical normals and continuous scrolling UV mapping
                    builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), x1 / radius, y1 / radius, z1 / radius);
                    builder.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u2, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), x2 / radius, y1 / radius, z2 / radius);
                    builder.addVertex(matrix, x3, y2, z3).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), x3 / radius, y2 / radius, z3 / radius);
                    builder.addVertex(matrix, x4, y2, z4).setColor(r, g, b, a).setUv(u1, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), x4 / radius, y2 / radius, z4 / radius);

                    builder.addVertex(matrix, x4, y2, z4).setColor(r, g, b, a).setUv(u1, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), -x4 / radius, -y2 / radius, -z4 / radius);
                    builder.addVertex(matrix, x3, y2, z3).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), -x3 / radius, -y2 / radius, -z3 / radius);
                    builder.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u2, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), -x2 / radius, -y1 / radius, -z2 / radius);
                    builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), -x1 / radius, -y1 / radius, -z1 / radius);
                }
            }
        }

        private static boolean isFractus(Entity entity) {
            if (entity == null) return false;
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
            return "fractus".equals(path) || "fractus_prime".equals(path);
        }

        private static boolean isAngry(LivingEntity entity) {
            if (entity == null) return false;
            return entity.getPersistentData().getBoolean("fractus_is_angry")
                || entity.getPersistentData().getBoolean("is_angry")
                || getSyncedDataBoolean(entity, "is_angry", false);
        }

        public static boolean getSyncedDataBoolean(Entity entity, String paramName, boolean defaultValue) {
            if (entity == null) return defaultValue;
            net.minecraft.network.syncher.EntityDataAccessor<Boolean> accessor = getEntityDataAccessor(entity, paramName);
            if (accessor != null) {
                try {
                    return entity.getEntityData().get(accessor);
                } catch (Exception ignored) {}
            }
            return entity.getPersistentData().contains(paramName) ? entity.getPersistentData().getBoolean(paramName) : defaultValue;
        }

        public static int getSyncedDataInt(Entity entity, String paramName, int defaultValue) {
            if (entity == null) return defaultValue;
            net.minecraft.network.syncher.EntityDataAccessor<Integer> accessor = getEntityDataAccessor(entity, paramName);
            if (accessor != null) {
                try {
                    return entity.getEntityData().get(accessor);
                } catch (Exception ignored) {}
            }
            return entity.getPersistentData().contains(paramName) ? entity.getPersistentData().getInt(paramName) : defaultValue;
        }

        private static final java.util.Map<String, java.lang.reflect.Field> SYNCED_DATA_FIELD_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        private static <T> net.minecraft.network.syncher.EntityDataAccessor<T> getEntityDataAccessor(Entity entity, String paramName) {
            if (entity == null || paramName == null) return null;
            String cacheKey = entity.getClass().getName() + ":" + paramName;
            java.lang.reflect.Field field = SYNCED_DATA_FIELD_CACHE.get(cacheKey);
            if (field == null) {
                String[] candidateNames = new String[]{
                    "DATA_" + paramName,
                    "DATA_" + paramName.toUpperCase(),
                    paramName,
                    "DATA_" + paramName.toLowerCase()
                };
                Class<?> current = entity.getClass();
                while (current != null && current != Object.class) {
                    for (String name : candidateNames) {
                        try {
                            java.lang.reflect.Field f = current.getDeclaredField(name);
                            f.setAccessible(true);
                            SYNCED_DATA_FIELD_CACHE.put(cacheKey, f);
                            field = f;
                            break;
                        } catch (NoSuchFieldException ignored) {}
                    }
                    if (field != null) break;
                    for (java.lang.reflect.Field f : current.getDeclaredFields()) {
                        if (f.getType() == net.minecraft.network.syncher.EntityDataAccessor.class) {
                            if (f.getName().equalsIgnoreCase("DATA_" + paramName) || f.getName().equalsIgnoreCase(paramName)) {
                                f.setAccessible(true);
                                SYNCED_DATA_FIELD_CACHE.put(cacheKey, f);
                                field = f;
                                break;
                            }
                        }
                    }
                    if (field != null) break;
                    current = current.getSuperclass();
                }
            }
            if (field != null) {
                try {
                    return (net.minecraft.network.syncher.EntityDataAccessor<T>) field.get(null);
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }

        private static List<LivingEntity> findMultipleTargets(Level level, LivingEntity self, boolean angry, double maxRange) {
            Vec3 startPos = laserStartSmooth(self, 1.0F);
            AABB searchBox = new AABB(
                startPos.x - maxRange, startPos.y - maxRange, startPos.z - maxRange,
                startPos.x + maxRange, startPos.y + maxRange, startPos.z + maxRange
            );
            List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> {
                if (e == self || !e.isAlive() || isUntargetable(e)) return false;
                if (e instanceof AgeableMob ageable && ageable.isBaby()) return false;
                if (!angry) {
                    boolean isStrong = e instanceof Player || e.getMaxHealth() >= 50.0f;
                    if (!isStrong) return false;
                }
                return true;
            });
            List<LivingEntity> targets = new ArrayList<>();
            list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(self)));
            for (LivingEntity candidate : list) {
                if (targets.size() >= 5) break;
                Vec3 targetCenter = candidate.position().add(0, candidate.getBbHeight() * 0.5, 0);
                BlockHitResult ray = level.clip(new ClipContext(startPos, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
                if (ray.getType() == HitResult.Type.MISS || ray.getLocation().distanceTo(startPos) >= startPos.distanceTo(targetCenter) - 0.5) {
                    targets.add(candidate);
                }
            }
            return targets;
        }

        private static Vec3 getLookVector(LivingEntity entity, float partialTicks) {
            float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            float yaw = Mth.lerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
            float f = pitch * ((float) Math.PI / 180F);
            float f1 = -yaw * ((float) Math.PI / 180F);
            float f2 = Mth.cos(f1);
            float f3 = Mth.sin(f1);
            float f4 = Mth.cos(f);
            float f5 = Mth.sin(f);
            return new Vec3(f3 * f4, -f5, f2 * f4).normalize();
        }

        private static Vec3 laserStartSmooth(LivingEntity entity, float partialTicks) {
            double x = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double y = Mth.lerp(partialTicks, entity.yo, entity.getY());
            double z = Mth.lerp(partialTicks, entity.zo, entity.getZ());
            Vec3 base = new Vec3(x, y + (double) entity.getBbHeight() * 0.5, z);

            float yaw = Mth.lerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
            float yawRad = -yaw * ((float) Math.PI / 180F);
            float cosYaw = Mth.cos(yawRad);
            float sinYaw = Mth.sin(yawRad);

            double rx = OFFSET_X * cosYaw - OFFSET_Z * sinYaw;
            double rz = OFFSET_X * sinYaw + OFFSET_Z * cosYaw;

            return base.add(rx, OFFSET_Y, rz);
        }

        private static Vec3 getActiveAimVector(LivingEntity entity, float partialTicks) {
            Vec3 aim = null;
            if (entity.getPersistentData().getInt("fractus_burst_timer") > 0) {
                double bx = entity.getPersistentData().getDouble("fractus_burst_aim_x");
                double by = entity.getPersistentData().getDouble("fractus_burst_aim_y");
                double bz = entity.getPersistentData().getDouble("fractus_burst_aim_z");
                Vec3 b = new Vec3(bx, by, bz);
                if (b.lengthSqr() > 0.001) aim = b.normalize();
            } else if (entity.getPersistentData().getInt("fractus_destroying_fire") > 0) {
                double dx = entity.getPersistentData().getDouble("fractus_destroying_aim_x");
                double dy = entity.getPersistentData().getDouble("fractus_destroying_aim_y");
                double dz = entity.getPersistentData().getDouble("fractus_destroying_aim_z");
                Vec3 d = new Vec3(dx, dy, dz);
                if (d.lengthSqr() > 0.001) aim = d.normalize();
            } else {
                String kx = entity.getPersistentData().contains("fractus_laser_aim_x") ? "fractus_laser_aim_x" : "fractus_aim_x";
                String ky = entity.getPersistentData().contains("fractus_laser_aim_y") ? "fractus_laser_aim_y" : "fractus_aim_y";
                String kz = entity.getPersistentData().contains("fractus_laser_aim_z") ? "fractus_laser_aim_z" : "fractus_aim_z";
                double ax = entity.getPersistentData().getDouble(kx);
                double ay = entity.getPersistentData().getDouble(ky);
                double az = entity.getPersistentData().getDouble(kz);
                Vec3 a = new Vec3(ax, ay, az);
                if (a.lengthSqr() > 0.001) aim = a.normalize();
            }

            Vec3 look = getLookVector(entity, partialTicks);
            if (aim == null || aim.lengthSqr() < 0.001) {
                return look;
            }
            return aim;
        }

        private static Vec3 getBurstAimVector(LivingEntity entity, float partialTicks) {
            double bx = entity.getPersistentData().getDouble("fractus_burst_aim_x");
            double by = entity.getPersistentData().getDouble("fractus_burst_aim_y");
            double bz = entity.getPersistentData().getDouble("fractus_burst_aim_z");
            Vec3 burstAim = new Vec3(bx, by, bz);
            if (burstAim.lengthSqr() > 0.001) return burstAim.normalize();

            Vec3 previousAim = BURST_LAST_AIM_VECTORS.get(entity.getId());
            return previousAim != null && previousAim.lengthSqr() > 0.001
                ? previousAim.normalize()
                : new Vec3(0, 0, 1);
        }

        private static Vec3 getBurstImpactPosition(Level level, LivingEntity entity, Vec3 startPos, float partialTicks, boolean isPrime) {
            Vec3 syncedImpact = syncedImpactPosition(entity, IMPACT_BURST, startPos, partialTicks);
            if (syncedImpact != null) return syncedImpact;

            Vec3 fallbackAim = getBurstAimVector(entity, partialTicks);
            return getBurstLaserEnd(level, entity, startPos, fallbackAim, isPrime ? 256.0 : 128.0);
        }

        private static Vec3 getTelekinesisEndPos(Level level, LivingEntity self, Vec3 start, Vec3 targetAim, double maxRange, float partialTicks) {
            String uuidStr = self.getPersistentData().getString("telekinesis_target_uuid");
            if (!uuidStr.isEmpty()) {
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                    for (Entity e : level.getEntitiesOfClass(LivingEntity.class, self.getBoundingBox().inflate(32.0))) {
                        if (e.getUUID().equals(uuid) && e.isAlive() && !isUntargetable(e)) {
                            return new Vec3(
                                Mth.lerp(partialTicks, e.xo, e.getX()),
                                Mth.lerp(partialTicks, e.yo, e.getY()) + e.getBbHeight() * 0.5,
                                Mth.lerp(partialTicks, e.zo, e.getZ())
                            );
                        }
                    }
                } catch (Exception ignored) {}
            }
            return getLaserEnd(level, self, start, targetAim, maxRange);
        }

        private static Vec3 getBurstLaserEnd(Level level, Entity self, Vec3 start, Vec3 direction, double maxRange) {
            if (direction == null || direction.lengthSqr() < 0.001) {
                direction = self.getLookAngle();
                if (direction == null || direction.lengthSqr() < 0.001) {
                    direction = new Vec3(0, 0, 1);
                }
            }
            direction = direction.normalize();

            Vec3 end = start.add(direction.scale(maxRange));
            BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
            return blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        }

        private static Vec3 getLaserEnd(Level level, Entity self, Vec3 start, Vec3 direction, double maxRange) {
            if (direction == null || direction.lengthSqr() < 0.001) {
                direction = self.getLookAngle();
                if (direction == null || direction.lengthSqr() < 0.001) {
                    direction = new Vec3(0, 0, 1);
                }
            }
            direction = direction.normalize();

            Vec3 end = start.add(direction.scale(maxRange));
            BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
            Vec3 blockedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

            AABB searchBox = new AABB(start, blockedEnd);
            Vec3 targetEnd = blockedEnd;
            double closestDist = start.distanceToSqr(blockedEnd);

            for (Entity entity : level.getEntities(self, searchBox, e -> e instanceof LivingEntity && e.isPickable() && !isUntargetable(e) && e != self)) {
                AABB aabb = entity.getBoundingBox();
                Optional<Vec3> clip = aabb.clip(start, blockedEnd);
                if (clip.isPresent()) {
                    double dist = start.distanceToSqr(clip.get());
                    if (dist < closestDist) {
                        closestDist = dist;
                        targetEnd = clip.get();
                    }
                }
            }
            return targetEnd;
        }

        private static Vec3 rotateToward(Vec3 current, Vec3 desired, double maxRadians) {
            double dot = Mth.clamp(current.dot(desired), -1.0, 1.0);
            double angle = Math.acos(dot);
            if (angle <= maxRadians || angle < 0.0001) {
                return desired;
            }
            double blend = maxRadians / angle;
            return current.scale(1.0 - blend).add(desired.scale(blend)).normalize();
        }
    }

    @EventBusSubscriber
    public static class FractusLaserServerHandler {

        @SubscribeEvent
        public static void onEntityTick(EntityTickEvent.Pre event) {
            Entity entity = event.getEntity();
            if (entity == null || entity.level().isClientSide()) {
                return;
            }

            String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
            boolean isPrime = "fractus_prime".equals(path);
            if ("fractus".equals(path) || isPrime) {
                // Check telekinesis state
                boolean isTelekinesisServer = isPrime && (
                    entity.getPersistentData().getBoolean("telekinesis_active")
                    || entity.getPersistentData().getBoolean("is_telekinesis")
                    || entity.getPersistentData().getInt("fractus_telekinesis_timer") > 0
                );
                if (entity instanceof LivingEntity living) {
                    living.setStingerCount(isTelekinesisServer ? 1 : 0);
                }

                // Check summoning state
                int summonRole = entity.getPersistentData().getInt("fractus_summon_role");
                boolean isSummoning = summonRole > 0
                    || entity.getPersistentData().getBoolean("is_summoning")
                    || entity.getPersistentData().getInt("fractus_summon_ticks") > 0;

                // STRICT RULE: In summoning mode of standard Fractus, NEVER activate the combat laser!
                if (isSummoning && "fractus".equals(path)) {
                    entity.setShiftKeyDown(true);
                    entity.setSprinting(true);
                    entity.setSwimming(true);
                    if (entity instanceof LivingEntity living) {
                        int sTicks = entity.getPersistentData().getInt("fractus_summon_ticks");
                        living.setArrowCount(sTicks > 0 ? sTicks : 100);
                    }
                    return;
                }

                int burstTicks = entity.getPersistentData().getInt("fractus_burst_timer");
                int burstState = entity.getPersistentData().getInt("fractus_burst_state");
                if (burstState == 0) {
                    burstState = entity.getPersistentData().getInt("fractus_laser_state");
                }
                boolean isBurst = burstTicks > 0
                    || entity.getPersistentData().getBoolean("is_laser_burst_activating")
                    || entity.getPersistentData().getBoolean("is_laser_burst");

                int threshold = isPrime ? 485 : 23;
                boolean isBurstCharging = isBurst && (burstState == 2 || (burstTicks > threshold && burstState != 3 && burstState != 4));
                boolean isBurstFiring = isBurst && (burstState == 3 || burstState == 4 || (burstTicks > 0 && burstTicks <= threshold));

                int state = entity.getPersistentData().getInt("fractus_laser_state");

                if (isBurstCharging || state == 2) { // charging
                    entity.setShiftKeyDown(true);
                    entity.setSprinting(false);
                } else if (isBurstFiring || state == 3) { // firing
                    entity.setShiftKeyDown(false);
                    entity.setSprinting(true);
                } else { // idle, tracking, cooldown
                    entity.setShiftKeyDown(false);
                    entity.setSprinting(false);
                }

                Vec3 activeAim = null;
                if (isBurst) {
                    double bx = entity.getPersistentData().getDouble("fractus_burst_aim_x");
                    double by = entity.getPersistentData().getDouble("fractus_burst_aim_y");
                    double bz = entity.getPersistentData().getDouble("fractus_burst_aim_z");
                    Vec3 b = new Vec3(bx, by, bz);
                    if (b.lengthSqr() > 0.001) activeAim = b.normalize();
                } else if (entity.getPersistentData().getInt("fractus_destroying_fire") > 0) {
                    double dx = entity.getPersistentData().getDouble("fractus_destroying_aim_x");
                    double dy = entity.getPersistentData().getDouble("fractus_destroying_aim_y");
                    double dz = entity.getPersistentData().getDouble("fractus_destroying_aim_z");
                    Vec3 d = new Vec3(dx, dy, dz);
                    if (d.lengthSqr() > 0.001) activeAim = d.normalize();
                } else {
                    String kx = entity.getPersistentData().contains("fractus_laser_aim_x") ? "fractus_laser_aim_x" : "fractus_aim_x";
                    String ky = entity.getPersistentData().contains("fractus_laser_aim_y") ? "fractus_laser_aim_y" : "fractus_aim_y";
                    String kz = entity.getPersistentData().contains("fractus_laser_aim_z") ? "fractus_laser_aim_z" : "fractus_aim_z";
                    double ax = entity.getPersistentData().getDouble(kx);
                    double ay = entity.getPersistentData().getDouble(ky);
                    double az = entity.getPersistentData().getDouble(kz);
                    Vec3 a = new Vec3(ax, ay, az);
                    if (a.lengthSqr() > 0.001) activeAim = a.normalize();
                }

                if (activeAim != null) {
                    float pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(activeAim.y, -1.0, 1.0)));
                    float yaw = (float) Math.toDegrees(Math.atan2(-activeAim.x, activeAim.z));
                    entity.setXRot(pitch);
                    entity.setYRot(yaw);
                    if (entity instanceof LivingEntity living) {
                        living.yHeadRot = yaw;
                        living.yBodyRot = yaw;
                    }
                }

                // Synchronize sphere burst timer to client via arrow count
                if ("fractus_prime".equals(path) && entity instanceof LivingEntity living) {
                    int sphereTicks = entity.getPersistentData().getInt("fractus_sphere_timer");
                    living.setArrowCount(sphereTicks);
                    entity.getPersistentData().putInt("sphere_ticks", sphereTicks);
                }
            }
        }

        @SubscribeEvent
        public static void onEntityTickPost(EntityTickEvent.Post event) {
            Entity entity = event.getEntity();
            if (entity == null || entity.level().isClientSide()) return;

            ResourceLocation typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (typeKey == null || !"the_backwoods".equals(typeKey.getNamespace())) return;

            String path = typeKey.getPath();
            boolean isPrime = "fractus_prime".equals(path);
            if (!"fractus".equals(path) && !isPrime) return;

            int burstTicks = entity.getPersistentData().getInt("fractus_burst_timer");
            boolean isBurst = burstTicks > 0
                || entity.getPersistentData().getBoolean("is_laser_burst_activating")
                || entity.getPersistentData().getBoolean("is_laser_burst");

            Vec3 start;
            if (entity instanceof LivingEntity living) {
                start = living.getEyePosition().subtract(0.0, living.getBbHeight() * 0.18, 0.0);
            } else {
                start = entity.position().add(0.0, entity.getBbHeight() * 0.65, 0.0);
            }

            if (isPrime && entity.getPersistentData().getBoolean("telekinesis_active")
                    && entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                String targetUuid = entity.getPersistentData().getString("telekinesis_target_uuid");
                if (!targetUuid.isEmpty()) {
                    try {
                        Entity telekinesisTarget = serverLevel.getEntity(java.util.UUID.fromString(targetUuid));
                        if (telekinesisTarget instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                            sendLaserImpact(entity, FractusLaserClientRenderer.IMPACT_TELEKINESIS, 0, livingTarget.getEyePosition());
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            if (isBurst) {
                Vec3 aim = readAim(entity, "fractus_burst_aim_x", "fractus_burst_aim_y", "fractus_burst_aim_z");
                if (aim != null) {
                    Vec3 impact = clipBlockImpact(entity, start, aim, isPrime ? 256.0 : 128.0);
                    int burstPhase = entity.getPersistentData().getInt("fractus_burst_state");
                    if (burstPhase == 0) {
                        burstPhase = entity.getPersistentData().getInt("fractus_laser_state");
                    }
                    sendLaserImpact(entity, FractusLaserClientRenderer.IMPACT_BURST, burstPhase, impact);
                }
                return;
            }

            if (entity.getPersistentData().getInt("fractus_destroying_fire") > 0) {
                Vec3 aim = readAim(entity, "fractus_destroying_aim_x", "fractus_destroying_aim_y", "fractus_destroying_aim_z");
                if (aim != null) {
                    Vec3 impact = clipBlockImpact(entity, start, aim, 52.0);
                    sendLaserImpact(entity, FractusLaserClientRenderer.IMPACT_DESTROYING, 0, impact);
                }
                return;
            }

            int laserState = entity.getPersistentData().getInt("fractus_laser_state");
            boolean regularLaserActive = laserState == 2 || laserState == 3
                || entity.getPersistentData().getInt("fractus_laser_charge") > 0
                || entity.getPersistentData().getInt("fractus_laser_fire") > 0;
            if (!regularLaserActive) return;

            boolean angry = entity instanceof LivingEntity living
                && living.getHealth() <= (isPrime ? 100.0f : 20.0f);
            double range = isPrime ? (angry ? 80.0 : 64.0) : (angry ? 44.0 : 32.0);
            int count = isPrime
                ? Mth.clamp(entity.getPersistentData().getInt("fractus_laser_aim_count"), 1, 5)
                : 1;

            for (int i = 0; i < count; i++) {
                String suffix = i == 0 ? "" : "_" + i;
                Vec3 aim = readAim(entity,
                    "fractus_laser_aim_x" + suffix,
                    "fractus_laser_aim_y" + suffix,
                    "fractus_laser_aim_z" + suffix
                );
                if (aim == null) continue;

                Vec3 impact = FractusLaserClientRenderer.getLaserEnd(entity.level(), entity, start, aim, range);
                sendLaserImpact(entity, FractusLaserClientRenderer.IMPACT_REGULAR_BASE + i, count, impact);
            }
        }

        private static Vec3 readAim(Entity entity, String xKey, String yKey, String zKey) {
            Vec3 aim = new Vec3(
                entity.getPersistentData().getDouble(xKey),
                entity.getPersistentData().getDouble(yKey),
                entity.getPersistentData().getDouble(zKey)
            );
            return aim.lengthSqr() < 0.001 ? null : aim.normalize();
        }

        private static Vec3 clipBlockImpact(Entity entity, Vec3 start, Vec3 aim, double range) {
            Vec3 rawEnd = start.add(aim.scale(range));
            BlockHitResult blockHit = entity.level().clip(new ClipContext(
                start, rawEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity
            ));
            return blockHit.getType() == HitResult.Type.MISS ? rawEnd : blockHit.getLocation();
        }

        private static void sendLaserImpact(Entity entity, int channel, int regularCount, Vec3 impact) {
            int route = (Mth.clamp(regularCount, 0, 5) << 8) | (channel & 0xff);
            PacketDistributor.sendToAllPlayers(createLaserImpactPayload(entity.getId(), route, impact.x, impact.y, impact.z));
        }
    }

    @EventBusSubscriber(modid = "the_backwoods")
    public static class LaserImpactNetworkRegistration {
        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(
                LaserImpactPayload.TYPE,
                LaserImpactPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> FractusLaserClientRenderer.receiveLaserImpact(payload))
            );
        }
    }

    public interface LaserImpactPayload extends CustomPacketPayload {
        public static final CustomPacketPayload.Type<LaserImpactPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("the_backwoods", "fractus_laser_impact")
        );
        public static final StreamCodec<ByteBuf, LaserImpactPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LaserImpactPayload::entityId,
            ByteBufCodecs.VAR_INT, LaserImpactPayload::route,
            ByteBufCodecs.DOUBLE, LaserImpactPayload::x,
            ByteBufCodecs.DOUBLE, LaserImpactPayload::y,
            ByteBufCodecs.DOUBLE, LaserImpactPayload::z,
            FractusLaserBeam::createLaserImpactPayload
        );

        int entityId();

        int route();

        double x();

        double y();

        double z();
    }

    private static LaserImpactPayload createLaserImpactPayload(int entityId, int route, double x, double y, double z) {
        return new LaserImpactPayload() {
            @Override
            public CustomPacketPayload.Type<LaserImpactPayload> type() {
                return LaserImpactPayload.TYPE;
            }

            @Override
            public int entityId() {
                return entityId;
            }

            @Override
            public int route() {
                return route;
            }

            @Override
            public double x() {
                return x;
            }

            @Override
            public double y() {
                return y;
            }

            @Override
            public double z() {
                return z;
            }
        };
    }
}

// 1.21.1

