package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.*;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

public class KyneSplinterEntity extends Monster {
	public final AnimationState animationState0 = new AnimationState();

	public KyneSplinterEntity(EntityType<KyneSplinterEntity> type, Level world) {
		super(type, world);
		xpReward = 7;
		setNoAi(false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1, true) {
			@Override
			protected boolean canPerformAttack(LivingEntity entity) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(entity) < 2.89 && this.mob.getSensing().hasLineOfSight(entity);
			}

			@Override
			public boolean canUse() {
				double x = KyneSplinterEntity.this.getX();
				double y = KyneSplinterEntity.this.getY();
				double z = KyneSplinterEntity.this.getZ();
				Entity entity = KyneSplinterEntity.this;
				Level world = KyneSplinterEntity.this.level();
				return super.canUse() && KyneAttackConditionProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = KyneSplinterEntity.this.getX();
				double y = KyneSplinterEntity.this.getY();
				double z = KyneSplinterEntity.this.getZ();
				Entity entity = KyneSplinterEntity.this;
				Level world = KyneSplinterEntity.this.level();
				return super.canContinueToUse() && KyneAttackConditionProcedure.execute(entity);
			}

		});
		this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, RotEntity.class, (float) 100, 1, 1.2));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, (float) 50));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1) {
			@Override
			public boolean canUse() {
				double x = KyneSplinterEntity.this.getX();
				double y = KyneSplinterEntity.this.getY();
				double z = KyneSplinterEntity.this.getZ();
				Entity entity = KyneSplinterEntity.this;
				Level world = KyneSplinterEntity.this.level();
				return super.canUse() && KyneWanderConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = KyneSplinterEntity.this.getX();
				double y = KyneSplinterEntity.this.getY();
				double z = KyneSplinterEntity.this.getZ();
				Entity entity = KyneSplinterEntity.this;
				Level world = KyneSplinterEntity.this.level();
				return super.canContinueToUse() && KyneWanderConditionProcedure.execute(world, entity);
			}
		});
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override
	public SoundEvent getAmbientSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_idle"));
	}

	@Override
	public void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bamboo_wood.step")), 0.15f, 1);
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.cherry_wood.hit"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_hurt"));
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

		KyneSplinterEntityIsHurtProcedure.execute(world, x, y, z, entity, sourceentity);
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
			this.animationState0.animateWhen(KyneSplinterPlaybackConditionPunchProcedure.execute(this), this.tickCount);
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		KyneSplinterOnEntityTickUpdateProcedure.execute(this);
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
		event.register(TheBackwoodsModEntities.KYNE_SPLINTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return KyneSplinterNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.28);
		builder = builder.add(Attributes.MAX_HEALTH, 20);
		builder = builder.add(Attributes.ARMOR, 2);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 5);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		builder = builder.add(Attributes.STEP_HEIGHT, 0.6);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
		return builder;
	}
}