package net.mcreator.thebackwoods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.thebackwoods.entity.RotEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

@EventBusSubscriber(modid = "the_backwoods", value = Dist.CLIENT)
public class RotImpactFrames {
	public static boolean ENABLE_IMPACT_FRAMES = true; // Setting to enable/disable impact frames

	private static int impactTicks = 0;
	private static int maxImpactTicks = 0;
	private static int hurtCooldown = 0;
	private static int previousHurtTime = 0;

	public static void trigger(int ticks) {
		if (!ENABLE_IMPACT_FRAMES) {
			return;
		}
		impactTicks = ticks;
		maxImpactTicks = ticks;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.level == null) {
			return;
		}

		if (!ENABLE_IMPACT_FRAMES) {
			impactTicks = 0;
			return;
		}

		if (impactTicks > 0) {
			impactTicks--;
		}

		if (hurtCooldown > 0) {
			hurtCooldown--;
		}

		int currentHurtTime = minecraft.player.hurtTime;

		/*
		 * Detect the moment the player starts taking damage.
		 * hurtTime jumps above 0 when damage starts.
		 */
		boolean justGotHurt = currentHurtTime > 0 && previousHurtTime <= 0;

		previousHurtTime = currentHurtTime;

		if (!justGotHurt) {
			return;
		}

		if (hurtCooldown > 0) {
			return;
		}

		/*
		 * Find nearby Rot entities.
		 * This makes the impact frame happen only during Rot combat.
		 */
		java.util.List<RotEntity> rots = minecraft.level.getEntitiesOfClass(
				RotEntity.class,
				minecraft.player.getBoundingBox().inflate(12.0D)
		);

		if (!rots.isEmpty()) {
			boolean bestMoveTriggered = false;
			for (RotEntity rot : rots) {
				try {
					boolean isGroundCrushing = rot.getEntityData().get(RotEntity.DATA_is_ground_crushing);
					boolean isOverhead = rot.getEntityData().get(RotEntity.DATA_is_overhead);
					boolean isRiderCharging = rot.getEntityData().get(RotEntity.DATA_is_rider_charging);

					if (isGroundCrushing || isOverhead || isRiderCharging) {
						bestMoveTriggered = true;
						break;
					}
				} catch (Exception e) {
					// Fallback in case of missing EntityData or init lag
				}
			}

			if (bestMoveTriggered) {
				trigger(8); // High-impact premium 8-tick cinematic flash!
				hurtCooldown = 15;
			}
		}
	}

	@SubscribeEvent
	public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
		if (!ENABLE_IMPACT_FRAMES || impactTicks <= 0) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		LivingEntity entity = event.getEntity();
		if (entity == null) {
			return;
		}

		boolean isRot = entity instanceof RotEntity;
		boolean isPlayer = entity == minecraft.player;

		if (!isRot && !isPlayer) {
			return;
		}

		// Calculate fighting-game hand-drawn style alternating high-contrast solid colors
		int color;
		if (impactTicks >= 5) {
			// Phase A: rot is solid bright vibrant red, player is solid pure white
			color = isRot ? 0xFFFF0000 : 0xFFFFFFFF;
		} else if (impactTicks >= 3) {
			// Phase B: rot is solid pure black, player is solid bright vibrant red
			color = isRot ? 0xFF000000 : 0xFFFF0000;
		} else {
			// Phase C: transition back - rot is solid pure white, player is solid pure black
			color = isRot ? 0xFFFFFFFF : 0xFF000000;
		}

		event.setCanceled(true);

		try {
			// RenderType.eyes is fullbright, ignores light/shadows - perfect for solid flat silhouettes
			RenderType renderType = RenderType.eyes(getEntityTexture(event.getRenderer(), entity));
			VertexConsumer vertexConsumer = event.getMultiBufferSource().getBuffer(renderType);

			@SuppressWarnings("unchecked")
			EntityModel<LivingEntity> model = (EntityModel<LivingEntity>) event.getRenderer().getModel();
			model.renderToBuffer(
				event.getPoseStack(),
				vertexConsumer,
				event.getPackedLight(),
				OverlayTexture.NO_OVERLAY,
				color
			);
		} catch (Exception e) {
			// Safety escape callback
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends LivingEntity> ResourceLocation getEntityTexture(net.minecraft.client.renderer.entity.EntityRenderer<?> renderer, LivingEntity entity) {
		return ((net.minecraft.client.renderer.entity.EntityRenderer<T>) renderer).getTextureLocation((T) entity);
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		if (!ENABLE_IMPACT_FRAMES || impactTicks <= 0) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.level == null) {
			return;
		}

		GuiGraphics guiGraphics = event.getGuiGraphics();

		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();

		float progress = maxImpactTicks <= 0 ? 1.0F : impactTicks / (float) maxImpactTicks;

		int baseColor;

		if (impactTicks % 2 == 0) {
			baseColor = 0x00FFFFFF; // white
		} else {
			baseColor = 0x00000000; // black
		}

		// 75% opacity maximum so that the stylized in-world silhouettes show through the high-contrast flash backgrounds
		int alpha = Math.min(190, Math.max(0, (int) (progress * 190.0F)));
		int color = (alpha << 24) | (baseColor & 0x00FFFFFF);

		guiGraphics.fill(0, 0, width, height, color);
	} // custom element, 1.21.1
}