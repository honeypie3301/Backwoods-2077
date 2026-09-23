package net.mcreator.thebackwoods.block;

import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.LignumCaroTrapdoorEntityWalksOnTheBlockProcedure;
import net.mcreator.thebackwoods.procedures.LignumCaroPlayerStartsToDestroyProcedure;

public class LignumCaroTrapdoorBlock extends TrapDoorBlock {
	public static final BooleanProperty INFECTED = BooleanProperty.create("infected");

	public LignumCaroTrapdoorBlock() {
		super(BlockSetType.OAK,
				BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.5f, 1.5f).noOcclusion().randomTicks().isRedstoneConductor((bs, br, bp) -> false).ignitedByLava().instrument(NoteBlockInstrument.BASS));
		this.registerDefaultState(this.stateDefinition.any().setValue(INFECTED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(INFECTED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(INFECTED, false);
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 1;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 2;
	}

	@Override
	public PathType getBlockPathType(BlockState state, BlockGetter world, BlockPos pos, Mob entity) {
		return PathType.TRAPDOOR;
	}

	@Override
	public void attack(BlockState blockstate, Level world, BlockPos pos, Player entity) {
		super.attack(blockstate, world, pos, entity);
		LignumCaroPlayerStartsToDestroyProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ(), entity);
	}

	@Override
	public void stepOn(Level world, BlockPos pos, BlockState blockstate, Entity entity) {
		super.stepOn(world, pos, blockstate, entity);
		LignumCaroTrapdoorEntityWalksOnTheBlockProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}
}