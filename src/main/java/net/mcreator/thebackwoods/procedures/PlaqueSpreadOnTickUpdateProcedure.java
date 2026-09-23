package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import java.util.Comparator;

public class PlaqueSpreadOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		RandomSource random = RandomSource.create();

		// Pick ONE random direction (original behavior)
		int dir = Mth.nextInt(random, 0, 5);

		BlockPos target = switch (dir) {
			case 0 -> BlockPos.containing(x + 1, y, z);
			case 1 -> BlockPos.containing(x - 1, y, z);
			case 2 -> BlockPos.containing(x, y + 1, z);
			case 3 -> BlockPos.containing(x, y - 1, z);
			case 4 -> BlockPos.containing(x, y, z + 1);
			default -> BlockPos.containing(x, y, z - 1);
		};

		// Default spread speed
		int spreadDelay = 8;

		// Find nearest player and check void_time
		Entity nearest = world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3(x, y, z), 256, 256, 256), e -> true)
				.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z))).orElse(null);

		if (nearest instanceof Player player) {
			double voidTime = player.getPersistentData().getDouble("void_time");
			boolean bellPlayed = player.getPersistentData().getBoolean("void_time_bell_played");

			if (voidTime >= 12000) {
				spreadDelay = 5;

				// Play bell ONCE at PLAYER position
				if (!bellPlayed) {
					if (world instanceof Level level) {
						if (!level.isClientSide()) {
							BlockPos p = BlockPos.containing(player.getX(), player.getY(), player.getZ());
							level.playSound(
									null,
									p,
									BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bell.resonate")),
									SoundSource.PLAYERS,
									1.8f,
									0.9f
							);
						}
					}
					player.getPersistentData().putBoolean("void_time_bell_played", true);
				}
			} else {
				// Reset flag if below threshold so it can play again next time threshold is reached
				if (bellPlayed) {
					player.getPersistentData().putBoolean("void_time_bell_played", false);
				}
			}
		}

		// Only spread if it's not air and not already plaque/plaque heart
		if (!world.getBlockState(target).isAir()
				&& world.getBlockState(target).getBlock() != TheBackwoodsModBlocks.PLAQUE.get()
				&& world.getBlockState(target).getBlock() != TheBackwoodsModBlocks.PLAQUE_HEART.get()) {

			world.setBlock(target, TheBackwoodsModBlocks.PLAQUE.get().defaultBlockState(), 3);
			world.scheduleTick(target, TheBackwoodsModBlocks.PLAQUE.get(), spreadDelay);
		} // 1.21.1
	}
}