package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class FractusPrimeEntityIsHurtProcedure {
	public static void execute(Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity.isAlive()) {
			if (entity instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
				_entity.setTarget(_ent);
			if (entity.getPersistentData().getBoolean("telekinesis_active") == true) {
				entity.getPersistentData().putDouble("telekinesis_hits_left", (entity.getPersistentData().getDouble("telekinesis_hits_left") - 1));
			}
			if (entity.getPersistentData().getDouble("fractus_burst_cooldown") > 60) {
				entity.getPersistentData().putDouble("fractus_burst_cooldown", (entity.getPersistentData().getDouble("fractus_burst_cooldown") - 20));
			}
		}
	}
}