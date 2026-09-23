package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.Entity;

import net.mcreator.thebackwoods.entity.SplinterEntity;
import net.mcreator.thebackwoods.entity.LogSplinterEntity;
import net.mcreator.thebackwoods.entity.BlindspotSplinterEntity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class SplinterAgeProcedure {
	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (entity != null) {
			if (entity instanceof SplinterEntity || entity instanceof LogSplinterEntity || entity instanceof BlindspotSplinterEntity) {
				if (entity instanceof SplinterEntity _datEntSetI)
					_datEntSetI.getEntityData().set(SplinterEntity.DATA_Age, (int) ((entity instanceof SplinterEntity _datEntI ? _datEntI.getEntityData().get(SplinterEntity.DATA_Age) : 0) + 1));
				if (entity instanceof BlindspotSplinterEntity _datEntSetI)
					_datEntSetI.getEntityData().set(BlindspotSplinterEntity.DATA_Age, (int) ((entity instanceof BlindspotSplinterEntity _datEntI ? _datEntI.getEntityData().get(BlindspotSplinterEntity.DATA_Age) : 0) + 1));
				if (entity instanceof LogSplinterEntity _datEntSetI)
					_datEntSetI.getEntityData().set(LogSplinterEntity.DATA_Age, (int) ((entity instanceof LogSplinterEntity _datEntI ? _datEntI.getEntityData().get(LogSplinterEntity.DATA_Age) : 0) + 1));
				if (entity.isInWaterRainOrBubble()) {
					if (entity instanceof SplinterEntity _datEntSetI)
						_datEntSetI.getEntityData().set(SplinterEntity.DATA_Age, (int) ((entity instanceof SplinterEntity _datEntI ? _datEntI.getEntityData().get(SplinterEntity.DATA_Age) : 0) + 9));
					if (entity instanceof BlindspotSplinterEntity _datEntSetI)
						_datEntSetI.getEntityData().set(BlindspotSplinterEntity.DATA_Age, (int) ((entity instanceof BlindspotSplinterEntity _datEntI ? _datEntI.getEntityData().get(BlindspotSplinterEntity.DATA_Age) : 0) + 9));
					if (entity instanceof LogSplinterEntity _datEntSetI)
						_datEntSetI.getEntityData().set(LogSplinterEntity.DATA_Age, (int) ((entity instanceof LogSplinterEntity _datEntI ? _datEntI.getEntityData().get(LogSplinterEntity.DATA_Age) : 0) + 12));
				} else if (entity.isOnFire()) {
					if (entity instanceof SplinterEntity _datEntSetI)
						_datEntSetI.getEntityData().set(SplinterEntity.DATA_Age, (int) ((entity instanceof SplinterEntity _datEntI ? _datEntI.getEntityData().get(SplinterEntity.DATA_Age) : 0) + 14));
					if (entity instanceof BlindspotSplinterEntity _datEntSetI)
						_datEntSetI.getEntityData().set(BlindspotSplinterEntity.DATA_Age, (int) ((entity instanceof BlindspotSplinterEntity _datEntI ? _datEntI.getEntityData().get(BlindspotSplinterEntity.DATA_Age) : 0) + 14));
					if (entity instanceof LogSplinterEntity _datEntSetI)
						_datEntSetI.getEntityData().set(LogSplinterEntity.DATA_Age, (int) ((entity instanceof LogSplinterEntity _datEntI ? _datEntI.getEntityData().get(LogSplinterEntity.DATA_Age) : 0) + 16));
				}
			}
		}
	}
}