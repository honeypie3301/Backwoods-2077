package net.mcreator.thebackwoods.block;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;

public class CobbledAmberGritBlock extends Block {
	public CobbledAmberGritBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).sound(SoundType.TUFF).strength(3f, 9f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.GUITAR));
	}
}