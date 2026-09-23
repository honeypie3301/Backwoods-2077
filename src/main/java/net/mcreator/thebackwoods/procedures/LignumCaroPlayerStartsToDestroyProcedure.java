package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

public class LignumCaroPlayerStartsToDestroyProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double chance = 0;
		boolean isWeapon = false;
		isWeapon = (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).is(ItemTags.create(ResourceLocation.parse("minecraft:swords")))
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).is(ItemTags.create(ResourceLocation.parse("minecraft:axes")));
		if (!world.isClientSide()) {
			chance = Math.random();
			if (isWeapon) {
				if (chance < 0.5) {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.sculk_shrieker.shriek")), SoundSource.BLOCKS, (float) 0.3, 3);
						} else {
							_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.sculk_shrieker.shriek")), SoundSource.BLOCKS, (float) 0.3, 3, false);
						}
					}
				}
				if (chance < 0.022) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 1, false, false));
				} else if (chance < 0.051) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20, 1, false, false));
				} else if (chance < 0.07) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2, false, false));
				}
			} else {
				if (chance < 0.15) {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.sculk_shrieker.shriek")), SoundSource.BLOCKS, (float) 0.3, 3);
						} else {
							_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.sculk_shrieker.shriek")), SoundSource.BLOCKS, (float) 0.3, 3, false);
						}
					}
				}
				if (chance < 0.02) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 1, false, false));
				} else if (chance < 0.045) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20, 1, false, false));
				} else if (chance < 0.06) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2, false, false));
				}
			}
		}
	}
}