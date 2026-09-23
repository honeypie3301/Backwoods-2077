// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelLignumGigas<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "lignumgigas"), "main");
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart left_arm;
	private final ModelPart arm1;
	private final ModelPart arm3;
	private final ModelPart arm4;
	private final ModelPart arm5;
	private final ModelPart right_arm;
	private final ModelPart arm10;
	private final ModelPart arm11;
	private final ModelPart arm12;
	private final ModelPart arm13;
	private final ModelPart left_leg;
	private final ModelPart right_leg;

	public ModelLignumGigas(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.left_arm = root.getChild("left_arm");
		this.arm1 = this.left_arm.getChild("arm1");
		this.arm3 = this.left_arm.getChild("arm3");
		this.arm4 = this.left_arm.getChild("arm4");
		this.arm5 = this.left_arm.getChild("arm5");
		this.right_arm = root.getChild("right_arm");
		this.arm10 = this.right_arm.getChild("arm10");
		this.arm11 = this.right_arm.getChild("arm11");
		this.arm12 = this.right_arm.getChild("arm12");
		this.arm13 = this.right_arm.getChild("arm13");
		this.left_leg = root.getChild("left_leg");
		this.right_leg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(18, 25)
						.addBox(3.0F, -8.0F, -4.0F, 1.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(0, 16)
						.addBox(-3.0F, -8.0F, -4.0F, 6.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(24, 0)
						.addBox(-3.0F, -1.0F, -4.0F, 6.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(0, 25)
						.addBox(-4.0F, -8.0F, -4.0F, 1.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(56, 0)
						.addBox(-8.0F, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(56, 5)
						.addBox(4.0F, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(),
				PartPose.offset(5.0F, 2.0F, 0.0F));

		PartDefinition arm1 = left_arm.addOrReplaceChild("arm1", CubeListBuilder.create(),
				PartPose.offset(0.25F, -0.5F, 1.25F));

		PartDefinition left_arm_r1 = arm1.addOrReplaceChild("left_arm_r1",
				CubeListBuilder.create().texOffs(4, 41).addBox(-1.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.5F, 0.0F, -0.5F, 0.0F, 0.0F, -0.0698F));

		PartDefinition left_arm_r2 = arm1.addOrReplaceChild("left_arm_r2",
				CubeListBuilder.create().texOffs(0, 41).addBox(-0.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, -0.0698F));

		PartDefinition arm3 = left_arm.addOrReplaceChild("arm3", CubeListBuilder.create(),
				PartPose.offset(0.0F, -0.5F, 0.0F));

		PartDefinition left_arm_r3 = arm3.addOrReplaceChild("left_arm_r3",
				CubeListBuilder.create().texOffs(12, 41).addBox(-1.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.5F, 0.0F, -0.5F, 0.0F, 0.0F, -0.0698F));

		PartDefinition left_arm_r4 = arm3.addOrReplaceChild("left_arm_r4",
				CubeListBuilder.create().texOffs(8, 41).addBox(-0.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, -0.0698F));

		PartDefinition arm4 = left_arm.addOrReplaceChild("arm4", CubeListBuilder.create(),
				PartPose.offset(1.5F, -0.5F, -0.25F));

		PartDefinition left_arm_r5 = arm4.addOrReplaceChild("left_arm_r5",
				CubeListBuilder.create().texOffs(20, 41).addBox(-1.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.5F, 0.0F, -0.5F, 0.0F, 0.0F, -0.0698F));

		PartDefinition left_arm_r6 = arm4.addOrReplaceChild("left_arm_r6",
				CubeListBuilder.create().texOffs(16, 41).addBox(-0.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, -0.0698F));

		PartDefinition arm5 = left_arm.addOrReplaceChild("arm5", CubeListBuilder.create(),
				PartPose.offset(1.25F, -0.5F, 1.5F));

		PartDefinition left_arm_r7 = arm5.addOrReplaceChild("left_arm_r7",
				CubeListBuilder.create().texOffs(28, 41).addBox(-1.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.5F, 0.0F, -0.5F, 0.0F, 0.0F, -0.0698F));

		PartDefinition left_arm_r8 = arm5.addOrReplaceChild("left_arm_r8",
				CubeListBuilder.create().texOffs(24, 41).addBox(-0.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, -0.0698F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(),
				PartPose.offset(-5.0F, 2.0F, 0.0F));

		PartDefinition arm10 = right_arm.addOrReplaceChild("arm10", CubeListBuilder.create(),
				PartPose.offset(-0.25F, -0.5F, 1.25F));

		PartDefinition right_arm_r1 = arm10.addOrReplaceChild("right_arm_r1",
				CubeListBuilder.create().texOffs(36, 41).addBox(-0.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0698F));

		PartDefinition right_arm_r2 = arm10.addOrReplaceChild("right_arm_r2",
				CubeListBuilder.create().texOffs(32, 41).addBox(-1.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0698F));

		PartDefinition arm11 = right_arm.addOrReplaceChild("arm11", CubeListBuilder.create(),
				PartPose.offset(0.0F, -0.5F, 0.0F));

		PartDefinition right_arm_r3 = arm11.addOrReplaceChild("right_arm_r3",
				CubeListBuilder.create().texOffs(44, 41).addBox(-0.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0698F));

		PartDefinition right_arm_r4 = arm11.addOrReplaceChild("right_arm_r4",
				CubeListBuilder.create().texOffs(40, 41).addBox(-1.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0698F));

		PartDefinition arm12 = right_arm.addOrReplaceChild("arm12", CubeListBuilder.create(),
				PartPose.offset(-1.5F, -0.5F, -0.25F));

		PartDefinition right_arm_r5 = arm12.addOrReplaceChild("right_arm_r5",
				CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0698F));

		PartDefinition right_arm_r6 = arm12.addOrReplaceChild("right_arm_r6",
				CubeListBuilder.create().texOffs(48, 41).addBox(-1.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0698F));

		PartDefinition arm13 = right_arm.addOrReplaceChild("arm13", CubeListBuilder.create(),
				PartPose.offset(-1.25F, -0.5F, 1.5F));

		PartDefinition right_arm_r7 = arm13.addOrReplaceChild("right_arm_r7",
				CubeListBuilder.create().texOffs(52, 40).addBox(-0.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0698F));

		PartDefinition right_arm_r8 = arm13.addOrReplaceChild("right_arm_r8",
				CubeListBuilder.create().texOffs(52, 20).addBox(-1.5F, -0.5F, 0.0F, 2.0F, 20.0F, 0.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0698F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(28, 9)
				.addBox(-1.9F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(1.9F, 12.0F, 0.0F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(36, 25).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-1.9F, 12.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}