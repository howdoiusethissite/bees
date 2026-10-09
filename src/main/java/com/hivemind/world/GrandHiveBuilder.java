package com.hivemind.world;

import com.hivemind.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The outside of a grand hive: a big woven skep, banded straw and honeycomb, with a few real bee nests
 * set into its sides and a glowing door at the front. It only fills in air and plants, so it never
 * eats into the ground or anything the player built. The door itself is always placed.
 */
public final class GrandHiveBuilder {
	/** How far from where the egg was planted a door can be and still find its hive. */
	public static final int DOOR_SEARCH_RANGE = 8;
	private static final int HEIGHT = 9;
	private static final double BASE_RADIUS = 4.6;
	private static final int BEES = 6;

	private static final BlockState STRAW = Blocks.HAY_BLOCK.defaultBlockState();
	private static final BlockState COMB = Blocks.HONEYCOMB_BLOCK.defaultBlockState();
	private static final BlockState HONEY = Blocks.HONEY_BLOCK.defaultBlockState();
	private static final BlockState LIGHT = Blocks.OCHRE_FROGLIGHT.defaultBlockState();

	private GrandHiveBuilder() {
	}

	/** Radius of the skep at height {@code dy} above its base: wide at the bottom, rounding off to a dome. */
	private static double radiusAt(final int dy) {
		double t = (double)dy / HEIGHT;
		return BASE_RADIUS * Math.sqrt(Math.max(0.0, 1.0 - t * t * t));
	}

	/** Builds the skep around {@code base} (where the egg was planted) with its door facing {@code front}. */
	public static void build(final ServerLevel level, final BlockPos base, final Direction front) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int r = Mth.ceil(BASE_RADIUS);
		for (int dy = 0; dy <= HEIGHT; dy++) {
			double radius = radiusAt(dy);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double dist = Math.sqrt(dx * dx + dz * dz);
					if (dist > radius + 0.3) {
						continue;
					}
					pos.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
					BlockState state;
					if (dy == HEIGHT || radius < 1.0) {
						state = HONEY;
					} else if (dist > radius - 1.0) {
						// Woven bands, with a ring of light partway up.
						state = dy == 3 ? LIGHT : dy % 2 == 0 ? STRAW : COMB;
					} else {
						state = COMB;
					}
					place(level, pos, state);
				}
			}
		}

		// A few real nests set into the sides, so the neighborhood's bees move in.
		Direction[] sides = {front.getClockWise(), front.getCounterClockWise(), front.getOpposite()};
		for (int i = 0; i < sides.length; i++) {
			Direction side = sides[i];
			int dy = 2 + i * 2;
			int out = Mth.floor(radiusAt(dy));
			BlockPos nest = base.relative(side, out).above(dy);
			BlockState state = Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, side);
			if (level.getBlockState(nest).canBeReplaced() || isOurs(level.getBlockState(nest))) {
				level.setBlock(nest, state, Block.UPDATE_ALL);
			}
		}

		// The door, flush with the front wall and framed in light. Always placed, so the hive can be entered.
		int out = Mth.floor(radiusAt(1));
		BlockPos door = base.relative(front, out).above();
		level.setBlock(door, ModBlocks.GRAND_HIVE_DOOR.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(door.above(), ModBlocks.GRAND_HIVE_DOOR.defaultBlockState(), Block.UPDATE_ALL);
		BlockPos below = door.below();
		if (level.getBlockState(below).canBeReplaced()) {
			level.setBlock(below, COMB, Block.UPDATE_ALL);
		}

		for (int i = 0; i < BEES; i++) {
			Bee bee = EntityTypes.BEE.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (bee == null) {
				continue;
			}
			double angle = i * Math.PI * 2 / BEES;
			bee.snapTo(base.getX() + 0.5 + Math.cos(angle) * 6, base.getY() + 3 + i % 3, base.getZ() + 0.5 + Math.sin(angle) * 6, 0.0F, 0.0F);
			level.addFreshEntity(bee);
		}

		level.playSound(null, base, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 2.0F, 0.6F);
		level.playSound(null, base, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 2.0F, 0.8F);
		level.sendParticles(ParticleTypes.WAX_ON, base.getX() + 0.5, base.getY() + HEIGHT / 2.0, base.getZ() + 0.5, 80, BASE_RADIUS, HEIGHT / 2.0, BASE_RADIUS, 0.1);
	}

	/** Only air and plants give way to the hive. */
	private static void place(final ServerLevel level, final BlockPos pos, final BlockState state) {
		BlockState current = level.getBlockState(pos);
		if (current.canBeReplaced() || current.is(ModBlocks.ROYAL_CRADLE)) {
			level.setBlock(pos, state, Block.UPDATE_ALL);
		}
	}

	private static boolean isOurs(final BlockState state) {
		return state.is(Blocks.HAY_BLOCK) || state.is(Blocks.HONEYCOMB_BLOCK) || state.is(Blocks.HONEY_BLOCK) || state.is(Blocks.OCHRE_FROGLIGHT) || state.canBeReplaced();
	}
}
