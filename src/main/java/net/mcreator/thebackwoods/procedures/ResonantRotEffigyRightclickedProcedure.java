package net.mcreator.thebackwoods.procedures;

import net.mcreator.thebackwoods.TheBackwoodsMod;
import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

@EventBusSubscriber
public class ResonantRotEffigyRightclickedProcedure {
	// Stronger scan settings
	private static final double SCAN_RADIUS = 64.0;
	private static final int HIGHLIGHT_TICKS = 280;      // 14s
	private static final int SCAN_COOLDOWN_TICKS = 360;  // 18s
	private static final int SCAN_FAIL_COOLDOWN_TICKS = 60;

	// Teleport settings
	private static final int TELEPORT_DELAY_TICKS = 60;
	private static final int TELEPORT_COOLDOWN_TICKS = 900; // 45s

	// Durability costs
	private static final int SCAN_DURABILITY_COST = 1;
	private static final int TELEPORT_DURABILITY_COST = 3;

	// Mode shuffle settings
	// 0 = ATTUNED (normal teleport: Rotting -> Still)
	// 1 = FRACTURED (Overworld teleport in any non-overworld dimension)
	private static final String NBT_MODE = "res_effigy_mode";
	private static final String NBT_SHUFFLE_T = "res_effigy_shuffle_t";
	private static final int SHUFFLE_INTERVAL_TICKS = 20; // slower shuffle: 1s per swap

	// Keep for MCreator compatibility
	public static void execute() {
	}

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

		// Right click behavior:
		// - Not crouching: scan
		// - Crouching: teleport using current shuffled mode
		if (entity instanceof Player p && p.isShiftKeyDown()) {
			int mode = p.getPersistentData().getInt(NBT_MODE);
			boolean overworldMode = (mode == 1);
			handleTeleport(world, x, y, z, entity, itemstack, overworldMode);
			return;
		}

		handleScan(world, x, y, z, entity, itemstack);
	}

	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		Level level = event.getLevel();
		ItemStack stack = event.getItemStack();

		if (stack.getItem() == TheBackwoodsModItems.RESONANT_ROT_EFFIGY.get()) {
			execute(level, player.getX(), player.getY(), player.getZ(), player, stack);
		}
	}

	// While crouching with resonant effigy in main hand, shuffle mode and show actionbar.
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide())
			return;

		ItemStack main = player.getMainHandItem();
		if (main.isEmpty() || main.getItem() != TheBackwoodsModItems.RESONANT_ROT_EFFIGY.get()) {
			player.getPersistentData().putInt(NBT_SHUFFLE_T, 0);
			return;
		}

		if (player.isShiftKeyDown()) {
			int t = player.getPersistentData().getInt(NBT_SHUFFLE_T) + 1;
			player.getPersistentData().putInt(NBT_SHUFFLE_T, t);

			if (t >= SHUFFLE_INTERVAL_TICKS) {
				player.getPersistentData().putInt(NBT_SHUFFLE_T, 0);

				int mode = player.getPersistentData().getInt(NBT_MODE);
				mode = (mode == 0) ? 1 : 0; // shuffle between two modes
				player.getPersistentData().putInt(NBT_MODE, mode);

				if (mode == 0) {
					player.displayClientMessage(Component.literal("Mode: ATTUNED"), true);
				} else {
					player.displayClientMessage(Component.literal("Mode: FRACTURED"), true);
				}
			}
		} else {
			player.getPersistentData().putInt(NBT_SHUFFLE_T, 0);
		}
	}

	private static void handleScan(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (!(world instanceof Level level) || level.isClientSide())
			return;

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

				if (world instanceof ServerLevel serverLevel) {
					itemstack.hurtAndBreak(SCAN_DURABILITY_COST, serverLevel, null, _stkprov -> {
					});
				}
			} else {
				player.getCooldowns().addCooldown(itemstack.getItem(), SCAN_FAIL_COOLDOWN_TICKS);
				player.displayClientMessage(Component.literal("No woodbound entities sensed."), true);
			}
		}
	}

	private static void handleTeleport(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack, boolean forceOverworldMode) {
		ResourceKey<Level> currentDim = entity.level().dimension();
		ResourceKey<Level> overworld = Level.OVERWORLD;
		ResourceKey<Level> rotting = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:rotting"));
		ResourceKey<Level> still = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:the_still"));

		ResourceKey<Level> destinationType = null;

		// FRACTURED mode: any non-overworld -> overworld
		if (forceOverworldMode && currentDim != overworld) {
			destinationType = overworld;
		}
		// ATTUNED mode: original route Rotting -> Still
		else if (!forceOverworldMode && currentDim == rotting) {
			destinationType = still;
		}

		if (destinationType != null) {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.crimson_forest.mood")), SoundSource.MASTER, 3, 1);
				} else {
					_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ambient.crimson_forest.mood")), SoundSource.MASTER, 3, 1, false);
				}
			}

			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
				_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, forceOverworldMode ? 6 : 5, false, false));
			}

			if (entity instanceof Player _player) {
				_player.getCooldowns().addCooldown(itemstack.getItem(), TELEPORT_COOLDOWN_TICKS);
			}

			if (world instanceof ServerLevel serverLevel) {
				itemstack.hurtAndBreak(TELEPORT_DURABILITY_COST, serverLevel, null, _stkprov -> {
				});
			}

			final ResourceKey<Level> finalDestination = destinationType;
			TheBackwoodsMod.queueServerWork(TELEPORT_DELAY_TICKS, () -> {
				if (entity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
					ServerLevel nextLevel = _player.server.getLevel(finalDestination);

					if (nextLevel != null) {
						_player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
						_player.teleportTo(nextLevel, _player.getX(), 129, _player.getZ(), _player.getYRot(), _player.getXRot());

						// Safety for high-altitude teleports in ALL dimensions
						_player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));

						_player.connection.send(new ClientboundPlayerAbilitiesPacket(_player.getAbilities()));
						for (MobEffectInstance _effectinstance : _player.getActiveEffects())
							_player.connection.send(new ClientboundUpdateMobEffectPacket(_player.getId(), _effectinstance, false));
						_player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
					}
				}
			});

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