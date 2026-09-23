/*
 * The code of this mod element is always locked.
 *
 * You can register new events in this class too.
 *
 * If you want to make a plain independent class, create it using
 * Project Browser -> New... and make sure to make the class
 * outside net.mcreator.thebackwoods as this package is managed by MCreator.
 *
 * If you change workspace package, modid or prefix, you will need
 * to manually adapt this file to these changes or remake it.
 *
 * This class will be added in the mod root package.
*/
package net.mcreator.thebackwoods;

import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@EventBusSubscriber
public class AntiGravityFarlands {
	// The coordinate threshold (X or Z absolute value) past which gravity begins to decay. Default: 836721
	public static int FAR_LANDS_THRESHOLD = 836721;

	// Scale factor for how quickly gravity decays. Higher values cause gravity to decay slower over distance. Default: 10000.0
	public static double GRAVITY_DECAY_RATE = 10000.0;

	// Maximum block radius multiplier for attracting dropped items together. Default: 5.0
	public static double MAGNETIC_RANGE_MULTIPLIER = 5.0;

	// Maximum jump height multiplier scale under fully decayed gravity. Default: 1.8
	public static double JUMP_MULTIPLIER_SCALE = 1.8;

	// Toggle to cluster floating items/drops together in zero-g fields. Default: true
	public static boolean ENABLE_MAGNETIC_DEBRIS = true;

	static {
		loadConfig();
	}

	public static void loadConfig() {
		File configFile = new File("config/backwoods_antigrav.properties");
		Properties props = new Properties();
		if (!configFile.getParentFile().exists()) {
			configFile.getParentFile().mkdirs();
		}
		if (configFile.exists()) {
			try (FileInputStream fis = new FileInputStream(configFile)) {
				props.load(fis);
				FAR_LANDS_THRESHOLD = Integer.parseInt(props.getProperty("farLandsThreshold", "836721"));
				GRAVITY_DECAY_RATE = Double.parseDouble(props.getProperty("gravityDecayRate", "10000.0"));
				MAGNETIC_RANGE_MULTIPLIER = Double.parseDouble(props.getProperty("magneticRangeMultiplier", "5.0"));
				JUMP_MULTIPLIER_SCALE = Double.parseDouble(props.getProperty("jumpMultiplierScale", "1.8"));
				ENABLE_MAGNETIC_DEBRIS = Boolean.parseBoolean(props.getProperty("enableMagneticDebris", "true"));
			} catch (Exception e) {
				// Fallback to defaults
			}
		} else {
			props.setProperty("farLandsThreshold", String.valueOf(FAR_LANDS_THRESHOLD));
			props.setProperty("gravityDecayRate", String.valueOf(GRAVITY_DECAY_RATE));
			props.setProperty("magneticRangeMultiplier", String.valueOf(MAGNETIC_RANGE_MULTIPLIER));
			props.setProperty("jumpMultiplierScale", String.valueOf(JUMP_MULTIPLIER_SCALE));
			props.setProperty("enableMagneticDebris", String.valueOf(ENABLE_MAGNETIC_DEBRIS));
			try (FileOutputStream fos = new FileOutputStream(configFile)) {
				props.store(fos, "Anti-Gravity Farlands Mod Configuration");
			} catch (IOException e) {
				// Ignore write fail
			}
		}
	}

	public AntiGravityFarlands() {
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new AntiGravityFarlands();
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		Entity entity = event.getEntity();
		if (entity == null) return;

		Level level = entity.level();
		double x = entity.getX();
		double z = entity.getZ();

		// Check if entity is inside the Far Lands coordinate sectors
		boolean inFarLands = Math.abs(x) >= FAR_LANDS_THRESHOLD || Math.abs(z) >= FAR_LANDS_THRESHOLD;
		
		// Context check: Ensure we are in a Backwoods-related dimension path
		String dimPath = level.dimension().location().getPath();
		boolean inBackwoods = dimPath.contains("backwoods") || dimPath.contains("grain") || dimPath.contains("rotting") || dimPath.contains("loss");

		if (inFarLands && inBackwoods) {
			// Calculate deep-coordinate progressive gravity decay factor:
			// As coordinates get farther beyond FAR_LANDS_THRESHOLD, gravityFactor approaches 0.0 (near-weightlessness)
			double maxDistance = Math.max(Math.abs(x), Math.abs(z));
			double deltaDistance = maxDistance - FAR_LANDS_THRESHOLD;
			// Halves the dynamic gravity effectiveness according to the configured decay rate past the border
			double gravityFactor = 1.0 / (1.0 + (deltaDistance / GRAVITY_DECAY_RATE));

			// Subvert physics based on entity category and current gravity intensity!
			
			// 1. FLOATING LOOT / HOVERING ARTIFACTS
			if (entity instanceof ItemEntity item) {
				if (!item.isNoGravity()) {
					item.setNoGravity(true);
					item.getPersistentData().putBoolean("backwoods_antigrav", true);
				}
				Vec3 delta = item.getDeltaMovement();
				// Bobbing amplitude gets slightly wider/slower as gravity weakens
				double bobAmplitude = 0.012 + 0.008 * (1.0 - gravityFactor);
				double bobSpeed = 0.04 * gravityFactor + 0.01;
				double bobY = bobAmplitude * Math.sin((item.getId() + item.tickCount) * bobSpeed);
				item.setDeltaMovement(new Vec3(delta.x * 0.95, bobY, delta.z * 0.95));

				// Gentle magnetic clustering: pull nearby floating item drops together to form debris structures
				if (ENABLE_MAGNETIC_DEBRIS && item.tickCount % 5 == 0) {
					double radius = 5.0 + MAGNETIC_RANGE_MULTIPLIER * (1.0 - gravityFactor); // Magnetic pull range expands as gravity weakens
					java.util.List<Entity> nearbyItems = level.getEntities(item, item.getBoundingBox().inflate(radius), e -> e instanceof ItemEntity);
					for (Entity other : nearbyItems) {
						if (other != item) {
							Vec3 attractionVec = other.position().subtract(item.position());
							double dist = attractionVec.length();
							if (dist > 0.4 && dist < radius) {
								Vec3 pull = attractionVec.normalize().scale(0.004 * (2.0 - gravityFactor));
								item.setDeltaMovement(item.getDeltaMovement().add(pull));
							}
						}
					}
				}

				// Spawn faint glowing trail particles
				if (item.tickCount % 15 == 0 && level instanceof ServerLevel serverLevel) {
					serverLevel.sendParticles(ParticleTypes.GLOW, item.getX(), item.getY() + 0.2, item.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
				}
			}
			
			// 2. PROGRESSIVE LOW-GRAVITY FOR LIVING ENTITIES
			else if (entity instanceof LivingEntity living) {
				// We map high jumps and low terminal fall vectors dynamically with the gravity factor,
				// which preserves precise player standard input controls and walking/jumping kinematics!
				if (!living.onGround()) {
					Vec3 delta = living.getDeltaMovement();
					// Counteract and absorb standard gravity on downward falls based on dynamic coordinates depth
					if (delta.y < 0) {
						// Dampen descend speed: ranges from 0.76 (at threshold) down to 0.98 (very high coordinate drift)
						double damping = 0.98 - (0.22 * gravityFactor);
						// Terminal velocity limit: ranges from -0.50 (at threshold) down to -0.06 (extremely slow orbit float)
						double terminalLimit = -0.06 - (0.44 * gravityFactor);
						living.setDeltaMovement(new Vec3(delta.x * 0.99, Math.max(delta.y * damping, terminalLimit), delta.z * 0.99));
					} else {
						// Prolong high-jump floatation phase: ranges from 0.94 up to 0.99
						double riseDamp = 0.99 - (0.05 * gravityFactor);
						living.setDeltaMovement(new Vec3(delta.x * 0.99, delta.y * riseDamp, delta.z * 0.99));
					}
					living.fallDistance = 0.0f; // Negate all kinetic fall damage safely
				} else {
					Vec3 delta = living.getDeltaMovement();
					// High-G thrust: jump impulse scales stronger as the regional gravity gets weaker!
					if (delta.y > 0.1) {
						double jumpMultiplier = 1.0 + (JUMP_MULTIPLIER_SCALE * (1.0 - gravityFactor));
						living.setDeltaMovement(new Vec3(delta.x * 1.25, delta.y * jumpMultiplier, delta.z * 1.25));
					}
				}

				// Create environmental distortion particles around active living entities
				if (living.tickCount % 25 == 0 && level instanceof ServerLevel serverLevel) {
					serverLevel.sendParticles(ParticleTypes.PORTAL, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 2, 0.3, 0.4, 0.3, 0.0);
				}
			}

			// 3. ZERO-GRAVITY RAYCAST PROJECTILES
			else if (entity instanceof Projectile arrow) {
				if (!arrow.isNoGravity()) {
					arrow.setNoGravity(true);
					arrow.getPersistentData().putBoolean("backwoods_antigrav", true);
				}
				// Projectiles fly in perfectly flat, continuous paths through the grid labyrinth
				if (level instanceof ServerLevel serverLevel && arrow.tickCount % 2 == 0) {
					serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
				}
			}
		} else {
			// Restore original gravity status and physics parameters when leaving Far Lands chunk sectors
			if (entity.getPersistentData().getBoolean("backwoods_antigrav")) {
				entity.setNoGravity(false);
				entity.getPersistentData().remove("backwoods_antigrav");
			}
		}
	}

	@EventBusSubscriber
	private static class AntiGravityFarlandsForgeBusEvents {
		@SubscribeEvent
		public static void serverLoad(ServerStartingEvent event) {
		}
	}
} // 1.21.1