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
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import org.joml.Quaternionf;

import java.util.Optional;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class FractusLaserBeam {

    // Configurable offsets to align the laser beam with the entity's model center.
    // Users can easily customize these to shift the laser beam's starting point:
    // OFFSET_X: Positive moves the laser right, negative moves left (relative to entity's head/body yaw facing direction)
    // OFFSET_Y: Positive moves the laser up, negative moves down
    // OFFSET_Z: Positive moves the laser forward, negative moves backward
    public static double OFFSET_X = 0.0;
    public static double OFFSET_Y = 0.0;
    public static double OFFSET_Z = 0.0;

    // Set to true to bring back the original particle-based laser beam instead of the beacon beam
    public static boolean USE_OLD_LASER_PARTICLES = true;

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
        private static final ResourceLocation BEACON_BEAM_LOCATION = ResourceLocation.parse("minecraft:textures/entity/beacon_beam.png");

        @SubscribeEvent
        public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
            if (USE_OLD_LASER_PARTICLES) {
                return;
            }
            LivingEntity entity = event.getEntity();
            if (entity == null || !isFractus(entity)) {
                return;
            }

            // Synchronized states: firing (main laser) via sprinting
            boolean firing = entity.isSprinting();

            if (!firing) {
                return;
            }

            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource bufferSource = event.getMultiBufferSource();
            float partialTicks = event.getPartialTick();

            // Smooth positions for camera movement
            double entityX = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double entityY = Mth.lerp(partialTicks, entity.yo, entity.getY());
            double entityZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());

            Vec3 startPos = laserStartSmooth(entity, partialTicks);
            Vec3 dirVec = getLookVector(entity, partialTicks);
            Vec3 endPos = getLaserEnd(entity.level(), entity, startPos, dirVec, isAngry(entity) ? ANGRY_LASER_RANGE : LASER_RANGE);

            poseStack.pushPose();

            // Relative transformation in rendering space
            double renderStartX = startPos.x - entityX;
            double renderStartY = startPos.y - entityY;
            double renderStartZ = startPos.z - entityZ;

            poseStack.translate(renderStartX, renderStartY, renderStartZ);

            Vec3 dir = endPos.subtract(startPos);
            double length = dir.length();
            if (length > 0.01) {
                Vec3 dirNorm = dir.normalize();
                Quaternionf rot = new Quaternionf().rotationTo(0, 1, 0, (float)dirNorm.x, (float)dirNorm.y, (float)dirNorm.z);
                poseStack.mulPose(rot);

                boolean angry = isAngry(entity);
                int packedColor;
                float innerRadius;
                float outerRadius;

                // Firing laser: orange if normal, bright red if angry
                float[] floatColor = angry ? new float[]{1.0f, 0.05f, 0.0f} : new float[]{1.0f, 0.41f, 0.0f};
                int r = (int) (floatColor[0] * 255.0f);
                int g = (int) (floatColor[1] * 255.0f);
                int b = (int) (floatColor[2] * 255.0f);
                packedColor = (255 << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);

                boolean isPrime = "fractus_prime".equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
                long gameTime = entity.level().getGameTime();

                int height = (int) Math.ceil(length);
                if (height < 1) height = 1;
                float scaleY = (float) (length / (double) height);
                poseStack.scale(1.0f, scaleY, 1.0f);

                // Cancel out BeaconRenderer's internal hardcoded .translate(0.5D, 0.0D, 0.5D)
                poseStack.translate(-0.5D, 0.0D, -0.5D);

                if (isPrime) {
                    // RAMIEL'S BEAM: Dual Layer/Multi-Pass
                    // Layer 1: High-intensity Violet/Deep-indigo Plasma Corona/Halo (size doubled)
                    float sizeMultiplier = 2.0f;
                    float shimmer = (float) Math.sin((gameTime + partialTicks) * 1.5f) * 0.03f;
                    
                    int coronaColor;
                    if (angry) {
                        coronaColor = (255 << 24) | (230 << 16) | (10 << 8) | 200;
                    } else {
                        coronaColor = (255 << 24) | (100 << 16) | (40 << 8) | 255;
                    }
                    
                    float coronaInner = (0.35f + shimmer) * sizeMultiplier;
                    float coronaOuter = (0.55f + shimmer) * sizeMultiplier;
                    if (coronaInner < 0.1f) coronaInner = 0.1f;
                    if (coronaOuter < 0.2f) coronaOuter = 0.2f;

                    BeaconRenderer.renderBeaconBeam(
                        poseStack,
                        bufferSource,
                        BEACON_BEAM_LOCATION,
                        partialTicks,
                        1.0f,
                        gameTime,
                        0,
                        height,
                        coronaColor,
                        coronaInner,
                        coronaOuter
                    );

                    // Layer 2: White-Hot Shining Core (Pure intense white Core, size doubled)
                    int coreColor = (255 << 24) | (255 << 16) | (255 << 8) | 255;
                    float coreInner = (0.12f + shimmer * 0.5f) * sizeMultiplier;
                    float coreOuter = (0.18f + shimmer * 0.5f) * sizeMultiplier;
                    if (coreInner < 0.05f) coreInner = 0.05f;
                    if (coreOuter < 0.08f) coreOuter = 0.08f;

                    BeaconRenderer.renderBeaconBeam(
                        poseStack,
                        bufferSource,
                        BEACON_BEAM_LOCATION,
                        partialTicks,
                        1.0f,
                        gameTime,
                        0,
                        height,
                        coreColor,
                        coreInner,
                        coreOuter
                    );
                } else {
                    // REGULAR FRACTUS: Design 2 "The Pulse-Surge Pillar" (Dynamic Harmonic Swelling)
                    float swell = (float) Math.sin((gameTime + partialTicks) * 0.35f);
                    float scale = 1.0f + swell * 0.15f; // Modulates between 0.85x and 1.15x
                    
                    innerRadius = (angry ? 0.15f : 0.12f) * scale;
                    outerRadius = (angry ? 0.22f : 0.1875f) * scale;

                    BeaconRenderer.renderBeaconBeam(
                        poseStack,
                        bufferSource,
                        BEACON_BEAM_LOCATION,
                        partialTicks,
                        1.0f,
                        gameTime,
                        0,
                        height,
                        packedColor,
                        innerRadius,
                        outerRadius
                    );
                }
            }

            poseStack.popPose();
        }

        private static boolean isFractus(Entity entity) {
            if (entity == null) {
                return false;
            }
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
            return "fractus".equals(path) || "fractus_prime".equals(path);
        }

        private static boolean isAngry(Entity entity) {
            return entity instanceof LivingEntity living && living.getHealth() <= 20.0f;
        }

        private static Vec3 getLookVector(LivingEntity entity, float partialTicks) {
            float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());

            float f = pitch * ((float)Math.PI / 180F);
            float f1 = -yaw * ((float)Math.PI / 180F);
            float f2 = Mth.cos(f1);
            float f3 = Mth.sin(f1);
            float f4 = Mth.cos(f);
            float f5 = Mth.sin(f);
            return new Vec3((double)(f3 * f4), (double)(-f5), (double)(f2 * f4));
        }

        private static Vec3 getSmoothEyePosition(Entity entity, float partialTicks) {
            double x = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double y = Mth.lerp(partialTicks, entity.yo, entity.getY());
            double z = Mth.lerp(partialTicks, entity.zo, entity.getZ());
            return new Vec3(x, y + entity.getEyeHeight(), z);
        }

        private static Vec3 laserStartSmooth(Entity entity, float partialTicks) {
            double x = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double y = Mth.lerp(partialTicks, entity.yo, entity.getY());
            double z = Mth.lerp(partialTicks, entity.zo, entity.getZ());
            Vec3 base = new Vec3(x, y + (double)entity.getBbHeight() * 0.5, z);

            float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
            float yawRad = -yaw * ((float)Math.PI / 180F);
            float cosYaw = Mth.cos(yawRad);
            float sinYaw = Mth.sin(yawRad);

            double rx = OFFSET_X * cosYaw - OFFSET_Z * sinYaw;
            double rz = OFFSET_X * sinYaw + OFFSET_Z * cosYaw;

            return base.add(rx, OFFSET_Y, rz);
        }

        private static Vec3 getLaserEnd(Level level, Entity self, Vec3 start, Vec3 direction, double maxRange) {
            Vec3 end = start.add(direction.scale(maxRange));
            BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
            Vec3 blockedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

            AABB searchBox = self.getBoundingBox().inflate(maxRange);
            Vec3 targetEnd = blockedEnd;
            double closestDist = start.distanceToSqr(blockedEnd);

            for (Entity entity : level.getEntities(self, searchBox, e -> e instanceof LivingEntity && e.isPickable() && e.isAlive() && e != self)) {
                AABB aabb = entity.getBoundingBox().inflate(0.3);
                Optional<Vec3> clip = aabb.clip(start, end);
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
    }

    @EventBusSubscriber
    public static class FractusLaserServerHandler {

        @SubscribeEvent
        public static void onEntityTick(EntityTickEvent.Pre event) {
            Entity entity = event.getEntity();
            if (entity == null || entity.level().isClientSide()) {
                return;
            }

            // Identify if this tick belongs to a Fractus entity
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
            if ("fractus".equals(path) || "fractus_prime".equals(path)) {
                // Read server-side laser state and synchronize it to client using EntityData sneak/sprint flags
                int state = entity.getPersistentData().getInt("fractus_laser_state");
                if (state == 2) { // charging
                    entity.setShiftKeyDown(true);
                    entity.setSprinting(false);
                } else if (state == 3) { // firing
                    entity.setShiftKeyDown(false);
                    entity.setSprinting(true);
                } else { // idle, tracking, cooldown
                    entity.setShiftKeyDown(false);
                    entity.setSprinting(false);
                }
            }
        }
    }
}