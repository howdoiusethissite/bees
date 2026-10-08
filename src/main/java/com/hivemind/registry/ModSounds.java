package com.hivemind.registry;

import com.hivemind.Hivemind;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	/** One short buzzy syllable per vowel. The queen strings these together when she talks. */
	public static final List<Holder<SoundEvent>> QUEEN_BABBLE = List.of(
		register("entity.queen_bee.babble.a"),
		register("entity.queen_bee.babble.e"),
		register("entity.queen_bee.babble.i"),
		register("entity.queen_bee.babble.o"),
		register("entity.queen_bee.babble.u")
	);

	private ModSounds() {
	}

	private static Holder<SoundEvent> register(final String name) {
		Identifier id = Hivemind.id(name);
		return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
	}
}
