package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import net.mcreator.thebackwoods.TheBackwoodsMod;

public class TheSubStrataPlayerEntersDimensionProcedure {
	public static void execute(LevelAccessor world, double x, double z, Entity entity) {
		if (entity == null)
			return;
		TheBackwoodsMod.queueServerWork(2, () -> {
			{
				Entity _ent = entity;
				_ent.teleportTo(x, (world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z) + 1), z);
				if (_ent instanceof ServerPlayer _serverPlayer)
					_serverPlayer.connection.teleport(x, (world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z) + 1), z, _ent.getYRot(), _ent.getXRot());
			}
		});
	}
}