package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.*;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import javax.annotation.Nullable;

public class WoodweaverEntity extends Monster {
	public static final EntityDataAccessor<Boolean> DATA_isLeaping = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isLanding = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isOrbLooping = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isHypnotizing = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isAttackingLeft = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isAttackingRight = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isClosing3s = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isClosing1_5s = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isClosing1s = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isAttackingBoth = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isHookingLeft = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isHookingRight = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isSweepingRight = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isSweepingLeft = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isHypnotizing2 = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isHypnotizing3 = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isWeaponClose = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isRetreating = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isRotatingLeft = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isRotatingRight = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isDead = SynchedEntityData.defineId(WoodweaverEntity.class, EntityDataSerializers.BOOLEAN);
	public final AnimationState animationState1 = new AnimationState();
	public final AnimationState animationState2 = new AnimationState();
	public final AnimationState animationState3 = new AnimationState();
	public final AnimationState animationState4 = new AnimationState();
	public final AnimationState animationState5 = new AnimationState();
	public final AnimationState animationState6 = new AnimationState();
	public final AnimationState animationState7 = new AnimationState();
	public final AnimationState animationState8 = new AnimationState();
	public final AnimationState animationState9 = new AnimationState();
	public final AnimationState animationState10 = new AnimationState();
	public final AnimationState animationState11 = new AnimationState();
	public final AnimationState animationState12 = new AnimationState();
	public final AnimationState animationState13 = new AnimationState();
	public final AnimationState animationState14 = new AnimationState();
	public final AnimationState animationState15 = new AnimationState();
	public final AnimationState animationState16 = new AnimationState();
	public final AnimationState animationState17 = new AnimationState();
	public final AnimationState animationState18 = new AnimationState();
	public final AnimationState animationState19 = new AnimationState();
	public final AnimationState animationState21 = new AnimationState();

	public WoodweaverEntity(EntityType<WoodweaverEntity> type, Level world) {
		super(type, world);
		xpReward = 700;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_isLeaping, false);
		builder.define(DATA_isLanding, false);
		builder.define(DATA_isOrbLooping, false);
		builder.define(DATA_isHypnotizing, false);
		builder.define(DATA_isAttackingLeft, false);
		builder.define(DATA_isAttackingRight, false);
		builder.define(DATA_isClosing3s, false);
		builder.define(DATA_isClosing1_5s, false);
		builder.define(DATA_isClosing1s, false);
		builder.define(DATA_isAttackingBoth, false);
		builder.define(DATA_isHookingLeft, false);
		builder.define(DATA_isHookingRight, false);
		builder.define(DATA_isSweepingRight, false);
		builder.define(DATA_isSweepingLeft, false);
		builder.define(DATA_isHypnotizing2, false);
		builder.define(DATA_isHypnotizing3, false);
		builder.define(DATA_isWeaponClose, false);
		builder.define(DATA_isRetreating, false);
		builder.define(DATA_isRotatingLeft, false);
		builder.define(DATA_isRotatingRight, false);
		builder.define(DATA_isDead, false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
			@Override
			protected boolean canPerformAttack(LivingEntity entity) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(entity) < (this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth()) && this.mob.getSensing().hasLineOfSight(entity);
			}

			@Override
			public boolean canUse() {
				double x = WoodweaverEntity.this.getX();
				double y = WoodweaverEntity.this.getY();
				double z = WoodweaverEntity.this.getZ();
				Entity entity = WoodweaverEntity.this;
				Level world = WoodweaverEntity.this.level();
				return super.canUse() && WoodweaverCanGoalRunProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = WoodweaverEntity.this.getX();
				double y = WoodweaverEntity.this.getY();
				double z = WoodweaverEntity.this.getZ();
				Entity entity = WoodweaverEntity.this;
				Level world = WoodweaverEntity.this.level();
				return super.canContinueToUse() && WoodweaverCanGoalRunProcedure.execute(entity);
			}

		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1) {
			@Override
			public boolean canUse() {
				double x = WoodweaverEntity.this.getX();
				double y = WoodweaverEntity.this.getY();
				double z = WoodweaverEntity.this.getZ();
				Entity entity = WoodweaverEntity.this;
				Level world = WoodweaverEntity.this.level();
				return super.canUse() && WoodweaverCanGoalRunProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = WoodweaverEntity.this.getX();
				double y = WoodweaverEntity.this.getY();
				double z = WoodweaverEntity.this.getZ();
				Entity entity = WoodweaverEntity.this;
				Level world = WoodweaverEntity.this.level();
				return super.canContinueToUse() && WoodweaverCanGoalRunProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				double x = WoodweaverEntity.this.getX();
				double y = WoodweaverEntity.this.getY();
				double z = WoodweaverEntity.this.getZ();
				Entity entity = WoodweaverEntity.this;
				Level world = WoodweaverEntity.this.level();
				return super.canUse() && WoodweaverCanGoalRunProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = WoodweaverEntity.this.getX();
				double y = WoodweaverEntity.this.getY();
				double z = WoodweaverEntity.this.getZ();
				Entity entity = WoodweaverEntity.this;
				Level world = WoodweaverEntity.this.level();
				return super.canContinueToUse() && WoodweaverCanGoalRunProcedure.execute(entity);
			}
		});
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	@Override
	public SoundEvent getAmbientSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_step"));
	}

	@Override
	public void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_step")), 0.15f, 1);
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_hurt"));
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

		WoodweaverEntityIsHurtProcedure.execute(world, x, z, entity, sourceentity);
		if (damagesource.is(DamageTypes.IN_FIRE))
			return false;
		if (damagesource.is(DamageTypes.FALL))
			return false;
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		if (damagesource.is(DamageTypes.DRAGON_BREATH))
			return false;
		if (damagesource.is(DamageTypes.WITHER) || damagesource.is(DamageTypes.WITHER_SKULL))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		WoodweaverEntityDiesProcedure.execute(this);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata);
		WoodweaverOnInitialEntitySpawnProcedure.execute(this);
		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putBoolean("DataisLeaping", this.entityData.get(DATA_isLeaping));
		compound.putBoolean("DataisLanding", this.entityData.get(DATA_isLanding));
		compound.putBoolean("DataisOrbLooping", this.entityData.get(DATA_isOrbLooping));
		compound.putBoolean("DataisHypnotizing", this.entityData.get(DATA_isHypnotizing));
		compound.putBoolean("DataisAttackingLeft", this.entityData.get(DATA_isAttackingLeft));
		compound.putBoolean("DataisAttackingRight", this.entityData.get(DATA_isAttackingRight));
		compound.putBoolean("DataisClosing3s", this.entityData.get(DATA_isClosing3s));
		compound.putBoolean("DataisClosing1_5s", this.entityData.get(DATA_isClosing1_5s));
		compound.putBoolean("DataisClosing1s", this.entityData.get(DATA_isClosing1s));
		compound.putBoolean("DataisAttackingBoth", this.entityData.get(DATA_isAttackingBoth));
		compound.putBoolean("DataisHookingLeft", this.entityData.get(DATA_isHookingLeft));
		compound.putBoolean("DataisHookingRight", this.entityData.get(DATA_isHookingRight));
		compound.putBoolean("DataisSweepingRight", this.entityData.get(DATA_isSweepingRight));
		compound.putBoolean("DataisSweepingLeft", this.entityData.get(DATA_isSweepingLeft));
		compound.putBoolean("DataisHypnotizing2", this.entityData.get(DATA_isHypnotizing2));
		compound.putBoolean("DataisHypnotizing3", this.entityData.get(DATA_isHypnotizing3));
		compound.putBoolean("DataisWeaponClose", this.entityData.get(DATA_isWeaponClose));
		compound.putBoolean("DataisRetreating", this.entityData.get(DATA_isRetreating));
		compound.putBoolean("DataisRotatingLeft", this.entityData.get(DATA_isRotatingLeft));
		compound.putBoolean("DataisRotatingRight", this.entityData.get(DATA_isRotatingRight));
		compound.putBoolean("DataisDead", this.entityData.get(DATA_isDead));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DataisLeaping"))
			this.entityData.set(DATA_isLeaping, compound.getBoolean("DataisLeaping"));
		if (compound.contains("DataisLanding"))
			this.entityData.set(DATA_isLanding, compound.getBoolean("DataisLanding"));
		if (compound.contains("DataisOrbLooping"))
			this.entityData.set(DATA_isOrbLooping, compound.getBoolean("DataisOrbLooping"));
		if (compound.contains("DataisHypnotizing"))
			this.entityData.set(DATA_isHypnotizing, compound.getBoolean("DataisHypnotizing"));
		if (compound.contains("DataisAttackingLeft"))
			this.entityData.set(DATA_isAttackingLeft, compound.getBoolean("DataisAttackingLeft"));
		if (compound.contains("DataisAttackingRight"))
			this.entityData.set(DATA_isAttackingRight, compound.getBoolean("DataisAttackingRight"));
		if (compound.contains("DataisClosing3s"))
			this.entityData.set(DATA_isClosing3s, compound.getBoolean("DataisClosing3s"));
		if (compound.contains("DataisClosing1_5s"))
			this.entityData.set(DATA_isClosing1_5s, compound.getBoolean("DataisClosing1_5s"));
		if (compound.contains("DataisClosing1s"))
			this.entityData.set(DATA_isClosing1s, compound.getBoolean("DataisClosing1s"));
		if (compound.contains("DataisAttackingBoth"))
			this.entityData.set(DATA_isAttackingBoth, compound.getBoolean("DataisAttackingBoth"));
		if (compound.contains("DataisHookingLeft"))
			this.entityData.set(DATA_isHookingLeft, compound.getBoolean("DataisHookingLeft"));
		if (compound.contains("DataisHookingRight"))
			this.entityData.set(DATA_isHookingRight, compound.getBoolean("DataisHookingRight"));
		if (compound.contains("DataisSweepingRight"))
			this.entityData.set(DATA_isSweepingRight, compound.getBoolean("DataisSweepingRight"));
		if (compound.contains("DataisSweepingLeft"))
			this.entityData.set(DATA_isSweepingLeft, compound.getBoolean("DataisSweepingLeft"));
		if (compound.contains("DataisHypnotizing2"))
			this.entityData.set(DATA_isHypnotizing2, compound.getBoolean("DataisHypnotizing2"));
		if (compound.contains("DataisHypnotizing3"))
			this.entityData.set(DATA_isHypnotizing3, compound.getBoolean("DataisHypnotizing3"));
		if (compound.contains("DataisWeaponClose"))
			this.entityData.set(DATA_isWeaponClose, compound.getBoolean("DataisWeaponClose"));
		if (compound.contains("DataisRetreating"))
			this.entityData.set(DATA_isRetreating, compound.getBoolean("DataisRetreating"));
		if (compound.contains("DataisRotatingLeft"))
			this.entityData.set(DATA_isRotatingLeft, compound.getBoolean("DataisRotatingLeft"));
		if (compound.contains("DataisRotatingRight"))
			this.entityData.set(DATA_isRotatingRight, compound.getBoolean("DataisRotatingRight"));
		if (compound.contains("DataisDead"))
			this.entityData.set(DATA_isDead, compound.getBoolean("DataisDead"));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.animationState1.animateWhen(WoodweaverPlaybackConditionHypnoProcedure.execute(this), this.tickCount);
			this.animationState2.animateWhen(WoodweaverPlaybackConditionAttackLeftProcedure.execute(this), this.tickCount);
			this.animationState3.animateWhen(WoodweaverPlaybackConditionAttackRightProcedure.execute(this), this.tickCount);
			this.animationState4.animateWhen(WoodweaverPlaybackConditionJumpProcedure.execute(this), this.tickCount);
			this.animationState5.animateWhen(WoodweaverPlaybackConditionLandProcedure.execute(this), this.tickCount);
			this.animationState6.animateWhen(true, this.tickCount);
			this.animationState7.animateWhen(WoodweaverPlaybackConditionHypnoStop3Procedure.execute(this), this.tickCount);
			this.animationState8.animateWhen(WoodweaverPlaybackConditionHypno15Procedure.execute(this), this.tickCount);
			this.animationState9.animateWhen(WoodweaverPlaybackCondition1sProcedure.execute(this), this.tickCount);
			this.animationState10.animateWhen(WoodweaverPlaybackConditionAttackBothProcedure.execute(this), this.tickCount);
			this.animationState11.animateWhen(WoodweaverPlaybackConditionLeftHookProcedure.execute(this), this.tickCount);
			this.animationState12.animateWhen(WoodweaverPlaybackConditionRightHookProcedure.execute(this), this.tickCount);
			this.animationState13.animateWhen(WoodweaverPlaybackConditionRightSweepProcedure.execute(this), this.tickCount);
			this.animationState14.animateWhen(WoodweaverPlaybackConditionLeftSweepProcedure.execute(this), this.tickCount);
			this.animationState15.animateWhen(WoodweaverPlaybackConditionHypno2Procedure.execute(this), this.tickCount);
			this.animationState16.animateWhen(WoodweaverPlaybackConditionHypno3Procedure.execute(this), this.tickCount);
			this.animationState17.animateWhen(WoodweaverPlaybackConditionWeaponizedHypnoCloseProcedure.execute(this), this.tickCount);
			this.animationState18.animateWhen(WoodweaverPlaybackConditionRotateLeftProcedure.execute(this), this.tickCount);
			this.animationState19.animateWhen(WoodweaverPlaybackConditionRotateRightProcedure.execute(this), this.tickCount);
			this.animationState21.animateWhen(WoodweaverPlaybackConditionDeathProcedure.execute(this), this.tickCount);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		WoodweaverOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
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

	@Override
	public boolean isPushedByFluid() {
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		Level world = this.level();
		Entity entity = this;
		return false;
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
		event.register(TheBackwoodsModEntities.WOODWEAVER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return WoodweaverNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.28);
		builder = builder.add(Attributes.MAX_HEALTH, 370);
		builder = builder.add(Attributes.ARMOR, 20);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 16);
		builder = builder.add(Attributes.FOLLOW_RANGE, 128);
		builder = builder.add(Attributes.STEP_HEIGHT, 2);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 1);
		return builder;
	}
}