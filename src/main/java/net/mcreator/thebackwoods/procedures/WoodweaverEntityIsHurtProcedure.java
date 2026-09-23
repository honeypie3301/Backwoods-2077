package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.entity.WoodweaverEntity;

public class WoodweaverEntityIsHurtProcedure {
	public static void execute(LevelAccessor world, double x, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		boolean is_hypno = false;
		boolean hit_orb = false;
		double orb_x = 0;
		double orb_y = 0;
		double orb_z = 0;
		double attacker_eye_x = 0;
		double attacker_eye_y = 0;
		double attacker_eye_z = 0;
		double look_x = 0;
		double look_y = 0;
		double look_z = 0;
		double to_orb_x = 0;
		double to_orb_y = 0;
		double to_orb_z = 0;
		double proj_t = 0;
		double dist_to_orb_sq = 0;
		double hp_ratio = 0;
		double required_hits = 0;
		double current_hits = 0;
		if (entity.getPersistentData().getDouble("hypno_phase") == 1 || entity.getPersistentData().getDouble("hypno_phase") == 2) {
			is_hypno = true;
		} else {
			is_hypno = false;
		}
		if (!is_hypno) {
			if (!(sourceentity instanceof WoodweaverEntity)) {
				if (entity instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
					_entity.setTarget(_ent);
			}
			return;
		}
		look_x = sourceentity.getLookAngle().x;
		look_y = sourceentity.getLookAngle().y;
		look_z = sourceentity.getLookAngle().z;
		orb_x = entity.getX() - Math.sin(Math.toRadians(entity.getYRot())) * 1.9;
		orb_y = entity.getY() + 5.701;
		orb_z = entity.getZ() + Math.cos(Math.toRadians(entity.getYRot())) * 1.9;
		attacker_eye_x = sourceentity.getX();
		attacker_eye_y = sourceentity.getY() + 1.5;
		attacker_eye_z = sourceentity.getZ();
		to_orb_x = orb_x - attacker_eye_x;
		to_orb_y = orb_y - attacker_eye_y;
		to_orb_z = orb_z - attacker_eye_z;
		proj_t = (to_orb_x * look_x) + (to_orb_y * look_y) + (to_orb_z * look_z);;
		hit_orb = false;
		if (proj_t > 0 && proj_t <= 6) {
			dist_to_orb_sq = Math.pow((attacker_eye_x + proj_t * look_x) - orb_x, 2) + Math.pow((attacker_eye_y + proj_t * look_y) - orb_y, 2) + Math.pow((attacker_eye_z + proj_t * look_z) - orb_z, 2);
			if (dist_to_orb_sq <= 1.21) {
				hit_orb = true;
			}
		}
		if (!hit_orb) {
			return;
		}
		hp_ratio = (double) (entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) / (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
		if (hp_ratio > 0.7) {
			required_hits = 3;
		} else if (hp_ratio > 0.35) {
			required_hits = 2;
		} else {
			required_hits = 1;
		}
		current_hits = entity.getPersistentData().getDouble("hypno_hits_received") + 1;
		entity.getPersistentData().putDouble("hypno_hits_received", current_hits);
		if (current_hits >= required_hits) {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, entity.getY() + 3, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.ender_dragon.growl")), SoundSource.HOSTILE, 4, (float) 0.48);
				} else {
					_level.playLocalSound(x, (entity.getY() + 3), z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.ender_dragon.growl")), SoundSource.HOSTILE, 4, (float) 0.48, false);
				}
			}
			entity.getPersistentData().putDouble("hypno_phase", 3);
			entity.getPersistentData().putBoolean("hypno_interrupted", true);
			entity.getPersistentData().putBoolean("is_hypnotizing", false);
			entity.getPersistentData().putDouble("hypno_hits_received", 0);
			entity.getPersistentData().putDouble("hypno_target_id", 0);
			entity.getPersistentData().putDouble("stare_ticks", 0);
			entity.getPersistentData().putDouble("hypno_cooldown", 240);
			if (entity instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
				_entity.setTarget(_ent);
		}
	}
}