package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

public class LignumTrilobitaEntityIsHurt2Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.hit")), SoundSource.HOSTILE, (float) 0.9, (float) 0.8);
			} else {
				_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.hit")), SoundSource.HOSTILE, (float) 0.9, (float) 0.8, false);
			}
		}
	}
}