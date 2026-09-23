package net.mcreator.thebackwoods.procedures;

import net.mcreator.thebackwoods.entity.LignumGigasEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class LignumGigasOnEntityTickUpdateProcedure {
	private static final int EFFECT_TICK = 50;
	private static final int PUSH_INTERVAL = 3;

	private static final int RUMBLE_INTERVAL_TICKS = 24;
	private static final float RUMBLE_VOLUME = 2.7f;
	private static final float RUMBLE_PITCH = 0.48f;

	// Heavy sink on spawn
	private static final int SINK_TOTAL_STEPS = 5;
	private static final int SINK_INTERVAL_TICKS = 10;
	private static final int SINK_RADIUS = 9;
	private static final float SINK_MAX_BREAK_HARDNESS = 40.0f; // do not break >= 40

	// =========================================================================
	// CONFIGURABLE FLING POWER SETTINGS
	// Adjust these constants to tune Gigas fling strength!
	// =========================================================================
	private static final double FLING_POWER_MULTIPLIER_H = 2.0; // Global multiplier for horizontal push
	private static final double FLING_POWER_MULTIPLIER_V = 1.8; // Global multiplier for vertical launch
	private static final double MIN_FLING_FALLOFF = 0.25;       // Minimum falloff floor at the aura boundary

	private static final double BASE_PUSH_H_EASY = 2.2;
	private static final double BASE_PUSH_H_NORMAL = 3.6;
	private static final double BASE_PUSH_H_HARD = 5.0;

	private static final double BASE_PUSH_V_EASY = 0.6;
	private static final double BASE_PUSH_V_NORMAL = 0.95;
	private static final double BASE_PUSH_V_HARD = 1.4;

	public static void execute() { }

	public static void execute(LignumGigasEntity gigas) {
		if (gigas == null) return;
		Level level = gigas.level();
		if (level.isClientSide()) return;

		// Prevent the giant boss from being pushed horizontally by players or collision physics
		Vec3 delta = gigas.getDeltaMovement();
		if (delta.x != 0 || delta.z != 0) {
			gigas.setDeltaMovement(0, delta.y, 0);
			gigas.hurtMarked = true;
		}

		if (!gigas.getEntityData().get(LignumGigasEntity.DATA_seq_active)) return;

		int t = gigas.getEntityData().get(LignumGigasEntity.DATA_seq_t) + 1;
		gigas.getEntityData().set(LignumGigasEntity.DATA_seq_t, t);

		// ---- heavy sink sequence (ONE TIME EVER for this entity) ----
		boolean sinkDoneForever = gigas.getPersistentData().getBoolean("bw_sink_done_forever");
		if (!sinkDoneForever) {
			int sinkStep = gigas.getPersistentData().getInt("bw_sink_step");
			if (sinkStep < SINK_TOTAL_STEPS && t % SINK_INTERVAL_TICKS == 0) {
				int targetY = (int) Math.floor(gigas.getY()) - 1;
				carveRoughLayer(level, (int) Math.floor(gigas.getX()), targetY, (int) Math.floor(gigas.getZ()), sinkStep);

				gigas.setPos(gigas.getX(), gigas.getY() - 1.0, gigas.getZ());
				gigas.hurtMarked = true;

				sinkStep++;
				gigas.getPersistentData().putInt("bw_sink_step", sinkStep);

				if (sinkStep >= SINK_TOTAL_STEPS) {
					gigas.getPersistentData().putBoolean("bw_sink_done_forever", true);
				}
			}
		}

		// ---- Telegraph phase leading up to EFFECT_TICK ----
		boolean done = gigas.getEntityData().get(LignumGigasEntity.DATA_effect_done);
		if (!done && t >= EFFECT_TICK - 15 && t < EFFECT_TICK) {
			if (level instanceof ServerLevel serverLevel && t % 2 == 0) {
				serverLevel.sendParticles(
						ParticleTypes.WITCH,
						gigas.getX(), gigas.getY() + 3.0, gigas.getZ(),
						25, 6.0, 3.0, 6.0, 0.15
				);
				serverLevel.sendParticles(
						ParticleTypes.ENCHANTED_HIT,
						gigas.getX(), gigas.getY() + 5.0, gigas.getZ(),
						15, 8.0, 4.0, 8.0, 0.2
				);
			}
			if (t % 3 == 0) {
				float progress = (float) (t - (EFFECT_TICK - 15)) / 15.0f;
				float pitch = 0.5f + progress * 0.9f;
				level.playSound(null, BlockPos.containing(gigas.getX(), gigas.getY(), gigas.getZ()),
						SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 2.0f, pitch);
			}
		}

		// ---- One-shot inventory strip trigger ----
		if (!done && t >= EFFECT_TICK) {
			gigas.getEntityData().set(LignumGigasEntity.DATA_effect_done, true);

			// Distinct sound for one-shot inventory strip moment (differentiated from ambient rumble)
			level.playSound(null, BlockPos.containing(gigas.getX(), gigas.getY(), gigas.getZ()),
					SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 5.0f, 0.85f);

			dropPlayersInventoryNoArmor(level, gigas);
		}

		int soundCd = gigas.getEntityData().get(LignumGigasEntity.DATA_aura_sound_cd);
		if (soundCd <= 0) {
			level.playSound(null, BlockPos.containing(gigas.getX(), gigas.getY(), gigas.getZ()),
					SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, RUMBLE_VOLUME, RUMBLE_PITCH);
			gigas.getEntityData().set(LignumGigasEntity.DATA_aura_sound_cd, RUMBLE_INTERVAL_TICKS);
		} else {
			gigas.getEntityData().set(LignumGigasEntity.DATA_aura_sound_cd, soundCd - 1);
		}

		if (t % PUSH_INTERVAL == 0) {
			flingAll(level, gigas);
		}

		int auraLeft = gigas.getEntityData().get(LignumGigasEntity.DATA_aura_time_left) - 1;
		gigas.getEntityData().set(LignumGigasEntity.DATA_aura_time_left, auraLeft);

		if (auraLeft <= 0) {
			gigas.getEntityData().set(LignumGigasEntity.DATA_play_attack_anim, false);
			gigas.getEntityData().set(LignumGigasEntity.DATA_play_lower_anim, true);

			gigas.getEntityData().set(LignumGigasEntity.DATA_seq_active, false);
			gigas.getEntityData().set(LignumGigasEntity.DATA_seq_t, 0);
			gigas.getEntityData().set(LignumGigasEntity.DATA_effect_done, false);
			gigas.getEntityData().set(LignumGigasEntity.DATA_aura_time_left, 0);
			gigas.getEntityData().set(LignumGigasEntity.DATA_aura_sound_cd, 0);

			// do NOT reset bw_sink_step / bw_sink_done_forever
		}
	}

	private static double getAuraRadius(Level level) {
		Difficulty diff = level.getDifficulty();
		if (diff == Difficulty.EASY) return 18.0;
		if (diff == Difficulty.NORMAL) return 28.0;
		if (diff == Difficulty.HARD) return 38.0;
		return 28.0;
	}

	private static double getPushH(Level level) {
		Difficulty diff = level.getDifficulty();
		double base = switch (diff) {
			case EASY -> BASE_PUSH_H_EASY;
			case HARD -> BASE_PUSH_H_HARD;
			default -> BASE_PUSH_H_NORMAL;
		};
		return base * FLING_POWER_MULTIPLIER_H;
	}

	private static double getPushV(Level level) {
		Difficulty diff = level.getDifficulty();
		double base = switch (diff) {
			case EASY -> BASE_PUSH_V_EASY;
			case HARD -> BASE_PUSH_V_HARD;
			default -> BASE_PUSH_V_NORMAL;
		};
		return base * FLING_POWER_MULTIPLIER_V;
	}

	private static boolean hasLineOfSight(Level level, Entity source, Entity target) {
		Vec3 start = source.getEyePosition();
		Vec3 end = target.getEyePosition();
		return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target)).getType() == HitResult.Type.MISS;
	}

	private static boolean isLavaHazard(Level level, BlockPos p) {
		if (isLava(level, p.below())) return true;
		for (Direction dir : Direction.values()) {
			if (isLava(level, p.relative(dir))) return true;
		}
		return false;
	}

	private static boolean isLava(Level level, BlockPos pos) {
		return level.getFluidState(pos).is(FluidTags.LAVA) || level.getBlockState(pos).is(Blocks.LAVA);
	}

	private static boolean isPlayerStandingOn(Level level, BlockPos p) {
		for (Player player : level.players()) {
			if (player != null && player.isAlive() && !player.isSpectator()) {
				BlockPos playerPos = player.blockPosition();
				if (playerPos.equals(p) || playerPos.below().equals(p) ||
						(Math.abs(player.getX() - (p.getX() + 0.5)) < 0.8 &&
								Math.abs(player.getZ() - (p.getZ() + 0.5)) < 0.8 &&
								Math.abs(player.getY() - (p.getY() + 1.0)) < 1.2)) {
					return true;
				}
			}
		}
		return false;
	}

	private static void carveRoughLayer(Level level, int cx, int y, int cz, int step) {
		long seed = (cx * 73428767L) ^ (cz * 912931L) ^ (y * 19349663L) ^ (step * 83492791L);

		for (int dx = -SINK_RADIUS; dx <= SINK_RADIUS; dx++) {
			for (int dz = -SINK_RADIUS; dz <= SINK_RADIUS; dz++) {
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist > SINK_RADIUS + 0.35) continue;

				double edgeFactor = dist / (double) SINK_RADIUS;
				double keepChance = 0.08 + (edgeFactor * 0.45);

				long h = seed + dx * 341873128712L + dz * 132897987541L;
				double r = ((h ^ (h >>> 13) ^ (h << 7)) & 1023) / 1023.0;
				if (r < keepChance) continue;

				BlockPos p = new BlockPos(cx + dx, y, cz + dz);
				if (!level.isEmptyBlock(p)) {
					float hardness = level.getBlockState(p).getDestroySpeed(level, p);
					if (hardness < 0.0f || hardness >= SINK_MAX_BREAK_HARDNESS) continue;

					// Hazard check: skip carving if player is standing on block or carving exposes/touches lava
					if (isPlayerStandingOn(level, p) || isLavaHazard(level, p)) continue;

					level.destroyBlock(p, false);
				}
			}
		}
	}

	private static void dropPlayersInventoryNoArmor(Level level, LignumGigasEntity g) {
		double auraRadius = getAuraRadius(level);
		List<Player> players = level.getEntitiesOfClass(
				Player.class,
				new AABB(new Vec3(g.getX(), g.getY(), g.getZ()), new Vec3(g.getX(), g.getY(), g.getZ())).inflate(auraRadius),
				p -> p != null && p.isAlive() && !p.isCreative() && !p.isSpectator()
		);
		for (Player p : players) {
			if (hasLineOfSight(level, g, p)) {
				dropPlayerInventoryNoArmor(p);
			}
		}
	}

	private static void flingAll(Level level, LignumGigasEntity g) {
		double cx = g.getX(), cy = g.getY(), cz = g.getZ();
		double auraRadius = getAuraRadius(level);
		double maxPushH = getPushH(level);
		double maxPushV = getPushV(level);

		List<Entity> entities = level.getEntitiesOfClass(
				Entity.class,
				new AABB(new Vec3(cx, cy, cz), new Vec3(cx, cy, cz)).inflate(auraRadius),
				e -> e != null && e.isAlive() && e != g && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
		);

		for (Entity e : entities) {
			if (!hasLineOfSight(level, g, e)) continue;

			double dx = e.getX() - cx;
			double dy = e.getY() - cy;
			double dz = e.getZ() - cz;
			double totalDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

			double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
			double nx = dx / len;
			double nz = dz / len;

			// Distance-based falloff: closer entities get pushed harder
			double falloff = Math.max(MIN_FLING_FALLOFF, 1.0 - (totalDist / auraRadius));
			double pushH = maxPushH * falloff;
			double pushV = maxPushV * falloff;

			e.setDeltaMovement(nx * pushH, Math.max(pushV, e.getDeltaMovement().y + 0.15), nz * pushH);
			e.hurtMarked = true;
		}
	}

	private static void dropPlayerInventoryNoArmor(Player p) {
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			ItemStack st = p.getInventory().getItem(i);
			if (!st.isEmpty()) {
				p.drop(st.copy(), true, false);
				p.getInventory().setItem(i, ItemStack.EMPTY);
			}
		}

		ItemStack off = p.getItemBySlot(EquipmentSlot.OFFHAND);
		if (!off.isEmpty()) {
			p.drop(off.copy(), true, false);
			p.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		} // 1.21.1

		p.getInventory().setChanged();
	}
}
