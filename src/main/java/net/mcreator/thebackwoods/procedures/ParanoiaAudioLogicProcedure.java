package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

@EventBusSubscriber
public class ParanoiaAudioLogicProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double soundSelector = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) < 10 || (entity instanceof Player _plr ? _plr.getFoodData().getFoodLevel() : 0) < 10) {
			if (Math.random() < 0.005) {
				if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:backwoods"))) {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x + Math.random() * 10 - 5, y, z + Math.random() * 10 - 5), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_creak_paranoia")), SoundSource.AMBIENT,
									(float) 0.7, (float) (0.7 + Math.random() * 0.3));
						} else {
							_level.playLocalSound((x + Math.random() * 10 - 5), y, (z + Math.random() * 10 - 5), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:wood_creak_paranoia")), SoundSource.AMBIENT, (float) 0.7,
									(float) (0.7 + Math.random() * 0.3), false);
						}
					}
				}
			}
		}
	}
}