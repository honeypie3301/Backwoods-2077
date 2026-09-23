package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import javax.annotation.Nullable;

@EventBusSubscriber
public class FamiliarItemCorruptionProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double slot = 0;
		double itemCount = 0;
		slot = 0;
		if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_familiar")) && hasEntityInInventory(entity, new ItemStack(Blocks.OBSIDIAN))) {
			if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandlerIter) {
				for (int _idx = 0; _idx < _modHandlerIter.getSlots(); _idx++) {
					ItemStack itemstackiterator = _modHandlerIter.getStackInSlot(_idx).copy();
					if (Mth.nextDouble(RandomSource.create(), 0, 1) < 0.01) {
						if (itemstackiterator.getItem() == Blocks.OBSIDIAN.asItem()) {
							itemCount = itemstackiterator.getCount();
							if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
								ItemStack _setstack = new ItemStack(TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_WOOD.get()).copy();
								_setstack.setCount(itemstackiterator.getCount());
								_modHandler.setStackInSlot((int) slot, _setstack);
							}
						}
					}
					slot = slot + 1;
				}
			}
		}
	}

	private static boolean hasEntityInInventory(Entity entity, ItemStack itemstack) {
		if (entity instanceof Player player)
			return player.getInventory().contains(stack -> !stack.isEmpty() && ItemStack.isSameItem(stack, itemstack));
		return false;
	}
}