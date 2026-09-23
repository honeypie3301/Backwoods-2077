package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

public class RotStateInfinityToggleOffProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments, Entity entity) {
		if (entity == null)
			return;
		(commandParameterEntity(arguments, "target")).getPersistentData().putBoolean("sentinel_is_infinity_totem", false);
		if (entity instanceof Player _player && !_player.level().isClientSide())
			_player.displayClientMessage(Component.literal("Rot infinity state is now ACTIVE!"), true);
	}

	private static Entity commandParameterEntity(CommandContext<CommandSourceStack> arguments, String parameter) {
		try {
			return EntityArgument.getEntity(arguments, parameter);
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}