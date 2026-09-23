package net.mcreator.thebackwoods.block;

import net.neoforged.neoforge.common.util.DeferredSoundType;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

public class FractusPrimeCoreBlock extends Block {
	private static final VoxelShape SHAPE = box(3, 0, 3, 13, 10, 13);

	public FractusPrimeCoreBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
				.sound(new DeferredSoundType(1.0f, 1.0f, () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.deactivate")), () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("intentionally_empty")),
						() -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")), () -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("intentionally_empty")),
						() -> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("intentionally_empty"))))
				.strength(5f, 12f).lightLevel(blockstate -> 13).noOcclusion().hasPostProcess((bs, br, bp) -> true).emissiveRendering((bs, br, bp) -> true).isRedstoneConductor((bs, br, bp) -> false).instrument(NoteBlockInstrument.IRON_XYLOPHONE));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}
}