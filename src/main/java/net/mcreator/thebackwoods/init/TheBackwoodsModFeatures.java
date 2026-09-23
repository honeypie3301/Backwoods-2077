/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.thebackwoods.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.core.registries.Registries;

import net.mcreator.thebackwoods.world.features.ScandereLignumGenerationFeature;
import net.mcreator.thebackwoods.TheBackwoodsMod;

public class TheBackwoodsModFeatures {
	public static final DeferredRegister<Feature<?>> REGISTRY = DeferredRegister.create(Registries.FEATURE, TheBackwoodsMod.MODID);
	public static final DeferredHolder<Feature<?>, Feature<?>> SCANDERE_LIGNUM_GENERATION = REGISTRY.register("scandere_lignum_generation", ScandereLignumGenerationFeature::new);
}