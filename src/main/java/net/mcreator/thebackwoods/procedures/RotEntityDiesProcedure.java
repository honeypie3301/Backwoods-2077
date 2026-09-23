package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;

public class RotEntityDiesProcedure {
	public static void execute(Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof Player) {
			if (sourceentity instanceof ServerPlayer _player) {
				AdvancementHolder _adv = _player.server.getAdvancements().get(ResourceLocation.parse("the_backwoods:rot_boss"));
				if (_adv != null) {
					AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
					if (!_ap.isDone()) {
						for (String criteria : _ap.getRemainingCriteria())
							_player.getAdvancements().award(_adv, criteria);
					}
				}
			}
		}
		entity.getPersistentData().putDouble("sentinel_solar_fire_ticks", 0);
		entity.getPersistentData().putDouble("rot_overhead_ticks", 0);
		entity.getPersistentData().putDouble("rot_armor_rip_ticks", 0);
		entity.getPersistentData().putDouble("rot_block_active_ticks", 0);
		entity.getPersistentData().putDouble("master_kill_target_id", 0);
		entity.getPersistentData().putDouble("sentinel_evaporation_windup", 0);
		entity.getPersistentData().putDouble("sentinel_evaporation_ticks", 0);
		entity.getPersistentData().putDouble("current_combat_action_state", 0);
		entity.getPersistentData().putDouble("current_action_commitment", 0);
		entity.getPersistentData().putBoolean("ai_is_attacking", false);
		entity.getPersistentData().putBoolean("is_blocking", false);
		entity.getPersistentData().putBoolean("is_falling_heavy", false);
		entity.getPersistentData().putBoolean("is_armor_ripping", false);
		entity.getPersistentData().putString("overhead_target_uuid", "");
		entity.getPersistentData().putString("master_target_queue", "");
	}
}