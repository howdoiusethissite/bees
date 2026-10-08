package com.hivemind.world;

import com.hivemind.Hivemind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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

	public static AABB bounds(final int index) {
		return new AABB(center(index)).inflate(RADIUS_XZ + 2, RADIUS_Y + 2, RADIUS_XZ + 2);
	}
}
