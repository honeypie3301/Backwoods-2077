package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class RottenOakSpreadOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (Math.random() < 0.6) {
			if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == Blocks.OAK_PLANKS) {
				{
					BlockPos _bp = BlockPos.containing(x + 1, y, z);
					BlockState _bs = TheBackwoodsModBlocks.ROTTEN_OAK_PLANKS.get().defaultBlockState();
					BlockState _bso = world.getBlockState(_bp);
					for (Property<?> _propertyOld : _bso.getProperties()) {
						Property _propertyNew = _bs.getBlock().getStateDefinition().getProperty(_propertyOld.getName());
						if (_propertyNew != null && _bs.getValue(_propertyNew) != null)
							try {
								_bs = _bs.setValue(_propertyNew, _bso.getValue(_propertyOld));
							} catch (Exception e) {
							}
					}
					world.setBlock(_bp, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == Blocks.OAK_PLANKS) {
				{
					BlockPos _bp = BlockPos.containing(x - 1, y, z);
					BlockState _bs = TheBackwoodsModBlocks.ROTTEN_OAK_PLANKS.get().defaultBlockState();
					BlockState _bso = world.getBlockState(_bp);
					for (Property<?> _propertyOld : _bso.getProperties()) {
						Property _propertyNew = _bs.getBlock().getStateDefinition().getProperty(_propertyOld.getName());
						if (_propertyNew != null && _bs.getValue(_propertyNew) != null)
							try {
								_bs = _bs.setValue(_propertyNew, _bso.getValue(_propertyOld));
							} catch (Exception e) {
							}
					}
					world.setBlock(_bp, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x, y + 1, z))).getBlock() == Blocks.OAK_PLANKS && Math.random() < 0.125) {
				{
					BlockPos _bp = BlockPos.containing(x, y - 1, z);
					BlockState _bs = TheBackwoodsModBlocks.ROTTEN_OAK_PLANKS.get().defaultBlockState();
					BlockState _bso = world.getBlockState(_bp);
					for (Property<?> _propertyOld : _bso.getProperties()) {
						Property _propertyNew = _bs.getBlock().getStateDefinition().getProperty(_propertyOld.getName());
						if (_propertyNew != null && _bs.getValue(_propertyNew) != null)
							try {
								_bs = _bs.setValue(_propertyNew, _bso.getValue(_propertyOld));
							} catch (Exception e) {
							}
					}
					world.setBlock(_bp, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x, y - 1, z))).getBlock() == Blocks.OAK_PLANKS && Math.random() < 0.125) {
				{
					BlockPos _bp = BlockPos.containing(x, y - 1, z);
					BlockState _bs = TheBackwoodsModBlocks.ROTTEN_OAK_PLANKS.get().defaultBlockState();
					BlockState _bso = world.getBlockState(_bp);
					for (Property<?> _propertyOld : _bso.getProperties()) {
						Property _propertyNew = _bs.getBlock().getStateDefinition().getProperty(_propertyOld.getName());
						if (_propertyNew != null && _bs.getValue(_propertyNew) != null)
							try {
								_bs = _bs.setValue(_propertyNew, _bso.getValue(_propertyOld));
							} catch (Exception e) {
							}
					}
					world.setBlock(_bp, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == Blocks.OAK_PLANKS) {
				{
					BlockPos _bp = BlockPos.containing(x, y, z + 1);
					BlockState _bs = TheBackwoodsModBlocks.ROTTEN_OAK_PLANKS.get().defaultBlockState();
					BlockState _bso = world.getBlockState(_bp);
					for (Property<?> _propertyOld : _bso.getProperties()) {
						Property _propertyNew = _bs.getBlock().getStateDefinition().getProperty(_propertyOld.getName());
						if (_propertyNew != null && _bs.getValue(_propertyNew) != null)
							try {
								_bs = _bs.setValue(_propertyNew, _bso.getValue(_propertyOld));
							} catch (Exception e) {
							}
					}
					world.setBlock(_bp, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == Blocks.OAK_PLANKS) {
				{
					BlockPos _bp = BlockPos.containing(x, y, z - 1);
					BlockState _bs = TheBackwoodsModBlocks.ROTTEN_OAK_PLANKS.get().defaultBlockState();
					BlockState _bso = world.getBlockState(_bp);
					for (Property<?> _propertyOld : _bso.getProperties()) {
						Property _propertyNew = _bs.getBlock().getStateDefinition().getProperty(_propertyOld.getName());
						if (_propertyNew != null && _bs.getValue(_propertyNew) != null)
							try {
								_bs = _bs.setValue(_propertyNew, _bso.getValue(_propertyOld));
							} catch (Exception e) {
							}
					}
					world.setBlock(_bp, _bs, 3);
				}
			}
		}
	}
}