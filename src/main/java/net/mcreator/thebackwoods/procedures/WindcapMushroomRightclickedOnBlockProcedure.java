package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class WindcapMushroomRightclickedOnBlockProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Direction direction, Entity entity, ItemStack itemstack) {
		if (direction == null || entity == null)
			return;
		if (world.isEmptyBlock(BlockPos.containing(x, y + 1, z)) && direction == Direction.UP) {
			world.setBlock(BlockPos.containing(x, y + 1, z), TheBackwoodsModBlocks.WINDCAP_MUSHROOM_PLANT.get().defaultBlockState(), 3);
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.rooted_dirt.place")), SoundSource.BLOCKS, (float) 0.7, 1);
				} else {
					_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.rooted_dirt.place")), SoundSource.BLOCKS, (float) 0.7, 1, false);
				}
			}
			if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
				itemstack.shrink(1);
			}
		}
	}
}