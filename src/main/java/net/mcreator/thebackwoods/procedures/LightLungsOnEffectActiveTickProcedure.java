package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

public class LightLungsOnEffectActiveTickProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity.tickCount % 40 == 0 && entity instanceof Player) {
			if (entity instanceof Player _player)
				_player.causeFoodExhaustion(-1);
		}
	}
}