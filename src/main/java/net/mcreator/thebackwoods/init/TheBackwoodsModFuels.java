/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.thebackwoods.init;

import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;

@EventBusSubscriber
public class TheBackwoodsModFuels {
	@SubscribeEvent
	public static void furnaceFuelBurnTimeEvent(FurnaceFuelBurnTimeEvent event) {
		ItemStack itemstack = event.getItemStack();
		if (itemstack.getItem() == TheBackwoodsModBlocks.FRACTUS_CORE.get().asItem())
			event.setBurnTime(1000000);
		else if (itemstack.getItem() == TheBackwoodsModItems.ROTTEN_STICK.get())
			event.setBurnTime(75);
		else if (itemstack.getItem() == TheBackwoodsModItems.PETRIFIED_ROTTEN_STICK.get())
			event.setBurnTime(300);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.ROTTEN_OAK_PLANKS.get().asItem())
			event.setBurnTime(200);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.ROTTEN_OAK_SLAB.get().asItem())
			event.setBurnTime(200);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.ROTTEN_OAK_STAIR.get().asItem())
			event.setBurnTime(200);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.ROTTEN_OAK_FENCE.get().asItem())
			event.setBurnTime(200);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.ROTTEN_OAK_GATE.get().asItem())
			event.setBurnTime(200);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.ROTTEN_OAK_TRAPDOOR.get().asItem())
			event.setBurnTime(200);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.ROTTEN_OAK_PRESSURE_PLATE.get().asItem())
			event.setBurnTime(200);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.ROTTEN_OAK_BUTTON.get().asItem())
			event.setBurnTime(75);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.ROTTEN_OAK_LOG.get().asItem())
			event.setBurnTime(240);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.ROTTEN_OAK_WOOD.get().asItem())
			event.setBurnTime(240);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.SCANDERE_LIGNUM.get().asItem())
			event.setBurnTime(270);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.SCANDERE_LIGNUM_LOG.get().asItem())
			event.setBurnTime(270);
		else if (itemstack.getItem() == TheBackwoodsModItems.FRACTUS_CORE_NUGGET.get())
			event.setBurnTime(111111);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.FRACTUS_PRIME_CORE.get().asItem())
			event.setBurnTime(2000000);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.DECAYING_LEAVES.get().asItem())
			event.setBurnTime(1600);
		else if (itemstack.getItem() == TheBackwoodsModBlocks.LIGNUM_CARO.get().asItem())
			event.setBurnTime(700);
	}
}