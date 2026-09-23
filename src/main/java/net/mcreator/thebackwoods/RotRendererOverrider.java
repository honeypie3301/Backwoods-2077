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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;
import com.mojang.blaze3d.vertex.PoseStack;

import net.mcreator.thebackwoods.entity.RotEntity;
import net.mcreator.thebackwoods.client.model.animations.RotAnimation;
import net.mcreator.thebackwoods.client.model.ModelRot;

@EventBusSubscriber(modid = "the_backwoods", value = Dist.CLIENT)
public class RotRendererOverrider {

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		EntityType<RotEntity> entityType = (EntityType<RotEntity>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("the_backwoods:rot"));
		if (entityType != null) {
			event.registerEntityRenderer(entityType, CustomRotRenderer::new);
		}
	}

	public static class CustomRotRenderer extends MobRenderer<RotEntity, CustomRotRenderer.AnimatedModel> {
		private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/rotskin.png");
		public static float phaseOpacity = 0.20f;

		public static float getPhaseOpacity(RotEntity entity) {
			if (entity != null && entity.getPersistentData().contains("rot_phase_opacity")) {
				return (float) entity.getPersistentData().getDouble("rot_phase_opacity");
			}
			return phaseOpacity;
		}

		public CustomRotRenderer(EntityRendererProvider.Context context) {
			super(context, new AnimatedModel(context.bakeLayer(ModelRot.LAYER_LOCATION)), 0.7f);
			this.addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this, context.getItemInHandRenderer()));
		}

		private net.minecraft.client.renderer.RenderType getTranslucentRenderType(net.minecraft.client.renderer.RenderType originalType) {
			if (originalType == null) {
				return originalType;
			}
			try {
				String str = originalType.toString();
				int start = str.indexOf("Optional[");
				if (start != -1) {
					int end = str.indexOf("]", start);
					if (end != -1) {
						String locStr = str.substring(start + 9, end);
						ResourceLocation loc = ResourceLocation.parse(locStr);
						return net.minecraft.client.renderer.RenderType.itemEntityTranslucentCull(loc);
					}
				}
			} catch (Exception ignored) {
			}
			return net.minecraft.client.renderer.RenderType.itemEntityTranslucentCull(entityTexture);
		}

		@Override
		public void render(RotEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight) {
			if (entity != null && entity.isInvisible()) {
				return;
			}
			if (net.mcreator.thebackwoods.procedures.RotIsEntityModelTransparentProcedure.execute(entity)) {
				float opacity = getPhaseOpacity(entity);
				net.minecraft.client.renderer.MultiBufferSource transparentBuffer = renderType -> {
					net.minecraft.client.renderer.RenderType translucentType = getTranslucentRenderType(renderType);
					return new AlphaVertexConsumer(bufferSource.getBuffer(translucentType), opacity);
				};
				super.render(entity, entityYaw, partialTicks, poseStack, transparentBuffer, packedLight);
			} else {
				super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
			}
		}

		@Override
		protected boolean isBodyVisible(RotEntity entity) {
			return super.isBodyVisible(entity);
		}

		@Override
		protected net.minecraft.client.renderer.RenderType getRenderType(RotEntity entity, boolean isBodyVisible, boolean translucent, boolean glowing) {
			if (net.mcreator.thebackwoods.procedures.RotIsEntityModelTransparentProcedure.execute(entity)) {
				return net.minecraft.client.renderer.RenderType.entityTranslucent(this.getTextureLocation(entity));
			}
			return super.getRenderType(entity, isBodyVisible, translucent, glowing);
		}

		@Override
		protected void scale(RotEntity entity, PoseStack poseStack, float f) {
			poseStack.scale(1.25f, 1.25f, 1.25f);
		}

		@Override
		public ResourceLocation getTextureLocation(RotEntity entity) {
			return entityTexture;
		}

		public static final class AnimatedModel extends ModelRot<RotEntity> implements net.minecraft.client.model.ArmedModel {
			private final ModelPart root;
			private final ModelPart rightArm;
			private final ModelPart leftArm;
			private final ModelPart trueRoot;
			private final ModelPart head;
			private final ModelPart body;
			private final ModelPart rightLeg;
			private final ModelPart leftLeg;
			private final HierarchicalModel animator = new HierarchicalModel<RotEntity>() {
				@Override
				public ModelPart root() {
					return root;
				}

					public final net.minecraft.world.entity.AnimationState localDeathAnimState = new net.minecraft.world.entity.AnimationState();

					@Override
					public void setupAnim(RotEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
						this.root().getAllParts().forEach(ModelPart::resetPose);
						boolean isHeavyLeftPunching = entity.getEntityData().get(RotEntity.DATA_is_heavy_left_punching);
						boolean isHeavyRightPunching = entity.getEntityData().get(RotEntity.DATA_is_heavy_right_punching);
						boolean isUppercuttingLeft = entity.getEntityData().get(RotEntity.DATA_is_uppercut_charging_left);
						boolean isUppercuttingRight = entity.getEntityData().get(RotEntity.DATA_is_uppercut_charging_right);
						boolean isHeavyPunching = isHeavyLeftPunching || isHeavyRightPunching;
						boolean isUppercuttingActive = isUppercuttingLeft || isUppercuttingRight;

						boolean isLanding1 = false;
						boolean isLanding2 = false;
						try {
							isLanding1 = entity.getEntityData().get(RotEntity.DATA_isLand);
						} catch (Exception e) {}
						try {
							isLanding2 = entity.getEntityData().get(RotEntity.DATA_isLand2);
						} catch (Exception e) {}

						if (isLanding1) {
							entity.animationState21.startIfStopped(entity.tickCount);
						} else {
							entity.animationState21.stop();
						}

						if (isLanding2) {
							entity.animationState22.startIfStopped(entity.tickCount);
						} else {
							entity.animationState22.stop();
						}

						boolean isLanding = isLanding1 || isLanding2;

						if (isUppercuttingLeft) {
							entity.animationState20.startIfStopped(entity.tickCount);
						} else {
							entity.animationState20.stop();
						}

						if (isUppercuttingRight) {
							entity.animationState19.startIfStopped(entity.tickCount);
						} else {
							entity.animationState19.stop();
						}

						if (isHeavyLeftPunching) {
							entity.animationState17.startIfStopped(entity.tickCount);
						} else {
							entity.animationState17.stop();
						}

						if (isHeavyRightPunching) {
							entity.animationState18.startIfStopped(entity.tickCount);
						} else {
							entity.animationState18.stop();
						}

						boolean isDeath = false;
						try {
							isDeath = entity.getEntityData().get(RotEntity.DATA_isDeath);
						} catch (Exception e) {}
						if (!isDeath) {
							isDeath = entity.getPersistentData().getBoolean("rot_death_sequence_active");
						}

						if (isDeath) {
							entity.animationState23.startIfStopped(entity.tickCount);
							localDeathAnimState.startIfStopped(entity.tickCount);
						} else {
							entity.animationState23.stop();
							localDeathAnimState.stop();
						}

						// [FIXED] Mapped rot_rider_kick to state 0 and checked WalkProcedure for conditional walking to prevent overlaps.
						this.animate(entity.animationState0, RotAnimation.rot_rider_kick, ageInTicks, 1f);
						if (net.mcreator.thebackwoods.procedures.RotPlaybackConditionWalkProcedure.execute(entity) 
							&& !entity.getPersistentData().getBoolean("is_blocking")
							&& !isHeavyPunching
							&& !isUppercuttingActive
							&& !isLanding
							&& !isDeath)
							this.animateWalk(RotAnimation.rot_walk, limbSwing, limbSwingAmount, 2f, 500f);
						if (entity.getPersistentData().getBoolean("is_sonic_boom") || entity.getPersistentData().getBoolean("is_sonic_boom_large")) {
							if (AnimatedModel.this.rightArm != null) AnimatedModel.this.rightArm.resetPose();
							if (AnimatedModel.this.leftArm != null) AnimatedModel.this.leftArm.resetPose();
						}
						this.animate(entity.animationState2, RotAnimation.rot_air_time, ageInTicks, 1f);
						this.animate(entity.animationState3, RotAnimation.rot_overhead, ageInTicks, 1f);
						this.animate(entity.animationState4, RotAnimation.rot_slam_crush, ageInTicks, 1f);
						this.animate(entity.animationState5, RotAnimation.rot_left_punch, ageInTicks, 1f);
						this.animate(entity.animationState6, RotAnimation.rot_right_punch, ageInTicks, 1f);
						this.animate(entity.animationState7, RotAnimation.rot_open_mouth_laser, ageInTicks, 1f);
						this.animate(entity.animationState8, RotAnimation.rot_close_mouth_laser, ageInTicks, 1f);
						this.animate(entity.animationState9, RotAnimation.rot_slam_charge, ageInTicks, 1f);
						this.animate(entity.animationState10, RotAnimation.rot_fall, ageInTicks, 1f);
						this.animate(entity.animationState11, RotAnimation.rot_sonic_boom, ageInTicks, 1f);
						this.animate(entity.animationState12, RotAnimation.rot_sonic_boom_large, ageInTicks, 1f);
						this.animate(entity.animationState13, RotAnimation.rot_armor_rip, ageInTicks, 1f);
						this.animate(entity.animationState14, RotAnimation.rot_block, ageInTicks, 1f);
						this.animate(entity.animationState15, RotAnimation.rot_block_finish, ageInTicks, 1f);
						this.animate(entity.animationState16, RotAnimation.rot_dropkick_charge, ageInTicks, 1f);
						this.animate(entity.animationState17, RotAnimation.rot_heavy_left_punch, ageInTicks, 1f);
						this.animate(entity.animationState18, RotAnimation.rot_heavy_right_punch, ageInTicks, 1f);
						this.animate(entity.animationState19, RotAnimation.rot_uppercut_charge_right, ageInTicks, 1f);
						this.animate(entity.animationState20, RotAnimation.rot_uppercut_charge_left, ageInTicks, 1f);
						this.animate(entity.animationState21, RotAnimation.rot_land, ageInTicks, 1f);
						this.animate(entity.animationState22, RotAnimation.rot_land_2, ageInTicks, 1f);
						this.animate(entity.animationState23, RotAnimation.rot_death, ageInTicks, 1f);
						this.animate(localDeathAnimState, RotAnimation.rot_death, ageInTicks, 1f);

				}
			};

			public AnimatedModel(ModelPart root) {
				super(root);
				this.root = root;
				ModelPart rArm = null;
				ModelPart lArm = null;
				ModelPart tRoot = null;
				try {
					tRoot = root.getChild("root");
					rArm = tRoot.getChild("right_arm");
				} catch (Exception e) {
					try {
						rArm = root.getChild("right_arm");
					} catch (Exception e2) {
						try {
							rArm = root.getChild("rightArm");
						} catch (Exception e3) {}
					}
				}
				try {
					if (tRoot != null) {
						lArm = tRoot.getChild("left_arm");
					} else {
						lArm = root.getChild("root").getChild("left_arm");
					}
				} catch (Exception e) {
					try {
						lArm = root.getChild("left_arm");
					} catch (Exception e2) {
						try {
							lArm = root.getChild("leftArm");
						} catch (Exception e3) {}
					}
				}
				this.rightArm = rArm;
				this.leftArm = lArm;
				this.trueRoot = tRoot;
				
				ModelPart bPart = null;
				try {
					if (tRoot != null) {
						bPart = tRoot.getChild("body");
					} else {
						bPart = root.getChild("root").getChild("body");
					}
				} catch (Exception e) {
					try {
						bPart = root.getChild("body");
					} catch (Exception e2) {
						try {
							bPart = tRoot != null ? tRoot.getChild("torso") : root.getChild("torso");
						} catch (Exception e3) {
							try {
								bPart = tRoot != null ? tRoot.getChild("chest") : root.getChild("chest");
							} catch (Exception e4) {
								try {
									bPart = tRoot != null ? tRoot.getChild("waist") : root.getChild("waist");
								} catch (Exception e5) {}
							}
						}
					}
				}
				this.body = bPart;

				ModelPart hPart = null;
				try {
					if (tRoot != null) {
						hPart = tRoot.getChild("head");
					} else {
						hPart = root.getChild("root").getChild("head");
					}
				} catch (Exception e) {
					try {
						hPart = root.getChild("head");
					} catch (Exception e2) {
						try {
							if (bPart != null) hPart = bPart.getChild("head");
						} catch (Exception e3) {}
					}
				}
				if (hPart == null) {
					try {
						if (tRoot != null) hPart = tRoot.getChild("Head");
						else hPart = root.getChild("Head");
					} catch (Exception e4) {
						try {
							if (bPart != null) hPart = bPart.getChild("Head");
						} catch (Exception e5) {}
					}
				}
				this.head = hPart;

				ModelPart rlPart = null;
				ModelPart llPart = null;
				try {
					if (tRoot != null) {
						rlPart = tRoot.getChild("right_leg");
						llPart = tRoot.getChild("left_leg");
					} else {
						rlPart = root.getChild("root").getChild("right_leg");
						llPart = root.getChild("root").getChild("left_leg");
					}
				} catch (Exception e) {
					try {
						rlPart = root.getChild("right_leg");
						llPart = root.getChild("left_leg");
					} catch (Exception e2) {}
				}
				this.rightLeg = rlPart;
				this.leftLeg = llPart;
			}

			@Override
			public void setupAnim(RotEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
				
				boolean isHeavyLeftPunching = entity.getEntityData().get(RotEntity.DATA_is_heavy_left_punching);
				boolean isHeavyRightPunching = entity.getEntityData().get(RotEntity.DATA_is_heavy_right_punching);
				boolean isUppercuttingLeft = entity.getEntityData().get(RotEntity.DATA_is_uppercut_charging_left);
				boolean isUppercuttingRight = entity.getEntityData().get(RotEntity.DATA_is_uppercut_charging_right);
				boolean isHeavyPunching = isHeavyLeftPunching || isHeavyRightPunching;
				boolean isUppercuttingActive = isUppercuttingLeft || isUppercuttingRight;

				boolean isOverhead = entity.getPersistentData().getBoolean("is_overhead");
				boolean isFallingHeavy = entity.getPersistentData().getBoolean("is_falling_heavy");
				boolean isSonicBoom = entity.getPersistentData().getBoolean("is_sonic_boom");
				boolean isSonicBoomLarge = entity.getPersistentData().getBoolean("is_sonic_boom_large");
				boolean isBlocking = entity.getPersistentData().getBoolean("is_blocking");
				boolean isBlockingFinish = entity.getPersistentData().getBoolean("is_blocking_finish");
				boolean isLanding = false;
				try {
					isLanding = entity.getEntityData().get(RotEntity.DATA_isLand) || entity.getEntityData().get(RotEntity.DATA_isLand2);
				} catch (Exception e) {}
				if (!isLanding) {
					isLanding = entity.getPersistentData().getBoolean("isLand") || entity.getPersistentData().getBoolean("isLand2") || entity.getPersistentData().getDouble("rot_land_timer") > 0;
				}

				double armorRipTicks = entity.getPersistentData().getDouble("rot_armor_rip_ticks");
				boolean armorRipFreezingActive = entity.getPersistentData().getBoolean("is_armor_ripping") && armorRipTicks <= 115;

				boolean isDeath = false;
				try {
					isDeath = entity.getEntityData().get(RotEntity.DATA_isDeath);
				} catch (Exception e) {}
				if (!isDeath) {
					isDeath = entity.getPersistentData().getBoolean("rot_death_sequence_active");
				}

				boolean freezeHead = !isDeath && (isOverhead || isSonicBoom || isSonicBoomLarge || isBlocking || isBlockingFinish || isLanding);
				boolean freezeArms = !isDeath && (isSonicBoom || isSonicBoomLarge || isBlocking || isBlockingFinish || armorRipFreezingActive || isHeavyPunching || isUppercuttingActive || isOverhead || isFallingHeavy || isLanding);
				boolean freezeTorso = !isDeath && (armorRipFreezingActive || isHeavyPunching || isUppercuttingActive || isOverhead || isFallingHeavy || isLanding);
				boolean freezeLegs = !isDeath && (isUppercuttingActive || isOverhead || isFallingHeavy || isLanding);
				boolean freezeRoot = !isDeath && (isUppercuttingActive || isOverhead || isFallingHeavy || isLanding);

				float animHeadXRot = 0, animHeadYRot = 0, animHeadZRot = 0;
				if (this.head != null) {
					animHeadXRot = this.head.xRot;
					animHeadYRot = this.head.yRot;
					animHeadZRot = this.head.zRot;
				}

				float animRightArmXRot = 0, animRightArmYRot = 0, animRightArmZRot = 0;
				float animLeftArmXRot = 0, animLeftArmYRot = 0, animLeftArmZRot = 0;
				if (freezeArms) {
					if (this.rightArm != null) {
						animRightArmXRot = this.rightArm.xRot;
						animRightArmYRot = this.rightArm.yRot;
						animRightArmZRot = this.rightArm.zRot;
					}
					if (this.leftArm != null) {
						animLeftArmXRot = this.leftArm.xRot;
						animLeftArmYRot = this.leftArm.yRot;
						animLeftArmZRot = this.leftArm.zRot;
					}
				}

				float animTorsoXRot = 0, animTorsoYRot = 0, animTorsoZRot = 0;
				if (freezeTorso && this.body != null) {
					animTorsoXRot = this.body.xRot;
					animTorsoYRot = this.body.yRot;
					animTorsoZRot = this.body.zRot;
				}

				float animRightLegXRot = 0, animRightLegYRot = 0, animRightLegZRot = 0;
				float animLeftLegXRot = 0, animLeftLegYRot = 0, animLeftLegZRot = 0;
				if (freezeLegs) {
					if (this.rightLeg != null) {
						animRightLegXRot = this.rightLeg.xRot;
						animRightLegYRot = this.rightLeg.yRot;
						animRightLegZRot = this.rightLeg.zRot;
					}
					if (this.leftLeg != null) {
						animLeftLegXRot = this.leftLeg.xRot;
						animLeftLegYRot = this.leftLeg.yRot;
						animLeftLegZRot = this.leftLeg.zRot;
					}
				}

				float animRootXRot = 0, animRootYRot = 0, animRootZRot = 0;
				if (freezeRoot && this.trueRoot != null) {
					animRootXRot = this.trueRoot.xRot;
					animRootYRot = this.trueRoot.yRot;
					animRootZRot = this.trueRoot.zRot;
				}

				super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
				
				double dieKickPhase = entity.getPersistentData().getDouble("sentinel_die_kick_phase");
				if (dieKickPhase >= 1 && dieKickPhase <= 3) {
					if (this.trueRoot != null) {
						net.minecraft.world.phys.Vec3 vel = entity.getDeltaMovement();
						double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
						float divePitch;
						if (horiz > 0.1) {
							divePitch = (float)(-Math.atan2(vel.y, horiz));
						} else {
							divePitch = (float)Math.toRadians(entity.getXRot());
						}
						this.trueRoot.xRot = 0F;
						this.trueRoot.yRot = (float)(Math.PI / 2.0);
						this.trueRoot.zRot = divePitch;
					}
				} else {
					if (this.trueRoot != null && !freezeRoot) {
						this.trueRoot.xRot = 0F;
						this.trueRoot.yRot = 0F;
						this.trueRoot.zRot = 0F;
					}
				}

				if (this.head != null) {
					if (freezeHead) {
						this.head.xRot = animHeadXRot;
						this.head.yRot = animHeadYRot;
						this.head.zRot = animHeadZRot;
					} else {
						this.head.xRot = animHeadXRot + headPitch * ((float) Math.PI / 180F);
						this.head.yRot = animHeadYRot + netHeadYaw * ((float) Math.PI / 180F);
					}
				}

				if (freezeArms) {
					if (this.rightArm != null) {
						this.rightArm.xRot = animRightArmXRot;
						this.rightArm.yRot = animRightArmYRot;
						this.rightArm.zRot = animRightArmZRot;
					}
					if (this.leftArm != null) {
						this.leftArm.xRot = animLeftArmXRot;
						this.leftArm.yRot = animLeftArmYRot;
						this.leftArm.zRot = animLeftArmZRot;
					}
				}

				if (freezeTorso && this.body != null) {
					this.body.xRot = animTorsoXRot;
					this.body.yRot = animTorsoYRot;
					this.body.zRot = animTorsoZRot;
				}

				if (freezeLegs) {
					if (this.rightLeg != null) {
						this.rightLeg.xRot = animRightLegXRot;
						this.rightLeg.yRot = animRightLegYRot;
						this.rightLeg.zRot = animRightLegZRot;
					}
					if (this.leftLeg != null) {
						this.leftLeg.xRot = animLeftLegXRot;
						this.leftLeg.yRot = animLeftLegYRot;
						this.leftLeg.zRot = animLeftLegZRot;
					}
				}

				if (freezeRoot && this.trueRoot != null) {
					this.trueRoot.xRot = animRootXRot;
					this.trueRoot.yRot = animRootYRot;
					this.trueRoot.zRot = animRootZRot;
				}
			}

			@Override
			public void translateToHand(net.minecraft.world.entity.HumanoidArm arm, PoseStack poseStack) {
				if (this.trueRoot != null) {
					this.trueRoot.translateAndRotate(poseStack);
				}
				if (arm == net.minecraft.world.entity.HumanoidArm.RIGHT) {
					if (this.rightArm != null) {
						this.rightArm.translateAndRotate(poseStack);
						// Tweak holding position here:
						// X: left/right relative to arm (negative is right, so increase X to move right arm inward)
						// Y: up/down along arm (decrease to move item UP closer to shoulder/inside the fist; increase to make it hang lower)
						// Z: forward/backward (positive moves item forward)
						poseStack.translate(0.0605F, 0.00F, 0.0625F);
					}
				} else {
					if (this.leftArm != null) {
						this.leftArm.translateAndRotate(poseStack);
						// For the left arm, decrease X to move it inward
						poseStack.translate(-0.0645F, 0.00F, 0.0625F);
					}
				} // for rendering held items, must register new animations
			}
		} // 1.21.1
	}

	private static class AlphaVertexConsumer implements com.mojang.blaze3d.vertex.VertexConsumer {
		private final com.mojang.blaze3d.vertex.VertexConsumer delegate;
		private final float alphaMultiplier;

		public AlphaVertexConsumer(com.mojang.blaze3d.vertex.VertexConsumer delegate, float alphaMultiplier) {
			this.delegate = delegate;
			this.alphaMultiplier = alphaMultiplier;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r, int g, int b, int a) {
			delegate.setColor(r, g, b, Math.max(0, Math.min(255, (int)(a * alphaMultiplier))));
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setColor(int color) {
			int a = (color >> 24) & 0xFF;
			int r = (color >> 16) & 0xFF;
			int g = (color >> 8) & 0xFF;
			int b = color & 0xFF;
			int newA = Math.max(0, Math.min(255, (int)(a * alphaMultiplier)));
			int newColor = (newA << 24) | (r << 16) | (g << 8) | b;
			delegate.setColor(newColor);
			return this;
		}

		@Override
		public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
			int a = (color >> 24) & 0xFF;
			int r = (color >> 16) & 0xFF;
			int g = (color >> 8) & 0xFF;
			int b = color & 0xFF;
			int newA = Math.max(0, Math.min(255, (int)(a * alphaMultiplier)));
			int newColor = (newA << 24) | (r << 16) | (g << 8) | b;
			delegate.addVertex(x, y, z, newColor, u, v, overlay, light, normalX, normalY, normalZ);
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) {
			delegate.setUv(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) {
			delegate.setUv1(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) {
			delegate.setUv2(u, v);
			return this;
		}

		@Override
		public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) {
			delegate.setNormal(x, y, z);
			return this;
		}
	}
}
// 1.21.1
