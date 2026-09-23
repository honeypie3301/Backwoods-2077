package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

public class RotDialoguesProcedure {

	public static boolean ENABLE_ROT_DIALOGUES = false;

	public static boolean isDialogueEnabled() {
		return ENABLE_ROT_DIALOGUES;
	}

	public static void setDialogueEnabled(boolean enabled) {
		ENABLE_ROT_DIALOGUES = enabled;
		RotEntityIsHurtProcedure.ENABLE_ROT_DIALOGUES = enabled;
	}

	private static final String[] WITHER_QUOTES = {
		"§7A false god of decay... I shall tear thy skulls apart.",
		"§7Your rose shall serve as fertilizer for the Backwoods.",
		"§7Three heads, yet none comprehend thy end.",
		"§7Weak.",
		"§7Thy end is now.",
		"§7Sentence is passed upon thy soul.",
		"§7A mere collection of bones. Ridiculous.",
		"§7Thou art but a transient ghost of the Nether.",
		"§7Cry out with thy triple throats. It shall not save thee.",
		"§7Wither away into nothingness.",
		"§7This realm belongs to the Backwoods. Cleanse the parasite.",
		"§7Thou art weak. Perish under the weight of thy own decay.",
		"§7Nothing but ash and dust shall remain of thy three heads.",
		"§7Thy stars are dim.",
		"§7Die.",
		"§7The roots of the Overworld run deeper than thy undercroft.",
		"§7What is a champion of wither to the ancient cycle of Rot?",
		"§7Return to the soil, skeleton king. Thy reign is finished.",
		"§7Your star will fall.",
		"§7Thou art but a shadow, a whisper of old ashes.",
		"§7Tremble, three-headed freak. The very woods rise against thee.",
		"§7Thy sulfurous breath cannot choke the vines of decay.",
		"§7This creature again.",
		"§7I have seen a larger one.",
		"§7Three brains, yet not a single spark of wisdom.",
		"§7Thou art a symptom of a diseased Nether... let me cure thee.",
		"§7The Cycle of Rot spares neither the mortal nor the undead.",
		"§7Flee back to thy bedrock ceiling, wretched intruder.",
		"§7I will feed thy ribcage to the saplings.",
		"§7No blast of thy skulls can pierce the ancient rot.",
		"§7Thy stars are stolen fire. Feel the genuine dark.",
		"§7I does not negotiate."
	};

	private static final String[] TOTEM_QUOTES = {
		"§7A talisman to cheat death... Is mortality really so fragile?",
		"§7To refuse the natural cycle of Rot... How futile.",
		"§7Interesting artifact.",
		"§7Intriguing artifact.",
		"§7A soul for a soul?",
		"§7Another life spared by gold and emeralds? It will not avail thee.",
		"§7How many more of those trinkets dost thou possess?",
		"§7A temporary reprieve. The Rot is inevitable.",
		"§7Clinging to life with borrowed magic... Pathetic.",
		"§7Thou canst not delay the harvest forever."
	};

	public static void execute() {
	}

	public static boolean canSendDialogue(Player player) {
		if (!ENABLE_ROT_DIALOGUES || player == null) return false;
		long currentTick = player.level().getGameTime();
		long lastTick = player.getPersistentData().getLong("rot_last_dialogue_tick");
		if (lastTick > 0 && currentTick - lastTick < 600) {
			return false;
		}
		player.getPersistentData().putLong("rot_last_dialogue_tick", currentTick);
		return true;
	}

	public static void sendActionBarToNearbyPlayers(LevelAccessor world, Vec3 pos, double range, String text) {
		if (!ENABLE_ROT_DIALOGUES) return;
		if (world instanceof ServerLevel s) {
			s.players().stream()
				.filter(p -> p.position().distanceToSqr(pos) <= range * range)
				.filter(RotDialoguesProcedure::canSendDialogue)
				.forEach(p -> p.displayClientMessage(Component.literal(text), true));
		}
	}

	public static void sendPlayerMessage(Player player, String message) {
		if (canSendDialogue(player)) {
			player.displayClientMessage(Component.literal(message), true);
		}
	}

	public static void sendRandomWitherQuote(LevelAccessor world, Entity rot, double range) {
		if (!ENABLE_ROT_DIALOGUES) return;
		if (rot != null) {
			String quote = WITHER_QUOTES[RandomSource.create().nextInt(WITHER_QUOTES.length)];
			sendActionBarToNearbyPlayers(world, rot.position(), range, quote);
		}
	}

	public static void sendRandomTotemQuote(Player player) {
		if (canSendDialogue(player)) {
			String quote = TOTEM_QUOTES[RandomSource.create().nextInt(TOTEM_QUOTES.length)];
			player.displayClientMessage(Component.literal(quote), true);
		}
	}

	public static void sendInfinityTotemQuote(Player player) {
		if (canSendDialogue(player)) {
			player.displayClientMessage(Component.literal("Prepare thyself."), true);
		}
	}

	public static void sendTotemObserved(Player player) {
		if (canSendDialogue(player)) {
			player.displayClientMessage(Component.literal("§cThe Rot observes the relic in your hands..."), true);
		}
	}

	public static void sendTotemPopped(Player player) {
		if (canSendDialogue(player)) {
			player.displayClientMessage(Component.literal("§cThe Rot witnesses your resurrection and analyzes the artifact..."), true);
		}
	}

	public static void sendTotemStolen(Player player) {
		if (canSendDialogue(player)) {
			player.displayClientMessage(Component.literal("§7Intriguing artifact..."), true);
		}
	}
}
// 1.21.1. never delete version comments
