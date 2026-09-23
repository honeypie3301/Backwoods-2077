package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.thebackwoods.init.TheBackwoodsModMobEffects;

import javax.annotation.Nullable;

@EventBusSubscriber
public class RottenOakInventoryDamageProcedure {
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
		if (world.getLevelData().getGameTime() % 45 == 0) {
			if (!(entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(TheBackwoodsModMobEffects.INOCULATION_EFFECT)) && entity instanceof LivingEntity) {
				if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
					if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandlerIter) {
						for (int _idx = 0; _idx < _modHandlerIter.getSlots(); _idx++) {
							ItemStack itemstackiterator = _modHandlerIter.getStackInSlot(_idx).copy();
							if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("the_backwoods:rotting_blocks")))) {
								entity.hurt(new DamageSource(world.holderOrThrow(DamageTypes.THORNS)), (float) 0.05);
							}
						}
					}
				}
			}
		}
	}
}