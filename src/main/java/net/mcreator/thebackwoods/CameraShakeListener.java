package net.mcreator.thebackwoods;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CameraShakeListener {

    public CameraShakeListener() {
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        // Core initialization setup
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void clientLoad(FMLClientSetupEvent event) {
        // Client-side initialization
    }

    // Client-only events nested to prevent classloading crashes on dedicated servers
    @EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class ClientEvents {
        private static int shakeDuration = 0;
        private static int totalDuration = 0;
        private static float shakeIntensity = 0.0F;

        // Triggers camera shake variables
        public static void triggerShake(int duration, float intensity) {
            if (duration > 0) {
                shakeDuration = duration;
                totalDuration = duration;
                shakeIntensity = intensity;
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
            if (event.getEntity().level().isClientSide() && event.getEntity() == net.minecraft.client.Minecraft.getInstance().player) {
                if (shakeDuration > 0) {
                    shakeDuration--;
                }
            }
        }

        @SubscribeEvent
        public static void onComputeCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
            if (shakeDuration > 0) {
                float progress = (float) shakeDuration / (float) totalDuration;
                // Fade out shake intensity over time
                float currentIntensity = shakeIntensity * progress;
                
                // Mathematical sine oscillation for clean shaking
                long time = System.currentTimeMillis();
                float pitchOffset = (float) (Math.sin(time * 0.05) * currentIntensity * 1.1F);
                float yawOffset = (float) (Math.cos(time * 0.05) * currentIntensity * 1.1F);
                float rollOffset = (float) (Math.sin(time * 0.03) * currentIntensity * 0.5F);

                event.setPitch(event.getPitch() + pitchOffset);
                event.setYaw(event.getYaw() + yawOffset);
                event.setRoll(event.getRoll() + rollOffset);
            }
        }

        // Smart Sound Interceptor: Detects the Rot's Crush impact sound & triggers camera shake dynamically based on distance!
        @SubscribeEvent
        public static void onPlaySound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && event.getSound() != null) {
                String soundName = event.getName();

                // Intercept the Warden Sonic Boom or Generic Explode played under the Rot's unique Pitch context
                if ("entity.warden.sonic_boom".equals(soundName) || "entity.generic.explode".equals(soundName)) {
                    double soundX = event.getSound().getX();
                    double soundY = event.getSound().getY();
                    double soundZ = event.getSound().getZ();
                    
                    double distSq = player.distanceToSqr(soundX, soundY, soundZ);
                    if (distSq < 400.0) { // Apply screen shake if the landing impact occurred within 20 blocks
                        float distanceFactor = 1.0F - (float) (Math.sqrt(distSq) / 20.0);
                        if (distanceFactor > 0.0F) {
                            // Scale intensity based on proximity to the landing epicentre
                            triggerShake(15, distanceFactor * 0.5F);
                        }
                    }
                }
            }
        }
    }
} // 1.21.1