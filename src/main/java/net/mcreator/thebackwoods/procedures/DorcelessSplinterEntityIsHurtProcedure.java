package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public class DorcelessSplinterEntityIsHurtProcedure {

	public static void execute(LevelAccessor world, double x, double y, double z, DamageSource damagesource, Entity entity, Entity immediatesourceentity, Entity sourceentity) {
		execute(null, world, x, y, z, damagesource, entity, immediatesourceentity, sourceentity);
	}

	private static void execute(@Nullable Object event, LevelAccessor world, double x, double y, double z, DamageSource damagesource, Entity entity, Entity immediatesourceentity, Entity sourceentity) {
		if (entity == null || !(entity instanceof Mob mob))
			return;

		Entity attacker = sourceentity != null ? sourceentity : immediatesourceentity;
		boolean isProjectile = (immediatesourceentity instanceof Projectile) || (damagesource != null && damagesource.getDirectEntity() instanceof Projectile);

		// 1. Calculate Dodge Probability (85% for projectiles like Enderman, 50% for melee)
		double dodgeChance = isProjectile ? 0.85 : 0.50;
		boolean dodged = mob.getRandom().nextDouble() < dodgeChance;

		if (attacker instanceof Player player && !player.isSpectator() && !player.isCreative()) {
			mob.setTarget(player);
		}

		// 2. Perform Enderman-style Dodge Teleport if triggered or hit
		if (dodged && attacker != null) {
			double px = attacker.getX();
			double py = attacker.getY();
			double pz = attacker.getZ();

			Vec3 awayVec = mob.position().subtract(attacker.position()).normalize();

			for (int attempt = 0; attempt < 12; attempt++) {
				// Random flank angle (30° - 80° offset to sides)
				double sideSign = mob.getRandom().nextBoolean() ? 1.0 : -1.0;
				double angleOffset = sideSign * (0.5 + mob.getRandom().nextDouble() * 0.8);
				double dist = 6.0 + mob.getRandom().nextDouble() * 5.0; // 6 - 11 blocks

				double tryX = px + (awayVec.x * Math.cos(angleOffset) - awayVec.z * Math.sin(angleOffset)) * dist;
				double tryZ = pz + (awayVec.x * Math.sin(angleOffset) + awayVec.z * Math.cos(angleOffset)) * dist;

				for (int yOffset = 3; yOffset >= -4; yOffset--) {
					BlockPos pos = BlockPos.containing(tryX, py + yOffset, tryZ);
					BlockPos below = pos.below();

					if (world.getBlockState(below).isSolid() && world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above())) {
						// Old Position FX (Ink Puff & Crackle)
						if (world instanceof ServerLevel serverLevel) {
							serverLevel.sendParticles(ParticleTypes.SQUID_INK, mob.getX(), mob.getY() + 1.0, mob.getZ(), 25, 0.3, 0.5, 0.3, 0.08);
							serverLevel.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1.0, mob.getZ(), 10, 0.2, 0.4, 0.2, 0.02);
						}
						if (world instanceof Level level) {
							level.playSound(null, BlockPos.containing(mob.getX(), mob.getY(), mob.getZ()), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.0F, 0.4F);
							level.playSound(null, BlockPos.containing(mob.getX(), mob.getY(), mob.getZ()), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
						}

						// Teleport away
						mob.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

						// Face attacker immediately
						double dx = attacker.getX() - (pos.getX() + 0.5);
						double dz = attacker.getZ() - (pos.getZ() + 0.5);
						float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
						mob.setYRot(yaw);
						mob.setYBodyRot(yaw);
						mob.setYHeadRot(yaw);
						mob.getLookControl().setLookAt(attacker, 360.0F, 360.0F);

						// New Position FX
						if (world instanceof ServerLevel serverLevel) {
							serverLevel.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.08);
						}
						if (world instanceof Level level) {
							level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
						}

						// Cooldown timer reset
						mob.getPersistentData().putDouble("dorceless_tp_timer", 50);
						return;
					}
				}
			}
		}
	}
} // 1.21.1