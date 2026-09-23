package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class WoodweaverEntityDiesProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putBoolean("is_dying", true);
		entity.getPersistentData().putDouble("death_start_y", (entity.getY()));
		entity.getPersistentData().putDouble("death_ticks", 0);
		if (entity instanceof LivingEntity _entity)
			_entity.setHealth((float) 0.2);
		if (entity instanceof net.minecraft.world.entity.Mob mob) {
			mob.setInvulnerable(true);
		}
	}
}