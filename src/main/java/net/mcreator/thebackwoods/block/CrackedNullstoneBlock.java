package net.mcreator.thebackwoods.block;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.core.Direction;

import net.mcreator.thebackwoods.procedures.NullstoneOnBlockHitByProjectileProcedure;

public class CrackedNullstoneBlock extends Block {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public CrackedNullstoneBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.EMPTY).strength(1.8f, 6f).requiresCorrectToolForDrops());
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	@Override
	public void onProjectileHit(Level world, BlockState blockstate, BlockHitResult hit, Projectile entity) {
		NullstoneOnBlockHitByProjectileProcedure.execute(world, hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ());
	}
}