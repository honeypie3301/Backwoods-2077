package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import javax.annotation.Nullable;

@EventBusSubscriber
public class PlaqueSicknessProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (hasEntityInInventory(entity, new ItemStack(TheBackwoodsModBlocks.PLAQUE.get()))) {
			entity.getPersistentData().putDouble("PlaqueSeverity", (entity.getPersistentData().getDouble("PlaqueSeverity") + 2));
		} else if (entity.getPersistentData().getDouble("PlaqueSeverity") > 0) {
			entity.getPersistentData().putDouble("PlaqueSeverity", (entity.getPersistentData().getDouble("PlaqueSeverity") - 4));
		}
		if (entity.getPersistentData().getDouble("PlaqueSeverity") > 0) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (entity.getPersistentData().getDouble("PlaqueSeverity") / 10 + 40), 0, false, false));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, (int) (entity.getPersistentData().getDouble("PlaqueSeverity") / 10 + 40), 0, false, false));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, (int) (entity.getPersistentData().getDouble("PlaqueSeverity") / 10 + 40), 0, false, false));
		}
	}

	private static boolean hasEntityInInventory(Entity entity, ItemStack itemstack) {
		if (entity instanceof Player player)
			return player.getInventory().contains(stack -> !stack.isEmpty() && ItemStack.isSameItem(stack, itemstack));
		return false;
	}
}