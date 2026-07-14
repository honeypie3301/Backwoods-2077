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

		public CustomRotRenderer(EntityRendererProvider.Context context) {
			super(context, new AnimatedModel(context.bakeLayer(ModelRot.LAYER_LOCATION)), 0.7f);
			this.addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this, context.getItemInHandRenderer()));
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
			private final HierarchicalModel animator = new HierarchicalModel<RotEntity>() {
				@Override
				public ModelPart root() {
					return root;
				}

				@Override
				public void setupAnim(RotEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
					this.root().getAllParts().forEach(ModelPart::resetPose);
					boolean isHeavyLeftPunching = entity.getEntityData().get(RotEntity.DATA_is_heavy_left_punching);
					boolean isHeavyRightPunching = entity.getEntityData().get(RotEntity.DATA_is_heavy_right_punching);
					boolean isHeavyPunching = isHeavyLeftPunching || isHeavyRightPunching;

					// [FIXED] Mapped rot_rider_kick to state 0 and checked WalkProcedure for conditional walking to prevent overlaps.
					this.animate(entity.animationState0, RotAnimation.rot_rider_kick, ageInTicks, 1f);
					if (net.mcreator.thebackwoods.procedures.RotPlaybackConditionWalkProcedure.execute(entity) 
						&& !entity.getPersistentData().getBoolean("is_blocking")
						&& !isHeavyPunching)
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
					this.animate(entity.animationState16, RotAnimation.rot_uppercut_charge, ageInTicks, 1f);
					this.animate(entity.animationState17, RotAnimation.rot_dropkick_charge, ageInTicks, 1f);
					this.animate(entity.animationState18, RotAnimation.rot_heavy_left_punch, ageInTicks, 1f);
					this.animate(entity.animationState19, RotAnimation.rot_heavy_right_punch, ageInTicks, 1f);
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
					} catch (Exception e2) {}
				}
				this.head = hPart;

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
			}

			@Override
			public void setupAnim(RotEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
				
				boolean isHeavyLeftPunching = entity.getEntityData().get(RotEntity.DATA_is_heavy_left_punching);
				boolean isHeavyRightPunching = entity.getEntityData().get(RotEntity.DATA_is_heavy_right_punching);
				boolean isHeavyPunching = isHeavyLeftPunching || isHeavyRightPunching;

				float animHeadXRot = 0, animHeadYRot = 0, animHeadZRot = 0;
				boolean freezeHead = entity.getPersistentData().getBoolean("is_overhead") 
					|| entity.getPersistentData().getBoolean("is_falling_heavy") 
					|| entity.getPersistentData().getBoolean("is_sonic_boom") 
					|| entity.getPersistentData().getBoolean("is_sonic_boom_large")
					|| entity.getPersistentData().getBoolean("is_blocking")
					|| entity.getPersistentData().getBoolean("is_blocking_finish");
				if (freezeHead && this.head != null) {
					animHeadXRot = this.head.xRot;
					animHeadYRot = this.head.yRot;
					animHeadZRot = this.head.zRot;
				}

				double armorRipTicks = entity.getPersistentData().getDouble("rot_armor_rip_ticks");
				boolean armorRipFreezingActive = entity.getPersistentData().getBoolean("is_armor_ripping") && armorRipTicks <= 115;

				boolean freezeArms = entity.getPersistentData().getBoolean("is_sonic_boom")
					|| entity.getPersistentData().getBoolean("is_sonic_boom_large")
					|| entity.getPersistentData().getBoolean("is_blocking")
					|| entity.getPersistentData().getBoolean("is_blocking_finish")
					|| armorRipFreezingActive
					|| isHeavyPunching;
				float animRightArmX = 0, animRightArmY = 0, animRightArmZ = 0;
				float animLeftArmX = 0, animLeftArmY = 0, animLeftArmZ = 0;
				if (freezeArms) {
					if (this.rightArm != null) {
						animRightArmX = this.rightArm.xRot;
						animRightArmY = this.rightArm.yRot;
						animRightArmZ = this.rightArm.zRot;
					}
					if (this.leftArm != null) {
						animLeftArmX = this.leftArm.xRot;
						animLeftArmY = this.leftArm.yRot;
						animLeftArmZ = this.leftArm.zRot;
					}
				}

				boolean freezeTorso = armorRipFreezingActive || isHeavyPunching;
				float animTorsoX = 0, animTorsoY = 0, animTorsoZ = 0;
				if (freezeTorso && this.body != null) {
					animTorsoX = this.body.xRot;
					animTorsoY = this.body.yRot;
					animTorsoZ = this.body.zRot;
				}

				super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
				
				if (freezeHead && this.head != null) {
					this.head.xRot = animHeadXRot;
					this.head.yRot = animHeadYRot;
					this.head.zRot = animHeadZRot;
				}

				if (freezeArms) {
					if (this.rightArm != null) {
						this.rightArm.xRot = animRightArmX;
						this.rightArm.yRot = animRightArmY;
						this.rightArm.zRot = animRightArmZ;
					}
					if (this.leftArm != null) {
						this.leftArm.xRot = animLeftArmX;
						this.leftArm.yRot = animLeftArmY;
						this.leftArm.zRot = animLeftArmZ;
					}
				}

				if (freezeTorso && this.body != null) {
					this.body.xRot = animTorsoX;
					this.body.yRot = animTorsoY;
					this.body.zRot = animTorsoZ;
				}
				
				if (entity.getPersistentData().getBoolean("is_rider_kick") || entity.getPersistentData().getDouble("sentinel_die_kick_phase") >= 2) {
					if (this.trueRoot != null) {
						net.minecraft.world.phys.Vec3 vel = entity.getDeltaMovement();
						double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
						float divePitch = (float)(-Math.atan2(vel.y, horiz));
						this.trueRoot.xRot += divePitch;
					}
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
}