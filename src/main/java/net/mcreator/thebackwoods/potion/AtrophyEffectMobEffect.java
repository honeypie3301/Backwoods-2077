package net.mcreator.thebackwoods.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.thebackwoods.TheBackwoodsMod;

public class AtrophyEffectMobEffect extends MobEffect {
	public AtrophyEffectMobEffect() {
		super(MobEffectCategory.HARMFUL, -10066330);
		this.addAttributeModifier(Attributes.MAX_HEALTH, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.atrophy_effect_0"), -4, AttributeModifier.Operation.ADD_VALUE);
		this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.atrophy_effect_1"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(Attributes.JUMP_STRENGTH, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.atrophy_effect_2"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
}