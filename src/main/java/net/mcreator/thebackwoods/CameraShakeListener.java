package net.mcreator.thebackwoods;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

@SuppressWarnings({"deprecation", "removal"})
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CameraShakeListener {
    public static float VERDANT_CAMERA_SHAKE_STRENGTH = 1.0F;
    public static float ROT_CAMERA_SHAKE_STRENGTH = 0.15F;

    public CameraShakeListener() {}

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {}

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void clientLoad(FMLClientSetupEvent event) {}

    @SuppressWarnings({"deprecation", "removal"})
    @EventBusSubscriber(value = Dist.CLIENT)
    public static class ClientEvents {
        private static int shakeDuration = 0, totalDuration = 0;
        private static float shakeIntensity = 0.0F;

        public static void triggerShake(int duration, float intensity) {
            if (duration > 0 && intensity > 0.0F) {
                shakeDuration = duration;
                totalDuration = duration;
                shakeIntensity = intensity;
            }
        }

        public static void triggerShakeForRot(int duration, float intensity) {
            triggerShake(duration, intensity * ROT_CAMERA_SHAKE_STRENGTH);
        }

        public static void triggerShakeForVerdant(int duration, float intensity) {
            triggerShake(duration, intensity * VERDANT_CAMERA_SHAKE_STRENGTH);
        }

        @SubscribeEvent
        public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
            if (event.getEntity().level().isClientSide() && event.getEntity() == net.minecraft.client.Minecraft.getInstance().player) {
                if (shakeDuration > 0) shakeDuration--;
            }
        }

        @SubscribeEvent
        public static void onComputeCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
            if (shakeDuration > 0 && totalDuration > 0) {
                float intensity = shakeIntensity * ((float) shakeDuration / (float) totalDuration);
                long t = System.currentTimeMillis();
                event.setPitch(event.getPitch() + (float) (Math.sin(t * 0.05) * intensity * 1.1F));
                event.setYaw(event.getYaw() + (float) (Math.cos(t * 0.05) * intensity * 1.1F));
                event.setRoll(event.getRoll() + (float) (Math.sin(t * 0.03) * intensity * 0.5F));
            }
        }

        @SubscribeEvent
        public static void onPlaySound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null || event.getSound() == null) return;

            String name = event.getName() != null ? event.getName().toLowerCase(java.util.Locale.ROOT) : "";
            String soundLoc = (event.getSound().getLocation() != null) ? event.getSound().getLocation().toString().toLowerCase(java.util.Locale.ROOT) : "";
            String soundPath = (event.getSound().getLocation() != null) ? event.getSound().getLocation().getPath().toLowerCase(java.util.Locale.ROOT) : "";

            boolean matchesSound = name.contains("sonic_boom") || name.contains("explode") || name.contains("smash_ground_heavy") || name.contains("lightning_bolt.impact")
                    || soundLoc.contains("sonic_boom") || soundLoc.contains("explode") || soundLoc.contains("smash_ground_heavy") || soundLoc.contains("lightning_bolt.impact")
                    || soundPath.contains("sonic_boom") || soundPath.contains("explode") || soundPath.contains("smash_ground_heavy") || soundPath.contains("lightning_bolt.impact");

            if (matchesSound) {
                try {
                    double sx = event.getSound().getX(), sy = event.getSound().getY(), sz = event.getSound().getZ();

                    float multiplier = getEntityShakeMultiplierAt(player, sx, sy, sz);
                    if (multiplier <= 0.0F) return;

                    double distSq = player.distanceToSqr(sx, sy, sz);
                    double maxRadius = 48.0; // 48-block sphere radius at beam tip
                    double maxDistSq = maxRadius * maxRadius;
                    if (distSq <= maxDistSq) {
                        float dist = (float) Math.sqrt(distSq);
                        // Linear decay over 48 sphere radius
                        float factor = Math.max(0.0F, 1.0F - (dist / 48.0F));
                        float intensity = 2.2F * factor * multiplier;
                        int duration = (int) (50 * factor);
                        if (duration > 0 && intensity > 0.02F) {
                            triggerShake(duration, intensity);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        private static float getEntityShakeMultiplierAt(net.minecraft.client.player.LocalPlayer player, double sx, double sy, double sz) {
            if (player == null || player.level() == null) return 0.0F;

            // Search within column around the sound origin for Verdant Engine or Rot
            net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(sx - 48.0, sy - 64.0, sz - 48.0, sx + 48.0, sy + 96.0, sz + 48.0);
            java.util.List<net.minecraft.world.entity.Entity> nearbyEntities = player.level().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, searchBox, e -> e != null && e.isAlive());

            for (net.minecraft.world.entity.Entity e : nearbyEntities) {
                String cls = e.getClass().getName().toLowerCase(java.util.Locale.ROOT);
                String reg = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().toLowerCase(java.util.Locale.ROOT);
                String desc = e.getType().getDescriptionId().toLowerCase(java.util.Locale.ROOT);

                // 1. Check Verdant Engine: Sound must be directly aligned with Verdant's vertical beam column (< 5 blocks horizontal distance)
                if (reg.contains("verdant_engine") || cls.contains("verdantengine") || reg.contains("verdant") || cls.contains("verdant") || desc.contains("verdant")) {
                    double horizDistSq = (sx - e.getX()) * (sx - e.getX()) + (sz - e.getZ()) * (sz - e.getZ());
                    if (horizDistSq <= 5.0 * 5.0) {
                        return VERDANT_CAMERA_SHAKE_STRENGTH;
                    }
                }

                // 2. Check Rot: Sound must originate within close proximity (< 12 blocks) of the Rot entity
                if (reg.contains("rot") || cls.contains("rotentity") || cls.contains(".rot") || desc.contains("rot")) {
                    double distToRotSq = e.distanceToSqr(sx, sy, sz);
                    if (distToRotSq <= 12.0 * 12.0) {
                        return ROT_CAMERA_SHAKE_STRENGTH;
                    }
                }
            }

            return 0.0F;
        }
    }
} // 1.21.1
