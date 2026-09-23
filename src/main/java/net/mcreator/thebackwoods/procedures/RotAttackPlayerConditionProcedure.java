package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

public class RotAttackPlayerConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (entity.getPersistentData().getBoolean("is_dueling") == true || (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) instanceof Player) {
			return true;
		}
		return false;
	}
}