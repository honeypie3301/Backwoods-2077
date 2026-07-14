package net.mcreator.thebackwoods.procedures;

import org.joml.Quaternionf;
import net.minecraft.world.level.LevelAccessor;
import java.util.Random;
import java.util.Map;

public class FractusLaserParticleRedParticleOrientationProcedure {
	public static Quaternionf execute(LevelAccessor world, double x, double y, double z, double speedX, double speedY, double speedZ, double angularVelocity, double angularAcceleration, double age) {
		// Calculate the squared speed of the particle to verify if it has movement
		double speedSq = speedX * speedX + speedY * speedY + speedZ * speedZ;

		// 1. Alignment quaternion: Aligns the local up vector (0, 1, 0) of the quad with the velocity vector
		Quaternionf alignment = new Quaternionf();
		if (speedSq > 1e-6) {
			float rSpeed = (float) Math.sqrt(speedSq);
			alignment.rotationTo(0.0f, 1.0f, 0.0f, (float) speedX / rSpeed, (float) speedY / rSpeed, (float) speedZ / rSpeed);
		}

		// 2. Stable unique start offset for each particle based on its spawn location (hash of coordinates)
		long coordHash = Double.doubleToLongBits(x) ^ Double.doubleToLongBits(y) ^ Double.doubleToLongBits(z);
		Random rand = new Random(coordHash);
		float initialSpin = rand.nextFloat() * (float) Math.PI * 2.0f;

		// 3. Active spinning using exact rotational kinematics: theta(t) = theta0 + omega * t + 0.5 * alpha * t^2
		float dynamicSpin = (float) ((angularVelocity * age) + (0.5 * angularAcceleration * age * age));
		float totalSpin = initialSpin + dynamicSpin;

		// 4. Apply local spinning around the main alignment (Y-axis of the particle)
		Quaternionf localSpin = new Quaternionf().rotationY(totalSpin);

		// Combine both rotations: align the local spinning quad with the movement direction
		return alignment.mul(localSpin);
	}

	// Overloaded fallback for standard MCreator dependency map or fewer arguments
	public static Quaternionf execute(Map<String, Object> dependencies) {
		if (dependencies == null) {
			return new Quaternionf();
		}
		double speedX = dependencies.get("speedX") instanceof Number n ? n.doubleValue() : 0.0;
		double speedY = dependencies.get("speedY") instanceof Number n ? n.doubleValue() : 0.0;
		double speedZ = dependencies.get("speedZ") instanceof Number n ? n.doubleValue() : 0.0;
		double angularVelocity = dependencies.get("angularVelocity") instanceof Number n ? n.doubleValue() : 0.0;
		double angularAcceleration = dependencies.get("angularAcceleration") instanceof Number n ? n.doubleValue() : 0.0;
		double age = dependencies.get("age") instanceof Number n ? n.doubleValue() : 0.0;
		double x = dependencies.get("x") instanceof Number n ? n.doubleValue() : 0.0;
		double y = dependencies.get("y") instanceof Number n ? n.doubleValue() : 0.0;
		double z = dependencies.get("z") instanceof Number n ? n.doubleValue() : 0.0;
		LevelAccessor world = dependencies.get("world") instanceof LevelAccessor lvl ? lvl : null;

		return execute(world, x, y, z, speedX, speedY, speedZ, angularVelocity, angularAcceleration, age);
	}
}