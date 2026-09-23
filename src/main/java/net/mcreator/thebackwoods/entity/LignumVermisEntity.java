package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.*;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import javax.annotation.Nullable;

public class LignumVermisEntity extends Monster {
	public LignumVermisEntity(EntityType<LignumVermisEntity> type, Level world) {
		super(type, world);
		xpReward = 2;
		setNoAi(false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.6, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity entity) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(entity) < (this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth()) && this.mob.getSensing().hasLineOfSight(entity);
			}
		});
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true, false));
		this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, RotEntity.class, (float) 100, 0.7, 0.9) {
			@Override
			public boolean canUse() {
				double x = LignumVermisEntity.this.getX();
				double y = LignumVermisEntity.this.getY();
				double z = LignumVermisEntity.this.getZ();
				Entity entity = LignumVermisEntity.this;
				Level world = LignumVermisEntity.this.level();
				return super.canUse() && IsRotNearbyConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = LignumVermisEntity.this.getX();
				double y = LignumVermisEntity.this.getY();
				double z = LignumVermisEntity.this.getZ();
				Entity entity = LignumVermisEntity.this;
				Level world = LignumVermisEntity.this.level();
				return super.canContinueToUse() && IsRotNearbyConditionProcedure.execute(world, entity);
			}
		});
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.4));
		this.targetSelector.addGoal(5, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(7, new FloatGoal(this));
	}

	@Override
	public SoundEvent getAmbientSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:vermis_idle"));
	}

	@Override
	public void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:vermis_step")), 0.15f, 1);
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:vermis_hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:vermis_death"));
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		Level world = this.level();
		Entity entity = this;
		Entity sourceentity = damagesource.getEntity();
		Entity immediatesourceentity = damagesource.getDirectEntity();

		LignumVermisEntityIsHurtProcedure.execute(entity, sourceentity);
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata);
		LignumVermisOnInitialEntitySpawnProcedure.execute(this);
		return retval;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		LignumVermisOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
	}

	@Override
	public void playerTouch(Player sourceentity) {
		super.playerTouch(sourceentity);
		LignumVermisPlayerCollidesWithThisEntityProcedure.execute(this.level(), this, sourceentity);
	}

	@Override
	public boolean canDrownInFluidType(FluidType type) {
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		Level world = this.level();
		Entity entity = this;
		return false;
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
		event.register(TheBackwoodsModEntities.LIGNUM_VERMIS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return LignumVermisNaturalEntitySpawningConditionProcedure.execute(y);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.5);
		builder = builder.add(Attributes.MAX_HEALTH, 10);
		builder = builder.add(Attributes.ARMOR, 0.5);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 2);
		builder = builder.add(Attributes.FOLLOW_RANGE, 32);
		builder = builder.add(Attributes.STEP_HEIGHT, 1);
		return builder;
	}
}