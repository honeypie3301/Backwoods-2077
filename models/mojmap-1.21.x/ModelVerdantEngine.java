// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelVerdantEngine<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "verdantengine"), "main");
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
	private final ModelPart shell3;
	private final ModelPart bone12;
	private final ModelPart bone13;
	private final ModelPart bone14;
	private final ModelPart bone15;
	private final ModelPart shell7;
	private final ModelPart bone28;
	private final ModelPart bone29;
	private final ModelPart bone30;
	private final ModelPart bone31;
	private final ModelPart shell4;
	private final ModelPart bone16;
	private final ModelPart bone17;
	private final ModelPart bone18;
	private final ModelPart bone19;
	private final ModelPart shell5;
	private final ModelPart bone20;
	private final ModelPart bone21;
	private final ModelPart bone22;
	private final ModelPart bone23;
	private final ModelPart core;

	public ModelVerdantEngine(ModelPart root) {
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
		this.shell3 = this.all.getChild("shell3");
		this.bone12 = this.shell3.getChild("bone12");
		this.bone13 = this.shell3.getChild("bone13");
		this.bone14 = this.shell3.getChild("bone14");
		this.bone15 = this.shell3.getChild("bone15");
		this.shell7 = this.all.getChild("shell7");
		this.bone28 = this.shell7.getChild("bone28");
		this.bone29 = this.shell7.getChild("bone29");
		this.bone30 = this.shell7.getChild("bone30");
		this.bone31 = this.shell7.getChild("bone31");
		this.shell4 = this.all.getChild("shell4");
		this.bone16 = this.shell4.getChild("bone16");
		this.bone17 = this.shell4.getChild("bone17");
		this.bone18 = this.shell4.getChild("bone18");
		this.bone19 = this.shell4.getChild("bone19");
		this.shell5 = this.all.getChild("shell5");
		this.bone20 = this.shell5.getChild("bone20");
		this.bone21 = this.shell5.getChild("bone21");
		this.bone22 = this.shell5.getChild("bone22");
		this.bone23 = this.shell5.getChild("bone23");
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

		PartDefinition bone7 = outer2.addOrReplaceChild("bone7", CubeListBuilder.create(),
				PartPose.offset(0.0F, 6.5F, 0.0F));

		PartDefinition bone2 = outer2.addOrReplaceChild("bone2", CubeListBuilder.create(),
				PartPose.offset(0.0F, -9.5F, 0.0F));

		PartDefinition shell = all.addOrReplaceChild("shell", CubeListBuilder.create(),
				PartPose.offset(0.0357F, -0.1667F, -0.0501F));

		PartDefinition bone3 = shell.addOrReplaceChild("bone3",
				CubeListBuilder.create().texOffs(8, 0).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0014F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, 9.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone4 = shell.addOrReplaceChild("bone4",
				CubeListBuilder.create().texOffs(8, 19).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0016F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, -9.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone5 = shell.addOrReplaceChild("bone5",
				CubeListBuilder.create().texOffs(8, 38).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0018F)),
				PartPose.offsetAndRotation(-9.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition bone6 = shell.addOrReplaceChild("bone6",
				CubeListBuilder.create().texOffs(8, 57).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(9.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition shell2 = all.addOrReplaceChild("shell2", CubeListBuilder.create(),
				PartPose.offset(0.0357F, -0.1667F, -0.0501F));

		PartDefinition bone8 = shell2.addOrReplaceChild("bone8",
				CubeListBuilder.create().texOffs(8, 0).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0014F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, 9.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone9 = shell2.addOrReplaceChild("bone9",
				CubeListBuilder.create().texOffs(8, 19).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0016F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, -9.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone10 = shell2.addOrReplaceChild("bone10",
				CubeListBuilder.create().texOffs(8, 38).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0018F)),
				PartPose.offsetAndRotation(-9.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition bone11 = shell2.addOrReplaceChild("bone11",
				CubeListBuilder.create().texOffs(8, 57).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 17.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(9.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition shell3 = all.addOrReplaceChild("shell3", CubeListBuilder.create(),
				PartPose.offset(0.0357F, -0.1667F, -0.0501F));

		PartDefinition bone12 = shell3.addOrReplaceChild("bone12",
				CubeListBuilder.create().texOffs(17, 9).addBox(-4.0F, -0.5F, 0.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0014F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, 5.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone13 = shell3.addOrReplaceChild("bone13",
				CubeListBuilder.create().texOffs(17, 28).addBox(-4.0F, -0.5F, 0.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0016F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, -5.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone14 = shell3.addOrReplaceChild("bone14",
				CubeListBuilder.create().texOffs(17, 47).addBox(-4.0F, -0.5F, 0.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0018F)),
				PartPose.offsetAndRotation(-5.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition bone15 = shell3.addOrReplaceChild("bone15",
				CubeListBuilder.create().texOffs(17, 66).addBox(-4.0F, -0.5F, 0.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(5.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition shell7 = all.addOrReplaceChild("shell7", CubeListBuilder.create(),
				PartPose.offset(0.0357F, -0.1667F, -0.0501F));

		PartDefinition bone28 = shell7.addOrReplaceChild("bone28",
				CubeListBuilder.create().texOffs(17, 9).addBox(-4.0F, -0.5F, 0.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0014F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, 5.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone29 = shell7.addOrReplaceChild("bone29",
				CubeListBuilder.create().texOffs(17, 28).addBox(-4.0F, -0.5F, 0.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0016F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, -5.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone30 = shell7.addOrReplaceChild("bone30",
				CubeListBuilder.create().texOffs(17, 47).addBox(-4.0F, -0.5F, 0.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0018F)),
				PartPose.offsetAndRotation(-5.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition bone31 = shell7.addOrReplaceChild("bone31",
				CubeListBuilder.create().texOffs(17, 66).addBox(-4.0F, -0.5F, 0.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(5.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition shell4 = all.addOrReplaceChild("shell4", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0357F, -0.1667F, -0.0501F, 0.0F, -0.7854F, 0.0F));

		PartDefinition bone16 = shell4.addOrReplaceChild("bone16",
				CubeListBuilder.create().texOffs(17, 9).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0014F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, 5.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone17 = shell4.addOrReplaceChild("bone17",
				CubeListBuilder.create().texOffs(17, 28).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0016F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, -5.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone18 = shell4.addOrReplaceChild("bone18",
				CubeListBuilder.create().texOffs(17, 47).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0018F)),
				PartPose.offsetAndRotation(-5.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition bone19 = shell4.addOrReplaceChild("bone19",
				CubeListBuilder.create().texOffs(17, 66).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(5.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition shell5 = all.addOrReplaceChild("shell5", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0357F, -0.1667F, -0.0501F, 0.0F, -0.7854F, 0.0F));

		PartDefinition bone20 = shell5.addOrReplaceChild("bone20",
				CubeListBuilder.create().texOffs(17, 9).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0014F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, 5.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone21 = shell5.addOrReplaceChild("bone21",
				CubeListBuilder.create().texOffs(17, 28).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0016F)),
				PartPose.offsetAndRotation(0.0F, -0.3333F, -5.5F, -1.5708F, 0.0F, 0.0F));

		PartDefinition bone22 = shell5.addOrReplaceChild("bone22",
				CubeListBuilder.create().texOffs(17, 47).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0018F)),
				PartPose.offsetAndRotation(-5.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition bone23 = shell5.addOrReplaceChild("bone23",
				CubeListBuilder.create().texOffs(17, 66).addBox(-4.0F, -0.5F, -8.5F, 8.0F, 1.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(5.5F, -0.3333F, 0.0F, 0.0F, 1.5708F, 1.5708F));

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