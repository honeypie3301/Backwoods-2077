// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelFractus_1<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "fractus_1"), "main");
	private final ModelPart all;
	private final ModelPart outer;
	private final ModelPart bone;
	private final ModelPart outer2;
	private final ModelPart bone7;
	private final ModelPart bone2;
	private final ModelPart shell;
	private final ModelPart bone3;
	private final ModelPart bone4;
	private final ModelPart bone5;
	private final ModelPart bone6;
	private final ModelPart shell2;
	private final ModelPart bone8;
	private final ModelPart bone9;
	private final ModelPart bone10;
	private final ModelPart bone11;
	private final ModelPart core;

	public ModelFractus_1(ModelPart root) {
		this.all = root.getChild("all");
		this.outer = this.all.getChild("outer");
		this.bone = this.outer.getChild("bone");
		this.outer2 = this.all.getChild("outer2");
		this.bone7 = this.outer2.getChild("bone7");
		this.bone2 = this.outer2.getChild("bone2");
		this.shell = this.all.getChild("shell");
		this.bone3 = this.shell.getChild("bone3");
		this.bone4 = this.shell.getChild("bone4");
		this.bone5 = this.shell.getChild("bone5");
		this.bone6 = this.shell.getChild("bone6");
		this.shell2 = this.all.getChild("shell2");
		this.bone8 = this.shell2.getChild("bone8");
		this.bone9 = this.shell2.getChild("bone9");
		this.bone10 = this.shell2.getChild("bone10");
		this.bone11 = this.shell2.getChild("bone11");
		this.core = this.all.getChild("core");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(),
				PartPose.offset(0.0F, 16.0F, 0.0F));

		PartDefinition outer = all.addOrReplaceChild("outer", CubeListBuilder.create(),
				PartPose.offset(0.0357F, -0.5F, -0.0501F));

		PartDefinition bone = outer.addOrReplaceChild("bone", CubeListBuilder.create(),
				PartPose.offset(0.0F, 7.5F, 0.0F));

		PartDefinition outer2 = all.addOrReplaceChild("outer2", CubeListBuilder.create(),
				PartPose.offset(0.0357F, 1.0F, -0.0501F));

		PartDefinition bone7 = outer2.addOrReplaceChild("bone7", CubeListBuilder.create().texOffs(82, 8).addBox(-4.0F,
				-0.5F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.001F)), PartPose.offset(0.0F, 6.5F, 0.0F));

		PartDefinition bone2 = outer2.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(82, 26).addBox(-4.0F,
				-0.5F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0012F)), PartPose.offset(0.0F, -9.5F, 0.0F));

		PartDefinition shell = all.addOrReplaceChild("shell", CubeListBuilder.create(),
				PartPose.offset(0.0357F, -0.1667F, -0.0501F));

		PartDefinition bone3 = shell.addOrReplaceChild("bone3",
				CubeListBuilder.create().texOffs(8, 0).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0014F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, 7.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone4 = shell.addOrReplaceChild("bone4",
				CubeListBuilder.create().texOffs(8, 19).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0016F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, -7.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone5 = shell.addOrReplaceChild("bone5",
				CubeListBuilder.create().texOffs(8, 38).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0018F)),
				PartPose.offsetAndRotation(-7.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition bone6 = shell.addOrReplaceChild("bone6",
				CubeListBuilder.create().texOffs(8, 57).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(7.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition shell2 = all.addOrReplaceChild("shell2", CubeListBuilder.create(),
				PartPose.offset(0.0357F, -0.1667F, -0.0501F));

		PartDefinition bone8 = shell2.addOrReplaceChild("bone8",
				CubeListBuilder.create().texOffs(8, 0).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0014F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, 7.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone9 = shell2.addOrReplaceChild("bone9",
				CubeListBuilder.create().texOffs(8, 19).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0016F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, -7.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone10 = shell2.addOrReplaceChild("bone10",
				CubeListBuilder.create().texOffs(8, 38).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0018F)),
				PartPose.offsetAndRotation(-7.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition bone11 = shell2.addOrReplaceChild("bone11",
				CubeListBuilder.create().texOffs(8, 57).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(7.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition core = all.addOrReplaceChild("core", CubeListBuilder.create().texOffs(66, 36).addBox(-3.0247F,
				-2.0F, -2.9681F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0713F, -1.0F, -0.1002F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		all.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}