package com.hivemind.registry;

import com.hivemind.Hivemind;
import com.hivemind.block.BroodCellBlock;
import com.hivemind.block.EmptyBroodCellBlock;
import com.hivemind.fluid.HoneyLiquidBlock;
import com.hivemind.block.HiveExitBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

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

	public static final ResourceKey<Block> BROOD_CELL_KEY = ResourceKey.create(Registries.BLOCK, Hivemind.id("brood_cell"));

	/** A nursery cell with a baby bee growing in it. Look after it and it hatches. */
	public static final Block BROOD_CELL = Blocks.register(
		BROOD_CELL_KEY,
		BroodCellBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.6F).noLootTable().randomTicks().sound(SoundType.CORAL_BLOCK)
	);

	public static final ResourceKey<Block> EMPTY_BROOD_CELL_KEY = ResourceKey.create(Registries.BLOCK, Hivemind.id("empty_brood_cell"));

	/** A nursery cell whose bee has hatched, waiting for the queen to lay in it again. */
	public static final Block EMPTY_BROOD_CELL = Blocks.register(
		EMPTY_BROOD_CELL_KEY,
		EmptyBroodCellBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.6F).noLootTable().sound(SoundType.CORAL_BLOCK)
	);

	public static final ResourceKey<Block> HONEY_KEY = ResourceKey.create(Registries.BLOCK, Hivemind.id("honey"));

	/** Liquid honey. */
	public static final Block HONEY = Blocks.register(
		HONEY_KEY,
		p -> new HoneyLiquidBlock(ModFluids.HONEY, p),
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE)
			.replaceable()
			.noCollision()
			.strength(100.0F)
			.pushReaction(PushReaction.POPPED)
			.noLootTable()
			.liquid()
			.sound(SoundType.EMPTY)
	);

	private ModBlocks() {
	}

	public static void init() {
	}
}
