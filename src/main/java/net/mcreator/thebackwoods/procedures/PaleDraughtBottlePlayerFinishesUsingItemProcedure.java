package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.TheBackwoodsMod;

public class PaleDraughtBottlePlayerFinishesUsingItemProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_still"))) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 90, 6, false, false));
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.cave")), SoundSource.AMBIENT, 1, 1);
				} else {
					_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.cave")), SoundSource.AMBIENT, 1, 1, false);
				}
			}
			itemstack.shrink(1);
			TheBackwoodsMod.queueServerWork(60, () -> {
				if (entity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
					ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_familiar"));
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
				{
					Entity _ent = entity;
					_ent.teleportTo(x, (world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z) + 2), z);
					if (_ent instanceof ServerPlayer _serverPlayer)
						_serverPlayer.connection.teleport(x, (world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z) + 2), z, _ent.getYRot(), _ent.getXRot());
				}
			});
		} else if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_grain"))) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 6, false, false));
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.cave")), SoundSource.AMBIENT, 1, (float) 0.7);
				} else {
					_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.cave")), SoundSource.AMBIENT, 1, (float) 0.7, false);
				}
			}
			itemstack.shrink(1);
			TheBackwoodsMod.queueServerWork(50, () -> {
				if (entity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
					ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_still"));
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
				{
					Entity _ent = entity;
					_ent.teleportTo(x, 129, z);
					if (_ent instanceof ServerPlayer _serverPlayer)
						_serverPlayer.connection.teleport(x, 129, z, _ent.getYRot(), _ent.getXRot());
				}
			});
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("It won't work."), true);
		}
	}
}