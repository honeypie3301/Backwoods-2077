package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

@EventBusSubscriber
public class GrainDimensionDepthEffectProcedure {
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
		if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_grain"))) {
			if (entity.getY() < -12 && entity.getY() > -50) {
				if (Math.random() < 0.000021) {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5), entity.getY(), entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
									BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.wood.place")), SoundSource.BLOCKS, 3, (float) 0.5);
						} else {
							_level.playLocalSound((entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5)), (entity.getY()), (entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
									BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.wood.place")), SoundSource.BLOCKS, 3, (float) 0.5, false);
						}
					}
				}
				if (Math.random() < 0.00005) {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5), entity.getY(), entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
									BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.chest.open")), SoundSource.BLOCKS, 1, 1);
						} else {
							_level.playLocalSound((entity.getX() + Mth.nextDouble(RandomSource.create(), -5, 5)), (entity.getY()), (entity.getZ() + Mth.nextDouble(RandomSource.create(), -5, 5)),
									BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.chest.open")), SoundSource.BLOCKS, 1, 1, false);
						}
					}
				}
				if (Math.random() < 0.00005) {
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
				}
				if (Math.random() < 0.00002) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 1, false, false));
				}
			} else if (entity.getY() < 3 && entity.getY() > -12) {
				if (Math.random() < 0.00015) {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_crack_1")), SoundSource.AMBIENT, 1, 1);
						} else {
							_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_crack_1")), SoundSource.AMBIENT, 1, 1, false);
						}
					}
				}
				if (Math.random() < 0.00008) {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_crack_2")), SoundSource.AMBIENT, 1, 1);
						} else {
							_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_crack_2")), SoundSource.AMBIENT, 1, 1, false);
						}
					}
				}
				if (Math.random() < 0.00006) {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_crack_4")), SoundSource.AMBIENT, 1, 1);
						} else {
							_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_crack_4")), SoundSource.AMBIENT, 1, 1, false);
						}
					}
				}
				if (Math.random() < 0.00001) {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_crack_5")), SoundSource.AMBIENT, 1, 1);
						} else {
							_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_crack_5")), SoundSource.AMBIENT, 1, 1, false);
						}
					}
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 1, false, false));
				}
			}
		}
	}
}