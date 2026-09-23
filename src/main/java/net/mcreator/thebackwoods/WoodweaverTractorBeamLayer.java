package net.mcreator.thebackwoods;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Element: WoodweaverTractorBeamLayer (Minecraft NeoForge 1.21.1)
 *
 * Authentic Wither Storm-style 4-sided square geometric tractor beam cone.
 * - True alpha blending / translucency (no solid opaque rendering)
 * - Configurable peak opacity and color constants
 * - Smooth client-side opacity transition (delayed fade-in, early fade-out)
 * - Exact single-vector target bounding box center tracking (no vertical drift during lift)
 * - Calibrated orb origin (height 4.2 in model space, forward 1.9)
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class WoodweaverTractorBeamLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

	private static final ResourceLocation PLAIN_TEXTURE = ResourceLocation.parse("minecraft:textures/misc/white.png");

	// =========================================================================
	// CONFIGURATION & CUSTOMIZATION SETTINGS
	// =========================================================================

	// 1. POSITION CALIBRATION (Blocks relative to Woodweaver entity model origin)
	public static double ORB_HEIGHT = 4.20;       // Vertical height of the chest orb center in model space
	public static double ORB_FORWARD = 1.90;      // Forward distance from entity origin to orb center

	// 2. TARGET AIM POINT CALIBRATION
	// Height ratio on target bounding box: 0.50 = exact geometric torso/center, 0.0 = feet, 1.0 = top of head
	public static double TARGET_AIM_HEIGHT_RATIO = 0.50;

	// 3. BEAM DIMENSIONS & PROPORTIONS
	public static float BEAM_START_HALF_SIZE = 0.28F; // Half-width at the orb (0.56x0.56 block square origin)
	public static float BEAM_SPREAD_RATE = 0.070F;    // Expansion rate per block of distance
	public static float MIN_BEAM_LENGTH = 45.0F;      // Minimum distance the beam shoots into the world

	// 4. COLOR & OPACITY
	// Opacity range: 0.0F (invisible) to 1.0F (completely solid). Recommended: 0.30F - 0.45F
	public static float BEAM_MAX_OPACITY = 0.38F;
	public static float COLOR_RED = 1.00F;        // Red channel (0.0 to 1.0)
	public static float COLOR_GREEN = 0.88F;      // Green channel (0.0 to 1.0)
	public static float COLOR_BLUE = 0.30F;       // Blue channel (0.0 to 1.0)

	// 5. TIMING & FADE TRANSITION (In ticks, 20 ticks = 1 second)
	// Opening phase is 60 ticks (3.0s). Hold phase is 160 ticks (8.0s). Total cycle = 220 ticks.
	public static int FADE_IN_START_TICK = 35;    // Beam stays hidden until tick 35, then fades in
	public static int FADE_IN_FULL_TICK = 55;     // Reaches full BEAM_MAX_OPACITY at tick 55
	public static int FADE_OUT_START_TICK = 185;  // Starts fading out at tick 185 (before loop finishes)
	public static int FADE_OUT_END_TICK = 215;    // Completely faded out by tick 215
	public static int INTERRUPT_FADE_TICKS = 8;   // Rapid fade-out duration if hypnosis is canceled/closing

	// =========================================================================

	// Client-side tracking to ensure smooth frame-rate independent fade transitions
	private static class ClientHypnoTracker {
		int startTick;
		int lastActiveTick;
		boolean wasClosing;
		float currentRed = COLOR_RED;
		float currentGreen = COLOR_GREEN;
		float currentBlue = COLOR_BLUE;
		int closingStartTick = -1;
	}

	private static final Map<Integer, ClientHypnoTracker> CLIENT_TRACKERS = new HashMap<>();

	public WoodweaverTractorBeamLayer(RenderLayerParent<T, M> renderer) {
		super(renderer);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@SubscribeEvent
	public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
		for (var entityType : event.getEntityTypes()) {
			String descId = entityType.getDescriptionId().toLowerCase();
			if (descId.contains("woodweaver") || descId.contains("weaver")) {
				var renderer = event.getRenderer(entityType);
				if (renderer instanceof LivingEntityRenderer livingRenderer) {
					livingRenderer.addLayer(new WoodweaverTractorBeamLayer(livingRenderer));
				}
			}
		}
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
					   float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
					   float netHeadYaw, float headPitch) {

		// 1. Determine Hypnosis Active State from SynchedData or NBT
		CompoundTag tag = entity.getPersistentData();
		int hypnoPhase = tag.getInt("hypno_phase");
		int weaponFlashPhase = tag.getInt("weapon_flash_phase");
		boolean isHypnotizing = tag.getBoolean("is_hypnotizing");
		int stareTicks = tag.getInt("stare_ticks");
		boolean isInterruptedNbt = tag.getBoolean("hypno_interrupted");
		boolean isWeaponCloseNbt = tag.getBoolean("isWeaponClose");

		boolean isSyncedHypno = false;
		boolean isSyncedClosing = false;
		boolean isOrbLooping = false;
		try {
			for (var field : entity.getClass().getFields()) {
				String name = field.getName();
				if (name.contains("OrbLoop") || name.contains("isOrbLooping")) {
					Object accessor = field.get(null);
					if (accessor instanceof net.minecraft.network.syncher.EntityDataAccessor dataAccessor) {
						Object val = entity.getEntityData().get(dataAccessor);
						if (val instanceof Boolean b && b) {
							isOrbLooping = true;
						}
					}
				}
				if (name.contains("Hypnotiz") || name.contains("isHypno") || name.contains("OrbLoop")) {
					Object accessor = field.get(null);
					if (accessor instanceof net.minecraft.network.syncher.EntityDataAccessor dataAccessor) {
						Object val = entity.getEntityData().get(dataAccessor);
						if (val instanceof Boolean b && b) {
							isSyncedHypno = true;
						}
					}
				}
				if (name.contains("isClosing") || name.contains("DATA_isClosing") || name.contains("WeaponClose")) {
					Object accessor = field.get(null);
					if (accessor instanceof net.minecraft.network.syncher.EntityDataAccessor dataAccessor) {
						Object val = entity.getEntityData().get(dataAccessor);
						if (val instanceof Boolean b && b) {
							isSyncedClosing = true;
						}
					}
				}
			}
		} catch (Throwable ignored) {}

		boolean isActivelyHypnotizing = (hypnoPhase == 1 || hypnoPhase == 2 || weaponFlashPhase == 1 || weaponFlashPhase == 2 || isHypnotizing || isSyncedHypno || isOrbLooping) && !isSyncedClosing && !isWeaponCloseNbt && hypnoPhase != 3 && weaponFlashPhase != 3;
		boolean isClosingOrInterrupted = isSyncedClosing || isWeaponCloseNbt || hypnoPhase == 3 || weaponFlashPhase == 3 || isInterruptedNbt;

		int entityId = entity.getId();
		int currentTick = entity.tickCount;

		ClientHypnoTracker tracker = CLIENT_TRACKERS.get(entityId);

		if (!isActivelyHypnotizing && !isClosingOrInterrupted) {
			if (tracker != null && (currentTick - tracker.lastActiveTick > 20)) {
				CLIENT_TRACKERS.remove(entityId);
			}
			return;
		}

		if (tracker == null) {
			tracker = new ClientHypnoTracker();
			tracker.startTick = currentTick;
			tracker.lastActiveTick = currentTick;
			CLIENT_TRACKERS.put(entityId, tracker);
		}

		if (isActivelyHypnotizing) {
			tracker.lastActiveTick = currentTick;
			if (!isClosingOrInterrupted) {
				tracker.wasClosing = false;
				tracker.closingStartTick = -1;
			}
		}

		if (isClosingOrInterrupted && !tracker.wasClosing) {
			tracker.wasClosing = true;
			tracker.closingStartTick = currentTick;
		}

		// --- Dynamic Color Fading Logic ---
		float targetRed = COLOR_RED;
		float targetGreen = COLOR_GREEN;
		float targetBlue = COLOR_BLUE;

		String hypnoMode = (entity != null) ? entity.getPersistentData().getString("hypno_mode") : "";
		boolean isVaporize = (entity != null && entity.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING)) || "vaporize".equals(hypnoMode) || weaponFlashPhase > 0;
		boolean isIncinerate = (entity != null && entity.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)) || "incinerate".equals(hypnoMode) || (entity != null && entity.isOnFire());

		if (isVaporize) {
			// Flash Vaporize: Vibrant fiery Orange
			targetRed = 1.00F;
			targetGreen = 0.45F;
			targetBlue = 0.00F;
		} else if (isIncinerate) {
			// Incinerate: Deep intense glowing Red
			targetRed = 1.00F;
			targetGreen = 0.04F;
			targetBlue = 0.02F;
		} else {
			// Infect (Default): Classic Golden Yellow
			targetRed = 1.00F;
			targetGreen = 0.88F;
			targetBlue = 0.30F;
		}

		float lerpFactor = 0.18F;
		tracker.currentRed = Mth.lerp(lerpFactor, tracker.currentRed, targetRed);
		tracker.currentGreen = Mth.lerp(lerpFactor, tracker.currentGreen, targetGreen);
		tracker.currentBlue = Mth.lerp(lerpFactor, tracker.currentBlue, targetBlue);

		// 2. Compute Smooth Opacity Transition (0.0 to 1.0 multiplier)
		float alphaMultiplier = 0.0F;

		if (tracker.wasClosing && tracker.closingStartTick > 0) {
			float closingProgress = (currentTick - tracker.closingStartTick + partialTicks) / (float) INTERRUPT_FADE_TICKS;
			alphaMultiplier = 1.0F - Mth.clamp(closingProgress, 0.0F, 1.0F);
		} else if (isOrbLooping || hypnoPhase == 2 || weaponFlashPhase == 2) {
			// Continuous full beam rendering during active hypnosis hold / weaponized flash!
			alphaMultiplier = 1.0F;
		} else if (isActivelyHypnotizing) {
			// Smooth opening fade-in during Phase 1
			float elapsedTicks = (currentTick - tracker.startTick) + partialTicks;
			alphaMultiplier = Mth.clamp(elapsedTicks / (float) FADE_IN_FULL_TICK, 0.0F, 1.0F);
		} else {
			alphaMultiplier = 0.0F;
		}

		float currentAlpha = BEAM_MAX_OPACITY * Mth.clamp(alphaMultiplier, 0.0F, 1.0F);
		if (currentAlpha <= 0.005F) {
			return;
		}

		// 3. Resolve Target Entity
		int targetId = tag.getInt("hypno_target_id");
		Entity targetEntity = null;
		if (targetId > 0 && entity.level() != null) {
			targetEntity = entity.level().getEntity(targetId);
		}
		if (targetEntity == null && entity instanceof Mob mob) {
			targetEntity = mob.getTarget();
		}
		if (targetEntity == null && entity.level() != null) {
			targetEntity = entity.level().getNearestPlayer(entity, 64.0);
		}

		// 4. Calculate Exact World Coordinates for True Orb Visual Origin and Target Aim Point
		float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
		float headYaw = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
		float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

		double weaverX = Mth.lerp(partialTicks, entity.xOld, entity.getX());
		double weaverY = Mth.lerp(partialTicks, entity.yOld, entity.getY());
		double weaverZ = Mth.lerp(partialTicks, entity.zOld, entity.getZ());

		double yawRad = Math.toRadians(bodyYaw);
		double forwardX = -Math.sin(yawRad) * ORB_FORWARD;
		double forwardZ = Math.cos(yawRad) * ORB_FORWARD;
		double orbWorldY = weaverY + 1.501 + ORB_HEIGHT;
		Vec3 orbWorldPos = new Vec3(weaverX + forwardX, orbWorldY, weaverZ + forwardZ);

		float beamLength = MIN_BEAM_LENGTH;
		float targetPitch = 0.0F;
		float targetYaw = 0.0F;

		if (targetEntity != null && targetEntity.isAlive()) {
			// Exact frame-interpolated target aim point (configurable ratio, default 0.50 = center of bounding box)
			double targetX = Mth.lerp(partialTicks, targetEntity.xOld, targetEntity.getX());
			double targetY = Mth.lerp(partialTicks, targetEntity.yOld, targetEntity.getY()) + (targetEntity.getBbHeight() * TARGET_AIM_HEIGHT_RATIO);
			double targetZ = Mth.lerp(partialTicks, targetEntity.zOld, targetEntity.getZ());
			Vec3 targetCenterPos = new Vec3(targetX, targetY, targetZ);

			// Single direction vector directly from orb origin to target center
			Vec3 toTarget = targetCenterPos.subtract(orbWorldPos);
			double totalDist = toTarget.length();
			beamLength = (float) Math.max(MIN_BEAM_LENGTH, totalDist + 24.0);

			double horizontalDist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
			targetPitch = (float) Math.toDegrees(-Math.atan2(toTarget.y, horizontalDist));
			targetYaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
		} else {
			targetPitch = pitch;
			targetYaw = headYaw;
		}

		// 5. Render Geometry in Model Space
		poseStack.pushPose();

		// Origin centered at chest orb (-Y is up in model space after 180 flip, -Z is forward)
		poseStack.translate(0.0, -ORB_HEIGHT, -ORB_FORWARD);

		// Aim towards target relative to current body rotation
		float relYaw = targetYaw - bodyYaw;
		float relPitch = targetPitch;

		poseStack.mulPose(Axis.YP.rotationDegrees(relYaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(relPitch));

		// True translucent glowing quad buffer (translucent = true enables alpha blending and disables depth blocking)
		VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.beaconBeam(PLAIN_TEXTURE, true));

		// End half-width based on beam length (clamped to prevent screen covering)
		float endSize = Math.min(2.5F, BEAM_START_HALF_SIZE + (beamLength * BEAM_SPREAD_RATE));

		renderSquarePyramidBeam(poseStack, vertexConsumer, BEAM_START_HALF_SIZE, endSize, beamLength,
				tracker.currentRed, tracker.currentGreen, tracker.currentBlue, currentAlpha);

		poseStack.popPose();
	}

	/**
	 * Renders a clean 4-sided square frustum/pyramid beam (Top, Bottom, Left, Right faces),
	 * double-sided so it is visible both from outside and when pulled inside the beam.
	 */
	private static void renderSquarePyramidBeam(PoseStack poseStack, VertexConsumer builder,
												float s, float e, float length,
												float r, float g, float b, float a) {

		Matrix4f matrix = poseStack.last().pose();

		// Coordinates in Model Space (-Z is forward, -Y is up, +Y is down, +X is right, -X is left):
		// Start Square (Z = 0)
		float sTop = -s;
		float sBottom = s;
		float sLeft = -s;
		float sRight = s;

		// End Square (Z = -length)
		float eTop = -e;
		float eBottom = e;
		float eLeft = -e;
		float eRight = e;

		float z0 = 0.0F;
		float z1 = -length;

		// 1. TOP FACE (Facing -Y up)
		addDoubleSidedQuad(matrix, poseStack, builder,
				sLeft, sTop, z0,
				sRight, sTop, z0,
				eRight, eTop, z1,
				eLeft, eTop, z1,
				0.0F, -1.0F, 0.0F, r, g, b, a);

		// 2. BOTTOM FACE (Facing +Y down)
		addDoubleSidedQuad(matrix, poseStack, builder,
				sRight, sBottom, z0,
				sLeft, sBottom, z0,
				eLeft, eBottom, z1,
				eRight, eBottom, z1,
				0.0F, 1.0F, 0.0F, r, g, b, a);

		// 3. LEFT FACE (Facing -X left)
		addDoubleSidedQuad(matrix, poseStack, builder,
				sLeft, sBottom, z0,
				sLeft, sTop, z0,
				eLeft, eTop, z1,
				eLeft, eBottom, z1,
				-1.0F, 0.0F, 0.0F, r, g, b, a);

		// 4. RIGHT FACE (Facing +X right)
		addDoubleSidedQuad(matrix, poseStack, builder,
				sRight, sTop, z0,
				sRight, sBottom, z0,
				eRight, eBottom, z1,
				eRight, eTop, z1,
				1.0F, 0.0F, 0.0F, r, g, b, a);
	}

	private static void addDoubleSidedQuad(Matrix4f matrix, PoseStack poseStack, VertexConsumer builder,
										   float x0, float y0, float z0,
										   float x1, float y1, float z1,
										   float x2, float y2, float z2,
										   float x3, float y3, float z3,
										   float nx, float ny, float nz,
										   float r, float g, float b, float a) {

		// Front Face
		builder.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), nx, ny, nz);
		builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), nx, ny, nz);
		builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), nx, ny, nz);
		builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), nx, ny, nz);

		// Back Face (Inside view)
		builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), -nx, -ny, -nz);
		builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), -nx, -ny, -nz);
		builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), -nx, -ny, -nz);
		builder.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), -nx, -ny, -nz);
	} // 1.21.1
}
