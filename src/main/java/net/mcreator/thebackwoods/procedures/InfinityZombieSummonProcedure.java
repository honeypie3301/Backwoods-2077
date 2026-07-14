package net.mcreator.thebackwoods.procedures;

import net.neoforged.fml.ModList;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.DoubleArgumentType;

public class InfinityZombieSummonProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, CommandContext<CommandSourceStack> arguments, Entity entity) {
		if (entity == null)
			return;
		if (ModList.get().isLoaded("avaritia")) {
			for (int index0 = 0; index0 < (int) DoubleArgumentType.getDouble(arguments, "count"); index0++) {
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"summon zombie ~ ~ ~ {invulnerable_to:[fall],ArmorItems:[{id:\"avaritia:infinity_boots\",count:1},{id:\"avaritia:infinity_pants\",count:1},{id:\"avaritia:infinity_chestplate\",count:1},{id:\"avaritia:infinity_helmet\",count:1}],HandItems:[{id:\"avaritia:crystal_sword\",count:1},{}],ArmorDropChances:[0.0f,0.0f,0.0f,0.0f],HandDropChances:[0.0f,0.0f]}");
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("You need Re-Avaritia."), true);
		}
	}
}