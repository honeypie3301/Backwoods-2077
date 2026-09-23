package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.entity.RotEntity;
import net.mcreator.thebackwoods.entity.PetrifiedLignumTrilobitaEntity;

import java.util.Comparator;

public class PetrifiedLignumTrilobitaOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		Entity targetItem = null;
		if (entity.isAlive()) {
			if (!world.getEntitiesOfClass(RotEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3((entity.getX()), (entity.getY()), (entity.getZ()))).inflate(200 / 2d), e -> true).isEmpty()) {
				if (entity instanceof PetrifiedLignumTrilobitaEntity _datEntSetL)
					_datEntSetL.getEntityData().set(PetrifiedLignumTrilobitaEntity.DATA_isHiding, false);
			} else if (entity.level().getGameTime() % 10 == 0) {
				if ((entity instanceof PetrifiedLignumTrilobitaEntity _datEntL6 && _datEntL6.getEntityData().get(PetrifiedLignumTrilobitaEntity.DATA_isHiding)) == true) {
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + 1);
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) >= 10) {
						if (entity instanceof PetrifiedLignumTrilobitaEntity _datEntSetL)
							_datEntSetL.getEntityData().set(PetrifiedLignumTrilobitaEntity.DATA_isHiding, false);
					}
				} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) <= 4) {
					if (entity instanceof PetrifiedLignumTrilobitaEntity _datEntSetL)
						_datEntSetL.getEntityData().set(PetrifiedLignumTrilobitaEntity.DATA_isHiding, true);
				}
				if ((entity instanceof PetrifiedLignumTrilobitaEntity _datEntI ? _datEntI.getEntityData().get(PetrifiedLignumTrilobitaEntity.DATA_eatTimer) : 0) > 0) {
					if (entity instanceof PetrifiedLignumTrilobitaEntity _datEntSetI)
						_datEntSetI.getEntityData().set(PetrifiedLignumTrilobitaEntity.DATA_eatTimer,
								(int) ((entity instanceof PetrifiedLignumTrilobitaEntity _datEntI ? _datEntI.getEntityData().get(PetrifiedLignumTrilobitaEntity.DATA_eatTimer) : 0) - 1));
					if ((entity instanceof PetrifiedLignumTrilobitaEntity _datEntI ? _datEntI.getEntityData().get(PetrifiedLignumTrilobitaEntity.DATA_eatTimer) : 0) == 1) {
						if (entity instanceof LivingEntity _livingEntity18 && _livingEntity18.getAttributes().hasAttribute(Attributes.SCALE))
							_livingEntity18.getAttribute(Attributes.SCALE)
									.setBaseValue(((entity instanceof LivingEntity _livingEntity17 && _livingEntity17.getAttributes().hasAttribute(Attributes.SCALE) ? _livingEntity17.getAttribute(Attributes.SCALE).getValue() : 0) + 0.07));
					}
				} else if (!world.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3((entity.getX()), (entity.getY()), (entity.getZ()))).inflate(12 / 2d), e -> true).isEmpty()) {
					targetItem = findEntityInWorldRange(world, ItemEntity.class, x, y, z, 12);
					if (targetItem.isAlive() && (targetItem instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).is(ItemTags.create(ResourceLocation.parse("the_backwoods:trilobita_edible")))) {
						if (entity instanceof Mob _entity)
							_entity.getNavigation().moveTo((targetItem.getX()), (targetItem.getY()), (targetItem.getZ()), 1.8);
						if ((targetItem.position()).distanceTo((entity.position())) <= 1.8) {
							if (!targetItem.level().isClientSide())
								targetItem.discard();
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.eat")), SoundSource.HOSTILE, (float) 0.6,
											(float) 1.1);
								} else {
									_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.eat")), SoundSource.HOSTILE, (float) 0.6, (float) 1.1, false);
								}
							}
							if (entity instanceof PetrifiedLignumTrilobitaEntity _datEntSetI)
								_datEntSetI.getEntityData().set(PetrifiedLignumTrilobitaEntity.DATA_eatTimer, 10);
						}
					}
				}
			}
		}
	}

	private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range) {
		return (Entity) world.getEntitiesOfClass(clazz, AABB.ofSize(new Vec3(x, y, z), range, range, range), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z))).findFirst().orElse(null);
	}
}