package com.hivemind.registry;

import com.hivemind.Hivemind;
import com.hivemind.item.BeeToolMode;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModComponents {
	/** Which tool the bee multitool is acting as right now. Synced so the item model can show it. */
	public static final DataComponentType<BeeToolMode> TOOL_MODE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Hivemind.id("tool_mode"),
		DataComponentType.<BeeToolMode>builder().persistent(BeeToolMode.CODEC).networkSynchronized(BeeToolMode.STREAM_CODEC).build()
	);

	private ModComponents() {
	}

	public static void init() {
	}
}
