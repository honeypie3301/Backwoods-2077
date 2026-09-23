package net.mcreator.thebackwoods.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Mob;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.BlindspotSplinterSpawnerOnTickUpdateProcedure;

public class BlindspotSplinterSpawnerBlock extends Block {
	private static final VoxelShape SHAPE = Shapes.empty();

	public BlindspotSplinterSpawnerBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).sound(SoundType.EMPTY).strength(-1, 3600000).noOcclusion().randomTicks().pushReaction(PushReaction.IGNORE).isRedstoneConductor((bs, br, bp) -> false).replaceable()
				.instrument(NoteBlockInstrument.HAT));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public PathType getBlockPathType(BlockState state, BlockGetter world, BlockPos pos, Mob entity) {
		return PathType.OPEN;
	}

	@Override
	public void randomTick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
		super.randomTick(blockstate, world, pos, random);
		BlindspotSplinterSpawnerOnTickUpdateProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}
}