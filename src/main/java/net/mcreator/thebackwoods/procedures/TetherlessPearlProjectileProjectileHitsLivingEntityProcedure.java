package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.entity.RotEntity;

public class TetherlessPearlProjectileProjectileHitsLivingEntityProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity immediatesourceentity, Entity sourceentity) {
		if (entity == null || immediatesourceentity == null || sourceentity == null)
			return;
		double playerX = 0;
		double playerY = 0;
		double playerZ = 0;
		double targetX = 0;
		double targetY = 0;
		double targetZ = 0;
		if (!(entity instanceof RotEntity) && !(entity == sourceentity)) {
			if (sourceentity.isAlive()) {
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.glass.break")), SoundSource.AMBIENT, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.glass.break")), SoundSource.AMBIENT, 1, 1, false);
					}
				}
				playerX = sourceentity.getX();
				playerY = sourceentity.getY();
				playerZ = sourceentity.getZ();
				targetX = entity.getX();
				targetY = entity.getY();
				targetZ = entity.getZ();
				{
					Entity _ent = sourceentity;
					_ent.teleportTo(targetX, targetY, targetZ);
					if (_ent instanceof ServerPlayer _serverPlayer)
						_serverPlayer.connection.teleport(targetX, targetY, targetZ, _ent.getYRot(), _ent.getXRot());
				}
				{
					Entity _ent = entity;
					_ent.teleportTo(playerX, playerY, playerZ);
					if (_ent instanceof ServerPlayer _serverPlayer)
						_serverPlayer.connection.teleport(playerX, playerY, playerZ, _ent.getYRot(), _ent.getXRot());
				}
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(targetX, targetY, targetZ), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.enderman.teleport")), SoundSource.AMBIENT, 1, (float) 1.2);
					} else {
						_level.playLocalSound(targetX, targetY, targetZ, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.enderman.teleport")), SoundSource.AMBIENT, 1, (float) 1.2, false);
					}
				}
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(playerX, playerY, playerZ), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.enderman.teleport")), SoundSource.AMBIENT, 1, (float) 0.8);
					} else {
						_level.playLocalSound(playerX, playerY, playerZ, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.enderman.teleport")), SoundSource.AMBIENT, 1, (float) 0.8, false);
					}
				}
				if (!immediatesourceentity.level().isClientSide())
					immediatesourceentity.discard();
			}
		}
	}
}