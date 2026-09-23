/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.thebackwoods.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;

import net.mcreator.thebackwoods.TheBackwoodsMod;

@EventBusSubscriber
public class TheBackwoodsModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheBackwoodsMod.MODID);

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			tabData.accept(TheBackwoodsModItems.BACKWOODS.get());
			tabData.accept(TheBackwoodsModItems.LOSS.get());
			tabData.accept(TheBackwoodsModItems.ROTTEN_STICK.get());
			tabData.accept(TheBackwoodsModItems.ROTTEN_AXE.get());
			tabData.accept(TheBackwoodsModItems.ROTTEN_PICKAXE.get());
			tabData.accept(TheBackwoodsModItems.ROTTEN_SHOVEL.get());
			tabData.accept(TheBackwoodsModItems.ROTTEN_HOE.get());
			tabData.accept(TheBackwoodsModItems.ROT_EFFIGY.get());
			tabData.accept(TheBackwoodsModItems.FADED_AXE.get());
			tabData.accept(TheBackwoodsModItems.FADED_PICKAXE.get());
			tabData.accept(TheBackwoodsModItems.FADED_HOE.get());
			tabData.accept(TheBackwoodsModItems.FADED_SHOVEL.get());
			tabData.accept(TheBackwoodsModItems.THE_PETRIFIED_WEALD.get());
			tabData.accept(TheBackwoodsModItems.RECOVERED_FADED_AXE.get());
			tabData.accept(TheBackwoodsModItems.RECOVERED_FADED_PICKAXE.get());
			tabData.accept(TheBackwoodsModItems.RECOVERED_FADED_HOE.get());
			tabData.accept(TheBackwoodsModItems.RECOVERED_FADED_SHOVEL.get());
			tabData.accept(TheBackwoodsModItems.RESONANT_ROT_EFFIGY.get());
			tabData.accept(TheBackwoodsModItems.NULL_POINTERAXE.get());
			tabData.accept(TheBackwoodsModItems.LIGNUM_COMPOST.get());
			tabData.accept(TheBackwoodsModItems.CRYSTALLIZED_SCANDERE_RESIN.get());
			tabData.accept(TheBackwoodsModItems.LIGNUM_CARO_SWORD.get());
			tabData.accept(TheBackwoodsModItems.TETHERLESS_PEARL.get());
			tabData.accept(TheBackwoodsModItems.RESONANT_TETHERLESS_PEARL.get());
			tabData.accept(TheBackwoodsModItems.SPLINTER_NEEDLE.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			tabData.accept(TheBackwoodsModItems.SPLINTER_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.HOLLOW_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.LOG_SPLINTER_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.ASH_WEAVER_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.ROT_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.BLINDSPOT_SPLINTER_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_LOG_SPLINTER_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.STILT_WALKER_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.LIGNUM_GIGAS_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.LIGNUM_VERMIS_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.FRACTUS_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.FRACTUS_PRIME_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.LIGNUM_PALUS_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.LIGNUM_SPINA_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.LIGNUM_TRILOBITA_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.LIGNUM_ECHINUS_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_LIGNUM_SPINA_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_LIGNUM_TRILOBITA_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_LIGNUM_ECHINUS_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.DORCELESS_SPLINTER_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.KYNE_SPLINTER_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.WOODWEAVER_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.HEWN_SPLINTER_SPAWN_EGG.get());
			tabData.accept(TheBackwoodsModItems.VERDANT_ENGINE_SPAWN_EGG.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
			tabData.accept(TheBackwoodsModBlocks.ASH_ROSE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.FADED_BLOCK.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_WOOD.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_WEALD_FOLIAGE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_LOG.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_STRIPPED_ROTTEN_OAK_LOG.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.FOSSILIZED_SILT.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.SEDGEBRUSH.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.TALL_SEDGEBRUSH.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.AMBER_GRIT.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.COBBLED_AMBER_GRIT.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PLAQUE_HEART.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.NULLSTONE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.CRACKED_NULLSTONE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.MEMORY_QUARTZ.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.SINKING_ASH.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.COBBLED_NULLSTONE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.NULLSTONE_SLAB.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.NULLSTONE_STAIR.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.NULLSTONE_WALL.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.COBBLED_NULLSTONE_STAIR.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.COBBLED_NULLSTONE_WALL.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.SCANDERE_LIGNUM.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.SCANDERE_LIGNUM_LOG.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.FALSE_OAK_PLANKS.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.LIGNUM_CARO.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.DECAYING_LEAVES.get().asItem());
			tabData.accept(TheBackwoodsModItems.WINDCAP_MUSHROOM.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
			tabData.accept(TheBackwoodsModBlocks.FADED_BLOCK.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PLAQUE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_PLANKS.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_WOOD.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_LOG.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_STAIR.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_SLAB.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_FENCE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_TRAPDOOR.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_BUTTON.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_GATE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_PRESSURE_PLATE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_PLANKS.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_STAIRS.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_SLAB.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_FENCE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_FENCE_GATE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.AMBER_GRIT.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.COBBLED_AMBER_GRIT.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.COBBLED_NULLSTONE.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.COBBLED_NULLSTONE_SLAB.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.COBBLED_NULLSTONE_STAIR.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.COBBLED_NULLSTONE_WALL.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.LIGNUM_CARO.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.SPLINTERED_OAK_PLANKS.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.LIGNUM_CARO_TRAPDOOR.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.SPLINTERED_OAK_WOOD.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_SPLINTERED_OAK_WOOD.get().asItem());
			tabData.accept(TheBackwoodsModBlocks.PETRIFIED_OAK_PLANKS.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.INGREDIENTS) {
			tabData.accept(TheBackwoodsModItems.SEEP.get());
			tabData.accept(TheBackwoodsModItems.ROTTEN_STICK.get());
			tabData.accept(TheBackwoodsModItems.SPLINTER_SHARD.get());
			tabData.accept(TheBackwoodsModItems.HEARTWOOD_SHARD.get());
			tabData.accept(TheBackwoodsModItems.FADED_IRON_INGOT.get());
			tabData.accept(TheBackwoodsModItems.FADED_GOLD_INGOT.get());
			tabData.accept(TheBackwoodsModItems.FADED_COPPER_INGOT.get());
			tabData.accept(TheBackwoodsModItems.FADED_NETHERITE_INGOT.get());
			tabData.accept(TheBackwoodsModItems.FADED_GOLD_NUGGET.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_BARK.get());
			tabData.accept(TheBackwoodsModItems.SHARPENED_SPLINTER_SHARD.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_ROTTEN_STICK.get());
			tabData.accept(TheBackwoodsModItems.MEMORY_FRAGMENT.get());
			tabData.accept(TheBackwoodsModItems.DISTORTED_MEMORY_FRAGMENT.get());
			tabData.accept(TheBackwoodsModItems.RECOVERED_MEMORY_FRAGMENT.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_RESIN.get());
			tabData.accept(TheBackwoodsModItems.COMPACT_PETRIFIED_RESIN.get());
			tabData.accept(TheBackwoodsModItems.MEMORY_SHARD.get());
			tabData.accept(TheBackwoodsModBlocks.FRACTUS_CORE.get().asItem());
			tabData.accept(TheBackwoodsModItems.FRACTUS_CORE_NUGGET.get());
			tabData.accept(TheBackwoodsModBlocks.FRACTUS_PRIME_CORE.get().asItem());
			tabData.accept(TheBackwoodsModItems.CRYSTALLIZED_SCANDERE_RESIN.get());
			tabData.accept(TheBackwoodsModItems.LIGNUM_CARO_ASH.get());
			tabData.accept(TheBackwoodsModItems.SPLINTER_NEEDLE.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_OAK_GALL.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
			tabData.accept(TheBackwoodsModItems.PALE_REMEDY.get());
			tabData.accept(TheBackwoodsModItems.PALE_DRAUGHT_BOTTLE.get());
			tabData.accept(TheBackwoodsModItems.FADED_BREAD.get());
			tabData.accept(TheBackwoodsModItems.FADED_APPLE.get());
			tabData.accept(TheBackwoodsModItems.FADED_GOLDEN_APPLE.get());
			tabData.accept(TheBackwoodsModItems.FADED_CARROT.get());
			tabData.accept(TheBackwoodsModItems.FADED_GOLDEN_CARROT.get());
			tabData.accept(TheBackwoodsModItems.FADED_COOKED_BEEF.get());
			tabData.accept(TheBackwoodsModItems.FADED_WHEAT.get());
			tabData.accept(TheBackwoodsModItems.FADED_COOKED_CHICKEN.get());
			tabData.accept(TheBackwoodsModItems.FADED_COOKIE.get());
			tabData.accept(TheBackwoodsModItems.FADED_POTATO.get());
			tabData.accept(TheBackwoodsModItems.FADED_BAKED_POTATO.get());
			tabData.accept(TheBackwoodsModItems.FADED_MELON_SLICE.get());
			tabData.accept(TheBackwoodsModItems.FADED_PUMPKIN_PIE.get());
			tabData.accept(TheBackwoodsModItems.FADED_MUTTON.get());
			tabData.accept(TheBackwoodsModItems.FADED_COOKED_MUTTON.get());
			tabData.accept(TheBackwoodsModItems.FADED_COOKED_PORKCHOP.get());
			tabData.accept(TheBackwoodsModItems.FADED_PORKCHOP.get());
			tabData.accept(TheBackwoodsModItems.FADED_RABBIT.get());
			tabData.accept(TheBackwoodsModItems.FADED_COOKED_RABBIT.get());
			tabData.accept(TheBackwoodsModItems.FADED_COD.get());
			tabData.accept(TheBackwoodsModItems.FADED_COOKED_COD.get());
			tabData.accept(TheBackwoodsModItems.STABILIZED_PALE_REMEDY.get());
			tabData.accept(TheBackwoodsModItems.FADED_DRIED_KELP.get());
			tabData.accept(TheBackwoodsModItems.FADED_BEETROOT.get());
			tabData.accept(TheBackwoodsModItems.FADED_ENCHANTED_GOLDEN_APPLE.get());
			tabData.accept(TheBackwoodsModItems.ASH_BERRIES.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(TheBackwoodsModItems.ROTTEN_SWORD.get());
			tabData.accept(TheBackwoodsModItems.HEARTWOOD_ROTTEN_SWORD.get());
			tabData.accept(TheBackwoodsModItems.ROTTEN_AXE.get());
			tabData.accept(TheBackwoodsModItems.FADED_SWORD.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_ROTTEN_SWORD.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_ROTTEN_AXE.get());
			tabData.accept(TheBackwoodsModItems.RECOVERED_FADED_SWORD.get());
			tabData.accept(TheBackwoodsModItems.PETRIFIED_ROTTEN_DAGGER.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
			tabData.accept(TheBackwoodsModBlocks.ROTTEN_OAK_PRESSURE_PLATE.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
			tabData.accept(TheBackwoodsModBlocks.LIGNUM_CARO_TRAPDOOR.get().asItem());
		}
	}
}