package net.mcreator.thebackwoods.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.thebackwoods.procedures.LightLungsOnEffectActiveTickProcedure;
import net.mcreator.thebackwoods.TheBackwoodsMod;

public class LightLungsMobEffect extends MobEffect {
	public LightLungsMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -6697729);
		this.addAttributeModifier(Attributes.MOVEMENT_EFFICIENCY, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.light_lungs_0"), 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		LightLungsOnEffectActiveTickProcedure.execute(entity);
		return super.applyEffectTick(entity, amplifier);
	}
}