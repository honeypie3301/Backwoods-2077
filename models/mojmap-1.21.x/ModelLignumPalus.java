// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelLignumPalus<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "lignumpalus"), "main");
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart mouth2;
	private final ModelPart mouth3;
	private final ModelPart mouth4;
	private final ModelPart mouth5;
	private final ModelPart mouth;
	private final ModelPart right_arm;
	private final ModelPart left_arm;
	private final ModelPart right_leg;
	private final ModelPart left_leg;

	public ModelLignumPalus(ModelPart root) {
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.mouth2 = this.head.getChild("mouth2");
		this.mouth3 = this.head.getChild("mouth3");
		this.mouth4 = this.head.getChild("mouth4");
		this.mouth5 = this.head.getChild("mouth5");
		this.mouth = this.head.getChild("mouth");
		this.right_arm = root.getChild("right_arm");
		this.left_arm = root.getChild("left_arm");
		this.right_leg = root.getChild("right_leg");
		this.left_leg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 0)
				.addBox(-2.0F, -13.0F, -2.0F, 4.0F, 26.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -34.0F, 0.0F));

		PartDefinition head = partdefinition.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(44, 21)
						.addBox(-2.0F, -20.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(44, 0)
						.addBox(-1.0F, -19.0F, 0.0F, 2.0F, 19.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(12, 49)
						.addBox(-2.0F, -7.0F, -2.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(44, 50)
						.addBox(1.0F, -3.0F, -2.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(16, 49)
						.addBox(-2.0F, -15.0F, -2.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(20, 49)
						.addBox(1.0F, -19.0F, -2.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(24, 49)
						.addBox(1.0F, -11.0F, -2.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(28, 30)
						.addBox(-2.0F, -19.0F, -1.0F, 1.0F, 19.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(36, 30)
						.addBox(1.0F, -19.0F, -1.0F, 1.0F, 19.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -47.0F, 0.0F));

		PartDefinition mouth2 = head.addOrReplaceChild("mouth2", CubeListBuilder.create().texOffs(44, 31).addBox(-2.5F,
				-2.0F, -0.5F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, -13.0F, -1.5F));

		PartDefinition mouth3 = head.addOrReplaceChild("mouth3", CubeListBuilder.create().texOffs(44, 36).addBox(-0.5F,
				-2.0F, -0.5F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, -9.0F, -1.5F));

		PartDefinition mouth4 = head.addOrReplaceChild("mouth4", CubeListBuilder.create().texOffs(44, 41).addBox(-2.5F,
				-2.0F, -0.5F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, -5.0F, -1.5F));

		PartDefinition mouth5 = head.addOrReplaceChild("mouth5", CubeListBuilder.create().texOffs(44, 46).addBox(-0.5F,
				-1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, -1.5F, -1.5F));

		PartDefinition mouth = head.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(44, 26).addBox(-0.5F,
				-2.0F, -0.5F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, -17.0F, -1.5F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-1.0F, -1.0F, -1.0F, 1.0F, 58.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-2.0F, -42.0F, 0.0F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(6, 0)
				.addBox(0.0F, -1.0F, -1.0F, 1.0F, 58.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(2.0F, -42.0F, 0.0F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(12, 0)
				.addBox(-1.0F, -1.0F, -1.0F, 2.0F, 47.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-1.0F, -22.0F, 0.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(20, 0)
				.addBox(-1.0F, -1.0F, -1.0F, 2.0F, 47.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(1.0F, -22.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
		this.head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.head.xRot = headPitch / (180F / (float) Math.PI);
		this.right_arm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
		this.left_leg.xRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
		this.left_arm.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
		this.right_leg.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
	}
}