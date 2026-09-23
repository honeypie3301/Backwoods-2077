package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class RotDisplayCondition7Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) <= 70) {
			return true;
		}
		return false;
	}
}