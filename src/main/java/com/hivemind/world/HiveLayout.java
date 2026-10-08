package com.hivemind.world;

import com.hivemind.Hivemind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Where each hive interior sits inside the hive dimension, and the landmarks inside it. */
public final class HiveLayout {
	public static final ResourceKey<Level> HIVE_LEVEL = ResourceKey.create(Registries.DIMENSION, Hivemind.id("hive"));

	/** Interiors are laid out on a grid this far apart so they never see each other. */
	public static final int SPACING = 512;
	public static final int GRID_WIDTH = 32;
	public static final int CENTER_Y = 80;
	/** Horizontal and vertical radius of the hive shell. */
	public static final int RADIUS_XZ = 26;
	public static final int RADIUS_Y = 18;
	/** The floor surface is this far below the center. */
	public static final int FLOOR_DEPTH = 8;

	private HiveLayout() {
	}

	public static boolean isHiveLevel(final Level level) {
		return level.dimension().equals(HIVE_LEVEL);
	}

	public static BlockPos center(final int index) {
		int gx = index % GRID_WIDTH;
		int gz = index / GRID_WIDTH;
		return new BlockPos(SPACING * 2 + gx * SPACING, CENTER_Y, SPACING * 2 + gz * SPACING);
	}

	/** Which hive interior a position in the hive dimension belongs to, or -1 if it is in the gaps between them. */
	public static int indexAt(final Vec3 pos) {
		int gx = Math.floorDiv((int)Math.floor(pos.x) - SPACING * 2 + SPACING / 2, SPACING);
		int gz = Math.floorDiv((int)Math.floor(pos.z) - SPACING * 2 + SPACING / 2, SPACING);
		if (gx < 0 || gz < 0 || gx >= GRID_WIDTH) {
			return -1;
		}
		return gz * GRID_WIDTH + gx;
	}

	public static int floorY() {
		return CENTER_Y - FLOOR_DEPTH;
	}

	/** The glowing exit block sits at the south end of the floor. */
	public static BlockPos exitPos(final int index) {
		return center(index).offset(0, -FLOOR_DEPTH + 1, RADIUS_XZ - 6);
	}

	/** Players arrive just in front of the exit, facing the queen. */
	public static Vec3 arrivalPos(final int index) {
		return Vec3.atBottomCenterOf(exitPos(index).north(2));
	}

	public static BlockPos throne(final int index) {
		return center(index).offset(0, -FLOOR_DEPTH + 3, 0);
	}

	/** The middle of the honey pool, set into the floor behind the throne (north of it). */
	public static BlockPos honeySpring(final int index) {
		return center(index).offset(0, -FLOOR_DEPTH, -11);
	}

	/** Angles (degrees) of the four nursery patches. They fall in the gaps between the comb pillars. */
	private static final int[] NURSERY_ANGLES = {30, 150, 210, 330};
	private static final double NURSERY_RADIUS = 10.5;

	/** The middle cell of each nursery patch, at floor level. */
	public static List<BlockPos> nurseryCenters(final int index) {
		BlockPos c = center(index);
		List<BlockPos> centers = new ArrayList<>();
		for (int degrees : NURSERY_ANGLES) {
			double angle = Math.toRadians(degrees);
			int x = c.getX() + Mth.floor(Math.cos(angle) * NURSERY_RADIUS + 0.5);
			int z = c.getZ() + Mth.floor(Math.sin(angle) * NURSERY_RADIUS + 0.5);
			centers.add(new BlockPos(x, floorY(), z));
		}
		return centers;
	}

	/** Whether a patch offset is one of its cells. Two opposite corners are trimmed so the patch reads as a little hexagon. */
	public static boolean isNurseryCell(final int dx, final int dz) {
		return Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && !(dx == dz && dx != 0);
	}

	public static AABB bounds(final int index) {
		return new AABB(center(index)).inflate(RADIUS_XZ + 2, RADIUS_Y + 2, RADIUS_XZ + 2);
	}
}
