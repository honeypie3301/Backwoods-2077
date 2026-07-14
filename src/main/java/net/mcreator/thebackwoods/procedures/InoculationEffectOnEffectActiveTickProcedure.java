package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.init.TheBackwoodsModMobEffects;

public class InoculationEffectOnEffectActiveTickProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(TheBackwoodsModMobEffects.INOCULATION_EFFECT) ? _livEnt.getEffect(TheBackwoodsModMobEffects.INOCULATION_EFFECT).getDuration() : 0) % 20 == 0 && entity instanceof LivingEntity) {
			if (entity instanceof net.minecraft.world.entity.LivingEntity _livEnt) {
				String[] effects = {"spore:biled", "spore:corrosion", "spore:frostbite", "spore:ignitable", "spore:madness", "spore:marker", "spore:mycelium_ef", "spore:starvation", "spore:symbiosis", "spore:uneasy", "arphex:spider_silk_touch",
						"arphex:splintered_sanity", "arphex:torment_spiral"};
				for (String id : effects) {
					String[] parts = id.split(":");
					net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(parts[0], parts[1])).ifPresent(holder -> {
						if (_livEnt.hasEffect(holder)) {
							net.minecraft.world.effect.MobEffectInstance inst = _livEnt.getEffect(holder);
							if (inst != null) {
								int currentTicks = inst.getDuration();
								int amp = inst.getAmplifier();
								// Subtracts 20 extra ticks. Combined with vanilla's natural decay,
								// the effect loses 40 ticks (2 seconds) total every 1.0 second.
								int newTicks = currentTicks - 20;
								_livEnt.removeEffect(holder);
								if (newTicks > 0) {
									_livEnt.addEffect(new net.minecraft.world.effect.MobEffectInstance(holder, newTicks, amp, inst.isAmbient(), inst.isVisible(), inst.showIcon()));
								}
							}
						}
					});
				}
			}
		}
	}
}