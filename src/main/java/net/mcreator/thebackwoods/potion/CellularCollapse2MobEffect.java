package net.mcreator.thebackwoods.potion;

import net.neoforged.neoforge.common.NeoForgeMod;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.thebackwoods.procedures.CellularCollapse2OnEffectActiveTickProcedure;
import net.mcreator.thebackwoods.TheBackwoodsMod;

public class CellularCollapse2MobEffect extends MobEffect {
	public CellularCollapse2MobEffect() {
		super(MobEffectCategory.HARMFUL, -15658735);
		this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.cellular_collapse_2_0"), -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(Attributes.ATTACK_KNOCKBACK, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.cellular_collapse_2_1"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.cellular_collapse_2_2"), -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.cellular_collapse_2_3"), -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(Attributes.SUBMERGED_MINING_SPEED, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.cellular_collapse_2_4"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(NeoForgeMod.SWIM_SPEED, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.cellular_collapse_2_5"), -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(Attributes.WATER_MOVEMENT_EFFICIENCY, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.cellular_collapse_2_6"), -0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(Attributes.SWEEPING_DAMAGE_RATIO, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.cellular_collapse_2_7"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(Attributes.JUMP_STRENGTH, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.cellular_collapse_2_8"), -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(Attributes.MAX_HEALTH, ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "effect.cellular_collapse_2_9"), -6, AttributeModifier.Operation.ADD_VALUE);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		CellularCollapse2OnEffectActiveTickProcedure.execute(entity.level(), entity);
		return super.applyEffectTick(entity, amplifier);
	}
}