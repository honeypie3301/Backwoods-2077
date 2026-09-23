// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelLignumTrilobita<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "lignumtrilobita"), "main");
	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart pleurals;
	private final ModelPart left_leg;
	private final ModelPart left_1;
	private final ModelPart left_2;
	private final ModelPart left_3;
	private final ModelPart left_4;
	private final ModelPart left_5;
	private final ModelPart left_6;
	private final ModelPart left_7;
	private final ModelPart right_leg;
	private final ModelPart right_1;
	private final ModelPart right_2;
	private final ModelPart right_3;
	private final ModelPart right_4;
	private final ModelPart right_5;
	private final ModelPart right_6;
	private final ModelPart right_7;

	public ModelLignumTrilobita(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.pleurals = this.body.getChild("pleurals");
		this.left_leg = this.root.getChild("left_leg");
		this.left_1 = this.left_leg.getChild("left_1");
		this.left_2 = this.left_leg.getChild("left_2");
		this.left_3 = this.left_leg.getChild("left_3");
		this.left_4 = this.left_leg.getChild("left_4");
		this.left_5 = this.left_leg.getChild("left_5");
		this.left_6 = this.left_leg.getChild("left_6");
		this.left_7 = this.left_leg.getChild("left_7");
		this.right_leg = this.root.getChild("right_leg");
		this.right_1 = this.right_leg.getChild("right_1");
		this.right_2 = this.right_leg.getChild("right_2");
		this.right_3 = this.right_leg.getChild("right_3");
		this.right_4 = this.right_leg.getChild("right_4");
		this.right_5 = this.right_leg.getChild("right_5");
		this.right_6 = this.right_leg.getChild("right_6");
		this.right_7 = this.right_leg.getChild("right_7");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(),
				PartPose.offset(0.0F, 25.0F, 0.8333F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, -1.3333F));

		PartDefinition pleurals = body.addOrReplaceChild("pleurals",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(0.25F, -1.0F, -3.25F, 2.0F, 2.0F, 17.0F, new CubeDeformation(0.0F)).texOffs(36, 31)
						.addBox(0.25F, 0.0F, 12.0F, 2.0F, 0.0F, 6.0F, new CubeDeformation(0.001F)).texOffs(38, 16)
						.addBox(-1.75F, 0.0F, -5.0F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0011F)),
				PartPose.offset(-1.25F, -4.0F, -4.75F));

		PartDefinition cube_r1 = pleurals.addOrReplaceChild("cube_r1",
				CubeListBuilder.create().texOffs(52, 31).addBox(-1.0F, 0.031F, -2.3F, 2.0F, 2.0F, 2.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(1.25F, -1.151F, -3.0F, 0.5061F, 0.0F, 0.0F));

		PartDefinition pleural_r1 = pleurals.addOrReplaceChild("pleural_r1",
				CubeListBuilder.create().texOffs(32, 49).addBox(-3.0F, 0.0F, -3.0F, 4.0F, 0.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2618F, 0.0F));

		PartDefinition pleural_r2 = pleurals.addOrReplaceChild("pleural_r2",
				CubeListBuilder.create().texOffs(48, 51).addBox(-3.0F, 0.001F, -2.0F, 6.0F, 0.0F, 2.0F,
						new CubeDeformation(0.0001F)),
				PartPose.offsetAndRotation(3.0F, -0.001F, -1.7F, 0.0F, -1.3526F, 0.0F));

		PartDefinition pleural_r3 = pleurals.addOrReplaceChild("pleural_r3",
				CubeListBuilder.create().texOffs(48, 49).addBox(-3.0F, 0.001F, -2.0F, 6.0F, 0.0F, 2.0F,
						new CubeDeformation(0.0001F)),
				PartPose.offsetAndRotation(-0.5F, -0.001F, -1.7F, 0.0F, 1.3526F, 0.0F));

		PartDefinition pleural_r4 = pleurals.addOrReplaceChild("pleural_r4",
				CubeListBuilder.create().texOffs(36, 25).addBox(-1.0F, -0.001F, -3.0F, 4.0F, 0.0F, 6.0F,
						new CubeDeformation(0.0015F)),
				PartPose.offsetAndRotation(2.5F, 0.0F, 13.0F, 0.0F, -0.2618F, 0.0F));

		PartDefinition pleural_r5 = pleurals.addOrReplaceChild("pleural_r5",
				CubeListBuilder.create().texOffs(36, 45).addBox(-1.0F, 0.0F, -3.0F, 4.0F, 0.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(2.5F, 0.0F, 11.0F, 0.0F, -0.2618F, 0.0F));

		PartDefinition pleural_r6 = pleurals.addOrReplaceChild("pleural_r6",
				CubeListBuilder.create().texOffs(38, 12).addBox(-1.0F, -0.001F, -3.0F, 4.0F, 0.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(2.5F, 0.0F, 8.0F, 0.0F, -0.2618F, 0.0F));

		PartDefinition pleural_r7 = pleurals.addOrReplaceChild("pleural_r7",
				CubeListBuilder.create().texOffs(38, 8).addBox(-1.0F, 0.0F, -3.0F, 4.0F, 0.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(2.5F, 0.0F, 5.0F, 0.0F, -0.2618F, 0.0F));

		PartDefinition pleural_r8 = pleurals.addOrReplaceChild("pleural_r8",
				CubeListBuilder.create().texOffs(38, 0).addBox(-1.0F, -0.001F, -3.0F, 4.0F, 0.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(2.5F, 0.0F, 2.0F, 0.0F, -0.2618F, 0.0F));

		PartDefinition pleural_r9 = pleurals.addOrReplaceChild("pleural_r9",
				CubeListBuilder.create().texOffs(0, 34).addBox(-1.4F, 0.0F, -3.0F, 3.0F, 0.0F, 15.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(2.5F, 0.5F, 0.0F, 0.0804F, -0.2494F, -0.3155F));

		PartDefinition pleural_r10 = pleurals.addOrReplaceChild("pleural_r10",
				CubeListBuilder.create().texOffs(36, 37).addBox(-1.0F, 0.0F, -3.0F, 4.0F, 0.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(2.5F, 0.0F, 0.0F, 0.0F, -0.2618F, 0.0F));

		PartDefinition pleural_r11 = pleurals.addOrReplaceChild("pleural_r11",
				CubeListBuilder.create().texOffs(36, 19).addBox(-3.0F, -0.001F, -3.0F, 4.0F, 0.0F, 6.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 13.0F, 0.0F, 0.2618F, 0.0F));

		PartDefinition pleural_r12 = pleurals.addOrReplaceChild("pleural_r12",
				CubeListBuilder.create().texOffs(38, 4).addBox(-3.0F, 0.0F, -3.0F, 4.0F, 0.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 11.0F, 0.0F, 0.2618F, 0.0F));

		PartDefinition pleural_r13 = pleurals.addOrReplaceChild("pleural_r13",
				CubeListBuilder.create().texOffs(16, 49).addBox(-3.0F, -0.001F, -3.0F, 4.0F, 0.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 8.0F, 0.0F, 0.2618F, 0.0F));

		PartDefinition pleural_r14 = pleurals.addOrReplaceChild("pleural_r14",
				CubeListBuilder.create().texOffs(0, 49).addBox(-3.0F, 0.0F, -3.0F, 4.0F, 0.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, 0.0F, 0.2618F, 0.0F));

		PartDefinition pleural_r15 = pleurals.addOrReplaceChild("pleural_r15",
				CubeListBuilder.create().texOffs(36, 41).addBox(-3.0F, -0.001F, -3.0F, 4.0F, 0.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, 0.2618F, 0.0F));

		PartDefinition pleural_r16 = pleurals.addOrReplaceChild("pleural_r16",
				CubeListBuilder.create().texOffs(0, 19).addBox(-1.6F, 0.0F, -3.0F, 3.0F, 0.0F, 15.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.0804F, 0.2494F, 0.3155F));

		PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.6667F));

		PartDefinition left_1 = left_leg.addOrReplaceChild("left_1", CubeListBuilder.create(),
				PartPose.offset(0.8478F, -3.1983F, -7.5F));

		PartDefinition left_legs_r1 = left_1.addOrReplaceChild("left_legs_r1",
				CubeListBuilder.create().texOffs(52, 45).addBox(-0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, -0.5236F));

		PartDefinition left_legs_r2 = left_1
				.addOrReplaceChild("left_legs_r2",
						CubeListBuilder.create().texOffs(52, 35).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.3526F));

		PartDefinition left_2 = left_leg.addOrReplaceChild("left_2", CubeListBuilder.create(),
				PartPose.offset(0.8478F, -3.1983F, -5.5F));

		PartDefinition left_legs_r3 = left_2.addOrReplaceChild("left_legs_r3",
				CubeListBuilder.create().texOffs(26, 53).addBox(-0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, -0.5236F));

		PartDefinition left_legs_r4 = left_2
				.addOrReplaceChild("left_legs_r4",
						CubeListBuilder.create().texOffs(0, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.3526F));

		PartDefinition left_3 = left_leg.addOrReplaceChild("left_3", CubeListBuilder.create(),
				PartPose.offset(0.8478F, -3.1983F, -3.5F));

		PartDefinition left_legs_r5 = left_3.addOrReplaceChild("left_legs_r5",
				CubeListBuilder.create().texOffs(30, 53).addBox(-0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, -0.5236F));

		PartDefinition left_legs_r6 = left_3
				.addOrReplaceChild("left_legs_r6",
						CubeListBuilder.create().texOffs(4, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.3526F));

		PartDefinition left_4 = left_leg.addOrReplaceChild("left_4", CubeListBuilder.create(),
				PartPose.offset(0.8478F, -3.1983F, -1.5F));

		PartDefinition left_legs_r7 = left_4.addOrReplaceChild("left_legs_r7",
				CubeListBuilder.create().texOffs(34, 53).addBox(-0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, -0.5236F));

		PartDefinition left_legs_r8 = left_4
				.addOrReplaceChild("left_legs_r8",
						CubeListBuilder.create().texOffs(8, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.3526F));

		PartDefinition left_5 = left_leg.addOrReplaceChild("left_5", CubeListBuilder.create(),
				PartPose.offset(0.8478F, -3.1983F, 0.5F));

		PartDefinition left_legs_r9 = left_5.addOrReplaceChild("left_legs_r9",
				CubeListBuilder.create().texOffs(38, 53).addBox(-0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, -0.5236F));

		PartDefinition left_legs_r10 = left_5
				.addOrReplaceChild("left_legs_r10",
						CubeListBuilder.create().texOffs(12, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.3526F));

		PartDefinition left_6 = left_leg.addOrReplaceChild("left_6", CubeListBuilder.create(),
				PartPose.offset(0.8478F, -3.1983F, 2.5F));

		PartDefinition left_legs_r11 = left_6.addOrReplaceChild("left_legs_r11",
				CubeListBuilder.create().texOffs(42, 53).addBox(-0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, -0.5236F));

		PartDefinition left_legs_r12 = left_6
				.addOrReplaceChild("left_legs_r12",
						CubeListBuilder.create().texOffs(16, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.3526F));

		PartDefinition left_7 = left_leg.addOrReplaceChild("left_7", CubeListBuilder.create(),
				PartPose.offset(0.8478F, -3.1983F, 4.5F));

		PartDefinition left_legs_r13 = left_7.addOrReplaceChild("left_legs_r13",
				CubeListBuilder.create().texOffs(46, 53).addBox(-0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, -0.5236F));

		PartDefinition left_legs_r14 = left_7
				.addOrReplaceChild("left_legs_r14",
						CubeListBuilder.create().texOffs(20, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.3526F));

		PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.6667F));

		PartDefinition right_1 = right_leg.addOrReplaceChild("right_1", CubeListBuilder.create(),
				PartPose.offset(-0.8478F, -3.1983F, 4.5F));

		PartDefinition right_legs_r1 = right_1
				.addOrReplaceChild("right_legs_r1",
						CubeListBuilder.create().texOffs(52, 40).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.3526F));

		PartDefinition right_legs_r2 = right_1.addOrReplaceChild("right_legs_r2",
				CubeListBuilder.create().texOffs(24, 53).addBox(0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, 0.5236F));

		PartDefinition right_2 = right_leg.addOrReplaceChild("right_2", CubeListBuilder.create(),
				PartPose.offset(-0.8478F, -3.1983F, -7.5F));

		PartDefinition right_legs_r3 = right_2.addOrReplaceChild("right_legs_r3",
				CubeListBuilder.create().texOffs(28, 53).addBox(0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, 0.5236F));

		PartDefinition right_legs_r4 = right_2
				.addOrReplaceChild("right_legs_r4",
						CubeListBuilder.create().texOffs(2, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.3526F));

		PartDefinition right_3 = right_leg.addOrReplaceChild("right_3", CubeListBuilder.create(),
				PartPose.offset(-0.8478F, -3.1983F, -5.5F));

		PartDefinition right_legs_r5 = right_3.addOrReplaceChild("right_legs_r5",
				CubeListBuilder.create().texOffs(32, 53).addBox(0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, 0.5236F));

		PartDefinition right_legs_r6 = right_3
				.addOrReplaceChild("right_legs_r6",
						CubeListBuilder.create().texOffs(6, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.3526F));

		PartDefinition right_4 = right_leg.addOrReplaceChild("right_4", CubeListBuilder.create(),
				PartPose.offset(-0.8478F, -3.1983F, -3.5F));

		PartDefinition right_legs_r7 = right_4.addOrReplaceChild("right_legs_r7",
				CubeListBuilder.create().texOffs(36, 53).addBox(0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, 0.5236F));

		PartDefinition right_legs_r8 = right_4
				.addOrReplaceChild("right_legs_r8",
						CubeListBuilder.create().texOffs(10, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.3526F));

		PartDefinition right_5 = right_leg.addOrReplaceChild("right_5", CubeListBuilder.create(),
				PartPose.offset(-0.8478F, -3.1983F, -1.5F));

		PartDefinition right_legs_r9 = right_5.addOrReplaceChild("right_legs_r9",
				CubeListBuilder.create().texOffs(40, 53).addBox(0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, 0.5236F));

		PartDefinition right_legs_r10 = right_5
				.addOrReplaceChild("right_legs_r10",
						CubeListBuilder.create().texOffs(14, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.3526F));

		PartDefinition right_6 = right_leg.addOrReplaceChild("right_6", CubeListBuilder.create(),
				PartPose.offset(-0.8478F, -3.1983F, 0.5F));

		PartDefinition right_legs_r11 = right_6.addOrReplaceChild("right_legs_r11",
				CubeListBuilder.create().texOffs(44, 53).addBox(0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, 0.5236F));

		PartDefinition right_legs_r12 = right_6
				.addOrReplaceChild("right_legs_r12",
						CubeListBuilder.create().texOffs(18, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.3526F));

		PartDefinition right_7 = right_leg.addOrReplaceChild("right_7", CubeListBuilder.create(),
				PartPose.offset(-0.8478F, -3.1983F, 2.5F));

		PartDefinition right_legs_r13 = right_7.addOrReplaceChild("right_legs_r13",
				CubeListBuilder.create().texOffs(48, 53).addBox(0.0194F, -0.43F, -0.5F, 0.0F, 2.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.424F, 0.8483F, 0.0F, 0.0F, 0.0F, 0.5236F));

		PartDefinition right_legs_r14 = right_7
				.addOrReplaceChild("right_legs_r14",
						CubeListBuilder.create().texOffs(22, 53).addBox(0.0F, -1.75F, -0.5F, 0.0F, 4.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.3526F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}