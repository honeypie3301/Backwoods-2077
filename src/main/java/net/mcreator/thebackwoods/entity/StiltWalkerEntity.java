package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.StiltWalkerOnInitialEntitySpawnProcedure;
import net.mcreator.thebackwoods.procedures.StiltWalkerOnEntityTickUpdateProcedure;
import net.mcreator.thebackwoods.procedures.StiltWalkerNaturalEntitySpawningConditionProcedure;
import net.mcreator.thebackwoods.procedures.StiltWalkerLookAroundConditionProcedure;
import net.mcreator.thebackwoods.procedures.IsRotNearbyConditionProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import javax.annotation.Nullable;

public class StiltWalkerEntity extends Monster {
	public StiltWalkerEntity(EntityType<StiltWalkerEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, (float) 100));
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, RotEntity.class, (float) 100, 1, 0.8) {
			@Override
			public boolean canUse() {
				double x = StiltWalkerEntity.this.getX();
				double y = StiltWalkerEntity.this.getY();
				double z = StiltWalkerEntity.this.getZ();
				Entity entity = StiltWalkerEntity.this;
				Level world = StiltWalkerEntity.this.level();
				return super.canUse() && IsRotNearbyConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = StiltWalkerEntity.this.getX();
				double y = StiltWalkerEntity.this.getY();
				double z = StiltWalkerEntity.this.getZ();
				Entity entity = StiltWalkerEntity.this;
				Level world = StiltWalkerEntity.this.level();
				return super.canContinueToUse() && IsRotNearbyConditionProcedure.execute(world, entity);
			}
		});
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				double x = StiltWalkerEntity.this.getX();
				double y = StiltWalkerEntity.this.getY();
				double z = StiltWalkerEntity.this.getZ();
				Entity entity = StiltWalkerEntity.this;
				Level world = StiltWalkerEntity.this.level();
				return super.canUse() && StiltWalkerLookAroundConditionProcedure.execute(world, x, y, z);
			}
		});
		this.goalSelector.addGoal(4, new FloatGoal(this));
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
	public boolean hurt(DamageSource damagesource, float amount) {
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
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata);
		StiltWalkerOnInitialEntitySpawnProcedure.execute(this);
		return retval;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		StiltWalkerOnEntityTickUpdateProcedure.execute();
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
		event.register(TheBackwoodsModEntities.STILT_WALKER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return StiltWalkerNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.16);
		builder = builder.add(Attributes.MAX_HEALTH, 40);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 5);
		builder = builder.add(Attributes.FOLLOW_RANGE, 32);
		builder = builder.add(Attributes.STEP_HEIGHT, 1);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
		return builder;
	}
}