package net.mcreator.thebackwoods.procedures;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import java.util.List;

@EventBusSubscriber
public class ScandereSymbiosisAuraProcedure {
	private static final int CHECK_INTERVAL_TICKS = 40;
	private static final int LOG_SCAN_RADIUS = 16;
	private static final int LOG_SCAN_Y_DOWN = 5;
	private static final int LOG_SCAN_Y_UP = 6;
	private static final int MAX_LOGS_PER_PASS = 8;

	private static final int ANIMAL_SCAN_RADIUS = 24;
	private static final float ATTRACT_CHANCE = 0.45f;
	private static final double IDLE_RING_MIN = 2.5;
	private static final double IDLE_RING_MAX = 5.0;

	// Breeding settings
	private static final float BREED_CHANCE = 0.045f; // % per eligible check
	private static final int BREED_COOLDOWN_TICKS = 20 * 40;

	// Fertilization settings
	private static final float FERTILIZE_CHANCE = 0.18f;
	private static final int FERTILIZE_ATTEMPTS = 8;
	private static final int FERTILIZE_RADIUS_XZ = 3;
	private static final int FERTILIZE_RADIUS_Y_DOWN = 2;
	private static final int FERTILIZE_RADIUS_Y_UP = 1;

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		MinecraftServer server = event.getServer();
		if (server == null) return;
		if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

		for (ServerLevel level : server.getAllLevels()) {
			if (level.players().isEmpty()) continue;

			RandomSource random = level.getRandom();

			level.players().forEach(player -> {
				BlockPos p = player.blockPosition();
				int processedLogs = 0;

				for (int dx = -LOG_SCAN_RADIUS; dx <= LOG_SCAN_RADIUS && processedLogs < MAX_LOGS_PER_PASS; dx++) {
					for (int dz = -LOG_SCAN_RADIUS; dz <= LOG_SCAN_RADIUS && processedLogs < MAX_LOGS_PER_PASS; dz++) {
						for (int dy = -LOG_SCAN_Y_DOWN; dy <= LOG_SCAN_Y_UP && processedLogs < MAX_LOGS_PER_PASS; dy++) {
							BlockPos logPos = p.offset(dx, dy, dz);

							if (level.getBlockState(logPos).getBlock() != TheBackwoodsModBlocks.SCANDERE_LIGNUM_LOG.get()) continue;

							processedLogs++;
							influenceNearbyAnimals(level, logPos, random);
						}
					}
				}
			});
		}
	}

	private static void influenceNearbyAnimals(ServerLevel level, BlockPos logPos, RandomSource random) {
		AABB area = AABB.ofSize(
				new Vec3(logPos.getX() + 0.5, logPos.getY() + 0.5, logPos.getZ() + 0.5),
				ANIMAL_SCAN_RADIUS, ANIMAL_SCAN_RADIUS, ANIMAL_SCAN_RADIUS
		);

		List<Animal> animals = level.getEntitiesOfClass(Animal.class, area, a -> a.isAlive() && !a.isBaby());

		for (Animal animal : animals) {
			if (!(animal instanceof Mob mob)) continue;

			// Attraction / idle near log
			if (random.nextFloat() <= ATTRACT_CHANCE) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double r = Mth.nextDouble(random, IDLE_RING_MIN, IDLE_RING_MAX);
				double tx = logPos.getX() + 0.5 + Math.cos(angle) * r;
				double tz = logPos.getZ() + 0.5 + Math.sin(angle) * r;
				double ty = logPos.getY();

				mob.getNavigation().moveTo(tx, ty, tz, 0.75);

				if (random.nextFloat() < 0.16f) {
					mob.getNavigation().stop();
				}
			}

			// Passive breeding trigger with cooldown (1.21.8-safe Optional handling)
			int cd = animal.getPersistentData().getInt("scandere_breed_cd");
			if (cd > 0) {
				animal.getPersistentData().putInt("scandere_breed_cd", Math.max(0, cd - CHECK_INTERVAL_TICKS));
				continue;
			}

			// Only trigger if animal is not already in love and old enough
			if (!animal.isInLove() && animal.getAge() == 0 && random.nextFloat() < BREED_CHANCE) {
				animal.setInLove(null); // no player required
				animal.getPersistentData().putInt("scandere_breed_cd", BREED_COOLDOWN_TICKS);
			}

			// Animals fertilize nearby Lignum: extra scheduled ticks around the animal
			if (random.nextFloat() < FERTILIZE_CHANCE) {
				fertilizeNearbyLignum(level, animal.blockPosition(), random);
			}
		}
	}

	private static void fertilizeNearbyLignum(ServerLevel level, BlockPos center, RandomSource random) {
		for (int i = 0; i < FERTILIZE_ATTEMPTS; i++) {
			int dx = Mth.nextInt(random, -FERTILIZE_RADIUS_XZ, FERTILIZE_RADIUS_XZ);
			int dy = Mth.nextInt(random, -FERTILIZE_RADIUS_Y_DOWN, FERTILIZE_RADIUS_Y_UP);
			int dz = Mth.nextInt(random, -FERTILIZE_RADIUS_XZ, FERTILIZE_RADIUS_XZ);
			BlockPos p = center.offset(dx, dy, dz);

			if (level.getBlockState(p).getBlock() == TheBackwoodsModBlocks.SCANDERE_LIGNUM.get()) {
				level.scheduleTick(p, TheBackwoodsModBlocks.SCANDERE_LIGNUM.get(), 1 + random.nextInt(4));
			}
		}
	}
}