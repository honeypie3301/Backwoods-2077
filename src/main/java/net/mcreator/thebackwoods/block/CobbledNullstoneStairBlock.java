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

import net.mcreator.thebackwoods.procedures.NullstoneOnBlockHitByProjectileProcedure;

public class CobbledNullstoneStairBlock extends StairBlock {
	public CobbledNullstoneStairBlock() {
		super(Blocks.AIR.defaultBlockState(), BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).sound(SoundType.EMPTY).strength(2.2f, 7.5f).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.IRON_XYLOPHONE));
	}

	@Override
	public float getExplosionResistance() {
		return 7.5f;
	}

	@Override
	public void onProjectileHit(Level world, BlockState blockstate, BlockHitResult hit, Projectile entity) {
		NullstoneOnBlockHitByProjectileProcedure.execute(world, hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ());
	}
}