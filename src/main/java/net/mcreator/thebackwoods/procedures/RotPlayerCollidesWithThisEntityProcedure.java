package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

public class RotPlayerCollidesWithThisEntityProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;

		boolean isPhaseShifting = entity.getPersistentData().getBoolean("rot_phase_shifting");
		if (isPhaseShifting) {
			// Ghost pass-through effect: gently glide the colliding player through the Rot
			Vec3 moveDir = sourceentity.getDeltaMovement();
			if (moveDir.lengthSqr() < 0.01) {
				moveDir = sourceentity.getLookAngle().scale(0.15);
			}
			sourceentity.setDeltaMovement(sourceentity.getDeltaMovement().add(moveDir.x * 0.2, 0.0, moveDir.z * 0.2));
			sourceentity.hasImpulse = true;
		}
	}
 // 1.21.1
}