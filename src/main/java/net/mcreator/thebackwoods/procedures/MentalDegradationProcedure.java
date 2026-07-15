package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
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
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import net.mcreator.thebackwoods.network.TheBackwoodsModVariables;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import javax.annotation.Nullable;

@EventBusSubscriber
public class MentalDegradationProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double random_sound = 0;
		double targetX = 0;
		double targetZ = 0;
		double targetY = 0;
		double adaptation_multiplier = 0;
		if (world.getLevelData().getGameTime() % 20 == 0) {
			adaptation_multiplier = 0.15;
			if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:backwoods"))) {
				entity.getPersistentData().putDouble("backwoods_time", (entity.getPersistentData().getDouble("backwoods_time") + 20));
				if (entity.getPersistentData().getDouble("backwoods_time") >= 31200 * (1 + entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).backwoods_adaptation * adaptation_multiplier)) {
					if (entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).music_stopped_s4 == 0) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"stopsound @s master");
						{
							TheBackwoodsModVariables.PlayerVariables _vars = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES);
							_vars.music_stopped_s4 = 1;
							_vars.markSyncDirty();
						}
					}
					if (Math.random() < 0.0032) {
						if (entity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
							ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_grain"));
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
					}
					if (Math.random() < 0.002666) {
						if (entity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
							ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:loss"));
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
					}
					if (Math.random() < 1.332) {
						targetX = entity.getX() + Mth.nextInt(RandomSource.create(), -3, 3);
						targetY = entity.getY() + Mth.nextInt(RandomSource.create(), -1, 3);
						targetZ = entity.getZ() + Mth.nextInt(RandomSource.create(), -3, 3);
						if ((world.getBlockState(BlockPos.containing(targetX, targetY, targetZ))).getBlock() == Blocks.OAK_PLANKS) {
							world.setBlock(BlockPos.containing(targetX, targetY, targetZ), TheBackwoodsModBlocks.ROTTEN_OAK_PLANKS.get().defaultBlockState(), 3);
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null, BlockPos.containing(targetX, targetY, targetZ), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.place")), SoundSource.BLOCKS, 1, 1);
								} else {
									_level.playLocalSound(targetX, targetY, targetZ, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.place")), SoundSource.BLOCKS, 1, 1, false);
								}
							}
						}
					}
				} else if (entity.getPersistentData().getDouble("backwoods_time") >= 21840 * (1 + entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).backwoods_adaptation * adaptation_multiplier)) {
					if (entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).music_stopped_s3 == 0) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"stopsound @s music");
						{
							TheBackwoodsModVariables.PlayerVariables _vars = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES);
							_vars.music_stopped_s3 = 1;
							_vars.markSyncDirty();
						}
					}
					if (Math.random() < 0.00094) {
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 1, false, false));
					}
					if (Math.random() < 0.04) {
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:degradation_o1")), SoundSource.MUSIC, (float) 0.67, 1);
							} else {
								_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:degradation_o1")), SoundSource.MUSIC, (float) 0.67, 1, false);
							}
						}
					}
					if (Math.random() < 0.0138) {
						entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((x + Mth.nextDouble(RandomSource.create(), -5, 5)), (y + Mth.nextDouble(RandomSource.create(), -5, 5)), (z + Mth.nextDouble(RandomSource.create(), -5, 5))));
					}
					if (Math.random() < 0.01332) {
						random_sound = Mth.nextInt(RandomSource.create(), 0, 4);
						if (random_sound == 0) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"stopsound @s ambient");
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null, BlockPos.containing(entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5), entity.getY(), entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
											BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.wood.place")), SoundSource.BLOCKS, 3, (float) 0.5);
								} else {
									_level.playLocalSound((entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5)), (entity.getY()), (entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
											BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.wood.place")), SoundSource.BLOCKS, 3, (float) 0.5, false);
								}
							}
						} else if (random_sound == 1) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"stopsound @s ambient");
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null, BlockPos.containing(entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5), entity.getY(), entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
											BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.chest.open")), SoundSource.BLOCKS, 1, 1);
								} else {
									_level.playLocalSound((entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5)), (entity.getY()), (entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
											BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.chest.open")), SoundSource.BLOCKS, 1, 1, false);
								}
							}
						} else if (random_sound == 2) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"stopsound @s ambient");
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null,
											BlockPos.containing(entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5), entity.getY() + Mth.nextDouble(RandomSource.create(), -1, 5), entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
											BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.chest.close")), SoundSource.BLOCKS, 1, 1);
								} else {
									_level.playLocalSound((entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5)), (entity.getY() + Mth.nextDouble(RandomSource.create(), -1, 5)), (entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
											BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.chest.close")), SoundSource.BLOCKS, 1, 1, false);
								}
							}
						} else if (random_sound == 3) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"stopsound @s ambient");
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null,
											BlockPos.containing(entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5), entity.getY() + Mth.nextDouble(RandomSource.create(), -3, 5), entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
											BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bell.resonate")), SoundSource.BLOCKS, 2, (float) 0.6);
								} else {
									_level.playLocalSound((entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5)), (entity.getY() + Mth.nextDouble(RandomSource.create(), -3, 5)), (entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
											BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bell.resonate")), SoundSource.BLOCKS, 2, (float) 0.6, false);
								}
							}
						} else if (random_sound == 4) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"stopsound @s ambient");
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null,
											BlockPos.containing(entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5), entity.getY() + Mth.nextDouble(RandomSource.create(), -4, 5), entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
											BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bell.use")), SoundSource.BLOCKS, 2, (float) 0.6);
								} else {
									_level.playLocalSound((entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5)), (entity.getY() + Mth.nextDouble(RandomSource.create(), -4, 5)), (entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
											BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bell.use")), SoundSource.BLOCKS, 2, (float) 0.6, false);
								}
							}
						}
					}
				} else if (entity.getPersistentData().getDouble("backwoods_time") >= 12480 * (1 + entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).backwoods_adaptation * adaptation_multiplier)) {
					if (entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).music_stopped_s2 == 0) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"stopsound @s music");
						{
							TheBackwoodsModVariables.PlayerVariables _vars = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES);
							_vars.music_stopped_s2 = 1;
							_vars.markSyncDirty();
						}
					}
					if (Math.random() < 0.0138) {
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 1, false, false));
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.cave")), SoundSource.AMBIENT, 2, (float) 0.8);
							} else {
								_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.cave")), SoundSource.AMBIENT, 2, (float) 0.8, false);
							}
						}
					} else if (Math.random() < 0.0066) {
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper1")), SoundSource.MUSIC, (float) 0.7, 1);
							} else {
								_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper1")), SoundSource.MUSIC, (float) 0.7, 1, false);
							}
						}
					} else if (Math.random() < 0.008) {
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper3")), SoundSource.MUSIC, (float) 0.7, 1);
							} else {
								_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper3")), SoundSource.MUSIC, (float) 0.7, 1, false);
							}
						}
					} else if (Math.random() < 0.01142) {
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper2")), SoundSource.MUSIC, (float) 0.7, 1);
							} else {
								_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper2")), SoundSource.MUSIC, (float) 0.7, 1, false);
							}
						}
					} else if (Math.random() < 0.01) {
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper4")), SoundSource.MUSIC, (float) 0.7, 1);
							} else {
								_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper4")), SoundSource.MUSIC, (float) 0.7, 1, false);
							}
						}
					}
				} else if (entity.getPersistentData().getDouble("backwoods_time") >= 7800 * (1 + entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).backwoods_adaptation * adaptation_multiplier)) {
					if (Math.random() < 0.00086) {
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper1")), SoundSource.MUSIC, (float) 0.7, 1);
							} else {
								_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper1")), SoundSource.MUSIC, (float) 0.7, 1, false);
							}
						}
					} else if (Math.random() < 0.004444) {
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper5")), SoundSource.MUSIC, (float) 0.7, 1);
							} else {
								_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:whisper5")), SoundSource.MUSIC, (float) 0.7, 1, false);
							}
						}
					}
				}
				if (entity.getPersistentData().getDouble("debug_mode") == 1) {
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(
								Component.literal(("Backwoods Time: " + entity.getPersistentData().getDouble("backwoods_time") + " | Adaptation Points: " + entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).backwoods_adaptation)), true);
				}
			} else {
				{
					TheBackwoodsModVariables.PlayerVariables _vars = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES);
					_vars.music_stopped_s2 = 0;
					_vars.music_stopped_s3 = 0;
					_vars.music_stopped_s4 = 0;
					_vars.markSyncDirty();
				}
				if (entity.getPersistentData().getDouble("backwoods_time") >= 24000) {
					{
						TheBackwoodsModVariables.PlayerVariables _vars = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES);
						_vars.backwoods_adaptation = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).backwoods_adaptation + 1;
						_vars.markSyncDirty();
					}
				}
				entity.getPersistentData().putDouble("backwoods_time", 0);
			}
		}
	}
}