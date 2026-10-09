package com.hivemind.registry;

import com.hivemind.Hivemind;
import com.mojang.serialization.Codec;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;

public final class ModAttachments {
	/** On a bee: the player it's following around. Saved with the bee. */
	public static final AttachmentType<UUID> ESCORTING = AttachmentRegistry.create(
		Hivemind.id("escorting"), builder -> builder.persistent(UUIDUtil.CODEC)
	);

	/** On a beehive or bee nest: honey stored past the five levels vanilla can show. */
	public static final AttachmentType<Integer> HONEY_RESERVE = AttachmentRegistry.create(
		Hivemind.id("honey_reserve"), builder -> builder.persistent(Codec.INT)
	);

	/** On a player: the highest favor they've ever reached with any queen. Kept through death. */
	public static final AttachmentType<Integer> INFLUENCE = AttachmentRegistry.create(
		Hivemind.id("influence"), builder -> builder.persistent(Codec.INT).copyOnDeath()
	);

	/** On a player: whether a queen has already handed them their royal egg for reaching the top rank. */
	public static final AttachmentType<Boolean> ROYAL_EGG_GIVEN = AttachmentRegistry.create(
		Hivemind.id("royal_egg_given"), builder -> builder.persistent(Codec.BOOL).copyOnDeath()
	);

	private ModAttachments() {
	}

	public static void init() {
	}
}
