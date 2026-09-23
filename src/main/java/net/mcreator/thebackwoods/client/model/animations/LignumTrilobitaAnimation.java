package net.mcreator.thebackwoods.client.model.animations;

import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.AnimationChannel;

// Save this class in your mod and generate all required imports
/**
 * Made with Blockbench 5.1.6 Exported for Minecraft version 1.19 or later with
 * Mojang mappings
 * 
 * @author Author
 */
public class LignumTrilobitaAnimation {
	public static final AnimationDefinition walk = AnimationDefinition.Builder.withLength(1.0F).looping()
			.addAnimation("left_1", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_1", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.4167F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.625F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -24.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_2", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.4167F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.625F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 24.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_2", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_3", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.2083F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.7083F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_3", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 24.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_4", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.2917F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5417F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.7917F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_4", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.2083F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.7083F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_5", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 24.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.4167F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.625F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.875F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 24.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_5", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.2917F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5417F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.7917F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_6", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.4167F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_6", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -24.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.4167F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.625F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.875F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -24.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_7", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.4167F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.625F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 24.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_7", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.4167F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.build();
	public static final AnimationDefinition hide = AnimationDefinition.Builder.withLength(0.625F)
			.addAnimation("pleurals", new AnimationChannel(AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, -0.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5833F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_leg", new AnimationChannel(AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, -0.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5833F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_leg", new AnimationChannel(AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, -0.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM), new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5833F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_1",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_1",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_2",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_2",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_3",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_3",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_4",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_4",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_5",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_5",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_6",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_6",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("left_7",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("right_7",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.build();
	public static final AnimationDefinition unhide = AnimationDefinition.Builder.withLength(0.625F)
			.addAnimation("pleurals",
					new AnimationChannel(AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -0.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.5833F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("left_leg",
					new AnimationChannel(AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -0.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.5833F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("right_leg",
					new AnimationChannel(AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, -2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -0.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.5833F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("left_1", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("right_1", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("left_2", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("right_2", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("left_3", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("right_3", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("left_4", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("right_4", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("left_5", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("right_5", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("left_6", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("right_6", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("left_7", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("right_7", new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -140.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.LINEAR), new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
			.build();
}