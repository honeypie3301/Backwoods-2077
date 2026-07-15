package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public class GrainTorchFadeProcedure {

	// ---------- TUNABLE ----------
	private static final int CHECK_INTERVAL_TICKS = 15;
	private static final double DISAPPEAR_CHANCE = 0.06;
	private static final double WATCH_DOT_THRESHOLD = 0.70;
	private static final int MAX_TRACKED_TORCHES = 256;
	// -----------------------------

	private static final String NBT_LIST_KEY = "grain_tracked_torches";

	private static final ResourceKey<Level> THE_GRAIN = ResourceKey.create(
		Registries.DIMENSION,
		ResourceLocation.parse("the_backwoods:the_grain")
	);

	private static final ResourceKey<Level> THE_WEALD = ResourceKey.create(
		Registries.DIMENSION,
		ResourceLocation.parse("the_backwoods:the_petrified_weald")
	);

	private static boolean isInAntiTorchDimension(Level level) {
		ResourceKey<Level> dim = level.dimension();
		return dim.equals(THE_GRAIN) || dim.equals(THE_WEALD);
	}

	private static String getNbtListKey(Level level) {
		return level.dimension().equals(THE_WEALD) ? "petrified_tracked_torches" : "grain_tracked_torches";
	}

	private static int getCheckInterval(Level level) {
		return level.dimension().equals(THE_WEALD) ? 10 : 15;
	}

	private static double getDisappearChance(Level level) {
		return level.dimension().equals(THE_WEALD) ? 0.07 : 0.06;
	}

	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		Entity e = event.getEntity();
		if (!(e instanceof Player player))
			return;

		Level level = player.level();
		if (level.isClientSide())
			return;

		if (!isInAntiTorchDimension(level))
			return;

		BlockState placed = event.getPlacedBlock();
		if (!isTorch(placed))
			return;

		BlockPos pos = event.getPos();
		addTrackedTorch(player, pos);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player == null)
			return;

		Level level = player.level();
		if (level.isClientSide())
			return;

		if (!isInAntiTorchDimension(level))
			return;

		if (player.tickCount % getCheckInterval(level) != 0)
			return;

		List<BlockPos> tracked = getTrackedTorches(player);
		if (tracked.isEmpty())
			return;

		List<BlockPos> survivors = new ArrayList<>(tracked.size());

		for (BlockPos pos : tracked) {
			BlockState state = level.getBlockState(pos);

			// Drop from tracking if torch no longer exists.
			if (!isTorch(state))
				continue;

			boolean watched = isPlayerWatchingTorch(level, player, pos);

			if (!watched && Math.random() < getDisappearChance(level)) {
				net.minecraft.world.level.block.Block unlit = getUnlitVersion(state);
				if (unlit == Blocks.AIR) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				} else {
					swapBlock(level, pos, state, unlit);
				}
				continue;
			}

			survivors.add(pos);
		}

		saveTrackedTorches(player, survivors);
	}

	private static boolean isTorch(BlockState state) {
		if (state.is(Blocks.TORCH)
				|| state.is(Blocks.WALL_TORCH)
				|| state.is(Blocks.SOUL_TORCH)
				|| state.is(Blocks.SOUL_WALL_TORCH)
				|| state.is(Blocks.REDSTONE_TORCH)
				|| state.is(Blocks.REDSTONE_WALL_TORCH)
				|| state.is(Blocks.LANTERN)
				|| state.is(Blocks.SOUL_LANTERN)
				|| state.is(Blocks.JACK_O_LANTERN)) {
			return true;
		}

		net.minecraft.world.level.block.Block block = state.getBlock();
		String regName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();

		if (regName.equals("totally_lit:glowstone_torch") || regName.equals("totally_lit:glowstone_wall_torch")) {
			return true;
		}

		if (net.neoforged.fml.ModList.get().isLoaded("hardcore_torches")) {
			if (regName.startsWith("hardcore_torches:lit_")) {
				return true;
			}
		}

		return false;
	}

	private static net.minecraft.world.level.block.Block getUnlitVersion(BlockState state) {
		if (state.is(Blocks.JACK_O_LANTERN)) {
			return Math.random() < 0.5 ? Blocks.CARVED_PUMPKIN : Blocks.PUMPKIN;
		}

		net.minecraft.world.level.block.Block block = state.getBlock();
		String regName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();

		// Determine if this is a wall-mounted torch
		boolean isWall = regName.contains("wall_torch") || regName.contains("wall_lantern")
				|| state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);

		if (net.neoforged.fml.ModList.get().isLoaded("totally_lit")) {
			if (regName.equals("minecraft:torch")) {
				return getBlockOrAir("totally_lit:unlit_torch");
			} else if (regName.equals("minecraft:wall_torch")) {
				return getBlockOrAir("totally_lit:unlit_wall_torch");
			} else if (regName.equals("minecraft:soul_torch")) {
				return getBlockOrAir("totally_lit:unlit_soul_torch");
			} else if (regName.equals("minecraft:soul_wall_torch")) {
				return getBlockOrAir("totally_lit:unlit_soul_wall_torch");
			} else if (regName.equals("minecraft:lantern")) {
				return getBlockOrAir("totally_lit:unlit_lantern");
			} else if (regName.equals("minecraft:soul_lantern")) {
				return getBlockOrAir("totally_lit:unlit_soul_lantern");
			} else if (regName.equals("totally_lit:glowstone_torch")) {
				return getBlockOrAir("totally_lit:unlit_torch");
			} else if (regName.equals("totally_lit:glowstone_wall_torch")) {
				return getBlockOrAir("totally_lit:unlit_wall_torch");
			}
		}

		if (net.neoforged.fml.ModList.get().isLoaded("hardcore_torches")) {
			if (regName.equals("minecraft:torch")) {
				return getBlockOrAir("hardcore_torches:unlit_torch");
			} else if (regName.equals("minecraft:wall_torch")) {
				return getBlockOrAir("hardcore_torches:unlit_wall_torch");
			} else if (regName.equals("minecraft:soul_torch")) {
				return getBlockOrAir("hardcore_torches:unlit_soul_torch");
			} else if (regName.equals("minecraft:soul_wall_torch")) {
				return getBlockOrAir("hardcore_torches:unlit_soul_wall_torch");
			} else if (regName.equals("minecraft:lantern")) {
				return getBlockOrAir("hardcore_torches:unlit_lantern");
			} else if (regName.equals("minecraft:soul_lantern")) {
				return getBlockOrAir("hardcore_torches:unlit_soul_lantern");
			}

			if (regName.startsWith("hardcore_torches:lit_")) {
				String base = regName.substring("hardcore_torches:lit_".length());
				// Preserve wall variant in the unlit name
				String unlitName = "hardcore_torches:unlit_" + base;
				net.minecraft.world.level.block.Block candidate = getBlockOrAir(unlitName);
				if (candidate != Blocks.AIR) return candidate;
			}
		}

		return Blocks.AIR;
	}

	private static net.minecraft.world.level.block.Block getBlockOrAir(String registryName) {
		net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.parse(registryName);
		if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(rl)) {
			return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(rl);
		}
		return Blocks.AIR;
	}

	private static void swapBlock(Level level, BlockPos pos, BlockState currentState, net.minecraft.world.level.block.Block newBlock) {
		BlockState newState = newBlock.defaultBlockState();
		for (net.minecraft.world.level.block.state.properties.Property<?> property : currentState.getProperties()) {
			if (newState.hasProperty(property)) {
				newState = copyProperty(currentState, newState, property);
			}
		}
		level.setBlock(pos, newState, 3);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> BlockState copyProperty(BlockState src, BlockState dest, net.minecraft.world.level.block.state.properties.Property<T> property) {
		return dest.setValue(property, src.getValue(property));
	}

	private static boolean isPlayerWatchingTorch(LevelAccessor world, Player player, BlockPos torchPos) {
		Vec3 eye = player.getEyePosition();
		Vec3 target = new Vec3(torchPos.getX() + 0.5, torchPos.getY() + 0.5, torchPos.getZ() + 0.5);

		Vec3 toTorch = target.subtract(eye).normalize();
		double dot = player.getLookAngle().normalize().dot(toTorch);
		boolean facingTorch = dot > WATCH_DOT_THRESHOLD;
		if (!facingTorch)
			return false;

		HitResult hit = world.clip(new ClipContext(
				eye,
				target,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player
		));

		if (hit.getType() == HitResult.Type.MISS)
			return true;

		if (hit.getType() == HitResult.Type.BLOCK) {
			BlockPos hitPos = BlockPos.containing(hit.getLocation());
			return hitPos.equals(torchPos);
		}

		return false;
	}

	private static void addTrackedTorch(Player player, BlockPos pos) {
		List<BlockPos> tracked = getTrackedTorches(player);

		for (BlockPos p : tracked) {
			if (p.equals(pos)) {
				return;
			}
		}

		tracked.add(pos);

		if (tracked.size() > MAX_TRACKED_TORCHES) {
			tracked = tracked.subList(tracked.size() - MAX_TRACKED_TORCHES, tracked.size());
		}

		saveTrackedTorches(player, tracked);
	}

	private static List<BlockPos> getTrackedTorches(Player player) {
		List<BlockPos> out = new ArrayList<>();
		CompoundTag data = player.getPersistentData();
		String listKey = getNbtListKey(player.level());

		if (!data.contains(listKey, Tag.TAG_LIST))
			return out;

		ListTag list = data.getList(listKey, Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++) {
			String s = list.getString(i);
			BlockPos p = parsePos(s);
			if (p != null) {
				out.add(p);
			}
		}

		return out;
	}

	private static void saveTrackedTorches(Player player, List<BlockPos> tracked) {
		ListTag list = new ListTag();
		for (BlockPos p : tracked) {
			list.add(StringTag.valueOf(encodePos(p)));
		}
		player.getPersistentData().put(getNbtListKey(player.level()), list);
	}

	private static String encodePos(BlockPos pos) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}

	private static BlockPos parsePos(String s) {
		try {
			String[] parts = s.split(",");
			if (parts.length != 3)
				return null;
			int x = Integer.parseInt(parts[0]);
			int y = Integer.parseInt(parts[1]);
			int z = Integer.parseInt(parts[2]);
			return new BlockPos(x, y, z);
		} catch (Exception ignored) {
			return null;
		} // 1.21.1
	}// harccore torches compat update
}