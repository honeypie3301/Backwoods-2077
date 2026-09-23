package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import net.mcreator.thebackwoods.procedures.HewnSplinterOnInitialEntitySpawnProcedure;
import net.mcreator.thebackwoods.procedures.HewnSplinterNaturalEntitySpawningConditionProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import javax.annotation.Nullable;

public class HewnSplinterEntity extends Monster {
	public HewnSplinterEntity(EntityType<HewnSplinterEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
		setPersistenceRequired();
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();

	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHitIn) {
		super.dropCustomDeathLoot(serverLevel, source, recentlyHitIn);
		this.spawnAtLocation(new ItemStack(Blocks.OAK_PLANKS));
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.hit"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.break"));
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		if (damagesource.getDirectEntity() instanceof AbstractArrow)
			return false;
		if (damagesource.is(DamageTypes.FALL))
			return false;
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		if (damagesource.is(DamageTypes.FALLING_ANVIL))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata);
		HewnSplinterOnInitialEntitySpawnProcedure.execute(this);
		return retval;
	}

	@Override
	public boolean isPushedByFluid() {
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		Level world = this.level();
		Entity entity = this;
		return false;
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return true;
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
		event.register(TheBackwoodsModEntities.HEWN_SPLINTER.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return HewnSplinterNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0);
		builder = builder.add(Attributes.MAX_HEALTH, 6);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 0);
		builder = builder.add(Attributes.FOLLOW_RANGE, 0);
		builder = builder.add(Attributes.STEP_HEIGHT, 0);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1);
		return builder;
	}
}