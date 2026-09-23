/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.thebackwoods.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;

import net.mcreator.thebackwoods.TheBackwoodsMod;

public class TheBackwoodsModParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, TheBackwoodsMod.MODID);
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FRACTUS_LASER_PARTICLE_WHITE = REGISTRY.register("fractus_laser_particle_white", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FRACTUS_LASER_PARTICLE_ORANGE = REGISTRY.register("fractus_laser_particle_orange", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FRACTUS_LASER_PARTICLE_RED = REGISTRY.register("fractus_laser_particle_red", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FRACTUS_LASER_PARTICLE_BURST = REGISTRY.register("fractus_laser_particle_burst", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FRACTUS_LASER_PARTICLE_SUMMON = REGISTRY.register("fractus_laser_particle_summon", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIG_SMOKE = REGISTRY.register("big_smoke", () -> new SimpleParticleType(true));
}