package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import javax.annotation.Nullable;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;

@EventBusSubscriber
public class ScandereLignumLogOnBlockRightclickedProcedure {
	
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		execute(event, event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity());
	}

	public static void execute() {
		// Dummy execute method to satisfy MCreator's code generator if dependencies aren't refreshed
	}

	public static void execute(LevelAccessor world, double x, double y, double z) {
		execute(null, world, x, y, z, null);
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
		BlockState blockstate = world.getBlockState(pos);

		// Check if the block has the oozing property
		Property<?> rawOozingProp = blockstate.getBlock().getStateDefinition().getProperty("oozing");
		if (rawOozingProp instanceof BooleanProperty oozingProperty) {
			
			// If it's currently oozing
			if (blockstate.getValue(oozingProperty)) {
				
				if (entity instanceof Player player) {
					ItemStack mainHandItem = player.getMainHandItem();
					
					// Check if the player is holding an Axe
					if (mainHandItem.getItem() instanceof AxeItem) {
						
						// 1. Set oozing to false
						world.setBlock(pos, blockstate.setValue(oozingProperty, false), 3);
						
						// 2. Play a scraping/squishing sound
						if (world instanceof Level level && !level.isClientSide()) {
							level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 1.2F);
							level.playSound(null, pos, SoundEvents.SLIME_BLOCK_STEP, SoundSource.BLOCKS, 1.0F, 0.8F);
							
							// 3. Drop the raw resin item
							ItemStack dropItem = new ItemStack(net.mcreator.thebackwoods.init.TheBackwoodsModItems.SCANDERE_RESIN.get());
							ItemEntity entityToSpawn = new ItemEntity(level, x + 0.5, y + 0.5, z + 0.5, dropItem);
							entityToSpawn.setPickUpDelay(10);
							level.addFreshEntity(entityToSpawn);
						}
						
						// 4. Swing the player's arm
						player.swing(InteractionHand.MAIN_HAND, true);
					}
				}
			}
		}
	}
} // 1.21.1