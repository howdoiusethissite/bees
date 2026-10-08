package com.hivemind.client;

import com.hivemind.network.QueenSpeechPayload;
import com.hivemind.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * Plays the queen's lines the Animal Crossing way: a quick buzzy syllable for every couple of
 * letters, with the pitch wobbling by letter, while the text types itself out above the hotbar.
 */
public final class QueenSpeechClient {
	private static final int TICKS_PER_LETTER = 1;
	private static final int PAUSE_TICKS = 4;

	private static final class Speech {
		final int queenId;
		final String text;
		final boolean addressed;
		final int favor;
		int shown;
		int wait;
		int lettersSinceSound;

		Speech(final int queenId, final String text, final boolean addressed, final int favor) {
			this.queenId = queenId;
			this.text = text;
			this.addressed = addressed;
			this.favor = favor;
		}
	}

	/** At most one line per queen at a time; a new line cuts off the old one. */
	private static final Map<Integer, Speech> ACTIVE = new HashMap<>();

	private QueenSpeechClient() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(QueenSpeechPayload.TYPE, (payload, context) -> {
			Speech speech = new Speech(payload.queenId(), I18n.get(payload.key()), payload.addressed(), payload.favor());
			ACTIVE.put(payload.queenId(), speech);
		});
		ClientTickEvents.END_CLIENT_TICK.register(QueenSpeechClient::tick);
	}

	private static void tick(final Minecraft minecraft) {
		if (minecraft.level == null || minecraft.isPaused()) {
			if (minecraft.level == null) {
				ACTIVE.clear();
			}
			return;
		}
		List<Integer> done = new ArrayList<>();
		for (Speech speech : ACTIVE.values()) {
			Entity queen = minecraft.level.getEntity(speech.queenId);
			if (queen == null) {
				done.add(speech.queenId);
				continue;
			}
			if (speech.wait > 0) {
				speech.wait--;
				continue;
			}
			char c = speech.text.charAt(speech.shown);
			speech.shown++;
			speech.wait = TICKS_PER_LETTER - 1;
			if (Character.isLetter(c)) {
				// One syllable for every other letter keeps it chattery without turning into a drone.
				if (speech.lettersSinceSound++ % 2 == 0) {
					babble(minecraft, queen, c, speech.text, speech.shown);
				}
			} else {
				speech.lettersSinceSound = 0;
				if (".!?,~".indexOf(c) >= 0) {
					speech.wait = PAUSE_TICKS;
				}
			}
			boolean finished = speech.shown >= speech.text.length();
			if (speech.addressed) {
				minecraft.gui.hud.setOverlayMessage(line(speech, finished ? speech.text : speech.text.substring(0, speech.shown), finished), false);
			}
			if (finished) {
				if (speech.addressed) {
					minecraft.gui.hud.getChat().addClientSystemMessage(line(speech, speech.text, true));
				}
				done.add(speech.queenId);
			}
		}
		done.forEach(ACTIVE::remove);
	}

	private static MutableComponent line(final Speech speech, final String text, final boolean withFavor) {
		MutableComponent line = Component.translatable("queen.hivemind.speaker", text).withStyle(ChatFormatting.GOLD);
		if (withFavor && speech.favor >= 0) {
			line.append(Component.translatable("queen.hivemind.favor", speech.favor).withStyle(ChatFormatting.GRAY));
		}
		return line;
	}

	private static void babble(final Minecraft minecraft, final Entity queen, final char letter, final String text, final int position) {
		char lower = Character.toLowerCase(letter);
		int sound = switch (lower) {
			case 'a', 'h', 'r', 'w' -> 0;
			case 'e', 'b', 'l', 'y' -> 1;
			case 'i', 'k', 's', 'z', 'c' -> 2;
			case 'o', 'g', 'm', 'p', 'v' -> 3;
			default -> 4;
		};
		float pitch = 1.0F + ((lower * 7) % 9 - 4) * 0.035F;
		// Questions go up at the end, the way they do in Animal Crossing.
		int end = text.length();
		if (end > 0 && text.charAt(end - 1) == '?' && position > end - 6) {
			pitch += 0.25F;
		}
		minecraft.level.playLocalSound(queen, ModSounds.QUEEN_BABBLE.get(sound).value(), SoundSource.NEUTRAL, 0.7F, pitch);
	}
}
