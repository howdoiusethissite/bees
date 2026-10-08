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
	public record Hive(int index, ResourceKey<Level> originLevel, BlockPos originPos) {
		public static final Codec<Hive> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("index").forGetter(Hive::index),
				Level.RESOURCE_KEY_CODEC.fieldOf("origin_level").forGetter(Hive::originLevel),
				BlockPos.CODEC.fieldOf("origin_pos").forGetter(Hive::originPos)
			).apply(i, Hive::new));
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
	public Hive getOrCreate(final ResourceKey<Level> level, final BlockPos pos, final boolean[] created) {
		for (Hive hive : this.hives) {
			if (hive.originLevel().equals(level) && hive.originPos().equals(pos)) {
				created[0] = false;
				return hive;
			}
		}
		Hive hive = new Hive(this.hives.size(), level, pos.immutable());
		this.hives.add(hive);
		this.setDirty();
		created[0] = true;
		return hive;
	}

	public Optional<Hive> byIndex(final int index) {
		return index >= 0 && index < this.hives.size() ? Optional.of(this.hives.get(index)) : Optional.empty();
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
