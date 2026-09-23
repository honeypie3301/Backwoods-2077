// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelRot<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "rot"),
			"main");
	private final ModelPart root;
	private final ModelPart right_arm;
	private final ModelPart head;
	private final ModelPart headfront;
	private final ModelPart mouth;
	private final ModelPart mouth2;
	private final ModelPart mouth3;
	private final ModelPart mouth4;
	private final ModelPart mouth5;
	private final ModelPart mouth6;
	private final ModelPart body;
	private final ModelPart left_arm;
	private final ModelPart left_leg;
	private final ModelPart right_leg;

	public ModelRot(ModelPart root) {
		this.root = root.getChild("root");
		this.right_arm = this.root.getChild("right_arm");
		this.head = this.root.getChild("head");
		this.headfront = this.head.getChild("headfront");
		this.mouth = this.head.getChild("mouth");
		this.mouth2 = this.head.getChild("mouth2");
		this.mouth3 = this.head.getChild("mouth3");
		this.mouth4 = this.head.getChild("mouth4");
		this.mouth5 = this.head.getChild("mouth5");
		this.mouth6 = this.head.getChild("mouth6");
		this.body = this.root.getChild("body");
		this.left_arm = this.root.getChild("left_arm");
		this.left_leg = this.root.getChild("left_leg");
		this.right_leg = this.root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(),
				PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 16)
				.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-5.0F, -22.0F, 0.0F));

		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F,
				-8.0F, 0.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -24.0F, 0.0F));

		PartDefinition headfront = head.addOrReplaceChild("headfront",
				CubeListBuilder.create().texOffs(40, 17)
						.addBox(-4.0F, -6.5F, -1.0F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(32, 32)
						.addBox(-3.0F, -6.5F, -1.0F, 6.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(40, 10)
						.addBox(3.0F, -6.5F, -1.0F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -1.5F, -2.0F));

		PartDefinition mouth_r1 = headfront.addOrReplaceChild("mouth_r1",
				CubeListBuilder.create().texOffs(32, 36)
						.addBox(-3.0F, -1.5F, -1.0F, 6.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(40, 24)
						.addBox(-4.0F, -1.5F, -1.0F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(32, 40)
						.addBox(3.0F, -1.5F, -1.0F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.1416F));

		PartDefinition mouth = head.addOrReplaceChild("mouth", CubeListBuilder.create(),
				PartPose.offset(0.0F, -0.5F, -3.5F));

		PartDefinition mouth_r2 = mouth.addOrReplaceChild("mouth_r2",
				CubeListBuilder.create().texOffs(40, 0).addBox(-3.0F, -0.5F, -0.5F, 6.0F, 4.0F, 1.0F,
						new CubeDeformation(-0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.1416F));

		PartDefinition mouth2 = head.addOrReplaceChild("mouth2", CubeListBuilder.create(),
				PartPose.offset(3.5F, -2.0F, -3.5F));

		PartDefinition mouth_r3 = mouth2
				.addOrReplaceChild("mouth_r3",
						CubeListBuilder.create().texOffs(40, 45).addBox(-1.5F, -2.0F, -0.5F, 3.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.1416F));

		PartDefinition mouth3 = head.addOrReplaceChild("mouth3", CubeListBuilder.create(),
				PartPose.offset(-3.5F, -2.0F, -3.5F));

		PartDefinition mouth_r4 = mouth3
				.addOrReplaceChild("mouth_r4",
						CubeListBuilder.create().texOffs(40, 40).addBox(-1.5F, -2.0F, -0.5F, 3.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.1416F));

		PartDefinition mouth4 = head.addOrReplaceChild("mouth4", CubeListBuilder.create().texOffs(0, 44).addBox(-0.5F,
				-2.0F, -0.5F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.5F, -6.0F, -3.5F));

		PartDefinition mouth5 = head.addOrReplaceChild("mouth5", CubeListBuilder.create().texOffs(8, 44).addBox(-2.5F,
				-2.0F, -0.5F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.5F, -6.0F, -3.5F));

		PartDefinition mouth6 = head.addOrReplaceChild("mouth6", CubeListBuilder.create().texOffs(40, 5).addBox(-3.0F,
				-0.5F, -0.5F, 6.0F, 4.0F, 1.0F, new CubeDeformation(-0.001F)), PartPose.offset(0.0F, -7.5F, -3.5F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -6.0F,
				-2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -18.0F, 0.0F));

		PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 0)
				.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(5.0F, -22.0F, 0.0F));

		PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 28).addBox(
				-1.9F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, -12.0F, 0.0F));

		PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 32)
				.addBox(-2.1F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-1.9F, -12.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
		this.head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.head.xRot = headPitch / (180F / (float) Math.PI);
	}
}