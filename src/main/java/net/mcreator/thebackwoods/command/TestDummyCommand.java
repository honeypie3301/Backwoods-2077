package net.mcreator.thebackwoods.command;

import org.checkerframework.checker.units.qual.s;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.commands.Commands;

import net.mcreator.thebackwoods.procedures.InfinityZombieSummonProcedure;
import net.mcreator.thebackwoods.procedures.GemZombieSummonProcedure;

import com.mojang.brigadier.arguments.DoubleArgumentType;

@EventBusSubscriber
public class TestDummyCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("testdummy").requires(s -> s.hasPermission(2)).then(Commands.literal("gem_zombie").then(Commands.argument("count", DoubleArgumentType.doubleArg()).executes(arguments -> {
			Level world = arguments.getSource().getUnsidedLevel();
			double x = arguments.getSource().getPosition().x();
			double y = arguments.getSource().getPosition().y();
			double z = arguments.getSource().getPosition().z();
			Entity entity = arguments.getSource().getEntity();
			if (entity == null && world instanceof ServerLevel _servLevel)
				entity = FakePlayerFactory.getMinecraft(_servLevel);
			Direction direction = Direction.DOWN;
			if (entity != null)
				direction = entity.getDirection();

			GemZombieSummonProcedure.execute(world, x, y, z, arguments, entity);
			return 0;
		}))).then(Commands.literal("infinity").then(Commands.argument("count", DoubleArgumentType.doubleArg()).executes(arguments -> {
			Level world = arguments.getSource().getUnsidedLevel();
			double x = arguments.getSource().getPosition().x();
			double y = arguments.getSource().getPosition().y();
			double z = arguments.getSource().getPosition().z();
			Entity entity = arguments.getSource().getEntity();
			if (entity == null && world instanceof ServerLevel _servLevel)
				entity = FakePlayerFactory.getMinecraft(_servLevel);
			Direction direction = Direction.DOWN;
			if (entity != null)
				direction = entity.getDirection();

			InfinityZombieSummonProcedure.execute(world, x, y, z, arguments, entity);
			return 0;
		}))));
	}

}