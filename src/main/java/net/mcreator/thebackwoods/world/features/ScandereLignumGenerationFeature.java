package net.mcreator.thebackwoods.world.features;

import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.WorldGenLevel;

import net.mcreator.thebackwoods.procedures.ScandereLignumGenerationAdditionalGenerationConditionProcedure;

public class ScandereLignumGenerationFeature extends TreeFeature {
	public ScandereLignumGenerationFeature() {
		super(TreeConfiguration.CODEC);
	}

	public boolean place(FeaturePlaceContext<TreeConfiguration> context) {
		WorldGenLevel world = context.level();
		int x = context.origin().getX();
		int y = context.origin().getY();
		int z = context.origin().getZ();
		if (!ScandereLignumGenerationAdditionalGenerationConditionProcedure.execute())
			return false;
		return super.place(context);
	}
}