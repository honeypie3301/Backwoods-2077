/*
 * The code of this mod element is always locked.
*/
package net.mcreator.thebackwoods;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;

import net.mcreator.thebackwoods.entity.RotEntity;

import java.util.List;

@EventBusSubscriber
public class RotDataGuardian {

	public static boolean ENABLE_GUARDIAN = true;

	public static boolean SILENT_SECURITY_MODE = false;
	public static boolean ENABLE_FEEDBACK_PENALTY = true;
	public static boolean ENABLE_LIGHTNING_STRIKES = false;
	public static boolean ENABLE_SOUND_EFFECTS = false;
	public static boolean ENABLE_PARTICLE_EFFECTS = false;
	public static boolean ENABLE_BROADCAST_WARNINGS = false;
	
	public static String MSG_COMMAND_BLOCKED = "§c[System] Lethal Feedback Protocol: Remote administrative override blocked on structural target.§r";
	public static String MSG_TAMPER_DETECTED = "§c[System] Structural anomaly detected (%s). Re-synchronizing atomics.§r";
	public static String MSG_FEEDBACK_TRIGGERED = "§4[System] Lethal Feedback Protocol engaged. Neural suppression active. Access denied.§r";

	public RotDataGuardian() {
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new RotDataGuardian();
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
	}

	@EventBusSubscriber
	private static class RotDataGuardianForgeBusEvents {

		@SubscribeEvent
		public static void serverLoad(ServerStartingEvent event) {
		}

		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public static void onCommandExecuted(CommandEvent event) {
			if (!ENABLE_GUARDIAN) {
				return;
			}

			String rawCommand = event.getParseResults().getReader().getString();
			if (rawCommand == null) {
				return;
			}

			String cmdLower = rawCommand.trim().toLowerCase();

			boolean isTamperCmd = cmdLower.startsWith("attribute") || cmdLower.startsWith("/attribute")
					|| cmdLower.startsWith("data") || cmdLower.startsWith("/data")
					|| cmdLower.startsWith("kill") || cmdLower.startsWith("/kill");

			if (isTamperCmd) {
				if (isCommandTargetingRot(cmdLower)) {
					if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
						ServerLevel level = (ServerLevel) player.level();
						
						boolean rotExists = false;
						for (net.minecraft.world.entity.Entity ent : level.getAllEntities()) {
							if (ent instanceof RotEntity && ent.isAlive()) {
								rotExists = true;
								break;
							}
						}
						
						if (rotExists) {
							event.setCanceled(true);
							if (SILENT_SECURITY_MODE) {
								player.sendSystemMessage(Component.literal(MSG_COMMAND_BLOCKED));
							} else {
								triggerGlobalTamperBacklash(level, player, "COMMAND_INJECTION: " + rawCommand);
							}
						}
					}
				}
			}
		}

		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public static void onLivingDeath(LivingDeathEvent event) {
			if (!ENABLE_GUARDIAN) {
				return;
			}
			if (event.getEntity() instanceof RotEntity rot && !rot.level().isClientSide()) {
				boolean isDying = false;
				try {
					isDying = rot.getEntityData().get(RotEntity.DATA_isDeath);
				} catch (Exception e) {}
				if (!isDying) {
					isDying = rot.getPersistentData().getBoolean("rot_death_sequence_active");
				}
				if (!isDying) {
					event.setCanceled(true);
					double maxHp = rot.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue();
					rot.setHealth((float) maxHp);
					rot.getPersistentData().putDouble("sentinel_validated_health", maxHp);
				}
			}
		}

		@SubscribeEvent
		public static void onEntityTick(EntityTickEvent.Pre event) {
			if (!ENABLE_GUARDIAN) {
				return;
			}

			Entity entity = event.getEntity();
			if (entity instanceof RotEntity rot && !rot.level().isClientSide() && rot.isAlive()) {
				ServerLevel level = (ServerLevel) rot.level();

				double validatedMaxHealth = rot.getPersistentData().getDouble("sentinel_validated_max_health");
				double validatedHealth = rot.getPersistentData().getDouble("sentinel_validated_health");
				int damageGraceTicks = rot.getPersistentData().getInt("sentinel_damage_grace_ticks");
				int expectedGoals = rot.getPersistentData().getInt("sentinel_expected_goals");
				int expectedTargets = rot.getPersistentData().getInt("sentinel_expected_targets");

				double currentBaseMaxHealth = rot.getAttribute(Attributes.MAX_HEALTH).getBaseValue();

				if (validatedMaxHealth < 1.0) {
					validatedMaxHealth = currentBaseMaxHealth;
					validatedHealth = rot.getHealth();
					expectedGoals = rot.goalSelector.getAvailableGoals().size();
					expectedTargets = rot.targetSelector.getAvailableGoals().size();
					rot.getPersistentData().putDouble("sentinel_validated_max_health", validatedMaxHealth);
					rot.getPersistentData().putDouble("sentinel_validated_health", validatedHealth);
					rot.getPersistentData().putInt("sentinel_expected_goals", expectedGoals);
					rot.getPersistentData().putInt("sentinel_expected_targets", expectedTargets);
				}

				if (currentBaseMaxHealth > validatedMaxHealth) {
					validatedMaxHealth = currentBaseMaxHealth;
					rot.getPersistentData().putDouble("sentinel_validated_max_health", validatedMaxHealth);
				} else if (currentBaseMaxHealth < validatedMaxHealth) {
					rot.getAttribute(Attributes.MAX_HEALTH).setBaseValue(validatedMaxHealth);
					rot.setHealth((float) validatedHealth);
				}

				if (damageGraceTicks > 0) {
					damageGraceTicks--;
					rot.getPersistentData().putInt("sentinel_damage_grace_ticks", damageGraceTicks);
				}

				double currentHealth = rot.getHealth();
				if (currentHealth < validatedHealth) {
					if (damageGraceTicks > 0) {
						rot.getPersistentData().putDouble("sentinel_validated_health", currentHealth);
					} else {
						rot.setHealth((float) validatedHealth);
					}
				} else if (currentHealth > validatedHealth) {
					rot.getPersistentData().putDouble("sentinel_validated_health", currentHealth);
				}

				if (rot.tickCount % 5 == 0) {
					LivingEntity combatTarget = rot.getTarget();
					if (combatTarget != null) {
						String targetName = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(combatTarget.getType()).toString().toLowerCase();
						if (targetName.contains("rot") || targetName.contains("splinter")) {
							rot.setTarget(null);
							rot.setLastHurtByMob(null);
						} else if (combatTarget instanceof net.minecraft.world.entity.player.Player) {
							boolean isDueling = rot.getPersistentData().getBoolean("is_dueling");
							if (!isDueling) {
								rot.setTarget(null);
								rot.setLastHurtByMob(null);
							}
						}
					}

					boolean isManifesting = rot.getPersistentData().getBoolean("rot_manifesting");
					boolean isDying = false;
					try {
						isDying = rot.getEntityData().get(RotEntity.DATA_isDeath);
					} catch (Exception e) {}
					if (!isDying) {
						isDying = rot.getPersistentData().getBoolean("rot_death_sequence_active");
					}
					if (!isManifesting && !isDying) {
						if (rot.isNoAi()) {
							rot.setNoAi(false);
						}
						if (rot.isSilent()) {
							rot.setSilent(false);
						}
						if (rot.isInvisible()) {
							rot.setInvisible(false);
						}
					}
					if (rot.isVehicle()) {
						rot.ejectPassengers();
					}
					if (rot.isNoGravity() && !isDying) {
						rot.setNoGravity(false);
					}

					java.util.List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> debuffsToRemove = new java.util.ArrayList<>();
					for (MobEffectInstance effect : rot.getActiveEffects()) {
						String effId = effect.getEffect().unwrapKey().map(k -> k.location().toString().toLowerCase()).orElse("");
						if (effect.getAmplifier() >= 4 || effId.contains("torment") || effId.contains("insanity") || effId.contains("mind_control") || effId.contains("arphex")) {
							debuffsToRemove.add(effect.getEffect());
						}
					}
					for (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effHolder : debuffsToRemove) {
						rot.removeEffect(effHolder);
					}
				}
			}
		}

		@SubscribeEvent
		public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
			if (!ENABLE_GUARDIAN) {
				return;
			}

			if (event.getEntity() instanceof RotEntity && !event.getEntity().level().isClientSide()) {
				RotEntity rot = (RotEntity) event.getEntity();
				rot.getPersistentData().putInt("sentinel_damage_grace_ticks", 20);
			}
		}

		private static void triggerTamperBacklash(ServerLevel level, RotEntity rot, String reason) {
			boolean showVisEffects = !SILENT_SECURITY_MODE;

			if (showVisEffects && ENABLE_SOUND_EFFECTS) {
				level.playSound(null, rot.getX(), rot.getY(), rot.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.5F);
				level.playSound(null, rot.getX(), rot.getY(), rot.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.0F, 0.5F);
			}

			if (showVisEffects && ENABLE_PARTICLE_EFFECTS) {
				level.sendParticles(ParticleTypes.DRAGON_BREATH, rot.getX(), rot.getY() + 1.0, rot.getZ(), 120, 1.5, 1.5, 1.5, 0.15);
				level.sendParticles(ParticleTypes.PORTAL, rot.getX(), rot.getY() + 1.0, rot.getZ(), 150, 2.0, 2.0, 2.0, 0.5);
				level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, rot.getX(), rot.getY() + 1.0, rot.getZ(), 5, 1.0, 1.0, 1.0, 0.0);
			}

			AABB searchBox = rot.getBoundingBox().inflate(32.0);
			List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, searchBox);
			boolean playerInvolved = false;
			for (ServerPlayer p : nearbyPlayers) {
				if (!p.isCreative() && !p.isSpectator()) {
					playerInvolved = true;
					if (ENABLE_LIGHTNING_STRIKES) {
						net.minecraft.world.entity.LightningBolt lightning = new net.minecraft.world.entity.LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
						lightning.setPos(p.getX(), p.getY(), p.getZ());
						level.addFreshEntity(lightning);
					}

					if (ENABLE_FEEDBACK_PENALTY) {
						applyLethalFeedback(p);
					}
				}
			}

			if (showVisEffects && ENABLE_BROADCAST_WARNINGS && playerInvolved) {
				String alertMsg = String.format(MSG_TAMPER_DETECTED, reason);
				level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(alertMsg), false);
			}
		}

		private static void triggerGlobalTamperBacklash(ServerLevel level, ServerPlayer player, String reason) {
			if (ENABLE_LIGHTNING_STRIKES) {
				net.minecraft.world.entity.LightningBolt lightning = new net.minecraft.world.entity.LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
				lightning.setPos(player.getX(), player.getY(), player.getZ());
				level.addFreshEntity(lightning);
			}

			if (ENABLE_FEEDBACK_PENALTY) {
				applyLethalFeedback(player);
			}
		}
		
		private static void applyLethalFeedback(ServerPlayer player) {
			net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceLocation.parse("the_backwoods:cellular_collapse")).ifPresent(holder -> {
				player.addEffect(new MobEffectInstance(holder, 600, 1));
			});
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 600, 0));
			player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 600, 0));
			player.addEffect(new MobEffectInstance(MobEffects.WITHER, 600, 3));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 4));
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 4));
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 4));
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 2));
			player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 2));
			player.sendSystemMessage(Component.literal(MSG_FEEDBACK_TRIGGERED));
		}
	}

	public static boolean isCommandTargetingRot(String cmd) {
		cmd = cmd.toLowerCase().trim();
		if (cmd.startsWith("/")) {
			cmd = cmd.substring(1);
		}
		
		String[] parts = cmd.split("\\s+");
		if (parts.length < 2) {
			return false;
		}
		
		String action = parts[0];
		String target = parts[1];
		
		if (!action.equals("kill") && !action.equals("data") && !action.equals("attribute")) {
			return false;
		}
		
		if (target.contains("@e")) {
			if (target.contains("[") && target.contains("]")) {
				String selectorParams = target.substring(target.indexOf("[") + 1, target.indexOf("]"));
				if (selectorParams.contains("type=!")) {
					return true;
				}
				if (selectorParams.contains("type=")) {
					if (selectorParams.contains("the_backwoods:rot") || selectorParams.contains("thebackwoods:rot") || selectorParams.contains("rot")) {
						return true;
					}
					return false;
				}
				return true;
			} else {
				return true;
			}
		}
		
		if (cmd.contains("the_backwoods:rot") || cmd.contains("thebackwoods:rot") || cmd.contains(":rot")) {
			return true;
		}
		
		if (target.startsWith("@s") || target.startsWith("@p") || target.startsWith("@r") || target.startsWith("@a")) {
			return false;
		}
		
		return false;
	}
} // 1.21.1
