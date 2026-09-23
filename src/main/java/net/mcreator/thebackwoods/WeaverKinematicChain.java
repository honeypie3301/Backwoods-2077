package net.mcreator.thebackwoods;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;

/**
 * Analytical Inverse Kinematics solver for Woodweaver's 3-segment limbs.
 * Aligns the flat foot segment perfectly horizontal on the ground surface without clipping.
 */
public class WeaverKinematicChain {

    /**
     * Solves Inverse Kinematics for a 3-segment leg and updates ModelPart rotations with angular limits.
     * 
     * @param hip             ModelPart for the hip joint (handles horizontal Yaw swing and vertical thigh Roll).
     * @param thigh           ModelPart for the thigh joint (handles vertical knee bend Roll).
     * @param shin            ModelPart for the shin joint (handles foot angle).
     * @param hipWorldPos     Absolute world coordinate of the hip joint center.
     * @param targetFootPos   Absolute world coordinate where the foot is planted.
     * @param segment1Length  Length of thigh (Segment 1) in block units.
     * @param segment2Length  Length of shin (Segment 2) in block units.
     * @param isLeft          True if this is on the left side of the model (flips mirror axes).
     * @param entityYaw       Current horizontal rotation of the entity in degrees.
     * @param minYaw          Minimum horizontal rotation limit (radians) to prevent crossing.
     * @param maxYaw          Maximum horizontal rotation limit (radians) to prevent crossing.
     */
    public static void solveIK(ModelPart hip, ModelPart thigh, ModelPart shin, 
                               Vec3 hipWorldPos, Vec3 targetFootPos, 
                               double segment1Length, double segment2Length, 
                               boolean isLeft, float entityYaw,
                               float minYaw, float maxYaw) {
        
        // 1. Adjust target vertically to account for foot box thickness (4 pixels = 0.25 blocks)
        // This keeps the ankle pivot elevated and places the bottom of the foot box perfectly flat on top of the ground.
        Vec3 adjustedTarget = new Vec3(targetFootPos.x, targetFootPos.y + 0.25, targetFootPos.z);
        
        // Relocalize foot coordinate to hip-centered coordinate system
        Vec3 relWorld = adjustedTarget.subtract(hipWorldPos);
        
        // Correct Minecraft World -> Local coordinate transformation
        // South is 0 degrees (increasing clockwise). Local -Z is Front, local -X is Left.
        double yawRad = Math.toRadians(entityYaw);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        
        double localX = relWorld.x * cos - relWorld.z * sin;
        double localZ = -(relWorld.x * sin + relWorld.z * cos);
        double localY = relWorld.y;

        // 2. Resolve Hip Yaw (rotation around Y-axis) with direction correction
        double angleToTarget = Math.atan2(localZ, localX);
        float calculatedYaw;
        if (isLeft) {
            calculatedYaw = (float) (Math.PI - angleToTarget);
        } else {
            calculatedYaw = (float) -angleToTarget;
        }

        // Clamp to natural angular boundaries to prevent crossing / tangling
        hip.yRot = Math.max(minYaw, Math.min(maxYaw, calculatedYaw));

        // 3. Resolve vertical segment pitches using the Law of Cosines
        double horizDist = Math.sqrt(localX * localX + localZ * localZ);
        double d = Math.sqrt(horizDist * horizDist + localY * localY);

        // Stretch constraints: Prevent NaN triggers and enforce a beautiful minimum knee bend
        // 98% of maximum physical extension prevents the leg from ever locking into a straight wooden pole while allowing full downhill reach
        // Enforce a healthy minimum extension of 1.5 blocks to prevent physical clipping, joint flipping, and mathematical singularities.
        double maxReach = (segment1Length + segment2Length) * 0.98;
        double minReach = 1.5;
        if (d > maxReach) {
            d = maxReach;
        } else if (d < minReach) {
            d = minReach;
        }

        // Segment lengths
        double a = segment1Length;
        double b = segment2Length;
        double c = d;

        // Law of Cosines calculations
        double cosThigh = (a * a + c * c - b * b) / (2 * a * c);
        double cosShin = (a * a + b * b - c * c) / (2 * a * b);

        // Boundaries clamp safety
        cosThigh = Math.max(-1.0, Math.min(1.0, cosThigh));
        cosShin = Math.max(-1.0, Math.min(1.0, cosShin));

        double angleThigh = Math.acos(cosThigh);
        double angleShin = Math.acos(cosShin);

        // Slope angle from joint horizon to target center
        double baseSlope = Math.atan2(localY, horizDist);

        // Reset 3D twisting on the child joints to keep thigh and shin in a pure vertical plane, preventing feet curling up sideways
        thigh.xRot = 0.0f;
        thigh.yRot = 0.0f;
        shin.xRot = 0.0f;
        shin.yRot = 0.0f;

        // Apply local rotations around Z-axis (Roll) as specified by the Blockbench model hierarchy
        if (isLeft) {
            // Hip vertical tilt (Segment 1)
            hip.zRot = (float) (baseSlope + angleThigh);
            // Thigh knee bend (Segment 2)
            thigh.zRot = (float) -(Math.PI - angleShin);
            // Shin foot bend (Segment 3) - Cancel hip and thigh rotation to keep foot flat on ground exactly like Blockbench
            shin.zRot = -(hip.zRot + thigh.zRot);
        } else {
            // Hip vertical tilt (Segment 1)
            hip.zRot = (float) -(baseSlope + angleThigh);
            // Thigh knee bend (Segment 2)
            thigh.zRot = (float) (Math.PI - angleShin);
            // Shin foot bend (Segment 3) - Cancel hip and thigh rotation to keep foot flat on ground exactly like Blockbench
            shin.zRot = -(hip.zRot + thigh.zRot);
        }
    }
}
