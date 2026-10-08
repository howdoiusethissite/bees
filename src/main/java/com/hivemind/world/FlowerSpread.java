package com.hivemind.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cross-pollination. Now and then, when a bee finishes collecting nectar from a small flower, a copy of
 * that flower sprouts on a free patch of ground nearby. Keep bees around a flower bed and it slowly fills
 * in, which keeps the queen's flower supply renewable.
 */
public final class FlowerSpread {
	private static final int CHANCE = 3;
	private static final int ATTEMPTS = 6;

	private FlowerSpread() {
	}

	public static void onPollinated(final ServerLevel level, final Bee bee, final BlockPos flowerPos) {
		RandomSource random = level.getRandom();
		if (random.nextInt(CHANCE) != 0) {
			return;
		}
		BlockState flower = level.getBlockState(flowerPos);
		// Tall flowers can already be bone-mealed, and nobody wants wither roses spreading.
		if (!flower.is(BlockTags.SMALL_FLOWERS) || flower.is(Blocks.WITHER_ROSE)) {
			return;
		}
		for (int i = 0; i < ATTEMPTS; i++) {
			BlockPos target = flowerPos.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
			if (level.isEmptyBlock(target) && flower.canSurvive(level, target)) {
				level.setBlock(target, flower, Block.UPDATE_ALL);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX() + 0.5, target.getY() + 0.4, target.getZ() + 0.5, 4, 0.25, 0.2, 0.25, 0.0);
				return;
			}
		}
	}
}
