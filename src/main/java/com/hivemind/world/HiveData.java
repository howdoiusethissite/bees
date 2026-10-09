package com.hivemind.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.hivemind.Hivemind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

/**
 * Remembers which overworld hives have an interior (and where that interior lives),
 * plus where each player should be sent back to when they leave a hive.
 */
public class HiveData extends SavedData {
	/**
	 * @param version which interior layout this hive was built with. Older hives get the newer
	 *                features added the next time someone goes in; see {@link HiveInteriorBuilder#upgrade}.
	 *                0 means the interior hasn't been built yet.
	 * @param founder for a grand hive grown from a royal egg, the player who planted it
	 */
	public record Hive(int index, ResourceKey<Level> originLevel, BlockPos originPos, int version, Optional<UUID> founder) {
		public static final Codec<Hive> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("index").forGetter(Hive::index),
				Level.RESOURCE_KEY_CODEC.fieldOf("origin_level").forGetter(Hive::originLevel),
				BlockPos.CODEC.fieldOf("origin_pos").forGetter(Hive::originPos),
				// Hives saved before versions existed are version 1.
				Codec.INT.optionalFieldOf("version", 1).forGetter(Hive::version),
				UUIDUtil.STRING_CODEC.optionalFieldOf("founder").forGetter(Hive::founder)
			).apply(i, Hive::new));

		public boolean isGrand() {
			return this.founder.isPresent();
		}

		public boolean isBuilt() {
			return this.version > 0;
		}
	}

	public record ReturnPoint(ResourceKey<Level> level, Vec3 pos, float yRot) {
		public static final Codec<ReturnPoint> CODEC = RecordCodecBuilder.create(i -> i.group(
				Level.RESOURCE_KEY_CODEC.fieldOf("level").forGetter(ReturnPoint::level),
				Vec3.CODEC.fieldOf("pos").forGetter(ReturnPoint::pos),
				Codec.FLOAT.fieldOf("y_rot").forGetter(ReturnPoint::yRot)
			).apply(i, ReturnPoint::new));
	}

	public static final Codec<HiveData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Hive.CODEC.listOf().optionalFieldOf("hives", List.of()).forGetter(d -> d.hives),
			Codec.unboundedMap(UUIDUtil.STRING_CODEC, ReturnPoint.CODEC).optionalFieldOf("returns", Map.of()).forGetter(d -> d.returns)
		).apply(i, HiveData::new));

	// Vanilla needs some data fixer type. Command storage is a plain compound tag, so it leaves our data alone.
	public static final SavedDataType<HiveData> TYPE = new SavedDataType<>(
		Hivemind.id("hives"), HiveData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final List<Hive> hives;
	private final Map<UUID, ReturnPoint> returns;

	public HiveData() {
		this(List.of(), Map.of());
	}

	private HiveData(final List<Hive> hives, final Map<UUID, ReturnPoint> returns) {
		this.hives = new ArrayList<>(hives);
		this.returns = new HashMap<>(returns);
	}

	public static HiveData get(final MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** Finds the hive record for a block in the outside world, or creates a new one. */
	public Hive getOrCreate(final ResourceKey<Level> level, final BlockPos pos) {
		for (Hive hive : this.hives) {
			if (hive.originLevel().equals(level) && hive.originPos().equals(pos)) {
				return hive;
			}
		}
		// Version 0: the interior gets built when the player goes in.
		Hive hive = new Hive(this.hives.size(), level, pos.immutable(), 0, Optional.empty());
		this.hives.add(hive);
		this.setDirty();
		return hive;
	}

	/** Records a grand hive grown from a royal egg at {@code pos}. Its interior is built the first time someone goes in. */
	public Hive createGrand(final ResourceKey<Level> level, final BlockPos pos, final UUID founder) {
		Hive hive = new Hive(this.hives.size(), level, pos.immutable(), 0, Optional.of(founder));
		this.hives.add(hive);
		this.setDirty();
		return hive;
	}

	/** The grand hive whose outside sits around {@code pos}, if there is one. */
	public Optional<Hive> findGrand(final ResourceKey<Level> level, final BlockPos pos, final int range) {
		Hive best = null;
		double bestDist = Double.MAX_VALUE;
		for (Hive hive : this.hives) {
			if (!hive.isGrand() || !hive.originLevel().equals(level)) {
				continue;
			}
			double dist = hive.originPos().distSqr(pos);
			if (dist <= range * range && dist < bestDist) {
				best = hive;
				bestDist = dist;
			}
		}
		return Optional.ofNullable(best);
	}

	public Optional<Hive> byIndex(final int index) {
		return index >= 0 && index < this.hives.size() ? Optional.of(this.hives.get(index)) : Optional.empty();
	}

	/** Records that a hive's interior has been brought up to the current layout. */
	public Hive markUpgraded(final Hive hive) {
		Hive upgraded = new Hive(hive.index(), hive.originLevel(), hive.originPos(), HiveInteriorBuilder.LAYOUT_VERSION, hive.founder());
		this.hives.set(hive.index(), upgraded);
		this.setDirty();
		return upgraded;
	}

	public List<Hive> hives() {
		return this.hives;
	}

	public void setReturn(final UUID player, final ReturnPoint point) {
		this.returns.put(player, point);
		this.setDirty();
	}

	public Optional<ReturnPoint> takeReturn(final UUID player) {
		ReturnPoint point = this.returns.remove(player);
		if (point != null) {
			this.setDirty();
		}
		return Optional.ofNullable(point);
	}
}
