package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import net.mcreator.thebackwoods.procedures.DorcelessSplinterPlaybackPunchConditionProcedure;
import net.mcreator.thebackwoods.procedures.DorcelessSplinterOnEntityTickUpdateProcedure;
import net.mcreator.thebackwoods.procedures.DorcelessSplinterNaturalEntitySpawningConditionProcedure;
import net.mcreator.thebackwoods.procedures.DorcelessSplinterEntityIsHurtProcedure;
import net.mcreator.thebackwoods.procedures.DorcelessCanWanderConditionProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

public class DorcelessSplinterEntity extends Monster {
	public final AnimationState animationState1 = new AnimationState();

	public DorcelessSplinterEntity(EntityType<DorcelessSplinterEntity> type, Level world) {
		super(type, world);
		xpReward = 10;
		setNoAi(false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, RotEntity.class, (float) 100, 1, 1.2));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, (float) 50));
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.7) {
			@Override
			public boolean canUse() {
				double x = DorcelessSplinterEntity.this.getX();
				double y = DorcelessSplinterEntity.this.getY();
				double z = DorcelessSplinterEntity.this.getZ();
				Entity entity = DorcelessSplinterEntity.this;
				Level world = DorcelessSplinterEntity.this.level();
				return super.canUse() && DorcelessCanWanderConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = DorcelessSplinterEntity.this.getX();
				double y = DorcelessSplinterEntity.this.getY();
				double z = DorcelessSplinterEntity.this.getZ();
				Entity entity = DorcelessSplinterEntity.this;
				Level world = DorcelessSplinterEntity.this.level();
				return super.canContinueToUse() && DorcelessCanWanderConditionProcedure.execute(entity);
			}
		});
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				double x = DorcelessSplinterEntity.this.getX();
				double y = DorcelessSplinterEntity.this.getY();
				double z = DorcelessSplinterEntity.this.getZ();
				Entity entity = DorcelessSplinterEntity.this;
				Level world = DorcelessSplinterEntity.this.level();
				return super.canUse() && DorcelessCanWanderConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = DorcelessSplinterEntity.this.getX();
				double y = DorcelessSplinterEntity.this.getY();
				double z = DorcelessSplinterEntity.this.getZ();
				Entity entity = DorcelessSplinterEntity.this;
				Level world = DorcelessSplinterEntity.this.level();
				return super.canContinueToUse() && DorcelessCanWanderConditionProcedure.execute(entity);
			}
		});
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.death"));
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

		DorcelessSplinterEntityIsHurtProcedure.execute(world, x, y, z, damagesource, entity, immediatesourceentity, sourceentity);
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.animationState1.animateWhen(DorcelessSplinterPlaybackPunchConditionProcedure.execute(this), this.tickCount);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		DorcelessSplinterOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
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
		event.register(TheBackwoodsModEntities.DORCELESS_SPLINTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return DorcelessSplinterNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.22);
		builder = builder.add(Attributes.MAX_HEALTH, 24);
		builder = builder.add(Attributes.ARMOR, 3);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 6);
		builder = builder.add(Attributes.FOLLOW_RANGE, 32);
		builder = builder.add(Attributes.STEP_HEIGHT, 0.6);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
		return builder;
	}
}