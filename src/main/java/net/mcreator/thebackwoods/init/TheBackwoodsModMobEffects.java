/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.thebackwoods.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.Registries;

import net.mcreator.thebackwoods.potion.*;
import net.mcreator.thebackwoods.TheBackwoodsMod;

public class TheBackwoodsModMobEffects {
	public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(Registries.MOB_EFFECT, TheBackwoodsMod.MODID);
	public static final DeferredHolder<MobEffect, MobEffect> HEAVY_LUNGS_POTION = REGISTRY.register("heavy_lungs_potion", () -> new HeavyLungsPotionMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> INOCULATION_EFFECT = REGISTRY.register("inoculation_effect", () -> new InoculationEffectMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> SPLINTERED_EFFECT = REGISTRY.register("splintered_effect", () -> new SplinteredEffectMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> CELLULAR_COLLAPSE = REGISTRY.register("cellular_collapse", () -> new CellularCollapseMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> CELLULAR_COLLAPSE_2 = REGISTRY.register("cellular_collapse_2", () -> new CellularCollapse2MobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> ATROPHY_EFFECT = REGISTRY.register("atrophy_effect", () -> new AtrophyEffectMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> LIGHT_LUNGS = REGISTRY.register("light_lungs", () -> new LightLungsMobEffect());
}