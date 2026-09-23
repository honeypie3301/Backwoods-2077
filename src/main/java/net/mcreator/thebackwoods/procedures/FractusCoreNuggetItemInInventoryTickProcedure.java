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

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

import javax.annotation.Nullable;

@EventBusSubscriber
public class FractusCoreNuggetItemInInventoryTickProcedure {
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
		coreCount = 0;
		if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandlerIter) {
			for (int _idx = 0; _idx < _modHandlerIter.getSlots(); _idx++) {
				ItemStack itemstackiterator = _modHandlerIter.getStackInSlot(_idx).copy();
				if (itemstackiterator.getItem() == TheBackwoodsModItems.FRACTUS_CORE_NUGGET.get()) {
					coreCount = coreCount + itemstackiterator.getCount();
				}
			}
		}
		if (entity instanceof LivingEntity _entity) {
			_entity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(ResourceLocation.parse("the_backwoods:coreweight"));
		}
		if (entity instanceof LivingEntity _entity) {
			_entity.getAttribute(Attributes.GRAVITY).removeModifier(ResourceLocation.parse("the_backwoods:coreweight2"));
		}
		if (coreCount > 0) {
			if (entity instanceof LivingEntity _entity) {
				AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:coreweight"), (coreCount * (-0.004)), AttributeModifier.Operation.ADD_VALUE);
				if (!_entity.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(modifier.id())) {
					_entity.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(modifier);
				}
			}
			if (entity instanceof LivingEntity _entity) {
				AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:coreweight2"), (coreCount * 0.0018), AttributeModifier.Operation.ADD_VALUE);
				if (!_entity.getAttribute(Attributes.GRAVITY).hasModifier(modifier.id())) {
					_entity.getAttribute(Attributes.GRAVITY).addTransientModifier(modifier);
				}
			}
		}
	}
}