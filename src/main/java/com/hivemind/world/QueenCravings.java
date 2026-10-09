package com.hivemind.world;

import com.hivemind.HivemindConfig;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;

/**
 * Each queen has a flower she's craving at the moment. It changes every few in-game days (see
 * {@code cravingDays}), and different hives crave different flowers. Bringing her the flower she
 * wants wins a lot more favor and a much bigger gift.
 */
public final class QueenCravings {
	/** Every flower she might crave, as its potted block so the hive's own pots can hold it. */
	public static final List<Block> POTTED = List.of(
		Blocks.POTTED_DANDELION, Blocks.POTTED_POPPY, Blocks.POTTED_BLUE_ORCHID, Blocks.POTTED_ALLIUM, Blocks.POTTED_AZURE_BLUET,
		Blocks.POTTED_RED_TULIP, Blocks.POTTED_ORANGE_TULIP, Blocks.POTTED_WHITE_TULIP, Blocks.POTTED_PINK_TULIP, Blocks.POTTED_OXEYE_DAISY,
		Blocks.POTTED_CORNFLOWER, Blocks.POTTED_LILY_OF_THE_VALLEY, Blocks.POTTED_TORCHFLOWER
	);
	public static final int FAVOR = 3;

	private QueenCravings() {
	}

	/** Which craving period the world is in. Counted on the overworld clock so every hive agrees. */
	private static long period(final MinecraftServer server) {
		return server.overworld().getGameTime() / (24000L * HivemindConfig.get().cravingDays);
	}

	/** The potted form of the flower the queen of hive {@code index} is craving right now. */
	public static Block craving(final MinecraftServer server, final int index) {
		long h = period(server) * 0x9E3779B97F4A7C15L + index * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 31;
		return POTTED.get((int)Math.floorMod(h, (long)POTTED.size()));
	}

	public static Item cravedItem(final MinecraftServer server, final int index) {
		return ((FlowerPotBlock)craving(server, index)).getPotted().asItem();
	}

	public static boolean isCraved(final MinecraftServer server, final int index, final ItemStack stack) {
		return stack.is(cravedItem(server, index));
	}
}
