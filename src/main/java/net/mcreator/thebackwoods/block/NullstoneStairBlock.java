package net.mcreator.thebackwoods.block;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.NullstonePlayerStartsToDestroyProcedure;
import net.mcreator.thebackwoods.procedures.NullstoneOnBlockHitByProjectileProcedure;

public class NullstoneStairBlock extends StairBlock {
	public NullstoneStairBlock() {
		super(Blocks.AIR.defaultBlockState(), BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).sound(SoundType.EMPTY).strength(2.2f, 7.5f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.HAT));
	}

	@Override
	public float getExplosionResistance() {
		return 7.5f;
	}

	@Override
	public void attack(BlockState blockstate, Level world, BlockPos pos, Player entity) {
		super.attack(blockstate, world, pos, entity);
		NullstonePlayerStartsToDestroyProcedure.execute(entity);
	}

	@Override
	public void onProjectileHit(Level world, BlockState blockstate, BlockHitResult hit, Projectile entity) {
		NullstoneOnBlockHitByProjectileProcedure.execute(world, hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ());
	}
}