package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import net.mcreator.thebackwoods.entity.RotEntity;

public class RotThisEntityKillsAnotherOneProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity immediatesourceentity, Entity sourceentity) {
		if (world == null)
			return;

		RotEntity rot = null;
		Entity victim = null;

		if (entity instanceof RotEntity r) {
			rot = r;
			victim = (sourceentity != null && sourceentity != r) ? sourceentity : immediatesourceentity;
		} else if (sourceentity instanceof RotEntity r) {
			rot = r;
			victim = (entity != r) ? entity : immediatesourceentity;
		} else if (immediatesourceentity instanceof RotEntity r) {
			rot = r;
			victim = (sourceentity != null && sourceentity != r) ? sourceentity : entity;
		}

		if (rot == null)
			return;

		RotOnEntityTickUpdateProcedure.UniversalCombatPredictionEngine.recordRotKill(rot, victim);

		if (victim != null) {
			String victimUuid = victim.getUUID().toString();
			String heldUuid = getPersistentString(rot, "overhead_target_uuid");
			if (victimUuid.equals(heldUuid)) {
				rot.getPersistentData().remove("overhead_target_uuid");
				rot.getPersistentData().putDouble("rot_overhead_ticks", 0);
			}

			rot.getPersistentData().putDouble("rot_armor_rip_ticks", 0);
			rot.getPersistentData().putBoolean("is_armor_ripping", false);
			rot.getPersistentData().putDouble("current_combat_action_state", 0);
			rot.getPersistentData().putDouble("current_action_commitment", 0);
			rot.getPersistentData().putBoolean("ai_is_attacking", false);

			advanceTargetQueue(world, rot, victim);

			net.minecraft.world.entity.player.Player master = null;
			if (rot.getPersistentData().getBoolean("master_guard_mode")) {
				master = RotOnEntityTickUpdateProcedure.getGuardPlayer(world, rot);
			} else if (rot.getPersistentData().getBoolean("master_follow_enabled")) {
				master = RotOnEntityTickUpdateProcedure.getFollowPlayer(world, rot);
			}
			boolean masterInDanger = false;
			if (master != null) {
				masterInDanger = master.getHealth() < master.getMaxHealth() * 0.5F || master.hurtTime > 0 || master.getLastHurtByMob() != null;
			}
			double postKillDelay = masterInDanger ? 10.0 : (20.0 + rot.getRandom().nextDouble() * 20.0);
			rot.getPersistentData().putDouble("rot_post_kill_teleport_delay_ticks", postKillDelay);
		}
	}

	private static void advanceTargetQueue(LevelAccessor world, RotEntity rot, Entity victim) {
		int currentKillId = getPersistentInt(rot, "master_kill_target_id");
		String queueStr = getPersistentString(rot, "master_target_queue");

		if (!queueStr.isEmpty()) {
			String[] ids = queueStr.split(",");
			Entity nextTarget = null;
			StringBuilder newQueue = new StringBuilder();

			for (String idStr : ids) {
				try {
					int id = Integer.parseInt(idStr.trim());
					if (victim != null && id == victim.getId()) {
						continue;
					}
					if (world instanceof Level level) {
						Entity candidate = level.getEntity(id);
						if (candidate instanceof LivingEntity le && le.isAlive() && candidate != rot) {
							if (nextTarget == null) {
								nextTarget = candidate;
							} else {
								if (newQueue.length() > 0) newQueue.append(",");
								newQueue.append(id);
							}
						}
					}
				} catch (Exception ignored) {}
			}

			if (nextTarget != null) {
				rot.getPersistentData().putString("master_target_queue", newQueue.toString());
				rot.getPersistentData().putInt("master_kill_target_id", nextTarget.getId());
				if (rot instanceof Mob mob && nextTarget instanceof LivingEntity livingNext) {
					mob.setTarget(livingNext);
				}
			} else {
				rot.getPersistentData().remove("master_kill_target_id");
				rot.getPersistentData().remove("master_target_queue");
				rot.getPersistentData().remove("master_assassinate_player_name");
				rot.getPersistentData().remove("master_queued_assassination_player");
			}
		} else if (victim != null && victim.getId() == currentKillId) {
			rot.getPersistentData().remove("master_kill_target_id");
			rot.getPersistentData().remove("master_target_queue");
			rot.getPersistentData().remove("master_assassinate_player_name");
			rot.getPersistentData().remove("master_queued_assassination_player");
		}
	}

	private static String getPersistentString(Entity entity, String key) {
		return entity.getPersistentData().getString(key);
	}

	private static int getPersistentInt(Entity entity, String key) {
		return entity.getPersistentData().getInt(key);
	}
} // 1.21.1