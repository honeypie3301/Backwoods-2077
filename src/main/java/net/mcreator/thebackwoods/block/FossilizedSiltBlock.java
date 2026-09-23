package net.mcreator.thebackwoods.block;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;

public class FossilizedSiltBlock extends Block {
	public FossilizedSiltBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).sound(SoundType.TUFF).strength(3.5f, 6f).instrument(NoteBlockInstrument.COW_BELL));
	}
}