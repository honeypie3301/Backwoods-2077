/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.thebackwoods.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.registries.Registries;

import net.mcreator.thebackwoods.TheBackwoodsMod;

public class TheBackwoodsModPotions {
	public static final DeferredRegister<Potion> REGISTRY = DeferredRegister.create(Registries.POTION, TheBackwoodsMod.MODID);
	public static final DeferredHolder<Potion, Potion> INOCULATION = REGISTRY.register("inoculation", () -> new Potion(new MobEffectInstance(TheBackwoodsModMobEffects.INOCULATION_EFFECT, 2400, 0, false, true)));
}