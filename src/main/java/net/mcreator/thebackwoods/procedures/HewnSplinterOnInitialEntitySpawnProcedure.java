package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class HewnSplinterOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (Math.random() >= 0.0001) {
			{
				Entity _ent = entity;
				_ent.setYRot(Math.round(entity.getYRot() / 90d) * 90);
				_ent.setXRot(0);
				_ent.setYBodyRot(_ent.getYRot());
				_ent.setYHeadRot(_ent.getYRot());
				_ent.yRotO = _ent.getYRot();
				_ent.xRotO = _ent.getXRot();
				if (_ent instanceof LivingEntity _entity) {
					_entity.yBodyRotO = _entity.getYRot();
					_entity.yHeadRotO = _entity.getYRot();
				}
			}
		}
	}
}