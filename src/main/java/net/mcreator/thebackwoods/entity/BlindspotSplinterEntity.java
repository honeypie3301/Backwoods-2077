package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.common.NeoForgeMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
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

import net.mcreator.thebackwoods.procedures.SplinterOnInitialEntitySpawnProcedure;
import net.mcreator.thebackwoods.procedures.SplinterEntityDiesProcedure;
import net.mcreator.thebackwoods.procedures.BlindspotSplinterOnEntityTickUpdateProcedure;
import net.mcreator.thebackwoods.procedures.BlindspotSplinterNaturalEntitySpawningConditionProcedure;
import net.mcreator.thebackwoods.procedures.BlindspotSplinterItIsStruckByLightningProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import javax.annotation.Nullable;

public class BlindspotSplinterEntity extends Monster {
	public static final EntityDataAccessor<Integer> DATA_mineProgress = SynchedEntityData.defineId(BlindspotSplinterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_mineX = SynchedEntityData.defineId(BlindspotSplinterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_mineY = SynchedEntityData.defineId(BlindspotSplinterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_mineZ = SynchedEntityData.defineId(BlindspotSplinterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_buildProgress = SynchedEntityData.defineId(BlindspotSplinterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_frozenByRose = SynchedEntityData.defineId(BlindspotSplinterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_scan_timer = SynchedEntityData.defineId(BlindspotSplinterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_watchTimer = SynchedEntityData.defineId(BlindspotSplinterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_isEnraged = SynchedEntityData.defineId(BlindspotSplinterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_Age = SynchedEntityData.defineId(BlindspotSplinterEntity.class, EntityDataSerializers.INT);

	public BlindspotSplinterEntity(EntityType<BlindspotSplinterEntity> type, Level world) {
		super(type, world);
		xpReward = 10;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_mineProgress, 0);
		builder.define(DATA_mineX, 0);
		builder.define(DATA_mineY, 0);
		builder.define(DATA_mineZ, 0);
		builder.define(DATA_buildProgress, 0);
		builder.define(DATA_frozenByRose, 0);
		builder.define(DATA_scan_timer, 0);
		builder.define(DATA_watchTimer, 0);
		builder.define(DATA_isEnraged, 0);
		builder.define(DATA_Age, 0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.getNavigation().getNodeEvaluator().setCanOpenDoors(true);
		this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true) {
			@Override
			protected boolean canPerformAttack(LivingEntity entity) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(entity) < 1.44 && this.mob.getSensing().hasLineOfSight(entity);
			}
		});
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, Player.class, false, true));
		this.goalSelector.addGoal(4, new FloatGoal(this));
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity entity) {
		return super.getPassengerRidingPosition(entity).add(0, -0.35F, 0);
	}

	protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHitIn) {
		super.dropCustomDeathLoot(serverLevel, source, recentlyHitIn);
		this.spawnAtLocation(new ItemStack(Blocks.OAK_PLANKS));
	}

	@Override
	public SoundEvent getAmbientSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_idle"));
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
	public void thunderHit(ServerLevel serverWorld, LightningBolt lightningBolt) {
		super.thunderHit(serverWorld, lightningBolt);
		BlindspotSplinterItIsStruckByLightningProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		if (damagesource.getDirectEntity() instanceof ThrownPotion || damagesource.getDirectEntity() instanceof AreaEffectCloud || damagesource.typeHolder().is(NeoForgeMod.POISON_DAMAGE))
			return false;
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		if (damagesource.is(DamageTypes.DRAGON_BREATH))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		SplinterEntityDiesProcedure.execute(this.level(), source.getEntity());
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata);
		SplinterOnInitialEntitySpawnProcedure.execute(this);
		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("DatamineProgress", this.entityData.get(DATA_mineProgress));
		compound.putInt("DatamineX", this.entityData.get(DATA_mineX));
		compound.putInt("DatamineY", this.entityData.get(DATA_mineY));
		compound.putInt("DatamineZ", this.entityData.get(DATA_mineZ));
		compound.putInt("DatabuildProgress", this.entityData.get(DATA_buildProgress));
		compound.putInt("DatafrozenByRose", this.entityData.get(DATA_frozenByRose));
		compound.putInt("Datascan_timer", this.entityData.get(DATA_scan_timer));
		compound.putInt("DatawatchTimer", this.entityData.get(DATA_watchTimer));
		compound.putInt("DataisEnraged", this.entityData.get(DATA_isEnraged));
		compound.putInt("DataAge", this.entityData.get(DATA_Age));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DatamineProgress"))
			this.entityData.set(DATA_mineProgress, compound.getInt("DatamineProgress"));
		if (compound.contains("DatamineX"))
			this.entityData.set(DATA_mineX, compound.getInt("DatamineX"));
		if (compound.contains("DatamineY"))
			this.entityData.set(DATA_mineY, compound.getInt("DatamineY"));
		if (compound.contains("DatamineZ"))
			this.entityData.set(DATA_mineZ, compound.getInt("DatamineZ"));
		if (compound.contains("DatabuildProgress"))
			this.entityData.set(DATA_buildProgress, compound.getInt("DatabuildProgress"));
		if (compound.contains("DatafrozenByRose"))
			this.entityData.set(DATA_frozenByRose, compound.getInt("DatafrozenByRose"));
		if (compound.contains("Datascan_timer"))
			this.entityData.set(DATA_scan_timer, compound.getInt("Datascan_timer"));
		if (compound.contains("DatawatchTimer"))
			this.entityData.set(DATA_watchTimer, compound.getInt("DatawatchTimer"));
		if (compound.contains("DataisEnraged"))
			this.entityData.set(DATA_isEnraged, compound.getInt("DataisEnraged"));
		if (compound.contains("DataAge"))
			this.entityData.set(DATA_Age, compound.getInt("DataAge"));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		BlindspotSplinterOnEntityTickUpdateProcedure.execute();
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
		event.register(TheBackwoodsModEntities.BLINDSPOT_SPLINTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return BlindspotSplinterNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.335);
		builder = builder.add(Attributes.MAX_HEALTH, 30);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 6);
		builder = builder.add(Attributes.FOLLOW_RANGE, 32);
		builder = builder.add(Attributes.STEP_HEIGHT, 1);
		return builder;
	}
}