// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelLignumEchinus<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "lignumechinus"), "main");
	private final ModelPart body;
	private final ModelPart spikes;
	private final ModelPart spikeset;
	private final ModelPart spikeset2;
	private final ModelPart spikeset3;
	private final ModelPart spikeset4;
	private final ModelPart spikeset5;
	private final ModelPart spikeset6;
	private final ModelPart spikeset7;
	private final ModelPart spikeset8;
	private final ModelPart spikes2;
	private final ModelPart spikeset9;
	private final ModelPart spikeset10;
	private final ModelPart spikeset11;
	private final ModelPart spikeset12;
	private final ModelPart spikeset13;
	private final ModelPart spikeset14;
	private final ModelPart spikeset15;
	private final ModelPart spikeset16;
	private final ModelPart spikes3;
	private final ModelPart spikeset17;
	private final ModelPart spikeset18;
	private final ModelPart spikeset19;
	private final ModelPart spikeset20;
	private final ModelPart spikeset21;
	private final ModelPart spikeset22;
	private final ModelPart spikeset23;
	private final ModelPart spikeset24;
	private final ModelPart spikes4;
	private final ModelPart spikeset25;
	private final ModelPart spikeset26;
	private final ModelPart spikeset27;
	private final ModelPart spikeset28;
	private final ModelPart spikeset29;
	private final ModelPart spikeset30;
	private final ModelPart spikeset31;
	private final ModelPart spikeset32;
	private final ModelPart spikes5;
	private final ModelPart spikeset33;
	private final ModelPart spikeset34;
	private final ModelPart spikeset35;
	private final ModelPart spikeset36;
	private final ModelPart spikeset37;
	private final ModelPart spikeset38;
	private final ModelPart spikeset39;
	private final ModelPart spikeset40;
	private final ModelPart spikes6;
	private final ModelPart spikeset41;
	private final ModelPart spikeset42;
	private final ModelPart spikeset43;
	private final ModelPart spikeset44;
	private final ModelPart spikeset45;
	private final ModelPart spikeset46;
	private final ModelPart spikeset47;
	private final ModelPart spikeset48;
	private final ModelPart spikes7;
	private final ModelPart spikeset49;
	private final ModelPart spikeset50;
	private final ModelPart spikeset51;
	private final ModelPart spikeset52;
	private final ModelPart spikeset53;
	private final ModelPart spikeset54;
	private final ModelPart spikeset55;
	private final ModelPart spikeset56;
	private final ModelPart spikes8;
	private final ModelPart spikeset57;
	private final ModelPart spikeset58;
	private final ModelPart spikeset59;
	private final ModelPart spikeset60;
	private final ModelPart spikeset61;
	private final ModelPart spikeset62;
	private final ModelPart spikeset63;
	private final ModelPart spikeset64;

	public ModelLignumEchinus(ModelPart root) {
		this.body = root.getChild("body");
		this.spikes = this.body.getChild("spikes");
		this.spikeset = this.spikes.getChild("spikeset");
		this.spikeset2 = this.spikes.getChild("spikeset2");
		this.spikeset3 = this.spikes.getChild("spikeset3");
		this.spikeset4 = this.spikes.getChild("spikeset4");
		this.spikeset5 = this.spikes.getChild("spikeset5");
		this.spikeset6 = this.spikes.getChild("spikeset6");
		this.spikeset7 = this.spikes.getChild("spikeset7");
		this.spikeset8 = this.spikes.getChild("spikeset8");
		this.spikes2 = this.body.getChild("spikes2");
		this.spikeset9 = this.spikes2.getChild("spikeset9");
		this.spikeset10 = this.spikes2.getChild("spikeset10");
		this.spikeset11 = this.spikes2.getChild("spikeset11");
		this.spikeset12 = this.spikes2.getChild("spikeset12");
		this.spikeset13 = this.spikes2.getChild("spikeset13");
		this.spikeset14 = this.spikes2.getChild("spikeset14");
		this.spikeset15 = this.spikes2.getChild("spikeset15");
		this.spikeset16 = this.spikes2.getChild("spikeset16");
		this.spikes3 = this.body.getChild("spikes3");
		this.spikeset17 = this.spikes3.getChild("spikeset17");
		this.spikeset18 = this.spikes3.getChild("spikeset18");
		this.spikeset19 = this.spikes3.getChild("spikeset19");
		this.spikeset20 = this.spikes3.getChild("spikeset20");
		this.spikeset21 = this.spikes3.getChild("spikeset21");
		this.spikeset22 = this.spikes3.getChild("spikeset22");
		this.spikeset23 = this.spikes3.getChild("spikeset23");
		this.spikeset24 = this.spikes3.getChild("spikeset24");
		this.spikes4 = this.body.getChild("spikes4");
		this.spikeset25 = this.spikes4.getChild("spikeset25");
		this.spikeset26 = this.spikes4.getChild("spikeset26");
		this.spikeset27 = this.spikes4.getChild("spikeset27");
		this.spikeset28 = this.spikes4.getChild("spikeset28");
		this.spikeset29 = this.spikes4.getChild("spikeset29");
		this.spikeset30 = this.spikes4.getChild("spikeset30");
		this.spikeset31 = this.spikes4.getChild("spikeset31");
		this.spikeset32 = this.spikes4.getChild("spikeset32");
		this.spikes5 = this.body.getChild("spikes5");
		this.spikeset33 = this.spikes5.getChild("spikeset33");
		this.spikeset34 = this.spikes5.getChild("spikeset34");
		this.spikeset35 = this.spikes5.getChild("spikeset35");
		this.spikeset36 = this.spikes5.getChild("spikeset36");
		this.spikeset37 = this.spikes5.getChild("spikeset37");
		this.spikeset38 = this.spikes5.getChild("spikeset38");
		this.spikeset39 = this.spikes5.getChild("spikeset39");
		this.spikeset40 = this.spikes5.getChild("spikeset40");
		this.spikes6 = this.body.getChild("spikes6");
		this.spikeset41 = this.spikes6.getChild("spikeset41");
		this.spikeset42 = this.spikes6.getChild("spikeset42");
		this.spikeset43 = this.spikes6.getChild("spikeset43");
		this.spikeset44 = this.spikes6.getChild("spikeset44");
		this.spikeset45 = this.spikes6.getChild("spikeset45");
		this.spikeset46 = this.spikes6.getChild("spikeset46");
		this.spikeset47 = this.spikes6.getChild("spikeset47");
		this.spikeset48 = this.spikes6.getChild("spikeset48");
		this.spikes7 = this.body.getChild("spikes7");
		this.spikeset49 = this.spikes7.getChild("spikeset49");
		this.spikeset50 = this.spikes7.getChild("spikeset50");
		this.spikeset51 = this.spikes7.getChild("spikeset51");
		this.spikeset52 = this.spikes7.getChild("spikeset52");
		this.spikeset53 = this.spikes7.getChild("spikeset53");
		this.spikeset54 = this.spikes7.getChild("spikeset54");
		this.spikeset55 = this.spikes7.getChild("spikeset55");
		this.spikeset56 = this.spikes7.getChild("spikeset56");
		this.spikes8 = this.body.getChild("spikes8");
		this.spikeset57 = this.spikes8.getChild("spikeset57");
		this.spikeset58 = this.spikes8.getChild("spikeset58");
		this.spikeset59 = this.spikes8.getChild("spikeset59");
		this.spikeset60 = this.spikes8.getChild("spikeset60");
		this.spikeset61 = this.spikes8.getChild("spikeset61");
		this.spikeset62 = this.spikes8.getChild("spikeset62");
		this.spikeset63 = this.spikes8.getChild("spikeset63");
		this.spikeset64 = this.spikes8.getChild("spikeset64");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(35, 238)
				.addBox(-4.0F, -3.9449F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 15.9449F, -0.0073F));

		PartDefinition spikes = body.addOrReplaceChild("spikes", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0551F, 0.0073F));

		PartDefinition spikeset = spikes.addOrReplaceChild("spikeset",
				CubeListBuilder.create().texOffs(70, 221).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r1 = spikeset.addOrReplaceChild("cube_r1",
				CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset2 = spikes.addOrReplaceChild("spikeset2", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.3927F));

		PartDefinition spikeset3 = spikes.addOrReplaceChild("spikeset3",
				CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.7854F));

		PartDefinition cube_r2 = spikeset3.addOrReplaceChild("cube_r2",
				CubeListBuilder.create().texOffs(0, 34).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset4 = spikes.addOrReplaceChild("spikeset4",
				CubeListBuilder.create().texOffs(35, 0).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.1781F));

		PartDefinition cube_r3 = spikeset4.addOrReplaceChild("cube_r3",
				CubeListBuilder.create().texOffs(35, 17).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset5 = spikes.addOrReplaceChild("spikeset5",
				CubeListBuilder.create().texOffs(35, 34).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r4 = spikeset5.addOrReplaceChild("cube_r4",
				CubeListBuilder.create().texOffs(0, 51).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset6 = spikes.addOrReplaceChild("spikeset6",
				CubeListBuilder.create().texOffs(35, 51).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.9635F));

		PartDefinition cube_r5 = spikeset6.addOrReplaceChild("cube_r5",
				CubeListBuilder.create().texOffs(0, 68).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset7 = spikes.addOrReplaceChild("spikeset7",
				CubeListBuilder.create().texOffs(35, 68).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.3562F));

		PartDefinition cube_r6 = spikeset7.addOrReplaceChild("cube_r6",
				CubeListBuilder.create().texOffs(70, 0).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset8 = spikes.addOrReplaceChild("spikeset8", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.7489F));

		PartDefinition spikes2 = body.addOrReplaceChild("spikes2", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0551F, 0.0073F, 0.0F, -0.3927F, 0.0F));

		PartDefinition spikeset9 = spikes2.addOrReplaceChild("spikeset9", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.0F));

		PartDefinition spikeset10 = spikes2.addOrReplaceChild("spikeset10",
				CubeListBuilder.create().texOffs(70, 17).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.3927F));

		PartDefinition cube_r7 = spikeset10.addOrReplaceChild("cube_r7",
				CubeListBuilder.create().texOffs(70, 34).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset11 = spikes2.addOrReplaceChild("spikeset11",
				CubeListBuilder.create().texOffs(70, 51).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.7854F));

		PartDefinition cube_r8 = spikeset11.addOrReplaceChild("cube_r8",
				CubeListBuilder.create().texOffs(70, 68).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset12 = spikes2.addOrReplaceChild("spikeset12",
				CubeListBuilder.create().texOffs(0, 85).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.1781F));

		PartDefinition cube_r9 = spikeset12.addOrReplaceChild("cube_r9",
				CubeListBuilder.create().texOffs(35, 85).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset13 = spikes2.addOrReplaceChild("spikeset13",
				CubeListBuilder.create().texOffs(70, 85).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r10 = spikeset13.addOrReplaceChild("cube_r10",
				CubeListBuilder.create().texOffs(0, 102).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset14 = spikes2.addOrReplaceChild("spikeset14",
				CubeListBuilder.create().texOffs(35, 102).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.9635F));

		PartDefinition cube_r11 = spikeset14.addOrReplaceChild("cube_r11",
				CubeListBuilder.create().texOffs(70, 102).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset15 = spikes2.addOrReplaceChild("spikeset15",
				CubeListBuilder.create().texOffs(105, 0).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.3562F));

		PartDefinition cube_r12 = spikeset15.addOrReplaceChild("cube_r12",
				CubeListBuilder.create().texOffs(105, 17).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset16 = spikes2.addOrReplaceChild("spikeset16",
				CubeListBuilder.create().texOffs(105, 34).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.7489F));

		PartDefinition cube_r13 = spikeset16.addOrReplaceChild("cube_r13",
				CubeListBuilder.create().texOffs(210, 221).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikes3 = body.addOrReplaceChild("spikes3", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0551F, 0.0073F, 0.0F, -0.7854F, 0.0F));

		PartDefinition spikeset17 = spikes3.addOrReplaceChild("spikeset17", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.0F));

		PartDefinition spikeset18 = spikes3.addOrReplaceChild("spikeset18", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.3927F));

		PartDefinition spikeset19 = spikes3.addOrReplaceChild("spikeset19",
				CubeListBuilder.create().texOffs(105, 51).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.7854F));

		PartDefinition cube_r14 = spikeset19.addOrReplaceChild("cube_r14",
				CubeListBuilder.create().texOffs(105, 68).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset20 = spikes3.addOrReplaceChild("spikeset20",
				CubeListBuilder.create().texOffs(105, 85).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.1781F));

		PartDefinition cube_r15 = spikeset20.addOrReplaceChild("cube_r15",
				CubeListBuilder.create().texOffs(105, 102).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset21 = spikes3.addOrReplaceChild("spikeset21",
				CubeListBuilder.create().texOffs(0, 119).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r16 = spikeset21.addOrReplaceChild("cube_r16",
				CubeListBuilder.create().texOffs(35, 119).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset22 = spikes3.addOrReplaceChild("spikeset22",
				CubeListBuilder.create().texOffs(70, 119).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.9635F));

		PartDefinition cube_r17 = spikeset22.addOrReplaceChild("cube_r17",
				CubeListBuilder.create().texOffs(105, 119).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset23 = spikes3.addOrReplaceChild("spikeset23",
				CubeListBuilder.create().texOffs(0, 136).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.3562F));

		PartDefinition cube_r18 = spikeset23.addOrReplaceChild("cube_r18",
				CubeListBuilder.create().texOffs(35, 136).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset24 = spikes3.addOrReplaceChild("spikeset24", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.7489F));

		PartDefinition spikes4 = body.addOrReplaceChild("spikes4", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0551F, 0.0073F, 0.0F, -1.1781F, 0.0F));

		PartDefinition spikeset25 = spikes4.addOrReplaceChild("spikeset25", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.0F));

		PartDefinition spikeset26 = spikes4.addOrReplaceChild("spikeset26",
				CubeListBuilder.create().texOffs(70, 136).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.3927F));

		PartDefinition cube_r19 = spikeset26.addOrReplaceChild("cube_r19",
				CubeListBuilder.create().texOffs(105, 221).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset27 = spikes4.addOrReplaceChild("spikeset27",
				CubeListBuilder.create().texOffs(105, 136).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.7854F));

		PartDefinition cube_r20 = spikeset27.addOrReplaceChild("cube_r20",
				CubeListBuilder.create().texOffs(140, 0).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset28 = spikes4.addOrReplaceChild("spikeset28",
				CubeListBuilder.create().texOffs(140, 17).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.1781F));

		PartDefinition cube_r21 = spikeset28.addOrReplaceChild("cube_r21",
				CubeListBuilder.create().texOffs(140, 34).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset29 = spikes4.addOrReplaceChild("spikeset29",
				CubeListBuilder.create().texOffs(140, 51).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r22 = spikeset29.addOrReplaceChild("cube_r22",
				CubeListBuilder.create().texOffs(140, 68).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset30 = spikes4.addOrReplaceChild("spikeset30",
				CubeListBuilder.create().texOffs(140, 85).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.9635F));

		PartDefinition cube_r23 = spikeset30.addOrReplaceChild("cube_r23",
				CubeListBuilder.create().texOffs(140, 102).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset31 = spikes4.addOrReplaceChild("spikeset31",
				CubeListBuilder.create().texOffs(140, 119).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.3562F));

		PartDefinition cube_r24 = spikeset31.addOrReplaceChild("cube_r24",
				CubeListBuilder.create().texOffs(140, 136).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset32 = spikes4.addOrReplaceChild("spikeset32",
				CubeListBuilder.create().texOffs(0, 153).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.7489F));

		PartDefinition cube_r25 = spikeset32.addOrReplaceChild("cube_r25",
				CubeListBuilder.create().texOffs(175, 221).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikes5 = body.addOrReplaceChild("spikes5", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0551F, 0.0073F, 0.0F, -1.5708F, 0.0F));

		PartDefinition spikeset33 = spikes5.addOrReplaceChild("spikeset33",
				CubeListBuilder.create().texOffs(35, 153).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.0F));

		PartDefinition spikeset34 = spikes5.addOrReplaceChild("spikeset34", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.3927F));

		PartDefinition spikeset35 = spikes5.addOrReplaceChild("spikeset35",
				CubeListBuilder.create().texOffs(70, 153).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.7854F));

		PartDefinition cube_r26 = spikeset35.addOrReplaceChild("cube_r26",
				CubeListBuilder.create().texOffs(105, 153).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset36 = spikes5.addOrReplaceChild("spikeset36",
				CubeListBuilder.create().texOffs(140, 153).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.1781F));

		PartDefinition cube_r27 = spikeset36.addOrReplaceChild("cube_r27",
				CubeListBuilder.create().texOffs(0, 170).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset37 = spikes5.addOrReplaceChild("spikeset37",
				CubeListBuilder.create().texOffs(35, 170).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r28 = spikeset37.addOrReplaceChild("cube_r28",
				CubeListBuilder.create().texOffs(70, 170).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset38 = spikes5.addOrReplaceChild("spikeset38",
				CubeListBuilder.create().texOffs(105, 170).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.9635F));

		PartDefinition cube_r29 = spikeset38.addOrReplaceChild("cube_r29",
				CubeListBuilder.create().texOffs(140, 170).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset39 = spikes5.addOrReplaceChild("spikeset39",
				CubeListBuilder.create().texOffs(175, 0).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.3562F));

		PartDefinition cube_r30 = spikeset39.addOrReplaceChild("cube_r30",
				CubeListBuilder.create().texOffs(175, 17).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset40 = spikes5.addOrReplaceChild("spikeset40", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.7489F));

		PartDefinition spikes6 = body.addOrReplaceChild("spikes6", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0551F, 0.0073F, 0.0F, -1.9635F, 0.0F));

		PartDefinition spikeset41 = spikes6.addOrReplaceChild("spikeset41", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.0F));

		PartDefinition spikeset42 = spikes6.addOrReplaceChild("spikeset42",
				CubeListBuilder.create().texOffs(175, 34).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.3927F));

		PartDefinition cube_r31 = spikeset42.addOrReplaceChild("cube_r31",
				CubeListBuilder.create().texOffs(0, 238).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset43 = spikes6.addOrReplaceChild("spikeset43",
				CubeListBuilder.create().texOffs(175, 51).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.7854F));

		PartDefinition cube_r32 = spikeset43.addOrReplaceChild("cube_r32",
				CubeListBuilder.create().texOffs(175, 68).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset44 = spikes6.addOrReplaceChild("spikeset44",
				CubeListBuilder.create().texOffs(175, 85).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.1781F));

		PartDefinition cube_r33 = spikeset44.addOrReplaceChild("cube_r33",
				CubeListBuilder.create().texOffs(175, 102).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset45 = spikes6.addOrReplaceChild("spikeset45",
				CubeListBuilder.create().texOffs(175, 119).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r34 = spikeset45.addOrReplaceChild("cube_r34",
				CubeListBuilder.create().texOffs(175, 136).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset46 = spikes6.addOrReplaceChild("spikeset46",
				CubeListBuilder.create().texOffs(175, 153).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.9635F));

		PartDefinition cube_r35 = spikeset46.addOrReplaceChild("cube_r35",
				CubeListBuilder.create().texOffs(175, 170).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset47 = spikes6.addOrReplaceChild("spikeset47",
				CubeListBuilder.create().texOffs(0, 187).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.3562F));

		PartDefinition cube_r36 = spikeset47.addOrReplaceChild("cube_r36",
				CubeListBuilder.create().texOffs(35, 187).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset48 = spikes6.addOrReplaceChild("spikeset48",
				CubeListBuilder.create().texOffs(70, 187).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.7489F));

		PartDefinition cube_r37 = spikeset48.addOrReplaceChild("cube_r37",
				CubeListBuilder.create().texOffs(105, 187).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikes7 = body.addOrReplaceChild("spikes7", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0551F, 0.0073F, 0.0F, -2.3562F, 0.0F));

		PartDefinition spikeset49 = spikes7.addOrReplaceChild("spikeset49", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.0F));

		PartDefinition spikeset50 = spikes7.addOrReplaceChild("spikeset50", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.3927F));

		PartDefinition spikeset51 = spikes7.addOrReplaceChild("spikeset51",
				CubeListBuilder.create().texOffs(140, 187).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.7854F));

		PartDefinition cube_r38 = spikeset51.addOrReplaceChild("cube_r38",
				CubeListBuilder.create().texOffs(175, 187).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset52 = spikes7.addOrReplaceChild("spikeset52",
				CubeListBuilder.create().texOffs(0, 204).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.1781F));

		PartDefinition cube_r39 = spikeset52.addOrReplaceChild("cube_r39",
				CubeListBuilder.create().texOffs(35, 204).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset53 = spikes7.addOrReplaceChild("spikeset53",
				CubeListBuilder.create().texOffs(70, 204).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r40 = spikeset53.addOrReplaceChild("cube_r40",
				CubeListBuilder.create().texOffs(105, 204).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset54 = spikes7.addOrReplaceChild("spikeset54",
				CubeListBuilder.create().texOffs(140, 204).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.9635F));

		PartDefinition cube_r41 = spikeset54.addOrReplaceChild("cube_r41",
				CubeListBuilder.create().texOffs(175, 204).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset55 = spikes7.addOrReplaceChild("spikeset55",
				CubeListBuilder.create().texOffs(210, 0).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.3562F));

		PartDefinition cube_r42 = spikeset55.addOrReplaceChild("cube_r42",
				CubeListBuilder.create().texOffs(210, 17).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset56 = spikes7.addOrReplaceChild("spikeset56", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.7489F));

		PartDefinition spikes8 = body.addOrReplaceChild("spikes8", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0551F, 0.0073F, 0.0F, -2.7489F, 0.0F));

		PartDefinition spikeset57 = spikes8.addOrReplaceChild("spikeset57", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.0F));

		PartDefinition spikeset58 = spikes8.addOrReplaceChild("spikeset58",
				CubeListBuilder.create().texOffs(210, 34).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.3927F));

		PartDefinition cube_r43 = spikeset58.addOrReplaceChild("cube_r43",
				CubeListBuilder.create().texOffs(210, 51).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset59 = spikes8.addOrReplaceChild("spikeset59",
				CubeListBuilder.create().texOffs(210, 68).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 0.7854F));

		PartDefinition cube_r44 = spikeset59.addOrReplaceChild("cube_r44",
				CubeListBuilder.create().texOffs(210, 85).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset60 = spikes8.addOrReplaceChild("spikeset60",
				CubeListBuilder.create().texOffs(210, 102).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.1781F));

		PartDefinition cube_r45 = spikeset60.addOrReplaceChild("cube_r45",
				CubeListBuilder.create().texOffs(210, 119).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset61 = spikes8.addOrReplaceChild("spikeset61",
				CubeListBuilder.create().texOffs(210, 136).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r46 = spikeset61.addOrReplaceChild("cube_r46",
				CubeListBuilder.create().texOffs(210, 153).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset62 = spikes8.addOrReplaceChild("spikeset62",
				CubeListBuilder.create().texOffs(210, 170).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 1.9635F));

		PartDefinition cube_r47 = spikeset62.addOrReplaceChild("cube_r47",
				CubeListBuilder.create().texOffs(210, 187).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset63 = spikes8.addOrReplaceChild("spikeset63",
				CubeListBuilder.create().texOffs(210, 204).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.3562F));

		PartDefinition cube_r48 = spikeset63.addOrReplaceChild("cube_r48",
				CubeListBuilder.create().texOffs(0, 221).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition spikeset64 = spikes8.addOrReplaceChild("spikeset64",
				CubeListBuilder.create().texOffs(35, 221).addBox(-0.5F, 0.0F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0005F, -0.0073F, 1.5708F, 0.0F, 2.7489F));

		PartDefinition cube_r49 = spikeset64.addOrReplaceChild("cube_r49",
				CubeListBuilder.create().texOffs(140, 221).addBox(0.0F, 0.5F, -7.9927F, 1.0F, 0.0F, 16.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}