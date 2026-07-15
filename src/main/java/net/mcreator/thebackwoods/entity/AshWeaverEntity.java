package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.SplinterEntityDiesProcedure;
import net.mcreator.thebackwoods.procedures.AshWeaverOnEntityTickUpdateProcedure;
import net.mcreator.thebackwoods.procedures.AshWeaverNaturalEntitySpawningConditionProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

public class AshWeaverEntity extends PathfinderMob {
	public static final EntityDataAccessor<Integer> DATA_roseCooldown = SynchedEntityData.defineId(AshWeaverEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_roseCount = SynchedEntityData.defineId(AshWeaverEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<String> DATA_assignedPlayer = SynchedEntityData.defineId(AshWeaverEntity.class, EntityDataSerializers.STRING);

	public AshWeaverEntity(EntityType<AshWeaverEntity> type, Level world) {
		super(type, world);
		xpReward = 1;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_roseCooldown, 0);
		builder.define(DATA_roseCount, 0);
		builder.define(DATA_assignedPlayer, "");
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.getNavigation().getNodeEvaluator().setCanOpenDoors(true);
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, (float) 32));
		this.goalSelector.addGoal(2, new PanicGoal(this, 1.2));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1));
		this.goalSelector.addGoal(5, new FloatGoal(this));
		this.goalSelector.addGoal(6, new OpenDoorGoal(this, false));
		this.goalSelector.addGoal(7, new OpenDoorGoal(this, true));
		this.targetSelector.addGoal(8, new NearestAttackableTargetGoal(this, SplinterEntity.class, false, false));
		this.targetSelector.addGoal(9, new NearestAttackableTargetGoal(this, LogSplinterEntity.class, false, false));
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity entity) {
		return super.getPassengerRidingPosition(entity).add(0, -0.35F, 0);
	}

	protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHitIn) {
		super.dropCustomDeathLoot(serverLevel, source, recentlyHitIn);
		this.spawnAtLocation(new ItemStack(TheBackwoodsModBlocks.ASH_ROSE.get()));
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
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("intentionally_empty"));
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		SplinterEntityDiesProcedure.execute(this.level(), source.getEntity());
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("DataroseCooldown", this.entityData.get(DATA_roseCooldown));
		compound.putInt("DataroseCount", this.entityData.get(DATA_roseCount));
		compound.putString("DataassignedPlayer", this.entityData.get(DATA_assignedPlayer));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DataroseCooldown"))
			this.entityData.set(DATA_roseCooldown, compound.getInt("DataroseCooldown"));
		if (compound.contains("DataroseCount"))
			this.entityData.set(DATA_roseCount, compound.getInt("DataroseCount"));
		if (compound.contains("DataassignedPlayer"))
			this.entityData.set(DATA_assignedPlayer, compound.getString("DataassignedPlayer"));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		AshWeaverOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
		event.register(TheBackwoodsModEntities.ASH_WEAVER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return AshWeaverNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 40);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 0);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		builder = builder.add(Attributes.STEP_HEIGHT, 1);
		return builder;
	}
}