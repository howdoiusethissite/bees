package com.hivemind.client;

import com.hivemind.client.model.GuardBeeGearModel;
import com.hivemind.client.model.QueenBeeModel;
import com.hivemind.client.render.GuardBeeRenderer;
import com.hivemind.client.render.QueenBeeRenderer;
import com.hivemind.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class HivemindClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(ModModelLayers.QUEEN_BEE, QueenBeeModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ModModelLayers.GUARD_BEE_GEAR, GuardBeeGearModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.QUEEN_BEE, QueenBeeRenderer::new);
		EntityRendererRegistry.register(ModEntities.GUARD_BEE, GuardBeeRenderer::new);
		EntityRendererRegistry.register(ModEntities.BEENADE, ThrownItemRenderer::new);
		QueenSpeechClient.init();
	}
}
