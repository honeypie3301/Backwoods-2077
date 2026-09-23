// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelLignumVermis<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "lignumvermis"), "main");
	private final ModelPart body1;
	private final ModelPart body2;
	private final ModelPart body3;
	private final ModelPart body4;
	private final ModelPart body5;
	private final ModelPart body6;
	private final ModelPart body7;
	private final ModelPart wing1;
	private final ModelPart wing2;
	private final ModelPart wing3;

	public ModelLignumVermis(ModelPart root) {
		this.body1 = root.getChild("body1");
		this.body2 = root.getChild("body2");
		this.body3 = root.getChild("body3");
		this.body4 = root.getChild("body4");
		this.body5 = root.getChild("body5");
		this.body6 = root.getChild("body6");
		this.body7 = root.getChild("body7");
		this.wing1 = root.getChild("wing1");
		this.wing2 = root.getChild("wing2");
		this.wing3 = root.getChild("wing3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body1 = partdefinition.addOrReplaceChild("body1", CubeListBuilder.create().texOffs(0, 0).addBox(
				-1.5F, 0.0F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.0F, -7.0F));

		PartDefinition body2 = partdefinition.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(0, 4).addBox(
				-2.0F, -1.5F, -1.0F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.5F, -5.0F));

		PartDefinition body3 = partdefinition.addOrReplaceChild("body3", CubeListBuilder.create().texOffs(0, 9).addBox(
				-3.0F, -2.0F, -1.5F, 6.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.0F, -2.5F));

		PartDefinition body4 = partdefinition.addOrReplaceChild("body4", CubeListBuilder.create().texOffs(0, 16).addBox(
				-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.5F, 0.5F));

		PartDefinition body5 = partdefinition.addOrReplaceChild("body5", CubeListBuilder.create().texOffs(0, 22).addBox(
				-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.0F, 3.5F));

		PartDefinition body6 = partdefinition.addOrReplaceChild("body6", CubeListBuilder.create().texOffs(11, 0).addBox(
				-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.5F, 6.0F));

		PartDefinition body7 = partdefinition.addOrReplaceChild("body7", CubeListBuilder.create().texOffs(13, 4).addBox(
				-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.5F, 8.0F));

		PartDefinition wing1 = partdefinition.addOrReplaceChild("wing1",
				CubeListBuilder.create().texOffs(23, 3)
						.addBox(-5.0F, 0.0F, -1.5F, 10.0F, 8.0F, 0.0F, new CubeDeformation(-0.01F)).texOffs(23, 3)
						.addBox(-5.0F, 0.0F, 1.5F, 10.0F, 8.0F, 0.0F, new CubeDeformation(-0.01F)),
				PartPose.offset(0.0F, 16.0F, -2.5F));

		PartDefinition wing2 = partdefinition.addOrReplaceChild("wing2",
				CubeListBuilder.create().texOffs(23, 14)
						.addBox(-3.0F, 0.0F, -1.5F, 6.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(23, 14)
						.addBox(-3.0F, 0.0F, 1.5F, 6.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 20.0F, 3.5F));

		PartDefinition wing3 = partdefinition.addOrReplaceChild("wing3",
				CubeListBuilder.create().texOffs(22, 20)
						.addBox(-3.0F, 0.0F, 0.5F, 6.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(22, 20)
						.addBox(-3.0F, 0.0F, -1.5F, 6.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 19.0F, -5.0F));

		return LayerDefinition.create(meshdefinition, 64, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		body1.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body4.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body5.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body6.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body7.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		wing1.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		wing2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		wing3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}