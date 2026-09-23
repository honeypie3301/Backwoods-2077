package net.mcreator.thebackwoods.command;
// 1.21.1
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;

import net.mcreator.thebackwoods.procedures.SwitchDimensionsProcProcedure;

@EventBusSubscriber
public class SwitchDimensionsCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("switchdimensions")
			.requires(s -> s.hasPermission(4))
			.then(Commands.argument("target_dim", DimensionArgument.dimension())
				.executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();

					// Forward execution to your locked procedure file
					SwitchDimensionsProcProcedure.execute(world, x, y, z, arguments);
					return 0;
				})
			)
		);
	}
}