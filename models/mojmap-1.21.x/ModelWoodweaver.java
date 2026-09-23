// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelWoodweaver<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "woodweaver"), "main");
	private final ModelPart all;
	private final ModelPart body2;
	private final ModelPart upper_body;
	private final ModelPart claw;
	private final ModelPart claw2;
	private final ModelPart claw3;
	private final ModelPart claw4;
	private final ModelPart claw5;
	private final ModelPart claw6;
	private final ModelPart head;
	private final ModelPart headtop;
	private final ModelPart headpiece1;
	private final ModelPart headpiece2;
	private final ModelPart headpiece3;
	private final ModelPart headpiece4;
	private final ModelPart orb;
	private final ModelPart body;
	private final ModelPart tendrilthing1;
	private final ModelPart tendrilthing2;
	private final ModelPart tendrilthing7;
	private final ModelPart tendrilthing8;
	private final ModelPart leg16;
	private final ModelPart leg17;
	private final ModelPart leg18;
	private final ModelPart leg7;
	private final ModelPart leg8;
	private final ModelPart leg9;
	private final ModelPart leg13;
	private final ModelPart leg14;
	private final ModelPart leg15;
	private final ModelPart leg4;
	private final ModelPart leg5;
	private final ModelPart leg6;
	private final ModelPart leg10;
	private final ModelPart leg11;
	private final ModelPart leg12;
	private final ModelPart leg1;
	private final ModelPart leg2;
	private final ModelPart leg3;
	private final ModelPart headpiece5;
	private final ModelPart headpiece7;
	private final ModelPart headpiece6;
	private final ModelPart headpiece9;

	public ModelWoodweaver(ModelPart root) {
		this.all = root.getChild("all");
		this.body2 = this.all.getChild("body2");
		this.upper_body = this.body2.getChild("upper_body");
		this.claw = this.upper_body.getChild("claw");
		this.claw2 = this.claw.getChild("claw2");
		this.claw3 = this.claw2.getChild("claw3");
		this.claw4 = this.upper_body.getChild("claw4");
		this.claw5 = this.claw4.getChild("claw5");
		this.claw6 = this.claw5.getChild("claw6");
		this.head = this.upper_body.getChild("head");
		this.headtop = this.head.getChild("headtop");
		this.headpiece1 = this.head.getChild("headpiece1");
		this.headpiece2 = this.headpiece1.getChild("headpiece2");
		this.headpiece3 = this.head.getChild("headpiece3");
		this.headpiece4 = this.headpiece3.getChild("headpiece4");
		this.orb = this.head.getChild("orb");
		this.body = this.all.getChild("body");
		this.tendrilthing1 = this.body.getChild("tendrilthing1");
		this.tendrilthing2 = this.tendrilthing1.getChild("tendrilthing2");
		this.tendrilthing7 = this.body.getChild("tendrilthing7");
		this.tendrilthing8 = this.tendrilthing7.getChild("tendrilthing8");
		this.leg16 = this.all.getChild("leg16");
		this.leg17 = this.leg16.getChild("leg17");
		this.leg18 = this.leg17.getChild("leg18");
		this.leg7 = this.all.getChild("leg7");
		this.leg8 = this.leg7.getChild("leg8");
		this.leg9 = this.leg8.getChild("leg9");
		this.leg13 = this.all.getChild("leg13");
		this.leg14 = this.leg13.getChild("leg14");
		this.leg15 = this.leg14.getChild("leg15");
		this.leg4 = this.all.getChild("leg4");
		this.leg5 = this.leg4.getChild("leg5");
		this.leg6 = this.leg5.getChild("leg6");
		this.leg10 = this.all.getChild("leg10");
		this.leg11 = this.leg10.getChild("leg11");
		this.leg12 = this.leg11.getChild("leg12");
		this.leg1 = this.all.getChild("leg1");
		this.leg2 = this.leg1.getChild("leg2");
		this.leg3 = this.leg2.getChild("leg3");
		this.headpiece5 = root.getChild("headpiece5");
		this.headpiece7 = root.getChild("headpiece7");
		this.headpiece6 = root.getChild("headpiece6");
		this.headpiece9 = root.getChild("headpiece9");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all",
				CubeListBuilder.create().texOffs(0, 59).addBox(-8.0F, -16.0F, -16.0F, 16.0F, 16.0F, 32.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -18.25F, 0.0F, 0.0F, 0.0F, -0.0088F));

		PartDefinition body2 = all.addOrReplaceChild("body2", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, -14.0F, -13.0F, 0.0331F, 0.0F, 0.0F));

		PartDefinition mid_torso_r1 = body2.addOrReplaceChild("mid_torso_r1",
				CubeListBuilder.create().texOffs(126, 83).addBox(-6.0F, -3.5F, -4.0F, 12.0F, 7.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -8.3744F, -6.1029F, -1.0472F, 0.0F, 0.0F));

		PartDefinition lower_torso_r1 = body2.addOrReplaceChild("lower_torso_r1",
				CubeListBuilder.create().texOffs(117, 80).addBox(-9.0F, -4.5F, -5.5F, 18.0F, 9.0F, 11.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -0.1471F, -1.3529F, -1.0472F, 0.0F, 0.0F));

		PartDefinition upper_body = body2.addOrReplaceChild("upper_body", CubeListBuilder.create(),
				PartPose.offset(0.0F, -11.4006F, -7.4689F));

		PartDefinition chest_r1 = upper_body.addOrReplaceChild("chest_r1",
				CubeListBuilder.create().texOffs(34, 30).addBox(-7.0F, -5.5F, -7.0F, 14.0F, 11.0F, 14.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -6.0F, -5.0F, -1.0472F, 0.0F, 0.0F));

		PartDefinition neck_r1 = upper_body.addOrReplaceChild("neck_r1",
				CubeListBuilder.create().texOffs(131, 82).addBox(-3.0F, -3.5F, -4.0F, 6.0F, 7.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -11.1962F, -8.0F, -1.0472F, 0.0F, 0.0F));

		PartDefinition claw = upper_body.addOrReplaceChild("claw",
				CubeListBuilder.create().texOffs(0, 107).addBox(-32.0F, -4.0F, -4.0F, 32.0F, 8.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-11.0F, -11.8561F, -1.8157F, 1.1722F, -0.2726F, -1.4388F));

		PartDefinition claw2 = claw.addOrReplaceChild("claw2", CubeListBuilder.create(),
				PartPose.offsetAndRotation(-27.701F, 2.836F, 0.0F, -0.0044F, -0.0038F, -2.9047F));

		PartDefinition claw2_r1 = claw2.addOrReplaceChild("claw2_r1",
				CubeListBuilder.create().texOffs(0, 123).addBox(-24.0F, -12.0F, 9.0F, 32.0F, 8.0F, 6.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.1566F, 14.5373F, -12.0F, 0.0F, 0.0F, -0.4363F));

		PartDefinition claw3 = claw2.addOrReplaceChild("claw3", CubeListBuilder.create(),
				PartPose.offsetAndRotation(-28.0F, 16.0F, 0.0F, 0.0097F, -0.0171F, -2.7972F));

		PartDefinition claw7_r1 = claw3.addOrReplaceChild("claw7_r1",
				CubeListBuilder.create().texOffs(196, 48).addBox(-42.9583F, -5.6559F, -4.0F, 46.0F, 6.0F, 4.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.2916F, 0.5858F, 1.9731F, 0.0F, 0.0F, -0.4363F));

		PartDefinition claw3_r1 = claw3.addOrReplaceChild("claw3_r1",
				CubeListBuilder.create().texOffs(196, 48).addBox(-16.0F, -2.0F, -2.0F, 17.0F, 3.0F, 4.0F,
						new CubeDeformation(0.006F)),
				PartPose.offsetAndRotation(-43.8638F, 15.0049F, -0.0269F, 0.0F, 0.0F, -0.9163F));

		PartDefinition claw4 = upper_body.addOrReplaceChild("claw4",
				CubeListBuilder.create().texOffs(0, 107).mirror()
						.addBox(0.0F, -4.0F, -4.0F, 32.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(11.0F, -11.8561F, -1.8157F, 1.1722F, 0.2726F, 1.4388F));

		PartDefinition claw5 = claw4.addOrReplaceChild("claw5", CubeListBuilder.create(),
				PartPose.offsetAndRotation(27.701F, 2.836F, 0.0F, -0.0044F, 0.0038F, 2.9047F));

		PartDefinition claw5_r1 = claw5.addOrReplaceChild("claw5_r1",
				CubeListBuilder.create().texOffs(0, 123).mirror()
						.addBox(-8.0F, -12.0F, 9.0F, 32.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(3.1566F, 14.5373F, -12.0F, 0.0F, 0.0F, 0.4363F));

		PartDefinition claw6 = claw5.addOrReplaceChild("claw6", CubeListBuilder.create(),
				PartPose.offsetAndRotation(28.0F, 16.0F, 0.0F, 0.0097F, 0.0171F, 2.7972F));

		PartDefinition claw7_r2 = claw6.addOrReplaceChild("claw7_r2",
				CubeListBuilder.create().texOffs(225, 48).mirror()
						.addBox(-1.0F, -2.0F, -2.0F, 17.0F, 3.0F, 4.0F, new CubeDeformation(0.006F)).mirror(false),
				PartPose.offsetAndRotation(43.8638F, 15.0049F, -0.0269F, 0.0F, 0.0F, 0.9163F));

		PartDefinition claw6_r1 = claw6.addOrReplaceChild("claw6_r1",
				CubeListBuilder.create().texOffs(196, 48).mirror()
						.addBox(-3.0417F, -5.6559F, -4.0F, 46.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(4.2916F, 0.5858F, 1.9731F, 0.0F, 0.0F, 0.4363F));

		PartDefinition head = upper_body.addOrReplaceChild("head", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, -16.0622F, -9.4282F, -0.1103F, 0.0009F, -0.0189F));

		PartDefinition head_r1 = head.addOrReplaceChild("head_r1",
				CubeListBuilder.create().texOffs(196, 60).addBox(-8.0F, -2.0F, -8.0F, 16.0F, 4.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, -0.2058F, 0.0F, 0.7854F, 0.0F));

		PartDefinition headtop = head.addOrReplaceChild("headtop", CubeListBuilder.create(),
				PartPose.offset(0.0F, -14.0F, 9.7942F));

		PartDefinition headtop_r1 = headtop.addOrReplaceChild("headtop_r1",
				CubeListBuilder.create().texOffs(196, 80).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 2.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 14.0F, -10.0F, 0.0F, 0.7854F, 0.0F));

		PartDefinition headpiece1 = head.addOrReplaceChild("headpiece1", CubeListBuilder.create(),
				PartPose.offset(-0.6569F, -8.0F, 9.0369F));

		PartDefinition headpiece1_r1 = headpiece1.addOrReplaceChild("headpiece1_r1",
				CubeListBuilder.create().texOffs(232, 0).addBox(-8.0F, -14.0F, 6.0F, 14.0F, 12.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.6569F, 8.0F, -9.2426F, 0.0F, -0.7854F, 0.0F));

		PartDefinition headpiece2 = headpiece1.addOrReplaceChild("headpiece2", CubeListBuilder.create(),
				PartPose.offset(-9.0F, 0.0F, -8.0F));

		PartDefinition headpiece2_r1 = headpiece2.addOrReplaceChild("headpiece2_r1",
				CubeListBuilder.create().texOffs(196, 223).addBox(-8.0F, -14.0F, -8.0F, 16.0F, 12.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(9.6569F, 8.0F, -1.0369F, 0.0F, 0.7854F, 0.0F));

		PartDefinition headpiece3 = head.addOrReplaceChild("headpiece3", CubeListBuilder.create(),
				PartPose.offset(-0.3432F, -8.0F, 9.0369F));

		PartDefinition headpiece3_r1 = headpiece3.addOrReplaceChild("headpiece3_r1",
				CubeListBuilder.create().texOffs(224, 132).addBox(-8.0F, -14.0F, 6.0F, 16.0F, 12.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.3432F, 8.0F, -9.2426F, 0.0F, 0.7854F, 0.0F));

		PartDefinition headpiece4 = headpiece3.addOrReplaceChild("headpiece4", CubeListBuilder.create(),
				PartPose.offset(10.0F, 0.0F, -8.0F));

		PartDefinition headpiece4_r1 = headpiece4.addOrReplaceChild("headpiece4_r1",
				CubeListBuilder.create().texOffs(232, 14).addBox(-6.0F, -14.0F, -8.0F, 14.0F, 12.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-9.6569F, 8.0F, -1.0369F, 0.0F, -0.7854F, 0.0F));

		PartDefinition orb = head.addOrReplaceChild("orb", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, -8.0F, -0.2058F, -1.2217F, 0.0F, 0.0F));

		PartDefinition orb_r1 = orb.addOrReplaceChild("orb_r1",
				CubeListBuilder.create().texOffs(224, 162).mirror()
						.addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(-1.0F)).mirror(false)
						.texOffs(224, 146).mirror()
						.addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

		PartDefinition orb_r2 = orb.addOrReplaceChild("orb_r2",
				CubeListBuilder.create().texOffs(224, 162)
						.addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(-1.0F)).texOffs(224, 146)
						.addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

		PartDefinition body = all.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -9.0F, -1.0F, 20.0F, 19.0F, 40.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -8.0F, 17.0F, -0.4196F, -0.0218F, -0.0147F));

		PartDefinition tendrilthing1 = body.addOrReplaceChild("tendrilthing1",
				CubeListBuilder.create().texOffs(96, 100)
						.addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 40.0F, new CubeDeformation(0.0F)).texOffs(96, 100)
						.mirror().addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 40.0F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(96, 100).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 40.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(6.0F, 0.0F, 24.0F, 0.3491F, 0.5236F, 0.0F));

		PartDefinition tendrilthing2 = tendrilthing1.addOrReplaceChild("tendrilthing2",
				CubeListBuilder.create().texOffs(112, 191)
						.addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 40.0F, new CubeDeformation(0.0F)).texOffs(112, 191)
						.mirror().addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 40.0F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(112, 191).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 40.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 40.0F, 0.6981F, -0.6981F, 0.0F));

		PartDefinition tendrilthing7 = body.addOrReplaceChild("tendrilthing7", CubeListBuilder.create().texOffs(96, 100)
				.addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 40.0F, new CubeDeformation(0.0F)).texOffs(96, 100).mirror()
				.addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 40.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-6.0F, 0.0F, 24.0F, 0.3491F, -0.5236F, 0.0F));

		PartDefinition tendrilthing8 = tendrilthing7.addOrReplaceChild("tendrilthing8",
				CubeListBuilder.create().texOffs(112, 191)
						.addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 40.0F, new CubeDeformation(0.0F)).texOffs(112, 191)
						.mirror().addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 40.0F, new CubeDeformation(0.0F))
						.mirror(false),
				PartPose.offsetAndRotation(0.0F, 0.0F, 40.0F, 0.6981F, 0.6981F, 0.0F));

		PartDefinition leg16 = all.addOrReplaceChild("leg16",
				CubeListBuilder.create().texOffs(120, 0).addBox(-48.0F, -4.0F, -4.0F, 48.0F, 8.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-8.0F, -8.0F, 1.0F, -0.0511F, 0.0602F, -0.0862F));

		PartDefinition leg17 = leg16.addOrReplaceChild("leg17",
				CubeListBuilder.create().texOffs(122, 18).addBox(-48.0F, -3.0F, -3.0F, 48.0F, 7.0F, 6.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-48.0F, 0.0F, 0.0F, -0.1682F, -0.0568F, -1.0504F));

		PartDefinition leg18 = leg17.addOrReplaceChild("leg18", CubeListBuilder.create(),
				PartPose.offsetAndRotation(-48.0F, 0.0F, 0.0F, 0.1855F, -0.1237F, 1.169F));

		PartDefinition leg18_r1 = leg18
				.addOrReplaceChild("leg18_r1",
						CubeListBuilder.create().texOffs(207, 197).addBox(-11.5F, -2.0F, -2.0F, 11.0F, 4.0F, 4.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.5F, 1.0F, 0.0F, 0.0F, 0.0F, -0.0873F));

		PartDefinition leg7 = all.addOrReplaceChild("leg7",
				CubeListBuilder.create().texOffs(120, 32).addBox(0.0F, -4.0F, -4.0F, 48.0F, 8.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(8.0F, -8.0F, 1.0F, -0.0511F, -0.0602F, 0.0862F));

		PartDefinition leg8 = leg7.addOrReplaceChild("leg8",
				CubeListBuilder.create().texOffs(0, 143).addBox(0.0F, -3.0F, -3.0F, 48.0F, 7.0F, 6.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(48.0F, 0.0F, 0.0F, -0.1682F, 0.0568F, 1.0504F));

		PartDefinition leg9 = leg8.addOrReplaceChild("leg9", CubeListBuilder.create(),
				PartPose.offsetAndRotation(48.0F, 0.0F, 0.0F, 0.1855F, 0.1237F, -1.169F));

		PartDefinition leg9_r1 = leg9
				.addOrReplaceChild("leg9_r1",
						CubeListBuilder.create().texOffs(6, 213).addBox(0.5F, -2.0F, -2.0F, 11.0F, 4.0F, 4.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(-0.5F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0873F));

		PartDefinition leg13 = all.addOrReplaceChild("leg13",
				CubeListBuilder.create().texOffs(112, 143).addBox(-48.0F, -4.0F, -4.0F, 48.0F, 8.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-8.0F, -8.0F, -10.0F, 0.2234F, -0.4824F, -0.0764F));

		PartDefinition leg14 = leg13.addOrReplaceChild("leg14",
				CubeListBuilder.create().texOffs(2, 161).addBox(-48.0F, -3.0F, -3.0F, 48.0F, 7.0F, 6.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-48.0F, 0.0F, 0.0F, -0.0844F, -0.2085F, -1.0491F));

		PartDefinition leg15 = leg14.addOrReplaceChild("leg15",
				CubeListBuilder.create().texOffs(63, 213).addBox(-11.0F, -1.0F, -2.0F, 11.0F, 4.0F, 4.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-48.0F, 0.0F, 0.0F, -0.0039F, 0.1095F, 1.1479F));

		PartDefinition leg4 = all.addOrReplaceChild("leg4",
				CubeListBuilder.create().texOffs(112, 159).addBox(0.0F, -4.0F, -4.0F, 48.0F, 8.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(8.0F, -8.0F, -10.0F, 0.2234F, 0.4824F, 0.0764F));

		PartDefinition leg5 = leg4.addOrReplaceChild("leg5",
				CubeListBuilder.create().texOffs(2, 178).addBox(0.0F, -3.0F, -3.0F, 48.0F, 7.0F, 6.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(48.0F, 0.0F, 0.0F, -0.0844F, 0.2085F, 1.0491F));

		PartDefinition leg6 = leg5.addOrReplaceChild("leg6",
				CubeListBuilder.create().texOffs(202, 213).addBox(0.0F, -1.0F, -2.0F, 11.0F, 4.0F, 4.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(48.0F, 0.0F, 0.0F, -0.0039F, -0.1095F, -1.1479F));

		PartDefinition leg10 = all.addOrReplaceChild("leg10",
				CubeListBuilder.create().texOffs(112, 175).addBox(-48.0F, -4.0F, -4.0F, 48.0F, 8.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-8.0F, -8.0F, 12.0F, -0.0788F, 0.4062F, -0.0995F));

		PartDefinition leg11 = leg10.addOrReplaceChild("leg11", CubeListBuilder.create(),
				PartPose.offsetAndRotation(-48.0F, 0.0F, 0.0F, 0.6098F, 0.1007F, -1.0571F));

		PartDefinition leg11_r1 = leg11.addOrReplaceChild("leg11_r1",
				CubeListBuilder.create().texOffs(184, 102).addBox(-24.0F, -3.5F, -3.0F, 48.0F, 7.0F, 6.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-24.0F, 0.5F, 0.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition leg12 = leg11.addOrReplaceChild("leg12", CubeListBuilder.create(),
				PartPose.offsetAndRotation(-48.0F, 0.0F, 0.0F, -0.1777F, 0.0312F, 1.3064F));

		PartDefinition leg12_r1 = leg12.addOrReplaceChild("leg12_r1",
				CubeListBuilder.create().texOffs(11, 229).addBox(-11.5F, -2.0F, -2.0F, 11.0F, 4.0F, 4.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.5F, 1.0F, 0.0F, -0.0902F, 0.4721F, -0.1962F));

		PartDefinition leg1 = all.addOrReplaceChild("leg1",
				CubeListBuilder.create().texOffs(182, 116).addBox(0.0F, -4.0F, -4.0F, 48.0F, 8.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(8.0F, -8.0F, 12.0F, -0.0788F, -0.4062F, 0.0995F));

		PartDefinition leg2 = leg1.addOrReplaceChild("leg2", CubeListBuilder.create(),
				PartPose.offsetAndRotation(48.0F, 0.0F, 0.0F, 0.6098F, -0.1007F, 1.0571F));

		PartDefinition leg2_r1 = leg2.addOrReplaceChild("leg2_r1",
				CubeListBuilder.create().texOffs(2, 194).addBox(-24.0F, -3.5F, -3.0F, 48.0F, 7.0F, 6.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(24.0F, 0.5F, 0.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition leg3 = leg2.addOrReplaceChild("leg3", CubeListBuilder.create(),
				PartPose.offsetAndRotation(48.0F, 0.0F, 0.0F, -0.1777F, -0.0312F, -1.3064F));

		PartDefinition leg3_r1 = leg3.addOrReplaceChild("leg3_r1",
				CubeListBuilder.create().texOffs(58, 229).addBox(0.5F, -2.0F, -2.0F, 11.0F, 4.0F, 4.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 1.0F, 0.0F, -0.0902F, -0.4721F, 0.1962F));

		PartDefinition headpiece5 = partdefinition.addOrReplaceChild("headpiece5", CubeListBuilder.create(),
				PartPose.offset(0.3432F, -23.4628F, -18.8602F));

		PartDefinition headpiece7 = partdefinition.addOrReplaceChild("headpiece7", CubeListBuilder.create(),
				PartPose.offset(0.6569F, -23.4628F, -18.8602F));

		PartDefinition headpiece6 = partdefinition.addOrReplaceChild("headpiece6", CubeListBuilder.create(),
				PartPose.offset(0.3432F, -22.4628F, -18.8602F));

		PartDefinition headpiece9 = partdefinition.addOrReplaceChild("headpiece9", CubeListBuilder.create(),
				PartPose.offset(0.6569F, -22.4628F, -18.8602F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		all.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		headpiece5.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		headpiece7.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		headpiece6.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		headpiece9.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
		this.head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.head.xRot = headPitch / (180F / (float) Math.PI);
	}
}