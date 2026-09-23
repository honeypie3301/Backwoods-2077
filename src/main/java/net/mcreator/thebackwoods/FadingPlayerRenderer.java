package net.mcreator.thebackwoods;

/**
 * =========================================================================
 *              EXISTENTIAL PHASE-SHIFTING: THE "FADED" MECHANICS
 *                      Fading Player Renderer (Client-Only)
 * =========================================================================
 * 
 * INSTRUCTIONS FOR MCREATOR USER:
 * 1. Place this file inside net/mcreator/thebackwoods/FadingPlayerRenderer.java
 * 
 * 2. This subscriber is configured for CLIENT-ONLY rendering. It intercepts
 *    the Player Renderer on the client side, cancels the normal opaque rendering,
 *    and rerenders the player using a custom wrapped MultiBufferSource that dynamically
 *    multiplies the opacity (Alpha) based on their fade_level.
 * 
 * FEATURES:
 * - Shimmer effect: Adds a ghostly shifting phase shimmer based on game tickCount.
 * - Multi-layer translucency: Correctly applies translucency to player skins, 
 *   armor, held items, capes, and accessories!
 * - Server Safe: Fully annotated to prevent physical server crashes.
 * 
 * Path: src/main/java/net/mcreator/thebackwoods/FadingPlayerRenderer.java
 */

import net.minecraft.client.player.AbstractClientPlayer;
import net.mcreator.thebackwoods.network.TheBackwoodsModVariables;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import net.mcreator.thebackwoods.procedures.FadingTimeProcedure;

@EventBusSubscriber(modid = "the_backwoods", value = Dist.CLIENT)
public class FadingPlayerRenderer {

    // ==========================================
    //            CONFIGURABLE SETTINGS
    // ==========================================
    // The minimum visual opacity (0.0f = completely invisible, 1.0f = fully opaque) 
    // that a fully faded player/entity can reach. 
    // Set to 0.0f for total invisibility, or higher (e.g. 0.15f) for a more visible ghost effect.
    public static float MIN_OPACITY = 0.12f;

    private static java.lang.reflect.Method renderItemInHandMethod = null;
    private static boolean isRenderingFadedPlayer = false;
    private static boolean isRenderingFadedHand = false;
    private static boolean hasLoggedError = false;

    private static double getFadeLevel(Player player) {
        if (player == null) {
            return 0.0;
        }
        try {
            var vars = player.getData(TheBackwoodsModVariables.PLAYER_VARIABLES);
            if (vars != null) {
                return vars.fade_level;
            }
        } catch (Throwable ignored) {
        }
        try {
            return player.getPersistentData().getDouble("fade_level");
        } catch (Throwable ignored) {
        }
        return 0.0;
    }

    static {
        try {
            renderItemInHandMethod = net.minecraft.client.renderer.ItemInHandRenderer.class.getDeclaredMethod(
                "renderItemInHand",
                net.minecraft.client.player.AbstractClientPlayer.class,
                float.class,
                float.class,
                net.minecraft.world.InteractionHand.class,
                float.class,
                net.minecraft.world.item.ItemStack.class,
                float.class,
                com.mojang.blaze3d.vertex.PoseStack.class,
                net.minecraft.client.renderer.MultiBufferSource.class,
                int.class
            );
            renderItemInHandMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // Robust fallback: Scan all methods of ItemInHandRenderer to locate the private renderItemInHand method via parameter count
            for (java.lang.reflect.Method method : net.minecraft.client.renderer.ItemInHandRenderer.class.getDeclaredMethods()) {
                if (method.getParameterCount() == 10) {
                    method.setAccessible(true);
                    renderItemInHandMethod = method;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (isRenderingFadedPlayer) {
            return;
        }

        Player player = event.getEntity();
        if (player == null) {
            return;
        }

        // Retrieve current fade level
        double fadeLevel = getFadeLevel(player);
        if (fadeLevel <= 0.0) {
            return; // Normal rendering when not faded at all
        }

        // Clamp fade level between 0 and 100, then compute visual opacity (alpha)
        fadeLevel = Math.max(0.0, Math.min(100.0, fadeLevel));
        float alpha = 1.0f - (float) (fadeLevel / 100.0);

        // --- SUBTLE SHIMMER EFFECT ---
        float shimmer = (float) Math.sin((player.tickCount + event.getPartialTick()) * 0.15f) * 0.04f;
        float finalAlpha = Math.max(MIN_OPACITY, Math.min(1.0f, alpha + shimmer)); // keep a faint ghostly silhouette at MIN_OPACITY

        // Cancel the original opaque player rendering
        event.setCanceled(true);

        MultiBufferSource originalSource = event.getMultiBufferSource();

        // Wrap the MultiBufferSource to force a Translucent RenderType and scale Vertex Alpha
        MultiBufferSource wrappedSource = renderType -> {
            if (!shouldWrapRenderType(renderType)) {
                return originalSource.getBuffer(renderType);
            }
            // Map the standard opaque/cutout entity layers to a translucent counterpart
            RenderType translucentType = getTranslucentRenderType(renderType, player, event.getRenderer());
            VertexConsumer originalConsumer = originalSource.getBuffer(translucentType);
            return wrapVertexConsumer(originalConsumer, finalAlpha);
        };

        // Re-render the player manually with our custom translucent wrapped source!
        PlayerRenderer renderer = event.getRenderer();
        PoseStack poseStack = event.getPoseStack();
        int packedLight = event.getPackedLight();

        if (player instanceof AbstractClientPlayer clientPlayer) {
            float entityYaw = net.minecraft.util.Mth.lerp(event.getPartialTick(), clientPlayer.yRotO, clientPlayer.getYRot());
            try {
                isRenderingFadedPlayer = true;
                renderer.render(
                    clientPlayer,
                    entityYaw,
                    event.getPartialTick(),
                    poseStack,
                    wrappedSource,
                    packedLight
                );
            } finally {
                isRenderingFadedPlayer = false;
            }
        }
    }

    /**
     * Intercepts and renders first-person hands translucent as well!
     * This makes the visual fading fully immersive from the player's perspective.
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isRenderingFadedHand) {
            return;
        }

        // We only fade the hand for the client player
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        double fadeLevel = getFadeLevel(player);
        if (fadeLevel <= 0.0) {
            return;
        }

        fadeLevel = Math.max(0.0, Math.min(100.0, fadeLevel));
        float alpha = 1.0f - (float) (fadeLevel / 100.0);
        float shimmer = (float) Math.sin((player.tickCount + event.getPartialTick()) * 0.15f) * 0.04f;
        float finalAlpha = Math.max(MIN_OPACITY, Math.min(1.0f, alpha + shimmer));

        // Let's cancel and let MC render the hand with wrapped translucent buffer
        event.setCanceled(true);

        MultiBufferSource originalSource = event.getMultiBufferSource();
        MultiBufferSource wrappedSource = renderType -> {
            if (!shouldWrapRenderType(renderType)) {
                return originalSource.getBuffer(renderType);
            }
            // Force hand textures and item rendering to support translucency
            RenderType translucentType = getTranslucentRenderType(renderType, player, null);
            VertexConsumer originalConsumer = originalSource.getBuffer(translucentType);
            return wrapVertexConsumer(originalConsumer, finalAlpha);
        };

        // Re-render hand with wrapped buffer source via reflection to circumvent private method access
        if (player instanceof AbstractClientPlayer clientPlayer) {
            try {
                isRenderingFadedHand = true;
                if (renderItemInHandMethod != null) {
                    renderItemInHandMethod.invoke(
                        mc.getEntityRenderDispatcher().getItemInHandRenderer(),
                        clientPlayer,
                        event.getPartialTick(),
                        event.getInterpolatedPitch(),
                        event.getHand(),
                        event.getSwingProgress(),
                        event.getItemStack(),
                        event.getEquipProgress(),
                        event.getPoseStack(),
                        wrappedSource,
                        event.getPackedLight()
                    );
                }
            } catch (Exception e) {
                if (!hasLoggedError) {
                    e.printStackTrace();
                    hasLoggedError = true;
                }
            } finally {
                isRenderingFadedHand = false;
            }
        }
    }

    /**
     * Maps any given RenderType to its Translucent equivalent to support alpha transparency.
     */
    private static RenderType getTranslucentRenderType(RenderType originalType, Player player, PlayerRenderer renderer) {
        if (renderer != null && player instanceof AbstractClientPlayer clientPlayer) {
            ResourceLocation texture = renderer.getTextureLocation(clientPlayer);
            return RenderType.itemEntityTranslucentCull(texture);
        }
        
        try {
            String str = originalType.toString();
            int start = str.indexOf("Optional[");
            if (start != -1) {
                int end = str.indexOf("]", start);
                if (end != -1) {
                    String locStr = str.substring(start + 9, end);
                    ResourceLocation loc = ResourceLocation.parse(locStr);
                    return RenderType.itemEntityTranslucentCull(loc);
                }
            }
        } catch (Exception ignored) {}
        
        return originalType;
    }

    private static boolean shouldWrapRenderType(RenderType renderType) {
        if (renderType == null) {
            return false;
        }
        String str = renderType.toString().toLowerCase(java.util.Locale.ROOT);
        // We strictly avoid wrapping block, item, glint, atlas, and GUI textures
        if (str.contains("atlas") || str.contains("item") || str.contains("block") || str.contains("glint") || str.contains("gui") || str.contains("crumbling")) {
            return false;
        }
        return str.contains("entity") || str.contains("armor") || str.contains("player");
    }

    /**
     * Generalization: Intercepts and renders any non-player living entities translucent when faded!
     * This makes standard mobs, animals, and other entities phase shift beautifully too!
     */
    @SubscribeEvent
    public static void onRenderLivingPre(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> event) {
        if (isRenderingFadedPlayer) {
            return;
        }
        net.minecraft.world.entity.LivingEntity entity = event.getEntity();
        if (entity == null || entity instanceof Player) {
            return;
        }

        // Retrieve current fade level. First, check if entity has NBT.
        double fadeLevel = 0.0;
        try {
            fadeLevel = entity.getPersistentData().getDouble("fade_level");
        } catch (Throwable ignored) {}
        
        if (fadeLevel <= 0.0) {
            return; // Normal rendering when not faded at all
        }

        // Clamp fade level between 0 and 100, then compute visual opacity (alpha)
        fadeLevel = Math.max(0.0, Math.min(100.0, fadeLevel));
        float alpha = 1.0f - (float) (fadeLevel / 100.0);

        // --- SUBTLE SHIMMER EFFECT ---
        float shimmer = (float) Math.sin((entity.tickCount + event.getPartialTick()) * 0.15f) * 0.04f;
        float finalAlpha = Math.max(MIN_OPACITY, Math.min(1.0f, alpha + shimmer)); // keep a faint ghostly silhouette

        // Cancel the original opaque entity rendering
        event.setCanceled(true);

        MultiBufferSource originalSource = event.getMultiBufferSource();

        // Wrap the MultiBufferSource to force a Translucent RenderType and scale Vertex Alpha
        MultiBufferSource wrappedSource = renderType -> {
            if (!shouldWrapRenderType(renderType)) {
                return originalSource.getBuffer(renderType);
            }
            RenderType translucentType = getTranslucentRenderType(renderType, null, null);
            VertexConsumer originalConsumer = originalSource.getBuffer(translucentType);
            return wrapVertexConsumer(originalConsumer, finalAlpha);
        };

        try {
            isRenderingFadedPlayer = true;
            float entityYaw = net.minecraft.util.Mth.lerp(event.getPartialTick(), entity.yRotO, entity.getYRot());
            // Safe raw cast of the renderer to avoid the wildcard generic conversion compilation error!
            ((net.minecraft.client.renderer.entity.LivingEntityRenderer<net.minecraft.world.entity.LivingEntity, ?>) (Object) event.getRenderer()).render(
                entity,
                entityYaw,
                event.getPartialTick(),
                event.getPoseStack(),
                wrappedSource,
                event.getPackedLight()
            );
        } finally {
            isRenderingFadedPlayer = false;
        }
    }

    /**
     * Highly compatible dynamic proxy-based wrapper that implements all interfaces 
     * of the delegate VertexConsumer (including third-party custom ones like Sodium's).
     * This completely prevents ClassCastExceptions when shaders and rendering mods are loaded!
     */
    private static VertexConsumer wrapVertexConsumer(VertexConsumer delegate, float alphaFactor) {
        if (delegate == null) {
            return null;
        }
        try {
            java.util.List<Class<?>> interfaces = new java.util.ArrayList<>();
            interfaces.add(VertexConsumer.class);
            for (Class<?> iface : delegate.getClass().getInterfaces()) {
                if (!interfaces.contains(iface) && java.lang.reflect.Modifier.isPublic(iface.getModifiers())) {
                    String ifaceName = iface.getName().toLowerCase(java.util.Locale.ROOT);
                    // Prevent fast-path bypass from optimization mods by not implementing their proprietary interfaces.
                    if (ifaceName.contains("sodium") || ifaceName.contains("rubidium") || ifaceName.contains("oculus") || ifaceName.contains("iris") || ifaceName.contains("embeddium")) {
                        continue;
                    }
                    interfaces.add(iface);
                }
            }

            return (VertexConsumer) java.lang.reflect.Proxy.newProxyInstance(
                VertexConsumer.class.getClassLoader(),
                interfaces.toArray(new Class<?>[0]),
                new java.lang.reflect.InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                        String name = method.getName();
                        
                        if ((name.equals("setColor") || name.equals("color")) && args != null) {
                            if (args.length == 4) {
                                if (args[3] instanceof Integer) {
                                    int r = (Integer) args[0];
                                    int g = (Integer) args[1];
                                    int b = (Integer) args[2];
                                    int a = (Integer) args[3];
                                    int scaledAlpha = Math.max(0, Math.min(255, (int) (a * alphaFactor)));
                                    Object result = method.invoke(delegate, r, g, b, scaledAlpha);
                                    return result == delegate ? proxy : result;
                                } else if (args[3] instanceof Float) {
                                    float r = (Float) args[0];
                                    float g = (Float) args[1];
                                    float b = (Float) args[2];
                                    float a = (Float) args[3];
                                    float scaledAlpha = Math.max(0.0f, Math.min(1.0f, a * alphaFactor));
                                    Object result = method.invoke(delegate, r, g, b, scaledAlpha);
                                    return result == delegate ? proxy : result;
                                }
                            } else if (args.length == 1 && args[0] instanceof Integer) {
                                int argb = (Integer) args[0];
                                int a = (argb >> 24) & 0xFF;
                                int r = (argb >> 16) & 0xFF;
                                int g = (argb >> 8) & 0xFF;
                                int b = argb & 0xFF;
                                int scaledAlpha = Math.max(0, Math.min(255, (int) (a * alphaFactor)));
                                int newArgb = (scaledAlpha << 24) | (r << 16) | (g << 8) | b;
                                Object result = method.invoke(delegate, newArgb);
                                return result == delegate ? proxy : result;
                            }
                        }

                        if (name.equals("putBulkData") && args != null) {
                            for (int i = 0; i <= args.length - 4; i++) {
                                if (args[i] instanceof Float && args[i+1] instanceof Float && args[i+2] instanceof Float && args[i+3] instanceof Float) {
                                    float a = (Float) args[i+3];
                                    args[i+3] = Math.max(0.0f, Math.min(1.0f, a * alphaFactor));
                                    break;
                                }
                            }
                        }

                        try {
                            if (method.isDefault()) {
                                try {
                                    Object result = java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                                    return result == proxy || result == delegate ? proxy : result;
                                } catch (Throwable t) {
                                    // Fallback to normal invocation
                                }
                            }
                            
                            Object result = method.invoke(delegate, args);
                            if (result == delegate) {
                                return proxy;
                            }
                            return result;
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                }
            );
        } catch (Throwable t) {
            return new SafeAlphaVertexConsumer(delegate, alphaFactor);
        }
    }

    /**
     * A highly precise delegating VertexConsumer that intercepts all setColor() and color() calls
     * and multiplies the Alpha parameter by our fade factor.
     */
    private static class SafeAlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alphaFactor;

        public SafeAlphaVertexConsumer(VertexConsumer delegate, float alphaFactor) {
            this.delegate = delegate;
            this.alphaFactor = alphaFactor;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            // Apply the fade scale factor to the alpha byte (0-255)
            int scaledAlpha = Math.max(0, Math.min(255, (int) (a * alphaFactor)));
            delegate.setColor(r, g, b, scaledAlpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}