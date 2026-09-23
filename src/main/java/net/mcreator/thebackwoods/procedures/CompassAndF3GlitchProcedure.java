package net.mcreator.thebackwoods.procedures;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import java.util.List;

@EventBusSubscriber
public class CompassAndF3GlitchProcedure {

	public static boolean ENABLE_GLITCH = true;

	// Thread-safe triggers for client integration
	public static boolean clientIsNearby = false;
	public static double clientMinDistance = 24.0;
	public static double clientActiveIntensity = 0.0;
	private static int clientLastTick = -1;
	private static BlockPos originalSpawn = null;
	private static float originalSpawnAngle = 0.0f;

	private static double getEntityMultiplier(String className) {
		if (className.contains("LignumVermis") || className.contains("AshWeaver")) {
			return 0.15; // A LOT weaker
		}
		if (className.contains("Splinter")) {
			return 0.45; // Weaker
		}
		if (className.contains("FractusPrime") || className.contains("PrimeFractus") || className.contains("LignumGigas") || className.contains("Rot")) {
			return 1.8; // Stronger
		}
		return 1.0; // Normal for LignumPalus, StiltStalker, StiltWalker, regular Fractus, Hollow
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		if (event.getEntity() instanceof Player player) {
			if (player.level().isClientSide()) {
				tickClientSideGlitch(player);
			}
		}
	}

	private static void tickClientSideGlitch(Player player) {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.level == null)
			return;

		if (!ENABLE_GLITCH || player.isCreative() || player.isSpectator()) {
			if (originalSpawn != null) {
				mc.level.setDefaultSpawnPos(originalSpawn, originalSpawnAngle);
				originalSpawn = null;
			}
			clientIsNearby = false;
			clientActiveIntensity = 0.0;
			return;
		}

		int tickCount = player.tickCount;
		if (tickCount != clientLastTick) {
			clientLastTick = tickCount;

			// Scan creepy entities only once every 5 ticks to guarantee absolute zero footprint
			if (tickCount % 5 == 0) {
				clientIsNearby = false;
				clientMinDistance = 24.0;
				clientActiveIntensity = 0.0;
				List<Entity> surrounding = mc.level.getEntities(null, player.getBoundingBox().inflate(24));
				for (Entity e : surrounding) {
					String name = e.getClass().getName();
					if (name.contains("Splinter") || name.contains("LignumPalus") || name.contains("Rot") || name.contains("Fractus") || name.contains("Hollow") || name.contains("StiltStalker") || name.contains("StiltWalker") || name.contains("LignumVermis") || name.contains("AshWeaver") || name.contains("LignumGigas")) {
						double d = e.distanceTo(player);
						if (d < 24.0) {
							double mult = getEntityMultiplier(name);
							double intensity = mult * (1.0 - (d / 24.0));
							if (intensity > clientActiveIntensity) {
								clientActiveIntensity = intensity;
								clientMinDistance = d;
							}
							clientIsNearby = true;
						}
					}
				}
			}

			// Dynamic client-side compass needle glitching without changing NBT or converting to Lodestone Compass!
			if (clientIsNearby && clientActiveIntensity > 0.005) {
				// Initialize and store the real spawn position of the world once
				if (originalSpawn == null) {
					originalSpawn = mc.level.getSharedSpawnPos();
					originalSpawnAngle = mc.level.getSharedSpawnAngle();
				}

				double ratio = Math.max(0.0, Math.min(1.0, clientActiveIntensity));
				// Scale the angle velocity and position jitter relative to player proximity
				double angle = (tickCount * 0.4) + Math.random() * (ratio * 2.0 * Math.PI);
				double distance = 5.0 + (1.0 - ratio) * 200.0;
				double spawnX = player.getX() + Math.cos(angle) * distance;
				double spawnY = player.getY();
				double spawnZ = player.getZ() + Math.sin(angle) * distance;

				mc.level.setDefaultSpawnPos(new BlockPos((int) spawnX, (int) spawnY, (int) spawnZ), 0.0f);
			} else {
				// Re-align the world spawn position cleanly when creepy entities retreat
				if (originalSpawn != null) {
					mc.level.setDefaultSpawnPos(originalSpawn, originalSpawnAngle);
					originalSpawn = null;
				}
			}
		}
	}

	public static void execute() {
		// Empty signature to satisfy MCreator's procedure call if hooked up in GUI
	}
}

@EventBusSubscriber(value = Dist.CLIENT)
class ClientCompassAndF3GlitchSubscriber {

	private static boolean isBlank(String str) {
		if (str == null) return true;
		int len = str.length();
		if (len == 0) return true;
		for (int i = 0; i < len; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean containsIgnoreCase(String src, String search) {
		if (src == null || search == null) return false;
		final int length = search.length();
		if (length == 0) return true;
		for (int i = src.length() - length; i >= 0; i--) {
			if (src.regionMatches(true, i, search, 0, length)) {
				return true;
			}
		}
		return false;
	}

	private static String fastStripColor(String input) {
		if (input == null) return "";
		int len = input.length();
		if (len == 0) return "";
		int firstSection = input.indexOf('\u00A7');
		if (firstSection == -1) {
			return input;
		}
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = input.charAt(i);
			if (c == '\u00A7' && i + 1 < len) {
				i++; // Skip the § and the following format code
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	@SubscribeEvent
	public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
		if (!CompassAndF3GlitchProcedure.ENABLE_GLITCH) {
			return;
		}

		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.player == null || mc.player.level() == null || mc.player.isCreative() || mc.player.isSpectator())
			return;

		if (CompassAndF3GlitchProcedure.clientIsNearby && CompassAndF3GlitchProcedure.clientActiveIntensity > 0.005) {
			double ratio = Math.max(0.0, Math.min(1.0, CompassAndF3GlitchProcedure.clientActiveIntensity));

			// Scramble coordinates and game orientation overlay fields on F3 (Beautiful raw white obfuscation)
			List<String> left = event.getLeft();
			for (int i = 0; i < left.size(); i++) {
				String line = left.get(i);
				if (isBlank(line))
					continue;

				if (line.startsWith("§f§k")) {
					continue;
				}

				boolean shouldScramble = false;
				if (ratio >= 0.8) {
					shouldScramble = true; // Extreme proximity: scramble everything
				} else if (ratio >= 0.4) {
					if (line.startsWith("XYZ:") || line.startsWith("Block:") || line.startsWith("Chunk:") || line.startsWith("Facing:") || line.startsWith("Biome:") || line.startsWith("E:") || containsIgnoreCase(line, "fps")) {
						shouldScramble = true;
					}
				} else {
					if (line.startsWith("Facing:") || line.startsWith("Biome:") || line.startsWith("E:")) {
						shouldScramble = true;
					}
				}

				if (shouldScramble && Math.random() < ratio) {
					String clean = fastStripColor(line);
					left.set(i, "§f§k" + clean);
				}
			}

			// Scramble specs, server metrics and hardware allocations overlay fields depending on entity proximity
			List<String> right = event.getRight();
			for (int i = 0; i < right.size(); i++) {
				String line = right.get(i);
				if (isBlank(line))
					continue;

				if (line.startsWith("§f§k")) {
					continue;
				}

				boolean shouldScramble = false;
				if (ratio >= 0.8) {
					shouldScramble = true; // Extreme proximity: scramble everything
				} else if (ratio >= 0.4) {
					if (line.contains("CPU:") || line.contains("Display:") || line.contains("Server:") || line.contains("Client:") || line.contains("Mem:") || containsIgnoreCase(line, "nvidia") || containsIgnoreCase(line, "intel") || containsIgnoreCase(line, "amd")) {
						shouldScramble = true;
					}
				} else {
					if (line.contains("Server:") || line.contains("Client:")) {
						shouldScramble = true;
					}
				}

				if (shouldScramble && Math.random() < ratio) {
					String clean = fastStripColor(line);
					right.set(i, "§f§k" + clean);
				}
			}
		}
	} // 1.21.1
}
