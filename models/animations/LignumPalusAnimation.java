// Save this class in your mod and generate all required imports

/**
 * Made with Blockbench 5.1.4 Exported for Minecraft version 1.19 or later with
 * Mojang mappings
 * 
 * @author Author
 */
public class LignumPalusAnimation {
	public static final AnimationDefinition open_mouth = AnimationDefinition.Builder.withLength(2.25F)
			.addAnimation("mouth",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 110.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("mouth5",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(2.25F, KeyframeAnimations.degreeVec(0.0F, 132.5F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("mouth2",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, -117.5F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("mouth3",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(1.75F, KeyframeAnimations.degreeVec(0.0F, 130.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("mouth4",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, -155.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM)))
			.build();

	public static final AnimationDefinition close_mouth = AnimationDefinition.Builder.withLength(2.25F)
			.addAnimation("mouth",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(1.25F, KeyframeAnimations.degreeVec(0.0F, 110.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(2.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("mouth5",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 132.5F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(2.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("mouth2",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, -117.5F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(2.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("mouth3",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 130.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(2.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("mouth4",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, -155.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM),
							new Keyframe(2.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.CATMULLROM)))
			.build();
}