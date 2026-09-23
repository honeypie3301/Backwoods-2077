/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.thebackwoods.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.Registries;

import net.mcreator.thebackwoods.entity.*;
import net.mcreator.thebackwoods.TheBackwoodsMod;

@EventBusSubscriber
public class TheBackwoodsModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, TheBackwoodsMod.MODID);
	public static final DeferredHolder<EntityType<?>, EntityType<SplinterEntity>> SPLINTER = register("splinter",
			EntityType.Builder.<SplinterEntity>of(SplinterEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.ridingOffset(-0.6f).sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<HollowEntity>> HOLLOW = register("hollow",
			EntityType.Builder.<HollowEntity>of(HollowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.ridingOffset(-0.6f).sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<LogSplinterEntity>> LOG_SPLINTER = register("log_splinter",
			EntityType.Builder.<LogSplinterEntity>of(LogSplinterEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.ridingOffset(-0.6f).sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<AshWeaverEntity>> ASH_WEAVER = register("ash_weaver",
			EntityType.Builder.<AshWeaverEntity>of(AshWeaverEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.ridingOffset(-0.6f).sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<RotEntity>> ROT = register("rot", EntityType.Builder.<RotEntity>of(RotEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)

			.sized(0.6f, 2f));
	public static final DeferredHolder<EntityType<?>, EntityType<BlindspotSplinterEntity>> BLINDSPOT_SPLINTER = register("blindspot_splinter",
			EntityType.Builder.<BlindspotSplinterEntity>of(BlindspotSplinterEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(32).setUpdateInterval(3)

					.ridingOffset(-0.6f).sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<PetrifiedLogSplinterEntity>> PETRIFIED_LOG_SPLINTER = register("petrified_log_splinter",
			EntityType.Builder.<PetrifiedLogSplinterEntity>of(PetrifiedLogSplinterEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune().ridingOffset(-0.6f).sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<StiltWalkerEntity>> STILT_WALKER = register("stilt_walker",
			EntityType.Builder.<StiltWalkerEntity>of(StiltWalkerEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 3.4f));
	public static final DeferredHolder<EntityType<?>, EntityType<LignumGigasEntity>> LIGNUM_GIGAS = register("lignum_gigas",
			EntityType.Builder.<LignumGigasEntity>of(LignumGigasEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 2.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<LignumVermisEntity>> LIGNUM_VERMIS = register("lignum_vermis",
			EntityType.Builder.<LignumVermisEntity>of(LignumVermisEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(48).setUpdateInterval(3)

					.sized(0.4f, 0.3f));
	public static final DeferredHolder<EntityType<?>, EntityType<FractusEntity>> FRACTUS = register("fractus",
			EntityType.Builder.<FractusEntity>of(FractusEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1f, 1f));
	public static final DeferredHolder<EntityType<?>, EntityType<FractusPrimeEntity>> FRACTUS_PRIME = register("fractus_prime",
			EntityType.Builder.<FractusPrimeEntity>of(FractusPrimeEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune()

					.sized(1f, 1f));
	public static final DeferredHolder<EntityType<?>, EntityType<LignumPalusEntity>> LIGNUM_PALUS = register("lignum_palus",
			EntityType.Builder.<LignumPalusEntity>of(LignumPalusEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(48).setUpdateInterval(3)

					.sized(0.4f, 5.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<LignumSpinaEntity>> LIGNUM_SPINA = register("lignum_spina",
			EntityType.Builder.<LignumSpinaEntity>of(LignumSpinaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 0.4f));
	public static final DeferredHolder<EntityType<?>, EntityType<LignumTrilobitaEntity>> LIGNUM_TRILOBITA = register("lignum_trilobita",
			EntityType.Builder.<LignumTrilobitaEntity>of(LignumTrilobitaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 0.3f));
	public static final DeferredHolder<EntityType<?>, EntityType<LignumEchinusEntity>> LIGNUM_ECHINUS = register("lignum_echinus",
			EntityType.Builder.<LignumEchinusEntity>of(LignumEchinusEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1f, 1f));
	public static final DeferredHolder<EntityType<?>, EntityType<PetrifiedLignumSpinaEntity>> PETRIFIED_LIGNUM_SPINA = register("petrified_lignum_spina",
			EntityType.Builder.<PetrifiedLignumSpinaEntity>of(PetrifiedLignumSpinaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 0.4f));
	public static final DeferredHolder<EntityType<?>, EntityType<PetrifiedLignumTrilobitaEntity>> PETRIFIED_LIGNUM_TRILOBITA = register("petrified_lignum_trilobita",
			EntityType.Builder.<PetrifiedLignumTrilobitaEntity>of(PetrifiedLignumTrilobitaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 0.3f));
	public static final DeferredHolder<EntityType<?>, EntityType<PetrifiedLignumEchinusEntity>> PETRIFIED_LIGNUM_ECHINUS = register("petrified_lignum_echinus",
			EntityType.Builder.<PetrifiedLignumEchinusEntity>of(PetrifiedLignumEchinusEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1f, 1f));
	public static final DeferredHolder<EntityType<?>, EntityType<DorcelessSplinterEntity>> DORCELESS_SPLINTER = register("dorceless_splinter",
			EntityType.Builder.<DorcelessSplinterEntity>of(DorcelessSplinterEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<TetherlessPearlProjectileEntity>> TETHERLESS_PEARL_PROJECTILE = register("tetherless_pearl_projectile",
			EntityType.Builder.<TetherlessPearlProjectileEntity>of(TetherlessPearlProjectileEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<KyneSplinterEntity>> KYNE_SPLINTER = register("kyne_splinter",
			EntityType.Builder.<KyneSplinterEntity>of(KyneSplinterEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<ResonantTetherlessPearlProjectileEntity>> RESONANT_TETHERLESS_PEARL_PROJECTILE = register("resonant_tetherless_pearl_projectile",
			EntityType.Builder.<ResonantTetherlessPearlProjectileEntity>of(ResonantTetherlessPearlProjectileEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<WoodweaverEntity>> WOODWEAVER = register("woodweaver",
			EntityType.Builder.<WoodweaverEntity>of(WoodweaverEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).fireImmune()

					.sized(2f, 6f));
	public static final DeferredHolder<EntityType<?>, EntityType<PetrifiedOakGallProjectileEntity>> PETRIFIED_OAK_GALL_PROJECTILE = register("petrified_oak_gall_projectile",
			EntityType.Builder.<PetrifiedOakGallProjectileEntity>of(PetrifiedOakGallProjectileEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final DeferredHolder<EntityType<?>, EntityType<HewnSplinterEntity>> HEWN_SPLINTER = register("hewn_splinter",
			EntityType.Builder.<HewnSplinterEntity>of(HewnSplinterEntity::new, MobCategory.AMBIENT).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(1f, 1f));
	public static final DeferredHolder<EntityType<?>, EntityType<VerdantEngineEntity>> VERDANT_ENGINE = register("verdant_engine",
			EntityType.Builder.<VerdantEngineEntity>of(VerdantEngineEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(1024).setUpdateInterval(3).fireImmune()

					.sized(1f, 1f));

	// Start of user code block custom entities
	// End of user code block custom entities
	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(RegisterSpawnPlacementsEvent event) {
		SplinterEntity.init(event);
		HollowEntity.init(event);
		LogSplinterEntity.init(event);
		AshWeaverEntity.init(event);
		RotEntity.init(event);
		BlindspotSplinterEntity.init(event);
		PetrifiedLogSplinterEntity.init(event);
		StiltWalkerEntity.init(event);
		LignumGigasEntity.init(event);
		LignumVermisEntity.init(event);
		FractusEntity.init(event);
		FractusPrimeEntity.init(event);
		LignumPalusEntity.init(event);
		LignumSpinaEntity.init(event);
		LignumTrilobitaEntity.init(event);
		LignumEchinusEntity.init(event);
		PetrifiedLignumSpinaEntity.init(event);
		PetrifiedLignumTrilobitaEntity.init(event);
		PetrifiedLignumEchinusEntity.init(event);
		DorcelessSplinterEntity.init(event);
		KyneSplinterEntity.init(event);
		WoodweaverEntity.init(event);
		HewnSplinterEntity.init(event);
		VerdantEngineEntity.init(event);
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(SPLINTER.get(), SplinterEntity.createAttributes().build());
		event.put(HOLLOW.get(), HollowEntity.createAttributes().build());
		event.put(LOG_SPLINTER.get(), LogSplinterEntity.createAttributes().build());
		event.put(ASH_WEAVER.get(), AshWeaverEntity.createAttributes().build());
		event.put(ROT.get(), RotEntity.createAttributes().build());
		event.put(BLINDSPOT_SPLINTER.get(), BlindspotSplinterEntity.createAttributes().build());
		event.put(PETRIFIED_LOG_SPLINTER.get(), PetrifiedLogSplinterEntity.createAttributes().build());
		event.put(STILT_WALKER.get(), StiltWalkerEntity.createAttributes().build());
		event.put(LIGNUM_GIGAS.get(), LignumGigasEntity.createAttributes().build());
		event.put(LIGNUM_VERMIS.get(), LignumVermisEntity.createAttributes().build());
		event.put(FRACTUS.get(), FractusEntity.createAttributes().build());
		event.put(FRACTUS_PRIME.get(), FractusPrimeEntity.createAttributes().build());
		event.put(LIGNUM_PALUS.get(), LignumPalusEntity.createAttributes().build());
		event.put(LIGNUM_SPINA.get(), LignumSpinaEntity.createAttributes().build());
		event.put(LIGNUM_TRILOBITA.get(), LignumTrilobitaEntity.createAttributes().build());
		event.put(LIGNUM_ECHINUS.get(), LignumEchinusEntity.createAttributes().build());
		event.put(PETRIFIED_LIGNUM_SPINA.get(), PetrifiedLignumSpinaEntity.createAttributes().build());
		event.put(PETRIFIED_LIGNUM_TRILOBITA.get(), PetrifiedLignumTrilobitaEntity.createAttributes().build());
		event.put(PETRIFIED_LIGNUM_ECHINUS.get(), PetrifiedLignumEchinusEntity.createAttributes().build());
		event.put(DORCELESS_SPLINTER.get(), DorcelessSplinterEntity.createAttributes().build());
		event.put(KYNE_SPLINTER.get(), KyneSplinterEntity.createAttributes().build());
		event.put(WOODWEAVER.get(), WoodweaverEntity.createAttributes().build());
		event.put(HEWN_SPLINTER.get(), HewnSplinterEntity.createAttributes().build());
		event.put(VERDANT_ENGINE.get(), VerdantEngineEntity.createAttributes().build());
	}
}