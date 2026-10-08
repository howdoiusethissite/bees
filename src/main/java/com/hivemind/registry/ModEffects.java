package com.hivemind.registry;

import com.hivemind.Hivemind;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ModEffects {
	/** Shrinks the player to a quarter of their size. While shrunk, right-clicking a hive takes you inside. */
	public static final Holder<MobEffect> SHRUNK = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT,
		Hivemind.id("shrunk"),
		new MobEffect(MobEffectCategory.NEUTRAL, 0xF5B82E) {}
			.addAttributeModifier(Attributes.SCALE, Hivemind.id("effect.shrunk"), -0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
	);

	private ModEffects() {
	}

	public static void init() {
	}
}
