package com.hivemind.registry;

import com.hivemind.Hivemind;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;

public final class ModAttachments {
	/** On a bee: the player it's following around. Saved with the bee. */
	public static final AttachmentType<UUID> ESCORTING = AttachmentRegistry.create(
		Hivemind.id("escorting"), builder -> builder.persistent(UUIDUtil.CODEC)
	);

	private ModAttachments() {
	}

	public static void init() {
	}
}
