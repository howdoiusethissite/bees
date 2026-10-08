package com.hivemind.registry;

import com.hivemind.Hivemind;
import com.hivemind.entity.GuardBee;
import com.hivemind.entity.QueenBee;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> GUARD_BEE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Hivemind.id("guard_bee"));
	public static final ResourceKey<EntityType<?>> QUEEN_BEE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Hivemind.id("queen_bee"));

	public static final EntityType<GuardBee> GUARD_BEE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		GUARD_BEE_KEY,
		EntityType.Builder.of(GuardBee::new, MobCategory.CREATURE).sized(1.1F, 1.0F).eyeHeight(0.6F).clientTrackingRange(8).build(GUARD_BEE_KEY)
	);
	public static final EntityType<QueenBee> QUEEN_BEE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		QUEEN_BEE_KEY,
		EntityType.Builder.of(QueenBee::new, MobCategory.CREATURE).sized(0.8F, 2.4F).eyeHeight(2.1F).clientTrackingRange(10).build(QUEEN_BEE_KEY)
	);

	private ModEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(GUARD_BEE, GuardBee.createAttributes());
		FabricDefaultAttributeRegistry.register(QUEEN_BEE, QueenBee.createAttributes());
	}
}
