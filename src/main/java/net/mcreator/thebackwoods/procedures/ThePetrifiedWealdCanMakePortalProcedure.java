package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

public class ThePetrifiedWealdCanMakePortalProcedure {
	public static boolean execute(LevelAccessor world, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return false;
		double successNeeded = 0;
		double roll = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheBackwoodsModItems.THE_PETRIFIED_WEALD.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheBackwoodsModItems.THE_PETRIFIED_WEALD.get()) {
			if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
				if (world instanceof ServerLevel _level) {
					itemstack.hurtAndBreak(16, _level, null, _stkprov -> {
					});
				}
			} else {
				if (world instanceof ServerLevel _level) {
					itemstack.hurtAndBreak(0, _level, null, _stkprov -> {
					});
				}
			}
			if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:backwoods"))) {
				successNeeded = 7;
			} else if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_petrified_weald"))) {
				successNeeded = 2;
			} else if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:loss"))) {
				successNeeded = 3;
			} else if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:rotting"))) {
				successNeeded = 8;
			} else if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_still"))) {
				successNeeded = 8;
			} else if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_familiar"))) {
				successNeeded = 1;
			} else if ((entity.level().dimension()) == Level.OVERWORLD) {
				successNeeded = 9;
			} else {
				successNeeded = 9;
			}
			roll = Mth.nextInt(RandomSource.create(), 1, 10);
			if (roll > successNeeded) {
				return true;
			} else {
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")), SoundSource.NEUTRAL, (float) 0.8, 1);
					} else {
						_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")), SoundSource.NEUTRAL, (float) 0.8, 1, false);
					}
				}
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SMOKE, (entity.getX()), (entity.getY()), (entity.getZ()), 15, 1, 1, 1, 0.1);
				return false;
			}
		}
		return false;
	}
}