package net.mcreator.thebackwoods.block;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;

public class FadedBlockBlock extends Block {
	public FadedBlockBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.EMPTY).strength(0.35f, 0.8f).instrument(NoteBlockInstrument.CHIME));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 14;
	}
}