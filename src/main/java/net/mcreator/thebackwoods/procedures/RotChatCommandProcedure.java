package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.mcreator.thebackwoods.entity.RotEntity;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.AbstractMap;

@EventBusSubscriber
public class RotChatCommandProcedure {
	public static boolean DEBUG_MODE = true;
	public static double OVERHEAD_TOTAL_TICKS = 56.0;
	public static double SONIC_BOOM_ANIMATION_TICKS = 90.0;
	public static final java.util.List<String> WHITELISTED_FRIENDS = new java.util.ArrayList<>(java.util.Arrays.asList("AEDADA"));

	public static boolean isWhitelistedFriend(String name) {
		if (name == null || name.isEmpty()) return false;
		for (String friend : WHITELISTED_FRIENDS) {
			if (friend.equalsIgnoreCase(name)) return true;
		}
		return false;
	}

	@SubscribeEvent
	public static void onChat(ServerChatEvent event) {
		if (event != null && event.getPlayer() != null) {
			execute(event, event.getPlayer().serverLevel(), event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ(), event.getPlayer(), event.getRawText());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, String text) {
		execute(null, world, x, y, z, entity, text);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, String text) {
		if (entity == null || text == null) return;
		ServerPlayer candidatePlayer = null;
		if (entity instanceof ServerPlayer sp) {
			candidatePlayer = sp;
		} else if (entity instanceof Player p && p instanceof ServerPlayer sp2) {
			candidatePlayer = sp2;
		}
		if (candidatePlayer == null) return;
		final ServerPlayer player = candidatePlayer;

		String msg = text.trim().toLowerCase();
		String name = player.getGameProfile().getName();

		if (DEBUG_MODE || name.equals("honeypie_3301") || name.equals("Dev")) {
			ServerLevel level = player.serverLevel();

			if (msg.startsWith("phase opacity ") || msg.startsWith("opacity ")) {
				String valStr = msg.replace("phase opacity ", "").replace("opacity ", "").trim();
				try {
					double val = Double.parseDouble(valStr);
					AABB box = player.getBoundingBox().inflate(64.0);
					List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, box);
					if (!rots.isEmpty()) {
						for (RotEntity rot : rots) {
							setRotPersistentDouble(rot, "rot_phase_opacity", val);
						}
						player.sendSystemMessage(Component.literal("§a[Rot] Set phase opacity to " + (int)(val * 100) + "% (" + val + ") for nearby Rot entities."), true);
					} else {
						player.sendSystemMessage(Component.literal("§c[Rot] No Rot entity found within 64m."), true);
					}
				} catch (NumberFormatException ignored) {}
				return;
			}

			else if (msg.equals("phase") || msg.equals("phasing") || msg.equals("togglephase") || msg.equals("testphase") || msg.equals("phase shift")) {
				AABB box = player.getBoundingBox().inflate(64.0);
				List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, box);
				if (!rots.isEmpty()) {
					for (RotEntity rot : rots) {
						boolean isPhasing = getRotPersistentBoolean(rot, "rot_phase_shifting", false);
						double pTicks = getRotPersistentDouble(rot, "rot_phase_ticks", 0.0);
						if (isPhasing || pTicks > 0) {
							rot.getPersistentData().putBoolean("rot_phase_shifting", false);
							setRotPersistentDouble(rot, "rot_phase_ticks", 0.0);
							setRotPersistentDouble(rot, "rot_phase_cooldown", 72000.0);
							rot.noPhysics = false;
							player.sendSystemMessage(Component.literal("§e[Rot] Phase Shift TOGGLED OFF for Rot entity."), true);
						} else {
							rot.getPersistentData().putBoolean("rot_phase_shifting", true);
							setRotPersistentDouble(rot, "rot_phase_ticks", 12000.0);
							setRotPersistentDouble(rot, "rot_phase_cooldown", 0.0);
							setRotPersistentDouble(rot, "rot_phase_mastery", 1.0);
							rot.noPhysics = false;
							level.sendParticles(ParticleTypes.PORTAL, rot.getX(), rot.getY() + 1.0, rot.getZ(), 25, 0.3, 0.5, 0.3, 0.05);
							playHostileSound(level, rot.getX(), rot.getY(), rot.getZ(), "entity.evoker.cast_spell", 1.5F, 0.5F);
							player.sendSystemMessage(Component.literal("§a[Rot] Phase Shift TOGGLED ON (forced 10m) for Rot entity."), true);
						}
					}
				} else {
					player.sendSystemMessage(Component.literal("§c[Rot] No Rot entity found within 64m."), true);
				}
				return;
			}

			else if (msg.equals("stop guard")) {
				clearGuardCommand(level, player);
				player.sendSystemMessage(Component.literal("§e[Rot] Guard mode disabled."), true);
				return;
			} else if (msg.equals("guard me") || msg.startsWith("guard ")) {
				Player guardTarget = msg.equals("guard me") ? player : findOnlinePlayer(player.getServer(), msg.substring(6).trim());
				if (guardTarget != null) {
					setGuardTarget(level, player, guardTarget);
					player.sendSystemMessage(Component.literal("§a[Rot] Guarding " + guardTarget.getGameProfile().getName() + "."), true);
				} else {
					player.sendSystemMessage(Component.literal("§c[Rot] Player not found."), true);
				}
				return;
			}

			else if (msg.equals("follow")) {
				setFollowTarget(level, player, player);
				player.sendSystemMessage(Component.literal("§a[Rot] Following you."), true);
				return;
			} else if (msg.equals("stay") || msg.equals("stop follow")) {
				stopFollowTarget(level, player);
				player.sendSystemMessage(Component.literal("§e[Rot] Staying."), true);
				return;
			}

			else if (msg.equals("duel")) {
				setDuelTarget(level, player);
				player.sendSystemMessage(Component.literal("§c[Rot] Duel mode enabled."), true);
				return;
			} else if (msg.equals("stop duel")) {
				stopDuelTarget(level, player);
				player.sendSystemMessage(Component.literal("§e[Rot] Duel mode disabled."), true);
				return;
			}

			else if (msg.equals("kill")) {
				Entity crosshairTarget = getPlayerFOVTarget(player, 24.0);
				if (crosshairTarget instanceof LivingEntity le && le.isAlive() && !(le instanceof RotEntity)) {
					int targetId = le.getId();
					AABB box = player.getBoundingBox().inflate(64.0);
					List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, box);
					for (RotEntity rot : rots) {
						rot.getPersistentData().remove("master_target_queue");
						rot.getPersistentData().putInt("master_kill_target_id", targetId);
						if (rot instanceof Mob mob) {
							mob.setTarget(le);
						}
					}
					player.sendSystemMessage(Component.literal("§a[Rot] Crosshair target acquired: §e" + le.getDisplayName().getString()), true);
				} else {
					List<LivingEntity> fovTargets = getEntitiesInPlayerFOV(player, 64.0);
					fovTargets.removeIf(e -> e instanceof RotEntity || !e.isAlive());
					if (!fovTargets.isEmpty()) {
						fovTargets.sort(Comparator.comparingDouble(e -> player.distanceToSqr(e)));
						StringBuilder sb = new StringBuilder();
						for (int i = 0; i < fovTargets.size(); i++) {
							if (i > 0) sb.append(",");
							sb.append(fovTargets.get(i).getId());
						}
						String queueStr = sb.toString();
						int firstId = fovTargets.get(0).getId();
						AABB box = player.getBoundingBox().inflate(64.0);
						List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, box);
						for (RotEntity rot : rots) {
							rot.getPersistentData().putString("master_target_queue", queueStr);
							rot.getPersistentData().putInt("master_kill_target_id", firstId);
							if (rot instanceof Mob mob) {
								mob.setTarget(fovTargets.get(0));
							}
						}
						player.sendSystemMessage(Component.literal("§a[Rot] Target queue acquired (" + fovTargets.size() + "), closest first: §e" + formatTargetNames(fovTargets)), true);
					} else {
						AABB searchBox = player.getBoundingBox().inflate(64.0);
						List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> {
							if (e == player || !e.isAlive() || e instanceof RotEntity) return false;
							if (e instanceof Player p) {
								String pName = p.getGameProfile().getName();
								if (pName.equals("honeypie_3301") || pName.equals("Dev")) return false;
							}
							return true;
						});
						LivingEntity closest = null;
						double minSq = Double.MAX_VALUE;
						for (LivingEntity e : candidates) {
							double dSq = player.distanceToSqr(e);
							if (dSq < minSq) {
								minSq = dSq;
								closest = e;
							}
						}
						if (closest != null) {
							int targetId = closest.getId();
							List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, searchBox);
							for (RotEntity rot : rots) {
								rot.getPersistentData().remove("master_target_queue");
								rot.getPersistentData().putInt("master_kill_target_id", targetId);
								if (rot instanceof Mob mob) {
									mob.setTarget(closest);
								}
							}
							player.sendSystemMessage(Component.literal("§a[Rot] Closest target acquired: §e" + closest.getDisplayName().getString()), true);
						} else {
							player.sendSystemMessage(Component.literal("§c[Rot] No targets found."), true);
						}
					}
				}
				return;
			} else if (msg.equals("kill everyone") || msg.equals("kill all")) {
				AABB box = player.getBoundingBox().inflate(64.0);
				List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, box);
				List<Player> players = level.getEntitiesOfClass(Player.class, box);
				StringBuilder sb = new StringBuilder();
				List<Player> targetPlayers = new ArrayList<>();
				for (Player p : players) {
					String pName = p.getGameProfile().getName();
					if (!pName.equals("Dev") && !pName.equals("honeypie_3301") && !isWhitelistedFriend(pName) && p.isAlive()) {
						targetPlayers.add(p);
						if (sb.length() > 0) sb.append(",");
						sb.append(p.getId());
					}
				}
				if (!targetPlayers.isEmpty()) {
					String queueStr = sb.toString();
					int firstId = targetPlayers.get(0).getId();
					for (RotEntity rot : rots) {
						rot.getPersistentData().putString("master_target_queue", queueStr);
						rot.getPersistentData().putInt("master_kill_target_id", firstId);
						if (rot instanceof Mob mob) {
							mob.setTarget(targetPlayers.get(0));
						}
					}
					player.sendSystemMessage(Component.literal("§a[Rot] Target queue acquired for all nearby players (" + targetPlayers.size() + "): §e" + formatTargetNames(targetPlayers)), true);
				} else {
					player.sendSystemMessage(Component.literal("§c[Rot] No other players found in radius."), true);
				}
				return;
			} else if (msg.equals("help")) {
				AABB searchBox = player.getBoundingBox().inflate(64.0);
				List<LivingEntity> attackers = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> {
					if (e == player || !e.isAlive() || e instanceof RotEntity) return false;
					if (e instanceof Player p) {
						String pName = p.getGameProfile().getName();
						if (pName.equals("honeypie_3301") || pName.equals("Dev") || isWhitelistedFriend(pName)) return false;
					}
					if (e instanceof Mob mob && mob.getTarget() == player) return true;
					if (player.getLastHurtByMob() == e || player.getLastHurtMob() == e) return true;
					if (e.getLastHurtByMob() == player || e.getLastHurtMob() == player) return true;
					return false;
				});
				if (!attackers.isEmpty()) {
					attackers.sort(Comparator.comparingDouble(e -> player.distanceToSqr(e)));
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < attackers.size(); i++) {
						if (i > 0) sb.append(",");
						sb.append(attackers.get(i).getId());
					}
					String queueStr = sb.toString();
					int firstId = attackers.get(0).getId();
					List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, searchBox);
					for (RotEntity rot : rots) {
						rot.getPersistentData().putString("master_target_queue", queueStr);
						rot.getPersistentData().putInt("master_kill_target_id", firstId);
						if (rot instanceof Mob mob) {
							mob.setTarget(attackers.get(0));
						}
					}
					player.sendSystemMessage(Component.literal("§a[Rot] Defending master from (" + attackers.size() + ") threat(s): §e" + formatTargetNames(attackers)), true);
				} else {
					player.sendSystemMessage(Component.literal("§a[Rot] No hostile threats currently attacking you."), true);
				}
				return;
			} else if (msg.startsWith("kill ")) {
				String targetQuery = msg.substring(5).trim();
				if (targetQuery.isEmpty()) return;

				ServerPlayer targetPlayer = null;
				MinecraftServer server = player.getServer();
				if (server != null) {
					for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
						if (sp.getGameProfile().getName().equalsIgnoreCase(targetQuery)) {
							targetPlayer = sp;
							break;
						}
					}
				}

				AABB box = player.getBoundingBox().inflate(128.0);
				List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, box);

				if (targetPlayer != null && targetPlayer.isAlive()) {
					boolean anyTeleportUnlocked = false;
					for (RotEntity rot : rots) {
						boolean unlockedTp = getRotPersistentBoolean(rot, "unlocked_teleportation", false);
						if (unlockedTp) {
							anyTeleportUnlocked = true;
							rot.getPersistentData().remove("master_target_queue");
							rot.getPersistentData().remove("master_queued_assassination_player");
							rot.getPersistentData().putInt("master_kill_target_id", targetPlayer.getId());
							rot.getPersistentData().putString("master_assassinate_player_name", targetPlayer.getGameProfile().getName());
							if (rot instanceof Mob mob) {
								mob.setTarget(targetPlayer);
							}
						} else {
							rot.getPersistentData().putString("master_queued_assassination_player", targetPlayer.getGameProfile().getName());
						}
					}
					if (anyTeleportUnlocked) {
						player.sendSystemMessage(Component.literal("§a[Rot] Assassination target '" + targetPlayer.getGameProfile().getName() + "' acquired globally. Rot dispatched."), true);
					} else {
						player.sendSystemMessage(Component.literal("§e[Rot] Assassination target '" + targetPlayer.getGameProfile().getName() + "' queued. Rot will execute upon learning teleportation."), true);
					}
					return;
				}

				List<LivingEntity> matchingEntities = level.getEntitiesOfClass(LivingEntity.class, box, e -> {
					if (e == player || !e.isAlive() || e instanceof RotEntity) return false;
					if (e.isBaby()) return false;
					String customName = e.hasCustomName() ? e.getCustomName().getString().toLowerCase() : "";
					String displayName = e.getDisplayName().getString().toLowerCase();
					String entityTypeStr = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().toLowerCase();
					String queryLower = targetQuery.toLowerCase();
					return customName.contains(queryLower) || displayName.contains(queryLower) || entityTypeStr.contains(queryLower);
				});

				if (!matchingEntities.isEmpty()) {
					matchingEntities.sort(Comparator.comparingDouble(e -> player.distanceToSqr(e)));
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < matchingEntities.size(); i++) {
						if (i > 0) sb.append(",");
						sb.append(matchingEntities.get(i).getId());
					}
					String queueStr = sb.toString();
					int firstId = matchingEntities.get(0).getId();
					for (RotEntity rot : rots) {
						rot.getPersistentData().putString("master_target_queue", queueStr);
						rot.getPersistentData().putInt("master_kill_target_id", firstId);
						if (rot instanceof Mob mob) {
							mob.setTarget(matchingEntities.get(0));
						}
					}
					player.sendSystemMessage(Component.literal("§a[Rot] Entity target(s) matching '" + targetQuery + "' acquired (" + matchingEntities.size() + " in render distance)."), true);
				} else {
					player.sendSystemMessage(Component.literal("§c[Rot] Target '" + targetQuery + "' not found (no player online or matching entity in render distance)."), true);
				}
				return;
			} else if (msg.equals("stop")) {
				AABB box = player.getBoundingBox().inflate(256.0);
				List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, box);
				for (RotEntity rot : rots) {
					rot.getPersistentData().remove("master_kill_target_id");
					rot.getPersistentData().remove("master_target_queue");
					rot.getPersistentData().remove("master_assassinate_player_name");
					rot.getPersistentData().remove("master_queued_assassination_player");
					rot.getPersistentData().remove("master_guard_target_uuid");
					rot.getPersistentData().remove("master_guard_target_name");
					rot.getPersistentData().remove("master_follow_target_uuid");
					rot.getPersistentData().putBoolean("master_guard_mode", false);
					rot.getPersistentData().putBoolean("master_follow_enabled", false);
					rot.getPersistentData().putBoolean("is_dueling", false);
					rot.getPersistentData().putDouble("sentinel_combat_ticks", 0.0);
					rot.getPersistentData().putDouble("rot_overhead_ticks", 0.0);
					rot.getPersistentData().remove("overhead_target_uuid");

					// Clear all combo stages and ticks
					rot.getPersistentData().remove("sentinel_cc1_stage");
					rot.getPersistentData().remove("sentinel_cc1_ticks");
					rot.getPersistentData().remove("sentinel_cc2_stage");
					rot.getPersistentData().remove("sentinel_cc2_ticks");
					rot.getPersistentData().remove("sentinel_cc2_air_ticks");
					rot.getPersistentData().remove("sentinel_cc3_stage");
					rot.getPersistentData().remove("sentinel_cc3_ticks");
					rot.getPersistentData().remove("sentinel_cc4_stage");
					rot.getPersistentData().remove("sentinel_cc4_ticks");
					rot.getPersistentData().remove("sentinel_cc5_stage");
					rot.getPersistentData().remove("sentinel_cc5_ticks");

					// Clear Minos stage and ticks
					rot.getPersistentData().remove("sentinel_minos_stage");
					rot.getPersistentData().remove("sentinel_minos_ticks");

					// Clear individual abilities
					rot.getPersistentData().remove("sentinel_judgment_ticks");
					rot.getPersistentData().remove("sentinel_slam_phase");
					rot.getPersistentData().remove("sentinel_die_kick_phase");
					rot.getPersistentData().remove("sentinel_die_kick_ticks");
					rot.getPersistentData().remove("sentinel_solar_fire_ticks");
					rot.getPersistentData().remove("sentinel_solar_charge_ticks");
					rot.getPersistentData().remove("sentinel_cryo_fire_ticks");
					rot.getPersistentData().remove("sentinel_cryo_charge_ticks");
					rot.getPersistentData().remove("sentinel_tk_ticks");
					rot.getPersistentData().remove("sentinel_omni_sonic_charge_ticks");
					rot.getPersistentData().remove("sentinel_sonic_ticks");
					rot.getPersistentData().remove("sentinel_sonic_scream_ticks");
					rot.getPersistentData().remove("rot_armor_rip_ticks");

					// Clear client-facing sync data
					rot.getPersistentData().putBoolean("is_blocking", false);
					rot.getPersistentData().putBoolean("is_blocking_finish", false);
					rot.getPersistentData().putBoolean("is_uppercutting", false);
					rot.getPersistentData().putBoolean("is_falling_heavy", false);
					rot.getPersistentData().putBoolean("is_sonic_boom", false);
					rot.getPersistentData().putBoolean("is_sonic_boom_large", false);
					rot.getPersistentData().putBoolean("is_overhead_preparing", false);
					rot.getPersistentData().putBoolean("is_overhead", false);
					rot.getPersistentData().putBoolean("is_slam_charge", false);
					rot.getPersistentData().putBoolean("is_ground_crushing", false);
					rot.getPersistentData().putBoolean("is_rider_charging", false);
					rot.getPersistentData().putBoolean("is_rider_kick", false);
					rot.getPersistentData().putBoolean("is_armor_ripping", false);

					// Clear threat levels, global cooldowns, defensive CD, etc.
					rot.getPersistentData().remove("sentinel_predicted_threat_level");
					rot.getPersistentData().remove("sentinel_global_ability_cooldown");
					rot.getPersistentData().remove("sentinel_recent_damage");
					rot.getPersistentData().remove("sentinel_recent_damage_timestamp");
					rot.getPersistentData().remove("sentinel_evasive_spacing_cd");

					if (rot instanceof Mob mob) {
						mob.setTarget(null);
						mob.setLastHurtByMob(null);
						mob.setLastHurtMob(null);
						mob.getNavigation().stop();
					}
				}
				player.sendSystemMessage(Component.literal("§e[Rot] Stopped all targeting and active modes."), true);
				return;
			}

			else if (msg.equals("why")) {
				AABB box = player.getBoundingBox().inflate(64.0);
				List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, box);
				if (!rots.isEmpty()) {
					RotEntity rot = rots.get(0);
					double minDistance = player.distanceTo(rot);
					for (RotEntity candidate : rots) {
						double d = player.distanceTo(candidate);
						if (d < minDistance) {
							minDistance = d;
							rot = candidate;
						}
					}
					List<RotOnEntityTickUpdateProcedure.AbilityInfo> available = RotOnEntityTickUpdateProcedure.getAvailableAbilities(rot);
					List<String> availIds = new ArrayList<>();
					for (RotOnEntityTickUpdateProcedure.AbilityInfo a : available) availIds.add(a.id);
					String strat = getRotPersistentString(rot, "sentinel_current_strategy", "BALANCED");
					String plan = getRotPersistentString(rot, "sentinel_tactical_plan", "BALANCED_PRESSURE");
					double globalCd = getRotPersistentDouble(rot, "sentinel_global_ability_cooldown", 0.0);
					double meleeCd = getRotPersistentDouble(rot, "sentinel_melee_cooldown", 0.0);
					StringBuilder scoresSb = new StringBuilder();
					Entity target = rot.getTarget();
					if (target != null) {
						RotOnEntityTickUpdateProcedure.CombatContext ctx = RotOnEntityTickUpdateProcedure.getCombatContext(rot, target);
						List<Map.Entry<String, Double>> scored = new ArrayList<>();
						for (RotOnEntityTickUpdateProcedure.AbilityInfo ab : available) {
							double s = RotOnEntityTickUpdateProcedure.scoreAbility(ab, ctx, rot, target);
							scored.add(new AbstractMap.SimpleEntry<>(ab.id, s));
						}
						scored.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
						for (int i = 0; i < Math.min(3, scored.size()); i++) {
							scoresSb.append(scored.get(i).getKey()).append(": ").append(String.format("%.1f", scored.get(i).getValue())).append("; ");
						}
					} else {
						scoresSb.append("No active target");
					}
					String whyText = String.format("§6[Rot AI Debug] §eAvailable: %s\n§eStrat: %s | Plan: %s\n§eTop Candidates: %s\n§eGlobalCD: %.1fs | MeleeCD: %.1fs",
						availIds.isEmpty() ? "None" : String.join(", ", availIds),
						strat, plan, scoresSb.toString(), globalCd / 20.0, meleeCd / 20.0
					);
					player.sendSystemMessage(Component.literal(whyText), false);
				} else {
					player.sendSystemMessage(Component.literal("§c[Rot AI Debug] No Rot entity within 64m."), false);
				}
				return;
			}
		}

		if (!DEBUG_MODE) return;

		if (msg.equals("slam") || msg.equals("overhead") || msg.equals("dropkick") || msg.equals("kick") || msg.equals("die") || msg.equals("rider") || msg.equals("kickrider") || msg.equals("triple") || msg.equals("highsky") || msg.equals("uppercut") || msg.equals("uppercutleft") || msg.equals("uppercutright") || msg.equals("punchkick") || msg.equals("punchrider") || msg.equals("repentanceplus") || msg.equals("testoverhead") || msg.equals("testrider") || msg.equals("resetanim") || msg.equals("sonic") || msg.equals("sonicboom") || msg.equals("testsonic") || msg.equals("fall") || msg.equals("falling") || msg.equals("testfall") || msg.equals("testheavyleft") || msg.equals("testheavyright") || msg.equals("testheavy") || msg.equals("testuppercutleft") || msg.equals("testuppercutright") || msg.equals("testuppercut")) {
			ServerLevel level = player.serverLevel();
			AABB box = player.getBoundingBox().inflate(64.0);
			List<RotEntity> rots = level.getEntitiesOfClass(RotEntity.class, box);
			for (RotEntity rot : rots) {
				if (msg.equals("slam")) {
					rot.getPersistentData().putDouble("sentinel_slam_phase", 1);
					rot.getPersistentData().putDouble("sentinel_slam_ticks", 22);
					rot.getPersistentData().putDouble("rot_overhead_ticks", 0);
					rot.setDeltaMovement(0.0, 1.9, 0.0);
					rot.hasImpulse = true;
					playHostileSound(level, rot.getX(), rot.getY(), rot.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
					level.sendParticles(ParticleTypes.CLOUD, rot.getX(), rot.getY() + 0.5, rot.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
				} else if (msg.equals("overhead")) {
					rot.getPersistentData().putDouble("rot_overhead_ticks", OVERHEAD_TOTAL_TICKS);
					rot.getPersistentData().putDouble("sentinel_slam_phase", 0);
					rot.getPersistentData().putString("overhead_target_uuid", player.getUUID().toString());
				} else if (msg.equals("dropkick") || msg.equals("kick")) {
					rot.getPersistentData().putDouble("sentinel_judgment_ticks", 60);
					rot.getPersistentData().putDouble("sentinel_slam_phase", 0);
					rot.getPersistentData().putDouble("rot_overhead_ticks", 0);
					rot.getPersistentData().putDouble("sentinel_die_kick_phase", 0);
					rot.setDeltaMovement(0.0, 0.0, 0.0);
					rot.hasImpulse = true;
					playHostileSound(level, rot.getX(), rot.getY(), rot.getZ(), "entity.warden.sonic_charge", 1.5F, 0.5F);
				} else if (msg.equals("die") || msg.equals("rider") || msg.equals("kickrider")) {
					rot.getPersistentData().putDouble("sentinel_die_kick_phase", 1);
					rot.getPersistentData().putDouble("sentinel_die_kick_ticks", 22);
					rot.getPersistentData().putDouble("sentinel_slam_phase", 0);
					rot.getPersistentData().putDouble("rot_overhead_ticks", 0);
					rot.getPersistentData().putDouble("sentinel_judgment_ticks", 0);
					rot.setDeltaMovement(0.0, 1.95, 0.0);
					rot.hasImpulse = true;
					playHostileSound(level, rot.getX(), rot.getY(), rot.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
					level.sendParticles(ParticleTypes.CLOUD, rot.getX(), rot.getY() + 0.5, rot.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
				} else if (msg.equals("triple")) {
					rot.getPersistentData().putBoolean("unlocked_triple_threat_combo", true);
					rot.getPersistentData().putDouble("sentinel_cc1_stage", 1);
					rot.getPersistentData().putDouble("sentinel_cc1_ticks", 5);
				} else if (msg.equals("highsky")) {
					rot.getPersistentData().putBoolean("unlocked_high_sky_slam_combo", true);
					rot.getPersistentData().putBoolean("is_uppercut_standalone", false);
					rot.getPersistentData().putDouble("sentinel_cc2_stage", 1);
				} else if (msg.equals("uppercut")) {
					rot.getPersistentData().putBoolean("unlocked_high_sky_slam_combo", true);
					rot.getPersistentData().putBoolean("is_uppercut_standalone", true);
					rot.getPersistentData().putBoolean("is_uppercutting", true);
					boolean isLeft = Math.random() < 0.5;
					rot.getPersistentData().putBoolean("is_uppercutting_left", isLeft);
					rot.getPersistentData().putBoolean("is_uppercutting_right", !isLeft);
					rot.getPersistentData().putDouble("sentinel_cc2_stage", 1);
					rot.getPersistentData().putDouble("sentinel_cc2_ticks", 0);
					try { rot.getEntityData().set(RotEntity.DATA_is_uppercutting, true); } catch (Exception e) {}
					player.sendSystemMessage(Component.literal("§a[AI Debug] Forcing Standalone Uppercut (" + (isLeft ? "Left" : "Right") + ")."));
				} else if (msg.equals("uppercutleft")) {
					rot.getPersistentData().putBoolean("unlocked_high_sky_slam_combo", true);
					rot.getPersistentData().putBoolean("is_uppercut_standalone", true);
					rot.getPersistentData().putBoolean("is_uppercutting", true);
					rot.getPersistentData().putBoolean("is_uppercutting_left", true);
					rot.getPersistentData().putBoolean("is_uppercutting_right", false);
					rot.getPersistentData().putDouble("sentinel_cc2_stage", 1);
					rot.getPersistentData().putDouble("sentinel_cc2_ticks", 0);
					try { rot.getEntityData().set(RotEntity.DATA_is_uppercutting, true); } catch (Exception e) {}
					player.sendSystemMessage(Component.literal("§a[AI Debug] Forcing Standalone Left Uppercut."));
				} else if (msg.equals("uppercutright")) {
					rot.getPersistentData().putBoolean("unlocked_high_sky_slam_combo", true);
					rot.getPersistentData().putBoolean("is_uppercut_standalone", true);
					rot.getPersistentData().putBoolean("is_uppercutting", true);
					rot.getPersistentData().putBoolean("is_uppercutting_left", false);
					rot.getPersistentData().putBoolean("is_uppercutting_right", true);
					rot.getPersistentData().putDouble("sentinel_cc2_stage", 1);
					rot.getPersistentData().putDouble("sentinel_cc2_ticks", 0);
					try { rot.getEntityData().set(RotEntity.DATA_is_uppercutting, true); } catch (Exception e) {}
					player.sendSystemMessage(Component.literal("§a[AI Debug] Forcing Standalone Right Uppercut."));
				} else if (msg.equals("punchkick")) {
					rot.getPersistentData().putBoolean("unlocked_knockback_dropkick_combo", true);
					rot.getPersistentData().putDouble("sentinel_cc3_stage", 1);
				} else if (msg.equals("punchrider")) {
					rot.getPersistentData().putBoolean("unlocked_knockback_rider_combo", true);
					rot.getPersistentData().putDouble("sentinel_cc4_stage", 1);
				} else if (msg.equals("repentanceplus")) {
					rot.getPersistentData().putBoolean("unlocked_heavenly_repentance_plus", true);
					rot.getPersistentData().putDouble("sentinel_cc5_stage", 1);
				} else if (msg.equals("testoverhead")) {
					boolean current = getRotPersistentBoolean(rot, "debug_force_overhead", false);
					rot.getPersistentData().putBoolean("debug_force_overhead", !current);
					rot.getPersistentData().putBoolean("debug_force_rider", false);
					player.sendSystemMessage(Component.literal("§e[AI Debug] Loop Overhead Animation: " + (!current ? "§a§lON" : "§c§lOFF")));
				} else if (msg.equals("testrider")) {
					boolean current = getRotPersistentBoolean(rot, "debug_force_rider", false);
					rot.getPersistentData().putBoolean("debug_force_rider", !current);
					rot.getPersistentData().putBoolean("debug_force_overhead", false);
					player.sendSystemMessage(Component.literal("§e[AI Debug] Loop Rider Kick Animation: " + (!current ? "§a§lON" : "§c§lOFF")));
				} else if (msg.equals("sonic") || msg.equals("sonicboom")) {
					rot.getPersistentData().putDouble("sentinel_sonic_ticks", SONIC_BOOM_ANIMATION_TICKS);
					rot.getPersistentData().putBoolean("unlocked_sonic_boom", true);
					playHostileSound(level, rot.getX(), rot.getY(), rot.getZ(), "entity.warden.sonic_charge", 1.4F, 0.4F);
					player.sendSystemMessage(Component.literal("§a[AI Debug] Forcing Sonic Boom attack."));
				} else if (msg.equals("testsonic")) {
					boolean current = getRotPersistentBoolean(rot, "debug_force_sonic", false);
					rot.getPersistentData().putBoolean("debug_force_sonic", !current);
					rot.getPersistentData().putBoolean("debug_force_overhead", false);
					rot.getPersistentData().putBoolean("debug_force_rider", false);
					rot.getPersistentData().putBoolean("debug_force_fall", false);
					player.sendSystemMessage(Component.literal("§e[AI Debug] Loop Sonic Boom Animation: " + (!current ? "§a§lON" : "§c§lOFF")));
				} else if (msg.equals("fall") || msg.equals("falling")) {
					rot.getPersistentData().putBoolean("is_falling_heavy", true);
					try {
						rot.getEntityData().set(RotEntity.DATA_is_falling_heavy, true);
					} catch (Exception e) {}
					player.sendSystemMessage(Component.literal("§a[AI Debug] Forcing Heavy Fall state."));
				} else if (msg.equals("testfall")) {
					boolean current = getRotPersistentBoolean(rot, "debug_force_fall", false);
					rot.getPersistentData().putBoolean("debug_force_fall", !current);
					rot.getPersistentData().putBoolean("debug_force_overhead", false);
					rot.getPersistentData().putBoolean("debug_force_rider", false);
					rot.getPersistentData().putBoolean("debug_force_sonic", false);
					player.sendSystemMessage(Component.literal("§e[AI Debug] Loop Fall Animation: " + (!current ? "§a§lON" : "§c§lOFF")));
				} else if (msg.equals("testheavyleft")) {
					boolean current = getRotPersistentBoolean(rot, "debug_force_heavy_left", false);
					rot.getPersistentData().putBoolean("debug_force_heavy_left", !current);
					rot.getPersistentData().putBoolean("debug_force_heavy_right", false);
					rot.getPersistentData().putBoolean("debug_force_overhead", false);
					rot.getPersistentData().putBoolean("debug_force_rider", false);
					rot.getPersistentData().putBoolean("debug_force_sonic", false);
					rot.getPersistentData().putBoolean("debug_force_fall", false);
					player.sendSystemMessage(Component.literal("§e[AI Debug] Loop Heavy Left Punch Animation: " + (!current ? "§a§lON" : "§c§lOFF")));
				} else if (msg.equals("testheavyright")) {
					boolean current = getRotPersistentBoolean(rot, "debug_force_heavy_right", false);
					rot.getPersistentData().putBoolean("debug_force_heavy_right", !current);
					rot.getPersistentData().putBoolean("debug_force_heavy_left", false);
					rot.getPersistentData().putBoolean("debug_force_overhead", false);
					rot.getPersistentData().putBoolean("debug_force_rider", false);
					rot.getPersistentData().putBoolean("debug_force_sonic", false);
					rot.getPersistentData().putBoolean("debug_force_fall", false);
					player.sendSystemMessage(Component.literal("§e[AI Debug] Loop Heavy Right Punch Animation: " + (!current ? "§a§lON" : "§c§lOFF")));
				} else if (msg.equals("testheavy")) {
					boolean current = getRotPersistentBoolean(rot, "debug_force_heavy_left", false) || getRotPersistentBoolean(rot, "debug_force_heavy_right", false);
					rot.getPersistentData().putBoolean("debug_force_heavy_left", !current);
					rot.getPersistentData().putBoolean("debug_force_heavy_right", !current);
					rot.getPersistentData().putBoolean("debug_force_overhead", false);
					rot.getPersistentData().putBoolean("debug_force_rider", false);
					rot.getPersistentData().putBoolean("debug_force_sonic", false);
					rot.getPersistentData().putBoolean("debug_force_fall", false);
					player.sendSystemMessage(Component.literal("§e[AI Debug] Loop Both Heavy Punch Animations: " + (!current ? "§a§lON" : "§c§lOFF")));
				} else if (msg.equals("testuppercutleft")) {
					boolean current = getRotPersistentBoolean(rot, "debug_force_uppercut_left", false);
					rot.getPersistentData().putBoolean("debug_force_uppercut_left", !current);
					rot.getPersistentData().putBoolean("debug_force_uppercut_right", false);
					rot.getPersistentData().putBoolean("debug_force_heavy_left", false);
					rot.getPersistentData().putBoolean("debug_force_heavy_right", false);
					rot.getPersistentData().putBoolean("debug_force_overhead", false);
					rot.getPersistentData().putBoolean("debug_force_rider", false);
					rot.getPersistentData().putBoolean("debug_force_sonic", false);
					rot.getPersistentData().putBoolean("debug_force_fall", false);
					player.sendSystemMessage(Component.literal("§e[AI Debug] Loop Uppercut Left Animation: " + (!current ? "§a§lON" : "§c§lOFF")));
				} else if (msg.equals("testuppercutright")) {
					boolean current = getRotPersistentBoolean(rot, "debug_force_uppercut_right", false);
					rot.getPersistentData().putBoolean("debug_force_uppercut_right", !current);
					rot.getPersistentData().putBoolean("debug_force_uppercut_left", false);
					rot.getPersistentData().putBoolean("debug_force_heavy_left", false);
					rot.getPersistentData().putBoolean("debug_force_heavy_right", false);
					rot.getPersistentData().putBoolean("debug_force_overhead", false);
					rot.getPersistentData().putBoolean("debug_force_rider", false);
					rot.getPersistentData().putBoolean("debug_force_sonic", false);
					rot.getPersistentData().putBoolean("debug_force_fall", false);
					player.sendSystemMessage(Component.literal("§e[AI Debug] Loop Uppercut Right Animation: " + (!current ? "§a§lON" : "§c§lOFF")));
				} else if (msg.equals("testuppercut")) {
					boolean current = getRotPersistentBoolean(rot, "debug_force_uppercut_left", false) || getRotPersistentBoolean(rot, "debug_force_uppercut_right", false);
					rot.getPersistentData().putBoolean("debug_force_uppercut_left", !current);
					rot.getPersistentData().putBoolean("debug_force_uppercut_right", !current);
					rot.getPersistentData().putBoolean("debug_force_heavy_left", false);
					rot.getPersistentData().putBoolean("debug_force_heavy_right", false);
					rot.getPersistentData().putBoolean("debug_force_overhead", false);
					rot.getPersistentData().putBoolean("debug_force_rider", false);
					rot.getPersistentData().putBoolean("debug_force_sonic", false);
					rot.getPersistentData().putBoolean("debug_force_fall", false);
					player.sendSystemMessage(Component.literal("§e[AI Debug] Loop Both Uppercut Animations: " + (!current ? "§a§lON" : "§c§lOFF")));
				} else if (msg.equals("resetanim")) {
					rot.getPersistentData().putBoolean("debug_force_overhead", false);
					rot.getPersistentData().putBoolean("debug_force_rider", false);
					rot.getPersistentData().putBoolean("debug_force_sonic", false);
					rot.getPersistentData().putBoolean("debug_force_fall", false);
					rot.getPersistentData().putBoolean("debug_force_heavy_left", false);
					rot.getPersistentData().putBoolean("debug_force_heavy_right", false);
					rot.getPersistentData().putBoolean("debug_force_uppercut_left", false);
					rot.getPersistentData().putBoolean("debug_force_uppercut_right", false);
					rot.getPersistentData().putBoolean("is_falling_heavy", false);
					rot.getPersistentData().putBoolean("is_uppercutting", false);
					rot.getPersistentData().putBoolean("is_uppercutting_left", false);
					rot.getPersistentData().putBoolean("is_uppercutting_right", false);
					rot.getPersistentData().putBoolean("is_uppercut_standalone", false);
					try {
						rot.getEntityData().set(RotEntity.DATA_is_falling_heavy, false);
					} catch (Exception e) {}
					try {
						rot.getEntityData().set(RotEntity.DATA_is_sonic_boom, false);
					} catch (Exception e) {}
					rot.getPersistentData().putDouble("rot_overhead_ticks", 0);
					rot.getPersistentData().putDouble("sentinel_cc2_stage", 0);
					rot.getPersistentData().putDouble("sentinel_cc2_ticks", 0);
					rot.getPersistentData().putDouble("sentinel_die_kick_phase", 0);
					rot.getPersistentData().putDouble("sentinel_die_kick_ticks", 0);
					rot.getPersistentData().putDouble("sentinel_sonic_ticks", 0);
					player.sendSystemMessage(Component.literal("§a[AI Debug] All forced test animations reset."));
				}
			}
		}
	}

	private static String formatTargetNames(List<? extends LivingEntity> entities) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (LivingEntity e : entities) {
			String dName = e.getDisplayName().getString();
			counts.put(dName, counts.getOrDefault(dName, 0) + 1);
		}
		StringBuilder namesSb = new StringBuilder();
		int cIdx = 0;
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			if (cIdx > 0) namesSb.append(", ");
			if (entry.getValue() > 1) {
				namesSb.append(entry.getValue()).append(" ").append(entry.getKey());
			} else {
				namesSb.append(entry.getKey());
			}
			cIdx++;
		}
		return namesSb.toString();
	}

	private static ServerPlayer findOnlinePlayer(MinecraftServer server, String name) {
		if (server == null || name == null || name.isEmpty()) return null;
		for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
			if (candidate.getGameProfile().getName().equalsIgnoreCase(name)) return candidate;
		}
		return null;
	}

	private static void setGuardTarget(ServerLevel level, ServerPlayer commander, Player guardTarget) {
		AABB box = commander.getBoundingBox().inflate(128.0);
		for (RotEntity rot : level.getEntitiesOfClass(RotEntity.class, box)) {
			rot.getPersistentData().putBoolean("master_guard_mode", true);
			rot.getPersistentData().putString("master_guard_target_uuid", guardTarget.getUUID().toString());
			rot.getPersistentData().putString("master_guard_target_name", guardTarget.getGameProfile().getName());
			rot.getPersistentData().putBoolean("master_follow_enabled", true);
			rot.getPersistentData().putString("master_follow_target_uuid", guardTarget.getUUID().toString());
			rot.getPersistentData().remove("master_kill_target_id");
			rot.getPersistentData().remove("master_target_queue");
			if (rot instanceof Mob mob) mob.setTarget(null);
		}
	}

	private static void clearGuardCommand(ServerLevel level, ServerPlayer commander) {
		AABB box = commander.getBoundingBox().inflate(128.0);
		for (RotEntity rot : level.getEntitiesOfClass(RotEntity.class, box)) {
			rot.getPersistentData().putBoolean("master_guard_mode", false);
			rot.getPersistentData().putBoolean("master_follow_enabled", false);
			rot.getPersistentData().remove("master_guard_target_uuid");
			rot.getPersistentData().remove("master_guard_target_name");
			rot.getPersistentData().remove("master_follow_target_uuid");
			if (rot instanceof Mob mob) mob.setTarget(null);
		}
	}

	private static void setFollowTarget(ServerLevel level, ServerPlayer commander, Player followTarget) {
		AABB box = commander.getBoundingBox().inflate(128.0);
		for (RotEntity rot : level.getEntitiesOfClass(RotEntity.class, box)) {
			rot.getPersistentData().putBoolean("master_follow_enabled", true);
			rot.getPersistentData().putString("master_follow_target_uuid", followTarget.getUUID().toString());
			rot.getPersistentData().putBoolean("master_guard_mode", false);
			rot.getPersistentData().remove("master_guard_target_uuid");
			rot.getPersistentData().remove("master_kill_target_id");
			rot.getPersistentData().remove("master_target_queue");
			if (rot instanceof Mob mob) mob.setTarget(null);
		}
	}

	private static void stopFollowTarget(ServerLevel level, ServerPlayer commander) {
		AABB box = commander.getBoundingBox().inflate(128.0);
		for (RotEntity rot : level.getEntitiesOfClass(RotEntity.class, box)) {
			rot.getPersistentData().putBoolean("master_guard_mode", false);
			rot.getPersistentData().putBoolean("master_follow_enabled", false);
			rot.getPersistentData().remove("master_guard_target_uuid");
			rot.getPersistentData().remove("master_guard_target_name");
			rot.getPersistentData().remove("master_follow_target_uuid");
			rot.getPersistentData().remove("master_kill_target_id");
			rot.getPersistentData().remove("master_target_queue");
			if (rot instanceof Mob mob) mob.setTarget(null);
		}
	}

	private static void setDuelTarget(ServerLevel level, ServerPlayer commander) {
		AABB box = commander.getBoundingBox().inflate(128.0);
		for (RotEntity rot : level.getEntitiesOfClass(RotEntity.class, box)) {
			rot.getPersistentData().putBoolean("is_dueling", true);
			rot.getPersistentData().putBoolean("master_guard_mode", false);
			rot.getPersistentData().putBoolean("master_follow_enabled", false);
			rot.getPersistentData().remove("master_guard_target_uuid");
			rot.getPersistentData().remove("master_guard_target_name");
			rot.getPersistentData().remove("master_follow_target_uuid");
			rot.getPersistentData().remove("master_kill_target_id");
			rot.getPersistentData().remove("master_target_queue");
			if (rot instanceof Mob mob) mob.setTarget(commander);
		}
	}

	private static void stopDuelTarget(ServerLevel level, ServerPlayer commander) {
		AABB box = commander.getBoundingBox().inflate(128.0);
		for (RotEntity rot : level.getEntitiesOfClass(RotEntity.class, box)) {
			rot.getPersistentData().putBoolean("is_dueling", false);
			if (rot instanceof Mob mob) mob.setTarget(null);
		}
	}

	private static List<LivingEntity> getEntitiesInPlayerFOV(Player player, double range) {
		List<LivingEntity> targets = new ArrayList<>();
		Vec3 eyePosition = player.getEyePosition(1.0F);
		Vec3 lookVec = player.getViewVector(1.0F).normalize();
		AABB searchBox = player.getBoundingBox().inflate(range);
		for (Entity entity : player.level().getEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive())) {
			if (entity instanceof RotEntity) continue;
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("spore:scent")) continue;
			if (entity instanceof Player p) {
				String pName = p.getGameProfile().getName();
				if (pName.equals("honeypie_3301") || pName.equals("Dev")) continue;
			}
			Vec3 toEntity = entity.position().subtract(eyePosition);
			double dist = toEntity.length();
			if (dist > range) continue;
			if (dist < 4.0) {
				targets.add((LivingEntity) entity);
				continue;
			}
			toEntity = toEntity.normalize();
			double dot = lookVec.dot(toEntity);
			if (dot > 0.5) {
				targets.add((LivingEntity) entity);
			}
		}
		return targets;
	}

	private static Entity getPlayerFOVTarget(Player player, double range) {
		Vec3 eyePosition = player.getEyePosition(1.0F);
		Vec3 lookVec = player.getViewVector(1.0F);
		AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D, 1.0D, 1.0D);
		Entity target = null;
		double closestDist = range;
		for (Entity entity : player.level().getEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive())) {
			if (entity instanceof RotEntity) continue;
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("spore:scent")) continue;
			if (entity instanceof Player p) {
				String pName = p.getGameProfile().getName();
				if (pName.equals("honeypie_3301") || pName.equals("Dev")) continue;
			}
			AABB hitBox = entity.getBoundingBox().inflate(0.3D);
			var hit = hitBox.clip(eyePosition, eyePosition.add(lookVec.scale(range)));
			if (hit.isPresent()) {
				double hitDist = eyePosition.distanceTo(hit.get());
				if (hitDist < closestDist) {
					closestDist = hitDist;
					target = entity;
				}
			}
		}
		return target;
	}

	private static void playHostileSound(LevelAccessor world, double x, double y, double z, String soundId, float volume, float pitch) {
		if (!(world instanceof Level level)) return;
		net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundId));
		if (sound != null) {
			if (!level.isClientSide()) {
				level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.HOSTILE, volume, pitch);
			} else {
				level.playLocalSound(x, y, z, sound, SoundSource.HOSTILE, volume, pitch, false);
			}
		}
	}

	private static boolean getRotPersistentBoolean(Entity entity, String key, boolean fallback) {
		if (entity == null) return fallback;
		if (entity.getPersistentData().contains(key)) {
			return entity.getPersistentData().getBoolean(key);
		}
		return fallback;
	}

	private static double getRotPersistentDouble(Entity entity, String key, double fallback) {
		if (entity == null) return fallback;
		if (entity.getPersistentData().contains(key)) {
			return entity.getPersistentData().getDouble(key);
		}
		return fallback;
	}

	private static String getRotPersistentString(Entity entity, String key, String fallback) {
		if (entity == null) return fallback;
		if (entity.getPersistentData().contains(key)) {
			return entity.getPersistentData().getString(key);
		}
		return fallback;
	}

	private static void setRotPersistentDouble(Entity entity, String key, double value) {
		if (entity != null) {
			entity.getPersistentData().putDouble(key, value);
		}
	}
 // 1.21.1
}
