package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import javax.annotation.Nullable;

@EventBusSubscriber
public class FractusCoreWeightProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double coreCount = 0;
		double heavyCoreCount = 0;
		coreCount = 0;
		if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandlerIter) {
			for (int _idx = 0; _idx < _modHandlerIter.getSlots(); _idx++) {
				ItemStack itemstackiterator = _modHandlerIter.getStackInSlot(_idx).copy();
				if (itemstackiterator.getItem() == TheBackwoodsModBlocks.FRACTUS_CORE.get().asItem()) {
					coreCount = coreCount + itemstackiterator.getCount();
				} else if (itemstackiterator.getItem() == TheBackwoodsModBlocks.FRACTUS_PRIME_CORE.get().asItem()) {
					heavyCoreCount = heavyCoreCount + itemstackiterator.getCount();
				}
			}
		}
		if (entity instanceof LivingEntity _entity) {
			_entity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(ResourceLocation.parse("the_backwoods:weight"));
		}
		if (entity instanceof LivingEntity _entity) {
			_entity.getAttribute(Attributes.GRAVITY).removeModifier(ResourceLocation.parse("the_backwoods:weight2"));
		}
		if (coreCount > 0 || heavyCoreCount > 0) {
			if (entity instanceof LivingEntity _entity) {
				AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:weight"), (coreCount * (-0.04) + heavyCoreCount * (-0.12)), AttributeModifier.Operation.ADD_VALUE);
				if (!_entity.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(modifier.id())) {
					_entity.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(modifier);
				}
			}
			if (entity instanceof LivingEntity _entity) {
				AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:weight2"), (coreCount * 0.017 + heavyCoreCount * 0.051), AttributeModifier.Operation.ADD_VALUE);
				if (!_entity.getAttribute(Attributes.GRAVITY).hasModifier(modifier.id())) {
					_entity.getAttribute(Attributes.GRAVITY).addTransientModifier(modifier);
				}
			}
		}
	}
}