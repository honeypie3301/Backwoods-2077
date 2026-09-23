package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Monster;
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

import net.mcreator.thebackwoods.procedures.*;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import javax.annotation.Nullable;

public class LignumGigasEntity extends Monster {
	public static final EntityDataAccessor<Boolean> DATA_play_attack_anim = SynchedEntityData.defineId(LignumGigasEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_play_lower_anim = SynchedEntityData.defineId(LignumGigasEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Integer> DATA_hit_stage = SynchedEntityData.defineId(LignumGigasEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_hit_count = SynchedEntityData.defineId(LignumGigasEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Boolean> DATA_seq_active = SynchedEntityData.defineId(LignumGigasEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_effect_done = SynchedEntityData.defineId(LignumGigasEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Integer> DATA_aura_time_left = SynchedEntityData.defineId(LignumGigasEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_aura_sound_cd = SynchedEntityData.defineId(LignumGigasEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_seq_t = SynchedEntityData.defineId(LignumGigasEntity.class, EntityDataSerializers.INT);
	public final AnimationState animationState0 = new AnimationState();
	public final AnimationState animationState1 = new AnimationState();

	public LignumGigasEntity(EntityType<LignumGigasEntity> type, Level world) {
		super(type, world);
		xpReward = 500;
		setNoAi(false);
		setPersistenceRequired();
		refreshDimensions();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_play_attack_anim, false);
		builder.define(DATA_play_lower_anim, false);
		builder.define(DATA_hit_stage, 0);
		builder.define(DATA_hit_count, 0);
		builder.define(DATA_seq_active, false);
		builder.define(DATA_effect_done, false);
		builder.define(DATA_aura_time_left, 0);
		builder.define(DATA_aura_sound_cd, 0);
		builder.define(DATA_seq_t, 0);
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
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:gigas_hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:gigas_death"));
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

		LignumGigasEntityIsHurtProcedure.execute(entity);
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
		LignumGigasOnInitialEntitySpawnProcedure.execute(this);
		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putBoolean("Dataplay_attack_anim", this.entityData.get(DATA_play_attack_anim));
		compound.putBoolean("Dataplay_lower_anim", this.entityData.get(DATA_play_lower_anim));
		compound.putInt("Datahit_stage", this.entityData.get(DATA_hit_stage));
		compound.putInt("Datahit_count", this.entityData.get(DATA_hit_count));
		compound.putBoolean("Dataseq_active", this.entityData.get(DATA_seq_active));
		compound.putBoolean("Dataeffect_done", this.entityData.get(DATA_effect_done));
		compound.putInt("Dataaura_time_left", this.entityData.get(DATA_aura_time_left));
		compound.putInt("Dataaura_sound_cd", this.entityData.get(DATA_aura_sound_cd));
		compound.putInt("Dataseq_t", this.entityData.get(DATA_seq_t));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Dataplay_attack_anim"))
			this.entityData.set(DATA_play_attack_anim, compound.getBoolean("Dataplay_attack_anim"));
		if (compound.contains("Dataplay_lower_anim"))
			this.entityData.set(DATA_play_lower_anim, compound.getBoolean("Dataplay_lower_anim"));
		if (compound.contains("Datahit_stage"))
			this.entityData.set(DATA_hit_stage, compound.getInt("Datahit_stage"));
		if (compound.contains("Datahit_count"))
			this.entityData.set(DATA_hit_count, compound.getInt("Datahit_count"));
		if (compound.contains("Dataseq_active"))
			this.entityData.set(DATA_seq_active, compound.getBoolean("Dataseq_active"));
		if (compound.contains("Dataeffect_done"))
			this.entityData.set(DATA_effect_done, compound.getBoolean("Dataeffect_done"));
		if (compound.contains("Dataaura_time_left"))
			this.entityData.set(DATA_aura_time_left, compound.getInt("Dataaura_time_left"));
		if (compound.contains("Dataaura_sound_cd"))
			this.entityData.set(DATA_aura_sound_cd, compound.getInt("Dataaura_sound_cd"));
		if (compound.contains("Dataseq_t"))
			this.entityData.set(DATA_seq_t, compound.getInt("Dataseq_t"));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.animationState0.animateWhen(LignumGigasPlaybackConditionProcedure.execute(this), this.tickCount);
			this.animationState1.animateWhen(LignumGigasPlaybackConditionReverseProcedure.execute(this), this.tickCount);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		LignumGigasOnEntityTickUpdateProcedure.execute(this);
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
		return super.getDefaultDimensions(pose).scale(28.5f);
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
		event.register(TheBackwoodsModEntities.LIGNUM_GIGAS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return LignumGigasNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.12);
		builder = builder.add(Attributes.MAX_HEALTH, 1008);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		builder = builder.add(Attributes.STEP_HEIGHT, 7);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 777);
		return builder;
	}
}