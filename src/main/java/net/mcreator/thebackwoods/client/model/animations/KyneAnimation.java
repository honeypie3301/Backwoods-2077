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
public class KyneAnimation {
	public static final AnimationDefinition right_punch = AnimationDefinition.Builder.withLength(0.2917F)
			.addAnimation("right_arm",
					new AnimationChannel(AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.0833F, KeyframeAnimations.degreeVec(-60.0F, 10.8513F, 10.419F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.125F, KeyframeAnimations.degreeVec(-80.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.2083F, KeyframeAnimations.degreeVec(-60.0F, -14.3386F, -15.6632F), AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(0.2917F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.build();
}