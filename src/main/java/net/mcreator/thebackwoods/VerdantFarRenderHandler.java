package net.mcreator.thebackwoods;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.mcreator.thebackwoods.entity.VerdantEngineEntity;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(value = Dist.CLIENT)
public class VerdantFarRenderHandler {

	public static boolean ENABLE_FAR_RENDERING = true;
	public static float DEFAULT_RENDER_DISTANCE_SCALE = 16.0F; // 16.0 = 1024 blocks
	public static double MAX_FAR_RENDER_DIST = 1024.0;
	public static double MAX_FAR_RENDER_DIST_SQ = 1024.0 * 1024.0;

	public static final java.util.List<String> FAR_RENDER_KEYWORDS = new java.util.ArrayList<>(java.util.Arrays.asList("verdant", "verdant_engine", "fractus", "fractus_prime", "blackhole", "black_hole", "singularity", "rift"));

	// Client-side entity proxy registry (Wither Storm architecture):
	// Retains client entity references even when vanilla ChunkMap unloads/despawns them at >192 blocks (12 chunks)
	private static final Map<UUID, Entity> CLIENT_FAR_ENTITIES = new ConcurrentHashMap<>();

	static {
		try {
			NeoForge.EVENT_BUS.register(VerdantFarRenderHandler.class);
		} catch (Throwable ignored) {}
	}

	public static void registerFarRenderKeyword(String keyword) {
		if (keyword != null && !keyword.isEmpty() && !FAR_RENDER_KEYWORDS.contains(keyword.toLowerCase())) {
			FAR_RENDER_KEYWORDS.add(keyword.toLowerCase());
		}
	}

	/**
	 * Custom AABB that preserves exact physical spatial bounds (0 collision churn, 0 chunk section lag)
	 * while overriding getSize() to 128.0.
	 * 
	 * Minecraft's Entity.shouldRenderAtSqr(distSq) evaluates:
	 *   d0 = this.getBoundingBox().getSize() * 64.0 * getViewScale();
	 *   return distSq < d0 * d0;
	 * 
	 * With getSize() = 128.0:
	 *   d0 = 128.0 * 64.0 * 1.0 = 8,192 blocks!
	 * This completely defeats distance culling while keeping the physical hitbox identical.
	 */
	public static class FarRenderAABB extends AABB {
		public FarRenderAABB(AABB original) {
			super(original.minX, original.minY, original.minZ, original.maxX, original.maxY, original.maxZ);
		}

		public FarRenderAABB(double x1, double y1, double z1, double x2, double y2, double z2) {
			super(x1, y1, z1, x2, y2, z2);
		}

		@Override
		public double getSize() {
			return 128.0;
		}

		@Override
		public double getXsize() {
			return 128.0;
		}

		@Override
		public double getYsize() {
			return 128.0;
		}

		@Override
		public double getZsize() {
			return 128.0;
		}

		@Override
		public AABB move(double dx, double dy, double dz) {
			return new FarRenderAABB(super.move(dx, dy, dz));
		}

		@Override
		public AABB move(Vec3 vec) {
			return new FarRenderAABB(super.move(vec));
		}

		@Override
		public AABB move(net.minecraft.core.BlockPos pos) {
			return new FarRenderAABB(super.move(pos));
		}

		@Override
		public AABB setMinX(double minX) {
			return new FarRenderAABB(super.setMinX(minX));
		}

		@Override
		public AABB setMinY(double minY) {
			return new FarRenderAABB(super.setMinY(minY));
		}

		@Override
		public AABB setMinZ(double minZ) {
			return new FarRenderAABB(super.setMinZ(minZ));
		}

		@Override
		public AABB setMaxX(double maxX) {
			return new FarRenderAABB(super.setMaxX(maxX));
		}

		@Override
		public AABB setMaxY(double maxY) {
			return new FarRenderAABB(super.setMaxY(maxY));
		}

		@Override
		public AABB setMaxZ(double maxZ) {
			return new FarRenderAABB(super.setMaxZ(maxZ));
		}

		@Override
		public AABB inflate(double dx, double dy, double dz) {
			return new FarRenderAABB(super.inflate(dx, dy, dz));
		}

		@Override
		public AABB inflate(double value) {
			return new FarRenderAABB(super.inflate(value));
		}

		@Override
		public AABB deflate(double dx, double dy, double dz) {
			return new FarRenderAABB(super.deflate(dx, dy, dz));
		}

		@Override
		public AABB deflate(double value) {
			return new FarRenderAABB(super.deflate(value));
		}
	}

	public static void applyFarRender(Entity entity) {
		if (entity == null) return;

		// 1. Set entity view scale to 16.0 (1024 blocks culling radius)
		try {
			entity.setViewScale(DEFAULT_RENDER_DISTANCE_SCALE);
		} catch (Throwable ignored) {}

		// 2. Bypass frustum culling so camera angles never clip off the 15x model
		try {
			for (java.lang.reflect.Field f : Entity.class.getDeclaredFields()) {
				if (f.getType() == boolean.class && (f.getName().equals("noCulling") || f.getName().equals("f_19794_"))) {
					f.setAccessible(true);
					if (!f.getBoolean(entity)) {
						f.setBoolean(entity, true);
					}
					break;
				}
			}
		} catch (Throwable ignored) {}

		// 3. Wrap client bounding box in FarRenderAABB to permanently defeat Minecraft's distance culling
		AABB current = entity.getBoundingBox();
		if (current != null && !(current instanceof FarRenderAABB)) {
			entity.setBoundingBox(new FarRenderAABB(current));
		}
	}

	private static Entity getEntityFromEvent(Object event) {
		if (event == null) return null;
		try {
			if (event instanceof net.neoforged.neoforge.event.entity.EntityEvent ee) {
				return ee.getEntity();
			}
		} catch (Throwable ignored) {}
		try {
			java.lang.reflect.Method m = event.getClass().getMethod("getEntity");
			Object obj = m.invoke(event);
			if (obj instanceof Entity e) return e;
		} catch (Throwable ignored) {}
		try {
			for (java.lang.reflect.Method m : event.getClass().getMethods()) {
				if (m.getParameterCount() == 0 && Entity.class.isAssignableFrom(m.getReturnType())) {
					Object obj = m.invoke(event);
					if (obj instanceof Entity e) return e;
				}
			}
		} catch (Throwable ignored) {}
		return null;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityJoinClient(EntityJoinLevelEvent event) {
		if (ENABLE_FAR_RENDERING && event.getLevel().isClientSide() && shouldFarRender(event.getEntity())) {
			Entity e = event.getEntity();
			CLIENT_FAR_ENTITIES.put(e.getUUID(), e);
			applyFarRender(e);
		}
	}

	@SubscribeEvent
	public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		CLIENT_FAR_ENTITIES.clear();
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel().isClientSide()) {
			CLIENT_FAR_ENTITIES.clear();
		}
	}

	/**
	 * Custom Far Render Stage Pipeline:
	 * If the player moves beyond 192 blocks, Vanilla stops including the entity in `entitiesForRendering()`.
	 * This stage hook renders the cached entity directly via EntityRenderDispatcher in the skybox/world.
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (!ENABLE_FAR_RENDERING) return;
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;

		Camera camera = event.getCamera();
		Vec3 camPos = camera.getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

		float partialTick = 1.0F;
		try {
			partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
		} catch (Throwable ignored) {}

		Set<Integer> vanillaRenderedIds = new HashSet<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			vanillaRenderedIds.add(e.getId());
		}

		for (java.util.Iterator<Map.Entry<UUID, Entity>> it = CLIENT_FAR_ENTITIES.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Entity> entry = it.next();
			Entity entity = entry.getValue();

			// Only purge from cache if genuinely killed/discarded or transferred to a different dimension.
			// DO NOT purge when Minecraft unloads it at >192 blocks (UNLOADED_WITH_PLAYER / UNLOADED_TO_CHUNK)!
			if (entity == null || isGenuinelyDead(entity) || entity.level() != mc.level) {
				it.remove();
				continue;
			}

			// If vanilla is already actively rendering it within simulation distance, don't duplicate render
			if (vanillaRenderedIds.contains(entity.getId())) {
				continue;
			}

			// Beyond 1024 blocks -> skip
			double distSq = entity.distanceToSqr(camPos.x, camPos.y, camPos.z);
			if (distSq > MAX_FAR_RENDER_DIST_SQ) {
				continue;
			}

			// Manually dispatch render call for far entity
			try {
				poseStack.pushPose();
				double rx = Mth.lerp((double) partialTick, entity.xOld, entity.getX()) - camPos.x;
				double ry = Mth.lerp((double) partialTick, entity.yOld, entity.getY()) - camPos.y;
				double rz = Mth.lerp((double) partialTick, entity.zOld, entity.getZ()) - camPos.z;
				float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

				int packedLight = 0xF000F0; // Full bright illumination for distant celestial entities
				dispatcher.render(entity, rx, ry, rz, yaw, partialTick, poseStack, bufferSource, packedLight);
				poseStack.popPose();
			} catch (Throwable ignored) {
				try { poseStack.popPose(); } catch (Throwable ignored2) {}
			}
		}

		try {
			bufferSource.endBatch();
		} catch (Throwable ignored) {}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		if (!ENABLE_FAR_RENDERING) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (shouldFarRender(entity)) {
					applyFarRender(entity);
				}
			}
			for (Entity entity : CLIENT_FAR_ENTITIES.values()) {
				if (entity != null && !isGenuinelyDead(entity)) {
					applyFarRender(entity);
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onClientTick(ClientTickEvent.Post event) {
		if (!ENABLE_FAR_RENDERING) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (shouldFarRender(entity)) {
					CLIENT_FAR_ENTITIES.put(entity.getUUID(), entity);
					applyFarRender(entity);
				}
			}
			// Maintain animation ticks for entities outside vanilla rendering list
			for (Entity entity : CLIENT_FAR_ENTITIES.values()) {
				if (entity != null && !isGenuinelyDead(entity)) {
					applyFarRender(entity);
					try {
						entity.tickCount++;
					} catch (Throwable ignored) {}
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	@SuppressWarnings("rawtypes")
	public static void onRenderLivingPre(RenderLivingEvent.Pre event) {
		if (!ENABLE_FAR_RENDERING || event == null) return;
		Entity entity = getEntityFromEvent(event);
		if (shouldFarRender(entity)) {
			applyFarRender(entity);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	@SuppressWarnings("rawtypes")
	public static void onRenderLivingPost(RenderLivingEvent.Post event) {
		if (!ENABLE_FAR_RENDERING || event == null) return;
		Entity entity = getEntityFromEvent(event);
		if (shouldFarRender(entity)) {
			applyFarRender(entity);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onClientEntityTick(EntityTickEvent.Post event) {
		if (!ENABLE_FAR_RENDERING || event == null) return;
		Entity entity = event.getEntity();
		if (entity != null && entity.level().isClientSide() && shouldFarRender(entity)) {
			CLIENT_FAR_ENTITIES.put(entity.getUUID(), entity);
			applyFarRender(entity);
		}
	}

	public static boolean isGenuinelyDead(Entity entity) {
		if (entity == null) return true;
		if (entity instanceof net.minecraft.world.entity.LivingEntity living && (living.isDeadOrDying() || living.getHealth() <= 0.0F)) {
			return true;
		}
		Entity.RemovalReason reason = entity.getRemovalReason();
		if (reason != null) {
			return reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED;
		}
		return false;
	}

	public static boolean shouldFarRender(Entity entity) {
		if (entity == null) return false;
		if (entity instanceof VerdantEngineEntity) return true;
		String name = entity.getClass().getSimpleName().toLowerCase();
		for (String kw : FAR_RENDER_KEYWORDS) {
			if (name.contains(kw)) return true;
		}
		try {
			String regName = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().toLowerCase();
			for (String kw : FAR_RENDER_KEYWORDS) {
				if (regName.contains(kw)) return true;
			}
		} catch (Throwable ignored) {}
		return false;
	}
 // 1.21.1
}
