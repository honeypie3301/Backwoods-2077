package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

public class FractusEntityIsHurtProcedure {
	public static void execute(Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity.isAlive()) {
			if (entity instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
				_entity.setTarget(_ent);
			if (!sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities")))) {
				entity.getPersistentData().putBoolean("is_angered_burst", true);
			}
		}
	}
}