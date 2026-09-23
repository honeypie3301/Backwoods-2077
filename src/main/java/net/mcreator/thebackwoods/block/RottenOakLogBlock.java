package net.mcreator.thebackwoods.block;

import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.RottenOakSpreadOnTickUpdateProcedure;
import net.mcreator.thebackwoods.procedures.RottenOakLogBlockDestroyedByPlayerProcedure;

import java.util.List;

public class RottenOakLogBlock extends Block {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

	public RottenOakLogBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
				.sound(new DeferredSoundType(1.0f, 1.0f, () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.wood.break")), () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:rotten_planks_step")),
						() -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.place")), () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:rotten_planks_break")),
						() -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.place"))))
				.strength(3f, 15f).friction(0.8f).randomTicks().ignitedByLava().instrument(NoteBlockInstrument.DIDGERIDOO));
		this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("block.the_backwoods.rotten_oak_log.description_0"));
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
		return 5;
	}

	@Override
	public void randomTick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
		super.randomTick(blockstate, world, pos, random);
		RottenOakSpreadOnTickUpdateProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState blockstate, Level world, BlockPos pos, Player entity, boolean willHarvest, FluidState fluid) {
		boolean retval = super.onDestroyedByPlayer(blockstate, world, pos, entity, willHarvest, fluid);
		RottenOakLogBlockDestroyedByPlayerProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
		return retval;
	}
}