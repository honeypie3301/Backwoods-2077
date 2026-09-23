package net.mcreator.thebackwoods.block;

import org.checkerframework.checker.units.qual.s;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.AshRosePlantRightclickedProcedure;

public class AshRoseBlock extends FlowerBlock {
	public AshRoseBlock() {
		super(MobEffects.HEAL, 100, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).randomTicks().sound(SoundType.GRASS).instabreak().lightLevel(s -> 2).noCollission().ignitedByLava().offsetType(BlockBehaviour.OffsetType.XZ)
				.pushReaction(PushReaction.DESTROY));
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 100;
	}

	@Override
	public PathType getBlockPathType(BlockState state, BlockGetter world, BlockPos pos, Mob entity) {
		return PathType.LAVA;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 60;
	}

	@Override
	public boolean mayPlaceOn(BlockState groundState, BlockGetter worldIn, BlockPos pos) {
		return groundState.is(Blocks.GRASS_BLOCK) || groundState.is(BlockTags.create(ResourceLocation.parse("minecraft:dirt"))) || groundState.is(BlockTags.create(ResourceLocation.parse("minecraft:logs")))
				|| groundState.is(BlockTags.create(ResourceLocation.parse("minecraft:planks"))) || groundState.is(Blocks.COBBLED_DEEPSLATE) || groundState.is(Blocks.DEEPSLATE);
	}

	@Override
	public boolean canSurvive(BlockState blockstate, LevelReader worldIn, BlockPos pos) {
		BlockPos blockpos = pos.below();
		BlockState groundState = worldIn.getBlockState(blockpos);
		return this.mayPlaceOn(groundState, worldIn, blockpos);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
		super.useWithoutItem(blockstate, world, pos, entity, hit);
		AshRosePlantRightclickedProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ(), entity);
		return InteractionResult.SUCCESS;
	}
}