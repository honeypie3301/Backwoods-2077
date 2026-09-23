package net.mcreator.thebackwoods.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.thebackwoods.TheBackwoodsMod;

public class HeavyLungsPotionMobEffect extends MobEffect {
	public HeavyLungsPotionMobEffect() {
		super(MobEffectCategory.HARMFUL, -11908534);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.heavy_lungs_potion_0"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		this.addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.heavy_lungs_potion_1"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		this.addAttributeModifier(Attributes.MINING_EFFICIENCY, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.heavy_lungs_potion_2"), -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		this.addAttributeModifier(Attributes.OXYGEN_BONUS, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.heavy_lungs_potion_3"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}
}