package com.hivemind.registry;

import com.hivemind.Hivemind;
import com.hivemind.fluid.HoneyFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;

public final class ModFluids {
	public static final FlowingFluid FLOWING_HONEY = Registry.register(BuiltInRegistries.FLUID, Hivemind.id("flowing_honey"), new HoneyFluid.Flowing());
	public static final FlowingFluid HONEY = Registry.register(BuiltInRegistries.FLUID, Hivemind.id("honey"), new HoneyFluid.Source());

	private ModFluids() {
	}

	public static void init() {
	}
}
