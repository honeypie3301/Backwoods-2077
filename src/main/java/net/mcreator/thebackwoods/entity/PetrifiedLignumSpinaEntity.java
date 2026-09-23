package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
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

import net.mcreator.thebackwoods.procedures.SpinaAvoidConditionProcedure;
import net.mcreator.thebackwoods.procedures.SpinaAttackConditionProcedure;
import net.mcreator.thebackwoods.procedures.PretrifiedLignumSpinaNaturalEntitySpawningConditionProcedure;
import net.mcreator.thebackwoods.procedures.PretrifiedLignumSpinaEntityIsHurtProcedure;
import net.mcreator.thebackwoods.procedures.LignumSpinaPlayerCollidesWithThisEntityProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

public class PetrifiedLignumSpinaEntity extends PathfinderMob {
	public PetrifiedLignumSpinaEntity(EntityType<PetrifiedLignumSpinaEntity> type, Level world) {
		super(type, world);
		xpReward = 5;
		setNoAi(false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumSpinaEntity.this.getX();
				double y = PetrifiedLignumSpinaEntity.this.getY();
				double z = PetrifiedLignumSpinaEntity.this.getZ();
				Entity entity = PetrifiedLignumSpinaEntity.this;
				Level world = PetrifiedLignumSpinaEntity.this.level();
				return super.canUse() && SpinaAttackConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumSpinaEntity.this.getX();
				double y = PetrifiedLignumSpinaEntity.this.getY();
				double z = PetrifiedLignumSpinaEntity.this.getZ();
				Entity entity = PetrifiedLignumSpinaEntity.this;
				Level world = PetrifiedLignumSpinaEntity.this.level();
				return super.canContinueToUse() && SpinaAttackConditionProcedure.execute(world, entity);
			}
		}.setAlertOthers());
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
			@Override
			protected boolean canPerformAttack(LivingEntity entity) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(entity) < (this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth()) && this.mob.getSensing().hasLineOfSight(entity);
			}

			@Override
			public boolean canUse() {
				double x = PetrifiedLignumSpinaEntity.this.getX();
				double y = PetrifiedLignumSpinaEntity.this.getY();
				double z = PetrifiedLignumSpinaEntity.this.getZ();
				Entity entity = PetrifiedLignumSpinaEntity.this;
				Level world = PetrifiedLignumSpinaEntity.this.level();
				return super.canUse() && SpinaAttackConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumSpinaEntity.this.getX();
				double y = PetrifiedLignumSpinaEntity.this.getY();
				double z = PetrifiedLignumSpinaEntity.this.getZ();
				Entity entity = PetrifiedLignumSpinaEntity.this;
				Level world = PetrifiedLignumSpinaEntity.this.level();
				return super.canContinueToUse() && SpinaAttackConditionProcedure.execute(world, entity);
			}

		});
		this.goalSelector.addGoal(3, new FloatGoal(this));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new AvoidEntityGoal<>(this, Player.class, (float) 6, 2, 1.2) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumSpinaEntity.this.getX();
				double y = PetrifiedLignumSpinaEntity.this.getY();
				double z = PetrifiedLignumSpinaEntity.this.getZ();
				Entity entity = PetrifiedLignumSpinaEntity.this;
				Level world = PetrifiedLignumSpinaEntity.this.level();
				return super.canUse() && SpinaAvoidConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumSpinaEntity.this.getX();
				double y = PetrifiedLignumSpinaEntity.this.getY();
				double z = PetrifiedLignumSpinaEntity.this.getZ();
				Entity entity = PetrifiedLignumSpinaEntity.this;
				Level world = PetrifiedLignumSpinaEntity.this.level();
				return super.canContinueToUse() && SpinaAvoidConditionProcedure.execute(world, entity);
			}
		});
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, (float) 6));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Mob.class, (float) 12));
	}

	@Override
	public SoundEvent getAmbientSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:vermis_idle"));
	}

	@Override
	public void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:vermis_step")), 0.15f, 1);
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:splinter_hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("the_backwoods:vermis_death"));
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

		PretrifiedLignumSpinaEntityIsHurtProcedure.execute(world, sourceentity);
		if (damagesource.is(DamageTypes.FALL))
			return false;
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DROWN))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public void playerTouch(Player sourceentity) {
		super.playerTouch(sourceentity);
		LignumSpinaPlayerCollidesWithThisEntityProcedure.execute(this.level(), this, sourceentity);
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
		event.register(TheBackwoodsModEntities.PETRIFIED_LIGNUM_SPINA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return PretrifiedLignumSpinaNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.26);
		builder = builder.add(Attributes.MAX_HEALTH, 28);
		builder = builder.add(Attributes.ARMOR, 8);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 6);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		builder = builder.add(Attributes.STEP_HEIGHT, 1);
		return builder;
	}
}