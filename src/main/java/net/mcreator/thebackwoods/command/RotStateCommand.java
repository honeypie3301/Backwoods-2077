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
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;

import net.mcreator.thebackwoods.procedures.RotTotemStateToggleOnProcedure;
import net.mcreator.thebackwoods.procedures.RotTotemStateToggleOffProcedure;
import net.mcreator.thebackwoods.procedures.RotStateInfinityToggleOnProcedure;
import net.mcreator.thebackwoods.procedures.RotStateInfinityToggleOffProcedure;

@EventBusSubscriber
public class RotStateCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("rotstate").requires(s -> s.hasPermission(2)).then(Commands.argument("target", EntityArgument.entity()).then(Commands.literal("totem").then(Commands.literal("on").executes(arguments -> {
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

			RotTotemStateToggleOnProcedure.execute(arguments, entity);
			return 0;
		})).then(Commands.literal("off").executes(arguments -> {
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

			RotTotemStateToggleOffProcedure.execute(arguments, entity);
			return 0;
		}))).then(Commands.literal("infinitytotem").then(Commands.literal("on").executes(arguments -> {
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

			RotStateInfinityToggleOnProcedure.execute(arguments, entity);
			return 0;
		})).then(Commands.literal("off").executes(arguments -> {
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

			RotStateInfinityToggleOffProcedure.execute(arguments, entity);
			return 0;
		})))));
	}

}