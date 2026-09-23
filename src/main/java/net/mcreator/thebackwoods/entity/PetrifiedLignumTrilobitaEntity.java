package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.*;
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

public class PetrifiedLignumTrilobitaEntity extends Monster {
	public static final EntityDataAccessor<Boolean> DATA_isHiding = SynchedEntityData.defineId(PetrifiedLignumTrilobitaEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Integer> DATA_eatTimer = SynchedEntityData.defineId(PetrifiedLignumTrilobitaEntity.class, EntityDataSerializers.INT);
	public final AnimationState animationState1 = new AnimationState();
	public final AnimationState animationState2 = new AnimationState();

	public PetrifiedLignumTrilobitaEntity(EntityType<PetrifiedLignumTrilobitaEntity> type, Level world) {
		super(type, world);
		xpReward = 4;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_isHiding, false);
		builder.define(DATA_eatTimer, 0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new FloatGoal(this) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canContinueToUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}
		});
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, RotEntity.class, (float) 100, 2, 1.7));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected boolean canPerformAttack(LivingEntity entity) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(entity) < 2.25 && this.mob.getSensing().hasLineOfSight(entity);
			}

			@Override
			public boolean canUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canUse() && PetrifiedTrilobitaAttackConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canContinueToUse() && PetrifiedTrilobitaAttackConditionProcedure.execute(entity);
			}

		});
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.8) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canContinueToUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}
		});
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canContinueToUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(6, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canUse() && PetrifiedTrilobitaAttackConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canContinueToUse() && PetrifiedTrilobitaAttackConditionProcedure.execute(entity);
			}
		}.setAlertOthers());
		this.goalSelector.addGoal(7, new AvoidEntityGoal<>(this, Player.class, (float) 6, 1, 1.2) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canContinueToUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}
		});
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, (float) 8) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canContinueToUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}
		});
		this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Mob.class, (float) 12) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumTrilobitaEntity.this.getX();
				double y = PetrifiedLignumTrilobitaEntity.this.getY();
				double z = PetrifiedLignumTrilobitaEntity.this.getZ();
				Entity entity = PetrifiedLignumTrilobitaEntity.this;
				Level world = PetrifiedLignumTrilobitaEntity.this.level();
				return super.canContinueToUse() && PetrifiedTrilobitaAvoidConditionProcedure.execute(entity);
			}
		});
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float f) {
		return super.getPassengerAttachmentPoint(entity, dimensions, f).add(0, -0.2f, 0);
	}

	@Override
	public SoundEvent getAmbientSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_idle"));
	}

	@Override
	public void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:vermis_step")), 0.15f, 1);
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_hurt"));
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

		LignumTrilobitaEntityIsHurt2Procedure.execute(world, x, y, z);
		if (damagesource.is(DamageTypes.FALL))
			return false;
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata);
		LignumTrilobitaOnInitialEntitySpawnProcedure.execute(this);
		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putBoolean("DataisHiding", this.entityData.get(DATA_isHiding));
		compound.putInt("DataeatTimer", this.entityData.get(DATA_eatTimer));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DataisHiding"))
			this.entityData.set(DATA_isHiding, compound.getBoolean("DataisHiding"));
		if (compound.contains("DataeatTimer"))
			this.entityData.set(DATA_eatTimer, compound.getInt("DataeatTimer"));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.animationState1.animateWhen(PetrifiedLignumTrilobitaPlaybackConditionLowHPProcedure.execute(this), this.tickCount);
			this.animationState2.animateWhen(PetrifiedLignumTrilobitaPlaybackConditionUnhideProcedure.execute(this), this.tickCount);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		PetrifiedLignumTrilobitaOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
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
		event.register(TheBackwoodsModEntities.PETRIFIED_LIGNUM_TRILOBITA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return PretrifiedLignumTrilobitaNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.14);
		builder = builder.add(Attributes.MAX_HEALTH, 40);
		builder = builder.add(Attributes.ARMOR, 14);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 4);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		builder = builder.add(Attributes.STEP_HEIGHT, 1);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 0.3);
		return builder;
	}
}