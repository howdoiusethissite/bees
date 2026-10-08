package com.hivemind.client;

import com.hivemind.Hivemind;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ModModelLayers {
	public static final ModelLayerLocation QUEEN_BEE = new ModelLayerLocation(Hivemind.id("queen_bee"), "main");
	public static final ModelLayerLocation GUARD_BEE_GEAR = new ModelLayerLocation(Hivemind.id("guard_bee"), "gear");

	private ModModelLayers() {
	}
}
