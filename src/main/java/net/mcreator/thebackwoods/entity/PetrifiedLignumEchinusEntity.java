package net.mcreator.thebackwoods.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.procedures.PretrifiedLignumEchinusNaturalEntitySpawningConditionProcedure;
import net.mcreator.thebackwoods.procedures.LignumEchinusPlayerCollidesWithThisEntityProcedure;
import net.mcreator.thebackwoods.procedures.LignumEchinusEntityIsHurtProcedure;
import net.mcreator.thebackwoods.procedures.EchinusAvoidConditionProcedure;
import net.mcreator.thebackwoods.procedures.EchinusAttackConditionProcedure;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

public class PetrifiedLignumEchinusEntity extends Monster {
	public PetrifiedLignumEchinusEntity(EntityType<PetrifiedLignumEchinusEntity> type, Level world) {
		super(type, world);
		xpReward = 6;
		setNoAi(false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
			@Override
			protected boolean canPerformAttack(LivingEntity entity) {
				return this.isTimeToAttack() && this.mob.distanceToSqr(entity) < (this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth()) && this.mob.getSensing().hasLineOfSight(entity);
			}

			@Override
			public boolean canUse() {
				double x = PetrifiedLignumEchinusEntity.this.getX();
				double y = PetrifiedLignumEchinusEntity.this.getY();
				double z = PetrifiedLignumEchinusEntity.this.getZ();
				Entity entity = PetrifiedLignumEchinusEntity.this;
				Level world = PetrifiedLignumEchinusEntity.this.level();
				return super.canUse() && EchinusAttackConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumEchinusEntity.this.getX();
				double y = PetrifiedLignumEchinusEntity.this.getY();
				double z = PetrifiedLignumEchinusEntity.this.getZ();
				Entity entity = PetrifiedLignumEchinusEntity.this;
				Level world = PetrifiedLignumEchinusEntity.this.level();
				return super.canContinueToUse() && EchinusAttackConditionProcedure.execute(world, entity);
			}

		});
		this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, (float) 6, 1.4, 1.1) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumEchinusEntity.this.getX();
				double y = PetrifiedLignumEchinusEntity.this.getY();
				double z = PetrifiedLignumEchinusEntity.this.getZ();
				Entity entity = PetrifiedLignumEchinusEntity.this;
				Level world = PetrifiedLignumEchinusEntity.this.level();
				return super.canUse() && EchinusAvoidConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumEchinusEntity.this.getX();
				double y = PetrifiedLignumEchinusEntity.this.getY();
				double z = PetrifiedLignumEchinusEntity.this.getZ();
				Entity entity = PetrifiedLignumEchinusEntity.this;
				Level world = PetrifiedLignumEchinusEntity.this.level();
				return super.canContinueToUse() && EchinusAvoidConditionProcedure.execute(world, entity);
			}
		});
		this.targetSelector.addGoal(4, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumEchinusEntity.this.getX();
				double y = PetrifiedLignumEchinusEntity.this.getY();
				double z = PetrifiedLignumEchinusEntity.this.getZ();
				Entity entity = PetrifiedLignumEchinusEntity.this;
				Level world = PetrifiedLignumEchinusEntity.this.level();
				return super.canUse() && EchinusAttackConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumEchinusEntity.this.getX();
				double y = PetrifiedLignumEchinusEntity.this.getY();
				double z = PetrifiedLignumEchinusEntity.this.getZ();
				Entity entity = PetrifiedLignumEchinusEntity.this;
				Level world = PetrifiedLignumEchinusEntity.this.level();
				return super.canContinueToUse() && EchinusAttackConditionProcedure.execute(world, entity);
			}
		}.setAlertOthers());
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.7) {
			@Override
			public boolean canUse() {
				double x = PetrifiedLignumEchinusEntity.this.getX();
				double y = PetrifiedLignumEchinusEntity.this.getY();
				double z = PetrifiedLignumEchinusEntity.this.getZ();
				Entity entity = PetrifiedLignumEchinusEntity.this;
				Level world = PetrifiedLignumEchinusEntity.this.level();
				return super.canUse() && EchinusAvoidConditionProcedure.execute(world, entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = PetrifiedLignumEchinusEntity.this.getX();
				double y = PetrifiedLignumEchinusEntity.this.getY();
				double z = PetrifiedLignumEchinusEntity.this.getZ();
				Entity entity = PetrifiedLignumEchinusEntity.this;
				Level world = PetrifiedLignumEchinusEntity.this.level();
				return super.canContinueToUse() && EchinusAvoidConditionProcedure.execute(world, entity);
			}
		});
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
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

		LignumEchinusEntityIsHurtProcedure.execute(world, sourceentity);
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
		LignumEchinusPlayerCollidesWithThisEntityProcedure.execute(this.level(), this, sourceentity);
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
		event.register(TheBackwoodsModEntities.PETRIFIED_LIGNUM_ECHINUS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (entityType, world, reason, pos, random) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return PretrifiedLignumEchinusNaturalEntitySpawningConditionProcedure.execute(world, x, y, z);
		}, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.38);
		builder = builder.add(Attributes.MAX_HEALTH, 32);
		builder = builder.add(Attributes.ARMOR, 10);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 7);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		builder = builder.add(Attributes.STEP_HEIGHT, 1);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
		return builder;
	}
}