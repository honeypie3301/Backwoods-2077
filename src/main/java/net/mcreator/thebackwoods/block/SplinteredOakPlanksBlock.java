package net.mcreator.thebackwoods.block;

import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.SplinteredOakPlanksOnBlockRightclickedProcedure;
import net.mcreator.thebackwoods.procedures.SplinteredOakPlanksEntityWalksOnTheBlockProcedure;
import net.mcreator.thebackwoods.procedures.SplinteredOakPlanksEntityFallsOnTheBlockProcedure;

import java.util.List;

public class SplinteredOakPlanksBlock extends Block {
	public SplinteredOakPlanksBlock() {
		super(BlockBehaviour.Properties.of()
				.sound(new DeferredSoundType(1.0f, 1.0f, () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.break")), () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bamboo.step")),
						() -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bamboo_wood.place")), () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.hit")),
						() -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.fall"))))
				.strength(2.2f, 3.5f).speedFactor(0.6f).ignitedByLava().instrument(NoteBlockInstrument.PLING));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("block.the_backwoods.splintered_oak_planks.description_0"));
		list.add(Component.translatable("block.the_backwoods.splintered_oak_planks.description_1"));
		list.add(Component.translatable("block.the_backwoods.splintered_oak_planks.description_2"));
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 5;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 5;
	}

	@Override
	public PathType getBlockPathType(BlockState state, BlockGetter world, BlockPos pos, Mob entity) {
		return PathType.DAMAGE_CAUTIOUS;
	}

	@Override
	public void stepOn(Level world, BlockPos pos, BlockState blockstate, Entity entity) {
		super.stepOn(world, pos, blockstate, entity);
		SplinteredOakPlanksEntityWalksOnTheBlockProcedure.execute(world, entity);
	}

	@Override
	public void fallOn(Level world, BlockState blockstate, BlockPos pos, Entity entity, float distance) {
		super.fallOn(world, blockstate, pos, entity, distance);
		SplinteredOakPlanksEntityFallsOnTheBlockProcedure.execute(world, entity);
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
		SplinteredOakPlanksOnBlockRightclickedProcedure.execute(world, x, y, z, entity);
		return InteractionResult.SUCCESS;
	}
}