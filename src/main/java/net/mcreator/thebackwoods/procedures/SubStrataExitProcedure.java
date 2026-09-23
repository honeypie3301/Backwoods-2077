package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.TheBackwoodsMod;

import javax.annotation.Nullable;

@EventBusSubscriber
public class SubStrataExitProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!world.isClientSide()) {
			if (world.getLevelData().getGameTime() % 15 == 0) {
				if (entity.getY() < -64) {
					if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_sub_strata"))) {
						if (entity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
							ResourceKey<Level> destinationType = Level.OVERWORLD;
							if (_player.level().dimension() == destinationType)
								return;
							ServerLevel nextLevel = _player.server.getLevel(destinationType);
							if (nextLevel != null) {
								_player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
								_player.teleportTo(nextLevel, _player.getX(), _player.getY(), _player.getZ(), _player.getYRot(), _player.getXRot());
								_player.connection.send(new ClientboundPlayerAbilitiesPacket(_player.getAbilities()));
								for (MobEffectInstance _effectinstance : _player.getActiveEffects())
									_player.connection.send(new ClientboundUpdateMobEffectPacket(_player.getId(), _effectinstance, false));
								_player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
							}
						}
						TheBackwoodsMod.queueServerWork(1, () -> {
							{
								Entity _ent = entity;
								_ent.teleportTo((entity.getX()), entity.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) entity.getX(), (int) entity.getZ()) + 1, (entity.getZ()));
								if (_ent instanceof ServerPlayer _serverPlayer)
									_serverPlayer.connection.teleport((entity.getX()), entity.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) entity.getX(), (int) entity.getZ()) + 1,
											(entity.getZ()), _ent.getYRot(), _ent.getXRot());
							}
						});
					}
				}
			}
		}
	}
}