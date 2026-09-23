package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

import java.util.Map;
import java.util.UUID;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class MemoryShardRightclickedProcedure {

	private static final Map<UUID, LinkedList<PlayerState>> serverPlayerHistories = new ConcurrentHashMap<>();
	private static final Map<UUID, LinkedList<PlayerState>> clientPlayerHistories = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> serverRewindTicks = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> clientRewindTicks = new ConcurrentHashMap<>();

	private static Map<UUID, LinkedList<PlayerState>> getHistoryMap(Level level) {
		return level.isClientSide() ? clientPlayerHistories : serverPlayerHistories;
	}

	private static Map<UUID, Integer> getRewindTicksMap(Level level) {
		return level.isClientSide() ? clientRewindTicks : serverRewindTicks;
	}

	public static class PlayerState {
		public final double x, y, z;
		public final float yaw, pitch;
		public final float health;
		public final int food;
		public final double motionX, motionY, motionZ;

		public PlayerState(double x, double y, double z, float yaw, float pitch, float health, int food, double motionX, double motionY, double motionZ) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
			this.health = health;
			this.food = food;
			this.motionX = motionX;
			this.motionY = motionY;
			this.motionZ = motionZ;
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (!(entity instanceof Player player))
			return;

		UUID uuid = player.getUUID();
		Map<UUID, Integer> ticksMap = getRewindTicksMap(player.level());

		// Prevent triggering rewind if already active
		if (ticksMap.containsKey(uuid))
			return;

		int mode = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("mode");

		if (mode == 1) {
			// Chrono-Stasis Anchor Mode
			boolean active = player.getPersistentData().getBoolean("anchor_active");
			if (!active) {
				// Drop anchor!
				if (!player.getAbilities().instabuild) {
					itemstack.shrink(1);
				}
				player.getPersistentData().putBoolean("anchor_active", true);
				player.getPersistentData().putDouble("anchor_x", player.getX());
				player.getPersistentData().putDouble("anchor_y", player.getY());
				player.getPersistentData().putDouble("anchor_z", player.getZ());
				player.getPersistentData().putFloat("anchor_yaw", player.getYRot());
				player.getPersistentData().putFloat("anchor_pitch", player.getXRot());
				player.getPersistentData().putFloat("anchor_health", player.getHealth());
				player.getPersistentData().putInt("anchor_food", player.getFoodData().getFoodLevel());
				player.getPersistentData().putInt("anchor_ticks_left", 300); // 15 seconds

				player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.5F);
				player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.5F);
				player.displayClientMessage(Component.literal("§5[Chrono-Stasis] §dAnchor Dropped (15s)"), true);
			} else {
				// Snap back immediately!
				double ax = player.getPersistentData().getDouble("anchor_x");
				double ay = player.getPersistentData().getDouble("anchor_y");
				double az = player.getPersistentData().getDouble("anchor_z");
				float ayaw = player.getPersistentData().getFloat("anchor_yaw");
				float apitch = player.getPersistentData().getFloat("anchor_pitch");
				float ahealth = player.getPersistentData().getFloat("anchor_health");
				int afood = player.getPersistentData().getInt("anchor_food");

				if (player.level() instanceof ServerLevel serverLevel) {
					// Bright flash at start position
					serverLevel.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
				}

				player.absMoveTo(ax, ay, az, ayaw, apitch);
				player.setDeltaMovement(0, 0, 0);
				player.setHealth(ahealth);
				player.getFoodData().setFoodLevel(afood);

				player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
				player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);

				if (player.level() instanceof ServerLevel serverLevel) {
					serverLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 1.0, 0.5, 0.1);
					// Bright flash at destination position
					serverLevel.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
				}

				player.getPersistentData().putBoolean("anchor_active", false);
				player.displayClientMessage(Component.literal("§5[Chrono-Stasis] §dTimeline Snapped!"), true);
			}
			return;
		}

		// Mode 0: Rewind Mode
		Map<UUID, LinkedList<PlayerState>> historyMap = getHistoryMap(player.level());
		LinkedList<PlayerState> history = historyMap.get(uuid);
		if (history == null || history.size() < 10) {
			// Fail feedback
			player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8F, 0.5F);
			return;
		}

		// Consume one memory shard on use
		if (!player.getAbilities().instabuild) {
			itemstack.shrink(1);
		}

		// Calculate rewind duration (cap at 30 ticks of rewind time = 120 states popped at 4x speed)
		int duration = Math.min(30, history.size() / 4);
		ticksMap.put(uuid, duration);

		// Increase temporal instability on use
		if (!player.level().isClientSide()) {
			double currentInstability = player.getPersistentData().getDouble("temporal_instability");
			player.getPersistentData().putDouble("temporal_instability", currentInstability + 45.0);
		}

		// Play temporal activation sound
		player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6F, 1.8F);
		player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.5F);

		// Execute original commands/tags to satisfy MCreator's Blocky declarations
		if (world instanceof ServerLevel _level) {
			_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "clear");
		}
		
		final String _tagName = "tagName";
		final ItemStack _tagValue = new ItemStack(TheBackwoodsModItems.NULL_POINTERAXE.get());
		CustomData.update(DataComponents.CUSTOM_DATA, itemstack, tag -> tag.put(_tagName, _tagValue.saveOptional(world.registryAccess())));
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player == null)
			return;

		UUID uuid = player.getUUID();
		boolean isClient = player.level().isClientSide();
		Map<UUID, Integer> ticksMap = getRewindTicksMap(player.level());
		Map<UUID, LinkedList<PlayerState>> historyMap = getHistoryMap(player.level());

		// Shake/Swing detection for changing modes
		if (!isClient && player.swinging && player.swingTime == 0) {
			boolean anchorActive = player.getPersistentData().getBoolean("anchor_active");
			boolean midRewind = ticksMap.containsKey(uuid);
			if (!anchorActive && !midRewind) {
				ItemStack stack = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
				if (stack.isEmpty() || stack.getItem() != TheBackwoodsModItems.MEMORY_SHARD.get()) {
					stack = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
				}
				if (!stack.isEmpty() && stack.getItem() == TheBackwoodsModItems.MEMORY_SHARD.get()) {
					long currentTime = player.level().getGameTime();
					long lastShake = player.getPersistentData().getLong("last_shard_shake_time");
					int shakeCount = player.getPersistentData().getInt("shard_shake_count");
					int requiredShakes = player.getPersistentData().getInt("shard_required_shakes");
	
					if (currentTime - lastShake > 30 || requiredShakes == 0) {
						shakeCount = 0;
						requiredShakes = player.getRandom().nextInt(4) + 2; // min 2, max 5 inclusive
						player.getPersistentData().putInt("shard_required_shakes", requiredShakes);
					}
	
					shakeCount++;
					player.getPersistentData().putLong("last_shard_shake_time", currentTime);
					player.getPersistentData().putInt("shard_shake_count", shakeCount);
	
					player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 0.4F, 1.8F);
	
					if (shakeCount >= requiredShakes) {
						int currentMode = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("mode");
						int newMode = currentMode == 0 ? 1 : 0;
						CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("mode", newMode));
	
						player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, newMode == 1 ? 0.5F : 1.5F);
						player.displayClientMessage(Component.literal(newMode == 1 ? "§5[Temporal Anchor Mode] §dArmed" : "§b[Rewind Mode] §3Armed"), true);
						player.getPersistentData().putInt("shard_shake_count", 0);
						player.getPersistentData().putInt("shard_required_shakes", 0);
					}
				}
			}
		}

		// Chrono-Stasis Anchor ticking
		if (!isClient) {
			boolean active = player.getPersistentData().getBoolean("anchor_active");
			if (active) {
				int ticksLeft = player.getPersistentData().getInt("anchor_ticks_left");
				ticksLeft--;
				if (ticksLeft > 0) {
					player.getPersistentData().putInt("anchor_ticks_left", ticksLeft);
					
					// Spawn enderman portal particles
					if (player.level() instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.5, player.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
						
						// Ring of reverse portal particles at anchor spot
						double ax = player.getPersistentData().getDouble("anchor_x");
						double ay = player.getPersistentData().getDouble("anchor_y");
						double az = player.getPersistentData().getDouble("anchor_z");
						serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, ax, ay + 0.2, az, 2, 0.1, 0.1, 0.1, 0.02);
						
						// High pitch warning tick sound when nearing the time limit (last 5 seconds)
						if (ticksLeft <= 100) {
							if (ticksLeft % 20 == 0) {
								player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 2.0F);
							}
						}
					}
				} else {
					// Time limit reached! Teleport back automatically
					double ax = player.getPersistentData().getDouble("anchor_x");
					double ay = player.getPersistentData().getDouble("anchor_y");
					double az = player.getPersistentData().getDouble("anchor_z");
					float ayaw = player.getPersistentData().getFloat("anchor_yaw");
					float apitch = player.getPersistentData().getFloat("anchor_pitch");
					float ahealth = player.getPersistentData().getFloat("anchor_health");
					int afood = player.getPersistentData().getInt("anchor_food");

					if (player.level() instanceof ServerLevel serverLevel) {
						// Bright flash at start position
						serverLevel.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
					}

					player.absMoveTo(ax, ay, az, ayaw, apitch);
					player.setDeltaMovement(0, 0, 0);
					player.setHealth(ahealth);
					player.getFoodData().setFoodLevel(afood);

					player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
					player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);

					if (player.level() instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 1.0, 0.5, 0.1);
						// Bright flash at destination position
						serverLevel.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
					}

					player.getPersistentData().putBoolean("anchor_active", false);
					player.displayClientMessage(Component.literal("§5[Chrono-Stasis] §dTimeline Snapped!"), true);
				}
			}
		}

		// Server-side temporal instability & paradox updates
		if (!isClient) {
			double instability = player.getPersistentData().getDouble("temporal_instability");
			
			if (ticksMap.containsKey(uuid)) {
				// Actively rewinding: decay instability rapidly (-1.5 per tick)
				if (instability > 0) {
					instability = Math.max(0, instability - 1.5);
					player.getPersistentData().putDouble("temporal_instability", instability);
				}
				
				// Smoothly reduce existing Nausea effect duration as they keep rewinding
				if (player.hasEffect(MobEffects.CONFUSION)) {
					MobEffectInstance activeNausea = player.getEffect(MobEffects.CONFUSION);
					if (activeNausea != null) {
						int currentDur = activeNausea.getDuration();
						player.removeEffect(MobEffects.CONFUSION);
						int newDur = currentDur - 8; // Reduce nausea quickly
						if (newDur > 0) {
							player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, newDur, 0, false, false, true));
						}
					}
				}
			} else {
				// Normal play: decay instability slowly (-0.1 per tick, -2.0 per second)
				if (instability > 0) {
					instability = Math.max(0, instability - 0.1);
					player.getPersistentData().putDouble("temporal_instability", instability);
				}
			}

			// Apply Nausea when unstable
			if (instability > 20.0) {
				if (!player.hasEffect(MobEffects.CONFUSION)) {
					player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
				}
			}

			// Medium Instability (Coherence < 50%): Darkness flickering glitch and audio warping
			if (instability > 50.0) {
				if (player.getRandom().nextFloat() < 0.04F) {
					player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, false, false, true));
					player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4F, 0.1F);
				}
				if (player.level() instanceof ServerLevel serverLevel) {
					serverLevel.sendParticles(ParticleTypes.FLASH, player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.5, player.getY() + 1.0 + (player.getRandom().nextDouble() - 0.5), player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.5, 1, 0.0, 0.0, 0.0, 0.0);
				}
			}

			// Extreme Instability (Coherence < 20%): Temporal Paradox! Take damage and glitch severely
			if (instability > 80.0) {
				if (player.getRandom().nextFloat() < 0.05F) {
					player.hurt(player.damageSources().magic(), 1.0F); // 0.5 hearts temporal friction damage
					player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6F, 0.1F);
					if (player.level() instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
					}
				}
			}
		}

		if (ticksMap.containsKey(uuid)) {
			int ticksLeft = ticksMap.get(uuid);
			if (ticksLeft > 0) {
				LinkedList<PlayerState> history = historyMap.get(uuid);
				if (history != null) {
					PlayerState targetState = null;
					synchronized (history) {
						if (!history.isEmpty()) {
							int steps = Math.min(4, history.size());
							for (int i = 0; i < steps; i++) {
								PlayerState popped = history.pollLast();
								if (popped != null) {
									targetState = popped;
								}
							}
						}
					}

					if (targetState != null) {
						// Smoothly update location & rotation
						player.absMoveTo(targetState.x, targetState.y, targetState.z, targetState.yaw, targetState.pitch);
						player.setDeltaMovement(0, 0, 0);

						// Gradual reverse restorations with smooth health interpolation
						float currentHealth = player.getHealth();
						float targetHealth = targetState.health;
						float diff = targetHealth - currentHealth;
						if (Math.abs(diff) > 0.01F) {
							float step = diff * 0.25F;
							if (Math.abs(step) < 0.05F) {
								step = Math.signum(diff) * 0.05F;
							}
							float nextHealth = currentHealth + step;
							if ((diff > 0 && nextHealth > targetHealth) || (diff < 0 && nextHealth < targetHealth)) {
								nextHealth = targetHealth;
							}
							player.setHealth(Math.min(player.getMaxHealth(), Math.max(0.0F, nextHealth)));
						} else {
							player.setHealth(targetHealth);
						}
						player.getFoodData().setFoodLevel(targetState.food);

						player.setInvulnerable(true);
						player.fallDistance = 0.0F;

						// Particle feedback on server
						if (!isClient && player.level() instanceof ServerLevel serverLevel) {
							serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.25, 0.5, 0.25, 0.05);
							serverLevel.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.2, 0.4, 0.2, 0.02);
						}

						// Client-side ticking sound and shader hook
						if (isClient) {
							triggerClientRewindTick();
						}
					}
					ticksMap.put(uuid, ticksLeft - 1);
				} else {
					finishRewind(player);
				}
			} else {
				finishRewind(player);
			}
		} else {
			// Record normal history (cap at 120 ticks) only if player carries a Memory Shard
			if (hasMemoryShardInInventory(player)) {
				LinkedList<PlayerState> history = historyMap.computeIfAbsent(uuid, k -> new LinkedList<>());
				PlayerState currentState = new PlayerState(
					player.getX(), player.getY(), player.getZ(),
					player.getYRot(), player.getXRot(),
					player.getHealth(), player.getFoodData().getFoodLevel(),
					player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z
				);
				synchronized (history) {
					history.addLast(currentState);
					while (history.size() > 120) {
						history.removeFirst();
					}
				}
			} else {
				historyMap.remove(uuid);
			}
		}
	}

	private static boolean hasMemoryShardInInventory(Player player) {
		for (ItemStack stack : player.getInventory().items) {
			if (!stack.isEmpty() && stack.getItem() == TheBackwoodsModItems.MEMORY_SHARD.get()) {
				return true;
			}
		}
		for (ItemStack stack : player.getInventory().offhand) {
			if (!stack.isEmpty() && stack.getItem() == TheBackwoodsModItems.MEMORY_SHARD.get()) {
				return true;
			}
		}
		for (ItemStack stack : player.getInventory().armor) {
			if (!stack.isEmpty() && stack.getItem() == TheBackwoodsModItems.MEMORY_SHARD.get()) {
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public static void onPlayerDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
		if (event.getEntity() instanceof Player player) {
			getHistoryMap(player.level()).remove(player.getUUID());
			getRewindTicksMap(player.level()).remove(player.getUUID());
			player.getPersistentData().putBoolean("anchor_active", false);
		}
	}

	private static void finishRewind(Player player) {
		UUID uuid = player.getUUID();
		getRewindTicksMap(player.level()).remove(uuid);
		player.setInvulnerable(false);
		player.fallDistance = 0.0F;

		Level level = player.level();
		if (level.isClientSide()) {
			ClientEvents.disableShader();
		} else {
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
		}
	}

	private static void triggerClientRewindTick() {
		ClientEvents.enableShader();
		ClientEvents.playTickingSound();
	}

	public static class ClientEvents {
		private static boolean shaderActive = false;
		private static int soundCooldown = 0;

		public static void enableShader() {
			if (!shaderActive) {
				shaderActive = true;
				try {
					net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
					if (mc.gameRenderer != null) {
						mc.gameRenderer.loadEffect(ResourceLocation.parse("minecraft:shaders/post/blur.json"));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public static void disableShader() {
			if (shaderActive) {
				shaderActive = false;
				try {
					net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
					if (mc.gameRenderer != null) {
						mc.gameRenderer.shutdownEffect();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public static void playTickingSound() {
			if (soundCooldown <= 0) {
				net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
				if (mc.player != null && mc.level != null) {
					mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
						SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS,
						0.4F, 1.8F, false);
				}
				soundCooldown = 3;
			} else {
				soundCooldown--;
			}
		}
	}
}

// Target Minecraft Version: 1.21.1
