package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.common.NeoForgeMod;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.monster.Monster;
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

import net.mcreator.thebackwoods.procedures.VerdantEnginePlaybackConditionHoldProcedure;
import net.mcreator.thebackwoods.procedures.VerdantEnginePlaybackConditionDeathProcedure;
import net.mcreator.thebackwoods.procedures.VerdantEnginePlaybackConditionDeactivateProcedure;
import net.mcreator.thebackwoods.procedures.VerdantEnginePlaybackConditionActivateProcedure;
import net.mcreator.thebackwoods.procedures.VerdantEngineOnEntityTickUpdateProcedure;

public class VerdantEngineEntity extends Monster {
	public static final EntityDataAccessor<Boolean> DATA_isTerraforming = SynchedEntityData.defineId(VerdantEngineEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isDead = SynchedEntityData.defineId(VerdantEngineEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_stopTerra = SynchedEntityData.defineId(VerdantEngineEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_currentlyTerraforming = SynchedEntityData.defineId(VerdantEngineEntity.class, EntityDataSerializers.BOOLEAN);
	public final AnimationState animationState0 = new AnimationState();
	public final AnimationState animationState1 = new AnimationState();
	public final AnimationState animationState2 = new AnimationState();
	public final AnimationState animationState3 = new AnimationState();
	public final AnimationState animationState4 = new AnimationState();

	public VerdantEngineEntity(EntityType<VerdantEngineEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
		setPersistenceRequired();
		refreshDimensions();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_isTerraforming, false);
		builder.define(DATA_isDead, false);
		builder.define(DATA_stopTerra, false);
		builder.define(DATA_currentlyTerraforming, false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();

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
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:fractus_hurt"));
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		if (damagesource.is(DamageTypes.IN_FIRE))
			return false;
		if (damagesource.getDirectEntity() instanceof AbstractArrow)
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
		if (damagesource.is(DamageTypes.FALLING_ANVIL))
			return false;
		if (damagesource.is(DamageTypes.WITHER) || damagesource.is(DamageTypes.WITHER_SKULL))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putBoolean("DataisTerraforming", this.entityData.get(DATA_isTerraforming));
		compound.putBoolean("DataisDead", this.entityData.get(DATA_isDead));
		compound.putBoolean("DatastopTerra", this.entityData.get(DATA_stopTerra));
		compound.putBoolean("DatacurrentlyTerraforming", this.entityData.get(DATA_currentlyTerraforming));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DataisTerraforming"))
			this.entityData.set(DATA_isTerraforming, compound.getBoolean("DataisTerraforming"));
		if (compound.contains("DataisDead"))
			this.entityData.set(DATA_isDead, compound.getBoolean("DataisDead"));
		if (compound.contains("DatastopTerra"))
			this.entityData.set(DATA_stopTerra, compound.getBoolean("DatastopTerra"));
		if (compound.contains("DatacurrentlyTerraforming"))
			this.entityData.set(DATA_currentlyTerraforming, compound.getBoolean("DatacurrentlyTerraforming"));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.animationState0.animateWhen(true, this.tickCount);
			this.animationState1.animateWhen(VerdantEnginePlaybackConditionActivateProcedure.execute(this), this.tickCount);
			this.animationState2.animateWhen(VerdantEnginePlaybackConditionDeathProcedure.execute(this), this.tickCount);
			this.animationState3.animateWhen(VerdantEnginePlaybackConditionDeactivateProcedure.execute(this), this.tickCount);
			this.animationState4.animateWhen(VerdantEnginePlaybackConditionHoldProcedure.execute(this), this.tickCount);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		VerdantEngineOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
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

	@Override
	public EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(15f);
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 1024);
		builder = builder.add(Attributes.ARMOR, 75);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 0);
		builder = builder.add(Attributes.FOLLOW_RANGE, 0);
		builder = builder.add(Attributes.STEP_HEIGHT, 5);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 10);
		return builder;
	}
}