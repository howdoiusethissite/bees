package com.hivemind.network;

import com.hivemind.Hivemind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells a client that the queen is saying something. The client looks the line up in its own
 * language, babbles it out loud a syllable at a time, and (if it was said to them) types it out
 * above the hotbar.
 *
 * @param favor the listener's favor to show after the line, or -1 to leave it off
 * @param log whether the line is worth keeping in the chat log once it's been said (gifts, raids),
 *            rather than just showing above the hotbar
 */
public record QueenSpeechPayload(int queenId, String key, boolean addressed, int favor, boolean log) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<QueenSpeechPayload> TYPE = new CustomPacketPayload.Type<>(Hivemind.id("queen_speech"));
	public static final StreamCodec<RegistryFriendlyByteBuf, QueenSpeechPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		QueenSpeechPayload::queenId,
		ByteBufCodecs.STRING_UTF8,
		QueenSpeechPayload::key,
		ByteBufCodecs.BOOL,
		QueenSpeechPayload::addressed,
		ByteBufCodecs.VAR_INT,
		QueenSpeechPayload::favor,
		ByteBufCodecs.BOOL,
		QueenSpeechPayload::log,
		QueenSpeechPayload::new
	);

	@Override
	public CustomPacketPayload.Type<QueenSpeechPayload> type() {
		return TYPE;
	}
}
