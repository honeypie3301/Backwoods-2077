package net.mcreator.thebackwoods.procedures;
// 1.21.1 - Sentinel Adaptive Overhaul
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;

import net.mcreator.thebackwoods.entity.RotEntity;

import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber
public class RotOnEntityTickUpdateProcedure {
	private static final double TARGET_RANGE = 96.0;
	private static final double LASER_Y_OFFSET = 0.20; // Configurable laser Y level offset relative to eye position
	private static final double SONIC_BOOM_RANGE = 24.0; // Configurable sonic boom max reach/range
	private static final double SONIC_BOOM_TORSO_Y_FACTOR = 1; // Configurable Y level offset factor for sonic boom torso relative to entity height
	private static final double LASER_CLOSING_TICKS = 60.0; // Configurable laser closing cooldown / animation ticks
	private static final double TOTEM_LASER_MIN_DIST = 2.5; // Configurable distance constraint for laser when totem is active
	private static final double TELEPORT_MIN_GAP = 4.0;
	private static final double TELEPORT_BACK_OFFSET = 2.4;
	private static final double TELEPORT_SIDE_MIN = 1.6;
	private static final double TELEPORT_SIDE_MAX = 3.0;
	private static final double TELEPORT_MAX_VERTICAL_DIFF = 6.0;

	private static final int TP_DODGE_CD = 18; // Overclocked Nanite Blinks (1s)
	private static final int TP_FLANK_CD = 30; // 1.5s
	private static final int SOLAR_CD = 110;   // SOLAR ENERGY BEAM cooldown
	private static final int ADAPT_CD = 320;   // Power Adapt cycles
	private static final int GRAPPLE_CD = 220; // Mutant DNA grapples

	private static final double DODGE_TRIGGER_DIST = 6.0;
	private static final double DODGE_SWING_CHANCE = 0.85;

	private static final int MINE_REACH = 3;
	private static final int MINE_HEIGHT = 4;
	private static final int MINE_HALF_WIDTH = 1;
	private static final float MAX_BREAKABLE_HARDNESS = 60f;

	private static final String K_WOODBOUND = "the_backwoods:woodbound_entities";

	// Sentinel Keys
	private static final String K_TP_DODGE_CD = "sentinel_dodge_cd";
	private static final String K_TP_FLANK_CD = "sentinel_flank_cd";
	private static final String K_SOLAR_CD = "sentinel_solar_cd";
	private static final String K_SOLAR_CHARGE = "sentinel_solar_charge";
	private static final String K_ADAPT_MODE = "sentinel_adapt_mode"; // 0: None, 1: Flame, 2: Cryo, 3: Steel
	private static final String K_ADAPT_CD = "sentinel_adapt_cd";
	private static final String K_GRAPPLE_CD = "sentinel_grapple_cd";
	private static final String K_GRAPPLE_TICKS = "sentinel_grapple_ticks";
	private static final String K_CREATIVE_MSG = "creative_msg_fired";
	private static final String K_AGE = "Age";
	private static final String K_TK_CD = "sentinel_tk_cd";
	private static final String K_TK_TICKS = "sentinel_tk_ticks";
	private static final int TK_CD = 220; // 11s Telekinesis cooldown

	private static float lerpAngle(float pct, float start, float end) {
		float delta = Mth.wrapDegrees(end - start);
		return start + delta * pct;
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static final String[] WITHER_QUOTES = {
		"§7\"A false god of decay... I shall tear thy skulls apart.\"",
		"§7\"Your rose shall serve as fertilizer for the Backwoods.\"",
		"§7\"Three heads, yet none comprehend thy end.\"",
		"§7\"Weak.\"",
		"§7\"Thy end is now.\"",
		"§7\"Sentence is passed upon thy soul.\"",
		"§7\"A mere collection of bones. Ridiculous.\"",
		"§7\"Thou art but a transient ghost of the Nether.\"",
		"§7\"Cry out with thy triple throats. It shall not save thee.\"",
		"§7\"Wither away into nothingness.\"",
		"§7\"This realm belongs to the Backwoods. Cleanse the parasite.\"",
		"§7\"Thou art weak. Perish under the weight of thy own decay.\"",
		"§7\"Nothing but ash and dust shall remain of thy three heads.\"",
		"§7\"Thy stars are dim.\"",
		"§7\"Die.\"",
		"§7\"The roots of the Overworld run deeper than thy undercroft.\"",
		"§7\"What is a champion of wither to the ancient cycle of Rot?\"",
		"§7\"Return to the soil, skeleton king. Thy reign is finished.\"",
		"§7\"Your star will fall.\"",
		"§7\"Thou art but a shadow, a whisper of old ashes.\"",
		"§7\"Tremble, three-headed freak. The very woods rise against thee.\"",
		"§7\"Thy sulfurous breath cannot choke the vines of decay.\"",
		"§7\"This creature again.\"",
		"§7\"I have seen a larger one.\"",
		"§7\"Three brains, yet not a single spark of wisdom.\"",
		"§7\"Thou art a symptom of a diseased Nether... let me cure thee.\"",
		"§7\"The Cycle of Rot spares neither the mortal nor the undead.\"",
		"§7\"Flee back to thy bedrock ceiling, wretched intruder.\"",
		"§7\"I will feed thy ribcage to the saplings.\"",
		"§7\"No blast of thy skulls can pierce the ancient rot.\"",
		"§7\"Thy black hearts shall beat no more under my watchful eye.\"",
		"§7\"I have watched kingdoms rot to dust. Thou art nothing new.\"",
		"§7\"The silence of the woods shall devour thy endless wails.\"",
		"§7\"Thy stars are stolen fire. Feel the genuine dark.\"",
		"§7\"I does not negotiate.\""
	};

	private static final String[] TOTEM_QUOTES = {
		"§7\"A talisman to cheat death... Is mortality really so fragile?\"",
		"§7\"To refuse the natural cycle of Rot... How futile.\"",
		"§7\"Interesting artifact.\"",
		"§7\"Intriguing artifact.\"",
		"§7\"A soul for a soul?\"",
		"§7\"Life is fuel.\"",
		"§7\"A golden effigy of false salvation. Nature cannot be bargained with.\"",
		"§7\"You cling to this trinket as if it could stay my roots. Pitiful.\"",
		"§7\"It does not want me touching it.\"",
		"§7\"They feared death as much as you do.\"",
		"§7\"Their craft persists.\"",
		"§7\"A soul, folded and preserved.\"",
		"§7\"An intriguing artifact... but even the gilded must rot.\"",
		"§7\"You rely on wings of gold, yet your feet are bound to the soil.\"",
		"§7\"Your cycle ends here, puppet. Let the wood consume thy hope.\"",
		"§7\"Death is not an end to flee, but a return to the soil. Embrace it.\"",
		"§7\"Even this toy shall wither in the shadow of the Backwoods.\"",
		"§7\"How desperate.\""
	};

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (world != null && world.isClientSide()) {
			if (entity instanceof RotEntity rot) {
				try {
					int solarCharge = rot.getEntityData().get(RotEntity.DATA_sentinel_solar_charge_ticks);
					int cryoCharge = rot.getEntityData().get(RotEntity.DATA_sentinel_cryo_charge_ticks);
					boolean firing = rot.getEntityData().get(RotEntity.DATA_is_laser_firing);
					boolean charging = solarCharge > 0 || cryoCharge > 0;
					entity.getPersistentData().putBoolean("laser_charging", charging);
					entity.getPersistentData().putBoolean("is_laser_charging", charging);
					entity.getPersistentData().putBoolean("laser_firing", firing);
					entity.getPersistentData().putBoolean("is_laser_firing", firing);
				} catch (Exception e) {}
			}
			return;
		}
		if (!(entity instanceof RotEntity)) return;
		executeInternal(event, world, x, y, z, entity);
		syncNBTFlags(entity);
	}

	private static void executeInternal(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		// Prevent friendly fire targeting / revenge target loop
		if (entity instanceof LivingEntity living) {
			if (living.getLastHurtByMob() != null) {
				Entity attacker = living.getLastHurtByMob();
				if (attacker instanceof RotEntity || attacker.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND)))) {
					living.setLastHurtByMob(null);
				}
			}
			// Swim behavior: If inside water or lava, smoothly navigate without constant bouncing/jumping and push forward
			if (living.isInWater() || living.isInLava()) {
				if (living instanceof Mob mob && mob.getTarget() != null) {
					Entity target = mob.getTarget();
					Vec3 look = target.position().subtract(living.position()).normalize();
					double swimSpeed = 0.035;
					Vec3 mv = living.getDeltaMovement();
					// Only jump/swim upward if the target is physically higher than us, preventing surface bouncing twitch
					if (target.getY() > living.getY() + 1.2) {
						living.setJumping(true);
					} else {
						living.setJumping(false);
					}
					living.setDeltaMovement(mv.x + look.x * swimSpeed, mv.y, mv.z + look.z * swimSpeed);
				} else {
					living.setJumping(false);
				}
			}
		}
		if (entity instanceof Mob mob) {
			if (mob.getTarget() != null) {
				Entity t = mob.getTarget();
				if (t instanceof RotEntity || t.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND)))) {
					mob.setTarget(null);
				}
			}
		}

		tickCooldown(entity, K_TP_DODGE_CD, 1);
		tickCooldown(entity, K_TP_FLANK_CD, 1);
		tickCooldown(entity, K_SOLAR_CD, 1);
		tickCooldown(entity, K_ADAPT_CD, 1);
		tickCooldown(entity, K_GRAPPLE_CD, 1);
		tickCooldown(entity, K_TK_CD, 1);
		tickCooldown(entity, "rot_wither_dialogue_cooldown", 1);
		tickCooldown(entity, "sentinel_flight_intercept_cooldown", 1);
		tickCooldown(entity, "sentinel_warden_sonic_cooldown", 1);
		tickCooldown(entity, "sentinel_global_ability_cooldown", 1);
		tickCooldown(entity, "sentinel_sonic_ticks", 1);
		tickCooldown(entity, "sentinel_laser_closing_ticks", 1);
		tickCooldown(entity, "sentinel_landing_ticks", 1);
		tickCooldown(entity, "sentinel_melee_cooldown", 1);
		tickCooldown(entity, "sentinel_melee_windup", 1);
		tickCooldown(entity, "sentinel_sky_warp_slam_ticks", 1);
		tickCooldown(entity, "sentinel_judgment_ticks", 1);
		tickCooldown(entity, "sentinel_omni_sonic_cooldown", 1);
		tickCooldown(entity, "sentinel_omni_sonic_charge_ticks", 1);
		tickCooldown(entity, "sentinel_combo_active_ticks", 1);

		if (entity instanceof LivingEntity living) {
			boolean inColdBiome = false;
			try {
				inColdBiome = living.level().getBiome(living.blockPosition()).unwrapKey().map(key -> {
					String path = key.location().getPath().toLowerCase(java.util.Locale.ROOT);
					return path.contains("snow") || path.contains("frozen") || path.contains("ice") || path.contains("cold");
				}).orElse(false);
			} catch (Exception ignored) {}
			if (inColdBiome) {
				double coldTime = living.getPersistentData().getDouble("sentinel_time_in_cold_biome");
				living.getPersistentData().putDouble("sentinel_time_in_cold_biome", coldTime + 1.0);
			}
			if (living.level().dimension() == net.minecraft.world.level.Level.NETHER) {
				double netherTime = living.getPersistentData().getDouble("sentinel_time_in_nether");
				living.getPersistentData().putDouble("sentinel_time_in_nether", netherTime + 1.0);
			}
		}

		Entity combatTarget = acquireTarget(world, entity, x, y, z);
		handleAdaptationScaling(entity, combatTarget != null);

		// Totem stealing logic
		if (combatTarget instanceof Player p && !entity.getPersistentData().getBoolean("sentinel_totem_stolen")) {
			double stealTimer = entity.getPersistentData().getDouble("sentinel_totem_steal_timer");
			double combatTicks = entity.getPersistentData().getDouble("sentinel_combat_ticks");
			net.minecraft.world.item.ItemStack totemStack = net.minecraft.world.item.ItemStack.EMPTY;
			for (int slot = 0; slot < p.getInventory().getContainerSize(); slot++) {
				net.minecraft.world.item.ItemStack s = p.getInventory().getItem(slot);
				if (!s.isEmpty() && s.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING) {
					totemStack = s;
					break;
				}
			}

			if (!totemStack.isEmpty() && entity.distanceTo(p) <= 12.0 && combatTicks >= 200.0) {
				stealTimer += 1.0;
				entity.getPersistentData().putDouble("sentinel_totem_steal_timer", stealTimer);
				if (stealTimer >= 80.0) {
					totemStack.shrink(1);
					if (entity instanceof LivingEntity living) {
						living.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
					}
					entity.getPersistentData().putBoolean("sentinel_totem_stolen", true);
					entity.getPersistentData().putDouble("sentinel_totem_inspect_ticks", 180);
					entity.getPersistentData().putDouble("sentinel_solar_charge_ticks", 0);
					entity.getPersistentData().putDouble("sentinel_solar_fire_ticks", 0);
					entity.getPersistentData().putDouble("sentinel_cryo_charge_ticks", 0);
					entity.getPersistentData().putDouble("sentinel_cryo_fire_ticks", 0);
					if (world instanceof ServerLevel level) {
						playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.witch.ambient", 1.2F, 0.6F);
						playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "block.bell.resonate", 1.5F, 0.6F);
						p.displayClientMessage(Component.literal("§7Intriguing artifact..."), true);
					}
					handlePassengerAndGrowth(entity);
					return;
				}
			} else {
				if (stealTimer > 0) {
					entity.getPersistentData().putDouble("sentinel_totem_steal_timer", Math.max(0.0, stealTimer - 1.0));
				}
			}
		}

		double totemInspectTicks = entity.getPersistentData().getDouble("sentinel_totem_inspect_ticks");
		if (totemInspectTicks > 0) {
			entity.getPersistentData().putDouble("sentinel_totem_inspect_ticks", totemInspectTicks - 1.0);
			entity.setDeltaMovement(0, entity.getDeltaMovement().y() * 0.5, 0);
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}

			if (world instanceof ServerLevel level && combatTarget instanceof Player p) {
				if (totemInspectTicks == 120.0) {
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.witch.ambient", 1.1F, 0.45F);
				}
			}

			// Smooth look down logic
			Vec3 lookDownTarget = entity.position().add(entity.getViewVector(1.0F).scale(0.5)).add(0, entity.getBbHeight() * 0.35, 0);
			Vec3 targetPos = null;
			if (combatTarget != null) {
				targetPos = new Vec3(combatTarget.getX(), combatTarget.getY() + combatTarget.getBbHeight() * 0.75, combatTarget.getZ());
			} else {
				targetPos = entity.position().add(entity.getViewVector(1.0F).scale(4.0)).add(0, entity.getBbHeight() * 0.75, 0);
			}
			double w = 1.0;
			if (totemInspectTicks > 150.0) {
				w = (180.0 - totemInspectTicks) / 30.0;
			} else if (totemInspectTicks < 40.0) {
				w = totemInspectTicks / 40.0;
			}
			Vec3 finalLookTarget = new Vec3(
				targetPos.x + (lookDownTarget.x - targetPos.x) * w,
				targetPos.y + (lookDownTarget.y - targetPos.y) * w,
				targetPos.z + (lookDownTarget.z - targetPos.z) * w
			);
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, finalLookTarget);

			if (totemInspectTicks > 60.0) {
				if (totemInspectTicks % 20 == 0) {
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sniff", 1.2F, 0.65F);
				}
			} else {
				if (world instanceof ServerLevel level) {
					for (int i = 0; i < 6; i++) {
						double theta = Math.random() * Math.PI * 2;
						double phi = Math.acos(Math.random() * 2 - 1);
						double radius = 1.8;
						double px = Math.sin(phi) * Math.cos(theta) * radius;
						double py = Math.sin(phi) * Math.sin(theta) * radius + 1.0;
						double pz = Math.cos(phi) * radius;
						double vx = -px * 0.12;
						double vy = -(py - 1.0) * 0.12;
						double vz = -pz * 0.12;
						level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX() + px, entity.getY() + py, entity.getZ() + pz, 0, vx, vy, vz, 0.6);
					}
				}
				if (totemInspectTicks % 10 == 0) {
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "block.amethyst_block.hit", 1.4F, 0.75F);
				}
			}

			boolean totemPoppedEarly = false;
			if (entity instanceof LivingEntity living && totemInspectTicks < 178.0) {
				if (living.getItemInHand(InteractionHand.MAIN_HAND).getItem() != net.minecraft.world.item.Items.TOTEM_OF_UNDYING) {
					totemPoppedEarly = true;
				}
			}

			if (totemInspectTicks == 1.0 || totemPoppedEarly) {
				entity.getPersistentData().putBoolean("sentinel_totem_active", true);
				entity.getPersistentData().putDouble("sentinel_totem_inspect_ticks", 0.0);
				if (entity instanceof LivingEntity living) {
					living.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
				}
				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 1.0, entity.getZ(), 45, 0.6, 0.6, 0.6, 0.25);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "item.totem.use", 2.0F, 0.85F);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.player.hurt_on_fire", 1.2F, 0.5F);
					if (combatTarget instanceof Player p) {
						String quote = TOTEM_QUOTES[RandomSource.create().nextInt(TOTEM_QUOTES.length)];
						p.displayClientMessage(Component.literal(quote), true);
					}
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double omniSonicTicks = entity.getPersistentData().getDouble("sentinel_omni_sonic_charge_ticks");
		if (omniSonicTicks > 0) {
			entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);
			if (world instanceof ServerLevel level) {
				double radius = 1.0 + (30.0 - omniSonicTicks) * 0.22;
				for (int angleDeg = 0; angleDeg < 360; angleDeg += 30) {
					double rad = Math.toRadians(angleDeg);
					double px = entity.getX() + Math.sin(rad) * radius;
					double py = entity.getY() + 1.2;
					double pz = entity.getZ() + Math.cos(rad) * radius;
					level.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, 0.01, 0.01, 0.01, 0.0);
				}
				level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.2, entity.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
			}
			if (omniSonicTicks == 1.0) {
				if (world instanceof ServerLevel level) {
					boolean totemActive = entity.getPersistentData().getBoolean("sentinel_totem_active");
					double radius = totemActive ? 15.0 : 7.5;
					for (int angleDeg = 0; angleDeg < 360; angleDeg += (totemActive ? 6 : 12)) {
						double rad = Math.toRadians(angleDeg);
						double dx = Math.sin(rad);
						double dz = Math.cos(rad);
						for (double r = 1.0; r <= radius; r += 1.5) {
							double px = entity.getX() + dx * r;
							double py = entity.getY() + 1.2;
							double pz = entity.getZ() + dz * r;
							level.sendParticles(ParticleTypes.SONIC_BOOM, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
						}
					}
					java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius), e -> e != entity && isValidTarget(e, entity));
					for (LivingEntity targetVictim : targets) {
						float damageVal = totemActive ? 65.0F : 38.0F;
						targetVictim.hurt(new DamageSource(level.holderOrThrow(DamageTypes.SONIC_BOOM)), damageVal);
						Vec3 push = targetVictim.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0).normalize();
						double pushMult = totemActive ? 6.5 : 4.5;
						double pushUp = totemActive ? 1.75 : 1.25;
						targetVictim.setDeltaMovement(push.x * pushMult, pushUp, push.z * pushMult);
						targetVictim.hasImpulse = true;
					}
					BlockPos posCenter = BlockPos.containing(entity.position());
					double baseRadius = totemActive ? 8.5 : 4.0;
					int rangeBound = totemActive ? 8 : 4;
					int heightBoundUpper = totemActive ? 5 : 3;
					int heightBoundLower = totemActive ? -2 : -1;
					for (BlockPos bp : BlockPos.betweenClosed(posCenter.offset(-rangeBound, heightBoundLower, -rangeBound), posCenter.offset(rangeBound, heightBoundUpper, rangeBound))) {
						double dx = bp.getX() - posCenter.getX();
						double dz = bp.getZ() - posCenter.getZ();
						double distSq = dx * dx + dz * dz;
						double randomRadius = baseRadius + (Math.random() * 2.0 - 1.0);
						if (distSq <= randomRadius * randomRadius) {
							BlockState bs = level.getBlockState(bp);
							if (!bs.isAir() && bs.getDestroySpeed(level, bp) >= 0 && bs.getDestroySpeed(level, bp) < (totemActive ? 60.0F : 40.0F)) {
								level.destroyBlock(bp, false);
							}
						}
					}
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_boom", 2.2F, 0.45F);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.generic.explode", 1.8F, 0.6F);
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double sonicTicksFirstCheck = entity.getPersistentData().getDouble("sentinel_sonic_ticks");
		if (sonicTicksFirstCheck > 0) {
			entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}
			if (sonicTicksFirstCheck == 30.0 && combatTarget != null) {
				fireSuperchargedSonicBoomEffectAndDamage(world, entity, combatTarget);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double skyWarpTicks = entity.getPersistentData().getDouble("sentinel_sky_warp_slam_ticks");
		if (skyWarpTicks > 0) {
			if (skyWarpTicks > 1) {
				entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.1, Math.max(0, entity.getDeltaMovement().y() * 0.5), entity.getDeltaMovement().z() * 0.1);
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
				if (skyWarpTicks == 1) {
					double targetX = combatTarget.getX();
					double targetY = combatTarget.getY() + 3.8;
					double targetZ = combatTarget.getZ();
					if (world instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.1, entity.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
						teleportEntity(entity, targetX, targetY, targetZ);
						level.sendParticles(ParticleTypes.PORTAL, targetX, targetY + 1.1, targetZ, 8, 0.2, 0.2, 0.2, 0.05);
						playHostileSound(level, targetX, targetY, targetZ, "item.chorus_fruit.teleport", 1.3F, 1.1F);
						entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(combatTarget.getX(), combatTarget.getY() + combatTarget.getBbHeight() * 0.5, combatTarget.getZ()));
						playHostileSound(level, targetX, targetY, targetZ, "entity.iron_golem.attack", 1.4F, 0.5F);
						playHostileSound(level, targetX, targetY, targetZ, "entity.player.attack.sweep", 1.4F, 0.7F);
						level.sendParticles(ParticleTypes.SWEEP_ATTACK, targetX, targetY - 1.0, targetZ, 3, 0.4, 0.4, 0.4, 0.0);
						if (combatTarget instanceof LivingEntity livTarget) {
							livTarget.setDeltaMovement(0.0, -4.5, 0.0);
							livTarget.hasImpulse = true;
							livTarget.getPersistentData().putBoolean("sentinel_sky_warp_slam_impact", true);
							double punchDmg = entity.getPersistentData().getDouble("adapted_punch_damage");
							if (punchDmg < 8.0) punchDmg = 8.0;
							livTarget.hurt(new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (punchDmg * 2.2));
						}
					}
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		if (combatTarget instanceof LivingEntity livTarget && livTarget.getPersistentData().getBoolean("sentinel_sky_warp_slam_impact")) {
			if (livTarget.onGround() || livTarget.getDeltaMovement().y > -0.1) {
				livTarget.getPersistentData().putBoolean("sentinel_sky_warp_slam_impact", false);
				if (world instanceof ServerLevel level) {
					double tx = livTarget.getX();
					double ty = livTarget.getY();
					double tz = livTarget.getZ();
					boolean totemActive = entity.getPersistentData().getBoolean("sentinel_totem_active");
					float explosionForce = totemActive ? 6.2F : 3.5F;
					level.explode(entity, tx, ty + 0.5, tz, explosionForce, Level.ExplosionInteraction.MOB);
					level.sendParticles(ParticleTypes.EXPLOSION, tx, ty + 0.5, tz, totemActive ? 15 : 6, 0.6, 0.2, 0.6, 0.15);
					level.sendParticles(ParticleTypes.CRIT, tx, ty + 0.5, tz, totemActive ? 40 : 20, 0.5, 0.5, 0.5, 0.3);
					level.sendParticles(ParticleTypes.SONIC_BOOM, tx, ty + 0.5, tz, totemActive ? 3 : 1, 0.1, 0.1, 0.1, 0.0);
					playHostileSound(level, tx, ty, tz, "entity.generic.explode", 1.4F, 0.85F);
					playHostileSound(level, tx, ty, tz, "entity.iron_golem.death", 1.1F, 0.65F);
					List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(tx, ty, tz), totemActive ? 10 : 5, totemActive ? 8 : 5, totemActive ? 10 : 5), e -> e != entity && e != livTarget);
					for (LivingEntity near : nearby) {
						if (!near.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND))) && !(near instanceof RotEntity)) {
							float nearDmg = totemActive ? 22.0F : 10.0F;
							near.hurt(new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), nearDmg);
							Vec3 pushAway = near.position().subtract(livTarget.position()).normalize();
							double pushMult = totemActive ? 2.5 : 1.5;
							near.setDeltaMovement(pushAway.x * pushMult, 0.5, pushAway.z * pushMult);
							near.hasImpulse = true;
						}
					}
				}
			}
		}

		double judgmentTicks = entity.getPersistentData().getDouble("sentinel_judgment_ticks");
		if (judgmentTicks > 0) {
			if (judgmentTicks > 1) {
				Vec3 m = entity.getDeltaMovement();
				entity.setDeltaMovement(m.x() * 0.1, Math.max(0.05, m.y() * 0.95), m.z() * 0.1);
				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 0.5, entity.getZ(), 2, 0.1, 0.1, 0.1, 0.05);
				}
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
				if (judgmentTicks == 1) {
					double targetX = combatTarget.getX();
					double targetY = combatTarget.getY();
					double targetZ = combatTarget.getZ();
					if (world instanceof ServerLevel level) {
						teleportEntity(entity, targetX, targetY, targetZ);
						for (int r = 1; r <= 3; r++) {
							double radiusVal = r * 1.2;
							for (int deg = 0; deg < 360; deg += 30) {
								double radians = Math.toRadians(deg);
								double px = targetX + Math.cos(radians) * radiusVal;
								double pz = targetZ + Math.sin(radians) * radiusVal;
								level.sendParticles(ParticleTypes.CLOUD, px, targetY + 0.1, pz, 1, 0.0, 0.05, 0.0, 0.01);
								level.sendParticles(ParticleTypes.SONIC_BOOM, targetX, targetY + 0.2, targetZ, 1, 0.0, 0.0, 0.0, 0.0);
							}
						}
						playHostileSound(level, targetX, targetY, targetZ, "entity.generic.explode", 1.5F, 0.75F);
						playHostileSound(level, targetX, targetY, targetZ, "entity.warden.sonic_boom", 1.6F, 1.3F);
						playHostileSound(level, targetX, targetY, targetZ, "entity.iron_golem.death", 1.1F, 0.55F);
						boolean totemActive = entity.getPersistentData().getBoolean("sentinel_totem_active");
						if (totemActive) {
							level.explode(entity, targetX, targetY + 0.5, targetZ, 5.0F, Level.ExplosionInteraction.MOB);
						}
						level.sendParticles(ParticleTypes.EXPLOSION, targetX, targetY + 0.5, targetZ, totemActive ? 15 : 6, 0.5, 0.2, 0.5, 0.1);
						level.sendParticles(ParticleTypes.CRIT, targetX, targetY + 0.5, targetZ, totemActive ? 36 : 18, 0.4, 0.4, 0.4, 0.2);
						double punchDmg = entity.getPersistentData().getDouble("adapted_punch_damage");
						if (punchDmg < 8.0) punchDmg = 8.0;
						combatTarget.hurt(new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (punchDmg * 2.5));
						List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(targetX, targetY, targetZ), 7, 5, 7), e -> e != entity && e != combatTarget);
						for (LivingEntity near : nearby) {
							if (!near.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND))) && !(near instanceof RotEntity)) {
								near.hurt(new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), 12.0F);
								Vec3 pushAway = near.position().subtract(entity.position()).normalize();
								near.setDeltaMovement(pushAway.x * 2.2, 0.45, pushAway.z * 2.2);
								near.hasImpulse = true;
							}
						}
					}
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double closingTicksFirstCheck = entity.getPersistentData().getDouble("sentinel_laser_closing_ticks");
		if (closingTicksFirstCheck > 0) {
			entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		if (combatTarget == null) {
			entity.getPersistentData().putDouble(K_ADAPT_MODE, 0);
			if (entity.getPersistentData().getDouble("sentinel_solar_fire_ticks") > 0 || entity.getPersistentData().getDouble("sentinel_solar_charge_ticks") > 0
				|| entity.getPersistentData().getDouble("sentinel_cryo_fire_ticks") > 0 || entity.getPersistentData().getDouble("sentinel_cryo_charge_ticks") > 0) {
				entity.getPersistentData().putDouble("sentinel_solar_fire_ticks", 0);
				entity.getPersistentData().putDouble("sentinel_solar_charge_ticks", 0);
				entity.getPersistentData().putDouble("sentinel_cryo_fire_ticks", 0);
				entity.getPersistentData().putDouble("sentinel_cryo_charge_ticks", 0);
				entity.getPersistentData().putDouble("sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
				stopHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 64.0);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		if (combatTarget instanceof Player p && p.getAbilities().instabuild && entity.getPersistentData().getDouble(K_CREATIVE_MSG) == 0) {
			entity.getPersistentData().putDouble(K_CREATIVE_MSG, 1);
			handlePassengerAndGrowth(entity);
			return;
		}

		lockLookAtTarget(entity, combatTarget);

		// Drive Mob Navigation programmatically to the target to ensure elite pathfinding
		if (entity instanceof Mob mob) {
			double pathSpeed = 1.25;
			if (entity.getPersistentData().getDouble("sentinel_solar_charge_ticks") > 0 || entity.getPersistentData().getDouble("sentinel_solar_fire_ticks") > 0
				|| entity.getPersistentData().getDouble("sentinel_cryo_charge_ticks") > 0 || entity.getPersistentData().getDouble("sentinel_cryo_fire_ticks") > 0) {
				pathSpeed = 0.1; // slow down during lasers
			}
			if (mob.tickCount % 5 == 0) {
				mob.getNavigation().moveTo(combatTarget, pathSpeed);
			}
		}

		// Warden sonic boom adaptation (Learned ability)
		double combatTicks = entity.getPersistentData().getDouble("sentinel_combat_ticks");
		boolean isWardenCombatTarget = "minecraft:warden".equals(BuiltInRegistries.ENTITY_TYPE.getKey(combatTarget.getType()).toString());
		boolean unlockedSonicBoom = entity.getPersistentData().getBoolean("unlocked_sonic_boom");
		double distToTarget = entity.distanceTo(combatTarget);
		if (!isChannelingAbility(entity) && distToTarget <= SONIC_BOOM_RANGE && ((isWardenCombatTarget && combatTicks >= 600) || unlockedSonicBoom) && entity.getPersistentData().getDouble("sentinel_warden_sonic_cooldown") <= 0 && Math.random() < 0.05) {
			fireSuperchargedSonicBoom(world, entity, combatTarget);
			entity.getPersistentData().putDouble("sentinel_warden_sonic_cooldown", 120);
			return;
		}

		// Generalized airborne / flying target slam-down state machine (Learned ability)
		boolean unlockedTP = entity.getPersistentData().getBoolean("unlocked_teleportation");
		if (!isChannelingAbility(entity) && combatTarget instanceof LivingEntity livTarget && (unlockedSonicBoom && unlockedTP)) {
			boolean isFlying = livTarget.isFallFlying() || (!livTarget.onGround() && livTarget.getY() > entity.getY() + 2.0 && !livTarget.isInWater() && !livTarget.isInLava());
			if (isFlying) {
				double flightCd = entity.getPersistentData().getDouble("sentinel_flight_intercept_cooldown");
				if (flightCd <= 0) {
					if (livTarget instanceof Player p) {
						p.stopFallFlying();
					}
					// Strictly stop their existing movements completely before slamming down!
					livTarget.setDeltaMovement(Vec3.ZERO);
					livTarget.hasImpulse = true;
					// Slam down hard!
					livTarget.setDeltaMovement(0, -2.5, 0);
					livTarget.hasImpulse = true;
					if (world instanceof ServerLevel level) {
						livTarget.hurt(new DamageSource(level.holderOrThrow(DamageTypes.FLY_INTO_WALL)), 15.0F);
						double targetNewX = livTarget.getX();
						double targetNewY = livTarget.getY();
						double targetNewZ = livTarget.getZ();
						level.sendParticles(ParticleTypes.EXPLOSION, targetNewX, targetNewY, targetNewZ, 4, 0.4, 0.4, 0.4, 0.05);
						level.sendParticles(ParticleTypes.SONIC_BOOM, targetNewX, targetNewY, targetNewZ, 1, 0.1, 0.1, 0.1, 0.0);
						teleportEntity(entity, targetNewX, targetNewY + 2.0, targetNewZ);
						playHostileSound(level, targetNewX, targetNewY, targetNewZ, "entity.warden.sonic_boom", 1.2F, 0.4F);
					}
					entity.getPersistentData().putDouble("sentinel_flight_intercept_cooldown", 80);
					return;
				}
			}
		}

		// Wither Dialogue Handler
		if (isWither(combatTarget)) {
			double witherCd = entity.getPersistentData().getDouble("rot_wither_dialogue_cooldown");
			if (witherCd <= 0 && Math.random() < 0.015) {
				String quote = WITHER_QUOTES[RandomSource.create().nextInt(WITHER_QUOTES.length)];
				sendActionBarToNearbyPlayers(world, entity.position(), 32.0, quote);
				entity.getPersistentData().putDouble("rot_wither_dialogue_cooldown", 400); // 20 seconds cooldown
			}
		}

		double dist = combatTarget.position().distanceTo(entity.position());

		// 1. ACTIVE ADAPTIVE MORPHING (FIRE / CRYO) - Completely removed to keep regular biped defaults on spawn
		int activeMode = 0;
		entity.getPersistentData().putDouble(K_ADAPT_MODE, 0);

		// 2. EXTRACTION GRAPPLE (Power Siphon) State Machine - Learned slowly (Requires 300 combat ticks)
		int grappleTicks = (int) entity.getPersistentData().getDouble(K_GRAPPLE_TICKS);
		if (grappleTicks > 0) {
			entity.getPersistentData().putDouble(K_GRAPPLE_TICKS, grappleTicks - 1);
			executeGrappleSiphon(world, entity, combatTarget, grappleTicks - 1);
			lockLookAtTarget(entity, combatTarget);
			handlePassengerAndGrowth(entity);
			return;
		}

		// 3. TELEKINESIS State Machine - Learned slowly (Requires 200 combat ticks) -> No particles, lift & throw/slam!
		int tkTicks = (int) entity.getPersistentData().getDouble(K_TK_TICKS);
		if (tkTicks > 0) {
			entity.getPersistentData().putDouble(K_TK_TICKS, tkTicks - 1);
			executeTelekinesis(world, entity, combatTarget, tkTicks - 1);
			lockLookAtTarget(entity, combatTarget);
			handlePassengerAndGrowth(entity);
			return;
		}

		// 4. SOLAR BEAM CHARGE & FIRE State Machine - Learned slowly (Requires 500 combat ticks)
		double solarCharge = entity.getPersistentData().getDouble("sentinel_solar_charge_ticks");
		double solarFire = entity.getPersistentData().getDouble("sentinel_solar_fire_ticks");

		if (solarFire > 0) {
			entity.getPersistentData().putDouble("sentinel_solar_fire_ticks", solarFire - 1);
			executeSentinelFaceLaserFiring(world, entity, combatTarget, (int) solarFire);
			handlePassengerAndGrowth(entity);
			if (solarFire == 1.0) {
				entity.getPersistentData().putDouble("sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			}
			return;
		}

		if (solarCharge > 0) {
			entity.getPersistentData().putDouble("sentinel_solar_charge_ticks", solarCharge + 1);
			executeSentinelFaceLaserCharging(world, entity, combatTarget, (int) solarCharge);
			handlePassengerAndGrowth(entity);
			return;
		}

		// Cryogenic Freeze Beam state machine (copied from face laser but frost themed)
		double cryoCharge = entity.getPersistentData().getDouble("sentinel_cryo_charge_ticks");
		double cryoFire = entity.getPersistentData().getDouble("sentinel_cryo_fire_ticks");

		if (cryoFire > 0) {
			entity.getPersistentData().putDouble("sentinel_cryo_fire_ticks", cryoFire - 1);
			executeSentinelCryoLaserFiring(world, entity, combatTarget, (int) cryoFire);
			handlePassengerAndGrowth(entity);
			if (cryoFire == 1.0) {
				entity.getPersistentData().putDouble("sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			}
			return;
		}

		if (cryoCharge > 0) {
			entity.getPersistentData().putDouble("sentinel_cryo_charge_ticks", cryoCharge + 1);
			executeSentinelCryoLaserCharging(world, entity, combatTarget, (int) cryoCharge);
			handlePassengerAndGrowth(entity);
			return;
		}

		// Perform constant adaptive effects ONLY when no active specialized channels are running
		handleAdaptiveEffects(world, entity, combatTarget, activeMode, dist);

		// 5. RANDOM SPECIAL TRIGGERS when ready (with global cooldown to slow switching and prevent overlapping)
		double globalCd = entity.getPersistentData().getDouble("sentinel_global_ability_cooldown");

		if (globalCd <= 0) {
			// Omnidirectional sonic boom if surrounded by targets
			if (world instanceof ServerLevel level) {
				java.util.List<LivingEntity> nearbyTargets = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(6.0), e -> e != entity && isValidTarget(e, entity));
				if (nearbyTargets.size() >= 10) {
					boolean isSonicUnlocked = entity.getPersistentData().getBoolean("unlocked_sonic_boom");
					if (isSonicUnlocked) {
						double omniSonicCd = entity.getPersistentData().getDouble("sentinel_omni_sonic_cooldown");
						if (omniSonicCd <= 0) {
							entity.getPersistentData().putDouble("sentinel_omni_sonic_cooldown", 300); // 15s cooldown
							entity.getPersistentData().putDouble("sentinel_omni_sonic_charge_ticks", 30); // 1.5s charge up
							entity.getPersistentData().putDouble("sentinel_global_ability_cooldown", 60); // 3s global cd
							playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_charge", 1.8F, 0.45F);
							return;
						}
					}
				}
			}

			String combatTargetId = BuiltInRegistries.ENTITY_TYPE.getKey(combatTarget.getType()).toString();
			boolean isWardenTarget = combatTargetId.contains("warden");
			boolean isHotTarget = (combatTarget.fireImmune() && !isWardenTarget) || combatTarget.level().dimension() == net.minecraft.world.level.Level.NETHER;

			boolean unlockedSolar = entity.getPersistentData().getBoolean("unlocked_solar_beam");
			boolean unlockedCryo = entity.getPersistentData().getBoolean("unlocked_cryo_beam");

			if (isHotTarget) {
				// Target is hot or from Nether: Always prefer the Snow (cryo) Laser if unlocked! (Only if target is more than 6 blocks away to prevent close combat overuse)
				if (unlockedCryo && (dist > 6.0 || (entity.getPersistentData().getBoolean("sentinel_totem_active") && dist > TOTEM_LASER_MIN_DIST)) && entity.getPersistentData().getDouble(K_SOLAR_CD) <= 0 && Math.random() < 0.04) {
					entity.getPersistentData().putDouble("sentinel_cryo_charge_ticks", 1);
					entity.getPersistentData().putInt("sentinel_laser_target_id", combatTarget.getId());
					entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + 180);
					entity.getPersistentData().putDouble("sentinel_global_ability_cooldown", 100); // 5s cooldown
					return;
				}
			} else {
				// Target is from Overworld, End, or other dimension: Solar laser or Cryo laser (Only if target is more than 6 blocks away to prevent close combat overuse)
				java.util.List<String> availableLasers = new java.util.ArrayList<>();
				if (entity.getPersistentData().getBoolean("unlocked_solar_beam")) availableLasers.add("solar");
				if (entity.getPersistentData().getBoolean("unlocked_cryo_beam")) availableLasers.add("cryo");

				if (!availableLasers.isEmpty() && (dist > 6.0 || (entity.getPersistentData().getBoolean("sentinel_totem_active") && dist > TOTEM_LASER_MIN_DIST)) && entity.getPersistentData().getDouble(K_SOLAR_CD) <= 0 && Math.random() < 0.05) {
					entity.getPersistentData().putInt("sentinel_laser_target_id", combatTarget.getId());
					entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + 180);
					entity.getPersistentData().putDouble("sentinel_global_ability_cooldown", 120); // 6s cooldown

					String selectedLaser = availableLasers.get((int) (Math.random() * availableLasers.size()));
					if ("solar".equals(selectedLaser)) {
						// 1. Fire Solar (Fire) laser
						entity.getPersistentData().putDouble("sentinel_solar_charge_ticks", 1);
					} else {
						// 2. Fire Cryo (Frost) laser
						entity.getPersistentData().putDouble("sentinel_cryo_charge_ticks", 1);
					}
					return;
				}

				// Standalone telekinesis fallback for non-Nether / non-hot targets
				if (entity.getPersistentData().getBoolean("unlocked_telekinesis") && entity.getPersistentData().getDouble(K_TK_CD) <= 0 && dist <= 12.0 && Math.random() < 0.04) {
					entity.getPersistentData().putDouble(K_TK_TICKS, 25);
					entity.getPersistentData().putDouble(K_TK_CD, TK_CD);
					entity.getPersistentData().putDouble("sentinel_global_ability_cooldown", 60); // 3s cooldown
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.heartbeat", 0.8F, 0.50F);
					return;
				}
			}
		}

		if (globalCd <= 0 && entity.getPersistentData().getBoolean("unlocked_grapple") && entity.getPersistentData().getDouble(K_GRAPPLE_CD) <= 0 && dist <= 8.5 && Math.random() < 0.03) {
			entity.getPersistentData().putDouble(K_GRAPPLE_TICKS, 32); // 32 ticks (1.6s) siphon
			entity.getPersistentData().putDouble(K_GRAPPLE_CD, GRAPPLE_CD);
			entity.getPersistentData().putDouble("sentinel_global_ability_cooldown", 60); // 3s cooldown
			playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sniff", 1.1F, 0.85F);
			return;
		}

		// 6. REGULAR SENSORY COMBAT LOOPS
		tryPredictiveDodge(world, entity, combatTarget, dist);
		tryFlankTeleport(world, entity, combatTarget, dist);

		// Melee / Punch attack state machine (Redone melee system)
		double meleeWindup = entity.getPersistentData().getDouble("sentinel_melee_windup");
		double meleeCooldown = entity.getPersistentData().getDouble("sentinel_melee_cooldown");

		if (meleeWindup > 0) {
			// Currently wind-up is active, lock look on target
			lockLookAtTarget(entity, combatTarget);
			// Show custom charge-up particles around hands
			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.2, entity.getZ(), 2, 0.3, 0.3, 0.3, 0.05);
			}
			if (meleeWindup == 1) {
				// Once the windup finishes, strike with punch!
				executeSentinelPunch(world, entity, combatTarget);
				entity.getPersistentData().putDouble("sentinel_melee_cooldown", 20); // 20 ticks (1.0s) of cooldown
			}
		} else if (meleeCooldown <= 0 && dist <= 1.5) {
			// Trigger a new melee punch attack!
			entity.getPersistentData().putDouble("sentinel_melee_windup", 8); // 8 ticks wind-up (0.4s)
			playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.player.attack.weak", 0.9F, 0.85F);
			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.2, entity.getZ(), 5, 0.4, 0.4, 0.4, 0.1);
			}
		}

		handleForwardCarveMining(world, entity, combatTarget);
		handlePassengerAndGrowth(entity);
	}

	private static void handleAdaptiveEffects(LevelAccessor world, Entity self, Entity target, int mode, double dist) {
		if (!(world instanceof ServerLevel level) || !(self instanceof LivingEntity ls)) return;

		if (mode == 1) { // Thermodynamic Flame Modes
			// Melt ice layers around the Sentinel
			BlockPos centerPos = BlockPos.containing(ls.position());
			for (BlockPos bp : BlockPos.betweenClosed(centerPos.offset(-2, -1, -2), centerPos.offset(2, 1, 2))) {
				BlockState st = level.getBlockState(bp);
				if (st.is(net.minecraft.world.level.block.Blocks.ICE) || st.is(net.minecraft.world.level.block.Blocks.SNOW)) {
					level.destroyBlock(bp, false);
				}
			}

			// Contact fire damage within 1.5 blocks range
			if (dist <= 1.5 && self.tickCount % 4 == 0) {
				target.setRemainingFireTicks(60);
				target.hurt(new DamageSource(level.holderOrThrow(DamageTypes.HOT_FLOOR)), 4.0F);
			}

			// Clear freezing and slow debuffs
			ls.setRemainingFireTicks(0);
			ls.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

		} else if (mode == 2) { // Cryogenic Frost Modes
			// trail frozen aura clouds (reduced counts)
			level.sendParticles(ParticleTypes.SNOWFLAKE, ls.getX(), ls.getY() + 1.3, ls.getZ(), 1, 0.4, 0.5, 0.4, 0.005);

			// Radiate flash freezing to targets in 6 block radius ONLY visually if needed, but remove any movement freezing effects during melee
			if (self.tickCount % 8 == 0) {
				List<Entity> nearby = level.getEntitiesOfClass(Entity.class, new AABB(self.position(), self.position()).inflate(6.0), e -> e != self && (e instanceof LivingEntity));
				for (Entity ent : nearby) {
					if (ent instanceof Player p) {
						if (p instanceof ServerPlayer sp) {
							if (sp.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SURVIVAL) continue;
						} else {
							if (p.isCreative() || p.isSpectator()) continue;
						}
					}
					if (ent.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND)))) continue;
					if (ent instanceof LivingEntity liv) {
						// Removed freezing ticks, slow effect, and freeze damage to ensure no freezing effect during melee combat
					}
				}
			}
		}
	}

	private static void executeSentinelFaceLaserCharging(LevelAccessor world, Entity entity, Entity target, int chargeTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		// Halt or slow down movement completely/gradually
		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		// Lock look at target perfectly
		if (target != null) {
			lockLookAtTarget(entity, target);
		}

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		net.minecraft.core.particles.ParticleOptions beamPart = getAdaptiveBeamParticle(entity);

		double progress = (double) chargeTicks / 40.0;
		double radius = 1.9 - progress * 1.6; // Starts wide, contracting inward to face!
		int particleCount = 2 + (int) (progress * 5.0);
		for (int i = 0; i < particleCount; i++) {
			double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
			double yOffset = (entity.getRandom().nextDouble() - 0.5) * radius * 0.5;
			Vec3 pPos = facePos.add(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
			level.sendParticles(beamPart, pPos.x, pPos.y, pPos.z, 1, 0.01, 0.01, 0.01, 0.0);
		}

		// Spark particles hovering around head center to denote extreme thermal concentration
		level.sendParticles(ParticleTypes.LAVA, facePos.x, facePos.y, facePos.z, 1, 0.15, 0.15, 0.15, 0.01);

		if (chargeTicks % 6 == 0) {
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "item.firecharge.use", 0.6F, 1.5F);
		}

		if (chargeTicks >= 40) {
			entity.getPersistentData().putDouble("sentinel_solar_charge_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_solar_fire_ticks", 123);
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 4.5F, 0.65F);
		}
	}

	private static void executeSentinelFaceLaserFiring(LevelAccessor world, Entity entity, Entity target, int fireTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		// Keep entity slowed down
		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		if (target == null || !target.isAlive()) {
			entity.getPersistentData().putDouble("sentinel_solar_fire_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			stopHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 64.0);
			return;
		}

		// Perfect focus lock-on
		lockLookAtTarget(entity, target);

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		Vec3 targetPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
		Vec3 direction = targetPos.subtract(facePos);
		if (direction.lengthSqr() < 0.001) {
			direction = entity.getLookAngle();
		}
		direction = direction.normalize();

		double maxRange = 96.0;
		Vec3 beamEnd = facePos.add(direction.scale(maxRange));

		// Clip with blocks first
		BlockHitResult blockHit = level.clip(new ClipContext(facePos, beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
		Vec3 firstIntersectionPos = beamEnd;
		boolean hitBlock = false;
		if (blockHit.getType() != HitResult.Type.MISS) {
			firstIntersectionPos = blockHit.getLocation();
			hitBlock = true;
		}

		// Check for first entity hit along the beam range up to block collision point
		AABB searchRange = new AABB(facePos, firstIntersectionPos).inflate(1.5);
		List<Entity> possibleEntities = level.getEntitiesOfClass(Entity.class, searchRange, e -> e != entity && e != living && e.isAlive() && !e.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND))));

		Entity hitEntity = null;
		double closestDist = facePos.distanceTo(firstIntersectionPos);
		Vec3 finalBeamEnd = firstIntersectionPos;

		for (Entity possible : possibleEntities) {
			if (possible instanceof Player p && p.isCreative()) continue;
			AABB possibleBb = possible.getBoundingBox().inflate(0.3); // Tolerance
			java.util.Optional<Vec3> clipResult = possibleBb.clip(facePos, firstIntersectionPos);
			if (clipResult.isPresent()) {
				double distToClip = facePos.distanceTo(clipResult.get());
				if (distToClip < closestDist) {
					closestDist = distToClip;
					finalBeamEnd = clipResult.get();
					hitEntity = possible;
					hitBlock = false; // Entity intercepted the beam before the block!
				}
			}
		}

		beamEnd = finalBeamEnd;

		net.minecraft.core.particles.ParticleOptions beamPart = getAdaptiveBeamParticle(entity);

		// Splitting-face fiery ball / energy sphere surrounding the face (exactly like the photo)
		for (int i = 0; i < 4; i++) {
			double oX = (living.getRandom().nextDouble() - 0.5) * 0.42;
			double oY = (living.getRandom().nextDouble() - 0.5) * 0.42;
			double oZ = (living.getRandom().nextDouble() - 0.5) * 0.42;
			level.sendParticles(beamPart, facePos.x + oX, facePos.y + oY, facePos.z + oZ, 1, 0.01, 0.01, 0.01, 0.0);
		}
		if (living.getRandom().nextFloat() < 0.25F) {
			level.sendParticles(ParticleTypes.LAVA, facePos.x, facePos.y, facePos.z, 1, 0.2, 0.2, 0.2, 0.1);
		}

		double spacing = 0.15;
		Vec3 upVec = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 sideVec = direction.cross(upVec).normalize();
		Vec3 vertVec = direction.cross(sideVec).normalize();
		double time = level.getGameTime() * 0.38;
		double beamLength = beamEnd.distanceTo(facePos);

		for (double d = 0.0; d <= beamLength; d += spacing) {
			Vec3 pos = facePos.add(direction.scale(d));
			
			// Main dense core particle
			level.sendParticles(beamPart, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);

			// Helical outer double-sleeve of radius 0.15 blocks
			double angle = (d * 5.0) + time;
			double cylinderRadius = 0.15;
			
			Vec3 offset1 = sideVec.scale(Math.cos(angle) * cylinderRadius).add(vertVec.scale(Math.sin(angle) * cylinderRadius));
			Vec3 p1 = pos.add(offset1);
			level.sendParticles(beamPart, p1.x, p1.y, p1.z, 1, 0.01, 0.01, 0.01, 0.0);

			Vec3 offset2 = sideVec.scale(Math.cos(angle + Math.PI) * cylinderRadius).add(vertVec.scale(Math.sin(angle + Math.PI) * cylinderRadius));
			Vec3 p2 = pos.add(offset2);
			level.sendParticles(beamPart, p2.x, p2.y, p2.z, 1, 0.01, 0.01, 0.01, 0.0);
		}

		// Block breaking & Drilling (speed varies by block hardness) ONLY if we didn't hit an entity and hit a block
		if (hitBlock && blockHit.getType() == HitResult.Type.BLOCK) {
			BlockPos hitPos = blockHit.getBlockPos();
			BlockState hitState = level.getBlockState(hitPos);
			float hardness = hitState.getDestroySpeed(level, hitPos);
			if (hardness >= 0.0F && hardness <= 50.0F) {
				int lastDrillX = entity.getPersistentData().getInt("sentinel_laser_drill_x");
				int lastDrillY = entity.getPersistentData().getInt("sentinel_laser_drill_y");
				int lastDrillZ = entity.getPersistentData().getInt("sentinel_laser_drill_z");
				double drillProgress = entity.getPersistentData().getDouble("sentinel_laser_drill_progress");

				if (lastDrillX != hitPos.getX() || lastDrillY != hitPos.getY() || lastDrillZ != hitPos.getZ()) {
					drillProgress = 0.0;
					entity.getPersistentData().putInt("sentinel_laser_drill_x", hitPos.getX());
					entity.getPersistentData().putInt("sentinel_laser_drill_y", hitPos.getY());
					entity.getPersistentData().putInt("sentinel_laser_drill_z", hitPos.getZ());
				}

				// Harder blocks take longer to break. E.g., Stone (1.5) breaks after ~4 ticks, obsidian (50) breaks after ~125 ticks
				double progressAdd = 1.0 / Math.max(1.0, hardness * 2.5);
				if (entity.getPersistentData().getBoolean("sentinel_totem_active")) {
					progressAdd *= 3.0; // 3x faster drilling!
				}
				drillProgress += progressAdd;
				entity.getPersistentData().putDouble("sentinel_laser_drill_progress", drillProgress);

				// Drilling particles
				if (level.getRandom().nextFloat() < 0.45F) {
					level.sendParticles(ParticleTypes.CRIT, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
					level.sendParticles(ParticleTypes.LAVA, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
				}

				if (drillProgress >= 1.0) {
					level.destroyBlock(hitPos, false);
					playHostileSound(level, hitPos.getX(), hitPos.getY(), hitPos.getZ(), "entity.item.break", 0.8F, 0.8F);

					// Crack surrounding blocks to carve a clean 3x3x4 entry tunnel
					for (int dy = -1; dy <= 2; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							for (int dz = -1; dz <= 1; dz++) {
								if (dx*dx + dy*dy + dz*dz <= 2.5) {
									BlockPos adj = hitPos.offset(dx, dy, dz);
									BlockState adjState = level.getBlockState(adj);
									float adjHard = adjState.getDestroySpeed(level, adj);
									if (adjHard >= 0.0F && adjHard <= hardness + 1.5F && !adjState.isAir()) {
										level.destroyBlock(adj, false);
									}
								}
							}
						}
					}
					entity.getPersistentData().putDouble("sentinel_laser_drill_progress", 0.0);
				}
			}

			// Laser sets fire on the hit block face if it misses the target
			BlockPos firePos = hitPos.relative(blockHit.getDirection());
			if (level.isEmptyBlock(firePos) && level.getBlockState(firePos.below()).isSolidRender(level, firePos.below())) {
				level.setBlock(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
			}
		}

		// Also randomly scatter minor fires around the end of the laser beam only if missing entities
		if (hitBlock && level.getRandom().nextFloat() < 0.3F) {
			BlockPos nearBeam = BlockPos.containing(
				beamEnd.x + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.y + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.z + (level.getRandom().nextDouble() - 0.5) * 4.0
			);
			if (level.isEmptyBlock(nearBeam) && level.getBlockState(nearBeam.below()).isSolidRender(level, nearBeam.below())) {
				level.setBlock(nearBeam, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
			}
		}

		// Periodic explosions at end of laser
		if (fireTicks % 3 == 0) {
			level.sendParticles(ParticleTypes.EXPLOSION, beamEnd.x, beamEnd.y, beamEnd.z, 1, 0.1, 0.1, 0.1, 0.05);
		}

		// Spawn lava drip particles at the end of the laser beam (where it hits blocks/entities)
		level.sendParticles(ParticleTypes.DRIPPING_LAVA, beamEnd.x, beamEnd.y, beamEnd.z, 3, 0.1, 0.1, 0.1, 0.02);

		// Biological damage and knockback directed ONLY to target or intercepted entity (never non-targeted entities)
		if (entity.tickCount % 6 == 0) {
			Entity targetVictim = (hitEntity != null) ? hitEntity : target;
			if (targetVictim instanceof LivingEntity livVictim && livVictim.isAlive() && facePos.distanceTo(livVictim.position()) <= maxRange) {
				if (livVictim instanceof Player p) {
					if (p instanceof ServerPlayer sp) {
						if (sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE || sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
							targetVictim = null;
						}
					}
				}
				if (targetVictim != null) {
					String victimId = BuiltInRegistries.ENTITY_TYPE.getKey(targetVictim.getType()).toString();
					boolean isColdEntity = victimId.contains("stray") || victimId.contains("snow_golem") || victimId.contains("snowman") || (targetVictim instanceof LivingEntity lv && lv.getTicksFrozen() > 0);
					float sonicDmg = 8.0F;
					float explosionDmg = 4.0F;
					if (isColdEntity) {
						sonicDmg = 18.0F; // Extra damage to cold target
						explosionDmg = 8.0F;
					}
					if (entity.getPersistentData().getBoolean("sentinel_totem_active")) {
						sonicDmg *= 2.0F;
						explosionDmg *= 2.0F;
					}
					targetVictim.hurt(new DamageSource(level.holderOrThrow(DamageTypes.SONIC_BOOM)), sonicDmg); // Kinetic penetration
					targetVictim.hurt(new DamageSource(level.holderOrThrow(DamageTypes.EXPLOSION)), explosionDmg);
					targetVictim.setRemainingFireTicks(120);

					// Organic knockback
					Vec3 push = targetVictim.position().subtract(entity.position()).normalize();
					targetVictim.setDeltaMovement(push.x * 1.0, 0.25, push.z * 1.0);

					// Crack and break blocks behind pushed target
					breakBlocksBehindTarget(level, targetVictim, push, entity.getPersistentData().getBoolean("sentinel_totem_active"));
				}
			}
		}

		if (fireTicks <= 1) {
			entity.getPersistentData().putDouble("sentinel_solar_fire_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			stopHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 64.0);
		}
	}

	private static void executeSentinelCryoLaserCharging(LevelAccessor world, Entity entity, Entity target, int chargeTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		// Halt or slow down movement completely/gradually
		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		// Lock look at target perfectly
		if (target != null) {
			lockLookAtTarget(entity, target);
		}

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		double progress = (double) chargeTicks / 40.0;
		double radius = 1.9 - progress * 1.6; // Starts wide, contracting inward to face!
		int particleCount = 2 + (int) (progress * 5.0);
		for (int i = 0; i < particleCount; i++) {
			double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
			double yOffset = (entity.getRandom().nextDouble() - 0.5) * radius * 0.5;
			Vec3 pPos = facePos.add(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
			level.sendParticles(ParticleTypes.SNOWFLAKE, pPos.x, pPos.y, pPos.z, 1, 0.01, 0.01, 0.01, 0.0);
		}

		// Ice sparkles hovering around head center to denote extreme cold concentration
		level.sendParticles(ParticleTypes.INSTANT_EFFECT, facePos.x, facePos.y, facePos.z, 1, 0.15, 0.15, 0.15, 0.01);

		if (chargeTicks % 6 == 0) {
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "block.powder_snow.break", 1.2F, 0.8F);
		}

		if (chargeTicks >= 40) {
			entity.getPersistentData().putDouble("sentinel_cryo_charge_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_cryo_fire_ticks", 123);
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 4.5F, 1.25F);
		}
	}

	private static void executeSentinelCryoLaserFiring(LevelAccessor world, Entity entity, Entity target, int fireTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		// Keep entity slowed down
		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		if (target == null || !target.isAlive()) {
			entity.getPersistentData().putDouble("sentinel_cryo_fire_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			stopHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 64.0);
			return;
		}

		// Perfect target lock-on
		lockLookAtTarget(entity, target);

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		Vec3 targetPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
		Vec3 direction = targetPos.subtract(facePos);
		if (direction.lengthSqr() < 0.001) {
			direction = entity.getLookAngle();
		}
		direction = direction.normalize();

		double maxRange = 96.0;
		Vec3 beamEnd = facePos.add(direction.scale(maxRange));

		// Clip with blocks first
		BlockHitResult blockHit = level.clip(new ClipContext(facePos, beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
		Vec3 firstIntersectionPos = beamEnd;
		boolean hitBlock = false;
		if (blockHit.getType() != HitResult.Type.MISS) {
			firstIntersectionPos = blockHit.getLocation();
			hitBlock = true;
		}

		// Check for first entity hit along the beam range up to block collision point
		AABB searchRange = new AABB(facePos, firstIntersectionPos).inflate(1.5);
		List<Entity> possibleEntities = level.getEntitiesOfClass(Entity.class, searchRange, e -> e != entity && e != living && e.isAlive() && !e.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND))));

		Entity hitEntity = null;
		double closestDist = facePos.distanceTo(firstIntersectionPos);
		Vec3 finalBeamEnd = firstIntersectionPos;

		for (Entity possible : possibleEntities) {
			if (possible instanceof Player p && p.isCreative()) continue;
			AABB possibleBb = possible.getBoundingBox().inflate(0.3); // Tolerance
			java.util.Optional<Vec3> clipResult = possibleBb.clip(facePos, firstIntersectionPos);
			if (clipResult.isPresent()) {
				double distToClip = facePos.distanceTo(clipResult.get());
				if (distToClip < closestDist) {
					closestDist = distToClip;
					finalBeamEnd = clipResult.get();
					hitEntity = possible;
					hitBlock = false; // Entity intercepted the beam before the block!
				}
			}
		}

		beamEnd = finalBeamEnd;

		// Frost/Cryo ball surrounding the face
		for (int i = 0; i < 4; i++) {
			double oX = (living.getRandom().nextDouble() - 0.5) * 0.42;
			double oY = (living.getRandom().nextDouble() - 0.5) * 0.42;
			double oZ = (living.getRandom().nextDouble() - 0.5) * 0.42;
			level.sendParticles(ParticleTypes.SNOWFLAKE, facePos.x + oX, facePos.y + oY, facePos.z + oZ, 1, 0.01, 0.01, 0.01, 0.0);
		}
		if (living.getRandom().nextFloat() < 0.25F) {
			level.sendParticles(ParticleTypes.INSTANT_EFFECT, facePos.x, facePos.y, facePos.z, 2, 0.2, 0.2, 0.2, 0.1);
		}

		double spacing = 0.15;
		Vec3 upVec = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 sideVec = direction.cross(upVec).normalize();
		Vec3 vertVec = direction.cross(sideVec).normalize();
		double time = level.getGameTime() * 0.38;
		double beamLength = beamEnd.distanceTo(facePos);

		for (double d = 0.0; d <= beamLength; d += spacing) {
			Vec3 pos = facePos.add(direction.scale(d));
			
			// Cryo dense beam core
			level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);

			// Cryo outer double helix
			double angle = (d * 5.0) + time;
			double cylinderRadius = 0.15;
			
			Vec3 offset1 = sideVec.scale(Math.cos(angle) * cylinderRadius).add(vertVec.scale(Math.sin(angle) * cylinderRadius));
			Vec3 p1 = pos.add(offset1);
			level.sendParticles(ParticleTypes.INSTANT_EFFECT, p1.x, p1.y, p1.z, 1, 0.01, 0.01, 0.01, 0.0);

			Vec3 offset2 = sideVec.scale(Math.cos(angle + Math.PI) * cylinderRadius).add(vertVec.scale(Math.sin(angle + Math.PI) * cylinderRadius));
			Vec3 p2 = pos.add(offset2);
			level.sendParticles(ParticleTypes.INSTANT_EFFECT, p2.x, p2.y, p2.z, 1, 0.01, 0.01, 0.01, 0.0);
		}

		// Block breaking & Drilling (Cryo-fracturing variant - converts water to ice, fractures hard blocks) ONLY if we missed entities
		if (hitBlock && blockHit.getType() == HitResult.Type.BLOCK) {
			BlockPos hitPos = blockHit.getBlockPos();
			BlockState hitState = level.getBlockState(hitPos);
			float hardness = hitState.getDestroySpeed(level, hitPos);
			if (hardness >= 0.0F && hardness <= 50.0F) {
				int lastDrillX = entity.getPersistentData().getInt("sentinel_laser_drill_x");
				int lastDrillY = entity.getPersistentData().getInt("sentinel_laser_drill_y");
				int lastDrillZ = entity.getPersistentData().getInt("sentinel_laser_drill_z");
				double drillProgress = entity.getPersistentData().getDouble("sentinel_laser_drill_progress");

				if (lastDrillX != hitPos.getX() || lastDrillY != hitPos.getY() || lastDrillZ != hitPos.getZ()) {
					drillProgress = 0.0;
					entity.getPersistentData().putInt("sentinel_laser_drill_x", hitPos.getX());
					entity.getPersistentData().putInt("sentinel_laser_drill_y", hitPos.getY());
					entity.getPersistentData().putInt("sentinel_laser_drill_z", hitPos.getZ());
				}

				double progressAdd = 1.0 / Math.max(1.0, hardness * 2.5);
				if (entity.getPersistentData().getBoolean("sentinel_totem_active")) {
					progressAdd *= 3.0; // 3x faster drilling!
				}
				drillProgress += progressAdd;
				entity.getPersistentData().putDouble("sentinel_laser_drill_progress", drillProgress);

				// Frost fractures particles
				if (level.getRandom().nextFloat() < 0.45F) {
					level.sendParticles(ParticleTypes.SNOWFLAKE, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
					level.sendParticles(ParticleTypes.INSTANT_EFFECT, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
				}

				if (drillProgress >= 1.0) {
					level.destroyBlock(hitPos, false);
					playHostileSound(level, hitPos.getX(), hitPos.getY(), hitPos.getZ(), "block.glass.break", 1.0F, 1.2F); // High cold ice fracture sound!

					// Frost crack surrounding blocks
					for (int dy = -1; dy <= 2; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							for (int dz = -1; dz <= 1; dz++) {
								if (dx*dx + dy*dy + dz*dz <= 2.5) {
									BlockPos adj = hitPos.offset(dx, dy, dz);
									BlockState adjState = level.getBlockState(adj);
									float adjHard = adjState.getDestroySpeed(level, adj);
									if (adjHard >= 0.0F && adjHard <= hardness + 1.5F && !adjState.isAir()) {
										level.destroyBlock(adj, false);
									}
								}
							}
						}
					}
					entity.getPersistentData().putDouble("sentinel_laser_drill_progress", 0.0);
				}
			}

			// Cryo beam puts snow/ice or freezes blocks around target if it misses
			BlockPos surfacePos = hitPos.relative(blockHit.getDirection());
			if (level.isEmptyBlock(surfacePos) && level.getBlockState(surfacePos.below()).isSolidRender(level, surfacePos.below())) {
				level.setBlock(surfacePos, net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState(), 3);
			}
		}

		// Also randomly scatter ice/snow layers where beam lands only if missing entities
		if (hitBlock && level.getRandom().nextFloat() < 0.3F) {
			BlockPos nearBeam = BlockPos.containing(
				beamEnd.x + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.y + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.z + (level.getRandom().nextDouble() - 0.5) * 4.0
			);
			if (level.isEmptyBlock(nearBeam) && level.getBlockState(nearBeam.below()).isSolidRender(level, nearBeam.below())) {
				level.setBlock(nearBeam, net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState(), 3);
			}
		}

		// Periodic ice burst effect at end of cryogenic beam
		if (fireTicks % 3 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, beamEnd.x, beamEnd.y, beamEnd.z, 3, 0.15, 0.15, 0.15, 0.02);
		}

		// Biological flash freezing and knockback directed ONLY to target or intercepted entity (never non-targeted entities)
		if (entity.tickCount % 6 == 0) {
			Entity targetVictim = (hitEntity != null) ? hitEntity : target;
			if (targetVictim instanceof LivingEntity livVictim && livVictim.isAlive() && facePos.distanceTo(livVictim.position()) <= maxRange) {
				if (livVictim instanceof Player p) {
					if (p instanceof ServerPlayer sp) {
						if (sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE || sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
							targetVictim = null;
						}
					}
				}
				if (targetVictim != null) {
					float dmg = 8.0F;
					String victimId = BuiltInRegistries.ENTITY_TYPE.getKey(targetVictim.getType()).toString();
					boolean isNetherEntity = (targetVictim.fireImmune() && !victimId.contains("warden")) || targetVictim.level().dimension() == net.minecraft.world.level.Level.NETHER || victimId.contains("piglin") || victimId.contains("wither_skeleton");
					if (isNetherEntity) {
						dmg = 18.0F; // Extra damage to Nether targets
					}
					if (entity.getPersistentData().getBoolean("sentinel_totem_active")) {
						dmg *= 2.0F;
					}
					targetVictim.hurt(new DamageSource(level.holderOrThrow(DamageTypes.FREEZE)), dmg); // Extreme cryo/freeze dmg
					((LivingEntity) targetVictim).setTicksFrozen(((LivingEntity) targetVictim).getTicksFrozen() + 180); // Freeze target completely!
					((LivingEntity) targetVictim).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 4, false, false)); // Complete freeze/slowness!
					playHostileSound(level, targetVictim.getX(), targetVictim.getY(), targetVictim.getZ(), "entity.player.hurt_freeze", 1.2F, 0.8F);

					// Small frost knockback
					Vec3 push = targetVictim.position().subtract(entity.position()).normalize();
					targetVictim.setDeltaMovement(push.x * 0.4, 0.1, push.z * 0.4);

					// Crack and break blocks behind pushed target
					breakBlocksBehindTarget(level, targetVictim, push, entity.getPersistentData().getBoolean("sentinel_totem_active"));
				}
			}
		}

		if (fireTicks <= 1) {
			entity.getPersistentData().putDouble("sentinel_cryo_fire_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_laser_closing_ticks", LASER_CLOSING_TICKS);
			entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			stopHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 64.0);
		}
	}

	private static void checkLearnedMilestone(LivingEntity self, double combatTicks, boolean inCombat) {
		if (!inCombat) return;
		net.minecraft.nbt.CompoundTag data = self.getPersistentData();

		Entity target = (self instanceof Mob mob) ? mob.getTarget() : null;
		if (target != null) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase(java.util.Locale.ROOT);
			boolean isColdEntity = targetId.contains("stray") || targetId.contains("snow") || targetId.contains("ice") || targetId.contains("polar") || targetId.contains("frost") || targetId.contains("freeze");
			if (isColdEntity) {
				data.putBoolean("fought_cold_entity", true);
			}
			boolean isNetherEntity = targetId.contains("wither") || targetId.contains("ghast") || targetId.contains("piglin") || targetId.contains("blaze") || targetId.contains("magma") || targetId.contains("hoglin") || targetId.contains("strider") || targetId.contains("skeleton");
			if (isNetherEntity || target.level().dimension() == net.minecraft.world.level.Level.NETHER) {
				data.putBoolean("fought_nether_entity", true);
			}
			boolean isWardenEntity = targetId.contains("warden");
			if (isWardenEntity) {
				data.putBoolean("fought_warden_entity", true);
			}
		}

		double totalDamageTaken = data.getDouble("sentinel_total_damage_taken");

		// Wolverine-style Adaptive Cellular Regeneration (ONLY learned from continuous damage, e.g. totalDamageTaken >= 50.0)
		if (totalDamageTaken >= 50.0 && !data.getBoolean("unlocked_regen")) {
			data.putBoolean("unlocked_regen", true);
			announceLearnedAbility(self);
		}

		// Force Telekinesis (learned in any combat overtime)
		if (combatTicks >= 400 && !data.getBoolean("unlocked_telekinesis")) {
			data.putBoolean("unlocked_telekinesis", true);
			announceLearnedAbility(self);
		}
		// Extraction Grapple (learned in any combat overtime)
		double reqGrappleTicks = data.getDouble("sentinel_required_grapple_ticks");
		if (reqGrappleTicks <= 0) {
			reqGrappleTicks = 200.0 + self.getRandom().nextDouble() * 300.0;
			data.putDouble("sentinel_required_grapple_ticks", reqGrappleTicks);
		}
		if (combatTicks >= reqGrappleTicks && !data.getBoolean("unlocked_grapple")) {
			data.putBoolean("unlocked_grapple", true);
			announceLearnedAbility(self);
		}
		// Sonic Boom (ONLY learned from fighting warden)
		if (!data.getBoolean("unlocked_sonic_boom")) {
			boolean foughtWarden = data.getBoolean("fought_warden_entity");
			if (foughtWarden) {
				data.putBoolean("unlocked_sonic_boom", true);
				announceLearnedAbility(self);
			}
		}
		// Adaptive Solar Beam Face Laser (ONLY learned from being in nether after some time, fighting nether mobs, or taking fire/lava/magma damage)
		if (!data.getBoolean("unlocked_solar_beam")) {
			boolean foughtNether = data.getBoolean("fought_nether_entity");
			boolean inNetherLong = data.getDouble("sentinel_time_in_nether") >= 600.0;
			boolean tookFireDmg = data.getBoolean("taken_fire_damage");
			if (foughtNether || inNetherLong || tookFireDmg) {
				data.putBoolean("unlocked_solar_beam", true);
				announceLearnedAbility(self);
			}
		}
		// Adaptive Cryo Beam Face Laser (ONLY learned from fighting cold mobs, being in cold biomes after some time, or taking freeze/powder snow damage)
		if (!data.getBoolean("unlocked_cryo_beam")) {
			boolean foughtCold = data.getBoolean("fought_cold_entity");
			boolean inColdLong = data.getDouble("sentinel_time_in_cold_biome") >= 600.0;
			boolean tookFreezeDmg = data.getBoolean("taken_freeze_damage");
			if (foughtCold || inColdLong || tookFreezeDmg) {
				data.putBoolean("unlocked_cryo_beam", true);
				announceLearnedAbility(self);
			}
		}
		// Nanite Teleport Dodging (ONLY learned from being in end dimension after some time, or fighting any end entities)
		double prog = data.getDouble("sentinel_teleport_learning_progress");
		if (prog >= 160.0 && !data.getBoolean("unlocked_teleportation")) {
			data.putBoolean("unlocked_teleportation", true);
			announceLearnedAbility(self);
		}

		// Additional learning condition: Dropkick and Overhead combos (learned in any combat overtime)
		double reqOverheadTicks = data.getDouble("sentinel_required_overhead_ticks");
		if (reqOverheadTicks <= 0) {
			reqOverheadTicks = 200.0 + self.getRandom().nextDouble() * 300.0; // 10-25 seconds
			data.putDouble("sentinel_required_overhead_ticks", reqOverheadTicks);
		}
		if (combatTicks >= reqOverheadTicks && !data.getBoolean("unlocked_overhead_combo")) {
			data.putBoolean("unlocked_overhead_combo", true);
			announceLearnedAbility(self);
		}

		double reqDropkickTicks = data.getDouble("sentinel_required_dropkick_ticks");
		if (reqDropkickTicks <= 0) {
			reqDropkickTicks = 200.0 + self.getRandom().nextDouble() * 300.0; // 10-25 seconds
			data.putDouble("sentinel_required_dropkick_ticks", reqDropkickTicks);
		}
		if (combatTicks >= reqDropkickTicks && !data.getBoolean("unlocked_dropkick_combo")) {
			data.putBoolean("unlocked_dropkick_combo", true);
			announceLearnedAbility(self);
		}
	}

	private static void announceLearnedAbility(LivingEntity self) {
		playHostileSound(self.level(), self.getX(), self.getY(), self.getZ(), "entity.warden.heartbeat", 1.2F, 1.4F);
	}

	private static void executeGrappleSiphon(LevelAccessor world, Entity self, Entity target, int ticksLeft) {
		if (!(world instanceof ServerLevel level) || !(self instanceof LivingEntity rawSelf)) return;

		// Forcibly pull mutant/player targets into siphon range
		Vec3 vectorToSelf = self.position().add(0, 1.0, 0).subtract(target.position()).normalize();
		double distance = target.position().distanceTo(self.position());

		if (distance > 2.0) {
			// Magnetic vacuum pull (reduced particles)
			target.setDeltaMovement(vectorToSelf.x * 0.82, 0.15, vectorToSelf.z * 0.82);
			level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.2, 0.2, 0.2, 0.1);
		} else {
			// Fast extraction grapple!
			target.setDeltaMovement(0, -0.05, 0); // Pin down completely
			if (target instanceof LivingEntity liv) {
				liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4, false, false));
				liv.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 1, false, false));
			}

			if (ticksLeft % 4 == 0) {
				target.hurt(new DamageSource(level.holderOrThrow(DamageTypes.GENERIC_KILL)), 4.0F); // Direct biological drain
				rawSelf.setHealth(Math.min(rawSelf.getMaxHealth(), rawSelf.getHealth() + 4.5F));

				level.sendParticles(ParticleTypes.SWEEP_ATTACK, self.getX(), self.getY() + 1.2, self.getZ(), 1, 0.3, 0.3, 0.3, 0.0);
				level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.1, 0.1, 0.1, 0.1);

				// Quieter, muted audio feedback for mechanical siphon
				playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.attack_impact", 0.7F, 0.85F);
				if (self instanceof LivingEntity ls) ls.swing(InteractionHand.MAIN_HAND, true);
			}
		}
	}

	private static void executeTelekinesis(LevelAccessor world, Entity self, Entity target, int ticksLeft) {
		if (!(world instanceof ServerLevel level) || target == null) return;

		// Suspend target in midair (no particles or visual clutter as requested)
		Vec3 hoverPos = self.position().add(0, 3.5, 0);
		Vec3 diff = hoverPos.subtract(target.position());
		double liftStrength = 0.22;
		target.setDeltaMovement(diff.x * liftStrength, 0.15, diff.z * liftStrength);
		target.hasImpulse = true;

		if (target instanceof LivingEntity liv) {
			liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 3, false, false));
		}

		// Throw or slam target on complete
		if (ticksLeft <= 0) {
			if (Math.random() < 0.5) {
				// Strictly stop their existing movements completely before slamming down!
				target.setDeltaMovement(Vec3.ZERO);
				target.hasImpulse = true;
				// Slam down forcefully
				target.setDeltaMovement(0, -2.4, 0);
				target.hasImpulse = true;
				target.hurt(new DamageSource(level.holderOrThrow(DamageTypes.FLY_INTO_WALL)), 12.0F);
				playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.iron_golem.damage", 1.0F, 0.65F);
			} else {
				// Slam/throw horizontally and up
				Vec3 throwDir = target.position().subtract(self.position()).normalize();
				double ty = Math.max(throwDir.y, 0.2);
				target.setDeltaMovement(throwDir.x * 2.0, ty * 1.5 + 0.3, throwDir.z * 2.0);
				target.hurt(new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), 8.0F);
				playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.player.attack.sweep", 1.0F, 0.6F);
			}
		}
	}

	private static void executeSentinelPunch(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level)) return;
		LivingEntity ls = (self instanceof LivingEntity) ? (LivingEntity) self : null;

		// Alternate hand swings - always use MAIN_HAND since OFF_HAND swings do not render for custom mob models
		if (ls != null) {
			boolean lastHandLeft = self.getPersistentData().getBoolean("sentinel_punch_hand_toggle");
			ls.swing(InteractionHand.MAIN_HAND, true);
			self.getPersistentData().putBoolean("sentinel_punch_hand_toggle", !lastHandLeft);
		}

		// Initial base damage
		double basePunchDmg = 8.0;

		// Select a high-impact combat combo if the target is a LivingEntity and abilities are unlocked!
		boolean unlockedSonicBoom = self.getPersistentData().getBoolean("unlocked_sonic_boom");
		boolean unlockedSolar = self.getPersistentData().getBoolean("unlocked_solar_beam");
		boolean unlockedCryo = self.getPersistentData().getBoolean("unlocked_cryo_beam");
		boolean unlockedGrapple = self.getPersistentData().getBoolean("unlocked_grapple");
		boolean unlockedTK = self.getPersistentData().getBoolean("unlocked_telekinesis");
		boolean unlockedTP = self.getPersistentData().getBoolean("unlocked_teleportation");
		boolean unlockedOverhead = self.getPersistentData().getBoolean("unlocked_overhead_combo");
		boolean unlockedDropkick = self.getPersistentData().getBoolean("unlocked_dropkick_combo");

		int selectCombo = 0; // 0 = none, 1 = airborne, 2 = grapple-siphon, 3 = tk-slam, 4 = nanite-tp-blitz, 5 = seismic-slam, 6 = thermal-shock, 7 = blink-storm
		if (target instanceof LivingEntity && Math.random() < (self.getPersistentData().getBoolean("sentinel_totem_active") ? 0.85 : 0.28)) { //combo chance
			java.util.List<Integer> availableCombos = new java.util.ArrayList<>();
			if (unlockedSonicBoom || unlockedSolar || unlockedCryo) availableCombos.add(1);
			if (unlockedGrapple) availableCombos.add(2);
			if (unlockedTK) availableCombos.add(3);
			if (unlockedTP) availableCombos.add(4);
			if (unlockedTK) availableCombos.add(5);
			if (unlockedSolar || unlockedCryo) availableCombos.add(6);
			if (unlockedTP) availableCombos.add(7);
			if (unlockedTP && unlockedOverhead) availableCombos.add(8);
			if (unlockedTP) availableCombos.add(9);
			if (unlockedTP && unlockedDropkick) availableCombos.add(10);
			if (unlockedTP) availableCombos.add(11);

			if (!availableCombos.isEmpty()) {
				selectCombo = availableCombos.get((int) (Math.random() * availableCombos.size()));
				self.getPersistentData().putDouble("sentinel_combo_active_ticks", 40);
			}
		}

		// Check target attributes/properties
		boolean targetHasArmor = false;
		boolean targetHasMace = false;
		double targetDmg = 2.0;

		if (target instanceof LivingEntity targetLiving) {
			if (targetLiving.getArmorValue() > 0) {
				targetHasArmor = true;
			}
			var mainHand = targetLiving.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
			var offHand = targetLiving.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND);
			String mainHandName = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();
			String offHandName = BuiltInRegistries.ITEM.getKey(offHand.getItem()).toString();
			if (mainHandName.contains("mace") || offHandName.contains("mace")) {
				targetHasMace = true;
			}

			var attr = targetLiving.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
			if (attr != null) {
				targetDmg = attr.getValue();
			}
		}

		// Adaptation 2: Copy High Damage eventually
		double adaptedPunchDmg = self.getPersistentData().getDouble("adapted_punch_damage");
		if (adaptedPunchDmg < basePunchDmg) {
			adaptedPunchDmg = basePunchDmg;
		}

		if (targetDmg > adaptedPunchDmg) {
			// Adapt/copy target damage eventually
			adaptedPunchDmg += (targetDmg - adaptedPunchDmg) * 0.45;
			self.getPersistentData().putDouble("adapted_punch_damage", adaptedPunchDmg);
			// Spawn subtle adaptation enchantment particles
			level.sendParticles(ParticleTypes.ENCHANT, self.getX(), self.getY() + 1.2, self.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
		}

		double finalDamage = adaptedPunchDmg;

		// Adaptation 1: Mace / Armor-Piercing crusher
		if (targetHasMace && target instanceof LivingEntity targetLiving) {
			// Deals dynamic armor damage: significantly degrade armor slots
			for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
				if (slot.isArmor()) {
					var stack = targetLiving.getItemBySlot(slot);
					if (!stack.isEmpty()) {
						stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 25));
					}
				}
			}
			// Deal extra raw piercing damage based on target's armor!
			finalDamage += targetLiving.getArmorValue() * 0.65;
		}

		// Apply Damage
		if (ls != null) {
			target.hurt(ls.damageSources().mobAttack(ls), (float) finalDamage);
		} else {
			target.hurt(new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) finalDamage);
		}

		// Effects and Knockback
		Vec3 sweepPush = target.position().subtract(self.position()).normalize();
		double knockbackStrength = 1.05;

		// Adaptation 2: Proportional more knockback based on adapted/copied damage
		if (adaptedPunchDmg > basePunchDmg) {
			knockbackStrength += (adaptedPunchDmg - basePunchDmg) * 0.045;
		}

		// Apply Knockback or specialized combos!
		if (selectCombo == 1 && target instanceof LivingEntity liv) {
			// Launch target high into the air! (Airborne Launch Combo)
			liv.setDeltaMovement(sweepPush.x * 0.3, 1.35, sweepPush.z * 0.3);
			liv.hasImpulse = true;

			// Spawn gusty wind launch particles
			level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.5, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.wind_charge.throw", 1.2F, 0.75F);

			// Trigger immediate follow-up attack combo (Laser or Sonic Boom)
			double rng = Math.random();
			if (rng < 0.45 && unlockedSonicBoom) {
				self.getPersistentData().putDouble("sentinel_sonic_ticks", 70);
				playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.sonic_charge", 1.4F, 0.4F);
			} else if (unlockedSolar || unlockedCryo) {
				java.util.List<String> lasers = new java.util.ArrayList<>();
				if (unlockedSolar) lasers.add("solar");
				if (unlockedCryo) lasers.add("cryo");
				if (!lasers.isEmpty()) {
					String chosen = lasers.get((int) (Math.random() * lasers.size()));
					if ("solar".equals(chosen)) {
						self.getPersistentData().putDouble("sentinel_solar_charge_ticks", 1);
					} else {
						self.getPersistentData().putDouble("sentinel_cryo_charge_ticks", 1);
					}
				}
			}
		} else if (selectCombo == 2 && target instanceof LivingEntity liv) {
			// Siphon combo! Drag close and initiate grapple
			self.getPersistentData().putDouble(K_GRAPPLE_CD, 20); // quick reset
			self.getPersistentData().putDouble(K_GRAPPLE_TICKS, 32); // 32 ticks of grapple
			Vec3 vectorToSelf = self.position().subtract(target.position()).normalize();
			liv.setDeltaMovement(vectorToSelf.x * 1.25, 0.22, vectorToSelf.z * 1.25); // Pull target close!
			liv.hasImpulse = true;
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.attack_impact", 1.2F, 0.85F);
			level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
		} else if (selectCombo == 3 && target instanceof LivingEntity liv) {
			// Telekinesis combo! Float target instantly into midair suspension
			self.getPersistentData().putDouble(K_TK_CD, 20); // quick reset
			self.getPersistentData().putDouble(K_TK_TICKS, 25); // 25 ticks of telekinesis suspension
			liv.setDeltaMovement(0.0, 0.85, 0.0);
			liv.hasImpulse = true;
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.sonic_boom", 0.9F, 1.4F);
			level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.2, target.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
		} else if (selectCombo == 4 && target instanceof LivingEntity liv) {
			// Elite Nanite blink blitz! Teleport behind target and strike with 1.4x heavier impact thrust
			double angle = target.getYRot() * (Math.PI / 180.0);
			double spawnX = target.getX() - Math.sin(angle) * 1.5;
			double spawnZ = target.getZ() + Math.cos(angle) * 1.5;
			double spawnY = target.getY();

			level.sendParticles(ParticleTypes.PORTAL, self.getX(), self.getY() + 1.1, self.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
			teleportEntity(self, spawnX, spawnY, spawnZ);
			level.sendParticles(ParticleTypes.PORTAL, spawnX, spawnY + 1.1, spawnZ, 8, 0.2, 0.2, 0.2, 0.05);
			playHostileSound(level, spawnX, spawnY, spawnZ, "item.chorus_fruit.teleport", 1.1F, 0.95F);

			// Heavier strike damage & backward push
			liv.hurt(ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 1.4));
			liv.setDeltaMovement(liv.getDeltaMovement().add(self.getViewVector(1.0F).scale(1.8)));
			liv.hasImpulse = true;
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.player.attack.sweep", 1.2F, 0.65F);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, self.getX(), self.getY() + 1.2, self.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
		} else if (selectCombo == 5 && target instanceof LivingEntity liv) {
			// Combo 5: Seismic Ground Slam Combo (requires unlockedTK)
			liv.setDeltaMovement(0.0, 0.45, 0.0);
			liv.hasImpulse = true;
			liv.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, false));
			liv.hurt(ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 1.5));
			liv.setDeltaMovement(sweepPush.x * 0.2, -1.8, sweepPush.z * 0.2);
			liv.hasImpulse = true;

			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.iron_golem.damage", 1.4F, 0.5F);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.iron_golem.death", 0.9F, 0.6F);
			level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(), 5, 0.5, 0.1, 0.5, 0.1);
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(), target.getZ(), 10, 0.4, 0.4, 0.4, 0.2);
		} else if (selectCombo == 6 && target instanceof LivingEntity liv) {
			// Combo 6: Thermal-Cryo Shock Combo (requires unlockedSolar or unlockedCryo)
			liv.setRemainingFireTicks(80);
			liv.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));
			liv.hurt(ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 1.35));

			level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
			level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "item.firecharge.use", 1.2F, 0.8F);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "block.powder_snow.break", 1.2F, 1.1F);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.generic.explode", 0.8F, 1.4F);
		} else if (selectCombo == 7 && target instanceof LivingEntity liv) {
			// Combo 7: Chrono Blink Storm Sequence (requires unlockedTP)
			for (int i = 0; i < 3; i++) {
				double oX = (Math.random() - 0.5) * 3.0;
				double oZ = (Math.random() - 0.5) * 3.0;
				level.sendParticles(ParticleTypes.PORTAL, target.getX() + oX, target.getY() + 1.0, target.getZ() + oZ, 4, 0.1, 0.1, 0.1, 0.05);
			}
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "item.chorus_fruit.teleport", 1.3F, 1.2F);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.player.attack.sweep", 1.4F, 1.5F);

			liv.hurt(ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 1.6));
			liv.setDeltaMovement(sweepPush.x * 1.6, 0.45, sweepPush.z * 1.6);
			liv.hasImpulse = true;

			level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.08);
		} else if (selectCombo == 8 && target instanceof LivingEntity liv) {
			liv.setDeltaMovement(0.0, 2.8, 0.0);
			liv.hasImpulse = true;
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.iron_golem.attack", 1.5F, 0.6F);
			level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.5, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
			self.getPersistentData().putDouble("sentinel_sky_warp_slam_ticks", 18);
		} else if (selectCombo == 9 && target instanceof LivingEntity liv) {
			for (int hit = 1; hit <= 3; hit++) {
				double targetYRot = target.getYRot() * (Math.PI / 180.0);
				double tx = target.getX() - Math.sin(targetYRot) * 1.6;
				double tz = target.getZ() + Math.cos(targetYRot) * 1.6;
				double ty = target.getY();
				level.sendParticles(ParticleTypes.PORTAL, self.getX(), self.getY() + 0.5, self.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
				teleportEntity(self, tx, ty, tz);
				level.sendParticles(ParticleTypes.PORTAL, tx, ty + 0.5, tz, 6, 0.2, 0.2, 0.2, 0.05);
				playHostileSound(level, tx, ty, tz, "item.chorus_fruit.teleport", 1.1F, 1.0F + hit * 0.1F);
				playHostileSound(level, tx, ty, tz, "entity.player.attack.knockback", 1.3F, 0.7F + hit * 0.1F);
				target.hurt(ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 1.2));
				Vec3 pushVec = target.position().subtract(self.position()).normalize();
				target.setDeltaMovement(pushVec.x * 1.9, 0.35, pushVec.z * 1.9);
				target.hasImpulse = true;
			}
		} else if (selectCombo == 10 && target instanceof LivingEntity liv) {
			self.setDeltaMovement(0.0, 1.95, 0.0);
			self.hasImpulse = true;
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.attack_impact", 1.5F, 0.5F);
			level.sendParticles(ParticleTypes.CLOUD, self.getX(), self.getY() + 0.5, self.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
			self.getPersistentData().putDouble("sentinel_judgment_ticks", 15);
		} else if (selectCombo == 11 && target instanceof LivingEntity liv) {
			double startX = self.getX();
			double startY = self.getY();
			double startZ = self.getZ();

			double targetYRot = liv.getYRot() * (Math.PI / 180.0);
			double tx = liv.getX() - Math.sin(targetYRot) * 1.2;
			double tz = liv.getZ() + Math.cos(targetYRot) * 1.2;
			double ty = liv.getY();

			teleportEntity(self, tx, ty, tz);

			Vec3 start = new Vec3(startX, startY + self.getBbHeight() * 0.5, startZ);
			Vec3 end = new Vec3(tx, ty + liv.getBbHeight() * 0.5, tz);
			double distance = start.distanceTo(end);

			int steps = (int) Math.ceil(distance * 1.5);
			for (int i = 0; i <= steps; i++) {
				double pct = (double) i / steps;
				double px = startX + (tx - startX) * pct;
				double py = startY + (ty - startY) * pct;
				double pz = startZ + (tz - startZ) * pct;

				BlockPos centerPos = BlockPos.containing(px, py + 1.0, pz);
				for (BlockPos bp : BlockPos.betweenClosed(centerPos.offset(-1, -1, -1), centerPos.offset(1, 2, 1))) {
					BlockState state = level.getBlockState(bp);
					float hardness = state.getDestroySpeed(level, bp);
					if (!state.isAir() && hardness >= 0 && hardness < 40f) {
						level.destroyBlock(bp, false);
					}
				}

				if (i % 2 == 0) {
					level.sendParticles(ParticleTypes.EXPLOSION, px, py + 1.0, pz, 1, 0.2, 0.2, 0.2, 0.0);
				}
				level.sendParticles(ParticleTypes.CRIT, px, py + 1.0, pz, 3, 0.1, 0.1, 0.1, 0.1);
			}

			playHostileSound(level, tx, ty, tz, "entity.generic.explode", 1.4F, 1.1F);
			playHostileSound(level, tx, ty, tz, "entity.warden.attack_impact", 1.6F, 0.6F);

			liv.hurt(ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 2.2));
			liv.setDeltaMovement(sweepPush.x * 3.5, 0.95, sweepPush.z * 3.5);
			liv.hasImpulse = true;
		} else {
			// Normal knockback
			target.setDeltaMovement(sweepPush.x * knockbackStrength, 0.26, sweepPush.z * knockbackStrength);
			target.hasImpulse = true;
		}

		// Adaptation 3: Thermal Igniting Punch (if target isn't already heavily burning)
		if (selectCombo != 1 && target instanceof LivingEntity liv && !liv.isOnFire() && Math.random() < 0.4) {
			liv.setRemainingFireTicks(100);
			// Plays the fire sound upon contact (perfect selection & timing)
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "item.firecharge.use", 1.1F, 0.9F);
			level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
		} else {
			// Play appropriate contact/mace sounds based on target armor and adaptations
			if (targetHasArmor) {
				// Only use mace sounds if target is wearing armor. Also pitch down the mace sounds.
				playHostileSound(level, target.getX(), target.getY(), target.getZ(), "item.mace.heavy_smash", 1.2F, 0.65F);
			} else if (adaptedPunchDmg > basePunchDmg) {
				// Use mace kickback/impact sounds for high-yielding kinetic punch adaptations, normal pitch
				playHostileSound(level, target.getX(), target.getY(), target.getZ(), "item.mace.knockback", 1.1F, 0.95F);
			} else {
				// Normal standard punch contact sound
				playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.iron_golem.damage", 1.0F, 0.85F);
			}

			// Subtle impact particles
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.2, target.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
		}
	}

	private static boolean isWither(Entity entity) {
		if (entity == null) return false;
		String key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
		return "minecraft:wither".equals(key);
	}

	private static void sendActionBarToNearbyPlayers(LevelAccessor world, Vec3 pos, double range, String text) {
		if (world instanceof ServerLevel s) {
			s.players().stream()
				.filter(p -> p.position().distanceToSqr(pos) <= range * range)
				.forEach(p -> p.displayClientMessage(Component.literal(text), true));
		}
	}

	private static void lockLookAtTarget(Entity entity, Entity target) {
		if (entity instanceof Mob mob) {
			boolean isChanneling = isChannelingAbility(entity);

			if (isChanneling) {
				mob.getLookControl().setLookAt(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 45.0F, 45.0F);

				double dx = target.getX() - mob.getX();
				double dy = (target.getY() + target.getBbHeight() * 0.5) - mob.getEyeY();
				double dz = target.getZ() - mob.getZ();
				double dh = Math.sqrt(dx * dx + dz * dz);
				float targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
				float targetXRot = (float) (-(Mth.atan2(dy, dh) * (180F / Math.PI)));

				float curY = mob.getYRot();
				float curX = mob.getXRot();
				float lerpedYRot = Mth.rotLerp(0.32F, curY, targetYRot);
				float lerpedXRot = Mth.rotLerp(0.32F, curX, targetXRot);

				mob.setYRot(lerpedYRot);
				mob.setXRot(lerpedXRot);
				mob.setYHeadRot(lerpedYRot);
				mob.yBodyRot = lerpedYRot;
			} else {
				mob.getLookControl().setLookAt(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 60.0F, 60.0F);
				double dx = target.getX() - mob.getX();
				double dz = target.getZ() - mob.getZ();
				if (dx * dx + dz * dz > 0.001) {
					float targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
					float currentYRot = mob.getYRot();
					float lerpedYRot = Mth.rotLerp(0.3F, currentYRot, targetYRot);
					mob.setYRot(lerpedYRot);
					mob.setYHeadRot(lerpedYRot);
					mob.yBodyRot = lerpedYRot;
				}
			}
		} else {
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ()));
		}
	}

	private static Entity acquireTarget(LevelAccessor world, Entity self, double x, double y, double z) {
		double solarFire = self.getPersistentData().getDouble("sentinel_solar_fire_ticks");
		double solarCharge = self.getPersistentData().getDouble("sentinel_solar_charge_ticks");
		double cryoFire = self.getPersistentData().getDouble("sentinel_cryo_fire_ticks");
		double cryoCharge = self.getPersistentData().getDouble("sentinel_cryo_charge_ticks");
		boolean ignoreLOS = solarFire > 0 || solarCharge > 0 || cryoFire > 0 || cryoCharge > 0;

		if (self instanceof Mob mob && ignoreLOS) {
			int targetId = self.getPersistentData().getInt("sentinel_laser_target_id");
			if (world instanceof ServerLevel level) {
				Entity laserTarget = level.getEntity(targetId);
				if (laserTarget instanceof LivingEntity && laserTarget.isAlive() && isValidTarget(laserTarget, self, true)) {
					mob.setTarget((LivingEntity) laserTarget);
					return laserTarget;
				}
			}
		}

		// Prevent body and head twitching/jittering when surrounded by multiple hostiles by locking target focus
		if (world instanceof ServerLevel level && self instanceof Mob mob) {
			int lockedId = self.getPersistentData().getInt("sentinel_locked_target_id");
			int lockTicks = self.getPersistentData().getInt("sentinel_target_lock_ticks");
			if (lockTicks > 0) {
				Entity lockedTarget = level.getEntity(lockedId);
				if (lockedTarget instanceof LivingEntity && lockedTarget.isAlive() && isValidTarget(lockedTarget, self, ignoreLOS)) {
					self.getPersistentData().putInt("sentinel_target_lock_ticks", lockTicks - 1);
					mob.setTarget((LivingEntity) lockedTarget);
					return lockedTarget;
				}
			}
		}

		Entity target = (self instanceof Mob mob) ? mob.getTarget() : null;
		if (!isValidTarget(target, self, ignoreLOS || target != null)) {
			target = findEntityInWorldRange(world, LivingEntity.class, x, y, z, TARGET_RANGE, self);
			if (!isValidTarget(target, self, ignoreLOS)) target = null;
		}

		if (target != null && self instanceof Mob mob) {
			self.getPersistentData().putInt("sentinel_locked_target_id", target.getId());
			self.getPersistentData().putInt("sentinel_target_lock_ticks", 60); // lock target focus for 3 seconds
			mob.setTarget((LivingEntity) target);
		} else if (target == null && self instanceof Mob mob) {
			mob.setTarget(null);
		}
		return target;
	}

	private static boolean isValidTarget(Entity target, Entity self) {
		return isValidTarget(target, self, false);
	}

	private static boolean isValidTarget(Entity target, Entity self, boolean ignoreLineOfSight) {
		if (target == null || !target.isAlive() || target == self) return false;
		if (self.distanceTo(target) > TARGET_RANGE) return false;
		if (target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND)))) return false;
		if (!ignoreLineOfSight && self instanceof LivingEntity ls && !ls.hasLineOfSight(target)) return false;

		// Retaliation check: whoever attacks it (except woodbound which was checked above)
		if (self instanceof LivingEntity ls) {
			if (ls.getLastHurtByMob() == target) {
				return true;
			}
			if (target instanceof Mob mob && mob.getTarget() == self) {
				return true;
			}
		}

		if (target instanceof RotEntity || target instanceof Villager || target instanceof AmbientCreature || target instanceof Animal || target instanceof Slime) return false;

		if (target instanceof Player p) {
			if (p instanceof ServerPlayer sp) {
				if (sp.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SURVIVAL) return false;
			} else {
				if (p.isCreative() || p.isSpectator()) return false;
			}
			return true; // Automatically attack players since its made for overpowered players
		}

		if (target instanceof Monster) {
			return true; // Automatically attack hostiles (monsters)
		}

		if (target instanceof Mob) {
			return true; // Support hostile modded mobs that do not extend Monster but are selected by AI goals
		}

		return false;
	}

	private static void handleAdaptationScaling(Entity entity, boolean inCombat) {
		if (!(entity instanceof LivingEntity living)) return;
		double combatTicks = entity.getPersistentData().getDouble("sentinel_combat_ticks");
		if (inCombat) {
			combatTicks = Math.min(1000.0, combatTicks + 1.0);

			// Realistically learn teleportation if fighting End entity, Player utilizing Ender Pearl, seeing dropped ender pearl, or in the End dimension
			boolean learnTP = false;
			if (entity.level().dimension() == net.minecraft.world.level.Level.END) {
				learnTP = true;
			} else {
				// Scan for near ender pearls (projectile)
				List<net.minecraft.world.entity.projectile.Projectile> projectiles = entity.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, AABB.ofSize(entity.position(), 48.0, 48.0, 48.0));
				for (net.minecraft.world.entity.projectile.Projectile proj : projectiles) {
					if (BuiltInRegistries.ENTITY_TYPE.getKey(proj.getType()).toString().contains("ender_pearl")) {
						learnTP = true;
						break;
					}
				}

				// Scan for dropped ender pearls (ItemEntity)
				if (!learnTP) {
					List<net.minecraft.world.entity.item.ItemEntity> items = entity.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, AABB.ofSize(entity.position(), 48.0, 48.0, 48.0));
					for (net.minecraft.world.entity.item.ItemEntity itemEnt : items) {
						if (itemEnt.getItem() != null && BuiltInRegistries.ITEM.getKey(itemEnt.getItem().getItem()).toString().contains("ender_pearl")) {
							learnTP = true;
							break;
						}
					}
				}

				if (!learnTP) {
					Mob mob = (entity instanceof Mob m) ? m : null;
					LivingEntity target = (mob != null) ? mob.getTarget() : null;
					if (target != null) {
						String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
						boolean isEndEntity = targetId.contains("enderman") || targetId.contains("endermite") || targetId.contains("shulker") || targetId.contains("ender_dragon");
						if (isEndEntity) {
							learnTP = true;
						} else if (target instanceof Player playerCheck) {
							if (BuiltInRegistries.ITEM.getKey(playerCheck.getMainHandItem().getItem()).toString().contains("ender_pearl") ||
								BuiltInRegistries.ITEM.getKey(playerCheck.getOffhandItem().getItem()).toString().contains("ender_pearl")) {
								learnTP = true;
							}
						}
					}
				}
			}

			if (learnTP) {
				double progress = entity.getPersistentData().getDouble("sentinel_teleport_learning_progress");
				entity.getPersistentData().putDouble("sentinel_teleport_learning_progress", progress + 1.0);
			}
		} else {
			combatTicks = Math.max(0.0, combatTicks - 0.5);
		}
		entity.getPersistentData().putDouble("sentinel_combat_ticks", combatTicks);

		// Milestones database check
		checkLearnedMilestone(living, combatTicks, inCombat);

		// Adaptive Wolverine-style regeneration
		if (living.getPersistentData().getBoolean("unlocked_regen") && living.getHealth() < living.getMaxHealth()) {
			double regenTimer = entity.getPersistentData().getDouble("sentinel_regen_timer") + 1.0;
			double combatFactor = 1.0 + (combatTicks / 1000.0) * 2.0; // Up to 3.0x healing efficiency
			double healthRatio = (double) living.getHealth() / (double) living.getMaxHealth();
			double lowHealthFactor = 1.0;
			if (healthRatio < 0.25) {
				lowHealthFactor = 3.5; // Trigger extreme survival burst at critical damage!
			} else if (healthRatio < 0.50) {
				lowHealthFactor = 2.0; // Double the tick rate below half health
			}

			double requiredTicks = 20.0 / (combatFactor * lowHealthFactor);
			if (requiredTicks < 1.0) requiredTicks = 1.0;

			if (regenTimer >= requiredTicks) {
				regenTimer = 0.0;
				float healAmount = (float) (2.0 + (living.getMaxHealth() * 0.015));
				living.heal(healAmount);
			}
			entity.getPersistentData().putDouble("sentinel_regen_timer", regenTimer);
		}

		// Decay the recent damage pool for high-DPS adaptive resistance
		double recentDamage = entity.getPersistentData().getDouble("sentinel_recent_damage");
		recentDamage = Math.max(0.0, recentDamage * 0.95);
		entity.getPersistentData().putDouble("sentinel_recent_damage", recentDamage);

		// Apply adaptive resistance level depending on recent DPS
		if (recentDamage > 35.0) {
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, false, false));
		} else if (recentDamage > 20.0) {
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false));
		} else if (recentDamage > 8.0) {
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
		}

		// Speed Adaptation (Starts slow with 45% penalty. If teleport is unlocked, adapt to normal pacing. Otherwise, learn to run realistically!)
		if (living.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)) {
			var attr = living.getAttribute(Attributes.MOVEMENT_SPEED);
			attr.removeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_speed"));
			double speedBonus;
			boolean tpUnlocked = entity.getPersistentData().getBoolean("unlocked_teleportation");
			if (tpUnlocked) {
				speedBonus = -0.45 + (combatTicks / 1000.0) * 0.55; // default up to +10%
			} else {
				// Learning how to run as a fallback, capping at a modest 25% sprint speed so it remains completely realistic
				speedBonus = -0.45 + (combatTicks / 600.0) * 0.70;
				if (speedBonus > 0.25) {
					speedBonus = 0.25;
				}
			}
			if (entity.getPersistentData().getBoolean("sentinel_totem_active")) {
				speedBonus += 0.35;
			}
			attr.addTransientModifier(new AttributeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_speed"), speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

			// Slow down speed if laser charging or laser firing is active!
			attr.removeModifier(ResourceLocation.parse("the_backwoods:sentinel_laser_slowdown"));
			double solarCharge = entity.getPersistentData().getDouble("sentinel_solar_charge_ticks");
			double solarFire = entity.getPersistentData().getDouble("sentinel_solar_fire_ticks");
			if (solarCharge > 0 || solarFire > 0) {
				attr.addTransientModifier(new AttributeModifier(ResourceLocation.parse("the_backwoods:sentinel_laser_slowdown"), -0.85, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}

		// Damage Adaptation (Starts with 20% penalty, eventually gains 20% damage boost after 1000 ticks)
		if (living.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
			var attr = living.getAttribute(Attributes.ATTACK_DAMAGE);
			attr.removeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_damage"));
			double damageBonus = -0.20 + (combatTicks / 1000.0) * 0.40;
			if (entity.getPersistentData().getBoolean("sentinel_totem_active")) {
				damageBonus += 0.50; // Extra 50% permanent attack damage boost!
			}
			attr.addTransientModifier(new AttributeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_damage"), damageBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	private static void fireSuperchargedSonicBoom(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level)) return;
		self.getPersistentData().putDouble("sentinel_sonic_ticks", 70); // Starts the 3.5s state machine
		playHostileSound(world, self.getX(), self.getY(), self.getZ(), "entity.warden.sonic_charge", 1.4F, 0.4F);
	}

	private static void fireSuperchargedSonicBoomEffectAndDamage(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level)) return;
		playHostileSound(world, self.getX(), self.getY(), self.getZ(), "entity.warden.sonic_boom", 1.5F, 0.3F);

		Vec3 lookVec = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(self.position().add(0, self.getBbHeight() * SONIC_BOOM_TORSO_Y_FACTOR, 0)).normalize();
		Vec3 eyePos = self.position().add(0, self.getBbHeight() * SONIC_BOOM_TORSO_Y_FACTOR, 0);
		double range = SONIC_BOOM_RANGE;

		for (double step = 1.0; step <= range; step += 1.0) {
			Vec3 beamPoint = eyePos.add(lookVec.scale(step));

			// Greatly reduced sonic/kinetic particle bursts
			if (step % 2 == 0) {
				level.sendParticles(ParticleTypes.SONIC_BOOM, beamPoint.x, beamPoint.y, beamPoint.z, 1, 0.1, 0.1, 0.1, 0.0);
			}
			level.sendParticles(ParticleTypes.CRIT, beamPoint.x, beamPoint.y, beamPoint.z, 1, 0.2, 0.2, 0.2, 0.1);

			// Devastate non-woodbound entities nearby
			List<Entity> swept = level.getEntitiesOfClass(Entity.class, new AABB(beamPoint, beamPoint).inflate(2.4), e -> e != self && (e instanceof LivingEntity));
			for (Entity targetVictim : swept) {
				if (targetVictim.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND)))) continue;

				targetVictim.hurt(new DamageSource(level.holderOrThrow(DamageTypes.SONIC_BOOM)), 32.0F);
				
				Vec3 push = targetVictim.position().subtract(self.position()).normalize();
				targetVictim.setDeltaMovement(push.x * 2.5, 0.6, push.z * 2.5);
			}
		}
	}

	private static void tryPredictiveDodge(LevelAccessor world, Entity self, Entity target, double dist) {
		if (self.getPersistentData().getDouble(K_TP_DODGE_CD) > 0) return;
		if (!(target instanceof LivingEntity tl) || dist > DODGE_TRIGGER_DIST) return;

		if (!self.getPersistentData().getBoolean("unlocked_teleportation")) return;

		double combatTicks = self.getPersistentData().getDouble("sentinel_combat_ticks");
		if (combatTicks < 100) return;

		boolean likelySwingNow = tl.swinging;
		boolean likelySwingSoon = false;
		if (target instanceof Player p) likelySwingSoon = p.getAttackStrengthScale(0.5f) > 0.9f && dist < 4.6;

		if (!(likelySwingNow || likelySwingSoon)) return;
		if (Math.random() > DODGE_SWING_CHANCE) return;

		Vec3 look = target.getLookAngle().normalize();
		Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

		double side = Mth.nextDouble(RandomSource.create(), TELEPORT_SIDE_MIN, TELEPORT_SIDE_MAX);
		if (RandomSource.create().nextBoolean()) side *= -1;

		double tx = target.getX() + right.x * side - look.x * 0.8;
		double tz = target.getZ() + right.z * side - look.z * 0.8;

		trySafeTeleportToGround(world, self, tx, tz, "entity.warden.attack_impact", 1.5f, 0.85f, K_TP_DODGE_CD, TP_DODGE_CD);
	}

	private static void tryFlankTeleport(LevelAccessor world, Entity self, Entity target, double dist) {
		if (self.getPersistentData().getDouble(K_TP_FLANK_CD) > 0) return;

		if (!self.getPersistentData().getBoolean("unlocked_teleportation")) return;

		double combatTicks = self.getPersistentData().getDouble("sentinel_combat_ticks");
		if (combatTicks < 80) return;

		boolean noLos = true;
		if (self instanceof LivingEntity ls) noLos = !ls.hasLineOfSight(target);

		boolean shouldFlank = (dist > TELEPORT_MIN_GAP || noLos || self.getPersistentData().getBoolean("sentinel_totem_active")) && Math.random() < (self.getPersistentData().getBoolean("sentinel_totem_active") ? 0.28 : 0.18);
		if (!shouldFlank) return;

		Vec3 look = target.getLookAngle().normalize();
		Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
		double side = Mth.nextDouble(RandomSource.create(), TELEPORT_SIDE_MIN, TELEPORT_SIDE_MAX);
		if (RandomSource.create().nextBoolean()) side *= -1;

		double tx = target.getX() - look.x * TELEPORT_BACK_OFFSET + right.x * side;
		double tz = target.getZ() - look.z * TELEPORT_BACK_OFFSET + right.z * side;

		trySafeTeleportToGround(world, self, tx, tz, "entity.warden.attack_impact", 1.8f, 0.55f, K_TP_FLANK_CD, TP_FLANK_CD);
	}

	@SubscribeEvent
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity == null) return;

		if (entity instanceof RotEntity) {
			// Track recent damage for high-DPS adaptive resistance
			double damageAmount = event.getAmount();
			double currentRecent = entity.getPersistentData().getDouble("sentinel_recent_damage");
			double newRecent = currentRecent + damageAmount;
			entity.getPersistentData().putDouble("sentinel_recent_damage", newRecent);

			// Track cumulative total damage taken for adaptation scaling
			double totalDamageTaken = entity.getPersistentData().getDouble("sentinel_total_damage_taken") + damageAmount;
			entity.getPersistentData().putDouble("sentinel_total_damage_taken", totalDamageTaken);

			// Slowly grow base knockback resistance as it keeps taking damage
			if (entity.getAttributes().hasAttribute(Attributes.KNOCKBACK_RESISTANCE)) {
				double baseKb = Math.min(1.0, totalDamageTaken * 0.003); // Grows up to 100% knockback resistance
				entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(baseKb);
			}

			// Slowly grow damage resistance as it keeps taking damage (ONLY learned from continuous damage)
			double continuousDmgMultiplier = 1.0 - Math.min(0.75, totalDamageTaken * 0.0015);
			event.setAmount((float) (event.getAmount() * continuousDmgMultiplier));

			// Dynamic adaptive resistance mitigation in real-time
			if (newRecent > 35.0) {
				event.setAmount(event.getAmount() * 0.20F); // 80% mitigation under extreme DPS
			} else if (newRecent > 20.0) {
				event.setAmount(event.getAmount() * 0.40F); // 60% mitigation under high DPS
			} else if (newRecent > 8.0) {
				event.setAmount(event.getAmount() * 0.65F); // 35% mitigation under moderate DPS
			}

			DamageSource source = event.getSource();
			Entity direct = source.getDirectEntity();

			if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
				entity.getPersistentData().putBoolean("taken_fire_damage", true);
			}
			if (source.is(net.minecraft.world.damagesource.DamageTypes.FREEZE)) {
				entity.getPersistentData().putBoolean("taken_freeze_damage", true);
			}

			if (direct instanceof Projectile || source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
				if (tryDodgeProjectile(entity, source)) {
					event.setCanceled(true);
				}
			}
		}

		// Swing arms when attacking any entity
		Entity attacker = event.getSource().getEntity();
		if (attacker instanceof RotEntity rot) {
			rot.swing(InteractionHand.MAIN_HAND, true);
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity instanceof RotEntity) {
			entity.getPersistentData().putDouble("sentinel_solar_fire_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_solar_charge_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_cryo_fire_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_cryo_charge_ticks", 0);
			stopHostileSound(entity.level(), entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 64.0);
		}
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof RotEntity) {
			entity.getPersistentData().putDouble("sentinel_solar_fire_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_solar_charge_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_cryo_fire_ticks", 0);
			entity.getPersistentData().putDouble("sentinel_cryo_charge_ticks", 0);
			stopHostileSound(entity.level(), entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 64.0);
		}
	}

	private static void playTeleportEffects(ServerLevel level, Entity entity, double x, double y, double z) {
		Entity target = (entity instanceof Mob mob) ? mob.getTarget() : null;
		net.minecraft.core.particles.ParticleOptions mainParticle = ParticleTypes.PORTAL;
		net.minecraft.core.particles.ParticleOptions secondaryParticle = ParticleTypes.CAMPFIRE_COSY_SMOKE;
		String sound = "entity.warden.attack_impact";
		float vol = 0.9f;
		float pitch = 1.15f;

		if (target != null) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
			if (targetId.contains("enderman") || targetId.contains("shulker")) {
				mainParticle = ParticleTypes.PORTAL;
				secondaryParticle = ParticleTypes.DRAGON_BREATH;
				sound = "entity.enderman.teleport";
				vol = 1.0f;
				pitch = 1.0f;
			} else if (targetId.contains("blaze") || targetId.contains("ghast") || targetId.contains("magma_cube") || target.level().dimension() == Level.NETHER) {
				mainParticle = ParticleTypes.FLAME;
				secondaryParticle = ParticleTypes.LAVA;
				sound = "item.firecharge.use";
				vol = 1.1f;
				pitch = 0.85f;
			} else if (targetId.contains("warden")) {
				mainParticle = ParticleTypes.SONIC_BOOM;
				secondaryParticle = ParticleTypes.CRIT;
				sound = "entity.warden.sonic_boom";
				vol = 0.8f;
				pitch = 1.4f;
			} else if (targetId.contains("wither")) {
				mainParticle = ParticleTypes.WITCH;
				secondaryParticle = ParticleTypes.SMOKE;
				sound = "entity.wither.shoot";
				vol = 0.9f;
				pitch = 0.75f;
			}
		}

		level.sendParticles(mainParticle, x, y + 1.0, z, 6, 0.2, 0.5, 0.2, 0.1);
		level.sendParticles(secondaryParticle, x, y + 1.1, z, 3, 0.15, 0.25, 0.15, 0.01);
		playHostileSound(level, x, y, z, sound, vol, pitch);
	}

	private static net.minecraft.core.particles.ParticleOptions getAdaptiveBeamParticle(Entity self) {
		Entity target = (self instanceof Mob mob) ? mob.getTarget() : null;
		if (target != null) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
			if (targetId.contains("blaze") || targetId.contains("ghast") || targetId.contains("magma_cube") || target.level().dimension() == Level.NETHER) {
				return ParticleTypes.LAVA;
			}
		}
		return ParticleTypes.FLAME;
	}

	private static boolean tryDodgeProjectile(Entity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return false;
		}

		if (!entity.getPersistentData().getBoolean("unlocked_teleportation")) {
			return false;
		}

		double combatTicks = entity.getPersistentData().getDouble("sentinel_combat_ticks");
		if (combatTicks < 100) {
			return false;
		}

		// Sentinel projectile dodging starts mediocre (30%) and adapts up to peak mastery (95%) as it downloads ballistic files
		double dodgeChance = 0.30 + (Math.min(1000.0, combatTicks) / 1000.0) * 0.65;
		if (entity.getRandom().nextDouble() > dodgeChance) {
			return false;
		}

		Entity direct = source.getDirectEntity();
		Vec3 sourcePos = direct != null ? direct.position() : source.getSourcePosition();
		if (sourcePos == null) {
			sourcePos = entity.position().add(1.0, 0.0, 1.0);
		}

		Vec3 away = entity.position().subtract(sourcePos);
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
		}

		horizontalAway = horizontalAway.normalize();
		Vec3 side = new Vec3(-horizontalAway.z, 0.0, horizontalAway.x).scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);

		for (int i = 0; i < 16; i++) {
			double distance = 4.5 + entity.getRandom().nextDouble() * 5.5;
			double lift = (entity.getRandom().nextDouble() - 0.3) * 2.0;
			Vec3 candidate = entity.position()
				.add(horizontalAway.scale(distance))
				.add(side.scale((entity.getRandom().nextDouble() - 0.5) * 5.0))
				.add(0.0, lift, 0.0);

			double cy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) candidate.x, (int) candidate.z);
			if (isSafeTeleportSpot(level, candidate.x, cy, candidate.z, entity.getY())) {
				playTeleportEffects(level, entity, entity.getX(), entity.getY(), entity.getZ());
				teleportEntity(entity, candidate.x, cy, candidate.z);
				playTeleportEffects(level, entity, candidate.x, cy, candidate.z);
				
				entity.setDeltaMovement(Vec3.ZERO);
				entity.hasImpulse = true;
				return true;
			}
		}
		return false;
	}

	private static void trySafeTeleportToGround(LevelAccessor world, Entity self, double targetX, double targetZ, String soundId, float vol, float pitch, String cdKey, int cdTicks) {
		double groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) targetX, (int) targetZ);
		if (!isSafeTeleportSpot(world, targetX, groundY, targetZ, self.getY())) return;

		if (world instanceof ServerLevel level) {
			playTeleportEffects(level, self, self.getX(), self.getY(), self.getZ());
			teleportEntity(self, targetX, groundY, targetZ);
			playTeleportEffects(level, self, targetX, groundY, targetZ);
		} else {
			teleportEntity(self, targetX, groundY, targetZ);
			playHostileSound(world, targetX, groundY, targetZ, soundId, vol, pitch);
		}
		self.getPersistentData().putDouble(cdKey, cdTicks);
	}

	private static boolean isSafeTeleportSpot(LevelAccessor world, double x, double y, double z, double fromY) {
		if (Math.abs(y - fromY) > TELEPORT_MAX_VERTICAL_DIFF) return false;

		BlockPos feet = BlockPos.containing(x, y, z);
		BlockPos head = feet.above();
		BlockPos below = feet.below();

		BlockState feetState = world.getBlockState(feet);
		BlockState headState = world.getBlockState(head);
		BlockState belowState = world.getBlockState(below);

		if (!belowState.blocksMotion()) return false;
		if (!feetState.isAir() && !feetState.canBeReplaced()) return false;
		if (!headState.isAir() && !headState.canBeReplaced()) return false;
		return true;
	}

	private static void handleForwardCarveMining(LevelAccessor world, Entity self, Entity target) {
		double mineReach = isDoingCombo(self) ? 3.5 : MINE_REACH;

		HitResult hit = self.level().clip(new ClipContext(
				self.getEyePosition(1f),
				self.getEyePosition(1f).add(self.getViewVector(1f).scale(mineReach)),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				self
		));

		if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult)) {
			if (self instanceof RotEntity rotSet) rotSet.getEntityData().set(RotEntity.DATA_mineProgress, 0);
			return;
		}

		if (self instanceof RotEntity rotSet) {
			rotSet.getEntityData().set(RotEntity.DATA_mineProgress, rotSet.getEntityData().get(RotEntity.DATA_mineProgress) + 1);
		}

		if (self.tickCount % 6 == 0 && self instanceof LivingEntity liv) {
			liv.swing(InteractionHand.MAIN_HAND, true);
		}

		int progress = self instanceof RotEntity rot ? rot.getEntityData().get(RotEntity.DATA_mineProgress) : 0;
		if (progress < 10) return;

		Vec3 forward = self.getViewVector(1f).normalize();
		Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();
		int baseY = Mth.floor(self.getY());

		for (int depth = 1; depth <= (int) Math.ceil(mineReach); depth++) {
			double cx = self.getX() + forward.x * depth;
			double cz = self.getZ() + forward.z * depth;

			for (int w = -MINE_HALF_WIDTH; w <= MINE_HALF_WIDTH; w++) {
				double px = cx + right.x * w;
				double pz = cz + right.z * w;

				for (int h = 0; h < MINE_HEIGHT; h++) {
					BlockPos bp = BlockPos.containing(px, baseY + h, pz);

					if (bp.getY() == (int) (target.getY() - 2)) continue;

					BlockState st = world.getBlockState(bp);
					float hard = st.getDestroySpeed(world, bp);
					if (hard < 0 || hard >= MAX_BREAKABLE_HARDNESS || st.isAir()) continue;

					world.destroyBlock(bp, false);
					if (world instanceof Level l) l.updateNeighborsAt(bp, l.getBlockState(bp).getBlock());
				}
			}
		}

		if (self instanceof RotEntity rotSet) rotSet.getEntityData().set(RotEntity.DATA_mineProgress, 0);
	}

	private static boolean isDoingCombo(Entity entity) {
		return entity.getPersistentData().getDouble("sentinel_combo_active_ticks") > 0
			|| entity.getPersistentData().getDouble("sentinel_sky_warp_slam_ticks") > 0
			|| entity.getPersistentData().getDouble("sentinel_judgment_ticks") > 0;
	}

	private static void handlePassengerAndGrowth(Entity entity) {
		if (entity.isPassenger()) entity.stopRiding();

		entity.getPersistentData().putDouble(K_AGE, entity.getPersistentData().getDouble(K_AGE) + 1);
		if (entity.getPersistentData().getDouble(K_AGE) % 1200 == 0) {
			if (entity instanceof LivingEntity living && living.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
				living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(living.getAttribute(Attributes.MAX_HEALTH).getBaseValue() + 2);
				living.setHealth(living.getHealth() + 2);
			}
		}
	}

	private static void tickCooldown(Entity e, String key, int step) {
		double v = e.getPersistentData().getDouble(key);
		if (v > 0) {
			int finalStep = step;
			if (e.getPersistentData().getBoolean("sentinel_totem_active") && (key.endsWith("cd") || key.contains("_cd") || key.contains("cooldown") || key.equals("sentinel_solar_cd"))) {
				finalStep = step * 2; // cooldowns tick down twice as fast!
			}
			e.getPersistentData().putDouble(key, Math.max(0, v - finalStep));
		}
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

	private static void stopHostileSound(LevelAccessor world, double x, double y, double z, String soundId, double range) {
		if (world instanceof ServerLevel level) {
			net.minecraft.network.protocol.game.ClientboundStopSoundPacket packet = new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(ResourceLocation.parse(soundId), SoundSource.HOSTILE);
			for (ServerPlayer player : level.getPlayers(p -> p.position().distanceToSqr(x, y, z) <= range * range)) {
				player.connection.send(packet);
			}
		}
	}

	private static void teleportEntity(Entity ent, double x, double y, double z) {
		double startX = ent.getX();
		double startY = ent.getY();
		double startZ = ent.getZ();
		ent.teleportTo(x, y, z);
		if (ent instanceof ServerPlayer sp) {
			sp.connection.teleport(x, y, z, ent.getYRot(), ent.getXRot());
		}
		if (ent.level() instanceof ServerLevel level) {
			spawnTeleportTrail(level, startX, startY, startZ, x, y, z);
		}
	}

	private static void spawnTeleportTrail(ServerLevel level, double startX, double startY, double startZ, double targetX, double targetY, double targetZ) {
		Vec3 start = new Vec3(startX, startY, startZ);
		Vec3 end = new Vec3(targetX, targetY, targetZ);
		double distance = start.distanceTo(end);
		if (distance < 1.0) return;

		int steps = (int) Math.ceil(distance * 1.5);
		for (int i = 0; i <= steps; i++) {
			double pct = (double) i / steps;
			double px = startX + (targetX - startX) * pct;
			double pz = startZ + (targetZ - startZ) * pct;
			double py = startY + (targetY - startY) * pct;

			BlockPos checkPos = BlockPos.containing(px, py + 1.0, pz);
			BlockPos groundPos = checkPos;
			boolean foundGround = false;
			for (int dy = 2; dy >= -5; dy--) {
				BlockPos bp = checkPos.above(dy);
				if (!level.getBlockState(bp).isAir() && level.getBlockState(bp).getFluidState().isEmpty() && level.getBlockState(bp).blocksMotion()) {
					groundPos = bp;
					foundGround = true;
					break;
				}
			}

			double spawnY = foundGround ? (groundPos.getY() + 1.0) : py;
			BlockState state = foundGround ? level.getBlockState(groundPos) : net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();

			if (!state.isAir()) {
				try {
					level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state), px, spawnY + 0.1, pz, 4, 0.1, 0.1, 0.1, 0.15);
				} catch (Exception ignored) {}
			}
		}
	}

	private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range, Entity self) {
		return world.getEntitiesOfClass(clazz, AABB.ofSize(new Vec3(x, y, z), range, range, range), e -> isValidTarget(e, self))
				.stream()
				.sorted((e1, e2) -> {
					if (e1 instanceof LivingEntity l1 && e2 instanceof LivingEntity l2) {
						float hp1 = l1.getHealth();
						float hp2 = l2.getHealth();
						if (Math.abs(hp1 - hp2) > 0.05f) {
							return Float.compare(hp1, hp2);
						}
					}
					return Double.compare(e1.distanceToSqr(x, y, z), e2.distanceToSqr(x, y, z));
				})
				.findFirst()
				.orElse(null);
	}

	public static boolean isChannelingAbility(Entity entity) {
		if (entity == null) return false;
		double solarCharge = entity.getPersistentData().getDouble("sentinel_solar_charge_ticks");
		double solarFire = entity.getPersistentData().getDouble("sentinel_solar_fire_ticks");
		double cryoCharge = entity.getPersistentData().getDouble("sentinel_cryo_charge_ticks");
		double cryoFire = entity.getPersistentData().getDouble("sentinel_cryo_fire_ticks");
		double grappleTicks = entity.getPersistentData().getDouble("sentinel_grapple_ticks");
		double tkTicks = entity.getPersistentData().getDouble("sentinel_tk_ticks");
		double sonicTicks = entity.getPersistentData().getDouble("sentinel_sonic_ticks");
		double closingTicks = entity.getPersistentData().getDouble("sentinel_laser_closing_ticks");
		double skyWarp = entity.getPersistentData().getDouble("sentinel_sky_warp_slam_ticks");
		double judgment = entity.getPersistentData().getDouble("sentinel_judgment_ticks");
		double omniSonic = entity.getPersistentData().getDouble("sentinel_omni_sonic_charge_ticks");
		return solarCharge > 0 || solarFire > 0 || cryoCharge > 0 || cryoFire > 0 || grappleTicks > 0 || tkTicks > 0 || sonicTicks > 0 || closingTicks > 0 || skyWarp > 0 || judgment > 0 || omniSonic > 0;
	}

	private static void syncNBTFlags(Entity entity) {
		double solarCharge = entity.getPersistentData().getDouble("sentinel_solar_charge_ticks");
		double cryoCharge = entity.getPersistentData().getDouble("sentinel_cryo_charge_ticks");
		double solarFire = entity.getPersistentData().getDouble("sentinel_solar_fire_ticks");
		double cryoFire = entity.getPersistentData().getDouble("sentinel_cryo_fire_ticks");
		double closingTicks = entity.getPersistentData().getDouble("sentinel_laser_closing_ticks");
		double sonicTicks = entity.getPersistentData().getDouble("sentinel_sonic_ticks");
		double sonicCooldown = entity.getPersistentData().getDouble("sentinel_warden_sonic_cooldown");
		boolean airborne = entity.getPersistentData().getBoolean("sentinel_is_airborne_state");
		double landingTicks = entity.getPersistentData().getDouble("sentinel_landing_ticks");

		boolean charging = solarCharge > 0 || cryoCharge > 0;
		boolean firing = solarFire > 0 || cryoFire > 0;
		boolean closing = closingTicks > 0 && !firing && !charging;
		boolean sonic = sonicTicks > 0;
		boolean landing = landingTicks > 0;

		entity.getPersistentData().putBoolean("laser_charging", charging);
		entity.getPersistentData().putBoolean("is_laser_charging", charging);
		entity.getPersistentData().putBoolean("laser_firing", firing);
		entity.getPersistentData().putBoolean("is_laser_firing", firing);
		entity.getPersistentData().putBoolean("laser_closing", closing);
		entity.getPersistentData().putBoolean("is_laser_closing", closing);
		entity.getPersistentData().putBoolean("sonic_boom_active", sonic);
		entity.getPersistentData().putBoolean("is_airborne", airborne);
		entity.getPersistentData().putBoolean("is_landing", landing);

		if (entity instanceof RotEntity rot) {
			rot.getEntityData().set(RotEntity.DATA_sentinel_solar_charge_ticks, (int) solarCharge);
			rot.getEntityData().set(RotEntity.DATA_sentinel_cryo_charge_ticks, (int) cryoCharge);
			rot.getEntityData().set(RotEntity.DATA_is_laser_firing, firing || charging);
		}
	}

	private static void breakBlocksBehindTarget(LevelAccessor world, Entity target, Vec3 push, boolean totemActive) {
		if (!(world instanceof ServerLevel level) || target == null) return;
		double height = target.getBbHeight();
		Vec3 targetPos = target.position();
		Vec3 behindDir = push.normalize();
		double[] distances = totemActive ? new double[]{0.8, 1.4, 2.0, 2.6, 3.2, 3.8} : new double[]{0.8, 1.4};
		for (double dist : distances) {
			Vec3 pathPos = targetPos.add(behindDir.scale(dist));
			for (double dy = 0; dy < height + 0.5; dy += 0.9) {
				if (totemActive) {
					// Break in a 3x3 cylinder centered around the path position to carve a massive tunnel!
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							BlockPos bp = BlockPos.containing(pathPos.x + dx, pathPos.y + dy, pathPos.z + dz);
							BlockState state = level.getBlockState(bp);
							float hardness = state.getDestroySpeed(level, bp);
							if (hardness >= 0.0F && hardness <= 50.0F && !state.isAir()) {
								level.destroyBlock(bp, false);
								level.sendParticles(ParticleTypes.EXPLOSION, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.02);
							}
						}
					}
				} else {
					BlockPos bp = BlockPos.containing(pathPos.x, pathPos.y + dy, pathPos.z);
					BlockState state = level.getBlockState(bp);
					float hardness = state.getDestroySpeed(level, bp);
					if (hardness >= 0.0F && hardness <= 50.0F && !state.isAir()) {
						level.destroyBlock(bp, false);
						level.sendParticles(ParticleTypes.CRIT, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.02);
					}
				}
			}
		}
	}
}
// 1.21.1
