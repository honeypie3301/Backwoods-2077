package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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
			float damageAmount = 0.0F;
			if (event instanceof LivingDamageEvent.Pre preEvent) {
				damageAmount = preEvent.getNewDamage();
			}

			// --- EXTRACTED FROM onIncomingDamage ---
			if (entity.getPersistentData().getDouble("sentinel_scanning_ticks") > 0) {
				Entity attackerEnt = event instanceof LivingDamageEvent.Pre pre ? pre.getSource().getEntity() : null;
				if (attackerEnt == null && event instanceof LivingDamageEvent.Pre pre) attackerEnt = pre.getSource().getDirectEntity();
				if (attackerEnt != null) {
					double dx = attackerEnt.getX() - entity.getX();
					double dz = attackerEnt.getZ() - entity.getZ();
					float targetYaw = (float) (Math.atan2(dz, dx) * (180F / Math.PI)) - 90F;
					entity.getPersistentData().putDouble("sentinel_scanning_base_yaw", targetYaw);
					entity.getPersistentData().putDouble("sentinel_scanning_ticks", 12.0);
					entity.getPersistentData().putDouble("sentinel_scan_max_ticks", 12.0);
					entity.getPersistentData().putBoolean("sentinel_reacting_to_attacker", true);
					
					double dy = attackerEnt.getEyeY() - entity.getEyeY();
					double dh = Math.sqrt(dx * dx + dz * dz);
					float targetPitch = (float) (-(Math.atan2(dy, dh) * (180F / Math.PI)));
					entity.getPersistentData().putDouble("sentinel_scan_target_pitch", targetPitch);
				}
			}

			if (event instanceof LivingDamageEvent.Pre preEvent) {
				DamageSource source = preEvent.getSource();
				Entity direct = source.getDirectEntity();

				// Track recent damage for high-DPS adaptive resistance
				double currentRecent = entity.getPersistentData().getDouble("sentinel_recent_damage");
				double newRecent = currentRecent + damageAmount;
				entity.getPersistentData().putDouble("sentinel_recent_damage", newRecent);

				// Track cumulative total damage taken for adaptation scaling
				double totalDamageTaken = entity.getPersistentData().getDouble("sentinel_total_damage_taken") + damageAmount;
				entity.getPersistentData().putDouble("sentinel_total_damage_taken", totalDamageTaken);

				// Slowly grow base knockback resistance as it keeps taking damage
				if (((LivingEntity)entity).getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)) {
					double baseKb = Math.min(1.0, totalDamageTaken * 0.003); // Grows up to 100% knockback resistance
					((LivingEntity)entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(baseKb);
				}

				if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
					entity.getPersistentData().putBoolean("taken_fire_damage", true);
				}
				if (source.is(net.minecraft.world.damagesource.DamageTypes.FREEZE)) {
					entity.getPersistentData().putBoolean("taken_freeze_damage", true);
				}

				if (direct instanceof net.minecraft.world.entity.projectile.Projectile || source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
					if (net.mcreator.thebackwoods.procedures.RotOnEntityTickUpdateProcedure.tryDodgeProjectile(entity, source)) {
						if (preEvent instanceof net.neoforged.bus.api.ICancellableEvent cancellable) cancellable.setCanceled(true);
						preEvent.setNewDamage(0.0F);
					}
				}

				// Swing arms when attacking any entity
				Entity attackerEnt = preEvent.getSource().getEntity();
				if (attackerEnt instanceof RotEntity rot) {
					rot.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
				}
			}
			// --- END EXTRACTED ---


			// 1. Choke state break mechanics (stagger from hits)
			if (entity.getPersistentData().getBoolean("is_armor_ripping")) {
				double hitsTaken = entity.getPersistentData().getDouble("rot_choke_hits_taken") + 1;
				entity.getPersistentData().putDouble("rot_choke_hits_taken", hitsTaken);
				double requiredHits = entity.getPersistentData().getDouble("rot_choke_break_hits");
				if (hitsTaken >= requiredHits) {
					entity.getPersistentData().putDouble("rot_armor_rip_ticks", 0);
					entity.getPersistentData().putBoolean("is_armor_ripping", false);
					if (world instanceof ServerLevel level) {
						level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
							net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.7F);
						level.sendParticles(ParticleTypes.DUST_PLUME, entity.getX(), entity.getY() + 1.0, entity.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
					}
				}
			}

			// 2. Blocking logic & immunity check
			if (entity.getPersistentData().getBoolean("is_blocking")) {
				if (event instanceof LivingDamageEvent.Pre preEvent) {
					DamageSource source = preEvent.getSource();
					boolean isBypassingBlock = false;
					if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
						|| source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
						|| source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)
						|| source.getMsgId().equals("magic")
						|| source.getMsgId().equals("indirectMagic")
						|| source.getMsgId().equals("potion")
						|| source.getMsgId().equals("void")
						|| source.getMsgId().equals("outOfWorld")) {
						isBypassingBlock = true;
					}

					if (!isBypassingBlock) {
						if (world instanceof Level lvl) {
							if (!lvl.isClientSide()) {
								lvl.playSound(null, BlockPos.containing(x, y, z),
									net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.85F);
							}
						}
						if (world instanceof ServerLevel level) {
							level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.2, entity.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
						}
						preEvent.setNewDamage(0.0F);
						return;
					}
				}
			} else {
				// Trigger block check
				double blockCooldown = entity.getPersistentData().getDouble("rot_block_cooldown");
				boolean isChannelingAbility = entity.getPersistentData().getDouble("sentinel_solar_charge_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_solar_fire_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_cryo_charge_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_cryo_fire_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_grapple_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_tk_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_sonic_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_laser_closing_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_sky_warp_slam_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_judgment_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_omni_sonic_charge_ticks") > 0
					|| entity.getPersistentData().getDouble("sentinel_sonic_scream_ticks") > 0
					|| entity.getPersistentData().getDouble("rot_armor_rip_ticks") > 0
					|| entity.getPersistentData().getDouble("rot_block_active_ticks") > 0;

				if (blockCooldown <= 0 && !isChannelingAbility) {
					double dmgThisSec = entity.getPersistentData().getDouble("rot_dmg_this_sec") + damageAmount;
					entity.getPersistentData().putDouble("rot_dmg_this_sec", dmgThisSec);

					double sec0 = entity.getPersistentData().getDouble("rot_dmg_sec_0");
					double sec1 = entity.getPersistentData().getDouble("rot_dmg_sec_1");
					double sec2 = entity.getPersistentData().getDouble("rot_dmg_sec_2");
					double sec3 = entity.getPersistentData().getDouble("rot_dmg_sec_3");
					double sec4 = entity.getPersistentData().getDouble("rot_dmg_sec_4");
					double totalDps = (sec0 + sec1 + sec2 + sec3 + sec4) / 5.0;

					if ((damageAmount >= 50.0F || totalDps >= 50.0F) && RotOnEntityTickUpdateProcedure.ENABLE_BLOCKING) {
						double blockFailCount = entity.getPersistentData().getDouble("rot_block_fail_count") + 1;
						entity.getPersistentData().putDouble("rot_block_fail_count", blockFailCount);
						double blockChance = 0.25 + (blockFailCount * 0.15);

						if (Math.random() < blockChance) {
							entity.getPersistentData().putDouble("rot_block_fail_count", 0);
							double minTicks = RotOnEntityTickUpdateProcedure.BLOCK_MIN_TICKS;
							double maxTicks = RotOnEntityTickUpdateProcedure.BLOCK_MAX_TICKS;
							double blockTicks = minTicks + Math.random() * (maxTicks - minTicks);
							entity.getPersistentData().putDouble("rot_block_active_ticks", blockTicks);
							entity.getPersistentData().putBoolean("is_blocking", true);
							entity.getPersistentData().putDouble("rot_block_cooldown", 300);

							if (world instanceof Level lvl) {
								if (!lvl.isClientSide()) {
									lvl.playSound(null, BlockPos.containing(x, y, z),
										net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.65F);
								}
							}
						}
					}
				}
			}

			attacker = (entity instanceof LivingEntity _entity) ? _entity.getLastHurtByMob() : null;
			foundPlayer = findEntityInWorldRange(world, Player.class, x, y, z, 64);
			if (attacker != null) {
				if (hasEntityInInventory(attacker, new ItemStack(Items.TOTEM_OF_UNDYING))) {
					if (Math.random() < 0.004) {
						if (attacker instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("I see the false life you clutch."), true);
					}
					if ((attacker.position()).distanceTo((entity.position())) > 1) {
						if (Math.random() < 0.067) {
							{
								Entity _ent = entity;
								_ent.teleportTo((attacker.getX() - attacker.getLookAngle().x * 2),
										(world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (attacker.getX() - attacker.getLookAngle().x * 2), (int) (attacker.getZ() - attacker.getLookAngle().z * 2))),
										(attacker.getZ() - attacker.getLookAngle().z * 2));
								if (_ent instanceof ServerPlayer _serverPlayer)
									_serverPlayer.connection.teleport((attacker.getX() - attacker.getLookAngle().x * 2),
											(world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (attacker.getX() - attacker.getLookAngle().x * 2), (int) (attacker.getZ() - attacker.getLookAngle().z * 2))),
											(attacker.getZ() - attacker.getLookAngle().z * 2), _ent.getYRot(), _ent.getXRot());
							}
							{
								final Vec3 _center = new Vec3(x, y, z);
								for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(16 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
										.toList()) {
									if (!(entityiterator instanceof RotEntity)) {
										entityiterator.hurt(new DamageSource(world.holderOrThrow(DamageTypes.SONIC_BOOM)), 10);
										entityiterator.setDeltaMovement(new Vec3((((entityiterator.getX() - entity.getX()) / (entityiterator.position()).distanceTo((entity.position()))) * 2), 0.6,
												(((entityiterator.getZ() - entity.getZ()) / (entityiterator.position()).distanceTo((entity.position()))) * 2)));
									}
								}
							}
							loop = 0;
							particleAmount = 100;
							masterRadius = 10;
							while (loop < particleAmount) {
								if (world instanceof ServerLevel _level)
									_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getX() + Math.cos((loop / particleAmount) * Math.PI * 2) * masterRadius), (entity.getY() + 2),
											(entity.getZ() + Math.sin((loop / particleAmount) * Math.PI * 2) * masterRadius), 1, 0, 0, 0, 0);
								loop = loop + 1;
							}
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.warden.sonic_boom")), SoundSource.HOSTILE, 2, (float) 0.6);
								} else {
									_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.warden.sonic_boom")), SoundSource.HOSTILE, 2, (float) 0.6, false);
								}
							}
						}
					}
				}
			}
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) <= 20) {
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:rot_roar")), SoundSource.HOSTILE, (float) 1.2, (float) 0.4);
					} else {
						_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:rot_roar")), SoundSource.HOSTILE, (float) 1.2, (float) 0.4, false);
					}
				}
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, (entity.getX()), (entity.getY() + 1.1), (entity.getZ()), 100, 0.1, 0.1, 0.1, 0);
				if (!entity.level().isClientSide())
					entity.discard();
				if (attacker instanceof ServerPlayer _player) {
					AdvancementHolder _adv = _player.server.getAdvancements().get(ResourceLocation.parse("the_backwoods:rot_vanish"));
					if (_adv != null) {
						AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
						if (!_ap.isDone()) {
							for (String criteria : _ap.getRemainingCriteria())
								_player.getAdvancements().award(_adv, criteria);
						}
					}
				}
			}
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) <= 250 && entity.getPersistentData().getDouble("msg1_fired") == 0) {
				if (foundPlayer != null) {
					entity.getPersistentData().putDouble("msg1_fired", 1);
					if (foundPlayer instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("I have felt deeper cuts than this."), true);
				}
			}
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) <= 100 && entity.getPersistentData().getDouble("msg2_fired") == 0) {
				if (foundPlayer != null) {
					entity.getPersistentData().putDouble("msg2_fired", 1);
					if (foundPlayer instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("I thin yet the hunger widens."), true);
				}
			}
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) <= 50 && entity.getPersistentData().getDouble("msg3_fired") == 0) {
				if (foundPlayer != null) {
					entity.getPersistentData().putDouble("msg3_fired", 1);
					if (foundPlayer instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("Pruning..."), true);
				}
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