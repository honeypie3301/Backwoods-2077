package net.mcreator.thebackwoods.procedures;

import net.mcreator.thebackwoods.TheBackwoodsMod;

import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import java.util.List;

public class RotEffigyRightclickedProcedure {
	// Scan settings
	private static final double SCAN_RADIUS = 40.0;
	private static final int HIGHLIGHT_TICKS = 140;
	private static final int SCAN_COOLDOWN_TICKS = 600;
	private static final int SCAN_FAIL_COOLDOWN_TICKS = 80;

	// Teleport settings
	private static final int TELEPORT_DELAY_TICKS = 60;
	private static final int TELEPORT_COOLDOWN_TICKS = 1200;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (entity == null || itemstack.isEmpty())
			return;
		if (!(world instanceof Level level))
			return;

		// Cooldown gate
		if (entity instanceof Player player) {
			if (player.getCooldowns().isOnCooldown(itemstack.getItem()))
				return;
		}

		// SHIFT + RIGHT CLICK = TELEPORT MODE
		if (entity.isShiftKeyDown()) {
			handleTeleport(world, x, y, z, entity, itemstack);
			return;
		}

		// NORMAL RIGHT CLICK = SCAN MODE
		handleScan(world, x, y, z, entity, itemstack);
	}

	private static void handleScan(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (!(world instanceof Level level) || level.isClientSide())
			return; // server-side only for authoritative glow/cooldown

		TagKey<EntityType<?>> woodboundTag = TagKey.create(
				Registries.ENTITY_TYPE,
				ResourceLocation.parse("the_backwoods:woodbound_entities")
		);

		List<Entity> targets = level.getEntitiesOfClass(
				Entity.class,
				new AABB(new Vec3(x, y, z), new Vec3(x, y, z)).inflate(SCAN_RADIUS),
				e -> e != null && e.isAlive() && e.getType().is(woodboundTag)
		);

		boolean foundAny = false;

		for (Entity target : targets) {
			target.setGlowingTag(true);
			foundAny = true;

			TheBackwoodsMod.queueServerWork(HIGHLIGHT_TICKS, () -> {
				if (target.isAlive()) {
					target.setGlowingTag(false);
				}
			});
		}

		if (entity instanceof Player player) {
			if (foundAny) {
				player.getCooldowns().addCooldown(itemstack.getItem(), SCAN_COOLDOWN_TICKS);
				player.swing(InteractionHand.MAIN_HAND, true);

				level.playSound(
						null,
						BlockPos.containing(x, y, z),
						BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.power_select")),
						SoundSource.PLAYERS,
						1.0f,
						0.8f
				);
			} else {
				player.getCooldowns().addCooldown(itemstack.getItem(), SCAN_FAIL_COOLDOWN_TICKS);
				player.displayClientMessage(Component.literal("No woodbound entities sensed."), true);
			}
		}
	}

	private static void handleTeleport(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:rotting"))) {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.crimson_forest.mood")), SoundSource.MASTER, 3, 1);
				} else {
					_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.crimson_forest.mood")), SoundSource.MASTER, 3, 1, false);
				}
			}

			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 5, false, false));

			if (entity instanceof Player _player)
				_player.getCooldowns().addCooldown(itemstack.getItem(), TELEPORT_COOLDOWN_TICKS);

			TheBackwoodsMod.queueServerWork(TELEPORT_DELAY_TICKS, () -> {
				if (entity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
					ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_still"));
					ServerLevel nextLevel = _player.server.getLevel(destinationType);

					if (nextLevel != null) {
						_player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
						_player.teleportTo(nextLevel, _player.getX(), 129, _player.getZ(), _player.getYRot(), _player.getXRot());
						_player.connection.send(new ClientboundPlayerAbilitiesPacket(_player.getAbilities()));
						for (MobEffectInstance _effectinstance : _player.getActiveEffects())
							_player.connection.send(new ClientboundUpdateMobEffectPacket(_player.getId(), _effectinstance, false));
						_player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
					}
				}

				if (entity instanceof Player _player) {
					ItemStack _setstack = new ItemStack(Items.GLASS_BOTTLE);
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
				}
			});

			itemstack.shrink(1);

		} else {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.basalt_deltas.mood")), SoundSource.MASTER, 2, 1);
				} else {
					_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.basalt_deltas.mood")), SoundSource.MASTER, 2, 1, false);
				}
			}

			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 2, false, false));

			TheBackwoodsMod.queueServerWork(18, () -> {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("It won't work."), true);
			});
		}
	}
}