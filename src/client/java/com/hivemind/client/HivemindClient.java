package com.hivemind.client;

import com.hivemind.Hivemind;
import com.hivemind.client.model.GuardBeeGearModel;
import com.hivemind.client.model.QueenBeeModel;
import com.hivemind.client.render.GuardBeeRenderer;
import com.hivemind.client.render.QueenBeeRenderer;
import com.hivemind.registry.ModEntities;
import com.hivemind.registry.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.model.sprite.Material;

public class HivemindClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(ModModelLayers.QUEEN_BEE, QueenBeeModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ModModelLayers.GUARD_BEE_GEAR, GuardBeeGearModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.QUEEN_BEE, QueenBeeRenderer::new);
		EntityRendererRegistry.register(ModEntities.GUARD_BEE, GuardBeeRenderer::new);
		EntityRendererRegistry.register(ModEntities.BEENADE, ThrownItemRenderer::new);
		QueenSpeechClient.init();
		// Honey is see-through, like water, but keeps its own amber color (no biome tint).
		FluidRenderingRegistry.register(
			ModFluids.HONEY,
			ModFluids.FLOWING_HONEY,
			new FluidModel.Unbaked(new Material(Hivemind.id("block/honey_still"), true), new Material(Hivemind.id("block/honey_flow"), true), null, null)
		);
	}
}
