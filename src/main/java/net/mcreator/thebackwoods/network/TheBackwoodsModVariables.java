package net.mcreator.thebackwoods.network;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

import net.mcreator.thebackwoods.TheBackwoodsMod;

import java.util.function.Supplier;

@EventBusSubscriber
public class TheBackwoodsModVariables {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TheBackwoodsMod.MODID);
	public static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES = ATTACHMENT_TYPES.register("player_variables", () -> AttachmentType.serializable(() -> new PlayerVariables()).build());

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		TheBackwoodsMod.addNetworkMessage(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handleData);
	}

	@SubscribeEvent
	public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
	}

	@SubscribeEvent
	public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
	}

	@SubscribeEvent
	public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
	}

	@SubscribeEvent
	public static void onPlayerTickUpdateSyncPlayerVariables(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player && player.getData(PLAYER_VARIABLES)._syncDirty) {
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
			player.getData(PLAYER_VARIABLES)._syncDirty = false;
		}
	}

	@SubscribeEvent
	public static void clonePlayer(PlayerEvent.Clone event) {
		PlayerVariables original = event.getOriginal().getData(PLAYER_VARIABLES);
		PlayerVariables clone = new PlayerVariables();
		clone.music_stopped_s2 = original.music_stopped_s2;
		clone.music_stopped_s3 = original.music_stopped_s3;
		clone.music_stopped_s4 = original.music_stopped_s4;
		clone.dayIncrementedToday = original.dayIncrementedToday;
		clone.daysInFamiliar = original.daysInFamiliar;
		clone.lossProgress = original.lossProgress;
		clone.lossTimer = original.lossTimer;
		clone.backwoods_adaptation = original.backwoods_adaptation;
		clone.fade_level = original.fade_level;
		if (!event.isWasDeath()) {
		}
		event.getEntity().setData(PLAYER_VARIABLES, clone);
	}

	public static class PlayerVariables implements INBTSerializable<CompoundTag> {
		boolean _syncDirty = false;
		public double music_stopped_s2 = 0;
		public double music_stopped_s3 = 0;
		public double music_stopped_s4 = 0;
		public boolean dayIncrementedToday = false;
		public double daysInFamiliar = 0;
		public double lossProgress = 0.0;
		public double lossTimer = 0;
		public double backwoods_adaptation = 0;
		public double fade_level = 0;

		@Override
		public CompoundTag serializeNBT(HolderLookup.Provider lookupProvider) {
			CompoundTag nbt = new CompoundTag();
			nbt.putDouble("music_stopped_s2", music_stopped_s2);
			nbt.putDouble("music_stopped_s3", music_stopped_s3);
			nbt.putDouble("music_stopped_s4", music_stopped_s4);
			nbt.putBoolean("dayIncrementedToday", dayIncrementedToday);
			nbt.putDouble("daysInFamiliar", daysInFamiliar);
			nbt.putDouble("lossProgress", lossProgress);
			nbt.putDouble("lossTimer", lossTimer);
			nbt.putDouble("backwoods_adaptation", backwoods_adaptation);
			nbt.putDouble("fade_level", fade_level);
			return nbt;
		}

		@Override
		public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
			music_stopped_s2 = nbt.getDouble("music_stopped_s2");
			music_stopped_s3 = nbt.getDouble("music_stopped_s3");
			music_stopped_s4 = nbt.getDouble("music_stopped_s4");
			dayIncrementedToday = nbt.getBoolean("dayIncrementedToday");
			daysInFamiliar = nbt.getDouble("daysInFamiliar");
			lossProgress = nbt.getDouble("lossProgress");
			lossTimer = nbt.getDouble("lossTimer");
			backwoods_adaptation = nbt.getDouble("backwoods_adaptation");
			fade_level = nbt.getDouble("fade_level");
		}

		public void markSyncDirty() {
			_syncDirty = true;
		}
	}

	public record PlayerVariablesSyncMessage(PlayerVariables data) implements CustomPacketPayload {
		public static final Type<PlayerVariablesSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TheBackwoodsMod.MODID, "player_variables_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec
				.of((RegistryFriendlyByteBuf buffer, PlayerVariablesSyncMessage message) -> buffer.writeNbt(message.data().serializeNBT(buffer.registryAccess())), (RegistryFriendlyByteBuf buffer) -> {
					PlayerVariablesSyncMessage message = new PlayerVariablesSyncMessage(new PlayerVariables());
					message.data.deserializeNBT(buffer.registryAccess(), buffer.readNbt());
					return message;
				});

		@Override
		public Type<PlayerVariablesSyncMessage> type() {
			return TYPE;
		}

		public static void handleData(final PlayerVariablesSyncMessage message, final IPayloadContext context) {
			if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
				context.enqueueWork(() -> context.player().getData(PLAYER_VARIABLES).deserializeNBT(context.player().registryAccess(), message.data.serializeNBT(context.player().registryAccess()))).exceptionally(e -> {
					context.connection().disconnect(Component.literal(e.getMessage()));
					return null;
				});
			}
		}
	}
}