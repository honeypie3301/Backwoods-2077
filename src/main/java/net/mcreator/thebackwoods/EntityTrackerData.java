package net.mcreator.thebackwoods;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "the_backwoods", bus = EventBusSubscriber.Bus.GAME)
public class EntityTrackerData extends SavedData {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final String DATA_NAME = "the_backwoods_entity_tracker";
	public static final int DEFAULT_LOADER_RADIUS = 2;
	public static final int MAX_LOADER_TICKET_RADIUS = 3;
	public static int LOADER_TICKET_RADIUS = DEFAULT_LOADER_RADIUS;

	// In-memory per-dimension registry
	private final Map<UUID, TrackedEntry> trackedEntities = new ConcurrentHashMap<>();
	private final Map<UUID, Set<Long>> entityDesiredChunks = new ConcurrentHashMap<>();
	private final Map<Long, Set<UUID>> chunkOwners = new ConcurrentHashMap<>();

	public EntityTrackerData() {
	}

	public static final SavedData.Factory<EntityTrackerData> FACTORY = new SavedData.Factory<>(
		EntityTrackerData::new,
		EntityTrackerData::load,
		null
	);

	public static EntityTrackerData get(ServerLevel level) {
		if (level == null) return null;
		return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	public static void track(Entity entity, int ticketRadius, String typeTag) {
		if (entity != null && !entity.level().isClientSide() && entity.level() instanceof ServerLevel serverLevel) {
			track(serverLevel, entity.getUUID(), entity.blockPosition(), ticketRadius, typeTag);
		}
	}

	public static void track(ServerLevel level, UUID uuid, BlockPos pos, int ticketRadius, String typeTag) {
		if (level == null || uuid == null || pos == null) return;
		EntityTrackerData data = get(level);
		if (data != null) {
			data.trackEntry(level, uuid, pos, ticketRadius, typeTag);
		}
	}

	public static void updatePosition(Entity entity, int ticketRadius) {
		if (entity != null && !entity.level().isClientSide() && entity.level() instanceof ServerLevel serverLevel) {
			EntityTrackerData data = get(serverLevel);
			if (data != null && data.trackedEntities.containsKey(entity.getUUID())) {
				TrackedEntry old = data.trackedEntities.get(entity.getUUID());
				String typeTag = old != null ? old.typeTag : "entity";
				track(serverLevel, entity.getUUID(), entity.blockPosition(), ticketRadius, typeTag);
			}
		}
	}

	public static void untrack(Entity entity) {
		if (entity != null && !entity.level().isClientSide() && entity.level() instanceof ServerLevel serverLevel) {
			untrack(serverLevel, entity.getUUID());
		}
	}

	public static void untrack(ServerLevel level, UUID uuid) {
		if (level == null || uuid == null) return;
		EntityTrackerData data = get(level);
		if (data != null) {
			data.untrackEntry(level, uuid);
		}
	}

	public static boolean isTracked(ServerLevel level, UUID uuid) {
		if (level == null || uuid == null) return false;
		EntityTrackerData data = get(level);
		return data != null && data.trackedEntities.containsKey(uuid);
	}

	public Map<UUID, TrackedEntry> getTrackedEntities() {
		return Collections.unmodifiableMap(this.trackedEntities);
	}

	public static Set<Long> computeChunkSet(BlockPos pos, int radius) {
		Set<Long> chunks = new HashSet<>();
		int centerCx = pos.getX() >> 4;
		int centerCz = pos.getZ() >> 4;
		int r = Math.max(1, radius);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				chunks.add(ChunkPos.asLong(centerCx + dx, centerCz + dz));
			}
		}
		return chunks;
	}

	public synchronized void trackEntry(ServerLevel level, UUID uuid, BlockPos pos, int radius, String typeTag) {
		if (uuid == null || pos == null) return;
		int safeRadius = Math.min(MAX_LOADER_TICKET_RADIUS, Math.max(1, radius));
		TrackedEntry entry = new TrackedEntry(uuid, pos.immutable(), safeRadius, typeTag != null ? typeTag : "entity");
		this.trackedEntities.put(uuid, entry);
		this.setDirty();

		Set<Long> newDesired = computeChunkSet(pos, safeRadius);
		Set<Long> oldDesired = this.entityDesiredChunks.put(uuid, newDesired);
		if (oldDesired == null) {
			oldDesired = Collections.emptySet();
		}

		for (Long chunkKey : oldDesired) {
			if (!newDesired.contains(chunkKey)) {
				Set<UUID> owners = this.chunkOwners.get(chunkKey);
				if (owners != null) {
					owners.remove(uuid);
					if (owners.isEmpty()) {
						this.chunkOwners.remove(chunkKey);
						applyChunkForced(level, chunkKey, false, uuid);
					}
				}
			}
		}

		for (Long chunkKey : newDesired) {
			Set<UUID> owners = this.chunkOwners.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet());
			boolean wasEmpty = owners.isEmpty();
			owners.add(uuid);
			if (wasEmpty) {
				applyChunkForced(level, chunkKey, true, uuid);
			}
		}
	}

	public synchronized void untrackEntry(ServerLevel level, UUID uuid) {
		if (uuid == null) return;
		this.trackedEntities.remove(uuid);
		this.setDirty();

		Set<Long> oldDesired = this.entityDesiredChunks.remove(uuid);
		if (oldDesired != null) {
			for (Long chunkKey : oldDesired) {
				Set<UUID> owners = this.chunkOwners.get(chunkKey);
				if (owners != null) {
					owners.remove(uuid);
					if (owners.isEmpty()) {
						this.chunkOwners.remove(chunkKey);
						applyChunkForced(level, chunkKey, false, uuid);
					}
				}
			}
		}
	}

	public boolean isChunkOwned(int cx, int cz) {
		long key = ChunkPos.asLong(cx, cz);
		Set<UUID> owners = this.chunkOwners.get(key);
		return owners != null && !owners.isEmpty();
	}

	public static boolean isChunkOwnedByTracker(ServerLevel level, int cx, int cz) {
		if (level == null) return false;
		EntityTrackerData data = get(level);
		return data != null && data.isChunkOwned(cx, cz);
	}

	private static void applyChunkForced(ServerLevel level, long chunkKey, boolean forced, UUID uuid) {
		if (level == null) return;
		ChunkPos chunkPos = new ChunkPos(chunkKey);
		int cx = chunkPos.x;
		int cz = chunkPos.z;
		try {
			level.setChunkForced(cx, cz, forced);
		} catch (Exception e) {
			LOGGER.error("Failed to {} chunk [{}, {}] in dimension {} for entity {}: {}",
				forced ? "force" : "unforce", cx, cz, level.dimension().location(), uuid != null ? uuid : "system", e.getMessage(), e);
		}
	}

	public synchronized void reconcileAndValidateTickets(ServerLevel level) {
		if (level == null) return;

		Map<UUID, Set<Long>> rebuiltEntityDesired = new HashMap<>();
		Map<Long, Set<UUID>> rebuiltChunkOwners = new HashMap<>();

		for (TrackedEntry entry : this.trackedEntities.values()) {
			Set<Long> desired = computeChunkSet(entry.pos, entry.ticketRadius);
			rebuiltEntityDesired.put(entry.uuid, desired);
			for (Long chunkKey : desired) {
				rebuiltChunkOwners.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(entry.uuid);
			}
		}

		for (Long chunkKey : new HashSet<>(this.chunkOwners.keySet())) {
			if (!rebuiltChunkOwners.containsKey(chunkKey)) {
				this.chunkOwners.remove(chunkKey);
				applyChunkForced(level, chunkKey, false, null);
			}
		}

		this.entityDesiredChunks.clear();
		this.entityDesiredChunks.putAll(rebuiltEntityDesired);
		for (Map.Entry<Long, Set<UUID>> entry : rebuiltChunkOwners.entrySet()) {
			this.chunkOwners.computeIfAbsent(entry.getKey(), k -> ConcurrentHashMap.newKeySet()).addAll(entry.getValue());
		}

		Set<Long> forcedChunks = level.getForcedChunks();
		for (Long chunkKey : this.chunkOwners.keySet()) {
			if (!forcedChunks.contains(chunkKey)) {
				applyChunkForced(level, chunkKey, true, null);
			}
		}
	}

	@Override
	public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
		ListTag list = new ListTag();
		for (TrackedEntry entry : this.trackedEntities.values()) {
			CompoundTag entryTag = new CompoundTag();
			entryTag.putUUID("uuid", entry.uuid);
			entryTag.putInt("x", entry.pos.getX());
			entryTag.putInt("y", entry.pos.getY());
			entryTag.putInt("z", entry.pos.getZ());
			entryTag.putInt("radius", entry.ticketRadius);
			entryTag.putString("type", entry.typeTag);
			list.add(entryTag);
		}
		compoundTag.put("TrackedEntities", list);
		return compoundTag;
	}

	public static EntityTrackerData load(CompoundTag compoundTag, HolderLookup.Provider provider) {
		EntityTrackerData data = new EntityTrackerData();
		if (compoundTag.contains("TrackedEntities", Tag.TAG_LIST)) {
			ListTag list = compoundTag.getList("TrackedEntities", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag entryTag = list.getCompound(i);
				if (entryTag.hasUUID("uuid")) {
					UUID uuid = entryTag.getUUID("uuid");
					int x = entryTag.getInt("x");
					int y = entryTag.getInt("y");
					int z = entryTag.getInt("z");
					int radius = entryTag.contains("radius") ? entryTag.getInt("radius") : DEFAULT_LOADER_RADIUS;
					String type = entryTag.contains("type") ? entryTag.getString("type") : "entity";
					data.trackedEntities.put(uuid, new TrackedEntry(uuid, new BlockPos(x, y, z), radius, type));
				}
			}
		}
		return data;
	}

	public record TrackedEntry(UUID uuid, BlockPos pos, int ticketRadius, String typeTag) {
	}

	@SubscribeEvent
	public static void onLevelLoad(LevelEvent.Load event) {
		if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel) {
			EntityTrackerData data = EntityTrackerData.get(serverLevel);
			if (data != null) {
				data.reconcileAndValidateTickets(serverLevel);
			}
		}
	}

	@SubscribeEvent
	public static void onServerStarting(ServerStartingEvent event) {
		if (event.getServer() != null) {
			for (ServerLevel level : event.getServer().getAllLevels()) {
				EntityTrackerData data = EntityTrackerData.get(level);
				if (data != null) {
					data.reconcileAndValidateTickets(level);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel) {
			if (serverLevel.getGameTime() % 40 == 0) {
				EntityTrackerData data = EntityTrackerData.get(serverLevel);
				if (data != null && !data.getTrackedEntities().isEmpty()) {
					data.reconcileAndValidateTickets(serverLevel);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		Entity entity = event.getEntity();
		if (entity != null && !event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel level) {
			Entity.RemovalReason reason = entity.getRemovalReason();
			if (reason != null && (reason.shouldDestroy() || reason == Entity.RemovalReason.CHANGED_DIMENSION)) {
				EntityTrackerData.untrack(level, entity.getUUID());
			}
		}
	}
 // 1.21.1
}
