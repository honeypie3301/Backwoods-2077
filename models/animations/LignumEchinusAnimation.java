// Save this class in your mod and generate all required imports

/**
 * Made with Blockbench 5.1.6 Exported for Minecraft version 1.19 or later with
 * Mojang mappings
 * 
 * @author Author
 */
public class LignumEchinusAnimation {
	public static final AnimationDefinition roll = AnimationDefinition.Builder.withLength(1.0F).looping()
			.addAnimation("body",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(-180.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(1.0F, KeyframeAnimations.degreeVec(180.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR)))
			.build();
}