package net.mcreator.thebackwoods.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.GeodeTeleporterEntityCollidesInTheBlockProcedure;

public class GeodeTeleporterBlock extends Block {
	private static final VoxelShape SHAPE = Shapes.empty();

	public GeodeTeleporterBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).sound(SoundType.EMPTY).instabreak().noCollission().isRedstoneConductor((bs, br, bp) -> false).replaceable().instrument(NoteBlockInstrument.HAT));
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
	public void entityInside(BlockState blockstate, Level world, BlockPos pos, Entity entity) {
		super.entityInside(blockstate, world, pos, entity);
		GeodeTeleporterEntityCollidesInTheBlockProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ(), entity);
	}
}