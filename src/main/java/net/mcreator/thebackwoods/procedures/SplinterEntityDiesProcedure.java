package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;

public class SplinterEntityDiesProcedure {
	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (!(entity.getPersistentData().getString("SplinterUsername")).equals("")) {
			if (world instanceof ServerLevel _level) {
				_level.getServer().getPlayerList().broadcastSystemMessage(Component.literal((entity.getPersistentData().getString("SplinterUsername") + " was slain by " + sourceentity.getDisplayName().getString())), false);
			}
		}
	}
}