package com.hivemind.world;

import com.hivemind.block.BroodCellBlock;
import com.hivemind.entity.GuardBee;
import com.hivemind.entity.QueenBee;
import com.hivemind.registry.ModBlocks;
import com.hivemind.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Carves out one hive interior: a big honeycomb dome with comb pillars, hanging combs,
 * a raised dais for the queen in the middle, and a glowing exit at the south end.
 */
public final class HiveInteriorBuilder {
	public static final String RESIDENT_TAG = "hivemind.resident";
	/** Bump this when adding something to the interior, and add the step to {@link #upgrade}. */
	public static final int LAYOUT_VERSION = 3;
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	private static final int WORKER_BEES = 10;
	private static final int GUARD_BEES = 6;
	/** A grand hive grown from a royal egg is a bigger colony. */
	private static final int GRAND_WORKER_BEES = 16;
	private static final int GRAND_GUARD_BEES = 8;
	/** Worker bees inside are scaled up so they look bee-sized next to a shrunken player. */
	public static final double RESIDENT_BEE_SCALE = 1.8;

	private static final BlockState COMB = Blocks.HONEYCOMB_BLOCK.defaultBlockState();
	private static final BlockState HONEY = Blocks.HONEY_BLOCK.defaultBlockState();
	private static final BlockState LIGHT = Blocks.OCHRE_FROGLIGHT.defaultBlockState();
	private static final BlockState NEST = Blocks.BEE_NEST.defaultBlockState();

	private HiveInteriorBuilder() {
	}

	/** Adds whatever newer layouts have that this hive, built with an older one, is missing. */
	public static void upgrade(final ServerLevel level, final int index, final int fromVersion) {
		if (fromVersion < 2) {
			buildNursery(level, index);
		}
		if (fromVersion < 3) {
			buildHoneySpring(level, index);
		}
	}

	/**
	 * @param founder for a grand hive, the player who planted the royal egg. Their queen adores them from the start.
	 */
	public static void build(final ServerLevel level, final int index, final @Nullable UUID founder) {
		BlockPos c = HiveLayout.center(index);
		buildShellAndFloor(level, c);
		buildPillars(level, c);
		buildHangingCombs(level, c);
		buildDais(level, c);
		buildExit(level, index);
		buildNursery(level, index);
		buildHoneySpring(level, index);
		spawnResidents(level, index, founder);
	}

	/**
	 * Four patches of brood cells set into the floor between the pillars, two on each side of the
	 * walkway. Each cell starts at a random stage; some already need looking after.
	 */
	public static void buildNursery(final ServerLevel level, final int index) {
		BlockPos c = HiveLayout.center(index);
		int floorY = HiveLayout.floorY();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BroodCellBlock.Need[] needs = BroodCellBlock.Need.values();
		for (BlockPos patch : HiveLayout.nurseryCenters(index)) {
			int px = patch.getX();
			int pz = patch.getZ();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (!HiveLayout.isNurseryCell(dx, dz)) {
						continue;
					}
					int x = px + dx;
					int z = pz + dz;
					pos.set(x, floorY, z);
					if (noise(x, floorY, z, 10) < 0.15) {
						// A few cells start out empty, waiting for the queen to lay in them.
						level.setBlock(pos, ModBlocks.EMPTY_BROOD_CELL.defaultBlockState(), FLAGS);
					} else {
						int stage = (int)(noise(x, floorY, z, 8) * (BroodCellBlock.CAPPED + 1));
						BroodCellBlock.Need need = stage == BroodCellBlock.CAPPED ? BroodCellBlock.Need.NONE : needs[(int)(noise(x, floorY, z, 9) * needs.length)];
						if (stage == BroodCellBlock.EGG && need == BroodCellBlock.Need.HUNGRY) {
							need = BroodCellBlock.Need.LONELY;
						}
						level.setBlock(pos, ModBlocks.BROOD_CELL.defaultBlockState().setValue(BroodCellBlock.STAGE, stage).setValue(BroodCellBlock.NEED, need), FLAGS);
					}
					// Clear anything sitting on top (a stray potted flower) so the cell can be reached.
					level.setBlock(pos.move(0, 1, 0), Blocks.AIR.defaultBlockState(), FLAGS);
				}
			}
			// A candle at the edge of each patch to keep the babies warm.
			BlockState candle = Blocks.DYED_CANDLE.orange().defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true);
			double angle = Math.atan2(pz - c.getZ(), px - c.getX());
			int cx = px + Mth.floor(Math.cos(angle) * 2.5 + 0.5);
			int cz = pz + Mth.floor(Math.sin(angle) * 2.5 + 0.5);
			level.setBlock(pos.set(cx, floorY + 1, cz), candle, FLAGS);
		}
	}

	/**
	 * A small pool of liquid honey set into the floor behind the throne. Standing in it heals you,
	 * which comes in handy during a raid, and you can bottle it.
	 */
	public static void buildHoneySpring(final ServerLevel level, final int index) {
		BlockPos spring = HiveLayout.honeySpring(index);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockState honey = ModBlocks.HONEY.defaultBlockState();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				double hd = hexDistance(dx, dz);
				if (hd > 2.6) {
					continue;
				}
				pos.set(spring.getX() + dx, spring.getY(), spring.getZ() + dz);
				if (hd <= 1.8) {
					level.setBlock(pos, honey, FLAGS);
					// Make sure it has a floor and nothing on top.
					level.setBlock(pos.move(0, -1, 0), COMB, FLAGS);
					level.setBlock(pos.move(0, 2, 0), Blocks.AIR.defaultBlockState(), FLAGS);
				} else {
					// A rim of honey blocks around the edge.
					level.setBlock(pos, HONEY, FLAGS);
				}
			}
		}
	}

	private static double shell(final double dx, final double dy, final double dz, final int shrink) {
		double rx = HiveLayout.RADIUS_XZ - shrink;
		double ry = HiveLayout.RADIUS_Y - shrink;
		return (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rx * rx);
	}

	private static boolean isInside(final BlockPos c, final BlockPos pos) {
		return shell(pos.getX() - c.getX(), pos.getY() - c.getY(), pos.getZ() - c.getZ(), 3) < 1.0;
	}

	/** Cheap deterministic per-block noise in [0, 1). */
	private static double noise(final int x, final int y, final int z, final int salt) {
		long h = x * 3129871L ^ z * 116129781L ^ y * 0x2545F4914F6CDD1DL ^ salt * 0x9E3779B97F4A7C15L;
		h = h * h * 42317861L + h * 11L;
		h ^= h >>> 29;
		return ((h >>> 16) & 0xFFFF) / 65536.0;
	}

	/** Distance in a flat-topped hex metric; a value <= r means the point is inside a hexagon of radius r. */
	private static double hexDistance(final double x, final double z) {
		double ax = Math.abs(x);
		double az = Math.abs(z);
		return Math.max(ax * 0.866 + az * 0.5, az);
	}

	private static void buildShellAndFloor(final ServerLevel level, final BlockPos c) {
		int rx = HiveLayout.RADIUS_XZ;
		int ry = HiveLayout.RADIUS_Y;
		int floorY = c.getY() - HiveLayout.FLOOR_DEPTH;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -rx; dx <= rx; dx++) {
			for (int dz = -rx; dz <= rx; dz++) {
				for (int dy = -ry; dy <= ry; dy++) {
					double outer = shell(dx, dy, dz, 0);
					if (outer > 1.0) {
						continue;
					}
					int x = c.getX() + dx;
					int y = c.getY() + dy;
					int z = c.getZ() + dz;
					pos.set(x, y, z);
					boolean wall = shell(dx, dy, dz, 3) >= 1.0;
					if (wall) {
						double n = noise(x, y, z, 1);
						BlockState state = n < 0.04 ? LIGHT : n < 0.16 ? HONEY : n < 0.18 ? NEST : COMB;
						level.setBlock(pos, state, FLAGS);
					} else if (y <= floorY) {
						boolean patch = y == floorY && noise(x >> 1, 0, z >> 1, 2) < 0.07;
						level.setBlock(pos, patch ? HONEY : COMB, FLAGS);
					}
				}
			}
		}
	}

	private static void buildPillars(final ServerLevel level, final BlockPos c) {
		int floorY = c.getY() - HiveLayout.FLOOR_DEPTH;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		// Pillar centers on a hex grid, skipping the middle where the queen's dais goes.
		for (int q = -3; q <= 3; q++) {
			for (int r = -3; r <= 3; r++) {
				double px = q * 12 + r * 6;
				double pz = r * 10.4;
				double dist = Math.sqrt(px * px + pz * pz);
				if (dist < 11 || dist > 19) {
					continue;
				}
				// Keep the walkway from the exit to the queen clear.
				if (Math.abs(px) < 5 && pz > 0) {
					continue;
				}
				int cx = c.getX() + Mth.floor(px);
				int cz = c.getZ() + Mth.floor(pz);
				for (int dx = -3; dx <= 3; dx++) {
					for (int dz = -3; dz <= 3; dz++) {
						double hd = hexDistance(dx, dz);
						if (hd > 2.6) {
							continue;
						}
						boolean rim = hd > 1.6;
						for (int y = floorY + 1; y < c.getY() + HiveLayout.RADIUS_Y; y++) {
							pos.set(cx + dx, y, cz + dz);
							if (!isInside(c, pos)) {
								break;
							}
							BlockState state;
							if (rim && (y - floorY) % 6 == 4) {
								state = LIGHT;
							} else if (rim && noise(pos.getX(), y, pos.getZ(), 3) < 0.2) {
								state = HONEY;
							} else {
								state = COMB;
							}
							level.setBlock(pos, state, FLAGS);
						}
					}
				}
			}
		}
	}

	private static void buildHangingCombs(final ServerLevel level, final BlockPos c) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < 14; i++) {
			double angle = i * 2.39996;
			double radius = 6 + noise(i, 0, 0, 4) * 14;
			int cx = c.getX() + Mth.floor(Math.cos(angle) * radius);
			int cz = c.getZ() + Mth.floor(Math.sin(angle) * radius);
			int length = 3 + (int)(noise(i, 1, 0, 5) * 6);
			// Find the ceiling above this column.
			int top = c.getY();
			while (top < c.getY() + HiveLayout.RADIUS_Y && isInside(c, pos.set(cx, top + 1, cz))) {
				top++;
			}
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					int columnLength = (dx == 0 && dz == 0) ? length : length / 2;
					for (int k = 0; k <= columnLength; k++) {
						pos.set(cx + dx, top - k, cz + dz);
						boolean tip = k == columnLength && dx == 0 && dz == 0;
						level.setBlock(pos, tip ? HONEY : COMB, FLAGS);
					}
				}
			}
		}
	}

	private static void buildDais(final ServerLevel level, final BlockPos c) {
		int floorY = c.getY() - HiveLayout.FLOOR_DEPTH;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -7; dx <= 7; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				double hd = hexDistance(dx, dz);
				if (hd <= 6.5) {
					level.setBlock(pos.set(c.getX() + dx, floorY + 1, c.getZ() + dz), COMB, FLAGS);
				}
				if (hd <= 3.5) {
					level.setBlock(pos.set(c.getX() + dx, floorY + 2, c.getZ() + dz), hd <= 1.0 ? Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState() : COMB, FLAGS);
				}
			}
		}

		// Throne back, behind the queen on the north side.
		for (int dx = -2; dx <= 2; dx++) {
			int height = 4 - Math.abs(dx);
			for (int dy = 0; dy < height; dy++) {
				level.setBlock(pos.set(c.getX() + dx, floorY + 3 + dy, c.getZ() - 3), dy == height - 1 ? LIGHT : COMB, FLAGS);
			}
		}

		// A ring of candles on the lower tier.
		BlockState candle = Blocks.DYED_CANDLE.yellow().defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true);
		for (int i = 0; i < 6; i++) {
			double angle = i * Math.PI / 3 + Math.PI / 6;
			pos.set(c.getX() + Mth.floor(Math.cos(angle) * 5 + 0.5), floorY + 2, c.getZ() + Mth.floor(Math.sin(angle) * 5 + 0.5));
			level.setBlock(pos, candle, FLAGS);
		}

		// Potted flowers around the base, gifts from past visitors.
		for (int i = 0; i < POTS.length; i++) {
			level.setBlock(potPos(c, i), POTS[i].defaultBlockState(), FLAGS);
		}
	}

	private static final Block[] POTS = {
		Blocks.POTTED_DANDELION, Blocks.POTTED_POPPY, Blocks.POTTED_CORNFLOWER, Blocks.POTTED_ALLIUM, Blocks.POTTED_OXEYE_DAISY, Blocks.POTTED_BLUE_ORCHID,
		Blocks.POTTED_AZURE_BLUET, Blocks.POTTED_DANDELION
	};
	/** Which of the pots hold the flower the queen is craving. They sit either side of the walkway, facing the entrance. */
	private static final int[] CRAVING_POTS = {1, 2};

	private static BlockPos potPos(final BlockPos c, final int i) {
		double angle = i * Math.PI / 4 + Math.PI / 8;
		return new BlockPos(c.getX() + Mth.floor(Math.cos(angle) * 9 + 0.5), HiveLayout.floorY() + 1, c.getZ() + Mth.floor(Math.sin(angle) * 9 + 0.5));
	}

	/**
	 * Puts a flower back in every pot around the dais that's been emptied, and makes sure two of them hold
	 * {@code craving}, the flower the queen wants right now. Called each time someone comes in, so the pots
	 * never run out. Only pots (empty or full) are touched.
	 */
	public static void refillFlowerPots(final ServerLevel level, final int index, final Block craving) {
		BlockPos c = HiveLayout.center(index);
		for (int i = 0; i < POTS.length; i++) {
			BlockPos pos = potPos(c, i);
			BlockState current = level.getBlockState(pos);
			// Only pots. A nursery patch may have taken a pot's spot, and that stays a nursery.
			if (!(current.getBlock() instanceof FlowerPotBlock)) {
				continue;
			}
			boolean cravingPot = i == CRAVING_POTS[0] || i == CRAVING_POTS[1];
			Block wanted = cravingPot ? craving : POTS[i];
			if (!current.is(wanted)) {
				level.setBlock(pos, wanted.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}

	private static void buildExit(final ServerLevel level, final int index) {
		BlockPos exit = HiveLayout.exitPos(index);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		// Glowing door, two blocks tall, framed by froglights, with the hive wall behind it.
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -1; dy <= 3; dy++) {
				pos.set(exit.getX() + dx, exit.getY() + dy, exit.getZ() + 1);
				level.setBlock(pos, COMB, FLAGS);
			}
		}
		for (int dy = 0; dy <= 2; dy++) {
			level.setBlock(pos.set(exit.getX() - 1, exit.getY() + dy, exit.getZ()), LIGHT, FLAGS);
			level.setBlock(pos.set(exit.getX() + 1, exit.getY() + dy, exit.getZ()), LIGHT, FLAGS);
		}
		level.setBlock(pos.set(exit.getX(), exit.getY() + 2, exit.getZ()), LIGHT, FLAGS);
		level.setBlock(exit, ModBlocks.HIVE_EXIT.defaultBlockState(), FLAGS);
		level.setBlock(exit.above(), ModBlocks.HIVE_EXIT.defaultBlockState(), FLAGS);
	}

	private static void spawnResidents(final ServerLevel level, final int index, final @Nullable UUID founder) {
		int guards = founder != null ? GRAND_GUARD_BEES : GUARD_BEES;
		int workers = founder != null ? GRAND_WORKER_BEES : WORKER_BEES;
		BlockPos c = HiveLayout.center(index);
		BlockPos throne = HiveLayout.throne(index);
		int floorY = HiveLayout.floorY();

		QueenBee queen = ModEntities.QUEEN_BEE.create(level, EntitySpawnReason.STRUCTURE);
		if (queen != null) {
			queen.snapTo(throne.getX() + 0.5, throne.getY(), throne.getZ() + 0.5, 0.0F, 0.0F);
			queen.setYHeadRot(0.0F);
			queen.setYBodyRot(0.0F);
			queen.setHomeTo(throne, 2);
			queen.setPersistenceRequired();
			queen.addTag(RESIDENT_TAG);
			if (founder != null) {
				queen.setFounder(founder);
			}
			level.addFreshEntity(queen);
		}

		for (int i = 0; i < guards; i++) {
			GuardBee guard = ModEntities.GUARD_BEE.create(level, EntitySpawnReason.STRUCTURE);
			if (guard == null) {
				continue;
			}
			double angle = i * Math.PI * 2 / guards;
			guard.snapTo(c.getX() + 0.5 + Math.cos(angle) * 8, floorY + 3, c.getZ() + 0.5 + Math.sin(angle) * 8, (float)Math.toDegrees(angle) + 90.0F, 0.0F);
			guard.setHomeTo(c, 20);
			guard.setPersistenceRequired();
			guard.addTag(RESIDENT_TAG);
			level.addFreshEntity(guard);
		}

		for (int i = 0; i < workers; i++) {
			Bee bee = EntityTypes.BEE.create(level, EntitySpawnReason.STRUCTURE);
			if (bee == null) {
				continue;
			}
			double angle = i * 2.39996;
			double radius = 8 + noise(i, 7, index, 6) * 10;
			bee.snapTo(c.getX() + Math.cos(angle) * radius, c.getY() + noise(i, 8, index, 7) * 8 - 4, c.getZ() + Math.sin(angle) * radius, 0.0F, 0.0F);
			AttributeInstance scale = bee.getAttribute(Attributes.SCALE);
			if (scale != null) {
				scale.setBaseValue(RESIDENT_BEE_SCALE);
			}
			bee.setHomeTo(c, 20);
			bee.setPersistenceRequired();
			bee.addTag(RESIDENT_TAG);
			level.addFreshEntity(bee);
		}
	}
}
