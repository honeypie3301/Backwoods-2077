package net.mcreator.thebackwoods.init;

// 1.21.1 neo
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.core.registries.BuiltInRegistries;

// Features
import net.mcreator.thebackwoods.LabyrinthineGridsMaze;
import net.mcreator.thebackwoods.CalcifiedSpikeFeature;
import net.mcreator.thebackwoods.LargeCalcifiedSpike;
import net.mcreator.thebackwoods.SubGrainAtriaFeature;
import net.mcreator.thebackwoods.world.feature.CavernGridFeature;
import net.mcreator.thebackwoods.world.feature.SkyGridFeature;
import net.mcreator.thebackwoods.world.feature.VoidBasementFeature; 
import net.mcreator.thebackwoods.world.feature.UpsideDownCityFeature;
import net.mcreator.thebackwoods.world.feature.RightSideUpCityFeature;
import net.mcreator.thebackwoods.world.feature.CatwalkSubCavernFeature;
import net.mcreator.thebackwoods.world.feature.ScaffoldingTowerFeature; 
import net.mcreator.thebackwoods.world.feature.TheUndersideFeature;
import net.mcreator.thebackwoods.world.feature.VoidBedrockPlanksFeature;
import net.mcreator.thebackwoods.world.feature.MengerSpongeFeature; // Added import
import net.mcreator.thebackwoods.world.features.OakStalactiteFeature;
import net.mcreator.thebackwoods.world.feature.FarLandsFeature; // Added import for Far Lands
import net.mcreator.thebackwoods.world.feature.FamiliarFarLandsFeature; // Added import for Familiar Far Lands
import net.mcreator.thebackwoods.world.feature.LignumColonyFeature; // Added import for Lignum Colony


public class ModFeatures {
    public static final DeferredRegister<Feature<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.FEATURE, "the_backwoods");

    public static final DeferredHolder<Feature<?>, LabyrinthineGridsMaze> LABYRINTHINE_MAZE = REGISTRY.register("labyrinthine_grids_maze", LabyrinthineGridsMaze::new);
    public static final DeferredHolder<Feature<?>, CalcifiedSpikeFeature> CALCIFIED_SPIKE = REGISTRY.register("calcified_spike", CalcifiedSpikeFeature::new);
    public static final DeferredHolder<Feature<?>, LargeCalcifiedSpike> LARGE_CALCIFIED_SPIKE = REGISTRY.register("large_calcified_spike", LargeCalcifiedSpike::new);
    public static final DeferredHolder<Feature<?>, SubGrainAtriaFeature> SUB_GRAIN_ATRIA = REGISTRY.register("sub_grain_atria", SubGrainAtriaFeature::new);
    public static final DeferredHolder<Feature<?>, CavernGridFeature> CAVERN_GRID = REGISTRY.register("cavern_grid", () -> new CavernGridFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, SkyGridFeature> SKY_GRID = REGISTRY.register("sky_grid", () -> new SkyGridFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, VoidBasementFeature> VOID_BASEMENT = REGISTRY.register("void_basement", () -> new VoidBasementFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC)); 
    
    public static final DeferredHolder<Feature<?>, UpsideDownCityFeature> UPSIDE_DOWN_CITY = REGISTRY.register("upside_down_city", () -> new UpsideDownCityFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, RightSideUpCityFeature> RIGHT_SIDE_UP_CITY = REGISTRY.register("right_side_up_city", () -> new RightSideUpCityFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, CatwalkSubCavernFeature> CATWALK_SUB_CAVERN = REGISTRY.register("catwalk_sub_cavern", () -> new CatwalkSubCavernFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    
    public static final DeferredHolder<Feature<?>, ScaffoldingTowerFeature> SCAFFOLDING_TOWER = REGISTRY.register("scaffolding_tower", () -> new ScaffoldingTowerFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, TheUndersideFeature> THE_UNDERSIDE = REGISTRY.register("the_underside", () -> new TheUndersideFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, VoidBedrockPlanksFeature> VOID_BEDROCK_PLANKS = REGISTRY.register("void_bedrock_planks", () -> new VoidBedrockPlanksFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    // Registered Menger Sponge with 1.21.1 explicit .CODEC syntax
    public static final DeferredHolder<Feature<?>, MengerSpongeFeature> MENGER_SPONGE = REGISTRY.register("menger_sponge", () -> new MengerSpongeFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    // Registered Far Lands Feature with explicit .CODEC syntax
    public static final DeferredHolder<Feature<?>, FarLandsFeature> FAR_LANDS = REGISTRY.register("far_lands", () -> new FarLandsFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    // Registered Familiar Far Lands Feature with explicit .CODEC syntax
    public static final DeferredHolder<Feature<?>, FamiliarFarLandsFeature> FAMILIAR_FAR_LANDS = REGISTRY.register("familiar_far_lands", () -> new FamiliarFarLandsFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    // Decoration Features
    public static final DeferredHolder<Feature<?>, OakStalactiteFeature> OAK_STALACTITE = REGISTRY.register("oak_stalactite", OakStalactiteFeature::new);

    // Procedural Surface Colony Feature
    public static final DeferredHolder<Feature<?>, LignumColonyFeature> LIGNUM_COLONY = REGISTRY.register("lignum_colony", () -> new LignumColonyFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));




    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }
}