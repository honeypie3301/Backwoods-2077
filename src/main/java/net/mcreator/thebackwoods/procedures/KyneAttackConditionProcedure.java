package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

public class KyneAttackConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) != null && ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) instanceof Player
				|| !((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities")))));
	}
}