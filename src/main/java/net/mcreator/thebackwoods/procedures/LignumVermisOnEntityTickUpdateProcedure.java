package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;

import net.mcreator.thebackwoods.TheBackwoodsMod;

import java.util.Comparator;

public class LignumVermisOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (IsRotNearbyConditionProcedure.execute(world, entity)) {
			if (entity instanceof Mob _mob) {
				_mob.setTarget(null);
				_mob.setLastHurtByMob(null);
			}
			return;
		}
		net.minecraft.world.level.block.state.BlockState blockBelow = world.getBlockState(BlockPos.containing(entity.getX(), entity.getY() - 0.1, entity.getZ()));
		if (blockBelow.isAir()) {
			blockBelow = world.getBlockState(BlockPos.containing(entity.getX(), entity.getY() - 1, entity.getZ()));
		}
		String blockName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockBelow.getBlock()).getPath();
		String camo = "default";
		boolean isLog = blockName.contains("log") || blockName.contains("wood") || blockName.contains("stem") || blockName.contains("hyphae");
		boolean isPlanks = blockName.contains("planks") || blockName.contains("bamboo");

		if (isLog) {
			if (blockName.contains("dark_oak")) {
				camo = "dark_oak_log";
			} else if (blockName.contains("oak")) {
				camo = "oak_log";
			} else if (blockName.contains("cherry")) {
				camo = "cherry_log";
			} else if (blockName.contains("acacia")) {
				camo = "acacia_log";
			} else if (blockName.contains("jungle")) {
				camo = "jungle_log";
			} else if (blockName.contains("spruce")) {
				camo = "spruce_log";
			} else if (blockName.contains("birch")) {
				camo = "birch_log";
			} else if (blockName.contains("mangrove")) {
				camo = "mangrove_log";
			}
		} else if (isPlanks) {
			if (blockName.contains("acacia")) {
				camo = "acacia_planks";
			} else if (blockName.contains("bamboo")) {
				camo = "bamboo_planks";
			} else if (blockName.contains("birch")) {
				camo = "birch_planks";
			} else if (blockName.contains("cherry")) {
				camo = "cherry_planks";
			} else if (blockName.contains("crimson")) {
				camo = "crimson_planks";
			} else if (blockName.contains("jungle")) {
				camo = "jungle_planks";
			} else if (blockName.contains("mangrove")) {
				camo = "mangrove_planks";
			} else if (blockName.contains("spruce")) {
				camo = "spruce_planks";
			} else if (blockName.contains("warped")) {
				camo = "warped_planks";
			} else if (blockName.contains("dark_oak")) {
				camo = "dark_oak_planks";
			} else if (blockName.contains("oak")) {
				camo = "oak_planks";
			}
		}
		entity.getPersistentData().putString("camoType", camo);

		Entity foundPlayer = null;
		boolean found = false;
		boolean foundPlank = false;
		double sx = 0;
		double sy = 0;
		double sz = 0;
		double targetX = 0;
		double targetY = 0;
		double targetZ = 0;
		if ((world.getBlockState(BlockPos.containing(entity.getX() + sx, entity.getY() + sy, entity.getZ() + sz))).is(BlockTags.create(ResourceLocation.parse("minecraft:replaceable")))) {
			world.destroyBlock(BlockPos.containing(entity.getX() + sx, entity.getY() + sy, entity.getZ() + sz), false);
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) <= 4) {
			found = false;
			double searchTicks = entity.getPersistentData().getDouble("vermisSearchTicks");
			if (searchTicks > 0) {
				entity.getPersistentData().putDouble("vermisSearchTicks", searchTicks - 1);
				if (entity.getPersistentData().getBoolean("vermisHideFound")) {
					found = true;
					targetX = entity.getPersistentData().getDouble("vermisHideX");
					targetY = entity.getPersistentData().getDouble("vermisHideY");
					targetZ = entity.getPersistentData().getDouble("vermisHideZ");
				}
			} else {
				sx = -16;
				search: for (int index0 = 0; index0 < 32; index0++) {
					sy = -16;
					for (int index1 = 0; index1 < 32; index1++) {
						sz = -16;
						for (int index2 = 0; index2 < 32; index2++) {
							if ((world.getBlockState(BlockPos.containing(entity.getX() + sx, entity.getY() + sy, entity.getZ() + sz))).is(BlockTags.create(ResourceLocation.parse("minecraft:vermis_hide")))) {
								found = true;
								targetX = entity.getX() + sx;
								targetY = entity.getY() + sy;
								targetZ = entity.getZ() + sz;
								entity.getPersistentData().putDouble("vermisHideX", targetX);
								entity.getPersistentData().putDouble("vermisHideY", targetY);
								entity.getPersistentData().putDouble("vermisHideZ", targetZ);
								entity.getPersistentData().putBoolean("vermisHideFound", true);
								break search;
							}
							sz = sz + 1;
						}
						sy = sy + 1;
					}
					sx = sx + 1;
				}
				if (!found) {
					entity.getPersistentData().putBoolean("vermisHideFound", false);
				}
				entity.getPersistentData().putDouble("vermisSearchTicks", 20);
			}
			if (entity instanceof LivingEntity _entity) {
				_entity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(ResourceLocation.parse("the_backwoods:freeze"));
			}
			if ((world.getBlockState(BlockPos.containing(entity.getX(), entity.getY() - 1, entity.getZ()))).is(BlockTags.create(ResourceLocation.parse("minecraft:vermis_hide")))
					|| (world.getBlockState(BlockPos.containing(entity.getX(), entity.getY() + 1, entity.getZ()))).is(BlockTags.create(ResourceLocation.parse("minecraft:vermis_hide")))
					|| (world.getBlockState(BlockPos.containing(entity.getX() + 1, entity.getY(), entity.getZ()))).is(BlockTags.create(ResourceLocation.parse("minecraft:vermis_hide")))
					|| (world.getBlockState(BlockPos.containing(entity.getX() - 1, entity.getY(), entity.getZ()))).is(BlockTags.create(ResourceLocation.parse("minecraft:vermis_hide")))
					|| (world.getBlockState(BlockPos.containing(entity.getX(), entity.getY(), entity.getZ() + 1))).is(BlockTags.create(ResourceLocation.parse("minecraft:vermis_hide")))
					|| (world.getBlockState(BlockPos.containing(entity.getX(), entity.getY(), entity.getZ() - 1))).is(BlockTags.create(ResourceLocation.parse("minecraft:vermis_hide")))
					|| found == true && (entity.position()).distanceTo((new Vec3(targetX, (entity.getY()), targetZ))) < 1.5) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SMOKE, x, y, z, 20, 0.3, 0.4, 0.3, 0.1);
				if (!entity.level().isClientSide())
					entity.discard();
			} else {
				if (found == true) {
					if (entity instanceof Mob _entity)
						_entity.getNavigation().moveTo(targetX, targetY, targetZ, 0.8);
				} else {
					foundPlayer = findEntityInWorldRange(world, Player.class, x, y, z, 16);
					if (foundPlayer != null) {
						if (entity instanceof Mob _entity)
							_entity.getNavigation().moveTo((entity.getX() + entity.getX() - foundPlayer.getX()), (entity.getY()), (entity.getZ() + entity.getZ() - foundPlayer.getZ()), 0.8);
					}
				}
			}
		} else {
			foundPlayer = findEntityInWorldRange(world, Player.class, x, y, z, 16);
			if (foundPlayer != null) {
				if (!(foundPlayer instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
					if (entity.getPersistentData().getDouble("isEnraged") == 0) {
						if ((entity instanceof net.minecraft.world.entity.LivingEntity _livEnt) ? !_livEnt.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, _livEnt.getBoundingBox().inflate(16.0)).stream().filter(_player -> {
							net.minecraft.world.phys.Vec3 toMob = _livEnt.getEyePosition().subtract(_player.getEyePosition());
							if (toMob.lengthSqr() > 1.0e-8)
								toMob = toMob.normalize();
							return (_player.getLookAngle().normalize().dot(toMob) > 0.5) && _player.hasLineOfSight(_livEnt);
						}).collect(java.util.stream.Collectors.toList()).isEmpty() : false) {
							if (entity instanceof Mob _entity)
								_entity.getNavigation().stop();
							if (entity instanceof LivingEntity _entity) {
								AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:freeze"), (-1), AttributeModifier.Operation.ADD_VALUE);
								if (!_entity.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(modifier.id())) {
									_entity.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(modifier);
								}
							}
						} else if (world.getLevelData().isRaining() && world.canSeeSkyFromBelowWater(BlockPos.containing(x, y, z))) {
							if (entity instanceof LivingEntity _entity) {
								AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:freeze"), (-1), AttributeModifier.Operation.ADD_VALUE);
								if (!_entity.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(modifier.id())) {
									_entity.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(modifier);
								}
							}
							if (entity instanceof Mob _entity)
								_entity.getNavigation().stop();
						} else {
							if (entity instanceof LivingEntity _entity) {
								_entity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(ResourceLocation.parse("the_backwoods:freeze"));
							}
							entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((foundPlayer.getX()), (foundPlayer.getY() + 1.2), (foundPlayer.getZ())));
							if (entity instanceof Mob _entity)
								_entity.getNavigation().moveTo((foundPlayer.getX()), (foundPlayer.getY()), (foundPlayer.getZ()), 0.6);
						}
					} else {
						if (Math.random() < 0.5) {
							TheBackwoodsMod.queueServerWork(40, () -> {
								if (entity instanceof LivingEntity _entity) {
									_entity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(ResourceLocation.parse("the_backwoods:freeze"));
								}
							});
						} else {
							if (entity instanceof LivingEntity _entity) {
								_entity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(ResourceLocation.parse("the_backwoods:freeze"));
							}
						}
						entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((foundPlayer.getX()), (foundPlayer.getY() + 1.2), (foundPlayer.getZ())));
						if (entity instanceof Mob _entity)
							_entity.getNavigation().moveTo((foundPlayer.getX()), (foundPlayer.getY()), (foundPlayer.getZ()), 0.6);
					}
				}
			}
		}
	}

	private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range) {
		return (Entity) world.getEntitiesOfClass(clazz, AABB.ofSize(new Vec3(x, y, z), range, range, range), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z))).findFirst().orElse(null);
	}
} // 1.21.1