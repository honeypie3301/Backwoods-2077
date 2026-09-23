package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.mcreator.thebackwoods.entity.RotEntity;

public class ClearFlightPathProcedure {

	public static boolean ENABLED = true;

	public static float MINOS_TIER2_HARDNESS = Float.MAX_VALUE;
	public static float MINOS_TIER1_HARDNESS = 20.0F;
	public static float MINOS_TIER0_HARDNESS = 10.0F;
	public static double MINOS_TIER1_DAMAGE_MULT = 1.2;
	public static double MINOS_TIER0_DAMAGE_MULT = 0.0;

	public static float OMNI_TIER2_HARDNESS = Float.MAX_VALUE;
	public static float OMNI_TIER1_HARDNESS = 15.0F;
	public static float OMNI_TIER0_HARDNESS = 5.0F;
	public static double OMNI_TIER1_DAMAGE_MULT = 1.8;
	public static double OMNI_TIER0_DAMAGE_MULT = 3.0;

	public static float HEAVY_TIER2_HARDNESS = 30.0F;
	public static float HEAVY_TIER1_HARDNESS = 10.0F;
	public static float HEAVY_TIER0_HARDNESS = 3.0F;
	public static double HEAVY_TIER1_DAMAGE_MULT = 2.5;
	public static double HEAVY_TIER0_DAMAGE_MULT = 5.0;

	public static void execute(LevelAccessor world, Entity entity, double startY, double endY, double radius) {
		if (entity == null) return;
		execute(world, entity, entity.getX(), startY, entity.getZ(), entity.getX(), endY, entity.getZ(), radius, 0);
	}

	public static void execute(LevelAccessor world, Entity entity, double startX, double startY, double startZ, double endX, double endY, double endZ, double radius) {
		execute(world, entity, startX, startY, startZ, endX, endY, endZ, radius, 0);
	}

	public static void execute(LevelAccessor world, Entity entity, double startX, double startY, double startZ, double endX, double endY, double endZ, double radius, boolean isHeavyPunchTarget) {
		execute(world, entity, startX, startY, startZ, endX, endY, endZ, radius, isHeavyPunchTarget ? 2 : 0);
	}

	public static double sqrDistanceToSegment(double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
		double abx = bx - ax;
		double aby = by - ay;
		double abz = bz - az;
		double ab2 = abx * abx + aby * aby + abz * abz;
		if (ab2 < 0.0001) {
			double ddx = px - ax;
			double ddy = py - ay;
			double ddz = pz - az;
			return ddx * ddx + ddy * ddy + ddz * ddz;
		}
		double apx = px - ax;
		double apy = py - ay;
		double apz = pz - az;
		double t = (apx * abx + apy * aby + apz * abz) / ab2;
		if (t < 0.0) t = 0.0;
		else if (t > 1.0) t = 1.0;
		double cx = ax + t * abx;
		double cy = ay + t * aby;
		double cz = az + t * abz;
		double dx = px - cx;
		double dy = py - cy;
		double dz = pz - cz;
		return dx * dx + dy * dy + dz * dz;
	}

	public static void execute(LevelAccessor world, Entity entity, double startX, double startY, double startZ, double endX, double endY, double endZ, double radius, int mode) {
		if (!ENABLED || entity == null || world == null) return;
		if (!(world instanceof Level level)) return;

		int tier = determineToughnessTier(entity);

		int minX = (int) Math.floor(Math.min(startX, endX) - radius - 1.0);
		int maxX = (int) Math.ceil(Math.max(startX, endX) + radius + 1.0);
		int minY = (int) Math.floor(Math.min(startY, endY));
		int maxY = (int) Math.ceil(Math.max(startY, endY) + entity.getBbHeight() + 0.5);
		int minZ = (int) Math.floor(Math.min(startZ, endZ) - radius - 1.0);
		int maxZ = (int) Math.ceil(Math.max(startZ, endZ) + radius + 1.0);

		double totalDamage = 0.0;
		double maxHardnessForModeTier = Float.MAX_VALUE;
		if (mode == 2) {
			maxHardnessForModeTier = (tier == 2) ? HEAVY_TIER2_HARDNESS : ((tier == 1) ? HEAVY_TIER1_HARDNESS : HEAVY_TIER0_HARDNESS);
		} else if (mode == 1) {
			maxHardnessForModeTier = (tier == 2) ? OMNI_TIER2_HARDNESS : ((tier == 1) ? OMNI_TIER1_HARDNESS : OMNI_TIER0_HARDNESS);
		} else {
			maxHardnessForModeTier = (tier == 2) ? MINOS_TIER2_HARDNESS : ((tier == 1) ? MINOS_TIER1_HARDNESS : MINOS_TIER0_HARDNESS);
		}

		double rSqr = (radius + 0.5) * (radius + 0.5);
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					double sqrDist = sqrDistanceToSegment(x + 0.5, y + 0.5, z + 0.5, startX, startY + 0.5, startZ, endX, endY + 0.5, endZ);
					if (sqrDist > rSqr) {
						continue;
					}

					mutablePos.set(x, y, z);
					BlockState state = level.getBlockState(mutablePos);

					if (state.isAir() || state.canBeReplaced()) {
						continue;
					}

					float destroySpeed = state.getDestroySpeed(level, mutablePos);
					boolean isUnbreakable = (destroySpeed < 0.0F || destroySpeed >= 50.0F);

					if (isUnbreakable) {
						continue;
					}

					if (destroySpeed <= maxHardnessForModeTier) {
						level.destroyBlock(mutablePos, false);
						double mult = 1.0;
						if (mode == 2) mult = (tier == 1) ? HEAVY_TIER1_DAMAGE_MULT : HEAVY_TIER0_DAMAGE_MULT;
						else if (mode == 1) mult = (tier == 1) ? OMNI_TIER1_DAMAGE_MULT : OMNI_TIER0_DAMAGE_MULT;
						else mult = (tier == 1) ? MINOS_TIER1_DAMAGE_MULT : MINOS_TIER0_DAMAGE_MULT;
						totalDamage += Math.max(1.0, destroySpeed * mult);

						if (level instanceof ServerLevel serverLevel) {
							if (tier == 2) {
								serverLevel.sendParticles(ParticleTypes.EXPLOSION, x + 0.5, y + 0.5, z + 0.5, 1, 0.1, 0.1, 0.1, 0.0);
								serverLevel.sendParticles(ParticleTypes.CRIT, x + 0.5, y + 0.5, z + 0.5, 3, 0.2, 0.2, 0.2, 0.05);
							} else {
								serverLevel.sendParticles(ParticleTypes.CRIT, x + 0.5, y + 0.5, z + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
							}
						}
					}
				}
			}
		}

		if (totalDamage > 0.0 && entity instanceof LivingEntity living) {
			try {
				living.hurt(level.damageSources().flyIntoWall(), (float) Math.min(totalDamage, 40.0));
			} catch (Exception e) {
				living.hurt(living.damageSources().generic(), (float) Math.min(totalDamage, 40.0));
			}
		}
	}

	public static int determineToughnessTier(Entity entity) {
		if (entity == null) return 1;

		String entityTypeStr = entity.getType().toString().toLowerCase();
		boolean isRot = entity instanceof RotEntity || entityTypeStr.contains("rot") || entity.getPersistentData().contains("rot_") || entity.getPersistentData().getBoolean("is_rot");
		if (isRot) {
			return 2;
		}

		if (entity instanceof Player) {
			return 1;
		}

		if (entity.getTags().contains("soft_entity") || entity.getTags().contains("tier_0") || entity.getPersistentData().getBoolean("soft_entity")) {
			return 0;
		}
		if (entity.getTags().contains("heavy_entity") || entity.getTags().contains("tier_2") || entity.getPersistentData().getBoolean("heavy_entity")) {
			return 2;
		}
		if (entity.getTags().contains("tier_1")) {
			return 1;
		}

		if (!(entity instanceof LivingEntity living)) {
			return 0;
		}

		double maxHealth = living.getMaxHealth();

		AABB bb = living.getBoundingBox();
		double volume = (bb != null) ? (bb.getXsize() * bb.getYsize() * bb.getZsize()) : 1.0;

		double armor = living.getArmorValue();

		double attackDamage = 0.0;
		try {
			var attackAttr = living.getAttribute(Attributes.ATTACK_DAMAGE);
			if (attackAttr != null) {
				attackDamage = attackAttr.getValue();
			}
		} catch (Exception ignored) {}

		double toughnessScore = (maxHealth * 0.5) + (volume * 5.0) + (armor * 2.0) + (attackDamage * 1.5);

		if (toughnessScore < 15.0 || maxHealth <= 10.0) {
			return 0;
		} else if (toughnessScore >= 75.0 || maxHealth >= 100.0) {
			return 2;
		} else {
			return 1;
		}
	}
} // 1.21.1
