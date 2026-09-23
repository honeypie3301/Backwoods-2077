package net.mcreator.thebackwoods.block;

import net.neoforged.neoforge.common.util.DeferredSoundType;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class PetrifiedRottenOakStairsBlock extends StairBlock {
	public PetrifiedRottenOakStairsBlock() {
		super(Blocks.AIR.defaultBlockState(),
				BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
						.sound(new DeferredSoundType(1.0f, 1.0f, () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.wood.break")), () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:rotten_planks_step")),
								() -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.place")), () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:rotten_planks_break")),
								() -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.place"))))
						.strength(10f, 9f).friction(0.8f).instrument(NoteBlockInstrument.DIDGERIDOO));
	}

	@Override
	public float getExplosionResistance() {
		return 9f;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 1;
	}
}