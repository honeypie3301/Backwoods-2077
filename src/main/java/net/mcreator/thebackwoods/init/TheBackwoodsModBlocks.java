/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.thebackwoods.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import net.mcreator.thebackwoods.block.*;
import net.mcreator.thebackwoods.TheBackwoodsMod;

public class TheBackwoodsModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(TheBackwoodsMod.MODID);
	public static final DeferredBlock<Block> BACKWOODS_PORTAL;
	public static final DeferredBlock<Block> ASH_ROSE;
	public static final DeferredBlock<Block> FADED_BLOCK;
	public static final DeferredBlock<Block> PLAQUE;
	public static final DeferredBlock<Block> ROTTEN_OAK_PLANKS;
	public static final DeferredBlock<Block> ROTTEN_OAK_WOOD;
	public static final DeferredBlock<Block> ROTTEN_OAK_LOG;
	public static final DeferredBlock<Block> GEODE_TELEPORTER;
	public static final DeferredBlock<Block> ROTTEN_OAK_STAIR;
	public static final DeferredBlock<Block> ROTTEN_OAK_SLAB;
	public static final DeferredBlock<Block> ROTTEN_OAK_FENCE;
	public static final DeferredBlock<Block> ROTTEN_OAK_TRAPDOOR;
	public static final DeferredBlock<Block> ROTTEN_OAK_BUTTON;
	public static final DeferredBlock<Block> ROTTEN_OAK_GATE;
	public static final DeferredBlock<Block> ROTTEN_OAK_PRESSURE_PLATE;
	public static final DeferredBlock<Block> PETRIFIED_ROTTEN_OAK_WOOD;
	public static final DeferredBlock<Block> BLINDSPOT_SPLINTER_SPAWNER;
	public static final DeferredBlock<Block> THE_PETRIFIED_WEALD_PORTAL;
	public static final DeferredBlock<Block> PETRIFIED_WEALD_FOLIAGE;
	public static final DeferredBlock<Block> PETRIFIED_ROTTEN_OAK_LOG;
	public static final DeferredBlock<Block> PETRIFIED_STRIPPED_ROTTEN_OAK_LOG;
	public static final DeferredBlock<Block> PETRIFIED_ROTTEN_OAK_PLANKS;
	public static final DeferredBlock<Block> PETRIFIED_ROTTEN_OAK_STAIRS;
	public static final DeferredBlock<Block> PETRIFIED_ROTTEN_OAK_SLAB;
	public static final DeferredBlock<Block> PETRIFIED_ROTTEN_OAK_FENCE;
	public static final DeferredBlock<Block> PETRIFIED_ROTTEN_OAK_FENCE_GATE;
	public static final DeferredBlock<Block> FOSSILIZED_SILT;
	public static final DeferredBlock<Block> POTTED_ASH_ROSE;
	public static final DeferredBlock<Block> SEDGEBRUSH;
	public static final DeferredBlock<Block> TALL_SEDGEBRUSH;
	public static final DeferredBlock<Block> AMBER_GRIT;
	public static final DeferredBlock<Block> COBBLED_AMBER_GRIT;
	public static final DeferredBlock<Block> PLAQUE_HEART;
	public static final DeferredBlock<Block> NULLSTONE;
	public static final DeferredBlock<Block> CRACKED_NULLSTONE;
	public static final DeferredBlock<Block> MEMORY_QUARTZ;
	public static final DeferredBlock<Block> SINKING_ASH;
	public static final DeferredBlock<Block> COBBLED_NULLSTONE;
	public static final DeferredBlock<Block> NULLSTONE_SLAB;
	public static final DeferredBlock<Block> NULLSTONE_STAIR;
	public static final DeferredBlock<Block> NULLSTONE_WALL;
	public static final DeferredBlock<Block> COBBLED_NULLSTONE_SLAB;
	public static final DeferredBlock<Block> COBBLED_NULLSTONE_STAIR;
	public static final DeferredBlock<Block> COBBLED_NULLSTONE_WALL;
	public static final DeferredBlock<Block> SCANDERE_LIGNUM;
	public static final DeferredBlock<Block> SCANDERE_LIGNUM_LOG;
	public static final DeferredBlock<Block> FALSE_OAK_PLANKS;
	public static final DeferredBlock<Block> FRACTUS_CORE;
	public static final DeferredBlock<Block> FRACTUS_PRIME_CORE;
	public static final DeferredBlock<Block> LIGNUM_CARO;
	public static final DeferredBlock<Block> DECAYING_LEAVES;
	static {
		BACKWOODS_PORTAL = REGISTRY.register("backwoods_portal", BackwoodsPortalBlock::new);
		ASH_ROSE = REGISTRY.register("ash_rose", AshRoseBlock::new);
		FADED_BLOCK = REGISTRY.register("faded_block", FadedBlockBlock::new);
		PLAQUE = REGISTRY.register("plaque", PlaqueBlock::new);
		ROTTEN_OAK_PLANKS = REGISTRY.register("rotten_oak_planks", RottenOakPlanksBlock::new);
		ROTTEN_OAK_WOOD = REGISTRY.register("rotten_oak_wood", RottenOakWoodBlock::new);
		ROTTEN_OAK_LOG = REGISTRY.register("rotten_oak_log", RottenOakLogBlock::new);
		GEODE_TELEPORTER = REGISTRY.register("geode_teleporter", GeodeTeleporterBlock::new);
		ROTTEN_OAK_STAIR = REGISTRY.register("rotten_oak_stair", RottenOakStairBlock::new);
		ROTTEN_OAK_SLAB = REGISTRY.register("rotten_oak_slab", RottenOakSlabBlock::new);
		ROTTEN_OAK_FENCE = REGISTRY.register("rotten_oak_fence", RottenOakFenceBlock::new);
		ROTTEN_OAK_TRAPDOOR = REGISTRY.register("rotten_oak_trapdoor", RottenOakTrapdoorBlock::new);
		ROTTEN_OAK_BUTTON = REGISTRY.register("rotten_oak_button", RottenOakButtonBlock::new);
		ROTTEN_OAK_GATE = REGISTRY.register("rotten_oak_gate", RottenOakGateBlock::new);
		ROTTEN_OAK_PRESSURE_PLATE = REGISTRY.register("rotten_oak_pressure_plate", RottenOakPressurePlateBlock::new);
		PETRIFIED_ROTTEN_OAK_WOOD = REGISTRY.register("petrified_rotten_oak_wood", PetrifiedRottenOakWoodBlock::new);
		BLINDSPOT_SPLINTER_SPAWNER = REGISTRY.register("blindspot_splinter_spawner", BlindspotSplinterSpawnerBlock::new);
		THE_PETRIFIED_WEALD_PORTAL = REGISTRY.register("the_petrified_weald_portal", ThePetrifiedWealdPortalBlock::new);
		PETRIFIED_WEALD_FOLIAGE = REGISTRY.register("petrified_weald_foliage", PetrifiedWealdFoliageBlock::new);
		PETRIFIED_ROTTEN_OAK_LOG = REGISTRY.register("petrified_rotten_oak_log", PetrifiedRottenOakLogBlock::new);
		PETRIFIED_STRIPPED_ROTTEN_OAK_LOG = REGISTRY.register("petrified_stripped_rotten_oak_log", PetrifiedStrippedRottenOakLogBlock::new);
		PETRIFIED_ROTTEN_OAK_PLANKS = REGISTRY.register("petrified_rotten_oak_planks", PetrifiedRottenOakPlanksBlock::new);
		PETRIFIED_ROTTEN_OAK_STAIRS = REGISTRY.register("petrified_rotten_oak_stairs", PetrifiedRottenOakStairsBlock::new);
		PETRIFIED_ROTTEN_OAK_SLAB = REGISTRY.register("petrified_rotten_oak_slab", PetrifiedRottenOakSlabBlock::new);
		PETRIFIED_ROTTEN_OAK_FENCE = REGISTRY.register("petrified_rotten_oak_fence", PetrifiedRottenOakFenceBlock::new);
		PETRIFIED_ROTTEN_OAK_FENCE_GATE = REGISTRY.register("petrified_rotten_oak_fence_gate", PetrifiedRottenOakFenceGateBlock::new);
		FOSSILIZED_SILT = REGISTRY.register("fossilized_silt", FossilizedSiltBlock::new);
		POTTED_ASH_ROSE = REGISTRY.register("potted_ash_rose", PottedAshRoseBlock::new);
		SEDGEBRUSH = REGISTRY.register("sedgebrush", SedgebrushBlock::new);
		TALL_SEDGEBRUSH = REGISTRY.register("tall_sedgebrush", TallSedgebrushBlock::new);
		AMBER_GRIT = REGISTRY.register("amber_grit", AmberGritBlock::new);
		COBBLED_AMBER_GRIT = REGISTRY.register("cobbled_amber_grit", CobbledAmberGritBlock::new);
		PLAQUE_HEART = REGISTRY.register("plaque_heart", PlaqueHeartBlock::new);
		NULLSTONE = REGISTRY.register("nullstone", NullstoneBlock::new);
		CRACKED_NULLSTONE = REGISTRY.register("cracked_nullstone", CrackedNullstoneBlock::new);
		MEMORY_QUARTZ = REGISTRY.register("memory_quartz", MemoryQuartzBlock::new);
		SINKING_ASH = REGISTRY.register("sinking_ash", SinkingAshBlock::new);
		COBBLED_NULLSTONE = REGISTRY.register("cobbled_nullstone", CobbledNullstoneBlock::new);
		NULLSTONE_SLAB = REGISTRY.register("nullstone_slab", NullstoneSlabBlock::new);
		NULLSTONE_STAIR = REGISTRY.register("nullstone_stair", NullstoneStairBlock::new);
		NULLSTONE_WALL = REGISTRY.register("nullstone_wall", NullstoneWallBlock::new);
		COBBLED_NULLSTONE_SLAB = REGISTRY.register("cobbled_nullstone_slab", CobbledNullstoneSlabBlock::new);
		COBBLED_NULLSTONE_STAIR = REGISTRY.register("cobbled_nullstone_stair", CobbledNullstoneStairBlock::new);
		COBBLED_NULLSTONE_WALL = REGISTRY.register("cobbled_nullstone_wall", CobbledNullstoneWallBlock::new);
		SCANDERE_LIGNUM = REGISTRY.register("scandere_lignum", ScandereLignumBlock::new);
		SCANDERE_LIGNUM_LOG = REGISTRY.register("scandere_lignum_log", ScandereLignumLogBlock::new);
		FALSE_OAK_PLANKS = REGISTRY.register("false_oak_planks", FalseOakPlanksBlock::new);
		FRACTUS_CORE = REGISTRY.register("fractus_core", FractusCoreBlock::new);
		FRACTUS_PRIME_CORE = REGISTRY.register("fractus_prime_core", FractusPrimeCoreBlock::new);
		LIGNUM_CARO = REGISTRY.register("lignum_caro", LignumCaroBlock::new);
		DECAYING_LEAVES = REGISTRY.register("decaying_leaves", DecayingLeavesBlock::new);
	}
	// Start of user code block custom blocks
	// End of user code block custom blocks
}