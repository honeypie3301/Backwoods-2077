// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelStiltWalker<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "stiltwalker"), "main");
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart headwear;
	private final ModelPart right_arm;
	private final ModelPart left_arm;
	private final ModelPart right_leg;
	private final ModelPart left_leg;

	public ModelStiltWalker(ModelPart root) {
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.headwear = root.getChild("headwear");
		this.right_arm = root.getChild("right_arm");
		this.left_arm = root.getChild("left_arm");
		this.right_leg = root.getChild("right_leg");
		this.left_leg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 23).addBox(
				-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -15.0F, 0.0F));

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-3.0F, -15.0F, -4.0F, 6.0F, 15.0F, 8.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -15.0F, 0.0F));

		PartDefinition headwear = partdefinition.addOrReplaceChild("headwear", CubeListBuilder.create().texOffs(24, 23)
				.addBox(-2.0F, -8.0F, -4.0F, 4.0F, 8.0F, 8.0F, new CubeDeformation(-0.5F)),
				PartPose.offset(0.0F, -15.0F, 0.0F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 39)
				.addBox(-1.0F, -2.0F, -1.0F, 2.0F, 30.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-5.0F, -13.0F, 0.0F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(8, 39)
				.addBox(-1.0F, -2.0F, -1.0F, 2.0F, 30.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(5.0F, -13.0F, 0.0F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(16, 39).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 30.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-2.0F, -6.0F, 0.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 39)
				.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 30.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(2.0F, -6.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 80, 80);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		headwear.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
		this.head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.head.xRot = headPitch / (180F / (float) Math.PI);
		this.headwear.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.headwear.xRot = headPitch / (180F / (float) Math.PI);
		this.right_arm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
		this.left_leg.xRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
		this.left_arm.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
		this.right_leg.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
	}
}