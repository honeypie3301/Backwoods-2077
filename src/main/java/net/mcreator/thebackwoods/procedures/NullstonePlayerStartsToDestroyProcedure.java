package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

public class NullstonePlayerStartsToDestroyProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof Player) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheBackwoodsModItems.RECOVERED_FADED_PICKAXE.get()) {
				if (!(entity instanceof LivingEntity _livingEntity3 && _livingEntity3.getAttribute(Attributes.BLOCK_BREAK_SPEED).hasModifier(ResourceLocation.parse("the_backwoods:nullstonehaste")))) {
					if (entity instanceof LivingEntity _entity) {
						AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:nullstonehaste"), 2.5, AttributeModifier.Operation.ADD_VALUE);
						if (!_entity.getAttribute(Attributes.BLOCK_BREAK_SPEED).hasModifier(modifier.id())) {
							_entity.getAttribute(Attributes.BLOCK_BREAK_SPEED).addTransientModifier(modifier);
						}
					}
				}
				entity.getPersistentData().putDouble("nullstone_haste_cd", 10);
			}
		}
	}
}