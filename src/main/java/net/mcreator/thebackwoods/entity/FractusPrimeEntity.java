package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.common.NeoForgeMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
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

public class FractusPrimeEntity extends Monster {
	public static final EntityDataAccessor<Boolean> DATA_is_laser_activating = SynchedEntityData.defineId(FractusPrimeEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_laser_deactivating = SynchedEntityData.defineId(FractusPrimeEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_laser_burst_activating = SynchedEntityData.defineId(FractusPrimeEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_laser_burst_deactivating = SynchedEntityData.defineId(FractusPrimeEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_laser_aoe_activating = SynchedEntityData.defineId(FractusPrimeEntity.class, EntityDataSerializers.BOOLEAN);
	public final AnimationState animationState0 = new AnimationState();
	public final AnimationState animationState1 = new AnimationState();
	public final AnimationState animationState2 = new AnimationState();
	public final AnimationState animationState3 = new AnimationState();
	public final AnimationState animationState4 = new AnimationState();
	public final AnimationState animationState5 = new AnimationState();

	public FractusPrimeEntity(EntityType<FractusPrimeEntity> type, Level world) {
		super(type, world);
		xpReward = 50;
		setNoAi(false);
		setPersistenceRequired();
		this.moveControl = new FlyingMoveControl(this, 10, true);
		refreshDimensions();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_is_laser_activating, false);
		builder.define(DATA_is_laser_deactivating, false);
		builder.define(DATA_is_laser_burst_activating, false);
		builder.define(DATA_is_laser_burst_deactivating, false);
		builder.define(DATA_is_laser_aoe_activating, false);
	}

	@Override
	protected PathNavigation createNavigation(Level world) {
		return new FlyingPathNavigation(this, world);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new FloatGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public SoundEvent getAmbientSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:fractus_idle"));
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:fractus_hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:fractus_death"));
	}

	@Override
	public boolean causeFallDamage(float l, float d, DamageSource source) {
		return false;
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

		FractusPrimeEntityIsHurtProcedure.execute(entity, sourceentity);
		if (damagesource.is(DamageTypes.IN_FIRE))
			return false;
		if (damagesource.getDirectEntity() instanceof ThrownPotion || damagesource.getDirectEntity() instanceof AreaEffectCloud || damagesource.typeHolder().is(NeoForgeMod.POISON_DAMAGE))
			return false;
		if (damagesource.is(DamageTypes.FALL))
			return false;
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		if (damagesource.is(DamageTypes.LIGHTNING_BOLT))
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
		FractusPrimeEntityDiesProcedure.execute(this);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putBoolean("Datais_laser_activating", this.entityData.get(DATA_is_laser_activating));
		compound.putBoolean("Datais_laser_deactivating", this.entityData.get(DATA_is_laser_deactivating));
		compound.putBoolean("Datais_laser_burst_activating", this.entityData.get(DATA_is_laser_burst_activating));
		compound.putBoolean("Datais_laser_burst_deactivating", this.entityData.get(DATA_is_laser_burst_deactivating));
		compound.putBoolean("Datais_laser_aoe_activating", this.entityData.get(DATA_is_laser_aoe_activating));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Datais_laser_activating"))
			this.entityData.set(DATA_is_laser_activating, compound.getBoolean("Datais_laser_activating"));
		if (compound.contains("Datais_laser_deactivating"))
			this.entityData.set(DATA_is_laser_deactivating, compound.getBoolean("Datais_laser_deactivating"));
		if (compound.contains("Datais_laser_burst_activating"))
			this.entityData.set(DATA_is_laser_burst_activating, compound.getBoolean("Datais_laser_burst_activating"));
		if (compound.contains("Datais_laser_burst_deactivating"))
			this.entityData.set(DATA_is_laser_burst_deactivating, compound.getBoolean("Datais_laser_burst_deactivating"));
		if (compound.contains("Datais_laser_aoe_activating"))
			this.entityData.set(DATA_is_laser_aoe_activating, compound.getBoolean("Datais_laser_aoe_activating"));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.animationState0.animateWhen(true, this.tickCount);
			this.animationState1.animateWhen(FractusPrimePlaybackConditionPrimeActivateProcedure.execute(this), this.tickCount);
			this.animationState2.animateWhen(FractusPrimePlaybackConditionPrimeDeactivateProcedure.execute(this), this.tickCount);
			this.animationState3.animateWhen(FractusPrimePlaybackConditionPrimeBurstOnProcedure.execute(this), this.tickCount);
			this.animationState4.animateWhen(FractusPrimePlaybackConditionPrimeBurstOffProcedure.execute(this), this.tickCount);
			this.animationState5.animateWhen(FractusPrimePlaybackConditionAOEProcedure.execute(this), this.tickCount);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		FractusPrimeOnEntityTickUpdateProcedure.execute();
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
	public void travel(Vec3 dir) {
		this.travelFlying(dir);
	}

	private void travelFlying(Vec3 dir) {
		if (this.isInWater()) {
			this.moveRelative(0.02F, dir);
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
		} else if (this.isInLava()) {
			this.moveRelative(0.02F, dir);
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
		} else {
			this.moveRelative((float) this.getAttributeValue(Attributes.FLYING_SPEED), dir);
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.setDeltaMovement(this.getDeltaMovement().scale(0.91));
		}
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(2f);
	}

	@Override
	protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
	}

	@Override
	public void setNoGravity(boolean ignored) {
		super.setNoGravity(true);
	}

	public void aiStep() {
		super.aiStep();
		this.setNoGravity(true);
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
		event.register(TheBackwoodsModEntities.FRACTUS_PRIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return FractusPrimeNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.2);
		builder = builder.add(Attributes.MAX_HEALTH, 270);
		builder = builder.add(Attributes.ARMOR, 20);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 12);
		builder = builder.add(Attributes.FOLLOW_RANGE, 32);
		builder = builder.add(Attributes.STEP_HEIGHT, 0.6);
		builder = builder.add(Attributes.FLYING_SPEED, 0.2);
		return builder;
	}
}