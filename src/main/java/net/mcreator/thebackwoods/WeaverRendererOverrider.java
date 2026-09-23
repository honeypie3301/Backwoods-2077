package net.mcreator.thebackwoods;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import net.mcreator.thebackwoods.entity.WoodweaverEntity;
import net.mcreator.thebackwoods.client.model.animations.WoodweaverAnimation;
import net.mcreator.thebackwoods.client.model.ModelWoodweaver;

/**
 * 1.21.1 Client-side renderer registration overrider for Woodweaver.
 * Employs an anonymous HierarchicalModel animator delegate to safely play MCreator keyframe animations in NeoForge 1.21.1.
 */
@EventBusSubscriber(modid = "the_backwoods", value = Dist.CLIENT)
public class WeaverRendererOverrider {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        EntityType<WoodweaverEntity> entityType = (EntityType<WoodweaverEntity>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("the_backwoods:woodweaver"));
        if (entityType != null) {
            event.registerEntityRenderer(entityType, CustomWoodweaverRenderer::new);
        }
    }

    public static class CustomWoodweaverRenderer extends MobRenderer<WoodweaverEntity, CustomWoodweaverRenderer.AnimatedModel> {
        private final ResourceLocation entityTexture = ResourceLocation.parse("the_backwoods:textures/entities/woodweaver_skin.png");
        public static boolean USE_PROCEDURAL_IK = false; // Master toggle to easily switch back to default keyframe animations if desired

        public CustomWoodweaverRenderer(EntityRendererProvider.Context context) {
            super(context, new AnimatedModel(context.bakeLayer(ModelWoodweaver.LAYER_LOCATION)), 3.0f);
        }

        @Override
        public ResourceLocation getTextureLocation(WoodweaverEntity entity) {
            return entityTexture;
        }

        public static final class AnimatedModel extends ModelWoodweaver<WoodweaverEntity> {
            private final ModelPart root;
            private final ModelPart localAll;

            // Re-fetch leg structures to bypass private access limits in parent class
            private final ModelPart localLeg16, localLeg17, localLeg18;
            private final ModelPart localLeg7,  localLeg8,  localLeg9;
            private final ModelPart localLeg13, localLeg14, localLeg15;
            private final ModelPart localLeg4,  localLeg5,  localLeg6;
            private final ModelPart localLeg10, localLeg11, localLeg12;
            private final ModelPart localLeg1,  localLeg2,  localLeg3;

            // Anonymous HierarchicalModel delegate animator matching RotRendererOverrider pattern
            private final net.minecraft.client.model.HierarchicalModel<WoodweaverEntity> animator = new net.minecraft.client.model.HierarchicalModel<WoodweaverEntity>() {
                @Override
                public ModelPart root() {
                    return root;
                }

                @Override
                public void setupAnim(WoodweaverEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
                    this.root().getAllParts().forEach(ModelPart::resetPose);

                    // Apply standard MCreator animations safely
                    if (net.mcreator.thebackwoods.procedures.WoodweaverPlaybackConditionWalkProcedure.execute(entity)) {
                        this.animateWalk(WoodweaverAnimation.Walk, limbSwing, limbSwingAmount, 1.3f, 1f);
                    }
                    
                    safeAnimate(entity.animationState1, WoodweaverAnimation.Hypnotize, ageInTicks);
                    safeAnimate(entity.animationState2, WoodweaverAnimation.Attack_left, ageInTicks);
                    safeAnimate(entity.animationState3, WoodweaverAnimation.Attack_right, ageInTicks);
                    safeAnimate(entity.animationState4, WoodweaverAnimation.Jump, ageInTicks);
                    safeAnimate(entity.animationState5, WoodweaverAnimation.Land, ageInTicks);
                    safeAnimate(entity.animationState6, WoodweaverAnimation.Orb, ageInTicks);
                    safeAnimate(entity.animationState7, WoodweaverAnimation.Hypnotize_reverse, ageInTicks);
                    safeAnimate(entity.animationState8, WoodweaverAnimation.Hypnotize_reverse_2, ageInTicks);
                    safeAnimate(entity.animationState9, WoodweaverAnimation.Hypnotize_reverse_3, ageInTicks);
                    safeAnimate(entity.animationState10, WoodweaverAnimation.Attack_both, ageInTicks);
                    safeAnimate(entity.animationState11, WoodweaverAnimation.Hook_Left, ageInTicks);
                    safeAnimate(entity.animationState12, WoodweaverAnimation.Hook_Right, ageInTicks);
                    safeAnimate(entity.animationState13, WoodweaverAnimation.Sweep_Right, ageInTicks);
                    safeAnimate(entity.animationState14, WoodweaverAnimation.Sweep_Left, ageInTicks);
                    safeAnimate(entity.animationState15, WoodweaverAnimation.Hypnotize_2, ageInTicks);
                    safeAnimate(entity.animationState16, WoodweaverAnimation.Hypnotize_3, ageInTicks);

                    if (net.mcreator.thebackwoods.procedures.WoodweaverPlaybackConditionRotateLeftProcedure.execute(entity)) {
                        this.animateWalk(WoodweaverAnimation.Rotate_left, limbSwing, limbSwingAmount, 1.0f, 1.0f);
                    }
                    if (net.mcreator.thebackwoods.procedures.WoodweaverPlaybackConditionRotateRightProcedure.execute(entity)) {
                        this.animateWalk(WoodweaverAnimation.Rotate_right, limbSwing, limbSwingAmount, 1.0f, 1.0f);
                    }
                    if (net.mcreator.thebackwoods.procedures.WoodweaverPlaybackConditionRetreatProcedure.execute(entity)) {
                        this.animateWalk(WoodweaverAnimation.Walk_backward, limbSwing, limbSwingAmount, 1.3f, 1.0f);
                    }

                    safeAnimate(entity.animationState21, WoodweaverAnimation.Death, ageInTicks);
                }

                private void safeAnimate(net.minecraft.world.entity.AnimationState state, net.minecraft.client.animation.AnimationDefinition animation, float ageInTicks) {
                    if (state == null || animation == null) return;
                    try {
                        this.animate(state, animation, ageInTicks, 1.0f);
                    } catch (Throwable e) {
                        // Safe fallback for animation states missing in current entity file
                    }
                }
            };

            public AnimatedModel(ModelPart root) {
                super(root);
                this.root = root;
                ModelPart allPart = root.getChild("all");
                this.localAll = allPart;

                // Locate and reference the individual nested model bone segments
                this.localLeg16 = allPart.getChild("leg16");
                this.localLeg17 = this.localLeg16.getChild("leg17");
                this.localLeg18 = this.localLeg17.getChild("leg18");

                this.localLeg7  = allPart.getChild("leg7");
                this.localLeg8  = this.localLeg7.getChild("leg8");
                this.localLeg9  = this.localLeg8.getChild("leg9");

                this.localLeg13 = allPart.getChild("leg13");
                this.localLeg14 = this.localLeg13.getChild("leg14");
                this.localLeg15 = this.localLeg14.getChild("leg15");

                this.localLeg4  = allPart.getChild("leg4");
                this.localLeg5  = this.localLeg4.getChild("leg5");
                this.localLeg6  = this.localLeg5.getChild("leg6");

                this.localLeg10 = allPart.getChild("leg10");
                this.localLeg11 = this.localLeg10.getChild("leg11");
                this.localLeg12 = this.localLeg11.getChild("leg12");

                this.localLeg1  = allPart.getChild("leg1");
                this.localLeg2  = this.localLeg1.getChild("leg2");
                this.localLeg3  = this.localLeg2.getChild("leg3");
            }

            @Override
            public void setupAnim(WoodweaverEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
                // 1. Run the keyframe animator delegate to animate all keyframe-driven parts
                this.animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

                // Run original model head rotations if defined
                super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

                // 2. Procedural Leg calculations override if entity is active and not dead/jumping/landing
                boolean isLeaping = entity.getEntityData().get(WoodweaverEntity.DATA_isLeaping);
                boolean isLanding = entity.getEntityData().get(WoodweaverEntity.DATA_isLanding);
                boolean isDead    = entity.getEntityData().get(WoodweaverEntity.DATA_isDead) || entity.isDeadOrDying();

                if (CustomWoodweaverRenderer.USE_PROCEDURAL_IK && !isDead && !isLeaping && !isLanding) {
                    // Reset leg joint rotations so standard animations don't compound with the IK solver
                    this.localLeg13.xRot = 0; this.localLeg13.yRot = 0; this.localLeg13.zRot = 0;
                    this.localLeg14.xRot = 0; this.localLeg14.yRot = 0; this.localLeg14.zRot = 0;
                    this.localLeg15.xRot = 0; this.localLeg15.yRot = 0; this.localLeg15.zRot = 0;

                    this.localLeg16.xRot = 0; this.localLeg16.yRot = 0; this.localLeg16.zRot = 0;
                    this.localLeg17.xRot = 0; this.localLeg17.yRot = 0; this.localLeg17.zRot = 0;
                    this.localLeg18.xRot = 0; this.localLeg18.yRot = 0; this.localLeg18.zRot = 0;

                    this.localLeg10.xRot = 0; this.localLeg10.yRot = 0; this.localLeg10.zRot = 0;
                    this.localLeg11.xRot = 0; this.localLeg11.yRot = 0; this.localLeg11.zRot = 0;
                    this.localLeg12.xRot = 0; this.localLeg12.yRot = 0; this.localLeg12.zRot = 0;

                    this.localLeg4.xRot = 0;  this.localLeg4.yRot = 0;  this.localLeg4.zRot = 0;
                    this.localLeg5.xRot = 0;  this.localLeg5.yRot = 0;  this.localLeg5.zRot = 0;
                    this.localLeg6.xRot = 0;  this.localLeg6.yRot = 0;  this.localLeg6.zRot = 0;

                    this.localLeg7.xRot = 0;  this.localLeg7.yRot = 0;  this.localLeg7.zRot = 0;
                    this.localLeg8.xRot = 0;  this.localLeg8.yRot = 0;  this.localLeg8.zRot = 0;
                    this.localLeg9.xRot = 0;  this.localLeg9.yRot = 0;  this.localLeg9.zRot = 0;

                    this.localLeg1.xRot = 0;  this.localLeg1.yRot = 0;  this.localLeg1.zRot = 0;
                    this.localLeg2.xRot = 0;  this.localLeg2.yRot = 0;  this.localLeg2.zRot = 0;
                    this.localLeg3.xRot = 0;  this.localLeg3.yRot = 0;  this.localLeg3.zRot = 0;

                    // Calculate frame tick progress fraction safely
                    float partialTicks = ageInTicks - entity.tickCount;
                    
                    // 1. Estimate initial baseline hip heights for gait update
                    double[] baselineHipY = new double[] {
                        (24.0 - (this.localAll.y + this.localLeg13.y)) / 16.0,
                        (24.0 - (this.localAll.y + this.localLeg4.y)) / 16.0,
                        (24.0 - (this.localAll.y + this.localLeg16.y)) / 16.0,
                        (24.0 - (this.localAll.y + this.localLeg7.y)) / 16.0,
                        (24.0 - (this.localAll.y + this.localLeg10.y)) / 16.0,
                        (24.0 - (this.localAll.y + this.localLeg1.y)) / 16.0
                    };

                    // Update gait controller to find/step to realistic ground positions
                    WeaverGait.updateGait(entity, partialTicks, baselineHipY);
                    WeaverGait.EntityGaitState gait = WeaverGait.getOrCreateState(entity.getUUID());

                    // 2. Dynamic Terrain-Adaptive Body Height and Slope Tilting (Pitch/Roll)
                    double sumFeetY = 0;
                    double sumLeftY = 0, sumRightY = 0;
                    double sumFrontY = 0, sumBackY = 0;
                    for (int i = 0; i < 6; i++) {
                        double fy = gait.legs[i].currentPlantedPos.y;
                        sumFeetY += fy;
                        if (i == 0 || i == 2 || i == 4) sumLeftY += fy;
                        else sumRightY += fy;
                        if (i == 0 || i == 1) sumFrontY += fy;
                        if (i == 4 || i == 5) sumBackY += fy;
                    }
                    double avgFeetY = sumFeetY / 6.0;
                    double avgLeftY = sumLeftY / 3.0;
                    double avgRightY = sumRightY / 3.0;
                    double avgFrontY = sumFrontY / 2.0;
                    double avgBackY = sumBackY / 2.0;

                    // Compute target root coordinate so body rests at exactly 1.6 blocks above average ground level
                    double targetStanceHeight = 1.6;
                    double targetAllY = 24.0 - (-8.0) - 16.0 * ((avgFeetY + targetStanceHeight) - entity.getY());
                    // Clamp root translation to prevent extreme separation
                    this.localAll.y = (float) Mth.lerp(0.15, this.localAll.y, Mth.clamp(targetAllY, -14.0, 4.0));

                    // Smoothly tilt body along terrain slopes (Z-Roll and X-Pitch) matching high-fidelity ArachnoMod
                    double targetRoll = Mth.clamp(Math.atan2(avgLeftY - avgRightY, 4.0), -0.35, 0.35);
                    double targetPitch = Mth.clamp(Math.atan2(avgBackY - avgFrontY, 3.0), -0.35, 0.35);
                    this.localAll.zRot = (float) Mth.lerp(0.12, this.localAll.zRot, (float) targetRoll);
                    this.localAll.xRot = (float) Mth.lerp(0.12, this.localAll.xRot, (float) targetPitch);

                    // Recalculate precise hip world coordinates with the updated body position
                    double[] hipY = new double[] {
                        (24.0 - (this.localAll.y + this.localLeg13.y)) / 16.0, // Front Left
                        (24.0 - (this.localAll.y + this.localLeg4.y)) / 16.0,  // Front Right
                        (24.0 - (this.localAll.y + this.localLeg16.y)) / 16.0, // Mid Left
                        (24.0 - (this.localAll.y + this.localLeg7.y)) / 16.0,  // Mid Right
                        (24.0 - (this.localAll.y + this.localLeg10.y)) / 16.0, // Back Left
                        (24.0 - (this.localAll.y + this.localLeg1.y)) / 16.0   // Back Right
                    };

                    Vec3 bodyPos = entity.position();
                    
                    // Use buttery-smooth interpolated body yaw
                    float yaw = Mth.lerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);

                    // Segment length metrics matching 1.21.1 model pivots (48 pixels thigh, 48 pixels shin)
                    double thighLen = 3.0; // 3.0 blocks = 48 texels
                    double shinLen = 3.0;  // 3.0 blocks = 48 texels

                    // Left Side Leg Solver (Offsets match Woodweaver.java exactly)
                    // Passing angular limits to prevent physical overlapping/tangling
                    WeaverKinematicChain.solveIK(this.localLeg13, this.localLeg14, this.localLeg15, bodyPos.add(-0.5, hipY[0], -0.625), gait.legs[0].currentPlantedPos, thighLen, shinLen, true, yaw, -1.25f, -0.10f);
                    WeaverKinematicChain.solveIK(this.localLeg16, this.localLeg17, this.localLeg18, bodyPos.add(-0.5, hipY[2],  0.0625), gait.legs[2].currentPlantedPos, thighLen, shinLen, true, yaw, -0.55f,  0.55f);
                    WeaverKinematicChain.solveIK(this.localLeg10, this.localLeg11, this.localLeg12, bodyPos.add(-0.5, hipY[4],  0.75), gait.legs[4].currentPlantedPos, thighLen, shinLen, true, yaw,  0.10f,  1.25f);

                    // Right Side Leg Solver (Offsets match Woodweaver.java exactly)
                    // Passing angular limits to prevent physical overlapping/tangling
                    WeaverKinematicChain.solveIK(this.localLeg4,  this.localLeg5,  this.localLeg6,  bodyPos.add( 0.5, hipY[1], -0.625), gait.legs[1].currentPlantedPos, thighLen, shinLen, false, yaw,  0.10f,  1.25f);
                    WeaverKinematicChain.solveIK(this.localLeg7,  this.localLeg8,  this.localLeg9,  bodyPos.add( 0.5, hipY[3],  0.0625), gait.legs[3].currentPlantedPos, thighLen, shinLen, false, yaw, -0.55f,  0.55f);
                    WeaverKinematicChain.solveIK(this.localLeg1,  this.localLeg2,  this.localLeg3,  bodyPos.add( 0.5, hipY[5],  0.75), gait.legs[5].currentPlantedPos, thighLen, shinLen, false, yaw, -1.25f, -0.10f);
                }
            } // 1.21.1
        }
    }
}
