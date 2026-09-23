package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.thebackwoods.network.TheBackwoodsModVariables;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import javax.annotation.Nullable;

@EventBusSubscriber
public class FadingTimeProcedure {
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
		boolean fading_condition = false;
		double current_fade = 0;
		if (world.getLevelData().getGameTime() % 20 == 0) {
			fading_condition = (entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:loss")) || hasEntityInInventory(entity, new ItemStack(TheBackwoodsModBlocks.FADED_BLOCK.get()));
			current_fade = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).fade_level;
			if (fading_condition) {
				current_fade = Math.min(100.0, current_fade + 0.1);
			} else {
				current_fade = Math.max(0.0, current_fade - 0.2);
			}
			{
				TheBackwoodsModVariables.PlayerVariables _vars = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES);
				_vars.fade_level = current_fade;
				_vars.markSyncDirty();
			}
			if (entity instanceof LivingEntity _entity) {
				_entity.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(ResourceLocation.parse("the_backwoods:faded_attack"));
			}
			if (entity instanceof LivingEntity _entity) {
				_entity.getAttribute(Attributes.GRAVITY).removeModifier(ResourceLocation.parse("the_backwoods:faded_gravity"));
			}
			if (current_fade > 0) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.ASH, (entity.getX()), (entity.getY() + 1), (entity.getZ()), 1, 0.3, 0.5, 0.3, 0.001);
				if (entity instanceof LivingEntity _entity) {
					AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:faded_attack"), -((current_fade * 0.99999) / 100.0), AttributeModifier.Operation.ADD_VALUE);
					if (!_entity.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(modifier.id())) {
						_entity.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(modifier);
					}
				}
				if (entity instanceof LivingEntity _entity) {
					AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:faded_gravity"), -((current_fade * 0.05) / 100.0), AttributeModifier.Operation.ADD_VALUE);
					if (!_entity.getAttribute(Attributes.GRAVITY).hasModifier(modifier.id())) {
						_entity.getAttribute(Attributes.GRAVITY).addTransientModifier(modifier);
					}
				}
				if (entity instanceof LivingEntity _entity) {
					AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:faded_nametag_distance"), -((current_fade * 0.05) / 100.0), AttributeModifier.Operation.ADD_VALUE);
					if (!_entity.getAttribute(NeoForgeMod.NAMETAG_DISTANCE).hasModifier(modifier.id())) {
						_entity.getAttribute(NeoForgeMod.NAMETAG_DISTANCE).addTransientModifier(modifier);
					}
				}
				if (entity instanceof LivingEntity _entity) {
					AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:faded_burning_time"), -((current_fade * 0.05) / 100.0), AttributeModifier.Operation.ADD_VALUE);
					if (!_entity.getAttribute(Attributes.BURNING_TIME).hasModifier(modifier.id())) {
						_entity.getAttribute(Attributes.BURNING_TIME).addTransientModifier(modifier);
					}
				}
			}
		}
	}

	private static boolean hasEntityInInventory(Entity entity, ItemStack itemstack) {
		if (entity instanceof Player player)
			return player.getInventory().contains(stack -> !stack.isEmpty() && ItemStack.isSameItem(stack, itemstack));
		return false;
	}
}