package net.mcreator.thebackwoods.block;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;

public class PetrifiedWealdFoliageBlock extends LeavesBlock {
	public PetrifiedWealdFoliageBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(6f, 1.3f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false).instrument(NoteBlockInstrument.DIDGERIDOO).isSuffocating((bs, br, bp) -> false)
				.isViewBlocking((bs, br, bp) -> false));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 13;
	}
}