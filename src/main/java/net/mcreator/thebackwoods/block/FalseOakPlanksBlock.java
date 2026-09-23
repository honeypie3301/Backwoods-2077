package net.mcreator.thebackwoods.block;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.FalseOakPlanksEntityWalksOnTheBlockProcedure;
import net.mcreator.thebackwoods.procedures.FalseOakPlanksEntityFallsOnTheBlockProcedure;

public class FalseOakPlanksBlock extends LeavesBlock {
	public FalseOakPlanksBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(0.2f).noOcclusion().ignitedByLava().instrument(NoteBlockInstrument.BASS).isSuffocating((bs, br, bp) -> false).isViewBlocking((bs, br, bp) -> false));
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 30;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 60;
	}

	@Override
	public void stepOn(Level world, BlockPos pos, BlockState blockstate, Entity entity) {
		super.stepOn(world, pos, blockstate, entity);
		FalseOakPlanksEntityWalksOnTheBlockProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ(), entity);
	}

	@Override
	public void fallOn(Level world, BlockState blockstate, BlockPos pos, Entity entity, float distance) {
		super.fallOn(world, blockstate, pos, entity, distance);
		FalseOakPlanksEntityFallsOnTheBlockProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ(), entity);
	}
}