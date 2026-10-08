package com.hivemind;

import com.hivemind.registry.ModBlocks;
import com.hivemind.registry.ModEffects;
import com.hivemind.registry.ModEntities;
import com.hivemind.registry.ModItems;
import com.hivemind.world.HiveCommands;
import com.hivemind.world.HiveEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hivemind implements ModInitializer {
	public static final String MOD_ID = "hivemind";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEffects.init();
		ModBlocks.init();
		ModEntities.init();
		ModItems.init();
		HiveEvents.init();
		HiveCommands.init();
	}
}
