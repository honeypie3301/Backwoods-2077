package net.mcreator.thebackwoods.block;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.SinkingAshOnBlockRightclickedProcedure;
import net.mcreator.thebackwoods.procedures.SinkingAshEntityCollidesInTheBlockProcedure;

import com.mojang.serialization.MapCodec;

public class SinkingAshBlock extends FallingBlock {
	public static final MapCodec<SinkingAshBlock> CODEC = simpleCodec(properties -> new SinkingAshBlock());

	public MapCodec<SinkingAshBlock> codec() {
		return CODEC;
	}

	public SinkingAshBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).sound(SoundType.EMPTY).strength(0.5f).noCollission().friction(0.67f).ignitedByLava());
	}

	@Override
	public PathType getBlockPathType(BlockState state, BlockGetter world, BlockPos pos, Mob entity) {
		return PathType.POWDER_SNOW;
	}

	@Override
	public void entityInside(BlockState blockstate, Level world, BlockPos pos, Entity entity) {
		super.entityInside(blockstate, world, pos, entity);
		SinkingAshEntityCollidesInTheBlockProcedure.execute(world, entity);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
		super.useWithoutItem(blockstate, world, pos, entity, hit);
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		double hitX = hit.getLocation().x;
		double hitY = hit.getLocation().y;
		double hitZ = hit.getLocation().z;
		Direction direction = hit.getDirection();
		SinkingAshOnBlockRightclickedProcedure.execute(world, x, y, z, entity);
		return InteractionResult.SUCCESS;
	}
}