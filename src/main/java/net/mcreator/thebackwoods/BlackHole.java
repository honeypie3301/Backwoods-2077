/*
 * Singularity & Gravity Rifts for NeoForge 1.21.1
 */
package net.mcreator.thebackwoods;

import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

// Client side rendering & viewport imports
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.List;

@EventBusSubscriber(modid = "the_backwoods")
public class BlackHole {

	// =========================================================================
	// SETTINGS
	// =========================================================================
	public static boolean MASTER_ENABLED = true;
	public static boolean GRAVITY_ENABLED = true;
	public static boolean HAWKING_EXPLOSION_ENABLED = true;
	public static boolean EXPLOSION_BLOCK_DESTRUCTION = true;
	public static float EXPLOSION_RADIUS_MULT = 2.5f;
	public static float PARTICLE_QUALITY = 1.0f;

	public static final String BH_NAME_PREFIX = "BlackHole";
	public static final String RIFT_STEADY_PREFIX = "RiftSteady";
	public static final String RIFT_RAPID_PREFIX = "RiftRapid";
	public static final String RIFT_VERDANT_PREFIX = "RiftVerdant";

	// Mode constants
	public static final int MODE_BLACK_HOLE = 0;
	public static final int MODE_RIFT_STEADY = 1;
	public static final int MODE_RIFT_RAPID = 2;
	public static final int MODE_VERDANT_SPAWN_RIFT = 3;

	public BlackHole() {
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new BlackHole();
	}

	public static boolean isBlackHole(AreaEffectCloud cloud) {
		if (cloud == null) return false;
		if (cloud.getTags().contains("BlackHoleEntity")) return true;
		Component name = cloud.getCustomName();
		if (name != null) {
			String str = name.getString();
			return str.startsWith(BH_NAME_PREFIX) || str.startsWith(RIFT_STEADY_PREFIX) || str.startsWith(RIFT_RAPID_PREFIX) || str.startsWith(RIFT_VERDANT_PREFIX);
		}
		return false;
	}

	public static boolean isBlackHole(Entity entity) {
		if (entity instanceof AreaEffectCloud cloud) {
			return isBlackHole(cloud);
		}
		return false;
	}

	public static boolean isShockwave(AreaEffectCloud cloud) {
		if (cloud == null) return false;
		return cloud.getTags().contains("BH_SHOCKWAVE_ENTITY");
	}

	public static boolean isShockwave(Entity entity) {
		if (entity instanceof AreaEffectCloud cloud) {
			return isShockwave(cloud);
		}
		return false;
	}

	public static boolean isRotEntity(Entity entity) {
		if (entity == null) return false;
		if (entity.getTags().contains("TheRot") || entity.getTags().contains("rot") || entity.getTags().contains("rot_entity")) return true;
		try {
			String entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
			if (entityKey.contains("rot")) return true;
		} catch (Exception ignored) {}
		String className = entity.getClass().getSimpleName().toLowerCase();
		return className.contains("rot");
	}

	public static boolean isVerdantEngine(Entity entity) {
		if (entity == null) return false;
		if (entity.getTags().contains("verdant_engine") || entity.getTags().contains("verdant")) return true;
		try {
			String entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
			if (entityKey.contains("verdant")) return true;
		} catch (Exception ignored) {}
		String className = entity.getClass().getSimpleName().toLowerCase();
		return className.contains("verdant");
	}

	public static boolean isImmuneBoss(Entity entity) {
		if (entity == null) return false;
		if (isRotEntity(entity)) return true;
		if (isVerdantEngine(entity)) return true;
		return false;
	}

	public static int getMode(AreaEffectCloud cloud) {
		if (cloud.getTags().contains("BH_RIFT_VERDANT")) return MODE_VERDANT_SPAWN_RIFT;
		if (cloud.getTags().contains("BH_RIFT_RAPID")) return MODE_RIFT_RAPID;
		if (cloud.getTags().contains("BH_RIFT_STEADY")) return MODE_RIFT_STEADY;
		if (cloud.getTags().contains("BH_BLACK_HOLE")) return MODE_BLACK_HOLE;
		Component name = cloud.getCustomName();
		if (name != null) {
			String str = name.getString();
			if (str.startsWith(RIFT_VERDANT_PREFIX)) return MODE_VERDANT_SPAWN_RIFT;
			if (str.startsWith(RIFT_RAPID_PREFIX)) return MODE_RIFT_RAPID;
			if (str.startsWith(RIFT_STEADY_PREFIX)) return MODE_RIFT_STEADY;
		}
		return MODE_BLACK_HOLE;
	}

	public static float getRadius(AreaEffectCloud cloud) {
		for (String tag : cloud.getTags()) {
			if (tag.startsWith("BH_RAD_")) {
				try {
					return Float.parseFloat(tag.substring(7));
				} catch (Exception ignored) {}
			}
		}
		Component name = cloud.getCustomName();
		if (name != null) {
			String str = name.getString();
			int idx = str.indexOf(':');
			if (idx != -1) {
				try {
					int secondIdx = str.indexOf(':', idx + 1);
					String radStr = secondIdx != -1 ? str.substring(idx + 1, secondIdx) : str.substring(idx + 1);
					return Float.parseFloat(radStr);
				} catch (Exception ignored) {}
			}
		}
		float r = cloud.getRadius();
		return r > 0.05f ? r : 1.2f;
	}

	public static float getDynamicInterpolatedRadius(AreaEffectCloud cloud) {
		float maxRadius = getRadius(cloud);
		if (maxRadius <= 0.05f) maxRadius = 1.2f;

		int maxTicks = getMaxTicks(cloud);
		if (maxTicks <= 0) return maxRadius;

		float ageProgress = Math.min(1.0f, Math.max(0.0f, (float) cloud.tickCount / (float) maxTicks));
		int mode = getMode(cloud);

		if (mode == MODE_VERDANT_SPAWN_RIFT) {
			if (ageProgress < 0.50f) {
				float ramp = ageProgress / 0.50f;
				return maxRadius * (ramp * ramp * (3.0f - 2.0f * ramp));
			} else if (ageProgress < 0.75f) {
				return maxRadius;
			} else {
				float t = (ageProgress - 0.75f) / 0.25f;
				return maxRadius * (float) Math.pow(2.0, -10.0 * t);
			}
		} else if (mode == MODE_RIFT_RAPID) {
			if (ageProgress < 0.75f) {
				float ramp = ageProgress / 0.75f;
				return maxRadius * (ramp * ramp * (3.0f - 2.0f * ramp));
			} else {
				float endPhase = (ageProgress - 0.75f) / 0.25f;
				float rem = Math.max(0.0f, 1.0f - endPhase);
				return maxRadius * (rem * rem);
			}
		} else {
			if (ageProgress < 0.15f) {
				float t = ageProgress / 0.15f;
				return maxRadius * (t * t * (3.0f - 2.0f * t));
			} else if (ageProgress < 0.80f) {
				return maxRadius;
			} else {
				float t = (ageProgress - 0.80f) / 0.20f;
				float rem = Math.max(0.08f, 1.0f - t);
				return maxRadius * (rem * rem);
			}
		}
	}

	public static int getMaxTicks(AreaEffectCloud cloud) {
		if (cloud == null) return 1200;
		for (String tag : cloud.getTags()) {
			if (tag.startsWith("BH_MAXTICKS_")) {
				try {
					return Integer.parseInt(tag.substring(12));
				} catch (Exception ignored) {}
			} else if (tag.startsWith("BH_SW_MAXTICKS_")) {
				try {
					return Integer.parseInt(tag.substring(15));
				} catch (Exception ignored) {}
			}
		}
		Component name = cloud.getCustomName();
		if (name != null) {
			String str = name.getString();
			int firstIdx = str.indexOf(':');
			if (firstIdx != -1) {
				int secondIdx = str.indexOf(':', firstIdx + 1);
				if (secondIdx != -1) {
					try {
						return Integer.parseInt(str.substring(secondIdx + 1));
					} catch (Exception ignored) {}
				}
			}
		}
		return 1200;
	}

	// =========================================================================
	// PUBLIC STATIC API
	// =========================================================================

	public static void spawnVerdantSpawnRift(Level level, double x, double y, double z) {
		spawnSingularity(level, x, y, z, 35.0f, 4.0f, MODE_VERDANT_SPAWN_RIFT, false);
	}

	public static void spawnRotRift(Level level, double x, double y, double z) {
		spawnSingularity(level, x, y + 1.15, z, 1.2f, 2.5f, MODE_RIFT_RAPID);
	}

	public static void spawnRotDeathHole(Level level, double x, double y, double z) {
		spawnRotDeathHole(level, x, y, z, 8.0f, 7.0f);
	}

	public static void spawnRotDeathHole(Level level, double x, double y, double z, float radius, float durationSeconds) {
		spawnSingularity(level, x, y, z, radius, durationSeconds, MODE_BLACK_HOLE, false);
	}

	public static void spawnBlackHole(Level level, double x, double y, double z, float radius, int durationSeconds) {
		spawnSingularity(level, x, y, z, radius, (float) durationSeconds, MODE_BLACK_HOLE);
	}

	public static void spawnBlackHole(Level level, double x, double y, double z, float radius, float durationSeconds, double ignoredGravity) {
		spawnSingularity(level, x, y, z, radius, durationSeconds, MODE_BLACK_HOLE);
	}

	public static void spawnRift(Level level, double x, double y, double z, float radius, int durationSeconds) {
		spawnSingularity(level, x, y, z, radius, (float) durationSeconds, MODE_RIFT_STEADY);
	}

	public static void spawnRift(Level level, double x, double y, double z, float radius, float durationSeconds, double ignoredGravity) {
		spawnSingularity(level, x, y, z, radius, durationSeconds, MODE_RIFT_STEADY);
	}

	public static void spawnRapidRift(Level level, double x, double y, double z, float radius, float durationSeconds) {
		spawnSingularity(level, x, y, z, radius, durationSeconds, MODE_RIFT_RAPID);
	}

	public static void spawnRapidRift(Level level, double x, double y, double z, float radius, float durationSeconds, double ignoredGravity) {
		spawnSingularity(level, x, y, z, radius, durationSeconds, MODE_RIFT_RAPID);
	}

	public static void spawnSingularity(Level level, double x, double y, double z, float radius, float durationSeconds, int mode) {
		spawnSingularity(level, x, y, z, radius, durationSeconds, mode, true);
	}

	public static void spawnSingularity(Level level, double x, double y, double z, float radius, float durationSeconds, int mode, double ignoredGravity) {
		spawnSingularity(level, x, y, z, radius, durationSeconds, mode, true);
	}

	public static void spawnSingularity(Level level, double x, double y, double z, float radius, float durationSeconds, int mode, boolean antiClippingTerrain) {
		if (!MASTER_ENABLED || level.isClientSide()) return;

		double safeY = y;
		if (antiClippingTerrain) {
			net.minecraft.core.BlockPos blockPos = net.minecraft.core.BlockPos.containing(x, y, z);
			int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos.getX(), blockPos.getZ());
			safeY = Math.max(y, (double) surfaceY + (double) radius * 0.45);
		}

		AreaEffectCloud cloud = new AreaEffectCloud(EntityType.AREA_EFFECT_CLOUD, level);
		if (cloud != null) {
			cloud.setPos(x, safeY, z);
			cloud.setRadius(0.0F);
			cloud.setRadiusPerTick(0.0F);
			cloud.setWaitTime(0);
			int totalTicks = Math.max(10, (int)(durationSeconds * 20.0f));
			cloud.setDuration(totalTicks + 200);
			
			cloud.addTag("BlackHoleEntity");
			cloud.addTag("BH_RAD_" + radius);
			cloud.addTag("BH_MAXTICKS_" + totalTicks);

			if (mode == MODE_VERDANT_SPAWN_RIFT) {
				cloud.addTag("BH_RIFT_VERDANT");
				cloud.setCustomName(Component.literal(RIFT_VERDANT_PREFIX + ":" + radius + ":" + totalTicks));
			} else if (mode == MODE_RIFT_RAPID) {
				cloud.addTag("BH_RIFT_RAPID");
				cloud.setCustomName(Component.literal(RIFT_RAPID_PREFIX + ":" + radius + ":" + totalTicks));
			} else if (mode == MODE_RIFT_STEADY) {
				cloud.addTag("BH_RIFT_STEADY");
				cloud.setCustomName(Component.literal(RIFT_STEADY_PREFIX + ":" + radius + ":" + totalTicks));
			} else {
				cloud.addTag("BH_BLACK_HOLE");
				cloud.setCustomName(Component.literal(BH_NAME_PREFIX + ":" + radius + ":" + totalTicks));
			}
			cloud.setCustomNameVisible(false);

			level.addFreshEntity(cloud);
		}
	}

	// =========================================================================
	// CLIENT-SIDE REAL-TIME GRAVITATIONAL LENSING SHADER & FOV SPAGHETTIFICATION
	// =========================================================================
	@OnlyIn(Dist.CLIENT)
	@EventBusSubscriber(value = Dist.CLIENT, modid = "the_backwoods")
	public static class ClientRenderEvents {

		private static int shaderProgram = 0;
		private static int uSceneTextureLoc = -1;
		private static int uDepthTextureLoc = -1;
		private static int uCenterLoc = -1;
		private static int uRadiusLoc = -1;
		private static int uAspectRatioLoc = -1;
		private static int uSphereDepthLoc = -1;
		private static int uNearFarLoc = -1;
		private static int uHasDepthLoc = -1;
		private static int uModeLoc = -1;
		private static int uTimeLoc = -1;
		private static int uAgeProgressLoc = -1;

		private static int sceneCopyTexId = -1;
		private static int lastWidth = -1;
		private static int lastHeight = -1;

		private static int quadVao = 0;
		private static int quadVbo = 0;
		private static boolean initialized = false;

		private static float currentSmoothedFovBoost = 0.0f;
		private static long lastFovFrameTimeNanos = 0L;

		@SubscribeEvent
		public static void onComputeFov(ViewportEvent.ComputeFov event) {
			if (!MASTER_ENABLED) return;
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null || mc.level == null) {
				currentSmoothedFovBoost = 0.0f;
				return;
			}

			long now = System.nanoTime();
			float dt = 0.016f;
			if (lastFovFrameTimeNanos > 0L) {
				dt = (float) ((now - lastFovFrameTimeNanos) / 1_000_000_000.0);
				dt = Math.max(0.001f, Math.min(0.1f, dt));
			}
			lastFovFrameTimeNanos = now;

			float targetFovBoost = 0.0f;

			Vec3 pPos = mc.player.position();
			AABB searchBox = new AABB(pPos.x - 64, pPos.y - 32, pPos.z - 64, pPos.x + 64, pPos.y + 32, pPos.z + 64);
			List<AreaEffectCloud> clouds = mc.level.getEntitiesOfClass(AreaEffectCloud.class, searchBox, BlackHole::isBlackHole);

			if (!clouds.isEmpty()) {
				double closestDist = Double.MAX_VALUE;
				double closestRadius = 2.5;
				AreaEffectCloud closestCloud = null;

				for (AreaEffectCloud cloud : clouds) {
					double dist = mc.player.distanceTo(cloud);
					if (dist < closestDist) {
						closestDist = dist;
						closestCloud = cloud;
						double r = getDynamicInterpolatedRadius(cloud);
						closestRadius = (r <= 0.05) ? 2.5 : r;
					}
				}

				if (closestCloud != null) {
					int maxTicks = BlackHole.getMaxTicks(closestCloud);
					float ageProgress = (maxTicks > 0) ? Math.min(1.0f, (float) closestCloud.tickCount / (float) maxTicks) : 0.5f;
					int mode = BlackHole.getMode(closestCloud);

					float lifeMultiplier = 1.0f;
					if (mode == MODE_RIFT_RAPID) {
						if (ageProgress < 0.75f) {
							float ramp = ageProgress / 0.75f;
							lifeMultiplier = ramp * ramp * (3.0f - 2.0f * ramp);
						} else {
							lifeMultiplier = Math.max(0.0f, (1.0f - ageProgress) / 0.25f);
						}
					} else {
						if (ageProgress < 0.10f) {
							float t = ageProgress / 0.10f;
							float inv = 1.0f - t;
							lifeMultiplier = 1.0f - (inv * inv * inv);
						} else if (ageProgress < 0.85f) {
							lifeMultiplier = 1.0f;
						} else {
							float t = (ageProgress - 0.85f) / 0.15f;
							float remain = Math.max(0.0f, 1.0f - Math.min(1.0f, Math.max(0.0f, t)));
							lifeMultiplier = remain * remain;
						}
					}

					double maxInfluence = closestRadius * 4.5;
					if (closestDist < maxInfluence) {
						double progress = 1.0 - (closestDist / maxInfluence);
						double curve = progress * progress * (3.0 - 2.0 * progress);
						targetFovBoost = (float) (curve * 38.0 * lifeMultiplier);
					}
				}
			}

			float smoothFactor = 1.0f - (float) Math.exp(-9.0 * dt);
			currentSmoothedFovBoost += (targetFovBoost - currentSmoothedFovBoost) * smoothFactor;

			if (Math.abs(currentSmoothedFovBoost) < 0.01f && targetFovBoost == 0.0f) {
				currentSmoothedFovBoost = 0.0f;
			}

			if (currentSmoothedFovBoost > 0.01f) {
				event.setFOV(event.getFOV() + currentSmoothedFovBoost);
			}
		}

		private static void initShader() {
			if (initialized) return;
			initialized = true;

			String vertSource = 
				"#version 150\n" +
				"in vec2 a_Position;\n" +
				"out vec2 v_TexCoord;\n" +
				"void main() {\n" +
				"    v_TexCoord = a_Position * 0.5 + 0.5;\n" +
				"    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
				"}\n";

			String fragSource = 
				"#version 150\n" +
				"uniform sampler2D u_SceneTexture;\n" +
				"uniform sampler2D u_DepthTexture;\n" +
				"uniform vec2 u_Center;\n" +
				"uniform float u_Radius;\n" +
				"uniform float u_AspectRatio;\n" +
				"uniform float u_SphereDepth;\n" +
				"uniform vec2 u_NearFar;\n" +
				"uniform int u_HasDepth;\n" +
				"uniform int u_Mode;\n" +        
				"uniform float u_Time;\n" +      
				"uniform float u_AgeProgress;\n" + 
				"in vec2 v_TexCoord;\n" +
				"out vec4 fragColor;\n" +
				"\n" +
				"float linearizeDepth(float z_b, float near, float far) {\n" +
				"    float z_n = 2.0 * z_b - 1.0;\n" +
				"    return (2.0 * near * far) / (far + near - z_n * (far - near));\n" +
				"}\n" +
				"\n" +
				"void main() {\n" +
				"    vec2 uv = v_TexCoord;\n" +
				"    vec2 delta = uv - u_Center;\n" +
				"    delta.x *= u_AspectRatio;\n" +
				"    float dist = length(delta);\n" +
				"\n" +
				"    float lifeScale = 1.0;\n" +
				"    float decayFade = 1.0;\n" +
				"\n" +
				"    if (u_Mode == 3) {\n" +
				"        if (u_AgeProgress < 0.50) {\n" +
				"            float ramp = u_AgeProgress / 0.50;\n" +
				"            lifeScale = ramp * ramp * (3.0 - 2.0 * ramp);\n" +
				"            decayFade = lifeScale;\n" +
				"        } else if (u_AgeProgress < 0.75) {\n" +
				"            lifeScale = 1.0;\n" +
				"            decayFade = 1.0;\n" +
				"        } else {\n" +
				"            float t = (u_AgeProgress - 0.75) / 0.25;\n" +
				"            lifeScale = pow(2.0, -10.0 * t);\n" +
				"            decayFade = lifeScale;\n" +
				"        }\n" +
				"    } else if (u_Mode == 2) {\n" +
				"        if (u_AgeProgress < 0.75) {\n" +
				"            float ramp = u_AgeProgress / 0.75;\n" +
				"            lifeScale = ramp * ramp * (3.0 - 2.0 * ramp);\n" +
				"            decayFade = lifeScale;\n" +
				"        } else {\n" +
				"            float endPhase = (u_AgeProgress - 0.75) / 0.25;\n" +
				"            lifeScale = max(0.0, 1.0 - endPhase);\n" +
				"            lifeScale = lifeScale * lifeScale;\n" +
				"            decayFade = lifeScale;\n" +
				"        }\n" +
				"    } else if (u_Mode == 1) {\n" +
				"        float inEase = clamp(u_AgeProgress * 4.0, 0.0, 1.0);\n" +
				"        float outEase = clamp((1.0 - u_AgeProgress) * 4.0, 0.0, 1.0);\n" +
				"        lifeScale = inEase * outEase;\n" +
				"        decayFade = lifeScale;\n" +
				"    } else {\n" +
				"        if (u_AgeProgress < 0.10) {\n" +
				"            float t = u_AgeProgress / 0.10;\n" +
				"            float inv = 1.0 - t;\n" +
				"            lifeScale = 1.0 - (inv * inv * inv);\n" +
				"        } else if (u_AgeProgress < 0.85) {\n" +
				"            lifeScale = 1.0;\n" +
				"        } else {\n" +
				"            float t = (u_AgeProgress - 0.85) / 0.15;\n" +
				"            float remain = max(0.0, 1.0 - clamp(t, 0.0, 1.0));\n" +
				"            lifeScale = remain * remain;\n" +
				"        }\n" +
				"        decayFade = lifeScale;\n" +
				"    }\n" +
				"\n" +
				"    if (lifeScale <= 0.001) {\n" +
				"        discard;\n" +
				"    }\n" +
				"\n" +
				"    float effRadius = u_Radius * lifeScale;\n" +
				"    float rMax = effRadius * 2.5;\n" +
				"    if (dist > rMax) {\n" +
				"        discard;\n" +
				"    }\n" +
				"\n" +
				"    float rHorizon = effRadius * 0.92;\n" +
				"    float rPhoton = effRadius * 0.96;\n" +
				"\n" +
				"    // 1. Anti-Clipping Depth Test (Foreground terrain & players occlude the sphere, but sky/clouds do not)\n" +
				"    if (u_HasDepth == 1) {\n" +
				"        float rawDepth = texture(u_DepthTexture, uv).r;\n" +
				"        if (rawDepth < 0.995) {\n" +
				"            float sceneLinearDepth = linearizeDepth(rawDepth, u_NearFar.x, u_NearFar.y);\n" +
				"            float capRadSq = max(0.0, rHorizon * rHorizon - min(dist * dist, rHorizon * rHorizon));\n" +
				"            float sphereFrontCap = u_SphereDepth - sqrt(capRadSq);\n" +
				"            if (sceneLinearDepth < (sphereFrontCap - 0.45)) {\n" +
				"                discard;\n" +
				"            }\n" +
				"        }\n" +
				"    }\n" +
				"\n" +
				"    // 2. Mode 0: Pitch-Black Event Horizon Void Sphere (Anti-Aliased Boundary, Zero Z-Fighting/Jagged Lines)\n" +
				"    if (u_Mode == 0 && dist <= rHorizon) {\n" +
				"        fragColor = vec4(0.0, 0.0, 0.0, 1.0);\n" +
				"        return;\n" +
				"    }\n" +
				"\n" +
				"    // 3. Spherical 3D Gravitational Ray Deflection & Relativistic Asymmetrical Doppler Beaming\n" +
				"    vec2 dir = (dist > 0.0001) ? (delta / dist) : vec2(0.0, 1.0);\n" +
				"    float normDist = dist / rMax;\n" +
				"    float edgeFade = 1.0 - smoothstep(0.0, 1.0, normDist);\n" +
				"    edgeFade = edgeFade * edgeFade;\n" +
				"\n" +
				"    float deflection = 0.0;\n" +
				"    vec3 glow = vec3(0.0);\n" +
				"\n" +
				"    float angle = atan(delta.y, delta.x);\n" +
				"    float dopplerBeaming = 1.0 + 0.45 * cos(angle + u_Time * 1.5);\n" +
				"\n" +
				"    if (u_Mode == 0) {\n" +
				"        float horizonNorm = clamp((dist - rHorizon) / max(0.0001, (rMax - rHorizon)), 0.0, 1.0);\n" +
				"        float bhFade = 1.0 - smoothstep(0.0, 1.0, horizonNorm);\n" +
				"        bhFade = bhFade * bhFade;\n" +
				"        deflection = ((effRadius * 0.40) / max(0.005, dist - rHorizon * 0.40)) * bhFade * 0.45;\n" +
				"        \n" +
				"        float ringDiff = abs(dist - rPhoton);\n" +
				"        float ringSpread = max(0.0001, effRadius * 0.006);\n" +
				"        float ringGlow = exp(-ringDiff * ringDiff / ringSpread) * 0.95 * clamp(decayFade, 0.0, 1.0);\n" +
				"        glow = vec3(0.65, 0.88, 1.6) * ringGlow * dopplerBeaming;\n" +
				"    } else if (u_Mode == 1) {\n" +
				"        float lensStrength = (effRadius * 0.45) / (dist + effRadius * 0.25);\n" +
				"        deflection = lensStrength * edgeFade;\n" +
				"        \n" +
				"        float coreGlow = exp(-dist * dist / max(0.0001, effRadius * effRadius * 0.04)) * 0.25 * decayFade;\n" +
				"        glow = vec3(0.5, 0.85, 1.6) * coreGlow * dopplerBeaming;\n" +
				"    } else {\n" +
				"        float pulseSpeed = 20.0 + (u_AgeProgress * 15.0);\n" +
				"        float wave = sin(u_Time * pulseSpeed - dist * 35.0);\n" +
				"        float pulseMultiplier = 1.0 + 0.45 * wave;\n" +
				"        float lensStrength = ((effRadius * 0.70) / (dist + effRadius * 0.16)) * pulseMultiplier;\n" +
				"        deflection = lensStrength * edgeFade;\n" +
				"        \n" +
				"        float rippleGlow = (0.5 + 0.5 * wave) * exp(-dist * dist / max(0.0001, effRadius * effRadius * 0.25)) * 0.45 * decayFade;\n" +
				"        glow = vec3(0.6, 0.9, 1.6) * rippleGlow * dopplerBeaming;\n" +
				"    }\n" +
				"\n" +
				"    vec2 distortedDelta = delta - dir * deflection;\n" +
				"    distortedDelta.x /= u_AspectRatio;\n" +
				"    vec2 sampleUV = clamp(u_Center + distortedDelta, vec2(0.001), vec2(0.999));\n" +
				"\n" +
				"    vec4 sceneColor = texture(u_SceneTexture, sampleUV);\n" +
				"    fragColor = vec4(sceneColor.rgb + glow, 1.0);\n" +
				"}\n";

			int vertShader = compileShader(GL20.GL_VERTEX_SHADER, vertSource);
			int fragShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragSource);

			if (vertShader != 0 && fragShader != 0) {
				shaderProgram = GL20.glCreateProgram();
				GL20.glAttachShader(shaderProgram, vertShader);
				GL20.glAttachShader(shaderProgram, fragShader);
				GL20.glBindAttribLocation(shaderProgram, 0, "a_Position");
				GL20.glLinkProgram(shaderProgram);

				uSceneTextureLoc = GL20.glGetUniformLocation(shaderProgram, "u_SceneTexture");
				uDepthTextureLoc = GL20.glGetUniformLocation(shaderProgram, "u_DepthTexture");
				uCenterLoc = GL20.glGetUniformLocation(shaderProgram, "u_Center");
				uRadiusLoc = GL20.glGetUniformLocation(shaderProgram, "u_Radius");
				uAspectRatioLoc = GL20.glGetUniformLocation(shaderProgram, "u_AspectRatio");
				uSphereDepthLoc = GL20.glGetUniformLocation(shaderProgram, "u_SphereDepth");
				uNearFarLoc = GL20.glGetUniformLocation(shaderProgram, "u_NearFar");
				uHasDepthLoc = GL20.glGetUniformLocation(shaderProgram, "u_HasDepth");
				uModeLoc = GL20.glGetUniformLocation(shaderProgram, "u_Mode");
				uTimeLoc = GL20.glGetUniformLocation(shaderProgram, "u_Time");
				uAgeProgressLoc = GL20.glGetUniformLocation(shaderProgram, "u_AgeProgress");
			}

			float[] quadVertices = {
				-1.0f, -1.0f,
				 1.0f, -1.0f,
				 1.0f,  1.0f,
				-1.0f, -1.0f,
				 1.0f,  1.0f,
				-1.0f,  1.0f
			};

			quadVao = GL30.glGenVertexArrays();
			quadVbo = GL15.glGenBuffers();

			GL30.glBindVertexArray(quadVao);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);
			java.nio.FloatBuffer buf = org.lwjgl.BufferUtils.createFloatBuffer(quadVertices.length);
			buf.put(quadVertices).flip();
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
			GL20.glEnableVertexAttribArray(0);
			GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);
			GL30.glBindVertexArray(0);
		}

		private static int compileShader(int type, String src) {
			int shader = GL20.glCreateShader(type);
			GL20.glShaderSource(shader, src);
			GL20.glCompileShader(shader);
			if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				GL20.glDeleteShader(shader);
				return 0;
			}
			return shader;
		}

		@SubscribeEvent
		public static void onRenderLevelStage(RenderLevelStageEvent event) {
			if (!MASTER_ENABLED) return;
			if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
				return;
			}

			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.getMainRenderTarget() == null) return;

			Camera camera = event.getCamera();
			Vec3 camPos = camera.getPosition();

			AABB searchBox = new AABB(camPos.x - 1000, camPos.y - 500, camPos.z - 1000,
					camPos.x + 1000, camPos.y + 500, camPos.z + 1000);
			List<AreaEffectCloud> clouds = mc.level.getEntitiesOfClass(AreaEffectCloud.class, searchBox,
					BlackHole::isBlackHole);

			if (clouds.isEmpty()) return;

			initShader();
			if (shaderProgram == 0) return;

			int width = mc.getMainRenderTarget().viewWidth;
			int height = mc.getMainRenderTarget().viewHeight;
			if (width <= 0 || height <= 0) return;

			if (sceneCopyTexId == -1 || width != lastWidth || height != lastHeight) {
				if (sceneCopyTexId != -1) {
					GL11.glDeleteTextures(sceneCopyTexId);
				}
				sceneCopyTexId = GL11.glGenTextures();
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneCopyTexId);
				GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
				lastWidth = width;
				lastHeight = height;
			}

			Matrix4f modelView = new Matrix4f(event.getModelViewMatrix());
			Matrix4f projection = new Matrix4f(event.getProjectionMatrix());

			int depthTextureId = mc.getMainRenderTarget().getDepthTextureId();
			float globalTime = (float) ((System.currentTimeMillis() % 1000000L) / 1000.0);

			for (AreaEffectCloud entity : clouds) {
				float radius = BlackHole.getRadius(entity);
				if (radius <= 0.05f) radius = 1.2f;

				int mode = BlackHole.getMode(entity);

				int maxTicks = BlackHole.getMaxTicks(entity);
				float ageProgress = (maxTicks > 0) ? Math.min(1.0f, (float) entity.tickCount / (float) maxTicks) : 0.5f;

				float rx = (float) (entity.getX() - camPos.x);
				float ry = (float) (entity.getY() - camPos.y);
				float rz = (float) (entity.getZ() - camPos.z);

				Vector4f eyePos = new Vector4f(rx, ry, rz, 1.0f);
				modelView.transform(eyePos);

				if (eyePos.z >= -0.1f) continue;

				Vector4f clipPos = new Vector4f(eyePos);
				projection.transform(clipPos);

				if (clipPos.w <= 0.001f) continue;

				float ndcX = clipPos.x / clipPos.w;
				float ndcY = clipPos.y / clipPos.w;

				if (ndcX < -2.0f || ndcX > 2.0f || ndcY < -2.0f || ndcY > 2.0f) continue;

				float screenU = (ndcX + 1.0f) * 0.5f;
				float screenV = (ndcY + 1.0f) * 0.5f;

				float depth = -eyePos.z;
				float fovScale = Math.abs(projection.m11());
				float screenRadius = (radius / Math.max(depth, 0.1f)) * (fovScale * 0.5f);

				GL13.glActiveTexture(GL13.GL_TEXTURE0);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneCopyTexId);
				GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

				boolean hasDepth = (depthTextureId > 0);
				if (hasDepth) {
					GL13.glActiveTexture(GL13.GL_TEXTURE1);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTextureId);
				}

				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.disableCull();
				RenderSystem.depthMask(false);
				RenderSystem.disableDepthTest();

				GL20.glUseProgram(shaderProgram);
				GL20.glUniform1i(uSceneTextureLoc, 0);
				GL20.glUniform1i(uDepthTextureLoc, 1);
				GL20.glUniform1i(uHasDepthLoc, hasDepth ? 1 : 0);
				GL20.glUniform1i(uModeLoc, mode);
				GL20.glUniform1f(uTimeLoc, globalTime);
				GL20.glUniform1f(uAgeProgressLoc, ageProgress);

				GL20.glUniform2f(uCenterLoc, screenU, screenV);
				GL20.glUniform1f(uRadiusLoc, screenRadius);
				GL20.glUniform1f(uAspectRatioLoc, (float) width / (float) height);
				GL20.glUniform1f(uSphereDepthLoc, depth);
				GL20.glUniform2f(uNearFarLoc, 0.05f, (float) (mc.options.getEffectiveRenderDistance() * 16));

				GL30.glBindVertexArray(quadVao);
				GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
				GL30.glBindVertexArray(0);

				GL20.glUseProgram(0);

				// Reset all texture bindings and active texture unit to avoid corrupting vanilla hand/item rendering
				if (hasDepth) {
					GL13.glActiveTexture(GL13.GL_TEXTURE1);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
				}
				GL13.glActiveTexture(GL13.GL_TEXTURE0);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

				RenderSystem.enableDepthTest();
				RenderSystem.depthMask(true);
				RenderSystem.enableCull();
				RenderSystem.disableBlend();
				RenderSystem.defaultBlendFunc();
			}
		}
	}

	// =========================================================================
	// SERVER / FORGE BUS LOGIC: COMMANDS, INVERSE-SQUARE GRAVITY & SOUNDS
	// =========================================================================
	@EventBusSubscriber(modid = "the_backwoods")
	private static class BlackHoleForgeBusEvents {

		@SubscribeEvent
		public static void registerCommands(RegisterCommandsEvent event) {
			if (!MASTER_ENABLED) return;
			event.getDispatcher().register(
				Commands.literal("blackhole")
					.requires(source -> source.hasPermission(2))
					.then(Commands.literal("spawn")
						.executes(context -> {
							CommandSourceStack source = context.getSource();
							Vec3 pos = getLookSpawnPos(source, 5.0);
							spawnBlackHole(source.getLevel(), pos.x, pos.y, pos.z, 2.5f, 60);
							source.sendSuccess(() -> Component.literal("§aBlack Hole (radius: 2.5, duration: 60s) spawned!"), true);
							return 1;
						})
						.then(Commands.argument("radius", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.5, 40.0))
							.executes(context -> {
								CommandSourceStack source = context.getSource();
								double radius = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "radius");
								Vec3 pos = getLookSpawnPos(source, 5.0);
								spawnBlackHole(source.getLevel(), pos.x, pos.y, pos.z, (float) radius, 60);
								source.sendSuccess(() -> Component.literal("§aBlack Hole (radius: " + radius + ", duration: 60s) spawned!"), true);
								return 1;
							})
							.then(Commands.argument("duration_seconds", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 7200))
								.executes(context -> {
									CommandSourceStack source = context.getSource();
									double radius = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "radius");
									int duration = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "duration_seconds");
									Vec3 pos = getLookSpawnPos(source, 5.0);
									spawnBlackHole(source.getLevel(), pos.x, pos.y, pos.z, (float) radius, duration);
									source.sendSuccess(() -> Component.literal("§aBlack Hole (radius: " + radius + ", duration: " + duration + "s) spawned!"), true);
									return 1;
								})
							)
						)
					)
					.then(Commands.literal("spawn_rift")
						.executes(context -> {
							CommandSourceStack source = context.getSource();
							Vec3 pos = getLookSpawnPos(source, 5.0);
							spawnRift(source.getLevel(), pos.x, pos.y, pos.z, 2.5f, 60);
							source.sendSuccess(() -> Component.literal("§bSteady Gravity Rift (radius: 2.5, duration: 60s) spawned!"), true);
							return 1;
						})
						.then(Commands.argument("radius", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.5, 40.0))
							.executes(context -> {
								CommandSourceStack source = context.getSource();
								double radius = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "radius");
								Vec3 pos = getLookSpawnPos(source, 5.0);
								spawnRift(source.getLevel(), pos.x, pos.y, pos.z, (float) radius, 60);
								source.sendSuccess(() -> Component.literal("§bSteady Gravity Rift (radius: " + radius + ", duration: 60s) spawned!"), true);
								return 1;
							})
							.then(Commands.argument("duration_seconds", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 7200))
								.executes(context -> {
									CommandSourceStack source = context.getSource();
									double radius = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "radius");
									int duration = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "duration_seconds");
									Vec3 pos = getLookSpawnPos(source, 5.0);
									spawnRift(source.getLevel(), pos.x, pos.y, pos.z, (float) radius, duration);
									source.sendSuccess(() -> Component.literal("§bSteady Gravity Rift (radius: " + radius + ", duration: " + duration + "s) spawned!"), true);
									return 1;
								})
							)
						)
					)
					.then(Commands.literal("spawn_rapid_rift")
						.executes(context -> {
							CommandSourceStack source = context.getSource();
							Vec3 pos = getLookSpawnPos(source, 5.0);
							spawnRapidRift(source.getLevel(), pos.x, pos.y, pos.z, 2.5f, 5.0f);
							source.sendSuccess(() -> Component.literal("§dRapid Spacetime Rift (radius: 2.5, duration: 5.0s) spawned!"), true);
							return 1;
						})
						.then(Commands.argument("radius", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.5, 40.0))
							.executes(context -> {
								CommandSourceStack source = context.getSource();
								double radius = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "radius");
								Vec3 pos = getLookSpawnPos(source, 5.0);
								spawnRapidRift(source.getLevel(), pos.x, pos.y, pos.z, (float) radius, 5.0f);
								source.sendSuccess(() -> Component.literal("§dRapid Spacetime Rift (radius: " + radius + ", duration: 5.0s) spawned!"), true);
								return 1;
							})
							.then(Commands.argument("duration_seconds", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 7200))
								.executes(context -> {
									CommandSourceStack source = context.getSource();
									double radius = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "radius");
									int duration = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "duration_seconds");
									Vec3 pos = getLookSpawnPos(source, 5.0);
									spawnRapidRift(source.getLevel(), pos.x, pos.y, pos.z, (float) radius, (float) duration);
									source.sendSuccess(() -> Component.literal("§dRapid Spacetime Rift (radius: " + radius + ", duration: " + duration + "s) spawned!"), true);
									return 1;
								})
							)
						)
					)
					.then(Commands.literal("gravity")
						.then(Commands.literal("enable")
							.executes(ctx -> {
								GRAVITY_ENABLED = true;
								ctx.getSource().sendSuccess(() -> Component.literal("§aGeneral relativity gravity ENABLED!"), true);
								return 1;
							})
						)
						.then(Commands.literal("disable")
							.executes(ctx -> {
								GRAVITY_ENABLED = false;
								ctx.getSource().sendSuccess(() -> Component.literal("§cGeneral relativity gravity DISABLED!"), true);
								return 1;
							})
						)
					)
					.then(Commands.literal("explosion")
						.then(Commands.literal("enable")
							.executes(ctx -> {
								HAWKING_EXPLOSION_ENABLED = true;
								ctx.getSource().sendSuccess(() -> Component.literal("§aHawking radiation explosion ENABLED!"), true);
								return 1;
							})
						)
						.then(Commands.literal("disable")
							.executes(ctx -> {
								HAWKING_EXPLOSION_ENABLED = false;
								ctx.getSource().sendSuccess(() -> Component.literal("§cHawking radiation explosion DISABLED!"), true);
								return 1;
							})
						)
						.then(Commands.literal("block_damage")
							.then(Commands.argument("allow", com.mojang.brigadier.arguments.BoolArgumentType.bool())
								.executes(ctx -> {
									EXPLOSION_BLOCK_DESTRUCTION = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "allow");
									ctx.getSource().sendSuccess(() -> Component.literal("§eExplosion block damage set to: " + EXPLOSION_BLOCK_DESTRUCTION), true);
									return 1;
								})
							)
						)
						.then(Commands.literal("multiplier")
							.then(Commands.argument("mult", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.5, 10.0))
								.executes(ctx -> {
									EXPLOSION_RADIUS_MULT = (float) com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "mult");
									ctx.getSource().sendSuccess(() -> Component.literal("§eExplosion radius multiplier set to: " + EXPLOSION_RADIUS_MULT), true);
									return 1;
								})
							)
						)
					)
					.then(Commands.literal("clear")
						.executes(context -> {
							CommandSourceStack source = context.getSource();
							clearBlackHoles(source.getLevel());
							source.sendSuccess(() -> Component.literal("§cAll Singularities & Gravity Rifts cleared!"), true);
							return 1;
						})
					)
			);
		}

		private static Vec3 getLookSpawnPos(CommandSourceStack source, double distance) {
			Vec3 pos = source.getPosition();
			if (source.getEntity() != null) {
				Vec3 look = source.getEntity().getLookAngle();
				pos = pos.add(0, source.getEntity().getEyeHeight(), 0).add(look.scale(distance));
			}
			return pos;
		}

		private static void clearBlackHoles(Level level) {
			AABB searchBox = new AABB(-10000, -100, -10000, 10000, 500, 10000);
			List<AreaEffectCloud> clouds = level.getEntitiesOfClass(AreaEffectCloud.class, searchBox, e -> BlackHole.isBlackHole(e) || BlackHole.isShockwave(e));
			for (AreaEffectCloud cloud : clouds) {
				cloud.discard();
			}
		}

		@SubscribeEvent
		public static void onLevelTick(LevelTickEvent.Post event) {
			if (!MASTER_ENABLED) return;
			Level level = event.getLevel();
			if (level.isClientSide()) return;

			AABB searchBox = new AABB(-10000, -100, -10000, 10000, 500, 10000);
			List<AreaEffectCloud> clouds = level.getEntitiesOfClass(AreaEffectCloud.class, searchBox, e -> BlackHole.isBlackHole(e) || BlackHole.isShockwave(e));
			if (clouds.isEmpty()) return;

			for (AreaEffectCloud cloud : clouds) {
				if (!cloud.isAlive() || cloud.isRemoved()) continue;

				if (BlackHole.isShockwave(cloud)) {
					tickExpandingShockwave(level, cloud);
					continue;
				}

				int maxTicks = BlackHole.getMaxTicks(cloud);

				if (level instanceof ServerLevel serverLevel && cloud.tickCount % 60 == 0) {
					LowSimDistanceSolution.forceLoadRiftArea(serverLevel, cloud.blockPosition());
				}

				if (cloud.tickCount >= maxTicks) {
					if (level instanceof ServerLevel serverLevel) {
						LowSimDistanceSolution.unforceRiftArea(serverLevel, cloud.blockPosition());
					}
					triggerHawkingExplosion(level, cloud);
					cloud.discard();
					continue;
				}

				float ageProgress = (float) cloud.tickCount / (float) maxTicks;
				float decayFactor = 1.0f;
				if (ageProgress > 0.85f) {
					float rem = Math.max(0.0f, (1.0f - ageProgress) / 0.15f);
					decayFactor = rem * rem;
				}

				double radius = BlackHole.getDynamicInterpolatedRadius(cloud);

				if (getMode(cloud) == MODE_VERDANT_SPAWN_RIFT) {
					if (cloud.tickCount >= 60 && !cloud.getTags().contains("verdant_spawned")) {
						cloud.addTag("verdant_spawned");
						if (level instanceof ServerLevel serverLevel) {
							try {
								Entity verdant = net.mcreator.thebackwoods.init.TheBackwoodsModEntities.VERDANT_ENGINE.get().create(serverLevel);
								if (verdant != null) {
									verdant.setPos(cloud.getX(), cloud.getY(), cloud.getZ());
									serverLevel.addFreshEntity(verdant);
									serverLevel.playSound(null, cloud.getX(), cloud.getY(), cloud.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 15.0F, 0.8F);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}

				if (getMode(cloud) == MODE_BLACK_HOLE && cloud.tickCount % 12 == 0) {
					float pitch = 1.3f + (level.getRandom().nextFloat() * 0.2f);
					float volShriek = Math.max(16.0f, (float) radius * 6.0f) * decayFactor;
					float volFire = Math.max(8.0f, (float) radius * 3.0f) * decayFactor;
					level.playSound(null, cloud.getX(), cloud.getY(), cloud.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.AMBIENT, volShriek, pitch);
					level.playSound(null, cloud.getX(), cloud.getY(), cloud.getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.AMBIENT, volFire, 1.3f);
				}

				if (GRAVITY_ENABLED && getMode(cloud) == MODE_BLACK_HOLE) {
					if (level instanceof ServerLevel serverLevel) {
						pullBlocksAndSpawnFallingEntities(serverLevel, cloud, radius);
					}

					double maxDist = radius * 5.0;
					AABB pullBox = new AABB(cloud.getX() - maxDist, cloud.getY() - maxDist, cloud.getZ() - maxDist, cloud.getX() + maxDist, cloud.getY() + maxDist, cloud.getZ() + maxDist);
					List<Entity> nearbyEntities = level.getEntities((Entity) null, pullBox, e -> e.isAlive() && !e.isRemoved() && !isImmuneBoss(e) && !isBlackHole(e) && !isShockwave(e));
					for (Entity entity : nearbyEntities) {
						applyGravityToEntity(entity, cloud, decayFactor);
					}
				}
			}
		}

		public static void pullBlocksAndSpawnFallingEntities(ServerLevel level, AreaEffectCloud cloud, double radius) {
			if (!EXPLOSION_BLOCK_DESTRUCTION) return;
			if (cloud.tickCount % 2 != 0) return;

			double cx = cloud.getX();
			double cy = cloud.getY();
			double cz = cloud.getZ();

			double suctionInnerR = Math.max(1.0, radius * 0.8);
			double suctionOuterR = Math.max(2.5, radius * 2.5);

			// Optimization: Limit active falling blocks to 50 max to ensure zero server/rendering lag
			List<net.minecraft.world.entity.item.FallingBlockEntity> activeBlocks = level.getEntitiesOfClass(
				net.minecraft.world.entity.item.FallingBlockEntity.class,
				new AABB(cx - suctionOuterR, cy - suctionOuterR, cz - suctionOuterR, cx + suctionOuterR, cy + suctionOuterR, cz + suctionOuterR)
			);
			if (activeBlocks.size() >= 50) return;

			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int maxBlocksToPullPerTick = Math.max(4, (int) (radius * 4 * PARTICLE_QUALITY));
			int pulledCount = 0;

			// Physics-Based Radial Shell Sweep: Nearest blocks facing the black hole checked FIRST
			double shellStep = 0.8;
			int totalShells = (int) Math.ceil((suctionOuterR - suctionInnerR) / shellStep);

			for (int s = 0; s < totalShells && pulledCount < maxBlocksToPullPerTick; s++) {
				double rMin = suctionInnerR + (s * shellStep);
				double rMax = Math.min(suctionOuterR, rMin + shellStep);

				for (int i = 0; i < 12 && pulledCount < maxBlocksToPullPerTick; i++) {
					double theta = level.getRandom().nextDouble() * Math.PI * 2;
					double phi = (level.getRandom().nextDouble() - 0.5) * Math.PI;
					double r = rMin + level.getRandom().nextDouble() * (rMax - rMin);

					int bx = (int) Math.floor(cx + r * Math.cos(phi) * Math.cos(theta));
					int by = (int) Math.floor(cy + r * Math.sin(phi));
					int bz = (int) Math.floor(cz + r * Math.cos(phi) * Math.sin(theta));

					if (by <= level.getMinBuildHeight() || by >= level.getMaxBuildHeight()) continue;

					pos.set(bx, by, bz);
					if (!level.hasChunkAt(pos)) continue;

					BlockState state = level.getBlockState(pos);
					if (state.isAir() || state.getBlock() == Blocks.BEDROCK || state.getBlock() == Blocks.BARRIER) continue;

					// 1. Surface Erosion Physics: Must be exposed to air/void (no pulling hidden interior blocks behind solid terrain)
					boolean isExposed = false;
					for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
						if (level.isEmptyBlock(pos.relative(dir))) {
							isExposed = true;
							break;
						}
					}
					if (!isExposed) continue;

					// 2. Lightest/Least Resistant Blocks First Physics Weighting
					float hardness = state.getDestroySpeed(level, pos);
					if (hardness < 0.0f) continue;

					// Lighter blocks (dirt, leaves, sand, grass, wood, glass) pull easily; heavier stone/metal resists longer
					double pullProbability = Math.exp(-hardness * 0.40 * (r / suctionInnerR));
					if (level.getRandom().nextDouble() > pullProbability) continue;

					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);

					net.minecraft.world.entity.item.FallingBlockEntity fallingBlock = net.minecraft.world.entity.item.FallingBlockEntity.fall(level, pos.immutable(), state);
					if (fallingBlock != null) {
						fallingBlock.dropItem = false;
						fallingBlock.noPhysics = true;

						Vec3 pullDir = cloud.position().subtract(fallingBlock.position()).normalize();
						fallingBlock.setDeltaMovement(pullDir.scale(1.2));
						fallingBlock.hasImpulse = true;
					}

					pulledCount++;
				}
			}
		}

		public static void applyGravityToEntity(Entity entity, AreaEffectCloud cloud, float decayFactor) {
			if (!GRAVITY_ENABLED) return;
			if (entity == null || !entity.isAlive() || entity.isRemoved() || entity == cloud) return;
			if (isImmuneBoss(entity)) return;

			if (entity instanceof Player player) {
				if (player.isCreative() || player.isSpectator()) return;
			}

			double radius = BlackHole.getDynamicInterpolatedRadius(cloud);
			if (radius <= 0.05) radius = 1.2;

			double dist = entity.distanceTo(cloud);

			// Immediate Event Horizon Disintegration & Instant Despawn (Prevents Slingshotting beyond Schwarzschild boundary)
			if (getMode(cloud) == MODE_BLACK_HOLE && dist <= Math.max(1.5, radius * 1.15)) {
				if (entity instanceof LivingEntity living) {
					living.hurt(entity.damageSources().fellOutOfWorld(), 2000.0F);
				}
				if (!(entity instanceof Player)) {
					entity.discard();
					return;
				}
			}

			double maxDist = radius * 5.0;

			if (dist < maxDist && dist > 0.05) {
				double safeDist = Math.max(0.1, dist);
				Vec3 pullDir = cloud.position().subtract(entity.position()).normalize();

				// Falling Block & Item Entities: Strict Inward Velocity Trajectory (No Slingshotting / Bouncing)
				if (entity instanceof net.minecraft.world.entity.item.FallingBlockEntity || entity instanceof net.minecraft.world.entity.item.ItemEntity) {
					entity.noPhysics = true;
					double speed = Math.min(3.0, 1.0 + (radius * 0.4) + (1.2 / safeDist));
					entity.setDeltaMovement(pullDir.scale(speed));
					entity.hasImpulse = true;
					return;
				}

				double relDenominator = Math.max(0.02, 1.0 - (radius / safeDist));
				double pullForce = ((12.0 * radius) / (safeDist * safeDist)) * (1.0 / relDenominator) * (double) decayFactor;

				if (entity.onGround()) {
					pullDir = pullDir.add(0, 0.50, 0).normalize();
				}

				double proximityMultiplier = 1.0 / Math.sqrt(relDenominator);
				double baseCap = Math.max(1.8, 1.2 + (radius * 0.45));
				double terminalVelocityCap = Math.min(8.5, baseCap * proximityMultiplier);

				Vec3 motion = entity.getDeltaMovement();
				// Cancel out outward tangential momentum to prevent slingshot catapults
				double dot = motion.dot(pullDir);
				if (dot < 0) {
					motion = motion.subtract(pullDir.scale(dot * 0.85));
				}

				Vec3 newMotion = motion.add(pullDir.scale(pullForce));

				if (newMotion.length() > terminalVelocityCap) {
					newMotion = newMotion.normalize().scale(terminalVelocityCap);
				}

				entity.setDeltaMovement(newMotion);
				entity.hasImpulse = true;
				entity.hurtMarked = true;
			}
		}

		public static <T extends net.minecraft.core.particles.ParticleOptions> void sendFarParticles(ServerLevel level, T particle, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed) {
			if (level == null || count <= 0) return;
			int finalCount = Math.max(1, (int) Math.round(count * PARTICLE_QUALITY * 0.80f));
			double maxDistSq = LowSimDistanceSolution.PARTICLE_MAX_DIST_SQ;
			for (net.minecraft.server.level.ServerPlayer player : level.players()) {
				if (player.distanceToSqr(x, y, z) <= maxDistSq) {
					level.sendParticles(player, particle, true, x, y, z, finalCount, deltaX, deltaY, deltaZ, speed);
				}
			}
		}

		public static void triggerHawkingExplosion(Level level, AreaEffectCloud cloud) {
			if (level.isClientSide()) return;
			if (cloud.getTags().contains("BH_EXPLODED")) return;
			cloud.addTag("BH_EXPLODED");

			if (getMode(cloud) != MODE_BLACK_HOLE) return;
			if (!HAWKING_EXPLOSION_ENABLED) return;

			float radius = getRadius(cloud);
			if (radius <= 0.05f) radius = 2.5f;

			double cx = cloud.getX();
			double cy = cloud.getY();
			double cz = cloud.getZ();

			if (level instanceof ServerLevel serverLevel) {
				double coreVoidR = radius * 3.0;
				double blastCraterR = radius * 6.0;
				double damageR = radius * 9.0;
				double fireR = radius * 5.0;
				double flashR = radius * 12.0;
				float epicenterDmg = Math.max(150.0F, radius * 80.0F);
				float volume = Math.max(64.0F, radius * 12.0F);

				sendFarParticles(serverLevel, ParticleTypes.FLASH, cx, cy, cz, 16, 3.0, 3.0, 3.0, 0.0);
				sendFarParticles(serverLevel, ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 24, 6.0, 6.0, 6.0, 0.0);
				sendFarParticles(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, cx, cy, cz, (int) (50 + radius * 25), 3.0, 3.0, 3.0, 0.08);
				sendFarParticles(serverLevel, ParticleTypes.LARGE_SMOKE, cx, cy, cz, (int) (70 + radius * 30), 3.5, 3.5, 3.5, 0.20);
				sendFarParticles(serverLevel, ParticleTypes.SQUID_INK, cx, cy, cz, (int) (60 + radius * 28), 3.0, 3.0, 3.0, 0.25);

				serverLevel.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, volume, 0.40F);
				serverLevel.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, volume, 0.35F);
				serverLevel.playSound(null, cx, cy, cz, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, volume * 0.8F, 0.30F);
				serverLevel.playSound(null, cx, cy, cz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, volume * 0.7F, 0.50F);

				if (EXPLOSION_BLOCK_DESTRUCTION) {
					BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
					int minX = (int) Math.floor(cx - blastCraterR);
					int maxX = (int) Math.floor(cx + blastCraterR);
					int minY = Math.max((int) level.getMinBuildHeight() + 1, (int) Math.floor(cy - blastCraterR));
					int maxY = Math.min((int) level.getMaxBuildHeight() - 1, (int) Math.floor(cy + blastCraterR));
					int minZ = (int) Math.floor(cz - blastCraterR);
					int maxZ = (int) Math.floor(cz + blastCraterR);

					double coreVoidR2 = coreVoidR * coreVoidR;
					double blastCraterR2 = blastCraterR * blastCraterR;

					for (int bx = minX; bx <= maxX; bx++) {
						for (int by = minY; by <= maxY; by++) {
							for (int bz = minZ; bz <= maxZ; bz++) {
								double dx = (bx + 0.5) - cx;
								double dy = (by + 0.5) - cy;
								double dz = (bz + 0.5) - cz;
								double dist2 = dx * dx + dy * dy + dz * dz;

								if (dist2 <= blastCraterR2) {
									pos.set(bx, by, bz);
									if (!level.hasChunkAt(pos)) continue;
									BlockState st = level.getBlockState(pos);
									if (st.isAir()) continue;

									// Smooth core void cavity
									if (dist2 <= coreVoidR2) {
										serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
									} else {
										// Organic outer geological fracture bowl
										double dist = Math.sqrt(dist2);
										double rimProgress = (dist - coreVoidR) / (blastCraterR - coreVoidR);
										double noise = 0.5 + 0.22 * Math.sin(bx * 0.35 + bz * 0.3) * Math.cos(by * 0.3) + 0.15 * Math.cos(bx * 0.6 - by * 0.4) * Math.sin(bz * 0.5);

										if (rimProgress + (0.35 * (1.0 - noise)) < 0.70) {
											if (st.getBlock() != Blocks.BEDROCK) {
												serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
											}
										}
									}
								}
							}
						}
					}
				}

				AABB blastBox = new AABB(cx - damageR, cy - damageR, cz - damageR, cx + damageR, cy + damageR, cz + damageR);
				Vec3 epicenter = new Vec3(cx, cy, cz);

				for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, blastBox, e -> e.isAlive() && !isImmuneBoss(e))) {
					boolean isCreativeOrSpectator = (victim instanceof Player p && (p.isCreative() || p.isSpectator()));

					double dist = victim.position().distanceTo(epicenter);
					if (dist <= damageR) {
						double normDist = dist / damageR;
						float rawDmg = (float) (epicenterDmg * (1.0 - normDist));

						if (!isCreativeOrSpectator) {
							if (rawDmg > 0.0F) {
								victim.hurt(level.damageSources().explosion(null, null), rawDmg);
							}

							if (dist <= fireR) {
								victim.setRemainingFireTicks(200);
							}

							if (dist <= flashR) {
								victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 40, 0, false, false, true));
							}
						}

						// Powerful knockback impulse (applies to Creative players as well!)
						if (dist > 0.1) {
							Vec3 pushDir = victim.position().subtract(epicenter).normalize();
							double maxPushForce = Math.max(4.0, radius * 0.80);
							double pushForce = (1.0 - normDist) * maxPushForce;

							Vec3 currentVel = victim.getDeltaMovement();
							Vec3 knockbackVel = currentVel.add(pushDir.scale(pushForce));

							victim.setDeltaMovement(knockbackVel);
							victim.hasImpulse = true;
							victim.hurtMarked = true;

							if (victim instanceof net.minecraft.server.level.ServerPlayer sp) {
								sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
							}
						}
					}
				}

				AreaEffectCloud shockwave = new AreaEffectCloud(EntityType.AREA_EFFECT_CLOUD, level);
				shockwave.setPos(cx, cy, cz);
				shockwave.setRadius(0.0F);
				shockwave.setRadiusPerTick(0.0F);
				shockwave.setWaitTime(0);
				int totalShockTicks = Math.max(20, (int) (radius * 5.0f));
				shockwave.setDuration(totalShockTicks + 20);
				shockwave.addTag("BH_SHOCKWAVE_ENTITY");
				shockwave.addTag("BH_RAD_" + radius);
				shockwave.addTag("BH_SW_MAXTICKS_" + totalShockTicks);

				double randYaw = level.getRandom().nextDouble() * Math.PI * 2.0;
				double randPitch = (level.getRandom().nextDouble() - 0.5) * Math.PI * 0.85;
				double randRoll = level.getRandom().nextDouble() * Math.PI * 2.0;

				double nx = Math.cos(randYaw) * Math.cos(randPitch);
				double ny = Math.sin(randPitch);
				double nz = Math.sin(randYaw) * Math.cos(randPitch);
				Vec3 normal = new Vec3(nx, ny, nz).normalize();

				Vec3 arbitrary = Math.abs(normal.y) < 0.85 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
				Vec3 u = normal.cross(arbitrary).normalize();
				Vec3 v = normal.cross(u).normalize();

				Vec3 rotU = u.scale(Math.cos(randRoll)).add(v.scale(Math.sin(randRoll))).normalize();
				Vec3 rotV = normal.cross(rotU).normalize();

				shockwave.addTag("BH_UX_" + (float) rotU.x);
				shockwave.addTag("BH_UY_" + (float) rotU.y);
				shockwave.addTag("BH_UZ_" + (float) rotU.z);
				shockwave.addTag("BH_VX_" + (float) rotV.x);
				shockwave.addTag("BH_VY_" + (float) rotV.y);
				shockwave.addTag("BH_VZ_" + (float) rotV.z);

				shockwave.setCustomName(Component.literal("BH_SW:" + radius + ":" + totalShockTicks));
				shockwave.setCustomNameVisible(false);
				level.addFreshEntity(shockwave);
			}
		}

		public static void tickExpandingShockwave(Level level, AreaEffectCloud cloud) {
			if (!(level instanceof ServerLevel serverLevel)) return;

			float radius = BlackHole.getRadius(cloud);
			if (radius <= 0.05f) radius = 2.5f;

			int maxTicks = 24;
			for (String tag : cloud.getTags()) {
				if (tag.startsWith("BH_SW_MAXTICKS_")) {
					try {
						maxTicks = Integer.parseInt(tag.substring(15));
					} catch (Exception ignored) {}
				}
			}

			int tick = cloud.tickCount;
			if (tick >= maxTicks) {
				cloud.discard();
				return;
			}

			double cx = cloud.getX();
			double cy = cloud.getY();
			double cz = cloud.getZ();

			// Read randomized plane basis vectors
			double ux = 1.0, uy = 0.0, uz = 0.0;
			double vx = 0.0, vy = 0.0, vz = 1.0;
			for (String tag : cloud.getTags()) {
				if (tag.startsWith("BH_UX_")) {
					try { ux = Double.parseDouble(tag.substring(6)); } catch (Exception ignored) {}
				} else if (tag.startsWith("BH_UY_")) {
					try { uy = Double.parseDouble(tag.substring(6)); } catch (Exception ignored) {}
				} else if (tag.startsWith("BH_UZ_")) {
					try { uz = Double.parseDouble(tag.substring(6)); } catch (Exception ignored) {}
				} else if (tag.startsWith("BH_VX_")) {
					try { vx = Double.parseDouble(tag.substring(6)); } catch (Exception ignored) {}
				} else if (tag.startsWith("BH_VY_")) {
					try { vy = Double.parseDouble(tag.substring(6)); } catch (Exception ignored) {}
				} else if (tag.startsWith("BH_VZ_")) {
					try { vz = Double.parseDouble(tag.substring(6)); } catch (Exception ignored) {}
				}
			}
			Vec3 u = new Vec3(ux, uy, uz);
			Vec3 v = new Vec3(vx, vy, vz);

			float progress = (float) tick / (float) maxTicks;
			float nextProgress = (float) (tick + 1) / (float) maxTicks;

			double maxExpansionDist = Math.max(16.0, radius * 9.5);
			double currentR = maxExpansionDist * Math.pow(progress, 0.70);
			double nextR = maxExpansionDist * Math.pow(nextProgress, 0.70);
			double prevR = (tick > 0) ? maxExpansionDist * Math.pow((float) (tick - 1) / (float) maxTicks, 0.70) : 0.0;

			float intensity = Math.max(0.08f, 1.0f - progress);

			// 1. High-Density Randomized Tilted Shockwave Disc
			int ringPoints = (int) Math.max(36, currentR * 14);
			for (int i = 0; i < ringPoints; i++) {
				double angle = (2.0 * Math.PI * i) / ringPoints;
				Vec3 dir = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
				double px = cx + (dir.x * currentR);
				double py = cy + (dir.y * currentR);
				double pz = cz + (dir.z * currentR);

				if (radius >= 6.0f && i % 8 == 0) {
					sendFarParticles(serverLevel, ParticleTypes.SONIC_BOOM, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
				}

				sendFarParticles(serverLevel, ParticleTypes.POOF, px, py, pz, 1, dir.x * 0.45, dir.y * 0.45, dir.z * 0.45, 0.18);

				if (i % 2 == 0) {
					sendFarParticles(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 1, dir.x * 0.15, dir.y * 0.15 + 0.04, dir.z * 0.15, 0.04);
					sendFarParticles(serverLevel, ParticleTypes.LARGE_SMOKE, px, py, pz, 1, dir.x * 0.25, dir.y * 0.25 + 0.02, dir.z * 0.25, 0.12);
					sendFarParticles(serverLevel, ParticleTypes.SQUID_INK, px, py, pz, 1, dir.x * 0.20, dir.y * 0.20 + 0.02, dir.z * 0.20, 0.15);
				}
			}

			// 2. Spherical / Hemispherical Atmospheric Compression Shell
			double[] elevationAngles = {0.25, 0.50, 0.75, 1.05, 1.30};
			for (double phi : elevationAngles) {
				double domeRadius = currentR * Math.cos(phi);
				double domeYOffset = currentR * Math.sin(phi) * 0.70;
				int domePoints = (int) Math.max(16, domeRadius * 8);

				for (int j = 0; j < domePoints; j++) {
					double angle = (2.0 * Math.PI * j) / domePoints;
					double dirX = Math.cos(angle);
					double dirZ = Math.sin(angle);
					double dpx = cx + (dirX * domeRadius);
					double dpy = cy + domeYOffset;
					double dpz = cz + (dirZ * domeRadius);

					sendFarParticles(serverLevel, ParticleTypes.POOF, dpx, dpy, dpz, 1, dirX * 0.30, Math.sin(phi) * 0.25, dirZ * 0.30, 0.12);
					if (j % 2 == 0) {
						sendFarParticles(serverLevel, ParticleTypes.LARGE_SMOKE, dpx, dpy, dpz, 1, dirX * 0.15, Math.sin(phi) * 0.15, dirZ * 0.15, 0.08);
					}
				}
			}

			// 3. Radial High-Velocity Vacuum Debris Rays in Disc Plane
			int rayCount = 16;
			for (int r = 0; r < rayCount; r++) {
				double angle = (2.0 * Math.PI * r) / rayCount;
				Vec3 dir = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
				double rayStart = prevR;
				double rayEnd = currentR;
				for (double d = rayStart; d <= rayEnd; d += 1.2) {
					double rpx = cx + (dir.x * d);
					double rpy = cy + (dir.y * d);
					double rpz = cz + (dir.z * d);
					sendFarParticles(serverLevel, ParticleTypes.LARGE_SMOKE, rpx, rpy, rpz, 1, dir.x * 0.05, dir.y * 0.05, dir.z * 0.05, 0.05);
					serverLevel.sendParticles(ParticleTypes.POOF, rpx, rpy, rpz, 1, dir.x * 0.10, dir.y * 0.10, dir.z * 0.10, 0.06);
				}
			}

			// 4. Cascading Atmospheric Acoustic Boom
			if (tick % 6 == 0 && intensity > 0.15f) {
				float soundPitch = 0.45f + (progress * 0.30f);
				float soundVol = 16.0f * intensity;
				serverLevel.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, soundVol, soundPitch);
				serverLevel.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, soundVol * 0.85f, soundPitch);
			}

			// 5. Kinetic Concussion Wave hitting entities dynamically as the wavefront reaches them
			double waveFrontMin = Math.max(0.0, currentR - 2.5);
			double waveFrontMax = nextR + 2.5;
			AABB scanBox = new AABB(cx - waveFrontMax, cy - (currentR * 0.8), cz - waveFrontMax,
					cx + waveFrontMax, cy + (currentR * 0.8), cz + waveFrontMax);

			List<Entity> affected = serverLevel.getEntities((Entity) null, scanBox,
					e -> e.isAlive() && !e.getTags().contains("ROT_ENTITY") && !(e instanceof AreaEffectCloud));

			float maxDamage = Math.max(16.0F, radius * 26.0F);

			for (Entity target : affected) {
				double dist = target.distanceTo(cloud);
				if (dist >= waveFrontMin && dist <= waveFrontMax) {
					float falloff = (float) Math.max(0.05, 1.0 - (dist / maxExpansionDist));
					float damage = maxDamage * (falloff * falloff) + 4.0F;

					target.hurt(target.damageSources().explosion(null, null), damage);

					Vec3 blastDir = target.position().subtract(cloud.position());
					if (blastDir.lengthSqr() < 0.001) blastDir = new Vec3(0, 1, 0);

					double pushMagnitude = (radius * 2.2 * falloff) + 1.2;
					Vec3 push = blastDir.normalize().scale(pushMagnitude).add(0, 0.85 * falloff, 0);
					target.setDeltaMovement(target.getDeltaMovement().scale(0.1).add(push));
					target.hasImpulse = true;
					target.hurtMarked = true;
				}
			}
		}
	}
}
// 1.21.1
