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

import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import org.joml.Quaternionf;
import java.lang.reflect.Field;

@EventBusSubscriber
public class VerdantEngineGravityBeam {
	public static boolean MASTER_ENABLED = true;
	public static boolean USE_BEACON_TEXTURE = false;

	public VerdantEngineGravityBeam() {
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new VerdantEngineGravityBeam();
	}

	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
	}

	@EventBusSubscriber
	private static class VerdantEngineGravityBeamForgeBusEvents {
		@SubscribeEvent
		public static void serverLoad(ServerStartingEvent event) {
		}
	}

	@EventBusSubscriber(value = Dist.CLIENT)
	public static class VerdantEngineGravityClientRenderer {
		private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.parse("minecraft:textures/misc/white.png");
		private static final ResourceLocation BEACON_TEXTURE = ResourceLocation.parse("minecraft:textures/entity/beacon_beam.png");
		private static final java.util.Map<String, Field> SYNCED_DATA_FIELD_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

		private static ResourceLocation getBeamTexture() {
			return USE_BEACON_TEXTURE ? BEACON_TEXTURE : WHITE_TEXTURE;
		}

		private static class ClientRenderState {
			double localRingTravel = 0.0;
			double prevRingTravel = 0.0;
			int lastGameTick = -1;
			int beamActiveTicks = 0;
			int currentCycle = 0;
			int decayTicks = 0;
			double opacityMultiplier = 1.0;
			double targetOpacity = 1.0;
			double groundY = 0.0;

			ClientRenderState(int initialTicks) {
				this.beamActiveTicks = initialTicks;
			}

			void update(LivingEntity entity, int gameTick, boolean isCharging, boolean isActive) {
				if (gameTick != lastGameTick) {
					lastGameTick = gameTick;
					prevRingTravel = localRingTravel;
					if (isCharging) {
						beamActiveTicks = 0;
						opacityMultiplier = 1.0;
						targetOpacity = 1.0;
					} else if (isActive) {
						beamActiveTicks++;
						if (beamActiveTicks >= 40) {
							currentCycle = (beamActiveTicks - 40) % 240;
							if (currentCycle == 0 && beamActiveTicks > 40) {
								decayTicks = 50;
								targetOpacity = 1.0;
							} else if (decayTicks > 0) {
								decayTicks--;
							}
						}
						// Smoothly decay opacity by a random 1-3% every second (20 ticks)
						if (beamActiveTicks >= 40) {
							if ((beamActiveTicks - 40) % 20 == 0) {
								double randDecrease = 0.01 + entity.getRandom().nextDouble() * 0.02;
								targetOpacity = Math.max(0.1, targetOpacity - randDecrease);
							}
							opacityMultiplier += (targetOpacity - opacityMultiplier) * 0.05;
						}
						// Direct client-side block collision check every tick
						groundY = findClientGroundBelow(entity.level(), entity.getX(), entity.getZ(), entity.getY() + entity.getBbHeight() * 0.5);
					}
					if (beamActiveTicks >= 40 && currentCycle >= 160 && currentCycle < 240) {
						double chargeProg = (double) (currentCycle - 160) / (240 - 160);
						double easeQuint = Math.pow(chargeProg, 3.2);
						double ringSpeed = 0.008 + 0.075 * easeQuint;
						localRingTravel += ringSpeed;
					} else {
						prevRingTravel = 0.0;
						localRingTravel = 0.0;
					}
				}
			}
		}

		private static final java.util.Map<Integer, ClientRenderState> CLIENT_STATES = new java.util.concurrent.ConcurrentHashMap<>();

		private static LivingEntity getEntityFromEvent(Object event) {
			if (event == null) return null;
			try {
				java.lang.reflect.Method m = event.getClass().getMethod("getEntity");
				Object res = m.invoke(event);
				if (res instanceof LivingEntity le) return le;
			} catch (Throwable ignored) {}
			try {
				java.lang.reflect.Method m = event.getClass().getMethod("getRenderState");
				Object state = m.invoke(event);
				if (state != null) {
					try {
						java.lang.reflect.Method m2 = state.getClass().getMethod("getEntity");
						Object res = m2.invoke(state);
						if (res instanceof LivingEntity le) return le;
					} catch (Throwable ignored) {}
					try {
						Field f = state.getClass().getDeclaredField("entity");
						f.setAccessible(true);
						Object res = f.get(state);
						if (res instanceof LivingEntity le) return le;
					} catch (Throwable ignored2) {}
				}
			} catch (Throwable ignored3) {}
			return null;
		}

		private static void setNoCulling(Entity entity) {
			if (entity == null) return;
			try {
				Field f = Entity.class.getField("noCulling");
				f.setBoolean(entity, true);
			} catch (Throwable ignored) {
				try {
					Field f = Entity.class.getDeclaredField("noCulling");
					f.setAccessible(true);
					f.setBoolean(entity, true);
				} catch (Throwable ignored2) {}
			}
		}

		private static Boolean shadersActiveCached = null;
		private static long lastShaderCheckTime = 0;

		public static boolean isShaderActive() {
			long now = System.currentTimeMillis();
			if (shadersActiveCached != null && (now - lastShaderCheckTime < 1000)) {
				return shadersActiveCached;
			}
			boolean active = false;
			try {
				Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
				Object instance = irisApi.getMethod("getInstance").invoke(null);
				if (instance != null) {
					Object res = irisApi.getMethod("isShaderPackInUse").invoke(instance);
					if (res instanceof Boolean b && b) {
						active = true;
					}
				}
			} catch (Throwable ignored) {}
			if (!active) {
				try {
					Class<?> optifineConfig = Class.forName("net.optifine.Config");
					Object res = optifineConfig.getMethod("isShaders").invoke(null);
					if (res instanceof Boolean b && b) {
						active = true;
					}
				} catch (Throwable ignored) {}
			}
			if (!active) {
				try {
					Class<?> optifineConfig2 = Class.forName("Config");
					Object res = optifineConfig2.getMethod("isShaders").invoke(null);
					if (res instanceof Boolean b && b) {
						active = true;
					}
				} catch (Throwable ignored) {}
			}
			shadersActiveCached = active;
			lastShaderCheckTime = now;
			return active;
		}

		private static RenderType getGlowRenderType(ResourceLocation texture) {
			return isShaderActive() ? RenderType.entityTranslucent(texture) : RenderType.beaconBeam(texture, true);
		}

		private static boolean isVerdantEngine(Entity entity) {
			if (entity == null) return false;
			String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
			return id.equals("the_backwoods:verdant_engine") || id.endsWith("verdant_engine") || entity.getClass().getSimpleName().toLowerCase().contains("verdantengine");
		}

		private static boolean getSyncedCurrentlyTerraforming(Entity entity) {
			if (entity == null) return false;
			EntityDataAccessor<Boolean> acc = getEntityDataAccessor(entity, "currentlyTerraforming");
			if (acc != null) {
				try { return entity.getEntityData().get(acc); } catch (Exception ignored) {}
			}
			return entity.getPersistentData().contains("currentlyTerraforming") ? entity.getPersistentData().getBoolean("currentlyTerraforming") : false;
		}

		private static boolean getSyncedIsTerraforming(Entity entity) {
			if (entity == null) return false;
			EntityDataAccessor<Boolean> acc = getEntityDataAccessor(entity, "isTerraforming");
			if (acc != null) {
				try { return entity.getEntityData().get(acc); } catch (Exception ignored) {}
			}
			return entity.getPersistentData().contains("isTerraforming") ? entity.getPersistentData().getBoolean("isTerraforming") : false;
		}

		@SuppressWarnings("unchecked")
		private static <T> EntityDataAccessor<T> getEntityDataAccessor(Entity entity, String paramName) {
			if (entity == null || paramName == null) return null;
			String key = entity.getClass().getName() + ":" + paramName;
			Field field = SYNCED_DATA_FIELD_CACHE.get(key);
			if (field == null) {
				Class<?> cur = entity.getClass();
				while (cur != null && cur != Object.class) {
					for (Field f : cur.getDeclaredFields()) {
						if (EntityDataAccessor.class.isAssignableFrom(f.getType()) && (f.getName().equalsIgnoreCase("DATA_" + paramName) || f.getName().equalsIgnoreCase(paramName) || f.getName().toLowerCase().contains(paramName.toLowerCase()))) {
							f.setAccessible(true);
							SYNCED_DATA_FIELD_CACHE.put(key, f);
							field = f;
							break;
						}
					}
					if (field != null) break;
					cur = cur.getSuperclass();
				}
			}
			if (field != null) {
				try { return (EntityDataAccessor<T>) field.get(null); } catch (Exception ignored) {}
			}
			return null;
		}

		private static void renderDoubleSidedCylinder(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
													  long gameTimeSpin, int height, int color, float rOuter) {
			VertexConsumer builder = bufferSource.getBuffer(getGlowRenderType(texture));
			org.joml.Matrix4f matrix = poseStack.last().pose();

			float r = (float) ((color >> 16) & 255) / 255.0f;
			float g = (float) ((color >> 8) & 255) / 255.0f;
			float b = (float) (color & 255) / 255.0f;
			float a = (float) ((color >> 24) & 255) / 255.0f;

			int slices = 4;
			double angleStep = 2.0 * Math.PI / slices;
			double angleOffset = Math.PI / 4.0;

			for (int slice = 0; slice < slices; slice++) {
				double a0 = slice * angleStep + angleOffset;
				double a1 = (slice + 1) * angleStep + angleOffset;

				float cos0 = (float) Math.cos(a0);
				float sin0 = (float) Math.sin(a0);
				float cos1 = (float) Math.cos(a1);
				float sin1 = (float) Math.sin(a1);

				float x0 = cos0 * rOuter, z0 = sin0 * rOuter;
				float x1 = cos1 * rOuter, z1 = sin1 * rOuter;

				float u0 = (float) slice / slices;
				float u1 = (float) (slice + 1) / slices;
				float scroll = (float) (-gameTimeSpin * 0.05f);
				float v0 = scroll;
				float v1 = (float) height + scroll;

				addDoubleSidedVerticalQuad(matrix, poseStack, builder,
					x0, 0.0f, z0,
					x1, 0.0f, z1,
					x1, (float) height, z1,
					x0, (float) height, z0,
					u0, u1, v0, v1,
					r, g, b, a);
			}
		}

		private static void addDoubleSidedVerticalQuad(org.joml.Matrix4f matrix, PoseStack poseStack, VertexConsumer builder,
													   float x0, float y0, float z0,
													   float x1, float y1, float z1,
													   float x2, float y2, float z2,
													   float x3, float y3, float z3,
													   float u0, float u1, float v0, float v1,
													   float r, float g, float b, float a) {
			// Front Face (CCW)
			builder.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), 0.0f, 1.0f, 0.0f);
			builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), 0.0f, 1.0f, 0.0f);
			builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u1, v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), 0.0f, 1.0f, 0.0f);
			builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(u0, v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), 0.0f, 1.0f, 0.0f);

			// Back Face (CW)
			builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(u0, v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), 0.0f, -1.0f, 0.0f);
			builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u1, v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), 0.0f, -1.0f, 0.0f);
			builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), 0.0f, -1.0f, 0.0f);
			builder.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), 0.0f, -1.0f, 0.0f);
		}

		private static void renderSquareOnionRing(PoseStack poseStack, VertexConsumer builder,
												 float inR, float outR, float r, float g, float b, float a) {
			org.joml.Matrix4f matrix = poseStack.last().pose();

			// Segment 1 (North)
			addDoubleSidedQuad(matrix, poseStack, builder,
					-outR, 0.0f, -outR,
					-inR, 0.0f, -inR,
					inR, 0.0f, -inR,
					outR, 0.0f, -outR,
					0.0f, 1.0f, 0.0f, r, g, b, a);

			// Segment 2 (East)
			addDoubleSidedQuad(matrix, poseStack, builder,
					outR, 0.0f, -outR,
					inR, 0.0f, -inR,
					inR, 0.0f, inR,
					outR, 0.0f, inR,
					0.0f, 1.0f, 0.0f, r, g, b, a);

			// Segment 3 (South)
			addDoubleSidedQuad(matrix, poseStack, builder,
					outR, 0.0f, inR,
					inR, 0.0f, inR,
					-inR, 0.0f, inR,
					-outR, 0.0f, inR,
					0.0f, 1.0f, 0.0f, r, g, b, a);

			// Segment 4 (West)
			addDoubleSidedQuad(matrix, poseStack, builder,
					-outR, 0.0f, inR,
					-inR, 0.0f, inR,
					-inR, 0.0f, -inR,
					-outR, 0.0f, -outR,
					0.0f, 1.0f, 0.0f, r, g, b, a);
		}

		private static void addDoubleSidedQuad(org.joml.Matrix4f matrix, PoseStack poseStack, VertexConsumer builder,
											   float x0, float y0, float z0,
											   float x1, float y1, float z1,
											   float x2, float y2, float z2,
											   float x3, float y3, float z3,
											   float nx, float ny, float nz,
											   float r, float g, float b, float a) {
			// Front Face
			builder.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setUv(0.0f, 0.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), nx, ny, nz);
			builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(1.0f, 0.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), nx, ny, nz);
			builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1.0f, 1.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), nx, ny, nz);
			builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.0f, 1.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), nx, ny, nz);

			// Back Face
			builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.0f, 1.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), -nx, -ny, -nz);
			builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1.0f, 1.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), -nx, -ny, -nz);
			builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(1.0f, 0.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), -nx, -ny, -nz);
			builder.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setUv(0.0f, 0.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(poseStack.last(), -nx, -ny, -nz);
		}

		private static double findClientGroundBelow(Level level, double x, double z, double fromY) {
			int floorX = Mth.floor(x);
			int floorZ = Mth.floor(z);
			int startY = Mth.floor(fromY);
			if (startY > level.getMaxBuildHeight()) {
				startY = level.getMaxBuildHeight();
			}
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(floorX, startY, floorZ);
			double highestLeafY = -999.0;
			while (pos.getY() > level.getMinBuildHeight()) {
				BlockState state = level.getBlockState(pos);
				if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) {
					boolean isLeafOrFoliage = state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS)
						|| state.getBlock() == Blocks.SHORT_GRASS || state.getBlock() == Blocks.TALL_GRASS || state.getBlock() == Blocks.FERN || state.getBlock() == Blocks.LARGE_FERN
						|| state.getBlock() == Blocks.DEAD_BUSH || state.getBlock() == Blocks.VINE || state.getBlock() == Blocks.HANGING_ROOTS || state.getBlock() == Blocks.MOSS_CARPET;
					if (isLeafOrFoliage) {
						if (highestLeafY < -900.0) {
							highestLeafY = pos.getY() + 1.0;
						}
					} else {
						// Found solid ground!
						return pos.getY() + 1.0;
					}
				}
				pos.move(0, -1, 0);
			}
			if (highestLeafY > -900.0) {
				return highestLeafY;
			}
			return level.getMinBuildHeight() + 1.0;
		}

		@SubscribeEvent
		public static void onRenderLevelStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
			if (!MASTER_ENABLED) return;
			if (event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
			if (mc.level == null) return;

			MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

			for (Entity entity : mc.level.entitiesForRendering()) {
				if (!(entity instanceof LivingEntity livingEntity)) continue;
				if (!isVerdantEngine(livingEntity)) continue;

				// Disable frustum culling safely via reflection
				setNoCulling(livingEntity);

				boolean isCharging = getSyncedIsTerraforming(livingEntity);
				boolean isActive = getSyncedCurrentlyTerraforming(livingEntity);

				if (isCharging) {
					ClientRenderState clientState = CLIENT_STATES.computeIfAbsent(livingEntity.getId(), id -> new ClientRenderState(0));
					clientState.update(livingEntity, (int) livingEntity.level().getGameTime(), isCharging, isActive);
					continue;
				}

				if (!isActive) {
					CLIENT_STATES.remove(livingEntity.getId());
					continue;
				}

				ClientRenderState clientState = CLIENT_STATES.computeIfAbsent(livingEntity.getId(), id -> new ClientRenderState(40));
				Level level = livingEntity.level();
				long gameTime = level.getGameTime();
				clientState.update(livingEntity, (int) gameTime, isCharging, isActive);

				if (clientState.beamActiveTicks < 40) {
					continue;
				}

				float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);

				double entityX = Mth.lerp(partialTicks, livingEntity.xo, livingEntity.getX());
				double entityY = Mth.lerp(partialTicks, livingEntity.yo, livingEntity.getY());
				double entityZ = Mth.lerp(partialTicks, livingEntity.zo, livingEntity.getZ());

				double beamStartYOffset = 0.0;
				double coreY = entityY + livingEntity.getBbHeight() * 0.5 + beamStartYOffset;
				Vec3 origin = new Vec3(entityX, coreY, entityZ);

				double groundY = clientState.groundY;
				if (groundY <= level.getMinBuildHeight()) {
					groundY = findClientGroundBelow(level, origin.x, origin.z, origin.y);
				}

				double h = origin.y - groundY;
				if (h <= 0) continue;

				PoseStack poseStack = event.getPoseStack();

				int cycle = clientState.currentCycle;
				int decayTicks = clientState.decayTicks;

				double densityFactor = 0.0;
				if (cycle >= 160 && cycle < 240) {
					double chargeProg = (double) (cycle - 160) / (240 - 160);
					densityFactor = chargeProg * chargeProg * (3.0 - 2.0 * chargeProg);
				} else if (decayTicks > 0) {
					double decayProg = (double) decayTicks / 50.0;
					densityFactor = decayProg * decayProg * (3.0 - 2.0 * decayProg);
				}

				poseStack.pushPose();

				Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
				double renderStartX = origin.x - camPos.x;
				double renderStartY = origin.y - camPos.y;
				double renderStartZ = origin.z - camPos.z;
				poseStack.translate(renderStartX, renderStartY, renderStartZ);

				Quaternionf rot = new Quaternionf().rotationTo(0, 1, 0, 0, -1, 0);
				poseStack.mulPose(rot);

				int height = (int) Math.ceil(h);
				if (height < 1) height = 1;
				float scaleY = (float) (h / (double) height);
				poseStack.scale(1.0f, scaleY, 1.0f);

				float widthFactor = 1.0f;

				double currentTargetOpacity = 0.75;
				if (cycle >= 160 && cycle < 240) {
					double chargeProg = (double) (cycle - 160) / (240 - 160);
					currentTargetOpacity = 0.75 + 0.20 * chargeProg;
				} else if (decayTicks > 0) {
					double progress = 1.0 - ((double) decayTicks / 50.0);
					double easeOutQuart = 1.0 - Math.pow(1.0 - progress, 4.0);
					currentTargetOpacity = 0.95 - 0.20 * easeOutQuart;
				}

				int coreA = Mth.clamp((int) (255 * currentTargetOpacity * clientState.opacityMultiplier), 0, 255);
				int coreColor = (coreA << 24) | (255 << 16) | (255 << 8) | 255;
				float coreRadius = 1.944f * widthFactor;
				renderDoubleSidedCylinder(poseStack, bufferSource, getBeamTexture(), (long) (gameTime * 0.2), height, coreColor, coreRadius);

				int coronaA = Mth.clamp((int) ((130 + 125 * densityFactor) * (currentTargetOpacity / 0.95) * clientState.opacityMultiplier), 0, 255);
				int coronaColor = (coronaA << 24) | (255 << 16) | (255 << 8) | 255;
				float outerRadius = 3.0f * widthFactor;
				renderDoubleSidedCylinder(poseStack, bufferSource, getBeamTexture(), (long) (-gameTime * 0.2), height, coronaColor, outerRadius);

				poseStack.popPose();

				if (cycle >= 160 && cycle < 240) {
					double cycleProgress = Math.max(0.0, Math.min(1.0, ((double) (cycle - 160) + partialTicks) / 80.0));
					double ringTravel = cycleProgress * 0.8 + Math.pow(cycleProgress, 4.2) * 1.8;
					double chargeProg = (double) (cycle - 160) / (240 - 160);

					double corePulse = (ringTravel * 2.0) % 1.0;
					double coreRingR = (3.0 * 0.45) + corePulse * (3.0 * 0.5);
					float coreInR = (float) (coreRingR - 0.22);
					float coreOutR = (float) (coreRingR + 0.22);

					VertexConsumer builder = bufferSource.getBuffer(getGlowRenderType(getBeamTexture()));
					poseStack.pushPose();
					poseStack.translate(origin.x - camPos.x, origin.y - camPos.y, origin.z - camPos.z);
					poseStack.mulPose(new Quaternionf().rotationY((float) (ringTravel * Math.PI * 1.5)));
					renderSquareOnionRing(poseStack, builder, coreInR, coreOutR, 1.0f, 1.0f, 1.0f, (float) (0.90 * clientState.opacityMultiplier));
					poseStack.popPose();

					int totalTiers = 3;
					for (int tier = 0; tier < totalTiers; tier++) {
						double phase = (ringTravel + (tier / (double) totalTiers)) % 1.0;
						double ringY = origin.y - phase * h;

						double targetInR = (3.0 * 0.45) + (1.0 - phase) * (3.0 * 0.55);
						double birthProgress = Math.min(1.0, phase * 5.0);
						// Form in a manner of easeOutQuart strictly
						double easeOutQuartScale = 1.0 - Math.pow(1.0 - birthProgress, 4.0);

						double inR = targetInR * easeOutQuartScale;
						double outR = (targetInR + 0.45) * easeOutQuartScale;

						// Decay quickly at end of travel in a manner of easeOutExpo strictly
						double ringAlpha = 0.90;
						if (phase > 0.8) {
							double endProgress = (phase - 0.8) * 5.0;
							ringAlpha *= (endProgress >= 1.0) ? 0.0 : Math.pow(2.0, -10.0 * endProgress);
						}

						poseStack.pushPose();
						double ringRenderY = ringY - camPos.y;
						poseStack.translate(origin.x - camPos.x, ringRenderY, origin.z - camPos.z);
						poseStack.mulPose(new Quaternionf().rotationY((float) ((ringTravel + tier * 0.3) * Math.PI * 1.5)));

						VertexConsumer ringBuilder = bufferSource.getBuffer(getGlowRenderType(getBeamTexture()));
						renderSquareOnionRing(poseStack, ringBuilder, (float) inR, (float) outR, 1.0f, 1.0f, 1.0f, (float) (ringAlpha * clientState.opacityMultiplier));

						if (chargeProg > 0.45) {
							double midR = (inR + outR) * 0.5;
							poseStack.mulPose(new Quaternionf().rotationY((float) (-ringTravel * Math.PI * 2.0)));
							renderSquareOnionRing(poseStack, ringBuilder, (float) (midR - 0.15), (float) (midR + 0.15), 1.0f, 1.0f, 1.0f, (float) (ringAlpha * clientState.opacityMultiplier));
						}
						poseStack.popPose();
					}
				}
			}
			bufferSource.endBatch(RenderType.entityTranslucent(getBeamTexture()));
			bufferSource.endBatch(RenderType.beaconBeam(getBeamTexture(), false));
			bufferSource.endBatch(RenderType.beaconBeam(getBeamTexture(), true));
		}
	}
 // 1.21.1
}