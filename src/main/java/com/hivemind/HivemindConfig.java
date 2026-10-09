package com.hivemind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Settings read from {@code config/hivemind.json}. The file is written with the defaults the first time
 * the game starts, and any setting missing from it (say, after an update adds one) is filled back in.
 * Edit it while the game is closed.
 */
public final class HivemindConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	/** Field names starting with an underscore are comments for whoever opens the file. */
	public static final class Values {
		public String _maxHiveHoney = "How much honey a beehive or bee nest can hold. Vanilla stops at 5; past that the hive keeps the extra in reserve and still looks full.";
		public int maxHiveHoney = 20;

		public String _shearsBonusHoneycomb = "Extra honeycomb from shearing a full hive, on top of vanilla's 3.";
		public int shearsBonusHoneycomb = 3;
		public String _shearsHoneycombPerStoredHoney = "Extra honeycomb for every point of honey stored past 5.";
		public double shearsHoneycombPerStoredHoney = 1.0;

		public String _bottleBonusHoney = "Extra honey bottles from bottling a full hive, on top of vanilla's 1. The extras don't use up glass bottles.";
		public int bottleBonusHoney = 1;
		public String _bottleHoneyPerStoredHoney = "Extra honey bottles for every point of honey stored past 5.";
		public double bottleHoneyPerStoredHoney = 0.5;

		public String _hiveWorkTimeMultiplier = "How long bees stay inside a hive making honey, compared to vanilla (2 minutes with nectar, 30 seconds without). 1.0 is vanilla.";
		public double hiveWorkTimeMultiplier = 0.2;
		public String _pollinationTimeMultiplier = "How long a bee has to hover over a flower to pick up nectar, compared to vanilla (20 seconds). 1.0 is vanilla.";
		public double pollinationTimeMultiplier = 0.5;

		public String _cravingDays = "How many in-game days the queen keeps craving the same flower before she wants a new one.";
		public int cravingDays = 7;

		public String _grandHiveGrowMinutes = "Real-time minutes a royal egg takes to grow into a grand hive.";
		public double grandHiveGrowMinutes = 10.0;
	}

	private static Values values = new Values();

	private HivemindConfig() {
	}

	public static Values get() {
		return values;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(Hivemind.MOD_ID + ".json");
		Values loaded = new Values();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				JsonObject json = GSON.fromJson(reader, JsonObject.class);
				if (json != null) {
					// Comments always come from the code, so a reworded explanation reaches old files too.
					json.keySet().removeIf(key -> key.startsWith("_"));
					// Gson builds Values with its constructor, so every comment keeps its default text.
					loaded = GSON.fromJson(json, Values.class);
				}
			} catch (IOException | RuntimeException e) {
				Hivemind.LOGGER.error("Couldn't read {}, using the defaults", path, e);
				values = new Values();
				return;
			}
		}
		loaded.maxHiveHoney = Math.max(5, loaded.maxHiveHoney);
		loaded.shearsBonusHoneycomb = Math.max(0, loaded.shearsBonusHoneycomb);
		loaded.shearsHoneycombPerStoredHoney = Math.max(0.0, loaded.shearsHoneycombPerStoredHoney);
		loaded.bottleBonusHoney = Math.max(0, loaded.bottleBonusHoney);
		loaded.bottleHoneyPerStoredHoney = Math.max(0.0, loaded.bottleHoneyPerStoredHoney);
		loaded.hiveWorkTimeMultiplier = Math.clamp(loaded.hiveWorkTimeMultiplier, 0.0, 10.0);
		loaded.pollinationTimeMultiplier = Math.clamp(loaded.pollinationTimeMultiplier, 0.0, 1.0);
		loaded.cravingDays = Math.max(1, loaded.cravingDays);
		loaded.grandHiveGrowMinutes = Math.max(0.05, loaded.grandHiveGrowMinutes);
		values = loaded;
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(values, writer);
		} catch (IOException e) {
			Hivemind.LOGGER.warn("Couldn't write {}", path, e);
		}
	}
}
