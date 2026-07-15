package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.*;
import net.mcreator.thebackwoods.init.TheBackwoodsModItems;

import javax.annotation.Nullable;

public class RotEntity extends Monster {
	public static final EntityDataAccessor<Integer> DATA_mineProgress = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_sentinel_solar_charge_ticks = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_sentinel_cryo_charge_ticks = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Boolean> DATA_is_laser_firing = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_airborne_state = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_overhead_preparing = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_ground_crushing = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_left_punching = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_right_punching = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_overhead = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_laser_closing = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_slam_charge = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_rider_charging = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_sonic_charging = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_sonic_active = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_uppercut_charging = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_rider_kick = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_sonic_boom = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_falling_heavy = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_sonic_boom_large = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_armor_ripping = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_blocking = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_blocking_finish = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_uppercutting = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_dropkick_charging = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_heavy_right_punching = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_is_heavy_left_punching = SynchedEntityData.defineId(RotEntity.class, EntityDataSerializers.BOOLEAN);
	public final AnimationState animationState0 = new AnimationState();
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

	public RotEntity(EntityType<RotEntity> type, Level world) {
		super(type, world);
		xpReward = 7777;
		setNoAi(false);
		refreshDimensions();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_mineProgress, 0);
		builder.define(DATA_sentinel_solar_charge_ticks, 0);
		builder.define(DATA_sentinel_cryo_charge_ticks, 0);
		builder.define(DATA_is_laser_firing, false);
		builder.define(DATA_is_airborne_state, false);
		builder.define(DATA_is_overhead_preparing, false);
		builder.define(DATA_is_ground_crushing, false);
		builder.define(DATA_is_left_punching, false);
		builder.define(DATA_is_right_punching, false);
		builder.define(DATA_is_overhead, false);
		builder.define(DATA_is_laser_closing, false);
		builder.define(DATA_is_slam_charge, false);
		builder.define(DATA_is_rider_charging, false);
		builder.define(DATA_is_sonic_charging, false);
		builder.define(DATA_is_sonic_active, false);
		builder.define(DATA_is_uppercut_charging, false);
		builder.define(DATA_is_rider_kick, false);
		builder.define(DATA_is_sonic_boom, false);
		builder.define(DATA_is_falling_heavy, false);
		builder.define(DATA_is_sonic_boom_large, false);
		builder.define(DATA_is_armor_ripping, false);
		builder.define(DATA_is_blocking, false);
		builder.define(DATA_is_blocking_finish, false);
		builder.define(DATA_is_uppercutting, false);
		builder.define(DATA_is_dropkick_charging, false);
		builder.define(DATA_is_heavy_right_punching, false);
		builder.define(DATA_is_heavy_left_punching, false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, false, false));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, Monster.class, false, false));
		this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.3, true) {
			@Override
			protected boolean canPerformAttack(LivingEntity entity) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(entity) < (this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth()) && this.mob.getSensing().hasLineOfSight(entity);
			}
		});
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(7, new FloatGoal(this));
	}

	protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHitIn) {
		super.dropCustomDeathLoot(serverLevel, source, recentlyHitIn);
		this.spawnAtLocation(new ItemStack(TheBackwoodsModItems.RESONANT_ROT_EFFIGY.get()));
	}

	@Override
	public SoundEvent getAmbientSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:rot_idle"));
	}

	@Override
	public void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:rot_step")), 0.15f, 1);
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("intentionally_empty"));
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		if (damagesource.is(DamageTypes.FALL))
			return false;
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		RotEntityDiesProcedure.execute(source.getEntity());
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata);
		RotOnInitialEntitySpawnProcedure.execute(world, this);
		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("DatamineProgress", this.entityData.get(DATA_mineProgress));
		compound.putInt("Datasentinel_solar_charge_ticks", this.entityData.get(DATA_sentinel_solar_charge_ticks));
		compound.putInt("Datasentinel_cryo_charge_ticks", this.entityData.get(DATA_sentinel_cryo_charge_ticks));
		compound.putBoolean("Datais_laser_firing", this.entityData.get(DATA_is_laser_firing));
		compound.putBoolean("Datais_airborne_state", this.entityData.get(DATA_is_airborne_state));
		compound.putBoolean("Datais_overhead_preparing", this.entityData.get(DATA_is_overhead_preparing));
		compound.putBoolean("Datais_ground_crushing", this.entityData.get(DATA_is_ground_crushing));
		compound.putBoolean("Datais_left_punching", this.entityData.get(DATA_is_left_punching));
		compound.putBoolean("Datais_right_punching", this.entityData.get(DATA_is_right_punching));
		compound.putBoolean("Datais_overhead", this.entityData.get(DATA_is_overhead));
		compound.putBoolean("Datais_laser_closing", this.entityData.get(DATA_is_laser_closing));
		compound.putBoolean("Datais_slam_charge", this.entityData.get(DATA_is_slam_charge));
		compound.putBoolean("Datais_rider_charging", this.entityData.get(DATA_is_rider_charging));
		compound.putBoolean("Datais_sonic_charging", this.entityData.get(DATA_is_sonic_charging));
		compound.putBoolean("Datais_sonic_active", this.entityData.get(DATA_is_sonic_active));
		compound.putBoolean("Datais_uppercut_charging", this.entityData.get(DATA_is_uppercut_charging));
		compound.putBoolean("Datais_rider_kick", this.entityData.get(DATA_is_rider_kick));
		compound.putBoolean("Datais_sonic_boom", this.entityData.get(DATA_is_sonic_boom));
		compound.putBoolean("Datais_falling_heavy", this.entityData.get(DATA_is_falling_heavy));
		compound.putBoolean("Datais_sonic_boom_large", this.entityData.get(DATA_is_sonic_boom_large));
		compound.putBoolean("Datais_armor_ripping", this.entityData.get(DATA_is_armor_ripping));
		compound.putBoolean("Datais_blocking", this.entityData.get(DATA_is_blocking));
		compound.putBoolean("Datais_blocking_finish", this.entityData.get(DATA_is_blocking_finish));
		compound.putBoolean("Datais_uppercutting", this.entityData.get(DATA_is_uppercutting));
		compound.putBoolean("Datais_dropkick_charging", this.entityData.get(DATA_is_dropkick_charging));
		compound.putBoolean("Datais_heavy_right_punching", this.entityData.get(DATA_is_heavy_right_punching));
		compound.putBoolean("Datais_heavy_left_punching", this.entityData.get(DATA_is_heavy_left_punching));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DatamineProgress"))
			this.entityData.set(DATA_mineProgress, compound.getInt("DatamineProgress"));
		if (compound.contains("Datasentinel_solar_charge_ticks"))
			this.entityData.set(DATA_sentinel_solar_charge_ticks, compound.getInt("Datasentinel_solar_charge_ticks"));
		if (compound.contains("Datasentinel_cryo_charge_ticks"))
			this.entityData.set(DATA_sentinel_cryo_charge_ticks, compound.getInt("Datasentinel_cryo_charge_ticks"));
		if (compound.contains("Datais_laser_firing"))
			this.entityData.set(DATA_is_laser_firing, compound.getBoolean("Datais_laser_firing"));
		if (compound.contains("Datais_airborne_state"))
			this.entityData.set(DATA_is_airborne_state, compound.getBoolean("Datais_airborne_state"));
		if (compound.contains("Datais_overhead_preparing"))
			this.entityData.set(DATA_is_overhead_preparing, compound.getBoolean("Datais_overhead_preparing"));
		if (compound.contains("Datais_ground_crushing"))
			this.entityData.set(DATA_is_ground_crushing, compound.getBoolean("Datais_ground_crushing"));
		if (compound.contains("Datais_left_punching"))
			this.entityData.set(DATA_is_left_punching, compound.getBoolean("Datais_left_punching"));
		if (compound.contains("Datais_right_punching"))
			this.entityData.set(DATA_is_right_punching, compound.getBoolean("Datais_right_punching"));
		if (compound.contains("Datais_overhead"))
			this.entityData.set(DATA_is_overhead, compound.getBoolean("Datais_overhead"));
		if (compound.contains("Datais_laser_closing"))
			this.entityData.set(DATA_is_laser_closing, compound.getBoolean("Datais_laser_closing"));
		if (compound.contains("Datais_slam_charge"))
			this.entityData.set(DATA_is_slam_charge, compound.getBoolean("Datais_slam_charge"));
		if (compound.contains("Datais_rider_charging"))
			this.entityData.set(DATA_is_rider_charging, compound.getBoolean("Datais_rider_charging"));
		if (compound.contains("Datais_sonic_charging"))
			this.entityData.set(DATA_is_sonic_charging, compound.getBoolean("Datais_sonic_charging"));
		if (compound.contains("Datais_sonic_active"))
			this.entityData.set(DATA_is_sonic_active, compound.getBoolean("Datais_sonic_active"));
		if (compound.contains("Datais_uppercut_charging"))
			this.entityData.set(DATA_is_uppercut_charging, compound.getBoolean("Datais_uppercut_charging"));
		if (compound.contains("Datais_rider_kick"))
			this.entityData.set(DATA_is_rider_kick, compound.getBoolean("Datais_rider_kick"));
		if (compound.contains("Datais_sonic_boom"))
			this.entityData.set(DATA_is_sonic_boom, compound.getBoolean("Datais_sonic_boom"));
		if (compound.contains("Datais_falling_heavy"))
			this.entityData.set(DATA_is_falling_heavy, compound.getBoolean("Datais_falling_heavy"));
		if (compound.contains("Datais_sonic_boom_large"))
			this.entityData.set(DATA_is_sonic_boom_large, compound.getBoolean("Datais_sonic_boom_large"));
		if (compound.contains("Datais_armor_ripping"))
			this.entityData.set(DATA_is_armor_ripping, compound.getBoolean("Datais_armor_ripping"));
		if (compound.contains("Datais_blocking"))
			this.entityData.set(DATA_is_blocking, compound.getBoolean("Datais_blocking"));
		if (compound.contains("Datais_blocking_finish"))
			this.entityData.set(DATA_is_blocking_finish, compound.getBoolean("Datais_blocking_finish"));
		if (compound.contains("Datais_uppercutting"))
			this.entityData.set(DATA_is_uppercutting, compound.getBoolean("Datais_uppercutting"));
		if (compound.contains("Datais_dropkick_charging"))
			this.entityData.set(DATA_is_dropkick_charging, compound.getBoolean("Datais_dropkick_charging"));
		if (compound.contains("Datais_heavy_right_punching"))
			this.entityData.set(DATA_is_heavy_right_punching, compound.getBoolean("Datais_heavy_right_punching"));
		if (compound.contains("Datais_heavy_left_punching"))
			this.entityData.set(DATA_is_heavy_left_punching, compound.getBoolean("Datais_heavy_left_punching"));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.animationState0.animateWhen(RotPlaybackConditionRiderKickProcedure.execute(this), this.tickCount);
			this.animationState2.animateWhen(RotPlaybackConditionAirTimeProcedure.execute(this), this.tickCount);
			this.animationState3.animateWhen(RotPlaybackConditionOverheadProcedure.execute(this), this.tickCount);
			this.animationState4.animateWhen(RotPlaybackConditionSlamCrushProcedure.execute(this), this.tickCount);
			this.animationState5.animateWhen(RotPlaybackConditionLeftPunchProcedure.execute(this), this.tickCount);
			this.animationState6.animateWhen(RotPlaybackConditionRightPunchProcedure.execute(this), this.tickCount);
			this.animationState7.animateWhen(RotPlaybackConditionOpenLaserProcedure.execute(this), this.tickCount);
			this.animationState8.animateWhen(RotPlaybackConditionCloseLaserProcedure.execute(this), this.tickCount);
			this.animationState9.animateWhen(RotPlaybackConditionSlamChargeProcedure.execute(this), this.tickCount);
			this.animationState10.animateWhen(RotPlaybackConditionFallProcedure.execute(this), this.tickCount);
			this.animationState11.animateWhen(RotPlaybackConditionSonicBoomProcedure.execute(this), this.tickCount);
			this.animationState12.animateWhen(RotPlaybackConditionSonicLargeProcedure.execute(this), this.tickCount);
			this.animationState13.animateWhen(RotPlaybackConditionArmorRipProcedure.execute(this), this.tickCount);
			this.animationState14.animateWhen(RotPlaybackConditionBlockProcedure.execute(this), this.tickCount);
			this.animationState15.animateWhen(RotPlaybackConditionBlockFinishProcedure.execute(this), this.tickCount);
			this.animationState16.animateWhen(RotPlaybackConditionUppercutProcedure.execute(this), this.tickCount);
			this.animationState17.animateWhen(RotPlaybackConditionDropkickProcedure.execute(this), this.tickCount);
			this.animationState18.animateWhen(RotPlaybackConditionHeavyLeftProcedure.execute(this), this.tickCount);
			this.animationState19.animateWhen(RotPlaybackConditionHeavyRightProcedure.execute(this), this.tickCount);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		RotOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
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
		return super.getDefaultDimensions(pose).scale(1.25f);
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 550);
		builder = builder.add(Attributes.ARMOR, 15);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 18);
		builder = builder.add(Attributes.FOLLOW_RANGE, 64);
		builder = builder.add(Attributes.STEP_HEIGHT, 1.5);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 0.6);
		return builder;
	}
}