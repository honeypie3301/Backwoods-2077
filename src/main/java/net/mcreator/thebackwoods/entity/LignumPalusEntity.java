package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.common.NeoForgeMod;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
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

public class LignumPalusEntity extends Monster {
	public static final EntityDataAccessor<Integer> DATA_mouth_state = SynchedEntityData.defineId(LignumPalusEntity.class, EntityDataSerializers.INT);
	public final AnimationState animationState0 = new AnimationState();
	public final AnimationState animationState1 = new AnimationState();

	public LignumPalusEntity(EntityType<LignumPalusEntity> type, Level world) {
		super(type, world);
		xpReward = 5;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_mouth_state, 0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1));
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, RotEntity.class, (float) 100, 0.9, 0.7) {
			@Override
			public boolean canUse() {
				double x = LignumPalusEntity.this.getX();
				double y = LignumPalusEntity.this.getY();
				double z = LignumPalusEntity.this.getZ();
				Entity entity = LignumPalusEntity.this;
				Level world = LignumPalusEntity.this.level();
				return super.canUse() && IsRotNearbyConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = LignumPalusEntity.this.getX();
				double y = LignumPalusEntity.this.getY();
				double z = LignumPalusEntity.this.getZ();
				Entity entity = LignumPalusEntity.this;
				Level world = LignumPalusEntity.this.level();
				return super.canContinueToUse() && IsRotNearbyConditionProcedure.execute(world, entity);
			}
		});
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
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
		if (damagesource.getDirectEntity() instanceof ThrownPotion || damagesource.getDirectEntity() instanceof AreaEffectCloud || damagesource.typeHolder().is(NeoForgeMod.POISON_DAMAGE))
			return false;
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		if (damagesource.is(DamageTypes.FALLING_ANVIL))
			return false;
		if (damagesource.is(DamageTypes.WITHER) || damagesource.is(DamageTypes.WITHER_SKULL))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata);
		LignumPalusOnInitialEntitySpawnProcedure.execute(this);
		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("Datamouth_state", this.entityData.get(DATA_mouth_state));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Datamouth_state"))
			this.entityData.set(DATA_mouth_state, compound.getInt("Datamouth_state"));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.animationState0.animateWhen(LignumPalusPlaybackConditionOpenProcedure.execute(this), this.tickCount);
			this.animationState1.animateWhen(LignumPalusPlaybackConditionCloseProcedure.execute(this), this.tickCount);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		LignumPalusOnEntityTickUpdateProcedure.execute();
	}

	@Override
	public void playerTouch(Player sourceentity) {
		super.playerTouch(sourceentity);
		LignumVermisPlayerCollidesWithThisEntityProcedure.execute(this.level(), this, sourceentity);
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
		event.register(TheBackwoodsModEntities.LIGNUM_PALUS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return LignumPalusNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.23);
		builder = builder.add(Attributes.MAX_HEALTH, 50);
		builder = builder.add(Attributes.ARMOR, 6);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 8);
		builder = builder.add(Attributes.FOLLOW_RANGE, 32);
		builder = builder.add(Attributes.STEP_HEIGHT, 1);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 0.8);
		return builder;
	}
}