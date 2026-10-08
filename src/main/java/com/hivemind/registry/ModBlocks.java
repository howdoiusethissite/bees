package com.hivemind.registry;

import com.hivemind.Hivemind;
import com.hivemind.block.HiveExitBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	public static final ResourceKey<Block> HIVE_EXIT_KEY = ResourceKey.create(Registries.BLOCK, Hivemind.id("hive_exit"));

	/** The glowing doorway inside a hive. Right-click it to go home. */
	public static final Block HIVE_EXIT = Blocks.register(
		HIVE_EXIT_KEY,
		HiveExitBlock::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE)
			.strength(-1.0F, 3600000.0F)
			.noLootTable()
			.lightLevel(state -> 12)
			.sound(SoundType.CORAL_BLOCK)
	);

	private ModBlocks() {
	}

	public static void init() {
	}
}
