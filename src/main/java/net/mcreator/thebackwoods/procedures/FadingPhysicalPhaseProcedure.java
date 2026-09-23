package net.mcreator.thebackwoods.procedures;

/**
 * =========================================================================
 *              EXISTENTIAL PHASE-SHIFTING: THE PHYSICAL PHASE
 *           Projectiles Passing Through Faded Players & Mobs (1.21.1)
 * =========================================================================
 * 
 * INSTRUCTIONS FOR MCREATOR USER:
 * 1. Inside MCreator, ensure you have the "fade_level" global player variable setup.
 * 
 * 2. Put this code in a Custom Code Procedure named "FadingPhysicalPhaseProcedure"
 *    in your 1.21.1 workspace.
 * 
 * Path: src/main/java/net/mcreator/thebackwoods/procedures/FadingPhysicalPhaseProcedure.java
 */

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.mcreator.thebackwoods.network.TheBackwoodsModVariables;

@EventBusSubscriber
public class FadingPhysicalPhaseProcedure {

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult entityHitResult) {
            Entity hitEntity = entityHitResult.getEntity();
            if (hitEntity != null) {
                double fadeLevel = getFadeLevel(hitEntity);
                if (fadeLevel > 15.0) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Highly optimized fade level retrieval for 1.21.1.
     */
    public static double getFadeLevel(Entity entity) {
        if (entity == null) {
            return 0.0;
        }
        if (entity instanceof Player player) {
            try {
                return player.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).fade_level;
            } catch (Throwable ignored) {}
        }
        try {
            return entity.getPersistentData().getDouble("fade_level");
        } catch (Throwable ignored) {}
        return 0.0;
    }
}
