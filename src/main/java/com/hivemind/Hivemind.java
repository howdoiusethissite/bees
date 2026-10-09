package com.hivemind;

import com.hivemind.network.QueenSpeechPayload;
import com.hivemind.registry.ModAttachments;
import com.hivemind.registry.ModBlocks;
import com.hivemind.registry.ModComponents;
import com.hivemind.registry.ModEffects;
import com.hivemind.registry.ModEntities;
import com.hivemind.registry.ModFluids;
import com.hivemind.registry.ModItems;
import com.hivemind.registry.ModSounds;
import com.hivemind.world.BeeArmor;
import com.hivemind.world.BeeEscorts;
import com.hivemind.world.BeeSwarms;
import com.hivemind.world.HiveCommands;
import com.hivemind.world.HiveEvents;
import com.hivemind.world.HoneyEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.DispenserBlock;
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
		HivemindConfig.load();
		ModEffects.init();
		ModSounds.init();
		ModComponents.init();
		ModAttachments.init();
		ModFluids.init();
		ModBlocks.init();
		ModEntities.init();
		ModItems.init();
		PayloadTypeRegistry.clientboundPlay().register(QueenSpeechPayload.TYPE, QueenSpeechPayload.CODEC);
		DispenserBlock.registerProjectileBehavior(ModItems.BEENADE);
		HiveEvents.init();
		HiveCommands.init();
		BeeSwarms.init();
		BeeArmor.init();
		BeeEscorts.init();
		HoneyEvents.init();
	}
}
