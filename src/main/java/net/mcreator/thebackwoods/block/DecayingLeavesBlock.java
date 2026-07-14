package net.mcreator.thebackwoods.block;

import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.DecayingLeavesEntityFallsOnTheBlockProcedure;

public class DecayingLeavesBlock extends LeavesBlock {
	public DecayingLeavesBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.AZALEA).strength(0.2f).noOcclusion().pushReaction(PushReaction.DESTROY).isRedstoneConductor((bs, br, bp) -> false).ignitedByLava().isSuffocating((bs, br, bp) -> false)
				.isViewBlocking((bs, br, bp) -> false));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 14;
	}

	@Override
	public void fallOn(Level world, BlockState blockstate, BlockPos pos, Entity entity, float distance) {
		super.fallOn(world, blockstate, pos, entity, distance);
		DecayingLeavesEntityFallsOnTheBlockProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ(), entity);
	}
}