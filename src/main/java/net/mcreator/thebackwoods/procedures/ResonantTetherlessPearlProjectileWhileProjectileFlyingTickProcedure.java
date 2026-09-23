package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;

public class ResonantTetherlessPearlProjectileWhileProjectileFlyingTickProcedure {
	public static void execute(double x, double y, double z, Entity entity, Entity immediatesourceentity) {
		if (entity == null || immediatesourceentity == null)
			return;
		double traveledDist = 0;
		if (immediatesourceentity.getPersistentData().getBoolean("initialized") == false) {
			immediatesourceentity.getPersistentData().putBoolean("initialized", true);
			immediatesourceentity.getPersistentData().putDouble("startX", x);
			immediatesourceentity.getPersistentData().putDouble("startY", y);
			immediatesourceentity.getPersistentData().putDouble("startZ", z);
			immediatesourceentity.getPersistentData().putDouble("maxDistance", (Mth.nextInt(RandomSource.create(), 64, 96)));
		}
		traveledDist = Math.sqrt(Math.pow(x - immediatesourceentity.getPersistentData().getDouble("startX"), 2) + Math.pow(y - immediatesourceentity.getPersistentData().getDouble("startY"), 2)
				+ Math.pow(z - immediatesourceentity.getPersistentData().getDouble("startZ"), 2));
		if (traveledDist >= immediatesourceentity.getPersistentData().getDouble("maxDistance")) {
			if (entity.isAlive()) {
				{
					Entity _ent = entity;
					_ent.teleportTo(x, y, z);
					if (_ent instanceof ServerPlayer _serverPlayer)
						_serverPlayer.connection.teleport(x, y, z, _ent.getYRot(), _ent.getXRot());
				}
				if (!immediatesourceentity.level().isClientSide())
					immediatesourceentity.discard();
			}
		}
	}
}