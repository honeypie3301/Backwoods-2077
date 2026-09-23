package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import net.mcreator.thebackwoods.TheBackwoodsMod;

public class BackwoodsPlayerEntersDimensionProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:backwoods"))) {
			TheBackwoodsMod.queueServerWork(100, () -> {
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL, new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"fill ~-15 ~-15 ~-15 ~15 ~15 ~15 air replace the_backwoods:backwoods_portal");
			});
		}
		AntiObsidianProcedure.execute(world, entity);
	}
}