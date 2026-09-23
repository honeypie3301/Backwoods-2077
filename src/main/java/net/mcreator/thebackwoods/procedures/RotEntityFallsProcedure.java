package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.mcreator.thebackwoods.entity.RotEntity;

public class RotEntityFallsProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof RotEntity rot) {
			float distance = rot.fallDistance;
			RotOnEntityTickUpdateProcedure.UniversalCombatPredictionEngine.recordRotLandingImpact(rot, distance);
			rot.resetFallDistance();
			rot.getPersistentData().putBoolean("is_falling_heavy", false);

			// Comprehensive check: ensure Rot is not performing ANY ability, combo, beam, laser, scream, or charge
			boolean isBusyWithAbility = rot.getPersistentData().getDouble("sentinel_slam_phase") > 0
				|| rot.getPersistentData().getDouble("rot_overhead_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_judgment_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_die_kick_phase") > 0
				|| rot.getPersistentData().getDouble("sentinel_sonic_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_heavy_left_punch_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_heavy_right_punch_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_minos_stage") > 0
				|| rot.getPersistentData().getDouble("sentinel_uppercut_anim_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_cc1_stage") > 0
				|| rot.getPersistentData().getDouble("sentinel_cc2_stage") > 0
				|| rot.getPersistentData().getDouble("sentinel_cc3_stage") > 0
				|| rot.getPersistentData().getDouble("sentinel_cc4_stage") > 0
				|| rot.getPersistentData().getDouble("sentinel_cc5_stage") > 0
				|| rot.getPersistentData().getDouble("sentinel_armor_rip_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_solar_fire_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_solar_charge_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_cryo_fire_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_cryo_charge_ticks") > 0
				|| rot.getPersistentData().getDouble("sentinel_sonic_scream_ticks") > 0
				|| rot.getPersistentData().getDouble("rot_superheat_charging") > 0
				|| rot.getPersistentData().getDouble("rot_superheat_active") > 0
				|| rot.getPersistentData().getDouble("controlled_adaptation_ticks") > 0
				|| rot.getPersistentData().getBoolean("is_blocking")
				|| rot.getPersistentData().getBoolean("is_uppercutting");

			// Trigger full landing crouch/kneel absorption (1.5s = 30 ticks) if fell from >= 4.0 blocks and not in an ability
			if (distance >= 4.0F && !isBusyWithAbility) {
				boolean pickFirst = Math.random() < 0.5;
				
				// Set MCreator synced entity data parameters (used directly by playback conditions)
				rot.getEntityData().set(RotEntity.DATA_isLand, pickFirst);
				rot.getEntityData().set(RotEntity.DATA_isLand2, !pickFirst);
				rot.getPersistentData().putBoolean("isLand", pickFirst);
				rot.getPersistentData().putBoolean("isLand2", !pickFirst);
				rot.getPersistentData().putDouble("rot_land_timer", 30.0); // 30 ticks (1.5s) for full absorption animation

				// Freeze motion on landing so crouch/kneel absorption does not blend with walking
				rot.setDeltaMovement(new Vec3(0, Math.min(0, rot.getDeltaMovement().y), 0));
				rot.hasImpulse = true;
				if (rot instanceof Mob mob) {
					mob.getNavigation().stop();
					mob.setSpeed(0.0F);
					mob.xxa = 0.0F;
					mob.zza = 0.0F;
				}
			}
		}
	}
} // 1.21.1