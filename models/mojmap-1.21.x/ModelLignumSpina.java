// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelLignumSpina<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "lignumspina"), "main");
	private final ModelPart root;
	private final ModelPart left_leg;
	private final ModelPart left_legs;
	private final ModelPart left_1;
	private final ModelPart left_2;
	private final ModelPart left_3;
	private final ModelPart left_4;
	private final ModelPart left_5;
	private final ModelPart right_leg;
	private final ModelPart right_legs;
	private final ModelPart right_1;
	private final ModelPart right_5;
	private final ModelPart right_2;
	private final ModelPart right_3;
	private final ModelPart right_4;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart spines;
	private final ModelPart bone;
	private final ModelPart bone4;
	private final ModelPart bone6;
	private final ModelPart bone10;
	private final ModelPart bone12;
	private final ModelPart bone7;
	private final ModelPart bone2;
	private final ModelPart bone3;
	private final ModelPart bone5;
	private final ModelPart bone8;
	private final ModelPart bone9;
	private final ModelPart bone11;

	public ModelLignumSpina(ModelPart root) {
		this.root = root.getChild("root");
		this.left_leg = this.root.getChild("left_leg");
		this.left_legs = this.left_leg.getChild("left_legs");
		this.left_1 = this.left_legs.getChild("left_1");
		this.left_2 = this.left_legs.getChild("left_2");
		this.left_3 = this.left_legs.getChild("left_3");
		this.left_4 = this.left_legs.getChild("left_4");
		this.left_5 = this.left_legs.getChild("left_5");
		this.right_leg = this.root.getChild("right_leg");
		this.right_legs = this.right_leg.getChild("right_legs");
		this.right_1 = this.right_legs.getChild("right_1");
		this.right_5 = this.right_legs.getChild("right_5");
		this.right_2 = this.right_legs.getChild("right_2");
		this.right_3 = this.right_legs.getChild("right_3");
		this.right_4 = this.right_legs.getChild("right_4");
		this.head = this.root.getChild("head");
		this.body = this.root.getChild("body");
		this.spines = this.body.getChild("spines");
		this.bone = this.spines.getChild("bone");
		this.bone4 = this.spines.getChild("bone4");
		this.bone6 = this.spines.getChild("bone6");
		this.bone10 = this.spines.getChild("bone10");
		this.bone12 = this.spines.getChild("bone12");
		this.bone7 = this.spines.getChild("bone7");
		this.bone2 = this.spines.getChild("bone2");
		this.bone3 = this.spines.getChild("bone3");
		this.bone5 = this.spines.getChild("bone5");
		this.bone8 = this.spines.getChild("bone8");
		this.bone9 = this.spines.getChild("bone9");
		this.bone11 = this.spines.getChild("bone11");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(),
				PartPose.offset(0.0F, 18.9464F, 0.1818F));

		PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
				PartPose.offset(1.5746F, 1.6168F, -2.1818F));

		PartDefinition left_legs = left_leg.addOrReplaceChild("left_legs", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition left_1 = left_legs.addOrReplaceChild("left_1", CubeListBuilder.create(),
				PartPose.offset(-1.0F, 1.0F, 0.0F));

		PartDefinition cube_r1 = left_1.addOrReplaceChild("cube_r1",
				CubeListBuilder.create().texOffs(4, 17).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, -1.5708F, 0.5672F));

		PartDefinition cube_r2 = left_1.addOrReplaceChild("cube_r2",
				CubeListBuilder.create().texOffs(2, 17).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.5672F));

		PartDefinition left_2 = left_legs.addOrReplaceChild("left_2", CubeListBuilder.create(),
				PartPose.offset(-1.0F, 1.0F, -2.0F));

		PartDefinition cube_r3 = left_2.addOrReplaceChild("cube_r3",
				CubeListBuilder.create().texOffs(6, 17).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, -1.5708F, 0.5672F));

		PartDefinition cube_r4 = left_2.addOrReplaceChild("cube_r4",
				CubeListBuilder.create().texOffs(8, 17).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.5672F));

		PartDefinition left_3 = left_legs.addOrReplaceChild("left_3", CubeListBuilder.create(),
				PartPose.offset(-1.0F, 1.0F, -4.0F));

		PartDefinition cube_r5 = left_3.addOrReplaceChild("cube_r5",
				CubeListBuilder.create().texOffs(20, 18).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, -1.5708F, 0.5672F));

		PartDefinition cube_r6 = left_3.addOrReplaceChild("cube_r6",
				CubeListBuilder.create().texOffs(20, 11).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.5672F));

		PartDefinition left_4 = left_legs.addOrReplaceChild("left_4", CubeListBuilder.create(),
				PartPose.offset(-1.0F, 1.0F, -6.0F));

		PartDefinition cube_r7 = left_4.addOrReplaceChild("cube_r7",
				CubeListBuilder.create().texOffs(14, 22).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.5672F));

		PartDefinition cube_r8 = left_4.addOrReplaceChild("cube_r8",
				CubeListBuilder.create().texOffs(12, 22).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, -1.5708F, 0.5672F));

		PartDefinition left_5 = left_legs.addOrReplaceChild("left_5", CubeListBuilder.create(),
				PartPose.offset(-1.0746F, 1.6468F, -7.6349F));

		PartDefinition cube_r9 = left_5.addOrReplaceChild("cube_r9",
				CubeListBuilder.create().texOffs(0, 17).addBox(0.0F, -6.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.138F, -1.3526F, -1.5708F));

		PartDefinition cube_r10 = left_5.addOrReplaceChild("cube_r10",
				CubeListBuilder.create().texOffs(16, 15).addBox(0.0F, -6.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1848F, -0.1166F, 0.5564F));

		PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
				PartPose.offset(-1.5746F, 1.6168F, -2.1818F));

		PartDefinition right_legs = right_leg.addOrReplaceChild("right_legs", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition right_1 = right_legs.addOrReplaceChild("right_1", CubeListBuilder.create(),
				PartPose.offset(1.0F, 1.0F, 0.0F));

		PartDefinition cube_r11 = right_1.addOrReplaceChild("cube_r11",
				CubeListBuilder.create().texOffs(18, 15).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.5672F));

		PartDefinition cube_r12 = right_1.addOrReplaceChild("cube_r12",
				CubeListBuilder.create().texOffs(10, 17).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.0F, 0.0F, 0.0F, 1.5708F, -0.5672F));

		PartDefinition right_5 = right_legs.addOrReplaceChild("right_5", CubeListBuilder.create(),
				PartPose.offset(1.0746F, 1.6468F, -7.6349F));

		PartDefinition cube_r13 = right_5.addOrReplaceChild("cube_r13",
				CubeListBuilder.create().texOffs(14, 15).addBox(0.0F, -6.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1848F, 0.1166F, -0.5564F));

		PartDefinition cube_r14 = right_5.addOrReplaceChild("cube_r14",
				CubeListBuilder.create().texOffs(12, 15).addBox(0.0F, -6.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.138F, 1.3526F, 1.5708F));

		PartDefinition right_2 = right_legs.addOrReplaceChild("right_2", CubeListBuilder.create(),
				PartPose.offset(1.0F, 1.0F, -2.0F));

		PartDefinition cube_r15 = right_2.addOrReplaceChild("cube_r15",
				CubeListBuilder.create().texOffs(22, 7).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.0F, 0.0F, 0.0F, 1.5708F, -0.5672F));

		PartDefinition cube_r16 = right_2.addOrReplaceChild("cube_r16",
				CubeListBuilder.create().texOffs(22, 0).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.5672F));

		PartDefinition right_3 = right_legs.addOrReplaceChild("right_3", CubeListBuilder.create(),
				PartPose.offset(1.0F, 1.0F, -4.0F));

		PartDefinition cube_r17 = right_3.addOrReplaceChild("cube_r17",
				CubeListBuilder.create().texOffs(16, 22).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.5672F));

		PartDefinition cube_r18 = right_3.addOrReplaceChild("cube_r18",
				CubeListBuilder.create().texOffs(22, 14).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.0F, 0.0F, 0.0F, 1.5708F, -0.5672F));

		PartDefinition right_4 = right_legs.addOrReplaceChild("right_4", CubeListBuilder.create(),
				PartPose.offset(1.0F, 1.0F, -6.0F));

		PartDefinition cube_r19 = right_4.addOrReplaceChild("cube_r19",
				CubeListBuilder.create().texOffs(22, 21).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.0F, 0.0F, 0.0F, 1.5708F, -0.5672F));

		PartDefinition cube_r20 = right_4.addOrReplaceChild("cube_r20",
				CubeListBuilder.create().texOffs(18, 22).addBox(0.0F, -4.0F, -0.5F, 0.0F, 6.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.5672F));

		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0893F, -3.7046F));

		PartDefinition head_r1 = head.addOrReplaceChild("head_r1",
				CubeListBuilder.create().texOffs(0, 11).addBox(-1.0F, -1.0F, -3.5F, 2.0F, 2.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.2883F, -0.363F, 0.48F, 0.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -6.0F,
				-4.0F, 2.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.0536F, 0.3182F));

		PartDefinition body_r1 = body.addOrReplaceChild("body_r1",
				CubeListBuilder.create().texOffs(12, 11).addBox(-1.0F, -1.0F, -0.75F, 2.0F, 2.0F, 2.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, -4.75F, 5.25F, -0.4363F, 0.0F, 0.0F));

		PartDefinition spines = body.addOrReplaceChild("spines", CubeListBuilder.create(),
				PartPose.offset(0.0F, -7.4368F, 0.5F));

		PartDefinition bone = spines.addOrReplaceChild("bone", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.8246F, 1.5F, -3.0F, 0.2182F, 0.0F, 0.0F));

		PartDefinition cube_r21 = bone.addOrReplaceChild("cube_r21",
				CubeListBuilder.create().texOffs(24, 6).addBox(0.0F, -2.0F, -0.5F, 0.0F, 4.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.75F, -1.5F, 0.0F, 0.0F, -1.5708F, 0.5672F));

		PartDefinition cube_r22 = bone.addOrReplaceChild("cube_r22",
				CubeListBuilder.create().texOffs(26, 0).addBox(0.0F, -1.0F, -0.5F, 0.0F, 3.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.75F, -1.5F, 0.0F, 0.0F, 0.0F, 0.5672F));

		PartDefinition bone4 = spines.addOrReplaceChild("bone4", CubeListBuilder.create(),
				PartPose.offset(0.8246F, 1.5F, -1.0F));

		PartDefinition cube_r23 = bone4.addOrReplaceChild("cube_r23",
				CubeListBuilder.create().texOffs(10, 24).addBox(0.0F, -2.0F, -0.5F, 0.0F, 4.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.75F, -1.5F, 0.0F, 0.0F, -1.5708F, 0.5672F));

		PartDefinition cube_r24 = bone4.addOrReplaceChild("cube_r24",
				CubeListBuilder.create().texOffs(24, 0).addBox(0.0F, -3.0F, -0.5F, 0.0F, 5.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.75F, -1.5F, 0.0F, 0.0F, 0.0F, 0.5672F));

		PartDefinition bone6 = spines.addOrReplaceChild("bone6", CubeListBuilder.create(),
				PartPose.offset(0.5746F, 1.5F, 1.0F));

		PartDefinition cube_r25 = bone6.addOrReplaceChild("cube_r25",
				CubeListBuilder.create().texOffs(24, 16).addBox(0.0F, -2.0F, -0.5F, 0.0F, 4.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.5F, 0.0F, 0.0F, -1.5708F, 0.5672F));

		PartDefinition cube_r26 = bone6.addOrReplaceChild("cube_r26",
				CubeListBuilder.create().texOffs(4, 24).addBox(0.0F, -3.0F, -0.5F, 0.0F, 5.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.5F, 0.0F, 0.0F, 0.0F, 0.5672F));

		PartDefinition bone10 = spines.addOrReplaceChild("bone10", CubeListBuilder.create(),
				PartPose.offset(0.5746F, 1.5F, 3.0F));

		PartDefinition cube_r27 = bone10.addOrReplaceChild("cube_r27",
				CubeListBuilder.create().texOffs(20, 25).addBox(0.0F, -2.0F, -0.5F, 0.0F, 4.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.5F, 0.0F, 0.0F, -1.5708F, 0.5672F));

		PartDefinition cube_r28 = bone10.addOrReplaceChild("cube_r28",
				CubeListBuilder.create().texOffs(26, 12).addBox(0.0F, -1.0F, -0.5F, 0.0F, 3.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.5F, 0.0F, 0.0F, 0.0F, 0.5672F));

		PartDefinition bone12 = spines.addOrReplaceChild("bone12", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.806F, 1.7527F, 4.4738F, -0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r29 = bone12.addOrReplaceChild("cube_r29",
				CubeListBuilder.create().texOffs(24, 26).addBox(0.0F, -1.0F, -0.5F, 0.0F, 3.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.7686F, -1.4217F, 0.0F, 0.0F, -1.5708F, 0.5672F));

		PartDefinition cube_r30 = bone12.addOrReplaceChild("cube_r30",
				CubeListBuilder.create().texOffs(26, 24).addBox(0.0F, -1.0F, -0.5F, 0.0F, 3.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.7686F, -1.4217F, 0.0F, 0.0F, 0.0F, 0.5672F));

		PartDefinition bone7 = spines.addOrReplaceChild("bone7", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.5746F, 2.5F, 6.25F, -0.6109F, 0.0F, 0.0F));

		PartDefinition cube_r31 = bone7.addOrReplaceChild("cube_r31",
				CubeListBuilder.create().texOffs(28, 3).addBox(0.0F, 0.0F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.669F, -0.5262F, 0.0F, -1.5708F, 0.5672F));

		PartDefinition cube_r32 = bone7.addOrReplaceChild("cube_r32",
				CubeListBuilder.create().texOffs(28, 0).addBox(0.0F, 0.0F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.0F, -1.669F, -0.5262F, 0.0F, 0.0F, 0.5672F));

		PartDefinition bone2 = spines.addOrReplaceChild("bone2", CubeListBuilder.create(),
				PartPose.offsetAndRotation(-0.8246F, 1.5F, -3.0F, 0.2182F, 0.0F, 0.0F));

		PartDefinition cube_r33 = bone2.addOrReplaceChild("cube_r33",
				CubeListBuilder.create().texOffs(26, 4).addBox(0.0F, -1.0F, -0.5F, 0.0F, 3.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-0.75F, -1.5F, 0.0F, 0.0F, 0.0F, -0.5672F));

		PartDefinition cube_r34 = bone2.addOrReplaceChild("cube_r34",
				CubeListBuilder.create().texOffs(6, 24).addBox(0.0F, -2.0F, -0.5F, 0.0F, 4.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-0.75F, -1.5F, 0.0F, 0.0F, 1.5708F, -0.5672F));

		PartDefinition bone3 = spines.addOrReplaceChild("bone3", CubeListBuilder.create(),
				PartPose.offset(-0.8246F, 1.5F, -1.0F));

		PartDefinition cube_r35 = bone3.addOrReplaceChild("cube_r35",
				CubeListBuilder.create().texOffs(0, 24).addBox(0.0F, -3.0F, -0.5F, 0.0F, 5.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-0.75F, -1.5F, 0.0F, 0.0F, 0.0F, -0.5672F));

		PartDefinition cube_r36 = bone3.addOrReplaceChild("cube_r36",
				CubeListBuilder.create().texOffs(8, 24).addBox(0.0F, -2.0F, -0.5F, 0.0F, 4.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-0.75F, -1.5F, 0.0F, 0.0F, 1.5708F, -0.5672F));

		PartDefinition bone5 = spines.addOrReplaceChild("bone5", CubeListBuilder.create(),
				PartPose.offset(-0.5746F, 1.5F, 1.0F));

		PartDefinition cube_r37 = bone5.addOrReplaceChild("cube_r37",
				CubeListBuilder.create().texOffs(2, 24).addBox(0.0F, -3.0F, -0.5F, 0.0F, 5.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.5F, 0.0F, 0.0F, 0.0F, -0.5672F));

		PartDefinition cube_r38 = bone5.addOrReplaceChild("cube_r38",
				CubeListBuilder.create().texOffs(24, 11).addBox(0.0F, -2.0F, -0.5F, 0.0F, 4.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.5F, 0.0F, 0.0F, 1.5708F, -0.5672F));

		PartDefinition bone8 = spines.addOrReplaceChild("bone8", CubeListBuilder.create(),
				PartPose.offset(-0.5746F, 1.5F, 3.0F));

		PartDefinition cube_r39 = bone8.addOrReplaceChild("cube_r39",
				CubeListBuilder.create().texOffs(26, 8).addBox(0.0F, -1.0F, -0.5F, 0.0F, 3.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.5F, 0.0F, 0.0F, 0.0F, -0.5672F));

		PartDefinition cube_r40 = bone8.addOrReplaceChild("cube_r40",
				CubeListBuilder.create().texOffs(24, 21).addBox(0.0F, -2.0F, -0.5F, 0.0F, 4.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.5F, 0.0F, 0.0F, 1.5708F, -0.5672F));

		PartDefinition bone9 = spines.addOrReplaceChild("bone9", CubeListBuilder.create(),
				PartPose.offsetAndRotation(-0.806F, 1.7527F, 4.4738F, -0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r41 = bone9.addOrReplaceChild("cube_r41",
				CubeListBuilder.create().texOffs(26, 20).addBox(0.0F, -1.0F, -0.5F, 0.0F, 3.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-0.7686F, -1.4217F, 0.0F, 0.0F, 0.0F, -0.5672F));

		PartDefinition cube_r42 = bone9.addOrReplaceChild("cube_r42",
				CubeListBuilder.create().texOffs(26, 16).addBox(0.0F, -1.0F, -0.5F, 0.0F, 3.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-0.7686F, -1.4217F, 0.0F, 0.0F, 1.5708F, -0.5672F));

		PartDefinition bone11 = spines.addOrReplaceChild("bone11", CubeListBuilder.create(),
				PartPose.offsetAndRotation(-0.5746F, 2.5F, 6.25F, -0.6109F, 0.0F, 0.0F));

		PartDefinition cube_r43 = bone11.addOrReplaceChild("cube_r43",
				CubeListBuilder.create().texOffs(28, 9).addBox(0.0F, 0.0F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.669F, -0.5262F, 0.0F, 0.0F, -0.5672F));

		PartDefinition cube_r44 = bone11.addOrReplaceChild("cube_r44",
				CubeListBuilder.create().texOffs(28, 6).addBox(0.0F, 0.0F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-1.0F, -1.669F, -0.5262F, 0.0F, 1.5708F, -0.5672F));

		return LayerDefinition.create(meshdefinition, 32, 32);
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