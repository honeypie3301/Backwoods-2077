package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

@EventBusSubscriber
public class LossExhaustionProcedure {
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
		if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:loss"))) {
			if (entity.isSwimming() || entity.isSprinting() || entity instanceof LivingEntity _livEnt5 && _livEnt5.isFallFlying() || (entity.getDeltaMovement().x() > 0.01 || entity.getDeltaMovement().z() > 0.01) && entity.onGround()) {
				if (entity instanceof Player _player)
					_player.causeFoodExhaustion((float) 0.004);
			}
		}
	}
}