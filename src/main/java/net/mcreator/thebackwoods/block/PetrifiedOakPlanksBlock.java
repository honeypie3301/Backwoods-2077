package net.mcreator.thebackwoods.block;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;

public class PetrifiedOakPlanksBlock extends Block {
	public PetrifiedOakPlanksBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.2f, 3f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.COW_BELL));
	}
}