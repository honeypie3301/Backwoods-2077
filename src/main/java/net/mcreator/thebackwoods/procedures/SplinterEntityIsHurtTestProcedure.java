package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.SplinterEntity;
import net.mcreator.thebackwoods.entity.LogSplinterEntity;
import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;

public class SplinterEntityIsHurtTestProcedure {
	public static void execute(Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof Player) {
			if (entity instanceof SplinterEntity _datEntSetI)
				_datEntSetI.getEntityData().set(SplinterEntity.DATA_Age, 577000);
			if (entity instanceof LogSplinterEntity _datEntSetI)
				_datEntSetI.getEntityData().set(LogSplinterEntity.DATA_Age, 430000);
			if (entity instanceof BlindspotSplinterEntity _datEntSetI)
				_datEntSetI.getEntityData().set(BlindspotSplinterEntity.DATA_Age, 577000);
		}
	}
}