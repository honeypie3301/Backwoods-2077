package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

@EventBusSubscriber
public class NullstoneHasteCDProcedure {
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
		if (entity.getPersistentData().getDouble("nullstone_haste_cd") > 0) {
			entity.getPersistentData().putDouble("nullstone_haste_cd", (entity.getPersistentData().getDouble("nullstone_haste_cd") - 1));
		} else {
			if (entity instanceof LivingEntity _entity) {
				_entity.getAttribute(Attributes.BLOCK_BREAK_SPEED).removeModifier(ResourceLocation.parse("the_backwoods:nullstonehaste"));
			}
		}
	}
}