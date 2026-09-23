package net.mcreator.thebackwoods.procedures;
// 1.21.1
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;

import com.mojang.brigadier.context.CommandContext;

public class SwitchDimensionsProcProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, CommandContext<CommandSourceStack> arguments) {
        try {
            // 1. Get the player who executed the command
            ServerPlayer player = arguments.getSource().getPlayerOrException();
            
            // 2. Safely capture the exact dimension selection object from the Minecraft engine
            ServerLevel targetLevel = DimensionArgument.getDimension(arguments, "target_dim");

            // 3. Teleport them seamlessly
            if (targetLevel != null) {
                player.teleportTo(targetLevel, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}