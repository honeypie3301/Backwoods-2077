package net.mcreator.thebackwoods.block;

import net.neoforged.neoforge.common.util.DeferredSoundType;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class PetrifiedStrippedRottenOakLogBlock extends Block {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

	public PetrifiedStrippedRottenOakLogBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.RAW_IRON)
				.sound(new DeferredSoundType(1.0f, 1.0f, () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.wood.break")), () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:rotten_planks_step")),
						() -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.place")), () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:rotten_planks_break")),
						() -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.place"))))
				.strength(17f, 22f).friction(0.7f).instrument(NoteBlockInstrument.DIDGERIDOO));
		this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(AXIS, context.getClickedFace().getAxis());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		return RotatedPillarBlock.rotatePillar(state, rot);
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 1;
	}
}