package net.mcreator.thebackwoods;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 1.21.1 Client-Side Procedural Gait Controller for Woodweaver Entity.
 * Coordinates tripod step gait and resolves foot target positions with beautifully spread spider legs.
 */
public class WeaverGait {
    private static final Map<UUID, EntityGaitState> GAIT_STATES = new HashMap<>();

    public static EntityGaitState getOrCreateState(UUID uuid) {
        return GAIT_STATES.computeIfAbsent(uuid, k -> new EntityGaitState());
    }

    public static void cleanUp(UUID uuid) {
        GAIT_STATES.remove(uuid);
    }

    public static class LegState {
        public Vec3 currentPlantedPos = Vec3.ZERO;
        public Vec3 lastPlantedPos = Vec3.ZERO;
        public Vec3 targetHomePos = Vec3.ZERO;
        public float stepProgress = 1.0f; // 1.0 = fully planted
        public boolean isStepping = false;

        public void init(Vec3 initialPos) {
            this.currentPlantedPos = initialPos;
            this.lastPlantedPos = initialPos;
            this.targetHomePos = initialPos;
            this.stepProgress = 1.0f;
            this.isStepping = false;
        }
    }

    public static class EntityGaitState {
        public final LegState[] legs = new LegState[6];
        public int activeGroup = 0; // 0 or 1
        public long lastTick = 0;

        public EntityGaitState() {
            for (int i = 0; i < 6; i++) {
                legs[i] = new LegState();
            }
        }
    }

    // Tripod Gait Groups (Alternating stability triangles)
    // Group 0: Left Front (13), Right Mid (7), Left Back (10)
    // Group 1: Right Front (4), Left Mid (16), Right Back (1)
    public static final int[][] GAIT_GROUPS = {
        {0, 3, 4}, // index mapping for [leg13, leg7, leg10]
        {1, 2, 5}  // index mapping for [leg4, leg16, leg1]
    };

    /**
     * Resolves the target block coordinate beneath a relative offset from the body, including an optional horizontal predictive offset.
     */
    public static Vec3 findGroundHeight(Level level, Entity entity, Vec3 relativeOffset, float yaw, Vec3 worldPredict) {
        double yawRad = Math.toRadians(yaw);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        
        // Correct Local to World conversion
        double rx = relativeOffset.x * cos - relativeOffset.z * sin;
        double rz = -(relativeOffset.x * sin + relativeOffset.z * cos);

        // Position the hip joint relative to body center, dynamic height, and predicted motion step
        Vec3 hipWorldPos = entity.position().add(rx, relativeOffset.y, rz).add(worldPredict);
        
        // Scan for ground from above hip down to beneath ground level
        // Starting 3.0 blocks above the hip ensures we find any raised terrain
        double startY = hipWorldPos.y + 3.0;
        // Ending 6.0 blocks below the entity position ensures we find any dropped terrain
        double endY = entity.getY() - 6.0;

        Vec3 rayStart = new Vec3(hipWorldPos.x, startY, hipWorldPos.z);
        Vec3 rayEnd = new Vec3(hipWorldPos.x, endY, hipWorldPos.z);

        BlockHitResult hit = level.clip(new ClipContext(
            rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity
        ));

        if (hit.getType() != BlockHitResult.Type.MISS) {
            return hit.getLocation();
        }
        return new Vec3(hipWorldPos.x, entity.getY(), hipWorldPos.z); // Fallback to feet plane
    }

    /**
     * Updates the gait cycle for the 6 legs of the Woodweaver using dynamic hip heights.
     */
    public static void updateGait(Entity entity, float partialTicks, double[] hipY) {
        if (!entity.level().isClientSide()) return;

        UUID uuid = entity.getUUID();
        EntityGaitState state = getOrCreateState(uuid);
        Level level = entity.level();
        
        // Use smooth body yaw from LivingEntity
        float yaw = entity.getYRot();
        if (entity instanceof LivingEntity living) {
            yaw = Mth.lerp(partialTicks, living.yBodyRotO, living.yBodyRot);
        }
        
        Vec3 velocity = entity.getDeltaMovement();
        double speed = velocity.horizontalDistance();

        // Step parameters
        double stepLength = 0.90; // Snappy trigger distance to take a step
        float stepSpeed = 0.20f; // Crisp default step progress increment per frame
        if (speed > 0.05) {
            stepSpeed = (float) Math.min(0.38, 0.18 + speed * 0.45);
        }

        // Dynamically scale home coordinates for spider feet positions based on ground height (uphill vs downhill)
        Vec3[] relativeHomeOffsets = new Vec3[6];
        for (int i = 0; i < 6; i++) {
            // Baseline spider spread offsets matching the Blockbench model's 4.0 blocks spread perfectly
            double baseOffsetX = 4.0;
            if (i == 0 || i == 2 || i == 4) baseOffsetX = -baseOffsetX;
            double baseOffsetZ = 0.0;
            if (i == 0 || i == 1) baseOffsetZ = -2.2;
            if (i == 4 || i == 5) baseOffsetZ = 2.2;

            // 1. Initial quick query to estimate the height of the terrain at this offset
            Vec3 baselineOffset = new Vec3(baseOffsetX, hipY[i], baseOffsetZ);
            Vec3 estGround = findGroundHeight(level, entity, baselineOffset, yaw, Vec3.ZERO);
            
            // Calculate relative vertical offset of the ground from the dynamic hip height
            double hipHeightEst = entity.getY() + hipY[i];
            double verticalDiff = hipHeightEst - estGround.y; // positive means ground is below hip

            // 2. Adaptive Horizontal Scale Factor
            double scale = 1.0;
            if (verticalDiff > 3.2) {
                // Stepping downhill: widen the foot target horizontally to maintain a gorgeous, stable stance
                scale += Math.min(0.40, (verticalDiff - 3.2) * 0.25);
            } else if (verticalDiff < 2.6) {
                // Stepping uphill: widen the foot target slightly to pull the knees outward and prevent overlapping
                scale += Math.min(0.30, (2.6 - verticalDiff) * 0.25);
            }

            double finalOffsetX = baseOffsetX * scale;
            double finalOffsetZ = baseOffsetZ * scale;
            relativeHomeOffsets[i] = new Vec3(finalOffsetX, hipY[i], finalOffsetZ);
        }

        // Initialize state on first tick
        if (state.legs[0].currentPlantedPos.equals(Vec3.ZERO)) {
            for (int i = 0; i < 6; i++) {
                Vec3 ground = findGroundHeight(level, entity, relativeHomeOffsets[i], yaw, Vec3.ZERO);
                state.legs[i].init(ground);
            }
        }

        // Update home targets and process current steps
        boolean groupIsStepping = false;
        for (int i = 0; i < 6; i++) {
            LegState leg = state.legs[i];
            
            // Calculate predicted world-space shift based on entity velocity
            Vec3 worldPredict = Vec3.ZERO;
            if (speed > 0.02) {
                worldPredict = new Vec3(velocity.x * 5.0, 0.0, velocity.z * 5.0);
            }

            // Calculate current idealized Home Position in world space, including the predictive raycast vector
            Vec3 homePos = findGroundHeight(level, entity, relativeHomeOffsets[i], yaw, worldPredict);
            leg.targetHomePos = homePos;

            // Sudden teleport / snap reset to prevent huge stretching
            if (entity.position().distanceToSqr(leg.currentPlantedPos) > 40.0) {
                leg.init(homePos);
            }

            if (leg.isStepping) {
                groupIsStepping = true;
                leg.stepProgress += stepSpeed;
                if (leg.stepProgress >= 1.0f) {
                    leg.stepProgress = 1.0f;
                    leg.isStepping = false;
                    leg.currentPlantedPos = leg.targetHomePos;
                } else {
                    // Smoothstep easing for buttery-smooth joint movements
                    double t = leg.stepProgress * leg.stepProgress * (3.0 - 2.0 * leg.stepProgress);
                    double lerpX = Mth.lerp(t, leg.lastPlantedPos.x, leg.targetHomePos.x);
                    double lerpZ = Mth.lerp(t, leg.lastPlantedPos.z, leg.targetHomePos.z);
                    
                    // Parabolic Height curve: h = max(lastY, targetY) + stepHeight * sin(pi * progress)
                    double stepHeight = 0.75; 
                    double curveY = Math.sin(leg.stepProgress * Math.PI) * stepHeight;
                    double lerpY = Mth.lerp(t, leg.lastPlantedPos.y, leg.targetHomePos.y) + curveY;
                    
                    leg.currentPlantedPos = new Vec3(lerpX, lerpY, lerpZ);
                }
            } else if (speed < 0.02) {
                // When standing still, gently align the foot's Y coordinate to the actual block collision shape at its current (X, Z) coordinates.
                // This prevents floating/clipping when block hitboxes are placed or broken under the spider's feet!
                Vec3 currentPos = leg.currentPlantedPos;
                double startY = currentPos.y + 2.5;
                double endY = currentPos.y - 2.5;
                Vec3 rayStart = new Vec3(currentPos.x, startY, currentPos.z);
                Vec3 rayEnd = new Vec3(currentPos.x, endY, currentPos.z);
                
                BlockHitResult hit = level.clip(new ClipContext(
                    rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity
                ));
                
                if (hit.getType() != BlockHitResult.Type.MISS) {
                    double targetY = hit.getLocation().y;
                    leg.currentPlantedPos = new Vec3(currentPos.x, Mth.lerp(0.2, currentPos.y, targetY), currentPos.z);
                }
            }
        }

        // Tripod alternation trigger logic (allowing standing adjustment shuffle)
        // Check if any leg is currently in the middle of a step to enforce coordination
        boolean anyLegStepping = false;
        for (int i = 0; i < 6; i++) {
            if (state.legs[i].isStepping) {
                anyLegStepping = true;
                break;
            }
        }

        if (!anyLegStepping) {
            // Neither group is stepping. Check which group has legs that need to step.
            double maxDistGrp0 = 0;
            double maxDistGrp1 = 0;
            boolean grp0NeedsStep = false;
            boolean grp1NeedsStep = false;

            boolean isMoving = speed > 0.01;
            double triggerDist = isMoving ? (stepLength * stepLength) : 2.25;

            for (int legIdx : GAIT_GROUPS[0]) {
                double d2 = state.legs[legIdx].currentPlantedPos.distanceToSqr(state.legs[legIdx].targetHomePos);
                if (d2 > maxDistGrp0) maxDistGrp0 = d2;
                if (d2 > triggerDist) grp0NeedsStep = true;
            }

            for (int legIdx : GAIT_GROUPS[1]) {
                double d2 = state.legs[legIdx].currentPlantedPos.distanceToSqr(state.legs[legIdx].targetHomePos);
                if (d2 > maxDistGrp1) maxDistGrp1 = d2;
                if (d2 > triggerDist) grp1NeedsStep = true;
            }

            int groupToStep = -1;
            if (grp0NeedsStep && grp1NeedsStep) {
                // If both need to step, prioritize the group with the leg that is furthest behind
                groupToStep = (maxDistGrp0 >= maxDistGrp1) ? 0 : 1;
            } else if (grp0NeedsStep) {
                groupToStep = 0;
            } else if (grp1NeedsStep) {
                groupToStep = 1;
            }

            if (groupToStep != -1) {
                // Trigger steps for all legs in the chosen group to step in a beautifully synchronized tripod team
                for (int legIdx : GAIT_GROUPS[groupToStep]) {
                    LegState leg = state.legs[legIdx];
                    double distToHome = leg.currentPlantedPos.distanceToSqr(leg.targetHomePos);
                    // Trigger if it is at least slightly off target (0.1 blocks) to keep them beautifully aligned
                    if (distToHome > 0.01 || !isMoving) {
                        leg.lastPlantedPos = leg.currentPlantedPos;
                        leg.stepProgress = 0.0f;
                        leg.isStepping = true;
                    }
                }
            }
        }
    }
}
