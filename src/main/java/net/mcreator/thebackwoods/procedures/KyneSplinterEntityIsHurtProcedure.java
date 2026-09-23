package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Difficulty;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.TheBackwoodsMod;

import java.util.Comparator;

public class KyneSplinterEntityIsHurtProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double scan_radius = 0;
		double pack_count = 0;
		if (entity instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
			_entity.setTarget(_ent);
		if (entity.getPersistentData().getDouble("avenge_cooldown") <= 0) {
			if (world.getDifficulty() == Difficulty.HARD) {
				scan_radius = (double) (world instanceof net.minecraft.server.level.ServerLevel _slv ? _slv.getServer().getPlayerList().getViewDistance() * 16 : 64);
			} else if (world.getDifficulty() == Difficulty.NORMAL) {
				scan_radius = 80;
			} else {
				scan_radius = 48;
			}
			pack_count = 0;
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(scan_radius / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities")))) {
						if (!(entityiterator == entity)) {
							pack_count = pack_count + 1;
						}
					}
				}
			}
			if (pack_count > 0) {
				entity.getPersistentData().putDouble("avenge_cooldown", 200);
				if (entity instanceof LivingEntity _entity) {
					AttributeModifier modifier = new AttributeModifier(ResourceLocation.parse("the_backwoods:wait"), (-1), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
					if (!_entity.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(modifier.id())) {
						_entity.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(modifier);
					}
				}
				TheBackwoodsMod.queueServerWork(Mth.nextInt(RandomSource.create(), 40, 80), () -> {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY() + 1, entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("particle.soul_escape")), SoundSource.HOSTILE, 45, (float) 0.4);
						} else {
							_level.playLocalSound((entity.getX()), (entity.getY() + 1), (entity.getZ()), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("particle.soul_escape")), SoundSource.HOSTILE, 45, (float) 0.4, false);
						}
					}
					if (entity instanceof LivingEntity _entity) {
						_entity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(ResourceLocation.parse("the_backwoods:wait"));
					}
					SplinterAvengeOnHurtProcedure.execute(world, x, y, z, entity, sourceentity);
				});
			}
		}
	}
}