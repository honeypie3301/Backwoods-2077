package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
// 1.21.1
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;

import net.mcreator.thebackwoods.entity.RotEntity;

import javax.annotation.Nullable;

import java.util.Comparator;

@EventBusSubscriber
public class RotEntityIsHurtProcedure {
	public static boolean ENABLE_ROT_DIALOGUES = false;

	private static final String[] DIALOGUES_STAGE_1 = {
		"I have felt deeper cuts than this.",
		"Your strikes only peel the bark.",
		"Flesh tires. The rot remains."
	};

	private static final String[] DIALOGUES_STAGE_2 = {
		"I thin yet the hunger widens.",
		"Every wound leaves more room to take.",
		"You cannot carve away what is already hollow."
	};

	private static final String[] DIALOGUES_STAGE_3 = {
		"Pruning...",
		"The roots do not bleed.",
		"You will wither long before I fall."
	};

	private static final String[] DIALOGUES_DEATH = {
		"This vessel is spent.",
		"You have broken nothing.",
		"Dissolving... only to take root elsewhere."
	};

	@SubscribeEvent
	public static void onEntityAttacked(LivingDamageEvent.Pre event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		Entity attacker = null;
		Entity foundPlayer = null;
		double loop = 0;
		double particleAmount = 0;
		double masterRadius = 0;
		if (entity instanceof RotEntity) {
			if (event instanceof LivingDamageEvent.Pre preEvent) {
				if (entity.getPersistentData().getBoolean("unlocked_solar_beam")) {
					DamageSource src = preEvent.getSource();
					if (src.is(DamageTypes.IN_FIRE) || src.is(DamageTypes.ON_FIRE) || src.is(DamageTypes.LAVA)
						|| src.is(DamageTypes.HOT_FLOOR) || src.is(DamageTypes.FIREBALL) || src.is(DamageTypes.UNATTRIBUTED_FIREBALL)
						|| src.is(DamageTypes.CAMPFIRE)) {
						preEvent.setNewDamage(0.0F);
						return;
					}
				}
				if (entity.getPersistentData().getBoolean("unlocked_cryo_beam")) {
					DamageSource src = preEvent.getSource();
					if (src.is(DamageTypes.FREEZE)) {
						preEvent.setNewDamage(0.0F);
						return;
					}
				}
				if (entity.getPersistentData().getBoolean("rot_phase_shifting")) {
					double mastery = entity.getPersistentData().getDouble("rot_phase_mastery");
					double leakChance = mastery < 0.35 ? 0.35 : (mastery < 0.75 ? 0.10 : 0.0);
					if (Math.random() < leakChance) {
						float partialRatio = mastery < 0.35 ? 0.40F : 0.15F;
						preEvent.setNewDamage(preEvent.getNewDamage() * partialRatio);
						if (world instanceof ServerLevel _level) {
							_level.sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 8, 0.2, 0.4, 0.2, 0.05);
						}
					} else {
						preEvent.setNewDamage(0.0F);
						if (world instanceof ServerLevel _level) {
							_level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 5, 0.2, 0.3, 0.2, 0.02);
						}
						return;
					}
				}
			}

			double activeAdaptTicks = entity.getPersistentData().getDouble("controlled_adaptation_ticks");
			if (activeAdaptTicks > 0) {
				if (event instanceof LivingDamageEvent.Pre preEv) {
					float originalDmg = preEv.getNewDamage();
					float reducedDmg = originalDmg * 0.10F;
					preEv.setNewDamage(reducedDmg);

					if (originalDmg >= 12.0F) {
						entity.getPersistentData().putDouble("controlled_adaptation_ticks", 0.0);
						entity.getPersistentData().putDouble("controlled_adaptation_cooldown", 500.0);
						if (world instanceof ServerLevel _sL) {
							_sL.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
						}
					}
				}
			}

			if (event instanceof LivingDamageEvent.Pre preEv) {
				double bioResist = entity.getPersistentData().getDouble("sentinel_biological_resistance_mult");
				if (bioResist > 0.0 && bioResist < 1.0) {
					preEv.setNewDamage((float) (preEv.getNewDamage() * bioResist));
				}

				DamageSource source = preEv.getSource();
				String category = classifyDamage(source);
				double mult = 1.0;

				if ("PROJECTILE".equals(category) || "ARTILLERY".equals(category)) {
					double projRes = entity.getPersistentData().getDouble("sentinel_bio_projectile_resist");
					projRes = Math.min(0.90, projRes + 0.15);
					entity.getPersistentData().putDouble("sentinel_bio_projectile_resist", projRes);
					mult = 1.0 - projRes;
				} else if ("EXPLOSION".equals(category)) {
					double expRes = entity.getPersistentData().getDouble("sentinel_bio_explosion_resist");
					expRes = Math.min(0.95, expRes + 0.25);
					entity.getPersistentData().putDouble("sentinel_bio_explosion_resist", expRes);
					mult = 1.0 - expRes;
				} else if ("MAGIC".equals(category)) {
					double magRes = entity.getPersistentData().getDouble("sentinel_bio_magic_resist");
					magRes = Math.min(0.90, magRes + 0.20);
					entity.getPersistentData().putDouble("sentinel_bio_magic_resist", magRes);
					mult = 1.0 - magRes;
				} else if ("MELEE".equals(category)) {
					double melRes = entity.getPersistentData().getDouble("sentinel_bio_melee_resist");
					melRes = Math.min(0.70, melRes + 0.05);
					entity.getPersistentData().putDouble("sentinel_bio_melee_resist", melRes);
					mult = 1.0 - melRes;
				}

				preEv.setNewDamage((float) (preEv.getNewDamage() * mult));
			}

			if (entity.getPersistentData().getBoolean("is_blocking")) {
				if (event instanceof LivingDamageEvent.Pre preEvent) {
					preEvent.setNewDamage(preEvent.getNewDamage() * 0.01F);
				}
			}

			long currentHitTick = (world instanceof Level _lvl) ? _lvl.getGameTime() : entity.tickCount;
			double lastHitTick = entity.getPersistentData().getDouble("sentinel_last_hit_tick");
			double sustainedHits = entity.getPersistentData().getDouble("sentinel_sustained_bullet_hits");

			if ((currentHitTick - (long) lastHitTick) <= 4) {
				sustainedHits = Math.min(15.0, sustainedHits + 1.0);
				entity.getPersistentData().putDouble("sentinel_last_high_rpm_tick", currentHitTick);
			} else if ((currentHitTick - (long) lastHitTick) > 20) {
				sustainedHits = Math.max(0.0, sustainedHits - 2.0);
			}
			entity.getPersistentData().putDouble("sentinel_last_hit_tick", currentHitTick);
			entity.getPersistentData().putDouble("sentinel_sustained_bullet_hits", sustainedHits);

			if (sustainedHits >= 3.0 && event instanceof LivingDamageEvent.Pre preEv) {
				double dampeningRatio = Math.max(0.12, 1.0 - (sustainedHits * 0.08));
				preEv.setNewDamage((float) (preEv.getNewDamage() * dampeningRatio));
				if (world instanceof ServerLevel _level) {
					_level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 3, 0.2, 0.3, 0.2, 0.05);
				}
			}

			if (event instanceof LivingDamageEvent.Pre cbcPreEv && isCBCProjectileOrDamage(cbcPreEv.getSource())) {
				double cbcHits = entity.getPersistentData().getDouble("sentinel_cbc_hits") + 1.0;
				entity.getPersistentData().putDouble("sentinel_cbc_hits", cbcHits);
				entity.getPersistentData().putBoolean("sentinel_adapted_cbc", true);

				float cbcDampening = cbcHits <= 1.0 ? 0.70F : (cbcHits <= 2.0 ? 0.35F : 0.10F);
				cbcPreEv.setNewDamage(cbcPreEv.getNewDamage() * cbcDampening);

				double adaptedPunchDmg = entity.getPersistentData().getDouble("adapted_punch_damage");
				entity.getPersistentData().putDouble("adapted_punch_damage", Math.min(60.0, Math.max(12.0, adaptedPunchDmg) + 3.5));

				if (world instanceof ServerLevel sLvl) {
					sLvl.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 8, 0.3, 0.3, 0.3, 0.15);
					sLvl.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + 1.0, entity.getZ(), 5, 0.25, 0.25, 0.25, 0.05);
					sLvl.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + 1.0, entity.getZ(), 1, 0.1, 0.1, 0.1, 0.02);
					sLvl.playSound(null, entity.getX(), entity.getY(), entity.getZ(), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")), SoundSource.HOSTILE, 1.2F, 1.5F);
					sLvl.playSound(null, entity.getX(), entity.getY(), entity.getZ(), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.shield.block")), SoundSource.HOSTILE, 1.4F, 0.75F);
				}

				DamageSource cbcSrc = cbcPreEv.getSource();
				Entity directShooter = cbcSrc.getEntity();
				if (directShooter != null) {
					entity.getPersistentData().putDouble("sentinel_artillery_target_x", directShooter.getX());
					entity.getPersistentData().putDouble("sentinel_artillery_target_y", directShooter.getY());
					entity.getPersistentData().putDouble("sentinel_artillery_target_z", directShooter.getZ());
					entity.getPersistentData().putDouble("sentinel_artillery_target_tick", currentHitTick);
				}
			}
			attacker = (entity instanceof LivingEntity _entity) ? _entity.getLastHurtByMob() : null;
			if (event instanceof LivingDamageEvent.Pre preEvent) {
				entity.getPersistentData().putString("sentinel_last_damage_category", classifyDamage(preEvent.getSource()));
			}
			foundPlayer = findEntityInWorldRange(world, Player.class, x, y, z, 64);
			Entity targetRecipient = (attacker instanceof Player) ? attacker : foundPlayer;

			if (attacker != null) {
				String actualAttackType = "MELEE";
				if (event instanceof LivingDamageEvent.Pre preEvent) {
					String category = classifyDamage(preEvent.getSource());
					if ("PROJECTILE".equals(category) || "ARTILLERY".equals(category)) {
						actualAttackType = "RANGED";
					} else if ("EXPLOSION".equals(category)) {
						actualAttackType = "EXPLOSION";
					} else if ("MAGIC".equals(category)) {
						actualAttackType = "MAGIC";
					} else if (attacker instanceof LivingEntity livAttacker && livAttacker.fallDistance > 0.0F && !livAttacker.onGround() && !livAttacker.isInWater()) {
						actualAttackType = "CRIT_ATTACK";
					}
				}
				RotOnEntityTickUpdateProcedure.UniversalCombatPredictionEngine.recordActualAttack(entity, attacker, actualAttackType);
				RotOnEntityTickUpdateProcedure.UniversalCombatPredictionEngine.recordRotDamage(entity, attacker);
				if (hasEntityInInventory(attacker, new ItemStack(Items.TOTEM_OF_UNDYING))) {
					if (ENABLE_ROT_DIALOGUES && entity.getPersistentData().getDouble("rot_totem_dialogue_fired") == 0.0) {
						entity.getPersistentData().putDouble("rot_totem_dialogue_fired", 1.0);
						if (attacker instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("I see the false life you clutch."), true);
					}
				}
			}
			boolean isDeathActive = false;
			if (entity instanceof LivingEntity _livEnt) {
				boolean isAlreadyDead = false;
				try {
					isAlreadyDead = entity.getEntityData().get(net.mcreator.thebackwoods.entity.RotEntity.DATA_isDeath);
				} catch (Exception e) {}
				isDeathActive = isAlreadyDead || entity.getPersistentData().getBoolean("rot_death_sequence_active");

				float currentHp = _livEnt.getHealth();
				float incomingDmg = 0.0F;
				if (event instanceof LivingDamageEvent.Pre preEv) {
					incomingDmg = preEv.getNewDamage();
				} else if (event instanceof LivingIncomingDamageEvent incEv) {
					incomingDmg = incEv.getAmount();
				}

				if (isDeathActive || currentHp <= 20.0F || (currentHp - incomingDmg) <= 0.0F || _livEnt.isDeadOrDying()) {
					if (event instanceof LivingDamageEvent.Pre preEv) {
						preEv.setNewDamage(0.0F);
					} else if (event instanceof LivingIncomingDamageEvent incEv) {
						incEv.setAmount(0.0F);
					}
					_livEnt.setHealth(1.0F);
					_livEnt.deathTime = 0;
					_livEnt.setInvulnerable(true);

					if (!isAlreadyDead && !entity.getPersistentData().getBoolean("rot_death_sequence_active")) {
						try {
							entity.getEntityData().set(net.mcreator.thebackwoods.entity.RotEntity.DATA_isDeath, true);
						} catch (Exception e) {}
						entity.getPersistentData().putBoolean("rot_death_sequence_active", true);
						entity.getPersistentData().putDouble("rot_death_ticks", 240.0);
						entity.getPersistentData().putDouble("rot_death_start_x", entity.getX());
						entity.getPersistentData().putDouble("rot_death_start_y", entity.getY());
						entity.getPersistentData().putDouble("rot_death_start_z", entity.getZ());
						double randomTargetHeight = 12.0 + (Math.random() * 4.0);
						entity.getPersistentData().putDouble("rot_death_target_height", randomTargetHeight);
						entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
						entity.hasImpulse = false;
						entity.noPhysics = true;

						triggerStageDialogue(entity, targetRecipient, DIALOGUES_DEATH, "rot_dialogue_death_fired");

						ServerPlayer targetPlayer = null;
						if (attacker instanceof ServerPlayer sp) {
							targetPlayer = sp;
						} else if (foundPlayer instanceof ServerPlayer sp) {
							targetPlayer = sp;
						}

						if (targetPlayer != null && targetPlayer.level() instanceof ServerLevel _level) {
							AdvancementHolder _adv = _level.getServer().getAdvancements().get(ResourceLocation.parse("the_backwoods:rot_boss"));
							if (_adv != null) {
								AdvancementProgress _ap = targetPlayer.getAdvancements().getOrStartProgress(_adv);
								if (!_ap.isDone()) {
									for (String criteria : _ap.getRemainingCriteria())
										targetPlayer.getAdvancements().award(_adv, criteria);
								}
							}
						}
					}
					return;
				}
			}

			float hp = (entity instanceof LivingEntity _livEnt) ? _livEnt.getHealth() : -1;
			if (hp > 0 && !isDeathActive) {
				if (hp <= 50) {
					triggerStageDialogue(entity, targetRecipient, DIALOGUES_STAGE_3, "msg3_fired");
				} else if (hp <= 100) {
					triggerStageDialogue(entity, targetRecipient, DIALOGUES_STAGE_2, "msg2_fired");
				} else if (hp <= 250) {
					triggerStageDialogue(entity, targetRecipient, DIALOGUES_STAGE_1, "msg1_fired");
				}
			}
		} else {
			if (event instanceof LivingDamageEvent.Pre preEvent) {
				DamageSource source = preEvent.getSource();
				Entity directAttacker = source.getEntity();
				if (directAttacker instanceof RotEntity) {
					boolean isHeavyLeft = directAttacker.getPersistentData().getBoolean("is_heavy_left_punching");
					boolean isHeavyRight = directAttacker.getPersistentData().getBoolean("is_heavy_right_punching");
					double slamTicks = directAttacker.getPersistentData().getDouble("rot_slam_ticks");
					double overheadTicks = directAttacker.getPersistentData().getDouble("rot_overhead_ticks");
					boolean isSpecialAbility = isHeavyLeft || isHeavyRight || (slamTicks > 0) || (overheadTicks > 0);

					if (!isSpecialAbility) {
						if (preEvent.getNewDamage() > 18.0F) {
							preEvent.setNewDamage(18.0F);
						}
					}

					if (entity instanceof LivingEntity victimLiving) {
						int armorValue = victimLiving.getArmorValue();
						float initialDamage = preEvent.getNewDamage();

						if (armorValue >= 20 || (initialDamage > 2.0F && preEvent.getNewDamage() < 1.0F)) {
							double frustration = directAttacker.getPersistentData().getDouble("sentinel_armor_frustration");
							directAttacker.getPersistentData().putDouble("sentinel_armor_frustration", Math.min(100.0, frustration + 10.0));
						}
					}
				}
			}
		}
	}

	private static String classifyDamage(DamageSource source) {
		if (source == null) return "NONE";
		if (isCBCProjectileOrDamage(source)) return "ARTILLERY";

		Entity direct = source.getDirectEntity();
		if (direct != null) {
			double velocity = direct.getDeltaMovement().length();
			String className = direct.getClass().getName().toLowerCase(java.util.Locale.ROOT);
			if (velocity > 1.5 || className.contains("bullet") || className.contains("round") || className.contains("shell") || className.contains("projectile") || className.contains("ammo")) {
				return "PROJECTILE";
			}
			if (className.contains("spell") || className.contains("magic") || className.contains("bolt") || className.contains("orb")) {
				return "MAGIC";
			}
			if (direct instanceof net.minecraft.world.entity.projectile.Projectile) {
				return "PROJECTILE";
			}
		}

		if (source.is(DamageTypes.ARROW) || source.is(DamageTypes.TRIDENT) || source.is(DamageTypes.THROWN)) return "PROJECTILE";
		if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) return "EXPLOSION";
		if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) return "MAGIC";
		if (source.is(DamageTypes.FIREBALL) || source.is(DamageTypes.UNATTRIBUTED_FIREBALL) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA)) return "FIRE";
		return source.getEntity() != null ? "MELEE" : "ENVIRONMENTAL";
	}

	private static boolean isCBCProjectileOrDamage(DamageSource source) {
		if (source == null) return false;
		Entity direct = source.getDirectEntity();
		if (direct != null) {
			String className = direct.getClass().getName().toLowerCase(java.util.Locale.ROOT);
			if (className.contains("cannon") || className.contains("cbc") || className.contains("bigcannon")
				|| className.contains("mortar") || className.contains("autocannon") || className.contains("grapeshot")
				|| className.contains("shrapnel") || className.contains("flak") || className.contains("artillery")
				|| className.contains("ballistic") || className.contains("submunition")) {
				return true;
			}
			ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType());
			String typeStr = typeId.toString().toLowerCase(java.util.Locale.ROOT);
			if (typeStr.contains("cannon") || typeStr.contains("mortar") || typeStr.contains("autocannon")
				|| typeStr.contains("grapeshot") || typeStr.contains("shrapnel") || typeStr.contains("flak")
				|| typeStr.contains("artillery") || typeStr.contains("bigcannons") || typeStr.contains("cbc")) {
				return true;
			}
		}
		String msgId = source.getMsgId().toLowerCase(java.util.Locale.ROOT);
		return msgId.contains("cannon") || msgId.contains("mortar") || msgId.contains("autocannon")
			|| msgId.contains("artillery") || msgId.contains("bigcannon") || msgId.contains("cbc")
			|| msgId.contains("shrapnel") || msgId.contains("grapeshot") || msgId.contains("concussive");
	}

	private static void triggerStageDialogue(Entity rotEntity, Entity target, String[] dialoguePool, String persistentTag) {
		if (!ENABLE_ROT_DIALOGUES || dialoguePool == null || dialoguePool.length == 0 || target == null)
			return;

		if (rotEntity.getPersistentData().getDouble(persistentTag) == 0.0) {
			rotEntity.getPersistentData().putDouble(persistentTag, 1.0);
			if (target instanceof Player _player && !_player.level().isClientSide()) {
				int index = (int) (Math.random() * dialoguePool.length);
				_player.displayClientMessage(Component.literal(dialoguePool[index]), true);
			}
		}
	}

	private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range) {
		return (Entity) world.getEntitiesOfClass(clazz, AABB.ofSize(new Vec3(x, y, z), range, range, range), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z))).findFirst().orElse(null);
	}

	private static boolean hasEntityInInventory(Entity entity, ItemStack itemstack) {
		if (entity instanceof Player player)
			return player.getInventory().contains(stack -> !stack.isEmpty() && ItemStack.isSameItem(stack, itemstack));
		return false;
	} // 1.21.1
}